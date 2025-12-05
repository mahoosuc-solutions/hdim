package com.healthdata.cql.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for CQL Engine Service
 *
 * Provides interactive API documentation at:
 * - Swagger UI: /swagger-ui.html
 * - OpenAPI JSON: /v3/api-docs
 *
 * Documents all 52 HEDIS measures and evaluation endpoints.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cqlEngineOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("CQL Engine Service - HEDIS Quality Measures API")
                .description(
                    "REST API for evaluating HEDIS quality measures.\n\n" +
                    "**Features:**\n" +
                    "- 52 HEDIS measures (100% coverage)\n" +
                    "- Single measure or batch evaluation\n" +
                    "- Patient quality dashboard\n" +
                    "- Care gap detection and prioritization\n" +
                    "- Multi-tenant support via X-Tenant-ID header\n" +
                    "- Redis caching for performance\n" +
                    "- Async/parallel evaluation\n\n" +
                    "**Measure Domains:**\n" +
                    "- Preventive Care & Screening (13 measures)\n" +
                    "- Chronic Disease Management (15 measures)\n" +
                    "- Behavioral Health & Substance Use (16 measures)\n" +
                    "- Utilization & Access (8 measures)"
                )
                .version("1.0.0")
                .contact(new Contact()
                    .name("HealthData-in-Motion Team")
                    .email("support@healthdata-in-motion.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8081")
                    .description("Local development server"),
                new Server()
                    .url("https://api.healthdata-in-motion.com")
                    .description("Production server")
            ));
    }
}
