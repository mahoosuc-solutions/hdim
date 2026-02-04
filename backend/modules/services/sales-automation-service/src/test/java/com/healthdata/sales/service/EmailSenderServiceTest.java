package com.healthdata.sales.service;

import com.healthdata.sales.config.EmailConfig;
import com.healthdata.sales.service.EmailSenderService.EmailMessage;
import com.healthdata.sales.service.EmailSenderService.SendResult;
import com.healthdata.sales.service.EmailSenderService.SendStats;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailSenderService.
 *
 * Tests cover:
 * - SMTP email sending
 * - SendGrid API integration
 * - Rate limiting
 * - Statistics tracking
 * - Error handling
 * - Provider selection
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmailSenderService Unit Tests")
class EmailSenderServiceTest {

    @Mock
    private EmailConfig emailConfig;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private RestTemplate emailRestTemplate;

    @Mock
    private MimeMessage mimeMessage;

    private EmailSenderService emailSenderService;

    private EmailConfig.From fromConfig;
    private EmailConfig.RateLimit rateLimitConfig;
    private EmailConfig.SendGrid sendGridConfig;

    @BeforeEach
    void setUp() {
        fromConfig = new EmailConfig.From();
        fromConfig.setAddress("noreply@hdim.health");
        fromConfig.setName("HealthData-in-Motion");

        rateLimitConfig = new EmailConfig.RateLimit();
        rateLimitConfig.setPerDay(1000);
        rateLimitConfig.setPerSecond(10);

        sendGridConfig = new EmailConfig.SendGrid();
        sendGridConfig.setApiKey("test-api-key");
        sendGridConfig.setApiUrl("https://api.sendgrid.com/v3/mail/send");

        emailSenderService = new EmailSenderService(emailConfig, javaMailSender, emailRestTemplate);
    }

    // ==========================================
    // SMTP Sending Tests
    // ==========================================

    @Nested
    @DisplayName("SMTP Email Sending Tests")
    class SmtpSendingTests {

        @BeforeEach
        void setUpSmtp() {
            when(emailConfig.isSendGridProvider()).thenReturn(false);
            when(emailConfig.getFrom()).thenReturn(fromConfig);
            when(emailConfig.getRateLimit()).thenReturn(rateLimitConfig);
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        }

