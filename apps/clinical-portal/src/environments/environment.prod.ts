/**
 * Production Environment Configuration
 *
 * This file is used when building for production deployment.
 * Update these values based on your production backend URLs.
 */

export const environment = {
  production: true,

  // API Configuration
  apiConfig: {
    // Use API Gateway in production
    useApiGateway: true,

    // Production API Gateway URL (Kong or similar)
    // Replace with your actual production gateway URL
    apiGatewayUrl: process.env['API_GATEWAY_URL'] || 'https://api.healthdata-in-motion.com',

    // Direct service URLs (fallback if gateway not used)
    cqlEngineUrl: process.env['CQL_ENGINE_URL'] || 'https://cql-api.healthdata-in-motion.com',
    qualityMeasureUrl: process.env['QUALITY_MEASURE_URL'] || 'https://quality-api.healthdata-in-motion.com',
    fhirServerUrl: process.env['FHIR_SERVER_URL'] || 'https://fhir-api.healthdata-in-motion.com',

    // Tenant configuration
    defaultTenantId: process.env['DEFAULT_TENANT_ID'] || 'default',

    // HTTP settings
    timeoutMs: 30000,
    retryAttempts: 3,
    retryDelayMs: 1000,
  },

  // Feature flags
  features: {
    enableAnalytics: true,
    enableErrorReporting: true,
    enablePerformanceMonitoring: true,
    enableDebugMode: false,
  },

  // Analytics
  analytics: {
    googleAnalyticsId: process.env['GA_MEASUREMENT_ID'] || '',
  },

  // Error reporting (e.g., Sentry)
  errorReporting: {
    dsn: process.env['SENTRY_DSN'] || '',
    environment: 'production',
  },

  // Compliance Configuration
  compliance: {
    disableFallbacks: true, // Disable fallbacks in production for compliance validation
    strictErrorHandling: true,
    enableErrorTracking: true,
    allowedFallbackServices: [],
    // Error retention policy (in days)
    errorRetentionDays: 90, // Keep errors for 90 days in production
    // Critical error thresholds
    criticalErrorThreshold: 10, // Alert if more than 10 critical errors
    errorRateThreshold: 100, // Alert if more than 100 errors per hour
    // Backend sync
    syncToBackend: true, // Send errors to backend service
    syncIntervalMs: 60000, // Sync every 60 seconds
  },

  // Demo monitoring configuration (Prometheus/Grafana)
  monitoring: {
    prometheusUrl: process.env['PROMETHEUS_URL'] || '/monitoring/prometheus',
    grafanaUrl: process.env['GRAFANA_URL'] || '/monitoring/grafana',
    refreshMs: 5000,
  },
};
