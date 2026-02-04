/**
 * Development Environment Configuration - Admin Portal
 *
 * This file is used during local development.
 */

export const environment = {
  production: false,

  // API Configuration
  apiConfig: {
    adminApiUrl: '/api/admin',
    gatewayUrl: '/api',
    authUrl: '/api/auth',
    defaultTenantId: 'TENANT001',
    timeoutMs: 30000,
    retryAttempts: 3,
    // Investor Dashboard Service API
    investorApiUrl: 'http://localhost:8120/investor',
    // Sales Automation Service API
    salesApiUrl: 'http://localhost:8106/sales-automation',
  },

  // Logging Configuration (HIPAA Compliance)
  logging: {
    level: 'DEBUG', // DEBUG, INFO, WARN, ERROR
    enableConsole: true, // Enable console in development
    enableRemote: false, // Disable remote logging in development
    remoteEndpoint: '',
    phiFiltering: false, // PHI filtering OFF in development for debugging
  },

  // Feature flags
  features: {
    enableAnalytics: false,
    enableErrorReporting: false,
    enableDebugMode: true,
  },

  // Error reporting (disabled in dev)
  errorReporting: {
    dsn: '',
    environment: 'development',
  },
};
