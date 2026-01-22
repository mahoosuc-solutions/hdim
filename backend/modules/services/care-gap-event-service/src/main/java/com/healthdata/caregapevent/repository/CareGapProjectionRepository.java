package com.healthdata.caregapevent.repository;

import com.healthdata.caregapevent.projection.CareGapProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Care Gap Projection (CQRS Read Model)
 *
 * Optimized for fast queries on denormalized care gap data.
 * All queries include tenant isolation for multi-tenancy.
 */
@Repository
public interface CareGapProjectionRepository extends JpaRepository<CareGapProjection, Long> {

    /**
     * Find care gap by tenant and care gap ID
     */
    Optional<CareGapProjection> findByTenantIdAndCareGapId(String tenantId, UUID careGapId);

    /**
     * Find all care gaps for a patient
     */
    List<CareGapProjection> findByTenantIdAndPatientIdOrderByPriorityDesc(String tenantId, UUID patientId);

    /**
     * Find open care gaps for a patient
     */
    @Query("SELECT c FROM CareGapProjection c WHERE c.tenantId = :tenantId AND c.patientId = :patientId AND c.status = 'OPEN' ORDER BY c.priority DESC, c.dueDate ASC")
    List<CareGapProjection> findOpenCareGapsForPatient(@Param("tenantId") String tenantId, @Param("patientId") UUID patientId);

    /**
     * Find open care gaps for a tenant (paginated)
     */
    @Query("SELECT c FROM CareGapProjection c WHERE c.tenantId = :tenantId AND c.status = 'OPEN' ORDER BY c.priority DESC, c.dueDate ASC")
    Page<CareGapProjection> findOpenCareGapsForTenant(@Param("tenantId") String tenantId, Pageable pageable);

    /**
     * Find urgent care gaps (URGENT priority)
     */
    @Query("SELECT c FROM CareGapProjection c WHERE c.tenantId = :tenantId AND c.status = 'OPEN' AND c.priority = 'URGENT' ORDER BY c.dueDate ASC")
    List<CareGapProjection> findUrgentCareGaps(@Param("tenantId") String tenantId);

    /**
     * Find care gaps by priority for a tenant
     */
    @Query("SELECT c FROM CareGapProjection c WHERE c.tenantId = :tenantId AND c.status = 'OPEN' AND c.priority = :priority ORDER BY c.dueDate ASC")
    List<CareGapProjection> findCareGapsByPriority(@Param("tenantId") String tenantId, @Param("priority") String priority);

    /**
     * Find overdue care gaps (due_date < today and status = OPEN)
     */
    @Query("SELECT c FROM CareGapProjection c WHERE c.tenantId = :tenantId AND c.status = 'OPEN' AND c.dueDate < CURRENT_DATE ORDER BY c.daysOverdue DESC")
    List<CareGapProjection> findOverdueCareGaps(@Param("tenantId") String tenantId);

    /**
     * Find care gaps due within N days
     * Note: Using native query because JPQL doesn't support date arithmetic directly
     */
    @Query(value = "SELECT * FROM care_gap_projections WHERE tenant_id = :tenantId AND status = 'OPEN' AND due_date BETWEEN CURRENT_DATE AND (CURRENT_DATE + CAST(:days AS INTEGER))", nativeQuery = true)
    List<CareGapProjection> findCareGapsDueWithinDays(@Param("tenantId") String tenantId, @Param("days") Integer days);

    /**
     * Find care gaps for a specific measure
     */
    @Query("SELECT c FROM CareGapProjection c WHERE c.tenantId = :tenantId AND c.measureId = :measureId AND c.status = 'OPEN' ORDER BY c.priority DESC")
    List<CareGapProjection> findCareGapsByMeasure(@Param("tenantId") String tenantId, @Param("measureId") String measureId);

    /**
     * Find care gaps assigned to a specific user
     */
    @Query("SELECT c FROM CareGapProjection c WHERE c.tenantId = :tenantId AND c.assignedTo = :assignedTo AND c.status = 'OPEN' ORDER BY c.priority DESC")
    List<CareGapProjection> findCareGapsAssignedTo(@Param("tenantId") String tenantId, @Param("assignedTo") String assignedTo);

    /**
     * Count open care gaps for a patient
     */
    @Query("SELECT COUNT(c) FROM CareGapProjection c WHERE c.tenantId = :tenantId AND c.patientId = :patientId AND c.status = 'OPEN'")
    long countOpenCareGapsForPatient(@Param("tenantId") String tenantId, @Param("patientId") UUID patientId);

    /**
     * Count urgent care gaps for a patient
     */
    @Query("SELECT COUNT(c) FROM CareGapProjection c WHERE c.tenantId = :tenantId AND c.patientId = :patientId AND c.status = 'OPEN' AND c.priority = 'URGENT'")
    long countUrgentCareGapsForPatient(@Param("tenantId") String tenantId, @Param("patientId") UUID patientId);

    /**
     * Count open care gaps for a tenant
     */
    @Query("SELECT COUNT(c) FROM CareGapProjection c WHERE c.tenantId = :tenantId AND c.status = 'OPEN'")
    long countOpenCareGapsForTenant(@Param("tenantId") String tenantId);

    /**
     * Count urgent care gaps for a tenant
     */
    @Query("SELECT COUNT(c) FROM CareGapProjection c WHERE c.tenantId = :tenantId AND c.status = 'OPEN' AND c.priority = 'URGENT'")
    long countUrgentCareGapsForTenant(@Param("tenantId") String tenantId);

    /**
     * Count overdue care gaps for a tenant
     */
    @Query("SELECT COUNT(c) FROM CareGapProjection c WHERE c.tenantId = :tenantId AND c.status = 'OPEN' AND c.dueDate < CURRENT_DATE")
    long countOverdueCareGapsForTenant(@Param("tenantId") String tenantId);

    /**
     * Get distinct tenant IDs (for rebuild operations)
     */
    @Query("SELECT DISTINCT c.tenantId FROM CareGapProjection c")
    List<String> findDistinctTenantIds();

    /**
     * Delete all projections for a tenant (when tenant is deprovisioned)
     */
    void deleteAllByTenantId(String tenantId);

    /**
     * Delete all projections for a patient (when patient is deleted)
     */
    void deleteAllByTenantIdAndPatientId(String tenantId, UUID patientId);
}
