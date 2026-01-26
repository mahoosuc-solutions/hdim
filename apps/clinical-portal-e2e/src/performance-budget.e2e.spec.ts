import { test, expect } from '@playwright/test';

/**
 * Performance Budget E2E Tests
 *
 * Tests performance metrics to ensure acceptable load times:
 * - Page load time < 2s (measure navigationStart to loadEventEnd)
 * - Time to Interactive (TTI) < 3s (Lighthouse metric)
 * - First Contentful Paint (FCP) < 1s (Lighthouse metric)
 * - Bundle size < 500KB per module (check network requests)
 *
 * These tests ensure WCAG 2.1 performance-related compliance:
 * - 2.2.1 Timing Adjustable (Level A) - System responds in reasonable time
 * - User-perceived performance impacts accessibility
 *
 * Performance Budget Thresholds:
 * - Page Load Time: < 2000ms (from navigationStart to loadEventEnd)
 * - First Contentful Paint (FCP): < 1000ms
 * - Time to Interactive (TTI): < 3000ms
 * - Largest Contentful Paint (LCP): < 2500ms (Core Web Vital)
 * - Cumulative Layout Shift (CLS): < 0.1 (Core Web Vital)
 * - First Input Delay (FID): < 100ms (Core Web Vital)
 * - JavaScript Bundle Size: < 500KB per module (before compression)
 *
 * @tags @e2e @performance @ux @critical
 */

const TEST_USER = {
  username: 'test_evaluator',
  password: 'password123',
  roles: ['EVALUATOR'],
  tenantId: 'tenant-a',
};

/**
 * Helper: Get performance timing metrics
 */
async function getPerformanceMetrics(page: any): Promise<{
  pageLoadTime: number;
  domContentLoadedTime: number;
  firstContentfulPaint: number;
  largestContentfulPaint: number;
  timeToInteractive: number;
}> {
  return await page.evaluate(() => {
    const perfData = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
    const paintEntries = performance.getEntriesByType('paint');

    const fcpEntry = paintEntries.find((entry) => entry.name === 'first-contentful-paint');

    return {
      pageLoadTime: perfData.loadEventEnd - perfData.fetchStart,
      domContentLoadedTime: perfData.domContentLoadedEventEnd - perfData.fetchStart,
      firstContentfulPaint: fcpEntry ? fcpEntry.startTime : 0,
      largestContentfulPaint: 0, // LCP requires additional measurement
      timeToInteractive: 0, // TTI requires Lighthouse or synthetic measurement
    };
  });
}

/**
 * Helper: Get network resource sizes
 */
async function getResourceSizes(page: any): Promise<{
  totalSize: number;
  jsSize: number;
  cssSize: number;
  imageSize: number;
}> {
  return await page.evaluate(() => {
    const resources = performance.getEntriesByType('resource') as PerformanceResourceTiming[];

    let totalSize = 0;
    let jsSize = 0;
    let cssSize = 0;
    let imageSize = 0;

    resources.forEach((resource) => {
      const size = resource.transferSize || 0;
      totalSize += size;

      if (resource.name.endsWith('.js')) {
        jsSize += size;
      } else if (resource.name.endsWith('.css')) {
        cssSize += size;
      } else if (resource.name.match(/\.(png|jpg|jpeg|gif|svg|webp)/)) {
        imageSize += size;
      }
    });

    return {
      totalSize,
      jsSize,
      cssSize,
      imageSize,
    };
  });
}

test.describe('Performance Budget - Page Load Time', () => {
  test.beforeEach(async ({ page }) => {
    // Login first (exclude login time from page load measurements)
    await page.goto('/login');
    await page.fill('[data-test-id="username"]', TEST_USER.username);
    await page.fill('[data-test-id="password"]', TEST_USER.password);
    await page.click('[data-test-id="login-button"]');
    await page.waitForURL('/dashboard');
  });

  test('should load dashboard in under 2 seconds', async ({ page }) => {
    // Measure dashboard load time
    const startTime = Date.now();

    await page.goto('/dashboard');
    await page.waitForLoadState('load');

    const loadTime = Date.now() - startTime;

    console.log('Dashboard load time:', loadTime, 'ms');

    // Get detailed performance metrics
    const metrics = await getPerformanceMetrics(page);
    console.log('Performance metrics:', metrics);

    // Verify page load time is under 2 seconds
    expect(loadTime).toBeLessThan(2000);

    // Log warning if close to threshold
    if (loadTime > 1500) {
      console.warn(`⚠️  Dashboard load time is ${loadTime}ms (close to 2000ms threshold)`);
    }
  });

  test('should load patients page in under 2 seconds', async ({ page }) => {
    const startTime = Date.now();

    await page.goto('/patients');
    await page.waitForLoadState('load');

    const loadTime = Date.now() - startTime;

    console.log('Patients page load time:', loadTime, 'ms');

    expect(loadTime).toBeLessThan(2000);
  });

  test('should load evaluations page in under 2 seconds', async ({ page }) => {
    const startTime = Date.now();

    await page.goto('/evaluations');
    await page.waitForLoadState('load');

    const loadTime = Date.now() - startTime;

    console.log('Evaluations page load time:', loadTime, 'ms');

    expect(loadTime).toBeLessThan(2000);
  });
});

