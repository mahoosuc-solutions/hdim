package com.healthdata.approval.notification;

import com.healthdata.approval.domain.entity.ApprovalRequest;
import com.healthdata.approval.domain.entity.ApprovalRequest.*;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmailNotificationService Tests")
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailNotificationService emailService;

    @Captor
    private ArgumentCaptor<Context> contextCaptor;

    @Captor
    private ArgumentCaptor<MimeMessage> messageCaptor;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String REVIEWER_EMAIL = "reviewer@test.com";
    private static final String REVIEWER_NAME = "Test Reviewer";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@hdim.health");
        ReflectionTestUtils.setField(emailService, "dashboardUrl", "http://localhost:5173/approvals");
        ReflectionTestUtils.setField(emailService, "replyTo", "support@hdim.health");

        // Setup mail sender to return a MimeMessage
        when(mailSender.createMimeMessage()).thenReturn(
            new MimeMessage(Session.getInstance(new Properties()))
        );
    }

    @Nested
    @DisplayName("Send Assignment Notification")
    class SendAssignmentNotificationTests {

        @Test
        @DisplayName("should send assignment email successfully")
        void sendAssignmentNotification_Success() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            when(templateEngine.process(eq("email/approval-assignment"), any(Context.class)))
                .thenReturn("<html>Test Email</html>");

            // When
            emailService.sendAssignmentNotification(request, REVIEWER_EMAIL, REVIEWER_NAME);
            Thread.sleep(100); // Wait for async execution

            // Then
            verify(templateEngine).process(eq("email/approval-assignment"), contextCaptor.capture());
            verify(mailSender).send(any(MimeMessage.class));

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("recipientName")).isEqualTo(REVIEWER_NAME);
            assertThat(context.getVariable("requestId")).isEqualTo(request.getId().toString());
            assertThat(context.getVariable("entityType")).isEqualTo(request.getEntityType());
        }

        @Test
        @DisplayName("should skip when email disabled")
        void sendAssignmentNotification_Disabled_Skips() throws Exception {
            // Given
            ReflectionTestUtils.setField(emailService, "emailEnabled", false);
            ApprovalRequest request = createRequest();

            // When
            emailService.sendAssignmentNotification(request, REVIEWER_EMAIL, REVIEWER_NAME);
            Thread.sleep(100);

            // Then
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("should skip when recipient email is null")
        void sendAssignmentNotification_NullEmail_Skips() throws Exception {
            // Given
            ApprovalRequest request = createRequest();

            // When
            emailService.sendAssignmentNotification(request, null, REVIEWER_NAME);
            Thread.sleep(100);

            // Then
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("should use default name when recipient name is null")
        void sendAssignmentNotification_NullName_UsesDefault() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Test Email</html>");

            // When
            emailService.sendAssignmentNotification(request, REVIEWER_EMAIL, null);
            Thread.sleep(100);

            // Then
            verify(templateEngine).process(anyString(), contextCaptor.capture());
            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("recipientName")).isEqualTo("Reviewer");
        }

        @Test
        @DisplayName("should include risk level color in context")
        void sendAssignmentNotification_IncludesRiskColor() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            request.setRiskLevel(RiskLevel.CRITICAL);
            when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Test Email</html>");

            // When
            emailService.sendAssignmentNotification(request, REVIEWER_EMAIL, REVIEWER_NAME);
            Thread.sleep(100);

            // Then
            verify(templateEngine).process(anyString(), contextCaptor.capture());
            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("riskLevelColor")).isEqualTo("#DC2626"); // Red for critical
        }

        @Test
        @DisplayName("should include dashboard URLs in context")
        void sendAssignmentNotification_IncludesDashboardUrls() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Test Email</html>");

            // When
            emailService.sendAssignmentNotification(request, REVIEWER_EMAIL, REVIEWER_NAME);
            Thread.sleep(100);

            // Then
            verify(templateEngine).process(anyString(), contextCaptor.capture());
            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("dashboardUrl")).isEqualTo("http://localhost:5173/approvals");
            assertThat(context.getVariable("approvalUrl"))
                .isEqualTo("http://localhost:5173/approvals?id=" + request.getId());
        }

        @Test
        @DisplayName("should handle template processing error gracefully")
        void sendAssignmentNotification_TemplateError_HandlesGracefully() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            when(templateEngine.process(anyString(), any(Context.class)))
                .thenThrow(new RuntimeException("Template error"));

            // When/Then
            assertThatCode(() -> {
                emailService.sendAssignmentNotification(request, REVIEWER_EMAIL, REVIEWER_NAME);
                Thread.sleep(100);
            }).doesNotThrowAnyException();

            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("should handle mail sending error gracefully")
        void sendAssignmentNotification_MailError_HandlesGracefully() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Test Email</html>");
            doThrow(new MailException("SMTP error") {})
                .when(mailSender).send(any(MimeMessage.class));

            // When/Then
            assertThatCode(() -> {
                emailService.sendAssignmentNotification(request, REVIEWER_EMAIL, REVIEWER_NAME);
                Thread.sleep(100);
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Send Status Change Notification")
    class SendStatusChangeNotificationTests {

        @Test
        @DisplayName("should send status change email")
        void sendStatusChangeNotification_Success() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            request.approve("reviewer-123", "Looks good");
            when(templateEngine.process(eq("email/approval-status-change"), any(Context.class)))
                .thenReturn("<html>Status Changed</html>");

            // When
            emailService.sendStatusChangeNotification(request, REVIEWER_EMAIL, REVIEWER_NAME, "reviewer-123");
            Thread.sleep(100);

            // Then
            verify(templateEngine).process(eq("email/approval-status-change"), contextCaptor.capture());
            verify(mailSender).send(any(MimeMessage.class));

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("status")).isEqualTo("APPROVED");
            assertThat(context.getVariable("decisionBy")).isEqualTo("reviewer-123");
            assertThat(context.getVariable("decisionReason")).isEqualTo("Looks good");
        }

        @Test
        @DisplayName("should include status color in context")
        void sendStatusChangeNotification_IncludesStatusColor() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            request.approve("reviewer-123", "Approved");
            when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Status Changed</html>");

            // When
            emailService.sendStatusChangeNotification(request, REVIEWER_EMAIL, REVIEWER_NAME, "reviewer-123");
            Thread.sleep(100);

            // Then
            verify(templateEngine).process(anyString(), contextCaptor.capture());
            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("statusColor")).isEqualTo("#16A34A"); // Green for approved
        }

        @Test
        @DisplayName("should skip when email disabled")
        void sendStatusChangeNotification_Disabled_Skips() throws Exception {
            // Given
            ReflectionTestUtils.setField(emailService, "emailEnabled", false);
            ApprovalRequest request = createRequest();

            // When
            emailService.sendStatusChangeNotification(request, REVIEWER_EMAIL, REVIEWER_NAME, "actor");
            Thread.sleep(100);

            // Then
            verify(mailSender, never()).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("Send Expiration Reminder Notification")
    class SendExpirationReminderTests {

        @Test
        @DisplayName("should send expiration reminder email")
        void sendExpirationReminderNotification_Success() throws Exception {
            // Given
            ApprovalRequest request = createExpiringRequest(Duration.ofHours(2));
            when(templateEngine.process(eq("email/approval-expiring-soon"), any(Context.class)))
                .thenReturn("<html>Expiring Soon</html>");

            // When
            emailService.sendExpirationReminderNotification(request, REVIEWER_EMAIL, REVIEWER_NAME);
            Thread.sleep(100);

            // Then
            verify(templateEngine).process(eq("email/approval-expiring-soon"), contextCaptor.capture());
            verify(mailSender).send(any(MimeMessage.class));

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("recipientName")).isEqualTo(REVIEWER_NAME);
            assertThat(context.getVariable("requestId")).isEqualTo(request.getId().toString());
        }

        @Test
        @DisplayName("should mark as urgent when expiring within 4 hours")
        void sendExpirationReminderNotification_Urgent_MarksAsUrgent() throws Exception {
            // Given
            ApprovalRequest request = createExpiringRequest(Duration.ofHours(2));
            when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Expiring Soon</html>");

            // When
            emailService.sendExpirationReminderNotification(request, REVIEWER_EMAIL, REVIEWER_NAME);
            Thread.sleep(100);

            // Then
            verify(templateEngine).process(anyString(), contextCaptor.capture());
            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("isUrgent")).isEqualTo(true);
        }

        @Test
        @DisplayName("should not mark as urgent when expiring after 4 hours")
        void sendExpirationReminderNotification_NotUrgent_NotMarked() throws Exception {
            // Given
            ApprovalRequest request = createExpiringRequest(Duration.ofHours(6));
            when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Expiring Soon</html>");

            // When
            emailService.sendExpirationReminderNotification(request, REVIEWER_EMAIL, REVIEWER_NAME);
            Thread.sleep(100);

            // Then
            verify(templateEngine).process(anyString(), contextCaptor.capture());
            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("isUrgent")).isEqualTo(false);
        }

        @Test
        @DisplayName("should format time remaining correctly")
        void sendExpirationReminderNotification_FormatsTimeRemaining() throws Exception {
            // Given
            ApprovalRequest request = createExpiringRequest(Duration.ofHours(2).plusMinutes(30));
            when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Expiring Soon</html>");

            // When
            emailService.sendExpirationReminderNotification(request, REVIEWER_EMAIL, REVIEWER_NAME);
            Thread.sleep(100);

            // Then
            verify(templateEngine).process(anyString(), contextCaptor.capture());
            Context context = contextCaptor.getValue();
            String timeRemaining = (String) context.getVariable("timeRemaining");
            assertThat(timeRemaining).contains("hours");
            assertThat(timeRemaining).contains("minutes");
        }
    }

    @Nested
    @DisplayName("Send Escalation Notification")
    class SendEscalationNotificationTests {

        @Test
        @DisplayName("should send escalation email")
        void sendEscalationNotification_Success() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            request.escalate("supervisor-123", "Needs senior review");
            when(templateEngine.process(eq("email/approval-escalation"), any(Context.class)))
                .thenReturn("<html>Escalation</html>");

            // When
            emailService.sendEscalationNotification(request, REVIEWER_EMAIL, REVIEWER_NAME, "reviewer-123");
            Thread.sleep(100);

            // Then
            verify(templateEngine).process(eq("email/approval-escalation"), contextCaptor.capture());
            verify(mailSender).send(any(MimeMessage.class));

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("escalatedBy")).isEqualTo("reviewer-123");
            assertThat(context.getVariable("escalationCount")).isEqualTo(1);
        }

        @Test
        @DisplayName("should skip when email disabled")
        void sendEscalationNotification_Disabled_Skips() throws Exception {
            // Given
            ReflectionTestUtils.setField(emailService, "emailEnabled", false);
            ApprovalRequest request = createRequest();

            // When
            emailService.sendEscalationNotification(request, REVIEWER_EMAIL, REVIEWER_NAME, "actor");
            Thread.sleep(100);

            // Then
            verify(mailSender, never()).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("Subject Line Construction")
    class SubjectLineTests {

        @Test
        @DisplayName("should prefix CRITICAL risk with URGENT")
        void buildSubject_Critical_HasUrgentPrefix() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            request.setRiskLevel(RiskLevel.CRITICAL);
            when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Test</html>");

            // Mock MimeMessage to capture subject
            MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // When
            emailService.sendAssignmentNotification(request, REVIEWER_EMAIL, REVIEWER_NAME);
            Thread.sleep(100);

            // Then
            verify(mailSender).send(messageCaptor.capture());
            MimeMessage sentMessage = messageCaptor.getValue();
            String subject = sentMessage.getSubject();
            assertThat(subject).startsWith("[URGENT]");
        }

        @Test
        @DisplayName("should prefix HIGH risk with HIGH PRIORITY")
        void buildSubject_High_HasHighPriorityPrefix() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            request.setRiskLevel(RiskLevel.HIGH);
            when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Test</html>");

            MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // When
            emailService.sendAssignmentNotification(request, REVIEWER_EMAIL, REVIEWER_NAME);
            Thread.sleep(100);

            // Then
            verify(mailSender).send(messageCaptor.capture());
            MimeMessage sentMessage = messageCaptor.getValue();
            String subject = sentMessage.getSubject();
            assertThat(subject).startsWith("[HIGH PRIORITY]");
        }

        @Test
        @DisplayName("should not prefix MEDIUM and LOW risk")
        void buildSubject_MediumLow_NoPrefix() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            request.setRiskLevel(RiskLevel.MEDIUM);
            when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Test</html>");

            MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // When
            emailService.sendAssignmentNotification(request, REVIEWER_EMAIL, REVIEWER_NAME);
            Thread.sleep(100);

            // Then
            verify(mailSender).send(messageCaptor.capture());
            MimeMessage sentMessage = messageCaptor.getValue();
            String subject = sentMessage.getSubject();
            assertThat(subject).doesNotStartWith("[");
        }
    }

    @Nested
    @DisplayName("Formatting Helpers")
    class FormattingHelpersTests {

        @Test
        @DisplayName("should format time remaining in hours and minutes")
        void formatTimeRemaining_HoursAndMinutes() throws Exception {
            // Given
            ApprovalRequest request = createExpiringRequest(Duration.ofHours(3).plusMinutes(45));
            when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Test</html>");

            // When
            emailService.sendExpirationReminderNotification(request, REVIEWER_EMAIL, REVIEWER_NAME);
            Thread.sleep(100);

            // Then
            verify(templateEngine).process(anyString(), contextCaptor.capture());
            Context context = contextCaptor.getValue();
            String timeRemaining = (String) context.getVariable("timeRemaining");
            assertThat(timeRemaining).matches(".*\\d+ hours.*\\d+ minutes.*");
        }

        @Test
        @DisplayName("should format time remaining in days when over 24 hours")
        void formatTimeRemaining_Days() throws Exception {
            // Given
            ApprovalRequest request = createExpiringRequest(Duration.ofHours(30));
            when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Test</html>");

            // When
            emailService.sendExpirationReminderNotification(request, REVIEWER_EMAIL, REVIEWER_NAME);
            Thread.sleep(100);

            // Then
            verify(templateEngine).process(anyString(), contextCaptor.capture());
            Context context = contextCaptor.getValue();
            String timeRemaining = (String) context.getVariable("timeRemaining");
            assertThat(timeRemaining).contains("days");
        }

        @Test
        @DisplayName("should show Expired for past expiration")
        void formatTimeRemaining_Expired() throws Exception {
            // Given
            ApprovalRequest request = createExpiringRequest(Duration.ofHours(-1));
            when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Test</html>");

            // When
            emailService.sendExpirationReminderNotification(request, REVIEWER_EMAIL, REVIEWER_NAME);
            Thread.sleep(100);

            // Then
            verify(templateEngine).process(anyString(), contextCaptor.capture());
            Context context = contextCaptor.getValue();
            String timeRemaining = (String) context.getVariable("timeRemaining");
            assertThat(timeRemaining).isEqualTo("Expired");
        }
    }

    // Helper methods

    private ApprovalRequest createRequest() {
        return ApprovalRequest.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .requestType(RequestType.AGENT_ACTION)
            .entityType("TestEntity")
            .entityId("entity-123")
            .actionRequested("EXECUTE")
            .riskLevel(RiskLevel.MEDIUM)
            .status(ApprovalStatus.PENDING)
            .requestedBy(USER_ID)
            .payload(new HashMap<>())
            .requestedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(86400))
            .build();
    }

    private ApprovalRequest createExpiringRequest(Duration timeUntilExpiration) {
        ApprovalRequest request = createRequest();
        request.setExpiresAt(Instant.now().plus(timeUntilExpiration));
        return request;
    }
}
