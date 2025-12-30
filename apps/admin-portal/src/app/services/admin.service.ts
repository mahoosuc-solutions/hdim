import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, of, catchError, map } from 'rxjs';
import { API_CONFIG, ADMIN_ENDPOINTS, HTTP_HEADERS, buildAdminUrl } from '../config/api.config';
import {
  User,
  CreateUserRequest,
  UpdateUserRequest,
  Tenant,
  CreateTenantRequest,
  DashboardSnapshot,
  SystemHealth,
  SystemHealthSnapshot,
  ServiceDefinition,
  ApiPreset,
  AuditLog,
  AuditLogFilter,
  PagedResponse,
} from '../models/admin.model';

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      [HTTP_HEADERS.TENANT_ID]: API_CONFIG.DEFAULT_TENANT_ID,
      [HTTP_HEADERS.CONTENT_TYPE]: 'application/json',
    });
  }

  // =====================
  // Dashboard & Metrics
  // =====================

  getDashboard(): Observable<DashboardSnapshot> {
    return this.http.get<any>(buildAdminUrl(ADMIN_ENDPOINTS.DASHBOARD), {
      headers: this.getHeaders(),
    }).pipe(
      map((response) => ({
        metrics: {
          registeredPatients: response.registeredPatients || 0,
          patientReadLatencyP95Ms: response.patientReadLatencyP95Ms || 0,
          cacheHitRate: response.cacheHitRate || 0,
          activeUsers: response.activeUsers || 0,
          evaluationsToday: response.evaluationsToday || 0,
          careGapsOpen: response.careGapsOpen || 0,
        },
        services: response.services || [],
        alerts: response.alerts || [],
        timestamp: new Date(response.timestamp || Date.now()),
      })),
      catchError(() => of(this.getMockDashboard()))
    );
  }

  getSystemHealth(): Observable<SystemHealth> {
    return this.http.get<any>(buildAdminUrl(ADMIN_ENDPOINTS.SYSTEM_HEALTH), {
      headers: this.getHeaders(),
    }).pipe(
      map((response) => ({
        services: response.services || [],
        metrics: {
          cpuUsage: response.cpuUsage || 0,
          memoryUsage: response.memoryUsage || 0,
          diskUsage: response.diskUsage || 0,
          activeConnections: response.activeConnections || 0,
          requestsPerSecond: response.requestsPerSecond || 0,
          avgResponseTimeMs: response.avgResponseTimeMs || 0,
        },
        overallStatus: response.overallStatus || 'healthy',
        timestamp: new Date(response.timestamp || Date.now()),
      })),
      catchError(() => of(this.getMockSystemHealthV2()))
    );
  }

  getSystemHealthSnapshot(): Observable<SystemHealthSnapshot> {
    return this.http.get<any>(buildAdminUrl(ADMIN_ENDPOINTS.SYSTEM_HEALTH), {
      headers: this.getHeaders(),
    }).pipe(
      map((response) => ({
        dependencies: response.dependencies || [],
        queues: response.queues || [],
        overallStatus: response.overallStatus || 'HEALTHY',
        timestamp: new Date(response.timestamp || Date.now()),
      })),
      catchError(() => of(this.getMockSystemHealth()))
    );
  }

  getServiceCatalog(): Observable<ServiceDefinition[]> {
    return this.http.get<any>(buildAdminUrl(ADMIN_ENDPOINTS.SERVICE_CATALOG), {
      headers: this.getHeaders(),
    }).pipe(
      map((response) => response.services || []),
      catchError(() => of(this.getMockServiceCatalog()))
    );
  }

  getApiPresets(): Observable<ApiPreset[]> {
    return this.http.get<any>(buildAdminUrl(ADMIN_ENDPOINTS.API_PRESETS), {
      headers: this.getHeaders(),
    }).pipe(
      map((response) => response.presets || []),
      catchError(() => of([]))
    );
  }

  // =====================
  // User Management
  // =====================

  getUsers(page = 0, pageSize = 20): Observable<PagedResponse<User>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString());

    return this.http.get<any>(buildAdminUrl(ADMIN_ENDPOINTS.USERS), {
      headers: this.getHeaders(),
      params,
    }).pipe(
      map((response) => ({
        content: response.content || response.users || [],
        page: response.page || page,
        pageSize: response.pageSize || pageSize,
        totalElements: response.totalElements || 0,
        totalPages: response.totalPages || 0,
      })),
      catchError(() => of(this.getMockUsers()))
    );
  }

  getUserById(id: string): Observable<User | null> {
    return this.http.get<User>(buildAdminUrl(ADMIN_ENDPOINTS.USER_BY_ID(id)), {
      headers: this.getHeaders(),
    }).pipe(
      catchError(() => of(null))
    );
  }

  createUser(request: CreateUserRequest): Observable<User> {
    return this.http.post<User>(buildAdminUrl(ADMIN_ENDPOINTS.USERS), request, {
      headers: this.getHeaders(),
    });
  }

  updateUser(id: string, request: UpdateUserRequest): Observable<User> {
    return this.http.put<User>(buildAdminUrl(ADMIN_ENDPOINTS.USER_BY_ID(id)), request, {
      headers: this.getHeaders(),
    });
  }

  deleteUser(id: string): Observable<void> {
    return this.http.delete<void>(buildAdminUrl(ADMIN_ENDPOINTS.USER_BY_ID(id)), {
      headers: this.getHeaders(),
    });
  }

  updateUserRoles(id: string, roles: string[]): Observable<User> {
    return this.http.put<User>(buildAdminUrl(ADMIN_ENDPOINTS.USER_ROLES(id)), { roles }, {
      headers: this.getHeaders(),
    });
  }

  // =====================
  // Tenant Management
  // =====================

  getTenants(page = 0, pageSize = 20): Observable<PagedResponse<Tenant>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString());

    return this.http.get<any>(buildAdminUrl(ADMIN_ENDPOINTS.TENANTS), {
      headers: this.getHeaders(),
      params,
    }).pipe(
      map((response) => ({
        content: response.content || response.tenants || [],
        page: response.page || page,
        pageSize: response.pageSize || pageSize,
        totalElements: response.totalElements || 0,
        totalPages: response.totalPages || 0,
      })),
      catchError(() => of(this.getMockTenants()))
    );
  }

  getTenantById(id: string): Observable<Tenant | null> {
    return this.http.get<Tenant>(buildAdminUrl(ADMIN_ENDPOINTS.TENANT_BY_ID(id)), {
      headers: this.getHeaders(),
    }).pipe(
      catchError(() => of(null))
    );
  }

  createTenant(request: CreateTenantRequest): Observable<Tenant> {
    return this.http.post<Tenant>(buildAdminUrl(ADMIN_ENDPOINTS.TENANTS), request, {
      headers: this.getHeaders(),
    }).pipe(
      catchError(() => {
        // Return mock created tenant for demo
        const newTenant: Tenant = {
          id: 'TENANT' + Date.now(),
          name: request.name,
          displayName: request.name,
          contactEmail: request.contactEmail,
          domain: request.domain,
          status: 'ACTIVE',
          subscription: request.subscription,
          maxUsers: request.maxUsers,
          maxPatients: 100000,
          userCount: 0,
          patientCount: 0,
          featureFlags: request.featureFlags,
          features: {
            aiAssistant: request.featureFlags.aiAssistant,
            predictiveAnalytics: request.featureFlags.predictiveModels,
            customMeasures: false,
            apiAccess: true,
            ssoEnabled: false,
            auditLogs: true,
          },
          createdAt: new Date(),
          updatedAt: new Date(),
        };
        return of(newTenant);
      })
    );
  }

  updateTenant(id: string, tenant: Partial<Tenant>): Observable<Tenant> {
    return this.http.put<Tenant>(buildAdminUrl(ADMIN_ENDPOINTS.TENANT_BY_ID(id)), tenant, {
      headers: this.getHeaders(),
    }).pipe(
      catchError(() => of(tenant as Tenant))
    );
  }

  deleteTenant(id: string): Observable<void> {
    return this.http.delete<void>(buildAdminUrl(ADMIN_ENDPOINTS.TENANT_BY_ID(id)), {
      headers: this.getHeaders(),
    }).pipe(
      catchError(() => of(void 0))
    );
  }

  // =====================
  // Audit Logs
  // =====================

  getAuditLogs(filter?: AuditLogFilter): Observable<PagedResponse<AuditLog>> {
    let params = new HttpParams()
      .set('page', (filter?.page || 0).toString())
      .set('size', (filter?.pageSize || 50).toString());

    if (filter?.userId) params = params.set('userId', filter.userId);
    if (filter?.action) params = params.set('action', filter.action);
    if (filter?.resource) params = params.set('resource', filter.resource);
    if (filter?.startDate) params = params.set('startDate', filter.startDate.toISOString());
    if (filter?.endDate) params = params.set('endDate', filter.endDate.toISOString());

    return this.http.get<any>(buildAdminUrl(ADMIN_ENDPOINTS.AUDIT_LOGS), {
      headers: this.getHeaders(),
      params,
    }).pipe(
      map((response) => ({
        content: response.content || response.logs || [],
        page: response.page || filter?.page || 0,
        pageSize: response.pageSize || filter?.pageSize || 50,
        totalElements: response.totalElements || 0,
        totalPages: response.totalPages || 0,
      })),
      catchError(() => of(this.getMockAuditLogs()))
    );
  }

  // =====================
  // Mock Data (Fallback)
  // =====================

  private getMockDashboard(): DashboardSnapshot {
    return {
      metrics: {
        registeredPatients: 15420,
        patientReadLatencyP95Ms: 45,
        cacheHitRate: 0.94,
        activeUsers: 28,
        evaluationsToday: 342,
        careGapsOpen: 1847,
      },
      services: [
        { name: 'Gateway', status: 'UP', uptime: 99.9, lastCheck: new Date() },
        { name: 'FHIR Service', status: 'UP', uptime: 99.8, lastCheck: new Date() },
        { name: 'Quality Measure', status: 'UP', uptime: 99.7, lastCheck: new Date() },
        { name: 'Care Gap', status: 'UP', uptime: 99.9, lastCheck: new Date() },
        { name: 'Patient Service', status: 'UP', uptime: 99.8, lastCheck: new Date() },
      ],
      alerts: [],
      timestamp: new Date(),
    };
  }

  private getMockSystemHealthV2(): SystemHealth {
    return {
      services: [
        { name: 'Gateway Service', status: 'UP', uptime: 99.9, responseTimeMs: 12, instances: 3, endpoint: 'http://gateway:8001' },
        { name: 'FHIR Service', status: 'UP', uptime: 99.8, responseTimeMs: 25, instances: 2, endpoint: 'http://fhir:8085' },
        { name: 'Quality Measure Service', status: 'UP', uptime: 99.7, responseTimeMs: 45, instances: 2, endpoint: 'http://quality-measure:8087' },
        { name: 'CQL Engine Service', status: 'UP', uptime: 99.9, responseTimeMs: 120, instances: 2, endpoint: 'http://cql-engine:8081' },
        { name: 'Patient Service', status: 'UP', uptime: 99.8, responseTimeMs: 18, instances: 2, endpoint: 'http://patient:8084' },
        { name: 'Care Gap Service', status: 'UP', uptime: 99.7, responseTimeMs: 35, instances: 2, endpoint: 'http://care-gap:8086' },
        { name: 'Analytics Service', status: 'UP', uptime: 99.5, responseTimeMs: 85, instances: 1, endpoint: 'http://analytics:8090' },
        { name: 'Notification Service', status: 'UP', uptime: 99.9, responseTimeMs: 15, instances: 1, endpoint: 'http://notification:8092' },
      ],
      metrics: {
        cpuUsage: 42.5,
        memoryUsage: 68.2,
        diskUsage: 45.8,
        activeConnections: 156,
        requestsPerSecond: 245,
        avgResponseTimeMs: 38,
      },
      overallStatus: 'healthy',
      timestamp: new Date(),
    };
  }

  private getMockSystemHealth(): SystemHealthSnapshot {
    return {
      dependencies: [
        { name: 'PostgreSQL', type: 'DATABASE', status: 'UP', responseTimeMs: 12 },
        { name: 'Redis', type: 'CACHE', status: 'UP', responseTimeMs: 2 },
        { name: 'Kafka', type: 'MESSAGE_QUEUE', status: 'UP', responseTimeMs: 8 },
      ],
      queues: [
        { name: 'patient-events', depth: 0, lag: 0, processedPerSecond: 45 },
        { name: 'care-gap-events', depth: 3, lag: 0, processedPerSecond: 23 },
      ],
      overallStatus: 'HEALTHY',
      timestamp: new Date(),
    };
  }

  private getMockServiceCatalog(): ServiceDefinition[] {
    return [
      {
        id: 'gateway-service',
        name: 'API Gateway',
        description: 'Central API gateway for all services',
        version: '1.0.0',
        status: 'ACTIVE',
        uptime: 99.9,
        slo: { availability: 99.9, latencyP95Ms: 100, errorRatePercent: 0.1 },
        endpoints: ['/api/*'],
        contact: { team: 'Platform', email: 'platform@hdim.ai' },
      },
    ];
  }

  private getMockUsers(): PagedResponse<User> {
    return {
      content: [
        {
          id: '1',
          email: 'admin@hdim.ai',
          username: 'admin',
          firstName: 'System',
          lastName: 'Admin',
          role: 'ADMIN',
          roles: ['ADMIN', 'EVALUATOR'],
          tenantId: 'TENANT001',
          active: true,
          lastLogin: new Date(),
          createdAt: new Date('2024-01-01'),
          updatedAt: new Date(),
        },
        {
          id: '2',
          email: 'analyst@hdim.ai',
          username: 'analyst',
          firstName: 'Quality',
          lastName: 'Analyst',
          role: 'ANALYST',
          roles: ['ANALYST'],
          tenantId: 'TENANT001',
          active: true,
          lastLogin: new Date(),
          createdAt: new Date('2024-02-15'),
          updatedAt: new Date(),
        },
        {
          id: '3',
          email: 'evaluator@hdim.ai',
          username: 'evaluator',
          firstName: 'Care',
          lastName: 'Evaluator',
          role: 'EVALUATOR',
          roles: ['EVALUATOR'],
          tenantId: 'TENANT001',
          active: true,
          lastLogin: new Date(Date.now() - 86400000),
          createdAt: new Date('2024-03-10'),
          updatedAt: new Date(),
        },
        {
          id: '4',
          email: 'viewer@hdim.ai',
          username: 'viewer',
          firstName: 'Read',
          lastName: 'Only',
          role: 'VIEWER',
          roles: ['VIEWER'],
          tenantId: 'TENANT001',
          active: false,
          createdAt: new Date('2024-04-20'),
          updatedAt: new Date(),
        },
      ],
      page: 0,
      pageSize: 20,
      totalElements: 4,
      totalPages: 1,
    };
  }

  private getMockTenants(): PagedResponse<Tenant> {
    return {
      content: [
        {
          id: 'TENANT001',
          name: 'Demo Healthcare',
          displayName: 'Demo Healthcare Organization',
          contactEmail: 'admin@demohealthcare.org',
          domain: 'demohealthcare.org',
          status: 'ACTIVE',
          subscription: 'PROFESSIONAL',
          maxUsers: 50,
          maxPatients: 100000,
          userCount: 12,
          patientCount: 15420,
          featureFlags: {
            advancedAnalytics: true,
            aiAssistant: true,
            predictiveModels: true,
            customReports: true,
          },
          features: {
            aiAssistant: true,
            predictiveAnalytics: true,
            customMeasures: true,
            apiAccess: true,
            ssoEnabled: false,
            auditLogs: true,
          },
          createdAt: new Date('2024-01-01'),
          updatedAt: new Date(),
        },
        {
          id: 'TENANT002',
          name: 'Regional ACO',
          displayName: 'Regional Accountable Care Organization',
          contactEmail: 'it@regionalaco.com',
          domain: 'regionalaco.com',
          status: 'ACTIVE',
          subscription: 'ENTERPRISE',
          maxUsers: 200,
          maxPatients: 500000,
          userCount: 45,
          patientCount: 125000,
          featureFlags: {
            advancedAnalytics: true,
            aiAssistant: true,
            predictiveModels: true,
            customReports: true,
          },
          features: {
            aiAssistant: true,
            predictiveAnalytics: true,
            customMeasures: true,
            apiAccess: true,
            ssoEnabled: true,
            auditLogs: true,
          },
          createdAt: new Date('2024-02-15'),
          updatedAt: new Date(),
        },
        {
          id: 'TENANT003',
          name: 'Community Health',
          displayName: 'Community Health Center Network',
          contactEmail: 'admin@communityhealth.org',
          domain: 'communityhealth.org',
          status: 'ACTIVE',
          subscription: 'BASIC',
          maxUsers: 20,
          maxPatients: 50000,
          userCount: 8,
          patientCount: 32000,
          featureFlags: {
            advancedAnalytics: false,
            aiAssistant: false,
            predictiveModels: false,
            customReports: true,
          },
          features: {
            aiAssistant: false,
            predictiveAnalytics: false,
            customMeasures: false,
            apiAccess: true,
            ssoEnabled: false,
            auditLogs: true,
          },
          createdAt: new Date('2024-06-01'),
          updatedAt: new Date(),
        },
      ],
      page: 0,
      pageSize: 20,
      totalElements: 3,
      totalPages: 1,
    };
  }

  private getMockAuditLogs(): PagedResponse<AuditLog> {
    const now = new Date();
    return {
      content: [
        {
          id: '1',
          userId: 'admin',
          tenantId: 'TENANT001',
          action: 'LOGIN',
          resourceType: 'AUTH',
          resourceId: 'session-001',
          severity: 'INFO',
          details: 'User logged in successfully',
          ipAddress: '192.168.1.100',
          userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0',
          timestamp: now,
        },
        {
          id: '2',
          userId: 'analyst',
          tenantId: 'TENANT001',
          action: 'READ',
          resourceType: 'PATIENT',
          resourceId: 'patient-12345',
          severity: 'INFO',
          details: 'Viewed patient record',
          ipAddress: '192.168.1.101',
          userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) Safari/605.1',
          timestamp: new Date(now.getTime() - 3600000),
        },
        {
          id: '3',
          userId: 'evaluator',
          tenantId: 'TENANT001',
          action: 'CREATE',
          resourceType: 'EVALUATION',
          resourceId: 'eval-789',
          severity: 'INFO',
          details: 'Created new quality measure evaluation',
          ipAddress: '192.168.1.102',
          userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Firefox/121.0',
          timestamp: new Date(now.getTime() - 7200000),
        },
        {
          id: '4',
          userId: 'admin',
          tenantId: 'TENANT001',
          action: 'UPDATE',
          resourceType: 'USER',
          resourceId: 'user-456',
          severity: 'WARNING',
          details: 'Updated user permissions - elevated to EVALUATOR role',
          ipAddress: '192.168.1.100',
          userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0',
          timestamp: new Date(now.getTime() - 10800000),
        },
        {
          id: '5',
          userId: 'system',
          tenantId: 'TENANT001',
          action: 'DELETE',
          resourceType: 'CACHE',
          resourceId: 'cache-patient-data',
          severity: 'INFO',
          details: 'Automated cache cleanup completed',
          ipAddress: '127.0.0.1',
          userAgent: 'System/1.0',
          timestamp: new Date(now.getTime() - 14400000),
        },
        {
          id: '6',
          userId: 'analyst',
          tenantId: 'TENANT001',
          action: 'EXPORT',
          resourceType: 'REPORT',
          resourceId: 'report-hedis-2024',
          severity: 'INFO',
          details: 'Exported HEDIS quality report to CSV',
          ipAddress: '192.168.1.101',
          userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) Safari/605.1',
          timestamp: new Date(now.getTime() - 18000000),
        },
        {
          id: '7',
          userId: 'unknown',
          tenantId: 'TENANT001',
          action: 'LOGIN',
          resourceType: 'AUTH',
          resourceId: 'session-failed',
          severity: 'ERROR',
          details: 'Failed login attempt - invalid credentials',
          ipAddress: '10.0.0.55',
          userAgent: 'Mozilla/5.0 (Linux; Android 10) Mobile/15E148',
          timestamp: new Date(now.getTime() - 21600000),
        },
        {
          id: '8',
          userId: 'admin',
          tenantId: 'TENANT001',
          action: 'CREATE',
          resourceType: 'TENANT',
          resourceId: 'TENANT003',
          severity: 'WARNING',
          details: 'Created new tenant organization',
          ipAddress: '192.168.1.100',
          userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0',
          timestamp: new Date(now.getTime() - 86400000),
        },
      ],
      page: 0,
      pageSize: 50,
      totalElements: 8,
      totalPages: 1,
    };
  }
}
