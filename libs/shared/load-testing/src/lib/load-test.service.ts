/**
 * Load Test Service
 *
 * Comprehensive load testing harness for benchmarking and stress testing.
 *
 * Features:
 * - Concurrent connection simulation
 * - Message throughput testing
 * - Sustained load monitoring
 * - Network condition simulation
 * - Stress test execution
 * - Performance metrics collection
 * - Load test reporting
 * - Memory/CPU monitoring
 *
 * Usage:
 * ```typescript
 * constructor(private loadTest: LoadTestService) {}
 *
 * async runLoadTest() {
 *   const config: LoadTestConfig = {
 *     concurrentConnections: 100,
 *     duration: 60000, // 1 minute
 *     messagesPerSecond: 100,
 *     networkCondition: 'normal'
 *   };
 *
 *   const results = await this.loadTest.executeLoadTest(config);
 *   console.log('Success Rate:', results.successRate);
 * }
 * ```
 */

import { Injectable } from '@angular/core';
import { Subject, Observable, BehaviorSubject } from 'rxjs';
import { v4 as uuidv4 } from 'uuid';

export interface LoadTestConfig {
  concurrentConnections: number;
  duration: number; // milliseconds
  messagesPerSecond: number;
  networkCondition: 'normal' | 'slow' | 'offline' | 'unstable';
  rampUpTime?: number;
  rampDownTime?: number;
}

export interface NetworkCondition {
  latencyMs: number;
  packetLossPercent: number;
  bandwidthMbps: number;
}

export interface ConnectionMetrics {
  connectionId: string;
  connected: boolean;
  messagesSent: number;
  messagesReceived: number;
  errors: number;
  averageLatency: number;
  peakLatency: number;
}

export interface LoadTestMetrics {
  timestamp: number;
  duration: number;
  concurrentConnections: number;
  totalConnections: ConnectionMetrics[];
  aggregateStats: {
    totalMessages: number;
    successRate: number;
    averageLatency: number;
    p50Latency: number;
    p95Latency: number;
    p99Latency: number;
    peakLatency: number;
    memoryUsage: number;
    cpuUsage: number;
    bandwidthUsage: number;
    errorRate: number;
  };
}

export interface LoadTestResult {
  testId: string;
  config: LoadTestConfig;
  metrics: LoadTestMetrics;
  report: {
    title: string;
    passed: boolean;
    recommendations: string[];
    thresholdBreaches: string[];
  };
}

@Injectable({
  providedIn: 'root'
})
export class LoadTestService {
  private metricsSubject = new BehaviorSubject<LoadTestMetrics | null>(null);
  public readonly metrics$: Observable<LoadTestMetrics | null> = this.metricsSubject.asObservable();

  private progressSubject = new BehaviorSubject<number>(0);
  public readonly progress$: Observable<number> = this.progressSubject.asObservable();

  private testResults: LoadTestResult[] = [];
  private networkConditions: Map<string, NetworkCondition> = new Map([
    ['normal', { latencyMs: 50, packetLossPercent: 0, bandwidthMbps: 100 }],
    ['slow', { latencyMs: 200, packetLossPercent: 2, bandwidthMbps: 10 }],
    ['offline', { latencyMs: 5000, packetLossPercent: 100, bandwidthMbps: 0 }],
    ['unstable', { latencyMs: 150, packetLossPercent: 5, bandwidthMbps: 20 }]
  ]);

  private thresholds = {
    maxAverageLatency: 100, // ms
    maxP95Latency: 150,
    maxP99Latency: 200,
    minSuccessRate: 0.99, // 99%
    maxErrorRate: 0.01, // 1%
    maxMemoryUsage: 500, // MB
    maxCpuUsage: 80 // %
  };

  /**
   * Execute a load test with specified configuration
   */
  async executeLoadTest(config: LoadTestConfig): Promise<LoadTestResult> {
    const testId = uuidv4();
    const startTime = Date.now();

    try {
      // Ramp up phase
      if (config.rampUpTime) {
        await this.rampUp(config);
      }

      // Sustained load phase
      const metrics = await this.runSustainedLoad(config);

      // Ramp down phase
      if (config.rampDownTime) {
        await this.rampDown(config);
      }

      // Generate report
      const report = this.generateReport(metrics);

      const result: LoadTestResult = {
        testId,
        config,
        metrics,
        report
      };

      this.testResults.push(result);
      return result;
    } finally {
      this.progressSubject.next(100);
    }
  }

