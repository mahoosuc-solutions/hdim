package com.healthdata.eventstore.client.config;

import com.healthdata.eventstore.client.exception.EventStoreException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom error decoder for EventStoreClient.
 * Converts HTTP errors into domain-specific exceptions.
 */
@Slf4j
public class EventStoreErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();

        log.error("Event store request failed: method={}, status={}, reason={}",
            methodKey, status, response.reason());

        return switch (status) {
            case 400 -> new EventStoreException("Invalid event data: " + response.reason());
            case 404 -> new EventStoreException("Event not found: " + response.reason());
            case 409 -> new EventStoreException("Event version conflict: " + response.reason());
            case 500 -> new EventStoreException("Event store internal error: " + response.reason());
            case 503 -> new EventStoreException("Event store unavailable: " + response.reason());
            default -> new EventStoreException("Event store error: " + response.reason());
        };
    }
}
