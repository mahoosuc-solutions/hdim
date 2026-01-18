package com.healthdata.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when refresh token has been revoked
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class RevokedTokenException extends RuntimeException {
    public RevokedTokenException(String message) {
        super(message);
    }

    public RevokedTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
