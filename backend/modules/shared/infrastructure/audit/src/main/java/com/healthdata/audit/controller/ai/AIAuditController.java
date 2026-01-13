package com.healthdata.audit.controller.ai;

import com.healthdata.audit.entity.ai.AIAgentDecisionEventEntity;
import com.healthdata.audit.entity.ai.ConfigurationEngineEventEntity;
import com.healthdata.audit.entity.ai.UserConfigurationActionEventEntity;
import com.healthdata.audit.repository.ai.AIAgentDecisionEventRepository;
import com.healthdata.audit.repository.ai.ConfigurationEngineEventRepository;
import com.healthdata.audit.repository.ai.UserConfigurationActionEventRepository;
import com.healthdata.audit.service.ai.AIAuditEventStore;
import com.healthdata.audit.service.ai.AIAuditEventStore.AuditTrail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for querying AI audit events.
 * 
 * Provides:
 * - AI decision event queries
 * - Configuration change history
 * - User action audit trail
 * - Complete correlation-based audit trails
 * - Real-time analytics and dashboards
 * 
 * Security: Requires ADMIN or AUDITOR role
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/audit/ai")
@Tag(name = "AI Audit", description = "AI audit event queries and analytics")
@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'QUALITY_OFFICER', 'QA_ANALYST', 'MPI_ADMIN', 'CLINICAL_PHYSICIAN')")
public class AIAuditController {

    private final AIAgentDecisionEventRepository aiDecisionRepository;
    private final ConfigurationEngineEventRepository configChangeRepository;
    private final UserConfigurationActionEventRepository userActionRepository;
    private final AIAuditEventStore auditEventStore;

    @Autowired
    public AIAuditController(
            AIAgentDecisionEventRepository aiDecisionRepository,
            ConfigurationEngineEventRepository configChangeRepository,
            UserConfigurationActionEventRepository userActionRepository,
            AIAuditEventStore auditEventStore) {
        this.aiDecisionRepository = aiDecisionRepository;
        this.configChangeRepository = configChangeRepository;
        this.userActionRepository = userActionRepository;
        this.auditEventStore = auditEventStore;
    }

    // ==================== AI Decision Queries ====================

