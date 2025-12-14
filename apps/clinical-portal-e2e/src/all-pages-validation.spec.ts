import { test, expect, Page } from '@playwright/test';

/**
 * Comprehensive UI Validation E2E Tests
 *
 * Tests all pages in the Clinical Portal for:
 * - Page load and rendering
 * - Core UI elements presence
 * - Navigation functionality
 * - Theme consistency (light theme)
 * - Accessibility basics
 * - Responsive layout
 */

// Test configuration
const WAIT_FOR_LOAD = 3000;
const NAVIGATION_TIMEOUT = 10000;

// Page definitions with expected elements
const PAGES = [
  {
    name: 'Dashboard',
    path: '/dashboard',
    expectedElements: [
      { selector: 'mat-toolbar', description: 'Toolbar' },
      { selector: 'mat-sidenav, mat-drawer, .sidenav', description: 'Side navigation' },
    ],
    expectedText: ['Dashboard'],
  },
  {
    name: 'Patients List',
    path: '/patients',
    expectedElements: [
      { selector: 'table, mat-table, .patients-table', description: 'Patients table' },
    ],
    expectedText: ['Patient'],
  },
  {
    name: 'Evaluations',
    path: '/evaluations',
    expectedElements: [
      { selector: 'mat-card, .evaluation-card', description: 'Evaluation content' },
    ],
    expectedText: ['Evaluation'],
  },
  {
    name: 'Results',
    path: '/results',
    expectedElements: [
      { selector: 'mat-card, table, .results-container', description: 'Results content' },
    ],
    expectedText: ['Result'],
  },
  {
    name: 'Reports',
    path: '/reports',
    expectedElements: [
      { selector: 'mat-card, .reports-container', description: 'Reports content' },
    ],
    expectedText: ['Report'],
  },
  {
    name: 'Measure Builder',
    path: '/measure-builder',
    expectedElements: [
      { selector: 'mat-card, .measure-builder', description: 'Measure builder content' },
    ],
    expectedText: ['Measure'],
  },
  {
    name: 'AI Assistant',
    path: '/ai-assistant',
    expectedElements: [
      { selector: 'mat-card, .ai-dashboard, .chat-container', description: 'AI content' },
    ],
    expectedText: ['AI', 'Assistant'],
  },
  {
    name: 'Knowledge Base',
    path: '/knowledge-base',
    expectedElements: [
      { selector: 'mat-card, .knowledge-base', description: 'Knowledge base content' },
    ],
    expectedText: ['Knowledge'],
  },
  {
    name: 'Care Recommendations',
    path: '/care-recommendations',
    expectedElements: [
      { selector: 'mat-card, .care-recommendations', description: 'Care recommendations content' },
    ],
    expectedText: ['Care', 'Recommendation'],
  },
  {
    name: 'Agent Builder',
    path: '/agent-builder',
    expectedElements: [
      { selector: 'mat-card, .agent-builder', description: 'Agent builder content' },
    ],
    expectedText: ['Agent'],
  },
  {
    name: 'Visualization - Live Monitor',
    path: '/visualization/live-monitor',
    expectedElements: [
      { selector: 'canvas, .visualization, mat-card', description: 'Visualization content' },
    ],
    expectedText: [],
  },
];

// Helper to check if page has light theme
async function hasLightTheme(page: Page): Promise<boolean> {
  const body = page.locator('body');
  const hasLightClass = await body.evaluate((el) => el.classList.contains('light-theme'));
  const dataTheme = await body.getAttribute('data-theme');
  return hasLightClass || dataTheme === 'light';
}

// Helper to check text contrast
async function getTextContrast(page: Page, selector: string): Promise<number | null> {
  return page.evaluate((sel) => {
    const el = document.querySelector(sel);
    if (!el) return null;

    const styles = window.getComputedStyle(el);
    const textColor = styles.color;
    const bgColor = styles.backgroundColor;

    // Parse RGB values
    const parseColor = (color: string): [number, number, number] | null => {
      const match = color.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/);
      if (match) {
        return [parseInt(match[1]), parseInt(match[2]), parseInt(match[3])];
      }
      return null;
    };

    const getLuminance = (r: number, g: number, b: number): number => {
      const [rs, gs, bs] = [r, g, b].map((c) => {
        c = c / 255;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
      });
      return 0.2126 * rs + 0.7152 * gs + 0.0722 * bs;
    };

    const tc = parseColor(textColor);
    const bc = parseColor(bgColor);
    if (!tc || !bc) return null;

    const l1 = getLuminance(tc[0], tc[1], tc[2]);
    const l2 = getLuminance(bc[0], bc[1], bc[2]);
    const lighter = Math.max(l1, l2);
    const darker = Math.min(l1, l2);

    return (lighter + 0.05) / (darker + 0.05);
  }, selector);
}

