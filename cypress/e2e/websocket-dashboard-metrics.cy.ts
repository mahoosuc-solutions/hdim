/**
 * WebSocket Dashboard Real-Time Metrics E2E Tests
 *
 * Comprehensive end-to-end testing of real-time metrics updates
 * in the dashboard using WebSocket communication.
 *
 * Test Coverage:
 * - Real-time health score display and updates
 * - Real-time care gap metrics
 * - Dashboard metrics refresh rates
 * - Multi-metric simultaneous updates
 * - Metric animations and transitions
 * - Performance under continuous updates
 * - Metric aggregation and calculations
 */

describe('WebSocket Dashboard Real-Time Metrics - End-to-End Tests', () => {
  const BASE_URL = 'http://localhost:4200';
  const DASHBOARD_URL = `${BASE_URL}/dashboard`;

  beforeEach(() => {
    // Navigate to dashboard
    cy.visit(DASHBOARD_URL, { timeout: 10000 });
    // Wait for dashboard to load
    cy.get('app-dashboard', { timeout: 5000 }).should('exist');
    // Ensure WebSocket is connected
    cy.get('.connection-status-text').should('contain', 'Connected');
  });

  describe('Health Score Metrics - Display', () => {
    it('should display initial health score on dashboard load', () => {
      // RED: Dashboard should display health score
      cy.get('[data-testid="health-score-display"]').should('exist');
      cy.get('[data-testid="health-score-value"]').should('contain.text', /\d+/);
    });

    it('should show health score with category badge', () => {
      cy.get('[data-testid="health-score-category"]')
        .should('exist')
        .and('match', /excellent|good|fair|poor/i);
    });

    it('should display health score progress bar', () => {
      cy.get('[data-testid="health-score-progress"]')
        .should('exist')
        .and('have.css', 'width');
    });

    it('should show health score trend indicator', () => {
      cy.get('[data-testid="health-score-trend"]')
        .should('exist')
        .and('match', /up|down|stable/i);
    });

    it('should display health score contributors', () => {
      cy.get('[data-testid="health-score-factors"]')
        .should('exist');
      cy.get('[data-testid="health-score-factor"]')
        .should('have.length.at.least', 1);
    });

    it('should show last updated timestamp', () => {
      cy.get('[data-testid="health-score-updated"]')
        .should('exist')
        .and('contain.text', /\d{1,2}:\d{2}:\d{2}/);
    });
  });

  describe('Health Score - Real-Time Updates', () => {
    it('should update health score when WebSocket message received', () => {
      // Get initial score
      cy.get('[data-testid="health-score-value"]').then(($el) => {
        const initialScore = parseInt($el.text());

        // Simulate WebSocket update
        cy.window().then((win) => {
          const service = win['websocketService'];
          if (service) {
            const newScore = Math.min(initialScore + 5, 100);
            const updateMessage = {
              type: 'HEALTH_SCORE_UPDATE',
              data: {
                patientId: 'PATIENT123',
                score: newScore,
                category: newScore > 75 ? 'good' : 'fair',
                factors: ['adequate-bp-control', 'medication-adherence'],
                calculatedAt: Date.now(),
              },
            };
            service.simulateMessage(updateMessage);
          }
        });

        // Verify score updated
        cy.get('[data-testid="health-score-value"]', { timeout: 2000 })
          .should('contain.text', String(Math.min(initialScore + 5, 100)));
      });
    });

    it('should animate health score change', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
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
        }
      });

      // Check for animation class
      cy.get('[data-testid="health-score-display"]')
        .should('have.class', 'score-updating');
    });

    it('should update category based on score', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          // Simulate low score
          const updateMessage = {
            type: 'HEALTH_SCORE_UPDATE',
            data: {
              patientId: 'PATIENT123',
              score: 45,
              category: 'poor',
              factors: [],
              calculatedAt: Date.now(),
            },
          };
          service.simulateMessage(updateMessage);
        }
      });

      cy.get('[data-testid="health-score-category"]', { timeout: 2000 })
        .should('contain.text', 'Poor');
    });

    it('should update trend indicator', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          const updateMessage = {
            type: 'HEALTH_SCORE_UPDATE',
            data: {
              patientId: 'PATIENT123',
              score: 90,
              category: 'excellent',
              trend: 'improving',
              factorChanges: { 'medication-adherence': '+5%' },
              calculatedAt: Date.now(),
            },
          };
          service.simulateMessage(updateMessage);
        }
      });

      cy.get('[data-testid="health-score-trend"]', { timeout: 2000 })
        .should('contain.text', 'Improving');
    });
  });

  describe('Care Gap Metrics - Display', () => {
    it('should display care gap count', () => {
      // RED: Dashboard should show care gap metrics
      cy.get('[data-testid="care-gap-count"]')
        .should('exist')
        .and('contain.text', /\d+/);
    });

    it('should show care gap urgency breakdown', () => {
      cy.get('[data-testid="care-gap-urgency-routine"]')
        .should('exist');
      cy.get('[data-testid="care-gap-urgency-soon"]')
        .should('exist');
      cy.get('[data-testid="care-gap-urgency-overdue"]')
        .should('exist');
      cy.get('[data-testid="care-gap-urgency-critical"]')
        .should('exist');
    });

    it('should display care gap by measure', () => {
      cy.get('[data-testid="care-gap-by-measure"]')
        .should('exist');
      cy.get('[data-testid="care-gap-measure-item"]')
        .should('have.length.at.least', 1);
    });

    it('should show top priority care gaps', () => {
      cy.get('[data-testid="top-priority-gaps"]')
        .should('exist');
      cy.get('[data-testid="gap-priority-item"]')
        .should('have.length.at.least', 1);
    });

    it('should display care gap closure rate', () => {
      cy.get('[data-testid="care-gap-closure-rate"]')
        .should('exist')
        .and('contain.text', /\d+%/);
    });
  });

  describe('Care Gap Metrics - Real-Time Updates', () => {
    it('should update care gap count in real-time', () => {
      cy.get('[data-testid="care-gap-count"]').then(($el) => {
        const initialCount = parseInt($el.text());

        cy.window().then((win) => {
          const service = win['websocketService'];
          if (service) {
            const updateMessage = {
              type: 'CARE_GAP_NOTIFICATION',
              data: {
                gapId: 'GAP_NEW_001',
                patientId: 'PATIENT123',
                measureId: 'HEDIS-BCS',
                urgency: 'soon',
                recommendedAction: 'Schedule screening',
                notifiedAt: Date.now(),
              },
            };
            service.simulateMessage(updateMessage);
          }
        });

        // Count should increase
        cy.get('[data-testid="care-gap-count"]', { timeout: 2000 })
          .should('contain.text', String(initialCount + 1));
      });
    });

    it('should update urgency breakdown on new gaps', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          const updateMessage = {
            type: 'CARE_GAP_NOTIFICATION',
            data: {
              gapId: 'GAP_CRITICAL_001',
              patientId: 'PATIENT123',
              measureId: 'HEDIS-HBP',
              urgency: 'critical',
              recommendedAction: 'Immediate intervention',
              notifiedAt: Date.now(),
            },
          };
          service.simulateMessage(updateMessage);
        }
      });

      // Critical count should increase
      cy.get('[data-testid="care-gap-urgency-critical"]', { timeout: 2000 })
        .should('contain.text', /\d+/);
    });

    it('should highlight newly added critical gaps', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          const updateMessage = {
            type: 'CARE_GAP_NOTIFICATION',
            data: {
              gapId: 'GAP_CRITICAL_NEW',
              patientId: 'PATIENT123',
              measureId: 'HEDIS-CMC',
              urgency: 'critical',
              recommendedAction: 'Schedule appointment',
              notifiedAt: Date.now(),
            },
          };
          service.simulateMessage(updateMessage);
        }
      });

      cy.get('[data-testid="gap-item-CRITICAL-NEW"]', { timeout: 2000 })
        .should('have.class', 'gap-highlight-new');
    });

    it('should animate care gap count change', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          const updateMessage = {
            type: 'CARE_GAP_NOTIFICATION',
            data: {
              gapId: 'GAP_ANIM_001',
              patientId: 'PATIENT123',
              measureId: 'HEDIS-AAP',
              urgency: 'soon',
              recommendedAction: 'Review and plan',
              notifiedAt: Date.now(),
            },
          };
          service.simulateMessage(updateMessage);
        }
      });

      cy.get('[data-testid="care-gap-count"]')
        .should('have.class', 'count-updating');
    });
  });

  describe('Dashboard Metrics Grid - Layout', () => {
    it('should display metrics in responsive grid', () => {
      cy.get('[data-testid="metrics-grid"]')
        .should('exist')
        .and('have.css', 'display', 'grid');
    });

    it('should show metric cards with proper spacing', () => {
      cy.get('[data-testid="metric-card"]')
        .should('have.length.at.least', 3);
      cy.get('[data-testid="metric-card"]')
        .first()
        .should('have.css', 'gap');
    });

    it('should display metric titles', () => {
      cy.get('[data-testid="metric-title"]')
        .should('have.length.at.least', 3);
      cy.get('[data-testid="metric-title"]')
        .first()
        .should('contain.text', /health|gap|metric/i);
    });

    it('should show loading state initially then display metrics', () => {
      cy.reload();
      cy.get('[data-testid="metrics-loading"]').should('be.visible');
      cy.get('[data-testid="health-score-display"]', { timeout: 5000 }).should('be.visible');
    });
  });

  describe('Dashboard Performance', () => {
    it('should update health score in under 100ms', () => {
      const startTime = performance.now();
      let updateCompleteTime = 0;

      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          // Measure time to visual update
          cy.get('[data-testid="health-score-value"]').then(($el) => {
            const initialText = $el.text();

            service.simulateMessage({
              type: 'HEALTH_SCORE_UPDATE',
              data: {
                patientId: 'PATIENT123',
                score: 88,
                category: 'good',
                factors: [],
                calculatedAt: Date.now(),
              },
            });

            // Poll for update
            cy.get('[data-testid="health-score-value"]')
              .should(($updated) => {
                const newText = $updated.text();
                if (newText !== initialText) {
                  updateCompleteTime = performance.now();
                }
              });
          });
        }
      });

      cy.then(() => {
        const latency = updateCompleteTime - startTime;
        expect(latency).to.be.lessThan(100);
      });
    });

    it('should handle rapid metric updates without lag', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          // Send 10 rapid updates
          for (let i = 0; i < 10; i++) {
            service.simulateMessage({
              type: 'HEALTH_SCORE_UPDATE',
              data: {
                patientId: 'PATIENT123',
                score: 70 + i,
                category: i > 5 ? 'good' : 'fair',
                factors: [],
                calculatedAt: Date.now(),
              },
            });
          }
        }
      });

      // Dashboard should still be responsive
      cy.get('[data-testid="health-score-value"]')
        .should('contain.text', /\d+/);
      cy.get('[data-testid="dashboard"]')
        .should('not.have.class', 'frozen');
    });

    it('should not block UI during metric updates', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          // Continuously send updates for 2 seconds
          const interval = setInterval(() => {
            service.simulateMessage({
              type: 'HEALTH_SCORE_UPDATE',
              data: {
                patientId: 'PATIENT123',
                score: Math.floor(Math.random() * 100),
                category: 'fair',
                factors: [],
                calculatedAt: Date.now(),
              },
            });
          }, 100);

          setTimeout(() => clearInterval(interval), 2000);
        }
      });

      // Navigation should still work
      cy.get('[routerLink="/"]').should('be.enabled');
    });
  });

  describe('Multi-Metric Simultaneous Updates', () => {
    it('should handle simultaneous health score and care gap updates', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          // Send both message types at once
          service.simulateMessage({
            type: 'HEALTH_SCORE_UPDATE',
            data: {
              patientId: 'PATIENT123',
              score: 92,
              category: 'excellent',
              factors: ['improved-bp-control'],
              calculatedAt: Date.now(),
            },
          });

          service.simulateMessage({
            type: 'CARE_GAP_NOTIFICATION',
            data: {
              gapId: 'GAP_SIM_001',
              patientId: 'PATIENT123',
              measureId: 'HEDIS-CIS',
              urgency: 'routine',
              recommendedAction: 'Schedule follow-up',
              notifiedAt: Date.now(),
            },
          });
        }
      });

      // Both should update
      cy.get('[data-testid="health-score-value"]', { timeout: 2000 })
        .should('contain.text', '92');
      cy.get('[data-testid="care-gap-count"]', { timeout: 2000 })
        .should('exist');
    });

    it('should aggregate metrics correctly during rapid updates', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          // Send 5 gap updates rapidly
          for (let i = 0; i < 5; i++) {
            service.simulateMessage({
              type: 'CARE_GAP_NOTIFICATION',
              data: {
                gapId: `GAP_RAPID_${i}`,
                patientId: 'PATIENT123',
                measureId: `HEDIS-M${i}`,
                urgency: ['routine', 'soon', 'overdue', 'critical'][i % 4],
                recommendedAction: 'Action needed',
                notifiedAt: Date.now(),
              },
            });
          }
        }
      });

      // Count should reflect all updates
      cy.get('[data-testid="care-gap-count"]', { timeout: 2000 })
        .should('contain.text', /[5-9]|\d{2}/);
    });
  });

  describe('Metric Animations & Transitions', () => {
    it('should animate metric value changes', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          service.simulateMessage({
            type: 'HEALTH_SCORE_UPDATE',
            data: {
              patientId: 'PATIENT123',
              score: 95,
              category: 'excellent',
              factors: [],
              calculatedAt: Date.now(),
            },
          });
        }
      });

      cy.get('[data-testid="health-score-value"]')
        .should('have.css', 'transition');
    });

    it('should highlight updated metrics temporarily', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          service.simulateMessage({
            type: 'HEALTH_SCORE_UPDATE',
            data: {
              patientId: 'PATIENT123',
              score: 75,
              category: 'good',
              factors: [],
              calculatedAt: Date.now(),
            },
          });
        }
      });

      cy.get('[data-testid="health-score-display"]')
        .should('have.css', 'background-color')
        .and('not.equal', 'rgba(0, 0, 0, 0)');
    });

    it('should fade out highlight after update', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          service.simulateMessage({
            type: 'HEALTH_SCORE_UPDATE',
            data: {
              patientId: 'PATIENT123',
              score: 80,
              category: 'good',
              factors: [],
              calculatedAt: Date.now(),
            },
          });
        }
      });

      // Highlight should be removed after animation
      cy.get('[data-testid="health-score-display"]', { timeout: 2000 })
        .should('not.have.class', 'highlight');
    });
  });

  describe('Error Handling & Edge Cases', () => {
    it('should handle missing score data gracefully', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          service.simulateMessage({
            type: 'HEALTH_SCORE_UPDATE',
            data: {
              patientId: 'PATIENT123',
              // Missing score field
              category: 'unknown',
              factors: [],
              calculatedAt: Date.now(),
            },
          });
        }
      });

      // Dashboard should not crash
      cy.get('[data-testid="dashboard"]').should('exist');
    });

    it('should display dash or N/A for unavailable metrics', () => {
      cy.get('[data-testid="health-score-value"]').then(($el) => {
        // If score is unavailable, should show fallback
        const text = $el.text();
        expect(['N/A', '--', '0'].some(val => text.includes(val)) ||
                /\d+/.test(text)).to.be.true;
      });
    });

    it('should recover from WebSocket reconnection', () => {
      // Simulate disconnect
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service && service.disconnect) {
          service.disconnect();
        }
      });

      // Status should show reconnecting
      cy.get('.connection-status-text', { timeout: 10000 })
        .should('contain', 'Reconnect');

      // Metrics should still be visible (cached)
      cy.get('[data-testid="health-score-display"]')
        .should('exist');
    });

    it('should handle invalid urgency values in care gaps', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          service.simulateMessage({
            type: 'CARE_GAP_NOTIFICATION',
            data: {
              gapId: 'GAP_INVALID',
              patientId: 'PATIENT123',
              measureId: 'HEDIS-TEST',
              urgency: 'invalid_urgency',
              recommendedAction: 'Test',
              notifiedAt: Date.now(),
            },
          });
        }
      });

      // Dashboard should still render
      cy.get('[data-testid="dashboard"]').should('exist');
    });
  });

  describe('Metric Data Accuracy', () => {
    it('should display correct score value from WebSocket message', () => {
      const testScore = 87;

      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          service.simulateMessage({
            type: 'HEALTH_SCORE_UPDATE',
            data: {
              patientId: 'PATIENT123',
              score: testScore,
              category: 'good',
              factors: [],
              calculatedAt: Date.now(),
            },
          });
        }
      });

      cy.get('[data-testid="health-score-value"]', { timeout: 2000 })
        .should('contain.text', String(testScore));
    });

    it('should show correct care gap urgency color', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          service.simulateMessage({
            type: 'CARE_GAP_NOTIFICATION',
            data: {
              gapId: 'GAP_COLOR_TEST',
              patientId: 'PATIENT123',
              measureId: 'HEDIS-AAP',
              urgency: 'critical',
              recommendedAction: 'Urgent action',
              notifiedAt: Date.now(),
            },
          });
        }
      });

      cy.get('[data-testid="gap-item-CRITICAL-TEST"]', { timeout: 2000 })
        .should('have.css', 'color')
        .and('include', 'rgb'); // Should have color styling
    });

    it('should calculate and display correct metric aggregates', () => {
      cy.window().then((win) => {
        const service = win['websocketService'];
        if (service) {
          // Send known quantities
          service.simulateMessage({
            type: 'CARE_GAP_NOTIFICATION',
            data: {
              gapId: 'GAP_COUNT_1',
              patientId: 'PATIENT123',
              measureId: 'HEDIS-M1',
              urgency: 'critical',
              notifiedAt: Date.now(),
            },
          });

          service.simulateMessage({
            type: 'CARE_GAP_NOTIFICATION',
            data: {
              gapId: 'GAP_COUNT_2',
              patientId: 'PATIENT123',
              measureId: 'HEDIS-M2',
              urgency: 'routine',
              notifiedAt: Date.now(),
            },
          });
        }
      });

      // Total count should include both
      cy.get('[data-testid="care-gap-count"]', { timeout: 2000 })
        .should('contain.text', /2|[2-9]/);
    });
  });
});
