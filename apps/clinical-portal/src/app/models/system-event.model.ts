/**
 * System Event Model
 *
 * Defines all system events that flow through the platform,
 * enabling real-time visualization of data processing and system activity.
 */

/**
 * System Event Types - categorized by source service
 */
export type SystemEventType =
  // FHIR Service Events
  | 'FHIR_RESOURCE_CREATED'
  | 'FHIR_RESOURCE_UPDATED'
  | 'FHIR_RESOURCE_DELETED'
  | 'FHIR_BUNDLE_IMPORTED'
  // CQL Engine Events
  | 'EVALUATION_STARTED'
  | 'EVALUATION_COMPLETED'
  | 'EVALUATION_FAILED'
  | 'BATCH_STARTED'
  | 'BATCH_PROGRESS'
  | 'BATCH_COMPLETED'
  // Quality Measure Events
  | 'MEASURE_CALCULATED'
  | 'COMPLIANCE_UPDATED'
  | 'HEALTH_SCORE_UPDATED'
  // Care Gap Events
  | 'CARE_GAP_DETECTED'
  | 'CARE_GAP_CLOSED'
  | 'CARE_GAP_ESCALATED'
  // Consent Events
  | 'CONSENT_GRANTED'
  | 'CONSENT_REVOKED'
  | 'CONSENT_EXPIRED'
  // System Events
  | 'SERVICE_STARTED'
  | 'SERVICE_STOPPED'
  | 'ERROR_OCCURRED';

/**
 * Event Category for grouping and filtering
 */
export type EventCategory =
  | 'fhir'
  | 'evaluation'
  | 'quality'
  | 'care-gap'
  | 'consent'
  | 'system';

/**
 * Event Severity Level
 */
export type EventSeverity = 'info' | 'success' | 'warning' | 'error';

/**
 * System Event - core event structure
 */
export interface SystemEvent {
  /** Unique event identifier */
  id: string;

  /** Event type */
  type: SystemEventType;

  /** Event category for filtering */
  category: EventCategory;

  /** Severity level */
  severity: EventSeverity;

  /** Human-readable event title */
  title: string;

  /** Detailed event description */
  description: string;

  /** ISO timestamp when event occurred */
  timestamp: string;

  /** Source service that generated the event */
  source: string;

  /** Optional patient context */
  patient?: {
    id: string;
    name?: string;
    mrn?: string;
  };

  /** Optional measure context */
  measure?: {
    id: string;
    name: string;
  };

  /** Additional event-specific metadata */
  metadata?: Record<string, any>;

  /** Processing duration in milliseconds (if applicable) */
  durationMs?: number;
}

/**
 * Live Metrics - real-time system performance metrics
 */
export interface LiveMetrics {
  /** Patients processed today */
  patientsProcessed: number;
  patientsProcessedChange: number;

  /** Current throughput (evaluations per second) */
  throughputPerSecond: number;
  maxThroughput: number;

  /** Overall compliance rate */
  complianceRate: number;
  complianceRateChange: number;

  /** Open care gaps */
  openCareGaps: number;
  careGapsChange: number;

  /** Success rate (last 100 operations) */
  successRate: number;

  /** Average processing time in milliseconds */
  avgProcessingTimeMs: number;

  /** Timestamp of last update */
  lastUpdated: string;
}

/**
 * Pipeline Node Status - status of each node in the data flow pipeline
 */
export type PipelineNodeStatus = 'active' | 'idle' | 'processing' | 'error';

/**
 * Pipeline Node - represents a service in the data flow diagram
 */
export interface PipelineNode {
  /** Node identifier */
  id: string;

  /** Display name */
  name: string;

  /** Short description */
  description: string;

  /** Current status */
  status: PipelineNodeStatus;

  /** Items processed per second */
  throughput: number;

  /** Error count (if any) */
  errorCount: number;

  /** Last activity timestamp */
  lastActivity: string;
}

/**
 * Pipeline Connection - represents data flow between nodes
 */
export interface PipelineConnection {
  /** Source node ID */
  from: string;

  /** Target node ID */
  to: string;

  /** Items flowing per second */
  throughput: number;

  /** Whether connection is active */
  isActive: boolean;
}

/**
 * Data Flow Pipeline State
 */
export interface PipelineState {
  nodes: PipelineNode[];
  connections: PipelineConnection[];
  lastUpdated: string;
}

/**
 * Event Filter Options
 */
export interface EventFilterOptions {
  categories?: EventCategory[];
  severities?: EventSeverity[];
  types?: SystemEventType[];
  patientId?: string;
  startTime?: string;
  endTime?: string;
}

// ============================================================
// Helper Functions
// ============================================================

/**
 * Get icon for event type
 */
