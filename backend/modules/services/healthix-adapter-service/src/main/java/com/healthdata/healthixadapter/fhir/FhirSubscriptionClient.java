package com.healthdata.healthixadapter.fhir;

import com.healthdata.common.external.ExternalEventEnvelope;
import com.healthdata.common.external.ExternalEventMetadata;
import com.healthdata.common.external.PhiLevel;
import com.healthdata.common.external.SourceSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * FHIR Subscription client for Healthix fhir-transformation-service.
 * Subscribes to real-time FHIR resource notifications and publishes
 * them to HDIM's Kafka event bus.
 *
 * PHI Level: FULL — all FHIR resources contain PHI.
 * Requires mTLS and HIPAA audit logging.
 */
@Service
@ConditionalOnProperty(name = "external.healthix.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class FhirSubscriptionClient {

    @Qualifier("healthixFhirRestTemplate")
    private final RestTemplate fhirRestTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_FHIR = "external.healthix.fhir";

    public void handleFhirNotification(Map<String, Object> fhirResource, String tenantId) {
        String resourceType = (String) fhirResource.getOrDefault("resourceType", "Unknown");
        String resourceId = (String) fhirResource.getOrDefault("id", "");

        log.info("Received FHIR {} notification from Healthix, id={}",
                resourceType, resourceId);

        ExternalEventEnvelope<Map<String, Object>> envelope = ExternalEventEnvelope.of(
                "external.healthix.fhir." + resourceType.toLowerCase() + ".received",
                "healthix-adapter-service",
                tenantId,
                fhirResource,
                ExternalEventMetadata.builder()
                        .sourceSystem(SourceSystem.HEALTHIX)
                        .phiLevel(PhiLevel.FULL)
                        .build());

        kafkaTemplate.send(TOPIC_FHIR, tenantId, envelope);
    }

    /**
     * Register HDIM as a FHIR Subscription consumer on Healthix.
     */
    public void registerSubscription(String callbackUrl, String resourceType) {
        log.info("Registering FHIR subscription for resourceType={}, callback={}",
                resourceType, callbackUrl);

        Map<String, Object> subscription = Map.of(
                "resourceType", "Subscription",
                "status", "requested",
                "criteria", resourceType + "?",
                "channel", Map.of(
                        "type", "rest-hook",
                        "endpoint", callbackUrl,
                        "payload", "application/fhir+json"
                )
        );

        fhirRestTemplate.postForEntity("/fhir/Subscription", subscription, Map.class);
        log.info("FHIR subscription registered for {}", resourceType);
    }
}
