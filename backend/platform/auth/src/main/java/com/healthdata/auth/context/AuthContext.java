package com.healthdata.auth.context;

import java.util.Optional;

import com.healthdata.auth.model.AuthPrincipal;

/**
 * Thread-local holder for authenticated principal information.
 * This provides a stable API for services to retrieve user/tenant context
 * without binding to a concrete security implementation.
 */
public final class AuthContext {

    private static final ThreadLocal<AuthPrincipal> CURRENT = new InheritableThreadLocal<>();

    private AuthContext() {
    }

    public static void setPrincipal(AuthPrincipal principal) {
        if (principal == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(principal);
        }
    }

    public static Optional<AuthPrincipal> currentPrincipal() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
