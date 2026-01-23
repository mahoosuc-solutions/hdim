package com.healthdata.authentication.exception;

/**
 * Exception thrown when a tenant is not found.
 */
public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(String tenantId) {
        super("Tenant not found: " + tenantId);
    }

    public TenantNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
