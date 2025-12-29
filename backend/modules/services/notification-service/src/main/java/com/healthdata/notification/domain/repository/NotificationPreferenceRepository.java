package com.healthdata.notification.domain.repository;

import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for NotificationPreference entity with multi-tenant filtering.
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    /**
     * Find preference by user, channel and tenant.
     */
    Optional<NotificationPreference> findByUserIdAndChannelAndTenantId(String userId, NotificationChannel channel, String tenantId);

    /**
     * Find all preferences for a user with tenant isolation.
     */
    List<NotificationPreference> findByUserIdAndTenantId(String userId, String tenantId);

    /**
     * Find enabled preferences for a user.
     */
    List<NotificationPreference> findByUserIdAndEnabledAndTenantId(String userId, Boolean enabled, String tenantId);

    /**
     * Check if preference exists.
     */
    boolean existsByUserIdAndChannelAndTenantId(String userId, NotificationChannel channel, String tenantId);

    /**
     * Delete all preferences for a user (for GDPR/right to be forgotten).
     */
    void deleteByUserIdAndTenantId(String userId, String tenantId);
}
