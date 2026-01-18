package com.healthdata.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when refresh token has expired
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class ExpiredTokenException extends RuntimeException {
    public ExpiredTokenException(String message) {
        super(message);
    }

    public ExpiredTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
