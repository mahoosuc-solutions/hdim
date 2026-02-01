package com.healthdata.admin.repository;

import com.healthdata.admin.domain.AlertConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Alert Configuration Repository
 *
 * Data access layer for alert configurations.
 * Enforces multi-tenant isolation at the query level.
 */
@Repository
public interface AlertConfigRepository extends JpaRepository<AlertConfig, UUID> {

    /**
     * Find all alert configurations for a specific tenant
     *
     * @param tenantId Tenant identifier
     * @return List of alert configurations
     */
    List<AlertConfig> findByTenantId(String tenantId);

    /**
     * Find alert configuration by ID and tenant
     * Ensures multi-tenant isolation
     *
     * @param id       Alert configuration ID
     * @param tenantId Tenant identifier
     * @return Optional alert configuration
     */
    Optional<AlertConfig> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Find all enabled alert configurations for a tenant
     * Used by alert evaluation engine
     *
     * @param tenantId Tenant identifier
     * @param enabled  Enabled status
     * @return List of enabled alert configurations
     */
    List<AlertConfig> findByTenantIdAndEnabled(String tenantId, Boolean enabled);

    /**
     * Find alert configurations by service name and tenant
     * Useful for service-specific alert management
     *
     * @param tenantId    Tenant identifier
     * @param serviceName Service name
     * @return List of alert configurations for the service
     */
    List<AlertConfig> findByTenantIdAndServiceName(String tenantId, String serviceName);

    /**
     * Count alert configurations for a tenant
     *
     * @param tenantId Tenant identifier
     * @return Count of alert configurations
     */
    long countByTenantId(String tenantId);

    /**
     * Delete all alert configurations for a tenant
     * Used during tenant cleanup
     *
     * @param tenantId Tenant identifier
     */
    void deleteByTenantId(String tenantId);

    /**
     * Find all enabled alerts across all tenants
     * Used by alert evaluation background job
     *
     * @param enabled Enabled status
     * @return List of all enabled alerts
     */
    List<AlertConfig> findByEnabled(Boolean enabled);
}
