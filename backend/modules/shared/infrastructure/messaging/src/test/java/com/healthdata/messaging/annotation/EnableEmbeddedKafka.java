package com.healthdata.messaging.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

/**
 * Meta-annotation combining @SpringBootTest, @EmbeddedKafka, and test profile.
 *
 * Replaces repetitive decorator pattern:
 * @SpringBootTest
 * @EmbeddedKafka(partitions = 3)
 * @ActiveProfiles("test")
 *
 * With simple:
 * @EnableEmbeddedKafka
 *
 * Enables embedded Kafka broker for integration tests without Docker.
 * Automatically activates "test" profile for Spring configuration.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@EmbeddedKafka(
    partitions = 3,
    topics = {},  // Topics auto-created by TopicInitializer
    bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@ActiveProfiles("test")
public @interface EnableEmbeddedKafka {
}
