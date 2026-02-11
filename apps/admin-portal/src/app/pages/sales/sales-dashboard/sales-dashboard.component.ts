import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subject, takeUntil, forkJoin } from 'rxjs';
import { SalesService } from '../../../services/sales.service';
import {
  SalesDashboard,
  Lead,
  Opportunity,
  Activity,
  LeadMetrics,
  PipelineSummary,
  ActivityMetrics,
} from '../../../models/sales.model';

@Component({
  selector: 'app-sales-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="sales-dashboard">
      <div class="dashboard-header">
        <div class="header-content">
          <h2>Sales Dashboard</h2>
          <p class="subtitle">Overview of your sales performance</p>
        </div>
        <div class="header-actions">
          <button class="btn btn-primary" (click)="openNewLead()">
            + New Lead
          </button>
          <button class="btn btn-secondary" (click)="refreshData()">
            Refresh
          </button>
        </div>
      </div>

      <!-- Loading State -->
      <div class="loading" *ngIf="isLoading()">
        <div class="spinner"></div>
        <span>Loading dashboard...</span>
      </div>

      <!-- Error State -->
      <div class="error-banner" *ngIf="error()">
        <span>{{ error() }}</span>
        <button class="btn-link" (click)="refreshData()">Retry</button>
      </div>

      <!-- Dashboard Content -->
      <ng-container *ngIf="!isLoading() && dashboard()">
        <!-- Lead Metrics -->
        <section class="metrics-section">
          <h3>Lead Performance</h3>
          <div class="metrics-grid">
            <div class="metric-card leads">
              <div class="metric-icon">
                <span>👤</span>
              </div>
              <div class="metric-content">
                <span class="metric-value">{{ dashboard()!.leadMetrics.totalLeads }}</span>
                <span class="metric-label">Total Leads</span>
              </div>
            </div>

            <div class="metric-card open">
              <div class="metric-icon">
                <span>📥</span>
              </div>
              <div class="metric-content">
                <span class="metric-value">{{ dashboard()!.leadMetrics.openLeads }}</span>
                <span class="metric-label">Open Leads</span>
              </div>
            </div>

            <div class="metric-card qualified">
              <div class="metric-icon">
                <span>✅</span>
              </div>
              <div class="metric-content">
                <span class="metric-value">{{ dashboard()!.leadMetrics.qualifiedLeads }}</span>
                <span class="metric-label">Qualified</span>
              </div>
            </div>

            <div class="metric-card converted">
              <div class="metric-icon">
                <span>🎯</span>
              </div>
              <div class="metric-content">
                <span class="metric-value">{{ (dashboard()!.leadMetrics.conversionRate * 100) | number:'1.1-1' }}%</span>
                <span class="metric-label">Conversion Rate</span>
              </div>
            </div>
          </div>
        </section>

        <!-- Pipeline Metrics -->
        <section class="metrics-section">
          <h3>Pipeline Overview</h3>
          <div class="metrics-grid">
            <div class="metric-card pipeline">
              <div class="metric-icon">
                <span>💰</span>
              </div>
              <div class="metric-content">
                <span class="metric-value">{{ dashboard()!.pipelineMetrics.totalValue | currency:'USD':'symbol':'1.0-0' }}</span>
                <span class="metric-label">Total Pipeline</span>
              </div>
            </div>

            <div class="metric-card weighted">
              <div class="metric-icon">
                <span>📊</span>
              </div>
              <div class="metric-content">
                <span class="metric-value">{{ dashboard()!.pipelineMetrics.weightedValue | currency:'USD':'symbol':'1.0-0' }}</span>
                <span class="metric-label">Weighted Value</span>
              </div>
            </div>

            <div class="metric-card deals">
              <div class="metric-icon">
                <span>📋</span>
              </div>
              <div class="metric-content">
                <span class="metric-value">{{ dashboard()!.pipelineMetrics.openOpportunities }}</span>
                <span class="metric-label">Open Deals</span>
              </div>
            </div>

            <div class="metric-card closing">
              <div class="metric-icon">
                <span>⏰</span>
              </div>
              <div class="metric-content">
                <span class="metric-value">{{ dashboard()!.pipelineMetrics.closingThisMonth }}</span>
                <span class="metric-label">Closing This Month</span>
              </div>
            </div>

            <div class="metric-card at-risk" *ngIf="dashboard()!.pipelineMetrics.atRiskDeals > 0">
              <div class="metric-icon warning">
                <span>⚠️</span>
              </div>
              <div class="metric-content">
                <span class="metric-value warning">{{ dashboard()!.pipelineMetrics.atRiskDeals }}</span>
                <span class="metric-label">At Risk</span>
              </div>
            </div>
          </div>
        </section>

        <!-- Activity Metrics -->
        <section class="metrics-section">
          <h3>Activity Summary</h3>
          <div class="metrics-grid">
            <div class="metric-card activities">
              <div class="metric-icon">
                <span>📞</span>
              </div>
              <div class="metric-content">
                <span class="metric-value">{{ dashboard()!.activityMetrics.callsThisWeek }}</span>
                <span class="metric-label">Calls This Week</span>
              </div>
            </div>

            <div class="metric-card emails">
              <div class="metric-icon">
                <span>📧</span>
              </div>
              <div class="metric-content">
                <span class="metric-value">{{ dashboard()!.activityMetrics.emailsThisWeek }}</span>
                <span class="metric-label">Emails This Week</span>
              </div>
            </div>

            <div class="metric-card meetings">
              <div class="metric-icon">
                <span>📅</span>
              </div>
              <div class="metric-content">
                <span class="metric-value">{{ dashboard()!.activityMetrics.meetingsThisWeek }}</span>
                <span class="metric-label">Meetings This Week</span>
              </div>
            </div>

            <div class="metric-card overdue" *ngIf="dashboard()!.activityMetrics.overdueCount > 0">
              <div class="metric-icon warning">
                <span>🔴</span>
              </div>
              <div class="metric-content">
                <span class="metric-value warning">{{ dashboard()!.activityMetrics.overdueCount }}</span>
                <span class="metric-label">Overdue</span>
              </div>
            </div>
          </div>
        </section>

        <!-- Recent Items -->
        <div class="recent-grid">
          <!-- Recent Leads -->
          <section class="recent-section">
            <div class="section-header">
              <h3>Recent Leads</h3>
              <a routerLink="/sales/leads" class="view-all">View All</a>
            </div>
            <div class="recent-list">
              <div class="recent-item" *ngFor="let lead of dashboard()!.recentLeads">
                <div class="item-avatar">{{ getInitials(lead.firstName, lead.lastName) }}</div>
                <div class="item-content">
                  <span class="item-name">{{ lead.firstName }} {{ lead.lastName }}</span>
                  <span class="item-detail">{{ lead.company }}</span>
                </div>
                <span class="status-badge" [class]="lead.status.toLowerCase()">
                  {{ formatStatus(lead.status) }}
                </span>
              </div>
              <div class="empty-state" *ngIf="!dashboard()!.recentLeads?.length">
                No recent leads
              </div>
            </div>
          </section>

          <!-- Recent Opportunities -->
          <section class="recent-section">
            <div class="section-header">
              <h3>Recent Opportunities</h3>
              <a routerLink="/sales/opportunities" class="view-all">View All</a>
            </div>
            <div class="recent-list">
              <div class="recent-item" *ngFor="let opp of dashboard()!.recentOpportunities">
                <div class="item-icon">💼</div>
                <div class="item-content">
                  <span class="item-name">{{ opp.name }}</span>
                  <span class="item-detail">{{ opp.amount | currency:'USD':'symbol':'1.0-0' }}</span>
                </div>
                <span class="stage-badge" [class]="opp.stage.toLowerCase()">
                  {{ formatStage(opp.stage) }}
                </span>
              </div>
              <div class="empty-state" *ngIf="!dashboard()!.recentOpportunities?.length">
                No recent opportunities
              </div>
            </div>
          </section>

          <!-- Upcoming Activities -->
          <section class="recent-section">
            <div class="section-header">
              <h3>Upcoming Activities</h3>
              <a routerLink="/sales/activities" class="view-all">View All</a>
            </div>
            <div class="recent-list">
              <div class="recent-item" *ngFor="let activity of dashboard()!.upcomingActivities">
                <div class="item-icon">{{ getActivityIcon(activity.type) }}</div>
                <div class="item-content">
                  <span class="item-name">{{ activity.subject }}</span>
                  <span class="item-detail">{{ activity.dueDate | date:'shortDate' }}</span>
                </div>
                <span class="priority-badge" [class]="activity.priority.toLowerCase()">
                  {{ activity.priority }}
                </span>
              </div>
              <div class="empty-state" *ngIf="!dashboard()!.upcomingActivities?.length">
                No upcoming activities
              </div>
            </div>
          </section>
        </div>

        <!-- Quick Actions -->
        <section class="quick-actions">
          <h3>Quick Actions</h3>
          <div class="actions-grid">
            <button class="action-btn" (click)="openNewLead()">
              <span class="action-icon">👤</span>
              <span>Add Lead</span>
            </button>
            <button class="action-btn" routerLink="/sales/activities">
              <span class="action-icon">📞</span>
              <span>Log Call</span>
            </button>
            <button class="action-btn" routerLink="/sales/activities">
              <span class="action-icon">📧</span>
              <span>Log Email</span>
            </button>
            <button class="action-btn" routerLink="/sales/opportunities">
              <span class="action-icon">💼</span>
              <span>New Opportunity</span>
            </button>
            <button class="action-btn" routerLink="/sales/pipeline">
              <span class="action-icon">📊</span>
              <span>View Pipeline</span>
            </button>
            <button class="action-btn" routerLink="/sales/sequences">
              <span class="action-icon">📋</span>
              <span>Email Sequences</span>
            </button>
            <button class="action-btn" routerLink="/sales/linkedin">
              <span class="action-icon">🔗</span>
              <span>LinkedIn Campaigns</span>
            </button>
          </div>
        </section>
      </ng-container>
    </div>
  `,
  styles: [`
    .sales-dashboard {
      max-width: 1400px;
      margin: 0 auto;
    }

    .dashboard-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 24px;
    }

    .header-content h2 {
      margin: 0;
      color: #1a237e;
    }

    .subtitle {
      margin: 4px 0 0;
      color: #666;
      font-size: 14px;
    }

    .header-actions {
      display: flex;
      gap: 12px;
    }

    .btn {
      padding: 10px 20px;
      border-radius: 8px;
      font-weight: 500;
      cursor: pointer;
      border: none;
      transition: all 0.2s ease;
    }

    .btn-primary {
      background: #1a237e;
      color: white;
    }

    .btn-primary:hover {
      background: #0d47a1;
    }

    .btn-secondary {
      background: #f5f5f5;
      color: #333;
      border: 1px solid #ddd;
    }

    .btn-secondary:hover {
      background: #e0e0e0;
    }

    .btn-link {
      background: none;
      border: none;
      color: #1a237e;
      cursor: pointer;
      text-decoration: underline;
    }

    .error-banner {
      background: #ffebee;
      color: #c62828;
      padding: 16px;
      border-radius: 8px;
      margin-bottom: 24px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .metrics-section {
      background: white;
      border-radius: 12px;
      padding: 24px;
      margin-bottom: 24px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    .metrics-section h3 {
      margin: 0 0 20px 0;
      color: #333;
      font-size: 18px;
    }

    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 16px;
    }

    .metric-card {
      background: #f8f9fa;
      border-radius: 12px;
      padding: 20px;
      display: flex;
      align-items: center;
      gap: 16px;
      transition: transform 0.2s ease;
    }

    .metric-card:hover {
      transform: translateY(-2px);
    }

    .metric-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      background: #e3f2fd;
    }

    .metric-icon.warning {
      background: #fff3e0;
    }

    .metric-card.leads .metric-icon { background: #e3f2fd; }
    .metric-card.open .metric-icon { background: #fff3e0; }
    .metric-card.qualified .metric-icon { background: #e8f5e9; }
    .metric-card.converted .metric-icon { background: #f3e5f5; }
    .metric-card.pipeline .metric-icon { background: #e8f5e9; }
    .metric-card.weighted .metric-icon { background: #fce4ec; }
    .metric-card.deals .metric-icon { background: #e0f7fa; }
    .metric-card.closing .metric-icon { background: #fff8e1; }
    .metric-card.activities .metric-icon { background: #e3f2fd; }
    .metric-card.emails .metric-icon { background: #f3e5f5; }
    .metric-card.meetings .metric-icon { background: #e8f5e9; }

    .metric-content {
      display: flex;
      flex-direction: column;
    }

    .metric-value {
      font-size: 24px;
      font-weight: 700;
      color: #1a237e;
    }

    .metric-value.warning {
      color: #f57c00;
    }

    .metric-label {
      font-size: 13px;
      color: #666;
    }

    .recent-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
      gap: 24px;
      margin-bottom: 24px;
    }

    .recent-section {
      background: white;
      border-radius: 12px;
      padding: 24px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }

    .section-header h3 {
      margin: 0;
      color: #333;
      font-size: 16px;
    }

    .view-all {
      color: #1a237e;
      font-size: 14px;
      text-decoration: none;
    }

    .view-all:hover {
      text-decoration: underline;
    }

    .recent-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .recent-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      background: #f8f9fa;
      border-radius: 8px;
      transition: background 0.2s ease;
    }

    .recent-item:hover {
      background: #f0f0f0;
    }

    .item-avatar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: #1a237e;
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      font-size: 14px;
    }

    .item-icon {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: #e3f2fd;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
    }

    .item-content {
      flex: 1;
      display: flex;
      flex-direction: column;
    }

    .item-name {
      font-weight: 500;
      color: #333;
    }

    .item-detail {
      font-size: 13px;
      color: #666;
    }

    .status-badge, .stage-badge, .priority-badge {
      padding: 4px 10px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
    }

    .status-badge.open { background: #e3f2fd; color: #1565c0; }
    .status-badge.qualified { background: #e8f5e9; color: #2e7d32; }
    .status-badge.unqualified { background: #fafafa; color: #757575; }
    .status-badge.converted { background: #f3e5f5; color: #7b1fa2; }
    .status-badge.lost { background: #ffebee; color: #c62828; }

    .stage-badge.discovery { background: #e3f2fd; color: #1565c0; }
    .stage-badge.demo { background: #fff3e0; color: #ef6c00; }
    .stage-badge.proposal { background: #fce4ec; color: #c2185b; }
    .stage-badge.negotiation { background: #f3e5f5; color: #7b1fa2; }
    .stage-badge.contract { background: #e0f2f1; color: #00796b; }
    .stage-badge.closed_won { background: #e8f5e9; color: #2e7d32; }
    .stage-badge.closed_lost { background: #ffebee; color: #c62828; }

    .priority-badge.low { background: #f5f5f5; color: #757575; }
    .priority-badge.medium { background: #fff3e0; color: #ef6c00; }
    .priority-badge.high { background: #ffebee; color: #c62828; }

    .empty-state {
      padding: 24px;
      text-align: center;
      color: #999;
      font-style: italic;
    }

    .quick-actions {
      background: white;
      border-radius: 12px;
      padding: 24px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    .quick-actions h3 {
      margin: 0 0 20px 0;
      color: #333;
      font-size: 18px;
    }

    .actions-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
      gap: 16px;
    }

    .action-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 20px;
      background: #f8f9fa;
      border: 1px solid #e0e0e0;
      border-radius: 12px;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .action-btn:hover {
      background: #e3f2fd;
      border-color: #1a237e;
    }

    .action-icon {
      font-size: 28px;
    }

    .action-btn span:last-child {
      font-size: 14px;
      color: #333;
      font-weight: 500;
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

    @media (max-width: 768px) {
      .dashboard-header {
        flex-direction: column;
        gap: 16px;
      }

      .recent-grid {
        grid-template-columns: 1fr;
      }
    }
  `],
})
export class SalesDashboardComponent implements OnInit, OnDestroy {
  private readonly salesService = inject(SalesService);
  private destroy$ = new Subject<void>();

  // Using service signals directly
  dashboard = this.salesService.dashboard;
  isLoading = this.salesService.isLoading;
  error = this.salesService.error;

  ngOnInit(): void {
    this.loadDashboard();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadDashboard(): void {
    this.salesService.loadDashboard()
      .pipe(takeUntil(this.destroy$))
      .subscribe();
  }

  refreshData(): void {
    this.loadDashboard();
  }

  openNewLead(): void {
    // TODO: Implement new lead dialog or navigate to lead creation page
  }

  getInitials(firstName: string, lastName: string): string {
    return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
  }

  formatStatus(status: string): string {
    return status.replace('_', ' ').toLowerCase()
      .replace(/\b\w/g, (l) => l.toUpperCase());
  }

  formatStage(stage: string): string {
    return stage.replace('_', ' ').toLowerCase()
      .replace(/\b\w/g, (l) => l.toUpperCase());
  }

  getActivityIcon(type: string): string {
    const icons: Record<string, string> = {
      CALL: '📞',
      EMAIL: '📧',
      MEETING: '📅',
      DEMO: '💻',
      TASK: '✅',
      FOLLOW_UP: '🔄',
      NOTE: '📝',
      OTHER: '📌',
    };
    return icons[type] || '📌';
  }
}
