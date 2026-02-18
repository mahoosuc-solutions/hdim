package com.healthdata.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Test-only application class for analytics-service controller tests.
 *
 * Replaces AnalyticsServiceApplication for controller-layer tests to avoid
 * scanning com.healthdata.audit (which requires Kafka and JPA infrastructure
 * not needed for controller-only tests).
 *
 * Scans only com.healthdata.analytics — the service's own package.
 */
@SpringBootApplication(scanBasePackages = "com.healthdata.analytics")
@EntityScan(basePackages = "com.healthdata.analytics.persistence")
@EnableJpaRepositories(basePackages = "com.healthdata.analytics.repository")
@EnableScheduling
public class TestAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestAnalyticsApplication.class, args);
    }
}
