import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import { CxApiService } from '../shared/services/cx-api.service';
import { Bead } from '../shared/models';

interface AgentActivity {
  type: string;
  action: string;
  agent_id: string;
  details: any;
  timestamp: string;
}

/**
 * Agents Component
 *
 * Real-time agent activity feed and bead management.
 */
@Component({
  selector: 'cx-agents',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="agents-container">
      <header class="page-header">
        <h1>Agent Activity</h1>
        <div class="connection-status" [ngClass]="{ connected: wsConnected }">
          <span class="dot"></span>
          {{ wsConnected ? 'Live' : 'Disconnected' }}
        </div>
      </header>

      <div class="agents-grid">
        <!-- Activity Feed -->
        <div class="card activity-feed-card">
          <div class="card-header">
            <h2>Live Activity Feed</h2>
            <button class="btn-small" (click)="clearFeed()">Clear</button>
          </div>
          <div class="activity-feed">
            <div
              class="activity-item"
              *ngFor="let activity of activityFeed"
              [ngClass]="activity.type"
            >
              <div class="activity-icon">
                {{ getActivityIcon(activity.type) }}
              </div>
              <div class="activity-content">
                <span class="activity-action">{{ activity.action }}</span>
                <span class="activity-agent">{{ activity.agent_id }}</span>
                <div class="activity-details" *ngIf="activity.details">
                  {{ activity.details.title || activity.details.subject || '' }}
                </div>
                <span class="activity-time">{{ activity.timestamp | date:'medium' }}</span>
              </div>
            </div>
            <div class="empty-state" *ngIf="!activityFeed.length">
              No recent activity. Connect to see real-time updates.
            </div>
          </div>
        </div>

        <!-- Ready Beads -->
        <div class="card beads-card">
          <div class="card-header">
            <h2>Ready Beads</h2>
            <span class="badge">{{ readyBeads.length }}</span>
          </div>
          <div class="beads-list">
            <div class="bead-item" *ngFor="let bead of readyBeads">
              <div class="bead-priority priority-{{ bead.priority }}">
                {{ getPriorityLabel(bead.priority) }}
              </div>
              <div class="bead-content">
                <span class="bead-title">{{ bead.title }}</span>
                <span class="bead-type">{{ bead.bead_type }}</span>
              </div>
              <div class="bead-actions">
                <button class="btn-icon" (click)="startBead(bead)" title="Start">
                  ▶
                </button>
              </div>
            </div>
            <div class="empty-state" *ngIf="!readyBeads.length">
              No beads ready to work on
            </div>
          </div>
        </div>

        <!-- Bead Stats -->
        <div class="card stats-card">
          <div class="card-header">
            <h2>Bead Statistics</h2>
          </div>
          <div class="stats-grid" *ngIf="beadStats">
            <div class="stat">
              <span class="stat-value">{{ beadStats.total }}</span>
              <span class="stat-label">Total</span>
            </div>
            <div class="stat">
              <span class="stat-value open">{{ beadStats.open }}</span>
              <span class="stat-label">Open</span>
            </div>
            <div class="stat">
              <span class="stat-value in-progress">{{ beadStats.in_progress }}</span>
              <span class="stat-label">In Progress</span>
            </div>
            <div class="stat">
              <span class="stat-value blocked">{{ beadStats.blocked }}</span>
              <span class="stat-label">Blocked</span>
            </div>
          </div>

          <div class="priority-breakdown" *ngIf="beadStats?.by_priority">
            <h4>By Priority</h4>
            <div class="priority-row" *ngFor="let item of getPriorityItems()">
              <span class="priority-label">{{ item.label }}</span>
              <div class="priority-bar">
                <div
                  class="priority-fill priority-{{ item.level }}"
                  [style.width.%]="(item.count / beadStats.total) * 100"
                ></div>
              </div>
              <span class="priority-count">{{ item.count }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .agents-container {
      padding: 24px;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }

    .page-header h1 {
      margin: 0;
      color: #1a1a2e;
    }

    .connection-status {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 16px;
      background: #f0f0f0;
      border-radius: 20px;
      font-size: 13px;
      color: #666;
    }

    .connection-status .dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #e74c3c;
    }

    .connection-status.connected .dot {
      background: #2ecc71;
    }

    .connection-status.connected {
      background: #d4edda;
      color: #155724;
    }

    .agents-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 24px;
    }

    .card {
      background: white;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    .activity-feed-card {
      grid-row: span 2;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px;
      border-bottom: 1px solid #eee;
    }

    .card-header h2 {
      margin: 0;
      font-size: 18px;
      color: #1a1a2e;
    }

    .btn-small {
      background: #f0f0f0;
      color: #666;
      border: none;
      padding: 6px 12px;
      border-radius: 4px;
      cursor: pointer;
      font-size: 12px;
    }

    .btn-small:hover {
      background: #e0e0e0;
    }

    .badge {
      background: #4361ee;
      color: white;
      padding: 4px 10px;
      border-radius: 12px;
      font-size: 12px;
    }

    .activity-feed {
      padding: 0;
      max-height: 600px;
      overflow-y: auto;
    }

    .activity-item {
      display: flex;
      gap: 16px;
      padding: 16px 20px;
      border-bottom: 1px solid #f0f0f0;
    }

    .activity-item:hover {
      background: #fafafa;
    }

    .activity-icon {
      font-size: 24px;
    }

    .activity-content {
      flex: 1;
    }

    .activity-action {
      display: block;
      font-weight: 500;
      color: #1a1a2e;
      margin-bottom: 4px;
    }

    .activity-agent {
      font-size: 12px;
      color: #4361ee;
      margin-right: 8px;
    }

    .activity-details {
      font-size: 13px;
      color: #666;
      margin-top: 4px;
    }

    .activity-time {
      font-size: 11px;
      color: #999;
    }

    .beads-list {
      padding: 0;
      max-height: 300px;
      overflow-y: auto;
    }

    .bead-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px 20px;
      border-bottom: 1px solid #f0f0f0;
    }

    .bead-priority {
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 10px;
      font-weight: 600;
      text-transform: uppercase;
      white-space: nowrap;
    }

    .priority-0 {
      background: #ff4757;
      color: white;
    }

    .priority-1 {
      background: #ffa502;
      color: white;
    }

    .priority-2 {
      background: #2ed573;
      color: white;
    }

    .priority-3 {
      background: #70a1ff;
      color: white;
    }

    .priority-4 {
      background: #dfe4ea;
      color: #666;
    }

    .bead-content {
      flex: 1;
    }

    .bead-title {
      display: block;
      font-size: 14px;
      color: #1a1a2e;
    }

    .bead-type {
      font-size: 12px;
      color: #999;
    }

    .btn-icon {
      background: #f0f4ff;
      color: #4361ee;
      border: none;
      width: 32px;
      height: 32px;
      border-radius: 6px;
      cursor: pointer;
      font-size: 14px;
    }

    .btn-icon:hover {
      background: #4361ee;
      color: white;
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16px;
      padding: 20px;
    }

    .stat {
      text-align: center;
    }

    .stat-value {
      display: block;
      font-size: 24px;
      font-weight: 600;
      color: #1a1a2e;
    }

    .stat-value.open {
      color: #4361ee;
    }

    .stat-value.in-progress {
      color: #f39c12;
    }

    .stat-value.blocked {
      color: #e74c3c;
    }

    .stat-label {
      font-size: 12px;
      color: #666;
    }

    .priority-breakdown {
      padding: 0 20px 20px;
    }

    .priority-breakdown h4 {
      margin: 0 0 12px 0;
      font-size: 14px;
      color: #666;
    }

    .priority-row {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 8px;
    }

    .priority-label {
      width: 60px;
      font-size: 12px;
      color: #666;
    }

    .priority-bar {
      flex: 1;
      height: 8px;
      background: #f0f0f0;
      border-radius: 4px;
      overflow: hidden;
    }

    .priority-fill {
      height: 100%;
      border-radius: 4px;
    }

    .priority-fill.priority-0 {
      background: #ff4757;
    }

    .priority-fill.priority-1 {
      background: #ffa502;
    }

    .priority-fill.priority-2 {
      background: #2ed573;
    }

    .priority-fill.priority-3 {
      background: #70a1ff;
    }

    .priority-fill.priority-4 {
      background: #dfe4ea;
    }

    .priority-count {
      width: 30px;
      text-align: right;
      font-size: 12px;
      color: #666;
    }

    .empty-state {
      padding: 32px;
      text-align: center;
      color: #999;
    }

    @media (max-width: 1024px) {
      .agents-grid {
        grid-template-columns: 1fr;
      }

      .activity-feed-card {
        grid-row: span 1;
      }
    }
  `],
})
export class AgentsComponent implements OnInit, OnDestroy {
  activityFeed: AgentActivity[] = [];
  readyBeads: Bead[] = [];
  beadStats: any = null;
  wsConnected = false;

  private destroy$ = new Subject<void>();

  constructor(private cxApi: CxApiService) {}

  ngOnInit(): void {
    this.loadReadyBeads();
    this.loadBeadStats();
    this.loadHistoricalActivity();
    this.connectWebSocket();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.cxApi.disconnectWebSocket();
  }

  loadReadyBeads(): void {
    this.cxApi.getReadyBeads().subscribe({
      next: (beads) => {
        this.readyBeads = beads;
      },
      error: (err) => {
        console.error('Failed to load ready beads:', err);
      },
    });
  }

  loadBeadStats(): void {
    this.cxApi.getBeadStats().subscribe({
      next: (stats) => {
        this.beadStats = stats;
      },
      error: (err) => {
        console.error('Failed to load bead stats:', err);
      },
    });
  }

  loadHistoricalActivity(): void {
    this.cxApi.getAgentActivity(20).subscribe({
      next: (activities) => {
        this.activityFeed = activities;
      },
      error: (err) => {
        console.error('Failed to load agent activity:', err);
      },
    });
  }

  connectWebSocket(): void {
    this.cxApi.connectWebSocket('agents').pipe(takeUntil(this.destroy$)).subscribe({
      next: (message) => {
        this.wsConnected = true;
        if (message.type && message.type !== 'raw') {
          this.activityFeed.unshift(message);
          // Keep only last 100 items
          if (this.activityFeed.length > 100) {
            this.activityFeed = this.activityFeed.slice(0, 100);
          }
        }
      },
      error: () => {
        this.wsConnected = false;
      },
    });

    // Set connected after a short delay
    setTimeout(() => {
      this.wsConnected = true;
    }, 1000);
  }

  clearFeed(): void {
    this.activityFeed = [];
  }

  startBead(bead: Bead): void {
    this.cxApi.startBead(bead.id, 'user').subscribe({
      next: () => {
        this.loadReadyBeads();
        this.loadBeadStats();
      },
      error: (err) => {
        console.error('Failed to start bead:', err);
      },
    });
  }

  getActivityIcon(type: string): string {
    const icons: Record<string, string> = {
      activity: '📋',
      bead_created: '➕',
      bead_started: '▶️',
      bead_completed: '✅',
      bead_blocked: '🚫',
      bead_updated: '📝',
      agent_activity: '🤖',
    };
    return icons[type] || '📌';
  }

  getPriorityLabel(priority: number): string {
    const labels: Record<number, string> = {
      0: 'Critical',
      1: 'High',
      2: 'Medium',
      3: 'Low',
      4: 'Backlog',
    };
    return labels[priority] || 'Medium';
  }

  getPriorityItems(): { label: string; level: number; count: number }[] {
    if (!this.beadStats?.by_priority) return [];

    return [
      { label: 'Critical', level: 0, count: this.beadStats.by_priority['critical'] || 0 },
      { label: 'High', level: 1, count: this.beadStats.by_priority['high'] || 0 },
      { label: 'Medium', level: 2, count: this.beadStats.by_priority['medium'] || 0 },
      { label: 'Low', level: 3, count: this.beadStats.by_priority['low'] || 0 },
      { label: 'Backlog', level: 4, count: this.beadStats.by_priority['backlog'] || 0 },
    ];
  }
}
