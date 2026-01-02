package com.healthdata.cql.exception;

import com.healthdata.common.exception.HdimErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exception handler for CQL Engine Service.
 *
 * This handler provides CQL-specific exception handling while delegating
 * common exception types to {@link com.healthdata.authentication.exception.HdimGlobalExceptionHandler}.
 *
 * CQL-specific exceptions handled:
 * - EntityNotFoundException (JPA) - mapped to 404
 * - IllegalArgumentException - mapped to 400
 *
 * All other exceptions (validation, security, etc.) are handled by the global handler.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.healthdata.cql")
public class CqlExceptionHandler {

    /**
     * Handle JPA EntityNotFoundException - returns 404 Not Found.
     * This catches cases where JPA can't find an entity by ID.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<HdimErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Entity not found at {}: {}", request.getRequestURI(), ex.getMessage());

        HdimErrorResponse response = HdimErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .errorCode("HDIM-CQL-404")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle IllegalArgumentException - returns 400 Bad Request.
     * This catches validation errors thrown by service layer code.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<HdimErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Invalid argument at {}: {}", request.getRequestURI(), ex.getMessage());

        HdimErrorResponse response = HdimErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .errorCode("HDIM-CQL-400")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle @Validated constraint violations - returns 400 Bad Request.
     * This catches Bean Validation constraint violations.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<HdimErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (v1, v2) -> v1
                ));

        log.warn("Constraint violation at {}: {}", request.getRequestURI(), fieldErrors);

        String message = fieldErrors.values().stream()
                .findFirst()
                .orElse("Validation failed");

        HdimErrorResponse response = HdimErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .errorCode("HDIM-CQL-VAL-400")
                .message(message)
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }
}
