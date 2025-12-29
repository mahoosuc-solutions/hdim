package com.healthdata.notification.application;

import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationTemplate;
import com.healthdata.notification.domain.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TemplateService {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*}}");

    private final NotificationTemplateRepository templateRepository;

    /**
     * Create a new template.
     */
    public NotificationTemplate createTemplate(CreateTemplateRequest request) {
        log.info("Creating template {} for tenant {}", request.getCode(), request.getTenantId());

        if (templateRepository.existsByTenantIdAndCode(request.getTenantId(), request.getCode())) {
            throw new IllegalArgumentException("Template with code already exists: " + request.getCode());
        }

        List<String> variables = extractVariables(request.getBodyTemplate());
        if (request.getSubjectTemplate() != null) {
            variables.addAll(extractVariables(request.getSubjectTemplate()));
        }

        NotificationTemplate template = NotificationTemplate.builder()
            .tenantId(request.getTenantId())
            .code(request.getCode())
            .name(request.getName())
            .description(request.getDescription())
            .channel(request.getChannel())
            .subjectTemplate(request.getSubjectTemplate())
            .bodyTemplate(request.getBodyTemplate())
            .htmlTemplate(request.getHtmlTemplate())
            .variables(new ArrayList<>(new HashSet<>(variables)))
            .active(true)
            .createdBy(request.getCreatedBy())
            .build();

        return templateRepository.save(template);
    }

    /**
     * Update an existing template.
     */
    public NotificationTemplate updateTemplate(UUID id, String tenantId, UpdateTemplateRequest request) {
        NotificationTemplate template = templateRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));

        if (request.getName() != null) {
            template.setName(request.getName());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getSubjectTemplate() != null) {
            template.setSubjectTemplate(request.getSubjectTemplate());
        }
        if (request.getBodyTemplate() != null) {
            template.setBodyTemplate(request.getBodyTemplate());
            List<String> variables = extractVariables(request.getBodyTemplate());
            if (template.getSubjectTemplate() != null) {
                variables.addAll(extractVariables(template.getSubjectTemplate()));
            }
            template.setVariables(new ArrayList<>(new HashSet<>(variables)));
        }
        if (request.getHtmlTemplate() != null) {
            template.setHtmlTemplate(request.getHtmlTemplate());
        }
        if (request.getActive() != null) {
            template.setActive(request.getActive());
        }
        
        template.setUpdatedBy(request.getUpdatedBy());
        template.setVersion(template.getVersion() + 1);

        return templateRepository.save(template);
    }

    /**
     * Get template by ID.
     */
    @Transactional(readOnly = true)
    public Optional<NotificationTemplate> getTemplate(UUID id, String tenantId) {
        return templateRepository.findByIdAndTenantId(id, tenantId);
    }

    /**
     * Get template by code.
     */
    @Transactional(readOnly = true)
    public Optional<NotificationTemplate> getTemplateByCode(String tenantId, String code) {
        return templateRepository.findByTenantIdAndCodeAndActiveTrue(tenantId, code);
    }

    /**
     * Get all templates for a tenant.
     */
    @Transactional(readOnly = true)
    public Page<NotificationTemplate> getTemplates(String tenantId, Pageable pageable) {
        return templateRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * Get templates by channel.
     */
    @Transactional(readOnly = true)
    public List<NotificationTemplate> getTemplatesByChannel(String tenantId, NotificationChannel channel) {
        return templateRepository.findByTenantIdAndChannelAndActiveTrue(tenantId, channel);
    }

    /**
     * Delete (deactivate) a template.
     */
    public void deleteTemplate(UUID id, String tenantId) {
        NotificationTemplate template = templateRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
        
        template.setActive(false);
        templateRepository.save(template);
    }

    /**
     * Render a template with variable substitution.
     */
    public String renderTemplate(String template, Map<String, Object> variables) {
        if (template == null || variables == null || variables.isEmpty()) {
            return template;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = resolveVariable(variableName, variables);
            String replacement = value != null ? Matcher.quoteReplacement(value.toString()) : "";
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Extract variable names from a template.
     */
    public List<String> extractVariables(String template) {
        List<String> variables = new ArrayList<>();
        if (template == null) {
            return variables;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return variables;
    }

    /**
     * Resolve nested variable paths (e.g., "patient.name").
     */
    @SuppressWarnings("unchecked")
    private Object resolveVariable(String path, Map<String, Object> variables) {
        String[] parts = path.split("\\.");
        Object current = variables;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CreateTemplateRequest {
        private String tenantId;
        private String code;
        private String name;
        private String description;
        private NotificationChannel channel;
        private String subjectTemplate;
        private String bodyTemplate;
        private String htmlTemplate;
        private String createdBy;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UpdateTemplateRequest {
        private String name;
        private String description;
        private String subjectTemplate;
        private String bodyTemplate;
        private String htmlTemplate;
        private Boolean active;
        private String updatedBy;
    }
}
