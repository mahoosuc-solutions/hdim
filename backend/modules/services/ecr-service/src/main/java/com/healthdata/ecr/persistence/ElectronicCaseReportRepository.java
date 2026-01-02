package com.healthdata.ecr.persistence;

import com.healthdata.ecr.persistence.ElectronicCaseReportEntity.EcrStatus;
import com.healthdata.ecr.persistence.ElectronicCaseReportEntity.EcrUrgency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElectronicCaseReportRepository extends JpaRepository<ElectronicCaseReportEntity, UUID> {

    Optional<ElectronicCaseReportEntity> findByTenantIdAndId(String tenantId, UUID id);

    List<ElectronicCaseReportEntity> findByTenantIdAndPatientId(String tenantId, UUID patientId);

    Page<ElectronicCaseReportEntity> findByTenantIdAndStatus(
        String tenantId, EcrStatus status, Pageable pageable);

    /**
     * Find eCRs pending transmission, ordered by urgency
     */
    @Query("""
        SELECT e FROM ElectronicCaseReportEntity e
        WHERE e.tenantId = :tenantId
        AND e.status = 'PENDING'
        ORDER BY
            CASE e.urgency
                WHEN 'IMMEDIATE' THEN 1
                WHEN 'WITHIN_24_HOURS' THEN 2
                WHEN 'WITHIN_72_HOURS' THEN 3
                ELSE 4
            END,
            e.triggerDetectedAt ASC
        """)
    List<ElectronicCaseReportEntity> findPendingByUrgency(@Param("tenantId") String tenantId);

    /**
     * Find eCRs ready for retry
     */
    @Query("""
        SELECT e FROM ElectronicCaseReportEntity e
        WHERE e.status = 'FAILED'
        AND e.retryCount < :maxRetries
        AND e.nextRetryAt <= :now
        """)
    List<ElectronicCaseReportEntity> findReadyForRetry(
        @Param("maxRetries") int maxRetries,
        @Param("now") LocalDateTime now);

    /**
     * Find eCRs awaiting Reportability Response
     */
    @Query("""
        SELECT e FROM ElectronicCaseReportEntity e
        WHERE e.status = 'SUBMITTED'
        AND e.submittedAt < :cutoffTime
        """)
    List<ElectronicCaseReportEntity> findAwaitingResponse(
        @Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find by AIMS tracking ID
     */
    Optional<ElectronicCaseReportEntity> findByAimsTrackingId(String aimsTrackingId);

    /**
     * Check if a trigger already has a pending/submitted eCR to avoid duplicates
     */
    @Query("""
        SELECT COUNT(e) > 0 FROM ElectronicCaseReportEntity e
        WHERE e.tenantId = :tenantId
        AND e.patientId = :patientId
        AND e.triggerCode = :triggerCode
        AND e.status IN ('PENDING', 'GENERATING', 'TRANSMITTING', 'SUBMITTED')
        AND e.triggerDetectedAt > :since
        """)
    boolean existsPendingForTrigger(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("triggerCode") String triggerCode,
        @Param("since") LocalDateTime since);

    /**
     * Count by status for monitoring dashboard
     */
    @Query("""
        SELECT e.status, COUNT(e) FROM ElectronicCaseReportEntity e
        WHERE e.tenantId = :tenantId
        GROUP BY e.status
        """)
    List<Object[]> countByStatus(@Param("tenantId") String tenantId);

    /**
     * Find immediate urgency eCRs that haven't been transmitted
     */
    @Query("""
        SELECT e FROM ElectronicCaseReportEntity e
        WHERE e.urgency = 'IMMEDIATE'
        AND e.status IN ('PENDING', 'GENERATING')
        AND e.triggerDetectedAt < :threshold
        """)
    List<ElectronicCaseReportEntity> findOverdueImmediateReports(
        @Param("threshold") LocalDateTime threshold);

    /**
     * Update status with optimistic locking
     */
    @Modifying
    @Query("""
        UPDATE ElectronicCaseReportEntity e
        SET e.status = :newStatus, e.updatedAt = :now
        WHERE e.id = :id AND e.status = :currentStatus
        """)
    int updateStatus(
        @Param("id") UUID id,
        @Param("currentStatus") EcrStatus currentStatus,
        @Param("newStatus") EcrStatus newStatus,
        @Param("now") LocalDateTime now);
}
