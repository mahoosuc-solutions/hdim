package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Chronic Disease Monitoring operations
 */
@Repository
public interface ChronicDiseaseMonitoringRepository extends JpaRepository<ChronicDiseaseMonitoringEntity, UUID> {

    /**
     * Find monitoring record for a specific patient and disease
     */
    Optional<ChronicDiseaseMonitoringEntity> findByTenantIdAndPatientIdAndDiseaseCode(
        String tenantId,
        UUID patientId,
        String diseaseCode
    );

    /**
     * Find all monitoring records for a patient
     */
    List<ChronicDiseaseMonitoringEntity> findByTenantIdAndPatientIdOrderByMonitoredAtDesc(
        String tenantId,
        UUID patientId
    );

    /**
     * Find all patients with deteriorating trends
     */
    List<ChronicDiseaseMonitoringEntity> findByTenantIdAndTrendOrderByMonitoredAtDesc(
        String tenantId,
        ChronicDiseaseMonitoringEntity.Trend trend
    );

    /**
     * Find all patients with active alerts
     */
    List<ChronicDiseaseMonitoringEntity> findByTenantIdAndAlertTriggeredTrueOrderByMonitoredAtDesc(
        String tenantId
    );

    /**
     * Find patients due for monitoring
     */
    @Query("SELECT m FROM ChronicDiseaseMonitoringEntity m " +
           "WHERE m.tenantId = :tenantId " +
           "AND m.nextMonitoringDue <= :dueDate " +
           "ORDER BY m.nextMonitoringDue ASC")
    List<ChronicDiseaseMonitoringEntity> findDueForMonitoring(
        @Param("tenantId") String tenantId,
        @Param("dueDate") Instant dueDate
    );

    /**
     * Count deteriorating diseases by tenant
     */
    @Query("SELECT COUNT(m) FROM ChronicDiseaseMonitoringEntity m " +
           "WHERE m.tenantId = :tenantId AND m.trend = 'DETERIORATING'")
    Long countDeterioratingByTenantId(@Param("tenantId") String tenantId);

    /**
     * Count active alerts by tenant
     */
    Long countByTenantIdAndAlertTriggeredTrue(String tenantId);

    /**
     * Find patients with specific disease and deteriorating trend
     */
    List<ChronicDiseaseMonitoringEntity> findByTenantIdAndDiseaseCodeAndTrend(
        String tenantId,
        String diseaseCode,
        ChronicDiseaseMonitoringEntity.Trend trend
    );
}
