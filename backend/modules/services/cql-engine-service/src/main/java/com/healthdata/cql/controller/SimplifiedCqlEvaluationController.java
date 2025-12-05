package com.healthdata.cql.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.cql.entity.CqlEvaluation;
import com.healthdata.cql.entity.CqlLibrary;
import com.healthdata.cql.repository.CqlLibraryRepository;
import com.healthdata.cql.service.CqlEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
    private final ObjectMapper objectMapper;

    public SimplifiedCqlEvaluationController(
            CqlEvaluationService evaluationService,
            CqlLibraryRepository libraryRepository,
            ObjectMapper objectMapper) {
        this.evaluationService = evaluationService;
        this.libraryRepository = libraryRepository;
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
            @RequestParam("patient") String patientId,
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

            // Add placeholder measure results for quality-measure-service parsing
            // TODO: Replace with actual CQL evaluation results once engine is fully implemented
            Map<String, Object> measureResult = new HashMap<>();
            measureResult.put("measureName", library.getLibraryName());
            measureResult.put("inNumerator", false);
            measureResult.put("inDenominator", true);
            measureResult.put("complianceRate", 0.0);
            measureResult.put("note", "CQL engine evaluation placeholder - full implementation pending");

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
}
