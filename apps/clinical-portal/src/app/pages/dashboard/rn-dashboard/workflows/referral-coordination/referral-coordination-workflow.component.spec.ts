/**
 * Referral Coordination Workflow Component - Unit Tests
 *
 * Tests for managing specialist referrals, verifying insurance,
 * and tracking appointment status.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { ReferralCoordinationWorkflowComponent } from './referral-coordination-workflow.component';
import { NurseWorkflowService } from '../../../../services/nurse-workflow/nurse-workflow.service';
import { ToastService } from '../../../../services/toast.service';
import { LoggerService } from '../../../../services/logger.service';

describe('ReferralCoordinationWorkflowComponent', () => {
  let component: ReferralCoordinationWorkflowComponent;
  let fixture: ComponentFixture<ReferralCoordinationWorkflowComponent>;
  let nurseWorkflowService: jasmine.SpyObj<NurseWorkflowService>;
  let toastService: jasmine.SpyObj<ToastService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ReferralCoordinationWorkflowComponent>>;

  const mockDialogData = {
    referralId: 'REF_001',
    patientId: 'PATIENT001',
    patientName: 'Margaret Wilson',
    referralType: 'CARDIOLOGY',
  };

  const mockSpecialists = [
    {
      id: 'SPEC_001',
      name: 'Dr. Sarah Heart',
      specialty: 'CARDIOLOGY',
      npi: '1234567890',
      phone: '555-1234',
      acceptingPatients: true,
    },
    {
      id: 'SPEC_002',
      name: 'Dr. John Cardiac',
      specialty: 'CARDIOLOGY',
      npi: '0987654321',
      phone: '555-5678',
      acceptingPatients: true,
    },
  ];

  beforeEach(async () => {
    const nurseWorkflowSpy = jasmine.createSpyObj('NurseWorkflowService', [
      'getReferralById',
      'getSpecialistsForReferral',
      'verifyInsuranceCoverage',
      'sendReferral',
      'getReferralStatus',
      'completeReferralCoordination',
      'setTenantContext',
    ]);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error', 'warning']);
    const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);
    const loggerSpy = jasmine.createSpyObj('LoggerService', ['withContext']);
    loggerSpy.withContext.and.returnValue({
      log: jasmine.createSpy(),
      debug: jasmine.createSpy(),
      info: jasmine.createSpy(),
      warn: jasmine.createSpy(),
      error: jasmine.createSpy(),
    });

    await TestBed.configureTestingModule({
      declarations: [],
      imports: [ReactiveFormsModule],
      providers: [
        FormBuilder,
        { provide: NurseWorkflowService, useValue: nurseWorkflowSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: mockDialogData },
        { provide: LoggerService, useValue: loggerSpy },
      ],
    }).compileComponents();

    nurseWorkflowService = TestBed.inject(NurseWorkflowService) as jasmine.SpyObj<NurseWorkflowService>;
    toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    dialogRef = TestBed.inject(MatDialogRef) as jasmine.SpyObj<MatDialogRef<ReferralCoordinationWorkflowComponent>>;

    fixture = TestBed.createComponent(ReferralCoordinationWorkflowComponent);
    component = fixture.componentInstance;
  });

  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with dialog data', () => {
      expect(component.referralId).toBe('REF_001');
      expect(component.patientId).toBe('PATIENT001');
      expect(component.patientName).toBe('Margaret Wilson');
      expect(component.referralType).toBe('CARDIOLOGY');
    });

    it('should set current step to 0', () => {
      expect(component.currentStep).toBe(0);
      expect(component.totalSteps).toBeGreaterThan(0);
    });

    it('should load referral on initialization', () => {
      const mockReferral = { id: 'REF_001', patientId: 'PATIENT001', type: 'CARDIOLOGY' };
      nurseWorkflowService.getReferralById.and.returnValue(of(mockReferral));

      component.ngOnInit();

      expect(nurseWorkflowService.getReferralById).toHaveBeenCalledWith('REF_001');
    });
  });

  describe('Step 0: Review Referral', () => {
    beforeEach(() => {
      component.currentStep = 0;
    });

    it('should display referral details', () => {
      expect(component.referralType).toBe('CARDIOLOGY');
      expect(component.patientName).toBe('Margaret Wilson');
    });

    it('should show referral reason', () => {
      component.form.patchValue({ referralReason: 'Annual cardiac screening' });
      expect(component.form.get('referralReason').value).toContain('cardiac');
    });

    it('should enable next button when reviewed', () => {
      component.form.patchValue({ referralReviewed: true });
      expect(component.canProceedToNextStep()).toBe(true);
    });
  });

  describe('Step 1: Select Specialist', () => {
    beforeEach(() => {
      component.currentStep = 1;
      nurseWorkflowService.getSpecialistsForReferral.and.returnValue(of(mockSpecialists));
    });

    it('should load specialists for referral type', (done) => {
      component.loadSpecialists();

      setTimeout(() => {
        expect(component.availableSpecialists.length).toBe(2);
        done();
      }, 100);
    });

    it('should require specialist selection', () => {
      component.form.patchValue({ selectedSpecialist: '' });
      expect(component.canProceedToNextStep()).toBe(false);

      component.form.patchValue({ selectedSpecialist: 'SPEC_001' });
      expect(component.canProceedToNextStep()).toBe(true);
    });

    it('should display specialist details', () => {
      component.availableSpecialists = mockSpecialists;
      component.form.patchValue({ selectedSpecialist: 'SPEC_001' });
      component.onSpecialistSelected();

      expect(component.selectedSpecialistName).toBe('Dr. Sarah Heart');
    });

    it('should show only accepting specialists', () => {
      const specialist = mockSpecialists[0];
      specialist.acceptingPatients = true;
      component.availableSpecialists = [specialist];

      expect(component.availableSpecialists[0].acceptingPatients).toBe(true);
    });
  });

  describe('Step 2: Verify Insurance', () => {
    beforeEach(() => {
      component.currentStep = 2;
    });

    it('should check insurance coverage', (done) => {
      const mockCoverage = { covered: true, requiresPriorAuth: true };
      nurseWorkflowService.verifyInsuranceCoverage.and.returnValue(of(mockCoverage));

      component.verifyInsuranceCoverage();

      setTimeout(() => {
        expect(nurseWorkflowService.verifyInsuranceCoverage).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should warn if prior authorization required', (done) => {
      const mockCoverage = { covered: true, requiresPriorAuth: true };
      nurseWorkflowService.verifyInsuranceCoverage.and.returnValue(of(mockCoverage));

      component.verifyInsuranceCoverage();

      setTimeout(() => {
        expect(component.requiresPriorAuth).toBe(true);
        expect(toastService.warning).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should handle insurance not covered', (done) => {
      const mockCoverage = { covered: false, reason: 'Out of network' };
      nurseWorkflowService.verifyInsuranceCoverage.and.returnValue(of(mockCoverage));

      component.verifyInsuranceCoverage();

      setTimeout(() => {
        expect(toastService.warning).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should require acknowledgment if prior auth needed', () => {
      component.requiresPriorAuth = true;
      component.form.patchValue({ acknowledgeInsurance: false });

      expect(component.canProceedToNextStep()).toBe(false);

      component.form.patchValue({ acknowledgeInsurance: true });
      expect(component.canProceedToNextStep()).toBe(true);
    });
  });

  describe('Step 3: Send Referral', () => {
    beforeEach(() => {
      component.currentStep = 3;
    });

    it('should send referral request', (done) => {
      const mockResponse = { id: 'REF_001', status: 'sent' };
      nurseWorkflowService.sendReferral.and.returnValue(of(mockResponse));

      component.sendReferral();

      setTimeout(() => {
        expect(nurseWorkflowService.sendReferral).toHaveBeenCalled();
        expect(toastService.success).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should handle send referral errors', (done) => {
      nurseWorkflowService.sendReferral.and.returnValue(
        throwError(() => new Error('Failed to send'))
      );

      component.sendReferral();

      setTimeout(() => {
        expect(toastService.error).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should enable appointment tracking after sending', (done) => {
      const mockResponse = { id: 'REF_001', status: 'sent' };
      nurseWorkflowService.sendReferral.and.returnValue(of(mockResponse));

      component.sendReferral();

      setTimeout(() => {
        expect(component.referralSent).toBe(true);
        done();
      }, 100);
    });
  });

  describe('Step 4: Track Appointment Status', () => {
    beforeEach(() => {
      component.currentStep = 4;
      component.referralSent = true;
    });

    it('should retrieve appointment status', (done) => {
      const mockStatus = { status: 'SCHEDULED', appointmentDate: new Date() };
      nurseWorkflowService.getReferralStatus.and.returnValue(of(mockStatus));

      component.getReferralStatus();

      setTimeout(() => {
        expect(nurseWorkflowService.getReferralStatus).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should show appointment scheduled notification', (done) => {
      const mockStatus = { status: 'SCHEDULED', appointmentDate: new Date() };
      nurseWorkflowService.getReferralStatus.and.returnValue(of(mockStatus));

      component.getReferralStatus();

      setTimeout(() => {
        expect(component.appointmentScheduled).toBe(true);
        done();
      }, 100);
    });

    it('should allow recording post-visit notes', () => {
      component.appointmentScheduled = true;
      component.form.patchValue({ postVisitNotes: 'Patient stable, follow-up in 2 weeks' });

      expect(component.form.get('postVisitNotes').value).toContain('stable');
    });
  });

  describe('Workflow Submission', () => {
    it('should save referral coordination', (done) => {
      const mockResponse = { id: 'REF_001', status: 'completed' };
      nurseWorkflowService.completeReferralCoordination.and.returnValue(of(mockResponse));

      component.completeReferralWorkflow();

      setTimeout(() => {
        expect(nurseWorkflowService.completeReferralCoordination).toHaveBeenCalled();
        expect(toastService.success).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should close dialog on successful completion', (done) => {
      const mockResponse = { id: 'REF_001', status: 'completed' };
      nurseWorkflowService.completeReferralCoordination.and.returnValue(of(mockResponse));

      component.completeReferralWorkflow();

      setTimeout(() => {
        expect(dialogRef.close).toHaveBeenCalledWith({ success: true, result: mockResponse });
        done();
      }, 100);
    });

    it('should handle save errors', (done) => {
      nurseWorkflowService.completeReferralCoordination.and.returnValue(
        throwError(() => new Error('Save failed'))
      );

      component.completeReferralWorkflow();

      setTimeout(() => {
        expect(toastService.error).toHaveBeenCalled();
        done();
      }, 100);
    });
  });

  describe('Form Navigation', () => {
    it('should advance steps', () => {
      component.currentStep = 0;
      component.nextStep();

      expect(component.currentStep).toBeGreaterThan(0);
    });

    it('should go back to previous step', () => {
      component.currentStep = 2;
      component.previousStep();

      expect(component.currentStep).toBe(1);
    });

    it('should not go below step 0', () => {
      component.currentStep = 0;
      component.previousStep();

      expect(component.currentStep).toBe(0);
    });
  });

  describe('Cleanup', () => {
    it('should unsubscribe on destroy', () => {
      spyOn(component['destroy$'], 'next');
      spyOn(component['destroy$'], 'complete');

      component.ngOnDestroy();

      expect(component['destroy$'].next).toHaveBeenCalled();
      expect(component['destroy$'].complete).toHaveBeenCalled();
    });
  });
});
