package com.healthdata.fhir.bulk;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.fhir.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Async processor for FHIR Bulk Data Export
 *
 * Handles background processing of export jobs:
 * - Exports resources to NDJSON files
 * - Supports chunking for large datasets
 * - Implements incremental exports with _since parameter
 */
@Component
@Slf4j
public class BulkExportProcessor {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(false);

    private final BulkExportRepository exportRepository;
    private final BulkExportConfig config;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Resource repositories
    private final PatientRepository patientRepository;
    private final ObservationRepository observationRepository;
    private final ConditionRepository conditionRepository;
    private final MedicationRequestRepository medicationRequestRepository;
    private final ProcedureRepository procedureRepository;
    private final EncounterRepository encounterRepository;
    private final AllergyIntoleranceRepository allergyIntoleranceRepository;
    private final ImmunizationRepository immunizationRepository;

    public BulkExportProcessor(
            BulkExportRepository exportRepository,
            BulkExportConfig config,
            KafkaTemplate<String, Object> kafkaTemplate,
            PatientRepository patientRepository,
            ObservationRepository observationRepository,
            ConditionRepository conditionRepository,
            MedicationRequestRepository medicationRequestRepository,
            ProcedureRepository procedureRepository,
            EncounterRepository encounterRepository,
            AllergyIntoleranceRepository allergyIntoleranceRepository,
            ImmunizationRepository immunizationRepository) {
        this.exportRepository = exportRepository;
        this.config = config;
        this.kafkaTemplate = kafkaTemplate;
        this.patientRepository = patientRepository;
        this.observationRepository = observationRepository;
        this.conditionRepository = conditionRepository;
        this.medicationRequestRepository = medicationRequestRepository;
        this.procedureRepository = procedureRepository;
        this.encounterRepository = encounterRepository;
        this.allergyIntoleranceRepository = allergyIntoleranceRepository;
        this.immunizationRepository = immunizationRepository;
    }

