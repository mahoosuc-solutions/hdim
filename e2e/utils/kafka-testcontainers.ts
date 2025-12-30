/**
 * Kafka Testcontainers Configuration
 *
 * Provides utilities for spinning up Kafka containers for integration testing.
 * Uses Testcontainers to manage Docker containers programmatically.
 *
 * Note: This configuration is designed for backend integration tests.
 * For E2E tests, the Kafka cluster should be running via docker-compose.
 */

import { EventType, HDIMEvent, MockEventFactory } from '../fixtures/mock-events';

/**
 * Kafka Topics used by HDIM
 */
export const KAFKA_TOPICS = {
  // Evaluation events
  EVALUATION_EVENTS: 'hdim.evaluation.events',
  EVALUATION_RESULTS: 'hdim.evaluation.results',

  // Care gap events
  CARE_GAP_EVENTS: 'hdim.care-gap.events',
  CARE_GAP_COMMANDS: 'hdim.care-gap.commands',

  // Alert events
  CLINICAL_ALERTS: 'hdim.clinical.alerts',
  ALERT_NOTIFICATIONS: 'hdim.alert.notifications',

  // Patient events
  PATIENT_UPDATES: 'hdim.patient.updates',
  PATIENT_SYNC: 'hdim.patient.sync',

  // Report events
  REPORT_REQUESTS: 'hdim.report.requests',
  REPORT_COMPLETED: 'hdim.report.completed',

  // System events
  AUDIT_EVENTS: 'hdim.audit.events',
  SYSTEM_HEALTH: 'hdim.system.health',
};

/**
 * Kafka Consumer Configuration for Testing
 */
export interface KafkaTestConsumerConfig {
  groupId: string;
  topics: string[];
  autoOffsetReset: 'earliest' | 'latest';
  maxPollRecords: number;
  pollTimeout: number;
}

/**
 * Default consumer configurations for different test scenarios
 */
export const CONSUMER_CONFIGS: Record<string, KafkaTestConsumerConfig> = {
  evaluationTests: {
    groupId: 'e2e-evaluation-tests',
    topics: [KAFKA_TOPICS.EVALUATION_EVENTS, KAFKA_TOPICS.EVALUATION_RESULTS],
    autoOffsetReset: 'latest',
    maxPollRecords: 100,
    pollTimeout: 5000,
  },

  careGapTests: {
    groupId: 'e2e-care-gap-tests',
    topics: [KAFKA_TOPICS.CARE_GAP_EVENTS],
    autoOffsetReset: 'latest',
    maxPollRecords: 100,
    pollTimeout: 5000,
  },

  alertTests: {
    groupId: 'e2e-alert-tests',
    topics: [KAFKA_TOPICS.CLINICAL_ALERTS, KAFKA_TOPICS.ALERT_NOTIFICATIONS],
    autoOffsetReset: 'latest',
    maxPollRecords: 100,
    pollTimeout: 5000,
  },

  allEvents: {
    groupId: 'e2e-all-events',
    topics: Object.values(KAFKA_TOPICS),
    autoOffsetReset: 'latest',
    maxPollRecords: 500,
    pollTimeout: 10000,
  },
};

/**
 * Kafka Test Utilities
 *
 * Provides helper functions for Kafka-based testing.
 * These utilities work with the existing docker-compose Kafka setup.
 */
export class KafkaTestUtils {
  private bootstrapServers: string;
  private mockEvents: MockEventFactory;

  constructor(bootstrapServers: string = 'localhost:9094') {
    this.bootstrapServers = bootstrapServers;
    this.mockEvents = new MockEventFactory();
  }

  /**
   * Get bootstrap servers configuration
   */
  getBootstrapServers(): string {
    return this.bootstrapServers;
  }

  /**
   * Create test topic configuration
   */
  getTopicConfig(topicName: string): {
    topic: string;
    numPartitions: number;
    replicationFactor: number;
  } {
    return {
      topic: topicName,
      numPartitions: 3,
      replicationFactor: 1, // For local testing
    };
  }

  /**
   * Generate test events for a specific topic
   */
  generateTestEvents(topic: string, count: number): HDIMEvent[] {
    const events: HDIMEvent[] = [];

    switch (topic) {
      case KAFKA_TOPICS.EVALUATION_EVENTS:
        for (let i = 0; i < count; i++) {
          const sequence = this.mockEvents.evaluationSequence(
            `PATIENT_${i}`,
            'CMS130v12',
            i % 3 === 0 ? 'COMPLIANT' : i % 3 === 1 ? 'NON_COMPLIANT' : 'NOT_ELIGIBLE'
          );
          events.push(...sequence);
        }
        break;

      case KAFKA_TOPICS.CARE_GAP_EVENTS:
        for (let i = 0; i < count; i++) {
          events.push(
            this.mockEvents.careGapCreated(
              `PATIENT_${i}`,
              'CMS130v12',
              i % 3 === 0 ? 'HIGH' : i % 3 === 1 ? 'MEDIUM' : 'LOW'
            )
          );
        }
        break;

      case KAFKA_TOPICS.CLINICAL_ALERTS:
        const alertTypes = [
          { type: 'A1C_CRITICAL' as const, severity: 'CRITICAL' as const },
          { type: 'BP_CRITICAL' as const, severity: 'HIGH' as const },
          { type: 'CARE_GAP_OVERDUE' as const, severity: 'MEDIUM' as const },
        ];
        for (let i = 0; i < count; i++) {
          const alertConfig = alertTypes[i % alertTypes.length];
          events.push(
            this.mockEvents.alertTriggered(
              `PATIENT_${i}`,
              alertConfig.type,
              alertConfig.severity
            )
          );
        }
        break;

      default:
        console.warn(`No event generator for topic: ${topic}`);
    }

    return events;
  }

