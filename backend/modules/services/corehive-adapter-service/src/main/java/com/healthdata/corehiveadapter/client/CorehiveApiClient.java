package com.healthdata.corehiveadapter.client;

import com.healthdata.corehiveadapter.model.CareGapScoringRequest;
import com.healthdata.corehiveadapter.model.CareGapScoringResponse;
import com.healthdata.corehiveadapter.model.VbcRoiRequest;
import com.healthdata.corehiveadapter.model.VbcRoiResponse;
import com.healthdata.corehiveadapter.observability.AdapterSpanHelper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.function.Supplier;

@Component
@ConditionalOnProperty(name = "external.corehive.enabled", havingValue = "true")
@Slf4j
public class CorehiveApiClient {

    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final AdapterSpanHelper spanHelper;

    public CorehiveApiClient(
            @Qualifier("corehiveRestTemplate") RestTemplate restTemplate,
            CircuitBreakerRegistry registry,
            AdapterSpanHelper spanHelper) {
        this.restTemplate = restTemplate;
        this.circuitBreaker = registry.circuitBreaker("corehive-api");
        this.spanHelper = spanHelper;
    }

    public CareGapScoringResponse scoreCareGaps(CareGapScoringRequest request) {
        return spanHelper.traced("corehive.client.score_care_gaps", () -> {
            Supplier<CareGapScoringResponse> supplier = CircuitBreaker.decorateSupplier(
                    circuitBreaker,
                    () -> restTemplate.postForObject(
                            "/api/healthcare/score",
                            request,
                            CareGapScoringResponse.class));
            return supplier.get();
        }, "adapter", "corehive", "phi.level", "NONE");
    }

    public VbcRoiResponse calculateRoi(VbcRoiRequest request) {
        return spanHelper.traced("corehive.client.calculate_roi", () -> {
            Supplier<VbcRoiResponse> supplier = CircuitBreaker.decorateSupplier(
                    circuitBreaker,
                    () -> restTemplate.postForObject(
                            "/api/healthcare/roi",
                            request,
                            VbcRoiResponse.class));
            return supplier.get();
        }, "adapter", "corehive", "phi.level", "NONE");
    }
}
