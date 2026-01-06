import { Component, Inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CareGapAlert } from '../../../models/care-gap.model';

/**
 * Quick Action Types based on gap type
 */
export type QuickActionType =
  | 'ORDER_LAB'
  | 'SCHEDULE_VISIT'
  | 'SEND_REMINDER'
  | 'REVIEW_MEDS'
  | 'REFILL_REQUEST'
  | 'ORDER_TEST'
  | 'SCHEDULE_PROCEDURE'
  | 'SEND_EDUCATION'
  | 'SCHEDULE_SCREENING'
  | 'REFER_SPECIALIST'
  | 'CLOSE_GAP';

/**
 * Quick Action Configuration per gap type
 */
export interface QuickActionConfig {
  type: QuickActionType;
  label: string;
  icon: string;
  color: string;
  description: string;
}

/**
 * Quick Action Dialog Data
 */
export interface QuickActionDialogData {
  gap: CareGapAlert;
  actionType: QuickActionType;
}

/**
 * Quick Action Result
 */
export interface QuickActionResult {
  success: boolean;
  actionType: QuickActionType;
  data: any;
  timestamp: Date;
  closureRequested?: boolean;
}

/**
 * Lab Order Options
 */
const LAB_ORDER_OPTIONS = [
  { code: 'HBA1C', name: 'Hemoglobin A1c', duration: '1-2 days' },
  { code: 'FIT', name: 'Fecal Immunochemical Test (FIT)', duration: '3-5 days' },
  { code: 'LIPID', name: 'Lipid Panel', duration: '1-2 days' },
  { code: 'CBC', name: 'Complete Blood Count', duration: '1 day' },
  { code: 'CMP', name: 'Comprehensive Metabolic Panel', duration: '1-2 days' },
  { code: 'MICROALB', name: 'Urine Microalbumin', duration: '1-2 days' },
  { code: 'TSH', name: 'Thyroid Stimulating Hormone', duration: '1-2 days' },
  { code: 'PSA', name: 'Prostate-Specific Antigen', duration: '1-2 days' },
];

/**
 * Procedure Options
 */
const PROCEDURE_OPTIONS = [
  { code: 'MAMMOGRAM', name: 'Mammogram', location: 'Radiology' },
  { code: 'COLONOSCOPY', name: 'Colonoscopy', location: 'Gastroenterology' },
  { code: 'EYE_EXAM', name: 'Diabetic Eye Exam', location: 'Ophthalmology' },
  { code: 'FOOT_EXAM', name: 'Diabetic Foot Exam', location: 'Podiatry' },
  { code: 'BONE_DENSITY', name: 'Bone Density Scan (DEXA)', location: 'Radiology' },
  { code: 'PAP_SMEAR', name: 'Pap Smear', location: 'OB/GYN' },
];

/**
 * Reminder Templates
 */
const REMINDER_TEMPLATES = [
  { id: 'overdue', name: 'Overdue Screening Reminder', channel: 'SMS' },
  { id: 'appointment', name: 'Appointment Reminder', channel: 'SMS' },
  { id: 'lab_order', name: 'Lab Order Ready', channel: 'Email' },
  { id: 'wellness', name: 'Annual Wellness Visit', channel: 'Letter' },
  { id: 'followup', name: 'Follow-up Care Needed', channel: 'Phone' },
];

/**
 * Specialist Referral Options
 */
const SPECIALIST_OPTIONS = [
  { specialty: 'Cardiology', reason: 'Cardiovascular evaluation' },
  { specialty: 'Endocrinology', reason: 'Diabetes management' },
  { specialty: 'Gastroenterology', reason: 'GI screening/evaluation' },
  { specialty: 'Nephrology', reason: 'Kidney function assessment' },
  { specialty: 'Ophthalmology', reason: 'Diabetic eye exam' },
  { specialty: 'Behavioral Health', reason: 'Mental health assessment' },
  { specialty: 'Podiatry', reason: 'Diabetic foot care' },
];

