import { FullConfig } from '@playwright/test';
import { ApiHelpers } from './utils/api-helpers';
import * as fs from 'fs';

/**
 * HDIM Global Teardown
 *
 * Runs once after all tests to:
 * 1. Clean up test data (optional)
 * 2. Generate test reports
 * 3. Archive artifacts
 * 4. Log summary
 */

const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8087';
const CLEANUP_TEST_DATA = process.env.CLEANUP_TEST_DATA === 'true';

async function globalTeardown(config: FullConfig): Promise<void> {
  console.log('\n=== HDIM E2E Test Teardown ===\n');

  const startTime = Date.now();

  // Step 1: Clean up test data (if enabled)
  if (CLEANUP_TEST_DATA) {
    console.log('Cleaning up test data...');
    try {
      await cleanupTestData();
      console.log('Test data cleaned up');
    } catch (error) {
      console.warn('Test data cleanup failed:', error);
    }
  } else {
    console.log('Test data cleanup skipped (CLEANUP_TEST_DATA not set)');
  }

  // Step 2: Clean up authentication state
  const authFile = 'e2e/.auth/user.json';
  if (fs.existsSync(authFile)) {
    // Keep auth file for debugging, but mark as stale
    console.log('Authentication state preserved for debugging');
  }

  // Step 3: Log test summary
  logTestSummary(config);

  // Step 4: Log completion
  const duration = ((Date.now() - startTime) / 1000).toFixed(2);
  console.log(`\n=== Teardown completed in ${duration}s ===\n`);
}

/**
 * Clean up test data created during tests
 */
async function cleanupTestData(): Promise<void> {
  const apiHelpers = new ApiHelpers({
    baseUrl: API_BASE_URL,
    tenantId: 'TENANT001',
  });

  // Authenticate as admin for cleanup
  try {
    await apiHelpers.authenticate('test_admin', 'password123');
  } catch (error) {
    console.warn('Admin authentication failed, skipping cleanup');
    return;
  }

  // Delete test patients (those with test prefix)
  try {
    const testPatients = await apiHelpers.request(
      'GET',
      '/api/v1/patients?search=TEST_'
    );

    for (const patient of testPatients || []) {
      await apiHelpers.request('DELETE', `/api/v1/patients/${patient.id}`);
    }

    console.log(`Deleted ${testPatients?.length || 0} test patients`);
  } catch (error) {
    console.warn('Patient cleanup failed:', error);
  }

  // Delete test care gaps
  try {
    const testGaps = await apiHelpers.request(
      'GET',
      '/api/v1/care-gaps?status=test'
    );

    for (const gap of testGaps || []) {
      await apiHelpers.request('DELETE', `/api/v1/care-gaps/${gap.id}`);
    }

    console.log(`Deleted ${testGaps?.length || 0} test care gaps`);
  } catch (error) {
    console.warn('Care gap cleanup failed:', error);
  }

  // Delete test evaluations
  try {
    const testEvals = await apiHelpers.request(
      'GET',
      '/api/v1/evaluations?source=e2e_test'
    );

    for (const evaluation of testEvals || []) {
      await apiHelpers.request('DELETE', `/api/v1/evaluations/${evaluation.id}`);
    }

    console.log(`Deleted ${testEvals?.length || 0} test evaluations`);
  } catch (error) {
    console.warn('Evaluation cleanup failed:', error);
  }
}

/**
 * Log test run summary
 */
function logTestSummary(config: FullConfig): void {
  console.log('\n--- Test Run Summary ---');
  console.log(`Projects configured: ${config.projects.length}`);
  console.log(`Test directory: ${config.rootDir}`);
  console.log(`Workers: ${config.workers}`);
  console.log(`Retries: ${config.projects[0]?.retries || 0}`);

  // Log artifact locations
  console.log('\nArtifacts:');
  console.log('  HTML Report: playwright-report/index.html');
  console.log('  JUnit XML: test-results/junit.xml');
  console.log('  JSON Results: test-results/results.json');

  // Log any environment info
  console.log('\nEnvironment:');
  console.log(`  BASE_URL: ${process.env.BASE_URL || 'http://localhost:4200'}`);
  console.log(`  API_BASE_URL: ${process.env.API_BASE_URL || 'http://localhost:8087'}`);
  console.log(`  CI: ${process.env.CI || 'false'}`);
}

export default globalTeardown;
