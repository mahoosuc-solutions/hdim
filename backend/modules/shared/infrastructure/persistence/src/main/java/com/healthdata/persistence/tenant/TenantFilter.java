package com.healthdata.persistence.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that extracts tenant ID from request headers and sets it in TenantContext.
 *
 * This filter should be configured early in the filter chain to ensure tenant
 * context is available for all subsequent processing including database operations.
 *
 * The tenant ID is expected in the 'X-Tenant-ID' header. If not present, the
 * request will proceed but RLS policies will block access to tenant-scoped data.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-ID";
    public static final String TENANT_ATTRIBUTE = "tenantId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String tenantId = extractTenantId(request);

            if (tenantId != null) {
                TenantContext.setCurrentTenant(tenantId);
                request.setAttribute(TENANT_ATTRIBUTE, tenantId);
                log.debug("Set tenant context from header: {}", tenantId);
            } else {
                log.debug("No tenant ID in request - RLS will enforce access control");
            }

            filterChain.doFilter(request, response);
        } finally {
            // Always clear tenant context to prevent leakage between requests
            TenantContext.clear();
        }
    }

    private String extractTenantId(HttpServletRequest request) {
        // Try header first
        String tenantId = request.getHeader(TENANT_HEADER);

        if (tenantId == null || tenantId.isBlank()) {
            // Try lowercase header variant
            tenantId = request.getHeader(TENANT_HEADER.toLowerCase());
        }

        if (tenantId == null || tenantId.isBlank()) {
            // Try query parameter as fallback (useful for WebSocket connections)
            tenantId = request.getParameter("tenantId");
        }

        return tenantId != null && !tenantId.isBlank() ? tenantId.trim() : null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Skip tenant filter for health checks and public endpoints
        return path.startsWith("/actuator/")
                || path.equals("/health")
                || path.equals("/ready")
                || path.equals("/live")
                || path.startsWith("/api-docs")
                || path.startsWith("/swagger")
                || path.equals("/favicon.ico");
    }
}
