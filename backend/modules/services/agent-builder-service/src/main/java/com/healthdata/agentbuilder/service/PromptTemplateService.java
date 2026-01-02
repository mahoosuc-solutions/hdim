package com.healthdata.agentbuilder.service;

import com.healthdata.agentbuilder.config.CacheConfig;
import com.healthdata.agentbuilder.domain.entity.PromptTemplate;
import com.healthdata.agentbuilder.domain.entity.PromptTemplate.TemplateCategory;
import com.healthdata.agentbuilder.domain.entity.PromptTemplate.TemplateVariable;
import com.healthdata.agentbuilder.repository.PromptTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for managing prompt templates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private final PromptTemplateRepository templateRepository;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");

    /**
     * Create a new prompt template.
     * Evicts cache for the tenant's template list.
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_PROMPT_TEMPLATES, key = "#template.tenantId"),
        @CacheEvict(value = CacheConfig.CACHE_SYSTEM_TEMPLATES, allEntries = true, condition = "#template.isSystem")
    })
    public PromptTemplate create(PromptTemplate template, String userId) {
        // Validate unique name
        if (templateRepository.existsByTenantIdAndName(template.getTenantId(), template.getName())) {
            throw new AgentConfigurationService.AgentBuilderException(
                "Template with name '" + template.getName() + "' already exists"
            );
        }

        // Extract variables from content
        List<TemplateVariable> variables = extractVariables(template.getContent());
        template.setVariables(variables);

        template.setCreatedBy(userId);

        PromptTemplate saved = templateRepository.save(template);
        log.info("Created prompt template: {} for tenant: {}", saved.getId(), saved.getTenantId());
        return saved;
    }

    /**
     * Update an existing prompt template.
     * Evicts cache for the tenant's template list.
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_PROMPT_TEMPLATES, key = "#updates.tenantId")
    public PromptTemplate update(UUID templateId, PromptTemplate updates, String userId) {
        PromptTemplate existing = templateRepository.findByTenantIdAndId(updates.getTenantId(), templateId)
            .orElseThrow(() -> new AgentConfigurationService.AgentBuilderException("Template not found: " + templateId));

        if (existing.getIsSystem()) {
            throw new AgentConfigurationService.AgentBuilderException("Cannot modify system templates");
        }

        if (updates.getName() != null) {
            existing.setName(updates.getName());
        }
        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }
        if (updates.getCategory() != null) {
            existing.setCategory(updates.getCategory());
        }
        if (updates.getContent() != null) {
            existing.setContent(updates.getContent());
            // Re-extract variables
            List<TemplateVariable> variables = extractVariables(updates.getContent());
            existing.setVariables(variables);
        }

        PromptTemplate saved = templateRepository.save(existing);
        log.info("Updated prompt template: {}", saved.getId());
        return saved;
    }

    /**
     * Get template by ID.
     */
    @Transactional(readOnly = true)
    public Optional<PromptTemplate> getById(String tenantId, UUID templateId) {
        return templateRepository.findByTenantIdAndId(tenantId, templateId);
    }

    /**
     * List templates for a tenant.
     */
    @Transactional(readOnly = true)
    public Page<PromptTemplate> list(String tenantId, Pageable pageable) {
        return templateRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * List templates by category.
     */
    @Transactional(readOnly = true)
    public Page<PromptTemplate> listByCategory(String tenantId, TemplateCategory category, Pageable pageable) {
        return templateRepository.findByTenantIdAndCategory(tenantId, category, pageable);
    }

    /**
     * Get all available templates (tenant + system).
     * Cached for 30 minutes per tenant.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_PROMPT_TEMPLATES, key = "#tenantId")
    public List<PromptTemplate> getAvailableTemplates(String tenantId) {
        log.debug("Cache miss: loading templates for tenant {}", tenantId);
        return templateRepository.findAvailableTemplates(tenantId);
    }

    /**
     * Get available templates by category.
     */
    @Transactional(readOnly = true)
    public List<PromptTemplate> getAvailableTemplatesByCategory(String tenantId, TemplateCategory category) {
        return templateRepository.findAvailableTemplatesByCategory(tenantId, category);
    }

    /**
     * Delete a template.
     * Evicts cache for the tenant's template list.
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_PROMPT_TEMPLATES, key = "#tenantId")
    public void delete(String tenantId, UUID templateId) {
        PromptTemplate template = templateRepository.findByTenantIdAndId(tenantId, templateId)
            .orElseThrow(() -> new AgentConfigurationService.AgentBuilderException("Template not found: " + templateId));

        if (template.getIsSystem()) {
            throw new AgentConfigurationService.AgentBuilderException("Cannot delete system templates");
        }

        templateRepository.delete(template);
        log.info("Deleted prompt template: {}", templateId);
    }

    /**
     * Render a template with variables.
     */
    @Transactional(readOnly = true)
    public String renderTemplate(String tenantId, UUID templateId, Map<String, String> variables) {
        PromptTemplate template = templateRepository.findByTenantIdAndId(tenantId, templateId)
            .orElseThrow(() -> new AgentConfigurationService.AgentBuilderException("Template not found: " + templateId));

        return renderContent(template.getContent(), variables);
    }

    /**
     * Render template content with variables.
     */
    public String renderContent(String content, Map<String, String> variables) {
        if (content == null || variables == null || variables.isEmpty()) {
            return content;
        }

        String result = content;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String pattern = "\\{\\{\\s*" + Pattern.quote(entry.getKey()) + "\\s*\\}\\}";
            result = result.replaceAll(pattern, Matcher.quoteReplacement(entry.getValue()));
        }

        return result;
    }

    /**
     * Validate template syntax.
     */
    public TemplateValidationResult validateTemplate(String content) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> variableNames = new HashSet<>();

        // Check for balanced braces
        int openCount = 0;
        int closeCount = 0;
        for (char c : content.toCharArray()) {
            if (c == '{') openCount++;
            if (c == '}') closeCount++;
        }
        if (openCount != closeCount) {
            errors.add("Unbalanced braces in template");
        }

        // Extract and validate variables
        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        while (matcher.find()) {
            String variable = matcher.group(1);
            variableNames.add(variable);
        }

        // Check for common issues
        if (content.contains("{{") && !content.contains("}}")) {
            errors.add("Found opening {{ without matching }}");
        }

        // Check for empty variables
        if (content.contains("{{}}") || content.contains("{{ }}")) {
            errors.add("Empty variable placeholder found");
        }

        // Warnings for potentially missing variables
        if (content.toLowerCase().contains("patient") && !variableNames.contains("patient_name") && !variableNames.contains("patientName")) {
            warnings.add("Template mentions 'patient' but no patient variable is defined");
        }

        return new TemplateValidationResult(
            errors.isEmpty(),
            new ArrayList<>(variableNames),
            errors,
            warnings
        );
    }

    /**
     * Clone a template.
     */
    @Transactional
    public PromptTemplate cloneTemplate(String tenantId, UUID sourceId, String newName, String userId) {
        PromptTemplate source = templateRepository.findByTenantIdAndId(tenantId, sourceId)
            .orElseGet(() -> templateRepository.findById(sourceId)
                .filter(t -> t.getIsSystem())
                .orElseThrow(() -> new AgentConfigurationService.AgentBuilderException("Template not found: " + sourceId)));

        List<TemplateVariable> clonedVariables = source.getVariables() != null
            ? source.getVariables().stream()
                .map(v -> new TemplateVariable(v.getName(), v.getDescription(), v.getDefaultValue(), v.isRequired()))
                .collect(Collectors.toList())
            : new ArrayList<>();

        PromptTemplate clone = PromptTemplate.builder()
            .tenantId(tenantId)
            .name(newName)
            .description(source.getDescription() + " (cloned)")
            .category(source.getCategory())
            .content(source.getContent())
            .variables(clonedVariables)
            .isSystem(false)
            .createdBy(userId)
            .build();

        return templateRepository.save(clone);
    }

    private List<TemplateVariable> extractVariables(String content) {
        List<TemplateVariable> variables = new ArrayList<>();
        if (content == null) return variables;

        Set<String> seen = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        while (matcher.find()) {
            String varName = matcher.group(1);
            if (seen.add(varName)) {
                variables.add(new TemplateVariable(varName, null, null, false));
            }
        }
        return variables;
    }

    public record TemplateValidationResult(
        boolean valid,
        List<String> variables,
        List<String> errors,
        List<String> warnings
    ) {}
}
