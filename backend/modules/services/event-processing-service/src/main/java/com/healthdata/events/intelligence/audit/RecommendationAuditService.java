package com.healthdata.events.intelligence.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.events.entity.EventEntity;
import com.healthdata.events.repository.EventRepository;
import com.healthdata.eventsourcing.intelligence.RecommendationReviewStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecommendationAuditService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationAuditService.class);

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public void recordStateTransition(
            String tenantId,
            UUID recommendationId,
            RecommendationReviewStatus previousStatus,
            RecommendationReviewStatus newStatus,
            String reviewedBy,
            String reviewNotes
    ) {
        EventEntity event = EventEntity.builder()
                .tenantId(tenantId)
                .eventType("RECOMMENDATION_STATE_CHANGED")
                .aggregateType("INTELLIGENCE_RECOMMENDATION")
                .aggregateId(recommendationId.toString())
                .eventData(asJson(Map.of(
                        "previousStatus", previousStatus.name(),
                        "newStatus", newStatus.name(),
                        "reviewedBy", reviewedBy,
                        "reviewNotes", reviewNotes == null ? "" : reviewNotes,
                        "changedAt", Instant.now().toString()
                )))
                .metadata(asJson(Map.of(
                        "source", "intelligence-review",
                        "auditVersion", "1.0"
                )))
                .userId(reviewedBy)
                .correlationId(recommendationId.toString())
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
            log.warn("Failed to serialize recommendation audit payload", e);
            return "{}";
        }
    }
}
