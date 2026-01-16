import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subscription } from 'rxjs';
import {
  DemoServiceHealth,
  DemoStartupMonitorService,
  DemoStartupSnapshot,
} from './demo-startup-monitor.service';

@Component({
  selector: 'app-demo-startup-monitor',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  template: `
    <div class="monitor-container">
      <mat-card class="monitor-header">
        <div class="header-text">
          <h2>Demo Startup Monitor</h2>
          <p>Real-time service readiness as containers initialize.</p>
        </div>
        <div class="header-actions">
          <button mat-stroked-button color="primary" (click)="refreshOnce()">
            <mat-icon>refresh</mat-icon>
            Refresh
          </button>
        </div>
      </mat-card>

      <mat-card class="summary-card">
        <div class="summary-grid">
          <div class="summary-item" [class.active]="snapshot?.prometheusReachable">
            <mat-icon>{{ snapshot?.prometheusReachable ? 'check_circle' : 'error' }}</mat-icon>
            <div>
              <span>Prometheus</span>
              <strong>{{ snapshot?.prometheusReachable ? 'Reachable' : 'Offline' }}</strong>
            </div>
          </div>
          <div class="summary-item" [class.active]="snapshot?.grafanaReachable">
            <mat-icon>{{ snapshot?.grafanaReachable ? 'check_circle' : 'error' }}</mat-icon>
            <div>
              <span>Grafana</span>
              <strong>{{ snapshot?.grafanaReachable ? 'Reachable' : 'Offline' }}</strong>
            </div>
          </div>
          <div class="summary-item">
            <mat-icon>schedule</mat-icon>
            <div>
              <span>Last Update</span>
              <strong>{{ snapshot?.updatedAt ? (snapshot?.updatedAt | date:'shortTime') : '—' }}</strong>
            </div>
          </div>
        </div>
      </mat-card>

      <div class="service-grid">
        <mat-card *ngFor="let service of services" class="service-card">
          <div class="service-status">
            <mat-icon [ngClass]="statusClass(service)">
              {{ statusIcon(service) }}
            </mat-icon>
            <div>
              <h3>{{ service.label }}</h3>
              <p>{{ service.detail || statusLabel(service) }}</p>
            </div>
          </div>
          <div class="status-pill" [ngClass]="statusClass(service)">
            {{ statusLabel(service) }}
          </div>
        </mat-card>
      </div>

      <div class="loading-state" *ngIf="loading">
        <mat-progress-spinner diameter="36"></mat-progress-spinner>
        <span>Waiting for monitoring data...</span>
      </div>
    </div>
  `,
  styles: [`
    .monitor-container {
      display: flex;
      flex-direction: column;
      gap: 20px;
      padding: 24px;
    }

    .monitor-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px 24px;
    }

    .monitor-header h2 {
      margin: 0 0 4px;
    }

    .monitor-header p {
      margin: 0;
      color: #6b7280;
    }

    .summary-card {
      padding: 16px 20px;
    }

    .summary-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px;
    }

    .summary-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 16px;
      border-radius: 12px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
    }

    .summary-item mat-icon {
      color: #ef4444;
    }

    .summary-item.active mat-icon {
      color: #22c55e;
    }

    .summary-item span {
      display: block;
      font-size: 12px;
      color: #6b7280;
    }

    .summary-item strong {
      font-size: 15px;
    }

    .service-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 16px;
    }

    .service-card {
      padding: 16px 18px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .service-status {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .service-status mat-icon {
      font-size: 28px;
      width: 28px;
      height: 28px;
    }

    .service-status h3 {
      margin: 0;
      font-size: 16px;
    }

    .service-status p {
      margin: 2px 0 0;
      color: #6b7280;
      font-size: 13px;
    }

    .status-pill {
      align-self: flex-start;
      padding: 4px 10px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 600;
    }

    .healthy {
      color: #15803d;
      background: #dcfce7;
    }

    .unhealthy {
      color: #b91c1c;
      background: #fee2e2;
    }

    .unknown {
      color: #7c3aed;
      background: #ede9fe;
    }

    .loading-state {
      display: flex;
      align-items: center;
      gap: 12px;
      color: #6b7280;
    }
  `],
})
export class DemoStartupMonitorComponent implements OnInit, OnDestroy {
  services: DemoServiceHealth[] = [];
  snapshot: DemoStartupSnapshot | null = null;
  loading = true;

  private subscription?: Subscription;

  constructor(private monitorService: DemoStartupMonitorService) {}

  ngOnInit(): void {
    this.subscription = this.monitorService.streamStatus().subscribe((snapshot) => {
      this.snapshot = snapshot;
      this.services = snapshot.services;
      this.loading = false;
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  refreshOnce(): void {
    this.loading = true;
    this.subscription?.unsubscribe();
    this.subscription = this.monitorService.streamStatus().subscribe((snapshot) => {
      this.snapshot = snapshot;
      this.services = snapshot.services;
      this.loading = false;
    });
  }

  statusLabel(service: DemoServiceHealth): string {
    if (service.status === 'healthy') return 'Healthy';
    if (service.status === 'unhealthy') return 'Unhealthy';
    return 'Pending';
  }

  statusIcon(service: DemoServiceHealth): string {
    if (service.status === 'healthy') return 'check_circle';
    if (service.status === 'unhealthy') return 'error';
    return 'hourglass_empty';
  }

  statusClass(service: DemoServiceHealth): string {
    return service.status;
  }
}
