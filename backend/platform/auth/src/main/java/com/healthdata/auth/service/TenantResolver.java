package com.healthdata.auth.service;

import java.util.Optional;

/**
 * Resolves tenant identifiers from request metadata.
 */
public interface TenantResolver {

    Optional<String> resolveTenant(AuthenticationService.RequestMetadata requestMetadata);
}
