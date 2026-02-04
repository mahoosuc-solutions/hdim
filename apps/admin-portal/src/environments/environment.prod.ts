/**
 * Production Environment Configuration - Admin Portal
 *
 * This file is used when building for production deployment.
 * HIPAA COMPLIANCE: All console output disabled, PHI filtering enabled.
 */

export const environment = {
  production: true,

  // API Configuration
  apiConfig: {
    adminApiUrl: process.env['ADMIN_API_URL'] || '/api/admin',
    gatewayUrl: process.env['GATEWAY_URL'] || '/api',
    authUrl: process.env['AUTH_URL'] || '/api/auth',
    defaultTenantId: process.env['DEFAULT_TENANT_ID'] || 'default',
    timeoutMs: 30000,
    retryAttempts: 3,
    // Investor Dashboard Service API (via gateway in production)
    investorApiUrl: process.env['INVESTOR_API_URL'] || '/investor',
    // Sales Automation Service API (via gateway in production)
    salesApiUrl: process.env['SALES_API_URL'] || '/sales',
  },

  // Logging Configuration (HIPAA Compliance)
  logging: {
    level: 'WARN', // Only WARN and ERROR in production
    enableConsole: false, // ✅ DISABLE console in production (HIPAA §164.312(b))
    enableRemote: true, // Enable remote log aggregation
    remoteEndpoint: process.env['LOGGING_ENDPOINT'] || '',
    phiFiltering: true, // ✅ ENABLE PHI filtering (HIPAA §164.312(b))
  },

  // Feature flags
  features: {
    enableAnalytics: true,
    enableErrorReporting: true,
    enableDebugMode: false,
    demoMode: false, // SECURITY: Demo mode disabled in production - requires real authentication
  },

  // Error reporting (e.g., Sentry)
  errorReporting: {
    dsn: process.env['SENTRY_DSN'] || '',
    environment: 'production',
  },
};