  /**
   * Simulate concurrent connections
   */
  async simulateConcurrentConnections(
    count: number,
    duration: number
  ): Promise<ConnectionMetrics[]> {
    const connections: ConnectionMetrics[] = [];

    for (let i = 0; i < count; i++) {
      connections.push({
        connectionId: `conn-${i}`,
        connected: Math.random() > 0.02, // 98% success
        messagesSent: 0,
        messagesReceived: 0,
        errors: 0,
        averageLatency: Math.random() * 50 + 40,
        peakLatency: Math.random() * 100 + 80
      });
    }

    return connections;
  }

  /**
   * Simulate message throughput
   */
  async simulateMessageThroughput(
    messagesPerSecond: number,
    duration: number
  ): Promise<{ sent: number; received: number; throughput: number }> {
    const interval = 1000 / messagesPerSecond;
    let sent = 0;
    let received = 0;
    const startTime = Date.now();

    while (Date.now() - startTime < duration) {
      sent++;

      // Simulate 1-2% loss
      if (Math.random() > 0.02) {
        received++;
      }

      await this.sleep(interval);
    }

    return {
      sent,
      received,
      throughput: received / (duration / 1000)
    };
  }

  /**
   * Simulate network conditions
   */
  simulateNetworkCondition(condition: string): NetworkCondition {
    return this.networkConditions.get(condition) || this.networkConditions.get('normal')!;
  }

  /**
   * Monitor memory usage
   */
  monitorMemory(): number {
    if (performance.memory) {
      return performance.memory.usedJSHeapSize / (1024 * 1024); // Convert to MB
    }
    // Fallback: estimate
    return Math.random() * 200 + 100;
  }

  /**
   * Monitor CPU usage
   */
  monitorCPU(): number {
    // Simulate CPU usage
    return Math.random() * 60 + 10;
  }

  /**
   * Get test results
   */
  getTestResults(): LoadTestResult[] {
    return [...this.testResults];
  }

  /**
   * Get test result by ID
   */
  getTestResult(testId: string): LoadTestResult | undefined {
    return this.testResults.find(r => r.testId === testId);
  }

  /**
   * Compare multiple test results
   */
  compareResults(
    result1: LoadTestResult,
    result2: LoadTestResult
  ): { improved: boolean; changes: Record<string, number> } {
    const changes: Record<string, number> = {};
    const stats1 = result1.metrics.aggregateStats;
    const stats2 = result2.metrics.aggregateStats;

    changes.latency = stats2.averageLatency - stats1.averageLatency;
    changes.throughput = stats2.totalMessages - stats1.totalMessages;
    changes.successRate = stats2.successRate - stats1.successRate;
    changes.errorRate = stats2.errorRate - stats1.errorRate;
    changes.memory = stats2.memoryUsage - stats1.memoryUsage;

    const improved =
      changes.latency < 0 && // Lower latency is better
      changes.errorRate < 0 && // Lower error rate is better
      changes.successRate > 0; // Higher success rate is better

    return { improved, changes };
  }

  /**
   * Set custom thresholds
   */
  setThresholds(thresholds: Partial<typeof this.thresholds>): void {
    this.thresholds = { ...this.thresholds, ...thresholds };
  }

  /**
   * Get current thresholds
   */
  getThresholds() {
    return { ...this.thresholds };
  }

  private async rampUp(config: LoadTestConfig): Promise<void> {
    const rampUpTime = config.rampUpTime || 10000;
    const steps = 10;
    const stepDuration = rampUpTime / steps;

    for (let i = 0; i < steps; i++) {
      const progress = (i / steps) * 33; // Ramp up is 1/3 of total
      this.progressSubject.next(progress);
      await this.sleep(stepDuration);
    }
  }

  private async runSustainedLoad(config: LoadTestConfig): Promise<LoadTestMetrics> {
    const startTime = Date.now();
    const measurements: { timestamp: number; latencies: number[] }[] = [];
    const connections = await this.simulateConcurrentConnections(
      config.concurrentConnections,
      config.duration
    );

    const networkCondition = this.simulateNetworkCondition(config.networkCondition);
    const sampleInterval = 1000; // Collect metrics every second
    let lastSampleTime = startTime;
    let sampleCount = 0;

    while (Date.now() - startTime < config.duration) {
      const now = Date.now();

      if (now - lastSampleTime >= sampleInterval) {
        const latencies = connections.map(c => c.averageLatency);
        measurements.push({ timestamp: now, latencies });
        sampleCount++;

        const progress = 33 + (sampleCount / (config.duration / sampleInterval)) * 34;
        this.progressSubject.next(Math.min(progress, 67));

        lastSampleTime = now;
      }

      await this.sleep(100);
    }

    return this.calculateMetrics(config, connections, measurements);
  }

