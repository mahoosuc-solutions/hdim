package com.healthdata.agentbuilder.service;

import com.healthdata.agentbuilder.client.AgentRuntimeClient;
import com.healthdata.agentbuilder.domain.entity.AgentConfiguration;
import com.healthdata.agentbuilder.domain.entity.AgentTestSession;
import com.healthdata.agentbuilder.domain.entity.AgentTestSession.*;
import com.healthdata.agentbuilder.repository.AgentTestSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for agent testing in sandbox environment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentTestService {

    private final AgentTestSessionRepository testRepository;
    private final AgentConfigurationService agentService;
    private final AgentRuntimeClient runtimeClient;

    /**
     * Start a new test session.
     */
    @Transactional
    public AgentTestSession startTestSession(String tenantId, UUID agentId, TestType testType, String scenario, String userId) {
        AgentConfiguration agent = agentService.getByIdOrThrow(tenantId, agentId);

        AgentTestSession session = AgentTestSession.builder()
            .agentConfiguration(agent)
            .tenantId(tenantId)
            .testType(testType)
            .testScenario(scenario)
            .status(TestStatus.IN_PROGRESS)
            .messages(new ArrayList<>())
            .toolInvocations(new ArrayList<>())
            .createdBy(userId)
            .build();

        AgentTestSession saved = testRepository.save(session);
        log.info("Started test session {} for agent {}", saved.getId(), agentId);
        return saved;
    }

    /**
     * Send a test message and get response (synchronous).
     */
    @Transactional
    public TestMessageResult sendTestMessage(String tenantId, UUID sessionId, String message) {
        return sendTestMessageInternal(tenantId, sessionId, message);
    }

    /**
     * Send a test message asynchronously for non-blocking operations.
     * Uses dedicated thread pool to prevent blocking HTTP threads.
     */
    @Async("agentTestExecutor")
    public CompletableFuture<TestMessageResult> sendTestMessageAsync(String tenantId, UUID sessionId, String message) {
        log.debug("Async test message for session {} on thread {}", sessionId, Thread.currentThread().getName());
        return CompletableFuture.completedFuture(sendTestMessageInternal(tenantId, sessionId, message));
    }

    /**
     * Internal implementation of test message sending.
     */
    @Transactional
    protected TestMessageResult sendTestMessageInternal(String tenantId, UUID sessionId, String message) {
        AgentTestSession session = testRepository.findByIdAndTenantId(sessionId, tenantId)
            .orElseThrow(() -> new AgentConfigurationService.AgentBuilderException("Test session not found"));

        if (session.getStatus() != TestStatus.IN_PROGRESS) {
            throw new AgentConfigurationService.AgentBuilderException("Test session is not active");
        }

        // Record user message
        TestMessage userMessage = new TestMessage("user", message, Instant.now(), null);
        session.getMessages().add(userMessage);

        long startTime = System.currentTimeMillis();

        try {
            // Call agent runtime with test configuration
            AgentConfiguration agent = session.getAgentConfiguration();
            Map<String, Object> request = buildTestRequest(agent, message, tenantId);

            Map<String, Object> response = runtimeClient.executeAgent(
                agent.getSlug(),
                request,
                tenantId
            );

            long latencyMs = System.currentTimeMillis() - startTime;

            // Record assistant response
            String content = (String) response.get("content");
            TestMessage assistantMessage = new TestMessage("assistant", content, Instant.now(), latencyMs);
            session.getMessages().add(assistantMessage);

            // Record tool invocations if any
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tools = (List<Map<String, Object>>) response.get("toolInvocations");
            if (tools != null) {
                for (Map<String, Object> tool : tools) {
                    ToolInvocation invocation = new ToolInvocation(
                        (String) tool.get("name"),
                        (Map<String, Object>) tool.get("arguments"),
                        (String) tool.get("result"),
                        Boolean.TRUE.equals(tool.get("success")),
                        ((Number) tool.getOrDefault("durationMs", 0L)).longValue()
                    );
                    session.getToolInvocations().add(invocation);
                }
            }

            // Update metrics
            updateMetrics(session);

            testRepository.save(session);

            return new TestMessageResult(true, content, latencyMs, null);

        } catch (Exception e) {
            log.error("Test message failed: {}", e.getMessage(), e);

            TestMessage errorMessage = new TestMessage(
                "system",
                "Error: " + e.getMessage(),
                Instant.now(),
                System.currentTimeMillis() - startTime
            );
            session.getMessages().add(errorMessage);
            testRepository.save(session);

            return new TestMessageResult(false, null, 0L, e.getMessage());
        }
    }

    /**
     * Complete a test session.
     */
    @Transactional
    public AgentTestSession completeTestSession(String tenantId, UUID sessionId, String feedback, Integer rating) {
        AgentTestSession session = testRepository.findByIdAndTenantId(sessionId, tenantId)
            .orElseThrow(() -> new AgentConfigurationService.AgentBuilderException("Test session not found"));

        session.setStatus(TestStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        session.setTesterFeedback(feedback);
        session.setTesterRating(rating);

        updateMetrics(session);

        AgentTestSession saved = testRepository.save(session);
        log.info("Completed test session {} with rating {}", sessionId, rating);
        return saved;
    }

    /**
     * Cancel a test session.
     */
    @Transactional
    public AgentTestSession cancelTestSession(String tenantId, UUID sessionId) {
        AgentTestSession session = testRepository.findByIdAndTenantId(sessionId, tenantId)
            .orElseThrow(() -> new AgentConfigurationService.AgentBuilderException("Test session not found"));

        session.setStatus(TestStatus.CANCELLED);
        session.setCompletedAt(Instant.now());

        return testRepository.save(session);
    }

    /**
     * Get test session by ID.
     */
    @Transactional(readOnly = true)
    public Optional<AgentTestSession> getTestSession(String tenantId, UUID sessionId) {
        return testRepository.findByIdAndTenantId(sessionId, tenantId);
    }

    /**
     * Get test sessions for an agent.
     */
    @Transactional(readOnly = true)
    public Page<AgentTestSession> getTestSessions(UUID agentId, Pageable pageable) {
        return testRepository.findByAgentConfigurationId(agentId, pageable);
    }

    /**
     * Get test session statistics for an agent.
     */
    @Transactional(readOnly = true)
    public TestStatistics getTestStatistics(UUID agentId) {
        long total = testRepository.countByAgentConfigurationId(agentId);
        long completed = testRepository.countByAgentConfigurationIdAndStatus(agentId, TestStatus.COMPLETED);
        long failed = testRepository.countByAgentConfigurationIdAndStatus(agentId, TestStatus.FAILED);

        return new TestStatistics(total, completed, failed, total - completed - failed);
    }

    private Map<String, Object> buildTestRequest(AgentConfiguration agent, String message, String tenantId) {
        Map<String, Object> request = new HashMap<>();
        request.put("message", message);
        request.put("systemPrompt", agent.getSystemPrompt());
        request.put("model", agent.getModelId());
        request.put("maxTokens", agent.getMaxTokens());
        request.put("temperature", agent.getTemperature());
        request.put("sessionId", UUID.randomUUID().toString());

        // Extract enabled tools
        if (agent.getToolConfiguration() != null) {
            List<String> enabledTools = agent.getToolConfiguration().stream()
                .filter(AgentConfiguration.ToolConfig::isEnabled)
                .map(AgentConfiguration.ToolConfig::getToolName)
                .toList();
            request.put("enabledTools", enabledTools);
        }

        return request;
    }

    private void updateMetrics(AgentTestSession session) {
        int totalMessages = session.getMessages().size();
        int toolInvocations = session.getToolInvocations().size();

        long totalLatency = session.getMessages().stream()
            .filter(m -> m.getLatencyMs() != null)
            .mapToLong(TestMessage::getLatencyMs)
            .sum();

        long avgLatency = totalMessages > 0 ? totalLatency / totalMessages : 0;

        TestMetrics metrics = new TestMetrics(
            totalMessages,
            toolInvocations,
            0L, // tokens would come from runtime
            avgLatency,
            0   // guardrail triggers would come from runtime
        );

        session.setMetrics(metrics);
    }

    public record TestMessageResult(
        boolean success,
        String content,
        long latencyMs,
        String error
    ) {}

    public record TestStatistics(
        long total,
        long completed,
        long failed,
        long inProgress
    ) {}
}
