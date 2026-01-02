package com.healthdata.agentbuilder.controller;

import com.healthdata.agentbuilder.domain.entity.AgentConfiguration;
import com.healthdata.agentbuilder.domain.entity.AgentConfiguration.AgentStatus;
import com.healthdata.agentbuilder.repository.AgentConfigurationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Agent Runtime service integration.
 * Provides internal API endpoints for fetching agent configurations.
 *
 * This controller is separate from AgentBuilderController to:
 * 1. Provide a stable API contract for service-to-service communication
 * 2. Use different paths that don't require tenant headers for certain operations
 * 3. Support cache warming and cross-tenant queries for admin operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agents/configurations")
@RequiredArgsConstructor
@Tag(name = "Agent Configuration API", description = "Internal API for Agent Runtime integration")
public class AgentConfigurationApiController {

    private final AgentConfigurationRepository repository;

    /**
     * Get active agent configurations for a tenant.
     * Used by Agent Runtime to load custom agents for a specific tenant.
     */
    @GetMapping
    @Operation(summary = "Get agent configurations for a tenant")
    public ResponseEntity<List<AgentConfiguration>> getConfigurations(
            @Parameter(description = "Tenant ID") @RequestParam String tenantId,
            @Parameter(description = "Status filter") @RequestParam(defaultValue = "ACTIVE") String status) {

        log.debug("Fetching configurations for tenant: {}, status: {}", tenantId, status);

        AgentStatus agentStatus;
        try {
            agentStatus = AgentStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            agentStatus = AgentStatus.ACTIVE;
        }

        List<AgentConfiguration> configs;
        if (agentStatus == AgentStatus.ACTIVE) {
            configs = repository.findActiveAgents(tenantId);
        } else {
            configs = repository.findByTenantIdAndStatus(tenantId, agentStatus,
                org.springframework.data.domain.Pageable.unpaged()).getContent();
        }

        log.info("Found {} configurations for tenant: {}", configs.size(), tenantId);
        return ResponseEntity.ok(configs);
    }

    /**
     * Get a specific agent configuration by ID.
     * Used by Agent Runtime for direct config lookup.
     */
    @GetMapping("/{configId}")
    @Operation(summary = "Get agent configuration by ID")
    public ResponseEntity<AgentConfiguration> getConfiguration(
            @Parameter(description = "Configuration ID") @PathVariable UUID configId) {

        log.debug("Fetching configuration: {}", configId);

        return repository.findById(configId)
            .map(config -> {
                log.debug("Found configuration: {} ({})", config.getName(), config.getSlug());
                return ResponseEntity.ok(config);
            })
            .orElseGet(() -> {
                log.warn("Configuration not found: {}", configId);
                return ResponseEntity.notFound().build();
            });
    }

    /**
     * Get agent configuration by agent type (slug) for a tenant.
     * Primary lookup method used by Agent Runtime.
     */
    @GetMapping("/by-type/{agentType}")
    @Operation(summary = "Get agent configuration by type (slug)")
    public ResponseEntity<AgentConfiguration> getConfigurationByType(
            @Parameter(description = "Agent type (slug)") @PathVariable String agentType,
            @Parameter(description = "Tenant ID") @RequestParam String tenantId) {

        log.debug("Fetching configuration by type: {}, tenant: {}", agentType, tenantId);

        return repository.findByTenantIdAndSlug(tenantId, agentType)
            .filter(config -> config.getStatus() == AgentStatus.ACTIVE ||
                             config.getStatus() == AgentStatus.TESTING)
            .map(config -> {
                log.debug("Found configuration: {} for type: {}", config.getId(), agentType);
                return ResponseEntity.ok(config);
            })
            .orElseGet(() -> {
                log.warn("Configuration not found for type: {}, tenant: {}", agentType, tenantId);
                return ResponseEntity.notFound().build();
            });
    }

    /**
     * Get all active configurations across all tenants.
     * Used by Agent Runtime for cache warming on startup.
     * Requires admin/service authentication (enforced by gateway).
     */
    @GetMapping("/all-active")
    @Operation(summary = "Get all active configurations (admin/service use only)")
    public ResponseEntity<List<AgentConfiguration>> getAllActiveConfigurations() {

        log.info("Fetching all active configurations for cache warming");

        List<AgentConfiguration> configs = repository.findAllActiveConfigurations();

        log.info("Found {} active configurations across all tenants", configs.size());
        return ResponseEntity.ok(configs);
    }

    /**
     * Health check endpoint for circuit breaker monitoring.
     */
    @GetMapping("/health")
    @Operation(summary = "Health check for Agent Configuration API")
    public ResponseEntity<HealthStatus> healthCheck() {
        long totalConfigs = repository.count();
        long activeConfigs = repository.findAllActiveConfigurations().size();

        return ResponseEntity.ok(new HealthStatus(
            "UP",
            totalConfigs,
            activeConfigs,
            System.currentTimeMillis()
        ));
    }

    public record HealthStatus(
        String status,
        long totalConfigurations,
        long activeConfigurations,
        long timestamp
    ) {}
}
