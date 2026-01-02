package com.healthdata.patient.service;

/**
 * Exception thrown when a patient is not found
 * This is a runtime exception for unchecked exception handling
 */
public class PatientNotFoundException extends RuntimeException {

    /**
     * Constructor with message
     *
     * @param message Error message
     */
    public PatientNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor with message and cause
     *
     * @param message Error message
     * @param cause The cause of the exception
     */
    public PatientNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with cause
     *
     * @param cause The cause of the exception
     */
    public PatientNotFoundException(Throwable cause) {
        super(cause);
    }
}
