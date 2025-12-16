package com.healthdata.caregap.service;

import com.healthdata.caregap.persistence.CareGapEntity;
import com.healthdata.caregap.persistence.CareGapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CareGapReportService.
 * Tests reporting, analytics, and summary generation for care gaps.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Care Gap Report Service Tests")
class CareGapReportServiceTest {

    @Mock
    private CareGapRepository careGapRepository;

    private CareGapReportService service;

    private static final String TENANT_ID = "tenant-123";
    private static final String PATIENT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final UUID PATIENT_UUID = UUID.fromString(PATIENT_ID);

    @BeforeEach
    void setUp() {
        service = new CareGapReportService(careGapRepository);
    }

    @Nested
    @DisplayName("Get Care Gap Summary Tests")
    class GetCareGapSummaryTests {

        @Test
        @DisplayName("Should generate comprehensive care gap summary")
        void shouldGenerateSummary() {
            // Given
            List<CareGapEntity> allGaps = createMixedGapsList();
            List<CareGapEntity> openGaps = allGaps.stream()
                    .filter(g -> "open".equals(g.getGapStatus()))
                    .toList();
            List<CareGapEntity> closedGaps = allGaps.stream()
                    .filter(g -> "closed".equals(g.getGapStatus()))
                    .toList();
            List<CareGapEntity> highPriorityGaps = openGaps.stream()
                    .filter(g -> "high".equals(g.getPriority()))
                    .toList();

            when(careGapRepository.findByTenantIdAndPatientId(TENANT_ID, PATIENT_UUID))
                    .thenReturn(allGaps);
            when(careGapRepository.findOpenGapsByPatient(TENANT_ID, PATIENT_UUID))
                    .thenReturn(openGaps);
            when(careGapRepository.findClosedGapsByPatient(TENANT_ID, PATIENT_UUID))
                    .thenReturn(closedGaps);
            when(careGapRepository.findHighPriorityOpenGaps(TENANT_ID, PATIENT_UUID))
                    .thenReturn(highPriorityGaps);
            when(careGapRepository.countOverdueGaps(eq(TENANT_ID), eq(PATIENT_UUID), any(LocalDate.class)))
                    .thenReturn(1L);

            // When
            CareGapReportService.CareGapSummary summary = service.getCareGapSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary.totalGaps()).isEqualTo(5);
            assertThat(summary.openGaps()).isEqualTo(3);
            assertThat(summary.closedGaps()).isEqualTo(2);
            assertThat(summary.highPriorityGaps()).isEqualTo(1);
            assertThat(summary.overdueGaps()).isEqualTo(1);
            assertThat(summary.closureRate()).isEqualTo(40.0); // 2/5 = 40%
        }

        @Test
        @DisplayName("Should calculate closure rate correctly")
        void shouldCalculateClosureRate() {
            // Given - all gaps closed
            List<CareGapEntity> allClosed = List.of(
                    createGap("CDC", "closed", "high", "HEDIS"),
                    createGap("BCS", "closed", "medium", "HEDIS")
            );

            when(careGapRepository.findByTenantIdAndPatientId(TENANT_ID, PATIENT_UUID))
                    .thenReturn(allClosed);
            when(careGapRepository.findOpenGapsByPatient(TENANT_ID, PATIENT_UUID))
                    .thenReturn(List.of());
            when(careGapRepository.findClosedGapsByPatient(TENANT_ID, PATIENT_UUID))
                    .thenReturn(allClosed);
            when(careGapRepository.findHighPriorityOpenGaps(TENANT_ID, PATIENT_UUID))
                    .thenReturn(List.of());
            when(careGapRepository.countOverdueGaps(eq(TENANT_ID), eq(PATIENT_UUID), any(LocalDate.class)))
                    .thenReturn(0L);

            // When
            CareGapReportService.CareGapSummary summary = service.getCareGapSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary.closureRate()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should handle zero gaps")
        void shouldHandleZeroGaps() {
            // Given
            when(careGapRepository.findByTenantIdAndPatientId(TENANT_ID, PATIENT_UUID))
                    .thenReturn(List.of());
            when(careGapRepository.findOpenGapsByPatient(TENANT_ID, PATIENT_UUID))
                    .thenReturn(List.of());
            when(careGapRepository.findClosedGapsByPatient(TENANT_ID, PATIENT_UUID))
                    .thenReturn(List.of());
            when(careGapRepository.findHighPriorityOpenGaps(TENANT_ID, PATIENT_UUID))
                    .thenReturn(List.of());
            when(careGapRepository.countOverdueGaps(eq(TENANT_ID), eq(PATIENT_UUID), any(LocalDate.class)))
                    .thenReturn(0L);

            // When
            CareGapReportService.CareGapSummary summary = service.getCareGapSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary.totalGaps()).isZero();
            assertThat(summary.closureRate()).isZero();
            assertThat(summary.measureCategories()).isEmpty();
        }

