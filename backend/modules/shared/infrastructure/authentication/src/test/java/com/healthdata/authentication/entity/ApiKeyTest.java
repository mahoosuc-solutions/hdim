package com.healthdata.authentication.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for ApiKey entity using TDD approach.
 *
 * Tests cover:
 * - isValid() method for various states (active, revoked, expired)
 * - hasScope() method with exact and wildcard matching
 * - recordUsage() method to update usage fields
 */
@DisplayName("ApiKey Entity Unit Tests")
class ApiKeyTest {

    private static final String TENANT_ID = "tenant-123";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String KEY_NAME = "Test API Key";
    private static final String KEY_DESCRIPTION = "Test Description";
    private static final String KEY_HASH = "a".repeat(64);
    private static final String KEY_PREFIX = "hdim_testkey";

    private ApiKey apiKey;

    @BeforeEach
    void setUp() {
        apiKey = createDefaultApiKey();
    }

    // ===========================
    // isValid() TESTS
    // ===========================

    @Test
    @DisplayName("isValid should return true for active, non-expired key")
    void testIsValid_ActiveNonExpiredKey() {
        // Given - default key is active and non-expired

        // When
        boolean result = apiKey.isValid();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isValid should return true for active key with future expiration")
    void testIsValid_ActiveKeyWithFutureExpiration() {
        // Given
        apiKey.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));

        // When
        boolean result = apiKey.isValid();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isValid should return false for revoked key")
    void testIsValid_RevokedKey() {
        // Given
        apiKey.revoke(USER_ID, "Test revocation");

        // When
        boolean result = apiKey.isValid();

        // Then
        assertThat(result).isFalse();
        assertThat(apiKey.getActive()).isFalse();
        assertThat(apiKey.getRevokedAt()).isNotNull();
        assertThat(apiKey.getRevokedBy()).isEqualTo(USER_ID);
        assertThat(apiKey.getRevocationReason()).isEqualTo("Test revocation");
    }

