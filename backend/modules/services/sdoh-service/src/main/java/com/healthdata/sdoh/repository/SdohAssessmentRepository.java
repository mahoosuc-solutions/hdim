package com.healthdata.sdoh.repository;

import com.healthdata.sdoh.entity.SdohAssessmentEntity;
import com.healthdata.sdoh.model.SdohAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SdohAssessmentRepository extends JpaRepository<SdohAssessmentEntity, String> {

    List<SdohAssessmentEntity> findByTenantIdAndPatientId(String tenantId, String patientId);

    @Query("SELECT a FROM SdohAssessmentEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId ORDER BY a.assessmentDate DESC LIMIT 1")
    Optional<SdohAssessmentEntity> findMostRecentByTenantIdAndPatientId(
            @Param("tenantId") String tenantId,
            @Param("patientId") String patientId);

    @Query("SELECT a FROM SdohAssessmentEntity a WHERE a.assessmentDate < :cutoffDate")
    List<SdohAssessmentEntity> findOldAssessments(@Param("cutoffDate") LocalDateTime cutoffDate);

    Long countByTenantId(String tenantId);

    Long countByTenantIdAndStatus(String tenantId, SdohAssessment.AssessmentStatus status);

    @Query("SELECT a FROM SdohAssessmentEntity a WHERE a.tenantId = :tenantId AND a.assessmentDate BETWEEN :startDate AND :endDate")
    List<SdohAssessmentEntity> findByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
