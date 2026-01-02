package com.healthdata.apidocs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Auto-configuration for OpenAPI documentation.
 *
 * Provides consistent API documentation across all HDIM microservices.
 *
 * Configuration Properties:
 * - springdoc.api-docs.enabled: Enable/disable OpenAPI (default: true)
 * - springdoc.swagger-ui.enabled: Enable/disable Swagger UI (default: true)
 * - hdim.openapi.title: API title (default: HDIM Service API)
 * - hdim.openapi.description: API description
 * - hdim.openapi.version: API version (default: 1.0.0)
 *
 * Access Points:
 * - Swagger UI: /swagger-ui.html
 * - OpenAPI JSON: /v3/api-docs
 * - OpenAPI YAML: /v3/api-docs.yaml
 *
 * Usage:
 * Services can either:
 * 1. Use this auto-configuration (default, no extra code needed)
 * 2. Provide their own OpenAPI bean to customize documentation
 */
@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiAutoConfiguration {

    @Value("${hdim.openapi.title:HDIM Service API}")
    private String title;

    @Value("${hdim.openapi.description:Health Data in Motion microservice API}")
    private String description;

    @Value("${hdim.openapi.version:1.0.0}")
    private String version;

    @Value("${spring.application.name:hdim-service}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * Default OpenAPI configuration.
     * Will not be created if the service provides its own OpenAPI bean.
     */
    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI hdimOpenAPI() {
        return new OpenAPI()
            .info(buildInfo())
            .servers(buildServers())
            .components(buildSecurityComponents())
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .addSecurityItem(new SecurityRequirement().addList("cookieAuth"));
    }

    private Info buildInfo() {
        return new Info()
            .title(title)
            .description(buildDescription())
            .version(version)
            .contact(new Contact()
                .name("HealthData-in-Motion Team")
                .email("support@healthdata-in-motion.com")
                .url("https://healthdata-in-motion.com"))
            .license(new License()
                .name("Proprietary")
                .url("https://healthdata-in-motion.com/terms"));
    }

    private String buildDescription() {
        return description + "\n\n" +
            "**Authentication:**\n" +
            "- Bearer token (JWT) via `Authorization: Bearer <token>` header\n" +
            "- HttpOnly cookies (for browser clients)\n\n" +
            "**Multi-Tenancy:**\n" +
            "- Include `X-Tenant-ID` header for tenant-specific operations\n\n" +
            "**Rate Limiting:**\n" +
            "- Standard: 100 req/sec\n" +
            "- Premium: 500 req/sec\n" +
            "- Enterprise: 2000 req/sec\n\n" +
            "**Service:** " + applicationName;
    }

    private List<Server> buildServers() {
        return List.of(
            new Server()
                .url("http://localhost:" + serverPort)
                .description("Local development"),
            new Server()
                .url("https://api.healthdata-in-motion.com")
                .description("Production"),
            new Server()
                .url("https://staging-api.healthdata-in-motion.com")
                .description("Staging")
        );
    }

    private Components buildSecurityComponents() {
        return new Components()
            .addSecuritySchemes("bearerAuth", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT access token obtained from /api/v1/auth/login"))
            .addSecuritySchemes("cookieAuth", new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("hdim_access_token")
                .description("HttpOnly cookie authentication (for browser clients)"))
            .addSecuritySchemes("tenantHeader", new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-Tenant-ID")
                .description("Tenant identifier for multi-tenant operations"));
    }
}
