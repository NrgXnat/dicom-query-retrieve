/**
 * Who may do what: the two access gates, project permissions, admin-only
 * calls, and other people's requests.
 *
 * The gates are site-wide settings that are off on many stacks, so every test
 * that depends on one switches it on and the spec puts both back afterwards.
 * Imports here are queued on a PACS made for the spec with no availability
 * window, so a request that is wrongly accepted is recorded but never runs,
 * and is removed afterwards.
 *
 * Accounts (created by the preflight if missing):
 *   DQR_ROLE_USER  DQR Access role, member of the project
 *   NO_DQR_USER    no role, member of the project
 *   READONLY_USER  DQR Access role, collaborator (read only) on the project
 */
import { request } from '@playwright/test';
import { test, expect } from '../../lib/fixtures';
import { ImportRequest, Pacs, XnatClient } from '../../lib/dqr';
import {
    ADMIN,
    DQR_ROLE_USER,
    NO_DQR_USER,
    PACS_LABEL,
    READONLY_USER,
    RECEIVER_AE,
    RECEIVER_PORT,
    XNAT_URL,
    projectNameFor,
    uniqueLabel,
} from '../../lib/env';
import { DISCO } from '../../lib/disco';
import { generateStudy } from '../../lib/dicom';
import { GATES_ON, SiteSettings } from '../../lib/stack';

const PROJECT = projectNameFor('perm');
const OTHER = projectNameFor('permx');

let admin: XnatClient;
let dqrUser: XnatClient;
let noRole: XnatClient;
let readOnly: XnatClient;
let pacs: Pacs;
let idle: Pacs;
let settings: SiteSettings;
let sessionId: string;

function importTo(projectId: string): ImportRequest {
    return {
        pacsId: idle.id,
        aeTitle: RECEIVER_AE,
        port: RECEIVER_PORT,
        projectId,
        forceImport: true,
        retrieveLevel: 'STUDY',
        requestId: uniqueLabel('e2ep'),
        studies: [{ studyInstanceUid: DISCO[1].studyInstanceUid }],
    };
}

async function clearQueue(): Promise<void> {
    const ours = (await admin.queue(true)).filter(r => r.pacsId === idle.id).map(r => r.id);
    await admin.removeFromQueueQuietly(ours);
}

test.beforeAll(async () => {
    admin = await XnatClient.as(ADMIN);
    dqrUser = await XnatClient.as(DQR_ROLE_USER);
    noRole = await XnatClient.as(NO_DQR_USER);
    readOnly = await XnatClient.as(READONLY_USER);
    pacs = await admin.pacsByLabel(PACS_LABEL);
    settings = new SiteSettings(admin);

    await admin.ensureProject(PROJECT);
    await admin.ensureProject(OTHER);
    await admin.addProjectUser(PROJECT, DQR_ROLE_USER.username, 'Members');
    await admin.addProjectUser(PROJECT, NO_DQR_USER.username, 'Members');
    await admin.addProjectUser(PROJECT, READONLY_USER.username, 'Collaborators');
    expect((await admin.rawSetProjectEnabled(PROJECT, true)).status()).toBeLessThan(400);

    idle = await admin.createPacs({
        aeTitle: 'ORTHANC',
        label: uniqueLabel('E2E_PERM_IDLE'),
        host: '127.0.0.1',
        queryRetrievePort: pacs.queryRetrievePort,
        queryable: true,
        storable: true,
        dicomWebEnabled: false,
    });

    const study = generateStudy(`P${Date.now().toString(36).toUpperCase()}`, 1);
    sessionId = await admin.uploadStudy(PROJECT, uniqueLabel('PermSubj'), uniqueLabel('PermSess'), study);
});

test.afterEach(async () => {
    await clearQueue();
    await settings.restore();
});

test.afterAll(async () => {
    await clearQueue();
    await admin.deletePacsQuietly(idle.id);
    await admin.deleteProjectSettingsQuietly(PROJECT);
    await admin.deleteProjectQuietly(PROJECT);
    await admin.deleteProjectQuietly(OTHER);
    for (const c of [dqrUser, noRole, readOnly, admin]) await c.dispose();
});

// ------------------------------------------------------------ the user gate

test('with the gates on, a DQR user who is a project member can search, import, and see their request', async () => {
    await settings.apply(GATES_ON);

    expect(
        (
            await dqrUser.rawQueryStudies({ pacsId: pacs.id, accessionNumber: DISCO[1].accessionNumber })
        ).status(),
    ).toBe(200);
    const res = await dqrUser.rawImport(importTo(PROJECT));
    expect(res.ok(), `import: HTTP ${res.status()} ${(await res.text()).slice(0, 160)}`).toBeTruthy();
    const [row] = await res.json();

    expect((await dqrUser.queue()).map(r => r.id)).toContain(row.id);
    expect(
        (await dqrUser.delete(`/xapi/dqr/import/queue/${row.id}`)).ok(),
        'they can remove their own request',
    ).toBeTruthy();
});

test('with the user gate on, a user without the DQR Access role cannot search or import', async () => {
    await settings.apply(GATES_ON);
    expect((await noRole.rawQueryStudies({ pacsId: pacs.id })).status()).toBe(403);
    expect((await noRole.rawImport(importTo(PROJECT))).status()).toBe(403);
});

