/**
 * Patient Outreach Workflow Component
 *
 * Manages patient contact, logging interactions, and scheduling follow-ups.
 * Implements a 5-step workflow:
 * 1. Select contact method (CALL, EMAIL, LETTER)
 * 2. Log contact attempt details
 * 3. Record outcome type
 * 4. Schedule follow-up (optional)
 * 5. Review and confirm
 */

import { Component, Inject, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatStepperModule } from '@angular/material/stepper';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, takeUntil } from 'rxjs';
import { NurseWorkflowService } from '../../../../../services/nurse-workflow/nurse-workflow.service';
import { OutreachLog } from '../../../../../services/nurse-workflow/nurse-workflow.models';
import { ToastService } from '../../../../../services/toast.service';
import { LoggerService, ContextualLogger } from '../../../../../services/logger.service';

export interface OutreachWorkflowData {
  outreachLogId: string;
  patientId: string;
  patientName: string;
}

export interface OutreachWorkflowResult {
  success: boolean;
  result?: OutreachLog;
  error?: string;
}

@Component({
  selector: 'app-patient-outreach-workflow',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatStepperModule,
    MatSelectModule,
    MatCheckboxModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatCardModule,
    MatTooltipModule,
  ],
  templateUrl: './patient-outreach-workflow.component.html',
  styleUrls: ['./patient-outreach-workflow.component.scss'],
})
export class PatientOutreachWorkflowComponent implements OnInit, OnDestroy {
  @Input() outreachLogId: string;
  @Input() patientId: string;
  @Input() patientName: string;
  @Output() workflowComplete = new EventEmitter<OutreachWorkflowResult>();

  form!: FormGroup;
  loading = false;
  currentStep = 0;
  totalSteps = 5;

  contactMethods = ['CALL', 'EMAIL', 'LETTER'];
  outcomeTypes = ['SUCCESSFUL', 'BUSY', 'VOICEMAIL', 'DISCONNECTED', 'NO_ANSWER', 'LEFT_MESSAGE'];

  private destroy$ = new Subject<void>();

  constructor(
    private formBuilder: FormBuilder,
    private nurseWorkflowService: NurseWorkflowService,
    private toastService: ToastService,
    private logger: LoggerService,
    private dialogRef: MatDialogRef<PatientOutreachWorkflowComponent>,
    @Inject(MAT_DIALOG_DATA) public data: OutreachWorkflowData
  ) {    this.outreachLogId = data.outreachLogId;
    this.patientId = data.patientId;
    this.patientName = data.patientName;
    this.initializeForm();
  }

