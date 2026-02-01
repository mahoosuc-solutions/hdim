package com.healthdata.eventsourcing.projection.patient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Patient Projection Service - CQRS Read Model
 *
 * Provides read-only access to patient projections (materialized views).
 * All queries are tenant-scoped for multi-tenant isolation.
 *
 * ★ Insight ─────────────────────────────────────
 * - Read model is optimized for queries (denormalized)
 * - Write model (commands) handles updates via events
 * - Projections are eventually consistent (from events)
 * - Indexes enable fast searches by tenant, MRN, name
 * ─────────────────────────────────────────────────
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientProjectionService {

    private final PatientProjectionRepository repository;

    /**
     * Save or update a patient projection
     *
     * @param projection Patient projection to save
     * @return Saved projection
     */
    @Transactional
    public PatientProjection saveProjection(PatientProjection projection) {
        log.debug("Saving patient projection for tenant: {}, patient: {}",
            projection.getTenantId(), projection.getPatientId());
        return repository.save(projection);
    }

    /**
     * Find a patient by tenant ID and MRN
     * Primary lookup pattern for patient identification
     *
     * @param tenantId Tenant identifier
     * @param mrn Medical Record Number
     * @return Optional containing patient if found
     */
    public Optional<PatientProjection> findByTenantAndMrn(String tenantId, String mrn) {
        log.debug("Finding patient by tenant: {}, MRN: {}", tenantId, mrn);
        return repository.findByTenantIdAndMrn(tenantId, mrn);
    }

    /**
     * Find all patients for a tenant
     *
     * @param tenantId Tenant identifier
     * @return List of all patients in tenant
     */
    public List<PatientProjection> findAllByTenant(String tenantId) {
        log.debug("Finding all patients for tenant: {}", tenantId);
        return repository.findByTenantId(tenantId);
    }

    /**
     * Count total patients for a tenant
     * Useful for analytics and pagination
     *
     * @param tenantId Tenant identifier
     * @return Count of patients in tenant
     */
    public long countByTenant(String tenantId) {
        log.debug("Counting patients for tenant: {}", tenantId);
        return repository.countByTenantId(tenantId);
    }

    /**
     * Search patients by first name prefix (autocomplete support)
     *
     * @param tenantId Tenant identifier
     * @param firstNamePrefix First name prefix to search
     * @return List of matching patients
     */
    public List<PatientProjection> findByTenantAndFirstNamePrefix(String tenantId, String firstNamePrefix) {
        log.debug("Searching patients by first name prefix: {} for tenant: {}", firstNamePrefix, tenantId);
        return repository.findByTenantIdAndFirstNameStartingWith(tenantId, firstNamePrefix);
    }

    /**
     * Find a patient by aggregate ID within tenant context
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient aggregate ID
     * @return Optional containing patient if found
     */
    public Optional<PatientProjection> findByTenantAndPatientId(String tenantId, String patientId) {
        log.debug("Finding patient by ID: {} for tenant: {}", patientId, tenantId);
        return repository.findByTenantIdAndPatientId(tenantId, patientId);
    }
}
