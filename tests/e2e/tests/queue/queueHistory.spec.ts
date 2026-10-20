/**
 * The import queue and history: what waits, what runs, what is recorded, and
 * what happens when a retrieve fails.
 *
 * Requests that must stay queued are sent to a PACS made for the spec with no
 * availability window: the same Orthanc, registered again through 127.0.0.1,
 * so nothing it queues runs until a test opens a window for it. Retrieves that
 * should run use the fixture PACS with the scheduler set to ten seconds.
 *
 * Every request relabels its study, because DQR applies a study's last stored
 * relabels to any later import that has none (see neutralizeRelabels), and a
 * test here must not depend on what an earlier test or user left behind.
 */
import { test, expect } from '../../lib/fixtures';
import { ImportRequest, Pacs, PacsRequestRow, XnatClient } from '../../lib/dqr';
import { ADMIN, PACS_LABEL, RECEIVER_AE, RECEIVER_PORT, projectNameFor, uniqueLabel } from '../../lib/env';
import { DISCO, DiscoStudy } from '../../lib/disco';
import { FAST_FAILURE, FAST_QUEUE, SiteSettings, neutralizeRelabels, waitForOutcome } from '../../lib/stack';

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
    await admin.ensureProject(PROJECT);
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
    await admin.removeFromQueueQuietly(queuedIds.splice(0));
    await admin.deletePacsQuietly(idle.id);
    await settings.restore();
    await settings.apply(FAST_QUEUE);
    await admin.deleteProjectQuietly(PROJECT);
});

test.afterAll(async () => {
    await neutralizeRelabels(admin, pacs.id, [...relabeled]);
    await admin.clearUnassignedPrearchive(madeLabels);
    await settings.restore();
    await admin.dispose();
});

test('a queued request can be listed, counted and read back by id', async () => {
    const [row] = await queue(request(DISCO[1]));

    const mine = await admin.queue();
    expect(mine.map(r => r.id)).toContain(row.id);
    const count = await (await admin.get('/xapi/dqr/import/queue/count')).json();
    expect(count).toBeGreaterThanOrEqual(1);

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
    const rows = await queue({
        ...request(DISCO[1]),
        studies: [DISCO[1], DISCO[2], DISCO[3]].map(s => labeled(s)),
    });
    const ids = rows.map(r => r.id);

    const page = async (n: number, sort: string) =>
        (await (
            await admin.get('/xapi/dqr/import/queue', { page: n, pageSize: 1, sort })
        ).json()) as PacsRequestRow[];
    const desc = [await page(1, 'desc'), await page(2, 'desc')].flat().map(r => r.id);
    expect(desc, 'one row per page, newest first').toHaveLength(2);
    expect(new Set(desc).size).toBe(2);

    const asc = (await (
        await admin.get('/xapi/dqr/import/queue', { sort: 'asc', pageSize: 1000 })
    ).json()) as PacsRequestRow[];
    const ours = asc.filter(r => ids.includes(r.id)).map(r => r.queuedTime);
    expect(ours, 'ascending by queue time').toEqual([...ours].sort((a, b) => a - b));
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
    // The setting is accepted, so a request should still be tried at least
    // once. What must not happen is the request being marked done with no
    // retrieve attempted.
    await settings.apply({ dqrMaxPacsRequestAttempts: '0' });

    await admin.openAllWeek(idle.id);
    const req = request(DISCO[7]);
    await queue(req);
    const [outcome] = await waitForOutcome(admin, req.requestId!);
    expect(outcome.status, `with zero attempts: ${outcome.errorMessage ?? ''}`).toBe('RECEIVED');
});
