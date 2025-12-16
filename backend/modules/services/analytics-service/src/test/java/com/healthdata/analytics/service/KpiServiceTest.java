package com.healthdata.analytics.service;

import com.healthdata.analytics.client.CareGapClient;
import com.healthdata.analytics.client.HccClient;
import com.healthdata.analytics.client.QualityMeasureClient;
import com.healthdata.analytics.dto.KpiSummaryDto;
import com.healthdata.analytics.persistence.MetricSnapshotEntity;
import com.healthdata.analytics.repository.MetricSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KpiService.
 * Tests KPI aggregation, fallback mechanisms, and trend calculations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KPI Service Tests")
class KpiServiceTest {

    @Mock
    private QualityMeasureClient qualityMeasureClient;

    @Mock
    private HccClient hccClient;

    @Mock
    private CareGapClient careGapClient;

    @Mock
    private MetricSnapshotRepository snapshotRepository;

    private KpiService service;

    private static final String TENANT_ID = "tenant-123";

    @BeforeEach
    void setUp() {
        service = new KpiService(qualityMeasureClient, hccClient, careGapClient, snapshotRepository);
    }

    @Nested
    @DisplayName("Get All KPIs Tests")
    class GetAllKpisTests {

        @Test
        @DisplayName("Should aggregate all KPI categories")
        void shouldAggregateAllKpis() {
            // Given
            Map<String, Object> qualitySummary = Map.of(
                    "overallScore", 85.5,
                    "starRating", 4.0,
                    "measuresMet", 15
            );
            Map<String, Object> hccSummary = Map.of(
                    "averageRafScore", 1.25,
                    "rafScoreGap", 0.15,
                    "suspectedHccCount", 45
            );
            Map<String, Object> careGapSummary = Map.of(
                    "openGapCount", 120,
                    "closureRate", 75.5,
                    "highPriorityCount", 25
            );

            when(qualityMeasureClient.getMeasureSummary(TENANT_ID)).thenReturn(qualitySummary);
            when(hccClient.getRafScoreSummary(TENANT_ID)).thenReturn(hccSummary);
            when(careGapClient.getCareGapSummary(TENANT_ID)).thenReturn(careGapSummary);
            when(snapshotRepository.findLatestSnapshots(any(), any(), any(), any()))
                    .thenReturn(List.of());

            // When
            Map<String, Object> result = service.getAllKpis(TENANT_ID);

            // Then
            assertThat(result).containsKeys("quality", "hcc", "careGaps", "asOfDate");
            assertThat((List<?>) result.get("quality")).isNotEmpty();
            assertThat((List<?>) result.get("hcc")).isNotEmpty();
            assertThat((List<?>) result.get("careGaps")).isNotEmpty();
        }

        @Test
        @DisplayName("Should include current date in response")
        void shouldIncludeCurrentDate() {
            // Given
            when(qualityMeasureClient.getMeasureSummary(TENANT_ID)).thenReturn(Map.of());
            when(hccClient.getRafScoreSummary(TENANT_ID)).thenReturn(Map.of());
            when(careGapClient.getCareGapSummary(TENANT_ID)).thenReturn(Map.of());

            // When
            Map<String, Object> result = service.getAllKpis(TENANT_ID);

            // Then
            assertThat(result.get("asOfDate")).isEqualTo(LocalDate.now());
        }
    }

    @Nested
    @DisplayName("Get Quality KPIs Tests")
    class GetQualityKpisTests {

        @Test
        @DisplayName("Should return quality KPIs from client")
        void shouldReturnQualityKpis() {
            // Given
            Map<String, Object> summary = Map.of(
                    "overallScore", 88.5,
                    "starRating", 4.5,
                    "measuresMet", 18
            );
            when(qualityMeasureClient.getMeasureSummary(TENANT_ID)).thenReturn(summary);
            when(snapshotRepository.findLatestSnapshots(any(), any(), any(), any()))
                    .thenReturn(List.of());

            // When
            List<KpiSummaryDto> result = service.getQualityKpis(TENANT_ID);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.stream().map(KpiSummaryDto::getMetricName))
                    .containsExactly("Overall Quality Score", "Star Rating", "Measures Met");
        }

