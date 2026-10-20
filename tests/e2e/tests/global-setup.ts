/**
 * Browser logins for the pages. The stack preflight runs earlier, in
 * global.setup.ts, before any test.
 */
import { test as setup, expect } from '@playwright/test';
import * as fs from 'fs';
import { login } from '../lib/auth';
import { ADMIN } from '../lib/env';

setup('authenticate as admin', async ({ page }) => {
    await login(page, ADMIN.username, ADMIN.password, 'admin-csrf.txt');
    await page.context().storageState({ path: '.auth/admin.json' });
});

setup('report what the preflight measured', async () => {
    const capabilities = JSON.parse(fs.readFileSync('.auth/capabilities.json', 'utf-8'));
    console.log(
        `[DQR-E2E] plugin ${capabilities.version}, PACS id ${capabilities.pacsId}, ` +
            `TARGET_BRANCH=${capabilities.targetBranch}.`,
    );
    expect(capabilities.version, 'the preflight did not record a plugin version').toBeTruthy();
});
