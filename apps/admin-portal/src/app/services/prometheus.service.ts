import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { LoggerService } from './logger.service';

/**
 * Prometheus Query Service
 *
 * Provides methods to query Prometheus metrics using PromQL.
 * Handles HTTP requests to Prometheus API and transforms responses.
 *
 * IMPORTANT: This service requires Prometheus to be running at the configured URL.
 * Default: http://localhost:9090
 */
@Injectable({
  providedIn: 'root',
})
export class PrometheusService {
  private prometheusUrl = 'http://localhost:9090';
  private logger: ReturnType<LoggerService['withContext']>;

  constructor(
    private http: HttpClient,
    private loggerService: LoggerService
  ) {
    this.logger = this.loggerService.withContext('PrometheusService');
  }

  /**
   * Execute an instant PromQL query
   * Returns the current value of the query at the current time
   *
   * @param query PromQL query string (e.g., 'up{job="hdim-services"}')
   * @param time Optional timestamp (default: current time)
   * @returns Observable<PrometheusQueryResult>
   */
  query(query: string, time?: number): Observable<PrometheusQueryResult> {
    this.logger.info('Executing Prometheus query', { query });

    let params = new HttpParams().set('query', query);
    if (time) {
      params = params.set('time', time.toString());
    }

    return this.http
      .get<PrometheusApiResponse>(`${this.prometheusUrl}/api/v1/query`, { params })
      .pipe(
        map((response) => {
          if (response.status !== 'success') {
            throw new Error(`Prometheus query failed: ${response.error || 'Unknown error'}`);
          }
          return response.data;
        }),
        catchError((error) => {
          this.logger.error('Prometheus query failed', error);
          return throwError(() => error);
        })
      );
  }

  /**
   * Execute a range PromQL query
   * Returns values over a time range
   *
   * @param query PromQL query string
   * @param start Start timestamp (Unix seconds)
   * @param end End timestamp (Unix seconds)
   * @param step Query resolution step (e.g., '15s', '1m', '5m')
   * @returns Observable<PrometheusQueryResult>
   */
  queryRange(
    query: string,
    start: number,
    end: number,
    step: string
  ): Observable<PrometheusQueryResult> {
    this.logger.info('Executing Prometheus range query', { query, start, end, step });

    const params = new HttpParams()
      .set('query', query)
      .set('start', start.toString())
      .set('end', end.toString())
      .set('step', step);

    return this.http
      .get<PrometheusApiResponse>(`${this.prometheusUrl}/api/v1/query_range`, { params })
      .pipe(
        map((response) => {
          if (response.status !== 'success') {
            throw new Error(`Prometheus range query failed: ${response.error || 'Unknown error'}`);
          }
          return response.data;
        }),
        catchError((error) => {
          this.logger.error('Prometheus range query failed', error);
          return throwError(() => error);
        })
      );
  }

  /**
   * Get CPU usage for a specific service
   * Uses rate() function to calculate CPU usage over 1 minute
   *
   * @param serviceName Name of the service (e.g., 'patient-service')
   * @returns Observable<number> CPU usage as percentage (0-100)
   */
  getCpuUsage(serviceName: string): Observable<number> {
    const query = `rate(process_cpu_seconds_total{job="hdim-services",instance=~"${serviceName}.*"}[1m]) * 100`;
    return this.query(query).pipe(
      map((result) => this.extractScalarValue(result) || 0)
    );
  }

  /**
   * Get memory usage for a specific service
   * Returns memory in MB
   *
   * @param serviceName Name of the service
   * @returns Observable<number> Memory usage in MB
   */
  getMemoryUsage(serviceName: string): Observable<number> {
    const query = `process_resident_memory_bytes{job="hdim-services",instance=~"${serviceName}.*"} / 1024 / 1024`;
    return this.query(query).pipe(
      map((result) => this.extractScalarValue(result) || 0)
    );
  }

  /**
   * Get request rate for a specific service
   * Returns requests per second
   *
   * @param serviceName Name of the service
   * @returns Observable<number> Requests per second
   */
  getRequestRate(serviceName: string): Observable<number> {
    const query = `rate(http_server_requests_seconds_count{job="hdim-services",instance=~"${serviceName}.*"}[1m])`;
    return this.query(query).pipe(
      map((result) => this.extractScalarValue(result) || 0)
    );
  }

  /**
   * Get error rate for a specific service
   * Returns errors per second (HTTP 5xx status codes)
   *
   * @param serviceName Name of the service
   * @returns Observable<number> Errors per second
   */
  getErrorRate(serviceName: string): Observable<number> {
    const query = `rate(http_server_requests_seconds_count{job="hdim-services",instance=~"${serviceName}.*",status=~"5.."}[1m])`;
    return this.query(query).pipe(
      map((result) => this.extractScalarValue(result) || 0)
    );
  }

  /**
   * Get P95 latency for a specific service
   * Returns 95th percentile response time in milliseconds
   *
   * @param serviceName Name of the service
   * @returns Observable<number> P95 latency in ms
   */
  getP95Latency(serviceName: string): Observable<number> {
    const query = `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{job="hdim-services",instance=~"${serviceName}.*"}[5m])) * 1000`;
    return this.query(query).pipe(
      map((result) => this.extractScalarValue(result) || 0)
    );
  }

