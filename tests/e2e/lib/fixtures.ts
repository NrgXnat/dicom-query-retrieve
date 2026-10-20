/**
 * The suite's `test` object.
 *
 * Extends Playwright's with automatic diagnostic capture, so a failure carries
 * the evidence needed to understand it without anyone re-running the suite
 * with extra flags. Everything here attaches only on failure, so a green run
 * stays quiet.
 *
 * Captured per test:
 *
 *   - browser console output, including errors thrown on the page
 *   - page errors (uncaught exceptions), which otherwise vanish silently
 *   - failed or non-2xx network responses, with status and URL
 *
 * Playwright already records a trace and a screenshot. What it does not record
 * is what the PAGE said while it was going wrong, and for a plugin whose UI is
 * driven by jQuery handlers that is usually the deciding evidence.
 */
import { test as base, expect } from '@playwright/test';

export const test = base.extend<{ diagnostics: void }>({
    diagnostics: [
        async ({ page }, use, testInfo) => {
            const consoleLines: string[] = [];
            const pageErrors: string[] = [];
            const badResponses: string[] = [];

            page.on('console', m => consoleLines.push(`[${m.type()}] ${m.text()}`));
            page.on('pageerror', e => pageErrors.push(String(e)));
            page.on('response', r => {
                if (r.status() >= 400) badResponses.push(`${r.status()} ${r.request().method()} ${r.url()}`);
            });

            await use();

            if (testInfo.status === testInfo.expectedStatus) return;

            const attach = async (name: string, lines: string[]) => {
                if (lines.length === 0) return;
                await testInfo.attach(name, { body: lines.join('\n'), contentType: 'text/plain' });
            };
            await attach('browser-console.txt', consoleLines);
            await attach('page-errors.txt', pageErrors);
            await attach('failed-responses.txt', badResponses);
        },
        { auto: true },
    ],
});

export { expect };
