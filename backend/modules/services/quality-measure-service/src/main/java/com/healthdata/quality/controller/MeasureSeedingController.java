package com.healthdata.quality.controller;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.quality.persistence.QualityMeasureEntity;
import com.healthdata.quality.persistence.QualityMeasureRepository;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import com.healthdata.quality.service.MeasureDefinitionSeedingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Controller for demo seeding of quality measures and evaluation results.
 * Used by demo-seeding-service to populate quality measure data.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class MeasureSeedingController {

    private final MeasureDefinitionSeedingService seedingService;
    private final QualityMeasureRepository measureRepository;
    private final QualityMeasureResultRepository resultRepository;

    /**
     * Seed HEDIS measure definitions for a tenant.
     */
    @PostMapping(value = "/measures/seed", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false)
    public ResponseEntity<SeedResponse> seedMeasures(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.info("POST /measures/seed - tenant: {}", tenantId);

        int seeded = seedingService.seedHedisMeasures(tenantId);

        SeedResponse response = new SeedResponse();
        response.setSeededCount(seeded);
        response.setMessage(seeded > 0
            ? "Seeded " + seeded + " HEDIS measure definitions"
            : "HEDIS measure definitions already exist");

        return ResponseEntity.ok(response);
    }

    /**
     * Get all measures for a tenant.
     */
    @GetMapping(value = "/measures", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<MeasureDTO>> getMeasures(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.info("GET /measures - tenant: {}", tenantId);

        List<QualityMeasureEntity> measures = measureRepository.findActiveByTenantId(tenantId);
        List<MeasureDTO> dtos = measures.stream().map(this::toDTO).toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Generate demo evaluation results for a batch of patients.
     * This creates realistic evaluation results without running actual CQL.
     */
    @PostMapping(value = "/demo/generate-results", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.CREATE)
    public ResponseEntity<DemoResultsResponse> generateDemoResults(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody DemoResultsRequest request) {
        log.info("POST /demo/generate-results - tenant: {}, patients: {}, measures: {}",
            tenantId, request.getPatientIds().size(), request.getMeasureIds().size());

        int resultsGenerated = 0;
        int compliantCount = 0;
        int nonCompliantCount = 0;

        Random random = new Random();
        int targetComplianceRate = 100 - request.getCareGapPercentage();

        for (String patientIdStr : request.getPatientIds()) {
            try {
                UUID patientId = UUID.fromString(patientIdStr);

                for (String measureId : request.getMeasureIds()) {
                    // Determine compliance based on target rate
                    boolean isCompliant = random.nextInt(100) < targetComplianceRate;

                    QualityMeasureResultEntity result = createDemoResult(
                        tenantId, patientId, measureId, isCompliant);

                    resultRepository.save(result);
                    resultsGenerated++;

                    if (isCompliant) {
                        compliantCount++;
                    } else {
                        nonCompliantCount++;
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to generate results for patient {}: {}",
                    patientIdStr, e.getMessage());
            }
        }

        DemoResultsResponse response = new DemoResultsResponse();
        response.setResultsGenerated(resultsGenerated);
        response.setCompliantCount(compliantCount);
        response.setNonCompliantCount(nonCompliantCount);

        log.info("Generated {} demo results ({} compliant, {} non-compliant) for tenant: {}",
            resultsGenerated, compliantCount, nonCompliantCount, tenantId);

        return ResponseEntity.ok(response);
    }

    /**
     * Create a demo evaluation result for a patient and measure.
     */
    private QualityMeasureResultEntity createDemoResult(
            String tenantId, UUID patientId, String measureId, boolean isCompliant) {

        // Get measure name from repository or use default
        String measureName = measureRepository
            .findByMeasureIdAndTenantId(measureId, tenantId)
            .map(QualityMeasureEntity::getMeasureName)
            .orElse(getMeasureNameDefault(measureId));

        return QualityMeasureResultEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .measureId(measureId)
            .measureName(measureName)
            .measureCategory("HEDIS")
            .measureYear(LocalDate.now().getYear())
            .numeratorCompliant(isCompliant)
            .denominatorElligible(true)
            .complianceRate(isCompliant ? 100.0 : 0.0)
            .score(isCompliant ? 1.0 : 0.0)
            .calculationDate(LocalDate.now())
            .cqlLibrary("HEDIS_2024_" + measureId)
            .cqlResult("{\"status\": \"demo\", \"compliant\": " + isCompliant + "}")
            .createdAt(LocalDateTime.now())
            .createdBy("demo-seeding-service")
            .build();
    }

    private String getMeasureNameDefault(String measureId) {
        return switch (measureId) {
            case "CDC" -> "Comprehensive Diabetes Care";
            case "BCS" -> "Breast Cancer Screening";
            case "COL" -> "Colorectal Cancer Screening";
            case "CBP" -> "Controlling High Blood Pressure";
            case "CCS" -> "Cervical Cancer Screening";
            case "EED" -> "Eye Exam for Patients With Diabetes";
            case "SPC" -> "Statin Therapy for Patients With Cardiovascular Disease";
            default -> measureId + " Measure";
        };
    }

    private MeasureDTO toDTO(QualityMeasureEntity entity) {
        MeasureDTO dto = new MeasureDTO();
        dto.setId(entity.getId().toString());
        dto.setMeasureId(entity.getMeasureId());
        dto.setMeasureName(entity.getMeasureName());
        dto.setMeasureSet(entity.getMeasureSet());
        dto.setVersion(entity.getVersion());
        dto.setDomain(entity.getDomain());
        dto.setCategory(entity.getCategory());
        dto.setDescription(entity.getDescription());
        dto.setActive(entity.getActive());
        return dto;
    }

    // Request/Response DTOs

    public static class SeedResponse {
        private int seededCount;
        private String message;

        public int getSeededCount() { return seededCount; }
        public void setSeededCount(int seededCount) { this.seededCount = seededCount; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class DemoResultsRequest {
        private List<String> patientIds;
        private int careGapPercentage;
        private List<String> measureIds;

        public List<String> getPatientIds() { return patientIds; }
        public void setPatientIds(List<String> patientIds) { this.patientIds = patientIds; }
        public int getCareGapPercentage() { return careGapPercentage; }
        public void setCareGapPercentage(int careGapPercentage) { this.careGapPercentage = careGapPercentage; }
        public List<String> getMeasureIds() { return measureIds; }
        public void setMeasureIds(List<String> measureIds) { this.measureIds = measureIds; }
    }

    public static class DemoResultsResponse {
        private int resultsGenerated;
        private int compliantCount;
        private int nonCompliantCount;

        public int getResultsGenerated() { return resultsGenerated; }
        public void setResultsGenerated(int resultsGenerated) { this.resultsGenerated = resultsGenerated; }
        public int getCompliantCount() { return compliantCount; }
        public void setCompliantCount(int compliantCount) { this.compliantCount = compliantCount; }
        public int getNonCompliantCount() { return nonCompliantCount; }
        public void setNonCompliantCount(int nonCompliantCount) { this.nonCompliantCount = nonCompliantCount; }
    }

    public static class MeasureDTO {
        private String id;
        private String measureId;
        private String measureName;
        private String measureSet;
        private String version;
        private String domain;
        private String category;
        private String description;
        private Boolean active;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getMeasureId() { return measureId; }
        public void setMeasureId(String measureId) { this.measureId = measureId; }
        public String getMeasureName() { return measureName; }
        public void setMeasureName(String measureName) { this.measureName = measureName; }
        public String getMeasureSet() { return measureSet; }
        public void setMeasureSet(String measureSet) { this.measureSet = measureSet; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }
}
