package com.healthdata.notification.integration;

import com.healthdata.notification.TestNotificationApplication;
import com.healthdata.testfixtures.validation.AbstractEntityMigrationValidationTest;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

/**
 * Integration test that validates entity-migration synchronization for the Notification service.
 *
 * This test ensures that all JPA entities for real-time notifications
 * have corresponding database schema definitions via Liquibase migrations.
 *
 * Key validations:
 * - All @Entity classes have corresponding database tables
 * - All @Column fields have matching database columns
 * - Notification channels are properly defined
 * - Notification templates and preferences are properly stored
 * - Multi-tenant isolation is enforced at the database level
 *
 * @author HDIM Platform Team
 */
@SpringBootTest(classes = TestNotificationApplication.class)
class EntityMigrationValidationTest extends AbstractEntityMigrationValidationTest {

    @Override
    protected String getServiceName() {
        return "notification-service";
    }

    @Override
    protected Set<String> getCriticalEntityNames() {
        return Set.of("NotificationChannel", "NotificationTemplate", "NotificationPreference");
    }
}
