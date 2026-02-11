/**
 * Jest Configuration for Pact Consumer Contract Tests
 *
 * This configuration is specifically designed for running Pact consumer
 * contract tests separately from the main Jest test suite.
 *
 * Run with: npx jest --config jest.pact.config.js --runInBand
 *
 * Note: --runInBand is required to ensure Pact mock servers are properly
 * created and destroyed in sequence, avoiding port conflicts.
 */

/** @type {import('jest').Config} */
module.exports = {
  displayName: 'clinical-portal-pact',

  // Use base preset for Angular compatibility
  preset: '../../jest.preset.js',

  // Test environment - Node is required for Pact native bindings
  testEnvironment: 'node',

  // Only run Pact contract test files
  testMatch: [
    '<rootDir>/src/test/contracts/**/*.pact.spec.ts',
    '<rootDir>/src/test/contracts/**/*.consumer.pact.spec.ts',
  ],

  // Transform TypeScript files
  transform: {
    '^.+\\.(ts|js)$': [
      'ts-jest',
      {
        tsconfig: '<rootDir>/tsconfig.spec.json',
        // Enable ESM interop for Pact modules
        useESM: false,
      },
    ],
  },

  // Module paths for imports
  moduleNameMapper: {
    // Resolve pact-config from the pact directory
    '^@pact/(.*)$': '<rootDir>/pact/$1',
    '^@test/(.*)$': '<rootDir>/src/test/$1',
  },

  // Files to ignore during transform
  transformIgnorePatterns: [
    'node_modules/(?!(@pact-foundation|@angular|rxjs)/)',
  ],

  // Setup files to run before tests
  setupFilesAfterEnv: [],

  // Timeout for each test (Pact tests may take longer due to mock server setup)
  testTimeout: 60000,

  // Coverage configuration for Pact tests
  coverageDirectory: '../../coverage/apps/clinical-portal/pact',
  collectCoverageFrom: [
    'src/test/contracts/**/*.ts',
    'pact/**/*.ts',
  ],

  // Ensure tests run sequentially (important for Pact)
  maxWorkers: 1,

  // Global setup and teardown
  globalSetup: undefined,
  globalTeardown: undefined,

  // Verbose output for debugging contract test failures
  verbose: true,

  // Clear mocks between tests
  clearMocks: true,

  // Fail on first error during development
  bail: false,
};
