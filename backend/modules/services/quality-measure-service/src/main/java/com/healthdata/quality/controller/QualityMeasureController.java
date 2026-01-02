package com.healthdata.quality.controller;

import com.healthdata.quality.dto.QualityMeasureResultDTO;
import com.healthdata.quality.dto.QualityMeasureResultMapper;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.SavedReportEntity;
import com.healthdata.quality.service.MeasureCalculationService;
import com.healthdata.quality.service.PopulationCalculationService;
import com.healthdata.quality.service.QualityReportService;
import com.healthdata.quality.service.ReportExportService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quality Measure Controller - REST API for HEDIS quality measures
 * Endpoints: /quality-measure/calculate, /quality-measure/results, /quality-measure/score, etc.
 */
@RestController
@RequestMapping("/quality-measure")
@RequiredArgsConstructor
@Slf4j
@Validated
public class QualityMeasureController {

    private final MeasureCalculationService calculationService;
    private final PopulationCalculationService populationCalculationService;
    private final QualityReportService reportService;
    private final ReportExportService exportService;

    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/calculate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QualityMeasureResultDTO> calculateMeasure(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestParam("patient") @NotNull(message = "Patient ID is required") UUID patientId,
            @RequestParam("measure") @NotBlank(message = "Measure ID is required") String measureId,
            @RequestParam(value = "createdBy", defaultValue = "system") String createdBy
    ) {
        log.info("POST /quality-measure/calculate - patient: {}, measure: {}", patientId, measureId);
        QualityMeasureResultEntity result = calculationService.calculateMeasure(tenantId, patientId, measureId, createdBy);
        QualityMeasureResultDTO dto = QualityMeasureResultMapper.toDTO(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/results", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<QualityMeasureResultDTO>> getPatientResults(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestParam(value = "patient", required = false) UUID patientId,
            @RequestParam(value = "page", required = false, defaultValue = "0") @PositiveOrZero Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") @PositiveOrZero Integer size
    ) {
        if (patientId != null) {
            log.info("GET /quality-measure/results - patient: {}", patientId);
            List<QualityMeasureResultEntity> results = calculationService.getPatientMeasureResults(tenantId, patientId);
            List<QualityMeasureResultDTO> dtos = QualityMeasureResultMapper.toDTOList(results);
            return ResponseEntity.ok(dtos);
        } else {
            log.info("GET /quality-measure/results - all results for tenant: {} (page: {}, size: {})", tenantId, page, size);
            List<QualityMeasureResultEntity> results = calculationService.getAllMeasureResults(tenantId, page, size);
            List<QualityMeasureResultDTO> dtos = QualityMeasureResultMapper.toDTOList(results);
            return ResponseEntity.ok(dtos);
        }
    }

    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/score", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MeasureCalculationService.QualityScore> getQualityScore(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestParam("patient") @NotNull(message = "Patient ID is required") UUID patientId
    ) {
        log.info("GET /quality-measure/score - patient: {}", patientId);
        return ResponseEntity.ok(calculationService.getQualityScore(tenantId, patientId));
    }

    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/report/patient", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QualityReportService.QualityReport> getPatientQualityReport(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestParam("patient") @NotNull(message = "Patient ID is required") UUID patientId
    ) {
        log.info("GET /quality-measure/report/patient - patient: {}", patientId);
        return ResponseEntity.ok(reportService.getPatientQualityReport(tenantId, patientId));
    }

    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/report/population", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QualityReportService.PopulationQualityReport> getPopulationQualityReport(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestParam(value = "year", required = false) Integer year
    ) {
        // Validate year is positive if provided
        if (year != null && year <= 0) {
            throw new IllegalArgumentException("Year must be a positive number");
        }
        int reportYear = year != null ? year : LocalDate.now().getYear();
        log.info("GET /quality-measure/report/population - year: {}", reportYear);
        return ResponseEntity.ok(reportService.getPopulationQualityReport(tenantId, reportYear));
    }

    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/_health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "quality-measure-service", "timestamp", LocalDate.now().toString()));
    }

    // ===== BATCH POPULATION CALCULATION ENDPOINTS =====

    /**
     * Start batch calculation for all measures across the entire patient population
     * This will calculate all HEDIS measures for all patients in the FHIR server
     *
     * @param tenantId Tenant ID
     * @param fhirServerUrl FHIR server base URL (optional, defaults to configured URL)
     * @param createdBy User who triggered the calculation
     * @return Job ID for tracking progress
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/population/calculate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> startPopulationCalculation(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestParam(value = "fhirServerUrl", required = false, defaultValue = "http://fhir-service-mock:8080/fhir") String fhirServerUrl,
            @RequestParam(value = "createdBy", defaultValue = "system") String createdBy
    ) {
        log.info("POST /quality-measure/population/calculate - Starting batch calculation for tenant: {}", tenantId);

        try {
            java.util.concurrent.CompletableFuture<String> jobFuture =
                populationCalculationService.calculateAllMeasuresForPopulation(tenantId, fhirServerUrl, createdBy);

            String jobId = jobFuture.get(); // Get the job ID immediately

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "jobId", jobId,
                "status", "STARTED",
                "message", "Population calculation job started. Use /population/jobs/" + jobId + " to track progress.",
                "tenantId", tenantId
            ));
        } catch (Exception e) {
            log.error("Failed to start population calculation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to start population calculation",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get status of a specific batch calculation job
     *
     * @param tenantId Tenant ID
     * @param jobId Job ID
     * @return Job status details including progress, counts, and errors
     */
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/population/jobs/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getJobStatus(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable("jobId") String jobId
    ) {
        log.info("GET /quality-measure/population/jobs/{} - tenant: {}", jobId, tenantId);

        PopulationCalculationService.BatchCalculationJob job = populationCalculationService.getJobStatus(jobId);

        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Job not found",
                "jobId", jobId
            ));
        }

        // Verify tenant matches
        if (!job.getTenantId().equals(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "Access denied to this job"
            ));
        }

        // Build response map (using HashMap since we have more than 10 fields)
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("jobId", job.getJobId());
        response.put("tenantId", job.getTenantId());
        response.put("status", job.getStatus().toString());
        response.put("createdBy", job.getCreatedBy());
        response.put("startedAt", job.getStartedAt().toString());
        response.put("completedAt", job.getCompletedAt() != null ? job.getCompletedAt().toString() : null);
        response.put("totalPatients", job.getTotalPatients());
        response.put("totalMeasures", job.getTotalMeasures());
        response.put("totalCalculations", job.getTotalCalculations());
        response.put("completedCalculations", job.getCompletedCalculations());
        response.put("successfulCalculations", job.getSuccessfulCalculations());
        response.put("failedCalculations", job.getFailedCalculations());
        response.put("progressPercent", job.getProgressPercent());
        response.put("duration", job.getDuration().toString());
        response.put("errors", job.getErrors());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all batch calculation jobs for a tenant
     *
     * @param tenantId Tenant ID
     * @return List of all jobs (active and completed) for the tenant
     */
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/population/jobs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getAllJobs(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId
    ) {
        log.info("GET /quality-measure/population/jobs - tenant: {}", tenantId);

        List<PopulationCalculationService.BatchCalculationJob> jobs =
            populationCalculationService.getActiveJobs(tenantId);

        List<Map<String, Object>> jobList = jobs.stream()
            .map(job -> {
                Map<String, Object> jobMap = new java.util.HashMap<>();
                jobMap.put("jobId", job.getJobId());
                jobMap.put("status", job.getStatus().toString());
                jobMap.put("createdBy", job.getCreatedBy());
                jobMap.put("startedAt", job.getStartedAt().toString());
                jobMap.put("completedAt", job.getCompletedAt() != null ? job.getCompletedAt().toString() : null);
                jobMap.put("totalPatients", job.getTotalPatients());
                jobMap.put("totalMeasures", job.getTotalMeasures());
                jobMap.put("totalCalculations", job.getTotalCalculations());
                jobMap.put("completedCalculations", job.getCompletedCalculations());
                jobMap.put("successfulCalculations", job.getSuccessfulCalculations());
                jobMap.put("failedCalculations", job.getFailedCalculations());
                jobMap.put("progressPercent", job.getProgressPercent());
                jobMap.put("duration", job.getDuration().toString());
                return jobMap;
            })
            .toList();

        return ResponseEntity.ok(jobList);
    }

    /**
     * Cancel a running batch calculation job
     *
     * @param tenantId Tenant ID
     * @param jobId Job ID to cancel
     * @return Success/failure response
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/population/jobs/{jobId}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> cancelJob(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable("jobId") String jobId
    ) {
        log.info("POST /quality-measure/population/jobs/{}/cancel - tenant: {}", jobId, tenantId);

        PopulationCalculationService.BatchCalculationJob job = populationCalculationService.getJobStatus(jobId);

        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Job not found",
                "jobId", jobId
            ));
        }

        // Verify tenant matches
        if (!job.getTenantId().equals(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "Access denied to this job"
            ));
        }

        boolean cancelled = populationCalculationService.cancelJob(jobId);

        if (cancelled) {
            return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "status", "CANCELLED",
                "message", "Job successfully cancelled"
            ));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Job cannot be cancelled",
                "message", "Job is not in CALCULATING status",
                "currentStatus", job.getStatus().toString()
            ));
        }
    }

    // ===== NEW: Saved Reports Endpoints =====

    /**
     * Save patient quality report
     */
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/report/patient/save", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SavedReportEntity> savePatientReport(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestParam("patient") @NotNull(message = "Patient ID is required") UUID patientId,
            @RequestParam("name") @NotBlank(message = "Report name is required") String reportName,
            @RequestParam(value = "createdBy", defaultValue = "system") String createdBy
    ) {
        log.info("POST /quality-measure/report/patient/save - patient: {}, name: {}", patientId, reportName);
        SavedReportEntity savedReport = reportService.savePatientReport(tenantId, patientId, reportName, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedReport);
    }

    /**
     * Save population quality report
     */
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/report/population/save", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SavedReportEntity> savePopulationReport(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestParam("year") Integer year,
            @RequestParam("name") @NotBlank(message = "Report name is required") String reportName,
            @RequestParam(value = "createdBy", defaultValue = "system") String createdBy
    ) {
        // Validate year is positive (but allow edge cases for testing)
        if (year == null || year <= 0) {
            throw new IllegalArgumentException("Year must be a positive number");
        }
        log.info("POST /quality-measure/report/population/save - year: {}, name: {}", year, reportName);
        SavedReportEntity savedReport = reportService.savePopulationReport(tenantId, year, reportName, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedReport);
    }

    /**
     * Get all saved reports for a tenant (optionally filtered by type)
     */
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/reports", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SavedReportEntity>> getSavedReports(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestParam(value = "type", required = false) String reportType
    ) {
        log.info("GET /quality-measure/reports - tenant: {}, type: {}", tenantId, reportType);
        List<SavedReportEntity> reports;
        if (reportType != null && !reportType.isBlank()) {
            reports = reportService.getSavedReportsByType(tenantId, reportType);
        } else {
            reports = reportService.getSavedReports(tenantId);
        }
        return ResponseEntity.ok(reports);
    }

    /**
     * Get a saved report by ID
     */
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/reports/{reportId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SavedReportEntity> getSavedReport(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable("reportId") String reportId
    ) {
        log.info("GET /quality-measure/reports/{} - tenant: {}", reportId, tenantId);
        try {
            UUID uuid = UUID.fromString(reportId);
            SavedReportEntity report = reportService.getSavedReport(tenantId, uuid);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid report ID format: " + reportId);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    /**
     * Delete a saved report
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping(value = "/reports/{reportId}")
    public ResponseEntity<Void> deleteSavedReport(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable("reportId") String reportId
    ) {
        log.info("DELETE /quality-measure/reports/{} - tenant: {}", reportId, tenantId);
        try {
            UUID uuid = UUID.fromString(reportId);
            reportService.deleteSavedReport(tenantId, uuid);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid report ID format: " + reportId);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    // ===== EXPORT ENDPOINTS =====

    /**
     * Export report to CSV
     */
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/reports/{reportId}/export/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportReportToCsv(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable("reportId") String reportId
    ) {
        log.info("GET /quality-measure/reports/{}/export/csv - tenant: {}", reportId, tenantId);
        try {
            UUID uuid = UUID.fromString(reportId);
            SavedReportEntity report = reportService.getSavedReport(tenantId, uuid);

            byte[] csvData = exportService.exportToCsv(report);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", sanitizeFilename(report.getReportName()) + ".csv");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvData);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid report ID format: " + reportId);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        } catch (IOException e) {
            log.error("Error exporting report to CSV: {}", e.getMessage());
            throw new RuntimeException("Failed to export report to CSV", e);
        }
    }

    /**
     * Export report to Excel
     */
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/reports/{reportId}/export/excel")
    public ResponseEntity<byte[]> exportReportToExcel(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable("reportId") String reportId
    ) {
        log.info("GET /quality-measure/reports/{}/export/excel - tenant: {}", reportId, tenantId);
        try {
            UUID uuid = UUID.fromString(reportId);
            SavedReportEntity report = reportService.getSavedReport(tenantId, uuid);

            byte[] excelData = exportService.exportToExcel(report);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", sanitizeFilename(report.getReportName()) + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid report ID format: " + reportId);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        } catch (IOException e) {
            log.error("Error exporting report to Excel: {}", e.getMessage());
            throw new RuntimeException("Failed to export report to Excel", e);
        }
    }

    /**
     * Sanitize filename to prevent path traversal and special character issues
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "report";
        }
        return filename.replaceAll("[^a-zA-Z0-9-_\\s]", "_").replaceAll("\\s+", "_");
    }
}
