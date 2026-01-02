package com.healthdata.sales.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Exception thrown when an activity is not found
 */
public class ActivityNotFoundException extends SalesException {

    public ActivityNotFoundException(UUID id) {
        super("Activity not found: " + id, HttpStatus.NOT_FOUND, "ACTIVITY_NOT_FOUND");
    }
}
