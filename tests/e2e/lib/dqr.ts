/**
 * REST client for the DQR plugin and the core XNAT calls the suite needs.
 *
 * Authenticates with HTTP basic auth, one client per account, so a spec can
 * hold an admin client and a restricted client side by side and compare what
 * each is allowed to do.
 *
 * Two kinds of method:
 *   - `raw*` and the plain HTTP verbs return the response untouched, for tests
 *     that assert on a refusal.
 *   - Everything else asserts success and returns parsed JSON, for setup and
 *     for the positive path.
 */
import { APIRequestContext, APIResponse, expect, request } from '@playwright/test';
import { Account, XNAT_URL } from './env';
import { GeneratedStudy } from './dicom';
import { zip } from './zip';

export type RetrieveLevel = 'SERIES' | 'STUDY';

export interface PacsSettings {
    aeTitle: string;
    label: string;
    host?: string;
    queryRetrievePort?: number;
    queryable?: boolean;
    storable?: boolean;
    defaultQueryRetrievePacs?: boolean;
    defaultStoragePacs?: boolean;
    dicomWebEnabled?: boolean;
    dicomWebRootUrl?: string;
    dicomObjectIdentifier?: string;
    anonymizationEnabled?: boolean;
    retrieveLevel?: RetrieveLevel;
    ormStrategySpringBeanId?: string;
    supportsExtendedNegotiations?: boolean;
}

export interface Pacs extends PacsSettings {
    id: number;
}

export interface StudyImport {
    studyInstanceUid: string;
    seriesInstanceUids?: string[];
    seriesDescriptions?: string[];
    relabelMap?: Record<string, string>;
    anonScript?: string;
}

export interface ImportRequest {
    pacsId: number;
    aeTitle: string;
    port: number;
    projectId: string;
    forceImport?: boolean;
    retrieveLevel?: RetrieveLevel;
    requestId?: string;
    studies: StudyImport[];
}

export interface PacsRequestRow {
    id: number;
    status: 'QUEUED' | 'PROCESSING' | 'ISSUED' | 'FAILED' | 'RECEIVED';
    username: string;
    studyInstanceUid: string;
    studyId?: string;
    accessionNumber?: string;
    studyDate?: string;
    xnatProject: string;
    pacsId: number;
    retrieveLevel: RetrieveLevel;
    seriesIds: string[];
    patientId?: string;
    patientName?: string;
    requestId?: string;
    destinationAeTitle: string;
    errorMessage?: string;
    queuedTime: number;
    executedTime?: number;
    priority?: number;
    retries?: number;
}

export interface StudyResult {
    studyInstanceUid: string;
    studyId?: string;
    studyDescription?: string;
    accessionNumber?: string;
    studyDate?: string;
    modalitiesInStudy?: string[];
    patient?: { id?: string; name?: string; sex?: string };
}

/** The ORM strategy bean the admin page always uses for a new PACS. */
export const DEFAULT_ORM_STRATEGY = 'dicomOrmStrategy';

export class XnatClient {
    private constructor(
        readonly ctx: APIRequestContext,
        readonly account: Account,
    ) {}

    static async as(account: Account): Promise<XnatClient> {
        const ctx = await request.newContext({
            baseURL: XNAT_URL,
            ignoreHTTPSErrors: true,
            httpCredentials: { username: account.username, password: account.password, send: 'always' },
            // Start with no cookies. Inside a project that uses a saved browser
            // login, a request context otherwise starts with that login's
            // session cookie, and a request made as another user over it ends
            // the browser's session.
            storageState: { cookies: [], origins: [] },
        });
        return new XnatClient(ctx, account);
    }

    async dispose(): Promise<void> {
        await this.ctx.dispose();
    }

    get(url: string, params?: Record<string, string | number | boolean>): Promise<APIResponse> {
        return this.ctx.get(url, { params });
    }

    post(
        url: string,
        data?: unknown,
        params?: Record<string, string | number | boolean>,
    ): Promise<APIResponse> {
        return this.ctx.post(url, { data, params });
    }

    put(
        url: string,
        data?: unknown,
        params?: Record<string, string | number | boolean>,
    ): Promise<APIResponse> {
        return this.ctx.put(url, { data, params });
    }

    delete(url: string, params?: Record<string, string | number | boolean>): Promise<APIResponse> {
        return this.ctx.delete(url, { params });
    }

