import { chromium, FullConfig } from '@playwright/test';
import { ApiHelpers } from './utils/api-helpers';
import { TestDataFactory } from './utils/test-data-factory';
import { ServiceHealthChecker } from './utils/service-health-checker';
import * as fs from 'fs';
import * as path from 'path';

/**
 * HDIM Global Setup
 *
 * Runs once before all tests to:
 * 1. Verify backend services are healthy (with graceful degradation)
 * 2. Set up authentication state
 * 3. Seed test data
 * 4. Configure test environment
 */

// Use absolute path to match playwright.config.ts
const AUTH_FILE = path.resolve(__dirname, '.auth/user.json');
const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080';
const CLINICAL_PORTAL_URL = process.env.BASE_URL || 'http://localhost:4200';
const SKIP_GLOBAL_SETUP = process.env.SKIP_GLOBAL_SETUP === 'true';
const STRICT_VALIDATION = process.env.STRICT_VALIDATION === 'true';

async function globalSetup(config: FullConfig): Promise<void> {
  console.log('\n=== HDIM E2E Test Setup ===\n');

  const startTime = Date.now();

  // Skip setup if requested
  if (SKIP_GLOBAL_SETUP) {
    console.log('⚠️  Global setup skipped (SKIP_GLOBAL_SETUP=true)');
    console.log('Creating minimal auth state...');
    createMockAuthState();
    return;
  }

  // Step 1: Ensure auth directory exists
  const authDir = path.dirname(AUTH_FILE);
  if (!fs.existsSync(authDir)) {
    fs.mkdirSync(authDir, { recursive: true });
    console.log('Created auth directory:', authDir);
  }

  // Step 2: Check backend health (with graceful degradation)
  console.log('Checking backend services health...');
  const healthChecker = new ServiceHealthChecker({
    timeout: 5000,
    retries: 2,
    retryDelay: 1000,
  });

  const healthSummary = await healthChecker.checkHdimServices({
    gateway: API_BASE_URL,
    patient: 'http://localhost:8084',
    qualityMeasure: 'http://localhost:8087',
    fhir: 'http://localhost:8085',
    portal: CLINICAL_PORTAL_URL,
  });

  // Log health status
  console.log(`\nService Health Summary:`);
  console.log(`  Healthy: ${healthSummary.healthyCount}/${healthSummary.totalCount}`);
  healthSummary.services.forEach(service => {
    const status = service.healthy ? '✓' : '✗';
    const details = service.responseTime 
      ? ` (${service.responseTime}ms)` 
      : service.error 
        ? ` (${service.error})` 
        : '';
    console.log(`  ${status} ${service.service}${details}`);
  });

  // Only fail if strict validation is enabled and required services are down
  if (STRICT_VALIDATION && !healthSummary.allHealthy) {
    console.error('\n❌ Strict validation enabled and services are not healthy');
    console.error('Set STRICT_VALIDATION=false to continue with degraded mode');
    throw new Error('Service health check failed (strict mode)');
  }

  if (healthSummary.healthyCount === healthSummary.totalCount) {
    console.log('\n✓ All services are healthy');
  } else if (healthSummary.healthyCount > 0) {
    console.warn('\n⚠️  Some services are not healthy, continuing in degraded mode');
    console.warn('Tests that require unavailable services may be skipped or fail');
  } else {
    console.warn('\n⚠️  No services are healthy, continuing with mock auth only');
    console.warn('Most tests will likely fail or be skipped');
  }

  // Step 3: Set up authentication state
  console.log('\nSetting up authentication state...');
  const apiHelpers = new ApiHelpers({ baseUrl: API_BASE_URL, tenantId: 'TENANT001' });

  // Try browser-based auth first (if portal is available)
  if (healthSummary.services.find(s => s.service === 'Clinical Portal' && s.healthy)) {
    try {
      const browser = await chromium.launch({ headless: true });
      const context = await browser.newContext();
      const page = await context.newPage();

      try {
        // Navigate to login page with timeout
        await page.goto(CLINICAL_PORTAL_URL, { waitUntil: 'domcontentloaded', timeout: 10000 });

        // Try to find and fill login form (with multiple selector strategies)
        const usernameSelectors = [
          '[data-testid="username-input"]',
          'input[type="email"]',
          'input[name="username"]',
          'input[placeholder*="email" i]',
          'input[placeholder*="username" i]',
        ];

        const passwordSelectors = [
          '[data-testid="password-input"]',
          'input[type="password"]',
          'input[name="password"]',
        ];

        const loginButtonSelectors = [
          '[data-testid="login-button"]',
          'button[type="submit"]',
          'button:has-text("Login")',
          'button:has-text("Sign in")',
        ];

        let filled = false;
        for (const usernameSelector of usernameSelectors) {
          try {
            const usernameInput = page.locator(usernameSelector).first();
            if (await usernameInput.isVisible({ timeout: 2000 })) {
              await usernameInput.fill('test_evaluator');
              filled = true;
              break;
            }
          } catch {
            continue;
          }
        }

        if (filled) {
          for (const passwordSelector of passwordSelectors) {
            try {
              const passwordInput = page.locator(passwordSelector).first();
              if (await passwordInput.isVisible({ timeout: 2000 })) {
                await passwordInput.fill('password123');
                break;
              }
            } catch {
              continue;
            }
          }

          for (const buttonSelector of loginButtonSelectors) {
            try {
              const loginButton = page.locator(buttonSelector).first();
              if (await loginButton.isVisible({ timeout: 2000 })) {
                await loginButton.click();
                await page.waitForURL('**/dashboard', { timeout: 15000 }).catch(() => {
                  // May redirect to different URL
                });
                break;
              }
            } catch {
              continue;
            }
          }

          // Save authentication state
          await context.storageState({ path: AUTH_FILE });
          console.log('✓ Browser-based authentication state saved');
          await browser.close();
          // Skip to data seeding
          gotoDataSeeding();
          return;
        }
      } catch (error: any) {
        console.warn('Browser-based auth failed:', error.message);
      } finally {
        await browser.close();
      }
    } catch (error: any) {
      console.warn('Browser launch failed:', error.message);
    }
  }

  // Fallback: Try API-based authentication
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
    console.log('✓ API-based authentication state saved');
  } catch (authError: any) {
    console.warn('API authentication failed:', authError.message);
    console.log('Creating mock auth state for test execution...');
    createMockAuthState();
  }

  // Step 4: Seed test data (if needed)
  await gotoDataSeeding();
}

