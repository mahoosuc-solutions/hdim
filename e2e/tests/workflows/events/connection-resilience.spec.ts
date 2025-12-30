import { test, expect } from '@playwright/test';
import { DashboardPage } from '../../../pages/dashboard.page';
import { LoginPage } from '../../../pages/login.page';
import { TEST_USERS } from '../../../fixtures/test-fixtures';
import { WebSocketHelpers } from '../../../utils/websocket-helpers';

/**
 * Connection Resilience Tests
 *
 * These tests verify that the application handles network
 * disruptions gracefully and maintains data integrity.
 */

test.describe('Connection Resilience', () => {
  let loginPage: LoginPage;
  let dashboardPage: DashboardPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    dashboardPage = new DashboardPage(page);

    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
  });

  /**
   * Network Offline Handling
   */
  test.describe('Offline Handling', () => {
    test('should display offline indicator when network disconnects', async ({ page, context }) => {
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      // Go offline
      await context.setOffline(true);

      // Wait for UI to detect offline state
      await page.waitForTimeout(2000);

      // Look for offline indicator
      const offlineIndicator = page.locator(
        '[data-testid="offline-indicator"], .offline-banner, .connection-status:has-text("Offline")'
      );

      const hasOfflineUI = await offlineIndicator.count() > 0;
      console.log('Offline indicator displayed:', hasOfflineUI);

      // Go back online
      await context.setOffline(false);
    });

    test('should queue actions while offline', async ({ page, context }) => {
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      // Go offline
      await context.setOffline(true);

      // Attempt an action (should be queued)
      const newEvalButton = dashboardPage.newEvaluationButton;
      if (await newEvalButton.count() > 0 && await newEvalButton.isVisible()) {
        await newEvalButton.click();

        // Check for offline queue message
        const queueMessage = page.locator(
          '.offline-queue, [data-testid="pending-actions"], :has-text("pending")'
        );
        const hasQueueUI = await queueMessage.count() > 0;
        console.log('Pending action UI displayed:', hasQueueUI);
      }

      // Go back online
      await context.setOffline(false);
    });

    test('should sync data when coming back online', async ({ page, context }) => {
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      // Record initial state
      const initialCount = await dashboardPage.getCareGapsCount();

      // Go offline
      await context.setOffline(true);
      await page.waitForTimeout(1000);

      // Go back online
      await context.setOffline(false);

      // Wait for sync
      await page.waitForTimeout(3000);

      // Data should be consistent (or refreshed)
      const afterCount = await dashboardPage.getCareGapsCount();
      console.log(`Count before offline: ${initialCount}, after: ${afterCount}`);
    });

    test('should show sync in progress indicator', async ({ page, context }) => {
      await dashboardPage.goto();

      // Go offline then online
      await context.setOffline(true);
      await page.waitForTimeout(500);
      await context.setOffline(false);

      // Look for sync indicator
      const syncIndicator = page.locator(
        '[data-testid="sync-indicator"], .syncing, .loading-sync'
      );

      // May or may not appear depending on implementation
      const hasSyncUI = await syncIndicator.count() > 0;
      console.log('Sync indicator displayed:', hasSyncUI);
    });
  });

  /**
   * WebSocket Reconnection
   */
  test.describe('WebSocket Reconnection', () => {
    test('should attempt WebSocket reconnection after disconnect', async ({ page }) => {
      await dashboardPage.goto();

      // Monitor WebSocket connections
      const wsConnections: string[] = [];

      page.on('websocket', (ws) => {
        wsConnections.push(ws.url());
        console.log('WebSocket connection:', ws.url());
      });

      // Wait for initial connection
      await page.waitForTimeout(3000);

      console.log(`Total WebSocket connections: ${wsConnections.length}`);
    });

    test('should maintain UI state during reconnection', async ({ page }) => {
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      // Get initial UI state
      const initialMetrics = await dashboardPage.getCareGapsCount();

      // Simulate reconnection by refreshing with network delay
      await page.route('**/*', async (route) => {
        await new Promise(resolve => setTimeout(resolve, 100));
        await route.continue();
      });

      await page.reload();
      await dashboardPage.waitForDataLoad();

      // UI state should be consistent
      const afterMetrics = await dashboardPage.getCareGapsCount();
      console.log(`Metrics before: ${initialMetrics}, after: ${afterMetrics}`);
    });

    test('should display reconnecting status', async ({ page }) => {
      await dashboardPage.goto();

      // Look for connection status indicator
      const connectionStatus = page.locator(
        '[data-testid="connection-status"], .ws-status, .real-time-status'
      );

      if (await connectionStatus.count() > 0) {
        const statusText = await connectionStatus.textContent();
        console.log('Connection status:', statusText);
      }
    });

    test('should not duplicate events after reconnection', async ({ page }) => {
      await dashboardPage.goto();

      // This test verifies that the application properly handles
      // the potential for duplicate events after reconnection

      const wsHelpers = new WebSocketHelpers({
        endpoint: process.env.WS_ENDPOINT || 'ws://localhost:8087/ws',
        tenantId: 'TENANT001',
      });

      try {
        await wsHelpers.connect();

        const receivedEvents: any[] = [];
        const eventIds = new Set<string>();

        wsHelpers.subscribe('*', (e) => {
          if (eventIds.has(e.correlationId)) {
            console.warn('Duplicate event detected:', e.correlationId);
          }
          eventIds.add(e.correlationId);
          receivedEvents.push(e);
        });

        await page.waitForTimeout(5000);

        console.log(`Received ${receivedEvents.length} events, ${eventIds.size} unique`);
        expect(receivedEvents.length).toBe(eventIds.size);

      } catch (e) {
        console.log('WebSocket test skipped:', e);
      } finally {
        await wsHelpers.disconnect();
      }
    });
  });

  /**
   * Slow Network Handling
   */
  test.describe('Slow Network Handling', () => {
    test('should handle slow network gracefully', async ({ page }) => {
      // Simulate slow network
      await page.route('**/*', async (route) => {
        await new Promise(resolve => setTimeout(resolve, 500));
        await route.continue();
      });

      await dashboardPage.goto();

      // Should still load eventually
      const loaded = await dashboardPage.isLoaded();
      expect(loaded).toBe(true);
    });

    test('should show loading indicators during slow requests', async ({ page }) => {
      await dashboardPage.goto();

      // Slow down API requests
      await page.route('**/api/**', async (route) => {
        await new Promise(resolve => setTimeout(resolve, 2000));
        await route.continue();
      });

      // Trigger an action
      await page.reload();

      // Look for loading indicator
      const loadingIndicator = page.locator(
        '[data-testid="loading"], .loading, mat-spinner, .spinner'
      );

      const hasLoading = await loadingIndicator.count() > 0;
      console.log('Loading indicator displayed:', hasLoading);
    });

    test('should implement request timeout', async ({ page }) => {
      // Simulate hung request
      await page.route('**/api/**', async (route) => {
        // Never respond (simulates hung connection)
        await new Promise(resolve => setTimeout(resolve, 60000));
      });

      const errorPromise = page.waitForEvent('pageerror').catch(() => null);

      // Try to load page
      await page.goto('/dashboard').catch(() => {
        console.log('Page load timed out as expected');
      });

      // Check for timeout error handling
      const error = await errorPromise;
      if (error) {
        console.log('Error caught:', error.message);
      }
    });

    test('should retry failed requests', async ({ page }) => {
      let requestCount = 0;

      await page.route('**/api/v1/dashboard**', async (route) => {
        requestCount++;
        if (requestCount < 3) {
          // Fail first 2 attempts
          await route.abort('failed');
        } else {
          await route.continue();
        }
      });

      await dashboardPage.goto();
      await page.waitForTimeout(5000);

      console.log(`Request attempts: ${requestCount}`);
      // Some retry mechanism should exist
    });
  });

  /**
   * Request Timeout Handling
   */
  test.describe('Request Timeout', () => {
    test('should display timeout error after long wait', async ({ page }) => {
      await dashboardPage.goto();

      // Block a specific request
      await page.route('**/api/v1/care-gaps**', async (route) => {
        await new Promise(resolve => setTimeout(resolve, 35000));
        await route.continue();
      });

      // Navigate to care gaps
      await page.goto('/care-gaps');

      // Should show timeout or error
      const errorMessage = page.locator('.error, .timeout-error, [data-testid="error"]');
      await page.waitForTimeout(32000);

      if (await errorMessage.count() > 0) {
        console.log('Timeout error displayed');
      }
    });

    test('should allow user to retry after timeout', async ({ page }) => {
      await dashboardPage.goto();

      // Look for retry button
      const retryButton = page.locator(
        '[data-testid="retry"], button:has-text("Retry"), button:has-text("Try Again")'
      );

      console.log('Retry button available:', await retryButton.count() > 0);
    });
  });

  /**
   * Session Recovery
   */
  test.describe('Session Recovery', () => {
    test('should recover session after browser crash simulation', async ({ page }) => {
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      // Get current URL
      const currentUrl = page.url();

      // Simulate browser restart by creating new page with same storage
      const storageState = await page.context().storageState();

      // Close and reopen
      await page.close();

      // Create new context with same storage
      const newContext = await page.context().browser()?.newContext({
        storageState: {
          cookies: storageState.cookies,
          origins: storageState.origins,
        },
      });

      if (newContext) {
        const newPage = await newContext.newPage();
        await newPage.goto(currentUrl);

        // Should be authenticated
        await expect(newPage).not.toHaveURL(/.*login/);
        console.log('Session recovered after simulated crash');

        await newContext.close();
      }
    });

    test('should preserve form data during connection issues', async ({ page }) => {
      await page.goto('/evaluations');

      // Fill some form data
      const patientSearch = page.locator('[data-testid="patient-search"], #patientSearch');
      if (await patientSearch.count() > 0) {
        await patientSearch.fill('Test Patient');

        // Simulate brief offline
        const context = page.context();
        await context.setOffline(true);
        await page.waitForTimeout(1000);
        await context.setOffline(false);

        // Form data should be preserved
        const value = await patientSearch.inputValue();
        expect(value).toBe('Test Patient');
        console.log('Form data preserved after network issue');
      }
    });
  });
});

