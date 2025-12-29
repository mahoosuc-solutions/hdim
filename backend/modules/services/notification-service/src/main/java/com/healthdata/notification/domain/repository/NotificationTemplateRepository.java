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

/**
 * Repository for NotificationTemplate entity with multi-tenant filtering.
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    /**
     * Find template by ID with tenant isolation.
     */
    Optional<NotificationTemplate> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Find template by name and channel with tenant isolation.
     */
    Optional<NotificationTemplate> findByNameAndChannelAndTenantId(String name, NotificationChannel channel, String tenantId);

    /**
     * Find active templates by channel.
     */
    List<NotificationTemplate> findByChannelAndActiveAndTenantId(NotificationChannel channel, Boolean active, String tenantId);

    /**
     * Find all templates for a tenant.
     */
    Page<NotificationTemplate> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Find active templates for a tenant.
     */
    List<NotificationTemplate> findByActiveAndTenantId(Boolean active, String tenantId);

    /**
     * Check if template name exists.
     */
    boolean existsByNameAndChannelAndTenantId(String name, NotificationChannel channel, String tenantId);
}
