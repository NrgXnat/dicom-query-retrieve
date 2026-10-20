/**
 * Site-wide state the suite changes, and the waits it uses.
 *
 * DQR's access gates, retry counts and scheduler frequency are site-wide
 * settings. Specs that change them do so through `SiteSettings`, which records
 * the values first and puts them back afterwards, then reads them again to
 * prove the restore worked. A run that left a stack with its gates switched
 * the wrong way would make every later run on that stack meaningless.
 *
 * Imports finish minutes after the request that starts them, over a separate
 * connection from the PACS. The waits here poll the cheapest thing that shows
 * the outcome (the history row for a request ID, then the archived session),
 * with a hard deadline, rather than sleeping.
 *
 * Relabeling leaves state behind too. DQR stores an import's relabels as a
 * site-wide script keyed to the study's UID and never removes it, so a later
 * import of that study without relabels of its own is renamed with the old
 * ones. `neutralizeRelabels` replaces that script with one that sets only the
 * patient name and ID to their original values, so the next plain import of
 * the study is unaffected. Every spec that relabels a fixture study calls it
 * afterwards.
 */
import { expect } from '@playwright/test';
import { PacsRequestRow, XnatClient } from './dqr';
import { DiscoStudy } from './disco';
import { RECEIVER_AE, RECEIVER_PORT, projectNameFor, uniqueLabel } from './env';

export class SiteSettings {
    private original: Record<string, unknown> | undefined;
    private touched = new Set<string>();

    constructor(private readonly admin: XnatClient) {}

    /** Applies values, remembering the originals of every key it touches. */
    async apply(values: Record<string, unknown>): Promise<void> {
        if (!this.original) this.original = await this.admin.getSettings();
        for (const k of Object.keys(values)) this.touched.add(k);
        await this.admin.setSettings(values);
        const now = await this.admin.getSettings();
        for (const [k, v] of Object.entries(values)) {
            expect(String(now[k]), `setting ${k} did not take`).toBe(String(v));
        }
    }

    /** Puts back every key apply() touched, and checks it is back. */
    async restore(): Promise<void> {
        if (!this.original || this.touched.size === 0) return;
        const back: Record<string, unknown> = {};
        for (const k of this.touched) back[k] = this.original[k];
        await this.admin.setSettings(back);
        const now = await this.admin.getSettings();
        for (const k of this.touched) {
            expect(String(now[k]), `setting ${k} was not restored`).toBe(String(this.original[k]));
        }
        this.touched.clear();
    }
}

/** Both access gates switched on, which is the plugin's own default. */
export const GATES_ON = { allowAllUsersToUseDqr: false, allowAllProjectsToUseDqr: false };

/**
 * Settings for specs that run real imports: the scheduler looks for work every
 * ten seconds rather than every minute, so an import finishes in seconds.
 */
export const FAST_QUEUE = { pacsAvailabilityCheckFrequency: '10 seconds' };

/** Settings for specs that make imports fail: two attempts, ten seconds apart. */
export const FAST_FAILURE = { dqrMaxPacsRequestAttempts: '2', dqrWaitToRetryRequestInSeconds: '10' };

const TERMINAL = new Set(['RECEIVED', 'FAILED']);

/**
 * Waits until a request ID has left the queue and every study it named has a
 * terminal history row. Returns the history rows for that request ID, newest
 * first. Fails with the last state seen if the deadline passes.
 */
export async function waitForOutcome(
    admin: XnatClient,
    requestId: string,
    { timeoutMs = 240_000, intervalMs = 5_000 } = {},
): Promise<PacsRequestRow[]> {
    const deadline = Date.now() + timeoutMs;
    let queued: PacsRequestRow[] = [];
    let done: PacsRequestRow[] = [];
    while (Date.now() < deadline) {
        queued = (await admin.queue(true)).filter(r => r.requestId === requestId);
        done = (await admin.history(true)).filter(r => r.requestId === requestId);
        if (queued.length === 0 && done.length > 0 && done.every(r => TERMINAL.has(r.status))) return done;
        await new Promise(r => setTimeout(r, intervalMs));
    }
    throw new Error(
        `request ${requestId} did not finish within ${timeoutMs / 1000}s. ` +
            `Queue: ${JSON.stringify(queued.map(r => [r.id, r.status]))}. ` +
            `History: ${JSON.stringify(done.map(r => [r.id, r.status, r.errorMessage]))}.`,
    );
}

/** Waits for a session with this label to be archived in the project. */
export async function waitForSession(
    admin: XnatClient,
    projectId: string,
    label: string,
    { timeoutMs = 120_000, intervalMs = 5_000 } = {},
): Promise<{ ID: string; label: string; xsiType: string }> {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
        const match = (await admin.listSessions(projectId)).find(s => s.label === label);
        if (match) return match;
        await new Promise(r => setTimeout(r, intervalMs));
    }
    const seen = (await admin.listSessions(projectId)).map(s => s.label);
    throw new Error(
        `no session "${label}" in ${projectId} after ${timeoutMs / 1000}s; saw ${JSON.stringify(seen)}`,
    );
}

/**
 * Leaves each study's stored relabel script harmless: one that sets the
 * patient name and ID to the values the PACS already has. Runs a real import
 * of each study into a scratch project, which it deletes afterwards. The
 * scheduler should already be fast (FAST_QUEUE) when this is called.
 */
export async function neutralizeRelabels(
    admin: XnatClient,
    pacsId: number,
    studies: DiscoStudy[],
): Promise<void> {
    if (studies.length === 0) return;
    const project = projectNameFor('neu');
    await admin.ensureProject(project);
    try {
        const requestId = uniqueLabel('e2eneutral');
        await admin.queueImport({
            pacsId,
            aeTitle: RECEIVER_AE,
            port: RECEIVER_PORT,
            projectId: project,
            forceImport: true,
            retrieveLevel: 'STUDY',
            requestId,
            studies: [...new Map(studies.map(s => [s.studyInstanceUid, s])).values()].map(s => ({
                studyInstanceUid: s.studyInstanceUid,
                relabelMap: { 'Relabel Patient Name': s.patientName, 'Relabel Patient ID': s.patientId },
            })),
        });
        const outcomes = await waitForOutcome(admin, requestId);
        expect(
            outcomes.map(o => o.status),
            'neutralizing the stored relabels',
        ).toEqual(outcomes.map(() => 'RECEIVED'));
    } finally {
        await admin.deleteProjectQuietly(project);
    }
}
