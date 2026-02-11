package com.healthdata.agentvalidation.config;

import com.healthdata.agentvalidation.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for ServiceWiringConfig.
 * Tests that services are properly wired via setter injection.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Service Wiring Configuration Tests")
class ServiceWiringConfigTest {

    @Mock
    private TestOrchestratorService testOrchestratorService;

    @Mock
    private EvaluationService evaluationService;

    @Mock
    private JaegerTraceService jaegerTraceService;

    @Mock
    private ReflectionService reflectionService;

    @Mock
    private RegressionService regressionService;

    @Mock
    private QAIntegrationService qaIntegrationService;

    private ServiceWiringConfig serviceWiringConfig;

    @BeforeEach
    void setUp() {
        serviceWiringConfig = new ServiceWiringConfig(
            testOrchestratorService,
            evaluationService,
            jaegerTraceService,
            reflectionService,
            regressionService,
            qaIntegrationService
        );
    }

    @Test
    @DisplayName("Should wire EvaluationService to TestOrchestratorService")
    void shouldWireEvaluationService() {
        // When
        serviceWiringConfig.wireServices();

        // Then
        verify(testOrchestratorService).setEvaluationService(evaluationService);
    }

    @Test
    @DisplayName("Should wire JaegerTraceService to TestOrchestratorService")
    void shouldWireJaegerTraceService() {
        // When
        serviceWiringConfig.wireServices();

        // Then
        verify(testOrchestratorService).setJaegerTraceService(jaegerTraceService);
    }

    @Test
    @DisplayName("Should wire ReflectionService to TestOrchestratorService")
    void shouldWireReflectionService() {
        // When
        serviceWiringConfig.wireServices();

        // Then
        verify(testOrchestratorService).setReflectionService(reflectionService);
    }

    @Test
    @DisplayName("Should wire RegressionService to TestOrchestratorService")
    void shouldWireRegressionService() {
        // When
        serviceWiringConfig.wireServices();

        // Then
        verify(testOrchestratorService).setRegressionService(regressionService);
    }

    @Test
    @DisplayName("Should wire QAIntegrationService to TestOrchestratorService")
    void shouldWireQAIntegrationService() {
        // When
        serviceWiringConfig.wireServices();

        // Then
        verify(testOrchestratorService).setQaIntegrationService(qaIntegrationService);
    }

    @Test
    @DisplayName("Should wire all services in single call")
    void shouldWireAllServicesInSingleCall() {
        // When
        serviceWiringConfig.wireServices();

        // Then - All services should be wired
        verify(testOrchestratorService, times(1)).setEvaluationService(evaluationService);
        verify(testOrchestratorService, times(1)).setJaegerTraceService(jaegerTraceService);
        verify(testOrchestratorService, times(1)).setReflectionService(reflectionService);
        verify(testOrchestratorService, times(1)).setRegressionService(regressionService);
        verify(testOrchestratorService, times(1)).setQaIntegrationService(qaIntegrationService);
    }

    @Test
    @DisplayName("Should handle null services gracefully during wiring")
    void shouldHandleNullServicesGracefully() {
        // Given - Config with null services (edge case)
        ServiceWiringConfig configWithNulls = new ServiceWiringConfig(
            testOrchestratorService,
            null,  // null evaluationService
            jaegerTraceService,
            reflectionService,
            regressionService,
            qaIntegrationService
        );

        // When/Then - Should not throw exception
        configWithNulls.wireServices();

        // Verify wiring was still attempted
        verify(testOrchestratorService).setEvaluationService(null);
        verify(testOrchestratorService).setJaegerTraceService(jaegerTraceService);
    }
}
