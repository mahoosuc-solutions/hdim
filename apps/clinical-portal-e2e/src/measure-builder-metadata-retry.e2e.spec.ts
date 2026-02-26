import { expect, type Page } from '@playwright/test';
import {
  authenticatedTest as test,
  waitForAppReady,
} from './fixtures/auth.fixture';

const measure = {
  id: 'm-1',
  tenantId: 'acme-health',
  name: 'Measure One',
  version: '1.0.0',
  status: 'DRAFT',
  description: 'Initial description',
  category: 'CUSTOM',
  year: 2026,
  owner: 'Team A',
  clinicalFocus: 'Diabetes',
  reportingCadence: 'MONTHLY',
  targetThreshold: '75%',
  priority: 'MEDIUM',
  implementationNotes: 'Pilot',
  tags: 'quality,diabetes',
  createdBy: 'measure-builder-user',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

async function ensureAuthenticatedOnMeasureBuilder(page: Page): Promise<void> {
  await page.goto('/login', { waitUntil: 'domcontentloaded' });
  await page.evaluate(() => {
    const user = {
      id: 'demo-user-1',
      username: 'demo',
      email: 'demo@healthdata.com',
      firstName: 'Demo',
      lastName: 'User',
      fullName: 'Demo User',
      roles: [
        {
          id: 'role-admin',
          name: 'ADMIN',
          description: 'Administrator',
          permissions: [
            { id: 'perm-1', name: 'VIEW_PATIENTS' },
            { id: 'perm-2', name: 'EDIT_PATIENTS' },
            { id: 'perm-3', name: 'VIEW_EVALUATIONS' },
            { id: 'perm-4', name: 'RUN_EVALUATIONS' },
            { id: 'perm-5', name: 'EXPORT_DATA' },
            { id: 'perm-6', name: 'VIEW_REPORTS' },
            { id: 'perm-7', name: 'VIEW_ANALYTICS' },
            { id: 'perm-8', name: 'VIEW_CARE_GAPS' },
          ],
        },
      ],
      tenantId: 'acme-health',
      tenantIds: ['acme-health'],
      active: true,
    };
    localStorage.setItem('healthdata_user', JSON.stringify(user));
    localStorage.setItem('healthdata_tenant', 'acme-health');
    localStorage.setItem('hdim_first_visit_complete', 'true');
    localStorage.setItem('hdim_tour_dashboard_complete', 'true');
  });
  await page.goto('/measure-builder', { waitUntil: 'domcontentloaded' });
  await waitForAppReady(page);
}

test.describe('Measure Builder Metadata Dialog', () => {
  test.describe.configure({ mode: 'serial' });
  // Keep this aligned with API_CONFIG.RETRY_ATTEMPTS (currently 3):
  // 1 initial PUT + 3 interceptor retries = 4 failed attempts before surface error.
  const attemptsBeforeDialogError = 4;

  test('shows non-field alert and supports retry on transient update failure', async ({
    authenticatedPage: page,
  }) => {
    let updateAttempts = 0;

    await page.route('**/quality-measure/custom-measures**', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([measure]),
        });
        return;
      }
      await route.fallback();
    });

    await page.route(
      '**/quality-measure/custom-measures/m-1',
      async (route) => {
        if (route.request().method() !== 'PUT') {
          await route.fallback();
          return;
        }

        updateAttempts += 1;
        if (updateAttempts <= attemptsBeforeDialogError) {
          await route.fulfill({
            status: 500,
            contentType: 'application/json',
            body: JSON.stringify({
              message: 'Service unavailable',
            }),
          });
          return;
        }

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            ...measure,
            name: 'Measure One Updated',
          }),
        });
      }
    );

    await page.goto('/measure-builder', { waitUntil: 'domcontentloaded' });
    await ensureAuthenticatedOnMeasureBuilder(page);

    await expect(page.getByText('Measure One').first()).toBeVisible();

    const firstRow = page.locator('table tbody tr, tr.mat-mdc-row').first();
    await firstRow.locator('button:has(mat-icon:has-text("more_vert"))').click();
    await page.getByRole('menuitem', { name: 'Edit Details' }).click();

    await expect(page.getByRole('dialog')).toBeVisible();
    await page.getByLabel('Measure Name').fill('Measure One Updated');

    await page.getByRole('button', { name: 'Save Details' }).click();
    const alertBanner = page.getByRole('alert');
    await expect(alertBanner).toBeVisible({ timeout: 15000 });
    await expect(alertBanner).toContainText(
      /Service unavailable|Internal Server Error|Failed to update measure details/i
    );
    await expect(page.getByRole('button', { name: 'Retry' })).toBeVisible();

    await page.getByRole('button', { name: 'Retry' }).click();

    await expect(page.getByRole('dialog')).not.toBeVisible();
    expect(updateAttempts).toBe(attemptsBeforeDialogError + 1);
    await expect(page.getByText('Measure One Updated').first()).toBeVisible();
  });

  test('maps backend field validation errors inline and keeps dialog open', async ({
    authenticatedPage: page,
  }) => {
    await page.route('**/quality-measure/custom-measures**', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([measure]),
        });
        return;
      }
      await route.fallback();
    });

    await page.route(
      '**/quality-measure/custom-measures/m-1',
      async (route) => {
        if (route.request().method() !== 'PUT') {
          await route.fallback();
          return;
        }

        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            fieldErrors: [
              {
                field: 'priority',
                defaultMessage: 'Priority must be LOW, MEDIUM, or HIGH',
              },
              {
                field: 'year',
                defaultMessage: 'Year must be >= 2000',
              },
            ],
          }),
        });
      }
    );

    await page.goto('/measure-builder', { waitUntil: 'domcontentloaded' });
    await ensureAuthenticatedOnMeasureBuilder(page);

    await expect(page.getByText('Measure One').first()).toBeVisible();

    const firstRow = page.locator('table tbody tr, tr.mat-mdc-row').first();
    const rowActionsButton = firstRow.locator(
      'button:has(mat-icon:has-text("more_vert"))'
    );
    await expect(rowActionsButton).toBeVisible();
    await rowActionsButton.click();
    await page.getByRole('menuitem', { name: 'Edit Details' }).click();

    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible();

    await page.getByLabel('Measure Name').fill('Measure One Updated');
    await page.getByRole('button', { name: 'Save Details' }).click();

    await expect(dialog).toBeVisible();
    await expect(dialog).toContainText('Priority must be LOW, MEDIUM, or HIGH');
    await expect(dialog).toContainText('Year must be >= 2000');
    await expect(page.getByRole('alert')).toHaveCount(0);
  });
});
