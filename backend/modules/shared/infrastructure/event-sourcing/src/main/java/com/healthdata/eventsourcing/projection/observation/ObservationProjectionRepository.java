package com.healthdata.eventsourcing.projection.observation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for ObservationProjection
 * Provides time-series and LOINC-specific queries for vital signs
 */
@Repository
public interface ObservationProjectionRepository extends JpaRepository<ObservationProjection, UUID> {

    /**
     * Find all observations for a patient, ordered by date descending
     */
    List<ObservationProjection> findByTenantIdAndPatientIdOrderByObservationDateDesc(
        String tenantId, String patientId);

    /**
     * Find observations by LOINC code for a patient
     */
    List<ObservationProjection> findByTenantIdAndPatientIdAndLoincCodeOrderByObservationDateDesc(
        String tenantId, String patientId, String loincCode);

    /**
     * Find observations within a date range
     */
    List<ObservationProjection> findByTenantIdAndPatientIdAndObservationDateBetweenOrderByObservationDateDesc(
        String tenantId, String patientId, Instant startDate, Instant endDate);

    /**
     * Find the latest observation for a specific LOINC code
     */
    Optional<ObservationProjection> findFirstByTenantIdAndPatientIdAndLoincCodeOrderByObservationDateDesc(
        String tenantId, String patientId, String loincCode);

    /**
     * Count observations for a patient
     */
    long countByTenantIdAndPatientId(String tenantId, String patientId);
}
