import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../services/admin.service';
import { SystemHealth, ServiceHealth } from '../../models/admin.model';
import { Subject, takeUntil, interval, startWith, switchMap } from 'rxjs';

@Component({
  selector: 'app-system-health',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="health-page">
      <div class="page-header">
        <div class="header-left">
          <h2>System Health</h2>
          <span class="last-updated" *ngIf="health">
            Last updated: {{ health.timestamp | date:'medium' }}
          </span>
        </div>
        <div class="header-right">
          <div
            class="overall-status"
            [class.healthy]="getOverallStatus() === 'healthy'"
            [class.degraded]="getOverallStatus() === 'degraded'"
            [class.unhealthy]="getOverallStatus() === 'unhealthy'"
          >
            <span class="status-dot"></span>
            <span class="status-text">{{ getOverallStatus() | titlecase }}</span>
          </div>
          <button class="btn-refresh" (click)="refresh()">
            <span>🔄</span> Refresh
          </button>
        </div>
      </div>

      <!-- System Metrics -->
      <div class="metrics-section" *ngIf="health">
        <h3>Infrastructure Metrics</h3>
        <div class="metrics-grid">
          <div class="metric-card">
            <div class="metric-header">
              <span class="metric-icon">💻</span>
              <span class="metric-title">CPU Usage</span>
            </div>
            <div class="metric-value">
              <span class="value">{{ health.metrics.cpuUsage | number:'1.1-1' }}%</span>
              <div
                class="progress-bar"
                [class.warning]="health.metrics.cpuUsage > 70"
                [class.danger]="health.metrics.cpuUsage > 90"
              >
                <div class="progress" [style.width.%]="health.metrics.cpuUsage"></div>
              </div>
            </div>
          </div>

          <div class="metric-card">
            <div class="metric-header">
              <span class="metric-icon">🧠</span>
              <span class="metric-title">Memory Usage</span>
            </div>
            <div class="metric-value">
              <span class="value">{{ health.metrics.memoryUsage | number:'1.1-1' }}%</span>
              <div
                class="progress-bar"
                [class.warning]="health.metrics.memoryUsage > 70"
                [class.danger]="health.metrics.memoryUsage > 90"
              >
                <div class="progress" [style.width.%]="health.metrics.memoryUsage"></div>
              </div>
            </div>
          </div>

          <div class="metric-card">
            <div class="metric-header">
              <span class="metric-icon">💾</span>
              <span class="metric-title">Disk Usage</span>
            </div>
            <div class="metric-value">
              <span class="value">{{ health.metrics.diskUsage | number:'1.1-1' }}%</span>
              <div
                class="progress-bar"
                [class.warning]="health.metrics.diskUsage > 70"
                [class.danger]="health.metrics.diskUsage > 90"
              >
                <div class="progress" [style.width.%]="health.metrics.diskUsage"></div>
              </div>
            </div>
          </div>

          <div class="metric-card">
            <div class="metric-header">
              <span class="metric-icon">🌐</span>
              <span class="metric-title">Active Connections</span>
            </div>
            <div class="metric-value">
              <span class="value large">{{ health.metrics.activeConnections | number }}</span>
            </div>
          </div>

          <div class="metric-card">
            <div class="metric-header">
              <span class="metric-icon">📊</span>
              <span class="metric-title">Request Rate</span>
            </div>
            <div class="metric-value">
              <span class="value large">{{ health.metrics.requestsPerSecond | number:'1.0-0' }}/s</span>
            </div>
          </div>

          <div class="metric-card">
            <div class="metric-header">
              <span class="metric-icon">⏱️</span>
              <span class="metric-title">Avg Response Time</span>
            </div>
            <div class="metric-value">
              <span class="value large">{{ health.metrics.avgResponseTimeMs | number:'1.0-0' }}ms</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Services Grid -->
      <div class="services-section" *ngIf="health">
        <h3>Services ({{ health.services.length }})</h3>
        <div class="services-grid">
          <div
            *ngFor="let service of health.services"
            class="service-card"
            [class.up]="service.status === 'UP'"
            [class.down]="service.status === 'DOWN'"
            [class.degraded]="service.status === 'DEGRADED'"
          >
            <div class="service-header">
              <div class="service-status-indicator"></div>
              <span class="service-name">{{ service.name }}</span>
              <span class="service-badge">{{ service.status }}</span>
            </div>

            <div class="service-details">
              <div class="detail">
                <span class="label">Uptime</span>
                <span class="value">{{ service.uptime | number:'1.2-2' }}%</span>
              </div>
              <div class="detail">
                <span class="label">Response</span>
                <span class="value">{{ service.responseTimeMs }}ms</span>
              </div>
              <div class="detail">
                <span class="label">Instances</span>
                <span class="value">{{ service.instances || 1 }}</span>
              </div>
            </div>

            <div class="service-endpoint" *ngIf="service.endpoint">
              <span class="endpoint">{{ service.endpoint }}</span>
            </div>

            <div class="service-error" *ngIf="service.lastError">
              <span class="error-icon">⚠️</span>
              <span class="error-text">{{ service.lastError }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Database Health -->
      <div class="database-section" *ngIf="health">
        <h3>Database Connections</h3>
        <div class="db-grid">
          <div class="db-card">
            <div class="db-header">
              <span class="db-icon">🐘</span>
              <span class="db-name">PostgreSQL</span>
              <span class="db-status up">Connected</span>
            </div>
            <div class="db-stats">
              <div class="db-stat">
                <span class="label">Active</span>
                <span class="value">{{ health.metrics.activeConnections }}</span>
              </div>
              <div class="db-stat">
                <span class="label">Pool Size</span>
                <span class="value">100</span>
              </div>
              <div class="db-stat">
                <span class="label">Idle</span>
                <span class="value">{{ 100 - health.metrics.activeConnections }}</span>
              </div>
            </div>
          </div>

          <div class="db-card">
            <div class="db-header">
              <span class="db-icon">🔴</span>
              <span class="db-name">Redis Cache</span>
              <span class="db-status up">Connected</span>
            </div>
            <div class="db-stats">
              <div class="db-stat">
                <span class="label">Hit Rate</span>
                <span class="value">94.2%</span>
              </div>
              <div class="db-stat">
                <span class="label">Memory</span>
                <span class="value">2.1 GB</span>
              </div>
              <div class="db-stat">
                <span class="label">Keys</span>
                <span class="value">125K</span>
              </div>
            </div>
          </div>

          <div class="db-card">
            <div class="db-header">
              <span class="db-icon">📨</span>
              <span class="db-name">Apache Kafka</span>
              <span class="db-status up">Connected</span>
            </div>
            <div class="db-stats">
              <div class="db-stat">
                <span class="label">Topics</span>
                <span class="value">12</span>
              </div>
              <div class="db-stat">
                <span class="label">Partitions</span>
                <span class="value">36</span>
              </div>
              <div class="db-stat">
                <span class="label">Lag</span>
                <span class="value">0</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Loading State -->
      <div class="loading" *ngIf="loading">
        <div class="spinner"></div>
        <span>Loading system health...</span>
      </div>
    </div>
  `,
  styles: [`
    .health-page {
      max-width: 1400px;
      margin: 0 auto;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }

    .header-left h2 {
      margin: 0;
      color: #1a237e;
    }

    .last-updated {
      color: #666;
      font-size: 14px;
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .overall-status {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 16px;
      border-radius: 20px;
      background: #e0e0e0;
    }

    .overall-status.healthy {
      background: #e8f5e9;
    }

    .overall-status.degraded {
      background: #fff3e0;
    }

    .overall-status.unhealthy {
      background: #ffebee;
    }

    .status-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background: #9e9e9e;
    }

    .overall-status.healthy .status-dot { background: #4caf50; }
    .overall-status.degraded .status-dot { background: #ff9800; }
    .overall-status.unhealthy .status-dot { background: #f44336; }

    .status-text {
      font-weight: 600;
      font-size: 14px;
    }

    .overall-status.healthy .status-text { color: #2e7d32; }
    .overall-status.degraded .status-text { color: #e65100; }
    .overall-status.unhealthy .status-text { color: #c62828; }

    .btn-refresh {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 16px;
      background: white;
      border: 1px solid #ddd;
      border-radius: 8px;
      cursor: pointer;
      font-weight: 500;
      transition: all 0.2s;
    }

    .btn-refresh:hover {
      background: #f5f5f5;
      border-color: #1a237e;
    }

    .metrics-section,
    .services-section,
    .database-section {
      background: white;
      border-radius: 12px;
      padding: 24px;
      margin-bottom: 24px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    .metrics-section h3,
    .services-section h3,
    .database-section h3 {
      margin: 0 0 20px 0;
      color: #333;
      font-size: 18px;
    }

    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
    }

    .metric-card {
      background: #f5f7fa;
      border-radius: 8px;
      padding: 16px;
    }

    .metric-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
    }

    .metric-icon {
      font-size: 20px;
    }

    .metric-title {
      font-size: 14px;
      color: #666;
    }

    .metric-value .value {
      font-size: 24px;
      font-weight: 700;
      color: #1a237e;
    }

    .metric-value .value.large {
      font-size: 28px;
    }

    .progress-bar {
      height: 8px;
      background: #e0e0e0;
      border-radius: 4px;
      margin-top: 8px;
      overflow: hidden;
    }

    .progress-bar .progress {
      height: 100%;
      background: #4caf50;
      border-radius: 4px;
      transition: width 0.3s ease;
    }

    .progress-bar.warning .progress { background: #ff9800; }
    .progress-bar.danger .progress { background: #f44336; }

    .services-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 16px;
    }

    .service-card {
      background: #f5f7fa;
      border-radius: 8px;
      padding: 16px;
      border-left: 4px solid #9e9e9e;
    }

    .service-card.up { border-left-color: #4caf50; }
    .service-card.down { border-left-color: #f44336; background: #ffebee; }
    .service-card.degraded { border-left-color: #ff9800; background: #fff3e0; }

    .service-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 12px;
    }

    .service-status-indicator {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background: #9e9e9e;
    }

    .service-card.up .service-status-indicator { background: #4caf50; }
    .service-card.down .service-status-indicator { background: #f44336; }
    .service-card.degraded .service-status-indicator { background: #ff9800; }

    .service-name {
      flex: 1;
      font-weight: 600;
      color: #333;
    }

    .service-badge {
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 600;
      background: #e0e0e0;
      color: #666;
    }

    .service-card.up .service-badge { background: #c8e6c9; color: #2e7d32; }
    .service-card.down .service-badge { background: #ffcdd2; color: #c62828; }
    .service-card.degraded .service-badge { background: #ffe0b2; color: #e65100; }

    .service-details {
      display: flex;
      gap: 16px;
      margin-bottom: 8px;
    }

    .service-details .detail {
      display: flex;
      flex-direction: column;
    }

    .service-details .label {
      font-size: 11px;
      color: #999;
    }

    .service-details .value {
      font-size: 14px;
      font-weight: 600;
      color: #333;
    }

    .service-endpoint {
      padding-top: 8px;
      border-top: 1px solid #e0e0e0;
    }

    .endpoint {
      font-family: monospace;
      font-size: 12px;
      color: #666;
    }

    .service-error {
      display: flex;
      align-items: flex-start;
      gap: 6px;
      margin-top: 8px;
      padding: 8px;
      background: rgba(244, 67, 54, 0.1);
      border-radius: 4px;
    }

    .error-icon {
      font-size: 14px;
    }

    .error-text {
      font-size: 12px;
      color: #c62828;
    }

    .db-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 16px;
    }

    .db-card {
      background: #f5f7fa;
      border-radius: 8px;
      padding: 16px;
    }

    .db-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 12px;
    }

    .db-icon {
      font-size: 24px;
    }

    .db-name {
      flex: 1;
      font-weight: 600;
      color: #333;
    }

    .db-status {
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 600;
    }

    .db-status.up {
      background: #c8e6c9;
      color: #2e7d32;
    }

    .db-status.down {
      background: #ffcdd2;
      color: #c62828;
    }

    .db-stats {
      display: flex;
      gap: 20px;
    }

    .db-stat {
      display: flex;
      flex-direction: column;
    }

    .db-stat .label {
      font-size: 11px;
      color: #999;
    }

    .db-stat .value {
      font-size: 16px;
      font-weight: 600;
      color: #1a237e;
    }

    .loading {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 48px;
      color: #666;
    }

    .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid #e0e0e0;
      border-top-color: #1a237e;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin-bottom: 16px;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `],
})
export class SystemHealthComponent implements OnInit, OnDestroy {
  health: SystemHealth | null = null;
  loading = true;
  private destroy$ = new Subject<void>();

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.startPolling();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  startPolling(): void {
    interval(15000)
      .pipe(
        startWith(0),
        switchMap(() => this.adminService.getSystemHealth()),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (data) => {
          this.health = data;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        },
      });
  }

  refresh(): void {
    this.loading = true;
    this.adminService
      .getSystemHealth()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.health = data;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        },
      });
  }

  getOverallStatus(): string {
    if (!this.health) return 'unknown';
    return this.health.overallStatus;
  }
}
