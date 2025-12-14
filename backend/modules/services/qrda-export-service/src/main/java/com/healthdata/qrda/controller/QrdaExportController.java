package com.healthdata.qrda.controller;

import com.healthdata.qrda.dto.QrdaExportJobDTO;
import com.healthdata.qrda.dto.QrdaExportRequest;
import com.healthdata.qrda.persistence.QrdaExportJobEntity;
import com.healthdata.qrda.persistence.QrdaExportJobRepository;
import com.healthdata.qrda.service.QrdaCategoryIService;
import com.healthdata.qrda.service.QrdaCategoryIIIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * REST controller for QRDA export operations.
 *
 * Provides endpoints for generating QRDA Category I and Category III documents
 * for CMS quality measure submission.
 */
@RestController
@RequestMapping("/api/v1/qrda")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "QRDA Export", description = "QRDA document generation for CMS quality reporting")
public class QrdaExportController {

    private final QrdaCategoryIService categoryIService;
    private final QrdaCategoryIIIService categoryIIIService;
    private final QrdaExportJobRepository jobRepository;

    /**
     * Initiates a QRDA Category I (patient-level) export job.
     */
    @PostMapping("/category-i/generate")
    @Operation(summary = "Generate QRDA Category I",
        description = "Creates patient-level QRDA documents for individual eCQM reporting")
    public ResponseEntity<QrdaExportJobDTO> generateCategoryI(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody QrdaExportRequest request,
            @AuthenticationPrincipal UserDetails user) {

        log.info("QRDA Category I export requested by {} for tenant {}", user.getUsername(), tenantId);

        QrdaExportJobEntity job = categoryIService.initiateExport(tenantId, request, user.getUsername());

        return ResponseEntity.accepted().body(mapToDTO(job));
    }

    /**
     * Initiates a QRDA Category III (aggregate) export job.
     */
    @PostMapping("/category-iii/generate")
    @Operation(summary = "Generate QRDA Category III",
        description = "Creates aggregate QRDA document for population-level eCQM reporting")
    public ResponseEntity<QrdaExportJobDTO> generateCategoryIII(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody QrdaExportRequest request,
            @AuthenticationPrincipal UserDetails user) {

        log.info("QRDA Category III export requested by {} for tenant {}", user.getUsername(), tenantId);

        QrdaExportJobEntity job = categoryIIIService.initiateExport(tenantId, request, user.getUsername());

        return ResponseEntity.accepted().body(mapToDTO(job));
    }

    /**
     * Gets the status of a QRDA export job.
     */
    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get job status",
        description = "Returns the current status and details of a QRDA export job")
    public ResponseEntity<QrdaExportJobDTO> getJobStatus(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID jobId) {

        return jobRepository.findByIdAndTenantId(jobId, tenantId)
            .map(job -> ResponseEntity.ok(mapToDTO(job)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lists all QRDA export jobs for a tenant.
     */
    @GetMapping("/jobs")
    @Operation(summary = "List export jobs",
        description = "Returns paginated list of QRDA export jobs for the tenant")
    public ResponseEntity<Page<QrdaExportJobDTO>> listJobs(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Job type filter")
            @RequestParam(required = false) QrdaExportJobEntity.QrdaJobType jobType,
            @Parameter(description = "Status filter")
            @RequestParam(required = false) QrdaExportJobEntity.QrdaJobStatus status,
            Pageable pageable) {

        Page<QrdaExportJobEntity> jobs = jobRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);

        return ResponseEntity.ok(jobs.map(this::mapToDTO));
    }

    /**
     * Downloads the generated QRDA document(s) for a completed job.
     */
    @GetMapping("/jobs/{jobId}/download")
    @Operation(summary = "Download QRDA documents",
        description = "Downloads the generated QRDA document(s) for a completed export job")
    public ResponseEntity<Resource> downloadQrda(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID jobId) {

        QrdaExportJobEntity job = jobRepository.findByIdAndTenantId(jobId, tenantId)
            .orElse(null);

        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        if (job.getStatus() != QrdaExportJobEntity.QrdaJobStatus.COMPLETED) {
            return ResponseEntity.badRequest().build();
        }

        if (job.getDocumentLocation() == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = Paths.get(job.getDocumentLocation());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                log.error("QRDA document not found at {}", job.getDocumentLocation());
                return ResponseEntity.notFound().build();
            }

            String filename = job.getJobType() == QrdaExportJobEntity.QrdaJobType.QRDA_I
                ? "qrda-i-" + jobId + ".zip"
                : "qrda-iii-" + jobId + ".xml";

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);

        } catch (MalformedURLException e) {
            log.error("Error creating resource for download", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Cancels a pending or running QRDA export job.
     */
    @PostMapping("/jobs/{jobId}/cancel")
    @Operation(summary = "Cancel export job",
        description = "Cancels a pending or running QRDA export job")
    public ResponseEntity<QrdaExportJobDTO> cancelJob(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID jobId,
            @AuthenticationPrincipal UserDetails user) {

        return jobRepository.findByIdAndTenantId(jobId, tenantId)
            .map(job -> {
                if (job.getStatus() == QrdaExportJobEntity.QrdaJobStatus.PENDING ||
                    job.getStatus() == QrdaExportJobEntity.QrdaJobStatus.RUNNING) {

                    job.setStatus(QrdaExportJobEntity.QrdaJobStatus.CANCELLED);
                    job = jobRepository.save(job);
                    log.info("QRDA export job {} cancelled by {}", jobId, user.getUsername());
                    return ResponseEntity.ok(mapToDTO(job));
                }
                return ResponseEntity.badRequest().<QrdaExportJobDTO>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Maps entity to DTO.
     */
    private QrdaExportJobDTO mapToDTO(QrdaExportJobEntity entity) {
        return QrdaExportJobDTO.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .jobType(entity.getJobType())
            .status(entity.getStatus())
            .measureIds(entity.getMeasureIds())
            .periodStart(entity.getPeriodStart())
            .periodEnd(entity.getPeriodEnd())
            .documentLocation(entity.getDocumentLocation())
            .documentCount(entity.getDocumentCount())
            .patientCount(entity.getPatientCount())
            .errorMessage(entity.getErrorMessage())
            .validationErrors(entity.getValidationErrors())
            .requestedBy(entity.getRequestedBy())
            .createdAt(entity.getCreatedAt())
            .startedAt(entity.getStartedAt())
            .completedAt(entity.getCompletedAt())
            .build();
    }
}
