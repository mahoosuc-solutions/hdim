package com.healthdata.featureflags;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tenant Feature Flag Service
 *
 * Provides feature flag management with Redis caching for performance.
 * Cache TTL: 5 minutes (HIPAA compliant for non-PHI configuration data).
 *
 * HIPAA Compliance:
 * - Multi-tenant isolation enforced
 * - Audit trail maintained via entity timestamps
 * - Configuration data is not PHI (tenant-level settings only)
 *
 * Usage:
 * <pre>
 * if (featureFlagService.isFeatureEnabled(tenantId, "twilio-sms-reminders")) {
 *     // Send SMS reminder
 * }
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TenantFeatureFlagService {

    private final TenantFeatureFlagRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Check if a feature is enabled for a tenant
     *
     * Cached for 5 minutes to minimize database queries.
     *
     * @param tenantId   Tenant ID (HIPAA §164.312(d))
     * @param featureKey Feature key (e.g., "twilio-sms-reminders")
     * @return true if enabled, false otherwise
     */
    @Cacheable(value = "tenant-feature-flags", key = "#tenantId + ':' + #featureKey")
    public boolean isFeatureEnabled(String tenantId, String featureKey) {
        log.debug("Checking feature flag: tenantId={}, featureKey={}", tenantId, featureKey);

        // Check tenant-specific flag first
        boolean enabled = repository.isFeatureEnabled(tenantId, featureKey);

        // Fallback to default tenant if no tenant-specific flag exists
        if (!enabled && !"default".equals(tenantId)) {
            enabled = repository.isFeatureEnabled("default", featureKey);
            log.debug("Using default feature flag for tenantId={}, featureKey={}: {}",
                    tenantId, featureKey, enabled);
        }

        return enabled;
    }

    /**
     * Get feature flag configuration
     *
     * Returns feature-specific configuration as a Map.
     * Cached for 5 minutes.
     *
     * @param tenantId   Tenant ID
     * @param featureKey Feature key
     * @return Configuration map, or empty map if not found
     */
    @Cacheable(value = "tenant-feature-config", key = "#tenantId + ':' + #featureKey")
    public Map<String, Object> getFeatureConfig(String tenantId, String featureKey) {
        log.debug("Getting feature config: tenantId={}, featureKey={}", tenantId, featureKey);

        Optional<TenantFeatureFlagEntity> flag = repository.findByTenantIdAndFeatureKey(tenantId, featureKey);

        // Fallback to default tenant if no tenant-specific flag
        if (flag.isEmpty() && !"default".equals(tenantId)) {
            flag = repository.findByTenantIdAndFeatureKey("default", featureKey);
        }

        return flag.map(this::parseConfig).orElse(Map.of());
    }

    /**
     * Get all feature flags for a tenant
     *
     * @param tenantId Tenant ID
     * @return List of feature flags
     */
    public List<TenantFeatureFlagEntity> getFeatureFlags(String tenantId) {
        log.debug("Getting all feature flags for tenantId={}", tenantId);
        return repository.findByTenantId(tenantId);
    }

    /**
     * Get all enabled features for a tenant
     *
     * @param tenantId Tenant ID
     * @return List of enabled feature keys
     */
    public List<String> getEnabledFeatures(String tenantId) {
        log.debug("Getting enabled features for tenantId={}", tenantId);
        return repository.findEnabledByTenantId(tenantId)
                .stream()
                .map(TenantFeatureFlagEntity::getFeatureKey)
                .toList();
    }

    /**
     * Enable a feature for a tenant
     *
     * @param tenantId   Tenant ID
     * @param featureKey Feature key
     * @param config     Feature configuration (optional)
     * @param userId     User enabling the feature (for audit trail)
     */
    @Transactional
    @CacheEvict(value = {"tenant-feature-flags", "tenant-feature-config"},
                key = "#tenantId + ':' + #featureKey")
    public void enableFeature(String tenantId, String featureKey, Map<String, Object> config, String userId) {
        log.info("Enabling feature: tenantId={}, featureKey={}, userId={}", tenantId, featureKey, userId);

        TenantFeatureFlagEntity flag = repository.findByTenantIdAndFeatureKey(tenantId, featureKey)
                .orElse(TenantFeatureFlagEntity.builder()
                        .tenantId(tenantId)
                        .featureKey(featureKey)
                        .createdBy(userId)
                        .build());

        flag.setEnabled(true);
        flag.setUpdatedBy(userId);

        if (config != null && !config.isEmpty()) {
            flag.setConfigJson(serializeConfig(config));
        }

        repository.save(flag);
    }

    /**
     * Disable a feature for a tenant
     *
     * @param tenantId   Tenant ID
     * @param featureKey Feature key
     * @param userId     User disabling the feature (for audit trail)
     */
    @Transactional
    @CacheEvict(value = {"tenant-feature-flags", "tenant-feature-config"},
                key = "#tenantId + ':' + #featureKey")
    public void disableFeature(String tenantId, String featureKey, String userId) {
        log.info("Disabling feature: tenantId={}, featureKey={}, userId={}", tenantId, featureKey, userId);

        repository.findByTenantIdAndFeatureKey(tenantId, featureKey)
                .ifPresent(flag -> {
                    flag.setEnabled(false);
                    flag.setUpdatedBy(userId);
                    repository.save(flag);
                });
    }

    /**
     * Update feature configuration without changing enabled status
     *
     * @param tenantId   Tenant ID
     * @param featureKey Feature key
     * @param config     New configuration
     * @param userId     User updating the config (for audit trail)
     */
    @Transactional
    @CacheEvict(value = "tenant-feature-config", key = "#tenantId + ':' + #featureKey")
    public void updateFeatureConfig(String tenantId, String featureKey, Map<String, Object> config, String userId) {
        log.info("Updating feature config: tenantId={}, featureKey={}, userId={}", tenantId, featureKey, userId);

        repository.findByTenantIdAndFeatureKey(tenantId, featureKey)
                .ifPresent(flag -> {
                    flag.setConfigJson(serializeConfig(config));
                    flag.setUpdatedBy(userId);
                    repository.save(flag);
                });
    }

    /**
     * Find all tenant IDs with a feature enabled
     *
     * Useful for scheduled jobs (e.g., appointment reminder job)
     *
     * @param featureKey Feature key
     * @return List of tenant IDs
     */
    public List<String> findTenantsWithFeatureEnabled(String featureKey) {
        log.debug("Finding tenants with feature enabled: {}", featureKey);
        return repository.findTenantIdsWithFeatureEnabled(featureKey);
    }

    // Helper methods

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseConfig(TenantFeatureFlagEntity flag) {
        if (flag.getConfigJson() == null || flag.getConfigJson().isBlank()) {
            return Map.of();
        }

        try {
            return (Map<String, Object>) objectMapper.readValue(flag.getConfigJson(), Map.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse config JSON for tenantId={}, featureKey={}: {}",
                    flag.getTenantId(), flag.getFeatureKey(), e.getMessage());
            return Map.of();
        }
    }

    private String serializeConfig(Map<String, Object> config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize config JSON: {}", e.getMessage());
            return "{}";
        }
    }
}
