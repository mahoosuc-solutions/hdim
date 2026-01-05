package com.healthdata.hcc.controller;

import com.healthdata.hcc.persistence.DocumentationGapEntity;
import com.healthdata.hcc.persistence.DocumentationGapRepository;
import com.healthdata.hcc.persistence.PatientHccProfileEntity;
import com.healthdata.hcc.persistence.PatientHccProfileRepository;
import com.healthdata.hcc.service.RafCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;

/**
 * REST controller for HCC risk adjustment operations.
 */
@RestController
@RequestMapping("/api/v1/hcc")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "HCC Risk Adjustment", description = "HCC V28 risk adjustment and documentation gap management")
public class HccController {

    private final RafCalculationService rafCalculationService;
    private final PatientHccProfileRepository profileRepository;
    private final DocumentationGapRepository gapRepository;

    /**
     * Calculate RAF score for a patient.
     */
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping("/patient/{patientId}/calculate")
    @Operation(summary = "Calculate RAF score",
        description = "Calculates V24, V28, and blended RAF scores for a patient")
    public ResponseEntity<RafCalculationService.RafCalculationResult> calculateRaf(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @Valid @RequestBody RafCalculationRequest request) {

        log.info("RAF calculation requested for patient {} by tenant {}", patientId, tenantId);

        RafCalculationService.DemographicFactors factors = RafCalculationService.DemographicFactors.builder()
            .age(request.getAge())
            .sex(request.getSex())
            .dualEligible(request.isDualEligible())
            .institutionalized(request.isInstitutionalized())
            .build();

        RafCalculationService.RafCalculationResult result = rafCalculationService.calculateRaf(
            tenantId, patientId, request.getDiagnosisCodes(), factors);

        return ResponseEntity.ok(result);
    }

    /**
     * Get patient HCC profile.
     */
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/patient/{patientId}/profile")
    @Operation(summary = "Get HCC profile",
        description = "Returns patient HCC profile with RAF scores and captured HCCs")
    public ResponseEntity<PatientHccProfileEntity> getProfile(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") Integer year) {

        return profileRepository.findByTenantIdAndPatientIdAndProfileYear(tenantId, patientId, year)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get ICD-10 to HCC crosswalk.
     */
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/crosswalk")
    @Operation(summary = "Get HCC crosswalk",
        description = "Returns V24 and V28 HCC mappings for given ICD-10 codes")
    public ResponseEntity<?> getCrosswalk(
            @RequestParam List<String> icd10Codes) {

        return ResponseEntity.ok(rafCalculationService.batchCrosswalk(icd10Codes));
    }

    /**
     * Get documentation gaps for a patient.
     */
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/patient/{patientId}/documentation-gaps")
    @Operation(summary = "Get documentation gaps",
        description = "Returns open documentation gaps that could improve RAF score")
    public ResponseEntity<List<DocumentationGapEntity>> getDocumentationGaps(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") Integer year) {

        List<DocumentationGapEntity> gaps = gapRepository.findByTenantIdAndPatientIdAndProfileYear(
            tenantId, patientId, year);

        return ResponseEntity.ok(gaps);
    }

    /**
     * Get high-value opportunities across population.
     */
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/opportunities")
    @Operation(summary = "Get high-value opportunities",
        description = "Returns patients with highest potential RAF uplift")
    public ResponseEntity<List<PatientHccProfileEntity>> getHighValueOpportunities(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") Integer year,
            @RequestParam(defaultValue = "0.1") java.math.BigDecimal minUplift) {

        List<PatientHccProfileEntity> opportunities = profileRepository.findHighValueOpportunities(
            tenantId, year, minUplift);

        return ResponseEntity.ok(opportunities);
    }

    @lombok.Data
    public static class RafCalculationRequest {
        private List<String> diagnosisCodes;
        private Integer age;
        private String sex;
        private boolean dualEligible;
        private boolean institutionalized;
    }
}
