/**
 * Cypress Configuration
 *
 * E2E testing configuration for Nurse Dashboard workflows
 */

import { defineConfig } from 'cypress';

export default defineConfig({
  e2e: {
    setupNodeEvents(on, config) {
      // implement node event listeners here
    },

    // Base URL for tests
    baseUrl: 'http://localhost:4200',

    // Test specs pattern
    specPattern: 'cypress/e2e/**/*.cy.{js,jsx,ts,tsx}',

    // Screenshot and video settings
    screenshotOnRunFailure: true,
    screenshotsFolder: 'cypress/screenshots',
    videosFolder: 'cypress/videos',
    video: false, // Disable video recording for faster tests

    // Timeout settings
    defaultCommandTimeout: 10000,
    requestTimeout: 10000,
    responseTimeout: 10000,

    // Browser settings
    browser: 'chrome',
    chromeWebSecurity: false, // Disable CORS for testing

    // Viewport settings
    viewportWidth: 1280,
    viewportHeight: 720,

    // Test settings
    numTestsKeptInMemory: 1,

    // Angular support
    experimentalMemoryManagement: true,
    experimentalSkipDomainInjection: true,

    // Retry settings
    retries: {
      runMode: 1, // Retry once in CI
      openMode: 0, // No retries in dev mode
    },

    // Request settings
    blockHosts: [
      '*google-analytics*',
      '*gtm.js',
      '*mixpanel*',
      '*fullstory*',
      '*intercom*',
    ],
  },

  component: {
    devServer: {
      framework: 'angular',
      bundler: 'webpack',
    },
    specPattern: 'src/**/*.cy.ts',
  },
});
