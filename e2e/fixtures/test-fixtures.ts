import { test as base, expect, Page, BrowserContext } from '@playwright/test';
import { ApiHelpers } from '../utils/api-helpers';
import { WebSocketHelpers } from '../utils/websocket-helpers';
import { TestDataFactory } from '../utils/test-data-factory';
import { PHIMasking } from '../utils/phi-masking';

/**
 * HDIM Custom Playwright Fixtures
 *
 * Extends Playwright's base test with healthcare-specific fixtures
 * for authentication, API access, WebSocket connections, and PHI handling.
 */

// Test user credentials interface
export interface TestUser {
  username: string;
  password: string;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'EVALUATOR' | 'ANALYST' | 'VIEWER';
  tenantId: string;
}

// Test users for different roles
export const TEST_USERS: Record<string, TestUser> = {
  superAdmin: {
    username: 'test_superadmin',
    password: 'password123',
    role: 'SUPER_ADMIN',
    tenantId: 'TENANT001',
  },
  admin: {
    username: 'test_admin',
    password: 'password123',
    role: 'ADMIN',
    tenantId: 'TENANT001',
  },
  evaluator: {
    username: 'test_evaluator',
    password: 'password123',
    role: 'EVALUATOR',
    tenantId: 'TENANT001',
  },
  analyst: {
    username: 'test_analyst',
    password: 'password123',
    role: 'ANALYST',
    tenantId: 'TENANT001',
  },
  viewer: {
    username: 'test_viewer',
    password: 'password123',
    role: 'VIEWER',
    tenantId: 'TENANT001',
  },
};

// Authentication state storage
export interface AuthState {
  token: string;
  refreshToken: string;
  user: TestUser;
  expiresAt: number;
}

// Custom fixture types
export type HDIMFixtures = {
  // Authenticated page with different roles
  authenticatedPage: Page;
  adminPage: Page;
  evaluatorPage: Page;
  viewerPage: Page;

  // API helpers for direct backend interaction
  apiHelpers: ApiHelpers;

  // WebSocket helpers for event-driven testing
  wsHelpers: WebSocketHelpers;

  // Test data factory for synthetic data
  testDataFactory: TestDataFactory;

  // PHI masking for HIPAA compliance
  phiMasking: PHIMasking;

  // Current authenticated user
  currentUser: TestUser;

  // Tenant context
  tenantId: string;
};

// Custom test options
export type HDIMOptions = {
  defaultRole: TestUser['role'];
  enablePHIMasking: boolean;
  wsEndpoint: string;
  apiBaseUrl: string;
};

/**
 * Extended test with HDIM-specific fixtures
 */
export const test = base.extend<HDIMFixtures & HDIMOptions>({
  // Default options
  defaultRole: ['EVALUATOR', { option: true }],
  enablePHIMasking: [true, { option: true }],
  wsEndpoint: ['ws://localhost:8087/ws', { option: true }],
  apiBaseUrl: ['http://localhost:8087', { option: true }],

  // Tenant context
  tenantId: async ({}, use) => {
    await use('TENANT001');
  },

  // Current user based on default role
  currentUser: async ({ defaultRole }, use) => {
    const userMap: Record<string, TestUser> = {
      SUPER_ADMIN: TEST_USERS.superAdmin,
      ADMIN: TEST_USERS.admin,
      EVALUATOR: TEST_USERS.evaluator,
      ANALYST: TEST_USERS.analyst,
      VIEWER: TEST_USERS.viewer,
    };
    await use(userMap[defaultRole] || TEST_USERS.evaluator);
  },

  // PHI masking utility
  phiMasking: async ({ enablePHIMasking }, use) => {
    const masking = new PHIMasking({ enabled: enablePHIMasking });
    await use(masking);
  },

  // Test data factory
  testDataFactory: async ({ phiMasking }, use) => {
    const factory = new TestDataFactory({ phiMasking });
    await use(factory);
  },

  // API helpers
  apiHelpers: async ({ apiBaseUrl, tenantId }, use) => {
    const helpers = new ApiHelpers({
      baseUrl: apiBaseUrl,
      tenantId,
    });
    await use(helpers);
  },

  // WebSocket helpers
  wsHelpers: async ({ wsEndpoint, tenantId }, use) => {
    const helpers = new WebSocketHelpers({
      endpoint: wsEndpoint,
      tenantId,
    });
    await helpers.connect();
    await use(helpers);
    await helpers.disconnect();
  },

  // Authenticated page with default role
  authenticatedPage: async ({ page, currentUser, apiHelpers }, use) => {
    await authenticateUser(page, currentUser, apiHelpers);
    await use(page);
  },

  // Admin authenticated page
  adminPage: async ({ browser, apiHelpers }, use) => {
    const context = await browser.newContext();
    const page = await context.newPage();
    await authenticateUser(page, TEST_USERS.admin, apiHelpers);
    await use(page);
    await context.close();
  },

  // Evaluator authenticated page
  evaluatorPage: async ({ browser, apiHelpers }, use) => {
    const context = await browser.newContext();
    const page = await context.newPage();
    await authenticateUser(page, TEST_USERS.evaluator, apiHelpers);
    await use(page);
    await context.close();
  },

  // Viewer authenticated page
  viewerPage: async ({ browser, apiHelpers }, use) => {
    const context = await browser.newContext();
    const page = await context.newPage();
    await authenticateUser(page, TEST_USERS.viewer, apiHelpers);
    await use(page);
    await context.close();
  },
});

/**
 * Authenticate a user and set up the session
 */
async function authenticateUser(
  page: Page,
  user: TestUser,
  apiHelpers: ApiHelpers
): Promise<void> {
  // Get authentication token
  const authResponse = await apiHelpers.authenticate(user.username, user.password);

  // Set auth cookie/localStorage
  await page.goto('/');
  await page.evaluate((token) => {
    localStorage.setItem('auth_token', token);
  }, authResponse.token);

  // Set tenant header for subsequent requests
  await page.setExtraHTTPHeaders({
    'Authorization': `Bearer ${authResponse.token}`,
    'X-Tenant-ID': user.tenantId,
  });
}

/**
 * Custom expect matchers for healthcare-specific assertions
 */
export { expect };

// Re-export for convenience
export { Page, BrowserContext };
