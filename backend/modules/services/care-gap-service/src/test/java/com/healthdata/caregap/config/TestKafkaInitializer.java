package com.healthdata.caregap.config;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
/**
 * Testcontainers Kafka initializer for care-gap service tests.
 *
 * Ensures a Kafka broker is available for audit publishing in test profile
 * without requiring an external localhost:9092 broker.
 */
public class TestKafkaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ensureDockerAvailable();
        String bootstrapServers = TestKafkaContainerProvider.getBootstrapServers();
        TestPropertyValues.of(
                "spring.kafka.bootstrap-servers=" + bootstrapServers,
                "spring.kafka.producer.bootstrap-servers=" + bootstrapServers,
                "spring.kafka.consumer.bootstrap-servers=" + bootstrapServers,
                "audit.kafka.enabled=true"
        ).applyTo(applicationContext.getEnvironment());
    }

    private static void ensureDockerAvailable() {
        org.testcontainers.DockerClientFactory.instance().client();
    }
}
