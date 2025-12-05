package com.healthdata.caregap.repository;

import com.healthdata.caregap.domain.CareGap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Care Gap entities
 * Provides comprehensive access to patient care gaps and quality opportunities
 * Supports complex queries, pagination, tenant isolation, and bulk updates
 * Used for gap detection, closure tracking, and quality improvement initiatives
 */
@Repository
public interface CareGapRepository extends JpaRepository<CareGap, String> {

    /**
     * Find care gaps by patient ID
     *
     * @param patientId Patient identifier
     * @return List of all care gaps for the patient
     */
    List<CareGap> findByPatientId(String patientId);

    /**
     * Find care gaps by patient ID and status
     * Status values include: OPEN, IN_PROGRESS, CLOSED
     *
     * @param patientId Patient identifier
     * @param status Care gap status
     * @return List of gaps with the specified status
     */
    List<CareGap> findByPatientIdAndStatus(String patientId, String status);

    /**
     * Find open care gaps for a patient
     * Critical for identifying actionable quality opportunities
     *
     * @param patientId Patient identifier
     * @return List of open care gaps
     */
    @Query("""
        SELECT cg FROM CareGap cg
        WHERE cg.patientId = :patientId
        AND cg.status = 'OPEN'
        ORDER BY cg.priority DESC, cg.dueDate ASC
        """)
    List<CareGap> findOpenGapsByPatient(@Param("patientId") String patientId);

    /**
     * Find care gaps by patient ID and gap type
     *
     * @param patientId Patient identifier
     * @param gapType Gap type (PREVENTIVE_CARE, CHRONIC_DISEASE_MONITORING, MEDICATION_ADHERENCE, CANCER_SCREENING)
     * @return List of gaps with the specified type
     */
    List<CareGap> findByPatientIdAndGapType(String patientId, String gapType);

    /**
     * Find care gaps by patient ID and priority
     *
     * @param patientId Patient identifier
     * @param priority Priority level (HIGH, MEDIUM, LOW)
     * @return List of gaps with the specified priority
     */
    List<CareGap> findByPatientIdAndPriority(String patientId, String priority);

    /**
     * Find gaps by type and priority with pagination and tenant isolation
     * Used for population-level gap management and prioritization
     *
     * @param gapType Type of care gap
     * @param priority Priority level
     * @param tenantId Tenant identifier
     * @param pageable Pagination information
     * @return Paginated list of gaps matching criteria
     */
    @Query("""
        SELECT cg FROM CareGap cg
        WHERE cg.gapType = :gapType
        AND cg.priority = :priority
        AND cg.tenantId = :tenantId
        AND cg.status = 'OPEN'
        ORDER BY cg.dueDate ASC
        """)
    Page<CareGap> findGapsByTypeAndPriority(
        @Param("gapType") String gapType,
        @Param("priority") String priority,
        @Param("tenantId") String tenantId,
        Pageable pageable
    );

    /**
     * Find overdue care gaps
     * Critical for identifying delinquent quality opportunities
     *
     * @param tenantId Tenant identifier
     * @return List of gaps with due date in the past
     */
    @Query("""
        SELECT cg FROM CareGap cg
        WHERE cg.tenantId = :tenantId
        AND cg.dueDate < CURRENT_TIMESTAMP
        AND cg.status = 'OPEN'
        ORDER BY cg.dueDate ASC
        """)
    List<CareGap> findOverdueGaps(@Param("tenantId") String tenantId);

