package com.healthdata.notification.application;

import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationPreference;
import com.healthdata.notification.domain.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    /**
     * Get or create preference for a user and channel.
     */
    public NotificationPreference getOrCreatePreference(
            String tenantId, String userId, NotificationChannel channel) {
        return preferenceRepository.findByTenantIdAndUserIdAndChannel(tenantId, userId, channel)
            .orElseGet(() -> {
                NotificationPreference preference = NotificationPreference.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .channel(channel)
                    .enabled(true)
                    .build();
                return preferenceRepository.save(preference);
            });
    }

    /**
     * Get all preferences for a user.
     */
    @Transactional(readOnly = true)
    public List<NotificationPreference> getUserPreferences(String tenantId, String userId) {
        return preferenceRepository.findByTenantIdAndUserId(tenantId, userId);
    }

    /**
     * Update preference.
     */
    public NotificationPreference updatePreference(UUID id, UpdatePreferenceRequest request) {
        NotificationPreference preference = preferenceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Preference not found: " + id));

        if (request.getEnabled() != null) {
            preference.setEnabled(request.getEnabled());
        }
        if (request.getEmail() != null) {
            preference.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            preference.setPhone(request.getPhone());
        }
        if (request.getQuietHoursEnabled() != null) {
            preference.setQuietHoursEnabled(request.getQuietHoursEnabled());
        }
        if (request.getQuietHoursStart() != null) {
            preference.setQuietHoursStart(request.getQuietHoursStart());
        }
        if (request.getQuietHoursEnd() != null) {
            preference.setQuietHoursEnd(request.getQuietHoursEnd());
        }
        if (request.getTimezone() != null) {
            preference.setTimezone(request.getTimezone());
        }

        return preferenceRepository.save(preference);
    }

    /**
     * Set user preference for a channel.
     */
    public NotificationPreference setPreference(
            String tenantId, String userId, NotificationChannel channel, 
            boolean enabled, String email, String phone) {
        
        NotificationPreference preference = getOrCreatePreference(tenantId, userId, channel);
        preference.setEnabled(enabled);
        
        if (email != null) {
            preference.setEmail(email);
        }
        if (phone != null) {
            preference.setPhone(phone);
        }

        return preferenceRepository.save(preference);
    }

    /**
     * Delete all preferences for a user.
     */
    public void deleteUserPreferences(String tenantId, String userId) {
        preferenceRepository.deleteByTenantIdAndUserId(tenantId, userId);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UpdatePreferenceRequest {
        private Boolean enabled;
        private String email;
        private String phone;
        private Boolean quietHoursEnabled;
        private LocalTime quietHoursStart;
        private LocalTime quietHoursEnd;
        private String timezone;
    }
}
