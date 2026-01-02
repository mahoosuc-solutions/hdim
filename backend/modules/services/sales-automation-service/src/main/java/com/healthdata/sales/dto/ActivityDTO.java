package com.healthdata.sales.dto;

import com.healthdata.sales.entity.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDTO {

    private UUID id;
    private UUID tenantId;
    private UUID leadId;
    private UUID contactId;
    private UUID accountId;
    private UUID opportunityId;

    @NotNull(message = "Activity type is required")
    private ActivityType activityType;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String description;
    private String outcome;
    private LocalDateTime scheduledAt;
    private LocalDateTime completedAt;
    private Integer durationMinutes;
    private Boolean isCompleted;
    private UUID assignedToUserId;
    private String zohoActivityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
