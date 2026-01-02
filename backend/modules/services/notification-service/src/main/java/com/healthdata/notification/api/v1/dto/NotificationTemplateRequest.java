package com.healthdata.notification.api.v1.dto;

import com.healthdata.notification.domain.model.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to create or update a notification template.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplateRequest {

    @NotBlank(message = "Template name is required")
    private String name;

    private String description;

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    /**
     * Subject template with {{variable}} placeholders.
     */
    private String subjectTemplate;

    /**
     * Body template with {{variable}} placeholders.
     */
    @NotBlank(message = "Body template is required")
    private String bodyTemplate;

    /**
     * List of variable names expected by this template.
     */
    private List<String> variables;

    @Builder.Default
    private Boolean active = true;
}
