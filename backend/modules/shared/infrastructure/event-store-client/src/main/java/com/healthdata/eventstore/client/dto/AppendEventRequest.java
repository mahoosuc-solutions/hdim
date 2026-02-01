package com.healthdata.eventstore.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for appending events to the event store.
 * Maps to the event-store-service REST API contract.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppendEventRequest {
    private UUID aggregateId;
    private String aggregateType;
    private String eventType;
    private Object payload;
    private UUID causationId;
    private UUID correlationId;
    private String userId;
    private String userEmail;
}
