package com.healthdata.notification.application;

import com.healthdata.notification.api.v1.dto.NotificationTemplateRequest;
import com.healthdata.notification.api.v1.dto.NotificationTemplateResponse;
import com.healthdata.notification.domain.model.NotificationTemplate;
import com.healthdata.notification.domain.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing notification templates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TemplateService {

    private final NotificationTemplateRepository templateRepository;

    /**
     * Create a new notification template.
     */
    @Transactional
    public NotificationTemplateResponse createTemplate(NotificationTemplateRequest request, String tenantId, String createdBy) {
        log.info("Creating notification template: {} for channel: {}", request.getName(), request.getChannel());

        if (templateRepository.existsByNameAndChannelAndTenantId(request.getName(), request.getChannel(), tenantId)) {
            throw new IllegalArgumentException("Template with name '" + request.getName() +
                "' already exists for channel " + request.getChannel());
        }

        NotificationTemplate template = NotificationTemplate.builder()
            .tenantId(tenantId)
            .name(request.getName())
            .description(request.getDescription())
            .channel(request.getChannel())
            .subjectTemplate(request.getSubjectTemplate())
            .bodyTemplate(request.getBodyTemplate())
            .variables(request.getVariables())
            .active(request.getActive())
            .createdBy(createdBy)
            .build();

        template = templateRepository.save(template);
        log.info("Created template with ID: {}", template.getId());

        return mapToResponse(template);
    }

    /**
     * Update an existing template.
     */
    @Transactional
    public Optional<NotificationTemplateResponse> updateTemplate(UUID id, NotificationTemplateRequest request, String tenantId) {
        return templateRepository.findByIdAndTenantId(id, tenantId)
            .map(existing -> {
                existing.setName(request.getName());
                existing.setDescription(request.getDescription());
                existing.setSubjectTemplate(request.getSubjectTemplate());
                existing.setBodyTemplate(request.getBodyTemplate());
                existing.setVariables(request.getVariables());
                existing.setActive(request.getActive());
                existing.setVersion(existing.getVersion() + 1);

                NotificationTemplate updated = templateRepository.save(existing);
                log.info("Updated template: {} to version {}", id, updated.getVersion());
                return mapToResponse(updated);
            });
    }

    /**
     * Get template by ID.
     */
    public Optional<NotificationTemplateResponse> getTemplate(UUID id, String tenantId) {
        return templateRepository.findByIdAndTenantId(id, tenantId)
            .map(this::mapToResponse);
    }

    /**
     * Get all templates for a tenant.
     */
    public Page<NotificationTemplateResponse> getTemplates(String tenantId, Pageable pageable) {
        return templateRepository.findByTenantId(tenantId, pageable)
            .map(this::mapToResponse);
    }

    /**
     * Get active templates for a tenant.
     */
    public List<NotificationTemplateResponse> getActiveTemplates(String tenantId) {
        return templateRepository.findByActiveAndTenantId(true, tenantId).stream()
            .map(this::mapToResponse)
            .toList();
    }

    /**
     * Delete a template.
     */
    @Transactional
    public boolean deleteTemplate(UUID id, String tenantId) {
        return templateRepository.findByIdAndTenantId(id, tenantId)
            .map(template -> {
                templateRepository.delete(template);
                log.info("Deleted template: {}", id);
                return true;
            })
            .orElse(false);
    }

    /**
     * Deactivate a template (soft delete).
     */
    @Transactional
    public Optional<NotificationTemplateResponse> deactivateTemplate(UUID id, String tenantId) {
        return templateRepository.findByIdAndTenantId(id, tenantId)
            .map(template -> {
                template.setActive(false);
                NotificationTemplate updated = templateRepository.save(template);
                log.info("Deactivated template: {}", id);
                return mapToResponse(updated);
            });
    }

    private NotificationTemplateResponse mapToResponse(NotificationTemplate template) {
        return NotificationTemplateResponse.builder()
            .id(template.getId())
            .name(template.getName())
            .description(template.getDescription())
            .channel(template.getChannel())
            .subjectTemplate(template.getSubjectTemplate())
            .bodyTemplate(template.getBodyTemplate())
            .variables(template.getVariables())
            .active(template.getActive())
            .version(template.getVersion())
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .createdBy(template.getCreatedBy())
            .build();
    }
}
