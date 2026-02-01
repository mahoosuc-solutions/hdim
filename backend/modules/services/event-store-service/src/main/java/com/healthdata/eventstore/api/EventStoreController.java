package com.healthdata.eventstore.api;

import com.healthdata.eventstore.domain.EventSnapshot;
import com.healthdata.eventstore.domain.EventStoreEntry;
import com.healthdata.eventstore.service.EventStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Event Store REST Controller
 *
 * Purpose: HTTP API for event store operations
 *
 * Endpoints:
 * - POST /api/v1/events - Append new event
 * - GET /api/v1/events/aggregate/{aggregateId} - Get all events for aggregate
 * - GET /api/v1/events/aggregate/{aggregateId}/after/{version} - Get events after version
 * - GET /api/v1/events/timerange - Get events in time range
 * - POST /api/v1/snapshots - Create snapshot
 * - GET /api/v1/snapshots/{aggregateId}/latest - Get latest snapshot
 *
 * Security: All endpoints require X-Tenant-ID header for multi-tenant isolation
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EventStoreController {

    private final EventStoreService eventStoreService;

    /**
     * Append a new event to the event store
     *
     * POST /api/v1/events
     *
     * Request Body:
     * {
     *   "aggregateId": "123e4567-e89b-12d3-a456-426614174000",
     *   "aggregateType": "Patient",
     *   "eventType": "PatientCreatedEvent",
     *   "payload": { "firstName": "John", "lastName": "Doe" },
     *   "correlationId": "abc-123",
     *   "userId": "user-456"
     * }
     */
    @PostMapping("/events")
    public ResponseEntity<EventStoreEntry> appendEvent(
            @RequestBody AppendEventRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Appending event: aggregateId={}, eventType={}",
                request.getAggregateId(), request.getEventType());

        EventStoreEntry entry = eventStoreService.appendEvent(
                request.getAggregateId(),
                request.getAggregateType(),
                request.getEventType(),
                request.getPayload(),
                tenantId,
                request.getCausationId(),
                request.getCorrelationId(),
                request.getUserId(),
                request.getUserEmail()
        );

        return ResponseEntity.ok(entry);
    }

    /**
     * Get all events for an aggregate
     *
     * GET /api/v1/events/aggregate/{aggregateId}?aggregateType=Patient
     */
    @GetMapping("/events/aggregate/{aggregateId}")
    public ResponseEntity<List<EventStoreEntry>> getEventsForAggregate(
            @PathVariable UUID aggregateId,
            @RequestParam String aggregateType,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.debug("Fetching events for aggregate: aggregateId={}, aggregateType={}",
                aggregateId, aggregateType);

        List<EventStoreEntry> events = eventStoreService.getEventsForAggregate(
                aggregateId, aggregateType, tenantId
        );

        return ResponseEntity.ok(events);
    }

    /**
     * Get events after a specific version
     *
     * GET /api/v1/events/aggregate/{aggregateId}/after/{version}?aggregateType=Patient
     */
    @GetMapping("/events/aggregate/{aggregateId}/after/{version}")
    public ResponseEntity<List<EventStoreEntry>> getEventsAfterVersion(
            @PathVariable UUID aggregateId,
            @PathVariable Integer version,
            @RequestParam String aggregateType,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.debug("Fetching events after version: aggregateId={}, afterVersion={}",
                aggregateId, version);

        List<EventStoreEntry> events = eventStoreService.getEventsAfterVersion(
                aggregateId, aggregateType, version, tenantId
        );

        return ResponseEntity.ok(events);
    }

    /**
     * Get events within a time range
     *
     * GET /api/v1/events/timerange?startTime=2026-01-01T00:00:00Z&endTime=2026-01-31T23:59:59Z
     */
    @GetMapping("/events/timerange")
    public ResponseEntity<List<EventStoreEntry>> getEventsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.debug("Fetching events in time range: {} to {}", startTime, endTime);

        List<EventStoreEntry> events = eventStoreService.getEventsByTimeRange(
                startTime, endTime, tenantId
        );

        return ResponseEntity.ok(events);
    }

    /**
     * Create a snapshot for an aggregate
     *
     * POST /api/v1/snapshots
     *
     * Request Body:
     * {
     *   "aggregateId": "123e4567-e89b-12d3-a456-426614174000",
     *   "aggregateType": "Patient",
     *   "snapshotData": { "firstName": "John", "status": "ACTIVE" }
     * }
     */
    @PostMapping("/snapshots")
    public ResponseEntity<EventSnapshot> createSnapshot(
            @RequestBody CreateSnapshotRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Creating snapshot for aggregate: aggregateId={}", request.getAggregateId());

        EventSnapshot snapshot = eventStoreService.createSnapshot(
                request.getAggregateId(),
                request.getAggregateType(),
                request.getSnapshotData(),
                tenantId
        );

        return ResponseEntity.ok(snapshot);
    }

    /**
     * Get latest snapshot for an aggregate
     *
     * GET /api/v1/snapshots/{aggregateId}/latest?aggregateType=Patient
     */
    @GetMapping("/snapshots/{aggregateId}/latest")
    public ResponseEntity<EventSnapshot> getLatestSnapshot(
            @PathVariable UUID aggregateId,
            @RequestParam String aggregateType,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.debug("Fetching latest snapshot for aggregate: aggregateId={}", aggregateId);

        return eventStoreService.getLatestSnapshot(aggregateId, aggregateType, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Health check endpoint
     *
     * GET /api/v1/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "event-store-service"
        ));
    }

    // ===== DTOs =====

    /**
     * Request DTO for appending events
     */
    public static class AppendEventRequest {
        private UUID aggregateId;
        private String aggregateType;
        private String eventType;
        private Object payload;
        private UUID causationId;
        private UUID correlationId;
        private String userId;
        private String userEmail;

        // Getters and setters
        public UUID getAggregateId() { return aggregateId; }
        public void setAggregateId(UUID aggregateId) { this.aggregateId = aggregateId; }

        public String getAggregateType() { return aggregateType; }
        public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }

        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }

        public Object getPayload() { return payload; }
        public void setPayload(Object payload) { this.payload = payload; }

        public UUID getCausationId() { return causationId; }
        public void setCausationId(UUID causationId) { this.causationId = causationId; }

        public UUID getCorrelationId() { return correlationId; }
        public void setCorrelationId(UUID correlationId) { this.correlationId = correlationId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    }

    /**
     * Request DTO for creating snapshots
     */
    public static class CreateSnapshotRequest {
        private UUID aggregateId;
        private String aggregateType;
        private Object snapshotData;

        // Getters and setters
        public UUID getAggregateId() { return aggregateId; }
        public void setAggregateId(UUID aggregateId) { this.aggregateId = aggregateId; }

        public String getAggregateType() { return aggregateType; }
        public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }

        public Object getSnapshotData() { return snapshotData; }
        public void setSnapshotData(Object snapshotData) { this.snapshotData = snapshotData; }
    }
}
