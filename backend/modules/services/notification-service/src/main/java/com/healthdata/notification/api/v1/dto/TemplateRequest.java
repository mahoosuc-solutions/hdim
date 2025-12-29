package com.healthdata.notification.api.v1.dto;

import com.healthdata.notification.domain.model.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 100, message = "Code must be 100 characters or less")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must be 200 characters or less")
    private String name;

    @Size(max = 500, message = "Description must be 500 characters or less")
    private String description;

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    @Size(max = 500, message = "Subject template must be 500 characters or less")
    private String subjectTemplate;

    @NotBlank(message = "Body template is required")
    private String bodyTemplate;

    private String htmlTemplate;
}
