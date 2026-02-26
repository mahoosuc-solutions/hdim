package com.healthdata.quality.controller;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.quality.persistence.CustomMeasureEntity;
import com.healthdata.quality.service.CustomMeasureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/custom-measures")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Custom Measures", description = "Custom quality measure management")
public class CustomMeasureController {

    private final CustomMeasureService customMeasureService;
    private final ObjectMapper objectMapper;

    @PreAuthorize("hasAnyRole('MEASURE_DEVELOPER', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<CustomMeasureEntity> createDraft(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Valid @RequestBody CreateCustomMeasureRequest request
    ) {
        log.info("POST /quality-measure/custom-measures - tenant {}", tenantId);
        CustomMeasureEntity saved = customMeasureService.createDraft(
                tenantId,
                request.name(),
                request.description(),
                request.category(),
                request.year(),
                request.owner(),
                request.clinicalFocus(),
                request.reportingCadence(),
                request.targetThreshold(),
                request.priority(),
                request.implementationNotes(),
                request.tags(),
                request.createdBy() != null ? request.createdBy() : "clinical-portal"
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PreAuthorize("hasAnyRole('MEASURE_DEVELOPER', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<List<CustomMeasureEntity>> list(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestParam(value = "status", required = false) String status
    ) {
        return ResponseEntity.ok(customMeasureService.list(tenantId, status));
    }

    @PreAuthorize("hasAnyRole('MEASURE_DEVELOPER', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<CustomMeasureEntity> getById(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(customMeasureService.getById(tenantId, id));
    }

    @PreAuthorize("hasAnyRole('MEASURE_DEVELOPER', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<CustomMeasureEntity> updateDraft(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCustomMeasureRequest request
    ) {
        return ResponseEntity.ok(customMeasureService.updateDraft(
                tenantId,
                id,
                request.name(),
                request.description(),
                request.category(),
                request.year(),
                request.owner(),
                request.clinicalFocus(),
                request.reportingCadence(),
                request.targetThreshold(),
                request.priority(),
                request.implementationNotes(),
                request.tags()
        ));
    }

    @PreAuthorize("hasAnyRole('MEASURE_DEVELOPER', 'ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a custom measure (soft delete)")
    @Audited(
            action = AuditAction.DELETE,
            resourceType = "CustomMeasure",
            description = "Delete custom quality measure"
    )
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable("id") UUID id
    ) {
        log.info("DELETE /quality-measure/custom-measures/{} - tenant {}", id, tenantId);
        customMeasureService.delete(tenantId, id, "clinical-portal");
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('MEASURE_DEVELOPER', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/batch-publish")
    @Operation(summary = "Batch publish draft measures", description = "Publish multiple draft measures at once. Only DRAFT measures will be published, others will be skipped.")
    @Audited(
            action = AuditAction.UPDATE,
            resourceType = "CustomMeasure",
            description = "Batch publish custom measures"
    )
    public ResponseEntity<BatchPublishResponse> batchPublish(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Valid @RequestBody BatchPublishRequest request
    ) {
        log.info("POST /quality-measure/custom-measures/batch-publish - tenant {}, count {}", tenantId, request.measureIds().size());

        try {
            CustomMeasureService.BatchPublishResult result = customMeasureService.batchPublish(
                    tenantId,
                    request.measureIds(),
                    "clinical-portal"
            );

            BatchPublishResponse response = new BatchPublishResponse(
                    result.publishedCount(),
                    result.skippedCount(),
                    result.failedCount(),
                    result.errors()
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Batch publish validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    new BatchPublishResponse(0, 0, 0, List.of(e.getMessage()))
            );
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/batch-delete")
    @Operation(summary = "Batch delete custom measures", description = "Soft delete multiple custom measures at once. Measures with evaluations require force=true.")
    @Audited(
            action = AuditAction.DELETE,
            resourceType = "CustomMeasure",
            description = "Batch delete custom measures"
    )
    public ResponseEntity<BatchDeleteResponse> batchDelete(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Valid @RequestBody BatchDeleteRequest request
    ) {
        log.info("POST /quality-measure/custom-measures/batch-delete - tenant {}, count {}, force {}",
                tenantId, request.measureIds().size(), request.force());

        try {
            CustomMeasureService.BatchDeleteResult result = customMeasureService.batchDelete(
                    tenantId,
                    request.measureIds(),
                    "clinical-portal",
                    request.force()
            );

            BatchDeleteResponse response = new BatchDeleteResponse(
                    result.deletedCount(),
                    result.failedCount(),
                    result.errors(),
                    result.measuresInUse()
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Batch delete validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    new BatchDeleteResponse(0, 0, List.of(e.getMessage()), List.of())
            );
        }
    }

    @PreAuthorize("hasAnyRole('MEASURE_DEVELOPER', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/{id}/cql")
    @Operation(summary = "Update CQL text for a custom measure")
    @Audited(
            action = AuditAction.UPDATE,
            resourceType = "CustomMeasure",
            description = "Update CQL text"
    )
    public ResponseEntity<CustomMeasureEntity> updateCql(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCqlRequest request
    ) {
        log.info("PUT /quality-measure/custom-measures/{}/cql - tenant {}", id, tenantId);
        return ResponseEntity.ok(customMeasureService.updateCql(tenantId, id, request.cqlText()));
    }

    @PreAuthorize("hasAnyRole('MEASURE_DEVELOPER', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/{id}/value-sets")
    @Operation(summary = "Update value sets for a custom measure")
    @Audited(
            action = AuditAction.UPDATE,
            resourceType = "CustomMeasure",
            description = "Update value sets"
    )
    public ResponseEntity<CustomMeasureEntity> updateValueSets(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateValueSetsRequest request
    ) {
        log.info("PUT /quality-measure/custom-measures/{}/value-sets - tenant {}", id, tenantId);
        try {
            String valueSetsJson = request.valueSets() != null
                    ? objectMapper.writeValueAsString(request.valueSets())
                    : null;
            return ResponseEntity.ok(customMeasureService.updateValueSets(tenantId, id, valueSetsJson));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize value sets for measure {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasAnyRole('MEASURE_DEVELOPER', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a single custom measure")
    @Audited(
            action = AuditAction.UPDATE,
            resourceType = "CustomMeasure",
            description = "Publish custom measure"
    )
    public ResponseEntity<CustomMeasureEntity> publish(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable("id") UUID id
    ) {
        log.info("POST /quality-measure/custom-measures/{}/publish - tenant {}", id, tenantId);
        try {
            return ResponseEntity.ok(customMeasureService.publish(tenantId, id));
        } catch (IllegalStateException e) {
            log.warn("Cannot publish measure {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasAnyRole('MEASURE_DEVELOPER', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/{id}/clone")
    @Operation(summary = "Clone a custom measure as a new draft")
    @Audited(
            action = AuditAction.CREATE,
            resourceType = "CustomMeasure",
            description = "Clone custom measure"
    )
    public ResponseEntity<CustomMeasureEntity> cloneMeasure(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable("id") UUID id
    ) {
        log.info("POST /quality-measure/custom-measures/{}/clone - tenant {}", id, tenantId);
        CustomMeasureEntity cloned = customMeasureService.clone(tenantId, id, "clinical-portal");
        return ResponseEntity.status(HttpStatus.CREATED).body(cloned);
    }

    @PreAuthorize("hasAnyRole('MEASURE_DEVELOPER', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/{id}/test")
    @Operation(summary = "Test a custom measure against sample patients")
    @Audited(
            action = AuditAction.READ,
            resourceType = "CustomMeasure",
            description = "Test custom measure"
    )
    public ResponseEntity<CustomMeasureService.TestMeasureResult> testMeasure(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable("id") UUID id
    ) {
        log.info("POST /quality-measure/custom-measures/{}/test - tenant {}", id, tenantId);
        return ResponseEntity.ok(customMeasureService.testMeasure(tenantId, id));
    }

    @PreAuthorize("hasAnyRole('MEASURE_DEVELOPER', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/evaluate-patient")
    @Operation(summary = "Evaluate CQL text against a specific patient")
    @Audited(
            action = AuditAction.READ,
            resourceType = "CustomMeasure",
            description = "Evaluate CQL against patient"
    )
    public ResponseEntity<PatientEvaluationResult> evaluatePatient(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Valid @RequestBody EvaluatePatientRequest request
    ) {
        log.info("POST /quality-measure/custom-measures/evaluate-patient - tenant {}, patientId {}", tenantId, request.patientId());
        PatientEvaluationResult result = customMeasureService.evaluatePatient(tenantId, request.cqlText(), request.patientId());
        return ResponseEntity.ok(result);
    }

    // Request/Response DTOs
    public record CreateCustomMeasureRequest(
            @NotBlank(message = "Measure name is required")
            String name,
            String description,
            String category,
            @Min(value = 2000, message = "Year must be >= 2000")
            @Max(value = 2100, message = "Year must be <= 2100")
            Integer year,
            String owner,
            String clinicalFocus,
            @Pattern(
                    regexp = "^(MONTHLY|QUARTERLY|ANNUAL)$",
                    message = "Reporting cadence must be one of: MONTHLY, QUARTERLY, ANNUAL"
            )
            String reportingCadence,
            String targetThreshold,
            @Pattern(
                    regexp = "^(LOW|MEDIUM|HIGH)$",
                    message = "Priority must be one of: LOW, MEDIUM, HIGH"
            )
            String priority,
            String implementationNotes,
            String tags,
            String createdBy
    ) {}

    public record UpdateCustomMeasureRequest(
            String name,
            String description,
            String category,
            @Min(value = 2000, message = "Year must be >= 2000")
            @Max(value = 2100, message = "Year must be <= 2100")
            Integer year,
            String owner,
            String clinicalFocus,
            @Pattern(
                    regexp = "^(MONTHLY|QUARTERLY|ANNUAL)$",
                    message = "Reporting cadence must be one of: MONTHLY, QUARTERLY, ANNUAL"
            )
            String reportingCadence,
            String targetThreshold,
            @Pattern(
                    regexp = "^(LOW|MEDIUM|HIGH)$",
                    message = "Priority must be one of: LOW, MEDIUM, HIGH"
            )
            String priority,
            String implementationNotes,
            String tags
    ) {}

    public record BatchPublishRequest(
            @NotEmpty(message = "Measure IDs list cannot be empty")
            List<UUID> measureIds
    ) {}

    public record BatchPublishResponse(
            int publishedCount,
            int skippedCount,
            int failedCount,
            List<String> errors
    ) {}

    public record BatchDeleteRequest(
            @NotEmpty(message = "Measure IDs list cannot be empty")
            List<UUID> measureIds,
            boolean force
    ) {}

    public record BatchDeleteResponse(
            int deletedCount,
            int failedCount,
            List<String> errors,
            List<String> measuresInUse
    ) {}

    public record UpdateCqlRequest(
            @NotBlank(message = "CQL text is required")
            String cqlText
    ) {}

    public record UpdateValueSetsRequest(
            List<Object> valueSets
    ) {}

    public record EvaluatePatientRequest(
            @NotBlank(message = "CQL text is required")
            String cqlText,
            @NotBlank(message = "Patient ID is required")
            String patientId
    ) {}

    /**
     * Result of evaluating CQL against a specific patient
     */
    public record PatientEvaluationResult(
            String patientId,
            String patientName,
            String mrn,
            String outcome,
            List<MatchedCriterion> matchedCriteria,
            String message
    ) {}

    /**
     * Individual criterion evaluation result
     */
    public record MatchedCriterion(
            String criterionName,
            boolean matched,
            String reason
    ) {}
}
