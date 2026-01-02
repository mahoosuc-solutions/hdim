import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';

export interface DateRange {
  start: Date | null;
  end: Date | null;
}

export interface PresetRange {
  label: string;
  start: Date;
  end: Date;
}

/**
 * DateRangePicker Component
 *
 * A reusable date range picker with validation and preset ranges.
 * Provides dual date pickers with quick preset options.
 *
 * Features:
 * - Two date pickers (from/to)
 * - Validation (end >= start)
 * - Preset ranges: Today, Last 7/30/90 days
 * - Min/max date constraints
 * - Material form field styling
 * - Accessible with ARIA attributes
 *
 * @example
 * <app-date-range-picker
 *   [startDate]="startDate"
 *   [endDate]="endDate"
 *   [minDate]="minDate"
 *   [maxDate]="maxDate"
 *   (rangeChange)="onDateRangeChange($event)">
 * </app-date-range-picker>
 */
@Component({
  selector: 'app-date-range-picker',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonModule,
    MatMenuModule,
    MatIconModule
  ],
  templateUrl: './date-range-picker.component.html',
  styleUrls: ['./date-range-picker.component.scss']
})
export class DateRangePickerComponent implements OnInit {
  /** Start date */
  @Input() startDate: Date | null = null;

  /** End date */
  @Input() endDate: Date | null = null;

  /** Minimum allowed date */
  @Input() minDate: Date | null = null;

  /** Maximum allowed date */
  @Input() maxDate: Date | null = null;

  /** Label for start date field */
  @Input() startLabel = 'Start Date';

  /** Label for end date field */
  @Input() endLabel = 'End Date';

  /** Show preset ranges menu */
  @Input() showPresets = true;

  /** Emits when date range changes */
  @Output() rangeChange = new EventEmitter<DateRange>();

  /** Internal start date */
  internalStartDate: Date | null = null;

  /** Internal end date */
  internalEndDate: Date | null = null;

  /** Preset date ranges */
  presets: PresetRange[] = [];

  ngOnInit(): void {
    this.internalStartDate = this.startDate;
    this.internalEndDate = this.endDate;
    this.initializePresets();
  }

  /**
   * Initialize preset date ranges
   */
  private initializePresets(): void {
    const today = new Date();
    const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate());

    this.presets = [
      {
        label: 'Today',
        start: todayStart,
        end: new Date(todayStart.getTime() + 24 * 60 * 60 * 1000 - 1)
      },
      {
        label: 'Last 7 Days',
        start: new Date(todayStart.getTime() - 7 * 24 * 60 * 60 * 1000),
        end: todayStart
      },
      {
        label: 'Last 30 Days',
        start: new Date(todayStart.getTime() - 30 * 24 * 60 * 60 * 1000),
        end: todayStart
      },
      {
        label: 'Last 90 Days',
        start: new Date(todayStart.getTime() - 90 * 24 * 60 * 60 * 1000),
        end: todayStart
      }
    ];
  }

  /**
   * Handle start date change
   */
  onStartDateChange(date: Date | null): void {
    this.internalStartDate = date;
    this.validateAndEmit();
  }

  /**
   * Handle end date change
   */
  onEndDateChange(date: Date | null): void {
    this.internalEndDate = date;
    this.validateAndEmit();
  }

  /**
   * Apply preset range
   */
  applyPreset(preset: PresetRange): void {
    this.internalStartDate = preset.start;
    this.internalEndDate = preset.end;
    this.validateAndEmit();
  }

  /**
   * Clear date range
   */
  clearRange(): void {
    this.internalStartDate = null;
    this.internalEndDate = null;
    this.emitChange();
  }

  /**
   * Validate dates and emit change
   */
  private validateAndEmit(): void {
    // Validate that end date is after start date
    if (this.internalStartDate && this.internalEndDate) {
      if (this.internalEndDate < this.internalStartDate) {
        // Auto-correct: set end date to start date
        this.internalEndDate = this.internalStartDate;
      }
    }

    this.emitChange();
  }

  /**
   * Emit range change event
   */
  private emitChange(): void {
    this.rangeChange.emit({
      start: this.internalStartDate,
      end: this.internalEndDate
    });
  }

  /**
   * Get minimum date for end date picker
   */
  getMinEndDate(): Date | null {
    return this.internalStartDate || this.minDate;
  }

  /**
   * Check if a date range is valid
   */
  isValidRange(): boolean {
    if (!this.internalStartDate || !this.internalEndDate) {
      return true; // Allow partial ranges
    }
    return this.internalEndDate >= this.internalStartDate;
  }

  /**
   * Get validation error message
   */
  getErrorMessage(): string {
    if (!this.isValidRange()) {
      return 'End date must be after start date';
    }
    return '';
  }
}
