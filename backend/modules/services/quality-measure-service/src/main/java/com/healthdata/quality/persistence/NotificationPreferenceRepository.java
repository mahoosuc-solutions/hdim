package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Notification Preference Repository
 *
 * Provides query methods for accessing user notification preferences.
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreferenceEntity, String> {

    /**
     * Find notification preferences by user ID and tenant ID
     */
    Optional<NotificationPreferenceEntity> findByUserIdAndTenantId(String userId, String tenantId);

    /**
     * Find notification preferences for multiple users
     */
    @Query("SELECT n FROM NotificationPreferenceEntity n WHERE n.userId IN :userIds " +
           "AND n.tenantId = :tenantId")
    List<NotificationPreferenceEntity> findByUserIdsAndTenantId(
        @Param("userIds") List<String> userIds,
        @Param("tenantId") String tenantId
    );

    /**
     * Find all preferences with email enabled for a tenant
     */
    @Query("SELECT n FROM NotificationPreferenceEntity n WHERE n.tenantId = :tenantId " +
           "AND n.emailEnabled = true AND n.consentGiven = true")
    List<NotificationPreferenceEntity> findEmailEnabledByTenantId(@Param("tenantId") String tenantId);

    /**
     * Find all preferences with SMS enabled for a tenant
     */
    @Query("SELECT n FROM NotificationPreferenceEntity n WHERE n.tenantId = :tenantId " +
           "AND n.smsEnabled = true AND n.consentGiven = true")
    List<NotificationPreferenceEntity> findSmsEnabledByTenantId(@Param("tenantId") String tenantId);

    /**
     * Check if user exists with notification preferences
     */
    boolean existsByUserIdAndTenantId(String userId, String tenantId);

    /**
     * Find users with consent given
     */
    @Query("SELECT n FROM NotificationPreferenceEntity n WHERE n.tenantId = :tenantId " +
           "AND n.consentGiven = true")
    List<NotificationPreferenceEntity> findConsentedByTenantId(@Param("tenantId") String tenantId);
}
