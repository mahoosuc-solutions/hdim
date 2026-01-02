package com.healthdata.agentbuilder.controller;

import com.healthdata.agentbuilder.client.AgentRuntimeClient;
import com.healthdata.agentbuilder.domain.entity.AgentConfiguration;
import com.healthdata.agentbuilder.domain.entity.AgentConfiguration.AgentStatus;
import com.healthdata.agentbuilder.domain.entity.AgentTestSession;
import com.healthdata.agentbuilder.domain.entity.AgentTestSession.TestType;
import com.healthdata.agentbuilder.domain.entity.AgentVersion;
import com.healthdata.agentbuilder.domain.entity.PromptTemplate;
import com.healthdata.agentbuilder.domain.entity.PromptTemplate.TemplateCategory;
import com.healthdata.agentbuilder.service.*;
import com.healthdata.agentbuilder.service.AgentTestService.*;
import com.healthdata.agentbuilder.service.AgentVersionService.VersionDiff;
import com.healthdata.agentbuilder.service.PromptTemplateService.TemplateValidationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for the No-Code Agent Builder.
 * Provides endpoints for creating, managing, testing, and publishing custom AI agents.
 *
 * Note: This service uses Pattern 3 (No Auth) - authentication and authorization
 * are handled by the gateway. User identity is passed via X-User-ID header.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent-builder")
@RequiredArgsConstructor
@Tag(name = "Agent Builder", description = "No-code agent configuration and management")
public class AgentBuilderController {

    private final AgentConfigurationService agentService;
    private final AgentVersionService versionService;
    private final AgentTestService testService;
    private final PromptTemplateService templateService;
    private final AgentRuntimeClient runtimeClient;

    // ==================== Agent Configuration Endpoints ====================

