/**
 * Shared Performance Monitoring Library
 *
 * Real-time performance metrics collection and monitoring dashboard.
 *
 * Features:
 * - Latency tracking with percentile calculations (p50, p95, p99)
 * - Memory usage monitoring and leak detection
 * - Frame rate and rendering performance metrics
 * - Network throughput and bandwidth monitoring
 * - Automatic performance threshold alerts
 * - Historical metrics tracking for trend analysis
 *
 * Usage:
 * 1. Inject PerformanceService
 * 2. Record metrics: recordLatency(), recordMemory(), recordFPS()
 * 3. Subscribe to alerts or display dashboard
 *
 * Example:
 * ```typescript
 * constructor(private performance: PerformanceService) {}
 *
 * measureOperation() {
 *   const start = performance.now();
 *   // ... operation ...
 *   const latency = performance.now() - start;
 *   this.performance.recordLatency(latency);
 * }
 * ```
 */

export * from './lib/performance.service';
export * from './lib/performance-dashboard.component';
