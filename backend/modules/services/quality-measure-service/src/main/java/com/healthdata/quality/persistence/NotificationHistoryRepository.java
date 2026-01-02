package com.healthdata.quality.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Notification History Repository
 *
 * Provides data access methods for notification history audit trail.
 * Supports HIPAA compliance queries and analytics.
 */
@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistoryEntity, UUID> {

    /**
     * Find all notifications for a specific patient
     *
     * @param patientId Patient FHIR ID
     * @param pageable Pagination parameters
     * @return Page of notification history records
     */
    Page<NotificationHistoryEntity> findByPatientIdOrderBySentAtDesc(
            UUID patientId,
            Pageable pageable
    );

    /**
     * Find all notifications for a specific tenant
     *
     * @param tenantId Tenant ID
     * @param pageable Pagination parameters
     * @return Page of notification history records
     */
    Page<NotificationHistoryEntity> findByTenantIdOrderBySentAtDesc(
            String tenantId,
            Pageable pageable
    );

    /**
     * Find notifications by channel and status
     *
     * @param channel Channel type (EMAIL, SMS, WEBSOCKET, etc.)
     * @param status Delivery status (SENT, DELIVERED, FAILED, etc.)
     * @param pageable Pagination parameters
     * @return Page of notification history records
     */
    Page<NotificationHistoryEntity> findByChannelAndStatusOrderBySentAtDesc(
            String channel,
            String status,
            Pageable pageable
    );

    /**
     * Find notifications by type within a date range
     *
     * @param notificationType Notification type (CRITICAL_ALERT, HEALTH_SCORE_UPDATE, etc.)
     * @param startDate Start of date range
     * @param endDate End of date range
     * @param pageable Pagination parameters
     * @return Page of notification history records
     */
    Page<NotificationHistoryEntity> findByNotificationTypeAndSentAtBetweenOrderBySentAtDesc(
            String notificationType,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    );

    /**
     * Find failed notifications for retry
     *
     * @param status Status to filter by (typically "FAILED")
     * @param sentAfter Only include failures after this time
     * @param pageable Pagination parameters
     * @return Page of failed notification records
     */
    Page<NotificationHistoryEntity> findByStatusAndSentAtAfterOrderBySentAtDesc(
            String status,
            Instant sentAfter,
            Pageable pageable
    );

    /**
     * Find notifications by alert ID
     * Useful for tracking all notifications related to a specific alert
     *
     * @param alertId Alert ID that triggered notifications
     * @return List of notification history records
     */
    List<NotificationHistoryEntity> findByAlertIdOrderBySentAtDesc(String alertId);

    /**
     * Count notifications by channel within a date range
     * Useful for analytics and reporting
     *
     * @param channel Channel type (EMAIL, SMS, etc.)
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of notifications
     */
    long countByChannelAndSentAtBetween(
            String channel,
            Instant startDate,
            Instant endDate
    );

    /**
     * Count failed notifications by channel
     * Useful for monitoring delivery reliability
     *
     * @param channel Channel type
     * @param status Status (typically "FAILED")
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of failed notifications
     */
    long countByChannelAndStatusAndSentAtBetween(
            String channel,
            String status,
            Instant startDate,
            Instant endDate
    );

    /**
     * Find recent notifications for a patient and tenant
     * Used to avoid duplicate notifications
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param notificationType Notification type
     * @param sentAfter Only include notifications sent after this time
     * @return List of recent notifications
     */
    List<NotificationHistoryEntity> findByTenantIdAndPatientIdAndNotificationTypeAndSentAtAfter(
            String tenantId,
            UUID patientId,
            String notificationType,
            Instant sentAfter
    );

    /**
     * Get notification delivery statistics by channel
     * Returns aggregated counts grouped by status
     *
     * @param channel Channel type (EMAIL, SMS, etc.)
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of [status, count] tuples
     */
    @Query("""
        SELECT n.status, COUNT(n)
        FROM NotificationHistoryEntity n
        WHERE n.channel = :channel
          AND n.sentAt BETWEEN :startDate AND :endDate
        GROUP BY n.status
        ORDER BY COUNT(n) DESC
        """)
    List<Object[]> getDeliveryStatsByChannel(
            @Param("channel") String channel,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    /**
     * Get notification volume by type over time
     * Useful for trend analysis
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of [notificationType, count] tuples
     */
    @Query("""
        SELECT n.notificationType, COUNT(n)
        FROM NotificationHistoryEntity n
        WHERE n.sentAt BETWEEN :startDate AND :endDate
        GROUP BY n.notificationType
        ORDER BY COUNT(n) DESC
        """)
    List<Object[]> getNotificationVolumeByType(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    /**
     * Find notifications pending delivery confirmation
     * (sent but no delivered_at timestamp)
     *
     * @param sentBefore Only include notifications sent before this time
     * @param pageable Pagination parameters
     * @return Page of pending notifications
     */
    @Query("""
        SELECT n
        FROM NotificationHistoryEntity n
        WHERE n.status = 'SENT'
          AND n.deliveredAt IS NULL
          AND n.sentAt < :sentBefore
        ORDER BY n.sentAt ASC
        """)
    Page<NotificationHistoryEntity> findPendingDeliveryConfirmations(
            @Param("sentBefore") Instant sentBefore,
            Pageable pageable
    );

    /**
     * Get average delivery time by channel
     * (time between sent_at and delivered_at)
     *
     * @param channel Channel type
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Average delivery time in seconds
     */
    @Query(value = """
        SELECT AVG(EXTRACT(EPOCH FROM (n.delivered_at - n.sent_at)))
        FROM notification_history n
        WHERE n.channel = :channel
          AND n.delivered_at IS NOT NULL
          AND n.sent_at BETWEEN :startDate AND :endDate
        """, nativeQuery = true)
    Double getAverageDeliveryTimeSeconds(
            @Param("channel") String channel,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
}