  /**
   * Serialize event for Kafka message
   */
  serializeEvent(event: HDIMEvent): string {
    return JSON.stringify(event);
  }

  /**
   * Deserialize Kafka message to event
   */
  deserializeEvent(message: string): HDIMEvent {
    return JSON.parse(message) as HDIMEvent;
  }

  /**
   * Generate message key for partitioning
   */
  generateMessageKey(event: HDIMEvent): string {
    // Use tenantId + correlationId for consistent partitioning
    return `${event.tenantId}:${event.correlationId}`;
  }
}

/**
 * Kafka Connection Test Configuration
 *
 * Configuration for testing Kafka connection resilience.
 */
export interface KafkaConnectionTestConfig {
  // Time to wait for initial connection
  connectionTimeout: number;

  // Time to wait for reconnection after failure
  reconnectTimeout: number;

  // Number of reconnection attempts
  maxRetries: number;

  // Backoff multiplier for reconnection
  backoffMultiplier: number;
}

export const DEFAULT_CONNECTION_CONFIG: KafkaConnectionTestConfig = {
  connectionTimeout: 10000,
  reconnectTimeout: 5000,
  maxRetries: 3,
  backoffMultiplier: 1.5,
};

/**
 * Event Ordering Validation
 *
 * Utilities for validating event ordering in Kafka streams.
 */
export class EventOrderValidator {
  private eventSequences: Map<string, HDIMEvent[]> = new Map();

  /**
   * Record an event for order validation
   */
  recordEvent(event: HDIMEvent): void {
    const key = event.correlationId;
    const sequence = this.eventSequences.get(key) || [];
    sequence.push(event);
    this.eventSequences.set(key, sequence);
  }

  /**
   * Validate evaluation event ordering
   *
   * Expected: STARTED -> PROGRESS* -> (COMPLETE | FAILED)
   */
  validateEvaluationSequence(correlationId: string): {
    valid: boolean;
    errors: string[];
  } {
    const sequence = this.eventSequences.get(correlationId) || [];
    const errors: string[] = [];

    if (sequence.length === 0) {
      return { valid: false, errors: ['No events found for correlation ID'] };
    }

    // First event should be STARTED
    if (sequence[0].type !== EventType.EVALUATION_STARTED) {
      errors.push(`First event should be EVALUATION_STARTED, got ${sequence[0].type}`);
    }

    // Last event should be COMPLETE or FAILED
    const lastEvent = sequence[sequence.length - 1];
    if (
      lastEvent.type !== EventType.EVALUATION_COMPLETE &&
      lastEvent.type !== EventType.EVALUATION_FAILED
    ) {
      errors.push(`Last event should be COMPLETE or FAILED, got ${lastEvent.type}`);
    }

    // Check timestamps are increasing
    for (let i = 1; i < sequence.length; i++) {
      if (sequence[i].timestamp < sequence[i - 1].timestamp) {
        errors.push(`Event ${i} has timestamp before event ${i - 1}`);
      }
    }

    // Check progress events are in order
    const progressEvents = sequence.filter(
      (e) => e.type === EventType.EVALUATION_PROGRESS
    );
    for (let i = 1; i < progressEvents.length; i++) {
      if (progressEvents[i].payload.progress < progressEvents[i - 1].payload.progress) {
        errors.push(`Progress decreased from ${progressEvents[i - 1].payload.progress} to ${progressEvents[i].payload.progress}`);
      }
    }

    return { valid: errors.length === 0, errors };
  }

  /**
   * Validate batch event ordering
   *
   * Expected: STARTED -> PROGRESS* -> (COMPLETE | FAILED)
   */
  validateBatchSequence(batchId: string): {
    valid: boolean;
    errors: string[];
  } {
    const sequence = this.eventSequences.get(batchId) || [];
    const errors: string[] = [];

    if (sequence.length === 0) {
      return { valid: false, errors: ['No events found for batch ID'] };
    }

    // First event should be BATCH_STARTED
    if (sequence[0].type !== EventType.BATCH_STARTED) {
      errors.push(`First event should be BATCH_STARTED, got ${sequence[0].type}`);
    }

    // Last event should be BATCH_COMPLETE or BATCH_FAILED
    const lastEvent = sequence[sequence.length - 1];
    if (
      lastEvent.type !== EventType.BATCH_COMPLETE &&
      lastEvent.type !== EventType.BATCH_FAILED
    ) {
      errors.push(`Last event should be BATCH_COMPLETE or BATCH_FAILED, got ${lastEvent.type}`);
    }

    // Check progress is monotonically increasing
    const progressEvents = sequence.filter(
      (e) => e.type === EventType.BATCH_PROGRESS
    );
    for (let i = 1; i < progressEvents.length; i++) {
      if (progressEvents[i].payload.completedPatients < progressEvents[i - 1].payload.completedPatients) {
        errors.push(`Completed count decreased`);
      }
    }

    return { valid: errors.length === 0, errors };
  }

  /**
   * Clear recorded events
   */
  clear(): void {
    this.eventSequences.clear();
  }

  /**
   * Get all recorded sequences
   */
  getAllSequences(): Map<string, HDIMEvent[]> {
    return new Map(this.eventSequences);
  }
}

/**
 * Export default instances
 */
export const kafkaTestUtils = new KafkaTestUtils();
export const eventOrderValidator = new EventOrderValidator();
