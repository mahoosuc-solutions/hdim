package com.healthdata.ihegateway.actors;

import com.healthdata.ihegateway.config.IheGatewayProperties;
import com.healthdata.ihegateway.observability.AdapterSpanHelper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
@Slf4j
public class DocumentConsumer {

    private static final String DOCUMENTS_RECEIVED_TOPIC = "ihe.documents.received";

    private final RestTemplate restTemplate;
    private final CircuitBreaker queryCircuitBreaker;
    private final CircuitBreaker retrieveCircuitBreaker;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AdapterSpanHelper spanHelper;
    private final IheGatewayProperties properties;

    public DocumentConsumer(
            RestTemplate restTemplate,
            CircuitBreakerRegistry registry,
            KafkaTemplate<String, Object> kafkaTemplate,
            AdapterSpanHelper spanHelper,
            IheGatewayProperties properties) {
        this.restTemplate = restTemplate;
        this.queryCircuitBreaker = registry.circuitBreaker("ihe-document-query");
        this.retrieveCircuitBreaker = registry.circuitBreaker("ihe-document-retrieve");
        this.kafkaTemplate = kafkaTemplate;
        this.spanHelper = spanHelper;
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    public DocumentQueryResult queryDocuments(String patientId, String documentType) {
        return spanHelper.traced("ihe.xds.registry_stored_query",
                () -> {
                    log.info("ITI-18 query: patient={} type={}", patientId, documentType);

                    String url = properties.getHealthixDocumentUrl()
                            + "/fhir/DocumentReference?patient=" + patientId
                            + "&type=" + documentType;

                    Supplier<Map> supplier = CircuitBreaker.decorateSupplier(
                            queryCircuitBreaker,
                            () -> restTemplate.getForObject(url, Map.class));

                    Map responseMap = supplier.get();

                    DocumentQueryResult result = new DocumentQueryResult();
                    result.setPatientId(patientId);
                    result.setDocumentType(documentType);
                    if (responseMap != null && responseMap.containsKey("entry")) {
                        List<Map<String, Object>> entries = (List<Map<String, Object>>) responseMap.get("entry");
                        result.setTotalResults(entries.size());
                    } else {
                        result.setTotalResults(0);
                    }

                    kafkaTemplate.send(DOCUMENTS_RECEIVED_TOPIC, patientId, result);
                    return result;
                },
                "ihe.transaction", "ITI-18",
                "patient.id", patientId,
                "document.type", documentType);
    }

    public byte[] retrieveDocument(String documentUrl) {
        return spanHelper.traced("ihe.xds.retrieve_document_set",
                () -> {
                    log.info("ITI-43 retrieve: url={}", documentUrl);

                    Supplier<byte[]> supplier = CircuitBreaker.decorateSupplier(
                            retrieveCircuitBreaker,
                            () -> restTemplate.getForObject(documentUrl, byte[].class));

                    return supplier.get();
                },
                "ihe.transaction", "ITI-43");
    }

    @Data
    public static class DocumentQueryResult {
        private String patientId;
        private String documentType;
        private int totalResults;

        public DocumentQueryResult() {
            this.totalResults = 0;
        }
    }
}
