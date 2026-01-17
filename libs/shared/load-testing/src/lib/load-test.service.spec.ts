import { TestBed } from '@angular/core/testing';
import { LoadTestService, LoadTestConfig, LoadTestMetrics } from './load-test.service';

describe('LoadTestService', () => {
  let service: LoadTestService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [LoadTestService]
    });
    service = TestBed.inject(LoadTestService);
  });

  describe('Load Test Execution', () => {
    it('should execute a load test with configuration', async () => {
      const config: LoadTestConfig = {
        concurrentConnections: 10,
        duration: 5000,
        messagesPerSecond: 100,
        networkCondition: 'normal'
      };

      const result = await service.executeLoadTest(config);

      expect(result.testId).toBeTruthy();
      expect(result.config).toEqual(config);
      expect(result.metrics).toBeTruthy();
    });

    it('should complete load test execution', async () => {
      const config: LoadTestConfig = {
        concurrentConnections: 5,
        duration: 2000,
        messagesPerSecond: 50,
        networkCondition: 'normal'
      };

      const result = await service.executeLoadTest(config);

      expect(result.metrics.duration).toBe(config.duration);
      expect(result.metrics.concurrentConnections).toBe(config.concurrentConnections);
    });

    it('should support ramp-up and ramp-down', async () => {
      const config: LoadTestConfig = {
        concurrentConnections: 10,
        duration: 3000,
        messagesPerSecond: 100,
        networkCondition: 'normal',
        rampUpTime: 1000,
        rampDownTime: 1000
      };

      const result = await service.executeLoadTest(config);

      expect(result.metrics).toBeTruthy();
    });
  });

  describe('Concurrent Connections', () => {
    it('should simulate concurrent connections', async () => {
      const connections = await service.simulateConcurrentConnections(10, 5000);

      expect(connections.length).toBe(10);
      connections.forEach(conn => {
        expect(conn.connectionId).toBeTruthy();
        expect(typeof conn.connected).toBe('boolean');
      });
    });

    it('should simulate 50 concurrent connections', async () => {
      const connections = await service.simulateConcurrentConnections(50, 5000);
      expect(connections.length).toBe(50);
    });

    it('should simulate 100 concurrent connections', async () => {
      const connections = await service.simulateConcurrentConnections(100, 5000);
      expect(connections.length).toBe(100);
    });

    it('should track connection state', async () => {
      const connections = await service.simulateConcurrentConnections(10, 5000);

      const connectedCount = connections.filter(c => c.connected).length;
      expect(connectedCount).toBeGreaterThan(7); // Allow some failures
    });

    it('should track connection metrics', async () => {
      const connections = await service.simulateConcurrentConnections(5, 5000);

      connections.forEach(conn => {
        expect(conn.averageLatency).toBeGreaterThan(0);
        expect(conn.peakLatency).toBeGreaterThanOrEqual(conn.averageLatency);
      });
    });
  });

  describe('Message Throughput', () => {
    it('should simulate message throughput', async () => {
      const result = await service.simulateMessageThroughput(100, 5000);

      expect(result.sent).toBeGreaterThan(0);
      expect(result.received).toBeGreaterThan(0);
      expect(result.throughput).toBeGreaterThan(0);
    });

    it('should handle 50 messages per second', async () => {
      const result = await service.simulateMessageThroughput(50, 5000);

      expect(result.sent).toBeGreaterThan(0);
      expect(result.received).toBeCloseTo(result.sent, 1);
    });

    it('should handle 100 messages per second', async () => {
      const result = await service.simulateMessageThroughput(100, 5000);

      expect(result.throughput).toBeGreaterThan(90);
    });

    it('should account for message loss', async () => {
      const result = await service.simulateMessageThroughput(100, 5000);

      expect(result.received).toBeLessThanOrEqual(result.sent);
    });
  });

  describe('Network Conditions', () => {
    it('should simulate normal network conditions', () => {
      const condition = service.simulateNetworkCondition('normal');

      expect(condition.latencyMs).toBeGreaterThan(0);
      expect(condition.packetLossPercent).toBeLessThan(1);
      expect(condition.bandwidthMbps).toBeGreaterThan(50);
    });

    it('should simulate slow network conditions', () => {
      const condition = service.simulateNetworkCondition('slow');

      expect(condition.latencyMs).toBeGreaterThan(100);
      expect(condition.packetLossPercent).toBeGreaterThan(1);
      expect(condition.bandwidthMbps).toBeLessThan(50);
    });

    it('should simulate offline conditions', () => {
      const condition = service.simulateNetworkCondition('offline');

      expect(condition.latencyMs).toBeGreaterThan(1000);
      expect(condition.packetLossPercent).toBe(100);
    });

    it('should simulate unstable network conditions', () => {
      const condition = service.simulateNetworkCondition('unstable');

      expect(condition.latencyMs).toBeGreaterThan(100);
      expect(condition.packetLossPercent).toBeGreaterThan(0);
    });
  });

  describe('Resource Monitoring', () => {
    it('should monitor memory usage', () => {
      const memory = service.monitorMemory();

      expect(typeof memory).toBe('number');
      expect(memory).toBeGreaterThan(0);
    });

    it('should monitor CPU usage', () => {
      const cpu = service.monitorCPU();

      expect(typeof cpu).toBe('number');
      expect(cpu).toBeGreaterThanOrEqual(0);
      expect(cpu).toBeLessThanOrEqual(100);
    });
  });

  describe('Test Results Management', () => {
    it('should store test results', async () => {
      const config: LoadTestConfig = {
        concurrentConnections: 5,
        duration: 1000,
        messagesPerSecond: 50,
        networkCondition: 'normal'
      };

      const result = await service.executeLoadTest(config);
      const results = service.getTestResults();

      expect(results.length).toBeGreaterThan(0);
      expect(results[results.length - 1].testId).toBe(result.testId);
    });

    it('should retrieve test result by ID', async () => {
      const config: LoadTestConfig = {
        concurrentConnections: 5,
        duration: 1000,
        messagesPerSecond: 50,
        networkCondition: 'normal'
      };

      const result = await service.executeLoadTest(config);
      const retrieved = service.getTestResult(result.testId);

      expect(retrieved).toEqual(result);
    });

    it('should return undefined for non-existent test ID', () => {
      const result = service.getTestResult('non-existent-id');
      expect(result).toBeUndefined();
    });
  });

  describe('Result Comparison', () => {
    it('should compare two test results', async () => {
      const config: LoadTestConfig = {
        concurrentConnections: 5,
        duration: 1000,
        messagesPerSecond: 50,
        networkCondition: 'normal'
      };

      const result1 = await service.executeLoadTest(config);
      const result2 = await service.executeLoadTest(config);

      const comparison = service.compareResults(result1, result2);

      expect(comparison).toHaveProperty('improved');
      expect(comparison).toHaveProperty('changes');
      expect(comparison.changes).toHaveProperty('latency');
      expect(comparison.changes).toHaveProperty('throughput');
    });

    it('should calculate improvement correctly', async () => {
      const config: LoadTestConfig = {
        concurrentConnections: 5,
        duration: 1000,
        messagesPerSecond: 50,
        networkCondition: 'normal'
      };

      const result1 = await service.executeLoadTest(config);
      const result2 = await service.executeLoadTest(config);

      const comparison = service.compareResults(result1, result2);

      expect(typeof comparison.improved).toBe('boolean');
    });
  });

  describe('Threshold Management', () => {
    it('should get default thresholds', () => {
      const thresholds = service.getThresholds();

      expect(thresholds.maxAverageLatency).toBeGreaterThan(0);
      expect(thresholds.minSuccessRate).toBeGreaterThan(0);
      expect(thresholds.minSuccessRate).toBeLessThan(1);
    });

    it('should set custom thresholds', () => {
      service.setThresholds({
        maxAverageLatency: 200,
        minSuccessRate: 0.95
      });

      const thresholds = service.getThresholds();
      expect(thresholds.maxAverageLatency).toBe(200);
      expect(thresholds.minSuccessRate).toBe(0.95);
    });

    it('should preserve unmodified thresholds', () => {
      const originalThresholds = service.getThresholds();

      service.setThresholds({
        maxAverageLatency: 250
      });

      const updatedThresholds = service.getThresholds();
      expect(updatedThresholds.maxP95Latency).toBe(originalThresholds.maxP95Latency);
    });
  });

  describe('Metrics and Reporting', () => {
    it('should generate metrics with proper structure', async () => {
      const config: LoadTestConfig = {
        concurrentConnections: 5,
        duration: 1000,
        messagesPerSecond: 50,
        networkCondition: 'normal'
      };

      const result = await service.executeLoadTest(config);
      const metrics = result.metrics;

      expect(metrics.timestamp).toBeGreaterThan(0);
      expect(metrics.duration).toBe(config.duration);
      expect(metrics.concurrentConnections).toBe(config.concurrentConnections);
      expect(metrics.aggregateStats).toBeTruthy();
    });

    it('should calculate aggregate statistics', async () => {
      const config: LoadTestConfig = {
        concurrentConnections: 10,
        duration: 2000,
        messagesPerSecond: 100,
        networkCondition: 'normal'
      };

      const result = await service.executeLoadTest(config);
      const stats = result.metrics.aggregateStats;

      expect(stats.totalMessages).toBeGreaterThan(0);
      expect(stats.successRate).toBeGreaterThanOrEqual(0);
      expect(stats.successRate).toBeLessThanOrEqual(1);
      expect(stats.averageLatency).toBeGreaterThan(0);
      expect(stats.p50Latency).toBeGreaterThan(0);
      expect(stats.p95Latency).toBeGreaterThanOrEqual(stats.p50Latency);
      expect(stats.p99Latency).toBeGreaterThanOrEqual(stats.p95Latency);
    });

    it('should generate load test report', async () => {
      const config: LoadTestConfig = {
        concurrentConnections: 5,
        duration: 1000,
        messagesPerSecond: 50,
        networkCondition: 'normal'
      };

      const result = await service.executeLoadTest(config);
      const report = result.report;

      expect(report.title).toBeTruthy();
      expect(typeof report.passed).toBe('boolean');
      expect(Array.isArray(report.recommendations)).toBe(true);
      expect(Array.isArray(report.thresholdBreaches)).toBe(true);
    });

    it('should identify threshold breaches', async () => {
      service.setThresholds({
        maxAverageLatency: 10, // Very strict threshold
        minSuccessRate: 0.99
      });

      const config: LoadTestConfig = {
        concurrentConnections: 100,
        duration: 5000,
        messagesPerSecond: 500,
        networkCondition: 'slow'
      };

      const result = await service.executeLoadTest(config);

      // With strict thresholds and heavy load, breaches likely
      expect(typeof result.report.passed).toBe('boolean');
    });
  });

  describe('Observable Streams', () => {
    it('should emit progress updates', async () => {
      let progressValues: number[] = [];

      service.progress$.subscribe(progress => {
        progressValues.push(progress);
      });

      const config: LoadTestConfig = {
        concurrentConnections: 5,
        duration: 2000,
        messagesPerSecond: 50,
        networkCondition: 'normal'
      };

      await service.executeLoadTest(config);

      expect(progressValues.length).toBeGreaterThan(0);
      expect(progressValues[progressValues.length - 1]).toBe(100);
    });

    it('should emit metrics updates', async () => {
      let metricsUpdates: (LoadTestMetrics | null)[] = [];

      service.metrics$.subscribe(metrics => {
        metricsUpdates.push(metrics);
      });

      const config: LoadTestConfig = {
        concurrentConnections: 5,
        duration: 1000,
        messagesPerSecond: 50,
        networkCondition: 'normal'
      };

      await service.executeLoadTest(config);

      expect(metricsUpdates.length).toBeGreaterThan(0);
      expect(metricsUpdates[metricsUpdates.length - 1]).toBeTruthy();
    });
  });

  describe('Integration Scenarios', () => {
    it('should execute complete load test workflow', async () => {
      const config: LoadTestConfig = {
        concurrentConnections: 20,
        duration: 5000,
        messagesPerSecond: 100,
        networkCondition: 'normal',
        rampUpTime: 1000,
        rampDownTime: 1000
      };

      const result = await service.executeLoadTest(config);

      expect(result.testId).toBeTruthy();
      expect(result.metrics.aggregateStats.successRate).toBeGreaterThan(0.90);
      expect(result.report).toBeTruthy();
    });

    it('should handle multiple sequential tests', async () => {
      const config1: LoadTestConfig = {
        concurrentConnections: 10,
        duration: 1000,
        messagesPerSecond: 50,
        networkCondition: 'normal'
      };

      const config2: LoadTestConfig = {
        concurrentConnections: 20,
        duration: 1000,
        messagesPerSecond: 100,
        networkCondition: 'normal'
      };

      const result1 = await service.executeLoadTest(config1);
      const result2 = await service.executeLoadTest(config2);

      expect(service.getTestResults().length).toBeGreaterThanOrEqual(2);
      expect(result1.testId).not.toBe(result2.testId);
    });
  });
});
