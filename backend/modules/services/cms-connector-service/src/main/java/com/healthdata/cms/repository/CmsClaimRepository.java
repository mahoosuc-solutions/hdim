package com.healthdata.cms.repository;

import com.healthdata.cms.model.CmsClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CmsClaimRepository extends JpaRepository<CmsClaim, UUID> {

    /**
     * Find all claims for a patient within a tenant
     */
    List<CmsClaim> findByBeneficiaryIdAndTenantId(String beneficiaryId, UUID tenantId);

    /**
     * Find claims by tenant
     */
    List<CmsClaim> findByTenantId(UUID tenantId);

    /**
     * Find claim by ID and tenant (multi-tenant safety)
     */
    Optional<CmsClaim> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Find claim by content hash (for deduplication)
     */
    Optional<CmsClaim> findByContentHash(String contentHash);

    /**
     * Check if claim exists by content hash
     */
    boolean existsByContentHash(String contentHash);

    /**
     * Count claims by beneficiary and tenant
     */
    long countByBeneficiaryIdAndTenantId(String beneficiaryId, UUID tenantId);

    /**
     * Find unprocessed claims for a tenant
     */
    @Query("SELECT c FROM CmsClaim c WHERE c.tenantId = :tenantId AND c.isProcessed = false")
    List<CmsClaim> findUnprocessedByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Find claims with validation errors
     */
    @Query("SELECT c FROM CmsClaim c WHERE c.tenantId = :tenantId AND c.hasValidationErrors = true")
    List<CmsClaim> findWithValidationErrorsByTenantId(@Param("tenantId") UUID tenantId);
}
