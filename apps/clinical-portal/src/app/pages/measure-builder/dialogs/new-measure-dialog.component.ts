import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { LoadingButtonComponent } from '../../../shared/components/loading-button/loading-button.component';
import { CreateCustomMeasureRequest } from '../../../services/custom-measure.service';

@Component({
  selector: 'app-new-measure-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    LoadingButtonComponent,
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon color="primary">add_circle</mat-icon>
      Create New Measure
    </h2>

    <mat-dialog-content class="dialog-content">
      <form #measureForm="ngForm">
        <!-- Measure Name -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Measure Name</mat-label>
          <input
            matInput
            [(ngModel)]="name"
            name="name"
            required
            placeholder="e.g., Custom Diabetes Screening"
            #nameInput="ngModel">
          <mat-hint>A descriptive name for your quality measure</mat-hint>
          @if (nameInput.invalid && nameInput.touched) {
            <mat-error>Measure name is required</mat-error>
          }
        </mat-form-field>

        <!-- Description -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Description</mat-label>
          <textarea
            matInput
            [(ngModel)]="description"
            name="description"
            rows="3"
            placeholder="What this measure evaluates and why it matters"></textarea>
          <mat-hint>Explain the purpose and clinical significance</mat-hint>
        </mat-form-field>

        <!-- Category -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Category</mat-label>
          <mat-select [(ngModel)]="category" name="category">
            <mat-option value="CUSTOM">Custom</mat-option>
            <mat-option value="HEDIS">HEDIS-Derived</mat-option>
            <mat-option value="CMS">CMS-Derived</mat-option>
            <mat-option value="QUALITY">Quality Improvement</mat-option>
            <mat-option value="COMPLIANCE">Compliance</mat-option>
          </mat-select>
          <mat-hint>Categorize your measure for organization</mat-hint>
        </mat-form-field>

        <!-- Year (Optional) -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Year (Optional)</mat-label>
          <input
            matInput
            type="number"
            [(ngModel)]="year"
            name="year"
            [min]="2000"
            [max]="2030"
            placeholder="2024">
          <mat-hint>Reporting year or specification version</mat-hint>
        </mat-form-field>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>
        <mat-icon>close</mat-icon>
        Cancel
      </button>
      <app-loading-button
        text="Create Draft"
        icon="save"
        color="primary"
        variant="raised"
        [disabled]="!measureForm.valid || !name.trim()"
        ariaLabel="Create measure draft"
        (buttonClick)="save()">
      </app-loading-button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-content {
      min-width: 500px;
      padding: 24px 24px 16px;
    }

    .full-width {
      width: 100%;
      margin-bottom: 16px;
    }

    h2 {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0;
      padding: 16px 24px;
      background-color: #f5f5f5;
      border-bottom: 1px solid #e0e0e0;

      mat-icon {
        font-size: 28px;
        width: 28px;
        height: 28px;
      }
    }

    mat-dialog-actions {
      padding: 16px 24px;
      border-top: 1px solid #e0e0e0;
      gap: 8px;
    }

    @media (max-width: 600px) {
      .dialog-content {
        min-width: 300px;
      }
    }
  `],
})
export class NewMeasureDialogComponent {
  name = '';
  description = '';
  category = 'CUSTOM';
  year?: number;

  constructor(private dialogRef: MatDialogRef<NewMeasureDialogComponent>) {}

  save(): void {
    const trimmed = this.name.trim();
    if (!trimmed) return;

    const draft: CreateCustomMeasureRequest = {
      name: trimmed,
      description: this.description.trim(),
      category: this.category,
      year: this.year,
      createdBy: 'clinical-portal',
    };

    this.dialogRef.close(draft);
  }
}
