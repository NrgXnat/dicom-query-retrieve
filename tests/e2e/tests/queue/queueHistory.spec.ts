/**
 * The import queue and history: what waits, what runs, what is recorded, and
 * what happens when a retrieve fails.
 *
 * Requests that must stay queued are sent to a PACS made for the spec with no
 * availability window: the same Orthanc, registered again through 127.0.0.1,
 * so nothing it queues runs until a test opens a window for it. Retrieves that
 * should run use the fixture PACS with the scheduler set to ten seconds.
 *
 * Every request relabels its study, so that what arrives never depends on a
 * relabel script an earlier test or user stored for that study. Whether DQR
 * reuses such a script is unconfirmed (see neutralizeRelabels); relabeling
 * every request makes the question irrelevant here.
 */
import { test, expect } from '../../lib/fixtures';
import { ImportRequest, Pacs, PacsRequestRow, XnatClient } from '../../lib/dqr';
import { ADMIN, PACS_LABEL, RECEIVER_AE, RECEIVER_PORT, projectNameFor, uniqueLabel } from '../../lib/env';
import { DISCO, DiscoStudy } from '../../lib/disco';
import {
    FAST_FAILURE,
    FAST_QUEUE,
    SiteSettings,
    cleanupAll,
    neutralizeRelabels,
    waitForOutcome,
} from '../../lib/stack';

let admin: XnatClient;
let pacs: Pacs;
let idle: Pacs;
let settings: SiteSettings;
let PROJECT: string;
const queuedIds: number[] = [];
const relabeled = new Set<DiscoStudy>();
const madeLabels: string[] = [];

/** A study entry with labels of its own, recorded for cleanup. */
function labeled(study: DiscoStudy) {
    const relabelMap = { Subject: uniqueLabel('QSubj'), Session: uniqueLabel('QSess') };
    relabeled.add(study);
    madeLabels.push(relabelMap.Session);
    return { studyInstanceUid: study.studyInstanceUid, relabelMap };
}

function request(study: DiscoStudy, extra: Partial<ImportRequest> = {}): ImportRequest {
    return {
        pacsId: idle.id,
        aeTitle: RECEIVER_AE,
        port: RECEIVER_PORT,
        projectId: PROJECT,
        forceImport: true,
        retrieveLevel: 'STUDY',
        requestId: uniqueLabel('e2eq'),
        studies: [labeled(study)],
        ...extra,
    };
}

/**
 * Queues a request on the fixture PACS and waits for it to finish. Used as a
 * control: once it has run, the scheduler has had its chance to run anything
 * else that was eligible, so a request that is still waiting was held back.
 */
async function runControl(): Promise<void> {
    const control = request(DISCO[4], { pacsId: pacs.id });
    await queue(control);
    const [outcome] = await waitForOutcome(admin, control.requestId!);
    expect(outcome.status, `control request: ${outcome.errorMessage ?? ''}`).toBe('RECEIVED');
}

async function queue(req: ImportRequest): Promise<PacsRequestRow[]> {
    const rows = await admin.queueImport(req);
    queuedIds.push(...rows.map(r => r.id));
    return rows;
}

test.beforeAll(async () => {
    admin = await XnatClient.as(ADMIN);
    pacs = await admin.pacsByLabel(PACS_LABEL);
    settings = new SiteSettings(admin);
    await settings.apply(FAST_QUEUE);
});

test.beforeEach(async () => {
    PROJECT = projectNameFor('que');
    await admin.ensureDqrProject(PROJECT);
    idle = await admin.createPacs({
        aeTitle: 'ORTHANC',
        label: uniqueLabel('E2E_IDLE'),
        host: '127.0.0.1',
        queryRetrievePort: pacs.queryRetrievePort,
        queryable: true,
        storable: false,
        dicomWebEnabled: false,
    });
});

test.afterEach(async () => {
    await cleanupAll(
        () => admin.removeFromQueueQuietly(queuedIds.splice(0)),
        () => admin.deletePacsQuietly(idle.id),
        () => admin.deleteProjectQuietly(PROJECT),
        async () => {
            await settings.restore();
            await settings.apply(FAST_QUEUE);
        },
    );
});

test.afterAll(async () => {
    try {
        await cleanupAll(
            () => neutralizeRelabels(admin, pacs.id, [...relabeled]),
            () => admin.clearUnassignedPrearchive(madeLabels),
            () => settings.restore(),
        );
    } finally {
        await admin.dispose();
    }
});

