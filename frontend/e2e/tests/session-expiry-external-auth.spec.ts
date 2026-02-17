import { test, expect } from '@playwright/test';

test.describe('Session expiry external auth redirect', () => {
  test('redirects login action to configured external auth URL', async ({ page }) => {
    await page.goto('/login');

    await expect(page.getByRole('heading', { name: /sign in required/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /^sign in$/i })).toBeVisible();

    await page.getByRole('button', { name: /^sign in$/i }).click();

    await page.waitForURL('**/external-login');
    await expect(page.getByRole('heading', { name: /external auth login/i })).toBeVisible();
  });
});
