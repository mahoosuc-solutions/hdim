package com.healthdata.agentvalidation.service;

import com.healthdata.agentvalidation.client.JaegerApiClient;
import com.healthdata.agentvalidation.client.dto.JaegerTraceResponse;
import com.healthdata.agentvalidation.config.ValidationProperties;
import com.healthdata.agentvalidation.domain.entity.TestExecution;
import com.healthdata.agentvalidation.domain.entity.TraceOutcome;
import com.healthdata.agentvalidation.repository.TraceOutcomeRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for fetching and correlating Jaeger traces with test executions.
 * Enables trace-based debugging for failed tests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JaegerTraceService {

    private final JaegerApiClient jaegerApiClient;
    private final TraceOutcomeRepository traceOutcomeRepository;
    private final ValidationProperties validationProperties;
    private final MeterRegistry meterRegistry;

    /**
     * Fetch and correlate a trace with a test execution.
     */
    @Transactional
    public TraceOutcome correlateTrace(TestExecution execution) {
        if (execution.getTraceId() == null) {
            log.warn("No trace ID for execution {}", execution.getId());
            return null;
        }

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            log.debug("Fetching trace {} for execution {}", execution.getTraceId(), execution.getId());

            JaegerTraceResponse traceResponse = jaegerApiClient.getTrace(execution.getTraceId());

            if (traceResponse == null || traceResponse.getData() == null || traceResponse.getData().isEmpty()) {
                log.warn("No trace data found for trace ID {}", execution.getTraceId());
                return null;
            }

            TraceOutcome outcome = buildTraceOutcome(execution, traceResponse);
            traceOutcomeRepository.save(outcome);

            log.info("Correlated trace {} with execution {}: {} tools, {} LLM calls, {} errors",
                execution.getTraceId(), execution.getId(),
                outcome.getToolInvocationCount(), outcome.getLlmCallCount(), outcome.getErrorCount());

            return outcome;

        } catch (Exception e) {
            log.error("Error fetching trace {}: {}", execution.getTraceId(), e.getMessage(), e);
            return null;
        } finally {
            sample.stop(Timer.builder("agent.validation.jaeger.fetch")
                .description("Jaeger trace fetch duration")
                .register(meterRegistry));
        }
    }

    /**
     * Fetch a trace by ID without correlation.
     */
    public Optional<JaegerTraceResponse> fetchTrace(String traceId) {
        try {
            JaegerTraceResponse response = jaegerApiClient.getTrace(traceId);
            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                return Optional.of(response);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching trace {}: {}", traceId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get trace outcome by trace ID.
     */
    public Optional<TraceOutcome> getTraceOutcome(String traceId) {
        return traceOutcomeRepository.findByTraceId(traceId);
    }

    /**
     * Get trace outcome by execution ID.
     */
    public Optional<TraceOutcome> getTraceOutcomeByExecution(TestExecution execution) {
        return traceOutcomeRepository.findByTestExecutionId(execution.getId());
    }

    /**
     * Build TraceOutcome from Jaeger response.
     */
    private TraceOutcome buildTraceOutcome(TestExecution execution, JaegerTraceResponse traceResponse) {
        JaegerTraceResponse.TraceData traceData = traceResponse.getData().get(0);

        List<JaegerTraceResponse.Span> spans = traceData.getSpans();
        Map<String, JaegerTraceResponse.Process> processes = traceData.getProcesses();

        // Extract tool invocations
        List<TraceOutcome.ToolInvocation> toolInvocations = extractToolInvocations(spans);

        // Extract LLM calls
        List<TraceOutcome.LlmCall> llmCalls = extractLlmCalls(spans);

        // Extract errors
        List<TraceOutcome.TraceError> errors = extractErrors(spans);

        // Build span summary
        List<TraceOutcome.SpanInfo> spanSummary = spans.stream()
            .limit(50) // Limit summary to first 50 spans
            .map(span -> TraceOutcome.SpanInfo.builder()
                .spanId(span.getSpanID())
                .operationName(span.getOperationName())
                .serviceName(getServiceName(span.getProcessID(), processes))
                .durationMs(span.getDuration() != null ? span.getDuration() / 1000 : null)
                .hasError(hasError(span))
                .tags(extractTagsAsMap(span.getTags()))
                .build())
            .collect(Collectors.toList());

        String jaegerUrl = String.format("%s/trace/%s",
            validationProperties.getJaeger().getApiUrl().replace("/api", ""),
            execution.getTraceId());

        return TraceOutcome.builder()
            .traceId(execution.getTraceId())
            .testExecution(execution)
            .totalDurationMs(traceResponse.getTotalDurationMs())
            .errorCount(errors.size())
            .toolInvocationCount(toolInvocations.size())
            .llmCallCount(llmCalls.size())
            .evaluationPassed(execution.isPassed())
            .evaluationScore(execution.getEvaluationScore())
            .spanSummary(spanSummary)
            .toolInvocations(toolInvocations)
            .llmCalls(llmCalls)
            .traceErrors(errors)
            .jaegerUrl(jaegerUrl)
            .build();
    }

    /**
     * Extract tool invocation spans.
     */
    private List<TraceOutcome.ToolInvocation> extractToolInvocations(List<JaegerTraceResponse.Span> spans) {
        return spans.stream()
            .filter(span -> span.getOperationName() != null &&
                          (span.getOperationName().startsWith("tool.") ||
                           span.getOperationName().contains("execute_tool")))
            .map(span -> TraceOutcome.ToolInvocation.builder()
                .toolName(extractToolName(span))
                .durationMs(span.getDuration() != null ? span.getDuration() / 1000 : null)
                .success(!hasError(span))
                .inputSummary(extractTagValue(span.getTags(), "tool.input"))
                .outputSummary(extractTagValue(span.getTags(), "tool.output"))
                .parameters(extractTagsAsObjectMap(span.getTags()))
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Extract LLM call spans.
     */
    private List<TraceOutcome.LlmCall> extractLlmCalls(List<JaegerTraceResponse.Span> spans) {
        return spans.stream()
            .filter(span -> span.getOperationName() != null &&
                          (span.getOperationName().startsWith("llm.") ||
                           span.getOperationName().contains("llm_call") ||
                           span.getOperationName().contains("claude") ||
                           span.getOperationName().contains("openai") ||
                           span.getOperationName().contains("bedrock")))
            .map(span -> TraceOutcome.LlmCall.builder()
                .provider(extractTagValue(span.getTags(), "llm.provider"))
                .model(extractTagValue(span.getTags(), "llm.model"))
                .durationMs(span.getDuration() != null ? span.getDuration() / 1000 : null)
                .inputTokens(extractTagValueAsInt(span.getTags(), "llm.input_tokens"))
                .outputTokens(extractTagValueAsInt(span.getTags(), "llm.output_tokens"))
                .success(!hasError(span))
                .errorMessage(extractTagValue(span.getTags(), "error.message"))
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Extract error spans.
     */
    private List<TraceOutcome.TraceError> extractErrors(List<JaegerTraceResponse.Span> spans) {
        return spans.stream()
            .filter(this::hasError)
            .map(span -> TraceOutcome.TraceError.builder()
                .spanId(span.getSpanID())
                .operationName(span.getOperationName())
                .errorType(extractTagValue(span.getTags(), "error.type"))
                .errorMessage(extractTagValue(span.getTags(), "error.message"))
                .stackTrace(extractStackTrace(span.getLogs()))
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Check if a span has an error.
     */
    private boolean hasError(JaegerTraceResponse.Span span) {
        if (span.getTags() == null) return false;
        return span.getTags().stream()
            .anyMatch(tag -> "error".equals(tag.getKey()) && Boolean.TRUE.equals(tag.getValue()));
    }

    /**
     * Get service name from process ID.
     */
    private String getServiceName(String processId, Map<String, JaegerTraceResponse.Process> processes) {
        if (processId == null || processes == null) return null;
        JaegerTraceResponse.Process process = processes.get(processId);
        return process != null ? process.getServiceName() : null;
    }

    /**
     * Extract tool name from span.
     */
    private String extractToolName(JaegerTraceResponse.Span span) {
        String opName = span.getOperationName();
        if (opName.startsWith("tool.")) {
            return opName.substring(5);
        }
        String toolNameTag = extractTagValue(span.getTags(), "tool.name");
        if (toolNameTag != null) {
            return toolNameTag;
        }
        return opName;
    }

    /**
     * Extract a tag value by key.
     */
    private String extractTagValue(List<JaegerTraceResponse.Tag> tags, String key) {
        if (tags == null) return null;
        return tags.stream()
            .filter(tag -> key.equals(tag.getKey()))
            .map(tag -> String.valueOf(tag.getValue()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Extract a tag value as integer.
     */
    private Integer extractTagValueAsInt(List<JaegerTraceResponse.Tag> tags, String key) {
        String value = extractTagValue(tags, key);
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Extract tags as a string map.
     */
    private Map<String, String> extractTagsAsMap(List<JaegerTraceResponse.Tag> tags) {
        if (tags == null) return Map.of();
        return tags.stream()
            .collect(Collectors.toMap(
                JaegerTraceResponse.Tag::getKey,
                tag -> String.valueOf(tag.getValue()),
                (v1, v2) -> v1
            ));
    }

    /**
     * Extract tags as an object map.
     */
    private Map<String, Object> extractTagsAsObjectMap(List<JaegerTraceResponse.Tag> tags) {
        if (tags == null) return Map.of();
        return tags.stream()
            .collect(Collectors.toMap(
                JaegerTraceResponse.Tag::getKey,
                JaegerTraceResponse.Tag::getValue,
                (v1, v2) -> v1
            ));
    }

    /**
     * Extract stack trace from logs.
     */
    private String extractStackTrace(List<JaegerTraceResponse.Log> logs) {
        if (logs == null) return null;
        for (JaegerTraceResponse.Log log : logs) {
            if (log.getFields() != null) {
                for (JaegerTraceResponse.Field field : log.getFields()) {
                    if ("stack".equals(field.getKey()) || "error.stack".equals(field.getKey())) {
                        return String.valueOf(field.getValue());
                    }
                }
            }
        }
        return null;
    }
}
