package com.healthdata.auth.service;

import java.util.Optional;

import com.healthdata.auth.model.AuthPrincipal;

/**
 * Contract for validating inbound requests and producing an {@link AuthPrincipal}.
 * Implementations may perform JWT validation, OAuth token introspection, etc.
 */
public interface AuthenticationService {

    Optional<AuthPrincipal> authenticate(RequestMetadata requestMetadata);

    record RequestMetadata(String authorizationHeader, String tenantHeader) {
    }
}
