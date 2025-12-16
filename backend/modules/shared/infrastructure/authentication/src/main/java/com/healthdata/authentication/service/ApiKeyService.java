package com.healthdata.authentication.service;

import com.healthdata.authentication.entity.ApiKey;
import com.healthdata.authentication.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service for API Key management.
 *
 * Handles:
 * - API key creation and validation
 * - API key rotation
 * - Rate limiting
 * - Usage tracking
 * - Automatic expiration cleanup
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String KEY_PREFIX = "hdim_";
    private static final int KEY_LENGTH = 32; // 256 bits

    /**
     * Result of API key creation containing both the raw key and the entity.
     */
    public record ApiKeyCreationResult(String rawKey, ApiKey apiKey) {}

    /**
     * Create a new API key.
     *
     * @param name Human-readable name
     * @param description Description of purpose
     * @param tenantId Tenant ID
     * @param createdBy User ID creating the key
     * @param scopes Permissions for the key
     * @param expiresInDays Days until expiration (null for no expiration)
     * @param rateLimitPerMinute Rate limit (null for default)
     * @return Result containing raw key (shown once) and the entity
     */
    @Transactional
    public ApiKeyCreationResult createApiKey(
        String name,
        String description,
        String tenantId,
        UUID createdBy,
        Set<String> scopes,
        Integer expiresInDays,
        Integer rateLimitPerMinute
    ) {
        log.info("Creating API key '{}' for tenant: {}", name, tenantId);

        // Generate raw key
        String rawKey = generateRawKey();
        String keyHash = hashKey(rawKey);
        String keyPrefix = rawKey.substring(0, 12); // First 12 chars for identification

        // Ensure prefix is unique
        while (apiKeyRepository.existsByKeyPrefix(keyPrefix)) {
            rawKey = generateRawKey();
            keyHash = hashKey(rawKey);
            keyPrefix = rawKey.substring(0, 12);
        }

        // Calculate expiration
        Instant expiresAt = expiresInDays != null
            ? Instant.now().plus(expiresInDays, ChronoUnit.DAYS)
            : null;

        // Create entity
        ApiKey apiKey = ApiKey.builder()
            .name(name)
            .description(description)
            .keyHash(keyHash)
            .keyPrefix(keyPrefix)
            .tenantId(tenantId)
            .createdBy(createdBy)
            .scopes(scopes != null ? new HashSet<>(scopes) : new HashSet<>())
            .active(true)
            .expiresAt(expiresAt)
            .rateLimitPerMinute(rateLimitPerMinute)
            .usageCount(0L)
            .build();

        apiKey = apiKeyRepository.save(apiKey);

        log.info("Created API key '{}' with prefix {} for tenant {}", name, keyPrefix, tenantId);

        return new ApiKeyCreationResult(rawKey, apiKey);
    }

    /**
     * Validate an API key and return the entity if valid.
     *
     * @param rawKey Raw API key
     * @param ipAddress IP address making the request
     * @return Optional containing the API key if valid
     */
    @Transactional
    public Optional<ApiKey> validateApiKey(String rawKey, String ipAddress) {
        if (rawKey == null || rawKey.isBlank()) {
            return Optional.empty();
        }

        String keyHash = hashKey(rawKey);
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyHash(keyHash);

        if (apiKeyOpt.isEmpty()) {
            log.debug("API key not found");
            return Optional.empty();
        }

        ApiKey apiKey = apiKeyOpt.get();

        // Check if key is valid
        if (!apiKey.isValid()) {
            log.warn("API key '{}' is invalid (revoked or expired)", apiKey.getKeyPrefix());
            return Optional.empty();
        }

        // Check IP restrictions
        if (!isIpAllowed(apiKey, ipAddress)) {
            log.warn("API key '{}' used from unauthorized IP: {}", apiKey.getKeyPrefix(), ipAddress);
            return Optional.empty();
        }

        // Record usage
        apiKey.recordUsage(ipAddress);
        apiKeyRepository.save(apiKey);

        log.debug("API key '{}' validated successfully", apiKey.getKeyPrefix());
        return Optional.of(apiKey);
    }

    /**
     * Get API key by ID.
     *
     * @param id API key ID
     * @return Optional containing the API key
     */
    public Optional<ApiKey> getApiKey(UUID id) {
        return apiKeyRepository.findById(id);
    }

    /**
     * Get all API keys for a tenant.
     *
     * @param tenantId Tenant ID
     * @return List of API keys
     */
    public List<ApiKey> getApiKeysForTenant(String tenantId) {
        return apiKeyRepository.findByTenantId(tenantId);
    }

    /**
     * Get active API keys for a tenant.
     *
     * @param tenantId Tenant ID
     * @return List of active API keys
     */
    public List<ApiKey> getActiveApiKeysForTenant(String tenantId) {
        return apiKeyRepository.findByTenantIdAndActiveTrue(tenantId);
    }

    /**
     * Rotate an API key (create new key, revoke old one).
     *
     * @param id API key ID to rotate
     * @param revokedBy User performing the rotation
     * @return New API key creation result
     */
    @Transactional
    public ApiKeyCreationResult rotateApiKey(UUID id, UUID revokedBy) {
        log.info("Rotating API key: {}", id);

        ApiKey oldKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("API key not found: " + id));

        // Create new key with same settings
        ApiKeyCreationResult result = createApiKey(
            oldKey.getName() + " (rotated)",
            oldKey.getDescription(),
            oldKey.getTenantId(),
            revokedBy,
            oldKey.getScopes(),
            null, // No expiration for rotated keys by default
            oldKey.getRateLimitPerMinute()
        );

        // Copy allowed IPs
        result.apiKey().setAllowedIps(new HashSet<>(oldKey.getAllowedIps()));
        apiKeyRepository.save(result.apiKey());

        // Revoke old key
        oldKey.revoke(revokedBy, "Rotated to new key: " + result.apiKey().getId());
        apiKeyRepository.save(oldKey);

        log.info("API key {} rotated to {}", id, result.apiKey().getId());

        return result;
    }

    /**
     * Revoke an API key.
     *
     * @param id API key ID
     * @param revokedBy User revoking the key
     * @param reason Reason for revocation
     */
    @Transactional
    public void revokeApiKey(UUID id, UUID revokedBy, String reason) {
        log.info("Revoking API key: {} - reason: {}", id, reason);

        ApiKey apiKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("API key not found: " + id));

        apiKey.revoke(revokedBy, reason);
        apiKeyRepository.save(apiKey);

        log.info("API key {} revoked", id);
    }

    /**
     * Update API key scopes.
     *
     * @param id API key ID
     * @param scopes New scopes
     */
    @Transactional
    public void updateScopes(UUID id, Set<String> scopes) {
        log.info("Updating scopes for API key: {}", id);

        ApiKey apiKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("API key not found: " + id));

        apiKey.setScopes(new HashSet<>(scopes));
        apiKeyRepository.save(apiKey);
    }

    /**
     * Update API key allowed IPs.
     *
     * @param id API key ID
     * @param allowedIps Allowed IP addresses (CIDR notation)
     */
    @Transactional
    public void updateAllowedIps(UUID id, Set<String> allowedIps) {
        log.info("Updating allowed IPs for API key: {}", id);

        ApiKey apiKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("API key not found: " + id));

        apiKey.setAllowedIps(new HashSet<>(allowedIps));
        apiKeyRepository.save(apiKey);
    }

    /**
     * Cleanup expired API keys (scheduled task).
     * Runs daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredKeys() {
        log.info("Running API key expiration cleanup");

        List<ApiKey> expiredKeys = apiKeyRepository.findExpiredKeys(Instant.now());

        for (ApiKey key : expiredKeys) {
            key.setActive(false);
            log.info("Deactivated expired API key: {} ({})", key.getKeyPrefix(), key.getName());
        }

        if (!expiredKeys.isEmpty()) {
            apiKeyRepository.saveAll(expiredKeys);
            log.info("Deactivated {} expired API keys", expiredKeys.size());
        }
    }

    /**
     * Generate a cryptographically secure raw API key.
     */
    private String generateRawKey() {
        byte[] bytes = new byte[KEY_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hash an API key using SHA-256.
     */
    private String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Convert bytes to hex string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Check if an IP address is allowed for the API key.
     */
    private boolean isIpAllowed(ApiKey apiKey, String ipAddress) {
        Set<String> allowedIps = apiKey.getAllowedIps();

        // If no restrictions, allow all
        if (allowedIps == null || allowedIps.isEmpty()) {
            return true;
        }

        // Check exact match first
        if (allowedIps.contains(ipAddress)) {
            return true;
        }

        // Check CIDR range matches
        for (String allowedIp : allowedIps) {
            if (allowedIp.contains("/") && isIpInCidrRange(ipAddress, allowedIp)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if an IP address falls within a CIDR range.
     *
     * @param ipAddress the IP address to check (e.g., "192.168.1.100")
     * @param cidr the CIDR range (e.g., "192.168.1.0/24")
     * @return true if the IP is within the CIDR range
     */
    private boolean isIpInCidrRange(String ipAddress, String cidr) {
        try {
            String[] cidrParts = cidr.split("/");
            if (cidrParts.length != 2) {
                log.warn("Invalid CIDR notation: {}", cidr);
                return false;
            }

            String networkAddress = cidrParts[0];
            int prefixLength = Integer.parseInt(cidrParts[1]);

            // Parse IP addresses
            InetAddress ip = InetAddress.getByName(ipAddress);
            InetAddress network = InetAddress.getByName(networkAddress);

            byte[] ipBytes = ip.getAddress();
            byte[] networkBytes = network.getAddress();

            // Check if both are same IP version (IPv4 or IPv6)
            if (ipBytes.length != networkBytes.length) {
                return false;
            }

            // Validate prefix length
            int maxPrefix = ipBytes.length * 8;
            if (prefixLength < 0 || prefixLength > maxPrefix) {
                log.warn("Invalid prefix length {} for CIDR: {}", prefixLength, cidr);
                return false;
            }

            // Create mask and compare
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            // Check full bytes
            for (int i = 0; i < fullBytes; i++) {
                if (ipBytes[i] != networkBytes[i]) {
                    return false;
                }
            }

            // Check remaining bits if any
            if (remainingBits > 0 && fullBytes < ipBytes.length) {
                int mask = 0xFF << (8 - remainingBits);
                if ((ipBytes[fullBytes] & mask) != (networkBytes[fullBytes] & mask)) {
                    return false;
                }
            }

            return true;

        } catch (UnknownHostException e) {
            log.warn("Invalid IP address in CIDR check: ip={}, cidr={}", ipAddress, cidr);
            return false;
        } catch (NumberFormatException e) {
            log.warn("Invalid prefix length in CIDR: {}", cidr);
            return false;
        }
    }
}
