import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * TEAM 6: Performance Monitoring Service
 *
 * Real-time performance tracking and analysis for measure builder
 * Monitors rendering, state updates, CQL generation, and user interactions
 */

export interface PerformanceMetric {
  name: string;
  duration: number;
  timestamp: Date;
  category: 'render' | 'state' | 'cql' | 'interaction' | 'export';
}

export interface PerformanceStats {
  category: string;
  count: number;
  totalTime: number;
  avgTime: number;
  minTime: number;
  maxTime: number;
  p95: number;
  p99: number;
}

export interface OptimizationOpportunity {
  severity: 'critical' | 'warning' | 'info';
  metric: string;
  currentValue: number;
  targetValue: number;
  recommendation: string;
}

@Injectable({
  providedIn: 'root'
})
export class PerformanceMonitoringService {
  private metricsSubject = new BehaviorSubject<PerformanceMetric[]>([]);
  private statsSubject = new BehaviorSubject<Map<string, PerformanceStats>>(new Map());
  private opportunitiesSubject = new BehaviorSubject<OptimizationOpportunity[]>([]);

  public metrics$ = this.metricsSubject.asObservable();
  public stats$ = this.statsSubject.asObservable();
  public opportunities$ = this.opportunitiesSubject.asObservable();

  // Performance budgets (thresholds)
  private performanceBudgets = {
    'render-50-blocks': 30,       // 30ms
    'render-100-blocks': 50,      // 50ms
    'render-200-blocks': 100,     // 100ms
    'slider-update': 5,           // 5ms
    'cql-generation': 200,        // 200ms
    'state-update': 5,            // 5ms
    'full-workflow': 500,         // 500ms
    'export': 300                 // 300ms
  };

  constructor() {}

  /**
   * Record performance metric
   */
  recordMetric(
    name: string,
    duration: number,
    category: 'render' | 'state' | 'cql' | 'interaction' | 'export'
  ): void {
    const metric: PerformanceMetric = {
      name,
      duration,
      timestamp: new Date(),
      category
    };

    const currentMetrics = this.metricsSubject.value;
    this.metricsSubject.next([...currentMetrics, metric]);

    // Keep only last 1000 metrics to prevent memory buildup
    if (currentMetrics.length > 1000) {
      this.metricsSubject.next(currentMetrics.slice(-1000));
    }

    // Update statistics and check for optimization opportunities
    this.updateStats();
    this.detectOptimizationOpportunities();
  }

  /**
   * Start performance measurement
   */
  startMeasure(name: string): () => void {
    const startTime = performance.now();

    return () => {
      const endTime = performance.now();
      const duration = endTime - startTime;

      // Infer category from metric name
      let category: 'render' | 'state' | 'cql' | 'interaction' | 'export';
      if (name.includes('render')) {
        category = 'render';
      } else if (name.includes('cql')) {
        category = 'cql';
      } else if (name.includes('state') || name.includes('update')) {
        category = 'state';
      } else if (name.includes('export')) {
        category = 'export';
      } else {
        category = 'interaction';
      }

      this.recordMetric(name, duration, category);
    };
  }

  /**
   * Update performance statistics
   */
  private updateStats(): void {
    const metrics = this.metricsSubject.value;
    const statsMap = new Map<string, PerformanceMetric[]>();

    // Group metrics by category
    metrics.forEach(metric => {
      if (!statsMap.has(metric.category)) {
        statsMap.set(metric.category, []);
      }
      statsMap.get(metric.category)!.push(metric);
    });

    // Calculate statistics for each category
    const stats = new Map<string, PerformanceStats>();

    statsMap.forEach((categoryMetrics, category) => {
      const durations = categoryMetrics
        .map(m => m.duration)
        .sort((a, b) => a - b);

      const totalTime = durations.reduce((a, b) => a + b, 0);
      const avgTime = totalTime / durations.length;
      const minTime = durations[0];
      const maxTime = durations[durations.length - 1];
      const p95 = durations[Math.floor(durations.length * 0.95)];
      const p99 = durations[Math.floor(durations.length * 0.99)];

      stats.set(category, {
        category,
        count: durations.length,
        totalTime,
        avgTime,
        minTime,
        maxTime,
        p95,
        p99
      });
    });

    this.statsSubject.next(stats);
  }

  /**
   * Detect optimization opportunities
   */
  private detectOptimizationOpportunities(): void {
    const opportunities: OptimizationOpportunity[] = [];
    const metrics = this.metricsSubject.value;
    const stats = this.statsSubject.value;

    // Analyze each metric category
    stats.forEach((categoryStats, category) => {
      // Check if average exceeds budget
      const budgetKey = `${category}-${categoryStats.count}`;
      let budget = this.performanceBudgets[budgetKey as keyof typeof this.performanceBudgets];

      if (!budget && category === 'render') {
        // Estimate budget based on block count from metric name
        budget = 100;
      } else if (!budget) {
        budget = 200;
      }

      if (categoryStats.avgTime > budget) {
        opportunities.push({
          severity: categoryStats.avgTime > budget * 2 ? 'critical' : 'warning',
          metric: category,
          currentValue: categoryStats.avgTime,
          targetValue: budget,
          recommendation: this.getOptimizationRecommendation(category)
        });
      }

      // Check for outliers (P99 significantly higher than average)
      if (categoryStats.p99 > categoryStats.avgTime * 3) {
        opportunities.push({
          severity: 'warning',
          metric: `${category}-p99`,
          currentValue: categoryStats.p99,
          targetValue: categoryStats.avgTime * 2,
          recommendation: `High variability in ${category} performance. Investigate outliers.`
        });
      }
    });

    this.opportunitiesSubject.next(opportunities);
  }

