import js from '@eslint/js';
import tseslint from 'typescript-eslint';
import playwright from 'eslint-plugin-playwright';
import prettier from 'eslint-config-prettier';

export default tseslint.config(
    { ignores: ['node_modules/', 'test-results/', 'playwright-report/', '.auth/'] },
    js.configs.recommended,
    ...tseslint.configs.recommended,
    {
        files: ['tests/**/*.ts'],
        ...playwright.configs['flat/recommended'],
        rules: {
            ...playwright.configs['flat/recommended'].rules,

            // A refusal's status and message are part of what is tested, so
            // several specs read a response and branch on it to clean up what
            // a wrongly accepted request created before asserting.
            'playwright/no-conditional-in-test': 'off',

            // Specs that check every fixture study or every setting do it in
            // a loop over the fixture, which is the point of those tests.
            'playwright/no-conditional-expect': 'off',
        },
    },
    {
        // The setup project's logins are steps, not tests; the assertions that
        // matter live in the login helper.
        files: ['tests/global-setup.ts'],
        rules: { 'playwright/expect-expect': 'off' },
    },
    {
        rules: {
            // Server responses are untyped JSON; the assertion that follows a
            // read is what checks its shape.
            '@typescript-eslint/no-explicit-any': 'off',
            '@typescript-eslint/no-non-null-assertion': 'off',
            'no-empty': ['error', { allowEmptyCatch: true }],
        },
    },
    prettier,
);
