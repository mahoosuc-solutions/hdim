package com.healthdata.authentication.filter;

import com.healthdata.authentication.constants.AuthHeaderConstants;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User Auto-Registration Filter
 *
 * Automatically registers users in the service's local database on first access.
 *
 * ARCHITECTURE:
 * - Gateway validates JWT and injects trusted X-Auth-* headers
 * - This filter checks if user exists in local service database
 * - If not, creates user record from gateway headers
 * - Ensures seamless user experience across all services
 *
 * SECURITY:
 * - Only trusts headers validated by gateway (X-Auth-Validated header)
 * - Does NOT allow external user creation (headers stripped by gateway)
 * - User data comes from verified JWT token, not client input
 *
 * MULTI-TENANCY:
 * - Preserves tenant IDs from gateway headers
 * - Each service maintains its own user records for isolation
 * - Tenant access validated by TrustedTenantAccessFilter
 *
 * COMPLIANCE (HIPAA §164.312):
 * - Audit logs user registration events
 * - Tracks user access to service for audit trail
 * - User ID and tenant IDs logged for accountability
 *
 * Order: 10 (runs after TrustedHeaderAuthFilter at order 5)
 */
@Component
@ConditionalOnBean(UserRepository.class)
@Order(10)
@RequiredArgsConstructor
public class UserAutoRegistrationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(UserAutoRegistrationFilter.class);

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Extract user info from gateway-validated headers
        String userIdHeader = request.getHeader(AuthHeaderConstants.HEADER_USER_ID);
        String usernameHeader = request.getHeader(AuthHeaderConstants.HEADER_USERNAME);
        String tenantIdsHeader = request.getHeader(AuthHeaderConstants.HEADER_TENANT_IDS);
        String rolesHeader = request.getHeader(AuthHeaderConstants.HEADER_ROLES);
        String validatedHeader = request.getHeader(AuthHeaderConstants.HEADER_VALIDATED);

        // Skip if no authenticated user (public endpoints)
        if (userIdHeader == null || usernameHeader == null || validatedHeader == null) {
            log.trace("No authenticated user, skipping auto-registration");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            UUID userId = UUID.fromString(userIdHeader);

            // Check if user exists in this service's database
            if (!userRepository.existsById(userId)) {
                // User doesn't exist - auto-register
                registerUser(userId, usernameHeader, tenantIdsHeader, rolesHeader, request);
            } else {
                // User exists - optionally update last access time
                updateLastAccess(userId);
            }

            filterChain.doFilter(request, response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format in header: {}", userIdHeader, e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid user ID format");
        } catch (Exception e) {
            log.error("Error during user auto-registration", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "User registration failed");
        }
    }

    /**
     * Register new user in service database from gateway headers
     */
    private void registerUser(
        UUID userId,
        String username,
        String tenantIdsHeader,
        String rolesHeader,
        HttpServletRequest request
    ) {
        // Parse tenant IDs
        Set<String> tenantIds = parseTenantIds(tenantIdsHeader);

        // Parse roles
        Set<UserRole> roles = parseRoles(rolesHeader);

        // Create user entity
        User user = User.builder()
            .id(userId)
            .username(username)
            .email(username + "@auto-registered.local") // Placeholder - real email comes from auth service
            .passwordHash("N/A") // Password managed by authentication service, not local
            .firstName("Auto")
            .lastName("Registered")
            .tenantIds(tenantIds)
            .roles(roles)
            .active(true)
            .emailVerified(false)
            .mfaEnabled(false)
            .failedLoginAttempts(0)
            .lastLoginAt(Instant.now())
            .notes("Auto-registered from gateway authentication")
            .build();

        // Save to database
        userRepository.save(user);

        // Audit log
        log.info("Auto-registered user in service database: userId={}, username={}, tenants={}, roles={}, service={}, ip={}",
            userId, username, tenantIds, roles, getServiceName(), getClientIp(request));
    }

    /**
     * Update user's last access timestamp
     */
    private void updateLastAccess(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
        });
    }

    /**
     * Parse comma-separated tenant IDs from header
     */
    private Set<String> parseTenantIds(String tenantIdsHeader) {
        if (tenantIdsHeader == null || tenantIdsHeader.isBlank()) {
            return new HashSet<>();
        }
        return Arrays.stream(tenantIdsHeader.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    }

    /**
     * Parse comma-separated roles from header
     */
    private Set<UserRole> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return Set.of(UserRole.VIEWER); // Default role
        }

        return Arrays.stream(rolesHeader.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(roleStr -> {
                try {
                    // Remove "ROLE_" prefix if present (Spring Security adds it)
                    String cleanRole = roleStr.startsWith("ROLE_")
                        ? roleStr.substring(5)
                        : roleStr;
                    return UserRole.valueOf(cleanRole);
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown role '{}', defaulting to VIEWER", roleStr);
                    return UserRole.VIEWER;
                }
            })
            .collect(Collectors.toSet());
    }

    /**
     * Get service name from application properties or class path
     */
    private String getServiceName() {
        // Try to detect service name from package or system property
        String serviceName = System.getProperty("spring.application.name");
        if (serviceName != null) {
            return serviceName;
        }

        // Fallback - extract from class path
        String className = this.getClass().getName();
        if (className.contains("quality")) return "quality-measure-service";
        if (className.contains("caregap")) return "care-gap-service";
        if (className.contains("fhir")) return "fhir-service";
        if (className.contains("patient")) return "patient-service";

        return "unknown-service";
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Handle multiple IPs in X-Forwarded-For (take first one)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
