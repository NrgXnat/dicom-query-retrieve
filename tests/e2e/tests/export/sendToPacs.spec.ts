/**
 * Send to PACS: pushing an XNAT session's DICOM scans to a PACS.
 *
 * The sessions sent here are generated for the run (lib/dicom.ts), with fresh
 * UIDs and patient values that start with E2E, so nothing sent can collide
 * with the fixture studies. Each run adds one small study to the PACS; the
 * rest of the suite looks studies up by UID, never by count, for that reason.
 */
import { test, expect } from '../../lib/fixtures';
import { Pacs, XnatClient } from '../../lib/dqr';
import { ADMIN, PACS_LABEL, projectNameFor, uniqueLabel } from '../../lib/env';
import { GeneratedStudy, generateStudy } from '../../lib/dicom';

const PROJECT = projectNameFor('exp');

let admin: XnatClient;
let pacs: Pacs;
let study: GeneratedStudy;
let sessionId: string;
const madePacs: number[] = [];

test.beforeAll(async () => {
    admin = await XnatClient.as(ADMIN);
    pacs = await admin.pacsByLabel(PACS_LABEL);
    await admin.ensureProject(PROJECT);
    study = generateStudy(Date.now().toString(36).toUpperCase(), 2);
    sessionId = await admin.uploadStudy(PROJECT, uniqueLabel('ExpSubj'), uniqueLabel('ExpSess'), study);
});

test.afterAll(async () => {
    for (const id of madePacs) await admin.deletePacsQuietly(id);
    await admin.deleteProjectQuietly(PROJECT);
    await admin.dispose();
});

async function makePacs(overrides: Record<string, unknown>): Promise<Pacs> {
    const ae = `E2EX${Date.now().toString(36).toUpperCase()}${Math.floor(Math.random() * 100)}`.slice(0, 16);
    const p = await admin.createPacs({
        aeTitle: ae,
        label: `E2E_${ae}`,
        host: '127.0.0.1',
        queryRetrievePort: 4242,
        queryable: true,
        storable: true,
        dicomWebEnabled: false,
        ...overrides,
    });
    madePacs.push(p.id);
    return p;
}

test('the session uploads as a CT session with one scan per generated series', async () => {
    expect((await admin.getSession(sessionId)).meta['xsi:type']).toBe('xnat:ctSessionData');
    expect((await admin.getScans(sessionId)).map(s => String(s.ID)).sort()).toEqual(['1', '2']);
});

test('sending one scan puts that series, and only that series, in the PACS', async () => {
    const res = await admin.rawExport(pacs.id, sessionId, ['1']);
    const body = await res.text();
    expect(res.ok(), `export: HTTP ${res.status()} ${body.slice(0, 200)}`).toBeTruthy();
    const answer = JSON.parse(body);
    expect(answer.session).toBe(sessionId);
    expect(answer.scans).toEqual(['1']);

    // The send runs in the background; poll the PACS for the study.
    await expect
        .poll(
            async () =>
                (await admin.queryStudies({ pacsId: pacs.id, studyInstanceUid: study.studyInstanceUid }))
                    .length,
            {
                timeout: 120_000,
                intervals: [5_000],
            },
        )
        .toBe(1);
    const series = await admin.querySeries(pacs.id, study.studyInstanceUid);
    expect(series.map(s => s.seriesInstanceUid)).toEqual([study.series[0].seriesInstanceUid]);

    const [found] = await admin.queryStudies({ pacsId: pacs.id, studyInstanceUid: study.studyInstanceUid });
    expect(found.patient?.id, 'the data arrives as archived, with its own patient ID').toBe(study.patientId);
});

test('the send is recorded as a completed workflow on the session, by the user who sent it', async () => {
    const res = await admin.rawExport(pacs.id, sessionId, ['2']);
    expect(res.ok()).toBeTruthy();
    const { workflow } = await res.json();

    await expect
        .poll(async () => (await (await admin.get(`/xapi/workflows/${workflow}`)).json()).status, {
            timeout: 120_000,
            intervals: [5_000],
        })
        .toBe('Complete');
    const wf = await (await admin.get(`/xapi/workflows/${workflow}`)).json();
    expect(wf.pipelineName).toBe('EXPORT_TO_PACS_REQUEST');
    expect(wf.id).toBe(sessionId);
    expect.soft(wf.dataType, 'the workflow names the session type that was sent').toBe('xnat:ctSessionData');
    expect.soft(wf.createUser, 'the workflow names who sent it').toBe(admin.account.username);

    await expect
        .poll(async () => (await admin.querySeries(pacs.id, study.studyInstanceUid)).length, {
            timeout: 120_000,
            intervals: [5_000],
        })
        .toBe(2);
});

test('a send with no scans is refused with 400', async () => {
    expect((await admin.rawExport(pacs.id, sessionId, [])).status()).toBe(400);
});

test('a send with no session is refused with 400', async () => {
    expect((await admin.rawExport(pacs.id, '', ['1'])).status()).toBe(400);
});

test('a send to an unknown PACS is 404', async () => {
    expect((await admin.rawExport(999999999, sessionId, ['1'])).status()).toBe(404);
});

test('a send of a session that does not exist is 404', async () => {
    expect((await admin.rawExport(pacs.id, 'XNAT_E_NO_SUCH', ['1'])).status()).toBe(404);
});

test('a send to a PACS that is not storable is refused with 409', async () => {
    const queryOnly = await makePacs({ storable: false });
    expect((await admin.rawExport(queryOnly.id, sessionId, ['1'])).status()).toBe(409);
});

test('a send to a storable PACS that does not answer is refused with 503', async () => {
    const down = await makePacs({ queryRetrievePort: 4299 });
    expect((await admin.rawExport(down.id, sessionId, ['1'])).status()).toBe(503);
});
