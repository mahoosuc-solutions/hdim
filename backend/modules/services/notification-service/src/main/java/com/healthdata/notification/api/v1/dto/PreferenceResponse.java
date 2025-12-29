package com.healthdata.notification.api.v1.dto;

import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationPreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceResponse {

    private UUID id;
    private String tenantId;
    private String userId;
    private NotificationChannel channel;
    private Boolean enabled;
    private String email;
    private String phone;
    private Boolean quietHoursEnabled;
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
    private String timezone;
    private Instant createdAt;
    private Instant updatedAt;

    public static PreferenceResponse fromEntity(NotificationPreference preference) {
        return PreferenceResponse.builder()
            .id(preference.getId())
            .tenantId(preference.getTenantId())
            .userId(preference.getUserId())
            .channel(preference.getChannel())
            .enabled(preference.getEnabled())
            .email(preference.getEmail())
            .phone(preference.getPhone())
            .quietHoursEnabled(preference.getQuietHoursEnabled())
            .quietHoursStart(preference.getQuietHoursStart())
            .quietHoursEnd(preference.getQuietHoursEnd())
            .timezone(preference.getTimezone())
            .createdAt(preference.getCreatedAt())
            .updatedAt(preference.getUpdatedAt())
            .build();
    }
}
