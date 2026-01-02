package com.healthdata.priorauth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for Prior Authorization Service.
 *
 * Provides API documentation per Da Vinci PAS Implementation Guide.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8102}")
    private int serverPort;

    @Bean
    public OpenAPI priorAuthOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Prior Authorization Service API")
                .description("""
                    FHIR-based Prior Authorization API implementing:
                    - **CMS Interoperability and Prior Authorization Rule (CMS-0057-F)**
                    - **Da Vinci Prior Authorization Support (PAS) Implementation Guide**
                    - **CMS Provider Access API**

                    ## Key Features
                    - Create, submit, and track prior authorization requests
                    - Real-time status checking with payer systems
                    - SLA tracking (72hr for STAT, 7 days for ROUTINE)
                    - Provider Access API for claims and coverage data

                    ## Authentication
                    All endpoints require JWT authentication via Bearer token.
                    Include `X-Tenant-Id` header for multi-tenant operations.

                    ## Compliance
                    This API is designed for compliance with:
                    - CMS-0057-F Final Rule (effective Jan 1, 2027)
                    - Da Vinci PAS IG v2.0.1
                    - HL7 FHIR R4
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("HealthData-in-Motion")
                    .email("support@healthdata-in-motion.com")
                    .url("https://healthdata-in-motion.com"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://healthdata-in-motion.com/license")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Local Development"),
                new Server()
                    .url("https://api.healthdata-in-motion.com")
                    .description("Production")))
            .tags(List.of(
                new Tag()
                    .name("Prior Authorization")
                    .description("Prior Authorization request management - submit, track, and manage PA requests"),
                new Tag()
                    .name("Provider Access")
                    .description("CMS Provider Access API - access claims, coverage, and clinical data from payers"),
                new Tag()
                    .name("Payer Configuration")
                    .description("Payer endpoint configuration management")))
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT authentication token")))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
