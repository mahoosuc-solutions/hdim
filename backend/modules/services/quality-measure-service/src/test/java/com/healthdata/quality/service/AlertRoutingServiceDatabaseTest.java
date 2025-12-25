package com.healthdata.quality.service;

import com.healthdata.quality.config.TestMessagingConfiguration;
import com.healthdata.quality.config.TestWebSocketConfiguration;
import com.healthdata.quality.dto.ClinicalAlertDTO;
import com.healthdata.quality.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Integration Tests for Database-Driven Alert Routing Service
 *
 * Tests the complete alert routing flow using database configuration:
 * - Tenant-specific routing rules
 * - Patient-specific care team assignments
 * - Business hours vs after-hours routing
 * - Escalation path configuration
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import({TestMessagingConfiguration.class, TestWebSocketConfiguration.class})
@DisplayName("Alert Routing Service Database Integration Tests")
class AlertRoutingServiceDatabaseTest {

    @Autowired
    private AlertRoutingService alertRoutingService;

    @Autowired
    private AlertRoutingConfigRepository routingConfigRepository;

    @Autowired
    private CareTeamAssignmentRepository careTeamRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @BeforeEach
    void setUp() {
        routingConfigRepository.deleteAll();
        careTeamRepository.deleteAll();
    }

    @Test
    @DisplayName("Should route alert using database routing configuration")
    void shouldRouteAlertUsingDatabaseRoutingConfiguration() {
        // Given: Database routing configuration for MENTAL_HEALTH_CRISIS
        createRoutingConfig(
            "MENTAL_HEALTH_CRISIS",
            "CRITICAL",
            "on-call-psychiatrist",
            1
        );
        createRoutingConfig(
            "MENTAL_HEALTH_CRISIS",
            "CRITICAL",
            "care-team-lead",
            2
        );

        ClinicalAlertDTO alert = createAlert(
            "MENTAL_HEALTH_CRISIS",
            "CRITICAL",
            "Suicide Risk Detected"
        );

        // When: Determining recipients from database
        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        // Then: Should return database-configured recipients
        assertThat(recipients).isNotEmpty();
        assertThat(recipients).contains("on-call-psychiatrist", "care-team-lead");
    }

    @Test
    @DisplayName("Should route to patient-specific care team members")
    void shouldRouteToPatientSpecificCareTeamMembers() {
        // Given: Patient-specific care team assignments
        createCareTeamAssignment(
            PATIENT_ID,
            "dr-smith-123",
            "primary-care-provider",
            "Dr. Sarah Smith",
            1
        );
        createCareTeamAssignment(
            PATIENT_ID,
            "coordinator-456",
            "care-coordinator",
            "Jane Coordinator",
            2
        );

        // And: Routing config that uses patient's care team
        createRoutingConfig(
            "RISK_ESCALATION",
            "HIGH",
            "primary-care-provider",
            1
        );
        createRoutingConfig(
            "RISK_ESCALATION",
            "HIGH",
            "care-coordinator",
            2
        );

        ClinicalAlertDTO alert = createAlert(
            "RISK_ESCALATION",
            "HIGH",
            "Patient Risk Level: Very High"
        );

        // When: Determining recipients
        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        // Then: Should return patient's actual care team members
        assertThat(recipients).contains("dr-smith-123", "coordinator-456");
    }

    @Test
    @DisplayName("Should apply business hours routing rules")
    void shouldApplyBusinessHoursRoutingRules() {
        // Given: Business hours and after-hours routing configs
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

        routingConfigRepository.save(businessHours);
        routingConfigRepository.save(afterHours);

        ClinicalAlertDTO alert = createAlert(
            "CRITICAL_LAB",
            "CRITICAL",
            "Critical Potassium Level"
        );

        // When: Determining recipients during business hours
        List<String> businessHoursRecipients = alertRoutingService
            .determineRecipientsForBusinessHours(TENANT_ID, alert);

        // Then: Should include business hours recipients
        assertThat(businessHoursRecipients).contains("ordering-provider");
        assertThat(businessHoursRecipients).doesNotContain("on-call-provider");

        // When: Determining recipients after hours
        List<String> afterHoursRecipients = alertRoutingService
            .determineRecipientsForAfterHours(TENANT_ID, alert);

        // Then: Should include after-hours recipients
        assertThat(afterHoursRecipients).contains("on-call-provider");
        assertThat(afterHoursRecipients).doesNotContain("ordering-provider");
    }