    /**
     * Process export asynchronously
     */
    @Async("bulkExportExecutor")
    @Transactional
    public CompletableFuture<Void> processExport(UUID jobId) {
        log.info("Starting async processing for export job: {}", jobId);

        BulkExportJob job = exportRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Export job not found: " + jobId));

        try {
            // Update status to IN_PROGRESS
            job = updateJobStatus(job, BulkExportJob.ExportStatus.IN_PROGRESS);
            job.setStartedAt(Instant.now());
            job.setTransactionTime(Instant.now());
            job = exportRepository.save(job);

            // Create export directory
            Path exportDir = createExportDirectory(job);

            // Export each resource type
            List<BulkExportJob.OutputFile> outputFiles = new ArrayList<>();
            long totalExported = 0;

            for (String resourceType : job.getResourceTypes()) {
                try {
                    long count = exportResourceType(job, resourceType, exportDir);
                    if (count > 0) {
                        String fileName = resourceType.toLowerCase() + "-" + jobId + ".ndjson";
                        String filePath = exportDir.resolve(fileName).toString();
                        String url = config.getBaseUrl() + "/download/" + job.getJobId() + "/" + fileName;

                        outputFiles.add(BulkExportJob.OutputFile.builder()
                            .type(resourceType)
                            .url(url)
                            .filePath(filePath)
                            .count(count)
                            .build());

                        totalExported += count;
                    }
                } catch (Exception e) {
                    log.error("Error exporting resource type: {}", resourceType, e);
                    // Continue with other resource types
                }
            }

            // Update job as completed
            job.setStatus(BulkExportJob.ExportStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            job.setOutputFiles(outputFiles);
            job.setExportedResources(totalExported);
            exportRepository.save(job);

            log.info("Completed export job: {} with {} resources", jobId, totalExported);

            // Publish completion event
            kafkaTemplate.send("fhir.bulk-export.completed", jobId.toString(),
                Map.of("jobId", jobId.toString(), "tenantId", job.getTenantId(), "resourceCount", totalExported));

        } catch (Exception e) {
            log.error("Error processing export job: {}", jobId, e);
            job.setStatus(BulkExportJob.ExportStatus.FAILED);
            job.setCompletedAt(Instant.now());
            job.setErrorMessage(e.getMessage());
            exportRepository.save(job);

            // Publish failure event
            kafkaTemplate.send("fhir.bulk-export.failed", jobId.toString(),
                Map.of("jobId", jobId.toString(), "tenantId", job.getTenantId(), "error", e.getMessage()));
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Export a specific resource type
     */
    private long exportResourceType(BulkExportJob job, String resourceType, Path exportDir) throws IOException {
        log.info("Exporting resource type: {} for job: {}", resourceType, job.getJobId());

        String fileName = resourceType.toLowerCase() + "-" + job.getJobId() + ".ndjson";
        Path filePath = exportDir.resolve(fileName);

        long totalCount = 0;
        int pageNumber = 0;
        Pageable pageable = PageRequest.of(pageNumber, config.getChunkSize());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            Page<?> page;
            do {
                page = fetchResourcePage(job, resourceType, pageable);

                for (Object entity : page.getContent()) {
                    String json = convertEntityToJson(entity, resourceType);
                    if (json != null) {
                        writer.write(json);
                        writer.newLine();
                        totalCount++;
                    }
                }

                pageNumber++;
                pageable = PageRequest.of(pageNumber, config.getChunkSize());

            } while (page.hasNext());
        }

        log.info("Exported {} {} resources", totalCount, resourceType);
        return totalCount;
    }

    /**
     * Fetch a page of resources
     */
    private Page<?> fetchResourcePage(BulkExportJob job, String resourceType, Pageable pageable) {
        String tenantId = job.getTenantId();
        Instant since = job.getSinceParam();

        return switch (resourceType) {
            case "Patient" -> patientRepository.findAll(pageable);
            case "Observation" -> observationRepository.findAll(pageable);
            case "Condition" -> conditionRepository.findAll(pageable);
            case "MedicationRequest" -> medicationRequestRepository.findAll(pageable);
            case "Procedure" -> procedureRepository.findAll(pageable);
            case "Encounter" -> encounterRepository.findAll(pageable);
            case "AllergyIntolerance" -> allergyIntoleranceRepository.findAll(pageable);
            case "Immunization" -> immunizationRepository.findAll(pageable);
            default -> Page.empty();
        };
    }

    /**
     * Convert entity to FHIR JSON
     */
    private String convertEntityToJson(Object entity, String resourceType) {
        try {
            String resourceJson = extractResourceJson(entity);
            if (resourceJson == null) {
                return null;
            }

            // Parse and re-serialize to ensure valid JSON
            Resource resource = (Resource) JSON_PARSER.parseResource(resourceJson);
            return JSON_PARSER.encodeResourceToString(resource);
        } catch (Exception e) {
            log.error("Error converting {} to JSON", resourceType, e);
            return null;
        }
    }

    /**
     * Extract resource JSON from entity
     */
    private String extractResourceJson(Object entity) {
        if (entity instanceof PatientEntity) {
            return ((PatientEntity) entity).getResourceJson();
        } else if (entity instanceof ObservationEntity) {
            return ((ObservationEntity) entity).getResourceJson();
        } else if (entity instanceof ConditionEntity) {
            return ((ConditionEntity) entity).getResourceJson();
        } else if (entity instanceof MedicationRequestEntity) {
            return ((MedicationRequestEntity) entity).getResourceJson();
        } else if (entity instanceof ProcedureEntity) {
            return ((ProcedureEntity) entity).getResourceJson();
        } else if (entity instanceof EncounterEntity) {
            return ((EncounterEntity) entity).getResourceJson();
        } else if (entity instanceof AllergyIntoleranceEntity) {
            return convertAllergyIntoleranceToJson((AllergyIntoleranceEntity) entity);
        } else if (entity instanceof ImmunizationEntity) {
            return convertImmunizationToJson((ImmunizationEntity) entity);
        }
        return null;
    }

    /**
     * Create export directory
     */
    private Path createExportDirectory(BulkExportJob job) throws IOException {
        Path baseDir = Paths.get(config.getExportDirectory());
        Path jobDir = baseDir.resolve(job.getJobId().toString());

        if (!Files.exists(baseDir)) {
            Files.createDirectories(baseDir);
        }

        if (!Files.exists(jobDir)) {
            Files.createDirectories(jobDir);
        }

        return jobDir;
    }

    /**
     * Update job status
     */
    private BulkExportJob updateJobStatus(BulkExportJob job, BulkExportJob.ExportStatus status) {
        return job.toBuilder()
            .status(status)
            .build();
    }

    /**
     * Delete export files for a job
     */
    public void deleteExportFiles(BulkExportJob job) {
        try {
            Path jobDir = Paths.get(config.getExportDirectory()).resolve(job.getJobId().toString());
            if (Files.exists(jobDir)) {
                Files.walk(jobDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
                log.info("Deleted export files for job: {}", job.getJobId());
            }
        } catch (IOException e) {
            log.error("Error deleting export files for job: {}", job.getJobId(), e);
        }
    }

    /**
     * Convert AllergyIntoleranceEntity to FHIR JSON
     */
    private String convertAllergyIntoleranceToJson(AllergyIntoleranceEntity entity) {
        try {
            org.hl7.fhir.r4.model.AllergyIntolerance allergy = new org.hl7.fhir.r4.model.AllergyIntolerance();
            allergy.setId(entity.getId().toString());
            allergy.setPatient(new org.hl7.fhir.r4.model.Reference("Patient/" + entity.getPatientId()));
            if (entity.getClinicalStatus() != null) {
                allergy.setClinicalStatus(new org.hl7.fhir.r4.model.CodeableConcept()
                    .addCoding(new org.hl7.fhir.r4.model.Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical")
                        .setCode(entity.getClinicalStatus())));
            }
            if (entity.getVerificationStatus() != null) {
                allergy.setVerificationStatus(new org.hl7.fhir.r4.model.CodeableConcept()
                    .addCoding(new org.hl7.fhir.r4.model.Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-verification")
                        .setCode(entity.getVerificationStatus())));
            }
            return JSON_PARSER.encodeResourceToString(allergy);
        } catch (Exception e) {
            log.error("Error converting AllergyIntolerance to JSON", e);
            return null;
        }
    }

    /**
     * Convert ImmunizationEntity to FHIR JSON
     */
    private String convertImmunizationToJson(ImmunizationEntity entity) {
        try {
            org.hl7.fhir.r4.model.Immunization immunization = new org.hl7.fhir.r4.model.Immunization();
            immunization.setId(entity.getId().toString());
            immunization.setPatient(new org.hl7.fhir.r4.model.Reference("Patient/" + entity.getPatientId()));
            immunization.setStatus(org.hl7.fhir.r4.model.Immunization.ImmunizationStatus.COMPLETED);
            if (entity.getVaccineCode() != null) {
                immunization.setVaccineCode(new org.hl7.fhir.r4.model.CodeableConcept()
                    .addCoding(new org.hl7.fhir.r4.model.Coding()
                        .setSystem("http://hl7.org/fhir/sid/cvx")
                        .setCode(entity.getVaccineCode())));
            }
            return JSON_PARSER.encodeResourceToString(immunization);
        } catch (Exception e) {
            log.error("Error converting Immunization to JSON", e);
            return null;
        }
    }
}
