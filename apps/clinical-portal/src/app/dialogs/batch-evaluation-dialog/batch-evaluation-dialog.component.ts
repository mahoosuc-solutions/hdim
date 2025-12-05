import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatInputModule } from '@angular/material/input';
import { SelectionModel } from '@angular/cdk/collections';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import { PatientSummary } from '../../models/patient.model';

export interface BatchEvaluationResult {
  successCount: number;
  errorCount: number;
  results: any[];
}

@Component({
  selector: 'app-batch-evaluation-dialog',
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatCheckboxModule,
    MatProgressBarModule,
    MatFormFieldModule,
    MatSelectModule,
    MatDatepickerModule,
    MatInputModule,
    LoadingButtonComponent,
  ],
  template: `
    <div class="batch-evaluation-dialog">
      <h2 mat-dialog-title>
        <mat-icon>analytics</mat-icon>
        Batch Evaluation
      </h2>

      <mat-dialog-content>
        @if (!isRunning() && !isComplete()) {
          <!-- Configuration Phase -->
          <div class="config-section">
            <h3>Select Patients</h3>
            <div class="table-actions">
              <button mat-button (click)="selectAll()">
                <mat-icon>select_all</mat-icon>
                Select All
              </button>
              <button mat-button (click)="deselectAll()">
                <mat-icon>deselect</mat-icon>
                Deselect All
              </button>
              <span class="selection-count">{{ selection.selected.length }} selected</span>
            </div>

            <div class="patients-table-container">
              <table mat-table [dataSource]="dataSource" class="patients-table">
                <ng-container matColumnDef="select">
                  <th mat-header-cell *matHeaderCellDef></th>
                  <td mat-cell *matCellDef="let row">
                    <mat-checkbox
                      (click)="$event.stopPropagation()"
                      (change)="$event ? selection.toggle(row) : null"
                      [checked]="selection.isSelected(row)"
                    ></mat-checkbox>
                  </td>
                </ng-container>

                <ng-container matColumnDef="name">
                  <th mat-header-cell *matHeaderCellDef>Name</th>
                  <td mat-cell *matCellDef="let patient">{{ patient.fullName }}</td>
                </ng-container>

                <ng-container matColumnDef="mrn">
                  <th mat-header-cell *matHeaderCellDef>MRN</th>
                  <td mat-cell *matCellDef="let patient">{{ patient.mrn }}</td>
                </ng-container>

                <ng-container matColumnDef="age">
                  <th mat-header-cell *matHeaderCellDef>Age</th>
                  <td mat-cell *matCellDef="let patient">{{ patient.age }}</td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
              </table>
            </div>

            <div class="evaluation-config">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Measure</mat-label>
                <mat-select [(value)]="selectedMeasure" multiple>
                  @for (measure of availableMeasures; track measure.id) {
                    <mat-option [value]="measure.id">{{ measure.name }}</mat-option>
                  }
                </mat-select>
                <mat-icon matPrefix>medical_services</mat-icon>
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Evaluation Date</mat-label>
                <input matInput [matDatepicker]="picker" [(ngModel)]="evaluationDate" />
                <mat-datepicker-toggle matSuffix [for]="picker"></mat-datepicker-toggle>
                <mat-datepicker #picker></mat-datepicker>
              </mat-form-field>
            </div>
          </div>
        } @else if (isRunning()) {
          <!-- Running Phase -->
          <div class="progress-section">
            <mat-icon class="progress-icon">hourglass_empty</mat-icon>
            <h3>Evaluating Patients...</h3>
            <p>Processing {{ currentIndex() }} of {{ selection.selected.length }}</p>
            <mat-progress-bar mode="determinate" [value]="progress()"></mat-progress-bar>
            <p class="progress-text">{{ progress() | number:'1.0-0' }}% Complete</p>
          </div>
        } @else {
          <!-- Complete Phase -->
          <div class="results-section">
            <mat-icon class="success-icon">check_circle</mat-icon>
            <h3>Evaluation Complete</h3>
            <div class="results-summary">
              <div class="result-item success">
                <mat-icon>check</mat-icon>
                <span>{{ successCount() }} Successful</span>
              </div>
              <div class="result-item error">
                <mat-icon>error</mat-icon>
                <span>{{ errorCount() }} Failed</span>
              </div>
            </div>
          </div>
        }
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        @if (!isRunning()) {
          <button mat-button (click)="onCancel()">
            <mat-icon>close</mat-icon>
            {{ isComplete() ? 'Close' : 'Cancel' }}
          </button>
          @if (!isComplete()) {
            <app-loading-button
              [disabled]="!canRun()"
              (click)="onRun()"
              color="primary"
              icon="play_arrow"
            >
              Run Evaluation
            </app-loading-button>
          }
        }
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .batch-evaluation-dialog { min-width: 700px; }
    h2[mat-dialog-title] { display: flex; align-items: center; gap: 12px; }
    .config-section { padding: 16px 0; }
    h3 { font-size: 18px; margin-bottom: 16px; }
    .table-actions { display: flex; gap: 8px; align-items: center; margin-bottom: 16px; }
    .selection-count { margin-left: auto; color: #666; }
    .patients-table-container { max-height: 300px; overflow-y: auto; border: 1px solid #e0e0e0; border-radius: 4px; }
    .evaluation-config { display: flex; flex-direction: column; gap: 16px; margin-top: 24px; }
    .full-width { width: 100%; }
    .progress-section, .results-section { text-align: center; padding: 48px 32px; }
    .progress-icon, .success-icon { font-size: 72px; width: 72px; height: 72px; color: #1976d2; }
    .success-icon { color: #4caf50; }
    .progress-text { margin-top: 16px; color: #666; }
    .results-summary { display: flex; justify-content: center; gap: 48px; margin-top: 24px; }
    .result-item { display: flex; align-items: center; gap: 8px; font-size: 18px; }
    .result-item.success { color: #4caf50; }
    .result-item.error { color: #f44336; }
  `],
})
export class BatchEvaluationDialogComponent implements OnInit {
  displayedColumns = ['select', 'name', 'mrn', 'age'];
  dataSource = new MatTableDataSource<PatientSummary>([]);
  selection = new SelectionModel<PatientSummary>(true, []);

