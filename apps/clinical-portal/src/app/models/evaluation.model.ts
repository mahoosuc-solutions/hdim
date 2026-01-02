/**
 * CQL Evaluation Model - Matches backend CqlEvaluation entity
 */
export interface CqlEvaluation {
  id: string; // UUID
  tenantId: string;
  library?: {
    id: string;
    name: string;
    version: string;
  };
  patientId: string;
  contextData?: Record<string, unknown>; // JSON
  evaluationResult?: Record<string, unknown>; // JSON
  status: EvaluationStatus;
  errorMessage?: string;
  durationMs?: number;
  evaluationDate: string; // ISO date string
  createdAt: string; // ISO date string
}

export type EvaluationStatus = 'SUCCESS' | 'FAILED' | 'PENDING';

/**
 * Evaluation Request - For submitting evaluations
 */
export interface EvaluationRequest {
  libraryId: string; // UUID
  patientId: string;
  contextData?: Record<string, unknown>;
}

/**
 * Batch Evaluation Request
 */
export interface BatchEvaluationRequest {
  libraryId: string; // UUID
  patientIds: string[];
  contextData?: Record<string, unknown>;
}

/**
 * Evaluation Response with results
 */
export interface EvaluationResponse {
  evaluationId: string;
  status: EvaluationStatus;
  result?: unknown;
  error?: string;
  durationMs?: number;
}

/**
 * Batch Evaluation Response - Backend returns List<CqlEvaluation>
 * This is a direct array of evaluations, not a wrapped object
 */
export type BatchEvaluationResponse = CqlEvaluation[];
