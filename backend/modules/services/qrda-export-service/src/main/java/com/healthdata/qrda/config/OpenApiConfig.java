package com.healthdata.qrda.config;

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
 * OpenAPI/Swagger configuration for QRDA Export Service.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8104}")
    private int serverPort;

    @Bean
    public OpenAPI qrdaOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("QRDA Export Service API")
                .description("""
                    Quality Reporting Document Architecture (QRDA) Export API for
                    CMS quality measure reporting.

                    ## Overview
                    This service generates QRDA Category I (patient-level) and Category III
                    (aggregate) documents for CMS quality measure submissions.

                    ## Key Features
                    - **QRDA Category I**: Individual patient quality data reports
                    - **QRDA Category III**: Aggregate population quality data reports
                    - **CMS Submission Tracking**: Track submission status and history
                    - **Validation**: Schema and Schematron validation against CMS standards

                    ## QRDA Categories
                    | Category | Purpose | Use Case |
                    |----------|---------|----------|
                    | QRDA I | Patient-level data | Individual patient quality reports |
                    | QRDA III | Aggregate data | Population-level summary reports |

                    ## CMS Programs Supported
                    - Merit-based Incentive Payment System (MIPS)
                    - Medicare Shared Savings Program (MSSP)
                    - Accountable Care Organizations (ACO)
                    - Hospital Quality Reporting (HQR)

                    ## Document Standards
                    - HL7 CDA R2 Implementation Guide for QRDA
                    - CMS eCQM specifications
                    - CMS Web Interface sunset preparation (2025)

                    ## Authentication
                    All endpoints require JWT authentication via Bearer token.
                    Include `X-Tenant-Id` header for multi-tenant operations.
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
                    .name("QRDA Category I")
                    .description("Patient-level quality reporting document generation"),
                new Tag()
                    .name("QRDA Category III")
                    .description("Aggregate quality reporting document generation"),
                new Tag()
                    .name("Export Jobs")
                    .description("QRDA export job management and status tracking"),
                new Tag()
                    .name("CMS Submissions")
                    .description("CMS submission tracking and history")))
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT authentication token")))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
