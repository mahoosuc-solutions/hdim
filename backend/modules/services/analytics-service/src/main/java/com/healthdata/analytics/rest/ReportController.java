package com.healthdata.analytics.rest;

import com.healthdata.analytics.dto.ReportDto;
import com.healthdata.analytics.dto.ReportExecutionDto;
import com.healthdata.analytics.service.ReportService;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<ReportDto>> getReports(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(reportService.getReports(tenantId));
    }

    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<Page<ReportDto>> getReportsPaginated(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(reportService.getReportsPaginated(tenantId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<ReportDto> getReport(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return reportService.getReport(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<ReportDto> createReport(
            @Valid @RequestBody ReportDto dto,
            @RequestHeader("X-Tenant-ID") String tenantId,
            Authentication authentication) {
        String userId = authentication.getName();
        ReportDto created = reportService.createReport(dto, tenantId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<ReportDto> updateReport(
            @PathVariable UUID id,
            @Valid @RequestBody ReportDto dto,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return reportService.updateReport(id, dto, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReport(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        if (reportService.deleteReport(id, tenantId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<ReportExecutionDto> executeReport(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody(required = false) Map<String, Object> parameters,
            Authentication authentication) {
        String userId = authentication.getName();
        ReportExecutionDto execution = reportService.executeReport(id, tenantId, userId, parameters);
        return ResponseEntity.accepted().body(execution);
    }

    @GetMapping("/{id}/executions")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<ReportExecutionDto>> getExecutions(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(reportService.getExecutions(id, tenantId));
    }

    @GetMapping("/{id}/executions/paginated")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<Page<ReportExecutionDto>> getExecutionsPaginated(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(reportService.getExecutionsPaginated(id, tenantId, pageable));
    }

    @GetMapping("/executions/{executionId}")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<ReportExecutionDto> getExecution(
            @PathVariable UUID executionId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return reportService.getExecution(executionId, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