    @GetMapping("/decisions")
    @Operation(summary = "Get AI agent decisions", description = "Query AI agent decision events with filters")
    public ResponseEntity<Page<AIAgentDecisionEventEntity>> getAIDecisions(
            @Parameter(description = "Tenant ID") @RequestParam(required = false) String tenantId,
            @Parameter(description = "Start time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @Parameter(description = "End time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").descending());
        
        Page<AIAgentDecisionEventEntity> decisions = tenantId != null
            ? aiDecisionRepository.findByTenantIdAndTimestampBetween(tenantId, startTime, endTime, pageRequest)
            : aiDecisionRepository.findAll(pageRequest);

        return ResponseEntity.ok(decisions);
    }

    @GetMapping("/decisions/{eventId}")
    @Operation(summary = "Get AI decision by ID")
    public ResponseEntity<AIAgentDecisionEventEntity> getAIDecisionById(
            @PathVariable UUID eventId) {
        return aiDecisionRepository.findById(eventId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/decisions/chain/{correlationId}")
    @Operation(summary = "Get AI decision chain", description = "Get all related AI decisions by correlation ID")
    public ResponseEntity<List<AIAgentDecisionEventEntity>> getAIDecisionChain(
            @PathVariable String correlationId) {
        List<AIAgentDecisionEventEntity> chain = auditEventStore.getDecisionChain(correlationId);
        return ResponseEntity.ok(chain);
    }

    @GetMapping("/decisions/analytics/confidence")
    @Operation(summary = "Get average confidence by agent type")
    public ResponseEntity<Map<String, Double>> getAverageConfidenceByAgentType(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        
        List<Object[]> results = aiDecisionRepository.calculateAverageConfidenceByAgentType(startTime, endTime);
        Map<String, Double> confidence = new HashMap<>();
        results.forEach(row -> confidence.put(row[0].toString(), (Double) row[1]));
        
        return ResponseEntity.ok(confidence);
    }

    @GetMapping("/decisions/analytics/outcomes")
    @Operation(summary = "Count decisions by outcome")
    public ResponseEntity<Map<String, Long>> countDecisionsByOutcome(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        
        List<Object[]> results = aiDecisionRepository.countDecisionsByOutcome(startTime, endTime);
        Map<String, Long> outcomes = new HashMap<>();
        results.forEach(row -> outcomes.put(row[0].toString(), (Long) row[1]));
        
        return ResponseEntity.ok(outcomes);
    }

    // ==================== Configuration Change Queries ====================

    @GetMapping("/config-changes")
    @Operation(summary = "Get configuration changes", description = "Query configuration change events with filters")
    public ResponseEntity<Page<ConfigurationEngineEventEntity>> getConfigChanges(
            @Parameter(description = "Service name") @RequestParam(required = false) String serviceName,
            @Parameter(description = "Start time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @Parameter(description = "End time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").descending());
        
        Page<ConfigurationEngineEventEntity> changes = serviceName != null
            ? configChangeRepository.findByServiceNameAndTimestampBetween(serviceName, startTime, endTime, pageRequest)
            : configChangeRepository.findAll(pageRequest);

        return ResponseEntity.ok(changes);
    }

    @GetMapping("/config-changes/{eventId}")
    @Operation(summary = "Get configuration change by ID")
    public ResponseEntity<ConfigurationEngineEventEntity> getConfigChangeById(
            @PathVariable UUID eventId) {
        return configChangeRepository.findById(eventId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/config-changes/history/{configKey}")
    @Operation(summary = "Get configuration history", description = "Get change history for a specific config key")
    public ResponseEntity<List<ConfigurationEngineEventEntity>> getConfigHistory(
            @PathVariable String configKey,
            @RequestParam(defaultValue = "30") int daysBack) {
        
        List<ConfigurationEngineEventEntity> history = 
            auditEventStore.getConfigurationHistory(configKey, Duration.ofDays(daysBack));
        
        return ResponseEntity.ok(history);
    }

    @GetMapping("/config-changes/high-risk")
    @Operation(summary = "Get high-risk changes", description = "Get production global-scope changes")
    public ResponseEntity<List<ConfigurationEngineEventEntity>> getHighRiskChanges(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        
        List<ConfigurationEngineEventEntity> highRisk = 
            configChangeRepository.findHighRiskChanges(startTime, endTime);
        
        return ResponseEntity.ok(highRisk);
    }

    @GetMapping("/config-changes/rollbacks")
    @Operation(summary = "Get rolled back changes")
    public ResponseEntity<List<ConfigurationEngineEventEntity>> getRolledBackChanges(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        
        List<ConfigurationEngineEventEntity> rollbacks = 
            configChangeRepository.findRolledBackChanges(startTime, endTime);
        
        return ResponseEntity.ok(rollbacks);
    }

    // ==================== User Action Queries ====================

    @GetMapping("/user-actions")
    @Operation(summary = "Get user actions", description = "Query user configuration action events")
    public ResponseEntity<Page<UserConfigurationActionEventEntity>> getUserActions(
            @Parameter(description = "User ID") @RequestParam(required = false) String userId,
            @Parameter(description = "Start time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @Parameter(description = "End time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").descending());
        
        Page<UserConfigurationActionEventEntity> actions = userId != null
            ? userActionRepository.findByUserIdAndTimestampBetween(userId, startTime, endTime, pageRequest)
            : userActionRepository.findAll(pageRequest);

        return ResponseEntity.ok(actions);
    }

    @GetMapping("/user-actions/{eventId}")
    @Operation(summary = "Get user action by ID")
    public ResponseEntity<UserConfigurationActionEventEntity> getUserActionById(
            @PathVariable UUID eventId) {
        return userActionRepository.findById(eventId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user-actions/pending-approvals")
    @Operation(summary = "Get pending approval requests")
    public ResponseEntity<List<UserConfigurationActionEventEntity>> getPendingApprovals() {
        List<UserConfigurationActionEventEntity> pending = auditEventStore.getPendingApprovals();
        return ResponseEntity.ok(pending);
    }

    @GetMapping("/user-actions/ai-feedback")
    @Operation(summary = "Get user feedback on AI recommendations")
    public ResponseEntity<Map<String, Object>> getAIRecommendationFeedback(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        
        List<UserConfigurationActionEventEntity> accepted = 
            userActionRepository.findAcceptedAIRecommendations(startTime, endTime);
        
        List<UserConfigurationActionEventEntity> rejected = 
            userActionRepository.findRejectedAIRecommendations(startTime, endTime);
        
        Double avgRating = 
            userActionRepository.calculateAverageAIRecommendationRating(startTime, endTime);
        
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("acceptedCount", accepted.size());
        feedback.put("rejectedCount", rejected.size());
        feedback.put("acceptanceRate", accepted.size() / (double) (accepted.size() + rejected.size()));
        feedback.put("averageRating", avgRating);
        
        return ResponseEntity.ok(feedback);
    }

    // ==================== Complete Audit Trail ====================

    @GetMapping("/trail/{correlationId}")
    @Operation(summary = "Get complete audit trail", 
               description = "Get all related events (AI decisions, user actions, config changes) by correlation ID")
    public ResponseEntity<AuditTrail> getCompleteAuditTrail(
            @PathVariable String correlationId) {
        
        AuditTrail trail = auditEventStore.getCompleteAuditTrail(correlationId);
        return ResponseEntity.ok(trail);
    }
}
