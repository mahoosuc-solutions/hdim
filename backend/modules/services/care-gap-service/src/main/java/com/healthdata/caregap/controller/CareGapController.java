package com.healthdata.caregap.controller;

import com.healthdata.caregap.persistence.CareGapEntity;
import com.healthdata.caregap.service.CareGapIdentificationService;
import com.healthdata.caregap.service.CareGapReportService;
import com.healthdata.caregap.service.ProviderCareGapPrioritizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;

/**
 * Care Gap Controller
 *
 * REST API for care gap identification, management, and reporting.
 *
 * Endpoints:
 * - POST /care-gap/identify - Identify all care gaps for a patient
 * - POST /care-gap/identify/{library} - Identify gaps for specific measure
 * - POST /care-gap/refresh - Refresh care gaps (re-evaluate)
 * - POST /care-gap/close - Close a care gap
 * - GET /care-gap/open - Get open care gaps
 * - GET /care-gap/high-priority - Get high priority gaps
 * - GET /care-gap/overdue - Get overdue gaps
 * - GET /care-gap/upcoming - Get upcoming gaps
 * - GET /care-gap/stats - Get care gap statistics
 * - GET /care-gap/summary - Get care gap summary
 * - GET /care-gap/by-category - Get gaps grouped by category
 * - GET /care-gap/by-priority - Get gaps grouped by priority
 * - GET /care-gap/population-report - Get population-level report
 * - GET /care-gap/_health - Health check endpoint
 */
@RestController
@RequestMapping("/care-gap")
@RequiredArgsConstructor
@Slf4j
public class CareGapController {

    private final CareGapIdentificationService identificationService;
    private final CareGapReportService reportService;
    private final ProviderCareGapPrioritizationService prioritizationService;

    // ==================== Care Gap Identification Endpoints ====================

    /**
     * Identify all care gaps for a patient
     *
     * @param tenantId Tenant ID (from header)
     * @param patientId Patient ID (query parameter)
     * @param createdBy User performing identification
     * @return List of identified care gaps
     */
    @PreAuthorize("hasPermission('CARE_GAP_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/identify", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CareGapEntity>> identifyAllCareGaps(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") UUID patientId,
            @RequestParam(value = "createdBy", defaultValue = "system") String createdBy
    ) {
        log.info("POST /care-gap/identify - tenant: {}, patient: {}", tenantId, patientId);

        List<CareGapEntity> gaps = identificationService.identifyAllCareGaps(
                tenantId, patientId, createdBy);

        return ResponseEntity.status(HttpStatus.CREATED).body(gaps);
    }

    /**
     * Identify care gaps for a specific CQL library/measure
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param library CQL library name
     * @param createdBy User performing identification
     * @return List of identified care gaps
     */
    @PreAuthorize("hasPermission('CARE_GAP_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/identify/{library}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CareGapEntity>> identifyCareGapsForLibrary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") UUID patientId,
            @PathVariable("library") String library,
            @RequestParam(value = "createdBy", defaultValue = "system") String createdBy
    ) {
        log.info("POST /care-gap/identify/{} - patient: {}", library, patientId);

        List<CareGapEntity> gaps = identificationService.identifyCareGapsForLibrary(
                tenantId, patientId, library, createdBy);

        return ResponseEntity.status(HttpStatus.CREATED).body(gaps);
    }

