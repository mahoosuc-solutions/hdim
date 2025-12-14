package com.healthdata.agentbuilder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agentbuilder.config.CacheConfig;
import com.healthdata.agentbuilder.domain.entity.AgentConfiguration;
import com.healthdata.agentbuilder.domain.entity.AgentConfiguration.AgentStatus;
import com.healthdata.agentbuilder.domain.entity.AgentVersion;
import com.healthdata.agentbuilder.domain.entity.AgentVersion.ChangeType;
import com.healthdata.agentbuilder.domain.entity.AgentVersion.VersionStatus;
import com.healthdata.agentbuilder.repository.AgentConfigurationRepository;
import com.healthdata.agentbuilder.repository.AgentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Service for managing agent configurations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentConfigurationService {

    private final AgentConfigurationRepository agentRepository;
    private final AgentVersionRepository versionRepository;
    private final ObjectMapper objectMapper;

    @Value("${hdim.agent-builder.max-agents-per-tenant:50}")
    private int maxAgentsPerTenant;

    @Value("${hdim.agent-builder.max-versions-per-agent:100}")
    private int maxVersionsPerAgent;

    /**
     * Create a new agent configuration.
     * Evicts active agents cache for the tenant.
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_ACTIVE_AGENTS, key = "#agent.tenantId")
    public AgentConfiguration create(AgentConfiguration agent, String userId) {
        // Validate tenant limit
        long currentCount = agentRepository.countByTenantId(agent.getTenantId());
        if (currentCount >= maxAgentsPerTenant) {
            throw new AgentBuilderException("Maximum agents per tenant reached: " + maxAgentsPerTenant);
        }

        // Validate unique name
        if (agentRepository.existsByTenantIdAndName(agent.getTenantId(), agent.getName())) {
            throw new AgentBuilderException("Agent with name '" + agent.getName() + "' already exists");
        }

        agent.setCreatedBy(userId);
        agent.setStatus(AgentStatus.DRAFT);

        AgentConfiguration saved = agentRepository.save(agent);

        // Create initial version
        createVersion(saved, "Initial version", ChangeType.MAJOR, userId);

        log.info("Created agent: {} for tenant: {}", saved.getId(), saved.getTenantId());
        return saved;
    }

    /**
     * Update an existing agent configuration.
     * Evicts caches for active agents and individual config.
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_ACTIVE_AGENTS, key = "#updates.tenantId"),
        @CacheEvict(value = CacheConfig.CACHE_AGENT_CONFIG, key = "#agentId")
    })
    public AgentConfiguration update(UUID agentId, AgentConfiguration updates, String userId, String changeSummary) {
        AgentConfiguration existing = getByIdOrThrow(updates.getTenantId(), agentId);

        // Validate status allows updates
        if (existing.getStatus() == AgentStatus.ARCHIVED) {
            throw new AgentBuilderException("Cannot update archived agent");
        }

        // Update fields
        if (updates.getName() != null) {
            existing.setName(updates.getName());
        }
        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }
        if (updates.getPersonaName() != null) {
            existing.setPersonaName(updates.getPersonaName());
        }
        if (updates.getPersonaRole() != null) {
            existing.setPersonaRole(updates.getPersonaRole());
        }
        if (updates.getModelProvider() != null) {
            existing.setModelProvider(updates.getModelProvider());
        }
        if (updates.getModelId() != null) {
            existing.setModelId(updates.getModelId());
        }
        if (updates.getMaxTokens() != null) {
            existing.setMaxTokens(updates.getMaxTokens());
        }
        if (updates.getTemperature() != null) {
            existing.setTemperature(updates.getTemperature());
        }
        if (updates.getSystemPrompt() != null) {
            existing.setSystemPrompt(updates.getSystemPrompt());
        }
        if (updates.getWelcomeMessage() != null) {
            existing.setWelcomeMessage(updates.getWelcomeMessage());
        }
        if (updates.getToolConfiguration() != null) {
            existing.setToolConfiguration(updates.getToolConfiguration());
        }
        if (updates.getGuardrailConfiguration() != null) {
            existing.setGuardrailConfiguration(updates.getGuardrailConfiguration());
        }
        if (updates.getUiConfiguration() != null) {
            existing.setUiConfiguration(updates.getUiConfiguration());
        }
        if (updates.getAllowedRoles() != null) {
            existing.setAllowedRoles(updates.getAllowedRoles());
        }
        if (updates.getTags() != null) {
            existing.setTags(updates.getTags());
        }

        existing.setUpdatedBy(userId);

        // Increment version
        String newVersion = incrementVersion(existing.getVersion());
        existing.setVersion(newVersion);

        AgentConfiguration saved = agentRepository.save(existing);

        // Create version snapshot
        createVersion(saved, changeSummary, ChangeType.MINOR, userId);

        log.info("Updated agent: {} to version: {}", saved.getId(), newVersion);
        return saved;
    }

    /**
     * Get agent by ID.
     */
    @Transactional(readOnly = true)
    public Optional<AgentConfiguration> getById(String tenantId, UUID agentId) {
        return agentRepository.findByTenantIdAndId(tenantId, agentId);
    }

    /**
     * Get agent by ID or throw.
     */
    @Transactional(readOnly = true)
    public AgentConfiguration getByIdOrThrow(String tenantId, UUID agentId) {
        return getById(tenantId, agentId)
            .orElseThrow(() -> new AgentNotFoundException("Agent not found: " + agentId));
    }

    /**
     * Get agent by slug.
     */
    @Transactional(readOnly = true)
    public Optional<AgentConfiguration> getBySlug(String tenantId, String slug) {
        return agentRepository.findByTenantIdAndSlug(tenantId, slug);
    }

    /**
     * List agents for a tenant.
     */
    @Transactional(readOnly = true)
    public Page<AgentConfiguration> list(String tenantId, Pageable pageable) {
        return agentRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * List agents by status.
     */
    @Transactional(readOnly = true)
    public Page<AgentConfiguration> listByStatus(String tenantId, AgentStatus status, Pageable pageable) {
        return agentRepository.findByTenantIdAndStatus(tenantId, status, pageable);
    }

    /**
     * Search agents.
     */
    @Transactional(readOnly = true)
    public Page<AgentConfiguration> search(String tenantId, String query, Pageable pageable) {
        return agentRepository.searchByTenantId(tenantId, query, pageable);
    }

    /**
     * Get active agents.
     * Cached for 5 minutes per tenant.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_ACTIVE_AGENTS, key = "#tenantId")
    public List<AgentConfiguration> getActiveAgents(String tenantId) {
        log.debug("Cache miss: loading active agents for tenant {}", tenantId);
        return agentRepository.findActiveAgents(tenantId);
    }

    /**
     * Delete (archive) an agent.
     * Evicts caches.
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_ACTIVE_AGENTS, key = "#tenantId"),
        @CacheEvict(value = CacheConfig.CACHE_AGENT_CONFIG, key = "#agentId")
    })
    public void delete(String tenantId, UUID agentId, String userId) {
        AgentConfiguration agent = getByIdOrThrow(tenantId, agentId);

        // Soft delete - archive
        agent.setStatus(AgentStatus.ARCHIVED);
        agent.setArchivedAt(Instant.now());
        agent.setUpdatedBy(userId);

        agentRepository.save(agent);
        log.info("Archived agent: {}", agentId);
    }

    /**
     * Publish an agent (make it active).
     * Evicts caches since status changes.
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_ACTIVE_AGENTS, key = "#tenantId"),
        @CacheEvict(value = CacheConfig.CACHE_AGENT_CONFIG, key = "#agentId")
    })
    public AgentConfiguration publish(String tenantId, UUID agentId, String userId) {
        AgentConfiguration agent = getByIdOrThrow(tenantId, agentId);

        if (agent.getStatus() == AgentStatus.ARCHIVED) {
            throw new AgentBuilderException("Cannot publish archived agent");
        }

        // Mark current published version as superseded
        versionRepository.findByAgentConfigurationIdAndStatus(agentId, VersionStatus.PUBLISHED)
            .ifPresent(v -> {
                v.setStatus(VersionStatus.SUPERSEDED);
                versionRepository.save(v);
            });

        // Update latest version to published
        versionRepository.findLatestVersion(agentId)
            .ifPresent(v -> {
                v.setStatus(VersionStatus.PUBLISHED);
                v.setPublishedAt(Instant.now());
                v.setPublishedBy(userId);
                versionRepository.save(v);
            });

        agent.setStatus(AgentStatus.ACTIVE);
        agent.setPublishedAt(Instant.now());
        agent.setUpdatedBy(userId);

        AgentConfiguration saved = agentRepository.save(agent);
        log.info("Published agent: {} by user: {}", agentId, userId);
        return saved;
    }

    /**
     * Deprecate an agent.
     */
    @Transactional
    public AgentConfiguration deprecate(String tenantId, UUID agentId, String userId) {
        AgentConfiguration agent = getByIdOrThrow(tenantId, agentId);

        agent.setStatus(AgentStatus.DEPRECATED);
        agent.setUpdatedBy(userId);

        AgentConfiguration saved = agentRepository.save(agent);
        log.info("Deprecated agent: {}", agentId);
        return saved;
    }

    /**
     * Clone an agent.
     */
    @Transactional
    public AgentConfiguration clone(String tenantId, UUID sourceAgentId, String newName, String userId) {
        AgentConfiguration source = getByIdOrThrow(tenantId, sourceAgentId);

        AgentConfiguration clone = AgentConfiguration.builder()
            .tenantId(tenantId)
            .name(newName)
            .description(source.getDescription() + " (cloned)")
            .personaName(source.getPersonaName())
            .personaRole(source.getPersonaRole())
            .personaAvatarUrl(source.getPersonaAvatarUrl())
            .modelProvider(source.getModelProvider())
            .modelId(source.getModelId())
            .maxTokens(source.getMaxTokens())
            .temperature(source.getTemperature())
            .systemPrompt(source.getSystemPrompt())
            .welcomeMessage(source.getWelcomeMessage())
            .toolConfiguration(source.getToolConfiguration())
            .guardrailConfiguration(source.getGuardrailConfiguration())
            .uiConfiguration(source.getUiConfiguration())
            .allowedRoles(source.getAllowedRoles())
            .requiresPatientContext(source.getRequiresPatientContext())
            .tags(source.getTags())
            .build();

        return create(clone, userId);
    }

    private void createVersion(AgentConfiguration agent, String changeSummary, ChangeType changeType, String userId) {
        // Check version limit
        long versionCount = versionRepository.countByAgentConfigurationId(agent.getId());
        if (versionCount >= maxVersionsPerAgent) {
            throw new AgentBuilderException("Maximum versions per agent reached: " + maxVersionsPerAgent);
        }

        Map<String, Object> snapshot = createConfigSnapshot(agent);

        AgentVersion version = AgentVersion.builder()
            .agentConfiguration(agent)
            .versionNumber(agent.getVersion())
            .configurationSnapshot(snapshot)
            .status(VersionStatus.DRAFT)
            .changeSummary(changeSummary)
            .changeType(changeType)
            .createdBy(userId)
            .build();

        versionRepository.save(version);
    }

    private Map<String, Object> createConfigSnapshot(AgentConfiguration agent) {
        return objectMapper.convertValue(agent, Map.class);
    }

    private String incrementVersion(String version) {
        String[] parts = version.split("\\.");
        int patch = Integer.parseInt(parts[2]) + 1;
        return parts[0] + "." + parts[1] + "." + patch;
    }

    public static class AgentBuilderException extends RuntimeException {
        public AgentBuilderException(String message) {
            super(message);
        }
    }

    public static class AgentNotFoundException extends RuntimeException {
        public AgentNotFoundException(String message) {
            super(message);
        }
    }
}
