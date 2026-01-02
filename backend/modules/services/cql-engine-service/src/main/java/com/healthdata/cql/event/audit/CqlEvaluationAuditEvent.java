package com.healthdata.cql.event.audit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Audit event for CQL evaluation operations.
 *
 * This is the most critical audit event for healthcare compliance as it tracks:
 * - Which CQL library was executed
 * - What patient data was used
 * - What the results were
 * - Who initiated the evaluation
 * - How long it took
 * - Any errors that occurred
 *
 * This allows users to see "how the data is flowing and why" which is essential
 * for understanding healthcare decisions made by the system.
 */
@Value
@Builder
@Jacksonized
public class CqlEvaluationAuditEvent implements AuditEvent {

    @JsonProperty("eventId")
    String eventId;

    @JsonProperty("timestamp")
    Instant timestamp;

    @JsonProperty("tenantId")
    String tenantId;

    @JsonProperty("performedBy")
    String performedBy;

    @JsonProperty("action")
    String action; // "EVALUATE_CQL"

    @JsonProperty("resourceType")
    String resourceType; // "CQL_EVALUATION"

    @JsonProperty("resourceId")
    String resourceId; // Evaluation ID

    @JsonProperty("result")
    OperationResult result;

    @JsonProperty("details")
    String details; // JSON with additional information

    @JsonProperty("clientIp")
    String clientIp;

    @JsonProperty("requestId")
    String requestId;

    // CQL Evaluation specific fields

    @JsonProperty("evaluationId")
    UUID evaluationId;

    @JsonProperty("cqlLibraryId")
    UUID cqlLibraryId;

    @JsonProperty("cqlLibraryName")
    String cqlLibraryName;

    @JsonProperty("cqlLibraryVersion")
    String cqlLibraryVersion;

    @JsonProperty("patientId")
    UUID patientId;

    @JsonProperty("measureIdentifier")
    String measureIdentifier; // For HEDIS/quality measures

    @JsonProperty("fhirResourcesAccessed")
    List<String> fhirResourcesAccessed; // e.g., ["Patient", "Observation", "Condition"]

    @JsonProperty("fhirResourceCount")
    Map<String, Integer> fhirResourceCount; // e.g., {"Observation": 45, "Condition": 12}

    @JsonProperty("evaluationStartTime")
    Instant evaluationStartTime;

    @JsonProperty("evaluationEndTime")
    Instant evaluationEndTime;

    @JsonProperty("durationMs")
    Long durationMs;

    @JsonProperty("resultSummary")
    String resultSummary; // Brief summary of evaluation result

    @JsonProperty("numerator")
    Boolean numerator; // For quality measures

    @JsonProperty("denominator")
    Boolean denominator; // For quality measures

    @JsonProperty("exclusion")
    Boolean exclusion; // For quality measures

    @JsonProperty("errorMessage")
    String errorMessage; // If evaluation failed

    @JsonProperty("cacheHit")
    Boolean cacheHit; // Was result served from cache

    @JsonProperty("dataFlowSteps")
    List<DataFlowStep> dataFlowSteps; // Detailed steps of how data flowed

    @Override
    public AuditEventType getAuditType() {
        return AuditEventType.CQL_EVALUATION;
    }

    /**
     * Represents a step in the data flow through the CQL engine.
     * This helps users understand "why" certain decisions were made.
     */
    @Value
    @Builder
    @Jacksonized
    public static class DataFlowStep {
        @JsonProperty("stepNumber")
        int stepNumber;

        @JsonProperty("stepName")
        String stepName; // e.g., "Fetch Patient Demographics", "Evaluate Diabetes Criteria"

        @JsonProperty("stepType")
        String stepType; // "DATA_FETCH", "EXPRESSION_EVAL", "LOGIC_DECISION"

        @JsonProperty("timestamp")
        Instant timestamp;

        @JsonProperty("resourcesAccessed")
        List<String> resourcesAccessed; // FHIR resources accessed in this step

        @JsonProperty("inputData")
        String inputData; // Summary of input data (sanitized)

        @JsonProperty("outputData")
        String outputData; // Summary of output data (sanitized)

        @JsonProperty("decision")
        String decision; // Decision made in this step

        @JsonProperty("reasoning")
        String reasoning; // Why this decision was made

        @JsonProperty("durationMs")
        Long durationMs;
    }
}
