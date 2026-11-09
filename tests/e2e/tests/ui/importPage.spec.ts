/**
 * The Import From PACS page, driven in a real browser as an admin.
 *
 * The API specs cover what the server does with a request. This spec covers
 * what the page sends and shows: which PACS and receiver it picks, what a
 * search lists, what the relabel boxes and the series dialog submit, and the
 * messages a user sees. Imports started here are followed to the archived
 * session in the project, not to the "queued" message.
 */
import * as fs from 'fs';
import * as path from 'path';
import { Page } from '@playwright/test';
import { test, expect } from '../../lib/fixtures';
import { Pacs, XnatClient } from '../../lib/dqr';
import { ADMIN, PACS_LABEL, RECEIVER_AE, RECEIVER_PORT, projectNameFor, uniqueLabel } from '../../lib/env';
import { DISCO, DiscoStudy } from '../../lib/disco';
import { FAST_QUEUE, SiteSettings, neutralizeRelabels, waitForSession } from '../../lib/stack';

const PROJECT = projectNameFor('ui');

let admin: XnatClient;
let pacs: Pacs;
let settings: SiteSettings;
const madePacs: number[] = [];
const relabeled = new Set<DiscoStudy>();
const madeLabels: string[] = [];

test.beforeAll(async () => {
    admin = await XnatClient.as(ADMIN);
    pacs = await admin.pacsByLabel(PACS_LABEL);
    settings = new SiteSettings(admin);
    await settings.apply(FAST_QUEUE);
    await admin.ensureDqrProject(PROJECT);
});

test.afterAll(async () => {
    try {
        for (const id of madePacs) await admin.deletePacsQuietly(id);
        await neutralizeRelabels(admin, pacs.id, [...relabeled]);
        await admin.clearUnassignedPrearchive(madeLabels);
        await admin.deleteProjectSettingsQuietly(PROJECT);
        await admin.deleteProjectQuietly(PROJECT);
    } finally {
        await settings.restore();
        await admin.dispose();
    }
});

async function openImportPage(page: Page, project = PROJECT): Promise<void> {
    await page.goto(`/app/template/Page.vm?view=dqr/import&project=${project}`);
    await expect(page.locator('#pacs-status-indicator')).toContainText('Ready to Query', { timeout: 30_000 });
}

async function searchAccession(page: Page, study: DiscoStudy) {
    await page.locator('input[name="accessionNumber"]').fill(study.accessionNumber);
    await page.locator('#submit-pacs-search').click();
    const row = page.locator(`#all-search-results tr[data-uid="${study.studyInstanceUid}"]`);
    await expect(row).toBeVisible({ timeout: 60_000 });
    return row;
}

function resultRows(page: Page) {
    return page.locator('#all-search-results tr[data-uid]');
}

/** The dialog XNAT is showing with this text in it. */
function dialogWith(page: Page, text: string | RegExp) {
    return page.locator('.xnat-dialog').filter({ hasText: text }).last();
}

test('the page opens ready to search, on the default PACS and the DQR receiver, without script errors', async ({
    page,
}) => {
    const errors: string[] = [];
    page.on('pageerror', e => errors.push(String(e)));
    await openImportPage(page);

    await expect(page.locator('#select-pacs')).toHaveValue(String(pacs.id));
    await expect(page.locator('#ae-menu')).toHaveValue(`${RECEIVER_AE}:${RECEIVER_PORT}`);
    await expect(page.locator('#pacs-query-msg')).toBeVisible();
    expect(errors, 'the page should load without JavaScript errors').toEqual([]);
});

test('every searchable PACS is offered in the Source PACS list', async ({ page }) => {
    // A second searchable PACS, registered after the default one.
    const second = await admin.createPacs({
        aeTitle: 'ORTHANC',
        label: uniqueLabel('E2E_UI_SECOND'),
        host: '127.0.0.1',
        queryRetrievePort: pacs.queryRetrievePort,
        queryable: true,
        storable: false,
        dicomWebEnabled: false,
    });
    madePacs.push(second.id);

    await openImportPage(page);
    const offered = await page
        .locator('#select-pacs option')
        .evaluateAll(o => o.map(x => (x as HTMLOptionElement).value));
    const searchable = (await admin.listPacs({ queryable: true })).map(p => String(p.id));
    for (const id of searchable)
        expect.soft(offered, `PACS ${id} is searchable but not offered`).toContain(id);
});

