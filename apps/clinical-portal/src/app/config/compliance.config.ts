/**
 * Compliance Configuration
 * 
 * Controls error handling and fallback behavior for compliance validation.
 * When enabled, all fallbacks are disabled and errors must be properly raised.
 */

export interface ComplianceConfig {
  /**
   * Master switch to disable all fallbacks across the application
   * When true, services must throw errors instead of returning mock/fallback data
   */
  disableFallbacks: boolean;

  /**
   * Require all errors to be logged and thrown (no silent failures)
   */
  strictErrorHandling: boolean;

  /**
   * Track errors for compliance reports and validation
   */
  enableErrorTracking: boolean;

  /**
   * Whitelist of services allowed to use fallbacks even when disableFallbacks is true
   * Useful for gradual migration or non-critical services
   */
  allowedFallbackServices: string[];

  /**
   * Error retention policy (in days)
   * Errors older than this will be automatically purged
   */
  errorRetentionDays?: number;

  /**
   * Critical error threshold - alert if exceeded
   */
  criticalErrorThreshold?: number;

  /**
   * Error rate threshold (errors per hour) - alert if exceeded
   */
  errorRateThreshold?: number;

  /**
   * Whether to sync errors to backend service
   */
  syncToBackend?: boolean;

  /**
   * Interval for syncing errors to backend (in milliseconds)
   */
  syncIntervalMs?: number;
}

import { environment } from '../../environments/environment';

/**
 * Default compliance configuration
 * Loaded from environment configuration
 */
export const COMPLIANCE_CONFIG: ComplianceConfig = {
  disableFallbacks: (environment as any).compliance?.disableFallbacks ?? false,
  strictErrorHandling: (environment as any).compliance?.strictErrorHandling ?? false,
  enableErrorTracking: (environment as any).compliance?.enableErrorTracking ?? true,
  allowedFallbackServices: (environment as any).compliance?.allowedFallbackServices ?? [],
  errorRetentionDays: (environment as any).compliance?.errorRetentionDays ?? 30,
  criticalErrorThreshold: (environment as any).compliance?.criticalErrorThreshold ?? 10,
  errorRateThreshold: (environment as any).compliance?.errorRateThreshold ?? 100,
  syncToBackend: (environment as any).compliance?.syncToBackend ?? false,
  syncIntervalMs: (environment as any).compliance?.syncIntervalMs ?? 60000,
};

/**
 * Get compliance configuration from environment
 */
export function getComplianceConfig(): ComplianceConfig {
  return COMPLIANCE_CONFIG;
}

/**
 * Check if fallbacks are allowed for a specific service
 */
export function isFallbackAllowed(serviceName: string): boolean {
  const config = getComplianceConfig();
  
  // If fallbacks are not disabled, always allow
  if (!config.disableFallbacks) {
    return true;
  }

  // Check if service is in whitelist
  return config.allowedFallbackServices.includes(serviceName);
}
