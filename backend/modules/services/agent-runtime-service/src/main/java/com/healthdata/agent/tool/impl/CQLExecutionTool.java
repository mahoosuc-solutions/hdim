package com.healthdata.agent.tool.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agent.client.CqlEngineClient;
import com.healthdata.agent.core.AgentContext;
import com.healthdata.agent.tool.Tool;
import com.healthdata.agent.tool.ToolDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Tool for executing CQL (Clinical Quality Language) measures.
 * Provides AI agents with quality measure evaluation capabilities.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CQLExecutionTool implements Tool {

    private final CqlEngineClient cqlClient;
    private final ObjectMapper objectMapper;

    private static final ToolDefinition DEFINITION = ToolDefinition.builder()
        .name("cql_execute")
        .description("""
            Execute CQL (Clinical Quality Language) measures and quality calculations.
            Use this tool to evaluate HEDIS measures, Star Ratings metrics, care gap criteria,
            and custom clinical logic against patient data.

            Can evaluate specific measures by ID or run ad-hoc CQL expressions.
            Results include measure populations, scores, and detailed breakdowns.
            """)
        .inputSchema(Map.of(
            "type", "object",
            "properties", Map.of(
                "measureId", Map.of(
                    "type", "string",
                    "description", "Quality measure identifier (e.g., CMS125v11, HEDIS-BCS)"
                ),
                "patientId", Map.of(
                    "type", "string",
                    "description", "Patient ID to evaluate measure for"
                ),
                "measurementPeriod", Map.of(
                    "type", "object",
                    "description", "Measurement period for evaluation",
                    "properties", Map.of(
                        "start", Map.of("type", "string", "format", "date"),
                        "end", Map.of("type", "string", "format", "date")
                    )
                ),
                "expression", Map.of(
                    "type", "string",
                    "description", "Ad-hoc CQL expression to evaluate (if measureId not provided)"
                ),
                "parameters", Map.of(
                    "type", "object",
                    "description", "Additional parameters for measure evaluation"
                ),
                "includePopulationDetails", Map.of(
                    "type", "boolean",
                    "description", "Include detailed population membership information",
                    "default", true
                )
            ),
            "required", List.of()
        ))
        .requiredParams(List.of())
        .requiresApproval(false)
        .category(ToolDefinition.ToolCategory.CQL_EXECUTION)
        .build();

    @Override
    public ToolDefinition getDefinition() {
        return DEFINITION;
    }

    @Override
    public Mono<ToolResult> execute(Map<String, Object> arguments, AgentContext context) {
        String measureId = (String) arguments.get("measureId");
        String patientId = (String) arguments.getOrDefault("patientId", context.getPatientId());
        String expression = (String) arguments.get("expression");
        @SuppressWarnings("unchecked")
        Map<String, Object> measurementPeriod = (Map<String, Object>) arguments.get("measurementPeriod");
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) arguments.getOrDefault("parameters", Map.of());
        boolean includeDetails = (Boolean) arguments.getOrDefault("includePopulationDetails", true);

        log.info("CQL execution: measureId={}, patientId={}, tenant={}",
            measureId, patientId, context.getTenantId());

        // At least measureId or expression must be provided
        if (measureId == null && expression == null) {
            return Mono.just(ToolResult.error("Either measureId or expression must be provided"));
        }

        // Validate patient access
        if (patientId != null && context.isPatientContext()
            && !patientId.equals(context.getPatientId())) {
            return Mono.just(ToolResult.error(
                "Access denied: Cannot evaluate measures for patient " + patientId));
        }

        return Mono.fromCallable(() -> {
            try {
                Object result;

                if (measureId != null) {
                    // Evaluate a specific measure
                    result = cqlClient.evaluateMeasure(
                        context.getTenantId(),
                        measureId,
                        patientId,
                        measurementPeriod,
                        parameters,
                        includeDetails
                    );
                } else {
                    // Execute ad-hoc CQL expression
                    result = cqlClient.executeExpression(
                        context.getTenantId(),
                        expression,
                        patientId,
                        parameters
                    );
                }

                String content = formatCqlResult(measureId, result);
                return ToolResult.success(content, Map.of("result", result));

            } catch (Exception e) {
                log.error("CQL execution failed: {}", e.getMessage(), e);
                return ToolResult.error("CQL execution failed: " + e.getMessage());
            }
        });
    }

    @Override
    public ValidationResult validate(Map<String, Object> arguments) {
        String measureId = (String) arguments.get("measureId");
        String expression = (String) arguments.get("expression");

        if (measureId == null && expression == null) {
            return ValidationResult.invalid("Either measureId or expression must be provided");
        }

        return ValidationResult.valid();
    }

    @Override
    public boolean isAvailable(AgentContext context) {
        return context.getTenantId() != null;
    }

    private String formatCqlResult(String measureId, Object result) {
        try {
            if (result == null) {
                return "No results from CQL evaluation.";
            }

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);

            if (measureId != null) {
                return String.format("Measure %s evaluation results:\n%s", measureId, json);
            } else {
                return String.format("CQL expression results:\n%s", json);
            }

        } catch (Exception e) {
            return "CQL evaluation completed (format error: " + e.getMessage() + ")";
        }
    }
}