function createMockAuthState(): void {
  const CLINICAL_PORTAL_URL = process.env.BASE_URL || 'http://localhost:4200';
  const mockStorageState = {
    cookies: [],
    origins: [
      {
        origin: CLINICAL_PORTAL_URL,
        localStorage: [
          { name: 'auth_token', value: 'mock_test_token' },
          { name: 'refresh_token', value: 'mock_refresh_token' },
          { name: 'tenant_id', value: 'TENANT001' },
          { name: 'user_role', value: 'EVALUATOR' },
        ],
      },
    ],
  };

  fs.writeFileSync(AUTH_FILE, JSON.stringify(mockStorageState, null, 2));
  console.log('✓ Mock auth state created - tests requiring real auth may need individual login');
}

async function gotoDataSeeding(): Promise<void> {
  const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080';
  const apiHelpers = new ApiHelpers({ baseUrl: API_BASE_URL, tenantId: 'TENANT001' });
  const startTime = Date.now();

  // Step 4: Seed test data (if needed)
  console.log('\nChecking test data...');
  try {
    const testDataFactory = new TestDataFactory({ phiMasking: null });
    await seedTestDataIfNeeded(apiHelpers, testDataFactory);
    console.log('✓ Test data ready');
  } catch (error: any) {
    console.warn('⚠️  Test data seeding skipped:', error.message);
  }

  // Step 5: Log setup completion
  const duration = ((Date.now() - startTime) / 1000).toFixed(2);
  console.log(`\n=== Setup completed in ${duration}s ===\n`);
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
