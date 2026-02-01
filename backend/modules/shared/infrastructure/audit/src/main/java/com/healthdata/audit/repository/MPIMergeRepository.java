package com.healthdata.audit.repository;

import com.healthdata.audit.entity.MPIMergeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MPI merge operations
 */
@Repository
public interface MPIMergeRepository extends JpaRepository<MPIMergeEntity, UUID> {
    
    /**
     * Find merge by ID and tenant (for security)
     */
    @Query("SELECT m FROM MPIMergeEntity m WHERE CAST(m.id AS string) = :mergeId AND m.tenantId = :tenantId")
    Optional<MPIMergeEntity> findByMergeIdAndTenantId(
        @Param("mergeId") String mergeId,
        @Param("tenantId") String tenantId
    );
    
    /**
     * Find all merges for a tenant with filtering
     */
    @Query("SELECT m FROM MPIMergeEntity m " +
           "WHERE m.tenantId = :tenantId " +
           "AND (:mergeStatus IS NULL OR m.mergeStatus = :mergeStatus) " +
           "AND (:validationStatus IS NULL OR m.validationStatus = :validationStatus) " +
           "AND (:startDate IS NULL OR m.mergeTimestamp >= :startDate) " +
           "AND (:endDate IS NULL OR m.mergeTimestamp <= :endDate) " +
           "AND (:mergeType IS NULL OR m.mergeType = :mergeType) " +
           "AND (:minConfidence IS NULL OR m.confidenceScore >= :minConfidence) " +
           "AND (:maxConfidence IS NULL OR m.confidenceScore <= :maxConfidence)")
    Page<MPIMergeEntity> findMergeHistory(
        @Param("tenantId") String tenantId,
        @Param("mergeStatus") String mergeStatus,
        @Param("validationStatus") String validationStatus,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("mergeType") String mergeType,
        @Param("minConfidence") Double minConfidence,
        @Param("maxConfidence") Double maxConfidence,
        Pageable pageable
    );
    
    /**
     * Count merges by status for metrics
     */
    @Query("SELECT COUNT(m) FROM MPIMergeEntity m WHERE m.tenantId = :tenantId AND m.mergeStatus = :status")
    Long countByTenantIdAndStatus(@Param("tenantId") String tenantId, @Param("status") String status);
    
    /**
     * Count merges by validation status
     */
    @Query("SELECT COUNT(m) FROM MPIMergeEntity m WHERE m.tenantId = :tenantId AND m.validationStatus = :status")
    Long countByTenantIdAndValidationStatus(@Param("tenantId") String tenantId, @Param("status") String status);
    
    /**
     * Get average confidence score
     */
    @Query("SELECT AVG(m.confidenceScore) FROM MPIMergeEntity m WHERE m.tenantId = :tenantId")
    Double getAverageConfidenceScore(@Param("tenantId") String tenantId);
    
    /**
     * Find merges within date range for trend analysis
     */
    @Query("SELECT m FROM MPIMergeEntity m " +
           "WHERE m.tenantId = :tenantId " +
           "AND m.mergeTimestamp >= :startDate " +
           "AND m.mergeTimestamp <= :endDate " +
           "ORDER BY m.mergeTimestamp ASC")
    List<MPIMergeEntity> findByTenantIdAndDateRange(
        @Param("tenantId") String tenantId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