    @PostMapping("/agents")
    @Operation(summary = "Create a new agent configuration")
    public ResponseEntity<AgentConfiguration> createAgent(
            @RequestBody @Valid AgentConfiguration agent,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId) {

        agent.setTenantId(tenantId);

        AgentConfiguration created = agentService.create(agent, userId);
        log.info("Created agent {} for tenant {}", created.getId(), tenantId);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/agents/{agentId}")
    @Operation(summary = "Update an agent configuration")
    public ResponseEntity<AgentConfiguration> updateAgent(
            @PathVariable UUID agentId,
            @RequestBody @Valid AgentConfiguration updates,
            @RequestParam(required = false, defaultValue = "Configuration updated") String changeSummary,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId) {

        updates.setTenantId(tenantId);

        AgentConfiguration updated = agentService.update(agentId, updates, userId, changeSummary);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/agents/{agentId}")
    @Operation(summary = "Get an agent configuration by ID")
    public ResponseEntity<AgentConfiguration> getAgent(
            @PathVariable UUID agentId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        return agentService.getById(tenantId, agentId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/agents/slug/{slug}")
    @Operation(summary = "Get an agent configuration by slug")
    public ResponseEntity<AgentConfiguration> getAgentBySlug(
            @PathVariable String slug,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        return agentService.getBySlug(tenantId, slug)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/agents")
    @Operation(summary = "List all agents for the tenant")
    public ResponseEntity<Page<AgentConfiguration>> listAgents(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AgentConfiguration> agents = agentService.list(tenantId, pageable);
        return ResponseEntity.ok(agents);
    }

    @GetMapping("/agents/status/{status}")
    @Operation(summary = "List agents by status")
    public ResponseEntity<Page<AgentConfiguration>> listAgentsByStatus(
            @PathVariable AgentStatus status,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AgentConfiguration> agents = agentService.listByStatus(tenantId, status, pageable);
        return ResponseEntity.ok(agents);
    }

    @GetMapping("/agents/search")
    @Operation(summary = "Search agents by name or description")
    public ResponseEntity<Page<AgentConfiguration>> searchAgents(
            @RequestParam String query,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AgentConfiguration> agents = agentService.search(tenantId, query, pageable);
        return ResponseEntity.ok(agents);
    }

    @GetMapping("/agents/active")
    @Operation(summary = "Get all active agents")
    public ResponseEntity<List<AgentConfiguration>> getActiveAgents(
            @RequestHeader("X-Tenant-ID") String tenantId) {

        List<AgentConfiguration> agents = agentService.getActiveAgents(tenantId);
        return ResponseEntity.ok(agents);
    }

    @DeleteMapping("/agents/{agentId}")
    @Operation(summary = "Archive an agent (soft delete)")
    public ResponseEntity<Void> deleteAgent(
            @PathVariable UUID agentId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId) {

        agentService.delete(tenantId, agentId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/agents/{agentId}/publish")
    @Operation(summary = "Publish an agent (make it active)")
    public ResponseEntity<AgentConfiguration> publishAgent(
            @PathVariable UUID agentId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId) {

        AgentConfiguration published = agentService.publish(tenantId, agentId, userId);
        return ResponseEntity.ok(published);
    }

    @PostMapping("/agents/{agentId}/deprecate")
    @Operation(summary = "Deprecate an agent")
    public ResponseEntity<AgentConfiguration> deprecateAgent(
            @PathVariable UUID agentId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId) {

        AgentConfiguration deprecated = agentService.deprecate(tenantId, agentId, userId);
        return ResponseEntity.ok(deprecated);
    }

    @PostMapping("/agents/{agentId}/clone")
    @Operation(summary = "Clone an existing agent")
    public ResponseEntity<AgentConfiguration> cloneAgent(
            @PathVariable UUID agentId,
            @RequestParam String newName,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId) {

        AgentConfiguration cloned = agentService.clone(tenantId, agentId, newName, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(cloned);
    }

    // ==================== Version Management Endpoints ====================

    @GetMapping("/agents/{agentId}/versions")
    @Operation(summary = "Get all versions of an agent")
    public ResponseEntity<Page<AgentVersion>> getVersions(
            @PathVariable UUID agentId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AgentVersion> versions = versionService.getVersions(agentId, pageable);
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/agents/{agentId}/versions/history")
    @Operation(summary = "Get version history for an agent")
    public ResponseEntity<List<AgentVersion>> getVersionHistory(
            @PathVariable UUID agentId) {

        List<AgentVersion> history = versionService.getVersionHistory(agentId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/agents/{agentId}/versions/{versionNumber}")
    @Operation(summary = "Get a specific version")
    public ResponseEntity<AgentVersion> getVersion(
            @PathVariable UUID agentId,
            @PathVariable String versionNumber) {

        return versionService.getVersion(agentId, versionNumber)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/agents/{agentId}/versions/latest")
    @Operation(summary = "Get the latest version")
    public ResponseEntity<AgentVersion> getLatestVersion(
            @PathVariable UUID agentId) {

        return versionService.getLatestVersion(agentId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/agents/{agentId}/versions/published")
    @Operation(summary = "Get the published version")
    public ResponseEntity<AgentVersion> getPublishedVersion(
            @PathVariable UUID agentId) {

        return versionService.getPublishedVersion(agentId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/agents/{agentId}/rollback")
    @Operation(summary = "Rollback to a previous version")
    public ResponseEntity<AgentConfiguration> rollback(
            @PathVariable UUID agentId,
            @RequestParam String targetVersion,
            @RequestParam(required = false) String reason,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId) {

        AgentConfiguration rolledBack = versionService.rollback(
            tenantId, agentId, targetVersion, reason, userId
        );
        return ResponseEntity.ok(rolledBack);
    }

    @GetMapping("/agents/{agentId}/versions/compare")
    @Operation(summary = "Compare two versions")
    public ResponseEntity<VersionDiff> compareVersions(
            @PathVariable UUID agentId,
            @RequestParam String version1,
            @RequestParam String version2) {

        VersionDiff diff = versionService.compareVersions(agentId, version1, version2);
        return ResponseEntity.ok(diff);
    }

    // ==================== Testing Sandbox Endpoints ====================

    @PostMapping("/agents/{agentId}/test/start")
    @Operation(summary = "Start a new test session")
    public ResponseEntity<AgentTestSession> startTestSession(
            @PathVariable UUID agentId,
            @RequestParam(defaultValue = "CONVERSATION") TestType testType,
            @RequestParam(required = false) String scenario,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId) {

        AgentTestSession session = testService.startTestSession(
            tenantId, agentId, testType, scenario, userId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @PostMapping("/test/sessions/{sessionId}/message")
    @Operation(summary = "Send a test message")
    public ResponseEntity<TestMessageResult> sendTestMessage(
            @PathVariable UUID sessionId,
            @RequestBody MessageRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        TestMessageResult result = testService.sendTestMessage(tenantId, sessionId, request.message());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/test/sessions/{sessionId}/complete")
    @Operation(summary = "Complete a test session with feedback")
    public ResponseEntity<AgentTestSession> completeTestSession(
            @PathVariable UUID sessionId,
            @RequestParam(required = false) String feedback,
            @RequestParam(required = false) Integer rating,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        AgentTestSession session = testService.completeTestSession(tenantId, sessionId, feedback, rating);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/test/sessions/{sessionId}/cancel")
    @Operation(summary = "Cancel a test session")
    public ResponseEntity<AgentTestSession> cancelTestSession(
            @PathVariable UUID sessionId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        AgentTestSession session = testService.cancelTestSession(tenantId, sessionId);
        return ResponseEntity.ok(session);
    }

    @GetMapping("/test/sessions/{sessionId}")
    @Operation(summary = "Get a test session")
    public ResponseEntity<AgentTestSession> getTestSession(
            @PathVariable UUID sessionId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        return testService.getTestSession(tenantId, sessionId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/agents/{agentId}/test/sessions")
    @Operation(summary = "Get test sessions for an agent")
    public ResponseEntity<Page<AgentTestSession>> getTestSessions(
            @PathVariable UUID agentId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AgentTestSession> sessions = testService.getTestSessions(agentId, pageable);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/agents/{agentId}/test/statistics")
    @Operation(summary = "Get test statistics for an agent")
    public ResponseEntity<TestStatistics> getTestStatistics(
            @PathVariable UUID agentId) {

        TestStatistics stats = testService.getTestStatistics(agentId);
        return ResponseEntity.ok(stats);
    }

    // ==================== Prompt Template Endpoints ====================

    @PostMapping("/templates")
    @Operation(summary = "Create a prompt template")
    public ResponseEntity<PromptTemplate> createTemplate(
            @RequestBody @Valid PromptTemplate template,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId) {

        template.setTenantId(tenantId);
        PromptTemplate created = templateService.create(template, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/templates/{templateId}")
    @Operation(summary = "Update a prompt template")
    public ResponseEntity<PromptTemplate> updateTemplate(
            @PathVariable UUID templateId,
            @RequestBody @Valid PromptTemplate updates,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId) {

        updates.setTenantId(tenantId);
        PromptTemplate updated = templateService.update(templateId, updates, userId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/templates/{templateId}")
    @Operation(summary = "Get a prompt template")
    public ResponseEntity<PromptTemplate> getTemplate(
            @PathVariable UUID templateId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        return templateService.getById(tenantId, templateId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/templates")
    @Operation(summary = "List prompt templates")
    public ResponseEntity<Page<PromptTemplate>> listTemplates(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<PromptTemplate> templates = templateService.list(tenantId, pageable);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/templates/category/{category}")
    @Operation(summary = "List templates by category")
    public ResponseEntity<Page<PromptTemplate>> listTemplatesByCategory(
            @PathVariable TemplateCategory category,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<PromptTemplate> templates = templateService.listByCategory(tenantId, category, pageable);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/templates/available")
    @Operation(summary = "Get all available templates (tenant + system)")
    public ResponseEntity<List<PromptTemplate>> getAvailableTemplates(
            @RequestHeader("X-Tenant-ID") String tenantId) {

        List<PromptTemplate> templates = templateService.getAvailableTemplates(tenantId);
        return ResponseEntity.ok(templates);
    }

    @DeleteMapping("/templates/{templateId}")
    @Operation(summary = "Delete a prompt template")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable UUID templateId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        templateService.delete(tenantId, templateId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/templates/{templateId}/render")
    @Operation(summary = "Render a template with variables")
    public ResponseEntity<RenderedTemplate> renderTemplate(
            @PathVariable UUID templateId,
            @RequestBody Map<String, String> variables,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        String rendered = templateService.renderTemplate(tenantId, templateId, variables);
        return ResponseEntity.ok(new RenderedTemplate(rendered));
    }

    @PostMapping("/templates/validate")
    @Operation(summary = "Validate template syntax")
    public ResponseEntity<TemplateValidationResult> validateTemplate(
            @RequestBody TemplateContent content) {

        TemplateValidationResult result = templateService.validateTemplate(content.content());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/templates/{templateId}/clone")
    @Operation(summary = "Clone a template")
    public ResponseEntity<PromptTemplate> cloneTemplate(
            @PathVariable UUID templateId,
            @RequestParam String newName,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId) {

        PromptTemplate cloned = templateService.cloneTemplate(tenantId, templateId, newName, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(cloned);
    }

    // ==================== Runtime Integration Endpoints ====================

    @GetMapping("/tools")
    @Operation(summary = "Get available tools from the agent runtime")
    public ResponseEntity<List<AgentRuntimeClient.ToolInfo>> getAvailableTools(
            @RequestHeader("X-Tenant-ID") String tenantId) {

        List<AgentRuntimeClient.ToolInfo> tools = runtimeClient.getAvailableTools(tenantId);
        return ResponseEntity.ok(tools);
    }

    @PostMapping("/agents/validate-config")
    @Operation(summary = "Validate agent configuration against runtime")
    public ResponseEntity<AgentRuntimeClient.ValidationResult> validateConfiguration(
            @RequestBody Map<String, Object> configuration,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        AgentRuntimeClient.ValidationResult result = runtimeClient.validateAgentConfiguration(configuration, tenantId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/providers")
    @Operation(summary = "Get supported LLM providers")
    public ResponseEntity<List<AgentRuntimeClient.ProviderInfo>> getSupportedProviders() {
        List<AgentRuntimeClient.ProviderInfo> providers = runtimeClient.getSupportedProviders();
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/providers/{provider}/models")
    @Operation(summary = "Get supported models for a provider")
    public ResponseEntity<List<AgentRuntimeClient.ModelInfo>> getSupportedModels(
            @PathVariable String provider) {

        List<AgentRuntimeClient.ModelInfo> models = runtimeClient.getSupportedModels(provider);
        return ResponseEntity.ok(models);
    }

    @GetMapping("/runtime/health")
    @Operation(summary = "Check agent runtime health")
    public ResponseEntity<Map<String, Object>> checkRuntimeHealth() {
        Map<String, Object> health = runtimeClient.healthCheck();
        return ResponseEntity.ok(health);
    }

    // ==================== DTOs ====================

    public record MessageRequest(String message) {}
    public record RenderedTemplate(String content) {}
    public record TemplateContent(String content) {}
}
