package com.healthdata.sales.service;

import com.healthdata.sales.entity.*;
import com.healthdata.sales.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for automated email sending from sequences
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailAutomationService {

    private final SequenceEnrollmentRepository enrollmentRepository;
    private final EmailSendLogRepository emailSendLogRepository;
    private final JavaMailSender mailSender;

    @Value("${email.from.address:${spring.mail.username:}}")
    private String defaultFromEmail;

    @Value("${email.from.name:HealthData-in-Motion}")
    private String defaultFromName;

    @Value("${app.base-url:http://localhost:8106}")
    private String baseUrl;

    @Value("${email.tracking.enabled:true}")
    private boolean trackingEnabled;

    private static final Pattern MERGE_FIELD_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    // ==================== Scheduled Email Processing ====================

    /**
     * Process scheduled emails - runs every minute
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void processScheduledEmails() {
        LocalDateTime now = LocalDateTime.now();
        List<SequenceEnrollment> dueEnrollments = enrollmentRepository.findDueForEmail(now);

        if (dueEnrollments.isEmpty()) {
            return;
        }

        log.info("Processing {} scheduled emails", dueEnrollments.size());

        for (SequenceEnrollment enrollment : dueEnrollments) {
            try {
                processEnrollment(enrollment);
            } catch (Exception e) {
                log.error("Error processing enrollment {}: {}", enrollment.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Process a single enrollment - send the next email
     */
    @Transactional
    public void processEnrollment(SequenceEnrollment enrollment) {
        if (!enrollment.canReceiveEmail()) {
            log.debug("Enrollment {} cannot receive email, skipping", enrollment.getId());
            return;
        }

        EmailSequence sequence = enrollment.getSequence();
        if (sequence == null || !sequence.getActive()) {
            log.debug("Sequence is inactive, skipping enrollment {}", enrollment.getId());
            return;
        }

        int currentStep = enrollment.getCurrentStep();
        List<EmailSequenceStep> steps = sequence.getSteps();

        if (currentStep >= steps.size()) {
            // All steps completed
            enrollment.complete("All steps completed");
            enrollmentRepository.save(enrollment);
            log.info("Enrollment {} completed all steps", enrollment.getId());
            return;
        }

        EmailSequenceStep step = steps.get(currentStep);

        if (!step.getActive()) {
            // Skip inactive step
            enrollment.setCurrentStep(currentStep + 1);
            scheduleNextEmail(enrollment);
            enrollmentRepository.save(enrollment);
            return;
        }

        // Send the email
        boolean sent = sendEmail(enrollment, step);

        if (sent) {
            enrollment.advanceStep();

            // Schedule next step
            if (enrollment.getCurrentStep() < steps.size()) {
                scheduleNextEmail(enrollment);
            } else {
                enrollment.complete("All steps completed");
            }
        }

        enrollmentRepository.save(enrollment);
    }

    /**
     * Send a single email
     */
    @Transactional
    public boolean sendEmail(SequenceEnrollment enrollment, EmailSequenceStep step) {
        EmailSequence sequence = enrollment.getSequence();

        // Create log entry
        EmailSendLog sendLog = EmailSendLog.builder()
            .id(UUID.randomUUID())
            .tenantId(enrollment.getTenantId())
            .enrollmentId(enrollment.getId())
            .sequenceId(sequence.getId())
            .stepId(step.getId())
            .stepNumber(step.getStepOrder())
            .leadId(enrollment.getLeadId())
            .contactId(enrollment.getContactId())
            .recipientEmail(enrollment.getEmail())
            .recipientName(enrollment.getDisplayName())
            .fromEmail(sequence.getFromEmail() != null ? sequence.getFromEmail() : defaultFromEmail)
            .fromName(sequence.getFromName() != null ? sequence.getFromName() : defaultFromName)
            .subject(processMergeFields(step.getSubject(), enrollment))
            .status(EmailSendLog.EmailStatus.PENDING)
            .trackingId(UUID.randomUUID().toString())
            .build();

        try {
            // Prepare email content
            String htmlContent = processMergeFields(step.getBodyHtml(), enrollment);

            // Add tracking pixel if enabled
            if (Boolean.TRUE.equals(sequence.getTrackOpens())) {
                htmlContent = addTrackingPixel(htmlContent, sendLog.getTrackingId());
            }

            // Add unsubscribe link if enabled
            if (Boolean.TRUE.equals(sequence.getIncludeUnsubscribeLink())) {
                htmlContent = addUnsubscribeLink(htmlContent, enrollment.getUnsubscribeToken());
            }

            // Send via JavaMailSender
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(enrollment.getEmail());
            helper.setSubject(sendLog.getSubject());
            helper.setText(step.getBodyText() != null ? processMergeFields(step.getBodyText(), enrollment) : "", htmlContent);

            if (sendLog.getFromEmail() != null) {
                if (sendLog.getFromName() != null) {
                    helper.setFrom(sendLog.getFromEmail(), sendLog.getFromName());
                } else {
                    helper.setFrom(sendLog.getFromEmail());
                }
            }

            if (sequence.getReplyToEmail() != null) {
                helper.setReplyTo(sequence.getReplyToEmail());
            }

            mailSender.send(message);

            sendLog.markSent(null);
            emailSendLogRepository.save(sendLog);

            log.info("Sent email to {} for enrollment {}, step {}",
                enrollment.getEmail(), enrollment.getId(), step.getStepOrder());
            return true;

        } catch (MessagingException e) {
            sendLog.markFailed(e.getMessage());
            emailSendLogRepository.save(sendLog);

            log.error("Failed to send email to {}: {}", enrollment.getEmail(), e.getMessage());

            // Record bounce if it's a delivery failure
            if (e.getMessage() != null && (e.getMessage().contains("550") || e.getMessage().contains("bounce"))) {
                enrollment.recordBounce();
            }

            return false;
        } catch (Exception e) {
            sendLog.markFailed(e.getMessage());
            emailSendLogRepository.save(sendLog);

            log.error("Error sending email: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Manually send next email for an enrollment
     */
    @Transactional
    public boolean sendNextEmail(UUID tenantId, UUID enrollmentId) {
        SequenceEnrollment enrollment = enrollmentRepository.findByIdAndTenantId(enrollmentId, tenantId)
            .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));

        if (!enrollment.canReceiveEmail()) {
            throw new RuntimeException("Enrollment cannot receive email");
        }

        processEnrollment(enrollment);
        return true;
    }

    // ==================== Email Tracking ====================

    /**
     * Record email open from tracking pixel
     */
    @Transactional
    public void recordOpen(String trackingId) {
        emailSendLogRepository.findByTrackingId(trackingId).ifPresent(log -> {
            log.markOpened();
            emailSendLogRepository.save(log);

            // Update enrollment
            if (log.getEnrollmentId() != null) {
                enrollmentRepository.findById(log.getEnrollmentId()).ifPresent(enrollment -> {
                    enrollment.recordOpen();
                    enrollmentRepository.save(enrollment);
                });
            }
        });
    }

    /**
     * Record link click
     */
    @Transactional
    public void recordClick(String trackingId) {
        emailSendLogRepository.findByTrackingId(trackingId).ifPresent(log -> {
            log.markClicked();
            emailSendLogRepository.save(log);

            // Update enrollment
            if (log.getEnrollmentId() != null) {
                enrollmentRepository.findById(log.getEnrollmentId()).ifPresent(enrollment -> {
                    enrollment.recordClick();
                    enrollmentRepository.save(enrollment);
                });
            }
        });
    }

    /**
     * Process unsubscribe
     */
    @Transactional
    public boolean processUnsubscribe(String token) {
        return enrollmentRepository.findByUnsubscribeToken(token)
            .map(enrollment -> {
                enrollment.unsubscribe();
                enrollmentRepository.save(enrollment);
                log.info("Unsubscribed enrollment {} via token", enrollment.getId());
                return true;
            })
            .orElse(false);
    }

    // ==================== Analytics ====================

    @Transactional(readOnly = true)
    public SequenceAnalytics getSequenceAnalytics(UUID sequenceId) {
        Long totalSent = emailSendLogRepository.countBySequenceId(sequenceId);
        Long opened = emailSendLogRepository.countOpensBySequenceId(sequenceId);
        Long clicked = emailSendLogRepository.countClicksBySequenceId(sequenceId);
        Long bounced = emailSendLogRepository.countBouncesBySequenceId(sequenceId);

        Long totalEnrollments = enrollmentRepository.countBySequenceId(sequenceId);
        Long activeEnrollments = enrollmentRepository.countBySequenceIdAndStatus(sequenceId, EnrollmentStatus.ACTIVE);
        Long completedEnrollments = enrollmentRepository.countBySequenceIdAndStatus(sequenceId, EnrollmentStatus.COMPLETED);

        double openRate = totalSent > 0 ? (opened * 100.0) / totalSent : 0;
        double clickRate = totalSent > 0 ? (clicked * 100.0) / totalSent : 0;
        double bounceRate = totalSent > 0 ? (bounced * 100.0) / totalSent : 0;

        return SequenceAnalytics.builder()
            .sequenceId(sequenceId)
            .totalEnrollments(totalEnrollments)
            .activeEnrollments(activeEnrollments)
            .completedEnrollments(completedEnrollments)
            .totalEmailsSent(totalSent)
            .emailsOpened(opened)
            .emailsClicked(clicked)
            .emailsBounced(bounced)
            .openRate(Math.round(openRate * 100.0) / 100.0)
            .clickRate(Math.round(clickRate * 100.0) / 100.0)
            .bounceRate(Math.round(bounceRate * 100.0) / 100.0)
            .build();
    }

    // ==================== Helper Methods ====================

    private void scheduleNextEmail(SequenceEnrollment enrollment) {
        EmailSequence sequence = enrollment.getSequence();
        int nextStepIndex = enrollment.getCurrentStep();

        if (nextStepIndex < sequence.getSteps().size()) {
            EmailSequenceStep nextStep = sequence.getSteps().get(nextStepIndex);
            LocalDateTime nextEmailTime = calculateNextEmailTime(nextStep);
            enrollment.setNextEmailAt(nextEmailTime);
        } else {
            enrollment.setNextEmailAt(null);
        }
    }

    private LocalDateTime calculateNextEmailTime(EmailSequenceStep step) {
        LocalDateTime nextTime = LocalDateTime.now();

        if (step.getDelayDays() != null && step.getDelayDays() > 0) {
            nextTime = nextTime.plusDays(step.getDelayDays());
        }
        if (step.getDelayHours() != null && step.getDelayHours() > 0) {
            nextTime = nextTime.plusHours(step.getDelayHours());
        }

        // Skip weekends
        if (Boolean.TRUE.equals(step.getSkipWeekends())) {
            while (nextTime.getDayOfWeek().getValue() > 5) {
                nextTime = nextTime.plusDays(1);
            }
        }

        // Apply preferred send time
        if (step.getSendTimePreference() != null) {
            try {
                String[] parts = step.getSendTimePreference().split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                nextTime = nextTime.withHour(hour).withMinute(minute);
            } catch (Exception ignored) {
            }
        }

        return nextTime;
    }

    private String processMergeFields(String content, SequenceEnrollment enrollment) {
        if (content == null) return null;

        Map<String, String> mergeData = Map.of(
            "firstName", enrollment.getFirstName() != null ? enrollment.getFirstName() : "",
            "lastName", enrollment.getLastName() != null ? enrollment.getLastName() : "",
            "email", enrollment.getEmail() != null ? enrollment.getEmail() : "",
            "displayName", enrollment.getDisplayName()
        );

        Matcher matcher = MERGE_FIELD_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String field = matcher.group(1);
            String replacement = mergeData.getOrDefault(field, "{{" + field + "}}");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String addTrackingPixel(String htmlContent, String trackingId) {
        String trackingPixel = String.format(
            "<img src=\"%s/sales-automation/api/sales/email/track/%s/open\" width=\"1\" height=\"1\" style=\"display:none;\" />",
            baseUrl, trackingId);

        if (htmlContent.contains("</body>")) {
            return htmlContent.replace("</body>", trackingPixel + "</body>");
        }
        return htmlContent + trackingPixel;
    }

    private String addUnsubscribeLink(String htmlContent, String unsubscribeToken) {
        String unsubscribeUrl = String.format("%s/sales-automation/api/sales/email/unsubscribe/%s", baseUrl, unsubscribeToken);
        String unsubscribeHtml = String.format(
            "<p style=\"text-align:center;font-size:12px;color:#666;margin-top:20px;\">" +
            "Don't want to receive these emails? <a href=\"%s\">Unsubscribe</a></p>",
            unsubscribeUrl);

        if (htmlContent.contains("</body>")) {
            return htmlContent.replace("</body>", unsubscribeHtml + "</body>");
        }
        return htmlContent + unsubscribeHtml;
    }

    // ==================== Analytics DTO ====================

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SequenceAnalytics {
        private UUID sequenceId;
        private Long totalEnrollments;
        private Long activeEnrollments;
        private Long completedEnrollments;
        private Long totalEmailsSent;
        private Long emailsOpened;
        private Long emailsClicked;
        private Long emailsBounced;
        private Double openRate;
        private Double clickRate;
        private Double bounceRate;
    }
}
