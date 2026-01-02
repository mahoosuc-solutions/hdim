package com.healthdata.cql.event.audit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit event for CQL library management operations.
 *
 * Tracks:
 * - Library uploads
 * - Library updates
 * - Library deletions
 * - Library retrievals
 */
@Value
@Builder
@Jacksonized
public class CqlLibraryAuditEvent implements AuditEvent {

    @JsonProperty("eventId")
    String eventId;

    @JsonProperty("timestamp")
    Instant timestamp;

    @JsonProperty("tenantId")
    String tenantId;

    @JsonProperty("performedBy")
    String performedBy;

    @JsonProperty("action")
    String action; // CREATE_LIBRARY, UPDATE_LIBRARY, DELETE_LIBRARY, GET_LIBRARY

    @JsonProperty("resourceType")
    String resourceType; // "CQL_LIBRARY"

    @JsonProperty("resourceId")
    String resourceId; // Library ID

    @JsonProperty("result")
    OperationResult result;

    @JsonProperty("details")
    String details;

    @JsonProperty("clientIp")
    String clientIp;

    @JsonProperty("requestId")
    String requestId;

    // CQL Library specific fields

    @JsonProperty("libraryId")
    UUID libraryId;

    @JsonProperty("libraryName")
    String libraryName;

    @JsonProperty("libraryVersion")
    String libraryVersion;

    @JsonProperty("libraryContentLength")
    Integer libraryContentLength; // Size of CQL content

    @JsonProperty("previousVersion")
    String previousVersion; // For updates

    @Override
    public AuditEventType getAuditType() {
        return AuditEventType.CQL_LIBRARY;
    }
}
