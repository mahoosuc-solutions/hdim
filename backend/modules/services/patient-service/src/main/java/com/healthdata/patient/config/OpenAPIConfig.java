package com.healthdata.patient.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 configuration for Patient Service API documentation.
 *
 * <p>Provides comprehensive API documentation for patient management operations including:
 * - Patient CRUD operations
 * - Patient timeline and history
 * - Allergies and medications
 * - Immunizations and procedures
 * - Search and filtering
 *
 * <p>HIPAA Compliance Notes:
 * - All endpoints require JWT authentication
 * - Multi-tenant isolation enforced via X-Tenant-ID header
 * - All patient data access is audited
 * - PHI responses include Cache-Control: no-store headers
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI patientServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HDIM Patient Service API")
                        .version("1.0.0")
                        .description("""
                                Patient management and clinical data API for the HealthData-in-Motion platform.

                                ## Overview
                                The Patient Service manages comprehensive patient records, clinical history, and demographics
                                for healthcare quality measure evaluation and care gap identification.

                                ## Key Features
                                - **Patient Management**: Create, read, update, and delete patient records
                                - **Clinical Timeline**: Retrieve patient history and clinical events
                                - **Allergies & Medications**: Manage patient allergies and medication lists
                                - **Immunizations**: Track patient immunization records
                                - **Procedures**: Record and retrieve patient procedures
                                - **Multi-Tenant Isolation**: Automatic tenant-level data isolation
                                - **HIPAA Compliance**: Full audit logging and PHI protection

                                ## Authentication
                                All endpoints require JWT Bearer authentication. Obtain a token from the
                                Authentication Service before making API calls.

                                ## Multi-Tenancy
                                All requests must include the `X-Tenant-ID` header for proper tenant isolation.
                                The system automatically filters all data by tenant to prevent cross-tenant data access.

                                ## Rate Limiting
                                - Standard endpoints: 100 requests/minute per user
                                - Bulk operations: 10 requests/minute per user
                                - Search endpoints: 50 requests/minute per user
                                """)
                        .contact(new Contact()
                                .name("HDIM Development Team")
                                .email("dev@healthdata.com")
                                .url("https://healthdata.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://healthdata.com/license")))
                .addServersItem(new Server()
                        .url("http://localhost:8084")
                        .description("Development Server (Direct Service Access)"))
                .addServersItem(new Server()
                        .url("http://localhost:18080/patient")
                        .description("Development Server (via API Gateway)"))
                .addServersItem(new Server()
                        .url("https://api.healthdata.com/patient")
                        .description("Production Server"))
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("""
                                                JWT Bearer token from the Authentication Service.

                                                **How to obtain a token:**
                                                1. POST to `/api/v1/auth/login` with credentials
                                                2. Extract the `accessToken` from the response
                                                3. Click "Authorize" button above and enter: `Bearer <token>`

                                                **Token format:** `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`

                                                **Token expiration:** 1 hour (configurable)
                                                """)));
    }
}
