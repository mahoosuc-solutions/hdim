package com.healthdata.agentvalidation.controller;

import com.healthdata.agentvalidation.client.JaegerApiClient;
import com.healthdata.agentvalidation.client.dto.JaegerTraceResponse;
import com.healthdata.agentvalidation.service.JaegerTraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Trace analysis endpoints backed by Jaeger data.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Trace Analysis", description = "Distributed tracing and span analysis endpoints")
public class TraceAnalysisController {

    private final JaegerTraceService jaegerTraceService;
    private final JaegerApiClient jaegerApiClient;

    @GetMapping("/traces/{traceId}")
    @Operation(summary = "Get trace details by trace ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<JaegerTraceResponse.TraceData> getTrace(@PathVariable String traceId) {
        Optional<JaegerTraceResponse> trace = jaegerTraceService.fetchTrace(traceId);
        return trace
            .map(response -> ResponseEntity.ok(response.getData().get(0)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/traces/{traceId}/critical-path")
    @Operation(summary = "Get critical path (slowest span lineage) for a trace")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<CriticalPathResponse> getCriticalPath(@PathVariable String traceId) {
        Optional<JaegerTraceResponse> trace = jaegerTraceService.fetchTrace(traceId);
        if (trace.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        JaegerTraceResponse.TraceData traceData = trace.get().getData().get(0);
        if (traceData.getSpans() == null || traceData.getSpans().isEmpty()) {
            return ResponseEntity.ok(new CriticalPathResponse(traceId, List.of(), 0L));
        }

        Map<String, JaegerTraceResponse.Span> spansById = new HashMap<>();
        for (JaegerTraceResponse.Span span : traceData.getSpans()) {
            spansById.put(span.getSpanID(), span);
        }

        JaegerTraceResponse.Span maxSpan = traceData.getSpans().stream()
            .max(Comparator.comparingLong(span -> span.getDuration() != null ? span.getDuration() : 0L))
            .orElse(null);

        if (maxSpan == null) {
            return ResponseEntity.ok(new CriticalPathResponse(traceId, List.of(), 0L));
        }

        List<SpanSummary> lineage = buildLineage(maxSpan, spansById, traceData.getProcesses());
        long totalMs = lineage.stream().mapToLong(SpanSummary::durationMs).sum();
        return ResponseEntity.ok(new CriticalPathResponse(traceId, lineage, totalMs));
    }

    @GetMapping("/spans/slow")
    @Operation(summary = "Get slow spans by service and threshold")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<List<SlowSpanResponse>> getSlowSpans(
        @RequestParam("service") String service,
        @RequestParam(value = "threshold", defaultValue = "100") long thresholdMs,
        @RequestParam(value = "hours", defaultValue = "24") int hours,
        @RequestParam(value = "limit", defaultValue = "100") int limit
    ) {
        long endMicros = Instant.now().toEpochMilli() * 1000;
        long startMicros = Instant.now().minus(hours, ChronoUnit.HOURS).toEpochMilli() * 1000;

        JaegerApiClient.JaegerTraceSearchResponse search =
            jaegerApiClient.searchTraces(service, null, startMicros, endMicros, limit, null);

        List<SlowSpanResponse> slowSpans = new ArrayList<>();
        if (search.data() != null) {
            for (JaegerTraceResponse.TraceData trace : search.data()) {
                if (trace.getSpans() == null) {
                    continue;
                }
                for (JaegerTraceResponse.Span span : trace.getSpans()) {
                    long durationMs = span.getDuration() != null ? span.getDuration() / 1000 : 0L;
                    if (durationMs >= thresholdMs) {
                        slowSpans.add(new SlowSpanResponse(
                            trace.getTraceID(),
                            span.getSpanID(),
                            span.getOperationName(),
                            serviceName(span, trace.getProcesses()),
                            durationMs,
                            hasError(span)
                        ));
                    }
                }
            }
        }

        slowSpans.sort(Comparator.comparingLong(SlowSpanResponse::durationMs).reversed());
        return ResponseEntity.ok(slowSpans);
    }

    @GetMapping("/traces/errors")
    @Operation(summary = "Get traces containing error spans for a service")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<List<TraceErrorResponse>> getErrorTraces(
        @RequestParam("service") String service,
        @RequestParam(value = "hours", defaultValue = "24") int hours,
        @RequestParam(value = "limit", defaultValue = "100") int limit
    ) {
        long endMicros = Instant.now().toEpochMilli() * 1000;
        long startMicros = Instant.now().minus(hours, ChronoUnit.HOURS).toEpochMilli() * 1000;

        JaegerApiClient.JaegerTraceSearchResponse search =
            jaegerApiClient.searchTraces(service, null, startMicros, endMicros, limit, null);

        List<TraceErrorResponse> errors = new ArrayList<>();
        if (search.data() != null) {
            for (JaegerTraceResponse.TraceData trace : search.data()) {
                long errorSpans = trace.getSpans() == null ? 0L : trace.getSpans().stream().filter(this::hasError).count();
                if (errorSpans > 0) {
                    errors.add(new TraceErrorResponse(trace.getTraceID(), errorSpans));
                }
            }
        }

        return ResponseEntity.ok(errors);
    }

    private List<SpanSummary> buildLineage(
        JaegerTraceResponse.Span span,
        Map<String, JaegerTraceResponse.Span> spansById,
        Map<String, JaegerTraceResponse.Process> processes
    ) {
        List<SpanSummary> reversed = new ArrayList<>();
        JaegerTraceResponse.Span current = span;

        while (current != null) {
            reversed.add(new SpanSummary(
                current.getSpanID(),
                current.getOperationName(),
                serviceName(current, processes),
                current.getDuration() != null ? current.getDuration() / 1000 : 0L,
                hasError(current)
            ));

            String parentId = parentSpanId(current);
            current = parentId == null ? null : spansById.get(parentId);
        }

        List<SpanSummary> lineage = new ArrayList<>();
        for (int i = reversed.size() - 1; i >= 0; i--) {
            lineage.add(reversed.get(i));
        }
        return lineage;
    }

    private String parentSpanId(JaegerTraceResponse.Span span) {
        if (span.getReferences() == null || span.getReferences().isEmpty()) {
            return null;
        }
        return span.getReferences().stream()
            .filter(ref -> "CHILD_OF".equalsIgnoreCase(ref.getRefType()))
            .map(JaegerTraceResponse.Reference::getSpanID)
            .findFirst()
            .orElse(null);
    }

    private boolean hasError(JaegerTraceResponse.Span span) {
        if (span.getTags() == null) {
            return false;
        }
        return span.getTags().stream()
            .anyMatch(tag -> "error".equals(tag.getKey()) && Boolean.TRUE.equals(tag.getValue()));
    }

    private String serviceName(JaegerTraceResponse.Span span, Map<String, JaegerTraceResponse.Process> processes) {
        if (span.getProcessID() == null || processes == null) {
            return null;
        }
        JaegerTraceResponse.Process process = processes.get(span.getProcessID());
        return process != null ? process.getServiceName() : null;
    }

    public record SpanSummary(
        String spanId,
        String operationName,
        String serviceName,
        long durationMs,
        boolean hasError
    ) {}

    public record CriticalPathResponse(
        String traceId,
        List<SpanSummary> path,
        long totalDurationMs
    ) {}

    public record SlowSpanResponse(
        String traceId,
        String spanId,
        String operationName,
        String serviceName,
        long durationMs,
        boolean hasError
    ) {}

    public record TraceErrorResponse(
        String traceId,
        long errorSpanCount
    ) {}
}
