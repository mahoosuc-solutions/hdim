package com.healthdata.notification.domain.repository;

import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    Optional<NotificationPreference> findByTenantIdAndUserIdAndChannel(
        String tenantId, String userId, NotificationChannel channel
    );

    List<NotificationPreference> findByTenantIdAndUserId(String tenantId, String userId);

    List<NotificationPreference> findByTenantIdAndUserIdAndEnabledTrue(String tenantId, String userId);

    boolean existsByTenantIdAndUserIdAndChannel(String tenantId, String userId, NotificationChannel channel);

    void deleteByTenantIdAndUserId(String tenantId, String userId);
}
