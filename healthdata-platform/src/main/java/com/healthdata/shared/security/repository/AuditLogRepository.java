package com.healthdata.shared.security.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Audit Log entities
 * Provides comprehensive access to audit trail and compliance logging
 * Supports complex queries, pagination, and temporal queries for forensic analysis
 * Critical for HIPAA, GDPR, and other regulatory compliance requirements
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    /**
     * Find audit logs by user ID with pagination
     * Used for tracking user actions and activity history
     *
     * @param userId User identifier
     * @param pageable Pagination information
     * @return Paginated list of audit logs for the user
     */
    @Query("""
        SELECT al FROM AuditLog al
        WHERE al.userId = :userId
        ORDER BY al.createdAt DESC
        """)
    Page<AuditLog> findByUserId(
        @Param("userId") String userId,
        Pageable pageable
    );

    /**
     * Find audit logs by entity type with pagination
     * Used for tracking changes to specific entity types
     *
     * @param entityType Type of entity (Patient, Observation, CareGap, etc.)
     * @param pageable Pagination information
     * @return Paginated list of audit logs for the entity type
     */
    @Query("""
        SELECT al FROM AuditLog al
        WHERE al.entityType = :entityType
        ORDER BY al.createdAt DESC
        """)
    Page<AuditLog> findByEntityType(
        @Param("entityType") String entityType,
        Pageable pageable
    );

    /**
     * Find audit logs within a date range with pagination
     * Used for compliance reporting and temporal audits
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @param pageable Pagination information
     * @return Paginated list of audit logs in the date range
     */
    @Query("""
        SELECT al FROM AuditLog al
        WHERE al.createdAt BETWEEN :startDate AND :endDate
        ORDER BY al.createdAt DESC
        """)
    Page<AuditLog> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    /**
     * Find audit logs for changes to a specific entity
     * Used for entity-level audit trails and forensic analysis
     *
     * @param entityType Type of entity
     * @param entityId Entity identifier
     * @param pageable Pagination information
     * @return Paginated list of all changes to the entity
     */
    @Query("""
        SELECT al FROM AuditLog al
        WHERE al.entityType = :entityType
        AND al.entityId = :entityId
        ORDER BY al.createdAt DESC
        """)
    Page<AuditLog> findChangesByEntity(
        @Param("entityType") String entityType,
        @Param("entityId") String entityId,
        Pageable pageable
    );

    /**
     * Find audit logs by action type
     * Action types include: CREATE, READ, UPDATE, DELETE, LOGIN, LOGOUT, EXPORT, etc.
     *
     * @param actionType Type of action performed
     * @return List of audit logs for the action type
     */
    @Query("""
        SELECT al FROM AuditLog al
        WHERE al.actionType = :actionType
        ORDER BY al.createdAt DESC
        """)
    List<AuditLog> findByActionType(@Param("actionType") String actionType);

    /**
     * Find audit logs by user and date range
     * Used for user activity forensics
     *
     * @param userId User identifier
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of audit logs for the user in the date range
     */
    @Query("""
        SELECT al FROM AuditLog al
        WHERE al.userId = :userId
        AND al.createdAt BETWEEN :startDate AND :endDate
        ORDER BY al.createdAt DESC
        """)
    List<AuditLog> findByUserIdAndDateRange(
        @Param("userId") String userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find sensitive action logs
     * Sensitive actions include: DELETE, EXPORT, MODIFY_SECURITY, etc.
     *
     * @param tenantId Tenant identifier
     * @return List of sensitive action logs
     */
    @Query("""
        SELECT al FROM AuditLog al
        WHERE al.tenantId = :tenantId
        AND al.actionType IN ('DELETE', 'EXPORT', 'MODIFY_SECURITY', 'BULK_MODIFY')
        ORDER BY al.createdAt DESC
        """)
    List<AuditLog> findSensitiveActions(@Param("tenantId") String tenantId);

    /**
     * Find failed access attempts
     * Used for security monitoring and breach detection
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of failed access attempts
     */
    @Query("""
        SELECT al FROM AuditLog al
        WHERE al.actionType = 'LOGIN'
        AND al.status = 'FAILED'
        AND al.createdAt BETWEEN :startDate AND :endDate
        ORDER BY al.createdAt DESC
        """)
    List<AuditLog> findFailedAccessAttempts(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find data access logs by patient ID
     * Critical for HIPAA audit trails
     *
     * @param patientId Patient identifier
     * @return List of all access events for the patient
     */
    @Query("""
        SELECT al FROM AuditLog al
        WHERE al.entityType = 'Patient'
        AND al.entityId = :patientId
        ORDER BY al.createdAt DESC
        """)
    List<AuditLog> findPatientAccessLogs(@Param("patientId") String patientId);

    /**
     * Find audit logs by user and entity type
     * Used for role-based activity analysis
     *
     * @param userId User identifier
     * @param entityType Type of entity
     * @return List of audit logs matching criteria
     */
    @Query("""
        SELECT al FROM AuditLog al
        WHERE al.userId = :userId
        AND al.entityType = :entityType
        ORDER BY al.createdAt DESC
        """)
    List<AuditLog> findByUserIdAndEntityType(
        @Param("userId") String userId,
        @Param("entityType") String entityType
    );

    /**
     * Count audit logs by user within date range
     * Used for activity metrics
     *
     * @param userId User identifier
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of audit logs
     */
    @Query("""
        SELECT COUNT(al) FROM AuditLog al
        WHERE al.userId = :userId
        AND al.createdAt BETWEEN :startDate AND :endDate
        """)
    long countByUserIdAndDateRange(
        @Param("userId") String userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find changes to a specific field within an entity
     * Used for detailed change tracking
     *
     * @param entityType Type of entity
     * @param entityId Entity identifier
     * @param fieldName Field that was changed
     * @return List of changes to the specific field
     */
    @Query("""
        SELECT al FROM AuditLog al
        WHERE al.entityType = :entityType
        AND al.entityId = :entityId
        AND LOWER(al.details) LIKE LOWER(CONCAT('%', :fieldName, '%'))
        ORDER BY al.createdAt DESC
        """)
    List<AuditLog> findFieldChanges(
        @Param("entityType") String entityType,
        @Param("entityId") String entityId,
        @Param("fieldName") String fieldName
    );

    /**
     * Find recent sensitive actions for compliance dashboard
     *
     * @param tenantId Tenant identifier
     * @param minutesAgo Minutes to look back
     * @return List of recent sensitive actions
     */
    @Query("""
        SELECT al FROM AuditLog al
        WHERE al.tenantId = :tenantId
        AND al.actionType IN ('DELETE', 'EXPORT', 'MODIFY_SECURITY')
        AND al.createdAt >= CURRENT_TIMESTAMP - INTERVAL :minutesAgo minute
        ORDER BY al.createdAt DESC
        """)
    List<AuditLog> findRecentSensitiveActions(
        @Param("tenantId") String tenantId,
        @Param("minutesAgo") int minutesAgo
    );

    /**
     * Find unusual access patterns
     * Multiple access attempts in short time period
     *
     * @param userId User identifier
     * @param secondsWindow Time window in seconds
     * @return List of rapid access attempts
     */
    @Query("""
        SELECT al FROM AuditLog al
        WHERE al.userId = :userId
        AND al.createdAt >= CURRENT_TIMESTAMP - INTERVAL :secondsWindow second
        ORDER BY al.createdAt DESC
        """)
    List<AuditLog> findRapidAccessAttempts(
        @Param("userId") String userId,
        @Param("secondsWindow") int secondsWindow
    );

    /**
     * Count audit logs by action type for a tenant
     * Used for audit metrics
     *
     * @param tenantId Tenant identifier
     * @param actionType Type of action
     * @return Count of audit logs
     */
    @Query("""
        SELECT COUNT(al) FROM AuditLog al
        WHERE al.tenantId = :tenantId
        AND al.actionType = :actionType
        """)
    long countByTenantAndActionType(
        @Param("tenantId") String tenantId,
        @Param("actionType") String actionType
    );

    /**
     * Find inactive users with recent activity
     * Anomaly detection for security
     *
     * @param daysSinceLastActivity Days since expected activity
     * @return List of audit logs for users with unusual patterns
     */
    @Query("""
        SELECT al FROM AuditLog al
        WHERE al.createdAt >= CURRENT_TIMESTAMP - INTERVAL :daysSinceLastActivity day
        ORDER BY al.userId, al.createdAt DESC
        """)
    List<AuditLog> findAnomalousActivity(@Param("daysSinceLastActivity") int daysSinceLastActivity);

    /**
     * Find deletion audit logs with details
     * Critical for data loss forensics
     *
     * @param entityType Type of deleted entity
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of deletion events with details
     */
    @Query("""
        SELECT al FROM AuditLog al
        WHERE al.actionType = 'DELETE'
        AND al.entityType = :entityType
        AND al.createdAt BETWEEN :startDate AND :endDate
        ORDER BY al.createdAt DESC
        """)
    List<AuditLog> findDeletionAudit(
        @Param("entityType") String entityType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count audit logs by tenant for capacity planning
     *
     * @param tenantId Tenant identifier
     * @return Total count of audit logs for the tenant
     */
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") String tenantId);

    /**
     * Find expired audit logs for archival
     * Retention policies may require archiving old logs
     *
     * @param retentionDays Number of days to retain logs
     * @return List of audit logs exceeding retention period
     */
    @Query("""
        SELECT al FROM AuditLog al
        WHERE al.createdAt < CURRENT_TIMESTAMP - INTERVAL :retentionDays day
        ORDER BY al.createdAt ASC
        """)
    List<AuditLog> findExpiredLogs(@Param("retentionDays") int retentionDays);
}

/**
 * Audit Log entity - represents system audit trail
 * Captures all significant user actions for compliance and forensics
 */
interface AuditLog {
    String getId();
    String getUserId();
    String getActionType();
    String getEntityType();
    String getEntityId();
    String getStatus();
    String getDetails();
    String getTenantId();
    LocalDateTime getCreatedAt();
}
