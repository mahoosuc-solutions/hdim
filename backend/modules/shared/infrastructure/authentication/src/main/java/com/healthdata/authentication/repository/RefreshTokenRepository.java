package com.healthdata.authentication.repository;

import com.healthdata.authentication.entity.RefreshToken;
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
 * Repository for managing refresh tokens.
 *
 * Provides CRUD operations and custom queries for refresh token management:
 * - Finding tokens by token value or user ID
 * - Deleting expired or revoked tokens
 * - Cleanup operations
 *
 * All delete operations should be transactional.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find a refresh token by its token value.
     *
     * @param token token value to search for
     * @return Optional containing the refresh token if found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Find a refresh token by token value that has not been revoked.
     * This is the most common query for token validation.
     *
     * @param token token value to search for
     * @return Optional containing the refresh token if found and not revoked
     */
    Optional<RefreshToken> findByTokenAndRevokedAtIsNull(String token);

    /**
     * Find all refresh tokens for a specific user.
     *
     * @param userId user ID
     * @return list of refresh tokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId")
    List<RefreshToken> findByUserId(@Param("userId") UUID userId);

    /**
     * Find all active (not revoked and not expired) refresh tokens for a user.
     *
     * @param userId user ID
     * @param now current timestamp for expiration check
     * @return list of active refresh tokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId " +
           "AND rt.revokedAt IS NULL AND rt.expiresAt > :now")
    List<RefreshToken> findActiveTokensByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Delete all refresh tokens for a specific user.
     * Used when user logs out from all devices or account is deleted.
     *
     * @param userId user ID
     * @return number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
    int deleteByUserId(@Param("userId") UUID userId);

    /**
     * Delete all expired refresh tokens.
     * Used by the cleanup scheduler to remove old tokens.
     *
     * @param date timestamp to compare against
     * @return number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :date")
    int deleteByExpiresAtBefore(@Param("date") Instant date);

    /**
     * Delete all revoked tokens older than a specific date.
     * Used by the cleanup scheduler to remove old revoked tokens.
     *
     * @param date timestamp to compare against
     * @return number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revokedAt IS NOT NULL AND rt.revokedAt < :date")
    int deleteRevokedTokensBefore(@Param("date") Instant date);

    /**
     * Count active refresh tokens for a user.
     *
     * @param userId user ID
     * @param now current timestamp for expiration check
     * @return number of active tokens
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.id = :userId " +
           "AND rt.revokedAt IS NULL AND rt.expiresAt > :now")
    long countActiveTokensByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Check if a token exists and is valid (not revoked and not expired).
     *
     * @param token token value
     * @param now current timestamp for expiration check
     * @return true if token is valid, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END FROM RefreshToken rt " +
           "WHERE rt.token = :token AND rt.revokedAt IS NULL AND rt.expiresAt > :now")
    boolean existsValidToken(@Param("token") String token, @Param("now") Instant now);

    /**
     * Revoke all active tokens for a user.
     *
     * @param userId user ID
     * @param revokedAt revocation timestamp
     * @return number of tokens revoked
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt " +
           "WHERE rt.user.id = :userId AND rt.revokedAt IS NULL")
    int revokeAllUserTokens(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);

    /**
     * Find all tokens that will expire soon.
     * Useful for notification or rotation purposes.
     *
     * @param start start of time window
     * @param end end of time window
     * @return list of tokens expiring in the given window
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.expiresAt BETWEEN :start AND :end " +
           "AND rt.revokedAt IS NULL")
    List<RefreshToken> findTokensExpiringSoon(@Param("start") Instant start, @Param("end") Instant end);
}
