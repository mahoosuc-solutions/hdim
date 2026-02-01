import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { LoggerService } from '../../services/logger.service';
import { PatientService } from '../../services/patient.service';
import { Patient, PatientSummary } from '../../models/patient.model';

/**
 * Patient Selection Dialog Component
 *
 * Allows users to search and select a patient for generating reports.
 *
 * Usage:
 * ```typescript
 * const dialogRef = this.dialog.open(PatientSelectionDialogComponent);
 * dialogRef.afterClosed().subscribe((patientId: string | null) => {
 *   if (patientId) {
 *     // Generate report for selected patient
 *   }
 * });
 * ```
 */
@Component({
  selector: 'app-patient-selection-dialog',
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTableModule,
  ],
  template: `
    <div class="dialog-container">
      <h2 mat-dialog-title>
        <mat-icon>person_search</mat-icon>
        Select Patient
      </h2>

      <mat-dialog-content>
        <!-- Search Input -->
        <mat-form-field class="search-field" appearance="outline">
          <mat-label>Search patients</mat-label>
          <input
            matInput
            [(ngModel)]="searchQuery"
            (ngModelChange)="onSearchChange()"
            placeholder="Search by name or MRN..."
            autocomplete="off"
          />
          <mat-icon matPrefix>search</mat-icon>
          @if (searchQuery) {
            <button
              mat-icon-button
              matSuffix
              (click)="clearSearch()"
              aria-label="Clear search"
            >
              <mat-icon>close</mat-icon>
            </button>
          }
        </mat-form-field>

        <!-- Loading State -->
        @if (isLoading()) {
          <div class="loading-container">
            <mat-spinner diameter="40"></mat-spinner>
            <p>Loading patients...</p>
          </div>
        }

        <!-- Empty State -->
        @else if (filteredPatients().length === 0 && !isLoading()) {
          <div class="empty-state">
            <mat-icon class="empty-icon">person_off</mat-icon>
            <h3>No Patients Found</h3>
            @if (searchQuery) {
              <p>No patients match "{{ searchQuery }}"</p>
              <button mat-button color="primary" (click)="clearSearch()">
                <mat-icon>clear</mat-icon>
                Clear Search
              </button>
            } @else {
              <p>No patients available in the system</p>
            }
          </div>
        }

        <!-- Patient List -->
        @else {
          <div class="patient-list">
            <table mat-table [dataSource]="filteredPatients()" class="patients-table">
              <!-- Name Column -->
              <ng-container matColumnDef="name">
                <th mat-header-cell *matHeaderCellDef>Name</th>
                <td mat-cell *matCellDef="let patient">
                  <div class="patient-name">
                    <mat-icon class="patient-icon">person</mat-icon>
                    <div class="name-info">
                      <div class="full-name">{{ patient.fullName }}</div>
                      @if (patient.mrn) {
                        <div class="mrn">
                          MRN: {{ patient.mrn }}
                          @if (patient.mrnAssigningAuthority) {
                            <span class="mrn-authority">({{ formatMRNAuthority(patient.mrnAssigningAuthority) }})</span>
                          }
                        </div>
                      }
                    </div>
                  </div>
                </td>
              </ng-container>

              <!-- Details Column -->
              <ng-container matColumnDef="details">
                <th mat-header-cell *matHeaderCellDef>Details</th>
                <td mat-cell *matCellDef="let patient">
                  <div class="patient-details">
                    @if (patient.dateOfBirth) {
                      <span class="detail-item">
                        <mat-icon class="detail-icon">cake</mat-icon>
                        {{ formatDate(patient.dateOfBirth) }}
                        @if (patient.age) {
                          <span class="age">({{ patient.age }} yrs)</span>
                        }
                      </span>
                    }
                    @if (patient.gender) {
                      <span class="detail-item">
                        <mat-icon class="detail-icon">{{
                          patient.gender === 'male' ? 'male' : patient.gender === 'female' ? 'female' : 'transgender'
                        }}</mat-icon>
                        {{ patient.gender | titlecase }}
                      </span>
                    }
                  </div>
                </td>
              </ng-container>

              <!-- Action Column -->
              <ng-container matColumnDef="action">
                <th mat-header-cell *matHeaderCellDef>Action</th>
                <td mat-cell *matCellDef="let patient">
                  <button
                    mat-raised-button
                    color="primary"
                    (click)="selectPatient(patient)"
                  >
                    <mat-icon>check</mat-icon>
                    Select
                  </button>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
              <tr
                mat-row
                *matRowDef="let row; columns: displayedColumns"
                [class.selected-row]="selectedPatient()?.id === row.id"
                (click)="highlightPatient(row)"
              ></tr>
            </table>
          </div>

          <div class="result-count">
            Showing {{ filteredPatients().length }} of {{ allPatients().length }} patients
          </div>
        }
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button (click)="onCancel()">
          <mat-icon>close</mat-icon>
          Cancel
        </button>
        <button
          mat-raised-button
          color="primary"
          [disabled]="!selectedPatient()"
          (click)="onConfirm()"
        >
          <mat-icon>check_circle</mat-icon>
          Confirm Selection
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .dialog-container {
        min-width: 600px;
        max-width: 800px;
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
        max-height: 60vh;
        overflow-y: auto;
      }

      .search-field {
        width: 100%;
        margin-bottom: 16px;
      }

      .loading-container {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 64px;
        gap: 16px;

        p {
          color: #666;
          font-size: 16px;
          margin: 0;
        }
      }

      .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 64px 32px;
        text-align: center;

        .empty-icon {
          font-size: 64px;
          width: 64px;
          height: 64px;
          color: #ccc;
          margin-bottom: 16px;
        }

        h3 {
          font-size: 20px;
          font-weight: 500;
          color: #333;
          margin: 0 0 8px 0;
        }

        p {
          color: #666;
          margin: 0 0 16px 0;
        }

        button mat-icon {
          margin-right: 8px;
        }
      }

      .patient-list {
        border: 1px solid #e0e0e0;
        border-radius: 4px;
        overflow: hidden;
      }

      .patients-table {
        width: 100%;

        th {
          background-color: #f5f5f5;
          font-weight: 600;
          color: #333;
        }

        td,
        th {
          padding: 12px 16px;
        }

        tr.mat-mdc-row {
          cursor: pointer;
          transition: background-color 0.2s;

          &:hover {
            background-color: #f9f9f9;
          }

          &.selected-row {
            background-color: #e3f2fd;
          }
        }
      }

      .patient-name {
        display: flex;
        align-items: center;
        gap: 12px;

        .patient-icon {
          font-size: 32px;
          width: 32px;
          height: 32px;
          color: #1976d2;
        }

        .name-info {
          display: flex;
          flex-direction: column;
          gap: 4px;

          .full-name {
            font-weight: 500;
            color: #1a1a1a;
            font-size: 15px;
          }

          .mrn {
            font-size: 12px;
            color: #666;

            .mrn-authority {
              font-size: 11px;
              color: #999;
              margin-left: 4px;
            }
          }
        }
      }

      .patient-details {
        display: flex;
        flex-direction: column;
        gap: 6px;

        .detail-item {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 14px;
          color: #555;

          .detail-icon {
            font-size: 16px;
            width: 16px;
            height: 16px;
            color: #777;
          }

          .age {
            color: #999;
            font-size: 13px;
          }
        }
      }

      .result-count {
        margin-top: 12px;
        padding: 8px 0;
        text-align: center;
        font-size: 13px;
        color: #666;
        border-top: 1px solid #e0e0e0;
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
export class PatientSelectionDialogComponent implements OnInit {
  searchQuery = '';
  displayedColumns = ['name', 'details', 'action'];

  allPatients = signal<PatientSummary[]>([]);
  filteredPatients = signal<PatientSummary[]>([]);
  selectedPatient = signal<PatientSummary | null>(null);
  isLoading = signal(false);

  constructor(
    private logger: LoggerService,
    private dialogRef: MatDialogRef<PatientSelectionDialogComponent>,
    private patientService: PatientService
  ) {}

  ngOnInit(): void {
    this.loadPatients();
  }

  /**
   * Load patients from the service
   */
  private loadPatients(): void {
    this.isLoading.set(true);
    this.patientService.getPatients(100).subscribe({
      next: (patients) => {
        const patientSummaries = patients.map((p) => this.toPatientSummary(p));
        this.allPatients.set(patientSummaries);
        this.filteredPatients.set(patientSummaries);
        this.isLoading.set(false);
      },
      error: (error) => {
        this.logger.error('Error loading patients:', { error });
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Convert FHIR Patient to PatientSummary
   */
  private toPatientSummary(patient: Patient): PatientSummary {
    const name = patient.name?.[0];
    const firstName = name?.given?.[0] || '';
    const lastName = name?.family || '';
    const fullName =
      name?.text || [firstName, lastName].filter(Boolean).join(' ') || 'Unknown Patient';

    // Extract MRN from identifiers - look for "Medical Record Number" in type.text
    const mrnIdentifier = patient.identifier?.find(
      (id) => id.type?.text === 'Medical Record Number'
    );
    const mrn = mrnIdentifier?.value;
    const mrnAssigningAuthority = mrnIdentifier?.system;

    let age: number | undefined;
    if (patient.birthDate) {
      const birthDate = new Date(patient.birthDate);
      const today = new Date();
      age = today.getFullYear() - birthDate.getFullYear();
      const monthDiff = today.getMonth() - birthDate.getMonth();
      if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
        age--;
      }
    }

    return {
      id: patient.id,
      mrn,
      mrnAssigningAuthority,
      fullName,
      firstName,
      lastName,
      dateOfBirth: patient.birthDate,
      age,
      gender: patient.gender,
      status: patient.active !== false ? 'Active' : 'Inactive',
    };
  }

  /**
   * Format MRN assigning authority for display
   */
  formatMRNAuthority(authority?: string): string {
    if (!authority) return '';
    // Extract domain from URL (e.g., "http://hospital.example.org/patients" -> "hospital.example.org")
    try {
      const url = new URL(authority);
      return url.hostname;
    } catch {
      // If not a valid URL, return as-is
      return authority;
    }
  }

  /**
   * Filter patients based on search query
   */
  onSearchChange(): void {
    const query = this.searchQuery.toLowerCase().trim();
    if (!query) {
      this.filteredPatients.set(this.allPatients());
      return;
    }

    const filtered = this.allPatients().filter((patient) => {
      return (
        patient.fullName.toLowerCase().includes(query) ||
        patient.mrn?.toLowerCase().includes(query) ||
        patient.firstName?.toLowerCase().includes(query) ||
        patient.lastName?.toLowerCase().includes(query)
      );
    });

    this.filteredPatients.set(filtered);
  }

  /**
   * Clear search query
   */
  clearSearch(): void {
    this.searchQuery = '';
    this.onSearchChange();
  }

  /**
   * Highlight patient on row click
   */
  highlightPatient(patient: PatientSummary): void {
    this.selectedPatient.set(patient);
  }

  /**
   * Select patient and close dialog
   */
  selectPatient(patient: PatientSummary): void {
    this.selectedPatient.set(patient);
    this.onConfirm();
  }

  /**
   * Confirm selection and close dialog
   */
  onConfirm(): void {
    const patient = this.selectedPatient();
    if (patient) {
      this.dialogRef.close(patient.id);
    }
  }

  /**
   * Cancel and close dialog
   */
  onCancel(): void {
    this.dialogRef.close(null);
  }

  /**
   * Format date for display
   */
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }
}
