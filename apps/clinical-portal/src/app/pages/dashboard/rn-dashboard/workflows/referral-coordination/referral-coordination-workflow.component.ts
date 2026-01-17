/**
 * Referral Coordination Workflow Component
 *
 * Manages specialist referrals through a 5-step workflow:
 * 1. Review referral details
 * 2. Select appropriate specialist
 * 3. Verify insurance coverage
 * 4. Send referral request
 * 5. Track appointment and follow-up
 */

import { Component, Inject, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, takeUntil } from 'rxjs';
import { NurseWorkflowService } from '../../../../../services/nurse-workflow/nurse-workflow.service';
import { ToastService } from '../../../../../services/toast.service';
import { LoggerService, ContextualLogger } from '../../../../../services/logger.service';

export interface Specialist {
  id: string;
  name: string;
  specialty: string;
  npi?: string;
  phone?: string;
  acceptingPatients: boolean;
}

export interface InsuranceCoverage {
  covered: boolean;
  requiresPriorAuth?: boolean;
  reason?: string;
  priorAuthNumber?: string;
}

export interface ReferralCoordinationWorkflowData {
  referralId: string;
  patientId: string;
  patientName: string;
  referralType: string;
}

export interface ReferralCoordinationResult {
  success: boolean;
  result?: any;
  error?: string;
}

@Component({
  selector: 'app-referral-coordination-workflow',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatCheckboxModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatCardModule,
    MatTableModule,
    MatChipsModule,
    MatTooltipModule,
  ],
  templateUrl: './referral-coordination-workflow.component.html',
  styleUrls: ['./referral-coordination-workflow.component.scss'],
})
export class ReferralCoordinationWorkflowComponent implements OnInit, OnDestroy {
  @Input() referralId: string;
  @Input() patientId: string;
  @Input() patientName: string;
  @Input() referralType: string;
  @Output() workflowComplete = new EventEmitter<ReferralCoordinationResult>();

  form!: FormGroup;
  loading = false;
  currentStep = 0;
  totalSteps = 5;

  // Referral data
  availableSpecialists: Specialist[] = [];
  selectedSpecialistName = '';
  insuranceCoverage: InsuranceCoverage | null = null;
  requiresPriorAuth = false;
  referralSent = false;
  appointmentScheduled = false;

  // Table columns
  specialistColumns = ['name', 'phone', 'accepting', 'action'];

  private destroy$ = new Subject<void>();
  private log: ContextualLogger;

  constructor(
    private formBuilder: FormBuilder,
    private nurseWorkflowService: NurseWorkflowService,
    private toastService: ToastService,
    private logger: LoggerService,
    private dialogRef: MatDialogRef<ReferralCoordinationWorkflowComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ReferralCoordinationWorkflowData
  ) {
    this.log = this.logger.withContext('ReferralCoordinationWorkflowComponent');
    this.referralId = data.referralId;
    this.patientId = data.patientId;
    this.patientName = data.patientName;
    this.referralType = data.referralType;
    this.initializeForm();
  }

  ngOnInit(): void {
    this.loadReferral();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Initialize form
   */
  private initializeForm(): void {
    this.form = this.formBuilder.group(
      {
        // Step 0: Review
        referralReviewed: [false, Validators.required],
        referralReason: [''],

        // Step 1: Specialist Selection
        selectedSpecialist: ['', Validators.required],

        // Step 2: Insurance
        insuranceVerified: [false],
        acknowledgeInsurance: [false],
        priorAuthNumber: [''],

        // Step 3: Send Referral
        urgencyLevel: ['ROUTINE'],

        // Step 4: Follow-up
        appointmentDate: [''],
        appointmentNotes: [''],
        postVisitNotes: [''],
      },
      {
        validators: [this.insuranceValidator.bind(this)],
      }
    );
  }

  /**
   * Custom validator for insurance
   */
  private insuranceValidator(group: FormGroup): { [key: string]: any } | null {
    if (this.requiresPriorAuth && !group.get('acknowledgeInsurance')?.value) {
      return { insuranceAcknowledgmentRequired: true };
    }
    return null;
  }

  /**
   * Load referral details
   */
  private loadReferral(): void {
    this.loading = true;
    this.nurseWorkflowService.setTenantContext('TENANT001'); // TODO: Get from auth

    this.nurseWorkflowService
      .getReferralById(this.referralId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (referral: unknown) => {
          this.loading = false;
          this.log.info('Referral loaded successfully');
        },
        error: (error: unknown) => {
          this.log.error('Failed to load referral:', error);
          this.toastService.error('Failed to load referral details');
          this.loading = false;
        },
      });
  }

  /**
   * Check if can proceed to next step
   */
  canProceedToNextStep(): boolean {
    switch (this.currentStep) {
      case 0:
        return this.form.get('referralReviewed')?.value === true;
      case 1:
        return !!this.form.get('selectedSpecialist')?.value;
      case 2:
        if (this.requiresPriorAuth) {
          return this.form.get('acknowledgeInsurance')?.value === true;
        }
        return true;
      case 3:
        return this.referralSent;
      case 4:
        return true;
      default:
        return false;
    }
  }

