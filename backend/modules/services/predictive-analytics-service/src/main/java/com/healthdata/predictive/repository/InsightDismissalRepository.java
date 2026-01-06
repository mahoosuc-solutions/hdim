package com.healthdata.predictive.repository;

import com.healthdata.predictive.entity.InsightDismissalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Issue #19: Population Health Insights Engine
 *
 * Repository for insight dismissal tracking.
 */
@Repository
public interface InsightDismissalRepository extends JpaRepository<InsightDismissalEntity, UUID> {

    /**
     * Find active dismissals for a provider (not expired)
     */
    @Query("SELECT d FROM InsightDismissalEntity d " +
           "WHERE d.tenantId = :tenantId " +
           "AND d.providerId = :providerId " +
           "AND d.active = true " +
           "AND (d.expiresAt IS NULL OR d.expiresAt > :now)")
    List<InsightDismissalEntity> findActiveDismissals(
        @Param("tenantId") String tenantId,
        @Param("providerId") String providerId,
        @Param("now") Instant now
    );

    /**
     * Get dismissed insight keys for filtering
     */
    @Query("SELECT d.insightKey FROM InsightDismissalEntity d " +
           "WHERE d.tenantId = :tenantId " +
           "AND d.providerId = :providerId " +
           "AND d.active = true " +
           "AND (d.expiresAt IS NULL OR d.expiresAt > :now)")
    Set<String> findDismissedInsightKeys(
        @Param("tenantId") String tenantId,
        @Param("providerId") String providerId,
        @Param("now") Instant now
    );

    /**
     * Find a specific dismissal by insight key
     */
    Optional<InsightDismissalEntity> findByTenantIdAndProviderIdAndInsightKeyAndActiveTrue(
        String tenantId,
        String providerId,
        String insightKey
    );

    /**
     * Find by insight ID
     */
    Optional<InsightDismissalEntity> findByTenantIdAndProviderIdAndInsightIdAndActiveTrue(
        String tenantId,
        String providerId,
        UUID insightId
    );

    /**
     * Count dismissals by provider
     */
    long countByTenantIdAndProviderIdAndActiveTrue(String tenantId, String providerId);
}
