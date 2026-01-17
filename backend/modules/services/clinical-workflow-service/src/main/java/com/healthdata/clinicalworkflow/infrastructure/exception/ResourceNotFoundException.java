package com.healthdata.clinicalworkflow.infrastructure.exception;

/**
 * Exception thrown when a requested resource cannot be found
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s not found with id: %s", resourceType, resourceId));
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
