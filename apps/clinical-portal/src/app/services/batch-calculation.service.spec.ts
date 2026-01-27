import { of } from 'rxjs';
import { BatchCalculationService, BatchJobStatus } from './batch-calculation.service';

const createJob = (status: BatchJobStatus) => ({
  jobId: 'job-1',
  tenantId: 't1',
  status,
  createdBy: 'user',
  startedAt: new Date().toISOString(),
  completedAt: null,
  totalPatients: 10,
  totalMeasures: 5,
  totalCalculations: 50,
  completedCalculations: 50,
  successfulCalculations: 50,
  failedCalculations: 0,
  progressPercent: 100,
  duration: 'PT1.0S',
});

describe('BatchCalculationService', () => {
  let http: { get: jest.Mock; post: jest.Mock };
  let service: BatchCalculationService;

  beforeEach(() => {
    http = { get: jest.fn(), post: jest.fn() };
    service = new BatchCalculationService(http as any);
  });

  it('starts calculations and fetches jobs', (done) => {
    http.post.mockReturnValueOnce(of({ jobId: 'job-1', status: 'PENDING', message: 'ok', tenantId: 't1' }));
    http.get.mockReturnValueOnce(of(createJob(BatchJobStatus.COMPLETED)));
    http.get.mockReturnValueOnce(of([createJob(BatchJobStatus.PENDING)]));

    service.startBatchCalculation('http://fhir', 'user').subscribe((response) => {
      expect(response.jobId).toBe('job-1');

      service.getJobStatus('job-1').subscribe((job) => {
        expect(job.status).toBe(BatchJobStatus.COMPLETED);

        service.getAllJobs().subscribe((jobs) => {
          expect(jobs.length).toBe(1);
          done();
        };
      });
    });
  });

  it('polls job status until completion', (done) => {
    jest.useFakeTimers();
    const completedJob = createJob(BatchJobStatus.COMPLETED);
    jest.spyOn(service, 'getJobStatus').mockReturnValue(of(completedJob));

    service.pollJobStatus('job-1', 10).subscribe({
      next: (job) => {
        expect(job.status).toBe(BatchJobStatus.COMPLETED);
      },
      complete: () => {
        jest.useRealTimers();
        done();
      },
    };
  });

  it('polls job status until completion after pending state', (done) => {
    jest.useFakeTimers();
    const pendingJob = createJob(BatchJobStatus.CALCULATING);
    const completedJob = createJob(BatchJobStatus.COMPLETED);
    const statusSpy = jest
      .spyOn(service, 'getJobStatus')
      .mockReturnValueOnce(of(pendingJob))
      .mockReturnValueOnce(of(completedJob));

    const results: BatchJobStatus[] = [];
    service.pollJobStatus('job-1', 10).subscribe({
      next: (job) => results.push(job.status),
      complete: () => {
        expect(results).toEqual([BatchJobStatus.CALCULATING, BatchJobStatus.COMPLETED]);
        statusSpy.mockRestore();
        jest.useRealTimers();
        done();
      },
    };

    jest.advanceTimersByTime(10);
  });

  it('formats duration and status colors', () => {
    expect(service.formatDuration('PT1.5S')).toBe('1.5s');
    expect(service.formatDuration('')).toBe('-');
    expect(service.getStatusColor(BatchJobStatus.COMPLETED)).toContain('green');
  };

  it('formats hour and minute durations and falls back', () => {
    expect(service.formatDuration('PT2.5H')).toBe('2.5h');
    expect(service.formatDuration('PT3.0M')).toBe('3.0m');
    expect(service.formatDuration('P1D')).toBe('P1D');
  };

  it('returns status color for each status', () => {
    expect(service.getStatusColor(BatchJobStatus.PENDING)).toContain('blue');
    expect(service.getStatusColor(BatchJobStatus.FETCHING_PATIENTS)).toContain('blue');
    expect(service.getStatusColor(BatchJobStatus.CALCULATING)).toContain('yellow');
    expect(service.getStatusColor(BatchJobStatus.FAILED)).toContain('red');
    expect(service.getStatusColor(BatchJobStatus.CANCELLED)).toContain('gray');
  };

  it('cancels a job', (done) => {
    http.post.mockReturnValueOnce(of({ jobId: 'job-2', status: 'CANCELLED', message: 'ok' }));

    service.cancelJob('job-2').subscribe((response) => {
      expect(response.status).toBe('CANCELLED');
      done();
    };
  });
});
