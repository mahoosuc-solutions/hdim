package com.healthdata.approval.notification;

import com.healthdata.approval.domain.entity.ApprovalRequest;
import com.healthdata.approval.domain.entity.ApprovalRequest.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Email notification service for approval workflow events.
 * Sends formatted HTML emails for new assignments, status changes,
 * and expiration reminders.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${hdim.approval.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${hdim.approval.email.from:noreply@hdim.health}")
    private String fromAddress;

    @Value("${hdim.approval.email.dashboard-url:http://localhost:5173/approvals}")
    private String dashboardUrl;

    @Value("${hdim.approval.email.reply-to:support@hdim.health}")
    private String replyTo;

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a z")
            .withZone(ZoneId.systemDefault());

    /**
     * Send notification when a new approval request is assigned.
     */
    @Async
    public void sendAssignmentNotification(ApprovalRequest request, String recipientEmail, String recipientName) {
        if (!emailEnabled || recipientEmail == null) {
            log.debug("Email disabled or no recipient - skipping assignment notification");
            return;
        }

        try {
            Context context = new Context(Locale.US);
            context.setVariable("recipientName", recipientName != null ? recipientName : "Reviewer");
            context.setVariable("requestId", request.getId().toString());
            context.setVariable("entityType", request.getEntityType());
            context.setVariable("actionRequested", request.getActionRequested());
            context.setVariable("riskLevel", request.getRiskLevel().name());
            context.setVariable("riskLevelColor", getRiskLevelColor(request.getRiskLevel()));
            context.setVariable("requestedBy", request.getRequestedBy());
            context.setVariable("requestedAt", formatInstant(request.getRequestedAt()));
            context.setVariable("expiresAt", formatInstant(request.getExpiresAt()));
            context.setVariable("timeRemaining", formatTimeRemaining(request.getExpiresAt()));
            context.setVariable("dashboardUrl", dashboardUrl);
            context.setVariable("approvalUrl", dashboardUrl + "?id=" + request.getId());

            String htmlContent = templateEngine.process("email/approval-assignment", context);

            sendEmail(
                recipientEmail,
                buildSubject("New Approval Request", request),
                htmlContent
            );

            log.info("Sent assignment notification: requestId={}, recipient={}",
                request.getId(), recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send assignment notification: requestId={}, error={}",
                request.getId(), e.getMessage(), e);
        }
    }

    /**
     * Send notification when approval status changes.
     */
    @Async
    public void sendStatusChangeNotification(
            ApprovalRequest request,
            String recipientEmail,
            String recipientName,
            String actor) {
        if (!emailEnabled || recipientEmail == null) {
            return;
        }

        try {
            Context context = new Context(Locale.US);
            context.setVariable("recipientName", recipientName != null ? recipientName : "User");
            context.setVariable("requestId", request.getId().toString());
            context.setVariable("entityType", request.getEntityType());
            context.setVariable("actionRequested", request.getActionRequested());
            context.setVariable("status", request.getStatus().name());
            context.setVariable("statusColor", getStatusColor(request.getStatus().name()));
            context.setVariable("decisionBy", actor);
            context.setVariable("decisionReason", request.getDecisionReason());
            context.setVariable("decisionAt", formatInstant(request.getDecisionAt()));
            context.setVariable("dashboardUrl", dashboardUrl);
            context.setVariable("detailsUrl", dashboardUrl + "?id=" + request.getId());

            String htmlContent = templateEngine.process("email/approval-status-change", context);

            sendEmail(
                recipientEmail,
                buildSubject("Approval " + request.getStatus().name(), request),
                htmlContent
            );

            log.info("Sent status change notification: requestId={}, status={}, recipient={}",
                request.getId(), request.getStatus(), recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send status change notification: requestId={}, error={}",
                request.getId(), e.getMessage(), e);
        }
    }

    /**
     * Send reminder for approval requests expiring soon.
     */
    @Async
    public void sendExpirationReminderNotification(
            ApprovalRequest request,
            String recipientEmail,
            String recipientName) {
        if (!emailEnabled || recipientEmail == null) {
            return;
        }

        try {
            Context context = new Context(Locale.US);
            context.setVariable("recipientName", recipientName != null ? recipientName : "Reviewer");
            context.setVariable("requestId", request.getId().toString());
            context.setVariable("entityType", request.getEntityType());
            context.setVariable("actionRequested", request.getActionRequested());
            context.setVariable("riskLevel", request.getRiskLevel().name());
            context.setVariable("riskLevelColor", getRiskLevelColor(request.getRiskLevel()));
            context.setVariable("expiresAt", formatInstant(request.getExpiresAt()));
            context.setVariable("timeRemaining", formatTimeRemaining(request.getExpiresAt()));
            context.setVariable("isUrgent", isUrgent(request.getExpiresAt()));
            context.setVariable("dashboardUrl", dashboardUrl);
            context.setVariable("approvalUrl", dashboardUrl + "?id=" + request.getId());

            String htmlContent = templateEngine.process("email/approval-expiring-soon", context);

            sendEmail(
                recipientEmail,
                buildSubject("Expiring Soon", request),
                htmlContent
            );

            log.info("Sent expiration reminder: requestId={}, expiresAt={}, recipient={}",
                request.getId(), request.getExpiresAt(), recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send expiration reminder: requestId={}, error={}",
                request.getId(), e.getMessage(), e);
        }
    }

    /**
     * Send escalation notification.
     */
    @Async
    public void sendEscalationNotification(
            ApprovalRequest request,
            String recipientEmail,
            String recipientName,
            String escalatedBy) {
        if (!emailEnabled || recipientEmail == null) {
            return;
        }

        try {
            Context context = new Context(Locale.US);
            context.setVariable("recipientName", recipientName != null ? recipientName : "Supervisor");
            context.setVariable("requestId", request.getId().toString());
            context.setVariable("entityType", request.getEntityType());
            context.setVariable("actionRequested", request.getActionRequested());
            context.setVariable("riskLevel", request.getRiskLevel().name());
            context.setVariable("riskLevelColor", getRiskLevelColor(request.getRiskLevel()));
            context.setVariable("escalatedBy", escalatedBy);
            context.setVariable("escalationCount", request.getEscalationCount());
            context.setVariable("expiresAt", formatInstant(request.getExpiresAt()));
            context.setVariable("dashboardUrl", dashboardUrl);
            context.setVariable("approvalUrl", dashboardUrl + "?id=" + request.getId());

            String htmlContent = templateEngine.process("email/approval-escalation", context);

            sendEmail(
                recipientEmail,
                buildSubject("ESCALATED", request),
                htmlContent
            );

            log.info("Sent escalation notification: requestId={}, escalatedTo={}, recipient={}",
                request.getId(), request.getEscalatedTo(), recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send escalation notification: requestId={}, error={}",
                request.getId(), e.getMessage(), e);
        }
    }

    private void sendEmail(String to, String subject, String htmlContent) throws MessagingException, MailException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromAddress);
        helper.setTo(to);
        helper.setReplyTo(replyTo);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    private String buildSubject(String action, ApprovalRequest request) {
        String prefix = request.getRiskLevel() == RiskLevel.CRITICAL ? "[URGENT] " :
                        request.getRiskLevel() == RiskLevel.HIGH ? "[HIGH PRIORITY] " : "";
        return prefix + "HDIM Approval: " + action + " - " + request.getEntityType();
    }

    private String getRiskLevelColor(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case CRITICAL -> "#DC2626";  // Red
            case HIGH -> "#EA580C";       // Orange
            case MEDIUM -> "#CA8A04";     // Yellow
            case LOW -> "#2563EB";        // Blue
        };
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "APPROVED" -> "#16A34A";   // Green
            case "REJECTED" -> "#DC2626";   // Red
            case "ESCALATED" -> "#EA580C";  // Orange
            case "EXPIRED" -> "#6B7280";    // Gray
            default -> "#3B82F6";           // Blue
        };
    }

    private String formatInstant(Instant instant) {
        if (instant == null) return "N/A";
        return DATE_FORMATTER.format(instant);
    }

    private String formatTimeRemaining(Instant expiresAt) {
        if (expiresAt == null) return "N/A";

        Duration remaining = Duration.between(Instant.now(), expiresAt);
        if (remaining.isNegative()) return "Expired";

        long hours = remaining.toHours();
        long minutes = remaining.toMinutesPart();

        if (hours > 24) {
            return String.format("%d days, %d hours", hours / 24, hours % 24);
        } else if (hours > 0) {
            return String.format("%d hours, %d minutes", hours, minutes);
        } else {
            return String.format("%d minutes", minutes);
        }
    }

    private boolean isUrgent(Instant expiresAt) {
        if (expiresAt == null) return false;
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        return remaining.toHours() < 4;
    }
}
