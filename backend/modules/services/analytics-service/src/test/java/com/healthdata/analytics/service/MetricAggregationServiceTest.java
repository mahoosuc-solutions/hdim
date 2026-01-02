package com.healthdata.analytics.service;

import com.healthdata.analytics.dto.KpiSummaryDto;
import com.healthdata.analytics.persistence.MetricSnapshotEntity;
import com.healthdata.analytics.repository.MetricSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MetricAggregationService.
 * Tests metric snapshot capture, storage, and cleanup operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Metric Aggregation Service Tests")
class MetricAggregationServiceTest {

    @Mock
    private MetricSnapshotRepository snapshotRepository;

    @Mock
    private KpiService kpiService;

    @Captor
    private ArgumentCaptor<MetricSnapshotEntity> snapshotCaptor;

    private MetricAggregationService service;

    private static final String TENANT_ID = "tenant-123";

    @BeforeEach
    void setUp() {
        service = new MetricAggregationService(snapshotRepository, kpiService);
    }

    @Nested
    @DisplayName("Save Snapshot Tests")
    class SaveSnapshotTests {

        @Test
        @DisplayName("Should save snapshot successfully")
        void shouldSaveSnapshotSuccessfully() {
            // Given
            String metricType = "QUALITY_SCORE";
            String metricName = "HEDIS Compliance";
            BigDecimal value = new BigDecimal("85.5");
            Map<String, Object> dimensions = Map.of("measure", "BCS", "year", 2024);

            when(snapshotRepository.save(any(MetricSnapshotEntity.class)))
                    .thenAnswer(inv -> {
                        MetricSnapshotEntity saved = inv.getArgument(0);
                        saved.setId(UUID.randomUUID());
                        return saved;
                    });

            // When
            MetricSnapshotEntity result = service.saveSnapshot(TENANT_ID, metricType, metricName, value, dimensions);

            // Then
            verify(snapshotRepository).save(snapshotCaptor.capture());
            MetricSnapshotEntity captured = snapshotCaptor.getValue();

            assertThat(captured.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(captured.getMetricType()).isEqualTo(metricType);
            assertThat(captured.getMetricName()).isEqualTo(metricName);
            assertThat(captured.getMetricValue()).isEqualTo(value);
            assertThat(captured.getDimensions()).isEqualTo(dimensions);
            assertThat(captured.getSnapshotDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("Should save snapshot with null dimensions")
        void shouldSaveSnapshotWithNullDimensions() {
            // Given
            when(snapshotRepository.save(any(MetricSnapshotEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            MetricSnapshotEntity result = service.saveSnapshot(
                    TENANT_ID, "RAF_SCORE", "Average RAF", new BigDecimal("1.25"), null);

            // Then
            assertThat(result).isNotNull();
            verify(snapshotRepository).save(any(MetricSnapshotEntity.class));
        }

        @Test
        @DisplayName("Should save snapshot with zero value")
        void shouldSaveSnapshotWithZeroValue() {
            // Given
            when(snapshotRepository.save(any(MetricSnapshotEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            MetricSnapshotEntity result = service.saveSnapshot(
                    TENANT_ID, "CARE_GAP_RATE", "Open Gaps", BigDecimal.ZERO, null);

            // Then
            assertThat(result.getMetricValue()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should save snapshot with complex dimensions")
        void shouldSaveSnapshotWithComplexDimensions() {
            // Given
            Map<String, Object> dimensions = new HashMap<>();
            dimensions.put("measureSet", List.of("BCS", "CDC", "COL"));
            dimensions.put("dateRange", Map.of("start", "2024-01-01", "end", "2024-12-31"));
            dimensions.put("includeExclusions", true);

            when(snapshotRepository.save(any(MetricSnapshotEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            MetricSnapshotEntity result = service.saveSnapshot(
                    TENANT_ID, "QUALITY_SCORE", "Multi-measure", new BigDecimal("78.3"), dimensions);

            // Then
            verify(snapshotRepository).save(snapshotCaptor.capture());
            assertThat(snapshotCaptor.getValue().getDimensions()).containsKeys("measureSet", "dateRange", "includeExclusions");
        }
    }

    @Nested
    @DisplayName("Capture KPI Snapshots Tests")
    class CaptureKpiSnapshotsTests {

        @Test
        @DisplayName("Should capture all KPI categories")
        void shouldCaptureAllKpiCategories() {
            // Given
            List<KpiSummaryDto> qualityKpis = List.of(
                    createKpiSummary("QUALITY_SCORE", "BCS Compliance", new BigDecimal("85.0")),
                    createKpiSummary("QUALITY_SCORE", "CDC Compliance", new BigDecimal("72.5"))
            );
            List<KpiSummaryDto> hccKpis = List.of(
                    createKpiSummary("RAF_SCORE", "Average RAF", new BigDecimal("1.35"))
            );
            List<KpiSummaryDto> careGapKpis = List.of(
                    createKpiSummary("CARE_GAP_RATE", "Open Gaps", new BigDecimal("15.2"))
            );

            when(kpiService.getQualityKpis(TENANT_ID)).thenReturn(qualityKpis);
            when(kpiService.getHccKpis(TENANT_ID)).thenReturn(hccKpis);
            when(kpiService.getCareGapKpis(TENANT_ID)).thenReturn(careGapKpis);
            when(snapshotRepository.save(any(MetricSnapshotEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.captureKpiSnapshots(TENANT_ID);

            // Then
            verify(kpiService).getQualityKpis(TENANT_ID);
            verify(kpiService).getHccKpis(TENANT_ID);
            verify(kpiService).getCareGapKpis(TENANT_ID);
            verify(snapshotRepository, times(4)).save(any(MetricSnapshotEntity.class));
        }

        @Test
        @DisplayName("Should handle empty KPI lists")
        void shouldHandleEmptyKpiLists() {
            // Given
            when(kpiService.getQualityKpis(TENANT_ID)).thenReturn(List.of());
            when(kpiService.getHccKpis(TENANT_ID)).thenReturn(List.of());
            when(kpiService.getCareGapKpis(TENANT_ID)).thenReturn(List.of());

            // When
            service.captureKpiSnapshots(TENANT_ID);

            // Then
            verify(snapshotRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should continue after quality KPI error")
        void shouldContinueAfterQualityKpiError() {
            // Given
            when(kpiService.getQualityKpis(TENANT_ID))
                    .thenThrow(new RuntimeException("Quality service unavailable"));

            // When - should not throw
            service.captureKpiSnapshots(TENANT_ID);

            // Then - service methods may not be called due to exception, but no exception propagates
            verify(kpiService).getQualityKpis(TENANT_ID);
        }

        @Test
        @DisplayName("Should save snapshots with correct breakdowns")
        void shouldSaveSnapshotsWithCorrectBreakdowns() {
            // Given
            Map<String, Object> breakdown = Map.of(
                    "numerator", 850,
                    "denominator", 1000,
                    "exclusions", 50
            );
            List<KpiSummaryDto> qualityKpis = List.of(
                    createKpiSummaryWithBreakdown("QUALITY_SCORE", "BCS", new BigDecimal("85.0"), breakdown)
            );

            when(kpiService.getQualityKpis(TENANT_ID)).thenReturn(qualityKpis);
            when(kpiService.getHccKpis(TENANT_ID)).thenReturn(List.of());
            when(kpiService.getCareGapKpis(TENANT_ID)).thenReturn(List.of());
            when(snapshotRepository.save(any(MetricSnapshotEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.captureKpiSnapshots(TENANT_ID);

            // Then
            verify(snapshotRepository).save(snapshotCaptor.capture());
            assertThat(snapshotCaptor.getValue().getDimensions()).isEqualTo(breakdown);
        }
    }

    @Nested
    @DisplayName("Cleanup Old Snapshots Tests")
    class CleanupOldSnapshotsTests {

        @Test
        @DisplayName("Should cleanup snapshots older than retention period")
        void shouldCleanupOldSnapshots() {
            // Given
            int retentionDays = 90;
            when(snapshotRepository.deleteOldSnapshots(any(LocalDate.class))).thenReturn(150);

            // When
            int deleted = service.cleanupOldSnapshots(retentionDays);

            // Then
            assertThat(deleted).isEqualTo(150);
            verify(snapshotRepository).deleteOldSnapshots(LocalDate.now().minusDays(retentionDays));
        }

        @Test
        @DisplayName("Should return zero when no old snapshots")
        void shouldReturnZeroWhenNoOldSnapshots() {
            // Given
            when(snapshotRepository.deleteOldSnapshots(any(LocalDate.class))).thenReturn(0);

            // When
            int deleted = service.cleanupOldSnapshots(30);

            // Then
            assertThat(deleted).isZero();
        }

        @Test
        @DisplayName("Should use correct threshold date")
        void shouldUseCorrectThresholdDate() {
            // Given
            int retentionDays = 365;
            LocalDate expectedThreshold = LocalDate.now().minusDays(365);
            when(snapshotRepository.deleteOldSnapshots(expectedThreshold)).thenReturn(1000);

            // When
            service.cleanupOldSnapshots(retentionDays);

            // Then
            verify(snapshotRepository).deleteOldSnapshots(expectedThreshold);
        }
    }

    @Nested
    @DisplayName("Get Snapshot Statistics Tests")
    class GetSnapshotStatisticsTests {

        @Test
        @DisplayName("Should return all statistics")
        void shouldReturnAllStatistics() {
            // Given
            when(snapshotRepository.countByTenantIdAndMetricType(TENANT_ID, "QUALITY_SCORE"))
                    .thenReturn(100L);
            when(snapshotRepository.countByTenantIdAndMetricType(TENANT_ID, "RAF_SCORE"))
                    .thenReturn(50L);
            when(snapshotRepository.countByTenantIdAndMetricType(TENANT_ID, "CARE_GAP_RATE"))
                    .thenReturn(75L);
            when(snapshotRepository.findLatestSnapshotDate(TENANT_ID, "QUALITY_SCORE"))
                    .thenReturn(Optional.of(LocalDate.now()));
            when(snapshotRepository.findLatestSnapshotDate(TENANT_ID, "RAF_SCORE"))
                    .thenReturn(Optional.of(LocalDate.now().minusDays(1)));
            when(snapshotRepository.findLatestSnapshotDate(TENANT_ID, "CARE_GAP_RATE"))
                    .thenReturn(Optional.of(LocalDate.now().minusDays(2)));

            // When
            Map<String, Object> stats = service.getSnapshotStatistics(TENANT_ID);

            // Then
            assertThat(stats).containsKeys(
                    "qualitySnapshotCount", "rafSnapshotCount", "careGapSnapshotCount",
                    "latestQualitySnapshot", "latestRafSnapshot", "latestCareGapSnapshot"
            );
            assertThat(stats.get("qualitySnapshotCount")).isEqualTo(100L);
            assertThat(stats.get("rafSnapshotCount")).isEqualTo(50L);
            assertThat(stats.get("careGapSnapshotCount")).isEqualTo(75L);
        }

        @Test
        @DisplayName("Should handle missing latest dates")
        void shouldHandleMissingLatestDates() {
            // Given
            when(snapshotRepository.countByTenantIdAndMetricType(eq(TENANT_ID), anyString()))
                    .thenReturn(0L);
            when(snapshotRepository.findLatestSnapshotDate(eq(TENANT_ID), anyString()))
                    .thenReturn(Optional.empty());

            // When
            Map<String, Object> stats = service.getSnapshotStatistics(TENANT_ID);

            // Then
            assertThat(stats).containsKeys("qualitySnapshotCount", "rafSnapshotCount", "careGapSnapshotCount");
            assertThat(stats).doesNotContainKeys("latestQualitySnapshot", "latestRafSnapshot", "latestCareGapSnapshot");
        }

        @Test
        @DisplayName("Should return zero counts for new tenant")
        void shouldReturnZeroCountsForNewTenant() {
            // Given
            String newTenantId = "new-tenant";
            when(snapshotRepository.countByTenantIdAndMetricType(eq(newTenantId), anyString()))
                    .thenReturn(0L);
            when(snapshotRepository.findLatestSnapshotDate(eq(newTenantId), anyString()))
                    .thenReturn(Optional.empty());

            // When
            Map<String, Object> stats = service.getSnapshotStatistics(newTenantId);

            // Then
            assertThat(stats.get("qualitySnapshotCount")).isEqualTo(0L);
            assertThat(stats.get("rafSnapshotCount")).isEqualTo(0L);
            assertThat(stats.get("careGapSnapshotCount")).isEqualTo(0L);
        }
    }

    // ==================== Helper Methods ====================

    private KpiSummaryDto createKpiSummary(String metricType, String metricName, BigDecimal value) {
        return KpiSummaryDto.builder()
                .metricType(metricType)
                .metricName(metricName)
                .currentValue(value)
                .breakdown(null)
                .build();
    }

    private KpiSummaryDto createKpiSummaryWithBreakdown(String metricType, String metricName,
                                                         BigDecimal value, Map<String, Object> breakdown) {
        return KpiSummaryDto.builder()
                .metricType(metricType)
                .metricName(metricName)
                .currentValue(value)
                .breakdown(breakdown)
                .build();
    }
}
