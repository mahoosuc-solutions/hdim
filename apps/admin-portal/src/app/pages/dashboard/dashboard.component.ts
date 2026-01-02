import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../services/admin.service';
import { DashboardSnapshot, ServiceStatus, SystemAlert } from '../../models/admin.model';
import { Subject, takeUntil, interval, switchMap, startWith } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dashboard">
      <div class="dashboard-header">
        <h2>System Dashboard</h2>
        <span class="last-updated" *ngIf="dashboard">
          Last updated: {{ dashboard.timestamp | date:'medium' }}
        </span>
      </div>

      <!-- Metrics Cards -->
      <div class="metrics-grid" *ngIf="dashboard">
        <div class="metric-card">
          <div class="metric-icon patients">👥</div>
          <div class="metric-content">
            <span class="metric-value">{{ dashboard.metrics.registeredPatients | number }}</span>
            <span class="metric-label">Registered Patients</span>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon users">👤</div>
          <div class="metric-content">
            <span class="metric-value">{{ dashboard.metrics.activeUsers }}</span>
            <span class="metric-label">Active Users</span>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon evaluations">📋</div>
          <div class="metric-content">
            <span class="metric-value">{{ dashboard.metrics.evaluationsToday }}</span>
            <span class="metric-label">Evaluations Today</span>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon gaps">⚠️</div>
          <div class="metric-content">
            <span class="metric-value">{{ dashboard.metrics.careGapsOpen | number }}</span>
            <span class="metric-label">Open Care Gaps</span>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon latency">⚡</div>
          <div class="metric-content">
            <span class="metric-value">{{ dashboard.metrics.patientReadLatencyP95Ms }}ms</span>
            <span class="metric-label">P95 Latency</span>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon cache">💾</div>
          <div class="metric-content">
            <span class="metric-value">{{ (dashboard.metrics.cacheHitRate * 100) | number:'1.1-1' }}%</span>
            <span class="metric-label">Cache Hit Rate</span>
          </div>
        </div>
      </div>

      <!-- Services Status -->
      <div class="section">
        <h3>Service Status</h3>
        <div class="services-grid" *ngIf="dashboard">
          <div
            *ngFor="let service of dashboard.services"
            class="service-card"
            [class.up]="service.status === 'UP'"
            [class.down]="service.status === 'DOWN'"
            [class.degraded]="service.status === 'DEGRADED'"
          >
            <div class="service-status-indicator"></div>
            <div class="service-info">
              <span class="service-name">{{ service.name }}</span>
              <span class="service-uptime">{{ service.uptime }}% uptime</span>
            </div>
            <span class="service-status-badge">{{ service.status }}</span>
          </div>
        </div>
      </div>

      <!-- Alerts -->
      <div class="section" *ngIf="dashboard?.alerts?.length">
        <h3>Active Alerts</h3>
        <div class="alerts-list">
          <div
            *ngFor="let alert of dashboard?.alerts"
            class="alert-item"
            [class.info]="alert.severity === 'INFO'"
            [class.warning]="alert.severity === 'WARNING'"
            [class.error]="alert.severity === 'ERROR'"
            [class.critical]="alert.severity === 'CRITICAL'"
          >
            <span class="alert-icon">
              {{ alert.severity === 'INFO' ? 'ℹ️' :
                 alert.severity === 'WARNING' ? '⚠️' :
                 alert.severity === 'ERROR' ? '❌' : '🚨' }}
            </span>
            <div class="alert-content">
              <span class="alert-message">{{ alert.message }}</span>
              <span class="alert-source">{{ alert.source }} - {{ alert.timestamp | date:'short' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- No Alerts -->
      <div class="section no-alerts" *ngIf="!dashboard?.alerts?.length">
        <div class="no-alerts-content">
          <span class="check-icon">✅</span>
          <span>No active alerts</span>
        </div>
      </div>

      <!-- Loading State -->
      <div class="loading" *ngIf="loading">
        <div class="spinner"></div>
        <span>Loading dashboard...</span>
      </div>
    </div>
  `,
  styles: [`
    .dashboard {
      max-width: 1400px;
      margin: 0 auto;
    }

    .dashboard-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }

    .dashboard-header h2 {
      margin: 0;
      color: #1a237e;
    }

    .last-updated {
      color: #666;
      font-size: 14px;
    }

    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 20px;
      margin-bottom: 32px;
    }

    .metric-card {
      background: white;
      border-radius: 12px;
      padding: 20px;
      display: flex;
      align-items: center;
      gap: 16px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      transition: transform 0.2s ease, box-shadow 0.2s ease;
    }

    .metric-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
    }

    .metric-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
    }

    .metric-icon.patients { background: #e3f2fd; }
    .metric-icon.users { background: #f3e5f5; }
    .metric-icon.evaluations { background: #e8f5e9; }
    .metric-icon.gaps { background: #fff3e0; }
    .metric-icon.latency { background: #fce4ec; }
    .metric-icon.cache { background: #e0f7fa; }

    .metric-content {
      display: flex;
      flex-direction: column;
    }

    .metric-value {
      font-size: 24px;
      font-weight: 700;
      color: #1a237e;
    }

    .metric-label {
      font-size: 13px;
      color: #666;
    }

    .section {
      background: white;
      border-radius: 12px;
      padding: 24px;
      margin-bottom: 24px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    .section h3 {
      margin: 0 0 20px 0;
      color: #333;
      font-size: 18px;
    }

    .services-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 16px;
    }

    .service-card {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
      background: #f5f7fa;
      border-radius: 8px;
      border-left: 4px solid #ccc;
    }

    .service-card.up {
      border-left-color: #4caf50;
    }

    .service-card.down {
      border-left-color: #f44336;
      background: #ffebee;
    }

    .service-card.degraded {
      border-left-color: #ff9800;
      background: #fff3e0;
    }

    .service-status-indicator {
      width: 12px;
      height: 12px;
      border-radius: 50%;
      background: #ccc;
    }

    .service-card.up .service-status-indicator { background: #4caf50; }
    .service-card.down .service-status-indicator { background: #f44336; }
    .service-card.degraded .service-status-indicator { background: #ff9800; }

    .service-info {
      flex: 1;
      display: flex;
      flex-direction: column;
    }

    .service-name {
      font-weight: 600;
      color: #333;
    }

    .service-uptime {
      font-size: 13px;
      color: #666;
    }

    .service-status-badge {
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 600;
      background: #e0e0e0;
      color: #666;
    }

    .service-card.up .service-status-badge {
      background: #c8e6c9;
      color: #2e7d32;
    }

    .service-card.down .service-status-badge {
      background: #ffcdd2;
      color: #c62828;
    }

    .alerts-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .alert-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 12px 16px;
      border-radius: 8px;
      background: #f5f5f5;
    }

    .alert-item.warning { background: #fff3e0; }
    .alert-item.error { background: #ffebee; }
    .alert-item.critical { background: #fce4ec; }

    .alert-icon {
      font-size: 20px;
    }

    .alert-content {
      display: flex;
      flex-direction: column;
    }

    .alert-message {
      font-weight: 500;
      color: #333;
    }

    .alert-source {
      font-size: 13px;
      color: #666;
    }

    .no-alerts {
      background: #e8f5e9;
    }

    .no-alerts-content {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      color: #2e7d32;
      font-weight: 500;
    }

    .check-icon {
      font-size: 24px;
    }

    .loading {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
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
export class DashboardComponent implements OnInit, OnDestroy {
  dashboard: DashboardSnapshot | null = null;
  loading = true;
  private destroy$ = new Subject<void>();

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    // Fetch dashboard data every 30 seconds
    interval(30000)
      .pipe(
        startWith(0),
        switchMap(() => this.adminService.getDashboard()),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (data) => {
          this.dashboard = data;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
