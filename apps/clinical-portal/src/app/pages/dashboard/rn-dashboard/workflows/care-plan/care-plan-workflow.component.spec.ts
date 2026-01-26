/**
 * Care Plan Workflow Component - Unit Tests
 *
 * Tests for creating and managing comprehensive care plans with
 * hierarchical problems, goals, interventions, and team coordination.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { CarePlanWorkflowComponent } from './care-plan-workflow.component';
import { CarePlanService } from '../../../../services/care-plan/care-plan.service';
import { ToastService } from '../../../../services/toast.service';
import { LoggerService } from '../../../../services/logger.service';
import { createMockMatDialogRef } from '../../testing/mocks';

describe('CarePlanWorkflowComponent', () => {
  let component: CarePlanWorkflowComponent;
  let fixture: ComponentFixture<CarePlanWorkflowComponent>;
  let carePlanService: jasmine.SpyObj<CarePlanService>;
  let toastService: jasmine.SpyObj<ToastService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CarePlanWorkflowComponent>>;

  const mockDialogData = {
    carePlanId: 'CP_001',
    patientId: 'PATIENT001',
    patientName: 'Helen Martinez',
  };

  const mockCarePlanTemplates = [
    { id: 'TEMPLATE_001', name: 'Diabetes Management', category: 'CHRONIC_DISEASE' },
    { id: 'TEMPLATE_002', name: 'Heart Failure', category: 'CHRONIC_DISEASE' },
  ];

  beforeEach(async () => {
    const carePlanSpy = jasmine.createSpyObj('CarePlanService', [
      'getCarePlanById',
      'getCarePlanTemplates',
      'createCarePlan',
      'addProblem',
      'addGoal',
      'addIntervention',
      'addTeamMember',
      'updateCarePlan',
      'completeCarePlan',
      'setTenantContext',
    ]);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error', 'info']);
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
        { provide: CarePlanService, useValue: carePlanSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: mockDialogData },
        { provide: LoggerService, useValue: loggerSpy },
        { provide: MatDialogRef, useValue: createMockMatDialogRef() }],
    }).compileComponents();

    carePlanService = TestBed.inject(CarePlanService) as jasmine.SpyObj<CarePlanService>;
    toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    dialogRef = TestBed.inject(MatDialogRef) as jasmine.SpyObj<MatDialogRef<CarePlanWorkflowComponent>>;

    fixture = TestBed.createComponent(CarePlanWorkflowComponent);
    component = fixture.componentInstance;
  });

  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with dialog data', () => {
      expect(component.carePlanId).toBe('CP_001');
      expect(component.patientId).toBe('PATIENT001');
      expect(component.patientName).toBe('Helen Martinez');
    });

    it('should set current step to 0', () => {
      expect(component.currentStep).toBe(0);
      expect(component.totalSteps).toBeGreaterThan(0);
    });

    it('should load care plan templates on initialization', () => {
      carePlanService.getCarePlanTemplates.and.returnValue(of(mockCarePlanTemplates));

      component.ngOnInit();

      expect(carePlanService.getCarePlanTemplates).toHaveBeenCalled();
    });
  });

  describe('Step 0: Initialize Care Plan', () => {
    beforeEach(() => {
      component.currentStep = 0;
      carePlanService.getCarePlanTemplates.and.returnValue(of(mockCarePlanTemplates));
    });

    it('should display care plan templates', (done) => {
      component.ngOnInit();

      setTimeout(() => {
        expect(component.carePlanTemplates.length).toBe(2);
        done();
      }, 100);
    });

    it('should require template selection', () => {
      component.form.patchValue({ selectedTemplate: '' });
      expect(component.canProceedToNextStep()).toBe(false);

      component.form.patchValue({ selectedTemplate: 'TEMPLATE_001' });
      expect(component.canProceedToNextStep()).toBe(true);
    });

    it('should initialize care plan with selected template', () => {
      const mockResponse = { id: 'CP_001', status: 'active' };
      carePlanService.createCarePlan.and.returnValue(of(mockResponse));

      component.form.patchValue({ selectedTemplate: 'TEMPLATE_001' });
      component.initializeCarePlan();

      expect(carePlanService.createCarePlan).toHaveBeenCalled();
    });
  });

  describe('Step 1: Add Problems/Diagnoses', () => {
    beforeEach(() => {
      component.currentStep = 1;
    });

    it('should initialize problem form', () => {
      expect(component.problemForm).toBeDefined();
      expect(component.problemForm.get('problemName')).toBeDefined();
    });

    it('should add problem to list', () => {
      component.problemForm.patchValue({
        problemName: 'Type 2 Diabetes',
        severity: 'HIGH',
      });

      component.addProblem();

      expect(component.problems.length).toBeGreaterThan(0);
      expect(component.problems[0].problemName).toBe('Type 2 Diabetes');
    });

    it('should validate problem form before adding', () => {
      component.problemForm.patchValue({ problemName: '' });

      expect(component.problemForm.valid).toBe(false);

      component.problemForm.patchValue({ problemName: 'Hypertension' });
      expect(component.problemForm.valid).toBe(true);
    });

    it('should remove problem from list', () => {
      component.problems = [
        { id: '1', problemName: 'Diabetes', severity: 'HIGH' },
        { id: '2', problemName: 'Hypertension', severity: 'MEDIUM' },
      ];

      component.removeProblem(0);

      expect(component.problems.length).toBe(1);
    });

    it('should require at least one problem', () => {
      component.problems = [];
      expect(component.canProceedToNextStep()).toBe(false);

      component.problems = [{ id: '1', problemName: 'Diabetes', severity: 'HIGH' }];
      expect(component.canProceedToNextStep()).toBe(true);
    });
  });

  describe('Step 2: Define Goals', () => {
    beforeEach(() => {
      component.currentStep = 2;
      component.problems = [{ id: '1', problemName: 'Diabetes', severity: 'HIGH' }];
    });

    it('should initialize goal form', () => {
      expect(component.goalForm).toBeDefined();
      expect(component.goalForm.get('goalDescription')).toBeDefined();
    });

    it('should add goal linked to problem', () => {
      component.goalForm.patchValue({
        relatedProblemId: '1',
        goalDescription: 'Achieve HbA1c < 7%',
        targetDate: new Date(),
      });

      component.addGoal();

      expect(component.goals.length).toBeGreaterThan(0);
      expect(component.goals[0].relatedProblemId).toBe('1');
    });

    it('should validate goal form', () => {
      component.goalForm.patchValue({ goalDescription: '' });
      expect(component.goalForm.valid).toBe(false);

      component.goalForm.patchValue({ goalDescription: 'Maintain blood glucose levels' });
      expect(component.goalForm.valid).toBe(true);
    });

    it('should validate target date is in future', () => {
      const pastDate = new Date();
      pastDate.setDate(pastDate.getDate() - 1);

      component.goalForm.patchValue({ targetDate: pastDate });
      expect(component.goalForm.get('targetDate').valid).toBe(false);

      const futureDate = new Date();
      futureDate.setMonth(futureDate.getMonth() + 3);
      component.goalForm.patchValue({ targetDate: futureDate });
      expect(component.goalForm.get('targetDate').valid).toBe(true);
    });

    it('should remove goal from list', () => {
      component.goals = [
        { id: '1', goalDescription: 'Goal 1', targetDate: new Date() },
        { id: '2', goalDescription: 'Goal 2', targetDate: new Date() },
      ];

      component.removeGoal(0);

      expect(component.goals.length).toBe(1);
    });

    it('should require at least one goal', () => {
      component.goals = [];
      expect(component.canProceedToNextStep()).toBe(false);

      component.goals = [{ id: '1', goalDescription: 'Goal', targetDate: new Date() }];
      expect(component.canProceedToNextStep()).toBe(true);
    });
  });

  describe('Step 3: Plan Interventions', () => {
    beforeEach(() => {
      component.currentStep = 3;
      component.goals = [{ id: '1', goalDescription: 'Achieve HbA1c < 7%', targetDate: new Date() }];
    });

    it('should initialize intervention form', () => {
      expect(component.interventionForm).toBeDefined();
      expect(component.interventionForm.get('interventionName')).toBeDefined();
    });

    it('should add intervention linked to goal', () => {
      component.interventionForm.patchValue({
        relatedGoalId: '1',
        interventionName: 'Monthly glucose monitoring',
        frequency: 'WEEKLY',
      });

      component.addIntervention();

      expect(component.interventions.length).toBeGreaterThan(0);
      expect(component.interventions[0].relatedGoalId).toBe('1');
    });

    it('should validate intervention form', () => {
      component.interventionForm.patchValue({ interventionName: '' });
      expect(component.interventionForm.valid).toBe(false);

      component.interventionForm.patchValue({ interventionName: 'Patient education' });
      expect(component.interventionForm.valid).toBe(true);
    });

    it('should remove intervention from list', () => {
      component.interventions = [
        { id: '1', interventionName: 'Intervention 1', frequency: 'DAILY' },
        { id: '2', interventionName: 'Intervention 2', frequency: 'WEEKLY' },
      ];

      component.removeIntervention(0);

      expect(component.interventions.length).toBe(1);
    });
  });

  describe('Step 4: Assign Team Members', () => {
    beforeEach(() => {
      component.currentStep = 4;
    });

    it('should initialize team member form', () => {
      expect(component.teamMemberForm).toBeDefined();
      expect(component.teamMemberForm.get('teamMemberId')).toBeDefined();
    });

    it('should add team member with role', () => {
      component.teamMemberForm.patchValue({
        teamMemberId: 'NURSE_001',
        teamMemberName: 'Jane Smith, RN',
        role: 'PRIMARY_NURSE',
      });

      component.addTeamMember();

      expect(component.teamMembers.length).toBeGreaterThan(0);
      expect(component.teamMembers[0].role).toBe('PRIMARY_NURSE');
    });

    it('should validate team member form', () => {
      component.teamMemberForm.patchValue({
        teamMemberId: '',
        teamMemberName: '',
      });

      expect(component.teamMemberForm.valid).toBe(false);

      component.teamMemberForm.patchValue({
        teamMemberId: 'NURSE_001',
        teamMemberName: 'Jane Smith, RN',
      });

      expect(component.teamMemberForm.valid).toBe(true);
    });

    it('should remove team member', () => {
      component.teamMembers = [
        { id: '1', name: 'Nurse 1', role: 'PRIMARY_NURSE' },
        { id: '2', name: 'Doctor 1', role: 'PHYSICIAN' },
      ];

      component.removeTeamMember(0);

      expect(component.teamMembers.length).toBe(1);
    });

    it('should not allow duplicate roles as primary', () => {
      component.teamMembers = [
        { id: '1', name: 'Nurse 1', role: 'PRIMARY_NURSE' },
      ];

      component.teamMemberForm.patchValue({
        teamMemberId: 'NURSE_002',
        teamMemberName: 'Nurse 2',
        role: 'PRIMARY_NURSE',
      });

      expect(component.canAddTeamMember()).toBe(false);
    });
  });

  describe('Step 5: Review & Schedule Reviews', () => {
    it('should generate care plan summary', () => {
      component.problems = [{ id: '1', problemName: 'Diabetes', severity: 'HIGH' }];
      component.goals = [{ id: '1', goalDescription: 'Goal', targetDate: new Date() }];
      component.interventions = [{ id: '1', interventionName: 'Action', frequency: 'WEEKLY' }];
      component.teamMembers = [{ id: '1', name: 'Nurse', role: 'PRIMARY_NURSE' }];

      component.generateCarePlanSummary();

      expect(component.carePlanSummary).toBeDefined();
      expect(component.carePlanSummary.problems).toEqual(component.problems);
      expect(component.carePlanSummary.goals).toEqual(component.goals);
    });

    it('should schedule next review date', () => {
      const futureDate = new Date();
      futureDate.setMonth(futureDate.getMonth() + 1);

      component.form.patchValue({ nextReviewDate: futureDate });

      expect(component.form.get('nextReviewDate').value).toEqual(futureDate);
    });

    it('should require next review date', () => {
      component.currentStep = 5;
      component.form.patchValue({ nextReviewDate: null });

      expect(component.canProceedToNextStep()).toBe(false);

      const futureDate = new Date();
      futureDate.setMonth(futureDate.getMonth() + 1);
      component.form.patchValue({ nextReviewDate: futureDate });

      expect(component.canProceedToNextStep()).toBe(true);
    });
  });

  describe('Workflow Submission', () => {
    it('should save complete care plan', (done) => {
      const mockResponse = { id: 'CP_001', status: 'completed' };
      carePlanService.completeCarePlan.and.returnValue(of(mockResponse));

      component.completeCarePlanWorkflow();

      setTimeout(() => {
        expect(carePlanService.completeCarePlan).toHaveBeenCalled();
        expect(toastService.success).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should close dialog on successful completion', (done) => {
      const mockResponse = { id: 'CP_001', status: 'completed' };
      carePlanService.completeCarePlan.and.returnValue(of(mockResponse));

      component.completeCarePlanWorkflow();

      setTimeout(() => {
        expect(dialogRef.close).toHaveBeenCalledWith({ success: true, result: mockResponse });
        done();
      }, 100);
    });

    it('should handle save errors gracefully', (done) => {
      carePlanService.completeCarePlan.and.returnValue(
        throwError(() => new Error('Save failed'))
      );

      component.completeCarePlanWorkflow();

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
