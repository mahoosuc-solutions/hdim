package com.healthdata.test.config;

import com.healthdata.audit.service.ai.AIAuditEventPublisher;
import com.healthdata.audit.service.ai.AIAuditEventStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * Test configuration for mocking audit components.
 * 
 * Provides mocked audit services for unit tests.
 * 
 * Usage:
 * <pre>
 * {@code
 * @SpringBootTest
 * @Import(MockAuditConfig.class)
 * class MyUnitTest {
 *     @Autowired
 *     private AIAuditEventPublisher publisher; // Will be mocked
 * }
 * }
 * </pre>
 */
@TestConfiguration
public class MockAuditConfig {
    
    @Bean
    @Primary
    public AIAuditEventPublisher mockAuditEventPublisher() {
        return mock(AIAuditEventPublisher.class);
    }
    
    @Bean
    @Primary
    public AIAuditEventStore mockAuditEventStore() {
        return mock(AIAuditEventStore.class);
    }
}