        @Test
        @DisplayName("Should extract unique measure categories")
        void shouldExtractUniqueCategories() {
            // Given
            List<CareGapEntity> openGaps = List.of(
                    createGap("CDC", "open", "high", "HEDIS"),
                    createGap("BCS", "open", "medium", "HEDIS"),
                    createGap("CMS130", "open", "low", "CMS")
            );

            when(careGapRepository.findByTenantIdAndPatientId(TENANT_ID, PATIENT_UUID))
                    .thenReturn(openGaps);
            when(careGapRepository.findOpenGapsByPatient(TENANT_ID, PATIENT_UUID))
                    .thenReturn(openGaps);
            when(careGapRepository.findClosedGapsByPatient(TENANT_ID, PATIENT_UUID))
                    .thenReturn(List.of());
            when(careGapRepository.findHighPriorityOpenGaps(TENANT_ID, PATIENT_UUID))
                    .thenReturn(List.of(openGaps.get(0)));
            when(careGapRepository.countOverdueGaps(eq(TENANT_ID), eq(PATIENT_UUID), any(LocalDate.class)))
                    .thenReturn(0L);

            // When
            CareGapReportService.CareGapSummary summary = service.getCareGapSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary.measureCategories()).containsExactly("CMS", "HEDIS"); // Sorted
        }
    }

    @Nested
    @DisplayName("Get Gaps By Measure Category Tests")
    class GetGapsByMeasureCategoryTests {

        @Test
        @DisplayName("Should group gaps by measure category")
        void shouldGroupByCategory() {
            // Given
            List<CareGapEntity> gaps = List.of(
                    createGap("CDC", "open", "high", "HEDIS"),
                    createGap("BCS", "open", "medium", "HEDIS"),
                    createGap("CMS130", "open", "low", "CMS"),
                    createGap("CUSTOM1", "open", "medium", null) // Null category
            );

            when(careGapRepository.findOpenGapsByPatient(TENANT_ID, PATIENT_UUID)).thenReturn(gaps);

            // When
            Map<String, Long> result = service.getGapsByMeasureCategory(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.get("HEDIS")).isEqualTo(2);
            assertThat(result.get("CMS")).isEqualTo(1);
            assertThat(result.get("Unknown")).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle empty gaps list")
        void shouldHandleEmptyList() {
            // Given
            when(careGapRepository.findOpenGapsByPatient(TENANT_ID, PATIENT_UUID))
                    .thenReturn(List.of());

            // When
            Map<String, Long> result = service.getGapsByMeasureCategory(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Gaps By Priority Tests")
    class GetGapsByPriorityTests {

        @Test
        @DisplayName("Should group gaps by priority")
        void shouldGroupByPriority() {
            // Given
            List<CareGapEntity> gaps = List.of(
                    createGap("CDC", "open", "high", "HEDIS"),
                    createGap("BCS", "open", "high", "HEDIS"),
                    createGap("CMS130", "open", "medium", "CMS"),
                    createGap("CMS131", "open", "low", "CMS"),
                    createGap("CUSTOM1", "open", null, "custom") // Null priority
            );

            when(careGapRepository.findOpenGapsByPatient(TENANT_ID, PATIENT_UUID)).thenReturn(gaps);

            // When
            Map<String, Long> result = service.getGapsByPriority(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).hasSize(4);
            assertThat(result.get("high")).isEqualTo(2);
            assertThat(result.get("medium")).isEqualTo(1);
            assertThat(result.get("low")).isEqualTo(1);
            assertThat(result.get("Unknown")).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Get Overdue Gaps Tests")
    class GetOverdueGapsTests {

        @Test
        @DisplayName("Should return overdue gaps sorted by due date")
        void shouldReturnOverdueGapsSorted() {
            // Given
            CareGapEntity overdue1 = createGapWithDueDate("CDC", LocalDate.now().minusDays(10));
            CareGapEntity overdue2 = createGapWithDueDate("BCS", LocalDate.now().minusDays(5));
            CareGapEntity notOverdue = createGapWithDueDate("CMS130", LocalDate.now().plusDays(5));
            CareGapEntity noDueDate = createGap("CUSTOM", "open", "medium", "custom");

            when(careGapRepository.findOpenGapsByPatient(TENANT_ID, PATIENT_UUID))
                    .thenReturn(List.of(overdue1, overdue2, notOverdue, noDueDate));

            // When
            List<CareGapEntity> result = service.getOverdueGaps(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getMeasureId()).isEqualTo("CDC"); // Oldest first
            assertThat(result.get(1).getMeasureId()).isEqualTo("BCS");
        }

        @Test
        @DisplayName("Should return empty list when no overdue gaps")
        void shouldReturnEmptyWhenNoOverdue() {
            // Given
            CareGapEntity futureGap = createGapWithDueDate("CDC", LocalDate.now().plusDays(30));

            when(careGapRepository.findOpenGapsByPatient(TENANT_ID, PATIENT_UUID))
                    .thenReturn(List.of(futureGap));

            // When
            List<CareGapEntity> result = service.getOverdueGaps(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Upcoming Gaps Tests")
    class GetUpcomingGapsTests {

        @Test
        @DisplayName("Should return gaps due within specified days")
        void shouldReturnUpcomingGaps() {
            // Given
            List<CareGapEntity> upcomingGaps = List.of(
                    createGapWithDueDate("CDC", LocalDate.now().plusDays(15)),
                    createGapWithDueDate("BCS", LocalDate.now().plusDays(25))
            );

            when(careGapRepository.findGapsDueInRange(
                    eq(TENANT_ID), eq(PATIENT_UUID), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(upcomingGaps);

            // When
            List<CareGapEntity> result = service.getUpcomingGaps(TENANT_ID, PATIENT_ID, 30);

            // Then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Get Population Gap Report Tests")
    class GetPopulationGapReportTests {

        @Test
        @DisplayName("Should generate population-level report")
        void shouldGeneratePopulationReport() {
            // Given
            UUID patient1 = UUID.randomUUID();
            UUID patient2 = UUID.randomUUID();
            UUID patient3 = UUID.randomUUID();

            List<CareGapEntity> allOpenGaps = List.of(
                    createGapForPatient(patient1, "CDC", "high", "HEDIS"),
                    createGapForPatient(patient1, "BCS", "medium", "HEDIS"),
                    createGapForPatient(patient2, "CDC", "high", "HEDIS"),
                    createGapForPatient(patient3, "CMS130", "low", "CMS")
            );

            when(careGapRepository.findAllOpenGaps(TENANT_ID)).thenReturn(allOpenGaps);

            // When
            CareGapReportService.PopulationGapReport report = service.getPopulationGapReport(TENANT_ID);

            // Then
            assertThat(report.totalOpenGaps()).isEqualTo(4);
            assertThat(report.uniquePatients()).isEqualTo(3);
            assertThat(report.avgGapsPerPatient()).isCloseTo(1.33, org.assertj.core.api.Assertions.within(0.01));
            assertThat(report.gapsByPriority().get("high")).isEqualTo(2);
            assertThat(report.gapsByPriority().get("medium")).isEqualTo(1);
            assertThat(report.gapsByPriority().get("low")).isEqualTo(1);
            assertThat(report.gapsByCategory().get("HEDIS")).isEqualTo(3);
            assertThat(report.gapsByCategory().get("CMS")).isEqualTo(1);
        }

        @Test
        @DisplayName("Should identify top measures")
        void shouldIdentifyTopMeasures() {
            // Given
            List<CareGapEntity> gaps = new ArrayList<>();
            // Add multiple CDC gaps
            for (int i = 0; i < 5; i++) {
                gaps.add(createGapForPatient(UUID.randomUUID(), "CDC_A1C", "high", "HEDIS"));
            }
            // Add some BCS gaps
            for (int i = 0; i < 3; i++) {
                gaps.add(createGapForPatient(UUID.randomUUID(), "BCS", "medium", "HEDIS"));
            }
            // Add one CMS130 gap
            gaps.add(createGapForPatient(UUID.randomUUID(), "CMS130", "low", "CMS"));

            when(careGapRepository.findAllOpenGaps(TENANT_ID)).thenReturn(gaps);

            // When
            CareGapReportService.PopulationGapReport report = service.getPopulationGapReport(TENANT_ID);

            // Then
            assertThat(report.topMeasures()).hasSize(3);
            // Verify ordering (most frequent first)
            List<Map.Entry<String, Long>> entries = new ArrayList<>(report.topMeasures().entrySet());
            assertThat(entries.get(0).getKey()).isEqualTo("CDC_A1C");
            assertThat(entries.get(0).getValue()).isEqualTo(5L);
            assertThat(entries.get(1).getKey()).isEqualTo("BCS");
            assertThat(entries.get(1).getValue()).isEqualTo(3L);
        }

        @Test
        @DisplayName("Should handle empty population")
        void shouldHandleEmptyPopulation() {
            // Given
            when(careGapRepository.findAllOpenGaps(TENANT_ID)).thenReturn(List.of());

            // When
            CareGapReportService.PopulationGapReport report = service.getPopulationGapReport(TENANT_ID);

            // Then
            assertThat(report.totalOpenGaps()).isZero();
            assertThat(report.uniquePatients()).isZero();
            assertThat(report.avgGapsPerPatient()).isZero();
            assertThat(report.gapsByPriority()).isEmpty();
            assertThat(report.gapsByCategory()).isEmpty();
            assertThat(report.topMeasures()).isEmpty();
        }

        @Test
        @DisplayName("Should limit top measures to 10")
        void shouldLimitTopMeasures() {
            // Given - create 15 different measures
            List<CareGapEntity> gaps = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                gaps.add(createGapForPatient(UUID.randomUUID(), "MEASURE_" + i, "medium", "custom"));
            }

            when(careGapRepository.findAllOpenGaps(TENANT_ID)).thenReturn(gaps);

            // When
            CareGapReportService.PopulationGapReport report = service.getPopulationGapReport(TENANT_ID);

            // Then
            assertThat(report.topMeasures()).hasSize(10);
        }
    }

    // ==================== Helper Methods ====================

    private CareGapEntity createGap(String measureId, String status, String priority, String category) {
        return CareGapEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_UUID)
                .measureId(measureId)
                .measureName(measureId + " Measure")
                .measureCategory(category)
                .gapStatus(status)
                .priority(priority)
                .identifiedDate(LocalDate.now())
                .build();
    }

    private CareGapEntity createGapWithDueDate(String measureId, LocalDate dueDate) {
        CareGapEntity gap = createGap(measureId, "open", "medium", "HEDIS");
        gap.setDueDate(dueDate);
        return gap;
    }

    private CareGapEntity createGapForPatient(UUID patientId, String measureId, String priority, String category) {
        return CareGapEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(patientId)
                .measureId(measureId)
                .measureName(measureId + " Measure")
                .measureCategory(category)
                .gapStatus("open")
                .priority(priority)
                .identifiedDate(LocalDate.now())
                .build();
    }

    private List<CareGapEntity> createMixedGapsList() {
        return List.of(
                createGap("CDC_A1C", "open", "high", "HEDIS"),
                createGap("BCS", "open", "medium", "HEDIS"),
                createGap("CMS130", "open", "low", "CMS"),
                createGap("CDC_BP", "closed", "medium", "HEDIS"),
                createGap("CMS131", "closed", "low", "CMS")
        );
    }
}
