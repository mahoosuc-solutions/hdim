package com.healthdata.authentication.repository;

import com.healthdata.authentication.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for API Key entity operations.
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    /**
     * Find API key by its hash.
     */
    Optional<ApiKey> findByKeyHash(String keyHash);

    /**
     * Find API key by its prefix (for identification).
     */
    Optional<ApiKey> findByKeyPrefix(String keyPrefix);

    /**
     * Find all API keys for a tenant.
     */
    List<ApiKey> findByTenantId(String tenantId);

    /**
     * Find all active API keys for a tenant.
     */
    List<ApiKey> findByTenantIdAndActiveTrue(String tenantId);

    /**
     * Find all API keys created by a user.
     */
    List<ApiKey> findByCreatedBy(UUID userId);

    /**
     * Find all expired API keys.
     */
    @Query("SELECT k FROM ApiKey k WHERE k.expiresAt IS NOT NULL AND k.expiresAt < :now AND k.active = true")
    List<ApiKey> findExpiredKeys(@Param("now") Instant now);

    /**
     * Find all API keys that haven't been used in a while.
     */
    @Query("SELECT k FROM ApiKey k WHERE k.lastUsedAt IS NOT NULL AND k.lastUsedAt < :threshold AND k.active = true")
    List<ApiKey> findUnusedKeys(@Param("threshold") Instant threshold);

    /**
     * Count active API keys for a tenant.
     */
    long countByTenantIdAndActiveTrue(String tenantId);

    /**
     * Check if a key hash already exists.
     */
    boolean existsByKeyHash(String keyHash);

    /**
     * Check if a key prefix already exists.
     */
    boolean existsByKeyPrefix(String keyPrefix);

    /**
     * Find API keys by name pattern.
     */
    List<ApiKey> findByNameContainingIgnoreCaseAndTenantId(String name, String tenantId);

    /**
     * Find all revoked API keys for audit.
     */
    List<ApiKey> findByRevokedAtIsNotNull();
}
