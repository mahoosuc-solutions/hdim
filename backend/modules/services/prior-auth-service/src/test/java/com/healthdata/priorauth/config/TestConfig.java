package com.healthdata.priorauth.config;

import com.healthdata.audit.service.AuditService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Test configuration that provides mock beans for components
 * that require external infrastructure (Kafka, Redis, etc.).
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return Mockito.mock(KafkaTemplate.class);
    }

    @Bean
    @Primary
    public AuditService auditService() {
        return Mockito.mock(AuditService.class);
    }
}
