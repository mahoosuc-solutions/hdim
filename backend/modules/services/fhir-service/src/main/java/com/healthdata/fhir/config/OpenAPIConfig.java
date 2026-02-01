package com.healthdata.fhir.config;

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
 * OpenAPI 3.0 configuration for FHIR Service API documentation.
 *
 * <p>Provides comprehensive API documentation for FHIR R4 resource operations including:
 * - Patient, Observation, Condition, Encounter resources
 * - MedicationRequest, AllergyIntolerance resources
 * - FHIR CRUD operations (create, read, update, delete, search)
 * - FHIR search parameters and modifiers
 * - SMART on FHIR OAuth 2.0 support
 *
 * <p>HIPAA Compliance Notes:
 * - All endpoints require JWT authentication or SMART on FHIR OAuth
 * - Multi-tenant isolation enforced via X-Tenant-ID header
 * - All FHIR resource access is audited
 * - PHI responses include Cache-Control: no-store headers
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI fhirServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HDIM FHIR Service API")
                        .version("1.0.0")
                        .description("""
                                FHIR R4-compliant resource management API for the HealthData-in-Motion platform.

                                ## Overview
                                The FHIR Service provides a standards-compliant FHIR R4 API for managing clinical resources,
                                enabling interoperability with EHR systems, health information exchanges, and external applications.

                                ## Key Features
                                - **FHIR R4 Compliance**: Full support for FHIR R4 specification
                                - **Resource Types**: Patient, Observation, Condition, Encounter, MedicationRequest, AllergyIntolerance
                                - **CRUD Operations**: Create, Read, Update, Delete for all supported resources
                                - **Search**: Advanced search with FHIR search parameters and modifiers
                                - **SMART on FHIR**: OAuth 2.0 authorization for third-party apps
                                - **Bulk Data Export**: FHIR Bulk Data Access ($export)
                                - **Subscriptions**: Real-time notifications via WebSocket
                                - **Multi-Tenant Isolation**: Automatic tenant-level data isolation
                                - **HIPAA Compliance**: Full audit logging and PHI protection

                                ## Supported FHIR Resources
                                This Phase 1 documentation covers the following resource types:
                                - **Patient**: Patient demographics and identifiers
                                - **Observation**: Clinical observations (vitals, lab results, etc.)
                                - **Condition**: Patient conditions and diagnoses
                                - **Encounter**: Patient encounters with healthcare providers
                                - **MedicationRequest**: Medication prescriptions and orders
                                - **AllergyIntolerance**: Patient allergies and intolerances

                                ## FHIR Operations
                                - **Create**: `POST /fhir/{resourceType}`
                                - **Read**: `GET /fhir/{resourceType}/{id}`
                                - **Update**: `PUT /fhir/{resourceType}/{id}`
                                - **Delete**: `DELETE /fhir/{resourceType}/{id}`
                                - **Search**: `GET /fhir/{resourceType}?{searchParams}`
                                - **History**: `GET /fhir/{resourceType}/{id}/_history`

                                ## FHIR Search Parameters
                                Each resource type supports standard FHIR search parameters:
                                - **Patient**: name, birthdate, identifier, gender
                                - **Observation**: patient, code, date, category, value-quantity
                                - **Condition**: patient, code, onset-date, clinical-status
                                - **Encounter**: patient, date, type, class
                                - **MedicationRequest**: patient, medication, status, date
                                - **AllergyIntolerance**: patient, code, clinical-status, type

                                ## Authentication
                                Supports two authentication methods:
                                1. **JWT Bearer**: Standard HDIM JWT token from Authentication Service
                                2. **SMART on FHIR OAuth**: OAuth 2.0 access token for external apps

                                ## Multi-Tenancy
                                All requests must include the `X-Tenant-ID` header for proper tenant isolation.
                                The system automatically filters all data by tenant to prevent cross-tenant data access.

                                ## Rate Limiting
                                - CRUD operations: 100 requests/minute per user
                                - Search operations: 50 requests/minute per user
                                - Bulk export: 5 requests/hour per tenant
                                """)
                        .contact(new Contact()
                                .name("HDIM Development Team")
                                .email("dev@healthdata.com")
                                .url("https://healthdata.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://healthdata.com/license")))
                .addServersItem(new Server()
                        .url("http://localhost:8085")
                        .description("Development Server (Direct Service Access)"))
                .addServersItem(new Server()
                        .url("http://localhost:18080/fhir")
                        .description("Development Server (via API Gateway)"))
                .addServersItem(new Server()
                        .url("https://api.healthdata.com/fhir")
                        .description("Production Server"))
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"))
                .addSecurityItem(new SecurityRequirement()
                        .addList("SMART on FHIR OAuth"))
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

                                                **Required Roles:**
                                                - ADMIN: Full access to all resources
                                                - EVALUATOR: Read/write access to clinical resources
                                                - ANALYST: Read-only access to all resources
                                                """))
                        .addSecuritySchemes("SMART on FHIR OAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.OAUTH2)
                                        .flows(new io.swagger.v3.oas.models.security.OAuthFlows()
                                                .authorizationCode(new io.swagger.v3.oas.models.security.OAuthFlow()
                                                        .authorizationUrl("http://localhost:18080/oauth/authorize")
                                                        .tokenUrl("http://localhost:18080/oauth/token")
                                                        .refreshUrl("http://localhost:18080/oauth/refresh")
                                                        .scopes(new io.swagger.v3.oas.models.security.Scopes()
                                                                .addString("patient/*.read", "Read patient resources")
                                                                .addString("patient/*.write", "Write patient resources")
                                                                .addString("user/*.read", "Read user resources")
                                                                .addString("user/*.write", "Write user resources")
                                                                .addString("launch", "Launch context")
                                                                .addString("launch/patient", "Patient launch context"))))
                                        .description("""
                                                SMART on FHIR OAuth 2.0 authorization for external applications.

                                                **Authorization Flow:**
                                                1. Redirect user to authorization URL
                                                2. User grants consent
                                                3. Exchange authorization code for access token
                                                4. Use access token to make FHIR API requests

                                                **Scopes:**
                                                - `patient/*.read`: Read all patient compartment resources
                                                - `patient/*.write`: Write all patient compartment resources
                                                - `user/*.read`: Read all user compartment resources
                                                - `launch`: Launch context for EHR integration
                                                - `launch/patient`: Patient-specific launch context

                                                **Token expiration:** 1 hour (access token), 30 days (refresh token)
                                                """)));
    }
}