        @Test
        @DisplayName("should send email via SMTP successfully")
        void shouldSendEmailViaSmtpSuccessfully() {
            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Test Subject")
                .textContent("Test body content")
                .build();

            SendResult result = emailSenderService.send(message);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.isRateLimited()).isFalse();
            verify(javaMailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("should send HTML email via SMTP")
        void shouldSendHtmlEmailViaSmtp() {
            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("HTML Test")
                .htmlContent("<h1>Hello</h1>")
                .textContent("Hello")
                .build();

            SendResult result = emailSenderService.send(message);

            assertThat(result.isSuccess()).isTrue();
            verify(javaMailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("should use custom from address when provided")
        void shouldUseCustomFromAddressWhenProvided() {
            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Test")
                .textContent("Test content")
                .fromAddress("custom@sender.com")
                .fromName("Custom Sender")
                .build();

            SendResult result = emailSenderService.send(message);

            assertThat(result.isSuccess()).isTrue();
            verify(javaMailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("should set reply-to when provided")
        void shouldSetReplyToWhenProvided() {
            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Test")
                .textContent("Test content")
                .replyTo("replyto@example.com")
                .build();

            SendResult result = emailSenderService.send(message);

            assertThat(result.isSuccess()).isTrue();
            verify(javaMailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("should handle HTML-only email")
        void shouldHandleHtmlOnlyEmail() {
            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("HTML Only")
                .htmlContent("<p>HTML content only</p>")
                .build();

            SendResult result = emailSenderService.send(message);

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("should handle text-only email")
        void shouldHandleTextOnlyEmail() {
            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Text Only")
                .textContent("Plain text content")
                .build();

            SendResult result = emailSenderService.send(message);

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("should handle empty content gracefully")
        void shouldHandleEmptyContentGracefully() {
            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("No Content")
                .build();

            SendResult result = emailSenderService.send(message);

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("should return failure on SMTP exception")
        void shouldReturnFailureOnSmtpException() {
            doThrow(new RuntimeException("SMTP connection failed"))
                .when(javaMailSender).send(any(MimeMessage.class));

            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Test")
                .textContent("Content")
                .build();

            SendResult result = emailSenderService.send(message);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError()).contains("SMTP connection failed");
        }
    }

    // ==========================================
    // SendGrid API Tests
    // ==========================================

    @Nested
    @DisplayName("SendGrid API Sending Tests")
    class SendGridSendingTests {

        @BeforeEach
        void setUpSendGrid() {
            when(emailConfig.isSendGridProvider()).thenReturn(true);
            when(emailConfig.getFrom()).thenReturn(fromConfig);
            when(emailConfig.getRateLimit()).thenReturn(rateLimitConfig);
            when(emailConfig.getSendgrid()).thenReturn(sendGridConfig);
        }

        @Test
        @DisplayName("should send email via SendGrid successfully")
        void shouldSendEmailViaSendGridSuccessfully() {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.set("X-Message-Id", "sendgrid-message-123");
            ResponseEntity<String> response = new ResponseEntity<>("", responseHeaders, HttpStatus.ACCEPTED);

            when(emailRestTemplate.exchange(
                eq(sendGridConfig.getApiUrl()),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
            )).thenReturn(response);

            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("SendGrid Test")
                .htmlContent("<h1>Hello from SendGrid</h1>")
                .build();

            SendResult result = emailSenderService.send(message);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessageId()).isEqualTo("sendgrid-message-123");
        }

        @Test
        @DisplayName("should build SendGrid request body correctly")
        void shouldBuildSendGridRequestBodyCorrectly() {
            ResponseEntity<String> response = new ResponseEntity<>("", HttpStatus.ACCEPTED);
            ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);

            when(emailRestTemplate.exchange(
                anyString(),
                any(HttpMethod.class),
                captor.capture(),
                eq(String.class)
            )).thenReturn(response);

            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Test Subject")
                .htmlContent("<p>Test</p>")
                .replyTo("reply@example.com")
                .build();

            emailSenderService.send(message);

            HttpEntity<Map<String, Object>> capturedRequest = captor.getValue();
            Map<String, Object> body = capturedRequest.getBody();

            assertThat(body).isNotNull();
            assertThat(body).containsKey("from");
            assertThat(body).containsKey("personalizations");
            assertThat(body).containsKey("content");
            assertThat(body).containsKey("reply_to");
            assertThat(body).containsKey("tracking_settings");

            // Verify Bearer token in headers
            assertThat(capturedRequest.getHeaders().get("Authorization"))
                .contains("Bearer test-api-key");
        }

        @Test
        @DisplayName("should use custom from address with SendGrid")
        void shouldUseCustomFromAddressWithSendGrid() {
            ResponseEntity<String> response = new ResponseEntity<>("", HttpStatus.ACCEPTED);
            ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);

            when(emailRestTemplate.exchange(anyString(), any(), captor.capture(), eq(String.class)))
                .thenReturn(response);

            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Test")
                .htmlContent("<p>Content</p>")
                .fromAddress("custom@sender.com")
                .fromName("Custom Name")
                .build();

            emailSenderService.send(message);

            @SuppressWarnings("unchecked")
            Map<String, String> from = (Map<String, String>) captor.getValue().getBody().get("from");
            assertThat(from.get("email")).isEqualTo("custom@sender.com");
            assertThat(from.get("name")).isEqualTo("Custom Name");
        }

        @Test
        @DisplayName("should send text content via SendGrid")
        void shouldSendTextContentViaSendGrid() {
            ResponseEntity<String> response = new ResponseEntity<>("", HttpStatus.ACCEPTED);
            ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);

            when(emailRestTemplate.exchange(anyString(), any(), captor.capture(), eq(String.class)))
                .thenReturn(response);

            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Plain Text")
                .textContent("Plain text email content")
                .build();

            emailSenderService.send(message);

            @SuppressWarnings("unchecked")
            var content = (java.util.List<Map<String, String>>) captor.getValue().getBody().get("content");
            assertThat(content.get(0).get("type")).isEqualTo("text/plain");
            assertThat(content.get(0).get("value")).isEqualTo("Plain text email content");
        }

        @Test
        @DisplayName("should return failure on SendGrid error response")
        void shouldReturnFailureOnSendGridErrorResponse() {
            ResponseEntity<String> response = new ResponseEntity<>(
                "{\"errors\":[{\"message\":\"Invalid API key\"}]}",
                HttpStatus.UNAUTHORIZED
            );

            when(emailRestTemplate.exchange(anyString(), any(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Test")
                .htmlContent("<p>Test</p>")
                .build();

            SendResult result = emailSenderService.send(message);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError()).contains("SendGrid error");
        }

        @Test
        @DisplayName("should return failure on SendGrid exception")
        void shouldReturnFailureOnSendGridException() {
            when(emailRestTemplate.exchange(anyString(), any(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));

            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Test")
                .htmlContent("<p>Test</p>")
                .build();

            SendResult result = emailSenderService.send(message);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError()).contains("SendGrid API error");
        }
    }

    // ==========================================
    // Rate Limiting Tests
    // ==========================================

    @Nested
    @DisplayName("Rate Limiting Tests")
    class RateLimitingTests {

        @Test
        @DisplayName("should reject email when daily limit exceeded")
        void shouldRejectEmailWhenDailyLimitExceeded() throws Exception {
            rateLimitConfig.setPerDay(5);
            when(emailConfig.getRateLimit()).thenReturn(rateLimitConfig);

            // Use reflection to set dailySendCount to limit
            Field dailySendCountField = EmailSenderService.class.getDeclaredField("dailySendCount");
            dailySendCountField.setAccessible(true);
            AtomicLong counter = (AtomicLong) dailySendCountField.get(emailSenderService);
            counter.set(5);

            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Test")
                .textContent("Content")
                .build();

            SendResult result = emailSenderService.send(message);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.isRateLimited()).isTrue();
            assertThat(result.getError()).contains("Daily send limit exceeded");
        }

        @Test
        @DisplayName("should allow email when under daily limit")
        void shouldAllowEmailWhenUnderDailyLimit() {
            when(emailConfig.isSendGridProvider()).thenReturn(false);
            when(emailConfig.getFrom()).thenReturn(fromConfig);
            rateLimitConfig.setPerDay(1000);
            when(emailConfig.getRateLimit()).thenReturn(rateLimitConfig);
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Test")
                .textContent("Content")
                .build();

            SendResult result = emailSenderService.send(message);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.isRateLimited()).isFalse();
        }

        @Test
        @DisplayName("should allow unlimited emails when perDay is 0")
        void shouldAllowUnlimitedEmailsWhenPerDayIsZero() throws Exception {
            when(emailConfig.isSendGridProvider()).thenReturn(false);
            when(emailConfig.getFrom()).thenReturn(fromConfig);
            rateLimitConfig.setPerDay(0); // 0 = unlimited
            when(emailConfig.getRateLimit()).thenReturn(rateLimitConfig);
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Set high send count
            Field dailySendCountField = EmailSenderService.class.getDeclaredField("dailySendCount");
            dailySendCountField.setAccessible(true);
            AtomicLong counter = (AtomicLong) dailySendCountField.get(emailSenderService);
            counter.set(10000);

            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Test")
                .textContent("Content")
                .build();

            SendResult result = emailSenderService.send(message);

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("should increment daily send count on successful send")
        void shouldIncrementDailySendCountOnSuccessfulSend() throws Exception {
            when(emailConfig.isSendGridProvider()).thenReturn(false);
            when(emailConfig.getFrom()).thenReturn(fromConfig);
            when(emailConfig.getRateLimit()).thenReturn(rateLimitConfig);
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

            Field dailySendCountField = EmailSenderService.class.getDeclaredField("dailySendCount");
            dailySendCountField.setAccessible(true);
            AtomicLong counter = (AtomicLong) dailySendCountField.get(emailSenderService);
            long initialCount = counter.get();

            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Test")
                .textContent("Content")
                .build();

            emailSenderService.send(message);

            assertThat(counter.get()).isEqualTo(initialCount + 1);
        }
    }

    // ==========================================
    // Statistics Tests
    // ==========================================

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("should return current send statistics")
        void shouldReturnCurrentSendStatistics() throws Exception {
            when(emailConfig.getProvider()).thenReturn("smtp");
            rateLimitConfig.setPerDay(1000);
            when(emailConfig.getRateLimit()).thenReturn(rateLimitConfig);

            // Set daily count
            Field dailySendCountField = EmailSenderService.class.getDeclaredField("dailySendCount");
            dailySendCountField.setAccessible(true);
            AtomicLong counter = (AtomicLong) dailySendCountField.get(emailSenderService);
            counter.set(150);

            SendStats stats = emailSenderService.getStats();

            assertThat(stats.getSentToday()).isEqualTo(150);
            assertThat(stats.getDailyLimit()).isEqualTo(1000);
            assertThat(stats.getProvider()).isEqualTo("smtp");
        }

        @Test
        @DisplayName("should reflect SendGrid provider in stats")
        void shouldReflectSendGridProviderInStats() {
            when(emailConfig.getProvider()).thenReturn("sendgrid");
            when(emailConfig.getRateLimit()).thenReturn(rateLimitConfig);

            SendStats stats = emailSenderService.getStats();

            assertThat(stats.getProvider()).isEqualTo("sendgrid");
        }
    }

    // ==========================================
    // SendResult Tests
    // ==========================================

    @Nested
    @DisplayName("SendResult Factory Methods Tests")
    class SendResultTests {

        @Test
        @DisplayName("should create success result")
        void shouldCreateSuccessResult() {
            SendResult result = SendResult.success("msg-123");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessageId()).isEqualTo("msg-123");
            assertThat(result.getError()).isNull();
            assertThat(result.isRateLimited()).isFalse();
        }

        @Test
        @DisplayName("should create success result with null messageId")
        void shouldCreateSuccessResultWithNullMessageId() {
            SendResult result = SendResult.success(null);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessageId()).isNull();
        }

        @Test
        @DisplayName("should create failed result")
        void shouldCreateFailedResult() {
            SendResult result = SendResult.failed("Connection timeout");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessageId()).isNull();
            assertThat(result.getError()).isEqualTo("Connection timeout");
            assertThat(result.isRateLimited()).isFalse();
        }

        @Test
        @DisplayName("should create rate limited result")
        void shouldCreateRateLimitedResult() {
            SendResult result = SendResult.rateLimited("Daily limit exceeded");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.isRateLimited()).isTrue();
            assertThat(result.getError()).isEqualTo("Daily limit exceeded");
        }
    }

