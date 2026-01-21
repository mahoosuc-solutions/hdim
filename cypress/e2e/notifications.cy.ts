/**
 * Phase 5C: Notifications Library E2E Tests
 *
 * Tests for real-time notification system with toast/alert components.
 * Covers notification display, types, user interactions, and cleanup.
 *
 * ★ Insight ─────────────────────────────────────
 * This E2E test suite validates the notification UX flow:
 * - Toast notifications auto-dismiss after timeout
 * - Alert notifications require user dismissal
 * - Notifications stack without overlapping
 * - WebSocket alerts trigger appropriate notifications
 * - Notification history tracks recent messages
 * ─────────────────────────────────────────────────
 */

describe('Notifications Library - E2E Tests', () => {
  beforeEach(() => {
    cy.visit('/');
    cy.wait(1000); // Wait for app initialization
  });

  describe('Toast Notifications', () => {
    it('should display success toast with message', () => {
      // Trigger a successful action
      cy.get('[data-testid="test-action-success"]').click();

      // Verify toast appears
      cy.get('[data-testid="toast-container"]').should('exist');
      cy.get('[data-testid="toast-message"]').should('contain', 'Success');
      cy.get('[data-testid="toast-icon"]').should('have.class', 'success-icon');
    });

    it('should display error toast with message', () => {
      cy.get('[data-testid="test-action-error"]').click();

      cy.get('[data-testid="toast-container"]').should('exist');
      cy.get('[data-testid="toast-message"]').should('contain', 'Error');
      cy.get('[data-testid="toast-icon"]').should('have.class', 'error-icon');
    });

    it('should display warning toast with message', () => {
      cy.get('[data-testid="test-action-warning"]').click();

      cy.get('[data-testid="toast-container"]').should('exist');
      cy.get('[data-testid="toast-message"]').should('contain', 'Warning');
      cy.get('[data-testid="toast-icon"]').should('have.class', 'warning-icon');
    });

    it('should display info toast with message', () => {
      cy.get('[data-testid="test-action-info"]').click();

      cy.get('[data-testid="toast-container"]').should('exist');
      cy.get('[data-testid="toast-message"]').should('contain', 'Info');
      cy.get('[data-testid="toast-icon"]').should('have.class', 'info-icon');
    });

    it('should auto-dismiss success toast after 3 seconds', () => {
      cy.get('[data-testid="test-action-success"]').click();
      cy.get('[data-testid="toast-container"]').should('be.visible');

      // Wait for auto-dismiss
      cy.wait(3500);
      cy.get('[data-testid="toast-container"]').should('not.exist');
    });

    it('should auto-dismiss error toast after 5 seconds', () => {
      cy.get('[data-testid="test-action-error"]').click();
      cy.get('[data-testid="toast-container"]').should('be.visible');

      // Wait for auto-dismiss (longer for errors)
      cy.wait(5500);
      cy.get('[data-testid="toast-container"]').should('not.exist');
    });

    it('should dismiss toast on close button click', () => {
      cy.get('[data-testid="test-action-success"]').click();
      cy.get('[data-testid="toast-container"]').should('be.visible');

      cy.get('[data-testid="toast-close-button"]').click();
      cy.get('[data-testid="toast-container"]').should('not.exist');
    });

    it('should show progress bar for auto-dismiss countdown', () => {
      cy.get('[data-testid="test-action-success"]').click();
      cy.get('[data-testid="toast-progress-bar"]').should('exist');

      // Progress bar should animate
      cy.get('[data-testid="toast-progress-bar"]').should('have.css', 'animation');
    });

    it('should stack multiple toasts vertically', () => {
      cy.get('[data-testid="test-action-success"]').click();
      cy.get('[data-testid="test-action-info"]').click();
      cy.get('[data-testid="test-action-warning"]').click();

      // All toasts should be visible
      cy.get('[data-testid="toast-container"]').should('have.length', 3);

      // Verify stacking (bottom positioning increases)
      cy.get('[data-testid="toast-container"]')
        .then(($toasts) => {
          const positions = Array.from($toasts).map(el =>
            window.getComputedStyle(el).bottom
          );
          // Verify positions increase (stacking)
          expect(positions[0]).toBeLessThan(positions[1]);
        });
    });

    it('should pause auto-dismiss on hover', () => {
      cy.get('[data-testid="test-action-success"]').click();
      cy.get('[data-testid="toast-container"]').should('be.visible');

      // Hover over toast
      cy.get('[data-testid="toast-container"]').trigger('mouseenter');

      // Wait 3 seconds (should not dismiss while hovered)
      cy.wait(3500);
      cy.get('[data-testid="toast-container"]').should('be.visible');

      // Leave hover
      cy.get('[data-testid="toast-container"]').trigger('mouseleave');

      // Should dismiss after remaining time
      cy.wait(1000);
      cy.get('[data-testid="toast-container"]').should('not.exist');
    });

    it('should support action button in toast', () => {
      cy.get('[data-testid="test-action-with-button"]').click();

      cy.get('[data-testid="toast-action-button"]').should('exist');
      cy.get('[data-testid="toast-action-button"]').click();

      // Action should trigger
      cy.get('[data-testid="action-executed"]').should('have.text', 'true');
    });
  });

  describe('Alert Notifications', () => {
    it('should display alert with title and message', () => {
      cy.get('[data-testid="test-alert-info"]').click();

      cy.get('[data-testid="alert-container"]').should('exist');
      cy.get('[data-testid="alert-title"]').should('contain', 'Information');
      cy.get('[data-testid="alert-message"]').should('contain', 'Alert message');
    });

    it('should show close button on alert', () => {
      cy.get('[data-testid="test-alert-warning"]').click();

      cy.get('[data-testid="alert-close-button"]').should('exist');
    });

    it('should dismiss alert on close button click', () => {
      cy.get('[data-testid="test-alert-info"]').click();
      cy.get('[data-testid="alert-container"]').should('be.visible');

      cy.get('[data-testid="alert-close-button"]').click();
      cy.get('[data-testid="alert-container"]').should('not.exist');
    });

    it('should NOT auto-dismiss alert', () => {
      cy.get('[data-testid="test-alert-info"]').click();
      cy.get('[data-testid="alert-container"]').should('be.visible');

      // Wait 10 seconds
      cy.wait(10000);

      // Alert should still be visible
      cy.get('[data-testid="alert-container"]').should('be.visible');
    });

    it('should display alert with action buttons', () => {
      cy.get('[data-testid="test-alert-confirm"]').click();

      cy.get('[data-testid="alert-confirm-button"]').should('exist');
      cy.get('[data-testid="alert-cancel-button"]').should('exist');
    });

    it('should trigger callback on confirm button', () => {
      cy.get('[data-testid="test-alert-confirm"]').click();
      cy.get('[data-testid="alert-confirm-button"]').click();

      cy.get('[data-testid="confirmation-result"]').should('have.text', 'confirmed');
    });

    it('should trigger callback on cancel button', () => {
      cy.get('[data-testid="test-alert-confirm"]').click();
      cy.get('[data-testid="alert-cancel-button"]').click();

      cy.get('[data-testid="confirmation-result"]').should('have.text', 'cancelled');
    });

    it('should display styled alerts for different types', () => {
      cy.get('[data-testid="test-alert-success"]').click();
      cy.get('[data-testid="alert-container"]').should('have.class', 'alert-success');

      cy.get('[data-testid="alert-close-button"]').click();

      cy.get('[data-testid="test-alert-error"]').click();
      cy.get('[data-testid="alert-container"]').should('have.class', 'alert-error');
    });
  });

  describe('WebSocket Alert Integration', () => {
    it('should display toast for SYSTEM_ALERT_MESSAGE', () => {
      // Simulate WebSocket alert message
      cy.window().then((win) => {
        win.__emitWebSocketMessage({
          type: 'SYSTEM_ALERT_MESSAGE',
          data: {
            severity: 'warning',
            message: 'Patient data updated',
            timestamp: Date.now(),
          },
        });
      });

      cy.get('[data-testid="toast-container"]').should('exist');
      cy.get('[data-testid="toast-message"]').should('contain', 'Patient data updated');
    });

    it('should display critical alert for CRITICAL_ALERT', () => {
      cy.window().then((win) => {
        win.__emitWebSocketMessage({
          type: 'CRITICAL_ALERT',
          data: {
            message: 'Critical system alert',
            action_required: true,
          },
        });
      });

      cy.get('[data-testid="alert-container"]').should('exist');
      cy.get('[data-testid="alert-message"]').should('contain', 'Critical system alert');
    });

    it('should queue notifications if connection is lost', () => {
      // Trigger notifications while disconnected
      cy.window().then((win) => {
        win.__setWebSocketConnected(false);
        win.__emitWebSocketMessage({
          type: 'SYSTEM_ALERT_MESSAGE',
          data: { message: 'Alert 1' },
        });
      });

      // Reconnect
      cy.window().then((win) => {
        win.__setWebSocketConnected(true);
      });

      // Queued notifications should display
      cy.get('[data-testid="toast-container"]').should('exist');
    });
  });

  describe('Notification Preferences', () => {
    it('should respect notification type preferences', () => {
      // Disable success notifications
      cy.get('[data-testid="notification-preferences"]').click();
      cy.get('[data-testid="pref-success-toggle"]').click();

      cy.get('[data-testid="test-action-success"]').click();

      // Success toast should not appear
      cy.get('[data-testid="toast-container"]').should('not.exist');
    });

    it('should show sound preference toggle', () => {
      cy.get('[data-testid="notification-preferences"]').click();
      cy.get('[data-testid="pref-sound-toggle"]').should('exist');
    });

    it('should show notification center icon with count', () => {
      cy.get('[data-testid="test-action-success"]').click();
      cy.get('[data-testid="test-action-info"]').click();

      // Notification center should show count
      cy.get('[data-testid="notification-center-badge"]').should('contain', '2');
    });
  });

  describe('Notification Center', () => {
    it('should display notification history in center', () => {
      cy.get('[data-testid="test-action-success"]').click();
      cy.get('[data-testid="test-action-info"]').click();

      // Open notification center
      cy.get('[data-testid="notification-center-button"]').click();

      // History should show both notifications
      cy.get('[data-testid="notification-history-item"]').should('have.length', 2);
    });

    it('should show timestamp for each notification', () => {
      cy.get('[data-testid="test-action-success"]').click();
      cy.get('[data-testid="notification-center-button"]').click();

      cy.get('[data-testid="notification-history-item"]').first()
        .find('[data-testid="notification-timestamp"]')
        .should('exist');
    });

    it('should clear notification history', () => {
      cy.get('[data-testid="test-action-success"]').click();
      cy.get('[data-testid="test-action-info"]').click();

      cy.get('[data-testid="notification-center-button"]').click();
      cy.get('[data-testid="clear-history-button"]').click();

      // History should be empty
      cy.get('[data-testid="notification-history-item"]').should('not.exist');
    });

    it('should limit history to last 50 notifications', () => {
      // Trigger 60 notifications
      for (let i = 0; i < 60; i++) {
        cy.get('[data-testid="test-action-success"]').click();
      }

      cy.get('[data-testid="notification-center-button"]').click();
      cy.get('[data-testid="notification-history-item"]').should('have.length', 50);
    });
  });

  describe('Accessibility', () => {
    it('should announce toast to screen readers', () => {
      cy.get('[data-testid="test-action-success"]').click();

      // Check for aria-live region
      cy.get('[data-testid="toast-container"]')
        .should('have.attr', 'role', 'status')
        .should('have.attr', 'aria-live', 'polite');
    });

    it('should announce critical alerts as assertive', () => {
      cy.get('[data-testid="test-alert-critical"]').click();

      cy.get('[data-testid="alert-container"]')
        .should('have.attr', 'role', 'alert')
        .should('have.attr', 'aria-live', 'assertive');
    });

    it('should have keyboard accessible close button', () => {
      cy.get('[data-testid="test-action-success"]').click();

      // Tab to close button
      cy.get('[data-testid="toast-close-button"]')
        .should('have.attr', 'tabindex')
        .and('not.equal', '-1');

      // Trigger with Enter
      cy.get('[data-testid="toast-close-button"]').focus().type('{enter}');
      cy.get('[data-testid="toast-container"]').should('not.exist');
    });

    it('should support keyboard navigation in notification center', () => {
      cy.get('[data-testid="notification-center-button"]').click();

      // Tab through items
      cy.get('[data-testid="notification-history-item"]')
        .first()
        .should('have.attr', 'tabindex')
        .and('not.equal', '-1');
    });
  });

  describe('Performance', () => {
    it('should handle rapid notification creation', () => {
      // Create 10 notifications rapidly
      for (let i = 0; i < 10; i++) {
        cy.get('[data-testid="test-action-success"]').click();
      }

      // All should be visible and stacked
      cy.get('[data-testid="toast-container"]').should('have.length', 10);

      // Should not cause layout thrashing (check render time)
      cy.get('[data-testid="performance-metric"]')
        .invoke('text')
        .then((text) => {
          const renderTime = parseInt(text);
          expect(renderTime).to.be.lessThan(100); // < 100ms
        });
    });

    it('should cleanup DOM when notifications are dismissed', () => {
      cy.get('[data-testid="test-action-success"]').click();
      cy.get('[data-testid="toast-container"]').should('have.length', 1);

      cy.get('[data-testid="toast-close-button"]').click();
      cy.get('[data-testid="toast-container"]').should('have.length', 0);

      // Verify no memory leaks (check DOM node count)
      cy.window().then((win) => {
        const nodeCount = win.document.querySelectorAll('*').length;
        cy.get('[data-testid="initial-node-count"]')
          .invoke('text')
          .then((initialCount) => {
            // Should not exceed initial by more than 5 nodes (for transient elements)
            expect(nodeCount).to.be.lessThan(parseInt(initialCount) + 5);
          });
      });
    });

    it('should not block main thread during notification display', () => {
      const startTime = performance.now();

      cy.get('[data-testid="test-action-success"]').click();

      // Perform synchronous operation
      cy.window().then((win) => {
        let sum = 0;
        for (let i = 0; i < 1000000; i++) {
          sum += i;
        }

        const endTime = performance.now();
        const duration = endTime - startTime;

        // Should complete in < 500ms (notification display shouldn't block)
        expect(duration).to.be.lessThan(500);
      });
    });
  });

  describe('Error Recovery', () => {
    it('should recover from notification service error', () => {
      cy.window().then((win) => {
        win.__throwNotificationServiceError = true;
      });

      cy.get('[data-testid="test-action-success"]').click();

      // Should still display even if service throws
      cy.get('[data-testid="toast-container"]').should('exist');

      cy.window().then((win) => {
        win.__throwNotificationServiceError = false;
      });
    });

    it('should handle missing notification data gracefully', () => {
      cy.window().then((win) => {
        win.__emitWebSocketMessage({
          type: 'SYSTEM_ALERT_MESSAGE',
          // Missing data field
        });
      });

      // Should not crash, should show generic message
      cy.get('[data-testid="toast-container"]').should('exist');
    });
  });
});