  /**
   * Load available specialists
   */
  loadSpecialists(): void {
    this.loading = true;

    this.nurseWorkflowService
      .getSpecialistsForReferral(this.referralType, this.patientId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (specialists: Specialist[]) => {
          this.availableSpecialists = specialists || [];
          this.loading = false;
          this.log.info(`Loaded ${specialists.length} specialists`);
        },
        error: (error: unknown) => {
          this.log.error('Failed to load specialists:', error);
          this.toastService.error('Failed to load specialist list');
          this.loading = false;
        },
      });
  }

  /**
   * Handle specialist selection
   */
  onSpecialistSelected(): void {
    const specialistId = this.form.get('selectedSpecialist')?.value;
    const specialist = this.availableSpecialists.find((s) => s.id === specialistId);

    if (specialist) {
      this.selectedSpecialistName = specialist.name;
    }
  }

  /**
   * Verify insurance coverage
   */
  verifyInsuranceCoverage(): void {
    this.loading = true;
    const specialistId = this.form.get('selectedSpecialist')?.value;

    this.nurseWorkflowService
      .verifyInsuranceCoverage(this.patientId, specialistId, this.referralType)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (coverage: { covered: boolean; requiresPriorAuth?: boolean; reason?: string }) => {
          this.loading = false;
          this.insuranceCoverage = coverage;

          if (coverage.covered) {
            this.toastService.success('Insurance covers this referral');
            if (coverage.requiresPriorAuth) {
              this.requiresPriorAuth = true;
              this.toastService.warning('Prior authorization required');
            }
          } else {
            this.toastService.warning(`Coverage issue: ${coverage.reason}`);
          }

          this.form.patchValue({ insuranceVerified: true });
        },
        error: (error: unknown) => {
          this.loading = false;
          this.log.error('Failed to verify insurance:', error);
          this.toastService.error('Failed to verify insurance coverage');
        },
      });
  }

  /**
   * Send referral to specialist
   */
  sendReferral(): void {
    this.loading = true;
    const specialistId = this.form.get('selectedSpecialist')?.value;

    const referralData = {
      referralId: this.referralId,
      patientId: this.patientId,
      specialistId: specialistId,
      referralType: this.referralType,
      urgencyLevel: this.form.get('urgencyLevel')?.value,
      requiresPriorAuth: this.requiresPriorAuth,
      priorAuthNumber: this.form.get('priorAuthNumber')?.value,
      sentAt: new Date(),
    };

    this.nurseWorkflowService
      .sendReferral(referralData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result: unknown) => {
          this.loading = false;
          this.referralSent = true;
          this.toastService.success('Referral sent successfully');
          this.log.info('Referral sent to specialist');
        },
        error: (error: unknown) => {
          this.loading = false;
          this.log.error('Failed to send referral:', error);
          this.toastService.error('Failed to send referral');
        },
      });
  }

  /**
   * Get referral status (check appointment)
   */
  getReferralStatus(): void {
    this.loading = true;

    this.nurseWorkflowService
      .getReferralStatus(this.referralId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (status: { status: string; appointmentDate?: string }) => {
          this.loading = false;

          if (status.status === 'SCHEDULED') {
            this.appointmentScheduled = true;
            this.form.patchValue({ appointmentDate: status.appointmentDate });
            this.toastService.success('Appointment has been scheduled');
          } else if (status.status === 'PENDING') {
            this.toastService.info('Awaiting appointment scheduling');
          } else {
            this.toastService.warning(`Referral status: ${status.status}`);
          }

          this.log.info(`Referral status: ${status.status}`);
        },
        error: (error: unknown) => {
          this.loading = false;
          this.log.error('Failed to get referral status:', error);
          this.toastService.error('Failed to check referral status');
        },
      });
  }

  /**
   * Advance to next step
   */
  nextStep(): void {
    if (this.currentStep === 1) {
      this.loadSpecialists();
    } else if (this.currentStep === 2) {
      this.verifyInsuranceCoverage();
    } else if (this.currentStep === 3) {
      this.sendReferral();
    } else if (this.currentStep === 4) {
      this.getReferralStatus();
    }

    if (this.currentStep < this.totalSteps - 1 && this.canProceedToNextStep()) {
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
   * Complete referral coordination
   */
  completeReferralWorkflow(): void {
    if (!this.canProceedToNextStep()) {
      this.toastService.error('Please complete all required steps');
      return;
    }

    this.loading = true;

    const completionData = {
      referralId: this.referralId,
      specialistId: this.form.get('selectedSpecialist')?.value,
      appointmentDate: this.form.get('appointmentDate')?.value,
      postVisitNotes: this.form.get('postVisitNotes')?.value,
      completedAt: new Date(),
    };

    this.nurseWorkflowService
      .completeReferralCoordination(this.referralId, completionData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result: unknown) => {
          this.loading = false;
          this.toastService.success('Referral coordination completed');
          this.log.info('Workflow completed successfully');

          const workflowResult: ReferralCoordinationResult = {
            success: true,
            result: result,
          };

          this.workflowComplete.emit(workflowResult);
          this.dialogRef.close({ success: true, result });
        },
        error: (error: unknown) => {
          this.loading = false;
          this.log.error('Failed to complete referral coordination:', error);
          this.toastService.error('Failed to save referral coordination');
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
