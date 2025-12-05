package com.healthdata.aiassistant.controller;

import com.healthdata.aiassistant.config.ClaudeConfig;
import com.healthdata.aiassistant.dto.ChatRequest;
import com.healthdata.aiassistant.dto.ChatResponse;
import com.healthdata.aiassistant.service.ClaudeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

/**
 * REST controller for AI Clinical Assistant.
 *
 * Provides endpoints for:
 * - Natural language clinical queries
 * - Patient summary generation
 * - Care gap analysis
 * - Quality measure interpretation
 *
 * Security:
 * - All endpoints require authentication
 * - Tenant isolation enforced
 * - All interactions audited
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final Optional<ClaudeService> claudeService;
    private final ClaudeConfig claudeConfig;

    /**
     * Send a chat message to the AI assistant.
     *
     * @param request Chat request with query and context
     * @param authentication Current user authentication
     * @return AI-generated response
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
        @Valid @RequestBody ChatRequest request,
        Authentication authentication
    ) {
        log.info("AI chat request: type={}, user={}",
            request.getQueryType(),
            authentication != null ? authentication.getName() : "anonymous");

        // Check if AI is enabled
        if (claudeService.isEmpty() || !claudeConfig.isEnabled()) {
            return ResponseEntity.ok(ChatResponse.builder()
                .response("AI assistant is not enabled. Please contact your administrator.")
                .error(true)
                .errorMessage("AI service disabled")
                .build());
        }

        // Validate query type
        if (!claudeConfig.isQueryTypeAllowed(request.getQueryType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Query type not allowed: " + request.getQueryType());
        }

        // Send to Claude
        long startTime = System.currentTimeMillis();
        ChatResponse response = claudeService.get().chat(request);
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);

        log.info("AI chat response: tokens={}/{}, time={}ms",
            response.getInputTokens(),
            response.getOutputTokens(),
            response.getProcessingTimeMs());

        return ResponseEntity.ok(response);
    }

    /**
     * Generate a patient summary.
     *
     * @param patientId Patient ID
     * @param patientData Patient data (JSON)
     * @param authentication Current user
     * @return AI-generated patient summary
     */
    @PostMapping("/patient-summary/{patientId}")
    public ResponseEntity<ChatResponse> generatePatientSummary(
        @PathVariable String patientId,
        @RequestBody String patientData,
        Authentication authentication
    ) {
        log.info("Patient summary request: patientId={}, user={}",
            patientId,
            authentication != null ? authentication.getName() : "anonymous");

        if (claudeService.isEmpty() || !claudeConfig.isEnabled()) {
            return ResponseEntity.ok(ChatResponse.builder()
                .response("AI assistant is not enabled.")
                .error(true)
                .errorMessage("AI service disabled")
                .build());
        }

        ChatResponse response = claudeService.get().generatePatientSummary(patientId, patientData);
        return ResponseEntity.ok(response);
    }

    /**
     * Analyze care gaps.
     *
     * @param gapData Care gap data (JSON)
     * @param authentication Current user
     * @return AI-generated care gap analysis
     */
    @PostMapping("/care-gaps/analyze")
    public ResponseEntity<ChatResponse> analyzeCareGaps(
        @RequestBody String gapData,
        Authentication authentication
    ) {
        log.info("Care gap analysis request: user={}",
            authentication != null ? authentication.getName() : "anonymous");

        if (claudeService.isEmpty() || !claudeConfig.isEnabled()) {
            return ResponseEntity.ok(ChatResponse.builder()
                .response("AI assistant is not enabled.")
                .error(true)
                .errorMessage("AI service disabled")
                .build());
        }

        ChatResponse response = claudeService.get().analyzeCareGaps(gapData);
        return ResponseEntity.ok(response);
    }

    /**
     * Answer a clinical question.
     *
     * @param query The clinical question
     * @param context Additional context data
     * @param authentication Current user
     * @return AI-generated answer
     */
    @GetMapping("/query")
    public ResponseEntity<ChatResponse> answerQuery(
        @RequestParam String query,
        @RequestParam(required = false) String context,
        Authentication authentication
    ) {
        log.info("Clinical query: query='{}', user={}",
            query.length() > 50 ? query.substring(0, 50) + "..." : query,
            authentication != null ? authentication.getName() : "anonymous");

        if (claudeService.isEmpty() || !claudeConfig.isEnabled()) {
            return ResponseEntity.ok(ChatResponse.builder()
                .response("AI assistant is not enabled.")
                .error(true)
                .errorMessage("AI service disabled")
                .build());
        }

        ChatResponse response = claudeService.get().answerClinicalQuery(
            query,
            context != null ? context : ""
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get AI assistant status and capabilities.
     *
     * @return Status information
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "enabled", claudeConfig.isEnabled(),
            "model", claudeConfig.getModel(),
            "allowedQueryTypes", claudeConfig.getAllowedQueryTypes(),
            "cachingEnabled", claudeConfig.isCachingEnabled(),
            "rateLimitPerMinute", claudeConfig.getRateLimitPerMinute()
        ));
    }

    /**
     * Health check endpoint.
     *
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        boolean healthy = claudeService.isPresent() && claudeConfig.isEnabled();

        return ResponseEntity.ok(Map.of(
            "status", healthy ? "UP" : "DOWN",
            "aiEnabled", claudeConfig.isEnabled(),
            "serviceAvailable", claudeService.isPresent()
        ));
    }
}