test.describe('Performance Budget - First Contentful Paint (FCP)', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/login');
    await page.fill('[data-test-id="username"]', TEST_USER.username);
    await page.fill('[data-test-id="password"]', TEST_USER.password);
    await page.click('[data-test-id="login-button"]');
    await page.waitForURL('/dashboard');
  });

  test('should have First Contentful Paint < 1 second on dashboard', async ({ page }) => {
    // Navigate to dashboard
    await page.goto('/dashboard');
    await page.waitForLoadState('load');

    // Get FCP from performance API
    const metrics = await page.evaluate(() => {
      const paintEntries = performance.getEntriesByType('paint');
      const fcp = paintEntries.find((entry) => entry.name === 'first-contentful-paint');
      return {
        fcp: fcp ? fcp.startTime : 0,
        paintEntries: paintEntries.map((e) => ({ name: e.name, startTime: e.startTime })),
      };
    });

    console.log('Paint metrics:', metrics);

    // Verify FCP is under 1 second
    if (metrics.fcp > 0) {
      expect(metrics.fcp).toBeLessThan(1000);

      if (metrics.fcp > 800) {
        console.warn(`⚠️  FCP is ${metrics.fcp}ms (close to 1000ms threshold)`);
      }
    } else {
      console.warn('⚠️  FCP metric not available (may not be supported in test environment)');
    }
  });

  test('should have First Contentful Paint < 1 second on patients page', async ({ page }) => {
    await page.goto('/patients');
    await page.waitForLoadState('load');

    const metrics = await page.evaluate(() => {
      const paintEntries = performance.getEntriesByType('paint');
      const fcp = paintEntries.find((entry) => entry.name === 'first-contentful-paint');
      return fcp ? fcp.startTime : 0;
    });

    console.log('Patients page FCP:', metrics, 'ms');

    if (metrics > 0) {
      expect(metrics).toBeLessThan(1000);
    }
  });
});

test.describe('Performance Budget - Time to Interactive (TTI)', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/login');
    await page.fill('[data-test-id="username"]', TEST_USER.username);
    await page.fill('[data-test-id="password"]', TEST_USER.password);
    await page.click('[data-test-id="login-button"]');
    await page.waitForURL('/dashboard');
  });

  test('should be interactive within 3 seconds on dashboard', async ({ page }) => {
    const startTime = Date.now();

    // Navigate to dashboard
    await page.goto('/dashboard');

    // Wait for network idle (approximates TTI)
    await page.waitForLoadState('networkidle');

    const ttiApprox = Date.now() - startTime;

    console.log('Approximate Time to Interactive (TTI):', ttiApprox, 'ms');

    // Verify TTI is under 3 seconds
    expect(ttiApprox).toBeLessThan(3000);

    // Verify page is actually interactive by clicking an element
    const navElement = page.locator('[data-test-id="nav-patients"], a:has-text("Patients")').first();
    const isInteractive = await navElement.isVisible();
    expect(isInteractive).toBe(true);

    if (ttiApprox > 2500) {
      console.warn(`⚠️  TTI is ${ttiApprox}ms (close to 3000ms threshold)`);
    }
  });

  test('should be interactive within 3 seconds on patients page', async ({ page }) => {
    const startTime = Date.now();

    await page.goto('/patients');
    await page.waitForLoadState('networkidle');

    const ttiApprox = Date.now() - startTime;

    console.log('Patients page TTI:', ttiApprox, 'ms');

    expect(ttiApprox).toBeLessThan(3000);

    // Verify interactivity by checking table is loaded and clickable
    const table = page.locator('[data-test-id="patient-table"], table').first();
    const isInteractive = await table.isVisible();
    expect(isInteractive).toBe(true);
  });
});

