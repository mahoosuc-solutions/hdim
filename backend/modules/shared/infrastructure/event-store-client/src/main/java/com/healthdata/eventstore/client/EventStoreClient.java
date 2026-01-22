package com.healthdata.eventstore.client;

import com.healthdata.eventstore.client.config.EventStoreClientConfig;
import com.healthdata.eventstore.client.dto.AppendEventRequest;
import com.healthdata.eventstore.client.dto.EventStoreEntry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Feign client for communicating with the event-store-service.
 * Provides methods for appending and querying events from the immutable event log.
 */
@FeignClient(
    name = "event-store-service",
    url = "${event-store.service.url:http://localhost:8090}",
    configuration = EventStoreClientConfig.class
)
public interface EventStoreClient {

    /**
     * Append a new event to the event store.
     *
     * @param tenantId The tenant ID for multi-tenant isolation
     * @param request  The event to append
     * @return The stored event with assigned ID and version
     */
    @PostMapping("/api/v1/events")
    EventStoreEntry appendEvent(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestBody AppendEventRequest request
    );

    /**
     * Retrieve all events for a specific aggregate.
     *
     * @param tenantId      The tenant ID
     * @param aggregateId   The aggregate ID (e.g., patientId, careGapId)
     * @param aggregateType The aggregate type (e.g., "Patient", "CareGap")
     * @return List of events ordered by version
     */
    @GetMapping("/api/v1/events/aggregate/{aggregateId}")
    List<EventStoreEntry> getEventsByAggregate(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable("aggregateId") UUID aggregateId,
        @RequestParam("aggregateType") String aggregateType
    );

    /**
     * Retrieve events by event type.
     *
     * @param tenantId  The tenant ID
     * @param eventType The event type (e.g., "PatientCreatedEvent")
     * @param limit     Maximum number of events to return
     * @return List of events
     */
    @GetMapping("/api/v1/events/type/{eventType}")
    List<EventStoreEntry> getEventsByType(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable("eventType") String eventType,
        @RequestParam(value = "limit", defaultValue = "100") int limit
    );

    /**
     * Get the latest version number for an aggregate.
     *
     * @param tenantId      The tenant ID
     * @param aggregateId   The aggregate ID
     * @param aggregateType The aggregate type
     * @return The latest version number, or null if no events exist
     */
    @GetMapping("/api/v1/events/aggregate/{aggregateId}/version")
    Integer getLatestVersion(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable("aggregateId") UUID aggregateId,
        @RequestParam("aggregateType") String aggregateType
    );
}
