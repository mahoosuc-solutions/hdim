package com.healthdata.auditquery.integration;

import com.healthdata.auditquery.AuditQueryServiceApplication;
import com.healthdata.testfixtures.validation.AbstractEntityMigrationValidationTest;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.Set;

@SpringBootTest(
        classes = AuditQueryServiceApplication.class,
        properties = "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml"
)
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
@Tag("slow")
@Tag("heavyweight")
class EntityMigrationValidationTest extends AbstractEntityMigrationValidationTest {

    @Override
    protected String getServiceName() {
        return "audit-query-service";
    }

    @Override
    protected Set<String> getCriticalEntityNames() {
        return Collections.emptySet();  // Validate all entities
    }
}
