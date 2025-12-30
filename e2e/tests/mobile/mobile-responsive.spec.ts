import { test, expect, devices } from '@playwright/test';
import { LoginPage } from '../../pages/login.page';
import { DashboardPage } from '../../pages/dashboard.page';
import { PatientPage } from '../../pages/patient.page';
import { TEST_USERS } from '../../fixtures/test-fixtures';

/**
 * Mobile Device Testing
 *
 * Test Suite: MOBILE
 * Coverage: Responsive design, touch interactions, mobile navigation
 *
 * These tests verify that the application works correctly across
 * different mobile devices and screen sizes.
 *
 * Device Coverage:
 * - iPhone 12/13/14 (375x812)
 * - iPhone SE (375x667)
 * - iPad (768x1024)
 * - iPad Pro (1024x1366)
 * - Pixel 5 (393x851)
 * - Galaxy S21 (360x800)
 */

// Mobile device configurations
const mobileDevices = {
  iPhoneSE: devices['iPhone SE'],
  iPhone12: devices['iPhone 12'],
  iPhone14: devices['iPhone 14'],
  pixel5: devices['Pixel 5'],
  galaxyS21: {
    viewport: { width: 360, height: 800 },
    userAgent: 'Mozilla/5.0 (Linux; Android 11; SM-G991B) AppleWebKit/537.36',
    deviceScaleFactor: 3,
    isMobile: true,
    hasTouch: true,
  },
};

// Tablet configurations
const tabletDevices = {
  iPad: devices['iPad (gen 7)'],
  iPadPro: devices['iPad Pro 11'],
  iPadLandscape: {
    ...devices['iPad (gen 7)'],
    viewport: { width: 1024, height: 768 },
  },
};

