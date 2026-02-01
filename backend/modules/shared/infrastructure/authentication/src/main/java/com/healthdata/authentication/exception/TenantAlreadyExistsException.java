package com.healthdata.authentication.exception;

/**
 * Exception thrown when attempting to create a tenant that already exists.
 */
public class TenantAlreadyExistsException extends RuntimeException {

    public TenantAlreadyExistsException(String tenantId) {
        super("Tenant already exists: " + tenantId);
    }

    public TenantAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
