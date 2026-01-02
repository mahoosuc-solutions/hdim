package com.healthdata.ehr.exception;

/**
 * Exception thrown when an unsupported EHR vendor type is encountered.
 */
public class UnsupportedVendorException extends RuntimeException {

    public UnsupportedVendorException(String message) {
        super(message);
    }

    public UnsupportedVendorException(String message, Throwable cause) {
        super(message, cause);
    }
}
