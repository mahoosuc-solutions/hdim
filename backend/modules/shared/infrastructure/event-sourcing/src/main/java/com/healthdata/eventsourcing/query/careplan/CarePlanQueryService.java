package com.healthdata.eventsourcing.query.careplan;

import com.healthdata.eventsourcing.projection.careplan.CarePlanProjection;
import com.healthdata.eventsourcing.projection.careplan.CarePlanProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarePlanQueryService {

    private final CarePlanProjectionRepository carePlanRepository;

    public List<CarePlanProjection> findByPatientAndTenant(String patientId, String tenantId) {
        log.debug("Finding care plans for patient: {} in tenant: {}", patientId, tenantId);
        return carePlanRepository.findByTenantIdAndPatientId(tenantId, patientId);
    }

    public List<CarePlanProjection> findByTenantAndCoordinator(String tenantId, String coordinatorId) {
        log.debug("Finding care plans by coordinator: {} in tenant: {}", coordinatorId, tenantId);
        return carePlanRepository.findByTenantIdAndCoordinatorId(tenantId, coordinatorId);
    }

    public List<CarePlanProjection> findActiveCarePlansByPatientAndTenant(String patientId, String tenantId) {
        log.debug("Finding active care plans for patient: {} in tenant: {}", patientId, tenantId);
        return carePlanRepository.findByTenantIdAndPatientIdAndStatus(tenantId, patientId, "active");
    }

    public List<CarePlanProjection> findCarePlansByStatusAndTenant(String tenantId, String status) {
        log.debug("Finding care plans with status: {} in tenant: {}", status, tenantId);
        return carePlanRepository.findByTenantIdAndStatus(tenantId, status);
    }

    public Optional<CarePlanProjection> findByPatientAndTenantAndTitle(String patientId, String tenantId, String title) {
        log.debug("Finding care plan titled: {} for patient: {} in tenant: {}", title, patientId, tenantId);
        return carePlanRepository.findByTenantIdAndPatientIdAndTitle(tenantId, patientId, title);
    }

    public List<CarePlanProjection> findAllByTenant(String tenantId) {
        log.debug("Finding all care plans in tenant: {}", tenantId);
        return carePlanRepository.findByTenantId(tenantId);
    }
}
