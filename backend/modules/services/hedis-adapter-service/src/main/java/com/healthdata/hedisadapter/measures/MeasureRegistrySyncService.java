package com.healthdata.hedisadapter.measures;

import com.healthdata.common.external.ExternalEventEnvelope;
import com.healthdata.common.external.ExternalEventMetadata;
import com.healthdata.common.external.PhiLevel;
import com.healthdata.common.external.SourceSystem;
import com.healthdata.hedisadapter.config.HedisProperties;
import com.healthdata.hedisadapter.observability.AdapterSpanHelper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Syncs measure definitions from hedis project (67 HEDIS + 271 MIPS + 40 STAR)
 * into HDIM's quality-measure-service.
 */
@Service
@ConditionalOnProperty(name = "external.hedis.enabled", havingValue = "true")
@Slf4j
public class MeasureRegistrySyncService {

    private final RestTemplate hedisRestTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CircuitBreaker circuitBreaker;
    private final AdapterSpanHelper spanHelper;

    private static final String TOPIC_MEASURES = "external.hedis.measures";

    public MeasureRegistrySyncService(
            @Qualifier("hedisRestTemplate") RestTemplate hedisRestTemplate,
            KafkaTemplate<String, Object> kafkaTemplate,
            CircuitBreakerRegistry registry,
            AdapterSpanHelper spanHelper) {
        this.hedisRestTemplate = hedisRestTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.circuitBreaker = registry.circuitBreaker("hedis-measures");
        this.spanHelper = spanHelper;
    }

    @Scheduled(fixedDelayString = "${external.hedis.measure-sync-interval-ms:3600000}")
    public void syncMeasureRegistry() {
        spanHelper.tracedRun("hedis.measures.sync_registry", () -> {
            log.info("Starting measure registry sync from hedis project");

            try {
                @SuppressWarnings("unchecked")
                Supplier<List<Map<String, Object>>> supplier = CircuitBreaker.decorateSupplier(
                        circuitBreaker,
                        () -> hedisRestTemplate.getForObject(
                                "/api/measures/sync",
                                List.class));

                List<Map<String, Object>> measures = supplier.get();

                if (measures == null || measures.isEmpty()) {
                    log.warn("No measures returned from hedis sync endpoint");
                    return;
                }

                log.info("Synced {} measures from hedis project", measures.size());

                ExternalEventEnvelope<List<Map<String, Object>>> envelope = ExternalEventEnvelope.of(
                        "external.hedis.measures.synced",
                        "hedis-adapter-service",
                        "system",
                        measures,
                        ExternalEventMetadata.builder()
                                .sourceSystem(SourceSystem.HEDIS)
                                .phiLevel(PhiLevel.NONE)
                                .build());

                kafkaTemplate.send(TOPIC_MEASURES, "system", envelope);
            } catch (Exception e) {
                log.error("Failed to sync measure registry: {}", e.getMessage());
            }
        }, "phi.level", "LIMITED");
    }
}
