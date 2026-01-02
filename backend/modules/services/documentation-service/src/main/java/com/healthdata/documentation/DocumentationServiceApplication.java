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
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.documentation",
    "com.healthdata.common",
    "com.healthdata.authentication",
    "com.healthdata.security",
    "com.healthdata.audit"
})
@EntityScan(basePackages = "com.healthdata.documentation.persistence")
@EnableJpaRepositories(basePackages = "com.healthdata.documentation.repository")
public class DocumentationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentationServiceApplication.class, args);
    }
}