test('with the user gate off, the same user can search', async () => {
    await settings.apply({ allowAllUsersToUseDqr: true });
    expect(
        (
            await noRole.rawQueryStudies({ pacsId: pacs.id, accessionNumber: DISCO[1].accessionNumber })
        ).status(),
    ).toBe(200);
});

// --------------------------------------------------------- project permission

test('a read-only collaborator can search but not import into the project', async () => {
    await settings.apply(GATES_ON);
    expect(
        (
            await readOnly.rawQueryStudies({ pacsId: pacs.id, accessionNumber: DISCO[1].accessionNumber })
        ).status(),
    ).toBe(200);
    expect((await readOnly.rawImport(importTo(PROJECT))).status()).toBe(403);
});

test('a DQR user cannot import into a project they have no access to', async () => {
    await settings.apply({ allowAllUsersToUseDqr: true, allowAllProjectsToUseDqr: true });
    expect((await dqrUser.rawImport(importTo(OTHER))).status()).toBe(403);
});

test('with the project gate on, an import into a project not switched on is refused', async () => {
    await settings.apply(GATES_ON);
    await admin.deleteProjectSettingsQuietly(OTHER);
    await admin.addProjectUser(OTHER, DQR_ROLE_USER.username, 'Members');

    const res = await dqrUser.rawImport(importTo(OTHER));
    expect(res.ok(), 'the project has not been switched on for DQR').toBeFalsy();
    expect(res.status(), 'refused as forbidden, not as a missing resource').toBe(403);
});

test("a read-only collaborator cannot send the project's data to a PACS", async () => {
    // The Send to PACS menu needs edit access and an enabled project. The API
    // behind it should ask for no less, since sending data out of XNAT is at
    // least as sensitive as bringing it in.
    await settings.apply(GATES_ON);
    const res = await readOnly.rawExport(idle.id, sessionId, ['1']);
    expect(res.status(), `answered ${res.status()} ${(await res.text()).slice(0, 120)}`).toBe(403);
});

// ------------------------------------------------------------- admin only

test('a non-admin cannot add, change or remove a PACS, or its schedule', async () => {
    const pacsBody = {
        aeTitle: 'E2ENOTALLOWED',
        label: uniqueLabel('E2E_NOTALLOWED'),
        host: '127.0.0.1',
        queryRetrievePort: 4242,
        queryable: true,
        dicomWebEnabled: false,
    };
    expect((await dqrUser.rawCreatePacs(pacsBody)).status()).toBe(403);
    expect((await dqrUser.rawUpdatePacs(idle.id, { ...idle, label: 'E2E_HIJACKED' })).status()).toBe(403);
    expect((await dqrUser.delete(`/xapi/pacs/${idle.id}`)).status()).toBe(403);
    expect(
        (
            await dqrUser.rawCreateAvailability(idle.id, {
                dayOfWeek: 'MONDAY',
                availabilityStart: '01:00',
                availabilityEnd: '02:00',
                threads: 1,
                utilizationPercent: 10,
            })
        ).status(),
    ).toBe(403);
    expect((await admin.getPacs(idle.id)).label, 'the PACS is unchanged').toBe(idle.label);
});

test('a non-admin can list PACS but not read or change the site settings', async () => {
    expect((await dqrUser.get('/xapi/pacs')).status()).toBe(200);
    expect((await dqrUser.get('/xapi/dqr/settings')).status()).toBe(403);
    expect((await dqrUser.post('/xapi/dqr/settings', { dqrMaxPacsRequestAttempts: '9' })).status()).toBe(403);
    expect(String((await admin.getSettings()).dqrMaxPacsRequestAttempts)).not.toBe('9');
});

test('a non-admin cannot switch a project on or off for DQR', async () => {
    expect((await dqrUser.rawSetProjectEnabled(PROJECT, false)).status()).toBe(403);
});

// --------------------------------------------------------- other people's rows

test("a non-admin cannot list everyone's queue or history", async () => {
    expect((await dqrUser.get('/xapi/dqr/import/queue', { all: true })).status()).toBe(403);
    expect((await dqrUser.get('/xapi/dqr/import/history', { all: true })).status()).toBe(403);
    expect((await dqrUser.get('/xapi/dqr/import/queue/count', { all: true })).status()).toBe(403);
});

test("a user cannot see or remove someone else's queued request", async () => {
    const [row] = await admin.queueImport(importTo(PROJECT));

    expect((await dqrUser.queue()).map(r => r.id)).not.toContain(row.id);
    const read = await dqrUser.get(`/xapi/dqr/import/queue/${row.id}`);
    expect(read.ok(), `reading another user's request answered ${read.status()}`).toBeFalsy();
    expect((await dqrUser.delete(`/xapi/dqr/import/queue/${row.id}`)).status()).toBe(403);
    expect(
        (await admin.queue(true)).map(r => r.id),
        'the request is still queued',
    ).toContain(row.id);
});

test('an anonymous caller cannot search', async () => {
    const anon = await request.newContext({ baseURL: XNAT_URL, ignoreHTTPSErrors: true });
    try {
        const res = await anon.post('/xapi/dqr/query/studies', { data: { pacsId: pacs.id } });
        expect(res.status()).toBe(401);
    } finally {
        await anon.dispose();
    }
});
