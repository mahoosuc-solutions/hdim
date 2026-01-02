package com.healthdata.analytics.service;

import com.healthdata.analytics.dto.KpiSummaryDto;
import com.healthdata.analytics.persistence.MetricSnapshotEntity;
import com.healthdata.analytics.repository.MetricSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricAggregationService {

    private final MetricSnapshotRepository snapshotRepository;
    private final KpiService kpiService;

    @Transactional
    public MetricSnapshotEntity saveSnapshot(String tenantId, String metricType, String metricName,
                                              BigDecimal value, Map<String, Object> dimensions) {
        MetricSnapshotEntity snapshot = MetricSnapshotEntity.builder()
                .tenantId(tenantId)
                .metricType(metricType)
                .metricName(metricName)
                .metricValue(value)
                .dimensions(dimensions)
                .snapshotDate(LocalDate.now())
                .build();

        snapshot = snapshotRepository.save(snapshot);
        log.debug("Saved metric snapshot: {} = {} for tenant {}", metricName, value, tenantId);
        return snapshot;
    }

    @Transactional
    public void captureKpiSnapshots(String tenantId) {
        log.info("Capturing KPI snapshots for tenant {}", tenantId);

        try {
            captureQualitySnapshots(tenantId);
            captureHccSnapshots(tenantId);
            captureCareGapSnapshots(tenantId);
            log.info("Completed KPI snapshot capture for tenant {}", tenantId);
        } catch (Exception e) {
            log.error("Failed to capture KPI snapshots for tenant {}: {}", tenantId, e.getMessage(), e);
        }
    }

    private void captureQualitySnapshots(String tenantId) {
        List<KpiSummaryDto> kpis = kpiService.getQualityKpis(tenantId);
        for (KpiSummaryDto kpi : kpis) {
            saveSnapshot(tenantId, kpi.getMetricType(), kpi.getMetricName(),
                    kpi.getCurrentValue(), kpi.getBreakdown());
        }
    }

    private void captureHccSnapshots(String tenantId) {
        List<KpiSummaryDto> kpis = kpiService.getHccKpis(tenantId);
        for (KpiSummaryDto kpi : kpis) {
            saveSnapshot(tenantId, kpi.getMetricType(), kpi.getMetricName(),
                    kpi.getCurrentValue(), kpi.getBreakdown());
        }
    }

    private void captureCareGapSnapshots(String tenantId) {
        List<KpiSummaryDto> kpis = kpiService.getCareGapKpis(tenantId);
        for (KpiSummaryDto kpi : kpis) {
            saveSnapshot(tenantId, kpi.getMetricType(), kpi.getMetricName(),
                    kpi.getCurrentValue(), kpi.getBreakdown());
        }
    }

    @Transactional
    public int cleanupOldSnapshots(int retentionDays) {
        LocalDate threshold = LocalDate.now().minusDays(retentionDays);
        int deleted = snapshotRepository.deleteOldSnapshots(threshold);
        log.info("Cleaned up {} old metric snapshots older than {}", deleted, threshold);
        return deleted;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSnapshotStatistics(String tenantId) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("qualitySnapshotCount", snapshotRepository.countByTenantIdAndMetricType(tenantId, "QUALITY_SCORE"));
        stats.put("rafSnapshotCount", snapshotRepository.countByTenantIdAndMetricType(tenantId, "RAF_SCORE"));
        stats.put("careGapSnapshotCount", snapshotRepository.countByTenantIdAndMetricType(tenantId, "CARE_GAP_RATE"));

        snapshotRepository.findLatestSnapshotDate(tenantId, "QUALITY_SCORE")
                .ifPresent(date -> stats.put("latestQualitySnapshot", date));
        snapshotRepository.findLatestSnapshotDate(tenantId, "RAF_SCORE")
                .ifPresent(date -> stats.put("latestRafSnapshot", date));
        snapshotRepository.findLatestSnapshotDate(tenantId, "CARE_GAP_RATE")
                .ifPresent(date -> stats.put("latestCareGapSnapshot", date));

        return stats;
    }
}
