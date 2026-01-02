package com.healthdata.ecr.controller;

import com.healthdata.ecr.persistence.ElectronicCaseReportEntity;
import com.healthdata.ecr.persistence.ElectronicCaseReportRepository;
import com.healthdata.ecr.service.EcrProcessingService;
import com.healthdata.ecr.service.EcrTriggerService;
import com.healthdata.ecr.trigger.RctcRulesEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for Electronic Case Reporting (eCR) operations.
 */
@RestController
@RequestMapping("/api/ecr")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Electronic Case Reporting", description = "eCR management and public health reporting")
public class EcrController {

    private final ElectronicCaseReportRepository ecrRepository;
    private final EcrProcessingService processingService;
    private final EcrTriggerService triggerService;
    private final RctcRulesEngine rctcRulesEngine;

    /**
     * Get all eCRs for a tenant with optional status filter.
     */
    @GetMapping
    @Operation(summary = "List eCRs", description = "Returns paginated list of electronic case reports")
    public ResponseEntity<Page<ElectronicCaseReportEntity>> listEcrs(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) ElectronicCaseReportEntity.EcrStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ElectronicCaseReportEntity> ecrs;
        if (status != null) {
            ecrs = ecrRepository.findByTenantIdAndStatus(tenantId, status, PageRequest.of(page, size));
        } else {
            ecrs = ecrRepository.findAll(PageRequest.of(page, size));
        }

