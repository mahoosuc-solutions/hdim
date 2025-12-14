package com.healthdata.hcc.config;

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
 * OpenAPI/Swagger configuration for HCC Risk Adjustment Service.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8105}")
    private int serverPort;

    @Bean
    public OpenAPI hccOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("HCC Risk Adjustment Service API")
                .description("""
                    HCC V24/V28 Risk Adjustment and Documentation Gap Management API.

                    ## Overview
                    This service provides CMS-HCC risk adjustment functionality supporting
                    both V24 and V28 models during the transition period (2024-2026).

                    ## Key Features
                    - **Dual Model RAF Calculation**: V24, V28, and blended RAF scores
                    - **ICD-10 to HCC Crosswalk**: V24→V28 mapping with change tracking
                    - **Documentation Gap Detection**: Identify coding opportunities
                    - **Recapture Tracking**: Annual chronic condition recapture
                    - **Population Analytics**: High-value opportunity identification

                    ## CMS-HCC Model Transition
                    - **2024**: 67% V24 + 33% V28 (blended)
                    - **2025**: 33% V24 + 67% V28 (blended)
                    - **2026+**: 100% V28

                    ## Authentication
                    All endpoints require JWT authentication via Bearer token.
                    Include `X-Tenant-ID` header for multi-tenant operations.

                    ## Data Sources
                    RAF coefficients and HCC mappings derived from CMS annual releases.
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
                    .name("HCC Risk Adjustment")
                    .description("RAF score calculation and HCC profile management"),
                new Tag()
                    .name("Documentation Gaps")
                    .description("Documentation gap detection and tracking"),
                new Tag()
                    .name("Recapture")
                    .description("Annual HCC recapture opportunity tracking"),
                new Tag()
                    .name("Crosswalk")
                    .description("ICD-10-CM to HCC code mapping")))
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT authentication token")))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
