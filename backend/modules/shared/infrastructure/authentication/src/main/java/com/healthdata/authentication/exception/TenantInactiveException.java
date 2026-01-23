package com.healthdata.authentication.exception;

/**
 * Exception thrown when attempting to use an inactive or suspended tenant.
 */
public class TenantInactiveException extends RuntimeException {

    public TenantInactiveException(String tenantId) {
        super("Tenant is not active: " + tenantId);
    }

    public TenantInactiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
