/**
 * Custom Test Fixtures for HealthData Platform E2E Tests
 *
 * Provides reusable test fixtures for:
 * - Authenticated users (admin, clinician, regular user)
 * - API clients with authentication
 * - Test data factories
 * - Page objects
 *
 * @author TDD Swarm Agent 5A
 */

import { test as base, expect, Page, APIRequestContext } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

/**
 * Custom fixture types
 */
type CustomFixtures = {
  authenticatedPage: Page;
  adminPage: Page;
  clinicianPage: Page;
  apiClient: APIRequestContext;
  adminApiClient: APIRequestContext;
  testData: TestDataFactory;
};

/**
 * Authentication helper
 */
class AuthHelper {
  static getAuthToken(authFile: string): string | null {
    const tokenFile = authFile.replace('.json', '-token.json');
    if (!fs.existsSync(tokenFile)) {
      return null;
    }
    const authData = JSON.parse(fs.readFileSync(tokenFile, 'utf-8'));
    return authData.token;
  }
}

/**
 * Test data factory
 */
class TestDataFactory {
  constructor(private apiClient: APIRequestContext) {}

  /**
   * Create a test patient
   */
  async createPatient(overrides: Partial<any> = {}) {
    const patient = {
      firstName: 'Test',
      lastName: `Patient${Date.now()}`,
      mrn: `MRN${Date.now()}`,
      dateOfBirth: '1980-01-01',
      gender: 'male',
      tenantId: 'test-tenant-001',
      ...overrides,
    };

    const response = await this.apiClient.post('/api/patients', {
      data: patient,
    });

    if (response.ok()) {
      return await response.json();
    }

    throw new Error(`Failed to create patient: ${response.status()} ${response.statusText()}`);
  }

  /**
   * Create test observation
   */
  async createObservation(patientId: string, overrides: Partial<any> = {}) {
    const observation = {
      resourceType: 'Observation',
      status: 'final',
      code: {
        coding: [
          {
            system: 'http://loinc.org',
            code: '4548-4',
            display: 'Hemoglobin A1c',
          },
        ],
      },
      subject: {
        reference: `Patient/${patientId}`,
      },
      valueQuantity: {
        value: 7.5,
        unit: '%',
        system: 'http://unitsofmeasure.org',
        code: '%',
      },
      effectiveDateTime: new Date().toISOString(),
      ...overrides,
    };

    const response = await this.apiClient.post('/api/fhir/Observation', {
      data: observation,
    });

    if (response.ok()) {
      return await response.json();
    }

    throw new Error(`Failed to create observation: ${response.status()}`);
  }

  /**
   * Create test care gap
   */
  async createCareGap(patientId: string, overrides: Partial<any> = {}) {
    const careGap = {
      patientId,
      measureId: 'hba1c-control',
      measureName: 'HbA1c Control',
      gapType: 'MISSING_DATA',
      priority: 'HIGH',
      status: 'OPEN',
      tenantId: 'test-tenant-001',
      ...overrides,
    };

    const response = await this.apiClient.post('/api/care-gaps', {
      data: careGap,
    });

    if (response.ok()) {
      return await response.json();
    }

    throw new Error(`Failed to create care gap: ${response.status()}`);
  }

  /**
   * Clean up test data
   */
  async cleanup() {
    // Delete test patients created during this session
    // This can be implemented based on your cleanup strategy
  }
}

/**
 * Extended test with custom fixtures
 */
export const test = base.extend<CustomFixtures>({
  /**
   * Regular authenticated user page
   */
  authenticatedPage: async ({ browser }, use) => {
    const context = await browser.newContext({
      storageState: path.join(__dirname, '../.auth/user.json'),
    });
    const page = await context.newPage();
    await use(page);
    await context.close();
  },

  /**
   * Admin user page
   */
  adminPage: async ({ browser }, use) => {
    const context = await browser.newContext({
      storageState: path.join(__dirname, '../.auth/admin.json'),
    });
    const page = await context.newPage();
    await use(page);
    await context.close();
  },

  /**
   * Clinician user page
   */
  clinicianPage: async ({ browser }, use) => {
    const context = await browser.newContext({
      storageState: path.join(__dirname, '../.auth/clinician.json'),
    });
    const page = await context.newPage();
    await use(page);
    await context.close();
  },

  /**
   * API client with authentication
   */
  apiClient: async ({ playwright }, use) => {
    const token = AuthHelper.getAuthToken(path.join(__dirname, '../.auth/user.json'));
    const context = await playwright.request.newContext({
      baseURL: process.env.BASE_URL || 'http://localhost:8080',
      extraHTTPHeaders: token
        ? {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
          }
        : {
            'Content-Type': 'application/json',
          },
    });
    await use(context);
    await context.dispose();
  },

  /**
   * Admin API client
   */
  adminApiClient: async ({ playwright }, use) => {
    const token = AuthHelper.getAuthToken(path.join(__dirname, '../.auth/admin.json'));
    const context = await playwright.request.newContext({
      baseURL: process.env.BASE_URL || 'http://localhost:8080',
      extraHTTPHeaders: token
        ? {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
          }
        : {
            'Content-Type': 'application/json',
          },
    });
    await use(context);
    await context.dispose();
  },

  /**
   * Test data factory
   */
  testData: async ({ apiClient }, use) => {
    const factory = new TestDataFactory(apiClient);
    await use(factory);
    await factory.cleanup();
  },
});

export { expect };
