package com.healthdata;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.Properties;

/**
 * Test configuration for the HealthData Platform.
 *
 * This configuration provides test-specific beans and mocks for:
 * - Email services
 * - REST template configuration
 * - Auditing configuration
 * - External service mocks
 *
 * Apply this configuration to test classes using:
 * @SpringBootTest
 * @Import(HealthDataTestConfiguration.class)
 */
@TestConfiguration
@ActiveProfiles("test")
public class HealthDataTestConfiguration {

    /**
     * Mock JavaMailSender for testing email functionality.
     * In tests, emails are captured in memory rather than being sent.
     */
    @Bean
    @Primary
    public JavaMailSender mailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(1025); // MailHog or similar test mail server port
        mailSender.setUsername("test");
        mailSender.setPassword("test");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.debug", "false");

        return mailSender;
    }

    /**
     * REST Template for making HTTP calls in tests.
     * Pre-configured with test-specific settings.
     */
    @Bean
    @Primary
    public RestTemplate testRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // Additional interceptors or configurations can be added here
        return restTemplate;
    }

    /**
     * Auditor aware for tracking who created/modified entities in tests.
     * Returns a fixed test user ID for audit purposes.
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("test-user");
    }

    /**
     * Test-specific configuration for external service URLs.
     * These should be set to mock endpoints or disabled for tests.
     */
    public static final class ExternalServiceConfig {
        public static final String FHIR_SERVER_URL = "http://localhost:8080/fhir";
        public static final String TERMINOLOGY_SERVER_URL = "http://localhost:8080/terminology";
        public static final String NOTIFICATION_SERVICE_URL = "http://localhost:8080/notifications";
        public static final String CQL_ENGINE_URL = "http://localhost:8080/cql";
    }

    /**
     * Feature flags for testing specific functionality.
     * Can be toggled based on test requirements.
     */
    public static final class TestFeatureFlags {
        public static boolean ENABLE_EMAIL_SENDING = false;
        public static boolean ENABLE_SMS_SENDING = false;
        public static boolean ENABLE_PUSH_NOTIFICATIONS = false;
        public static boolean ENABLE_EXTERNAL_SERVICE_CALLS = false;
        public static boolean ENABLE_ASYNC_PROCESSING = true;
        public static boolean ENABLE_CACHING = true;
        public static boolean ENABLE_DATABASE_CLEANUP = true;
    }

    /**
     * Test data configuration constants.
     * Provides standard test data that can be reused across tests.
     */
    public static final class TestDataConfig {
        // Test patient IDs
        public static final String TEST_PATIENT_ID = "patient-123";
        public static final String TEST_PROVIDER_ID = "provider-456";
        public static final String TEST_TENANT_ID = "tenant-789";

        // Test measure IDs
        public static final String TEST_MEASURE_ID = "measure-001";
        public static final String TEST_QUALITY_MEASURE_ID = "quality-measure-001";

        // Test CareGap IDs
        public static final String TEST_CARE_GAP_ID = "caregap-001";

        // Test authentication
        public static final String TEST_JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IlRlc3QgVXNlciIsImlhdCI6MTUxNjIzOTAyMn0." +
                "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        public static final String TEST_USER_ID = "test-user-id";
        public static final String TEST_USERNAME = "testuser";
        public static final String TEST_EMAIL = "test@healthdata.com";
    }

    /**
     * Test timeout configurations.
     * Provides consistent timeout values for async and integration tests.
     */
    public static final class TestTimeouts {
        public static final long ASYNC_TIMEOUT_MILLIS = 5000; // 5 seconds
        public static final long DATABASE_TIMEOUT_MILLIS = 10000; // 10 seconds
        public static final long API_TIMEOUT_MILLIS = 3000; // 3 seconds
        public static final long WEBSOCKET_TIMEOUT_MILLIS = 5000; // 5 seconds
    }
}
