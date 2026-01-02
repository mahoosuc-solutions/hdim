import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';

/**
 * Period type for comparison
 */
export type ComparisonPeriodType = 'year' | 'quarter' | 'month';

/**
 * Period comparison result
 */
export interface PeriodComparisonSelection {
  type: ComparisonPeriodType;
  period1: { label: string; startDate: Date; endDate: Date };
  period2: { label: string; startDate: Date; endDate: Date };
}

/**
 * Period Comparison Dialog Component
 *
 * Allows users to select two time periods for comparative analysis.
 *
 * Usage:
 * ```typescript
 * const dialogRef = this.dialog.open(PeriodComparisonDialogComponent);
 * dialogRef.afterClosed().subscribe((result: PeriodComparisonSelection | null) => {
 *   if (result) {
 *     // Generate comparative report
 *   }
 * });
 * ```
 */
@Component({
  selector: 'app-period-comparison-dialog',
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatChipsModule,
    MatDividerModule,
  ],
  template: `
    <div class="dialog-container">
      <h2 mat-dialog-title>
        <mat-icon>compare_arrows</mat-icon>
        Compare Time Periods
      </h2>

      <mat-dialog-content>
        <p class="description">
          Select two time periods to compare compliance rates and identify trends.
        </p>

        <!-- Period Type Selection -->
        <div class="period-type-section">
          <h4>Comparison Type</h4>
          <div class="type-chips">
            <button
              mat-stroked-button
              [class.selected]="periodType === 'year'"
              (click)="setPeriodType('year')">
              <mat-icon>calendar_today</mat-icon>
              Year-over-Year
            </button>
            <button
              mat-stroked-button
              [class.selected]="periodType === 'quarter'"
              (click)="setPeriodType('quarter')">
              <mat-icon>date_range</mat-icon>
              Quarter-over-Quarter
            </button>
            <button
              mat-stroked-button
              [class.selected]="periodType === 'month'"
              (click)="setPeriodType('month')">
              <mat-icon>event</mat-icon>
              Month-over-Month
            </button>
          </div>
        </div>

        <mat-divider></mat-divider>

        <!-- Period Selection -->
        <div class="periods-section">
          <div class="period-row">
            <div class="period-box baseline">
              <div class="period-header">
                <mat-icon>flag</mat-icon>
                <span>Baseline Period</span>
              </div>
              <mat-form-field appearance="outline">
                <mat-label>Select {{ periodType }}</mat-label>
                <mat-select [(ngModel)]="baselinePeriod">
                  @for (option of getPeriodOptions(); track option.value) {
                    <mat-option [value]="option.value">
                      {{ option.label }}
                    </mat-option>
                  }
                </mat-select>
              </mat-form-field>
            </div>

            <div class="compare-arrow">
              <mat-icon>arrow_forward</mat-icon>
              <span>vs</span>
            </div>

            <div class="period-box comparison">
              <div class="period-header">
                <mat-icon>compare</mat-icon>
                <span>Comparison Period</span>
              </div>
              <mat-form-field appearance="outline">
                <mat-label>Select {{ periodType }}</mat-label>
                <mat-select [(ngModel)]="comparisonPeriod">
                  @for (option of getPeriodOptions(); track option.value) {
                    <mat-option [value]="option.value" [disabled]="option.value === baselinePeriod">
                      {{ option.label }}
                    </mat-option>
                  }
                </mat-select>
              </mat-form-field>
            </div>
          </div>
        </div>

        @if (baselinePeriod && comparisonPeriod) {
          <div class="selection-summary">
            <mat-icon>analytics</mat-icon>
            <div class="summary-text">
              <strong>Comparison Summary</strong>
              <p>
                Comparing <span class="highlight">{{ getLabel(baselinePeriod) }}</span>
                to <span class="highlight">{{ getLabel(comparisonPeriod) }}</span>
              </p>
              <p class="analysis-note">
                The report will show changes in compliance rates, identify improving and declining measures,
                and highlight significant trends.
              </p>
            </div>
          </div>
        }

        <!-- Quick Presets -->
        <div class="presets-section">
          <h4>Quick Presets</h4>
          <div class="preset-buttons">
            <button mat-stroked-button (click)="applyPreset('lastYear')">
              <mat-icon>history</mat-icon>
              This Year vs Last Year
            </button>
            <button mat-stroked-button (click)="applyPreset('lastQuarter')">
              <mat-icon>trending_up</mat-icon>
              This Quarter vs Last Quarter
            </button>
            <button mat-stroked-button (click)="applyPreset('lastMonth')">
              <mat-icon>schedule</mat-icon>
              This Month vs Last Month
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
          [disabled]="!isValid()"
          (click)="onConfirm()">
          <mat-icon>compare_arrows</mat-icon>
          Generate Comparison
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .dialog-container {
      min-width: 650px;
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
        color: #9c27b0;
      }
    }

    mat-dialog-content {
      padding: 0 24px 24px;
    }

    .description {
      color: #666;
      margin: 0 0 24px 0;
      line-height: 1.6;
    }

    h4 {
      font-size: 14px;
      font-weight: 600;
      color: #666;
      margin: 0 0 12px 0;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .period-type-section {
      margin-bottom: 24px;

      .type-chips {
        display: flex;
        gap: 12px;
        flex-wrap: wrap;

        button {
          flex: 1;
          min-width: 180px;

          mat-icon {
            margin-right: 8px;
          }

          &.selected {
            background-color: #f3e5f5;
            border-color: #9c27b0;
            color: #9c27b0;
          }
        }
      }
    }

    mat-divider {
      margin: 24px 0;
    }

    .periods-section {
      margin-bottom: 24px;

      .period-row {
        display: flex;
        align-items: center;
        gap: 16px;

        .period-box {
          flex: 1;
          padding: 16px;
          border-radius: 8px;
          border: 2px solid #e0e0e0;

          .period-header {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 12px;
            font-weight: 600;
            color: #333;

            mat-icon {
              font-size: 20px;
              width: 20px;
              height: 20px;
            }
          }

          mat-form-field {
            width: 100%;
          }

          &.baseline {
            border-color: #1976d2;
            background: #e3f2fd;

            .period-header mat-icon {
              color: #1976d2;
            }
          }

          &.comparison {
            border-color: #9c27b0;
            background: #f3e5f5;

            .period-header mat-icon {
              color: #9c27b0;
            }
          }
        }

        .compare-arrow {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 4px;
          color: #666;

          mat-icon {
            font-size: 24px;
            width: 24px;
            height: 24px;
          }

          span {
            font-size: 12px;
            font-weight: 600;
            text-transform: uppercase;
          }
        }
      }
    }

    .selection-summary {
      display: flex;
      gap: 16px;
      padding: 16px;
      background: linear-gradient(135deg, #f3e5f5 0%, #e3f2fd 100%);
      border-radius: 8px;
      margin-bottom: 24px;

      mat-icon {
        font-size: 32px;
        width: 32px;
        height: 32px;
        color: #9c27b0;
        flex-shrink: 0;
      }

      .summary-text {
        flex: 1;

        strong {
          font-size: 16px;
          color: #333;
        }

        p {
          margin: 8px 0 0 0;
          color: #555;
          font-size: 14px;
        }

        .highlight {
          font-weight: 600;
          color: #9c27b0;
        }

        .analysis-note {
          font-size: 13px;
          color: #666;
          margin-top: 8px;
        }
      }
    }

    .presets-section {
      .preset-buttons {
        display: flex;
        gap: 12px;
        flex-wrap: wrap;

        button {
          flex: 1;
          min-width: 200px;

          mat-icon {
            margin-right: 8px;
          }
        }
      }
    }

    mat-dialog-actions {
      padding: 16px 24px;
      border-top: 1px solid #e0e0e0;

      button mat-icon {
        margin-right: 8px;
      }
    }

    @media (max-width: 768px) {
      .dialog-container {
        min-width: auto;
        width: 100%;
      }

      .period-row {
        flex-direction: column !important;

        .compare-arrow {
          transform: rotate(90deg);
        }
      }

      .type-chips,
      .preset-buttons {
        flex-direction: column !important;

        button {
          min-width: auto !important;
        }
      }
    }
  `],
})
export class PeriodComparisonDialogComponent {
  private readonly currentYear = new Date().getFullYear();
  private readonly currentMonth = new Date().getMonth();
  private readonly currentQuarter = Math.floor(this.currentMonth / 3);

