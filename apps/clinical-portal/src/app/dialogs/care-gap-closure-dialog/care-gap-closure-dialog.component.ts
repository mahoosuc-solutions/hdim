/**
 * Care Gap Closure Dialog Component
 *
 * Issue #7: Provider-Optimized Care Gap Closure Workflow
 * Provides quick actions for closing care gaps in 2-3 clicks:
 * - Order Lab/Test
 * - Schedule Appointment
 * - E-Prescribe
 * - Patient Declined
 * - Manual Closure with smart reason suggestions
 *
 * Features:
 * - Pre-filled order dialogs based on gap type
 * - Smart closure reasons based on measure type
 * - Bulk closure capability for similar gaps
 * - Closure time tracking for workflow optimization
 */

import { Component, Inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  MatDialogModule,
  MatDialogRef,
  MAT_DIALOG_DATA,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatRadioModule } from '@angular/material/radio';
import {
  CareGapService,
  CareGap,
  CareGapType,
  CareGapClosureRequest,
  InterventionType,
  Intervention,
} from '../../services/care-gap.service';
import { NotificationService } from '../../services/notification.service';

/**
 * Dialog data interface
 */
export interface CareGapClosureDialogData {
  gap: CareGapForClosure;
  relatedGaps?: CareGapForClosure[];
  patientName: string;
  patientMRN: string;
}

/**
 * Gap data for closure dialog
 */
export interface CareGapForClosure {
  id: string;
  patientId: string;
  gapType: string;
  measureName: string;
  description: string;
  recommendation: string;
  priority: 'critical' | 'high' | 'moderate' | 'low';
  dueDate?: string;
}

/**
 * Quick action types
 */
export type QuickActionType = 'order-lab' | 'schedule-visit' | 'e-prescribe' | 'patient-declined' | 'manual-close' | 'already-done';

/**
 * Quick action configuration
 */
interface QuickAction {
  type: QuickActionType;
  label: string;
  icon: string;
  color: string;
  description: string;
  closureReason: string;
  interventionType?: InterventionType;
}

/**
 * Closure reason suggestion
 */
interface ClosureReasonSuggestion {
  reason: string;
  category: string;
}

/**
 * Dialog result
 */
export interface CareGapClosureResult {
  action: QuickActionType;
  gapId: string;
  closedGapIds: string[];
  closureRequest: CareGapClosureRequest;
  intervention?: Intervention;
  closureTimeMs: number;
}

@Component({
  selector: 'app-care-gap-closure-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatListModule,
    MatTooltipModule,
    MatRadioModule,
  ],
  templateUrl: './care-gap-closure-dialog.component.html',
  styleUrls: ['./care-gap-closure-dialog.component.scss'],
})
export class CareGapClosureDialogComponent implements OnInit {
  // Signals for reactive state management
  isProcessing = signal(false);
  selectedAction = signal<QuickActionType | null>(null);
  selectedRelatedGaps = signal<string[]>([]);
  showManualForm = signal(false);

  // Form for manual closure
  manualClosureForm!: FormGroup;

  // Track dialog open time for metrics
  private dialogOpenTime: number;

  // Quick actions available based on gap type
  quickActions: QuickAction[] = [];

  // Closure reason suggestions based on gap type
  closureReasonSuggestions: ClosureReasonSuggestion[] = [];

  // Computed: Check if bulk closure is available
  hasBulkOption = computed(() => {
    return this.data.relatedGaps && this.data.relatedGaps.length > 0;
  });

  constructor(
    private dialogRef: MatDialogRef<CareGapClosureDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CareGapClosureDialogData,
    private fb: FormBuilder,
    private careGapService: CareGapService,
    private notificationService: NotificationService
  ) {
    this.dialogOpenTime = Date.now();
  }

  ngOnInit(): void {
    this.initializeQuickActions();
    this.initializeClosureReasons();
    this.initializeForm();
  }

