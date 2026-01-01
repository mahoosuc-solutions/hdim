package com.healthdata.authentication.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user login.
 * Contains credentials needed to authenticate a user.
 *
 * Supports both:
 * - "username": for programmatic/API clients
 * - "email": for backward compatibility with browser clients (clinical portal)
 *
 * @author HDIM Authentication Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginRequest {

    /**
     * Username or email address of the user.
     * Can be set via:
     * - "username" field (primary)
     * - "email" field (alias, for backward compatibility)
     */
    @NotBlank(message = "Username is required")
    @JsonAlias("email")
    private String username;

    /**
     * User's password.
     * IMPORTANT: This field should never be logged or stored in plaintext.
     */
    @NotBlank(message = "Password is required")
    private String password;
}

