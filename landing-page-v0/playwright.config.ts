import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright configuration for HDIM Landing Page E2E tests
 */
export default defineConfig({
  testDir: './tests/e2e',
  outputDir: 'test-results/artifacts',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html', { outputFolder: 'test-results/html' }],
    ['json', { outputFile: 'test-results/results.json' }],
    ['list'],
  ],
  use: {
    baseURL: process.env.BASE_URL || `http://localhost:${process.env.E2E_PORT || '3000'}`,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
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
    {
      name: 'Mobile Chrome',
      use: { ...devices['Pixel 5'] },
    },
    {
      name: 'Mobile Safari',
      use: { ...devices['iPhone 12'] },
    },
    {
      name: 'Tablet',
      use: { ...devices['iPad Pro'] },
    },
  ],

  webServer: process.env.CI
    ? undefined
    : {
        command: `PORT=${process.env.E2E_PORT || '3000'} npm run dev -- -p ${process.env.E2E_PORT || '3000'} -H 127.0.0.1`,
        url: `http://localhost:${process.env.E2E_PORT || '3000'}`,
        reuseExistingServer: !process.env.CI,
        timeout: 120 * 1000,
      },
});