@Component({
  selector: 'app-quick-action-dialog',
  standalone: true,
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
    MatCheckboxModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatTooltipModule,
  ],
  template: `
    <h2 mat-dialog-title class="dialog-title">
      <mat-icon [style.color]="getActionColor()">{{ getActionIcon() }}</mat-icon>
      {{ getActionTitle() }}
    </h2>

    <mat-dialog-content class="dialog-content">
      <!-- Patient Info Banner -->
      <div class="patient-banner">
        <div class="patient-info">
          <strong>{{ data.gap.patientName }}</strong>
          <span class="mrn">MRN: {{ data.gap.mrn }}</span>
        </div>
        <div class="gap-info">
          <span class="measure">{{ data.gap.measureName }}</span>
          <span class="description">{{ data.gap.gapDescription }}</span>
        </div>
      </div>

      <mat-divider></mat-divider>

      <!-- Order Lab Form -->
      @if (data.actionType === 'ORDER_LAB') {
        <form [formGroup]="labOrderForm" class="action-form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Lab Test</mat-label>
            <mat-select formControlName="labCode" required>
              @for (lab of labOptions; track lab.code) {
                <mat-option [value]="lab.code">
                  {{ lab.name }} ({{ lab.duration }})
                </mat-option>
              }
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Priority</mat-label>
            <mat-select formControlName="priority">
              <mat-option value="routine">Routine</mat-option>
              <mat-option value="urgent">Urgent</mat-option>
              <mat-option value="stat">STAT</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Ordering Provider</mat-label>
            <input matInput formControlName="provider" placeholder="Provider name">
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Special Instructions</mat-label>
            <textarea matInput formControlName="instructions" rows="2"
                      placeholder="Fasting required, etc."></textarea>
          </mat-form-field>

          <mat-checkbox formControlName="closeOnComplete" color="primary">
            Automatically close gap when lab results received
          </mat-checkbox>
        </form>
      }

      <!-- Schedule Visit Form -->
      @if (data.actionType === 'SCHEDULE_VISIT') {
        <form [formGroup]="visitForm" class="action-form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Visit Type</mat-label>
            <mat-select formControlName="visitType" required>
              <mat-option value="office">Office Visit</mat-option>
              <mat-option value="telehealth">Telehealth</mat-option>
              <mat-option value="wellness">Annual Wellness Visit</mat-option>
              <mat-option value="followup">Follow-up Visit</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Preferred Date</mat-label>
            <input matInput [matDatepicker]="picker" formControlName="preferredDate">
            <mat-datepicker-toggle matSuffix [for]="picker"></mat-datepicker-toggle>
            <mat-datepicker #picker></mat-datepicker>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Provider</mat-label>
            <input matInput formControlName="provider" placeholder="PCP or specialist">
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Reason for Visit</mat-label>
            <textarea matInput formControlName="reason" rows="2"
                      [value]="data.gap.gapDescription"></textarea>
          </mat-form-field>

          <mat-checkbox formControlName="sendReminder" color="primary">
            Send appointment reminder to patient
          </mat-checkbox>
        </form>
      }

      <!-- Send Reminder Form -->
      @if (data.actionType === 'SEND_REMINDER') {
        <form [formGroup]="reminderForm" class="action-form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Reminder Template</mat-label>
            <mat-select formControlName="template" required>
              @for (template of reminderTemplates; track template.id) {
                <mat-option [value]="template.id">
                  {{ template.name }} ({{ template.channel }})
                </mat-option>
              }
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Channel</mat-label>
            <mat-select formControlName="channel" required>
              <mat-option value="sms">SMS</mat-option>
              <mat-option value="email">Email</mat-option>
              <mat-option value="letter">Letter</mat-option>
              <mat-option value="phone">Phone Call</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Custom Message (Optional)</mat-label>
            <textarea matInput formControlName="customMessage" rows="3"
                      placeholder="Add personalized message..."></textarea>
          </mat-form-field>

          <mat-checkbox formControlName="scheduleFollowup" color="primary">
            Schedule follow-up reminder in 7 days if no response
          </mat-checkbox>
        </form>
      }

      <!-- Schedule Procedure Form -->
      @if (data.actionType === 'SCHEDULE_PROCEDURE' || data.actionType === 'ORDER_TEST') {
        <form [formGroup]="procedureForm" class="action-form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Procedure</mat-label>
            <mat-select formControlName="procedureCode" required>
              @for (proc of procedureOptions; track proc.code) {
                <mat-option [value]="proc.code">
                  {{ proc.name }} ({{ proc.location }})
                </mat-option>
              }
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Preferred Date</mat-label>
            <input matInput [matDatepicker]="procPicker" formControlName="preferredDate">
            <mat-datepicker-toggle matSuffix [for]="procPicker"></mat-datepicker-toggle>
            <mat-datepicker #procPicker></mat-datepicker>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Facility/Location</mat-label>
            <input matInput formControlName="facility" placeholder="Preferred facility">
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Pre-procedure Instructions</mat-label>
            <textarea matInput formControlName="instructions" rows="2"
                      placeholder="Prep instructions, etc."></textarea>
          </mat-form-field>

          <mat-checkbox formControlName="closeOnComplete" color="primary">
            Automatically close gap when procedure completed
          </mat-checkbox>
        </form>
      }

      <!-- Refer to Specialist Form -->
      @if (data.actionType === 'REFER_SPECIALIST') {
        <form [formGroup]="referralForm" class="action-form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Specialty</mat-label>
            <mat-select formControlName="specialty" required>
              @for (spec of specialistOptions; track spec.specialty) {
                <mat-option [value]="spec.specialty">
                  {{ spec.specialty }} - {{ spec.reason }}
                </mat-option>
              }
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Urgency</mat-label>
            <mat-select formControlName="urgency">
              <mat-option value="routine">Routine</mat-option>
              <mat-option value="urgent">Urgent (within 2 weeks)</mat-option>
              <mat-option value="emergent">Emergent (within 48 hours)</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Reason for Referral</mat-label>
            <textarea matInput formControlName="reason" rows="3" required
                      [value]="data.gap.gapDescription"></textarea>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Clinical Notes</mat-label>
            <textarea matInput formControlName="clinicalNotes" rows="2"
                      placeholder="Relevant history, current medications, etc."></textarea>
          </mat-form-field>
        </form>
      }

      <!-- Close Gap Form -->
      @if (data.actionType === 'CLOSE_GAP') {
        <form [formGroup]="closureForm" class="action-form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Closure Reason</mat-label>
            <mat-select formControlName="reason" required>
              @for (reason of suggestedReasons(); track reason.value) {
                <mat-option [value]="reason.value">
                  {{ reason.label }}
                  @if (reason.suggested) {
                    <mat-chip class="suggested-chip">Suggested</mat-chip>
                  }
                </mat-option>
              }
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Closure Date</mat-label>
            <input matInput [matDatepicker]="closurePicker" formControlName="closureDate" required>
            <mat-datepicker-toggle matSuffix [for]="closurePicker"></mat-datepicker-toggle>
            <mat-datepicker #closurePicker></mat-datepicker>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Evidence/Documentation Reference</mat-label>
            <input matInput formControlName="evidence"
                   placeholder="Lab result ID, procedure note, etc.">
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Notes</mat-label>
            <textarea matInput formControlName="notes" rows="2"
                      placeholder="Additional closure notes..."></textarea>
          </mat-form-field>
        </form>
      }

      <!-- Processing State -->
      @if (processing()) {
        <div class="processing-overlay">
          <mat-spinner diameter="48"></mat-spinner>
          <p>Processing {{ getActionTitle() }}...</p>
        </div>
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Cancel</button>
      <button mat-raised-button [color]="data.actionType === 'CLOSE_GAP' ? 'accent' : 'primary'"
              (click)="submit()"
              [disabled]="!isFormValid() || processing()">
        <mat-icon>{{ getActionIcon() }}</mat-icon>
        {{ getSubmitLabel() }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-title {
      display: flex;
      align-items: center;
      gap: 8px;

      mat-icon {
        font-size: 28px;
        width: 28px;
        height: 28px;
      }
    }

    .dialog-content {
      min-width: 450px;
      max-width: 550px;
    }

    .patient-banner {
      background: #f5f5f5;
      border-radius: 8px;
      padding: 16px;
      margin-bottom: 16px;

      .patient-info {
        display: flex;
        align-items: center;
        gap: 16px;
        margin-bottom: 8px;

        strong {
          font-size: 16px;
        }

        .mrn {
          color: #666;
          font-size: 13px;
        }
      }

      .gap-info {
        .measure {
          display: inline-block;
          background: #e3f2fd;
          color: #1976d2;
          padding: 2px 8px;
          border-radius: 4px;
          font-size: 12px;
          font-weight: 500;
          margin-right: 8px;
        }

        .description {
          color: #666;
          font-size: 13px;
        }
      }
    }

    .action-form {
      display: flex;
      flex-direction: column;
      gap: 8px;
      padding-top: 16px;

      .full-width {
        width: 100%;
      }

      mat-checkbox {
        margin-top: 8px;
      }
    }

    .suggested-chip {
      font-size: 10px;
      min-height: 18px;
      padding: 0 6px;
      margin-left: 8px;
      background: #4caf50 !important;
      color: white !important;
    }

    .processing-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(255, 255, 255, 0.9);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 16px;
      z-index: 10;

      p {
        color: #666;
        font-size: 14px;
      }
    }

    mat-dialog-actions {
      padding: 16px 24px;
      gap: 8px;
    }
  `],
})
export class QuickActionDialogComponent {
  // Options
  labOptions = LAB_ORDER_OPTIONS;
  procedureOptions = PROCEDURE_OPTIONS;
  reminderTemplates = REMINDER_TEMPLATES;
  specialistOptions = SPECIALIST_OPTIONS;

