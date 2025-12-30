/**
 * Admin Portal Models
 */

// User Management
export interface User {
  id: string;
  email: string;
  username: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  roles: UserRole[];
  tenantId: string;
  active: boolean;
  lastLogin?: Date;
  createdAt: Date;
  updatedAt: Date;
}

export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'EVALUATOR' | 'ANALYST' | 'VIEWER';

export interface CreateUserRequest {
  email: string;
  username: string;
  firstName: string;
  lastName: string;
  password: string;
  role: UserRole;
  tenantId: string;
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  role?: UserRole;
  active?: boolean;
}

// Tenant Management
export interface Tenant {
  id: string;
  name: string;
  displayName: string;
  contactEmail: string;
  domain?: string;
  status: TenantStatus;
  subscription: SubscriptionTier;
  maxUsers: number;
  maxPatients: number;
  userCount: number;
  patientCount: number;
  featureFlags: TenantFeatureFlags;
  features: TenantFeatures;
  createdAt: Date;
  updatedAt: Date;
}

export type TenantStatus = 'ACTIVE' | 'SUSPENDED' | 'PENDING' | 'INACTIVE';

export type SubscriptionTier = 'FREE' | 'BASIC' | 'PROFESSIONAL' | 'ENTERPRISE' | 'TRIAL' | 'STARTER';

export interface TenantFeatures {
  aiAssistant: boolean;
  predictiveAnalytics: boolean;
  customMeasures: boolean;
  apiAccess: boolean;
  ssoEnabled: boolean;
  auditLogs: boolean;
}

export interface TenantFeatureFlags {
  advancedAnalytics: boolean;
  aiAssistant: boolean;
  predictiveModels: boolean;
  customReports: boolean;
}

export interface CreateTenantRequest {
  name: string;
  contactEmail: string;
  domain?: string;
  subscription: SubscriptionTier;
  maxUsers: number;
  featureFlags: TenantFeatureFlags;
}

// Dashboard & Metrics
export interface DashboardSnapshot {
  metrics: DashboardMetrics;
  services: ServiceStatus[];
  alerts: SystemAlert[];
  timestamp: Date;
}

export interface DashboardMetrics {
  registeredPatients: number;
  patientReadLatencyP95Ms: number;
  cacheHitRate: number;
  activeUsers: number;
  evaluationsToday: number;
  careGapsOpen: number;
}

export interface ServiceStatus {
  name: string;
  status: 'UP' | 'DOWN' | 'DEGRADED';
  uptime: number;
  lastCheck: Date;
  latencyMs?: number;
}

export interface SystemAlert {
  id: string;
  severity: 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL';
  message: string;
  source: string;
  timestamp: Date;
  acknowledged: boolean;
}

// System Health
export interface SystemHealth {
  services: ServiceHealth[];
  metrics: SystemMetrics;
  overallStatus: 'healthy' | 'degraded' | 'unhealthy';
  timestamp: Date;
}

export interface ServiceHealth {
  name: string;
  status: 'UP' | 'DOWN' | 'DEGRADED';
  uptime: number;
  responseTimeMs: number;
  instances?: number;
  endpoint?: string;
  lastError?: string;
}

export interface SystemMetrics {
  cpuUsage: number;
  memoryUsage: number;
  diskUsage: number;
  activeConnections: number;
  requestsPerSecond: number;
  avgResponseTimeMs: number;
}

export interface SystemHealthSnapshot {
  dependencies: DependencyHealth[];
  queues: QueueMetrics[];
  overallStatus: 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY';
  timestamp: Date;
}

export interface DependencyHealth {
  name: string;
  type: 'DATABASE' | 'CACHE' | 'MESSAGE_QUEUE' | 'EXTERNAL_SERVICE';
  status: 'UP' | 'DOWN' | 'DEGRADED';
  responseTimeMs: number;
  details?: Record<string, unknown>;
}

export interface QueueMetrics {
  name: string;
  depth: number;
  lag: number;
  processedPerSecond: number;
}

// Audit Logs
export interface AuditLog {
  id: string;
  userId: string;
  tenantId: string;
  action: string;
  resourceType: string;
  resourceId: string;
  severity: 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL';
  details: string;
  ipAddress: string;
  userAgent: string;
  timestamp: Date;
  metadata?: Record<string, unknown>;
}

export interface AuditLogEntry {
  id: string;
  userId: string;
  username: string;
  action: string;
  resource: string;
  resourceId?: string;
  details?: Record<string, unknown>;
  ipAddress: string;
  userAgent: string;
  timestamp: Date;
  tenantId: string;
}

export interface AuditLogFilter {
  userId?: string;
  action?: string;
  resource?: string;
  startDate?: Date;
  endDate?: Date;
  page?: number;
  pageSize?: number;
}

// Service Catalog
export interface ServiceDefinition {
  id: string;
  name: string;
  description: string;
  version: string;
  status: 'ACTIVE' | 'MAINTENANCE' | 'DEPRECATED';
  uptime: number;
  slo: ServiceSLO;
  endpoints: string[];
  contact: ServiceContact;
}

export interface ServiceSLO {
  availability: number;
  latencyP95Ms: number;
  errorRatePercent: number;
}

export interface ServiceContact {
  team: string;
  email: string;
  slack?: string;
}

// API Presets
export interface ApiPreset {
  id: string;
  name: string;
  description: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  url: string;
  headers?: Record<string, string>;
  queryParams?: Record<string, string>;
  body?: unknown;
}

// Pagination
export interface PagedResponse<T> {
  content: T[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
}
