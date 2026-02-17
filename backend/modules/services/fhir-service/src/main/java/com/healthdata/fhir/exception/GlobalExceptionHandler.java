package com.healthdata.fhir.exception;

import com.healthdata.fhir.security.smart.SmartAuthorizationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for FHIR Service.
 * HIPAA-compliant: All error messages are sanitized to prevent PHI exposure in responses.
 * Handles SMART on FHIR authorization failures with RFC 6750 compliant error bodies.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(SmartAuthorizationException.class)
    public ResponseEntity<Map<String, String>> handleSmartAuthorization(
            SmartAuthorizationException ex,
            HttpServletRequest request) {

        log.warn("SMART authorization failure on {}: {} - {}", request.getRequestURI(),
                ex.getErrorCode(), ex.getErrorDescription());

        // RFC 6750 §3.1 error response format
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "error", ex.getErrorCode(),
                        "error_description", ex.getErrorDescription()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::convertFieldError)
                .collect(Collectors.toList());

        log.warn("Validation error on {}: {} field errors", request.getRequestURI(), fieldErrors.size());

        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message("Request validation failed")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(
            MissingRequestHeaderException ex,
            HttpServletRequest request) {

        log.warn("Missing required header {} on {}", ex.getHeaderName(), request.getRequestURI());

        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message("Required header missing: " + ex.getHeaderName())
                .path(request.getRequestURI())
                .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("FORBIDDEN")
                .message("Access denied to requested resource")
                .path(request.getRequestURI())
                .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Illegal argument on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message(sanitizeMessage(ex.getMessage()))
                .path(request.getRequestURI())
                .build());
    }

    /**
     * Catch-all handler — prevents PHI, stack traces, and internal details from
     * reaching the client (HIPAA §164.312(b) audit control requirement).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Internal error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_ERROR")
                .message("An internal error occurred. Please contact support.")
                .path(request.getRequestURI())
                .build());
    }

    private ErrorResponse.FieldError convertFieldError(FieldError fieldError) {
        return ErrorResponse.FieldError.builder()
                .field(fieldError.getField())
                .rejectedValue(sanitizeValue(fieldError.getRejectedValue()))
                .message(fieldError.getDefaultMessage())
                .build();
    }

    private String sanitizeValue(Object value) {
        if (value == null) return "null";
        String s = String.valueOf(value);
        return s.length() > 50 ? "[value too long]" : s;
    }

    private String sanitizeMessage(String message) {
        if (message == null) return "Invalid request";
        return message
                .replaceAll("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b", "[email]")
                .replaceAll("\\b\\d{3}-\\d{2}-\\d{4}\\b", "[ssn]")
                .replaceAll("\\b\\d{3}-\\d{3}-\\d{4}\\b", "[phone]");
    }
}
