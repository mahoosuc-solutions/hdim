package com.healthdata.fhir.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.healthdata.auth.context.AuthContext;
import com.healthdata.auth.context.ScopedTenant;
import com.healthdata.auth.service.AuthenticationService;
import com.healthdata.auth.service.AuthenticationService.RequestMetadata;
import com.healthdata.auth.service.TenantResolver;

@Component
public class AuthContextFilter extends OncePerRequestFilter {

    private final AuthenticationService authenticationService;
    private final TenantResolver tenantResolver;

    public AuthContextFilter(AuthenticationService authenticationService, TenantResolver tenantResolver) {
        this.authenticationService = authenticationService;
        this.tenantResolver = tenantResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        RequestMetadata metadata = new RequestMetadata(
                request.getHeader("Authorization"),
                request.getHeader("X-Tenant-Id"));

        try {
            authenticationService.authenticate(metadata).ifPresent(AuthContext::setPrincipal);
            tenantResolver.resolveTenant(metadata).ifPresent(ScopedTenant::setTenant);
            filterChain.doFilter(request, response);
        } finally {
            AuthContext.clear();
            ScopedTenant.clear();
        }
    }
}
