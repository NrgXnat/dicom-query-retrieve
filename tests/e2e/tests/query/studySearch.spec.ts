/**
 * Searching the PACS for studies, series and patients.
 *
 * Every assertion is against the Disco fixture studies (lib/disco.ts), by
 * study UID, and never by count, because export tests add studies of their
 * own to the PACS. "Only these Disco studies" is checked by intersecting the
 * results with the fixture.
 */
import { test, expect } from '../../lib/fixtures';
import { Pacs, StudyResult, XnatClient } from '../../lib/dqr';
import { ADMIN, PACS_LABEL } from '../../lib/env';
import { ALL_DISCO, DISCO, DiscoStudy } from '../../lib/disco';
import { SiteSettings } from '../../lib/stack';

let admin: XnatClient;
let pacs: Pacs;
let settings: SiteSettings;
const made: number[] = [];

/** Which Disco studies (by number) a result list contains. */
function discoIn(results: StudyResult[]): number[] {
    const uids = new Set(results.map(r => r.studyInstanceUid));
    return ALL_DISCO.filter(s => uids.has(s.studyInstanceUid))
        .map(s => s.n)
        .sort();
}

function numbers(...studies: DiscoStudy[]): number[] {
    return studies.map(s => s.n).sort();
}

test.beforeAll(async () => {
    admin = await XnatClient.as(ADMIN);
    pacs = await admin.pacsByLabel(PACS_LABEL);
    settings = new SiteSettings(admin);
});

test.afterEach(async () => {
    await settings.restore();
});

test.afterAll(async () => {
    for (const id of made) await admin.deletePacsQuietly(id);
    await admin.dispose();
});

test('a search with no criteria returns every fixture study, with its details', async () => {
    const results = await admin.queryStudies({ pacsId: pacs.id });
    expect(discoIn(results)).toEqual(numbers(...ALL_DISCO));

    for (const s of ALL_DISCO) {
        const r = results.find(x => x.studyInstanceUid === s.studyInstanceUid)!;
        expect.soft(r.accessionNumber, `Disco ${s.n} accession`).toBe(s.accessionNumber);
        expect.soft(r.studyDate, `Disco ${s.n} date`).toBe(s.studyDate);
        expect.soft(r.modalitiesInStudy, `Disco ${s.n} modality`).toEqual([s.modality]);
        expect.soft(r.patient?.id, `Disco ${s.n} patient ID`).toBe(s.patientId);
    }
});

test('searching by accession number finds that study', async () => {
    expect(
        discoIn(await admin.queryStudies({ pacsId: pacs.id, accessionNumber: DISCO[2].accessionNumber })),
    ).toEqual([2]);
});

test('an accession number shared by two studies finds both', async () => {
    expect(
        discoIn(await admin.queryStudies({ pacsId: pacs.id, accessionNumber: DISCO[3].accessionNumber })),
    ).toEqual([3, 4]);
});

test("searching by patient ID finds that patient's study", async () => {
    expect(discoIn(await admin.queryStudies({ pacsId: pacs.id, patientId: DISCO[6].patientId }))).toEqual([
        6,
    ]);
});

test('searching by patient name finds the patient, quoted exactly or by a leading part with a wildcard', async () => {
    // DQR turns a space or a comma in the name into DICOM's name separator,
    // so "Doe John" and "Doe, John" both search for Doe^John. The fixture
    // names contain plain spaces, so an exact search has to be quoted, which
    // DQR sends as typed.
    expect(
        discoIn(await admin.queryStudies({ pacsId: pacs.id, patientName: `"${DISCO[7].patientName}"` })),
    ).toEqual([7]);
    expect(discoIn(await admin.queryStudies({ pacsId: pacs.id, patientName: 'Disco*' }))).toEqual(
        ALL_DISCO.map(s => s.n),
    );
});

test('searching by study UID finds exactly that study', async () => {
    expect(
        discoIn(await admin.queryStudies({ pacsId: pacs.id, studyInstanceUid: DISCO[5].studyInstanceUid })),
    ).toEqual([5]);
});

test('searching by modality finds the studies of that modality', async () => {
    expect(discoIn(await admin.queryStudies({ pacsId: pacs.id, modality: 'CT' }))).toEqual([2, 3, 4, 5]);
    expect(discoIn(await admin.queryStudies({ pacsId: pacs.id, modality: 'MR' }))).toEqual([1, 6]);
    expect(discoIn(await admin.queryStudies({ pacsId: pacs.id, modality: 'US' }))).toEqual([7]);
});

test('a date range finds the studies inside it, inclusive at both ends', async () => {
    const results = await admin.queryStudies({
        pacsId: pacs.id,
        studyDateRange: { start: '20201101', end: '20201102' },
    });
    expect(discoIn(results)).toEqual([1, 2, 3, 5]);
});

