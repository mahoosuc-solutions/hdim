package com.healthdata.notification.domain.repository;

import com.healthdata.notification.domain.model.NotificationType;
import com.healthdata.notification.domain.model.ProviderNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for provider notification preferences.
 * Issue #148: Smart Notification Preferences
 */
@Repository
public interface ProviderNotificationPreferenceRepository
        extends JpaRepository<ProviderNotificationPreference, UUID> {

    /**
     * Find all preferences for a provider.
     */
    List<ProviderNotificationPreference> findByTenantIdAndProviderId(
            String tenantId, UUID providerId);

    /**
     * Find preference for a specific notification type.
     */
    Optional<ProviderNotificationPreference> findByTenantIdAndProviderIdAndNotificationType(
            String tenantId, UUID providerId, NotificationType notificationType);

    /**
     * Find all enabled preferences for a provider.
     */
    @Query("SELECT p FROM ProviderNotificationPreference p " +
           "WHERE p.tenantId = :tenantId " +
           "AND p.providerId = :providerId " +
           "AND p.enabled = true")
    List<ProviderNotificationPreference> findEnabledByTenantIdAndProviderId(
            @Param("tenantId") String tenantId,
            @Param("providerId") UUID providerId);

    /**
     * Find all providers who have enabled a specific notification type.
     */
    @Query("SELECT p FROM ProviderNotificationPreference p " +
           "WHERE p.tenantId = :tenantId " +
           "AND p.notificationType = :type " +
           "AND p.enabled = true")
    List<ProviderNotificationPreference> findProvidersWithEnabledType(
            @Param("tenantId") String tenantId,
            @Param("type") NotificationType type);

    /**
     * Check if provider has notifications enabled for a type.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM ProviderNotificationPreference p " +
           "WHERE p.tenantId = :tenantId " +
           "AND p.providerId = :providerId " +
           "AND p.notificationType = :type " +
           "AND p.enabled = true")
    boolean isNotificationEnabled(
            @Param("tenantId") String tenantId,
            @Param("providerId") UUID providerId,
            @Param("type") NotificationType type);

    /**
     * Delete all preferences for a provider.
     */
    void deleteByTenantIdAndProviderId(String tenantId, UUID providerId);

    /**
     * Find providers with digest enabled for a notification type.
     */
    @Query("SELECT p FROM ProviderNotificationPreference p " +
           "WHERE p.tenantId = :tenantId " +
           "AND p.notificationType = :type " +
           "AND p.enabled = true " +
           "AND p.digestEnabled = true " +
           "AND p.digestFrequency = :frequency")
    List<ProviderNotificationPreference> findDigestSubscribers(
            @Param("tenantId") String tenantId,
            @Param("type") NotificationType type,
            @Param("frequency") String frequency);
}
