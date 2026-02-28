package com.healthdata.ingestion.application;

import com.healthdata.ingestion.interoperability.AdtEventState;
import com.healthdata.ingestion.interoperability.InteroperabilityErrorCode;
import com.healthdata.ingestion.interoperability.dto.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdtExchangeService {

    private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of("A01", "A03", "A08");
    private static final Set<String> SOURCE_ALLOWLIST = Set.of("hie-main", "ehr-bridge");

    private final Map<String, EncounterEventRecord> recordsByEventId = new ConcurrentHashMap<>();
    private final Map<String, String> eventIdBySourceMessageId = new ConcurrentHashMap<>();

    public AdtMessageIngestResponse ingestMessage(AdtMessageIngestRequest request) {
        if (!SOURCE_ALLOWLIST.contains(request.getSourceSystem())) {
            return AdtMessageIngestResponse.builder()
                    .tenantId(request.getTenantId())
                    .sourceMessageId(request.getSourceMessageId())
                    .correlationId(request.getCorrelationId())
                    .state(AdtEventState.REJECTED)
                    .errorCode(InteroperabilityErrorCode.AUTHZ_ERROR)
                    .auditEnvelope(audit(request, "AUTHZ_DENIED"))
                    .build();
        }

        if (!SUPPORTED_EVENT_TYPES.contains(request.getEventType())) {
            return AdtMessageIngestResponse.builder()
                    .tenantId(request.getTenantId())
                    .sourceMessageId(request.getSourceMessageId())
                    .correlationId(request.getCorrelationId())
                    .state(AdtEventState.REJECTED)
                    .errorCode(InteroperabilityErrorCode.UNSUPPORTED_EVENT_TYPE)
                    .auditEnvelope(audit(request, "UNSUPPORTED_EVENT_TYPE"))
                    .build();
        }

        if (isUnmatchedPatient(request.getPatientExternalId())) {
            return AdtMessageIngestResponse.builder()
                    .tenantId(request.getTenantId())
                    .sourceMessageId(request.getSourceMessageId())
                    .correlationId(request.getCorrelationId())
                    .state(AdtEventState.REJECTED)
                    .errorCode(InteroperabilityErrorCode.PATIENT_MATCH_FAILED)
                    .auditEnvelope(audit(request, "PATIENT_MATCH_FAILED"))
                    .build();
        }

        String existingEventId = eventIdBySourceMessageId.get(request.getSourceMessageId());
        if (existingEventId != null) {
            EncounterEventRecord existingRecord = recordsByEventId.get(existingEventId);
            return AdtMessageIngestResponse.builder()
                    .eventId(existingEventId)
                    .tenantId(existingRecord.getTenantId())
                    .sourceMessageId(existingRecord.getSourceMessageId())
                    .correlationId(existingRecord.getCorrelationId())
                    .state(existingRecord.getState())
                    .duplicate(true)
                    .auditEnvelope(audit(request, "DUPLICATE_SUPPRESSED"))
                    .build();
        }

        String eventId = UUID.randomUUID().toString();
        EncounterEventRecord record = EncounterEventRecord.builder()
                .eventId(eventId)
                .tenantId(request.getTenantId())
                .sourceSystem(request.getSourceSystem())
                .sourceMessageId(request.getSourceMessageId())
                .eventType(request.getEventType())
                .patientExternalId(request.getPatientExternalId())
                .encounterExternalId(request.getEncounterExternalId())
                .payloadHash(request.getPayloadHash())
                .correlationId(request.getCorrelationId())
                .state(AdtEventState.ROUTED)
                .auditEnvelope(audit(request, "ROUTED"))
                .build();

        recordsByEventId.put(eventId, record);
        eventIdBySourceMessageId.put(request.getSourceMessageId(), eventId);

        return AdtMessageIngestResponse.builder()
                .eventId(eventId)
                .tenantId(request.getTenantId())
                .sourceMessageId(request.getSourceMessageId())
                .correlationId(request.getCorrelationId())
                .state(AdtEventState.ROUTED)
                .duplicate(false)
                .auditEnvelope(record.getAuditEnvelope())
                .build();
    }

    public AdtAcknowledgementResponse acknowledge(AdtAcknowledgementRequest request) {
        EncounterEventRecord record = recordsByEventId.get(request.getEventId());
        if (record == null || !record.getTenantId().equals(request.getTenantId())) {
            return AdtAcknowledgementResponse.builder()
                    .eventId(request.getEventId())
                    .tenantId(request.getTenantId())
                    .correlationId(request.getCorrelationId())
                    .state(AdtEventState.REJECTED)
                    .errorCode(InteroperabilityErrorCode.NON_RETRYABLE_UPSTREAM)
                    .auditEnvelope(InteroperabilityAuditEnvelope.builder()
                            .tenantId(request.getTenantId())
                            .correlationId(request.getCorrelationId())
                            .sourceSystem(request.getSourceSystem())
                            .eventType("ACK")
                            .timestamp(Instant.now())
                            .outcome("EVENT_NOT_FOUND")
                            .build())
                    .build();
        }

        record.setState(AdtEventState.ACKNOWLEDGED);
        record.setAuditEnvelope(InteroperabilityAuditEnvelope.builder()
                .tenantId(record.getTenantId())
                .correlationId(record.getCorrelationId())
                .sourceSystem(record.getSourceSystem())
                .eventType(record.getEventType())
                .timestamp(Instant.now())
                .outcome("ACKNOWLEDGED")
                .build());

        return AdtAcknowledgementResponse.builder()
                .eventId(record.getEventId())
                .tenantId(record.getTenantId())
                .correlationId(record.getCorrelationId())
                .state(record.getState())
                .auditEnvelope(record.getAuditEnvelope())
                .build();
    }

    public EncounterEventRecord getEvent(String eventId) {
        return recordsByEventId.get(eventId);
    }

    private InteroperabilityAuditEnvelope audit(AdtMessageIngestRequest request, String outcome) {
        return InteroperabilityAuditEnvelope.builder()
                .tenantId(request.getTenantId())
                .correlationId(request.getCorrelationId())
                .sourceSystem(request.getSourceSystem())
                .eventType(request.getEventType())
                .timestamp(Instant.now())
                .outcome(outcome)
                .build();
    }

    private boolean isUnmatchedPatient(String patientExternalId) {
        return "UNMATCHED".equalsIgnoreCase(patientExternalId)
                || patientExternalId.toLowerCase(Locale.ROOT).startsWith("unknown-");
    }
}
