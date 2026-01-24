package com.healthdata.admin.service;

import com.healthdata.admin.domain.AlertConfig;
import com.healthdata.admin.dto.AlertConfigRequest;
import com.healthdata.admin.dto.AlertConfigResponse;
import com.healthdata.admin.dto.AlertConfigUpdateRequest;
import com.healthdata.admin.repository.AlertConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Alert Configuration Service
 *
 * Business logic for managing alert configurations.
 * Enforces multi-tenant isolation and validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AlertConfigService {

    private final AlertConfigRepository alertConfigRepository;

    /**
     * Get all alert configurations for a tenant
     *
     * @param tenantId Tenant identifier
     * @return List of alert configuration responses
     */
    public List<AlertConfigResponse> getAllAlertConfigs(String tenantId) {
        log.info("Fetching all alert configurations for tenant: {}", tenantId);

        List<AlertConfig> configs = alertConfigRepository.findByTenantId(tenantId);

        log.info("Found {} alert configurations for tenant: {}", configs.size(), tenantId);

        return configs.stream()
                .map(AlertConfigResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get alert configuration by ID
     *
     * @param tenantId Tenant identifier
     * @param id       Alert configuration ID
     * @return Alert configuration response
     * @throws IllegalArgumentException if alert not found
     */
    public AlertConfigResponse getAlertConfig(String tenantId, UUID id) {
        log.info("Fetching alert configuration: {} for tenant: {}", id, tenantId);

        AlertConfig config = alertConfigRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Alert configuration not found: " + id));

        return AlertConfigResponse.fromEntity(config);
    }

    /**
     * Create a new alert configuration
     *
     * @param tenantId Tenant identifier
     * @param request  Alert configuration request
     * @param username Username of the creator
     * @return Created alert configuration response
     */
    @Transactional
    public AlertConfigResponse createAlertConfig(
            String tenantId,
            AlertConfigRequest request,
            String username) {

        log.info("Creating alert configuration for tenant: {}, service: {}, type: {}",
                tenantId, request.getServiceName(), request.getAlertType());

        AlertConfig config = AlertConfig.builder()
                .tenantId(tenantId)
                .serviceName(request.getServiceName())
                .displayName(request.getDisplayName())
                .alertType(request.getAlertType())
                .threshold(request.getThreshold())
                .durationMinutes(request.getDurationMinutes())
                .severity(request.getSeverity())
                .enabled(request.getEnabled())
                .notificationChannels(request.getNotificationChannels())
                .createdBy(username)
                .build();

        AlertConfig saved = alertConfigRepository.save(config);

        log.info("Created alert configuration: {} for tenant: {}", saved.getId(), tenantId);

        return AlertConfigResponse.fromEntity(saved);
    }

    /**
     * Update an existing alert configuration
     *
     * @param tenantId Tenant identifier
     * @param id       Alert configuration ID
     * @param request  Update request
     * @return Updated alert configuration response
     * @throws IllegalArgumentException if alert not found
     */
    @Transactional
    public AlertConfigResponse updateAlertConfig(
            String tenantId,
            UUID id,
            AlertConfigUpdateRequest request) {

        log.info("Updating alert configuration: {} for tenant: {}", id, tenantId);

        AlertConfig config = alertConfigRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Alert configuration not found: " + id));

        // Update only non-null fields
        if (request.getThreshold() != null) {
            config.setThreshold(request.getThreshold());
        }
        if (request.getDurationMinutes() != null) {
            config.setDurationMinutes(request.getDurationMinutes());
        }
        if (request.getSeverity() != null) {
            config.setSeverity(request.getSeverity());
        }
        if (request.getEnabled() != null) {
            config.setEnabled(request.getEnabled());
        }
        if (request.getNotificationChannels() != null) {
            config.setNotificationChannels(request.getNotificationChannels());
        }

        AlertConfig updated = alertConfigRepository.save(config);

        log.info("Updated alert configuration: {} for tenant: {}", id, tenantId);

        return AlertConfigResponse.fromEntity(updated);
    }

    /**
     * Delete an alert configuration
     *
     * @param tenantId Tenant identifier
     * @param id       Alert configuration ID
     * @throws IllegalArgumentException if alert not found
     */
    @Transactional
    public void deleteAlertConfig(String tenantId, UUID id) {
        log.info("Deleting alert configuration: {} for tenant: {}", id, tenantId);

        AlertConfig config = alertConfigRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Alert configuration not found: " + id));

        alertConfigRepository.delete(config);

        log.info("Deleted alert configuration: {} for tenant: {}", id, tenantId);
    }

    /**
     * Get all enabled alert configurations for a tenant
     * Used by alert evaluation engine
     *
     * @param tenantId Tenant identifier
     * @return List of enabled alert configurations
     */
    public List<AlertConfig> getEnabledAlertConfigs(String tenantId) {
        log.debug("Fetching enabled alert configurations for tenant: {}", tenantId);
        return alertConfigRepository.findByTenantIdAndEnabled(tenantId, true);
    }

    /**
     * Get all enabled alert configurations across all tenants
     * Used by alert evaluation background job
     *
     * @return List of all enabled alert configurations
     */
    public List<AlertConfig> getAllEnabledAlertConfigs() {
        log.debug("Fetching all enabled alert configurations");
        return alertConfigRepository.findByEnabled(true);
    }

    /**
     * Mark alert as triggered
     * Updates the last triggered timestamp
     *
     * @param alertConfigId Alert configuration ID
     */
    @Transactional
    public void markAlertTriggered(UUID alertConfigId) {
        log.info("Marking alert as triggered: {}", alertConfigId);

        alertConfigRepository.findById(alertConfigId).ifPresent(config -> {
            config.markTriggered();
            alertConfigRepository.save(config);
        });
    }
}
