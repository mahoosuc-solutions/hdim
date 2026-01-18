package com.healthdata.eventsourcing.projection.condition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConditionProjectionRepository extends JpaRepository<ConditionProjection, UUID> {

    List<ConditionProjection> findByTenantIdAndPatientId(String tenantId, String patientId);

    List<ConditionProjection> findByTenantIdAndPatientIdAndStatus(String tenantId, String patientId, String status);

    Optional<ConditionProjection> findByTenantIdAndPatientIdAndIcdCode(String tenantId, String patientId, String icdCode);

    List<ConditionProjection> findByTenantIdAndPatientIdAndIcdCodeOrderByOnsetDateDesc(String tenantId, String patientId, String icdCode);

    long countByTenantIdAndPatientId(String tenantId, String patientId);

    long countByTenantIdAndPatientIdAndStatus(String tenantId, String patientId, String status);
}
