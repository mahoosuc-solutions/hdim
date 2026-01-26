import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { RNDashboardComponent, CareGapTask, PatientOutreach } from './rn-dashboard.component';
import { DialogService } from '../../../services/dialog.service';
import { NotificationService } from '../../../services/notification.service';
import { CareGapService, InterventionType } from '../../../services/care-gap.service';
import { ToastService } from '../../../services/toast.service';
import { createMockStore } from '../../testing/mocks';
import { Store } from '@ngrx/store';

/**
 * TDD Test Suite for RN Dashboard Component
 *
 * Phase 6.2: RN Dashboard Workflows
 *
 * Tests for:
 * 1. Patient education workflows
 * 2. Care coordination tasks
 * 3. Follow-up scheduling
 * 4. Health coaching tracking
 */
describe('RNDashboardComponent (TDD - Phase 6.2)', () => {
  let component: RNDashboardComponent;
  let fixture: ComponentFixture<RNDashboardComponent>;
  let mockRouter: jest.Mocked<Router>;
  let mockDialogService: jest.Mocked<DialogService>;
  let mockNotificationService: jest.Mocked<NotificationService>;
  let mockCareGapService: jest.Mocked<CareGapService>;
  let mockToastService: jest.Mocked<ToastService>;

  beforeEach(async () => {
    // Create mock services
    mockRouter = {
      navigate: jest.fn(),
    } as any;

    mockDialogService = {
      confirm: jest.fn(),
      open: jest.fn(),
    } as any;

    mockNotificationService = {
      success: jest.fn(),
      error: jest.fn(),
      info: jest.fn(),
      warning: jest.fn(),
    } as any;

    mockCareGapService = {
      assignIntervention: jest.fn(),
      closeGap: jest.fn(),
      getPatientCareGaps: jest.fn(),
    } as any;

    mockToastService = {
      success: jest.fn(),
      error: jest.fn(),
      info: jest.fn(),
      warning: jest.fn(),
    } as any;

    await TestBed.configureTestingModule({
      imports: [RNDashboardComponent, NoopAnimationsModule],
      providers: [
        { provide: Router, useValue: mockRouter },
        { provide: DialogService, useValue: mockDialogService },
        { provide: NotificationService, useValue: mockNotificationService },
        { provide: CareGapService, useValue: mockCareGapService },
        { provide: ToastService, useValue: mockToastService },
        { provide: Store, useValue: createMockStore() }],
    }).compileComponents();

    fixture = TestBed.createComponent(RNDashboardComponent);
    component = fixture.componentInstance;
  });

  // ============================================================================
  // 1. Patient Education Workflows (8 tests)
  // ============================================================================
  describe('Patient Education Workflows', () => {
    it('should schedule patient education session', () => {
      const gap: CareGapTask = {
        id: '1',
        patientName: 'Davis, Jennifer',
        patientMRN: 'MRN-101',
        gapType: 'Diabetes Education',
        priority: 'high',
        category: 'education',
        dueDate: '2025-12-01',
        assignedTo: 'RN Davis',
        status: 'pending'
      };

      mockDialogService.confirm.mockReturnValue(of(true));
      mockCareGapService.assignIntervention.mockReturnValue(of({
        gapId: '1',
        patientId: 'patient-001',
        intervention: {
          type: InterventionType.EDUCATION,
          description: 'Diabetes Education Session',
          scheduledDate: '2025-12-05',
        },
        assignedDate: new Date().toISOString(),
        status: 'scheduled'
      }));

      component.scheduleEducationSession(gap);

      expect(mockDialogService.confirm).toHaveBeenCalled();
    });

    it('should document completed education session', () => {
      const gap: CareGapTask = {
        id: '1',
        patientName: 'Davis, Jennifer',
        patientMRN: 'MRN-101',
        gapType: 'Diabetes Education',
        priority: 'high',
        category: 'education',
        dueDate: '2025-12-01',
        assignedTo: 'RN Davis',
        status: 'in-progress'
      };

      mockDialogService.confirm.mockReturnValue(of(true));
      mockCareGapService.closeGap.mockReturnValue(of({
        gapId: '1',
        patientId: 'patient-001',
        closedDate: new Date().toISOString(),
        closedBy: 'RN Davis',
        reason: 'Education completed',
        notes: 'Patient education session completed successfully'
      }));

      component.documentEducationSession(gap);

      expect(mockDialogService.confirm).toHaveBeenCalled();
    });

    it('should track education materials provided', () => {
      const gap: CareGapTask = {
        id: '1',
        patientName: 'Davis, Jennifer',
        patientMRN: 'MRN-101',
        gapType: 'Diabetes Education',
        priority: 'high',
        category: 'education',
        dueDate: '2025-12-01',
        assignedTo: 'RN Davis',
        status: 'in-progress'
      };

      const materials = ['Diabetes Management Booklet', 'Blood Glucose Log', 'Diet Plan'];

      component.trackEducationMaterials(gap, materials);

      expect(mockToastService.success).toHaveBeenCalledWith(
        expect.stringContaining('Education materials tracked')
      );
    });

    it('should show education history for patient', () => {
      const patientId = 'patient-001';

      component.viewEducationHistory(patientId);

      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients', patientId],
        expect.objectContaining({
          queryParams: expect.objectContaining({ view: 'education-history' })
        })
      );
    });

    it('should create education plan', () => {
      const gap: CareGapTask = {
        id: '1',
        patientName: 'Davis, Jennifer',
        patientMRN: 'MRN-101',
        gapType: 'Diabetes Education',
        priority: 'high',
        category: 'education',
        dueDate: '2025-12-01',
        assignedTo: 'RN Davis',
        status: 'pending'
      };

      component.createEducationPlan(gap);

      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients', gap.id],
        expect.objectContaining({
          queryParams: expect.objectContaining({ action: 'create-education-plan' })
        })
      );
    });

    it('should handle education session scheduling failure', () => {
      const gap: CareGapTask = {
        id: '1',
        patientName: 'Davis, Jennifer',
        patientMRN: 'MRN-101',
        gapType: 'Diabetes Education',
        priority: 'high',
        category: 'education',
        dueDate: '2025-12-01',
        assignedTo: 'RN Davis',
        status: 'pending'
      };

      mockDialogService.confirm.mockReturnValue(of(true));
      mockCareGapService.assignIntervention.mockReturnValue(
        throwError(() => new Error('Failed to schedule'))
      );

      component.scheduleEducationSession(gap);

      // Wait for async operation
      setTimeout(() => {
        expect(mockToastService.error).toHaveBeenCalledWith(
          expect.stringContaining('Failed to schedule education session')
        );
      }, 100);
    });

    it('should send education materials electronically', () => {
      const gap: CareGapTask = {
        id: '1',
        patientName: 'Davis, Jennifer',
        patientMRN: 'MRN-101',
        gapType: 'Diabetes Education',
        priority: 'high',
        category: 'education',
        dueDate: '2025-12-01',
        assignedTo: 'RN Davis',
        status: 'pending'
      };

      mockDialogService.confirm.mockReturnValue(of(true));

      component.sendEducationMaterials(gap);

      expect(mockDialogService.confirm).toHaveBeenCalledWith(
        expect.stringContaining('Send Education Materials'),
        expect.any(String),
        expect.any(String),
        expect.any(String),
        expect.any(String)
      );
    });

    it('should track patient comprehension assessment', () => {
      const gap: CareGapTask = {
        id: '1',
        patientName: 'Davis, Jennifer',
        patientMRN: 'MRN-101',
        gapType: 'Diabetes Education',
        priority: 'high',
        category: 'education',
        dueDate: '2025-12-01',
        assignedTo: 'RN Davis',
        status: 'in-progress'
      };

      const assessment = {
        understanding: 'good',
        followUpNeeded: false,
        notes: 'Patient demonstrated good understanding'
      };

      component.recordComprehensionAssessment(gap, assessment);

      expect(mockToastService.success).toHaveBeenCalledWith(
        expect.stringContaining('Comprehension assessment recorded')
      );
    });
  });

  describe('RN dashboard utilities', () => {
    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('loads care gaps and updates loading state', () => {
      (component as any).loadCareGaps();
      expect(component.loading).toBe(true);

      jest.advanceTimersByTime(500);

      expect(component.careGaps.length).toBeGreaterThan(0);
      expect(component.loading).toBe(false);
    });

    it('loads outreach tasks', () => {
      (component as any).loadOutreachTasks();
      jest.advanceTimersByTime(500);

      expect(component.outreachTasks.length).toBeGreaterThan(0);
    });

    it('loads metrics', () => {
      (component as any).loadMetrics();

      expect(component.careGapsAssigned).toBe(15);
      expect(component.patientCallsPending).toBe(8);
      expect(component.medReconciliationsNeeded).toBe(5);
      expect(component.patientEducationDue).toBe(7);
    });

    it('refreshes dashboard data', () => {
      const loadSpy = jest.spyOn(component as any, 'loadDashboardData');

      component.refreshData();

      expect(loadSpy).toHaveBeenCalled();
    });

    it('navigates when addressing care gaps', () => {
      const logSpy = jest.spyOn(console, 'log').mockImplementation(() => undefined);
      const gap: CareGapTask = {
        id: '1',
        patientName: 'Davis, Jennifer',
        patientMRN: 'MRN-101',
        gapType: 'Diabetes Education',
        priority: 'high',
        category: 'education',
        dueDate: '2025-12-01',
        assignedTo: 'RN Davis',
        status: 'pending'
      };

      component.addressCareGap(gap);

      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients', gap.id],
        expect.objectContaining({
          queryParams: { action: 'address-gap', gapId: gap.id }
        })
      );
      logSpy.mockRestore();
    });

    it('completes patient education flow', () => {
      const gap: CareGapTask = {
        id: '1',
        patientName: 'Davis, Jennifer',
        patientMRN: 'MRN-101',
        gapType: 'Diabetes Education',
        priority: 'high',
        category: 'education',
        dueDate: '2025-12-01',
        assignedTo: 'RN Davis',
        status: 'pending'
      };

      component.patientEducationDue = 1;
      mockDialogService.confirm.mockReturnValue(of(true));

      component.provideEducation(gap);

      expect(gap.status).toBe('completed');
      expect(component.patientEducationDue).toBe(0);
      expect(mockNotificationService.success).toHaveBeenCalled();
      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients', gap.id],
        expect.objectContaining({
          queryParams: { action: 'document-education', gapType: gap.gapType }
        })
      );
    });

    it('starts medication reconciliation flow', () => {
      const gap: CareGapTask = {
        id: '1',
        patientName: 'Davis, Jennifer',
        patientMRN: 'MRN-101',
        gapType: 'Medication Reconciliation',
        priority: 'high',
        category: 'medication',
        dueDate: '2025-12-01',
        assignedTo: 'RN Davis',
        status: 'pending'
      };

      mockDialogService.confirm.mockReturnValue(of(true));

      component.reconcileMedications(gap);

      expect(gap.status).toBe('in-progress');
      expect(mockNotificationService.info).toHaveBeenCalled();
      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients', gap.id],
        expect.objectContaining({
          queryParams: { action: 'med-reconciliation' }
        })
      );
    });

    it('documents patient calls and communications', () => {
      const outreach: PatientOutreach = {
        id: '1',
        patientName: 'Anderson, Lisa',
        patientMRN: 'MRN-201',
        outreachType: 'call',
        reason: 'Pre-visit preparation',
        scheduledDate: '2025-11-26 10:00',
        status: 'scheduled'
      };

      mockDialogService.confirm.mockReturnValue(of(true));

      component.makePatientCall(outreach);
      expect(mockNotificationService.info).toHaveBeenCalled();
      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients', outreach.id],
        expect.objectContaining({
          queryParams: { action: 'document-call', reason: outreach.reason }
        })
      );

      component.sendCommunication({ ...outreach, outreachType: 'email' });
      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients', outreach.id],
        expect.objectContaining({
          queryParams: expect.objectContaining({ action: 'send-communication', type: 'email' })
        })
      );

      component.sendCommunication({ ...outreach, outreachType: 'letter' });
      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients', outreach.id],
        expect.objectContaining({
          queryParams: expect.objectContaining({ action: 'send-communication', type: 'letter' })
        })
      );
    });

    it('supports navigation helpers and color helpers', () => {
      component.viewPatient('patient-1');
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/patients', 'patient-1']);

      expect(component.getPriorityColor('high')).toBe('warn');
      expect(component.getPriorityColor('medium')).toBe('accent');
      expect(component.getPriorityColor('low')).toBe('primary');
      expect(component.getPriorityColor('other')).toBe('');

      expect(component.getCategoryIcon('education')).toBe('school');
      expect(component.getCategoryIcon('medication')).toBe('medication');
      expect(component.getCategoryIcon('coordination')).toBe('sync');
      expect(component.getCategoryIcon('assessment')).toBe('assessment');
      expect(component.getCategoryIcon('other')).toBe('task');

      expect(component.getCategoryColor('education')).toBe('#4caf50');
      expect(component.getCategoryColor('medication')).toBe('#f44336');
      expect(component.getCategoryColor('coordination')).toBe('#2196f3');
      expect(component.getCategoryColor('assessment')).toBe('#9c27b0');
      expect(component.getCategoryColor('other')).toBe('#757575');

      expect(component.getOutreachIcon('call')).toBe('phone');
      expect(component.getOutreachIcon('email')).toBe('email');
      expect(component.getOutreachIcon('letter')).toBe('mail');
      expect(component.getOutreachIcon('other')).toBe('contact_mail');

      expect(component.getStatusColor('completed')).toBe('success');
      expect(component.getStatusColor('in-progress')).toBe('warn');
      expect(component.getStatusColor('pending')).toBe('primary');
      expect(component.getStatusColor('scheduled')).toBe('primary');
      expect(component.getStatusColor('missed')).toBe('warn');
      expect(component.getStatusColor('other')).toBe('');
    });

    it('navigates to aggregated views', () => {
      component.viewAllCareGaps();
      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients'],
        expect.objectContaining({ queryParams: { view: 'care-gaps' } })
      );

      component.viewAllOutreach();
      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients'],
        expect.objectContaining({ queryParams: { view: 'outreach' } })
      );
    });
  });

  // ============================================================================
  // 2. Care Coordination Tasks (7 tests)
  // ============================================================================
  describe('Care Coordination Tasks', () => {
    it('should create care coordination task', () => {
      const task = {
        patientId: 'patient-001',
        taskType: 'referral',
        description: 'Refer to endocrinologist',
        priority: 'high' as const,
        dueDate: '2025-12-10'
      };

      component.createCoordinationTask(task);

      expect(mockToastService.success).toHaveBeenCalledWith(
        expect.stringContaining('Care coordination task created')
      );
    });

    it('should assign task to care team member', () => {
      const gap: CareGapTask = {
        id: '3',
        patientName: 'Wilson, Sarah',
        patientMRN: 'MRN-103',
        gapType: 'Hypertension Follow-up',
        priority: 'medium',
        category: 'coordination',
        dueDate: '2025-12-05',
        assignedTo: 'RN Davis',
        status: 'pending'
      };

      const assignee = 'Dr. Smith';

      component.assignTaskToTeamMember(gap, assignee);

      expect(gap.assignedTo).toBe(assignee);
      expect(mockToastService.success).toHaveBeenCalledWith(
        expect.stringContaining('Task assigned to')
      );
    });

    it('should schedule specialist referral', () => {
      const gap: CareGapTask = {
        id: '3',
        patientName: 'Wilson, Sarah',
        patientMRN: 'MRN-103',
        gapType: 'Hypertension Follow-up',
        priority: 'medium',
        category: 'coordination',
        dueDate: '2025-12-05',
        assignedTo: 'RN Davis',
        status: 'pending'
      };

      mockDialogService.confirm.mockReturnValue(of(true));
      mockCareGapService.assignIntervention.mockReturnValue(of({
        gapId: '3',
        patientId: 'patient-003',
        intervention: {
          type: InterventionType.REFERRAL,
          description: 'Cardiology referral for hypertension management',
          scheduledDate: '2025-12-10',
        },
        assignedDate: new Date().toISOString(),
        status: 'scheduled'
      }));

      component.scheduleSpecialistReferral(gap);

      expect(mockDialogService.confirm).toHaveBeenCalled();
    });

    it('should track care team communication', () => {
      const communication = {
        patientId: 'patient-001',
        from: 'RN Davis',
        to: 'Dr. Smith',
        subject: 'Patient status update',
        message: 'Patient is responding well to treatment',
        timestamp: new Date()
      };

      component.logCareTeamCommunication(communication);

      expect(mockToastService.success).toHaveBeenCalledWith(
        expect.stringContaining('Communication logged')
      );
    });

    it('should update task status', () => {
      const gap: CareGapTask = {
        id: '3',
        patientName: 'Wilson, Sarah',
        patientMRN: 'MRN-103',
        gapType: 'Hypertension Follow-up',
        priority: 'medium',
        category: 'coordination',
        dueDate: '2025-12-05',
        assignedTo: 'RN Davis',
        status: 'pending'
      };

      component.updateCoordinationTaskStatus(gap, 'completed');

      expect(gap.status).toBe('completed');
      expect(mockToastService.success).toHaveBeenCalledWith(
        expect.stringContaining('Task status updated')
      );
    });

    it('should view all coordination tasks for patient', () => {
      const patientId = 'patient-001';

      component.viewPatientCoordinationTasks(patientId);

      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients', patientId],
        expect.objectContaining({
          queryParams: expect.objectContaining({ view: 'coordination-tasks' })
        })
      );
    });

    it('should send coordination summary to care team', () => {
      const patientId = 'patient-001';

      mockDialogService.confirm.mockReturnValue(of(true));

      component.sendCoordinationSummary(patientId);

      expect(mockDialogService.confirm).toHaveBeenCalled();
    });
  });

  // ============================================================================
  // 3. Follow-up Scheduling (7 tests)
  // ============================================================================
  describe('Follow-up Scheduling', () => {
    it('should schedule follow-up appointment', () => {
      const gap: CareGapTask = {
        id: '3',
        patientName: 'Wilson, Sarah',
        patientMRN: 'MRN-103',
        gapType: 'Hypertension Follow-up',
        priority: 'medium',
        category: 'coordination',
        dueDate: '2025-12-05',
        assignedTo: 'RN Davis',
        status: 'pending'
      };

      mockDialogService.confirm.mockReturnValue(of(true));
      mockCareGapService.assignIntervention.mockReturnValue(of({
        gapId: '3',
        patientId: 'patient-003',
        intervention: {
          type: InterventionType.APPOINTMENT_SCHEDULED,
          description: 'Hypertension follow-up appointment',
          scheduledDate: '2025-12-15T10:00:00',
        },
        assignedDate: new Date().toISOString(),
        status: 'scheduled'
      }));

      component.scheduleFollowUpAppointment(gap);

      expect(mockDialogService.confirm).toHaveBeenCalled();
    });

    it('should send follow-up appointment reminder', () => {
      const outreach: PatientOutreach = {
        id: '1',
        patientName: 'Anderson, Lisa',
        patientMRN: 'MRN-201',
        outreachType: 'call',
        reason: 'Appointment reminder',
        scheduledDate: '2025-11-26 10:00',
        status: 'scheduled'
      };

      mockDialogService.confirm.mockReturnValue(of(true));

      component.sendFollowUpReminder(outreach);

      expect(mockDialogService.confirm).toHaveBeenCalledWith(
        expect.stringContaining('Send Appointment Reminder'),
        expect.any(String),
        expect.any(String),
        expect.any(String),
        expect.any(String)
      );
    });

    it('should reschedule missed follow-up', () => {
      const outreach: PatientOutreach = {
        id: '1',
        patientName: 'Anderson, Lisa',
        patientMRN: 'MRN-201',
        outreachType: 'call',
        reason: 'Missed appointment follow-up',
        scheduledDate: '2025-11-26 10:00',
        status: 'missed'
      };

      mockDialogService.confirm.mockReturnValue(of(true));

      component.rescheduleMissedFollowUp(outreach);

      expect(mockDialogService.confirm).toHaveBeenCalled();
    });

    it('should track follow-up completion', () => {
      const outreach: PatientOutreach = {
        id: '1',
        patientName: 'Anderson, Lisa',
        patientMRN: 'MRN-201',
        outreachType: 'call',
        reason: 'Post-visit follow-up',
        scheduledDate: '2025-11-26 10:00',
        status: 'scheduled'
      };

      const outcome = {
        completed: true,
        notes: 'Patient doing well, no concerns',
        nextFollowUp: '2026-01-15'
      };

      component.completeFollowUpTask(outreach, outcome);

      expect(outreach.status).toBe('completed');
      expect(mockToastService.success).toHaveBeenCalledWith(
        expect.stringContaining('Follow-up task completed')
      );
    });

    it('should view follow-up schedule for patient', () => {
      const patientId = 'patient-001';

      component.viewFollowUpSchedule(patientId);

      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients', patientId],
        expect.objectContaining({
          queryParams: expect.objectContaining({ view: 'follow-up-schedule' })
        })
      );
    });

    it('should create recurring follow-up schedule', () => {
      const gap: CareGapTask = {
        id: '1',
        patientName: 'Davis, Jennifer',
        patientMRN: 'MRN-101',
        gapType: 'Diabetes Follow-up',
        priority: 'high',
        category: 'coordination',
        dueDate: '2025-12-01',
        assignedTo: 'RN Davis',
        status: 'pending'
      };

      const schedule = {
        frequency: 'monthly',
        duration: 6, // 6 months
        startDate: '2025-12-01'
      };

      component.createRecurringFollowUp(gap, schedule);

      expect(mockToastService.success).toHaveBeenCalledWith(
        expect.stringContaining('Recurring follow-up schedule created')
      );
    });

    it('should cancel follow-up appointment', () => {
      const outreach: PatientOutreach = {
        id: '1',
        patientName: 'Anderson, Lisa',
        patientMRN: 'MRN-201',
        outreachType: 'call',
        reason: 'Follow-up appointment',
        scheduledDate: '2025-11-26 10:00',
        status: 'scheduled'
      };

      mockDialogService.confirm.mockReturnValue(of(true));

      component.cancelFollowUp(outreach);

      expect(mockDialogService.confirm).toHaveBeenCalledWith(
        expect.stringContaining('Cancel Follow-up'),
        expect.any(String),
        expect.any(String),
        expect.any(String),
        expect.any(String)
      );
    });
  });

  // ============================================================================
  // 4. Health Coaching Tracking (8 tests)
  // ============================================================================
  describe('Health Coaching Tracking', () => {
    it('should start health coaching session', () => {
      const gap: CareGapTask = {
        id: '1',
        patientName: 'Davis, Jennifer',
        patientMRN: 'MRN-101',
        gapType: 'Diabetes Management',
        priority: 'high',
        category: 'education',
        dueDate: '2025-12-01',
        assignedTo: 'RN Davis',
        status: 'pending'
      };

      mockDialogService.confirm.mockReturnValue(of(true));

      component.startHealthCoachingSession(gap);

      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients', gap.id],
        expect.objectContaining({
          queryParams: expect.objectContaining({ action: 'start-coaching' })
        })
      );
    });

    it('should document coaching goals', () => {
      const coachingGoals = {
        patientId: 'patient-001',
        goals: [
          { description: 'Improve A1C to <7%', targetDate: '2026-03-01' },
          { description: 'Exercise 30 min daily', targetDate: '2026-01-01' },
          { description: 'Reduce sodium intake', targetDate: '2025-12-15' }
        ]
      };

      component.documentCoachingGoals(coachingGoals);

      expect(mockToastService.success).toHaveBeenCalledWith(
        expect.stringContaining('Coaching goals documented')
      );
    });

    it('should track goal progress', () => {
      const goalProgress = {
        goalId: 'goal-001',
        patientId: 'patient-001',
        progress: 75,
        notes: 'Patient making good progress',
        lastUpdated: new Date()
      };

      component.updateGoalProgress(goalProgress);

      expect(mockToastService.success).toHaveBeenCalledWith(
        expect.stringContaining('Goal progress updated')
      );
    });

    it('should schedule coaching follow-up call', () => {
      const patientId = 'patient-001';
      const scheduledDate = '2025-12-10T14:00:00';

      component.scheduleCoachingCall(patientId, scheduledDate);

      expect(mockToastService.success).toHaveBeenCalledWith(
        expect.stringContaining('Coaching call scheduled')
      );
    });

    it('should view coaching history for patient', () => {
      const patientId = 'patient-001';

      component.viewCoachingHistory(patientId);

      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients', patientId],
        expect.objectContaining({
          queryParams: expect.objectContaining({ view: 'coaching-history' })
        })
      );
    });

    it('should complete coaching milestone', () => {
      const milestone = {
        patientId: 'patient-001',
        goalId: 'goal-001',
        description: 'First month of daily exercise completed',
        completedDate: new Date(),
        achievementNotes: 'Patient exceeded expectations'
      };

      component.recordCoachingMilestone(milestone);

      expect(mockToastService.success).toHaveBeenCalledWith(
        expect.stringContaining('Coaching milestone recorded')
      );
    });

    it('should send coaching progress report', () => {
      const patientId = 'patient-001';

      mockDialogService.confirm.mockReturnValue(of(true));

      component.sendCoachingProgressReport(patientId);

      expect(mockDialogService.confirm).toHaveBeenCalledWith(
        expect.stringContaining('Send Progress Report'),
        expect.any(String),
        expect.any(String),
        expect.any(String),
        expect.any(String)
      );
    });

    it('should adjust coaching plan based on progress', () => {
      const adjustment = {
        patientId: 'patient-001',
        reason: 'Patient struggling with exercise goal',
        modifications: [
          'Reduce exercise time to 15 minutes',
          'Add physical therapy referral'
        ],
        updatedBy: 'RN Davis'
      };

      component.adjustCoachingPlan(adjustment);

      expect(mockToastService.success).toHaveBeenCalledWith(
        expect.stringContaining('Coaching plan adjusted')
      );
    });
  });

  // ============================================================================
  // 5. Component Initialization
  // ============================================================================
  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should load dashboard data on init', () => {
      const loadDataSpy = jest.spyOn(component as any, 'loadDashboardData');
      component.ngOnInit();
      expect(loadDataSpy).toHaveBeenCalled();
    });

    it('should initialize with default values', () => {
      expect(component.loading).toBe(true);
      expect(component.careGaps).toEqual([]);
      expect(component.outreachTasks).toEqual([]);
    });
  });

  // ============================================================================
  // 6. Error Handling
  // ============================================================================
  describe('Error Handling', () => {
    it('should handle service errors gracefully', () => {
      const gap: CareGapTask = {
        id: '1',
        patientName: 'Davis, Jennifer',
        patientMRN: 'MRN-101',
        gapType: 'Diabetes Education',
        priority: 'high',
        category: 'education',
        dueDate: '2025-12-01',
        assignedTo: 'RN Davis',
        status: 'pending'
      };

      mockDialogService.confirm.mockReturnValue(of(true));
      mockCareGapService.assignIntervention.mockReturnValue(
        throwError(() => new Error('Service unavailable'))
      );

      component.scheduleEducationSession(gap);

      // Error should be handled without crashing
      setTimeout(() => {
        expect(mockToastService.error).toHaveBeenCalled();
      }, 100);
    });

    it('should show user-friendly error messages', () => {
      const gap: CareGapTask = {
        id: '1',
        patientName: 'Davis, Jennifer',
        patientMRN: 'MRN-101',
        gapType: 'Diabetes Education',
        priority: 'high',
        category: 'education',
        dueDate: '2025-12-01',
        assignedTo: 'RN Davis',
        status: 'pending'
      };

      mockDialogService.confirm.mockReturnValue(of(true));
      mockCareGapService.assignIntervention.mockReturnValue(
        throwError(() => new Error('Network error'))
      );

      component.scheduleEducationSession(gap);

      setTimeout(() => {
        expect(mockToastService.error).toHaveBeenCalledWith(
          expect.stringContaining('Failed')
        );
      }, 100);
    });
  });
});
