package com.healthdata.healthixadapter.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface HealthixAuditLogRepository extends JpaRepository<HealthixAuditLog, UUID> {

    List<HealthixAuditLog> findByTenantIdAndCreatedAtBetween(
            String tenantId, Instant start, Instant end);

    List<HealthixAuditLog> findByPatientId(String patientId);

    long countByTenantIdAndStatus(String tenantId, String status);
}
