package com.healthdata.{{DOMAIN}}event.listener;

import com.healthdata.{{DOMAIN}}event.projection.{{DOMAIN_PASCAL}}Projection;
import com.healthdata.{{DOMAIN}}event.repository.{{DOMAIN_PASCAL}}ProjectionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * {{DOMAIN_PASCAL}}EventListener - Kafka Event Consumer
 *
 * Consumes domain events from Kafka and updates {{DOMAIN_PASCAL}}Projection read model.
 * Implements idempotent event processing with find-or-create pattern.
 *
 * Kafka Configuration:
 * - Group ID: {{SERVICE_NAME}}
 * - Auto Offset Reset: earliest (replay from beginning)
 * - Manual Commit: false (auto-commit after successful processing)
 *
 * Topics Consumed:
{{TOPICS_COMMENT}}
 *
 * Eventual Consistency SLA: < 500ms from event publication to projection update
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class {{DOMAIN_PASCAL}}EventListener {

    private final {{DOMAIN_PASCAL}}ProjectionRepository projectionRepository;
    private final ObjectMapper objectMapper;

    // ========================================
    // Event Handlers (Add one per event type)
    // ========================================

    // TODO: Add @KafkaListener methods for each event type
    // Template:
    //
    // @KafkaListener(
    //     topics = "{{DOMAIN}}.created",
    //     groupId = "{{SERVICE_NAME}}",
    //     containerFactory = "kafkaListenerContainerFactory"
    // )
    // @Transactional
    // public void on{{Domain}}Created(String message) {
    //     try {
    //         JsonNode event = objectMapper.readTree(message);
    //
    //         String tenantId = event.get("tenantId").asText();
    //         UUID {{DOMAIN}}Id = UUID.fromString(event.get("{{DOMAIN}}Id").asText());
    //
    //         log.debug("Processing {{DOMAIN}}.created for {{DOMAIN}}={} in tenant={}", {{DOMAIN}}Id, tenantId);
    //
    //         // Idempotent: find existing or create new
    //         projectionRepository.findByTenantIdAnd{{DOMAIN_PASCAL}}Id(tenantId, {{DOMAIN}}Id)
    //             .ifPresentOrElse(
    //                 projection -> {
    //                     log.warn("Projection already exists for {{DOMAIN}}={}, skipping duplicate event", {{DOMAIN}}Id);
    //                 },
    //                 () -> {
    //                     {{DOMAIN_PASCAL}}Projection projection = {{DOMAIN_PASCAL}}Projection.builder()
    //                         .tenantId(tenantId)
    //                         .{{DOMAIN}}Id({{DOMAIN}}Id)
    //                         // Extract fields from event
    //                         .eventVersion(0L)
    //                         .build();
    //
    //                     projectionRepository.save(projection);
    //                     log.info("Created projection for {{DOMAIN}}={} in tenant={}", {{DOMAIN}}Id, tenantId);
    //                 }
    //             );
    //     } catch (Exception e) {
    //         log.error("Failed to process {{DOMAIN}}.created event: {}", message, e);
    //         throw new RuntimeException("Event processing failed", e);
    //     }
    // }
    //
    // @KafkaListener(
    //     topics = "{{DOMAIN}}.updated",
    //     groupId = "{{SERVICE_NAME}}",
    //     containerFactory = "kafkaListenerContainerFactory"
    // )
    // @Transactional
    // public void on{{Domain}}Updated(String message) {
    //     try {
    //         JsonNode event = objectMapper.readTree(message);
    //
    //         String tenantId = event.get("tenantId").asText();
    //         UUID {{DOMAIN}}Id = UUID.fromString(event.get("{{DOMAIN}}Id").asText());
    //
    //         log.debug("Processing {{DOMAIN}}.updated for {{DOMAIN}}={} in tenant={}", {{DOMAIN}}Id, tenantId);
    //
    //         {{DOMAIN_PASCAL}}Projection projection = projectionRepository
    //             .findByTenantIdAnd{{DOMAIN_PASCAL}}Id(tenantId, {{DOMAIN}}Id)
    //             .orElseThrow(() -> new IllegalStateException(
    //                 "Cannot update non-existent projection for {{DOMAIN}}=" + {{DOMAIN}}Id
    //             ));
    //
    //         // Update fields from event
    //         // projection.setFieldName(event.get("fieldName").asText());
    //
    //         projection.incrementVersion();
    //         projectionRepository.save(projection);
    //
    //         log.info("Updated projection for {{DOMAIN}}={} in tenant={}", {{DOMAIN}}Id, tenantId);
    //     } catch (Exception e) {
    //         log.error("Failed to process {{DOMAIN}}.updated event: {}", message, e);
    //         throw new RuntimeException("Event processing failed", e);
    //     }
    // }

    // ========================================
    // Helper Methods (Private)
    // ========================================

    /**
     * Extract tenant ID from event (required field)
     */
    private String extractTenantId(JsonNode event) {
        if (!event.has("tenantId")) {
            throw new IllegalArgumentException("Event missing required field: tenantId");
        }
        return event.get("tenantId").asText();
    }

    /**
     * Extract entity ID from event (required field)
     */
    private UUID extractEntityId(JsonNode event, String fieldName) {
        if (!event.has(fieldName)) {
            throw new IllegalArgumentException("Event missing required field: " + fieldName);
        }
        return UUID.fromString(event.get(fieldName).asText());
    }

    /**
     * Extract timestamp from event (with fallback to now)
     */
    private Instant extractTimestamp(JsonNode event) {
        if (event.has("timestamp")) {
            return Instant.parse(event.get("timestamp").asText());
        }
        return Instant.now();
    }

    /**
     * Safely extract string field (null if missing)
     */
    private String extractString(JsonNode event, String fieldName) {
        return event.has(fieldName) ? event.get(fieldName).asText() : null;
    }

    /**
     * Safely extract integer field (null if missing)
     */
    private Integer extractInt(JsonNode event, String fieldName) {
        return event.has(fieldName) ? event.get(fieldName).asInt() : null;
    }

    /**
     * Safely extract boolean field (default false if missing)
     */
    private boolean extractBoolean(JsonNode event, String fieldName) {
        return event.has(fieldName) && event.get(fieldName).asBoolean();
    }
}
