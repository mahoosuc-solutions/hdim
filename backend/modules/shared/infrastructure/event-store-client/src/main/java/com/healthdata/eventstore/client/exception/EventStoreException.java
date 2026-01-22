package com.healthdata.eventstore.client.exception;

/**
 * Exception thrown when event store operations fail.
 */
public class EventStoreException extends RuntimeException {

    public EventStoreException(String message) {
        super(message);
    }

    public EventStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
