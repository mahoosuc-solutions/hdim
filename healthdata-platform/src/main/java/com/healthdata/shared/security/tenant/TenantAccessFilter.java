package com.healthdata.shared.security.tenant;

import com.healthdata.shared.security.jwt.JwtAuthenticationDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

/**
 * Enforces tenant isolation for requests that specify a tenant ID.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantAccessFilter extends OncePerRequestFilter {

    private static final String TENANT_PARAM = "tenantId";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    private static final List<String> PUBLIC_PATHS = List.of(
            "/actuator/health",
            "/api/auth",
            "/swagger-ui",
            "/v3/api-docs",
            "/api-docs"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        if (isPublicPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestedTenantId = resolveTenantId(request);
        if (requestedTenantId == null || requestedTenantId.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        String tokenTenantId = null;
        Object details = authentication.getDetails();
        if (details instanceof JwtAuthenticationDetails jwtDetails) {
            tokenTenantId = jwtDetails.getTenantId();
        }

        if (tokenTenantId == null || tokenTenantId.isBlank()) {
            log.warn("Tenant ID missing in token for user {} on path {}", authentication.getName(), requestPath);
            sendForbiddenResponse(response, "Tenant context missing from token");
            return;
        }

        if (!tokenTenantId.equals(requestedTenantId)) {
            log.warn("Tenant mismatch for user {}: requested {}, token {}", authentication.getName(),
                    requestedTenantId, tokenTenantId);
            sendForbiddenResponse(response, "Tenant access denied");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveTenantId(HttpServletRequest request) {
        String paramTenantId = request.getParameter(TENANT_PARAM);
        if (paramTenantId != null && !paramTenantId.isBlank()) {
            return paramTenantId;
        }
        return request.getHeader(TENANT_HEADER);
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"status\":\"error\",\"message\":\"%s\"}", message));
    }
}
