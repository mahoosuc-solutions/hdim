package com.healthdata.quality.persistence;

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
 * Mental Health Assessment Repository
 */
@Repository
public interface MentalHealthAssessmentRepository extends JpaRepository<MentalHealthAssessmentEntity, UUID> {

    /**
     * Find all assessments for a patient, ordered by date descending
     */
    List<MentalHealthAssessmentEntity> findByTenantIdAndPatientIdOrderByAssessmentDateDesc(
        String tenantId,
        String patientId
    );

    /**
     * Find assessments for a patient by type
     */
    List<MentalHealthAssessmentEntity> findByTenantIdAndPatientIdAndTypeOrderByAssessmentDateDesc(
        String tenantId,
        String patientId,
        MentalHealthAssessmentEntity.AssessmentType type
    );

    /**
     * Find assessments with pagination
     */
    List<MentalHealthAssessmentEntity> findByTenantIdAndPatientIdOrderByAssessmentDateDesc(
        String tenantId,
        String patientId,
        Pageable pageable
    );

    /**
     * Find most recent assessment of a specific type
     */
    Optional<MentalHealthAssessmentEntity> findFirstByTenantIdAndPatientIdAndTypeOrderByAssessmentDateDesc(
        String tenantId,
        String patientId,
        MentalHealthAssessmentEntity.AssessmentType type
    );

    /**
     * Find assessments requiring follow-up
     */
    @Query("SELECT a FROM MentalHealthAssessmentEntity a " +
           "WHERE a.tenantId = :tenantId " +
           "AND a.patientId = :patientId " +
           "AND a.positiveScreen = true " +
           "AND a.requiresFollowup = true " +
           "ORDER BY a.assessmentDate DESC")
    List<MentalHealthAssessmentEntity> findPositiveScreensRequiringFollowup(
        @Param("tenantId") String tenantId,
        @Param("patientId") String patientId
    );

    /**
     * Find assessments within date range
     */
    @Query("SELECT a FROM MentalHealthAssessmentEntity a " +
           "WHERE a.tenantId = :tenantId " +
           "AND a.patientId = :patientId " +
           "AND a.type = :type " +
           "AND a.assessmentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY a.assessmentDate ASC")
    List<MentalHealthAssessmentEntity> findByDateRange(
        @Param("tenantId") String tenantId,
        @Param("patientId") String patientId,
        @Param("type") MentalHealthAssessmentEntity.AssessmentType type,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Count positive screens for a patient
     */
    @Query("SELECT COUNT(a) FROM MentalHealthAssessmentEntity a " +
           "WHERE a.tenantId = :tenantId " +
           "AND a.patientId = :patientId " +
           "AND a.positiveScreen = true")
    long countPositiveScreens(
        @Param("tenantId") String tenantId,
        @Param("patientId") String patientId
    );

    /**
     * Count total assessments for a patient
     */
    long countByTenantIdAndPatientId(String tenantId, String patientId);
}
