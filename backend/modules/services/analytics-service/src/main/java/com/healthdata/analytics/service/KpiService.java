package com.healthdata.analytics.service;

import com.healthdata.analytics.client.CareGapClient;
import com.healthdata.analytics.client.HccClient;
import com.healthdata.analytics.client.QualityMeasureClient;
import com.healthdata.analytics.dto.KpiSummaryDto;
import com.healthdata.analytics.persistence.MetricSnapshotEntity;
import com.healthdata.analytics.repository.MetricSnapshotRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KpiService {

    private final QualityMeasureClient qualityMeasureClient;
    private final HccClient hccClient;
    private final CareGapClient careGapClient;
    private final MetricSnapshotRepository snapshotRepository;

    @Cacheable(value = "kpi-summary", key = "#tenantId", unless = "#result == null")
    public Map<String, Object> getAllKpis(String tenantId) {
        Map<String, Object> result = new HashMap<>();
        result.put("quality", getQualityKpis(tenantId));
        result.put("hcc", getHccKpis(tenantId));
        result.put("careGaps", getCareGapKpis(tenantId));
        result.put("asOfDate", LocalDate.now());
        return result;
    }

    @CircuitBreaker(name = "quality-measure", fallbackMethod = "getQualityKpisFallback")
    public List<KpiSummaryDto> getQualityKpis(String tenantId) {
        try {
            Map<String, Object> summary = qualityMeasureClient.getMeasureSummary(tenantId);
            List<KpiSummaryDto> kpis = new ArrayList<>();

            if (summary != null) {
                kpis.add(buildKpiFromMetric(tenantId, "QUALITY_SCORE", "Overall Quality Score", summary.get("overallScore")));
                kpis.add(buildKpiFromMetric(tenantId, "STAR_RATING", "Star Rating", summary.get("starRating")));
                kpis.add(buildKpiFromMetric(tenantId, "QUALITY_SCORE", "Measures Met", summary.get("measuresMet")));
            }

            return kpis;
        } catch (Exception e) {
            log.error("Failed to fetch quality KPIs for tenant {}: {}", tenantId, e.getMessage());
            return getQualityKpisFallback(tenantId, e);
        }
    }

    public List<KpiSummaryDto> getQualityKpisFallback(String tenantId, Throwable t) {
        log.warn("Using fallback for quality KPIs: {}", t.getMessage());
        return getHistoricalKpis(tenantId, "QUALITY_SCORE");
    }

    @CircuitBreaker(name = "hcc", fallbackMethod = "getHccKpisFallback")
    public List<KpiSummaryDto> getHccKpis(String tenantId) {
        try {
            Map<String, Object> summary = hccClient.getRafScoreSummary(tenantId);
            List<KpiSummaryDto> kpis = new ArrayList<>();

            if (summary != null) {
                kpis.add(buildKpiFromMetric(tenantId, "RAF_SCORE", "Average RAF Score", summary.get("averageRafScore")));
                kpis.add(buildKpiFromMetric(tenantId, "RAF_SCORE", "RAF Score Gap", summary.get("rafScoreGap")));
                kpis.add(buildKpiFromMetric(tenantId, "HCC_COUNT", "Suspected HCCs", summary.get("suspectedHccCount")));
            }

            return kpis;
        } catch (Exception e) {
            log.error("Failed to fetch HCC KPIs for tenant {}: {}", tenantId, e.getMessage());
            return getHccKpisFallback(tenantId, e);
        }
    }

    public List<KpiSummaryDto> getHccKpisFallback(String tenantId, Throwable t) {
        log.warn("Using fallback for HCC KPIs: {}", t.getMessage());
        return getHistoricalKpis(tenantId, "RAF_SCORE");
    }

    @CircuitBreaker(name = "care-gap", fallbackMethod = "getCareGapKpisFallback")
    public List<KpiSummaryDto> getCareGapKpis(String tenantId) {
        try {
            Map<String, Object> summary = careGapClient.getCareGapSummary(tenantId);
            List<KpiSummaryDto> kpis = new ArrayList<>();

            if (summary != null) {
                kpis.add(buildKpiFromMetric(tenantId, "CARE_GAP_RATE", "Open Care Gaps", summary.get("openGapCount")));
                kpis.add(buildKpiFromMetric(tenantId, "CARE_GAP_RATE", "Closure Rate", summary.get("closureRate")));
                kpis.add(buildKpiFromMetric(tenantId, "CARE_GAP_RATE", "High Priority Gaps", summary.get("highPriorityCount")));
            }

            return kpis;
        } catch (Exception e) {
            log.error("Failed to fetch care gap KPIs for tenant {}: {}", tenantId, e.getMessage());
            return getCareGapKpisFallback(tenantId, e);
        }
    }

    public List<KpiSummaryDto> getCareGapKpisFallback(String tenantId, Throwable t) {
        log.warn("Using fallback for care gap KPIs: {}", t.getMessage());
        return getHistoricalKpis(tenantId, "CARE_GAP_RATE");
    }

    public List<KpiSummaryDto> getTrends(String tenantId, String metricType, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        List<MetricSnapshotEntity> snapshots = snapshotRepository
                .findByTenantIdAndMetricTypeAndSnapshotDateBetween(tenantId, metricType, startDate, endDate);

        Map<String, List<MetricSnapshotEntity>> byMetricName = new HashMap<>();
        for (MetricSnapshotEntity snapshot : snapshots) {
            byMetricName.computeIfAbsent(snapshot.getMetricName(), k -> new ArrayList<>()).add(snapshot);
        }

        List<KpiSummaryDto> trends = new ArrayList<>();
        for (Map.Entry<String, List<MetricSnapshotEntity>> entry : byMetricName.entrySet()) {
            List<MetricSnapshotEntity> metricSnapshots = entry.getValue();
            metricSnapshots.sort(Comparator.comparing(MetricSnapshotEntity::getSnapshotDate));

            List<KpiSummaryDto.TrendPointDto> trendPoints = new ArrayList<>();
            for (MetricSnapshotEntity snapshot : metricSnapshots) {
                trendPoints.add(KpiSummaryDto.TrendPointDto.builder()
                        .date(snapshot.getSnapshotDate())
                        .value(snapshot.getMetricValue())
                        .build());
            }

            BigDecimal currentValue = metricSnapshots.isEmpty() ? BigDecimal.ZERO :
                    metricSnapshots.get(metricSnapshots.size() - 1).getMetricValue();
            BigDecimal previousValue = metricSnapshots.size() < 2 ? BigDecimal.ZERO :
                    metricSnapshots.get(0).getMetricValue();

            BigDecimal changePercent = calculateChangePercent(currentValue, previousValue);

            trends.add(KpiSummaryDto.builder()
                    .metricType(metricType)
                    .metricName(entry.getKey())
                    .currentValue(currentValue)
                    .previousValue(previousValue)
                    .changePercent(changePercent)
                    .asOfDate(LocalDate.now())
                    .trendData(trendPoints)
                    .build());
        }

        return trends;
    }

    private KpiSummaryDto buildKpiFromMetric(String tenantId, String metricType, String metricName, Object value) {
        BigDecimal currentValue = toBigDecimal(value);

        List<MetricSnapshotEntity> previous = snapshotRepository.findLatestSnapshots(
                tenantId, metricType, metricName, PageRequest.of(0, 2));

        BigDecimal previousValue = previous.size() > 1 ? previous.get(1).getMetricValue() : BigDecimal.ZERO;
        BigDecimal changePercent = calculateChangePercent(currentValue, previousValue);

        return KpiSummaryDto.builder()
                .metricType(metricType)
                .metricName(metricName)
                .currentValue(currentValue)
                .previousValue(previousValue)
                .changePercent(changePercent)
                .asOfDate(LocalDate.now())
                .build();
    }

    private List<KpiSummaryDto> getHistoricalKpis(String tenantId, String metricType) {
        List<String> metricNames = snapshotRepository.findDistinctMetricNames(tenantId, metricType);
        List<KpiSummaryDto> kpis = new ArrayList<>();

        for (String metricName : metricNames) {
            List<MetricSnapshotEntity> snapshots = snapshotRepository.findLatestSnapshots(
                    tenantId, metricType, metricName, PageRequest.of(0, 2));

            if (!snapshots.isEmpty()) {
                MetricSnapshotEntity latest = snapshots.get(0);
                BigDecimal previousValue = snapshots.size() > 1 ? snapshots.get(1).getMetricValue() : BigDecimal.ZERO;

                kpis.add(KpiSummaryDto.builder()
                        .metricType(metricType)
                        .metricName(metricName)
                        .currentValue(latest.getMetricValue())
                        .previousValue(previousValue)
                        .changePercent(calculateChangePercent(latest.getMetricValue(), previousValue))
                        .asOfDate(latest.getSnapshotDate())
                        .build());
            }
        }

        return kpis;
    }

    private BigDecimal calculateChangePercent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
