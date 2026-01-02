package com.healthdata.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error response structure for the HDIM platform.
 *
 * This DTO provides a consistent error response format across all services,
 * enabling:
 * - Machine-readable error codes for client handling
 * - Human-readable messages for debugging
 * - Correlation IDs for distributed tracing
 * - Field-level validation errors
 *
 * HIPAA Compliance:
 * - Never include PHI (Protected Health Information) in error messages
 * - Error details should be generic enough to not expose system internals
 * - Correlation IDs enable secure log correlation without exposing data
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HdimErrorResponse {

    private final int status;
    private final String error;
    private final String errorCode;
    private final String message;
    private final String correlationId;
    private final String path;
    private final Instant timestamp;
    private final Map<String, String> fieldErrors;

    private HdimErrorResponse(Builder builder) {
        this.status = builder.status;
        this.error = builder.error;
        this.errorCode = builder.errorCode;
        this.message = builder.message;
        this.correlationId = builder.correlationId;
        this.path = builder.path;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.fieldErrors = builder.fieldErrors;
    }

    // Getters
    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getPath() {
        return path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int status;
        private String error;
        private String errorCode;
        private String message;
        private String correlationId;
        private String path;
        private Instant timestamp;
        private Map<String, String> fieldErrors;

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder fieldErrors(Map<String, String> fieldErrors) {
            this.fieldErrors = fieldErrors;
            return this;
        }

        public HdimErrorResponse build() {
            return new HdimErrorResponse(this);
        }
    }

    /**
     * Create an error response from an HdimException.
     *
     * @param exception The HDIM exception
     * @param path      The request path
     * @return Configured error response
     */
    public static HdimErrorResponse fromException(HdimException exception, String path) {
        return builder()
                .status(exception.getHttpStatus())
                .error(getErrorName(exception.getHttpStatus()))
                .errorCode(exception.getErrorCode())
                .message(exception.getMessage())
                .correlationId(exception.getCorrelationId())
                .path(path)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Get the standard HTTP error name for a status code.
     */
    private static String getErrorName(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 422 -> "Unprocessable Entity";
            case 429 -> "Too Many Requests";
            case 499 -> "Client Closed Request";
            case 500 -> "Internal Server Error";
            case 501 -> "Not Implemented";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default -> "Error";
        };
    }
}
