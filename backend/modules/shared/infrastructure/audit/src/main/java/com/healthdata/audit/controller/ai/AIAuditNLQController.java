package com.healthdata.audit.controller.ai;

import com.healthdata.audit.entity.ai.AIAgentDecisionEventEntity;
import com.healthdata.audit.entity.ai.ConfigurationEngineEventEntity;
import com.healthdata.audit.entity.ai.UserConfigurationActionEventEntity;
import com.healthdata.audit.repository.ai.AIAgentDecisionEventRepository;
import com.healthdata.audit.repository.ai.ConfigurationEngineEventRepository;
import com.healthdata.audit.repository.ai.UserConfigurationActionEventRepository;
import com.healthdata.audit.service.ai.AIAuditEventStore;
import com.healthdata.audit.service.ai.NaturalLanguageAuditQuery;
import com.healthdata.audit.service.ai.NaturalLanguageAuditQuery.ParsedQuery;
import com.healthdata.audit.service.ai.NaturalLanguageAuditQuery.QueryIntent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Natural language query interface for AI audit events.
 * 
 * Allows users to query audit events using plain English questions.
 * 
 * Examples:
 * - "Show me all AI decisions from the last 24 hours"
 * - "What configuration changes were made yesterday?"
 * - "Who rejected AI recommendations this week?"
 * - "Show me the audit trail for correlation ID abc-123"
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/audit/ai/nlq")
@Tag(name = "AI Audit NLQ", description = "Natural language query interface for AI audit events")
@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'OPERATOR', 'QUALITY_OFFICER', 'QA_ANALYST', 'MPI_ADMIN', 'CLINICAL_PHYSICIAN', 'CLINICAL_NURSE')")
public class AIAuditNLQController {

    private final NaturalLanguageAuditQuery nlqService;
    private final AIAgentDecisionEventRepository aiDecisionRepository;
    private final ConfigurationEngineEventRepository configChangeRepository;
    private final UserConfigurationActionEventRepository userActionRepository;
    private final AIAuditEventStore auditEventStore;

    @Autowired
    public AIAuditNLQController(
            NaturalLanguageAuditQuery nlqService,
            AIAgentDecisionEventRepository aiDecisionRepository,
            ConfigurationEngineEventRepository configChangeRepository,
            UserConfigurationActionEventRepository userActionRepository,
            AIAuditEventStore auditEventStore) {
        this.nlqService = nlqService;
        this.aiDecisionRepository = aiDecisionRepository;
        this.configChangeRepository = configChangeRepository;
        this.userActionRepository = userActionRepository;
        this.auditEventStore = auditEventStore;
    }

