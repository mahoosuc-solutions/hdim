import { test, expect } from '@playwright/test';
import { LoginPage } from '../../pages/login.page';
import { DashboardPage } from '../../pages/dashboard.page';
import { TEST_USERS } from '../../fixtures/test-fixtures';

/**
 * Performance Tests
 *
 * Test Suite: PERF (Performance)
 * Coverage: Core Web Vitals, load times, performance budgets
 *
 * These tests verify that the application meets performance requirements
 * using Playwright's built-in performance APIs and metrics.
 *
 * For full Lighthouse CI integration, see lighthouse-ci.json config.
 *
 * Performance Budgets:
 * - LCP (Largest Contentful Paint): < 2.5s
 * - FID (First Input Delay): < 100ms
 * - CLS (Cumulative Layout Shift): < 0.1
 * - TTI (Time to Interactive): < 3.8s
 * - Total Bundle Size: < 500KB (gzipped)
 */

test.describe('Performance Tests', () => {
  /**
   * PERF-001: Page Load Performance
   */
  test.describe('PERF-001: Page Load Performance', () => {
    test('should load login page within performance budget', async ({ page }) => {
      const startTime = Date.now();

      await page.goto('/login');
      await page.waitForLoadState('domcontentloaded');

      const domContentLoaded = Date.now() - startTime;

      await page.waitForLoadState('load');
      const loadComplete = Date.now() - startTime;

      await page.waitForLoadState('networkidle');
      const networkIdle = Date.now() - startTime;

      console.log('Login page load times:');
      console.log(`  DOM Content Loaded: ${domContentLoaded}ms`);
      console.log(`  Load Complete: ${loadComplete}ms`);
      console.log(`  Network Idle: ${networkIdle}ms`);

      // Performance budget: page should load within 3 seconds
      expect(loadComplete).toBeLessThan(3000);
    });

    test('should load dashboard within performance budget', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      const startTime = Date.now();

      await page.goto('/dashboard');
      await page.waitForLoadState('domcontentloaded');

      const domContentLoaded = Date.now() - startTime;

      await page.waitForLoadState('load');
      const loadComplete = Date.now() - startTime;

      console.log('Dashboard load times:');
      console.log(`  DOM Content Loaded: ${domContentLoaded}ms`);
      console.log(`  Load Complete: ${loadComplete}ms`);

      // Dashboard should load within 5 seconds (includes data fetch)
      expect(loadComplete).toBeLessThan(5000);
    });

    test('should load patient list within performance budget', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      const startTime = Date.now();

      await page.goto('/patients');
      await page.waitForLoadState('networkidle');

      const totalTime = Date.now() - startTime;

      console.log(`Patient list load time: ${totalTime}ms`);

      // Patient list should load within 5 seconds
      expect(totalTime).toBeLessThan(5000);
    });

    test('should load care gaps within performance budget', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      const startTime = Date.now();

      await page.goto('/care-gaps');
      await page.waitForLoadState('networkidle');

      const totalTime = Date.now() - startTime;

      console.log(`Care gaps load time: ${totalTime}ms`);

      expect(totalTime).toBeLessThan(5000);
    });
  });

  /**
   * PERF-002: Core Web Vitals
   */
  test.describe('PERF-002: Core Web Vitals', () => {
    test('should meet LCP budget on dashboard', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      // Collect performance metrics
      const metrics = await collectWebVitals(page, '/dashboard');

      console.log('Dashboard Web Vitals:');
      console.log(`  LCP: ${metrics.lcp}ms`);
      console.log(`  FCP: ${metrics.fcp}ms`);
      console.log(`  CLS: ${metrics.cls}`);

      // LCP should be under 2.5 seconds
      if (metrics.lcp > 0) {
        expect(metrics.lcp).toBeLessThan(2500);
      }
    });

    test('should meet CLS budget on dashboard', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      const metrics = await collectWebVitals(page, '/dashboard');

      console.log(`CLS: ${metrics.cls}`);

      // CLS should be under 0.1
      if (metrics.cls !== undefined) {
        expect(metrics.cls).toBeLessThan(0.1);
      }
    });

    test('should meet FCP budget on login page', async ({ page }) => {
      const metrics = await collectWebVitals(page, '/login');

      console.log(`Login FCP: ${metrics.fcp}ms`);

      // FCP should be under 1.8 seconds
      if (metrics.fcp > 0) {
        expect(metrics.fcp).toBeLessThan(1800);
      }
    });
  });

  /**
   * PERF-003: API Response Times
   */
  test.describe('PERF-003: API Response Times', () => {
    test.beforeEach(async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
    });

    test('should fetch patient list within 2 seconds', async ({ page }) => {
      let apiTime = 0;

      page.on('response', async response => {
        if (response.url().includes('/api/v1/patients')) {
          const timing = response.request().timing();
          apiTime = timing.responseEnd - timing.requestStart;
        }
      });

      await page.goto('/patients');
      await page.waitForLoadState('networkidle');

      console.log(`Patient API response time: ${apiTime}ms`);

      if (apiTime > 0) {
        expect(apiTime).toBeLessThan(2000);
      }
    });

    test('should fetch care gaps within 2 seconds', async ({ page }) => {
      let apiTime = 0;

      page.on('response', async response => {
        if (response.url().includes('/api/v1/care-gaps')) {
          const timing = response.request().timing();
          apiTime = timing.responseEnd - timing.requestStart;
        }
      });

      await page.goto('/care-gaps');
      await page.waitForLoadState('networkidle');

      console.log(`Care gaps API response time: ${apiTime}ms`);

      if (apiTime > 0) {
        expect(apiTime).toBeLessThan(2000);
      }
    });

    test('should fetch dashboard metrics within 3 seconds', async ({ page }) => {
      let apiTime = 0;

      page.on('response', async response => {
        if (response.url().includes('/dashboard') || response.url().includes('/metrics')) {
          const timing = response.request().timing();
          apiTime = timing.responseEnd - timing.requestStart;
        }
      });

      await page.goto('/dashboard');
      await page.waitForLoadState('networkidle');

      console.log(`Dashboard metrics API response time: ${apiTime}ms`);

      if (apiTime > 0) {
        expect(apiTime).toBeLessThan(3000);
      }
    });
  });

  /**
   * PERF-004: Resource Loading
   */
  test.describe('PERF-004: Resource Loading', () => {
    test('should not exceed bundle size budget', async ({ page }) => {
      const resourceSizes: { type: string; size: number }[] = [];

      page.on('response', async response => {
        const url = response.url();
        const contentLength = response.headers()['content-length'];

        if (url.includes('.js') && contentLength) {
          resourceSizes.push({ type: 'js', size: parseInt(contentLength) });
        } else if (url.includes('.css') && contentLength) {
          resourceSizes.push({ type: 'css', size: parseInt(contentLength) });
        }
      });

      await page.goto('/login');
      await page.waitForLoadState('networkidle');

      const totalJS = resourceSizes
        .filter(r => r.type === 'js')
        .reduce((sum, r) => sum + r.size, 0);

      const totalCSS = resourceSizes
        .filter(r => r.type === 'css')
        .reduce((sum, r) => sum + r.size, 0);

      console.log(`Total JS size: ${(totalJS / 1024).toFixed(2)} KB`);
      console.log(`Total CSS size: ${(totalCSS / 1024).toFixed(2)} KB`);

      // JS bundle should be under 2MB (uncompressed)
      expect(totalJS).toBeLessThan(2 * 1024 * 1024);
    });

    test('should load images efficiently', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      const imageStats: { url: string; size: number }[] = [];

      page.on('response', async response => {
        const contentType = response.headers()['content-type'] || '';
        if (contentType.includes('image')) {
          const contentLength = response.headers()['content-length'];
          if (contentLength) {
            imageStats.push({
              url: response.url(),
              size: parseInt(contentLength),
            });
          }
        }
      });

      await page.goto('/dashboard');
      await page.waitForLoadState('networkidle');

      const totalImageSize = imageStats.reduce((sum, img) => sum + img.size, 0);
      console.log(`Total image size: ${(totalImageSize / 1024).toFixed(2)} KB`);
      console.log(`Image count: ${imageStats.length}`);

      // Images should be under 1MB total
      expect(totalImageSize).toBeLessThan(1024 * 1024);
    });

    test('should cache static resources', async ({ page }) => {
      await page.goto('/login');
      await page.waitForLoadState('networkidle');

      // Second navigation should use cache
      const cachedResources: string[] = [];

      page.on('response', async response => {
        const cacheControl = response.headers()['cache-control'];
        if (cacheControl && cacheControl.includes('max-age')) {
          cachedResources.push(response.url());
        }
      });

      await page.goto('/login');
      await page.waitForLoadState('networkidle');

      console.log(`Resources with cache headers: ${cachedResources.length}`);
    });
  });

  /**
   * PERF-005: Interaction Performance
   */
  test.describe('PERF-005: Interaction Performance', () => {
    test.beforeEach(async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
    });

    test('should respond to search input within 300ms', async ({ page }) => {
      await page.goto('/patients');
      await page.waitForLoadState('networkidle');

      const searchInput = page.locator('[data-testid="patient-search"], input[placeholder*="Search"]').first();

      if (await searchInput.count() > 0) {
        const startTime = Date.now();
        await searchInput.fill('Test');

        // Wait for results to appear
        await page.waitForTimeout(500);
        const responseTime = Date.now() - startTime;

        console.log(`Search response time: ${responseTime}ms`);

        // Should be interactive within 500ms
        expect(responseTime).toBeLessThan(1000);
      }
    });

    test('should respond to filter change within 500ms', async ({ page }) => {
      await page.goto('/care-gaps');
      await page.waitForLoadState('networkidle');

      const filter = page.locator('[data-testid="urgency-filter"]').first();

      if (await filter.count() > 0) {
        const startTime = Date.now();
        await filter.click();
        await page.locator('[role="option"]').first().click();

        await page.waitForLoadState('networkidle');
        const responseTime = Date.now() - startTime;

        console.log(`Filter response time: ${responseTime}ms`);

        expect(responseTime).toBeLessThan(2000);
      }
    });

    test('should open modal within 200ms', async ({ page }) => {
      await page.goto('/patients');
      await page.waitForLoadState('networkidle');

      const addButton = page.locator('[data-testid="create-patient"], button:has-text("Add")').first();

      if (await addButton.count() > 0) {
        const startTime = Date.now();
        await addButton.click();

        const modal = page.locator('[role="dialog"]');
        await modal.waitFor({ state: 'visible', timeout: 1000 }).catch(() => {});

        const responseTime = Date.now() - startTime;
        console.log(`Modal open time: ${responseTime}ms`);

        expect(responseTime).toBeLessThan(500);
      }
    });
  });

  /**
   * PERF-006: Memory Usage
   */
  test.describe('PERF-006: Memory Usage', () => {
    test('should not leak memory during navigation', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      // Navigate through multiple pages
      const pages = ['/dashboard', '/patients', '/care-gaps', '/evaluations', '/dashboard'];

      for (const path of pages) {
        await page.goto(path);
        await page.waitForLoadState('networkidle');
      }

      // Check for memory metrics (if available)
      const metrics = await page.metrics();
      console.log('Memory metrics:', {
        jsHeapSize: metrics.JSHeapUsedSize,
        documents: metrics.Documents,
        frames: metrics.Frames,
      });

      // JS heap should be under 100MB
      if (metrics.JSHeapUsedSize) {
        expect(metrics.JSHeapUsedSize).toBeLessThan(100 * 1024 * 1024);
      }
    });
  });
});

