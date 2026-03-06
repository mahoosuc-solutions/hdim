package com.healthdata.events.intelligence.projection;

import com.healthdata.events.intelligence.dto.TenantTrustDashboardResponse;
import com.healthdata.events.intelligence.entity.IntelligenceTenantTrustProjectionEntity;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.FindingStatus;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.FindingType;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.Severity;
import com.healthdata.events.intelligence.repository.IntelligenceTenantTrustProjectionRepository;
import com.healthdata.events.intelligence.repository.IntelligenceValidationFindingRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class TenantTrustProjectionService {

    private final IntelligenceValidationFindingRepository findingRepository;
    private final IntelligenceTenantTrustProjectionRepository projectionRepository;
    private final ConcurrentMap<String, AtomicLong> trustProjectionLastUpdatedEpochByTenant = new ConcurrentHashMap<>();

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TenantTrustDashboardResponse refreshForTenant(String tenantId) {
        List<IntelligenceValidationFindingEntity> openFindings =
                findingRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, FindingStatus.OPEN);

        long total = openFindings.size();
        long high = openFindings.stream().filter(f -> f.getSeverity() == Severity.HIGH).count();
        long consistency = openFindings.stream().filter(f -> f.getFindingType() == FindingType.CONSISTENCY).count();
        long completeness = openFindings.stream().filter(f -> f.getFindingType() == FindingType.DATA_COMPLETENESS).count();
        long temporal = openFindings.stream().filter(f -> f.getFindingType() == FindingType.TEMPORAL).count();

        int trustScore = 100;
        for (IntelligenceValidationFindingEntity finding : openFindings) {
            trustScore -= switch (finding.getSeverity()) {
                case HIGH -> 20;
                case MEDIUM -> 10;
                case LOW -> 5;
            };
        }
        trustScore = Math.max(0, trustScore);

        IntelligenceTenantTrustProjectionEntity projection = IntelligenceTenantTrustProjectionEntity.builder()
                .tenantId(tenantId)
                .totalOpenFindings(total)
                .highSeverityOpenFindings(high)
                .consistencyOpenFindings(consistency)
                .dataCompletenessOpenFindings(completeness)
                .temporalOpenFindings(temporal)
                .trustScore(trustScore)
                .lastUpdatedAt(Instant.now())
                .build();

        IntelligenceTenantTrustProjectionEntity saved = projectionRepository.save(projection);
        updateProjectionMetrics(saved.getTenantId(), saved.getLastUpdatedAt());

        return new TenantTrustDashboardResponse(
                saved.getTenantId(),
                saved.getTrustScore(),
                saved.getTotalOpenFindings(),
                saved.getHighSeverityOpenFindings(),
                saved.getConsistencyOpenFindings(),
                saved.getDataCompletenessOpenFindings(),
                saved.getTemporalOpenFindings(),
                saved.getLastUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public TenantTrustDashboardResponse getTenantDashboard(String tenantId) {
        return projectionRepository.findById(tenantId)
                .map(saved -> new TenantTrustDashboardResponse(
                        saved.getTenantId(),
                        saved.getTrustScore(),
                        saved.getTotalOpenFindings(),
                        saved.getHighSeverityOpenFindings(),
                        saved.getConsistencyOpenFindings(),
                        saved.getDataCompletenessOpenFindings(),
                        saved.getTemporalOpenFindings(),
                        saved.getLastUpdatedAt()
                ))
                .orElseGet(() -> refreshForTenant(tenantId));
    }

    private void updateProjectionMetrics(String tenantId, Instant lastUpdatedAt) {
        AtomicLong lastUpdatedEpoch = trustProjectionLastUpdatedEpochByTenant.computeIfAbsent(tenantId, key -> {
            AtomicLong gaugeRef = new AtomicLong(0);
            Gauge.builder("intelligence.trust.projection.last_updated.epoch.seconds", gaugeRef, AtomicLong::doubleValue)
                    .description("Last update time (epoch seconds) of tenant trust projection")
                    .tag("tenant_id", tenantId)
                    .register(Metrics.globalRegistry);
            Gauge.builder("intelligence.trust.projection.freshness.seconds", gaugeRef, ref ->
                            Math.max(0, Duration.between(Instant.ofEpochSecond(ref.get()), Instant.now()).getSeconds()))
                    .description("Seconds since tenant trust projection was last refreshed")
                    .tag("tenant_id", tenantId)
                    .register(Metrics.globalRegistry);
            return gaugeRef;
        });
        lastUpdatedEpoch.set(lastUpdatedAt.getEpochSecond());
    }
}
