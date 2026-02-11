package com.healthdata.payer.controller;

import com.healthdata.payer.domain.Phase2ExecutionTask;
import com.healthdata.payer.domain.Phase2ExecutionTask.*;
import com.healthdata.payer.dto.CaseStudyResponse;
import com.healthdata.payer.dto.MeasureROIResponse;
import com.healthdata.payer.service.Phase2ExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Phase 2 Execution Task Management API
 *
 * RESTful endpoints for scheduling, tracking, and completing
 * the March 2026 go-to-market execution plan.
 */
@RestController
@RequestMapping("/api/v1/payer/phase2-execution")
@RequiredArgsConstructor
@Tag(name = "Phase 2 Execution", description = "GTM Execution Task Management")
public class Phase2ExecutionController {
    private final Phase2ExecutionService executionService;

    // ===== Create =====

    @PostMapping("/tasks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create a new Phase 2 execution task")
    public ResponseEntity<Phase2ExecutionTask> createTask(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody CreateTaskRequest request) {

        Phase2ExecutionTask task = executionService.createTask(
                tenantId,
                request.getTaskName(),
                request.getDescription(),
                request.getCategory(),
                request.getTargetDueDate(),
                request.getPriority(),
                request.getOwnerName(),
                request.getOwnerRole());

        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    // ===== Read - Dashboard =====

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIEWER')")
    @Operation(summary = "Get Phase 2 execution dashboard summary")
    public ResponseEntity<Phase2ExecutionService.Phase2DashboardSummary> getDashboard(
            @RequestHeader("X-Tenant-ID") String tenantId) {

        var summary = executionService.getDashboardSummary(tenantId);
        return ResponseEntity.ok(summary);
    }

    // ===== Read - By Category =====

    @GetMapping("/tasks/category/{category}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIEWER')")
    @Operation(summary = "Get Phase 2 tasks by category")
    public ResponseEntity<Page<Phase2ExecutionTask>> getTasksByCategory(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable TaskCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);
        var tasks = executionService.getTasksByCategory(tenantId, category, pageable);
        return ResponseEntity.ok(tasks);
    }

    // ===== Read - By Status =====

