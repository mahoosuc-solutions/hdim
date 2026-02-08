import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { CxApiService } from '../shared/services/cx-api.service';

@Component({
  selector: 'cx-campaigns-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="campaigns-dashboard">
      <!-- Header -->
      <div class="dashboard-header">
        <h1>Campaigns</h1>
        <button class="btn btn-primary" (click)="createCampaign()">
          <span class="icon">➕</span>
          New Campaign
        </button>
      </div>

      <!-- Stats Overview -->
      <div class="stats-grid" *ngIf="stats">
        <div class="stat-card">
          <div class="stat-label">Total Campaigns</div>
          <div class="stat-value">{{ stats.total }}</div>
        </div>
        <div class="stat-card active">
          <div class="stat-label">Active</div>
          <div class="stat-value">{{ stats.active }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Total Prospects</div>
          <div class="stat-value">{{ stats.total_prospects }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Contacted</div>
          <div class="stat-value">{{ stats.total_contacted }}</div>
        </div>
        <div class="stat-card success">
          <div class="stat-label">Converted</div>
          <div class="stat-value">{{ stats.total_converted }}</div>
          <div class="stat-subtext">
            {{ getConversionRate() }}% rate
          </div>
        </div>
      </div>

      <!-- Filter Tabs -->
      <div class="filter-tabs">
        <button
          *ngFor="let filter of statusFilters"
          class="tab"
          [class.active]="selectedFilter === filter.value"
          (click)="filterByStatus(filter.value)"
        >
          {{ filter.label }}
          <span class="badge" *ngIf="getFilterCount(filter.value) > 0">
            {{ getFilterCount(filter.value) }}
          </span>
        </button>
      </div>

      <!-- Loading State -->
      <div *ngIf="loading" class="loading-state">
        <div class="spinner"></div>
        <p>Loading campaigns...</p>
      </div>

      <!-- Empty State -->
      <div *ngIf="!loading && filteredCampaigns.length === 0" class="empty-state">
        <div class="empty-icon">📋</div>
        <h3>No campaigns yet</h3>
        <p>Create your first campaign to start reaching out to prospects</p>
        <button class="btn btn-primary" (click)="createCampaign()">
          Create Campaign
        </button>
      </div>

      <!-- Campaigns List -->
      <div *ngIf="!loading && filteredCampaigns.length > 0" class="campaigns-list">
        <div
          *ngFor="let campaign of filteredCampaigns"
          class="campaign-card"
          (click)="viewCampaign(campaign.id)"
        >
          <!-- Status Badge -->
          <div class="campaign-status-badge" [class]="'status-' + campaign.status">
            {{ formatStatus(campaign.status) }}
          </div>

          <!-- Campaign Header -->
          <div class="campaign-header">
            <div>
              <h3>{{ campaign.name }}</h3>
              <p class="campaign-description">{{ campaign.description }}</p>
            </div>
            <div class="campaign-type-badge" [class]="'type-' + campaign.campaign_type">
              {{ formatType(campaign.campaign_type) }}
            </div>
          </div>

          <!-- Campaign Metrics -->
          <div class="campaign-metrics">
            <div class="metric">
              <div class="metric-label">Prospects</div>
              <div class="metric-value">{{ campaign.metrics?.total_prospects || 0 }}</div>
            </div>
            <div class="metric">
              <div class="metric-label">Contacted</div>
              <div class="metric-value">{{ campaign.metrics?.prospects_contacted || 0 }}</div>
            </div>
            <div class="metric">
              <div class="metric-label">Engaged</div>
              <div class="metric-value">{{ campaign.metrics?.prospects_engaged || 0 }}</div>
            </div>
            <div class="metric">
              <div class="metric-label">Converted</div>
              <div class="metric-value success">{{ campaign.metrics?.prospects_converted || 0 }}</div>
            </div>
          </div>

          <!-- Performance Indicators -->
          <div class="campaign-performance" *ngIf="campaign.metrics">
            <div class="performance-bar">
              <div class="bar-label">Open Rate</div>
              <div class="bar-container">
                <div class="bar-fill" [style.width.%]="campaign.metrics.open_rate"></div>
                <span class="bar-text">{{ campaign.metrics.open_rate }}%</span>
              </div>
            </div>
            <div class="performance-bar">
              <div class="bar-label">Reply Rate</div>
              <div class="bar-container">
                <div class="bar-fill" [style.width.%]="campaign.metrics.reply_rate"></div>
                <span class="bar-text">{{ campaign.metrics.reply_rate }}%</span>
              </div>
            </div>
          </div>

          <!-- Campaign Meta -->
          <div class="campaign-meta">
            <span class="meta-item">
              <span class="icon">📋</span>
              {{ campaign.formula_id }}
            </span>
            <span class="meta-item">
              <span class="icon">📅</span>
              {{ formatDate(campaign.created_at) }}
            </span>
            <span class="meta-item" *ngIf="campaign.metrics?.approvals_pending > 0">
              <span class="icon">⏳</span>
              {{ campaign.metrics.approvals_pending }} pending approvals
            </span>
          </div>

          <!-- Quick Actions -->
          <div class="campaign-actions" (click)="$event.stopPropagation()">
            <button
              *ngIf="campaign.status === 'draft'"
              class="btn-action"
              (click)="startCampaign(campaign.id)"
            >
              ▶️ Start
            </button>
            <button
              *ngIf="campaign.status === 'active'"
              class="btn-action"
              (click)="pauseCampaign(campaign.id)"
            >
              ⏸️ Pause
            </button>
            <button
              *ngIf="campaign.status === 'paused'"
              class="btn-action"
              (click)="resumeCampaign(campaign.id)"
            >
              ▶️ Resume
            </button>
            <button class="btn-action" (click)="viewCampaign(campaign.id)">
              👁️ View
            </button>
            <button class="btn-action" (click)="cloneCampaign(campaign)">
              📋 Clone
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .campaigns-dashboard {
      padding: 2rem;
      max-width: 1400px;
      margin: 0 auto;
    }

    .dashboard-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;
    }

    .dashboard-header h1 {
      margin: 0;
      font-size: 2rem;
      color: #333;
    }

    .btn {
      padding: 0.75rem 1.5rem;
      border: none;
      border-radius: 6px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 0.5rem;
      transition: all 0.3s;
    }

    .btn-primary {
      background: #007bff;
      color: white;
    }

    .btn-primary:hover {
      background: #0056b3;
      transform: translateY(-1px);
      box-shadow: 0 4px 8px rgba(0,123,255,0.3);
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1.5rem;
      margin-bottom: 2rem;
    }

    .stat-card {
      background: white;
      border-radius: 8px;
      padding: 1.5rem;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      border-left: 4px solid #e0e0e0;
      transition: transform 0.2s;
    }

    .stat-card:hover {
      transform: translateY(-2px);
    }

    .stat-card.active {
      border-left-color: #007bff;
      background: #f0f7ff;
    }

    .stat-card.success {
      border-left-color: #28a745;
      background: #f0fff4;
    }

    .stat-label {
      font-size: 0.875rem;
      color: #666;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-bottom: 0.5rem;
    }

    .stat-value {
      font-size: 2rem;
      font-weight: 700;
      color: #333;
    }

    .stat-subtext {
      font-size: 0.875rem;
      color: #666;
      margin-top: 0.25rem;
    }

    .filter-tabs {
      display: flex;
      gap: 1rem;
      margin-bottom: 2rem;
      border-bottom: 2px solid #e0e0e0;
    }

    .tab {
      padding: 0.75rem 1.5rem;
      border: none;
      background: transparent;
      cursor: pointer;
      font-size: 1rem;
      color: #666;
      position: relative;
      transition: all 0.3s;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .tab:hover {
      color: #007bff;
    }

    .tab.active {
      color: #007bff;
      font-weight: 600;
    }

    .tab.active::after {
      content: '';
      position: absolute;
      bottom: -2px;
      left: 0;
      right: 0;
      height: 2px;
      background: #007bff;
    }

    .badge {
      background: #007bff;
      color: white;
      border-radius: 12px;
      padding: 0.25rem 0.5rem;
      font-size: 0.75rem;
      font-weight: 600;
    }

    .tab.active .badge {
      background: white;
      color: #007bff;
    }

    .loading-state,
    .empty-state {
      text-align: center;
      padding: 4rem 2rem;
      background: white;
      border-radius: 8px;
    }

    .spinner {
      width: 40px;
      height: 40px;
      border: 4px solid #f3f3f3;
      border-top: 4px solid #007bff;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin: 0 auto 1rem;
    }

    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }

    .empty-icon {
      font-size: 4rem;
      margin-bottom: 1rem;
    }

    .empty-state h3 {
      margin: 0 0 0.5rem 0;
      color: #333;
    }

    .empty-state p {
      color: #666;
      margin-bottom: 2rem;
    }

    .campaigns-list {
      display: grid;
      gap: 1.5rem;
    }

    .campaign-card {
      background: white;
      border-radius: 8px;
      padding: 1.5rem;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      cursor: pointer;
      transition: all 0.3s;
      position: relative;
    }

    .campaign-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    }

    .campaign-status-badge {
      position: absolute;
      top: 1rem;
      right: 1rem;
      padding: 0.25rem 0.75rem;
      border-radius: 12px;
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
    }

    .status-draft {
      background: #f0f0f0;
      color: #666;
    }

    .status-active {
      background: #d4edda;
      color: #155724;
    }

    .status-paused {
      background: #fff3cd;
      color: #856404;
    }

    .status-completed {
      background: #d1ecf1;
      color: #0c5460;
    }

    .campaign-header {
      display: flex;
      justify-content: space-between;
      align-items: start;
      margin-bottom: 1.5rem;
      padding-right: 100px;
    }

    .campaign-header h3 {
      margin: 0 0 0.5rem 0;
      font-size: 1.25rem;
      color: #333;
    }

    .campaign-description {
      margin: 0;
      color: #666;
      font-size: 0.875rem;
    }

    .campaign-type-badge {
      padding: 0.5rem 1rem;
      border-radius: 6px;
      font-size: 0.875rem;
      font-weight: 600;
    }

    .type-investor {
      background: #e7f3ff;
      color: #0056b3;
    }

    .type-customer {
      background: #e8f5e9;
      color: #2e7d32;
    }

    .type-partner {
      background: #fff3e0;
      color: #e65100;
    }

    .campaign-metrics {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 1.5rem;
      margin-bottom: 1.5rem;
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 6px;
    }

    .metric {
      text-align: center;
    }

    .metric-label {
      font-size: 0.75rem;
      color: #666;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-bottom: 0.5rem;
    }

    .metric-value {
      font-size: 1.75rem;
      font-weight: 700;
      color: #333;
    }

    .metric-value.success {
      color: #28a745;
    }

    .campaign-performance {
      display: flex;
      gap: 2rem;
      margin-bottom: 1.5rem;
    }

    .performance-bar {
      flex: 1;
    }

    .bar-label {
      font-size: 0.875rem;
      color: #666;
      margin-bottom: 0.5rem;
    }

    .bar-container {
      position: relative;
      height: 24px;
      background: #e0e0e0;
      border-radius: 12px;
      overflow: hidden;
    }

    .bar-fill {
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      background: linear-gradient(90deg, #007bff, #00a8ff);
      transition: width 0.5s ease;
    }

    .bar-text {
      position: absolute;
      right: 8px;
      top: 50%;
      transform: translateY(-50%);
      font-size: 0.75rem;
      font-weight: 600;
      color: #333;
    }

    .campaign-meta {
      display: flex;
      gap: 1.5rem;
      margin-bottom: 1rem;
      font-size: 0.875rem;
      color: #666;
    }

    .meta-item {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .campaign-actions {
      display: flex;
      gap: 0.75rem;
      padding-top: 1rem;
      border-top: 1px solid #e0e0e0;
    }

    .btn-action {
      padding: 0.5rem 1rem;
      border: 1px solid #e0e0e0;
      background: white;
      border-radius: 4px;
      font-size: 0.875rem;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-action:hover {
      background: #f8f9fa;
      border-color: #007bff;
      color: #007bff;
    }
  `]
})
export class CampaignsDashboardComponent implements OnInit {
  campaigns: any[] = [];
  filteredCampaigns: any[] = [];
  stats: any = null;
  loading = true;
  selectedFilter = 'all';

  statusFilters = [
    { label: 'All', value: 'all' },
    { label: 'Draft', value: 'draft' },
    { label: 'Active', value: 'active' },
    { label: 'Paused', value: 'paused' },
    { label: 'Completed', value: 'completed' }
  ];

  constructor(
    private cxApi: CxApiService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadCampaigns();
    this.loadStats();
  }

  async loadCampaigns(): Promise<void> {
    this.loading = true;
    try {
      this.campaigns = await this.cxApi.getCampaigns().toPromise() || [];
      this.filterByStatus(this.selectedFilter);
    } catch (error) {
      console.error('Failed to load campaigns:', error);
    } finally {
      this.loading = false;
    }
  }

  async loadStats(): Promise<void> {
    try {
      this.stats = await this.cxApi.getCampaignStats().toPromise();
    } catch (error) {
      console.error('Failed to load campaign stats:', error);
    }
  }

  filterByStatus(status: string): void {
    this.selectedFilter = status;
    if (status === 'all') {
      this.filteredCampaigns = this.campaigns;
    } else {
      this.filteredCampaigns = this.campaigns.filter(c => c.status === status);
    }
  }

  getFilterCount(status: string): number {
    if (status === 'all') return this.campaigns.length;
    return this.campaigns.filter(c => c.status === status).length;
  }

  getConversionRate(): number {
    if (!this.stats || this.stats.total_contacted === 0) return 0;
    return Math.round((this.stats.total_converted / this.stats.total_contacted) * 100);
  }

  formatStatus(status: string): string {
    return status.charAt(0).toUpperCase() + status.slice(1);
  }

  formatType(type: string): string {
    return type.charAt(0).toUpperCase() + type.slice(1);
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  }

  createCampaign(): void {
    this.router.navigate(['/campaigns/new']);
  }

  viewCampaign(id: string): void {
    this.router.navigate(['/campaigns', id]);
  }

  async startCampaign(id: string): Promise<void> {
    try {
      await this.cxApi.startCampaign(id).toPromise();
      await this.loadCampaigns();
      await this.loadStats();
    } catch (error) {
      console.error('Failed to start campaign:', error);
      alert('Failed to start campaign');
    }
  }

  async pauseCampaign(id: string): Promise<void> {
    try {
      await this.cxApi.pauseCampaign(id).toPromise();
      await this.loadCampaigns();
    } catch (error) {
      console.error('Failed to pause campaign:', error);
      alert('Failed to pause campaign');
    }
  }

  async resumeCampaign(id: string): Promise<void> {
    try {
      await this.cxApi.resumeCampaign(id).toPromise();
      await this.loadCampaigns();
    } catch (error) {
      console.error('Failed to resume campaign:', error);
      alert('Failed to resume campaign');
    }
  }

  async cloneCampaign(campaign: any): Promise<void> {
    const newName = prompt(`Clone "${campaign.name}" as:`, `${campaign.name} (Copy)`);
    if (!newName) return;

    try {
      const cloned = await this.cxApi.cloneCampaign(campaign.id, newName).toPromise();
      await this.loadCampaigns();
      this.router.navigate(['/campaigns', cloned.id]);
    } catch (error) {
      console.error('Failed to clone campaign:', error);
      alert('Failed to clone campaign');
    }
  }
}
