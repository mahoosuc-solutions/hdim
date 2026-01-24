package com.healthdata.auditquery.controller;

import com.healthdata.audit.entity.ai.AIAgentDecisionEventEntity;
import com.healthdata.audit.repository.ai.AIAgentDecisionEventRepository;
import com.healthdata.auditquery.dto.AIAuditEventDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * REST controller for streaming AI audit events in real-time using Server-Sent Events (SSE).
 *
 * <p>Provides:
 * <ul>
 *   <li>Real-time streaming of AI decision events</li>
 *   <li>Event filtering by agent type and severity</li>
 *   <li>Automatic client reconnection support</li>
 *   <li>Multi-tenant isolation</li>
 * </ul>
 *
 * <p>SSE Protocol:
 * <ul>
 *   <li>One-way communication (server → client)</li>
 *   <li>Auto-reconnect on connection loss</li>
 *   <li>Works through firewalls/proxies</li>
 *   <li>Standard HTTP (no WebSocket upgrade)</li>
 * </ul>
 *
 * <p>Security:
 * <ul>
 *   <li>Requires AUDITOR or ADMIN role</li>
 *   <li>Gateway trust authentication (X-Auth-* headers)</li>
 *   <li>Tenant isolation enforced</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/audit/ai")
@Tag(name = "AI Audit Stream", description = "Real-time AI audit event streaming via SSE")
@SecurityRequirement(name = "gateway-trust-auth")
public class AIAuditStreamController {

    private final AIAgentDecisionEventRepository aiDecisionRepository;
    private final ThreadPoolTaskExecutor taskExecutor;

