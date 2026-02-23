package com.healthdata.nurseworkflow.domain.repository;

import com.healthdata.nurseworkflow.domain.model.PatientEngagementMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface PatientEngagementMessageRepository extends JpaRepository<PatientEngagementMessageEntity, UUID> {

    Page<PatientEngagementMessageEntity> findByTenantIdAndThreadIdOrderByCreatedAtAsc(
        String tenantId,
        UUID threadId,
        Pageable pageable
    );

    long countByTenantIdAndCreatedAtBetween(String tenantId, Instant from, Instant to);

    long countByTenantIdAndSenderTypeAndCreatedAtBetween(
        String tenantId,
        PatientEngagementMessageEntity.SenderType senderType,
        Instant from,
        Instant to
    );
}