  periodType: ComparisonPeriodType = 'year';
  baselinePeriod: string | null = null;
  comparisonPeriod: string | null = null;

  private periodLabels = new Map<string, string>();

  constructor(private dialogRef: MatDialogRef<PeriodComparisonDialogComponent>) {
    // Set default selections
    this.applyPreset('lastYear');
  }

  /**
   * Set the period type and reset selections
   */
  setPeriodType(type: ComparisonPeriodType): void {
    this.periodType = type;
    this.baselinePeriod = null;
    this.comparisonPeriod = null;
  }

  /**
   * Get period options based on type
   */
  getPeriodOptions(): { value: string; label: string }[] {
    const options: { value: string; label: string }[] = [];

    switch (this.periodType) {
      case 'year':
        for (let i = 0; i < 5; i++) {
          const year = this.currentYear - i;
          const label = `${year}`;
          options.push({ value: `year-${year}`, label });
          this.periodLabels.set(`year-${year}`, label);
        }
        break;

      case 'quarter':
        for (let i = 0; i < 8; i++) {
          let q = this.currentQuarter - i;
          let y = this.currentYear;
          while (q < 0) {
            q += 4;
            y--;
          }
          const label = `Q${q + 1} ${y}`;
          const value = `quarter-${y}-${q}`;
          options.push({ value, label });
          this.periodLabels.set(value, label);
        }
        break;

      case 'month':
        const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        for (let i = 0; i < 12; i++) {
          let m = this.currentMonth - i;
          let y = this.currentYear;
          while (m < 0) {
            m += 12;
            y--;
          }
          const label = `${months[m]} ${y}`;
          const value = `month-${y}-${m}`;
          options.push({ value, label });
          this.periodLabels.set(value, label);
        }
        break;
    }

    return options;
  }

