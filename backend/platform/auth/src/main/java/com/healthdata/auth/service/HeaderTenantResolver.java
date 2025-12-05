package com.healthdata.auth.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Resolves tenant information from the `X-Tenant-Id` header if present.
 */
@Component
public class HeaderTenantResolver implements TenantResolver {

    private static final String DEFAULT_TENANT = "tenant-1";

    @Override
    public Optional<String> resolveTenant(AuthenticationService.RequestMetadata requestMetadata) {
        String tenant = requestMetadata.tenantHeader();
        if (tenant == null || tenant.isBlank()) {
            return Optional.of(DEFAULT_TENANT);
        }
        return Optional.of(tenant);
    }
}
