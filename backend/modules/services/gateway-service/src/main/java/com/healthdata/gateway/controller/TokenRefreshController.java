package com.healthdata.gateway.controller;

import com.healthdata.gateway.dto.TokenRefreshRequest;
import com.healthdata.gateway.dto.TokenRefreshResponse;
import com.healthdata.gateway.service.TokenRefreshService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Token Refresh Controller (Phase 2.0 Team 3.1)
 *
 * Provides REST endpoint for refreshing access tokens
 * Rate limited to 100 requests per minute
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class TokenRefreshController {

    private final TokenRefreshService tokenRefreshService;

    /**
     * Refresh access token using refresh token
     *
     * Request:
     * POST /api/v1/auth/refresh
     * Header: X-Tenant-ID: tenant-001
     * Body: { "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." }
     *
     * Response:
     * 200 OK
     * {
     *   "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "tokenType": "Bearer",
     *   "expiresIn": 900
     * }
     *
     * Errors:
     * 401 Unauthorized - Invalid or expired refresh token
     * 403 Forbidden - Token has been revoked
     * 429 Too Many Requests - Rate limit exceeded
     *
     * @param request TokenRefreshRequest with refresh token
     * @param tenantId Tenant ID from X-Tenant-ID header
     * @return TokenRefreshResponse with new tokens
     */
    @PostMapping("/refresh")
    @PreAuthorize("isAuthenticated()")  // Must be authenticated to refresh
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request,
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId) {

        log.info("Token refresh requested for tenant: {}", tenantId);

        TokenRefreshResponse response = tokenRefreshService.refreshToken(request, tenantId);

        log.info("Token refreshed successfully for tenant: {}", tenantId);

        return ResponseEntity.ok(response);
    }
}
