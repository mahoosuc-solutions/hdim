package com.healthdata.caregap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import org.junit.jupiter.api.Tag;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for CareGapController.
 * Tests REST API endpoints for care gap management.
 *
 * Uses standalone MockMvc setup with Mockito for fast, isolated testing
 * without requiring Spring context, Kafka, or database infrastructure.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Care Gap Controller Tests")
@Tag("unit")
class CareGapControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private CareGapIdentificationService identificationService;

    @Mock
    private CareGapReportService reportService;

    @InjectMocks
    private CareGapController controller;

    private static final String TENANT_ID = "tenant-123";
    private static final UUID PATIENT_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Nested
    @DisplayName("POST /care-gap/identify Tests")
    class IdentifyAllCareGapsTests {

        @Test
        @DisplayName("Should identify all care gaps and return 201")
        void shouldIdentifyAllCareGaps() throws Exception {
            // Given
            List<CareGapEntity> gaps = List.of(
                    createGap("CDC_A1C", "OPEN", "high"),
                    createGap("BCS", "OPEN", "medium")
            );
            when(identificationService.identifyAllCareGaps(TENANT_ID, PATIENT_UUID, "system"))
                    .thenReturn(gaps);

            // When/Then
            mockMvc.perform(post("/care-gap/identify")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_UUID.toString()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].measureId").value("CDC_A1C"))
                    .andExpect(jsonPath("$[1].measureId").value("BCS"));
        }

        @Test
        @DisplayName("Should use custom createdBy parameter")
        void shouldUseCustomCreatedBy() throws Exception {
            // Given
            when(identificationService.identifyAllCareGaps(TENANT_ID, PATIENT_UUID, "clinician-1"))
                    .thenReturn(List.of());

            // When/Then
            mockMvc.perform(post("/care-gap/identify")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_UUID.toString())
                            .param("createdBy", "clinician-1"))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("POST /care-gap/identify/{library} Tests")
    class IdentifyGapsForLibraryTests {

        @Test
        @DisplayName("Should identify gaps for specific library")
        void shouldIdentifyGapsForLibrary() throws Exception {
            // Given
            List<CareGapEntity> gaps = List.of(createGap("HEDIS_CDC_A1C", "OPEN", "high"));
            when(identificationService.identifyCareGapsForLibrary(
                    TENANT_ID, PATIENT_UUID, "HEDIS_CDC_A1C", "system"))
                    .thenReturn(gaps);

            // When/Then
            mockMvc.perform(post("/care-gap/identify/HEDIS_CDC_A1C")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_UUID.toString()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].measureId").value("HEDIS_CDC_A1C"));
        }
    }

    @Nested
    @DisplayName("POST /care-gap/refresh Tests")
    class RefreshCareGapsTests {

        @Test
        @DisplayName("Should refresh care gaps and return 200")
        void shouldRefreshCareGaps() throws Exception {
            // Given
            List<CareGapEntity> refreshedGaps = List.of(createGap("CDC_A1C", "OPEN", "high"));
            when(identificationService.refreshCareGaps(TENANT_ID, PATIENT_UUID, "system"))
                    .thenReturn(refreshedGaps);

            // When/Then
            mockMvc.perform(post("/care-gap/refresh")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_UUID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("POST /care-gap/close Tests")
    class CloseCareGapTests {

        @Test
        @DisplayName("Should close care gap successfully")
        void shouldCloseCareGap() throws Exception {
            // Given
            UUID gapId = UUID.randomUUID();
            CareGapEntity closedGap = createGap("CDC_A1C", "CLOSED", "high");
            closedGap.setId(gapId);
            closedGap.setClosedBy("clinician-1");
            closedGap.setClosureReason("A1C test completed");
            closedGap.setClosedDate(Instant.now());

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
    @DisplayName("GET /care-gap/open Tests")
    class GetOpenCareGapsTests {

        @Test
        @DisplayName("Should return open care gaps")
        void shouldReturnOpenGaps() throws Exception {
            // Given
            List<CareGapEntity> gaps = List.of(
                    createGap("CDC_A1C", "OPEN", "high"),
                    createGap("BCS", "OPEN", "medium")
            );
            when(identificationService.getOpenCareGaps(TENANT_ID, PATIENT_UUID)).thenReturn(gaps);

            // When/Then
            mockMvc.perform(get("/care-gap/open")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_UUID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].gapStatus").value("OPEN"));
        }

        @Test
        @DisplayName("Should return empty list when no open gaps")
        void shouldReturnEmptyList() throws Exception {
            // Given
            when(identificationService.getOpenCareGaps(TENANT_ID, PATIENT_UUID)).thenReturn(List.of());

            // When/Then
            mockMvc.perform(get("/care-gap/open")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_UUID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /care-gap/high-priority Tests")
    class GetHighPriorityCareGapsTests {

        @Test
        @DisplayName("Should return high priority gaps")
        void shouldReturnHighPriorityGaps() throws Exception {
            // Given
            List<CareGapEntity> gaps = List.of(createGap("CRITICAL_MEASURE", "OPEN", "high"));
            when(identificationService.getHighPriorityCareGaps(TENANT_ID, PATIENT_UUID)).thenReturn(gaps);

            // When/Then
            mockMvc.perform(get("/care-gap/high-priority")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_UUID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].priority").value("high"));
        }
    }

    @Nested
    @DisplayName("GET /care-gap/overdue Tests")
    class GetOverdueGapsTests {

        @Test
        @DisplayName("Should return overdue gaps")
        void shouldReturnOverdueGaps() throws Exception {
            // Given
            CareGapEntity overdueGap = createGap("CDC_A1C", "OPEN", "high");
            overdueGap.setDueDate(LocalDate.now().minusDays(10));
            when(reportService.getOverdueGaps(TENANT_ID, PATIENT_UUID)).thenReturn(List.of(overdueGap));

            // When/Then
            mockMvc.perform(get("/care-gap/overdue")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_UUID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /care-gap/upcoming Tests")
    class GetUpcomingGapsTests {

        @Test
        @DisplayName("Should return upcoming gaps within default 30 days")
        void shouldReturnUpcomingGaps() throws Exception {
            // Given
            CareGapEntity upcomingGap = createGap("CDC_A1C", "OPEN", "medium");
            upcomingGap.setDueDate(LocalDate.now().plusDays(15));
            when(reportService.getUpcomingGaps(TENANT_ID, PATIENT_UUID, 30))
                    .thenReturn(List.of(upcomingGap));

            // When/Then
            mockMvc.perform(get("/care-gap/upcoming")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_UUID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("Should use custom days parameter")
        void shouldUseCustomDays() throws Exception {
            // Given
            when(reportService.getUpcomingGaps(TENANT_ID, PATIENT_UUID, 7))
                    .thenReturn(List.of());

            // When/Then
            mockMvc.perform(get("/care-gap/upcoming")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_UUID.toString())
                            .param("days", "7"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /care-gap/stats Tests")
    class GetCareGapStatsTests {

        @Test
        @DisplayName("Should return care gap statistics")
        void shouldReturnStats() throws Exception {
            // Given
            CareGapIdentificationService.CareGapStats stats =
                    new CareGapIdentificationService.CareGapStats(5, 2, 1, true, true);
            when(identificationService.getCareGapStats(TENANT_ID, PATIENT_UUID)).thenReturn(stats);

            // When/Then
            mockMvc.perform(get("/care-gap/stats")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_UUID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.openGapsCount").value(5))
                    .andExpect(jsonPath("$.highPriorityCount").value(2))
                    .andExpect(jsonPath("$.overdueCount").value(1))
                    .andExpect(jsonPath("$.hasOpenGaps").value(true))
                    .andExpect(jsonPath("$.hasHighPriorityGaps").value(true));
        }
    }

    @Nested
    @DisplayName("GET /care-gap/summary Tests")
    class GetCareGapSummaryTests {

        @Test
        @DisplayName("Should return care gap summary")
        void shouldReturnSummary() throws Exception {
            // Given
            CareGapReportService.CareGapSummary summary = new CareGapReportService.CareGapSummary(
                    PATIENT_UUID,
                    10, 6, 4, 2, 1, 40.0,
                    List.of("HEDIS", "CMS"),
                    Map.of("CDC_A1C", 3L, "BCS", 2L)
            );
            when(reportService.getCareGapSummary(TENANT_ID, PATIENT_UUID)).thenReturn(summary);

            // When/Then
            mockMvc.perform(get("/care-gap/summary")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_UUID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalGaps").value(10))
                    .andExpect(jsonPath("$.openGaps").value(6))
                    .andExpect(jsonPath("$.closedGaps").value(4))
                    .andExpect(jsonPath("$.closureRate").value(40.0))
                    .andExpect(jsonPath("$.measureCategories.length()").value(2));
        }
    }

    @Nested
    @DisplayName("GET /care-gap/by-category Tests")
    class GetGapsByCategoryTests {

        @Test
        @DisplayName("Should return gaps grouped by category")
        void shouldReturnGapsByCategory() throws Exception {
            // Given
            Map<String, Long> categoryMap = Map.of("HEDIS", 5L, "CMS", 3L);
            when(reportService.getGapsByMeasureCategory(TENANT_ID, PATIENT_UUID)).thenReturn(categoryMap);

            // When/Then
            mockMvc.perform(get("/care-gap/by-category")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_UUID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.HEDIS").value(5))
                    .andExpect(jsonPath("$.CMS").value(3));
        }
    }

    @Nested
    @DisplayName("GET /care-gap/by-priority Tests")
    class GetGapsByPriorityTests {

        @Test
        @DisplayName("Should return gaps grouped by priority")
        void shouldReturnGapsByPriority() throws Exception {
            // Given
            Map<String, Long> priorityMap = Map.of("high", 2L, "medium", 5L, "low", 1L);
            when(reportService.getGapsByPriority(TENANT_ID, PATIENT_UUID)).thenReturn(priorityMap);

            // When/Then
            mockMvc.perform(get("/care-gap/by-priority")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_UUID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.high").value(2))
                    .andExpect(jsonPath("$.medium").value(5))
                    .andExpect(jsonPath("$.low").value(1));
        }
    }

    @Nested
    @DisplayName("GET /care-gap/population-report Tests")
    class GetPopulationGapReportTests {

        @Test
        @DisplayName("Should return population-level report")
        void shouldReturnPopulationReport() throws Exception {
            // Given
            CareGapReportService.PopulationGapReport report = new CareGapReportService.PopulationGapReport(
                    100L, 75L, 1.33,
                    Map.of("high", 20L, "medium", 50L, "low", 30L),
                    Map.of("HEDIS", 70L, "CMS", 30L),
                    Map.of("CDC_A1C", 15L, "BCS", 12L),
                    15L,  // overdueCount
                    8L    // closedThisMonth
            );
            when(reportService.getPopulationGapReport(TENANT_ID)).thenReturn(report);

            // When/Then
            mockMvc.perform(get("/care-gap/population-report")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalOpenGaps").value(100))
                    .andExpect(jsonPath("$.uniquePatients").value(75))
                    .andExpect(jsonPath("$.avgGapsPerPatient").value(1.33))
                    .andExpect(jsonPath("$.gapsByPriority.high").value(20))
                    .andExpect(jsonPath("$.gapsByCategory.HEDIS").value(70));
        }
    }

    @Nested
    @DisplayName("GET /care-gap/_health Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return health status")
        void shouldReturnHealthStatus() throws Exception {
            // When/Then
            mockMvc.perform(get("/care-gap/_health")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.service").value("care-gap-service"));
        }
    }

    // ==================== Helper Methods ====================

    private CareGapEntity createGap(String measureId, String status, String priority) {
        return CareGapEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_UUID)
                .measureId(measureId)
                .measureName(measureId + " Measure")
                .gapCategory("HEDIS")
                .gapType("care-gap")
                .gapStatus(status)
                .priority(priority)
                .identifiedDate(Instant.now())
                .build();
    }
}