    private async ok<T>(res: APIResponse, what: string): Promise<T> {
        const body = await res.text();
        expect(res.ok(), `${what} failed: HTTP ${res.status()} ${body.slice(0, 300)}`).toBeTruthy();
        return (body ? JSON.parse(body) : undefined) as T;
    }

    // ---------------------------------------------------------------- settings

    async getSettings(): Promise<Record<string, unknown>> {
        return this.ok(await this.get('/xapi/dqr/settings'), 'GET /xapi/dqr/settings');
    }

    rawSetSettings(values: Record<string, unknown>): Promise<APIResponse> {
        return this.post('/xapi/dqr/settings', values);
    }

    async setSettings(values: Record<string, unknown>): Promise<void> {
        await this.ok(await this.post('/xapi/dqr/settings', values), 'POST /xapi/dqr/settings');
    }

    // -------------------------------------------------------------------- pacs

    async listPacs(filter: { queryable?: boolean; storable?: boolean } = {}): Promise<Pacs[]> {
        return this.ok(await this.get('/xapi/pacs', filter), 'GET /xapi/pacs');
    }

    async getPacs(id: number): Promise<Pacs> {
        return this.ok(await this.get(`/xapi/pacs/${id}`), `GET /xapi/pacs/${id}`);
    }

    async pacsByLabel(label: string): Promise<Pacs> {
        const match = (await this.listPacs()).find(p => p.label === label);
        expect(match, `no PACS labeled "${label}" is configured`).toBeDefined();
        return match!;
    }

    rawCreatePacs(settings: PacsSettings): Promise<APIResponse> {
        return this.post('/xapi/pacs', { ormStrategySpringBeanId: DEFAULT_ORM_STRATEGY, ...settings });
    }

    async createPacs(settings: PacsSettings): Promise<Pacs> {
        const pacs = await this.ok<Pacs | undefined>(await this.rawCreatePacs(settings), 'POST /xapi/pacs');
        expect(pacs?.id, `creating PACS "${settings.label}" returned no PACS`).toBeTruthy();
        return pacs!;
    }

    rawUpdatePacs(id: number, settings: PacsSettings): Promise<APIResponse> {
        return this.put(`/xapi/pacs/${id}`, { ormStrategySpringBeanId: DEFAULT_ORM_STRATEGY, ...settings });
    }

    async deletePacsQuietly(id: number): Promise<void> {
        await this.delete(`/xapi/pacs/${id}`).catch(() => undefined);
    }

    async ping(id: number): Promise<{ successful: boolean; pacsId: number }> {
        return this.ok(await this.get(`/xapi/pacs/${id}/status`), `GET /xapi/pacs/${id}/status`);
    }

    // ---------------------------------------------------------------- schedule

    async getAvailability(pacsId: number): Promise<Record<string, any[]>> {
        return this.ok(await this.get(`/xapi/pacs/${pacsId}/availability`), 'GET availability');
    }

    rawCreateAvailability(pacsId: number, window: Record<string, unknown>): Promise<APIResponse> {
        return this.post(`/xapi/pacs/${pacsId}/availability`, { pacsId, ...window });
    }

    /** A window for every day, all day, as the admin page creates for a new PACS. */
    async openAllWeek(pacsId: number, threads = 1, utilizationPercent = 100): Promise<void> {
        for (const dayOfWeek of [
            'MONDAY',
            'TUESDAY',
            'WEDNESDAY',
            'THURSDAY',
            'FRIDAY',
            'SATURDAY',
            'SUNDAY',
        ]) {
            await this.ok(
                await this.rawCreateAvailability(pacsId, {
                    dayOfWeek,
                    availabilityStart: '00:00',
                    availabilityEnd: '24:00',
                    threads,
                    utilizationPercent,
                }),
                `POST availability ${dayOfWeek}`,
            );
        }
    }

    // ------------------------------------------------------------------ search

    /** `timeoutMs` for the rare search that is expected to wait on a PACS that does not answer. */
    rawQueryStudies(criteria: Record<string, unknown>, timeoutMs?: number): Promise<APIResponse> {
        return this.ctx.post('/xapi/dqr/query/studies', { data: criteria, timeout: timeoutMs });
    }

    /** Study search; an empty array when the PACS answered 204. */
    async queryStudies(criteria: Record<string, unknown>): Promise<StudyResult[]> {
        const res = await this.rawQueryStudies(criteria);
        if (res.status() === 204) return [];
        return this.ok(res, 'POST /xapi/dqr/query/studies');
    }

