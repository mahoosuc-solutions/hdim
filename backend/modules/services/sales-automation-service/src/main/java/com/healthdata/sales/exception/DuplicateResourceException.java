package com.healthdata.sales.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting to create a duplicate resource
 */
public class DuplicateResourceException extends SalesException {

    public DuplicateResourceException(String resourceType, String identifier) {
        super(
            String.format("%s already exists: %s", resourceType, identifier),
            HttpStatus.CONFLICT,
            "DUPLICATE_RESOURCE"
        );
    }

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
    }
}
