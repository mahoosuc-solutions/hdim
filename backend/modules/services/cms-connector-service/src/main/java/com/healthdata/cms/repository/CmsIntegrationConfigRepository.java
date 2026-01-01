package com.healthdata.cms.repository;

import com.healthdata.cms.model.CmsIntegrationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CMS Integration Configuration Repository
 * 
 * Data access layer for CMS Integration Config entities.
 */
@Repository
public interface CmsIntegrationConfigRepository extends JpaRepository<CmsIntegrationConfig, UUID> {

    /**
     * Find CMS integration config for a tenant
     */
    Optional<CmsIntegrationConfig> findByTenantId(UUID tenantId);

    /**
     * Find all active integrations
     */
    List<CmsIntegrationConfig> findByStatusAndIsActiveTrue(CmsIntegrationConfig.IntegrationStatus status);

    /**
     * Find all verified integrations
     */
    @Query("SELECT c FROM CmsIntegrationConfig c WHERE c.status = 'VERIFIED' AND c.isActive = true")
    List<CmsIntegrationConfig> findVerifiedConfigs();

    /**
     * Find integrations needing sync (based on last sync time and interval)
     */
    @Query("""
        SELECT c FROM CmsIntegrationConfig c 
        WHERE c.status = 'ACTIVE' 
        AND c.isActive = true 
        AND c.apiType IN ('BCDA', 'MULTI')
        AND (c.lastSyncTimestamp IS NULL OR TIMESTAMPDIFF(HOUR, c.lastSyncTimestamp, NOW()) >= c.bcdaSyncIntervalHours)
        """)
    List<CmsIntegrationConfig> findDueForSync();

    /**
     * Find DPC-enabled integrations
     */
    @Query("SELECT c FROM CmsIntegrationConfig c WHERE c.dpcRealTimeEnabled = true AND c.isActive = true")
    List<CmsIntegrationConfig> findDpcEnabledConfigs();

    /**
     * Find integrations by type
     */
    List<CmsIntegrationConfig> findByApiTypeAndIsActiveTrue(CmsIntegrationConfig.IntegrationType apiType);

    /**
     * Find failed integrations
     */
    List<CmsIntegrationConfig> findByStatusAndIsActiveTrue(CmsIntegrationConfig.IntegrationStatus status);

    /**
     * Check if tenant has CMS integration
     */
    boolean existsByTenantId(UUID tenantId);
}
