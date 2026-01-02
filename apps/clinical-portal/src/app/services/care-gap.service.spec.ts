import { BehaviorSubject, firstValueFrom, of, throwError } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import { CareGapService, CareGapStatus, CareGapType, GapPriority } from './care-gap.service';
import { ApiService } from './api.service';
import { LoggerService } from './logger.service';

const mockLoggerService = {
  withContext: jest.fn().mockReturnValue({
    debug: jest.fn(),
    info: jest.fn(),
    warn: jest.fn(),
    error: jest.fn(),
  }),
  debug: jest.fn(),
  info: jest.fn(),
  warn: jest.fn(),
  error: jest.fn(),
};

const createGap = () => ({
  id: 'gap-1',
  patientId: 'patient-1',
  measureId: 'measure-1',
  measureName: 'Measure',
  gapType: CareGapType.PREVENTIVE_SCREENING,
  status: CareGapStatus.OPEN,
  priority: GapPriority.HIGH,
  priorityScore: 90,
  description: 'desc',
  recommendation: 'rec',
  detectedDate: new Date().toISOString(),
});

describe('CareGapService', () => {
  let apiService: ApiService;
  let service: CareGapService;

  beforeEach(() => {
    apiService = {
      get: jest.fn(),
      post: jest.fn(),
    } as unknown as ApiService;
    const mockHttpClient = {} as any;
    service = new CareGapService(mockHttpClient, apiService, mockLoggerService as unknown as LoggerService);
  });

  it('caches patient gaps and returns cached results', async () => {
    (apiService.get as jest.Mock).mockReturnValueOnce(new BehaviorSubject([createGap()]));

    const first = await firstValueFrom(service.getPatientCareGaps('patient-1'));
    expect(first.length).toBe(1);

    const second = await firstValueFrom(service.getPatientCareGaps('patient-1'));
    expect(second.length).toBe(1);
    expect(apiService.get).toHaveBeenCalledTimes(1);
  });

  it('detects gaps and emits update', async () => {
    (apiService.post as jest.Mock).mockReturnValueOnce(new BehaviorSubject([createGap()]));

    const updatePromise = firstValueFrom(service.gapUpdates$.pipe(filter(Boolean), take(1)));
    const gaps = await firstValueFrom(service.detectGapsForPatient('patient-1'));

    expect(gaps.length).toBe(1);
    const update = await updatePromise;
    expect(update?.type).toBe('detected');
  });

  it('propagates errors when detecting gaps', async () => {
    (apiService.post as jest.Mock).mockReturnValueOnce(throwError(() => new Error('detect fail')));

    await expect(firstValueFrom(service.detectGapsForPatient('patient-1'))).rejects.toThrow('detect fail');
  });

  it('detects gaps in batch and emits update', async () => {
    (apiService.post as jest.Mock).mockReturnValueOnce(of({
      batchId: 'batch-1',
      totalPatients: 2,
      successCount: 2,
      failureCount: 0,
      patientGaps: [
        { patientId: 'patient-1', gaps: [createGap()] },
        { patientId: 'patient-2', gaps: [createGap()] },
      ],
      errors: [],
    }));

    const updatePromise = firstValueFrom(service.gapUpdates$.pipe(filter(Boolean), take(1)));
    const result = await firstValueFrom(service.detectGapsBatch(['patient-1', 'patient-2']));

    expect(result.batchId).toBe('batch-1');
    const update = await updatePromise;
    expect(update?.type).toBe('batch-detected');
  });

  it('propagates errors when detecting gaps in batch', async () => {
    (apiService.post as jest.Mock).mockReturnValueOnce(throwError(() => new Error('batch fail')));

    await expect(firstValueFrom(service.detectGapsBatch(['patient-1']))).rejects.toThrow('batch fail');
  });

  it('closes gap and invalidates cache', async () => {
    const invalidateSpy = jest.spyOn(service, 'invalidatePatientCache');
    (apiService.post as jest.Mock).mockReturnValueOnce(of({
      gapId: 'gap-1',
      patientId: 'patient-1',
      closedDate: 'now',
      closedBy: 'me',
      reason: 'done',
    }));

    const updatePromise = firstValueFrom(service.gapUpdates$.pipe(filter(Boolean), take(1)));
    await firstValueFrom(service.closeGap('gap-1', { reason: 'done', closedBy: 'me' }));

    expect(invalidateSpy).toHaveBeenCalledWith('patient-1');
    const update = await updatePromise;
    expect(update?.type).toBe('closed');
  });

  it('bulk closes gaps and invalidates cache', async () => {
    const invalidateSpy = jest.spyOn(service, 'invalidatePatientCache');
    (apiService.post as jest.Mock).mockReturnValueOnce(new BehaviorSubject({
      successCount: 1,
      failureCount: 0,
      closedGaps: [{ gapId: 'gap-1', patientId: 'patient-1', closedDate: 'now', closedBy: 'me', reason: 'done' }],
      errors: [],
    }));

    await firstValueFrom(service.bulkCloseGaps(['gap-1'], { reason: 'done', closedBy: 'me' }));

    expect(invalidateSpy).toHaveBeenCalledWith('patient-1');
  });

  it('emits updates for bulk closures', async () => {
    (apiService.post as jest.Mock).mockReturnValueOnce(of({
      successCount: 1,
      failureCount: 0,
      closedGaps: [{ gapId: 'gap-1', patientId: 'patient-1', closedDate: 'now', closedBy: 'me', reason: 'done' }],
      errors: [],
    }));

    const updatePromise = firstValueFrom(service.gapUpdates$.pipe(filter(Boolean), take(1)));
    await firstValueFrom(service.bulkCloseGaps(['gap-1'], { reason: 'done', closedBy: 'me' }));

    const update = await updatePromise;
    expect(update?.type).toBe('bulk-closed');
  });

  it('assigns interventions and invalidates cache', async () => {
    const invalidateSpy = jest.spyOn(service, 'invalidatePatientCache');
    (apiService.post as jest.Mock).mockReturnValueOnce(of({
      gapId: 'gap-1',
      patientId: 'patient-1',
      intervention: { type: 'OUTREACH', description: 'Call patient' },
      assignedDate: 'now',
      status: 'assigned',
    }));

    await firstValueFrom(service.assignIntervention('gap-1', { type: 'OUTREACH', description: 'Call patient' }));

    expect(invalidateSpy).toHaveBeenCalledWith('patient-1');
  });

  it('emits updates for assigned interventions', async () => {
    (apiService.post as jest.Mock).mockReturnValueOnce(of({
      gapId: 'gap-1',
      patientId: 'patient-1',
      intervention: { type: 'OUTREACH', description: 'Call patient' },
      assignedDate: 'now',
      status: 'assigned',
    }));

    const updatePromise = firstValueFrom(service.gapUpdates$.pipe(filter(Boolean), take(1)));
    await firstValueFrom(service.assignIntervention('gap-1', { type: 'OUTREACH', description: 'Call patient' }));

    const update = await updatePromise;
    expect(update?.type).toBe('intervention-assigned');
  });

  it('gets gaps by status', async () => {
    (apiService.get as jest.Mock).mockReturnValueOnce(new BehaviorSubject([createGap()]));

    const gaps = await firstValueFrom(service.getGapsByStatus(CareGapStatus.OPEN));
    expect(gaps.length).toBe(1);
  });

  it('propagates errors for status queries', async () => {
    (apiService.get as jest.Mock).mockReturnValueOnce(throwError(() => new Error('status fail')));

    await expect(firstValueFrom(service.getGapsByStatus(CareGapStatus.OPEN))).rejects.toThrow('status fail');
  });

  it('returns high priority gaps', async () => {
    (apiService.get as jest.Mock).mockReturnValueOnce(of([createGap()]));

    const gaps = await firstValueFrom(service.getHighPriorityGaps(10));
    expect(gaps.length).toBe(1);
  });

  it('propagates errors for high priority queries', async () => {
    (apiService.get as jest.Mock).mockReturnValueOnce(throwError(() => new Error('priority fail')));

    await expect(firstValueFrom(service.getHighPriorityGaps(10))).rejects.toThrow('priority fail');
  });

  it('refreshes cache when refresh is true', async () => {
    (apiService.get as jest.Mock).mockReturnValue(of([createGap()]));

    await firstValueFrom(service.getPatientCareGaps('patient-1'));
    await firstValueFrom(service.getPatientCareGaps('patient-1', true));

    expect(apiService.get).toHaveBeenCalledTimes(2);
  });

  it('expires cache and reloads data', async () => {
    const nowSpy = jest.spyOn(Date, 'now');
    const cacheTimeout = (service as any).cacheTimeout as number;
    nowSpy
      .mockReturnValueOnce(0)
      .mockReturnValueOnce(cacheTimeout + 1)
      .mockReturnValueOnce(cacheTimeout + 2);

    (apiService.get as jest.Mock).mockReturnValue(of([createGap()]));

    await firstValueFrom(service.getPatientCareGaps('patient-1'));
    await firstValueFrom(service.getPatientCareGaps('patient-1'));

    expect(apiService.get).toHaveBeenCalledTimes(2);
    nowSpy.mockRestore();
  });

  it('clears cache to force reloads', async () => {
    (apiService.get as jest.Mock).mockReturnValue(of([createGap()]));

    await firstValueFrom(service.getPatientCareGaps('patient-1'));
    service.clearCache();
    await firstValueFrom(service.getPatientCareGaps('patient-1'));

    expect(apiService.get).toHaveBeenCalledTimes(2);
  });

  it('propagates errors for priority score calls', async () => {
    (apiService.get as jest.Mock).mockReturnValueOnce(throwError(() => new Error('fail')));

    await expect(firstValueFrom(service.getGapPriorityScore('gap-1'))).rejects.toThrow('fail');
  });

  it('returns priority score for a gap', async () => {
    (apiService.get as jest.Mock).mockReturnValueOnce(of({
      gapId: 'gap-1',
      priority: GapPriority.HIGH,
      score: 90,
      factors: {},
      calculation: 'test',
    }));

    const result = await firstValueFrom(service.getGapPriorityScore('gap-1'));
    expect(result.priority).toBe(GapPriority.HIGH);
  });
});
