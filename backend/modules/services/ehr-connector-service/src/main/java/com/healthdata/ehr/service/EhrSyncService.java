package com.healthdata.ehr.service;

import com.healthdata.ehr.connector.EhrConnector;
import com.healthdata.ehr.model.EhrEncounter;
import com.healthdata.ehr.model.EhrObservation;
import com.healthdata.ehr.model.EhrPatient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Service for orchestrating data synchronization from EHR systems.
 * Provides high-level operations for retrieving clinical data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EhrSyncService {

    private final EhrConnectionManager connectionManager;

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

        return Mono.fromCallable(() -> getConnectorOrThrow(connectionId, tenantId))
                .flatMap(connector -> connector.syncPatientData(ehrPatientId, startDate, endDate, tenantId))
                .doOnSuccess(result -> log.info("Patient sync completed: encounters={}, observations={}",
                        result.encountersRetrieved(), result.observationsRetrieved()))
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

        return Mono.fromCallable(() -> getConnectorOrThrow(connectionId, tenantId))
                .flatMap(connector -> connector.getPatient(ehrPatientId, tenantId));
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
