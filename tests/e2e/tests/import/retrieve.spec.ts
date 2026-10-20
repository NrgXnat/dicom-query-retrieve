/**
 * Real retrieves from the PACS into XNAT, followed to the archived session.
 *
 * Each test queues an import, waits for its history row to reach RECEIVED or
 * FAILED, then waits for the session in the project, and asserts on what was
 * archived: session type, labels, and the scans compared with what the PACS
 * says the study holds. "Queued" is never treated as success.
 *
 * The scheduler is set to look for work every ten seconds for this spec, and
 * put back afterwards, so an import finishes in seconds rather than a minute.
 * Each test imports into a project of its own, deleted afterwards.
 */
import { test, expect } from '../../lib/fixtures';
import { ImportRequest, Pacs, XnatClient } from '../../lib/dqr';
import { ADMIN, PACS_LABEL, RECEIVER_AE, RECEIVER_PORT, projectNameFor, uniqueLabel } from '../../lib/env';
import { DISCO, DiscoStudy, SESSION_TYPE } from '../../lib/disco';
import { FAST_QUEUE, SiteSettings, waitForOutcome, waitForSession } from '../../lib/stack';

let admin: XnatClient;
/** Each test gets its own project, so imports of the same study in different tests never meet. */
let PROJECT: string;
let pacs: Pacs;
let settings: SiteSettings;
const madePacs: number[] = [];

test.beforeAll(async () => {
    admin = await XnatClient.as(ADMIN);
    pacs = await admin.pacsByLabel(PACS_LABEL);
    settings = new SiteSettings(admin);
    await settings.apply(FAST_QUEUE);
});

test.beforeEach(async () => {
    PROJECT = projectNameFor('ret');
    await admin.ensureProject(PROJECT);
});

test.afterEach(async () => {
    await admin.deleteProjectQuietly(PROJECT);
});

test.afterAll(async () => {
    for (const id of madePacs) await admin.deletePacsQuietly(id);
    await settings.restore();
    await admin.dispose();
});

function request(study: DiscoStudy, extra: Partial<ImportRequest> = {}, studyExtra = {}): ImportRequest {
    return {
        pacsId: pacs.id,
        aeTitle: RECEIVER_AE,
        port: RECEIVER_PORT,
        projectId: PROJECT,
        forceImport: true,
        requestId: uniqueLabel('e2e'),
        studies: [{ studyInstanceUid: study.studyInstanceUid, ...studyExtra }],
        ...extra,
    };
}

function labels(prefix: string) {
    return { Subject: uniqueLabel(`${prefix}Subj`), Session: uniqueLabel(`${prefix}Sess`) };
}

/** Series numbers the PACS reports for a study, sorted, as strings (XNAT scan IDs). */
async function pacsSeriesNumbers(study: DiscoStudy): Promise<string[]> {
    return (await admin.querySeries(pacs.id, study.studyInstanceUid)).map(s => String(s.seriesNumber)).sort();
}

async function scanIds(sessionId: string): Promise<string[]> {
    return (await admin.getScans(sessionId)).map(s => String(s.ID)).sort();
}

test('a series-level import of a whole study archives every series as a scan, under the typed labels', async () => {
    const relabel = labels('Ser');
    const series = await admin.querySeries(pacs.id, DISCO[1].studyInstanceUid);
    const req = request(
        DISCO[1],
        {},
        { seriesInstanceUids: series.map(s => s.seriesInstanceUid), relabelMap: relabel },
    );

    const queued = await admin.queueImport(req);
    expect(queued.map(r => r.studyInstanceUid)).toEqual([DISCO[1].studyInstanceUid]);
    expect(queued[0].status).toBe('QUEUED');
    expect(queued[0].retrieveLevel).toBe('SERIES');

    const [outcome] = await waitForOutcome(admin, req.requestId!);
    expect(outcome.status, `history: ${outcome.errorMessage ?? ''}`).toBe('RECEIVED');
    expect(outcome.seriesIds.sort(), 'history names every series it moved').toEqual(
        series.map(s => s.seriesInstanceUid).sort(),
    );

    const session = await waitForSession(admin, PROJECT, relabel.Session);
    expect(session.xsiType).toBe(SESSION_TYPE.MR);
    expect(await scanIds(session.ID), 'one scan per series on the PACS').toEqual(
        await pacsSeriesNumbers(DISCO[1]),
    );
    const subjectId = (await admin.getSession(session.ID)).data_fields.subject_ID;
    expect(await admin.getSubjectLabel(subjectId)).toBe(relabel.Subject);
});

