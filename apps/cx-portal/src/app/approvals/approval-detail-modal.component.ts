import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';

import { CxApiService } from '../shared/services/cx-api.service';
import { PendingAction, ApprovalDecision, Urgency } from '../shared/models';

@Component({
  selector: 'app-approval-detail-modal',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatChipsModule,
    MatDividerModule,
  ],
  templateUrl: './approval-detail-modal.component.html',
  styleUrls: ['./approval-detail-modal.component.scss'],
})
export class ApprovalDetailModalComponent {
  editMode = false;
  processing = false;

  // Editable fields
  editedSubject: string;
  editedContent: string;
  decisionNotes = '';

  constructor(
    @Inject(MAT_DIALOG_DATA) public action: PendingAction,
    private dialogRef: MatDialogRef<ApprovalDetailModalComponent>,
    private cxApi: CxApiService
  ) {
    this.editedSubject = action.subject || '';
    this.editedContent = action.content;
  }

  /**
   * Toggle edit mode
   */
  toggleEditMode(): void {
    this.editMode = !this.editMode;
    if (!this.editMode) {
      // Reset to original values
      this.editedSubject = this.action.subject || '';
      this.editedContent = this.action.content;
    }
  }

  /**
   * Approve action (execute as-is)
   */
  approve(): void {
    const decision: ApprovalDecision = {
      action_id: this.action.id,
      decision: 'approve',
      decision_notes: this.decisionNotes || 'Approved',
    };

    this.submitDecision(decision, 'approved');
  }

  /**
   * Edit and approve (update content, then execute)
   */
  editAndApprove(): void {
    if (!this.editMode) {
      this.editMode = true;
      return;
    }

    const decision: ApprovalDecision = {
      action_id: this.action.id,
      decision: 'edit',
      decision_notes: this.decisionNotes || 'Edited and approved',
      edited_subject: this.editedSubject,
      edited_content: this.editedContent,
    };

    this.submitDecision(decision, 'edited');
  }

  /**
   * Reject action (do not execute)
   */
  reject(): void {
    const decision: ApprovalDecision = {
      action_id: this.action.id,
      decision: 'reject',
      decision_notes: this.decisionNotes || 'Rejected',
    };

    this.submitDecision(decision, 'rejected');
  }

  /**
   * Submit approval decision to API
   */
  private submitDecision(decision: ApprovalDecision, resultType: string): void {
    this.processing = true;
    this.cxApi.submitApprovalDecision(decision).subscribe({
      next: () => {
        this.dialogRef.close(resultType);
      },
      error: (err) => {
        console.error('Failed to submit decision:', err);
        this.processing = false;
      },
    });
  }

  /**
   * Cancel and close modal
   */
  cancel(): void {
    this.dialogRef.close();
  }

  /**
   * Get urgency badge color
   */
  getUrgencyColor(urgency: Urgency): string {
    switch (urgency) {
      case 'urgent':
        return 'warn';
      case 'normal':
        return 'primary';
      case 'low':
        return 'accent';
    }
  }

  /**
   * Format action type for display
   */
  formatActionType(actionType: string): string {
    return actionType.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
  }

  /**
   * Check if content has been modified
   */
  isModified(): boolean {
    return (
      this.editedSubject !== (this.action.subject || '') ||
      this.editedContent !== this.action.content
    );
  }
}
