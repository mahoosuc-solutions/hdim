/**
 * Playwright E2E Test Configuration for HealthData Platform
 *
 * Comprehensive configuration supporting:
 * - Multi-browser testing (Chrome, Firefox, Safari)
 * - Mobile device testing (iOS, Android)
 * - Parallel execution
 * - Video & screenshot capture
 * - Multiple reporting formats
 * - CI/CD integration
 * - Performance monitoring
 * - Accessibility testing
 *
 * @author TDD Swarm Agent 5A
 * @version 1.0.0
 */

import { defineConfig, devices } from '@playwright/test';
import type { PlaywrightTestConfig } from '@playwright/test';

/**
 * Environment configuration
 */
const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const CI = !!process.env.CI;
const WORKERS = process.env.WORKERS ? parseInt(process.env.WORKERS, 10) : (CI ? 2 : undefined);
const RETRIES = process.env.RETRIES ? parseInt(process.env.RETRIES, 10) : (CI ? 2 : 0);
const HEADLESS = process.env.HEADLESS !== 'false';
const TIMEOUT = parseInt(process.env.TIMEOUT || '30000', 10);
const GLOBAL_TIMEOUT = parseInt(process.env.GLOBAL_TIMEOUT || '600000', 10);

/**
 * Playwright configuration
 */
