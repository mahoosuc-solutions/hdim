package com.healthdata.gateway.controller;

import com.healthdata.gateway.dto.TokenRevocationRequest;
import com.healthdata.gateway.dto.TokenRevocationResponse;
import com.healthdata.gateway.service.TokenRevocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;

/**
 * Token Revocation Controller (Phase 2.0 Team 3.2)
 *
 * REST endpoints for token revocation operations:
 * - POST /api/v1/auth/logout - Revoke all tokens for authenticated user
 * - POST /api/v1/auth/revoke - Revoke specific access token
 *
 * Requires:
 * - Authentication: Bearer token in Authorization header
 * - Tenant: X-Tenant-ID header
 * - Rate limiting: Applied via RateLimitFilter
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Slf4j
public class TokenRevocationController {

    private final TokenRevocationService tokenRevocationService;

    /**
     * Logout endpoint - revoke all tokens for authenticated user
     *
     * @param tenantId Tenant ID from header
     * @param request HTTP request context
     * @return ResponseEntity with token count revoked
     */
    @PostMapping("/logout")
    public ResponseEntity<TokenRevocationResponse> logout(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            HttpServletRequest request) {

        try {
            // Get authenticated user from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userId = authentication.getName();

            // Validate tenant ID
            if (tenantId == null || tenantId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(TokenRevocationResponse.builder()
                        .message("X-Tenant-ID header is required")
                        .build());
            }

            log.debug("Processing logout for user: {} (tenant: {})", userId, tenantId);

            // Revoke all tokens for user
            int tokensRevoked = tokenRevocationService.revokeAllUserTokens(userId, tenantId, "LOGOUT");

            log.info("User logged out successfully: {} (tokens revoked: {})", userId, tokensRevoked);

            return ResponseEntity.ok(TokenRevocationResponse.builder()
                .message("Successfully logged out")
                .tokensRevoked(tokensRevoked)
                .build());

        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument during logout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(TokenRevocationResponse.builder()
                    .message("Invalid tenant or user")
                    .build());
        } catch (Exception e) {
            log.error("Unexpected error during logout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(TokenRevocationResponse.builder()
                    .message("Logout failed: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Revoke specific token endpoint
     *
     * @param tenantId Tenant ID from header
     * @param request Request body with token and reason
     * @param httpRequest HTTP request context
     * @return ResponseEntity with revocation timestamp
     */
    @PostMapping("/revoke")
    public ResponseEntity<TokenRevocationResponse> revokeToken(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @Valid @RequestBody TokenRevocationRequest request,
            HttpServletRequest httpRequest) {

        try {
            // Get authenticated user from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Validate tenant ID
            if (tenantId == null || tenantId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(TokenRevocationResponse.builder()
                        .message("X-Tenant-ID header is required")
                        .build());
            }

            // Validate request fields
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(TokenRevocationResponse.builder()
                        .message("Token is required")
                        .build());
            }

            if (request.getReason() == null || request.getReason().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(TokenRevocationResponse.builder()
                        .message("Revocation reason is required")
                        .build());
            }

            log.debug("Processing token revocation with reason: {} (tenant: {})", request.getReason(), tenantId);

            // Revoke the token
            tokenRevocationService.revokeAccessToken(request.getToken(), tenantId, request.getReason());

            log.info("Token revoked successfully with reason: {}", request.getReason());

            return ResponseEntity.ok(TokenRevocationResponse.builder()
                .message("Token revoked successfully")
                .revokedAt(Instant.now())
                .build());

        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument during token revocation: {}", e.getMessage());

            // Check if it's an invalid reason error
            if (e.getMessage() != null && e.getMessage().contains("Invalid revocation reason")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(TokenRevocationResponse.builder()
                        .message(e.getMessage())
                        .build());
            }

            // Check if it's a tenant mismatch error
            if (e.getMessage() != null && e.getMessage().contains("Tenant mismatch")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(TokenRevocationResponse.builder()
                        .message("Tenant mismatch")
                        .build());
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(TokenRevocationResponse.builder()
                    .message(e.getMessage())
                    .build());

        } catch (IllegalStateException e) {
            log.warn("Token already revoked: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(TokenRevocationResponse.builder()
                    .message("Token already revoked")
                    .build());

        } catch (Exception e) {
            log.error("Unexpected error during token revocation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(TokenRevocationResponse.builder()
                    .message("Revocation failed: " + e.getMessage())
                    .build());
        }
    }
}