  // State
  processing = signal(false);

  // Forms
  labOrderForm: FormGroup;
  visitForm: FormGroup;
  reminderForm: FormGroup;
  procedureForm: FormGroup;
  referralForm: FormGroup;
  closureForm: FormGroup;

  // Computed suggested closure reasons based on gap type
  suggestedReasons = computed(() => {
    const gapType = this.data.gap.gapType;
    const measureName = this.data.gap.measureName.toLowerCase();

    const reasons = [
      { value: 'completed', label: 'Care completed - Gap resolved', suggested: false },
      { value: 'not-applicable', label: 'Not applicable to patient', suggested: false },
      { value: 'patient-declined', label: 'Patient declined', suggested: false },
      { value: 'contraindicated', label: 'Medically contraindicated', suggested: false },
      { value: 'already-done', label: 'Service already performed elsewhere', suggested: false },
      { value: 'other', label: 'Other', suggested: false },
    ];

    // Mark suggested reasons based on gap type
    if (gapType === 'screening' || gapType === 'lab') {
      reasons[0].suggested = true; // completed
      reasons[4].suggested = true; // already done
    } else if (gapType === 'medication') {
      reasons[0].suggested = true; // completed
      reasons[3].suggested = true; // contraindicated
    } else if (gapType === 'followup') {
      reasons[0].suggested = true; // completed
    }

    // Sort to put suggested first
    return reasons.sort((a, b) => (b.suggested ? 1 : 0) - (a.suggested ? 1 : 0));
  });

