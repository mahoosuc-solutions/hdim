package com.healthdata.sales.repository;

import com.healthdata.sales.entity.LinkedInToken;
import com.healthdata.sales.entity.LinkedInToken.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LinkedIn OAuth Token storage
 */
@Repository
public interface LinkedInTokenRepository extends JpaRepository<LinkedInToken, UUID> {

    /**
     * Find token for a specific user in a tenant
     */
    Optional<LinkedInToken> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    /**
     * Find active token for a user
     */
    @Query("SELECT t FROM LinkedInToken t WHERE t.tenantId = :tenantId " +
           "AND t.userId = :userId AND t.status = 'ACTIVE'")
    Optional<LinkedInToken> findActiveToken(
        @Param("tenantId") UUID tenantId,
        @Param("userId") UUID userId
    );

    /**
     * Find all active tokens for a tenant
     */
    List<LinkedInToken> findByTenantIdAndStatus(UUID tenantId, TokenStatus status);

    /**
     * Find tokens expiring soon (for proactive refresh)
     */
    @Query("SELECT t FROM LinkedInToken t WHERE t.status = 'ACTIVE' " +
           "AND t.expiresAt < :threshold")
    List<LinkedInToken> findTokensExpiringSoon(@Param("threshold") Instant threshold);

    /**
     * Check if user has a connected LinkedIn account
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
           "FROM LinkedInToken t WHERE t.tenantId = :tenantId " +
           "AND t.userId = :userId AND t.status = 'ACTIVE'")
    boolean hasActiveToken(
        @Param("tenantId") UUID tenantId,
        @Param("userId") UUID userId
    );

    /**
     * Revoke all tokens for a user
     */
    @Modifying
    @Query("UPDATE LinkedInToken t SET t.status = 'REVOKED', " +
           "t.accessToken = NULL, t.refreshToken = NULL " +
           "WHERE t.tenantId = :tenantId AND t.userId = :userId")
    int revokeTokensForUser(
        @Param("tenantId") UUID tenantId,
        @Param("userId") UUID userId
    );

    /**
     * Delete expired tokens (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM LinkedInToken t WHERE t.status IN ('REVOKED', 'ERROR') " +
           "AND t.updatedAt < :before")
    int deleteOldRevokedTokens(@Param("before") Instant before);
}
