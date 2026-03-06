package com.healthdata.events.intelligence.consumer;

import com.healthdata.events.intelligence.projection.TenantTrustProjectionService;
import com.healthdata.eventsourcing.intelligence.IntelligenceTopics;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Consumer that updates tenant trust projection when validation findings are emitted.
 */
@Component
@RequiredArgsConstructor
public class ValidationFindingsProjectionConsumer {

    private static final Logger log = LoggerFactory.getLogger(ValidationFindingsProjectionConsumer.class);

    private final TenantTrustProjectionService tenantTrustProjectionService;
    private final ConcurrentMap<String, AtomicLong> consumerLagSecondsByTenant = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicLong> consumerLastProcessedEpochByTenant = new ConcurrentHashMap<>();

    @KafkaListener(
            topics = IntelligenceTopics.VALIDATION_FINDINGS,
            groupId = "tenant-trust-projection-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleValidationFinding(Map<String, Object> findingEvent) {
        Object tenantIdRaw = findingEvent.get("tenantId");
        if (tenantIdRaw == null) {
            log.warn("validation.findings event missing tenantId: {}", findingEvent);
            return;
        }

        String tenantId = String.valueOf(tenantIdRaw);
        recordConsumerMetrics(tenantId, findingEvent);
        tenantTrustProjectionService.refreshForTenant(tenantId);
    }

    private void recordConsumerMetrics(String tenantId, Map<String, Object> findingEvent) {
        AtomicLong lagGauge = consumerLagSecondsByTenant.computeIfAbsent(tenantId, key -> {
            AtomicLong gaugeRef = new AtomicLong(0);
            Gauge.builder("intelligence.validation.findings.consumer.lag.seconds", gaugeRef, AtomicLong::doubleValue)
                    .description("Consumer lag in seconds between finding emission and projection consumption")
                    .tag("tenant_id", tenantId)
                    .register(Metrics.globalRegistry);
            return gaugeRef;
        });

        AtomicLong lastProcessedGauge = consumerLastProcessedEpochByTenant.computeIfAbsent(tenantId, key -> {
            AtomicLong gaugeRef = new AtomicLong(0);
            Gauge.builder("intelligence.validation.findings.consumer.last_processed.epoch.seconds", gaugeRef, AtomicLong::doubleValue)
                    .description("Epoch seconds when validation.findings consumer last processed a message")
                    .tag("tenant_id", tenantId)
                    .register(Metrics.globalRegistry);
            return gaugeRef;
        });

        long nowEpoch = java.time.Instant.now().getEpochSecond();
        long emittedAtEpoch = extractEpochSeconds(findingEvent.get("emittedAtEpochSeconds"), nowEpoch);

        lagGauge.set(Math.max(0, nowEpoch - emittedAtEpoch));
        lastProcessedGauge.set(nowEpoch);
    }

    private long extractEpochSeconds(Object rawValue, long fallback) {
        if (rawValue == null) {
            return fallback;
        }
        if (rawValue instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(rawValue));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
