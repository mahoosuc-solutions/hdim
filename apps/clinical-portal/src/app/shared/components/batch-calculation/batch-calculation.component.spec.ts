import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BatchCalculationComponent } from './batch-calculation.component';
import { BatchCalculationService } from '../../../services/batch-calculation.service';
import { of, throwError, Subject } from 'rxjs';

describe('BatchCalculationComponent', () => {
  let component: BatchCalculationComponent;
  let fixture: ComponentFixture<BatchCalculationComponent>;
  let batchCalculationService: jest.Mocked<BatchCalculationService>;

  beforeEach(async () => {
    batchCalculationService = {
      startBatchCalculation: jest.fn(),
      pollJobStatus: jest.fn(),
      cancelJob: jest.fn(),
      getAllJobs: jest.fn(),
      getStatusColor: jest.fn(),
      formatDuration: jest.fn(),
    } as unknown as jest.Mocked<BatchCalculationService>;

    await TestBed.configureTestingModule({
      imports: [BatchCalculationComponent],
      providers: [{ provide: BatchCalculationService, useValue: batchCalculationService }],
    }).compileComponents();

    fixture = TestBed.createComponent(BatchCalculationComponent);
    component = fixture.componentInstance;

    // Mock the getAllJobs method to return empty array
    batchCalculationService.getAllJobs.mockReturnValue(of([]));

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with no active job', () => {
    expect(component.activeJob).toBeNull();
    expect(component.isCalculating).toBe(false);
  });

  it('should format duration correctly', () => {
    batchCalculationService.formatDuration.mockReturnValue('1.9s');
    expect(component.formatDuration('PT1.906862298S')).toBe('1.9s');
    expect(batchCalculationService.formatDuration).toHaveBeenCalledWith('PT1.906862298S');
  });

  it('should calculate success rate correctly', () => {
    const mockJob: any = {
      totalCalculations: 100,
      successfulCalculations: 75
    };
    expect(component.getSuccessRate(mockJob)).toBe(75);
  });

  it('should handle zero total calculations', () => {
    const mockJob: any = {
      totalCalculations: 0,
      successfulCalculations: 0
    };
    expect(component.getSuccessRate(mockJob)).toBe(0);
  });

  it('starts batch calculation and polls for status', () => {
    const status$ = of({ jobId: 'job-1', status: 'COMPLETED' });
    batchCalculationService.startBatchCalculation.mockReturnValue(of({ jobId: 'job-1' }));
    batchCalculationService.pollJobStatus.mockReturnValue(status$);
    batchCalculationService.getAllJobs.mockReturnValue(of([]));

    component.startBatchCalculation();

    expect(component.isLoading).toBe(false);
    expect(component.isCalculating).toBe(false);
    expect(batchCalculationService.startBatchCalculation).toHaveBeenCalled();
    expect(batchCalculationService.pollJobStatus).toHaveBeenCalledWith('job-1', 1000);
  });

  it('does not start batch calculation when already running', () => {
    component.isCalculating = true;

    component.startBatchCalculation();

    expect(batchCalculationService.startBatchCalculation).not.toHaveBeenCalled();
  });

  it('handles start batch calculation error', () => {
    batchCalculationService.startBatchCalculation.mockReturnValue(
      throwError(() => ({ error: { message: 'Start failed' } }))
    );

    component.startBatchCalculation();

    expect(component.isLoading).toBe(false);
    expect(component.error).toBe('Start failed');
  });

  it('handles polling errors', () => {
    batchCalculationService.pollJobStatus.mockReturnValue(
      throwError(() => new Error('polling failed'))
    );

    (component as any).pollJobStatus('job-1');

    expect(component.error).toBe('Failed to get job status');
    expect(component.isCalculating).toBe(false);
  });

  it('stops polling when job completes', () => {
    const historySpy = jest.spyOn(component, 'loadJobHistory');
    batchCalculationService.pollJobStatus.mockReturnValue(
      of({ jobId: 'job-1', status: 'COMPLETED' } as any)
    );

    (component as any).pollJobStatus('job-1');

    expect(component.isCalculating).toBe(false);
    expect(historySpy).toHaveBeenCalled();
  });

  it('loads history and resumes running job', () => {
    const pollSubject = new Subject<any>();
    batchCalculationService.getAllJobs.mockReturnValue(of([
      { jobId: 'running', status: 'CALCULATING' },
    ] as any));
    batchCalculationService.pollJobStatus.mockReturnValue(pollSubject.asObservable());

    component.loadJobHistory();

    expect(component.activeJob?.jobId).toBe('running');
    expect(component.isCalculating).toBe(true);
    expect(batchCalculationService.pollJobStatus).toHaveBeenCalledWith('running', 1000);
    pollSubject.complete();
  });

  it('cancels active job and refreshes history', () => {
    component.activeJob = { jobId: 'job-2' } as any;
    batchCalculationService.cancelJob.mockReturnValue(of({}));
    batchCalculationService.getAllJobs.mockReturnValue(of([]));

    component.cancelJob();

    expect(batchCalculationService.cancelJob).toHaveBeenCalledWith('job-2');
    expect(component.isCalculating).toBe(false);
  });

  it('does nothing when cancel is called without an active job', () => {
    component.activeJob = null;

    component.cancelJob();

    expect(batchCalculationService.cancelJob).not.toHaveBeenCalled();
  });

  it('handles cancel job errors', () => {
    component.activeJob = { jobId: 'job-3' } as any;
    batchCalculationService.cancelJob.mockReturnValue(
      throwError(() => ({ error: { message: 'Cancel failed' } }))
    );

    component.cancelJob();

    expect(component.error).toBe('Cancel failed');
  });

  it('handles job history load errors', () => {
    batchCalculationService.getAllJobs.mockReturnValue(
      throwError(() => new Error('history failed'))
    );

    component.loadJobHistory();

    expect(component.jobHistory).toEqual([]);
  });

  it('shows and closes job errors', () => {
    component.showJobErrors({ errors: ['e1', 'e2'] } as any);

    expect(component.showErrors).toBe(true);
    expect(component.selectedJobErrors).toEqual(['e1', 'e2']);

    component.closeErrors();

    expect(component.showErrors).toBe(false);
    expect(component.selectedJobErrors).toEqual([]);
  });

  it('returns status class and formats timestamps', () => {
    batchCalculationService.getStatusColor.mockReturnValue('status-ok');

    expect(component.getStatusClass('COMPLETED' as any)).toBe('status-ok');
    expect(component.formatTimestamp(null)).toBe('-');
    expect(component.formatTimestamp('2024-01-02T10:00:00Z')).toContain('2024');
  });

  it('toggles and refreshes job history', () => {
    const historySpy = jest.spyOn(component, 'loadJobHistory');
    const initialState = component.showHistory;

    component.toggleHistory();
    expect(component.showHistory).toBe(!initialState);

    component.refreshHistory();
    expect(historySpy).toHaveBeenCalled();
  });
});
