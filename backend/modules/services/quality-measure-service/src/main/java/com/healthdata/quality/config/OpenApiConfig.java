package com.healthdata.quality.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for Quality Measure Service
 *
 * Provides interactive API documentation at:
 * - Swagger UI: /quality-measure/swagger-ui.html
 * - OpenAPI JSON: /quality-measure/v3/api-docs
 *
 * Documents all quality measure calculation and reporting endpoints.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI qualityMeasureOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Quality Measure Service - HEDIS Quality Measures API")
                .description(
                    "REST API for calculating and reporting HEDIS quality measures.\n\n" +
                    "**Features:**\n" +
                    "- Calculate quality measures for individual patients\n" +
                    "- Retrieve quality measure results and history\n" +
                    "- Generate quality scores and compliance rates\n" +
                    "- Multi-tenant support via X-Tenant-ID header\n" +
                    "- Integration with CQL Engine for measure evaluation\n" +
                    "- Redis caching for performance\n" +
                    "- Event-driven architecture with Kafka\n\n" +
                    "**Supported Measures:**\n" +
                    "- Comprehensive Diabetes Care (CDC)\n" +
                    "- Controlling High Blood Pressure (CBP)\n" +
                    "- Breast Cancer Screening (BCS)\n" +
                    "- Cervical Cancer Screening (CCS)\n" +
                    "- Colorectal Cancer Screening (COL)\n" +
                    "- And more HEDIS measures..."
                )
                .version("1.0.7")
                .contact(new Contact()
                    .name("HealthData-in-Motion Team")
                    .email("support@healthdata-in-motion.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8087")
                    .description("Local development server"),
                new Server()
                    .url("https://api.healthdata-in-motion.com")
                    .description("Production server")
            ));
    }
}
