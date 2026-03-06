package com.healthdata.events.intelligence.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.events.entity.EventEntity;
import com.healthdata.events.repository.EventRepository;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.FindingStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ValidationFindingAuditService {

    private static final Logger log = LoggerFactory.getLogger(ValidationFindingAuditService.class);

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void recordStateTransition(
            String tenantId,
            UUID findingId,
            FindingStatus previousStatus,
            FindingStatus newStatus,
            String actedBy,
            String actionNotes
    ) {
        EventEntity event = EventEntity.builder()
                .tenantId(tenantId)
                .eventType("VALIDATION_FINDING_STATE_CHANGED")
                .aggregateType("INTELLIGENCE_VALIDATION_FINDING")
                .aggregateId(findingId.toString())
                .eventData(asJson(Map.of(
                        "previousStatus", previousStatus.name(),
                        "newStatus", newStatus.name(),
                        "actedBy", actedBy,
                        "actionNotes", actionNotes == null ? "" : actionNotes,
                        "changedAt", Instant.now().toString()
                )))
                .metadata(asJson(Map.of(
                        "source", "validation-finding-state",
                        "auditVersion", "1.0"
                )))
                .userId(actedBy)
                .correlationId(findingId.toString())
                .causationId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .version(1L)
                .processed(true)
                .processedAt(Instant.now())
                .build();

        eventRepository.save(event);
    }

    private String asJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize validation finding audit payload", e);
            return "{}";
        }
    }
}