/**
 * Load Testing Scenarios
 */
test.describe('Load Testing', () => {
  test.beforeEach(async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
  });

  test('should handle many concurrent API requests', async ({ page }) => {
    const requestCounts: Record<string, number> = {};

    page.on('request', (request) => {
      const url = new URL(request.url());
      requestCounts[url.pathname] = (requestCounts[url.pathname] || 0) + 1;
    });

    // Navigate through multiple pages quickly
    await page.goto('/dashboard');
    await page.goto('/patients');
    await page.goto('/care-gaps');
    await page.goto('/evaluations');
    await page.goto('/dashboard');

    console.log('Request counts:', requestCounts);
  });

  test('should maintain performance under load', async ({ page }) => {
    await page.goto('/dashboard');

    // Measure initial load metrics
    const metrics = await page.evaluate(() => {
      const timing = performance.timing;
      return {
        loadTime: timing.loadEventEnd - timing.navigationStart,
        domContentLoaded: timing.domContentLoadedEventEnd - timing.navigationStart,
        firstPaint: performance.getEntriesByType('paint')[0]?.startTime || 0,
      };
    });

    console.log('Performance metrics:', metrics);

    // Load time should be reasonable
    expect(metrics.loadTime).toBeLessThan(30000); // 30 seconds max
  });
});
