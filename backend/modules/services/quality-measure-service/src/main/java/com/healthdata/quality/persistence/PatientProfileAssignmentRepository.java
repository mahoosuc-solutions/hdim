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
 * Patient Profile Assignment Repository
 * Manages patient-to-profile assignments for configuration templates.
 */
@Repository
public interface PatientProfileAssignmentRepository extends JpaRepository<PatientProfileAssignmentEntity, UUID> {

    // Tenant-isolated queries
    Optional<PatientProfileAssignmentEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<PatientProfileAssignmentEntity> findByTenantIdAndPatientId(String tenantId, UUID patientId);

    List<PatientProfileAssignmentEntity> findByTenantIdAndProfileId(String tenantId, UUID profileId);

    // Active assignments
    @Query("SELECT a FROM PatientProfileAssignmentEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId AND a.active = true")
    List<PatientProfileAssignmentEntity> findActiveByPatient(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    @Query("SELECT a FROM PatientProfileAssignmentEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId " +
           "AND a.profileId = :profileId AND a.active = true")
    Optional<PatientProfileAssignmentEntity> findActiveByPatientAndProfile(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("profileId") UUID profileId
    );

    // Effective assignments (by date)
    @Query("SELECT a FROM PatientProfileAssignmentEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId " +
           "AND a.active = true AND a.effectiveFrom <= :date AND (a.effectiveUntil IS NULL OR a.effectiveUntil >= :date)")
    List<PatientProfileAssignmentEntity> findEffectiveAssignments(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("date") LocalDate date
    );

    // Auto-assigned vs manual
    @Query("SELECT a FROM PatientProfileAssignmentEntity a WHERE a.tenantId = :tenantId AND a.autoAssigned = :autoAssigned AND a.active = true")
    List<PatientProfileAssignmentEntity> findByAutoAssigned(
        @Param("tenantId") String tenantId,
        @Param("autoAssigned") Boolean autoAssigned
    );

    // Count by profile
    @Query("SELECT COUNT(a) FROM PatientProfileAssignmentEntity a WHERE a.tenantId = :tenantId AND a.profileId = :profileId AND a.active = true")
    long countActiveByProfile(
        @Param("tenantId") String tenantId,
        @Param("profileId") UUID profileId
    );

    // Count by patient
    @Query("SELECT COUNT(a) FROM PatientProfileAssignmentEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId AND a.active = true")
    long countActiveByPatient(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );
}
