/**
 * Development Environment Configuration
 *
 * This file is used during local development.
 */

export const environment = {
  production: false,

  // API Configuration
  apiConfig: {
    // Don't use API Gateway in development (connect directly to services)
    useApiGateway: false,

    // Local development URLs
    apiGatewayUrl: 'http://localhost:8080',
    cqlEngineUrl: 'http://localhost:8081/cql-engine',
    qualityMeasureUrl: 'http://localhost:8087/quality-measure',
    fhirServerUrl: 'http://localhost:8085/fhir',

    // Tenant configuration - must match backend test data
    defaultTenantId: 'DEMO001',  // Demo tenant UUID from demo-users.sql

    // HTTP settings
    timeoutMs: 30000,
    retryAttempts: 3,
    retryDelayMs: 1000,
  },

  // Feature flags
  features: {
    enableAnalytics: false,
    enableErrorReporting: false,
    enablePerformanceMonitoring: false,
    enableDebugMode: true,
  },

  // Analytics (disabled in dev)
  analytics: {
    googleAnalyticsId: '',
  },

  // Error reporting (disabled in dev)
  errorReporting: {
    dsn: '',
    environment: 'development',
  },
};
