package com.healthdata.investor.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Principal object representing the authenticated user.
 * Used in the SecurityContext after JWT validation.
 */
@Getter
@AllArgsConstructor
public class UserPrincipal {

    private final UUID userId;
    private final String email;
    private final String role;

    @Override
    public String toString() {
        return "UserPrincipal{userId=" + userId + ", email='" + email + "', role='" + role + "'}";
    }
}
