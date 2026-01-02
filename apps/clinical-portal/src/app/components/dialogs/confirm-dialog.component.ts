import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

/**
 * Confirmation Dialog Data Interface
 */
export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  confirmColor?: 'primary' | 'accent' | 'warn';
  icon?: string;
  iconColor?: string;
}

/**
 * Reusable Confirmation Dialog Component
 *
 * Usage:
 * ```typescript
 * const dialogRef = this.dialog.open(ConfirmDialogComponent, {
 *   data: {
 *     title: 'Delete Report?',
 *     message: 'Are you sure you want to delete this report? This action cannot be undone.',
 *     confirmText: 'Delete',
 *     cancelText: 'Cancel',
 *     confirmColor: 'warn',
 *     icon: 'warning',
 *     iconColor: '#f44336'
 *   }
 * });
 *
 * dialogRef.afterClosed().subscribe((confirmed: boolean) => {
 *   if (confirmed) {
 *     // User confirmed
 *   }
 * });
 * ```
 */
@Component({
  selector: 'app-confirm-dialog',
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <div class="confirm-dialog">
      @if (data.icon) {
        <div class="dialog-icon" [style.color]="data.iconColor || '#ff9800'">
          <mat-icon>{{ data.icon }}</mat-icon>
        </div>
      }

      <h2 mat-dialog-title>{{ data.title }}</h2>

      <mat-dialog-content>
        <p [innerHTML]="data.message"></p>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button (click)="onCancel()">
          {{ data.cancelText || 'Cancel' }}
        </button>
        <button
          mat-raised-button
          [color]="data.confirmColor || 'primary'"
          (click)="onConfirm()"
        >
          {{ data.confirmText || 'Confirm' }}
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .confirm-dialog {
        min-width: 400px;
        max-width: 500px;
      }

      .dialog-icon {
        text-align: center;
        margin-bottom: 16px;
        mat-icon {
          font-size: 48px;
          width: 48px;
          height: 48px;
        }
      }

      h2[mat-dialog-title] {
        text-align: center;
        margin: 0 0 16px 0;
        font-size: 20px;
        font-weight: 600;
        color: #1a1a1a;
      }

      mat-dialog-content {
        padding: 0 24px 24px;
        text-align: center;
        p {
          margin: 0;
          font-size: 14px;
          color: #666;
          line-height: 1.6;
        }
      }

      mat-dialog-actions {
        padding: 16px 24px;
        border-top: 1px solid #e0e0e0;
        button {
          margin-left: 8px;
        }
      }
    `,
  ],
})
export class ConfirmDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData
  ) {}

  onConfirm(): void {
    this.dialogRef.close(true);
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
