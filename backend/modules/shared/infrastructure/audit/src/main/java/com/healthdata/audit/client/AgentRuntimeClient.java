package com.healthdata.audit.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Client for communicating with the Agent Runtime Service.
 * 
 * Used by Decision Replay Service to re-execute AI agent decisions.
 * 
 * This client is optional - if RestTemplate is not available, Decision Replay Service
 * will fall back to validation replay mode.
 */
@Slf4j
@Component
@ConditionalOnBean(RestTemplate.class)
public class AgentRuntimeClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${hdim.agent-runtime.url:http://agent-runtime-service:8080}")
    private String agentRuntimeUrl;

    @Autowired(required = false)
    public AgentRuntimeClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Execute an agent with the given request.
     * 
     * @param agentSlug Agent identifier (e.g., "clinical-decision", "care-gap-optimizer")
     * @param request Request payload containing message and parameters
     * @param tenantId Tenant ID for multi-tenant context
     * @return Response from agent execution
     */
    public AgentExecutionResponse executeAgent(String agentSlug, Map<String, Object> request, String tenantId) {
        try {
            String url = agentRuntimeUrl + "/api/v1/agents/" + agentSlug + "/execute";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-ID", tenantId);
            
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                httpEntity,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseResponse(response.getBody());
            } else {
                log.warn("Agent execution returned non-2xx status: {}", response.getStatusCode());
                return AgentExecutionResponse.failure("Agent execution failed: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error executing agent {}: {}", agentSlug, e.getMessage(), e);
            return AgentExecutionResponse.failure("Agent execution error: " + e.getMessage());
        }
    }

    /**
     * Parse agent response from Map to structured response object.
     */
    private AgentExecutionResponse parseResponse(Map<String, Object> responseBody) {
        try {
            Boolean success = (Boolean) responseBody.get("success");
            String content = (String) responseBody.get("content");
            String error = (String) responseBody.get("error");
            String model = (String) responseBody.get("model");
            
            // Extract token usage if available
            Map<String, Object> usageMap = (Map<String, Object>) responseBody.get("usage");
            TokenUsage usage = null;
            if (usageMap != null) {
                usage = new TokenUsage(
                    ((Number) usageMap.getOrDefault("inputTokens", 0)).intValue(),
                    ((Number) usageMap.getOrDefault("outputTokens", 0)).intValue(),
                    ((Number) usageMap.getOrDefault("totalTokens", 0)).intValue()
                );
            }
            
            if (success != null && success) {
                return AgentExecutionResponse.success(content, usage, model);
            } else {
                return AgentExecutionResponse.failure(error != null ? error : "Unknown error");
            }
            
        } catch (Exception e) {
            log.error("Error parsing agent response: {}", e.getMessage(), e);
            return AgentExecutionResponse.failure("Failed to parse agent response: " + e.getMessage());
        }
    }

    /**
     * Agent execution response.
     */
    public static class AgentExecutionResponse {
        private final boolean success;
        private final String content;
        private final String error;
        private final TokenUsage usage;
        private final String model;

        private AgentExecutionResponse(boolean success, String content, String error, TokenUsage usage, String model) {
            this.success = success;
            this.content = content;
            this.error = error;
            this.usage = usage;
            this.model = model;
        }

        public static AgentExecutionResponse success(String content, TokenUsage usage, String model) {
            return new AgentExecutionResponse(true, content, null, usage, model);
        }

        public static AgentExecutionResponse failure(String error) {
            return new AgentExecutionResponse(false, null, error, null, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getContent() {
            return content;
        }

        public String getError() {
            return error;
        }

        public TokenUsage getUsage() {
            return usage;
        }

        public String getModel() {
            return model;
        }
    }

    /**
     * Token usage information.
     */
    public static class TokenUsage {
        private final int inputTokens;
        private final int outputTokens;
        private final int totalTokens;

        public TokenUsage(int inputTokens, int outputTokens, int totalTokens) {
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.totalTokens = totalTokens;
        }

        public int getInputTokens() {
            return inputTokens;
        }

        public int getOutputTokens() {
            return outputTokens;
        }

        public int getTotalTokens() {
            return totalTokens;
        }
    }
}
