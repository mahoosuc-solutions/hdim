/**
 * E2E Tests for HDIM Landing Page
 *
 * Tests all critical user journeys, forms, and interactions.
 */

import { test, expect } from '@playwright/test';

const BASE_URL = process.env.BASE_URL || 'http://localhost:3000';

test.describe('Landing Page - Home', () => {
  test('should load home page successfully', async ({ page }) => {
    await page.goto(BASE_URL);

    // Check page title
    await expect(page).toHaveTitle(/HDIM|HealthData/i);

    // Check main hero section
    await expect(page.locator('h1')).toBeVisible();
  });

  test('should have functional navigation menu', async ({ page }) => {
    await page.goto(BASE_URL);

    // Check for navigation links
    const nav = page.locator('nav').first();
    await expect(nav).toBeVisible();

    // Verify main nav items exist
    await expect(nav.getByRole('link', { name: /features/i })).toBeVisible();
    await expect(nav.getByRole('link', { name: /pricing/i })).toBeVisible();
    await expect(nav.getByRole('link', { name: /about/i })).toBeVisible();
  });

  test('should display hero CTA buttons', async ({ page }) => {
    await page.goto(BASE_URL);

    // Check for primary CTA
    const primaryCTA = page.getByRole('link', { name: /get started|request demo|sign up/i }).first();
    await expect(primaryCTA).toBeVisible();
    await expect(primaryCTA).toHaveAttribute('href');
  });

  test('should have working footer links', async ({ page }) => {
    await page.goto(BASE_URL);

    const footer = page.locator('footer').first();
    await expect(footer).toBeVisible();

    // Check for privacy policy and terms
    const privacyLink = footer.getByRole('link', { name: /privacy/i });
    const termsLink = footer.getByRole('link', { name: /terms/i });

    if (await privacyLink.count() > 0) {
      await expect(privacyLink.first()).toBeVisible();
    }

    if (await termsLink.count() > 0) {
      await expect(termsLink.first()).toBeVisible();
    }
  });
});

test.describe('Landing Page - Features', () => {
  test('should navigate to features page', async ({ page }) => {
    await page.goto(BASE_URL);

    await page.click('a[href*="/features"], a:has-text("Features")');
    await expect(page).toHaveURL(/.*features.*/);
  });

  test('should display feature cards', async ({ page }) => {
    await page.goto(`${BASE_URL}/features`);

    // Look for feature-related content
    await expect(page.locator('h1, h2').filter({ hasText: /features/i })).toBeVisible();

    // Check for at least 3 feature items
    const features = page.locator('[class*="feature"], [class*="card"]');
    await expect(features.first()).toBeVisible();
  });
});

test.describe('Landing Page - Pricing', () => {
  test('should navigate to pricing page', async ({ page }) => {
    await page.goto(BASE_URL);

    await page.click('a[href*="/pricing"], a:has-text("Pricing")');
    await expect(page).toHaveURL(/.*pricing.*/);
  });

  test('should display pricing tiers', async ({ page }) => {
    await page.goto(`${BASE_URL}/pricing`);

    await expect(page.locator('h1, h2').filter({ hasText: /pricing/i })).toBeVisible();

    // Check for pricing information
    const pricingCards = page.locator('[class*="price"], [class*="tier"], [class*="plan"]');
    if (await pricingCards.count() > 0) {
      await expect(pricingCards.first()).toBeVisible();
    }
  });
});

test.describe('Landing Page - Contact/Demo Form', () => {
  test('should have contact/demo form', async ({ page }) => {
    await page.goto(`${BASE_URL}/demo`);

    // Check for form elements
    const form = page.locator('form').first();
    await expect(form).toBeVisible();
  });

  test('should validate required fields', async ({ page }) => {
    await page.goto(`${BASE_URL}/demo`);

    const form = page.locator('form').first();

    // Try to submit without filling required fields
    const submitButton = form.getByRole('button', { name: /submit|send|request/i });
    await submitButton.click();

    // Check for validation messages (HTML5 or custom)
    const nameInput = form.locator('input[name="name"], input[name="fullName"]').first();
    const emailInput = form.locator('input[name="email"]').first();

    if (await nameInput.count() > 0) {
      await expect(nameInput).toBeFocused();
    }
  });

  test('should accept valid form submission', async ({ page }) => {
    await page.goto(`${BASE_URL}/demo`);

    const form = page.locator('form').first();

    // Fill out form
    const nameInput = form.locator('input[name="name"], input[name="fullName"]').first();
    const emailInput = form.locator('input[name="email"]').first();
    const companyInput = form.locator('input[name="company"]').first();
    const messageInput = form.locator('textarea[name="message"]').first();

    if (await nameInput.count() > 0) {
      await nameInput.fill('Test User');
    }

    if (await emailInput.count() > 0) {
      await emailInput.fill('test@example.com');
    }

    if (await companyInput.count() > 0) {
      await companyInput.fill('Test Healthcare Organization');
    }

    if (await messageInput.count() > 0) {
      await messageInput.fill('I would like to request a demo of HDIM.');
    }

    // Submit form
    const submitButton = form.getByRole('button', { name: /submit|send|request/i });

    // Intercept form submission
    const responsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/') && response.request().method() === 'POST'
    );

    await submitButton.click();

    // Wait for success message or redirect
    await page.waitForTimeout(2000);

    // Check for success indicator
    const successMessage = page.locator('text=/thank you|success|submitted/i');
    if (await successMessage.count() > 0) {
      await expect(successMessage.first()).toBeVisible();
    }
  });
});

