import { Injectable } from '@angular/core';
import { LoggerService } from '../logger.service';
import { Observable, Subject, merge, of, throwError, combineLatest } from 'rxjs';
import { LoggerService } from '../logger.service';
import { map, switchMap, catchError, tap, shareReplay, take } from 'rxjs/operators';
import { LoggerService } from '../logger.service';

import { PatientService } from './patient.service';
import { LoggerService } from '../logger.service';
import { EvaluationService } from './evaluation.service';
import { LoggerService } from '../logger.service';
import {
  WebSocketVisualizationService,
  BatchProgressEvent,
  WebSocketStatus,
} from '../visualization/core/websocket-visualization.service';
import { CqlEvaluation, BatchEvaluationResponse } from '../models/evaluation.model';
import { LoggerService } from '../logger.service';
import { Patient } from '../models/patient.model';
import { LoggerService } from '../logger.service';

/**
 * Batch Monitor Configuration
 */
export interface BatchMonitorConfig {
  libraryId: string;
  patientCount?: number;
  patientIds?: string[];
  autoConnect?: boolean;
  measureId?: string;
}

/**
 * Batch Monitor State
 */
export interface BatchMonitorState {
  batchId?: string;
  status: 'IDLE' | 'LOADING_PATIENTS' | 'SUBMITTING_BATCH' | 'MONITORING' | 'COMPLETED' | 'ERROR' | 'FAILED';
  error?: Error;
  patientsLoaded?: number;
  batchSubmitted?: boolean;
}

/**
 * Batch Monitor Service
 *
 * Orchestrates batch evaluation workflow:
 * 1. Loads patients from FHIR server
 * 2. Submits batch evaluation to CQL Engine
 * 3. Connects WebSocket for real-time progress updates
 * 4. Provides combined observable stream of progress
 *
 * Usage:
 * ```typescript
 * batchMonitor.startBatchEvaluation({
 *   libraryId: 'cql-lib-123',
 *   patientCount: 100
 * }).subscribe(progress => {
 *   this.logger.info('Batch progress:', progress);
 * });
 * ```
 */
@Injectable({
  providedIn: 'root'
})
export class BatchMonitorService {
  private readonly logger: any;
  private stateSubject = new Subject<BatchMonitorState>();
  // ⚠️ CRITICAL HIPAA COMPLIANCE - DO NOT REMOVE refCount: true ⚠️
  // refCount: true ensures cache is destroyed when all subscribers unsubscribe
  // This prevents PHI from persisting in browser memory after component destruction
  // See: /backend/HIPAA-CACHE-COMPLIANCE.md for full documentation
  public state$ = this.stateSubject.asObservable().pipe(
    shareReplay({ bufferSize: 1, refCount: true })
  );

  private currentBatchId?: string;

  constructor(
    private loggerService: LoggerService,
    private patientService: PatientService,
    private evaluationService: EvaluationService,
    private websocketService: WebSocketVisualizationService
  ) {
    this.logger = this.loggerService.withContext(\'BatchMonitorService');
    // Initialize state
    this.stateSubject.next({ status: 'IDLE' });
  }

