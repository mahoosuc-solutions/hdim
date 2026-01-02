package com.healthdata.sales.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Exception thrown when a lead is not found
 */
public class LeadNotFoundException extends SalesException {

    public LeadNotFoundException(UUID id) {
        super("Lead not found: " + id, HttpStatus.NOT_FOUND, "LEAD_NOT_FOUND");
    }

    public LeadNotFoundException(String email) {
        super("Lead not found with email: " + email, HttpStatus.NOT_FOUND, "LEAD_NOT_FOUND");
    }
}
