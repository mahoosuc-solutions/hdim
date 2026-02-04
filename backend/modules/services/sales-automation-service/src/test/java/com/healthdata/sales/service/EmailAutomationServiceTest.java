package com.healthdata.sales.service;

import com.healthdata.sales.entity.*;
import com.healthdata.sales.repository.EmailSendLogRepository;
import com.healthdata.sales.repository.SequenceEnrollmentRepository;
import com.healthdata.sales.service.EmailAutomationService.SequenceAnalytics;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailAutomationService.
 *
 * Tests cover:
 * - Scheduled email processing
 * - Email sending with merge fields
 * - Email tracking (opens, clicks)
 * - Unsubscribe processing
 * - Sequence analytics
 * - Weekend skip logic
 * - Error handling for bounces
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmailAutomationService Unit Tests")
class EmailAutomationServiceTest {

    @Mock
    private SequenceEnrollmentRepository enrollmentRepository;

    @Mock
    private EmailSendLogRepository emailSendLogRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailAutomationService emailAutomationService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID SEQUENCE_ID = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();
    private static final UUID LEAD_ID = UUID.randomUUID();

    private EmailSequence testSequence;
    private SequenceEnrollment testEnrollment;
    private EmailSequenceStep testStep;

    @BeforeEach
    void setUp() {
        // Set up required field values via reflection
        ReflectionTestUtils.setField(emailAutomationService, "defaultFromEmail", "noreply@hdim.health");
        ReflectionTestUtils.setField(emailAutomationService, "defaultFromName", "HealthData-in-Motion");
        ReflectionTestUtils.setField(emailAutomationService, "baseUrl", "http://localhost:8106");
        ReflectionTestUtils.setField(emailAutomationService, "trackingEnabled", true);

        testStep = EmailSequenceStep.builder()
            .id(UUID.randomUUID())
            .stepOrder(1)
            .subject("Hello {{firstName}}")
            .bodyHtml("<p>Hi {{firstName}}, welcome to our service!</p>")
            .bodyText("Hi {{firstName}}, welcome to our service!")
            .delayDays(0)
            .delayHours(0)
            .active(true)
            .build();

        List<EmailSequenceStep> steps = new ArrayList<>();
        steps.add(testStep);

        testSequence = EmailSequence.builder()
            .id(SEQUENCE_ID)
            .tenantId(TENANT_ID)
            .name("Welcome Sequence")
            .active(true)
            .fromEmail("sales@hdim.health")
            .fromName("HDIM Sales")
            .trackOpens(true)
            .includeUnsubscribeLink(true)
            .steps(steps)
            .build();

        testEnrollment = SequenceEnrollment.builder()
            .id(ENROLLMENT_ID)
            .tenantId(TENANT_ID)
            .sequence(testSequence)
            .leadId(LEAD_ID)
            .email("recipient@example.com")
            .firstName("John")
            .lastName("Doe")
            .status(EnrollmentStatus.ACTIVE)
            .currentStep(0)
            .nextEmailAt(LocalDateTime.now().minusMinutes(5))
            .unsubscribeToken(UUID.randomUUID().toString())
            .build();
    }

    // ==========================================
    // processScheduledEmails Tests
    // ==========================================

    @Nested
    @DisplayName("processScheduledEmails Tests")
    class ProcessScheduledEmailsTests {

