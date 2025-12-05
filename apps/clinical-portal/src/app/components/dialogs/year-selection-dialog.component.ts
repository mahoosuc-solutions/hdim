import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';

/**
 * Year Selection Dialog Component
 *
 * Allows users to select a year for generating population reports.
 *
 * Usage:
 * ```typescript
 * const dialogRef = this.dialog.open(YearSelectionDialogComponent);
 * dialogRef.afterClosed().subscribe((year: number | null) => {
 *   if (year) {
 *     // Generate population report for selected year
 *   }
 * });
 * ```
 */
@Component({
  selector: 'app-year-selection-dialog',
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
  ],
  template: `
    <div class="dialog-container">
      <h2 mat-dialog-title>
        <mat-icon>calendar_today</mat-icon>
        Select Reporting Year
      </h2>

      <mat-dialog-content>
        <p class="description">
          Choose a year for which to generate the population quality report.
        </p>

        <mat-form-field appearance="outline" class="year-field">
          <mat-label>Report Year</mat-label>
          <mat-select [(ngModel)]="selectedYear" placeholder="Select year">
            @for (year of availableYears; track year) {
              <mat-option [value]="year">
                <div class="year-option">
                  <span class="year-value">{{ year }}</span>
                  @if (year === currentYear) {
                    <span class="current-badge">Current</span>
                  }
                </div>
              </mat-option>
            }
          </mat-select>
          <mat-icon matPrefix>event</mat-icon>
        </mat-form-field>

        @if (selectedYear) {
          <div class="selection-info">
            <mat-icon class="info-icon">info</mat-icon>
            <div class="info-text">
              <strong>Selected Year:</strong> {{ selectedYear }}
              <p>The report will include all quality measures evaluated during {{ selectedYear }}.</p>
            </div>
          </div>
        }

        <div class="year-hints">
          <h4>Quick Selection</h4>
          <div class="hint-buttons">
            <button
              mat-stroked-button
              (click)="selectYear(currentYear)"
              [class.selected]="selectedYear === currentYear"
            >
              <mat-icon>today</mat-icon>
              Current Year ({{ currentYear }})
            </button>
            <button
              mat-stroked-button
              (click)="selectYear(currentYear - 1)"
              [class.selected]="selectedYear === currentYear - 1"
            >
              <mat-icon>history</mat-icon>
              Last Year ({{ currentYear - 1 }})
            </button>
          </div>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button (click)="onCancel()">
          <mat-icon>close</mat-icon>
          Cancel
        </button>
        <button
          mat-raised-button
          color="primary"
          [disabled]="!selectedYear"
          (click)="onConfirm()"
        >
          <mat-icon>check_circle</mat-icon>
          Generate Report
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .dialog-container {
        min-width: 500px;
      }

      h2[mat-dialog-title] {
        display: flex;
        align-items: center;
        gap: 12px;
        margin: 0;
        padding: 24px 24px 16px;
        font-size: 24px;
        font-weight: 500;
        color: #1a1a1a;

        mat-icon {
          font-size: 28px;
          width: 28px;
          height: 28px;
          color: #1976d2;
        }
      }

      mat-dialog-content {
        padding: 0 24px 24px;
      }

      .description {
        color: #666;
        margin: 0 0 20px 0;
        line-height: 1.6;
      }

      .year-field {
        width: 100%;
        margin-bottom: 20px;
      }

      .year-option {
        display: flex;
        align-items: center;
        justify-content: space-between;
        width: 100%;

        .year-value {
          font-size: 16px;
        }

        .current-badge {
          background-color: #e3f2fd;
          color: #1976d2;
          padding: 2px 8px;
          border-radius: 12px;
          font-size: 11px;
          font-weight: 600;
          text-transform: uppercase;
        }
      }

      .selection-info {
        display: flex;
        gap: 12px;
        padding: 16px;
        background-color: #f5f5f5;
        border-radius: 8px;
        margin-bottom: 20px;

        .info-icon {
          font-size: 24px;
          width: 24px;
          height: 24px;
          color: #1976d2;
          flex-shrink: 0;
        }

        .info-text {
          flex: 1;

          strong {
            color: #333;
            font-weight: 600;
          }

          p {
            margin: 4px 0 0 0;
            color: #666;
            font-size: 14px;
          }
        }
      }

      .year-hints {
        h4 {
          font-size: 14px;
          font-weight: 600;
          color: #666;
          margin: 0 0 12px 0;
          text-transform: uppercase;
          letter-spacing: 0.5px;
        }

        .hint-buttons {
          display: flex;
          gap: 12px;
          flex-wrap: wrap;

          button {
            flex: 1;
            min-width: 200px;

            mat-icon {
              margin-right: 8px;
            }

            &.selected {
              background-color: #e3f2fd;
              border-color: #1976d2;
              color: #1976d2;
            }
          }
        }
      }

      mat-dialog-actions {
        padding: 16px 24px;
        border-top: 1px solid #e0e0e0;

        button {
          mat-icon {
            margin-right: 8px;
          }
        }
      }
    `,
  ],
})
export class YearSelectionDialogComponent {
  currentYear = new Date().getFullYear();
  selectedYear: number | null = this.currentYear;
  availableYears: number[] = [];

  constructor(private dialogRef: MatDialogRef<YearSelectionDialogComponent>) {
    // Generate years from 5 years ago to current year
    const startYear = this.currentYear - 5;
    this.availableYears = Array.from(
      { length: 6 },
      (_, i) => this.currentYear - i
    );
  }

  /**
   * Select a year
   */
  selectYear(year: number): void {
    this.selectedYear = year;
  }

  /**
   * Confirm selection and close dialog
   */
  onConfirm(): void {
    if (this.selectedYear) {
      this.dialogRef.close(this.selectedYear);
    }
  }

  /**
   * Cancel and close dialog
   */
  onCancel(): void {
    this.dialogRef.close(null);
  }
}
