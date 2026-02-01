package com.healthdata.test.config;

import com.healthdata.test.containers.SharedKafkaContainer;
import com.healthdata.test.containers.SharedPostgresContainer;
import com.healthdata.test.containers.SharedRedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Test configuration for Testcontainers.
 * 
 * Provides shared container instances and configures Spring properties.
 * 
 * Usage:
 * <pre>
 * {@code
 * @SpringBootTest
 * @Import(TestContainersConfig.class)
 * class MyHeavyweightTest {
 *     // Containers will be automatically configured
 * }
 * }
 * </pre>
 */
@TestConfiguration
public class TestContainersConfig {
    
    /**
     * Configure Kafka properties from shared container.
     */
    @DynamicPropertySource
    static void configureKafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", SharedKafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", SharedKafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", SharedKafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.group-id", () -> "test-group");
    }
    
    /**
     * Configure PostgreSQL properties from shared container.
     */
    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedPostgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", SharedPostgresContainer::getUsername);
        registry.add("spring.datasource.password", SharedPostgresContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
    
    /**
     * Configure Redis properties from shared container.
     */
    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", SharedRedisContainer::getHost);
        registry.add("spring.data.redis.port", SharedRedisContainer::getPort);
    }
    
    @Bean
    public KafkaContainer kafkaContainer() {
        return SharedKafkaContainer.getInstance();
    }
    
    @Bean
    public PostgreSQLContainer<?> postgresContainer() {
        return SharedPostgresContainer.getInstance();
    }
}

