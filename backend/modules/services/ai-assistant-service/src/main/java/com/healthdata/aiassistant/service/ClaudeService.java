package com.healthdata.aiassistant.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.aiassistant.config.ClaudeConfig;
import com.healthdata.aiassistant.dto.ChatMessage;
import com.healthdata.aiassistant.dto.ChatRequest;
import com.healthdata.aiassistant.dto.ChatResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for interacting with Anthropic Claude API.
 *
 * Features:
 * - Natural language query processing
 * - Clinical context-aware responses
 * - Response caching
 * - Rate limiting
 * - Circuit breaker for resilience
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "claude", name = "enabled", havingValue = "true")
public class ClaudeService {

    private final ClaudeConfig claudeConfig;
    private final ObjectMapper objectMapper;

    private WebClient webClient;

    /**
     * Initialize WebClient on startup.
     */
    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
            .baseUrl(claudeConfig.getApiUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("x-api-key", claudeConfig.getApiKey())
            .defaultHeader("anthropic-version", "2023-06-01")
            .build();

        log.info("Claude WebClient initialized for model: {}", claudeConfig.getModel());
    }

    /**
     * Send a chat request to Claude.
     *
     * @param request Chat request with messages
     * @return Chat response from Claude
     */
    @CircuitBreaker(name = "claude", fallbackMethod = "chatFallback")
    @RateLimiter(name = "claude")
    public ChatResponse chat(ChatRequest request) {
        log.debug("Sending chat request to Claude: {}", request.getQueryType());

        // Build API request
        ClaudeApiRequest apiRequest = buildApiRequest(request);

        try {
            ClaudeApiResponse response = webClient.post()
                .uri("/messages")
                .bodyValue(apiRequest)
                .retrieve()
                .bodyToMono(ClaudeApiResponse.class)
                .timeout(Duration.ofSeconds(claudeConfig.getTimeoutSeconds()))
                .block();

            if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
                log.warn("Empty response from Claude API");
                return buildErrorResponse("Empty response from AI service");
            }

            // Extract text from response
            String responseText = response.getContent().stream()
                .filter(c -> "text".equals(c.getType()))
                .map(ClaudeApiResponse.ContentBlock::getText)
                .findFirst()
                .orElse("No response generated");

            log.debug("Received response from Claude (tokens: input={}, output={})",
                response.getUsage() != null ? response.getUsage().getInputTokens() : 0,
                response.getUsage() != null ? response.getUsage().getOutputTokens() : 0);

            return ChatResponse.builder()
                .id(UUID.randomUUID().toString())
                .queryType(request.getQueryType())
                .response(responseText)
                .model(claudeConfig.getModel())
                .inputTokens(response.getUsage() != null ? response.getUsage().getInputTokens() : 0)
                .outputTokens(response.getUsage() != null ? response.getUsage().getOutputTokens() : 0)
                .cached(false)
                .build();

        } catch (Exception e) {
            log.error("Error calling Claude API", e);
            return buildErrorResponse("AI service unavailable: " + e.getMessage());
        }
    }

    /**
     * Send a cached chat request (for common queries).
     */
    @Cacheable(value = "claude-responses", key = "#request.cacheKey()")
    public ChatResponse chatCached(ChatRequest request) {
        return chat(request);
    }

    /**
     * Fallback method when Claude is unavailable.
     */
    public ChatResponse chatFallback(ChatRequest request, Exception e) {
        log.warn("Claude circuit breaker triggered, using fallback: {}", e.getMessage());
        return ChatResponse.builder()
            .id(UUID.randomUUID().toString())
            .queryType(request.getQueryType())
            .response("AI assistant is temporarily unavailable. Please try again later or use the dashboard for direct data access.")
            .model("fallback")
            .error(true)
            .errorMessage("Service temporarily unavailable")
            .build();
    }

    /**
     * Generate a clinical summary for a patient.
     */
    public ChatResponse generatePatientSummary(String patientId, String patientData) {
        return generatePatientSummary(patientId, patientData, "unknown-tenant");
    }

    public ChatResponse generatePatientSummary(String patientId, String patientData, String tenantId) {
        String prompt = String.format("""
            Generate a concise clinical summary for the patient with the following data.
            Focus on:
            1. Active care gaps and priority
            2. Quality measure compliance status
            3. Key health concerns
            4. Recommended actions

            Patient ID: %s

            Patient Data:
            %s

            Provide a structured summary suitable for clinical review.
            """, patientId, patientData);

        ChatRequest request = ChatRequest.builder()
            .queryType("patient_summary")
            .query(prompt)
            .tenantId(tenantId)
            .build();

        return chat(request);
    }

    /**
     * Analyze care gaps for a patient panel.
     */
    public ChatResponse analyzeCareGaps(String gapData) {
        return analyzeCareGaps(gapData, "unknown-tenant");
    }

    public ChatResponse analyzeCareGaps(String gapData, String tenantId) {
        String prompt = String.format("""
            Analyze the following care gap data and provide insights:
            1. Prioritize gaps by clinical urgency
            2. Identify patterns across the patient panel
            3. Suggest outreach strategies
            4. Estimate potential quality score improvement

            Care Gap Data:
            %s

            Provide actionable recommendations for the care team.
            """, gapData);

        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query(prompt)
            .tenantId(tenantId)
            .build();

        return chat(request);
    }

    /**
     * Answer a natural language clinical query.
     */
    public ChatResponse answerClinicalQuery(String query, String context) {
        return answerClinicalQuery(query, context, "unknown-tenant");
    }

    public ChatResponse answerClinicalQuery(String query, String context, String tenantId) {
        String prompt = String.format("""
            Answer the following clinical query using the provided context.

            Query: %s

            Available Context:
            %s

            Provide a clear, accurate answer based on the data. If the information is not available in the context, say so.
            """, query, context);

        ChatRequest request = ChatRequest.builder()
            .queryType("quality_measures")
            .query(prompt)
            .tenantId(tenantId)
            .build();

        return chat(request);
    }

    /**
     * Build Claude API request from chat request.
     */
    private ClaudeApiRequest buildApiRequest(ChatRequest request) {
        ClaudeApiRequest apiRequest = new ClaudeApiRequest();
        apiRequest.setModel(claudeConfig.getModel());
        apiRequest.setMaxTokens(claudeConfig.getMaxTokens());
        apiRequest.setTemperature(claudeConfig.getTemperature());
        apiRequest.setSystem(claudeConfig.getSystemPrompt());

        List<ClaudeApiRequest.Message> messages = new ArrayList<>();

        // Add conversation history
        if (request.getMessages() != null) {
            for (ChatMessage msg : request.getMessages()) {
                ClaudeApiRequest.Message apiMsg = new ClaudeApiRequest.Message();
                apiMsg.setRole(msg.getRole());
                apiMsg.setContent(msg.getContent());
                messages.add(apiMsg);
            }
        }

        // Add current query
        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            ClaudeApiRequest.Message userMsg = new ClaudeApiRequest.Message();
            userMsg.setRole("user");
            userMsg.setContent(request.getQuery());
            messages.add(userMsg);
        }

        apiRequest.setMessages(messages);

        return apiRequest;
    }

    /**
     * Build error response.
     */
    private ChatResponse buildErrorResponse(String errorMessage) {
        return ChatResponse.builder()
            .id(UUID.randomUUID().toString())
            .response("Unable to process request: " + errorMessage)
            .error(true)
            .errorMessage(errorMessage)
            .build();
    }

    // ========== Claude API Request/Response DTOs ==========

    @Data
    public static class ClaudeApiRequest {
        private String model;

        @JsonProperty("max_tokens")
        private int maxTokens;

        private double temperature;
        private String system;
        private List<Message> messages;

        @Data
        public static class Message {
            private String role;
            private String content;
        }
    }

    @Data
    public static class ClaudeApiResponse {
        private String id;
        private String type;
        private String role;
        private List<ContentBlock> content;
        private String model;

        @JsonProperty("stop_reason")
        private String stopReason;

        @JsonProperty("stop_sequence")
        private String stopSequence;

        private Usage usage;

        @Data
        public static class ContentBlock {
            private String type;
            private String text;
        }

        @Data
        public static class Usage {
            @JsonProperty("input_tokens")
            private int inputTokens;

            @JsonProperty("output_tokens")
            private int outputTokens;
        }
    }
}
