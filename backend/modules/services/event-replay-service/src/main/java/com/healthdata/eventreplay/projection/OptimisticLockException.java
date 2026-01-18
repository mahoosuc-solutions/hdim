package com.healthdata.eventreplay.projection;

/**
 * OptimisticLockException - Raised when projection version conflict occurs
 *
 * Indicates that the projection version changed between read and write,
 * suggesting concurrent modification.
 */
public class OptimisticLockException extends RuntimeException {

    public OptimisticLockException(String message) {
        super(message);
    }

    public OptimisticLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
