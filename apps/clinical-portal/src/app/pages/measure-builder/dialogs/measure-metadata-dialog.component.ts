import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import {
  CustomMeasure,
  CustomMeasureService,
  UpdateCustomMeasureRequest,
} from '../../../services/custom-measure.service';
import {
  extractApiFieldErrors,
  getApiErrorMessage,
} from '../utils/api-error-parser';

export interface MeasureMetadataDialogData {
  measure: CustomMeasure;
}

@Component({
  selector: 'app-measure-metadata-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon color="primary">tune</mat-icon>
      Edit Measure Details
    </h2>

    <mat-dialog-content class="dialog-content">
      @if (apiErrorMessage) {
        <div class="error-banner" role="alert">
          <div class="error-message">{{ apiErrorMessage }}</div>
          <button
            mat-stroked-button
            color="warn"
            type="button"
            [disabled]="loading || !canSave()"
            (click)="retrySave()">
            Retry
          </button>
        </div>
      }

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Measure Name</mat-label>
        <input
          matInput
          [(ngModel)]="name"
          (blur)="touchField('name')"
          (ngModelChange)="clearApiFieldError('name')"
          required />
        @if (shouldShowClientError('name') && !hasName()) {
          <div class="inline-error">Measure name is required</div>
        }
        @if (apiFieldErrors['name']) {
          <div class="inline-error">{{ apiFieldErrors['name'] }}</div>
        }
      </mat-form-field>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Description</mat-label>
        <textarea matInput rows="3" [(ngModel)]="description"></textarea>
      </mat-form-field>

      <div class="grid">
        <mat-form-field appearance="outline">
          <mat-label>Category</mat-label>
          <mat-select [(ngModel)]="category">
            <mat-option value="CUSTOM">Custom</mat-option>
            <mat-option value="QUALITY">Quality Improvement</mat-option>
            <mat-option value="HEDIS">HEDIS-Derived</mat-option>
            <mat-option value="CMS">CMS-Derived</mat-option>
            <mat-option value="COMPLIANCE">Compliance</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Measurement Year</mat-label>
          <input
            matInput
            type="number"
            [(ngModel)]="year"
            (blur)="touchField('year')"
            (ngModelChange)="clearApiFieldError('year')"
            min="2000"
            max="2100" />
          @if (shouldShowClientError('year') && !isYearValid()) {
            <div class="inline-error">Year must be between 2000 and 2100</div>
          }
          @if (apiFieldErrors['year']) {
            <div class="inline-error">{{ apiFieldErrors['year'] }}</div>
          }
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Owner</mat-label>
          <input matInput [(ngModel)]="owner" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Clinical Focus</mat-label>
          <input matInput [(ngModel)]="clinicalFocus" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Reporting Cadence</mat-label>
          <mat-select
            [(ngModel)]="reportingCadence"
            (selectionChange)="touchField('reportingCadence')"
            (ngModelChange)="clearApiFieldError('reportingCadence')">
            <mat-option value="MONTHLY">Monthly</mat-option>
            <mat-option value="QUARTERLY">Quarterly</mat-option>
            <mat-option value="ANNUAL">Annual</mat-option>
          </mat-select>
          @if (shouldShowClientError('reportingCadence') && !isReportingCadenceValid()) {
            <div class="inline-error">Reporting cadence must be Monthly, Quarterly, or Annual</div>
          }
          @if (apiFieldErrors['reportingCadence']) {
            <div class="inline-error">{{ apiFieldErrors['reportingCadence'] }}</div>
          }
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Priority</mat-label>
          <mat-select
            [(ngModel)]="priority"
            (selectionChange)="touchField('priority')"
            (ngModelChange)="clearApiFieldError('priority')">
            <mat-option value="LOW">Low</mat-option>
            <mat-option value="MEDIUM">Medium</mat-option>
            <mat-option value="HIGH">High</mat-option>
          </mat-select>
          @if (shouldShowClientError('priority') && !isPriorityValid()) {
            <div class="inline-error">Priority must be Low, Medium, or High</div>
          }
          @if (apiFieldErrors['priority']) {
            <div class="inline-error">{{ apiFieldErrors['priority'] }}</div>
          }
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Target Threshold</mat-label>
          <input matInput [(ngModel)]="targetThreshold" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Tags</mat-label>
          <input matInput [(ngModel)]="tags" />
        </mat-form-field>
      </div>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Implementation Notes</mat-label>
        <textarea matInput rows="3" [(ngModel)]="implementationNotes"></textarea>
      </mat-form-field>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button
        mat-raised-button
        color="primary"
        [disabled]="loading || !canSave()"
        (click)="save()">
        Save Details
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .dialog-content {
        display: flex;
        flex-direction: column;
        gap: 12px;
        min-width: 760px;
        max-width: 860px;
        padding-top: 8px;
      }

      .full-width {
        width: 100%;
      }

      .error-banner {
        border: 1px solid #ef9a9a;
        border-radius: 8px;
        background: #ffebee;
        color: #b71c1c;
        padding: 10px 12px;
        font-size: 0.9rem;
        display: flex;
        gap: 10px;
        align-items: center;
        justify-content: space-between;
      }

      .error-message {
        flex: 1;
      }

      .inline-error {
        margin-top: 4px;
        font-size: 12px;
        color: #b00020;
      }

      .grid {
        display: grid;
        gap: 12px;
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }

      @media (max-width: 900px) {
        .dialog-content {
          min-width: 100%;
        }

        .grid {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class MeasureMetadataDialogComponent {
  private static readonly VALID_CADENCES = new Set([
    'MONTHLY',
    'QUARTERLY',
    'ANNUAL',
  ]);
  private static readonly VALID_PRIORITIES = new Set(['LOW', 'MEDIUM', 'HIGH']);

  loading = false;
  apiFieldErrors: Record<string, string> = {};
  apiErrorMessage = '';
  showClientValidation = false;
  touchedFields: Record<string, boolean> = {};
  name: string;
  description: string;
  category: string;
  year?: number;
  owner: string;
  clinicalFocus: string;
  reportingCadence: string;
  targetThreshold: string;
  priority: string;
  implementationNotes: string;
  tags: string;

  constructor(
    private customMeasureService: CustomMeasureService,
    private dialogRef: MatDialogRef<MeasureMetadataDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: MeasureMetadataDialogData
  ) {
    const { measure } = data;
    this.name = measure.name || '';
    this.description = measure.description || '';
    this.category = measure.category || 'CUSTOM';
    this.year = measure.year;
    this.owner = measure.owner || '';
    this.clinicalFocus = measure.clinicalFocus || '';
    this.reportingCadence = measure.reportingCadence || 'MONTHLY';
    this.targetThreshold = measure.targetThreshold || '';
    this.priority = measure.priority || 'MEDIUM';
    this.implementationNotes = measure.implementationNotes || '';
    this.tags = measure.tags || '';
  }

  save(): void {
    if (!this.canSave()) {
      this.showClientValidation = true;
      this.markAllValidationFieldsTouched();
      return;
    }

    this.showClientValidation = false;
    this.apiErrorMessage = '';
    this.apiFieldErrors = {};

    const payload: UpdateCustomMeasureRequest = {
      name: this.name?.trim() || undefined,
      description: this.description || undefined,
      category: this.category || undefined,
      year: this.year || undefined,
      owner: this.owner || undefined,
      clinicalFocus: this.clinicalFocus || undefined,
      reportingCadence: this.reportingCadence || undefined,
      targetThreshold: this.targetThreshold || undefined,
      priority: this.priority || undefined,
      implementationNotes: this.implementationNotes || undefined,
      tags: this.tags || undefined,
    };

    this.loading = true;
    this.customMeasureService.update(this.data.measure.id, payload).subscribe({
      next: (updatedMeasure) => {
        this.loading = false;
        this.dialogRef.close(updatedMeasure);
      },
      error: (error) => {
        this.loading = false;
        this.apiFieldErrors = extractApiFieldErrors(error);
        if (this.hasFieldErrors()) {
          return;
        }
        this.apiErrorMessage = getApiErrorMessage(
          error,
          'Failed to update measure details'
        );
      },
    });
  }

  canSave(): boolean {
    return (
      this.hasName() &&
      this.isYearValid() &&
      this.isReportingCadenceValid() &&
      this.isPriorityValid()
    );
  }

  clearApiFieldError(field: string): void {
    this.touchField(field);
    this.apiFieldErrors[field] = '';
    this.apiErrorMessage = '';
  }

  retrySave(): void {
    this.save();
  }

  hasName(): boolean {
    return !!this.name?.trim();
  }

  isYearValid(): boolean {
    return !this.year || (this.year >= 2000 && this.year <= 2100);
  }

  isReportingCadenceValid(): boolean {
    return MeasureMetadataDialogComponent.VALID_CADENCES.has(
      (this.reportingCadence || '').toUpperCase()
    );
  }

  isPriorityValid(): boolean {
    return MeasureMetadataDialogComponent.VALID_PRIORITIES.has(
      (this.priority || '').toUpperCase()
    );
  }

  touchField(field: string): void {
    this.touchedFields[field] = true;
  }

  shouldShowClientError(field: string): boolean {
    return this.showClientValidation || !!this.touchedFields[field];
  }

  private hasFieldErrors(): boolean {
    return Object.values(this.apiFieldErrors).some((message) => !!message);
  }

  private markAllValidationFieldsTouched(): void {
    this.touchField('name');
    this.touchField('year');
    this.touchField('reportingCadence');
    this.touchField('priority');
  }
}
