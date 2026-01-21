package com.healthdata.nurseworkflow.domain.repository;

import com.healthdata.nurseworkflow.domain.model.OutreachLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for OutreachLogEntity
 *
 * Provides data access methods for patient outreach tracking with multi-tenant isolation.
 */
@Repository
public interface OutreachLogRepository extends JpaRepository<OutreachLogEntity, UUID> {

    /**
     * Find all outreach logs for a patient
     */
    Page<OutreachLogEntity> findByTenantIdAndPatientIdOrderByAttemptedAtDesc(
        String tenantId,
        UUID patientId,
        Pageable pageable
    );

    /**
     * Find outreach logs by nurse
     */
    Page<OutreachLogEntity> findByTenantIdAndNurseIdOrderByAttemptedAtDesc(
        String tenantId,
        UUID nurseId,
        Pageable pageable
    );

    /**
     * Find outreach logs by outcome type
     */
    Page<OutreachLogEntity> findByTenantIdAndOutcomeTypeOrderByAttemptedAtDesc(
        String tenantId,
        OutreachLogEntity.OutcomeType outcomeType,
        Pageable pageable
    );

    /**
     * Find outreach logs by reason (post-discharge, medication-reminder, etc.)
     */
    Page<OutreachLogEntity> findByTenantIdAndReasonOrderByAttemptedAtDesc(
        String tenantId,
        String reason,
        Pageable pageable
    );

    /**
     * Find outreach logs with scheduled follow-up within date range
     */
    @Query("""
        SELECT o FROM OutreachLogEntity o
        WHERE o.tenantId = :tenantId
        AND o.scheduledFollowUp IS NOT NULL
        AND o.scheduledFollowUp BETWEEN :startDate AND :endDate
        ORDER BY o.scheduledFollowUp ASC
    """)
    List<OutreachLogEntity> findScheduledFollowUpsByDateRange(
        @Param("tenantId") String tenantId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Find outreach logs for patient within date range
     */
    Page<OutreachLogEntity> findByTenantIdAndPatientIdAndAttemptedAtBetweenOrderByAttemptedAtDesc(
        String tenantId,
        UUID patientId,
        Instant startDate,
        Instant endDate,
        Pageable pageable
    );

    /**
     * Find outreach logs by FHIR Task reference
     */
    OutreachLogEntity findByTenantIdAndTaskId(String tenantId, String taskId);

    /**
     * Count outreach logs for patient
     */
    long countByTenantIdAndPatientId(String tenantId, UUID patientId);

    /**
     * Find successful contacts for patient
     */
    List<OutreachLogEntity> findByTenantIdAndPatientIdAndOutcomeTypeOrderByAttemptedAtDesc(
        String tenantId,
        UUID patientId,
        OutreachLogEntity.OutcomeType outcomeType
    );
}
