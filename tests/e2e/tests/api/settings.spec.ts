/**
 * Site DQR settings and the per-project switch.
 *
 * The settings form writes every value as JSON to one endpoint, so a setting
 * that does not round trip is one an admin cannot actually change. The project
 * switch decides whether a project accepts imports when the project gate is
 * on. Every change here is put back afterwards and checked.
 */
import { test, expect } from '../../lib/fixtures';
import { XnatClient } from '../../lib/dqr';
import { ADMIN, projectNameFor } from '../../lib/env';
import { SiteSettings } from '../../lib/stack';

let admin: XnatClient;
let settings: SiteSettings;
const PROJECT = projectNameFor('set');

test.beforeAll(async () => {
    admin = await XnatClient.as(ADMIN);
    settings = new SiteSettings(admin);
    await admin.ensureProject(PROJECT);
});

test.afterEach(async () => {
    await settings.restore();
});

test.afterAll(async () => {
    await admin.deleteProjectSettingsQuietly(PROJECT);
    await admin.deleteProjectQuietly(PROJECT);
    await admin.dispose();
});

/** A value for each setting on the admin form, different from its default. */
const FORM_VALUES: Record<string, string | boolean> = {
    dqrCallingAe: 'XNATE2E',
    pacsAvailabilityCheckFrequency: '2 minutes',
    dqrMaxPacsRequestAttempts: '3',
    dqrWaitToRetryRequestInSeconds: '120',
    dqrMaxThrottleSleepSeconds: '900',
    dimseConnectTimeoutSeconds: '31',
    dimseAcceptTimeoutSeconds: '61',
    dimseResponseTimeoutSeconds: '62',
    dimseRetrieveTimeoutSeconds: '120',
    dicomWebMetadataReadTimeoutSeconds: '21',
    dicomWebRetrieveReadTimeoutSeconds: '22',
    notifyAdminOnImport: true,
    assumeSameSessionIfArrivedWithin: '45 minutes',
};

test('every setting on the admin form saves and reads back', async () => {
    const before = await admin.getSettings();
    for (const k of Object.keys(FORM_VALUES)) expect(before, `setting ${k} is reported`).toHaveProperty(k);

    // SiteSettings.apply() reads every value back and fails on any that did not take.
    await settings.apply(FORM_VALUES);
});

test('both access gates can be switched on and off', async () => {
    for (const value of [false, true]) {
        await settings.apply({ allowAllUsersToUseDqr: value, allowAllProjectsToUseDqr: value });
        const now = await admin.getSettings();
        expect([now.allowAllUsersToUseDqr, now.allowAllProjectsToUseDqr]).toEqual([value, value]);
    }
});

test('with the project gate off, every project is enabled even with no switch set', async () => {
    await settings.apply({ allowAllProjectsToUseDqr: true });
    await admin.deleteProjectSettingsQuietly(PROJECT);

    const res = await admin.get(`/xapi/dqr/settings/project/${PROJECT}/enabled`);
    expect(res.ok(), `HTTP ${res.status()}`).toBeTruthy();
    expect(await res.json()).toBe(true);
});

test('with the project gate on, a project nobody has switched on reads as not enabled', async () => {
    // The answer for a project with no switch set should be "no", the same as
    // a project switched off, not an error.
    await settings.apply({ allowAllProjectsToUseDqr: false });
    await admin.deleteProjectSettingsQuietly(PROJECT);

    const res = await admin.get(`/xapi/dqr/settings/project/${PROJECT}/enabled`);
    expect(res.status(), 'a project with no switch set is a normal answer, not a missing resource').toBe(200);
    expect(await res.json()).toBe(false);
});

test('the project switch turns a project on and off when the project gate is on', async () => {
    await settings.apply({ allowAllProjectsToUseDqr: false });

    const on = await admin.rawSetProjectEnabled(PROJECT, true);
    expect(on.ok(), `switch on: HTTP ${on.status()}`).toBeTruthy();
    expect(await (await admin.get(`/xapi/dqr/settings/project/${PROJECT}/enabled`)).json()).toBe(true);

    const listed = await (await admin.get('/xapi/dqr/settings/project')).json();
    expect(listed.find((r: any) => r.projectId === PROJECT)?.dqrEnabled, 'the switch shows in the list').toBe(
        true,
    );

    const off = await admin.rawSetProjectEnabled(PROJECT, false);
    expect(off.ok(), `switch off: HTTP ${off.status()}`).toBeTruthy();
    expect(await (await admin.get(`/xapi/dqr/settings/project/${PROJECT}/enabled`)).json()).toBe(false);
});

test('setting the project switch to the value it already has answers 304 Not Modified', async () => {
    await admin.rawSetProjectEnabled(PROJECT, true);
    const again = await admin.rawSetProjectEnabled(PROJECT, true);
    expect(again.status()).toBe(304);
});

test('removing the project switch deletes it', async () => {
    await admin.rawSetProjectEnabled(PROJECT, true);
    const del = await admin.delete(`/xapi/dqr/settings/project/${PROJECT}`);
    expect(del.ok(), `delete: HTTP ${del.status()}`).toBeTruthy();
    expect((await admin.get(`/xapi/dqr/settings/project/${PROJECT}`)).status()).toBe(404);
});

test('the project switch cannot be set for a project that does not exist', async () => {
    const res = await admin.rawSetProjectEnabled('NO_SUCH_PROJECT_E2E', true);
    expect(res.status()).toBe(404);
});
