package com.healthdata.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when request violates multi-tenant isolation
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class TenantAccessDeniedException extends RuntimeException {
    public TenantAccessDeniedException(String message) {
        super(message);
    }

    public TenantAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
