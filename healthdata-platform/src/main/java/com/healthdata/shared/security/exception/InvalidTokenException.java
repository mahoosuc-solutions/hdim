package com.healthdata.shared.security.exception;

/**
 * Invalid Token Exception - Thrown when JWT token is invalid
 *
 * Reasons: malformed, invalid signature, wrong format
 */
public class InvalidTokenException extends JwtException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
