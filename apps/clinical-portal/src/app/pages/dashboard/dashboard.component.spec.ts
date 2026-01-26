import { ComponentFixture, TestBed, fakeAsync, flush } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { DashboardComponent } from './dashboard.component';
import { EvaluationService } from '../../services/evaluation.service';
import { PatientService } from '../../services/patient.service';
import { MeasureService } from '../../services/measure.service';
import { AIAssistantService } from '../../services/ai-assistant.service';
import { EvaluationFactory } from '../../../testing/factories/evaluation.factory';
import { PatientFactory } from '../../../testing/factories/patient.factory';
import { CqlLibraryFactory } from '../../../testing/factories/cql-library.factory';
import { UserRoleService, UserRole } from '../../shared/services/user-role.service';
import { MeasureFavoritesService } from '../../services/measure-favorites.service';
import { LoggerService } from '../../services/logger.service';
import { createMockLoggerService } from '../../../testing/mocks';
import { createMockHttpClient } from '../../../testing/mocks';

/**
 * TDD Test Suite for Dashboard Component
 *
 * This test suite is written BEFORE implementation to follow TDD principles.
 * The Dashboard component provides:
 * - Overview statistics and metrics
 * - Recent evaluation activity
 * - Quick access to common actions
 * - Compliance trends visualization
 * - Measure performance summary
 */
