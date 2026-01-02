package com.healthdata.shared.security.exception;

/**
 * Base JWT Exception - Parent class for all JWT-related exceptions
 *
 * Used for authentication and token validation errors.
 */
public class JwtException extends RuntimeException {

    public JwtException(String message) {
        super(message);
    }

    public JwtException(String message, Throwable cause) {
        super(message, cause);
    }
}
