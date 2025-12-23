import { test, expect } from '@playwright/test';

test('has title', async ({ page }) => {
  await page.goto('/');

  // Expect h1 to contain the dashboard title (use first() to avoid strict mode violation)
  expect(await page.locator('h1').first().innerText()).toContain('Clinical Portal');
});
