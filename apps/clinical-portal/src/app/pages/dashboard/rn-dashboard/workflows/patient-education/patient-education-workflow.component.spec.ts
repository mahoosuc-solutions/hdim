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
import { NurseWorkflowService } from '../../../../services/nurse-workflow/nurse-workflow.service';
import { ToastService } from '../../../../services/toast.service';
import { LoggerService } from '../../../../services/logger.service';
import { createMockMatDialogRef } from '../../testing/mocks';

describe('PatientEducationWorkflowComponent', () => {
  let component: PatientEducationWorkflowComponent;
  let fixture: ComponentFixture<PatientEducationWorkflowComponent>;
  let nurseWorkflowService: jasmine.SpyObj<NurseWorkflowService>;
  let toastService: jasmine.SpyObj<ToastService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<PatientEducationWorkflowComponent>>;

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
    const nurseWorkflowSpy = jasmine.createSpyObj('NurseWorkflowService', [
      'getEducationTopics',
      'getEducationSessionById',
      'recordPatientEducation',
      'assessUnderstanding',
      'setTenantContext',
    ]);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error', 'info', 'warning']);
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
      providers: [FormBuilder,
        { provide: NurseWorkflowService, useValue: nurseWorkflowSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: mockDialogData },
        { provide: LoggerService, useValue: loggerSpy },
        { provide: MatDialogRef, useValue: createMockMatDialogRef() }],
    }).compileComponents();

    nurseWorkflowService = TestBed.inject(NurseWorkflowService) as jasmine.SpyObj<NurseWorkflowService>;
    toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    dialogRef = TestBed.inject(MatDialogRef) as jasmine.SpyObj<MatDialogRef<PatientEducationWorkflowComponent>>;

    fixture = TestBed.createComponent(PatientEducationWorkflowComponent);
    component = fixture.componentInstance;
  });

  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    }, 30000);

    it('should initialize with dialog data', () => {
      expect(component.educationSessionId).toBe('EDU_SESSION001');
      expect(component.patientId).toBe('PATIENT001');
      expect(component.patientName).toBe('Robert Johnson');
    }, 30000);

    it('should set current step to 0', () => {
      expect(component.currentStep).toBe(0);
      expect(component.totalSteps).toBe(5);
    }, 30000);

    it('should load education topics on initialization', () => {
      nurseWorkflowService.getEducationTopics.and.returnValue(of(mockEducationTopics));

      component.ngOnInit();

      expect(nurseWorkflowService.getEducationTopics).toHaveBeenCalled();
    }, 30000);
  }, 30000);

  describe('Step 0: Select Education Topic', () => {
    beforeEach(() => {
      nurseWorkflowService.getEducationTopics.and.returnValue(of(mockEducationTopics));
      component.ngOnInit();
    }, 30000);

    it('should display education topics', (done) => {
      setTimeout(() => {
        expect(component.educationTopics.length).toBe(2);
        done();
      }, 100);
    }, 30000);

    it('should require topic selection before proceeding', () => {
      component.form.patchValue({ selectedTopic: '' }, 30000);
      expect(component.canProceedToNextStep()).toBe(false);

      component.form.patchValue({ selectedTopic: 'TOPIC_001' }, 30000);
      expect(component.canProceedToNextStep()).toBe(true);
    }, 30000);

    it('should load materials for selected topic', () => {
      component.form.patchValue({ selectedTopic: 'TOPIC_001' }, 30000);
      component.onTopicSelected();

      expect(component.selectedTopicMaterials.length).toBeGreaterThan(0);
    }, 30000);

    it('should show topic description and duration', () => {
      component.form.patchValue({ selectedTopic: 'TOPIC_001' }, 30000);
      component.onTopicSelected();

      expect(component.selectedTopicName).toBe('Diabetes Management');
    }, 30000);
  }, 30000);

  describe('Step 1: Select Material Type', () => {
    beforeEach(() => {
      component.currentStep = 1;
      component.materialTypes = ['VIDEO', 'HANDOUT', 'INTERACTIVE'];
    }, 30000);

    it('should display available material types', () => {
      expect(component.materialTypes).toEqual(['VIDEO', 'HANDOUT', 'INTERACTIVE']);
    }, 30000);

    it('should require material type selection', () => {
      component.form.patchValue({ materialType: '' }, 30000);
      expect(component.form.valid).toBe(false);

      component.form.patchValue({ materialType: 'VIDEO' }, 30000);
      expect(component.form.valid).toBe(true);
    }, 30000);

    it('should show material duration estimate for videos', () => {
      component.form.patchValue({ materialType: 'VIDEO' }, 30000);
      const duration = component.getMaterialDuration();

      expect(duration).toBeGreaterThan(0);
    }, 30000);

    it('should enable next button when material type selected', () => {
      component.form.patchValue({ materialType: 'VIDEO' }, 30000);
      expect(component.canProceedToNextStep()).toBe(true);
    }, 30000);
  }, 30000);

  describe('Step 2: Present Material', () => {
    beforeEach(() => {
      component.currentStep = 2;
    }, 30000);

    it('should display selected material content', () => {
      component.form.patchValue({ materialType: 'VIDEO' }, 30000);
      expect(component.shouldShowMaterialContent()).toBe(true);
    }, 30000);

    it('should show video player for video materials', () => {
      component.form.patchValue({ materialType: 'VIDEO' }, 30000);
      expect(component.isVideoMaterial()).toBe(true);
    }, 30000);

    it('should show handout content for handout materials', () => {
      component.form.patchValue({ materialType: 'HANDOUT' }, 30000);
      expect(component.isHandoutMaterial()).toBe(true);
    }, 30000);

    it('should show interactive quiz for interactive materials', () => {
      component.form.patchValue({ materialType: 'INTERACTIVE' }, 30000);
      expect(component.isInteractiveMaterial()).toBe(true);
    }, 30000);

    it('should allow marking material as completed', () => {
      component.markMaterialComplete();
      expect(component.materialCompleted).toBe(true);
    }, 30000);

    it('should require material completion before proceeding', () => {
      component.materialCompleted = false;
      expect(component.canProceedToNextStep()).toBe(false);

      component.materialCompleted = true;
      expect(component.canProceedToNextStep()).toBe(true);
    }, 30000);
  }, 30000);

  describe('Step 3: Assess Understanding', () => {
    beforeEach(() => {
      component.currentStep = 3;
    }, 30000);

    it('should display assessment questions', () => {
      expect(component.assessmentQuestions.length).toBeGreaterThan(0);
    }, 30000);

    it('should calculate understanding score', () => {
      component.recordAssessmentAnswers([1, 1, 0, 1]); // 75% correct
      component.calculateUnderstandingScore();

      expect(component.understandingScore).toBeGreaterThan(0);
      expect(component.understandingScore).toBeLessThanOrEqual(100);
    }, 30000);

    it('should classify understanding levels correctly', () => {
      component.understandingScore = 90;
      expect(component.getUnderstandingLevel()).toBe('EXCELLENT');

      component.understandingScore = 75;
      expect(component.getUnderstandingLevel()).toBe('GOOD');

      component.understandingScore = 60;
      expect(component.getUnderstandingLevel()).toBe('FAIR');

      component.understandingScore = 40;
      expect(component.getUnderstandingLevel()).toBe('POOR');
    }, 30000);

    it('should require minimum score to proceed for new learners', () => {
      component.understandingScore = 50;
      expect(component.canProceedToNextStep()).toBe(false);

      component.understandingScore = 70;
      expect(component.canProceedToNextStep()).toBe(true);
    }, 30000);

    it('should flag poor understanding for follow-up', () => {
      component.understandingScore = 40;
      component.flagForFollowUp();

      expect(component.form.get('needsFollowUp').value).toBe(true);
      expect(component.form.get('followUpReason').value).toContain('understanding');
    }, 30000);
  }, 30000);

  describe('Step 4: Document Learning Barriers', () => {
    beforeEach(() => {
      component.currentStep = 4;
    }, 30000);

    it('should display learning barrier checkboxes', () => {
      expect(component.learningBarrierOptions.length).toBeGreaterThan(0);
    }, 30000);

    it('should allow selecting multiple learning barriers', () => {
      component.form.patchValue({
        healthLiteracy: true,
        language: true,
        cognitive: false,
      }, 30000);

      expect(component.form.get('healthLiteracy').value).toBe(true);
      expect(component.form.get('language').value).toBe(true);
      expect(component.form.get('cognitive').value).toBe(false);
    }, 30000);

    it('should show detailed barrier descriptions', () => {
      component.form.patchValue({ healthLiteracy: true }, 30000);
      expect(component.getBarrierDescription('healthLiteracy')).toBeDefined();
    }, 30000);

    it('should allow recording specific barrier notes', () => {
      component.form.patchValue({
        healthLiteracy: true,
        barrierNotes: 'Patient struggled with medical terminology',
      }, 30000);

      expect(component.form.get('barrierNotes').value).toContain('struggled');
    }, 30000);

    it('should suggest educational modifications based on barriers', () => {
      component.form.patchValue({ language: true }, 30000);
      const suggestions = component.getSuggestedModifications();

      expect(suggestions).toContain(jasmine.objectContaining({
        barrier: 'language',
      }));
    }, 30000);

    it('should schedule follow-up if barriers identified', () => {
      component.form.patchValue({
        healthLiteracy: true,
        needsFollowUp: false,
      }, 30000);

      component.applyBarrierRecommendations();

      expect(component.form.get('needsFollowUp').value).toBe(true);
    }, 30000);
  }, 30000);

  describe('Step 5: Schedule Follow-up & Complete', () => {
    beforeEach(() => {
      component.currentStep = 4;
    }, 30000);

    it('should initialize follow-up form fields', () => {
      expect(component.form.get('scheduleFollowUp')).toBeDefined();
      expect(component.form.get('followUpDate')).toBeDefined();
    }, 30000);

    it('should validate follow-up date when needed', () => {
      component.form.patchValue({ needsFollowUp: true }, 30000);

      component.form.patchValue({ followUpDate: null }, 30000);
      expect(component.form.valid).toBe(false);

      const futureDate = new Date();
      futureDate.setDate(futureDate.getDate() + 7);
      component.form.patchValue({ followUpDate: futureDate }, 30000);
      expect(component.form.valid).toBe(true);
    }, 30000);

    it('should allow suggesting follow-up interval', () => {
      component.understandingScore = 50;
      const suggestedInterval = component.suggestFollowUpInterval();

      expect(suggestedInterval).toBeGreaterThan(0);
    }, 30000);

    it('should generate education record summary', () => {
      component.generateEducationSummary();

      expect(component.educationSummary).toBeDefined();
      expect(component.educationSummary.topicId).toBeDefined();
      expect(component.educationSummary.understandingLevel).toBeDefined();
    }, 30000);
  }, 30000);

  describe('Workflow Submission', () => {
    it('should save education session', (done) => {
      const mockResponse = { id: 'EDU_SESSION001', status: 'completed' };
      nurseWorkflowService.recordPatientEducation.and.returnValue(of(mockResponse));

      component.completeEducationWorkflow();

      setTimeout(() => {
        expect(nurseWorkflowService.recordPatientEducation).toHaveBeenCalled();
        expect(toastService.success).toHaveBeenCalled();
        done();
      }, 100);
    }, 30000);

    it('should close dialog on successful completion', (done) => {
      const mockResponse = { id: 'EDU_SESSION001', status: 'completed' };
      nurseWorkflowService.recordPatientEducation.and.returnValue(of(mockResponse));

      component.completeEducationWorkflow();

      setTimeout(() => {
        expect(dialogRef.close).toHaveBeenCalledWith({ success: true, result: mockResponse }, 30000);
        done();
      }, 100);
    });

    it('should handle save errors gracefully', (done) => {
      nurseWorkflowService.recordPatientEducation.and.returnValue(
        throwError(() => new Error('Save failed'))
      );

      component.completeEducationWorkflow();

      setTimeout(() => {
        expect(toastService.error).toHaveBeenCalled();
        done();
      }, 100);
    }, 30000);
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
