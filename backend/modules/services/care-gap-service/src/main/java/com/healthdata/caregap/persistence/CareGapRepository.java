package com.healthdata.caregap.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Care Gap Repository
 *
 * Data access layer for care gap persistence with specialized queries
 * for quality measure tracking and gap closure management.
 */
@Repository
public interface CareGapRepository extends JpaRepository<CareGapEntity, UUID> {

    // ==================== Basic Queries ====================

    /**
     * Find care gap by ID and tenant
     */
    Optional<CareGapEntity> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Find all care gaps for a patient
     */
    @Query("SELECT c FROM CareGapEntity c WHERE c.tenantId = :tenantId AND c.patientId = :patientId ORDER BY c.identifiedDate DESC")
    List<CareGapEntity> findByTenantIdAndPatientId(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    // ==================== Status Queries ====================

    /**
     * Find open care gaps for a patient
     */
    @Query("SELECT c FROM CareGapEntity c WHERE c.tenantId = :tenantId AND c.patientId = :patientId AND c.gapStatus = 'open' ORDER BY c.priority DESC, c.dueDate ASC")
    List<CareGapEntity> findOpenGapsByPatient(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    /**
     * Find high priority open gaps for a patient
     */
    @Query("SELECT c FROM CareGapEntity c WHERE c.tenantId = :tenantId AND c.patientId = :patientId AND c.gapStatus = 'open' AND c.priority = 'high' ORDER BY c.dueDate ASC")
    List<CareGapEntity> findHighPriorityOpenGaps(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    /**
     * Find closed care gaps for a patient
     */
    @Query("SELECT c FROM CareGapEntity c WHERE c.tenantId = :tenantId AND c.patientId = :patientId AND c.gapStatus = 'closed' ORDER BY c.closedDate DESC")
    List<CareGapEntity> findClosedGapsByPatient(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    // ==================== Measure Queries ====================

    /**
     * Find care gaps by measure ID
     */
    @Query("SELECT c FROM CareGapEntity c WHERE c.tenantId = :tenantId AND c.patientId = :patientId AND c.measureId = :measureId ORDER BY c.identifiedDate DESC")
    List<CareGapEntity> findByMeasure(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("measureId") String measureId
    );

    /**
     * Find care gaps by measure category (HEDIS, CMS, etc.)
     */
    @Query("SELECT c FROM CareGapEntity c WHERE c.tenantId = :tenantId AND c.patientId = :patientId AND c.measureCategory = :category AND c.gapStatus = 'open' ORDER BY c.priority DESC")
    List<CareGapEntity> findByMeasureCategory(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("category") String category
    );

    /**
     * Find care gaps by measure year
     */
    @Query("SELECT c FROM CareGapEntity c WHERE c.tenantId = :tenantId AND c.measureYear = :year ORDER BY c.patientId, c.measureId")
    List<CareGapEntity> findByMeasureYear(
        @Param("tenantId") String tenantId,
        @Param("year") Integer year
    );

    // ==================== Date Queries ====================

    /**
     * Find overdue care gaps
     */
    @Query("SELECT c FROM CareGapEntity c WHERE c.tenantId = :tenantId AND c.gapStatus = 'open' AND c.dueDate < :currentDate ORDER BY c.dueDate ASC")
    List<CareGapEntity> findOverdueGaps(
        @Param("tenantId") String tenantId,
        @Param("currentDate") LocalDate currentDate
    );

    /**
     * Find care gaps due within date range
     */
    @Query("SELECT c FROM CareGapEntity c WHERE c.tenantId = :tenantId AND c.patientId = :patientId AND c.gapStatus = 'open' AND c.dueDate BETWEEN :startDate AND :endDate ORDER BY c.dueDate ASC")
    List<CareGapEntity> findGapsDueInRange(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find gaps identified within date range
     */
    @Query("SELECT c FROM CareGapEntity c WHERE c.tenantId = :tenantId AND c.identifiedDate BETWEEN :startDate AND :endDate ORDER BY c.identifiedDate DESC")
    List<CareGapEntity> findGapsIdentifiedInRange(
        @Param("tenantId") String tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // ==================== Count Queries ====================

    /**
     * Count open care gaps for a patient
     */
    @Query("SELECT COUNT(c) FROM CareGapEntity c WHERE c.tenantId = :tenantId AND c.patientId = :patientId AND c.gapStatus = 'open'")
    long countOpenGaps(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    /**
     * Count high priority gaps for a patient
     */
    @Query("SELECT COUNT(c) FROM CareGapEntity c WHERE c.tenantId = :tenantId AND c.patientId = :patientId AND c.gapStatus = 'open' AND c.priority = 'high'")
    long countHighPriorityGaps(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    /**
     * Count overdue gaps for a patient
     */
    @Query("SELECT COUNT(c) FROM CareGapEntity c WHERE c.tenantId = :tenantId AND c.patientId = :patientId AND c.gapStatus = 'open' AND c.dueDate < :currentDate")
    long countOverdueGaps(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("currentDate") LocalDate currentDate
    );

    /**
     * Count care gaps by measure
     */
    @Query("SELECT COUNT(c) FROM CareGapEntity c WHERE c.tenantId = :tenantId AND c.patientId = :patientId AND c.measureId = :measureId")
    long countByMeasure(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("measureId") String measureId
    );

    // ==================== Analytics Queries ====================

    /**
     * Find all open gaps for a tenant (for population-level reporting)
     */
    @Query("SELECT c FROM CareGapEntity c WHERE c.tenantId = :tenantId AND c.gapStatus = 'open' ORDER BY c.priority DESC, c.dueDate ASC")
    List<CareGapEntity> findAllOpenGaps(@Param("tenantId") String tenantId);

    /**
     * Count gaps by status for a tenant
     */
    @Query("SELECT c.gapStatus, COUNT(c) FROM CareGapEntity c WHERE c.tenantId = :tenantId GROUP BY c.gapStatus")
    List<Object[]> countByStatus(@Param("tenantId") String tenantId);

    /**
     * Count gaps by priority for a tenant
     */
    @Query("SELECT c.priority, COUNT(c) FROM CareGapEntity c WHERE c.tenantId = :tenantId AND c.gapStatus = 'open' GROUP BY c.priority")
    List<Object[]> countByPriority(@Param("tenantId") String tenantId);

    /**
     * Count gaps by measure category for a tenant
     */
    @Query("SELECT c.measureCategory, COUNT(c) FROM CareGapEntity c WHERE c.tenantId = :tenantId AND c.gapStatus = 'open' GROUP BY c.measureCategory")
    List<Object[]> countByMeasureCategory(@Param("tenantId") String tenantId);

    // ==================== Existence Checks ====================

    /**
     * Check if patient has open gap for specific measure
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CareGapEntity c WHERE c.tenantId = :tenantId AND c.patientId = :patientId AND c.measureId = :measureId AND c.gapStatus = 'open'")
    boolean hasOpenGapForMeasure(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("measureId") String measureId
    );

    /**
     * Check if patient has any high priority gaps
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CareGapEntity c WHERE c.tenantId = :tenantId AND c.patientId = :patientId AND c.gapStatus = 'open' AND c.priority = 'high'")
    boolean hasHighPriorityGaps(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );
}
