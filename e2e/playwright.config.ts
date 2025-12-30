import { defineConfig, devices } from '@playwright/test';
import * as path from 'path';

/**
 * HDIM Unified E2E Test Configuration
 *
 * This configuration supports testing both the Angular Clinical Portal
 * and React Sales Portal with shared utilities and fixtures.
 *
 * @see https://playwright.dev/docs/test-configuration
 */

// Environment configuration
const BASE_URL = process.env.BASE_URL || 'http://localhost:4200';
const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8001';
const CI = !!process.env.CI;
const USE_DOCKER = process.env.USE_DOCKER === 'true';

// Auth state file location (relative to e2e directory)
const AUTH_STATE = path.resolve(__dirname, '.auth/user.json');

export default defineConfig({
  // Test directory
  testDir: './tests',

  // Test file patterns
  testMatch: '**/*.spec.ts',

  // Run tests in parallel
  fullyParallel: true,

  // Fail the build on CI if you accidentally left test.only in the source code
  forbidOnly: CI,

  // Retry failed tests
  retries: CI ? 2 : 0,

  // Limit parallel workers
  workers: CI ? 4 : undefined,

  // Reporter configuration
  reporter: [
    ['html', { open: 'never', outputFolder: 'playwright-report' }],
    ['junit', { outputFile: 'test-results/junit.xml' }],
    ['json', { outputFile: 'test-results/results.json' }],
    CI ? ['github'] : ['line'],
  ],

  // Output directory for test artifacts
  outputDir: 'test-results',

  // Global setup and teardown
  globalSetup: path.resolve(__dirname, 'global.setup.ts'),
  globalTeardown: path.resolve(__dirname, 'global.teardown.ts'),

  // Shared settings for all projects
  use: {
    // Base URL for navigation
    baseURL: BASE_URL,

    // Collect trace when retrying the failed test
    trace: 'on-first-retry',

    // Capture screenshot on failure
    screenshot: 'only-on-failure',

    // Record video on failure
    video: 'retain-on-failure',

    // Action timeout
    actionTimeout: 15000,

    // Navigation timeout
    navigationTimeout: 30000,

    // Extra HTTP headers for API requests
    extraHTTPHeaders: {
      'Accept': 'application/json',
    },

    // Viewport size
    viewport: { width: 1280, height: 720 },

    // Ignore HTTPS errors (for local testing)
    ignoreHTTPSErrors: true,
  },

  // Test projects for different browsers/devices
  // Note: globalSetup/globalTeardown handle auth, so no setup project dependencies needed
  projects: [
    // Desktop browsers
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        storageState: AUTH_STATE,
      },
    },
    {
      name: 'firefox',
      use: {
        ...devices['Desktop Firefox'],
        storageState: AUTH_STATE,
      },
    },
    {
      name: 'webkit',
      use: {
        ...devices['Desktop Safari'],
        storageState: AUTH_STATE,
      },
    },

    // Mobile browsers
    {
      name: 'mobile-chrome',
      use: {
        ...devices['Pixel 5'],
        storageState: AUTH_STATE,
      },
    },
    {
      name: 'mobile-safari',
      use: {
        ...devices['iPhone 12'],
        storageState: AUTH_STATE,
      },
    },

    // Tablet
    {
      name: 'tablet',
      use: {
        ...devices['iPad Pro 11'],
        storageState: AUTH_STATE,
      },
    },

    // Smoke tests - quick critical path verification
    {
      name: 'smoke',
      testMatch: '**/smoke/**/*.spec.ts',
      use: {
        ...devices['Desktop Chrome'],
        storageState: AUTH_STATE,
      },
    },

    // Security tests - tenant isolation, RBAC, audit
    {
      name: 'security',
      testMatch: '**/security/**/*.spec.ts',
      use: {
        ...devices['Desktop Chrome'],
        storageState: AUTH_STATE,
      },
    },

    // Accessibility tests (run on chromium only)
    {
      name: 'accessibility',
      testMatch: '**/accessibility/**/*.spec.ts',
      use: {
        ...devices['Desktop Chrome'],
        storageState: AUTH_STATE,
      },
    },

    // Performance tests (run on chromium only)
    {
      name: 'performance',
      testMatch: '**/performance/**/*.spec.ts',
      use: {
        ...devices['Desktop Chrome'],
        storageState: AUTH_STATE,
      },
    },
  ],

  // Web server configuration
  // In CI/Docker mode, services are started externally via docker-compose
  // In local development, start the frontend and wait for backend
  webServer: USE_DOCKER || CI
    ? undefined
    : [
        {
          command: 'cd ../frontend && npm run start',
          url: 'http://localhost:4200',
          reuseExistingServer: true,
          timeout: 120000,
          stdout: 'pipe',
          stderr: 'pipe',
        },
      ],

  // Expect configuration
  expect: {
    // Maximum time expect() should wait for the condition to be met
    timeout: 10000,

    // Configure snapshot testing
    toHaveScreenshot: {
      maxDiffPixels: 100,
    },
    toMatchSnapshot: {
      maxDiffPixelRatio: 0.1,
    },
  },
});
