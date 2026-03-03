package com.healthdata.ehr.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.ehr.service.EhrConnectionManager;
import com.healthdata.ehr.service.EhrSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Kafka consumer for on-demand EHR sync requests.
 *
 * <p>Listens for {@code ehr.sync.requested} events published by the CQL engine's
 * {@code CqlDataProviderService} when a patient's data is stale or missing.
 * Triggers a sync for the requested patient using the first available connection
 * for the tenant.</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ehr.sync.consumer.enabled", havingValue = "true", matchIfMissing = false)
public class EhrSyncRequestConsumer {

    private final EhrSyncService syncService;
    private final EhrConnectionManager connectionManager;
    private final ObjectMapper objectMapper;

    public EhrSyncRequestConsumer(EhrSyncService syncService,
                                   EhrConnectionManager connectionManager,
                                   ObjectMapper objectMapper) {
        this.syncService = syncService;
        this.connectionManager = connectionManager;
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    @KafkaListener(
            topics = "ehr.sync.requested",
            groupId = "ehr-connector-sync-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSyncRequested(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String tenantId = (String) event.get("tenantId");
            String patientId = (String) event.get("patientId");

            if (tenantId == null || patientId == null) {
                log.warn("Received sync request missing tenantId or patientId, skipping");
                return;
            }

            log.info("Received on-demand sync request for patient {} in tenant {}", patientId, tenantId);

            // Find first available connection for the tenant
            List<String> connectionIds = connectionManager.getConnectionsByTenant(tenantId);
            if (connectionIds.isEmpty()) {
                log.warn("No active EHR connections for tenant {}, cannot fulfill sync request", tenantId);
                return;
            }

            String connectionId = connectionIds.get(0);
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusYears(2); // Default lookback: 2 years

            syncService.syncPatientDataWithPersistence(
                    connectionId, tenantId, patientId, startDate, endDate
            ).subscribe(
                    result -> {
                        if (result.success()) {
                            log.info("On-demand sync completed for patient {}: {} encounters, {} observations",
                                    patientId, result.encountersRetrieved(), result.observationsRetrieved());
                        } else {
                            log.warn("On-demand sync failed for patient {}: {}",
                                    patientId, result.errorMessage());
                        }
                    },
                    error -> log.error("On-demand sync error for patient {}: {}",
                            patientId, error.getMessage())
            );

        } catch (Exception e) {
            log.error("Failed to process sync request: {}", e.getMessage(), e);
        }
    }
}
