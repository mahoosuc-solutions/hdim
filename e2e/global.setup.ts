import { chromium, FullConfig } from '@playwright/test';
import { ApiHelpers } from './utils/api-helpers';
import { TestDataFactory } from './utils/test-data-factory';
import * as fs from 'fs';
import * as path from 'path';

/**
 * HDIM Global Setup
 *
 * Runs once before all tests to:
 * 1. Verify backend services are healthy
 * 2. Set up authentication state
 * 3. Seed test data
 * 4. Configure test environment
 */

const AUTH_FILE = 'e2e/.auth/user.json';
const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8087';
const CLINICAL_PORTAL_URL = process.env.BASE_URL || 'http://localhost:4200';

async function globalSetup(config: FullConfig): Promise<void> {
  console.log('\n=== HDIM E2E Test Setup ===\n');

  const startTime = Date.now();

  // Step 1: Ensure auth directory exists
  const authDir = path.dirname(AUTH_FILE);
  if (!fs.existsSync(authDir)) {
    fs.mkdirSync(authDir, { recursive: true });
    console.log('Created auth directory:', authDir);
  }

  // Step 2: Check backend health
  console.log('Checking backend services health...');
  const apiHelpers = new ApiHelpers({ baseUrl: API_BASE_URL, tenantId: 'TENANT001' });

  try {
    await waitForService(`${API_BASE_URL}/actuator/health`, 60000);
    console.log('Backend services are healthy');
  } catch (error) {
    console.error('Backend services not available. Ensure docker compose is running.');
    throw error;
  }

  // Step 3: Set up authentication state
  console.log('Setting up authentication state...');
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    // Navigate to login page
    await page.goto(CLINICAL_PORTAL_URL);

    // Perform login as default test user (evaluator)
    await page.fill('[data-testid="username-input"]', 'test_evaluator');
    await page.fill('[data-testid="password-input"]', 'password123');
    await page.click('[data-testid="login-button"]');

    // Wait for successful authentication
    await page.waitForURL('**/dashboard', { timeout: 30000 });

    // Save authentication state
    await context.storageState({ path: AUTH_FILE });
    console.log('Authentication state saved to:', AUTH_FILE);

  } catch (error) {
    console.warn('Browser-based auth failed, using API authentication fallback...');

    // Fallback: Use API-based authentication
    try {
      const authResponse = await apiHelpers.authenticate('test_evaluator', 'password123');

      // Create storage state manually
      const storageState = {
        cookies: [],
        origins: [
          {
            origin: CLINICAL_PORTAL_URL,
            localStorage: [
              { name: 'auth_token', value: authResponse.token },
              { name: 'refresh_token', value: authResponse.refreshToken || '' },
              { name: 'tenant_id', value: 'TENANT001' },
            ],
          },
        ],
      };

      fs.writeFileSync(AUTH_FILE, JSON.stringify(storageState, null, 2));
      console.log('API-based authentication state saved');

    } catch (authError) {
      console.error('Authentication failed:', authError);
      throw authError;
    }
  } finally {
    await browser.close();
  }

  // Step 4: Seed test data (if needed)
  console.log('Checking test data...');
  try {
    const testDataFactory = new TestDataFactory({ phiMasking: null });
    await seedTestDataIfNeeded(apiHelpers, testDataFactory);
    console.log('Test data ready');
  } catch (error) {
    console.warn('Test data seeding skipped:', error);
  }

  // Step 5: Log setup completion
  const duration = ((Date.now() - startTime) / 1000).toFixed(2);
  console.log(`\n=== Setup completed in ${duration}s ===\n`);
}

/**
 * Wait for a service to become available
 */
async function waitForService(url: string, timeout: number): Promise<void> {
  const startTime = Date.now();

  while (Date.now() - startTime < timeout) {
    try {
      const response = await fetch(url);
      if (response.ok) {
        return;
      }
    } catch {
      // Service not ready yet
    }

    await new Promise(resolve => setTimeout(resolve, 1000));
  }

  throw new Error(`Service at ${url} not available after ${timeout}ms`);
}

/**
 * Seed test data if not already present
 */
async function seedTestDataIfNeeded(
  apiHelpers: ApiHelpers,
  testDataFactory: TestDataFactory
): Promise<void> {
  // Check if test patients exist
  const existingPatients = await apiHelpers.request('GET', '/api/v1/patients?limit=1');

  if (existingPatients && existingPatients.length > 0) {
    console.log('Test data already exists, skipping seeding');
    return;
  }

  console.log('Seeding test data...');

  // Create test patients
  const testPatients = testDataFactory.createPatients(10);
  for (const patient of testPatients) {
    try {
      await apiHelpers.request('POST', '/api/v1/patients', patient);
    } catch (error) {
      console.warn('Failed to seed patient:', error);
    }
  }

  // Create test quality measures
  const testMeasures = testDataFactory.createQualityMeasures(5);
  for (const measure of testMeasures) {
    try {
      await apiHelpers.request('POST', '/api/v1/quality-measures', measure);
    } catch (error) {
      console.warn('Failed to seed quality measure:', error);
    }
  }
}

export default globalSetup;
