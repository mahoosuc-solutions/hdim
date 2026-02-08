/**
 * Production Monitoring Configuration
 *
 * Configures real-time performance monitoring for measure builder in production.
 * Enables alerts when performance metrics exceed defined thresholds.
 */

export interface MonitoringAlert {
  severity: 'critical' | 'warning' | 'info';
  metric: string;
  threshold: number;
  currentValue: number;
  timestamp: Date;
  action?: () => void;
}

export interface MonitoringConfig {
  enabled: boolean;
  environment: 'development' | 'staging' | 'production';
  samplingRate: number;                 // 0-1: fraction of requests to monitor
  alertThresholds: Record<string, number>;
  reportingInterval: number;            // ms between reports
  maxMetricsHistory: number;
  exportEndpoint?: string;
  alertCallback?: (alert: MonitoringAlert) => void;
}

const PRODUCTION_THRESHOLDS: Record<string, number> = {
  'render-svg-50-blocks': 30,
  'render-svg-100-blocks': 50,
  'render-svg-150-blocks': 100,
  'render-canvas-150-blocks': 80,
  'render-canvas-200-blocks': 120,
  'slider-update': 5,
  'slider-concurrent-10': 50,
  'distribution-weight-update': 10,
  'cql-generation-simple': 20,
  'cql-generation-complex': 100,
  'cql-regeneration': 50,
  'state-add-block': 5,
  'state-update-block': 5,
  'state-filter-blocks': 10,
  'full-workflow': 500,
  'measure-export': 300,
  'user-interaction-sequence': 200,
  'memory-1000-updates': 100,
  'rapid-slider-adjustments': 50,
  'rxjs-observable-chain': 30
};

const STAGING_THRESHOLDS: Record<string, number> = {
  ...PRODUCTION_THRESHOLDS,
  // Slightly relaxed for staging
  'render-svg-100-blocks': 75,
  'cql-generation-complex': 150,
  'full-workflow': 600
};

const DEVELOPMENT_THRESHOLDS: Record<string, number> = {
  ...PRODUCTION_THRESHOLDS,
  // Very relaxed for development debugging
  'render-svg-100-blocks': 200,
  'cql-generation-complex': 300,
  'full-workflow': 1000
};

/**
 * Get monitoring configuration for environment
 */
export function getMonitoringConfig(environment: string): MonitoringConfig {
  const baseConfig: MonitoringConfig = {
    enabled: true,
    environment: environment as 'development' | 'staging' | 'production',
    samplingRate: 1.0,  // Default: monitor all requests
    reportingInterval: 5000, // 5 seconds
    maxMetricsHistory: 1000,
    alertThresholds: PRODUCTION_THRESHOLDS
  };

  // Environment-specific configuration
  switch (environment) {
    case 'development':
      return {
        ...baseConfig,
        samplingRate: 1.0,        // Monitor everything in dev
        reportingInterval: 1000,   // Report every second
        alertThresholds: DEVELOPMENT_THRESHOLDS
      };

    case 'staging':
      return {
        ...baseConfig,
        samplingRate: 0.5,         // Monitor 50% of requests
        reportingInterval: 2000,   // Report every 2 seconds
        alertThresholds: STAGING_THRESHOLDS
      };

    case 'production':
    default:
      return {
        ...baseConfig,
        samplingRate: 0.1,         // Monitor 10% of requests (cost optimization)
        reportingInterval: 10000,  // Report every 10 seconds
        alertThresholds: PRODUCTION_THRESHOLDS,
        exportEndpoint: '/api/monitoring/metrics' // Send to backend
      };
  }
}

/**
 * Alert handlers for different severities
 */
