import { Injectable, NgZone } from '@angular/core';
import { Observable, Subject, BehaviorSubject, timer, of } from 'rxjs';
import { retry, share, distinctUntilChanged, filter } from 'rxjs/operators';
import { EvaluationProgressEvent } from '../data/data-transform.service';
import { API_CONFIG } from '../../config/api.config';

/**
 * WebSocket Connection Status
 */
export enum WebSocketStatus {
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED',
  ERROR = 'ERROR',
  RECONNECTING = 'RECONNECTING',
}

/**
 * Batch Progress Event (from backend WebSocket)
 * Aligned with backend BatchProgressEvent model
 */
export interface BatchProgressEvent {
  // Identifiers
  batchId: string;
  tenantId?: string;
  measureId?: string;
  measureName?: string;

  // Progress metrics
  totalPatients: number;
  completedCount: number;
  successCount: number;
  failedCount: number;
  pendingCount: number;

  // Performance metrics
  avgDurationMs: number;
  currentThroughput: number;
  elapsedTimeMs: number;
  estimatedTimeRemainingMs?: number;

  // Quality metrics
  denominatorCount?: number;
  numeratorCount?: number;
  cumulativeComplianceRate?: number;

  // Metadata
  timestamp: string;
  status: 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';

  // Legacy fields for backward compatibility (deprecated)
  /** @deprecated Use completedCount instead */
  completedPatients?: number;
  /** @deprecated Use successCount instead */
  successfulEvaluations?: number;
  /** @deprecated Use failedCount instead */
  failedEvaluations?: number;
  /** @deprecated Use avgDurationMs instead */
  averageDurationMs?: number;
  /** @deprecated Use currentThroughput instead */
  throughputPerSecond?: number;
  /** @deprecated Use measureName instead */
  currentMeasure?: string;
  completionPercentage?: number;
}

/**
 * WebSocket Visualization Service
 *
 * Manages WebSocket connection to CQL Engine Service for real-time
 * evaluation progress updates. Transforms backend events into
 * visualization-ready data streams.
 *
 * Features:
 * - Auto-reconnection with exponential backoff
 * - Connection status monitoring
 * - Event type filtering
 * - Error handling and recovery
 * - RxJS observable streams for reactive visualization
 */
@Injectable({
  providedIn: 'root'
})
export class WebSocketVisualizationService {
  // WebSocket connection
  private socket?: WebSocket;
  private reconnectAttempts = 0;
  private readonly maxReconnectAttempts = 5;
  private readonly reconnectDelay = 2000; // Initial delay in ms
  private reconnectTimer?: ReturnType<typeof setTimeout>;

  // Observable streams
  private statusSubject = new BehaviorSubject<WebSocketStatus>(WebSocketStatus.DISCONNECTED);
  private batchProgressSubject = new Subject<BatchProgressEvent>();
  private evaluationProgressSubject = new Subject<EvaluationProgressEvent>();
  private errorSubject = new Subject<Error>();

  // Public observables
  public status$ = this.statusSubject.asObservable().pipe(distinctUntilChanged(), share());
  public batchProgress$ = this.batchProgressSubject.asObservable().pipe(share());
  public evaluationProgress$ = this.evaluationProgressSubject.asObservable().pipe(share());
  public error$ = this.errorSubject.asObservable().pipe(share());

  // WebSocket endpoint (from backend CQL Engine Service)
  private readonly wsUrl = API_CONFIG.CQL_ENGINE_URL.replace('http://', 'ws://').replace('/cql-engine', '') + '/ws/evaluation-progress';

  constructor(private ngZone: NgZone) {}

  /**
   * Connect to WebSocket
   */
  connect(): void {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      console.log('WebSocket already connected');
      return;
    }

    if (this.socket && this.socket.readyState === WebSocket.CONNECTING) {
      console.log('WebSocket connection in progress');
      return;
    }

    console.log(`Connecting to WebSocket: ${this.wsUrl}`);
    this.statusSubject.next(WebSocketStatus.CONNECTING);

