package com.healthdata.authentication.service;

import com.healthdata.authentication.entity.ApiKey;
import com.healthdata.authentication.repository.ApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ApiKeyService using TDD approach.
 *
 * Tests cover:
 * - API key creation and uniqueness
 * - API key validation (valid, invalid, expired, revoked)
 * - API key rotation
 * - API key revocation
 * - Scope management
 * - IP restriction validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyService Unit Tests")
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @InjectMocks
    private ApiKeyService apiKeyService;

    private static final String TENANT_ID = "tenant-123";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String KEY_NAME = "Test API Key";
    private static final String KEY_DESCRIPTION = "Test API Key Description";
    private static final Set<String> SCOPES = Set.of("fhir:read", "fhir:write");

    @BeforeEach
    void setUp() {
        // Default behavior: no existing keys
        lenient().when(apiKeyRepository.existsByKeyPrefix(anyString())).thenReturn(false);
    }

    // ===========================
    // CREATE API KEY TESTS
    // ===========================

    @Test
    @DisplayName("createApiKey should generate unique keys")
    void testCreateApiKey_GeneratesUniqueKeys() {
        // Given
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ApiKeyService.ApiKeyCreationResult result1 = apiKeyService.createApiKey(
            KEY_NAME, KEY_DESCRIPTION, TENANT_ID, USER_ID, SCOPES, null, null
        );
        ApiKeyService.ApiKeyCreationResult result2 = apiKeyService.createApiKey(
            KEY_NAME, KEY_DESCRIPTION, TENANT_ID, USER_ID, SCOPES, null, null
        );

        // Then
        assertThat(result1.rawKey()).isNotEqualTo(result2.rawKey());
        assertThat(result1.apiKey().getKeyHash()).isNotEqualTo(result2.apiKey().getKeyHash());
        assertThat(result1.apiKey().getKeyPrefix()).isNotEqualTo(result2.apiKey().getKeyPrefix());
        assertThat(result1.rawKey()).startsWith("hdim_");
        assertThat(result2.rawKey()).startsWith("hdim_");
    }

    @Test
    @DisplayName("createApiKey should hash keys correctly using SHA-256")
    void testCreateApiKey_HashesKeysCorrectly() {
        // Given
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ApiKeyService.ApiKeyCreationResult result = apiKeyService.createApiKey(
            KEY_NAME, KEY_DESCRIPTION, TENANT_ID, USER_ID, SCOPES, null, null
        );

        // Then
        assertThat(result.apiKey().getKeyHash())
            .isNotNull()
            .hasSize(64) // SHA-256 produces 64 hex characters
            .matches("[a-f0-9]{64}");

        // Key hash should not contain the raw key
        assertThat(result.apiKey().getKeyHash()).doesNotContain(result.rawKey());
    }

    @Test
    @DisplayName("createApiKey should set all fields correctly")
    void testCreateApiKey_SetsAllFieldsCorrectly() {
        // Given
        ArgumentCaptor<ApiKey> apiKeyCaptor = ArgumentCaptor.forClass(ApiKey.class);
        when(apiKeyRepository.save(apiKeyCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        Integer expiresInDays = 90;
        Integer rateLimitPerMinute = 100;

        // When
        ApiKeyService.ApiKeyCreationResult result = apiKeyService.createApiKey(
            KEY_NAME, KEY_DESCRIPTION, TENANT_ID, USER_ID, SCOPES, expiresInDays, rateLimitPerMinute
        );

        // Then
        ApiKey savedKey = apiKeyCaptor.getValue();
        assertThat(savedKey.getName()).isEqualTo(KEY_NAME);
        assertThat(savedKey.getDescription()).isEqualTo(KEY_DESCRIPTION);
        assertThat(savedKey.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(savedKey.getCreatedBy()).isEqualTo(USER_ID);
        assertThat(savedKey.getScopes()).containsExactlyInAnyOrderElementsOf(SCOPES);
        assertThat(savedKey.getActive()).isTrue();
        assertThat(savedKey.getUsageCount()).isEqualTo(0L);
        assertThat(savedKey.getRateLimitPerMinute()).isEqualTo(rateLimitPerMinute);
        assertThat(savedKey.getExpiresAt())
            .isNotNull()
            .isAfter(Instant.now())
            .isBefore(Instant.now().plus(expiresInDays + 1, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("createApiKey should handle key prefix collision by regenerating")
    void testCreateApiKey_HandlesKeyPrefixCollision() {
        // Given
        when(apiKeyRepository.existsByKeyPrefix(anyString()))
            .thenReturn(true)  // First attempt: collision
            .thenReturn(false); // Second attempt: no collision
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ApiKeyService.ApiKeyCreationResult result = apiKeyService.createApiKey(
            KEY_NAME, KEY_DESCRIPTION, TENANT_ID, USER_ID, SCOPES, null, null
        );

        // Then
        assertThat(result.rawKey()).isNotNull();
        verify(apiKeyRepository, atLeast(2)).existsByKeyPrefix(anyString());
    }

    @Test
    @DisplayName("createApiKey should set no expiration when expiresInDays is null")
    void testCreateApiKey_NoExpiration() {
        // Given
        ArgumentCaptor<ApiKey> apiKeyCaptor = ArgumentCaptor.forClass(ApiKey.class);
        when(apiKeyRepository.save(apiKeyCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ApiKeyService.ApiKeyCreationResult result = apiKeyService.createApiKey(
            KEY_NAME, KEY_DESCRIPTION, TENANT_ID, USER_ID, SCOPES, null, null
        );

        // Then
        assertThat(apiKeyCaptor.getValue().getExpiresAt()).isNull();
    }

    // ===========================
    // VALIDATE API KEY TESTS
    // ===========================

    @Test
    @DisplayName("validateApiKey should return key for valid key")
    void testValidateApiKey_ValidKey() {
        // Given
        String rawKey = "hdim_testkey12345";
        String ipAddress = "192.168.1.1";
        ApiKey validKey = createValidApiKey();

        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.of(validKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(validKey);

        // When
        Optional<ApiKey> result = apiKeyService.validateApiKey(rawKey, ipAddress);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(validKey);
        verify(apiKeyRepository).save(validKey);
    }

    @Test
    @DisplayName("validateApiKey should return empty for invalid key hash")
    void testValidateApiKey_InvalidKeyHash() {
        // Given
        String rawKey = "hdim_invalidkey";
        String ipAddress = "192.168.1.1";

        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.empty());

        // When
        Optional<ApiKey> result = apiKeyService.validateApiKey(rawKey, ipAddress);

        // Then
        assertThat(result).isEmpty();
        verify(apiKeyRepository, never()).save(any());
    }

    @Test
    @DisplayName("validateApiKey should return empty for null or blank key")
    void testValidateApiKey_NullOrBlankKey() {
        // When/Then
        assertThat(apiKeyService.validateApiKey(null, "192.168.1.1")).isEmpty();
        assertThat(apiKeyService.validateApiKey("", "192.168.1.1")).isEmpty();
        assertThat(apiKeyService.validateApiKey("   ", "192.168.1.1")).isEmpty();

        verify(apiKeyRepository, never()).findByKeyHash(anyString());
    }

    @Test
    @DisplayName("validateApiKey should return empty for expired key")
    void testValidateApiKey_ExpiredKey() {
        // Given
        String rawKey = "hdim_expiredkey";
        String ipAddress = "192.168.1.1";
        ApiKey expiredKey = createValidApiKey();
        expiredKey.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));

        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.of(expiredKey));

        // When
        Optional<ApiKey> result = apiKeyService.validateApiKey(rawKey, ipAddress);

        // Then
        assertThat(result).isEmpty();
        verify(apiKeyRepository, never()).save(any());
    }

    @Test
    @DisplayName("validateApiKey should return empty for revoked key")
    void testValidateApiKey_RevokedKey() {
        // Given
        String rawKey = "hdim_revokedkey";
        String ipAddress = "192.168.1.1";
        ApiKey revokedKey = createValidApiKey();
        revokedKey.revoke(USER_ID, "Test revocation");

        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.of(revokedKey));

        // When
        Optional<ApiKey> result = apiKeyService.validateApiKey(rawKey, ipAddress);

        // Then
        assertThat(result).isEmpty();
        verify(apiKeyRepository, never()).save(any());
    }

    @Test
    @DisplayName("validateApiKey should return empty when IP is not allowed")
    void testValidateApiKey_IpNotAllowed() {
        // Given
        String rawKey = "hdim_restrictedkey";
        String ipAddress = "192.168.1.100";
        ApiKey restrictedKey = createValidApiKey();
        restrictedKey.setAllowedIps(Set.of("10.0.0.1", "10.0.0.2"));

        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.of(restrictedKey));

        // When
        Optional<ApiKey> result = apiKeyService.validateApiKey(rawKey, ipAddress);

        // Then
        assertThat(result).isEmpty();
        verify(apiKeyRepository, never()).save(any());
    }

    @Test
    @DisplayName("validateApiKey should allow when IP is in allowed list")
    void testValidateApiKey_IpAllowed() {
        // Given
        String rawKey = "hdim_restrictedkey";
        String ipAddress = "10.0.0.1";
        ApiKey restrictedKey = createValidApiKey();
        restrictedKey.setAllowedIps(Set.of("10.0.0.1", "10.0.0.2"));

        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.of(restrictedKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(restrictedKey);

        // When
        Optional<ApiKey> result = apiKeyService.validateApiKey(rawKey, ipAddress);

        // Then
        assertThat(result).isPresent();
        verify(apiKeyRepository).save(restrictedKey);
    }

    @Test
    @DisplayName("validateApiKey should record usage on successful validation")
    void testValidateApiKey_RecordsUsage() {
        // Given
        String rawKey = "hdim_testkey";
        String ipAddress = "192.168.1.1";
        ApiKey validKey = createValidApiKey();
        Long initialUsageCount = validKey.getUsageCount();

        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.of(validKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(validKey);

        // When
        apiKeyService.validateApiKey(rawKey, ipAddress);

        // Then
        assertThat(validKey.getUsageCount()).isEqualTo(initialUsageCount + 1);
        assertThat(validKey.getLastUsedIp()).isEqualTo(ipAddress);
        assertThat(validKey.getLastUsedAt()).isNotNull();
    }

    // ===========================
    // ROTATE API KEY TESTS
    // ===========================

    @Test
    @DisplayName("rotateApiKey should create new key and revoke old key")
    void testRotateApiKey_CreatesNewAndRevokesOld() {
        // Given
        UUID oldKeyId = UUID.randomUUID();
        UUID revokedBy = UUID.randomUUID();
        ApiKey oldKey = createValidApiKey();
        oldKey.setId(oldKeyId);
        oldKey.setName("Original Key");
        oldKey.setAllowedIps(Set.of("10.0.0.1"));

        when(apiKeyRepository.findById(oldKeyId)).thenReturn(Optional.of(oldKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> {
            ApiKey key = invocation.getArgument(0);
            if (key.getId() == null) {
                key.setId(UUID.randomUUID());
            }
            return key;
        });

        // When
        ApiKeyService.ApiKeyCreationResult result = apiKeyService.rotateApiKey(oldKeyId, revokedBy);

        // Then
        assertThat(result.rawKey()).isNotNull().startsWith("hdim_");
        assertThat(result.apiKey().getName()).contains("rotated");
        assertThat(result.apiKey().getTenantId()).isEqualTo(oldKey.getTenantId());
        assertThat(result.apiKey().getScopes()).isEqualTo(oldKey.getScopes());
        assertThat(result.apiKey().getRateLimitPerMinute()).isEqualTo(oldKey.getRateLimitPerMinute());
        assertThat(result.apiKey().getAllowedIps()).containsExactlyElementsOf(oldKey.getAllowedIps());

        // Old key should be revoked
        assertThat(oldKey.getActive()).isFalse();
        assertThat(oldKey.getRevokedAt()).isNotNull();
        assertThat(oldKey.getRevokedBy()).isEqualTo(revokedBy);
        assertThat(oldKey.getRevocationReason()).contains("Rotated to new key");

        verify(apiKeyRepository, atLeast(2)).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("rotateApiKey should throw exception for non-existent key")
    void testRotateApiKey_NonExistentKey() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(apiKeyRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> apiKeyService.rotateApiKey(nonExistentId, USER_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("API key not found");
    }

    // ===========================
    // REVOKE API KEY TESTS
    // ===========================

    @Test
    @DisplayName("revokeApiKey should set revocation fields correctly")
    void testRevokeApiKey_SetsRevocationFields() {
        // Given
        UUID keyId = UUID.randomUUID();
        UUID revokedBy = UUID.randomUUID();
        String reason = "Security breach";
        ApiKey apiKey = createValidApiKey();
        apiKey.setId(keyId);

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(apiKey);

        // When
        apiKeyService.revokeApiKey(keyId, revokedBy, reason);

        // Then
        assertThat(apiKey.getActive()).isFalse();
        assertThat(apiKey.getRevokedAt()).isNotNull().isBeforeOrEqualTo(Instant.now());
        assertThat(apiKey.getRevokedBy()).isEqualTo(revokedBy);
        assertThat(apiKey.getRevocationReason()).isEqualTo(reason);

        verify(apiKeyRepository).save(apiKey);
    }

    @Test
    @DisplayName("revokeApiKey should throw exception for non-existent key")
    void testRevokeApiKey_NonExistentKey() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(apiKeyRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> apiKeyService.revokeApiKey(nonExistentId, USER_ID, "Test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("API key not found");
    }

    // ===========================
    // UPDATE SCOPES TESTS
    // ===========================

    @Test
    @DisplayName("updateScopes should update scopes correctly")
    void testUpdateScopes_UpdatesCorrectly() {
        // Given
        UUID keyId = UUID.randomUUID();
        ApiKey apiKey = createValidApiKey();
        apiKey.setId(keyId);
        Set<String> originalScopes = new HashSet<>(apiKey.getScopes());
        Set<String> newScopes = Set.of("fhir:read", "measures:read", "patients:write");

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(apiKey);

        // When
        apiKeyService.updateScopes(keyId, newScopes);

        // Then
        assertThat(apiKey.getScopes()).containsExactlyInAnyOrderElementsOf(newScopes);
        assertThat(apiKey.getScopes()).isNotEqualTo(originalScopes);

        verify(apiKeyRepository).save(apiKey);
    }

    @Test
    @DisplayName("updateScopes should throw exception for non-existent key")
    void testUpdateScopes_NonExistentKey() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(apiKeyRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> apiKeyService.updateScopes(nonExistentId, Set.of("fhir:read")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("API key not found");
    }

    // ===========================
    // IP RESTRICTION VALIDATION TESTS
    // ===========================

    @Test
    @DisplayName("validateApiKey should allow all IPs when no restrictions set")
    void testIpRestriction_NoRestrictions() {
        // Given
        String rawKey = "hdim_testkey";
        ApiKey apiKey = createValidApiKey();
        apiKey.setAllowedIps(new HashSet<>()); // Empty set

        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.of(apiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(apiKey);

        // When
        Optional<ApiKey> result1 = apiKeyService.validateApiKey(rawKey, "192.168.1.1");
        Optional<ApiKey> result2 = apiKeyService.validateApiKey(rawKey, "10.0.0.1");
        Optional<ApiKey> result3 = apiKeyService.validateApiKey(rawKey, "172.16.0.1");

        // Then
        assertThat(result1).isPresent();
        assertThat(result2).isPresent();
        assertThat(result3).isPresent();
    }

    @Test
    @DisplayName("validateApiKey should enforce IP restrictions when set")
    void testIpRestriction_EnforcesRestrictions() {
        // Given
        String rawKey = "hdim_testkey";
        ApiKey apiKey = createValidApiKey();
        apiKey.setAllowedIps(Set.of("192.168.1.1", "10.0.0.5"));

        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.of(apiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(apiKey);

        // When
        Optional<ApiKey> allowedResult = apiKeyService.validateApiKey(rawKey, "192.168.1.1");
        Optional<ApiKey> deniedResult = apiKeyService.validateApiKey(rawKey, "192.168.1.2");

        // Then
        assertThat(allowedResult).isPresent();
        assertThat(deniedResult).isEmpty();
    }

    // ===========================
    // HELPER METHODS
    // ===========================

    private ApiKey createValidApiKey() {
        return ApiKey.builder()
            .id(UUID.randomUUID())
            .name(KEY_NAME)
            .description(KEY_DESCRIPTION)
            .keyHash("a".repeat(64))
            .keyPrefix("hdim_testpre")
            .tenantId(TENANT_ID)
            .createdBy(USER_ID)
            .scopes(new HashSet<>(SCOPES))
            .active(true)
            .expiresAt(null)
            .usageCount(0L)
            .rateLimitPerMinute(100)
            .allowedIps(new HashSet<>())
            .build();
    }
}
