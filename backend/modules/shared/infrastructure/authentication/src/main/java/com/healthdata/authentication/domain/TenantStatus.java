package com.healthdata.authentication.domain;

/**
 * Tenant status enumeration for lifecycle management.
 *
 * Status Lifecycle:
 * 1. ACTIVE - Tenant is operational and users can access services
 * 2. SUSPENDED - Temporary suspension (e.g., payment issues, policy violations)
 * 3. INACTIVE - Permanently deactivated (e.g., contract ended, deletion requested)
 */
public enum TenantStatus {
    /**
     * Active tenant.
     * - Users can login and access all services
     * - All operations permitted
     * - Default status for new tenants
     */
    ACTIVE("Active", "Tenant is operational"),

    /**
     * Suspended tenant.
     * - Temporary suspension (payment, compliance, security issues)
     * - Users cannot login
     * - Data preserved but inaccessible
     * - Can be reactivated
     */
    SUSPENDED("Suspended", "Temporarily suspended"),

    /**
     * Inactive tenant.
     * - Permanently deactivated
     * - Users cannot login
     * - Data may be scheduled for deletion
     * - Cannot be reactivated (must create new tenant)
     */
    INACTIVE("Inactive", "Permanently deactivated");

    private final String displayName;
    private final String description;

    TenantStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if tenant status allows user operations.
     */
    public boolean isOperational() {
        return this == ACTIVE;
    }

    /**
     * Check if tenant can be reactivated from current status.
     */
    public boolean canBeReactivated() {
        return this == SUSPENDED;
    }
}
