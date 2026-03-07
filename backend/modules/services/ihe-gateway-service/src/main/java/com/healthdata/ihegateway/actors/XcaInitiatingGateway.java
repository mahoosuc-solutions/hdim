package com.healthdata.ihegateway.actors;

import com.healthdata.ihegateway.observability.AdapterSpanHelper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "external.ihe.xca.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class XcaInitiatingGateway {

    private static final String XCA_QUERY_RESULTS_TOPIC = "ihe.xca.query.results";
    private static final String XCA_RETRIEVE_RESULTS_TOPIC = "ihe.xca.retrieve.results";

    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AdapterSpanHelper spanHelper;
    private final String remoteCommunityUrl;

    public XcaInitiatingGateway(
            RestTemplate restTemplate,
            KafkaTemplate<String, Object> kafkaTemplate,
            AdapterSpanHelper spanHelper,
            @Value("${external.ihe.xca.remote-community-url}") String remoteCommunityUrl) {
        this.restTemplate = restTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.spanHelper = spanHelper;
        this.remoteCommunityUrl = remoteCommunityUrl;
    }

    @SuppressWarnings("unchecked")
    public XcaQueryResult crossGatewayQuery(String patientId, String documentType) {
        return spanHelper.traced("ihe.xca.cross_gateway_query",
                () -> {
                    log.info("ITI-38 Cross Gateway Query: patient={} type={}", patientId, documentType);

                    String url = remoteCommunityUrl
                            + "/fhir/DocumentReference?patient=" + patientId
                            + "&type=" + documentType;

                    Map responseMap = restTemplate.getForObject(url, Map.class);

                    XcaQueryResult result = new XcaQueryResult();
                    result.setPatientId(patientId);
                    result.setDocumentType(documentType);
                    result.setSourceCommunity(remoteCommunityUrl);

                    if (responseMap != null && responseMap.containsKey("entry")) {
                        List<Map<String, Object>> entries = (List<Map<String, Object>>) responseMap.get("entry");
                        result.setTotalResults(entries.size());
                    } else {
                        result.setTotalResults(0);
                    }

                    if (result.getTotalResults() > 0) {
                        kafkaTemplate.send(XCA_QUERY_RESULTS_TOPIC, patientId, result);
                    }

                    return result;
                },
                "ihe.transaction", "ITI-38",
                "patient.id", patientId,
                "document.type", documentType);
    }

    public byte[] crossGatewayRetrieve(String documentUrl) {
        return spanHelper.traced("ihe.xca.cross_gateway_retrieve",
                () -> {
                    log.info("ITI-39 Cross Gateway Retrieve: url={}", documentUrl);

                    byte[] document = restTemplate.getForObject(documentUrl, byte[].class);

                    kafkaTemplate.send(XCA_RETRIEVE_RESULTS_TOPIC, Map.of(
                            "documentUrl", documentUrl,
                            "size", document != null ? document.length : 0
                    ));

                    return document;
                },
                "ihe.transaction", "ITI-39");
    }

    @Data
    public static class XcaQueryResult {
        private String patientId;
        private String documentType;
        private int totalResults;
        private String sourceCommunity;
    }
}
