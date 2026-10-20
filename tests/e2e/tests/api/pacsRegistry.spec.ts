/**
 * The PACS registry: adding, changing and removing the PACS XNAT talks to.
 *
 * Every PACS made here has its own AE title, because host, port and AE title
 * must be unique together, and points at 127.0.0.1. None of them is used to
 * search or retrieve, so the AE title does not need to match the Orthanc's.
 * Each is deleted afterwards.
 *
 * The validation rules come from the plugin's own PACS service: a DIMSE PACS
 * needs a host, a default storage PACS must be storable, a default query PACS
 * must be queryable, and a DICOMweb PACS must be queryable, not storable, and
 * series level. An admin who breaks one should be told, and nothing should be
 * saved.
 */
import { test, expect } from '../../lib/fixtures';
import { Pacs, PacsSettings, XnatClient } from '../../lib/dqr';
import { ADMIN, PACS_LABEL } from '../../lib/env';

let admin: XnatClient;
const made: number[] = [];

function uniqueAe(): string {
    return `E2E${Date.now().toString(36).toUpperCase()}${Math.floor(Math.random() * 100)}`.slice(0, 16);
}

/** A valid DIMSE PACS with a unique AE title. */
function dimse(overrides: Partial<PacsSettings> = {}): PacsSettings {
    const ae = uniqueAe();
    return {
        aeTitle: ae,
        label: `E2E_${ae}`,
        host: '127.0.0.1',
        queryRetrievePort: 4242,
        queryable: true,
        storable: false,
        dicomWebEnabled: false,
        ...overrides,
    };
}

async function create(settings: PacsSettings): Promise<Pacs> {
    const pacs = await admin.createPacs(settings);
    made.push(pacs.id);
    return pacs;
}

test.beforeAll(async () => {
    admin = await XnatClient.as(ADMIN);
});

test.afterAll(async () => {
    for (const id of made) await admin.deletePacsQuietly(id);
    // Belt and braces: nothing this spec labeled E2E_ may survive it.
    for (const p of await admin.listPacs())
        if (p.label.startsWith('E2E_')) await admin.deletePacsQuietly(p.id);
    await admin.dispose();
});

test('a DIMSE PACS is created, read back with every field, updated and deleted', async () => {
    const settings = dimse({ storable: true, retrieveLevel: 'STUDY' });
    const pacs = await create(settings);

    const read = await admin.getPacs(pacs.id);
    for (const [k, v] of Object.entries(settings)) {
        expect.soft(read[k as keyof Pacs], `field ${k} round trips`).toEqual(v);
    }

    const renamed = `${settings.label}_R`;
    const update = await admin.rawUpdatePacs(pacs.id, {
        ...settings,
        label: renamed,
        retrieveLevel: 'SERIES',
    });
    expect(update.ok(), `update: HTTP ${update.status()}`).toBeTruthy();
    const after = await admin.getPacs(pacs.id);
    expect(after.label).toBe(renamed);
    expect(after.retrieveLevel).toBe('SERIES');

    const del = await admin.delete(`/xapi/pacs/${pacs.id}`);
    expect(del.ok(), `delete: HTTP ${del.status()}`).toBeTruthy();
    expect((await admin.get(`/xapi/pacs/${pacs.id}`)).status(), 'a deleted PACS is gone').toBe(404);
});

test('a PACS saved without a retrieve level reads back as SERIES level', async () => {
    const settings = dimse();
    delete settings.retrieveLevel;
    const pacs = await create(settings);
    expect((await admin.getPacs(pacs.id)).retrieveLevel).toBe('SERIES');
});

test('the queryable and storable filters list exactly the matching PACS', async () => {
    const queryOnly = await create(dimse({ queryable: true, storable: false }));
    const storeOnly = await create(dimse({ queryable: false, storable: true }));

    const queryable = (await admin.listPacs({ queryable: true })).map(p => p.id);
    const storable = (await admin.listPacs({ storable: true })).map(p => p.id);

    expect(queryable).toContain(queryOnly.id);
    expect(queryable).not.toContain(storeOnly.id);
    expect(storable).toContain(storeOnly.id);
    expect(storable).not.toContain(queryOnly.id);
});

test('an unknown PACS id is 404', async () => {
    expect((await admin.get('/xapi/pacs/999999999')).status()).toBe(404);
});

const INVALID: Array<[string, Partial<PacsSettings>]> = [
    ['a DIMSE PACS with no host', { host: '' }],
    ['a default storage PACS that is not storable', { defaultStoragePacs: true, storable: false }],
    ['a default query PACS that is not queryable', { defaultQueryRetrievePacs: true, queryable: false }],
    [
        'a DICOMweb PACS that is storable',
        {
            dicomWebEnabled: true,
            dicomWebRootUrl: 'http://127.0.0.1:8042/dicom-web/',
            storable: true,
            host: undefined,
        },
    ],
    [
        'a DICOMweb PACS at study level',
        {
            dicomWebEnabled: true,
            dicomWebRootUrl: 'http://127.0.0.1:8042/dicom-web/',
            retrieveLevel: 'STUDY',
            host: undefined,
        },
    ],
    [
        'a DICOMweb PACS that is not queryable',
        {
            dicomWebEnabled: true,
            dicomWebRootUrl: 'http://127.0.0.1:8042/dicom-web/',
            queryable: false,
            host: undefined,
        },
    ],
];