test('a search lists the matching study with its details and empty relabel boxes', async ({ page }) => {
    await openImportPage(page);
    const row = await searchAccession(page, DISCO[2]);

    await expect(resultRows(page)).toHaveCount(1);
    await expect(row.locator('td.accessionNumber')).toHaveText(DISCO[2].accessionNumber);
    await expect(row.locator('td.patientName')).toHaveText(DISCO[2].patientName);
    await expect(row.locator('td.studyDate')).toHaveText(DISCO[2].studyDate);
    await expect(row.locator('td.modalitiesInStudy')).toHaveText(DISCO[2].modality);
    await expect(row.locator('input.relabel-patient-name')).toHaveValue('');
    await expect(row.locator('input.relabel-study-id')).toHaveValue('');
});

test('a search with no matches says so', async ({ page }) => {
    await openImportPage(page);
    await page.locator('input[name="accessionNumber"]').fill('0000000000000');
    await page.locator('#submit-pacs-search').click();
    await expect(page.locator('#pacs-no-results')).toBeVisible({ timeout: 60_000 });
    await expect(resultRows(page)).toHaveCount(0);
});

test("a single-day date search lists only that day's studies", async ({ page }) => {
    await openImportPage(page);
    await page.locator('#study-date-from').fill('2020-11-01');
    await page.locator('#study-date-to').fill('2020-11-01');
    await page.locator('#submit-pacs-search').click();
    await expect(resultRows(page).first()).toBeVisible({ timeout: 60_000 });

    const uids = await resultRows(page).evaluateAll(rows => rows.map(r => r.getAttribute('data-uid')));
    const disco = Object.values(DISCO)
        .filter(s => uids.includes(s.studyInstanceUid))
        .map(s => s.n);
    expect(disco, 'Disco Study 3 is the only fixture study dated 2020-11-01').toEqual([3]);
});

test('a "From" date after the "To" date is refused with a message', async ({ page }) => {
    await openImportPage(page);
    await page.locator('#study-date-from').fill('2020-11-02');
    await page.locator('#study-date-to').fill('2020-11-01');
    await page.locator('#submit-pacs-search').click();
    await expect(dialogWith(page, /cannot (come after|come before)/)).toBeVisible();
});

test('Begin Import with nothing ticked asks the user to choose a study', async ({ page }) => {
    await openImportPage(page);
    await searchAccession(page, DISCO[2]);
    await page.locator('#import-selected-sessions').click();
    await expect(dialogWith(page, 'Please select at least one study to import.')).toBeVisible();
});

test('importing chosen series through the page archives them under the typed labels', async ({ page }) => {
    const subject = uniqueLabel('UiSubj');
    const session = uniqueLabel('UiSess');
    madeLabels.push(session);
    relabeled.add(DISCO[6]);

    await openImportPage(page);
    const row = await searchAccession(page, DISCO[6]);
    await row.locator('input.relabel-patient-name').fill(subject);
    await row.locator('input.relabel-study-id').fill(session);
    await row.locator('input.select-session').check();
    await page.locator('#import-selected-sessions').click();

    const list = page.locator('#scan-types-list');
    await expect(list).toBeVisible({ timeout: 60_000 });
    const boxes = list.locator('input.select-scan-type');
    const descriptions = await boxes.evaluateAll(b => b.map(x => (x as HTMLInputElement).value));
    expect(descriptions.length, 'Disco Study 6 offers more than one series description').toBeGreaterThan(1);
    // Keep only the first description.
    for (let i = 1; i < descriptions.length; i++) await boxes.nth(i).uncheck();

    await page.getByRole('button', { name: 'Import Selected' }).click();
    await expect(dialogWith(page, 'PACS data has been queued for import.')).toBeVisible({ timeout: 30_000 });

    const archived = await waitForSession(admin, PROJECT, session, { timeoutMs: 240_000 });
    const subjectId = (await admin.getSession(archived.ID)).data_fields.subject_ID;
    expect(await admin.getSubjectLabel(subjectId)).toBe(subject);
    const series = await admin.querySeries(pacs.id, DISCO[6].studyInstanceUid);
    const expected = series
        .filter(s => s.seriesDescription === descriptions[0])
        .map(s => String(s.seriesNumber))
        .sort();
    expect((await admin.getScans(archived.ID)).map(s => String(s.ID)).sort()).toEqual(expected);
});

