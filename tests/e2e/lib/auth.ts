/**
 * XNAT login helper.
 *
 * Authenticates through the standard login form rather than basic auth, so the
 * session carries the CSRF token XNAT requires on every state-changing
 * request. The token is written next to the storage state so the REST helpers
 * in api.ts can reuse it without logging in again.
 */
import { Page, expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

const AUTH_DIR = path.resolve(__dirname, '..', '.auth');

export async function login(page: Page, username: string, password: string, csrfFile: string): Promise<void> {
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    await page.locator('#username').fill(username);
    await page.locator('#password').fill(password);

    // Register the URL wait before the click. If the click resolves after a
    // fast navigation has already completed, a wait installed afterwards would
    // never observe the transition and would time out.
    await Promise.all([
        page.waitForURL(url => !url.toString().includes('Login.vm'), { timeout: 30_000 }),
        page
            .locator('#login_form')
            .locator('button[type="submit"], input[type="submit"], #submit_login')
            .click(),
    ]);
    await page.waitForLoadState('domcontentloaded');

    const csrfToken = await page.evaluate(() => (window as any).csrfToken);
    expect(csrfToken, `Could not extract CSRF token after logging in as ${username}`).toBeTruthy();

    fs.mkdirSync(AUTH_DIR, { recursive: true });
    fs.writeFileSync(path.join(AUTH_DIR, csrfFile), csrfToken, 'utf-8');
}

/** Read a CSRF token saved by a previous login. */
export function readCsrfToken(csrfFile: string): string {
    return fs.readFileSync(path.join(AUTH_DIR, csrfFile), 'utf-8').trim();
}
