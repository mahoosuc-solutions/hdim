package com.healthdata.messaging.extension;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmbeddedKafkaExtensionTest {

    private final EmbeddedKafkaExtension extension = new EmbeddedKafkaExtension(3);

    @Test
    void shouldProvideBootstrapServersString() {
        String bootstrapServers = extension.getBootstrapServersString();

        // Extension provides default bootstrap servers when not running with @EmbeddedKafka
        assertThat(bootstrapServers).isNotBlank();
    }

    @Test
    void shouldProvideConsumerProperties() {
        var consumerProps = extension.getConsumerProperties();

        assertThat(consumerProps).containsKey("bootstrap.servers");
        assertThat(consumerProps).containsKey("group.id");
    }

    @Test
    void shouldProvideProducerProperties() {
        var producerProps = extension.getProducerProperties();

        assertThat(producerProps).containsKey("bootstrap.servers");
    }
}
