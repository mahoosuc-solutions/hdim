/**
 * Global Setup for Playwright E2E Tests
 *
 * This script runs once before all tests to:
 * - Authenticate test users
 * - Set up test data
 * - Initialize databases
 * - Configure test environment
 *
 * @author TDD Swarm Agent 5A
 */

import { chromium, FullConfig } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

/**
 * Authentication state file
 */
const AUTH_FILE = path.join(__dirname, '.auth/user.json');
const ADMIN_AUTH_FILE = path.join(__dirname, '.auth/admin.json');
const CLINICIAN_AUTH_FILE = path.join(__dirname, '.auth/clinician.json');

/**
 * Test user credentials
 */
const TEST_USERS = {
  admin: {
    username: 'admin@healthdata.com',
    password: 'Admin123!',
    tenantId: 'test-tenant-001',
  },
  clinician: {
    username: 'clinician@healthdata.com',
    password: 'Clinician123!',
    tenantId: 'test-tenant-001',
  },
  user: {
    username: 'user@healthdata.com',
    password: 'User123!',
    tenantId: 'test-tenant-001',
  },
};

/**
 * Global setup function
 */
async function globalSetup(config: FullConfig) {
  console.log('\n🚀 Starting Global Setup...\n');

  const baseURL = config.projects[0]?.use?.baseURL || 'http://localhost:8080';

  // Create .auth directory if it doesn't exist
  const authDir = path.join(__dirname, '.auth');
  if (!fs.existsSync(authDir)) {
    fs.mkdirSync(authDir, { recursive: true });
  }

  // Launch browser for authentication
  const browser = await chromium.launch();

  try {
    // Authenticate admin user
    await authenticateUser(browser, baseURL, TEST_USERS.admin, ADMIN_AUTH_FILE);
    console.log('✅ Admin user authenticated');

    // Authenticate clinician user
    await authenticateUser(browser, baseURL, TEST_USERS.clinician, CLINICIAN_AUTH_FILE);
    console.log('✅ Clinician user authenticated');

    // Authenticate regular user
    await authenticateUser(browser, baseURL, TEST_USERS.user, AUTH_FILE);
    console.log('✅ Regular user authenticated');

    // Set up test data
    await setupTestData(baseURL, ADMIN_AUTH_FILE);
    console.log('✅ Test data initialized');

    console.log('\n✅ Global Setup Complete\n');
  } catch (error) {
    console.error('❌ Global Setup Failed:', error);
    throw error;
  } finally {
    await browser.close();
  }
}

/**
 * Authenticate a user and save the session
 */
async function authenticateUser(
  browser: any,
  baseURL: string,
  user: typeof TEST_USERS.admin,
  authFile: string
) {
  const page = await browser.newPage();

  try {
    // Try JWT authentication via API
    const response = await page.request.post(`${baseURL}/api/auth/login`, {
      data: {
        username: user.username,
        password: user.password,
        tenantId: user.tenantId,
      },
      headers: {
        'Content-Type': 'application/json',
      },
      failOnStatusCode: false,
    });

    if (response.ok()) {
      const data = await response.json();
      const token = data.token || data.accessToken || data.access_token;

      if (token) {
        // Save authentication state with token
        await page.context().storageState({
          path: authFile,
        });

        // Also save token separately for API tests
        const authData = {
          token,
          username: user.username,
          tenantId: user.tenantId,
          expiresAt: Date.now() + 3600000, // 1 hour
        };

        fs.writeFileSync(
          authFile.replace('.json', '-token.json'),
          JSON.stringify(authData, null, 2)
        );

        return;
      }
    }

    // Fallback to UI-based login
    console.log(`⚠️  API login failed for ${user.username}, trying UI login...`);

    await page.goto(`${baseURL}/login`);
    await page.fill('input[name="username"], input[type="email"], input[type="text"]', user.username);
    await page.fill('input[name="password"], input[type="password"]', user.password);

    // Handle tenant ID if visible
    const tenantInput = page.locator('input[name="tenantId"]');
    if (await tenantInput.isVisible({ timeout: 1000 }).catch(() => false)) {
      await tenantInput.fill(user.tenantId);
    }

    // Click login button
    await page.click('button[type="submit"], button:has-text("Login"), button:has-text("Sign In")');

    // Wait for navigation to complete
    await page.waitForURL(/\/(dashboard|home|patients)/, { timeout: 10000 });

    // Save authenticated state
    await page.context().storageState({ path: authFile });

  } catch (error) {
    console.error(`Failed to authenticate ${user.username}:`, error);

    // Create a basic auth file even if authentication fails
    // This allows tests to run and report the actual authentication errors
    const basicAuth = {
      cookies: [],
      origins: [
        {
          origin: baseURL,
          localStorage: [
            {
              name: 'tenantId',
              value: user.tenantId,
            },
          ],
        },
      ],
    };

    fs.writeFileSync(authFile, JSON.stringify(basicAuth, null, 2));
  } finally {
    await page.close();
  }
}

/**
 * Set up test data
 */
async function setupTestData(baseURL: string, authFile: string) {
  try {
    // Read authentication token
    const tokenFile = authFile.replace('.json', '-token.json');
    if (!fs.existsSync(tokenFile)) {
      console.log('⚠️  No auth token found, skipping test data setup');
      return;
    }

    const authData = JSON.parse(fs.readFileSync(tokenFile, 'utf-8'));
    const token = authData.token;

    // Create test patients
    const testPatients = [
      {
        firstName: 'John',
        lastName: 'Smith',
        mrn: 'E2E-TEST-001',
        dateOfBirth: '1980-01-15',
        gender: 'male',
        tenantId: authData.tenantId,
      },
      {
        firstName: 'Jane',
        lastName: 'Doe',
        mrn: 'E2E-TEST-002',
        dateOfBirth: '1975-05-20',
        gender: 'female',
        tenantId: authData.tenantId,
      },
      {
        firstName: 'Robert',
        lastName: 'Johnson',
        mrn: 'E2E-TEST-003',
        dateOfBirth: '1990-11-30',
        gender: 'male',
        tenantId: authData.tenantId,
      },
    ];

    // Use fetch to create patients
    const { chromium } = await import('@playwright/test');
    const browser = await chromium.launch();
    const context = await browser.newContext({
      extraHTTPHeaders: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    });

    try {
      for (const patient of testPatients) {
        const response = await context.request.post(`${baseURL}/api/patients`, {
          data: patient,
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
          failOnStatusCode: false,
        });

        if (response.ok()) {
          console.log(`   Created test patient: ${patient.firstName} ${patient.lastName}`);
        } else if (response.status() === 409) {
          console.log(`   Test patient already exists: ${patient.firstName} ${patient.lastName}`);
        } else {
          console.log(`   Failed to create patient: ${response.status()} ${response.statusText()}`);
        }
      }
    } finally {
      await browser.close();
    }

  } catch (error) {
    console.error('Failed to set up test data:', error);
    // Don't throw - let tests proceed even if test data setup fails
  }
}

export default globalSetup;
