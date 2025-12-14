package com.healthdata.agentbuilder.controller;

import com.healthdata.agentbuilder.service.AgentConfigurationService.AgentBuilderException;
import com.healthdata.agentbuilder.service.AgentConfigurationService.AgentNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Global exception handler for the Agent Builder service.
 */
@Slf4j
@RestControllerAdvice("com.healthdata.agentbuilder")
public class AgentBuilderExceptionHandler {

    @ExceptionHandler(AgentNotFoundException.class)
    public ProblemDetail handleAgentNotFound(AgentNotFoundException ex) {
        log.warn("Agent not found: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        problem.setTitle("Agent Not Found");
        problem.setType(URI.create("https://hdim.healthdata.com/problems/agent-not-found"));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(AgentBuilderException.class)
    public ProblemDetail handleAgentBuilderException(AgentBuilderException ex) {
        log.warn("Agent builder error: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problem.setTitle("Agent Builder Error");
        problem.setType(URI.create("https://hdim.healthdata.com/problems/agent-builder-error"));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed"
        );
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://hdim.healthdata.com/problems/validation-error"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
            .map(e -> new ValidationError(e.getField(), e.getDefaultMessage()))
            .toList()
        );

        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problem.setTitle("Invalid Request");
        problem.setType(URI.create("https://hdim.healthdata.com/problems/invalid-request"));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"
        );
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://hdim.healthdata.com/problems/internal-error"));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    private record ValidationError(String field, String message) {}
}
