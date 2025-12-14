package com.healthdata.qrda.service;

import com.healthdata.qrda.dto.QrdaExportRequest;
import com.healthdata.qrda.persistence.QrdaExportJobEntity;
import com.healthdata.qrda.persistence.QrdaExportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for generating QRDA Category I (patient-level) documents.
 *
 * QRDA Category I contains individual patient data for quality measure reporting.
 * Each document represents one patient's clinical data for the reporting period.
 *
 * CMS Requirement: Required for patient-level eCQM submission starting 2025.
 *
 * @see <a href="https://ecqi.healthit.gov/qrda">eCQI QRDA I Implementation Guide</a>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QrdaCategoryIService {

    private final QrdaExportJobRepository jobRepository;
    private final CdaDocumentBuilder cdaDocumentBuilder;
    private final QrdaValidationService validationService;

    /**
     * Initiates an async QRDA Category I export job.
     *
     * @param tenantId The tenant identifier
     * @param request The export request parameters
     * @param requestedBy The user who initiated the request
     * @return The created job entity
     */
    @Transactional
    public QrdaExportJobEntity initiateExport(String tenantId, QrdaExportRequest request, String requestedBy) {
        log.info("Initiating QRDA Category I export for tenant {} with {} measures and {} patients",
            tenantId, request.getMeasureIds().size(),
            request.getPatientIds() != null ? request.getPatientIds().size() : "all");

        QrdaExportJobEntity job = QrdaExportJobEntity.builder()
            .tenantId(tenantId)
            .jobType(QrdaExportJobEntity.QrdaJobType.QRDA_I)
            .status(QrdaExportJobEntity.QrdaJobStatus.PENDING)
            .measureIds(request.getMeasureIds())
            .patientIds(request.getPatientIds())
            .periodStart(request.getPeriodStart())
            .periodEnd(request.getPeriodEnd())
            .requestedBy(requestedBy)
            .build();

        job = jobRepository.save(job);

        // Start async processing
        processExportAsync(job.getId(), request);

        return job;
    }

    /**
     * Asynchronously processes the QRDA Category I export.
     */
    @Async
    public void processExportAsync(UUID jobId, QrdaExportRequest request) {
        try {
            QrdaExportJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Job not found: " + jobId));

            job.setStatus(QrdaExportJobEntity.QrdaJobStatus.RUNNING);
            job.setStartedAt(LocalDateTime.now());
            jobRepository.save(job);

            log.info("Starting QRDA Category I generation for job {}", jobId);

            // Generate documents for each patient
            int documentCount = generatePatientDocuments(job, request);

            // Validate if requested
            if (request.isValidateDocuments()) {
                job.setStatus(QrdaExportJobEntity.QrdaJobStatus.VALIDATING);
                jobRepository.save(job);

                var validationErrors = validationService.validateCategoryI(job.getDocumentLocation());
                if (!validationErrors.isEmpty()) {
                    job.setValidationErrors(validationErrors);
                    log.warn("QRDA Category I validation found {} errors for job {}",
                        validationErrors.size(), jobId);
                }
            }

            job.setStatus(QrdaExportJobEntity.QrdaJobStatus.COMPLETED);
            job.setDocumentCount(documentCount);
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);

            log.info("QRDA Category I export completed for job {} with {} documents", jobId, documentCount);

        } catch (Exception e) {
            log.error("QRDA Category I export failed for job {}", jobId, e);
            jobRepository.findById(jobId).ifPresent(job -> {
                job.setStatus(QrdaExportJobEntity.QrdaJobStatus.FAILED);
                job.setErrorMessage(e.getMessage());
                job.setCompletedAt(LocalDateTime.now());
                jobRepository.save(job);
            });
        }
    }

    /**
     * Generates QRDA Category I documents for each patient.
     *
     * @param job The export job entity
     * @param request The export request
     * @return The number of documents generated
     */
    private int generatePatientDocuments(QrdaExportJobEntity job, QrdaExportRequest request) {
        // TODO: Implement patient document generation
        // 1. Fetch patient list from patient-service
        // 2. For each patient, fetch clinical data from FHIR service
        // 3. Fetch measure results from quality-measure-service
        // 4. Build CDA document using CdaDocumentBuilder
        // 5. Apply QRDA Category I template
        // 6. Write to storage location

        log.info("Generating QRDA Category I documents for job {}", job.getId());

        // Placeholder - actual implementation will iterate over patients
        int patientCount = job.getPatientIds() != null ? job.getPatientIds().size() : 0;
        job.setPatientCount(patientCount);

        return patientCount;
    }
}
