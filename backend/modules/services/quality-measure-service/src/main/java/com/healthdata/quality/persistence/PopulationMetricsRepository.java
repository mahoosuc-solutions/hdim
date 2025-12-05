package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Population Metrics Repository (CQRS Read Model)
 *
 * Repository for pre-aggregated population metrics.
 */
@Repository
public interface PopulationMetricsRepository extends JpaRepository<PopulationMetricsEntity, Long> {

    /**
     * Find metrics for specific tenant and date
     */
    Optional<PopulationMetricsEntity> findByTenantIdAndMetricDate(String tenantId, LocalDate metricDate);

    /**
     * Find latest metrics for tenant
     */
    Optional<PopulationMetricsEntity> findTopByTenantIdOrderByMetricDateDesc(String tenantId);

    /**
     * Find metrics for date range
     */
    List<PopulationMetricsEntity> findByTenantIdAndMetricDateBetweenOrderByMetricDateDesc(
        String tenantId,
        LocalDate startDate,
        LocalDate endDate
    );

    /**
     * Find all metrics for tenant (for trending)
     */
    List<PopulationMetricsEntity> findByTenantIdOrderByMetricDateDesc(String tenantId);
}
