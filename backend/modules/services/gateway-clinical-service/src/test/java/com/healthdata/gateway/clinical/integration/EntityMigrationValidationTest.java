package com.healthdata.gateway.clinical.integration;

import com.healthdata.testfixtures.validation.AbstractEntityMigrationValidationTest;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.Set;

@SpringBootTest(
        classes = EntityMigrationValidationTest.TestGatewayClinicalEntityValidationApplication.class,
        properties = {
                "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml",
                "spring.autoconfigure.exclude=com.healthdata.authentication.config.AuthenticationAutoConfiguration"
        }
)
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
@Tag("slow")
@Tag("heavyweight")
class EntityMigrationValidationTest extends AbstractEntityMigrationValidationTest {

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = "com.healthdata.gateway.clinical.compliance")
    @EntityScan(basePackages = "com.healthdata.gateway.clinical.compliance.entity")
    @EnableJpaRepositories(basePackages = "com.healthdata.gateway.clinical.compliance.repository")
    static class TestGatewayClinicalEntityValidationApplication {
    }

    @Override
    protected String getServiceName() {
        return "gateway-clinical-service";
    }

    @Override
    protected Set<String> getCriticalEntityNames() {
        return Collections.emptySet();  // Validate all entities
    }
}
