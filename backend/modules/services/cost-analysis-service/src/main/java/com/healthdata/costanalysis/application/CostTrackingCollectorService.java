package com.healthdata.costanalysis.application;

import com.healthdata.costanalysis.domain.model.CostTrackingEntity;
import com.healthdata.costanalysis.domain.repository.CostTrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CostTrackingCollectorService {

    private static final BigDecimal CPU_HOURLY_RATE = new BigDecimal("0.09");
    private static final BigDecimal MEMORY_GB_HOURLY_RATE = new BigDecimal("0.02");
    private static final BigDecimal NETWORK_GB_RATE = new BigDecimal("0.04");

    private final CostTrackingRepository costTrackingRepository;

    @Transactional
    public CostTrackingEntity recordMethodExecution(
        String tenantId,
        String serviceId,
        String metricType,
        String featureKey,
        long durationMs
    ) {
        BigDecimal executionCost = estimateExecutionCost(durationMs);
        return costTrackingRepository.save(CostTrackingEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .serviceId(serviceId)
            .metricType(metricType)
            .metricValue(BigDecimal.valueOf(durationMs))
            .costAmount(executionCost)
            .featureKey(featureKey)
            .timestampUtc(Instant.now())
            .build());
    }

    public BigDecimal estimateExecutionCost(long durationMs) {
        BigDecimal durationHours = BigDecimal.valueOf(durationMs).divide(BigDecimal.valueOf(3_600_000), 8, RoundingMode.HALF_UP);
        BigDecimal cpuCost = CPU_HOURLY_RATE.multiply(durationHours);
        BigDecimal memoryCost = MEMORY_GB_HOURLY_RATE.multiply(durationHours);
        BigDecimal networkCost = NETWORK_GB_RATE.multiply(durationHours).divide(BigDecimal.valueOf(4), 8, RoundingMode.HALF_UP);
        return cpuCost.add(memoryCost).add(networkCost).setScale(4, RoundingMode.HALF_UP);
    }
}