  /**
   * Initialize quick actions based on gap type
   */
  private initializeQuickActions(): void {
    const gapType = this.data.gap.gapType.toLowerCase();

    // Base actions available for all gap types
    const baseActions: QuickAction[] = [
      {
        type: 'already-done',
        label: 'Already Done',
        icon: 'check_circle',
        color: '#4caf50',
        description: 'Care was already provided (not in system)',
        closureReason: 'Care completed outside system',
        interventionType: InterventionType.OTHER,
      },
      {
        type: 'patient-declined',
        label: 'Patient Declined',
        icon: 'do_not_disturb',
        color: '#f44336',
        description: 'Patient declined recommended care',
        closureReason: 'Patient declined recommended care',
        interventionType: InterventionType.EDUCATION,
      },
      {
        type: 'manual-close',
        label: 'Other Reason',
        icon: 'edit_note',
        color: '#757575',
        description: 'Specify closure reason manually',
        closureReason: '',
        interventionType: InterventionType.OTHER,
      },
    ];

    // Gap-type specific actions
    const typeSpecificActions: QuickAction[] = [];

    if (this.isLabRelatedGap(gapType)) {
      typeSpecificActions.push({
        type: 'order-lab',
        label: 'Order Lab',
        icon: 'science',
        color: '#2196f3',
        description: 'Order the required lab test',
        closureReason: 'Lab ordered',
        interventionType: InterventionType.APPOINTMENT_SCHEDULED,
      });
    }

    if (this.isScreeningGap(gapType)) {
      typeSpecificActions.push({
        type: 'schedule-visit',
        label: 'Schedule Screening',
        icon: 'event',
        color: '#9c27b0',
        description: 'Schedule screening appointment',
        closureReason: 'Screening appointment scheduled',
        interventionType: InterventionType.APPOINTMENT_SCHEDULED,
      });
    }

    if (this.isMedicationGap(gapType)) {
      typeSpecificActions.push({
        type: 'e-prescribe',
        label: 'E-Prescribe',
        icon: 'medication',
        color: '#ff9800',
        description: 'Send prescription electronically',
        closureReason: 'Medication prescribed',
        interventionType: InterventionType.MEDICATION_REVIEW,
      });
    }

    if (this.isFollowUpGap(gapType)) {
      typeSpecificActions.push({
        type: 'schedule-visit',
        label: 'Schedule Follow-up',
        icon: 'event_repeat',
        color: '#009688',
        description: 'Schedule follow-up appointment',
        closureReason: 'Follow-up appointment scheduled',
        interventionType: InterventionType.APPOINTMENT_SCHEDULED,
      });
    }

    // Combine type-specific actions first, then base actions
    this.quickActions = [...typeSpecificActions, ...baseActions];
  }

  /**
   * Initialize closure reason suggestions based on gap type
   */
  private initializeClosureReasons(): void {
    const gapType = this.data.gap.gapType.toLowerCase();
    const measureName = this.data.gap.measureName.toLowerCase();

    // Common reasons
    const commonReasons: ClosureReasonSuggestion[] = [
      { reason: 'Patient declined recommended care', category: 'Patient Choice' },
      { reason: 'Care completed at external facility', category: 'External Care' },
      { reason: 'Unable to contact patient', category: 'Access Issue' },
      { reason: 'Patient transferred to another provider', category: 'Transfer' },
    ];

    // Type-specific reasons
    const typeSpecificReasons: ClosureReasonSuggestion[] = [];

    if (this.isLabRelatedGap(gapType) || measureName.includes('hba1c') || measureName.includes('diabetes')) {
      typeSpecificReasons.push(
        { reason: 'Lab ordered and pending results', category: 'Lab' },
        { reason: 'Lab completed - results in system', category: 'Lab' },
        { reason: 'Point-of-care test performed', category: 'Lab' }
      );
    }

    if (this.isScreeningGap(gapType) || measureName.includes('screening')) {
      typeSpecificReasons.push(
        { reason: 'Screening scheduled for future date', category: 'Screening' },
        { reason: 'Screening completed - documentation added', category: 'Screening' },
        { reason: 'Patient not eligible for screening', category: 'Exclusion' }
      );
    }

    if (this.isMedicationGap(gapType) || measureName.includes('statin') || measureName.includes('medication')) {
      typeSpecificReasons.push(
        { reason: 'Medication prescribed', category: 'Medication' },
        { reason: 'Patient on alternative therapy', category: 'Medication' },
        { reason: 'Contraindication documented', category: 'Exclusion' }
      );
    }

    if (measureName.includes('blood pressure') || measureName.includes('bp') || measureName.includes('hypertension')) {
      typeSpecificReasons.push(
        { reason: 'BP at goal on current therapy', category: 'Clinical' },
        { reason: 'Home BP monitoring initiated', category: 'Clinical' },
        { reason: 'Medication adjustment made', category: 'Medication' }
      );
    }

    if (measureName.includes('depression') || measureName.includes('phq')) {
      typeSpecificReasons.push(
        { reason: 'PHQ-9 screening completed', category: 'Screening' },
        { reason: 'Referred to behavioral health', category: 'Referral' },
        { reason: 'Treatment plan documented', category: 'Clinical' }
      );
    }

    this.closureReasonSuggestions = [...typeSpecificReasons, ...commonReasons];
  }

  /**
   * Initialize manual closure form
   */
  private initializeForm(): void {
    this.manualClosureForm = this.fb.group({
      reason: ['', Validators.required],
      notes: [''],
      scheduleFollowUp: [false],
      followUpDate: [''],
    });
  }

  /**
   * Handle quick action click
   */
  onQuickAction(action: QuickAction): void {
    if (action.type === 'manual-close') {
      this.showManualForm.set(true);
      this.selectedAction.set(action.type);
      return;
    }

    this.selectedAction.set(action.type);
    this.processQuickAction(action);
  }

