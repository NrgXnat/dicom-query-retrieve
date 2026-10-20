/**
 * Search Via CSV: each row of an uploaded CSV is a search, and relabel columns
 * in the same row travel with its matches into the import.
 *
 * A coordinator's CSV is usually produced by a spreadsheet, so the formats a
 * spreadsheet writes (dates with dashes, a header row and nothing else) are
 * the ones that matter.
 */
import { test, expect } from '../../lib/fixtures';
import { Pacs, XnatClient } from '../../lib/dqr';
import { ADMIN, PACS_LABEL } from '../../lib/env';
import { DISCO } from '../../lib/disco';

let admin: XnatClient;
let pacs: Pacs;

interface BatchRow {
    studies: Array<{ studyInstanceUid: string }>;
    criteria: Record<string, unknown>;
    relabelMap?: Record<string, string>;
}

async function batch(csv: string): Promise<BatchRow[]> {
    const res = await admin.rawQueryBatch(pacs.id, csv);
    const body = await res.text();
    expect(res.ok(), `CSV search: HTTP ${res.status()} ${body.slice(0, 200)}`).toBeTruthy();
    return JSON.parse(body);
}

function uids(row: BatchRow): string[] {
    return row.studies.map(s => s.studyInstanceUid).sort();
}

test.beforeAll(async () => {
    admin = await XnatClient.as(ADMIN);
    pacs = await admin.pacsByLabel(PACS_LABEL);
});

test.afterAll(async () => {
    await admin.dispose();
});

test('each row of accession numbers finds its study and carries its Subject and Session', async () => {
    const rows = await batch(
        'Accession Number,Subject,Session\n' +
            `${DISCO[2].accessionNumber},CSV_SUBJ_2,CSV_SESS_2\n` +
            `${DISCO[6].accessionNumber},CSV_SUBJ_6,CSV_SESS_6\n`,
    );
    expect(rows).toHaveLength(2);
    expect(uids(rows[0])).toEqual([DISCO[2].studyInstanceUid]);
    expect(rows[0].relabelMap).toEqual({ Subject: 'CSV_SUBJ_2', Session: 'CSV_SESS_2' });
    expect(uids(rows[1])).toEqual([DISCO[6].studyInstanceUid]);
    expect(rows[1].relabelMap).toEqual({ Subject: 'CSV_SUBJ_6', Session: 'CSV_SESS_6' });
});

test('rows can search by study UID, patient ID or modality', async () => {
    const rows = await batch(
        'Study Instance UID,Patient ID,Modality\n' +
            `${DISCO[1].studyInstanceUid},,\n` +
            `,${DISCO[6].patientId},\n` +
            ',,US\n',
    );
    expect(uids(rows[0])).toEqual([DISCO[1].studyInstanceUid]);
    expect(uids(rows[1])).toEqual([DISCO[6].studyInstanceUid]);
    expect(uids(rows[2])).toContain(DISCO[7].studyInstanceUid);
});

test('the other relabel columns are carried as well', async () => {
    const rows = await batch(
        'Accession Number,Relabel Patient Name,Relabel Patient ID,Relabel Accession Number\n' +
            `${DISCO[5].accessionNumber},E2E^RELABEL,E2E_PID,E2E_ACC\n`,
    );
    expect(rows[0].relabelMap).toEqual({
        'Relabel Patient Name': 'E2E^RELABEL',
        'Relabel Patient ID': 'E2E_PID',
        'Relabel Accession Number': 'E2E_ACC',
    });
});

test('a CSV with no relabel columns still searches, and says the import will not relabel', async () => {
    const res = await admin.rawQueryBatch(pacs.id, `Accession Number\n${DISCO[2].accessionNumber}\n`);
    expect(res.ok()).toBeTruthy();
    expect(uids((await res.json())[0])).toEqual([DISCO[2].studyInstanceUid]);
    expect(res.headers()['warning'], 'a Warning header flags that nothing will be relabeled').toBeTruthy();
});

test('a row with no criteria is skipped, because it would match every study on the PACS', async () => {
    const rows = await batch(
        `Accession Number,Subject\n,ORPHAN_SUBJ\n${DISCO[2].accessionNumber},CSV_SUBJ_2\n`,
    );
    expect(rows.map(r => r.relabelMap?.Subject)).toEqual(['CSV_SUBJ_2']);
});

test('a study date written with dashes, as spreadsheets write it, is read', async () => {
    const res = await admin.rawQueryBatch(pacs.id, 'Study Date\n2017-12-06\n');
    const body = await res.text();
    expect(
        res.ok(),
        `a dashed date should be accepted: HTTP ${res.status()} ${body.slice(0, 200)}`,
    ).toBeTruthy();
    expect(uids(JSON.parse(body)[0])).toEqual([DISCO[6].studyInstanceUid]);
});

test('a study date range in a CSV row is read', async () => {
    const rows = await batch('Study Date\n20201101-20201102\n');
    for (const n of [1, 2, 3, 5] as const) expect(uids(rows[0])).toContain(DISCO[n].studyInstanceUid);
});

test('a single study date in a CSV row finds only that day', async () => {
    const rows = await batch('Study Date\n20201101\n');
    const found = uids(rows[0]).filter(u => Object.values(DISCO).some(s => s.studyInstanceUid === u));
    expect(found, 'Disco Study 3 is the only study on 2020-11-01').toEqual([DISCO[3].studyInstanceUid]);
});

test('a CSV of more than 1000 rows is refused with 400', async () => {
    const lines = [
        'Accession Number',
        ...Array.from({ length: 1001 }, (_, i) => `E2E${String(i).padStart(6, '0')}`),
    ];
    const res = await admin.rawQueryBatch(pacs.id, lines.join('\n') + '\n');
    expect(res.status()).toBe(400);
});

test('a CSV search of an unknown PACS is 404', async () => {
    const res = await admin.rawQueryBatch(999999999, `Accession Number\n${DISCO[2].accessionNumber}\n`);
    expect(res.status()).toBe(404);
});

test('a malformed date in a CSV is refused as a client error that names the value', async () => {
    const res = await admin.rawQueryBatch(pacs.id, 'Study Date\nnot-a-date\n');
    const body = await res.text();
    expect(
        res.status(),
        `a bad value in the user's file is their mistake, not a server error: ${body.slice(0, 160)}`,
    ).toBe(400);
    expect(body).toContain('not-a-date');
});
