package com.healthdata.cql.event.audit;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;

/**
 * Base interface for audit events in the CQL Engine.
 *
 * Audit events track all significant operations for compliance and debugging:
 * - CQL library operations (upload, update, delete)
 * - Value set operations (upload, update, delete)
 * - CQL evaluations (start, complete, fail)
 * - Data access patterns
 * - Security events
 *
 * Events are published asynchronously to Kafka and consumed by the Audit Service
 * which stores them in TimescaleDB for long-term retention and analysis.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "auditType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CqlLibraryAuditEvent.class, name = "CQL_LIBRARY"),
    @JsonSubTypes.Type(value = ValueSetAuditEvent.class, name = "VALUE_SET"),
    @JsonSubTypes.Type(value = CqlEvaluationAuditEvent.class, name = "CQL_EVALUATION"),
    @JsonSubTypes.Type(value = DataAccessAuditEvent.class, name = "DATA_ACCESS"),
    @JsonSubTypes.Type(value = SecurityAuditEvent.class, name = "SECURITY")
})
public interface AuditEvent {

    /**
     * Unique identifier for this audit event
     */
    String getEventId();

    /**
     * Type of audit event
     */
    AuditEventType getAuditType();

    /**
     * When the event occurred
     */
    Instant getTimestamp();

    /**
     * Tenant identifier for multi-tenancy
     */
    String getTenantId();

    /**
     * User who performed the action (username or service account)
     */
    String getPerformedBy();

    /**
     * Action performed (CREATE, UPDATE, DELETE, EXECUTE, ACCESS, etc.)
     */
    String getAction();

    /**
     * Resource type being audited (CQL_LIBRARY, VALUE_SET, PATIENT_DATA, etc.)
     */
    String getResourceType();

    /**
     * Unique identifier of the resource being audited
     */
    String getResourceId();

    /**
     * Result of the operation (SUCCESS, FAILURE, PARTIAL)
     */
    OperationResult getResult();

    /**
     * Additional details about the operation (JSON)
     */
    String getDetails();

    /**
     * Client IP address if available
     */
    String getClientIp();

    /**
     * Request ID for correlation with application logs
     */
    String getRequestId();

    /**
     * Audit event type enumeration
     */
    enum AuditEventType {
        CQL_LIBRARY,
        VALUE_SET,
        CQL_EVALUATION,
        DATA_ACCESS,
        SECURITY
    }

    /**
     * Operation result enumeration
     */
    enum OperationResult {
        SUCCESS,
        FAILURE,
        PARTIAL
    }
}
