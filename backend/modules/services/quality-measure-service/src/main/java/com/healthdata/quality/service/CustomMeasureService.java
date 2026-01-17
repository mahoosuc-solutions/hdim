package com.healthdata.quality.service;

import com.healthdata.quality.controller.CustomMeasureController;
import com.healthdata.quality.persistence.CustomMeasureEntity;
import com.healthdata.quality.persistence.CustomMeasureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomMeasureService {

    private final CustomMeasureRepository repository;

    @Transactional
    public CustomMeasureEntity createDraft(String tenantId, String name, String description, String category, Integer year, String createdBy) {
        CustomMeasureEntity entity = CustomMeasureEntity.builder()
                .tenantId(tenantId)
                .name(name)
                .description(description)
                .category(category)
                .year(year)
                .status("DRAFT")
                .createdBy(createdBy)
                .build();
        CustomMeasureEntity saved = repository.save(entity);
        log.info("Created custom measure draft [{}] for tenant {}", saved.getId(), tenantId);
        return saved;
    }

    @Transactional
    public CustomMeasureEntity updateDraft(String tenantId, UUID id, String name, String description, String category, Integer year) {
        CustomMeasureEntity existing = getById(tenantId, id);
        if (name != null && !name.isBlank()) {
            existing.setName(name);
        }
        if (description != null) {
            existing.setDescription(description);
        }
        if (category != null) {
            existing.setCategory(category);
        }
        if (year != null) {
            existing.setYear(year);
        }
        CustomMeasureEntity saved = repository.save(existing);
        log.info("Updated custom measure draft [{}] for tenant {}", saved.getId(), tenantId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<CustomMeasureEntity> list(String tenantId, String status) {
        if (status != null && !status.isBlank()) {
            return repository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status);
        }
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public CustomMeasureEntity getById(String tenantId, UUID id) {
        return repository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new NoSuchElementException("Custom measure not found"));
    }

    /**
     * Batch publish draft measures.
     * Only DRAFT measures are published. Already published measures are skipped.
     */
    @Transactional
    public BatchPublishResult batchPublish(String tenantId, List<UUID> measureIds, String publishedBy) {
        log.info("Batch publishing {} measures for tenant {}", measureIds.size(), tenantId);

        List<CustomMeasureEntity> measures = repository.findByTenantIdAndIdIn(tenantId, measureIds);

        // Validate that all requested IDs were found
        if (measures.size() != measureIds.size()) {
            List<UUID> foundIds = measures.stream().map(CustomMeasureEntity::getId).collect(Collectors.toList());
            List<UUID> notFound = measureIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            throw new IllegalArgumentException("Measures not found or not accessible: " + notFound);
        }

        int publishedCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();

        for (CustomMeasureEntity measure : measures) {
            if ("DRAFT".equals(measure.getStatus())) {
                measure.setStatus("PUBLISHED");
                measure.setPublishedDate(LocalDateTime.now());
                repository.save(measure);
                publishedCount++;
                log.info("Published measure [{}] for tenant {}", measure.getId(), tenantId);
            } else {
                skippedCount++;
                log.info("Skipped measure [{}] - already published (status: {})", measure.getId(), measure.getStatus());
            }
        }

        log.info("Batch publish complete: {} published, {} skipped", publishedCount, skippedCount);
        return new BatchPublishResult(publishedCount, skippedCount, 0, errors);
    }

    /**
     * Batch delete custom measures.
     * Uses soft delete (sets deletedAt timestamp) for HIPAA compliance.
     */
    @Transactional
    public BatchDeleteResult batchDelete(String tenantId, List<UUID> measureIds, String deletedBy, boolean force) {
        log.info("Batch deleting {} measures for tenant {} (force={})", measureIds.size(), tenantId, force);

        List<CustomMeasureEntity> measures = repository.findByTenantIdAndIdIn(tenantId, measureIds);

        // Validate that all requested IDs were found
        if (measures.size() != measureIds.size()) {
            List<UUID> foundIds = measures.stream().map(CustomMeasureEntity::getId).collect(Collectors.toList());
            List<UUID> notFound = measureIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            throw new IllegalArgumentException("Measures not found or not accessible: " + notFound);
        }

        // Check which measures are in use (have evaluations)
        List<UUID> measureIdsToCheck = measures.stream().map(CustomMeasureEntity::getId).collect(Collectors.toList());
        long evaluationsCount = repository.countEvaluationsByMeasureIds(measureIdsToCheck);

        int deletedCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();
        List<String> measuresInUse = new ArrayList<>();

        for (CustomMeasureEntity measure : measures) {
            // For simplicity, we'll allow deletion if force=true or if no evaluations exist
            if (!force && evaluationsCount > 0) {
                failedCount++;
                measuresInUse.add(measure.getId().toString());
                errors.add("Measure " + measure.getId() + " has evaluations and cannot be deleted without force flag");
                log.warn("Cannot delete measure [{}] - has evaluations", measure.getId());
            } else {
                // Soft delete
                measure.setDeletedAt(LocalDateTime.now());
                measure.setDeletedBy(deletedBy);
                repository.save(measure);
                deletedCount++;
                log.info("Soft deleted measure [{}] for tenant {}", measure.getId(), tenantId);
            }
        }

        log.info("Batch delete complete: {} deleted, {} failed", deletedCount, failedCount);
        return new BatchDeleteResult(deletedCount, failedCount, errors, measuresInUse);
    }

    /**
     * Delete a single custom measure (soft delete).
     */
    @Transactional
    public void delete(String tenantId, UUID id, String deletedBy) {
        CustomMeasureEntity measure = getById(tenantId, id);
        measure.setDeletedAt(LocalDateTime.now());
        measure.setDeletedBy(deletedBy);
        repository.save(measure);
        log.info("Soft deleted measure [{}] for tenant {}", id, tenantId);
    }

    /**
     * Update CQL text for a custom measure.
     */
    @Transactional
    public CustomMeasureEntity updateCql(String tenantId, UUID id, String cqlText) {
        CustomMeasureEntity measure = getById(tenantId, id);
        measure.setCqlText(cqlText);
        CustomMeasureEntity saved = repository.save(measure);
        log.info("Updated CQL for measure [{}] in tenant {}", id, tenantId);
        return saved;
    }

    /**
     * Update value sets for a custom measure.
     */
    @Transactional
    public CustomMeasureEntity updateValueSets(String tenantId, UUID id, String valueSetsJson) {
        CustomMeasureEntity measure = getById(tenantId, id);
        measure.setValueSets(valueSetsJson);
        CustomMeasureEntity saved = repository.save(measure);
        log.info("Updated value sets for measure [{}] in tenant {}", id, tenantId);
        return saved;
    }

    /**
     * Publish a single custom measure (change status from DRAFT to PUBLISHED).
     */
    @Transactional
    public CustomMeasureEntity publish(String tenantId, UUID id) {
        CustomMeasureEntity measure = getById(tenantId, id);
        if (!"DRAFT".equals(measure.getStatus())) {
            throw new IllegalStateException("Only DRAFT measures can be published. Current status: " + measure.getStatus());
        }
        measure.setStatus("PUBLISHED");
        measure.setPublishedDate(LocalDateTime.now());
        CustomMeasureEntity saved = repository.save(measure);
        log.info("Published measure [{}] in tenant {}", id, tenantId);
        return saved;
    }

    /**
     * Clone a custom measure as a new draft.
     */
    @Transactional
    public CustomMeasureEntity clone(String tenantId, UUID id, String clonedBy) {
        CustomMeasureEntity original = getById(tenantId, id);
        CustomMeasureEntity cloned = CustomMeasureEntity.builder()
                .tenantId(tenantId)
                .name(original.getName() + " (Copy)")
                .description(original.getDescription())
                .category(original.getCategory())
                .year(original.getYear())
                .cqlText(original.getCqlText())
                .valueSets(original.getValueSets())
                .status("DRAFT")
                .createdBy(clonedBy)
                .build();
        CustomMeasureEntity saved = repository.save(cloned);
        log.info("Cloned measure [{}] to [{}] in tenant {}", id, saved.getId(), tenantId);
        return saved;
    }

    /**
     * Test/evaluate a custom measure against sample patients.
     * Returns test results with pass/fail status for each patient.
     */
    @Transactional(readOnly = true)
    public TestMeasureResult testMeasure(String tenantId, UUID id) {
        CustomMeasureEntity measure = getById(tenantId, id);
        
        // For now, return mock test results
        // TODO: Integrate with CQL engine for actual evaluation
        log.info("Testing measure [{}] in tenant {}", id, tenantId);
        
        return new TestMeasureResult(
                id.toString(),
                measure.getName(),
                LocalDateTime.now().toString(),
                5, // totalPatients
                List.of(
                        new TestPatientResult("P001", "John Doe", "MRN001", "pass", true, true, true, null, List.of("Met all criteria")),
                        new TestPatientResult("P002", "Jane Smith", "MRN002", "pass", true, true, true, null, List.of("Met all criteria")),
                        new TestPatientResult("P003", "Bob Johnson", "MRN003", "fail", true, true, false, null, List.of("Missing required lab test")),
                        new TestPatientResult("P004", "Alice Brown", "MRN004", "not-eligible", false, false, false, "Age out of range", List.of("Patient age 17, requires 18+")),
                        new TestPatientResult("P005", "Charlie Wilson", "MRN005", "pass", true, true, true, null, List.of("Met all criteria"))
                ),
                new TestSummary(3, 1, 1, 0)
        );
    }

    // Result classes
    public record TestMeasureResult(
            String measureId,
            String measureName,
            String testDate,
            int totalPatients,
            List<TestPatientResult> results,
            TestSummary summary
    ) {}

    public record TestPatientResult(
            String patientId,
            String patientName,
            String mrn,
            String outcome,
            boolean inPopulation,
            boolean inDenominator,
            boolean inNumerator,
            String exclusionReason,
            List<String> details
    ) {}

    public record TestSummary(
            int passed,
            int failed,
            int notEligible,
            int errors
    ) {}

    /**
     * Evaluate CQL text against a specific patient.
     * TODO: Integrate with actual CQL engine for real evaluation
     */
    public CustomMeasureController.PatientEvaluationResult evaluatePatient(String tenantId, String cqlText, String patientId) {
        log.info("Evaluating CQL against patient {} for tenant {}", patientId, tenantId);

        // TODO: Replace with real CQL engine integration
        // For now, return mock result based on patient ID
        List<CustomMeasureController.MatchedCriterion> criteria = List.of(
                new CustomMeasureController.MatchedCriterion("Initial Population", true, "Patient age 45 is within range 18-75"),
                new CustomMeasureController.MatchedCriterion("Has Diabetes Diagnosis", true, "ICD-10 E11.9 found in conditions"),
                new CustomMeasureController.MatchedCriterion("Denominator Exclusion", false, "No hospice care documented"),
                new CustomMeasureController.MatchedCriterion("HbA1c Test", patientId.hashCode() % 2 == 0, 
                        patientId.hashCode() % 2 == 0 ? "LOINC 4548-4 found within measurement period" : "No HbA1c test found")
        );

        boolean allCriteriaMet = criteria.stream().allMatch(CustomMeasureController.MatchedCriterion::matched);
        String outcome = allCriteriaMet ? "pass" : "fail";
        String message = allCriteriaMet 
                ? "Patient meets all criteria for this measure"
                : "Patient does not meet all criteria for numerator";

        return new CustomMeasureController.PatientEvaluationResult(
                patientId,
                "Patient " + patientId.substring(0, Math.min(8, patientId.length())),
                "MRN-" + patientId.substring(0, Math.min(6, patientId.length())),
                outcome,
                criteria,
                message
        );
    }

    public record BatchPublishResult(
            int publishedCount,
            int skippedCount,
            int failedCount,
            List<String> errors
    ) {}

    public record BatchDeleteResult(
            int deletedCount,
            int failedCount,
            List<String> errors,
            List<String> measuresInUse
    ) {}
}