    /**
     * Refresh care gaps for a patient (re-evaluate all measures)
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param createdBy User performing refresh
     * @return List of active care gaps
     */
    @PreAuthorize("hasPermission('CARE_GAP_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CareGapEntity>> refreshCareGaps(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") UUID patientId,
            @RequestParam(value = "createdBy", defaultValue = "system") String createdBy
    ) {
        log.info("POST /care-gap/refresh - patient: {}", patientId);

        List<CareGapEntity> gaps = identificationService.refreshCareGaps(
                tenantId, patientId, createdBy);

        return ResponseEntity.ok(gaps);
    }

    /**
     * Close a care gap
     *
     * @param tenantId Tenant ID
     * @param gapId Gap ID
     * @param closedBy User closing the gap
     * @param closureReason Reason for closure
     * @param closureAction Action taken to close gap
     * @return Updated care gap
     */
    @PreAuthorize("hasPermission('CARE_GAP_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/close", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CareGapEntity> closeCareGap(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("gapId") String gapId,
            @RequestParam("closedBy") String closedBy,
            @RequestParam(value = "closureReason", required = false) String closureReason,
            @RequestParam(value = "closureAction", required = false) String closureAction
    ) {
        log.info("POST /care-gap/close - gapId: {}, closedBy: {}", gapId, closedBy);

        CareGapEntity gap = identificationService.closeCareGap(
                tenantId,
                UUID.fromString(gapId),
                closedBy,
                closureReason,
                closureAction
        );

        return ResponseEntity.ok(gap);
    }

    // ==================== Care Gap Query Endpoints ====================

    /**
     * Get open care gaps for a patient
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return List of open care gaps
     */
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/open", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CareGapEntity>> getOpenCareGaps(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") UUID patientId
    ) {
        log.info("GET /care-gap/open - patient: {}", patientId);

        List<CareGapEntity> gaps = identificationService.getOpenCareGaps(tenantId, patientId);

        return ResponseEntity.ok(gaps);
    }

    /**
     * Get high priority care gaps for a patient
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return List of high priority care gaps
     */
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/high-priority", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CareGapEntity>> getHighPriorityCareGaps(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") UUID patientId
    ) {
        log.info("GET /care-gap/high-priority - patient: {}", patientId);

        List<CareGapEntity> gaps = identificationService.getHighPriorityCareGaps(tenantId, patientId);

        return ResponseEntity.ok(gaps);
    }

    /**
     * Get overdue care gaps for a patient
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return List of overdue care gaps
     */
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/overdue", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CareGapEntity>> getOverdueGaps(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") UUID patientId
    ) {
        log.info("GET /care-gap/overdue - patient: {}", patientId);

        List<CareGapEntity> gaps = reportService.getOverdueGaps(tenantId, patientId);

        return ResponseEntity.ok(gaps);
    }

    /**
     * Get upcoming care gaps (due within N days)
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param days Number of days to look ahead (default: 30)
     * @return List of upcoming care gaps
     */
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/upcoming", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CareGapEntity>> getUpcomingGaps(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") UUID patientId,
            @RequestParam(value = "days", defaultValue = "30") int days
    ) {
        log.info("GET /care-gap/upcoming - patient: {}, days: {}", patientId, days);

        List<CareGapEntity> gaps = reportService.getUpcomingGaps(tenantId, patientId, days);

        return ResponseEntity.ok(gaps);
    }

    // ==================== Care Gap Statistics & Reports ====================

    /**
     * Get care gap statistics for a patient
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Care gap statistics
     */
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CareGapIdentificationService.CareGapStats> getCareGapStats(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") UUID patientId
    ) {
        log.info("GET /care-gap/stats - patient: {}", patientId);

        CareGapIdentificationService.CareGapStats stats =
                identificationService.getCareGapStats(tenantId, patientId);

        return ResponseEntity.ok(stats);
    }

    /**
     * Get care gap summary for a patient
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Care gap summary
     */
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CareGapReportService.CareGapSummary> getCareGapSummary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") UUID patientId
    ) {
        log.info("GET /care-gap/summary - patient: {}", patientId);

        CareGapReportService.CareGapSummary summary =
                reportService.getCareGapSummary(tenantId, patientId);

        return ResponseEntity.ok(summary);
    }

    /**
     * Get care gaps grouped by measure category
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Map of measure category -> gap count
     */
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/by-category", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> getGapsByCategory(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") UUID patientId
    ) {
        log.info("GET /care-gap/by-category - patient: {}", patientId);

        Map<String, Long> gaps = reportService.getGapsByMeasureCategory(tenantId, patientId);

        return ResponseEntity.ok(gaps);
    }

    /**
     * Get care gaps grouped by priority
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Map of priority -> gap count
     */
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/by-priority", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> getGapsByPriority(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") UUID patientId
    ) {
        log.info("GET /care-gap/by-priority - patient: {}", patientId);

        Map<String, Long> gaps = reportService.getGapsByPriority(tenantId, patientId);

        return ResponseEntity.ok(gaps);
    }

    /**
     * Get population-level gap report for a tenant
     *
     * @param tenantId Tenant ID
     * @return Population gap report
     */
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/population-report", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CareGapReportService.PopulationGapReport> getPopulationGapReport(
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        log.info("GET /care-gap/population-report - tenant: {}", tenantId);

        CareGapReportService.PopulationGapReport report =
                reportService.getPopulationGapReport(tenantId);

        return ResponseEntity.ok(report);
    }

    // ==================== Provider-Specific Endpoints (Issue #138) ====================

    /**
     * Get prioritized care gaps for a provider
     *
     * Issue #138: Provider-specific care gap prioritization with scoring algorithm.
     * Scoring: urgency (40%) + due date (30%) + intervention ease (30%)
     * Includes primary care priority rules and recommended actions.
     *
     * @param tenantId Tenant ID
     * @param providerId Provider ID
     * @param limit Maximum number of gaps to return (default: 50)
     * @return List of prioritized care gaps with scores and recommendations
     */
    @PreAuthorize("hasPermission('CARE_GAP_WRITE')")
    @Audited(action = AuditAction.READ, resourceType = "CareGap", purposeOfUse = "TREATMENT",
            description = "Provider care gap prioritization lookup")
    @GetMapping(value = "/providers/{providerId}/prioritized", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProviderCareGapPrioritizationService.PrioritizedCareGap>> getProviderPrioritizedGaps(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable("providerId") String providerId,
            @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        log.info("GET /care-gap/providers/{}/prioritized - tenant: {}, limit: {}", providerId, tenantId, limit);

        List<ProviderCareGapPrioritizationService.PrioritizedCareGap> prioritizedGaps =
                prioritizationService.getPrioritizedCareGaps(tenantId, providerId, limit);

        return ResponseEntity.ok(prioritizedGaps);
    }

    /**
     * Get provider gap summary statistics
     *
     * @param tenantId Tenant ID
     * @param providerId Provider ID
     * @return Summary statistics for provider's care gaps
     */
    @PreAuthorize("hasPermission('CARE_GAP_WRITE')")
    @Audited(action = AuditAction.READ, resourceType = "CareGap", purposeOfUse = "TREATMENT",
            description = "Provider care gap summary lookup")
    @GetMapping(value = "/providers/{providerId}/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProviderCareGapPrioritizationService.ProviderGapSummary> getProviderGapSummary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable("providerId") String providerId
    ) {
        log.info("GET /care-gap/providers/{}/summary - tenant: {}", providerId, tenantId);

        ProviderCareGapPrioritizationService.ProviderGapSummary summary =
                prioritizationService.getProviderGapSummary(tenantId, providerId);

        return ResponseEntity.ok(summary);
    }

    // ==================== Health Check ====================

    /**
     * Health check endpoint
     *
     * @return Health status
     */
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/_health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "care-gap-service",
                "timestamp", LocalDate.now().toString()
        ));
    }
}
