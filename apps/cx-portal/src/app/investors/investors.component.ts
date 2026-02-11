import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CxApiService } from '../shared/services/cx-api.service';
import { Investor, PipelineView } from '../shared/models';

/**
 * Investors Component
 *
 * Kanban-style view of investor pipeline.
 */
@Component({
  selector: 'cx-investors',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="investors-container">
      <header class="page-header">
        <h1>Investor Pipeline</h1>
        <button class="btn-primary" (click)="addInvestor()">+ Add Investor</button>
      </header>

      <div class="stats-bar" *ngIf="stats">
        <div class="stat">
          <span class="stat-value">{{ stats.total }}</span>
          <span class="stat-label">Total</span>
        </div>
        <div class="stat">
          <span class="stat-value highlight">{{ stats.engaged }}</span>
          <span class="stat-label">Engaged</span>
        </div>
        <div class="stat">
          <span class="stat-value success">{{ stats.committed }}</span>
          <span class="stat-label">Committed</span>
        </div>
      </div>

      <div class="pipeline-board" *ngIf="pipelineData">
        <div class="pipeline-column" *ngFor="let stage of stages">
          <div class="column-header" [ngClass]="stage.key">
            <h3>{{ stage.label }}</h3>
            <span class="column-count">{{ getStageCount(stage.key) }}</span>
          </div>
          <div class="column-content">
            <div
              class="investor-card"
              *ngFor="let investor of pipelineData.pipeline[stage.key] || []"
            >
              <div class="investor-header">
                <span class="investor-name">{{ investor.name }}</span>
                <span class="investor-tier tier-{{ investor.tier?.toLowerCase() }}">{{ investor.tier }}</span>
              </div>
              <div class="investor-org">{{ investor.organization }}</div>
              <div class="investor-type">{{ investor.investor_type?.toUpperCase() }}</div>
            </div>
            <div class="empty-column" *ngIf="!pipelineData.pipeline[stage.key]?.length">
              No investors
            </div>
          </div>
        </div>
      </div>

      <div class="loading" *ngIf="!pipelineData && !error">
        Loading investors...
      </div>

      <div class="error" *ngIf="error">
        {{ error }}
      </div>
    </div>
  `,
  styles: [`
    .investors-container {
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

    .btn-primary {
      background: #4361ee;
      color: white;
      border: none;
      padding: 10px 20px;
      border-radius: 8px;
      cursor: pointer;
      font-size: 14px;
    }

    .stats-bar {
      display: flex;
      gap: 24px;
      margin-bottom: 24px;
      padding: 16px 24px;
      background: white;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
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

    .stat-label {
      font-size: 12px;
      color: #666;
    }

    .pipeline-board {
      display: flex;
      gap: 16px;
      overflow-x: auto;
      padding-bottom: 16px;
    }

    .pipeline-column {
      min-width: 260px;
      background: #f5f7fa;
      border-radius: 12px;
      display: flex;
      flex-direction: column;
    }

    .column-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      border-bottom: 2px solid;
    }

    .column-header.identified {
      border-color: #90caf9;
    }

    .column-header.researching {
      border-color: #a5d6a7;
    }

    .column-header.outreach {
      border-color: #ffcc80;
    }

    .column-header.engaged {
      border-color: #f48fb1;
    }

    .column-header.committed {
      border-color: #2ecc71;
    }

    .column-header.passed {
      border-color: #e57373;
    }

    .column-header h3 {
      margin: 0;
      font-size: 14px;
      color: #666;
      text-transform: uppercase;
    }

    .column-count {
      background: #e0e0e0;
      padding: 4px 8px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 600;
    }

    .column-content {
      padding: 16px;
      flex: 1;
      overflow-y: auto;
      max-height: calc(100vh - 280px);
    }

    .investor-card {
      background: white;
      border-radius: 8px;
      padding: 16px;
      margin-bottom: 12px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
      cursor: pointer;
      transition: transform 0.2s, box-shadow 0.2s;
    }

    .investor-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .investor-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
    }

    .investor-name {
      font-weight: 600;
      color: #1a1a2e;
    }

    .investor-tier {
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 600;
    }

    .tier-a {
      background: #e8f5e9;
      color: #2e7d32;
    }

    .tier-b {
      background: #fff3e0;
      color: #ef6c00;
    }

    .tier-c {
      background: #fce4ec;
      color: #c2185b;
    }

    .investor-org {
      font-size: 13px;
      color: #666;
    }

    .investor-type {
      font-size: 11px;
      color: #999;
      margin-top: 8px;
      padding-top: 8px;
      border-top: 1px solid #eee;
    }

    .empty-column {
      text-align: center;
      color: #999;
      padding: 24px;
    }

    .loading, .error {
      text-align: center;
      padding: 48px;
      color: #666;
    }

    .error {
      color: #dc3545;
    }
  `],
})
export class InvestorsComponent implements OnInit {
  pipelineData: PipelineView | null = null;
  stats: any = null;
  error: string | null = null;

  stages = [
    { key: 'identified', label: 'Identified' },
    { key: 'researching', label: 'Researching' },
    { key: 'outreach', label: 'Outreach' },
    { key: 'engaged', label: 'Engaged' },
    { key: 'committed', label: 'Committed' },
    { key: 'passed', label: 'Passed' },
  ];

  constructor(private cxApi: CxApiService) {}

  ngOnInit(): void {
    this.loadPipeline();
    this.loadStats();
  }

  loadPipeline(): void {
    this.cxApi.getInvestorPipeline().subscribe({
      next: (data) => {
        this.pipelineData = data;
        this.error = null;
      },
      error: (err) => {
        this.error = 'Failed to load investor pipeline. Is the CX API running?';
        console.error('Investor pipeline error:', err);
      },
    });
  }

  loadStats(): void {
    this.cxApi.getInvestorStats().subscribe({
      next: (stats) => {
        this.stats = stats;
      },
      error: (err) => {
        console.error('Failed to load investor stats:', err);
      },
    });
  }

  getStageCount(stage: string): number {
    return this.pipelineData?.stage_totals[stage]?.count || 0;
  }

  addInvestor(): void {
    // TODO: Open add investor modal
    console.log('Add investor clicked');
  }
}
