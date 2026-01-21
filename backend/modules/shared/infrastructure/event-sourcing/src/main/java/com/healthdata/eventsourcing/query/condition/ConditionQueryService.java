package com.healthdata.eventsourcing.query.condition;

import com.healthdata.eventsourcing.projection.condition.ConditionProjection;
import com.healthdata.eventsourcing.projection.condition.ConditionProjectionRepository;
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
public class ConditionQueryService {

    private final ConditionProjectionRepository conditionRepository;

    public List<ConditionProjection> findByPatientAndTenant(String patientId, String tenantId) {
        log.debug("Finding conditions for patient: {} in tenant: {}", patientId, tenantId);
        return conditionRepository.findByPatientIdAndTenantId(patientId, tenantId);
    }

    public List<ConditionProjection> findByIcdCodeAndTenant(String icdCode, String tenantId) {
        log.debug("Finding conditions by ICD: {} in tenant: {}", icdCode, tenantId);
        return conditionRepository.findByIcdCodeAndTenantId(icdCode, tenantId);
    }

    public List<ConditionProjection> findActiveConditionsByPatientAndTenant(String patientId, String tenantId) {
        log.debug("Finding active conditions for patient: {} in tenant: {}", patientId, tenantId);
        return conditionRepository.findByPatientIdAndTenantIdAndStatus(patientId, tenantId, "active");
    }

    public List<ConditionProjection> findAllByTenant(String tenantId) {
        log.debug("Finding all conditions in tenant: {}", tenantId);
        return conditionRepository.findByTenantId(tenantId);
    }
}
