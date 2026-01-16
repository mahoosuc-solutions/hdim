package com.healthdata.quality.controller;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.quality.persistence.CustomMeasureEntity;
import com.healthdata.quality.service.CustomMeasureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/quality-measure/custom-measures")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Custom Measures", description = "Custom quality measure management")
public class CustomMeasureController {

    private final CustomMeasureService customMeasureService;

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
                request.year()
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
    @DeleteMapping("/batch-delete")
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
        log.info("DELETE /quality-measure/custom-measures/batch-delete - tenant {}, count {}, force {}",
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

    // Request/Response DTOs
    public record CreateCustomMeasureRequest(
            @NotBlank(message = "Measure name is required")
            String name,
            String description,
            String category,
            Integer year,
            String createdBy
    ) {}

    public record UpdateCustomMeasureRequest(
            String name,
            String description,
            String category,
            Integer year
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
}
