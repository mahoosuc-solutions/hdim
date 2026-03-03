package com.healthdata.cql.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.cql.entity.CqlLibrary;
import com.healthdata.cql.repository.CqlLibraryRepository;
import com.healthdata.cql.service.CqlEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Kafka consumer that triggers CQL evaluation when new FHIR data arrives.
 *
 * <p>Listens for persistence events from all data sources:
 * <ul>
 *   <li>{@code ehr.sync.fhir-persisted} — EHR connector sync (Epic/Cerner/Athena)</li>
 *   <li>{@code cdr.hl7v2.fhir-persisted} — CDR HL7 v2 message processing</li>
 *   <li>{@code cdr.cda.fhir-persisted} — CDR CDA document processing</li>
 *   <li>{@code fhir.bulk-import.completed} — Bulk NDJSON import</li>
 *   <li>{@code cms.bcda.ingest-completed} — CMS BCDA bulk export ingest</li>
 * </ul>
 *
 * <p>When data arrives, this consumer finds all active CQL libraries for the
 * tenant and evaluates affected patients against each measure. Results flow to
 * care-gap-service via existing {@code cql.evaluation.completed} events.</p>
 *
 * <p>Flow: FHIR data persisted → this consumer → CqlEvaluationService.batchEvaluate
 * → publishes cql.evaluation.completed → care-gap-service listens</p>
 */
@Service
@ConditionalOnProperty(name = "cql.auto-evaluation.enabled", havingValue = "true", matchIfMissing = false)
public class FhirDataAvailableConsumer {

    private static final Logger logger = LoggerFactory.getLogger(FhirDataAvailableConsumer.class);

    private final CqlEvaluationService evaluationService;
    private final CqlLibraryRepository libraryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public FhirDataAvailableConsumer(
            CqlEvaluationService evaluationService,
            CqlLibraryRepository libraryRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.evaluationService = evaluationService;
        this.libraryRepository = libraryRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "ehr.sync.fhir-persisted",
            groupId = "cql-engine-auto-evaluation",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onEhrSyncPersisted(String message) {
        processDataAvailableEvent(message, "ehr-sync");
    }

    @KafkaListener(
            topics = "cdr.hl7v2.fhir-persisted",
            groupId = "cql-engine-auto-evaluation",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onCdrHl7Persisted(String message) {
        processDataAvailableEvent(message, "cdr-hl7v2");
    }

    @KafkaListener(
            topics = "cdr.cda.fhir-persisted",
            groupId = "cql-engine-auto-evaluation",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onCdrCdaPersisted(String message) {
        processDataAvailableEvent(message, "cdr-cda");
    }

    @KafkaListener(
            topics = "fhir.bulk-import.completed",
            groupId = "cql-engine-auto-evaluation",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onBulkImportCompleted(String message) {
        processDataAvailableEvent(message, "bulk-import");
    }

    @KafkaListener(
            topics = "cms.bcda.ingest-completed",
            groupId = "cql-engine-auto-evaluation",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onCmsBcdaIngestCompleted(String message) {
        processDataAvailableEvent(message, "cms-bcda");
    }

    @SuppressWarnings("unchecked")
    private void processDataAvailableEvent(String message, String source) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String tenantId = (String) event.get("tenantId");

            if (tenantId == null) {
                logger.warn("Received {} event without tenantId, skipping", source);
                return;
            }

            // Extract patient IDs if available (EHR sync and some CDR events include them)
            List<String> patientIdStrings = event.containsKey("patientIds")
                    ? (List<String>) event.get("patientIds")
                    : List.of();

            List<UUID> patientIds = patientIdStrings.stream()
                    .map(this::parseUuidSafe)
                    .filter(id -> id != null)
                    .collect(Collectors.toList());

            logger.info("Processing {} data-available event for tenant: {}, patients: {}",
                    source, tenantId, patientIds.size());

            if (patientIds.isEmpty()) {
                logger.info("No specific patient IDs in {} event for tenant {}; skipping auto-evaluation",
                        source, tenantId);
                return;
            }

            // Find all active CQL libraries for this tenant
            List<CqlLibrary> activeLibraries = libraryRepository.findByTenantIdAndActiveTrue(tenantId);
            if (activeLibraries.isEmpty()) {
                logger.info("No active CQL libraries for tenant: {}, skipping evaluation", tenantId);
                return;
            }

            logger.info("Triggering evaluation of {} patients against {} libraries for tenant: {}",
                    patientIds.size(), activeLibraries.size(), tenantId);

            // Evaluate each library against the affected patients
            for (CqlLibrary library : activeLibraries) {
                try {
                    evaluationService.batchEvaluate(tenantId, library.getId(), patientIds);
                    logger.info("Completed evaluation of library {} for {} patients",
                            library.getLibraryName(), patientIds.size());
                } catch (Exception e) {
                    logger.error("Failed to evaluate library {} for tenant {}: {}",
                            library.getLibraryName(), tenantId, e.getMessage());
                }
            }

            // Publish pipeline completion event
            kafkaTemplate.send("cql.pipeline.auto-evaluation-completed",
                    UUID.randomUUID().toString(),
                    Map.of(
                            "tenantId", tenantId,
                            "source", source,
                            "patientCount", patientIds.size(),
                            "libraryCount", activeLibraries.size(),
                            "timestamp", Instant.now().toString()
                    ));

        } catch (Exception e) {
            logger.error("Failed to process {} data-available event: {}", source, e.getMessage(), e);
        }
    }

    private UUID parseUuidSafe(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            logger.debug("Skipping non-UUID patient ID: {}", value);
            return null;
        }
    }
}