test('an invalid PACS is refused with a 400 that says why, and nothing is saved', async () => {
    // Each rule is checked separately so one run reports every rule that is
    // not enforced, rather than stopping at the first.
    for (const [what, overrides] of INVALID) {
        const settings = dimse(overrides);
        const res = await admin.rawCreatePacs(settings);
        const body = await res.text();
        const saved = (await admin.listPacs()).find(p => p.label === settings.label);
        if (saved) made.push(saved.id);

        expect
            .soft(res.status(), `${what}: should be refused as a client error, got: ${body.slice(0, 120)}`)
            .toBe(400);
        expect.soft(body.trim().length, `${what}: the refusal should say what is wrong`).toBeGreaterThan(0);
        expect.soft(saved, `${what}: must not be saved`).toBeUndefined();
    }
});

test('an invalid update is refused, and the stored PACS is left as it was', async () => {
    const valid = dimse();
    const pacs = await create(valid);

    const res = await admin.rawUpdatePacs(pacs.id, { ...valid, host: '' });
    const stored = await admin.getPacs(pacs.id);

    expect
        .soft(res.status(), 'removing the host of a DIMSE PACS should be refused as a client error')
        .toBe(400);
    expect(stored.host, 'the refused change must not be saved').toBe(valid.host);
});

test('making a PACS the default query PACS takes the flag from the previous default', async () => {
    const fixture = await admin.pacsByLabel(PACS_LABEL);
    expect(fixture.defaultQueryRetrievePacs, 'the fixture PACS starts as the default').toBeTruthy();

    try {
        const pacs = await create(dimse({ defaultQueryRetrievePacs: true }));
        const defaults = (await admin.listPacs()).filter(p => p.defaultQueryRetrievePacs).map(p => p.id);
        expect(defaults, 'exactly one PACS is the default query PACS').toEqual([pacs.id]);
    } finally {
        for (const id of made.splice(0)) await admin.deletePacsQuietly(id);
        const restore = await admin.rawUpdatePacs(fixture.id, { ...fixture, defaultQueryRetrievePacs: true });
        expect(restore.ok(), 'restoring the fixture PACS as default').toBeTruthy();
        const defaults = (await admin.listPacs()).filter(p => p.defaultQueryRetrievePacs).map(p => p.label);
        expect(defaults, 'the fixture PACS is the only default again').toEqual([PACS_LABEL]);
    }
});

test('two PACS may not share a label', async () => {
    const first = await create(dimse());
    const res = await admin.rawCreatePacs(dimse({ label: first.label }));
    const sameLabel = (await admin.listPacs()).filter(p => p.label === first.label);
    for (const p of sameLabel) if (p.id !== first.id) made.push(p.id);

    expect(
        res.status(),
        'a second PACS with the same label should be refused as a client error',
    ).toBeGreaterThanOrEqual(400);
    expect(res.status(), "a duplicate is the caller's mistake, not a server error").toBeLessThan(500);
    expect(sameLabel, 'only the first PACS carries the label').toHaveLength(1);
});

test('two PACS may not share a host, port and AE title', async () => {
    const first = await create(dimse());
    const twin = dimse({
        aeTitle: first.aeTitle,
        host: first.host,
        queryRetrievePort: first.queryRetrievePort,
    });
    const res = await admin.rawCreatePacs(twin);
    const body = await res.text();
    const saved = (await admin.listPacs()).find(p => p.label === twin.label);
    if (saved) made.push(saved.id);

    expect(saved, 'the duplicate must not be saved').toBeUndefined();
    expect(
        res.status(),
        `a duplicate should be refused as a client error, got: ${body.slice(0, 120)}`,
    ).toBeGreaterThanOrEqual(400);
    expect(res.status(), "a duplicate is the caller's mistake, not a server error").toBeLessThan(500);
});

test('a PACS added through the API can retrieve, because it has an availability schedule', async () => {
    // The admin page creates a window for every day when it adds a PACS. The
    // queue only moves while a window is open, so a PACS added any other way
    // with no schedule accepts imports that never run, and nothing says why.
    const pacs = await create(dimse());
    const windows = Object.values(await admin.getAvailability(pacs.id)).flat();
    expect(windows.length, 'a new PACS with no schedule can never run an import').toBeGreaterThan(0);
});

test('ping reports a live PACS as up and records the result', async () => {
    const fixture = await admin.pacsByLabel(PACS_LABEL);
    const ping = await admin.ping(fixture.id);
    expect(ping.successful).toBeTruthy();

    const last = await (await admin.get(`/xapi/pacs/${fixture.id}/status/last`)).json();
    expect(last.successful).toBeTruthy();
    expect(last.pingTime, 'the recorded ping is the one just made').toBeGreaterThanOrEqual(
        Date.now() - 120_000,
    );
});

test('ping reports a PACS that is not listening as down, rather than failing', async () => {
    const pacs = await create(dimse({ queryRetrievePort: 4299 }));
    const res = await admin.get(`/xapi/pacs/${pacs.id}/status`);
    expect(res.ok(), 'a down PACS is a result, not a server error').toBeTruthy();
    expect((await res.json()).successful).toBeFalsy();
});
