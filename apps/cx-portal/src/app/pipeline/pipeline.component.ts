import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { Subscription } from 'rxjs';
import { CxApiService } from '../shared/services/cx-api.service';
import { WebSocketService } from '../shared/services/websocket.service';
import { Lead, PipelineView } from '../shared/models';
import { LeadModalComponent } from '../shared/components/lead-modal.component';
import { AddLeadModalComponent } from '../shared/components/add-lead-modal.component';

/**
 * Sales Pipeline Component
 *
 * Kanban-style view of the sales pipeline.
 */
@Component({
  selector: 'cx-pipeline',
  standalone: true,
  imports: [CommonModule, DragDropModule, LeadModalComponent, AddLeadModalComponent],
  template: `
    <div class="pipeline-container">
      <header class="page-header">
        <h1>Sales Pipeline</h1>
        <button class="btn-primary" (click)="addLead()" title="Create a new lead">
          + Add Lead
        </button>
      </header>

      <!-- Tips Section -->
      <div class="tips-banner" *ngIf="showTips">
        <div class="tip-icon">💡</div>
        <div class="tip-content">
          <strong>Quick Tips:</strong>
          <span class="tip-item">• <strong>Drag cards</strong> between stages to update status</span>
          <span class="tip-item">• <strong>Click any card</strong> to view/edit details</span>
          <span class="tip-item">• <strong>Add Lead</strong> button creates new opportunities</span>
        </div>
        <button class="tip-close" (click)="showTips = false" title="Hide tips">&times;</button>
      </div>

      <div class="pipeline-board" *ngIf="pipelineData" cdkDropListGroup>
        <div class="pipeline-column" *ngFor="let stage of stages">
          <div class="column-header">
            <h3>{{ stage.label }}</h3>
            <span class="column-count">{{ getStageCount(stage.key) }}</span>
          </div>
          <div
            class="column-content"
            cdkDropList
            [cdkDropListData]="pipelineData.pipeline[stage.key] || []"
            [id]="stage.key"
            (cdkDropListDropped)="onDrop($event, stage.key)"
          >
            <div
              class="lead-card"
              *ngFor="let lead of pipelineData.pipeline[stage.key] || []"
              cdkDrag
              (click)="viewLead(lead)"
              title="Click to view details • Drag to change stage"
            >
              <div class="drag-handle" title="Drag to move">⋮⋮</div>
              <div class="lead-content">
                <div class="lead-header">
                  <span class="lead-name">{{ lead.name }}</span>
                  <span class="lead-tier tier-{{ lead.tier?.toLowerCase() }}">{{ lead.tier }}</span>
                </div>
                <div class="lead-company">{{ lead.company }}</div>
                <div class="lead-footer" *ngIf="lead.deal_size">
                  <span class="lead-value">\${{ lead.deal_size | number:'1.0-0' }}</span>
                  <span class="lead-probability">{{ lead.close_probability }}%</span>
                </div>
              </div>
            </div>
            <div class="empty-column" *ngIf="!pipelineData.pipeline[stage.key]?.length">
              No leads
            </div>
          </div>
        </div>
      </div>

      <div class="loading" *ngIf="!pipelineData && !error">
        Loading pipeline...
      </div>

      <div class="error" *ngIf="error">
        {{ error }}
      </div>
    </div>

    <cx-lead-modal
      [lead]="selectedLead"
      [isOpen]="isModalOpen"
      (closed)="closeModal()"
      (saved)="onLeadSaved($event)"
    ></cx-lead-modal>

    <cx-add-lead-modal
      [isOpen]="isAddModalOpen"
      (closed)="closeAddModal()"
      (created)="onLeadCreated($event)"
    ></cx-add-lead-modal>
  `,
  styles: [`
    .pipeline-container {
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

    .btn-primary:hover {
      background: #3651d4;
    }

    /* Tips Banner */
    .tips-banner {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      padding: 16px 20px;
      border-radius: 12px;
      margin-bottom: 24px;
      display: flex;
      align-items: center;
      gap: 16px;
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
      animation: slideDown 0.3s ease-out;
    }

    @keyframes slideDown {
      from {
        opacity: 0;
        transform: translateY(-20px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .tip-icon {
      font-size: 32px;
      flex-shrink: 0;
    }

    .tip-content {
      flex: 1;
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      align-items: center;
    }

    .tip-content strong {
      font-size: 16px;
      margin-right: 8px;
    }

    .tip-item {
      font-size: 14px;
      opacity: 0.95;
    }

    .tip-close {
      background: rgba(255, 255, 255, 0.2);
      border: none;
      color: white;
      font-size: 24px;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: background 0.2s;
      flex-shrink: 0;
    }

    .tip-close:hover {
      background: rgba(255, 255, 255, 0.3);
    }

    .pipeline-board {
      display: flex;
      gap: 8px;
      overflow-x: auto;
      padding-bottom: 16px;
    }

    .pipeline-column {
      width: 140px !important;
      min-width: 140px !important;
      max-width: 140px !important;
      flex-shrink: 0 !important;
      flex-grow: 0 !important;
      background: #f5f7fa;
      border-radius: 8px;
      display: flex;
      flex-direction: column;
    }

    .column-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px;
      border-bottom: 1px solid #e0e0e0;
    }

    .column-header h3 {
      margin: 0;
      font-size: 11px;
      color: #666;
      text-transform: uppercase;
      font-weight: 600;
    }

    .column-count {
      background: #e0e0e0;
      padding: 4px 8px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 600;
    }

    .column-content {
      padding: 8px;
      flex: 1;
      overflow-y: auto;
      max-height: calc(100vh - 200px);
    }

    .lead-card {
      background: white;
      border-radius: 8px;
      padding: 12px;
      padding-left: 8px;
      margin-bottom: 8px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
      cursor: pointer;
      transition: transform 0.2s, box-shadow 0.2s;
      position: relative;
      display: flex;
      gap: 8px;
    }

    .lead-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .lead-card:hover .drag-handle {
      opacity: 1;
    }

    .drag-handle {
      color: #999;
      font-size: 16px;
      line-height: 1;
      opacity: 0.3;
      transition: opacity 0.2s;
      cursor: grab;
      flex-shrink: 0;
      align-self: flex-start;
      padding: 4px 0;
    }

    .drag-handle:active {
      cursor: grabbing;
    }

    .lead-content {
      flex: 1;
      min-width: 0;
    }

    .lead-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
    }

    .lead-name {
      font-weight: 600;
      color: #1a1a2e;
      font-size: 13px;
    }

    .lead-tier {
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

    .lead-company {
      font-size: 12px;
      color: #666;
      margin-bottom: 6px;
    }

    .lead-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 8px;
      border-top: 1px solid #eee;
    }

    .lead-value {
      font-weight: 600;
      color: #4361ee;
    }

    .lead-probability {
      font-size: 12px;
      color: #999;
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

    /* CDK Drag and Drop Styles */
    .cdk-drag-preview {
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
      opacity: 0.9;
      border-radius: 8px;
      background: white;
    }

    .cdk-drag-placeholder {
      opacity: 0.3;
      background: #f0f0f0;
      border: 2px dashed #ccc;
    }

    .cdk-drag-animating {
      transition: transform 250ms cubic-bezier(0, 0, 0.2, 1);
    }

    .column-content.cdk-drop-list-dragging .lead-card:not(.cdk-drag-placeholder) {
      transition: transform 250ms cubic-bezier(0, 0, 0.2, 1);
    }

    .cdk-drop-list-receiving {
      background: #e3f2fd;
      border-radius: 8px;
    }
  `],
})
export class PipelineComponent implements OnInit, OnDestroy {
  pipelineData: PipelineView | null = null;
  error: string | null = null;
  selectedLead: Lead | null = null;
  isModalOpen = false;
  isAddModalOpen = false;
  showTips = true;
  private wsSubscription?: Subscription;

