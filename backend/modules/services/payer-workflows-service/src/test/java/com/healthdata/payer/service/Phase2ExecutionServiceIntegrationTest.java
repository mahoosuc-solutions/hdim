package com.healthdata.payer.service;

import com.healthdata.payer.domain.Phase2ExecutionTask;
import com.healthdata.payer.domain.Phase2ExecutionTask.TaskStatus;
import com.healthdata.payer.repository.Phase2ExecutionTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Phase2ExecutionService financial ROI tracking.
 *
 * Tests the complete flow of financial data persistence and aggregation:
 * - Database persistence of all 11 financial fields
 * - Multi-tenant isolation with tenant_id filtering
 * - Monthly financial summary calculations across multiple tasks
 * - Case study publication status tracking
 *
 * Uses @SpringBootTest with Liquibase migrations enabled for full entity-migration validation.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Phase2 Execution Service Integration Tests - Financial ROI Tracking")
class Phase2ExecutionServiceIntegrationTest {

    @Autowired
    private Phase2ExecutionTaskRepository taskRepository;

    @Autowired
    private Phase2ExecutionService service;

    private static final String TEST_TENANT = "hdim-test-financial";
    private static final String SECONDARY_TENANT = "hdim-test-secondary";

    @BeforeEach
    void setUp() {
        // Clean up test data before each test
        taskRepository.deleteAll();
    }

    // ===== Test Case 1: Create Task with Financial Fields =====

    @Test
    @Transactional
    @DisplayName("Should create task with all financial fields and persist to database")
    void shouldCreateTaskWithFinancialFields() {
        // GIVEN: Create task with complete financial data
        Phase2ExecutionTask task = Phase2ExecutionTask.builder()
                .tenantId(TEST_TENANT)
                .taskName("BCS Improvement Campaign")
                .status(TaskStatus.COMPLETED)
                .priority(Phase2ExecutionTask.TaskPriority.HIGH)
                .hediseMeasure("BCS")
                .baselinePerformancePercentage(new BigDecimal("72.00"))
                .currentPerformancePercentage(new BigDecimal("78.50"))
                .qualityBonusAtRisk(new BigDecimal("8000000.00"))
                .qualityBonusCaptured(new BigDecimal("400000.00"))
                .interventionType("pre_visit_briefing")
                .gapsClosed(127)
                .costPerGap(new BigDecimal("125.00"))
                .roiPercentage(new BigDecimal("640.00"))
                .customerQuote("This solution saved us significant time and money")
                .caseStudyPublished(false)
                .targetDueDate(Instant.now())
                .build();

        // WHEN: Save to database
        Phase2ExecutionTask saved = taskRepository.save(task);
        taskRepository.flush();

        // THEN: Verify all financial fields persisted correctly
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getHediseMeasure()).isEqualTo("BCS");
        assertThat(saved.getBaselinePerformancePercentage()).isEqualByComparingTo(new BigDecimal("72.00"));
        assertThat(saved.getCurrentPerformancePercentage()).isEqualByComparingTo(new BigDecimal("78.50"));
        assertThat(saved.getQualityBonusAtRisk()).isEqualByComparingTo(new BigDecimal("8000000.00"));
        assertThat(saved.getQualityBonusCaptured()).isEqualByComparingTo(new BigDecimal("400000.00"));
        assertThat(saved.getInterventionType()).isEqualTo("pre_visit_briefing");
        assertThat(saved.getGapsClosed()).isEqualTo(127);
        assertThat(saved.getCostPerGap()).isEqualByComparingTo(new BigDecimal("125.00"));
        assertThat(saved.getRoiPercentage()).isEqualByComparingTo(new BigDecimal("640.00"));
        assertThat(saved.getCustomerQuote()).isEqualTo("This solution saved us significant time and money");
        assertThat(saved.getCaseStudyPublished()).isFalse();

