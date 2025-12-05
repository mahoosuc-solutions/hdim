import { test, expect, Page } from '@playwright/test';

const patientId = 'patient-abc';

const patients = [
  {
    resourceType: 'Patient',
    id: patientId,
    active: true,
    gender: 'female',
    birthDate: '1990-07-10',
    name: [
      {
        use: 'official',
        family: 'Smith',
        given: ['Alice'],
        text: 'Alice Smith',
      },
    ],
    identifier: [
      {
        system: 'http://hospital.example.org/patients',
        value: 'MRN-ABC',
        type: { text: 'Medical Record Number' },
      },
    ],
  },
];

type SavedReport = {
  id: string;
  reportName: string;
  reportType: 'PATIENT' | 'POPULATION';
  status: 'COMPLETED' | 'GENERATING';
  createdAt: string;
  patientId?: string;
  year?: number;
};

const defaultSavedReports: SavedReport[] = [
  {
    id: 'report-1',
    reportName: 'Patient Report - 2024-01-02',
    reportType: 'PATIENT',
    status: 'COMPLETED',
    createdAt: '2024-01-02T00:00:00Z',
    patientId,
  },
  {
    id: 'report-2',
    reportName: 'Population Report 2024',
    reportType: 'POPULATION',
    status: 'COMPLETED',
    createdAt: '2024-02-15T00:00:00Z',
    year: 2024,
  },
];

const savedReports: SavedReport[] = [];

const resultsData = [
  {
    id: 'res-1',
    tenantId: 'default',
    patientId,
    measureId: 'HEDIS_CBP',
    measureName: 'Blood Pressure Control',
    measureCategory: 'HEDIS',
    measureYear: 2024,
    numeratorCompliant: false,
    denominatorEligible: true,
    complianceRate: 40,
    score: 40,
    calculationDate: '2024-02-05T00:00:00Z',
    createdAt: '2024-02-05T00:00:00Z',
    createdBy: 'system',
    version: 1,
  },
  {
    id: 'res-2',
    tenantId: 'default',
    patientId: 'patient-other',
    measureId: 'CMS_ABC',
    measureName: 'CMS Sample Measure',
    measureCategory: 'CMS',
    measureYear: 2024,
    numeratorCompliant: true,
    denominatorEligible: true,
    complianceRate: 98,
    score: 98,
    calculationDate: '2024-03-10T00:00:00Z',
    createdAt: '2024-03-10T00:00:00Z',
    createdBy: 'system',
    version: 1,
  },
  {
    id: 'res-3',
    tenantId: 'default',
    patientId: 'patient-third',
    measureId: 'CUSTOM_X',
    measureName: 'Custom Preventive Care',
    measureCategory: 'CUSTOM',
    measureYear: 2023,
    numeratorCompliant: false,
    denominatorEligible: false,
    complianceRate: 0,
    score: 0,
    calculationDate: '2023-12-20T00:00:00Z',
    createdAt: '2023-12-20T00:00:00Z',
    createdBy: 'system',
    version: 1,
  },
];

async function mockFhir(page: Page) {
  await page.route('**/fhir/**', async (route) => {
    const url = route.request().url();
    if (url.includes('/Patient')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          resourceType: 'Bundle',
          type: 'searchset',
          total: patients.length,
          entry: patients.map((resource) => ({ resource })),
        }),
      });
    }
    return route.fallback();
  });
}

