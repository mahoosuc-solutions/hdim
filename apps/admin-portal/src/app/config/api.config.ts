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

  // Tenant Management
  TENANTS: '/tenants',
  TENANT_BY_ID: (id: string) => `/tenants/${id}`,
  TENANT_CONFIG: (id: string) => `/tenants/${id}/config`,

  // Audit Logs
  AUDIT_LOGS: '/audit-logs',
  AUDIT_LOG_BY_ID: (id: string) => `/audit-logs/${id}`,

  // System Configuration
  CONFIG: '/config',
  ALERTS: '/alerts',
};

export const HTTP_HEADERS = {
  TENANT_ID: 'X-Tenant-ID',
  CONTENT_TYPE: 'Content-Type',
  AUTHORIZATION: 'Authorization',
};

export function buildAdminUrl(endpoint: string): string {
  return `${API_CONFIG.ADMIN_API_URL}${endpoint}`;
}
