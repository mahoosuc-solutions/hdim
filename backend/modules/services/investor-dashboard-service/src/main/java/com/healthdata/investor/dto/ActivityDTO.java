package com.healthdata.investor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for OutreachActivity entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityDTO {

    private UUID id;

    private UUID contactId;

    private String contactName;

    @NotBlank(message = "Activity type is required")
    private String activityType;

    private String status;

    private String subject;

    private String content;

    @NotNull(message = "Activity date is required")
    private LocalDate activityDate;

    private Instant scheduledTime;

    private Instant responseReceived;

    private String responseContent;

    private String notes;

    private String linkedInMessageId;

    private String linkedInConnectionStatus;

    private UUID createdBy;

    private Instant createdAt;

    private Instant updatedAt;

    /**
     * Request DTO for creating an activity.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        @NotNull(message = "Contact ID is required")
        private UUID contactId;

        @NotBlank(message = "Activity type is required")
        private String activityType;

        private String subject;

        private String content;

        @NotNull(message = "Activity date is required")
        private LocalDate activityDate;

        private Instant scheduledTime;

        private String notes;
    }

    /**
     * Request DTO for updating an activity.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        private String activityType;
        private String status;
        private String subject;
        private String content;
        private LocalDate activityDate;
        private Instant scheduledTime;
        private Instant responseReceived;
        private String responseContent;
        private String notes;
    }
}
