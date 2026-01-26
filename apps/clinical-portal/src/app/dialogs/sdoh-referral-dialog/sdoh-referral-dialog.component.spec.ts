import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { SDOHReferralDialogComponent } from './sdoh-referral-dialog.component';
import { SDOHReferralService } from '../../services/sdoh-referral.service';
import { ToastService } from '../../services/toast.service';
import { createMockMatDialogRef } from '../../testing/mocks';
import {
  SDOHReferralDialogData,
  StaffMember,
  SDOHReferralDetail,
  ReferralWorkflowStep,
  InternalReferralDestination,
} from '../../models/sdoh-referral.model';
import {
  SDOHNeedWithDetails,
  CommunityResource,
  SDOHScreeningResult,
} from '../../models/patient-health.model';

describe('SDOHReferralDialogComponent', () => {
  let component: SDOHReferralDialogComponent;
  let fixture: ComponentFixture<SDOHReferralDialogComponent>;
  let dialogRef: jest.Mocked<MatDialogRef<SDOHReferralDialogComponent>>;
  let referralService: jest.Mocked<SDOHReferralService>;
  let toastService: jest.Mocked<ToastService>;

  const mockNeeds: SDOHNeedWithDetails[] = [
    {
      category: 'food-insecurity',
      severity: 'moderate',
      zCode: 'Z59.4',
      questionText: 'Within the past 12 months, have you worried about running out of food?',
      response: 'Yes',
    },
    {
      category: 'housing-instability',
      severity: 'severe',
      zCode: 'Z59.0',
      questionText: 'Are you currently experiencing housing instability?',
      response: 'Yes, homeless',
    },
  ];

  const mockScreeningResult: SDOHScreeningResult = {
    screeningDate: new Date('2024-01-15'),
    questionnaireType: 'PRAPARE',
    questionnaireName: 'PRAPARE',
    needs: mockNeeds,
    overallRisk: 'high',
    zCodes: ['Z59.4', 'Z59.0'],
  };

  const mockStaff: StaffMember[] = [
    {
      id: 'staff-1',
      name: 'Sarah Johnson',
      role: 'social-worker',
      email: 'sjohnson@clinic.com',
      department: 'Social Services',
      availableCapacity: 75,
    },
    {
      id: 'staff-2',
      name: 'Michael Chen',
      role: 'care-coordinator',
      email: 'mchen@clinic.com',
      department: 'Care Coordination',
      availableCapacity: 60,
    },
  ];

  const mockCommunityResources: CommunityResource[] = [
    {
      id: 'resource-1',
      name: 'Local Food Bank',
      category: 'food-insecurity',
      description: 'Provides emergency food assistance',
      address: '123 Main St',
      phone: '555-0100',
      website: 'www.foodbank.org',
      servicesOffered: ['Food pantry', 'Hot meals'],
      eligibilityRequirements: ['Must be resident of county'],
      operatingHours: 'Mon-Fri 9am-5pm',
    },
  ];

  const mockDialogData: SDOHReferralDialogData = {
    patientId: 'patient-123',
    patientName: 'John Doe',
    screeningResult: mockScreeningResult,
    mode: 'create',
  };

  const mockReferralDetail: SDOHReferralDetail = {
    id: 'referral-123',
    patientId: 'patient-123',
    patientName: 'John Doe',
    needs: mockNeeds,
    destination: {
      type: 'internal',
      internalType: 'social-worker',
    },
    urgency: 'routine',
    consentStatus: 'obtained-verbal',
    status: 'pending',
    statusHistory: [],
    createdAt: new Date(),
    createdBy: 'current-user',
  };

  beforeEach(async () => {
    const dialogRefSpy = {
      close: jest.fn(),
    } as unknown as MatDialogRef<SDOHReferralDialogComponent>;

    const referralServiceSpy = {
      getAvailableStaff: jest.fn().mockReturnValue(of(mockStaff)),
      searchCommunityResources: jest.fn().mockReturnValue(of(mockCommunityResources)),
      search211Resources: jest.fn().mockReturnValue(of([])),
      searchFindHelpResources: jest.fn().mockReturnValue(of([])),
      submitReferral: jest.fn().mockReturnValue(of(mockReferralDetail)),
      saveDraft: jest.fn().mockReturnValue(of(mockReferralDetail)),
    };

    const toastServiceSpy = {
      success: jest.fn(),
      error: jest.fn(),
      info: jest.fn(),
      warning: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [SDOHReferralDialogComponent, NoopAnimationsModule],
      providers: [{ provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: mockDialogData },
        { provide: SDOHReferralService, useValue: referralServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
        { provide: MatDialogRef, useValue: createMockMatDialogRef() }],
    }).compileComponents();

    dialogRef = TestBed.inject(MatDialogRef) as jest.Mocked<
      MatDialogRef<SDOHReferralDialogComponent>
    >;
    referralService = TestBed.inject(SDOHReferralService) as jest.Mocked<SDOHReferralService>;
    toastService = TestBed.inject(ToastService) as jest.Mocked<ToastService>;

    fixture = TestBed.createComponent(SDOHReferralDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ============================================
  // 1. Component Creation
  // ============================================

  describe('Component Creation', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with default workflow state', () => {
      expect(component.currentStep).toBe(ReferralWorkflowStep.SELECT_NEEDS);
      expect(component.isLinear).toBe(true);
      expect(component.submitting).toBe(false);
    });

    it('should initialize forms with validators', () => {
      expect(component.detailsForm).toBeDefined();
      expect(component.detailsForm.get('urgency')).toBeDefined();
      expect(component.detailsForm.get('consentStatus')).toBeDefined();
      expect(component.detailsForm.get('clinicalNotes')).toBeDefined();
    });
  });

  // ============================================
  // 2. Dialog Initialization with Data
  // ============================================

  describe('Dialog Initialization', () => {
    it('should initialize with preselected needs from screening result', () => {
      expect(component.selectedNeeds.length).toBe(2);
      expect(component.selectedNeeds).toEqual(mockNeeds);
    });

    it('should initialize with preselected needs when provided directly', () => {
      const customNeeds: SDOHNeedWithDetails[] = [
        {
          category: 'transportation',
          severity: 'mild',
          zCode: 'Z59.82',
          questionText: 'Do you have transportation issues?',
          response: 'Sometimes',
        },
      ];

      const customDialogData: SDOHReferralDialogData = {
        ...mockDialogData,
        preselectedNeeds: customNeeds,
      };

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [SDOHReferralDialogComponent, NoopAnimationsModule],
        providers: [
          { provide: MatDialogRef, useValue: dialogRef },
          { provide: MAT_DIALOG_DATA, useValue: customDialogData },
          { provide: SDOHReferralService, useValue: referralService },
          { provide: ToastService, useValue: toastService },
        ],
      });

      const customFixture = TestBed.createComponent(SDOHReferralDialogComponent);
      const customComponent = customFixture.componentInstance;
      customFixture.detectChanges();

      expect(customComponent.selectedNeeds).toEqual(customNeeds);
    });

    it('should load available staff on initialization', () => {
      expect(referralService.getAvailableStaff).toHaveBeenCalled();
      expect(component.availableStaff).toEqual(mockStaff);
    });

    it('should have empty selected needs when no data provided', () => {
      const emptyDialogData: SDOHReferralDialogData = {
        patientId: 'patient-456',
        patientName: 'Jane Smith',
        mode: 'create',
      };

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [SDOHReferralDialogComponent, NoopAnimationsModule],
        providers: [
          { provide: MatDialogRef, useValue: dialogRef },
          { provide: MAT_DIALOG_DATA, useValue: emptyDialogData },
          { provide: SDOHReferralService, useValue: referralService },
          { provide: ToastService, useValue: toastService },
        ],
      });

      const emptyFixture = TestBed.createComponent(SDOHReferralDialogComponent);
      const emptyComponent = emptyFixture.componentInstance;
      emptyFixture.detectChanges();

      expect(emptyComponent.selectedNeeds.length).toBe(0);
    });
  });

  // ============================================
  // 3. Step 1: Need Selection
  // ============================================

  describe('Step 1: Need Selection', () => {
    beforeEach(() => {
      component.selectedNeeds = [];
    });

    it('should toggle need selection', () => {
      const need = mockNeeds[0];

      component.toggleNeedSelection(need);
      expect(component.selectedNeeds.length).toBe(1);
      expect(component.isNeedSelected(need)).toBe(true);

      component.toggleNeedSelection(need);
      expect(component.selectedNeeds.length).toBe(0);
      expect(component.isNeedSelected(need)).toBe(false);
    });

    it('should select all needs', () => {
      component.selectAllNeeds();
      expect(component.selectedNeeds.length).toBe(2);
      expect(component.selectedNeeds).toEqual(mockNeeds);
    });

    it('should clear need selection', () => {
      component.selectedNeeds = [...mockNeeds];
      component.clearNeedSelection();
      expect(component.selectedNeeds.length).toBe(0);
    });

    it('should validate step 1 when needs are selected', () => {
      expect(component.step1Valid).toBe(false);

      component.selectedNeeds = [mockNeeds[0]];
      expect(component.step1Valid).toBe(true);
    });

    it('should identify selected needs correctly', () => {
      component.selectedNeeds = [mockNeeds[0]];
      expect(component.isNeedSelected(mockNeeds[0])).toBe(true);
      expect(component.isNeedSelected(mockNeeds[1])).toBe(false);
    });
  });

  // ============================================
  // 4. Step 2: Destination Selection
  // ============================================

  describe('Step 2: Destination Selection', () => {
    beforeEach(() => {
      component.selectedNeeds = [mockNeeds[0]];
    });

    describe('Internal Referral', () => {
      it('should set referral type to internal', () => {
        component.onReferralTypeChange('internal');
        expect(component.referralType).toBe('internal');
        expect(component.selectedCommunityResource).toBeNull();
      });

      it('should select internal type and load staff', () => {
        component.onInternalTypeChange('social-worker');
        expect(component.selectedInternalType).toBe('social-worker');
        expect(component.selectedStaff).toBeNull();
        expect(referralService.getAvailableStaff).toHaveBeenCalledWith('social-worker');
      });

      it('should select staff member', () => {
        const staff = mockStaff[0];
        component.onStaffSelect(staff);
        expect(component.selectedStaff).toEqual(staff);
      });

      it('should validate step 2 for internal referral with type selected', () => {
        component.referralType = 'internal';
        component.selectedInternalType = 'social-worker';
        expect(component.step2Valid).toBe(true);
      });

      it('should not validate step 2 for internal referral without type', () => {
        component.referralType = 'internal';
        component.selectedInternalType = null;
        expect(component.step2Valid).toBe(false);
      });

      it('should reset selections when changing referral type', () => {
        component.referralType = 'internal';
        component.selectedInternalType = 'social-worker';
        component.selectedStaff = mockStaff[0];

        component.onReferralTypeChange('external');

        expect(component.referralType).toBe('external');
        expect(component.selectedInternalType).toBeNull();
        expect(component.selectedStaff).toBeNull();
      });
    });

    describe('External Referral', () => {
      it('should set referral type to external', () => {
        component.onReferralTypeChange('external');
        expect(component.referralType).toBe('external');
        expect(component.selectedInternalType).toBeNull();
      });

      it('should select community resource', () => {
        const resource = mockCommunityResources[0];
        component.onResourceSelect(resource);
        expect(component.selectedCommunityResource).toEqual(resource);
      });

      it('should validate step 2 for external referral with resource selected', () => {
        component.referralType = 'external';
        component.selectedCommunityResource = mockCommunityResources[0];
        expect(component.step2Valid).toBe(true);
      });

      it('should not validate step 2 for external referral without resource', () => {
        component.referralType = 'external';
        component.selectedCommunityResource = null;
        expect(component.step2Valid).toBe(false);
      });
    });

    it('should not validate step 2 when no referral type is selected', () => {
      component.referralType = null;
      expect(component.step2Valid).toBe(false);
    });
  });

  // ============================================
  // 5. Step 3: Details Form Validation
  // ============================================

  describe('Step 3: Details Form Validation', () => {
    it('should initialize form with default values', () => {
      expect(component.detailsForm.get('urgency')?.value).toBe('routine');
      expect(component.detailsForm.get('consentStatus')?.value).toBe('pending');
      expect(component.detailsForm.get('followUpDays')?.value).toBe(14);
      expect(component.detailsForm.get('notifyOnStatusChange')?.value).toBe(true);
    });

    it('should require clinical notes', () => {
      const clinicalNotesControl = component.detailsForm.get('clinicalNotes');
      expect(clinicalNotesControl?.hasError('required')).toBe(true);

      clinicalNotesControl?.setValue('Short');
      expect(clinicalNotesControl?.hasError('minlength')).toBe(true);

      clinicalNotesControl?.setValue('This is a valid clinical note with sufficient length');
      expect(clinicalNotesControl?.valid).toBe(true);
    });

    it('should enforce max length on clinical notes', () => {
      const clinicalNotesControl = component.detailsForm.get('clinicalNotes');
      const longText = 'a'.repeat(501);
      clinicalNotesControl?.setValue(longText);
      expect(clinicalNotesControl?.hasError('maxlength')).toBe(true);
    });

    it('should require urgency', () => {
      const urgencyControl = component.detailsForm.get('urgency');
      urgencyControl?.setValue(null);
      expect(urgencyControl?.hasError('required')).toBe(true);
    });

    it('should require consent status', () => {
      const consentControl = component.detailsForm.get('consentStatus');
      consentControl?.setValue(null);
      expect(consentControl?.hasError('required')).toBe(true);
    });

    it('should validate follow-up days range', () => {
      const followUpControl = component.detailsForm.get('followUpDays');

      followUpControl?.setValue(0);
      expect(followUpControl?.hasError('min')).toBe(true);

      followUpControl?.setValue(91);
      expect(followUpControl?.hasError('max')).toBe(true);

      followUpControl?.setValue(30);
      expect(followUpControl?.valid).toBe(true);
    });

    it('should track clinical notes length', () => {
      const notes = 'Patient experiencing food insecurity';
      component.detailsForm.get('clinicalNotes')?.setValue(notes);
      expect(component.clinicalNotesLength).toBe(notes.length);
    });

    it('should determine if consent date is required', () => {
      component.detailsForm.get('consentStatus')?.setValue('obtained-verbal');
      expect(component.requiresConsentDate).toBe(true);

      component.detailsForm.get('consentStatus')?.setValue('pending');
      expect(component.requiresConsentDate).toBe(false);

      component.detailsForm.get('consentStatus')?.setValue('declined');
      expect(component.requiresConsentDate).toBe(false);
    });

    it('should validate step 3 when form is valid', () => {
      component.detailsForm.patchValue({
        urgency: 'urgent',
        consentStatus: 'obtained-verbal',
        clinicalNotes: 'Patient needs urgent food assistance due to recent job loss',
        followUpDays: 7,
      });

      expect(component.step3Valid).toBe(true);
    });

    it('should not validate step 3 when form is invalid', () => {
      component.detailsForm.patchValue({
        urgency: 'urgent',
        consentStatus: 'obtained-verbal',
        clinicalNotes: 'Short', // Too short
        followUpDays: 7,
      });

      expect(component.step3Valid).toBe(false);
    });
  });

  // ============================================
  // 6. Step 4: Review and Confirmation
  // ============================================

  describe('Step 4: Review and Confirmation', () => {
    beforeEach(() => {
      component.selectedNeeds = [mockNeeds[0]];
      component.detailsForm.patchValue({
        urgency: 'urgent',
        consentStatus: 'obtained-verbal',
        clinicalNotes: 'Patient needs urgent food assistance',
        followUpDays: 7,
      });
    });

    it('should generate destination summary for internal referral', () => {
      component.referralType = 'internal';
      component.selectedInternalType = 'social-worker';
      component.selectedStaff = mockStaff[0];

      expect(component.destinationSummary).toBe('Sarah Johnson');
    });

    it('should generate destination summary for internal referral without staff', () => {
      component.referralType = 'internal';
      component.selectedInternalType = 'social-worker';
      component.selectedStaff = null;

      expect(component.destinationSummary).toBe('Social Worker');
    });

    it('should generate destination summary for external referral', () => {
      component.referralType = 'external';
      component.selectedCommunityResource = mockCommunityResources[0];

      expect(component.destinationSummary).toBe('Local Food Bank');
    });

    it('should generate selected needs summary', () => {
      component.selectedNeeds = mockNeeds;
      expect(component.selectedNeedsSummary).toContain('food insecurity');
      expect(component.selectedNeedsSummary).toContain('housing instability');
    });

    it('should detect warnings for pending consent', () => {
      component.detailsForm.get('consentStatus')?.setValue('pending');
      expect(component.hasWarnings).toBe(true);
      expect(component.warningMessage).toContain('pending');
    });

    it('should detect warnings for declined consent', () => {
      component.detailsForm.get('consentStatus')?.setValue('declined');
      expect(component.hasWarnings).toBe(true);
      expect(component.warningMessage).toContain('declined');
    });

    it('should not have warnings for obtained consent', () => {
      component.detailsForm.get('consentStatus')?.setValue('obtained-verbal');
      expect(component.hasWarnings).toBe(false);
      expect(component.warningMessage).toBe('');
    });
  });

  // ============================================
  // 7. Step Navigation
  // ============================================

  describe('Step Navigation', () => {
    it('should navigate to specific step', () => {
      component.goToStep(ReferralWorkflowStep.CHOOSE_DESTINATION);
      expect(component.currentStep).toBe(ReferralWorkflowStep.CHOOSE_DESTINATION);

      component.goToStep(ReferralWorkflowStep.REVIEW_CONFIRM);
      expect(component.currentStep).toBe(ReferralWorkflowStep.REVIEW_CONFIRM);
    });

    it('should maintain step validation states', () => {
      // Step 1 invalid without needs
      component.selectedNeeds = [];
      expect(component.step1Valid).toBe(false);

      // Step 1 valid with needs
      component.selectedNeeds = [mockNeeds[0]];
      expect(component.step1Valid).toBe(true);

      // Step 2 invalid without destination
      expect(component.step2Valid).toBe(false);

      // Step 2 valid with destination
      component.referralType = 'internal';
      component.selectedInternalType = 'social-worker';
      expect(component.step2Valid).toBe(true);
    });
  });

  // ============================================
  // 8. Form Submission
  // ============================================

  describe('Form Submission', () => {
    beforeEach(() => {
      component.selectedNeeds = [mockNeeds[0]];
      component.referralType = 'internal';
      component.selectedInternalType = 'social-worker';
      component.selectedStaff = mockStaff[0];
      component.detailsForm.patchValue({
        urgency: 'urgent',
        consentStatus: 'obtained-verbal',
        clinicalNotes: 'Patient needs urgent food assistance due to recent job loss',
        followUpDays: 7,
      });
    });

    it('should submit valid referral request', () => {
      component.submit();

      expect(component.submitting).toBe(true);
      expect(referralService.submitReferral).toHaveBeenCalled();

      const submittedRequest = (referralService.submitReferral as jest.Mock).mock.calls[0][0];
      expect(submittedRequest.patientId).toBe('patient-123');
      expect(submittedRequest.needs).toEqual([mockNeeds[0]]);
      expect(submittedRequest.destination.type).toBe('internal');
      expect(submittedRequest.details.urgency).toBe('urgent');
    });

    it('should close dialog on successful submission', fakeAsync(() => {
      component.submit();
      tick();

      expect(toastService.success).toHaveBeenCalledWith('Referral submitted successfully');
      expect(dialogRef.close).toHaveBeenCalledWith({
        action: 'submitted',
        referral: mockReferralDetail,
      });
    }));

    it('should handle submission error', fakeAsync(() => {
      referralService.submitReferral.mockReturnValue(
        throwError(() => new Error('Network error'))
      );

      component.submit();
      tick();

      expect(toastService.error).toHaveBeenCalledWith(
        'Failed to submit referral. Please try again.'
      );
      expect(component.submitting).toBe(false);
      expect(dialogRef.close).not.toHaveBeenCalled();
    }));

    it('should not submit invalid request', () => {
      component.selectedNeeds = []; // Invalid - no needs selected

      component.submit();

      expect(toastService.error).toHaveBeenCalled();
      expect(referralService.submitReferral).not.toHaveBeenCalled();
    });

    it('should build internal referral request correctly', () => {
      component.submit();

      const submittedRequest = (referralService.submitReferral as jest.Mock).mock.calls[0][0];
      expect(submittedRequest.destination).toEqual({
        type: 'internal',
        internalType: 'social-worker',
        assignedStaff: mockStaff[0],
      });
    });

    it('should build external referral request correctly', () => {
      component.referralType = 'external';
      component.selectedCommunityResource = mockCommunityResources[0];

      component.submit();

      const submittedRequest = (referralService.submitReferral as jest.Mock).mock.calls[0][0];
      expect(submittedRequest.destination).toEqual({
        type: 'external',
        externalSource: 'community-resource',
        communityResource: mockCommunityResources[0],
      });
    });
  });

  // ============================================
  // 9. Save Draft
  // ============================================

  describe('Save Draft', () => {
    beforeEach(() => {
      component.selectedNeeds = [mockNeeds[0]];
      component.referralType = 'internal';
      component.selectedInternalType = 'social-worker';
    });

    it('should save draft successfully', fakeAsync(() => {
      component.saveDraft();
      tick();

      expect(referralService.saveDraft).toHaveBeenCalled();
      expect(toastService.success).toHaveBeenCalledWith('Draft saved');
      expect(dialogRef.close).toHaveBeenCalledWith({
        action: 'saved-draft',
        referral: mockReferralDetail,
      });
    }));

    it('should handle draft save error', fakeAsync(() => {
      referralService.saveDraft.mockReturnValue(
        throwError(() => new Error('Save failed'))
      );

      component.saveDraft();
      tick();

      expect(toastService.error).toHaveBeenCalledWith('Failed to save draft');
      expect(dialogRef.close).not.toHaveBeenCalled();
    }));
  });

  // ============================================
  // 10. Cancel Functionality
  // ============================================

  describe('Cancel Functionality', () => {
    it('should close dialog with cancelled action', () => {
      component.cancel();
      expect(dialogRef.close).toHaveBeenCalledWith({ action: 'cancelled' });
    });
  });

  // ============================================
  // 11. Search Functionality for External Resources
  // ============================================

  describe('Search Functionality', () => {
    beforeEach(() => {
      component.selectedNeeds = [mockNeeds[0]];
      component.referralType = 'external';
    });

    it('should trigger search when query length is >= 2', fakeAsync(() => {
      component.onSearchQueryChange('food');
      tick(300); // Wait for debounce

      expect(component.searchLoading).toBe(false);
      expect(referralService.searchCommunityResources).toHaveBeenCalled();
    }));

    it('should not trigger search for short queries', () => {
      component.onSearchQueryChange('f');
      expect(component.searchResults).toEqual([]);
    });

    it('should set loading state during search', () => {
      component.onSearchQueryChange('food');
      expect(component.searchLoading).toBe(true);
    });

    it('should search community resources by default', fakeAsync(() => {
      component.searchSource = 'community';
      component.onSearchQueryChange('food');
      tick(300);

      expect(referralService.searchCommunityResources).toHaveBeenCalled();
      expect(component.searchResults).toEqual(mockCommunityResources);
    }));

    it('should search 211 resources when source is 211', fakeAsync(() => {
      component.searchSource = '211';
      component.onSearchQueryChange('food');
      tick(300);

      expect(referralService.search211Resources).toHaveBeenCalled();
      expect(referralService.searchCommunityResources).toHaveBeenCalled();
    }));

    it('should search findhelp resources when source is findhelp', fakeAsync(() => {
      component.searchSource = 'findhelp';
      component.onSearchQueryChange('food');
      tick(300);

      expect(referralService.searchFindHelpResources).toHaveBeenCalled();
      expect(referralService.searchCommunityResources).toHaveBeenCalled();
    }));

    it('should debounce search queries', fakeAsync(() => {
      component.onSearchQueryChange('fo');
      tick(100);
      component.onSearchQueryChange('foo');
      tick(100);
      component.onSearchQueryChange('food');
      tick(300);

      // Should only call once after debounce completes
      expect(referralService.searchCommunityResources).toHaveBeenCalledTimes(1);
    }));

    it('should clear search results for empty query', () => {
      component.searchResults = mockCommunityResources;
      component.onSearchQueryChange('');
      expect(component.searchResults).toEqual([]);
    });
  });

  // ============================================
  // 12. Utility Methods
  // ============================================

  describe('Utility Methods', () => {
    it('should get SDOH category icon', () => {
      expect(component.getSDOHCategoryIcon('food-insecurity')).toBe('restaurant');
      expect(component.getSDOHCategoryIcon('housing-instability')).toBe('home');
      expect(component.getSDOHCategoryIcon('transportation')).toBe('directions_car');
    });

    it('should format category names', () => {
      expect(component.formatCategory('food-insecurity')).toBe('food insecurity');
      expect(component.formatCategory('housing-instability')).toBe('housing instability');
    });
  });

  // ============================================
  // 13. Component Cleanup
  // ============================================

  describe('Component Cleanup', () => {
    it('should unsubscribe on destroy', () => {
      const destroySpy = jest.spyOn(component['destroy$'], 'next');
      const completeSpy = jest.spyOn(component['destroy$'], 'complete');

      component.ngOnDestroy();

      expect(destroySpy).toHaveBeenCalled();
      expect(completeSpy).toHaveBeenCalled();
    });
  });

  // ============================================
  // 14. Edge Cases
  // ============================================

  describe('Edge Cases', () => {
    it('should handle missing screening result gracefully', () => {
      const dataWithoutScreening: SDOHReferralDialogData = {
        patientId: 'patient-789',
        patientName: 'Test Patient',
        mode: 'create',
      };

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [SDOHReferralDialogComponent, NoopAnimationsModule],
        providers: [
          { provide: MatDialogRef, useValue: dialogRef },
          { provide: MAT_DIALOG_DATA, useValue: dataWithoutScreening },
          { provide: SDOHReferralService, useValue: referralService },
          { provide: ToastService, useValue: toastService },
        ],
      });

      const testFixture = TestBed.createComponent(SDOHReferralDialogComponent);
      const testComponent = testFixture.componentInstance;
      testFixture.detectChanges();

      expect(testComponent.selectedNeeds.length).toBe(0);
      expect(testComponent).toBeTruthy();
    });

    it('should handle internal referral without assigned staff', () => {
      component.selectedNeeds = [mockNeeds[0]];
      component.referralType = 'internal';
      component.selectedInternalType = 'social-worker';
      component.selectedStaff = null;
      component.detailsForm.patchValue({
        urgency: 'routine',
        consentStatus: 'obtained-verbal',
        clinicalNotes: 'Patient needs assistance with food resources',
        followUpDays: 14,
      });

      component.submit();

      const submittedRequest = (referralService.submitReferral as jest.Mock).mock.calls[0][0];
      expect(submittedRequest.destination.assignedStaff).toBeUndefined();
    });

    it('should use primary category for search', () => {
      component.selectedNeeds = [mockNeeds[0], mockNeeds[1]];
      component.referralType = 'external';
      component.searchSource = 'community';

      component.onSearchQueryChange('test');

      expect(component['getPrimaryCategory']()).toBe('food-insecurity');
    });

    it('should default to food-insecurity category when no needs selected', () => {
      component.selectedNeeds = [];
      expect(component['getPrimaryCategory']()).toBe('food-insecurity');
    });

    it('should handle changing internal type and reload staff', () => {
      component.onInternalTypeChange('care-coordinator');
      expect(referralService.getAvailableStaff).toHaveBeenCalledWith('care-coordinator');
      expect(component.selectedStaff).toBeNull();
    });
  });

  // ============================================
  // 15. Constants and Configuration
  // ============================================

  describe('Constants and Configuration', () => {
    it('should have internal types defined', () => {
      expect(component.internalTypes).toContain('social-worker');
      expect(component.internalTypes).toContain('care-coordinator');
      expect(component.internalTypes).toContain('behavioral-health');
      expect(component.internalTypes.length).toBe(6);
    });

    it('should have urgency options defined', () => {
      expect(component.urgencyOptions.length).toBe(4);
      expect(component.urgencyOptions.some(o => o.value === 'emergent')).toBe(true);
      expect(component.urgencyOptions.some(o => o.value === 'urgent')).toBe(true);
      expect(component.urgencyOptions.some(o => o.value === 'soon')).toBe(true);
      expect(component.urgencyOptions.some(o => o.value === 'routine')).toBe(true);
    });

    it('should have consent options defined', () => {
      expect(component.consentOptions.length).toBe(5);
      expect(component.consentOptions.some(o => o.value === 'obtained-verbal')).toBe(true);
      expect(component.consentOptions.some(o => o.value === 'obtained-written')).toBe(true);
      expect(component.consentOptions.some(o => o.value === 'pending')).toBe(true);
    });
  });
});
