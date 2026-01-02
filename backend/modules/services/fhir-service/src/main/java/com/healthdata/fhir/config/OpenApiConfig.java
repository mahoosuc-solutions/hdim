package com.healthdata.fhir.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * OpenAPI/Swagger configuration for FHIR R4 API documentation.
 *
 * Provides comprehensive API documentation including:
 * - FHIR R4 resource endpoints
 * - SMART on FHIR OAuth 2.0 security
 * - Subscription/WebSocket endpoints
 * - Bulk Data Export endpoints
 */
@Configuration
public class OpenApiConfig {

    @Value("${smart.base-url:http://localhost:8085/fhir}")
    private String baseUrl;

    @Bean
    public OpenAPI fhirOpenAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .externalDocs(externalDocs())
            .servers(servers())
            .tags(tags())
            .components(securityComponents())
            .addSecurityItem(new SecurityRequirement()
                .addList("smart-oauth2")
                .addList("bearer-jwt")
                .addList("api-key"));
    }

    private Info apiInfo() {
        return new Info()
            .title("HDIM FHIR R4 API")
            .description("""
                ## Healthcare Data Integration Manager - FHIR R4 API

                This API provides HL7 FHIR R4 compliant endpoints for managing healthcare data.

                ### Features
                - **FHIR R4 Resources**: Patient, Observation, Condition, Encounter, MedicationRequest, Procedure, AllergyIntolerance, Immunization, DiagnosticReport, DocumentReference, CarePlan, Goal, Coverage
                - **SMART on FHIR**: OAuth 2.0 authorization with SMART App Launch support
                - **Real-time Subscriptions**: WebSocket and webhook-based notifications
                - **Bulk Data Export**: Async export of large datasets in NDJSON format
                - **Multi-tenant**: Full tenant isolation via X-Tenant-ID header

                ### Authentication
                This API supports multiple authentication methods:
                - **SMART on FHIR OAuth 2.0**: For patient-facing and EHR-integrated apps
                - **Bearer JWT**: For backend service authentication
                - **API Key**: For system-to-system integration

                ### FHIR Compliance
                This server implements HL7 FHIR R4 (4.0.1) specification.
                See the capability statement at `/metadata` for supported features.
                """)
            .version("1.0.0")
            .contact(new Contact()
                .name("HDIM Support")
                .email("support@healthdata.com")
                .url("https://healthdata.com"))
            .license(new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0"));
    }

    private ExternalDocumentation externalDocs() {
        return new ExternalDocumentation()
            .description("HL7 FHIR R4 Specification")
            .url("https://hl7.org/fhir/R4/");
    }

    private List<Server> servers() {
        return Arrays.asList(
            new Server()
                .url("http://localhost:8085/fhir")
                .description("Local Development Server"),
            new Server()
                .url("https://api.healthdata.com/fhir")
                .description("Production Server")
        );
    }

    private List<Tag> tags() {
        return Arrays.asList(
            new Tag()
                .name("Patient")
                .description("Patient demographics and administrative information"),
            new Tag()
                .name("Observation")
                .description("Clinical observations including vital signs, lab results, and social history"),
            new Tag()
                .name("Condition")
                .description("Patient conditions, problems, and diagnoses"),
            new Tag()
                .name("Encounter")
                .description("Healthcare encounters and visits"),
            new Tag()
                .name("MedicationRequest")
                .description("Medication prescriptions and orders"),
            new Tag()
                .name("MedicationAdministration")
                .description("Records of medication administration"),
            new Tag()
                .name("Procedure")
                .description("Clinical procedures performed on a patient"),
            new Tag()
                .name("AllergyIntolerance")
                .description("Allergies and intolerances"),
            new Tag()
                .name("Immunization")
                .description("Vaccination records"),
            new Tag()
                .name("DiagnosticReport")
                .description("Diagnostic test results and reports"),
            new Tag()
                .name("DocumentReference")
                .description("Clinical document references"),
            new Tag()
                .name("CarePlan")
                .description("Care plans and treatment plans"),
            new Tag()
                .name("Goal")
                .description("Patient health goals"),
            new Tag()
                .name("Coverage")
                .description("Insurance coverage information"),
            new Tag()
                .name("Subscription")
                .description("FHIR Subscriptions for real-time notifications"),
            new Tag()
                .name("Bulk Export")
                .description("Async bulk data export operations"),
            new Tag()
                .name("SMART")
                .description("SMART on FHIR OAuth 2.0 endpoints"),
            new Tag()
                .name("System")
                .description("System operations and metadata")
        );
    }

    private Components securityComponents() {
        return new Components()
            .addSecuritySchemes("smart-oauth2", smartOAuth2Scheme())
            .addSecuritySchemes("bearer-jwt", bearerJwtScheme())
            .addSecuritySchemes("api-key", apiKeyScheme());
    }

    private SecurityScheme smartOAuth2Scheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.OAUTH2)
            .description("SMART on FHIR OAuth 2.0 Authorization")
            .flows(new OAuthFlows()
                .authorizationCode(new OAuthFlow()
                    .authorizationUrl(baseUrl + "/oauth/authorize")
                    .tokenUrl(baseUrl + "/oauth/token")
                    .scopes(smartScopes())));
    }

    private Scopes smartScopes() {
        Scopes scopes = new Scopes();
        // Launch scopes
        scopes.addString("launch", "Permission to obtain launch context");
        scopes.addString("launch/patient", "Permission to request patient context");
        scopes.addString("launch/encounter", "Permission to request encounter context");

        // Identity scopes
        scopes.addString("openid", "OpenID Connect identity token");
        scopes.addString("fhirUser", "Access to FHIR resource for the current user");
        scopes.addString("profile", "Access to user profile claims");
        scopes.addString("offline_access", "Request refresh token for offline access");

        // Patient scopes
        scopes.addString("patient/Patient.read", "Read Patient resources in patient context");
        scopes.addString("patient/Observation.read", "Read Observation resources in patient context");
        scopes.addString("patient/Condition.read", "Read Condition resources in patient context");
        scopes.addString("patient/*.read", "Read all resources in patient context");

        // User scopes
        scopes.addString("user/Patient.read", "Read Patient resources user has access to");
        scopes.addString("user/*.read", "Read all resources user has access to");

        // System scopes
        scopes.addString("system/Patient.read", "System-level read access to Patient");
        scopes.addString("system/*.read", "System-level read access to all resources");
        scopes.addString("system/$export", "System-level bulk data export");

        return scopes;
    }

    private SecurityScheme bearerJwtScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("JWT Bearer token authentication");
    }

    private SecurityScheme apiKeyScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.HEADER)
            .name("X-API-Key")
            .description("API Key for system-to-system integration");
    }
}
