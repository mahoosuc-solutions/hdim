package com.healthdata.investor.controller;

import com.healthdata.investor.dto.LinkedInDTO;
import com.healthdata.investor.security.UserPrincipal;
import com.healthdata.investor.service.LinkedInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for LinkedIn OAuth integration.
 */
@RestController
@RequestMapping("/api/linkedin")
@RequiredArgsConstructor
@Tag(name = "LinkedIn", description = "LinkedIn OAuth integration endpoints")
public class LinkedInController {

    private final LinkedInService linkedInService;

    @GetMapping("/auth-url")
    @Operation(summary = "Get LinkedIn OAuth authorization URL")
    public ResponseEntity<LinkedInDTO.AuthorizationUrlResponse> getAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        String authUrl = linkedInService.getAuthorizationUrl(state);

        return ResponseEntity.ok(LinkedInDTO.AuthorizationUrlResponse.builder()
                .authorizationUrl(authUrl)
                .state(state)
                .build());
    }

    @PostMapping("/callback")
    @Operation(summary = "Handle LinkedIn OAuth callback")
    public ResponseEntity<LinkedInDTO.ConnectionStatus> handleCallback(
            @RequestBody LinkedInDTO.OAuthCallbackRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        LinkedInDTO.ConnectionStatus status = linkedInService.handleOAuthCallback(
                request.getCode(),
                principal.getUserId()
        );
        return ResponseEntity.ok(status);
    }

    @GetMapping("/status")
    @Operation(summary = "Get LinkedIn connection status")
    public ResponseEntity<LinkedInDTO.ConnectionStatus> getConnectionStatus(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(linkedInService.getConnectionStatus(principal.getUserId()));
    }

    @PostMapping("/disconnect")
    @Operation(summary = "Disconnect LinkedIn account")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal UserPrincipal principal) {
        linkedInService.disconnect(principal.getUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh LinkedIn access token")
    public ResponseEntity<Void> refreshToken(@AuthenticationPrincipal UserPrincipal principal) {
        linkedInService.refreshTokenIfNeeded(principal.getUserId());
        return ResponseEntity.ok().build();
    }
}
