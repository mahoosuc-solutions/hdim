package com.healthdata.fhir.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.healthdata.auth.context.AuthContext;
import com.healthdata.auth.context.ScopedTenant;
import com.healthdata.auth.model.AuthPrincipal;
import com.healthdata.auth.service.AuthenticationService;
import com.healthdata.auth.service.TenantResolver;

@ExtendWith(MockitoExtension.class)
class AuthContextFilterTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private TenantResolver tenantResolver;

    @Test
    void shouldPopulateAndClearAuthContext() throws ServletException, IOException {
        AuthContextFilter filter = new AuthContextFilter(authenticationService, tenantResolver);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer token");
        request.addHeader("X-Tenant-Id", "tenant-1");

        AuthPrincipal principal = new AuthPrincipal(
                UUID.randomUUID(),
                "user",
                "tenant-1",
                Set.of("ROLE_USER"),
                Map.of(),
                Instant.now());
        when(authenticationService.authenticate(any())).thenReturn(Optional.of(principal));
        when(tenantResolver.resolveTenant(any())).thenReturn(Optional.of("tenant-1"));

        AtomicReference<AuthPrincipal> capturedPrincipal = new AtomicReference<>();
        AtomicReference<String> capturedTenant = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            capturedPrincipal.set(AuthContext.currentPrincipal().orElse(null));
            capturedTenant.set(ScopedTenant.currentTenant().orElse(null));
        };

        filter.doFilter(request, response, chain);

        assertThat(capturedPrincipal.get()).isEqualTo(principal);
        assertThat(capturedTenant.get()).isEqualTo("tenant-1");
        assertThat(AuthContext.currentPrincipal()).isEmpty();
        assertThat(ScopedTenant.currentTenant()).isEmpty();
        verify(authenticationService).authenticate(any(AuthenticationService.RequestMetadata.class));
        verify(tenantResolver).resolveTenant(any(AuthenticationService.RequestMetadata.class));
    }

    @Test
    void shouldHandleMissingAuth() throws ServletException, IOException {
        AuthContextFilter filter = new AuthContextFilter(authenticationService, tenantResolver);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authenticationService.authenticate(any())).thenReturn(Optional.empty());
        when(tenantResolver.resolveTenant(any())).thenReturn(Optional.empty());

        AtomicReference<AuthPrincipal> capturedPrincipal = new AtomicReference<>();
        AtomicReference<String> capturedTenant = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            capturedPrincipal.set(AuthContext.currentPrincipal().orElse(null));
            capturedTenant.set(ScopedTenant.currentTenant().orElse(null));
        };

        filter.doFilter(request, response, chain);

        assertThat(capturedPrincipal.get()).isNull();
        assertThat(capturedTenant.get()).isNull();
        assertThat(AuthContext.currentPrincipal()).isEmpty();
        assertThat(ScopedTenant.currentTenant()).isEmpty();
    }
}
