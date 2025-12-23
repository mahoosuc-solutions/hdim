import { Injectable, NgZone, OnDestroy } from '@angular/core';
import { BehaviorSubject, Subject, Observable, interval, Subscription } from 'rxjs';
import { distinctUntilChanged, share, takeUntil, map } from 'rxjs/operators';
import {
  SystemEvent,
  SystemEventType,
  LiveMetrics,
  PipelineState,
  PipelineNode,
  PipelineConnection,
  EventFilterOptions,
  DEFAULT_PIPELINE_NODES,
  DEFAULT_PIPELINE_CONNECTIONS,
  DEFAULT_LIVE_METRICS,
  createSystemEvent,
  getCategoryFromType,
} from '../models/system-event.model';
import { WebSocketVisualizationService, WebSocketStatus, BatchProgressEvent, CareGapNotificationEvent } from '../visualization/core/websocket-visualization.service';
import { NotificationService } from './notification.service';
import { API_CONFIG } from '../config/api.config';

/**
 * System Events Service
 *
 * Aggregates real-time events from multiple sources (WebSocket, Kafka, polling)
 * and provides unified streams for visualization components.
 *
 * Features:
 * - WebSocket integration for real-time updates
 * - Event buffering (max 100 events)
 * - Pipeline state management
 * - Live metrics calculation
 * - Event filtering
 * - Simulation mode for testing/demo
 */
@Injectable({
  providedIn: 'root'
})
export class SystemEventsService implements OnDestroy {
  // Configuration
  private readonly MAX_EVENTS = 100;
  private readonly METRICS_UPDATE_INTERVAL = 1000; // 1 second

  // Destroy subject for cleanup
  private destroy$ = new Subject<void>();

  // Event buffer
  private eventsBuffer: SystemEvent[] = [];
  private eventsSubject = new BehaviorSubject<SystemEvent[]>([]);

  // Latest event for animations
  private latestEventSubject = new Subject<SystemEvent>();

  // Pipeline state
  private pipelineSubject = new BehaviorSubject<PipelineState>({
    nodes: [...DEFAULT_PIPELINE_NODES],
    connections: [...DEFAULT_PIPELINE_CONNECTIONS],
    lastUpdated: new Date().toISOString(),
  });

  // Live metrics
  private metricsSubject = new BehaviorSubject<LiveMetrics>({ ...DEFAULT_LIVE_METRICS });

  // Simulation state
  private simulationSubscription?: Subscription;
  private isSimulatingSubject = new BehaviorSubject<boolean>(false);

  // Connection status
  private connectionStatusSubject = new BehaviorSubject<'connected' | 'disconnected' | 'simulating'>('disconnected');

  // Paused state
  private isPausedSubject = new BehaviorSubject<boolean>(false);

  // Public observables
  public events$ = this.eventsSubject.asObservable().pipe(share());
  public latestEvent$ = this.latestEventSubject.asObservable().pipe(share());
  public pipeline$ = this.pipelineSubject.asObservable().pipe(distinctUntilChanged(), share());
  public metrics$ = this.metricsSubject.asObservable().pipe(distinctUntilChanged(), share());
  public isSimulating$ = this.isSimulatingSubject.asObservable();
  public isPaused$ = this.isPausedSubject.asObservable();
  public connectionStatus$ = this.connectionStatusSubject.asObservable();

  // Metrics tracking
  private operationResults: boolean[] = []; // Track last 100 operation successes
  private processingTimes: number[] = []; // Track processing times

  constructor(
    private wsService: WebSocketVisualizationService,
    private ngZone: NgZone,
    private notificationService: NotificationService
  ) {
    this.initializeWebSocketSubscription();
  }

