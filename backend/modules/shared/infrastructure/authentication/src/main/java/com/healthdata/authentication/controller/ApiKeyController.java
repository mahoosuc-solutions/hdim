package com.healthdata.authentication.controller;

import com.healthdata.authentication.entity.ApiKey;
import com.healthdata.authentication.service.ApiKeyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * REST controller for API Key management.
 *
 * Provides endpoints for:
 * - Creating API keys
 * - Listing API keys
 * - Rotating API keys
 * - Revoking API keys
 * - Updating API key settings
 *
 * Security:
 * - Requires ADMIN or API_ADMIN role
 * - Keys are scoped to tenants
 * - Raw key is only returned once at creation
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'API_ADMIN')")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * Request DTO for creating an API key.
     */
    @Data
    public static class CreateApiKeyRequest {
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be 100 characters or less")
        private String name;

        @Size(max = 500, message = "Description must be 500 characters or less")
        private String description;

        @NotBlank(message = "Tenant ID is required")
        private String tenantId;

        private Set<String> scopes;

        private Integer expiresInDays;

        private Integer rateLimitPerMinute;

        private Set<String> allowedIps;
    }

    /**
     * Response DTO for API key creation (includes raw key).
     */
    @Data
    public static class ApiKeyCreatedResponse {
        private UUID id;
        private String name;
        private String description;
        private String tenantId;
        private String keyPrefix;
        private String rawKey; // Only returned once!
        private Set<String> scopes;
        private String expiresAt;
        private Integer rateLimitPerMinute;
        private String createdAt;
        private String message;
    }

    /**
     * Response DTO for API key details (no raw key).
     */
    @Data
    public static class ApiKeyResponse {
        private UUID id;
        private String name;
        private String description;
        private String tenantId;
        private String keyPrefix;
        private Set<String> scopes;
        private Boolean active;
        private String expiresAt;
        private String lastUsedAt;
        private String lastUsedIp;
        private Long usageCount;
        private Integer rateLimitPerMinute;
        private Set<String> allowedIps;
        private String createdAt;
        private String updatedAt;
        private String revokedAt;
        private String revocationReason;
    }

    /**
     * Request DTO for updating scopes.
     */
    @Data
    public static class UpdateScopesRequest {
        private Set<String> scopes;
    }

    /**
     * Request DTO for updating allowed IPs.
     */
    @Data
    public static class UpdateAllowedIpsRequest {
        private Set<String> allowedIps;
    }

    /**
     * Request DTO for revoking a key.
     */
    @Data
    public static class RevokeKeyRequest {
        @Size(max = 500, message = "Reason must be 500 characters or less")
        private String reason;
    }

    /**
     * Create a new API key.
     *
     * @param request API key creation request
     * @param authentication Current user authentication
     * @return Created API key with raw key (shown once!)
     */
    @PostMapping
    public ResponseEntity<ApiKeyCreatedResponse> createApiKey(
        @Valid @RequestBody CreateApiKeyRequest request,
        Authentication authentication
    ) {
        log.info("Creating API key '{}' for tenant: {}", request.getName(), request.getTenantId());

        UUID userId = extractUserId(authentication);

        ApiKeyService.ApiKeyCreationResult result = apiKeyService.createApiKey(
            request.getName(),
            request.getDescription(),
            request.getTenantId(),
            userId,
            request.getScopes(),
            request.getExpiresInDays(),
            request.getRateLimitPerMinute()
        );

        // Update allowed IPs if provided
        if (request.getAllowedIps() != null && !request.getAllowedIps().isEmpty()) {
            apiKeyService.updateAllowedIps(result.apiKey().getId(), request.getAllowedIps());
        }

        ApiKeyCreatedResponse response = new ApiKeyCreatedResponse();
        response.setId(result.apiKey().getId());
        response.setName(result.apiKey().getName());
        response.setDescription(result.apiKey().getDescription());
        response.setTenantId(result.apiKey().getTenantId());
        response.setKeyPrefix(result.apiKey().getKeyPrefix());
        response.setRawKey(result.rawKey()); // Only returned once!
        response.setScopes(result.apiKey().getScopes());
        response.setExpiresAt(result.apiKey().getExpiresAt() != null
            ? result.apiKey().getExpiresAt().toString() : null);
        response.setRateLimitPerMinute(result.apiKey().getRateLimitPerMinute());
        response.setCreatedAt(result.apiKey().getCreatedAt().toString());
        response.setMessage("API key created. Store the raw key securely - it will not be shown again!");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all API keys for a tenant.
     *
     * @param tenantId Tenant ID
     * @return List of API keys (without raw keys)
     */
    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> listApiKeys(
        @RequestParam String tenantId
    ) {
        log.debug("Listing API keys for tenant: {}", tenantId);

        List<ApiKey> apiKeys = apiKeyService.getApiKeysForTenant(tenantId);
        List<ApiKeyResponse> response = apiKeys.stream()
            .map(this::toResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Get API key details by ID.
     *
     * @param id API key ID
     * @return API key details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiKeyResponse> getApiKey(@PathVariable UUID id) {
        log.debug("Getting API key: {}", id);

        ApiKey apiKey = apiKeyService.getApiKey(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "API key not found: " + id));

        return ResponseEntity.ok(toResponse(apiKey));
    }

    /**
     * Rotate an API key (create new, revoke old).
     *
     * @param id API key ID to rotate
     * @param authentication Current user
     * @return New API key with raw key
     */
    @PostMapping("/{id}/rotate")
    public ResponseEntity<ApiKeyCreatedResponse> rotateApiKey(
        @PathVariable UUID id,
        Authentication authentication
    ) {
        log.info("Rotating API key: {}", id);

        UUID userId = extractUserId(authentication);

        ApiKeyService.ApiKeyCreationResult result = apiKeyService.rotateApiKey(id, userId);

        ApiKeyCreatedResponse response = new ApiKeyCreatedResponse();
        response.setId(result.apiKey().getId());
        response.setName(result.apiKey().getName());
        response.setDescription(result.apiKey().getDescription());
        response.setTenantId(result.apiKey().getTenantId());
        response.setKeyPrefix(result.apiKey().getKeyPrefix());
        response.setRawKey(result.rawKey());
        response.setScopes(result.apiKey().getScopes());
        response.setExpiresAt(result.apiKey().getExpiresAt() != null
            ? result.apiKey().getExpiresAt().toString() : null);
        response.setRateLimitPerMinute(result.apiKey().getRateLimitPerMinute());
        response.setCreatedAt(result.apiKey().getCreatedAt().toString());
        response.setMessage("API key rotated. Old key has been revoked. Store the new key securely!");

        return ResponseEntity.ok(response);
    }

    /**
     * Revoke an API key.
     *
     * @param id API key ID
     * @param request Revocation request with reason
     * @param authentication Current user
     * @return Empty response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeApiKey(
        @PathVariable UUID id,
        @RequestBody(required = false) RevokeKeyRequest request,
        Authentication authentication
    ) {
        log.info("Revoking API key: {}", id);

        UUID userId = extractUserId(authentication);
        String reason = request != null ? request.getReason() : "Revoked by admin";

        apiKeyService.revokeApiKey(id, userId, reason);

        return ResponseEntity.noContent().build();
    }

    /**
     * Update API key scopes.
     *
     * @param id API key ID
     * @param request New scopes
     * @return Updated API key
     */
    @PatchMapping("/{id}/scopes")
    public ResponseEntity<ApiKeyResponse> updateScopes(
        @PathVariable UUID id,
        @RequestBody UpdateScopesRequest request
    ) {
        log.info("Updating scopes for API key: {}", id);

        apiKeyService.updateScopes(id, request.getScopes());

        ApiKey apiKey = apiKeyService.getApiKey(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "API key not found: " + id));

        return ResponseEntity.ok(toResponse(apiKey));
    }

    /**
     * Update API key allowed IPs.
     *
     * @param id API key ID
     * @param request Allowed IP addresses
     * @return Updated API key
     */
    @PatchMapping("/{id}/allowed-ips")
    public ResponseEntity<ApiKeyResponse> updateAllowedIps(
        @PathVariable UUID id,
        @RequestBody UpdateAllowedIpsRequest request
    ) {
        log.info("Updating allowed IPs for API key: {}", id);

        apiKeyService.updateAllowedIps(id, request.getAllowedIps());

        ApiKey apiKey = apiKeyService.getApiKey(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "API key not found: " + id));

        return ResponseEntity.ok(toResponse(apiKey));
    }

    /**
     * Convert entity to response DTO.
     */
    private ApiKeyResponse toResponse(ApiKey apiKey) {
        ApiKeyResponse response = new ApiKeyResponse();
        response.setId(apiKey.getId());
        response.setName(apiKey.getName());
        response.setDescription(apiKey.getDescription());
        response.setTenantId(apiKey.getTenantId());
        response.setKeyPrefix(apiKey.getKeyPrefix());
        response.setScopes(apiKey.getScopes());
        response.setActive(apiKey.getActive());
        response.setExpiresAt(apiKey.getExpiresAt() != null
            ? apiKey.getExpiresAt().toString() : null);
        response.setLastUsedAt(apiKey.getLastUsedAt() != null
            ? apiKey.getLastUsedAt().toString() : null);
        response.setLastUsedIp(apiKey.getLastUsedIp());
        response.setUsageCount(apiKey.getUsageCount());
        response.setRateLimitPerMinute(apiKey.getRateLimitPerMinute());
        response.setAllowedIps(apiKey.getAllowedIps());
        response.setCreatedAt(apiKey.getCreatedAt().toString());
        response.setUpdatedAt(apiKey.getUpdatedAt().toString());
        response.setRevokedAt(apiKey.getRevokedAt() != null
            ? apiKey.getRevokedAt().toString() : null);
        response.setRevocationReason(apiKey.getRevocationReason());
        return response;
    }

    /**
     * Extract user ID from authentication.
     */
    private UUID extractUserId(Authentication authentication) {
        // In a real implementation, extract from JWT claims or UserDetails
        // For now, return a placeholder
        if (authentication != null && authentication.getPrincipal() != null) {
            String username = authentication.getName();
            // This would normally query the user service
            return UUID.nameUUIDFromBytes(username.getBytes());
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
    }
}
