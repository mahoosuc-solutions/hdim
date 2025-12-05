package com.healthdata.quality.config;

import com.healthdata.authentication.service.JwtTokenService;
import com.healthdata.quality.persistence.NotificationHistoryRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test Configuration for Notification E2E Tests
 *
 * Provides mock beans for external dependencies that aren't needed
 * for notification testing, allowing tests to run without Kafka, WebSocket auth, etc.
 */
@TestConfiguration
@Profile("test")
public class NotificationTestConfiguration {

    /**
     * Mock KafkaTemplate<String, Object> to avoid needing a real Kafka broker
     */
    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplateObject;

    /**
     * Mock KafkaTemplate<String, String> for consumers requiring String-String template
     * (required by CareGapClosureEventConsumer and other Kafka consumers)
     */
    @MockBean
    private KafkaTemplate<String, String> kafkaTemplateString;

    /**
     * Mock JwtTokenService to avoid needing full authentication setup
     * (required by WebSocketConfig's JwtWebSocketHandshakeInterceptor)
     */
    @MockBean
    private JwtTokenService jwtTokenService;

    /**
     * Mock JavaMailSender to avoid needing SMTP configuration
     * (required by EmailNotificationChannel)
     */
    @MockBean
    private JavaMailSender javaMailSender;

    /**
     * Mock NotificationHistoryRepository to avoid query validation issues
     * (getAverageDeliveryTimeSeconds uses EXTRACT function not supported in HQL validation)
     */
    @MockBean
    private NotificationHistoryRepository notificationHistoryRepository;
}
