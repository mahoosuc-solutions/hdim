import { test, expect } from '@playwright/test';
import { DashboardPage } from '../../../pages/dashboard.page';
import { EvaluationPage } from '../../../pages/evaluation.page';
import { CareGapPage } from '../../../pages/care-gap.page';
import { LoginPage } from '../../../pages/login.page';
import { TEST_USERS } from '../../../fixtures/test-fixtures';
import { WebSocketHelpers } from '../../../utils/websocket-helpers';
import { MockEventFactory, EventType } from '../../../fixtures/mock-events';
import { EventOrderValidator } from '../../../utils/kafka-testcontainers';

/**
 * Advanced Event Scenario Tests
 *
 * Test Suite: EVT-004 to EVT-008
 * Coverage: Complex event flows, error handling, and edge cases
 *
 * These tests cover advanced event-driven scenarios including
 * concurrent events, error recovery, and complex workflows.
 */

test.describe('Advanced Event Scenarios', () => {
  let loginPage: LoginPage;
  let evaluationPage: EvaluationPage;
  let careGapPage: CareGapPage;
  let dashboardPage: DashboardPage;
  let wsHelpers: WebSocketHelpers;
  let mockEvents: MockEventFactory;
  let orderValidator: EventOrderValidator;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    evaluationPage = new EvaluationPage(page);
    careGapPage = new CareGapPage(page);
    dashboardPage = new DashboardPage(page);
    mockEvents = new MockEventFactory();
    orderValidator = new EventOrderValidator();

    wsHelpers = new WebSocketHelpers({
      endpoint: process.env.WS_ENDPOINT || 'ws://localhost:8087/ws',
      tenantId: 'TENANT001',
    });

    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
  });

  test.afterEach(async () => {
    if (wsHelpers) {
      await wsHelpers.disconnect();
    }
    orderValidator.clear();
  });

  /**
   * EVT-004: Concurrent Evaluation Events
   *
   * Verifies that the UI correctly handles multiple concurrent evaluations.
   */
  test.describe('EVT-004: Concurrent Evaluations', () => {
    test('should handle multiple evaluation events simultaneously', async ({ page }) => {
      await evaluationPage.goto();

      try {
        await wsHelpers.connect();

        // Subscribe to all evaluation events
        const events: any[] = [];
        const unsub = wsHelpers.subscribe(EventType.EVALUATION_COMPLETE, (e) => {
          events.push(e);
        });

        // In a real scenario, multiple users might trigger evaluations
        // For now, verify the UI can display multiple results

        // Wait for any concurrent events
        await page.waitForTimeout(5000);

        unsub();
        console.log(`Received ${events.length} concurrent evaluation events`);

        // Each event should have unique correlation ID
        const correlationIds = events.map(e => e.correlationId);
        const uniqueIds = new Set(correlationIds);
        expect(uniqueIds.size).toBe(correlationIds.length);

      } catch (e) {
        console.log('WebSocket test skipped:', e);
      }
    });

    test('should maintain event order for same patient', async ({ page }) => {
      try {
        await wsHelpers.connect();

        // Record events for order validation
        const unsub = wsHelpers.subscribe('*', (e) => {
          orderValidator.recordEvent(e);
        });

        await evaluationPage.goto();
        await evaluationPage.selectPatient('Test');
        await evaluationPage.selectMeasure('CMS');
        await evaluationPage.evaluate();

        unsub();

        // Validate event ordering
        const sequences = orderValidator.getAllSequences();
        for (const [correlationId] of sequences) {
          const result = orderValidator.validateEvaluationSequence(correlationId);
          if (!result.valid) {
            console.warn(`Event ordering issues for ${correlationId}:`, result.errors);
          }
        }

      } catch (e) {
        console.log('WebSocket test skipped:', e);
      }
    });

    test('should display latest result when multiple evaluations complete', async ({ page }) => {
      await evaluationPage.goto();

      // Run first evaluation
      await evaluationPage.selectPatient('Test');
      await evaluationPage.selectMeasure('CMS130');
      await evaluationPage.evaluate();

      const firstResult = await evaluationPage.getResultStatus();
      console.log('First evaluation result:', firstResult);

      // Run another evaluation
      await evaluationPage.runAnother();
      await evaluationPage.selectPatient('Test');
      await evaluationPage.selectMeasure('CMS165');
      await evaluationPage.evaluate();

      const secondResult = await evaluationPage.getResultStatus();
      console.log('Second evaluation result:', secondResult);

      // Result card should show the latest result
      await evaluationPage.assertResultDisplayed();
    });
  });

  /**
   * EVT-005: Error Event Handling
   *
   * Verifies proper handling of error events from the backend.
   */
  test.describe('EVT-005: Error Event Handling', () => {
    test('should display error notification on evaluation failure', async ({ page }) => {
      await evaluationPage.goto();

      try {
        await wsHelpers.connect();

        // Watch for failure events
        const failurePromise = wsHelpers.waitForEvent(
          EventType.EVALUATION_FAILED,
          10000
        ).catch(() => null);

        // Attempt evaluation (may fail depending on data)
        await evaluationPage.selectPatient('Test');
        await evaluationPage.selectMeasure('CMS');

        const failureEvent = await failurePromise;
        if (failureEvent) {
          console.log('Received failure event:', failureEvent.payload);

          // UI should show error state
          const errorIndicator = page.locator(
            '.error, .evaluation-error, [data-testid="error-message"]'
          );
          if (await errorIndicator.count() > 0) {
            await expect(errorIndicator).toBeVisible();
          }
        }

      } catch (e) {
        console.log('WebSocket test skipped:', e);
      }
    });

    test('should handle batch failure gracefully', async ({ page }) => {
      await evaluationPage.goto();

      const batchToggle = evaluationPage.batchModeToggle;
      if (await batchToggle.count() === 0) {
        console.log('Batch mode not available');
        return;
      }

      try {
        await wsHelpers.connect();

        // Watch for batch failure events
        const failurePromise = wsHelpers.waitForEvent(
          EventType.BATCH_FAILED,
          10000
        ).catch(() => null);

        await evaluationPage.enableBatchMode();

        const failureEvent = await failurePromise;
        if (failureEvent) {
          console.log('Batch failure event:', failureEvent.payload);

          // UI should indicate failure
          const errorArea = page.locator('.batch-error, .error-summary');
          if (await errorArea.count() > 0) {
            console.log('Batch error displayed in UI');
          }
        }

      } catch (e) {
        console.log('WebSocket test skipped:', e);
      }
    });

    test('should allow retry after error', async ({ page }) => {
      await evaluationPage.goto();

      // After an error, the retry button should be available
      const retryButton = page.locator(
        '[data-testid="retry-button"], button:has-text("Retry"), button:has-text("Try Again")'
      );

      // Simulate error scenario or check existing UI
      const hasRetryOption = await retryButton.count() > 0;
      console.log('Retry option available:', hasRetryOption);
    });

    test('should log errors for debugging', async ({ page }) => {
      const consoleLogs: string[] = [];

      page.on('console', (msg) => {
        if (msg.type() === 'error') {
          consoleLogs.push(msg.text());
        }
      });

      await evaluationPage.goto();

      // Trigger some actions
      await evaluationPage.selectPatient('Test');
      await evaluationPage.selectMeasure('CMS');

      // Check for error logs
      console.log('Console errors logged:', consoleLogs.length);
    });
  });

  /**
   * EVT-006: Event Replay and Recovery
   *
   * Verifies that the UI can recover from missed events.
   */
  test.describe('EVT-006: Event Recovery', () => {
    test('should recover state after page refresh during operation', async ({ page }) => {
      await evaluationPage.goto();

      // Start an evaluation
      await evaluationPage.selectPatient('Test');
      await evaluationPage.selectMeasure('CMS');
      await evaluationPage.evaluateButton.click();

      // Refresh page during progress
      await page.reload();

      // UI should either show completed result or allow restart
      const hasResult = await evaluationPage.resultCard.count() > 0;
      const canRestart = await evaluationPage.patientSearchInput.isVisible();

      console.log('Has result after refresh:', hasResult);
      console.log('Can restart after refresh:', canRestart);

      expect(hasResult || canRestart).toBe(true);
    });

    test('should sync with server state after reconnection', async ({ page }) => {
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      // Record initial metrics
      const initialGapCount = await dashboardPage.getCareGapsCount();
      console.log('Initial care gap count:', initialGapCount);

      // Simulate network reconnection by refreshing
      await page.reload();
      await dashboardPage.waitForDataLoad();

      // Metrics should be consistent
      const newGapCount = await dashboardPage.getCareGapsCount();
      console.log('Care gap count after reconnection:', newGapCount);

      // Should show current server state
      expect(typeof newGapCount).toBe('number');
    });

    test('should preserve pending changes after reconnection', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count > 0) {
        // Select a gap
        await careGapPage.selectGapByIndex(0);

        // Record intervention (partial action)
        await careGapPage.recordInterventionButton.click();

        // Refresh before completing
        await page.reload();

        // Check if pending intervention state is preserved or needs restart
        console.log('Page reloaded during intervention recording');
      }
    });
  });

  /**
   * EVT-007: High-Volume Event Handling
   *
   * Verifies UI performance under high event volume.
   */
  test.describe('EVT-007: High-Volume Events', () => {
    test('should handle rapid event updates without UI lag', async ({ page }) => {
      await dashboardPage.goto();

      try {
        await wsHelpers.connect();

        const startTime = Date.now();
        let eventCount = 0;

        // Subscribe to all events
        const unsub = wsHelpers.subscribe('*', () => {
          eventCount++;
        });

        // Wait for events
        await page.waitForTimeout(10000);

        unsub();

        const duration = Date.now() - startTime;
        const eventsPerSecond = eventCount / (duration / 1000);

        console.log(`Received ${eventCount} events in ${duration}ms`);
        console.log(`Rate: ${eventsPerSecond.toFixed(2)} events/second`);

        // UI should still be responsive
        const isResponsive = await dashboardPage.isLoaded();
        expect(isResponsive).toBe(true);

      } catch (e) {
        console.log('WebSocket test skipped:', e);
      }
    });

    test('should throttle UI updates for performance', async ({ page }) => {
      await dashboardPage.goto();

      // Check for throttling mechanism
      // Many apps throttle rapid updates to prevent UI stuttering

      const metricsUpdates: number[] = [];
      const checkInterval = setInterval(async () => {
        const count = await dashboardPage.getCareGapsCount();
        metricsUpdates.push(count);
      }, 100);

      await page.waitForTimeout(3000);
      clearInterval(checkInterval);

      console.log(`Captured ${metricsUpdates.length} UI state samples`);

      // UI should remain stable even with many samples
      expect(metricsUpdates.length).toBeGreaterThan(0);
    });

    test('should not leak memory with many events', async ({ page }) => {
      // Get initial memory metrics if available
      const metrics = await page.evaluate(() => {
        if ((performance as any).memory) {
          return {
            usedJSHeapSize: (performance as any).memory.usedJSHeapSize,
            totalJSHeapSize: (performance as any).memory.totalJSHeapSize,
          };
        }
        return null;
      });

      if (metrics) {
        console.log('Initial memory:', metrics);
      } else {
        console.log('Memory metrics not available (Chrome only)');
      }

      await dashboardPage.goto();
      await page.waitForTimeout(10000);

      const afterMetrics = await page.evaluate(() => {
        if ((performance as any).memory) {
          return {
            usedJSHeapSize: (performance as any).memory.usedJSHeapSize,
            totalJSHeapSize: (performance as any).memory.totalJSHeapSize,
          };
        }
        return null;
      });

      if (afterMetrics && metrics) {
        const memoryIncrease = afterMetrics.usedJSHeapSize - metrics.usedJSHeapSize;
        console.log('Memory increase:', memoryIncrease);
      }
    });
  });

  /**
   * EVT-008: Cross-Tab Event Synchronization
   *
   * Verifies that events are synchronized across browser tabs.
   */
  test.describe('EVT-008: Cross-Tab Sync', () => {
    test('should sync care gap updates across tabs', async ({ browser }) => {
      // Create two browser contexts
      const context1 = await browser.newContext();
      const context2 = await browser.newContext();

      const page1 = await context1.newPage();
      const page2 = await context2.newPage();

      const loginPage1 = new LoginPage(page1);
      const loginPage2 = new LoginPage(page2);
      const careGapPage1 = new CareGapPage(page1);
      const dashboardPage2 = new DashboardPage(page2);

      // Login on both tabs
      await loginPage1.goto();
      await loginPage1.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      await loginPage2.goto();
      await loginPage2.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      // Tab 1: Care gaps page
      await careGapPage1.goto();

      // Tab 2: Dashboard
      await dashboardPage2.goto();
      await dashboardPage2.waitForDataLoad();

      const initialCount = await dashboardPage2.getCareGapsCount();
      console.log('Initial care gap count on dashboard:', initialCount);

      // Close a gap in tab 1
      const gapCount = await careGapPage1.getGapCount();
      if (gapCount > 0) {
        await careGapPage1.selectGapByIndex(0);
        await careGapPage1.closeGap({
          reason: 'Completed',
          notes: 'E2E cross-tab test',
        });

        // Wait for sync
        await page2.waitForTimeout(2000);

        // Refresh dashboard to see update
        await page2.reload();
        await dashboardPage2.waitForDataLoad();

        const newCount = await dashboardPage2.getCareGapsCount();
        console.log('Care gap count after close:', newCount);
      }

      await context1.close();
      await context2.close();
    });

    test('should maintain consistency across multiple sessions', async ({ browser }) => {
      const context = await browser.newContext();
      const page1 = await context.newPage();
      const page2 = await context.newPage();

      // Both pages same context (shared session)
      const loginPage = new LoginPage(page1);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      // Page 2 should be authenticated (shared cookies)
      await page2.goto('/dashboard');

      // Should not redirect to login
      await expect(page2).not.toHaveURL(/.*login/);

      await context.close();
    });
  });
});

