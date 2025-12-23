package com.healthdata.cql.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.cql.entity.CqlEvaluation;
import com.healthdata.cql.entity.CqlLibrary;
import com.healthdata.cql.registry.HedisMeasureRegistry;
import com.healthdata.cql.repository.CqlLibraryRepository;
import com.healthdata.cql.service.CqlEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Simplified CQL Evaluation Controller
 *
 * Provides a simplified /evaluate endpoint that matches the Feign client contract
 * used by quality-measure-service. This adapter endpoint delegates to the existing
 * CqlEvaluationService while providing a simpler interface.
 *
 * This controller was created to bridge the API contract mismatch between:
 * - quality-measure-service expecting: POST /evaluate
 * - cql-engine-service providing: POST /api/v1/cql/evaluations
 */
@RestController
@RequestMapping("/evaluate")
public class SimplifiedCqlEvaluationController {

    private static final Logger logger = LoggerFactory.getLogger(SimplifiedCqlEvaluationController.class);

    private final CqlEvaluationService evaluationService;
    private final CqlLibraryRepository libraryRepository;
    private final HedisMeasureRegistry measureRegistry;
    private final ObjectMapper objectMapper;

    public SimplifiedCqlEvaluationController(
            CqlEvaluationService evaluationService,
            CqlLibraryRepository libraryRepository,
            HedisMeasureRegistry measureRegistry,
            ObjectMapper objectMapper) {
        this.evaluationService = evaluationService;
        this.libraryRepository = libraryRepository;
        this.measureRegistry = measureRegistry;
        this.objectMapper = objectMapper;
    }

