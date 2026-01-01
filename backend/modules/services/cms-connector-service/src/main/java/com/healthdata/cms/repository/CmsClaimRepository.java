package com.healthdata.cms.repository;

import com.healthdata.cms.model.CmsClaim;
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
 * CMS Claim Repository
 * 
 * Data access layer for CMS Claim entities.
 */
@Repository
public interface CmsClaimRepository extends JpaRepository<CmsClaim, UUID> {

    /**
     * Find claim by CMS claim ID
     */
    Optional<CmsClaim> findByClaimId(String claimId);

    /**
     * Find all claims for a beneficiary
     */
    List<CmsClaim> findByBeneficiaryId(String beneficiaryId);

    /**
     * Find all claims for a beneficiary, paginated
     */
    Page<CmsClaim> findByBeneficiaryId(String beneficiaryId, Pageable pageable);

    /**
     * Find all claims for a tenant
     */
    List<CmsClaim> findByTenantId(UUID tenantId);

    /**
     * Find claims from a specific data source
     */
    List<CmsClaim> findByTenantIdAndDataSource(UUID tenantId, CmsClaim.ClaimSource dataSource);

    /**
     * Find claims imported within a date range
     */
    @Query("SELECT c FROM CmsClaim c WHERE c.tenantId = :tenantId AND c.importedAt BETWEEN :startDate AND :endDate")
    List<CmsClaim> findClaimsImportedBetween(
        @Param("tenantId") UUID tenantId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find unprocessed claims
     */
    List<CmsClaim> findByTenantIdAndIsProcessedFalse(UUID tenantId);

    /**
     * Find claims with validation errors
     */
    List<CmsClaim> findByTenantIdAndHasValidationErrorsTrue(UUID tenantId);

    /**
     * Count claims by source for a tenant
     */
    @Query("SELECT COUNT(c) FROM CmsClaim c WHERE c.tenantId = :tenantId AND c.dataSource = :dataSource")
    long countByTenantAndSource(@Param("tenantId") UUID tenantId, @Param("dataSource") CmsClaim.ClaimSource dataSource);

    /**
     * Find duplicate claims using content hash
     */
    List<CmsClaim> findByTenantIdAndContentHash(UUID tenantId, String contentHash);

    /**
     * Delete claims older than specified date
     */
    void deleteByTenantIdAndImportedAtBefore(UUID tenantId, LocalDateTime cutoffDate);
}