        @Test
        @DisplayName("Should calculate change percentage from historical data")
        void shouldCalculateChangePercent() {
            // Given
            Map<String, Object> summary = Map.of("overallScore", 90.0);

            MetricSnapshotEntity previousSnapshot = MetricSnapshotEntity.builder()
                    .metricValue(BigDecimal.valueOf(80.0))
                    .snapshotDate(LocalDate.now().minusDays(7))
                    .build();

            when(qualityMeasureClient.getMeasureSummary(TENANT_ID)).thenReturn(summary);
            when(snapshotRepository.findLatestSnapshots(eq(TENANT_ID), eq("QUALITY_SCORE"),
                    eq("Overall Quality Score"), any(PageRequest.class)))
                    .thenReturn(List.of(previousSnapshot, previousSnapshot));

            // When
            List<KpiSummaryDto> result = service.getQualityKpis(TENANT_ID);

            // Then
            assertThat(result.get(0).getChangePercent()).isNotNull();
        }

        @Test
        @DisplayName("Should use fallback when client fails")
        void shouldUseFallbackOnFailure() {
            // Given
            when(qualityMeasureClient.getMeasureSummary(TENANT_ID))
                    .thenThrow(new RuntimeException("Connection failed"));
            when(snapshotRepository.findDistinctMetricNames(TENANT_ID, "QUALITY_SCORE"))
                    .thenReturn(List.of("Overall Quality Score"));
            when(snapshotRepository.findLatestSnapshots(eq(TENANT_ID), eq("QUALITY_SCORE"),
                    any(), any(PageRequest.class)))
                    .thenReturn(List.of(createSnapshot("Overall Quality Score", 85.0)));

            // When
            List<KpiSummaryDto> result = service.getQualityKpis(TENANT_ID);

            // Then
            assertThat(result).isNotEmpty();
            verify(snapshotRepository).findDistinctMetricNames(TENANT_ID, "QUALITY_SCORE");
        }

