package com.healthdata.migration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

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
@EnableAsync
@EnableScheduling
public class MigrationWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(MigrationWorkflowApplication.class, args);
    }
}
