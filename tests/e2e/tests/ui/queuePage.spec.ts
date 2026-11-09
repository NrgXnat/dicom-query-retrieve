/**
 * The Import Queue / History page, driven in a real browser as an admin.
 *
 * Requests shown here are queued on a PACS made for the spec with no
 * availability window, so they stay in the queue for as long as the test
 * needs them, and are removed afterwards.
 */
import { Page, Request } from '@playwright/test';
import { test, expect } from '../../lib/fixtures';
import { Pacs, PacsRequestRow, XnatClient } from '../../lib/dqr';
import {
    ADMIN,
    DQR_ROLE_USER,
    PACS_LABEL,
    RECEIVER_AE,
    RECEIVER_PORT,
    projectNameFor,
    uniqueLabel,
} from '../../lib/env';
import { DISCO } from '../../lib/disco';

const PROJECT = projectNameFor('uiq');

let admin: XnatClient;
let dqrUser: XnatClient;
let idle: Pacs;
let mine: PacsRequestRow;
let theirs: PacsRequestRow;

test.beforeAll(async () => {
    admin = await XnatClient.as(ADMIN);
    dqrUser = await XnatClient.as(DQR_ROLE_USER);
    const pacs = await admin.pacsByLabel(PACS_LABEL);
    await admin.ensureDqrProject(PROJECT);
    await admin.addProjectUser(PROJECT, DQR_ROLE_USER.username, 'Members');
    idle = await admin.createPacs({
        aeTitle: 'ORTHANC',
        label: uniqueLabel('E2E_UIQ_IDLE'),
        host: '127.0.0.1',
        queryRetrievePort: pacs.queryRetrievePort,
        queryable: true,
        storable: false,
        dicomWebEnabled: false,
    });
    const body = (client: XnatClient, n: 1 | 2) =>
        client.queueImport({
            pacsId: idle.id,
            aeTitle: RECEIVER_AE,
            port: RECEIVER_PORT,
            projectId: PROJECT,
            forceImport: true,
            retrieveLevel: 'STUDY',
            requestId: uniqueLabel('e2euiq'),
            studies: [{ studyInstanceUid: DISCO[n].studyInstanceUid }],
        });
    [mine] = await body(admin, 1);
    [theirs] = await body(dqrUser, 2);
});

test.afterAll(async () => {
    const left = (await admin.queue(true)).filter(r => r.pacsId === idle.id).map(r => r.id);
    await admin.removeFromQueueQuietly(left);
    await admin.deletePacsQuietly(idle.id);
    await admin.deleteProjectQuietly(PROJECT);
    await dqrUser.dispose();
    await admin.dispose();
});

function queueRows(page: Page) {
    return page.locator('#pacs-import-queue-panel-container tbody tr');
}

/** The row for one request. Each row carries its request ID as data-id. */
function rowById(rows: ReturnType<Page['locator']>, page: Page, id: number) {
    return rows.and(page.locator(`tr[data-id="${id}"]`));
}

async function openQueue(page: Page, role: 'user' | 'admin' = 'user'): Promise<void> {
    await page.goto(`/app/template/Page.vm?view=dqr/queue&role=${role}#tab=queue`);
    await expect(page.locator('#pacs-import-queue-panel-container')).toBeVisible({ timeout: 30_000 });
}

test("the queue tab lists the admin's own request, with its project and destination", async ({ page }) => {
    await openQueue(page);
    const row = rowById(queueRows(page), page, mine.id);
    await expect(row).toHaveCount(1);
    await expect(row).toContainText(PROJECT);
    await expect(row).toContainText(`${RECEIVER_AE}:${RECEIVER_PORT}`);
    await expect(
        rowById(queueRows(page), page, theirs.id),
        'another user\'s request is not in "current user"',
    ).toHaveCount(0);
});

test('the "all users" view lists everyone\'s requests and who made them', async ({ page }) => {
    await openQueue(page, 'admin');
    await expect(rowById(queueRows(page), page, mine.id)).toHaveCount(1);
    const row = rowById(queueRows(page), page, theirs.id);
    await expect(row).toHaveCount(1);
    await expect(row).toContainText(DQR_ROLE_USER.username);
});

test('in the "current user" view the page counts only the current user\'s requests', async ({ page }) => {
    const countCalls: string[] = [];
    page.on('request', (r: Request) => {
        if (r.url().includes('/xapi/dqr/import/queue/count')) countCalls.push(r.url());
    });
    await openQueue(page, 'user');
    await expect.poll(() => countCalls.length, { timeout: 15_000 }).toBeGreaterThan(0);
    for (const url of countCalls)
        expect(url, 'the count for "current user" must not ask for all users').not.toContain('all=true');
});

test('clicking a request opens its details', async ({ page }) => {
    await openQueue(page);
    await rowById(queueRows(page), page, mine.id).locator('td.show-data').first().click();
    const dialog = page.locator('.xnat-dialog').filter({ hasText: DISCO[1].studyInstanceUid }).last();
    await expect(dialog).toBeVisible();
});

test('removing a request through the page asks first, then removes it', async ({ page }) => {
    await openQueue(page);
    await rowById(queueRows(page), page, mine.id).locator('a.remove-queue-item').click();
    const confirm = page.locator('.xnat-dialog').filter({ hasText: 'Remove queued item?' }).last();
    await expect(confirm).toBeVisible();
    await confirm.getByRole('button', { name: 'Remove' }).click();

    await expect(page.getByText('Item removed from queue.')).toBeVisible();
    await expect(rowById(queueRows(page), page, mine.id)).toHaveCount(0);
    expect((await admin.queue(true)).map(r => r.id)).not.toContain(mine.id);
});

test('the history tab lists finished requests', async ({ page }) => {
    await page.goto('/app/template/Page.vm?view=dqr/queue&role=admin#tab=history');
    const history = page.locator('#pacs-import-history-panel-container');
    await expect(history).toBeVisible({ timeout: 30_000 });
    const latest = (await admin.history(true))[0];
    await expect(rowById(history.locator('tbody tr'), page, latest.id)).toHaveCount(1);
});
