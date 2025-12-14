package com.healthdata.fhir.bulk;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FHIR Bulk Data Export API Controller
 *
 * Implements the FHIR Bulk Data Access specification endpoints:
 * - System-level export: GET /fhir/$export
 * - Patient-level export: GET /fhir/Patient/$export
 * - Group-level export: GET /fhir/Group/{id}/$export
 * - Status polling: GET /fhir/$export-poll-status/{jobId}
 * - Job cancellation: DELETE /fhir/$export-poll-status/{jobId}
 * - File download: GET /fhir/download/{jobId}/{fileName}
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Bulk Export", description = "Async bulk data export operations following FHIR Bulk Data Access specification")
@SecurityRequirement(name = "smart-oauth2")
@Slf4j
public class BulkExportController {

    private final BulkExportService exportService;

    public BulkExportController(BulkExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * System-level bulk export
     * GET /fhir/$export
     */
    @GetMapping("/$export")
    @Operation(summary = "System-level bulk export", description = "Export all data for the tenant")
    @ApiResponse(responseCode = "202", description = "Export job initiated")
    @ApiResponse(responseCode = "429", description = "Too many concurrent exports")
    public ResponseEntity<Void> systemExport(
            @Parameter(description = "Resource types to export (comma-separated)")
            @RequestParam(name = "_type", required = false) String type,
            @Parameter(description = "Only export resources modified since this timestamp")
            @RequestParam(name = "_since", required = false) String since,
            @Parameter(description = "FHIR search parameters for filtering")
            @RequestParam(name = "_typeFilter", required = false) String typeFilter,
            @RequestHeader(name = "Prefer", required = false) String prefer) {

        String tenantId = getTenantId();
        String userId = getUserId();

        List<String> resourceTypes = type != null ? Arrays.asList(type.split(",")) : null;
        Instant sinceParam = parseSinceParam(since);
        List<String> typeFilters = typeFilter != null ? Arrays.asList(typeFilter.split(",")) : null;

        String requestUrl = "/fhir/$export" + buildQueryString(type, since, typeFilter);

        try {
            UUID jobId = exportService.kickOffExport(
                tenantId,
                BulkExportJob.ExportLevel.SYSTEM,
                null,
                resourceTypes,
                sinceParam,
                typeFilters,
                requestUrl,
                userId
            );

            String statusUrl = "/fhir/$export-poll-status/" + jobId;

            return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .header(HttpHeaders.CONTENT_LOCATION, statusUrl)
                .build();

        } catch (BulkExportService.ExportLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
    }

    /**
     * Patient-level bulk export
     * GET /fhir/Patient/$export
     */
    @GetMapping("/Patient/$export")
    @Operation(summary = "Patient-level bulk export", description = "Export all Patient resources and related data")
    @ApiResponse(responseCode = "202", description = "Export job initiated")
    public ResponseEntity<Void> patientExport(
            @RequestParam(name = "_type", required = false) String type,
            @RequestParam(name = "_since", required = false) String since,
            @RequestParam(name = "_typeFilter", required = false) String typeFilter) {

        String tenantId = getTenantId();
        String userId = getUserId();

        List<String> resourceTypes = type != null ? Arrays.asList(type.split(",")) : null;
        Instant sinceParam = parseSinceParam(since);
        List<String> typeFilters = typeFilter != null ? Arrays.asList(typeFilter.split(",")) : null;

        String requestUrl = "/fhir/Patient/$export" + buildQueryString(type, since, typeFilter);

        try {
            UUID jobId = exportService.kickOffExport(
                tenantId,
                BulkExportJob.ExportLevel.PATIENT,
                null,
                resourceTypes,
                sinceParam,
                typeFilters,
                requestUrl,
                userId
            );

            String statusUrl = "/fhir/$export-poll-status/" + jobId;

            return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .header(HttpHeaders.CONTENT_LOCATION, statusUrl)
                .build();

        } catch (BulkExportService.ExportLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
    }

    /**
     * Group-level bulk export
     * GET /fhir/Group/{id}/$export
     */
    @GetMapping("/Group/{id}/$export")
    @Operation(summary = "Group-level bulk export", description = "Export data for a specific group")
    @ApiResponse(responseCode = "202", description = "Export job initiated")
    public ResponseEntity<Void> groupExport(
            @PathVariable String id,
            @RequestParam(name = "_type", required = false) String type,
            @RequestParam(name = "_since", required = false) String since,
            @RequestParam(name = "_typeFilter", required = false) String typeFilter) {

        String tenantId = getTenantId();
        String userId = getUserId();

        List<String> resourceTypes = type != null ? Arrays.asList(type.split(",")) : null;
        Instant sinceParam = parseSinceParam(since);
        List<String> typeFilters = typeFilter != null ? Arrays.asList(typeFilter.split(",")) : null;

        String requestUrl = "/fhir/Group/" + id + "/$export" + buildQueryString(type, since, typeFilter);

        try {
            UUID jobId = exportService.kickOffExport(
                tenantId,
                BulkExportJob.ExportLevel.GROUP,
                id,
                resourceTypes,
                sinceParam,
                typeFilters,
                requestUrl,
                userId
            );

            String statusUrl = "/fhir/$export-poll-status/" + jobId;

            return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .header(HttpHeaders.CONTENT_LOCATION, statusUrl)
                .build();

        } catch (BulkExportService.ExportLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
    }

    /**
     * Poll export job status
     * GET /fhir/$export-poll-status/{jobId}
     */
    @GetMapping("/$export-poll-status/{jobId}")
    @Operation(summary = "Poll export job status", description = "Check status of bulk export job")
    @ApiResponse(responseCode = "202", description = "Export still in progress")
    @ApiResponse(responseCode = "200", description = "Export completed, manifest returned")
    @ApiResponse(responseCode = "404", description = "Export job not found")
    public ResponseEntity<?> pollStatus(@PathVariable String jobId) {
        String tenantId = getTenantId();

        try {
            UUID uuid = UUID.fromString(jobId);
            Optional<BulkExportJob> jobOpt = exportService.getJobStatus(tenantId, uuid);

            if (jobOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            BulkExportJob job = jobOpt.get();

            return switch (job.getStatus()) {
                case PENDING, IN_PROGRESS -> ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .header("X-Progress", job.getStatus().name())
                    .build();

                case COMPLETED -> {
                    BulkExportService.ExportManifest manifest = exportService.buildManifest(job);
                    yield ResponseEntity
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(manifest);
                }

                case FAILED -> ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Export failed: " + job.getErrorMessage()));

                case CANCELLED -> ResponseEntity
                    .status(HttpStatus.GONE)
                    .body(new ErrorResponse("Export was cancelled"));
            };

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid job ID format"));
        }
    }

    /**
     * Cancel export job
     * DELETE /fhir/$export-poll-status/{jobId}
     */
    @DeleteMapping("/$export-poll-status/{jobId}")
    @Operation(summary = "Cancel export job", description = "Cancel a running bulk export job")
    @ApiResponse(responseCode = "202", description = "Export job cancelled")
    @ApiResponse(responseCode = "404", description = "Export job not found")
    public ResponseEntity<Void> cancelExport(@PathVariable String jobId) {
        String tenantId = getTenantId();
        String userId = getUserId();

        try {
            UUID uuid = UUID.fromString(jobId);
            exportService.cancelJob(tenantId, uuid, userId);

            return ResponseEntity.status(HttpStatus.ACCEPTED).build();

        } catch (BulkExportService.ExportJobNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * Download export file
     * GET /fhir/download/{jobId}/{fileName}
     */
    @GetMapping("/download/{jobId}/{fileName}")
    @Operation(summary = "Download export file", description = "Download a specific export file")
    @ApiResponse(responseCode = "200", description = "File download")
    @ApiResponse(responseCode = "404", description = "File not found")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String jobId,
            @PathVariable String fileName) {

        String tenantId = getTenantId();

        try {
            UUID uuid = UUID.fromString(jobId);
            Optional<BulkExportJob> jobOpt = exportService.getJobStatus(tenantId, uuid);

            if (jobOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            BulkExportJob job = jobOpt.get();

            // Find the file in output files
            Optional<BulkExportJob.OutputFile> outputFile = job.getOutputFiles().stream()
                .filter(f -> f.getUrl().endsWith(fileName))
                .findFirst();

            if (outputFile.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            File file = new File(outputFile.get().getFilePath());
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/fhir+ndjson"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Parse _since parameter
     */
    private Instant parseSinceParam(String since) {
        if (since == null || since.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(since);
        } catch (DateTimeParseException e) {
            log.warn("Invalid _since parameter: {}", since);
            return null;
        }
    }

    /**
     * Build query string for request URL
     */
    private String buildQueryString(String type, String since, String typeFilter) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        if (type != null) {
            sb.append("?_type=").append(type);
            first = false;
        }

        if (since != null) {
            sb.append(first ? "?" : "&").append("_since=").append(since);
            first = false;
        }

        if (typeFilter != null) {
            sb.append(first ? "?" : "&").append("_typeFilter=").append(typeFilter);
        }

        return sb.toString();
    }

    /**
     * Error response
     */
    record ErrorResponse(String message) {}

    /**
     * Get tenant ID from security context
     */
    private String getTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> details = (java.util.Map<String, Object>) auth.getDetails();
            return (String) details.getOrDefault("tenantId", "default");
        }
        return "default";
    }

    /**
     * Get user ID from security context
     */
    private String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
