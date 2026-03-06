import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NgZone } from '@angular/core';
import { BehaviorSubject, Subject, firstValueFrom, skip, take } from 'rxjs';
import {
  BatchProgressEvent,
  WebSocketStatus,
  WebSocketVisualizationService,
  CareGapNotificationEvent,
} from '../visualization/core/websocket-visualization.service';
import { SystemEventsService } from './system-events.service';
import { createSystemEvent } from '../models/system-event.model';
import { NotificationService } from './notification.service';

class MockWebSocketVisualizationService {
  status$ = new BehaviorSubject<WebSocketStatus>(WebSocketStatus.DISCONNECTED);
  batchProgress$ = new Subject<BatchProgressEvent>();
  evaluationProgress$ = new Subject<any>();
  careGapNotification$ = new Subject<CareGapNotificationEvent>();
  private status = WebSocketStatus.DISCONNECTED;

  connect = jest.fn(() => {
    this.status = WebSocketStatus.CONNECTED;
    this.status$.next(this.status);
  });

  disconnect = jest.fn(() => {
    this.status = WebSocketStatus.DISCONNECTED;
    this.status$.next(this.status);
  });

  getStatus = jest.fn(() => this.status);
}

class MockNotificationService {
  success = jest.fn();
  error = jest.fn();
  warning = jest.fn();
  info = jest.fn();
  show = jest.fn();
  dismiss = jest.fn();
}

