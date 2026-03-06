package com.healthdata.events.intelligence.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.events.intelligence.dto.TrustProfileResponse;
import com.healthdata.events.intelligence.dto.UpdateValidationFindingStatusRequest;
import com.healthdata.events.intelligence.dto.ValidationFindingResponse;
import com.healthdata.events.intelligence.audit.ValidationFindingAuditService;
import com.healthdata.events.intelligence.controller.TenantScopedResourceNotFoundException;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.FindingStatus;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.FindingType;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.Severity;
import com.healthdata.events.intelligence.repository.IntelligenceValidationFindingRepository;
import com.healthdata.events.intelligence.projection.TenantTrustProjectionService;
import com.healthdata.eventsourcing.intelligence.CanonicalEventEnvelope;
import com.healthdata.eventsourcing.intelligence.IntelligenceTopics;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IntelligenceValidationService {

    private static final Logger log = LoggerFactory.getLogger(IntelligenceValidationService.class);

    private final IntelligenceValidationFindingRepository findingRepository;
    private final TenantTrustProjectionService tenantTrustProjectionService;
    private final ValidationFindingAuditService validationFindingAuditService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<ValidationFindingResponse> runValidation(CanonicalEventEnvelope envelope) {
        List<IntelligenceValidationFindingEntity> findings = evaluate(envelope).stream()
                .map(f -> findingRepository.save(toEntity(envelope, f)))
                .toList();

        findings.forEach(f -> kafkaTemplate.send(IntelligenceTopics.VALIDATION_FINDINGS, envelope.getTenantId(), Map.of(
                "findingId", f.getId().toString(),
                "tenantId", f.getTenantId(),
                "patientRef", f.getPatientRef(),
                "ruleCode", f.getRuleCode(),
                "severity", f.getSeverity().name(),
                "status", f.getStatus().name(),
                "emittedAtEpochSeconds", Instant.now().getEpochSecond()
        )));

        tenantTrustProjectionService.refreshForTenant(envelope.getTenantId());

        return findings.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ValidationFindingResponse> getFindings(String tenantId, String patientRef) {
        return findingRepository.findByTenantIdAndPatientRefOrderByCreatedAtDesc(tenantId, patientRef)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrustProfileResponse getTrustProfile(String tenantId, String patientRef) {
        List<IntelligenceValidationFindingEntity> openFindings =
                findingRepository.findByTenantIdAndPatientRefAndStatusOrderByCreatedAtDesc(
                        tenantId,
                        patientRef,
                        FindingStatus.OPEN
                );

        int trustScore = 100;
        for (IntelligenceValidationFindingEntity finding : openFindings) {
            trustScore -= switch (finding.getSeverity()) {
                case HIGH -> 20;
                case MEDIUM -> 10;
                case LOW -> 5;
            };
        }
        trustScore = Math.max(0, trustScore);

        long highSeverityOpenFindings = openFindings.stream().filter(f -> f.getSeverity() == Severity.HIGH).count();
        long consistencyFindings = openFindings.stream().filter(f -> f.getFindingType() == FindingType.CONSISTENCY).count();
        long dataCompletenessFindings = openFindings.stream().filter(f -> f.getFindingType() == FindingType.DATA_COMPLETENESS).count();
        long temporalFindings = openFindings.stream().filter(f -> f.getFindingType() == FindingType.TEMPORAL).count();

        return new TrustProfileResponse(
                patientRef,
                trustScore,
                openFindings.size(),
                highSeverityOpenFindings,
                consistencyFindings,
                dataCompletenessFindings,
                temporalFindings
        );
    }

    @Transactional
    public ValidationFindingResponse updateFindingStatus(
            String tenantId,
            UUID findingId,
            UpdateValidationFindingStatusRequest request
    ) {
        IntelligenceValidationFindingEntity finding = findingRepository.findByIdAndTenantId(findingId, tenantId)
                .orElseThrow(() -> new TenantScopedResourceNotFoundException("Validation finding not found"));

        validateFindingTransition(finding.getStatus(), request.status(), findingId);
        FindingStatus previous = finding.getStatus();

        finding.setStatus(request.status());
        finding.setActedBy(request.actedBy());
        finding.setActionNotes(request.notes());
        finding.setActedAt(Instant.now());

        IntelligenceValidationFindingEntity saved = findingRepository.save(finding);

        validationFindingAuditService.recordStateTransition(
                saved.getTenantId(),
                saved.getId(),
                previous,
                saved.getStatus(),
                request.actedBy(),
                request.notes()
        );

        kafkaTemplate.send(IntelligenceTopics.VALIDATION_FINDINGS, saved.getTenantId(), Map.of(
                "findingId", saved.getId().toString(),
                "tenantId", saved.getTenantId(),
                "patientRef", saved.getPatientRef(),
                "ruleCode", saved.getRuleCode(),
                "severity", saved.getSeverity().name(),
                "status", saved.getStatus().name(),
                "emittedAtEpochSeconds", Instant.now().getEpochSecond()
        ));

        tenantTrustProjectionService.refreshForTenant(saved.getTenantId());
        return toResponse(saved);
    }

    private void validateFindingTransition(FindingStatus current, FindingStatus target, UUID findingId) {
        if (current == target) {
            return;
        }
        boolean allowed = switch (current) {
            case OPEN -> target == FindingStatus.RESOLVED || target == FindingStatus.DISMISSED;
            case RESOLVED, DISMISSED -> false;
        };

        if (!allowed) {
            throw new IllegalStateException(
                    "Invalid validation finding state transition for " + findingId + ": " + current + " -> " + target
            );
        }
    }

    private List<FindingDraft> evaluate(CanonicalEventEnvelope envelope) {
        List<FindingDraft> findings = new ArrayList<>();
        Map<String, Object> payload = envelope.getPayload() == null ? Map.of() : envelope.getPayload();

        if (payload.isEmpty()) {
            findings.add(new FindingDraft(
                    "STRUCTURE_EMPTY_PAYLOAD",
                    "Payload is empty",
                    "Incoming event payload is empty and cannot support downstream clinical reasoning.",
                    Severity.HIGH,
                    FindingType.STRUCTURE,
                    Map.of("resourceType", envelope.getResourceType())
            ));
        }

        if (envelope.getPatientRef() == null || envelope.getPatientRef().isBlank()) {
            findings.add(new FindingDraft(
                    "STRUCTURE_MISSING_PATIENT",
                    "Missing patient reference",
                    "Event envelope is missing patientRef.",
                    Severity.HIGH,
                    FindingType.STRUCTURE,
                    Map.of()
            ));
        }

        if ("OBSERVATION".equalsIgnoreCase(envelope.getResourceType()) && !payload.containsKey("value")) {
            findings.add(new FindingDraft(
                    "DATA_MISSING_OBSERVATION_VALUE",
                    "Missing observation value",
                    "Observation event does not include a value, reducing recommendation confidence.",
                    Severity.MEDIUM,
                    FindingType.DATA_COMPLETENESS,
                    Map.of("requiredField", "value")
            ));
        }

        Instant occurredAt = envelope.getOccurredAt();
        if (occurredAt != null && Duration.between(occurredAt, Instant.now()).toDays() > 180) {
            findings.add(new FindingDraft(
                    "TEMPORAL_STALE_EVENT",
                    "Event data is stale",
                    "Event timestamp is older than 180 days and may not represent current clinical state.",
                    Severity.MEDIUM,
                    FindingType.TEMPORAL,
                    Map.of("occurredAt", occurredAt.toString())
            ));
        }

        if (Boolean.TRUE.equals(payload.get("conflictingIdentifiers"))) {
            findings.add(new FindingDraft(
                    "CONSISTENCY_IDENTIFIER_CONFLICT",
                    "Cross-source identifier conflict",
                    "Incoming data indicates identifier conflicts across source systems.",
                    Severity.HIGH,
                    FindingType.CONSISTENCY,
                    Map.of("flag", "conflictingIdentifiers")
            ));
        }

        return findings;
    }

    private IntelligenceValidationFindingEntity toEntity(CanonicalEventEnvelope envelope, FindingDraft draft) {
        return IntelligenceValidationFindingEntity.builder()
                .tenantId(envelope.getTenantId())
                .patientRef(envelope.getPatientRef())
                .sourceEventId(envelope.getEventId())
                .ruleCode(draft.ruleCode)
                .title(draft.title)
                .description(draft.description)
                .severity(draft.severity)
                .findingType(draft.findingType)
                .status(FindingStatus.OPEN)
                .details(asJson(draft.details))
                .build();
    }

    private ValidationFindingResponse toResponse(IntelligenceValidationFindingEntity entity) {
        return new ValidationFindingResponse(
                entity.getId(),
                entity.getSourceEventId(),
                entity.getRuleCode(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getSeverity(),
                entity.getFindingType(),
                entity.getStatus(),
                entity.getDetails(),
                entity.getCreatedAt()
        );
    }

    private String asJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize validation finding details", e);
            return "{}";
        }
    }

    private record FindingDraft(
            String ruleCode,
            String title,
            String description,
            Severity severity,
            FindingType findingType,
            Map<String, Object> details
    ) {
    }
}
