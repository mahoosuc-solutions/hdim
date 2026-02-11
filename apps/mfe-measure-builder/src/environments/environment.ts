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

  // Compliance Configuration
  compliance: {
    disableFallbacks: false, // Set to true for compliance validation
    strictErrorHandling: false,
    enableErrorTracking: true,
    allowedFallbackServices: [],
    // Error retention policy (in days)
    errorRetentionDays: 7, // Keep errors for 7 days in development
    // Critical error thresholds
    criticalErrorThreshold: 5, // Alert if more than 5 critical errors
    errorRateThreshold: 50, // Alert if more than 50 errors per hour
    // Backend sync
    syncToBackend: true, // Send errors to backend service
    syncIntervalMs: 30000, // Sync every 30 seconds in dev
  },

  // Demo monitoring configuration (Prometheus/Grafana)
  monitoring: {
    prometheusUrl: '/monitoring/prometheus',
    grafanaUrl: '/monitoring/grafana',
    refreshMs: 5000,
  },
};
