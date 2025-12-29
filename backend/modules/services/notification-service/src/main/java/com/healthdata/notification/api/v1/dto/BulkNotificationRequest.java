package com.healthdata.notification.api.v1.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkNotificationRequest {

    @NotEmpty(message = "At least one notification is required")
    @Size(max = 100, message = "Maximum 100 notifications per request")
    @Valid
    private List<SendNotificationRequest> notifications;
}
