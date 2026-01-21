package com.healthdata.caregap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import com.healthdata.audit.service.ai.AIAuditEventPublisher;

/**
 * Care Gap Service - Quality measure gap identification and closure
 *
 * Identifies care gaps for HEDIS measures using CQL rules, provides recommendations,
 * and tracks gap closures for quality improvement. Integrates with Care Gap Service
 * for patient data and CQL Engine for rule evaluation.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.healthdata.caregap"
})
@Import(AIAuditEventPublisher.class)
@EnableFeignClients
@EnableCaching
@EnableJpaRepositories(basePackages = {
    "com.healthdata.caregap.persistence"  // Service repositories
})
@EntityScan(basePackages = {
    "com.healthdata.caregap.persistence",  // Service entities
    "com.healthdata.caregap.entity"  // Legacy service entities
})
public class CareGapServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareGapServiceApplication.class, args);
    }
}
