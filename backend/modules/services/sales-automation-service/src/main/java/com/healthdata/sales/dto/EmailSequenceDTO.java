package com.healthdata.sales.dto;

import com.healthdata.sales.entity.SequenceType;
import com.healthdata.sales.entity.TargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSequenceDTO {

    private UUID id;
    private UUID tenantId;

    @NotBlank(message = "Sequence name is required")
    private String name;

    private String description;

    @NotNull(message = "Sequence type is required")
    private SequenceType sequenceType;

    @NotNull(message = "Target type is required")
    private TargetType targetType;

    private Boolean active;
    private String fromName;
    private String fromEmail;
    private String replyToEmail;
    private Boolean includeUnsubscribeLink;
    private Boolean trackOpens;
    private Boolean trackClicks;
    private UUID ownerUserId;

    private List<EmailSequenceStepDTO> steps;

    // Computed fields
    private Integer stepCount;
    private Integer totalDurationDays;
    private Long totalEnrollments;
    private Long activeEnrollments;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
