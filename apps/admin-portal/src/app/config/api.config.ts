/**
 * API Configuration for Admin Portal
 */

export const API_CONFIG = {
  // Base URLs - Proxied through Angular dev server
  ADMIN_API_URL: '/api/admin',
  GATEWAY_URL: '/api',
  AUTH_URL: '/api/auth',

  // Tenant Configuration
  DEFAULT_TENANT_ID: 'TENANT001',

  // HTTP Settings
  TIMEOUT_MS: 30000,
  RETRY_ATTEMPTS: 3,
};

export const ADMIN_ENDPOINTS = {
  // Dashboard & Metrics
  DASHBOARD: '/dashboard',
  SYSTEM_HEALTH: '/system-health',
  SERVICE_CATALOG: '/service-catalog',
  API_PRESETS: '/api-presets',

  // User Management
  USERS: '/users',
  USER_BY_ID: (id: string) => `/users/${id}`,
  USER_ROLES: (id: string) => `/users/${id}/roles`,
  USER_RESET_PASSWORD: (id: string) => `/users/${id}/reset-password`,

  // Tenant Management
  TENANTS: '/tenants',
  TENANT_BY_ID: (id: string) => `/tenants/${id}`,
  TENANT_CONFIG: (id: string) => `/tenants/${id}/config`,

  // Audit Logs (Legacy)
  AUDIT_LOGS: '/audit-logs',
  AUDIT_LOG_BY_ID: (id: string) => `/audit-logs/${id}`,

  // Audit Query Service (New - Port 8093)
  AUDIT_SEARCH: '/v1/audit/logs/search',
  AUDIT_EVENT_BY_ID: (id: string) => `/v1/audit/logs/${id}`,
  AUDIT_STATISTICS: '/v1/audit/logs/statistics',
  AUDIT_EXPORT: '/v1/audit/logs/export',

  // System Configuration
  CONFIG: '/config',
  ALERTS: '/alerts',
};

export const GATEWAY_ENDPOINTS = {
  ADMIN_SERVICES_HEALTH: '/v1/admin/services/health',
  CONFIG_VERSIONS: (serviceName: string, tenantId: string) =>
    `/v1/configs/${serviceName}/tenants/${tenantId}/versions`,
  CONFIG_VERSION: (serviceName: string, tenantId: string, versionId: string) =>
    `/v1/configs/${serviceName}/tenants/${tenantId}/versions/${versionId}`,
  CONFIG_CURRENT: (serviceName: string, tenantId: string) =>
    `/v1/configs/${serviceName}/tenants/${tenantId}/current`,
  CONFIG_PROMOTE: (serviceName: string, tenantId: string) =>
    `/v1/configs/${serviceName}/tenants/${tenantId}/promote`,
  CONFIG_ACTIVATE: (serviceName: string, tenantId: string, versionId: string) =>
    `/v1/configs/${serviceName}/tenants/${tenantId}/activate/${versionId}`,
  CONFIG_AUDIT: (serviceName: string, tenantId: string) =>
    `/v1/configs/${serviceName}/tenants/${tenantId}/audit`,
  CONFIG_APPROVALS: (serviceName: string, tenantId: string, versionId: string) =>
    `/v1/configs/${serviceName}/tenants/${tenantId}/versions/${versionId}/approvals`,
  CONFIG_APPROVAL_REQUEST: (serviceName: string, tenantId: string, versionId: string) =>
    `/v1/configs/${serviceName}/tenants/${tenantId}/versions/${versionId}/approvals/request`,
  CONFIG_APPROVAL_APPROVE: (serviceName: string, tenantId: string, versionId: string) =>
    `/v1/configs/${serviceName}/tenants/${tenantId}/versions/${versionId}/approvals/approve`,
  CONFIG_APPROVAL_REJECT: (serviceName: string, tenantId: string, versionId: string) =>
    `/v1/configs/${serviceName}/tenants/${tenantId}/versions/${versionId}/approvals/reject`,
};

export const HTTP_HEADERS = {
  TENANT_ID: 'X-Tenant-ID',
  CONTENT_TYPE: 'Content-Type',
  AUTHORIZATION: 'Authorization',
};

export function buildAdminUrl(endpoint: string): string {
  return `${API_CONFIG.ADMIN_API_URL}${endpoint}`;
}

export function buildGatewayUrl(endpoint: string): string {
  return `${API_CONFIG.GATEWAY_URL}${endpoint}`;
}