export function getEventIcon(type: SystemEventType): string {
  const iconMap: Record<SystemEventType, string> = {
    // FHIR
    FHIR_RESOURCE_CREATED: 'add_circle',
    FHIR_RESOURCE_UPDATED: 'edit',
    FHIR_RESOURCE_DELETED: 'delete',
    FHIR_BUNDLE_IMPORTED: 'cloud_upload',
    // Evaluation
    EVALUATION_STARTED: 'play_arrow',
    EVALUATION_COMPLETED: 'check_circle',
    EVALUATION_FAILED: 'error',
    BATCH_STARTED: 'batch_prediction',
    BATCH_PROGRESS: 'pending',
    BATCH_COMPLETED: 'task_alt',
    // Quality
    MEASURE_CALCULATED: 'calculate',
    COMPLIANCE_UPDATED: 'verified',
    HEALTH_SCORE_UPDATED: 'health_and_safety',
    // Care Gap
    CARE_GAP_DETECTED: 'warning',
    CARE_GAP_CLOSED: 'check',
    CARE_GAP_ESCALATED: 'priority_high',
    // Consent
    CONSENT_GRANTED: 'thumb_up',
    CONSENT_REVOKED: 'thumb_down',
    CONSENT_EXPIRED: 'schedule',
    // System
    SERVICE_STARTED: 'power_settings_new',
    SERVICE_STOPPED: 'power_off',
    ERROR_OCCURRED: 'report_problem',
  };
  return iconMap[type] || 'info';
}

/**
 * Get category from event type
 */
export function getCategoryFromType(type: SystemEventType): EventCategory {
  if (type.startsWith('FHIR_')) return 'fhir';
  if (type.startsWith('EVALUATION_') || type.startsWith('BATCH_')) return 'evaluation';
  if (type.startsWith('MEASURE_') || type.startsWith('COMPLIANCE_') || type.startsWith('HEALTH_SCORE_')) return 'quality';
  if (type.startsWith('CARE_GAP_')) return 'care-gap';
  if (type.startsWith('CONSENT_')) return 'consent';
  return 'system';
}

/**
 * Get severity color class
 */
export function getSeverityColor(severity: EventSeverity): string {
  const colorMap: Record<EventSeverity, string> = {
    info: 'primary',
    success: 'success',
    warning: 'warn',
    error: 'error',
  };
  return colorMap[severity] || 'primary';
}

/**
 * Get node status color
 */
export function getNodeStatusColor(status: PipelineNodeStatus): string {
  const colorMap: Record<PipelineNodeStatus, string> = {
    active: '#4caf50',
    idle: '#9e9e9e',
    processing: '#2196f3',
    error: '#f44336',
  };
  return colorMap[status] || '#9e9e9e';
}

/**
 * Format timestamp as relative time (e.g., "2 seconds ago")
 */
export function formatRelativeTime(timestamp: string): string {
  const now = new Date();
  const then = new Date(timestamp);
  const diffMs = now.getTime() - then.getTime();
  const diffSec = Math.floor(diffMs / 1000);

  if (diffSec < 5) return 'just now';
  if (diffSec < 60) return `${diffSec}s ago`;

  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}m ago`;

  const diffHour = Math.floor(diffMin / 60);
  if (diffHour < 24) return `${diffHour}h ago`;

  const diffDay = Math.floor(diffHour / 24);
  return `${diffDay}d ago`;
}

/**
 * Generate a unique event ID
 */
export function generateEventId(): string {
  return `evt-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

/**
 * Create a system event with defaults
 */
export function createSystemEvent(
  type: SystemEventType,
  title: string,
  description: string,
  options?: Partial<SystemEvent>
): SystemEvent {
  return {
    id: generateEventId(),
    type,
    category: getCategoryFromType(type),
    severity: 'info',
    title,
    description,
    timestamp: new Date().toISOString(),
    source: 'unknown',
    ...options,
  };
}

/**
 * Default pipeline nodes for the data flow visualization
 */
export const DEFAULT_PIPELINE_NODES: PipelineNode[] = [
  {
    id: 'fhir',
    name: 'FHIR Server',
    description: 'Patient data ingestion',
    status: 'idle',
    throughput: 0,
    errorCount: 0,
    lastActivity: new Date().toISOString(),
  },
  {
    id: 'cql',
    name: 'CQL Engine',
    description: 'Quality measure evaluation',
    status: 'idle',
    throughput: 0,
    errorCount: 0,
    lastActivity: new Date().toISOString(),
  },
  {
    id: 'quality',
    name: 'Quality Measures',
    description: 'Compliance calculation',
    status: 'idle',
    throughput: 0,
    errorCount: 0,
    lastActivity: new Date().toISOString(),
  },
  {
    id: 'caregap',
    name: 'Care Gap Detector',
    description: 'Gap identification',
    status: 'idle',
    throughput: 0,
    errorCount: 0,
    lastActivity: new Date().toISOString(),
  },
];

/**
 * Default pipeline connections
 */
export const DEFAULT_PIPELINE_CONNECTIONS: PipelineConnection[] = [
  { from: 'fhir', to: 'cql', throughput: 0, isActive: false },
  { from: 'cql', to: 'quality', throughput: 0, isActive: false },
  { from: 'quality', to: 'caregap', throughput: 0, isActive: false },
];

/**
 * Default live metrics
 */
export const DEFAULT_LIVE_METRICS: LiveMetrics = {
  patientsProcessed: 0,
  patientsProcessedChange: 0,
  throughputPerSecond: 0,
  maxThroughput: 10,
  complianceRate: 0,
  complianceRateChange: 0,
  openCareGaps: 0,
  careGapsChange: 0,
  successRate: 100,
  avgProcessingTimeMs: 0,
  lastUpdated: new Date().toISOString(),
};
