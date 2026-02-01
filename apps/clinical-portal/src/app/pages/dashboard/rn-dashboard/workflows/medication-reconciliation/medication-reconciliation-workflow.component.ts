/**
 * Medication Reconciliation Workflow Component
 *
 * Manages medication list verification through a 6-step workflow:
 * 1. Load current medications
 * 2. Compare with patient report
 * 3. Identify discrepancies
 * 4. Mark duplicates/discontinued
 * 5. Add missing medications
 * 6. Check drug interactions and complete
 */

import { Component, Inject, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatStepperModule } from '@angular/material/stepper';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { Subject, takeUntil } from 'rxjs';
import { MedicationService } from '../../../../../services/medication/medication.service';
import { MedicationOrder, PrescriptionStatus } from '../../../../../services/medication/medication.models';
import { ToastService } from '../../../../../services/toast.service';
import { LoggerService, ContextualLogger } from '../../../../../services/logger.service';

export interface PatientReportedMedication {
  name: string;
  dosage: string;
  frequency: string;
  reason?: string;
  otherDetails?: string;
}

export interface MedicationDiscrepancy {
  id: string;
  type: 'MISSING_MEDICATION' | 'DISCONTINUED' | 'DUPLICATE_THERAPY' | 'DOSE_DISCREPANCY' | 'FREQUENCY_MISMATCH';
  severity: 'HIGH' | 'MEDIUM' | 'LOW';
  description: string;
  medication?: MedicationOrder;
  recommendation?: string;
}

export interface MedicationReconciliationWorkflowData {
  reconciliationId: string;
  patientId: string;
  patientName: string;
}

export interface MedicationReconciliationResult {
  success: boolean;
  result?: any;
  error?: string;
}

@Component({
  selector: 'app-medication-reconciliation-workflow',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatStepperModule,
    MatSelectModule,
    MatCheckboxModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatCardModule,
    MatTooltipModule,
    MatExpansionModule,
  ],
  templateUrl: './medication-reconciliation-workflow.component.html',
  styleUrls: ['./medication-reconciliation-workflow.component.scss'],
})
export class MedicationReconciliationWorkflowComponent implements OnInit, OnDestroy {
  @Input() reconciliationId: string;
  @Input() patientId: string;
  @Input() patientName: string;
  @Output() workflowComplete = new EventEmitter<MedicationReconciliationResult>();

  form!: FormGroup;
  newMedicationForm!: FormGroup;
  loading = false;
  currentStep = 0;
  totalSteps = 6;

  // Medication data
  systemMedications: MedicationOrder[] = [];
  patientReportedMedications: PatientReportedMedication[] = [];
  newMedications: any[] = [];
  discrepancies: MedicationDiscrepancy[] = [];

  // Tracking
  medicationMarking: any[] = [];
  hasSignificantInteractions = false;
  acknowledgedInteractions = false;

  // Tables
  medicationColumns = ['medication', 'dosage', 'frequency', 'status', 'actions'];
  discrepancyColumns = ['severity', 'type', 'description', 'recommendation', 'actions'];

  private destroy$ = new Subject<void>();

  constructor(
    private formBuilder: FormBuilder,
    private medicationService: MedicationService,
    private toastService: ToastService,
    private logger: LoggerService,
    private dialogRef: MatDialogRef<MedicationReconciliationWorkflowComponent>,
    @Inject(MAT_DIALOG_DATA) public data: MedicationReconciliationWorkflowData
  ) {    this.reconciliationId = data.reconciliationId;
    this.patientId = data.patientId;
    this.patientName = data.patientName;
    this.initializeForms();
  }