    @Test
    @DisplayName("Should support escalation path configuration")
    void shouldSupportEscalationPathConfiguration() {
        // Given: Escalation routing configs with time thresholds
        AlertRoutingConfigEntity immediate = createRoutingConfig(
            "MENTAL_HEALTH_CRISIS",
            "CRITICAL",
            "on-call-psychiatrist",
            1
        );

        AlertRoutingConfigEntity escalation15 = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("MENTAL_HEALTH_CRISIS")
            .severity("CRITICAL")
            .recipientRole("care-team-lead")
            .priority(2)
            .escalationThresholdMinutes(15)
            .active(true)
            .build();

        AlertRoutingConfigEntity escalation30 = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("MENTAL_HEALTH_CRISIS")
            .severity("CRITICAL")
            .recipientRole("medical-director")
            .priority(3)
            .escalationThresholdMinutes(30)
            .active(true)
            .build();

        routingConfigRepository.save(immediate);
        routingConfigRepository.save(escalation15);
        routingConfigRepository.save(escalation30);

        ClinicalAlertDTO alert = createAlert(
            "MENTAL_HEALTH_CRISIS",
            "CRITICAL",
            "Suicide Risk"
        );

        // When: Getting escalation recipients after 20 minutes
        List<String> escalatedRecipients = alertRoutingService
            .getEscalationRecipientsAfterMinutes(TENANT_ID, alert, 20);

        // Then: Should include 15-minute escalation recipient
        assertThat(escalatedRecipients).contains("care-team-lead");
        assertThat(escalatedRecipients).doesNotContain("medical-director");

        // When: Getting escalation recipients after 35 minutes
        List<String> furtherEscalatedRecipients = alertRoutingService
            .getEscalationRecipientsAfterMinutes(TENANT_ID, alert, 35);

        // Then: Should include both 15 and 30 minute escalation recipients
        assertThat(furtherEscalatedRecipients).contains("care-team-lead", "medical-director");
    }

    @Test
    @DisplayName("Should apply routing rules in priority order")
    void shouldApplyRoutingRulesInPriorityOrder() {
        // Given: Multiple routing configs with different priorities
        createRoutingConfig("HEALTH_DECLINE", "HIGH", "care-coordinator", 10);
        createRoutingConfig("HEALTH_DECLINE", "HIGH", "on-call-provider", 1);
        createRoutingConfig("HEALTH_DECLINE", "HIGH", "primary-care-provider", 5);

        ClinicalAlertDTO alert = createAlert(
            "HEALTH_DECLINE",
            "HIGH",
            "Health Score Decline"
        );

        // When: Determining recipients
        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        // Then: Should be ordered by priority (1, 5, 10)
        assertThat(recipients).hasSize(3);
        assertThat(recipients.get(0)).isEqualTo("on-call-provider");
        assertThat(recipients.get(1)).isEqualTo("primary-care-provider");
        assertThat(recipients.get(2)).isEqualTo("care-coordinator");
    }

    @Test
    @DisplayName("Should use general severity rules when specific severity not configured")
    void shouldUseGeneralSeverityRulesWhenSpecificNotConfigured() {
        // Given: General rule (NULL severity) and specific rule
        AlertRoutingConfigEntity generalRule = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("CHRONIC_DETERIORATION")
            .severity(null) // Applies to all severities
            .recipientRole("primary-care-provider")
            .priority(10)
            .active(true)
            .build();

        AlertRoutingConfigEntity specificRule = createRoutingConfig(
            "CHRONIC_DETERIORATION",
            "CRITICAL",
            "on-call-provider",
            1
        );

        routingConfigRepository.save(generalRule);
        routingConfigRepository.save(specificRule);

        // When: Routing CRITICAL alert
        ClinicalAlertDTO criticalAlert = createAlert(
            "CHRONIC_DETERIORATION",
            "CRITICAL",
            "Severe Deterioration"
        );
        List<String> criticalRecipients = alertRoutingService
            .determineRecipients(TENANT_ID, criticalAlert);

        // Then: Should include both specific and general rules
        assertThat(criticalRecipients).contains("on-call-provider", "primary-care-provider");

        // When: Routing MEDIUM alert (no specific rule)
        ClinicalAlertDTO mediumAlert = createAlert(
            "CHRONIC_DETERIORATION",
            "MEDIUM",
            "Moderate Deterioration"
        );
        List<String> mediumRecipients = alertRoutingService
            .determineRecipients(TENANT_ID, mediumAlert);

        // Then: Should use general rule
        assertThat(mediumRecipients).contains("primary-care-provider");
    }

    @Test
    @DisplayName("Should fall back to default recipients when no configuration exists")
    void shouldFallBackToDefaultRecipientsWhenNoConfigurationExists() {
        // Given: No routing configuration

        ClinicalAlertDTO alert = createAlert(
            "UNKNOWN_ALERT_TYPE",
            "HIGH",
            "Unknown Alert"
        );

        // When: Determining recipients
        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        // Then: Should return default recipients (from hardcoded fallback)
        assertThat(recipients).isNotEmpty();
        assertThat(recipients).contains("primary-care-provider");
    }

