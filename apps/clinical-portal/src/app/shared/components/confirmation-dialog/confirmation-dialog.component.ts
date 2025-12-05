/**
 * Confirmation Dialog Component
 *
 * Reusable confirmation dialog using MatDialog.
 * Supports dangerous actions with red confirm button.
 *
 * @example
 * const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
 *   data: {
 *     title: 'Delete Patient',
 *     message: 'Are you sure you want to delete this patient?',
 *     confirmText: 'Delete',
 *     cancelText: 'Cancel',
 *     isDangerous: true
 *   }
 * });
 *
 * dialogRef.afterClosed().subscribe(result => {
 *   if (result) {
 *     // User confirmed
 *   }
 * });
 */
import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

/**
 * Dialog data interface
 */
export interface ConfirmationDialogData {
  /** Dialog title */
  title: string;
  /** Confirmation message */
  message: string;
  /** Confirm button text */
  confirmText?: string;
  /** Cancel button text */
  cancelText?: string;
  /** Is this a dangerous action (red button) */
  isDangerous?: boolean;
  /** Icon to display */
  icon?: string;
}

@Component({
  selector: 'app-confirmation-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <div class="confirmation-dialog">
      <h2 mat-dialog-title class="dialog-title">
        <mat-icon *ngIf="data.icon" [class.danger-icon]="data.isDangerous">
          {{ data.icon }}
        </mat-icon>
        {{ data.title }}
      </h2>

      <mat-dialog-content class="dialog-content">
        <p>{{ data.message }}</p>
      </mat-dialog-content>

      <mat-dialog-actions align="end" class="dialog-actions">
        <button
          mat-button
          (click)="onCancel()"
          cdkFocusInitial>
          {{ data.cancelText || 'Cancel' }}
        </button>
        <button
          mat-raised-button
          [color]="data.isDangerous ? 'warn' : 'primary'"
          (click)="onConfirm()"
          [class.danger-button]="data.isDangerous">
          {{ data.confirmText || 'Confirm' }}
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .confirmation-dialog {
      min-width: 300px;
      max-width: 500px;
    }

    .dialog-title {
      display: flex;
      align-items: center;
      gap: 12px;
      margin: 0;
      padding: 20px 24px 16px;
      font-size: 20px;
      font-weight: 500;
    }

    .dialog-title mat-icon {
      font-size: 28px;
      width: 28px;
      height: 28px;
    }

    .danger-icon {
      color: #f44336;
    }

    .dialog-content {
      padding: 0 24px;
      margin-bottom: 16px;
      color: rgba(0, 0, 0, 0.6);
      line-height: 1.6;
    }

    .dialog-content p {
      margin: 0;
      font-size: 14px;
    }

    .dialog-actions {
      padding: 12px 24px 20px;
      gap: 8px;
    }

    .danger-button {
      background-color: #f44336 !important;
      color: white !important;
    }

    .danger-button:hover {
      background-color: #d32f2f !important;
    }

    /* Dark theme */
    @media (prefers-color-scheme: dark) {
      .dialog-content {
        color: rgba(255, 255, 255, 0.7);
      }
    }

    /* Responsive */
    @media (max-width: 600px) {
      .confirmation-dialog {
        min-width: 280px;
      }

      .dialog-title {
        font-size: 18px;
        padding: 16px 20px 12px;
      }

      .dialog-content {
        padding: 0 20px;
        font-size: 13px;
      }

      .dialog-actions {
        padding: 12px 20px 16px;
      }
    }
  `]
})
export class ConfirmationDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmationDialogData
  ) {
    // Set default icon if not provided
    if (!this.data.icon) {
      this.data.icon = this.data.isDangerous ? 'warning' : 'help_outline';
    }
  }

  onConfirm(): void {
    this.dialogRef.close(true);
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
