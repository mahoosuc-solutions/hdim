package com.healthdata.ecr.config;

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
 * OpenAPI/Swagger configuration for Electronic Case Reporting Service.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8101}")
    private int serverPort;

    @Bean
    public OpenAPI ecrOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Electronic Case Reporting (eCR) Service API")
                .description("""
                    Electronic Case Reporting (eCR) API implementing HL7 eCR standards
                    for automated public health reporting.

                    ## Overview
                    This service automates the detection and reporting of reportable conditions
                    to public health authorities via the AIMS (Association of Public Health
                    Laboratories Informatics Messaging Services) platform.

                    ## Key Features
                    - **RCTC Trigger Detection**: Automated detection of reportable conditions
                      using CDC RCTC (Reportable Condition Trigger Codes) value sets
                    - **eICR Generation**: FHIR-based electronic Initial Case Report bundle creation
                    - **AIMS Integration**: Automated transmission to state/local public health
                    - **Reportability Response**: Processing of RR (Reportability Response) from AIMS

                    ## Trigger Categories
                    - **Diagnosis**: ICD-10 codes indicating reportable conditions
                    - **Lab Results**: LOINC codes for reportable test results
                    - **Medications**: RxNorm codes for reportable medications

                    ## Urgency Levels
                    - **IMMEDIATE**: Report within 4 hours (e.g., suspected bioterrorism)
                    - **24_HOURS**: Report within 24 hours (e.g., measles)
                    - **72_HOURS**: Report within 72 hours (standard)

                    ## Compliance
                    - HL7 CDA R2 IG for Public Health Case Reports
                    - HL7 FHIR eCR Implementation Guide
                    - CDC RCTC Value Sets
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
                    .name("eCR Management")
                    .description("Electronic Case Report lifecycle management"),
                new Tag()
                    .name("Trigger Codes")
                    .description("RCTC trigger code configuration and evaluation"),
                new Tag()
                    .name("AIMS Integration")
                    .description("AIMS platform integration and transmission status")))
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT authentication token")))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
