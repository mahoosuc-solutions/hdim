/**
 * API Mocking Utilities for Playwright E2E Tests
 *
 * Provides consistent mock responses for backend APIs to ensure
 * reliable tests regardless of backend state.
 */

import { Page, Route } from '@playwright/test';

/**
 * Mock data for patients
 */
export const mockPatients = [
  {
    id: 'patient-001',
    fhirId: 'fhir-001',
    mrn: 'MRN-001',
    fullName: 'Smith, John',
    firstName: 'John',
    lastName: 'Smith',
    dateOfBirth: '1965-03-15',
    age: 59,
    gender: 'male',
    status: 'Active',
    isMaster: true,
  },
  {
    id: 'patient-002',
    fhirId: 'fhir-002',
    mrn: 'MRN-002',
    fullName: 'Johnson, Sarah',
    firstName: 'Sarah',
    lastName: 'Johnson',
    dateOfBirth: '1978-07-22',
    age: 46,
    gender: 'female',
    status: 'Active',
    isMaster: true,
  },
  {
    id: 'patient-003',
    fhirId: 'fhir-003',
    mrn: 'MRN-003',
    fullName: 'Williams, Michael',
    firstName: 'Michael',
    lastName: 'Williams',
    dateOfBirth: '1950-11-08',
    age: 74,
    gender: 'male',
    status: 'Active',
    isMaster: true,
  },
];

/**
 * Mock data for care gaps
 */
export const mockCareGaps = [
  {
    id: 'gap-001',
    patientId: 'patient-001',
    patientName: 'Smith, John',
    mrn: 'MRN-001',
    gapType: 'screening',
    gapDescription: 'Diabetes HbA1c test overdue',
    measureName: 'Diabetes Care - HbA1c Testing',
    urgency: 'high',
    daysOverdue: 45,
    status: 'open',
  },
  {
    id: 'gap-002',
    patientId: 'patient-002',
    patientName: 'Johnson, Sarah',
    mrn: 'MRN-002',
    gapType: 'medication',
    gapDescription: 'Statin therapy not prescribed',
    measureName: 'Statin Therapy - CVD',
    urgency: 'medium',
    daysOverdue: 30,
    status: 'open',
  },
  {
    id: 'gap-003',
    patientId: 'patient-003',
    patientName: 'Williams, Michael',
    mrn: 'MRN-003',
    gapType: 'followup',
    gapDescription: 'Annual wellness visit needed',
    measureName: 'Annual Wellness Visit',
    urgency: 'low',
    daysOverdue: 15,
    status: 'open',
  },
];

/**
 * Mock data for quality measures
 */
export const mockQualityMeasures = [
  {
    id: 'measure-001',
    measureId: 'CDC',
    measureName: 'Diabetes Care - HbA1c Control',
    complianceRate: 78.5,
    numerator: 157,
    denominator: 200,
    target: 80.0,
  },
  {
    id: 'measure-002',
    measureId: 'CBP',
    measureName: 'Controlling High Blood Pressure',
    complianceRate: 72.3,
    numerator: 144,
    denominator: 199,
    target: 75.0,
  },
  {
    id: 'measure-003',
    measureId: 'SPC',
    measureName: 'Statin Therapy - CVD',
    complianceRate: 68.9,
    numerator: 137,
    denominator: 199,
    target: 70.0,
  },
];

/**
 * Mock care gap summary
 */
export const mockCareGapSummary = {
  totalGaps: 156,
  highUrgencyCount: 42,
  mediumUrgencyCount: 68,
  lowUrgencyCount: 46,
  byType: {
    screening: 45,
    medication: 38,
    followup: 32,
    lab: 25,
    assessment: 16,
  },
};

/**
 * Mock patient statistics
 */
export const mockPatientStats = {
  totalPatients: 1250,
  activePatients: 1180,
  averageAge: 52.3,
  genderDistribution: {
    male: 580,
    female: 600,
    other: 70,
  },
};

/**
 * Setup API mocks for a page
 * @param page Playwright page
 * @param options Mock configuration options
 */