  /**
   * Get optimization recommendation for metric
   */
  private getOptimizationRecommendation(metric: string): string {
    const recommendations: Record<string, string> = {
      'render': 'Consider using virtual scrolling for large block lists or Canvas rendering instead of SVG',
      'state': 'Optimize state immutability pattern or consider using structural sharing libraries',
      'cql': 'Cache CQL generation results or use memoization for frequently used patterns',
      'interaction': 'Debounce event handlers or use requestAnimationFrame for smooth updates',
      'export': 'Optimize JSON serialization or use streaming for large exports'
    };

    return recommendations[metric] || 'Review implementation for bottlenecks';
  }

  /**
   * Get all metrics
   */
  getMetrics(): PerformanceMetric[] {
    return this.metricsSubject.value;
  }

  /**
   * Get metrics for specific category
   */
  getMetricsByCategory(category: string): PerformanceMetric[] {
    return this.metricsSubject.value.filter(m => m.category === category);
  }

  /**
   * Get statistics
   */
  getStats(): Map<string, PerformanceStats> {
    return this.statsSubject.value;
  }

  /**
   * Get optimization opportunities
   */
  getOpportunities(): OptimizationOpportunity[] {
    return this.opportunitiesSubject.value;
  }

  /**
   * Generate performance report
   */
  generateReport(): string {
    const stats = this.statsSubject.value;
    const opportunities = this.opportunitiesSubject.value;

    let report = `\n${'='.repeat(80)}\nPERFORMANCE MONITORING REPORT\n${'='.repeat(80)}\n\n`;

    // Statistics by category
    report += `PERFORMANCE STATISTICS\n${'─'.repeat(80)}\n`;
    stats.forEach((stat, category) => {
      report += `\n${category.toUpperCase()}\n`;
      report += `  Count: ${stat.count}\n`;
      report += `  Avg: ${stat.avgTime.toFixed(2)}ms | Min: ${stat.minTime.toFixed(2)}ms | Max: ${stat.maxTime.toFixed(2)}ms\n`;
      report += `  P95: ${stat.p95.toFixed(2)}ms | P99: ${stat.p99.toFixed(2)}ms\n`;
    });

    // Optimization opportunities
    if (opportunities.length > 0) {
      report += `\n${'─'.repeat(80)}\nOPTIMIZATION OPPORTUNITIES\n${'─'.repeat(80)}\n`;

      const critical = opportunities.filter(o => o.severity === 'critical');
      const warnings = opportunities.filter(o => o.severity === 'warning');
      const info = opportunities.filter(o => o.severity === 'info');

      if (critical.length > 0) {
        report += `\n🔴 CRITICAL (${critical.length})\n`;
        critical.forEach(opp => {
          report += `  ${opp.metric}: ${opp.currentValue.toFixed(2)}ms (target: ${opp.targetValue.toFixed(2)}ms)\n`;
          report += `    → ${opp.recommendation}\n`;
        });
      }

      if (warnings.length > 0) {
        report += `\n🟡 WARNING (${warnings.length})\n`;
        warnings.forEach(opp => {
          report += `  ${opp.metric}: ${opp.currentValue.toFixed(2)}ms (target: ${opp.targetValue.toFixed(2)}ms)\n`;
          report += `    → ${opp.recommendation}\n`;
        });
      }

      if (info.length > 0) {
        report += `\n🔵 INFO (${info.length})\n`;
        info.forEach(opp => {
          report += `  ${opp.metric}: ${opp.recommendation}\n`;
        });
      }
    } else {
      report += `\n✅ No optimization opportunities detected.\n`;
    }

    report += `\n${'='.repeat(80)}\n`;

    return report;
  }

  /**
   * Reset all metrics and statistics
   */
  reset(): void {
    this.metricsSubject.next([]);
    this.statsSubject.next(new Map());
    this.opportunitiesSubject.next([]);
  }

  /**
   * Export metrics as JSON
   */
  exportMetrics(): string {
    const metrics = this.metricsSubject.value;
    const stats = this.statsSubject.value;

    return JSON.stringify(
      {
        metrics,
        stats: Array.from(stats.entries()).reduce((acc, [key, value]) => {
          acc[key] = value;
          return acc;
        }, {} as Record<string, PerformanceStats>),
        exportedAt: new Date().toISOString()
      },
      null,
      2
    );
  }

  /**
   * Check if performance is within budget
   */
  isWithinBudget(metricName: string): boolean {
    const metric = this.performanceBudgets[metricName as keyof typeof this.performanceBudgets];
    const relevantMetrics = this.metricsSubject.value.filter(m => m.name === metricName);

    if (relevantMetrics.length === 0) {
      return true;
    }

    const avgTime = relevantMetrics.reduce((sum, m) => sum + m.duration, 0) / relevantMetrics.length;
    return metric ? avgTime <= metric : true;
  }

  /**
   * Get performance summary
   */
  getSummary(): { metricsCount: number; categoriesMonitored: number; opportunitiesFound: number } {
    return {
      metricsCount: this.metricsSubject.value.length,
      categoriesMonitored: this.statsSubject.value.size,
      opportunitiesFound: this.opportunitiesSubject.value.length
    };
  }
}