test('Import Entire Studies retrieves the whole study at study level', async ({ page }) => {
    const session = uniqueLabel('UiWhole');
    madeLabels.push(session);
    relabeled.add(DISCO[7]);

    await openImportPage(page);
    const row = await searchAccession(page, DISCO[7]);
    await row.locator('input.relabel-patient-name').fill(uniqueLabel('UiWholeSubj'));
    await row.locator('input.relabel-study-id').fill(session);
    await row.locator('input.select-session').check();
    await page.locator('#import-selected-sessions').click();
    await page.getByRole('button', { name: 'Import Entire Studies' }).click();
    await expect(dialogWith(page, 'PACS data has been queued for import.')).toBeVisible({ timeout: 30_000 });

    const archived = await waitForSession(admin, PROJECT, session, { timeoutMs: 240_000 });
    const expected = (await admin.querySeries(pacs.id, DISCO[7].studyInstanceUid))
        .map(s => String(s.seriesNumber))
        .sort();
    expect((await admin.getScans(archived.ID)).map(s => String(s.ID)).sort()).toEqual(expected);
    const row7 = (await admin.history(true)).find(
        h => h.xnatProject === PROJECT && h.studyInstanceUid === DISCO[7].studyInstanceUid,
    );
    expect(row7?.retrieveLevel).toBe('STUDY');
});

test('on a study-level PACS the series are locked and only the whole study can be imported', async ({
    page,
}) => {
    const studyPacs = await admin.createPacs({
        aeTitle: 'ORTHANC',
        label: uniqueLabel('E2E_UI_STUDY'),
        host: '127.0.0.1',
        queryRetrievePort: pacs.queryRetrievePort,
        queryable: true,
        storable: false,
        dicomWebEnabled: false,
        retrieveLevel: 'STUDY',
    });
    madePacs.push(studyPacs.id);

    await openImportPage(page);
    const offered = await page
        .locator('#select-pacs option')
        .evaluateAll(o => o.map(x => (x as HTMLOptionElement).value));
    expect(offered, 'the study-level PACS must be offered in the Source PACS list').toContain(
        String(studyPacs.id),
    );
    await page.locator('#select-pacs').selectOption(String(studyPacs.id));
    await expect(page.locator('#pacs-status-indicator')).toContainText('Ready to Query', { timeout: 30_000 });
    const row = await searchAccession(page, DISCO[1]);
    await row.locator('input.select-session').check();
    await page.locator('#import-selected-sessions').click();

    const boxes = page.locator('#scan-types-list input.select-scan-type');
    await expect(boxes.first()).toBeVisible({ timeout: 60_000 });
    for (const box of await boxes.all()) {
        await expect(box).toBeChecked();
        await expect(box).toBeDisabled();
    }
    await expect(page.getByRole('button', { name: 'Import Entire Studies' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Import Selected' })).toHaveCount(0);
    await page.getByRole('button', { name: 'Cancel' }).click();
});

test("Search Via CSV fills the results with the CSV's matches and its labels", async ({ page }, testInfo) => {
    const csv = testInfo.outputPath('search.csv');
    fs.mkdirSync(path.dirname(csv), { recursive: true });
    fs.writeFileSync(
        csv,
        `Accession Number,Subject,Session\n${DISCO[5].accessionNumber},CSV_UI_SUBJ,CSV_UI_SESS\n`,
    );

    await openImportPage(page);
    await page.locator('#import-csv').click();
    const dialog = dialogWith(page, 'Start New Search via CSV File');
    await expect(dialog).toBeVisible();
    await dialog.locator('input[type="file"]').setInputFiles(csv);
    await dialog.getByRole('button', { name: 'Upload' }).click();

    const row = page.locator(`#all-search-results tr[data-uid="${DISCO[5].studyInstanceUid}"]`);
    await expect(row).toBeVisible({ timeout: 60_000 });
    await expect(row.locator('input.relabel-patient-name')).toHaveValue('CSV_UI_SUBJ');
    await expect(row.locator('input.relabel-study-id')).toHaveValue('CSV_UI_SESS');
});

test('with the project gate on, a project not switched on for DQR says so instead of offering a search', async ({
    page,
}) => {
    const off = projectNameFor('uioff');
    await admin.ensureProject(off);
    try {
        await settings.apply({ allowAllProjectsToUseDqr: false });
        await page.goto(`/app/template/Page.vm?view=dqr/import&project=${off}`);
        await expect(page.getByText('This project is not configured for PACS DICOM import')).toBeVisible({
            timeout: 30_000,
        });
        await expect(page.locator('#submit-pacs-search')).toBeHidden();
    } finally {
        await settings.restore();
        await settings.apply(FAST_QUEUE);
        await admin.deleteProjectQuietly(off);
    }
});
