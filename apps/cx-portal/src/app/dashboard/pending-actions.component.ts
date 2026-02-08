import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PendingAction, ApprovalDecision } from '../shared/models';
import { ApprovalModalComponent } from '../shared/components/approval-modal.component';
import { CxApiService } from '../shared/services/cx-api.service';

@Component({
  selector: 'cx-pending-actions',
  standalone: true,
  imports: [CommonModule, ApprovalModalComponent],
  template: `
    <div class="pending-actions-container">
      <header class="section-header">
        <h2>Pending Approvals</h2>
        <span class="count-badge" *ngIf="pendingActions.length > 0">
          {{ pendingActions.length }}
        </span>
      </header>

      <div class="actions-list" *ngIf="pendingActions.length > 0">
        <div
          class="action-card"
          *ngFor="let action of pendingActions"
          (click)="openApprovalModal(action)"
        >
          <div class="action-header">
            <div class="action-type-badge" [class]="'type-' + action.action_type">
              {{ getActionTypeIcon(action.action_type) }}
            </div>
            <div class="action-meta">
              <div class="action-lead">{{ action.lead_name }}</div>
              <div class="action-time">{{ action.created_at | date:'short' }}</div>
            </div>
            <button class="review-btn">Review</button>
          </div>
          <div class="action-subject">{{ action.subject }}</div>
          <div class="action-preview">{{ action.content | slice:0:150 }}...</div>
        </div>
      </div>

      <div class="empty-state" *ngIf="pendingActions.length === 0">
        <div class="empty-icon">✅</div>
        <div class="empty-title">All Caught Up!</div>
        <div class="empty-message">
          No pending approvals at the moment. New communication requests will appear here.
        </div>
      </div>

      <div class="error" *ngIf="error">
        {{ error }}
      </div>
    </div>

    <cx-approval-modal
      [action]="selectedAction"
      [isOpen]="isModalOpen"
      (closed)="closeModal()"
      (decision)="onDecision($event)"
    ></cx-approval-modal>
  `,
  styles: [`
    .pending-actions-container {
      background: white;
      border-radius: 12px;
      padding: 24px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }

    .section-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 20px;
    }

    .section-header h2 {
      margin: 0;
      font-size: 20px;
      color: #1a1a2e;
    }

    .count-badge {
      background: #dc3545;
      color: white;
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 14px;
      font-weight: 600;
    }

    .actions-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .action-card {
      border: 2px solid #e0e0e0;
      border-radius: 8px;
      padding: 16px;
      cursor: pointer;
      transition: all 0.2s;
    }

    .action-card:hover {
      border-color: #4361ee;
      box-shadow: 0 4px 12px rgba(67, 97, 238, 0.15);
      transform: translateY(-2px);
    }

    .action-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;
    }

    .action-type-badge {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      flex-shrink: 0;
    }

    .type-email {
      background: #e3f2fd;
    }

    .type-linkedin {
      background: #e8eaf6;
    }

    .type-meeting_request {
      background: #f3e5f5;
    }

    .type-phone_call {
      background: #e8f5e9;
    }

    .action-meta {
      flex: 1;
    }

    .action-lead {
      font-weight: 600;
      color: #1a1a2e;
      font-size: 16px;
    }

    .action-time {
      font-size: 12px;
      color: #999;
      margin-top: 4px;
    }

    .review-btn {
      background: #4361ee;
      color: white;
      border: none;
      padding: 8px 16px;
      border-radius: 6px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s;
    }

    .review-btn:hover {
      background: #3651d4;
    }

    .action-subject {
      font-weight: 600;
      color: #1a1a2e;
      margin-bottom: 8px;
      font-size: 15px;
    }

    .action-preview {
      font-size: 14px;
      color: #666;
      line-height: 1.5;
    }

    .empty-state {
      text-align: center;
      padding: 64px 24px;
    }

    .empty-icon {
      font-size: 64px;
      margin-bottom: 16px;
    }

    .empty-title {
      font-size: 24px;
      font-weight: 600;
      color: #1a1a2e;
      margin-bottom: 8px;
    }

    .empty-message {
      font-size: 16px;
      color: #666;
    }

    .error {
      text-align: center;
      padding: 24px;
      color: #dc3545;
      background: #fee;
      border-radius: 8px;
    }
  `],
})
export class PendingActionsComponent implements OnInit {
  pendingActions: PendingAction[] = [];
  selectedAction: PendingAction | null = null;
  isModalOpen = false;
  error: string | null = null;

  constructor(private cxApi: CxApiService) {}

  ngOnInit(): void {
    this.loadPendingActions();
  }

  loadPendingActions(): void {
    this.cxApi.getPendingActions().subscribe({
      next: (actions) => {
        this.pendingActions = actions;
        this.error = null;
      },
      error: (err) => {
        this.error = 'Failed to load pending actions';
        console.error('Error loading pending actions:', err);
      },
    });
  }

  getActionTypeIcon(type: string): string {
    const icons: Record<string, string> = {
      email: '📧',
      linkedin: '💼',
      meeting_request: '📅',
      phone_call: '📞',
    };
    return icons[type] || '📝';
  }

  openApprovalModal(action: PendingAction): void {
    this.selectedAction = action;
    this.isModalOpen = true;
  }

  closeModal(): void {
    this.isModalOpen = false;
    this.selectedAction = null;
  }

  onDecision(decision: ApprovalDecision): void {
    this.cxApi.submitApprovalDecision(decision).subscribe({
      next: () => {
        // Remove the action from the list
        this.pendingActions = this.pendingActions.filter(
          (a) => a.id !== decision.action_id
        );
        this.closeModal();

        // Show success message based on decision
        const messages: Record<string, string> = {
          approve: 'Message approved and sent successfully',
          edit: 'Message edited and sent successfully',
          reject: 'Message rejected successfully',
        };
        alert(messages[decision.decision] || 'Action completed');
      },
      error: (err) => {
        console.error('Failed to submit decision:', err);
        alert('Failed to submit decision. Please try again.');
        this.closeModal();
      },
    });
  }
}
