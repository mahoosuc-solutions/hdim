package com.healthdata.messaging.annotation;

import com.healthdata.messaging.config.TestKafkaTopicsConfiguration;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

/**
 * Meta-annotation combining @SpringBootTest, @EmbeddedKafka, and test profile.
 * <p>
 * Replaces repetitive decorator pattern:
 * <pre>{@code
 * @SpringBootTest
 * @EmbeddedKafka(partitions = 3)
 * @ActiveProfiles("test")
 * }</pre>
 * <p>
 * With simple:
 * <pre>{@code
 * @EnableEmbeddedKafka
 * }</pre>
 * <p>
 * Enables embedded Kafka broker for integration tests without Docker.
 * Automatically activates "test" profile for Spring configuration.
 * <p>
 * <strong>Topic Pre-creation:</strong> Imports {@link TestKafkaTopicsConfiguration}
 * which pre-creates common HDIM topics to prevent topic timeout warnings and
 * reduce test initialization delays.
 *
 * @see TestKafkaTopicsConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@EmbeddedKafka(
    partitions = 3,
    topics = {},  // Topics pre-created by TestKafkaTopicsConfiguration
    bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@Import(TestKafkaTopicsConfiguration.class)
@ActiveProfiles("test")
public @interface EnableEmbeddedKafka {
}