  /**
   * Get P99 latency for a specific service
   * Returns 99th percentile response time in milliseconds
   *
   * @param serviceName Name of the service
   * @returns Observable<number> P99 latency in ms
   */
  getP99Latency(serviceName: string): Observable<number> {
    const query = `histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{job="hdim-services",instance=~"${serviceName}.*"}[5m])) * 1000`;
    return this.query(query).pipe(
      map((result) => this.extractScalarValue(result) || 0)
    );
  }

  /**
   * Get all metrics for a specific service
   * Convenience method that fetches CPU, memory, request rate, error rate,
   * and latency percentiles (P95/P99)
   *
   * @param serviceName Name of the service
   * @returns Observable<ServiceMetrics>
   */
  getAllMetrics(serviceName: string): Observable<ServiceMetrics> {
    // In a production app, we'd use forkJoin to parallelize these queries
    // For now, we'll fetch them sequentially for simplicity
    return new Observable((observer) => {
      const metrics: Partial<ServiceMetrics> = { serviceName };

      this.getCpuUsage(serviceName).subscribe({
        next: (cpu) => {
          metrics.cpuUsage = cpu;
          if (Object.keys(metrics).length === 7) {
            observer.next(metrics as ServiceMetrics);
            observer.complete();
          }
        },
        error: (err) => observer.error(err),
      });

      this.getMemoryUsage(serviceName).subscribe({
        next: (memory) => {
          metrics.memoryUsageMb = memory;
          if (Object.keys(metrics).length === 7) {
            observer.next(metrics as ServiceMetrics);
            observer.complete();
          }
        },
        error: (err) => observer.error(err),
      });

      this.getRequestRate(serviceName).subscribe({
        next: (requestRate) => {
          metrics.requestsPerSecond = requestRate;
          if (Object.keys(metrics).length === 7) {
            observer.next(metrics as ServiceMetrics);
            observer.complete();
          }
        },
        error: (err) => observer.error(err),
      });

      this.getErrorRate(serviceName).subscribe({
        next: (errorRate) => {
          metrics.errorsPerSecond = errorRate;
          if (Object.keys(metrics).length === 7) {
            observer.next(metrics as ServiceMetrics);
            observer.complete();
          }
        },
        error: (err) => observer.error(err),
      });

      this.getP95Latency(serviceName).subscribe({
        next: (latency) => {
          metrics.p95LatencyMs = latency;
          if (Object.keys(metrics).length === 7) {
            observer.next(metrics as ServiceMetrics);
            observer.complete();
          }
        },
        error: (err) => observer.error(err),
      });

      this.getP99Latency(serviceName).subscribe({
        next: (latency) => {
          metrics.p99LatencyMs = latency;
          if (Object.keys(metrics).length === 7) {
            observer.next(metrics as ServiceMetrics);
            observer.complete();
          }
        },
        error: (err) => observer.error(err),
      });
    });
  }

  /**
   * Check if Prometheus is reachable
   * Useful for determining if we should use mock data or real data
   *
   * @returns Observable<boolean> True if Prometheus is reachable
   */
  isPrometheusAvailable(): Observable<boolean> {
    return this.http.get(`${this.prometheusUrl}/-/healthy`, { responseType: 'text' }).pipe(
      map(() => {
        this.logger.info('Prometheus is available');
        return true;
      }),
      catchError((error) => {
        this.logger.warn('Prometheus is not available, will use mock data', error);
        return of(false);
      })
    );
  }

  /**
   * Extract a scalar value from a Prometheus query result
   * Handles both vector and matrix result types
   *
   * @param result Prometheus query result
   * @returns number or null
   */
  private extractScalarValue(result: PrometheusQueryResult): number | null {
    if (result.resultType === 'vector' && result.result.length > 0) {
      const firstResult = result.result[0];
      if (firstResult?.value) {
        const value = firstResult.value[1];
        return typeof value === 'string' ? parseFloat(value) : value;
      }
    }
    if (result.resultType === 'matrix' && result.result.length > 0) {
      const firstResult = result.result[0];
      const values = firstResult?.values;
      if (values && values.length > 0) {
        const lastValue = values[values.length - 1][1];
        return typeof lastValue === 'string' ? parseFloat(lastValue) : lastValue;
      }
    }
    return null;
  }
}

/**
 * Prometheus API Response
 */
export interface PrometheusApiResponse {
  status: 'success' | 'error';
  data: PrometheusQueryResult;
  error?: string;
  errorType?: string;
}

/**
 * Prometheus Query Result
 */
export interface PrometheusQueryResult {
  resultType: 'matrix' | 'vector' | 'scalar' | 'string';
  result: PrometheusResultEntry[];
}

/**
 * Prometheus Result Entry
 */
export interface PrometheusResultEntry {
  metric: Record<string, string>;
  value?: [number, number | string]; // [timestamp, value] for instant queries
  values?: Array<[number, number | string]>; // [[timestamp, value], ...] for range queries
}

/**
 * Service Metrics Model
 */
export interface ServiceMetrics {
  serviceName: string;
  cpuUsage: number; // Percentage (0-100)
  memoryUsageMb: number; // MB
  requestsPerSecond: number;
  errorsPerSecond: number;
  p95LatencyMs: number; // Milliseconds
  p99LatencyMs: number; // Milliseconds
}
