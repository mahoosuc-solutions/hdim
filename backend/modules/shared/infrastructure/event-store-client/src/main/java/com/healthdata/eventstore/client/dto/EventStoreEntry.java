package com.healthdata.eventstore.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO representing a stored event.
 * Maps to the event-store-service EventStoreEntry entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventStoreEntry {
    private UUID id;
    private String tenantId;
    private UUID aggregateId;
    private String aggregateType;
    private String eventType;
    private Integer eventVersion;
    private Object payload;
    private UUID causationId;
    private UUID correlationId;
    private String userId;
    private String userEmail;
    private Instant occurredAt;
}
