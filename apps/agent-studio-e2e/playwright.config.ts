import { defineConfig, devices } from '@playwright/test';
import { nxE2EPreset } from '@nx/playwright/preset';
import { workspaceRoot } from '@nx/devkit';

// For CI, you may want to set BASE_URL to the deployed application.
const baseURL = process.env['BASE_URL'] || 'http://localhost:4200';

// Backend service URLs for API testing
const gatewayUrl = process.env['GATEWAY_URL'] || 'http://localhost:18080';

// Export for use in test files
export const TEST_CONFIG = {
  gatewayUrl,
  baseURL,
};

/**
 * Playwright Configuration for Agent Studio E2E Tests
 *
 * Test Coverage:
 * - Agent creation wizard workflow
 * - Template library browsing and creation
 * - Version control and rollback
 * - Testing sandbox interactions
 * - Guardrail configuration
 */
export default defineConfig({
  ...nxE2EPreset(__filename, { testDir: './src' }),

  /* Test execution settings */
  timeout: 60 * 1000, // 60 seconds per test
  expect: {
    timeout: 10 * 1000, // 10 seconds for assertions
  },

  /* Shared settings for all projects */
  use: {
    baseURL,
    /* Collect trace when retrying the failed test */
    trace: 'on-first-retry',
    /* Take screenshot on failure */
    screenshot: 'only-on-failure',
    /* Record video on failure */
    video: 'retain-on-failure',
    /* Default viewport size */
    viewport: { width: 1920, height: 1080 },
  },

  /* Run your local dev server before starting the tests */
  webServer: {
    command: 'npx nx run clinical-portal:serve --port=4200',
    url: 'http://localhost:4200',
    reuseExistingServer: true,
    timeout: 180 * 1000,
    cwd: workspaceRoot,
  },

  /* Browser projects */
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
  ],

  /* Reporter to use */
  reporter: [
    ['html', { outputFolder: 'apps/agent-studio-e2e/test-results/html' }],
    ['json', { outputFile: 'apps/agent-studio-e2e/test-results/results.json' }],
    ['list'],
  ],

  /* Folder for test artifacts */
  outputDir: 'apps/agent-studio-e2e/test-results',
});
