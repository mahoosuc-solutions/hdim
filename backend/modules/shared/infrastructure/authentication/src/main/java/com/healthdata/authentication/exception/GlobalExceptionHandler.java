package com.healthdata.authentication.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Legacy exception handler for authentication-related endpoints.
 *
 * @deprecated Use {@link HdimGlobalExceptionHandler} instead.
 *             This class is kept for backwards compatibility with existing code
 *             that may depend on the ErrorResponse inner class.
 *
 * The new HdimGlobalExceptionHandler provides:
 * - Handling for the full HdimException hierarchy
 * - Consistent error codes and correlation ID support
 * - Better structured error responses via HdimErrorResponse
 */
@Deprecated(since = "1.2.0", forRemoval = true)
public class GlobalExceptionHandler {

    /**
     * Standard error response structure.
     *
     * @deprecated Use {@link com.healthdata.common.exception.HdimErrorResponse} instead.
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private Map<String, String> fieldErrors;
        private Instant timestamp;
    }
}
