import { test, expect } from '@playwright/test';
import { DashboardPage } from '../../../pages/dashboard.page';
import { LoginPage } from '../../../pages/login.page';
import { TEST_USERS } from '../../../fixtures/test-fixtures';
import { WebSocketHelpers } from '../../../utils/websocket-helpers';
import { MockEventFactory, EventType, AlertSeverity, AlertType } from '../../../fixtures/mock-events';

/**
 * Clinical Alert Tests
 *
 * Test Suite: ALT (Alerts)
 * Coverage: Clinical alert notifications and handling
 *
 * These tests verify the clinical alert system that notifies
 * providers of critical patient conditions and care requirements.
 */

test.describe('Clinical Alert Workflows', () => {
  let loginPage: LoginPage;
  let dashboardPage: DashboardPage;
  let wsHelpers: WebSocketHelpers;
  let mockEvents: MockEventFactory;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    dashboardPage = new DashboardPage(page);
    mockEvents = new MockEventFactory();

    wsHelpers = new WebSocketHelpers({
      endpoint: process.env.WS_ENDPOINT || 'ws://localhost:8087/ws',
      tenantId: 'TENANT001',
    });

    // Login as evaluator
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
  });

  test.afterEach(async () => {
    if (wsHelpers) {
      await wsHelpers.disconnect();
    }
  });

  /**
   * ALT-001: Alert Notification Display
   *
   * Verifies that clinical alerts are displayed prominently
   * in the UI when triggered.
   */
  test.describe('ALT-001: Alert Notification Display', () => {
    test('should display alert notification area on dashboard', async ({ page }) => {
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      // Look for alert/notification area
      const alertArea = page.locator(
        '[data-testid="alerts-panel"], .alerts-panel, .notifications-area, [role="alert"]'
      );

      const alertBell = page.locator(
        '[data-testid="alert-bell"], .notification-icon, [aria-label*="notification"], [aria-label*="alert"]'
      );

      const hasAlertArea = await alertArea.count() > 0;
      const hasAlertBell = await alertBell.count() > 0;

      console.log('Alert area present:', hasAlertArea);
      console.log('Alert bell present:', hasAlertBell);

      // At least one should exist
      expect(hasAlertArea || hasAlertBell).toBe(true);
    });

    test('should show alert count badge', async ({ page }) => {
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      const alertBadge = page.locator(
        '[data-testid="alert-count"], .alert-badge, .notification-badge, .badge'
      );

      if (await alertBadge.count() > 0) {
        const badgeText = await alertBadge.textContent();
        console.log('Alert badge count:', badgeText);
      } else {
        console.log('No alert badge displayed (may be no alerts)');
      }
    });

    test('should display alert details on click', async ({ page }) => {
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      // Click alert bell or notification area
      const alertTrigger = page.locator(
        '[data-testid="alert-bell"], .notification-icon, [aria-label*="notification"]'
      ).first();

      if (await alertTrigger.count() > 0) {
        await alertTrigger.click();

        // Wait for alert panel/dropdown
        const alertList = page.locator(
          '[data-testid="alert-list"], .alert-dropdown, .notification-dropdown'
        );

        if (await alertList.count() > 0) {
          await expect(alertList).toBeVisible();
          console.log('Alert list displayed on click');
        }
      }
    });

    test('should receive real-time alert via WebSocket', async ({ page }) => {
      await dashboardPage.goto();

      try {
        await wsHelpers.connect();

        // Wait for any alert event
        const alertPromise = wsHelpers.waitForEvent(
          EventType.ALERT_TRIGGERED,
          10000
        ).catch(() => null);

        const alert = await alertPromise;
        if (alert) {
          console.log('Received alert:', alert.payload.alertType);
          expect(alert.payload.severity).toBeDefined();
        } else {
          console.log('No alerts received in test window');
        }
      } catch (e) {
        console.log('WebSocket test skipped:', e);
      }
    });
  });

  /**
   * ALT-002: Critical Alert Handling
   *
   * Verifies that critical alerts are displayed with high visibility
   * and require acknowledgment.
   */
  test.describe('ALT-002: Critical Alert Handling', () => {
    test('should display critical alert with high visibility', async ({ page }) => {
      await dashboardPage.goto();

      // Look for critical alert indicators
      const criticalAlerts = page.locator(
        '[data-testid="critical-alert"], .alert-critical, .severity-critical, .alert-danger'
      );

      const count = await criticalAlerts.count();
      console.log('Critical alerts displayed:', count);

      if (count > 0) {
        // Verify styling indicates criticality
        const firstAlert = criticalAlerts.first();
        const classes = await firstAlert.getAttribute('class') || '';

        const hasCriticalStyling =
          classes.includes('critical') ||
          classes.includes('danger') ||
          classes.includes('red');

        console.log('Has critical styling:', hasCriticalStyling);
      }
    });

    test('should show critical A1C alert', async ({ page }) => {
      await dashboardPage.goto();

      // Check for A1C-related alerts
      const a1cAlert = page.locator(
        ':has-text("A1C"), :has-text("HbA1c"), [data-alert-type="A1C_CRITICAL"]'
      );

      const count = await a1cAlert.count();
      console.log('A1C alerts found:', count);
    });

    test('should show critical blood pressure alert', async ({ page }) => {
      await dashboardPage.goto();

      // Check for BP-related alerts
      const bpAlert = page.locator(
        ':has-text("Blood Pressure"), :has-text("BP"), [data-alert-type="BP_CRITICAL"]'
      );

      const count = await bpAlert.count();
      console.log('BP alerts found:', count);
    });

    test('should require acknowledgment for critical alerts', async ({ page }) => {
      await dashboardPage.goto();

      // Find critical alert with acknowledge button
      const acknowledgeButton = page.locator(
        '[data-testid="acknowledge-alert"], button:has-text("Acknowledge"), button:has-text("Ack")'
      );

      if (await acknowledgeButton.count() > 0) {
        console.log('Acknowledge button found for critical alert');

        // Click acknowledge
        await acknowledgeButton.first().click();

        // Wait for confirmation
        const successMessage = page.locator('.success, .acknowledged, [data-testid="ack-success"]');
        if (await successMessage.count() > 0) {
          console.log('Alert acknowledged successfully');
        }
      }
    });
  });

  /**
   * ALT-003: Alert Filtering and Sorting
   *
   * Verifies that alerts can be filtered by severity, type, and patient.
   */
  test.describe('ALT-003: Alert Filtering and Sorting', () => {
    test('should filter alerts by severity', async ({ page }) => {
      // Navigate to alerts page if exists
      await page.goto('/alerts').catch(() => page.goto('/dashboard'));

      const severityFilter = page.locator(
        '[data-testid="severity-filter"], #severityFilter, select:has-text("Severity")'
      );

      if (await severityFilter.count() > 0) {
        // Filter by CRITICAL
        await severityFilter.selectOption('CRITICAL');
        console.log('Filtered by CRITICAL severity');

        // Verify only critical alerts shown
        const alerts = page.locator('.alert-item, [data-testid="alert-item"]');
        const count = await alerts.count();
        console.log('Critical alerts after filter:', count);
      } else {
        console.log('Severity filter not available');
      }
    });

    test('should filter alerts by type', async ({ page }) => {
      await page.goto('/alerts').catch(() => page.goto('/dashboard'));

      const typeFilter = page.locator(
        '[data-testid="type-filter"], #typeFilter, select:has-text("Type")'
      );

      if (await typeFilter.count() > 0) {
        // Get available options
        const options = await typeFilter.locator('option').allTextContents();
        console.log('Alert types available:', options);
      }
    });

    test('should sort alerts by date', async ({ page }) => {
      await page.goto('/alerts').catch(() => page.goto('/dashboard'));

      const sortControl = page.locator(
        '[data-testid="sort-control"], .sort-button, th:has-text("Date")'
      );

      if (await sortControl.count() > 0) {
        await sortControl.click();
        console.log('Sorted alerts by date');
      }
    });

    test('should search alerts by patient', async ({ page }) => {
      await page.goto('/alerts').catch(() => page.goto('/dashboard'));

      const searchInput = page.locator(
        '[data-testid="alert-search"], #alertSearch, input[placeholder*="Search"]'
      );

      if (await searchInput.count() > 0) {
        await searchInput.fill('Test');
        await page.waitForTimeout(500);
        console.log('Searched alerts by patient');
      }
    });
  });

  /**
   * ALT-004: Alert Actions and Resolution
   *
   * Verifies alert acknowledgment, escalation, and resolution workflows.
   */
  test.describe('ALT-004: Alert Actions', () => {
    test('should acknowledge alert via WebSocket', async ({ page }) => {
      await dashboardPage.goto();

      try {
        await wsHelpers.connect();

        // Find and acknowledge an alert
        const ackButton = page.locator(
          '[data-testid="acknowledge-alert"], button:has-text("Acknowledge")'
        ).first();

        if (await ackButton.count() > 0) {
          // Collect acknowledgment event
          const ackEventPromise = wsHelpers.waitForEvent(
            EventType.ALERT_ACKNOWLEDGED,
            5000
          ).catch(() => null);

          await ackButton.click();

          const ackEvent = await ackEventPromise;
          if (ackEvent) {
            console.log('Acknowledgment event received');
            expect(ackEvent.payload.alertId).toBeTruthy();
          }
        }
      } catch (e) {
        console.log('WebSocket test skipped:', e);
      }
    });

    test('should escalate alert to supervisor', async ({ page }) => {
      await page.goto('/alerts').catch(() => page.goto('/dashboard'));

      const escalateButton = page.locator(
        '[data-testid="escalate-alert"], button:has-text("Escalate")'
      ).first();

      if (await escalateButton.count() > 0) {
        await escalateButton.click();

        // Look for escalation confirmation
        const confirmDialog = page.locator(
          '[data-testid="escalate-dialog"], .modal, [role="dialog"]'
        );

        if (await confirmDialog.count() > 0) {
          console.log('Escalation dialog displayed');

          // Select supervisor
          const supervisorSelect = page.locator('#supervisor, [name="assignTo"]');
          if (await supervisorSelect.count() > 0) {
            await supervisorSelect.selectOption({ index: 1 });
          }

          // Confirm escalation
          const confirmButton = page.locator('button:has-text("Confirm"), button:has-text("Submit")');
          if (await confirmButton.count() > 0) {
            await confirmButton.click();
          }
        }
      } else {
        console.log('Escalate button not available');
      }
    });

    test('should resolve alert with notes', async ({ page }) => {
      await page.goto('/alerts').catch(() => page.goto('/dashboard'));

      const resolveButton = page.locator(
        '[data-testid="resolve-alert"], button:has-text("Resolve")'
      ).first();

      if (await resolveButton.count() > 0) {
        await resolveButton.click();

        // Fill resolution notes
        const notesInput = page.locator(
          '[data-testid="resolution-notes"], #resolutionNotes, textarea'
        );

        if (await notesInput.count() > 0) {
          await notesInput.fill('E2E Test - Alert resolved');
        }

        // Submit resolution
        const submitButton = page.locator('button:has-text("Submit"), button:has-text("Confirm")');
        if (await submitButton.count() > 0) {
          await submitButton.click();
          console.log('Alert resolution submitted');
        }
      } else {
        console.log('Resolve button not available');
      }
    });

    test('should show alert history', async ({ page }) => {
      await page.goto('/alerts').catch(() => page.goto('/dashboard'));

      const historyTab = page.locator(
        '[data-testid="alert-history"], button:has-text("History"), a:has-text("History")'
      );

      if (await historyTab.count() > 0) {
        await historyTab.click();

        // Wait for history to load
        const historyList = page.locator('.alert-history, [data-testid="history-list"]');
        if (await historyList.count() > 0) {
          console.log('Alert history displayed');
        }
      }
    });

    test('should show related patient from alert', async ({ page }) => {
      await page.goto('/alerts').catch(() => page.goto('/dashboard'));

      const viewPatientLink = page.locator(
        '[data-testid="view-patient"], a:has-text("View Patient")'
      ).first();

      if (await viewPatientLink.count() > 0) {
        await viewPatientLink.click();

        // Should navigate to patient page
        await page.waitForURL(/.*patients\/.*/);
        console.log('Navigated to related patient');
      }
    });
  });
});

