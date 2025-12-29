package com.healthdata.notification.application;

import com.healthdata.notification.api.v1.dto.NotificationPreferenceRequest;
import com.healthdata.notification.api.v1.dto.NotificationPreferenceResponse;
import com.healthdata.notification.domain.model.NotificationPreference;
import com.healthdata.notification.domain.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing user notification preferences.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    /**
     * Get all preferences for a user.
     */
    public List<NotificationPreferenceResponse> getUserPreferences(String userId, String tenantId) {
        return preferenceRepository.findByUserIdAndTenantId(userId, tenantId).stream()
            .map(this::mapToResponse)
            .toList();
    }

    /**
     * Update or create a preference for a user.
     */
    @Transactional
    public NotificationPreferenceResponse upsertPreference(String userId, NotificationPreferenceRequest request, String tenantId) {
        log.info("Updating preference for user: {}, channel: {}", userId, request.getChannel());

        NotificationPreference preference = preferenceRepository
            .findByUserIdAndChannelAndTenantId(userId, request.getChannel(), tenantId)
            .orElseGet(() -> NotificationPreference.builder()
                .tenantId(tenantId)
                .userId(userId)
                .channel(request.getChannel())
                .build());

        preference.setEnabled(request.getEnabled());
        preference.setQuietHoursStart(request.getQuietHoursStart());
        preference.setQuietHoursEnd(request.getQuietHoursEnd());
        preference.setTimezone(request.getTimezone());

        preference = preferenceRepository.save(preference);
        log.info("Saved preference: {} for user: {}", preference.getId(), userId);

        return mapToResponse(preference);
    }

    /**
     * Delete all preferences for a user (GDPR right to be forgotten).
     */
    @Transactional
    public void deleteUserPreferences(String userId, String tenantId) {
        log.info("Deleting all preferences for user: {}", userId);
        preferenceRepository.deleteByUserIdAndTenantId(userId, tenantId);
    }

    /**
     * Check if a channel is enabled for a user.
     */
    public boolean isChannelEnabled(String userId, String channelName, String tenantId) {
        return preferenceRepository
            .findByUserIdAndChannelAndTenantId(userId,
                com.healthdata.notification.domain.model.NotificationChannel.fromValue(channelName),
                tenantId)
            .map(NotificationPreference::getEnabled)
            .orElse(true); // Default to enabled if no preference exists
    }

    private NotificationPreferenceResponse mapToResponse(NotificationPreference preference) {
        return NotificationPreferenceResponse.builder()
            .id(preference.getId())
            .userId(preference.getUserId())
            .channel(preference.getChannel())
            .enabled(preference.getEnabled())
            .quietHoursStart(preference.getQuietHoursStart())
            .quietHoursEnd(preference.getQuietHoursEnd())
            .timezone(preference.getTimezone())
            .createdAt(preference.getCreatedAt())
            .updatedAt(preference.getUpdatedAt())
            .build();
    }
}
