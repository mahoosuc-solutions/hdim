package com.healthdata.caregap.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.repository.ai.AIAgentDecisionEventRepository;
import com.healthdata.audit.repository.ai.ConfigurationEngineEventRepository;
import com.healthdata.audit.repository.ai.UserConfigurationActionEventRepository;
import com.healthdata.audit.service.ai.AIAuditEventConsumer;
import com.healthdata.audit.service.ai.AIAuditEventPublisher;
import com.healthdata.audit.service.ai.AIAuditEventStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Test Configuration for Audit Integration
 * 
 * Provides beans and mocks for both lightweight and heavyweight tests.
 * 
 * Lightweight Tests (Unit Tests):
 * - Use @MockBean to mock AIAuditEventPublisher directly in test class
 * - No Kafka infrastructure needed
 * - Fast execution
 * 
 * Heavyweight Tests (Integration Tests):
 * - Real AIAuditEventPublisher is created automatically when KafkaTemplate is available
 * - Requires Kafka (Testcontainers)
 * - Mocks repository dependencies and consumer services that require database
 * - Full end-to-end verification
 * 
 * Note: This configuration provides a real AIAuditEventPublisher bean when
 * KafkaTemplate is available (from Spring Boot auto-configuration in heavyweight tests).
 * For lightweight tests, tests should use @MockBean directly.
 */
@TestConfiguration
public class TestAuditConfiguration {

    /**
     * Mock repositories to avoid database dependency in tests.
     * These are required by AIAuditEventStore but not needed for publisher tests.
     */
    @MockBean
    private AIAgentDecisionEventRepository aiAgentDecisionEventRepository;

    @MockBean
    private ConfigurationEngineEventRepository configurationEngineEventRepository;

    @MockBean
    private UserConfigurationActionEventRepository userConfigurationActionEventRepository;

    /**
     * Mock AIAuditEventStore to avoid loading repository dependencies.
     * We only need the publisher for these tests.
     */
    @MockBean
    private AIAuditEventStore aiAuditEventStore;

    /**
     * Mock AIAuditEventConsumer to avoid Kafka listener setup.
     * We're testing publishing, not consumption.
     */
    @MockBean
    private AIAuditEventConsumer aiAuditEventConsumer;

    /**
     * Real AIAuditEventPublisher bean for heavyweight integration tests.
     * This bean is only created when a real KafkaTemplate is available.
     * 
     * For lightweight tests, use @MockBean AIAuditEventPublisher in the test class.
     */
    @Bean
    @Primary
    public AIAuditEventPublisher aiAuditEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        return new AIAuditEventPublisher(kafkaTemplate, objectMapper);
    }
}

