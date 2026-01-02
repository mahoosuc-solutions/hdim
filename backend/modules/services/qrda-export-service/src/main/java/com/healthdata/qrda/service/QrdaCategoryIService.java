package com.healthdata.qrda.service;

import com.healthdata.qrda.client.PatientServiceClient;
import com.healthdata.qrda.client.QualityMeasureClient;
import com.healthdata.qrda.dto.QrdaExportRequest;
import com.healthdata.qrda.persistence.QrdaExportJobEntity;
import com.healthdata.qrda.persistence.QrdaExportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private final PatientServiceClient patientServiceClient;
    private final QualityMeasureClient qualityMeasureClient;

    @Value("${qrda.export.storage-path:/tmp/qrda-exports}")
    private String storagePath;

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
     * Workflow:
     * 1. Fetch patient list from patient-service (or use provided patient IDs)
     * 2. For each patient, fetch measure results from quality-measure-service
     * 3. Build CDA document using CdaDocumentBuilder
     * 4. Write to storage location
     *
     * @param job The export job entity
     * @param request The export request
     * @return The number of documents generated
     */
    private int generatePatientDocuments(QrdaExportJobEntity job, QrdaExportRequest request) {
        log.info("Generating QRDA Category I documents for job {}", job.getId());

        String tenantId = job.getTenantId();
        List<UUID> patientIds = job.getPatientIds();

        // 1. Fetch patient list from patient-service if not provided
        List<PatientServiceClient.PatientDTO> patients = fetchPatients(tenantId, patientIds);
        log.info("Processing {} patients for QRDA Category I export", patients.size());

        job.setPatientCount(patients.size());

        // Create storage directory for this job
        Path jobStoragePath;
        try {
            jobStoragePath = createJobStorageDirectory(tenantId, job.getId());
            job.setDocumentLocation(jobStoragePath.toString());
            jobRepository.save(job);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directory", e);
        }

        int successCount = 0;
        List<String> errors = new ArrayList<>();

        // 2-5. For each patient, generate QRDA Category I document
        for (PatientServiceClient.PatientDTO patient : patients) {
            try {
                // 3. Fetch measure results from quality-measure-service
                List<QualityMeasureClient.PatientMeasureResultDTO> measureResults =
                    qualityMeasureClient.getPatientMeasureResults(
                        tenantId,
                        patient.getId(),
                        job.getMeasureIds(),
                        request.getPeriodStart(),
                        request.getPeriodEnd()
                    );

                // 4. Build CDA document using CdaDocumentBuilder
                String document = cdaDocumentBuilder.buildQrdaCategoryI(
                    patient.getId(),
                    request.getPeriodStart(),
                    request.getPeriodEnd()
                );

                // 5. Write to storage location
                String filename = String.format("qrda-i-%s-%s.xml",
                    patient.getId(),
                    job.getId().toString().substring(0, 8));
                Path documentPath = jobStoragePath.resolve(filename);
                Files.writeString(documentPath, document, StandardCharsets.UTF_8);

                successCount++;
                log.debug("Generated QRDA Category I document for patient {}: {}",
                    patient.getId(), documentPath);

            } catch (Exception e) {
                String errorMsg = String.format("Patient %s: %s", patient.getId(), e.getMessage());
                errors.add(errorMsg);
                log.error("Error generating QRDA Category I for patient {}: {}",
                    patient.getId(), e.getMessage(), e);
            }
        }

        // Record any errors
        if (!errors.isEmpty()) {
            job.setValidationErrors(errors);
            log.warn("QRDA Category I export completed with {} errors out of {} patients",
                errors.size(), patients.size());
        }

        log.info("Generated {} QRDA Category I documents for job {}", successCount, job.getId());
        return successCount;
    }

    /**
     * Fetches patients from patient-service.
     *
     * If patientIds are provided, fetches those specific patients.
     * Otherwise, fetches all patients for the tenant.
     */
    private List<PatientServiceClient.PatientDTO> fetchPatients(String tenantId, List<UUID> patientIds) {
        try {
            if (patientIds != null && !patientIds.isEmpty()) {
                // Fetch specific patients
                log.debug("Fetching {} specific patients from patient-service", patientIds.size());
                return patientServiceClient.getPatientsByIds(tenantId, patientIds);
            } else {
                // Fetch all patients for tenant (paginated)
                log.debug("Fetching all patients for tenant {} from patient-service", tenantId);
                List<PatientServiceClient.PatientDTO> allPatients = new ArrayList<>();
                int page = 0;
                int pageSize = 100;
                List<PatientServiceClient.PatientDTO> batch;

                do {
                    batch = patientServiceClient.getPatients(tenantId, page, pageSize);
                    allPatients.addAll(batch);
                    page++;
                } while (batch.size() == pageSize);

                return allPatients;
            }
        } catch (Exception e) {
            log.error("Error fetching patients from patient-service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch patients", e);
        }
    }

    /**
     * Creates the storage directory for a QRDA export job.
     */
    private Path createJobStorageDirectory(String tenantId, UUID jobId) throws IOException {
        Path tenantDir = Paths.get(storagePath, tenantId, "qrda-i");
        Files.createDirectories(tenantDir);

        Path jobDir = tenantDir.resolve(jobId.toString());
        Files.createDirectories(jobDir);

        return jobDir;
    }
}
