import { test, expect } from '@playwright/test';
import { DEMO_USER } from './fixtures/auth.fixture';

test('has title', async ({ page }) => {
  // Set up authentication via localStorage before navigation
  await page.addInitScript((demoUser) => {
    localStorage.setItem('healthdata_auth_token', 'demo-jwt-token-' + Date.now());
    localStorage.setItem('healthdata_user', JSON.stringify(demoUser));
  }, DEMO_USER);

  await page.goto('/');
  await page.waitForLoadState('domcontentloaded');

  // Expect h1 to contain either the app title or dashboard title
  const h1Text = await page.locator('h1').first().innerText();
  expect(h1Text).toMatch(/Health Data|Clinical|Dashboard/i);
});