test('a range with only a start date finds everything from that day on', async () => {
    const results = await admin.queryStudies({ pacsId: pacs.id, studyDateRange: { start: '20180101' } });
    expect(discoIn(results)).toEqual([1, 2, 3, 5]);
});

test('the three accepted date formats give the same results', async () => {
    const expected = [1, 2, 3, 5];
    for (const [start, end] of [
        ['20201101', '20201102'],
        ['2020-11-01', '2020-11-02'],
        ['2020/11/01', '2020/11/02'],
    ]) {
        const got = discoIn(await admin.queryStudies({ pacsId: pacs.id, studyDateRange: { start, end } }));
        expect.soft(got, `range ${start} to ${end}`).toEqual(expected);
    }
});

test("a search for a single day finds only that day's studies", async () => {
    // Disco Study 3 is the only study on 2020-11-01; Studies 1, 2 and 5 are on
    // the next day. A single-day search is what the page sends when From and
    // To are the same date.
    expect(
        discoIn(
            await admin.queryStudies({
                pacsId: pacs.id,
                studyDateRange: { start: '20201101', end: '20201101' },
            }),
        ),
    ).toEqual([3]);
    expect(
        discoIn(
            await admin.queryStudies({ pacsId: pacs.id, studyDateFrom: '20201101', studyDateTo: '20201101' }),
        ),
    ).toEqual([3]);
});

test('criteria are combined, so every one must match', async () => {
    const results = await admin.queryStudies({
        pacsId: pacs.id,
        modality: 'CT',
        studyDateRange: { start: '20201101', end: '20201102' },
    });
    expect(discoIn(results)).toEqual([2, 3, 5]);
});

test('a search that matches nothing answers 204 No Content', async () => {
    const res = await admin.rawQueryStudies({ pacsId: pacs.id, accessionNumber: '0000000000000' });
    expect(res.status()).toBe(204);
});

test('PACS id 0 means the default query PACS', async () => {
    expect(
        discoIn(await admin.queryStudies({ pacsId: 0, accessionNumber: DISCO[1].accessionNumber })),
    ).toEqual([1]);
});

test('a search of an unknown PACS is 404', async () => {
    expect((await admin.rawQueryStudies({ pacsId: 999999999 })).status()).toBe(404);
});

test('a search of a PACS that is not queryable is refused with 409', async () => {
    const ae = `E2ENQ${Date.now().toString(36).toUpperCase()}`.slice(0, 16);
    const storeOnly = await admin.createPacs({
        aeTitle: ae,
        label: `E2E_${ae}`,
        host: '127.0.0.1',
        queryRetrievePort: 4242,
        queryable: false,
        storable: true,
        dicomWebEnabled: false,
    });
    made.push(storeOnly.id);
    expect((await admin.rawQueryStudies({ pacsId: storeOnly.id })).status()).toBe(409);
});

test('the series of a study come back with their descriptions and numbers', async () => {
    const series = await admin.querySeries(pacs.id, DISCO[1].studyInstanceUid);
    expect(series.length, 'Disco Study 1 has series').toBeGreaterThan(0);
    for (const s of series) {
        expect
            .soft(
                s.seriesInstanceUid.startsWith(DISCO[1].studyInstanceUid),
                'series UID belongs to the study',
            )
            .toBeTruthy();
        expect.soft(s.study?.studyInstanceUid).toBe(DISCO[1].studyInstanceUid);
        expect.soft(s.modality).toBe('MR');
        expect.soft(typeof s.seriesDescription).toBe('string');
    }
});

test('a series search with no study UIDs answers 204 No Content', async () => {
    expect((await admin.rawQuerySeries(pacs.id, [])).status()).toBe(204);
});

test('a patient search finds the patient with their details', async () => {
    const res = await admin.post('/xapi/dqr/query/patients', {
        pacsId: pacs.id,
        patientId: DISCO[6].patientId,
    });
    expect(res.ok(), `HTTP ${res.status()}`).toBeTruthy();
    const body = await res.json();
    expect(body.results.map((p: any) => p.id)).toEqual([DISCO[6].patientId]);
});

test('when the PACS refuses a search, the caller is told it failed rather than that nothing matched', async () => {
    // XNAT calls the PACS with its calling AE title. Orthanc answers a ping
    // from any AE but refuses a search from one it does not know, which is
    // the situation of a PACS that has not been told about this XNAT.
    test.setTimeout(420_000);
    await settings.apply({ dqrCallingAe: 'E2EUNKNOWN' });

    const started = Date.now();
    const res = await admin.rawQueryStudies({ pacsId: pacs.id }, 400_000);
    const seconds = Math.round((Date.now() - started) / 1000);

    expect(
        res.status(),
        `answered ${res.status()} after ${seconds}s; 204 tells the user nothing matched`,
    ).not.toBe(204);
    expect(res.ok(), 'a refused search is an error').toBeFalsy();
});
