package com.healthdata.caregap.config;

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
 * OpenAPI 3.0 configuration for Care Gap Service API documentation.
 *
 * <p>Provides comprehensive API documentation for care gap management operations including:
 * - Care gap identification and detection
 * - Care gap closure and tracking
 * - Bulk care gap operations
 * - Provider-specific care gaps
 * - Care gap analytics and reporting
 *
 * <p>HIPAA Compliance Notes:
 * - All endpoints require JWT authentication
 * - Multi-tenant isolation enforced via X-Tenant-ID header
 * - All care gap operations are audited
 * - PHI responses include Cache-Control: no-store headers
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI careGapServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HDIM Care Gap Service API")
                        .version("1.0.0")
                        .description("""
                                Care gap identification, closure, and tracking API for the HealthData-in-Motion platform.

                                ## Overview
                                The Care Gap Service identifies clinical care gaps based on HEDIS quality measures,
                                tracks closure progress, and provides analytics for value-based care contracts.

                                ## Key Features
                                - **Care Gap Detection**: Automated identification using CQL evaluation
                                - **Gap Closure Tracking**: Monitor progress towards closing identified gaps
                                - **Bulk Operations**: Bulk close, assign, and prioritize care gaps
                                - **Provider Assignment**: Assign care gaps to specific providers
                                - **Analytics**: Care gap reports, trends, and KPIs
                                - **Multi-Tenant Isolation**: Automatic tenant-level data isolation
                                - **HIPAA Compliance**: Full audit logging and PHI protection

                                ## Care Gap Lifecycle
                                1. **Identified**: Gap detected via quality measure evaluation
                                2. **Assigned**: Gap assigned to a provider
                                3. **In Progress**: Provider working on gap closure
                                4. **Closed**: Gap successfully addressed
                                5. **Excluded**: Gap marked as excluded (with reason)

                                ## Authentication
                                All endpoints require JWT Bearer authentication. Obtain a token from the
                                Authentication Service before making API calls.

                                ## Multi-Tenancy
                                All requests must include the `X-Tenant-ID` header for proper tenant isolation.
                                The system automatically filters all data by tenant to prevent cross-tenant data access.

                                ## Rate Limiting
                                - Standard endpoints: 100 requests/minute per user
                                - Bulk operations: 10 requests/minute per user
                                - Analytics endpoints: 50 requests/minute per user
                                """)
                        .contact(new Contact()
                                .name("HDIM Development Team")
                                .email("dev@healthdata.com")
                                .url("https://healthdata.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://healthdata.com/license")))
                .addServersItem(new Server()
                        .url("http://localhost:8086")
                        .description("Development Server (Direct Service Access)"))
                .addServersItem(new Server()
                        .url("http://localhost:18080/care-gap")
                        .description("Development Server (via API Gateway)"))
                .addServersItem(new Server()
                        .url("https://api.healthdata.com/care-gap")
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

                                                **Required Roles:**
                                                - ADMIN: Full access to all endpoints
                                                - EVALUATOR: Create and close care gaps
                                                - ANALYST: Read-only access to analytics
                                                """)));
    }
}
