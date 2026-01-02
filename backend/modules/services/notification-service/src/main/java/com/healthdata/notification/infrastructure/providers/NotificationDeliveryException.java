package com.healthdata.notification.infrastructure.providers;

/**
 * Exception thrown when notification delivery fails.
 */
public class NotificationDeliveryException extends RuntimeException {

    private final boolean retryable;

    public NotificationDeliveryException(String message) {
        super(message);
        this.retryable = true;
    }

    public NotificationDeliveryException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public NotificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
        this.retryable = true;
    }

    public NotificationDeliveryException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