  ngOnInit(): void {
    this.loadSystemMedications();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Initialize all forms
   */
  private initializeForms(): void {
    this.form = this.formBuilder.group({
      patientMedications: this.formBuilder.array([]),
      acknowledgeDiscrepancies: [false],
      acknowledgeInteractions: [false],
    });

    this.newMedicationForm = this.formBuilder.group({
      medicationName: ['', Validators.required],
      dosage: ['', Validators.required],
      frequency: ['', Validators.required],
      indication: [''],
      prescriber: [''],
    });
  }

  /**
   * Load system medications from service
   */
  private loadSystemMedications(): void {
    this.loading = true;
    this.medicationService.setTenantContext('TENANT001'); // TODO: Get from auth

    this.medicationService
      .getActiveOrdersForPatient(this.patientId, 0, 100)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.systemMedications = response.content || [];
          this.logger.info(`Loaded ${this.systemMedications.length} medications`);
          this.loading = false;
        },
        error: (error) => {
          this.logger.error('Failed to load medications:', error);
          this.toastService.error('Failed to load medication list');
          this.loading = false;
        },
      });
  }

  /**
   * Get patient medications form array
   */
  get patientMedicationsArray(): FormArray {
    return this.form.get('patientMedications') as FormArray;
  }

  /**
   * Add blank patient medication entry
   */
  addPatientMedication(): void {
    const medicationForm = this.formBuilder.group({
      name: ['', Validators.required],
      dosage: ['', Validators.required],
      frequency: ['', Validators.required],
      reason: [''],
      otherDetails: [''],
    });

    this.patientMedicationsArray.push(medicationForm);
  }

  /**
   * Remove patient medication entry
   */
  removePatientMedication(index: number): void {
    this.patientMedicationsArray.removeAt(index);
  }

  /**
   * Step 1: Compare system medications with patient report
   */
  compareWithPatientReport(): void {
    this.patientReportedMedications = this.patientMedicationsArray.value;
    this.identifyDiscrepancies();
  }

  /**
   * Step 2: Identify all discrepancies
   */
  identifyDiscrepancies(): void {
    this.discrepancies = [];

    this.identifyMissingMedications();
    this.identifyDoseDiscrepancies();
    this.identifyDiscontinuedMedications();
    this.identifyDuplicateTherapy();
    this.identifyFrequencyMismatches();

    if (this.discrepancies.length > 0) {
      this.toastService.warning(`Found ${this.discrepancies.length} discrepancies`);
    }
  }

  /**
   * Identify medications in patient report but not in system
   */
  private identifyMissingMedications(): void {
    for (const patientMed of this.patientReportedMedications) {
      const found = this.systemMedications.some(
        (sysMed) =>
          sysMed.medication?.name?.toLowerCase().includes(patientMed.name.toLowerCase()) ||
          patientMed.name.toLowerCase().includes(sysMed.medication?.name?.toLowerCase() || '')
      );

      if (!found && patientMed.name) {
        this.discrepancies.push({
          id: `DISC_${Date.now()}_${Math.random()}`,
          type: 'MISSING_MEDICATION',
          severity: 'HIGH',
          description: `Patient reports taking "${patientMed.name}" but not found in system`,
          recommendation: 'Verify with patient and add to active medication list if appropriate',
        });
      }
    }
  }

  /**
   * Identify discontinued medications in system
   */
  private identifyDiscontinuedMedications(): void {
    for (const sysMed of this.systemMedications) {
      if (sysMed.prescriptionStatus === 'CANCELLED' || sysMed.prescriptionStatus === 'SUSPENDED') {
        // Check if patient still taking it
        const patientStillTaking = this.patientReportedMedications.some(
          (pMed) =>
            pMed.name.toLowerCase().includes(sysMed.medication?.name?.toLowerCase() || '') ||
            sysMed.medication?.name?.toLowerCase().includes(pMed.name.toLowerCase())
        );

        if (!patientStillTaking && sysMed.prescriptionStatus === 'CANCELLED') {
          this.discrepancies.push({
            id: sysMed.id || `temp-${Date.now()}`,
            type: 'DISCONTINUED',
            severity: 'MEDIUM',
            description: `${sysMed.medication?.name} is marked as discontinued but may still be needed`,
            medication: sysMed,
            recommendation: 'Confirm discontinuation with patient and prescriber',
          });
        }
      }
    }
  }

  /**
   * Identify dose discrepancies
   */
  private identifyDoseDiscrepancies(): void {
    for (const patientMed of this.patientReportedMedications) {
      const sysMed = this.systemMedications.find(
        (m) =>
          m.medication?.name?.toLowerCase().includes(patientMed.name.toLowerCase()) ||
          patientMed.name.toLowerCase().includes(m.medication?.name?.toLowerCase() || '')
      );

      if (sysMed && patientMed.dosage && patientMed.dosage !== `${sysMed.dosage} ${sysMed.dosageUnit}`) {
        this.discrepancies.push({
          id: `DISC_${sysMed.id}`,
          type: 'DOSE_DISCREPANCY',
          severity: 'HIGH',
          description: `Patient reports ${patientMed.dosage}, but system shows ${sysMed.dosage} ${sysMed.dosageUnit}`,
          medication: sysMed,
          recommendation: 'Clarify dose with patient and prescriber, update if necessary',
        });
      }
    }
  }

  /**
   * Identify duplicate therapies
   */
  identifyDuplicateTherapy(): void {
    const therapeuticClasses: { [key: string]: MedicationOrder[] } = {};

    for (const med of this.systemMedications) {
      const tc = med.medication?.therapeuticClass || 'UNKNOWN';
      if (!therapeuticClasses[tc]) {
        therapeuticClasses[tc] = [];
      }
      therapeuticClasses[tc].push(med);
    }

    for (const [tc, meds] of Object.entries(therapeuticClasses)) {
      if (meds.length > 1) {
        for (let i = 0; i < meds.length - 1; i++) {
          this.discrepancies.push({
            id: `DUPLICATE_${meds[i].id}_${meds[i + 1].id}`,
            type: 'DUPLICATE_THERAPY',
            severity: 'HIGH',
            description: `Multiple medications in ${tc} class: ${meds.map((m) => m.medication?.name).join(', ')}`,
            recommendation: 'Verify if therapeutic duplication is intentional',
          });
        }
      }
    }
  }

  /**
   * Identify frequency mismatches
   */
  private identifyFrequencyMismatches(): void {
    for (const patientMed of this.patientReportedMedications) {
      const sysMed = this.systemMedications.find((m) =>
        patientMed.name.toLowerCase().includes(m.medication?.name?.toLowerCase() || '')
      );

      if (sysMed && patientMed.frequency && patientMed.frequency !== sysMed.frequency) {
        this.discrepancies.push({
          id: `FREQ_${sysMed.id}`,
          type: 'FREQUENCY_MISMATCH',
          severity: 'MEDIUM',
          description: `Patient reports taking ${patientMed.frequency}, but system shows ${sysMed.frequency}`,
          medication: sysMed,
          recommendation: 'Clarify frequency with patient and update if needed',
        });
      }
    }
  }

  /**
   * Mark medication for discontinuation
   */
  markForDiscontinuation(medication: MedicationOrder): void {
    medication.prescriptionStatus = PrescriptionStatus.CANCELLED;
    this.medicationMarking.push({
      id: medication.id,
      status: 'DISCONTINUED',
    });
  }

  /**
   * Mark duplicate medications
   */
  markDuplicateTherapy(index1: number, index2: number): void {
    this.medicationMarking.push(
      {
        id: this.systemMedications[index1].id,
        status: 'DUPLICATE',
      },
      {
        id: this.systemMedications[index2].id,
        status: 'DUPLICATE',
      }
    );
  }

  /**
   * Unmark medication
   */
  unmarkMedication(medicationId: string): void {
    this.medicationMarking = this.medicationMarking.filter((m) => m.id !== medicationId);
  }

  /**
   * Initialize new medication form
   */
  initializeNewMedicationForm(): void {
    this.newMedicationForm.reset();
  }

  /**
   * Add new medication to list
   */
  addNewMedication(): void {
    if (this.newMedicationForm.valid) {
      this.newMedications.push(this.newMedicationForm.value);
      this.initializeNewMedicationForm();
      this.toastService.success('Medication added to list');
    }
  }

  /**
   * Remove new medication from list
   */
  removeNewMedication(index: number): void {
    this.newMedications.splice(index, 1);
  }

  /**
   * Check for drug interactions
   */
  checkDrugInteractions(): void {
    const medicationIds = this.systemMedications.map((m) => m.medicationId);

    if (medicationIds.length === 0) {
      return;
    }

    this.medicationService
      .checkDrugInteractionsBulk(this.patientId, medicationIds)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.hasSignificantInteractions = result.hasSignificantInteractions || false;
          if (this.hasSignificantInteractions) {
            this.toastService.warning('Significant drug interactions detected. Please review.');
          }
        },
        error: (error) => {
          this.logger.error('Failed to check interactions:', error);
          this.toastService.error('Could not check drug interactions');
        },
      });
  }

  /**
   * Check if reconciliation can be completed
   */
  canCompleteReconciliation(): boolean {
    if (this.hasSignificantInteractions && !this.acknowledgedInteractions) {
      return false;
    }
    return true;
  }

  /**
   * Advance to next step
   */
  nextStep(): void {
    if (this.currentStep === 1) {
      this.compareWithPatientReport();
    } else if (this.currentStep === 2) {
      // Identification already done
    } else if (this.currentStep === 5) {
      this.checkDrugInteractions();
    }

    if (this.currentStep < this.totalSteps - 1) {
      this.currentStep++;
    }
  }

  /**
   * Go to previous step
   */
  previousStep(): void {
    if (this.currentStep > 0) {
      this.currentStep--;
    }
  }

  /**
   * Complete reconciliation and save
   */
  completeReconciliation(): void {
    if (!this.canCompleteReconciliation()) {
      this.toastService.error('Please acknowledge all warnings before completing');
      return;
    }

    this.loading = true;

    const reconciliationData = {
      id: this.reconciliationId,
      patientId: this.patientId,
      systemMedications: this.systemMedications,
      patientReportedMedications: this.patientReportedMedications,
      discrepancies: this.discrepancies,
      newMedications: this.newMedications,
      medicationChanges: this.medicationMarking,
      completedAt: new Date(),
    };

    this.medicationService
      .completeMedicationReconciliation(this.reconciliationId, reconciliationData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result: unknown) => {
          this.loading = false;
          this.toastService.success('Medication reconciliation completed successfully');
          this.logger.info('Workflow completed successfully');

          const workflowResult: MedicationReconciliationResult = {
            success: true,
            result: result,
          };

          this.workflowComplete.emit(workflowResult);
          this.dialogRef.close({ success: true, result });
        },
        error: (error: unknown) => {
          this.loading = false;
          this.logger.error('Failed to complete reconciliation:', error);
          this.toastService.error('Failed to save medication reconciliation');
        },
      });
  }

  /**
   * Cancel workflow
   */
  cancelWorkflow(): void {
    this.dialogRef.close({ success: false });
  }
}
