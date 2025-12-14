package com.healthdata.persistence.tenant;

/**
 * Thread-local storage for tenant context.
 *
 * This class provides a mechanism to store and retrieve the current tenant ID
 * in a thread-safe manner. It is used in conjunction with Row-Level Security (RLS)
 * to ensure tenant isolation at the database level.
 *
 * Usage:
 * - Set tenant at the beginning of a request (e.g., in a filter or interceptor)
 * - Clear tenant at the end of the request to prevent tenant leakage
 *
 * @see TenantConnectionPreparer
 * @see TenantFilter
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SYSTEM_MODE = ThreadLocal.withInitial(() -> false);

    private TenantContext() {
        // Utility class
    }

    /**
     * Get the current tenant ID.
     *
     * @return the current tenant ID, or null if not set
     */
    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    /**
     * Set the current tenant ID.
     *
     * @param tenantId the tenant ID to set
     */
    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Clear the current tenant ID.
     * Should be called at the end of request processing to prevent tenant leakage.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
        SYSTEM_MODE.remove();
    }

    /**
     * Check if a tenant is currently set.
     *
     * @return true if a tenant is set, false otherwise
     */
    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }

    /**
     * Enable system mode for cross-tenant operations.
     * When in system mode, RLS policies are bypassed.
     *
     * Use with caution - only for background jobs and system operations.
     */
    public static void enableSystemMode() {
        SYSTEM_MODE.set(true);
    }

    /**
     * Disable system mode.
     */
    public static void disableSystemMode() {
        SYSTEM_MODE.set(false);
    }

    /**
     * Check if system mode is enabled.
     *
     * @return true if system mode is enabled, false otherwise
     */
    public static boolean isSystemMode() {
        return Boolean.TRUE.equals(SYSTEM_MODE.get());
    }

    /**
     * Execute a runnable in system mode (bypassing RLS).
     *
     * @param runnable the operation to execute
     */
    public static void runInSystemMode(Runnable runnable) {
        boolean wasSystemMode = isSystemMode();
        try {
            enableSystemMode();
            runnable.run();
        } finally {
            if (!wasSystemMode) {
                disableSystemMode();
            }
        }
    }
}
