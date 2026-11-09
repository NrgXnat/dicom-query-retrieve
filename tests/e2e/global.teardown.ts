/**
 * Runs once, after every test. Fails the run if the suite left the stack's
 * DQR settings or PACS list different from how the preflight found them.
 *
 * Each spec restores what it changes, but a spec that dies partway through
 * its cleanup could leave, say, the scheduler at ten seconds or a gate
 * switched the wrong way, and the next run would then record that as normal.
 * This check makes such a leftover a failure of the run that caused it.
 */
import { request } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import * as env from './lib/env';

export default async function globalTeardown(): Promise<void> {
    const file = path.resolve(__dirname, '.auth/stack-at-start.json');
    if (!fs.existsSync(file)) return;
    const before = JSON.parse(fs.readFileSync(file, 'utf-8'));

    const admin = await request.newContext({
        baseURL: env.XNAT_URL,
        ignoreHTTPSErrors: true,
        httpCredentials: { username: env.ADMIN.username, password: env.ADMIN.password, send: 'always' },
    });
    const settings = await (await admin.get('/xapi/dqr/settings')).json();
    const pacs = ((await (await admin.get('/xapi/pacs')).json()) as any[]).map(p => p.label).sort();
    await admin.dispose();

    const problems: string[] = [];
    for (const k of new Set([...Object.keys(before.settings), ...Object.keys(settings)])) {
        if (String(before.settings[k]) !== String(settings[k])) {
            problems.push(`setting ${k} was ${before.settings[k]}, is now ${settings[k]}`);
        }
    }
    if (JSON.stringify(before.pacs) !== JSON.stringify(pacs)) {
        problems.push(`PACS list was ${JSON.stringify(before.pacs)}, is now ${JSON.stringify(pacs)}`);
    }
    if (problems.length) {
        throw new Error(`[DQR-E2E] The run left the stack changed:\n  - ${problems.join('\n  - ')}`);
    }
    console.log('[DQR-E2E] Stack settings and PACS list are as the run found them.');
}
