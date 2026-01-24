package com.healthdata.documentation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Documentation Service - Clinical Documents and Product Documentation
 *
 * Provides documentation capabilities including:
 * - Clinical documents (FHIR DocumentReference, CDA/C-CDA)
 * - Product documentation with version control
 * - Document search and feedback
 * - OCR text extraction from document attachments
 *
 * NOTE: This service excludes AuthenticationAutoConfiguration as it's non-clinical,
 * read-only documentation that doesn't require tenant isolation. Uses gateway-trust
 * authentication pattern like CQL Engine Service.
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.healthdata.documentation",
        "com.healthdata.common"
        // Excluded: com.healthdata.security - JWT filters require authentication beans
        // Excluded: com.healthdata.authentication - no tenant isolation needed for documentation
    },
    exclude = {
        // Exclude authentication auto-configurations to prevent User/Tenant entity scanning
        // and JWT filter creation (which requires CookieService, etc.)
        // Documentation service doesn't need authentication - non-clinical, read-only service
        com.healthdata.authentication.config.AuthenticationAutoConfiguration.class,
        com.healthdata.authentication.config.AuthenticationJwtAutoConfiguration.class
    }
)
@EntityScan(basePackages = {
    "com.healthdata.documentation.persistence"
})
@EnableJpaRepositories(basePackages = {
    "com.healthdata.documentation.repository"
})
public class DocumentationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentationServiceApplication.class, args);
    }
}
