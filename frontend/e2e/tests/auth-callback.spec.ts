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
    await expect(page.getByText(/missing token\/code/i)).toBeVisible();
  });

  test('shows callback oauth error details', async ({ page }) => {
    await page.goto('/auth/callback?error=access_denied&error_description=Denied');
    await expect(page.getByText(/authorization error: denied/i)).toBeVisible();
  });

  test('shows callback state mismatch error', async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem(
        'pendingSmartAuth',
        JSON.stringify({ state: 'expected-state', createdAt: new Date().toISOString() })
      );
    });
    await page.goto('/auth/callback?state=unexpected-state');
    await expect(page.getByText(/state mismatch detected during smart callback/i)).toBeVisible();
  });
});
