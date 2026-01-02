/**
 * Global Teardown for Playwright E2E Tests
 *
 * This script runs once after all tests to:
 * - Clean up test data
 * - Close connections
 * - Generate final reports
 *
 * @author TDD Swarm Agent 5A
 */

import { FullConfig } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

/**
 * Global teardown function
 */
async function globalTeardown(config: FullConfig) {
  console.log('\n🧹 Starting Global Teardown...\n');

  try {
    // Clean up authentication files (optional - commented out to preserve for debugging)
    // const authDir = path.join(__dirname, '.auth');
    // if (fs.existsSync(authDir)) {
    //   fs.rmSync(authDir, { recursive: true, force: true });
    //   console.log('✅ Authentication files cleaned up');
    // }

    // Generate test summary
    generateTestSummary();

    console.log('\n✅ Global Teardown Complete\n');
  } catch (error) {
    console.error('❌ Global Teardown Failed:', error);
    // Don't throw - we don't want teardown failures to fail the test run
  }
}

/**
 * Generate test summary report
 */
function generateTestSummary() {
  const resultsFile = path.join(__dirname, 'test-results', 'test-results.json');

  if (!fs.existsSync(resultsFile)) {
    console.log('⚠️  No test results found');
    return;
  }

  try {
    const results = JSON.parse(fs.readFileSync(resultsFile, 'utf-8'));

    const summary = {
      total: results.stats?.expected || 0,
      passed: results.stats?.expectedPassed || 0,
      failed: results.stats?.unexpected || 0,
      skipped: results.stats?.skipped || 0,
      flaky: results.stats?.flaky || 0,
      duration: results.stats?.duration || 0,
    };

    console.log('📊 Test Summary:');
    console.log(`   Total Tests: ${summary.total}`);
    console.log(`   ✅ Passed: ${summary.passed}`);
    console.log(`   ❌ Failed: ${summary.failed}`);
    console.log(`   ⏭️  Skipped: ${summary.skipped}`);
    console.log(`   🔄 Flaky: ${summary.flaky}`);
    console.log(`   ⏱️  Duration: ${(summary.duration / 1000).toFixed(2)}s`);

    // Write summary to file
    const summaryFile = path.join(__dirname, 'test-results', 'summary.json');
    fs.writeFileSync(summaryFile, JSON.stringify(summary, null, 2));

  } catch (error) {
    console.error('Failed to generate test summary:', error);
  }
}

export default globalTeardown;