test('a series-level import of chosen series archives only those series', async () => {
    const relabel = labels('Pick');
    const series = (await admin.querySeries(pacs.id, DISCO[6].studyInstanceUid)).sort(
        (a, b) => a.seriesNumber - b.seriesNumber,
    );
    expect(series.length, 'Disco Study 6 has more than one series to choose from').toBeGreaterThan(1);
    const chosen = series[0];

    const req = request(
        DISCO[6],
        {},
        { seriesInstanceUids: [chosen.seriesInstanceUid], relabelMap: relabel },
    );
    await admin.queueImport(req);
    const [outcome] = await waitForOutcome(admin, req.requestId!);
    expect(outcome.status, `history: ${outcome.errorMessage ?? ''}`).toBe('RECEIVED');
    expect(outcome.seriesIds).toEqual([chosen.seriesInstanceUid]);

    const session = await waitForSession(admin, PROJECT, relabel.Session);
    expect(await scanIds(session.ID)).toEqual([String(chosen.seriesNumber)]);
});

test('a series-level import by series description archives every series with that description', async () => {
    const relabel = labels('Desc');
    const series = await admin.querySeries(pacs.id, DISCO[2].studyInstanceUid);
    const description = series.find(
        s => series.filter(o => o.seriesDescription === s.seriesDescription).length > 1,
    )?.seriesDescription;
    expect(description, 'Disco Study 2 has a description shared by two series').toBeDefined();
    const expected = series
        .filter(s => s.seriesDescription === description)
        .map(s => String(s.seriesNumber))
        .sort();

    const req = request(DISCO[2], {}, { seriesDescriptions: [description], relabelMap: relabel });
    await admin.queueImport(req);
    const [outcome] = await waitForOutcome(admin, req.requestId!);
    expect(outcome.status, `history: ${outcome.errorMessage ?? ''}`).toBe('RECEIVED');

    const session = await waitForSession(admin, PROJECT, relabel.Session);
    expect(await scanIds(session.ID)).toEqual(expected);
});

test('a study-level import of a series-level PACS retrieves the whole study in one move', async () => {
    const relabel = labels('Stu');
    const req = request(DISCO[7], { retrieveLevel: 'STUDY' }, { relabelMap: relabel });

    const queued = await admin.queueImport(req);
    expect(queued[0].retrieveLevel).toBe('STUDY');
    expect(queued[0].seriesIds, 'a study-level request names no series').toEqual([]);

    const [outcome] = await waitForOutcome(admin, req.requestId!);
    expect(outcome.status, `history: ${outcome.errorMessage ?? ''}`).toBe('RECEIVED');
    expect(outcome.retrieveLevel).toBe('STUDY');

    const session = await waitForSession(admin, PROJECT, relabel.Session);
    expect(session.xsiType).toBe(SESSION_TYPE.US);
    expect(await scanIds(session.ID)).toEqual(await pacsSeriesNumbers(DISCO[7]));
});

test('a PACS set to study level retrieves at study level without being asked', async () => {
    // The same Orthanc, registered a second time through 127.0.0.1 at study
    // level, with an all-week schedule as the admin page would give it.
    const studyPacs = await admin.createPacs({
        aeTitle: 'ORTHANC',
        label: uniqueLabel('E2E_STUDYLEVEL'),
        host: '127.0.0.1',
        queryRetrievePort: pacs.queryRetrievePort,
        queryable: true,
        storable: false,
        dicomWebEnabled: false,
        retrieveLevel: 'STUDY',
    });
    madePacs.push(studyPacs.id);
    await admin.openAllWeek(studyPacs.id);

    const relabel = labels('StuPacs');
    const req = request(DISCO[5], { pacsId: studyPacs.id }, { relabelMap: relabel });
    const queued = await admin.queueImport(req);
    expect(queued[0].retrieveLevel).toBe('STUDY');

    const [outcome] = await waitForOutcome(admin, req.requestId!);
    expect(outcome.status, `history: ${outcome.errorMessage ?? ''}`).toBe('RECEIVED');
    const session = await waitForSession(admin, PROJECT, relabel.Session);
    expect(session.xsiType).toBe(SESSION_TYPE.CT);
    expect(await scanIds(session.ID)).toEqual(await pacsSeriesNumbers(DISCO[5]));
});

