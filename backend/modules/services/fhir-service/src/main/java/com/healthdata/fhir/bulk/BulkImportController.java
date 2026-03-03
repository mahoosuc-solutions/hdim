package com.healthdata.fhir.bulk;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * FHIR Bulk Data Import API Controller.
 *
 * Mirrors the BulkExportController async pattern:
 * - POST /$import — submit NDJSON file, returns 202 + Content-Location
 * - GET /$import-poll-status/{jobId} — poll progress
 *
 * NDJSON files are streamed line-by-line (never loaded fully into memory)
 * and batched into 200-resource transactions via BundleTransactionService.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Bulk Import", description = "Async bulk NDJSON import operations")
@SecurityRequirement(name = "smart-oauth2")
@Slf4j
public class BulkImportController {

    private final BulkImportService importService;

    public BulkImportController(BulkImportService importService) {
        this.importService = importService;
    }

    @Operation(
        summary = "Initiate bulk NDJSON import",
        description = "Accepts a multipart NDJSON file containing one FHIR resource per line. "
            + "Returns 202 Accepted with a Content-Location header for status polling. "
            + "The file is streamed line-by-line — large files (millions of records) are supported."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Import job initiated — poll Content-Location for status"),
        @ApiResponse(responseCode = "400", description = "No file provided or invalid format"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden — insufficient permissions"),
        @ApiResponse(responseCode = "429", description = "Too many concurrent imports")
    })
    @PostMapping(value = "/$import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Audited(
        action = AuditAction.CREATE,
        resourceType = "BulkImport",
        purposeOfUse = "OPERATIONS",
        description = "Initiate bulk NDJSON import"
    )
    public ResponseEntity<Void> initiateImport(
            @Parameter(description = "NDJSON file with one FHIR resource per line")
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String tenantId = getTenantId();
        String userId = getUserId();

        try {
            UUID jobId = importService.initiateImport(tenantId, userId);

            // Start async processing
            importService.processNdjsonStream(jobId, tenantId, file.getInputStream());

            String statusUrl = "/fhir/$import-poll-status/" + jobId;

            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .header(HttpHeaders.CONTENT_LOCATION, statusUrl)
                    .build();

        } catch (BulkImportService.ImportLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        } catch (Exception e) {
            log.error("Failed to initiate bulk import", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Poll import job status",
        description = "Returns the current status and progress of a bulk import job."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Import completed — progress returned"),
        @ApiResponse(responseCode = "202", description = "Import still in progress"),
        @ApiResponse(responseCode = "404", description = "Import job not found")
    })
    @GetMapping("/$import-poll-status/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST', 'VIEWER')")
    public ResponseEntity<?> pollStatus(
            @Parameter(description = "Import job ID", required = true)
            @PathVariable String jobId) {

        String tenantId = getTenantId();

        try {
            UUID uuid = UUID.fromString(jobId);
            Optional<BulkImportJob> jobOpt = importService.getJobStatus(tenantId, uuid);

            if (jobOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            BulkImportJob job = jobOpt.get();

            Map<String, Object> statusResponse = Map.of(
                    "jobId", job.getJobId().toString(),
                    "status", job.getStatus().name(),
                    "totalRecords", job.getTotalRecords() != null ? job.getTotalRecords() : 0,
                    "processedRecords", job.getProcessedRecords() != null ? job.getProcessedRecords() : 0,
                    "failedRecords", job.getFailedRecords() != null ? job.getFailedRecords() : 0,
                    "submittedAt", job.getSubmittedAt().toString()
            );

            return switch (job.getStatus()) {
                case PENDING, IN_PROGRESS -> ResponseEntity
                        .status(HttpStatus.ACCEPTED)
                        .header("X-Progress", job.getStatus().name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(statusResponse);

                case COMPLETED -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(statusResponse);

                case FAILED -> ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "jobId", job.getJobId().toString(),
                                "status", "FAILED",
                                "error", job.getErrorSummary() != null ? job.getErrorSummary() : "Unknown error",
                                "processedRecords", job.getProcessedRecords() != null ? job.getProcessedRecords() : 0,
                                "failedRecords", job.getFailedRecords() != null ? job.getFailedRecords() : 0
                        ));

                case CANCELLED -> ResponseEntity
                        .status(HttpStatus.GONE)
                        .body(Map.of("status", "CANCELLED"));
            };

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid job ID format"));
        }
    }

    private String getTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> details = (java.util.Map<String, Object>) auth.getDetails();
            return (String) details.getOrDefault("tenantId", "default");
        }
        return "default";
    }

    private String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