        @Test
        @DisplayName("Should handle null summary response")
        void shouldHandleNullSummary() {
            // Given
            when(qualityMeasureClient.getMeasureSummary(TENANT_ID)).thenReturn(null);

            // When
            List<KpiSummaryDto> result = service.getQualityKpis(TENANT_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get HCC KPIs Tests")
    class GetHccKpisTests {

        @Test
        @DisplayName("Should return HCC KPIs from client")
        void shouldReturnHccKpis() {
            // Given
            Map<String, Object> summary = Map.of(
                    "averageRafScore", 1.35,
                    "rafScoreGap", 0.20,
                    "suspectedHccCount", 55
            );
            when(hccClient.getRafScoreSummary(TENANT_ID)).thenReturn(summary);
            when(snapshotRepository.findLatestSnapshots(any(), any(), any(), any()))
                    .thenReturn(List.of());

            // When
            List<KpiSummaryDto> result = service.getHccKpis(TENANT_ID);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.stream().map(KpiSummaryDto::getMetricName))
                    .containsExactly("Average RAF Score", "RAF Score Gap", "Suspected HCCs");
        }

        @Test
        @DisplayName("Should use fallback when HCC client fails")
        void shouldUseFallbackOnHccFailure() {
            // Given
            when(hccClient.getRafScoreSummary(TENANT_ID))
                    .thenThrow(new RuntimeException("HCC service unavailable"));
            when(snapshotRepository.findDistinctMetricNames(TENANT_ID, "RAF_SCORE"))
                    .thenReturn(List.of());

            // When
            List<KpiSummaryDto> result = service.getHccKpis(TENANT_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Care Gap KPIs Tests")
    class GetCareGapKpisTests {

        @Test
        @DisplayName("Should return care gap KPIs from client")
        void shouldReturnCareGapKpis() {
            // Given
            Map<String, Object> summary = Map.of(
                    "openGapCount", 150,
                    "closureRate", 68.5,
                    "highPriorityCount", 30
            );
            when(careGapClient.getCareGapSummary(TENANT_ID)).thenReturn(summary);
            when(snapshotRepository.findLatestSnapshots(any(), any(), any(), any()))
                    .thenReturn(List.of());

            // When
            List<KpiSummaryDto> result = service.getCareGapKpis(TENANT_ID);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.stream().map(KpiSummaryDto::getMetricName))
                    .containsExactly("Open Care Gaps", "Closure Rate", "High Priority Gaps");
        }

        @Test
        @DisplayName("Should use fallback when care gap client fails")
        void shouldUseFallbackOnCareGapFailure() {
            // Given
            when(careGapClient.getCareGapSummary(TENANT_ID))
                    .thenThrow(new RuntimeException("Care gap service unavailable"));
            when(snapshotRepository.findDistinctMetricNames(TENANT_ID, "CARE_GAP_RATE"))
                    .thenReturn(List.of("Open Care Gaps"));
            when(snapshotRepository.findLatestSnapshots(eq(TENANT_ID), eq("CARE_GAP_RATE"),
                    any(), any(PageRequest.class)))
                    .thenReturn(List.of(createSnapshot("Open Care Gaps", 100)));

            // When
            List<KpiSummaryDto> result = service.getCareGapKpis(TENANT_ID);

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Get Trends Tests")
    class GetTrendsTests {

        @Test
        @DisplayName("Should return trend data for metric type")
        void shouldReturnTrendData() {
            // Given
            LocalDate today = LocalDate.now();
            List<MetricSnapshotEntity> snapshots = List.of(
                    createSnapshotWithDate("Quality Score", 80.0, today.minusDays(6)),
                    createSnapshotWithDate("Quality Score", 82.0, today.minusDays(4)),
                    createSnapshotWithDate("Quality Score", 85.0, today.minusDays(2)),
                    createSnapshotWithDate("Quality Score", 88.0, today)
            );

            when(snapshotRepository.findByTenantIdAndMetricTypeAndSnapshotDateBetween(
                    eq(TENANT_ID), eq("QUALITY_SCORE"), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(snapshots);

            // When
            List<KpiSummaryDto> result = service.getTrends(TENANT_ID, "QUALITY_SCORE", 7);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTrendData()).hasSize(4);
        }

        @Test
        @DisplayName("Should calculate change percent in trend")
        void shouldCalculateChangePercent() {
            // Given
            LocalDate today = LocalDate.now();
            List<MetricSnapshotEntity> snapshots = List.of(
                    createSnapshotWithDate("Quality Score", 80.0, today.minusDays(3)),
                    createSnapshotWithDate("Quality Score", 100.0, today)
            );

            when(snapshotRepository.findByTenantIdAndMetricTypeAndSnapshotDateBetween(
                    eq(TENANT_ID), eq("QUALITY_SCORE"), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(snapshots);

            // When
            List<KpiSummaryDto> result = service.getTrends(TENANT_ID, "QUALITY_SCORE", 7);

            // Then
            assertThat(result.get(0).getChangePercent()).isEqualTo(BigDecimal.valueOf(25.00).setScale(2));
        }

        @Test
        @DisplayName("Should handle empty trend data")
        void shouldHandleEmptyTrendData() {
            // Given
            when(snapshotRepository.findByTenantIdAndMetricTypeAndSnapshotDateBetween(
                    eq(TENANT_ID), eq("QUALITY_SCORE"), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of());

            // When
            List<KpiSummaryDto> result = service.getTrends(TENANT_ID, "QUALITY_SCORE", 30);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should group trend by metric name")
        void shouldGroupTrendByMetricName() {
            // Given
            LocalDate today = LocalDate.now();
            List<MetricSnapshotEntity> snapshots = List.of(
                    createSnapshotWithDate("Quality Score", 80.0, today.minusDays(2)),
                    createSnapshotWithDate("Quality Score", 85.0, today),
                    createSnapshotWithDate("Star Rating", 4.0, today.minusDays(2)),
                    createSnapshotWithDate("Star Rating", 4.5, today)
            );

            when(snapshotRepository.findByTenantIdAndMetricTypeAndSnapshotDateBetween(
                    eq(TENANT_ID), eq("QUALITY_SCORE"), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(snapshots);

            // When
            List<KpiSummaryDto> result = service.getTrends(TENANT_ID, "QUALITY_SCORE", 7);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.stream().map(KpiSummaryDto::getMetricName))
                    .containsExactlyInAnyOrder("Quality Score", "Star Rating");
        }
    }

    @Nested
    @DisplayName("Fallback Tests")
    class FallbackTests {

        @Test
        @DisplayName("Should return historical data on quality fallback")
        void shouldReturnHistoricalOnQualityFallback() {
            // Given
            when(snapshotRepository.findDistinctMetricNames(TENANT_ID, "QUALITY_SCORE"))
                    .thenReturn(List.of("Overall Score", "Star Rating"));
            when(snapshotRepository.findLatestSnapshots(eq(TENANT_ID), eq("QUALITY_SCORE"),
                    eq("Overall Score"), any(PageRequest.class)))
                    .thenReturn(List.of(createSnapshot("Overall Score", 85.0)));
            when(snapshotRepository.findLatestSnapshots(eq(TENANT_ID), eq("QUALITY_SCORE"),
                    eq("Star Rating"), any(PageRequest.class)))
                    .thenReturn(List.of(createSnapshot("Star Rating", 4.0)));

            // When
            List<KpiSummaryDto> result = service.getQualityKpisFallback(TENANT_ID,
                    new RuntimeException("Service unavailable"));

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list when no historical data")
        void shouldReturnEmptyWhenNoHistoricalData() {
            // Given
            when(snapshotRepository.findDistinctMetricNames(TENANT_ID, "QUALITY_SCORE"))
                    .thenReturn(List.of());

            // When
            List<KpiSummaryDto> result = service.getQualityKpisFallback(TENANT_ID,
                    new RuntimeException("Service unavailable"));

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return historical data on HCC fallback")
        void shouldReturnHistoricalOnHccFallback() {
            // Given
            when(snapshotRepository.findDistinctMetricNames(TENANT_ID, "RAF_SCORE"))
                    .thenReturn(List.of("Average RAF Score"));
            when(snapshotRepository.findLatestSnapshots(eq(TENANT_ID), eq("RAF_SCORE"),
                    any(), any(PageRequest.class)))
                    .thenReturn(List.of(createSnapshot("Average RAF Score", 1.25)));

            // When
            List<KpiSummaryDto> result = service.getHccKpisFallback(TENANT_ID,
                    new RuntimeException("Service unavailable"));

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCurrentValue()).isEqualTo(BigDecimal.valueOf(1.25));
        }

        @Test
        @DisplayName("Should return historical data on care gap fallback")
        void shouldReturnHistoricalOnCareGapFallback() {
            // Given
            when(snapshotRepository.findDistinctMetricNames(TENANT_ID, "CARE_GAP_RATE"))
                    .thenReturn(List.of("Closure Rate"));
            when(snapshotRepository.findLatestSnapshots(eq(TENANT_ID), eq("CARE_GAP_RATE"),
                    any(), any(PageRequest.class)))
                    .thenReturn(List.of(createSnapshot("Closure Rate", 72.5)));

            // When
            List<KpiSummaryDto> result = service.getCareGapKpisFallback(TENANT_ID,
                    new RuntimeException("Service unavailable"));

            // Then
            assertThat(result).hasSize(1);
        }
    }

    // ==================== Helper Methods ====================

    private MetricSnapshotEntity createSnapshot(String metricName, double value) {
        return MetricSnapshotEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .metricType("QUALITY_SCORE")
                .metricName(metricName)
                .metricValue(BigDecimal.valueOf(value))
                .snapshotDate(LocalDate.now())
                .build();
    }

    private MetricSnapshotEntity createSnapshotWithDate(String metricName, double value, LocalDate date) {
        return MetricSnapshotEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .metricType("QUALITY_SCORE")
                .metricName(metricName)
                .metricValue(BigDecimal.valueOf(value))
                .snapshotDate(date)
                .build();
    }
}
