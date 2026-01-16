import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, forkJoin, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { API_CONFIG } from '../config/api.config';

export interface ServiceHealthResult {
  healthy: boolean;
  statusCode?: number;
  responseTime?: number;
  error?: string;
  serviceName?: string;
}

export interface TestDataValidationResult {
  valid: boolean;
  message: string;
  details?: {
    patients: number;
    careGaps: number;
    qualityMeasures: number;
  };
  timestamp: Date;
}

export interface ApiTestResult {
  success: boolean;
  status?: number;
  responseSummary?: string;
  error?: string;
  responseTime?: number;
  serviceName?: string;
  endpoint?: string;
  method?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface PatientResponse {
  id: string;
  mrn: string;
  name: string;
  [key: string]: any;
}

export interface CareGapResponse {
  id: string;
  patientId: string;
  priority: string;
  [key: string]: any;
}

export interface QualityMeasureResponse {
  id: string;
  patientId: string;
  measureId: string;
  [key: string]: any;
}

/**
 * Testing Service
 * 
 * Provides utilities for testing, data management, and service health checks.
 */
@Injectable({
  providedIn: 'root',
})
export class TestingService {
  private readonly tenantId = 'acme-health'; // Default tenant for testing

  constructor(private http: HttpClient) {}

  /**
   * Check health of a specific service
   */
  checkServiceHealth(serviceName: string): Observable<ServiceHealthResult> {
    const healthUrl = this.getServiceHealthUrl(serviceName);
    const startTime = Date.now();

    return this.http.get<any>(healthUrl, { observe: 'response' }).pipe(
      map((response) => {
        const responseTime = Date.now() - startTime;
        return {
          healthy: response.status === 200,
          statusCode: response.status,
          responseTime,
          serviceName,
        };
      }),
      catchError((error) => {
        const responseTime = Date.now() - startTime;
        const errorMessage = error.status 
          ? `HTTP ${error.status}: ${error.statusText || 'Service unavailable'}`
          : error.message || 'Service unavailable - connection failed';
        return of({
          healthy: false,
          statusCode: error.status || 0,
          responseTime,
          error: errorMessage,
          serviceName,
        });
      })
    );
  }

  /**
   * Check health of all services
   */
  checkAllServiceHealth(): Observable<Map<string, ServiceHealthResult>> {
    const services = [
      'patient-service',
      'care-gap-service',
      'quality-measure-service',
      'fhir-service',
      'demo-seeding-service',
      'gateway-service',
    ];

    const healthChecks = services.map((service) =>
      this.checkServiceHealth(service).pipe(
        map((result) => ({ service, result }))
      )
    );

    return forkJoin(healthChecks).pipe(
      map((results) => {
        const healthMap = new Map<string, ServiceHealthResult>();
        results.forEach(({ service, result }) => {
          healthMap.set(service, result);
        });
        return healthMap;
      }),
      catchError((error) => {
        // Return all as unhealthy on error
        const healthMap = new Map<string, ServiceHealthResult>();
        const errorMessage = error.message || 'Health check failed - unable to reach services';
        services.forEach((service) => {
          healthMap.set(service, {
            healthy: false,
            error: errorMessage,
            serviceName: service,
          });
        });
        return of(healthMap);
      })
    );
  }

  /**
   * Seed test data
   */
  seedTestData(): Observable<void> {
    // Use demo seeding service to seed data
    const demoApiUrl = API_CONFIG.API_GATEWAY_URL ? `${API_CONFIG.API_GATEWAY_URL}/demo` : '/demo';
    const url = `${demoApiUrl}/api/v1/demo/scenarios/hedis-evaluation`;
    return this.http.post<void>(url, {}, {
      headers: {
        'X-Tenant-ID': this.tenantId,
      },
    });
  }