    /**
     * Simplified CQL evaluation endpoint
     *
     * Matches the Feign client interface:
     * @param tenantId    - X-Tenant-ID header
     * @param libraryName - CQL library name (e.g., "CDC")
     * @param patientId   - Patient identifier
     * @param parameters  - Optional JSON parameters (currently unused)
     * @return JSON string with evaluation result
     *
     * Example call:
     * POST /evaluate?library=CDC&patient=test-patient-001
     * X-Tenant-ID: test-tenant
     */
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> evaluateCql(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("library") String libraryName,
            @RequestParam("patient") UUID patientId,
            @RequestBody(required = false) String parameters) {

        logger.info("Simplified evaluation request - tenant: {}, library: {}, patient: {}",
                tenantId, libraryName, patientId);

        try {
            // 1. Look up library by name (use latest version)
            Optional<CqlLibrary> libraryOpt = libraryRepository.findLatestVersionByName(tenantId, libraryName);

            if (libraryOpt.isEmpty()) {
                logger.error("Library not found: {} for tenant: {}", libraryName, tenantId);
                return ResponseEntity.status(404).body(createErrorResponse(
                        "Library not found: " + libraryName));
            }

            CqlLibrary library = libraryOpt.get();
            logger.info("Found library: {} v{} (ID: {})",
                    library.getLibraryName(), library.getVersion(), library.getId());

            // 2. Create evaluation record
            CqlEvaluation evaluation = evaluationService.createEvaluation(
                    tenantId, library.getId(), patientId);

            logger.info("Created evaluation with ID: {}", evaluation.getId());

            // 3. Execute evaluation
            CqlEvaluation executedEvaluation = evaluationService.executeEvaluation(
                    evaluation.getId(), tenantId);

            logger.info("Executed evaluation - status: {}, duration: {}ms",
                    executedEvaluation.getStatus(), executedEvaluation.getDurationMs());

            // 4. Build response
            String response = buildEvaluationResponse(executedEvaluation, library);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            logger.error("Evaluation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(createErrorResponse(
                    "Evaluation failed: " + e.getMessage()));
        }
    }

    /**
     * Get all available HEDIS measures
     *
     * Returns a list of all registered Java measure implementations.
     * This endpoint can be used by the frontend to populate measure dropdowns.
     *
     * Query params:
     * - evaluableOnly: if true, only return measures that have CQL libraries (default: false)
     *
     * @return JSON array of available measures with hasCqlLibrary flag
     */
    @GetMapping(value = "/measures", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAvailableMeasures(
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId,
            @RequestParam(value = "evaluableOnly", defaultValue = "false") boolean evaluableOnly) {
        try {
            logger.info("Fetching available measures from registry (tenant: {}, evaluableOnly: {})",
                    tenantId, evaluableOnly);

            List<HedisMeasureRegistry.MeasureInfo> measures = measureRegistry.getMeasureInfoList();

            // Get all CQL library names for this tenant
            List<String> availableLibraries = libraryRepository.findByTenantIdAndStatusAndActiveTrue(tenantId, "ACTIVE")
                    .stream()
                    .map(CqlLibrary::getLibraryName)
                    .toList();

            logger.info("Found {} active CQL libraries for tenant {}: {}",
                    availableLibraries.size(), tenantId, availableLibraries);

            // Enrich measures with hasCqlLibrary flag
            // Keep original property names expected by frontend: measureId, measureName, version, implementationClass
            List<Map<String, Object>> enrichedMeasures = measures.stream()
                    .map(m -> {
                        Map<String, Object> measureMap = new HashMap<>();
                        measureMap.put("measureId", m.measureId());
                        measureMap.put("measureName", m.measureName());
                        measureMap.put("version", m.version());
                        measureMap.put("implementationClass", m.implementationClass());
                        measureMap.put("hasCqlLibrary", availableLibraries.contains(m.measureId()));
                        return measureMap;
                    })
                    .filter(m -> !evaluableOnly || (boolean) m.get("hasCqlLibrary"))
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("count", enrichedMeasures.size());
            response.put("measures", enrichedMeasures);
            response.put("totalMeasures", measures.size());
            response.put("evaluableMeasures", availableLibraries.size());

            logger.info("Returning {} measures ({} evaluable)", enrichedMeasures.size(), availableLibraries.size());
            return ResponseEntity.ok(objectMapper.writeValueAsString(response));

        } catch (Exception e) {
            logger.error("Error fetching available measures: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(createErrorResponse(
                    "Failed to fetch available measures: " + e.getMessage()));
        }
    }

    /**
     * Get measure details by ID
     *
     * @param measureId The HEDIS measure ID (e.g., "CDC", "CBP")
     * @return Measure details or 404 if not found
     */
    @GetMapping(value = "/measures/{measureId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getMeasureDetails(@PathVariable String measureId) {
        try {
            logger.info("Fetching measure details for: {}", measureId);

            return measureRegistry.getMeasure(measureId)
                    .map(measure -> {
                        try {
                            Map<String, Object> response = new HashMap<>();
                            response.put("measureId", measure.getMeasureId());
                            response.put("measureName", measure.getMeasureName());
                            response.put("version", measure.getVersion());
                            response.put("implementationClass", measure.getClass().getName());

                            return ResponseEntity.ok(objectMapper.writeValueAsString(response));
                        } catch (Exception e) {
                            return ResponseEntity.status(500).body(createErrorResponse(e.getMessage()));
                        }
                    })
                    .orElseGet(() -> ResponseEntity.status(404).body(createErrorResponse(
                            "Measure not found: " + measureId)));

        } catch (Exception e) {
            logger.error("Error fetching measure details: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(createErrorResponse(
                    "Failed to fetch measure details: " + e.getMessage()));
        }
    }

    /**
     * Check if measure is registered
     *
     * @param measureId The HEDIS measure ID
     * @return Boolean indicating if measure exists
     */
    @GetMapping(value = "/measures/{measureId}/exists", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> measureExists(@PathVariable String measureId) {
        try {
            boolean exists = measureRegistry.hasMeasure(measureId);

            Map<String, Object> response = new HashMap<>();
            response.put("measureId", measureId);
            response.put("exists", exists);

            return ResponseEntity.ok(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            logger.error("Error checking measure existence: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Build JSON response from evaluation result
     */
    private String buildEvaluationResponse(CqlEvaluation evaluation, CqlLibrary library) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("evaluationId", evaluation.getId().toString());
            response.put("status", evaluation.getStatus());
            response.put("libraryName", library.getLibraryName());
            response.put("libraryVersion", library.getVersion());
            response.put("patientId", evaluation.getPatientId());
            response.put("evaluationDate", evaluation.getEvaluationDate().toString());
            response.put("durationMs", evaluation.getDurationMs());

            // Include result if available
            if (evaluation.getEvaluationResult() != null) {
                response.put("result", evaluation.getEvaluationResult());
            }

            // Include error if failed
            if ("FAILED".equals(evaluation.getStatus()) && evaluation.getErrorMessage() != null) {
                response.put("error", evaluation.getErrorMessage());
            }

            // Parse actual measure results from evaluation or use defaults
            Map<String, Object> measureResult = new HashMap<>();
            measureResult.put("measureName", library.getLibraryName());

            if (evaluation.getEvaluationResult() != null && !evaluation.getEvaluationResult().isEmpty()) {
                try {
                    JsonNode resultNode = objectMapper.readTree(evaluation.getEvaluationResult());

                    // Extract actual values from the evaluation result
                    measureResult.put("inNumerator", resultNode.has("inNumerator") ?
                            resultNode.get("inNumerator").asBoolean() : false);
                    measureResult.put("inDenominator", resultNode.has("inDenominator") ?
                            resultNode.get("inDenominator").asBoolean() : true);
                    measureResult.put("complianceRate", resultNode.has("complianceRate") ?
                            resultNode.get("complianceRate").asDouble() : 0.0);

                    if (resultNode.has("exclusionReason") && !resultNode.get("exclusionReason").isNull()) {
                        measureResult.put("exclusionReason", resultNode.get("exclusionReason").asText());
                    }
                    if (resultNode.has("careGaps") && resultNode.get("careGaps").isArray()) {
                        measureResult.put("careGaps", objectMapper.convertValue(
                                resultNode.get("careGaps"), java.util.List.class));
                    }
                } catch (Exception e) {
                    logger.warn("Could not parse evaluation result, using defaults: {}", e.getMessage());
                    measureResult.put("inNumerator", false);
                    measureResult.put("inDenominator", true);
                    measureResult.put("complianceRate", 0.0);
                }
            } else {
                // No evaluation result available, use defaults
                measureResult.put("inNumerator", false);
                measureResult.put("inDenominator", true);
                measureResult.put("complianceRate", 0.0);
            }

            response.put("measureResult", measureResult);

            return objectMapper.writeValueAsString(response);

        } catch (Exception e) {
            logger.error("Error building response: {}", e.getMessage(), e);
            return "{\"error\": \"Failed to build response\"}";
        }
    }

    /**
     * Create error response JSON
     */
    private String createErrorResponse(String message) {
        try {
            Map<String, String> error = new HashMap<>();
            error.put("error", message);
            return objectMapper.writeValueAsString(error);
        } catch (Exception e) {
            return "{\"error\": \"" + message.replace("\"", "\\\"") + "\"}";
        }
    }

    /**
     * Derive category from implementation class name
     * Maps measure implementation classes to HEDIS categories
     */
    private String deriveCategoryFromImplementation(String implementationClass) {
        if (implementationClass == null) return "OTHER";

        // Map based on measure type patterns in class name
        String className = implementationClass.toLowerCase();

        if (className.contains("diabetes") || className.contains("cdc") ||
            className.contains("hba1c") || className.contains("bloodsugar")) {
            return "CHRONIC_CONDITION";
        }
        if (className.contains("cancer") || className.contains("screening") ||
            className.contains("bcs") || className.contains("ccs") || className.contains("col")) {
            return "PREVENTIVE";
        }
        if (className.contains("blood") && className.contains("pressure") ||
            className.contains("cbp") || className.contains("hypertension")) {
            return "CHRONIC_CONDITION";
        }
        if (className.contains("medication") || className.contains("amm") ||
            className.contains("antidepressant") || className.contains("adherence")) {
            return "MEDICATION";
        }
        if (className.contains("mental") || className.contains("depression") ||
            className.contains("behavioral")) {
            return "BEHAVIORAL_HEALTH";
        }
        if (className.contains("immuniz") || className.contains("vaccin")) {
            return "IMMUNIZATION";
        }
        if (className.contains("access") || className.contains("utilization")) {
            return "ACCESS";
        }

        return "OTHER";
    }
}
