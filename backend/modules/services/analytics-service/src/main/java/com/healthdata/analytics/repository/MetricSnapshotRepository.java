package com.healthdata.analytics.repository;

import com.healthdata.analytics.persistence.MetricSnapshotEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetricSnapshotRepository extends JpaRepository<MetricSnapshotEntity, UUID> {

    List<MetricSnapshotEntity> findByTenantIdAndMetricType(String tenantId, String metricType);

    List<MetricSnapshotEntity> findByTenantIdAndMetricTypeAndSnapshotDateBetween(
            String tenantId, String metricType, LocalDate startDate, LocalDate endDate);

    @Query("SELECT m FROM MetricSnapshotEntity m WHERE m.tenantId = :tenantId AND m.metricType = :metricType " +
           "AND m.metricName = :metricName ORDER BY m.snapshotDate DESC")
    List<MetricSnapshotEntity> findLatestSnapshots(
            @Param("tenantId") String tenantId,
            @Param("metricType") String metricType,
            @Param("metricName") String metricName,
            Pageable pageable);

    @Query("SELECT m FROM MetricSnapshotEntity m WHERE m.tenantId = :tenantId AND m.metricType = :metricType " +
           "AND m.snapshotDate = :date")
    List<MetricSnapshotEntity> findByTenantIdAndMetricTypeAndDate(
            @Param("tenantId") String tenantId,
            @Param("metricType") String metricType,
            @Param("date") LocalDate date);

    @Query("SELECT DISTINCT m.metricName FROM MetricSnapshotEntity m WHERE m.tenantId = :tenantId AND m.metricType = :metricType")
    List<String> findDistinctMetricNames(@Param("tenantId") String tenantId, @Param("metricType") String metricType);

    @Query("SELECT MAX(m.snapshotDate) FROM MetricSnapshotEntity m WHERE m.tenantId = :tenantId AND m.metricType = :metricType")
    Optional<LocalDate> findLatestSnapshotDate(@Param("tenantId") String tenantId, @Param("metricType") String metricType);

    @Modifying
    @Query("DELETE FROM MetricSnapshotEntity m WHERE m.snapshotDate < :threshold")
    int deleteOldSnapshots(@Param("threshold") LocalDate threshold);

    @Query("SELECT COUNT(m) FROM MetricSnapshotEntity m WHERE m.tenantId = :tenantId AND m.metricType = :metricType")
    long countByTenantIdAndMetricType(@Param("tenantId") String tenantId, @Param("metricType") String metricType);
}
