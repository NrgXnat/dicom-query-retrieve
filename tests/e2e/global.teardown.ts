/**
 * Runs once, after every test. Fails the run if the suite left the stack
 * different from how the preflight found it: DQR settings, the PACS list, the
 * fixture PACS and its schedule, the receivers, and anything the suite makes
 * (test projects, their DQR switch rows, queue rows and prearchive entries).
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
import { StackSnapshot, compareSnapshots, takeSnapshot } from './lib/stackSnapshot';

export default async function globalTeardown(): Promise<void> {
    const file = path.resolve(__dirname, '.auth/stack-at-start.json');
    if (!fs.existsSync(file)) return;
    const before: StackSnapshot = JSON.parse(fs.readFileSync(file, 'utf-8'));

    const admin = await request.newContext({
        baseURL: env.XNAT_URL,
        ignoreHTTPSErrors: true,
        httpCredentials: { username: env.ADMIN.username, password: env.ADMIN.password, send: 'always' },
    });
    const after = await takeSnapshot(admin, env.PACS_LABEL);
    await admin.dispose();

    const problems = compareSnapshots(before, after);
    if (problems.length) {
        throw new Error(`[DQR-E2E] The run left the stack changed:\n  - ${problems.join('\n  - ')}`);
    }
    console.log(
        '[DQR-E2E] The stack is as the run found it (settings, PACS, schedule, receivers, test projects, switch rows, queue, prearchive).',
    );
}
