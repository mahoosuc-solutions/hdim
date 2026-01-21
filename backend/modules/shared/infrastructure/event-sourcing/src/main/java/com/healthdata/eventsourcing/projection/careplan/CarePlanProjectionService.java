package com.healthdata.eventsourcing.projection.careplan;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * CarePlan Projection Service - CQRS Read Model
 * Provides care coordination and team queries
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarePlanProjectionService {

    private final CarePlanProjectionRepository repository;

    @Transactional
    public CarePlanProjection saveProjection(CarePlanProjection projection) {
        log.debug("Saving care plan projection for tenant: {}, patient: {}, title: {}",
            projection.getTenantId(), projection.getPatientId(), projection.getTitle());
        return repository.save(projection);
    }

    public List<CarePlanProjection> findByTenantAndPatient(String tenantId, String patientId) {
        log.debug("Finding care plans for tenant: {}, patient: {}", tenantId, patientId);
        return repository.findByTenantIdAndPatientId(tenantId, patientId);
    }

    public List<CarePlanProjection> findActiveCarePlans(String tenantId, String patientId) {
        log.debug("Finding active care plans for tenant: {}, patient: {}", tenantId, patientId);
        return repository.findByTenantIdAndPatientIdAndStatus(tenantId, patientId, "active");
    }

    public List<CarePlanProjection> findByTenantPatientAndStatus(String tenantId, String patientId, String status) {
        log.debug("Finding care plans with status {} for tenant: {}, patient: {}", status, tenantId, patientId);
        return repository.findByTenantIdAndPatientIdAndStatus(tenantId, patientId, status);
    }

    public List<CarePlanProjection> findByTenantAndCoordinator(String tenantId, String coordinatorId) {
        log.debug("Finding care plans for coordinator: {} in tenant: {}", coordinatorId, tenantId);
        return repository.findByTenantIdAndCoordinatorId(tenantId, coordinatorId);
    }

    public Optional<CarePlanProjection> findByTenantPatientAndTitle(String tenantId, String patientId, String title) {
        log.debug("Finding care plan by title: {} for tenant: {}, patient: {}", title, tenantId, patientId);
        return repository.findByTenantIdAndPatientIdAndTitle(tenantId, patientId, title);
    }

    public long countByTenantAndPatient(String tenantId, String patientId) {
        log.debug("Counting care plans for tenant: {}, patient: {}", tenantId, patientId);
        return repository.countByTenantIdAndPatientId(tenantId, patientId);
    }

    public long countActiveCarePlans(String tenantId, String patientId) {
        log.debug("Counting active care plans for tenant: {}, patient: {}", tenantId, patientId);
        return repository.countByTenantIdAndPatientIdAndStatus(tenantId, patientId, "active");
    }
}
