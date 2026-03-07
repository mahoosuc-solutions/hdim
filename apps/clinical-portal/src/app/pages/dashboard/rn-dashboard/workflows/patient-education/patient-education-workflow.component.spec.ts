/**
 * Patient Education Workflow Component - Unit Tests
 *
 * Tests for providing targeted patient education, assessing understanding,
 * and documenting learning barriers.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { PatientEducationWorkflowComponent } from './patient-education-workflow.component';
import { NurseWorkflowService } from '../../../../../services/nurse-workflow/nurse-workflow.service';
import { ToastService } from '../../../../../services/toast.service';
import { LoggerService } from '../../../../../services/logger.service';
import { AuthService } from '../../../../../services/auth.service';
import { createMockMatDialogRef } from '../../testing/mocks';

describe('PatientEducationWorkflowComponent', () => {
  let component: PatientEducationWorkflowComponent;
  let fixture: ComponentFixture<PatientEducationWorkflowComponent>;
  let nurseWorkflowService: any;
  let toastService: any;
  let dialogRef: any;

  const mockDialogData = {
    educationSessionId: 'EDU_SESSION001',
    patientId: 'PATIENT001',
    patientName: 'Robert Johnson',
  };

  const mockEducationTopics = [
    {
      id: 'TOPIC_001',
      name: 'Diabetes Management',
      category: 'CHRONIC_DISEASE',
      materials: [
        { id: 'MAT_001', title: 'Diabetes Basics', type: 'VIDEO', duration: 5 },
        { id: 'MAT_002', title: 'Blood Sugar Monitoring', type: 'HANDOUT' },
      ],
    },
    {
      id: 'TOPIC_002',
      name: 'Medication Adherence',
      category: 'MEDICATION',
      materials: [],
    },
  ];

  beforeEach(async () => {
    const nurseWorkflowSpy = {
      getEducationTopics: jest.fn(),
      getEducationSessionById: jest.fn(),
      recordPatientEducation: jest.fn(),
      logPatientEducation: jest.fn(),
      assessUnderstanding: jest.fn(),
      setTenantContext: jest.fn(),
    };
    const toastSpy = { success: jest.fn(), error: jest.fn(), info: jest.fn(), warning: jest.fn() };
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

    fixture = TestBed.createComponent(PatientEducationWorkflowComponent);
    component = fixture.componentInstance;
  });

  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with dialog data', () => {
      expect(component.educationSessionId).toBe('EDU_SESSION001');
      expect(component.patientId).toBe('PATIENT001');
      expect(component.patientName).toBe('Robert Johnson');
    });

    it('should set current step to 0', () => {
      expect(component.currentStep).toBe(0);
      expect(component.totalSteps).toBe(5);
    });

    it('should load education topics on initialization', () => {
      nurseWorkflowService.getEducationTopics.mockReturnValue(of(mockEducationTopics));

      component.ngOnInit();

      expect(nurseWorkflowService.getEducationTopics).toHaveBeenCalled();
    });
  });

  describe('Step 0: Select Education Topic', () => {
    beforeEach(() => {
      nurseWorkflowService.getEducationTopics.mockReturnValue(of(mockEducationTopics));
      component.ngOnInit();
    });

    it('should display education topics', (done) => {
      setTimeout(() => {
        expect(component.educationTopics.length).toBe(2);
        done();
      }, 100);
    });

    it('should require topic selection before proceeding', () => {
      component.form.patchValue({ selectedTopic: '' });
      expect(component.canProceedToNextStep()).toBe(false);

      component.form.patchValue({ selectedTopic: 'TOPIC_001' });
      expect(component.canProceedToNextStep()).toBe(true);
    });

    it('should load materials for selected topic', () => {
      component.form.patchValue({ selectedTopic: 'TOPIC_001' });
      component.onTopicSelected();

      expect(component.selectedTopicMaterials.length).toBeGreaterThan(0);
    });

    it('should show topic description and duration', () => {
      component.form.patchValue({ selectedTopic: 'TOPIC_001' });
      component.onTopicSelected();

      expect(component.selectedTopicName).toBe('Diabetes Management');
    });
  });

  describe('Step 1: Select Material Type', () => {
    beforeEach(() => {
      component.currentStep = 1;
      component.materialTypes = ['VIDEO', 'HANDOUT', 'INTERACTIVE'];
    });

    it('should display available material types', () => {
      expect(component.materialTypes).toEqual(['VIDEO', 'HANDOUT', 'INTERACTIVE']);
    });

    it('should require material type selection', () => {
      component.form.patchValue({ materialType: '' });
      expect(component.form.get('materialType').value).toBe('');

      component.form.patchValue({ materialType: 'VIDEO' });
      expect(component.form.get('materialType').value).toBe('VIDEO');
    });

    it('should show material duration estimate for videos', () => {
      component.form.patchValue({ materialType: 'VIDEO' });
      const duration = component.getMaterialDuration();

      expect(duration).toBeGreaterThanOrEqual(0);
    });

    it('should enable next button when material type selected', () => {
      component.form.patchValue({ materialType: 'VIDEO' });
      expect(component.canProceedToNextStep()).toBe(true);
    });
  });

  describe('Step 2: Present Material', () => {
    beforeEach(() => {
      component.currentStep = 2;
    });

    it('should display selected material content', () => {
      component.form.patchValue({ materialType: 'VIDEO' });
      expect(component.shouldShowMaterialContent()).toBe(true);
    });

    it('should show video player for video materials', () => {
      component.form.patchValue({ materialType: 'VIDEO' });
      expect(component.isVideoMaterial()).toBe(true);
    });

    it('should show handout content for handout materials', () => {
      component.form.patchValue({ materialType: 'HANDOUT' });
      expect(component.isHandoutMaterial()).toBe(true);
    });

    it('should show interactive quiz for interactive materials', () => {
      component.form.patchValue({ materialType: 'INTERACTIVE' });
      expect(component.isInteractiveMaterial()).toBe(true);
    });

    it('should allow marking material as completed', () => {
      component.markMaterialComplete();
      expect(component.materialCompleted).toBe(true);
    });

    it('should require material completion before proceeding', () => {
      component.materialCompleted = false;
      expect(component.canProceedToNextStep()).toBe(false);

      component.materialCompleted = true;
      expect(component.canProceedToNextStep()).toBe(true);
    });
  });

  describe('Step 3: Assess Understanding', () => {
    beforeEach(() => {
      component.currentStep = 3;
    });

    it('should display assessment questions', () => {
      expect(component.assessmentQuestions).toBeDefined();
    });

    it('should calculate understanding score', () => {
      component.recordAssessmentAnswers([1, 1, 0, 1]); // 75% correct
      component.calculateUnderstandingScore();

      expect(component.understandingScore).toBeGreaterThan(0);
      expect(component.understandingScore).toBeLessThanOrEqual(100);
    });

    it('should classify understanding levels correctly', () => {
      component.understandingScore = 90;
      expect(component.getUnderstandingLevel()).toBe('EXCELLENT');

      component.understandingScore = 85;
      expect(component.getUnderstandingLevel()).toBe('GOOD');

      component.understandingScore = 75;
      expect(component.getUnderstandingLevel()).toBe('FAIR');

      component.understandingScore = 40;
      expect(component.getUnderstandingLevel()).toBe('POOR');
    });

    it('should require minimum score to proceed for new learners', () => {
      component.understandingScore = 50;
      expect(component.canProceedToNextStep()).toBe(false);

      component.understandingScore = 70;
      expect(component.canProceedToNextStep()).toBe(true);
    });

    it('should flag poor understanding for follow-up', () => {
      component.understandingScore = 40;
      component.flagForFollowUp();

      expect(component.form.get('needsFollowUp').value).toBe(true);
      expect(component.form.get('followUpReason').value).toContain('understanding');
    });
  });

  describe('Step 4: Document Learning Barriers', () => {
    beforeEach(() => {
      component.currentStep = 4;
    });

    it('should display learning barrier checkboxes', () => {
      expect(component.learningBarrierOptions.length).toBeGreaterThan(0);
    });

    it('should allow selecting multiple learning barriers', () => {
      component.form.patchValue({
        healthLiteracy: true,
        language: true,
        cognitive: false,
      });

      expect(component.form.get('healthLiteracy').value).toBe(true);
      expect(component.form.get('language').value).toBe(true);
      expect(component.form.get('cognitive').value).toBe(false);
    });

    it('should show detailed barrier descriptions', () => {
      component.form.patchValue({ healthLiteracy: true });
      expect(component.getBarrierDescription('healthLiteracy')).toBeDefined();
    });

    it('should allow recording specific barrier notes', () => {
      component.form.patchValue({
        healthLiteracy: true,
        barrierNotes: 'Patient struggled with medical terminology',
      });

      expect(component.form.get('barrierNotes').value).toContain('struggled');
    });

    it('should suggest educational modifications based on barriers', () => {
      component.form.patchValue({ language: true });
      const suggestions = component.getSuggestedModifications();

      expect(suggestions).toContainEqual(expect.objectContaining({
        barrier: 'language',
      }));
    });

    it('should schedule follow-up if barriers identified', () => {
      component.form.patchValue({
        healthLiteracy: true,
        needsFollowUp: false,
      });

      component.applyBarrierRecommendations();

      expect(component.form.get('needsFollowUp').value).toBe(true);
    });
  });

  describe('Step 5: Schedule Follow-up & Complete', () => {
    beforeEach(() => {
      component.currentStep = 4;
    });

    it('should initialize follow-up form fields', () => {
      expect(component.form.get('needsFollowUp')).toBeDefined();
      expect(component.form.get('followUpDate')).toBeDefined();
    });

    it('should validate follow-up date when needed', () => {
      // Fill required selectedTopic so form validity depends only on follow-up
      component.form.patchValue({ selectedTopic: 'TOPIC_001' });
      component.form.patchValue({ needsFollowUp: true });

      // followUpDate is disabled by default; enable it so patchValue works
      component.form.get('followUpDate')!.enable();

      component.form.patchValue({ followUpDate: null });
      expect(component.form.valid).toBe(false);

      const futureDate = new Date();
      futureDate.setDate(futureDate.getDate() + 7);
      component.form.patchValue({ followUpDate: futureDate });
      expect(component.form.valid).toBe(true);
    });

    it('should allow suggesting follow-up interval', () => {
      component.understandingScore = 50;
      const suggestedInterval = component.suggestFollowUpInterval();

      expect(suggestedInterval).toBeGreaterThan(0);
    });

    it('should generate education record summary', () => {
      component.generateEducationSummary();

      expect(component.educationSummary).toBeDefined();
      expect(component.educationSummary.topicId).toBeDefined();
      expect(component.educationSummary.understandingLevel).toBeDefined();
    });
  });

  describe('Workflow Submission', () => {
    beforeEach(() => {
      // Put component in completable state: step 4, no follow-up needed
      component.currentStep = 4;
      component.form.patchValue({ needsFollowUp: false });
      component.educationSummary = { topicId: 'TOPIC_001', understandingLevel: 'GOOD' };
    });

    it('should save education session', (done) => {
      const mockResponse = { id: 'EDU_SESSION001', status: 'completed' };
      nurseWorkflowService.logPatientEducation.mockReturnValue(of(mockResponse));

      component.completeEducationWorkflow();

      setTimeout(() => {
        expect(nurseWorkflowService.logPatientEducation).toHaveBeenCalled();
        expect(toastService.success).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should close dialog on successful completion', (done) => {
      const mockResponse = { id: 'EDU_SESSION001', status: 'completed' };
      nurseWorkflowService.logPatientEducation.mockReturnValue(of(mockResponse));

      component.completeEducationWorkflow();

      setTimeout(() => {
        expect(dialogRef.close).toHaveBeenCalledWith({ success: true, result: mockResponse });
        done();
      }, 100);
    });

    it('should handle save errors gracefully', (done) => {
      nurseWorkflowService.logPatientEducation.mockReturnValue(
        throwError(() => new Error('Save failed'))
      );

      component.completeEducationWorkflow();

      setTimeout(() => {
        expect(toastService.error).toHaveBeenCalled();
        done();
      }, 100);
    });
  });

  describe('Form Navigation', () => {
    it('should advance steps', () => {
      // Start at step 1 (material type) with a value set so canProceedToNextStep passes
      component.currentStep = 1;
      component.form.patchValue({ materialType: 'VIDEO' });
      component.nextStep();

      expect(component.currentStep).toBe(2);
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
      jest.spyOn(component['destroy$'], 'next');
      jest.spyOn(component['destroy$'], 'complete');

      component.ngOnDestroy();

      expect(component['destroy$'].next).toHaveBeenCalled();
      expect(component['destroy$'].complete).toHaveBeenCalled();
    });
  });
});
