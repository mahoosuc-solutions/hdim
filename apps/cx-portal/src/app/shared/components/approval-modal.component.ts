import { Component, EventEmitter, Input, Output, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PendingAction, ApprovalDecision } from '../models';

@Component({
  selector: 'cx-approval-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="modal-overlay" *ngIf="isOpen" (click)="onOverlayClick($event)">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h2>Approve External Communication</h2>
          <button class="close-btn" (click)="close()">&times;</button>
        </div>

        <div class="modal-body" *ngIf="action">
          <!-- Action Type Badge -->
          <div class="action-type-badge" [class]="'type-' + action.action_type">
            {{ getActionTypeLabel(action.action_type) }}
          </div>

          <!-- Lead Information -->
          <div class="info-section">
            <h3>Recipient</h3>
            <div class="recipient-info">
              <div class="recipient-name">{{ action.lead_name }}</div>
              <div class="recipient-email" *ngIf="action.metadata?.['to']">
                <span class="label">To:</span> {{ action.metadata?.['to'] }}
              </div>
              <div class="recipient-cc" *ngIf="action.metadata?.['cc']">
                <span class="label">CC:</span> {{ action.metadata?.['cc'] }}
              </div>
            </div>
          </div>

          <!-- Subject -->
          <div class="info-section">
            <h3>Subject</h3>
            <input
              type="text"
              [(ngModel)]="editedSubject"
              [readonly]="!isEditMode"
              [class.editable]="isEditMode"
              class="subject-input"
            />
          </div>

          <!-- Content -->
          <div class="info-section">
            <h3>Message</h3>
            <textarea
              [(ngModel)]="editedContent"
              [readonly]="!isEditMode"
              [class.editable]="isEditMode"
              class="content-textarea"
              rows="12"
            ></textarea>
          </div>

          <!-- Metadata -->
          <div class="info-section" *ngIf="action.metadata">
            <h3>Additional Details</h3>
            <div class="metadata-grid">
              <div class="metadata-item" *ngIf="action.scheduled_for">
                <span class="label">Scheduled:</span>
                <span class="value">{{ action.scheduled_for | date:'short' }}</span>
              </div>
              <div class="metadata-item" *ngIf="action.metadata?.['meeting_duration']">
                <span class="label">Duration:</span>
                <span class="value">{{ action.metadata?.['meeting_duration'] }} minutes</span>
              </div>
              <div class="metadata-item" *ngIf="action.metadata?.['linkedin_url']">
                <span class="label">LinkedIn:</span>
                <a [href]="action.metadata?.['linkedin_url']" target="_blank" class="value link">
                  View Profile
                </a>
              </div>
            </div>
          </div>

          <!-- Notes -->
          <div class="info-section">
            <h3>Approval Notes (Optional)</h3>
            <textarea
              [(ngModel)]="approvalNotes"
              placeholder="Add notes about this decision..."
              class="notes-textarea"
              rows="3"
            ></textarea>
          </div>

          <!-- Warning -->
          <div class="warning-box" *ngIf="!isEditMode">
            <strong>⚠️ Human Review Required</strong><br>
            This message will be sent to <strong>{{ action.lead_name }}</strong>.
            Please review carefully before approving.
          </div>

          <div class="edit-warning-box" *ngIf="isEditMode">
            <strong>✏️ Edit Mode</strong><br>
            Make your changes above, then click "Save & Approve" to send the edited version.
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-secondary" (click)="close()">
            Cancel
          </button>
          <button
            class="btn-danger"
            (click)="reject()"
            [disabled]="isProcessing"
          >
            {{ isProcessing && lastAction === 'reject' ? 'Rejecting...' : 'Reject' }}
          </button>
          <button
            class="btn-edit"
            *ngIf="!isEditMode"
            (click)="enableEditMode()"
          >
            Edit Message
          </button>
          <button
            class="btn-primary"
            *ngIf="isEditMode"
            (click)="approveWithEdits()"
            [disabled]="isProcessing"
          >
            {{ isProcessing && lastAction === 'edit' ? 'Saving...' : 'Save & Approve' }}
          </button>
          <button
            class="btn-success"
            *ngIf="!isEditMode"
            (click)="approve()"
            [disabled]="isProcessing"
          >
            {{ isProcessing && lastAction === 'approve' ? 'Approving...' : 'Approve & Send' }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 2000;
      padding: 20px;
    }

    .modal-content {
      background: white;
      border-radius: 12px;
      max-width: 900px;
      width: 100%;
      max-height: 90vh;
      display: flex;
      flex-direction: column;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 24px;
      border-bottom: 1px solid #e0e0e0;
    }

    .modal-header h2 {
      margin: 0;
      color: #1a1a2e;
      font-size: 24px;
    }

    .close-btn {
      background: none;
      border: none;
      font-size: 32px;
      color: #999;
      cursor: pointer;
      padding: 0;
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      line-height: 1;
    }

    .close-btn:hover {
      color: #333;
    }

    .modal-body {
      padding: 24px;
      overflow-y: auto;
      flex: 1;
    }

    .action-type-badge {
      display: inline-block;
      padding: 8px 16px;
      border-radius: 20px;
      font-size: 14px;
      font-weight: 600;
      text-transform: uppercase;
      margin-bottom: 24px;
    }

    .type-email {
      background: #e3f2fd;
      color: #1976d2;
    }

    .type-linkedin {
      background: #e8eaf6;
      color: #3f51b5;
    }

    .type-meeting_request {
      background: #f3e5f5;
      color: #7b1fa2;
    }

    .type-phone_call {
      background: #e8f5e9;
      color: #388e3c;
    }

    .info-section {
      margin-bottom: 24px;
    }

    .info-section h3 {
      margin: 0 0 12px 0;
      font-size: 14px;
      color: #666;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      font-weight: 600;
    }

    .recipient-info {
      background: #f5f7fa;
      padding: 16px;
      border-radius: 8px;
    }

    .recipient-name {
      font-size: 18px;
      font-weight: 600;
      color: #1a1a2e;
      margin-bottom: 8px;
    }

    .recipient-email,
    .recipient-cc {
      font-size: 14px;
      color: #666;
      margin-bottom: 4px;
    }

    .label {
      font-weight: 600;
      color: #999;
    }

    .subject-input {
      width: 100%;
      padding: 12px;
      border: 1px solid #e0e0e0;
      border-radius: 6px;
      font-size: 16px;
      font-weight: 600;
      color: #1a1a2e;
      background: #f5f7fa;
    }

    .subject-input.editable {
      background: white;
      border-color: #4361ee;
    }

    .subject-input:focus {
      outline: none;
      border-color: #4361ee;
      box-shadow: 0 0 0 3px rgba(67, 97, 238, 0.1);
    }

    .content-textarea,
    .notes-textarea {
      width: 100%;
      padding: 12px;
      border: 1px solid #e0e0e0;
      border-radius: 6px;
      font-size: 14px;
      line-height: 1.6;
      color: #1a1a2e;
      background: #f5f7fa;
      font-family: inherit;
      resize: vertical;
    }

    .content-textarea.editable {
      background: white;
      border-color: #4361ee;
    }

    .content-textarea:focus,
    .notes-textarea:focus {
      outline: none;
      border-color: #4361ee;
      box-shadow: 0 0 0 3px rgba(67, 97, 238, 0.1);
    }

    .notes-textarea {
      background: white;
    }

    .metadata-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
    }

    .metadata-item {
      background: #f5f7fa;
      padding: 12px;
      border-radius: 6px;
    }

    .metadata-item .value {
      color: #1a1a2e;
      font-weight: 500;
    }

    .metadata-item .value.link {
      color: #4361ee;
      text-decoration: none;
    }

    .metadata-item .value.link:hover {
      text-decoration: underline;
    }

    .warning-box,
    .edit-warning-box {
      padding: 16px;
      border-radius: 8px;
      margin-top: 24px;
      font-size: 14px;
    }

    .warning-box {
      background: #fff3e0;
      border: 2px solid #f57c00;
      color: #e65100;
    }

    .edit-warning-box {
      background: #e3f2fd;
      border: 2px solid #1976d2;
      color: #0d47a1;
    }

    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      padding: 24px;
      border-top: 1px solid #e0e0e0;
    }

    .btn-secondary,
    .btn-primary,
    .btn-danger,
    .btn-success,
    .btn-edit {
      padding: 12px 24px;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: all 0.2s;
    }

    .btn-secondary {
      background: #f5f7fa;
      color: #666;
    }

    .btn-secondary:hover {
      background: #e0e0e0;
    }

    .btn-danger {
      background: #dc3545;
      color: white;
    }

    .btn-danger:hover {
      background: #c82333;
    }

    .btn-edit {
      background: #ff9800;
      color: white;
    }

    .btn-edit:hover {
      background: #f57c00;
    }

    .btn-success {
      background: #28a745;
      color: white;
    }

    .btn-success:hover {
      background: #218838;
    }

    .btn-primary {
      background: #4361ee;
      color: white;
    }

    .btn-primary:hover {
      background: #3651d4;
    }

    .btn-primary:disabled,
    .btn-success:disabled,
    .btn-danger:disabled {
      background: #ccc;
      cursor: not-allowed;
    }
  `],
})
export class ApprovalModalComponent implements OnChanges {
  @Input() action: PendingAction | null = null;
  @Input() isOpen = false;
  @Output() closed = new EventEmitter<void>();
  @Output() decision = new EventEmitter<ApprovalDecision>();

  isEditMode = false;
  isProcessing = false;
  lastAction: 'approve' | 'reject' | 'edit' = 'approve';
  editedSubject = '';
  editedContent = '';
  approvalNotes = '';

  ngOnChanges(): void {
    if (this.action) {
      this.editedSubject = this.action.subject || '';
      this.editedContent = this.action.content || '';
      this.isEditMode = false;
      this.approvalNotes = '';
    }
  }

  getActionTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      email: '📧 Email',
      linkedin: '💼 LinkedIn Message',
      meeting_request: '📅 Meeting Request',
      phone_call: '📞 Phone Call',
    };
    return labels[type] || type;
  }

  enableEditMode(): void {
    this.isEditMode = true;
  }

  approve(): void {
    if (!this.action) return;
    this.isProcessing = true;
    this.lastAction = 'approve';

    this.decision.emit({
      action_id: this.action.id,
      decision: 'approve',
      notes: this.approvalNotes || undefined,
    });
  }

  approveWithEdits(): void {
    if (!this.action) return;
    this.isProcessing = true;
    this.lastAction = 'edit';

    this.decision.emit({
      action_id: this.action.id,
      decision: 'edit',
      edited_content: this.editedContent,
      notes: this.approvalNotes || undefined,
    });
  }

  reject(): void {
    if (!this.action) return;
    this.isProcessing = true;
    this.lastAction = 'reject';

    this.decision.emit({
      action_id: this.action.id,
      decision: 'reject',
      notes: this.approvalNotes || undefined,
    });
  }

  close(): void {
    this.isProcessing = false;
    this.isEditMode = false;
    this.approvalNotes = '';
    this.closed.emit();
  }

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-overlay')) {
      this.close();
    }
  }
}