test('a queued request can be listed, counted and read back by id', async () => {
    const [row] = await queue(request(DISCO[1]));

    const mine = await admin.queue();
    expect(mine.map(r => r.id)).toContain(row.id);
    const count = await (await admin.get('/xapi/dqr/import/queue/count')).json();
    expect(count, 'the count matches the listing it counts').toBe(mine.length);

    const one = await (await admin.get(`/xapi/dqr/import/queue/${row.id}`)).json();
    expect(one.studyInstanceUid).toBe(DISCO[1].studyInstanceUid);
    expect(one.xnatProject).toBe(PROJECT);
    expect(one.status).toBe('QUEUED');
    expect(one.destinationAeTitle).toBe(`${RECEIVER_AE}:${RECEIVER_PORT}`);
});

test('a single study is queued at high priority, several at standard priority', async () => {
    const [single] = await queue(request(DISCO[1]));
    const several = await queue({
        ...request(DISCO[2]),
        studies: [
            { studyInstanceUid: DISCO[2].studyInstanceUid },
            { studyInstanceUid: DISCO[3].studyInstanceUid },
        ],
    });
    expect(single.priority).toBe(1);
    expect(several.map(r => r.priority)).toEqual([10, 10]);
});

test('the queue pages and sorts', async () => {
    // Three separate requests, so the rows have distinct queue times and the
    // order is fully determined. One request queues all its rows at once.
    const ids: number[] = [];
    for (const s of [DISCO[1], DISCO[2], DISCO[3]]) {
        const [row] = await queue(request(s));
        ids.push(row.id);
    }
    const listing = async (params: Record<string, string | number>) => {
        const res = await admin.get('/xapi/dqr/import/queue', params);
        expect(res.ok(), `GET queue ${JSON.stringify(params)}: HTTP ${res.status()}`).toBeTruthy();
        return ((await res.json()) as PacsRequestRow[]).map(r => r.id);
    };
    const queuedTimes = (await admin.queue()).filter(r => ids.includes(r.id)).map(r => r.queuedTime);
    expect(new Set(queuedTimes).size, 'precondition: the three rows have distinct queue times').toBe(3);

    const asc = (await listing({ sort: 'asc', pageSize: 1000 })).filter(id => ids.includes(id));
    const desc = (await listing({ sort: 'desc', pageSize: 1000 })).filter(id => ids.includes(id));
    expect(asc, 'asc lists the oldest first').toEqual(ids);
    expect(desc, 'desc lists the newest first').toEqual([...ids].reverse());

    // Nothing else is queued after ours, so ours are the first rows newest first.
    const pages = [
        await listing({ sort: 'desc', page: 1, pageSize: 1 }),
        await listing({ sort: 'desc', page: 2, pageSize: 1 }),
    ];
    expect(pages, 'one row per page, in order').toEqual([[ids[2]], [ids[1]]]);
});

test('a sort other than asc or desc is refused with 400', async () => {
    expect((await admin.get('/xapi/dqr/import/queue', { sort: 'sideways' })).status()).toBe(400);
    expect((await admin.get('/xapi/dqr/import/history', { sort: 'sideways' })).status()).toBe(400);
});

test('an unknown queue or history id is 404', async () => {
    expect((await admin.get('/xapi/dqr/import/queue/999999999')).status()).toBe(404);
    expect((await admin.get('/xapi/dqr/import/history/999999999')).status()).toBe(404);
});

test('a removed request leaves the queue and is never run', async () => {
    const [row] = await queue(request(DISCO[1]));
    const res = await admin.delete(`/xapi/dqr/import/queue/${row.id}`);
    expect(res.ok(), `remove: HTTP ${res.status()}`).toBeTruthy();

    expect((await admin.queue()).map(r => r.id)).not.toContain(row.id);
    await admin.openAllWeek(idle.id);
    await runControl();
    expect(
        (await admin.history(true)).filter(r => r.requestId === row.requestId),
        'nothing ran',
    ).toEqual([]);
});

test('several requests can be removed at once', async () => {
    const rows = await queue({
        ...request(DISCO[1]),
        studies: [DISCO[1], DISCO[2]].map(s => labeled(s)),
    });
    const res = await admin.post(
        '/xapi/dqr/import/queue',
        rows.map(r => r.id),
    );
    expect(res.ok(), `bulk remove: HTTP ${res.status()}`).toBeTruthy();
    const left = (await admin.queue()).map(r => r.id);
    for (const r of rows) expect(left).not.toContain(r.id);
});

