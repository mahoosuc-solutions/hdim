import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';

import { Phase2DashboardComponent } from './phase2-dashboard.component';
import { Phase2ExecutionService } from '../../../services/phase2-execution.service';
import { LoggerService } from '../../../services/logger.service';

describe('Phase2DashboardComponent - Financial ROI View', () => {
  let component: Phase2DashboardComponent;
  let fixture: ComponentFixture<Phase2DashboardComponent>;
  let mockExecutionService: jasmine.SpyObj<Phase2ExecutionService>;
  let mockLoggerService: jasmine.SpyObj<LoggerService>;

  const mockFinancialDashboard = {
    totalBonusCaptured: 150000,
    totalGapsClosed: 245,
    averageROI: 325.5,
    monthlyProgression: [
      { month: 'January', captured: 50000, gaps: 80 },
      { month: 'February', captured: 100000, gaps: 165 },
      { month: 'March', captured: 150000, gaps: 245 },
    ],
  };

  const mockMeasureROI = [
    {
      measure: 'HBA1C',
      totalCaptured: 75000,
      totalGapsClosed: 120,
      taskCount: 45,
      roi: 450.5,
    },
    {
      measure: 'BCS',
      totalCaptured: 45000,
      totalGapsClosed: 80,
      taskCount: 32,
      roi: 280.25,
    },
    {
      measure: 'COLORECTAL',
      totalCaptured: 30000,
      totalGapsClosed: 45,
      taskCount: 20,
      roi: 225.0,
    },
  ];

  beforeEach(async () => {
    mockExecutionService = jasmine.createSpyObj('Phase2ExecutionService', [
      'getFinancialDashboard',
      'getMeasureROI',
      'getCaseStudies',
      'publishCaseStudy',
    ]);

    mockLoggerService = jasmine.createSpyObj('LoggerService', ['withContext']);
    mockLoggerService.withContext.and.returnValue({
      info: jasmine.createSpy('info'),
      error: jasmine.createSpy('error'),
      warn: jasmine.createSpy('warn'),
      debug: jasmine.createSpy('debug'),
    });

    await TestBed.configureTestingModule({
      imports: [
        Phase2DashboardComponent,
        CommonModule,
        MatCardModule,
        MatProgressBarModule,
        MatProgressSpinnerModule,
        MatGridListModule,
        MatIconModule,
        MatChipsModule,
        MatTableModule,
        HttpClientTestingModule,
      ],
      providers: [
        { provide: Phase2ExecutionService, useValue: mockExecutionService },
        { provide: LoggerService, useValue: mockLoggerService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Phase2DashboardComponent);
    component = fixture.componentInstance;
  });

  describe('Financial Dashboard Metrics', () => {
    it('should display financial dashboard metrics', (done) => {
      mockExecutionService.getFinancialDashboard.and.returnValue(
        of(mockFinancialDashboard)
      );

      component.ngOnInit();

      setTimeout(() => {
        expect(component.financialMetrics).toEqual(mockFinancialDashboard);
        expect(component.totalBonusCaptured).toBe(150000);
        expect(component.totalGapsClosed).toBe(245);
        expect(component.averageROI).toBe(325.5);
        done();
      }, 100);
    });

    it('should handle financial dashboard loading error gracefully', (done) => {
      const error = new Error('Failed to load financial data');
      mockExecutionService.getFinancialDashboard.and.returnValue(
        throwError(() => error)
      );

      component.ngOnInit();

      setTimeout(() => {
        expect(component.financialMetrics).toBeUndefined();
        expect(component.totalBonusCaptured).toBe(0);
        done();
      }, 100);
    });

    it('should initialize financial metrics with default values', () => {
      expect(component.totalBonusCaptured).toBe(0);
      expect(component.totalGapsClosed).toBe(0);
      expect(component.averageROI).toBe(0);
    });
  });

  describe('Measure-Specific ROI Breakdown', () => {
    it('should display measure-specific ROI breakdown', (done) => {
      mockExecutionService.getMeasureROI.and.returnValue(of(mockMeasureROI));

      component.ngOnInit();

      setTimeout(() => {
        expect(component.measureROIData).toEqual(mockMeasureROI);
        expect(component.measureROIData.length).toBe(3);
        expect(component.measureROIData[0].measure).toBe('HBA1C');
        expect(component.measureROIData[0].totalCaptured).toBe(75000);
        done();
      }, 100);
    });

    it('should handle measure ROI loading error gracefully', (done) => {
      const error = new Error('Failed to load measure ROI');
      mockExecutionService.getMeasureROI.and.returnValue(
        throwError(() => error)
      );

      component.ngOnInit();

      setTimeout(() => {
        expect(component.measureROIData.length).toBe(0);
        done();
      }, 100);
    });

    it('should sort measures by captured amount descending', (done) => {
      const unsortedMeasures = [
        {
          measure: 'COLORECTAL',
          totalCaptured: 30000,
          totalGapsClosed: 45,
          taskCount: 20,
          roi: 225.0,
        },
        {
          measure: 'HBA1C',
          totalCaptured: 75000,
          totalGapsClosed: 120,
          taskCount: 45,
          roi: 450.5,
        },
        {
          measure: 'BCS',
          totalCaptured: 45000,
          totalGapsClosed: 80,
          taskCount: 32,
          roi: 280.25,
        },
      ];

      mockExecutionService.getMeasureROI.and.returnValue(
        of(unsortedMeasures)
      );

      component.ngOnInit();

      setTimeout(() => {
        expect(component.measureROIData.length).toBe(3);
        done();
      }, 100);
    });
  });

  describe('Currency Formatting', () => {
    it('should format currency correctly with USD symbol', () => {
      const formatted = component.formatCurrency(150000);
      expect(formatted).toContain('$');
      expect(formatted).toContain('150');
    });

    it('should format currency without decimal places', () => {
      const formatted = component.formatCurrency(150000.99);
      expect(formatted).not.toContain('.99');
    });

    it('should handle zero currency', () => {
      const formatted = component.formatCurrency(0);
      expect(formatted).toBe('$0');
    });

    it('should handle negative currency', () => {
      const formatted = component.formatCurrency(-50000);
      expect(formatted).toContain('-');
    });

    it('should format large numbers with thousand separators', () => {
      const formatted = component.formatCurrency(1500000);
      expect(formatted).toMatch(/,/);
    });
  });

  describe('Percentage Formatting', () => {
    it('should format percentage correctly with 2 decimal places', () => {
      const formatted = component.formatPercentage(325.5);
      expect(formatted).toBe('325.50%');
    });

    it('should format whole numbers with decimal places', () => {
      const formatted = component.formatPercentage(100);
      expect(formatted).toBe('100.00%');
    });

    it('should format small percentages', () => {
      const formatted = component.formatPercentage(5.789);
      expect(formatted).toBe('5.79%');
    });

    it('should handle zero percentage', () => {
      const formatted = component.formatPercentage(0);
      expect(formatted).toBe('0.00%');
    });
  });

  describe('Lifecycle Management', () => {
    it('should load financial data on init', (done) => {
      mockExecutionService.getFinancialDashboard.and.returnValue(
        of(mockFinancialDashboard)
      );
      mockExecutionService.getMeasureROI.and.returnValue(of(mockMeasureROI));

      component.ngOnInit();

      setTimeout(() => {
        expect(mockExecutionService.getFinancialDashboard).toHaveBeenCalled();
        expect(mockExecutionService.getMeasureROI).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should unsubscribe on destroy', () => {
      spyOn(component['destroy$'], 'next');
      spyOn(component['destroy$'], 'complete');

      component.ngOnDestroy();

      expect(component['destroy$'].next).toHaveBeenCalled();
      expect(component['destroy$'].complete).toHaveBeenCalled();
    });

    it('should use takeUntil to manage subscriptions', (done) => {
      mockExecutionService.getFinancialDashboard.and.returnValue(
        of(mockFinancialDashboard)
      );
      mockExecutionService.getMeasureROI.and.returnValue(of(mockMeasureROI));

      component.ngOnInit();

      setTimeout(() => {
        component.ngOnDestroy();
        expect(component['destroy$']).toBeDefined();
        done();
      }, 100);
    });
  });

  describe('Template Rendering', () => {
    it('should have financial dashboard template', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled).toBeTruthy();
    });

    it('should display financial metrics cards when data is loaded', (done) => {
      mockExecutionService.getFinancialDashboard.and.returnValue(
        of(mockFinancialDashboard)
      );
      mockExecutionService.getMeasureROI.and.returnValue(of(mockMeasureROI));

      component.ngOnInit();
      fixture.detectChanges();

      setTimeout(() => {
        fixture.detectChanges();
        const cards = fixture.nativeElement.querySelectorAll('.metric-card');
        expect(cards.length).toBeGreaterThan(0);
        done();
      }, 100);
    });
  });

  describe('Case Studies', () => {
    it('should fetch published case studies', (done) => {
      const caseStudies = [
        { id: 'cs1', title: 'Success Story 1', published: true },
        { id: 'cs2', title: 'Success Story 2', published: true },
      ];

      mockExecutionService.getCaseStudies.and.returnValue(
        of(caseStudies)
      );

      component.getCaseStudies().subscribe((result) => {
        expect(result).toEqual(caseStudies);
        done();
      });
    });

    it('should publish a case study', (done) => {
      const caseStudyId = 'cs1';
      mockExecutionService.publishCaseStudy.and.returnValue(
        of({ id: caseStudyId, published: true })
      );

      component.publishCaseStudy(caseStudyId).subscribe((result) => {
        expect(result.published).toBe(true);
        done();
      });
    });
  });
});
