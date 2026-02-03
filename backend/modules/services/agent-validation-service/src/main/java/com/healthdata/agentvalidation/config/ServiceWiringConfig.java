package com.healthdata.agentvalidation.config;

import com.healthdata.agentvalidation.service.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to wire service dependencies.
 * Uses setter injection to resolve circular dependencies between services.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ServiceWiringConfig {

    private final TestOrchestratorService testOrchestratorService;
    private final EvaluationService evaluationService;
    private final JaegerTraceService jaegerTraceService;
    private final ReflectionService reflectionService;
    private final RegressionService regressionService;
    private final QAIntegrationService qaIntegrationService;

    @PostConstruct
    public void wireServices() {
        log.info("Wiring validation services...");

        testOrchestratorService.setEvaluationService(evaluationService);
        testOrchestratorService.setJaegerTraceService(jaegerTraceService);
        testOrchestratorService.setReflectionService(reflectionService);
        testOrchestratorService.setRegressionService(regressionService);
        testOrchestratorService.setQaIntegrationService(qaIntegrationService);

        log.info("Validation services wired successfully");
        log.info("  - EvaluationService: {}", evaluationService != null ? "configured" : "missing");
        log.info("  - JaegerTraceService: {}", jaegerTraceService != null ? "configured" : "missing");
        log.info("  - ReflectionService: {}", reflectionService != null ? "configured" : "missing");
        log.info("  - RegressionService: {}", regressionService != null ? "configured" : "missing");
        log.info("  - QAIntegrationService: {}", qaIntegrationService != null ? "configured" : "missing");
    }
}
