package com.healthdata.shared.security.api;

import com.healthdata.shared.security.jwt.JwtTokenProvider;
import com.healthdata.shared.security.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authentication Controller - Provides JWT authentication endpoints
 *
 * Endpoints:
 * - POST /api/auth/login - Authenticate user and receive JWT tokens
 * - POST /api/auth/refresh - Refresh expired access token
 *
 * Spring Boot 3.3.5 compatible - Uses Jakarta EE (jakarta.*)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    /**
     * Authenticate user with username and password
     *
     * Returns JWT access token and refresh token on successful authentication.
     *
     * @param username User's username
     * @param password User's password
     * @return JWT tokens and user information
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestParam String username,
            @RequestParam String password) {

        log.info("Authentication request for user: {}", username);

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            // Extract user roles
            List<String> roles = authentication.getAuthorities().stream()
                    .map(auth -> {
                        String authority = auth.getAuthority();
                        return authority.startsWith("ROLE_") ? authority.substring(5) : authority;
                    })
                    .toList();

            // Generate tokens
            Map<String, String> tokens = tokenProvider.generateTokenPair(username, roles);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "User authenticated successfully");
            response.put("accessToken", tokens.get("accessToken"));
            response.put("refreshToken", tokens.get("refreshToken"));
            response.put("username", username);
            response.put("roles", roles);
            response.put("expiresIn", tokenProvider.getTokenExpirationMs() / 1000); // seconds

            log.info("User {} authenticated successfully", username);
            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            log.warn("Authentication failed for user {}: {}", username, ex.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Authentication failed: Invalid username or password");
            error.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Refresh access token using refresh token
     *
     * Returns a new access token when the current one is expired.
     * Refresh token must be valid and not expired.
     *
     * @param refreshToken Refresh token from login response
     * @return New JWT access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestParam String refreshToken) {

        log.debug("Token refresh request");

        try {
            // Validate refresh token
            if (!tokenProvider.validateToken(refreshToken)) {
                log.warn("Invalid or expired refresh token");

                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Invalid or expired refresh token");
                error.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            // Extract user info from refresh token
            String username = tokenProvider.getUsernameFromToken(refreshToken);
            List<String> roles = tokenProvider.getRolesFromToken(refreshToken);

            // Generate new access token
            String newAccessToken = tokenProvider.generateToken(username, roles);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Token refreshed successfully");
            response.put("accessToken", newAccessToken);
            response.put("expiresIn", tokenProvider.getTokenExpirationMs() / 1000); // seconds

            log.debug("Token refreshed successfully for user: {}", username);
            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            log.error("Error refreshing token: {}", ex.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to refresh token");
            error.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Get current authenticated user info
     *
     * Returns information about the currently authenticated user.
     *
     * @return Current user information
     */
    @PostMapping("/me")
    public ResponseEntity<?> getCurrentUser() {

        String username = null;
        try {
            Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated()) {
                username = auth.getName();

                List<String> roles = auth.getAuthorities().stream()
                        .map(authority -> {
                            String authority_str = authority.getAuthority();
                            return authority_str.startsWith("ROLE_")
                                    ? authority_str.substring(5)
                                    : authority_str;
                        })
                        .toList();

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("username", username);
                response.put("roles", roles);

                return ResponseEntity.ok(response);
            }
        } catch (Exception ex) {
            log.error("Error getting current user: {}", ex.getMessage());
        }

        Map<String, Object> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", "User not authenticated");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}
