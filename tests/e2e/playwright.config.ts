import { defineConfig } from '@playwright/test';
import * as path from 'path';
import * as dotenv from 'dotenv';

dotenv.config({ path: path.resolve(__dirname, '.env') });

const BASE_URL = process.env.XNAT_URL || 'http://localhost';
const ADMIN_STATE = path.resolve(__dirname, '.auth/admin.json');

export default defineConfig({
    testDir: './tests',

    // Preflight: plugin, PACS, receiver, fixture studies and test accounts.
    // Fails the whole run with the reason if any is missing. See the file.
    globalSetup: require.resolve('./global.setup.ts'),

    // A real import is a search, a queue, a scheduler tick, a C-MOVE and an
    // archive. The waits inside tests have their own deadlines; this is the
    // outer bound.
    timeout: 300_000,
    expect: { timeout: 15_000 },

    // DQR's access gates, retry settings, scheduler frequency and PACS list are
    // site-wide. Two specs at once would change them under each other, and a
    // test would assert against a state it did not set up. Sequential
    // execution is a correctness requirement, not a performance choice.
    fullyParallel: false,
    workers: 1,
    retries: 0,

    reporter: [['html', { open: 'never' }], ['list']],

    use: {
        baseURL: BASE_URL,
        ignoreHTTPSErrors: true,
        screenshot: 'only-on-failure',
        trace: 'retain-on-failure',
        actionTimeout: 15_000,
        navigationTimeout: 30_000,
    },

    // One project per KIND of test, matching the directories under tests/.
    projects: [
        { name: 'setup', testMatch: /global-setup\.ts$/ },
        // PACS registry, schedules, site settings and project switches.
        { name: 'api', testDir: './tests/api', dependencies: ['setup'] },
        // Searching the PACS: form criteria and CSV batches.
        { name: 'query', testDir: './tests/query', dependencies: ['setup'] },
        // Real retrieves into XNAT, followed to the archived session.
        { name: 'import', testDir: './tests/import', dependencies: ['setup'] },
        // Queue and history: scheduling, removal, retries, what is recorded.
        { name: 'queue', testDir: './tests/queue', dependencies: ['setup'] },
        // Send to PACS.
        { name: 'export', testDir: './tests/export', dependencies: ['setup'] },
        // The access gates and project permissions, as each test account.
        { name: 'permissions', testDir: './tests/permissions', dependencies: ['setup'] },
        // The pages, driven in a real browser.
        {
            name: 'browser',
            testDir: './tests/ui',
            dependencies: ['setup'],
            use: { storageState: ADMIN_STATE },
        },
    ],
});
