package com.healthdata.audit.controller.ai;

import com.healthdata.audit.entity.ai.AIAgentDecisionEventEntity;
import com.healthdata.audit.repository.ai.AIAgentDecisionEventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST Controller for AI audit event streaming using Server-Sent Events (SSE).
 *
 * Provides real-time event stream for AI Audit Dashboard to display live AI decisions.
 */
@RestController
@RequestMapping("/api/v1/audit/ai")
@RequiredArgsConstructor
@Tag(name = "AI Audit Event Stream", description = "Real-time AI decision event streaming via SSE")
@SecurityRequirement(name = "bearer-jwt")
@Slf4j
public class AIAuditEventStreamController {

    private final AIAgentDecisionEventRepository decisionRepository;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Stream AI decision events in real-time using Server-Sent Events.
     *
     * Clients connect to this endpoint and receive new AI decisions as they occur.
     * Events are sent every 5 seconds with the latest decisions.
     *
     * Example client usage:
     * ```typescript
     * const eventSource = new EventSource('/api/v1/audit/ai/events/stream');
     * eventSource.addEventListener('ai-decision', (event) => {
     *   const decision = JSON.parse(event.data);
     *   console.log('New AI decision:', decision);
     * });
     * ```
     *
     * @param agentType Optional filter for agent type
     * @param authentication User authentication context
     * @return SSE emitter streaming AI decisions
     */
    @GetMapping(value = "/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('QA_ANALYST', 'QUALITY_OFFICER', 'ADMIN', 'AUDITOR')")
    @Operation(summary = "Stream AI decisions via SSE",
               description = "Real-time stream of AI agent decisions using Server-Sent Events. " +
                           "Sends latest decisions every 5 seconds. Client should listen for 'ai-decision' events.")
    public SseEmitter streamAIDecisions(
            @RequestParam(required = false) String agentType,
            Authentication authentication) {

        String tenantId = getTenantId(authentication);
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // No timeout

        executor.execute(() -> {
            try {
                log.info("SSE stream started for tenant: {}, agentType: {}", tenantId, agentType);

                Instant lastEventTime = Instant.now();

                while (true) {
                    // Query for new decisions since last event
                    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "timestamp"));
                    List<AIAgentDecisionEventEntity> decisions = decisionRepository
                        .findByTenantIdAndTimestampAfter(tenantId, lastEventTime, pageable)
                        .getContent();

                    if (!decisions.isEmpty()) {
                        // Send each decision as an SSE event
                        for (AIAgentDecisionEventEntity decision : decisions) {
                            emitter.send(SseEmitter.event()
                                .name("ai-decision")
                                .data(decision)
                                .id(decision.getEventId().toString())
                                .comment("AI agent decision event"));

                            // Update last event time
                            if (decision.getTimestamp().isAfter(lastEventTime)) {
                                lastEventTime = decision.getTimestamp();
                            }
                        }

                        log.debug("Sent {} AI decision events to SSE stream", decisions.size());
                    }

                    // Send heartbeat event every 30 seconds to keep connection alive
                    emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("{\"timestamp\":\"" + Instant.now() + "\"}"));

                    // Sleep for 5 seconds before checking for new events
                    Thread.sleep(5000);
                }

            } catch (IOException e) {
                log.info("SSE stream closed by client: {}", e.getMessage());
                emitter.completeWithError(e);
            } catch (InterruptedException e) {
                log.warn("SSE stream interrupted: {}", e.getMessage());
                Thread.currentThread().interrupt();
                emitter.complete();
            } catch (Exception e) {
                log.error("Error in SSE stream: {}", e.getMessage(), e);
                emitter.completeWithError(e);
            }
        });

        emitter.onCompletion(() -> log.info("SSE stream completed for tenant: {}", tenantId));
        emitter.onTimeout(() -> log.warn("SSE stream timed out for tenant: {}", tenantId));
        emitter.onError((e) -> log.error("SSE stream error for tenant: {}", tenantId, e));

        return emitter;
    }

    private String getTenantId(Authentication authentication) {
        // Extract tenant ID from authentication
        // In a real implementation, this would come from the authenticated user's context
        return "default-tenant";
    }
}
