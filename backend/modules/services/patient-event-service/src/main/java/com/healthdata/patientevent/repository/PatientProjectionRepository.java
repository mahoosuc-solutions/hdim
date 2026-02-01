package com.healthdata.patientevent.repository;

import com.healthdata.patientevent.projection.PatientProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Patient Projection (CQRS Read Model)
 *
 * Optimized for fast queries on denormalized patient data.
 * All queries include tenant isolation for multi-tenancy.
 */
@Repository
public interface PatientProjectionRepository extends JpaRepository<PatientProjection, Long> {

    /**
     * Find patient projection by tenant and patient ID
     * (FIX for duplicate bean error: unique package name)
     */
    Optional<PatientProjection> findByTenantIdAndPatientId(String tenantId, UUID patientId);

    /**
     * Find all patients for a tenant
     */
    Page<PatientProjection> findByTenantIdOrderByLastNameAsc(String tenantId, Pageable pageable);

    /**
     * Find patients by last name (for search)
     */
    Page<PatientProjection> findByTenantIdAndLastNameContainingIgnoreCaseOrderByLastName(
        String tenantId, String lastName, Pageable pageable);

    /**
     * Find high-risk patients for a tenant
     */
    @Query("SELECT p FROM PatientProjection p WHERE p.tenantId = :tenantId AND p.riskLevel = 'HIGH' ORDER BY p.lastUpdatedAt DESC")
    List<PatientProjection> findHighRiskPatients(@Param("tenantId") String tenantId);

    /**
     * Find patients with urgent care gaps
     */
    @Query("SELECT p FROM PatientProjection p WHERE p.tenantId = :tenantId AND p.urgentCareGapsCount > 0 ORDER BY p.urgentCareGapsCount DESC")
    List<PatientProjection> findPatientsWithUrgentGaps(@Param("tenantId") String tenantId);

    /**
     * Find patients with critical alerts
     */
    @Query("SELECT p FROM PatientProjection p WHERE p.tenantId = :tenantId AND p.hasCriticalAlert = true ORDER BY p.lastUpdatedAt DESC")
    List<PatientProjection> findPatientsWithCriticalAlerts(@Param("tenantId") String tenantId);

    /**
     * Find patients with mental health flags
     */
    @Query("SELECT p FROM PatientProjection p WHERE p.tenantId = :tenantId AND p.mentalHealthFlag = true ORDER BY p.lastUpdatedAt DESC")
    List<PatientProjection> findPatientsWithMentalHealthFlags(@Param("tenantId") String tenantId);

    /**
     * Count patients for a tenant
     */
    long countByTenantId(String tenantId);

    /**
     * Count high-risk patients
     */
    @Query("SELECT COUNT(p) FROM PatientProjection p WHERE p.tenantId = :tenantId AND p.riskLevel = 'HIGH'")
    long countHighRiskPatients(@Param("tenantId") String tenantId);

    /**
     * Count patients with urgent care gaps
     */
    @Query("SELECT COUNT(p) FROM PatientProjection p WHERE p.tenantId = :tenantId AND p.urgentCareGapsCount > 0")
    long countPatientsWithUrgentGaps(@Param("tenantId") String tenantId);

    /**
     * Count patients with active alerts
     */
    @Query("SELECT COUNT(p) FROM PatientProjection p WHERE p.tenantId = :tenantId AND p.activeAlertsCount > 0")
    long countPatientsWithActiveAlerts(@Param("tenantId") String tenantId);

    /**
     * Get distinct tenant IDs (for rebuild operations)
     */
    @Query("SELECT DISTINCT p.tenantId FROM PatientProjection p")
    List<String> findDistinctTenantIds();

    /**
     * Delete all projections for a tenant (when tenant is deprovisioned)
     */
    void deleteAllByTenantId(String tenantId);
}
