package com.healthdata.priorauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Prior Authorization Service Application.
 *
 * Implements CMS Interoperability and Prior Authorization Rule (CMS-0057-F)
 * effective January 1, 2027.
 *
 * Key Capabilities:
 * - FHIR-based Prior Authorization (PAS) using Da Vinci Implementation Guide
 * - Payer API integration for PA request/response
 * - Provider Access API for claims and clinical data exchange
 * - Real-time PA status tracking and notifications
 * - SLA compliance monitoring (72hr for urgent, 7 days for standard)
 *
 * @see <a href="https://hl7.org/fhir/us/davinci-pas/">Da Vinci PAS Implementation Guide</a>
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.priorauth",
    "com.healthdata.common",
    "com.healthdata.security",
    "com.healthdata.persistence",
    "com.healthdata.audit"
})
@EnableJpaRepositories(basePackages = {
    "com.healthdata.priorauth.persistence",
    "com.healthdata.persistence"
})
@EntityScan(basePackages = {
    "com.healthdata.priorauth.persistence",
    "com.healthdata.persistence"
})
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableFeignClients
@EnableKafka
public class PriorAuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PriorAuthServiceApplication.class, args);
    }
}
