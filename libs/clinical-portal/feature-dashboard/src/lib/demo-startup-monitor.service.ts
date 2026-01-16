import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, timer } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { environment } from '../../../../apps/clinical-portal/src/environments/environment';

export interface DemoServiceHealth {
  id: string;
  label: string;
  status: 'healthy' | 'unhealthy' | 'unknown';
  detail?: string;
}

export interface DemoStartupSnapshot {
  services: DemoServiceHealth[];
  prometheusReachable: boolean;
  grafanaReachable: boolean;
  updatedAt: Date;
}

interface PrometheusTargetsResponse {
  data?: {
    activeTargets?: Array<{
      labels?: { job?: string };
      health?: string;
      lastError?: string;
    }>;
  };
}

@Injectable({
  providedIn: 'root',
})
export class DemoStartupMonitorService {
  private readonly prometheusUrl = environment.monitoring?.prometheusUrl ?? 'http://localhost:9090';
  private readonly grafanaUrl = environment.monitoring?.grafanaUrl ?? 'http://localhost:3001';
  private readonly refreshMs = environment.monitoring?.refreshMs ?? 5000;

  private readonly demoServices: Array<{ id: string; label: string; job: string }> = [
    { id: 'gateway', label: 'Gateway', job: 'gateway-service' },
    { id: 'fhir', label: 'FHIR Service', job: 'fhir-service' },
    { id: 'patient', label: 'Patient Service', job: 'patient-service' },
    { id: 'care-gap', label: 'Care Gap Service', job: 'care-gap-service' },
    { id: 'quality', label: 'Quality Measure Service', job: 'quality-measure-service' },
    { id: 'cql', label: 'CQL Engine Service', job: 'cql-engine-service' },
  ];

  constructor(private http: HttpClient) {}

  streamStatus(): Observable<DemoStartupSnapshot> {
    return timer(0, this.refreshMs).pipe(
      switchMap(() => this.fetchSnapshot()),
      catchError(() =>
        of({
          services: this.demoServices.map((service) => ({
            id: service.id,
            label: service.label,
            status: 'unknown',
            detail: 'Monitoring API unavailable',
          })),
          prometheusReachable: false,
          grafanaReachable: false,
          updatedAt: new Date(),
        })
      )
    );
  }

  private fetchSnapshot(): Observable<DemoStartupSnapshot> {
    return this.http
      .get<PrometheusTargetsResponse>(`${this.prometheusUrl}/api/v1/targets`)
      .pipe(
        map((response) => {
          const activeTargets = response?.data?.activeTargets ?? [];
          const services = this.demoServices.map((service) => {
            const target = activeTargets.find((item) => item.labels?.job === service.job);
            if (!target) {
              return {
                id: service.id,
                label: service.label,
                status: 'unknown',
                detail: 'Awaiting scrape',
              };
            }
            const isUp = target.health === 'up';
            return {
              id: service.id,
              label: service.label,
              status: isUp ? 'healthy' : 'unhealthy',
              detail: isUp ? 'Healthy' : target.lastError || 'Unhealthy',
            };
          });
          return {
            services,
            prometheusReachable: true,
            grafanaReachable: false,
            updatedAt: new Date(),
          };
        }),
        switchMap((snapshot) =>
          this.http.get(`${this.grafanaUrl}/api/health`, { observe: 'response' }).pipe(
            map((response) => ({
              ...snapshot,
              grafanaReachable: response.status === 200,
            })),
            catchError(() =>
              of({
                ...snapshot,
                grafanaReachable: false,
              })
            )
          )
        ),
        catchError(() =>
          of({
            services: this.demoServices.map((service) => ({
              id: service.id,
              label: service.label,
              status: 'unknown',
              detail: 'Prometheus unavailable',
            })),
            prometheusReachable: false,
            grafanaReachable: false,
            updatedAt: new Date(),
          })
        )
      );
  }
}
