/**
 * WebSocket Real-Time Integration E2E Tests
 *
 * Comprehensive end-to-end testing of WebSocket real-time communication
 * integration into the shell-app and dashboard components.
 *
 * Test Coverage:
 * - WebSocket connection establishment
 * - Real-time health score updates
 * - Real-time care gap notifications
 * - Real-time system alerts
 * - Connection status display in header
 * - Automatic reconnection behavior
 * - Message buffering during disconnection
 */

describe('WebSocket Real-Time Integration - End-to-End Tests', () => {
  const BASE_URL = 'http://localhost:4200';
  const DASHBOARD_URL = `${BASE_URL}/dashboard`;
  const SHELL_URL = BASE_URL;

  beforeEach(() => {
    // Visit shell app before each test
    cy.visit(SHELL_URL, { timeout: 10000 });
    // Wait for shell layout to load
    cy.get('health-platform-shell-layout', { timeout: 5000 }).should('exist');
  });

  describe('Shell App Layout - WebSocket Integration', () => {
    it('should render shell layout with header, main, and footer', () => {
      cy.get('.shell-layout').should('exist');
      cy.get('.shell-header').should('exist');
      cy.get('.shell-main').should('exist');
      cy.get('.shell-footer').should('exist');
    });

    it('should display Health Data Platform title', () => {
      cy.get('.shell-title').should('contain', 'Health Data Platform');
    });

    it('should have navigation links', () => {
      cy.get('.shell-nav a').should('have.length.at.least', 2);
      cy.get('.shell-nav').should('contain', 'Home');
      cy.get('.shell-nav').should('contain', 'Patients');
    });
  });

  describe('WebSocket Connection Status - Header Display', () => {
    it('should display connection status indicator in header', () => {
      // RED: This element should exist when WebSocket integration is complete
      cy.get('.connection-status-indicator', { timeout: 5000 }).should('exist');
    });

    it('should show "Connected" status when WebSocket is connected', () => {
      cy.get('.connection-status-indicator')
        .should('exist')
        .and('have.class', 'status-connected');
      cy.get('.connection-status-text').should('contain', 'Connected');
    });

    it('should show connection status badge with visual indicator', () => {
      cy.get('.connection-status-badge')
        .should('exist')
        .and('have.class', 'badge-success');
    });

    it('should display reconnecting status when connection is lost', () => {
      // RED: This tests auto-reconnection behavior
      // Simulate network disconnection
      cy.window().then((win) => {
        // Access WebSocket service and simulate disconnection
        const service = win['websocketService'];
        if (service) {
          service.disconnect();
        }
      });

      // Status should change to "Reconnecting"
      cy.get('.connection-status-text', { timeout: 10000 })
        .should('contain', 'Reconnecting');
      cy.get('.connection-status-badge').should('have.class', 'badge-warning');
    });
  });

  describe('WebSocket Connection Lifecycle', () => {
    it('should establish WebSocket connection on app initialization', () => {
      cy.window().then((win) => {
        // Verify that WebSocket service is available
        expect(win['websocketService']).to.exist;
      });
    });

    it('should maintain connection state across navigation', () => {
      // Navigate to home
      cy.get('[routerLink="/"]').click();
      cy.get('.shell-main').should('contain', 'Home');

      // Connection status should still be connected
      cy.get('.connection-status-text').should('contain', 'Connected');
    });

    it('should queue messages during temporary disconnection', () => {
      // RED: Message queuing behavior test
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service && service.messageQueue) {
          expect(service.messageQueue.isEmpty()).to.be.true;
        }
      });
    });
  });

  describe('Real-Time Health Score Updates', () => {
    it('should receive health score update messages from WebSocket', () => {
      // RED: This tests receiving real-time health score updates
      cy.get('.health-score-display', { timeout: 10000 }).should('exist');
    });

    it('should display current patient health score', () => {
      cy.get('.health-score-value')
        .should('exist')
        .and('contain.text', /\d+/); // Should contain a number
    });

    it('should update health score in real-time when new message received', () => {
      // Get initial health score
      cy.get('.health-score-value').then(($el) => {
        const initialScore = $el.text();

        // Simulate health score update via WebSocket
        cy.window().then((win) => {
          const service = win['websocketService'];
          if (service) {
            const updateMessage = {
              type: 'HEALTH_SCORE_UPDATE',
              data: {
                patientId: 'PATIENT123',
                score: parseInt(initialScore) + 5,
                category: 'good',
                factors: ['adequate-bp-control', 'medication-adherence'],
                calculatedAt: Date.now(),
              },
            };
            service.simulateMessage(updateMessage);
          }
        });

        // Verify health score updated
        cy.get('.health-score-value')
          .should('contain.text', String(parseInt(initialScore) + 5));
      });
    });

    it('should display health score category badge', () => {
      cy.get('.health-score-badge')
        .should('exist')
        .and('match', /good|fair|poor/);
    });

    it('should highlight health score when changed significantly', () => {
      // Simulate health score change
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          const changeMessage = {
            type: 'HEALTH_SCORE_CHANGE',
            data: {
              patientId: 'PATIENT123',
              priority: 'high',
              previousScore: 70,
              newScore: 85,
              changePercent: 21.4,
              changedAt: Date.now(),
            },
          };
          service.simulateMessage(changeMessage);
        }
      });

      // Should show change notification
      cy.get('.health-score-change-alert', { timeout: 5000 })
        .should('exist')
        .and('have.class', 'alert-success');
    });
  });

  describe('Real-Time Care Gap Notifications', () => {
    it('should receive care gap notification messages from WebSocket', () => {
      // RED: This tests receiving real-time care gap notifications
      cy.get('.care-gap-notification', { timeout: 10000 }).should('exist');
    });

    it('should display care gap urgency indicator', () => {
      cy.get('.care-gap-urgency-badge')
        .should('exist')
        .and('match', /routine|soon|overdue|critical/);
    });

    it('should show recommended action for care gap', () => {
      cy.get('.care-gap-action')
        .should('exist')
        .and('contain.text', /medication|education|referral|screening/);
    });

    it('should update care gap status when WebSocket message received', () => {
      // Simulate care gap notification
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          const gapMessage = {
            type: 'CARE_GAP_NOTIFICATION',
            data: {
              gapId: 'GAP123',
              patientId: 'PATIENT123',
              measureId: 'HEDIS-BCS',
              urgency: 'critical',
              recommendedAction: 'Schedule screening mammography',
              assignedTo: 'RN-001',
              notifiedAt: Date.now(),
            },
          };
          service.simulateMessage(gapMessage);
        }
      });

      // Should show critical urgency
      cy.get('.care-gap-urgency-badge')
        .should('have.class', 'urgency-critical');
    });
  });

  describe('Real-Time System Alerts', () => {
    it('should receive system alert messages from WebSocket', () => {
      // RED: This tests receiving real-time system alerts
      cy.get('.system-alert-container', { timeout: 10000 }).should('exist');
    });

    it('should display alert with appropriate severity level', () => {
      // Simulate system alert
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          const alertMessage = {
            type: 'SYSTEM_ALERT_MESSAGE',
            data: {
              alertId: 'ALERT123',
              alertType: 'processing_delay',
              severity: 'warning',
              message: 'Quality measure evaluation delayed by 2 minutes',
              timestamp: Date.now(),
            },
          };
          service.simulateMessage(alertMessage);
        }
      });

      // Should show alert
      cy.get('.system-alert')
        .should('exist')
        .and('have.class', 'severity-warning');
    });

    it('should display alert message content', () => {
      cy.get('.alert-message').should('exist');
    });
  });

  describe('Automatic Reconnection with Exponential Backoff', () => {
    it('should attempt reconnection after connection loss', () => {
      // RED: This tests exponential backoff retry behavior
      cy.window().then((win) => {
        const service = win['websocketService'];
        expect(service.config.maxReconnectAttempts).to.equal(10);
      });
    });

    it('should use exponential backoff delays (1s, 2s, 4s, 8s...)', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        expect(service.config.reconnectInterval).to.equal(1000); // Initial 1s
      });
    });

    it('should cap reconnection delay at 30 seconds max', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        expect(service.config.maxReconnectDelay).to.be.lessThanOrEqual(30000);
      });
    });
  });

  describe('Multi-Tenant Isolation', () => {
    it('should filter messages by tenant ID', () => {
      // RED: This tests multi-tenant message filtering
      cy.window().then((win) => {
        const service = win['websocketService'];
        // Should have forTenant method for filtering
        expect(service.forTenant).to.exist;
      });
    });

    it('should only show messages for current tenant', () => {
      // Current tenant messages should be visible
      cy.get('.health-score-display').should('exist');

      // Other tenant messages should be filtered out via forTenant()
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service && service.forTenant) {
          const tenantMessages = service.forTenant('tenant-123');
          expect(tenantMessages).to.exist;
        }
      });
    });
  });

  describe('WebSocket Performance & Latency', () => {
    it('should receive health score updates in under 100ms (p95)', () => {
      const startTime = performance.now();
      let updateReceivedTime = 0;

      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          // Subscribe to health score updates
          const subscription = service
            .ofType('HEALTH_SCORE_UPDATE')
            .subscribe(() => {
              updateReceivedTime = performance.now();
            });

          // Simulate message
          setTimeout(() => {
            const updateMessage = {
              type: 'HEALTH_SCORE_UPDATE',
              data: {
                patientId: 'PATIENT123',
                score: 85,
                category: 'good',
                factors: [],
                calculatedAt: Date.now(),
              },
            };
            service.simulateMessage(updateMessage);
          }, 0);
        }
      });

      // Verify latency is under 100ms
      cy.window().then(() => {
        const latency = updateReceivedTime - startTime;
        expect(latency).to.be.lessThan(100);
      });
    });
  });

  describe('Connection State Persistence', () => {
    it('should persist connection state across page refresh', () => {
      // Get initial connection status
      cy.get('.connection-status-text').should('contain', 'Connected');

      // Refresh page
      cy.reload();

      // Connection should be reestablished
      cy.get('.connection-status-text', { timeout: 10000 }).should(
        'contain',
        'Connected'
      );
    });

    it('should restore session after refresh', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service && service.sessionId) {
          const sessionId = service.sessionId;

          // Refresh page
          cy.reload();

          // Session should be maintained
          cy.window().then((newWin) => {
            const newService = newWin['websocketService'];
            expect(newService.sessionId).to.equal(sessionId);
          });
        }
      });
    });
  });

  describe('Error Handling & Resilience', () => {
    it('should handle invalid messages gracefully', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          // Send invalid message
          const invalidMessage = {
            type: 'INVALID_TYPE',
            data: {},
          };
          service.simulateMessage(invalidMessage);
        }
      });

      // App should remain stable
      cy.get('.shell-layout').should('exist');
      cy.get('.connection-status-text').should('contain', 'Connected');
    });

    it('should log errors without crashing', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          // Simulate error
          service.simulateError('Connection reset by peer');
        }
      });

      // App should recover
      cy.get('.shell-layout').should('exist');
    });
  });
});
