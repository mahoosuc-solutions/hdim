package com.healthdata.notification.api.v1.dto;

import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateResponse {

    private UUID id;
    private String tenantId;
    private String code;
    private String name;
    private String description;
    private NotificationChannel channel;
    private String subjectTemplate;
    private String bodyTemplate;
    private String htmlTemplate;
    private List<String> variables;
    private Boolean active;
    private Integer version;
    private Instant createdAt;
    private Instant updatedAt;

    public static TemplateResponse fromEntity(NotificationTemplate template) {
        return TemplateResponse.builder()
            .id(template.getId())
            .tenantId(template.getTenantId())
            .code(template.getCode())
            .name(template.getName())
            .description(template.getDescription())
            .channel(template.getChannel())
            .subjectTemplate(template.getSubjectTemplate())
            .bodyTemplate(template.getBodyTemplate())
            .htmlTemplate(template.getHtmlTemplate())
            .variables(template.getVariables())
            .active(template.getActive())
            .version(template.getVersion())
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .build();
    }
}