    try {
      // Create WebSocket connection outside Angular zone for better performance
      this.ngZone.runOutsideAngular(() => {
        this.socket = new WebSocket(this.wsUrl);

        this.socket.onopen = (event) => {
          this.ngZone.run(() => this.onOpen(event));
        };

        this.socket.onmessage = (event) => {
          this.ngZone.run(() => this.onMessage(event));
        };

        this.socket.onerror = (event) => {
          this.ngZone.run(() => this.onError(event));
        };

        this.socket.onclose = (event) => {
          this.ngZone.run(() => this.onClose(event));
        };
      });
    } catch (error) {
      console.error('Failed to create WebSocket:', error);
      this.statusSubject.next(WebSocketStatus.ERROR);
      this.errorSubject.next(error as Error);
      this.scheduleReconnect();
    }
  }

  /**
   * Disconnect from WebSocket
   */
  disconnect(): void {
    console.log('Disconnecting WebSocket');

    // Clear reconnect timer
    window.clearTimeout(this.reconnectTimer as any);
    this.reconnectTimer = undefined;

    // Close socket
    if (this.socket) {
      this.socket.close(1000, 'Client disconnect');
      this.socket = undefined;
    }

    this.reconnectAttempts = 0;
    this.statusSubject.next(WebSocketStatus.DISCONNECTED);
  }

  /**
   * Handle WebSocket open event
   */
  private onOpen(event: Event): void {
    console.log('WebSocket connected', event);
    this.statusSubject.next(WebSocketStatus.CONNECTED);
    this.reconnectAttempts = 0;

    // Send initial subscription message if needed
    this.sendMessage({
      type: 'subscribe',
      events: ['batch_progress', 'evaluation_progress']
    });
  }

  /**
   * Handle WebSocket message event
   */
  private onMessage(event: MessageEvent): void {
    try {
      const data = JSON.parse(event.data);
      console.log('WebSocket message received:', data);

      // Route message based on type
      if (data.type === 'batch_progress' || data.batchId) {
        this.handleBatchProgress(data);
      } else if (data.type === 'evaluation_progress' || data.patientId) {
        this.handleEvaluationProgress(data);
      } else {
        console.warn('Unknown WebSocket message type:', data);
      }
    } catch (error) {
      console.error('Failed to parse WebSocket message:', error);
      this.errorSubject.next(new Error('Failed to parse WebSocket message'));
    }
  }

  /**
   * Handle WebSocket error event
   */
  private onError(event: Event): void {
    console.error('WebSocket error:', event);
    this.statusSubject.next(WebSocketStatus.ERROR);
    this.errorSubject.next(new Error('WebSocket connection error'));
  }

  /**
   * Handle WebSocket close event
   */
  private onClose(event: CloseEvent): void {
    console.log('WebSocket closed:', event.code, event.reason);
    this.statusSubject.next(WebSocketStatus.DISCONNECTED);
    this.socket = undefined;

    // Attempt reconnection if not a normal closure
    if (event.code !== 1000 && this.reconnectAttempts < this.maxReconnectAttempts) {
      this.scheduleReconnect();
    }
  }

  /**
   * Schedule reconnection attempt
   */
  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached');
      this.statusSubject.next(WebSocketStatus.ERROR);
      return;
    }

    this.reconnectAttempts++;
    const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1); // Exponential backoff

    console.log(`Scheduling reconnection attempt ${this.reconnectAttempts} in ${delay}ms`);
    this.statusSubject.next(WebSocketStatus.RECONNECTING);

    this.reconnectTimer = setTimeout(() => {
      console.log(`Reconnection attempt ${this.reconnectAttempts}`);
      this.connect();
    }, delay);
  }

  /**
   * Handle batch progress event
   */
  private handleBatchProgress(data: any): void {
    const totalPatients = data.totalPatients ?? 0;
    const completedCount = data.completedCount ?? data.completedPatients ?? 0;
    const successCount = data.successCount ?? data.successfulEvaluations ?? 0;
    const failedCount = data.failedCount ?? data.failedEvaluations ?? 0;
    const pendingCount = data.pendingCount ?? 0;
    const avgDurationMs = data.avgDurationMs ?? data.averageDurationMs ?? 0;
    const currentThroughput = data.currentThroughput ?? data.throughputPerSecond ?? 0;

    const event: BatchProgressEvent = {
      // Identifiers
      batchId: data.batchId || data.id,
      tenantId: data.tenantId,
      measureId: data.measureId,
      measureName: data.measureName || data.currentMeasure,

      // Progress metrics
      totalPatients,
      completedCount,
      successCount,
      failedCount,
      pendingCount,

      // Performance metrics
      avgDurationMs,
      currentThroughput,
      elapsedTimeMs: data.elapsedTimeMs ?? 0,
      estimatedTimeRemainingMs: data.estimatedTimeRemainingMs,

      // Quality metrics
      denominatorCount: data.denominatorCount,
      numeratorCount: data.numeratorCount,
      cumulativeComplianceRate: data.cumulativeComplianceRate,

      // Metadata
      timestamp: data.timestamp || new Date().toISOString(),
      status: data.status || 'IN_PROGRESS',

      // Legacy fields for backward compatibility
      completedPatients: data.completedPatients ?? data.completedCount ?? 0,
      successfulEvaluations: data.successfulEvaluations ?? data.successCount ?? 0,
      failedEvaluations: data.failedEvaluations ?? data.failedCount ?? 0,
      averageDurationMs: data.averageDurationMs ?? data.avgDurationMs ?? 0,
      throughputPerSecond: data.throughputPerSecond ?? data.currentThroughput ?? 0,
      currentMeasure: data.currentMeasure || data.measureName,
      completionPercentage:
        data.completionPercentage ??
        (totalPatients > 0 ? (completedCount / totalPatients) * 100 : 0),
    };

    this.batchProgressSubject.next(event);
  }

  /**
   * Handle evaluation progress event
   */
  private handleEvaluationProgress(data: any): void {
    const event: EvaluationProgressEvent = {
      batchId: data.batchId,
      patientId: data.patientId,
      status: data.status || 'PENDING',
      progress: data.progress || 0,
      message: data.message,
      timestamp: data.timestamp || new Date().toISOString(),
    };

    this.evaluationProgressSubject.next(event);
  }

  /**
   * Send message to WebSocket
   */
  private sendMessage(message: any): void {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      try {
        this.socket.send(JSON.stringify(message));
      } catch (error) {
        console.error('Failed to send WebSocket message:', error);
        this.errorSubject.next(error as Error);
      }
    } else {
      console.warn('Cannot send message: WebSocket not connected');
    }
  }

  /**
   * Get current connection status
   */
  getStatus(): WebSocketStatus {
    return this.statusSubject.value;
  }

  /**
   * Check if connected
   */
  isConnected(): boolean {
    return this.statusSubject.value === WebSocketStatus.CONNECTED;
  }

  /**
   * Simulate batch progress for testing (when WebSocket is not available)
   */
  simulateBatchProgress(durationSeconds: number = 30): Observable<BatchProgressEvent> {
    const interval = 1000; // Update every second
    const totalUpdates = durationSeconds;
    const patientsTotal = 100;

    return new Observable<BatchProgressEvent>(subscriber => {
      let updateCount = 0;

      const simulationInterval = setInterval(() => {
        updateCount++;
        const progress = updateCount / totalUpdates;
        const completed = Math.floor(patientsTotal * progress);
        const successful = Math.floor(completed * 0.9); // 90% success rate
        const failed = completed - successful;

        const event: BatchProgressEvent = {
          batchId: 'simulation-' + Date.now(),
          tenantId: 'default',
          measureId: 'sim-measure-1',
          measureName: 'CDC-A1C9',
          totalPatients: patientsTotal,
          completedCount: completed,
          successCount: successful,
          failedCount: failed,
          pendingCount: patientsTotal - completed,
          avgDurationMs: (durationSeconds * 1000) / patientsTotal,
          currentThroughput: patientsTotal / durationSeconds,
          elapsedTimeMs: updateCount * interval,
          estimatedTimeRemainingMs: (totalUpdates - updateCount) * interval,
          denominatorCount: completed,
          numeratorCount: successful,
          cumulativeComplianceRate: completed > 0 ? (successful / completed) * 100 : 0,
          timestamp: new Date().toISOString(),
          status: progress < 1 ? 'IN_PROGRESS' : 'COMPLETED',
          // Legacy fields for backward compatibility
          completedPatients: completed,
          successfulEvaluations: successful,
          failedEvaluations: failed,
          averageDurationMs: (durationSeconds * 1000) / patientsTotal,
          throughputPerSecond: patientsTotal / durationSeconds,
          currentMeasure: 'CDC-A1C9',
          completionPercentage: progress * 100,
        };

        subscriber.next(event);

        if (updateCount >= totalUpdates) {
          clearInterval(simulationInterval);
          subscriber.complete();
        }
      }, interval);

      // Cleanup function
      return () => {
        clearInterval(simulationInterval);
      };
    });
  }

  /**
   * Clean up resources
   */
  dispose(): void {
    this.disconnect();
    this.statusSubject.complete();
    this.batchProgressSubject.complete();
    this.evaluationProgressSubject.complete();
    this.errorSubject.complete();
  }
}
