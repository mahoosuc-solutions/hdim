package com.healthdata.qrda.persistence;

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
 * Repository for CMS submission history persistence.
 */
@Repository
public interface CmsSubmissionRepository extends JpaRepository<CmsSubmissionEntity, UUID> {

    Optional<CmsSubmissionEntity> findByIdAndTenantId(UUID id, String tenantId);

    Page<CmsSubmissionEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    List<CmsSubmissionEntity> findByTenantIdAndSubmissionYear(String tenantId, Integer submissionYear);

    List<CmsSubmissionEntity> findByTenantIdAndProgramType(
        String tenantId,
        CmsSubmissionEntity.ProgramType programType
    );

    List<CmsSubmissionEntity> findByTenantIdAndStatus(
        String tenantId,
        CmsSubmissionEntity.SubmissionStatus status
    );

    @Query("SELECT s FROM CmsSubmissionEntity s WHERE s.tenantId = :tenantId " +
           "AND s.submissionYear = :year AND s.programType = :programType " +
           "ORDER BY s.createdAt DESC")
    List<CmsSubmissionEntity> findByYearAndProgram(
        @Param("tenantId") String tenantId,
        @Param("year") Integer year,
        @Param("programType") CmsSubmissionEntity.ProgramType programType
    );

    @Query("SELECT s FROM CmsSubmissionEntity s WHERE s.tenantId = :tenantId " +
           "AND s.status IN ('SUBMITTED', 'ACKNOWLEDGED') " +
           "ORDER BY s.submittedAt DESC")
    List<CmsSubmissionEntity> findPendingAcknowledgment(@Param("tenantId") String tenantId);

    Optional<CmsSubmissionEntity> findByCmsTrackingNumber(String cmsTrackingNumber);
}
