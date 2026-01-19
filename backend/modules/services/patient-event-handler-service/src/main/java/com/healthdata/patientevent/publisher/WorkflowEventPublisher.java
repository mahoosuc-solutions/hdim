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
 * Workflow Event Publisher
 *
 * Publishes patient merge events to clinical workflow service for consolidation of
 * active tasks and workflows from source patient into target patient.
 *
 * ★ Insight ─────────────────────────────────────
 * - Microservices pattern: Each service handles its own domain (workflows)
 * - Event propagation: Single merge event triggers updates across multiple services
 * - Ordering guarantees: Kafka partitioning by tenant ensures serial processing
 * - Error resilience: If workflow service is down, message queues for retry
 * ─────────────────────────────────────────────────
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEventPublisher {

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String WORKFLOW_MERGED_TOPIC = "workflow.patient.merged";

    /**
     * Publish workflow merge event for task consolidation
     *
     * When a patient merge occurs, this publishes an event to notify the clinical workflow service
     * to consolidate active tasks and workflows from the source patient into the target patient.
     *
     * @param sourcePatientId Patient being merged away
     * @param targetPatientId Patient receiving merge
     * @param tenantId Tenant for isolation
     * @param mergedAt Timestamp of merge
     * @param confidenceScore Merge confidence (0-1)
     * @param sourceIdentifiers Source patient identifiers for tracing
     * @param targetIdentifiers Target patient identifiers for tracing
     */
    public void publishMergedWorkflows(
            String sourcePatientId,
            String targetPatientId,
            String tenantId,
            Instant mergedAt,
            Double confidenceScore,
            java.util.List<?> sourceIdentifiers,
            java.util.List<?> targetIdentifiers) {

        try {
            Map<String, Object> event = buildWorkflowMergedEvent(
                sourcePatientId, targetPatientId, tenantId, mergedAt,
                confidenceScore, sourceIdentifiers, targetIdentifiers
            );

            String key = String.format("%s-%s", tenantId, targetPatientId);
            kafkaTemplate.send(WORKFLOW_MERGED_TOPIC, key, event);

            log.info("Published workflow merge event: source={}, target={}, tenant={}, topic={}",
                sourcePatientId, targetPatientId, tenantId, WORKFLOW_MERGED_TOPIC);

        } catch (Exception e) {
            log.error("Error publishing workflow merge event: source={}, target={}, tenant={}",
                sourcePatientId, targetPatientId, tenantId, e);
            // Do not re-throw - cascade is best-effort
        }
    }

    /**
     * Build workflow merged event payload
     *
     * @return Event payload for Kafka publication
     */
    private Map<String, Object> buildWorkflowMergedEvent(
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