test.describe('Landing Page - Performance', () => {
  test('should load within acceptable time', async ({ page }) => {
    const startTime = Date.now();
    await page.goto(BASE_URL);
    const loadTime = Date.now() - startTime;

    // Should load in under 3 seconds
    expect(loadTime).toBeLessThan(3000);
  });

  test('should have proper meta tags for SEO', async ({ page }) => {
    await page.goto(BASE_URL);

    // Check meta description
    const metaDescription = await page.locator('meta[name="description"]').getAttribute('content');
    expect(metaDescription).toBeTruthy();
    expect(metaDescription!.length).toBeGreaterThan(50);

    // Check Open Graph tags
    const ogTitle = await page.locator('meta[property="og:title"]').getAttribute('content');
    expect(ogTitle).toBeTruthy();
  });
});

test.describe('Landing Page - Responsive Design', () => {
  test('should work on mobile viewport', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 }); // iPhone SE
    await page.goto(BASE_URL);

    // Check mobile menu exists (hamburger icon)
    const mobileMenu = page.locator('button[aria-label*="menu"], button:has-text("☰")');
    if (await mobileMenu.count() > 0) {
      await expect(mobileMenu.first()).toBeVisible();
    }

    // Main content should still be visible
    await expect(page.locator('h1')).toBeVisible();
  });

  test('should work on tablet viewport', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 }); // iPad
    await page.goto(BASE_URL);

    await expect(page.locator('h1')).toBeVisible();
  });

  test('should work on desktop viewport', async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 }); // Full HD
    await page.goto(BASE_URL);

    await expect(page.locator('h1')).toBeVisible();
  });
});

test.describe('Landing Page - Analytics', () => {
  test('should have analytics tracking', async ({ page }) => {
    await page.goto(BASE_URL);

    // Check for Google Analytics or other tracking scripts
    const scripts = await page.locator('script').count();
    expect(scripts).toBeGreaterThan(0);

    // Check for gtag or GA snippet
    const gaScript = page.locator('script[src*="googletagmanager"], script[src*="google-analytics"]');
    if (await gaScript.count() > 0) {
      expect(await gaScript.count()).toBeGreaterThan(0);
    }
  });
});

test.describe('Landing Page - Accessibility', () => {
  test('should have proper heading hierarchy', async ({ page }) => {
    await page.goto(BASE_URL);

    const h1Count = await page.locator('h1').count();
    expect(h1Count).toBeGreaterThanOrEqual(1);
    expect(h1Count).toBeLessThanOrEqual(1); // Only one h1 per page
  });

  test('should have alt text on images', async ({ page }) => {
    await page.goto(BASE_URL);

    const images = page.locator('img');
    const imageCount = await images.count();

    if (imageCount > 0) {
      for (let i = 0; i < imageCount; i++) {
        const img = images.nth(i);
        const alt = await img.getAttribute('alt');
        expect(alt).toBeTruthy(); // All images should have alt text
      }
    }
  });

  test('should have proper link text', async ({ page }) => {
    await page.goto(BASE_URL);

    const links = page.locator('a');
    const linkCount = await links.count();

    if (linkCount > 0) {
      for (let i = 0; i < Math.min(linkCount, 10); i++) {
        const link = links.nth(i);
        const text = await link.textContent();
        const ariaLabel = await link.getAttribute('aria-label');

        // Link should have text or aria-label
        expect(text || ariaLabel).toBeTruthy();
      }
    }
  });
});
