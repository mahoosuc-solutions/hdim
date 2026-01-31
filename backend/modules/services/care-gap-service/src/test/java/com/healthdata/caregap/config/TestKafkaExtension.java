package com.healthdata.caregap.config;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.testcontainers.DockerClientFactory;

/**
 * JUnit extension that ensures Testcontainers Kafka is running for all tests.
 *
 * Auto-detected via junit-platform.properties + service loader registration.
 */
public class TestKafkaExtension implements BeforeAllCallback {

    private static final Namespace NAMESPACE = Namespace.create(TestKafkaExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) {
        context.getRoot().getStore(NAMESPACE)
                .getOrComputeIfAbsent("kafka", key -> new KafkaResource(), KafkaResource.class);
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
