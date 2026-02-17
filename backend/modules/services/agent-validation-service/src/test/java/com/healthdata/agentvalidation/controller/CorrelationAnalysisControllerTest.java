package com.healthdata.agentvalidation.controller;

import com.healthdata.agentvalidation.service.CorrelationAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorrelationAnalysisControllerTest {

    @Mock
    private CorrelationAnalysisService correlationAnalysisService;

    private CorrelationAnalysisController controller;

    @BeforeEach
    void setUp() {
        controller = new CorrelationAnalysisController(correlationAnalysisService);
    }

    @Test
    void getRootCausesReturnsRankedCandidates() {
        CorrelationAnalysisService.RootCauseAnalysisResponse payload =
            new CorrelationAnalysisService.RootCauseAnalysisResponse(
                "anomaly-1",
                "gateway-service",
                List.of(new CorrelationAnalysisService.RootCauseCandidate("patient-service", 0.92, 4, 10, 130)),
                Instant.now()
            );
        when(correlationAnalysisService.analyzeRootCauses("anomaly-1", "gateway-service", 24, 100))
            .thenReturn(payload);

        var response = controller.getRootCauses("anomaly-1", "gateway-service", 24, 100);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().candidates()).hasSize(1);
        assertThat(response.getBody().candidates().get(0).serviceName()).isEqualTo("patient-service");
    }

    @Test
    void getServiceDependenciesReturnsGraph() {
        CorrelationAnalysisService.ServiceDependencyResponse payload =
            new CorrelationAnalysisService.ServiceDependencyResponse(
                "gateway-service",
                List.of(new CorrelationAnalysisService.ServiceNode("gateway-service", 2, 0, 12, 0, 1)),
                List.of(new CorrelationAnalysisService.ServiceEdge("gateway-service", "patient-service", 2, 1)),
                Instant.now()
            );
        when(correlationAnalysisService.getServiceDependencies("gateway-service", 12, 50))
            .thenReturn(payload);

        var response = controller.getServiceDependencies("gateway-service", 12, 50);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().edges()).hasSize(1);
        assertThat(response.getBody().edges().get(0).targetService()).isEqualTo("patient-service");
    }

    @Test
    void getFailurePathsReturnsPaths() {
        CorrelationAnalysisService.FailurePathResponse payload =
            new CorrelationAnalysisService.FailurePathResponse(
                "gateway-service",
                "gateway-service",
                List.of(new CorrelationAnalysisService.FailurePath(
                    List.of("gateway-service", "patient-service"), 3, 4
                )),
                Instant.now()
            );
        when(correlationAnalysisService.getFailurePaths("gateway-service", "gateway-service", 24, 100))
            .thenReturn(payload);

        var response = controller.getFailurePaths("gateway-service", "gateway-service", 24, 100);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().paths()).hasSize(1);
        assertThat(response.getBody().paths().get(0).services())
            .containsExactly("gateway-service", "patient-service");
    }
}