    // ==========================================
    // EmailMessage Builder Tests
    // ==========================================

    @Nested
    @DisplayName("EmailMessage Builder Tests")
    class EmailMessageTests {

        @Test
        @DisplayName("should build email message with all fields")
        void shouldBuildEmailMessageWithAllFields() {
            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Test Subject")
                .textContent("Plain text")
                .htmlContent("<p>HTML content</p>")
                .fromAddress("sender@example.com")
                .fromName("Sender Name")
                .replyTo("reply@example.com")
                .headers(Map.of("X-Custom", "value"))
                .metadata(Map.of("campaignId", "123"))
                .build();

            assertThat(message.getTo()).isEqualTo("recipient@example.com");
            assertThat(message.getSubject()).isEqualTo("Test Subject");
            assertThat(message.getTextContent()).isEqualTo("Plain text");
            assertThat(message.getHtmlContent()).isEqualTo("<p>HTML content</p>");
            assertThat(message.getFromAddress()).isEqualTo("sender@example.com");
            assertThat(message.getFromName()).isEqualTo("Sender Name");
            assertThat(message.getReplyTo()).isEqualTo("reply@example.com");
            assertThat(message.getHeaders()).containsEntry("X-Custom", "value");
            assertThat(message.getMetadata()).containsEntry("campaignId", "123");
        }

