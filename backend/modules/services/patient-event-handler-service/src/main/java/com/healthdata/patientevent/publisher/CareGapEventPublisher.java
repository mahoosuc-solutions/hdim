package com.healthdata.patientevent.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Care Gap Event Publisher
 *
 * Publishes patient merge events to care gap service for consolidation of care gaps
 * from source patient into target patient.
 *
 * ★ Insight ─────────────────────────────────────
 * - Loose coupling: care-gap-service listens independently
 * - Best-effort cascade: failures logged but don't block merge
 * - Multi-tenant isolation: tenantId included in every event
 * - Idempotency: Events include merge metadata for deduplication
 * ─────────────────────────────────────────────────
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CareGapEventPublisher {

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String CARE_GAP_MERGED_TOPIC = "careGap.patient.merged";

    /**
     * Publish care gap merge event for consolidation
     *
     * When a patient merge occurs, this publishes an event to notify the care gap service
     * to consolidate care gaps from the source patient into the target patient.
     *
     * @param sourcePatientId Patient being merged away
     * @param targetPatientId Patient receiving merge
     * @param tenantId Tenant for isolation
     * @param mergedAt Timestamp of merge
     * @param confidenceScore Merge confidence (0-1)
     * @param sourceIdentifiers Source patient identifiers for tracing
     * @param targetIdentifiers Target patient identifiers for tracing
     */
    public void publishMergedGaps(
            String sourcePatientId,
            String targetPatientId,
            String tenantId,
            Instant mergedAt,
            Double confidenceScore,
            java.util.List<?> sourceIdentifiers,
            java.util.List<?> targetIdentifiers) {

        try {
            Map<String, Object> event = buildCareGapMergedEvent(
                sourcePatientId, targetPatientId, tenantId, mergedAt,
                confidenceScore, sourceIdentifiers, targetIdentifiers
            );

            String key = String.format("%s-%s", tenantId, targetPatientId);
            kafkaTemplate.send(CARE_GAP_MERGED_TOPIC, key, event);

            log.info("Published care gap merge event: source={}, target={}, tenant={}, topic={}",
                sourcePatientId, targetPatientId, tenantId, CARE_GAP_MERGED_TOPIC);

        } catch (Exception e) {
            log.error("Error publishing care gap merge event: source={}, target={}, tenant={}",
                sourcePatientId, targetPatientId, tenantId, e);
            // Do not re-throw - cascade is best-effort
        }
    }

    /**
     * Build care gap merged event payload
     *
     * @return Event payload for Kafka publication
     */
    private Map<String, Object> buildCareGapMergedEvent(
            String sourcePatientId,
            String targetPatientId,
            String tenantId,
            Instant mergedAt,
            Double confidenceScore,
            java.util.List<?> sourceIdentifiers,
            java.util.List<?> targetIdentifiers) {

        Map<String, Object> event = new HashMap<>();

        // Event metadata
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "patient.merged");
        event.put("eventTimestamp", Instant.now().toString());

        // Merge details
        event.put("sourcePatientId", sourcePatientId);
        event.put("targetPatientId", targetPatientId);
        event.put("tenantId", tenantId);
        event.put("mergedAt", mergedAt.toString());
        event.put("mergeConfidenceScore", confidenceScore);

        // Additional metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sourceIdentifiers", sourceIdentifiers);
        metadata.put("targetIdentifiers", targetIdentifiers);
        metadata.put("mergeChainDepth", 1);  // TODO: Include actual depth from merge event

        event.put("additionalMetadata", metadata);

        return event;
    }
}
