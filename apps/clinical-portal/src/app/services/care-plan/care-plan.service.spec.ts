/**
 * Care Plan Service - Unit Tests
 *
 * Test-Driven Development approach: Tests define the contract
 * that the service implementation must satisfy.
 *
 * Coverage areas:
 * - Tenant context management
 * - Care plan CRUD and lifecycle operations
 * - Problem/diagnosis management
 * - Goal setting and progress tracking
 * - Intervention planning and execution
 * - Care team coordination
 * - Patient engagement tracking
 * - Care plan reviews and transitions
 * - Metrics and KPI reporting
 * - Error handling and caching
 */

import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import {
  CarePlanService,
  CARE_PLAN_BASE_URL,
} from './care-plan.service';
import {
  CarePlan,
  CarePlanProblem,
  CarePlanGoal,
  CarePlanIntervention,
  CarePlanTeamMember,
  PatientEngagement,
  CarePlanReview,
  CareTransition,
  CarePlanMetrics,
  CarePlanContext,
  PaginatedResponse,
  CarePlanStatus,
  CarePlanTemplate,
  CarePlanPriority,
  ProblemSeverity,
  ProblemStatus,
  GoalStatus,
  MeasurementUnit,
  InterventionType,
  InterventionFrequency,
  InterventionStatus,
  TeamMemberRole,
  PatientEngagementLevel,
  ReviewType,
  TransitionType,
} from './care-plan.models';