export default defineConfig({
  /**
   * Test directory configuration
   */
  testDir: './tests',

  /**
   * Test file pattern matching
   */
  testMatch: '**/*.spec.ts',

  /**
   * Output directory for test artifacts
   */
  outputDir: './test-results',

  /**
   * Run tests in files in parallel
   */
  fullyParallel: true,

  /**
   * Fail the build on CI if you accidentally left test.only in the source code
   */
  forbidOnly: CI,

  /**
   * Retry configuration
   * - CI: Retry failed tests twice to handle flakiness
   * - Local: No retries for faster feedback
   */
  retries: RETRIES,

  /**
   * Worker configuration
   * - CI: Limited workers to avoid resource contention
   * - Local: Optimal workers based on CPU cores
   */
  workers: WORKERS,

  /**
   * Maximum time one test can run for
   */
  timeout: TIMEOUT,

  /**
   * Maximum time entire test suite can run
   */
  globalTimeout: GLOBAL_TIMEOUT,

  /**
   * Maximum time to wait for an action like click to succeed
   */
  expect: {
    /**
     * Timeout for assertions
     */
    timeout: 10000,

    /**
     * Custom matchers for better error messages
     */
    toHaveScreenshot: {
      maxDiffPixels: 100,
      threshold: 0.2,
    },
  },

  /**
   * Reporter configuration
   * Multiple reporters for comprehensive test results
   */
  reporter: [
    // HTML report for local development
    ['html', {
      outputFolder: 'playwright-report',
      open: CI ? 'never' : 'on-failure',
    }],

    // JUnit XML for CI/CD integration
    ['junit', {
      outputFile: 'test-results/junit.xml',
      includeProjectInTestName: true,
    }],

    // JSON report for custom processing
    ['json', {
      outputFile: 'test-results/test-results.json',
    }],

    // GitHub Actions integration
    ...(CI ? [['github' as const]] : []),

    // Console output with detailed information
    ['list', {
      printSteps: !CI,
    }],

    // Allure report (if enabled)
    ...(process.env.ALLURE_RESULTS_DIR ? [
      ['allure-playwright', {
        resultsDir: process.env.ALLURE_RESULTS_DIR,
      }] as const
    ] : []),
  ],

  /**
   * Shared settings for all projects
   */
  use: {
    /**
     * Base URL for navigation
     */
    baseURL: BASE_URL,

    /**
     * Collect trace when retrying the failed test
     */
    trace: CI ? 'on-first-retry' : 'retain-on-failure',

    /**
     * Screenshot configuration
     */
    screenshot: 'only-on-failure',

    /**
     * Video recording configuration
     */
    video: CI ? 'retain-on-failure' : 'off',

    /**
     * Viewport size (can be overridden per project)
     */
    viewport: { width: 1280, height: 720 },

    /**
     * Browser context options
     */
    ignoreHTTPSErrors: true,

    /**
     * Permissions
     */
    permissions: ['clipboard-read', 'clipboard-write'],

    /**
     * Locale and timezone
     */
    locale: 'en-US',
    timezoneId: 'America/New_York',

    /**
     * Default action timeout
     */
    actionTimeout: 15000,

    /**
     * Navigation timeout
     */
    navigationTimeout: 30000,

    /**
     * Extra HTTP headers
     */
    extraHTTPHeaders: {
      'Accept-Language': 'en-US,en;q=0.9',
    },

    /**
     * Context options for API testing
     */
    contextOptions: {
      recordVideo: {
        dir: './test-results/videos',
        size: { width: 1280, height: 720 },
      },
    },
  },

  /**
   * Test projects for multiple browsers and devices
   */
  projects: [
    /**
     * Setup project - runs before all tests
     * Handles authentication and global setup
     */
    {
      name: 'setup',
      testMatch: /global\.setup\.ts/,
    },

    /**
     * Desktop Chrome
     */
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
      },
      dependencies: ['setup'],
    },

    /**
     * Desktop Firefox
     */
    {
      name: 'firefox',
      use: {
        ...devices['Desktop Firefox'],
      },
      dependencies: ['setup'],
    },

    /**
     * Desktop Safari (WebKit)
     */
    {
      name: 'webkit',
      use: {
        ...devices['Desktop Safari'],
      },
      dependencies: ['setup'],
    },

    /**
     * Edge Browser
     */
    {
      name: 'edge',
      use: {
        ...devices['Desktop Edge'],
        channel: 'msedge',
      },
      dependencies: ['setup'],
    },

    /**
     * Mobile Chrome (Pixel 5)
     */
    {
      name: 'mobile-chrome',
      use: {
        ...devices['Pixel 5'],
      },
      dependencies: ['setup'],
    },

    /**
     * Mobile Safari (iPhone 12)
     */
    {
      name: 'mobile-safari',
      use: {
        ...devices['iPhone 12'],
      },
      dependencies: ['setup'],
    },

    /**
     * Tablet iPad Pro
     */
    {
      name: 'tablet-ipad',
      use: {
        ...devices['iPad Pro'],
      },
      dependencies: ['setup'],
    },

    /**
     * API Testing (no browser required)
     */
    {
      name: 'api',
      testMatch: /.*\.api\.spec\.ts/,
      use: {
        baseURL: BASE_URL,
      },
    },

    /**
     * Accessibility Testing
     */
    {
      name: 'accessibility',
      testMatch: /accessibility\.spec\.ts/,
      use: {
        ...devices['Desktop Chrome'],
      },
      dependencies: ['setup'],
    },

    /**
     * Performance Testing
     */
    {
      name: 'performance',
      testMatch: /performance\.spec\.ts/,
      use: {
        ...devices['Desktop Chrome'],
      },
      dependencies: ['setup'],
    },
  ],

  /**
   * Web Server configuration
   * Starts the application before running tests
   */
  webServer: process.env.SKIP_WEBSERVER ? undefined : {
    command: 'npm run serve',
    url: BASE_URL,
    reuseExistingServer: !CI,
    timeout: 120000,
    stdout: 'pipe',
    stderr: 'pipe',
  },

  /**
   * Global setup script
   */
  globalSetup: './global.setup.ts',

  /**
   * Global teardown script
   */
  globalTeardown: './global.teardown.ts',

  /**
   * Grep pattern for test filtering
   * Use tags like @api, @workflow, @security, etc.
   */
  grep: process.env.GREP ? new RegExp(process.env.GREP) : undefined,
  grepInvert: process.env.GREP_INVERT ? new RegExp(process.env.GREP_INVERT) : undefined,

  /**
   * Maximum number of test failures before stopping the run
   */
  maxFailures: process.env.MAX_FAILURES ? parseInt(process.env.MAX_FAILURES, 10) : undefined,

  /**
   * Preserve output directory for debugging
   */
  preserveOutput: CI ? 'failures-only' : 'always',

  /**
   * Update snapshots when running locally
   */
  updateSnapshots: process.env.UPDATE_SNAPSHOTS === 'true' ? 'all' : 'missing',
});
