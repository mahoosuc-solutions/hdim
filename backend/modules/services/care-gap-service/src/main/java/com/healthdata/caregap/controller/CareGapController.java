package com.healthdata.caregap.controller;

import com.healthdata.caregap.dto.CareGapDetectionRequest;
import com.healthdata.caregap.dto.CareGapDetectionResponse;
import com.healthdata.caregap.persistence.CareGapEntity;
import com.healthdata.caregap.persistence.CareGapRepository;
import com.healthdata.caregap.service.CareGapIdentificationService;
import com.healthdata.caregap.service.CareGapReportService;
import com.healthdata.caregap.service.ProviderCareGapPrioritizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * - POST /care-gap/bulk-close - Bulk close multiple care gaps (Issue #241)
 * - POST /care-gap/bulk-assign-intervention - Bulk assign intervention (Issue #241)
 * - PUT /care-gap/bulk-update-priority - Bulk update priority (Issue #241)
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
@Tag(
    name = "Care Gap Management",
    description = """
        APIs for care gap identification, tracking, closure, and reporting.

        Care gaps represent missed quality measure opportunities or preventive care services.
        This API supports:
        - CQL-based care gap identification (HEDIS, CMS measures)
        - Bulk operations for provider workflows
        - Priority scoring and provider-specific gap lists
        - Population-level gap reporting for quality teams
        - Closure tracking with intervention documentation

        All endpoints require JWT authentication and X-Tenant-ID header.
        Care gap operations are audited for compliance and quality reporting.
        """
)
public class CareGapController {

    private final CareGapIdentificationService identificationService;
    private final CareGapReportService reportService;
    private final ProviderCareGapPrioritizationService prioritizationService;
    private final CareGapRepository careGapRepository;

    // ==================== Care Gap Identification Endpoints ====================

    /**
     * Identify all care gaps for a patient
     *
     * @param tenantId Tenant ID (from header)
     * @param patientId Patient ID (query parameter)
     * @param createdBy User performing identification
     * @return List of identified care gaps
     */
    @Operation(
        summary = "Identify all care gaps for a patient",
        description = """
            Executes all configured CQL measure libraries to identify care gaps for a patient.

            Evaluates HEDIS, CMS, and custom quality measures against patient's clinical data.
            Creates new care gap records for measures not currently met.
            Automatically calculates due dates and priority based on measure specifications.

            Use for: Initial patient assessment, annual wellness visits, care management enrollment.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Care gaps identified successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Care Gap List",
                    value = """
                        [
                          {
                            "id": "550e8400-e29b-41d4-a716-446655440000",
                            "patientId": "patient-123",
                            "measureName": "Diabetes HbA1c Testing",
                            "measureCategory": "HEDIS",
                            "status": "OPEN",
                            "priority": "HIGH",
                            "dueDate": "2024-03-31",
                            "identifiedDate": "2024-01-24"
                          }
                        ]
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission(null, 'CARE_GAP_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/identify", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CareGapEntity>> identifyAllCareGaps(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID (UUID)", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam("patient") UUID patientId,
            @Parameter(description = "User performing identification", example = "system")
            @RequestParam(value = "createdBy", defaultValue = "system") String createdBy
    ) {
        log.info("POST /care-gap/identify - tenant: {}, patient: {}", tenantId, patientId);

        List<CareGapEntity> gaps = identificationService.identifyAllCareGaps(
                tenantId, patientId, createdBy);

        return ResponseEntity.status(HttpStatus.CREATED).body(gaps);
    }

    /**
     * Detect a care gap from measure results and persist it.
     */
    @Operation(
        summary = "Detect a care gap from measure results",
        description = """
            Creates a care gap record when a patient is eligible for a measure but is not compliant.
            Intended for direct measure-driven workflows (e.g., Annual Wellness Visit).
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Care gap detected and created",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(responseCode = "204", description = "No care gap detected"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission(null, 'CARE_GAP_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/detect", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CareGapDetectionResponse> detectCareGap(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody CareGapDetectionRequest request
    ) {
        if (!request.isDenominatorEligible() || request.isNumeratorCompliant()) {
            return ResponseEntity.noContent().build();
        }

        String measureId = request.getMeasureId();
        String category = "PREVENTIVE";
        String title = "Care Gap";
        String priority = "HIGH";
        String gapType = "preventive-care";
        String description = "Preventive care gap detected";

        if ("HEDIS_AWV".equalsIgnoreCase(measureId)) {
            title = "Annual Wellness Visit";
            description = "Annual Wellness Visit is due";
        }

        CareGapEntity gap = CareGapEntity.builder()
                .tenantId(tenantId)
                .patientId(request.getPatientId())
                .measureId(measureId)
                .measureName(title)
                .gapCategory(category)
                .gapType(gapType)
                .gapStatus("OPEN")
                .priority(priority)
                .gapDescription(description)
                .measureYear(LocalDate.now().getYear())
                .dueDate(LocalDate.now().plusDays(30))
                .createdBy(request.getCreatedBy() != null ? request.getCreatedBy() : "system")
                .build();

        CareGapEntity saved = careGapRepository.save(gap);

        CareGapDetectionResponse response = CareGapDetectionResponse.builder()
                .id(saved.getId())
                .category(saved.getGapCategory())
                .title(saved.getMeasureName())
                .priority(saved.getPriority())
                .status(saved.getGapStatus())
                .dueDate(saved.getDueDate())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
    @Operation(
        summary = "Identify care gaps for specific measure",
        description = """
            Executes a single CQL measure library to identify gaps for a patient.

            Useful for targeted gap identification (e.g., diabetes measures only).
            Faster than full identification when evaluating specific clinical domains.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Care gaps identified", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Patient or library not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission(null, 'CARE_GAP_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/identify/{library}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CareGapEntity>> identifyCareGapsForLibrary(
            @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true) @RequestParam("patient") UUID patientId,
            @Parameter(description = "CQL library name", required = true, example = "DiabetesHbA1c") @PathVariable("library") String library,
            @Parameter(description = "User performing identification") @RequestParam(value = "createdBy", defaultValue = "system") String createdBy
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
    @Operation(
        summary = "Refresh care gaps (re-evaluate all measures)",
        description = """
            Re-evaluates all CQL measures and updates existing care gap records.

            Closes gaps that are now met, identifies new gaps based on updated clinical data.
            Use after significant clinical data imports or annual re-evaluation.

            More efficient than delete+re-identify as it preserves gap history.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Care gaps refreshed successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission(null, 'CARE_GAP_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CareGapEntity>> refreshCareGaps(
            @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true) @RequestParam("patient") UUID patientId,
            @Parameter(description = "User performing refresh") @RequestParam(value = "createdBy", defaultValue = "system") String createdBy
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
    @Operation(
        summary = "Close a care gap",
        description = """
            Marks a care gap as closed and records intervention details.

            Captures closure reason and action taken for quality reporting and compliance.
            Use when gap is addressed (intervention completed) or no longer applicable.

            Closure data is used for HEDIS/Stars quality measure numerator reporting.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Care gap closed successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Closed Gap",
                    value = """
                        {
                          "id": "550e8400-e29b-41d4-a716-446655440000",
                          "status": "CLOSED",
                          "closedDate": "2024-01-24",
                          "closedBy": "dr.smith",
                          "closureReason": "Annual screening completed",
                          "closureAction": "Mammogram performed"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Care gap not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission(null, 'CARE_GAP_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/close", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CareGapEntity> closeCareGap(
            @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Care gap ID (UUID)", required = true, example = "550e8400-e29b-41d4-a716-446655440000") @RequestParam("gapId") String gapId,
            @Parameter(description = "User closing the gap", required = true, example = "dr.smith") @RequestParam("closedBy") String closedBy,
            @Parameter(description = "Reason for closure", example = "Annual screening completed") @RequestParam(value = "closureReason", required = false) String closureReason,
            @Parameter(description = "Action taken to close gap", example = "Mammogram performed") @RequestParam(value = "closureAction", required = false) String closureAction
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
    @Operation(summary = "Get open care gaps", description = "Retrieves all currently open gaps for a patient.\n\nUse for care management worklists and patient care plans.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Open gaps retrieved", content = @Content(mediaType = "application/json")), @ApiResponse(responseCode = "404", description = "Patient not found")})
    @PreAuthorize("hasPermission(null, 'CARE_GAP_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/open", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CareGapEntity>> getOpenCareGaps(
            @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true) @RequestParam("patient") UUID patientId
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
    @Operation(summary = "Get high priority care gaps", description = "Retrieves gaps marked as high priority.\n\nUse for urgent intervention planning and care team escalation.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "High priority gaps retrieved", content = @Content(mediaType = "application/json")), @ApiResponse(responseCode = "404", description = "Patient not found")})
    @PreAuthorize("hasPermission(null, 'CARE_GAP_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/high-priority", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CareGapEntity>> getHighPriorityCareGaps(
            @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true) @RequestParam("patient") UUID patientId
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
    @Operation(summary = "Get overdue care gaps", description = "Retrieves gaps past their due date.\n\nUse for quality measure deadline tracking and compliance reporting.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Overdue gaps retrieved", content = @Content(mediaType = "application/json")), @ApiResponse(responseCode = "404", description = "Patient not found")})
    @PreAuthorize("hasPermission(null, 'CARE_GAP_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/overdue", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CareGapEntity>> getOverdueGaps(
            @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true) @RequestParam("patient") UUID patientId
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
    @Operation(summary = "Get upcoming care gaps", description = "Retrieves gaps due within N days.\n\nUse for proactive care planning and outreach scheduling.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Upcoming gaps retrieved", content = @Content(mediaType = "application/json"))})
    @PreAuthorize("hasPermission(null, 'CARE_GAP_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/upcoming", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CareGapEntity>> getUpcomingGaps(
            @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true) @RequestParam("patient") UUID patientId,
            @Parameter(description = "Days to look ahead", example = "30") @RequestParam(value = "days", defaultValue = "30") int days
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
    @Operation(summary = "Get care gap statistics", description = "Returns aggregated statistics (open count, closed count, etc.).\n\nUse for dashboard KPIs and patient overview metrics.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Statistics retrieved", content = @Content(mediaType = "application/json"))})
    @PreAuthorize("hasPermission(null, 'CARE_GAP_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CareGapIdentificationService.CareGapStats> getCareGapStats(
            @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true) @RequestParam("patient") UUID patientId
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
    @Operation(summary = "Get care gap summary", description = "Returns comprehensive summary with counts by status, priority, category.\n\nUse for patient overview dashboards and care team coordination.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Summary retrieved", content = @Content(mediaType = "application/json"))})
    @PreAuthorize("hasPermission(null, 'CARE_GAP_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CareGapReportService.CareGapSummary> getCareGapSummary(
            @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true) @RequestParam("patient") UUID patientId
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
    @Operation(summary = "Get gaps grouped by measure category", description = "Returns gap counts by HEDIS/CMS category.\n\nUse for quality measure domain analysis and focused intervention planning.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Category breakdown retrieved", content = @Content(mediaType = "application/json"))})
    @PreAuthorize("hasPermission(null, 'CARE_GAP_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/by-category", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> getGapsByCategory(
            @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true) @RequestParam("patient") UUID patientId
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
    @Operation(summary = "Get gaps grouped by priority", description = "Returns gap counts by priority level.\n\nUse for workload distribution and resource allocation.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Priority breakdown retrieved", content = @Content(mediaType = "application/json"))})
    @PreAuthorize("hasPermission(null, 'CARE_GAP_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/by-priority", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> getGapsByPriority(
            @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true) @RequestParam("patient") UUID patientId
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
    @Operation(summary = "Get population-level gap report", description = "Returns tenant-wide gap statistics for quality reporting.\n\nUse for HEDIS/Stars reporting, population health management, and ACO quality submissions.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Population report retrieved", content = @Content(mediaType = "application/json"))})
    @PreAuthorize("hasPermission(null, 'CARE_GAP_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/population-report", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CareGapReportService.PopulationGapReport> getPopulationGapReport(
            @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        log.info("GET /care-gap/population-report - tenant: {}", tenantId);

        CareGapReportService.PopulationGapReport report =
                reportService.getPopulationGapReport(tenantId);

        return ResponseEntity.ok(report);
    }

    // ==================== Bulk Operations Endpoints (Issue #241) ====================

    /**
     * Bulk close multiple care gaps
     *
     * Issue #241: Care Gap Bulk Actions
     * Closes multiple care gaps in a single operation with shared closure reason and notes.
     * Returns detailed success/failure information for each gap.
     *
     * @param tenantId Tenant ID (from header)
     * @param request Bulk closure request
     * @return Bulk operation response with success/failure counts
     */
    @Operation(summary = "Bulk close multiple care gaps", description = "Closes multiple gaps with shared closure reason (Issue #241).\n\nReturns success/failure counts for each gap. Use for mass care gap closure workflows.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Bulk closure completed", content = @Content(mediaType = "application/json"))})
    @PreAuthorize("hasPermission(null, 'CARE_GAP_WRITE')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/bulk-close", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<com.healthdata.caregap.dto.BulkOperationResponse> bulkCloseCareGaps(
            @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Bulk closure request with gap IDs and closure details") @RequestBody @jakarta.validation.Valid com.healthdata.caregap.dto.BulkClosureRequest request
    ) {
        log.info("POST /care-gap/bulk-close - tenant: {}, gaps: {}, closedBy: {}",
                tenantId, request.getGapIds().size(), request.getClosedBy());

        com.healthdata.caregap.dto.BulkOperationResponse response =
                identificationService.bulkCloseCareGaps(tenantId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Bulk assign intervention to multiple care gaps
     *
     * Issue #241: Care Gap Bulk Actions
     * Assigns the same intervention to multiple care gaps in a single operation.
     *
     * @param tenantId Tenant ID (from header)
     * @param request Bulk intervention request
     * @return Bulk operation response with success/failure counts
     */
    @Operation(summary = "Bulk assign intervention", description = "Assigns intervention to multiple gaps (Issue #241).\n\nStreamlines provider workflows for care coordination and intervention assignment.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Bulk assignment completed", content = @Content(mediaType = "application/json"))})
    @PreAuthorize("hasPermission(null, 'CARE_GAP_WRITE')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/bulk-assign-intervention", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<com.healthdata.caregap.dto.BulkOperationResponse> bulkAssignIntervention(
            @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Bulk intervention request with gap IDs and intervention type") @RequestBody @jakarta.validation.Valid com.healthdata.caregap.dto.BulkInterventionRequest request
    ) {
        log.info("POST /care-gap/bulk-assign-intervention - tenant: {}, gaps: {}, type: {}",
                tenantId, request.getGapIds().size(), request.getInterventionType());

        com.healthdata.caregap.dto.BulkOperationResponse response =
                identificationService.bulkAssignIntervention(tenantId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Bulk update priority for multiple care gaps
     *
     * Issue #241: Care Gap Bulk Actions
     * Updates priority level for multiple care gaps in a single operation.
     *
     * @param tenantId Tenant ID (from header)
     * @param request Bulk priority update request
     * @return Bulk operation response with success/failure counts
     */
    @Operation(summary = "Bulk update priority", description = "Updates priority for multiple gaps (Issue #241).\n\nUse for mass prioritization based on clinical criteria or organizational goals.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Bulk update completed", content = @Content(mediaType = "application/json"))})
    @PreAuthorize("hasPermission(null, 'CARE_GAP_WRITE')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping(value = "/bulk-update-priority", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<com.healthdata.caregap.dto.BulkOperationResponse> bulkUpdatePriority(
            @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Bulk priority update request with gap IDs and new priority") @RequestBody @jakarta.validation.Valid com.healthdata.caregap.dto.BulkPriorityUpdateRequest request
    ) {
        log.info("PUT /care-gap/bulk-update-priority - tenant: {}, gaps: {}, priority: {}",
                tenantId, request.getGapIds().size(), request.getPriority());

        com.healthdata.caregap.dto.BulkOperationResponse response =
                identificationService.bulkUpdatePriority(tenantId, request);

        return ResponseEntity.ok(response);
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
    @Operation(summary = "Get prioritized gaps for provider", description = "Returns provider's gaps sorted by scoring algorithm (Issue #138).\n\nScoring: urgency (40%) + due date (30%) + ease (30%).\nIncludes recommended actions for efficient workflows.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Prioritized gaps retrieved", content = @Content(mediaType = "application/json"))})
    @PreAuthorize("hasPermission(null, 'CARE_GAP_WRITE')")
    @Audited(action = AuditAction.READ, resourceType = "CareGap", purposeOfUse = "TREATMENT",
            description = "Provider care gap prioritization lookup")
    @GetMapping(value = "/providers/{providerId}/prioritized", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProviderCareGapPrioritizationService.PrioritizedCareGap>> getProviderPrioritizedGaps(
            @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Provider ID (UUID)", required = true, example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable("providerId") String providerId,
            @Parameter(description = "Maximum gaps to return", example = "50") @RequestParam(value = "limit", defaultValue = "50") int limit
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
    @Operation(summary = "Get provider gap summary", description = "Returns summary statistics for provider's patient panel.\n\nUse for provider dashboards, panel management, and performance tracking.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Provider summary retrieved", content = @Content(mediaType = "application/json"))})
    @PreAuthorize("hasPermission(null, 'CARE_GAP_WRITE')")
    @Audited(action = AuditAction.READ, resourceType = "CareGap", purposeOfUse = "TREATMENT",
            description = "Provider care gap summary lookup")
    @GetMapping(value = "/providers/{providerId}/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProviderCareGapPrioritizationService.ProviderGapSummary> getProviderGapSummary(
            @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Provider ID (UUID)", required = true, example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable("providerId") String providerId
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
    @Operation(summary = "Service health check", description = "Returns service availability status.\n\nUse for monitoring, load balancer health probes, and readiness checks.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Service is healthy", content = @Content(mediaType = "application/json"))})
    @PreAuthorize("hasPermission(null, 'CARE_GAP_READ')")
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