describe('CarePlanService', () => {
  let service: CarePlanService;
  let httpMock: HttpTestingController;

  const tenantId = 'TENANT001';
  const patientId = '550e8400-e29b-41d4-a716-446655440000';
  const coordinatorId = '660e8400-e29b-41d4-a716-446655440001';

  // Mock data
  const mockCarePlan: CarePlan = {
    id: 'plan-001',
    patientId,
    tenantId,
    primaryCaregiverId: coordinatorId,
    templateType: CarePlanTemplate.CHRONIC_DISEASE,
    status: CarePlanStatus.ACTIVE,
    priority: CarePlanPriority.HIGH,
    title: 'Diabetes Management Care Plan',
    description: 'Comprehensive care plan for Type 2 Diabetes',
    planStartDate: new Date('2024-01-01'),
    problemCount: 3,
    goalCount: 5,
    interventionCount: 12,
    patientEngaged: true,
  };

  const mockProblem: CarePlanProblem = {
    id: 'prob-001',
    carePlanId: 'plan-001',
    tenantId,
    icd10Code: 'E11.9',
    problemName: 'Type 2 Diabetes Mellitus',
    severity: ProblemSeverity.MODERATE,
    status: ProblemStatus.ACTIVE,
    onsetDate: new Date('2020-06-15'),
    priorityOrder: 1,
  };

  const mockGoal: CarePlanGoal = {
    id: 'goal-001',
    carePlanId: 'plan-001',
    problemId: 'prob-001',
    tenantId,
    goalStatement: 'Achieve HbA1c < 7%',
    targetDate: new Date('2024-06-01'),
    priority: 'HIGH',
    status: GoalStatus.IN_PROGRESS,
    measurementType: MeasurementUnit.PERCENTAGE,
    baselineValue: '8.5',
    targetValue: '7.0',
    currentValue: '8.1',
    progressNotes: 'Patient showing good progress with medication adherence',
  };

  const mockIntervention: CarePlanIntervention = {
    id: 'int-001',
    carePlanId: 'plan-001',
    goalId: 'goal-001',
    tenantId,
    interventionType: InterventionType.PATIENT_EDUCATION,
    description: 'Diabetes education and self-management training',
    actionStatement: 'Attend monthly diabetes education group sessions',
    status: InterventionStatus.IN_PROGRESS,
    frequency: InterventionFrequency.MONTHLY,
    priority: 'HIGH',
    responsibleParty: 'PROVIDER',
  };

  const mockTeamMember: CarePlanTeamMember = {
    id: 'team-001',
    carePlanId: 'plan-001',
    tenantId,
    userId: coordinatorId,
    fullName: 'Dr. Sarah Johnson',
    role: TeamMemberRole.PRIMARY_CARE_PHYSICIAN,
    isPrimaryCoordinator: true,
    assignedDate: new Date('2024-01-01'),
  };

  const mockEngagement: PatientEngagement = {
    id: 'eng-001',
    carePlanId: 'plan-001',
    patientId,
    tenantId,
    engagementLevel: PatientEngagementLevel.HIGHLY_ENGAGED,
    planReviewedWithPatient: true,
    planReviewDate: new Date('2024-01-10'),
    patientAgreement: true,
    agreedDate: new Date('2024-01-10'),
    preferredCommunicationChannel: 'EMAIL',
  };

  const mockReview: CarePlanReview = {
    id: 'review-001',
    carePlanId: 'plan-001',
    tenantId,
    reviewType: ReviewType.SCHEDULED,
    reviewDate: new Date('2024-01-15'),
    reviewedBy: coordinatorId,
    goalProgressSummary: 'Patient progressing well on diabetes control',
    interventionCompletionRate: 85,
    needsEscalation: false,
    nextReviewDate: new Date('2024-02-15'),
  };

  const mockTransition: CareTransition = {
    id: 'trans-001',
    carePlanId: 'plan-001',
    patientId,
    tenantId,
    transitionType: TransitionType.HOSPITAL_TO_HOME,
    fromLocation: 'City General Hospital',
    toLocation: 'Home',
    transitionDate: new Date('2024-01-20'),
    medicationReconciliationCompleted: true,
    handoffSummaryProvided: true,
    followUpAppointmentScheduled: true,
    status: 'PLANNED',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CarePlanService],
    });
    service = TestBed.inject(CarePlanService);
    httpMock = TestBed.inject(HttpTestingController);
    service.setTenantContext(tenantId);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ==================== Tenant Context Tests ====================

  describe('Tenant Context Management', () => {
    it('should set and retrieve tenant context', () => {
      service.setTenantContext('NEW_TENANT');
      expect(service.getTenantContext()).toBe('NEW_TENANT');
    });

    it('should throw error when making requests without tenant context', () => {
      const unsetService = new CarePlanService(TestBed.inject(HttpClientTestingModule) as any);
      expect(() =>
        unsetService.getCarePlanById('plan-001').subscribe()
      ).toThrowError('Tenant context not set');
    });
  });

  // ==================== Care Plan CRUD Tests ====================

  describe('Care Plan Operations', () => {
    it('should create a care plan', (done) => {
      service.createCarePlan(mockCarePlan).subscribe((result) => {
        expect(result.id).toBeDefined();
        expect(result.patientId).toBe(patientId);
        expect(result.status).toBe(CarePlanStatus.ACTIVE);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans') && r.method === 'POST'
      );
      expect(req.request.headers.get('X-Tenant-ID')).toBe(tenantId);
      req.flush(mockCarePlan);
    });

    it('should retrieve care plan by ID', (done) => {
      service.getCarePlanById('plan-001').subscribe((result) => {
        expect(result.id).toBe('plan-001');
        expect(result.title).toBe('Diabetes Management Care Plan');
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001')
      );
      req.flush(mockCarePlan);
    });

    it('should get active care plans for patient', (done) => {
      const mockResponse: PaginatedResponse<CarePlan> = {
        content: [mockCarePlan],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service.getActiveCarePlansForPatient(patientId, 0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        expect(result.content[0].status).toBe(CarePlanStatus.ACTIVE);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/patient') && r.url.includes('active')
      );
      req.flush(mockResponse);
    });

    it('should update care plan', (done) => {
      const updated = { ...mockCarePlan, status: CarePlanStatus.ON_HOLD };
      service.updateCarePlan('plan-001', updated).subscribe((result) => {
        expect(result.status).toBe(CarePlanStatus.ON_HOLD);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001') && r.method === 'PUT'
      );
      req.flush(updated);
    });

    it('should close/complete care plan', (done) => {
      service.closeCarePlan('plan-001', 'Goals achieved').subscribe((result) => {
        expect(result.status).toBe(CarePlanStatus.COMPLETED);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001/close')
      );
      req.flush({ ...mockCarePlan, status: CarePlanStatus.COMPLETED });
    });

    it('should get care plans due for review', (done) => {
      const mockResponse: PaginatedResponse<CarePlan> = {
        content: [mockCarePlan],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service.getCarePlansDueForReview(0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/due-for-review')
      );
      req.flush(mockResponse);
    });
  });

  // ==================== Problem Management Tests ====================

  describe('Care Plan Problem Management', () => {
    it('should add problem to care plan', (done) => {
      service.addProblem('plan-001', mockProblem).subscribe((result) => {
        expect(result.id).toBeDefined();
        expect(result.status).toBe(ProblemStatus.ACTIVE);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001/problems') && r.method === 'POST'
      );
      req.flush(mockProblem);
    });

    it('should retrieve problems for care plan', (done) => {
      const mockResponse: PaginatedResponse<CarePlanProblem> = {
        content: [mockProblem],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service.getProblemsForCarePlan('plan-001', 0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        expect(result.content[0].problemName).toBe('Type 2 Diabetes Mellitus');
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001/problems')
      );
      req.flush(mockResponse);
    });

    it('should update problem', (done) => {
      const updated = { ...mockProblem, severity: ProblemSeverity.SEVERE };
      service.updateProblem('prob-001', updated).subscribe((result) => {
        expect(result.severity).toBe(ProblemSeverity.SEVERE);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('problems/prob-001') && r.method === 'PUT'
      );
      req.flush(updated);
    });

    it('should resolve problem', (done) => {
      service.resolveProblem('prob-001', 'Patient achieved diabetes control').subscribe((result) => {
        expect(result.status).toBe(ProblemStatus.RESOLVED);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('problems/prob-001/resolve')
      );
      req.flush({ ...mockProblem, status: ProblemStatus.RESOLVED });
    });
  });

  // ==================== Goal Management Tests ====================

  describe('Care Plan Goal Management', () => {
    it('should add goal to care plan', (done) => {
      service.addGoal('plan-001', mockGoal).subscribe((result) => {
        expect(result.id).toBeDefined();
        expect(result.status).toBe(GoalStatus.IN_PROGRESS);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001/goals') && r.method === 'POST'
      );
      req.flush(mockGoal);
    });

    it('should retrieve goals for care plan', (done) => {
      const mockResponse: PaginatedResponse<CarePlanGoal> = {
        content: [mockGoal],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service.getGoalsForCarePlan('plan-001', 0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        expect(result.content[0].goalStatement).toBe('Achieve HbA1c < 7%');
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001/goals')
      );
      req.flush(mockResponse);
    });

    it('should update goal progress', (done) => {
      const updated = { ...mockGoal, currentValue: '7.5', progressNotes: 'Good progress' };
      service.updateGoal('goal-001', updated).subscribe((result) => {
        expect(result.currentValue).toBe('7.5');
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('goals/goal-001') && r.method === 'PUT'
      );
      req.flush(updated);
    });

    it('should mark goal as achieved', (done) => {
      service.achieveGoal('goal-001').subscribe((result) => {
        expect(result.status).toBe(GoalStatus.ACHIEVED);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('goals/goal-001/achieve')
      );
      req.flush({ ...mockGoal, status: GoalStatus.ACHIEVED });
    });

    it('should get goals nearing target date', (done) => {
      const mockResponse: PaginatedResponse<CarePlanGoal> = {
        content: [mockGoal],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service.getGoalsNearingTargetDate(0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('goals/nearing-target')
      );
      req.flush(mockResponse);
    });
  });

  // ==================== Intervention Management Tests ====================

  describe('Care Plan Intervention Management', () => {
    it('should add intervention to care plan', (done) => {
      service.addIntervention('plan-001', mockIntervention).subscribe((result) => {
        expect(result.id).toBeDefined();
        expect(result.status).toBe(InterventionStatus.IN_PROGRESS);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001/interventions') && r.method === 'POST'
      );
      req.flush(mockIntervention);
    });

    it('should retrieve interventions for care plan', (done) => {
      const mockResponse: PaginatedResponse<CarePlanIntervention> = {
        content: [mockIntervention],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service.getInterventionsForCarePlan('plan-001', 0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001/interventions')
      );
      req.flush(mockResponse);
    });

    it('should update intervention', (done) => {
      const updated = { ...mockIntervention, status: InterventionStatus.COMPLETED };
      service.updateIntervention('int-001', updated).subscribe((result) => {
        expect(result.status).toBe(InterventionStatus.COMPLETED);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('interventions/int-001') && r.method === 'PUT'
      );
      req.flush(updated);
    });

    it('should complete intervention', (done) => {
      service.completeIntervention('int-001').subscribe((result) => {
        expect(result.status).toBe(InterventionStatus.COMPLETED);
        expect(result.completionDate).toBeDefined();
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('interventions/int-001/complete')
      );
      req.flush({ ...mockIntervention, status: InterventionStatus.COMPLETED, completionDate: new Date() });
    });

    it('should get pending interventions', (done) => {
      const mockResponse: PaginatedResponse<CarePlanIntervention> = {
        content: [mockIntervention],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service.getPendingInterventions(0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('interventions/pending')
      );
      req.flush(mockResponse);
    });
  });

  // ==================== Team Management Tests ====================

  describe('Care Team Management', () => {
    it('should add team member to care plan', (done) => {
      service.addTeamMember('plan-001', mockTeamMember).subscribe((result) => {
        expect(result.id).toBeDefined();
        expect(result.role).toBe(TeamMemberRole.PRIMARY_CARE_PHYSICIAN);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001/team') && r.method === 'POST'
      );
      req.flush(mockTeamMember);
    });

    it('should get team members for care plan', (done) => {
      const mockResponse: PaginatedResponse<CarePlanTeamMember> = {
        content: [mockTeamMember],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service.getTeamMembersForCarePlan('plan-001', 0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        expect(result.content[0].isPrimaryCoordinator).toBe(true);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001/team')
      );
      req.flush(mockResponse);
    });

    it('should remove team member from care plan', (done) => {
      service.removeTeamMember('plan-001', 'team-001').subscribe((result) => {
        expect(result).toBeTruthy();
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001/team/team-001') && r.method === 'DELETE'
      );
      req.flush({});
    });
  });

  // ==================== Patient Engagement Tests ====================

  describe('Patient Engagement Tracking', () => {
    it('should record patient engagement', (done) => {
      service.recordPatientEngagement('plan-001', mockEngagement).subscribe((result) => {
        expect(result.id).toBeDefined();
        expect(result.engagementLevel).toBe(PatientEngagementLevel.HIGHLY_ENGAGED);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001/engagement') && r.method === 'POST'
      );
      req.flush(mockEngagement);
    });

    it('should get patient engagement for care plan', (done) => {
      service.getPatientEngagement('plan-001').subscribe((result) => {
        expect(result.engagementLevel).toBe(PatientEngagementLevel.HIGHLY_ENGAGED);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001/engagement')
      );
      req.flush(mockEngagement);
    });

    it('should update patient engagement', (done) => {
      const updated = { ...mockEngagement, engagementLevel: PatientEngagementLevel.MODERATELY_ENGAGED };
      service.updatePatientEngagement('plan-001', updated).subscribe((result) => {
        expect(result.engagementLevel).toBe(PatientEngagementLevel.MODERATELY_ENGAGED);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001/engagement') && r.method === 'PUT'
      );
      req.flush(updated);
    });
  });

  // ==================== Care Plan Review Tests ====================

  describe('Care Plan Review', () => {
    it('should create care plan review', (done) => {
      service.createCarePlanReview('plan-001', mockReview).subscribe((result) => {
        expect(result.id).toBeDefined();
        expect(result.reviewType).toBe(ReviewType.SCHEDULED);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001/reviews') && r.method === 'POST'
      );
      req.flush(mockReview);
    });

    it('should get reviews for care plan', (done) => {
      const mockResponse: PaginatedResponse<CarePlanReview> = {
        content: [mockReview],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service.getReviewsForCarePlan('plan-001', 0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001/reviews')
      );
      req.flush(mockResponse);
    });
  });

  // ==================== Care Transition Tests ====================

  describe('Care Transition Management', () => {
    it('should create care transition', (done) => {
      service.createCareTransition('plan-001', mockTransition).subscribe((result) => {
        expect(result.id).toBeDefined();
        expect(result.transitionType).toBe(TransitionType.HOSPITAL_TO_HOME);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001/transitions') && r.method === 'POST'
      );
      req.flush(mockTransition);
    });

    it('should get transitions for care plan', (done) => {
      const mockResponse: PaginatedResponse<CareTransition> = {
        content: [mockTransition],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service.getTransitionsForCarePlan('plan-001', 0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001/transitions')
      );
      req.flush(mockResponse);
    });

    it('should update care transition', (done) => {
      const updated = { ...mockTransition, status: 'COMPLETED' as const };
      service.updateCareTransition('trans-001', updated).subscribe((result) => {
        expect(result.status).toBe('COMPLETED');
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('transitions/trans-001') && r.method === 'PUT'
      );
      req.flush(updated);
    });
  });

  // ==================== Metrics Tests ====================

  describe('Care Plan Metrics', () => {
    it('should get care plan metrics for patient', (done) => {
      const mockMetrics: CarePlanMetrics = {
        patientId,
        totalCarePlans: 3,
        activeCarePlans: 1,
        completedCarePlans: 2,
        totalGoals: 5,
        achievedGoals: 3,
        inProgressGoals: 2,
        goalCompletionRate: 60,
        interventionCompletionRate: 85,
        patientEngagementScore: 85,
        teamEngagementScore: 90,
      };

      service.getCarePlanMetrics(patientId).subscribe((result) => {
        expect(result.goalCompletionRate).toBe(60);
        expect(result.interventionCompletionRate).toBe(85);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('metrics/patient') && r.url.includes(patientId)
      );
      req.flush(mockMetrics);
    });
  });

  // ==================== Error Handling Tests ====================

  describe('Error Handling', () => {
    it('should handle 404 errors gracefully', (done) => {
      service.getCarePlanById('nonexistent').subscribe(
        () => fail('should not succeed'),
        (error) => {
          expect(error.status).toBe(404);
          done();
        }
      );

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/nonexistent')
      );
      req.flush('Not found', { status: 404, statusText: 'Not Found' });
    });

    it('should handle 401 Unauthorized errors', (done) => {
      service.createCarePlan(mockCarePlan).subscribe(
        () => fail('should not succeed'),
        (error) => {
          expect(error.status).toBe(401);
          done();
        }
      );

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans') && r.method === 'POST'
      );
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });
  });

  // ==================== Caching Tests ====================

  describe('Caching Behavior', () => {
    it('should cache care plan results with TTL', (done) => {
      service.getCarePlanById('plan-001').subscribe(() => {
        // Second call should use cache
        service.getCarePlanById('plan-001').subscribe(() => {
          // Verify only one HTTP call was made
          const requests = httpMock.match((r) =>
            r.url.includes('care-plans/plan-001')
          );
          expect(requests.length).toBe(1);
          done();
        });
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001')
      );
      req.flush(mockCarePlan);
    });

    it('should invalidate cache when care plan is updated', (done) => {
      service.updateCarePlan('plan-001', mockCarePlan).subscribe(() => {
        // Cache should be invalidated
        service.getCarePlanById('plan-001').subscribe(() => {
          done();
        });

        const secondReq = httpMock.expectOne((r) =>
          r.url.includes('care-plans/plan-001') && r.method === 'GET'
        );
        secondReq.flush(mockCarePlan);
      });

      const firstReq = httpMock.expectOne((r) =>
        r.url.includes('care-plans/plan-001') && r.method === 'PUT'
      );
      firstReq.flush(mockCarePlan);
    });
  });
});