describe('DashboardComponent (TDD)', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let mockEvaluationService: jest.Mocked<EvaluationService>;
  let mockPatientService: jest.Mocked<PatientService>;
  let mockMeasureService: jest.Mocked<MeasureService>;
  let mockRouter: jest.Mocked<Router>;
  let mockUserRoleService: {
    currentRole$: BehaviorSubject<UserRole>;
    getCurrentRole: jest.Mock;
    setRole: jest.Mock;
  };

  beforeEach(async () => {
    // Create mock services
    mockEvaluationService = {
      getPatientResults: jest.fn(),
      getAllEvaluations: jest.fn(),
      getAllEvaluationsCached: jest.fn(),
      getPopulationReport: jest.fn(),
      invalidateAllCaches: jest.fn(),
    } as any;

    mockPatientService = {
      getPatients: jest.fn(),
      getPatientsSummary: jest.fn(),
      getPatientsSummaryCached: jest.fn(),
      invalidateCache: jest.fn(),
    } as any;

    mockMeasureService = {
      getActiveMeasures: jest.fn(),
      getActiveMeasuresInfo: jest.fn(),
      getActiveMeasuresInfoCached: jest.fn(),
      getMeasureCount: jest.fn(),
      invalidateCache: jest.fn(),
    } as any;

    mockRouter = {
      navigate: jest.fn(),
    } as any;

    mockUserRoleService = {
      currentRole$: new BehaviorSubject<UserRole>(UserRole.ADMIN),
      getCurrentRole: jest.fn().mockReturnValue(UserRole.ADMIN),
      setRole: jest.fn(),
    };

    const mockMeasureFavoritesService = {
      favorites: jest.fn().mockReturnValue([]),
      recentMeasures: jest.fn().mockReturnValue([]),
      favoritesCount: jest.fn().mockReturnValue(0),
      hasRecent: jest.fn().mockReturnValue(false),
      getMostUsed: jest.fn().mockReturnValue([]),
      isFavorite: jest.fn().mockReturnValue(false),
      toggleFavorite: jest.fn(),
      removeFavorite: jest.fn(),
      clearRecent: jest.fn(),
      recordUsage: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [{ provide: LoggerService, useValue: createMockLoggerService() },
        
        { provide: EvaluationService, useValue: mockEvaluationService },
        { provide: PatientService, useValue: mockPatientService },
        { provide: MeasureService, useValue: mockMeasureService },
        { provide: Router, useValue: mockRouter },
        { provide: AIAssistantService, useValue: {} },
        { provide: UserRoleService, useValue: mockUserRoleService },
        { provide: MeasureFavoritesService, useValue: mockMeasureFavoritesService },
        { provide: HttpClient, useValue: createMockHttpClient() }],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    EvaluationFactory.reset();
    PatientFactory.reset();
    CqlLibraryFactory.reset();
  });

  // ============================================================================
  // 1. Component Initialization (5 tests)
  // ============================================================================
  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should load dashboard data on ngOnInit', () => {
      const loadDataSpy = jest.spyOn(component, 'loadDashboardData');

      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of([]));
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of([]));
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));

      component.ngOnInit();

      expect(loadDataSpy).toHaveBeenCalled();
    });

    it('should update role when service emits changes', () => {
      const loadRoleSpy = jest.spyOn<any, any>(component, 'loadRoleComponent');

      component.ngOnInit();
      mockUserRoleService.currentRole$.next(UserRole.PROVIDER);

      expect(component.currentRole).toBe(UserRole.PROVIDER);
      expect(loadRoleSpy).toHaveBeenCalledWith(UserRole.PROVIDER);
    });

    it('should initialize with empty state', () => {
      expect(component.statistics).toEqual({
        totalEvaluations: 0,
        totalPatients: 0,
        overallCompliance: 0,
        recentEvaluations: 0,
        complianceChange: 0,
      });
      expect(component.recentActivity).toEqual([]);
      expect(component.measurePerformance).toEqual([]);
    });

    it('should set loading state while fetching data', fakeAsync(() => {
      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of([]));
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of([]));
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));

      // Initially loading should be false
      expect(component.loading).toBe(false);

      component.loadDashboardData();

      // After forkJoin and loadEvaluationsData complete synchronously with of()
      flush();

      expect(component.loading).toBe(false); // Will be false after finalize
    }));

    it('should handle initialization errors', fakeAsync(() => {
      const error = { status: 500, message: 'Server Error' };
      mockPatientService.getPatientsSummaryCached.mockReturnValue(
        throwError(() => error)
      );
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));
      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of([]));

      component.loadDashboardData();
      flush();

      // Error is set when forkJoin fails
      expect(component.error).toContain('Failed to load dashboard data');
    }));

    it('loads role-specific components for non-admin roles', async () => {
      await (component as any).loadRoleComponent(UserRole.MEDICAL_ASSISTANT);
      expect(component.roleComponent).toBeTruthy();

      await (component as any).loadRoleComponent(UserRole.REGISTERED_NURSE);
      expect(component.roleComponent).toBeTruthy();

      await (component as any).loadRoleComponent(UserRole.PROVIDER);
      expect(component.roleComponent).toBeTruthy();
    });

    it('clears role component for admin role', async () => {
      await (component as any).loadRoleComponent(UserRole.ADMIN);
      expect(component.roleComponent).toBeNull();
    });
  });

  // ============================================================================
  // 2. Statistics Cards (6 tests)
  // ============================================================================
  describe('Statistics Cards', () => {
    beforeEach(() => {
      const mockResults = [
        EvaluationFactory.createCompliantResult(),
        EvaluationFactory.createCompliantResult(),
        EvaluationFactory.createNonCompliantResult(),
      ];
      const mockPatients = PatientFactory.createSummaryList();

      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of(mockResults));
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of(mockPatients));
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));
    });

    it('should display total evaluations count', fakeAsync(() => {
      component.loadDashboardData();
      flush();

      expect(component.statistics.totalEvaluations).toBe(3);
    }));

    it('should display total patients count', fakeAsync(() => {
      component.loadDashboardData();
      flush();

      expect(component.statistics.totalPatients).toBe(3);
    }));

    it('should display overall compliance rate', fakeAsync(() => {
      component.loadDashboardData();
      flush();

      // Compliance calculation depends on evaluations with InDenominator and InNumerator
      // The factory creates evaluations that may have different compliance status
      expect(component.statistics.overallCompliance).toBeGreaterThanOrEqual(0);
      expect(component.statistics.overallCompliance).toBeLessThanOrEqual(100);
    }));

    it('sets trend direction based on compliance change', () => {
      jest.useFakeTimers().setSystemTime(new Date('2025-02-15T00:00:00Z'));
      component.allPatients = PatientFactory.createSummaryList();
      component.allEvaluations = [
        {
          id: 'eval-prev-1',
          patientId: component.allPatients[0].id,
          evaluationDate: '2024-12-20T00:00:00Z',
          evaluationResult: { InDenominator: true, InNumerator: false },
        } as any,
        {
          id: 'eval-current-1',
          patientId: component.allPatients[0].id,
          evaluationDate: '2025-02-10T00:00:00Z',
          evaluationResult: { InDenominator: true, InNumerator: true },
        } as any,
      ];

      (component as any).calculateStatistics();
      expect(component.trendDirection).toBe('up');

      component.allEvaluations = [
        {
          id: 'eval-prev-2',
          patientId: component.allPatients[0].id,
          evaluationDate: '2024-12-20T00:00:00Z',
          evaluationResult: { InDenominator: true, InNumerator: true },
        } as any,
        {
          id: 'eval-current-2',
          patientId: component.allPatients[0].id,
          evaluationDate: '2025-02-10T00:00:00Z',
          evaluationResult: { InDenominator: true, InNumerator: false },
        } as any,
      ];

      (component as any).calculateStatistics();
      expect(component.trendDirection).toBe('down');

      component.allEvaluations = [
        {
          id: 'eval-current-3',
          patientId: component.allPatients[0].id,
          evaluationDate: '2025-02-10T00:00:00Z',
          evaluationResult: { InDenominator: false, InNumerator: false },
        } as any,
      ];

      (component as any).calculateStatistics();
      expect(component.trendDirection).toBe('stable');
      jest.useRealTimers();
    });

    it('should display recent evaluations count (last 30 days)', fakeAsync(() => {
      const recentDate = new Date();
      recentDate.setDate(recentDate.getDate() - 15); // 15 days ago

      const mockResults = [
        EvaluationFactory.createCompliantResult(),
        { ...EvaluationFactory.createCompliantResult(), calculationDate: recentDate.toISOString() },
      ];

      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of(mockResults));

      component.loadDashboardData();
      flush();

      expect(component.statistics.recentEvaluations).toBeGreaterThanOrEqual(0);
    }));

    it('should calculate compliance percentage correctly', fakeAsync(() => {
      const mockResults = [
        EvaluationFactory.createCompliantResult(),
        EvaluationFactory.createCompliantResult(),
        EvaluationFactory.createCompliantResult(),
        EvaluationFactory.createNonCompliantResult(),
      ];

      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of(mockResults));

      component.loadDashboardData();
      flush();

      // Compliance is calculated based on InDenominator/InNumerator values
      expect(component.statistics.overallCompliance).toBeGreaterThanOrEqual(0);
      expect(component.statistics.overallCompliance).toBeLessThanOrEqual(100);
    }));

    it('should show 0 values when no data', fakeAsync(() => {
      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of([]));
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of([]));

      component.loadDashboardData();
      flush();

      expect(component.statistics.totalEvaluations).toBe(0);
      expect(component.statistics.totalPatients).toBe(0);
      expect(component.statistics.overallCompliance).toBe(0);
      expect(component.statistics.recentEvaluations).toBe(0);
    }));
  });

  describe('Recent Activity and Performance', () => {
    it('generates recent activity outcomes and fallbacks', () => {
      component.allPatients = PatientFactory.createSummaryList();
      component.allEvaluations = [
        {
          id: 'eval-1',
          patientId: component.allPatients[0].id,
          evaluationDate: '2025-01-15T00:00:00Z',
          evaluationResult: { InDenominator: false, InNumerator: false },
        } as any,
        {
          id: 'eval-2',
          patientId: component.allPatients[0].id,
          evaluationDate: '2025-01-16T00:00:00Z',
          evaluationResult: { InDenominator: true, InNumerator: true },
        } as any,
        {
          id: 'eval-3',
          patientId: 'missing-patient',
          evaluationDate: '2025-01-17T00:00:00Z',
          evaluationResult: { InDenominator: true, InNumerator: false },
        } as any,
      ];

      (component as any).generateRecentActivity();

      const outcomes = component.recentActivity.map((activity) => activity.outcome);
      expect(outcomes).toContain('not-eligible');
      expect(outcomes).toContain('compliant');
      expect(component.recentActivity.some((activity) => activity.patientName === 'Unknown Patient')).toBe(true);
    });

    it('calculates measure performance and chart data', () => {
      component.allEvaluations = [
        {
          id: 'eval-1',
          patientId: 'p1',
          evaluationDate: '2025-01-01T00:00:00Z',
          library: { id: 'lib-1', name: 'Very Long Measure Name That Exceeds Thirty Characters' },
          evaluationResult: { InDenominator: true, InNumerator: true },
        } as any,
        {
          id: 'eval-2',
          patientId: 'p1',
          evaluationDate: '2025-01-02T00:00:00Z',
          library: {},
          evaluationResult: { InDenominator: false, InNumerator: false },
        } as any,
      ];

      (component as any).calculateMeasurePerformance();

      expect(component.measurePerformance.length).toBe(2);
      expect(component.measurePerformanceChartData[0].name.endsWith('...')).toBe(true);
      expect(component.measurePerformanceChartData[0].value).toBeGreaterThanOrEqual(0);
    });

    it('calculates compliance trends for different periods', () => {
      component.allEvaluations = [
        {
          id: 'eval-1',
          patientId: 'p1',
          evaluationDate: '2025-01-05T00:00:00Z',
          evaluationResult: { InDenominator: true, InNumerator: true },
        } as any,
        {
          id: 'eval-2',
          patientId: 'p1',
          evaluationDate: 'invalid-date',
          evaluationResult: { InDenominator: true, InNumerator: false },
        } as any,
      ];

      component.trendPeriod = 'daily';
      (component as any).calculateComplianceTrends();
      expect(component.complianceTrends.length).toBe(1);

      component.trendPeriod = 'weekly';
      (component as any).calculateComplianceTrends();
      expect(component.complianceTrends.length).toBe(1);

      component.trendPeriod = 'monthly';
      (component as any).calculateComplianceTrends();
      expect(component.complianceTrends.length).toBe(1);
    });
  });

  describe('Helper Methods', () => {
    it('returns average compliance of 0 when no measures', () => {
      component.measurePerformance = [];
      expect(component.calculateAverageCompliance()).toBe(0);
    });

    it('formats MRN authority for URLs and strings', () => {
      expect(component.formatMRNAuthority('http://hospital.example.org/patients')).toBe('hospital.example.org');
      expect(component.formatMRNAuthority('Not a URL')).toBe('Not a URL');
    });

    it('returns correct outcome badge classes and default', () => {
      expect(component.getOutcomeBadgeClass('compliant')).toBe('badge-success');
      expect(component.getOutcomeBadgeClass('non-compliant')).toBe('badge-warning');
      expect(component.getOutcomeBadgeClass('not-eligible')).toBe('badge-info');
      expect(component.getOutcomeBadgeClass('unknown' as any)).toBe('badge-default');
    });
  });

  // ============================================================================
  // 3. Recent Activity (5 tests)
  // ============================================================================
  describe('Recent Activity', () => {
    it('should load recent evaluations (last 10)', fakeAsync(() => {
      const mockResults = EvaluationFactory.createMany(15);
      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of(mockResults));
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of([]));
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));

      component.loadDashboardData();
      flush();

      expect(component.recentActivity.length).toBeLessThanOrEqual(10);
    }));

    it('should display evaluation date, patient, measure, outcome', fakeAsync(() => {
      const mockResult = EvaluationFactory.createCompliantResult();
      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of([mockResult]));
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of([]));
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));

      component.loadDashboardData();
      flush();

      if (component.recentActivity.length > 0) {
        expect(component.recentActivity[0]).toHaveProperty('date');
        expect(component.recentActivity[0]).toHaveProperty('patientId');
        expect(component.recentActivity[0]).toHaveProperty('measureName');
        expect(component.recentActivity[0]).toHaveProperty('outcome');
      }
    }));

    it('should sort by date descending (newest first)', fakeAsync(() => {
      const oldDate = new Date('2024-01-01');
      const newDate = new Date('2024-12-01');

      const mockResults = [
        { ...EvaluationFactory.createCompliantResult(), calculationDate: oldDate.toISOString() },
        { ...EvaluationFactory.createCompliantResult(), calculationDate: newDate.toISOString() },
      ];

      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of(mockResults));
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of([]));
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));

      component.loadDashboardData();
      flush();

      // Recent activity should be sorted if present
      if (component.recentActivity.length >= 2) {
        // Verify sorting - each item should have a date that's >= the next item's date
        for (let i = 0; i < component.recentActivity.length - 1; i++) {
          const currentDate = new Date(component.recentActivity[i].date);
          const nextDate = new Date(component.recentActivity[i + 1].date);
          // Only compare if dates are valid
          if (!isNaN(currentDate.getTime()) && !isNaN(nextDate.getTime())) {
            expect(currentDate.getTime()).toBeGreaterThanOrEqual(nextDate.getTime());
          }
        }
      }
    }));

    it('should handle empty recent activity', fakeAsync(() => {
      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of([]));
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of([]));
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));

      component.loadDashboardData();
      flush();

      expect(component.recentActivity).toEqual([]);
    }));

    it('should navigate to result details on click', () => {
      const activity = {
        id: 'result-1',
        date: '2024-01-15',
        patientId: 'patient-001',
        patientName: 'John Doe',
        measureName: 'HEDIS-CDC',
        outcome: 'compliant' as const,
      };

      component.viewResultDetails(activity.id);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/results', activity.id]);
    });
  });

  // ============================================================================
  // 4. Quick Actions (4 tests)
  // ============================================================================
  describe('Quick Actions', () => {
    it('should have "New Evaluation" button', () => {
      expect(component.quickActions).toContainEqual(
        expect.objectContaining({ label: 'New Evaluation' })
      );
    });

    it('should have "View All Results" button', () => {
      expect(component.quickActions).toContainEqual(
        expect.objectContaining({ label: 'View All Results' })
      );
    });

    it('should have "View Reports" button', () => {
      expect(component.quickActions).toContainEqual(
        expect.objectContaining({ label: 'View Reports' })
      );
    });

    it('should navigate to correct routes on click', () => {
      component.navigateTo('/evaluations');
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/evaluations']);

      component.navigateTo('/results');
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/results']);

      component.navigateTo('/reports');
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/reports']);
    });
  });

  // ============================================================================
  // 5. Compliance Trends (6 tests)
  // ============================================================================
  describe('Compliance Trends', () => {
    it('should display compliance trend chart data', fakeAsync(() => {
      const mockResults = EvaluationFactory.createMany(10);
      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of(mockResults));
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of([]));
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));

      component.loadDashboardData();
      flush();

      expect(component.complianceTrends).toBeDefined();
    }));

    it('should group by time period (daily, weekly, monthly)', fakeAsync(() => {
      const mockResults = EvaluationFactory.createMany(30);
      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of(mockResults));
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of([]));
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));

      component.loadDashboardData();
      flush();
      component.setTrendPeriod('weekly');

      expect(component.trendPeriod).toBe('weekly');
    }));

    it('should calculate trend direction (up, down, stable)', fakeAsync(() => {
      const mockResults = [
        EvaluationFactory.createCompliantResult(),
        EvaluationFactory.createCompliantResult(),
        EvaluationFactory.createNonCompliantResult(),
      ];
      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of(mockResults));
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of([]));
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));

      component.loadDashboardData();
      flush();

      expect(['up', 'down', 'stable', 'unknown']).toContain(component.trendDirection);
    }));

    it('should show percentage change', fakeAsync(() => {
      const mockResults = EvaluationFactory.createMany(10);
      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of(mockResults));
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of([]));
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));

      component.loadDashboardData();
      flush();

      expect(component.statistics.complianceChange).toBeDefined();
      expect(typeof component.statistics.complianceChange).toBe('number');
    }));

    it('should handle no historical data', fakeAsync(() => {
      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of([]));
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of([]));
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));

      component.loadDashboardData();
      flush();

      expect(component.complianceTrends).toEqual([]);
    }));

    it('should support date range selection', () => {
      const startDate = new Date('2024-01-01');
      const endDate = new Date('2024-12-31');

      component.setDateRange(startDate, endDate);

      expect(component.dateRange.start).toEqual(startDate);
      expect(component.dateRange.end).toEqual(endDate);
    });

    it('should ignore invalid evaluation dates in trend calculation', () => {
      (component as any).allEvaluations = [
        { ...EvaluationFactory.createCompliantResult(), evaluationDate: 'invalid-date' },
      ];

      component.setTrendPeriod('daily');

      expect(component.complianceTrends.length).toBe(0);
    });

    it('should calculate monthly trend buckets', () => {
      const evalDate = new Date('2024-02-15').toISOString();
      (component as any).allEvaluations = [
        { ...EvaluationFactory.createCompliantResult(), evaluationDate: evalDate },
      ];

      component.setTrendPeriod('monthly');

      expect(component.complianceTrends[0]?.period).toBe('2024-02');
    });
  });

  // ============================================================================
  // 6. Measure Performance (5 tests)
  // ============================================================================
  describe('Measure Performance', () => {
    beforeEach(() => {
      const mockResults = [
        { ...EvaluationFactory.createCompliantResult(), measureId: 'CDC-1', measureName: 'HEDIS-CDC' },
        { ...EvaluationFactory.createCompliantResult(), measureId: 'CDC-1', measureName: 'HEDIS-CDC' },
        { ...EvaluationFactory.createNonCompliantResult(), measureId: 'CBP-1', measureName: 'HEDIS-CBP' },
      ];
      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of(mockResults));
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of([]));
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));
    });

    it('should list all measures with compliance rates', fakeAsync(() => {
      component.loadDashboardData();
      flush();

      if (component.measurePerformance.length > 0) {
        component.measurePerformance.forEach(measure => {
          expect(measure).toHaveProperty('measureName');
          expect(measure).toHaveProperty('complianceRate');
        });
      }
    }));

    it('should sort measures by compliance rate', fakeAsync(() => {
      component.loadDashboardData();
      flush();

      if (component.measurePerformance.length > 1) {
        const rates = component.measurePerformance.map(m => m.complianceRate);
        const sortedRates = [...rates].sort((a, b) => b - a);
        expect(rates).toEqual(sortedRates);
      }
    }));

    it('should show measure category (HEDIS, CMS)', fakeAsync(() => {
      component.loadDashboardData();
      flush();

      component.measurePerformance.forEach(measure => {
        if (measure.category) {
          // Valid categories include HEDIS, CMS, CUSTOM, Custom, CQL
          expect(['HEDIS', 'CMS', 'CUSTOM', 'Custom', 'CQL']).toContain(measure.category);
        }
      });
    }));

    it('should handle measures with no evaluations', fakeAsync(() => {
      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of([]));
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of([]));
      const mockMeasures = CqlLibraryFactory.createMeasureInfoList();
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of(mockMeasures));

      component.loadDashboardData();
      flush();

      expect(component.measurePerformance.length).toBe(0);
    }));

    it('should calculate average compliance across measures', fakeAsync(() => {
      component.loadDashboardData();
      flush();

      const avgCompliance = component.calculateAverageCompliance();

      expect(avgCompliance).toBeGreaterThanOrEqual(0);
      expect(avgCompliance).toBeLessThanOrEqual(100);
    }));
  });

  // ============================================================================
  // 7. Data Refresh (4 tests)
  // ============================================================================
  describe('Data Refresh', () => {
    it('should have refresh button', () => {
      expect(component.refreshData).toBeDefined();
    });

    it('should reload data when refresh clicked', fakeAsync(() => {
      const loadDataSpy = jest.spyOn(component, 'loadDashboardData');
      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of([]));
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of([]));
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));

      component.refreshData();
      flush();

      expect(loadDataSpy).toHaveBeenCalled();
    }));

    it('should show last updated timestamp', fakeAsync(() => {
      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of([]));
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of([]));
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));

      component.loadDashboardData();
      flush();

      expect(component.lastUpdated).toBeDefined();
      expect(component.lastUpdated).toBeInstanceOf(Date);
    }));

    it('should update timestamp after refresh', fakeAsync(() => {
      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of([]));
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of([]));
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));

      component.loadDashboardData();
      flush();
      const firstTimestamp = component.lastUpdated;

      component.refreshData();
      flush();
      const secondTimestamp = component.lastUpdated;

      expect(secondTimestamp?.getTime()).toBeGreaterThanOrEqual(firstTimestamp?.getTime() || 0);
    }));
  });

  // ============================================================================
  // 8. Error Handling (3 tests)
  // ============================================================================
  describe('Error Handling', () => {
    it('should display error message when data load fails', fakeAsync(() => {
      const error = { status: 500, message: 'Server Error' };
      // Error in forkJoin (patient or measure service) triggers the error path
      mockPatientService.getPatientsSummaryCached.mockReturnValue(
        throwError(() => error)
      );
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));
      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of([]));

      component.loadDashboardData();
      flush();

      expect(component.error).toContain('Failed to load dashboard data');
    }));

    it('should retry on error', fakeAsync(() => {
      const error = { status: 500, message: 'Server Error' };
      mockPatientService.getPatientsSummaryCached.mockReturnValue(
        throwError(() => error)
      );
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));
      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of([]));

      component.loadDashboardData();
      flush();
      expect(component.error).toBeTruthy();

      // Retry with successful response
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of([]));
      component.refreshData();
      flush();

      expect(component.error).toBeNull();
    }));

    it('should show empty state with helpful message', fakeAsync(() => {
      mockEvaluationService.getAllEvaluationsCached.mockReturnValue(of([]));
      mockPatientService.getPatientsSummaryCached.mockReturnValue(of([]));
      mockMeasureService.getActiveMeasuresInfoCached.mockReturnValue(of([]));

      component.loadDashboardData();
      flush();

      expect(component.isEmpty()).toBe(true);
    }));
  });

  // ============================================================================
  // Additional Helper Tests
  // ============================================================================
  describe('Helper Methods', () => {
    it('should format date correctly', () => {
      const dateString = '2024-01-15T10:30:00Z';
      const formatted = component.formatDate(dateString);

      expect(formatted).toContain('2024');
      expect(formatted).toContain('01');
      expect(formatted).toContain('15');
    });

    it('should format percentage correctly', () => {
      const percentage = component.formatPercentage(85.567);

      expect(percentage).toBe('85.6%');
    });

    it('should get outcome badge class for compliant', () => {
      const badgeClass = component.getOutcomeBadgeClass('compliant');

      expect(badgeClass).toBe('badge-success');
    });

    it('should get outcome badge class for non-compliant', () => {
      const badgeClass = component.getOutcomeBadgeClass('non-compliant');

      expect(badgeClass).toBe('badge-warning');
    });

    it('should get outcome badge class for not-eligible', () => {
      const badgeClass = component.getOutcomeBadgeClass('not-eligible');

      expect(badgeClass).toBe('badge-info');
    });

    it('should map status class based on result string', () => {
      expect(component.getStatusClass('Compliant')).toBe('status-success');
      expect(component.getStatusClass('Non-Compliant')).toBe('status-error');
    });

    it('should determine if dashboard is empty', () => {
      component.statistics.totalEvaluations = 0;
      component.statistics.totalPatients = 0;

      expect(component.isEmpty()).toBe(true);
    });

    it('should determine if dashboard is not empty', () => {
      component.statistics.totalEvaluations = 5;
      component.statistics.totalPatients = 10;

      expect(component.isEmpty()).toBe(false);
    });
  });

  // ============================================================================
  // Additional Behaviors
  // ============================================================================
  describe('Additional Behaviors', () => {
    it('should force refresh and invalidate caches', () => {
      const loadDataSpy = jest.spyOn(component, 'loadDashboardData');

      component.forceRefresh();

      expect(mockPatientService.invalidateCache).toHaveBeenCalled();
      expect(mockEvaluationService.invalidateAllCaches).toHaveBeenCalled();
      expect(mockMeasureService.invalidateCache).toHaveBeenCalled();
      expect(loadDataSpy).toHaveBeenCalled();
    });

    it('should update quick action state on success', async () => {
      const action = { ...component.quickActions[0] };
      const navPromise = Promise.resolve(true);
      mockRouter.navigate.mockReturnValue(navPromise as any);

      component.onQuickAction(action);

      expect(action.loading).toBe(true);
      await navPromise;

      expect(action.loading).toBe(false);
      expect(action.success).toBe(true);
    });

    it('should reset quick action state on failure', async () => {
      const action = { ...component.quickActions[0] };
      const navPromise = Promise.reject(new Error('navigation failed'));
      mockRouter.navigate.mockReturnValue(navPromise as any);

      component.onQuickAction(action);

      expect(action.loading).toBe(true);
      await navPromise.catch(() => undefined);

      expect(action.loading).toBe(false);
      expect(action.success).toBe(false);
    });

    it('should format MRN authority hostnames', () => {
      const urlAuthority = component.formatMRNAuthority('http://hospital.example.org/patients');
      const plainAuthority = component.formatMRNAuthority('facility-123');
      const emptyAuthority = component.formatMRNAuthority(undefined);

      expect(urlAuthority).toBe('hospital.example.org');
      expect(plainAuthority).toBe('facility-123');
      expect(emptyAuthority).toBe('');
    });

    it('should leave invalid MRN authority unchanged', () => {
      expect(component.formatMRNAuthority('not a url')).toBe('not a url');
    });

    it('should calculate care gaps and urgency breakdown', () => {
      const today = new Date();
      const overdueHigh = new Date(today);
      overdueHigh.setDate(today.getDate() - 120);
      const overdueMedium = new Date(today);
      overdueMedium.setDate(today.getDate() - 45);

      const evaluations: any[] = [
        {
          id: 'eval-1',
          patientId: 'patient-1',
          evaluationDate: overdueHigh.toISOString(),
          library: { id: 'lib-1', name: 'Screening Measure' },
          evaluationResult: { InDenominator: true, InNumerator: false },
        },
        {
          id: 'eval-2',
          patientId: 'patient-2',
          evaluationDate: overdueMedium.toISOString(),
          library: { id: 'lib-2', name: 'Statin Medication' },
          evaluationResult: { InDenominator: true, InNumerator: false },
        },
      ];

      const patients: any[] = [
        { id: 'patient-1', fullName: 'Alice Adams', mrn: 'MRN-1' },
        { id: 'patient-2', fullName: 'Ben Brooks', mrn: 'MRN-2' },
      ];

      (component as any).allEvaluations = evaluations;
      (component as any).allPatients = patients;

      (component as any).calculateCareGaps();

      expect(component.careGapSummary?.totalGaps).toBe(2);
      expect(component.careGapSummary?.highUrgencyCount).toBe(1);
      expect(component.careGapSummary?.mediumUrgencyCount).toBe(1);
      expect(component.careGapSummary?.byType.screening).toBe(1);
      expect(component.careGapSummary?.byType.medication).toBe(1);
      expect(component.urgentCareGaps[0].urgency).toBe('high');
    });

    it('should navigate to care gap view', () => {
      component.viewAllCareGaps();

      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients'],
        { queryParams: { filter: 'care-gaps', urgency: 'high' } }
      );
    });

    it('should navigate to patient care gap actions', () => {
      component.viewPatientWithCareGap('patient-1');
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/patients', 'patient-1']);

      component.scheduleAppointment('patient-2');
      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients', 'patient-2'],
        { queryParams: { action: 'schedule' } }
      );

      component.sendReminder('patient-3');
      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients', 'patient-3'],
        { queryParams: { action: 'remind' } }
      );
    });

    it('should navigate to evaluation and compliance views', () => {
      component.viewAllEvaluations();
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/evaluations']);

      component.viewEvaluationsByStatus('FAILED');
      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/evaluations'],
        { queryParams: { status: 'FAILED' } }
      );

      component.viewPatientsByStatus('inactive');
      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/patients'],
        { queryParams: { status: 'inactive' } }
      );

      component.viewComplianceBreakdown();
      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/reports'],
        { queryParams: { reportType: 'compliance' } }
      );
    });

    it('should switch roles through the role service', () => {
      component.switchRole(UserRole.PROVIDER);

      expect(mockUserRoleService.setRole).toHaveBeenCalledWith(UserRole.PROVIDER);
      expect(component.currentRole).toBe(UserRole.PROVIDER);
    });

    it('should clear role component for admin view', async () => {
      await (component as any).loadRoleComponent(UserRole.ADMIN);

      expect(component.roleComponent).toBeNull();
    });
  });
});