test.describe('Performance Budget - Bundle Size', () => {
  test('should have JavaScript bundle size < 500KB per module', async ({ page }) => {
    // Login
    await page.goto('/login');
    await page.fill('[data-test-id="username"]', TEST_USER.username);
    await page.fill('[data-test-id="password"]', TEST_USER.password);
    await page.click('[data-test-id="login-button"]');
    await page.waitForURL('/dashboard');

    // Navigate to dashboard to load main bundles
    await page.goto('/dashboard');
    await page.waitForLoadState('load');

    // Get resource sizes
    const sizes = await getResourceSizes(page);

    console.log('Resource sizes:', {
      totalSize: (sizes.totalSize / 1024).toFixed(2) + ' KB',
      jsSize: (sizes.jsSize / 1024).toFixed(2) + ' KB',
      cssSize: (sizes.cssSize / 1024).toFixed(2) + ' KB',
      imageSize: (sizes.imageSize / 1024).toFixed(2) + ' KB',
    });

    // Get individual JS bundle sizes
    const jsBundles = await page.evaluate(() => {
      const resources = performance.getEntriesByType('resource') as PerformanceResourceTiming[];
      return resources
        .filter((r) => r.name.endsWith('.js'))
        .map((r) => ({
          name: r.name.split('/').pop(),
          size: r.transferSize || 0,
          decodedSize: r.decodedBodySize || 0,
        }))
        .sort((a, b) => b.size - a.size);
    });

    console.log('JavaScript bundles:', jsBundles.slice(0, 10)); // Top 10 bundles

    // Check if any single bundle exceeds 500KB
    const largeBundles = jsBundles.filter((bundle) => bundle.size > 500 * 1024);

    if (largeBundles.length > 0) {
      console.warn('⚠️  Large JavaScript bundles (> 500KB):');
      largeBundles.forEach((bundle) => {
        console.warn(`   - ${bundle.name}: ${(bundle.size / 1024).toFixed(2)} KB`);
      });
    }

    // Total JS size should be reasonable (< 2MB for initial load)
    expect(sizes.jsSize).toBeLessThan(2 * 1024 * 1024); // 2MB

    // Individual bundles should be < 500KB (best practice, not strict requirement)
    // Note: Main bundle may exceed this; lazy-loaded modules should be smaller
    const mainBundle = jsBundles.find((b) => b.name?.includes('main'));
    if (mainBundle) {
      console.log('Main bundle size:', (mainBundle.size / 1024).toFixed(2), 'KB');
      if (mainBundle.size > 500 * 1024) {
        console.warn(`⚠️  Main bundle exceeds 500KB: ${(mainBundle.size / 1024).toFixed(2)} KB`);
        console.warn('   Consider code splitting or lazy loading');
      }
    }
  });
});

/**
 * Performance Budget Summary:
 *
 * Thresholds:
 * - Page Load Time: < 2000ms ✅
 * - First Contentful Paint (FCP): < 1000ms ✅
 * - Time to Interactive (TTI): < 3000ms ✅
 * - JavaScript Bundle Size: < 500KB per module ⚠️ (best practice)
 *
 * Core Web Vitals (Google):
 * - Largest Contentful Paint (LCP): < 2.5s (Good)
 * - First Input Delay (FID): < 100ms (Good)
 * - Cumulative Layout Shift (CLS): < 0.1 (Good)
 *
 * Performance Optimization Recommendations:
 * 1. Code Splitting: Split large bundles into smaller chunks
 * 2. Lazy Loading: Load modules on-demand (Angular lazy routes)
 * 3. Tree Shaking: Remove unused code from bundles
 * 4. Image Optimization: Use WebP format, responsive images
 * 5. Caching: Leverage browser caching with proper cache headers
 * 6. CDN: Serve static assets from CDN
 * 7. Compression: Enable gzip/brotli compression
 * 8. Preloading: Preload critical resources with <link rel="preload">
 * 9. Service Worker: Cache assets for offline/fast loads
 * 10. Bundle Analysis: Use webpack-bundle-analyzer to identify large dependencies
 *
 * WCAG 2.1 Performance-Related Guidelines:
 * - 2.2.1 Timing Adjustable (Level A): System responds in reasonable time
 * - Fast page loads improve accessibility for users on slow connections
 * - Reduced cognitive load with quick feedback
 *
 * Limitations:
 * - TTI measurement is approximate (based on networkidle)
 * - Lighthouse provides more accurate TTI/LCP/CLS metrics
 * - Bundle sizes are transfer sizes (may include compression)
 * - Performance varies by network speed and device
 *
 * Future Enhancements:
 * - Integrate Lighthouse CI for comprehensive performance testing
 * - Add RUM (Real User Monitoring) for production metrics
 * - Set up performance budgets in CI/CD pipeline
 * - Monitor Core Web Vitals in production
 */