  constructor(
    public dialogRef: MatDialogRef<QuickActionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: QuickActionDialogData,
    private fb: FormBuilder
  ) {
    // Initialize all forms
    this.labOrderForm = this.fb.group({
      labCode: ['', Validators.required],
      priority: ['routine'],
      provider: [''],
      instructions: [''],
      closeOnComplete: [true],
    });

    this.visitForm = this.fb.group({
      visitType: ['office', Validators.required],
      preferredDate: [null],
      provider: [''],
      reason: [data.gap.gapDescription],
      sendReminder: [true],
    });

    this.reminderForm = this.fb.group({
      template: ['', Validators.required],
      channel: ['sms', Validators.required],
      customMessage: [''],
      scheduleFollowup: [true],
    });

    this.procedureForm = this.fb.group({
      procedureCode: ['', Validators.required],
      preferredDate: [null],
      facility: [''],
      instructions: [''],
      closeOnComplete: [true],
    });

    this.referralForm = this.fb.group({
      specialty: ['', Validators.required],
      urgency: ['routine'],
      reason: [data.gap.gapDescription, Validators.required],
      clinicalNotes: [''],
    });

    this.closureForm = this.fb.group({
      reason: ['', Validators.required],
      closureDate: [new Date(), Validators.required],
      evidence: [''],
      notes: [''],
    });

    // Pre-select appropriate lab/procedure based on measure
    this.preselectOptions();
  }