    public AIAuditStreamController(
            AIAgentDecisionEventRepository aiDecisionRepository,
            @Qualifier("sseTaskExecutor") ThreadPoolTaskExecutor taskExecutor) {
        this.aiDecisionRepository = aiDecisionRepository;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Active SSE connections by tenant ID.
     * Each tenant can have multiple concurrent connections (different browser tabs, users).
     */
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> activeEmitters = new ConcurrentHashMap<>();

    /**
     * Default timeout for SSE connections: 5 minutes
     * After timeout, client will auto-reconnect
     */
    private static final long SSE_TIMEOUT_MS = 300_000L; // 5 minutes

    /**
     * Polling interval for checking new events: 2 seconds
     */
    private static final long POLLING_INTERVAL_MS = 2000L;

    /**
     * Stream AI audit events in real-time.
     *
     * <p>Endpoint: GET /api/v1/audit/ai/stream
     *
     * <p>Query Parameters:
     * <ul>
     *   <li>agentType: Filter by AI agent type (e.g., CLINICAL_DECISION, POOL_OPTIMIZER)</li>
     *   <li>severity: Filter by severity (HIGH, MEDIUM, LOW)</li>
     * </ul>
     *
     * <p>Event Format:
     * <pre>
     * event: AI_DECISION
     * id: event-id-123
     * data: {"eventId":"123","agentType":"CLINICAL_DECISION",...}
     * </pre>
     *
     * @param tenantId Tenant identifier (from X-Tenant-ID header)
     * @param agentType Optional agent type filter
     * @param severity Optional severity filter
     * @return SseEmitter for streaming events
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasPermission('AUDIT_READ')")
    @Operation(
        summary = "Stream AI audit events in real-time",
        description = "Establishes SSE connection for streaming AI decision events with optional filtering",
        responses = {
            @ApiResponse(responseCode = "200", description = "SSE stream established",
                content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                    schema = @Schema(implementation = AIAuditEventDTO.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
        }
    )
    public SseEmitter streamAIAuditEvents(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,

            @Parameter(description = "Filter by AI agent type (e.g., CLINICAL_DECISION)")
            @RequestParam(required = false) String agentType,

            @Parameter(description = "Filter by severity level (HIGH, MEDIUM, LOW)")
            @RequestParam(required = false) String severity) {

        log.info("Establishing SSE stream for tenant: {}, agentType: {}, severity: {}",
            tenantId, agentType, severity);

        // Create SSE emitter with timeout
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        // Track last event timestamp to avoid duplicates
        final Instant[] lastEventTimestamp = {Instant.now()};

        // Register emitter for this tenant
        activeEmitters.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Handle emitter completion/timeout
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed for tenant: {}", tenantId);
            removeEmitter(tenantId, emitter);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE connection timed out for tenant: {}", tenantId);
            removeEmitter(tenantId, emitter);
        });

        emitter.onError(throwable -> {
            log.error("SSE connection error for tenant: {}", tenantId, throwable);
            removeEmitter(tenantId, emitter);
        });

        // Start async polling task
        taskExecutor.submit(() -> pollAndSendEvents(emitter, tenantId, agentType, severity, lastEventTimestamp));

        return emitter;
    }

    /**
     * Poll database for new events and send to client via SSE.
     *
     * @param emitter SSE emitter
     * @param tenantId Tenant identifier
     * @param agentType Optional agent type filter
     * @param severity Optional severity filter
     * @param lastEventTimestamp Last event timestamp to avoid duplicates
     */
    private void pollAndSendEvents(
            SseEmitter emitter,
            String tenantId,
            String agentType,
            String severity,
            Instant[] lastEventTimestamp) {

        try {
            while (true) {
                // Query for new events since last timestamp
                Pageable pageable = PageRequest.of(0, 10); // Fetch up to 10 new events
                var newEvents = aiDecisionRepository.findByTenantIdAndTimestampAfter(
                    tenantId, lastEventTimestamp[0], pageable
                );

                for (AIAgentDecisionEventEntity event : newEvents.getContent()) {
                    // Apply filters if specified
                    if (agentType != null && !event.getAgentType().name().contains(agentType)) {
                        continue;
                    }

                    // TODO: Add severity filtering when severity field is added to entity

                    // Convert entity to DTO
                    AIAuditEventDTO dto = convertToDTO(event);

                    // Send event via SSE
                    emitter.send(SseEmitter.event()
                        .id(event.getEventId().toString())
                        .name("AI_DECISION")
                        .data(dto));

                    // Update last timestamp
                    if (event.getTimestamp().isAfter(lastEventTimestamp[0])) {
                        lastEventTimestamp[0] = event.getTimestamp();
                    }

                    log.debug("Sent event {} to tenant {}", event.getEventId(), tenantId);
                }

                // Sleep before next poll
                Thread.sleep(POLLING_INTERVAL_MS);
            }
        } catch (IOException e) {
            log.error("Error sending SSE event to tenant: {}", tenantId, e);
            emitter.completeWithError(e);
        } catch (InterruptedException e) {
            log.info("SSE polling interrupted for tenant: {}", tenantId);
            Thread.currentThread().interrupt();
            emitter.complete();
        } catch (Exception e) {
            log.error("Unexpected error in SSE polling for tenant: {}", tenantId, e);
            emitter.completeWithError(e);
        }
    }

    /**
     * Convert AIAgentDecisionEventEntity to DTO for SSE transmission.
     */
    private AIAuditEventDTO convertToDTO(AIAgentDecisionEventEntity entity) {
        AIAuditEventDTO dto = new AIAuditEventDTO();
        dto.setEventId(entity.getEventId().toString());
        dto.setTimestamp(entity.getTimestamp());
        dto.setAgentType(entity.getAgentType().name());
        dto.setDecisionType(entity.getDecisionType().name());
        dto.setResourceType(entity.getResourceType());
        dto.setResourceId(entity.getResourceId());

        // Build recommendation object from flattened fields
        Map<String, Object> recommendation = new java.util.HashMap<>();
        recommendation.put("configType", entity.getConfigType());
        recommendation.put("currentValue", entity.getCurrentValue());
        recommendation.put("recommendedValue", entity.getRecommendedValue());
        recommendation.put("expectedImpact", entity.getExpectedImpact());
        dto.setRecommendation(recommendation);

        dto.setConfidenceScore(entity.getConfidenceScore());
        dto.setOutcome(entity.getOutcome().name());
        dto.setCostEstimate(entity.getCostEstimate());
        dto.setProcessingTimeMs(entity.getInferenceTimeMs());
        return dto;
    }

    /**
     * Remove emitter from active connections.
     */
    private void removeEmitter(String tenantId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = activeEmitters.get(tenantId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                activeEmitters.remove(tenantId);
            }
        }
    }

    /**
     * Get count of active SSE connections (for monitoring).
     */
    @GetMapping("/stream/stats")
    @PreAuthorize("hasPermission('AUDIT_READ')")
    @Operation(summary = "Get SSE connection statistics")
    public Map<String, Object> getStreamStats() {
        int totalConnections = activeEmitters.values().stream()
            .mapToInt(CopyOnWriteArrayList::size)
            .sum();

        return Map.of(
            "activeTenants", activeEmitters.size(),
            "totalConnections", totalConnections,
            "connectionsByTenant", activeEmitters.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().size()
                ))
        );
    }
}
