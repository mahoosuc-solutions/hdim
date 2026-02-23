package com.healthdata.nurseworkflow.domain.repository;

import com.healthdata.nurseworkflow.domain.model.PatientEngagementEscalationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface PatientEngagementEscalationRepository extends JpaRepository<PatientEngagementEscalationEntity, UUID> {

    long countByTenantIdAndCreatedAtBetween(String tenantId, Instant from, Instant to);

    long countByTenantIdAndSeverityAndCreatedAtBetween(
        String tenantId,
        PatientEngagementEscalationEntity.EscalationSeverity severity,
        Instant from,
        Instant to
    );
}
