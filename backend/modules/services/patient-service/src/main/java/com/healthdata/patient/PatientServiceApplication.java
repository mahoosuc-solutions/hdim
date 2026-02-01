package com.healthdata.patient;

import com.healthdata.authentication.config.AuthenticationJwtAutoConfiguration;
import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
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
 * Patient Service - Patient aggregation and timeline service
 *
 * Aggregates FHIR resources from FHIR Service, provides timeline views,
 * health status dashboards, and applies consent filters for patient data.
 *
 * SECURITY: Uses gateway-trust authentication pattern (TrustedHeaderAuthFilter)
 * instead of direct JWT validation. Excludes AuthenticationAutoConfiguration
 * and AuthenticationJwtAutoConfiguration from auto-config since only gateway needs ApiKey entity.
 */
@SpringBootApplication(exclude = {
    AuthenticationAutoConfiguration.class,
    AuthenticationJwtAutoConfiguration.class
})
@ComponentScan(basePackages = {
    "com.healthdata.patient"
})
@Import(AIAuditEventPublisher.class)
@EnableFeignClients
@EnableCaching
@EnableJpaRepositories(basePackages = {
    "com.healthdata.patient.repository",
    "com.healthdata.patient.persistence"  // Includes UserRepository and TenantRepository
})
@EntityScan(basePackages = {
    "com.healthdata.patient.entity",
    "com.healthdata.authentication.domain"  // Enable User/Tenant persistence for UserAutoRegistrationFilter
})
public class PatientServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatientServiceApplication.class, args);
    }
}
