package com.healthdata.eventsourcing.projection.condition;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * Condition Projection Service - CQRS Read Model
 * Provides diagnosis history and status tracking queries
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConditionProjectionService {

    private final ConditionProjectionRepository repository;

    @Transactional
    public ConditionProjection saveProjection(ConditionProjection projection) {
        log.debug("Saving condition projection for tenant: {}, patient: {}, ICD: {}",
            projection.getTenantId(), projection.getPatientId(), projection.getIcdCode());
        return repository.save(projection);
    }

    public List<ConditionProjection> findByTenantAndPatient(String tenantId, String patientId) {
        log.debug("Finding all conditions for tenant: {}, patient: {}", tenantId, patientId);
        return repository.findByTenantIdAndPatientId(tenantId, patientId);
    }

    public List<ConditionProjection> findActiveConditions(String tenantId, String patientId) {
        log.debug("Finding active conditions for tenant: {}, patient: {}", tenantId, patientId);
        return repository.findByTenantIdAndPatientIdAndStatus(tenantId, patientId, "active");
    }

    public List<ConditionProjection> findByTenantPatientAndStatus(String tenantId, String patientId, String status) {
        log.debug("Finding conditions with status {} for tenant: {}, patient: {}", status, tenantId, patientId);
        return repository.findByTenantIdAndPatientIdAndStatus(tenantId, patientId, status);
    }

    public Optional<ConditionProjection> findByTenantPatientAndIcdCode(String tenantId, String patientId, String icdCode) {
        log.debug("Finding condition by ICD: {} for tenant: {}, patient: {}", icdCode, tenantId, patientId);
        return repository.findByTenantIdAndPatientIdAndIcdCode(tenantId, patientId, icdCode);
    }

    public List<ConditionProjection> findConditionHistory(String tenantId, String patientId, String icdCode) {
        log.debug("Finding history for ICD: {} for tenant: {}, patient: {}", icdCode, tenantId, patientId);
        return repository.findByTenantIdAndPatientIdAndIcdCodeOrderByOnsetDateDesc(tenantId, patientId, icdCode);
    }

    public long countByTenantAndPatient(String tenantId, String patientId) {
        log.debug("Counting conditions for tenant: {}, patient: {}", tenantId, patientId);
        return repository.countByTenantIdAndPatientId(tenantId, patientId);
    }

    public long countActiveConditions(String tenantId, String patientId) {
        log.debug("Counting active conditions for tenant: {}, patient: {}", tenantId, patientId);
        return repository.countByTenantIdAndPatientIdAndStatus(tenantId, patientId, "active");
    }
}
