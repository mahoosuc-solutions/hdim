package com.healthdata.clinicalworkflow.infrastructure.exception;

/**
 * Exception thrown when business rules are violated
 * Examples: Duplicate check-in, room already occupied, invalid workflow state
 */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }

    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
