package com.healthdata.payer.service;

import com.healthdata.payer.domain.Phase2ExecutionTask;
import com.healthdata.payer.domain.Phase2ExecutionTask.*;
import com.healthdata.payer.repository.Phase2ExecutionTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD tests for Phase2ExecutionService - Financial ROI calculations and monthly summaries.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Phase2 Execution Service Financial Tests")
class Phase2ExecutionServiceTest {

    @Mock
    private Phase2ExecutionTaskRepository taskRepository;

    private Phase2ExecutionService service;

    @BeforeEach
    void setUp() {
        service = new Phase2ExecutionService(taskRepository);
    }

    // ===== ROI Calculation Tests =====

    @Test
    @DisplayName("Should calculate ROI percentage correctly")
    void shouldCalculateROIPercentageCorrectly() {
        // Given
        BigDecimal captured = new BigDecimal("400000.00"); // $400K
        BigDecimal cost = new BigDecimal("50000.00");      // $50K

        // When
        BigDecimal roi = service.calculateROI(captured, cost);

        // Then
        // Formula: (captured - cost) / cost * 100 = (400K - 50K) / 50K * 100 = 700%
        assertThat(roi).isEqualTo(new BigDecimal("700.00"));
    }

    @Test
    @DisplayName("Should handle zero cost by returning zero ROI")
    void shouldHandleZeroCostByReturningZeroROI() {
        // Given
        BigDecimal captured = new BigDecimal("100000.00");
        BigDecimal cost = BigDecimal.ZERO;

        // When
        BigDecimal roi = service.calculateROI(captured, cost);

        // Then
        assertThat(roi).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should handle null cost by returning zero ROI")
    void shouldHandleNullCostByReturningZeroROI() {
        // Given
        BigDecimal captured = new BigDecimal("100000.00");
        BigDecimal cost = null;

        // When
        BigDecimal roi = service.calculateROI(captured, cost);

        // Then
        assertThat(roi).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should use RoundingMode.HALF_UP with 2 decimal places")
    void shouldUseCorrectRoundingMode() {
        // Given - Test rounding: 333.333... should round to 333.33
        BigDecimal captured = new BigDecimal("100000.00");
        BigDecimal cost = new BigDecimal("30000.00");

        // When
        BigDecimal roi = service.calculateROI(captured, cost);

        // Then
        // (100K - 30K) / 30K * 100 = 233.333...
        assertThat(roi).isEqualTo(new BigDecimal("233.33"));
    }

    // ===== Performance Improvement Tests =====

    @Test
    @DisplayName("Should calculate performance improvement correctly")
    void shouldCalculatePerformanceImprovementCorrectly() {
        // Given
        Phase2ExecutionTask task = Phase2ExecutionTask.builder()
                .baselinePerformancePercentage(new BigDecimal("72.00"))
                .currentPerformancePercentage(new BigDecimal("78.50"))
                .build();

        // When
        BigDecimal improvement = service.calculatePerformanceImprovement(task);

        // Then
        // 78.50 - 72.00 = 6.50 percentage points
        assertThat(improvement).isEqualTo(new BigDecimal("6.50"));
    }

    @Test
    @DisplayName("Should handle null baseline by returning zero improvement")
    void shouldHandleNullBaselineByReturningZeroImprovement() {
        // Given
        Phase2ExecutionTask task = Phase2ExecutionTask.builder()
                .baselinePerformancePercentage(null)
                .currentPerformancePercentage(new BigDecimal("78.50"))
                .build();

        // When
        BigDecimal improvement = service.calculatePerformanceImprovement(task);

        // Then
        assertThat(improvement).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should handle null current by returning zero improvement")
    void shouldHandleNullCurrentByReturningZeroImprovement() {
        // Given
        Phase2ExecutionTask task = Phase2ExecutionTask.builder()
                .baselinePerformancePercentage(new BigDecimal("72.00"))
                .currentPerformancePercentage(null)
                .build();

        // When
        BigDecimal improvement = service.calculatePerformanceImprovement(task);

        // Then
        assertThat(improvement).isEqualTo(BigDecimal.ZERO);
    }

    // ===== Monthly Financial Summary Tests =====

    @Test
    @DisplayName("Should calculate monthly financial summary")
    void shouldCalculateMonthlyFinancialSummary() {
        // Given
        String tenantId = "test-tenant-123";
        List<Phase2ExecutionTask> completedTasks = List.of(
            Phase2ExecutionTask.builder()
                    .tenantId(tenantId)
                    .status(TaskStatus.COMPLETED)
                    .qualityBonusCaptured(new BigDecimal("100000.00"))
                    .gapsClosed(50)
                    .roiPercentage(new BigDecimal("250.00"))
                    .build(),
            Phase2ExecutionTask.builder()
                    .tenantId(tenantId)
                    .status(TaskStatus.COMPLETED)
                    .qualityBonusCaptured(new BigDecimal("50000.00"))
                    .gapsClosed(25)
                    .roiPercentage(new BigDecimal("150.00"))
                    .build()
        );

        when(taskRepository.findByTenantIdAndStatus(tenantId, TaskStatus.COMPLETED))
                .thenReturn(completedTasks);

        // When
        Phase2ExecutionService.FinancialSummary summary = service.getMonthlyFinancialSummary(tenantId);

        // Then
        assertThat(summary).isNotNull();
        assertThat(summary.getTenantId()).isEqualTo(tenantId);
        assertThat(summary.getTotalBonusCaptured()).isEqualTo(new BigDecimal("150000.00"));
        assertThat(summary.getTotalGapsClosed()).isEqualTo(75);
        assertThat(summary.getTotalTasksCompleted()).isEqualTo(2);
        assertThat(summary.getAverageROI()).isEqualTo(new BigDecimal("200.00"));
        assertThat(summary.getCalculatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should return zero values for empty task list")
    void shouldReturnZeroValuesForEmptyTaskList() {
        // Given
        String tenantId = "empty-tenant";
        when(taskRepository.findByTenantIdAndStatus(tenantId, TaskStatus.COMPLETED))
                .thenReturn(List.of());

        // When
        Phase2ExecutionService.FinancialSummary summary = service.getMonthlyFinancialSummary(tenantId);

        // Then
        assertThat(summary.getTotalBonusCaptured()).isEqualTo(BigDecimal.ZERO);
        assertThat(summary.getTotalGapsClosed()).isEqualTo(0);
        assertThat(summary.getTotalTasksCompleted()).isEqualTo(0);
        assertThat(summary.getAverageROI()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should handle null values in tasks for financial summary")
    void shouldHandleNullValuesInTasksForFinancialSummary() {
        // Given
        String tenantId = "test-tenant";
        List<Phase2ExecutionTask> tasks = List.of(
            Phase2ExecutionTask.builder()
                    .tenantId(tenantId)
                    .status(TaskStatus.COMPLETED)
                    .qualityBonusCaptured(new BigDecimal("100000.00"))
                    .gapsClosed(50)
                    .roiPercentage(new BigDecimal("200.00"))
                    .build(),
            Phase2ExecutionTask.builder()
                    .tenantId(tenantId)
                    .status(TaskStatus.COMPLETED)
                    .qualityBonusCaptured(null)  // Null bonus
                    .gapsClosed(null)            // Null gaps
                    .roiPercentage(null)         // Null ROI
                    .build()
        );

        when(taskRepository.findByTenantIdAndStatus(tenantId, TaskStatus.COMPLETED))
                .thenReturn(tasks);

        // When
        Phase2ExecutionService.FinancialSummary summary = service.getMonthlyFinancialSummary(tenantId);

        // Then
        assertThat(summary.getTotalBonusCaptured()).isEqualTo(new BigDecimal("100000.00"));
        assertThat(summary.getTotalGapsClosed()).isEqualTo(50);
        assertThat(summary.getTotalTasksCompleted()).isEqualTo(2);
        assertThat(summary.getAverageROI()).isEqualTo(new BigDecimal("200.00"));
    }

    // ===== Highest ROI Intervention Tests =====

    @Test
    @DisplayName("Should identify highest ROI intervention")
    void shouldIdentifyHighestROIIntervention() {
        // Given
        List<Phase2ExecutionTask> tasks = List.of(
            Phase2ExecutionTask.builder()
                    .id("task-1")
                    .taskName("Intervention A")
                    .roiPercentage(new BigDecimal("250.00"))
                    .build(),
            Phase2ExecutionTask.builder()
                    .id("task-2")
                    .taskName("Intervention B")
                    .roiPercentage(new BigDecimal("400.00"))
                    .build(),
            Phase2ExecutionTask.builder()
                    .id("task-3")
                    .taskName("Intervention C")
                    .roiPercentage(new BigDecimal("350.00"))
                    .build()
        );

        // When
        Phase2ExecutionTask highest = service.findHighestROIIntervention(tasks);

        // Then
        assertThat(highest).isNotNull();
        assertThat(highest.getId()).isEqualTo("task-2");
        assertThat(highest.getTaskName()).isEqualTo("Intervention B");
        assertThat(highest.getRoiPercentage()).isEqualTo(new BigDecimal("400.00"));
    }

    @Test
    @DisplayName("Should handle null ROI values when finding highest ROI")
    void shouldHandleNullROIValuesWhenFindingHighestROI() {
        // Given
        List<Phase2ExecutionTask> tasks = List.of(
            Phase2ExecutionTask.builder()
                    .id("task-1")
                    .taskName("Intervention A")
                    .roiPercentage(null)
                    .build(),
            Phase2ExecutionTask.builder()
                    .id("task-2")
                    .taskName("Intervention B")
                    .roiPercentage(new BigDecimal("300.00"))
                    .build(),
            Phase2ExecutionTask.builder()
                    .id("task-3")
                    .taskName("Intervention C")
                    .roiPercentage(null)
                    .build()
        );

        // When
        Phase2ExecutionTask highest = service.findHighestROIIntervention(tasks);

        // Then
        assertThat(highest).isNotNull();
        assertThat(highest.getId()).isEqualTo("task-2");
        assertThat(highest.getRoiPercentage()).isEqualTo(new BigDecimal("300.00"));
    }

    @Test
    @DisplayName("Should return null when list is empty")
    void shouldReturnNullWhenListIsEmpty() {
        // Given
        List<Phase2ExecutionTask> tasks = List.of();

        // When
        Phase2ExecutionTask highest = service.findHighestROIIntervention(tasks);

        // Then
        assertThat(highest).isNull();
    }

    @Test
    @DisplayName("Should return first max when multiple tasks have same ROI")
    void shouldReturnFirstMaxWhenMultipleTasksHaveSameROI() {
        // Given
        List<Phase2ExecutionTask> tasks = List.of(
            Phase2ExecutionTask.builder()
                    .id("task-1")
                    .taskName("Intervention A")
                    .roiPercentage(new BigDecimal("400.00"))
                    .build(),
            Phase2ExecutionTask.builder()
                    .id("task-2")
                    .taskName("Intervention B")
                    .roiPercentage(new BigDecimal("400.00"))
                    .build()
        );

        // When
        Phase2ExecutionTask highest = service.findHighestROIIntervention(tasks);

        // Then
        assertThat(highest).isNotNull();
        assertThat(highest.getId()).isEqualTo("task-1");
    }

    // ===== Draft Case Studies Tests =====

    @Test
    @DisplayName("Should retrieve draft case studies")
    void shouldRetrieveDraftCaseStudies() {
        // Given
        String tenantId = "test-tenant";
        List<Phase2ExecutionTask> draftStudies = List.of(
            Phase2ExecutionTask.builder()
                    .id("task-1")
                    .taskName("Case Study 1")
                    .caseStudyPublished(false)
                    .build(),
            Phase2ExecutionTask.builder()
                    .id("task-2")
                    .taskName("Case Study 2")
                    .caseStudyPublished(false)
                    .build()
        );

        when(taskRepository.findByCaseStudyPublishedAndTenantId(false, tenantId))
                .thenReturn(draftStudies);

        // When
        List<Phase2ExecutionTask> result = service.getDraftCaseStudies(tenantId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> t.getCaseStudyPublished() == false);
    }

    @Test
    @DisplayName("Should return empty list when no draft case studies exist")
    void shouldReturnEmptyListWhenNoDraftCaseStudiesExist() {
        // Given
        String tenantId = "empty-tenant";
        when(taskRepository.findByCaseStudyPublishedAndTenantId(false, tenantId))
                .thenReturn(List.of());

        // When
        List<Phase2ExecutionTask> result = service.getDraftCaseStudies(tenantId);

        // Then
        assertThat(result).isEmpty();
    }

    // ===== Publish Case Study Tests =====

    @Test
    @DisplayName("Should publish case study")
    void shouldPublishCaseStudy() {
        // Given
        String taskId = "task-123";
        String tenantId = "test-tenant";
        Phase2ExecutionTask task = Phase2ExecutionTask.builder()
                .id(taskId)
                .tenantId(tenantId)
                .taskName("Case Study")
                .caseStudyPublished(false)
                .build();

        when(taskRepository.findByIdAndTenantId(taskId, tenantId))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(Phase2ExecutionTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Phase2ExecutionTask published = service.publishCaseStudy(taskId, tenantId);

        // Then
        assertThat(published).isNotNull();
        assertThat(published.getCaseStudyPublished()).isTrue();
        verify(taskRepository).save(task);
    }

    @Test
    @DisplayName("Should throw exception when publishing non-existent case study")
    void shouldThrowExceptionWhenPublishingNonExistentCaseStudy() {
        // Given
        String taskId = "non-existent";
        String tenantId = "test-tenant";
        when(taskRepository.findByIdAndTenantId(taskId, tenantId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.publishCaseStudy(taskId, tenantId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Task not found");
    }
}
