package com.healthdata.cql.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * CQL data provider that checks for stale patient data before evaluation.
 *
 * <p>Before querying the FHIR store for a patient, this service checks a Redis
 * key {@code ehr:lastsync:{tenantId}:{patientId}}. If the key is absent or older
 * than 24 hours, it publishes an {@code ehr.sync.requested} Kafka event so
 * the EHR connector service can pull fresh data on demand.</p>
 *
 * <p>This enables "lazy fetch" — CQL evaluation triggers on-demand EHR pulls
 * for patients not yet in the FHIR store or with stale data.</p>
 */
@Service
@ConditionalOnProperty(name = "cql.lazy-fetch.enabled", havingValue = "true", matchIfMissing = false)
public class CqlDataProviderService {

    private static final Logger logger = LoggerFactory.getLogger(CqlDataProviderService.class);
    private static final Duration STALE_THRESHOLD = Duration.ofHours(24);
    private static final String REDIS_KEY_PREFIX = "ehr:lastsync:";

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CqlDataProviderService(StringRedisTemplate redisTemplate,
                                   KafkaTemplate<String, Object> kafkaTemplate) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Check if patient data is fresh. If stale or absent, request an EHR sync.
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @return true if data is fresh (no sync needed), false if sync was requested
     */
    public boolean ensureFreshData(String tenantId, String patientId) {
        String key = REDIS_KEY_PREFIX + tenantId + ":" + patientId;
        String lastSyncValue = redisTemplate.opsForValue().get(key);

        if (lastSyncValue != null) {
            try {
                Instant lastSync = Instant.parse(lastSyncValue);
                if (Duration.between(lastSync, Instant.now()).compareTo(STALE_THRESHOLD) < 0) {
                    logger.debug("Patient {} data is fresh (last sync: {})", patientId, lastSyncValue);
                    return true;
                }
                logger.info("Patient {} data is stale (last sync: {}), requesting refresh", patientId, lastSyncValue);
            } catch (Exception e) {
                logger.warn("Invalid last-sync timestamp for patient {}: {}", patientId, lastSyncValue);
            }
        } else {
            logger.info("No sync record for patient {} in tenant {}, requesting initial sync", patientId, tenantId);
        }

        requestEhrSync(tenantId, patientId);
        return false;
    }

    /**
     * Record that a patient's data was synced (called by EHR sync completion handler).
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     */
    public void recordSync(String tenantId, String patientId) {
        String key = REDIS_KEY_PREFIX + tenantId + ":" + patientId;
        redisTemplate.opsForValue().set(key, Instant.now().toString(), STALE_THRESHOLD);
        logger.debug("Recorded sync for patient {} in tenant {}", patientId, tenantId);
    }

    private void requestEhrSync(String tenantId, String patientId) {
        kafkaTemplate.send("ehr.sync.requested",
                UUID.randomUUID().toString(),
                Map.of(
                        "tenantId", tenantId,
                        "patientId", patientId,
                        "requestedAt", Instant.now().toString(),
                        "reason", "stale-data"
                ));
        logger.info("Published ehr.sync.requested for patient {} in tenant {}", patientId, tenantId);
    }
}
