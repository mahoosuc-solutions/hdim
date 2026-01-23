import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
  FormsModule,
} from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogRef,
  MatDialogModule,
} from '@angular/material/dialog';
import { MatStepperModule, StepperOrientation } from '@angular/material/stepper';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatRadioModule } from '@angular/material/radio';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule } from '@angular/material/tabs';
import { MatListModule } from '@angular/material/list';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatButtonToggleModule } from '@angular/material/button-toggle';

import { Subject, Observable, BehaviorSubject } from 'rxjs';
import { takeUntil, debounceTime, switchMap, startWith } from 'rxjs/operators';

import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';
import { SDOHReferralService } from '../../services/sdoh-referral.service';
import { ToastService } from '../../services/toast.service';
import { LoggerService } from '../../services/logger.service';

import {
  SDOHReferralDialogData,
  SDOHReferralDialogResult,
  SDOHReferralRequest,
  ReferralDestination,
  ReferralDetailsForm,
  ReferralWorkflowStep,
  StaffMember,
  InternalReferralDestination,
  ReferralUrgency,
  ConsentStatus,
  INTERNAL_DESTINATION_NAMES,
  INTERNAL_DESTINATION_ICONS,
  REFERRAL_URGENCY_CONFIG,
  CONSENT_STATUS_CONFIG,
  getDefaultReferralDetails,
  isConsentObtained,
  getDestinationDisplayName,
  validateReferralRequest,
} from '../../models/sdoh-referral.model';
import {
  SDOHNeedWithDetails,
  SDOHCategory,
  CommunityResource,
} from '../../models/patient-health.model';

@Component({
  selector: 'app-sdoh-referral-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatDialogModule,
    MatStepperModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatCheckboxModule,
    MatRadioModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    MatTabsModule,
    MatListModule,
    MatAutocompleteModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDividerModule,
    MatButtonToggleModule,
    StatusBadgeComponent,
  ],
  templateUrl: './sdoh-referral-dialog.component.html',
  styleUrl: './sdoh-referral-dialog.component.scss',
})
export class SDOHReferralDialogComponent implements OnInit, OnDestroy {
  // Workflow state
  currentStep = ReferralWorkflowStep.SELECT_NEEDS;
  isLinear = true;

  // Step 1: Selected needs
  selectedNeeds: SDOHNeedWithDetails[] = [];

  // Step 2: Destination
  referralType: 'internal' | 'external' | null = null;
  selectedInternalType: InternalReferralDestination | null = null;
  selectedStaff: StaffMember | null = null;
  selectedCommunityResource: CommunityResource | null = null;
  availableStaff: StaffMember[] = [];
  searchResults: CommunityResource[] = [];
  searchQuery = '';
  searchSource: 'community' | '211' | 'findhelp' = 'community';
  searchLoading = false;

  // Step 3: Details form
  detailsForm!: FormGroup;

  // Loading states
  submitting = false;

  // Destroy subject
  private destroy$ = new Subject<void>();
  private searchSubject = new Subject<string>();
  private logger = this.loggerService.withContext('SDOHReferralDialogComponent');

  // Constants for template
  readonly INTERNAL_DESTINATION_NAMES = INTERNAL_DESTINATION_NAMES;
  readonly INTERNAL_DESTINATION_ICONS = INTERNAL_DESTINATION_ICONS;
  readonly REFERRAL_URGENCY_CONFIG = REFERRAL_URGENCY_CONFIG;
  readonly CONSENT_STATUS_CONFIG = CONSENT_STATUS_CONFIG;

  readonly internalTypes: InternalReferralDestination[] = [
    'care-coordinator',
    'social-worker',
    'behavioral-health',
    'case-manager',
    'community-health-worker',
    'patient-navigator',
  ];

  readonly urgencyOptions = Object.entries(REFERRAL_URGENCY_CONFIG).map(
    ([value, config]) => ({
      value: value as ReferralUrgency,
      ...config,
    })
  );

  readonly consentOptions = Object.entries(CONSENT_STATUS_CONFIG).map(
    ([value, config]) => ({
      value: value as ConsentStatus,
      ...config,
    })
  );

  constructor(
    private dialogRef: MatDialogRef<
      SDOHReferralDialogComponent,
      SDOHReferralDialogResult
    >,
    @Inject(MAT_DIALOG_DATA) public data: SDOHReferralDialogData,
    private fb: FormBuilder,
    private referralService: SDOHReferralService,
    private toastService: ToastService,
    private loggerService: LoggerService
  ) {
    this.initializeForm();
  }