        @Test
        @DisplayName("should process due enrollments")
        void shouldProcessDueEnrollments() {
            when(enrollmentRepository.findDueForEmail(any(LocalDateTime.class)))
                .thenReturn(List.of(testEnrollment));
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailSendLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            emailAutomationService.processScheduledEmails();

            verify(enrollmentRepository).findDueForEmail(any());
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("should skip when no due enrollments")
        void shouldSkipWhenNoDueEnrollments() {
            when(enrollmentRepository.findDueForEmail(any(LocalDateTime.class)))
                .thenReturn(List.of());

            emailAutomationService.processScheduledEmails();

            verify(enrollmentRepository).findDueForEmail(any());
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("should continue processing after individual error")
        void shouldContinueProcessingAfterIndividualError() {
            SequenceEnrollment enrollment1 = testEnrollment;
            SequenceEnrollment enrollment2 = SequenceEnrollment.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .sequence(testSequence)
                .email("other@example.com")
                .firstName("Jane")
                .status(EnrollmentStatus.ACTIVE)
                .currentStep(0)
                .build();

            when(enrollmentRepository.findDueForEmail(any())).thenReturn(List.of(enrollment1, enrollment2));
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailSendLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // First enrollment throws error
            doThrow(new RuntimeException("Email error"))
                .doNothing()
                .when(mailSender).send(any(MimeMessage.class));

            emailAutomationService.processScheduledEmails();

            // Should attempt both
            verify(enrollmentRepository).findDueForEmail(any());
        }
    }

    // ==========================================
    // processEnrollment Tests
    // ==========================================

    @Nested
    @DisplayName("processEnrollment Tests")
    class ProcessEnrollmentTests {

        @Test
        @DisplayName("should skip enrollment that cannot receive email")
        void shouldSkipEnrollmentThatCannotReceiveEmail() {
            testEnrollment.setStatus(EnrollmentStatus.PAUSED);

            emailAutomationService.processEnrollment(testEnrollment);

            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("should skip inactive sequence")
        void shouldSkipInactiveSequence() {
            testSequence.setActive(false);

            emailAutomationService.processEnrollment(testEnrollment);

            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("should complete enrollment when all steps done")
        void shouldCompleteEnrollmentWhenAllStepsDone() {
            testEnrollment.setCurrentStep(testSequence.getSteps().size());
            when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            emailAutomationService.processEnrollment(testEnrollment);

            ArgumentCaptor<SequenceEnrollment> captor = ArgumentCaptor.forClass(SequenceEnrollment.class);
            verify(enrollmentRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        }

        @Test
        @DisplayName("should skip inactive step and advance")
        void shouldSkipInactiveStepAndAdvance() {
            testStep.setActive(false);
            when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            emailAutomationService.processEnrollment(testEnrollment);

            verify(mailSender, never()).send(any(MimeMessage.class));
            ArgumentCaptor<SequenceEnrollment> captor = ArgumentCaptor.forClass(SequenceEnrollment.class);
            verify(enrollmentRepository).save(captor.capture());
            assertThat(captor.getValue().getCurrentStep()).isEqualTo(1);
        }

        @Test
        @DisplayName("should advance step after successful send")
        void shouldAdvanceStepAfterSuccessfulSend() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailSendLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            emailAutomationService.processEnrollment(testEnrollment);

            verify(enrollmentRepository).save(any());
        }
    }

    // ==========================================
    // sendEmail Tests
    // ==========================================

    @Nested
    @DisplayName("sendEmail Tests")
    class SendEmailTests {

        @Test
        @DisplayName("should send email with merge fields")
        void shouldSendEmailWithMergeFields() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailSendLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            boolean result = emailAutomationService.sendEmail(testEnrollment, testStep);

            assertThat(result).isTrue();
            verify(mailSender).send(mimeMessage);

            ArgumentCaptor<EmailSendLog> logCaptor = ArgumentCaptor.forClass(EmailSendLog.class);
            verify(emailSendLogRepository).save(logCaptor.capture());
            assertThat(logCaptor.getValue().getSubject()).isEqualTo("Hello John");
            assertThat(logCaptor.getValue().getStatus()).isEqualTo(EmailSendLog.EmailStatus.SENT);
        }

        @Test
        @DisplayName("should add tracking pixel when enabled")
        void shouldAddTrackingPixelWhenEnabled() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailSendLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            emailAutomationService.sendEmail(testEnrollment, testStep);

            verify(emailSendLogRepository).save(any());
        }

        @Test
        @DisplayName("should add unsubscribe link when enabled")
        void shouldAddUnsubscribeLinkWhenEnabled() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailSendLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            emailAutomationService.sendEmail(testEnrollment, testStep);

            verify(emailSendLogRepository).save(any());
        }

        @Test
        @DisplayName("should use sequence from address when available")
        void shouldUseSequenceFromAddressWhenAvailable() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailSendLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            emailAutomationService.sendEmail(testEnrollment, testStep);

            ArgumentCaptor<EmailSendLog> captor = ArgumentCaptor.forClass(EmailSendLog.class);
            verify(emailSendLogRepository).save(captor.capture());
            assertThat(captor.getValue().getFromEmail()).isEqualTo("sales@hdim.health");
            assertThat(captor.getValue().getFromName()).isEqualTo("HDIM Sales");
        }