    /**
     * Find care gaps by patient ID and due date range
     *
     * @param patientId Patient identifier
     * @param startDate Start of due date range
     * @param endDate End of due date range
     * @return List of gaps with due dates in the range
     */
    @Query("""
        SELECT cg FROM CareGap cg
        WHERE cg.patientId = :patientId
        AND cg.dueDate BETWEEN :startDate AND :endDate
        ORDER BY cg.dueDate ASC
        """)
    List<CareGap> findByPatientIdAndDueDateBetween(
        @Param("patientId") String patientId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find care gaps by provider ID
     *
     * @param providerId Provider identifier
     * @return List of gaps assigned to the provider
     */
    List<CareGap> findByProviderId(String providerId);

    /**
     * Find care gaps by care team ID
     *
     * @param careTeamId Care team identifier
     * @return List of gaps assigned to the care team
     */
    List<CareGap> findByCareTeamId(String careTeamId);

    /**
     * Find care gaps by measure ID
     * Used to identify gaps related to specific quality measures
     *
     * @param measureId Quality measure identifier
     * @return List of gaps associated with the measure
     */
    List<CareGap> findByMeasureId(String measureId);

    /**
     * Count open care gaps for a patient
     *
     * @param patientId Patient identifier
     * @return Count of open gaps
     */
    @Query("""
        SELECT COUNT(cg) FROM CareGap cg
        WHERE cg.patientId = :patientId
        AND cg.status = 'OPEN'
        """)
    Long countOpenGapsByPatient(@Param("patientId") String patientId);

    /**
     * Find high priority open care gaps
     * Used for prioritized gap closure workflows
     *
     * @return List of high priority open gaps ordered by due date
     */
    @Query("""
        SELECT cg FROM CareGap cg
        WHERE cg.status = 'OPEN'
        AND cg.priority = 'HIGH'
        ORDER BY cg.dueDate ASC
        """)
    List<CareGap> findHighPriorityOpenGaps();

    /**
     * Count open gaps grouped by gap type
     * Provides metrics for gap distribution analysis
     *
     * @return Object array containing gap type and count
     */
    @Query("""
        SELECT cg.gapType, COUNT(cg)
        FROM CareGap cg
        WHERE cg.status = 'OPEN'
        GROUP BY cg.gapType
        """)
    List<Object[]> countOpenGapsByType();

    /**
     * Find care gaps by tenant ID
     *
     * @param tenantId Tenant identifier
     * @return List of gaps for the tenant
     */
    List<CareGap> findByTenantId(String tenantId);

    /**
     * Find high-risk care gaps
     * Used for risk-based prioritization and early intervention
     *
     * @param minScore Minimum risk score threshold
     * @return List of gaps with risk score above threshold
     */
    @Query("""
        SELECT cg FROM CareGap cg
        WHERE cg.riskScore >= :minScore
        AND cg.status = 'OPEN'
        ORDER BY cg.riskScore DESC
        """)
    List<CareGap> findHighRiskGaps(@Param("minScore") Double minScore);

    /**
     * Count gaps by status and tenant
     * Provides metrics for gap tracking and reporting
     *
     * @param status Care gap status
     * @param tenantId Tenant identifier
     * @return Count of gaps with the specified status
     */
    @Query("""
        SELECT COUNT(cg) FROM CareGap cg
        WHERE cg.status = :status
        AND cg.tenantId = :tenantId
        """)
    long countGapsByStatusAndTenant(
        @Param("status") String status,
        @Param("tenantId") String tenantId
    );

    /**
     * Find gaps needing intervention soon
     * Returns gaps due within specified days
     *
     * @param tenantId Tenant identifier
     * @param daysUntilDue Days until due date
     * @return List of gaps due soon
     */
    @Query("""
        SELECT cg FROM CareGap cg
        WHERE cg.tenantId = :tenantId
        AND cg.status = 'OPEN'
        AND cg.dueDate <= CURRENT_TIMESTAMP + INTERVAL :daysUntilDue day
        ORDER BY cg.dueDate ASC
        """)
    List<CareGap> findGapsDueSoon(
        @Param("tenantId") String tenantId,
        @Param("daysUntilDue") int daysUntilDue
    );

    /**
     * Find gaps with high financial impact
     *
     * @param tenantId Tenant identifier
     * @param minImpact Minimum financial impact threshold
     * @return List of high-impact gaps
     */
    @Query("""
        SELECT cg FROM CareGap cg
        WHERE cg.tenantId = :tenantId
        AND cg.financialImpact >= :minImpact
        AND cg.status = 'OPEN'
        ORDER BY cg.financialImpact DESC
        """)
    List<CareGap> findHighImpactGaps(
        @Param("tenantId") String tenantId,
        @Param("minImpact") Double minImpact
    );

    /**
     * Find recently closed gaps for a patient
     * Used for closure tracking and outcomes measurement
     *
     * @param patientId Patient identifier
     * @param daysSinceClosure Days since closure to include
     * @return List of recently closed gaps
     */
    @Query("""
        SELECT cg FROM CareGap cg
        WHERE cg.patientId = :patientId
        AND cg.status = 'CLOSED'
        AND cg.closedDate >= CURRENT_TIMESTAMP - INTERVAL :daysSinceClosure day
        ORDER BY cg.closedDate DESC
        """)
    List<CareGap> findRecentlyClosedGaps(
        @Param("patientId") String patientId,
        @Param("daysSinceClosure") int daysSinceClosure
    );

    /**
     * Calculate average risk score for a tenant
     *
     * @param tenantId Tenant identifier
     * @return Average risk score or null if no gaps
     */
    @Query("""
        SELECT AVG(cg.riskScore) FROM CareGap cg
        WHERE cg.tenantId = :tenantId
        AND cg.status = 'OPEN'
        """)
    Double getAverageRiskScore(@Param("tenantId") String tenantId);

    /**
     * Update care gap status and closure information
     * Used for gap closure workflows
     *
     * @param gapId Care gap identifier
     * @param newStatus New status
     * @param closureReason Reason for closure
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE CareGap cg
        SET cg.status = :newStatus,
            cg.closureReason = :closureReason,
            cg.closedDate = CURRENT_TIMESTAMP,
            cg.updatedAt = CURRENT_TIMESTAMP
        WHERE cg.id = :gapId
        """)
    void updateGapStatus(
        @Param("gapId") String gapId,
        @Param("newStatus") String newStatus,
        @Param("closureReason") String closureReason
    );

    /**
     * Bulk update gaps by patient and type
     *
     * @param patientId Patient identifier
     * @param gapType Gap type to update
     * @param newStatus New status
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE CareGap cg
        SET cg.status = :newStatus, cg.updatedAt = CURRENT_TIMESTAMP
        WHERE cg.patientId = :patientId
        AND cg.gapType = :gapType
        AND cg.status = 'OPEN'
        """)
    void updateGapsByPatientAndType(
        @Param("patientId") String patientId,
        @Param("gapType") String gapType,
        @Param("newStatus") String newStatus
    );

    /**
     * Count total financial impact of open gaps for a tenant
     *
     * @param tenantId Tenant identifier
     * @return Sum of financial impact values
     */
    @Query("""
        SELECT SUM(cg.financialImpact) FROM CareGap cg
        WHERE cg.tenantId = :tenantId
        AND cg.status = 'OPEN'
        """)
    Double getTotalFinancialImpact(@Param("tenantId") String tenantId);
}
