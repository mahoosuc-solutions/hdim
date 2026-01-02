package com.healthdata.authentication.dto;

import com.healthdata.authentication.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Request DTO for user registration.
 * Contains all information needed to create a new user account.
 * Only users with ADMIN or SUPER_ADMIN roles can register new users.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    /**
     * Unique username for the new user.
     * Must be 3-50 characters.
     */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    /**
     * Email address for the new user.
     * Must be unique and valid email format.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    /**
     * Password for the new user.
     * Must be at least 8 characters.
     * IMPORTANT: This will be hashed with BCrypt before storage.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /**
     * First name of the user.
     */
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    /**
     * Last name of the user.
     */
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    /**
     * Tenant IDs the user should have access to.
     * At least one tenant is required.
     */
    @NotEmpty(message = "At least one tenant ID is required")
    private Set<String> tenantIds;

    /**
     * Roles to assign to the user.
     * At least one role is required.
     */
    @NotEmpty(message = "At least one role is required")
    private Set<UserRole> roles;
}
