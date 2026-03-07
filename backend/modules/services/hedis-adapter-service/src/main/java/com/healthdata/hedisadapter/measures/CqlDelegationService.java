package com.healthdata.hedisadapter.measures;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Delegates CQL measure calculations to hedis's CQL engine
 * for measures HDIM doesn't natively support (MIPS, STAR).
 */
@Service
@ConditionalOnProperty(name = "external.hedis.enabled", havingValue = "true")
@Slf4j
public class CqlDelegationService {

    private final RestTemplate cqlRestTemplate;
    private final CircuitBreaker circuitBreaker;

    public CqlDelegationService(
            @Qualifier("hedisCqlRestTemplate") RestTemplate cqlRestTemplate,
            CircuitBreakerRegistry registry) {
        this.cqlRestTemplate = cqlRestTemplate;
        this.circuitBreaker = registry.circuitBreaker("hedis-cql");
    }

    public CalculationResult calculateMeasure(CalculationRequest request) {
        log.info("Delegating measure calculation to hedis CQL engine: measure={}",
                request.getMeasureId());

        Supplier<CalculationResult> supplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                () -> cqlRestTemplate.postForObject(
                        "/api/measures/calculate",
                        request,
                        CalculationResult.class));

        return supplier.get();
    }

    public CalculationResult calculateStarMeasure(CalculationRequest request) {
        log.info("Delegating STAR measure calculation: measure={}", request.getMeasureId());

        Supplier<CalculationResult> supplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                () -> cqlRestTemplate.postForObject(
                        "/api/star/calculate",
                        request,
                        CalculationResult.class));

        return supplier.get();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CalculationRequest {
        private String measureId;
        private String measureType; // HEDIS, MIPS, STAR
        private String tenantId;
        private String patientId;
        private String measurementPeriodStart;
        private String measurementPeriodEnd;
        private Map<String, Object> parameters;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CalculationResult {
        private String measureId;
        private String measureType;
        private String patientId;
        private String status; // NUMERATOR, DENOMINATOR, EXCEPTION, EXCLUSION
        private double score;
        private Map<String, Object> details;
    }
}