test('a request waits while its PACS has no open window, and runs once one opens', async () => {
    const req = request(DISCO[7]);
    await queue(req);

    await runControl();
    const still = (await admin.queue()).filter(r => r.requestId === req.requestId);
    expect(
        still.map(r => r.status),
        'no window, so it stays queued',
    ).toEqual(['QUEUED']);

    await admin.openAllWeek(idle.id);
    const [outcome] = await waitForOutcome(admin, req.requestId!);
    expect(outcome.status, `history: ${outcome.errorMessage ?? ''}`).toBe('RECEIVED');
});

test('the retrieve level is fixed when a request is queued, not when it runs', async () => {
    const req = request(DISCO[6], { retrieveLevel: undefined });
    const [row] = await queue(req);
    expect(row.retrieveLevel, 'the idle PACS is series level').toBe('SERIES');

    const updated = await admin.rawUpdatePacs(idle.id, { ...idle, retrieveLevel: 'STUDY' });
    expect(updated.ok()).toBeTruthy();
    expect((await admin.getPacs(idle.id)).retrieveLevel, 'precondition: the PACS is now study level').toBe(
        'STUDY',
    );
    await admin.openAllWeek(idle.id);

    const [outcome] = await waitForOutcome(admin, req.requestId!);
    expect(outcome.status, `history: ${outcome.errorMessage ?? ''}`).toBe('RECEIVED');
    expect(outcome.retrieveLevel).toBe('SERIES');
    expect(outcome.seriesIds.length, 'it moved series, as queued').toBeGreaterThan(0);
});

test('history records the patient the request was for', async () => {
    const req = request(DISCO[7]);
    const [row] = await queue(req);
    expect(row.patientId, 'the queued request carries the patient ID').toBe(DISCO[7].patientId);

    await admin.openAllWeek(idle.id);
    const [outcome] = await waitForOutcome(admin, req.requestId!);
    expect(outcome.patientName).toBe(DISCO[7].patientName);
    expect(outcome.patientId, 'history keeps the patient ID as well as the name').toBe(DISCO[7].patientId);
});

test('a retrieve to a destination the PACS does not know fails once, with a reason', async () => {
    // The PACS refuses a move to an AE title it does not know. That cannot
    // succeed on a second try, and the history should say what went wrong.
    await settings.apply(FAST_FAILURE);
    await admin.openAllWeek(idle.id);
    const req = request(DISCO[7], { aeTitle: 'E2ENOSUCHAE' });
    await queue(req);

    const outcomes = await waitForOutcome(admin, req.requestId!, { timeoutMs: 180_000 });
    expect(outcomes.every(o => o.status === 'FAILED')).toBeTruthy();
    for (const o of outcomes) {
        expect.soft(o.errorMessage ?? '', `history row ${o.id} should say why it failed`).not.toBe('');
    }
    expect.soft(outcomes, 'a permanent refusal is not retried').toHaveLength(1);
});

test('a maximum of zero attempts does not silently skip the retrieve', async () => {
    // Either fix is acceptable: refuse 0 when the setting is saved, or still
    // try the request. What must not happen is a saved 0 that then fails every
    // request without a retrieve ever being attempted.
    const original = (await admin.getSettings()).dqrMaxPacsRequestAttempts;
    const res = await admin.rawSetSettings({ dqrMaxPacsRequestAttempts: '0' });
    const saved = String((await admin.getSettings()).dqrMaxPacsRequestAttempts);
    if (saved !== '0') {
        expect(res.status(), 'a refused 0 is refused as a client error').toBeGreaterThanOrEqual(400);
        expect(res.status()).toBeLessThan(500);
        expect(saved, 'a refused 0 leaves the setting as it was').toBe(String(original));
        return;
    }
    // Recorded so afterEach puts the original back.
    await settings.apply({ dqrMaxPacsRequestAttempts: '0' });
    expect(res.ok(), 'the setting was saved').toBeTruthy();

    await admin.openAllWeek(idle.id);
    const req = request(DISCO[7]);
    await queue(req);
    const [outcome] = await waitForOutcome(admin, req.requestId!);
    expect(
        outcome.status,
        `0 was accepted, so the request must still be tried: ${outcome.errorMessage ?? ''}`,
    ).toBe('RECEIVED');
});
