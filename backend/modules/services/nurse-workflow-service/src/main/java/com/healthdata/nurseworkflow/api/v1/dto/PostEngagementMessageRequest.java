package com.healthdata.nurseworkflow.api.v1.dto;

import com.healthdata.nurseworkflow.domain.model.PatientEngagementMessageEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PostEngagementMessageRequest {
    @NotNull
    private PatientEngagementMessageEntity.SenderType senderType;

    @NotBlank
    private String senderId;

    @NotBlank
    private String messageText;

    private Boolean containsPhi;

    private Boolean escalationFlag;
}
