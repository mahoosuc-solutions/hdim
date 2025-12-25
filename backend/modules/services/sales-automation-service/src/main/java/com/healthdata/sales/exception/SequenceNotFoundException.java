package com.healthdata.sales.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Exception thrown when an email sequence is not found
 */
public class SequenceNotFoundException extends SalesException {

    public SequenceNotFoundException(UUID id) {
        super("Email sequence not found: " + id, HttpStatus.NOT_FOUND, "SEQUENCE_NOT_FOUND");
    }

    public SequenceNotFoundException(String name) {
        super("Email sequence not found with name: " + name, HttpStatus.NOT_FOUND, "SEQUENCE_NOT_FOUND");
    }
}
