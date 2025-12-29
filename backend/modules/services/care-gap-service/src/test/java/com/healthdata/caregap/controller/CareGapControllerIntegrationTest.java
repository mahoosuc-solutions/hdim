package com.healthdata.caregap.controller;

import com.healthdata.caregap.persistence.CareGapEntity;
import com.healthdata.caregap.service.CareGapIdentificationService;
import com.healthdata.caregap.service.CareGapReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CareGapController.
 * Tests REST API endpoints with mocked services using MockMvc standalone setup.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CareGapController Integration Tests")
class CareGapControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private CareGapIdentificationService identificationService;

    @Mock
    private CareGapReportService reportService;

    @InjectMocks
    private CareGapController controller;

    private static final String TENANT_ID = "tenant-123";
    private static final UUID PATIENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("Identify Care Gaps Endpoints")
    class IdentifyCareGapsTests {

        @Test
        @DisplayName("POST /care-gap/identify should identify all care gaps")
        void shouldIdentifyAllCareGaps() throws Exception {
            // Given
            CareGapEntity gap = createGap("HEDIS_CDC", "Diabetes A1C Control", "high");
            when(identificationService.identifyAllCareGaps(eq(TENANT_ID), eq(PATIENT_ID), anyString()))
                    .thenReturn(List.of(gap));

            // When/Then
            mockMvc.perform(post("/care-gap/identify")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID.toString())
                            .param("createdBy", "test-user"))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].measureId").value("HEDIS_CDC"))
                    .andExpect(jsonPath("$[0].measureName").value("Diabetes A1C Control"))
                    .andExpect(jsonPath("$[0].priority").value("high"));
        }

        @Test
        @DisplayName("POST /care-gap/identify/{library} should identify gaps for specific library")
        void shouldIdentifyGapsForSpecificLibrary() throws Exception {
            // Given
            String library = "HEDIS_BCS";
            CareGapEntity gap = createGap("HEDIS_BCS", "Breast Cancer Screening", "high");
            when(identificationService.identifyCareGapsForLibrary(eq(TENANT_ID), eq(PATIENT_ID), eq(library), anyString()))
                    .thenReturn(List.of(gap));

            // When/Then
            mockMvc.perform(post("/care-gap/identify/{library}", library)
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID.toString()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[0].measureId").value("HEDIS_BCS"));
        }

        @Test
        @DisplayName("POST /care-gap/identify should return empty list when no gaps found")
        void shouldReturnEmptyListWhenNoGaps() throws Exception {
            // Given
            when(identificationService.identifyAllCareGaps(anyString(), any(UUID.class), anyString()))
                    .thenReturn(List.of());

            // When/Then
            mockMvc.perform(post("/care-gap/identify")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID.toString()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("Refresh Care Gaps Endpoint")
    class RefreshCareGapsTests {

        @Test
        @DisplayName("POST /care-gap/refresh should refresh and return current gaps")
        void shouldRefreshCareGaps() throws Exception {
            // Given
            CareGapEntity gap = createGap("HEDIS_CDC", "Diabetes Control", "medium");
            when(identificationService.refreshCareGaps(eq(TENANT_ID), eq(PATIENT_ID), anyString()))
                    .thenReturn(List.of(gap));

            // When/Then
            mockMvc.perform(post("/care-gap/refresh")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].measureId").value("HEDIS_CDC"));
        }
    }

    @Nested
    @DisplayName("Close Care Gap Endpoint")
    class CloseCareGapTests {

        @Test
        @DisplayName("POST /care-gap/close should close a care gap")
        void shouldCloseCareGap() throws Exception {
            // Given
            UUID gapId = UUID.randomUUID();
            CareGapEntity closedGap = createGap("HEDIS_CDC", "Diabetes Control", "high");
            closedGap.setGapStatus("CLOSED");
            closedGap.setClosedBy("clinician-1");
            closedGap.setClosureReason("A1C test completed");

            when(identificationService.closeCareGap(
                    eq(TENANT_ID), eq(gapId), eq("clinician-1"), eq("A1C test completed"), eq("Lab order")))
                    .thenReturn(closedGap);

            // When/Then
            mockMvc.perform(post("/care-gap/close")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("gapId", gapId.toString())
                            .param("closedBy", "clinician-1")
                            .param("closureReason", "A1C test completed")
                            .param("closureAction", "Lab order"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.gapStatus").value("CLOSED"))
                    .andExpect(jsonPath("$.closedBy").value("clinician-1"));
        }
    }

    @Nested
    @DisplayName("Query Care Gaps Endpoints")
    class QueryCareGapsTests {

        @Test
        @DisplayName("GET /care-gap/open should return open care gaps")
        void shouldGetOpenCareGaps() throws Exception {
            // Given
            CareGapEntity gap1 = createGap("HEDIS_CDC", "Diabetes", "high");
            CareGapEntity gap2 = createGap("HEDIS_BCS", "Breast Cancer Screening", "high");
            when(identificationService.getOpenCareGaps(TENANT_ID, PATIENT_ID))
                    .thenReturn(List.of(gap1, gap2));

            // When/Then
            mockMvc.perform(get("/care-gap/open")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("GET /care-gap/high-priority should return high priority gaps")
        void shouldGetHighPriorityGaps() throws Exception {
            // Given
            CareGapEntity gap = createGap("HEDIS_CDC", "Diabetes Control", "high");
            when(identificationService.getHighPriorityCareGaps(TENANT_ID, PATIENT_ID))
                    .thenReturn(List.of(gap));

            // When/Then
            mockMvc.perform(get("/care-gap/high-priority")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].priority").value("high"));
        }

        @Test
        @DisplayName("GET /care-gap/overdue should return overdue gaps")
        void shouldGetOverdueGaps() throws Exception {
            // Given
            CareGapEntity gap = createGap("HEDIS_CCS", "Cervical Cancer Screening", "high");
            gap.setDueDate(LocalDate.now().minusDays(30));
            when(reportService.getOverdueGaps(TENANT_ID, PATIENT_ID))
                    .thenReturn(List.of(gap));

            // When/Then
            mockMvc.perform(get("/care-gap/overdue")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].measureId").value("HEDIS_CCS"));
        }

        @Test
        @DisplayName("GET /care-gap/upcoming should return upcoming gaps with default days")
        void shouldGetUpcomingGapsWithDefaultDays() throws Exception {
            // Given
            CareGapEntity gap = createGap("HEDIS_COL", "Colorectal Cancer", "medium");
            gap.setDueDate(LocalDate.now().plusDays(15));
            when(reportService.getUpcomingGaps(TENANT_ID, PATIENT_ID, 30))
                    .thenReturn(List.of(gap));

            // When/Then
            mockMvc.perform(get("/care-gap/upcoming")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].measureId").value("HEDIS_COL"));
        }

        @Test
        @DisplayName("GET /care-gap/upcoming should accept custom days parameter")
        void shouldGetUpcomingGapsWithCustomDays() throws Exception {
            // Given
            when(reportService.getUpcomingGaps(TENANT_ID, PATIENT_ID, 60))
                    .thenReturn(List.of());

            // When/Then
            mockMvc.perform(get("/care-gap/upcoming")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID.toString())
                            .param("days", "60"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("Statistics and Reports Endpoints")
    class StatisticsAndReportsTests {

        @Test
        @DisplayName("GET /care-gap/stats should return care gap statistics")
        void shouldGetCareGapStats() throws Exception {
            // Given
            CareGapIdentificationService.CareGapStats stats =
                    new CareGapIdentificationService.CareGapStats(5L, 2L, 1L, true, true);
            when(identificationService.getCareGapStats(TENANT_ID, PATIENT_ID))
                    .thenReturn(stats);

            // When/Then
            mockMvc.perform(get("/care-gap/stats")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.openGapsCount").value(5))
                    .andExpect(jsonPath("$.highPriorityCount").value(2))
                    .andExpect(jsonPath("$.overdueCount").value(1));
        }

        @Test
        @DisplayName("GET /care-gap/summary should return care gap summary")
        void shouldGetCareGapSummary() throws Exception {
            // Given
            CareGapReportService.CareGapSummary summary =
                    new CareGapReportService.CareGapSummary(
                            10, 5, 3, 2, 1, 85.0,
                            List.of("HEDIS", "CMS"),
                            Map.of("CDC", 3L, "BCS", 2L));
            when(reportService.getCareGapSummary(TENANT_ID, PATIENT_ID))
                    .thenReturn(summary);

            // When/Then
            mockMvc.perform(get("/care-gap/summary")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.openGaps").value(5))
                    .andExpect(jsonPath("$.closureRate").value(85.0));
        }

        @Test
        @DisplayName("GET /care-gap/by-category should return gaps grouped by category")
        void shouldGetGapsByCategory() throws Exception {
            // Given
            Map<String, Long> categories = Map.of("HEDIS", 3L, "CMS", 2L);
            when(reportService.getGapsByMeasureCategory(TENANT_ID, PATIENT_ID))
                    .thenReturn(categories);

            // When/Then
            mockMvc.perform(get("/care-gap/by-category")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.HEDIS").value(3))
                    .andExpect(jsonPath("$.CMS").value(2));
        }

        @Test
        @DisplayName("GET /care-gap/by-priority should return gaps grouped by priority")
        void shouldGetGapsByPriority() throws Exception {
            // Given
            Map<String, Long> priorities = Map.of("high", 2L, "medium", 3L, "low", 1L);
            when(reportService.getGapsByPriority(TENANT_ID, PATIENT_ID))
                    .thenReturn(priorities);

            // When/Then
            mockMvc.perform(get("/care-gap/by-priority")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.high").value(2))
                    .andExpect(jsonPath("$.medium").value(3));
        }

        @Test
        @DisplayName("GET /care-gap/population-report should return population report")
        void shouldGetPopulationReport() throws Exception {
            // Given
            CareGapReportService.PopulationGapReport report =
                    new CareGapReportService.PopulationGapReport(
                            350L, 1000L, 1.5,
                            Map.of("high", 50L, "medium", 200L, "low", 100L),
                            Map.of("HEDIS", 250L, "CMS", 100L),
                            Map.of("HEDIS_CDC", 100L, "HEDIS_BCS", 75L));
            when(reportService.getPopulationGapReport(TENANT_ID))
                    .thenReturn(report);

            // When/Then
            mockMvc.perform(get("/care-gap/population-report")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalOpenGaps").value(350))
                    .andExpect(jsonPath("$.uniquePatients").value(1000))
                    .andExpect(jsonPath("$.avgGapsPerPatient").value(1.5));
        }
    }

    @Nested
    @DisplayName("Health Check Endpoint")
    class HealthCheckTests {

        @Test
        @DisplayName("GET /care-gap/_health should return health status")
        void shouldReturnHealthStatus() throws Exception {
            mockMvc.perform(get("/care-gap/_health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.service").value("care-gap-service"));
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation Tests")
    class MultiTenantTests {

        @Test
        @DisplayName("Should pass tenant ID to service layer")
        void shouldPassTenantIdToService() throws Exception {
            // Given
            String differentTenant = "tenant-456";
            when(identificationService.getOpenCareGaps(eq(differentTenant), any(UUID.class)))
                    .thenReturn(List.of());

            // When/Then - verify different tenant ID is correctly passed
            mockMvc.perform(get("/care-gap/open")
                            .header("X-Tenant-ID", differentTenant)
                            .param("patient", PATIENT_ID.toString()))
                    .andExpect(status().isOk());
        }
    }

    private CareGapEntity createGap(String measureId, String measureName, String priority) {
        return CareGapEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .measureId(measureId)
                .measureName(measureName)
                .priority(priority)
                .gapCategory("HEDIS")
                .gapStatus("open")
                .gapDescription("Care gap identified")
                .riskScore(0.7)
                .dueDate(LocalDate.now().plusDays(30))
                .build();
    }
}
