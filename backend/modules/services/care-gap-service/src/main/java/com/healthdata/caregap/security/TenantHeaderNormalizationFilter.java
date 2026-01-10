package com.healthdata.caregap.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Ensures X-Tenant-ID is present by falling back to X-Auth-Tenant-Ids.
 * This keeps demo/gateway-authenticated requests aligned with tenant context.
 */
public class TenantHeaderNormalizationFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String AUTH_TENANTS_HEADER = "X-Auth-Tenant-Ids";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId != null && !tenantId.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String authTenantIds = request.getHeader(AUTH_TENANTS_HEADER);
        if (authTenantIds == null || authTenantIds.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String primaryTenant = authTenantIds.split(",", 2)[0].trim();
        if (primaryTenant.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if (TENANT_HEADER.equalsIgnoreCase(name)) {
                    return primaryTenant;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (TENANT_HEADER.equalsIgnoreCase(name)) {
                    return Collections.enumeration(List.of(primaryTenant));
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                Set<String> names = new LinkedHashSet<>();
                Enumeration<String> originalNames = super.getHeaderNames();
                while (originalNames.hasMoreElements()) {
                    names.add(originalNames.nextElement());
                }
                names.add(TENANT_HEADER);
                return Collections.enumeration(names);
            }
        };

        filterChain.doFilter(wrapped, response);
    }
}
