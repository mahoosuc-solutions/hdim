package com.healthdata.events.intelligence.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.events.intelligence.dto.RecommendationLineageResponse;
import com.healthdata.events.intelligence.dto.RecommendationResponse;
import com.healthdata.events.intelligence.dto.ReviewRecommendationRequest;
import com.healthdata.events.intelligence.dto.SignalResponse;
import com.healthdata.events.intelligence.audit.RecommendationAuditService;
import com.healthdata.events.intelligence.controller.TenantScopedResourceNotFoundException;
import com.healthdata.events.intelligence.entity.IntelligenceRecommendationEntity;
import com.healthdata.events.intelligence.repository.IntelligenceRecommendationRepository;
import com.healthdata.eventsourcing.intelligence.CanonicalEventEnvelope;
import com.healthdata.eventsourcing.intelligence.IntelligenceTopics;
import com.healthdata.eventsourcing.intelligence.RecommendationReviewStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IntelligenceRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(IntelligenceRecommendationService.class);

    private final IntelligenceRecommendationRepository recommendationRepository;
    private final IntelligenceValidationService validationService;
    private final RecommendationAuditService recommendationAuditService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public List<RecommendationResponse> ingestAndGenerate(CanonicalEventEnvelope envelope) {
        CanonicalEventEnvelope normalized = normalizeEnvelope(envelope);
        kafkaTemplate.send(IntelligenceTopics.INGEST_NORMALIZED, normalized.getTenantId(), normalized);
        validationService.runValidation(normalized);

        List<IntelligenceRecommendationEntity> generated = new ArrayList<>();

        detectRuleSignals(normalized).forEach(signal -> generated.add(saveRecommendation(normalized, signal)));

        SignalDraft anomalySignal = detectAnomalySignal(normalized);
        if (anomalySignal != null) {
            generated.add(saveRecommendation(normalized, anomalySignal));
        }

        if (generated.isEmpty()) {
            SignalDraft observabilitySignal = new SignalDraft(
                    "INGEST_OBSERVED",
                    "Ingestion observed",
                    "Data ingested successfully with no immediate actionable issues.",
                    "LOW",
                    Math.max(0.50, normalized.getConfidence())
            );
            generated.add(saveRecommendation(normalized, observabilitySignal));
        }

        return generated.stream().map(this::toRecommendation).toList();
    }

    @Transactional(readOnly = true)
    public List<SignalResponse> getSignals(String tenantId, String patientRef) {
        return recommendationRepository.findByTenantIdAndPatientRefOrderByCreatedAtDesc(tenantId, patientRef)
                .stream()
                .map(r -> new SignalResponse(
                        r.getId(),
                        r.getPatientRef(),
                        r.getSignalType(),
                        r.getRiskTier(),
                        r.getConfidence(),
                        r.getTitle(),
                        r.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(String tenantId, String patientRef) {
        return recommendationRepository.findByTenantIdAndPatientRefOrderByCreatedAtDesc(tenantId, patientRef)
                .stream()
                .map(this::toRecommendation)
                .toList();
    }

    @Transactional
    public RecommendationResponse reviewRecommendation(String tenantId, UUID recommendationId, ReviewRecommendationRequest request) {
        IntelligenceRecommendationEntity entity = recommendationRepository.findByIdAndTenantId(recommendationId, tenantId)
                .orElseThrow(() -> new TenantScopedResourceNotFoundException("Recommendation not found"));

        validateTransition(entity.getStatus(), request.status(), recommendationId);

        RecommendationReviewStatus previousStatus = entity.getStatus();
        entity.setStatus(request.status());
        entity.setReviewedBy(request.reviewedBy());
        entity.setReviewNotes(request.notes());
        entity.setReviewedAt(Instant.now());

        IntelligenceRecommendationEntity updated = recommendationRepository.save(entity);
        recommendationAuditService.recordStateTransition(
                updated.getTenantId(),
                updated.getId(),
                previousStatus,
                updated.getStatus(),
                updated.getReviewedBy(),
                updated.getReviewNotes()
        );

        kafkaTemplate.send(IntelligenceTopics.RECOMMENDATIONS_REVIEWED, updated.getTenantId(), Map.of(
                "recommendationId", updated.getId().toString(),
                "tenantId", updated.getTenantId(),
                "status", updated.getStatus().name(),
                "reviewedBy", updated.getReviewedBy(),
                "reviewedAt", updated.getReviewedAt().toString()
        ));

        return toRecommendation(updated);
    }

    private void validateTransition(
            RecommendationReviewStatus current,
            RecommendationReviewStatus target,
            UUID recommendationId
    ) {
        if (current == target) {
            return;
        }

        boolean allowed = switch (current) {
            case PROPOSED -> EnumSet.of(RecommendationReviewStatus.TRIAGED, RecommendationReviewStatus.REJECTED).contains(target);
            case TRIAGED -> EnumSet.of(RecommendationReviewStatus.APPROVED, RecommendationReviewStatus.REJECTED).contains(target);
            case APPROVED -> EnumSet.of(RecommendationReviewStatus.IMPLEMENTED).contains(target);
            case IMPLEMENTED -> EnumSet.of(RecommendationReviewStatus.OUTCOME_OBSERVED).contains(target);
            case REJECTED, OUTCOME_OBSERVED -> false;
        };

        if (!allowed) {
            throw new IllegalStateException(
                    "Invalid recommendation state transition for " + recommendationId + ": " + current + " -> " + target
            );
        }
    }

    @Transactional(readOnly = true)
    public RecommendationLineageResponse getLineage(String tenantId, UUID recommendationId) {
        IntelligenceRecommendationEntity entity = recommendationRepository.findByIdAndTenantId(recommendationId, tenantId)
                .orElseThrow(() -> new TenantScopedResourceNotFoundException("Recommendation lineage not found"));

        return new RecommendationLineageResponse(
                entity.getId(),
                entity.getSourceEventId(),
                entity.getPatientRef(),
                entity.getSignalType(),
                entity.getEvidence(),
                entity.getRiskTier(),
                entity.getConfidence(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getReviewedAt(),
                entity.getReviewedBy()
        );
    }

    private IntelligenceRecommendationEntity saveRecommendation(CanonicalEventEnvelope envelope, SignalDraft draft) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("payload", envelope.getPayload());
        evidence.put("provenance", envelope.getProvenance());
        evidence.put("traceId", envelope.getTraceId());
        evidence.put("schemaVersion", envelope.getSchemaVersion());
        evidence.put("signal", draft.signalType);

        IntelligenceRecommendationEntity recommendation = IntelligenceRecommendationEntity.builder()
                .tenantId(envelope.getTenantId())
                .patientRef(envelope.getPatientRef())
                .sourceEventId(envelope.getEventId())
                .signalType(draft.signalType)
                .title(draft.title)
                .description(draft.description)
                .riskTier(draft.riskTier)
                .confidence(draft.confidence)
                .evidence(asJson(evidence))
                .status(RecommendationReviewStatus.PROPOSED)
                .build();

        IntelligenceRecommendationEntity saved = recommendationRepository.save(recommendation);

        kafkaTemplate.send(IntelligenceTopics.INTELLIGENCE_SIGNALS, saved.getTenantId(), Map.of(
                "recommendationId", saved.getId().toString(),
                "patientRef", saved.getPatientRef(),
                "signalType", saved.getSignalType(),
                "riskTier", saved.getRiskTier(),
                "confidence", saved.getConfidence()
        ));

        kafkaTemplate.send(IntelligenceTopics.RECOMMENDATIONS_GENERATED, saved.getTenantId(), Map.of(
                "recommendationId", saved.getId().toString(),
                "patientRef", saved.getPatientRef(),
                "status", saved.getStatus().name(),
                "title", saved.getTitle()
        ));

        return saved;
    }

    private List<SignalDraft> detectRuleSignals(CanonicalEventEnvelope envelope) {
        List<SignalDraft> signals = new ArrayList<>();
        Map<String, Object> payload = envelope.getPayload();

        String source = envelope.getSourceType().toUpperCase(Locale.ROOT);
        if ("CLAIMS".equals(source) && truthy(payload.get("deniedClaim"))) {
            signals.add(new SignalDraft(
                    "CLAIM_DENIAL_RISK",
                    "Claim denial risk detected",
                    "Claims intake indicates denial risk requiring quality review.",
                    "HIGH",
                    0.82
            ));
        }

        if (truthy(payload.get("careGap")) || "OPEN".equalsIgnoreCase(String.valueOf(payload.get("gapStatus")))) {
            signals.add(new SignalDraft(
                    "CARE_GAP_OPEN",
                    "Open care gap detected",
                    "Patient has an open care gap candidate requiring coordinator review.",
                    "HIGH",
                    0.88
            ));
        }

        if (truthy(payload.get("missingData")) || payload.containsKey("missingFields")) {
            signals.add(new SignalDraft(
                    "DATA_QUALITY_GAP",
                    "Data quality gap detected",
                    "Incoming payload is missing required clinical data for high-confidence decisions.",
                    "MEDIUM",
                    0.76
            ));
        }

        if ("OBSERVATION".equalsIgnoreCase(envelope.getResourceType()) && payload.containsKey("value")
                && parseDouble(payload.get("value")) > 9.0) {
            signals.add(new SignalDraft(
                    "A1C_HIGH_RISK",
                    "Elevated A1C signal",
                    "Observation value indicates elevated A1C trend needing clinical validation.",
                    "HIGH",
                    0.84
            ));
        }

        return signals;
    }

    private SignalDraft detectAnomalySignal(CanonicalEventEnvelope envelope) {
        double anomalyScore = 0.0;
        Map<String, Object> payload = envelope.getPayload();

        if (payload.size() > 18) {
            anomalyScore += 0.35;
        }
        if (envelope.getConfidence() != null && envelope.getConfidence() < 0.60) {
            anomalyScore += 0.30;
        }
        if ("HIGH".equalsIgnoreCase(envelope.getRiskTier())) {
            anomalyScore += 0.20;
        }
        if (payload.containsKey("outlier")) {
            anomalyScore += 0.25;
        }

        if (anomalyScore < 0.45) {
            return null;
        }

        double calibrated = Math.min(0.95, Math.max(0.50, anomalyScore));
        return new SignalDraft(
                "NOVEL_PATTERN",
                "Novel pattern detected",
                "Hybrid anomaly scoring found non-routine data shape; route for human clinical review.",
                calibrated >= 0.75 ? "HIGH" : "MEDIUM",
                calibrated
        );
    }

    private CanonicalEventEnvelope normalizeEnvelope(CanonicalEventEnvelope envelope) {
        if (envelope.getOccurredAt() == null) {
            envelope.setOccurredAt(Instant.now());
        }
        if (envelope.getProvenance() == null) {
            envelope.setProvenance(Map.of());
        }
        if (envelope.getPayload() == null) {
            envelope.setPayload(Map.of());
        }
        if (envelope.getConfidence() == null) {
            envelope.setConfidence(0.70);
        }
        if (envelope.getRiskTier() == null || envelope.getRiskTier().isBlank()) {
            envelope.setRiskTier("MEDIUM");
        }
        return envelope;
    }

    private RecommendationResponse toRecommendation(IntelligenceRecommendationEntity entity) {
        return new RecommendationResponse(
                entity.getId(),
                entity.getPatientRef(),
                entity.getSignalType(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getRiskTier(),
                entity.getConfidence(),
                entity.getStatus(),
                entity.getReviewedBy(),
                entity.getReviewNotes(),
                entity.getReviewedAt(),
                entity.getCreatedAt()
        );
    }

    private String asJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize recommendation evidence", e);
            return "{}";
        }
    }

    private boolean truthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
    }

    private double parseDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private record SignalDraft(String signalType, String title, String description, String riskTier, double confidence) {
    }
}
