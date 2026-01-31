package com.healthdata.cdr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * CDR (Clinical Data Repository) Processor Service Application.
 *
 * Provides HL7 v2/v3 message processing capabilities including:
 * - HL7 v2 message parsing (ADT, ORU, ORM)
 * - HL7 to FHIR conversion
 * - Multi-tenant support
 * - Audit logging and DLQ integration
 * - Batch message processing
 *
 * Supports:
 * - ADT^A01, A02, A03, A04, A08 (Admit/Discharge/Transfer)
 * - ORU^R01 (Lab Results)
 * - ORM^O01 (Lab Orders)
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.cdr",
    "com.healthdata.common",
    "com.healthdata.authentication",
    "com.healthdata.audit",
    "com.healthdata.messaging",
    "com.healthdata.persistence"
})
@EntityScan(basePackages = {
    "com.healthdata.cdr",
    "com.healthdata.audit.entity",
    "com.healthdata.authentication.domain"
})
@EnableConfigurationProperties
@EnableCaching
@EnableAsync
@EnableScheduling
public class CdrProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CdrProcessorApplication.class, args);
    }
}
