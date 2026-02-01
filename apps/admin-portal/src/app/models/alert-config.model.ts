/**
 * Alert Configuration Models
 *
 * Models for configuring and managing alerting rules in the HDIM Admin Portal.
 * Alerts can be configured per service with custom thresholds and notification preferences.
 */

/**
 * Alert Configuration
 * Defines an alerting rule for a specific service
 */
export interface AlertConfig {
  id: string;
  serviceName: string;
  displayName: string;
  alertType: AlertType;
  threshold: number;
  durationMinutes: number;
  severity: AlertSeverity;
  enabled: boolean;
  notificationChannels: NotificationChannel[];
  createdAt: Date;
  updatedAt: Date;
  createdBy: string;
  lastTriggered?: Date;
}

/**
 * Alert Type
 * Types of metrics that can trigger alerts
 */
export type AlertType = 'CPU_USAGE' | 'MEMORY_USAGE' | 'ERROR_RATE' | 'LATENCY' | 'REQUEST_RATE';

/**
 * Alert Severity
 * Severity levels for alerts
 */
export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL';

/**
 * Notification Channel
 * Where to send alert notifications
 */
export type NotificationChannel = 'EMAIL' | 'SLACK' | 'WEBHOOK' | 'SMS';

/**
 * Create Alert Config Request
 * DTO for creating a new alert configuration
 */
export interface CreateAlertConfigRequest {
  serviceName: string;
  displayName: string;
  alertType: AlertType;
  threshold: number;
  durationMinutes: number;
  severity: AlertSeverity;
  enabled: boolean;
  notificationChannels: NotificationChannel[];
}

/**
 * Update Alert Config Request
 * DTO for updating an existing alert configuration
 */
export interface UpdateAlertConfigRequest {
  threshold?: number;
  durationMinutes?: number;
  severity?: AlertSeverity;
  enabled?: boolean;
  notificationChannels?: NotificationChannel[];
}

/**
 * Alert Event
 * Represents a triggered alert
 */
export interface AlertEvent {
  id: string;
  alertConfigId: string;
  serviceName: string;
  alertType: AlertType;
  severity: AlertSeverity;
  currentValue: number;
  threshold: number;
  message: string;
  triggeredAt: Date;
  resolvedAt?: Date;
  acknowledged: boolean;
  acknowledgedBy?: string;
  acknowledgedAt?: Date;
}

/**
 * Alert Threshold Presets
 * Recommended threshold values for different alert types
 */
export const ALERT_THRESHOLD_PRESETS: Record<AlertType, { recommended: number; unit: string; description: string }> = {
  CPU_USAGE: {
    recommended: 80,
    unit: '%',
    description: 'CPU usage exceeds this percentage',
  },
  MEMORY_USAGE: {
    recommended: 850,
    unit: 'MB',
    description: 'Memory usage exceeds this amount',
  },
  ERROR_RATE: {
    recommended: 5,
    unit: 'errors/s',
    description: 'Error rate exceeds this value',
  },
  LATENCY: {
    recommended: 500,
    unit: 'ms',
    description: 'P95 latency exceeds this value',
  },
  REQUEST_RATE: {
    recommended: 1000,
    unit: 'req/s',
    description: 'Request rate exceeds this value',
  },
};

/**
 * Alert Type Labels
 * Human-readable labels for alert types
 */
export const ALERT_TYPE_LABELS: Record<AlertType, string> = {
  CPU_USAGE: 'CPU Usage',
  MEMORY_USAGE: 'Memory Usage',
  ERROR_RATE: 'Error Rate',
  LATENCY: 'P95 Latency',
  REQUEST_RATE: 'Request Rate',
};

/**
 * Severity Labels
 * Human-readable labels for severity levels
 */
export const SEVERITY_LABELS: Record<AlertSeverity, string> = {
  INFO: 'Info',
  WARNING: 'Warning',
  CRITICAL: 'Critical',
};

/**
 * Notification Channel Labels
 * Human-readable labels for notification channels
 */
export const NOTIFICATION_CHANNEL_LABELS: Record<NotificationChannel, string> = {
  EMAIL: 'Email',
  SLACK: 'Slack',
  WEBHOOK: 'Webhook',
  SMS: 'SMS',
};
