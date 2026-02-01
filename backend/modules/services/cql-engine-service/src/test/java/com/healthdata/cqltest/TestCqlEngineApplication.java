package com.healthdata.cqltest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;

/**
 * Minimal Spring Boot application for testing cql-engine-service.
 *
 * This application excludes external dependencies (Kafka, Redis) and uses
 * a minimal component scan to reduce Spring context initialization time
 * and prevent conflicts with shared infrastructure beans.
 *
 * Key features:
 * - Scans only com.healthdata.cql package
 * - Excludes Kafka and all its variants, Redis auto-configurations
 * - Only scans CQL persistence entities
 * - Enables JPA repositories only for CQL domain
 *
 * The CQL Engine service processes FHIR measures and CQL expressions,
 * requiring careful test isolation to prevent timing issues with
 * expression evaluation and cache operations.
 *
 * @author HDIM Platform Team
 */
@SpringBootApplication(
        scanBasePackages = {"com.healthdata.cql"},
        exclude = {
                KafkaAutoConfiguration.class,
                RedisAutoConfiguration.class,
                RedisRepositoriesAutoConfiguration.class
        }
)
public class TestCqlEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestCqlEngineApplication.class, args);
    }
}