    @Test
    @DisplayName("Should isolate routing by tenant")
    void shouldIsolateRoutingByTenant() {
        // Given: Routing configs for different tenants
        createRoutingConfig(
            "MENTAL_HEALTH_CRISIS",
            "CRITICAL",
            "tenant1-psychiatrist",
            1
        );

        AlertRoutingConfigEntity tenant2Config = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId("other-tenant")
            .alertType("MENTAL_HEALTH_CRISIS")
            .severity("CRITICAL")
            .recipientRole("tenant2-psychiatrist")
            .priority(1)
            .active(true)
            .build();

        routingConfigRepository.save(tenant2Config);

        ClinicalAlertDTO alert = createAlert(
            "MENTAL_HEALTH_CRISIS",
            "CRITICAL",
            "Crisis"
        );

        // When: Routing for tenant1
        List<String> tenant1Recipients = alertRoutingService
            .determineRecipients(TENANT_ID, alert);

        // When: Routing for tenant2
        List<String> tenant2Recipients = alertRoutingService
            .determineRecipients("other-tenant", alert);

        // Then: Each tenant should get their own recipients
        assertThat(tenant1Recipients).contains("tenant1-psychiatrist");
        assertThat(tenant1Recipients).doesNotContain("tenant2-psychiatrist");
        assertThat(tenant2Recipients).contains("tenant2-psychiatrist");
        assertThat(tenant2Recipients).doesNotContain("tenant1-psychiatrist");
    }

    @Test
    @DisplayName("Should only use active care team assignments")
    void shouldOnlyUseActiveCareTeamAssignments() {
        // Given: Active and inactive care team members
        createCareTeamAssignment(
            PATIENT_ID,
            "active-provider",
            "primary-care-provider",
            "Dr. Active",
            1
        );

        CareTeamAssignmentEntity inactive = CareTeamAssignmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .providerId("inactive-provider")
            .providerName("Dr. Inactive")
            .role("primary-care-provider")
            .contactPriority(2)
            .active(false)
            .build();

        careTeamRepository.save(inactive);

        createRoutingConfig(
            "CARE_GAP_OVERDUE",
            "HIGH",
            "primary-care-provider",
            1
        );

        ClinicalAlertDTO alert = createAlert(
            "CARE_GAP_OVERDUE",
            "HIGH",
            "Screening Overdue"
        );

        // When: Determining recipients
        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        // Then: Should only include active provider
        assertThat(recipients).contains("active-provider");
        assertThat(recipients).doesNotContain("inactive-provider");
    }

    @Test
    @DisplayName("Should respect effective date ranges for care team assignments")
    void shouldRespectEffectiveDateRangesForCareTeamAssignments() {
        // Given: Care team assignments with effective dates
        LocalDate today = LocalDate.now();
        LocalDate future = today.plusDays(30);

        // Current provider
        createCareTeamAssignment(
            PATIENT_ID,
            "current-provider",
            "primary-care-provider",
            "Dr. Current",
            1
        );

        // Future provider (not yet effective)
        CareTeamAssignmentEntity futureAssignment = CareTeamAssignmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .providerId("future-provider")
            .providerName("Dr. Future")
            .role("primary-care-provider")
            .contactPriority(1)
            .active(true)
            .effectiveFrom(future)
            .build();

        careTeamRepository.save(futureAssignment);

        createRoutingConfig(
            "HEALTH_DECLINE",
            "MEDIUM",
            "primary-care-provider",
            1
        );

        ClinicalAlertDTO alert = createAlert(
            "HEALTH_DECLINE",
            "MEDIUM",
            "Health Decline"
        );

        // When: Determining recipients today
        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        // Then: Should only include current provider
        assertThat(recipients).contains("current-provider");
        assertThat(recipients).doesNotContain("future-provider");
    }

    // Helper methods
    private AlertRoutingConfigEntity createRoutingConfig(
        String alertType,
        String severity,
        String recipientRole,
        int priority
    ) {
        AlertRoutingConfigEntity config = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType(alertType)
            .severity(severity)
            .recipientRole(recipientRole)
            .priority(priority)
            .active(true)
            .build();
        return routingConfigRepository.save(config);
    }

    private CareTeamAssignmentEntity createCareTeamAssignment(
        UUID patientId,
        String providerId,
        String role,
        String providerName,
        int contactPriority
    ) {
        CareTeamAssignmentEntity assignment = CareTeamAssignmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(patientId)
            .providerId(providerId)
            .providerName(providerName)
            .role(role)
            .contactPriority(contactPriority)
            .active(true)
            .build();
        return careTeamRepository.save(assignment);
    }

    private ClinicalAlertDTO createAlert(String alertType, String severity, String title) {
        return ClinicalAlertDTO.builder()
            .id(UUID.randomUUID().toString())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .alertType(alertType)
            .severity(severity)
            .title(title)
            .message("Test alert message")
            .status("ACTIVE")
            .triggeredAt(Instant.now())
            .escalated(false)
            .build();
    }
}
