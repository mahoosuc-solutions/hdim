package com.healthdata.analytics.repository;

import com.healthdata.analytics.persistence.AlertRuleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRuleEntity, UUID> {

    List<AlertRuleEntity> findByTenantId(String tenantId);

    Page<AlertRuleEntity> findByTenantId(String tenantId, Pageable pageable);

    Optional<AlertRuleEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<AlertRuleEntity> findByTenantIdAndIsActiveTrue(String tenantId);

    List<AlertRuleEntity> findByTenantIdAndMetricType(String tenantId, String metricType);

    @Query("SELECT a FROM AlertRuleEntity a WHERE a.isActive = true AND a.metricType = :metricType")
    List<AlertRuleEntity> findActiveRulesForMetricType(@Param("metricType") String metricType);

    @Query("SELECT a FROM AlertRuleEntity a WHERE a.tenantId = :tenantId AND a.isActive = true AND a.severity = :severity")
    List<AlertRuleEntity> findActiveRulesBySeverity(@Param("tenantId") String tenantId, @Param("severity") String severity);

    @Query("SELECT a FROM AlertRuleEntity a WHERE a.tenantId = :tenantId AND a.lastTriggeredAt IS NOT NULL " +
           "ORDER BY a.lastTriggeredAt DESC")
    List<AlertRuleEntity> findRecentlyTriggeredRules(@Param("tenantId") String tenantId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM AlertRuleEntity a WHERE a.tenantId = :tenantId AND a.isActive = true")
    long countActiveRules(@Param("tenantId") String tenantId);

    boolean existsByIdAndTenantId(UUID id, String tenantId);
}
