package com.healthdata.auth.context;

import java.util.Optional;

/**
 * Convenience accessor for tenant scoping reused across services.
 */
public final class ScopedTenant {

    private static final ThreadLocal<String> TENANT = new InheritableThreadLocal<>();

    private ScopedTenant() {
    }

    public static void setTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            TENANT.remove();
        } else {
            TENANT.set(tenantId);
        }
    }

    public static Optional<String> currentTenant() {
        return Optional.ofNullable(TENANT.get());
    }

    public static void clear() {
        TENANT.remove();
    }
}
