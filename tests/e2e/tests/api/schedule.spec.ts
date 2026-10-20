/**
 * PACS availability windows: when XNAT may pull from a PACS, with how many
 * requests at once, and what share of the PACS's time.
 *
 * The queue only moves while a window is open, so a schedule that saves the
 * wrong thing stops imports without any error. Everything here runs against a
 * PACS made for the spec, never the fixture PACS, whose 24-hour schedule the
 * rest of the suite depends on.
 */
import { test, expect } from '../../lib/fixtures';
import { Pacs, XnatClient } from '../../lib/dqr';
import { ADMIN } from '../../lib/env';

let admin: XnatClient;
let pacs: Pacs;

const DAY = 'WEDNESDAY';

function window(start: string, end: string, extra: Record<string, unknown> = {}) {
    return {
        dayOfWeek: DAY,
        availabilityStart: start,
        availabilityEnd: end,
        threads: 1,
        utilizationPercent: 50,
        ...extra,
    };
}

async function dayWindows(): Promise<any[]> {
    const all = await admin.getAvailability(pacs.id);
    return (all[DAY] ?? []).sort((a, b) => a.availabilityStart.localeCompare(b.availabilityStart));
}

async function createWindow(start: string, end: string, extra: Record<string, unknown> = {}): Promise<any> {
    const res = await admin.rawCreateAvailability(pacs.id, window(start, end, extra));
    expect(
        res.ok(),
        `create window ${start}-${end}: HTTP ${res.status()} ${(await res.text()).slice(0, 200)}`,
    ).toBeTruthy();
    return res.json();
}

test.beforeAll(async () => {
    admin = await XnatClient.as(ADMIN);
    const ae = `E2ESCH${Date.now().toString(36).toUpperCase()}`.slice(0, 16);
    pacs = await admin.createPacs({
        aeTitle: ae,
        label: `E2E_${ae}`,
        host: '127.0.0.1',
        queryRetrievePort: 4242,
        queryable: true,
        dicomWebEnabled: false,
    });
});

test.beforeEach(async () => {
    for (const w of Object.values(await admin.getAvailability(pacs.id)).flat()) {
        await admin.delete(`/xapi/pacs/${pacs.id}/availability/${w.id}`);
    }
});

test.afterAll(async () => {
    await admin.deletePacsQuietly(pacs.id);
    await admin.dispose();
});

test('a window is created, read back by day and by id, and deleted', async () => {
    const made = await createWindow('08:00', '12:00', { threads: 3, utilizationPercent: 40 });

    const listed = await dayWindows();
    expect(
        listed.map(w => [w.availabilityStart, w.availabilityEnd, w.threads, w.utilizationPercent]),
    ).toEqual([['08:00', '12:00', 3, 40]]);

    const byId = await (await admin.get(`/xapi/pacs/${pacs.id}/availability/${made.id}`)).json();
    expect(byId.availabilityStart).toBe('08:00');

    const del = await admin.delete(`/xapi/pacs/${pacs.id}/availability/${made.id}`);
    expect(del.ok()).toBeTruthy();
    expect(await dayWindows()).toEqual([]);
    expect((await admin.get(`/xapi/pacs/${pacs.id}/availability/${made.id}`)).status()).toBe(404);
});

test('an overlapping window is refused, and the schedule is unchanged', async () => {
    await createWindow('08:00', '12:00');
    const res = await admin.rawCreateAvailability(pacs.id, window('11:00', '13:00'));

    expect(res.status(), 'an overlap is refused as a client error').toBe(400);
    expect((await dayWindows()).map(w => [w.availabilityStart, w.availabilityEnd])).toEqual([
        ['08:00', '12:00'],
    ]);
});

test('windows that only touch at the boundary do not overlap', async () => {
    await createWindow('08:00', '12:00');
    await createWindow('12:00', '16:00');
    expect((await dayWindows()).map(w => [w.availabilityStart, w.availabilityEnd])).toEqual([
        ['08:00', '12:00'],
        ['12:00', '16:00'],
    ]);
});

