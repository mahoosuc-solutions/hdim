import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { CxApiService } from '../shared/services/cx-api.service';
import { Subscription, interval } from 'rxjs';
import { switchMap } from 'rxjs/operators';

interface Bead {
  id: string;
  title: string;
  status: string;
  priority: number;
  tags: string[];
  created_at: string;
  updated_at: string;
  depends_on?: string[];
  blocked_by?: string[];
}

interface Activity {
  id: string;
  type: string;
  description: string;
  timestamp: string;
  actor: string;
  metadata?: Record<string, any>;
}

@Component({
  selector: 'app-campaign-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressBarModule,
    MatTabsModule,
    MatTableModule,
    MatMenuModule,
    MatBadgeModule,
  ],
  template: `
    <div class="campaign-detail-container">
      <!-- Header -->
      <div class="campaign-header" *ngIf="campaign">
        <div class="header-content">
          <div class="title-section">
            <button mat-icon-button (click)="goBack()" class="back-button">
              <mat-icon>arrow_back</mat-icon>
            </button>
            <div>
              <h1>{{ campaign.name }}</h1>
              <p class="description">{{ campaign.description }}</p>
            </div>
          </div>
          <div class="header-actions">
            <mat-chip-set>
              <mat-chip [class]="'status-chip status-' + campaign.status">
                {{ campaign.status }}
              </mat-chip>
              <mat-chip class="type-chip">
                {{ campaign.campaign_type }}
              </mat-chip>
            </mat-chip-set>
            <button mat-button [matMenuTriggerFor]="actionsMenu">
              <mat-icon>more_vert</mat-icon>
              Actions
            </button>
            <mat-menu #actionsMenu="matMenu">
              <button mat-menu-item (click)="startCampaign()" *ngIf="campaign.status === 'draft'">
                <mat-icon>play_arrow</mat-icon>
                Start Campaign
              </button>
              <button mat-menu-item (click)="pauseCampaign()" *ngIf="campaign.status === 'active'">
                <mat-icon>pause</mat-icon>
                Pause Campaign
              </button>
              <button mat-menu-item (click)="resumeCampaign()" *ngIf="campaign.status === 'paused'">
                <mat-icon>play_arrow</mat-icon>
                Resume Campaign
              </button>
              <button mat-menu-item (click)="completeCampaign()" *ngIf="campaign.status === 'active'">
                <mat-icon>check_circle</mat-icon>
                Mark Complete
              </button>
              <button mat-menu-item (click)="editCampaign()">
                <mat-icon>edit</mat-icon>
                Edit Campaign
              </button>
              <button mat-menu-item (click)="cloneCampaign()">
                <mat-icon>content_copy</mat-icon>
                Clone Campaign
              </button>
              <button mat-menu-item (click)="archiveCampaign()" *ngIf="campaign.status === 'completed'">
                <mat-icon>archive</mat-icon>
                Archive
              </button>
            </mat-menu>
          </div>
        </div>
      </div>

      <!-- Metrics Dashboard -->
      <div class="metrics-section" *ngIf="metrics">
        <mat-card class="metric-card">
          <div class="metric-value">{{ metrics.total_prospects || 0 }}</div>
          <div class="metric-label">Total Prospects</div>
        </mat-card>
        <mat-card class="metric-card">
          <div class="metric-value">{{ metrics.contacted || 0 }}</div>
          <div class="metric-label">Contacted</div>
          <mat-progress-bar
            mode="determinate"
            [value]="(metrics.contacted / metrics.total_prospects) * 100">
          </mat-progress-bar>
        </mat-card>
        <mat-card class="metric-card">
          <div class="metric-value">{{ metrics.engaged || 0 }}</div>
          <div class="metric-label">Engaged</div>
          <mat-progress-bar
            mode="determinate"
            [value]="(metrics.engaged / metrics.contacted) * 100">
          </mat-progress-bar>
        </mat-card>
        <mat-card class="metric-card">
          <div class="metric-value">{{ metrics.converted || 0 }}</div>
          <div class="metric-label">Converted</div>
          <mat-progress-bar
            mode="determinate"
            [value]="(metrics.converted / metrics.total_prospects) * 100">
          </mat-progress-bar>
        </mat-card>
      </div>

      <!-- Performance Metrics -->
      <div class="performance-section" *ngIf="metrics">
        <mat-card>
          <h3>Performance Metrics</h3>
          <div class="performance-grid">
            <div class="performance-item">
              <div class="performance-label">Open Rate</div>
              <div class="performance-value">{{ metrics.open_rate || 0 }}%</div>
              <mat-progress-bar
                mode="determinate"
                [value]="metrics.open_rate || 0"
                class="rate-bar">
              </mat-progress-bar>
            </div>
            <div class="performance-item">
              <div class="performance-label">Reply Rate</div>
              <div class="performance-value">{{ metrics.reply_rate || 0 }}%</div>
              <mat-progress-bar
                mode="determinate"
                [value]="metrics.reply_rate || 0"
                class="rate-bar">
              </mat-progress-bar>
            </div>
            <div class="performance-item">
              <div class="performance-label">Conversion Rate</div>
              <div class="performance-value">{{ metrics.conversion_rate || 0 }}%</div>
              <mat-progress-bar
                mode="determinate"
                [value]="metrics.conversion_rate || 0"
                class="rate-bar">
              </mat-progress-bar>
            </div>
            <div class="performance-item">
              <div class="performance-label">Pending Approvals</div>
              <div class="performance-value">
                <span [matBadge]="metrics.pending_approvals || 0"
                      [matBadgeHidden]="!metrics.pending_approvals"
                      matBadgeColor="warn">
                  {{ metrics.pending_approvals || 0 }}
                </span>
              </div>
            </div>
          </div>
        </mat-card>
      </div>

      <!-- Tabs: Timeline, Beads, Activities, Settings -->
      <mat-tab-group class="campaign-tabs">
        <!-- Timeline Tab -->
        <mat-tab label="Timeline">
          <div class="tab-content">
            <div class="timeline" *ngIf="activities.length > 0">
              <div class="timeline-item" *ngFor="let activity of activities">
                <div class="timeline-marker">
                  <mat-icon [class]="'activity-icon activity-' + activity.type">
                    {{ getActivityIcon(activity.type) }}
                  </mat-icon>
                </div>
                <div class="timeline-content">
                  <div class="activity-header">
                    <span class="activity-description">{{ activity.description }}</span>
                    <span class="activity-time">{{ formatTime(activity.timestamp) }}</span>
                  </div>
                  <div class="activity-meta" *ngIf="activity.actor">
                    by {{ activity.actor }}
                  </div>
                  <div class="activity-details" *ngIf="activity.metadata">
                    <span *ngFor="let key of objectKeys(activity.metadata)" class="detail-item">
                      {{ key }}: {{ activity.metadata[key] }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <div class="empty-state" *ngIf="activities.length === 0">
              <mat-icon>timeline</mat-icon>
              <p>No activities yet</p>
            </div>
          </div>
        </mat-tab>

        <!-- Beads Tab -->
        <mat-tab label="Beads">
          <div class="tab-content">
            <div class="beads-controls">
              <button mat-raised-button color="primary" (click)="spawnBeads()"
                      [disabled]="campaign?.status !== 'active'">
                <mat-icon>add</mat-icon>
                Spawn Beads
              </button>
              <button mat-button (click)="refreshBeads()">
                <mat-icon>refresh</mat-icon>
                Refresh
              </button>
            </div>

            <div class="beads-table" *ngIf="beads.length > 0">
              <table mat-table [dataSource]="beads" class="beads-mat-table">
                <ng-container matColumnDef="title">
                  <th mat-header-cell *matHeaderCellDef>Title</th>
                  <td mat-cell *matCellDef="let bead">{{ bead.title }}</td>
                </ng-container>

                <ng-container matColumnDef="status">
                  <th mat-header-cell *matHeaderCellDef>Status</th>
                  <td mat-cell *matCellDef="let bead">
                    <mat-chip [class]="'bead-status-chip bead-' + bead.status">
                      {{ bead.status }}
                    </mat-chip>
                  </td>
                </ng-container>

                <ng-container matColumnDef="priority">
                  <th mat-header-cell *matHeaderCellDef>Priority</th>
                  <td mat-cell *matCellDef="let bead">
                    <mat-icon [class]="'priority-icon priority-' + bead.priority">
                      {{ bead.priority === 0 ? 'priority_high' : 'low_priority' }}
                    </mat-icon>
                  </td>
                </ng-container>

                <ng-container matColumnDef="tags">
                  <th mat-header-cell *matHeaderCellDef>Tags</th>
                  <td mat-cell *matCellDef="let bead">
                    <mat-chip-set>
                      <mat-chip *ngFor="let tag of bead.tags">{{ tag }}</mat-chip>
                    </mat-chip-set>
                  </td>
                </ng-container>

                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef>Actions</th>
                  <td mat-cell *matCellDef="let bead">
                    <button mat-icon-button (click)="viewBead(bead)">
                      <mat-icon>visibility</mat-icon>
                    </button>
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="beadColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: beadColumns;"></tr>
              </table>
            </div>

            <div class="empty-state" *ngIf="beads.length === 0">
              <mat-icon>grain</mat-icon>
              <p>No beads spawned yet</p>
              <button mat-raised-button color="primary" (click)="spawnBeads()"
                      [disabled]="campaign?.status !== 'active'">
                <mat-icon>add</mat-icon>
                Spawn Beads
              </button>
            </div>
          </div>
        </mat-tab>

        <!-- Settings Tab -->
        <mat-tab label="Settings">
          <div class="tab-content">
            <mat-card>
              <h3>Campaign Configuration</h3>
              <div class="settings-grid">
                <div class="setting-item">
                  <label>Formula</label>
                  <span>{{ campaign?.formula_id || 'None' }}</span>
                </div>
                <div class="setting-item">
                  <label>Created</label>
                  <span>{{ formatDate(campaign?.created_at) }}</span>
                </div>
                <div class="setting-item">
                  <label>Created By</label>
                  <span>{{ campaign?.created_by || 'Unknown' }}</span>
                </div>
                <div class="setting-item">
                  <label>Priority</label>
                  <span>{{ campaign?.priority || 'normal' }}</span>
                </div>
                <div class="setting-item">
                  <label>Sync to HDIM</label>
                  <span>{{ campaign?.sync_to_hdim ? 'Yes' : 'No' }}</span>
                </div>
                <div class="setting-item" *ngIf="campaign?.hdim_campaign_tag">
                  <label>HDIM Tag</label>
                  <span>{{ campaign.hdim_campaign_tag }}</span>
                </div>
              </div>
            </mat-card>

            <mat-card class="target-segment-card" *ngIf="campaign?.target_segment">
              <h3>Target Segment</h3>
              <div class="settings-grid">
                <div class="setting-item" *ngIf="campaign.target_segment.tiers">
                  <label>Tiers</label>
                  <span>{{ campaign.target_segment.tiers.join(', ') }}</span>
                </div>
                <div class="setting-item" *ngIf="campaign.target_segment.sources">
                  <label>Sources</label>
                  <span>{{ campaign.target_segment.sources.join(', ') }}</span>
                </div>
                <div class="setting-item" *ngIf="campaign.target_segment.investor_types">
                  <label>Investor Types</label>
                  <span>{{ campaign.target_segment.investor_types.join(', ') }}</span>
                </div>
                <div class="setting-item" *ngIf="campaign.target_segment.industries">
                  <label>Industries</label>
                  <span>{{ campaign.target_segment.industries.join(', ') }}</span>
                </div>
              </div>
            </mat-card>

            <mat-card class="content-sources-card" *ngIf="campaign?.content_sources">
              <h3>Content Sources</h3>
              <div class="content-list">
                <div class="content-item" *ngFor="let item of objectKeys(campaign.content_sources)">
                  <mat-icon>description</mat-icon>
                  <div>
                    <div class="content-label">{{ item }}</div>
                    <div class="content-path">{{ campaign.content_sources[item] }}</div>
                  </div>
                </div>
              </div>
            </mat-card>
          </div>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: [`
    .campaign-detail-container {
      padding: 24px;
      max-width: 1400px;
      margin: 0 auto;
    }

    .campaign-header {
      background: white;
      border-radius: 8px;
      padding: 24px;
      margin-bottom: 24px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }

    .header-content {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }

    .title-section {
      display: flex;
      align-items: flex-start;
      gap: 16px;
    }

    .back-button {
      margin-top: 4px;
    }

    .campaign-header h1 {
      margin: 0;
      font-size: 28px;
      font-weight: 500;
    }

    .description {
      color: #666;
      margin: 8px 0 0 0;
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .status-chip {
      font-weight: 500;
    }

    .status-draft { background: #e3f2fd; color: #1976d2; }
    .status-active { background: #e8f5e9; color: #2e7d32; }
    .status-paused { background: #fff3e0; color: #f57c00; }
    .status-completed { background: #f3e5f5; color: #7b1fa2; }

    .type-chip {
      background: #f5f5f5;
      color: #616161;
    }

    .metrics-section {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }

    .metric-card {
      padding: 20px;
      text-align: center;
    }

    .metric-value {
      font-size: 36px;
      font-weight: 600;
      color: #1976d2;
      margin-bottom: 8px;
    }

    .metric-label {
      color: #666;
      font-size: 14px;
      margin-bottom: 12px;
    }

    .performance-section {
      margin-bottom: 24px;
    }

    .performance-section mat-card {
      padding: 24px;
    }

    .performance-section h3 {
      margin: 0 0 20px 0;
    }

    .performance-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 24px;
    }

    .performance-item {
      text-align: center;
    }

    .performance-label {
      color: #666;
      font-size: 14px;
      margin-bottom: 8px;
    }

    .performance-value {
      font-size: 32px;
      font-weight: 600;
      color: #1976d2;
      margin-bottom: 12px;
    }

    .rate-bar {
      height: 8px;
      border-radius: 4px;
    }

    .campaign-tabs {
      background: white;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }

    .tab-content {
      padding: 24px;
      min-height: 400px;
    }

    .timeline {
      position: relative;
      padding-left: 40px;
    }

    .timeline::before {
      content: '';
      position: absolute;
      left: 15px;
      top: 0;
      bottom: 0;
      width: 2px;
      background: #e0e0e0;
    }

    .timeline-item {
      position: relative;
      margin-bottom: 24px;
    }

    .timeline-marker {
      position: absolute;
      left: -40px;
      top: 0;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: white;
      border: 2px solid #1976d2;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .activity-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
      color: #1976d2;
    }

    .timeline-content {
      background: #f5f5f5;
      padding: 16px;
      border-radius: 8px;
    }

    .activity-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
    }

    .activity-description {
      font-weight: 500;
    }

    .activity-time {
      color: #666;
      font-size: 13px;
    }

    .activity-meta {
      color: #666;
      font-size: 13px;
      margin-bottom: 8px;
    }

    .activity-details {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .detail-item {
      background: white;
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 12px;
    }

    .beads-controls {
      display: flex;
      gap: 12px;
      margin-bottom: 24px;
    }

    .beads-mat-table {
      width: 100%;
    }

    .bead-status-chip {
      font-size: 12px;
    }

    .bead-pending { background: #e3f2fd; color: #1976d2; }
    .bead-ready { background: #e8f5e9; color: #2e7d32; }
    .bead-in_progress { background: #fff3e0; color: #f57c00; }
    .bead-completed { background: #f3e5f5; color: #7b1fa2; }
    .bead-blocked { background: #ffebee; color: #c62828; }

    .priority-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
    }

    .priority-0 { color: #c62828; }
    .priority-1 { color: #f57c00; }
    .priority-2 { color: #616161; }

    .empty-state {
      text-align: center;
      padding: 60px 20px;
      color: #999;
    }

    .empty-state mat-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      color: #ccc;
      margin-bottom: 16px;
    }

    .settings-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 20px;
    }

    .setting-item {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .setting-item label {
      font-size: 12px;
      color: #666;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .setting-item span {
      font-size: 16px;
      color: #333;
    }

    .target-segment-card,
    .content-sources-card {
      margin-top: 16px;
    }

    .content-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .content-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 12px;
      background: #f5f5f5;
      border-radius: 8px;
    }

    .content-item mat-icon {
      color: #1976d2;
    }

    .content-label {
      font-weight: 500;
      margin-bottom: 4px;
    }

    .content-path {
      font-size: 13px;
      color: #666;
    }
  `],
})
export class CampaignDetailComponent implements OnInit, OnDestroy {
  campaignId: string = '';
  campaign: any = null;
  metrics: any = null;
  beads: Bead[] = [];
  activities: Activity[] = [];

  beadColumns: string[] = ['title', 'status', 'priority', 'tags', 'actions'];

  private refreshSubscription?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private cxApiService: CxApiService
  ) {}

  async ngOnInit(): Promise<void> {
    this.campaignId = this.route.snapshot.paramMap.get('id') || '';
    if (!this.campaignId) {
      console.error('No campaign ID provided');
      this.router.navigate(['/campaigns']);
      return;
    }

    await this.loadCampaign();
    await this.loadMetrics();
    await this.loadBeads();
    await this.loadActivities();

    // Auto-refresh metrics every 30 seconds
    this.refreshSubscription = interval(30000)
      .pipe(switchMap(() => this.cxApiService.getCampaignMetrics(this.campaignId, true)))
      .subscribe({
        next: (metrics) => {
          this.metrics = metrics;
          console.info('Metrics refreshed', this.campaignId);
        },
        error: (err) => console.error('Failed to refresh metrics', err),
      });
  }

  ngOnDestroy(): void {
    this.refreshSubscription?.unsubscribe();
  }

  async loadCampaign(): Promise<void> {
    try {
      this.campaign = await this.cxApiService.getCampaign(this.campaignId).toPromise();
      console.info('Campaign loaded', this.campaignId);
    } catch (error) {
      console.error('Failed to load campaign', error);
    }
  }

  async loadMetrics(): Promise<void> {
    try {
      this.metrics = await this.cxApiService.getCampaignMetrics(this.campaignId).toPromise();
      console.info('Metrics loaded', this.campaignId);
    } catch (error) {
      console.error('Failed to load metrics', error);
    }
  }

  async loadBeads(): Promise<void> {
    try {
      const beads = await this.cxApiService.getCampaignBeads(this.campaignId).toPromise();
      this.beads = beads || [];
      console.info('Beads loaded', { count: this.beads.length });
    } catch (error) {
      console.error('Failed to load beads', error);
    }
  }

  async loadActivities(): Promise<void> {
    // TODO: Implement activity loading from backend
    // For now, generate mock timeline from campaign data
    this.activities = [];

    if (this.campaign) {
      this.activities.push({
        id: '1',
        type: 'created',
        description: 'Campaign created',
        timestamp: this.campaign.created_at,
        actor: this.campaign.created_by,
      });

      if (this.campaign.status !== 'draft') {
        this.activities.push({
          id: '2',
          type: 'started',
          description: 'Campaign started',
          timestamp: this.campaign.updated_at,
          actor: this.campaign.created_by,
        });
      }
    }

    console.info('Activities loaded', { count: this.activities.length });
  }

  async startCampaign(): Promise<void> {
    try {
      await this.cxApiService.startCampaign(this.campaignId).toPromise();
      console.info('Campaign started', this.campaignId);
      await this.loadCampaign();
      await this.loadMetrics();
    } catch (error) {
      console.error('Failed to start campaign', error);
    }
  }

  async pauseCampaign(): Promise<void> {
    try {
      await this.cxApiService.pauseCampaign(this.campaignId).toPromise();
      console.info('Campaign paused', this.campaignId);
      await this.loadCampaign();
    } catch (error) {
      console.error('Failed to pause campaign', error);
    }
  }

  async resumeCampaign(): Promise<void> {
    try {
      await this.cxApiService.resumeCampaign(this.campaignId).toPromise();
      console.info('Campaign resumed', this.campaignId);
      await this.loadCampaign();
    } catch (error) {
      console.error('Failed to resume campaign', error);
    }
  }

  async completeCampaign(): Promise<void> {
    try {
      await this.cxApiService.completeCampaign(this.campaignId).toPromise();
      console.info('Campaign completed', this.campaignId);
      await this.loadCampaign();
    } catch (error) {
      console.error('Failed to complete campaign', error);
    }
  }

  async archiveCampaign(): Promise<void> {
    try {
      await this.cxApiService.archiveCampaign(this.campaignId).toPromise();
      console.info('Campaign archived', this.campaignId);
      this.router.navigate(['/campaigns']);
    } catch (error) {
      console.error('Failed to archive campaign', error);
    }
  }

  editCampaign(): void {
    this.router.navigate(['/campaigns', this.campaignId, 'edit']);
  }

  async cloneCampaign(): Promise<void> {
    const newName = `${this.campaign.name} (Copy)`;
    try {
      const cloned = await this.cxApiService.cloneCampaign(this.campaignId, newName).toPromise();
      console.info('Campaign cloned', { original: this.campaignId, cloned: cloned.id });
      this.router.navigate(['/campaigns', cloned.id]);
    } catch (error) {
      console.error('Failed to clone campaign', error);
    }
  }

  async spawnBeads(): Promise<void> {
    console.info('Spawning beads', this.campaignId);
    // TODO: Implement bead spawning API call
    alert('Bead spawning not yet implemented');
  }

  async refreshBeads(): Promise<void> {
    await this.loadBeads();
  }

  viewBead(bead: Bead): void {
    console.info('View bead', bead.id);
    // TODO: Navigate to bead detail or open modal
    alert(`View bead: ${bead.title}`);
  }

  goBack(): void {
    this.router.navigate(['/campaigns']);
  }

  getActivityIcon(type: string): string {
    const icons: Record<string, string> = {
      created: 'add_circle',
      started: 'play_arrow',
      paused: 'pause',
      resumed: 'play_arrow',
      completed: 'check_circle',
      email_sent: 'email',
      response: 'reply',
      meeting: 'event',
      note: 'note',
    };
    return icons[type] || 'info';
  }

  formatTime(timestamp: string): string {
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
  }

  formatDate(timestamp: string): string {
    if (!timestamp) return 'N/A';
    return new Date(timestamp).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }

  objectKeys(obj: any): string[] {
    return obj ? Object.keys(obj) : [];
  }
}
