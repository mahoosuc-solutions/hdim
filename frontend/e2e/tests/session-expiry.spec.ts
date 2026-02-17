import { test, expect } from '@playwright/test';

test.describe('Session expiry recovery flow', () => {
  test('forces 401, redirects to login, and recovers via sign in', async ({ page }) => {
    await page.goto('/evaluations');

    const force401Chip = page.getByText('Force 401: Off');
    await expect(force401Chip).toBeVisible();

    await force401Chip.click();
    await expect(page.getByText('Force 401: On')).toBeVisible();

    await page.waitForURL('**/login');
    await expect(page.getByRole('heading', { name: /sign in required/i })).toBeVisible();
    await expect(page.getByText(/session expired/i)).toBeVisible();

    const marker = await page.evaluate(() => localStorage.getItem('sessionExpiredAt'));
    expect(marker).toBeTruthy();

    const authTokenBeforeSignIn = await page.evaluate(() => localStorage.getItem('authToken'));
    expect(authTokenBeforeSignIn).toBeNull();

    await page.getByRole('button', { name: /sign in/i }).click();
    await page.waitForURL('**/evaluations');

    const authTokenAfterSignIn = await page.evaluate(() => localStorage.getItem('authToken'));
    expect(authTokenAfterSignIn).toMatch(/^dev-session-/);
  });
});