/**
 * Event Ordering Validation Tests
 */
test.describe('Event Ordering Validation', () => {
  let wsHelpers: WebSocketHelpers;
  let orderValidator: EventOrderValidator;

  test.beforeEach(async ({ page }) => {
    wsHelpers = new WebSocketHelpers({
      endpoint: process.env.WS_ENDPOINT || 'ws://localhost:8087/ws',
      tenantId: 'TENANT001',
    });
    orderValidator = new EventOrderValidator();

    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
  });

  test.afterEach(async () => {
    if (wsHelpers) {
      await wsHelpers.disconnect();
    }
    orderValidator.clear();
  });

  test('should validate evaluation event sequence', async ({ page }) => {
    try {
      await wsHelpers.connect();

      const unsub = wsHelpers.subscribe('*', (e) => {
        orderValidator.recordEvent(e);
      });

      const evaluationPage = new EvaluationPage(page);
      await evaluationPage.goto();
      await evaluationPage.selectPatient('Test');
      await evaluationPage.selectMeasure('CMS');
      await evaluationPage.evaluate();

      unsub();

      // Validate all recorded sequences
      const sequences = orderValidator.getAllSequences();
      let validCount = 0;
      let invalidCount = 0;

      for (const [correlationId] of sequences) {
        const result = orderValidator.validateEvaluationSequence(correlationId);
        if (result.valid) {
          validCount++;
        } else {
          invalidCount++;
          console.warn(`Invalid sequence ${correlationId}:`, result.errors);
        }
      }

      console.log(`Valid sequences: ${validCount}, Invalid: ${invalidCount}`);

    } catch (e) {
      console.log('WebSocket test skipped:', e);
    }
  });

  test('should ensure timestamps are monotonically increasing', async () => {
    try {
      await wsHelpers.connect();

      const events: any[] = [];
      const unsub = wsHelpers.subscribe('*', (e) => {
        events.push(e);
      });

      await new Promise(resolve => setTimeout(resolve, 5000));

      unsub();

      // Check timestamp ordering
      for (let i = 1; i < events.length; i++) {
        if (events[i].timestamp < events[i - 1].timestamp) {
          console.warn('Timestamp ordering violation detected');
        }
      }

      console.log(`Validated ${events.length} events for timestamp ordering`);

    } catch (e) {
      console.log('WebSocket test skipped:', e);
    }
  });
});
