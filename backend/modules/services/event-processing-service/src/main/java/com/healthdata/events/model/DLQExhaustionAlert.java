package com.healthdata.events.model;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DLQ Exhaustion Alert Model
 *
 * Represents an alert for a DLQ event that has exhausted all retry attempts.
 * Used to notify operations team about critical failures requiring manual intervention.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DLQExhaustionAlert {

    /**
     * Unique identifier for the DLQ event
     */
    private UUID eventId;

    /**
     * Type of the event that failed (e.g., "PATIENT_REGISTERED", "HEALTH_RECORD_UPDATED")
     */
    private String eventType;

    /**
     * Tenant ID to identify which organization owns this event
     */
    private String tenantId;

    /**
     * Original error message from the first failure
     */
    private String originalErrorMessage;

    /**
     * Number of retry attempts made
     */
    private Integer retryCount;

    /**
     * Timestamp of the first failure
     */
    private Instant firstFailureTimestamp;

    /**
     * Timestamp of the last failure/retry
     */
    private Instant lastFailureTimestamp;

    /**
     * Patient ID affected by this failure (if available)
     */
    private String affectedPatientId;

    /**
     * Kafka topic where the event originated
     */
    private String topic;

    /**
     * DLQ entry ID for reference
     */
    private UUID dlqId;

    /**
     * Stack trace of the last error (for detailed diagnostics)
     */
    private String stackTrace;

    /**
     * Determine if this is a critical event type that requires immediate escalation
     */
    public boolean isCritical() {
        if (eventType == null) {
            return false;
        }

        // Critical event types that affect patient safety or regulatory compliance
        return eventType.contains("CRITICAL")
            || eventType.contains("EMERGENCY")
            || eventType.contains("PATIENT_REGISTERED")
            || eventType.contains("MEDICATION")
            || eventType.contains("CLINICAL_ALERT");
    }

    /**
     * Get a human-readable summary of the alert
     */
    public String getSummary() {
        return String.format(
            "DLQ Event Exhausted: eventType=%s, tenantId=%s, retries=%d, patientId=%s",
            eventType, tenantId, retryCount, affectedPatientId != null ? affectedPatientId : "N/A"
        );
    }
}
