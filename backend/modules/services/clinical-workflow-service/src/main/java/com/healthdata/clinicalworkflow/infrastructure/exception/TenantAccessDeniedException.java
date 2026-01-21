package com.healthdata.clinicalworkflow.infrastructure.exception;

/**
 * Exception thrown when tenant isolation is violated
 * HIPAA-critical: Prevents cross-tenant data access
 */
public class TenantAccessDeniedException extends RuntimeException {

    public TenantAccessDeniedException(String message) {
        super(message);
    }

    public TenantAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
