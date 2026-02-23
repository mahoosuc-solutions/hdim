package com.healthdata.nurseworkflow.api.v1.dto;

import com.healthdata.nurseworkflow.domain.model.PatientEngagementEscalationEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateEscalationRequest {
    @NotBlank
    private String reason;

    @NotNull
    private PatientEngagementEscalationEntity.EscalationSeverity severity;

    @NotBlank
    private String recipientId;

    @NotBlank
    private String recipientEmail;
}
