package com.healthdata.fhir.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for FHIR Subscription entities.
 */
@Repository
public interface FhirSubscriptionRepository extends JpaRepository<FhirSubscription, UUID> {

    /**
     * Find all subscriptions for a tenant.
     */
    List<FhirSubscription> findByTenantId(String tenantId);

    /**
     * Find active subscriptions for a tenant.
     */
    List<FhirSubscription> findByTenantIdAndStatus(
        String tenantId,
        FhirSubscription.SubscriptionStatus status
    );

    /**
     * Find active subscriptions for a resource type.
     */
    @Query("SELECT s FROM FhirSubscription s WHERE s.tenantId = :tenantId " +
           "AND s.resourceType = :resourceType AND s.status = 'ACTIVE' " +
           "AND (s.endTime IS NULL OR s.endTime > :now)")
    List<FhirSubscription> findActiveSubscriptionsForResource(
        @Param("tenantId") String tenantId,
        @Param("resourceType") String resourceType,
        @Param("now") Instant now
    );

    /**
     * Find all active WebSocket subscriptions for a tenant.
     */
    @Query("SELECT s FROM FhirSubscription s WHERE s.tenantId = :tenantId " +
           "AND s.channelType = 'WEBSOCKET' AND s.status = 'ACTIVE' " +
           "AND (s.endTime IS NULL OR s.endTime > :now)")
    List<FhirSubscription> findActiveWebSocketSubscriptions(
        @Param("tenantId") String tenantId,
        @Param("now") Instant now
    );

    /**
     * Find subscriptions in error state.
     */
    List<FhirSubscription> findByStatusAndErrorCountLessThan(
        FhirSubscription.SubscriptionStatus status,
        int maxErrors
    );

    /**
     * Find expired subscriptions.
     */
    @Query("SELECT s FROM FhirSubscription s WHERE s.status = 'ACTIVE' " +
           "AND s.endTime IS NOT NULL AND s.endTime < :now")
    List<FhirSubscription> findExpiredSubscriptions(@Param("now") Instant now);

    /**
     * Count active subscriptions for a tenant.
     */
    long countByTenantIdAndStatus(String tenantId, FhirSubscription.SubscriptionStatus status);

    /**
     * Find subscriptions by tag.
     */
    List<FhirSubscription> findByTenantIdAndTag(String tenantId, String tag);
}
