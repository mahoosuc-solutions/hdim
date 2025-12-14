package com.healthdata.qrda.service;

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
import java.util.List;
import java.util.UUID;

/**
 * Service for generating QRDA Category III (aggregate) documents.
 *
 * QRDA Category III contains aggregate quality measure data at the population level.
 * Used for reporting performance rates, stratifications, and supplemental data.
 *
 * CMS Requirement: Primary submission format for MIPS and APP reporting.
 *
 * @see <a href="https://ecqi.healthit.gov/qrda">eCQI QRDA III Implementation Guide</a>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QrdaCategoryIIIService {

    private final QrdaExportJobRepository jobRepository;
    private final CdaDocumentBuilder cdaDocumentBuilder;
    private final QrdaValidationService validationService;
    private final QualityMeasureClient qualityMeasureClient;

    @Value("${qrda.export.storage-path:/tmp/qrda-exports}")
    private String storagePath;

    /**
     * Initiates an async QRDA Category III export job.
     *
     * @param tenantId The tenant identifier
     * @param request The export request parameters
     * @param requestedBy The user who initiated the request
     * @return The created job entity
     */
    @Transactional
    public QrdaExportJobEntity initiateExport(String tenantId, QrdaExportRequest request, String requestedBy) {
        log.info("Initiating QRDA Category III export for tenant {} with {} measures",
            tenantId, request.getMeasureIds().size());

        QrdaExportJobEntity job = QrdaExportJobEntity.builder()
            .tenantId(tenantId)
            .jobType(QrdaExportJobEntity.QrdaJobType.QRDA_III)
            .status(QrdaExportJobEntity.QrdaJobStatus.PENDING)
            .measureIds(request.getMeasureIds())
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
     * Asynchronously processes the QRDA Category III export.
     */
    @Async
    public void processExportAsync(UUID jobId, QrdaExportRequest request) {
        try {
            QrdaExportJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Job not found: " + jobId));

            job.setStatus(QrdaExportJobEntity.QrdaJobStatus.RUNNING);
            job.setStartedAt(LocalDateTime.now());
            jobRepository.save(job);

            log.info("Starting QRDA Category III generation for job {}", jobId);

            // Generate aggregate document
            String documentPath = generateAggregateDocument(job, request);
            job.setDocumentLocation(documentPath);

            // Validate if requested
            if (request.isValidateDocuments()) {
                job.setStatus(QrdaExportJobEntity.QrdaJobStatus.VALIDATING);
                jobRepository.save(job);

                var validationErrors = validationService.validateCategoryIII(documentPath);
                if (!validationErrors.isEmpty()) {
                    job.setValidationErrors(validationErrors);
                    log.warn("QRDA Category III validation found {} errors for job {}",
                        validationErrors.size(), jobId);
                }
            }

            job.setStatus(QrdaExportJobEntity.QrdaJobStatus.COMPLETED);
            job.setDocumentCount(1); // QRDA III is always a single document
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);

            log.info("QRDA Category III export completed for job {}", jobId);

        } catch (Exception e) {
            log.error("QRDA Category III export failed for job {}", jobId, e);
            jobRepository.findById(jobId).ifPresent(job -> {
                job.setStatus(QrdaExportJobEntity.QrdaJobStatus.FAILED);
                job.setErrorMessage(e.getMessage());
                job.setCompletedAt(LocalDateTime.now());
                jobRepository.save(job);
            });
        }
    }

    /**
     * Generates the QRDA Category III aggregate document.
     *
     * @param job The export job entity
     * @param request The export request
     * @return Path to the generated document
     */
    private String generateAggregateDocument(QrdaExportJobEntity job, QrdaExportRequest request) {
        log.info("Generating QRDA Category III document for job {}", job.getId());

        try {
            // 1. Fetch population-level measure results from quality-measure-service
            List<QualityMeasureClient.MeasureAggregateDTO> measureResults =
                qualityMeasureClient.getAggregateResults(
                    job.getTenantId(),
                    request.getMeasureIds(),
                    request.getPeriodStart(),
                    request.getPeriodEnd()
                );

            log.info("Retrieved {} aggregate measure results for job {}", measureResults.size(), job.getId());

            // 2. Build CDA document with QRDA III template and actual data
            String documentContent = cdaDocumentBuilder.buildQrdaCategoryIIIWithData(
                request.getPeriodStart(),
                request.getPeriodEnd(),
                measureResults
            );

            // 3. Write to storage
            String documentPath = writeDocumentToStorage(job.getTenantId(), job.getId(), documentContent);

            log.info("QRDA Category III document written to {}", documentPath);

            return documentPath;

        } catch (Exception e) {
            log.error("Failed to generate QRDA Category III document for job {}", job.getId(), e);
            throw new RuntimeException("Failed to generate QRDA Category III document", e);
        }
    }

    /**
     * Writes the document content to the configured storage location.
     */
    private String writeDocumentToStorage(String tenantId, UUID jobId, String content) throws IOException {
        // Create directory structure
        Path tenantDir = Paths.get(storagePath, tenantId);
        Files.createDirectories(tenantDir);

        // Write document
        String filename = String.format("qrda-iii-%s.xml", jobId);
        Path documentPath = tenantDir.resolve(filename);
        Files.writeString(documentPath, content, StandardCharsets.UTF_8);

        return documentPath.toString();
    }

    /**
     * Calculates aggregate measure results for the performance period.
     *
     * @param tenantId The tenant identifier
     * @param measureId The measure identifier
     * @param request The export request with period dates
     * @return Aggregate results including IPP, DENOM, NUMER, DENEX, DENEXCEP
     */
    public MeasureAggregateResult calculateAggregateResults(
            String tenantId, String measureId, QrdaExportRequest request) {
        // TODO: Call quality-measure-service to get aggregate results
        return MeasureAggregateResult.builder()
            .measureId(measureId)
            .initialPopulation(0)
            .denominator(0)
            .numerator(0)
            .denominatorExclusions(0)
            .denominatorExceptions(0)
            .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class MeasureAggregateResult {
        private String measureId;
        private int initialPopulation;
        private int denominator;
        private int numerator;
        private int denominatorExclusions;
        private int denominatorExceptions;

        public double getPerformanceRate() {
            int eligiblePopulation = denominator - denominatorExclusions - denominatorExceptions;
            if (eligiblePopulation <= 0) return 0.0;
            return (double) numerator / eligiblePopulation * 100.0;
        }
    }
}
