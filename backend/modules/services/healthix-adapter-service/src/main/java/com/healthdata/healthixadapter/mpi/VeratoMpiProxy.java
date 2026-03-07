package com.healthdata.healthixadapter.mpi;

import com.healthdata.healthixadapter.observability.AdapterSpanHelper;
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

import java.util.List;
import java.util.function.Supplier;

/**
 * Verato MPI (Master Patient Index) proxy.
 * Accepts HDIM patient records and queries Healthix patient-identity-service
 * for cross-reference identifiers across participating organizations.
 */
@Service
@ConditionalOnProperty(name = "external.healthix.enabled", havingValue = "true")
@Slf4j
public class VeratoMpiProxy {

    private final RestTemplate mpiRestTemplate;
    private final CircuitBreaker circuitBreaker;
    private final AdapterSpanHelper spanHelper;

    public VeratoMpiProxy(
            @Qualifier("healthixMpiRestTemplate") RestTemplate mpiRestTemplate,
            CircuitBreakerRegistry registry,
            AdapterSpanHelper spanHelper) {
        this.mpiRestTemplate = mpiRestTemplate;
        this.circuitBreaker = registry.circuitBreaker("healthix-mpi");
        this.spanHelper = spanHelper;
    }

    /**
     * Query Healthix MPI for cross-reference identifiers.
     */
    public MpiMatchResult queryPatientMatch(MpiMatchRequest request) {
        return spanHelper.traced("healthix.mpi.query_match", () -> {
            log.info("Querying Verato MPI for patient match, identifiers={}",
                    request.getIdentifiers().size());

            Supplier<MpiMatchResult> supplier = CircuitBreaker.decorateSupplier(
                    circuitBreaker,
                    () -> mpiRestTemplate.postForObject(
                            "/api/v1/patient-identity/match",
                            request,
                            MpiMatchResult.class));

            return supplier.get();
        }, "adapter", "healthix", "phi.level", "FULL");
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MpiMatchRequest {
        private String tenantId;
        private List<PatientIdentifier> identifiers;
        private String firstName;
        private String lastName;
        private String dateOfBirth;
        private String gender;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PatientIdentifier {
        private String system;
        private String value;
        private String assigningAuthority;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MpiMatchResult {
        private String enterpriseId;
        private double matchConfidence;
        private List<CrossReference> crossReferences;
        private String matchStatus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CrossReference {
        private String organization;
        private String patientId;
        private String system;
        private String assigningAuthority;
    }
}
