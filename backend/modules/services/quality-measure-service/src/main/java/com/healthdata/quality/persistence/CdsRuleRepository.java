package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CDS Rule operations
 */
@Repository
public interface CdsRuleRepository extends JpaRepository<CdsRuleEntity, UUID> {

    /**
     * Find all active rules for a tenant, ordered by priority
     */
    List<CdsRuleEntity> findByTenantIdAndActiveTrueOrderByPriorityAsc(String tenantId);

    /**
     * Find all rules by tenant
     */
    List<CdsRuleEntity> findByTenantIdOrderByPriorityAsc(String tenantId);

    /**
     * Find active rules by category
     */
    List<CdsRuleEntity> findByTenantIdAndCategoryAndActiveTrueOrderByPriorityAsc(
        String tenantId,
        CdsRuleEntity.CdsCategory category
    );

    /**
     * Find rule by code
     */
    Optional<CdsRuleEntity> findByTenantIdAndRuleCode(String tenantId, String ruleCode);

    /**
     * Check if a rule with the given code exists
     */
    boolean existsByTenantIdAndRuleCode(String tenantId, String ruleCode);

    /**
     * Find rules by CQL library
     */
    List<CdsRuleEntity> findByTenantIdAndCqlLibraryIdAndActiveTrue(
        String tenantId,
        UUID cqlLibraryId
    );

    /**
     * Count active rules by category
     */
    @Query("SELECT COUNT(r) FROM CdsRuleEntity r " +
           "WHERE r.tenantId = :tenantId AND r.active = true AND r.category = :category")
    Long countActiveRulesByCategory(
        @Param("tenantId") String tenantId,
        @Param("category") CdsRuleEntity.CdsCategory category
    );

    /**
     * Find rules that require acknowledgment
     */
    List<CdsRuleEntity> findByTenantIdAndActiveTrueAndRequiresAcknowledgmentTrueOrderByPriorityAsc(
        String tenantId
    );
}
