import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { Subject, takeUntil } from 'rxjs';
import { SalesService } from '../../../services/sales.service';
import { Opportunity, OpportunityStage, PipelineKanban, KanbanStage, PipelineMetrics } from '../../../models/sales.model';

@Component({
  selector: 'app-sales-pipeline',
  standalone: true,
  imports: [CommonModule, FormsModule, DragDropModule],
  template: `
    <div class="pipeline-page">
      <div class="page-header">
        <div class="header-content">
          <h2>Sales Pipeline</h2>
          <p class="subtitle">Drag and drop opportunities between stages</p>
        </div>
        <div class="header-actions">
          <button class="btn btn-secondary" (click)="refreshData()">Refresh</button>
        </div>
      </div>

      <!-- Pipeline Metrics Summary -->
      <div class="metrics-bar" *ngIf="metrics()">
        <div class="metric">
          <span class="metric-value">{{ metrics()!.totalPipeline | currency:'USD':'symbol':'1.0-0' }}</span>
          <span class="metric-label">Total Pipeline</span>
        </div>
        <div class="metric">
          <span class="metric-value">{{ metrics()!.weightedPipeline | currency:'USD':'symbol':'1.0-0' }}</span>
          <span class="metric-label">Weighted</span>
        </div>
        <div class="metric">
          <span class="metric-value">{{ metrics()!.averageDealSize | currency:'USD':'symbol':'1.0-0' }}</span>
          <span class="metric-label">Avg Deal Size</span>
        </div>
        <div class="metric">
          <span class="metric-value">{{ (metrics()!.winRate * 100) | number:'1.0-0' }}%</span>
          <span class="metric-label">Win Rate</span>
        </div>
        <div class="metric">
          <span class="metric-value">{{ metrics()!.averageSalesCycle }}</span>
          <span class="metric-label">Avg Days to Close</span>
        </div>
      </div>

      <!-- Loading State -->
      <div class="loading" *ngIf="isLoading()">
        <div class="spinner"></div>
        <span>Loading pipeline...</span>
      </div>

      <!-- Kanban Board -->
      <div class="kanban-board" *ngIf="!isLoading() && kanban()">
        <div
          class="kanban-column"
          *ngFor="let stage of stages"
          [class]="stage.toLowerCase()"
        >
          <div class="column-header">
            <div class="column-title">
              <span class="stage-icon">{{ getStageIcon(stage) }}</span>
              <span class="stage-name">{{ formatStage(stage) }}</span>
            </div>
            <div class="column-stats">
              <span class="count">{{ getStageData(stage).count }}</span>
              <span class="value">{{ getStageData(stage).value | currency:'USD':'symbol':'1.0-0' }}</span>
            </div>
          </div>

          <div
            class="column-content"
            cdkDropList
            [cdkDropListData]="getStageOpportunities(stage)"
            [id]="stage"
            [cdkDropListConnectedTo]="stages"
            (cdkDropListDropped)="onDrop($event, stage)"
          >
            <div
              class="opportunity-card"
              *ngFor="let opp of getStageOpportunities(stage)"
              cdkDrag
              [cdkDragData]="opp"
              (click)="selectOpportunity(opp)"
            >
              <div class="card-header">
                <span class="opp-name">{{ opp.name }}</span>
                <span class="opp-amount">{{ opp.amount | currency:'USD':'symbol':'1.0-0' }}</span>
              </div>
              <div class="card-body">
                <div class="card-row">
                  <span class="label">Probability:</span>
                  <span class="probability">{{ opp.probability }}%</span>
                </div>
                <div class="card-row">
                  <span class="label">Close Date:</span>
                  <span class="close-date" [class.overdue]="isOverdue(opp.closeDate)">
                    {{ opp.closeDate | date:'shortDate' }}
                  </span>
                </div>
                <div class="card-row" *ngIf="opp.nextStep">
                  <span class="next-step">{{ opp.nextStep }}</span>
                </div>
              </div>
              <div class="card-footer" *ngIf="opp.ownerId">
                <div class="owner-avatar">{{ opp.ownerId.charAt(0).toUpperCase() }}</div>
              </div>
            </div>

            <div class="empty-column" *ngIf="!getStageOpportunities(stage).length">
              No opportunities
            </div>
          </div>
        </div>
      </div>

      <!-- Opportunity Detail Panel -->
      <div class="detail-panel" *ngIf="selectedOpp()" (click)="selectedOpp.set(null)">
        <div class="detail-content" (click)="$event.stopPropagation()">
          <div class="detail-header">
            <h3>{{ selectedOpp()!.name }}</h3>
            <button class="close-btn" (click)="selectedOpp.set(null)">×</button>
          </div>

          <div class="detail-body">
            <div class="detail-section">
              <div class="detail-row large">
                <span class="label">Amount:</span>
                <span class="value">{{ selectedOpp()!.amount | currency:'USD':'symbol':'1.0-0' }}</span>
              </div>
              <div class="detail-row">
                <span class="label">Stage:</span>
                <select [(ngModel)]="selectedOpp()!.stage" (change)="updateStage(selectedOpp()!)">
                  <option *ngFor="let stage of stages" [value]="stage">
                    {{ formatStage(stage) }}
                  </option>
                </select>
              </div>
              <div class="detail-row">
                <span class="label">Probability:</span>
                <span>{{ selectedOpp()!.probability }}%</span>
              </div>
              <div class="detail-row">
                <span class="label">Close Date:</span>
                <span [class.overdue]="isOverdue(selectedOpp()!.closeDate)">
                  {{ selectedOpp()!.closeDate | date:'mediumDate' }}
                </span>
              </div>
            </div>

            <div class="detail-section" *ngIf="selectedOpp()!.description">
              <h4>Description</h4>
              <p>{{ selectedOpp()!.description }}</p>
            </div>

            <div class="detail-section" *ngIf="selectedOpp()!.nextStep">
              <h4>Next Step</h4>
              <p>{{ selectedOpp()!.nextStep }}</p>
            </div>

            <div class="detail-section">
              <h4>Timeline</h4>
              <div class="timeline">
                <div class="timeline-item">
                  <span class="timeline-date">{{ selectedOpp()!.createdAt | date:'shortDate' }}</span>
                  <span class="timeline-event">Created</span>
                </div>
                <div class="timeline-item" *ngIf="selectedOpp()!.closedAt">
                  <span class="timeline-date">{{ selectedOpp()!.closedAt | date:'shortDate' }}</span>
                  <span class="timeline-event">Closed</span>
                </div>
              </div>
            </div>
          </div>

          <div class="detail-actions">
            <button class="btn btn-secondary" (click)="markAsLost(selectedOpp()!)">
              Mark as Lost
            </button>
            <button class="btn btn-primary" (click)="markAsWon(selectedOpp()!)">
              Mark as Won
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .pipeline-page {
      max-width: 100%;
      margin: 0 auto;
      overflow-x: hidden;
    }

    .page-header {
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

    .btn-secondary {
      background: #f5f5f5;
      color: #333;
      border: 1px solid #ddd;
    }

    .metrics-bar {
      display: flex;
      gap: 24px;
      padding: 20px 24px;
      background: white;
      border-radius: 12px;
      margin-bottom: 24px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      flex-wrap: wrap;
    }

    .metric {
      display: flex;
      flex-direction: column;
    }

    .metric-value {
      font-size: 20px;
      font-weight: 700;
      color: #1a237e;
    }

    .metric-label {
      font-size: 12px;
      color: #666;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .kanban-board {
      display: flex;
      gap: 16px;
      overflow-x: auto;
      padding-bottom: 20px;
      min-height: 600px;
    }

    .kanban-column {
      flex: 0 0 280px;
      background: #f5f7fa;
      border-radius: 12px;
      display: flex;
      flex-direction: column;
    }

    .column-header {
      padding: 16px;
      border-bottom: 1px solid #e0e0e0;
    }

    .column-title {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
    }

    .stage-icon {
      font-size: 18px;
    }

    .stage-name {
      font-weight: 600;
      color: #333;
    }

    .column-stats {
      display: flex;
      justify-content: space-between;
      font-size: 13px;
      color: #666;
    }

    .column-stats .count {
      background: rgba(0, 0, 0, 0.1);
      padding: 2px 8px;
      border-radius: 12px;
    }

    .column-content {
      flex: 1;
      padding: 12px;
      display: flex;
      flex-direction: column;
      gap: 12px;
      min-height: 200px;
    }

    .opportunity-card {
      background: white;
      border-radius: 8px;
      padding: 16px;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
      cursor: grab;
      transition: box-shadow 0.2s ease, transform 0.2s ease;
    }

    .opportunity-card:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    .opportunity-card:active {
      cursor: grabbing;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 12px;
    }

    .opp-name {
      font-weight: 600;
      color: #333;
      font-size: 14px;
    }

    .opp-amount {
      font-weight: 700;
      color: #1a237e;
      font-size: 14px;
    }

    .card-body {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .card-row {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
    }

    .card-row .label {
      color: #666;
    }

    .probability {
      font-weight: 500;
      color: #333;
    }

    .close-date {
      color: #333;
    }

    .close-date.overdue {
      color: #c62828;
      font-weight: 500;
    }

    .next-step {
      color: #1565c0;
      font-style: italic;
    }

    .card-footer {
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid #eee;
    }

    .owner-avatar {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: #e3f2fd;
      color: #1a237e;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      font-size: 12px;
    }

    .empty-column {
      padding: 24px;
      text-align: center;
      color: #999;
      font-style: italic;
      font-size: 13px;
    }

    /* Drag and Drop Styles */
    .cdk-drag-preview {
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
      border-radius: 8px;
    }

    .cdk-drag-placeholder {
      opacity: 0.3;
    }

    .cdk-drag-animating {
      transition: transform 250ms cubic-bezier(0, 0, 0.2, 1);
    }

    .column-content.cdk-drop-list-dragging .opportunity-card:not(.cdk-drag-placeholder) {
      transition: transform 250ms cubic-bezier(0, 0, 0.2, 1);
    }

    /* Stage Colors */
    .kanban-column.discovery .column-header { border-top: 4px solid #2196f3; }
    .kanban-column.demo .column-header { border-top: 4px solid #ff9800; }
    .kanban-column.proposal .column-header { border-top: 4px solid #e91e63; }
    .kanban-column.negotiation .column-header { border-top: 4px solid #9c27b0; }
    .kanban-column.contract .column-header { border-top: 4px solid #009688; }
    .kanban-column.closed_won .column-header { border-top: 4px solid #4caf50; }
    .kanban-column.closed_lost .column-header { border-top: 4px solid #f44336; }

    /* Detail Panel */
    .detail-panel {
      position: fixed;
      top: 0;
      right: 0;
      bottom: 0;
      width: 100%;
      background: rgba(0, 0, 0, 0.3);
      display: flex;
      justify-content: flex-end;
      z-index: 1000;
    }

    .detail-content {
      width: 480px;
      background: white;
      height: 100%;
      overflow-y: auto;
      box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
    }

    .detail-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 24px;
      border-bottom: 1px solid #eee;
    }

    .detail-header h3 {
      margin: 0;
    }

    .close-btn {
      background: none;
      border: none;
      font-size: 24px;
      cursor: pointer;
      color: #666;
    }

    .detail-body {
      padding: 24px;
    }

    .detail-section {
      margin-bottom: 24px;
    }

    .detail-section h4 {
      margin: 0 0 12px 0;
      color: #1a237e;
      font-size: 14px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .detail-row {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      border-bottom: 1px solid #f5f5f5;
    }

    .detail-row.large .value {
      font-size: 24px;
      font-weight: 700;
      color: #1a237e;
    }

    .detail-row select {
      padding: 6px 10px;
      border: 1px solid #ddd;
      border-radius: 4px;
    }

    .timeline {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .timeline-item {
      display: flex;
      gap: 12px;
    }

    .timeline-date {
      color: #666;
      font-size: 13px;
      min-width: 80px;
    }

    .timeline-event {
      color: #333;
    }

    .detail-actions {
      padding: 24px;
      border-top: 1px solid #eee;
      display: flex;
      gap: 12px;
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
export class SalesPipelineComponent implements OnInit, OnDestroy {
  private readonly salesService = inject(SalesService);
  private destroy$ = new Subject<void>();

  kanban = signal<PipelineKanban | null>(null);
  metrics = signal<PipelineMetrics | null>(null);
  selectedOpp = signal<Opportunity | null>(null);
  isLoading = signal(false);

  stages: OpportunityStage[] = ['DISCOVERY', 'DEMO', 'PROPOSAL', 'NEGOTIATION', 'CONTRACT', 'CLOSED_WON', 'CLOSED_LOST'];

  private opportunitiesByStage = new Map<OpportunityStage, Opportunity[]>();

  ngOnInit(): void {
    this.loadPipeline();
    this.loadMetrics();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadPipeline(): void {
    this.isLoading.set(true);

    this.salesService.getPipelineKanban()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.kanban.set(data);
          this.updateOpportunitiesMap(data);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
        },
      });
  }

  loadMetrics(): void {
    this.salesService.getPipelineMetrics()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => this.metrics.set(data),
      });
  }

  refreshData(): void {
    this.loadPipeline();
    this.loadMetrics();
  }

  private updateOpportunitiesMap(data: PipelineKanban): void {
    this.opportunitiesByStage.clear();
    data.stages.forEach((stage) => {
      this.opportunitiesByStage.set(stage.stage, [...stage.opportunities]);
    });
  }

  getStageData(stage: OpportunityStage): KanbanStage {
    const kanbanData = this.kanban();
    if (!kanbanData) {
      return { stage, opportunities: [], count: 0, value: 0 };
    }
    return kanbanData.stages.find((s) => s.stage === stage) || { stage, opportunities: [], count: 0, value: 0 };
  }

  getStageOpportunities(stage: OpportunityStage): Opportunity[] {
    return this.opportunitiesByStage.get(stage) || [];
  }

  onDrop(event: CdkDragDrop<Opportunity[]>, newStage: OpportunityStage): void {
    const opportunity: Opportunity = event.item.data;

    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex
      );

      // Update the opportunity stage on the server
      this.salesService.updateOpportunityStage({
        opportunityId: opportunity.id,
        newStage,
      }).pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            opportunity.stage = newStage;
            this.loadMetrics(); // Refresh metrics after stage change
          },
          error: () => {
            // Revert on error
            this.loadPipeline();
          },
        });
    }
  }

  selectOpportunity(opp: Opportunity): void {
    this.selectedOpp.set(opp);
  }

  updateStage(opp: Opportunity): void {
    this.salesService.updateOpportunityStage({
      opportunityId: opp.id,
      newStage: opp.stage,
    }).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loadPipeline();
          this.loadMetrics();
        },
      });
  }

  markAsWon(opp: Opportunity): void {
    this.salesService.updateOpportunityStage({
      opportunityId: opp.id,
      newStage: 'CLOSED_WON',
    }).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.selectedOpp.set(null);
          this.loadPipeline();
          this.loadMetrics();
        },
      });
  }

  markAsLost(opp: Opportunity): void {
    this.salesService.updateOpportunityStage({
      opportunityId: opp.id,
      newStage: 'CLOSED_LOST',
    }).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.selectedOpp.set(null);
          this.loadPipeline();
          this.loadMetrics();
        },
      });
  }

  isOverdue(closeDate: string): boolean {
    return new Date(closeDate) < new Date();
  }

  formatStage(stage: string): string {
    return stage.replace('_', ' ').toLowerCase()
      .replace(/\b\w/g, (l) => l.toUpperCase());
  }

  getStageIcon(stage: OpportunityStage): string {
    const icons: Record<OpportunityStage, string> = {
      DISCOVERY: '🔍',
      DEMO: '💻',
      PROPOSAL: '📄',
      NEGOTIATION: '🤝',
      CONTRACT: '📝',
      CLOSED_WON: '🏆',
      CLOSED_LOST: '❌',
    };
    return icons[stage];
  }
}
