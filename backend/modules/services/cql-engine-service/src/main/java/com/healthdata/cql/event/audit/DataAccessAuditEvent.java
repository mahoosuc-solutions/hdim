package com.healthdata.cql.event.audit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Audit event for patient data access.
 *
 * This is critical for HIPAA compliance - tracks all access to patient data
 * including FHIR resource retrievals.
 */
@Value
@Builder
@Jacksonized
public class DataAccessAuditEvent implements AuditEvent {

    @JsonProperty("eventId")
    String eventId;

    @JsonProperty("timestamp")
    Instant timestamp;

    @JsonProperty("tenantId")
    String tenantId;

    @JsonProperty("performedBy")
    String performedBy;

    @JsonProperty("action")
    String action; // FETCH_PATIENT_DATA, SEARCH_PATIENTS, GET_FHIR_RESOURCE

    @JsonProperty("resourceType")
    String resourceType; // "PATIENT_DATA"

    @JsonProperty("resourceId")
    String resourceId; // Patient ID or resource ID

    @JsonProperty("result")
    OperationResult result;

    @JsonProperty("details")
    String details;

    @JsonProperty("clientIp")
    String clientIp;

    @JsonProperty("requestId")
    String requestId;

    // Data Access specific fields

    @JsonProperty("patientId")
    UUID patientId;

    @JsonProperty("fhirResourceType")
    String fhirResourceType; // e.g., "Observation", "Condition"

    @JsonProperty("fhirResourceIds")
    List<String> fhirResourceIds; // IDs of resources accessed

    @JsonProperty("resourceCount")
    Integer resourceCount; // Number of resources accessed

    @JsonProperty("purpose")
    String purpose; // Purpose of access (e.g., "CQL_EVALUATION", "QUALITY_MEASURE")

    @JsonProperty("evaluationId")
    String evaluationId; // Related evaluation ID if applicable

    @Override
    public AuditEventType getAuditType() {
        return AuditEventType.DATA_ACCESS;
    }
}
