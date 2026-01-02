package com.healthdata.sales.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Exception thrown when a sequence enrollment is not found
 */
public class EnrollmentNotFoundException extends SalesException {

    public EnrollmentNotFoundException(UUID id) {
        super("Enrollment not found: " + id, HttpStatus.NOT_FOUND, "ENROLLMENT_NOT_FOUND");
    }
}
