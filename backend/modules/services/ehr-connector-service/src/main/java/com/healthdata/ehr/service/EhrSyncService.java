package com.healthdata.ehr.service;

import com.healthdata.ehr.audit.EhrConnectorAuditIntegration;
import com.healthdata.ehr.connector.EhrConnector;
import com.healthdata.ehr.model.EhrEncounter;
import com.healthdata.ehr.model.EhrObservation;
import com.healthdata.ehr.model.EhrPatient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import ca.uhn.fhir.context.FhirContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for orchestrating data synchronization from EHR systems.
 * Provides high-level operations for retrieving clinical data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EhrSyncService {

    private final EhrConnectionManager connectionManager;
    private final EhrConnectorAuditIntegration ehrConnectorAuditIntegration;
    private final EhrFhirPersistenceService fhirPersistenceService;
    private final FhirContext fhirContext = FhirContext.forR4();

    /**
     * Sync all patient data within a date range.
     *
     * @param connectionId Connection identifier
     * @param tenantId Tenant identifier
     * @param ehrPatientId EHR patient identifier
     * @param startDate Start date for sync
     * @param endDate End date for sync
     * @return Mono containing sync result
     */
    public Mono<EhrConnector.SyncResult> syncPatientData(String connectionId, String tenantId,
                                                          String ehrPatientId,
                                                          LocalDateTime startDate,
                                                          LocalDateTime endDate) {
        log.info("Starting patient data sync for patient {} using connection {}",
                ehrPatientId, connectionId);

        long startTime = System.currentTimeMillis();
        return Mono.fromCallable(() -> getConnectorOrThrow(connectionId, tenantId))
                .flatMap(connector -> connector.syncPatientData(ehrPatientId, startDate, endDate, tenantId))
                .doOnSuccess(result -> {
                    log.info("Patient sync completed: encounters={}, observations={}",
                            result.encountersRetrieved(), result.observationsRetrieved());
                    
                    // Publish audit event
                    ehrConnectorAuditIntegration.publishEhrDataSyncEvent(
                        tenantId,
                        connectionId,
                        "EHR", // Generic vendor name
                        ehrPatientId,
                        startDate,
                        endDate,
                        result.encountersRetrieved(),
                        result.observationsRetrieved(),
                        result.success(),
                        result.errorMessage(),
                        System.currentTimeMillis() - startTime,
                        "system" // User context not available in this method
                    );
                })
                .doOnError(error -> log.error("Patient sync failed for patient {}", ehrPatientId, error));
    }

    /**
     * Get patient information from EHR.
     *
     * @param connectionId Connection identifier
     * @param tenantId Tenant identifier
     * @param ehrPatientId EHR patient identifier
     * @return Mono containing patient information
     */
    public Mono<EhrPatient> getPatient(String connectionId, String tenantId, String ehrPatientId) {
        log.debug("Fetching patient {} from connection {}", ehrPatientId, connectionId);

        long startTime = System.currentTimeMillis();
        return Mono.fromCallable(() -> getConnectorOrThrow(connectionId, tenantId))
                .flatMap(connector -> connector.getPatient(ehrPatientId, tenantId))
                .doOnSuccess(patient -> {
                    ehrConnectorAuditIntegration.publishEhrPatientFetchEvent(
                        tenantId, connectionId, "EHR", ehrPatientId,
                        patient != null, null, System.currentTimeMillis() - startTime, "system"
                    );
                })
                .doOnError(error -> {
                    ehrConnectorAuditIntegration.publishEhrPatientFetchEvent(
                        tenantId, connectionId, "EHR", ehrPatientId,
                        false, error.getMessage(), System.currentTimeMillis() - startTime, "system"
                    );
                });
    }

    /**
     * Search for patients by criteria.
     *
     * @param connectionId Connection identifier
     * @param tenantId Tenant identifier
     * @param familyName Family name
     * @param givenName Given name
     * @param dateOfBirth Date of birth
     * @return Flux of matching patients
     */
    public Flux<EhrPatient> searchPatients(String connectionId, String tenantId,
                                            String familyName, String givenName,
                                            LocalDateTime dateOfBirth) {
        log.debug("Searching patients: family={}, given={}", familyName, givenName);

        EhrConnector connector = getConnectorOrThrow(connectionId, tenantId);
        return connector.searchPatients(familyName, givenName, dateOfBirth, tenantId);
    }

    /**
     * Get encounters for a patient.
     *
     * @param connectionId Connection identifier
     * @param tenantId Tenant identifier
     * @param ehrPatientId EHR patient identifier
     * @param startDate Start date
     * @param endDate End date
     * @return Flux of encounters
     */
    public Flux<EhrEncounter> getEncounters(String connectionId, String tenantId,
                                             String ehrPatientId,
                                             LocalDateTime startDate,
                                             LocalDateTime endDate) {
        log.debug("Fetching encounters for patient {}", ehrPatientId);

        EhrConnector connector = getConnectorOrThrow(connectionId, tenantId);
        return connector.getEncounters(ehrPatientId, startDate, endDate, tenantId);
    }

    /**
     * Get a specific encounter.
     *
     * @param connectionId Connection identifier
     * @param tenantId Tenant identifier
     * @param ehrEncounterId EHR encounter identifier
     * @return Mono containing encounter
     */
    public Mono<EhrEncounter> getEncounter(String connectionId, String tenantId,
                                            String ehrEncounterId) {
        log.debug("Fetching encounter {}", ehrEncounterId);

        return Mono.fromCallable(() -> getConnectorOrThrow(connectionId, tenantId))
                .flatMap(connector -> connector.getEncounter(ehrEncounterId, tenantId));
    }

    /**
     * Get observations for a patient.
     *
     * @param connectionId Connection identifier
     * @param tenantId Tenant identifier
     * @param ehrPatientId EHR patient identifier
     * @param category Observation category
     * @param startDate Start date
     * @param endDate End date
     * @return Flux of observations
     */
    public Flux<EhrObservation> getObservations(String connectionId, String tenantId,
                                                 String ehrPatientId, String category,
                                                 LocalDateTime startDate,
                                                 LocalDateTime endDate) {
        log.debug("Fetching observations for patient {}, category={}", ehrPatientId, category);

        EhrConnector connector = getConnectorOrThrow(connectionId, tenantId);
        return connector.getObservations(ehrPatientId, category, startDate, endDate, tenantId);
    }

    /**
     * Get observations for an encounter.
     *
     * @param connectionId Connection identifier
     * @param tenantId Tenant identifier
     * @param ehrEncounterId EHR encounter identifier
     * @return Flux of observations
     */
    public Flux<EhrObservation> getObservationsByEncounter(String connectionId, String tenantId,
                                                            String ehrEncounterId) {
        log.debug("Fetching observations for encounter {}", ehrEncounterId);

        EhrConnector connector = getConnectorOrThrow(connectionId, tenantId);
        return connector.getObservationsByEncounter(ehrEncounterId, tenantId);
    }

    /**
     * Sync patient data and persist the resulting FHIR resources to the FHIR store.
     *
     * <p>Collects encounters and observations as raw FHIR resources, assembles them
     * into a transaction Bundle, and forwards to the FHIR store via
     * {@link EhrFhirPersistenceService}.</p>
     *
     * @param connectionId Connection identifier
     * @param tenantId Tenant identifier
     * @param ehrPatientId EHR patient identifier
     * @param startDate Start date for sync
     * @param endDate End date for sync
     * @return Mono containing sync result
     */
    public Mono<EhrConnector.SyncResult> syncPatientDataWithPersistence(
            String connectionId, String tenantId,
            String ehrPatientId,
            LocalDateTime startDate, LocalDateTime endDate) {

        log.info("Starting patient data sync with FHIR persistence for patient {} using connection {}",
                ehrPatientId, connectionId);

        EhrConnector connector = getConnectorOrThrow(connectionId, tenantId);
        long startTime = System.currentTimeMillis();

        return Mono.zip(
                connector.getEncounters(ehrPatientId, startDate, endDate, tenantId).collectList(),
                connector.getObservations(ehrPatientId, null, startDate, endDate, tenantId).collectList()
        ).flatMap(tuple -> {
            List<EhrEncounter> encounters = tuple.getT1();
            List<EhrObservation> observations = tuple.getT2();

            // Build a FHIR transaction Bundle from raw resources stored in each model
            Bundle bundle = buildFhirBundle(encounters, observations);

            EhrConnector.SyncResult result = new EhrConnector.SyncResult(
                    ehrPatientId,
                    encounters.size(),
                    observations.size(),
                    startDate != null ? startDate : LocalDateTime.now(),
                    LocalDateTime.now(),
                    true,
                    null
            );

            if (bundle.hasEntry()) {
                try {
                    fhirPersistenceService.persistBundle(bundle, tenantId, List.of(ehrPatientId));
                    log.info("Persisted {} FHIR resources for patient {}", bundle.getEntry().size(), ehrPatientId);
                } catch (Exception e) {
                    log.warn("FHIR persistence failed (non-blocking) for patient {}: {}", ehrPatientId, e.getMessage());
                }
            }

            // Publish audit event
            ehrConnectorAuditIntegration.publishEhrDataSyncEvent(
                    tenantId, connectionId, "EHR", ehrPatientId,
                    startDate, endDate,
                    encounters.size(), observations.size(),
                    true, null,
                    System.currentTimeMillis() - startTime, "system"
            );

            return Mono.just(result);
        }).onErrorResume(error -> {
            log.error("Patient sync with persistence failed for patient {}", ehrPatientId, error);
            return Mono.just(new EhrConnector.SyncResult(
                    ehrPatientId, 0, 0,
                    startDate != null ? startDate : LocalDateTime.now(),
                    LocalDateTime.now(), false, error.getMessage()
            ));
        });
    }

    /**
     * Build a FHIR transaction Bundle from EHR model objects.
     * Each EhrEncounter and EhrObservation stores the raw FHIR JSON in {@code rawFhirResource}.
     */
    private Bundle buildFhirBundle(List<EhrEncounter> encounters, List<EhrObservation> observations) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);

        for (EhrEncounter encounter : encounters) {
            if (encounter.getRawFhirResource() != null) {
                Resource resource = (Resource) fhirContext.newJsonParser()
                        .parseResource(encounter.getRawFhirResource());
                bundle.addEntry()
                        .setResource(resource)
                        .getRequest()
                        .setMethod(Bundle.HTTPVerb.PUT)
                        .setUrl("Encounter/" + encounter.getEhrEncounterId());
            }
        }

        for (EhrObservation observation : observations) {
            if (observation.getRawFhirResource() != null) {
                Resource resource = (Resource) fhirContext.newJsonParser()
                        .parseResource(observation.getRawFhirResource());
                bundle.addEntry()
                        .setResource(resource)
                        .getRequest()
                        .setMethod(Bundle.HTTPVerb.PUT)
                        .setUrl("Observation/" + observation.getEhrObservationId());
            }
        }

        return bundle;
    }

    /**
     * Get connector or throw exception if not found.
     */
    private EhrConnector getConnectorOrThrow(String connectionId, String tenantId) {
        EhrConnector connector = connectionManager.getConnection(connectionId, tenantId);
        if (connector == null) {
            throw new IllegalArgumentException(
                    "Connection not found: " + connectionId + " for tenant: " + tenantId);
        }
        return connector;
    }
}
