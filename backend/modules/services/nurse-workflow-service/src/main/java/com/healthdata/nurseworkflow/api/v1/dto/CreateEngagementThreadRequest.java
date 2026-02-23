package com.healthdata.nurseworkflow.api.v1.dto;

import com.healthdata.nurseworkflow.domain.model.PatientEngagementThreadEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateEngagementThreadRequest {
    @NotNull
    private UUID patientId;

    @NotBlank
    private String subject;

    @NotBlank
    private String createdBy;

    private String assignedClinicianId;

    private PatientEngagementThreadEntity.ThreadPriority priority;

    @NotBlank
    private String initialMessage;

    private Boolean containsPhi;
}
