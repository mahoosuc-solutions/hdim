import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil, interval, startWith, switchMap } from 'rxjs';
import { CxApiService } from '../shared/services/cx-api.service';
import { DashboardData, Activity, Bead } from '../shared/models';

/**
 * CX Dashboard Component
 *
 * Main dashboard showing:
 * - Sales pipeline summary
 * - Investor pipeline summary
 * - Customer health overview
 * - Recent activity feed
 * - Agent activity (ready beads)
 */
@Component({
  selector: 'cx-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="dashboard-container">
      <header class="dashboard-header">
        <h1>CX Dashboard</h1>
        <span class="timestamp" *ngIf="data">
          Updated: {{ data.timestamp | date:'short' }}
        </span>
      </header>

      <div class="dashboard-grid" *ngIf="data">
        <!-- Sales Pipeline Card -->
        <div class="card sales-card">
          <div class="card-header">
            <h2>Sales Pipeline</h2>
            <a routerLink="/pipeline" class="view-all">View All →</a>
          </div>
          <div class="stats-grid">
            <div class="stat">
              <span class="stat-value">{{ data.leads.total }}</span>
              <span class="stat-label">Total Leads</span>
            </div>
            <div class="stat">
              <span class="stat-value highlight">{{ data.leads.hot_leads }}</span>
              <span class="stat-label">Hot Leads</span>
            </div>
            <div class="stat">
              <span class="stat-value">{{ data.leads.qualified }}</span>
              <span class="stat-label">Qualified</span>
            </div>
            <div class="stat">
              <span class="stat-value money">\${{ data.leads.pipeline_value | number:'1.0-0' }}</span>
              <span class="stat-label">Pipeline Value</span>
            </div>
          </div>
          <div class="pipeline-mini">
            <div class="stage" *ngFor="let stage of pipelineStages">
              <span class="stage-count">{{ getLeadCount(stage) }}</span>
              <span class="stage-name">{{ stage }}</span>
            </div>
          </div>
        </div>

        <!-- Investor Pipeline Card -->
        <div class="card investor-card">
          <div class="card-header">
            <h2>Investor Pipeline</h2>
            <a routerLink="/investors" class="view-all">View All →</a>
          </div>
          <div class="stats-grid">
            <div class="stat">
              <span class="stat-value">{{ data.investors.total }}</span>
              <span class="stat-label">Total Investors</span>
            </div>
            <div class="stat">
              <span class="stat-value highlight">{{ data.investors.engaged }}</span>
              <span class="stat-label">Engaged</span>
            </div>
            <div class="stat">
              <span class="stat-value success">{{ data.investors.committed }}</span>
              <span class="stat-label">Committed</span>
            </div>
            <div class="stat">
              <span class="stat-value">{{ data.investors.outreach }}</span>
              <span class="stat-label">In Outreach</span>
            </div>
          </div>
          <div class="tier-breakdown">
            <div class="tier tier-a">
              <span class="tier-label">Tier A</span>
              <span class="tier-value">{{ data.investors.by_tier.A }}</span>
            </div>
            <div class="tier tier-b">
              <span class="tier-label">Tier B</span>
              <span class="tier-value">{{ data.investors.by_tier.B }}</span>
            </div>
            <div class="tier tier-c">
              <span class="tier-label">Tier C</span>
              <span class="tier-value">{{ data.investors.by_tier.C }}</span>
            </div>
          </div>
        </div>

        <!-- Customer Health Card -->
        <div class="card customer-card">
          <div class="card-header">
            <h2>Customer Success</h2>
            <a routerLink="/customers" class="view-all">View All →</a>
          </div>
          <div class="stats-grid">
            <div class="stat">
              <span class="stat-value">{{ data.customers.total }}</span>
              <span class="stat-label">Customers</span>
            </div>
            <div class="stat">
              <span class="stat-value success">{{ data.customers.active }}</span>
              <span class="stat-label">Active</span>
            </div>
            <div class="stat">
              <span class="stat-value">{{ data.customers.onboarding }}</span>
              <span class="stat-label">Onboarding</span>
            </div>
            <div class="stat">
              <span class="stat-value">{{ data.customers.total_patients | number:'1.0-0' }}</span>
              <span class="stat-label">Patients</span>
            </div>
          </div>
          <div class="contract-value">
            <span class="label">Total Contract Value</span>
            <span class="value">\${{ data.customers.total_contract_value | number:'1.0-0' }}</span>
          </div>
        </div>

        <!-- Activity This Week Card -->
        <div class="card activity-card">
          <div class="card-header">
            <h2>Activity This Week</h2>
          </div>
          <div class="stats-grid">
            <div class="stat">
              <span class="stat-value">{{ data.activities.total_this_week }}</span>
              <span class="stat-label">Total Activities</span>
            </div>
            <div class="stat">
              <span class="stat-value">{{ data.activities.emails_sent }}</span>
              <span class="stat-label">Emails Sent</span>
            </div>
            <div class="stat">
              <span class="stat-value">{{ data.activities.meetings }}</span>
              <span class="stat-label">Meetings</span>
            </div>
            <div class="stat">
              <span class="stat-value">{{ data.activities.calls }}</span>
              <span class="stat-label">Calls</span>
            </div>
          </div>
        </div>

        <!-- Recent Activity Feed -->
        <div class="card feed-card">
          <div class="card-header">
            <h2>Recent Activity</h2>
          </div>
          <div class="activity-feed">
            <div class="activity-item" *ngFor="let activity of data.recent_feed.slice(0, 8)">
              <span class="activity-icon" [ngClass]="getActivityIcon(activity.activity_type)">
                {{ getActivityEmoji(activity.activity_type) }}
              </span>
              <div class="activity-content">
                <span class="activity-subject">{{ activity.subject || activity.activity_type }}</span>
                <span class="activity-time">{{ activity.created_at | date:'short' }}</span>
              </div>
            </div>
            <div class="empty-state" *ngIf="!data.recent_feed.length">
              No recent activity
            </div>
          </div>
        </div>

        <!-- Ready Beads / Agent Work -->
        <div class="card beads-card">
          <div class="card-header">
            <h2>Ready to Work On</h2>
            <span class="badge">{{ data.beads.open }}</span>
          </div>
          <div class="beads-list">
            <div class="bead-item" *ngFor="let bead of data.ready_beads">
              <span class="bead-priority priority-{{ bead.priority }}">
                {{ getPriorityLabel(bead.priority) }}
              </span>
              <div class="bead-content">
                <span class="bead-title">{{ bead.title }}</span>
                <span class="bead-type">{{ bead.bead_type }}</span>
              </div>
            </div>
            <div class="empty-state" *ngIf="!data.ready_beads.length">
              No ready beads
            </div>
          </div>
          <div class="beads-summary">
            <span>In Progress: {{ data.beads.in_progress }}</span>
            <span>Blocked: {{ data.beads.blocked }}</span>
            <span>Done This Week: {{ data.beads.done_this_week }}</span>
          </div>
        </div>
      </div>

      <div class="loading" *ngIf="!data && !error">
        Loading dashboard...
      </div>

      <div class="error" *ngIf="error">
        {{ error }}
      </div>
    </div>
  `,
  styles: [`
    .dashboard-container {
      padding: 24px;
      max-width: 1600px;
      margin: 0 auto;
    }

    .dashboard-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }

    .dashboard-header h1 {
      margin: 0;
      color: #1a1a2e;
      font-size: 28px;
    }

    .timestamp {
      color: #666;
      font-size: 14px;
    }

    .dashboard-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 24px;
    }

    .card {
      background: white;
      border-radius: 12px;
      padding: 20px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }

    .card-header h2 {
      margin: 0;
      font-size: 18px;
      color: #1a1a2e;
    }

    .view-all {
      color: #4361ee;
      text-decoration: none;
      font-size: 14px;
    }

    .view-all:hover {
      text-decoration: underline;
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 16px;
    }

    .stat {
      display: flex;
      flex-direction: column;
    }

    .stat-value {
      font-size: 24px;
      font-weight: 600;
      color: #1a1a2e;
    }

    .stat-value.highlight {
      color: #f72585;
    }

    .stat-value.success {
      color: #2ecc71;
    }

    .stat-value.money {
      color: #4361ee;
    }

    .stat-label {
      font-size: 12px;
      color: #666;
      margin-top: 4px;
    }

    .pipeline-mini {
      display: flex;
      gap: 8px;
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #eee;
    }

    .stage {
      flex: 1;
      text-align: center;
      padding: 8px;
      background: #f8f9fa;
      border-radius: 8px;
    }

    .stage-count {
      display: block;
      font-size: 18px;
      font-weight: 600;
    }

    .stage-name {
      display: block;
      font-size: 10px;
      color: #666;
      text-transform: uppercase;
    }

    .tier-breakdown {
      display: flex;
      gap: 12px;
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #eee;
    }

    .tier {
      flex: 1;
      text-align: center;
      padding: 12px;
      border-radius: 8px;
    }

    .tier-a {
      background: #e8f5e9;
    }

    .tier-b {
      background: #fff3e0;
    }

    .tier-c {
      background: #fce4ec;
    }

    .tier-label {
      display: block;
      font-size: 12px;
      color: #666;
    }

    .tier-value {
      display: block;
      font-size: 20px;
      font-weight: 600;
    }

    .contract-value {
      margin-top: 16px;
      padding: 16px;
      background: #f0f4ff;
      border-radius: 8px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .contract-value .label {
      color: #666;
    }

    .contract-value .value {
      font-size: 24px;
      font-weight: 600;
      color: #4361ee;
    }

    .activity-feed {
      max-height: 300px;
      overflow-y: auto;
    }

    .activity-item {
      display: flex;
      gap: 12px;
      padding: 12px 0;
      border-bottom: 1px solid #eee;
    }

    .activity-item:last-child {
      border-bottom: none;
    }

    .activity-icon {
      font-size: 20px;
    }

    .activity-content {
      flex: 1;
      display: flex;
      flex-direction: column;
    }

    .activity-subject {
      font-size: 14px;
      color: #1a1a2e;
    }

    .activity-time {
      font-size: 12px;
      color: #999;
    }

    .beads-list {
      max-height: 200px;
      overflow-y: auto;
    }

    .bead-item {
      display: flex;
      gap: 12px;
      padding: 12px 0;
      border-bottom: 1px solid #eee;
    }

    .bead-priority {
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 10px;
      font-weight: 600;
      text-transform: uppercase;
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
      display: flex;
      flex-direction: column;
    }

    .bead-title {
      font-size: 14px;
      color: #1a1a2e;
    }

    .bead-type {
      font-size: 12px;
      color: #999;
    }

    .beads-summary {
      display: flex;
      gap: 16px;
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #eee;
      font-size: 12px;
      color: #666;
    }

    .badge {
      background: #4361ee;
      color: white;
      padding: 4px 8px;
      border-radius: 12px;
      font-size: 12px;
    }

    .empty-state {
      padding: 24px;
      text-align: center;
      color: #999;
    }

    .loading, .error {
      padding: 48px;
      text-align: center;
      color: #666;
    }

    .error {
      color: #dc3545;
    }

    @media (max-width: 1200px) {
      .dashboard-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }

    @media (max-width: 768px) {
      .dashboard-grid {
        grid-template-columns: 1fr;
      }
    }
  `],
})
export class DashboardComponent implements OnInit, OnDestroy {
  data: DashboardData | null = null;
  error: string | null = null;

  pipelineStages = ['new', 'contacted', 'qualified', 'demo', 'proposal'];

  private destroy$ = new Subject<void>();

  constructor(private cxApi: CxApiService) {}

  ngOnInit(): void {
    // Poll every 30 seconds
    interval(30000)
      .pipe(
        startWith(0),
        switchMap(() => this.cxApi.getDashboard()),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (data) => {
          this.data = data;
          this.error = null;
        },
        error: (err) => {
          this.error = 'Failed to load dashboard. Is the CX API running?';
          console.error('Dashboard error:', err);
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  getLeadCount(stage: string): number {
    // Map display stages to actual status
    const statusMap: Record<string, string> = {
      new: 'new',
      contacted: 'contacted',
      qualified: 'qualified',
      demo: 'demo_scheduled',
      proposal: 'proposal_sent',
    };
    // This would need to be from detailed stats
    return 0;
  }

  getActivityIcon(type: string): string {
    const icons: Record<string, string> = {
      email_sent: 'email',
      email_received: 'email',
      call_outbound: 'call',
      call_inbound: 'call',
      meeting_scheduled: 'meeting',
      meeting_completed: 'meeting',
      linkedin_sent: 'linkedin',
      note: 'note',
    };
    return icons[type] || 'other';
  }

  getActivityEmoji(type: string): string {
    const emojis: Record<string, string> = {
      email_sent: '📧',
      email_received: '📨',
      call_outbound: '📞',
      call_inbound: '📲',
      meeting_scheduled: '📅',
      meeting_completed: '✅',
      linkedin_sent: '💼',
      linkedin_accepted: '🤝',
      note: '📝',
    };
    return emojis[type] || '📋';
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
}