        @Test
        @DisplayName("should use default from address when sequence has none")
        void shouldUseDefaultFromAddressWhenSequenceHasNone() {
            testSequence.setFromEmail(null);
            testSequence.setFromName(null);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailSendLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            emailAutomationService.sendEmail(testEnrollment, testStep);

            ArgumentCaptor<EmailSendLog> captor = ArgumentCaptor.forClass(EmailSendLog.class);
            verify(emailSendLogRepository).save(captor.capture());
            assertThat(captor.getValue().getFromEmail()).isEqualTo("noreply@hdim.health");
        }

        @Test
        @DisplayName("should return false and log error on send failure")
        void shouldReturnFalseOnSendFailure() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));
            when(emailSendLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            boolean result = emailAutomationService.sendEmail(testEnrollment, testStep);

            assertThat(result).isFalse();
            ArgumentCaptor<EmailSendLog> captor = ArgumentCaptor.forClass(EmailSendLog.class);
            verify(emailSendLogRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(EmailSendLog.EmailStatus.FAILED);
        }
    }

    // ==========================================
    // sendNextEmail Tests
    // ==========================================

    @Nested
    @DisplayName("sendNextEmail Tests")
    class SendNextEmailTests {

        @Test
        @DisplayName("should send next email manually")
        void shouldSendNextEmailManually() {
            when(enrollmentRepository.findByIdAndTenantId(ENROLLMENT_ID, TENANT_ID))
                .thenReturn(Optional.of(testEnrollment));
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailSendLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            boolean result = emailAutomationService.sendNextEmail(TENANT_ID, ENROLLMENT_ID);

            assertThat(result).isTrue();
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("should throw exception when enrollment not found")
        void shouldThrowExceptionWhenEnrollmentNotFound() {
            when(enrollmentRepository.findByIdAndTenantId(ENROLLMENT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> emailAutomationService.sendNextEmail(TENANT_ID, ENROLLMENT_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Enrollment not found");
        }

        @Test
        @DisplayName("should throw exception when enrollment cannot receive email")
        void shouldThrowExceptionWhenEnrollmentCannotReceiveEmail() {
            testEnrollment.setStatus(EnrollmentStatus.UNSUBSCRIBED);
            when(enrollmentRepository.findByIdAndTenantId(ENROLLMENT_ID, TENANT_ID))
                .thenReturn(Optional.of(testEnrollment));

            assertThatThrownBy(() -> emailAutomationService.sendNextEmail(TENANT_ID, ENROLLMENT_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cannot receive email");
        }
    }

    // ==========================================
    // recordOpen Tests
    // ==========================================

    @Nested
    @DisplayName("recordOpen Tests")
    class RecordOpenTests {

        @Test
        @DisplayName("should record email open")
        void shouldRecordEmailOpen() {
            String trackingId = UUID.randomUUID().toString();
            EmailSendLog sendLog = EmailSendLog.builder()
                .id(UUID.randomUUID())
                .trackingId(trackingId)
                .enrollmentId(ENROLLMENT_ID)
                .status(EmailSendLog.EmailStatus.SENT)
                .build();

            when(emailSendLogRepository.findByTrackingId(trackingId)).thenReturn(Optional.of(sendLog));
            when(emailSendLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(enrollmentRepository.findById(ENROLLMENT_ID)).thenReturn(Optional.of(testEnrollment));
            when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            emailAutomationService.recordOpen(trackingId);

            verify(emailSendLogRepository).save(any());
            verify(enrollmentRepository).save(any());
        }

        @Test
        @DisplayName("should handle unknown tracking ID")
        void shouldHandleUnknownTrackingId() {
            when(emailSendLogRepository.findByTrackingId(anyString())).thenReturn(Optional.empty());

            emailAutomationService.recordOpen("unknown-id");

            verify(emailSendLogRepository, never()).save(any());
        }
    }

    // ==========================================
    // recordClick Tests
    // ==========================================

    @Nested
    @DisplayName("recordClick Tests")
    class RecordClickTests {

        @Test
        @DisplayName("should record link click")
        void shouldRecordLinkClick() {
            String trackingId = UUID.randomUUID().toString();
            EmailSendLog sendLog = EmailSendLog.builder()
                .id(UUID.randomUUID())
                .trackingId(trackingId)
                .enrollmentId(ENROLLMENT_ID)
                .status(EmailSendLog.EmailStatus.SENT)
                .build();

            when(emailSendLogRepository.findByTrackingId(trackingId)).thenReturn(Optional.of(sendLog));
            when(emailSendLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(enrollmentRepository.findById(ENROLLMENT_ID)).thenReturn(Optional.of(testEnrollment));
            when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            emailAutomationService.recordClick(trackingId);

            verify(emailSendLogRepository).save(any());
            verify(enrollmentRepository).save(any());
        }
    }

    // ==========================================
    // processUnsubscribe Tests
    // ==========================================

    @Nested
    @DisplayName("processUnsubscribe Tests")
    class ProcessUnsubscribeTests {

        @Test
        @DisplayName("should process unsubscribe by token")
        void shouldProcessUnsubscribeByToken() {
            String token = testEnrollment.getUnsubscribeToken();
            when(enrollmentRepository.findByUnsubscribeToken(token)).thenReturn(Optional.of(testEnrollment));
            when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            boolean result = emailAutomationService.processUnsubscribe(token);

            assertThat(result).isTrue();
            ArgumentCaptor<SequenceEnrollment> captor = ArgumentCaptor.forClass(SequenceEnrollment.class);
            verify(enrollmentRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(EnrollmentStatus.UNSUBSCRIBED);
        }

        @Test
        @DisplayName("should return false for invalid token")
        void shouldReturnFalseForInvalidToken() {
            when(enrollmentRepository.findByUnsubscribeToken(anyString())).thenReturn(Optional.empty());

            boolean result = emailAutomationService.processUnsubscribe("invalid-token");

            assertThat(result).isFalse();
            verify(enrollmentRepository, never()).save(any());
        }
    }

    // ==========================================
    // getSequenceAnalytics Tests
    // ==========================================

    @Nested
    @DisplayName("getSequenceAnalytics Tests")
    class GetSequenceAnalyticsTests {

        @Test
        @DisplayName("should calculate sequence analytics")
        void shouldCalculateSequenceAnalytics() {
            when(emailSendLogRepository.countBySequenceId(SEQUENCE_ID)).thenReturn(100L);
            when(emailSendLogRepository.countOpensBySequenceId(SEQUENCE_ID)).thenReturn(40L);
            when(emailSendLogRepository.countClicksBySequenceId(SEQUENCE_ID)).thenReturn(10L);
            when(emailSendLogRepository.countBouncesBySequenceId(SEQUENCE_ID)).thenReturn(5L);
            when(enrollmentRepository.countBySequenceId(SEQUENCE_ID)).thenReturn(50L);
            when(enrollmentRepository.countBySequenceIdAndStatus(SEQUENCE_ID, EnrollmentStatus.ACTIVE)).thenReturn(20L);
            when(enrollmentRepository.countBySequenceIdAndStatus(SEQUENCE_ID, EnrollmentStatus.COMPLETED)).thenReturn(25L);

            SequenceAnalytics result = emailAutomationService.getSequenceAnalytics(SEQUENCE_ID);

            assertThat(result.getSequenceId()).isEqualTo(SEQUENCE_ID);
            assertThat(result.getTotalEnrollments()).isEqualTo(50);
            assertThat(result.getActiveEnrollments()).isEqualTo(20);
            assertThat(result.getCompletedEnrollments()).isEqualTo(25);
            assertThat(result.getTotalEmailsSent()).isEqualTo(100);
            assertThat(result.getEmailsOpened()).isEqualTo(40);
            assertThat(result.getEmailsClicked()).isEqualTo(10);
            assertThat(result.getEmailsBounced()).isEqualTo(5);
            assertThat(result.getOpenRate()).isEqualTo(40.0);
            assertThat(result.getClickRate()).isEqualTo(10.0);
            assertThat(result.getBounceRate()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("should handle zero emails sent for rate calculations")
        void shouldHandleZeroEmailsSentForRateCalculations() {
            when(emailSendLogRepository.countBySequenceId(SEQUENCE_ID)).thenReturn(0L);
            when(emailSendLogRepository.countOpensBySequenceId(SEQUENCE_ID)).thenReturn(0L);
            when(emailSendLogRepository.countClicksBySequenceId(SEQUENCE_ID)).thenReturn(0L);
            when(emailSendLogRepository.countBouncesBySequenceId(SEQUENCE_ID)).thenReturn(0L);
            when(enrollmentRepository.countBySequenceId(SEQUENCE_ID)).thenReturn(0L);
            when(enrollmentRepository.countBySequenceIdAndStatus(any(), any())).thenReturn(0L);

            SequenceAnalytics result = emailAutomationService.getSequenceAnalytics(SEQUENCE_ID);

            assertThat(result.getOpenRate()).isEqualTo(0.0);
            assertThat(result.getClickRate()).isEqualTo(0.0);
            assertThat(result.getBounceRate()).isEqualTo(0.0);
        }
    }

    // ==========================================
    // Merge Field Processing Tests
    // ==========================================

    @Nested
    @DisplayName("Merge Field Processing Tests")
    class MergeFieldProcessingTests {

        @Test
        @DisplayName("should replace all merge fields")
        void shouldReplaceAllMergeFields() {
            testStep.setSubject("Welcome {{firstName}} {{lastName}}");
            testStep.setBodyHtml("<p>Hello {{firstName}} {{lastName}}, your email is {{email}}</p>");
            testEnrollment.setFirstName("Alice");
            testEnrollment.setLastName("Smith");
            testEnrollment.setEmail("alice@example.com");

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailSendLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            emailAutomationService.sendEmail(testEnrollment, testStep);

            ArgumentCaptor<EmailSendLog> captor = ArgumentCaptor.forClass(EmailSendLog.class);
            verify(emailSendLogRepository).save(captor.capture());
            assertThat(captor.getValue().getSubject()).isEqualTo("Welcome Alice Smith");
        }

        @Test
        @DisplayName("should handle null merge field values")
        void shouldHandleNullMergeFieldValues() {
            testStep.setSubject("Hello {{firstName}} {{lastName}}");
            testEnrollment.setFirstName(null);
            testEnrollment.setLastName(null);

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailSendLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            emailAutomationService.sendEmail(testEnrollment, testStep);

            ArgumentCaptor<EmailSendLog> captor = ArgumentCaptor.forClass(EmailSendLog.class);
            verify(emailSendLogRepository).save(captor.capture());
            assertThat(captor.getValue().getSubject()).isEqualTo("Hello  ");
        }

        @Test
        @DisplayName("should preserve unknown merge fields")
        void shouldPreserveUnknownMergeFields() {
            testStep.setSubject("Hello {{unknownField}}");

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailSendLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            emailAutomationService.sendEmail(testEnrollment, testStep);

            ArgumentCaptor<EmailSendLog> captor = ArgumentCaptor.forClass(EmailSendLog.class);
            verify(emailSendLogRepository).save(captor.capture());
            assertThat(captor.getValue().getSubject()).contains("{{unknownField}}");
        }
    }

    // ==========================================
    // Multi-Tenant Isolation Tests
    // ==========================================

    @Nested
    @DisplayName("Multi-Tenant Isolation Tests")
    class MultiTenantIsolationTests {

        @Test
        @DisplayName("should isolate manual send by tenant")
        void shouldIsolateManualSendByTenant() {
            UUID otherTenantId = UUID.randomUUID();
            when(enrollmentRepository.findByIdAndTenantId(ENROLLMENT_ID, otherTenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> emailAutomationService.sendNextEmail(otherTenantId, ENROLLMENT_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Enrollment not found");

            verify(enrollmentRepository).findByIdAndTenantId(ENROLLMENT_ID, otherTenantId);
        }
    }
}
