package com.healthdata.clinicalworkflow.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for Clinical Workflow Service
 *
 * Provides comprehensive API documentation accessible at:
 * - Swagger UI: http://localhost:8088/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8088/v3/api-docs
 *
 * @author HDIM Platform Team
 * @version 1.0
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @Bean
    public OpenAPI clinicalWorkflowOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(apiServers())
                .components(securityComponents())
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }

    private Info apiInfo() {
        return new Info()
                .title("Clinical Workflow Service API")
                .description("""
                        RESTful API for clinical workflow management including:
                        - Patient check-in and arrival tracking
                        - Vital signs recording and monitoring
                        - Exam room management and assignments
                        - Patient queue management across workflow stages
                        - Pre-visit checklist workflow

                        **Authentication**: All endpoints require JWT bearer token authentication.

                        **Multi-Tenancy**: All requests must include X-Tenant-ID header for tenant isolation.

                        **HIPAA Compliance**: All PHI access is audited and responses include cache-control headers.

                        **Error Handling**: Standardized error responses with HIPAA-safe messaging.
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("HDIM Platform Team")
                        .email("support@healthdata.example.com")
                        .url("https://healthdata.example.com"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://healthdata.example.com/license"));
    }

    private List<Server> apiServers() {
        return List.of(
                new Server()
                        .url("http://localhost:8088")
                        .description("Local development server"),
                new Server()
                        .url("http://localhost:8001/clinical-workflow")
                        .description("Development via API Gateway"),
                new Server()
                        .url("https://api.healthdata.example.com/clinical-workflow")
                        .description("Production API Gateway")
        );
    }

    private Components securityComponents() {
        return new Components()
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                        .name("bearer-jwt")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT token obtained from authentication service. " +
                                "Include in Authorization header as: Bearer {token}"));
    }
}
