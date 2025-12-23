package com.healthdata.quality.rest;

import com.healthdata.quality.dto.RiskAssessmentDTO;
import com.healthdata.quality.persistence.RiskAssessmentEntity;
import com.healthdata.quality.persistence.RiskAssessmentRepository;
import com.healthdata.quality.service.RiskStratificationService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Risk Assessment Controller - REST API for risk stratification
 *
 * Provides endpoints for:
 * - Getting current risk assessment for a patient
 * - Viewing risk history
 * - Getting category-specific risk assessments
 * - Triggering risk recalculation
 * - Viewing population-level risk statistics
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Validated
public class RiskAssessmentController {

    private final RiskStratificationService riskStratificationService;
    private final RiskAssessmentRepository riskAssessmentRepository;

    /**
     * GET /api/patients/{patientId}/risk-assessment
     * Get the most recent risk assessment for a patient
     */
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/patients/{patientId}/risk-assessment", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RiskAssessmentDTO> getCurrentRiskAssessment(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable("patientId") UUID patientId
    ) {
        log.info("GET /api/patients/{}/risk-assessment - tenant: {}", patientId, tenantId);

        RiskAssessmentDTO assessment = riskStratificationService.getRiskAssessment(tenantId, patientId);

        if (assessment == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(assessment);
    }

    /**
     * GET /api/patients/{patientId}/risk-history
     * Get all risk assessments for a patient (ordered by date, most recent first)
     */
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/patients/{patientId}/risk-history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RiskAssessmentDTO>> getRiskHistory(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable("patientId") UUID patientId
    ) {
        log.info("GET /api/patients/{}/risk-history - tenant: {}", patientId, tenantId);

        List<RiskAssessmentEntity> history = riskAssessmentRepository
            .findByTenantIdAndPatientIdOrderByAssessmentDateDesc(tenantId, patientId);

        List<RiskAssessmentDTO> dtos = history.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/patients/{patientId}/risk-by-category/{category}
     * Get the most recent risk assessment for a specific category (e.g., CARDIOVASCULAR, DIABETES)
     */
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/patients/{patientId}/risk-by-category/{category}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RiskAssessmentDTO> getRiskByCategory(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable("patientId") UUID patientId,
            @PathVariable("category") String category
    ) {
        log.info("GET /api/patients/{}/risk-by-category/{} - tenant: {}", patientId, category, tenantId);

        Optional<RiskAssessmentEntity> assessment = riskAssessmentRepository
            .findLatestByCategoryAndPatient(tenantId, patientId, category);

        return assessment
            .map(this::mapToDTO)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * POST /api/patients/{patientId}/recalculate-risk
     * Trigger a risk recalculation for a patient
     */
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/patients/{patientId}/recalculate-risk", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RiskAssessmentDTO> recalculateRisk(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable("patientId") UUID patientId
    ) {
        log.info("POST /api/patients/{}/recalculate-risk - tenant: {}", patientId, tenantId);

        RiskAssessmentDTO assessment = riskStratificationService.calculateRiskAssessment(tenantId, patientId);

        return ResponseEntity.ok(assessment);
    }

    /**
     * GET /api/risk/population-stats
     * Get population-level risk statistics for a tenant
     */
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/risk/population-stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PopulationRiskStats> getPopulationStats(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId
    ) {
        log.info("GET /api/risk/population-stats - tenant: {}", tenantId);

        // Get unique patient count for each risk level
        Long lowCount = riskAssessmentRepository.countPatientsByRiskLevel(tenantId, RiskAssessmentEntity.RiskLevel.LOW);
        Long moderateCount = riskAssessmentRepository.countPatientsByRiskLevel(tenantId, RiskAssessmentEntity.RiskLevel.MODERATE);
        Long highCount = riskAssessmentRepository.countPatientsByRiskLevel(tenantId, RiskAssessmentEntity.RiskLevel.HIGH);
        Long veryHighCount = riskAssessmentRepository.countPatientsByRiskLevel(tenantId, RiskAssessmentEntity.RiskLevel.VERY_HIGH);

        Long totalPatients = lowCount + moderateCount + highCount + veryHighCount;

        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("low", lowCount);
        distribution.put("moderate", moderateCount);
        distribution.put("high", highCount);
        distribution.put("very-high", veryHighCount);

        PopulationRiskStats stats = new PopulationRiskStats(totalPatients, distribution);

        return ResponseEntity.ok(stats);
    }

    /**
     * Map entity to DTO
     */
    private RiskAssessmentDTO mapToDTO(RiskAssessmentEntity entity) {
        List<RiskAssessmentDTO.RiskFactorDTO> riskFactorDTOs = entity.getRiskFactors().stream()
            .map(map -> RiskAssessmentDTO.RiskFactorDTO.builder()
                .factor((String) map.get("factor"))
                .category((String) map.get("category"))
                .weight(((Number) map.get("weight")).intValue())
                .severity((String) map.get("severity"))
                .evidence((String) map.get("evidence"))
                .build())
            .collect(Collectors.toList());

        List<RiskAssessmentDTO.PredictedOutcomeDTO> outcomeDTOs = entity.getPredictedOutcomes().stream()
            .map(map -> RiskAssessmentDTO.PredictedOutcomeDTO.builder()
                .outcome((String) map.get("outcome"))
                .probability(((Number) map.get("probability")).doubleValue())
                .timeframe((String) map.get("timeframe"))
                .build())
            .collect(Collectors.toList());

        return RiskAssessmentDTO.builder()
            .id(entity.getId().toString())
            .patientId(entity.getPatientId())
            .riskCategory(entity.getRiskCategory())
            .riskScore(entity.getRiskScore())
            .riskLevel(entity.getRiskLevel().name().toLowerCase().replace("_", "-"))
            .riskFactors(riskFactorDTOs)
            .predictedOutcomes(outcomeDTOs)
            .recommendations(entity.getRecommendations())
            .assessmentDate(entity.getAssessmentDate())
            .createdAt(entity.getCreatedAt())
            .build();
    }

    /**
     * Response DTO for population statistics
     */
    public record PopulationRiskStats(
        Long totalPatients,
        Map<String, Long> riskLevelDistribution
    ) {}

    /**
     * Exception handler for validation errors
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(IllegalArgumentException ex) {
        log.error("Validation error: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage()));
    }
}