export async function setupApiMocks(
  page: Page,
  options: {
    mockPatients?: boolean;
    mockCareGaps?: boolean;
    mockQualityMeasures?: boolean;
    mockAuth?: boolean;
  } = {}
): Promise<void> {
  const {
    mockPatients: shouldMockPatients = true,
    mockCareGaps: shouldMockCareGaps = true,
    mockQualityMeasures: shouldMockQualityMeasures = true,
    mockAuth = true,
  } = options;

  // Mock authentication endpoints
  if (mockAuth) {
    await page.route('**/auth/**', async (route: Route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          token: 'mock-jwt-token',
          user: {
            id: 'user-001',
            username: 'test_admin',
            roles: ['ADMIN'],
            tenantId: 'TENANT001',
          },
        }),
      });
    });
  }

  // Mock patient endpoints
  if (shouldMockPatients) {
    // Patient list
    await page.route('**/patient/**', async (route: Route) => {
      const url = route.request().url();

      if (url.includes('/Patient') || url.includes('/demographics')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockPatients),
        });
      } else {
        await route.continue();
      }
    });

    // FHIR Patient endpoint
    await page.route('**/fhir/Patient**', async (route: Route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          resourceType: 'Bundle',
          type: 'searchset',
          total: mockPatients.length,
          entry: mockPatients.map(p => ({
            resource: {
              resourceType: 'Patient',
              id: p.fhirId,
              identifier: [{ value: p.mrn }],
              name: [{ family: p.lastName, given: [p.firstName] }],
              gender: p.gender,
              birthDate: p.dateOfBirth,
            },
          })),
        }),
      });
    });
  }

  // Mock care gap endpoints
  if (shouldMockCareGaps) {
    await page.route('**/care-gap/**', async (route: Route) => {
      const url = route.request().url();

      if (url.includes('/summary')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCareGapSummary),
        });
      } else if (url.includes('/high-priority')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCareGaps.filter(g => g.urgency === 'high')),
        });
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCareGaps),
        });
      }
    });

    // Quality measure care gap endpoints
    await page.route('**/quality-measure/**/care-gaps**', async (route: Route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockCareGaps),
      });
    });
  }

  // Mock quality measure endpoints
  if (shouldMockQualityMeasures) {
    await page.route('**/quality-measure/**', async (route: Route) => {
      const url = route.request().url();

      if (url.includes('/population') || url.includes('/report')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            measureResults: mockQualityMeasures,
            totalPatients: 200,
            evaluatedPatients: 199,
          }),
        });
      } else if (url.includes('/results')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockQualityMeasures),
        });
      } else if (url.includes('/stats')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            total: 156,
            last30Days: 42,
            successRate: 94.5,
          }),
        });
      } else {
        await route.continue();
      }
    });

    // CQL Engine endpoints
    await page.route('**/cql-engine/**', async (route: Route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          libraries: [],
          measures: mockQualityMeasures.map(m => ({
            id: m.measureId,
            name: m.measureName,
          })),
        }),
      });
    });
  }

  // Mock health check endpoints
  await page.route('**/_health', async (route: Route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ status: 'UP' }),
    });
  });

  await page.route('**/health', async (route: Route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ status: 'UP' }),
    });
  });
}

/**
 * Clear all API mocks from a page
 */
export async function clearApiMocks(page: Page): Promise<void> {
  await page.unrouteAll();
}

/**
 * Mock a specific API endpoint with custom data
 */
export async function mockEndpoint(
  page: Page,
  urlPattern: string,
  response: unknown,
  statusCode = 200
): Promise<void> {
  await page.route(urlPattern, async (route: Route) => {
    await route.fulfill({
      status: statusCode,
      contentType: 'application/json',
      body: JSON.stringify(response),
    });
  });
}

/**
 * Mock API error for testing error handling
 */
export async function mockApiError(
  page: Page,
  urlPattern: string,
  statusCode = 500,
  errorMessage = 'Internal Server Error'
): Promise<void> {
  await page.route(urlPattern, async (route: Route) => {
    await route.fulfill({
      status: statusCode,
      contentType: 'application/json',
      body: JSON.stringify({
        error: errorMessage,
        status: statusCode,
      }),
    });
  });
}
