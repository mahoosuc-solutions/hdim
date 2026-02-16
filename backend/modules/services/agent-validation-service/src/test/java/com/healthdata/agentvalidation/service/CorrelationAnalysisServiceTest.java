package com.healthdata.agentvalidation.service;

import com.healthdata.agentvalidation.client.JaegerApiClient;
import com.healthdata.agentvalidation.client.dto.JaegerTraceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorrelationAnalysisServiceTest {

    @Mock
    private JaegerApiClient jaegerApiClient;

    private CorrelationAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new CorrelationAnalysisService(jaegerApiClient);
    }

    @Test
    void getServiceDependenciesBuildsCrossServiceGraph() {
        JaegerTraceResponse.TraceData trace = sampleTrace();
        when(jaegerApiClient.searchTraces(eq("gateway-service"), isNull(), anyLong(), anyLong(), anyInt(), isNull()))
            .thenReturn(new JaegerApiClient.JaegerTraceSearchResponse(List.of(trace), 1, 100, 0, List.of()));

        var response = service.getServiceDependencies("gateway-service", 24, 100);

        assertThat(response.nodes()).extracting(CorrelationAnalysisService.ServiceNode::serviceName)
            .contains("gateway-service", "patient-service", "db-service");
        assertThat(response.edges())
            .extracting(edge -> edge.sourceService() + "->" + edge.targetService())
            .contains("gateway-service->patient-service", "patient-service->db-service");
    }

    @Test
    void analyzeRootCausesRanksErrorServiceFirst() {
        JaegerTraceResponse.TraceData trace = sampleTrace();
        when(jaegerApiClient.searchTraces(eq("gateway-service"), isNull(), anyLong(), anyLong(), anyInt(), isNull()))
            .thenReturn(new JaegerApiClient.JaegerTraceSearchResponse(List.of(trace), 1, 100, 0, List.of()));

        var response = service.analyzeRootCauses("anomaly-77", "gateway-service", 24, 100);

        assertThat(response.candidates()).isNotEmpty();
        assertThat(response.candidates().get(0).serviceName()).isEqualTo("db-service");
        assertThat(response.candidates().get(0).errorSpanCount()).isEqualTo(1);
    }

    @Test
    void getFailurePathsReturnsPropagationPathToErrorService() {
        JaegerTraceResponse.TraceData trace = sampleTrace();
        when(jaegerApiClient.searchTraces(eq("gateway-service"), isNull(), anyLong(), anyLong(), anyInt(), isNull()))
            .thenReturn(new JaegerApiClient.JaegerTraceSearchResponse(List.of(trace), 1, 100, 0, List.of()));

        var response = service.getFailurePaths("gateway-service", "gateway-service", 24, 100);

        assertThat(response.paths()).isNotEmpty();
        assertThat(response.paths().get(0).services())
            .containsExactly("gateway-service", "patient-service", "db-service");
    }

    private JaegerTraceResponse.TraceData sampleTrace() {
        JaegerTraceResponse.Span gatewaySpan = JaegerTraceResponse.Span.builder()
            .spanID("s1")
            .operationName("GET /patients")
            .duration(100_000L)
            .processID("p1")
            .build();

        JaegerTraceResponse.Span patientSpan = JaegerTraceResponse.Span.builder()
            .spanID("s2")
            .operationName("PatientController.search")
            .duration(180_000L)
            .processID("p2")
            .references(List.of(JaegerTraceResponse.Reference.builder().refType("CHILD_OF").spanID("s1").build()))
            .build();

        JaegerTraceResponse.Span dbSpan = JaegerTraceResponse.Span.builder()
            .spanID("s3")
            .operationName("PatientRepository.findAll")
            .duration(350_000L)
            .processID("p3")
            .references(List.of(JaegerTraceResponse.Reference.builder().refType("CHILD_OF").spanID("s2").build()))
            .tags(List.of(JaegerTraceResponse.Tag.builder().key("error").value(true).build()))
            .build();

        return JaegerTraceResponse.TraceData.builder()
            .traceID("trace-1")
            .spans(List.of(gatewaySpan, patientSpan, dbSpan))
            .processes(Map.of(
                "p1", JaegerTraceResponse.Process.builder().serviceName("gateway-service").build(),
                "p2", JaegerTraceResponse.Process.builder().serviceName("patient-service").build(),
                "p3", JaegerTraceResponse.Process.builder().serviceName("db-service").build()
            ))
            .build();
    }
}
