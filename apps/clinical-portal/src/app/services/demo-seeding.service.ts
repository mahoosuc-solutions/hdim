import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_CONFIG } from '../config/api.config';

export interface DemoStatusResponse {
  ready: boolean;
  scenarioCount: number;
  templateCount: number;
  currentSessionId?: string;
  currentScenario?: string;
  sessionStatus?: string;
}

export interface DemoScenarioResponse {
  name: string;
  displayName: string;
  description?: string;
  scenarioType?: string;
  patientCount?: number;
  estimatedLoadTimeSeconds?: number;
  tenantId?: string;
}

export interface DemoProgressResponse {
  sessionId: string;
  scenarioName?: string;
  tenantId?: string;
  stage: string;
  progressPercent: number;
  patientsGenerated?: number | null;
  patientsPersisted?: number | null;
  careGapsCreated?: number | null;
  measuresSeeded?: number | null;
  message?: string;
  updatedAt?: string;
  cancelRequested?: boolean;
}

export interface DemoActionResponse {
  success?: boolean;
  errorMessage?: string;
  message?: string;
  sessionId?: string;
  scenarioName?: string;
  patientCount?: number;
  careGapCount?: number;
  loadTimeMs?: number;
}

@Injectable({ providedIn: 'root' })
export class DemoSeedingService {
  private readonly demoBaseUrl = API_CONFIG.API_GATEWAY_URL
    ? `${API_CONFIG.API_GATEWAY_URL}/demo`
    : '/demo';

  constructor(private readonly http: HttpClient) {}

  getStatus(): Observable<DemoStatusResponse> {
    return this.http.get<DemoStatusResponse>(`${this.demoBaseUrl}/api/v1/demo/status`);
  }

  getProgress(): Observable<DemoProgressResponse> {
    return this.http.get<DemoProgressResponse>(`${this.demoBaseUrl}/api/v1/demo/sessions/current/progress`);
  }

  listScenarios(): Observable<DemoScenarioResponse[]> {
    return this.http.get<DemoScenarioResponse[]>(`${this.demoBaseUrl}/api/v1/demo/scenarios`);
  }

  loadScenario(name: string): Observable<DemoActionResponse> {
    return this.http.post<DemoActionResponse>(`${this.demoBaseUrl}/api/v1/demo/scenarios/${name}`, {});
  }

  reloadScenario(): Observable<DemoActionResponse> {
    return this.http.post<DemoActionResponse>(`${this.demoBaseUrl}/api/v1/demo/scenarios/reload`, {});
  }

  cancelCurrent(): Observable<void> {
    return this.http.post<void>(`${this.demoBaseUrl}/api/v1/demo/sessions/current/cancel`, {});
  }

  stopCurrent(): Observable<void> {
    return this.http.post<void>(`${this.demoBaseUrl}/api/v1/demo/sessions/current/stop`, {});
  }

  resetCurrentTenant(): Observable<DemoActionResponse> {
    return this.http.post<DemoActionResponse>(`${this.demoBaseUrl}/api/v1/demo/reset/current-tenant`, {});
  }

  fullReset(): Observable<DemoActionResponse> {
    return this.http.post<DemoActionResponse>(`${this.demoBaseUrl}/api/v1/demo/reset`, {});
  }
}
