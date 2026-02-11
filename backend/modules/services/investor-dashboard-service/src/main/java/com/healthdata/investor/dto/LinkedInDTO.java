package com.healthdata.investor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTOs for LinkedIn integration.
 */
public class LinkedInDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConnectionStatus {
        private boolean connected;
        private String linkedInMemberId;
        private String profileUrl;
        private Instant expiresAt;
        private Instant lastSync;
        private String syncError;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Profile {
        private String id;
        private String firstName;
        private String lastName;
        private String profileUrl;
        private String email;
        private String headline;
        private String pictureUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthorizationUrlResponse {
        private String authorizationUrl;
        private String state;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OAuthCallbackRequest {
        private String code;
        private String state;
    }
}
