package com.healthdata.payer.controller;

import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import com.healthdata.payer.domain.Phase2ExecutionTask;
import com.healthdata.payer.domain.Phase2ExecutionTask.*;
import com.healthdata.payer.service.Phase2ExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD tests for Phase2ExecutionController Financial Dashboard endpoints.
 * Tests the 4 financial dashboard REST endpoints for ROI tracking and case studies.
 *
 * Note: These are integration tests that require a full Spring Boot context
 * with mocked services. Run with testFast or testIntegration Gradle tasks.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@DisplayName("Phase 2 Financial Dashboard Controller Tests")
@Tag("integration")
class Phase2ExecutionControllerFinancialTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private Phase2ExecutionService executionService;

    private String tenantId = "hdim-test";
    private Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        // Setup default mocks
    }

    // ===== GET /financial/dashboard =====

    @Test
    @DisplayName("GET /financial/dashboard - Should return financial dashboard summary")
    void shouldReturnFinancialDashboard() throws Exception {
        // Given
        Phase2ExecutionService.FinancialSummary summary = Phase2ExecutionService.FinancialSummary.builder()
                .tenantId(tenantId)
                .totalBonusCaptured(new BigDecimal("400000.00"))
                .totalGapsClosed(127)
                .totalTasksCompleted(5)
                .averageROI(new BigDecimal("640.00"))
                .calculatedAt(now)
                .build();

        when(executionService.getMonthlyFinancialSummary(tenantId)).thenReturn(summary);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/phase2-execution/financial/dashboard")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tenantId").value(tenantId))
                .andExpect(jsonPath("$.totalBonusCaptured").value(400000.00))
                .andExpect(jsonPath("$.totalGapsClosed").value(127))
                .andExpect(jsonPath("$.totalTasksCompleted").value(5))
                .andExpect(jsonPath("$.averageROI").value(640.00))
                .andExpect(jsonPath("$.calculatedAt").exists());
    }

    @Test
    @DisplayName("GET /financial/dashboard - Should handle zero metrics")
    void shouldReturnFinancialDashboardWithZeroMetrics() throws Exception {
        // Given - Empty tenant with no completed tasks
        Phase2ExecutionService.FinancialSummary summary = Phase2ExecutionService.FinancialSummary.builder()
                .tenantId(tenantId)
                .totalBonusCaptured(BigDecimal.ZERO)
                .totalGapsClosed(0)
                .totalTasksCompleted(0)
                .averageROI(BigDecimal.ZERO)
                .calculatedAt(now)
                .build();

        when(executionService.getMonthlyFinancialSummary(tenantId)).thenReturn(summary);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/phase2-execution/financial/dashboard")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBonusCaptured").value(0.00))
                .andExpect(jsonPath("$.totalGapsClosed").value(0))
                .andExpect(jsonPath("$.totalTasksCompleted").value(0))
                .andExpect(jsonPath("$.averageROI").value(0.00));
    }

    // ===== GET /financial/by-measure =====

    @Test
    @DisplayName("GET /financial/by-measure - Should return ROI analysis by measure")
    void shouldReturnMeasureROIAnalysis() throws Exception {
        // Given - Mock tasks for different HEDIS measures
        List<Phase2ExecutionTask> bcsTasksHi = List.of(
                createTask("BCS", new BigDecimal("75000.00"), 25),
                createTask("BCS", new BigDecimal("75000.00"), 25)
        );
        List<Phase2ExecutionTask> cdcTasksHi = List.of(
                createTask("CDC", new BigDecimal("125000.00"), 40),
                createTask("CDC", new BigDecimal("125000.00"), 37)
        );
        List<Phase2ExecutionTask> colTasksHi = List.of();
        List<Phase2ExecutionTask> cwpTasksHi = List.of();
        List<Phase2ExecutionTask> dmTasksHi = List.of();

        when(executionService.getTasksByMeasure("BCS", tenantId)).thenReturn(bcsTasksHi);
        when(executionService.getTasksByMeasure("CDC", tenantId)).thenReturn(cdcTasksHi);
        when(executionService.getTasksByMeasure("COL", tenantId)).thenReturn(colTasksHi);
        when(executionService.getTasksByMeasure("CWP", tenantId)).thenReturn(cwpTasksHi);
        when(executionService.getTasksByMeasure("DM", tenantId)).thenReturn(dmTasksHi);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/phase2-execution/financial/by-measure")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2)) // Only BCS and CDC have tasks
                .andExpect(jsonPath("$[0].measure").value("BCS"))
                .andExpect(jsonPath("$[0].totalCaptured").value(150000.00))
                .andExpect(jsonPath("$[0].totalGapsClosed").value(50))
                .andExpect(jsonPath("$[0].taskCount").value(2))
                .andExpect(jsonPath("$[1].measure").value("CDC"))
                .andExpect(jsonPath("$[1].totalCaptured").value(250000.00))
                .andExpect(jsonPath("$[1].totalGapsClosed").value(77))
                .andExpect(jsonPath("$[1].taskCount").value(2));
    }

    @Test
    @DisplayName("GET /financial/by-measure - Should filter out measures with no captured revenue")
    void shouldFilterEmptyMeasures() throws Exception {
        // Given - Only one measure with revenue
        when(executionService.getTasksByMeasure("BCS", tenantId))
                .thenReturn(List.of(createTask("BCS", new BigDecimal("100000.00"), 30)));
        when(executionService.getTasksByMeasure("CDC", tenantId)).thenReturn(List.of());
        when(executionService.getTasksByMeasure("COL", tenantId)).thenReturn(List.of());
        when(executionService.getTasksByMeasure("CWP", tenantId)).thenReturn(List.of());
        when(executionService.getTasksByMeasure("DM", tenantId)).thenReturn(List.of());

        // When/Then
        mockMvc.perform(get("/api/v1/payer/phase2-execution/financial/by-measure")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].measure").value("BCS"))
                .andExpect(jsonPath("$[0].totalCaptured").value(100000.00));
    }

    // ===== GET /case-studies =====

    @Test
    @DisplayName("GET /case-studies?published=false - Should return draft case studies")
    void shouldReturnDraftCaseStudies() throws Exception {
        // Given
        List<Phase2ExecutionTask> draftCaseStudies = List.of(
                createCaseStudy("task-1", "BCS Care Gap Closure", "BCS", false),
                createCaseStudy("task-2", "CDC Screening Program", "CDC", false)
        );

        when(executionService.getDraftCaseStudies(tenantId)).thenReturn(draftCaseStudies);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/phase2-execution/case-studies")
                .header("X-Tenant-ID", tenantId)
                .param("published", "false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("task-1"))
                .andExpect(jsonPath("$[0].taskName").value("BCS Care Gap Closure"))
                .andExpect(jsonPath("$[0].hediseMeasure").value("BCS"))
                .andExpect(jsonPath("$[0].published").value(false))
                .andExpect(jsonPath("$[1].id").value("task-2"))
                .andExpect(jsonPath("$[1].hediseMeasure").value("CDC"));
    }

    @Test
    @DisplayName("GET /case-studies?published=true - Should return published case studies")
    void shouldReturnPublishedCaseStudies() throws Exception {
        // Given
        List<Phase2ExecutionTask> publishedCaseStudies = List.of(
                createCaseStudy("task-3", "Award-Winning Intervention", "BCS", true),
                createCaseStudy("task-4", "Preventive Care Success", "CDC", true)
        );

        when(executionService.getPublishedCaseStudies(tenantId)).thenReturn(publishedCaseStudies);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/phase2-execution/case-studies")
                .header("X-Tenant-ID", tenantId)
                .param("published", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].published").value(true))
                .andExpect(jsonPath("$[1].published").value(true));
    }

    @Test
    @DisplayName("GET /case-studies - Should default to draft case studies when published param omitted")
    void shouldDefaultToDraftCaseStudies() throws Exception {
        // Given
        List<Phase2ExecutionTask> draftCaseStudies = List.of(
                createCaseStudy("task-1", "BCS Care Gap Closure", "BCS", false)
        );

        when(executionService.getDraftCaseStudies(tenantId)).thenReturn(draftCaseStudies);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/phase2-execution/case-studies")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].published").value(false));
    }

    @Test
    @DisplayName("GET /case-studies - Should return empty list when no case studies exist")
    void shouldReturnEmptyCaseStudiesList() throws Exception {
        // Given
        when(executionService.getDraftCaseStudies(tenantId)).thenReturn(List.of());

        // When/Then
        mockMvc.perform(get("/api/v1/payer/phase2-execution/case-studies")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ===== POST /case-studies/{id}/publish =====

    @Test
    @DisplayName("POST /case-studies/{caseStudyId}/publish - Should publish a case study")
    void shouldPublishCaseStudy() throws Exception {
        // Given
        String caseStudyId = "task-1";
        Phase2ExecutionTask publishedTask = createCaseStudy(caseStudyId, "BCS Care Gap Closure", "BCS", true);

        when(executionService.publishCaseStudy(caseStudyId, tenantId)).thenReturn(publishedTask);

        // When/Then
        mockMvc.perform(post("/api/v1/payer/phase2-execution/case-studies/{caseStudyId}/publish", caseStudyId)
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(caseStudyId))
                .andExpect(jsonPath("$.taskName").value("BCS Care Gap Closure"))
                .andExpect(jsonPath("$.hediseMeasure").value("BCS"))
                .andExpect(jsonPath("$.published").value(true));
    }

    @Test
    @DisplayName("POST /case-studies/{caseStudyId}/publish - Should handle case study not found")
    void shouldHandleCaseStudyNotFound() throws Exception {
        // Given
        String caseStudyId = "nonexistent-id";
        when(executionService.publishCaseStudy(caseStudyId, tenantId))
                .thenThrow(new IllegalArgumentException("Task not found: " + caseStudyId));

        // When/Then
        mockMvc.perform(post("/api/v1/payer/phase2-execution/case-studies/{caseStudyId}/publish", caseStudyId)
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /case-studies/{caseStudyId}/publish - Should include all case study fields")
    void shouldIncludeAllCaseStudyFieldsOnPublish() throws Exception {
        // Given
        String caseStudyId = "task-complete";
        Phase2ExecutionTask publishedTask = Phase2ExecutionTask.builder()
                .id(caseStudyId)
                .taskName("Complete Case Study")
                .hediseMeasure("BCS")
                .baselinePerformancePercentage(new BigDecimal("65.00"))
                .currentPerformancePercentage(new BigDecimal("82.50"))
                .qualityBonusCaptured(new BigDecimal("150000.00"))
                .gapsClosed(45)
                .customerQuote("This intervention transformed our care delivery and patient outcomes.")
                .caseStudyPublished(true)
                .build();

        when(executionService.publishCaseStudy(caseStudyId, tenantId)).thenReturn(publishedTask);

        // When/Then
        mockMvc.perform(post("/api/v1/payer/phase2-execution/case-studies/{caseStudyId}/publish", caseStudyId)
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(caseStudyId))
                .andExpect(jsonPath("$.taskName").value("Complete Case Study"))
                .andExpect(jsonPath("$.hediseMeasure").value("BCS"))
                .andExpect(jsonPath("$.baselinePerformance").value(65.00))
                .andExpect(jsonPath("$.currentPerformance").value(82.50))
                .andExpect(jsonPath("$.bonusCaptured").value(150000.00))
                .andExpect(jsonPath("$.gapsClosed").value(45))
                .andExpect(jsonPath("$.customerQuote").value("This intervention transformed our care delivery and patient outcomes."))
                .andExpect(jsonPath("$.published").value(true));
    }

    // ===== Helper Methods =====

    private Phase2ExecutionTask createTask(String measure, BigDecimal bonusCapturedi, Integer gapsClosed) {
        return Phase2ExecutionTask.builder()
                .id("task-" + measure.toLowerCase() + "-" + System.nanoTime())
                .tenantId(tenantId)
                .taskName(measure + " Intervention")
                .hediseMeasure(measure)
                .qualityBonusCaptured(bonusCapturedi)
                .gapsClosed(gapsClosed)
                .status(TaskStatus.COMPLETED)
                .baselinePerformancePercentage(new BigDecimal("50.00"))
                .currentPerformancePercentage(new BigDecimal("70.00"))
                .caseStudyPublished(false)
                .build();
    }

    private Phase2ExecutionTask createCaseStudy(String id, String taskName, String measure, Boolean published) {
        return Phase2ExecutionTask.builder()
                .id(id)
                .tenantId(tenantId)
                .taskName(taskName)
                .hediseMeasure(measure)
                .baselinePerformancePercentage(new BigDecimal("60.00"))
                .currentPerformancePercentage(new BigDecimal("80.00"))
                .qualityBonusCaptured(new BigDecimal("120000.00"))
                .gapsClosed(40)
                .customerQuote("Great results from this intervention.")
                .caseStudyPublished(published)
                .status(TaskStatus.COMPLETED)
                .build();
    }
}
