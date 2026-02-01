package com.healthdata.quality.config;

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
 * OpenAPI 3.0 configuration for Quality Measure Service API documentation.
 *
 * <p>Provides comprehensive API documentation for quality measure operations including:
 * - HEDIS and CMS quality measure calculation
 * - Batch evaluation jobs
 * - Measure reports (saved, scheduled, exported)
 * - Measure definitions and metadata
 * - Numerator/denominator analysis
 *
 * <p>HIPAA Compliance Notes:
 * - All endpoints require JWT authentication
 * - Multi-tenant isolation enforced via X-Tenant-ID header
 * - All measure evaluations are audited
 * - PHI responses include Cache-Control: no-store headers
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI qualityMeasureServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HDIM Quality Measure Service API")
                        .version("1.0.0")
                        .description("""
                                Quality measure calculation, evaluation, and reporting API for the HealthData-in-Motion platform.

                                ## Overview
                                The Quality Measure Service evaluates HEDIS and CMS quality measures using Clinical Quality
                                Language (CQL), tracks measure performance, and generates compliance reports for value-based
                                care contracts.

                                ## Key Features
                                - **Measure Evaluation**: Real-time and batch quality measure calculations
                                - **HEDIS Support**: Comprehensive HEDIS measure library (100+ measures)
                                - **CMS Measures**: CMS quality measures for Medicare/Medicaid reporting
                                - **Batch Jobs**: Scheduled evaluation jobs for large patient populations
                                - **Report Generation**: Saved reports, scheduled reports, and exports (CSV, Excel, PDF)
                                - **Numerator/Denominator Analysis**: Detailed breakdown of measure calculations
                                - **Multi-Tenant Isolation**: Automatic tenant-level data isolation
                                - **HIPAA Compliance**: Full audit logging and PHI protection

                                ## Supported Measure Sets
                                - **HEDIS 2024**: 120+ measures across 8 clinical domains
                                - **CMS Stars**: Medicare Advantage Star Ratings measures
                                - **ACO MSSP**: Medicare Shared Savings Program measures
                                - **Custom Measures**: Organization-specific quality measures

                                ## Evaluation Process
                                1. **Submit Evaluation**: Specify measure, patient population, and time period
                                2. **CQL Execution**: CQL Engine evaluates measure logic
                                3. **Calculate Results**: Numerator, denominator, exclusions computed
                                4. **Generate Report**: Results formatted for reporting/export
                                5. **Identify Care Gaps**: Failed measures trigger care gap detection

                                ## Authentication
                                All endpoints require JWT Bearer authentication. Obtain a token from the
                                Authentication Service before making API calls.

                                ## Multi-Tenancy
                                All requests must include the `X-Tenant-ID` header for proper tenant isolation.
                                The system automatically filters all data by tenant to prevent cross-tenant data access.

                                ## Rate Limiting
                                - Standard endpoints: 100 requests/minute per user
                                - Batch evaluation: 5 requests/minute per user (long-running)
                                - Report generation: 20 requests/minute per user
                                """)
                        .contact(new Contact()
                                .name("HDIM Development Team")
                                .email("dev@healthdata.com")
                                .url("https://healthdata.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://healthdata.com/license")))
                .addServersItem(new Server()
                        .url("http://localhost:8087")
                        .description("Development Server (Direct Service Access)"))
                .addServersItem(new Server()
                        .url("http://localhost:18080/quality-measure")
                        .description("Development Server (via API Gateway)"))
                .addServersItem(new Server()
                        .url("https://api.healthdata.com/quality-measure")
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
                                                - EVALUATOR: Run evaluations and generate reports
                                                - ANALYST: Read-only access to reports and results
                                                - VIEWER: Read-only access to measure definitions
                                                """)));
    }
}
