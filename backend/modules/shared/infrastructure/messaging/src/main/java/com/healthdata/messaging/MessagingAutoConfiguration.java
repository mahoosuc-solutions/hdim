package com.healthdata.messaging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Auto-configuration for Kafka messaging infrastructure.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(MessagingProperties.class)
public class MessagingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KafkaAdmin kafkaAdmin(MessagingProperties properties) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        configs.put(AdminClientConfig.CLIENT_ID_CONFIG, properties.getClientId());
        return new KafkaAdmin(configs);
    }

    @Bean
    @ConditionalOnMissingBean(ProducerFactory.class)
    public ProducerFactory<String, Object> kafkaProducerFactory(
            MessagingProperties properties,
            ObjectProvider<ObjectMapper> mapperProvider) {

        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, properties.getClientId());
        configProps.put(ProducerConfig.ACKS_CONFIG, properties.getProducer().getAcks());
        configProps.put(ProducerConfig.RETRIES_CONFIG, properties.getProducer().getRetries());
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, properties.getProducer().getBatchSize());
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, (int) properties.getProducer().getLinger().toMillis());
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, properties.getProducer().isIdempotent());
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, properties.getProducer().getMaxInFlight());
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, properties.getProducer().getCompressionType());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        ObjectMapper mapper = mapperProvider.getIfAvailable(ObjectMapper::new);
        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>(mapper);
        jsonSerializer.setAddTypeInfo(false);
        jsonSerializer.setUseTypeMapperForKey(false);

        return new DefaultKafkaProducerFactory<>(configProps, new StringSerializer(), jsonSerializer);
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        KafkaTemplate<String, Object> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        kafkaTemplate.setObservationEnabled(true);
        return kafkaTemplate;
    }

    @Bean
    @ConditionalOnMissingBean(ConsumerFactory.class)
    public ConsumerFactory<String, Object> kafkaConsumerFactory(MessagingProperties properties) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getConsumer().getGroupId());
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, properties.getConsumer().getAutoOffsetReset());
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, properties.getConsumer().isEnableAutoCommit());
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, String.join(",", properties.getConsumer().getTrustedPackages()));

        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>();
        jsonDeserializer.addTrustedPackages(properties.getConsumer().getTrustedPackages().toArray(String[]::new));
        jsonDeserializer.setRemoveTypeHeaders(true);
        jsonDeserializer.setUseTypeMapperForKey(false);
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            MessagingProperties properties,
            ObjectProvider<ObjectMapper> mapperProvider) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setPollTimeout(properties.getConsumer().getPollTimeout().toMillis());
        factory.setConcurrency(1);
        ObjectMapper mapper = mapperProvider.getIfAvailable(ObjectMapper::new);
        factory.setRecordMessageConverter(new StringJsonMessageConverter(mapper));
        return factory;
    }

    @Bean
    public List<NewTopic> kafkaTopics(MessagingProperties properties) {
        return properties.getTopics().stream()
                .map(topicProps -> {
                    TopicBuilder builder = TopicBuilder.name(topicProps.getName())
                            .partitions(topicProps.getPartitions())
                            .replicas(topicProps.getReplicas());

                    Map<String, String> configs = new HashMap<>();
                    configs.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(topicProps.getRetention().toMillis()));
                    if (topicProps.isCompacted()) {
                        configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT);
                    }

                    if (!configs.isEmpty()) {
                        builder.configs(configs);
                    }
                    return builder.build();
                })
                .toList();
    }
}