  /**
   * Pre-select options based on the measure type
   */
  private preselectOptions(): void {
    const measureName = this.data.gap.measureName.toLowerCase();

    // Lab suggestions
    if (measureName.includes('diabetes') || measureName.includes('cdc')) {
      this.labOrderForm.patchValue({ labCode: 'HBA1C' });
    } else if (measureName.includes('colorectal') || measureName.includes('col')) {
      this.labOrderForm.patchValue({ labCode: 'FIT' });
    } else if (measureName.includes('statin') || measureName.includes('spc')) {
      this.labOrderForm.patchValue({ labCode: 'LIPID' });
    }

    // Procedure suggestions
    if (measureName.includes('breast') || measureName.includes('bcs')) {
      this.procedureForm.patchValue({ procedureCode: 'MAMMOGRAM' });
    } else if (measureName.includes('colorectal') || measureName.includes('col')) {
      this.procedureForm.patchValue({ procedureCode: 'COLONOSCOPY' });
    } else if (measureName.includes('diabetes') && this.data.gap.gapDescription.toLowerCase().includes('eye')) {
      this.procedureForm.patchValue({ procedureCode: 'EYE_EXAM' });
    } else if (measureName.includes('diabetes') && this.data.gap.gapDescription.toLowerCase().includes('foot')) {
      this.procedureForm.patchValue({ procedureCode: 'FOOT_EXAM' });
    }

    // Specialist suggestions
    if (measureName.includes('diabetes')) {
      this.referralForm.patchValue({ specialty: 'Endocrinology' });
    } else if (measureName.includes('blood pressure') || measureName.includes('cbp')) {
      this.referralForm.patchValue({ specialty: 'Cardiology' });
    }
  }

  getActionTitle(): string {
    const titles: Record<QuickActionType, string> = {
      ORDER_LAB: 'Order Lab Test',
      SCHEDULE_VISIT: 'Schedule Visit',
      SEND_REMINDER: 'Send Reminder',
      REVIEW_MEDS: 'Review Medications',
      REFILL_REQUEST: 'Request Refill',
      ORDER_TEST: 'Order Test',
      SCHEDULE_PROCEDURE: 'Schedule Procedure',
      SEND_EDUCATION: 'Send Education Materials',
      SCHEDULE_SCREENING: 'Schedule Screening',
      REFER_SPECIALIST: 'Refer to Specialist',
      CLOSE_GAP: 'Close Care Gap',
    };
    return titles[this.data.actionType] || 'Quick Action';
  }