  /**
   * Get label for a period value
   */
  getLabel(value: string): string {
    return this.periodLabels.get(value) || value;
  }

  /**
   * Apply a preset comparison
   */
  applyPreset(preset: 'lastYear' | 'lastQuarter' | 'lastMonth'): void {
    switch (preset) {
      case 'lastYear':
        this.periodType = 'year';
        this.getPeriodOptions(); // Populate labels
        this.baselinePeriod = `year-${this.currentYear - 1}`;
        this.comparisonPeriod = `year-${this.currentYear}`;
        break;

      case 'lastQuarter':
        this.periodType = 'quarter';
        this.getPeriodOptions();
        const lastQ = this.currentQuarter - 1;
        const lastQYear = lastQ < 0 ? this.currentYear - 1 : this.currentYear;
        const lastQNum = lastQ < 0 ? 3 : lastQ;
        this.baselinePeriod = `quarter-${lastQYear}-${lastQNum}`;
        this.comparisonPeriod = `quarter-${this.currentYear}-${this.currentQuarter}`;
        break;

      case 'lastMonth':
        this.periodType = 'month';
        this.getPeriodOptions();
        const lastM = this.currentMonth - 1;
        const lastMYear = lastM < 0 ? this.currentYear - 1 : this.currentYear;
        const lastMNum = lastM < 0 ? 11 : lastM;
        this.baselinePeriod = `month-${lastMYear}-${lastMNum}`;
        this.comparisonPeriod = `month-${this.currentYear}-${this.currentMonth}`;
        break;
    }
  }

  /**
   * Check if selection is valid
   */
  isValid(): boolean {
    return !!this.baselinePeriod && !!this.comparisonPeriod && this.baselinePeriod !== this.comparisonPeriod;
  }

  /**
   * Parse a period string into dates
   */
  private parsePeriod(value: string): { startDate: Date; endDate: Date } {
    const parts = value.split('-');
    const type = parts[0];
    const year = parseInt(parts[1], 10);

    switch (type) {
      case 'year':
        return {
          startDate: new Date(year, 0, 1),
          endDate: new Date(year, 11, 31, 23, 59, 59),
        };

      case 'quarter':
        const quarter = parseInt(parts[2], 10);
        const startMonth = quarter * 3;
        return {
          startDate: new Date(year, startMonth, 1),
          endDate: new Date(year, startMonth + 3, 0, 23, 59, 59),
        };

      case 'month':
        const month = parseInt(parts[2], 10);
        return {
          startDate: new Date(year, month, 1),
          endDate: new Date(year, month + 1, 0, 23, 59, 59),
        };

      default:
        throw new Error(`Unknown period type: ${type}`);
    }
  }

  /**
   * Confirm selection and close dialog
   */
  onConfirm(): void {
    if (!this.isValid()) return;

    const result: PeriodComparisonSelection = {
      type: this.periodType,
      period1: {
        label: this.getLabel(this.baselinePeriod!),
        ...this.parsePeriod(this.baselinePeriod!),
      },
      period2: {
        label: this.getLabel(this.comparisonPeriod!),
        ...this.parsePeriod(this.comparisonPeriod!),
      },
    };

    this.dialogRef.close(result);
  }

  /**
   * Cancel and close dialog
   */
  onCancel(): void {
    this.dialogRef.close(null);
  }
}
