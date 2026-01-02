import { Component, Inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  FormArray,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {
  MatDialogModule,
  MatDialogRef,
  MAT_DIALOG_DATA,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';

/**
 * Filter configuration
 */
export interface FilterConfig {
  logic: 'AND' | 'OR';
  filters: FilterCriteria[];
  name?: string;
}

export interface FilterCriteria {
  field: string;
  operator: string;
  value: any;
}

/**
 * Advanced Filter Dialog Data
 */
export interface AdvancedFilterDialogData {
  currentFilters?: FilterConfig;
  availableFields: FilterField[];
}

export interface FilterField {
  name: string;
  label: string;
  type: 'text' | 'number' | 'date' | 'select';
  options?: { value: any; label: string }[];
}

/**
 * Advanced Filter Dialog Component
 *
 * Provides complex filtering with dynamic filter rows and AND/OR logic.
 *
 * Features:
 * - Add/Remove filter rows dynamically
 * - Multiple filter operators (equals, contains, between, etc.)
 * - AND/OR logic between filters
 * - Save and load filter presets
 * - Preview count of matching results
 *
 * Usage:
 * ```typescript
 * const dialogRef = this.dialog.open(AdvancedFilterDialogComponent, {
 *   data: {
 *     currentFilters: existingFilters,
 *     availableFields: fields
 *   }
 * });
 *
 * dialogRef.afterClosed().subscribe((config: FilterConfig | null) => {
 *   if (config) {
 *     // Apply filters
 *   }
 * });
 * ```
 */
@Component({
  selector: 'app-advanced-filter-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatChipsModule,
    MatButtonToggleModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './advanced-filter-dialog.component.html',
  styleUrls: ['./advanced-filter-dialog.component.scss'],
})
export class AdvancedFilterDialogComponent implements OnInit {
  filterForm!: FormGroup;
  previewCount = signal<number | null>(null);
  isLoadingPreview = signal(false);

  operatorsByType: Record<string, { value: string; label: string }[]> = {
    text: [
      { value: 'contains', label: 'Contains' },
      { value: 'equals', label: 'Equals' },
      { value: 'startsWith', label: 'Starts With' },
      { value: 'endsWith', label: 'Ends With' },
      { value: 'notEquals', label: 'Not Equals' },
    ],
    number: [
      { value: 'equals', label: 'Equals' },
      { value: 'notEquals', label: 'Not Equals' },
      { value: 'greaterThan', label: 'Greater Than' },
      { value: 'lessThan', label: 'Less Than' },
      { value: 'between', label: 'Between' },
    ],
    date: [
      { value: 'equals', label: 'On Date' },
      { value: 'before', label: 'Before' },
      { value: 'after', label: 'After' },
      { value: 'between', label: 'Between' },
    ],
    select: [
      { value: 'equals', label: 'Is' },
      { value: 'notEquals', label: 'Is Not' },
      { value: 'in', label: 'In List' },
    ],
  };

  savedFilters: { name: string; config: FilterConfig }[] = [
    {
      name: 'Active Patients',
      config: {
        logic: 'AND',
        filters: [
          { field: 'status', operator: 'equals', value: 'Active' },
        ],
      },
    },
    {
      name: 'Recent Evaluations',
      config: {
        logic: 'AND',
        filters: [
          { field: 'evaluationDate', operator: 'after', value: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000) },
        ],
      },
    },
  ];

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<AdvancedFilterDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AdvancedFilterDialogData
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    if (this.data.currentFilters) {
      this.loadFilters(this.data.currentFilters);
    }
  }

  /**
   * Initialize the filter form
   */
  private initializeForm(): void {
    this.filterForm = this.fb.group({
      logic: ['AND'],
      name: [''],
      filters: this.fb.array([]),
    });

    // Add initial filter row
    this.addFilter();
  }

  /**
   * Get filters form array
   */
  get filters(): FormArray {
    return this.filterForm.get('filters') as FormArray;
  }

  /**
   * Create a new filter form group
   */
  private createFilterGroup(criteria?: FilterCriteria): FormGroup {
    return this.fb.group({
      field: [criteria?.field || '', Validators.required],
      operator: [criteria?.operator || '', Validators.required],
      value: [criteria?.value || ''],
      value2: [''], // For 'between' operator
    });
  }

  /**
   * Add a new filter row
   */
  addFilter(): void {
    this.filters.push(this.createFilterGroup());
  }

  /**
   * Remove a filter row
   */
  removeFilter(index: number): void {
    if (this.filters.length > 1) {
      this.filters.removeAt(index);
      this.updatePreview();
    }
  }

  /**
   * Load existing filters into form
   */
  private loadFilters(config: FilterConfig): void {
    this.filterForm.patchValue({
      logic: config.logic,
      name: config.name || '',
    });

    // Clear existing filters
    this.filters.clear();

    // Add loaded filters
    config.filters.forEach((criteria) => {
      this.filters.push(this.createFilterGroup(criteria));
    });
  }

  /**
   * Get field configuration
   */
  getField(fieldName: string): FilterField | undefined {
    return this.data.availableFields.find((f) => f.name === fieldName);
  }

  /**
   * Get operators for selected field
   */
  getOperatorsForField(index: number): { value: string; label: string }[] {
    const filterGroup = this.filters.at(index) as FormGroup;
    const fieldName = filterGroup.get('field')?.value;
    const field = this.getField(fieldName);

    if (!field) return [];

    return this.operatorsByType[field.type] || [];
  }

  /**
   * Handle field change
   */
  onFieldChange(index: number): void {
    const filterGroup = this.filters.at(index) as FormGroup;
    const fieldName = filterGroup.get('field')?.value;
    const field = this.getField(fieldName);

    if (field) {
      const operators = this.operatorsByType[field.type];
      if (operators && operators.length > 0) {
        filterGroup.patchValue({
          operator: operators[0].value,
          value: '',
          value2: '',
        });
      }
    }

    this.updatePreview();
  }

  /**
   * Check if operator requires second value (e.g., 'between')
   */
  requiresSecondValue(index: number): boolean {
    const filterGroup = this.filters.at(index) as FormGroup;
    const operator = filterGroup.get('operator')?.value;
    return operator === 'between';
  }

  /**
   * Check if field is date type
   */
  isDateField(index: number): boolean {
    const filterGroup = this.filters.at(index) as FormGroup;
    const fieldName = filterGroup.get('field')?.value;
    const field = this.getField(fieldName);
    return field?.type === 'date';
  }

  /**
   * Check if field is select type
   */
  isSelectField(index: number): boolean {
    const filterGroup = this.filters.at(index) as FormGroup;
    const fieldName = filterGroup.get('field')?.value;
    const field = this.getField(fieldName);
    return field?.type === 'select';
  }

  /**
   * Get select options for field
   */
  getSelectOptions(index: number): { value: any; label: string }[] {
    const filterGroup = this.filters.at(index) as FormGroup;
    const fieldName = filterGroup.get('field')?.value;
    const field = this.getField(fieldName);
    return field?.options || [];
  }

  /**
   * Update preview count
   */
  updatePreview(): void {
    if (!this.filterForm.valid) {
      this.previewCount.set(null);
      return;
    }

    this.isLoadingPreview.set(true);

    // Simulate API call to get count
    setTimeout(() => {
      const randomCount = Math.floor(Math.random() * 500) + 1;
      this.previewCount.set(randomCount);
      this.isLoadingPreview.set(false);
    }, 500);
  }

  /**
   * Load saved filter preset
   */
  loadSavedFilter(filter: { name: string; config: FilterConfig }): void {
    this.loadFilters(filter.config);
    this.updatePreview();
  }

  /**
   * Save current filter configuration
   */
  saveFilter(): void {
    const config = this.buildFilterConfig();
    if (config.name) {
      this.savedFilters.push({
        name: config.name,
        config,
      });
      // In production, save to backend
    }
  }

  /**
   * Build filter configuration from form
   */
  private buildFilterConfig(): FilterConfig {
    const formValue = this.filterForm.value;
    return {
      logic: formValue.logic,
      name: formValue.name,
      filters: formValue.filters
        .filter((f: any) => f.field && f.operator)
        .map((f: any) => ({
          field: f.field,
          operator: f.operator,
          value: f.value,
          value2: f.value2,
        })),
    };
  }

  /**
   * Reset all filters
   */
  onReset(): void {
    this.filterForm.reset({ logic: 'AND' });
    this.filters.clear();
    this.addFilter();
    this.previewCount.set(null);
  }

  /**
   * Apply filters and close dialog
   */
  onApply(): void {
    if (!this.filterForm.valid) {
      this.filterForm.markAllAsTouched();
      return;
    }

    const config = this.buildFilterConfig();
    this.dialogRef.close(config);
  }

  /**
   * Cancel and close dialog
   */
  onCancel(): void {
    this.dialogRef.close(null);
  }
}
