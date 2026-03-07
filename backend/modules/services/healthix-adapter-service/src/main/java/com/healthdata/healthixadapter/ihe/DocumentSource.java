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

import java.util.Map;
import java.util.function.Supplier;

@Component
@ConditionalOnProperty(name = "external.healthix.enabled", havingValue = "true")
@Slf4j
public class DocumentSource {

    private static final String DOCUMENTS_SUBMITTED_TOPIC = "ihe.documents.submitted";

    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AdapterSpanHelper spanHelper;

    public DocumentSource(
            @Qualifier("healthixFhirRestTemplate") RestTemplate restTemplate,
            CircuitBreakerRegistry registry,
            KafkaTemplate<String, Object> kafkaTemplate,
            AdapterSpanHelper spanHelper) {
        this.restTemplate = restTemplate;
        this.circuitBreaker = registry.circuitBreaker("healthix-fhir");
        this.kafkaTemplate = kafkaTemplate;
        this.spanHelper = spanHelper;
    }

    @SuppressWarnings("unchecked")
    public DocumentSubmissionResult submitDocument(DocumentSubmission submission) {
        return spanHelper.traced("healthix.ihe.provide_and_register",
                () -> {
                    log.info("ITI-41 submit: patient={} type={}", submission.getPatientId(), submission.getDocumentType());

                    Supplier<Map> supplier = CircuitBreaker.decorateSupplier(
                            circuitBreaker,
                            () -> restTemplate.postForObject(
                                    "/fhir/DocumentReference",
                                    Map.of(
                                            "resourceType", "DocumentReference",
                                            "status", "current",
                                            "subject", Map.of("reference", "Patient/" + submission.getPatientId()),
                                            "type", Map.of("text", submission.getDocumentType()),
                                            "description", submission.getDescription()
                                    ),
                                    Map.class));

                    Map responseMap = supplier.get();

                    DocumentSubmissionResult result = new DocumentSubmissionResult();
                    result.setPatientId(submission.getPatientId());
                    result.setDocumentType(submission.getDocumentType());
                    result.setSuccess(responseMap != null);
                    if (responseMap != null) {
                        result.setDocumentId(String.valueOf(responseMap.getOrDefault("id", "")));
                    }

                    kafkaTemplate.send(DOCUMENTS_SUBMITTED_TOPIC, submission.getPatientId(), result);
                    log.info("ITI-41 submission result published: success={}", result.isSuccess());

                    return result;
                },
                "adapter", "healthix",
                "ihe.transaction", "ITI-41",
                "phi.level", "FULL");
    }

    @Data
    public static class DocumentSubmission {
        private String patientId;
        private String documentType;
        private String description;
        private byte[] content;
    }

    @Data
    public static class DocumentSubmissionResult {
        private String patientId;
        private String documentType;
        private String documentId;
        private boolean success;
    }
}
