package com.healthdata.audit.repository;

import com.healthdata.audit.entity.DataQualityIssueEntity;
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
 * Repository for data quality issues
 */
@Repository
public interface DataQualityIssueRepository extends JpaRepository<DataQualityIssueEntity, UUID> {
    
    /**
     * Find issue by ID and tenant (for security)
     */
    @Query("SELECT d FROM DataQualityIssueEntity d WHERE CAST(d.id AS string) = :issueId AND d.tenantId = :tenantId")
    Optional<DataQualityIssueEntity> findByIssueIdAndTenantId(
        @Param("issueId") String issueId,
        @Param("tenantId") String tenantId
    );
    
    /**
     * Find all issues for a tenant with filtering
     */
    @Query("SELECT d FROM DataQualityIssueEntity d " +
           "WHERE d.tenantId = :tenantId " +
           "AND (:status IS NULL OR d.status = :status) " +
           "AND (:severity IS NULL OR d.severity = :severity) " +
           "ORDER BY d.detectedAt DESC")
    Page<DataQualityIssueEntity> findIssuesByTenant(
        @Param("tenantId") String tenantId,
        @Param("status") String status,
        @Param("severity") String severity,
        Pageable pageable
    );
    
    /**
     * Find issues for a specific patient
     */
    List<DataQualityIssueEntity> findByTenantIdAndPatientIdOrderByDetectedAtDesc(
        String tenantId,
        String patientId
    );
    
    /**
     * Count issues by status
     */
    @Query("SELECT COUNT(d) FROM DataQualityIssueEntity d WHERE d.tenantId = :tenantId AND d.status = :status")
    Long countByTenantIdAndStatus(@Param("tenantId") String tenantId, @Param("status") String status);
    
    /**
     * Count issues by severity
     */
    @Query("SELECT COUNT(d) FROM DataQualityIssueEntity d WHERE d.tenantId = :tenantId AND d.severity = :severity")
    Long countByTenantIdAndSeverity(@Param("tenantId") String tenantId, @Param("severity") String severity);
}
