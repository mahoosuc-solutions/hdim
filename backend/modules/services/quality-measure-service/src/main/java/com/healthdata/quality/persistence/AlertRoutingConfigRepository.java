package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Alert Routing Configuration
 *
 * Provides methods for querying tenant-specific alert routing rules
 * to determine which roles/providers should receive specific alert types.
 */
@Repository
public interface AlertRoutingConfigRepository extends JpaRepository<AlertRoutingConfigEntity, UUID> {

    /**
     * Find all active routing configurations for a tenant
     *
     * @param tenantId Tenant ID
     * @return List of active routing configurations
     */
    List<AlertRoutingConfigEntity> findByTenantIdAndActiveOrderByPriorityAsc(
        String tenantId,
        boolean active
    );

    /**
     * Find routing configurations for specific alert type and severity
     * Ordered by priority (lower number = higher priority)
     *
     * @param tenantId Tenant ID
     * @param alertType Alert type
     * @param severity Severity level
     * @param active Whether to include only active configs
     * @return List of matching routing configurations
     */
    List<AlertRoutingConfigEntity> findByTenantIdAndAlertTypeAndSeverityAndActiveOrderByPriorityAsc(
        String tenantId,
        String alertType,
        String severity,
        boolean active
    );

    /**
     * Find routing configurations for specific alert type (any severity)
     * Ordered by priority
     *
     * @param tenantId Tenant ID
     * @param alertType Alert type
     * @param active Whether to include only active configs
     * @return List of matching routing configurations
     */
    List<AlertRoutingConfigEntity> findByTenantIdAndAlertTypeAndActiveOrderByPriorityAsc(
        String tenantId,
        String alertType,
        boolean active
    );

    /**
     * Find routing configurations including those with NULL severity (applies to all)
     * This is useful for finding both specific severity rules and general rules
     *
     * @param tenantId Tenant ID
     * @param alertType Alert type
     * @param severity Severity level
     * @param active Whether to include only active configs
     * @return List of matching routing configurations
     */
    @Query("""
        SELECT r FROM AlertRoutingConfigEntity r
        WHERE r.tenantId = :tenantId
        AND r.alertType = :alertType
        AND (r.severity = :severity OR r.severity IS NULL)
        AND r.active = :active
        ORDER BY r.priority ASC
        """)
    List<AlertRoutingConfigEntity> findRoutingConfigsForAlertAndSeverity(
        @Param("tenantId") String tenantId,
        @Param("alertType") String alertType,
        @Param("severity") String severity,
        @Param("active") boolean active
    );

    /**
     * Find business hours routing configurations
     *
     * @param tenantId Tenant ID
     * @param alertType Alert type
     * @param severity Severity level
     * @param active Whether to include only active configs
     * @return List of business hours routing configurations
     */
    @Query("""
        SELECT r FROM AlertRoutingConfigEntity r
        WHERE r.tenantId = :tenantId
        AND r.alertType = :alertType
        AND (r.severity = :severity OR r.severity IS NULL)
        AND r.active = :active
        AND (r.businessHoursOnly = true OR (r.businessHoursOnly = false AND r.afterHoursOnly = false))
        ORDER BY r.priority ASC
        """)
    List<AlertRoutingConfigEntity> findBusinessHoursRoutingConfigs(
        @Param("tenantId") String tenantId,
        @Param("alertType") String alertType,
        @Param("severity") String severity,
        @Param("active") boolean active
    );

    /**
     * Find after-hours routing configurations
     *
     * @param tenantId Tenant ID
     * @param alertType Alert type
     * @param severity Severity level
     * @param active Whether to include only active configs
     * @return List of after-hours routing configurations
     */
    @Query("""
        SELECT r FROM AlertRoutingConfigEntity r
        WHERE r.tenantId = :tenantId
        AND r.alertType = :alertType
        AND (r.severity = :severity OR r.severity IS NULL)
        AND r.active = :active
        AND (r.afterHoursOnly = true OR (r.businessHoursOnly = false AND r.afterHoursOnly = false))
        ORDER BY r.priority ASC
        """)
    List<AlertRoutingConfigEntity> findAfterHoursRoutingConfigs(
        @Param("tenantId") String tenantId,
        @Param("alertType") String alertType,
        @Param("severity") String severity,
        @Param("active") boolean active
    );

    /**
     * Find routing configurations with escalation thresholds
     *
     * @param tenantId Tenant ID
     * @param alertType Alert type
     * @param active Whether to include only active configs
     * @return List of routing configurations with escalation settings
     */
    @Query("""
        SELECT r FROM AlertRoutingConfigEntity r
        WHERE r.tenantId = :tenantId
        AND r.alertType = :alertType
        AND r.active = :active
        AND r.escalationThresholdMinutes IS NOT NULL
        ORDER BY r.escalationThresholdMinutes ASC, r.priority ASC
        """)
    List<AlertRoutingConfigEntity> findEscalationRoutingConfigs(
        @Param("tenantId") String tenantId,
        @Param("alertType") String alertType,
        @Param("active") boolean active
    );
}