test('the validate call answers true for a free slot and false for an overlap', async () => {
    await createWindow('08:00', '12:00');
    const free = await admin.post(`/xapi/pacs/${pacs.id}/availability/validate`, {
        pacsId: pacs.id,
        ...window('13:00', '14:00'),
    });
    const clash = await admin.post(`/xapi/pacs/${pacs.id}/availability/validate`, {
        pacsId: pacs.id,
        ...window('09:00', '10:00'),
    });

    expect(free.ok()).toBeTruthy();
    expect(await free.json()).toBe(true);
    expect(clash.ok(), 'an overlap is an answer, not an error').toBeTruthy();
    expect(await clash.json()).toBe(false);
});

test('a window missing its day, start or end is refused', async () => {
    for (const missing of ['dayOfWeek', 'availabilityStart', 'availabilityEnd']) {
        const body: Record<string, unknown> = window('08:00', '09:00');
        delete body[missing];
        const res = await admin.rawCreateAvailability(pacs.id, body);
        expect.soft(res.status(), `a window with no ${missing} is refused as a client error`).toBe(400);
    }
    expect(await dayWindows()).toEqual([]);
});

test('widening a window over its neighbor trims the neighbor rather than overlapping it', async () => {
    const first = await createWindow('08:00', '12:00');
    await createWindow('13:00', '17:00');

    const res = await admin.put(`/xapi/pacs/${pacs.id}/availability/${first.id}`, {
        id: first.id,
        pacsId: pacs.id,
        ...window('08:00', '14:00'),
    });
    expect(res.ok(), `update: HTTP ${res.status()}`).toBeTruthy();

    expect(
        (await dayWindows()).map(w => [w.availabilityStart, w.availabilityEnd]),
        'the neighbor now starts where the widened window ends',
    ).toEqual([
        ['08:00', '14:00'],
        ['14:00', '17:00'],
    ]);
});

test('saving a window unchanged answers 304 Not Modified', async () => {
    const w = await createWindow('08:00', '12:00');
    const res = await admin.put(`/xapi/pacs/${pacs.id}/availability/${w.id}`, {
        id: w.id,
        pacsId: pacs.id,
        ...window('08:00', '12:00'),
    });
    expect(res.status()).toBe(304);
});

test('a window with zero threads also has zero utilization, so it can never run anything', async () => {
    await createWindow('08:00', '12:00', { threads: 0, utilizationPercent: 80 });
    const [w] = await dayWindows();
    expect([w.threads, w.utilizationPercent]).toEqual([0, 0]);
});

test('an all-day window ending at midnight is open now', async () => {
    // Made on the server's current weekday, so "open now" is checkable. The
    // stack runs on US Central time; XNAT reports availableNow in its own zone.
    const today = new Date()
        .toLocaleDateString('en-US', { weekday: 'long', timeZone: 'America/Chicago' })
        .toUpperCase();
    const res = await admin.rawCreateAvailability(pacs.id, { ...window('00:00', '24:00'), dayOfWeek: today });
    expect(res.ok(), `create all-day window: HTTP ${res.status()}`).toBeTruthy();

    const [w] = (await admin.getAvailability(pacs.id))[today];
    expect(w.availabilityStart).toBe('00:00');
    expect(w.availableNow, 'an all-day window for today is open now').toBeTruthy();
});

test('a window on an unknown PACS is refused, and nothing is stored for it', async () => {
    const missing = 999999999;
    const res = await admin.post(`/xapi/pacs/${missing}/availability`, {
        pacsId: missing,
        ...window('08:00', '09:00'),
    });
    const orphans = Object.values(
        (await (await admin.get(`/xapi/pacs/${missing}/availability`)).json().catch(() => ({}))) as Record<
            string,
            any[]
        >,
    ).flat();
    for (const o of orphans) await admin.delete(`/xapi/pacs/${missing}/availability/${o.id}`);

    expect
        .soft(res.status(), 'a schedule for a PACS that does not exist is refused as a client error')
        .toBeGreaterThanOrEqual(400);
    expect.soft(res.status()).toBeLessThan(500);
    expect(orphans, 'no schedule entry is stored for a PACS that does not exist').toEqual([]);
});
