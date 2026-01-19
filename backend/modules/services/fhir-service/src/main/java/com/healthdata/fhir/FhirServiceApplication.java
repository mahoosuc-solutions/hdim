package com.healthdata.fhir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.context.annotation.Import;
import com.healthdata.audit.service.ai.AIAuditEventPublisher;

/**
 * FHIR R4 Service Application
 *
 * Provides FHIR resource management with:
 * - 150+ FHIR R4 resource types
 * - HIPAA-compliant audit logging
 * - Redis caching for performance
 * - Kafka event publishing
 * - PostgreSQL persistence
 * - Authentication-based tenant isolation
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.fhir",
    "com.healthdata.common",
    "com.healthdata.fhir.models",
    "com.healthdata.authentication",  // Include for JWT filter and config
    "com.healthdata.cache"  // Include for CacheEvictionService (lazy init to avoid circular dependency)
})
@Import(AIAuditEventPublisher.class)
@EnableJpaRepositories(basePackages = {
    "com.healthdata.fhir.persistence",
    "com.healthdata.fhir.bulk"  // Include bulk export repository
    // NOTE: authentication.repository excluded - contains ApiKey/RefreshToken repos (Gateway-only)
})
@EnableRedisRepositories(basePackages = {})  // Explicitly disable Redis repository scanning (not used for repositories)
@EntityScan(basePackages = {
    "com.healthdata.fhir.persistence",
    "com.healthdata.fhir.bulk",  // Include BulkExportJob entity
    "com.healthdata.authentication.domain"  // Include User and RefreshToken entities (no ApiKey)
})
@EnableCaching
@EnableKafka
public class FhirServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FhirServiceApplication.class, args);
    }
}
