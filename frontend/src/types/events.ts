/**
 * TypeScript event type definitions mirroring backend Java event classes
 * Backend: com.healthdata.cql.event
 */

export enum EventType {
  EVALUATION_STARTED = 'EVALUATION_STARTED',
  EVALUATION_COMPLETED = 'EVALUATION_COMPLETED',
  EVALUATION_FAILED = 'EVALUATION_FAILED',
  BATCH_STARTED = 'BATCH_STARTED',
  BATCH_PROGRESS = 'BATCH_PROGRESS',
  BATCH_COMPLETED = 'BATCH_COMPLETED',
  CACHE_HIT = 'CACHE_HIT',
  CACHE_MISS = 'CACHE_MISS',
  TEMPLATE_LOADED = 'TEMPLATE_LOADED'
}

export enum FailureCategory {
  FHIR_FETCH_ERROR = 'FHIR_FETCH_ERROR',
  CQL_PARSE_ERROR = 'CQL_PARSE_ERROR',
  TIMEOUT_ERROR = 'TIMEOUT_ERROR',
  RUNTIME_ERROR = 'RUNTIME_ERROR',
  UNKNOWN_ERROR = 'UNKNOWN_ERROR'
}

/**
 * Base interface for all evaluation events
 */
export interface EvaluationEvent {
  eventId: string;
  eventType: EventType;
  tenantId: string;
  timestamp: number; // Unix timestamp in milliseconds
  evaluationId: string;
}

/**
 * Published when a single patient evaluation starts
 */
export interface EvaluationStartedEvent extends EvaluationEvent {
  eventType: EventType.EVALUATION_STARTED;
  measureId: string;
  measureName?: string;
  patientId: string;
  batchId?: string;
}

/**
 * Published when a single patient evaluation completes successfully
 */
export interface EvaluationCompletedEvent extends EvaluationEvent {
  eventType: EventType.EVALUATION_COMPLETED;
  measureId: string;
  measureName?: string;
  patientId: string;
  batchId?: string;
  inDenominator: boolean;
  inNumerator: boolean;
  complianceRate: number; // 0.0 to 1.0
  score: number;
  durationMs: number;
  evidence: Record<string, any>;
  careGapCount: number;
}

/**
 * Published when a single patient evaluation fails
 */
export interface EvaluationFailedEvent extends EvaluationEvent {
  eventType: EventType.EVALUATION_FAILED;
  measureId: string;
  measureName?: string;
  patientId: string;
  batchId?: string;
  errorMessage: string;
  errorCategory: FailureCategory;
  stackTrace?: string;
  durationMs: number;
}

/**
 * PRIMARY VISUALIZATION EVENT - Real-time batch progress updates
 * Published every 5 seconds OR every 10 patients (whichever comes first)
 */
export interface BatchProgressEvent {
  eventType: EventType.BATCH_PROGRESS;
  batchId: string;
  tenantId: string;
  measureId: string;
  measureName: string;

  // Progress metrics
  totalPatients: number;
  completedCount: number;
  successCount: number;
  failedCount: number;
  pendingCount: number;
  percentComplete: number; // 0.0 to 100.0

  // Performance metrics
  avgDurationMs: number;
  currentThroughput: number; // evaluations per second
  elapsedTimeMs: number;
  estimatedTimeRemainingMs: number;

  // Clinical quality metrics
  denominatorCount: number;
  numeratorCount: number;
  cumulativeComplianceRate: number; // 0.0 to 100.0

  timestamp: number;
}

/**
 * WebSocket message wrapper from backend
 */
export interface WebSocketMessage {
  type: 'EVALUATION_EVENT' | 'WELCOME' | 'ERROR' | 'CONNECTION_ESTABLISHED';
  timestamp: number;
  data?: any;
  message?: string;
}

/**
 * Union type for all event types (for discriminated unions)
 */
export type AnyEvaluationEvent =
  | EvaluationStartedEvent
  | EvaluationCompletedEvent
  | EvaluationFailedEvent
  | BatchProgressEvent;

/**
 * Type guards for event type checking
 */
export const isEvaluationStartedEvent = (event: any): event is EvaluationStartedEvent => {
  return event.eventType === EventType.EVALUATION_STARTED;
};

export const isEvaluationCompletedEvent = (event: any): event is EvaluationCompletedEvent => {
  return event.eventType === EventType.EVALUATION_COMPLETED;
};

export const isEvaluationFailedEvent = (event: any): event is EvaluationFailedEvent => {
  return event.eventType === EventType.EVALUATION_FAILED;
};

export const isBatchProgressEvent = (event: any): event is BatchProgressEvent => {
  return event.eventType === EventType.BATCH_PROGRESS;
};
