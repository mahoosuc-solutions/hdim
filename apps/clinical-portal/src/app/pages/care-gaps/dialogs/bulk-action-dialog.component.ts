import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';

/**
 * Bulk Action Dialog Component
 *
 * Issue #241: Care Gap Bulk Actions
 * Provides a unified dialog for performing bulk operations on care gaps:
 * - Bulk Close: Close multiple gaps with single closure reason
 * - Bulk Intervention: Assign intervention to multiple gaps
 * - Bulk Priority Update: Change priority for multiple gaps
 */

export type BulkActionType = 'close' | 'assign-intervention' | 'update-priority';

export interface BulkActionDialogData {
  actionType: BulkActionType;
  selectedCount: number;
  gapIds: string[];
}

export interface BulkActionResult {
  success: boolean;
  actionType: BulkActionType;
  formData: any;
  gapIds: string[];
}

@Component({
  selector: 'app-bulk-action-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatProgressBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>{{ getIcon() }}</mat-icon>
      {{ getTitle() }}
    </h2>

    <mat-dialog-content>
      <div class="dialog-info">
        <mat-icon>info</mat-icon>
        <p>{{ selectedCount }} care gap(s) selected</p>
      </div>

      <form [formGroup]="actionForm">
        <!-- Bulk Close Form -->
        <ng-container *ngIf="data.actionType === 'close'">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Closure Reason</mat-label>
            <mat-select formControlName="closureReason" required>
              <mat-option value="completed">Care Gap Completed</mat-option>
              <mat-option value="not-applicable">Not Applicable</mat-option>
              <mat-option value="patient-declined">Patient Declined</mat-option>
              <mat-option value="other">Other</mat-option>
            </mat-select>
            <mat-hint>Select the primary reason for closing these care gaps</mat-hint>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Notes (Optional)</mat-label>
            <textarea matInput formControlName="notes" rows="3"
                      placeholder="Add any additional notes about this closure..."></textarea>
            <mat-hint>These notes will be applied to all selected gaps</mat-hint>
          </mat-form-field>
        </ng-container>

        <!-- Bulk Intervention Assignment Form -->
        <ng-container *ngIf="data.actionType === 'assign-intervention'">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Intervention Type</mat-label>
            <mat-select formControlName="interventionType" required>
              <mat-option value="OUTREACH">Member Outreach Letter</mat-option>
              <mat-option value="REMINDER">Reminder/Follow-up Call</mat-option>
              <mat-option value="EDUCATION">Patient Education Material</mat-option>
              <mat-option value="REFERRAL">Specialist Referral</mat-option>
              <mat-option value="APPOINTMENT_SCHEDULED">Schedule Appointment</mat-option>
              <mat-option value="MEDICATION_REVIEW">Medication Review</mat-option>
              <mat-option value="OTHER">Other</mat-option>
            </mat-select>
            <mat-hint>Select the type of intervention to assign</mat-hint>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Description</mat-label>
            <textarea matInput formControlName="description" rows="2" required
                      placeholder="Describe the intervention plan..."></textarea>
            <mat-hint>This description will apply to all selected gaps</mat-hint>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Target Date (Optional)</mat-label>
            <input matInput type="date" formControlName="scheduledDate">
            <mat-hint>When should this intervention be completed?</mat-hint>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Assigned To (Optional)</mat-label>
            <input matInput formControlName="assignedTo"
                   placeholder="care-coordinator@example.com">
            <mat-hint>User or team responsible for this intervention</mat-hint>
          </mat-form-field>
        </ng-container>

        <!-- Bulk Priority Update Form -->
        <ng-container *ngIf="data.actionType === 'update-priority'">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>New Priority</mat-label>
            <mat-select formControlName="priority" required>
              <mat-option value="CRITICAL">
                <mat-icon class="priority-icon critical">warning</mat-icon>
                Critical
              </mat-option>
              <mat-option value="HIGH">
                <mat-icon class="priority-icon high">error</mat-icon>
                High
              </mat-option>
              <mat-option value="MEDIUM">
                <mat-icon class="priority-icon medium">priority_high</mat-icon>
                Medium
              </mat-option>
              <mat-option value="LOW">
                <mat-icon class="priority-icon low">info</mat-icon>
                Low
              </mat-option>
            </mat-select>
            <mat-hint>All selected gaps will be updated to this priority</mat-hint>
          </mat-form-field>

          <div class="priority-info">
            <mat-icon>lightbulb</mat-icon>
            <p>Priority changes will affect the order gaps appear in lists and reports.</p>
          </div>
        </ng-container>
      </form>

      <!-- Progress indicator -->
      <mat-progress-bar *ngIf="processing" mode="indeterminate"></mat-progress-bar>

      <!-- Error summary -->
      <div *ngIf="errorSummary" class="error-summary">
        <mat-icon>error</mat-icon>
        <p class="error-text">{{ errorSummary }}</p>
      </div>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()" [disabled]="processing">
        Cancel
      </button>
      <button mat-raised-button color="primary"
              (click)="onSubmit()"
              [disabled]="actionForm.invalid || processing">
        <mat-icon>{{ getSubmitIcon() }}</mat-icon>
        {{ getSubmitButtonText() }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2[mat-dialog-title] {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    h2 mat-icon {
      font-size: 28px;
      width: 28px;
      height: 28px;
    }

    .dialog-info {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 16px;
      padding: 12px;
      background-color: #e3f2fd;
      border-radius: 4px;
      border-left: 4px solid #2196f3;
    }

    .dialog-info mat-icon {
      color: #1976d2;
    }

    .dialog-info p {
      margin: 0;
      font-weight: 500;
      color: #1565c0;
    }

    .full-width {
      width: 100%;
      margin-bottom: 16px;
    }

    .priority-info {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      margin-top: 8px;
      padding: 12px;
      background-color: #fff3e0;
      border-radius: 4px;
    }

    .priority-info mat-icon {
      color: #f57c00;
      margin-top: 2px;
    }

    .priority-info p {
      margin: 0;
      color: #e65100;
      font-size: 0.9em;
    }

    .priority-icon {
      margin-right: 8px;
      font-size: 18px;
      width: 18px;
      height: 18px;
      vertical-align: middle;
    }

    .priority-icon.critical { color: #d32f2f; }
    .priority-icon.high { color: #f57c00; }
    .priority-icon.medium { color: #fbc02d; }
    .priority-icon.low { color: #7cb342; }

    .error-summary {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      margin-top: 16px;
      padding: 12px;
      background-color: #ffebee;
      border-left: 4px solid #f44336;
      border-radius: 4px;
    }

    .error-summary mat-icon {
      color: #c62828;
      margin-top: 2px;
    }

    .error-text {
      color: #c62828;
      margin: 0;
      flex: 1;
    }

    mat-dialog-actions button mat-icon {
      margin-right: 4px;
      font-size: 18px;
      width: 18px;
      height: 18px;
    }
  `]
})
export class BulkActionDialogComponent {
  actionForm: FormGroup;
  processing = false;
  errorSummary: string | null = null;

  get selectedCount(): number {
    return this.data.selectedCount;
  }

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<BulkActionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: BulkActionDialogData
  ) {
    this.actionForm = this.createForm(data.actionType);
  }

  private createForm(actionType: BulkActionType): FormGroup {
    switch (actionType) {
      case 'close':
        return this.fb.group({
          closureReason: ['', Validators.required],
          notes: ['']
        });
      case 'assign-intervention':
        return this.fb.group({
          interventionType: ['', Validators.required],
          description: ['', Validators.required],
          scheduledDate: [''],
          assignedTo: ['']
        });
      case 'update-priority':
        return this.fb.group({
          priority: ['', Validators.required]
        });
    }
  }

  getTitle(): string {
    switch (this.data.actionType) {
      case 'close':
        return 'Close Care Gaps';
      case 'assign-intervention':
        return 'Assign Intervention';
      case 'update-priority':
        return 'Update Priority';
    }
  }

  getIcon(): string {
    switch (this.data.actionType) {
      case 'close':
        return 'close';
      case 'assign-intervention':
        return 'assignment';
      case 'update-priority':
        return 'flag';
    }
  }

  getSubmitIcon(): string {
    switch (this.data.actionType) {
      case 'close':
        return 'check_circle';
      case 'assign-intervention':
        return 'send';
      case 'update-priority':
        return 'done';
    }
  }

  getSubmitButtonText(): string {
    if (this.processing) return 'Processing...';

    switch (this.data.actionType) {
      case 'close':
        return `Close ${this.selectedCount} Gap(s)`;
      case 'assign-intervention':
        return `Assign to ${this.selectedCount} Gap(s)`;
      case 'update-priority':
        return `Update ${this.selectedCount} Gap(s)`;
    }
  }

  onSubmit(): void {
    if (this.actionForm.invalid) return;

    const result: BulkActionResult = {
      success: true,
      actionType: this.data.actionType,
      formData: this.actionForm.value,
      gapIds: this.data.gapIds
    };

    this.dialogRef.close(result);
  }

  onCancel(): void {
    this.dialogRef.close(null);
  }
}