    @PostMapping("/query")
    @Operation(summary = "Query audit events using natural language",
               description = "Ask questions in plain English about AI audit events")
    public ResponseEntity<NLQResponse> queryAuditEvents(
            @Parameter(description = "Natural language query", 
                      example = "Show me all AI decisions from the last 24 hours")
            @RequestBody NLQRequest request) {

        log.info("Processing natural language query: {}", request.getQuery());

        // Parse the natural language query
        ParsedQuery parsed = nlqService.parseQuery(request.getQuery());

        // Execute the appropriate query based on intent
        NLQResponse response = new NLQResponse();
        response.setOriginalQuery(request.getQuery());
        response.setParsedIntent(parsed.getIntent().toString());
        response.setTimeRange(String.format("%s to %s", parsed.getStartTime(), parsed.getEndTime()));
        response.setEntities(parsed.getEntities());

        try {
            Object results = executeQuery(parsed);
            response.setResults(results);
            response.setSuccess(true);
            
            if (results instanceof List<?> list) {
                response.setResultCount(list.size());
            } else if (results instanceof Map<?, ?> map) {
                response.setResultCount(map.size());
            } else {
                response.setResultCount(1);
            }

        } catch (Exception e) {
            log.error("Error executing natural language query: {}", request.getQuery(), e);
            response.setSuccess(false);
            response.setError(e.getMessage());
            response.setResultCount(0);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Execute the appropriate query based on parsed intent.
     */
    private Object executeQuery(ParsedQuery parsed) {
        PageRequest pageRequest = PageRequest.of(0, 100, Sort.by("timestamp").descending());

        return switch (parsed.getIntent()) {
            case LIST_AI_DECISIONS -> {
                String tenantId = parsed.getEntities().get("tenantId");
                if (tenantId != null) {
                    yield aiDecisionRepository.findByTenantIdAndTimestampBetween(
                        tenantId, parsed.getStartTime(), parsed.getEndTime(), pageRequest
                    ).getContent();
                } else {
                    yield aiDecisionRepository.findAll(pageRequest).getContent();
                }
            }

            case GET_DECISION_CHAIN -> {
                String correlationId = parsed.getEntities().get("correlationId");
                if (correlationId != null) {
                    yield auditEventStore.getDecisionChain(correlationId);
                } else {
                    throw new IllegalArgumentException("Correlation ID required for decision chain query");
                }
            }

            case LIST_CONFIG_CHANGES -> {
                String serviceName = parsed.getEntities().get("serviceName");
                if (serviceName != null) {
                    yield configChangeRepository.findByServiceNameAndTimestampBetween(
                        serviceName, parsed.getStartTime(), parsed.getEndTime(), pageRequest
                    ).getContent();
                } else {
                    yield configChangeRepository.findAll(pageRequest).getContent();
                }
            }

            case GET_CONFIG_HISTORY -> {
                String configKey = parsed.getEntities().get("configKey");
                if (configKey != null) {
                    yield configChangeRepository.findConfigurationHistory(
                        configKey, parsed.getStartTime(), parsed.getEndTime()
                    );
                } else {
                    throw new IllegalArgumentException("Configuration key required for history query");
                }
            }

            case LIST_HIGH_RISK_CHANGES -> 
                configChangeRepository.findHighRiskChanges(parsed.getStartTime(), parsed.getEndTime());

            case LIST_USER_ACTIONS -> {
                String userId = parsed.getEntities().get("userId");
                if (userId != null) {
                    yield userActionRepository.findByUserIdAndTimestampBetween(
                        userId, parsed.getStartTime(), parsed.getEndTime(), pageRequest
                    ).getContent();
                } else {
                    yield userActionRepository.findAll(pageRequest).getContent();
                }
            }

            case LIST_AI_FEEDBACK -> {
                List<UserConfigurationActionEventEntity> accepted = 
                    userActionRepository.findAcceptedAIRecommendations(
                        parsed.getStartTime(), parsed.getEndTime()
                    );
                List<UserConfigurationActionEventEntity> rejected = 
                    userActionRepository.findRejectedAIRecommendations(
                        parsed.getStartTime(), parsed.getEndTime()
                    );
                
                Map<String, Object> feedback = new HashMap<>();
                feedback.put("accepted", accepted);
                feedback.put("rejected", rejected);
                feedback.put("acceptanceRate", 
                    accepted.size() / (double) (accepted.size() + rejected.size()));
                yield feedback;
            }

            case LIST_PENDING_APPROVALS -> 
                auditEventStore.getPendingApprovals();

            case ANALYTICS_CONFIDENCE -> {
                List<Object[]> results = aiDecisionRepository.calculateAverageConfidenceByAgentType(
                    parsed.getStartTime(), parsed.getEndTime()
                );
                Map<String, Double> confidence = new HashMap<>();
                results.forEach(row -> confidence.put(row[0].toString(), (Double) row[1]));
                yield confidence;
            }

            case ANALYTICS_OUTCOMES -> {
                List<Object[]> results = aiDecisionRepository.countDecisionsByOutcome(
                    parsed.getStartTime(), parsed.getEndTime()
                );
                Map<String, Long> outcomes = new HashMap<>();
                results.forEach(row -> outcomes.put(row[0].toString(), (Long) row[1]));
                yield outcomes;
            }

            case GET_AUDIT_TRAIL -> {
                String correlationId = parsed.getEntities().get("correlationId");
                if (correlationId != null) {
                    yield auditEventStore.getCompleteAuditTrail(correlationId);
                } else {
                    throw new IllegalArgumentException("Correlation ID required for audit trail query");
                }
            }

            default -> 
                throw new IllegalArgumentException("Unknown or unsupported query intent: " + parsed.getIntent());
        };
    }

    @GetMapping("/examples")
    @Operation(summary = "Get example natural language queries")
    public ResponseEntity<List<String>> getExampleQueries() {
        List<String> examples = Arrays.asList(
            "Show me all AI decisions from the last 24 hours",
            "What configuration changes were made yesterday?",
            "Show me configuration changes to pool size this week",
            "Who rejected AI recommendations today?",
            "Show me the audit trail for correlation-id abc-123",
            "What was the average confidence score for AI decisions this month?",
            "Show me high-risk configuration changes from last week",
            "Get pending approvals",
            "Show me user actions by john.doe",
            "What is the history for spring.datasource.hikari.maximum-pool-size?",
            "Show me AI decision outcomes from the last 7 days",
            "What configuration changes were rolled back this week?"
        );
        return ResponseEntity.ok(examples);
    }

    /**
     * Natural language query request.
     */
    @Data
    public static class NLQRequest {
        private String query;
    }

    /**
     * Natural language query response.
     */
    @Data
    public static class NLQResponse {
        private String originalQuery;
        private String parsedIntent;
        private String timeRange;
        private Map<String, String> entities;
        private boolean success;
        private Object results;
        private int resultCount;
        private String error;
    }
}