  /**
   * Initialize WebSocket subscription
   */
  private initializeWebSocketSubscription(): void {
    // Subscribe to WebSocket status
    this.wsService.status$
      .pipe(takeUntil(this.destroy$))
      .subscribe(status => {
        if (status === WebSocketStatus.CONNECTED) {
          this.connectionStatusSubject.next('connected');
        } else if (status === WebSocketStatus.DISCONNECTED || status === WebSocketStatus.ERROR) {
          if (!this.isSimulatingSubject.value) {
            this.connectionStatusSubject.next('disconnected');
          }
        }
      });

    // Subscribe to batch progress events
    this.wsService.batchProgress$
      .pipe(takeUntil(this.destroy$))
      .subscribe(event => this.handleBatchProgressEvent(event));

    // Subscribe to evaluation progress events
    this.wsService.evaluationProgress$
      .pipe(takeUntil(this.destroy$))
      .subscribe(event => this.handleEvaluationProgressEvent(event));

    // Subscribe to care gap notifications
    this.wsService.careGapNotification$
      .pipe(takeUntil(this.destroy$))
      .subscribe(event => this.handleCareGapNotification(event));
  }

  /**
   * Handle care gap notification from WebSocket
   * Creates system event and shows toast notification
   */
  private handleCareGapNotification(event: CareGapNotificationEvent): void {
    if (this.isPausedSubject.value) return;

    // Determine event type and severity based on notification type
    let eventType: SystemEventType;
    let severity: 'info' | 'success' | 'warning' | 'error' = 'info';

    if (event.type === 'CARE_GAP_ADDRESSED') {
      eventType = 'CARE_GAP_CLOSED';
      severity = 'success';
    } else {
      eventType = 'CARE_GAP_DETECTED';
      severity = event.priority === 'CRITICAL' || event.priority === 'HIGH' ? 'warning' : 'info';
    }

    // Create system event using positional parameters: type, title, description, options
    const systemEvent = createSystemEvent(
      eventType,
      event.title,
      event.message,
      {
        severity,
        source: 'quality-measure-service',
        patient: {
          id: event.patientId,
          name: event.patientName,
        },
        measure: event.qualityMeasure ? {
          id: event.qualityMeasure,
          name: event.qualityMeasure,
        } : undefined,
        metadata: {
          gapId: event.gapId,
          gapType: event.gapType,
          category: event.category,
          priority: event.priority,
          dueDate: event.dueDate,
          actionUrl: event.actionUrl,
        },
      }
    );

    // Add event to buffer
    this.addEvent(systemEvent);

    // Show toast notification for high-priority care gaps
    this.showCareGapToastNotification(event);
  }

  /**
   * Show toast notification for care gap events
   */
  private showCareGapToastNotification(event: CareGapNotificationEvent): void {
    const patientName = event.patientName || `Patient ${event.patientId}`;

    if (event.type === 'CARE_GAP_ADDRESSED') {
      this.notificationService.success(
        `Care gap resolved: ${event.title}`,
        4000,
        'View'
      );
    } else if (event.priority === 'CRITICAL') {
      this.notificationService.error(
        `[CRITICAL] New care gap for ${patientName}: ${event.title}`,
        6000,
        'View Details'
      );
    } else if (event.priority === 'HIGH') {
      this.notificationService.warning(
        `[HIGH] New care gap for ${patientName}: ${event.title}`,
        5000,
        'View Details'
      );
    } else {
      this.notificationService.info(
        `New care gap detected for ${patientName}: ${event.title}`,
        4000,
        'View'
      );
    }
  }

  /**
   * Connect to real-time event sources
   */
  connect(): void {
    this.wsService.connect();
  }

  /**
   * Disconnect from event sources
   */
  disconnect(): void {
    this.wsService.disconnect();
    this.stopSimulation();
    this.connectionStatusSubject.next('disconnected');
  }

