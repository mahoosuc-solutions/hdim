import { test, expect, Page, Route } from '@playwright/test';
import {
  setupDemoAuthViaUI,
  navigateAuthenticated,
  waitForAppReady,
} from './fixtures/auth.fixture';

const LOCAL_MEASURES_RESPONSE = [
  {
    measureId: 'CDC',
    measureName: 'Comprehensive Diabetes Care',
    version: '1.0.0',
    category: 'CHRONIC_DISEASE',
  },
];

const QUALITY_RESULTS_RESPONSE = [
  {
    id: 'result-1',
    tenantId: 'acme-health',
    patientId: 'patient-001',
    measureId: 'CDC',
    measureName: 'Comprehensive Diabetes Care',
    measureCategory: 'HEDIS',
    measureYear: 2026,
    numeratorCompliant: false,
    denominatorEligible: true,
    complianceRate: 0,
    score: 0,
    calculationDate: '2026-02-24T00:00:00Z',
    createdAt: '2026-02-24T00:00:00Z',
    createdBy: 'system',
    version: 1,
  },
];

const FHIR_PATIENT_BUNDLE = {
  resourceType: 'Bundle',
  type: 'searchset',
  total: 2,
  entry: [
    {
      resource: {
        resourceType: 'Patient',
        id: 'patient-001',
        active: true,
        name: [{ given: ['Jane'], family: 'Doe' }],
        identifier: [{ value: 'MRN-001', type: { text: 'Medical Record Number' } }],
      },
    },
    {
      resource: {
        resourceType: 'Patient',
        id: 'patient-002',
        active: true,
        name: [{ given: ['John'], family: 'Smith' }],
        identifier: [{ value: 'MRN-002', type: { text: 'Medical Record Number' } }],
      },
    },
  ],
};

async function fulfillJson(route: Route, body: unknown, status = 200): Promise<void> {
  await route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  });
}

async function mockQualityMeasureApis(page: Page): Promise<void> {
  await page.route('**/quality-measure/measures/local**', (route) => fulfillJson(route, LOCAL_MEASURES_RESPONSE));
  await page.route('**/quality-measure/results**', (route) => fulfillJson(route, QUALITY_RESULTS_RESPONSE));
  await page.route('**/quality-measure/evaluation-presets/default**', async (route) => {
    const request = route.request();
    if (request.method() === 'GET') {
      await fulfillJson(route, { patientId: 'patient-001', measureId: 'CDC', useCqlEngine: false });
      return;
    }
    await route.continue();
  });

  await page.route('**/quality-measure/calculate-local**', (route) =>
    fulfillJson(route, {
      measureId: 'CDC',
      measureName: 'Comprehensive Diabetes Care',
      patientId: 'patient-001',
      eligible: true,
      denominatorMembership: true,
      denominatorExclusion: false,
      subMeasures: {},
      careGaps: [],
      recommendations: [],
      calculatedAt: '2026-02-24T12:00:00Z',
    })
  );

  await page.route('**/fhir/Patient?_count=1**', (route) => fulfillJson(route, FHIR_PATIENT_BUNDLE));
  await page.route('**/fhir/Patient?_count=100**', (route) => fulfillJson(route, FHIR_PATIENT_BUNDLE));
}

test.describe('Quality Measures - Patient Evaluation Context', () => {
  test.beforeEach(async ({ page }) => {
    await mockQualityMeasureApis(page);
    await setupDemoAuthViaUI(page);
    await navigateAuthenticated(page, '/quality-measures');
    await waitForAppReady(page);
  });

  test('shows patient autocomplete and disables run evaluation until valid patient context', async ({ page }) => {
    await expect(page).toHaveURL(/quality-measures/);

    const patientInput = page.getByLabel('Evaluation Patient');
    await expect(patientInput).toBeVisible();
    await expect(patientInput).toHaveValue(/Jane Doe|patient-001/i);

    // Invalidate context by typing a free-form value.
    await patientInput.fill('Unknown Person');
    await page.waitForTimeout(200);

    const runEvaluationButton = page.getByRole('button', { name: /run evaluation/i });
    await expect(runEvaluationButton).toBeDisabled();
  });

  test('selects patient from autocomplete and runs local evaluation', async ({ page }) => {
    const patientInput = page.getByLabel('Evaluation Patient');
    await patientInput.fill('John');

    const option = page.getByRole('option', { name: /John Smith \(MRN: MRN-002\)/i });
    await expect(option).toBeVisible();
    await option.click();

    // Select the measure card and run evaluation.
    const measureCard = page.locator('.measure-card').first();
    await expect(measureCard).toBeVisible();
    await measureCard.click();

    const runEvaluationButton = page.getByRole('button', { name: /run evaluation/i });
    await expect(runEvaluationButton).toBeEnabled();
    await runEvaluationButton.click();

    await expect(page.locator('.evaluation-results')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(/evaluation complete/i)).toBeVisible({ timeout: 10000 });
  });
});
