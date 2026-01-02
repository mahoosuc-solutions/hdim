package com.healthdata.cql.event.audit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit event for value set management operations.
 *
 * Tracks:
 * - Value set uploads
 * - Value set updates
 * - Value set deletions
 * - Value set retrievals
 */
@Value
@Builder
@Jacksonized
public class ValueSetAuditEvent implements AuditEvent {

    @JsonProperty("eventId")
    String eventId;

    @JsonProperty("timestamp")
    Instant timestamp;

    @JsonProperty("tenantId")
    String tenantId;

    @JsonProperty("performedBy")
    String performedBy;

    @JsonProperty("action")
    String action; // CREATE_VALUE_SET, UPDATE_VALUE_SET, DELETE_VALUE_SET, GET_VALUE_SET

    @JsonProperty("resourceType")
    String resourceType; // "VALUE_SET"

    @JsonProperty("resourceId")
    String resourceId; // Value set ID

    @JsonProperty("result")
    OperationResult result;

    @JsonProperty("details")
    String details;

    @JsonProperty("clientIp")
    String clientIp;

    @JsonProperty("requestId")
    String requestId;

    // Value Set specific fields

    @JsonProperty("valueSetId")
    UUID valueSetId;

    @JsonProperty("valueSetOid")
    String valueSetOid; // OID from VSAC

    @JsonProperty("valueSetName")
    String valueSetName;

    @JsonProperty("valueSetVersion")
    String valueSetVersion;

    @JsonProperty("codesCount")
    Integer codesCount; // Number of codes in the value set

    @Override
    public AuditEventType getAuditType() {
        return AuditEventType.VALUE_SET;
    }
}
