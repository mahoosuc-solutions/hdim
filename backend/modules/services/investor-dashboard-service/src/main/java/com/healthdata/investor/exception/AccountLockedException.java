package com.healthdata.investor.exception;

/**
 * Exception thrown when an account is locked due to too many failed login attempts.
 */
public class AccountLockedException extends RuntimeException {

    public AccountLockedException(String message) {
        super(message);
    }
}
