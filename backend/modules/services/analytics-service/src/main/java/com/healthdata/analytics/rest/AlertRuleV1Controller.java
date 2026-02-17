package com.healthdata.analytics.rest;

import com.healthdata.analytics.dto.AlertDto;
import com.healthdata.analytics.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Versioned alert-rule API compatible with Phase 4 contract paths.
 */
@RestController
@RequestMapping("/api/v1/alert-rules")
@RequiredArgsConstructor
public class AlertRuleV1Controller {

    private final AlertService alertService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<AlertDto>> getAlertRules(
        @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        return ResponseEntity.ok(alertService.getAlertRules(tenantId));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<AlertDto>> getActiveAlertRules(
        @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        return ResponseEntity.ok(alertService.getActiveAlertRules(tenantId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<AlertDto> createAlertRule(
        @Valid @RequestBody AlertDto dto,
        @RequestHeader("X-Tenant-ID") String tenantId,
        Authentication authentication
    ) {
        String userId = authentication.getName();
        return ResponseEntity.status(HttpStatus.CREATED).body(alertService.createAlertRule(dto, tenantId, userId));
    }

    @PutMapping("/{ruleId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<AlertDto> updateAlertRule(
        @PathVariable UUID ruleId,
        @Valid @RequestBody AlertDto dto,
        @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        return alertService.updateAlertRule(ruleId, dto, tenantId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{ruleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAlertRule(
        @PathVariable UUID ruleId,
        @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        if (alertService.deleteAlertRule(ruleId, tenantId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/predictive-check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AlertService.PredictiveAlertDto>> predictiveCheck(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam(defaultValue = "14") int days
    ) {
        return ResponseEntity.ok(alertService.checkPredictiveAlerts(tenantId, days));
    }
}
