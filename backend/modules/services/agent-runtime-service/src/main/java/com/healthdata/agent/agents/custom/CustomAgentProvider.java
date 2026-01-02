package com.healthdata.agent.agents.custom;

import com.healthdata.agent.agents.AgentDefinition;
import com.healthdata.agent.client.AgentBuilderClient;
import com.healthdata.agent.client.dto.CustomAgentConfigDTO;
import com.healthdata.agent.core.AgentOrchestrator;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Provider for custom agents defined in the Agent Builder service.
 * Manages caching, retrieval, and lifecycle of custom agent configurations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomAgentProvider {

    private static final String CACHE_NAME = "custom-agents";
    private static final String CIRCUIT_BREAKER_NAME = "agentBuilderService";

    private final AgentBuilderClient agentBuilderClient;
    private final AgentOrchestrator orchestrator;

    /**
     * Local cache of custom agents by tenant and agent type.
     * Structure: tenantId -> (agentType -> adapter)
     */
    private final Map<String, Map<String, CustomAgentAdapter>> agentCache = new ConcurrentHashMap<>();

    /**
     * Global cache of all active custom agent types (for quick lookups).
     */
    private final Set<String> knownCustomAgentTypes = ConcurrentHashMap.newKeySet();

    /**
     * Initialize provider and warm cache on startup.
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing CustomAgentProvider...");
        warmCache();
    }

    /**
     * Get a custom agent by type for a specific tenant.
     *
     * @param agentType The agent type identifier
     * @param tenantId  The tenant ID
     * @return Optional containing the agent if found
     */
    @Cacheable(value = CACHE_NAME, key = "#tenantId + ':' + #agentType")
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getAgentFromCache")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public Optional<AgentDefinition> getAgent(String agentType, String tenantId) {
        log.debug("Fetching custom agent: type={}, tenant={}", agentType, tenantId);

        // Check local cache first
        Map<String, CustomAgentAdapter> tenantCache = agentCache.get(tenantId);
        if (tenantCache != null) {
            CustomAgentAdapter cached = tenantCache.get(agentType);
            if (cached != null) {
                return Optional.of(cached);
            }
        }

        // Fetch from Agent Builder service
        try {
            CustomAgentConfigDTO config = agentBuilderClient.getConfigurationByType(agentType, tenantId);
            if (config != null && isActive(config)) {
                CustomAgentAdapter adapter = new CustomAgentAdapter(config, orchestrator);
                cacheAgent(tenantId, agentType, adapter);
                return Optional.of(adapter);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch custom agent: type={}, tenant={}, error={}",
                agentType, tenantId, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Fallback method when circuit breaker is open.
     */
    public Optional<AgentDefinition> getAgentFromCache(String agentType, String tenantId, Exception e) {
        log.warn("Using cached agent due to circuit breaker: type={}, tenant={}", agentType, tenantId);
        Map<String, CustomAgentAdapter> tenantCache = agentCache.get(tenantId);
        if (tenantCache != null) {
            return Optional.ofNullable(tenantCache.get(agentType));
        }
        return Optional.empty();
    }

    /**
     * Get all available custom agents for a tenant.
     *
     * @param tenantId The tenant ID
     * @return List of available custom agents
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getAgentsFromCache")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public List<AgentDefinition> getAgentsForTenant(String tenantId) {
        log.debug("Fetching all custom agents for tenant: {}", tenantId);

        try {
            List<CustomAgentConfigDTO> configs = agentBuilderClient.getActiveConfigurations(tenantId, "ACTIVE");

            List<AgentDefinition> agents = configs.stream()
                .filter(this::isActive)
                .map(config -> {
                    CustomAgentAdapter adapter = new CustomAgentAdapter(config, orchestrator);
                    cacheAgent(tenantId, config.getAgentType(), adapter);
                    return (AgentDefinition) adapter;
                })
                .collect(Collectors.toList());

            log.info("Loaded {} custom agents for tenant: {}", agents.size(), tenantId);
            return agents;

        } catch (Exception e) {
            log.error("Failed to fetch custom agents for tenant: {}, error={}", tenantId, e.getMessage());
            return getAgentsFromCache(tenantId, e);
        }
    }

    /**
     * Fallback method for getting all agents when circuit breaker is open.
     */
    public List<AgentDefinition> getAgentsFromCache(String tenantId, Exception e) {
        log.warn("Using cached agents due to circuit breaker: tenant={}", tenantId);
        Map<String, CustomAgentAdapter> tenantCache = agentCache.get(tenantId);
        if (tenantCache != null) {
            return new ArrayList<>(tenantCache.values());
        }
        return List.of();
    }

    /**
     * Check if an agent type is a known custom agent.
     *
     * @param agentType The agent type to check
     * @return true if this is a custom agent type
     */
    public boolean isCustomAgentType(String agentType) {
        return knownCustomAgentTypes.contains(agentType);
    }

    /**
     * Invalidate cache for a specific agent.
     *
     * @param agentType The agent type
     * @param tenantId  The tenant ID
     */
    @CacheEvict(value = CACHE_NAME, key = "#tenantId + ':' + #agentType")
    public void invalidateAgent(String agentType, String tenantId) {
        log.info("Invalidating custom agent cache: type={}, tenant={}", agentType, tenantId);
        Map<String, CustomAgentAdapter> tenantCache = agentCache.get(tenantId);
        if (tenantCache != null) {
            tenantCache.remove(agentType);
        }
    }

    /**
     * Invalidate all cached agents for a tenant.
     *
     * @param tenantId The tenant ID
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void invalidateTenant(String tenantId) {
        log.info("Invalidating all custom agents for tenant: {}", tenantId);
        agentCache.remove(tenantId);
    }

    /**
     * Reload a specific agent configuration.
     *
     * @param agentType The agent type
     * @param tenantId  The tenant ID
     */
    public void reloadAgent(String agentType, String tenantId) {
        invalidateAgent(agentType, tenantId);
        getAgent(agentType, tenantId);
    }

    /**
     * Warm the cache by loading all active configurations.
     * Called on startup and periodically.
     */
    @Scheduled(fixedRateString = "${hdim.agent.custom.cache-refresh-minutes:30}000")
    public void warmCache() {
        log.info("Warming custom agent cache...");
        try {
            List<CustomAgentConfigDTO> allConfigs = agentBuilderClient.getAllActiveConfigurations();

            knownCustomAgentTypes.clear();

            for (CustomAgentConfigDTO config : allConfigs) {
                if (isActive(config)) {
                    String tenantId = config.getTenantId();
                    CustomAgentAdapter adapter = new CustomAgentAdapter(config, orchestrator);
                    cacheAgent(tenantId, config.getAgentType(), adapter);
                    knownCustomAgentTypes.add(config.getAgentType());
                }
            }

            log.info("Warmed cache with {} custom agent types across {} tenants",
                knownCustomAgentTypes.size(), agentCache.size());

        } catch (Exception e) {
            log.warn("Failed to warm custom agent cache: {}", e.getMessage());
        }
    }

    /**
     * Get cache statistics.
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("tenantCount", agentCache.size());
        stats.put("knownAgentTypes", knownCustomAgentTypes.size());

        int totalAgents = agentCache.values().stream()
            .mapToInt(Map::size)
            .sum();
        stats.put("totalCachedAgents", totalAgents);

        return stats;
    }

    /**
     * Add an agent to the local cache.
     */
    private void cacheAgent(String tenantId, String agentType, CustomAgentAdapter adapter) {
        agentCache.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>())
            .put(agentType, adapter);
        knownCustomAgentTypes.add(agentType);
    }

    /**
     * Check if a configuration is active and should be available.
     */
    private boolean isActive(CustomAgentConfigDTO config) {
        return config != null &&
               (config.getStatus() == CustomAgentConfigDTO.Status.ACTIVE ||
                config.getStatus() == CustomAgentConfigDTO.Status.TESTING);
    }
}