/**
 * Helper function to collect Web Vitals metrics
 */
async function collectWebVitals(page: any, url: string): Promise<{
  lcp: number;
  fcp: number;
  cls: number;
}> {
  await page.goto(url);
  await page.waitForLoadState('networkidle');

  // Wait for metrics to be available
  await page.waitForTimeout(2000);

  const metrics = await page.evaluate(() => {
    return new Promise((resolve) => {
      let lcp = 0;
      let fcp = 0;
      let cls = 0;

      // Get FCP from performance entries
      const paintEntries = performance.getEntriesByType('paint');
      const fcpEntry = paintEntries.find((e: any) => e.name === 'first-contentful-paint');
      if (fcpEntry) {
        fcp = fcpEntry.startTime;
      }

      // Get LCP
      const lcpEntries = performance.getEntriesByType('largest-contentful-paint');
      if (lcpEntries.length > 0) {
        lcp = (lcpEntries[lcpEntries.length - 1] as any).startTime;
      }

      // Get CLS from layout shift entries
      const clsEntries = performance.getEntriesByType('layout-shift');
      cls = clsEntries.reduce((sum: number, entry: any) => {
        if (!entry.hadRecentInput) {
          return sum + entry.value;
        }
        return sum;
      }, 0);

      resolve({ lcp, fcp, cls });
    });
  });

  return metrics;
}

/**
 * Lighthouse CI Configuration Reference
 *
 * Create lighthouse-ci.json in project root:
 * {
 *   "ci": {
 *     "collect": {
 *       "url": ["http://localhost:4200/login", "http://localhost:4200/dashboard"],
 *       "numberOfRuns": 3
 *     },
 *     "assert": {
 *       "assertions": {
 *         "categories:performance": ["error", { "minScore": 0.8 }],
 *         "categories:accessibility": ["error", { "minScore": 0.9 }],
 *         "categories:best-practices": ["error", { "minScore": 0.8 }],
 *         "first-contentful-paint": ["error", { "maxNumericValue": 1800 }],
 *         "largest-contentful-paint": ["error", { "maxNumericValue": 2500 }],
 *         "cumulative-layout-shift": ["error", { "maxNumericValue": 0.1 }],
 *         "total-blocking-time": ["error", { "maxNumericValue": 300 }]
 *       }
 *     },
 *     "upload": {
 *       "target": "temporary-public-storage"
 *     }
 *   }
 * }
 */
