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
  version?: string;
  lastDeploymentAt?: Date;
  logsUrl?: string;
  metricsUrl?: string;
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

// Audit Logs - Enhanced for audit-query-service integration
export enum AuditAction {
  CREATE = 'CREATE',
  READ = 'READ',
  UPDATE = 'UPDATE',
  DELETE = 'DELETE',
  LOGIN = 'LOGIN',
  LOGOUT = 'LOGOUT',
  EXPORT = 'EXPORT',
  SEARCH = 'SEARCH',
  EXECUTE = 'EXECUTE'
}

export enum AuditOutcome {
  SUCCESS = 'SUCCESS',
  FAILURE = 'FAILURE',
  PARTIAL = 'PARTIAL'
}

export interface AuditEvent {
  id: string;
  timestamp: string;
  tenantId: string;
  userId: string;
  username: string;
  role: string;
  ipAddress: string;
  userAgent: string;
  action: AuditAction;
  resourceType: string;
  resourceId?: string;
  outcome: AuditOutcome;
  serviceName: string;
  requestPayload?: Record<string, unknown>;
  responsePayload?: Record<string, unknown>;
  errorMessage?: string;
  durationMs?: number;
}

export interface AuditSearchRequest {
  userId?: string;
  username?: string;
  role?: string;
  resourceType?: string;
  resourceId?: string;
  actions?: AuditAction[];
  outcomes?: AuditOutcome[];
  serviceName?: string;
  startTime?: string;
  endTime?: string;
  searchText?: string;
  page: number;
  size: number;
  sortBy: string;
  sortDirection: 'ASC' | 'DESC';
}

export interface AuditSearchResponse {
  content: AuditEvent[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface AuditStatistics {
  totalEvents: number;
  actionDistribution: { [action: string]: number };
  outcomeDistribution: { [outcome: string]: number };
  topUsers: Array<{ username: string; count: number }>;
  topResources: Array<{ resourceType: string; count: number }>;
  timeRange: {
    startTime: string;
    endTime: string;
  };
}

// Legacy interfaces (kept for backwards compatibility)
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

// Config Versioning
export type ConfigStatus =
  | 'DRAFT'
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'REJECTED'
  | 'ACTIVE'
  | 'SUPERSEDED';

export interface ConfigVersion {
  id: string;
  tenantId: string;
  serviceName: string;
  versionNumber: number;
  status: ConfigStatus;
  config: Record<string, unknown> | null;
  configHash: string;
  changeSummary?: string;
  sourceVersionId?: string;
  createdBy: string;
  createdAt?: Date;
  updatedAt?: Date;
}

export interface ConfigAuditEntry {
  id: string;
  tenantId: string;
  serviceName: string;
  versionId: string;
  action: string;
  actor: string;
  details?: Record<string, unknown> | null;
  createdAt?: Date;
}

export type ConfigApprovalAction = 'REQUESTED' | 'APPROVED' | 'REJECTED';

export interface ConfigApproval {
  id: string;
  tenantId: string;
  serviceName: string;
  versionId: string;
  action: ConfigApprovalAction;
  actor: string;
  comment?: string | null;
  createdAt?: Date;
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
