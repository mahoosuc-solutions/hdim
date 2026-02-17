package com.healthdata.agentvalidation.controller;

import com.healthdata.agentvalidation.client.JaegerApiClient;
import com.healthdata.agentvalidation.client.dto.JaegerTraceResponse;
import com.healthdata.agentvalidation.service.JaegerTraceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceAnalysisControllerTest {

    @Mock
    private JaegerTraceService jaegerTraceService;

    @Mock
    private JaegerApiClient jaegerApiClient;

    private TraceAnalysisController controller;

    @BeforeEach
    void setUp() {
        controller = new TraceAnalysisController(jaegerTraceService, jaegerApiClient);
    }

    @Test
    void getTraceReturnsTraceDataWhenFound() {
        JaegerTraceResponse.TraceData traceData = JaegerTraceResponse.TraceData.builder()
            .traceID("trace-1")
            .spans(List.of())
            .build();
        JaegerTraceResponse response = JaegerTraceResponse.builder()
            .data(List.of(traceData))
            .build();

        when(jaegerTraceService.fetchTrace("trace-1")).thenReturn(Optional.of(response));

        var result = controller.getTrace("trace-1");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getTraceID()).isEqualTo("trace-1");
    }

    @Test
    void getCriticalPathBuildsLineageForLongestSpan() {
        JaegerTraceResponse.Span root = JaegerTraceResponse.Span.builder()
            .spanID("root")
            .operationName("GET /api")
            .duration(50_000L)
            .processID("p1")
            .build();

        JaegerTraceResponse.Span child = JaegerTraceResponse.Span.builder()
            .spanID("child")
            .operationName("db.query")
            .duration(120_000L)
            .processID("p2")
            .references(List.of(JaegerTraceResponse.Reference.builder().refType("CHILD_OF").spanID("root").build()))
            .build();

        JaegerTraceResponse.TraceData traceData = JaegerTraceResponse.TraceData.builder()
            .traceID("trace-2")
            .spans(List.of(root, child))
            .processes(Map.of(
                "p1", JaegerTraceResponse.Process.builder().serviceName("gateway").build(),
                "p2", JaegerTraceResponse.Process.builder().serviceName("patient-service").build()
            ))
            .build();

        JaegerTraceResponse response = JaegerTraceResponse.builder().data(List.of(traceData)).build();
        when(jaegerTraceService.fetchTrace("trace-2")).thenReturn(Optional.of(response));

        var result = controller.getCriticalPath("trace-2");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().traceId()).isEqualTo("trace-2");
        assertThat(result.getBody().path()).hasSize(2);
        assertThat(result.getBody().path().get(0).spanId()).isEqualTo("root");
        assertThat(result.getBody().path().get(1).spanId()).isEqualTo("child");
    }

    @Test
    void getSlowSpansFiltersByThreshold() {
        JaegerTraceResponse.Span fast = JaegerTraceResponse.Span.builder()
            .spanID("s1")
            .operationName("fast")
            .duration(20_000L)
            .processID("p1")
            .build();

        JaegerTraceResponse.Span slow = JaegerTraceResponse.Span.builder()
            .spanID("s2")
            .operationName("slow")
            .duration(300_000L)
            .processID("p1")
            .build();

        JaegerTraceResponse.TraceData trace = JaegerTraceResponse.TraceData.builder()
            .traceID("trace-3")
            .spans(List.of(fast, slow))
            .processes(Map.of("p1", JaegerTraceResponse.Process.builder().serviceName("svc-a").build()))
            .build();

        when(jaegerApiClient.searchTraces(eq("svc-a"), isNull(), anyLong(), anyLong(), anyInt(), isNull()))
            .thenReturn(new JaegerApiClient.JaegerTraceSearchResponse(List.of(trace), 1, 100, 0, List.of()));

        var result = controller.getSlowSpans("svc-a", 100, 24, 100);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
        assertThat(result.getBody().get(0).spanId()).isEqualTo("s2");
        assertThat(result.getBody().get(0).durationMs()).isEqualTo(300L);
    }

    @Test
    void getErrorTracesReturnsOnlyTracesWithErrorSpans() {
        JaegerTraceResponse.Span okSpan = JaegerTraceResponse.Span.builder()
            .spanID("ok")
            .tags(List.of(JaegerTraceResponse.Tag.builder().key("http.status_code").value(200).build()))
            .build();

        JaegerTraceResponse.Span errSpan = JaegerTraceResponse.Span.builder()
            .spanID("err")
            .tags(List.of(JaegerTraceResponse.Tag.builder().key("error").value(true).build()))
            .build();

        JaegerTraceResponse.TraceData okTrace = JaegerTraceResponse.TraceData.builder()
            .traceID("trace-ok")
            .spans(List.of(okSpan))
            .build();

        JaegerTraceResponse.TraceData errTrace = JaegerTraceResponse.TraceData.builder()
            .traceID("trace-err")
            .spans(List.of(errSpan))
            .build();

        when(jaegerApiClient.searchTraces(eq("svc-b"), isNull(), anyLong(), anyLong(), anyInt(), isNull()))
            .thenReturn(new JaegerApiClient.JaegerTraceSearchResponse(List.of(okTrace, errTrace), 2, 100, 0, List.of()));

        var result = controller.getErrorTraces("svc-b", 24, 100);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
        assertThat(result.getBody().get(0).traceId()).isEqualTo("trace-err");
        assertThat(result.getBody().get(0).errorSpanCount()).isEqualTo(1);
    }
}
