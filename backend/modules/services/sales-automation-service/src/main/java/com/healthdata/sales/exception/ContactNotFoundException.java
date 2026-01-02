package com.healthdata.sales.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Exception thrown when a contact is not found
 */
public class ContactNotFoundException extends SalesException {

    public ContactNotFoundException(UUID id) {
        super("Contact not found: " + id, HttpStatus.NOT_FOUND, "CONTACT_NOT_FOUND");
    }

    public ContactNotFoundException(String email) {
        super("Contact not found with email: " + email, HttpStatus.NOT_FOUND, "CONTACT_NOT_FOUND");
    }
}