  getActionIcon(): string {
    const icons: Record<QuickActionType, string> = {
      ORDER_LAB: 'biotech',
      SCHEDULE_VISIT: 'event',
      SEND_REMINDER: 'notifications',
      REVIEW_MEDS: 'medication',
      REFILL_REQUEST: 'replay',
      ORDER_TEST: 'science',
      SCHEDULE_PROCEDURE: 'medical_services',
      SEND_EDUCATION: 'school',
      SCHEDULE_SCREENING: 'health_and_safety',
      REFER_SPECIALIST: 'send',
      CLOSE_GAP: 'check_circle',
    };
    return icons[this.data.actionType] || 'flash_on';
  }

  getActionColor(): string {
    const colors: Record<QuickActionType, string> = {
      ORDER_LAB: '#2196f3',
      SCHEDULE_VISIT: '#4caf50',
      SEND_REMINDER: '#ff9800',
      REVIEW_MEDS: '#9c27b0',
      REFILL_REQUEST: '#9c27b0',
      ORDER_TEST: '#2196f3',
      SCHEDULE_PROCEDURE: '#e91e63',
      SEND_EDUCATION: '#00bcd4',
      SCHEDULE_SCREENING: '#4caf50',
      REFER_SPECIALIST: '#795548',
      CLOSE_GAP: '#4caf50',
    };
    return colors[this.data.actionType] || '#2196f3';
  }

  getSubmitLabel(): string {
    const labels: Record<QuickActionType, string> = {
      ORDER_LAB: 'Place Order',
      SCHEDULE_VISIT: 'Schedule',
      SEND_REMINDER: 'Send',
      REVIEW_MEDS: 'Complete Review',
      REFILL_REQUEST: 'Submit Request',
      ORDER_TEST: 'Place Order',
      SCHEDULE_PROCEDURE: 'Schedule',
      SEND_EDUCATION: 'Send Materials',
      SCHEDULE_SCREENING: 'Schedule',
      REFER_SPECIALIST: 'Send Referral',
      CLOSE_GAP: 'Close Gap',
    };
    return labels[this.data.actionType] || 'Submit';
  }

  isFormValid(): boolean {
    switch (this.data.actionType) {
      case 'ORDER_LAB':
        return this.labOrderForm.valid;
      case 'SCHEDULE_VISIT':
        return this.visitForm.valid;
      case 'SEND_REMINDER':
        return this.reminderForm.valid;
      case 'SCHEDULE_PROCEDURE':
      case 'ORDER_TEST':
        return this.procedureForm.valid;
      case 'REFER_SPECIALIST':
        return this.referralForm.valid;
      case 'CLOSE_GAP':
        return this.closureForm.valid;
      default:
        return true;
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  submit(): void {
    if (!this.isFormValid()) return;

    this.processing.set(true);

    // Simulate async operation
    setTimeout(() => {
      const result: QuickActionResult = {
        success: true,
        actionType: this.data.actionType,
        data: this.getFormData(),
        timestamp: new Date(),
        closureRequested: this.shouldCloseGap(),
      };

      this.processing.set(false);
      this.dialogRef.close(result);
    }, 1000);
  }

  private getFormData(): any {
    switch (this.data.actionType) {
      case 'ORDER_LAB':
        return this.labOrderForm.value;
      case 'SCHEDULE_VISIT':
        return this.visitForm.value;
      case 'SEND_REMINDER':
        return this.reminderForm.value;
      case 'SCHEDULE_PROCEDURE':
      case 'ORDER_TEST':
        return this.procedureForm.value;
      case 'REFER_SPECIALIST':
        return this.referralForm.value;
      case 'CLOSE_GAP':
        return this.closureForm.value;
      default:
        return {};
    }
  }

  private shouldCloseGap(): boolean {
    if (this.data.actionType === 'CLOSE_GAP') return true;
    if (this.data.actionType === 'ORDER_LAB') return this.labOrderForm.value.closeOnComplete;
    if (this.data.actionType === 'SCHEDULE_PROCEDURE' || this.data.actionType === 'ORDER_TEST') {
      return this.procedureForm.value.closeOnComplete;
    }
    return false;
  }
}