        @Test
        @DisplayName("should build minimal email message")
        void shouldBuildMinimalEmailMessage() {
            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Test")
                .build();

            assertThat(message.getTo()).isEqualTo("recipient@example.com");
            assertThat(message.getSubject()).isEqualTo("Test");
            assertThat(message.getTextContent()).isNull();
            assertThat(message.getHtmlContent()).isNull();
        }
    }

    // ==========================================
    // Provider Selection Tests
    // ==========================================

    @Nested
    @DisplayName("Provider Selection Tests")
    class ProviderSelectionTests {

        @Test
        @DisplayName("should use SMTP when SendGrid is not configured")
        void shouldUseSmtpWhenSendGridNotConfigured() {
            when(emailConfig.isSendGridProvider()).thenReturn(false);
            when(emailConfig.getFrom()).thenReturn(fromConfig);
            when(emailConfig.getRateLimit()).thenReturn(rateLimitConfig);
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Test")
                .textContent("Content")
                .build();

            emailSenderService.send(message);

            verify(javaMailSender).send(mimeMessage);
            verify(emailRestTemplate, never()).exchange(anyString(), any(), any(), any(Class.class));
        }

        @Test
        @DisplayName("should use SendGrid when configured")
        void shouldUseSendGridWhenConfigured() {
            when(emailConfig.isSendGridProvider()).thenReturn(true);
            when(emailConfig.getFrom()).thenReturn(fromConfig);
            when(emailConfig.getRateLimit()).thenReturn(rateLimitConfig);
            when(emailConfig.getSendgrid()).thenReturn(sendGridConfig);

            ResponseEntity<String> response = new ResponseEntity<>("", HttpStatus.ACCEPTED);
            when(emailRestTemplate.exchange(anyString(), any(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

            EmailMessage message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Test")
                .htmlContent("<p>Content</p>")
                .build();

            emailSenderService.send(message);

            verify(emailRestTemplate).exchange(anyString(), any(), any(HttpEntity.class), eq(String.class));
            verify(javaMailSender, never()).send(any(MimeMessage.class));
        }
    }

    // ==========================================
    // Initialization Tests
    // ==========================================

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("should log provider info on init")
        void shouldLogProviderInfoOnInit() {
            when(emailConfig.getProvider()).thenReturn("smtp");
            when(emailConfig.isSendGridProvider()).thenReturn(false);

            // init() is called - this just verifies no exceptions
            emailSenderService.init();

            verify(emailConfig).getProvider();
        }

        @Test
        @DisplayName("should log SendGrid config when provider is SendGrid")
        void shouldLogSendGridConfigWhenProviderIsSendGrid() {
            when(emailConfig.getProvider()).thenReturn("sendgrid");
            when(emailConfig.isSendGridProvider()).thenReturn(true);

            emailSenderService.init();

            verify(emailConfig).isSendGridProvider();
        }
    }
}