  /**
   * Start a new batch evaluation and monitor progress
   *
   * @param config Batch monitor configuration
   * @returns Observable stream of batch progress events
   */
  startBatchEvaluation(config: BatchMonitorConfig): Observable<BatchProgressEvent> {
    this.logger.info('Starting batch evaluation with config:', config);

    // Update state
    this.updateState({ status: 'LOADING_PATIENTS' });

    // Create the workflow
    return this.loadPatients(config).pipe(
      switchMap(patients => {
        this.logger.info(`Loaded ${patients.length} patients`);
        this.updateState({
          status: 'SUBMITTING_BATCH',
          patientsLoaded: patients.length
        });

        // Extract patient IDs
        const patientIds = patients.map(p => p.id).filter(id => id != null) as string[];

        // Submit batch evaluation
        return this.submitBatch(config.libraryId, patientIds);
      }),
      switchMap(batchResponse => {
        this.logger.info('Batch evaluation submitted:', batchResponse);

        // Generate a local batch ID for tracking (backend returns array directly)
        const localBatchId = crypto.randomUUID();
        this.currentBatchId = localBatchId;

        this.updateState({
          status: 'MONITORING',
          batchId: localBatchId,
          batchSubmitted: true
        });

        // Connect WebSocket if not already connected
        if (config.autoConnect !== false) {
          this.ensureWebSocketConnection();
        }

        // Return progress stream
        return this.monitorBatchProgress(localBatchId);
      }),
      tap({
        next: (progress) => {
          // Update state on completion
          if (progress.status === 'COMPLETED' || progress.status === 'FAILED') {
            this.updateState({ status: progress.status });
          }
        },
        error: (error) => {
          this.logger.error('Batch evaluation error:', { error });
          this.updateState({ status: 'ERROR', error });
        }
      }),
      catchError(error => {
        this.updateState({ status: 'ERROR', error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Load patients from FHIR server
   */
  private loadPatients(config: BatchMonitorConfig): Observable<Patient[]> {
    // If specific patient IDs provided, fetch those
    if (config.patientIds && config.patientIds.length > 0) {
      return combineLatest(
        config.patientIds.map(id => this.patientService.getPatient(id))
      );
    }

    // Otherwise, fetch patients by count
    const count = config.patientCount ?? 100;
    return this.patientService.getPatients(count);
  }

  /**
   * Submit batch evaluation to CQL Engine
   */
  private submitBatch(libraryId: string, patientIds: string[]): Observable<BatchEvaluationResponse> {
    if (patientIds.length === 0) {
      return throwError(() => new Error('No patient IDs to evaluate'));
    }

    return this.evaluationService.batchEvaluate(libraryId, patientIds);
  }

  /**
   * Monitor batch progress via WebSocket
   */
  private monitorBatchProgress(batchId: string): Observable<BatchProgressEvent> {
    return this.websocketService.batchProgress$.pipe(
      // Filter for this specific batch ID
      map(event => {
        // If event batchId matches or no batchId in event (single batch scenario)
        if (!event.batchId || event.batchId === batchId) {
          return event;
        }
        // Return null for non-matching batches
        return null;
      }),
      // Filter out nulls
      map(event => {
        if (!event) {
          throw new Error('Event does not match batch ID');
        }
        return event;
      })
    );
  }

  /**
   * Ensure WebSocket connection is established
   */
  private ensureWebSocketConnection(): void {
    const status = this.websocketService.getStatus();

    if (status === WebSocketStatus.DISCONNECTED || status === WebSocketStatus.ERROR) {
      this.logger.info('Connecting to WebSocket...');
      this.websocketService.connect();
    }
  }

  /**
   * Stop monitoring current batch
   */
  stopBatchMonitoring(): void {
    this.logger.info('Stopping batch monitoring');
    this.currentBatchId = undefined;
    this.updateState({ status: 'IDLE', batchId: undefined });
  }

  /**
   * Get historical evaluations for a library
   */
  getBatchHistory(libraryId: string): Observable<CqlEvaluation[]> {
    return this.evaluationService.getLibraryEvaluations(libraryId);
  }

  /**
   * Get current batch ID
   */
  getCurrentBatchId(): string | undefined {
    return this.currentBatchId;
  }

  /**
   * Get WebSocket connection status
   */
  getWebSocketStatus(): WebSocketStatus {
    return this.websocketService.getStatus();
  }

  /**
   * Get WebSocket status observable
   */
  get websocketStatus$(): Observable<WebSocketStatus> {
    return this.websocketService.status$;
  }

  /**
   * Manually connect WebSocket
   */
  connectWebSocket(): void {
    this.websocketService.connect();
  }

  /**
   * Manually disconnect WebSocket
   */
  disconnectWebSocket(): void {
    this.websocketService.disconnect();
  }

  /**
   * Update internal state
   */
  private updateState(partialState: Partial<BatchMonitorState>): void {
    this.stateSubject.pipe(take(1)).subscribe(currentState => {
      this.stateSubject.next({ ...currentState, ...partialState });
    });
  }

  /**
   * Reset service to initial state
   */
  reset(): void {
    this.currentBatchId = undefined;
    this.updateState({ status: 'IDLE', batchId: undefined, error: undefined });
  }
}