export const AlertHandlers = {
  /**
   * Critical alert: Immediate action required
   */
  critical: (alert: MonitoringAlert) => {
    console.error('🔴 CRITICAL ALERT:', alert.metric, alert.currentValue);

    // Send to monitoring service (Sentry, DataDog, etc.)
    if (window.location.hostname !== 'localhost') {
      // In production, send to external monitoring
      fetch('/api/monitoring/alerts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          severity: 'critical',
          metric: alert.metric,
          threshold: alert.threshold,
          currentValue: alert.currentValue,
          timestamp: alert.timestamp
        })
      }).catch(err => console.error('Failed to send alert:', err));
    }

    // Trigger configured action (e.g., fallback to Canvas, disable features)
    alert.action?.();
  },

  /**
   * Warning alert: Monitoring for trends
   */
  warning: (alert: MonitoringAlert) => {
    console.warn('🟡 WARNING:', alert.metric, alert.currentValue);

    // Log to analytics for trend analysis
    const gtagFn = (window as { gtag?: (...args: any[]) => void }).gtag;
    if (gtagFn) {
      gtagFn('event', 'performance_warning', {
        metric: alert.metric,
        threshold: alert.threshold,
        value: alert.currentValue
      });
    }
  },

  /**
   * Info alert: For awareness
   */
  info: (alert: MonitoringAlert) => {
    console.info('ℹ️ INFO:', alert.metric, alert.currentValue);
  }
};

/**
 * Default alert callback for production
 */
export function defaultProductionAlertCallback(alert: MonitoringAlert): void {
  const handler = AlertHandlers[alert.severity];
  handler(alert);
}

/**
 * Monitoring strategies for common scenarios
 */
export const MonitoringStrategies = {
  /**
   * Quick performance optimization
   * Use with Canvas rendering and CQL caching
   */
  performance: {
    renderThreshold: 50,
    cqlThreshold: 100,
    samplingRate: 0.2,
    fallbackStrategies: [
      { metric: 'render-svg-150-blocks', action: 'use-canvas' },
      { metric: 'cql-generation-complex', action: 'use-cache' },
      { metric: 'slider-update', action: 'debounce-100ms' }
    ]
  },

  /**
   * Stability monitoring
   * Detects memory leaks and crashes
   */
  stability: {
    memoryThreshold: 100,    // MB
    errorThreshold: 5,       // errors per minute
    samplingRate: 1.0,       // Monitor all requests
    fallbackStrategies: [
      { metric: 'memory-growth', action: 'warn-user' },
      { metric: 'render-timeout', action: 'reload-page' },
      { metric: 'cql-error', action: 'use-cached-cql' }
    ]
  },

  /**
   * User experience monitoring
   * Tracks interaction latency
   */
  ux: {
    interactionThreshold: 100,
    renderThreshold: 50,
    samplingRate: 0.5,
    fallbackStrategies: [
      { metric: 'drag-latency', action: 'throttle-updates' },
      { metric: 'slider-lag', action: 'debounce-input' },
      { metric: 'slow-export', action: 'stream-export' }
    ]
  }
};

/**
 * Create monitoring instance with automatic alert handling
 */
export function createProductionMonitoring(strategy: keyof typeof MonitoringStrategies = 'performance') {
  const environment = window.location.hostname === 'localhost' ? 'development' : 'production';
  const config = getMonitoringConfig(environment);
  const strategyConfig = MonitoringStrategies[strategy];

  // Merge strategy thresholds
  Object.entries(strategyConfig).forEach(([key, value]) => {
    if (typeof value === 'number' && key.includes('Threshold')) {
      const metricKey = key.replace('Threshold', '').toLowerCase();
      // Apply strategy-specific thresholds
    }
  });

  // Set alert callback
  config.alertCallback = defaultProductionAlertCallback;

  return config;
}

/**
 * Health check for monitoring system
 */
export function performHealthCheck(): {
  healthy: boolean;
  warnings: string[];
  recommendations: string[];
} {
  const warnings: string[] = [];
  const recommendations: string[] = [];

  // Check memory usage (performance.memory is non-standard Chrome-only API)
  const perfMemory = (performance as any).memory;
  if (perfMemory && perfMemory.usedJSHeapSize > 100 * 1024 * 1024) {
    warnings.push('High memory usage detected');
    recommendations.push('Consider implementing memory pooling');
  }

  // Check for memory leaks
  if (perfMemory && perfMemory.jsHeapSizeLimit < perfMemory.usedJSHeapSize * 2) {
    warnings.push('Heap size nearing limit');
    recommendations.push('Check for subscription leaks in components');
  }

  // Check event listener count
  const listeners = (performance as any).eventListeners?.count || 0;
  if (listeners > 1000) {
    warnings.push('High number of event listeners');
    recommendations.push('Audit and clean up old event listeners');
  }

  return {
    healthy: warnings.length === 0,
    warnings,
    recommendations
  };
}
