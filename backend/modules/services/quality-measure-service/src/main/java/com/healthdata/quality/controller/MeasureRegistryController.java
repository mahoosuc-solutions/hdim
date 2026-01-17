package com.healthdata.quality.controller;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.quality.measure.MeasureRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Measure Registry Controller
 *
 * Exposes locally registered measures for the demo portal.
 */
@RestController
@RequestMapping("/measures")
@RequiredArgsConstructor
@Slf4j
public class MeasureRegistryController {

    private final MeasureRegistry measureRegistry;

    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/local", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MeasureRegistry.MeasureMetadata>> getLocalMeasures(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.info("GET /measures/local - tenant: {}", tenantId);
        return ResponseEntity.ok(measureRegistry.getMeasuresMetadata());
    }
}
