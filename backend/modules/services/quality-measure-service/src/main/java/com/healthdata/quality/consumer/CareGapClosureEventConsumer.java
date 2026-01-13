package com.healthdata.quality.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.dto.CareGapClosureEvent;
import com.healthdata.quality.dto.FhirResourceEvent;
import com.healthdata.quality.persistence.CareGapEntity;
import com.healthdata.quality.service.CareGapMatchingService;
import com.healthdata.quality.service.CareGapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Care Gap Closure Event Consumer
 * Listens for FHIR resource events and automatically closes matching care gaps
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CareGapClosureEventConsumer {

    private final CareGapService careGapService;
    private final CareGapMatchingService matchingService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String CARE_GAP_CLOSURE_TOPIC = "care-gap.auto-closed";

    /**
     * Handle Procedure creation events
     */
    @KafkaListener(
        topics = "fhir.procedures.created",
        groupId = "quality-measure-service"
    )
    public void handleProcedureCreated(Map<String, Object> message,
                                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            log.debug("Received procedure created event: {}", message);

            FhirResourceEvent event = buildFhirResourceEvent(message, topic);

            // Validate event
            if (event.getTenantId() == null || event.getPatientId() == null) {
                log.warn("Invalid procedure event - missing tenant or patient ID");
                return;
            }

            // Find matching care gaps
            List<CareGapEntity> matchingGaps = matchingService.findMatchingCareGaps(
                event.getTenantId(),
                event.getPatientId(),
                event
            );

            if (matchingGaps.isEmpty()) {
                log.debug("No matching care gaps found for procedure {}", event.getResourceId());
                return;
            }

            // Auto-close matching care gaps
            for (CareGapEntity gap : matchingGaps) {
                autoCloseCareGap(gap, event);
            }

        } catch (Exception e) {
            log.error("Error processing procedure created event", e);
            // Don't rethrow - we don't want to block the Kafka consumer
        }
    }

    /**
     * Handle Observation creation events (lab results)
     */
    @KafkaListener(
        topics = "fhir.observations.created",
        groupId = "quality-measure-service"
    )
    public void handleObservationCreated(Map<String, Object> message,
                                         @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            log.debug("Received observation created event: {}", message);

            FhirResourceEvent event = buildFhirResourceEvent(message, topic);

            // Validate event
            if (event.getTenantId() == null || event.getPatientId() == null) {
                log.warn("Invalid observation event - missing tenant or patient ID");
                return;
            }

            // Find matching care gaps
            List<CareGapEntity> matchingGaps = matchingService.findMatchingCareGaps(
                event.getTenantId(),
                event.getPatientId(),
                event
            );

            if (matchingGaps.isEmpty()) {
                log.debug("No matching care gaps found for observation {}", event.getResourceId());
                return;
            }

            // Auto-close matching care gaps
            for (CareGapEntity gap : matchingGaps) {
                autoCloseCareGap(gap, event);
            }

        } catch (Exception e) {
            log.error("Error processing observation created event", e);
            // Don't rethrow - we don't want to block the Kafka consumer
        }
    }

    /**
     * Auto-close a care gap with evidence from FHIR resource
     */
    private void autoCloseCareGap(CareGapEntity gap, FhirResourceEvent event) {
        try {
            // Skip if already closed
            if (gap.getStatus() == CareGapEntity.Status.CLOSED ||
                gap.getStatus() == CareGapEntity.Status.DISMISSED) {
                log.debug("Care gap {} already closed/dismissed, skipping", gap.getId());
                return;
            }

            log.info("Auto-closing care gap {} for patient {} based on {} {}",
                gap.getId(), gap.getPatientId(), event.getResourceType(), event.getResourceId());

            // Auto-close the care gap
            careGapService.autoCloseCareGap(
                gap.getTenantId(),
                gap.getId(),
                event.getResourceType(),
                event.getResourceId(),
                matchingService.getMatchingSummary(gap, event)
            );

            // Publish care gap closure event
            publishClosureEvent(gap, event);

            log.info("Successfully auto-closed care gap {}", gap.getId());

        } catch (Exception e) {
            log.error("Error auto-closing care gap {}", gap.getId(), e);
        }
    }

    /**
     * Publish care gap closure event to Kafka
     */
    private void publishClosureEvent(CareGapEntity gap, FhirResourceEvent event) {
        try {
            CareGapClosureEvent closureEvent = CareGapClosureEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("care-gap.auto-closed")
                .tenantId(gap.getTenantId())
                .patientId(gap.getPatientId())
                .careGapId(gap.getId().toString())
                .gapType(gap.getGapType())
                .category(gap.getCategory().name().toLowerCase())
                .evidenceResourceType(event.getResourceType())
                .evidenceResourceId(event.getResourceId())
                .closedAt(Instant.now())
                .closedBy("SYSTEM")
                .build();

            String eventJson = objectMapper.writeValueAsString(closureEvent);
            kafkaTemplate.send(CARE_GAP_CLOSURE_TOPIC, closureEvent.getEventId(), eventJson);

            log.debug("Published care gap closure event for gap {}", gap.getId());

        } catch (Exception e) {
            log.error("Error publishing care gap closure event", e);
            // Don't rethrow - closure was successful even if event publishing failed
        }
    }

    private FhirResourceEvent buildFhirResourceEvent(Map<String, Object> message, String topic) {
        Map<String, Object> resource = getResourceMap(message.get("resource"));
        String resourceType = getString(message.get("resourceType"));
        if (resourceType == null && topic != null) {
            resourceType = topic.contains("procedures") ? "Procedure" : "Observation";
        }
        String resourceId = getString(message.getOrDefault("resourceId", message.get("id")));

        return FhirResourceEvent.builder()
            .eventId(getString(message.getOrDefault("eventId", UUID.randomUUID().toString())))
            .eventType(getString(message.getOrDefault("eventType", topic)))
            .resourceType(resourceType)
            .resourceId(resourceId)
            .tenantId(getString(message.get("tenantId")))
            .patientId(parseUuid(message.get("patientId")))
            .timestamp(parseInstant(message.getOrDefault("occurredAt", message.get("timestamp"))))
            .codes(extractCodes(resource))
            .category(extractCategory(resource))
            .status(getString(resource != null ? resource.get("status") : null))
            .performedDate(parseInstant(resource != null
                ? (resource.containsKey("performedDateTime")
                    ? resource.get("performedDateTime")
                    : resource.get("effectiveDateTime"))
                : null))
            .build();
    }

    private List<FhirResourceEvent.CodeableConcept> extractCodes(Map<String, Object> resource) {
        if (resource == null) {
            return List.of();
        }
        Object code = resource.get("code");
        Map<String, Object> codeMap = getMap(code);
        if (codeMap == null) {
            return List.of();
        }
        return List.of(toCodeableConcept(codeMap));
    }

    private FhirResourceEvent.CodeableConcept extractCategory(Map<String, Object> resource) {
        if (resource == null) {
            return null;
        }
        Map<String, Object> category = getMap(resource.get("category"));
        if (category == null) {
            return null;
        }
        return toCodeableConcept(category);
    }

    private FhirResourceEvent.CodeableConcept toCodeableConcept(Map<String, Object> data) {
        return FhirResourceEvent.CodeableConcept.builder()
            .text(getString(data.get("text")))
            .coding(extractCoding(data.get("coding")))
            .build();
    }

    private List<FhirResourceEvent.Coding> extractCoding(Object codingValue) {
        if (!(codingValue instanceof List<?> codingList)) {
            return List.of();
        }
        return codingList.stream()
            .map(this::getMap)
            .filter(map -> map != null)
            .map(map -> FhirResourceEvent.Coding.builder()
                .system(getString(map.get("system")))
                .code(getString(map.get("code")))
                .display(getString(map.get("display")))
                .build())
            .toList();
    }

    private Map<String, Object> getMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private Map<String, Object> getResourceMap(Object value) {
        Map<String, Object> resource = getMap(value);
        if (resource != null) {
            return resource;
        }
        if (value instanceof String resourceJson) {
            try {
                return objectMapper.readValue(resourceJson, Map.class);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String getString(Object value) {
        return value != null ? value.toString() : null;
    }

    private UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Instant parseInstant(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
