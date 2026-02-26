import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_CONFIG } from '../config/api.config';

export interface RunQueuedResponse {
  runId?: string | null;
  message: string;
}

export interface OperationRunSummary {
  id: string;
  operationType: string;
  status: string;
  parameters?: Record<string, unknown> | null;
  requestedBy: string;
  requestedAt: string;
  startedAt?: string | null;
  completedAt?: string | null;
  summary?: string | null;
  exitCode?: number | null;
  logOutput?: string | null;
  cancelRequested?: boolean;
  validation?: ValidationScorecard | null;
}

export interface OperationRunStep {
  id: string;
  stepOrder: number;
  stepName: string;
  status: string;
  commandText?: string | null;
  message?: string | null;
  output?: string | null;
  startedAt?: string | null;
  completedAt?: string | null;
}

export interface OperationRunDetail {
  run: OperationRunSummary;
  steps: OperationRunStep[];
}

export interface ValidationGate {
  gateKey: string;
  gateName: string;
  critical: boolean;
  weight: number;
  status: string;
  actualValue?: string | null;
  expectedValue?: string | null;
  evidenceText?: string | null;
  measuredAt: string;
}

export interface ValidationScorecard {
  score: number;
  grade: string;
  criticalPass: boolean;
  passed: boolean;
  gates: ValidationGate[];
  createdAt: string;
}

export interface OperationSystemStatus {
  runningCount: number;
  latestStackStart?: OperationRunSummary | null;
  latestStackStop?: OperationRunSummary | null;
  latestStackRestart?: OperationRunSummary | null;
  latestSeedSmoke?: OperationRunSummary | null;
  latestSeedFull?: OperationRunSummary | null;
  latestValidate?: OperationRunSummary | null;
}

export interface SeedRunRequest {
  profile: 'smoke' | 'full';
  scheduleMode: 'none' | 'appointment-task' | 'encounter' | 'both';
}

@Injectable({ providedIn: 'root' })
export class OperationsService {
  private readonly baseUrl = API_CONFIG.API_GATEWAY_URL
    ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/ops`
    : '/api/v1/ops';

  constructor(private readonly http: HttpClient) {}

  startStack(): Observable<RunQueuedResponse> {
    return this.http.post<RunQueuedResponse>(`${this.baseUrl}/stack/start`, {
      idempotencyKey: this.idempotencyKey('stack-start'),
    });
  }

  stopStack(): Observable<RunQueuedResponse> {
    return this.http.post<RunQueuedResponse>(`${this.baseUrl}/stack/stop`, {
      idempotencyKey: this.idempotencyKey('stack-stop'),
    });
  }

  restartStack(): Observable<RunQueuedResponse> {
    return this.http.post<RunQueuedResponse>(`${this.baseUrl}/stack/restart`, {
      idempotencyKey: this.idempotencyKey('stack-restart'),
    });
  }

  runSeed(request: SeedRunRequest): Observable<RunQueuedResponse> {
    return this.http.post<RunQueuedResponse>(`${this.baseUrl}/seed/run`, {
      ...request,
      idempotencyKey: this.idempotencyKey(`seed-${request.profile}-${request.scheduleMode}`),
    });
  }

  runValidate(): Observable<RunQueuedResponse> {
    return this.http.post<RunQueuedResponse>(`${this.baseUrl}/validate/run`, {
      idempotencyKey: this.idempotencyKey('validate'),
    });
  }

  cancelRun(runId: string): Observable<RunQueuedResponse> {
    return this.http.post<RunQueuedResponse>(`${this.baseUrl}/runs/${runId}/cancel`, {});
  }

  listRuns(limit = 50): Observable<OperationRunSummary[]> {
    return this.http.get<OperationRunSummary[]>(`${this.baseUrl}/runs?limit=${limit}`);
  }

  getRun(runId: string): Observable<OperationRunDetail> {
    return this.http.get<OperationRunDetail>(`${this.baseUrl}/runs/${runId}`);
  }

  getSystemStatus(): Observable<OperationSystemStatus> {
    return this.http.get<OperationSystemStatus>(`${this.baseUrl}/system-status`);
  }

  private idempotencyKey(prefix: string): string {
    const minuteBucket = new Date().toISOString().slice(0, 16);
    return `${prefix}-${minuteBucket}`;
  }
}
