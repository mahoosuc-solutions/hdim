package com.healthdata.eventreplay.projection;

/**
 * Exception hierarchy for event replay and projection operations
 */

public class OptimisticLockException extends RuntimeException {
    public OptimisticLockException(String message) {
        super(message);
    }

    public OptimisticLockException(String message, Throwable cause) {
        super(message, cause);
    }
}

class EventApplyException extends RuntimeException {
    public EventApplyException(String message) {
        super(message);
    }

    public EventApplyException(String message, Throwable cause) {
        super(message, cause);
    }
}

class ProjectionException extends RuntimeException {
    public ProjectionException(String message) {
        super(message);
    }

    public ProjectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
