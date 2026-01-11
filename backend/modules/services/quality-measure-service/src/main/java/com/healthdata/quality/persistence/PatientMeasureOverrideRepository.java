package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Patient Measure Override Repository
 * Manages patient-specific measure parameter overrides with clinical justification.
 */
@Repository
public interface PatientMeasureOverrideRepository extends JpaRepository<PatientMeasureOverrideEntity, UUID> {

    // Tenant-isolated queries
    Optional<PatientMeasureOverrideEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<PatientMeasureOverrideEntity> findByTenantIdAndPatientId(String tenantId, UUID patientId);

    List<PatientMeasureOverrideEntity> findByTenantIdAndMeasureId(String tenantId, UUID measureId);

    // Active overrides
    @Query("SELECT o FROM PatientMeasureOverrideEntity o WHERE o.tenantId = :tenantId AND o.patientId = :patientId AND o.active = true")
    List<PatientMeasureOverrideEntity> findActiveByPatient(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    @Query("SELECT o FROM PatientMeasureOverrideEntity o WHERE o.tenantId = :tenantId AND o.patientId = :patientId " +
           "AND o.measureId = :measureId AND o.active = true")
    List<PatientMeasureOverrideEntity> findActiveByPatientAndMeasure(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("measureId") UUID measureId
    );

    // Effective date queries (for override resolution)
    @Query("SELECT o FROM PatientMeasureOverrideEntity o WHERE o.tenantId = :tenantId AND o.patientId = :patientId " +
           "AND o.measureId = :measureId AND o.active = true " +
           "AND o.effectiveFrom <= :date AND (o.effectiveUntil IS NULL OR o.effectiveUntil >= :date)")
    List<PatientMeasureOverrideEntity> findEffectiveOverrides(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("measureId") UUID measureId,
        @Param("date") LocalDate date
    );

    // Overrides needing review
    @Query("SELECT o FROM PatientMeasureOverrideEntity o WHERE o.tenantId = :tenantId AND o.requiresPeriodicReview = true " +
           "AND o.active = true AND o.nextReviewDate <= :date ORDER BY o.nextReviewDate")
    List<PatientMeasureOverrideEntity> findOverridesDueForReview(
        @Param("tenantId") String tenantId,
        @Param("date") LocalDate date
    );

    // By override type
    @Query("SELECT o FROM PatientMeasureOverrideEntity o WHERE o.tenantId = :tenantId AND o.overrideType = :type AND o.active = true")
    List<PatientMeasureOverrideEntity> findByOverrideType(
        @Param("tenantId") String tenantId,
        @Param("type") String type
    );

    // Pending approval
    @Query("SELECT o FROM PatientMeasureOverrideEntity o WHERE o.tenantId = :tenantId AND o.approvedBy IS NULL AND o.active = true")
    List<PatientMeasureOverrideEntity> findPendingApproval(
        @Param("tenantId") String tenantId
    );

    // Count overrides
    @Query("SELECT COUNT(o) FROM PatientMeasureOverrideEntity o WHERE o.tenantId = :tenantId AND o.patientId = :patientId AND o.active = true")
    long countActiveByPatient(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );
}
