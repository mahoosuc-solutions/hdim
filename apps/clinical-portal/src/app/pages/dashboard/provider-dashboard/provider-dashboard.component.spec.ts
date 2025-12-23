import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ProviderDashboardComponent, PendingResult, HighPriorityCareGap, QualityMeasure } from './provider-dashboard.component';
import { DialogService } from '../../../services/dialog.service';
import { NotificationService } from '../../../services/notification.service';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

/**
 * TDD Test Suite for Provider Dashboard Component
 * Phase 6.3: Provider Dashboard Workflows
 * - Result signing workflow (individual and bulk)
 * - Schedule management workflow
 */
describe('ProviderDashboardComponent', () => {
  let component: ProviderDashboardComponent;
  let fixture: ComponentFixture<ProviderDashboardComponent>;
  let mockRouter: jest.Mocked<Router>;
  let mockDialogService: jest.Mocked<DialogService>;
  let mockNotificationService: jest.Mocked<NotificationService>;

  beforeEach(async () => {
    mockRouter = {
      navigate: jest.fn()
    } as any;

    mockDialogService = {
      confirm: jest.fn(),
      open: jest.fn()
    } as any;

    mockNotificationService = {
      success: jest.fn(),
      error: jest.fn(),
      warning: jest.fn(),
      info: jest.fn()
    } as any;

    await TestBed.configureTestingModule({
      imports: [ProviderDashboardComponent, NoopAnimationsModule],
      providers: [
        { provide: Router, useValue: mockRouter },
        { provide: DialogService, useValue: mockDialogService },
        { provide: NotificationService, useValue: mockNotificationService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProviderDashboardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Result Signing Workflow', () => {
    let mockResult: PendingResult;

    beforeEach(() => {
      mockResult = {
        id: '1',
        patientName: 'Test Patient',
        patientMRN: 'MRN-001',
        resultType: 'Lab - CBC',
        date: '2025-12-01',
        abnormal: false,
        requiresReview: true
      };
    });

    it('should sign a normal result successfully', () => {
      mockDialogService.confirm.mockReturnValue(of(true));
      component.resultsToReview = 5;

      component.signResult(mockResult);

      expect(mockDialogService.confirm).toHaveBeenCalledWith(
        'Sign Result',
        expect.stringContaining('Test Patient'),
        'Sign Result',
        'Cancel',
        'primary'
      );
      expect(mockResult.requiresReview).toBe(false);
      expect(component.resultsToReview).toBe(4);
      expect(mockNotificationService.success).toHaveBeenCalledWith('Result signed for Test Patient');
    });

    it('should sign an abnormal result with warning', () => {
      mockResult.abnormal = true;
      mockDialogService.confirm.mockReturnValue(of(true));
      component.resultsToReview = 3;

      component.signResult(mockResult);

      expect(mockDialogService.confirm).toHaveBeenCalledWith(
        'Sign Result',
        expect.stringContaining('abnormal values'),
        'Sign Result',
        'Cancel',
        'warn'
      );
      expect(mockResult.requiresReview).toBe(false);
      expect(component.resultsToReview).toBe(2);
      expect(mockNotificationService.success).toHaveBeenCalledWith('Result signed for Test Patient');
    });

    it('should not sign result when user cancels', () => {
      mockDialogService.confirm.mockReturnValue(of(false));
      const initialReviewCount = component.resultsToReview;

      component.signResult(mockResult);

      expect(mockResult.requiresReview).toBe(true);
      expect(component.resultsToReview).toBe(initialReviewCount);
      expect(mockNotificationService.success).not.toHaveBeenCalled();
    });

    it('should handle bulk result signing', () => {
      const results: PendingResult[] = [
        { ...mockResult, id: '1', patientName: 'Patient 1' },
        { ...mockResult, id: '2', patientName: 'Patient 2' },
        { ...mockResult, id: '3', patientName: 'Patient 3' }
      ];
      mockDialogService.confirm.mockReturnValue(of(true));
      component.resultsToReview = 10;

      component.signMultipleResults(results);

      expect(mockDialogService.confirm).toHaveBeenCalledWith(
        'Sign Multiple Results',
        expect.stringContaining('3 results'),
        'Sign All',
        'Cancel',
        'primary'
      );
      results.forEach(result => {
        expect(result.requiresReview).toBe(false);
      });
      expect(component.resultsToReview).toBe(7);
      expect(mockNotificationService.success).toHaveBeenCalledWith('Successfully signed 3 results');
    });

    it('should warn when there are no unsigned results for bulk signing', () => {
      const results: PendingResult[] = [
        { ...mockResult, id: '1', requiresReview: false },
        { ...mockResult, id: '2', requiresReview: false },
      ];

      component.signMultipleResults(results);

      expect(mockDialogService.confirm).not.toHaveBeenCalled();
      expect(mockNotificationService.warning).toHaveBeenCalledWith('No unsigned results to sign');
    });

    it('should use warning styling when bulk signing has abnormal results', () => {
      const results: PendingResult[] = [
        { ...mockResult, id: '1', abnormal: true, requiresReview: true },
        { ...mockResult, id: '2', abnormal: false, requiresReview: true },
      ];
      mockDialogService.confirm.mockReturnValue(of(true));
      component.resultsToReview = 2;

      component.signMultipleResults(results);

      expect(mockDialogService.confirm).toHaveBeenCalledWith(
        'Sign Multiple Results',
        expect.stringContaining('2 results'),
        'Sign All',
        'Cancel',
        'warn'
      );
      expect(component.resultsToReview).toBe(0);
    });

    it('should filter unsigned results only for bulk signing', () => {
      const results: PendingResult[] = [
        { ...mockResult, id: '1', requiresReview: true },
        { ...mockResult, id: '2', requiresReview: false },
        { ...mockResult, id: '3', requiresReview: true }
      ];

      const unsignedResults = component.getUnsignedResults(results);

      expect(unsignedResults.length).toBe(2);
      expect(unsignedResults.every(r => r.requiresReview)).toBe(true);
    });

    it('should navigate to result detail when reviewing', () => {
      component.reviewResult(mockResult);

      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patient-detail', mockResult.id],
        { queryParams: { action: 'review-results', resultId: mockResult.id } }
      );
    });

    it('should not decrement results below zero when signing', () => {
      mockDialogService.confirm.mockReturnValue(of(true));
      component.resultsToReview = 0;

      component.signResult(mockResult);

      expect(component.resultsToReview).toBe(0);
    });
  });

  describe('Schedule Management Workflow', () => {
    it('should load provider schedule for today', async () => {
      const mockSchedule = await component.loadProviderSchedule(new Date());

      expect(mockSchedule).toBeDefined();
      expect(Array.isArray(mockSchedule)).toBe(true);
    });

    it('should return empty schedule for non-today dates', async () => {
      const schedule = await component.loadProviderSchedule(new Date('2024-01-01'));

      expect(schedule).toEqual([]);
    });

    it('should view today schedule with correct date', () => {
      component.viewTodaySchedule();

      const today = new Date().toISOString().split('T')[0];
      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/dashboard'],
        { queryParams: { view: 'schedule', date: today } }
      );
    });

    it('should view schedule for specific date', () => {
      const specificDate = new Date('2025-12-15');
      component.viewScheduleForDate(specificDate);

      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/dashboard'],
        { queryParams: { view: 'schedule', date: '2025-12-15' } }
      );
    });

    it('should navigate to schedule management view', () => {
      component.manageSchedule();

      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/dashboard'],
        { queryParams: { view: 'schedule-management' } }
      );
    });

    it('should filter appointments by date range', () => {
      const startDate = new Date('2025-12-01');
      const endDate = new Date('2025-12-02');

      jest.spyOn(component, 'loadProviderSchedule').mockImplementation((date: Date) => ([
        {
          id: `appt-${date.toISOString().split('T')[0]}`,
          patientName: 'Test Patient',
          patientMRN: 'MRN-001',
          startTime: '09:00',
          endTime: '09:30',
          date: date.toISOString().split('T')[0],
          type: 'Follow-up',
          status: 'scheduled'
        }
      ]));

      const appointments = component.getAppointmentsByDateRange(startDate, endDate);

      expect(appointments).toBeDefined();
      expect(Array.isArray(appointments)).toBe(true);
      expect(appointments.length).toBe(2);
    });

    it('should get upcoming appointments count', () => {
      jest.spyOn(component, 'loadProviderSchedule').mockReturnValue([
        {
          id: '1',
          patientName: 'Test Patient',
          patientMRN: 'MRN-001',
          startTime: '09:00',
          endTime: '09:30',
          date: '2025-12-03',
          type: 'Follow-up',
          status: 'scheduled'
        }
      ]);

      const count = component.getUpcomingAppointmentsCount();

      expect(typeof count).toBe('number');
      expect(count).toBeGreaterThanOrEqual(0);
    });

    it('should handle schedule conflicts', () => {
      jest.spyOn(component, 'loadProviderSchedule').mockReturnValue([
        {
          id: '1',
          patientName: 'Existing Patient',
          patientMRN: 'MRN-999',
          startTime: '09:00',
          endTime: '09:30',
          date: '2025-12-03',
          type: 'Follow-up',
          status: 'scheduled'
        }
      ]);

      const appointment = {
        id: '2',
        patientName: 'Test Patient',
        startTime: '09:00',
        endTime: '09:30',
        date: '2025-12-03'
      };

      const hasConflict = component.checkScheduleConflict(appointment);

      expect(typeof hasConflict).toBe('boolean');
      expect(hasConflict).toBe(true); // Should conflict with existing appointment
    });

    it('should return false when appointment data is incomplete', () => {
      const hasConflict = component.checkScheduleConflict({ id: '1' });

      expect(hasConflict).toBe(false);
    });

    it('should block time slot for provider', () => {
      mockDialogService.confirm.mockReturnValue(of(true));

      const timeSlot = {
        date: '2025-12-10',
        startTime: '14:00',
        endTime: '15:00',
        reason: 'Administrative time'
      };

      component.blockTimeSlot(timeSlot);

      expect(mockDialogService.confirm).toHaveBeenCalledWith(
        'Block Time Slot',
        expect.stringContaining(timeSlot.reason),
        'Block',
        'Cancel',
        'primary'
      );
      expect(mockNotificationService.success).toHaveBeenCalledWith('Time slot blocked successfully');
    });

    it('should not block time slot when cancelled', () => {
      mockDialogService.confirm.mockReturnValue(of(false));

      component.blockTimeSlot({
        date: '2025-12-10',
        startTime: '14:00',
        endTime: '15:00',
        reason: 'Admin'
      });

      expect(mockNotificationService.success).not.toHaveBeenCalled();
    });

    it('should cancel blocked time slot', () => {
      mockDialogService.confirm.mockReturnValue(of(true));

      const blockId = 'block-123';
      component.cancelBlockedSlot(blockId);

      expect(mockDialogService.confirm).toHaveBeenCalled();
      expect(mockNotificationService.success).toHaveBeenCalledWith('Blocked time slot cancelled');
    });

    it('should keep blocked time slot when cancellation declined', () => {
      mockDialogService.confirm.mockReturnValue(of(false));

      component.cancelBlockedSlot('block-456');

      expect(mockNotificationService.success).not.toHaveBeenCalled();
    });
  });

  describe('Dashboard Data Loading', () => {
    it('should populate data after dashboard load', () => {
      jest.useFakeTimers();

      (component as any).loadDashboardData();
      jest.advanceTimersByTime(500);

      expect(component.highPriorityCareGaps.length).toBeGreaterThan(0);
      expect(component.qualityMeasures.length).toBeGreaterThan(0);
      expect(component.pendingResults.length).toBeGreaterThan(0);
      expect(component.loading).toBe(false);

      jest.useRealTimers();
    });

    it('should load all dashboard data on init', () => {
      jest.spyOn<any, any>(component, 'loadHighPriorityCareGaps');
      jest.spyOn<any, any>(component, 'loadQualityMeasures');
      jest.spyOn<any, any>(component, 'loadPendingResults');
      jest.spyOn<any, any>(component, 'loadMetrics');

      component.ngOnInit();

      expect(component['loadHighPriorityCareGaps']).toHaveBeenCalled();
      expect(component['loadQualityMeasures']).toHaveBeenCalled();
      expect(component['loadPendingResults']).toHaveBeenCalled();
      expect(component['loadMetrics']).toHaveBeenCalled();
    });

    it('should refresh dashboard data', () => {
      jest.spyOn<any, any>(component, 'loadDashboardData');

      component.refreshData();

      expect(component['loadDashboardData']).toHaveBeenCalled();
    });
  });

  describe('Care Gap Management', () => {
    it('should navigate to patient detail when addressing care gap', () => {
      const gap: HighPriorityCareGap = {
        id: '1',
        patientName: 'Test Patient',
        patientMRN: 'MRN-001',
        gapType: 'Diabetes Control',
        clinicalContext: 'HbA1c 9.2%',
        risk: 'critical',
        dueDate: '2025-12-01',
        requiresAction: 'Medication adjustment'
      };

      component.addressCareGap(gap);

      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patient-detail', gap.id],
        { queryParams: { action: 'clinical-review', gapId: gap.id } }
      );
    });

    it('should navigate to patient detail by id', () => {
      component.viewPatient('patient-123');

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/patient-detail', 'patient-123']);
    });
  });

  describe('Quality Measures', () => {
    it('should navigate to measure details', () => {
      const measure: QualityMeasure = {
        id: '1',
        name: 'Diabetes HbA1c Control',
        performance: 78.5,
        target: 80.0,
        numerator: 157,
        denominator: 200,
        trend: 'up'
      };

      component.viewMeasureDetails(measure);

      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/results'],
        { queryParams: { measureId: measure.id } }
      );
    });

    it('should return correct risk colors', () => {
      expect(component.getRiskColor('critical')).toBe('#d32f2f');
      expect(component.getRiskColor('high')).toBe('#f57c00');
      expect(component.getRiskColor('moderate')).toBe('#fbc02d');
      expect(component.getRiskColor('unknown')).toBe('#757575');
    });

    it('should return correct risk icons', () => {
      expect(component.getRiskIcon('critical')).toBe('error');
      expect(component.getRiskIcon('high')).toBe('warning');
      expect(component.getRiskIcon('moderate')).toBe('info');
      expect(component.getRiskIcon('other')).toBe('help');
    });

    it('should return correct performance colors', () => {
      expect(component.getPerformanceColor(80, 80)).toBe('#4caf50'); // At target
      expect(component.getPerformanceColor(75, 80)).toBe('#ff9800'); // Above 90% of target
      expect(component.getPerformanceColor(60, 80)).toBe('#f44336'); // Below 90% of target
    });

    it('should return correct trend icons and colors', () => {
      expect(component.getTrendIcon('up')).toBe('trending_up');
      expect(component.getTrendIcon('down')).toBe('trending_down');
      expect(component.getTrendIcon('stable')).toBe('trending_flat');
      expect(component.getTrendIcon('other')).toBe('remove');

      expect(component.getTrendColor('up')).toBe('#4caf50');
      expect(component.getTrendColor('down')).toBe('#f44336');
      expect(component.getTrendColor('stable')).toBe('#757575');
      expect(component.getTrendColor('other')).toBe('#757575');
    });
  });

  describe('Dashboard Navigation', () => {
    it('should navigate to all results', () => {
      component.viewAllResults();

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/results']);
    });

    it('should navigate to all evaluations', () => {
      component.viewAllEvaluations();

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/evaluations']);
    });

    it('should navigate to all patients', () => {
      component.viewAllPatients();

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/patients']);
    });

    it('should navigate to compliant patients', () => {
      component.viewCompliantPatients();

      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients'],
        { queryParams: { compliance: 'compliant' } }
      );
    });

    it('should navigate to non-compliant patients', () => {
      component.viewNonCompliantPatients();

      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients'],
        { queryParams: { compliance: 'non-compliant' } }
      );
    });

    it('should navigate to recent evaluations', () => {
      jest.useFakeTimers();
      jest.setSystemTime(new Date('2025-12-15T12:00:00Z'));

      component.viewRecentEvaluations();

      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/evaluations'],
        { queryParams: { startDate: '2025-11-15' } }
      );

      jest.useRealTimers();
    });

    it('should navigate to high urgency care gaps', () => {
      component.viewAllCareGaps();

      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients'],
        { queryParams: { filter: 'care-gaps', urgency: 'high' } }
      );
    });

    it('should navigate to reports', () => {
      component.viewReports();

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/reports']);
    });
  });

  describe('Component Cleanup', () => {
    it('should complete destroy$ subject on destroy', () => {
      jest.spyOn(component['destroy$'], 'next');
      jest.spyOn(component['destroy$'], 'complete');

      component.ngOnDestroy();

      expect(component['destroy$'].next).toHaveBeenCalled();
      expect(component['destroy$'].complete).toHaveBeenCalled();
    });
  });
});
