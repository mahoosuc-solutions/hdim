package com.healthdata.sdoh.controller;

import com.healthdata.sdoh.model.*;
import com.healthdata.sdoh.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;

/**
 * SDOH Integration Service REST API Controller
 *
 * Endpoints for SDOH screening, Z-code mapping, community resources, and health equity analytics
 */
@RestController
@RequestMapping("/api/v1/sdoh")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SDOH Service", description = "Social Determinants of Health API")
public class SdohController {

    private final GravityScreeningService screeningService;
    private final ZCodeMapper zCodeMapper;
    private final CommunityResourceService resourceService;
    private final HealthEquityAnalyzer equityAnalyzer;
    private final SdohRiskCalculator riskCalculator;

    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping("/screening/{patientId}")
    @PreAuthorize("hasPermission('patient:write')")
    @Operation(summary = "Submit SDOH screening for patient")
    public ResponseEntity<SdohAssessment> submitScreening(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable String patientId,
            @RequestBody Map<String, Object> request) {

        if (tenantId == null || tenantId.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String screeningTool = (String) request.get("screeningTool");
        @SuppressWarnings("unchecked")
        List<SdohScreeningResponse> responses = (List<SdohScreeningResponse>) request.get("responses");

        SdohAssessment assessment = screeningService.submitScreening(
                tenantId, patientId, screeningTool, responses);

        return ResponseEntity.ok(assessment);
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/assessment/{patientId}")
    @PreAuthorize("hasPermission('patient:read')")
    @Operation(summary = "Get most recent SDOH assessment for patient")
    public ResponseEntity<SdohAssessment> getPatientAssessment(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String patientId) {

        return screeningService.getMostRecentAssessment(tenantId, patientId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/z-codes/{patientId}")
    @PreAuthorize("hasPermission('patient:read')")
    @Operation(summary = "Get SDOH Z-codes for patient")
    public ResponseEntity<List<SdohDiagnosis>> getPatientZCodes(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String patientId) {

        List<SdohDiagnosis> diagnoses = zCodeMapper.getActiveDiagnoses(tenantId, patientId);
        return ResponseEntity.ok(diagnoses);
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/resources")
    @PreAuthorize("hasPermission('patient:read')")
    @Operation(summary = "Search community resources")
    public ResponseEntity<List<CommunityResource>> searchCommunityResources(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String zipCode) {

        List<CommunityResource> resources;

        if (category != null) {
            resources = resourceService.searchByCategory(ResourceCategory.valueOf(category));
        } else if (city != null && state != null) {
            resources = resourceService.searchByLocation(city, state);
        } else if (zipCode != null) {
            resources = resourceService.searchByZipCode(zipCode);
        } else {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(resources);
    }

    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping("/referral")
    @PreAuthorize("hasPermission('patient:write')")
    @Operation(summary = "Create resource referral for patient")
    public ResponseEntity<ResourceReferral> createResourceReferral(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody Map<String, Object> request) {

        String patientId = (String) request.get("patientId");
        String resourceId = (String) request.get("resourceId");
        SdohCategory category = SdohCategory.valueOf((String) request.get("category"));
        String reason = (String) request.get("reason");
        String referredBy = (String) request.get("referredBy");

        ResourceReferral referral = resourceService.createReferral(
                tenantId, patientId, resourceId, category, reason, referredBy);

        return ResponseEntity.status(HttpStatus.CREATED).body(referral);
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/equity/report")
    @PreAuthorize("hasPermission('report:read')")
    @Operation(summary = "Generate health equity analytics report")
    public ResponseEntity<EquityReport> getHealthEquityReport(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        EquityReport report = equityAnalyzer.generateEquityReport(
                tenantId,
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59));

        return ResponseEntity.ok(report);
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/risk/{patientId}")
    @PreAuthorize("hasPermission('patient:read')")
    @Operation(summary = "Get SDOH risk score for patient")
    public ResponseEntity<List<SdohRiskScore>> getPatientRiskScore(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String patientId) {

        List<SdohRiskScore> scores = riskCalculator.getRiskScoreHistory(tenantId, patientId);
        return ResponseEntity.ok(scores);
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/referrals/{patientId}")
    @PreAuthorize("hasPermission('patient:read')")
    @Operation(summary = "Get resource referrals for patient")
    public ResponseEntity<List<ResourceReferral>> getPatientReferrals(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String patientId) {

        List<ResourceReferral> referrals = resourceService.getPatientReferrals(tenantId, patientId);
        return ResponseEntity.ok(referrals);
    }

    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping("/referral/{referralId}/status")
    @PreAuthorize("hasPermission('patient:write')")
    @Operation(summary = "Update referral status")
    public ResponseEntity<Void> updateReferralStatus(
            @PathVariable String referralId,
            @RequestBody Map<String, Object> request) {

        String status = (String) request.get("status");
        resourceService.updateReferralStatus(referralId, ResourceReferral.ReferralStatus.valueOf(status));

        return ResponseEntity.ok().build();
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/screening/questions")
    @PreAuthorize("hasPermission('patient:read')")
    @Operation(summary = "Get screening questionnaire")
    public ResponseEntity<List<SdohScreeningQuestion>> getScreeningQuestions(
            @RequestParam(defaultValue = "AHC-HRSN") String tool) {

        List<SdohScreeningQuestion> questions;
        if ("PRAPARE".equalsIgnoreCase(tool)) {
            questions = screeningService.createPrapareQuestionnaire();
        } else {
            questions = screeningService.createAhcHrsnQuestionnaire();
        }

        return ResponseEntity.ok(questions);
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/_health")
    @Operation(summary = "Health check endpoint")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "sdoh-service",
                "timestamp", LocalDate.now().toString()
        ));
    }
}