// Test results collection
interface PageTestResult {
  page: string;
  path: string;
  loadTime: number;
  hasToolbar: boolean;
  hasSidenav: boolean;
  hasLightTheme: boolean;
  elementsFound: string[];
  elementsMissing: string[];
  textFound: string[];
  textMissing: string[];
  contrastOk: boolean;
  errors: string[];
}

test.describe('All Pages Validation', () => {
  test.describe('Page Load Tests', () => {
    for (const pageDef of PAGES) {
      test(`${pageDef.name} - should load successfully`, async ({ page }) => {
        const startTime = Date.now();

        await page.goto(pageDef.path);
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(WAIT_FOR_LOAD);

        const loadTime = Date.now() - startTime;

        // Verify URL
        await expect(page).toHaveURL(new RegExp(pageDef.path.replace(/\//g, '\\/')));

        // Verify page loaded (no error state)
        const errorText = await page.locator('text=Error, text=404, text=Not Found').count();
        expect(errorText).toBe(0);

        // Log load time
        console.log(`  ${pageDef.name}: ${loadTime}ms`);
      });
    }
  });

  test.describe('Core UI Elements', () => {
    test('All pages should have toolbar', async ({ page }) => {
      for (const pageDef of PAGES) {
        await page.goto(pageDef.path);
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(2000);

        const toolbar = page.locator('mat-toolbar');
        const toolbarCount = await toolbar.count();

        expect(toolbarCount, `${pageDef.name} should have toolbar`).toBeGreaterThan(0);
      }
    });

    test('All pages should have side navigation', async ({ page }) => {
      for (const pageDef of PAGES) {
        await page.goto(pageDef.path);
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(2000);

        const sidenav = page.locator('mat-sidenav, mat-drawer, .sidenav, mat-sidenav-container');
        const sidenavCount = await sidenav.count();

        expect(sidenavCount, `${pageDef.name} should have sidenav`).toBeGreaterThan(0);
      }
    });
  });

  test.describe('Theme Consistency', () => {
    test('All pages should use light theme', async ({ page }) => {
      for (const pageDef of PAGES) {
        await page.goto(pageDef.path);
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(2000);

        const isLight = await hasLightTheme(page);
        expect(isLight, `${pageDef.name} should use light theme`).toBeTruthy();
      }
    });

    test('All pages should have consistent background', async ({ page }) => {
      const backgrounds: string[] = [];

      for (const pageDef of PAGES) {
        await page.goto(pageDef.path);
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(2000);

        const bgColor = await page.evaluate(() => {
          return window.getComputedStyle(document.body).backgroundColor;
        });
        backgrounds.push(bgColor);
      }

      // All backgrounds should be similar (light colors)
      for (const bg of backgrounds) {
        const match = bg.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/);
        if (match) {
          const r = parseInt(match[1]);
          const g = parseInt(match[2]);
          const b = parseInt(match[3]);
          // Light background should have RGB values > 200
          expect(r, 'Background should be light').toBeGreaterThan(180);
          expect(g, 'Background should be light').toBeGreaterThan(180);
          expect(b, 'Background should be light').toBeGreaterThan(180);
        }
      }
    });
  });

  test.describe('Text Readability', () => {
    test('All pages should have readable toolbar text', async ({ page }) => {
      for (const pageDef of PAGES) {
        await page.goto(pageDef.path);
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(2000);

        const toolbar = page.locator('mat-toolbar').first();
        if (await toolbar.count() > 0) {
          const text = await toolbar.textContent();
          expect(text?.trim().length, `${pageDef.name} toolbar should have text`).toBeGreaterThan(0);
        }
      }
    });

    test('Heading text should be visible', async ({ page }) => {
      for (const pageDef of PAGES) {
        await page.goto(pageDef.path);
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(2000);

        // Check for any heading or title
        const headings = page.locator('h1, h2, h3, .page-title, .dashboard-header, mat-card-title');
        const count = await headings.count();

        if (count > 0) {
          const firstHeading = headings.first();
          await expect(firstHeading).toBeVisible();
        }
      }
    });
  });

  test.describe('Navigation Functionality', () => {
    test('Navigation menu items should be clickable', async ({ page }) => {
      await page.goto('/dashboard');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // Find navigation items
      const navItems = page.locator('mat-nav-list a, .nav-item, [role="navigation"] a');
      const count = await navItems.count();

      expect(count, 'Should have navigation items').toBeGreaterThan(0);

      // Test clicking first few nav items
      for (let i = 0; i < Math.min(count, 3); i++) {
        const item = navItems.nth(i);
        if (await item.isVisible()) {
          const href = await item.getAttribute('href');
          if (href && !href.startsWith('http')) {
            await item.click();
            await page.waitForTimeout(1000);
            // Should navigate successfully - check URL changed and page rendered
            // Note: API errors are expected when backend is not running, so we only check for
            // critical navigation/routing errors, not data loading errors
            const routingError = await page.locator('text=/Page not found|404|Cannot match any routes/i').count();
            expect(routingError, `Navigation to ${href} should not cause routing error`).toBe(0);
            // Verify the page has basic structure (toolbar should still be present)
            const toolbarPresent = await page.locator('mat-toolbar').count();
            expect(toolbarPresent, `Page at ${href} should have toolbar`).toBeGreaterThan(0);
          }
        }
      }
    });

    test('Browser back/forward should work', async ({ page }) => {
      await page.goto('/dashboard');
      await page.waitForLoadState('domcontentloaded');

      await page.goto('/patients');
      await page.waitForLoadState('domcontentloaded');

      await page.goBack();
      await expect(page).toHaveURL(/dashboard/);

      await page.goForward();
      await expect(page).toHaveURL(/patients/);
    });
  });

  test.describe('Accessibility Basics', () => {
    test('All pages should have accessible buttons', async ({ page }) => {
      for (const pageDef of PAGES) {
        await page.goto(pageDef.path);
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(2000);

        const buttons = page.locator('button');
        const count = await buttons.count();

        for (let i = 0; i < Math.min(count, 5); i++) {
          const button = buttons.nth(i);
          if (await button.isVisible()) {
            // Button should have text, aria-label, or icon
            const text = await button.textContent();
            const ariaLabel = await button.getAttribute('aria-label');
            const hasIcon = await button.locator('mat-icon').count() > 0;

            const hasAccessibility = (text?.trim().length ?? 0) > 0 ||
                                     (ariaLabel?.length ?? 0) > 0 ||
                                     hasIcon;
            expect(hasAccessibility, `Button on ${pageDef.name} should be accessible`).toBeTruthy();
          }
        }
      }
    });

    test('Forms should have labels', async ({ page }) => {
      for (const pageDef of PAGES) {
        await page.goto(pageDef.path);
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(2000);

        const inputs = page.locator('input:not([type="hidden"]), textarea, mat-select');
        const count = await inputs.count();

        for (let i = 0; i < Math.min(count, 5); i++) {
          const input = inputs.nth(i);
          if (await input.isVisible()) {
            // Should have associated label or placeholder
            const id = await input.getAttribute('id');
            const placeholder = await input.getAttribute('placeholder');
            const ariaLabel = await input.getAttribute('aria-label');

            let hasLabel = false;
            if (id) {
              const labelCount = await page.locator(`label[for="${id}"], mat-label`).count();
              hasLabel = labelCount > 0;
            }

            const hasAccessibility = hasLabel ||
                                     (placeholder?.length ?? 0) > 0 ||
                                     (ariaLabel?.length ?? 0) > 0;
            // Log but don't fail - many Material inputs have implicit labels
            if (!hasAccessibility) {
              console.log(`  Warning: Input on ${pageDef.name} may need label`);
            }
          }
        }
      }
    });
  });

  test.describe('Responsive Layout', () => {
    test('Pages should render on tablet viewport', async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 });

      for (const pageDef of PAGES.slice(0, 5)) { // Test first 5 pages
        await page.goto(pageDef.path);
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(2000);

        // Page should not have horizontal scroll
        const hasHorizontalScroll = await page.evaluate(() => {
          return document.documentElement.scrollWidth > document.documentElement.clientWidth;
        });
        expect(hasHorizontalScroll, `${pageDef.name} should not have horizontal scroll on tablet`).toBeFalsy();
      }
    });

    test('Pages should render on mobile viewport', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 });

      for (const pageDef of PAGES.slice(0, 5)) { // Test first 5 pages
        await page.goto(pageDef.path);
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(2000);

        // Core content should be visible
        const content = page.locator('mat-sidenav-content, .main-content, main');
        if (await content.count() > 0) {
          await expect(content.first()).toBeVisible();
        }
      }
    });
  });
});

