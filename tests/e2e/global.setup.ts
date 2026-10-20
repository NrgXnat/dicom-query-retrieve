/**
 * Preflight. Runs once, before any test, and fails the run if the stack
 * cannot support it.
 *
 * Every check here is something that, if wrong, would make a large share of
 * the suite fail for a reason unrelated to the test that failed: the plugin
 * missing, the PACS unreachable, the receiver misconfigured, a fixture study
 * gone. Failing once, here, with the actual reason, is better than a hundred
 * red tests that each have to be read to find it.
 *
 * It also creates the test accounts if they do not exist, so a fresh stack
 * needs only the admin credentials and the passwords in .env.
 */
import { request } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
// playwright.config.ts loads .env before this module is required.
import * as env from './lib/env';
import { ALL_DISCO } from './lib/disco';

const AUTH_DIR = path.resolve(__dirname, '.auth');

export default async function globalSetup(): Promise<void> {
    fs.mkdirSync(AUTH_DIR, { recursive: true });
    const problems: string[] = [];

    const admin = await request.newContext({
        baseURL: env.XNAT_URL,
        ignoreHTTPSErrors: true,
        httpCredentials: { username: env.ADMIN.username, password: env.ADMIN.password, send: 'always' },
    });

    const pluginRes = await admin.get('/xapi/plugins/dicom-query-retrieve');
    if (!pluginRes.ok()) {
        await admin.dispose();
        throw new Error(
            `[DQR-E2E] The dicom-query-retrieve plugin is not installed on ${env.XNAT_URL} ` +
                `(GET /xapi/plugins/dicom-query-retrieve answered ${pluginRes.status()}).`,
        );
    }
    const version: string = (await pluginRes.json()).version;

    // The PACS: configured, answering, and with a window open so queued
    // imports actually run.
    const pacsList: any[] = await (await admin.get('/xapi/pacs')).json();
    const pacs = pacsList.find(p => p.label === env.PACS_LABEL);
    if (!pacs) {
        problems.push(
            `no PACS labeled "${env.PACS_LABEL}" (have: ${pacsList.map(p => p.label).join(', ') || 'none'})`,
        );
    } else {
        const ping = await (await admin.get(`/xapi/pacs/${pacs.id}/status`)).json();
        if (!ping.successful) problems.push(`PACS ${env.PACS_LABEL} does not answer a ping`);
        const windows = await (await admin.get(`/xapi/pacs/${pacs.id}/availability`)).json();
        const openNow = Object.values(windows as Record<string, any[]>)
            .flat()
            .some(w => w.availableNow);
        if (!openNow)
            problems.push(`PACS ${env.PACS_LABEL} has no availability window open now, so imports never run`);
    }

    // The receiver the PACS sends to.
    const receivers: any[] = await (await admin.get('/xapi/dicomscp')).json();
    const receiver = receivers.find(r => r.aeTitle === env.RECEIVER_AE && r.port === env.RECEIVER_PORT);
    if (!receiver) {
        problems.push(`no SCP receiver ${env.RECEIVER_AE}:${env.RECEIVER_PORT}`);
    } else {
        if (!receiver.enabled) problems.push(`receiver ${env.RECEIVER_AE} is disabled`);
        if (receiver.identifier !== 'dqrObjectIdentifier') {
            problems.push(
                `receiver ${env.RECEIVER_AE} uses identifier ${receiver.identifier}, not dqrObjectIdentifier`,
            );
        }
        if (!receiver.customProcessing)
            problems.push(`receiver ${env.RECEIVER_AE} has custom processing off`);
    }

    // The fixture studies.
    if (pacs) {
        const res = await admin.post('/xapi/dqr/query/studies', { data: { pacsId: pacs.id } });
        const found: any[] = res.status() === 204 ? [] : await res.json();
        const uids = new Set(found.map(s => s.studyInstanceUid));
        const missing = ALL_DISCO.filter(s => !uids.has(s.studyInstanceUid)).map(s => `Disco Study ${s.n}`);
        if (missing.length) problems.push(`the PACS is missing fixture studies: ${missing.join(', ')}`);
    }

    // Test accounts.
    const accounts = [
        { ...env.DQR_ROLE_USER, role: true },
        { ...env.NO_DQR_USER, role: false },
        { ...env.READONLY_USER, role: true },
    ];
    for (const a of accounts) {
        if (!a.password) {
            problems.push(`no password set for ${a.username} in .env`);
            continue;
        }
        const exists = (await admin.get(`/xapi/users/${a.username}`)).ok();
        if (!exists) {
            const created = await admin.post('/xapi/users', {
                data: {
                    username: a.username,
                    password: a.password,
                    email: `${a.username}@example.com`,
                    firstName: 'E2E',
                    lastName: a.username,
                    enabled: true,
                    verified: true,
                },
            });
            if (!created.ok()) problems.push(`could not create ${a.username}: HTTP ${created.status()}`);
        }
        const roleUrl = `/xapi/users/${a.username}/roles/Dqr`;
        const roleRes = a.role ? await admin.put(roleUrl) : await admin.delete(roleUrl);
        if (!roleRes.ok() && !(roleRes.status() === 404 && !a.role)) {
            problems.push(
                `could not ${a.role ? 'grant' : 'remove'} the Dqr role for ${a.username}: HTTP ${roleRes.status()}`,
            );
        }
        const login = await request.newContext({
            baseURL: env.XNAT_URL,
            ignoreHTTPSErrors: true,
            httpCredentials: { username: a.username, password: a.password, send: 'always' },
        });
        if (!(await login.get('/data/JSESSION')).ok())
            problems.push(`${a.username} cannot log in with the .env password`);
        await login.dispose();
    }

    await admin.dispose();

    if (problems.length) {
        throw new Error(`[DQR-E2E] The stack cannot run this suite:\n  - ${problems.join('\n  - ')}`);
    }

    fs.writeFileSync(
        path.join(AUTH_DIR, 'capabilities.json'),
        JSON.stringify({ version, pacsId: pacs.id, targetBranch: env.TARGET_BRANCH }),
        'utf-8',
    );
    console.log(
        `[DQR-E2E] dicom-query-retrieve ${version}; PACS ${env.PACS_LABEL} (id ${pacs.id}) up; ` +
            `receiver ${env.RECEIVER_AE}:${env.RECEIVER_PORT}; ${ALL_DISCO.length} fixture studies present.`,
    );
}
