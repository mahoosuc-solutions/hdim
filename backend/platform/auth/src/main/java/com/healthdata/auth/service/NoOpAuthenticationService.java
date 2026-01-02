package com.healthdata.auth.service;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.healthdata.auth.model.AuthPrincipal;

/**
 * Default implementation for environments where authentication is not yet enabled.
 * Produces a synthetic principal so downstream code can rely on non-null context.
 */
@Component
public class NoOpAuthenticationService implements AuthenticationService {

    @Override
    public Optional<AuthPrincipal> authenticate(RequestMetadata requestMetadata) {
        return Optional.of(new AuthPrincipal(
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                "anonymous",
                requestMetadata.tenantHeader(),
                Set.of("ROLE_ANONYMOUS"),
                null,
                Instant.now()));
    }
}