    rawQuerySeries(pacsId: number, studyInstanceUids: string[]): Promise<APIResponse> {
        return this.post('/xapi/dqr/query/series', { pacsId, studyInstanceUids });
    }

    async querySeries(pacsId: number, studyInstanceUid: string): Promise<any[]> {
        const byStudy = await this.ok<Record<string, { results: any[] }>>(
            await this.rawQuerySeries(pacsId, [studyInstanceUid]),
            'POST /xapi/dqr/query/series',
        );
        return byStudy[studyInstanceUid]?.results ?? [];
    }

    rawQueryBatch(pacsId: number, csv: string, fileName = 'batch.csv'): Promise<APIResponse> {
        return this.ctx.post('/xapi/dqr/query/batch', {
            multipart: {
                pacsId: String(pacsId),
                csv_to_store: { name: fileName, mimeType: 'text/csv', buffer: Buffer.from(csv, 'utf-8') },
            },
        });
    }

    // ------------------------------------------------------------------ import

    rawImport(body: ImportRequest): Promise<APIResponse> {
        return this.post('/xapi/dqr/import', body);
    }

    async queueImport(body: ImportRequest): Promise<PacsRequestRow[]> {
        return this.ok(await this.rawImport(body), 'POST /xapi/dqr/import');
    }

    // ----------------------------------------------------------- queue/history

    async queue(all = false): Promise<PacsRequestRow[]> {
        return this.ok(await this.get('/xapi/dqr/import/queue', { all }), 'GET queue');
    }

    async history(all = false): Promise<PacsRequestRow[]> {
        return this.ok(await this.get('/xapi/dqr/import/history', { all, pageSize: 1000 }), 'GET history');
    }

    async removeFromQueueQuietly(ids: number[]): Promise<void> {
        if (ids.length) await this.post('/xapi/dqr/import/queue', ids).catch(() => undefined);
    }

    // ------------------------------------------------------------------ export

    rawExport(pacsId: number, sessionId: string, scans: string[]): Promise<APIResponse> {
        const qs = new URLSearchParams({ pacsId: String(pacsId), session: sessionId });
        for (const s of scans) qs.append('scansToExport', s);
        return this.ctx.put(`/xapi/dqr/export?${qs.toString()}`);
    }

    // -------------------------------------------------------- project settings

    rawSetProjectEnabled(projectId: string, enabled: boolean): Promise<APIResponse> {
        return this.put(`/xapi/dqr/settings/project/${encodeURIComponent(projectId)}`, {
            projectId,
            enabled,
        });
    }

    async deleteProjectSettingsQuietly(projectId: string): Promise<void> {
        await this.delete(`/xapi/dqr/settings/project/${encodeURIComponent(projectId)}`).catch(
            () => undefined,
        );
    }

    // -------------------------------------------------------------- core XNAT

    async ensureProject(id: string): Promise<void> {
        const res = await this.ctx.put(`/data/projects/${encodeURIComponent(id)}`, {
            params: { secondary_ID: id, name: id },
        });
        expect(res.ok(), `create project ${id}: HTTP ${res.status()}`).toBeTruthy();
    }

    /**
     * A project that accepts DQR imports whatever the site's project gate is
     * set to: created, then switched on for DQR. Specs that are not testing
     * the gate use this, so they never depend on how a stack is configured.
     */
    async ensureDqrProject(id: string): Promise<void> {
        await this.ensureProject(id);
        const res = await this.rawSetProjectEnabled(id, true);
        expect([200, 304], `switch ${id} on for DQR: HTTP ${res.status()}`).toContain(res.status());
    }

    /**
     * Deletes a project and its DQR switch row. XNAT does not remove the DQR
     * row when the project goes, so without the first call every deleted test
     * project would leave a row behind.
     */
    async deleteProjectQuietly(id: string): Promise<void> {
        await this.deleteProjectSettingsQuietly(id);
        await this.ctx
            .delete(`/data/projects/${encodeURIComponent(id)}`, { params: { removeFiles: 'true' } })
            .catch(() => undefined);
    }

    /** Every project's DQR switch row (admin only). */
    async listProjectSettings(): Promise<Array<{ projectId: string; enabled: boolean }>> {
        return this.ok(await this.get('/xapi/dqr/settings/project'), 'GET /xapi/dqr/settings/project');
    }

