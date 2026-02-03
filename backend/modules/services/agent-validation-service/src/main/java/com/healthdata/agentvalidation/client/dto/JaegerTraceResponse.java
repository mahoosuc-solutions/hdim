package com.healthdata.agentvalidation.client.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for Jaeger trace data.
 * Matches the Jaeger REST API response format.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JaegerTraceResponse {

    private List<TraceData> data;
    private int total;
    private int limit;
    private int offset;
    private List<String> errors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TraceData {
        private String traceID;
        private List<Span> spans;
        private Map<String, Process> processes;
        private List<String> warnings;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Span {
        private String traceID;
        private String spanID;
        private String operationName;
        private List<Reference> references;
        private Long startTime; // Unix microseconds
        private Long duration; // Microseconds
        private List<Tag> tags;
        private List<Log> logs;
        private String processID;
        private List<String> warnings;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Reference {
        private String refType; // CHILD_OF or FOLLOWS_FROM
        private String traceID;
        private String spanID;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Tag {
        private String key;
        private String type; // string, bool, int64, float64, binary
        private Object value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Log {
        private Long timestamp;
        private List<Field> fields;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Field {
        private String key;
        private String type;
        private Object value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Process {
        private String serviceName;
        private List<Tag> tags;
    }

    /**
     * Check if the trace has any errors.
     */
    public boolean hasErrors() {
        if (data == null || data.isEmpty()) return false;
        return data.stream()
            .flatMap(td -> td.getSpans().stream())
            .anyMatch(span -> span.getTags().stream()
                .anyMatch(tag -> "error".equals(tag.getKey()) && Boolean.TRUE.equals(tag.getValue())));
    }

    /**
     * Get total duration in milliseconds.
     */
    public Long getTotalDurationMs() {
        if (data == null || data.isEmpty()) return null;
        return data.stream()
            .flatMap(td -> td.getSpans().stream())
            .mapToLong(span -> span.getDuration() != null ? span.getDuration() / 1000 : 0)
            .max()
            .orElse(0L);
    }

    /**
     * Count spans matching an operation name pattern.
     */
    public int countSpansMatching(String operationPattern) {
        if (data == null || data.isEmpty()) return 0;
        return (int) data.stream()
            .flatMap(td -> td.getSpans().stream())
            .filter(span -> span.getOperationName() != null &&
                          span.getOperationName().contains(operationPattern))
            .count();
    }

    /**
     * Get spans with errors.
     */
    public List<Span> getErrorSpans() {
        if (data == null || data.isEmpty()) return List.of();
        return data.stream()
            .flatMap(td -> td.getSpans().stream())
            .filter(span -> span.getTags().stream()
                .anyMatch(tag -> "error".equals(tag.getKey()) && Boolean.TRUE.equals(tag.getValue())))
            .toList();
    }
}
