package com.healthdata.quality.service.notification;

import com.healthdata.quality.model.NotificationRecipient;
import com.healthdata.quality.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Recipient Resolution Service
 *
 * Resolves notification recipients by:
 * - Querying patient's care team members
 * - Fetching user notification preferences
 * - Filtering by preferred channels
 * - Respecting quiet hours settings
 * - Applying severity thresholds
 *
 * Returns a list of NotificationRecipient objects with all necessary
 * contact information and preferences.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecipientResolutionService {

    private final CareTeamMemberRepository careTeamRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    /**
     * Resolve recipients for a patient notification
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param channel Notification channel
     * @param severity Notification severity
     * @return List of resolved recipients
     */
    public List<NotificationRecipient> resolveRecipients(
            String tenantId,
            UUID patientId,
            NotificationEntity.NotificationChannel channel,
            NotificationEntity.NotificationSeverity severity) {

        log.debug("Resolving recipients for patient {} on channel {} with severity {}",
                patientId, channel, severity);

        // Step 1: Query patient's care team members
        List<CareTeamMemberEntity> careTeamMembers = careTeamRepository
                .findActiveByPatientIdAndTenantId(patientId, tenantId);

        if (careTeamMembers.isEmpty()) {
            log.warn("No active care team members found for patient {} in tenant {}", patientId, tenantId);
            return Collections.emptyList();
        }

        log.debug("Found {} active care team members", careTeamMembers.size());

        // Step 2: Extract user IDs
        List<String> userIds = careTeamMembers.stream()
                .map(CareTeamMemberEntity::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // Step 3: Fetch notification preferences for all users
        List<NotificationPreferenceEntity> preferences = preferenceRepository
                .findByUserIdsAndTenantId(userIds, tenantId);

        if (preferences.isEmpty()) {
            log.warn("No notification preferences found for care team members");
            return Collections.emptyList();
        }

        // Step 4: Create a map for quick lookup
        Map<String, NotificationPreferenceEntity> preferenceMap = preferences.stream()
                .collect(Collectors.toMap(NotificationPreferenceEntity::getUserId, p -> p));

        Map<String, CareTeamMemberEntity> careTeamMap = careTeamMembers.stream()
                .collect(Collectors.toMap(CareTeamMemberEntity::getUserId, m -> m, (m1, m2) -> m1));

        // Step 5: Build recipients list and apply filters
        List<NotificationRecipient> recipients = new ArrayList<>();

        for (CareTeamMemberEntity member : careTeamMembers) {
            NotificationPreferenceEntity preference = preferenceMap.get(member.getUserId());

            if (preference == null) {
                log.debug("No preference found for user {}, skipping", member.getUserId());
                continue;
            }

            // Check if user should receive notification based on preferences
            if (!preference.shouldReceive(channel, null, severity)) {
                log.debug("User {} filtered out by preferences (channel={}, severity={})",
                        member.getUserId(), channel, severity);
                continue;
            }

            // Build recipient object
            NotificationRecipient recipient = buildRecipient(member, preference);

            if (recipient != null && hasValidContact(recipient, channel)) {
                recipients.add(recipient);
            }
        }

        // Step 6: Sort recipients - primary care providers first
        recipients.sort(Comparator.comparing(NotificationRecipient::isPrimary).reversed());

        log.info("Resolved {} recipients for patient {} (channel={}, severity={})",
                recipients.size(), patientId, channel, severity);

        return recipients;
    }

    /**
     * Resolve recipients for a patient across all channels
     */
    public Map<NotificationEntity.NotificationChannel, List<NotificationRecipient>> resolveRecipientsForAllChannels(
            String tenantId,
            UUID patientId,
            NotificationEntity.NotificationSeverity severity) {

        Map<NotificationEntity.NotificationChannel, List<NotificationRecipient>> recipientsByChannel = new HashMap<>();

        for (NotificationEntity.NotificationChannel channel : NotificationEntity.NotificationChannel.values()) {
            List<NotificationRecipient> recipients = resolveRecipients(tenantId, patientId, channel, severity);
            if (!recipients.isEmpty()) {
                recipientsByChannel.put(channel, recipients);
            }
        }

        return recipientsByChannel;
    }

    /**
     * Build a NotificationRecipient from care team member and preference
     */
    private NotificationRecipient buildRecipient(
            CareTeamMemberEntity member,
            NotificationPreferenceEntity preference) {

        Set<NotificationEntity.NotificationChannel> enabledChannels = new HashSet<>();

        if (Boolean.TRUE.equals(preference.getEmailEnabled())) {
            enabledChannels.add(NotificationEntity.NotificationChannel.EMAIL);
        }
        if (Boolean.TRUE.equals(preference.getSmsEnabled())) {
            enabledChannels.add(NotificationEntity.NotificationChannel.SMS);
        }
        if (Boolean.TRUE.equals(preference.getPushEnabled())) {
            enabledChannels.add(NotificationEntity.NotificationChannel.PUSH);
        }
        if (Boolean.TRUE.equals(preference.getInAppEnabled())) {
            enabledChannels.add(NotificationEntity.NotificationChannel.IN_APP);
        }
        // WebSocket is always enabled
        enabledChannels.add(NotificationEntity.NotificationChannel.WEBSOCKET);

        return NotificationRecipient.builder()
                .userId(member.getUserId())
                .emailAddress(preference.getEmailAddress())
                .phoneNumber(preference.getPhoneNumber())
                .enabledChannels(enabledChannels)
                .careTeamRole(member.getRole())
                .isPrimary(Boolean.TRUE.equals(member.getIsPrimary()))
                .severityThreshold(preference.getSeverityThreshold())
                .build();
    }

    /**
     * Check if recipient has valid contact information for the channel
     */
    private boolean hasValidContact(NotificationRecipient recipient, NotificationEntity.NotificationChannel channel) {
        String contact = recipient.getContactForChannel(channel);
        if (contact == null || contact.isBlank()) {
            log.debug("User {} missing contact info for channel {}", recipient.getUserId(), channel);
            return false;
        }
        return true;
    }

    /**
     * Get primary care provider for a patient
     */
    public Optional<NotificationRecipient> getPrimaryCareProvider(String tenantId, UUID patientId) {
        return careTeamRepository.findPrimaryByPatientIdAndTenantId(patientId, tenantId)
                .flatMap(member -> {
                    Optional<NotificationPreferenceEntity> preference =
                            preferenceRepository.findByUserIdAndTenantId(member.getUserId(), tenantId);

                    return preference.map(pref -> buildRecipient(member, pref));
                });
    }
}