  /**
   * Handle batch progress event from WebSocket
   */
  private handleBatchProgressEvent(event: BatchProgressEvent): void {
    if (this.isPausedSubject.value) return;

    let eventType: SystemEventType;
    let severity: 'info' | 'success' | 'warning' | 'error' = 'info';

    if (event.status === 'COMPLETED') {
      eventType = 'BATCH_COMPLETED';
      severity = 'success';
    } else if (event.status === 'FAILED') {
      eventType = 'EVALUATION_FAILED';
      severity = 'error';
    } else {
      eventType = 'BATCH_PROGRESS';
    }

    const systemEvent = createSystemEvent(
      eventType,
      `Batch ${event.batchId.substring(0, 8)}`,
      `${event.completedCount}/${event.totalPatients} patients processed`,
      {
        severity,
        source: 'cql-engine',
        measure: event.measureName ? { id: event.measureId || '', name: event.measureName } : undefined,
        metadata: {
          batchId: event.batchId,
          completedCount: event.completedCount,
          totalPatients: event.totalPatients,
          complianceRate: event.cumulativeComplianceRate,
        },
        durationMs: event.avgDurationMs,
      }
    );

    this.addEvent(systemEvent);
    this.updatePipelineFromBatch(event);
    this.updateMetricsFromBatch(event);
  }

  /**
   * Handle evaluation progress event from WebSocket
   */
  private handleEvaluationProgressEvent(event: any): void {
    if (this.isPausedSubject.value) return;

    const eventType: SystemEventType = event.status === 'COMPLETED' ? 'EVALUATION_COMPLETED' :
                                        event.status === 'FAILED' ? 'EVALUATION_FAILED' : 'EVALUATION_STARTED';

    const systemEvent = createSystemEvent(
      eventType,
      `Evaluation ${event.status}`,
      event.message || `Patient ${event.patientId}`,
      {
        severity: event.status === 'COMPLETED' ? 'success' : event.status === 'FAILED' ? 'error' : 'info',
        source: 'cql-engine',
        patient: { id: event.patientId },
        metadata: { progress: event.progress },
      }
    );

    this.addEvent(systemEvent);
  }

  /**
   * Add event to buffer
   */
  addEvent(event: SystemEvent): void {
    if (this.isPausedSubject.value) return;

    // Add to buffer
    this.eventsBuffer.unshift(event);

    // Trim buffer to max size
    if (this.eventsBuffer.length > this.MAX_EVENTS) {
      this.eventsBuffer = this.eventsBuffer.slice(0, this.MAX_EVENTS);
    }

    // Emit updates
    this.eventsSubject.next([...this.eventsBuffer]);
    this.latestEventSubject.next(event);

    // Track operation result
    const isSuccess = event.severity !== 'error';
    this.operationResults.unshift(isSuccess);
    if (this.operationResults.length > 100) {
      this.operationResults.pop();
    }

    // Track processing time
    if (event.durationMs) {
      this.processingTimes.unshift(event.durationMs);
      if (this.processingTimes.length > 100) {
        this.processingTimes.pop();
      }
    }
  }

  /**
   * Get filtered events
   */
  getFilteredEvents(filter: EventFilterOptions): Observable<SystemEvent[]> {
    return this.events$.pipe(
      map(events => events.filter(event => {
        if (filter.categories && !filter.categories.includes(event.category)) return false;
        if (filter.severities && !filter.severities.includes(event.severity)) return false;
        if (filter.types && !filter.types.includes(event.type)) return false;
        if (filter.patientId && event.patient?.id !== filter.patientId) return false;
        if (filter.startTime && event.timestamp < filter.startTime) return false;
        if (filter.endTime && event.timestamp > filter.endTime) return false;
        return true;
      }))
    );
  }

  /**
   * Clear all events
   */
  clearEvents(): void {
    this.eventsBuffer = [];
    this.eventsSubject.next([]);
  }

  /**
   * Pause/resume event streaming
   */
  togglePause(): void {
    this.isPausedSubject.next(!this.isPausedSubject.value);
  }

  /**
   * Set paused state
   */
  setPaused(paused: boolean): void {
    this.isPausedSubject.next(paused);
  }

