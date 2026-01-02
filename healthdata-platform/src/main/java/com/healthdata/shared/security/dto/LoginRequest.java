package com.healthdata.shared.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Login Request DTO - Represents user login credentials
 *
 * Used by the authentication endpoint to authenticate users with username/password.
 * Includes validation annotations for input validation.
 *
 * Spring Boot 3.3.5 compatible - Uses Jakarta EE (jakarta.*)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    /**
     * Username for authentication
     * Must not be blank
     * Length: 3-100 characters
     */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;

    /**
     * Password for authentication
     * Must not be blank
     * Length: 6-255 characters
     *
     * Note: Minimum password length is 6 characters for this endpoint.
     * The application may enforce stricter password requirements during registration.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 255, message = "Password must be between 6 and 255 characters")
    private String password;

    /**
     * Optional tenant ID for multi-tenant support
     * If not provided, the system will attempt to identify tenant from other context
     */
    private String tenantId;

    /**
     * Validate that both username and password are provided
     *
     * @return true if both fields are populated
     */
    public boolean isValid() {
        return username != null && !username.isBlank() &&
               password != null && !password.isBlank();
    }

    /**
     * Get masked password for logging (security best practice)
     *
     * @return Masked password string
     */
    public String getMaskedPassword() {
        if (password == null || password.isEmpty()) {
            return "***";
        }
        if (password.length() <= 2) {
            return "*".repeat(password.length());
        }
        return password.charAt(0) + "*".repeat(password.length() - 2) + password.charAt(password.length() - 1);
    }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "username='" + username + '\'' +
                ", password='" + getMaskedPassword() + '\'' +
                ", tenantId='" + tenantId + '\'' +
                '}';
    }
}