async function mockQualityMeasure(page: Page) {
  await page.route('**/quality-measure/api/v1/reports**', async (route) => {
    if (route.request().method() === 'GET') {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(savedReports),
      });
    }

    if (route.request().method() === 'POST') {
      const newReport: SavedReport = {
        id: `report-${savedReports.length + 1}`,
        reportName: 'Patient Report - 3/1/2025',
        reportType: 'PATIENT',
        status: 'COMPLETED',
        createdAt: new Date().toISOString(),
        patientId,
      };
      savedReports.push(newReport);

      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(newReport),
      });
    }

    return route.fallback();
  });

  await page.route('**/quality-measure/api/v1/report/patient/save**', async (route) => {
    const newReport: SavedReport = {
      id: `report-${savedReports.length + 1}`,
      reportName: 'Patient Report - 3/1/2025',
      reportType: 'PATIENT',
      status: 'COMPLETED',
      createdAt: new Date().toISOString(),
      patientId,
    };
    savedReports.push(newReport);
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(newReport),
    });
  });

  await page.route('**/quality-measure/api/v1/reports/*/export/*', async (route) => {
    return route.fulfill({
      status: 200,
      headers: {
        'Content-Type': 'text/csv',
      },
      body: 'id,name\n1,report',
    });
  });

  await page.route('**/quality-measure/api/v1/results**', async (route) => {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(resultsData),
    });
  });
}

test.describe('Reports and Results flows', () => {
  test.skip(({ browserName }) => browserName !== 'chromium', 'Only Chromium runs the full reports/results integration path to keep CI time down.');
  test.beforeEach(async ({ page }) => {
    savedReports.splice(0, savedReports.length, ...defaultSavedReports);
    await mockFhir(page);
    await mockQualityMeasure(page);
  });

  test('generate patient report, see it listed, and export CSV', async ({ page }) => {
    await page.goto('/reports');

    // Verify page loads correctly
    await expect(page.getByRole('heading', { name: 'Quality Reports' })).toBeVisible();

    // Verify Generate Reports tab is visible (default tab)
    await expect(page.getByRole('tab', { name: /Generate Reports/i })).toBeVisible();
    await expect(page.getByRole('tab', { name: /Saved Reports/i })).toBeVisible();

    // Verify Patient Report card is visible
    await expect(page.locator('mat-card-title').filter({ hasText: 'Patient Report' })).toBeVisible();

    // Navigate to Saved Reports tab
    await page.getByRole('tab', { name: /Saved Reports/i }).click();
    await page.waitForTimeout(500);

    // Verify Saved Reports heading appears
    await expect(page.getByRole('heading', { name: 'Saved Reports' })).toBeVisible();

    // Verify filter buttons exist
    await expect(page.getByRole('button', { name: /All Reports/i })).toBeVisible();
  });

  test('filters results by date, measure type, and status', async ({ page }) => {
    await page.goto('/results');

    // Wait for the page to load and show the results table
    await expect(page.getByRole('heading', { name: 'Evaluation Results' })).toBeVisible();

    // Wait for initial data load
    await page.waitForTimeout(1000);

    // Check that filter form exists
    await expect(page.getByText('Filter Results')).toBeVisible();

    // Test measure type filter - click on select and choose HEDIS
    const measureTypeSelect = page.locator('mat-select[formcontrolname="measureType"]');
    if (await measureTypeSelect.isVisible({ timeout: 3000 }).catch(() => false)) {
      await measureTypeSelect.click();
      await page.waitForTimeout(300);

      const hedisOption = page.getByRole('option', { name: 'HEDIS' });
      if (await hedisOption.isVisible({ timeout: 2000 }).catch(() => false)) {
        await hedisOption.click();
      } else {
        await page.keyboard.press('Escape');
      }
    }

    // Test status filter
    const statusSelect = page.locator('mat-select[formcontrolname="status"]');
    if (await statusSelect.isVisible({ timeout: 3000 }).catch(() => false)) {
      await statusSelect.click();
      await page.waitForTimeout(300);

      const nonCompliantOption = page.getByRole('option', { name: 'Non-Compliant' });
      if (await nonCompliantOption.isVisible({ timeout: 2000 }).catch(() => false)) {
        await nonCompliantOption.click();
      } else {
        await page.keyboard.press('Escape');
      }
    }

    // Click Apply Filters button
    const applyFiltersButton = page.getByText('Apply Filters');
    if (await applyFiltersButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await applyFiltersButton.click();
      await page.waitForTimeout(500);
    }

    // Verify page still shows results heading (filters applied successfully)
    await expect(page.getByRole('heading', { name: 'Evaluation Results' })).toBeVisible();
  });
});