  /**
   * Update pipeline state from batch progress
   */
  private updatePipelineFromBatch(event: BatchProgressEvent): void {
    const currentState = this.pipelineSubject.value;
    const nodes = currentState.nodes.map(node => {
      if (node.id === 'cql') {
        return {
          ...node,
          status: event.status === 'IN_PROGRESS' ? 'processing' as const : 'active' as const,
          throughput: event.currentThroughput || 0,
          lastActivity: new Date().toISOString(),
        };
      }
      if (node.id === 'fhir' && event.status === 'IN_PROGRESS') {
        return {
          ...node,
          status: 'active' as const,
          throughput: event.currentThroughput || 0,
          lastActivity: new Date().toISOString(),
        };
      }
      if (node.id === 'quality' && event.cumulativeComplianceRate !== undefined) {
        return {
          ...node,
          status: 'active' as const,
          lastActivity: new Date().toISOString(),
        };
      }
      return node;
    });

    const connections = currentState.connections.map(conn => ({
      ...conn,
      isActive: event.status === 'IN_PROGRESS',
      throughput: event.status === 'IN_PROGRESS' ? event.currentThroughput || 0 : 0,
    }));

    this.pipelineSubject.next({
      nodes,
      connections,
      lastUpdated: new Date().toISOString(),
    });
  }

  /**
   * Update metrics from batch progress
   */
  private updateMetricsFromBatch(event: BatchProgressEvent): void {
    const currentMetrics = this.metricsSubject.value;

    const successRate = this.operationResults.length > 0
      ? (this.operationResults.filter(r => r).length / this.operationResults.length) * 100
      : 100;

    const avgProcessingTime = this.processingTimes.length > 0
      ? this.processingTimes.reduce((a, b) => a + b, 0) / this.processingTimes.length
      : 0;

    this.metricsSubject.next({
      ...currentMetrics,
      patientsProcessed: event.completedCount,
      throughputPerSecond: event.currentThroughput || 0,
      complianceRate: event.cumulativeComplianceRate || currentMetrics.complianceRate,
      successRate: Math.round(successRate * 10) / 10,
      avgProcessingTimeMs: Math.round(avgProcessingTime),
      lastUpdated: new Date().toISOString(),
    });
  }