    @Test
    @DisplayName("isValid should return false for expired key")
    void testIsValid_ExpiredKey() {
        // Given
        apiKey.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));

        // When
        boolean result = apiKey.isValid();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValid should return false for inactive key")
    void testIsValid_InactiveKey() {
        // Given
        apiKey.setActive(false);

        // When
        boolean result = apiKey.isValid();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValid should return false when revokedAt is set even if active is true")
    void testIsValid_RevokedAtSetButActiveTrue() {
        // Given
        apiKey.setActive(true);
        apiKey.setRevokedAt(Instant.now());

        // When
        boolean result = apiKey.isValid();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValid should return false for key expiring exactly now")
    void testIsValid_ExpiringNow() {
        // Given - set expiration to a very recent past moment (within milliseconds)
        apiKey.setExpiresAt(Instant.now().minusMillis(1));

        // When
        boolean result = apiKey.isValid();

        // Then
        assertThat(result).isFalse();
    }

    // ===========================
    // hasScope() TESTS
    // ===========================

    @Test
    @DisplayName("hasScope should return true for exact scope match")
    void testHasScope_ExactMatch() {
        // Given
        apiKey.setScopes(Set.of("fhir:read", "fhir:write", "patients:read"));

        // When/Then
        assertThat(apiKey.hasScope("fhir:read")).isTrue();
        assertThat(apiKey.hasScope("fhir:write")).isTrue();
        assertThat(apiKey.hasScope("patients:read")).isTrue();
    }

    @Test
    @DisplayName("hasScope should return false for non-existent scope")
    void testHasScope_NoMatch() {
        // Given
        apiKey.setScopes(Set.of("fhir:read", "fhir:write"));

        // When/Then
        assertThat(apiKey.hasScope("patients:read")).isFalse();
        assertThat(apiKey.hasScope("measures:write")).isFalse();
    }

    @Test
    @DisplayName("hasScope should match wildcard (fhir:* matches fhir:read)")
    void testHasScope_WildcardMatch() {
        // Given
        apiKey.setScopes(Set.of("fhir:*"));

        // When/Then
        assertThat(apiKey.hasScope("fhir:read")).isTrue();
        assertThat(apiKey.hasScope("fhir:write")).isTrue();
        assertThat(apiKey.hasScope("fhir:delete")).isTrue();
        assertThat(apiKey.hasScope("fhir:anything")).isTrue();
    }

    @Test
    @DisplayName("hasScope should not match wildcard for different resource")
    void testHasScope_WildcardNoMatchDifferentResource() {
        // Given
        apiKey.setScopes(Set.of("fhir:*"));

        // When/Then
        assertThat(apiKey.hasScope("patients:read")).isFalse();
        assertThat(apiKey.hasScope("measures:write")).isFalse();
    }

    @Test
    @DisplayName("hasScope should match multiple wildcards")
    void testHasScope_MultipleWildcards() {
        // Given
        apiKey.setScopes(Set.of("fhir:*", "patients:*", "measures:read"));

        // When/Then
        assertThat(apiKey.hasScope("fhir:read")).isTrue();
        assertThat(apiKey.hasScope("fhir:write")).isTrue();
        assertThat(apiKey.hasScope("patients:read")).isTrue();
        assertThat(apiKey.hasScope("patients:write")).isTrue();
        assertThat(apiKey.hasScope("measures:read")).isTrue();
        assertThat(apiKey.hasScope("measures:write")).isFalse();
    }

    @Test
    @DisplayName("hasScope should match global wildcard")
    void testHasScope_GlobalWildcard() {
        // Given
        apiKey.setScopes(Set.of("*"));

        // When/Then
        assertThat(apiKey.hasScope("fhir:read")).isTrue();
        assertThat(apiKey.hasScope("patients:write")).isTrue();
        assertThat(apiKey.hasScope("measures:delete")).isTrue();
        assertThat(apiKey.hasScope("anything:anything")).isTrue();
    }

    @Test
    @DisplayName("hasScope should return false for empty scopes")
    void testHasScope_EmptyScopes() {
        // Given
        apiKey.setScopes(new HashSet<>());

        // When/Then
        assertThat(apiKey.hasScope("fhir:read")).isFalse();
    }

    @Test
    @DisplayName("hasScope should return false for null scopes")
    void testHasScope_NullScopes() {
        // Given
        apiKey.setScopes(null);

        // When/Then
        assertThat(apiKey.hasScope("fhir:read")).isFalse();
    }

    @Test
    @DisplayName("hasScope should prefer exact match over wildcard")
    void testHasScope_ExactMatchTakesPrecedence() {
        // Given
        apiKey.setScopes(Set.of("fhir:read", "fhir:*"));

        // When/Then - both should work
        assertThat(apiKey.hasScope("fhir:read")).isTrue();
        assertThat(apiKey.hasScope("fhir:write")).isTrue();
    }

    @Test
    @DisplayName("hasScope should handle scope without colon")
    void testHasScope_ScopeWithoutColon() {
        // Given
        apiKey.setScopes(Set.of("admin", "read"));

        // When/Then
        assertThat(apiKey.hasScope("admin")).isTrue();
        assertThat(apiKey.hasScope("read")).isTrue();
    }

    // ===========================
    // hasAnyScope() TESTS
    // ===========================

    @Test
    @DisplayName("hasAnyScope should return true if any scope matches")
    void testHasAnyScope_OneMatches() {
        // Given
        apiKey.setScopes(Set.of("fhir:read", "patients:write"));

        // When/Then
        assertThat(apiKey.hasAnyScope("fhir:read", "measures:read")).isTrue();
        assertThat(apiKey.hasAnyScope("admin:write", "fhir:read")).isTrue();
    }

    @Test
    @DisplayName("hasAnyScope should return false if no scopes match")
    void testHasAnyScope_NoneMatch() {
        // Given
        apiKey.setScopes(Set.of("fhir:read", "patients:write"));

        // When/Then
        assertThat(apiKey.hasAnyScope("measures:read", "admin:write")).isFalse();
    }

    @Test
    @DisplayName("hasAnyScope should work with wildcards")
    void testHasAnyScope_WithWildcards() {
        // Given
        apiKey.setScopes(Set.of("fhir:*"));

        // When/Then
        assertThat(apiKey.hasAnyScope("fhir:read", "fhir:write", "patients:read")).isTrue();
    }

    // ===========================
    // recordUsage() TESTS
    // ===========================

    @Test
    @DisplayName("recordUsage should update lastUsedAt, lastUsedIp, and increment usageCount")
    void testRecordUsage_UpdatesAllFields() {
        // Given
        String ipAddress = "192.168.1.100";
        Instant beforeUsage = Instant.now();
        Long initialCount = apiKey.getUsageCount();

        // When
        apiKey.recordUsage(ipAddress);

        // Then
        assertThat(apiKey.getLastUsedAt())
            .isNotNull()
            .isAfterOrEqualTo(beforeUsage);
        assertThat(apiKey.getLastUsedIp()).isEqualTo(ipAddress);
        assertThat(apiKey.getUsageCount()).isEqualTo(initialCount + 1);
    }

    @Test
    @DisplayName("recordUsage should increment usageCount multiple times")
    void testRecordUsage_MultipleIncrements() {
        // Given
        Long initialCount = apiKey.getUsageCount();

        // When
        apiKey.recordUsage("192.168.1.1");
        apiKey.recordUsage("192.168.1.2");
        apiKey.recordUsage("192.168.1.3");

        // Then
        assertThat(apiKey.getUsageCount()).isEqualTo(initialCount + 3);
        assertThat(apiKey.getLastUsedIp()).isEqualTo("192.168.1.3");
    }

    @Test
    @DisplayName("recordUsage should handle null initial usageCount")
    void testRecordUsage_NullInitialCount() {
        // Given
        apiKey.setUsageCount(null);

        // When
        apiKey.recordUsage("192.168.1.1");

        // Then
        assertThat(apiKey.getUsageCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("recordUsage should update timestamp on each call")
    void testRecordUsage_UpdatesTimestamp() throws InterruptedException {
        // Given
        apiKey.recordUsage("192.168.1.1");
        Instant firstUsage = apiKey.getLastUsedAt();

        // Small delay to ensure timestamp difference
        Thread.sleep(10);

        // When
        apiKey.recordUsage("192.168.1.2");

        // Then
        assertThat(apiKey.getLastUsedAt())
            .isNotNull()
            .isAfter(firstUsage);
    }

    // ===========================
    // revoke() TESTS
    // ===========================

    @Test
    @DisplayName("revoke should set all revocation fields correctly")
    void testRevoke_SetsAllFields() {
        // Given
        UUID revokedByUser = UUID.randomUUID();
        String reason = "Security breach detected";
        Instant beforeRevocation = Instant.now();

        // When
        apiKey.revoke(revokedByUser, reason);

        // Then
        assertThat(apiKey.getActive()).isFalse();
        assertThat(apiKey.getRevokedAt())
            .isNotNull()
            .isAfterOrEqualTo(beforeRevocation);
        assertThat(apiKey.getRevokedBy()).isEqualTo(revokedByUser);
        assertThat(apiKey.getRevocationReason()).isEqualTo(reason);
    }

    @Test
    @DisplayName("revoke should work with null reason")
    void testRevoke_NullReason() {
        // Given
        UUID revokedByUser = UUID.randomUUID();

        // When
        apiKey.revoke(revokedByUser, null);

        // Then
        assertThat(apiKey.getActive()).isFalse();
        assertThat(apiKey.getRevokedAt()).isNotNull();
        assertThat(apiKey.getRevokedBy()).isEqualTo(revokedByUser);
        assertThat(apiKey.getRevocationReason()).isNull();
    }

    // ===========================
    // BUILDER TESTS
    // ===========================

    @Test
    @DisplayName("Builder should create ApiKey with all fields")
    void testBuilder_CreatesCompleteObject() {
        // Given
        UUID id = UUID.randomUUID();
        Set<String> scopes = Set.of("fhir:read", "fhir:write");
        Set<String> allowedIps = Set.of("10.0.0.1", "10.0.0.2");
        Instant expiresAt = Instant.now().plus(90, ChronoUnit.DAYS);

        // When
        ApiKey key = ApiKey.builder()
            .id(id)
            .name(KEY_NAME)
            .description(KEY_DESCRIPTION)
            .keyHash(KEY_HASH)
            .keyPrefix(KEY_PREFIX)
            .tenantId(TENANT_ID)
            .createdBy(USER_ID)
            .scopes(scopes)
            .active(true)
            .expiresAt(expiresAt)
            .usageCount(0L)
            .rateLimitPerMinute(100)
            .allowedIps(allowedIps)
            .build();

        // Then
        assertThat(key.getId()).isEqualTo(id);
        assertThat(key.getName()).isEqualTo(KEY_NAME);
        assertThat(key.getDescription()).isEqualTo(KEY_DESCRIPTION);
        assertThat(key.getKeyHash()).isEqualTo(KEY_HASH);
        assertThat(key.getKeyPrefix()).isEqualTo(KEY_PREFIX);
        assertThat(key.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(key.getCreatedBy()).isEqualTo(USER_ID);
        assertThat(key.getScopes()).isEqualTo(scopes);
        assertThat(key.getActive()).isTrue();
        assertThat(key.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(key.getUsageCount()).isEqualTo(0L);
        assertThat(key.getRateLimitPerMinute()).isEqualTo(100);
        assertThat(key.getAllowedIps()).isEqualTo(allowedIps);
    }

    @Test
    @DisplayName("Builder should use default values for scopes, active, usageCount, and allowedIps")
    void testBuilder_UsesDefaults() {
        // When
        ApiKey key = ApiKey.builder()
            .name(KEY_NAME)
            .keyHash(KEY_HASH)
            .keyPrefix(KEY_PREFIX)
            .tenantId(TENANT_ID)
            .createdBy(USER_ID)
            .build();

        // Then
        assertThat(key.getScopes()).isNotNull().isEmpty();
        assertThat(key.getActive()).isTrue();
        assertThat(key.getUsageCount()).isEqualTo(0L);
        assertThat(key.getAllowedIps()).isNotNull().isEmpty();
    }

    // ===========================
    // HELPER METHODS
    // ===========================

    private ApiKey createDefaultApiKey() {
        return ApiKey.builder()
            .id(UUID.randomUUID())
            .name(KEY_NAME)
            .description(KEY_DESCRIPTION)
            .keyHash(KEY_HASH)
            .keyPrefix(KEY_PREFIX)
            .tenantId(TENANT_ID)
            .createdBy(USER_ID)
            .scopes(new HashSet<>(Set.of("fhir:read", "fhir:write")))
            .active(true)
            .expiresAt(null) // No expiration
            .usageCount(0L)
            .rateLimitPerMinute(100)
            .allowedIps(new HashSet<>())
            .build();
    }
}