    /** Ids of every project visible to this account. */
    async listProjectIds(): Promise<string[]> {
        const res = await this.get('/data/projects', { format: 'json' });
        const rows: any[] = (await this.ok<any>(res, 'GET /data/projects')).ResultSet.Result;
        return rows.map(r => String(r.ID));
    }

    /** Prearchive rows, in any project, for a session label. */
    async prearchiveRows(
        sessionLabel: string,
    ): Promise<Array<{ project: string; status: string; name: string }>> {
        const res = await this.get('/data/prearchive', { format: 'json' });
        if (!res.ok()) return [];
        const rows: any[] = (await res.json()).ResultSet.Result;
        return rows.filter(r => r.name === sessionLabel);
    }

    /** Adds a user to a project group: Owners, Members or Collaborators. */
    async addProjectUser(projectId: string, username: string, group: 'Owners' | 'Members' | 'Collaborators') {
        const res = await this.ctx.put(
            `/data/projects/${encodeURIComponent(projectId)}/users/${group}/${encodeURIComponent(username)}`,
        );
        expect(res.ok(), `add ${username} to ${projectId} ${group}: HTTP ${res.status()}`).toBeTruthy();
    }

    /**
     * Sessions in a project, from /data/experiments?project=. The project's own
     * listing omits session types not registered as searchable (ultrasound
     * among them), so it cannot be used to prove what an import created.
     */
    async listSessions(projectId: string): Promise<Array<{ ID: string; label: string; xsiType: string }>> {
        return (
            await this.ok<any>(
                await this.get('/data/experiments', { format: 'json', project: projectId }),
                'list sessions',
            )
        ).ResultSet.Result;
    }

    async getSession(id: string): Promise<any> {
        return (
            await this.ok<any>(await this.get(`/data/experiments/${id}`, { format: 'json' }), 'GET session')
        ).items[0];
    }

    async getScans(sessionId: string): Promise<any[]> {
        return (
            await this.ok<any>(
                await this.get(`/data/experiments/${sessionId}/scans`, { format: 'json' }),
                'GET scans',
            )
        ).ResultSet.Result;
    }

    /**
     * Uploads a generated study straight into the archive as one session, and
     * returns the session's ID.
     */
    async uploadStudy(
        projectId: string,
        subject: string,
        session: string,
        study: GeneratedStudy,
    ): Promise<string> {
        const files: Record<string, Buffer> = {};
        for (const s of study.series) files[`series${s.seriesNumber}/image.dcm`] = s.file;
        const res = await this.ctx.post('/data/services/import', {
            params: {
                'import-handler': 'DICOM-zip',
                PROJECT_ID: projectId,
                SUBJECT_ID: subject,
                EXPT_LABEL: session,
                dest: '/archive',
                overwrite: 'append',
            },
            multipart: { file: { name: 'study.zip', mimeType: 'application/zip', buffer: zip(files) } },
            timeout: 120_000,
        });
        const body = (await res.text()).trim();
        expect(res.ok(), `upload: HTTP ${res.status()} ${body.slice(0, 200)}`).toBeTruthy();
        const match = (await this.listSessions(projectId)).find(s => s.label === session);
        expect(
            match,
            `uploaded session ${session} is in ${projectId} (upload answered ${body})`,
        ).toBeDefined();
        return match!.ID;
    }

    /**
     * Deletes "Unassigned" prearchive sessions carrying one of these labels,
     * which is where a misrouted import ends up. Only labels the suite made
     * are passed in, so nobody else's data is touched. Returns what it deleted.
     */
    async clearUnassignedPrearchive(sessionLabels: string[]): Promise<string[]> {
        const wanted = new Set(sessionLabels);
        const res = await this.get('/data/prearchive', { format: 'json' });
        if (!res.ok()) return [];
        const rows: any[] = (await res.json()).ResultSet.Result;
        const removed: string[] = [];
        for (const r of rows) {
            if (r.project !== 'Unassigned' || !wanted.has(r.name)) continue;
            await this.delete(
                `/data/prearchive/projects/Unassigned/${r.timestamp}/${encodeURIComponent(r.name)}`,
            );
            removed.push(r.name);
        }
        return removed;
    }

    async getSubjectLabel(subjectId: string): Promise<string> {
        const s = await this.ok<any>(
            await this.get(`/data/subjects/${subjectId}`, { format: 'json' }),
            'GET subject',
        );
        return s.items[0].data_fields.label;
    }
}
