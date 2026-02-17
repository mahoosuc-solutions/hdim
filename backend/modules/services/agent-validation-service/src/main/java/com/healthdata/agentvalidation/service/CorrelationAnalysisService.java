package com.healthdata.agentvalidation.service;

import com.healthdata.agentvalidation.client.JaegerApiClient;
import com.healthdata.agentvalidation.client.dto.JaegerTraceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CorrelationAnalysisService {

    private final JaegerApiClient jaegerApiClient;

    public RootCauseAnalysisResponse analyzeRootCauses(String anomalyId, String service, int hours, int limit) {
        DependencyGraph graph = buildGraph(service, hours, limit);
        int maxDegree = graph.nodes().values().stream()
            .mapToInt(node -> node.inboundEdges() + node.outboundEdges())
            .max()
            .orElse(1);

        List<RootCauseCandidate> candidates = graph.nodes().values().stream()
            .filter(node -> node.errorSpanCount() > 0)
            .map(node -> {
                double errorRate = node.totalSpans() == 0 ? 0D : (double) node.errorSpanCount() / node.totalSpans();
                double centrality = (double) (node.inboundEdges() + node.outboundEdges()) / maxDegree;
                double score = roundTo2((errorRate * 0.7D) + (centrality * 0.3D));
                return new RootCauseCandidate(
                    node.serviceName(),
                    score,
                    node.errorSpanCount(),
                    node.totalSpans(),
                    node.averageDurationMs()
                );
            })
            .sorted(Comparator.comparingDouble(RootCauseCandidate::score).reversed())
            .limit(5)
            .toList();

        return new RootCauseAnalysisResponse(anomalyId, service, candidates, Instant.now());
    }

    public ServiceDependencyResponse getServiceDependencies(String service, int hours, int limit) {
        DependencyGraph graph = buildGraph(service, hours, limit);
        List<ServiceNode> nodes = graph.nodes().values().stream()
            .sorted(Comparator.comparing(ServiceNode::serviceName))
            .toList();
        List<ServiceEdge> edges = graph.edges().values().stream()
            .sorted(Comparator.comparing(ServiceEdge::sourceService).thenComparing(ServiceEdge::targetService))
            .toList();
        return new ServiceDependencyResponse(service, nodes, edges, Instant.now());
    }

    public FailurePathResponse getFailurePaths(String serviceId, String service, int hours, int limit) {
        DependencyGraph graph = buildGraph(service, hours, limit);
        Map<String, List<ServiceEdge>> adjacency = new HashMap<>();
        for (ServiceEdge edge : graph.edges().values()) {
            adjacency.computeIfAbsent(edge.sourceService(), ignored -> new ArrayList<>()).add(edge);
        }
        adjacency.values().forEach(list ->
            list.sort(Comparator.comparingInt(ServiceEdge::errorCount).reversed())
        );

        List<FailurePath> failurePaths = new ArrayList<>();
        ArrayDeque<PathFrame> queue = new ArrayDeque<>();
        queue.add(new PathFrame(List.of(serviceId), serviceId));

        while (!queue.isEmpty() && failurePaths.size() < 10) {
            PathFrame frame = queue.poll();
            List<ServiceEdge> outgoing = adjacency.getOrDefault(frame.lastService(), List.of());

            for (ServiceEdge edge : outgoing) {
                if (frame.path().contains(edge.targetService()) || frame.path().size() >= 4) {
                    continue;
                }

                List<String> extended = new ArrayList<>(frame.path());
                extended.add(edge.targetService());
                ServiceNode target = graph.nodes().get(edge.targetService());
                if (target != null && target.errorSpanCount() > 0) {
                    failurePaths.add(new FailurePath(extended, target.errorSpanCount(), edge.callCount()));
                }

                queue.add(new PathFrame(extended, edge.targetService()));
            }
        }

        failurePaths.sort(
            Comparator.comparingInt(FailurePath::errorSpanCount).reversed()
                .thenComparingInt(FailurePath::edgeCallCount).reversed()
        );

        return new FailurePathResponse(serviceId, service, failurePaths.stream().limit(10).toList(), Instant.now());
    }

    private DependencyGraph buildGraph(String service, int hours, int limit) {
        long endMicros = Instant.now().toEpochMilli() * 1000;
        long startMicros = Instant.now().minus(hours, ChronoUnit.HOURS).toEpochMilli() * 1000;

        JaegerApiClient.JaegerTraceSearchResponse traces =
            jaegerApiClient.searchTraces(service, null, startMicros, endMicros, limit, null);

        Map<String, MutableNode> nodes = new HashMap<>();
        Map<String, MutableEdge> edges = new HashMap<>();

        if (traces.data() != null) {
            for (JaegerTraceResponse.TraceData trace : traces.data()) {
                Map<String, JaegerTraceResponse.Span> spansById = new HashMap<>();
                Map<String, JaegerTraceResponse.Process> processes =
                    trace.getProcesses() == null ? Map.of() : trace.getProcesses();

                if (trace.getSpans() == null) {
                    continue;
                }

                for (JaegerTraceResponse.Span span : trace.getSpans()) {
                    spansById.put(span.getSpanID(), span);

                    String currentService = serviceName(span, processes);
                    if (currentService == null) {
                        continue;
                    }

                    MutableNode node = nodes.computeIfAbsent(currentService, MutableNode::new);
                    node.totalSpans++;
                    node.totalDurationMs += durationMs(span);
                    if (hasError(span)) {
                        node.errorSpanCount++;
                    }
                }

                for (JaegerTraceResponse.Span span : trace.getSpans()) {
                    String childService = serviceName(span, processes);
                    if (childService == null) {
                        continue;
                    }
                    String parentSpanId = parentSpanId(span);
                    if (parentSpanId == null) {
                        continue;
                    }

                    JaegerTraceResponse.Span parentSpan = spansById.get(parentSpanId);
                    if (parentSpan == null) {
                        continue;
                    }

                    String parentService = serviceName(parentSpan, processes);
                    if (parentService == null || parentService.equals(childService)) {
                        continue;
                    }

                    String edgeKey = parentService + "->" + childService;
                    MutableEdge edge = edges.computeIfAbsent(edgeKey, ignored -> new MutableEdge(parentService, childService));
                    edge.callCount++;
                    if (hasError(span)) {
                        edge.errorCount++;
                    }

                    nodes.get(parentService).outboundEdges++;
                    nodes.get(childService).inboundEdges++;
                }
            }
        }

        Map<String, ServiceNode> immutableNodes = new HashMap<>();
        for (MutableNode node : nodes.values()) {
            long avgDurationMs = node.totalSpans == 0 ? 0 : node.totalDurationMs / node.totalSpans;
            immutableNodes.put(node.serviceName, new ServiceNode(
                node.serviceName,
                node.totalSpans,
                node.errorSpanCount,
                avgDurationMs,
                node.inboundEdges,
                node.outboundEdges
            ));
        }

        Map<String, ServiceEdge> immutableEdges = new HashMap<>();
        for (Map.Entry<String, MutableEdge> entry : edges.entrySet()) {
            MutableEdge edge = entry.getValue();
            immutableEdges.put(entry.getKey(), new ServiceEdge(
                edge.sourceService,
                edge.targetService,
                edge.callCount,
                edge.errorCount
            ));
        }

        return new DependencyGraph(immutableNodes, immutableEdges);
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

    private String serviceName(JaegerTraceResponse.Span span, Map<String, JaegerTraceResponse.Process> processes) {
        if (span.getProcessID() == null || processes == null) {
            return null;
        }
        JaegerTraceResponse.Process process = processes.get(span.getProcessID());
        return process == null ? null : process.getServiceName();
    }

    private boolean hasError(JaegerTraceResponse.Span span) {
        if (span.getTags() == null) {
            return false;
        }
        return span.getTags().stream()
            .anyMatch(tag -> "error".equals(tag.getKey()) && Boolean.TRUE.equals(tag.getValue()));
    }

    private long durationMs(JaegerTraceResponse.Span span) {
        return span.getDuration() == null ? 0 : span.getDuration() / 1000;
    }

    private double roundTo2(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    private record DependencyGraph(
        Map<String, ServiceNode> nodes,
        Map<String, ServiceEdge> edges
    ) {}

    private record PathFrame(List<String> path, String lastService) {}

    private static class MutableNode {
        private final String serviceName;
        private int totalSpans;
        private int errorSpanCount;
        private long totalDurationMs;
        private int inboundEdges;
        private int outboundEdges;

        private MutableNode(String serviceName) {
            this.serviceName = serviceName;
        }
    }

    private static class MutableEdge {
        private final String sourceService;
        private final String targetService;
        private int callCount;
        private int errorCount;

        private MutableEdge(String sourceService, String targetService) {
            this.sourceService = sourceService;
            this.targetService = targetService;
        }
    }

    public record RootCauseAnalysisResponse(
        String anomalyId,
        String sourceService,
        List<RootCauseCandidate> candidates,
        Instant generatedAt
    ) {}

    public record RootCauseCandidate(
        String serviceName,
        double score,
        int errorSpanCount,
        int totalSpans,
        long averageDurationMs
    ) {}

    public record ServiceDependencyResponse(
        String sourceService,
        List<ServiceNode> nodes,
        List<ServiceEdge> edges,
        Instant generatedAt
    ) {}

    public record ServiceNode(
        String serviceName,
        int totalSpans,
        int errorSpanCount,
        long averageDurationMs,
        int inboundEdges,
        int outboundEdges
    ) {}

    public record ServiceEdge(
        String sourceService,
        String targetService,
        int callCount,
        int errorCount
    ) {}

    public record FailurePathResponse(
        String serviceId,
        String sourceService,
        List<FailurePath> paths,
        Instant generatedAt
    ) {}

    public record FailurePath(
        List<String> services,
        int errorSpanCount,
        int edgeCallCount
    ) {}
}