  /**
   * Validate test data
   */
  validateTestData(): Observable<TestDataValidationResult> {
    // Check if we have patients, care gaps, and quality measures
    const patientUrl = `${API_CONFIG.PATIENT_URL}/api/v1/patients?page=0&size=1`;
    const careGapUrl = `${API_CONFIG.CARE_GAP_URL}/api/v1/care-gaps?page=0&size=1`;
    const qualityMeasureUrl = `${API_CONFIG.QUALITY_MEASURE_URL}/api/v1/quality-measures/results?page=0&size=1`;

    const checks = [
      this.http.get<PaginatedResponse<PatientResponse>>(patientUrl, { headers: { 'X-Tenant-ID': this.tenantId } }).pipe(
        map((response) => ({ type: 'patients', count: response.totalElements || 0, error: null })),
        catchError((error) => of({ 
          type: 'patients', 
          count: 0, 
          error: `Patient service error: ${error.status ? `HTTP ${error.status}` : error.message}` 
        }))
      ),
      this.http.get<PaginatedResponse<CareGapResponse>>(careGapUrl, { headers: { 'X-Tenant-ID': this.tenantId } }).pipe(
        map((response) => ({ type: 'careGaps', count: response.totalElements || 0, error: null })),
        catchError((error) => of({ 
          type: 'careGaps', 
          count: 0, 
          error: `Care gap service error: ${error.status ? `HTTP ${error.status}` : error.message}` 
        }))
      ),
      this.http.get<PaginatedResponse<QualityMeasureResponse>>(qualityMeasureUrl, { headers: { 'X-Tenant-ID': this.tenantId } }).pipe(
        map((response) => ({ type: 'qualityMeasures', count: response.totalElements || 0, error: null })),
        catchError((error) => of({ 
          type: 'qualityMeasures', 
          count: 0, 
          error: `Quality measure service error: ${error.status ? `HTTP ${error.status}` : error.message}` 
        }))
      ),
    ];

    return forkJoin(checks).pipe(
      map((results) => {
        const patients = results.find((r) => r.type === 'patients')?.count || 0;
        const careGaps = results.find((r) => r.type === 'careGaps')?.count || 0;
        const qualityMeasures = results.find((r) => r.type === 'qualityMeasures')?.count || 0;
        const errors = results.filter((r) => r.error).map((r) => r.error).join('; ');

        const hasData = patients > 0 || careGaps > 0 || qualityMeasures > 0;
        let message: string;
        
        if (errors) {
          message = `Validation completed with errors: ${errors}. Found ${patients} patients, ${careGaps} care gaps, ${qualityMeasures} quality measures`;
        } else if (hasData) {
          message = `Validation passed: Found ${patients} patients, ${careGaps} care gaps, ${qualityMeasures} quality measures`;
        } else {
          message = 'Validation failed: No test data found. Please seed test data first.';
        }

        return {
          valid: hasData,
          message,
          details: { patients, careGaps, qualityMeasures },
          timestamp: new Date(),
        };
      }),
      catchError((error) => {
        return of({
          valid: false,
          message: `Validation failed: ${error.status ? `HTTP ${error.status} - ` : ''}${error.message || 'Unknown error'}`,
          timestamp: new Date(),
        });
      })
    );
  }

  /**
   * Reset test data
   */
  resetTestData(): Observable<void> {
    // Use demo seeding service to reset
    const demoApiUrl = API_CONFIG.API_GATEWAY_URL ? `${API_CONFIG.API_GATEWAY_URL}/demo` : '/demo';
    const url = `${demoApiUrl}/api/v1/demo/reset`;
    return this.http.post<void>(url, {}, {
      headers: {
        'X-Tenant-ID': this.tenantId,
      },
    });
  }

  /**
   * Test an API endpoint
   */
  testApiEndpoint(service: string, endpoint: string, method: string = 'GET'): Observable<ApiTestResult> {
    const url = this.getServiceBaseUrl(service) + endpoint;
    const startTime = Date.now();

    const request = this.http.request(method, url, {
      headers: {
        'X-Tenant-ID': this.tenantId,
        'Accept': 'application/json',
      },
      observe: 'response',
    });

    return request.pipe(
      map((response) => {
        const responseTime = Date.now() - startTime;
        const body = response.body as any;
        let responseSummary: string;
        
        if (!body) {
          responseSummary = 'No response body';
        } else if (Array.isArray(body)) {
          responseSummary = `Array with ${body.length} items`;
        } else if (typeof body === 'object') {
          if (body.content && Array.isArray(body.content)) {
            // Paginated response
            responseSummary = `Paginated response: ${body.totalElements || body.content.length} total items, page ${body.number || 0}`;
          } else {
            const keyCount = Object.keys(body).length;
            responseSummary = keyCount > 0 
              ? `Object with ${keyCount} properties: ${Object.keys(body).slice(0, 3).join(', ')}${keyCount > 3 ? '...' : ''}`
              : 'Empty object';
          }
        } else {
          responseSummary = String(body).substring(0, 100);
        }

        return {
          success: response.status >= 200 && response.status < 300,
          status: response.status,
          responseSummary,
          responseTime,
          serviceName: service,
          endpoint,
          method,
        };
      }),
      catchError((error) => {
        const responseTime = Date.now() - startTime;
        const errorMessage = error.status
          ? `HTTP ${error.status} ${error.statusText || ''}: ${error.error?.message || error.message || 'Request failed'}`
          : error.message || 'Request failed - connection error';
        
        return of({
          success: false,
          status: error.status || 0,
          error: errorMessage,
          responseTime,
          serviceName: service,
          endpoint,
          method,
        });
      })
    );
  }

  /**
   * Get base URL for a service
   */
  getServiceBaseUrl(service: string): string {
    const demoApiUrl = API_CONFIG.API_GATEWAY_URL ? `${API_CONFIG.API_GATEWAY_URL}/demo` : '/demo';
    const serviceMap: Record<string, string> = {
      'patient-service': API_CONFIG.PATIENT_URL,
      'care-gap-service': API_CONFIG.CARE_GAP_URL,
      'quality-measure-service': API_CONFIG.QUALITY_MEASURE_URL,
      'fhir-service': API_CONFIG.FHIR_SERVER_URL,
      'demo-seeding-service': demoApiUrl,
      'gateway-service': API_CONFIG.API_GATEWAY_URL || 'http://localhost:18080',
    };
    return serviceMap[service] || '';
  }

  /**
   * Get health check URL for a service
   */
  private getServiceHealthUrl(serviceName: string): string {
    const baseUrl = this.getServiceBaseUrl(serviceName);
    // Most services use /actuator/health
    return `${baseUrl}/actuator/health`;
  }
}
