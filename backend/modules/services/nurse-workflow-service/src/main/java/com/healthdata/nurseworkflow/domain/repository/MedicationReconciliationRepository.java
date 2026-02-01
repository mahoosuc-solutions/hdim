package com.healthdata.nurseworkflow.domain.repository;

import com.healthdata.nurseworkflow.domain.model.MedicationReconciliationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MedicationReconciliationEntity
 *
 * Provides data access for medication reconciliation tracking with multi-tenant isolation.
 */
@Repository
public interface MedicationReconciliationRepository extends JpaRepository<MedicationReconciliationEntity, UUID> {

    /**
     * Find medication reconciliations for a patient
     */
    Page<MedicationReconciliationEntity> findByTenantIdAndPatientIdOrderByStartedAtDesc(
        String tenantId,
        UUID patientId,
        Pageable pageable
    );

    /**
     * Find pending medication reconciliations (not completed)
     */
    @Query("""
        SELECT m FROM MedicationReconciliationEntity m
        WHERE m.tenantId = :tenantId
        AND m.status IN ('REQUESTED', 'IN_PROGRESS')
        ORDER BY m.startedAt ASC
    """)
    Page<MedicationReconciliationEntity> findPendingByTenant(
        @Param("tenantId") String tenantId,
        Pageable pageable
    );

    /**
     * Find pending medication reconciliations for specific patient
     */
    List<MedicationReconciliationEntity> findByTenantIdAndPatientIdAndStatusOrderByStartedAtAsc(
        String tenantId,
        UUID patientId,
        MedicationReconciliationEntity.ReconciliationStatus status
    );

    /**
     * Find medication reconciliations by trigger type
     */
    Page<MedicationReconciliationEntity> findByTenantIdAndTriggerTypeOrderByStartedAtDesc(
        String tenantId,
        MedicationReconciliationEntity.TriggerType triggerType,
        Pageable pageable
    );

    /**
     * Find medication reconciliations by reconciler (nurse)
     */
    Page<MedicationReconciliationEntity> findByTenantIdAndReconcilerIdOrderByStartedAtDesc(
        String tenantId,
        UUID reconcilerId,
        Pageable pageable
    );

    /**
     * Find completed medication reconciliations within date range
     */
    Page<MedicationReconciliationEntity> findByTenantIdAndStatusAndCompletedAtBetweenOrderByCompletedAtDesc(
        String tenantId,
        MedicationReconciliationEntity.ReconciliationStatus status,
        Instant startDate,
        Instant endDate,
        Pageable pageable
    );

    /**
     * Find medication reconciliation by FHIR Task reference
     */
    Optional<MedicationReconciliationEntity> findByTenantIdAndTaskId(String tenantId, String taskId);

    /**
     * Count pending medication reconciliations for tenant
     */
    long countByTenantIdAndStatusIn(
        String tenantId,
        java.util.Collection<MedicationReconciliationEntity.ReconciliationStatus> statuses
    );

    /**
     * Find medication reconciliations with poor patient understanding (need follow-up)
     */
    @Query("""
        SELECT m FROM MedicationReconciliationEntity m
        WHERE m.tenantId = :tenantId
        AND m.status = 'COMPLETED'
        AND m.patientUnderstanding IN ('FAIR', 'POOR')
        ORDER BY m.completedAt DESC
    """)
    List<MedicationReconciliationEntity> findWithPoorUnderstanding(
        @Param("tenantId") String tenantId
    );
}
