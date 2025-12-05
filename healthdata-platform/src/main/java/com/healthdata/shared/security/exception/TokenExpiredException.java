package com.healthdata.shared.security.exception;

/**
 * Token Expired Exception - Thrown when JWT token has expired
 *
 * User should refresh token using refresh endpoint
 */
public class TokenExpiredException extends JwtException {

    public TokenExpiredException(String message) {
        super(message);
    }

    public TokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
