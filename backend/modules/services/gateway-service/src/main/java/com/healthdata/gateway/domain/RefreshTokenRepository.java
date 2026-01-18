package com.healthdata.gateway.domain;

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
 * Repository for RefreshToken entity (Phase 2.0 Team 3.1)
 *
 * Provides query methods for refresh token management and validation
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find refresh token by JWT ID claim
     */
    Optional<RefreshToken> findByTokenJti(String tokenJti);

    /**
     * Find refresh token by token hash
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Find all non-revoked refresh tokens for user
     */
    @Query("""
        SELECT r FROM RefreshToken r
        WHERE r.userId = :userId
        AND r.tenantId = :tenantId
        AND r.revokedAt IS NULL
        ORDER BY r.createdAt DESC
        """)
    List<RefreshToken> findActiveTokensByUser(
        @Param("userId") String userId,
        @Param("tenantId") String tenantId);

    /**
     * Find all refresh tokens for user (including revoked)
     */
    @Query("""
        SELECT r FROM RefreshToken r
        WHERE r.userId = :userId
        AND r.tenantId = :tenantId
        ORDER BY r.createdAt DESC
        """)
    Page<RefreshToken> findByUserIdAndTenantId(
        @Param("userId") String userId,
        @Param("tenantId") String tenantId,
        Pageable pageable);

    /**
     * Count active (non-revoked, non-expired) tokens for user
     */
    @Query("""
        SELECT COUNT(r) FROM RefreshToken r
        WHERE r.userId = :userId
        AND r.tenantId = :tenantId
        AND r.revokedAt IS NULL
        AND r.expiresAt > CURRENT_TIMESTAMP
        """)
    long countActiveTokensByUser(
        @Param("userId") String userId,
        @Param("tenantId") String tenantId);

    /**
     * Find recently expired tokens for cleanup
     */
    @Query("""
        SELECT r FROM RefreshToken r
        WHERE r.expiresAt < :cutoffTime
        AND r.revokedAt IS NULL
        """)
    Page<RefreshToken> findExpiredTokens(
        @Param("cutoffTime") Instant cutoffTime,
        Pageable pageable);

    /**
     * Find revoked tokens within date range
     */
    @Query("""
        SELECT r FROM RefreshToken r
        WHERE r.userId = :userId
        AND r.tenantId = :tenantId
        AND r.revokedAt >= :startTime
        AND r.revokedAt <= :endTime
        ORDER BY r.revokedAt DESC
        """)
    Page<RefreshToken> findRevokedTokensByDateRange(
        @Param("userId") String userId,
        @Param("tenantId") String tenantId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        Pageable pageable);

    /**
     * Delete expired tokens (for cleanup)
     */
    @Query("""
        DELETE FROM RefreshToken r
        WHERE r.expiresAt < :cutoffTime
        """)
    void deleteExpiredTokens(@Param("cutoffTime") Instant cutoffTime);
}
