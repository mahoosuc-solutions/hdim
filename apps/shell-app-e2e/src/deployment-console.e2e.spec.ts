import { test, expect, APIRequestContext } from '@playwright/test';

const OPS_BASE_URL = process.env['OPS_BASE_URL'] || 'http://localhost:4710';

test.describe('Deployment Console', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: OPS_BASE_URL,
      timeout: 30000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test('loads deployment UI and shows ops connectivity', async ({ page }) => {
    await page.addInitScript((opsUrl) => {
      (window as any).__HDIM_OPS_BASE_URL = opsUrl;
    }, OPS_BASE_URL);

    await page.goto('/deployment');

    await expect(page.getByText('Deployment & Seeding Console')).toBeVisible({ timeout: 20000 });
    await expect(page.getByText('Ops service connected')).toBeVisible({ timeout: 20000 });

    await expect(page.getByText('Compose Override')).toBeVisible();
    await expect(page.locator('textarea[readonly]')).toBeVisible();
  });

  test('ops status responds with services', async () => {
    const response = await apiContext.get('/ops/status');
    expect(response.ok()).toBeTruthy();

    const payload = await response.json();
    expect(Array.isArray(payload.services)).toBeTruthy();
    expect(payload.services.length).toBeGreaterThan(0);
  });
});
