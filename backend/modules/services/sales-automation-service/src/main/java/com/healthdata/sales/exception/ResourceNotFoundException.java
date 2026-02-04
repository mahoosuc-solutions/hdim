package com.healthdata.sales.exception;

import org.springframework.http.HttpStatus;

/**
 * Generic exception thrown when a resource is not found
 */
public class ResourceNotFoundException extends SalesException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceType, Object id) {
        super(resourceType + " not found: " + id, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
