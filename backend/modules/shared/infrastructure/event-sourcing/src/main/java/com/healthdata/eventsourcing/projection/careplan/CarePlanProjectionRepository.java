package com.healthdata.eventsourcing.projection.careplan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CarePlanProjectionRepository extends JpaRepository<CarePlanProjection, UUID> {

    List<CarePlanProjection> findByTenantIdAndPatientId(String tenantId, String patientId);

    List<CarePlanProjection> findByTenantIdAndPatientIdAndStatus(String tenantId, String patientId, String status);

    List<CarePlanProjection> findByTenantIdAndCoordinatorId(String tenantId, String coordinatorId);

    Optional<CarePlanProjection> findByTenantIdAndPatientIdAndTitle(String tenantId, String patientId, String title);

    long countByTenantIdAndPatientId(String tenantId, String patientId);

    long countByTenantIdAndPatientIdAndStatus(String tenantId, String patientId, String status);
}
