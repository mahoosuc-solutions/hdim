package com.healthdata.notification.application;

import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationType;
import com.healthdata.notification.domain.model.ProviderNotificationPreference;
import com.healthdata.notification.domain.repository.ProviderNotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for managing provider notification preferences.
 * Issue #148: Smart Notification Preferences
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProviderPreferenceService {

    private final ProviderNotificationPreferenceRepository repository;

    /**
     * Get all preferences for a provider.
     * Creates default preferences for any missing notification types.
     */
    @Transactional(readOnly = true)
    public List<ProviderNotificationPreference> getProviderPreferences(
            String tenantId, UUID providerId) {

        List<ProviderNotificationPreference> existing =
            repository.findByTenantIdAndProviderId(tenantId, providerId);

        // Check if we need to create defaults for missing types
        Set<NotificationType> existingTypes = new HashSet<>();
        for (ProviderNotificationPreference pref : existing) {
            existingTypes.add(pref.getNotificationType());
        }

        // For read-only queries, just return what exists
        return existing;
    }

    /**
     * Get all preferences with defaults populated for missing types.
     */
    public List<ProviderNotificationPreference> getOrCreateAllPreferences(
            String tenantId, UUID providerId) {

        List<ProviderNotificationPreference> existing =
            repository.findByTenantIdAndProviderId(tenantId, providerId);

        Set<NotificationType> existingTypes = new HashSet<>();
        for (ProviderNotificationPreference pref : existing) {
            existingTypes.add(pref.getNotificationType());
        }

        // Create defaults for missing types
        List<ProviderNotificationPreference> result = new ArrayList<>(existing);
        for (NotificationType type : NotificationType.values()) {
            if (!existingTypes.contains(type)) {
                ProviderNotificationPreference newPref = createDefaultPreference(
                    tenantId, providerId, type);
                result.add(repository.save(newPref));
            }
        }

        return result;
    }

    /**
     * Get preference for a specific notification type.
     */
    @Transactional(readOnly = true)
    public Optional<ProviderNotificationPreference> getPreference(
            String tenantId, UUID providerId, NotificationType type) {
        return repository.findByTenantIdAndProviderIdAndNotificationType(
            tenantId, providerId, type);
    }

    /**
     * Get or create preference for a notification type.
     */
    public ProviderNotificationPreference getOrCreatePreference(
            String tenantId, UUID providerId, NotificationType type) {

        return repository.findByTenantIdAndProviderIdAndNotificationType(
            tenantId, providerId, type)
            .orElseGet(() -> {
                ProviderNotificationPreference pref = createDefaultPreference(
                    tenantId, providerId, type);
                return repository.save(pref);
            });
    }

    /**
     * Update preference for a notification type.
     */
    public ProviderNotificationPreference updatePreference(
            String tenantId,
            UUID providerId,
            NotificationType type,
            Boolean enabled,
            NotificationChannel deliveryMethod,
            String additionalDeliveryMethods,
            String minUrgency,
            Boolean digestEnabled,
            String digestFrequency) {

        ProviderNotificationPreference pref = getOrCreatePreference(tenantId, providerId, type);

        // Cannot disable always-enabled types
        if (enabled != null && !type.isAlwaysEnabled()) {
            pref.setEnabled(enabled);
        }

        if (deliveryMethod != null) {
            pref.setDeliveryMethod(deliveryMethod);
        }

        if (additionalDeliveryMethods != null) {
            pref.setAdditionalDeliveryMethods(additionalDeliveryMethods);
        }

        if (minUrgency != null) {
            pref.setMinUrgency(minUrgency);
        }

        if (digestEnabled != null) {
            pref.setDigestEnabled(digestEnabled);
        }

        if (digestFrequency != null) {
            pref.setDigestFrequency(digestFrequency);
        }

        log.info("Updated notification preference for provider {} type {}: enabled={}, delivery={}",
            providerId, type, pref.getEnabled(), pref.getDeliveryMethod());

        return repository.save(pref);
    }

    /**
     * Bulk update multiple preferences.
     */
    public List<ProviderNotificationPreference> bulkUpdatePreferences(
            String tenantId,
            UUID providerId,
            List<PreferenceUpdateRequest> updates) {

        List<ProviderNotificationPreference> results = new ArrayList<>();

        for (PreferenceUpdateRequest update : updates) {
            ProviderNotificationPreference pref = updatePreference(
                tenantId,
                providerId,
                update.getNotificationType(),
                update.getEnabled(),
                update.getDeliveryMethod(),
                update.getAdditionalDeliveryMethods(),
                update.getMinUrgency(),
                update.getDigestEnabled(),
                update.getDigestFrequency()
            );
            results.add(pref);
        }

        return results;
    }

    /**
     * Check if a notification should be sent based on provider preferences.
     */
    @Transactional(readOnly = true)
    public boolean shouldNotify(String tenantId, UUID providerId,
                                NotificationType type, String urgency) {

        // Always-enabled types bypass preferences
        if (type.isAlwaysEnabled()) {
            return true;
        }

        Optional<ProviderNotificationPreference> pref =
            repository.findByTenantIdAndProviderIdAndNotificationType(tenantId, providerId, type);

        if (pref.isEmpty()) {
            // Default to enabled for new types
            return true;
        }

        return pref.get().shouldNotify(urgency);
    }

    /**
     * Get the delivery method for a notification type.
     */
    @Transactional(readOnly = true)
    public NotificationChannel getDeliveryMethod(String tenantId, UUID providerId,
                                                  NotificationType type) {
        return repository.findByTenantIdAndProviderIdAndNotificationType(tenantId, providerId, type)
            .map(ProviderNotificationPreference::getDeliveryMethod)
            .orElse(NotificationChannel.IN_APP);
    }

    /**
     * Reset preferences to defaults for a provider.
     */
    public List<ProviderNotificationPreference> resetToDefaults(String tenantId, UUID providerId) {
        repository.deleteByTenantIdAndProviderId(tenantId, providerId);

        List<ProviderNotificationPreference> defaults = new ArrayList<>();
        for (NotificationType type : NotificationType.values()) {
            ProviderNotificationPreference pref = createDefaultPreference(tenantId, providerId, type);
            defaults.add(repository.save(pref));
        }

        log.info("Reset notification preferences to defaults for provider {}", providerId);
        return defaults;
    }

    /**
     * Create a default preference for a notification type.
     */
    private ProviderNotificationPreference createDefaultPreference(
            String tenantId, UUID providerId, NotificationType type) {

        return ProviderNotificationPreference.builder()
            .tenantId(tenantId)
            .providerId(providerId)
            .notificationType(type)
            .enabled(true)
            .deliveryMethod(getDefaultDeliveryMethod(type))
            .minUrgency(getDefaultMinUrgency(type))
            .digestEnabled(type != NotificationType.CRITICAL_RESULT)
            .digestFrequency("DAILY")
            .build();
    }

    private NotificationChannel getDefaultDeliveryMethod(NotificationType type) {
        return switch (type) {
            case CRITICAL_RESULT -> NotificationChannel.IN_APP;
            case CARE_GAP_OVERDUE -> NotificationChannel.IN_APP;
            case QUALITY_MEASURE_UPDATE -> NotificationChannel.IN_APP;
            case PATIENT_MESSAGE -> NotificationChannel.IN_APP;
            case RISK_SCORE_CHANGE -> NotificationChannel.IN_APP;
            case PRIOR_AUTH_UPDATE -> NotificationChannel.IN_APP;
            case CARE_TEAM_UPDATE -> NotificationChannel.IN_APP;
        };
    }

    private String getDefaultMinUrgency(NotificationType type) {
        return switch (type) {
            case CRITICAL_RESULT -> "CRITICAL";
            case CARE_GAP_OVERDUE -> "MEDIUM";
            case QUALITY_MEASURE_UPDATE -> "LOW";
            case PATIENT_MESSAGE -> "LOW";
            case RISK_SCORE_CHANGE -> "HIGH";
            case PRIOR_AUTH_UPDATE -> "MEDIUM";
            case CARE_TEAM_UPDATE -> "LOW";
        };
    }

    /**
     * Request object for preference updates.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PreferenceUpdateRequest {
        private NotificationType notificationType;
        private Boolean enabled;
        private NotificationChannel deliveryMethod;
        private String additionalDeliveryMethods;
        private String minUrgency;
        private Boolean digestEnabled;
        private String digestFrequency;
    }
}