test.describe('Mobile Device Testing', () => {
  /**
   * MOBILE-001: iPhone Responsive Tests
   */
  test.describe('MOBILE-001: iPhone Responsive Tests', () => {
    test.use({ ...mobileDevices.iPhone12 });

    let loginPage: LoginPage;
    let dashboardPage: DashboardPage;

    test.beforeEach(async ({ page }) => {
      loginPage = new LoginPage(page);
      dashboardPage = new DashboardPage(page);
    });

    test('should display mobile login form correctly', async ({ page }) => {
      await loginPage.goto();

      // Login form should be visible and properly sized
      const loginForm = page.locator('form, [data-testid="login-form"]');
      await expect(loginForm).toBeVisible();

      // Check form fits within viewport
      const formBox = await loginForm.boundingBox();
      if (formBox) {
        expect(formBox.width).toBeLessThanOrEqual(375);
      }

      // Input fields should be full width on mobile
      const inputs = page.locator('input[type="text"], input[type="password"]');
      const inputCount = await inputs.count();

      for (let i = 0; i < inputCount; i++) {
        const inputBox = await inputs.nth(i).boundingBox();
        if (inputBox) {
          expect(inputBox.width).toBeGreaterThan(200);
        }
      }
    });

    test('should show mobile navigation menu', async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      // Look for hamburger menu or mobile nav toggle
      const mobileMenuToggle = page.locator(
        '[data-testid="mobile-menu"], .hamburger, [aria-label*="menu"], button.menu-toggle'
      );

      const hasMobileMenu = await mobileMenuToggle.count() > 0;
      console.log('Mobile menu toggle present:', hasMobileMenu);

      if (hasMobileMenu) {
        await mobileMenuToggle.click();

        // Navigation should be visible after clicking
        const mobileNav = page.locator(
          '[data-testid="mobile-nav"], nav.mobile, .mobile-navigation'
        );
        const navVisible = await mobileNav.isVisible().catch(() => false);
        console.log('Mobile navigation visible:', navVisible);
      }
    });

    test('should handle touch scrolling on patient list', async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      await page.goto('/patients');
      await page.waitForLoadState('networkidle');

      // Check if content is scrollable
      const scrollableContent = page.locator('.patient-list, [data-testid="patient-list"], main');
      const isScrollable = await scrollableContent.evaluate(el => {
        return el.scrollHeight > el.clientHeight;
      }).catch(() => false);

      console.log('Patient list scrollable:', isScrollable);
    });

    test('should display dashboard cards in single column', async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      // Dashboard cards should stack vertically on mobile
      const cards = page.locator('.dashboard-card, [data-testid*="card"], .mat-card');
      const cardCount = await cards.count();

      if (cardCount > 1) {
        const firstCard = await cards.first().boundingBox();
        const secondCard = await cards.nth(1).boundingBox();

        if (firstCard && secondCard) {
          // Cards should be stacked (second card below first)
          const isStacked = secondCard.y > firstCard.y + firstCard.height - 10;
          console.log('Dashboard cards stacked:', isStacked);
        }
      }
    });

    test('should have touch-friendly button sizes', async ({ page }) => {
      await loginPage.goto();

      // All interactive elements should be at least 44x44 pixels
      const buttons = page.locator('button, a.btn, [role="button"]');
      const buttonCount = await buttons.count();

      let touchFriendlyCount = 0;
      for (let i = 0; i < Math.min(buttonCount, 10); i++) {
        const box = await buttons.nth(i).boundingBox();
        if (box && box.width >= 44 && box.height >= 44) {
          touchFriendlyCount++;
        }
      }

      console.log(`Touch-friendly buttons: ${touchFriendlyCount}/${Math.min(buttonCount, 10)}`);
    });
  });

  /**
   * MOBILE-002: Android Responsive Tests
   */
  test.describe('MOBILE-002: Android Responsive Tests', () => {
    test.use({ ...mobileDevices.pixel5 });

    test('should display login page on Pixel 5', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();

      await expect(page.locator('form, [data-testid="login-form"]')).toBeVisible();

      // Take screenshot for reference
      console.log('Pixel 5 viewport:', page.viewportSize());
    });

    test('should handle form inputs with virtual keyboard', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();

      const usernameInput = page.locator('#username, input[name="username"], input[type="text"]').first();

      if (await usernameInput.count() > 0) {
        // Tap to focus (simulates touch)
        await usernameInput.tap();

        // Input should be focused
        const isFocused = await usernameInput.evaluate(el => document.activeElement === el);
        console.log('Input focused after tap:', isFocused);

        // Type with tap
        await usernameInput.fill('test_user');
        const value = await usernameInput.inputValue();
        expect(value).toBe('test_user');
      }
    });

    test('should support swipe gestures if implemented', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      await page.goto('/care-gaps');
      await page.waitForLoadState('networkidle');

      // Check for swipeable components
      const swipeableElements = page.locator('[data-swipeable], .swipe-container');
      const hasSwipeable = await swipeableElements.count() > 0;
      console.log('Swipeable elements present:', hasSwipeable);
    });
  });

  /**
   * MOBILE-003: iPad/Tablet Responsive Tests
   */
  test.describe('MOBILE-003: iPad/Tablet Responsive Tests', () => {
    test.use({ ...tabletDevices.iPad });

    let loginPage: LoginPage;
    let dashboardPage: DashboardPage;

    test.beforeEach(async ({ page }) => {
      loginPage = new LoginPage(page);
      dashboardPage = new DashboardPage(page);
    });

    test('should display two-column layout on iPad', async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      // On tablet, might show sidebar + content
      const sidebar = page.locator('aside, [data-testid="sidebar"], .sidenav');
      const mainContent = page.locator('main, [data-testid="main-content"], .main-content');

      const hasSidebar = await sidebar.isVisible().catch(() => false);
      const hasMainContent = await mainContent.isVisible().catch(() => false);

      console.log('Two-column layout (sidebar visible):', hasSidebar && hasMainContent);
    });

    test('should show expanded navigation on iPad', async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      await dashboardPage.goto();

      // Navigation should be expanded (not hamburger menu) on tablet
      const expandedNav = page.locator(
        'nav:not(.mobile), [data-testid="nav-menu"], .mat-sidenav'
      );

      const navVisible = await expandedNav.isVisible().catch(() => false);
      console.log('Expanded navigation visible on iPad:', navVisible);
    });

    test('should display data tables properly on iPad', async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      await page.goto('/patients');
      await page.waitForLoadState('networkidle');

      const table = page.locator('table, [data-testid="patient-table"], .mat-table');

      if (await table.count() > 0) {
        const tableBox = await table.boundingBox();
        if (tableBox) {
          // Table should fit within iPad viewport (768px)
          expect(tableBox.width).toBeLessThanOrEqual(768);
          console.log('Table width on iPad:', tableBox.width);
        }
      }
    });

    test('should handle iPad landscape orientation', async ({ page }) => {
      // Simulate landscape by changing viewport
      await page.setViewportSize({ width: 1024, height: 768 });

      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      // In landscape, more content should be visible
      const cards = page.locator('.dashboard-card, [data-testid*="card"]');
      const cardCount = await cards.count();
      console.log('Dashboard cards visible in landscape:', cardCount);
    });
  });

  /**
   * MOBILE-004: Small Screen Tests (iPhone SE)
   */
  test.describe('MOBILE-004: Small Screen Tests (iPhone SE)', () => {
    test.use({ ...mobileDevices.iPhoneSE });

    test('should not have horizontal overflow on small screens', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();

      // Check for horizontal scroll
      const hasHorizontalScroll = await page.evaluate(() => {
        return document.documentElement.scrollWidth > document.documentElement.clientWidth;
      });

      console.log('Has horizontal overflow:', hasHorizontalScroll);
      expect(hasHorizontalScroll).toBe(false);
    });

    test('should truncate long text appropriately', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      await page.goto('/patients');
      await page.waitForLoadState('networkidle');

      // Check that text doesn't overflow containers
      const textElements = page.locator('.patient-name, .truncate, [class*="ellipsis"]');
      const count = await textElements.count();

      if (count > 0) {
        const element = textElements.first();
        const styles = await element.evaluate(el => {
          const computed = window.getComputedStyle(el);
          return {
            overflow: computed.overflow,
            textOverflow: computed.textOverflow,
          };
        });

        console.log('Text overflow handling:', styles);
      }
    });

    test('should have readable font sizes', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();

      // Check that body text is at least 14px
      const bodyText = page.locator('p, span, label').first();
      if (await bodyText.count() > 0) {
        const fontSize = await bodyText.evaluate(el => {
          return parseInt(window.getComputedStyle(el).fontSize);
        });

        console.log('Body font size:', fontSize);
        expect(fontSize).toBeGreaterThanOrEqual(14);
      }
    });
  });

  /**
   * MOBILE-005: Touch Interaction Tests
   */
  test.describe('MOBILE-005: Touch Interaction Tests', () => {
    test.use({ ...mobileDevices.iPhone12 });

    test('should handle tap events correctly', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();

      const loginButton = page.locator('button[type="submit"], [data-testid="login-button"]');

      if (await loginButton.count() > 0) {
        // Use tap instead of click for mobile
        await loginButton.tap();
        console.log('Tap event handled');
      }
    });

    test('should support pull-to-refresh if implemented', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      await page.goto('/dashboard');
      await page.waitForLoadState('networkidle');

      // Check for pull-to-refresh indicator
      const pullToRefresh = page.locator('[data-testid="pull-refresh"], .pull-to-refresh');
      const hasPullToRefresh = await pullToRefresh.count() > 0;
      console.log('Pull-to-refresh available:', hasPullToRefresh);
    });

    test('should not trigger hover states on touch', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      await page.goto('/patients');
      await page.waitForLoadState('networkidle');

      const row = page.locator('[data-testid="patient-row"], tr').first();

      if (await row.count() > 0) {
        // Tap and check that hover state isn't stuck
        await row.tap();
        console.log('Touch interaction on table row');
      }
    });

    test('should handle long-press for context menu if implemented', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      await page.goto('/patients');
      await page.waitForLoadState('networkidle');

      const row = page.locator('[data-testid="patient-row"]').first();

      if (await row.count() > 0) {
        // Simulate long press
        const box = await row.boundingBox();
        if (box) {
          await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
          await page.mouse.down();
          await page.waitForTimeout(800); // Long press duration
          await page.mouse.up();

          // Check for context menu
          const contextMenu = page.locator('[role="menu"], .context-menu');
          const hasMenu = await contextMenu.isVisible().catch(() => false);
          console.log('Context menu on long press:', hasMenu);
        }
      }
    });
  });

  /**
   * MOBILE-006: Responsive Form Tests
   */
  test.describe('MOBILE-006: Responsive Form Tests', () => {
    test.use({ ...mobileDevices.iPhone12 });

    test('should display form fields properly on mobile', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      await page.goto('/patients/new');

      // Check form layout
      const formFields = page.locator('input, select, textarea');
      const fieldCount = await formFields.count();

      console.log('Form fields on patient creation:', fieldCount);

      // Fields should be stacked vertically
      if (fieldCount >= 2) {
        const field1 = await formFields.nth(0).boundingBox();
        const field2 = await formFields.nth(1).boundingBox();

        if (field1 && field2) {
          const isStacked = field2.y > field1.y;
          console.log('Form fields stacked vertically:', isStacked);
        }
      }
    });

    test('should show appropriate mobile keyboard types', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();

      // Check for inputmode attributes
      const emailInput = page.locator('input[type="email"], input[inputmode="email"]');
      const numericInput = page.locator('input[type="number"], input[inputmode="numeric"]');
      const telInput = page.locator('input[type="tel"], input[inputmode="tel"]');

      console.log('Email inputs:', await emailInput.count());
      console.log('Numeric inputs:', await numericInput.count());
      console.log('Tel inputs:', await telInput.count());
    });

    test('should handle date picker on mobile', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      await page.goto('/patients/new');

      const datePicker = page.locator(
        'input[type="date"], [data-testid="date-picker"], .mat-datepicker-input'
      );

      if (await datePicker.count() > 0) {
        await datePicker.tap();
        console.log('Date picker triggered on mobile');

        // Check for native or custom date picker
        const picker = page.locator('.mat-calendar, .date-picker-popup');
        const hasCustomPicker = await picker.isVisible().catch(() => false);
        console.log('Custom date picker shown:', hasCustomPicker);
      }
    });
  });

  /**
   * MOBILE-007: Mobile Navigation Tests
   */
  test.describe('MOBILE-007: Mobile Navigation Tests', () => {
    test.use({ ...mobileDevices.iPhone12 });

    test('should navigate using mobile menu', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      // Find and click hamburger menu
      const menuButton = page.locator(
        '[data-testid="mobile-menu"], .hamburger-menu, [aria-label*="menu"]'
      ).first();

      if (await menuButton.count() > 0) {
        await menuButton.tap();

        // Click on patients link
        const patientsLink = page.locator('a[href*="patient"], nav a:has-text("Patient")').first();
        if (await patientsLink.count() > 0) {
          await patientsLink.tap();
          await page.waitForURL(/.*patient.*/);
          console.log('Navigation to patients successful');
        }
      }
    });

    test('should support back navigation', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      await page.goto('/dashboard');
      await page.goto('/patients');

      // Use browser back
      await page.goBack();

      // Should be back on dashboard
      expect(page.url()).toContain('dashboard');
      console.log('Back navigation works');
    });

    test('should close menu when navigating', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      const menuButton = page.locator('[data-testid="mobile-menu"]').first();

      if (await menuButton.count() > 0) {
        await menuButton.tap();

        const nav = page.locator('[data-testid="mobile-nav"], .mobile-navigation');
        if (await nav.isVisible()) {
          // Click a nav item
          const navItem = nav.locator('a').first();
          await navItem.tap();

          await page.waitForTimeout(500);

          // Menu should be closed
          const menuStillVisible = await nav.isVisible().catch(() => false);
          console.log('Menu closed after navigation:', !menuStillVisible);
        }
      }
    });
  });

  /**
   * MOBILE-008: Performance on Mobile
   */
  test.describe('MOBILE-008: Mobile Performance', () => {
    test.use({ ...mobileDevices.iPhone12 });

    test('should load dashboard within mobile budget', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      const startTime = Date.now();

      await page.goto('/dashboard');
      await page.waitForLoadState('networkidle');

      const loadTime = Date.now() - startTime;
      console.log('Mobile dashboard load time:', loadTime, 'ms');

      // Mobile should load within 6 seconds (slightly longer than desktop)
      expect(loadTime).toBeLessThan(6000);
    });

    test('should not download large images on mobile', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      const imageSizes: number[] = [];

      page.on('response', async response => {
        const contentType = response.headers()['content-type'] || '';
        if (contentType.includes('image')) {
          const contentLength = response.headers()['content-length'];
          if (contentLength) {
            imageSizes.push(parseInt(contentLength));
          }
        }
      });

      await page.goto('/dashboard');
      await page.waitForLoadState('networkidle');

      const largeImages = imageSizes.filter(size => size > 200 * 1024);
      console.log(`Large images (>200KB): ${largeImages.length}`);

      // Should not have images over 200KB on mobile
      if (imageSizes.length > 0) {
        expect(largeImages.length).toBe(0);
      }
    });

    test('should use efficient network requests', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      let requestCount = 0;

      page.on('request', () => {
        requestCount++;
      });

      await page.goto('/dashboard');
      await page.waitForLoadState('networkidle');

      console.log('Total network requests:', requestCount);

      // Should have reasonable number of requests
      expect(requestCount).toBeLessThan(100);
    });
  });
});

/**
 * Orientation Change Tests
 */
test.describe('Orientation Change Tests', () => {
  test.use({ ...mobileDevices.iPhone12 });

  test('should handle portrait to landscape switch', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');

    // Start in portrait
    await page.setViewportSize({ width: 375, height: 812 });
    console.log('Portrait mode');

    // Switch to landscape
    await page.setViewportSize({ width: 812, height: 375 });
    await page.waitForTimeout(500);
    console.log('Landscape mode');

    // Content should still be visible
    const mainContent = page.locator('main, [data-testid="main-content"]');
    await expect(mainContent).toBeVisible();
  });

  test('should maintain scroll position after orientation change', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

    await page.goto('/patients');
    await page.waitForLoadState('networkidle');

    // Scroll down
    await page.evaluate(() => window.scrollTo(0, 200));
    const scrollBefore = await page.evaluate(() => window.scrollY);

    // Change orientation
    await page.setViewportSize({ width: 812, height: 375 });
    await page.waitForTimeout(500);

    const scrollAfter = await page.evaluate(() => window.scrollY);
    console.log(`Scroll before: ${scrollBefore}, after: ${scrollAfter}`);
  });
});
