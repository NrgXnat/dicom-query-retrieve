/**
 * Environment-derived settings shared by every spec.
 *
 * Accounts other than the admin are created by global setup if they do not
 * exist, so a fresh stack needs only the admin credentials plus a password for
 * each test account.
 */
export const XNAT_URL = process.env.XNAT_URL || 'http://localhost';

export const ADMIN = {
    username: process.env.ADMIN_USER || 'admin',
    password: process.env.ADMIN_PASS || 'admin',
};

/** Holds the DQR Access role and is a member of the projects it imports into. */
export const DQR_ROLE_USER = {
    username: process.env.DQR_ROLE_USER || 'e2edqrrole',
    password: process.env.DQR_ROLE_USER_PASS || '',
};

/** A project member WITHOUT the DQR Access role. */
export const NO_DQR_USER = {
    username: process.env.NO_DQR_USER || 'e2enodqr',
    password: process.env.NO_DQR_USER_PASS || '',
};

/** Holds the DQR Access role, but is only a collaborator (read only) on the project. */
export const READONLY_USER = {
    username: process.env.READONLY_USER || 'e2edqrreadonly',
    password: process.env.READONLY_USER_PASS || '',
};

export type Account = { username: string; password: string };

/** The PACS the suite searches and retrieves from, by label. */
export const PACS_LABEL = process.env.DQR_PACS_LABEL || 'ORTHANC';

/** The XNAT receiver the PACS sends to. */
export const RECEIVER_AE = process.env.DQR_RECEIVER_AE || 'XDQR';
export const RECEIVER_PORT = Number(process.env.DQR_RECEIVER_PORT || 8104);

export const TARGET_BRANCH = (process.env.TARGET_BRANCH ?? 'develop').toLowerCase();

/** A project ID unique to this run, so two runs never collide. XNAT caps IDs at 24 characters here. */
export function projectNameFor(suite: string): string {
    const stamp = `${Date.now().toString(36)}${Math.floor(Math.random() * 1e3)}`;
    return `DQR_E2E_${suite}_${stamp}`.slice(0, 24);
}

/** A label unique within a run. */
export function uniqueLabel(prefix: string): string {
    return `${prefix}_${Date.now().toString(36)}${Math.floor(Math.random() * 1e3)}`;
}
