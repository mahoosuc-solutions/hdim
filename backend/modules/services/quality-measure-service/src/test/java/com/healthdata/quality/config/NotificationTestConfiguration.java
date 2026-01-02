package com.healthdata.quality.config;

import com.healthdata.authentication.service.JwtTokenService;
import com.healthdata.quality.persistence.NotificationHistoryRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.context.annotation.Profile;

/**
 * Test Configuration for Notification E2E Tests
 *
 * Provides mock beans for external dependencies that aren't needed
 * for notification testing, allowing tests to run without Kafka, WebSocket auth, etc.
 *
 * Note: KafkaTemplate mocks have been moved to TestMessagingConfiguration to avoid
 * duplicate mock definition errors when integration tests also define @MockBean.
 */
@TestConfiguration
@Profile("test")
public class NotificationTestConfiguration {

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
