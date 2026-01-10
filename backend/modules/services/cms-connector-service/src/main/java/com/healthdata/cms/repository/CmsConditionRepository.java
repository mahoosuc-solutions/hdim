package com.healthdata.cms.repository;

import com.healthdata.cms.model.CmsCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CMS Condition entities
 */
@Repository
public interface CmsConditionRepository extends JpaRepository<CmsCondition, UUID> {

    /**
     * Find all conditions for a patient within a tenant
     */
    List<CmsCondition> findByPatientIdAndTenantId(String patientId, UUID tenantId);

    /**
     * Find conditions by tenant
     */
    List<CmsCondition> findByTenantId(UUID tenantId);

    /**
     * Find condition by ID and tenant (multi-tenant safety)
     */
    Optional<CmsCondition> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Find condition by content hash (for deduplication)
     */
    Optional<CmsCondition> findByContentHash(String contentHash);

    /**
     * Check if condition exists by content hash
     */
    boolean existsByContentHash(String contentHash);

    /**
     * Find active conditions for a patient
     */
    @Query("SELECT c FROM CmsCondition c WHERE c.patientId = :patientId AND c.tenantId = :tenantId AND c.clinicalStatus = 'active'")
    List<CmsCondition> findActiveConditions(@Param("patientId") String patientId, @Param("tenantId") UUID tenantId);

    /**
     * Find conditions by code
     */
    List<CmsCondition> findByCodeSystemAndCodeValueAndTenantId(String codeSystem, String codeValue, UUID tenantId);

    /**
     * Find conditions with onset after a specific date
     */
    @Query("SELECT c FROM CmsCondition c WHERE c.tenantId = :tenantId AND c.onsetDate >= :startDate")
    List<CmsCondition> findConditionsAfterDate(@Param("tenantId") UUID tenantId, @Param("startDate") LocalDate startDate);

    /**
     * Count conditions by patient and tenant
     */
    long countByPatientIdAndTenantId(String patientId, UUID tenantId);

    /**
     * Find unprocessed conditions
     */
    List<CmsCondition> findByTenantIdAndIsProcessedFalse(UUID tenantId);
}
