package com.healthdata.investor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for InvestorContact entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactDTO {

    private UUID id;

    @NotBlank(message = "Name is required")
    private String name;

    private String title;

    private String organization;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;

    private String linkedInUrl;

    private String linkedInProfileId;

    @NotBlank(message = "Category is required")
    private String category;

    private String status;

    private String tier;

    private String investmentThesis;

    private String notes;

    private Instant lastContacted;

    private Instant nextFollowUp;

    private Integer activityCount;

    private List<ActivityDTO> recentActivities;

    private Instant createdAt;

    private Instant updatedAt;

    /**
     * Request DTO for creating a contact.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        @NotBlank(message = "Name is required")
        private String name;

        private String title;

        private String organization;

        @Email(message = "Invalid email format")
        private String email;

        private String phone;

        private String linkedInUrl;

        @NotBlank(message = "Category is required")
        private String category;

        private String tier;

        private String investmentThesis;

        private String notes;
    }

    /**
     * Request DTO for updating a contact.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        private String name;
        private String title;
        private String organization;
        private String email;
        private String phone;
        private String linkedInUrl;
        private String category;
        private String status;
        private String tier;
        private String investmentThesis;
        private String notes;
        private Instant nextFollowUp;
    }
}
