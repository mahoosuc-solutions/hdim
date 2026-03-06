/**
 * Patient Outreach Workflow Component - Unit Tests
 *
 * Tests for managing patient contact, logging interactions,
 * and scheduling follow-ups.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { PatientOutreachWorkflowComponent } from './patient-outreach-workflow.component';
import { NurseWorkflowService } from '../../../../../services/nurse-workflow/nurse-workflow.service';
import { ToastService } from '../../../../../services/toast.service';
import { LoggerService } from '../../../../../services/logger.service';
import { AuthService } from '../../../../../services/auth.service';
import { createMockMatDialogRef } from '../../testing/mocks';

describe('PatientOutreachWorkflowComponent', () => {
  let component: PatientOutreachWorkflowComponent;
  let fixture: ComponentFixture<PatientOutreachWorkflowComponent>;
  let nurseWorkflowService: any;
  let toastService: any;
  let dialogRef: any;

  const mockDialogData = {
    outreachLogId: 'OUTREACH001',
    patientId: 'PATIENT001',
    patientName: 'John Smith',
  };

  beforeEach(async () => {
    const nurseWorkflowSpy = {
      getOutreachLogById: jest.fn(),
      updateOutreachLog: jest.fn(),
      logContactAttempt: jest.fn(),
      setTenantContext: jest.fn(),
    };
    const toastSpy = { success: jest.fn(), error: jest.fn(), info: jest.fn() };
    const dialogRefSpy = { close: jest.fn() };
    const loggerSpy = {
      withContext: jest.fn(),
      log: jest.fn(),
      debug: jest.fn(),
      info: jest.fn(),
      warn: jest.fn(),
      error: jest.fn(),
    };
    loggerSpy.withContext.mockReturnValue({
      log: jest.fn(),
      debug: jest.fn(),
      info: jest.fn(),
      warn: jest.fn(),
      error: jest.fn(),
    });

    await TestBed.configureTestingModule({
      declarations: [],
      imports: [ReactiveFormsModule],
      providers: [FormBuilder,
        { provide: NurseWorkflowService, useValue: nurseWorkflowSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: mockDialogData },
        { provide: LoggerService, useValue: loggerSpy },
        { provide: AuthService, useValue: { getTenantId: jest.fn().mockReturnValue('test-tenant') } },
        { provide: MatDialogRef, useValue: createMockMatDialogRef() }],
    }).compileComponents();

    nurseWorkflowService = TestBed.inject(NurseWorkflowService) as any;
    toastService = TestBed.inject(ToastService) as any;
    dialogRef = TestBed.inject(MatDialogRef) as any;

    // Default mock return for outreach log loading
    nurseWorkflowService.getOutreachLogById.mockReturnValue(of({
      id: 'OUTREACH001',
      patientId: 'PATIENT001',
      contactMethod: 'CALL',
      reason: 'Follow-up',
    }));
    nurseWorkflowService.updateOutreachLog.mockReturnValue(of({ id: 'OUTREACH001', status: 'completed' }));
    nurseWorkflowService.logContactAttempt.mockReturnValue(of({ id: 'ATTEMPT001', status: 'logged' }));

    fixture = TestBed.createComponent(PatientOutreachWorkflowComponent);
    component = fixture.componentInstance;
  });

  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with dialog data', () => {
      expect(component.outreachLogId).toBe('OUTREACH001');
      expect(component.patientId).toBe('PATIENT001');
      expect(component.patientName).toBe('John Smith');
    });

    it('should set current step to 0 (contact method selection)', () => {
      expect(component.currentStep).toBe(0);
      expect(component.totalSteps).toBeGreaterThan(0);
    });

    it('should initialize form with required fields', () => {
      expect(component.form).toBeDefined();
      expect(component.form.get('contactMethod')).toBeDefined();
      expect(component.form.get('notes')).toBeDefined();
    });

    it('should load outreach log on initialization', () => {
      const mockOutreachLog = {
        id: 'OUTREACH001',
        patientId: 'PATIENT001',
        contactMethod: 'CALL',
        reason: 'Follow-up',
      };
      nurseWorkflowService.getOutreachLogById.mockReturnValue(of(mockOutreachLog));

      component.ngOnInit();

      expect(nurseWorkflowService.getOutreachLogById).toHaveBeenCalledWith('OUTREACH001');
    });
  });

  describe('Contact Method Selection (Step 0)', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should display contact method options', () => {
      expect(component.contactMethods).toEqual(['CALL', 'EMAIL', 'LETTER']);
    });

    it('should validate contact method selection', () => {
      component.form.patchValue({ contactMethod: '' });
      expect(component.form.valid).toBe(false);

      component.form.patchValue({ contactMethod: 'CALL' });
      expect(component.form.valid).toBe(true);
    });

    it('should disable next button if contact method not selected', () => {
      component.form.patchValue({ contactMethod: '' });
      expect(component.canProceedToNextStep()).toBe(false);
    });

    it('should enable next button when contact method selected', () => {
      component.form.patchValue({ contactMethod: 'CALL' });
      expect(component.canProceedToNextStep()).toBe(true);
    });

    it('should advance to step 1 on next', () => {
      component.form.patchValue({ contactMethod: 'CALL' });
      component.nextStep();

      expect(component.currentStep).toBe(1);
    });
  });

  describe('Contact Attempt Logging (Step 1)', () => {
    beforeEach(() => {
      component.ngOnInit();
      component.form.patchValue({ contactMethod: 'CALL' });
      component.nextStep();
    });

    it('should initialize contact duration field for calls', () => {
      component.form.patchValue({ contactMethod: 'CALL' });
      expect(component.form.get('contactDuration')).toBeDefined();
    });

    it('should validate contact duration for calls', () => {
      component.form.patchValue({ contactDuration: 15 });
      expect(component.form.get('contactDuration').value).toBe(15);
    });

    it('should log contact attempt', (done) => {
      const mockResponse = { id: 'ATTEMPT001', status: 'logged' };
      nurseWorkflowService.logContactAttempt.mockReturnValue(of(mockResponse));

      component.logContactAttempt();

      setTimeout(() => {
        expect(nurseWorkflowService.logContactAttempt).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should show error toast on failed contact attempt', (done) => {
      nurseWorkflowService.logContactAttempt.mockReturnValue(
        throwError(() => new Error('Failed to log'))
      );

      component.logContactAttempt();

      setTimeout(() => {
        expect(toastService.error).toHaveBeenCalled();
        done();
      }, 100);
    });
  });

  describe('Outcome Type Selection (Step 2)', () => {
    beforeEach(() => {
      component.ngOnInit();
      component.currentStep = 2;
    });

    it('should display outcome type options', () => {
      expect(component.outcomeTypes).toContain('SUCCESSFUL');
      expect(component.outcomeTypes).toContain('BUSY');
      expect(component.outcomeTypes).toContain('VOICEMAIL');
      expect(component.outcomeTypes).toContain('DISCONNECTED');
    });

    it('should validate outcome type selection', () => {
      component.form.patchValue({ outcomeType: '' });
      expect(component.form.valid).toBe(false);

      component.form.patchValue({ outcomeType: 'SUCCESSFUL' });
      expect(component.form.valid).toBe(true);
    });

    it('should show additional notes field for unsuccessful outcomes', () => {
      component.form.patchValue({ outcomeType: 'BUSY' });
      expect(component.shouldShowRetryInfo()).toBe(true);
    });

    it('should not require retry info for successful outcome', () => {
      component.form.patchValue({ outcomeType: 'SUCCESSFUL' });
      expect(component.shouldShowRetryInfo()).toBe(false);
    });
  });

  describe('Follow-up Scheduling (Step 3)', () => {
    beforeEach(() => {
      component.ngOnInit();
      component.currentStep = 3;
    });

    it('should initialize follow-up form fields', () => {
      expect(component.form.get('scheduleFollowUp')).toBeDefined();
      expect(component.form.get('followUpDate')).toBeDefined();
      expect(component.form.get('followUpReason')).toBeDefined();
    });

    it('should have follow-up scheduling fields', () => {
      component.form.patchValue({ scheduleFollowUp: true });
      expect(component.form.get('scheduleFollowUp').value).toBe(true);
    });

    it('should accept follow-up date', () => {
      const futureDate = new Date();
      futureDate.setDate(futureDate.getDate() + 1);
      component.form.patchValue({ followUpDate: futureDate });
      expect(component.form.get('followUpDate').value).toEqual(futureDate);
    });

    it('should accept follow-up reason', () => {
      component.form.patchValue({ followUpReason: 'Recheck medication adherence' });
      expect(component.form.get('followUpReason').value).toBe('Recheck medication adherence');
    });
  });

  describe('Review & Confirmation (Step 4)', () => {
    beforeEach(() => {
      component.ngOnInit();
      component.currentStep = 4;
    });

    it('should display summary of all entered data', () => {
      const summary = component.getWorkflowSummary();
      expect(summary).toBeDefined();
    });

    it('should show edit buttons for each section', () => {
      expect(component.canEditStep(0)).toBe(true);
      expect(component.canEditStep(1)).toBe(true);
      expect(component.canEditStep(2)).toBe(true);
      expect(component.canEditStep(3)).toBe(true);
    });

    it('should allow editing previous steps', () => {
      component.editStep(1);
      expect(component.currentStep).toBe(1);
    });
  });

  describe('Workflow Submission', () => {
    beforeEach(() => {
      component.ngOnInit();
      component.currentStep = 4;
    });

    it('should save completed workflow', (done) => {
      const mockResponse = { id: 'OUTREACH001', status: 'completed' };
      nurseWorkflowService.updateOutreachLog.mockReturnValue(of(mockResponse));

      component.completeWorkflow();

      setTimeout(() => {
        expect(nurseWorkflowService.updateOutreachLog).toHaveBeenCalled();
        expect(toastService.success).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should close dialog on successful completion', (done) => {
      const mockResponse = { id: 'OUTREACH001', status: 'completed' };
      nurseWorkflowService.updateOutreachLog.mockReturnValue(of(mockResponse));

      component.completeWorkflow();

      setTimeout(() => {
        expect(dialogRef.close).toHaveBeenCalledWith({ success: true, result: mockResponse });
        done();
      }, 100);
    });

    it('should handle save errors gracefully', (done) => {
      nurseWorkflowService.updateOutreachLog.mockReturnValue(
        throwError(() => new Error('Save failed'))
      );

      component.completeWorkflow();

      setTimeout(() => {
        expect(toastService.error).toHaveBeenCalled();
        expect(dialogRef.close).not.toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should emit workflowComplete event', (done) => {
      jest.spyOn(component.workflowComplete, 'emit');
      const mockResponse = { id: 'OUTREACH001', status: 'completed' };
      nurseWorkflowService.updateOutreachLog.mockReturnValue(of(mockResponse));

      component.completeWorkflow();

      setTimeout(() => {
        expect(component.workflowComplete.emit).toHaveBeenCalled();
        done();
      }, 100);
    });
  });

  describe('Form Navigation', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should go to next step', () => {
      component.form.patchValue({ contactMethod: 'CALL' });
      component.nextStep();

      expect(component.currentStep).toBe(1);
    });

    it('should go to previous step', () => {
      component.currentStep = 1;
      component.previousStep();

      expect(component.currentStep).toBe(0);
    });

    it('should not go below step 0', () => {
      component.currentStep = 0;
      component.previousStep();

      expect(component.currentStep).toBe(0);
    });

    it('should not go beyond final step', () => {
      component.currentStep = component.totalSteps - 1;
      component.nextStep();

      expect(component.currentStep).toBe(component.totalSteps - 1);
    });

    it('should disable previous button on step 0', () => {
      component.currentStep = 0;
      expect(component.canGoToPreviousStep()).toBe(false);
    });

    it('should disable next button on final step', () => {
      component.currentStep = component.totalSteps - 1;
      expect(component.canGoToNextStep()).toBe(false);
    });
  });

  describe('Error Handling', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should handle service initialization errors', (done) => {
      nurseWorkflowService.getOutreachLogById.mockReturnValue(
        throwError(() => new Error('Failed to load'))
      );

      component.ngOnInit();

      setTimeout(() => {
        expect(toastService.error).toHaveBeenCalled();
        expect(component.loading).toBe(false);
        done();
      }, 100);
    });

    it('should clear form errors when user corrects input', () => {
      component.form.patchValue({ contactMethod: '' });
      expect(component.form.get('contactMethod').valid).toBe(false);

      component.form.patchValue({ contactMethod: 'CALL' });
      expect(component.form.get('contactMethod').valid).toBe(true);
    });
  });

  describe('Component Cleanup', () => {
    it('should unsubscribe on destroy', () => {
      jest.spyOn(component['destroy$'], 'next');
      jest.spyOn(component['destroy$'], 'complete');

      component.ngOnDestroy();

      expect(component['destroy$'].next).toHaveBeenCalled();
      expect(component['destroy$'].complete).toHaveBeenCalled();
    });
  });
});
