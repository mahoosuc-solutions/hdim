package com.healthdata.quality.controller;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.quality.dto.EvaluationDefaultPresetDTO;
import com.healthdata.quality.dto.SaveEvaluationPresetRequest;
import com.healthdata.quality.persistence.EvaluationDefaultPresetEntity;
import com.healthdata.quality.service.EvaluationPresetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/evaluation-presets")
@RequiredArgsConstructor
@Slf4j
@Validated
public class EvaluationPresetController {

    private final EvaluationPresetService presetService;

    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN', 'MEASURE_DEVELOPER')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/default", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EvaluationDefaultPresetDTO> getDefaultPreset(
        @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
        @RequestHeader(value = "X-Auth-User-Id", required = false) String userId
    ) {
        log.info("GET /quality-measure/evaluation-presets/default - tenant: {}", tenantId);

        return presetService.findDefaultPreset(tenantId, userId)
            .map(EvaluationDefaultPresetDTO::fromEntity)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN', 'MEASURE_DEVELOPER')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = true, includeResponsePayload = false)
    @PutMapping(value = "/default", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EvaluationDefaultPresetDTO> saveDefaultPreset(
        @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
        @RequestHeader(value = "X-Auth-User-Id", required = false) String userId,
        @Valid @RequestBody SaveEvaluationPresetRequest request
    ) {
        log.info("PUT /quality-measure/evaluation-presets/default - tenant: {}", tenantId);

        EvaluationDefaultPresetEntity saved = presetService.saveDefaultPreset(tenantId, userId, request);
        return ResponseEntity.ok(EvaluationDefaultPresetDTO.fromEntity(saved));
    }

    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN', 'MEASURE_DEVELOPER')")
    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/default")
    public ResponseEntity<Void> clearDefaultPreset(
        @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
        @RequestHeader(value = "X-Auth-User-Id", required = false) String userId
    ) {
        log.info("DELETE /quality-measure/evaluation-presets/default - tenant: {}", tenantId);

        presetService.clearDefaultPreset(tenantId, userId);
        return ResponseEntity.noContent().build();
    }
}
