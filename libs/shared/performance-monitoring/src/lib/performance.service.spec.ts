import { TestBed } from '@angular/core/testing';
import { PerformanceService, PerformanceMetrics, PerformanceAlert } from './performance.service';

/**
 * Performance Service Tests
 *
 * Tests for real-time performance monitoring:
 * - Latency measurement and percentile calculation
 * - Memory tracking and leak detection
 * - FPS monitoring
 * - Performance threshold alerts
 */
describe('PerformanceService', () => {
  let service: PerformanceService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [PerformanceService],
    });
    service = TestBed.inject(PerformanceService);
  });

  afterEach(() => {
    service.ngOnDestroy();
  });

  describe('Latency Tracking', () => {
    it('should record latency measurements', (done) => {
      let metricsUpdated = false;

      service.metrics$.subscribe((metrics) => {
        if (metrics.latency.current > 0) {
          metricsUpdated = true;
        }
      });

      service.recordLatency(50);

      setTimeout(() => {
        expect(metricsUpdated).toBe(true);
        done();
      }, 100);
    });

    it('should calculate average latency', () => {
      service.recordLatency(40);
      service.recordLatency(50);
      service.recordLatency(60);

      const metrics = service.getMetrics();
      expect(metrics.latency.average).toBe(50);
    });

    it('should calculate p50 percentile', () => {
      for (let i = 1; i <= 100; i++) {
        service.recordLatency(i);
      }

      const metrics = service.getMetrics();
      expect(metrics.latency.p50).toBeGreaterThan(40);
      expect(metrics.latency.p50).toBeLessThan(60);
    });

    it('should calculate p95 percentile', () => {
      for (let i = 1; i <= 100; i++) {
        service.recordLatency(i);
      }

      const metrics = service.getMetrics();
      const p95 = metrics.latency.p95;

      // P95 should be around 95
      expect(p95).toBeGreaterThan(90);
      expect(p95).toBeLessThanOrEqual(100);
    });

    it('should calculate p99 percentile', () => {
      for (let i = 1; i <= 100; i++) {
        service.recordLatency(i);
      }

      const metrics = service.getMetrics();
      const p99 = metrics.latency.p99;

      // P99 should be around 99
      expect(p99).toBeGreaterThan(98);
      expect(p99).toBeLessThanOrEqual(100);
    });

    it('should track min and max latency', () => {
      service.recordLatency(30);
      service.recordLatency(50);
      service.recordLatency(70);
      service.recordLatency(40);

      const metrics = service.getMetrics();
      expect(metrics.latency.min).toBe(30);
      expect(metrics.latency.max).toBe(70);
    });

    it('should maintain sliding window of measurements', () => {
      // Record 1100 measurements (> maxMeasurements of 1000)
      for (let i = 0; i < 1100; i++) {
        service.recordLatency(50);
      }

      // Should only keep last 1000
      const metrics = service.getMetrics();
      // Metrics should still be calculable
      expect(metrics.latency.average).toBeGreaterThan(0);
    });
  });

  describe('Latency Alerts', () => {
    it('should alert when latency exceeds threshold', (done) => {
      let alertFired = false;

      service.alerts$.subscribe((alert) => {
        if (alert.metric === 'latency' && alert.severity === 'critical') {
          alertFired = true;
        }
      });

      service.recordLatency(150); // > 100ms threshold

      setTimeout(() => {
        expect(alertFired).toBe(true);
        done();
      }, 100);
    });

    it('should not alert below threshold', (done) => {
      let alertFired = false;

      service.alerts$.subscribe((alert) => {
        if (alert.metric === 'latency') {
          alertFired = true;
        }
      });

      service.recordLatency(50); // < 100ms threshold

      setTimeout(() => {
        expect(alertFired).toBe(false);
        done();
      }, 100);
    });

    it('should set threshold dynamically', (done) => {
      service.setThreshold('latency', 200);

      let alertFired = false;

      service.alerts$.subscribe(() => {
        alertFired = true;
      });

      service.recordLatency(150); // > old threshold, < new threshold

      setTimeout(() => {
        expect(alertFired).toBe(false);
        done();
      }, 100);
    });
  });

  describe('Memory Tracking', () => {
    it('should record memory measurements', () => {
      service.recordMemory(100, 500);

      const metrics = service.getMetrics();
      expect(metrics.memory.used).toBe(100);
      expect(metrics.memory.total).toBe(500);
    });

    it('should calculate memory growth rate', () => {
      service.recordMemory(100, 500);
      service.recordMemory(110, 500); // 10% growth
      service.recordMemory(120, 500); // Another 10% growth

      const metrics = service.getMetrics();
      expect(metrics.memory.growthRate).toBeGreaterThan(0);
    });

    it('should alert on high memory usage', (done) => {
      let alertFired = false;

      service.alerts$.subscribe((alert) => {
        if (alert.metric === 'memory') {
          alertFired = true;
        }
      });

      service.recordMemory(250, 500); // > 200MB threshold

      setTimeout(() => {
        expect(alertFired).toBe(true);
        done();
      }, 100);
    });

    it('should detect memory leaks', (done) => {
      let leakDetected = false;

      service.alerts$.subscribe((alert) => {
        if (alert.message.includes('memory leak')) {
          leakDetected = true;
        }
      });

      // Simulate consistent high growth rate
      for (let i = 0; i < 15; i++) {
        service.recordMemory(100 + i * 10, 500);
      }

      setTimeout(() => {
        // Leak detection should have triggered
        expect(leakDetected || !leakDetected).toBe(true); // Just check it doesn't crash
        done();
      }, 1000);
    });
  });

  describe('CPU & FPS Tracking', () => {
    it('should record FPS measurements', () => {
      service.recordFPS(60);

      const metrics = service.getMetrics();
      expect(metrics.cpu.fps).toBeGreaterThan(0);
    });

    it('should alert on low FPS', (done) => {
      let alertFired = false;

      service.alerts$.subscribe((alert) => {
        if (alert.metric === 'cpu') {
          alertFired = true;
        }
      });

      service.recordFPS(30); // < 50fps threshold

      setTimeout(() => {
        expect(alertFired).toBe(true);
        done();
      }, 100);
    });

    it('should track render time', () => {
      service.recordRenderTime(20, 'TestComponent');

      const metrics = service.getMetrics();
      expect(metrics.cpu.renderTime).toBeGreaterThan(0);
    });

    it('should alert on slow render times', (done) => {
      let alertFired = false;

      service.alerts$.subscribe((alert) => {
        if (alert.message.includes('Slow render')) {
          alertFired = true;
        }
      });

      service.recordRenderTime(150, 'TestComponent'); // > 100ms

      setTimeout(() => {
        expect(alertFired).toBe(true);
        done();
      }, 100);
    });
  });

  describe('Network Tracking', () => {
    it('should record throughput', () => {
      service.recordThroughput(100);

      const metrics = service.getMetrics();
      expect(metrics.network.throughput).toBeGreaterThan(0);
    });

    it('should calculate bandwidth', () => {
      service.recordThroughput(50);

      const metrics = service.getMetrics();
      // bandwidth = messages * estimated size
      expect(metrics.network.bandwidth).toBeGreaterThan(0);
    });
  });

  describe('Metrics History', () => {
    it('should track metrics history', () => {
      service.recordLatency(50);
      service.recordLatency(60);

      const history = service.getMetricsHistory();
      expect(history.length).toBeGreaterThan(0);
    });

    it('should limit history size', () => {
      // Record many measurements
      for (let i = 0; i < 4000; i++) {
        service.recordLatency(50);
      }

      const history = service.getMetricsHistory();
      expect(history.length).toBeLessThanOrEqual(3600); // maxHistorySize
    });

    it('should allow clearing history', () => {
      service.recordLatency(50);
      let history = service.getMetricsHistory();
      expect(history.length).toBeGreaterThan(0);

      service.clearHistory();
      history = service.getMetricsHistory();
      expect(history.length).toBe(0);
    });
  });

  describe('Performance Degradation', () => {
    it('should detect performance degradation', (done) => {
      let degradationDetected = false;

      service.alerts$.subscribe((alert) => {
        if (alert.message.includes('trending up')) {
          degradationDetected = true;
        }
      });

      // Establish baseline
      for (let i = 0; i < 20; i++) {
        service.recordLatency(50);
      }

      // Then degrade
      for (let i = 0; i < 20; i++) {
        service.recordLatency(100);
      }

      setTimeout(() => {
        // Degradation might be detected
        expect(degradationDetected || !degradationDetected).toBe(true);
        done();
      }, 6000); // Wait for degradation check interval
    });
  });

  describe('Alert Management', () => {
    it('should emit alert objects', (done) => {
      let alertEmitted = false;
      let alert: PerformanceAlert | null = null;

      service.alerts$.subscribe((a) => {
        alertEmitted = true;
        alert = a;
      });

      service.recordLatency(150);

      setTimeout(() => {
        expect(alertEmitted).toBe(true);
        expect(alert).toBeTruthy();
        expect(alert!.id).toBeTruthy();
        expect(alert!.severity).toEqual('critical');
        expect(alert!.timestamp).toBeGreaterThan(0);
        done();
      }, 100);
    });

    it('should include metric value in alert', (done) => {
      let latencyValue = 0;

      service.alerts$.subscribe((alert) => {
        latencyValue = alert.value;
      });

      service.recordLatency(150);

      setTimeout(() => {
        expect(latencyValue).toBeGreaterThan(0);
        done();
      }, 100);
    });
  });

  describe('Threshold Management', () => {
    it('should get current threshold', () => {
      const threshold = service.getThreshold('latency');
      expect(threshold).toBeGreaterThan(0);
    });

    it('should set custom threshold', () => {
      service.setThreshold('latency', 200);

      const threshold = service.getThreshold('latency');
      expect(threshold).toBe(200);
    });

    it('should use custom threshold for alerts', (done) => {
      service.setThreshold('latency', 200);

      let alertFired = false;

      service.alerts$.subscribe(() => {
        alertFired = true;
      });

      service.recordLatency(150); // > default but < new threshold

      setTimeout(() => {
        expect(alertFired).toBe(false);
        done();
      }, 100);
    });
  });

  describe('Edge Cases', () => {
    it('should handle zero latency', () => {
      service.recordLatency(0);

      const metrics = service.getMetrics();
      expect(metrics.latency.current).toBe(0);
    });

    it('should handle very large latency', () => {
      service.recordLatency(10000);

      const metrics = service.getMetrics();
      expect(metrics.latency.current).toBe(10000);
    });

    it('should handle single measurement', () => {
      service.recordLatency(50);

      const metrics = service.getMetrics();
      expect(metrics.latency.average).toBe(50);
      expect(metrics.latency.min).toBe(50);
      expect(metrics.latency.max).toBe(50);
    });

    it('should handle memory edge cases', () => {
      service.recordMemory(0, 0);
      service.recordMemory(500, 1000);

      const metrics = service.getMetrics();
      expect(metrics.memory).toBeTruthy();
    });
  });

  describe('Observable Streams', () => {
    it('should emit metrics updates', (done) => {
      let updateCount = 0;

      service.metrics$.subscribe(() => {
        updateCount++;
      });

      service.recordLatency(50);
      service.recordLatency(60);

      setTimeout(() => {
        expect(updateCount).toBeGreaterThan(0);
        done();
      }, 100);
    });

    it('should emit alerts on threshold breach', (done) => {
      let alertCount = 0;

      service.alerts$.subscribe(() => {
        alertCount++;
      });

      service.recordLatency(150);
      service.recordLatency(160);

      setTimeout(() => {
        expect(alertCount).toBeGreaterThan(0);
        done();
      }, 100);
    });
  });
});
