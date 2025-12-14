package com.healthdata.fhir.security.smart;

/**
 * Exception thrown when SMART on FHIR authorization fails.
 */
public class SmartAuthorizationException extends RuntimeException {

    private final String errorCode;
    private final String errorDescription;

    public SmartAuthorizationException(String message) {
        super(message);
        this.errorCode = "invalid_request";
        this.errorDescription = message;
    }

    public SmartAuthorizationException(String errorCode, String errorDescription) {
        super(errorDescription);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public SmartAuthorizationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "server_error";
        this.errorDescription = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }
}
