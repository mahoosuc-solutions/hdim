package com.healthdata.authentication.controller;

import com.healthdata.authentication.config.JwtConfig;
import com.healthdata.authentication.domain.Tenant;
import com.healthdata.authentication.domain.TenantStatus;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.dto.*;
import com.healthdata.authentication.entity.RefreshToken;
import com.healthdata.authentication.exception.TenantInactiveException;
import com.healthdata.authentication.exception.TenantNotFoundException;
import com.healthdata.authentication.repository.TenantRepository;
import com.healthdata.authentication.repository.UserRepository;
import com.healthdata.authentication.audit.MfaAuditEvent;
import com.healthdata.authentication.service.CookieService;
import com.healthdata.authentication.service.JwtTokenService;
import com.healthdata.authentication.service.LogoutService;
import com.healthdata.authentication.service.MfaService;
import com.healthdata.authentication.service.MfaPolicyService;
import com.healthdata.authentication.service.MfaTokenService;
import com.healthdata.authentication.service.RefreshTokenService;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.audit.models.AuditEvent;
import com.healthdata.audit.models.AuditOutcome;
import com.healthdata.audit.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Optional;

/**
 * REST controller for authentication operations.
 * Provides endpoints for login, registration, logout, and user information.
 *
 * SECURITY CRITICAL: This controller handles user authentication and registration.
 * All endpoints use proper validation and error handling.
 *
 * Endpoints:
 * - POST /api/v1/auth/login - Authenticate user
 * - POST /api/v1/auth/register - Register new user (ADMIN/SUPER_ADMIN only)
 * - POST /api/v1/auth/logout - Logout current user
 * - GET /api/v1/auth/me - Get current user information
 *
 * CONDITIONAL LOADING:
 * This controller is conditionally loaded by AuthenticationControllerAutoConfiguration
 * only when authentication.controller.enabled=true in application properties.
 * By default, this is disabled in all services except the Gateway.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Validated
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    prefix = "authentication.controller",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class AuthController {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final LogoutService logoutService;
    private final JwtConfig jwtConfig;
    private final MfaService mfaService;
    private final MfaTokenService mfaTokenService;
    private final CookieService cookieService;
    private final com.healthdata.authentication.service.MfaPolicyService mfaPolicyService;
    private final AuditService auditService;  // Optional - may be null if audit module not available

    public AuthController(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            LogoutService logoutService,
            JwtConfig jwtConfig,
            MfaService mfaService,
            MfaTokenService mfaTokenService,
            CookieService cookieService,
            com.healthdata.authentication.service.MfaPolicyService mfaPolicyService,
            @Autowired(required = false) AuditService auditService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.logoutService = logoutService;
        this.jwtConfig = jwtConfig;
        this.mfaService = mfaService;
        this.mfaTokenService = mfaTokenService;
        this.cookieService = cookieService;
        this.mfaPolicyService = mfaPolicyService;
        this.auditService = auditService;
    }

    /**
     * Authenticate user with username/password.
     * Returns JWT access and refresh tokens, or MFA challenge if MFA is enabled.
     *
     * SECURITY: Tokens are also set as HttpOnly cookies for XSS protection.
     * Clients can use either the response body tokens (legacy) or cookies.
     * For maximum security, use HttpOnly cookies with withCredentials: true.
     *
     * @param loginRequest Login credentials
     * @param httpRequest HTTP request for IP tracking
     * @param httpResponse HTTP response for setting cookies
     * @return JwtAuthenticationResponse with tokens, or MfaRequiredResponse if MFA is enabled
     * @throws ResponseStatusException 401 if credentials are invalid or account is locked/disabled
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
        @Valid @RequestBody LoginRequest loginRequest,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());

        try {
            // Resolve username: if input is email, look up the actual username
            String authUsername = loginRequest.getUsername();
            
            // Check if input looks like an email (contains @)
            if (authUsername.contains("@")) {
                log.debug("Email-based login detected, resolving to username");
                // Try to find user by email and use their username for authentication
                Optional<User> userByEmail = userRepository.findByEmail(authUsername);
                if (userByEmail.isPresent()) {
                    authUsername = userByEmail.get().getUsername();
                    log.debug("Resolved email {} to username {}", loginRequest.getUsername(), authUsername);
                } else {
                    log.warn("Email not found: {}", authUsername);
                    // Let authentication fail naturally below
                }
            }

            // Authenticate using Spring Security's AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    authUsername,
                    loginRequest.getPassword()
                )
            );

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Load user details from database
            User user = userRepository.findByUsernameOrEmail(loginRequest.getUsername())
                .orElseThrow(() -> {
                    log.error("User not found after authentication: {}", loginRequest.getUsername());
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication failed");
                });

            // Update last login time and reset failed login attempts
            user.resetFailedLoginAttempts();
            userRepository.save(user);

            // HIPAA §164.312(d) - Enforce MFA policy for administrative accounts
            if (mfaPolicyService.isMfaRequired(user) && !user.isMfaConfigured()) {
                long daysRemaining = mfaPolicyService.getGracePeriodRemainingDays(user);

                if (daysRemaining <= 0) {
                    // Grace period expired - block login
                    log.warn("MFA_AUDIT: event={}, userId={}, ip={}, username={}, outcome=LOGIN_BLOCKED",
                        MfaAuditEvent.MFA_LOGIN_BLOCKED, user.getId(),
                        extractIpAddress(httpRequest), user.getUsername());

                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "MFA setup required. Your admin account must enable Multi-Factor Authentication " +
                        "for security compliance. Please contact your administrator for account recovery.");
                } else {
                    // Within grace period - allow login with warning
                    log.warn("Admin login allowed with MFA grace period - {} days remaining for user: {}",
                        daysRemaining, user.getUsername());
                    // Warning will be added to response below
                }
            }

            // Check if MFA is enabled
            if (user.isMfaConfigured()) {
                log.info("MFA required for user: {}", user.getUsername());

                // Generate MFA token (short-lived, proves password auth succeeded)
                String mfaToken = mfaTokenService.generateMfaToken(user.getId(), user.getUsername());

                // Clear security context (user not fully authenticated yet)
                SecurityContextHolder.clearContext();

                // Determine available MFA methods
                List<String> availableMethods = new java.util.ArrayList<>();
                String smsPhoneNumber = null;

                if (user.getMfaMethod() == User.MfaMethod.TOTP || user.getMfaMethod() == User.MfaMethod.BOTH) {
                    availableMethods.add("TOTP");
                }
                if (user.getMfaMethod() == User.MfaMethod.SMS || user.getMfaMethod() == User.MfaMethod.BOTH) {
                    availableMethods.add("SMS");
                    // Mask phone number
                    String phone = user.getMfaPhoneNumber();
                    if (phone != null && phone.length() >= 4) {
                        smsPhoneNumber = "****" + phone.substring(phone.length() - 4);
                    }
                }

                MfaRequiredResponse mfaResponse = MfaRequiredResponse.builder()
                    .mfaRequired(true)
                    .mfaToken(mfaToken)
                    .availableMethods(availableMethods)
                    .smsPhoneNumber(smsPhoneNumber)
                    .message("MFA verification required. Choose your preferred method.")
                    .build();

                return ResponseEntity.ok(mfaResponse);
            }

            log.info("Login successful for user: {} (ID: {})", user.getUsername(), user.getId());

            // Generate JWT tokens
            String accessToken = jwtTokenService.generateAccessToken(user);
            String refreshToken = jwtTokenService.generateRefreshToken(user);

            // Store refresh token in database
            refreshTokenService.createRefreshToken(user, refreshToken, httpRequest);

            log.info("Authentication successful for user {} from IP: {}",
                user.getUsername(), extractIpAddress(httpRequest));

            // SECURITY: Set tokens as HttpOnly cookies for XSS protection
            cookieService.setAccessTokenCookie(httpResponse, accessToken);
            cookieService.setRefreshTokenCookie(httpResponse, refreshToken);
            log.debug("Set HttpOnly cookies for user: {}", user.getUsername());

            // Build JWT response (tokens also included for legacy client support)
            JwtAuthenticationResponse.JwtAuthenticationResponseBuilder responseBuilder =
                JwtAuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtConfig.getAccessTokenExpirationSeconds())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .roles(user.getRoles())
                    .tenantIds(user.getTenantIds())
                    .mfaEnabled(false);

            // Add MFA warning if admin is in grace period
            if (mfaPolicyService.isMfaRequired(user) && !user.isMfaConfigured()) {
                long daysRemaining = mfaPolicyService.getGracePeriodRemainingDays(user);
                String warningMessage = String.format(
                    "Login successful. WARNING: Your admin account must enable MFA within %d days. " +
                    "Use POST /api/v1/auth/mfa/setup to configure Multi-Factor Authentication.",
                    daysRemaining
                );
                responseBuilder.message(warningMessage);
            } else {
                responseBuilder.message("Login successful");
            }

            JwtAuthenticationResponse response = responseBuilder.build();

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Invalid credentials for user: {} from IP: {}",
                loginRequest.getUsername(), extractIpAddress(httpRequest));

            // Increment failed login attempts
            userRepository.findByUsernameOrEmail(loginRequest.getUsername())
                .ifPresent(user -> {
                    user.incrementFailedLoginAttempts();
                    userRepository.save(user);
                    log.warn("Failed login attempt {} for user: {}",
                        user.getFailedLoginAttempts(), user.getUsername());
                });

            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");

        } catch (LockedException e) {
            log.warn("Account locked for user: {} from IP: {}",
                loginRequest.getUsername(), extractIpAddress(httpRequest));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Account is locked. Please try again later.");

        } catch (DisabledException e) {
            log.warn("Account disabled for user: {} from IP: {}",
                loginRequest.getUsername(), extractIpAddress(httpRequest));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Account is disabled. Please contact an administrator.");

        } catch (Exception e) {
            log.error("Authentication error for user: {} from IP: {}",
                loginRequest.getUsername(), extractIpAddress(httpRequest), e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication failed");
        }
    }

    /**
     * Register a new user account.
     * Only users with ADMIN or SUPER_ADMIN roles can register new users.
     *
     * @param request Registration details
     * @return UserInfoResponse with created user details
     * @throws ResponseStatusException 409 if username or email already exists
     * @throws ResponseStatusException 400 if validation fails
     */
    @PreAuthorize("hasPermission('USER_WRITE')")
    @PostMapping("/register")
    public ResponseEntity<UserInfoResponse> register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletRequest httpRequest,
        Authentication authentication
    ) {
        log.info("Registration attempt for username: {}, email: {}",
            request.getUsername(), request.getEmail());

        // Validate username doesn't already exist
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Username already exists: {}", request.getUsername());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Username already exists: " + request.getUsername());
        }

        // Validate email doesn't already exist
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Email already exists: {}", request.getEmail());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Email already exists: " + request.getEmail());
        }

        // Validate all tenant IDs exist and are active
        for (String tenantId : request.getTenantIds()) {
            Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

            if (tenant.getStatus() != TenantStatus.ACTIVE) {
                throw new TenantInactiveException(tenantId);
            }
        }

        // Hash password with BCrypt
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // Create new user
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordHash)
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .tenantIds(new HashSet<>(request.getTenantIds()))
            .roles(new HashSet<>(request.getRoles()))
            .active(true)
            .emailVerified(false)
            .failedLoginAttempts(0)
            .build();

        // Save user to database
        user = userRepository.save(user);

        log.info("User registered successfully: {} (ID: {}) by admin: {}",
            user.getUsername(), user.getId(),
            authentication != null ? authentication.getName() : "SYSTEM");

        // Audit user creation (system-level event, no specific tenant)
        String adminUserId = null;
        String adminUsername = null;
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                adminUserId = ((User) principal).getId().toString();
                adminUsername = ((User) principal).getUsername();
            } else if (principal instanceof String) {
                adminUsername = (String) principal;
            }
        }

        // Audit user registration (if audit service is available)
        if (auditService != null) {
            auditService.logAuditEvent(AuditEvent.builder()
                .tenantId(null)  // System-level event
                .userId(adminUserId)
                .username(adminUsername)
                .action(AuditAction.CREATE)
                .resourceType("User")
                .resourceId(user.getId().toString())
                .outcome(AuditOutcome.SUCCESS)
                .serviceName("AuthController")
                .methodName("register")
                .build());
        } else {
            log.debug("Audit service not available - skipping audit log for user registration: {}", user.getUsername());
        }

        // Build response (never include password hash)
        UserInfoResponse response = UserInfoResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .roles(user.getRoles())
            .tenantIds(user.getTenantIds())
            .active(user.getActive())
            .emailVerified(user.getEmailVerified())
            .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Refresh access token using refresh token.
     *
     * Supports both:
     * - Cookie-based refresh: Reads refresh token from HttpOnly cookie
     * - Body-based refresh: Reads refresh token from request body (legacy)
     *
     * @param request Refresh token request (optional if using cookies)
     * @param httpRequest HTTP request for IP tracking and cookie reading
     * @param httpResponse HTTP response for setting new cookies
     * @return New JWT access token (and optionally rotated refresh token)
     * @throws ResponseStatusException 401 if refresh token is invalid
     */
    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthenticationResponse> refreshToken(
        @RequestBody(required = false) RefreshTokenRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        log.info("Token refresh attempt");

        try {
            // Get refresh token from cookie first, then fall back to body
            String refreshToken = cookieService.getRefreshTokenFromCookie(httpRequest)
                .orElse(request != null ? request.getRefreshToken() : null);

            if (refreshToken == null || refreshToken.isBlank()) {
                log.warn("No refresh token provided");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No refresh token provided");
            }

            // Validate refresh token and get user
            Optional<User> userOpt = refreshTokenService.getUserFromRefreshToken(refreshToken);

            if (userOpt.isEmpty()) {
                log.warn("Invalid or expired refresh token");
                // Clear cookies if invalid
                cookieService.clearAuthCookies(httpResponse);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
            }

            User user = userOpt.get();

            // Verify user account is still active
            if (!user.isAccountActive()) {
                log.warn("Inactive account attempted token refresh: {}", user.getUsername());
                cookieService.clearAuthCookies(httpResponse);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is inactive");
            }

            log.info("Token refresh successful for user: {}", user.getUsername());

            // Generate new access token
            String newAccessToken = jwtTokenService.generateAccessToken(user);

            // Optionally rotate refresh token (for better security)
            String newRefreshToken = jwtTokenService.generateRefreshToken(user);

            // Revoke old refresh token
            refreshTokenService.revokeRefreshToken(refreshToken);

            // Store new refresh token
            refreshTokenService.createRefreshToken(user, newRefreshToken, httpRequest);

            // SECURITY: Set new tokens as HttpOnly cookies
            cookieService.setAccessTokenCookie(httpResponse, newAccessToken);
            cookieService.setRefreshTokenCookie(httpResponse, newRefreshToken);

            // Build response
            JwtAuthenticationResponse response = JwtAuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getAccessTokenExpirationSeconds())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles())
                .tenantIds(user.getTenantIds())
                .message("Token refreshed successfully")
                .build();

            return ResponseEntity.ok(response);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error refreshing token", e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token refresh failed");
        }
    }

    /**
     * Logout current user.
     * Revokes the refresh token and clears HttpOnly cookies.
     *
     * @param request Refresh token request (optional if using cookies)
     * @param authentication Current authentication
     * @param httpRequest HTTP request for reading cookies
     * @param httpResponse HTTP response for clearing cookies
     * @return Empty response with 200 OK
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @RequestBody(required = false) RefreshTokenRequest request,
        Authentication authentication,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            log.info("Logout for user: {}", username);

            log.info("User logged out: {} from IP: {}", username, extractIpAddress(httpRequest));

            // ⚠️ CRITICAL: Clear PHI caches on logout (HIPAA compliance)
            // This ensures all tenant-specific PHI caches are evicted when the user logs out
            // See: /backend/HIPAA-CACHE-COMPLIANCE.md
            logoutService.performLogout(username);

            // Revoke refresh token from cookie first, then fall back to body
            String refreshToken = cookieService.getRefreshTokenFromCookie(httpRequest)
                .orElse(request != null ? request.getRefreshToken() : null);

            if (refreshToken != null && !refreshToken.isBlank()) {
                refreshTokenService.revokeRefreshToken(refreshToken);
            }

            // Clear security context
            SecurityContextHolder.clearContext();
        }

        // SECURITY: Always clear cookies on logout (even if not authenticated)
        cookieService.clearAuthCookies(httpResponse);

        return ResponseEntity.ok().build();
    }

    /**
     * Revoke all refresh tokens for the current user.
     * Logs out from all devices.
     *
     * @param authentication Current authentication
     * @return Empty response with 200 OK
     */
    @PostMapping("/revoke")
    public ResponseEntity<Void> revokeAllTokens(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        String username = authentication.getName();
        log.info("Revoking all tokens for user: {}", username);

        // Find user
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Revoke all refresh tokens
        int count = refreshTokenService.revokeAllUserTokens(user.getId());
        log.info("Revoked {} refresh tokens for user: {}", count, username);

        // Clear security context
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok().build();
    }

    /**
     * Get current authenticated user information.
     *
     * @param authentication Current authentication
     * @return UserInfoResponse with current user details
     * @throws ResponseStatusException 401 if not authenticated
     * @throws ResponseStatusException 404 if user not found in database
     */
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        String username = authentication.getName();
        log.debug("Fetching user info for: {}", username);

        // Load user from database
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.error("User not found in database: {}", username);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            });

        // Verify account is still active
        if (!user.isAccountActive()) {
            log.warn("Inactive account accessed: {}", username);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Account is inactive or locked");
        }

        // Build response
        UserInfoResponse response = UserInfoResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .roles(user.getRoles())
            .tenantIds(user.getTenantIds())
            .active(user.getActive())
            .emailVerified(user.getEmailVerified())
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Extract IP address from HTTP request.
     * Handles X-Forwarded-For header for proxied requests.
     *
     * @param request HTTP request
     * @return IP address
     */
    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
