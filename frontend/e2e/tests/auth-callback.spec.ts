import { test, expect } from '@playwright/test';

test.describe('Auth callback flow', () => {
  test('stores token from callback URL and redirects to evaluations', async ({ page }) => {
    await page.goto('/auth/callback?access_token=e2e-token-123');

    await page.waitForURL('**/evaluations');

    const token = await page.evaluate(() => localStorage.getItem('authToken'));
    expect(token).toBe('e2e-token-123');
  });

  test('shows callback error when token is missing', async ({ page }) => {
    await page.goto('/auth/callback');

    await expect(page.getByRole('heading', { name: /sign in callback error/i })).toBeVisible();
    await expect(page.getByText(/missing token/i)).toBeVisible();
  });
});
