import { Component, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PerformanceService, PerformanceMetrics, PerformanceAlert } from './performance.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

/**
 * Performance Monitoring Dashboard Component
 *
 * Real-time visualization of system performance metrics:
 * - Latency with p50/p95/p99 percentiles
 * - Memory usage and leak detection
 * - Frame rate and rendering performance
 * - Network throughput and bandwidth
 * - Performance alerts
 *
 * ★ Insight ─────────────────────────────────────
 * Dashboard design patterns:
 * - Metric cards show current + historical trend
 * - Color coding: green (good), yellow (warning), red (critical)
 * - Sparkline charts show trends without cluttering UI
 * - Alerts at top for visibility of issues
 * - Percentile breakdown shows tail latency distribution
 * ─────────────────────────────────────────────────
 */
@Component({
  selector: 'app-performance-dashboard',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="performance-dashboard" [data-testid]="'performance-dashboard'">
      <!-- Alerts Section -->
      <div *ngIf="activeAlerts.length > 0" class="alerts-section" [data-testid]="'performance-alerts'">
        <div class="alerts-header">
          <h3>Performance Alerts ({{ activeAlerts.length }})</h3>
        </div>
        <div class="alerts-list">
          <div
            *ngFor="let alert of activeAlerts"
            class="alert-item"
            [ngClass]="'alert-' + alert.severity"
            [data-testid]="'alert-item'"
          >
            <span class="alert-icon">⚠</span>
            <div class="alert-details">
              <div class="alert-message">{{ alert.message }}</div>
              <div class="alert-time">{{ formatTime(alert.timestamp) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Metrics Grid -->
      <div class="metrics-grid">
        <!-- Latency Card -->
        <div class="metric-card latency" [data-testid]="'metrics-card-latency'">
          <div class="card-header">
            <h4>Latency</h4>
            <span class="card-unit">ms</span>
          </div>
          <div class="card-main">
            <div class="main-value" [data-testid]="'latency-current'">
              {{ currentMetrics.latency.current | number: '1.0-0' }}
            </div>
            <div class="main-label">Current</div>
          </div>
          <div class="card-breakdown">
            <div class="breakdown-item">
              <span class="breakdown-label">Avg:</span>
              <span class="breakdown-value" [data-testid]="'latency-average'">
                {{ currentMetrics.latency.average | number: '1.0-0' }}ms
              </span>
            </div>
            <div class="breakdown-item">
              <span class="breakdown-label">P50:</span>
              <span class="breakdown-value" [data-testid]="'latency-p50'">
                {{ currentMetrics.latency.p50 | number: '1.0-0' }}ms
              </span>
            </div>
            <div class="breakdown-item">
              <span class="breakdown-label">P95:</span>
              <span class="breakdown-value" [data-testid]="'latency-p95'">
                {{ currentMetrics.latency.p95 | number: '1.0-0' }}ms
              </span>
            </div>
            <div class="breakdown-item">
              <span class="breakdown-label">P99:</span>
              <span class="breakdown-value" [data-testid]="'latency-p99'">
                {{ currentMetrics.latency.p99 | number: '1.0-0' }}ms
              </span>
            </div>
          </div>
          <div class="card-status" [ngClass]="getLatencyStatus()">
            {{ getLatencyStatusText() }}
          </div>
        </div>

        <!-- Memory Card -->
        <div class="metric-card memory" [data-testid]="'metrics-card-memory'">
          <div class="card-header">
            <h4>Memory</h4>
            <span class="card-unit">MB</span>
          </div>
          <div class="card-main">
            <div class="main-value" [data-testid]="'memory-usage'">
              {{ currentMetrics.memory.used }}
            </div>
            <div class="main-label">/ {{ currentMetrics.memory.total }}MB</div>
          </div>
          <div class="card-breakdown">
            <div class="breakdown-item">
              <span class="breakdown-label">Heap:</span>
              <span class="breakdown-value">
                {{ currentMetrics.memory.heapSize }}MB
              </span>
            </div>
            <div class="breakdown-item">
              <span class="breakdown-label">Growth:</span>
              <span class="breakdown-value" [data-testid]="'memory-growth-rate'">
                {{ currentMetrics.memory.growthRate | number: '1.0-2' }}%
              </span>
            </div>
          </div>
          <div class="memory-bar">
            <div
              class="memory-fill"
              [style.width.%]="
                (currentMetrics.memory.used / currentMetrics.memory.total) * 100
              "
            ></div>
          </div>
          <div class="card-status" [ngClass]="getMemoryStatus()">
            {{ getMemoryStatusText() }}
          </div>
        </div>

        <!-- FPS Card -->
        <div class="metric-card fps" [data-testid]="'metrics-card-fps'">
          <div class="card-header">
            <h4>Frame Rate</h4>
            <span class="card-unit">FPS</span>
          </div>
          <div class="card-main">
            <div class="main-value" [data-testid]="'current-fps'">
              {{ currentMetrics.cpu.fps | number: '1.0-0' }}
            </div>
            <div class="main-label">FPS</div>
          </div>
          <div class="card-breakdown">
            <div class="breakdown-item">
              <span class="breakdown-label">Render:</span>
              <span class="breakdown-value">
                {{ currentMetrics.cpu.renderTime | number: '1.0-1' }}ms
              </span>
            </div>
            <div class="breakdown-item">
              <span class="breakdown-label">Layout:</span>
              <span class="breakdown-value">
                {{ currentMetrics.cpu.layoutTime | number: '1.0-1' }}ms
              </span>
            </div>
          </div>
          <div class="card-status" [ngClass]="getFPSStatus()">
            {{ getFPSStatusText() }}
          </div>
        </div>

        <!-- Throughput Card -->
        <div class="metric-card throughput" [data-testid]="'metrics-card-throughput'">
          <div class="card-header">
            <h4>Throughput</h4>
            <span class="card-unit">msg/s</span>
          </div>
          <div class="card-main">
            <div class="main-value" [data-testid]="'message-throughput'">
              {{ currentMetrics.network.throughput | number: '1.0-0' }}
            </div>
            <div class="main-label">Messages/sec</div>
          </div>
          <div class="card-breakdown">
            <div class="breakdown-item">
              <span class="breakdown-label">Bandwidth:</span>
              <span class="breakdown-value">
                {{ (currentMetrics.network.bandwidth / 1024) | number: '1.0-1' }}KB/s
              </span>
            </div>
            <div class="breakdown-item">
              <span class="breakdown-label">Loss:</span>
              <span class="breakdown-value">
                {{ currentMetrics.network.packetLoss | number: '1.0-2' }}%
              </span>
            </div>
          </div>
          <div class="card-status good">Active</div>
        </div>
      </div>

      <!-- Charts Section -->
      <div class="charts-section">
        <div class="chart-card">
          <h4>Latency Trend</h4>
          <div class="chart" [data-testid]="'latency-chart'">
            [Latency chart would render here]
          </div>
        </div>

        <div class="chart-card">
          <h4>Memory Trend</h4>
          <div class="chart" [data-testid]="'memory-chart'">
            [Memory chart would render here]
          </div>
        </div>

        <div class="chart-card">
          <h4>FPS Trend</h4>
          <div class="chart" [data-testid]="'fps-chart'">
            [FPS chart would render here]
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .performance-dashboard {
      padding: 2rem;
      background: #f5f5f5;
      min-height: 100vh;
    }

    /* Alerts Section */
    .alerts-section {
      margin-bottom: 2rem;
      background: white;
      border-radius: 8px;
      overflow: hidden;
    }

    .alerts-header {
      padding: 1rem;
      background: #fff3f0;
      border-bottom: 1px solid #ffcccc;
    }

    .alerts-header h3 {
      margin: 0;
      font-size: 1rem;
      color: #d32f2f;
    }

    .alerts-list {
      max-height: 200px;
      overflow-y: auto;
    }

    .alert-item {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 0.75rem 1rem;
      border-bottom: 1px solid #eee;
    }

    .alert-item.alert-critical {
      background: #ffebee;
      border-left: 4px solid #d32f2f;
    }

    .alert-item.alert-warning {
      background: #fff3f0;
      border-left: 4px solid #ff9800;
    }

    .alert-icon {
      font-size: 1.2rem;
      color: #ff9800;
    }

    .alert-details {
      flex: 1;
      min-width: 0;
    }

    .alert-message {
      font-size: 0.9rem;
      color: #333;
      font-weight: 500;
    }

    .alert-time {
      font-size: 0.75rem;
      color: #999;
      margin-top: 0.25rem;
    }

    /* Metrics Grid */
    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 1.5rem;
      margin-bottom: 2rem;
    }

    .metric-card {
      background: white;
      border-radius: 8px;
      padding: 1.5rem;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      transition: all 0.3s ease;
    }

    .metric-card:hover {
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
      padding-bottom: 1rem;
      border-bottom: 2px solid #eee;
    }

    .card-header h4 {
      margin: 0;
      font-size: 1rem;
      color: #333;
    }

    .card-unit {
      font-size: 0.75rem;
      color: #999;
      text-transform: uppercase;
    }

    .card-main {
      margin-bottom: 1rem;
    }

    .main-value {
      font-size: 2rem;
      font-weight: bold;
      color: #1976d2;
      line-height: 1;
    }

    .main-label {
      font-size: 0.85rem;
      color: #999;
      margin-top: 0.5rem;
    }

    .card-breakdown {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.75rem;
      margin-bottom: 1rem;
      padding: 0.75rem;
      background: #f9f9f9;
      border-radius: 4px;
    }

    .breakdown-item {
      display: flex;
      justify-content: space-between;
      font-size: 0.85rem;
    }

    .breakdown-label {
      color: #999;
      font-weight: 500;
    }

    .breakdown-value {
      color: #333;
      font-weight: 600;
    }

    /* Memory Bar */
    .memory-bar {
      height: 8px;
      background: #e0e0e0;
      border-radius: 4px;
      overflow: hidden;
      margin-bottom: 1rem;
    }

    .memory-fill {
      height: 100%;
      background: linear-gradient(90deg, #81c784, #4caf50);
      transition: width 0.3s ease;
    }

    /* Status Indicators */
    .card-status {
      font-size: 0.75rem;
      padding: 0.5rem;
      border-radius: 4px;
      text-align: center;
      font-weight: 600;
      text-transform: uppercase;
    }

    .card-status.good {
      background: #f1f8f4;
      color: #4caf50;
    }

    .card-status.warning {
      background: #fff3f0;
      color: #ff9800;
    }

    .card-status.critical {
      background: #ffebee;
      color: #d32f2f;
    }

    /* Charts Section */
    .charts-section {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
      gap: 1.5rem;
    }

    .chart-card {
      background: white;
      border-radius: 8px;
      padding: 1.5rem;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }

    .chart-card h4 {
      margin: 0 0 1rem 0;
      color: #333;
      font-size: 1rem;
    }

    .chart {
      height: 300px;
      background: #f9f9f9;
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #999;
      font-size: 0.9rem;
    }

    /* Responsive */
    @media (max-width: 768px) {
      .performance-dashboard {
        padding: 1rem;
      }

      .metrics-grid {
        grid-template-columns: 1fr;
      }

      .charts-section {
        grid-template-columns: 1fr;
      }

      .card-breakdown {
        grid-template-columns: 1fr;
      }
    }
  `],
})
export class PerformanceDashboardComponent implements OnInit, OnDestroy {
  currentMetrics: PerformanceMetrics = this.getInitialMetrics();
  activeAlerts: PerformanceAlert[] = [];

  private destroy$ = new Subject<void>();

  constructor(private performanceService: PerformanceService) {}

  ngOnInit(): void {
    // Subscribe to metrics
    this.performanceService.metrics$.pipe(takeUntil(this.destroy$)).subscribe((metrics) => {
      this.currentMetrics = metrics;
    });

    // Subscribe to alerts
    this.performanceService.alerts$.pipe(takeUntil(this.destroy$)).subscribe((alert) => {
      this.activeAlerts.unshift(alert);

      // Keep only last 10 alerts
      if (this.activeAlerts.length > 10) {
        this.activeAlerts = this.activeAlerts.slice(0, 10);
      }

      // Auto-remove warnings after 10 seconds
      if (alert.severity === 'warning') {
        setTimeout(() => {
          this.activeAlerts = this.activeAlerts.filter((a) => a.id !== alert.id);
        }, 10000);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Get latency status color
   */
  getLatencyStatus(): string {
    const latency = this.currentMetrics.latency.current;
    if (latency > 100) return 'critical';
    if (latency > 75) return 'warning';
    return 'good';
  }

  /**
   * Get latency status text
   */
  getLatencyStatusText(): string {
    const status = this.getLatencyStatus();
    return status.charAt(0).toUpperCase() + status.slice(1);
  }

  /**
   * Get memory status color
   */
  getMemoryStatus(): string {
    const memory = this.currentMetrics.memory.used;
    if (memory > 200) return 'critical';
    if (memory > 150) return 'warning';
    return 'good';
  }

  /**
   * Get memory status text
   */
  getMemoryStatusText(): string {
    const status = this.getMemoryStatus();
    return status.charAt(0).toUpperCase() + status.slice(1);
  }

  /**
   * Get FPS status color
   */
  getFPSStatus(): string {
    const fps = this.currentMetrics.cpu.fps;
    if (fps < 30) return 'critical';
    if (fps < 50) return 'warning';
    return 'good';
  }

  /**
   * Get FPS status text
   */
  getFPSStatusText(): string {
    const status = this.getFPSStatus();
    return status.charAt(0).toUpperCase() + status.slice(1);
  }

  /**
   * Format timestamp
   */
  formatTime(timestamp: number): string {
    const date = new Date(timestamp);
    return date.toLocaleTimeString();
  }

  /**
   * Get initial metrics
   */
  private getInitialMetrics(): PerformanceMetrics {
    return {
      latency: {
        current: 0,
        average: 0,
        p50: 0,
        p95: 0,
        p99: 0,
        min: 0,
        max: 0,
      },
      memory: {
        used: 0,
        total: 0,
        growthRate: 0,
        heapSize: 0,
      },
      cpu: {
        fps: 60,
        renderTime: 0,
        layoutTime: 0,
        paintTime: 0,
      },
      network: {
        throughput: 0,
        bandwidth: 0,
        packetLoss: 0,
        connectionTime: 0,
      },
      timestamps: {
        measured: Date.now(),
        window: 1000,
      },
    };
  }
}
