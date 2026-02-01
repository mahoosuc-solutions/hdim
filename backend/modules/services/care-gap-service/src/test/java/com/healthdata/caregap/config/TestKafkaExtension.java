package com.healthdata.caregap.config;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.testcontainers.DockerClientFactory;

/**
 * JUnit extension that ensures Testcontainers Kafka is running for all tests.
 *
 * Auto-detected via junit-platform.properties + service loader registration.
 *
 * Note: Skips initialization if @EmbeddedKafka is used (Spring's embedded Kafka handles it).
 */
public class TestKafkaExtension implements BeforeAllCallback {

    private static final Namespace NAMESPACE = Namespace.create(TestKafkaExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) {
        // Skip if @EmbeddedKafka annotation is present (Spring will handle Kafka initialization)
        if (hasEmbeddedKafkaAnnotation(context)) {
            return;
        }

        context.getRoot().getStore(NAMESPACE)
                .getOrComputeIfAbsent("kafka", key -> new KafkaResource(), KafkaResource.class);
    }

    private boolean hasEmbeddedKafkaAnnotation(ExtensionContext context) {
        Class<?> testClass = context.getTestClass().orElse(null);
        if (testClass == null) {
            return false;
        }
        return testClass.isAnnotationPresent(EmbeddedKafka.class);
    }

    private static final class KafkaResource implements CloseableResource {

        KafkaResource() {
            DockerClientFactory.instance().client();
            TestKafkaContainerProvider.ensureStarted();
            String bootstrapServers = TestKafkaContainerProvider.getBootstrapServers();
            System.setProperty("spring.kafka.bootstrap-servers", bootstrapServers);
            System.setProperty("spring.kafka.producer.bootstrap-servers", bootstrapServers);
            System.setProperty("spring.kafka.consumer.bootstrap-servers", bootstrapServers);
            System.setProperty("audit.kafka.enabled", "true");
        }

        @Override
        public void close() {
            TestKafkaContainerProvider.stop();
        }
    }
}
