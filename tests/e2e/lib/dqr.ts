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

    async deleteProjectQuietly(id: string): Promise<void> {
        await this.ctx
            .delete(`/data/projects/${encodeURIComponent(id)}`, { params: { removeFiles: 'true' } })
            .catch(() => undefined);
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

    async getSubjectLabel(subjectId: string): Promise<string> {
        const s = await this.ok<any>(
            await this.get(`/data/subjects/${subjectId}`, { format: 'json' }),
            'GET subject',
        );
        return s.items[0].data_fields.label;
    }
}
