package com.healthdata.sales.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all sales-related errors
 */
public class SalesException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public SalesException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
        this.errorCode = "SALES_ERROR";
    }

    public SalesException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorCode = "SALES_ERROR";
    }

    public SalesException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public SalesException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
        this.errorCode = "SALES_ERROR";
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
