package com.healthdata.nurseworkflow.domain.repository;

import com.healthdata.nurseworkflow.domain.model.PatientEngagementThreadEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PatientEngagementThreadRepository extends JpaRepository<PatientEngagementThreadEntity, UUID> {

    Optional<PatientEngagementThreadEntity> findByIdAndTenantId(UUID id, String tenantId);

    Page<PatientEngagementThreadEntity> findByTenantIdOrderByLastMessageAtDesc(String tenantId, Pageable pageable);

    long countByTenantIdAndCreatedAtBetween(String tenantId, Instant from, Instant to);

    long countByTenantIdAndStatus(String tenantId, PatientEngagementThreadEntity.ThreadStatus status);
}
