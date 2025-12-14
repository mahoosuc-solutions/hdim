package com.healthdata.migration.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthdata.migration.dto.DataQualityReport;
import com.healthdata.migration.dto.JobStatus;
import com.healthdata.migration.dto.MigrationJobRequest;
import com.healthdata.migration.dto.MigrationJobResponse;
import com.healthdata.migration.dto.MigrationProgress;
import com.healthdata.migration.dto.MigrationSummary;
import com.healthdata.migration.persistence.MigrationErrorEntity;
import com.healthdata.migration.repository.MigrationErrorRepository;
import com.healthdata.migration.service.MigrationJobService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller for migration job management
 */
@RestController
@RequestMapping("/api/v1/migrations")
@RequiredArgsConstructor
@Tag(name = "Migration Jobs", description = "Endpoints for managing data migration jobs")
public class MigrationJobController {

    private final MigrationJobService jobService;
    private final MigrationErrorRepository errorRepository;

    @PostMapping
    @Operation(summary = "Create a new migration job")
    public ResponseEntity<MigrationJobResponse> createJob(
            @Valid @RequestBody MigrationJobRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        request.setTenantId(tenantId);
        MigrationJobResponse response = jobService.createJob(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List migration jobs")
    public ResponseEntity<Page<MigrationJobResponse>> listJobs(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Filter by status") @RequestParam(required = false) JobStatus status,
            @Parameter(description = "Filter by name") @RequestParam(required = false) String name,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<MigrationJobResponse> jobs = jobService.listJobs(tenantId, status, name, pageable);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get a migration job by ID")
    public ResponseEntity<MigrationJobResponse> getJob(
            @PathVariable UUID jobId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        MigrationJobResponse job = jobService.getJob(jobId, tenantId);
        return ResponseEntity.ok(job);
    }

    @PostMapping("/{jobId}/start")
    @Operation(summary = "Start a migration job")
    public ResponseEntity<MigrationJobResponse> startJob(
            @PathVariable UUID jobId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        MigrationJobResponse job = jobService.startJob(jobId, tenantId);
        return ResponseEntity.ok(job);
    }

    @PostMapping("/{jobId}/pause")
    @Operation(summary = "Pause a running migration job")
    public ResponseEntity<MigrationJobResponse> pauseJob(
            @PathVariable UUID jobId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        MigrationJobResponse job = jobService.pauseJob(jobId, tenantId);
        return ResponseEntity.ok(job);
    }

    @PostMapping("/{jobId}/resume")
    @Operation(summary = "Resume a paused migration job")
    public ResponseEntity<MigrationJobResponse> resumeJob(
            @PathVariable UUID jobId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        MigrationJobResponse job = jobService.resumeJob(jobId, tenantId);
        return ResponseEntity.ok(job);
    }

    @PostMapping("/{jobId}/cancel")
    @Operation(summary = "Cancel a migration job")
    public ResponseEntity<MigrationJobResponse> cancelJob(
            @PathVariable UUID jobId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        MigrationJobResponse job = jobService.cancelJob(jobId, tenantId);
        return ResponseEntity.ok(job);
    }

    @DeleteMapping("/{jobId}")
    @Operation(summary = "Delete a migration job")
    public ResponseEntity<Void> deleteJob(
            @PathVariable UUID jobId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        jobService.deleteJob(jobId, tenantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{jobId}/progress")
    @Operation(summary = "Get current progress of a migration job")
    public ResponseEntity<MigrationProgress> getProgress(
            @PathVariable UUID jobId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        MigrationProgress progress = jobService.getProgress(jobId, tenantId);
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/{jobId}/summary")
    @Operation(summary = "Get summary of a completed migration job")
    public ResponseEntity<MigrationSummary> getSummary(
            @PathVariable UUID jobId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        MigrationSummary summary = jobService.getSummary(jobId, tenantId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{jobId}/errors")
    @Operation(summary = "Get errors for a migration job")
    public ResponseEntity<Page<MigrationErrorEntity>> getErrors(
            @PathVariable UUID jobId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Filter by error category")
            @RequestParam(required = false) String category,
            @Parameter(description = "Search in error messages")
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {

        // Verify job belongs to tenant
        jobService.getJob(jobId, tenantId);

        Page<MigrationErrorEntity> errors;
        if (category != null || search != null) {
            errors = errorRepository.findByJobIdWithFilters(
                    jobId,
                    category != null ? com.healthdata.migration.dto.MigrationErrorCategory.valueOf(category) : null,
                    search,
                    pageable);
        } else {
            errors = errorRepository.findByJobId(jobId, pageable);
        }

        return ResponseEntity.ok(errors);
    }

    @GetMapping("/{jobId}/quality")
    @Operation(summary = "Get data quality report for a migration job")
    public ResponseEntity<DataQualityReport> getQualityReport(
            @PathVariable UUID jobId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        DataQualityReport report = jobService.getQualityReport(jobId, tenantId);
        return ResponseEntity.ok(report);
    }

    @GetMapping(value = "/{jobId}/quality/export", produces = "text/csv")
    @Operation(summary = "Export data quality report as CSV")
    public ResponseEntity<String> exportQualityReport(
            @PathVariable UUID jobId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        String csv = jobService.exportQualityReportCsv(jobId, tenantId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=quality-report-" + jobId + ".csv")
                .body(csv);
    }

    @GetMapping(value = "/{jobId}/errors/export", produces = "text/csv")
    @Operation(summary = "Export errors as CSV")
    public ResponseEntity<String> exportErrors(
            @PathVariable UUID jobId,
            @RequestHeader("X-Tenant-ID") String tenantId) throws java.io.IOException {

        String csv = jobService.exportErrorsCsv(jobId, tenantId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=errors-" + jobId + ".csv")
                .body(csv);
    }
}
