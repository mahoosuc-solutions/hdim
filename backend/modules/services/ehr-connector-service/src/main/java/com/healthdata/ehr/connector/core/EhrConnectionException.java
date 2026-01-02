package com.healthdata.ehr.connector.core;

/**
 * Exception thrown when EHR connection operations fail.
 */
public class EhrConnectionException extends RuntimeException {

    private final String ehrSystem;
    private final int statusCode;

    public EhrConnectionException(String message, String ehrSystem) {
        super(message);
        this.ehrSystem = ehrSystem;
        this.statusCode = -1;
    }

    public EhrConnectionException(String message, String ehrSystem, Throwable cause) {
        super(message, cause);
        this.ehrSystem = ehrSystem;
        this.statusCode = -1;
    }

    public EhrConnectionException(String message, String ehrSystem, int statusCode) {
        super(message);
        this.ehrSystem = ehrSystem;
        this.statusCode = statusCode;
    }

    public EhrConnectionException(String message, String ehrSystem, int statusCode, Throwable cause) {
        super(message, cause);
        this.ehrSystem = ehrSystem;
        this.statusCode = statusCode;
    }

    public String getEhrSystem() {
        return ehrSystem;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