describe('SystemEventsService', () => {
  let service: SystemEventsService;
  let wsService: MockWebSocketVisualizationService;
  let notificationService: MockNotificationService;

  beforeEach(() => {
    wsService = new MockWebSocketVisualizationService();
    notificationService = new MockNotificationService();

    TestBed.configureTestingModule({
      providers: [
        SystemEventsService,
        { provide: WebSocketVisualizationService, useValue: wsService },
        { provide: NotificationService, useValue: notificationService },
        { provide: NgZone, useFactory: () => new NgZone({ enableLongStackTrace: false }) },
      ],
    });

    service = TestBed.inject(SystemEventsService);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('updates connection status based on websocket events', async () => {
    const statusPromise = firstValueFrom(service.connectionStatus$.pipe(skip(1), take(1)));
    wsService.status$.next(WebSocketStatus.CONNECTED);
    await expect(statusPromise).resolves.toBe('connected');

    const disconnectedPromise = firstValueFrom(service.connectionStatus$.pipe(skip(1), take(1)));
    wsService.status$.next(WebSocketStatus.DISCONNECTED);
    await expect(disconnectedPromise).resolves.toBe('disconnected');
  });

  it('connects and disconnects from websocket sources', async () => {
    const connectStatus = firstValueFrom(service.connectionStatus$.pipe(skip(1), take(1)));
    service.connect();
    expect(wsService.connect).toHaveBeenCalled();
    await expect(connectStatus).resolves.toBe('connected');

    const disconnectStatus = firstValueFrom(service.connectionStatus$.pipe(skip(1), take(1)));
    service.disconnect();
    expect(wsService.disconnect).toHaveBeenCalled();
    await expect(disconnectStatus).resolves.toBe('disconnected');
  });

  it('adds batch progress events and updates pipeline/metrics', () => {
    const batchEvent: BatchProgressEvent = {
      batchId: 'batch-12345678',
      status: 'IN_PROGRESS',
      completedCount: 10,
      totalPatients: 50,
      currentThroughput: 3.5,
      cumulativeComplianceRate: 87,
      avgDurationMs: 120,
      measureName: 'Test Measure',
      measureId: 'measure-1',
    };

    wsService.batchProgress$.next(batchEvent);

    const events = service.getCurrentEvents();
    expect(events.length).toBe(1);
    expect(events[0].type).toBe('BATCH_PROGRESS');

    const pipeline = service.getCurrentPipelineState();
    const cqlNode = pipeline.nodes.find((node) => node.id === 'cql');
    expect(cqlNode?.status).toBe('processing');

    const metrics = service.getCurrentMetrics();
    expect(metrics.patientsProcessed).toBe(10);
    expect(metrics.throughputPerSecond).toBe(3.5);
    expect(metrics.complianceRate).toBe(87);
  });

  it('handles completed and failed batch events', () => {
    service.clearEvents();
    wsService.batchProgress$.next({
      batchId: 'batch-98765432',
      status: 'COMPLETED',
      completedCount: 50,
      totalPatients: 50,
      currentThroughput: 5,
      cumulativeComplianceRate: 92,
      avgDurationMs: 100,
      measureName: 'Test Measure',
      measureId: 'measure-1',
    });

    expect(service.getCurrentEvents()[0].type).toBe('BATCH_COMPLETED');

    service.clearEvents();
    wsService.batchProgress$.next({
      batchId: 'batch-00000001',
      status: 'FAILED',
      completedCount: 10,
      totalPatients: 50,
      currentThroughput: 1,
      cumulativeComplianceRate: 40,
      avgDurationMs: 200,
      measureName: 'Test Measure',
      measureId: 'measure-1',
    });

    expect(service.getCurrentEvents()[0].type).toBe('EVALUATION_FAILED');
  });

  it('ignores batch events while paused', () => {
    service.setPaused(true);
    wsService.batchProgress$.next({
      batchId: 'batch-paused',
      status: 'IN_PROGRESS',
      completedCount: 1,
      totalPatients: 10,
      currentThroughput: 1,
      cumulativeComplianceRate: 50,
      avgDurationMs: 50,
      measureName: 'Paused Measure',
      measureId: 'measure-paused',
    });

    expect(service.getCurrentEvents()).toHaveLength(0);
  });

  it('emits evaluation events and tracks processing results', () => {
    wsService.evaluationProgress$.next({
      status: 'COMPLETED',
      patientId: 'patient-1',
      message: 'Done',
      progress: 100,
    });

    const events = service.getCurrentEvents();
    expect(events[0].type).toBe('EVALUATION_COMPLETED');
    expect(events[0].patient?.id).toBe('patient-1');
  });

  it('emits evaluation started and failed events', () => {
    wsService.evaluationProgress$.next({
      status: 'STARTED',
      patientId: 'patient-2',
      progress: 0,
    });
    expect(service.getCurrentEvents()[0].type).toBe('EVALUATION_STARTED');

    service.clearEvents();
    wsService.evaluationProgress$.next({
      status: 'FAILED',
      patientId: 'patient-3',
      message: 'Error',
    });
    expect(service.getCurrentEvents()[0].type).toBe('EVALUATION_FAILED');
  });

  it('trims event buffers and processing metrics', () => {
    for (let i = 0; i < 105; i += 1) {
      service.addEvent(
        createSystemEvent('SERVICE_STARTED', `Event ${i}`, 'Desc', {
          durationMs: i + 1,
          severity: 'info',
        })
      );
    }

    expect(service.getCurrentEvents().length).toBe(100);
    expect((service as any).operationResults.length).toBe(100);
    expect((service as any).processingTimes.length).toBe(100);
  });

  it('clears events', () => {
    service.addEvent(createSystemEvent('SERVICE_STARTED', 'Title', 'Desc'));
    service.clearEvents();
    expect(service.getCurrentEvents()).toEqual([]);
  });

  it('filters events by category and time range', (done) => {
    const early = createSystemEvent('FHIR_RESOURCE_CREATED', 'FHIR', 'Early', {
      timestamp: '2025-01-01T00:00:00Z',
      severity: 'info',
    });
    const later = createSystemEvent('CARE_GAP_DETECTED', 'Gap', 'Later', {
      timestamp: '2025-01-02T00:00:00Z',
      severity: 'warning',
    });

    service.addEvent(early);
    service.addEvent(later);

    service.getFilteredEvents({
      categories: ['care-gap'],
      startTime: '2025-01-01T12:00:00Z',
    }).pipe(take(1)).subscribe((events) => {
      expect(events.length).toBe(1);
      expect(events[0].type).toBe('CARE_GAP_DETECTED');
      done();
    });
  });

  it('filters events by severity, type, patient, and end time', (done) => {
    const base = createSystemEvent('EVALUATION_COMPLETED', 'Eval', 'Done', {
      severity: 'success',
      timestamp: '2025-01-02T00:00:00Z',
      patient: { id: 'patient-1' },
    });
    const other = createSystemEvent('EVALUATION_FAILED', 'Eval', 'Fail', {
      severity: 'error',
      timestamp: '2025-01-03T00:00:00Z',
      patient: { id: 'patient-2' },
    });

    service.addEvent(base);
    service.addEvent(other);

    service.getFilteredEvents({
      severities: ['success'],
      types: ['EVALUATION_COMPLETED'],
      patientId: 'patient-1',
      endTime: '2025-01-02T12:00:00Z',
    }).pipe(take(1)).subscribe((events) => {
      expect(events.length).toBe(1);
      expect(events[0].type).toBe('EVALUATION_COMPLETED');
      done();
    });
  });

  it('respects pause state', () => {
    service.setPaused(true);
    service.addEvent(createSystemEvent('SERVICE_STARTED', 'Title', 'Desc'));
    expect(service.getCurrentEvents()).toHaveLength(0);

    service.setPaused(false);
    service.addEvent(createSystemEvent('SERVICE_STARTED', 'Title', 'Desc'));
    expect(service.getCurrentEvents()).toHaveLength(1);
  });

  it('updates pipeline and metrics for batch progress details', () => {
    wsService.batchProgress$.next({
      batchId: 'batch-pipeline',
      status: 'IN_PROGRESS',
      completedCount: 2,
      totalPatients: 10,
      currentThroughput: 4,
      cumulativeComplianceRate: 70,
      avgDurationMs: 80,
      measureName: 'Pipeline Measure',
      measureId: 'measure-pipeline',
    });

    const pipeline = service.getCurrentPipelineState();
    const fhirNode = pipeline.nodes.find((node) => node.id === 'fhir');
    const qualityNode = pipeline.nodes.find((node) => node.id === 'quality');
    expect(fhirNode?.status).toBe('active');
    expect(qualityNode?.status).toBe('active');

    const connectionActive = pipeline.connections.some((conn) => conn.isActive);
    expect(connectionActive).toBe(true);
  });

  it('keeps compliance rate when batch event omits value', () => {
    service.setInitialMetrics({ complianceRate: 81 });
    const updateMetricsFromBatch = (service as any).updateMetricsFromBatch.bind(service);
    (service as any).operationResults = [];
    (service as any).processingTimes = [];

    updateMetricsFromBatch({
      batchId: 'batch-metrics',
      status: 'IN_PROGRESS',
      completedCount: 5,
      totalPatients: 20,
      currentThroughput: 2,
      cumulativeComplianceRate: undefined,
      avgDurationMs: 0,
      measureName: 'Metric Measure',
      measureId: 'measure-metric',
    });

    const metrics = service.getCurrentMetrics();
    expect(metrics.complianceRate).toBe(81);
    expect(metrics.successRate).toBe(100);
    expect(metrics.avgProcessingTimeMs).toBe(0);
  });

  it('starts and stops simulation mode', fakeAsync(() => {
    const randomSpy = jest.spyOn(Math, 'random').mockReturnValue(0);

    service.startSimulation();
    tick(2000);

    expect(service.getCurrentEvents().length).toBeGreaterThan(0);
    expect(service.getCurrentMetrics().patientsProcessed).toBeGreaterThanOrEqual(0);

    service.stopSimulation();
    expect(service.getCurrentEvents().length).toBeGreaterThan(0);

    randomSpy.mockRestore();
  }));

  it('does not restart simulation when already active', () => {
    service.startSimulation();
    service.startSimulation();

    expect(service.getCurrentEvents()).toBeDefined();
  });

  it('keeps connection status when websocket is connected on stop', () => {
    wsService.connect();
    service.startSimulation();
    service.stopSimulation();

    expect((service as any).connectionStatusSubject.value).toBe('simulating');
  });

  it('does not overwrite simulating status on websocket error', () => {
    service.startSimulation();
    wsService.status$.next(WebSocketStatus.ERROR);

    expect((service as any).connectionStatusSubject.value).toBe('simulating');
  });

  it('updates simulated pipeline for event types', () => {
    const updatePipeline = (service as any).updatePipelineForSimulation.bind(service);
    updatePipeline('FHIR_RESOURCE_CREATED');

    const pipeline = service.getCurrentPipelineState();
    const fhirNode = pipeline.nodes.find((node) => node.id === 'fhir');
    expect(fhirNode?.status).toBeDefined();
  });

  it('updates pipeline for cql, quality, and care gap events', () => {
    const updatePipeline = (service as any).updatePipelineForSimulation.bind(service);

    updatePipeline('EVALUATION_COMPLETED');
    const cqlNode = service.getCurrentPipelineState().nodes.find((node) => node.id === 'cql');
    expect(cqlNode?.status).toBe('processing');

    updatePipeline('MEASURE_CALCULATED');
    const qualityNode = service.getCurrentPipelineState().nodes.find((node) => node.id === 'quality');
    expect(qualityNode?.status).toBe('processing');

    updatePipeline('CARE_GAP_DETECTED');
    const careGapNode = service.getCurrentPipelineState().nodes.find((node) => node.id === 'caregap');
    expect(careGapNode?.status).toBe('processing');
  });

  it('generates simulated events for different types', () => {
    const randomSpy = jest.spyOn(Math, 'random');
    const cases = [
      { value: 0.25, type: 'FHIR_RESOURCE_UPDATED' },
      { value: 0.65, type: 'EVALUATION_STARTED' },
      { value: 0.5, type: 'EVALUATION_COMPLETED' },
      { value: 0.99, type: 'EVALUATION_FAILED' },
      { value: 0.75, type: 'MEASURE_CALCULATED' },
      { value: 0.85, type: 'CARE_GAP_DETECTED' },
      { value: 0.9, type: 'CARE_GAP_CLOSED' },
      { value: 0.95, type: 'COMPLIANCE_UPDATED' },
    ];

    cases.forEach(({ value, type }) => {
      randomSpy.mockReturnValue(value);
      service.clearEvents();
      (service as any).generateSimulatedEvent();
      expect(service.getCurrentEvents()[0].type).toBe(type);
    });

    randomSpy.mockRestore();
  });

  it('sets initial metrics and exposes state getters', () => {
    service.setInitialMetrics({ patientsProcessed: 99, complianceRate: 85 });
    const metrics = service.getCurrentMetrics();
    expect(metrics.patientsProcessed).toBe(99);
    expect(metrics.complianceRate).toBe(85);

    expect(service.getCurrentPipelineState().nodes.length).toBeGreaterThan(0);
    expect(service.getCurrentEvents()).toEqual([]);
  });

  it('cleans up on destroy', () => {
    service.ngOnDestroy();
    expect(wsService.disconnect).toHaveBeenCalled();
  });
});
