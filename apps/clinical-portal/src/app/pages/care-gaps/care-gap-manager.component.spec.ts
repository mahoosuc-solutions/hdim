import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { CareGapManagerComponent } from './care-gap-manager.component';
import { PatientService } from '../../services/patient.service';
import { MeasureService } from '../../services/measure.service';
import { DialogService } from '../../services/dialog.service';

describe('CareGapManagerComponent', () => {
  let component: CareGapManagerComponent;
  let fixture: ComponentFixture<CareGapManagerComponent>;
  let mockPatientService: jest.Mocked<PatientService>;
  let mockMeasureService: jest.Mocked<MeasureService>;
  let mockDialogService: jest.Mocked<DialogService>;
  let mockRouter: jest.Mocked<Router>;

  beforeEach(async () => {
    // Create mock services
    mockPatientService = {
      getPatientsSummary: jest.fn(),
      getPatient: jest.fn(),
    } as any;

    mockMeasureService = {
      getCareGaps: jest.fn(),
      closeCareGap: jest.fn(),
    } as any;

    mockDialogService = {
      confirm: jest.fn(),
    } as any;

    mockRouter = {
      navigate: jest.fn(),
    } as any;

    await TestBed.configureTestingModule({
      imports: [
        CareGapManagerComponent,
        ReactiveFormsModule,
        FormsModule,
        BrowserAnimationsModule,
      ],
      providers: [
        { provide: PatientService, useValue: mockPatientService },
        { provide: MeasureService, useValue: mockMeasureService },
        { provide: DialogService, useValue: mockDialogService },
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

    expect(mockRouter.navigate).toHaveBeenCalledWith(['/patients', '1']);
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

  it('should handle bulk close gaps', () => {
    mockDialogService.confirm.mockReturnValue(of(true));

    component.ngOnInit();
    const initialLength = component.careGaps.length;
    const gapsToSelect = component.careGaps.slice(0, 2);
    gapsToSelect.forEach((gap) => component.selection.select(gap));

    component.bulkCloseGaps();

    expect(mockDialogService.confirm).toHaveBeenCalled();
    expect(component.careGaps.length).toBe(initialLength - 2);
  });

  it('should clear selection', () => {
    component.ngOnInit();
    component.selection.select(component.careGaps[0]);

    component.clearSelection();

    expect(component.selection.selected.length).toBe(0);
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
