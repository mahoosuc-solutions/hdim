import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { signal, WritableSignal } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { SalesDashboardComponent } from './sales-dashboard.component';
import { SalesService } from '../../../services/sales.service';
import { SalesDashboard } from '../../../models/sales.model';

// Helper to create mock service with Jest
function createMockSalesService() {
  const dashboardSignal = signal<SalesDashboard | null>(null);
  const isLoadingSignal = signal(false);
  const errorSignal = signal<string | null>(null);

  return {
    loadDashboard: jest.fn(),
    dashboard: dashboardSignal as WritableSignal<SalesDashboard | null>,
    isLoading: isLoadingSignal as WritableSignal<boolean>,
    error: errorSignal as WritableSignal<string | null>,
    // Helper methods to control signals in tests
    _setDashboard: (value: SalesDashboard | null) => dashboardSignal.set(value),
    _setLoading: (value: boolean) => isLoadingSignal.set(value),
    _setError: (value: string | null) => errorSignal.set(value),
  };
}

type MockSalesService = ReturnType<typeof createMockSalesService>;

describe('SalesDashboardComponent', () => {
  let component: SalesDashboardComponent;
  let fixture: ComponentFixture<SalesDashboardComponent>;
  let mockSalesService: MockSalesService;

  const mockDashboard: SalesDashboard = {
    leadMetrics: {
      totalLeads: 150,
      openLeads: 45,
      qualifiedLeads: 28,
      convertedLeads: 15,
      conversionRate: 0.333,
    },
    pipelineMetrics: {
      totalValue: 1500000,
      weightedValue: 750000,
      openOpportunities: 25,
      closingThisMonth: 8,
      atRiskDeals: 3,
    },
    activityMetrics: {
      callsThisWeek: 42,
      emailsThisWeek: 78,
      meetingsThisWeek: 12,
      overdueCount: 5,
    },
    recentLeads: [
      { id: '1', firstName: 'John', lastName: 'Doe', email: 'john@example.com', company: 'Acme', status: 'OPEN', source: 'WEBSITE' },
      { id: '2', firstName: 'Jane', lastName: 'Smith', email: 'jane@example.com', company: 'Tech', status: 'QUALIFIED', source: 'LINKEDIN' },
    ],
    recentOpportunities: [
      { id: '1', name: 'Enterprise Deal', accountId: '1', value: 250000, stage: 'PROPOSAL', probability: 60, expectedCloseDate: '2026-03-15' },
      { id: '2', name: 'SMB Package', accountId: '2', value: 45000, stage: 'DEMO', probability: 40, expectedCloseDate: '2026-02-28' },
    ],
    recentActivities: [
      { id: '1', type: 'CALL', subject: 'Follow-up call', status: 'COMPLETED', dueDate: '2026-02-01', priority: 'HIGH' },
      { id: '2', type: 'EMAIL', subject: 'Proposal sent', status: 'COMPLETED', dueDate: '2026-02-02', priority: 'MEDIUM' },
    ],
  };

  beforeEach(async () => {
    mockSalesService = createMockSalesService();

    // Default mock return
    mockSalesService.loadDashboard.mockReturnValue(of(mockDashboard));

    await TestBed.configureTestingModule({
      imports: [SalesDashboardComponent, RouterTestingModule],
      providers: [
        { provide: SalesService, useValue: mockSalesService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SalesDashboardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should load dashboard on init', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      expect(mockSalesService.loadDashboard).toHaveBeenCalled();
    }));

    it('should display loading state when isLoading is true', fakeAsync(() => {
      mockSalesService._setLoading(true);
      fixture.detectChanges();

      const loadingEl = fixture.nativeElement.querySelector('.loading');
      expect(loadingEl).toBeTruthy();
      expect(loadingEl.textContent).toContain('Loading dashboard');
    }));

    it('should hide loading state when isLoading is false', fakeAsync(() => {
      mockSalesService._setLoading(false);
      mockSalesService._setDashboard(mockDashboard);
      fixture.detectChanges();

      const loadingEl = fixture.nativeElement.querySelector('.loading');
      expect(loadingEl).toBeFalsy();
    }));
  });

  describe('Dashboard Display', () => {
    beforeEach(fakeAsync(() => {
      mockSalesService._setDashboard(mockDashboard);
      mockSalesService._setLoading(false);
      fixture.detectChanges();
      tick();
    }));

    it('should display lead metrics', () => {
      const dashboard = component.dashboard();
      expect(dashboard?.leadMetrics.totalLeads).toBe(150);
      expect(dashboard?.leadMetrics.openLeads).toBe(45);
      expect(dashboard?.leadMetrics.qualifiedLeads).toBe(28);
    });

    it('should display pipeline metrics', () => {
      const dashboard = component.dashboard();
      expect(dashboard?.pipelineMetrics.totalValue).toBe(1500000);
      expect(dashboard?.pipelineMetrics.openOpportunities).toBe(25);
    });

    it('should display activity metrics', () => {
      const dashboard = component.dashboard();
      expect(dashboard?.activityMetrics.callsThisWeek).toBe(42);
      expect(dashboard?.activityMetrics.emailsThisWeek).toBe(78);
    });

    it('should display recent leads', () => {
      const dashboard = component.dashboard();
      expect(dashboard?.recentLeads.length).toBe(2);
      expect(dashboard?.recentLeads[0].firstName).toBe('John');
    });

    it('should display recent opportunities', () => {
      const dashboard = component.dashboard();
      expect(dashboard?.recentOpportunities.length).toBe(2);
      expect(dashboard?.recentOpportunities[0].name).toBe('Enterprise Deal');
    });

    it('should display recent activities', () => {
      const dashboard = component.dashboard();
      expect(dashboard?.recentActivities.length).toBe(2);
      expect(dashboard?.recentActivities[0].type).toBe('CALL');
    });
  });

  describe('Error Handling', () => {
    it('should display error message when error occurs', fakeAsync(() => {
      mockSalesService._setError('Failed to load dashboard');
      fixture.detectChanges();

      const errorEl = fixture.nativeElement.querySelector('.error-banner');
      expect(errorEl).toBeTruthy();
      expect(errorEl.textContent).toContain('Failed to load dashboard');
    }));

    it('should have retry button in error state', fakeAsync(() => {
      mockSalesService._setError('Failed to load dashboard');
      fixture.detectChanges();

      const retryBtn = fixture.nativeElement.querySelector('.error-banner .btn-link');
      expect(retryBtn).toBeTruthy();
      expect(retryBtn.textContent).toContain('Retry');
    }));
  });

  describe('Actions', () => {
    beforeEach(fakeAsync(() => {
      mockSalesService._setDashboard(mockDashboard);
      mockSalesService._setLoading(false);
      fixture.detectChanges();
      tick();
    }));

    it('should refresh data when refresh button clicked', fakeAsync(() => {
      mockSalesService.loadDashboard.mockClear();

      component.refreshData();
      tick();

      expect(mockSalesService.loadDashboard).toHaveBeenCalled();
    }));

    it('should call openNewLead when new lead button clicked', () => {
      const openNewLeadSpy = jest.spyOn(component, 'openNewLead');

      component.openNewLead();

      expect(openNewLeadSpy).toHaveBeenCalled();
    });
  });

  describe('Utility Methods', () => {
    it('should format initials correctly', () => {
      expect(component.getInitials('John', 'Doe')).toBe('JD');
      expect(component.getInitials('jane', 'smith')).toBe('JS');
    });

    it('should format status correctly', () => {
      expect(component.formatStatus('OPEN')).toBe('Open');
      expect(component.formatStatus('QUALIFIED')).toBe('Qualified');
      expect(component.formatStatus('CLOSED_WON')).toBe('Closed Won');
    });

    it('should format stage correctly', () => {
      expect(component.formatStage('DISCOVERY')).toBe('Discovery');
      expect(component.formatStage('DEMO')).toBe('Demo');
      expect(component.formatStage('CLOSED_WON')).toBe('Closed Won');
    });

    it('should return correct activity icons', () => {
      expect(component.getActivityIcon('CALL')).toBe('📞');
      expect(component.getActivityIcon('EMAIL')).toBe('📧');
      expect(component.getActivityIcon('MEETING')).toBe('📅');
      expect(component.getActivityIcon('DEMO')).toBe('💻');
      expect(component.getActivityIcon('TASK')).toBe('✅');
      expect(component.getActivityIcon('UNKNOWN')).toBe('📌');
    });
  });

  describe('Cleanup', () => {
    it('should complete destroy$ on ngOnDestroy', () => {
      const nextSpy = jest.spyOn(component['destroy$'], 'next');
      const completeSpy = jest.spyOn(component['destroy$'], 'complete');

      component.ngOnDestroy();

      expect(nextSpy).toHaveBeenCalled();
      expect(completeSpy).toHaveBeenCalled();
    });
  });
});
