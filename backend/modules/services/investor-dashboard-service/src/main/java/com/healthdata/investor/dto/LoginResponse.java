package com.healthdata.investor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for login responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private UserDTO user;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserDTO {
        private UUID id;
        private String email;
        private String firstName;
        private String lastName;
        private String role;
    }
}
