package com.healthdata.sales.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Exception thrown when an opportunity is not found
 */
public class OpportunityNotFoundException extends SalesException {

    public OpportunityNotFoundException(UUID id) {
        super("Opportunity not found: " + id, HttpStatus.NOT_FOUND, "OPPORTUNITY_NOT_FOUND");
    }
}
