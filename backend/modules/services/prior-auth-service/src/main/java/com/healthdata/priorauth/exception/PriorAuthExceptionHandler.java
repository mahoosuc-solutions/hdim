package com.healthdata.priorauth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for Prior Authorization service.
 * Provides consistent error responses for PA-specific operations.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.healthdata.priorauth")
public class PriorAuthExceptionHandler {

    /**
     * Handle IllegalArgumentException for not-found resources.
     * Returns 404 Not Found.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        String message = ex.getMessage();

        // Check if this is a "not found" type error
        if (message != null && (message.contains("not found") || message.contains("Not found"))) {
            log.warn("Resource not found: {}", message);

            ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(message)
                .timestamp(Instant.now())
                .build();

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // Otherwise it's a bad request
        log.warn("Bad request: {}", message);

        ErrorResponse response = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message(message)
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle validation errors from @Valid annotations.
     * Returns 400 Bad Request with field-specific error messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        log.warn("Validation error: {}", fieldErrors);

        ErrorResponse response = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Input validation failed. Please check your request.")
            .fieldErrors(fieldErrors)
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle unexpected exceptions.
     * Returns 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error in prior auth service", ex);

        ErrorResponse response = ErrorResponse.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred. Please try again later.")
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Standard error response structure.
     */
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