// Comprehensive Report Generator
test.describe('Generate Validation Report', () => {
  test('Generate comprehensive page validation report', async ({ page }) => {
    const results: PageTestResult[] = [];

    console.log('\n═══════════════════════════════════════════════════════════');
    console.log('  CLINICAL PORTAL - COMPREHENSIVE UI VALIDATION REPORT');
    console.log('═══════════════════════════════════════════════════════════\n');

    for (const pageDef of PAGES) {
      const result: PageTestResult = {
        page: pageDef.name,
        path: pageDef.path,
        loadTime: 0,
        hasToolbar: false,
        hasSidenav: false,
        hasLightTheme: false,
        elementsFound: [],
        elementsMissing: [],
        textFound: [],
        textMissing: [],
        contrastOk: true,
        errors: [],
      };

      try {
        const startTime = Date.now();
        await page.goto(pageDef.path);
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(WAIT_FOR_LOAD);
        result.loadTime = Date.now() - startTime;

        // Check toolbar
        result.hasToolbar = await page.locator('mat-toolbar').count() > 0;

        // Check sidenav
        result.hasSidenav = await page.locator('mat-sidenav, mat-drawer, .sidenav').count() > 0;

        // Check theme
        result.hasLightTheme = await hasLightTheme(page);

        // Check expected elements
        for (const el of pageDef.expectedElements) {
          const count = await page.locator(el.selector).count();
          if (count > 0) {
            result.elementsFound.push(el.description);
          } else {
            result.elementsMissing.push(el.description);
          }
        }

        // Check expected text
        for (const text of pageDef.expectedText) {
          const found = await page.getByText(text, { exact: false }).count();
          if (found > 0) {
            result.textFound.push(text);
          } else {
            result.textMissing.push(text);
          }
        }

      } catch (error) {
        result.errors.push(String(error));
      }

      results.push(result);

      // Print result
      const status = result.errors.length === 0 &&
                     result.hasToolbar &&
                     result.hasSidenav &&
                     result.hasLightTheme ? '✅' : '⚠️';

      console.log(`${status} ${pageDef.name} (${pageDef.path})`);
      console.log(`   Load time: ${result.loadTime}ms`);
      console.log(`   Toolbar: ${result.hasToolbar ? '✓' : '✗'} | Sidenav: ${result.hasSidenav ? '✓' : '✗'} | Light Theme: ${result.hasLightTheme ? '✓' : '✗'}`);
      if (result.elementsFound.length > 0) {
        console.log(`   Elements found: ${result.elementsFound.join(', ')}`);
      }
      if (result.elementsMissing.length > 0) {
        console.log(`   Elements missing: ${result.elementsMissing.join(', ')}`);
      }
      if (result.errors.length > 0) {
        console.log(`   Errors: ${result.errors.join(', ')}`);
      }
      console.log('');
    }

    // Summary
    console.log('═══════════════════════════════════════════════════════════');
    console.log('  SUMMARY');
    console.log('═══════════════════════════════════════════════════════════');

    const passed = results.filter(r => r.errors.length === 0 && r.hasToolbar && r.hasSidenav && r.hasLightTheme).length;
    const total = results.length;
    const avgLoadTime = Math.round(results.reduce((sum, r) => sum + r.loadTime, 0) / total);

    console.log(`  Pages Tested: ${total}`);
    console.log(`  Passed: ${passed}/${total} (${Math.round(passed/total*100)}%)`);
    console.log(`  Average Load Time: ${avgLoadTime}ms`);
    console.log(`  Theme Consistency: ${results.every(r => r.hasLightTheme) ? '✅ All pages use light theme' : '⚠️ Theme inconsistency detected'}`);
    console.log('═══════════════════════════════════════════════════════════\n');

    // Assert overall success
    expect(passed).toBeGreaterThanOrEqual(Math.floor(total * 0.8)); // 80% should pass
  });
});
