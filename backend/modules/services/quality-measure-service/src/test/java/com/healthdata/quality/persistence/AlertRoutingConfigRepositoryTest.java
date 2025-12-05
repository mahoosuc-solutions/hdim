package com.healthdata.quality.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Tests for Alert Routing Config Repository
 *
 * Tests database-driven routing configuration queries
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Alert Routing Config Repository Tests")
class AlertRoutingConfigRepositoryTest {

    @Autowired
    private AlertRoutingConfigRepository repository;

    private static final String TENANT_ID = "test-tenant";
    private static final String OTHER_TENANT_ID = "other-tenant";

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Should find routing configs by tenant and alert type")
    void shouldFindRoutingConfigsByTenantAndAlertType() {
        // Given: Routing configs for different alert types
        AlertRoutingConfigEntity mentalHealthConfig = createRoutingConfig(
            TENANT_ID,
            "MENTAL_HEALTH_CRISIS",
            "CRITICAL",
            "on-call-psychiatrist",
            1
        );
        AlertRoutingConfigEntity labConfig = createRoutingConfig(
            TENANT_ID,
            "CRITICAL_LAB",
            "CRITICAL",
            "ordering-provider",
            1
        );

        repository.save(mentalHealthConfig);
        repository.save(labConfig);

        // When: Finding configs for mental health alerts
        List<AlertRoutingConfigEntity> configs = repository
            .findByTenantIdAndAlertTypeAndActiveOrderByPriorityAsc(
                TENANT_ID,
                "MENTAL_HEALTH_CRISIS",
                true
            );

        // Then: Should return only mental health configs
        assertThat(configs).hasSize(1);
        assertThat(configs.get(0).getAlertType()).isEqualTo("MENTAL_HEALTH_CRISIS");
        assertThat(configs.get(0).getRecipientRole()).isEqualTo("on-call-psychiatrist");
    }

    @Test
    @DisplayName("Should find routing configs by tenant, alert type, and severity")
    void shouldFindRoutingConfigsByTenantAlertTypeAndSeverity() {
        // Given: Routing configs for different severities
        AlertRoutingConfigEntity criticalConfig = createRoutingConfig(
            TENANT_ID,
            "MENTAL_HEALTH_CRISIS",
            "CRITICAL",
            "on-call-psychiatrist",
            1
        );
        AlertRoutingConfigEntity highConfig = createRoutingConfig(
            TENANT_ID,
            "MENTAL_HEALTH_CRISIS",
            "HIGH",
            "care-team-lead",
            2
        );

        repository.save(criticalConfig);
        repository.save(highConfig);

        // When: Finding configs for CRITICAL severity
        List<AlertRoutingConfigEntity> configs = repository
            .findByTenantIdAndAlertTypeAndSeverityAndActiveOrderByPriorityAsc(
                TENANT_ID,
                "MENTAL_HEALTH_CRISIS",
                "CRITICAL",
                true
            );

        // Then: Should return only CRITICAL configs
        assertThat(configs).hasSize(1);
        assertThat(configs.get(0).getSeverity()).isEqualTo("CRITICAL");
        assertThat(configs.get(0).getRecipientRole()).isEqualTo("on-call-psychiatrist");
    }

    @Test
    @DisplayName("Should find routing configs including NULL severity (applies to all)")
    void shouldFindRoutingConfigsIncludingNullSeverity() {
        // Given: Routing configs with specific and NULL severity
        AlertRoutingConfigEntity criticalConfig = createRoutingConfig(
            TENANT_ID,
            "RISK_ESCALATION",
            "CRITICAL",
            "on-call-provider",
            1
        );
        AlertRoutingConfigEntity anyConfig = createRoutingConfig(
            TENANT_ID,
            "RISK_ESCALATION",
            null, // Applies to all severities
            "primary-care-provider",
            3
        );

        repository.save(criticalConfig);
        repository.save(anyConfig);

        // When: Finding configs for CRITICAL severity (including NULL)
        List<AlertRoutingConfigEntity> configs = repository
            .findRoutingConfigsForAlertAndSeverity(
                TENANT_ID,
                "RISK_ESCALATION",
                "CRITICAL",
                true
            );

        // Then: Should return both specific and general configs
        assertThat(configs).hasSize(2);
        assertThat(configs.get(0).getPriority()).isLessThan(configs.get(1).getPriority());
    }

