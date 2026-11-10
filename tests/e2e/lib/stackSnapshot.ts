/**
 * What the run must leave as it found it, read once by the preflight and
 * again by the global teardown.
 *
 * Only state the suite itself creates or changes is compared, so other people
 * using the same stack during a run do not fail it: test projects and their
 * DQR switch rows are matched by the DQR_E2E_ prefix, queue rows and
 * prearchive entries by their test project or by being new in Unassigned.
 */
import { APIRequestContext } from '@playwright/test';

export const TEST_PROJECT_PREFIX = 'DQR_E2E_';

export interface StackSnapshot {
    settings: Record<string, unknown>;
    pacs: string[];
    fixturePacs: Record<string, unknown> | null;
    fixtureSchedule: unknown;
    receivers: unknown[];
    testProjects: string[];
    testProjectSettings: string[];
    testQueueRows: number[];
    prearchive: string[];
}

async function json(ctx: APIRequestContext, url: string, params?: Record<string, string | boolean>) {
    const res = await ctx.get(url, { params });
    if (!res.ok()) throw new Error(`[DQR-E2E] snapshot: GET ${url} answered ${res.status()}`);
    return res.json();
}

export async function takeSnapshot(ctx: APIRequestContext, pacsLabel: string): Promise<StackSnapshot> {
    const pacsList: any[] = await json(ctx, '/xapi/pacs');
    const fixture = pacsList.find(p => p.label === pacsLabel) ?? null;
    let fixturePacs: Record<string, unknown> | null = null;
    let fixtureSchedule: unknown = null;
    if (fixture) {
        // The timestamp moves whenever the default flags are written back, even
        // to the same values, so it is not part of the comparison.
        const copy: Record<string, unknown> = { ...fixture };
        delete copy.timestamp;
        fixturePacs = copy;
        const windows = await json(ctx, `/xapi/pacs/${fixture.id}/availability`);
        fixtureSchedule = Object.fromEntries(
            Object.entries(windows as Record<string, any[]>).map(([day, ws]) => [
                day,
                ws
                    .map(
                        w =>
                            `${w.dayOfWeek} ${w.availabilityStart}-${w.availabilityEnd} t${w.threads} u${w.utilizationPercent} ${w.enabled}`,
                    )
                    .sort(),
            ]),
        );
    }
    const receivers = ((await json(ctx, '/xapi/dicomscp')) as any[])
        .map(r => ({
            id: r.id,
            aeTitle: r.aeTitle,
            port: r.port,
            enabled: r.enabled,
            identifier: r.identifier,
            customProcessing: r.customProcessing,
        }))
        .sort((a, b) => a.id - b.id);
    const projects: any[] = (await json(ctx, '/data/projects', { format: 'json' } as any)).ResultSet.Result;
    const projectSettings: any[] = await json(ctx, '/xapi/dqr/settings/project');
    const queue: any[] = await json(ctx, '/xapi/dqr/import/queue', { all: true });
    const pre: any[] = (await json(ctx, '/data/prearchive', { format: 'json' } as any)).ResultSet.Result;
    const isTest = (p: unknown) => String(p ?? '').startsWith(TEST_PROJECT_PREFIX);

    return {
        settings: await json(ctx, '/xapi/dqr/settings'),
        pacs: pacsList.map(p => p.label).sort(),
        fixturePacs,
        fixtureSchedule,
        receivers,
        testProjects: projects
            .map(p => String(p.ID))
            .filter(isTest)
            .sort(),
        testProjectSettings: projectSettings
            .map(r => String(r.projectId))
            .filter(isTest)
            .sort(),
        testQueueRows: queue
            .filter(r => isTest(r.xnatProject))
            .map(r => r.id)
            .sort((a, b) => a - b),
        prearchive: pre
            .filter(r => isTest(r.project) || r.project === 'Unassigned')
            .map(r => `${r.project}/${r.timestamp}/${r.name}`)
            .sort(),
    };
}

/** Differences that mean the run left something behind or changed something. */
export function compareSnapshots(before: StackSnapshot, after: StackSnapshot): string[] {
    const problems: string[] = [];
    for (const k of new Set([...Object.keys(before.settings), ...Object.keys(after.settings)])) {
        if (String(before.settings[k]) !== String(after.settings[k])) {
            problems.push(`setting ${k} was ${before.settings[k]}, is now ${after.settings[k]}`);
        }
    }
    const same = (a: unknown, b: unknown) => JSON.stringify(a) === JSON.stringify(b);
    if (!same(before.pacs, after.pacs)) {
        problems.push(`PACS list was ${JSON.stringify(before.pacs)}, is now ${JSON.stringify(after.pacs)}`);
    }
    if (!same(before.fixturePacs, after.fixturePacs)) {
        problems.push(
            `the fixture PACS changed: was ${JSON.stringify(before.fixturePacs)}, is now ${JSON.stringify(after.fixturePacs)}`,
        );
    }
    if (!same(before.fixtureSchedule, after.fixtureSchedule)) {
        problems.push(
            `the fixture PACS schedule changed: was ${JSON.stringify(before.fixtureSchedule)}, is now ${JSON.stringify(after.fixtureSchedule)}`,
        );
    }
    if (!same(before.receivers, after.receivers)) {
        problems.push(
            `the SCP receivers changed: was ${JSON.stringify(before.receivers)}, is now ${JSON.stringify(after.receivers)}`,
        );
    }
    const added = (a: Array<string | number>, b: Array<string | number>) => b.filter(x => !a.includes(x));
    const leftovers: Array<[string, Array<string | number>]> = [
        ['test projects', added(before.testProjects, after.testProjects)],
        ['DQR switch rows for test projects', added(before.testProjectSettings, after.testProjectSettings)],
        ['queue rows for test projects', added(before.testQueueRows, after.testQueueRows)],
        ['prearchive entries (test projects or Unassigned)', added(before.prearchive, after.prearchive)],
    ];
    for (const [what, items] of leftovers) {
        if (items.length) problems.push(`the run left ${items.length} ${what}: ${items.join(', ')}`);
    }
    return problems;
}
