package com.healthdata.sales.controller;

import com.healthdata.sales.service.LinkedInOAuthService;
import com.healthdata.sales.service.LinkedInOAuthService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * LinkedIn OAuth Controller
 *
 * Handles LinkedIn OAuth 2.0 authorization flow for connecting user accounts.
 * This enables the sales team to:
 * - Connect their LinkedIn account for outreach tracking
 * - View their connection status
 * - Disconnect when no longer needed
 */
@RestController
@RequestMapping("/api/sales/linkedin/oauth")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "LinkedIn OAuth",
    description = """
        OAuth 2.0 authorization endpoints for connecting LinkedIn accounts.

        Flow:
        1. GET /authorize - Get authorization URL
        2. User authorizes in LinkedIn
        3. LinkedIn redirects to callback URL with code
        4. POST /callback - Exchange code for tokens

        Requirements:
        - LinkedIn Developer App with OAuth 2.0 enabled
        - Redirect URI configured in LinkedIn app settings
        - Environment variables: linkedin.oauth.clientId, linkedin.oauth.clientSecret
        """
)
@SecurityRequirement(name = "bearer-jwt")
public class LinkedInOAuthController {

    private final LinkedInOAuthService oauthService;

    @GetMapping("/authorize")
    @Operation(
        summary = "Get authorization URL",
        description = """
            Generates the LinkedIn OAuth authorization URL.

            The frontend should redirect the user to this URL to start the OAuth flow.
            After the user authorizes, LinkedIn will redirect back to the configured
            redirect URI with an authorization code.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Authorization URL generated",
            content = @Content(schema = @Schema(implementation = AuthorizationUrlResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "LinkedIn integration not enabled"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<AuthorizationUrlResponse> getAuthorizationUrl(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "User ID for token storage", required = true)
            @RequestHeader("X-User-ID") UUID userId,
            @Parameter(description = "Optional custom redirect URI (must be registered in LinkedIn app)")
            @RequestParam(required = false) String redirectUri) {

        log.info("Generating LinkedIn authorization URL for user {} in tenant {}", userId, tenantId);
        return ResponseEntity.ok(oauthService.getAuthorizationUrl(tenantId, userId, redirectUri));
    }

    @PostMapping("/callback")
    @Operation(
        summary = "Exchange authorization code",
        description = """
            Exchanges the authorization code from LinkedIn for access tokens.

            Call this endpoint after the user is redirected back from LinkedIn.
            The code and state parameters come from LinkedIn's redirect.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully connected LinkedIn account",
            content = @Content(schema = @Schema(implementation = LinkedInTokenResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid or expired state/code"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Failed to exchange code with LinkedIn")
    })
    public ResponseEntity<LinkedInTokenResponse> handleCallback(
            @Parameter(description = "Authorization code from LinkedIn", required = true)
            @RequestParam String code,
            @Parameter(description = "State parameter for CSRF validation", required = true)
            @RequestParam String state,
            @Parameter(description = "Redirect URI used in authorization (must match)")
            @RequestParam(required = false) String redirectUri) {

        log.info("Processing LinkedIn OAuth callback");
        return ResponseEntity.ok(oauthService.exchangeCodeForTokens(code, state, redirectUri));
    }

    @GetMapping("/status")
    @Operation(
        summary = "Get connection status",
        description = "Check if the current user has an active LinkedIn connection."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Connection status retrieved",
            content = @Content(schema = @Schema(implementation = ConnectionStatus.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ConnectionStatus> getConnectionStatus(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "User ID", required = true)
            @RequestHeader("X-User-ID") UUID userId) {

        return ResponseEntity.ok(oauthService.getConnectionStatus(tenantId, userId));
    }

    @PostMapping("/disconnect")
    @Operation(
        summary = "Disconnect LinkedIn account",
        description = """
            Revokes the stored LinkedIn tokens and disconnects the account.

            Note: This only removes tokens from our system. The user should also
            revoke access in their LinkedIn Settings > Data Privacy > Permitted Services.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account disconnected"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> disconnect(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "User ID", required = true)
            @RequestHeader("X-User-ID") UUID userId) {

        log.info("Disconnecting LinkedIn account for user {} in tenant {}", userId, tenantId);
        oauthService.disconnect(tenantId, userId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "LinkedIn account disconnected"
        ));
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = """
            Manually trigger a token refresh.

            Tokens are automatically refreshed when needed, but this endpoint
            allows explicit refresh for testing or recovery scenarios.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token refresh attempted"),
        @ApiResponse(responseCode = "400", description = "No token to refresh"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> refreshToken(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "User ID", required = true)
            @RequestHeader("X-User-ID") UUID userId) {

        log.info("Manual token refresh requested for user {} in tenant {}", userId, tenantId);
        boolean success = oauthService.refreshToken(tenantId, userId);
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "Token refreshed" : "Failed to refresh token"
        ));
    }
}
