package com.healthdata.eventsourcing.query.patient;

import com.healthdata.eventsourcing.projection.patient.PatientProjection;
import com.healthdata.eventsourcing.projection.patient.PatientProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Query Service for Patient Projections (CQRS Read Model)
 * Provides multi-tenant aware queries on the patient_projections table
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientQueryService {

    private final PatientProjectionRepository patientRepository;

    /**
     * Find patient by ID and tenant
     * @param patientId The patient ID
     * @param tenantId The tenant ID for isolation
     * @return Optional containing patient if found
     */
    public Optional<PatientProjection> findByIdAndTenant(String patientId, String tenantId) {
        log.debug("Finding patient: {} in tenant: {}", patientId, tenantId);
        return patientRepository.findByPatientIdAndTenantId(patientId, tenantId);
    }

    /**
     * Find all patients for a tenant
     * @param tenantId The tenant ID
     * @return List of patients in tenant
     */
    public List<PatientProjection> findAllByTenant(String tenantId) {
        log.debug("Finding all patients in tenant: {}", tenantId);
        return patientRepository.findByTenantId(tenantId);
    }

    /**
     * Find patient by MRN and tenant
     * @param mrn Medical Record Number
     * @param tenantId The tenant ID for isolation
     * @return Optional containing patient if found
     */
    public Optional<PatientProjection> findByMrnAndTenant(String mrn, String tenantId) {
        log.debug("Finding patient by MRN: {} in tenant: {}", mrn, tenantId);
        return patientRepository.findByMrnAndTenantId(mrn, tenantId);
    }

    /**
     * Find patient by insurance member ID and tenant
     * @param insuranceMemberId The insurance member ID
     * @param tenantId The tenant ID for isolation
     * @return Optional containing patient if found
     */
    public Optional<PatientProjection> findByInsuranceMemberIdAndTenant(String insuranceMemberId, String tenantId) {
        log.debug("Finding patient by insurance member ID: {} in tenant: {}", insuranceMemberId, tenantId);
        return patientRepository.findByInsuranceMemberIdAndTenantId(insuranceMemberId, tenantId);
    }
}
