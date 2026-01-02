import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';

export type FilterType = 'text' | 'select' | 'date-range' | 'number-range';

export interface FilterConfig {
  key: string;
  label: string;
  type: FilterType;
  options?: Array<{ label: string; value: any }>;
  placeholder?: string;
  defaultValue?: any;
}

export interface FilterValues {
  [key: string]: any;
}

/**
 * FilterPanel Component
 *
 * A reusable, config-driven filter panel with collapsible expansion panel.
 * Supports multiple filter types and displays active filters as chips.
 *
 * Features:
 * - Collapsible expansion panel
 * - Config-driven filter definitions
 * - Multiple filter types: text, select, date range, number range
 * - Active filter chips with removal
 * - Apply/Reset functionality
 * - Material Design form fields
 * - Accessible with ARIA attributes
 *
 * @example
 * <app-filter-panel
 *   [filters]="filterConfig"
 *   [expanded]="true"
 *   (filterChange)="onFilterChange($event)">
 * </app-filter-panel>
 */
@Component({
  selector: 'app-filter-panel',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonModule,
    MatChipsModule,
    MatIconModule
  ],
  templateUrl: './filter-panel.component.html',
  styleUrls: ['./filter-panel.component.scss']
})
export class FilterPanelComponent implements OnInit {
  /** Array of filter configurations */
  @Input() filters: FilterConfig[] = [];

  /** Whether panel is initially expanded */
  @Input() expanded = false;

  /** Panel title */
  @Input() title = 'Filters';

  /** Emits when filters are applied */
  @Output() filterChange = new EventEmitter<FilterValues>();

  /** Current filter values */
  filterValues: FilterValues = {};

  /** Applied filter values (for displaying chips) */
  appliedFilters: FilterValues = {};

  ngOnInit(): void {
    // Initialize filter values with defaults
    this.filters.forEach(filter => {
      if (filter.defaultValue !== undefined) {
        this.filterValues[filter.key] = filter.defaultValue;
      }
    });
  }

  /**
   * Get active filter chips for display
   */
  getActiveFilterChips(): Array<{ key: string; label: string; value: string }> {
    const chips: Array<{ key: string; label: string; value: string }> = [];

    Object.keys(this.appliedFilters).forEach(key => {
      const value = this.appliedFilters[key];
      if (value !== undefined && value !== null && value !== '') {
        const filter = this.filters.find(f => f.key === key);
        if (filter) {
          chips.push({
            key,
            label: filter.label,
            value: this.formatFilterValue(filter, value)
          });
        }
      }
    });

    return chips;
  }

  /**
   * Format filter value for display in chip
   */
  private formatFilterValue(filter: FilterConfig, value: any): string {
    if (filter.type === 'select' && filter.options) {
      const option = filter.options.find(opt => opt.value === value);
      return option ? option.label : String(value);
    }

    if (filter.type === 'date-range') {
      if (value.start && value.end) {
        return `${this.formatDate(value.start)} - ${this.formatDate(value.end)}`;
      }
      return value.start ? `From ${this.formatDate(value.start)}` : `To ${this.formatDate(value.end)}`;
    }

    if (filter.type === 'number-range') {
      if (value.min !== undefined && value.max !== undefined) {
        return `${value.min} - ${value.max}`;
      }
      return value.min !== undefined ? `Min: ${value.min}` : `Max: ${value.max}`;
    }

    return String(value);
  }

  /**
   * Format date for display
   */
  private formatDate(date: Date): string {
    return new Date(date).toLocaleDateString();
  }

  /**
   * Apply filters
   */
  onApply(): void {
    // Only include non-empty values
    const activeFilters: FilterValues = {};
    Object.keys(this.filterValues).forEach(key => {
      const value = this.filterValues[key];
      if (value !== undefined && value !== null && value !== '') {
        activeFilters[key] = value;
      }
    });

    this.appliedFilters = { ...activeFilters };
    this.filterChange.emit(activeFilters);
  }

  /**
   * Reset all filters
   */
  onReset(): void {
    this.filterValues = {};
    this.appliedFilters = {};

    // Reset to default values
    this.filters.forEach(filter => {
      if (filter.defaultValue !== undefined) {
        this.filterValues[filter.key] = filter.defaultValue;
      }
    });

    this.filterChange.emit({});
  }

  /**
   * Remove specific filter chip
   */
  onRemoveChip(key: string): void {
    delete this.filterValues[key];
    delete this.appliedFilters[key];
    this.filterChange.emit({ ...this.appliedFilters });
  }

  /**
   * Track by function for ngFor
   */
  trackByKey(index: number, filter: FilterConfig): string {
    return filter.key;
  }
}
