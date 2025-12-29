package com.healthdata.notification.domain.repository;

import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByIdAndTenantId(UUID id, String tenantId);

    Optional<NotificationTemplate> findByTenantIdAndCode(String tenantId, String code);

    Optional<NotificationTemplate> findByTenantIdAndCodeAndActiveTrue(String tenantId, String code);

    Page<NotificationTemplate> findByTenantId(String tenantId, Pageable pageable);

    Page<NotificationTemplate> findByTenantIdAndActiveTrue(String tenantId, Pageable pageable);

    List<NotificationTemplate> findByTenantIdAndChannel(String tenantId, NotificationChannel channel);

    List<NotificationTemplate> findByTenantIdAndChannelAndActiveTrue(String tenantId, NotificationChannel channel);

    boolean existsByTenantIdAndCode(String tenantId, String code);
}
