package com.healthdata.audit.repository.ai;

import com.healthdata.audit.entity.ai.ConfigurationEngineEventEntity;
import com.healthdata.audit.models.ai.ConfigurationEngineEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for configuration engine change events.
 */
@Repository
public interface ConfigurationEngineEventRepository extends JpaRepository<ConfigurationEngineEventEntity, UUID> {

    /**
     * Find all configuration changes for a tenant.
     */
    Page<ConfigurationEngineEventEntity> findByTenantIdAndTimestampBetween(
        String tenantId,
        Instant startTime,
        Instant endTime,
        Pageable pageable
    );

    /**
     * Find configuration changes by service name.
     */
    Page<ConfigurationEngineEventEntity> findByServiceNameAndTimestampBetween(
        String serviceName,
        Instant startTime,
        Instant endTime,
        Pageable pageable
    );

    /**
     * Find configuration changes by type.
     */
    List<ConfigurationEngineEventEntity> findByChangeType(
        ConfigurationEngineEvent.ChangeType changeType
    );

    /**
     * Find configuration changes by execution status.
     */
    List<ConfigurationEngineEventEntity> findByExecutionStatus(
        ConfigurationEngineEvent.ExecutionStatus executionStatus
    );

    /**
     * Find configuration changes by correlation ID.
     */
    List<ConfigurationEngineEventEntity> findByCorrelationIdOrderByTimestampAsc(
        String correlationId
    );

    /**
     * Find configuration changes linked to an AI recommendation.
     */
    List<ConfigurationEngineEventEntity> findByAiRecommendationId(
        UUID aiRecommendationId
    );

    /**
     * Find configuration changes by change ID.
     */
    Optional<ConfigurationEngineEventEntity> findByChangeId(UUID changeId);

    /**
     * Find configuration changes for a specific config key.
     */
    @Query("SELECT e FROM ConfigurationEngineEventEntity e WHERE e.configKey = :configKey " +
           "AND e.timestamp BETWEEN :startTime AND :endTime ORDER BY e.timestamp DESC")
    List<ConfigurationEngineEventEntity> findConfigurationHistory(
        @Param("configKey") String configKey,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find high-risk changes (production + global scope).
     */
    @Query("SELECT e FROM ConfigurationEngineEventEntity e WHERE e.environment = 'PROD' " +
           "AND e.configurationScope = 'GLOBAL' AND e.timestamp BETWEEN :startTime AND :endTime")
    List<ConfigurationEngineEventEntity> findHighRiskChanges(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find changes that required rollback.
     */
    @Query("SELECT e FROM ConfigurationEngineEventEntity e WHERE e.rollbackRequired = true " +
           "AND e.timestamp BETWEEN :startTime AND :endTime")
    List<ConfigurationEngineEventEntity> findRolledBackChanges(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Count changes by source.
     */
    @Query("SELECT e.changeSource, COUNT(e) FROM ConfigurationEngineEventEntity e " +
           "WHERE e.timestamp BETWEEN :startTime AND :endTime GROUP BY e.changeSource")
    List<Object[]> countChangesBySource(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find changes with negative performance impact.
     */
    @Query("SELECT e FROM ConfigurationEngineEventEntity e WHERE " +
           "e.impactSeverity IN ('NEGATIVE_SIGNIFICANT', 'CRITICAL') " +
           "AND e.timestamp BETWEEN :startTime AND :endTime")
    List<ConfigurationEngineEventEntity> findChangesWithNegativeImpact(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );
}
