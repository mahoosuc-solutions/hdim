package com.healthdata.notification.domain.repository;

import com.healthdata.notification.domain.model.Notification;
import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Notification entity with multi-tenant filtering.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Find notification by ID with tenant isolation.
     */
    Optional<Notification> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Find notifications by recipient with tenant isolation.
     */
    Page<Notification> findByRecipientIdAndTenantId(String recipientId, String tenantId, Pageable pageable);

    /**
     * Find notifications by status with tenant isolation.
     */
    List<Notification> findByStatusAndTenantId(NotificationStatus status, String tenantId);

    /**
     * Find pending notifications ready for retry.
     */
    @Query("SELECT n FROM Notification n WHERE n.status IN :statuses AND n.retryCount < n.maxRetries AND n.createdAt > :since ORDER BY n.priority ASC, n.createdAt ASC")
    List<Notification> findPendingForRetry(
        @Param("statuses") List<NotificationStatus> statuses,
        @Param("since") Instant since
    );

    /**
     * Find notifications by channel with tenant isolation.
     */
    Page<Notification> findByChannelAndTenantId(NotificationChannel channel, String tenantId, Pageable pageable);

    /**
     * Count notifications by status and tenant.
     */
    long countByStatusAndTenantId(NotificationStatus status, String tenantId);

    /**
     * Find recent notifications for a recipient.
     */
    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId AND n.tenantId = :tenantId AND n.createdAt > :since ORDER BY n.createdAt DESC")
    List<Notification> findRecentByRecipient(
        @Param("recipientId") String recipientId,
        @Param("tenantId") String tenantId,
        @Param("since") Instant since
    );

    /**
     * Delete old notifications for cleanup (HIPAA compliance).
     */
    @Query("DELETE FROM Notification n WHERE n.createdAt < :before AND n.status IN :terminalStatuses")
    int deleteOldNotifications(
        @Param("before") Instant before,
        @Param("terminalStatuses") List<NotificationStatus> terminalStatuses
    );
}
