package com.healthdata.migration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Migration Workflow Service Application
 *
 * Provides end-to-end migration infrastructure for healthcare data:
 * - Job management with progress tracking and resumability
 * - Source connectors (File, SFTP, MLLP)
 * - Data quality reporting and error tracking
 * - WebSocket progress streaming
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.migration",
    "com.healthdata.security",
    "com.healthdata.audit",
    "com.healthdata.persistence"
})
@EnableJpaRepositories(basePackages = {
    "com.healthdata.migration.repository",
    "com.healthdata.authentication.repository"
})
@EntityScan(basePackages = {
    "com.healthdata.migration.persistence",
    "com.healthdata.audit.entity",
    "com.healthdata.authentication.entity",
    "com.healthdata.authentication.domain"
})
@EnableAsync
@EnableScheduling
public class MigrationWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(MigrationWorkflowApplication.class, args);
    }
}
