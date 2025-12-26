package com.healthdata.priorauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Test application configuration for Prior Authorization Service.
 *
 * This configuration excludes authentication entity scanning to allow
 * tests to run without requiring the full authentication infrastructure.
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.healthdata.priorauth"
    },
    exclude = {
        org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class
    }
)
@EnableCaching
@EnableAsync
@EntityScan(basePackages = {
    "com.healthdata.priorauth.persistence"
})
@EnableJpaRepositories(basePackages = {
    "com.healthdata.priorauth.persistence"
})
public class TestPriorAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestPriorAuthApplication.class, args);
    }
}