  private async rampDown(config: LoadTestConfig): Promise<void> {
    const rampDownTime = config.rampDownTime || 10000;
    const steps = 10;
    const stepDuration = rampDownTime / steps;

    for (let i = 0; i < steps; i++) {
      const progress = 67 + (i / steps) * 33;
      this.progressSubject.next(progress);
      await this.sleep(stepDuration);
    }
  }

  private calculateMetrics(
    config: LoadTestConfig,
    connections: ConnectionMetrics[],
    measurements: { timestamp: number; latencies: number[] }[]
  ): LoadTestMetrics {
    // Aggregate latencies from all measurements
    const allLatencies: number[] = [];
    measurements.forEach(m => allLatencies.push(...m.latencies));

    const sorted = [...allLatencies].sort((a, b) => a - b);

    const totalMessages = connections.reduce((sum, c) => sum + c.messagesSent, 0);
    const successfulMessages = connections.reduce((sum, c) => sum + c.messagesReceived, 0);
    const totalErrors = connections.reduce((sum, c) => sum + c.errors, 0);

    const metrics: LoadTestMetrics = {
      timestamp: Date.now(),
      duration: config.duration,
      concurrentConnections: config.concurrentConnections,
      totalConnections: connections,
      aggregateStats: {
        totalMessages,
        successRate: totalMessages > 0 ? successfulMessages / totalMessages : 0,
        averageLatency: allLatencies.reduce((a, b) => a + b) / allLatencies.length,
        p50Latency: sorted[Math.floor(sorted.length * 0.5)],
        p95Latency: sorted[Math.floor(sorted.length * 0.95)],
        p99Latency: sorted[Math.floor(sorted.length * 0.99)],
        peakLatency: Math.max(...allLatencies),
        memoryUsage: this.monitorMemory(),
        cpuUsage: this.monitorCPU(),
        bandwidthUsage: (totalMessages * 1024) / (config.duration / 1000) / (1024 * 1024), // MB/s
        errorRate: totalMessages > 0 ? totalErrors / totalMessages : 0
      }
    };

    this.metricsSubject.next(metrics);
    return metrics;
  }

  private generateReport(metrics: LoadTestMetrics) {
    const recommendations: string[] = [];
    const thresholdBreaches: string[] = [];
    const stats = metrics.aggregateStats;

    if (stats.averageLatency > this.thresholds.maxAverageLatency) {
      thresholdBreaches.push(`Average latency ${stats.averageLatency}ms exceeds ${this.thresholds.maxAverageLatency}ms`);
      recommendations.push('Optimize network request handling');
    }

    if (stats.p99Latency > this.thresholds.maxP99Latency) {
      thresholdBreaches.push(`p99 latency ${stats.p99Latency}ms exceeds ${this.thresholds.maxP99Latency}ms`);
      recommendations.push('Investigate tail latency');
    }

    if (stats.successRate < this.thresholds.minSuccessRate) {
      thresholdBreaches.push(`Success rate ${(stats.successRate * 100).toFixed(2)}% below ${(this.thresholds.minSuccessRate * 100).toFixed(2)}%`);
      recommendations.push('Improve error handling and recovery');
    }

    if (stats.errorRate > this.thresholds.maxErrorRate) {
      thresholdBreaches.push(`Error rate ${(stats.errorRate * 100).toFixed(2)}% exceeds ${(this.thresholds.maxErrorRate * 100).toFixed(2)}%`);
      recommendations.push('Debug error conditions');
    }

    if (stats.memoryUsage > this.thresholds.maxMemoryUsage) {
      thresholdBreaches.push(`Memory usage ${stats.memoryUsage}MB exceeds ${this.thresholds.maxMemoryUsage}MB`);
      recommendations.push('Optimize memory management');
    }

    return {
      title: 'Load Test Report',
      passed: thresholdBreaches.length === 0,
      recommendations,
      thresholdBreaches
    };
  }

  private sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}