  /**
   * Start simulation mode for demo/testing
   */
  startSimulation(): void {
    if (this.simulationSubscription) {
      return; // Already simulating
    }

    this.isSimulatingSubject.next(true);
    this.connectionStatusSubject.next('simulating');

    // Generate random events at intervals
    this.simulationSubscription = interval(800 + Math.random() * 1200)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (!this.isPausedSubject.value) {
          this.generateSimulatedEvent();
        }
      });

    // Also update metrics periodically
    interval(this.METRICS_UPDATE_INTERVAL)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (!this.isPausedSubject.value && this.isSimulatingSubject.value) {
          this.updateSimulatedMetrics();
        }
      });
  }

  /**
   * Stop simulation mode
   */
  stopSimulation(): void {
    if (this.simulationSubscription) {
      this.simulationSubscription.unsubscribe();
      this.simulationSubscription = undefined;
    }
    this.isSimulatingSubject.next(false);

    if (this.wsService.getStatus() !== WebSocketStatus.CONNECTED) {
      this.connectionStatusSubject.next('disconnected');
    }
  }

  /**
   * Generate a simulated event
   */
  private generateSimulatedEvent(): void {
    const eventTypes: { type: SystemEventType; weight: number }[] = [
      { type: 'FHIR_RESOURCE_CREATED', weight: 20 },
      { type: 'FHIR_RESOURCE_UPDATED', weight: 15 },
      { type: 'EVALUATION_COMPLETED', weight: 25 },
      { type: 'EVALUATION_STARTED', weight: 10 },
      { type: 'MEASURE_CALCULATED', weight: 10 },
      { type: 'CARE_GAP_DETECTED', weight: 8 },
      { type: 'CARE_GAP_CLOSED', weight: 5 },
      { type: 'COMPLIANCE_UPDATED', weight: 5 },
      { type: 'EVALUATION_FAILED', weight: 2 },
    ];

    // Weighted random selection
    const totalWeight = eventTypes.reduce((sum, e) => sum + e.weight, 0);
    let random = Math.random() * totalWeight;
    let selectedType: SystemEventType = 'EVALUATION_COMPLETED';

    for (const { type, weight } of eventTypes) {
      random -= weight;
      if (random <= 0) {
        selectedType = type;
        break;
      }
    }

    const patientNames = [
      'John Smith', 'Maria Garcia', 'James Wilson', 'Sarah Johnson',
      'Robert Brown', 'Emily Davis', 'Michael Lee', 'Jennifer Martinez'
    ];
    const measures = [
      'HbA1c Control', 'Blood Pressure Control', 'Diabetic Eye Exam',
      'Colorectal Cancer Screening', 'Depression Screening', 'Statin Therapy'
    ];
    const fhirResources = ['Observation', 'Condition', 'MedicationRequest', 'Procedure'];

    const patientName = patientNames[Math.floor(Math.random() * patientNames.length)];
    const patientId = `patient-${Math.floor(Math.random() * 1000).toString().padStart(3, '0')}`;
    const mrn = `MRN${Math.floor(Math.random() * 100000).toString().padStart(6, '0')}`;
    const measure = measures[Math.floor(Math.random() * measures.length)];

    let title = '';
    let description = '';
    let severity: 'info' | 'success' | 'warning' | 'error' = 'info';
    let source = 'system';

    switch (selectedType) {
      case 'FHIR_RESOURCE_CREATED':
        const resource = fhirResources[Math.floor(Math.random() * fhirResources.length)];
        title = `${resource} Created`;
        description = `New ${resource.toLowerCase()} for ${patientName}`;
        severity = 'info';
        source = 'fhir-service';
        break;
      case 'FHIR_RESOURCE_UPDATED':
        title = 'Patient Record Updated';
        description = `${patientName} - Clinical data updated`;
        severity = 'info';
        source = 'fhir-service';
        break;
      case 'EVALUATION_STARTED':
        title = 'Evaluation Started';
        description = `${measure} for ${patientName}`;
        severity = 'info';
        source = 'cql-engine';
        break;
      case 'EVALUATION_COMPLETED':
        const compliant = Math.random() > 0.3;
        title = 'Evaluation Completed';
        description = `${measure} - ${compliant ? 'Compliant' : 'Non-compliant'}`;
        severity = 'success';
        source = 'cql-engine';
        break;
      case 'EVALUATION_FAILED':
        title = 'Evaluation Failed';
        description = `${measure} - Processing error`;
        severity = 'error';
        source = 'cql-engine';
        break;
      case 'MEASURE_CALCULATED':
        const score = Math.floor(70 + Math.random() * 25);
        title = 'Measure Calculated';
        description = `${measure}: ${score}% compliance`;
        severity = 'success';
        source = 'quality-measure-service';
        break;
      case 'CARE_GAP_DETECTED':
        title = 'Care Gap Detected';
        description = `${patientName} - ${measure} overdue`;
        severity = 'warning';
        source = 'care-gap-service';
        break;
      case 'CARE_GAP_CLOSED':
        title = 'Care Gap Closed';
        description = `${patientName} - ${measure} completed`;
        severity = 'success';
        source = 'care-gap-service';
        break;
      case 'COMPLIANCE_UPDATED':
        title = 'Compliance Updated';
        description = `Population compliance recalculated`;
        severity = 'info';
        source = 'quality-measure-service';
        break;
    }

    const event = createSystemEvent(selectedType, title, description, {
      severity,
      source,
      patient: { id: patientId, name: patientName, mrn },
      measure: { id: `measure-${Math.random().toString(36).substr(2, 6)}`, name: measure },
      durationMs: Math.floor(50 + Math.random() * 200),
    });

    this.addEvent(event);
    this.updatePipelineForSimulation(selectedType);
  }

  /**
   * Update pipeline state for simulation
   */
  private updatePipelineForSimulation(eventType: SystemEventType): void {
    const currentState = this.pipelineSubject.value;

    const nodes = currentState.nodes.map(node => {
      let status = node.status;
      let throughput = node.throughput;

      // Determine which node is active based on event type
      if (eventType.startsWith('FHIR_') && node.id === 'fhir') {
        status = 'processing';
        throughput = Math.random() * 5 + 2;
      } else if ((eventType.startsWith('EVALUATION_') || eventType.startsWith('BATCH_')) && node.id === 'cql') {
        status = 'processing';
        throughput = Math.random() * 3 + 1;
      } else if ((eventType.startsWith('MEASURE_') || eventType.startsWith('COMPLIANCE_')) && node.id === 'quality') {
        status = 'processing';
        throughput = Math.random() * 4 + 1;
      } else if (eventType.startsWith('CARE_GAP_') && node.id === 'caregap') {
        status = 'processing';
        throughput = Math.random() * 2 + 0.5;
      } else {
        // Decay throughput if not active
        throughput = Math.max(0, throughput - 0.1);
        status = throughput > 0.5 ? 'active' : 'idle';
      }

      return {
        ...node,
        status: status as 'active' | 'idle' | 'processing' | 'error',
        throughput: Math.round(throughput * 10) / 10,
        lastActivity: status === 'processing' ? new Date().toISOString() : node.lastActivity,
      };
    });

    // Update connections based on node activity
    const connections = currentState.connections.map(conn => {
      const fromNode = nodes.find(n => n.id === conn.from);
      const toNode = nodes.find(n => n.id === conn.to);
      const isActive = fromNode?.status === 'processing' || toNode?.status === 'processing';
      return {
        ...conn,
        isActive,
        throughput: isActive ? Math.min(fromNode?.throughput || 0, toNode?.throughput || 0) : 0,
      };
    });

    this.pipelineSubject.next({
      nodes,
      connections,
      lastUpdated: new Date().toISOString(),
    });
  }

  /**
   * Update simulated metrics
   */
  private updateSimulatedMetrics(): void {
    const currentMetrics = this.metricsSubject.value;

    // Gradually increase patients processed
    const patientsProcessed = currentMetrics.patientsProcessed + Math.floor(Math.random() * 3);

    // Fluctuate throughput
    const throughput = 2 + Math.random() * 8;

    // Slightly vary compliance rate
    const complianceChange = (Math.random() - 0.5) * 0.5;
    const complianceRate = Math.max(70, Math.min(99, currentMetrics.complianceRate + complianceChange));

    // Calculate success rate from operation results
    const successRate = this.operationResults.length > 0
      ? (this.operationResults.filter(r => r).length / this.operationResults.length) * 100
      : 95 + Math.random() * 5;

    // Calculate average processing time
    const avgProcessingTime = this.processingTimes.length > 0
      ? this.processingTimes.reduce((a, b) => a + b, 0) / this.processingTimes.length
      : 80 + Math.random() * 40;

    // Update care gaps (occasionally change)
    const careGapsChange = Math.random() > 0.8 ? (Math.random() > 0.5 ? 1 : -1) : 0;
    const openCareGaps = Math.max(0, currentMetrics.openCareGaps + careGapsChange);

    this.metricsSubject.next({
      patientsProcessed,
      patientsProcessedChange: patientsProcessed - (currentMetrics.patientsProcessed || 0),
      throughputPerSecond: Math.round(throughput * 10) / 10,
      maxThroughput: Math.max(currentMetrics.maxThroughput, throughput),
      complianceRate: Math.round(complianceRate * 10) / 10,
      complianceRateChange: Math.round(complianceChange * 10) / 10,
      openCareGaps,
      careGapsChange,
      successRate: Math.round(successRate * 10) / 10,
      avgProcessingTimeMs: Math.round(avgProcessingTime),
      lastUpdated: new Date().toISOString(),
    });
  }

  /**
   * Get current events
   */
  getCurrentEvents(): SystemEvent[] {
    return [...this.eventsBuffer];
  }

  /**
   * Get current pipeline state
   */
  getCurrentPipelineState(): PipelineState {
    return this.pipelineSubject.value;
  }

  /**
   * Get current metrics
   */
  getCurrentMetrics(): LiveMetrics {
    return this.metricsSubject.value;
  }

  /**
   * Set initial metrics (e.g., from dashboard data)
   */
  setInitialMetrics(metrics: Partial<LiveMetrics>): void {
    this.metricsSubject.next({
      ...this.metricsSubject.value,
      ...metrics,
      lastUpdated: new Date().toISOString(),
    });
  }

  /**
   * Cleanup on destroy
   */
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.stopSimulation();
    this.wsService.disconnect();
  }
}
