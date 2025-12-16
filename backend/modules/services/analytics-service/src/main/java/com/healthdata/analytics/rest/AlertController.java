package com.healthdata.analytics.rest;

import com.healthdata.analytics.dto.AlertDto;
import com.healthdata.analytics.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics/alerts")
@RequiredArgsConstructor
@Slf4j
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<AlertDto>> getAlertRules(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(alertService.getAlertRules(tenantId));
    }

    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<Page<AlertDto>> getAlertRulesPaginated(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(alertService.getAlertRulesPaginated(tenantId, pageable));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<AlertDto>> getActiveAlertRules(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(alertService.getActiveAlertRules(tenantId));
    }

    @GetMapping("/triggered")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<AlertDto>> getRecentlyTriggeredAlerts(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(alertService.getRecentlyTriggeredAlerts(tenantId, limit));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<AlertDto> getAlertRule(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return alertService.getAlertRule(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<AlertDto> createAlertRule(
            @Valid @RequestBody AlertDto dto,
            @RequestHeader("X-Tenant-ID") String tenantId,
            Authentication authentication) {
        String userId = authentication.getName();
        AlertDto created = alertService.createAlertRule(dto, tenantId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<AlertDto> updateAlertRule(
            @PathVariable UUID id,
            @Valid @RequestBody AlertDto dto,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return alertService.updateAlertRule(id, dto, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAlertRule(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        if (alertService.deleteAlertRule(id, tenantId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AlertDto>> checkAlerts(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        List<AlertDto> triggeredAlerts = alertService.checkAlerts(tenantId);
        return ResponseEntity.ok(triggeredAlerts);
    }
}