    @GetMapping("/tasks/status/{status}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIEWER')")
    @Operation(summary = "Get Phase 2 tasks by status")
    public ResponseEntity<Page<Phase2ExecutionTask>> getTasksByStatus(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable TaskStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);
        var tasks = executionService.getTasksByStatus(tenantId, status, pageable);
        return ResponseEntity.ok(tasks);
    }

    // ===== Read - By Week =====

    @GetMapping("/tasks/week/{week}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIEWER')")
    @Operation(summary = "Get Phase 2 tasks for a specific week")
    public ResponseEntity<List<Phase2ExecutionTask>> getTasksByWeek(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable Integer week) {

        var tasks = executionService.getTasksByTenantAndWeek(tenantId, week);
        return ResponseEntity.ok(tasks);
    }

    // ===== Read - Open Tasks =====

    @GetMapping("/tasks/open")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIEWER')")
    @Operation(summary = "Get all open Phase 2 tasks")
    public ResponseEntity<List<Phase2ExecutionTask>> getOpenTasks(
            @RequestHeader("X-Tenant-ID") String tenantId) {

        var tasks = executionService.getOpenTasks(tenantId);
        return ResponseEntity.ok(tasks);
    }

    // ===== Update - Status & Progress =====

    @PatchMapping("/tasks/{taskId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update task status and progress")
    public ResponseEntity<Phase2ExecutionTask> updateTaskStatus(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String taskId,
            @RequestBody UpdateStatusRequest request) {

        var task = executionService.updateTaskStatus(
                taskId,
                tenantId,
                request.getStatus(),
                request.getProgressPercentage());

        return ResponseEntity.ok(task);
    }

    // ===== Update - Complete Task =====

    @PostMapping("/tasks/{taskId}/complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Mark task as complete with outcomes")
    public ResponseEntity<Phase2ExecutionTask> completeTask(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String taskId,
            @RequestBody CompleteTaskRequest request) {

        var task = executionService.completeTask(
                taskId,
                tenantId,
                request.getActualOutcomes());

        return ResponseEntity.ok(task);
    }

    // ===== Update - Block Task =====

    @PostMapping("/tasks/{taskId}/block")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Block a task with unblock date")
    public ResponseEntity<Phase2ExecutionTask> blockTask(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String taskId,
            @RequestBody BlockTaskRequest request) {

        var task = executionService.blockTask(
                taskId,
                tenantId,
                request.getBlockReason(),
                request.getUnblockDate());

        return ResponseEntity.ok(task);
    }

    // ===== Update - Unblock Task =====

    @PostMapping("/tasks/{taskId}/unblock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Unblock a task")
    public ResponseEntity<Phase2ExecutionTask> unblockTask(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String taskId) {

        var task = executionService.unblockTask(taskId, tenantId);
        return ResponseEntity.ok(task);
    }

    // ===== Update - Add Note =====

    @PostMapping("/tasks/{taskId}/notes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Add a note to a task")
    public ResponseEntity<Phase2ExecutionTask> addNote(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String taskId,
            @RequestBody AddNoteRequest request) {

        var task = executionService.addNote(taskId, tenantId, request.getNote());
        return ResponseEntity.ok(task);
    }

    // ===== Dependencies =====

    @GetMapping("/tasks/{taskId}/blocked-by")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIEWER')")
    @Operation(summary = "Get tasks blocked by a specific task")
    public ResponseEntity<List<Phase2ExecutionTask>> getBlockedByTask(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String taskId) {

        var tasks = executionService.getBlockedByTask(taskId, tenantId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/tasks/{taskId}/blocking")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIEWER')")
    @Operation(summary = "Get tasks blocking a specific task")
    public ResponseEntity<List<Phase2ExecutionTask>> getBlockingTasks(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String taskId) {

        var tasks = executionService.getBlockingTasks(taskId, tenantId);
        return ResponseEntity.ok(tasks);
    }

    // ===== Financial Dashboard & Case Studies =====

    @GetMapping("/financial/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Operation(summary = "Get financial dashboard with aggregated ROI metrics")
    public ResponseEntity<Phase2ExecutionService.FinancialSummary> getFinancialDashboard(
            @RequestHeader("X-Tenant-ID") String tenantId) {

        var summary = executionService.getMonthlyFinancialSummary(tenantId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/financial/by-measure")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Operation(summary = "Get ROI analysis broken down by HEDIS measure")
    public ResponseEntity<List<MeasureROIResponse>> getMeasureROIAnalysis(
            @RequestHeader("X-Tenant-ID") String tenantId) {

        // Aggregate tasks by measure
        List<MeasureROIResponse> measures = new ArrayList<>();

        // Iterate through common HEDIS measures
        for (String measure : List.of("BCS", "CDC", "COL", "CWP", "DM")) {
            List<Phase2ExecutionTask> tasks = executionService.getTasksByMeasure(measure, tenantId);

            // Only include measures with captured revenue
            if (tasks.isEmpty()) {
                continue;
            }

            BigDecimal totalCaptured = BigDecimal.ZERO;
            Integer totalGapsClosed = 0;

            for (Phase2ExecutionTask task : tasks) {
                if (task.getQualityBonusCaptured() != null) {
                    totalCaptured = totalCaptured.add(task.getQualityBonusCaptured());
                }
                if (task.getGapsClosed() != null) {
                    totalGapsClosed += task.getGapsClosed();
                }
            }

            // Only add measure if there's captured revenue
            if (totalCaptured.compareTo(BigDecimal.ZERO) > 0) {
                measures.add(MeasureROIResponse.builder()
                        .measure(measure)
                        .totalCaptured(totalCaptured)
                        .totalGapsClosed(totalGapsClosed)
                        .taskCount(tasks.size())
                        .build());
            }
        }

        return ResponseEntity.ok(measures);
    }

    @GetMapping("/case-studies")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Operation(summary = "Get case studies (published or draft)")
    public ResponseEntity<List<CaseStudyResponse>> getCaseStudies(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "false") Boolean published) {

        List<Phase2ExecutionTask> caseStudies = published
                ? executionService.getPublishedCaseStudies(tenantId)
                : executionService.getDraftCaseStudies(tenantId);

        List<CaseStudyResponse> responses = caseStudies.stream()
                .map(this::mapToCaseStudyResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/case-studies/{caseStudyId}/publish")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Publish a case study")
    public ResponseEntity<CaseStudyResponse> publishCaseStudy(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String caseStudyId) {

        Phase2ExecutionTask task = executionService.publishCaseStudy(caseStudyId, tenantId);
        return ResponseEntity.ok(mapToCaseStudyResponse(task));
    }

    // ===== Helper Methods =====

    private CaseStudyResponse mapToCaseStudyResponse(Phase2ExecutionTask task) {
        return CaseStudyResponse.builder()
                .id(task.getId())
                .taskName(task.getTaskName())
                .hediseMeasure(task.getHediseMeasure())
                .baselinePerformance(task.getBaselinePerformancePercentage())
                .currentPerformance(task.getCurrentPerformancePercentage())
                .bonusCaptured(task.getQualityBonusCaptured())
                .gapsClosed(task.getGapsClosed())
                .customerQuote(task.getCustomerQuote())
                .published(task.getCaseStudyPublished())
                .build();
    }

    // ===== Request DTOs =====

    @lombok.Data
    public static class CreateTaskRequest {
        private String taskName;
        private String description;
        private TaskCategory category;
        private Instant targetDueDate;
        private TaskPriority priority;
        private String ownerName;
        private String ownerRole;
    }

    @lombok.Data
    public static class UpdateStatusRequest {
        private TaskStatus status;
        private Integer progressPercentage;
    }

    @lombok.Data
    public static class CompleteTaskRequest {
        private String actualOutcomes;
    }

    @lombok.Data
    public static class BlockTaskRequest {
        private String blockReason;
        private Instant unblockDate;
    }

    @lombok.Data
    public static class AddNoteRequest {
        private String note;
    }
}