  /**
   * Process quick action closure
   */
  private processQuickAction(action: QuickAction): void {
    this.isProcessing.set(true);

    const closureTimeMs = Date.now() - this.dialogOpenTime;
    const selectedGapIds = [this.data.gap.id, ...this.selectedRelatedGaps()];

    const closureRequest: CareGapClosureRequest = {
      reason: action.closureReason,
      notes: `Quick action: ${action.label}`,
      closedBy: 'current-user', // Would come from auth context
      closureDate: new Date().toISOString(),
    };

    if (action.interventionType) {
      closureRequest.intervention = {
        type: action.interventionType,
        description: action.description,
        scheduledDate: new Date().toISOString(),
      };
    }

    // Simulate processing - in production, would call service
    setTimeout(() => {
      const result: CareGapClosureResult = {
        action: action.type,
        gapId: this.data.gap.id,
        closedGapIds: selectedGapIds,
        closureRequest,
        intervention: closureRequest.intervention,
        closureTimeMs,
      };

      this.isProcessing.set(false);
      this.notificationService.success(`Care gap closed: ${action.label}`);
      this.dialogRef.close(result);
    }, 500);
  }

  /**
   * Submit manual closure
   */
  onManualClose(): void {
    if (this.manualClosureForm.invalid) {
      return;
    }

    this.isProcessing.set(true);

    const closureTimeMs = Date.now() - this.dialogOpenTime;
    const formValue = this.manualClosureForm.value;
    const selectedGapIds = [this.data.gap.id, ...this.selectedRelatedGaps()];

    const closureRequest: CareGapClosureRequest = {
      reason: formValue.reason,
      notes: formValue.notes,
      closedBy: 'current-user',
      closureDate: new Date().toISOString(),
    };

    if (formValue.scheduleFollowUp && formValue.followUpDate) {
      closureRequest.intervention = {
        type: InterventionType.APPOINTMENT_SCHEDULED,
        description: 'Follow-up scheduled',
        scheduledDate: formValue.followUpDate,
      };
    }

    // Simulate processing
    setTimeout(() => {
      const result: CareGapClosureResult = {
        action: 'manual-close',
        gapId: this.data.gap.id,
        closedGapIds: selectedGapIds,
        closureRequest,
        intervention: closureRequest.intervention,
        closureTimeMs,
      };

      this.isProcessing.set(false);
      this.notificationService.success('Care gap closed successfully');
      this.dialogRef.close(result);
    }, 500);
  }

  /**
   * Select a suggested closure reason
   */
  selectSuggestedReason(suggestion: ClosureReasonSuggestion): void {
    this.manualClosureForm.patchValue({ reason: suggestion.reason });
  }

  /**
   * Toggle related gap selection
   */
  toggleRelatedGap(gapId: string): void {
    const current = this.selectedRelatedGaps();
    if (current.includes(gapId)) {
      this.selectedRelatedGaps.set(current.filter(id => id !== gapId));
    } else {
      this.selectedRelatedGaps.set([...current, gapId]);
    }
  }

  /**
   * Select all related gaps
   */
  selectAllRelatedGaps(): void {
    if (this.data.relatedGaps) {
      this.selectedRelatedGaps.set(this.data.relatedGaps.map(g => g.id));
    }
  }

  /**
   * Deselect all related gaps
   */
  deselectAllRelatedGaps(): void {
    this.selectedRelatedGaps.set([]);
  }

  /**
   * Check if gap is related to lab work
   */
  private isLabRelatedGap(gapType: string): boolean {
    return gapType.includes('lab') ||
           gapType.includes('hba1c') ||
           gapType.includes('lipid') ||
           gapType.includes('kidney') ||
           gapType.includes('diabetes');
  }

  /**
   * Check if gap is a screening
   */
  private isScreeningGap(gapType: string): boolean {
    return gapType.includes('screening') ||
           gapType.includes('mammogram') ||
           gapType.includes('colonoscopy') ||
           gapType.includes('pap') ||
           gapType.includes('eye exam');
  }

  /**
   * Check if gap is medication related
   */
  private isMedicationGap(gapType: string): boolean {
    return gapType.includes('medication') ||
           gapType.includes('statin') ||
           gapType.includes('adherence') ||
           gapType.includes('prescription');
  }

  /**
   * Check if gap is for follow-up care
   */
  private isFollowUpGap(gapType: string): boolean {
    return gapType.includes('follow') ||
           gapType.includes('management') ||
           gapType.includes('control');
  }

  /**
   * Get priority badge color
   */
  getPriorityColor(priority: string): string {
    switch (priority) {
      case 'critical': return '#d32f2f';
      case 'high': return '#f57c00';
      case 'moderate': return '#fbc02d';
      default: return '#4caf50';
    }
  }

  /**
   * Back to quick actions from manual form
   */
  backToQuickActions(): void {
    this.showManualForm.set(false);
    this.selectedAction.set(null);
  }

  /**
   * Cancel and close dialog
   */
  onCancel(): void {
    this.dialogRef.close(null);
  }

  /**
   * Check if related gap is selected
   */
  isRelatedGapSelected(gapId: string): boolean {
    return this.selectedRelatedGaps().includes(gapId);
  }
}
