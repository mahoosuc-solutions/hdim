package com.healthdata.sales.dto;

import com.healthdata.sales.entity.EnrollmentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SequenceEnrollmentDTO {

    private UUID id;
    private UUID tenantId;

    @NotNull(message = "Sequence ID is required")
    private UUID sequenceId;

    private String sequenceName;

    private UUID leadId;
    private UUID contactId;

    @Email(message = "Valid email is required")
    private String email;

    private String firstName;
    private String lastName;
    private String displayName;

    private Integer currentStep;
    private EnrollmentStatus status;
    private LocalDateTime nextEmailAt;
    private LocalDateTime lastEmailSentAt;

    private Integer emailsSent;
    private Integer emailsOpened;
    private Integer emailsClicked;
    private Integer emailsBounced;

    private UUID enrolledByUserId;
    private String pauseReason;
    private String completionReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
