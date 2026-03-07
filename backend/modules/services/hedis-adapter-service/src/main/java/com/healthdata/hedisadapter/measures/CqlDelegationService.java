package com.healthdata.hedisadapter.measures;

import com.healthdata.hedisadapter.observability.AdapterSpanHelper;
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
    private final AdapterSpanHelper spanHelper;

    public CqlDelegationService(
            @Qualifier("hedisCqlRestTemplate") RestTemplate cqlRestTemplate,
            CircuitBreakerRegistry registry,
            AdapterSpanHelper spanHelper) {
        this.cqlRestTemplate = cqlRestTemplate;
        this.circuitBreaker = registry.circuitBreaker("hedis-cql");
        this.spanHelper = spanHelper;
    }

    public CalculationResult calculateMeasure(CalculationRequest request) {
        return spanHelper.traced("hedis.cql.delegate_calculation", () -> {
            log.info("Delegating measure calculation to hedis CQL engine: measure={}",
                    request.getMeasureId());

            Supplier<CalculationResult> supplier = CircuitBreaker.decorateSupplier(
                    circuitBreaker,
                    () -> cqlRestTemplate.postForObject(
                            "/api/measures/calculate",
                            request,
                            CalculationResult.class));

            return supplier.get();
        }, "phi.level", "LIMITED", "measure.id", request.getMeasureId());
    }

    public CalculationResult calculateStarMeasure(CalculationRequest request) {
        return spanHelper.traced("hedis.cql.delegate_calculation", () -> {
            log.info("Delegating STAR measure calculation: measure={}", request.getMeasureId());

            Supplier<CalculationResult> supplier = CircuitBreaker.decorateSupplier(
                    circuitBreaker,
                    () -> cqlRestTemplate.postForObject(
                            "/api/star/calculate",
                            request,
                            CalculationResult.class));

            return supplier.get();
        }, "phi.level", "LIMITED", "measure.id", request.getMeasureId(), "measure.type", "STAR");
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
