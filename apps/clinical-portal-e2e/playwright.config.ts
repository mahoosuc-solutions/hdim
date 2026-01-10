import { defineConfig, devices } from '@playwright/test';
import { nxE2EPreset } from '@nx/playwright/preset';
import { workspaceRoot } from '@nx/devkit';

// For CI, you may want to set BASE_URL to the deployed application.
const baseURL = process.env['BASE_URL'] || 'http://localhost:4200';

// Backend service URLs for API testing
const gatewayUrl = process.env['GATEWAY_URL'] || 'http://localhost:8001';
const gatewayEdgeUrl = process.env['GATEWAY_EDGE_URL'] || 'http://localhost:8080';
const externalFhirUrl = process.env['EXTERNAL_FHIR_URL'] || 'http://localhost:8088';
const jaegerUrl = process.env['JAEGER_URL'] || 'http://localhost:16686';

// Export for use in test files
export const TEST_CONFIG = {
  gatewayUrl,
  gatewayEdgeUrl,
  externalFhirUrl,
  jaegerUrl,
};

/**
 * Read environment variables from file.
 * https://github.com/motdotla/dotenv
 */
// require('dotenv').config();

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  ...nxE2EPreset(__filename, { testDir: './src' }),
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    baseURL,
    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: 'on-first-retry',
    /* Take screenshot on failure */
    screenshot: 'only-on-failure',
    /* Record video on failure */
    video: 'retain-on-failure',
  },
  /* Run your local dev server before starting the tests */
  webServer: {
    command: 'npx nx run clinical-portal:serve --port=4200',
    url: 'http://localhost:4200',
    reuseExistingServer: true,
    timeout: 180 * 1000,
    cwd: workspaceRoot,
  },
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

    // Uncomment for mobile browsers support
    /* {
      name: 'Mobile Chrome',
      use: { ...devices['Pixel 5'] },
    },
    {
      name: 'Mobile Safari',
      use: { ...devices['iPhone 12'] },
    }, */

    // Uncomment for branded browsers
    /* {
      name: 'Microsoft Edge',
      use: { ...devices['Desktop Edge'], channel: 'msedge' },
    },
    {
      name: 'Google Chrome',
      use: { ...devices['Desktop Chrome'], channel: 'chrome' },
    } */
  ],
});
