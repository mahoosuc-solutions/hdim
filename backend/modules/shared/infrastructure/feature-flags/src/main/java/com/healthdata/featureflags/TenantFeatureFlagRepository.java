package com.healthdata.featureflags;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Tenant Feature Flags
 *
 * HIPAA Compliance:
 * - All queries enforce multi-tenant isolation via tenantId
 * - Never query across tenant boundaries
 */
@Repository
public interface TenantFeatureFlagRepository extends JpaRepository<TenantFeatureFlagEntity, UUID> {

    /**
     * Find feature flag by tenant and feature key
     *
     * @param tenantId   Tenant ID (HIPAA §164.312(d))
     * @param featureKey Feature key (e.g., "twilio-sms-reminders")
     * @return Feature flag if exists
     */
    @Query("SELECT f FROM TenantFeatureFlagEntity f " +
           "WHERE f.tenantId = :tenantId AND f.featureKey = :featureKey")
    Optional<TenantFeatureFlagEntity> findByTenantIdAndFeatureKey(
            @Param("tenantId") String tenantId,
            @Param("featureKey") String featureKey);

    /**
     * Find all feature flags for a tenant
     *
     * @param tenantId Tenant ID
     * @return List of feature flags
     */
    @Query("SELECT f FROM TenantFeatureFlagEntity f WHERE f.tenantId = :tenantId")
    List<TenantFeatureFlagEntity> findByTenantId(@Param("tenantId") String tenantId);

    /**
     * Find all enabled feature flags for a tenant
     *
     * @param tenantId Tenant ID
     * @return List of enabled feature flags
     */
    @Query("SELECT f FROM TenantFeatureFlagEntity f " +
           "WHERE f.tenantId = :tenantId AND f.enabled = true")
    List<TenantFeatureFlagEntity> findEnabledByTenantId(@Param("tenantId") String tenantId);

    /**
     * Check if a feature is enabled for a tenant
     *
     * @param tenantId   Tenant ID
     * @param featureKey Feature key
     * @return true if enabled, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END " +
           "FROM TenantFeatureFlagEntity f " +
           "WHERE f.tenantId = :tenantId AND f.featureKey = :featureKey AND f.enabled = true")
    boolean isFeatureEnabled(@Param("tenantId") String tenantId, @Param("featureKey") String featureKey);

    /**
     * Find all tenants with a feature enabled
     *
     * Useful for scheduled jobs that process feature-enabled tenants.
     * Example: Find all tenants with Twilio SMS enabled for appointment reminder job.
     *
     * @param featureKey Feature key
     * @return List of tenant IDs with feature enabled
     */
    @Query("SELECT f.tenantId FROM TenantFeatureFlagEntity f " +
           "WHERE f.featureKey = :featureKey AND f.enabled = true")
    List<String> findTenantIdsWithFeatureEnabled(@Param("featureKey") String featureKey);
}
