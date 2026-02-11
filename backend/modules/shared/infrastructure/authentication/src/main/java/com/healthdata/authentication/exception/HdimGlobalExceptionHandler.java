package com.healthdata.authentication.exception;

import com.healthdata.common.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the HDIM platform.
 *
 * This handler provides consistent error responses across all services by:
 * - Handling the custom HdimException hierarchy
 * - Handling Spring validation and security exceptions
 * - Providing structured error responses with correlation IDs
 * - Logging errors appropriately based on severity
 *
 * Services can extend this class or use it directly via component scanning.
 *
 * HIPAA Compliance:
 * - Error messages are generic and don't expose PHI
 * - Stack traces are logged but not returned to clients
 * - Correlation IDs enable secure log correlation
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HdimGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(HdimGlobalExceptionHandler.class);

    // ==================== HDIM Exception Handlers ====================

    /**
     * Handle all HdimException types (base handler).
     * More specific handlers below will take precedence.
     */
    @ExceptionHandler(HdimException.class)
    public ResponseEntity<HdimErrorResponse> handleHdimException(
            HdimException ex,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        int status = ex.getHttpStatus();

        // Log based on severity
        if (status >= 500) {
            log.error("System error [{}]: {} - {}", ex.getErrorCode(), ex.getMessage(), path, ex);
        } else if (status >= 400) {
            log.warn("Client error [{}]: {} - {}", ex.getErrorCode(), ex.getMessage(), path);
        }

        HdimErrorResponse response = HdimErrorResponse.fromException(ex, path);
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Handle business exceptions (400-409 range).
     */
    @ExceptionHandler(HdimBusinessException.class)
    public ResponseEntity<HdimErrorResponse> handleBusinessException(
            HdimBusinessException ex,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        log.warn("Business error [{}]: {} - {}", ex.getErrorCode(), ex.getMessage(), path);

        HdimErrorResponse response = HdimErrorResponse.fromException(ex, path);
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    /**
     * Handle security exceptions (401, 403).
     */
    @ExceptionHandler(HdimSecurityException.class)
    public ResponseEntity<HdimErrorResponse> handleSecurityException(
            HdimSecurityException ex,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        // Security errors logged at WARN level - don't log stack traces
        log.warn("Security error [{}]: {} - {}", ex.getErrorCode(), ex.getMessage(), path);

        HdimErrorResponse response = HdimErrorResponse.fromException(ex, path);
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    /**
     * Handle integration exceptions (502, 503, 504).
     */
    @ExceptionHandler(HdimIntegrationException.class)
    public ResponseEntity<HdimErrorResponse> handleIntegrationException(
            HdimIntegrationException ex,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        String serviceName = ex.getServiceName() != null ? ex.getServiceName() : "Unknown";
        log.error("Integration error [{}] with {}: {} - {}",
                ex.getErrorCode(), serviceName, ex.getMessage(), path, ex);

        HdimErrorResponse response = HdimErrorResponse.fromException(ex, path);
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    /**
     * Handle system exceptions (500 range).
     */
    @ExceptionHandler(HdimSystemException.class)
    public ResponseEntity<HdimErrorResponse> handleSystemException(
            HdimSystemException ex,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        log.error("System error [{}]: {} - {}", ex.getErrorCode(), ex.getMessage(), path, ex);

        HdimErrorResponse response = HdimErrorResponse.fromException(ex, path);
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    // ==================== Spring/Jakarta Exception Handlers ====================

    /**
     * Handle @Valid annotation validation failures.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<HdimErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        log.warn("Validation error at {}: {}", request.getRequestURI(), fieldErrors);

        HdimErrorResponse response = HdimErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .errorCode("HDIM-VAL-400")
                .message("Validation failed")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle @Validated constraint violations.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<HdimErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (v1, v2) -> v1 // Keep first if duplicate
                ));

        log.warn("Constraint violation at {}: {}", request.getRequestURI(), fieldErrors);

        HdimErrorResponse response = HdimErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .errorCode("HDIM-VAL-400")
                .message("Validation failed")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle missing request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<HdimErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        log.warn("Missing parameter at {}: {}", request.getRequestURI(), ex.getParameterName());

        HdimErrorResponse response = HdimErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .errorCode("HDIM-VAL-400")
                .message("Missing required parameter: " + ex.getParameterName())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle missing request headers.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<HdimErrorResponse> handleMissingHeader(
            MissingRequestHeaderException ex,
            HttpServletRequest request) {

        log.warn("Missing header at {}: {}", request.getRequestURI(), ex.getHeaderName());

        HdimErrorResponse response = HdimErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .errorCode("HDIM-VAL-400")
                .message("Missing required header: " + ex.getHeaderName())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle type mismatch (e.g., invalid UUID format).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<HdimErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String message = String.format("Invalid value for parameter '%s': expected %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "valid value");

        log.warn("Type mismatch at {}: {}", request.getRequestURI(), message);

        HdimErrorResponse response = HdimErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .errorCode("HDIM-VAL-400")
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle unsupported HTTP method (405).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<HdimErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        log.warn("Method not allowed at {}: {} method is not supported",
                request.getRequestURI(), ex.getMethod());

        HdimErrorResponse response = HdimErrorResponse.builder()
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error("Method Not Allowed")
                .errorCode("HDIM-HTTP-405")
                .message("HTTP method '" + ex.getMethod() + "' is not supported for this endpoint")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * Handle unacceptable media types (406).
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<HdimErrorResponse> handleNotAcceptable(
            HttpMediaTypeNotAcceptableException ex,
            HttpServletRequest request) {

        log.warn("Not acceptable at {}: {}", request.getRequestURI(), ex.getMessage());

        HdimErrorResponse response = HdimErrorResponse.builder()
                .status(HttpStatus.NOT_ACCEPTABLE.value())
                .error("Not Acceptable")
                .errorCode("HDIM-HTTP-406")
                .message("Requested media type is not acceptable")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(response);
    }

    /**
     * Handle missing handlers (404) from MVC routing.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<HdimErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        log.warn("No handler found for {} {}", ex.getHttpMethod(), ex.getRequestURL());

        HdimErrorResponse response = HdimErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .errorCode("HDIM-HTTP-404")
                .message("Requested resource not found")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle missing static resources (404).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<HdimErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        log.warn("No resource found at {}: {}", request.getRequestURI(), ex.getMessage());

        HdimErrorResponse response = HdimErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .errorCode("HDIM-HTTP-404")
                .message("Requested resource not found")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle illegal argument exceptions (400).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<HdimErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Illegal argument at {}: {}", request.getRequestURI(), ex.getMessage());

        HdimErrorResponse response = HdimErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .errorCode("HDIM-VAL-400")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle Spring Security access denied.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<HdimErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());

        HdimErrorResponse response = HdimErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .errorCode("HDIM-SEC-403")
                .message("You do not have permission to access this resource")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle Spring Security authentication exceptions.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<HdimErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {

        log.warn("Authentication failed at {}: {}", request.getRequestURI(), ex.getMessage());

        HdimErrorResponse response = HdimErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .errorCode("HDIM-SEC-401")
                .message("Authentication required")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle ResponseStatusException.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<HdimErrorResponse> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        log.warn("ResponseStatusException at {}: {} - {}",
                request.getRequestURI(), status, ex.getReason());

        HdimErrorResponse response = HdimErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .errorCode("HDIM-" + status.value())
                .message(ex.getReason())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Catch-all handler for unexpected exceptions.
     * SECURITY: Never expose internal error details to clients.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<HdimErrorResponse> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request) {

        // Log full stack trace for debugging
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        // Return generic error message - don't expose internals
        HdimErrorResponse response = HdimErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .errorCode("HDIM-SYS-500")
                .message("An unexpected error occurred. Please try again later.")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
