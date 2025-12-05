package com.healthdata.ehr.connector;

import com.healthdata.ehr.dto.EhrConnectionConfig;
import com.healthdata.ehr.model.EhrConnectionStatus;
import com.healthdata.ehr.model.EhrEncounter;
import com.healthdata.ehr.model.EhrObservation;
import com.healthdata.ehr.model.EhrPatient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Base contract for all EHR connector implementations.
 * Defines the common operations that all EHR connectors must support.
 *
 * Implementations should handle vendor-specific authentication, API calls,
 * and data transformation to the normalized EHR model.
 */
public interface EhrConnector {

    /**
     * Initialize the connector with the given configuration.
     *
     * @param config Connection configuration
     * @return Mono that completes when initialization is successful
     */
    Mono<Void> initialize(EhrConnectionConfig config);

    /**
     * Test the connection to the EHR system.
     *
     * @return Connection status
     */
    Mono<EhrConnectionStatus> testConnection();

    /**
     * Disconnect and cleanup resources.
     *
     * @return Mono that completes when disconnection is successful
     */
    Mono<Void> disconnect();

    /**
     * Get the current connection status.
     *
     * @return Current connection status
     */
    Mono<EhrConnectionStatus> getConnectionStatus();

    /**
     * Retrieve a patient by their EHR-specific identifier.
     *
     * @param ehrPatientId EHR-specific patient identifier
     * @param tenantId Tenant identifier
     * @return Patient information
     */
    Mono<EhrPatient> getPatient(String ehrPatientId, String tenantId);

    /**
     * Search for patients by various criteria.
     *
     * @param familyName Patient's family name
     * @param givenName Patient's given name
     * @param dateOfBirth Date of birth
     * @param tenantId Tenant identifier
     * @return Flux of matching patients
     */
    Flux<EhrPatient> searchPatients(String familyName, String givenName,
                                     LocalDateTime dateOfBirth, String tenantId);

    /**
     * Retrieve encounters for a specific patient.
     *
     * @param ehrPatientId EHR-specific patient identifier
     * @param startDate Start date for encounter search
     * @param endDate End date for encounter search
     * @param tenantId Tenant identifier
     * @return Flux of encounters
     */
    Flux<EhrEncounter> getEncounters(String ehrPatientId, LocalDateTime startDate,
                                      LocalDateTime endDate, String tenantId);

    /**
     * Retrieve a specific encounter by ID.
     *
     * @param ehrEncounterId EHR-specific encounter identifier
     * @param tenantId Tenant identifier
     * @return Encounter information
     */
    Mono<EhrEncounter> getEncounter(String ehrEncounterId, String tenantId);

    /**
     * Retrieve observations/lab results for a specific patient.
     *
     * @param ehrPatientId EHR-specific patient identifier
     * @param category Observation category (e.g., "laboratory", "vital-signs")
     * @param startDate Start date for observation search
     * @param endDate End date for observation search
     * @param tenantId Tenant identifier
     * @return Flux of observations
     */
    Flux<EhrObservation> getObservations(String ehrPatientId, String category,
                                          LocalDateTime startDate, LocalDateTime endDate,
                                          String tenantId);

    /**
     * Retrieve observations for a specific encounter.
     *
     * @param ehrEncounterId EHR-specific encounter identifier
     * @param tenantId Tenant identifier
     * @return Flux of observations
     */
    Flux<EhrObservation> getObservationsByEncounter(String ehrEncounterId, String tenantId);

    /**
     * Sync all data for a patient within a date range.
     * This is a convenience method that retrieves patient, encounters, and observations.
     *
     * @param ehrPatientId EHR-specific patient identifier
     * @param startDate Start date for data sync
     * @param endDate End date for data sync
     * @param tenantId Tenant identifier
     * @return Mono containing sync result metadata
     */
    Mono<SyncResult> syncPatientData(String ehrPatientId, LocalDateTime startDate,
                                      LocalDateTime endDate, String tenantId);

    /**
     * Get the configuration used by this connector.
     *
     * @return Connection configuration
     */
    EhrConnectionConfig getConfig();

    /**
     * Result of a data sync operation.
     */
    record SyncResult(
            String patientId,
            int encountersRetrieved,
            int observationsRetrieved,
            LocalDateTime syncStartTime,
            LocalDateTime syncEndTime,
            boolean success,
            String errorMessage
    ) {}
}
