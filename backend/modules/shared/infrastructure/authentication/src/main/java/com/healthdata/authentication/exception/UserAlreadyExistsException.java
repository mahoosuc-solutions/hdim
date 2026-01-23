package com.healthdata.authentication.exception;

import com.healthdata.common.exception.HdimBusinessException.DuplicateEntityException;

/**
 * Exception thrown when attempting to create a user with a username or email
 * that already exists in the system.
 *
 * This exception maps to HTTP 409 Conflict status.
 */
public class UserAlreadyExistsException extends DuplicateEntityException {

    /**
     * Create exception for duplicate username.
     *
     * @param username The duplicate username
     * @return UserAlreadyExistsException
     */
    public static UserAlreadyExistsException forUsername(String username) {
        return new UserAlreadyExistsException("username", username);
    }

    /**
     * Create exception for duplicate email.
     *
     * @param email The duplicate email
     * @return UserAlreadyExistsException
     */
    public static UserAlreadyExistsException forEmail(String email) {
        return new UserAlreadyExistsException("email", email);
    }

    /**
     * Create exception with entity type and identifier.
     *
     * @param entityType The type of duplicate (username or email)
     * @param identifier The duplicate value
     */
    private UserAlreadyExistsException(String entityType, String identifier) {
        super("User " + entityType, identifier);
    }
}
