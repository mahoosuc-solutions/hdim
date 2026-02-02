package com.healthdata.investor.controller;

import com.healthdata.investor.dto.LoginRequest;
import com.healthdata.investor.dto.LoginResponse;
import com.healthdata.investor.dto.RefreshTokenRequest;
import com.healthdata.investor.security.UserPrincipal;
import com.healthdata.investor.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authentication endpoints.
 * Handles login, token refresh, and user info retrieval.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user information")
    public ResponseEntity<LoginResponse.UserDTO> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        LoginResponse.UserDTO user = authService.getCurrentUser(principal.getUserId());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout (client-side token invalidation)")
    public ResponseEntity<Void> logout() {
        // JWT tokens are stateless, so logout is handled client-side by removing the token
        // For enhanced security, implement token blacklisting with Redis in production
        return ResponseEntity.ok().build();
    }
}
