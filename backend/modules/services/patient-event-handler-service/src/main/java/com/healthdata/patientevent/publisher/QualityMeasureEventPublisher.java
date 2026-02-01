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
 * Quality Measure Event Publisher
 *
 * Publishes patient merge events to quality measure service for consolidation of
 * quality measure evaluation results from source patient into target patient.
 *
 * ★ Insight ─────────────────────────────────────
 * - Event-driven architecture: quality service listens asynchronously
 * - Eventual consistency: Merges propagate to dependent services over time
 * - Partition key strategy: Partitioned by tenant for ordering guarantees
 * - HIPAA audit trail: All merge events logged in Kafka for compliance
 * ─────────────────────────────────────────────────
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualityMeasureEventPublisher {

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String QUALITY_MEASURE_MERGED_TOPIC = "qualityMeasure.patient.merged";

    /**
     * Publish quality measure merge event for result consolidation
     *
     * When a patient merge occurs, this publishes an event to notify the quality measure service
     * to consolidate quality measure evaluation results from the source patient into the target patient.
     *
     * @param sourcePatientId Patient being merged away
     * @param targetPatientId Patient receiving merge
     * @param tenantId Tenant for isolation
     * @param mergedAt Timestamp of merge
     * @param confidenceScore Merge confidence (0-1)
     * @param sourceIdentifiers Source patient identifiers for tracing
     * @param targetIdentifiers Target patient identifiers for tracing
     */
    public void publishMergedResults(
            String sourcePatientId,
            String targetPatientId,
            String tenantId,
            Instant mergedAt,
            Double confidenceScore,
            java.util.List<?> sourceIdentifiers,
            java.util.List<?> targetIdentifiers) {

        try {
            Map<String, Object> event = buildQualityMeasureMergedEvent(
                sourcePatientId, targetPatientId, tenantId, mergedAt,
                confidenceScore, sourceIdentifiers, targetIdentifiers
            );

            String key = String.format("%s-%s", tenantId, targetPatientId);
            kafkaTemplate.send(QUALITY_MEASURE_MERGED_TOPIC, key, event);

            log.info("Published quality measure merge event: source={}, target={}, tenant={}, topic={}",
                sourcePatientId, targetPatientId, tenantId, QUALITY_MEASURE_MERGED_TOPIC);

        } catch (Exception e) {
            log.error("Error publishing quality measure merge event: source={}, target={}, tenant={}",
                sourcePatientId, targetPatientId, tenantId, e);
            // Do not re-throw - cascade is best-effort
        }
    }

    /**
     * Build quality measure merged event payload
     *
     * @return Event payload for Kafka publication
     */
    private Map<String, Object> buildQualityMeasureMergedEvent(
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