  ngOnInit(): void {
    // Initialize with preselected needs if provided
    if (this.data.preselectedNeeds?.length) {
      this.selectedNeeds = [...this.data.preselectedNeeds];
    } else if (this.data.screeningResult?.needs?.length) {
      this.selectedNeeds = [...this.data.screeningResult.needs];
    }

    // Load available staff
    this.loadAvailableStaff();

    // Setup search debounce
    this.searchSubject
      .pipe(
        debounceTime(300),
        switchMap((query) => this.performSearch(query)),
        takeUntil(this.destroy$)
      )
      .subscribe((results) => {
        this.searchResults = results;
        this.searchLoading = false;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeForm(): void {
    const defaults = getDefaultReferralDetails();

    this.detailsForm = this.fb.group({
      urgency: [defaults.urgency, Validators.required],
      consentStatus: [defaults.consentStatus, Validators.required],
      consentDate: [null],
      clinicalNotes: [
        '',
        [Validators.required, Validators.minLength(10), Validators.maxLength(500)],
      ],
      relevantHistory: [''],
      preferredContactMethod: [defaults.preferredContactMethod],
      interpreterNeeded: [false],
      followUpDays: [defaults.followUpDays, [Validators.required, Validators.min(1), Validators.max(90)]],
      notifyOnStatusChange: [defaults.notifyOnStatusChange],
    });
  }

  private loadAvailableStaff(): void {
    this.referralService
      .getAvailableStaff()
      .pipe(takeUntil(this.destroy$))
      .subscribe((staff) => {
        this.availableStaff = staff;
      });
  }

  // ============================================
  // Step 1: Need Selection
  // ============================================

  toggleNeedSelection(need: SDOHNeedWithDetails): void {
    const index = this.selectedNeeds.findIndex((n) => n.zCode === need.zCode);
    if (index >= 0) {
      this.selectedNeeds.splice(index, 1);
    } else {
      this.selectedNeeds.push(need);
    }
  }

  isNeedSelected(need: SDOHNeedWithDetails): boolean {
    return this.selectedNeeds.some((n) => n.zCode === need.zCode);
  }

  selectAllNeeds(): void {
    if (this.data.screeningResult?.needs) {
      this.selectedNeeds = [...this.data.screeningResult.needs];
    }
  }

  clearNeedSelection(): void {
    this.selectedNeeds = [];
  }

  get step1Valid(): boolean {
    return this.selectedNeeds.length > 0;
  }

  // ============================================
  // Step 2: Destination Selection
  // ============================================

  onReferralTypeChange(type: 'internal' | 'external'): void {
    this.referralType = type;
    this.selectedInternalType = null;
    this.selectedStaff = null;
    this.selectedCommunityResource = null;
  }

  onInternalTypeChange(type: InternalReferralDestination): void {
    this.selectedInternalType = type;
    this.selectedStaff = null;

    // Load staff for this type
    this.referralService
      .getAvailableStaff(type)
      .pipe(takeUntil(this.destroy$))
      .subscribe((staff) => {
        this.availableStaff = staff;
      });
  }

  onStaffSelect(staff: StaffMember): void {
    this.selectedStaff = staff;
  }

  onResourceSelect(resource: CommunityResource): void {
    this.selectedCommunityResource = resource;
  }

  onSearchQueryChange(query: string): void {
    this.searchQuery = query;
    if (query.length >= 2) {
      this.searchLoading = true;
      this.searchSubject.next(query);
    } else {
      this.searchResults = [];
    }
  }

  private performSearch(query: string): Observable<CommunityResource[]> {
    const category = this.getPrimaryCategory();

    switch (this.searchSource) {
      case '211':
        return this.referralService.search211Resources(category, query).pipe(
          switchMap(() => this.referralService.searchCommunityResources(category, query))
        );
      case 'findhelp':
        return this.referralService.searchFindHelpResources(category, query).pipe(
          switchMap(() => this.referralService.searchCommunityResources(category, query))
        );
      default:
        return this.referralService.searchCommunityResources(category, query);
    }
  }

  private getPrimaryCategory(): SDOHCategory {
    if (this.selectedNeeds.length > 0) {
      return this.selectedNeeds[0].category;
    }
    return 'food-insecurity';
  }

  get step2Valid(): boolean {
    if (this.referralType === 'internal') {
      return this.selectedInternalType !== null;
    } else if (this.referralType === 'external') {
      return this.selectedCommunityResource !== null;
    }
    return false;
  }

  // ============================================
  // Step 3: Details
  // ============================================

  get step3Valid(): boolean {
    return this.detailsForm.valid;
  }

  get clinicalNotesLength(): number {
    return this.detailsForm.get('clinicalNotes')?.value?.length || 0;
  }

  get requiresConsentDate(): boolean {
    const status = this.detailsForm.get('consentStatus')?.value;
    return isConsentObtained(status);
  }

  // ============================================
  // Step 4: Review & Confirm
  // ============================================

  get destinationSummary(): string {
    const destination = this.buildDestination();
    return getDestinationDisplayName(destination);
  }

  get selectedNeedsSummary(): string {
    return this.selectedNeeds
      .map((n) => n.category.replace('-', ' '))
      .join(', ');
  }

  get hasWarnings(): boolean {
    const consent = this.detailsForm.get('consentStatus')?.value;
    return consent === 'pending' || consent === 'declined';
  }

  get warningMessage(): string {
    const consent = this.detailsForm.get('consentStatus')?.value;
    if (consent === 'pending') {
      return 'Patient consent is pending. Referral may be delayed.';
    }
    if (consent === 'declined') {
      return 'Patient has declined consent. Referral will be documented but not sent.';
    }
    return '';
  }

  goToStep(step: number): void {
    this.currentStep = step;
  }

  // ============================================
  // Submission
  // ============================================

  submit(): void {
    const request = this.buildRequest();
    const validation = validateReferralRequest(request);

    if (!validation.valid) {
      this.toastService.error(validation.errors.join('. '));
      return;
    }

    this.submitting = true;

    this.referralService
      .submitReferral(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (referral) => {
          this.toastService.success('Referral submitted successfully');
          this.dialogRef.close({
            action: 'submitted',
            referral,
          });
        },
        error: (error) => {
          this.submitting = false;
          this.toastService.error('Failed to submit referral. Please try again.');
          this.logger.error('Referral submission error', error);
        },
      });
  }

  saveDraft(): void {
    const request = this.buildRequest();

    this.referralService
      .saveDraft(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (referral) => {
          this.toastService.success('Draft saved');
          this.dialogRef.close({
            action: 'saved-draft',
            referral,
          });
        },
        error: (error) => {
          this.toastService.error('Failed to save draft');
          this.logger.error('Draft save error', error);
        },
      });
  }

  cancel(): void {
    this.dialogRef.close({ action: 'cancelled' });
  }

  private buildDestination(): ReferralDestination {
    if (this.referralType === 'internal') {
      return {
        type: 'internal',
        internalType: this.selectedInternalType!,
        assignedStaff: this.selectedStaff || undefined,
      };
    } else {
      return {
        type: 'external',
        externalSource: 'community-resource',
        communityResource: this.selectedCommunityResource!,
      };
    }
  }

  private buildRequest(): SDOHReferralRequest {
    const formValue = this.detailsForm.value;

    return {
      patientId: this.data.patientId,
      patientName: this.data.patientName,
      needs: this.selectedNeeds,
      destination: this.buildDestination(),
      details: {
        urgency: formValue.urgency,
        consentStatus: formValue.consentStatus,
        consentDate: formValue.consentDate,
        clinicalNotes: formValue.clinicalNotes,
        relevantHistory: formValue.relevantHistory,
        preferredContactMethod: formValue.preferredContactMethod,
        interpreterNeeded: formValue.interpreterNeeded,
        followUpDays: formValue.followUpDays,
        notifyOnStatusChange: formValue.notifyOnStatusChange,
      },
      screeningId: this.data.screeningResult?.screeningDate?.toString(),
      createdBy: 'current-user', // Would come from auth service
      createdAt: new Date(),
    };
  }

  // ============================================
  // Utility Methods
  // ============================================

  getSDOHCategoryIcon(category: SDOHCategory): string {
    const icons: Record<string, string> = {
      'food-insecurity': 'restaurant',
      'housing-instability': 'home',
      transportation: 'directions_car',
      'utility-assistance': 'power',
      'interpersonal-safety': 'security',
      education: 'school',
      employment: 'work',
      'social-isolation': 'people',
      'financial-strain': 'attach_money',
      food: 'restaurant',
      housing: 'home',
      financial: 'attach_money',
      social: 'people',
      safety: 'security',
    };
    return icons[category] || 'help';
  }

  formatCategory(category: string): string {
    return category.replace(/-/g, ' ');
  }

  getConsentStatusLabel(status: string | null | undefined): string {
    if (!status) return '';
    const config = CONSENT_STATUS_CONFIG[status as ConsentStatus];
    return config?.label || status;
  }
}