        return ResponseEntity.ok(ecrs);
    }

    /**
     * Get a specific eCR by ID.
     */
    @GetMapping("/{ecrId}")
    @Operation(summary = "Get eCR", description = "Returns a specific electronic case report")
    public ResponseEntity<ElectronicCaseReportEntity> getEcr(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID ecrId) {

        return ecrRepository.findByTenantIdAndId(tenantId, ecrId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get eCRs for a specific patient.
     */
    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get patient eCRs", description = "Returns all eCRs for a patient")
    public ResponseEntity<List<ElectronicCaseReportEntity>> getPatientEcrs(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        List<ElectronicCaseReportEntity> ecrs =
            ecrRepository.findByTenantIdAndPatientId(tenantId, patientId);

        return ResponseEntity.ok(ecrs);
    }

    /**
     * Manually trigger eCR evaluation for a patient's clinical codes.
     */
    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate codes", description = "Manually evaluate clinical codes for reportable conditions")
    public ResponseEntity<List<ElectronicCaseReportEntity>> evaluateCodes(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody EvaluationRequest request) {

        log.info("Manual eCR evaluation requested for patient {}", request.getPatientId());

        List<ElectronicCaseReportEntity> createdEcrs = triggerService.evaluatePatientCodes(
            tenantId,
            request.getPatientId(),
            request.getDiagnosisCodes() != null ? request.getDiagnosisCodes() : List.of(),
            request.getLabCodes() != null ? request.getLabCodes() : List.of()
        );

        return ResponseEntity.ok(createdEcrs);
    }

    /**
     * Manually reprocess a failed eCR.
     */
    @PostMapping("/{ecrId}/reprocess")
    @Operation(summary = "Reprocess eCR", description = "Manually reprocess a failed electronic case report")
    public ResponseEntity<Map<String, String>> reprocessEcr(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID ecrId) {

        ElectronicCaseReportEntity ecr = ecrRepository.findByTenantIdAndId(tenantId, ecrId)
            .orElse(null);

        if (ecr == null) {
            return ResponseEntity.notFound().build();
        }

        if (ecr.getStatus() != ElectronicCaseReportEntity.EcrStatus.FAILED) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Only FAILED eCRs can be reprocessed"));
        }

        // Reset for reprocessing
        ecr.setStatus(ElectronicCaseReportEntity.EcrStatus.PENDING);
        ecr.setErrorMessage(null);
        ecr.setRetryCount(0);
        ecrRepository.save(ecr);

        // Process immediately
        processingService.processImmediately(ecrId);

        return ResponseEntity.ok(Map.of(
            "message", "eCR queued for reprocessing",
            "ecrId", ecrId.toString()
        ));
    }

    /**
     * Cancel a pending eCR.
     */
    @PostMapping("/{ecrId}/cancel")
    @Operation(summary = "Cancel eCR", description = "Cancel a pending electronic case report")
    public ResponseEntity<ElectronicCaseReportEntity> cancelEcr(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID ecrId) {

        ElectronicCaseReportEntity ecr = ecrRepository.findByTenantIdAndId(tenantId, ecrId)
            .orElse(null);

        if (ecr == null) {
            return ResponseEntity.notFound().build();
        }

        if (ecr.getStatus() == ElectronicCaseReportEntity.EcrStatus.SUBMITTED ||
            ecr.getStatus() == ElectronicCaseReportEntity.EcrStatus.ACKNOWLEDGED) {
            return ResponseEntity.badRequest().build();
        }

        ecr.setStatus(ElectronicCaseReportEntity.EcrStatus.CANCELLED);
        ecr = ecrRepository.save(ecr);

        return ResponseEntity.ok(ecr);
    }

    /**
     * Get eCR status summary for dashboard.
     */
    @GetMapping("/summary")
    @Operation(summary = "Get eCR summary", description = "Returns status counts for dashboard")
    public ResponseEntity<Map<String, Long>> getStatusSummary(
            @RequestHeader("X-Tenant-ID") String tenantId) {

        List<Object[]> counts = ecrRepository.countByStatus(tenantId);

        Map<String, Long> summary = counts.stream()
            .collect(Collectors.toMap(
                row -> ((ElectronicCaseReportEntity.EcrStatus) row[0]).name(),
                row -> (Long) row[1]
            ));

        return ResponseEntity.ok(summary);
    }

    /**
     * Get list of monitored conditions from RCTC.
     */
    @GetMapping("/conditions")
    @Operation(summary = "Get monitored conditions", description = "Returns list of reportable conditions being monitored")
    public ResponseEntity<Set<String>> getMonitoredConditions() {
        return ResponseEntity.ok(rctcRulesEngine.getMonitoredConditions());
    }

    /**
     * Check if a specific code is a reportable trigger.
     */
    @GetMapping("/check-trigger")
    @Operation(summary = "Check trigger code", description = "Check if a code triggers reportable condition detection")
    public ResponseEntity<TriggerCheckResult> checkTriggerCode(
            @RequestParam String code,
            @RequestParam String codeSystem) {

        boolean isReportable = rctcRulesEngine.isReportableTrigger(code, codeSystem);

        RctcRulesEngine.TriggerMatch match = null;
        if (isReportable) {
            match = switch (codeSystem) {
                case RctcRulesEngine.ICD10CM_OID ->
                    rctcRulesEngine.evaluateDiagnosis(code).orElse(null);
                case RctcRulesEngine.LOINC_OID ->
                    rctcRulesEngine.evaluateLabResult(code).orElse(null);
                case RctcRulesEngine.RXNORM_OID ->
                    rctcRulesEngine.evaluateMedication(code).orElse(null);
                default -> null;
            };
        }

        return ResponseEntity.ok(TriggerCheckResult.builder()
            .code(code)
            .codeSystem(codeSystem)
            .isReportable(isReportable)
            .conditionName(match != null ? match.getConditionName() : null)
            .urgency(match != null ? match.getUrgency().name() : null)
            .build());
    }

    // Request/Response DTOs

    @lombok.Data
    public static class EvaluationRequest {
        private UUID patientId;
        private List<String> diagnosisCodes;
        private List<String> labCodes;
    }

    @lombok.Data
    @lombok.Builder
    public static class TriggerCheckResult {
        private String code;
        private String codeSystem;
        private boolean isReportable;
        private String conditionName;
        private String urgency;
    }
}