test('without relabeling, the subject is the patient name and the session is the study ID', async () => {
    const req = request(DISCO[3], { retrieveLevel: 'STUDY' });
    await admin.queueImport(req);
    const [outcome] = await waitForOutcome(admin, req.requestId!);
    expect(outcome.status, `history: ${outcome.errorMessage ?? ''}`).toBe('RECEIVED');

    // XNAT labels cannot contain spaces, so "Disco Study 3" becomes Disco_Study_3.
    const session = await waitForSession(admin, PROJECT, 'Disco_Study_3');
    const subjectId = (await admin.getSession(session.ID)).data_fields.subject_ID;
    expect(await admin.getSubjectLabel(subjectId)).toBe('Disco_Patient_3');
});

test('one request for two studies queues one row per study, and both arrive', async () => {
    const a = labels('TwoA');
    const b = labels('TwoB');
    const req: ImportRequest = {
        ...request(DISCO[2], { retrieveLevel: 'STUDY' }),
        studies: [
            { studyInstanceUid: DISCO[2].studyInstanceUid, relabelMap: a },
            { studyInstanceUid: DISCO[6].studyInstanceUid, relabelMap: b },
        ],
    };
    const queued = await admin.queueImport(req);
    expect(queued.map(r => r.studyInstanceUid).sort()).toEqual(
        [DISCO[2].studyInstanceUid, DISCO[6].studyInstanceUid].sort(),
    );

    const outcomes = await waitForOutcome(admin, req.requestId!);
    expect(outcomes.map(o => o.status)).toEqual(['RECEIVED', 'RECEIVED']);
    expect((await waitForSession(admin, PROJECT, a.Session)).xsiType).toBe(SESSION_TYPE.CT);
    expect((await waitForSession(admin, PROJECT, b.Session)).xsiType).toBe(SESSION_TYPE.MR);
});

test('importing a study again into the same project adds nothing and duplicates nothing', async () => {
    const relabel = labels('Again');
    for (let i = 0; i < 2; i++) {
        const req = request(DISCO[7], { retrieveLevel: 'STUDY' }, { relabelMap: relabel });
        await admin.queueImport(req);
        const [outcome] = await waitForOutcome(admin, req.requestId!);
        expect(outcome.status, `import ${i + 1}: ${outcome.errorMessage ?? ''}`).toBe('RECEIVED');
    }
    const session = await waitForSession(admin, PROJECT, relabel.Session);
    // Give the second import's files time to be merged before counting.
    await expect
        .poll(
            async () => (await admin.listSessions(PROJECT)).filter(s => s.label === relabel.Session).length,
            {
                timeout: 120_000,
                intervals: [10_000],
            },
        )
        .toBe(1);
    expect(await scanIds(session.ID)).toEqual(await pacsSeriesNumbers(DISCO[7]));
});

test('a study-level import that names series is refused with 400', async () => {
    const series = await admin.querySeries(pacs.id, DISCO[1].studyInstanceUid);
    const res = await admin.rawImport(
        request(DISCO[1], { retrieveLevel: 'STUDY' }, { seriesInstanceUids: [series[0].seriesInstanceUid] }),
    );
    expect(res.status()).toBe(400);
});

test('a study-level import from a DICOMweb PACS is refused with 400', async () => {
    const web = await admin.createPacs({
        aeTitle: uniqueLabel('E2EWEB').slice(0, 16),
        label: uniqueLabel('E2E_WEB'),
        queryable: true,
        storable: false,
        dicomWebEnabled: true,
        dicomWebRootUrl: 'http://127.0.0.1:8042/dicom-web/',
    });
    madePacs.push(web.id);
    const res = await admin.rawImport(request(DISCO[1], { pacsId: web.id, retrieveLevel: 'STUDY' }));
    expect(res.status()).toBe(400);
});

test('an import of a study the PACS does not have is refused, not reported as queued', async () => {
    const req = request(DISCO[1]);
    req.studies = [{ studyInstanceUid: '1.2.826.0.1.3680043.2.1125.999999999.1' }];
    const res = await admin.rawImport(req);
    const body = await res.text();
    expect(
        res.ok() && JSON.parse(body || '[]').length === 0,
        `answered ${res.status()} ${body.slice(0, 120)}: success with nothing queued tells the user it worked`,
    ).toBeFalsy();
});

test('an import whose series filter matches nothing is refused, not reported as queued', async () => {
    const res = await admin.rawImport(request(DISCO[1], {}, { seriesDescriptions: ['NO SUCH SERIES E2E'] }));
    const body = await res.text();
    expect(
        res.ok() && JSON.parse(body || '[]').length === 0,
        `answered ${res.status()} ${body.slice(0, 120)}: success with nothing queued tells the user it worked`,
    ).toBeFalsy();
});

test('an import from an unknown PACS is 404', async () => {
    expect((await admin.rawImport(request(DISCO[1], { pacsId: 999999999 }))).status()).toBe(404);
});
