package com.healthdata.sales.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Exception thrown when an account is not found
 */
public class AccountNotFoundException extends SalesException {

    public AccountNotFoundException(UUID id) {
        super("Account not found: " + id, HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND");
    }

    public AccountNotFoundException(String name) {
        super("Account not found with name: " + name, HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND");
    }
}
