package com.healthdata.agent.tool.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agent.client.FhirServiceClient;
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
 * Tool for querying FHIR resources.
 * Provides AI agents with access to patient data, observations, conditions, etc.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FHIRQueryTool implements Tool {

    private final FhirServiceClient fhirClient;
    private final ObjectMapper objectMapper;

    private static final ToolDefinition DEFINITION = ToolDefinition.builder()
        .name("fhir_query")
        .description("""
            Query FHIR resources from the healthcare data repository.
            Use this tool to retrieve patient information, observations, conditions,
            medications, encounters, and other clinical data.

            Supported resource types: Patient, Observation, Condition, MedicationRequest,
            Encounter, Procedure, DiagnosticReport, CarePlan, CareTeam, Goal, Immunization.
            """)
        .inputSchema(Map.of(
            "type", "object",
            "properties", Map.of(
                "resourceType", Map.of(
                    "type", "string",
                    "description", "FHIR resource type (e.g., Patient, Observation, Condition)",
                    "enum", List.of("Patient", "Observation", "Condition", "MedicationRequest",
                        "Encounter", "Procedure", "DiagnosticReport", "CarePlan", "CareTeam",
                        "Goal", "Immunization", "AllergyIntolerance", "Coverage")
                ),
                "patientId", Map.of(
                    "type", "string",
                    "description", "Patient ID to filter results (required for most resources)"
                ),
                "resourceId", Map.of(
                    "type", "string",
                    "description", "Specific resource ID to retrieve"
                ),
                "searchParams", Map.of(
                    "type", "object",
                    "description", "Additional FHIR search parameters as key-value pairs",
                    "additionalProperties", Map.of("type", "string")
                ),
                "count", Map.of(
                    "type", "integer",
                    "description", "Maximum number of results to return (default: 10, max: 100)",
                    "default", 10,
                    "maximum", 100
                )
            ),
            "required", List.of("resourceType")
        ))
        .requiredParams(List.of("resourceType"))
        .requiresApproval(false)
        .category(ToolDefinition.ToolCategory.FHIR_QUERY)
        .build();

    @Override
    public ToolDefinition getDefinition() {
        return DEFINITION;
    }

    @Override
    public Mono<ToolResult> execute(Map<String, Object> arguments, AgentContext context) {
        String resourceType = (String) arguments.get("resourceType");
        String patientId = (String) arguments.getOrDefault("patientId", context.getPatientId());
        String resourceId = (String) arguments.get("resourceId");
        @SuppressWarnings("unchecked")
        Map<String, String> searchParams = (Map<String, String>) arguments.getOrDefault("searchParams", Map.of());
        int count = ((Number) arguments.getOrDefault("count", 10)).intValue();

        log.info("FHIR query: resourceType={}, patientId={}, tenant={}",
            resourceType, patientId, context.getTenantId());

        // Validate patient access if patient-specific query
        if (patientId != null && context.isPatientContext()
            && !patientId.equals(context.getPatientId())) {
            return Mono.just(ToolResult.error(
                "Access denied: Cannot query data for patient " + patientId));
        }

        return Mono.fromCallable(() -> {
            try {
                Object result;

                if (resourceId != null) {
                    // Fetch specific resource
                    result = fhirClient.getResource(
                        context.getTenantId(),
                        resourceType,
                        resourceId
                    );
                } else {
                    // Search resources
                    Map<String, String> params = new java.util.HashMap<>(searchParams);
                    if (patientId != null && needsPatientFilter(resourceType)) {
                        params.put("patient", patientId);
                    }
                    params.put("_count", String.valueOf(Math.min(count, 100)));

                    result = fhirClient.searchResources(
                        context.getTenantId(),
                        resourceType,
                        params
                    );
                }

                String content = formatFhirResult(resourceType, result);
                return ToolResult.success(content, Map.of("data", result));

            } catch (Exception e) {
                log.error("FHIR query failed: {}", e.getMessage(), e);
                return ToolResult.error("FHIR query failed: " + e.getMessage());
            }
        });
    }

    @Override
    public ValidationResult validate(Map<String, Object> arguments) {
        List<String> errors = new java.util.ArrayList<>();

        String resourceType = (String) arguments.get("resourceType");
        if (resourceType == null || resourceType.isBlank()) {
            errors.add("resourceType is required");
        }

        Object count = arguments.get("count");
        if (count != null) {
            int countValue = ((Number) count).intValue();
            if (countValue < 1 || countValue > 100) {
                errors.add("count must be between 1 and 100");
            }
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    @Override
    public boolean isAvailable(AgentContext context) {
        // Tool requires tenant context
        return context.getTenantId() != null;
    }

    private boolean needsPatientFilter(String resourceType) {
        return !resourceType.equals("Patient") &&
            !resourceType.equals("Organization") &&
            !resourceType.equals("Practitioner") &&
            !resourceType.equals("Location");
    }

    private String formatFhirResult(String resourceType, Object result) {
        try {
            if (result == null) {
                return "No " + resourceType + " resources found.";
            }

            String json = objectMapper.writeValueAsString(result);

            // Check if it's a bundle
            if (json.contains("\"resourceType\":\"Bundle\"")) {
                Map<String, Object> bundle = objectMapper.readValue(json, Map.class);
                int total = ((Number) bundle.getOrDefault("total", 0)).intValue();
                List<?> entries = (List<?>) bundle.get("entry");
                int count = entries != null ? entries.size() : 0;

                return String.format("Found %d %s resource(s) (showing %d):\n%s",
                    total, resourceType, count, json);
            }

            return String.format("%s resource:\n%s", resourceType, json);

        } catch (Exception e) {
            return "Retrieved " + resourceType + " data (format error: " + e.getMessage() + ")";
        }
    }
}
