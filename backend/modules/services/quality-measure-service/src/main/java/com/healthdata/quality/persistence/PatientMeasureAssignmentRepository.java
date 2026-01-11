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
 * Patient Measure Assignment Repository
 * Manages patient-specific measure assignments (manual and automatic).
 */
@Repository
public interface PatientMeasureAssignmentRepository extends JpaRepository<PatientMeasureAssignmentEntity, UUID> {

    // Tenant-isolated queries
    Optional<PatientMeasureAssignmentEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<PatientMeasureAssignmentEntity> findByTenantIdAndPatientId(String tenantId, UUID patientId);

    List<PatientMeasureAssignmentEntity> findByTenantIdAndMeasureId(String tenantId, UUID measureId);

    // Active assignments
    @Query("SELECT a FROM PatientMeasureAssignmentEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId AND a.active = true")
    List<PatientMeasureAssignmentEntity> findActiveByPatient(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    @Query("SELECT a FROM PatientMeasureAssignmentEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId AND a.measureId = :measureId AND a.active = true")
    Optional<PatientMeasureAssignmentEntity> findActiveByPatientAndMeasure(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("measureId") UUID measureId
    );

    // Effective date queries
    @Query("SELECT a FROM PatientMeasureAssignmentEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId " +
           "AND a.active = true AND a.effectiveFrom <= :date AND (a.effectiveUntil IS NULL OR a.effectiveUntil >= :date)")
    List<PatientMeasureAssignmentEntity> findEffectiveAssignments(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("date") LocalDate date
    );

    // Auto-assigned vs manual
    @Query("SELECT a FROM PatientMeasureAssignmentEntity a WHERE a.tenantId = :tenantId AND a.autoAssigned = :autoAssigned AND a.active = true")
    List<PatientMeasureAssignmentEntity> findByAutoAssigned(
        @Param("tenantId") String tenantId,
        @Param("autoAssigned") Boolean autoAssigned
    );

    // Count assignments
    @Query("SELECT COUNT(a) FROM PatientMeasureAssignmentEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId AND a.active = true")
    long countActiveByPatient(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );
}
