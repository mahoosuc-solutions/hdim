package com.healthdata.eventsourcing.projection.observation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Observation Projection Service - CQRS Read Model
 * Provides time-series queries for vital signs data
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ObservationProjectionService {

    private final ObservationProjectionRepository repository;

    /**
     * Save or update an observation projection
     */
    @Transactional
    public ObservationProjection saveProjection(ObservationProjection projection) {
        log.debug("Saving observation projection for tenant: {}, patient: {}, LOINC: {}",
            projection.getTenantId(), projection.getPatientId(), projection.getLoincCode());
        return repository.save(projection);
    }

    /**
     * Find all observations for a patient (ordered by date descending)
     */
    public List<ObservationProjection> findByTenantAndPatient(String tenantId, String patientId) {
        log.debug("Finding observations for tenant: {}, patient: {}", tenantId, patientId);
        return repository.findByTenantIdAndPatientIdOrderByObservationDateDesc(tenantId, patientId);
    }

    /**
     * Find observations by LOINC code for a patient (trend analysis)
     */
    public List<ObservationProjection> findByTenantPatientAndLoinc(
            String tenantId, String patientId, String loincCode) {
        log.debug("Finding observations by LOINC: {} for tenant: {}, patient: {}",
            loincCode, tenantId, patientId);
        return repository.findByTenantIdAndPatientIdAndLoincCodeOrderByObservationDateDesc(
            tenantId, patientId, loincCode);
    }

    /**
     * Find observations within a date range
     */
    public List<ObservationProjection> findByTenantPatientAndDateRange(
            String tenantId, String patientId, Instant startDate, Instant endDate) {
        log.debug("Finding observations in date range {} to {} for tenant: {}, patient: {}",
            startDate, endDate, tenantId, patientId);
        return repository.findByTenantIdAndPatientIdAndObservationDateBetweenOrderByObservationDateDesc(
            tenantId, patientId, startDate, endDate);
    }

    /**
     * Find the latest observation for a LOINC code
     */
    public Optional<ObservationProjection> findLatestByTenantPatientAndLoinc(
            String tenantId, String patientId, String loincCode) {
        log.debug("Finding latest observation for LOINC: {} for tenant: {}, patient: {}",
            loincCode, tenantId, patientId);
        return repository.findFirstByTenantIdAndPatientIdAndLoincCodeOrderByObservationDateDesc(
            tenantId, patientId, loincCode);
    }

    /**
     * Count observations for a patient
     */
    public long countByTenantAndPatient(String tenantId, String patientId) {
        log.debug("Counting observations for tenant: {}, patient: {}", tenantId, patientId);
        return repository.countByTenantIdAndPatientId(tenantId, patientId);
    }
}
