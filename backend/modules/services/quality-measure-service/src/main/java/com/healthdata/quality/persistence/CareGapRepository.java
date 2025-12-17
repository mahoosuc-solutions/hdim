package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Care Gap operations
 */
@Repository
public interface CareGapRepository extends JpaRepository<CareGapEntity, UUID> {

    /**
     * Find all care gaps for a patient, ordered by priority and due date
     */
    List<CareGapEntity> findByTenantIdAndPatientIdOrderByPriorityAscDueDateAsc(
        String tenantId,
        String patientId
    );

    /**
     * Find open care gaps for a patient
     */
    @Query("SELECT c FROM CareGapEntity c " +
           "WHERE c.tenantId = :tenantId AND c.patientId = :patientId " +
           "AND c.status IN ('OPEN', 'IN_PROGRESS') " +
           "ORDER BY c.priority ASC, c.dueDate ASC")
    List<CareGapEntity> findOpenCareGaps(
        @Param("tenantId") String tenantId,
        @Param("patientId") String patientId
    );

    /**
     * Find care gaps by status
     */
    List<CareGapEntity> findByTenantIdAndPatientIdAndStatusOrderByDueDateAsc(
        String tenantId,
        String patientId,
        CareGapEntity.Status status
    );

    /**
     * Find care gaps by category
     */
    List<CareGapEntity> findByTenantIdAndPatientIdAndCategoryOrderByPriorityAscDueDateAsc(
        String tenantId,
        String patientId,
        CareGapEntity.GapCategory category
    );

    /**
     * Find urgent care gaps across all patients (for care coordination)
     */
    @Query("SELECT c FROM CareGapEntity c " +
           "WHERE c.tenantId = :tenantId " +
           "AND c.status = 'OPEN' " +
           "AND c.priority = 'URGENT' " +
           "ORDER BY c.dueDate ASC")
    List<CareGapEntity> findAllUrgentCareGaps(@Param("tenantId") String tenantId);

    /**
     * Find overdue care gaps
     */
    @Query("SELECT c FROM CareGapEntity c " +
           "WHERE c.tenantId = :tenantId AND c.patientId = :patientId " +
           "AND c.status IN ('OPEN', 'IN_PROGRESS') " +
           "AND c.dueDate < :now " +
           "ORDER BY c.priority ASC, c.dueDate ASC")
    List<CareGapEntity> findOverdueCareGaps(
        @Param("tenantId") String tenantId,
        @Param("patientId") String patientId,
        @Param("now") Instant now
    );

    /**
     * Count open care gaps by priority
     */
    @Query("SELECT COUNT(c) FROM CareGapEntity c " +
           "WHERE c.tenantId = :tenantId AND c.patientId = :patientId " +
           "AND c.status IN ('OPEN', 'IN_PROGRESS') " +
           "AND c.priority = :priority")
    Long countOpenCareGapsByPriority(
        @Param("tenantId") String tenantId,
        @Param("patientId") String patientId,
        @Param("priority") CareGapEntity.Priority priority
    );

    /**
     * Check if a specific care gap already exists
     */
    @Query("SELECT COUNT(c) > 0 FROM CareGapEntity c " +
           "WHERE c.tenantId = :tenantId AND c.patientId = :patientId " +
           "AND c.gapType = :gapType " +
           "AND c.status IN ('OPEN', 'IN_PROGRESS')")
    boolean existsOpenCareGap(
        @Param("tenantId") String tenantId,
        @Param("patientId") String patientId,
        @Param("gapType") String gapType
    );

    /**
     * Count care gaps by patient and status
     */
    Long countByTenantIdAndPatientIdAndStatus(
        String tenantId,
        String patientId,
        CareGapEntity.Status status
    );

    /**
     * Count all open care gaps for a patient
     */
    @Query("SELECT COUNT(c) FROM CareGapEntity c " +
           "WHERE c.tenantId = :tenantId AND c.patientId = :patientId " +
           "AND c.status IN ('OPEN', 'IN_PROGRESS')")
    Long countOpenCareGaps(
        @Param("tenantId") String tenantId,
        @Param("patientId") String patientId
    );

    /**
     * Count urgent care gaps for a patient
     */
    @Query("SELECT COUNT(c) FROM CareGapEntity c " +
           "WHERE c.tenantId = :tenantId AND c.patientId = :patientId " +
           "AND c.status IN ('OPEN', 'IN_PROGRESS') " +
           "AND c.priority = 'URGENT'")
    Long countUrgentCareGaps(
        @Param("tenantId") String tenantId,
        @Param("patientId") String patientId
    );

    /**
     * Get all distinct tenant IDs
     */
    @Query("SELECT DISTINCT c.tenantId FROM CareGapEntity c")
    List<String> findDistinctTenantIds();

    /**
     * Get all distinct patient IDs for a tenant
     */
    @Query("SELECT DISTINCT c.patientId FROM CareGapEntity c " +
           "WHERE c.tenantId = :tenantId")
    List<String> findDistinctPatientIdsByTenantId(@Param("tenantId") String tenantId);
}
