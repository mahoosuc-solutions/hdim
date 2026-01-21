package com.healthdata.gateway.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for AuditLog entity (Phase 2.0 Team 2)
 *
 * Provides query methods for audit trail access and compliance reporting
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find audit logs by user ID
     */
    Page<AuditLog> findByUserIdOrderByTimestampDesc(
        String userId, Pageable pageable);

    /**
     * Find audit logs by tenant ID
     */
    Page<AuditLog> findByTenantIdOrderByTimestampDesc(
        String tenantId, Pageable pageable);

    /**
     * Find audit logs by HTTP status code (for error tracking)
     */
    Page<AuditLog> findByHttpStatusCodeOrderByTimestampDesc(
        Integer httpStatusCode, Pageable pageable);

    /**
     * Find failed authorization attempts
     */
    Page<AuditLog> findByAuthorizationAllowedFalseOrderByTimestampDesc(
        Pageable pageable);

    /**
     * Find all logs for specific user and tenant
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.userId = :userId AND a.tenantId = :tenantId
        ORDER BY a.timestamp DESC
        """)
    Page<AuditLog> findByUserAndTenant(
        @Param("userId") String userId,
        @Param("tenantId") String tenantId,
        Pageable pageable);

    /**
     * Find logs within date range
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.timestamp >= :startTime AND a.timestamp <= :endTime
        ORDER BY a.timestamp DESC
        """)
    Page<AuditLog> findByDateRange(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        Pageable pageable);

    /**
     * Find logs by trace ID (for distributed tracing)
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.traceId = :traceId
        ORDER BY a.timestamp DESC
        """)
    List<AuditLog> findByTraceId(@Param("traceId") String traceId);

    /**
     * Find failed login attempts (brute force detection)
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.requestPath = '/api/v1/auth/login'
        AND a.httpStatusCode = 401
        AND a.timestamp >= :since
        ORDER BY a.timestamp DESC
        """)
    Page<AuditLog> findFailedLoginAttempts(
        @Param("since") Instant since,
        Pageable pageable);

    /**
     * Find failed login attempts for specific IP (brute force detection)
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.requestPath = '/api/v1/auth/login'
        AND a.httpStatusCode = 401
        AND a.clientIp = :clientIp
        AND a.timestamp >= :since
        ORDER BY a.timestamp DESC
        """)
    Page<AuditLog> findFailedLoginAttemptsFromIp(
        @Param("clientIp") String clientIp,
        @Param("since") Instant since,
        Pageable pageable);

    /**
     * Find rate limit exceeded events
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.httpStatusCode = 429
        AND a.timestamp >= :since
        ORDER BY a.timestamp DESC
        """)
    Page<AuditLog> findRateLimitEvents(
        @Param("since") Instant since,
        Pageable pageable);

    /**
     * Find unauthorized access attempts (403)
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.httpStatusCode = 403
        AND a.timestamp >= :since
        ORDER BY a.timestamp DESC
        """)
    Page<AuditLog> findUnauthorizedAttempts(
        @Param("since") Instant since,
        Pageable pageable);

    /**
     * Find most recent log entry
     */
    AuditLog findFirstByOrderByTimestampDesc();

    /**
     * Count logs by tenant for audit reporting
     */
    Long countByTenantId(String tenantId);

    /**
     * Count logs by tenant and date range
     */
    @Query("""
        SELECT COUNT(a) FROM AuditLog a
        WHERE a.tenantId = :tenantId
        AND a.timestamp >= :startTime
        AND a.timestamp <= :endTime
        """)
    Long countByTenantAndDateRange(
        @Param("tenantId") String tenantId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
}
