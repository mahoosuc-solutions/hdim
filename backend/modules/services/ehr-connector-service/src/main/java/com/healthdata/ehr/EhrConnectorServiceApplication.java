package com.healthdata.ehr;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * EHR Connector Service Application.
 *
 * Provides a pluggable adapter framework for connecting to various EHR systems including:
 * - Epic (FHIR R4)
 * - Cerner/Oracle Health (FHIR R4)
 * - athenahealth
 * - Generic FHIR-compliant systems
 *
 * Features:
 * - Multi-tenant support
 * - OAuth2 client credentials authentication
 * - Circuit breaker and retry patterns for fault tolerance
 * - Normalized data models across vendors
 * - Real-time data synchronization
 * - Connection health monitoring
 *
 * @author HDIM Platform Team
 */
@SpringBootApplication(scanBasePackages = {
        "com.healthdata.ehr",
        "com.healthdata.common",
        "com.healthdata.authentication",
        "com.healthdata.audit",
        "com.healthdata.messaging",
        "com.healthdata.persistence",
        "com.healthdata.cache"
})
@EnableConfigurationProperties
@EnableCaching
@EnableAsync
@EnableScheduling
@OpenAPIDefinition(
        info = @Info(
                title = "EHR Connector Service API",
                version = "1.0",
                description = "Pluggable adapter framework for EHR system integration"
        )
)
@SecurityScheme(
        name = "bearer-jwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class EhrConnectorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EhrConnectorServiceApplication.class, args);
    }
}
