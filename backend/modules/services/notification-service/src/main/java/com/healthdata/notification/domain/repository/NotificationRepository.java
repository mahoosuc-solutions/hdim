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

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByIdAndTenantId(UUID id, String tenantId);

    Page<Notification> findByTenantId(String tenantId, Pageable pageable);

    Page<Notification> findByTenantIdAndRecipientId(String tenantId, String recipientId, Pageable pageable);

    Page<Notification> findByTenantIdAndStatus(String tenantId, NotificationStatus status, Pageable pageable);

    Page<Notification> findByTenantIdAndChannel(String tenantId, NotificationChannel channel, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.retryCount < n.maxRetries")
    List<Notification> findRetryableNotifications(@Param("status") NotificationStatus status);

    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' AND " +
           "(n.scheduledAt IS NULL OR n.scheduledAt <= :now)")
    List<Notification> findPendingNotificationsToSend(@Param("now") Instant now);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.tenantId = :tenantId AND n.status = :status")
    long countByTenantIdAndStatus(@Param("tenantId") String tenantId, @Param("status") NotificationStatus status);

    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.createdAt >= :since")
    Page<Notification> findRecentByTenantId(
        @Param("tenantId") String tenantId, 
        @Param("since") Instant since, 
        Pageable pageable
    );

    List<Notification> findByCorrelationId(String correlationId);
}