        // VERIFY: Read back from database to ensure persistence
        Optional<Phase2ExecutionTask> retrieved = taskRepository.findByIdAndTenantId(saved.getId(), TEST_TENANT);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getRoiPercentage()).isEqualByComparingTo(new BigDecimal("640.00"));
        assertThat(retrieved.get().getQualityBonusCaptured()).isEqualByComparingTo(new BigDecimal("400000.00"));
    }

    // ===== Test Case 2: Calculate Monthly Financial Summary =====

    @Test
    @Transactional
    @DisplayName("Should calculate monthly financial summary across multiple completed tasks")
    void shouldCalculateMonthlyFinancialSummaryAcrossTasks() {
        // GIVEN: Create multiple completed tasks with financial data
        createCompletedTask("BCS", new BigDecimal("150000.00"), 50, new BigDecimal("250.00"));
        createCompletedTask("CDC", new BigDecimal("250000.00"), 77, new BigDecimal("500.00"));
        createCompletedTask("COL", new BigDecimal("100000.00"), 40, new BigDecimal("300.00"));

        // WHEN: Get monthly financial summary for tenant
        Phase2ExecutionService.FinancialSummary summary = service.getMonthlyFinancialSummary(TEST_TENANT);

        // THEN: Verify aggregation is correct
        assertThat(summary).isNotNull();
        assertThat(summary.getTenantId()).isEqualTo(TEST_TENANT);

        // Total bonus: 150K + 250K + 100K = 500K
        assertThat(summary.getTotalBonusCaptured()).isEqualByComparingTo(new BigDecimal("500000.00"));

        // Total gaps: 50 + 77 + 40 = 167
        assertThat(summary.getTotalGapsClosed()).isEqualTo(167);

        // Total tasks: 3
        assertThat(summary.getTotalTasksCompleted()).isEqualTo(3);

        // Average ROI: (250 + 500 + 300) / 3 = 350
        assertThat(summary.getAverageROI()).isEqualByComparingTo(new BigDecimal("350.00"));

        // Timestamp should be recent
        assertThat(summary.getCalculatedAt()).isNotNull();
        assertThat(summary.getCalculatedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @Transactional
    @DisplayName("Should exclude incomplete tasks from financial summary")
    void shouldExcludeIncompleteTasksFromFinancialSummary() {
        // GIVEN: Mix of completed and incomplete tasks
        createCompletedTask("BCS", new BigDecimal("100000.00"), 50, new BigDecimal("200.00"));

        // Create incomplete task (should be ignored)
        Phase2ExecutionTask pendingTask = Phase2ExecutionTask.builder()
                .tenantId(TEST_TENANT)
                .taskName("Incomplete Task")
                .status(TaskStatus.PENDING)
                .priority(Phase2ExecutionTask.TaskPriority.MEDIUM)
                .qualityBonusCaptured(new BigDecimal("999999999.00"))  // Large number, should be ignored
                .gapsClosed(999999)  // Should be ignored
                .roiPercentage(new BigDecimal("999.00"))  // Should be ignored
                .targetDueDate(Instant.now())
                .build();
        taskRepository.save(pendingTask);

        // WHEN: Get monthly financial summary
        Phase2ExecutionService.FinancialSummary summary = service.getMonthlyFinancialSummary(TEST_TENANT);

        // THEN: Only completed task should be included
        assertThat(summary.getTotalBonusCaptured()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(summary.getTotalGapsClosed()).isEqualTo(50);
        assertThat(summary.getTotalTasksCompleted()).isEqualTo(1);
        assertThat(summary.getAverageROI()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    @Transactional
    @DisplayName("Should return zero values for tenant with no completed tasks")
    void shouldReturnZeroValuesForEmptyTenant() {
        // GIVEN: No tasks for tenant

        // WHEN: Get monthly financial summary
        Phase2ExecutionService.FinancialSummary summary = service.getMonthlyFinancialSummary(TEST_TENANT);

        // THEN: Verify zero values
        assertThat(summary.getTotalBonusCaptured()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTotalGapsClosed()).isEqualTo(0);
        assertThat(summary.getTotalTasksCompleted()).isEqualTo(0);
        assertThat(summary.getAverageROI()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @Transactional
    @DisplayName("Should isolate financial summaries by tenant")
    void shouldIsolateFinancialSummariesByTenant() {
        // GIVEN: Create tasks for two different tenants
        createCompletedTaskForTenant(TEST_TENANT, "BCS", new BigDecimal("100000.00"), 50, new BigDecimal("200.00"));
        createCompletedTaskForTenant(SECONDARY_TENANT, "BCS", new BigDecimal("500000.00"), 100, new BigDecimal("400.00"));

        // WHEN: Get summary for each tenant
        Phase2ExecutionService.FinancialSummary summary1 = service.getMonthlyFinancialSummary(TEST_TENANT);
        Phase2ExecutionService.FinancialSummary summary2 = service.getMonthlyFinancialSummary(SECONDARY_TENANT);

        // THEN: Each tenant sees only their own data
        assertThat(summary1.getTotalBonusCaptured()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(summary2.getTotalBonusCaptured()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(summary1.getTotalGapsClosed()).isEqualTo(50);
        assertThat(summary2.getTotalGapsClosed()).isEqualTo(100);
    }

    // ===== Test Case 3: Publish Case Study =====

    @Test
    @Transactional
    @DisplayName("Should publish case study by updating published flag")
    void shouldPublishCaseStudy() {
        // GIVEN: Create unpublished case study
        Phase2ExecutionTask task = createCompletedTask("COL", new BigDecimal("100000.00"), 40, new BigDecimal("300.00"));
        assertThat(task.getCaseStudyPublished()).isFalse();

        // WHEN: Publish the case study
        Phase2ExecutionTask published = service.publishCaseStudy(task.getId(), TEST_TENANT);

        // THEN: Verify published status is updated
        assertThat(published.getCaseStudyPublished()).isTrue();

        // VERIFY: Confirm persistence by reading from repository
        Optional<Phase2ExecutionTask> retrieved = taskRepository.findByIdAndTenantId(task.getId(), TEST_TENANT);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getCaseStudyPublished()).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("Should throw exception when publishing non-existent case study")
    void shouldThrowExceptionWhenPublishingNonExistentCaseStudy() {
        // GIVEN: Non-existent task ID

        // WHEN & THEN: Publishing should throw exception
        assertThatThrownBy(() -> service.publishCaseStudy("non-existent-id", TEST_TENANT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Task not found");
    }

    @Test
    @Transactional
    @DisplayName("Should retrieve published case studies for tenant")
    void shouldRetrievePublishedCaseStudiesForTenant() {
        // GIVEN: Create mix of published and unpublished case studies
        Phase2ExecutionTask task1 = createCompletedTask("BCS", new BigDecimal("100000.00"), 50, new BigDecimal("200.00"));
        Phase2ExecutionTask task2 = createCompletedTask("CDC", new BigDecimal("200000.00"), 75, new BigDecimal("300.00"));
        Phase2ExecutionTask task3 = createCompletedTask("COL", new BigDecimal("150000.00"), 60, new BigDecimal("250.00"));

        // Publish only first two
        service.publishCaseStudy(task1.getId(), TEST_TENANT);
        service.publishCaseStudy(task2.getId(), TEST_TENANT);

        // WHEN: Get published case studies
        var publishedStudies = service.getPublishedCaseStudies(TEST_TENANT);

        // THEN: Verify only published studies are returned
        assertThat(publishedStudies).hasSize(2);
        assertThat(publishedStudies).allMatch(t -> t.getCaseStudyPublished() == true);
    }

    @Test
    @Transactional
    @DisplayName("Should retrieve draft (unpublished) case studies for tenant")
    void shouldRetrieveDraftCaseStudiesForTenant() {
        // GIVEN: Create mix of published and unpublished case studies
        Phase2ExecutionTask task1 = createCompletedTask("BCS", new BigDecimal("100000.00"), 50, new BigDecimal("200.00"));
        Phase2ExecutionTask task2 = createCompletedTask("CDC", new BigDecimal("200000.00"), 75, new BigDecimal("300.00"));
        Phase2ExecutionTask task3 = createCompletedTask("COL", new BigDecimal("150000.00"), 60, new BigDecimal("250.00"));

        // Publish only first two
        service.publishCaseStudy(task1.getId(), TEST_TENANT);
        service.publishCaseStudy(task2.getId(), TEST_TENANT);

        // WHEN: Get draft case studies
        var draftStudies = service.getDraftCaseStudies(TEST_TENANT);

        // THEN: Verify only unpublished studies are returned
        assertThat(draftStudies).hasSize(1);
        assertThat(draftStudies).allMatch(t -> t.getCaseStudyPublished() == false);
        assertThat(draftStudies.get(0).getTaskName()).contains("COL");
    }

    // ===== Test Case 4: Financial Field Data Types & Precision =====

    @Test
    @Transactional
    @DisplayName("Should preserve BigDecimal precision for financial fields")
    void shouldPreserveBigDecimalPrecisionForFinancialFields() {
        // GIVEN: Create task with high-precision financial values
        Phase2ExecutionTask task = Phase2ExecutionTask.builder()
                .tenantId(TEST_TENANT)
                .taskName("Precision Test")
                .status(TaskStatus.COMPLETED)
                .priority(Phase2ExecutionTask.TaskPriority.MEDIUM)
                .baselinePerformancePercentage(new BigDecimal("72.12"))
                .currentPerformancePercentage(new BigDecimal("78.56"))
                .qualityBonusAtRisk(new BigDecimal("8000000.00"))
                .qualityBonusCaptured(new BigDecimal("400000.99"))
                .costPerGap(new BigDecimal("125.49"))
                .roiPercentage(new BigDecimal("640.12"))
                .targetDueDate(Instant.now())
                .build();

        // WHEN: Save and retrieve
        Phase2ExecutionTask saved = taskRepository.save(task);
        Optional<Phase2ExecutionTask> retrieved = taskRepository.findByIdAndTenantId(saved.getId(), TEST_TENANT);

        // THEN: Verify precision is maintained
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getBaselinePerformancePercentage()).isEqualByComparingTo(new BigDecimal("72.12"));
        assertThat(retrieved.get().getCurrentPerformancePercentage()).isEqualByComparingTo(new BigDecimal("78.56"));
        assertThat(retrieved.get().getQualityBonusCaptured()).isEqualByComparingTo(new BigDecimal("400000.99"));
        assertThat(retrieved.get().getCostPerGap()).isEqualByComparingTo(new BigDecimal("125.49"));
        assertThat(retrieved.get().getRoiPercentage()).isEqualByComparingTo(new BigDecimal("640.12"));
    }

    @Test
    @Transactional
    @DisplayName("Should handle null values in optional financial fields")
    void shouldHandleNullValuesInOptionalFinancialFields() {
        // GIVEN: Create task with null optional fields
        Phase2ExecutionTask task = Phase2ExecutionTask.builder()
                .tenantId(TEST_TENANT)
                .taskName("Null Fields Test")
                .status(TaskStatus.COMPLETED)
                .priority(Phase2ExecutionTask.TaskPriority.MEDIUM)
                .hediseMeasure("BCS")
                .qualityBonusCaptured(new BigDecimal("100000.00"))
                .gapsClosed(50)
                .roiPercentage(new BigDecimal("200.00"))
                // Leave these null
                .baselinePerformancePercentage(null)
                .currentPerformancePercentage(null)
                .qualityBonusAtRisk(null)
                .interventionType(null)
                .costPerGap(null)
                .customerQuote(null)
                .caseStudyPublished(null)
                .targetDueDate(Instant.now())
                .build();

        // WHEN: Save and retrieve
        Phase2ExecutionTask saved = taskRepository.save(task);
        Optional<Phase2ExecutionTask> retrieved = taskRepository.findByIdAndTenantId(saved.getId(), TEST_TENANT);

        // THEN: Verify task persists with null values
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getBaselinePerformancePercentage()).isNull();
        assertThat(retrieved.get().getCurrentPerformancePercentage()).isNull();
        assertThat(retrieved.get().getQualityBonusAtRisk()).isNull();
        assertThat(retrieved.get().getInterventionType()).isNull();
        assertThat(retrieved.get().getCostPerGap()).isNull();
        assertThat(retrieved.get().getCustomerQuote()).isNull();
        // Still have the required fields
        assertThat(retrieved.get().getQualityBonusCaptured()).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    // ===== Helper Methods =====

    /**
     * Helper to create a completed task with financial data for the default test tenant
     */
    private Phase2ExecutionTask createCompletedTask(
            String measure,
            BigDecimal capturedAmount,
            int gapsClosed,
            BigDecimal roiPercentage) {
        return createCompletedTaskForTenant(TEST_TENANT, measure, capturedAmount, gapsClosed, roiPercentage);
    }

    /**
     * Helper to create a completed task with financial data for a specific tenant
     */
    private Phase2ExecutionTask createCompletedTaskForTenant(
            String tenantId,
            String measure,
            BigDecimal capturedAmount,
            int gapsClosed,
            BigDecimal roiPercentage) {
        Phase2ExecutionTask task = Phase2ExecutionTask.builder()
                .tenantId(tenantId)
                .taskName(measure + " Improvement Campaign")
                .status(TaskStatus.COMPLETED)
                .priority(Phase2ExecutionTask.TaskPriority.HIGH)
                .hediseMeasure(measure)
                .baselinePerformancePercentage(new BigDecimal("70.00"))
                .currentPerformancePercentage(new BigDecimal("75.00"))
                .qualityBonusAtRisk(new BigDecimal("2000000.00"))
                .qualityBonusCaptured(capturedAmount)
                .interventionType("intervention_" + measure.toLowerCase())
                .gapsClosed(gapsClosed)
                .costPerGap(new BigDecimal("100.00"))
                .roiPercentage(roiPercentage)
                .caseStudyPublished(false)
                .targetDueDate(Instant.now())
                .completedDate(Instant.now())
                .build();

        return taskRepository.save(task);
    }
}
