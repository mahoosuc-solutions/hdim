package com.healthdata.events.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthdata.events.entity.DeadLetterQueueEntity;
import com.healthdata.events.service.DeadLetterQueueService;
import com.healthdata.events.service.DeadLetterQueueService.DLQStats;

import lombok.RequiredArgsConstructor;

/**
 * Dead Letter Queue Management API
 *
 * Provides endpoints for monitoring and managing failed events.
 */
@RestController
@RequestMapping("/api/v1/dead-letter-queue")
@RequiredArgsConstructor
public class DeadLetterQueueController {

    private final DeadLetterQueueService dlqService;

    /**
     * Get failed events for a tenant
     */
    @GetMapping("/failed")
    public ResponseEntity<Page<DeadLetterQueueEntity>> getFailedEvents(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(dlqService.getFailedByTenant(tenantId, pageable));
    }

    /**
     * Get failed events for a patient
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<DeadLetterQueueEntity>> getFailedEventsByPatient(
            @PathVariable String patientId) {
        return ResponseEntity.ok(dlqService.getFailedByPatient(patientId));
    }

    /**
     * Get failed events by topic
     */
    @GetMapping("/topic/{topic}")
    public ResponseEntity<List<DeadLetterQueueEntity>> getFailedEventsByTopic(
            @PathVariable String topic) {
        return ResponseEntity.ok(dlqService.getFailedByTopic(topic));
    }

    /**
     * Get exhausted events (need manual intervention)
     */
    @GetMapping("/exhausted")
    public ResponseEntity<List<DeadLetterQueueEntity>> getExhausted() {
        return ResponseEntity.ok(dlqService.getExhausted());
    }

    /**
     * Get recent failures
     */
    @GetMapping("/recent")
    public ResponseEntity<List<DeadLetterQueueEntity>> getRecentFailures(
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(dlqService.getRecentFailures(hours));
    }

    /**
     * Get DLQ statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<DLQStats> getStats(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(dlqService.getStats(tenantId));
    }

    /**
     * Manually retry a failed event
     */
    @PostMapping("/{dlqId}/retry")
    public ResponseEntity<Void> retryEvent(@PathVariable UUID dlqId) {
        dlqService.markForRetry(dlqId);
        return ResponseEntity.accepted().build();
    }

    /**
     * Mark event as resolved
     */
    @PostMapping("/{dlqId}/resolve")
    public ResponseEntity<Void> resolveEvent(
            @PathVariable UUID dlqId,
            @RequestBody ResolveRequest request) {
        dlqService.markAsResolved(dlqId, request.resolvedBy, request.notes);
        return ResponseEntity.ok().build();
    }

    /**
     * Mark event as exhausted (manual intervention needed)
     */
    @PostMapping("/{dlqId}/exhaust")
    public ResponseEntity<Void> exhaustEvent(@PathVariable UUID dlqId) {
        dlqService.markAsExhausted(dlqId);
        return ResponseEntity.ok().build();
    }

    public record ResolveRequest(String resolvedBy, String notes) {}
}
