package com.healthdata.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user login.
 * Contains credentials needed to authenticate a user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    /**
     * Username or email address of the user.
     */
    @NotBlank(message = "Username is required")
    private String username;

    /**
     * User's password.
     * IMPORTANT: This field should never be logged or stored in plaintext.
     */
    @NotBlank(message = "Password is required")
    private String password;
}