    @Test
    @DisplayName("Should order routing configs by priority")
    void shouldOrderRoutingConfigsByPriority() {
        // Given: Multiple configs with different priorities
        AlertRoutingConfigEntity lowPriority = createRoutingConfig(
            TENANT_ID,
            "MENTAL_HEALTH_CRISIS",
            "CRITICAL",
            "care-coordinator",
            10
        );
        AlertRoutingConfigEntity highPriority = createRoutingConfig(
            TENANT_ID,
            "MENTAL_HEALTH_CRISIS",
            "CRITICAL",
            "on-call-psychiatrist",
            1
        );
        AlertRoutingConfigEntity mediumPriority = createRoutingConfig(
            TENANT_ID,
            "MENTAL_HEALTH_CRISIS",
            "CRITICAL",
            "care-team-lead",
            5
        );

        repository.save(lowPriority);
        repository.save(highPriority);
        repository.save(mediumPriority);

        // When: Finding configs
        List<AlertRoutingConfigEntity> configs = repository
            .findByTenantIdAndAlertTypeAndSeverityAndActiveOrderByPriorityAsc(
                TENANT_ID,
                "MENTAL_HEALTH_CRISIS",
                "CRITICAL",
                true
            );

        // Then: Should be ordered by priority (1, 5, 10)
        assertThat(configs).hasSize(3);
        assertThat(configs.get(0).getRecipientRole()).isEqualTo("on-call-psychiatrist");
        assertThat(configs.get(1).getRecipientRole()).isEqualTo("care-team-lead");
        assertThat(configs.get(2).getRecipientRole()).isEqualTo("care-coordinator");
    }

    @Test
    @DisplayName("Should find business hours routing configs")
    void shouldFindBusinessHoursRoutingConfigs() {
        // Given: Business hours and after-hours configs
        AlertRoutingConfigEntity businessHours = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("MENTAL_HEALTH_CRISIS")
            .severity("CRITICAL")
            .recipientRole("primary-care-provider")
            .priority(1)
            .businessHoursOnly(true)
            .active(true)
            .build();

        AlertRoutingConfigEntity afterHours = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("MENTAL_HEALTH_CRISIS")
            .severity("CRITICAL")
            .recipientRole("on-call-psychiatrist")
            .priority(1)
            .afterHoursOnly(true)
            .active(true)
            .build();

        AlertRoutingConfigEntity anytime = createRoutingConfig(
            TENANT_ID,
            "MENTAL_HEALTH_CRISIS",
            "CRITICAL",
            "care-team-lead",
            2
        );

        repository.save(businessHours);
        repository.save(afterHours);
        repository.save(anytime);

        // When: Finding business hours configs
        List<AlertRoutingConfigEntity> configs = repository
            .findBusinessHoursRoutingConfigs(
                TENANT_ID,
                "MENTAL_HEALTH_CRISIS",
                "CRITICAL",
                true
            );

        // Then: Should include business hours and anytime configs
        assertThat(configs).hasSize(2);
        assertThat(configs)
            .extracting(AlertRoutingConfigEntity::getRecipientRole)
            .containsExactlyInAnyOrder("primary-care-provider", "care-team-lead");
    }

    @Test
    @DisplayName("Should find after-hours routing configs")
    void shouldFindAfterHoursRoutingConfigs() {
        // Given: Business hours and after-hours configs
        AlertRoutingConfigEntity businessHours = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("CRITICAL_LAB")
            .severity("CRITICAL")
            .recipientRole("ordering-provider")
            .priority(1)
            .businessHoursOnly(true)
            .active(true)
            .build();

        AlertRoutingConfigEntity afterHours = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("CRITICAL_LAB")
            .severity("CRITICAL")
            .recipientRole("on-call-provider")
            .priority(1)
            .afterHoursOnly(true)
            .active(true)
            .build();

        AlertRoutingConfigEntity anytime = createRoutingConfig(
            TENANT_ID,
            "CRITICAL_LAB",
            "CRITICAL",
            "care-team-lead",
            2
        );

        repository.save(businessHours);
        repository.save(afterHours);
        repository.save(anytime);

        // When: Finding after-hours configs
        List<AlertRoutingConfigEntity> configs = repository
            .findAfterHoursRoutingConfigs(
                TENANT_ID,
                "CRITICAL_LAB",
                "CRITICAL",
                true
            );

        // Then: Should include after-hours and anytime configs
        assertThat(configs).hasSize(2);
        assertThat(configs)
            .extracting(AlertRoutingConfigEntity::getRecipientRole)
            .containsExactlyInAnyOrder("on-call-provider", "care-team-lead");
    }