  stages = [
    { key: 'new', label: 'New' },
    { key: 'contacted', label: 'Contacted' },
    { key: 'engaged', label: 'Engaged' },
    { key: 'qualified', label: 'Qualified' },
    { key: 'demo_scheduled', label: 'Demo Scheduled' },
    { key: 'demo_completed', label: 'Demo Completed' },
    { key: 'proposal_sent', label: 'Proposal' },
    { key: 'negotiation', label: 'Negotiation' },
  ];

  constructor(
    private cxApi: CxApiService,
    private wsService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.loadPipeline();
    this.connectWebSocket();
  }

  ngOnDestroy(): void {
    this.disconnectWebSocket();
  }

  private connectWebSocket(): void {
    this.wsService.connect();
    this.wsSubscription = this.wsService.messages$.subscribe({
      next: (message) => {
        console.log('Pipeline update received:', message);
        // Reload pipeline when any update is received
        if (['pipeline_update', 'lead_created', 'lead_updated', 'lead_deleted'].includes(message.type)) {
          this.loadPipeline();
        }
      },
      error: (err) => {
        console.error('WebSocket subscription error:', err);
      },
    });
  }

  private disconnectWebSocket(): void {
    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe();
    }
    this.wsService.disconnect();
  }

  loadPipeline(): void {
    this.cxApi.getPipelineView().subscribe({
      next: (data) => {
        this.pipelineData = data;
        this.error = null;
      },
      error: (err) => {
        this.error = 'Failed to load pipeline. Is the CX API running?';
        console.error('Pipeline error:', err);
      },
    });
  }

  getStageCount(stage: string): number {
    return this.pipelineData?.stage_totals[stage]?.count || 0;
  }

  addLead(): void {
    this.isAddModalOpen = true;
  }

  closeAddModal(): void {
    this.isAddModalOpen = false;
  }

  onLeadCreated(newLead: Lead): void {
    // Reload pipeline to show new lead
    this.loadPipeline();
    this.closeAddModal();
  }

  viewLead(pipelineItem: any): void {
    // Fetch full lead details from API
    this.cxApi.getLead(pipelineItem.id).subscribe({
      next: (fullLead) => {
        this.selectedLead = fullLead;
        this.isModalOpen = true;
      },
      error: (err) => {
        console.error('Failed to load lead details', err);
        alert('Failed to load lead details. Please try again.');
      },
    });
  }

  closeModal(): void {
    this.isModalOpen = false;
    this.selectedLead = null;
  }

  onLeadSaved(updatedLead: Lead): void {
    // Reload pipeline to reflect changes
    this.loadPipeline();
    this.closeModal();
  }

  onDrop(event: CdkDragDrop<any[]>, targetStage: string): void {
    const lead = event.item.data || event.previousContainer.data[event.previousIndex];
    const previousStage = event.previousContainer.id;

    // If dropped in same column, just reorder (no API call needed)
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
      return;
    }

    // Move item between arrays
    transferArrayItem(
      event.previousContainer.data,
      event.container.data,
      event.previousIndex,
      event.currentIndex
    );

    // Update lead status via API
    this.cxApi.updateLead(lead.id, { status: targetStage as any }).subscribe({
      next: () => {
        // Reload pipeline to get fresh data and counts
        this.loadPipeline();
      },
      error: (err) => {
        console.error('Failed to update lead status', err);
        // Revert the move on error
        transferArrayItem(
          event.container.data,
          event.previousContainer.data,
          event.currentIndex,
          event.previousIndex
        );
        alert('Failed to update lead. Please try again.');
      },
    });
  }
}