  selectedMeasure: string[] = [];
  evaluationDate = new Date();

  availableMeasures = [
    { id: 'CMS125', name: 'Breast Cancer Screening' },
    { id: 'CMS130', name: 'Colorectal Cancer Screening' },
    { id: 'CMS134', name: 'Diabetes: Medical Attention for Nephropathy' },
  ];

  isRunning = signal(false);
  isComplete = signal(false);
  progress = signal(0);
  currentIndex = signal(0);
  successCount = signal(0);
  errorCount = signal(0);

  constructor(private dialogRef: MatDialogRef<BatchEvaluationDialogComponent>) {}

  ngOnInit(): void {
    // Load mock patients
    this.dataSource.data = this.generateMockPatients();
  }

  private generateMockPatients(): PatientSummary[] {
    return Array.from({ length: 20 }, (_, i) => ({
      id: `patient-${i + 1}`,
      mrn: `MRN-${1000 + i}`,
      fullName: `Patient ${i + 1}`,
      firstName: `First${i + 1}`,
      lastName: `Last${i + 1}`,
      age: 30 + i,
      gender: i % 2 === 0 ? 'male' : 'female',
      status: 'Active' as const,
    }));
  }

  selectAll(): void {
    this.dataSource.data.forEach(row => this.selection.select(row));
  }

  deselectAll(): void {
    this.selection.clear();
  }

  canRun(): boolean {
    return this.selection.selected.length > 0 && this.selectedMeasure.length > 0;
  }

  onRun(): void {
    this.isRunning.set(true);
    const total = this.selection.selected.length;
    let current = 0;

    const interval = setInterval(() => {
      current++;
      this.currentIndex.set(current);
      this.progress.set((current / total) * 100);

      // Simulate random success/failure
      if (Math.random() > 0.1) {
        this.successCount.update(c => c + 1);
      } else {
        this.errorCount.update(c => c + 1);
      }

      if (current >= total) {
        clearInterval(interval);
        this.isRunning.set(false);
        this.isComplete.set(true);
      }
    }, 200);
  }

  onCancel(): void {
    if (this.isComplete()) {
      const result: BatchEvaluationResult = {
        successCount: this.successCount(),
        errorCount: this.errorCount(),
        results: [],
      };
      this.dialogRef.close(result);
    } else {
      this.dialogRef.close(null);
    }
  }
}