    @Test
    @DisplayName("Should find escalation routing configs")
    void shouldFindEscalationRoutingConfigs() {
        // Given: Configs with and without escalation thresholds
        AlertRoutingConfigEntity immediate = createRoutingConfig(
            TENANT_ID,
            "MENTAL_HEALTH_CRISIS",
            "CRITICAL",
            "on-call-psychiatrist",
            1
        );

        AlertRoutingConfigEntity escalation15min = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("MENTAL_HEALTH_CRISIS")
            .severity("CRITICAL")
            .recipientRole("care-team-lead")
            .priority(2)
            .escalationThresholdMinutes(15)
            .active(true)
            .build();

        AlertRoutingConfigEntity escalation30min = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("MENTAL_HEALTH_CRISIS")
            .severity("CRITICAL")
            .recipientRole("on-call-provider")
            .priority(3)
            .escalationThresholdMinutes(30)
            .active(true)
            .build();

        repository.save(immediate);
        repository.save(escalation15min);
        repository.save(escalation30min);

        // When: Finding escalation configs
        List<AlertRoutingConfigEntity> configs = repository
            .findEscalationRoutingConfigs(
                TENANT_ID,
                "MENTAL_HEALTH_CRISIS",
                true
            );

        // Then: Should return only configs with escalation thresholds, ordered by time
        assertThat(configs).hasSize(2);
        assertThat(configs.get(0).getEscalationThresholdMinutes()).isEqualTo(15);
        assertThat(configs.get(1).getEscalationThresholdMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("Should not find inactive routing configs")
    void shouldNotFindInactiveRoutingConfigs() {
        // Given: Active and inactive configs
        AlertRoutingConfigEntity active = createRoutingConfig(
            TENANT_ID,
            "RISK_ESCALATION",
            "HIGH",
            "care-coordinator",
            1
        );

        AlertRoutingConfigEntity inactive = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("RISK_ESCALATION")
            .severity("HIGH")
            .recipientRole("old-role")
            .priority(1)
            .active(false)
            .build();

        repository.save(active);
        repository.save(inactive);

        // When: Finding active configs
        List<AlertRoutingConfigEntity> configs = repository
            .findByTenantIdAndAlertTypeAndSeverityAndActiveOrderByPriorityAsc(
                TENANT_ID,
                "RISK_ESCALATION",
                "HIGH",
                true
            );

        // Then: Should return only active configs
        assertThat(configs).hasSize(1);
        assertThat(configs.get(0).getRecipientRole()).isEqualTo("care-coordinator");
    }

    @Test
    @DisplayName("Should isolate routing configs by tenant")
    void shouldIsolateRoutingConfigsByTenant() {
        // Given: Configs for different tenants
        AlertRoutingConfigEntity tenant1Config = createRoutingConfig(
            TENANT_ID,
            "MENTAL_HEALTH_CRISIS",
            "CRITICAL",
            "tenant1-psychiatrist",
            1
        );

        AlertRoutingConfigEntity tenant2Config = createRoutingConfig(
            OTHER_TENANT_ID,
            "MENTAL_HEALTH_CRISIS",
            "CRITICAL",
            "tenant2-psychiatrist",
            1
        );

        repository.save(tenant1Config);
        repository.save(tenant2Config);

        // When: Finding configs for tenant1
        List<AlertRoutingConfigEntity> configs = repository
            .findByTenantIdAndAlertTypeAndActiveOrderByPriorityAsc(
                TENANT_ID,
                "MENTAL_HEALTH_CRISIS",
                true
            );

        // Then: Should return only tenant1's configs
        assertThat(configs).hasSize(1);
        assertThat(configs.get(0).getRecipientRole()).isEqualTo("tenant1-psychiatrist");
    }

    // Helper method
    private AlertRoutingConfigEntity createRoutingConfig(
        String tenantId,
        String alertType,
        String severity,
        String recipientRole,
        int priority
    ) {
        return AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .alertType(alertType)
            .severity(severity)
            .recipientRole(recipientRole)
            .priority(priority)
            .active(true)
            .build();
    }
}
