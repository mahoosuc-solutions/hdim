package com.healthdata.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

class MessagingAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MessagingAutoConfiguration.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withPropertyValues(
                    "healthdata.messaging.bootstrap-servers=localhost:29092",
                    "healthdata.messaging.client-id=messaging-test-client",
                    "healthdata.messaging.consumer.group-id=messaging-test-group",
                    "healthdata.messaging.consumer.trusted-packages=*",
                    "healthdata.messaging.topics[0].name=messaging-test-topic",
                    "healthdata.messaging.topics[0].partitions=3",
                    "healthdata.messaging.topics[0].replicas=1");

    @Test
    void shouldConfigureKafkaTemplateWithJsonSerializer() {
        contextRunner.run(context -> {
            @SuppressWarnings("unchecked")
            ProducerFactory<String, Object> producerFactory = context.getBean(ProducerFactory.class);
            assertThat(producerFactory).isInstanceOf(DefaultKafkaProducerFactory.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> config = ((DefaultKafkaProducerFactory<String, Object>) producerFactory)
                    .getConfigurationProperties();

            assertThat(config.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)).isEqualTo(JsonSerializer.class);

            KafkaTemplate<String, Object> kafkaTemplate = context.getBean("kafkaTemplate", KafkaTemplate.class);
            assertThat(kafkaTemplate).isNotNull();
        });
    }

    @Test
    void shouldRegisterConfiguredTopics() {
        contextRunner.run(context -> {
            Object bean = context.getBean("kafkaTopics");
            assertThat(bean).isInstanceOf(List.class);

            @SuppressWarnings("unchecked")
            List<NewTopic> topics = (List<NewTopic>) bean;
            assertThat(topics).hasSize(1);

            NewTopic topic = topics.get(0);
            assertThat(topic.name()).isEqualTo("messaging-test-topic");
            assertThat(topic.numPartitions()).isEqualTo(3);
        });
    }
}
