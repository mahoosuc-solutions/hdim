package com.healthdata.healthixadapter.ihe;

import com.healthdata.healthixadapter.observability.AdapterSpanHelper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
@ConditionalOnProperty(name = "external.healthix.enabled", havingValue = "true")
@Slf4j
public class PixV3Client {

    private static final String PIX_CROSSREF_TOPIC = "ihe.patient.crossref";

    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AdapterSpanHelper spanHelper;

    public PixV3Client(
            @Qualifier("healthixMpiRestTemplate") RestTemplate restTemplate,
            CircuitBreakerRegistry registry,
            KafkaTemplate<String, Object> kafkaTemplate,
            AdapterSpanHelper spanHelper) {
        this.restTemplate = restTemplate;
        this.circuitBreaker = registry.circuitBreaker("healthix-mpi");
        this.kafkaTemplate = kafkaTemplate;
        this.spanHelper = spanHelper;
    }

    public PixCrossReferenceResult queryCrossReferences(String patientId, String assigningAuthority) {
        return spanHelper.traced("healthix.ihe.pix_query",
                () -> {
                    log.info("PIX query for patient={} authority={}", patientId, assigningAuthority);

                    Map<String, String> request = Map.of(
                            "patientId", patientId,
                            "assigningAuthority", assigningAuthority
                    );

                    Supplier<PixCrossReferenceResult> supplier = CircuitBreaker.decorateSupplier(
                            circuitBreaker,
                            () -> restTemplate.postForObject(
                                    "/api/v1/mpi/pix-query",
                                    request,
                                    PixCrossReferenceResult.class));

                    PixCrossReferenceResult result = supplier.get();

                    if (result != null && result.getIdentifiers() != null && !result.getIdentifiers().isEmpty()) {
                        kafkaTemplate.send(PIX_CROSSREF_TOPIC, patientId, result);
                        log.info("PIX cross-references published: {} identifiers", result.getIdentifiers().size());
                    }

                    return result;
                },
                "adapter", "healthix",
                "ihe.transaction", "ITI-45",
                "phi.level", "FULL");
    }

    @Data
    public static class PixCrossReferenceResult {
        private String sourcePatientId;
        private String sourceAuthority;
        private List<CrossReferenceIdentifier> identifiers;

        public PixCrossReferenceResult() {
            this.identifiers = Collections.emptyList();
        }
    }

    @Data
    public static class CrossReferenceIdentifier {
        private String patientId;
        private String assigningAuthority;
        private String identifierType;
    }
}
