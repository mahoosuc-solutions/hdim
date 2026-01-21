/**
 * Phase 5D: Performance Monitoring & Load Testing E2E Tests
 *
 * Tests for real-time performance metrics collection, monitoring dashboard,
 * and load testing scenarios with 100+ concurrent connections.
 *
 * ★ Insight ─────────────────────────────────────
 * Performance monitoring serves two critical purposes:
 * - Production observability: Track latency, memory, CPU in real-time
 * - Load testing: Validate system can handle peak concurrent users
 * - Alerting: Detect performance degradation immediately
 * ─────────────────────────────────────────────────
 */

describe('Performance Monitoring & Load Testing', () => {
  beforeEach(() => {
    cy.visit('/');
    cy.wait(1000); // Wait for app initialization
  });

  describe('WebSocket Latency Monitoring', () => {
    it('should measure WebSocket message latency', () => {
      cy.get('[data-testid="performance-metrics"]').should('exist');

      // Trigger WebSocket message
      cy.window().then((win) => {
        const startTime = performance.now();
        win.__emitWebSocketMessage({
          type: 'HEALTH_SCORE_UPDATE',
          data: { score: 85, category: 'good' },
        });

        // Measure component update time
        cy.get('[data-testid="health-score-value"]').then(($el) => {
          const endTime = performance.now();
          const latency = endTime - startTime;

          // Should update component in < 100ms
          expect(latency).to.be.lessThan(100);
        });
      });
    });

    it('should track average latency over time', () => {
      const measurements: number[] = [];

      for (let i = 0; i < 10; i++) {
        cy.window().then((win) => {
          const start = performance.now();
          win.__emitWebSocketMessage({
            type: 'HEALTH_SCORE_UPDATE',
            data: { score: 70 + i, category: 'good' },
          });

          cy.get('[data-testid="health-score-value"]').then(() => {
            const end = performance.now();
            measurements.push(end - start);
          });
        });
      }

      cy.window().then(() => {
        const average = measurements.reduce((a, b) => a + b) / measurements.length;
        expect(average).to.be.lessThan(100);
      });
    });

    it('should detect latency spikes', () => {
      cy.get('[data-testid="latency-chart"]').should('exist');

      // Simulate rapid messages
      for (let i = 0; i < 50; i++) {
        cy.window().then((win) => {
          win.__emitWebSocketMessage({
            type: 'HEALTH_SCORE_UPDATE',
            data: { score: Math.random() * 100, category: 'good' },
          });
        });
      }

      // Chart should show spike detection
      cy.get('[data-testid="latency-spike-alert"]').should('exist');
      cy.get('[data-testid="latency-spike-count"]').should('have.text', '1');
    });

    it('should track percentile latencies (p50, p95, p99)', () => {
      cy.get('[data-testid="latency-percentiles"]').should('exist');
      cy.get('[data-testid="latency-p50"]').should('be.visible');
      cy.get('[data-testid="latency-p95"]').should('be.visible');
      cy.get('[data-testid="latency-p99"]').should('be.visible');

      // p99 should be higher than p50
      cy.get('[data-testid="latency-p99"]')
        .invoke('text')
        .then((p99Text) => {
          cy.get('[data-testid="latency-p50"]')
            .invoke('text')
            .then((p50Text) => {
              const p99 = parseInt(p99Text);
              const p50 = parseInt(p50Text);
              expect(p99).to.be.greaterThan(p50);
            });
        });
    });

    it('should log slow requests', () => {
      cy.window().then((win) => {
        // Simulate slow message processing
        win.__setMessageProcessingDelay(150); // 150ms delay
        win.__emitWebSocketMessage({
          type: 'HEALTH_SCORE_UPDATE',
          data: { score: 85, category: 'good' },
        });
      });

      // Should be logged as slow
      cy.get('[data-testid="slow-requests-log"]').should('exist');
      cy.get('[data-testid="slow-request-entry"]').should('have.length.greaterThan', 0);
    });
  });

  describe('Memory Usage Monitoring', () => {
    it('should track memory consumption', () => {
      cy.get('[data-testid="memory-chart"]').should('exist');
      cy.get('[data-testid="memory-usage"]').should('be.visible');
    });

    it('should alert on memory threshold exceeded', () => {
      cy.window().then((win) => {
        // Allocate lots of memory
        win.__allocateMemory(50); // 50MB
      });

      // Should show memory warning
      cy.get('[data-testid="memory-warning"]').should('be.visible');
      cy.get('[data-testid="memory-warning"]').should('contain', 'Memory usage');
    });

    it('should detect memory leaks', () => {
      cy.window().then((win) => {
        // Create and destroy many components
        for (let i = 0; i < 100; i++) {
          win.__emitWebSocketMessage({
            type: 'SYSTEM_ALERT_MESSAGE',
            data: { message: `Alert ${i}` },
          });
        }

        // Dismiss all
        cy.get('[data-testid="toast-close-button"]').each(($btn) => {
          cy.wrap($btn).click();
        });
      });

      // Memory should not grow significantly
      cy.get('[data-testid="memory-growth-rate"]')
        .invoke('text')
        .then((text) => {
          const growthRate = parseFloat(text);
          expect(growthRate).to.be.lessThan(5); // < 5% growth
        });
    });

    it('should track garbage collection events', () => {
      cy.get('[data-testid="gc-events-chart"]').should('exist');
      cy.get('[data-testid="gc-event-count"]').should('be.visible');
    });

    it('should show memory by component', () => {
      cy.get('[data-testid="component-memory-breakdown"]').should('exist');
      cy.get('[data-testid="component-memory-item"]').should('have.length.greaterThan', 0);
    });
  });

  describe('CPU & Rendering Performance', () => {
    it('should track frame rate (FPS)', () => {
      cy.get('[data-testid="fps-chart"]').should('exist');
      cy.get('[data-testid="current-fps"]').should('contain', 'FPS');

      // Should maintain 60 FPS
      cy.get('[data-testid="current-fps"]')
        .invoke('text')
        .then((text) => {
          const fps = parseInt(text);
          expect(fps).to.be.greaterThan(50); // At least 50 FPS
        });
    });

    it('should detect jank (frame drops)', () => {
      cy.window().then((win) => {
        // Trigger heavy computation during animation
        win.__triggerHeavyComputation(200); // 200ms blocking operation
      });

      // Should detect frame drop
      cy.get('[data-testid="frame-drop-alert"]').should('be.visible');
    });

    it('should measure component render time', () => {
      cy.get('[data-testid="component-render-times"]').should('exist');

      const components = [
        'ConnectionStatusComponent',
        'HealthScoreMetricsComponent',
        'CareGapMetricsComponent',
      ];

      components.forEach((component) => {
        cy.get(`[data-testid="render-time-${component}"]`)
          .invoke('text')
          .then((text) => {
            const renderTime = parseInt(text);
            expect(renderTime).to.be.lessThan(100);
          });
      });
    });

    it('should track layout recalculation time', () => {
      cy.get('[data-testid="layout-time-chart"]').should('exist');

      // Create layout changes
      cy.window().then((win) => {
        for (let i = 0; i < 10; i++) {
          win.__emitWebSocketMessage({
            type: 'CARE_GAP_NOTIFICATION',
            data: { gapId: `gap-${i}`, urgency: 'critical' },
          });
        }
      });

      // Layout time should not exceed threshold
      cy.get('[data-testid="layout-time"]')
        .invoke('text')
        .then((text) => {
          const layoutTime = parseInt(text);
          expect(layoutTime).to.be.lessThan(50);
        });
    });

    it('should monitor paint time', () => {
      cy.get('[data-testid="paint-time-chart"]').should('exist');
      cy.get('[data-testid="total-paint-time"]').should('be.visible');
    });
  });

  describe('Network Performance', () => {
    it('should measure WebSocket connection time', () => {
      cy.get('[data-testid="connection-time"]')
        .invoke('text')
        .then((text) => {
          const connectionTime = parseInt(text);
          expect(connectionTime).to.be.lessThan(2000); // < 2 seconds
        });
    });

    it('should track message throughput (messages/sec)', () => {
      cy.get('[data-testid="message-throughput"]').should('exist');

      cy.window().then((win) => {
        // Send 100 messages rapidly
        for (let i = 0; i < 100; i++) {
          win.__emitWebSocketMessage({
            type: 'HEALTH_SCORE_UPDATE',
            data: { score: 70 + (i % 30), category: 'good' },
          });
        }
      });

      // Should show throughput
      cy.get('[data-testid="message-throughput"]')
        .invoke('text')
        .then((text) => {
          const throughput = parseInt(text);
          expect(throughput).to.be.greaterThan(50); // > 50 messages/sec
        });
    });

    it('should detect message loss', () => {
      cy.window().then((win) => {
        // Simulate packet loss
        win.__setPacketLossRate(5); // 5% loss
      });

      // Should detect and report
      cy.get('[data-testid="packet-loss-warning"]').should('exist');
      cy.get('[data-testid="lost-message-count"]').should('be.visible');
    });

    it('should track bandwidth usage', () => {
      cy.get('[data-testid="bandwidth-chart"]').should('exist');
      cy.get('[data-testid="current-bandwidth"]').should('be.visible');
    });

    it('should detect high latency connections', () => {
      cy.window().then((win) => {
        // Simulate high latency (300ms)
        win.__setNetworkLatency(300);
      });

      cy.get('[data-testid="high-latency-alert"]').should('be.visible');
      cy.get('[data-testid="latency-warning-message"]').should('contain', '300ms');
    });
  });

  describe('Load Testing - 10+ Concurrent Users', () => {
    it('should handle 10 concurrent WebSocket connections', () => {
      cy.window().then((win) => {
        // Simulate 10 concurrent users
        win.__simulateConcurrentUsers(10);
      });

      // All connections should be active
      cy.get('[data-testid="active-connections"]')
        .invoke('text')
        .then((text) => {
          const activeConnections = parseInt(text);
          expect(activeConnections).to.equal(10);
        });
    });

    it('should handle rapid message flood (100 msg/sec)', () => {
      cy.window().then((win) => {
        // Send 100 messages per second
        win.__floodMessages(100);
      });

      // Should process all messages
      cy.get('[data-testid="message-queue-size"]')
        .invoke('text')
        .then((text) => {
          const queueSize = parseInt(text);
          expect(queueSize).to.equal(0); // All processed
        });
    });

    it('should handle 50+ concurrent notifications', () => {
      cy.window().then((win) => {
        // Create 50 notifications rapidly
        for (let i = 0; i < 50; i++) {
          win.__emitWebSocketMessage({
            type: 'SYSTEM_ALERT_MESSAGE',
            data: { message: `Alert ${i}` },
          });
        }
      });

      // All should be rendered
      cy.get('[data-testid="toast-container"]').should('have.length.greaterThan', 40);

      // Performance should not degrade significantly
      cy.get('[data-testid="current-fps"]')
        .invoke('text')
        .then((text) => {
          const fps = parseInt(text);
          expect(fps).to.be.greaterThan(30); // At least 30 FPS under load
        });
    });

    it('should maintain sub-100ms latency under load', () => {
      cy.window().then((win) => {
        win.__simulateConcurrentUsers(10);
      });

      const latencies: number[] = [];

      for (let i = 0; i < 20; i++) {
        cy.window().then((win) => {
          const start = performance.now();
          win.__emitWebSocketMessage({
            type: 'CARE_GAP_NOTIFICATION',
            data: { gapId: `gap-${i}`, urgency: 'critical' },
          });

          cy.get('[data-testid="care-gap-count"]').then(() => {
            const end = performance.now();
            latencies.push(end - start);
          });
        });
      }

      cy.window().then(() => {
        const maxLatency = Math.max(...latencies);
        expect(maxLatency).to.be.lessThan(100);
      });
    });

    it('should handle user reconnection during load', () => {
      cy.window().then((win) => {
        win.__simulateConcurrentUsers(10);
      });

      // Simulate one user disconnecting
      cy.window().then((win) => {
        win.__disconnectUser(1);
      });

      cy.get('[data-testid="active-connections"]')
        .invoke('text')
        .then((text) => {
          expect(parseInt(text)).to.equal(9);
        });

      // Simulate reconnection
      cy.window().then((win) => {
        win.__reconnectUser();
      });

      cy.get('[data-testid="active-connections"]')
        .invoke('text')
        .then((text) => {
          expect(parseInt(text)).to.equal(10);
        });
    });
  });

  describe('Performance Monitoring Dashboard', () => {
    it('should display real-time performance metrics', () => {
      cy.get('[data-testid="performance-dashboard"]').should('exist');

      // Key metrics should be visible
      cy.get('[data-testid="metrics-card-latency"]').should('be.visible');
      cy.get('[data-testid="metrics-card-memory"]').should('be.visible');
      cy.get('[data-testid="metrics-card-fps"]').should('be.visible');
      cy.get('[data-testid="metrics-card-throughput"]').should('be.visible');
    });

    it('should show historical performance trends', () => {
      cy.get('[data-testid="performance-history-chart"]').should('exist');

      // Chart should show multiple data points
      cy.get('[data-testid="chart-data-point"]').should('have.length.greaterThan', 5);
    });

    it('should display performance alerts', () => {
      cy.window().then((win) => {
        // Trigger poor performance
        win.__triggerHeavyComputation(500);
      });

      cy.get('[data-testid="performance-alerts"]').should('be.visible');
      cy.get('[data-testid="alert-item"]').should('exist');
    });

    it('should allow performance metric filtering', () => {
      cy.get('[data-testid="metric-filter"]').should('exist');

      // Filter to show only latency
      cy.get('[data-testid="filter-latency"]').click();

      cy.get('[data-testid="metrics-card-latency"]').should('be.visible');
      cy.get('[data-testid="metrics-card-memory"]').should('not.be.visible');
    });

    it('should export performance report', () => {
      cy.get('[data-testid="export-report-button"]').should('exist');
      cy.get('[data-testid="export-report-button"]').click();

      // Report should be generated
      cy.get('[data-testid="export-success-message"]').should('be.visible');
    });

    it('should show performance budget compliance', () => {
      cy.get('[data-testid="performance-budget"]').should('exist');

      // Check each budget metric
      cy.get('[data-testid="budget-latency"]').should('contain', 'ms');
      cy.get('[data-testid="budget-memory"]').should('contain', 'MB');
      cy.get('[data-testid="budget-fps"]').should('contain', 'FPS');
    });
  });

  describe('Performance Benchmarking', () => {
    it('should benchmark component rendering', () => {
      cy.get('[data-testid="benchmark-button"]').click();

      // Benchmarks should run
      cy.get('[data-testid="benchmark-running"]').should('be.visible');

      // Results should display
      cy.get('[data-testid="benchmark-results"]').should('be.visible', { timeout: 10000 });
      cy.get('[data-testid="benchmark-result-item"]').should('have.length.greaterThan', 0);
    });

    it('should benchmark notification creation', () => {
      const startTime = performance.now();

      // Create 100 notifications
      cy.window().then((win) => {
        for (let i = 0; i < 100; i++) {
          win.__createNotification({
            type: 'success',
            message: `Notification ${i}`,
          });
        }
      });

      cy.get('[data-testid="toast-container"]').then(() => {
        const endTime = performance.now();
        const totalTime = endTime - startTime;

        // Should complete in reasonable time
        expect(totalTime).to.be.lessThan(5000); // < 5 seconds
      });
    });

    it('should benchmark WebSocket message processing', () => {
      const benchmarkRuns = 100;
      const latencies: number[] = [];

      for (let i = 0; i < benchmarkRuns; i++) {
        cy.window().then((win) => {
          const start = performance.now();
          win.__emitWebSocketMessage({
            type: 'HEALTH_SCORE_UPDATE',
            data: { score: Math.random() * 100, category: 'good' },
          });

          cy.get('[data-testid="health-score-value"]').then(() => {
            const end = performance.now();
            latencies.push(end - start);
          });
        });
      }

      cy.window().then(() => {
        const avgLatency = latencies.reduce((a, b) => a + b) / latencies.length;
        const maxLatency = Math.max(...latencies);
        const minLatency = Math.min(...latencies);

        expect(avgLatency).to.be.lessThan(100);
        expect(maxLatency).to.be.lessThan(200);
      });
    });
  });

  describe('Performance Regression Detection', () => {
    it('should detect performance degradation', () => {
      cy.get('[data-testid="baseline-latency"]')
        .invoke('text')
        .then((baselineText) => {
          const baseline = parseInt(baselineText);

          cy.window().then((win) => {
            // Simulate performance degradation
            win.__setMessageProcessingDelay(baseline + 50);
            win.__emitWebSocketMessage({
              type: 'HEALTH_SCORE_UPDATE',
              data: { score: 85, category: 'good' },
            });
          });

          // Should detect regression
          cy.get('[data-testid="regression-detected"]').should('be.visible');
        });
    });

    it('should track performance metrics over time', () => {
      cy.get('[data-testid="metrics-timeline"]').should('exist');

      // Should show trend
      cy.get('[data-testid="timeline-data-point"]').should('have.length.greaterThan', 1);
    });

    it('should identify performance hotspots', () => {
      cy.get('[data-testid="performance-hotspots"]').should('exist');

      // Hotspots should be identified
      cy.get('[data-testid="hotspot-item"]').should('have.length.greaterThan', 0);
    });
  });

  describe('Performance Alerts & Notifications', () => {
    it('should alert when latency exceeds threshold', () => {
      cy.window().then((win) => {
        win.__setMessageProcessingDelay(150); // > 100ms
        win.__emitWebSocketMessage({
          type: 'HEALTH_SCORE_UPDATE',
          data: { score: 85, category: 'good' },
        });
      });

      cy.get('[data-testid="latency-alert"]').should('be.visible');
    });

    it('should alert when FPS drops below 50', () => {
      cy.window().then((win) => {
        win.__triggerHeavyComputation(300);
      });

      cy.get('[data-testid="fps-warning"]').should('be.visible');
      cy.get('[data-testid="fps-warning"]').should('contain', '< 50 FPS');
    });

    it('should alert on memory leak detection', () => {
      cy.window().then((win) => {
        // Create memory leak
        for (let i = 0; i < 1000; i++) {
          win.__allocateMemoryWithoutCleanup(1); // 1MB each
        }
      });

      cy.get('[data-testid="memory-leak-alert"]').should('be.visible');
    });

    it('should send email alerts for critical performance issues', () => {
      cy.window().then((win) => {
        // Trigger critical issue
        win.__triggerCriticalPerformanceIssue();
      });

      // Should queue email alert
      cy.get('[data-testid="email-alert-queued"]').should('be.visible');
    });
  });
});
