import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { CareGapManagerComponent } from './care-gap-manager.component';
import { PatientService } from '../../services/patient.service';
import { MeasureService } from '../../services/measure.service';
import { DialogService } from '../../services/dialog.service';
import { CareGapService } from '../../services/care-gap.service';
import { LoggerService } from '../../services/logger.service';
import { createMockLoggerService } from '../../testing/mocks';
import { CSVHelper } from '../../utils/csv-helper';

describe('CareGapManagerComponent', () => {
  let component: CareGapManagerComponent;
  let fixture: ComponentFixture<CareGapManagerComponent>;
  let mockPatientService: jest.Mocked<PatientService>;
  let mockMeasureService: jest.Mocked<MeasureService>;
  let mockCareGapService: jest.Mocked<CareGapService>;
  let mockDialogService: jest.Mocked<DialogService>;
  let mockMatDialog: jest.Mocked<MatDialog>;
  let mockSnackBar: jest.Mocked<MatSnackBar>;
  let mockRouter: jest.Mocked<Router>;

  beforeEach(async () => {
    const mockPatientData = [
      { id: '1', fullName: 'Anderson, Sarah', mrn: 'MRN001', status: 'Active' },
      { id: '2', fullName: 'Martinez, Elena', mrn: 'MRN002', status: 'Active' },
      { id: '3', fullName: 'Johnson, Robert', mrn: 'MRN003', status: 'Active' },
    ];
    const mockCareGapData = {
      content: [
        { id: 'gap-1', patientId: '1', measureId: 'BCS', measureName: 'BCS', gapStatus: 'OPEN', priority: 'HIGH', gapDescription: 'Mammogram overdue', dueDate: '2025-10-15' },
        { id: 'gap-2', patientId: '2', measureId: 'BCS', measureName: 'BCS', gapStatus: 'OPEN', priority: 'HIGH', gapDescription: 'Mammogram needed', dueDate: '2025-06-01' },
        { id: 'gap-3', patientId: '3', measureId: 'COL', measureName: 'COL', gapStatus: 'OPEN', priority: 'MEDIUM', gapDescription: 'Colonoscopy overdue', dueDate: '2024-12-01' },
      ],
      totalElements: 3,
    };

    // Create mock services
    mockPatientService = {
      getPatientsSummary: jest.fn().mockReturnValue(of(mockPatientData)),
      getPatientsSummaryCached: jest.fn().mockReturnValue(of(mockPatientData)),
      getPatient: jest.fn(),
    } as any;

    mockMeasureService = {
      getCareGaps: jest.fn().mockReturnValue(of(mockCareGapData.content)),
      getCareGapsPage: jest.fn().mockReturnValue(of(mockCareGapData)),
      closeCareGap: jest.fn(),
    } as any;

    mockDialogService = {
      confirm: jest.fn(),
    } as any;

    mockCareGapService = {
      getCareGapsPage: jest.fn().mockReturnValue(of({
        content: [
          {
            id: 'gap-1',
            tenantId: 'TENANT-001',
            patientId: 'p1',
            measureId: 'COL-001',
            measureName: 'COL - Colorectal Cancer Screening',
            gapCategory: 'SCREENING',
            priority: 'HIGH',
            gapDescription: 'Screening overdue',
            dueDate: '2025-01-01',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 200,
      })),
    } as any;

    mockMatDialog = {
      open: jest.fn(),
    } as any;

    mockSnackBar = {
      open: jest.fn(),
    } as any;

    mockRouter = {
      navigate: jest.fn(),
    } as any;

    const mockCareGapService = {
      getCareGaps: jest.fn().mockReturnValue(of(mockCareGapData.content)),
      getCareGapsPage: jest.fn().mockReturnValue(of(mockCareGapData)),
      closeCareGap: jest.fn().mockReturnValue(of({})),
      closeCareGaps: jest.fn().mockReturnValue(of({})),
    };

    const mockMatDialog = {
      open: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [
        CareGapManagerComponent,
        ReactiveFormsModule,
        FormsModule,
        BrowserAnimationsModule,
      ],
      providers: [
        { provide: LoggerService, useValue: createMockLoggerService() },
        { provide: PatientService, useValue: mockPatientService },
        { provide: MeasureService, useValue: mockMeasureService },
        { provide: CareGapService, useValue: mockCareGapService },
        { provide: DialogService, useValue: mockDialogService },
        { provide: MatDialog, useValue: mockMatDialog },
        { provide: MatSnackBar, useValue: mockSnackBar },
        { provide: Router, useValue: mockRouter },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CareGapManagerComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with default values', () => {
    expect(component.careGaps).toEqual([]);
    expect(component.loading).toBe(false);
    expect(component.error).toBeNull();
    expect(component.searchTerm).toBe('');
  });

  it('should load care gaps on init', () => {
    component.ngOnInit();
    expect(component.careGaps.length).toBeGreaterThan(0);
  });

  it('should calculate summary statistics', () => {
    component.ngOnInit();
    expect(component.summary).toBeTruthy();
    expect(component.summary?.totalGaps).toBeGreaterThanOrEqual(0);
  });

  it('should apply filters correctly', () => {
    component.ngOnInit();
    const initialCount = component.filteredGaps.length;

    component.filterForm.patchValue({ urgency: 'high' });
    component.applyFilters();

    expect(component.filteredGaps.every((gap) => gap.urgency === 'high')).toBe(true);
    expect(component.filteredGaps.length).toBeLessThanOrEqual(initialCount);
  });

  it('should filter by search term and days overdue range', () => {
    component.ngOnInit();
    component.searchTerm = 'john';
    component.filterForm.patchValue({ daysOverdueMin: 40, daysOverdueMax: 50 });

    component.applyFilters();

    expect(component.filteredGaps.every((gap) => gap.patientName.toLowerCase().includes('john'))).toBe(true);
    expect(component.filteredGaps.every((gap) => gap.daysOverdue >= 40 && gap.daysOverdue <= 50)).toBe(true);
  });

  it('should debounce search input changes', () => {
    jest.useFakeTimers();
    const applySpy = jest.spyOn(component, 'applyFilters');
    component.ngOnInit();
    component.searchTerm = 'smith';

    component.onSearchChange();
    jest.advanceTimersByTime(300);

    expect(applySpy).toHaveBeenCalled();
    jest.useRealTimers();
  });

  it('should reset filters', () => {
    component.ngOnInit();
    component.filterForm.patchValue({ urgency: 'high' });
    component.searchTerm = 'test';

    component.resetFilters();

    expect(component.searchTerm).toBe('');
    expect(component.filterForm.get('urgency')?.value).toBeNull();
  });

  it('should navigate to patient detail', () => {
    const mockGap = {
      patientId: '1',
      patientName: 'Test Patient',
      mrn: 'MRN001',
      gapType: 'screening' as const,
      gapDescription: 'Test gap',
      daysOverdue: 30,
      urgency: 'high' as const,
      measureName: 'TEST',
    };

    component.viewPatientDetail(mockGap);

    expect(mockRouter.navigate).toHaveBeenCalledWith(
      ['/patients', '1'],
      expect.objectContaining({
        queryParams: expect.objectContaining({ tab: 'care-gaps', source: 'care-gap-manager' }),
      })
    );
  });

  it('should open intervention form', () => {
    const mockGap = {
      patientId: '1',
      patientName: 'Test Patient',
      mrn: 'MRN001',
      gapType: 'screening' as const,
      gapDescription: 'Test gap',
      daysOverdue: 30,
      urgency: 'high' as const,
      measureName: 'TEST',
    };

    component.openInterventionForm(mockGap);

    expect(component.showInterventionForm).toBe(true);
    expect(component.selectedGap).toEqual(mockGap);
  });

  it('should open closure form', () => {
    const mockGap = {
      patientId: '1',
      patientName: 'Test Patient',
      mrn: 'MRN001',
      gapType: 'screening' as const,
      gapDescription: 'Test gap',
      daysOverdue: 30,
      urgency: 'high' as const,
      measureName: 'TEST',
    };

    component.openClosureForm(mockGap);

    expect(component.showClosureForm).toBe(true);
    expect(component.selectedGap).toEqual(mockGap);
  });

  it('should submit intervention when form is valid', () => {
    const mockGap = {
      patientId: '1',
      patientName: 'Test Patient',
      mrn: 'MRN001',
      gapType: 'screening' as const,
      gapDescription: 'Test gap',
      daysOverdue: 30,
      urgency: 'high' as const,
      measureName: 'TEST',
    };

    component.selectedGap = mockGap;
    component.interventionForm.patchValue({
      interventionType: 'call',
      description: 'Follow-up call',
    });

    component.submitIntervention();

    expect(component.showInterventionForm).toBe(false);
  });

  it('should not submit intervention when form is invalid', () => {
    component.selectedGap = {
      patientId: '1',
      patientName: 'Test Patient',
      mrn: 'MRN001',
      gapType: 'screening' as const,
      gapDescription: 'Test gap',
      daysOverdue: 30,
      urgency: 'high' as const,
      measureName: 'TEST',
    };
    component.showInterventionForm = true;

    component.interventionForm.patchValue({
      interventionType: '',
      description: '',
    });

    component.submitIntervention();

    expect(component.showInterventionForm).toBe(true);
  });

  it('should submit closure when form is valid', () => {
    const mockGap = {
      patientId: '1',
      patientName: 'Test Patient',
      mrn: 'MRN001',
      gapType: 'screening' as const,
      gapDescription: 'Test gap',
      daysOverdue: 30,
      urgency: 'high' as const,
      measureName: 'TEST',
    };

    component.careGaps = [mockGap];
    component.selectedGap = mockGap;
    component.closureForm.patchValue({
      closureReason: 'completed',
      closureDate: '2024-12-01',
    });

    const initialLength = component.careGaps.length;
    component.submitClosure();

    expect(component.careGaps.length).toBe(initialLength - 1);
    expect(component.showClosureForm).toBe(false);
  });

  it('should not submit closure when form is invalid', () => {
    component.selectedGap = {
      patientId: '1',
      patientName: 'Test Patient',
      mrn: 'MRN001',
      gapType: 'screening' as const,
      gapDescription: 'Test gap',
      daysOverdue: 30,
      urgency: 'high' as const,
      measureName: 'TEST',
    };
    component.showClosureForm = true;

    component.closureForm.patchValue({
      closureReason: '',
      closureDate: '',
    });

    component.submitClosure();

    expect(component.showClosureForm).toBe(true);
  });

  it('should handle bulk close gaps', () => {
    mockDialogService.confirm.mockReturnValue(of(true));

    component.ngOnInit();
    const initialLength = component.careGaps.length;
    const gapsToSelect = component.careGaps.slice(0, Math.min(2, initialLength));
    gapsToSelect.forEach((gap) => component.selection.select(gap));

    component.bulkCloseGaps();

    expect(mockDialogService.confirm).toHaveBeenCalled();
    expect(component.careGaps.length).toBe(initialLength - gapsToSelect.length);
  });

  it('should assign selected gaps to care manager', () => {
    component.ngOnInit();
    const firstGap = component.careGaps[0];
    component.selection.select(firstGap);
    (component as any).dialog = mockMatDialog;
    mockMatDialog.open.mockReturnValue({
      afterClosed: () => of({
        success: true,
        actionType: 'assign-care-manager',
        formData: { careManager: 'cm1' },
        gapIds: ['gap-1'],
      }),
    } as any);

    component.bulkAssignCareManager();

    expect(mockMatDialog.open).toHaveBeenCalled();
    expect(component.careGaps[0].careManager).toBe('cm1');
    expect(component.selection.selected.length).toBe(0);
  });

  it('should export selected gaps to excel', () => {
    const downloadSpy = jest.spyOn(CSVHelper, 'downloadCSV').mockImplementation(() => undefined);
    component.ngOnInit();
    component.selection.select(component.careGaps[0]);

    component.exportSelectedToExcel();

    expect(downloadSpy).toHaveBeenCalled();
    expect(downloadSpy.mock.calls[0][0]).toContain('.xlsx');
    downloadSpy.mockRestore();
  });

  it('should not bulk close when no selection', () => {
    component.ngOnInit();
    component.selection.clear();

    component.bulkCloseGaps();

    expect(mockDialogService.confirm).not.toHaveBeenCalled();
  });

  it('should not close gaps when bulk close is cancelled', () => {
    mockDialogService.confirm.mockReturnValue(of(false));
    component.ngOnInit();
    const initialLength = component.careGaps.length;
    component.selection.select(component.careGaps[0]);

    component.bulkCloseGaps();

    expect(component.careGaps.length).toBe(initialLength);
  });

  it('should clear selection', () => {
    component.ngOnInit();
    component.selection.select(component.careGaps[0]);

    component.clearSelection();

    expect(component.selection.selected.length).toBe(0);
  });

  it('should toggle selection helpers', () => {
    component.ngOnInit();
    component.dataSource.data = component.careGaps;

    expect(component.isAllSelected()).toBe(false);
    component.masterToggle();
    expect(component.isAllSelected()).toBe(true);
    component.masterToggle();
    expect(component.isAllSelected()).toBe(false);
  });

  it('should format dates and urgency badge class', () => {
    expect(component.formatDate(undefined)).toBe('N/A');
    expect(component.getUrgencyBadgeClass('high')).toBe('urgency-high');
    expect(component.getUrgencyBadgeClass('medium')).toBe('urgency-medium');
    expect(component.getUrgencyBadgeClass('low')).toBe('urgency-low');
    expect(component.getUrgencyBadgeClass('unknown' as any)).toBe('urgency-low');
  });

  it('should render checkbox labels correctly', () => {
    component.ngOnInit();
    component.dataSource.data = component.careGaps;
    const row = component.careGaps[0];

    expect(component.checkboxLabel()).toContain('select all');
    component.selection.select(row);
    expect(component.checkboxLabel(row)).toContain(`deselect row ${row.patientId}`);
  });

  it('should format days overdue correctly', () => {
    expect(component.formatDaysOverdue(0)).toContain('today');
    expect(component.formatDaysOverdue(1)).toContain('1 day');
    // 30 days returns "1 month overdue" based on implementation
    expect(component.formatDaysOverdue(30)).toContain('month');
  });

  it('should get correct urgency color', () => {
    expect(component.getUrgencyColor('high')).toBe('warn');
    expect(component.getUrgencyColor('medium')).toBe('accent');
    expect(component.getUrgencyColor('low')).toBe('primary');
  });

  afterEach(() => {
    fixture.destroy();
  });
});
