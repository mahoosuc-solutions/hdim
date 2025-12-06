package com.healthdata.authentication.security;

import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Security filter that validates tenant access for all API requests.
 *
 * CRITICAL SECURITY COMPONENT: This filter prevents tenant isolation bypass.
 * It ensures that authenticated users can only access data from tenants they have access to.
 *
 * Process:
 * 1. Extract X-Tenant-ID header from request
 * 2. Get authenticated user from SecurityContext
 * 3. Verify user has access to the requested tenant
 * 4. Block request with 403 if access denied
 *
 * This addresses CRITICAL finding #2 from security audit:
 * "Complete Bypass of Tenant Isolation"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantAccessFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    /**
     * Public endpoints that don't require tenant validation
     */
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/actuator/health",
        "/api/v1/health",
        "/swagger-ui",
        "/v3/api-docs",
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh"
    );

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String tenantId = request.getHeader("X-Tenant-ID");

        log.debug("Tenant access filter: path={}, tenantId={}", requestPath, tenantId);

        // Skip validation for public endpoints
        if (isPublicPath(requestPath)) {
            log.debug("Public path, skipping tenant validation: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // If no tenant ID in header, allow (controller will handle validation)
        // This allows endpoints that don't require tenant context
        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.debug("No tenant ID in request, allowing");
            filterChain.doFilter(request, response);
            return;
        }

        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // If no authentication or anonymous, let Spring Security handle it
        if (authentication == null || !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getPrincipal())) {
            log.debug("No authentication, allowing Spring Security to handle");
            filterChain.doFilter(request, response);
            return;
        }

        // Get username from authentication
        String username = null;
        if (authentication.getPrincipal() instanceof UserDetails) {
            username = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else if (authentication.getPrincipal() instanceof String) {
            username = (String) authentication.getPrincipal();
        }

        if (username == null) {
            log.warn("Could not extract username from authentication: {}", authentication.getPrincipal());
            sendForbiddenResponse(response, "Invalid authentication principal");
            return;
        }

        // Load user from database to check tenant access
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            log.warn("User not found in database: {}", username);
            sendForbiddenResponse(response, "User not found");
            return;
        }

        // Validate tenant access
        if (!user.getTenantIds().contains(tenantId)) {
            log.warn("SECURITY: User {} attempted to access unauthorized tenant: {}. Authorized tenants: {}",
                username, tenantId, user.getTenantIds());

            sendForbiddenResponse(response,
                String.format("Access denied to tenant: %s. User does not have access to this tenant.", tenantId));
            return;
        }

        log.debug("Tenant access validated: user={}, tenant={}", username, tenantId);

        // Access granted, continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Check if the request path is public (doesn't require tenant validation)
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Send 403 Forbidden response with JSON error body
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"error\":\"Forbidden\",\"message\":\"%s\",\"status\":403}",
            message
        ));
        response.getWriter().flush();
    }
}
