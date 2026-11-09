/**
 * The admin DQR Settings tab and the Send to PACS page, driven in a real
 * browser as an admin.
 *
 * A PACS added through the admin dialog is registered as 127.0.0.1 with a
 * unique AE title, so it can never be confused with the fixture PACS, and is
 * deleted afterwards. The session sent to the PACS is generated for the run
 * (lib/dicom.ts), so nothing sent collides with the fixture studies.
 */
import { Page } from '@playwright/test';
import { test, expect } from '../../lib/fixtures';
import { Pacs, XnatClient } from '../../lib/dqr';
import { ADMIN, PACS_LABEL, projectNameFor, uniqueLabel } from '../../lib/env';
import { GeneratedStudy, generateStudy } from '../../lib/dicom';

const PROJECT = projectNameFor('uia');

let admin: XnatClient;
let pacs: Pacs;
let study: GeneratedStudy;
let sessionId: string;
const addedLabels: string[] = [];

test.beforeAll(async () => {
    admin = await XnatClient.as(ADMIN);
    pacs = await admin.pacsByLabel(PACS_LABEL);
    await admin.ensureDqrProject(PROJECT);
    study = generateStudy(`U${Date.now().toString(36).toUpperCase()}`, 2);
    sessionId = await admin.uploadStudy(PROJECT, uniqueLabel('UiSendSubj'), uniqueLabel('UiSendSess'), study);
});

test.afterAll(async () => {
    for (const p of await admin.listPacs())
        if (addedLabels.includes(p.label)) await admin.deletePacsQuietly(p.id);
    await admin.deleteProjectQuietly(PROJECT);
    await admin.dispose();
});

async function openDqrSettings(page: Page): Promise<void> {
    await page.goto('/app/template/Page.vm?view=admin/plugins');
    await page.getByText('DQR Settings', { exact: true }).first().click();
    await expect(page.locator('#pacsTable')).toBeVisible({ timeout: 30_000 });
}

/** The table is built by script without a tbody; each PACS row carries its id. */
function pacsRow(page: Page, label: string) {
    return page.locator('#pacsTable tr[data-id]').filter({ hasText: label });
}

test('the DQR Settings tab lists the fixture PACS as a DIMSE PACS', async ({ page }) => {
    await openDqrSettings(page);
    const row = pacsRow(page, PACS_LABEL);
    await expect(row).toHaveCount(1);
    await expect(row).toContainText('DIMSE');
    await expect(row).toContainText(String(pacs.queryRetrievePort));
});

test('Ping reports the fixture PACS as up', async ({ page }) => {
    await openDqrSettings(page);
    await pacsRow(page, PACS_LABEL).locator('button.ping-pacs').click();
    await expect(page.getByText('PACS Status: Up')).toBeVisible({ timeout: 30_000 });
});

test('a PACS added through the dialog is saved, and gets an all-week schedule', async ({ page }) => {
    const ae = `E2EUI${Date.now().toString(36).toUpperCase()}`.slice(0, 16);
    const label = `E2E_${ae}`;
    addedLabels.push(label);

    await openDqrSettings(page);
    await page.locator('#addNewPacs').click();
    const dialog = page.locator('.xnat-dialog').filter({ hasText: 'Add New DICOM AE Connection' }).last();
    await expect(dialog).toBeVisible();
    await dialog.locator('#aeTitle').fill(ae);
    await dialog.locator('#connection_type').selectOption('dimse');
    await dialog.locator('input[name="label"]').fill(label);
    await dialog.locator('input[name="host"]').fill('127.0.0.1');
    await dialog.locator('input[name="queryRetrievePort"]').fill('4242');
    await dialog.getByRole('button', { name: 'Save' }).click();

    await expect(page.getByText('Created new DICOM AE connection')).toBeVisible({ timeout: 30_000 });
    await expect(pacsRow(page, label)).toHaveCount(1);

    const saved = (await admin.listPacs()).find(p => p.label === label);
    expect(saved, 'the PACS is saved').toBeDefined();
    expect(saved!.host).toBe('127.0.0.1');
    const windows = await admin.getAvailability(saved!.id);
    for (const day of ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']) {
        expect.soft(windows[day]?.length, `${day} has a window`).toBe(1);
    }
});

test('the Add dialog refuses to save without a connection type', async ({ page }) => {
    await openDqrSettings(page);
    await page.locator('#addNewPacs').click();
    const dialog = page.locator('.xnat-dialog').filter({ hasText: 'Add New DICOM AE Connection' }).last();
    await dialog.locator('#aeTitle').fill('E2ENOTYPE');
    await dialog.getByRole('button', { name: 'Save' }).click();
    await expect(page.getByText('You must select a connection type.')).toBeVisible();
    expect((await admin.listPacs()).map(p => p.aeTitle)).not.toContain('E2ENOTYPE');
});

test("Send to PACS lists the session's scans and sends the chosen one", async ({ page }) => {
    await page.goto(`/app/template/ExportSessionToPacs.vm?sessionId=${sessionId}&project=${PROJECT}`);
    await expect(page.locator('#pacsId')).toHaveValue(String(pacs.id));
    const boxes = page.locator('#scansToExport input[name="scansToExport"]');
    await expect(boxes).toHaveCount(2);

    await page.locator('#scan-1').check();
    await page.locator('#submitScansToPacs').click();
    await expect(
        page.getByText('The request to store your session to the requested PACS has been sent.'),
    ).toBeVisible({
        timeout: 30_000,
    });

    await expect
        .poll(
            async () =>
                (await admin.querySeries(pacs.id, study.studyInstanceUid)).map(s => s.seriesInstanceUid),
            {
                timeout: 120_000,
                intervals: [5_000],
            },
        )
        .toEqual([study.series[0].seriesInstanceUid]);
});

test('Send to PACS with no scan chosen asks the user to choose one', async ({ page }) => {
    await page.goto(`/app/template/ExportSessionToPacs.vm?sessionId=${sessionId}&project=${PROJECT}`);
    await page.locator('#submitScansToPacs').click();
    await expect(page.getByText('Please select a valid scan to send.')).toBeVisible();
});
