package com.healthdata.healthixadapter.ihe;

import com.healthdata.healthixadapter.observability.AdapterSpanHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "external.healthix.enabled", havingValue = "true")
public class XcaRespondingGateway {

    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AdapterSpanHelper spanHelper;

    public XcaRespondingGateway(@Qualifier("healthixFhirRestTemplate") RestTemplate restTemplate,
                                 KafkaTemplate<String, Object> kafkaTemplate,
                                 AdapterSpanHelper spanHelper) {
        this.restTemplate = restTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.spanHelper = spanHelper;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> respondToQuery(String patientId, String documentType) {
        return spanHelper.traced("ihe.xca.responding_query", () -> {
            String url = "/fhir/DocumentReference?patient=" + patientId + "&type=" + documentType;
            log.info("XCA Responding Gateway: query for patient={}, type={}", patientId, documentType);

            Map<String, Object> bundle = restTemplate.getForObject(url, Map.class);

            Map<String, Object> event = Map.of(
                    "patientId", patientId,
                    "documentType", documentType,
                    "totalResults", bundle != null ? bundle.getOrDefault("total", 0) : 0,
                    "respondedAt", Instant.now().toString()
            );
            kafkaTemplate.send("ihe.xca.responding.query", patientId, event);

            return bundle;
        }, "adapter", "healthix", "ihe.transaction", "ITI-38-response",
                "patient.id", patientId, "phi.level", "FULL");
    }

    public byte[] respondToRetrieve(String documentId) {
        return spanHelper.traced("ihe.xca.responding_retrieve", () -> {
            String url = "/fhir/DocumentReference/" + documentId + "/$binary";
            log.info("XCA Responding Gateway: retrieve document={}", documentId);

            byte[] document = restTemplate.getForObject(url, byte[].class);

            Map<String, Object> event = Map.of(
                    "documentId", documentId,
                    "documentSize", document != null ? document.length : 0,
                    "retrievedAt", Instant.now().toString()
            );
            kafkaTemplate.send("ihe.xca.responding.retrieve", documentId, event);

            return document;
        }, "adapter", "healthix", "ihe.transaction", "ITI-39-response",
                "document.id", documentId, "phi.level", "FULL");
    }
}
