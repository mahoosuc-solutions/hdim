import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { TestPreviewDialogComponent } from './test-preview-dialog.component';
import { CustomMeasureService } from '../../../services/custom-measure.service';

describe('TestPreviewDialogComponent', () => {
  let fixture: ComponentFixture<TestPreviewDialogComponent>;
  let component: TestPreviewDialogComponent;
  let dialogRef: { close: jest.Mock };
  let customMeasureService: jest.Mocked<CustomMeasureService>;

  beforeEach(async () => {
    dialogRef = { close: jest.fn() };
    customMeasureService = {
      testMeasure: jest.fn(),
    } as unknown as jest.Mocked<CustomMeasureService>;

    await TestBed.configureTestingModule({
      imports: [TestPreviewDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { measureId: 'm1', measureName: 'Measure A' } },
        { provide: CustomMeasureService, useValue: customMeasureService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TestPreviewDialogComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('auto-runs the test on init and maps results', () => {
    customMeasureService.testMeasure.mockReturnValue(of({
      results: [
        {
          patientId: 'p1',
          patientName: 'Alice',
          mrn: 'MRN-1',
          outcome: 'pass',
          inPopulation: true,
          inDenominator: true,
          inNumerator: true,
          details: ['ok'],
          executionTimeMs: 50,
        },
      ],
    }));

    fixture.detectChanges();

    expect(component.loading).toBe(false);
    expect(component.testResults).toHaveLength(1);
    expect(component.totalExecutionTimeMs).toBe(50);
  });

  it('falls back to sample data on error', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    customMeasureService.testMeasure.mockReturnValue(throwError(() => new Error('fail')));

    component.runTest();

    expect(warnSpy).toHaveBeenCalled();
    expect(component.loading).toBe(false);
    expect(component.errorMessage).toContain('Using sample data');
    expect(component.testResults).toHaveLength(5);
  });

  it('returns icons and counts from results', () => {
    component.testResults = [
      { patientId: '1', patientName: 'A', mrn: '1', outcome: 'pass', inPopulation: true, inDenominator: true, inNumerator: true, details: [] },
      { patientId: '2', patientName: 'B', mrn: '2', outcome: 'fail', inPopulation: true, inDenominator: true, inNumerator: false, details: [] },
      { patientId: '3', patientName: 'C', mrn: '3', outcome: 'not-eligible', inPopulation: false, inDenominator: false, inNumerator: false, details: [] },
    ];

    expect(component.getOutcomeIcon('pass')).toBe('check_circle');
    expect(component.getOutcomeIcon('fail')).toBe('cancel');
    expect(component.getOutcomeIcon('not-eligible')).toBe('info');
    expect(component.getOutcomeIcon('other')).toBe('help');
    expect(component.getPassCount()).toBe(1);
    expect(component.getFailCount()).toBe(1);
    expect(component.getNotEligibleCount()).toBe(1);
  });

  it('unsubscribes from pending test on destroy', () => {
    const unsubscribe = jest.fn();
    (component as any).testSubscription = { unsubscribe };

    component.ngOnDestroy();

    expect(unsubscribe).toHaveBeenCalled();
  });

  it('cancels prior request before starting a new test', () => {
    const unsubscribe = jest.fn();
    (component as any).testSubscription = { unsubscribe };
    customMeasureService.testMeasure.mockReturnValue(of({ results: [] }));

    component.runTest();

    expect(unsubscribe).toHaveBeenCalled();
  });
});