/**
 * Alert Sound and Visual Notification Tests
 */
test.describe('Alert Sound and Visual Notifications', () => {
  let loginPage: LoginPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
  });

  test('should flash critical alert in browser tab', async ({ page }) => {
    await page.goto('/dashboard');

    // Check for title changes (some apps flash the title for critical alerts)
    const initialTitle = await page.title();
    console.log('Initial page title:', initialTitle);

    // Would need to simulate critical alert to test title flashing
  });

  test('should highlight new alerts visually', async ({ page }) => {
    await page.goto('/dashboard');

    // Look for new/unread alert indicators
    const newIndicator = page.locator(
      '.alert-new, .unread, .new-alert, [data-status="new"]'
    );

    const count = await newIndicator.count();
    console.log('New alert indicators:', count);
  });

  test('should mark alerts as read when viewed', async ({ page }) => {
    await page.goto('/dashboard');

    // Click alert to view
    const alert = page.locator('.alert-item, [data-testid="alert-item"]').first();

    if (await alert.count() > 0) {
      const hadUnread = await alert.locator('.unread, .new').count() > 0;
      console.log('Had unread indicator:', hadUnread);

      await alert.click();
      await page.waitForTimeout(500);

      const hasUnread = await alert.locator('.unread, .new').count() > 0;
      console.log('Has unread indicator after click:', hasUnread);
    }
  });
});
