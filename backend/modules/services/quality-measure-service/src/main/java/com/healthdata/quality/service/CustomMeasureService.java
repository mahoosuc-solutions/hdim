package com.healthdata.quality.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.client.CqlEngineServiceClient;
import com.healthdata.quality.controller.CustomMeasureController;
import com.healthdata.quality.persistence.CustomMeasureEntity;
import com.healthdata.quality.persistence.CustomMeasureRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
    private final CqlEngineServiceClient cqlEngineClient;
    private final ObjectMapper objectMapper;

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
     *
     * @param tenantId Tenant ID
     * @param id Measure ID
     * @param patientIds List of patient IDs to test (optional, uses sample patients if null)
     * @return Test results with pass/fail status for each patient
     */
    @Transactional(readOnly = true)
    @CircuitBreaker(name = "cqlEngine", fallbackMethod = "testMeasureFallback")
    public TestMeasureResult testMeasure(String tenantId, UUID id, List<String> patientIds) {
        CustomMeasureEntity measure = getById(tenantId, id);

        log.info("Testing measure [{}] against {} patients in tenant {}",
                id, patientIds != null ? patientIds.size() : 0, tenantId);

        // Use provided patient IDs or default sample patients for testing
        List<String> testPatientIds = patientIds != null && !patientIds.isEmpty()
                ? patientIds
                : List.of("sample-patient-001", "sample-patient-002", "sample-patient-003");

        // Evaluate measure against each patient using CQL engine
        List<TestPatientResult> results = new ArrayList<>();
        int passed = 0;
        int failed = 0;
        int notEligible = 0;
        int errors = 0;

        for (String patientId : testPatientIds) {
            try {
                CustomMeasureController.PatientEvaluationResult evalResult =
                    evaluatePatient(tenantId, measure.getCqlText(), patientId);

                // Determine outcome from evaluation criteria
                boolean inPopulation = evalResult.matchedCriteria().stream()
                    .anyMatch(c -> c.criterionName().contains("Population") && c.matched());
                boolean inDenominator = evalResult.matchedCriteria().stream()
                    .anyMatch(c -> c.criterionName().contains("Denominator") && c.matched());
                boolean inNumerator = evalResult.matchedCriteria().stream()
                    .anyMatch(c -> c.criterionName().contains("Numerator") && c.matched());

                String exclusionReason = evalResult.matchedCriteria().stream()
                    .filter(c -> c.criterionName().contains("Exclusion") && c.matched())
                    .map(CustomMeasureController.MatchedCriterion::reason)
                    .findFirst()
                    .orElse(null);

                List<String> details = evalResult.matchedCriteria().stream()
                    .map(c -> c.criterionName() + ": " + c.reason())
                    .collect(Collectors.toList());

                String outcome = evalResult.outcome();
                if ("pass".equals(outcome)) {
                    passed++;
                } else if ("fail".equals(outcome)) {
                    failed++;
                } else if ("not-eligible".equals(outcome)) {
                    notEligible++;
                }

                results.add(new TestPatientResult(
                    patientId,
                    evalResult.patientName(),
                    evalResult.mrn(),
                    outcome,
                    inPopulation,
                    inDenominator,
                    inNumerator,
                    exclusionReason,
                    details
                ));

            } catch (Exception e) {
                log.error("Failed to evaluate patient {}: {}", patientId, e.getMessage());
                errors++;
                results.add(new TestPatientResult(
                    patientId,
                    "Unknown",
                    "Unknown",
                    "error",
                    false,
                    false,
                    false,
                    "Evaluation error: " + e.getMessage(),
                    List.of("CQL evaluation failed")
                ));
            }
        }

        return new TestMeasureResult(
                id.toString(),
                measure.getName(),
                LocalDateTime.now().toString(),
                testPatientIds.size(),
                results,
                new TestSummary(passed, failed, notEligible, errors)
        );
    }

    /**
     * Overload method for backward compatibility - uses default sample patients
     */
    @Transactional(readOnly = true)
    public TestMeasureResult testMeasure(String tenantId, UUID id) {
        return testMeasure(tenantId, id, null);
    }

    /**
     * Fallback method when CQL Engine Service is unavailable for test evaluation.
     */
    private TestMeasureResult testMeasureFallback(
            String tenantId, UUID id, List<String> patientIds, Throwable t) {
        log.warn("Using fallback for testMeasure due to: {}", t.getMessage());

        CustomMeasureEntity measure;
        try {
            measure = getById(tenantId, id);
        } catch (Exception e) {
            measure = CustomMeasureEntity.builder()
                    .name("Unknown Measure")
                    .build();
        }

        return new TestMeasureResult(
                id.toString(),
                measure.getName(),
                LocalDateTime.now().toString(),
                0,
                List.of(),
                new TestSummary(0, 0, 0, 1)
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
     * Integrates with CQL Engine Service for real evaluation.
     */
    @CircuitBreaker(name = "cqlEngine", fallbackMethod = "evaluatePatientFallback")
    public CustomMeasureController.PatientEvaluationResult evaluatePatient(String tenantId, String cqlText, String patientId) {
        log.info("Evaluating CQL against patient {} for tenant {}", patientId, tenantId);

        try {
            // Extract library name from CQL text (first library definition)
            String libraryName = extractLibraryName(cqlText);

            // Call CQL Engine Service
            String jsonResponse = cqlEngineClient.evaluateCql(
                tenantId,
                libraryName,
                UUID.fromString(patientId),
                null // parameters
            );

            // Parse JSON response
            JsonNode result = objectMapper.readTree(jsonResponse);

            // Map CQL evaluation result to PatientEvaluationResult
            return mapCqlResultToEvaluationResult(patientId, result);

        } catch (Exception e) {
            log.error("CQL evaluation failed for patient {}: {}", patientId, e.getMessage(), e);
            throw new RuntimeException("CQL evaluation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when CQL Engine Service is unavailable.
     */
    private CustomMeasureController.PatientEvaluationResult evaluatePatientFallback(
            String tenantId, String cqlText, String patientId, Throwable t) {
        log.warn("Using fallback for CQL evaluation due to: {}", t.getMessage());

        return new CustomMeasureController.PatientEvaluationResult(
                patientId,
                "Patient " + patientId.substring(0, Math.min(8, patientId.length())),
                "MRN-" + patientId.substring(0, Math.min(6, patientId.length())),
                "error",
                List.of(new CustomMeasureController.MatchedCriterion(
                    "CQL Engine", false, "Service unavailable: " + t.getMessage()
                )),
                "CQL evaluation service temporarily unavailable"
        );
    }

    /**
     * Extract library name from CQL text.
     * Looks for "library <name>" declaration.
     */
    private String extractLibraryName(String cqlText) {
        if (cqlText == null || cqlText.isEmpty()) {
            return "DefaultLibrary";
        }

        // Match: library LibraryName version '1.0.0'
        String[] lines = cqlText.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("library ")) {
                String[] parts = trimmed.split("\\s+");
                if (parts.length >= 2) {
                    return parts[1];
                }
            }
        }

        return "DefaultLibrary";
    }

    /**
     * Map CQL Engine JSON result to PatientEvaluationResult.
     */
    private CustomMeasureController.PatientEvaluationResult mapCqlResultToEvaluationResult(
            String patientId, JsonNode result) {

        List<CustomMeasureController.MatchedCriterion> criteria = new ArrayList<>();

        // Extract criteria from CQL result
        // Expected JSON structure: { "InitialPopulation": true, "Denominator": true, "Numerator": false, ... }
        if (result.isObject()) {
            result.fields().forEachRemaining(entry -> {
                String criterionName = entry.getKey();
                JsonNode value = entry.getValue();

                boolean matched = value.isBoolean() && value.asBoolean();
                String detail = matched
                    ? criterionName + " criteria met"
                    : criterionName + " criteria not met";

                criteria.add(new CustomMeasureController.MatchedCriterion(
                    criterionName, matched, detail
                ));
            });
        }

        // Determine overall outcome
        boolean allCriteriaMet = !criteria.isEmpty() &&
            criteria.stream().allMatch(CustomMeasureController.MatchedCriterion::matched);

        String outcome = criteria.isEmpty() ? "error" : (allCriteriaMet ? "pass" : "fail");
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
