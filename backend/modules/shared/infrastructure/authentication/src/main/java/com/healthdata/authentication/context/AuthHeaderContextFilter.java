package com.healthdata.authentication.context;

import com.healthdata.authentication.constants.AuthHeaderConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that captures authentication headers and stores them in AuthHeaderContext.
 *
 * This filter must run early in the filter chain (before security filters) to ensure
 * that auth headers are available in the InheritableThreadLocal context for any
 * downstream processing, including Feign client calls.
 *
 * The filter:
 * 1. Extracts all X-Auth-* headers from the incoming request
 * 2. Also extracts X-Tenant-ID header
 * 3. Stores them in AuthHeaderContext (InheritableThreadLocal)
 * 4. Clears the context after request processing to prevent memory leaks
 *
 * This enables service-to-service calls (via Feign) to forward auth headers even
 * when executing in different threads.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // Run very early, but after basic servlet filters
public class AuthHeaderContextFilter extends OncePerRequestFilter {

    private static final String TENANT_ID_HEADER = "X-Tenant-ID";

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Capture all auth headers into context
            captureAuthHeaders(request);

            // Continue filter chain
            filterChain.doFilter(request, response);

        } finally {
            // Always clear context after request processing
            AuthHeaderContext.clear();
        }
    }

    /**
     * Capture authentication headers from the request into AuthHeaderContext.
     */
    private void captureAuthHeaders(HttpServletRequest request) {
        int headerCount = 0;

        // Capture all standard auth headers
        for (String headerName : AuthHeaderConstants.getAllAuthHeaders()) {
            String headerValue = request.getHeader(headerName);
            if (headerValue != null && !headerValue.isBlank()) {
                AuthHeaderContext.setHeader(headerName, headerValue);
                headerCount++;
            }
        }

        // Also capture X-Tenant-ID
        String tenantId = request.getHeader(TENANT_ID_HEADER);
        if (tenantId != null && !tenantId.isBlank()) {
            AuthHeaderContext.setHeader(TENANT_ID_HEADER, tenantId);
            headerCount++;
        }

        if (headerCount > 0) {
            log.debug("AuthHeaderContextFilter: captured {} auth headers for request to {}",
                headerCount, request.getRequestURI());
        } else {
            log.trace("AuthHeaderContextFilter: no auth headers found for request to {}",
                request.getRequestURI());
        }
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Skip for health checks and static resources to reduce overhead
        return path.equals("/favicon.ico");
    }
}
