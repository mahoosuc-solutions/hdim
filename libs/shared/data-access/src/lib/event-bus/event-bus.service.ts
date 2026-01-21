/**
 * Inter-MFE Event Bus
 *
 * Enables communication between micro frontends for:
 * - Clinical workflow state synchronization
 * - 360 data pipeline events
 * - Patient context updates
 * - Quality measure evaluations
 *
 * IMPORTANT: This service maintains the single source of truth for clinical data
 * across all MFEs, ensuring data consistency in the 360 pipeline.
 */

import { Injectable } from '@angular/core';
import { Subject, Observable, BehaviorSubject } from 'rxjs';
import { filter } from 'rxjs/operators';

/**
 * Clinical event types for the 360 data pipeline
 */
export enum ClinicalEventType {
  // Patient events
  PATIENT_SELECTED = 'patient:selected',
  PATIENT_UPDATED = 'patient:updated',
  PATIENT_DEMOGRAPHICS_CHANGED = 'patient:demographics:changed',

  // Workflow events
  WORKFLOW_STARTED = 'workflow:started',
  WORKFLOW_STEP_COMPLETED = 'workflow:step:completed',
  WORKFLOW_CLOSED = 'workflow:closed',

  // Quality measure events
  MEASURE_EVALUATION_STARTED = 'measure:evaluation:started',
  MEASURE_EVALUATION_COMPLETED = 'measure:evaluation:completed',
  MEASURE_EVALUATION_CACHED = 'measure:evaluation:cached',

  // Care gap events
  CARE_GAP_IDENTIFIED = 'care-gap:identified',
  CARE_GAP_RESOLVED = 'care-gap:resolved',

  // 360 pipeline events
  DATA_PIPELINE_INITIALIZED = 'pipeline:initialized',
  DATA_PIPELINE_STEP_COMPLETE = 'pipeline:step:complete',
  DATA_PIPELINE_READY = 'pipeline:ready',

  // Tenant events
  TENANT_SWITCHED = 'tenant:switched',
}

/**
 * Base clinical event
 */
export interface ClinicalEvent<T = unknown> {
  type: ClinicalEventType;
  timestamp: number;
  source: string; // MFE name that generated the event
  data: T;
}

/**
 * Patient selected event
 */
export interface PatientSelectedEvent extends ClinicalEvent<{ patientId: string; tenantId: string }> {
  type: ClinicalEventType.PATIENT_SELECTED;
}

/**
 * Workflow started event
 */
export interface WorkflowStartedEvent extends ClinicalEvent<{ workflowId: string; patientId: string; type: string }> {
  type: ClinicalEventType.WORKFLOW_STARTED;
}

/**
 * Measure evaluation event
 */
export interface MeasureEvaluationEvent extends ClinicalEvent<{ measureId: string; patientId: string; result?: unknown }> {
  type: ClinicalEventType.MEASURE_EVALUATION_COMPLETED;
}

/**
 * Inter-MFE Event Bus Service
 *
 * Usage:
 * ```typescript
 * constructor(private eventBus: EventBusService) {}
 *
 * ngOnInit() {
 *   this.eventBus.on(ClinicalEventType.PATIENT_SELECTED).subscribe(event => {
 *     console.log('Patient selected:', event.data.patientId);
 *   });
 * }
 *
 * selectPatient(patientId: string) {
 *   this.eventBus.emit({
 *     type: ClinicalEventType.PATIENT_SELECTED,
 *     source: 'mfe-patients',
 *     data: { patientId, tenantId: this.tenantId }
 *   });
 * }
 * ```
 */
@Injectable({
  providedIn: 'root',
})
export class EventBusService {
  private eventSubject = new Subject<ClinicalEvent>();

  // Track current patient context for the 360 pipeline
  private currentPatientSubject = new BehaviorSubject<{ patientId: string | null; tenantId: string | null }>({
    patientId: null,
    tenantId: null,
  });

  public currentPatient$ = this.currentPatientSubject.asObservable();

  /**
   * Emit a clinical event to all subscribed MFEs
   */
  emit<T = unknown>(event: ClinicalEvent<T>): void {
    const eventWithTimestamp: ClinicalEvent<T> = {
      ...event,
      timestamp: Date.now(),
    };

    // Update patient context if this is a patient selection event
    if (event.type === ClinicalEventType.PATIENT_SELECTED) {
      const patientEvent = event as PatientSelectedEvent;
      this.currentPatientSubject.next({
        patientId: patientEvent.data.patientId,
        tenantId: patientEvent.data.tenantId,
      });
    }

    this.eventSubject.next(eventWithTimestamp);
  }

  /**
   * Subscribe to specific event types
   *
   * Usage: eventBus.on(ClinicalEventType.PATIENT_SELECTED)
   */
  on<T = unknown>(eventType: ClinicalEventType): Observable<ClinicalEvent<T>> {
    return this.eventSubject.pipe(
      filter((event) => event.type === eventType)
    ) as Observable<ClinicalEvent<T>>;
  }

  /**
   * Subscribe to all events (for debugging/monitoring)
   */
  onAll(): Observable<ClinicalEvent> {
    return this.eventSubject.asObservable();
  }

  /**
   * Get current patient context
   */
  getCurrentPatient(): { patientId: string | null; tenantId: string | null } {
    return this.currentPatientSubject.value;
  }

  /**
   * Clear patient context (e.g., on logout)
   */
  clearPatientContext(): void {
    this.currentPatientSubject.next({ patientId: null, tenantId: null });
  }
}
