package com.healthdata.hedisadapter.crm;

import com.healthdata.hedisadapter.config.HedisProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Bidirectional CRM sync between HDIM sales-automation-service
 * and hedis's Zoho integration.
 */
@Service
@ConditionalOnProperty(name = "external.hedis.enabled", havingValue = "true")
@Slf4j
public class CrmSyncService {

    private final RestTemplate hedisRestTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CircuitBreaker circuitBreaker;

    public CrmSyncService(
            @Qualifier("hedisRestTemplate") RestTemplate hedisRestTemplate,
            KafkaTemplate<String, Object> kafkaTemplate,
            CircuitBreakerRegistry registry) {
        this.hedisRestTemplate = hedisRestTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.circuitBreaker = registry.circuitBreaker("hedis-crm");
    }

    /**
     * Push sales deal update from HDIM to hedis CRM.
     */
    public void pushDealUpdate(Map<String, Object> dealData) {
        log.info("Pushing deal update to hedis CRM integration");

        Supplier<Void> supplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                () -> {
                    hedisRestTemplate.postForEntity("/api/crm/deals/sync", dealData, Void.class);
                    return null;
                });

        supplier.get();
    }

    /**
     * Handle incoming CRM webhook from hedis (Zoho updates).
     */
    public void onCrmWebhook(Map<String, Object> webhookPayload) {
        log.info("Received CRM webhook from hedis");
        kafkaTemplate.send("external.hedis.crm", "crm-sync", webhookPayload);
    }
}
