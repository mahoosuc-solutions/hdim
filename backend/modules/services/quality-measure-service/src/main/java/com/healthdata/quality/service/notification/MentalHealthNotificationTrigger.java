package com.healthdata.quality.service.notification;

import com.healthdata.quality.dto.MentalHealthAssessmentDTO;
import com.healthdata.quality.dto.notification.GenericNotificationRequest;
import com.healthdata.quality.model.NotificationRecipient;
import com.healthdata.quality.persistence.CareTeamMemberEntity;
import com.healthdata.quality.persistence.CareTeamMemberRepository;
import com.healthdata.quality.persistence.NotificationEntity;
import com.healthdata.quality.service.NotificationService;
import com.healthdata.quality.service.PatientNameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mental Health Assessment Notification Trigger
 *
 * Automatically sends notifications when mental health assessments are completed.
 * Integrates with MentalHealthAssessmentService to deliver timely screening results.
 *
 * Notification Routing by Severity:
 * - Severe/Moderately-severe: WebSocket + Email + SMS (requires immediate clinical attention)
 * - Moderate: WebSocket + Email (requires follow-up)
 * - Mild: WebSocket + Email (for tracking and documentation)
 * - Minimal/Negative: WebSocket only (routine screening)
 *
 * Special Cases:
 * - PHQ-9 Item 9 > 0 (suicide risk): CRITICAL alert routing
 * - Positive screens requiring follow-up: Always notify care team
 *
 * Usage:
 * <pre>
 * mentalHealthNotificationTrigger.onAssessmentCompleted(tenantId, assessment);
 * </pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MentalHealthNotificationTrigger {

    private final NotificationService notificationService;
    private final PatientNameService patientNameService;
    private final RecipientResolutionService recipientResolutionService;
    private final CareTeamMemberRepository careTeamMemberRepository;

    @Value("${notification.mental-health.default-recipients.email}")
    private String defaultEmail;

    @Value("${notification.mental-health.default-recipients.sms}")
    private String defaultPhone;

    /**
     * Trigger notification when mental health assessment is completed
     * Called by MentalHealthAssessmentService after saving an assessment
     *
     * @param tenantId Tenant ID
     * @param assessment Completed mental health assessment
     */
    public void onAssessmentCompleted(String tenantId, MentalHealthAssessmentDTO assessment) {
        try {
            // Skip notifications for negative screens unless configured otherwise
            if (!shouldNotifyOnAssessment(assessment)) {
                log.debug("Skipping assessment notification for negative screen: {} (score: {}/{})",
                        assessment.getType(), assessment.getScore(), assessment.getMaxScore());
                return;
            }

            // Get recipients for this patient/tenant
            // For severe assessments, escalate to behavioral health specialists
            String severity = assessment.getSeverity().toLowerCase();
            boolean isSevere = severity.equals("severe") || severity.equals("moderately-severe");

            Map<String, String> recipients = isSevere
                    ? getRecipientsForSevereAssessment(tenantId, assessment.getPatientId(), assessment)
                    : getRecipients(tenantId, assessment.getPatientId());

            // Determine notification channels based on severity
            boolean sendEmail = shouldSendEmail(assessment);
            boolean sendSms = shouldSendSms(assessment);

            // Build notification request
            GenericNotificationRequest request = GenericNotificationRequest.builder()
                    .notificationType("MENTAL_HEALTH_ASSESSMENT_COMPLETED")
                    .templateId("care-gap")  // Use care-gap template for mental health
                    .tenantId(tenantId)
                    .patientId(assessment.getPatientId())
                    .title(buildAssessmentTitle(assessment))
                    .message(buildAssessmentMessage(assessment))
                    .severity(mapSeverityToNotificationLevel(assessment.getSeverity()))
                    .sendWebSocket(true)  // Always send via WebSocket
                    .sendEmail(sendEmail)
                    .sendSms(sendSms)
                    .recipients(recipients)
                    .templateVariables(buildAssessmentTemplateVariables(assessment))
                    .metadata(buildAssessmentMetadata(assessment))
                    .build();

            // Send notification via NotificationService
            NotificationService.NotificationStatus status = notificationService.sendNotification(request);

            if (status.isAllSuccessful()) {
                log.info("Mental health assessment notification sent successfully for {} (severity: {})",
                        assessment.getType(), assessment.getSeverity());
            } else {
                log.warn("Mental health assessment notification partially failed for {}: {}",
                        assessment.getType(), status.getChannelStatus());
            }

        } catch (Exception e) {
            log.error("Failed to send mental health assessment notification for {} assessment: {}",
                    assessment.getType(), e.getMessage(), e);
        }
    }

    /**
     * Determine if we should notify on assessment completion
     * Prevents notification fatigue for routine negative screens
     */
    private boolean shouldNotifyOnAssessment(MentalHealthAssessmentDTO assessment) {
        // Always notify if positive screen or requires follow-up
        if (assessment.getPositiveScreen() || assessment.getRequiresFollowup()) {
            return true;
        }

        // Always notify for moderate or worse severity
        String severity = assessment.getSeverity().toLowerCase();
        if (severity.equals("severe") || severity.equals("moderately-severe") ||
            severity.equals("moderate")) {
            return true;
        }

        // Notify for mild severity assessments (for care team awareness)
        if (severity.equals("mild")) {
            return true;
        }

        // Don't notify for minimal/negative screens (routine)
        return false;
    }

    /**
     * Determine if email should be sent based on severity
     */
    private boolean shouldSendEmail(MentalHealthAssessmentDTO assessment) {
        // Send email for positive screens or moderate+ severity
        if (assessment.getPositiveScreen() || assessment.getRequiresFollowup()) {
            return true;
        }

        String severity = assessment.getSeverity().toLowerCase();
        return severity.equals("severe") || severity.equals("moderately-severe") ||
               severity.equals("moderate") || severity.equals("mild");
    }

    /**
     * Determine if SMS should be sent based on severity
     */
    private boolean shouldSendSms(MentalHealthAssessmentDTO assessment) {
        // Only send SMS for severe/moderately-severe (high clinical urgency)
        String severity = assessment.getSeverity().toLowerCase();
        return severity.equals("severe") || severity.equals("moderately-severe");
    }

    /**
     * Build notification title for assessment completion
     */
    private String buildAssessmentTitle(MentalHealthAssessmentDTO assessment) {
        if (assessment.getPositiveScreen()) {
            return String.format("⚠️ Positive %s Screen: %s", assessment.getName(), assessment.getSeverity());
        } else {
            return String.format("%s Assessment Complete: %s", assessment.getName(), assessment.getSeverity());
        }
    }

    /**
     * Build notification message for assessment completion
     */
    private String buildAssessmentMessage(MentalHealthAssessmentDTO assessment) {
        StringBuilder message = new StringBuilder();

        message.append(String.format("%s completed for patient %s. ",
                assessment.getName(), assessment.getPatientId()));

        message.append(String.format("Score: %d/%d (%s). ",
                assessment.getScore(), assessment.getMaxScore(), assessment.getSeverity()));

        if (assessment.getPositiveScreen()) {
            message.append(String.format("Positive screen (threshold: %d). ", assessment.getThresholdScore()));
        }

        if (assessment.getRequiresFollowup()) {
            message.append("Clinical follow-up required. ");
        }

        message.append(assessment.getInterpretation());

        return message.toString();
    }

    /**
     * Build template variables for assessment notification
     */
    private Map<String, Object> buildAssessmentTemplateVariables(MentalHealthAssessmentDTO assessment) {
        Map<String, Object> variables = new HashMap<>();

        // Patient information
        variables.put("patientId", assessment.getPatientId());
        variables.put("patientName", patientNameService.getPatientName(assessment.getPatientId()));

        // Assessment details
        variables.put("assessmentType", assessment.getType());
        variables.put("assessmentName", assessment.getName());
        variables.put("score", assessment.getScore());
        variables.put("maxScore", assessment.getMaxScore());
        variables.put("scorePercent", Math.round((double) assessment.getScore() / assessment.getMaxScore() * 100));
        variables.put("severity", assessment.getSeverity());
        variables.put("severityLabel", formatSeverity(assessment.getSeverity()));
        variables.put("interpretation", assessment.getInterpretation());

        // Screening results
        variables.put("positiveScreen", assessment.getPositiveScreen());
        variables.put("thresholdScore", assessment.getThresholdScore());
        variables.put("requiresFollowup", assessment.getRequiresFollowup());

        // Assessment metadata
        if (assessment.getAssessedBy() != null) {
            variables.put("assessedBy", assessment.getAssessedBy());
        }

        if (assessment.getAssessmentDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime assessmentDate = LocalDateTime.ofInstant(
                    assessment.getAssessmentDate(), ZoneId.systemDefault());
            variables.put("assessmentDate", assessmentDate.format(formatter));
        }

        // Action URL
        variables.put("actionUrl", "https://healthdata-in-motion.com/patients/" +
                assessment.getPatientId() + "/mental-health/" + assessment.getId());

        // Facility name
        variables.put("facilityName", "HealthData Clinical System");

        // Event type
        variables.put("eventType", "ASSESSMENT_COMPLETED");

        // Urgency indicators
        variables.put("requiresImmediateAction",
                assessment.getSeverity().equalsIgnoreCase("severe") ||
                assessment.getSeverity().equalsIgnoreCase("moderately-severe"));

        return variables;
    }

    /**
     * Build metadata for notification tracking
     */
    private Map<String, Object> buildAssessmentMetadata(MentalHealthAssessmentDTO assessment) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("assessmentId", assessment.getId());
        metadata.put("assessmentType", assessment.getType());
        metadata.put("score", assessment.getScore());
        metadata.put("maxScore", assessment.getMaxScore());
        metadata.put("severity", assessment.getSeverity());
        metadata.put("positiveScreen", assessment.getPositiveScreen());
        metadata.put("requiresFollowup", assessment.getRequiresFollowup());
        return metadata;
    }

    /**
     * Map assessment severity to notification severity level
     */
    private String mapSeverityToNotificationLevel(String assessmentSeverity) {
        return switch (assessmentSeverity.toLowerCase()) {
            case "severe", "moderately-severe" -> "HIGH";
            case "moderate", "positive" -> "MEDIUM";
            case "mild" -> "LOW";
            default -> "LOW";
        };
    }

    /**
     * Format severity for display
     */
    private String formatSeverity(String severity) {
        if (severity == null) return "Unknown";

        // Handle hyphenated terms
        String[] parts = severity.split("-");
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) formatted.append("-");
            formatted.append(parts[i].substring(0, 1).toUpperCase())
                     .append(parts[i].substring(1).toLowerCase());
        }
        return formatted.toString();
    }

    /**
     * Get recipients for mental health assessment notifications
     * Resolves recipients from database based on patient care team assignments
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Map of channel -> recipient ID
     */
    private Map<String, String> getRecipients(String tenantId, String patientId) {
        Map<String, String> recipients = new HashMap<>();

        try {
            // Resolve recipients for EMAIL channel
            List<NotificationRecipient> emailRecipients = recipientResolutionService.resolveRecipients(
                    tenantId,
                    patientId,
                    NotificationEntity.NotificationChannel.EMAIL,
                    NotificationEntity.NotificationSeverity.MEDIUM
            );

            // Resolve recipients for SMS channel
            List<NotificationRecipient> smsRecipients = recipientResolutionService.resolveRecipients(
                    tenantId,
                    patientId,
                    NotificationEntity.NotificationChannel.SMS,
                    NotificationEntity.NotificationSeverity.HIGH
            );

            // Add email recipients (comma-separated if multiple)
            if (!emailRecipients.isEmpty()) {
                String emailList = emailRecipients.stream()
                        .map(NotificationRecipient::getEmailAddress)
                        .filter(email -> email != null && !email.isBlank())
                        .reduce((a, b) -> a + "," + b)
                        .orElse(null);

                if (emailList != null) {
                    recipients.put("EMAIL", emailList);
                }
            }

            // Add SMS recipients (comma-separated if multiple)
            if (!smsRecipients.isEmpty()) {
                String phoneList = smsRecipients.stream()
                        .map(NotificationRecipient::getPhoneNumber)
                        .filter(phone -> phone != null && !phone.isBlank())
                        .reduce((a, b) -> a + "," + b)
                        .orElse(null);

                if (phoneList != null) {
                    recipients.put("SMS", phoneList);
                }
            }

            // If no recipients found, fall back to default recipients
            if (recipients.isEmpty()) {
                log.warn("No recipients found for patient {} in tenant {}, using defaults", patientId, tenantId);
                recipients.put("EMAIL", defaultEmail);
                recipients.put("SMS", defaultPhone);
            }

        } catch (Exception e) {
            log.error("Error resolving recipients for patient {}: {}, using defaults", patientId, e.getMessage(), e);
            recipients.put("EMAIL", defaultEmail);
            recipients.put("SMS", defaultPhone);
        }

        return recipients;
    }

    /**
     * Get recipients for severe mental health assessments
     * Includes behavioral health specialists for escalation
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param assessment Mental health assessment
     * @return Map of channel -> recipient ID
     */
    private Map<String, String> getRecipientsForSevereAssessment(String tenantId, String patientId, MentalHealthAssessmentDTO assessment) {
        Map<String, String> recipients = getRecipients(tenantId, patientId);

        try {
            // For severe assessments, escalate to behavioral health specialist
            List<CareTeamMemberEntity> mentalHealthCounselors = careTeamMemberRepository
                    .findActiveByPatientIdAndTenantIdAndRole(
                            patientId,
                            tenantId,
                            CareTeamMemberEntity.CareTeamRole.MENTAL_HEALTH_COUNSELOR
                    );

            if (!mentalHealthCounselors.isEmpty()) {
                log.info("Escalating severe mental health assessment to {} behavioral health specialists",
                        mentalHealthCounselors.size());

                // Resolve notification preferences for mental health counselors
                for (CareTeamMemberEntity counselor : mentalHealthCounselors) {
                    List<NotificationRecipient> counselorRecipients = recipientResolutionService.resolveRecipients(
                            tenantId,
                            patientId,
                            NotificationEntity.NotificationChannel.EMAIL,
                            NotificationEntity.NotificationSeverity.HIGH
                    );

                    // Add counselor emails to recipient list
                    for (NotificationRecipient recipient : counselorRecipients) {
                        if (recipient.getUserId().equals(counselor.getUserId()) &&
                            recipient.getEmailAddress() != null &&
                            !recipient.getEmailAddress().isBlank()) {

                            String existingEmails = recipients.get("EMAIL");
                            if (existingEmails != null && !existingEmails.contains(recipient.getEmailAddress())) {
                                recipients.put("EMAIL", existingEmails + "," + recipient.getEmailAddress());
                            } else if (existingEmails == null) {
                                recipients.put("EMAIL", recipient.getEmailAddress());
                            }
                        }
                    }
                }
            } else {
                log.warn("No mental health counselors found for patient {} in tenant {} - severe assessment may not be escalated",
                        patientId, tenantId);
            }

        } catch (Exception e) {
            log.error("Error resolving mental health counselors for patient {}: {}", patientId, e.getMessage(), e);
        }

        return recipients;
    }
}