  ngOnInit(): void {
    this.loadOutreachLog();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Initialize form with all fields needed across all steps
   */
  private initializeForm(): void {
    this.form = this.formBuilder.group(
      {
        // Step 0: Contact Method
        contactMethod: ['', [Validators.required]],

        // Step 1: Contact Attempt
        contactDuration: [0],
        notes: [''],

        // Step 2: Outcome Type
        outcomeType: [''],
        retryReason: [''],

        // Step 3: Follow-up
        scheduleFollowUp: [false],
        followUpDate: [{ value: null, disabled: true }],
        followUpReason: [''],

        // Additional tracking
        attemptedAt: [new Date()],
        completedAt: [null],
      },
      {
        validators: [this.outcomeTypeValidator.bind(this), this.followUpValidator.bind(this)],
      }
    );
  }

  /**
   * Custom validator: outcome type required after step 2
   */
  private outcomeTypeValidator(group: FormGroup): { [key: string]: any } | null {
    if (this.currentStep >= 2 && !group.get('outcomeType')?.value) {
      return { outcomeTypeRequired: true };
    }
    return null;
  }

  /**
   * Custom validator: follow-up validation
   */
  private followUpValidator(group: FormGroup): { [key: string]: any } | null {
    const scheduleFollowUp = group.get('scheduleFollowUp')?.value;
    const followUpDate = group.get('followUpDate')?.value;
    const followUpReason = group.get('followUpReason')?.value;

    if (!scheduleFollowUp) {
      return null;
    }

    if (!followUpDate) {
      return { followUpDateRequired: true };
    }

    if (!followUpReason) {
      return { followUpReasonRequired: true };
    }

    // Check if date is in future
    const selectedDate = new Date(followUpDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (selectedDate < today) {
      return { followUpDateInPast: true };
    }

    return null;
  }

  /**
   * Load existing outreach log from service
   */
  private loadOutreachLog(): void {
    this.loading = true;
    this.nurseWorkflowService.setTenantContext('TENANT001'); // TODO: Get from auth

    this.nurseWorkflowService
      .getOutreachLogById(this.outreachLogId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (outreachLog: OutreachLog) => {
          this.populateFormFromOutreachLog(outreachLog);
          this.loading = false;
        },
        error: (error: unknown) => {
          this.logger.error('Failed to load outreach log:', error);
          this.toastService.error('Failed to load outreach information');
          this.loading = false;
        },
      });
  }

  /**
   * Populate form with existing outreach log data
   */
  private populateFormFromOutreachLog(outreachLog: OutreachLog): void {
    this.form.patchValue(
      {
        contactMethod: outreachLog.contactMethod || '',
        notes: outreachLog.notes || '',
      },
      { emitEvent: false }
    );
  }

  /**
   * Check if current step is valid and can proceed
   */
  canProceedToNextStep(): boolean {
    switch (this.currentStep) {
      case 0:
        return !!this.form.get('contactMethod')?.value;
      case 1:
        return true; // Contact attempt is optional
      case 2:
        return !!this.form.get('outcomeType')?.value;
      case 3:
        if (this.form.get('scheduleFollowUp')?.value) {
          return !!this.form.get('followUpDate')?.value && !!this.form.get('followUpReason')?.value;
        }
        return true;
      case 4:
        return true; // Review step
      default:
        return false;
    }
  }

  /**
   * Check if outcome requires additional retry information
   */
  shouldShowRetryInfo(): boolean {
    const outcomeType = this.form.get('outcomeType')?.value;
    const retryOutcomes = ['BUSY', 'VOICEMAIL', 'NO_ANSWER'];
    return retryOutcomes.includes(outcomeType);
  }

  /**
   * Log contact attempt
   */
  logContactAttempt(): void {
    const contactMethod = this.form.get('contactMethod')?.value;
    const duration = this.form.get('contactDuration')?.value;
    const notes = this.form.get('notes')?.value;

    this.nurseWorkflowService
      .logContactAttempt(this.patientId, contactMethod, duration, notes)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result: unknown) => {
          this.logger.info('Contact attempt logged successfully');
        },
        error: (error: unknown) => {
          this.logger.error('Failed to log contact attempt:', error);
          this.toastService.error('Failed to log contact attempt');
        },
      });
  }

  /**
   * Get summary of workflow data for review step
   */
  getWorkflowSummary(): { [key: string]: any } {
    return {
      'Contact Method': this.form.get('contactMethod')?.value,
      'Contact Duration': `${this.form.get('contactDuration')?.value} minutes`,
      'Outcome Type': this.form.get('outcomeType')?.value,
      'Notes': this.form.get('notes')?.value || 'No notes',
      'Follow-up Scheduled': this.form.get('scheduleFollowUp')?.value ? 'Yes' : 'No',
      'Follow-up Date': this.form.get('followUpDate')?.value
        ? new Date(this.form.get('followUpDate')?.value).toLocaleDateString()
        : 'N/A',
      'Follow-up Reason': this.form.get('followUpReason')?.value || 'N/A',
    };
  }

  /**
   * Get icon for a contact method
   */
  getMethodIcon(method: string): string {
    const icons: { [key: string]: string } = {
      'CALL': 'phone',
      'EMAIL': 'email',
      'LETTER': 'mail',
    };
    return icons[method] || 'contact_support';
  }

  /**
   * Get description for a contact method
   */
  getMethodDescription(method: string): string {
    const descriptions: { [key: string]: string } = {
      'CALL': 'Phone call to patient',
      'EMAIL': 'Email communication',
      'LETTER': 'Written correspondence',
    };
    return descriptions[method] || method;
  }

  /**
   * Get icon for an outcome type
   */
  getOutcomeIcon(outcome: string): string {
    const icons: { [key: string]: string } = {
      'SUCCESSFUL': 'check_circle',
      'BUSY': 'phone_paused',
      'VOICEMAIL': 'voicemail',
      'DISCONNECTED': 'phone_disabled',
      'NO_ANSWER': 'phone_missed',
      'LEFT_MESSAGE': 'message',
    };
    return icons[outcome] || 'help';
  }

  /**
   * Get description for an outcome type
   */
  getOutcomeDescription(outcome: string): string {
    const descriptions: { [key: string]: string } = {
      'SUCCESSFUL': 'Successfully contacted patient',
      'BUSY': 'Line was busy',
      'VOICEMAIL': 'Left a voicemail message',
      'DISCONNECTED': 'Phone disconnected or invalid',
      'NO_ANSWER': 'No answer after multiple rings',
      'LEFT_MESSAGE': 'Left message with someone',
    };
    return descriptions[outcome] || outcome;
  }

  /**
   * Check if a previous step can be edited
   */
  canEditStep(step: number): boolean {
    return step < this.currentStep;
  }

  /**
   * Navigate to a previous step for editing
   */
  editStep(step: number): void {
    if (this.canEditStep(step)) {
      this.currentStep = step;
    }
  }

  /**
   * Advance to next step
   */
  nextStep(): void {
    if (this.currentStep < this.totalSteps - 1 && this.canProceedToNextStep()) {
      if (this.currentStep === 1) {
        this.logContactAttempt();
      }
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
   * Check if can go to previous step
   */
  canGoToPreviousStep(): boolean {
    return this.currentStep > 0;
  }

  /**
   * Check if can go to next step
   */
  canGoToNextStep(): boolean {
    return this.currentStep < this.totalSteps - 1 && this.canProceedToNextStep();
  }

  /**
   * Complete workflow and save to service
   */
  completeWorkflow(): void {
    if (!this.canProceedToNextStep()) {
      this.toastService.error('Please complete all required fields');
      return;
    }

    this.loading = true;
    const outreachLogUpdate = {
      id: this.outreachLogId,
      patientId: this.patientId,
      contactMethod: this.form.get('contactMethod')?.value,
      outcomeType: this.form.get('outcomeType')?.value,
      notes: this.form.get('notes')?.value,
      contactDuration: this.form.get('contactDuration')?.value,
      scheduleFollowUp: this.form.get('scheduleFollowUp')?.value,
      followUpDate: this.form.get('followUpDate')?.value,
      followUpReason: this.form.get('followUpReason')?.value,
      completedAt: new Date(),
    } as Partial<OutreachLog>;

    this.nurseWorkflowService
      .updateOutreachLog(this.outreachLogId, outreachLogUpdate)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result: OutreachLog) => {
          this.loading = false;
          this.toastService.success('Outreach logged successfully');
          this.logger.info('Workflow completed successfully');

          const workflowResult: OutreachWorkflowResult = {
            success: true,
            result: result,
          };

          this.workflowComplete.emit(workflowResult);
          this.dialogRef.close({ success: true, result });
        },
        error: (error: unknown) => {
          this.loading = false;
          this.logger.error('Failed to complete workflow:', error);
          this.toastService.error('Failed to save outreach information');
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
