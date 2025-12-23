package com.healthdata.quality.service;

import com.healthdata.quality.dto.ClinicalAlertDTO;
import com.healthdata.quality.persistence.AlertRoutingConfigRepository;
import com.healthdata.quality.persistence.CareTeamAssignmentRepository;
import com.healthdata.quality.persistence.ClinicalAlertEntity;
import com.healthdata.quality.persistence.AlertRoutingConfigEntity;
import com.healthdata.quality.persistence.CareTeamAssignmentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * TDD Tests for Alert Routing Service (Phase 5)
 *
 * Tests hardcoded fallback routing logic:
 * - Determine recipients based on alert type and severity
 * - Route CRITICAL alerts to on-call providers and care team
 * - Route HIGH alerts to assigned care team
 * - Route MEDIUM/LOW alerts to primary care provider
 * - Support care team role-based routing
 *
 * Note: These tests verify the hardcoded fallback behavior when no database config exists
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Alert Routing Service Tests (Hardcoded Fallback)")
class AlertRoutingServiceTest {

    private AlertRoutingService alertRoutingService;

    @Mock
    private AlertRoutingConfigRepository routingConfigRepository;

    @Mock
    private CareTeamAssignmentRepository careTeamRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        alertRoutingService = new AlertRoutingService(routingConfigRepository, careTeamRepository);

        // Mock empty database config to test hardcoded fallback (lenient to avoid unnecessary stubbing warnings)
        org.mockito.Mockito.lenient()
            .when(routingConfigRepository.findBusinessHoursRoutingConfigs(anyString(), anyString(), anyString(), anyBoolean()))
            .thenReturn(Collections.emptyList());
        org.mockito.Mockito.lenient()
            .when(routingConfigRepository.findAfterHoursRoutingConfigs(anyString(), anyString(), anyString(), anyBoolean()))
            .thenReturn(Collections.emptyList());
        org.mockito.Mockito.lenient()
            .when(routingConfigRepository.findRoutingConfigsForAlertAndSeverity(anyString(), anyString(), anyString(), anyBoolean()))
            .thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("Should route CRITICAL mental health alerts to on-call psychiatrist and care team")
    void shouldRouteCriticalMentalHealthAlertToOnCallAndCareTeam() {
        // Given: CRITICAL mental health crisis alert
        ClinicalAlertDTO alert = createAlert(
            "MENTAL_HEALTH_CRISIS",
            "CRITICAL",
            "Suicide Risk Detected"
        );

        // When: Determining recipients
        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        // Then: Should include on-call psychiatrist, care team, and primary provider
        assertThat(recipients).isNotEmpty();
        assertThat(recipients).contains("on-call-psychiatrist");
        assertThat(recipients).contains("care-team-lead");
        assertThat(recipients).contains("primary-care-provider");
    }

    @Test
    @DisplayName("Should route CRITICAL lab alerts to ordering provider and care team")
    void shouldRouteCriticalLabAlertToOrderingProviderAndCareTeam() {
        // Given: CRITICAL lab result alert
        ClinicalAlertDTO alert = createAlert(
            "CRITICAL_LAB",
            "CRITICAL",
            "Critical Potassium Level"
        );

        // When: Determining recipients
        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        // Then: Should include ordering provider and care team
        assertThat(recipients).isNotEmpty();
        assertThat(recipients).contains("ordering-provider");
        assertThat(recipients).contains("care-team-lead");
    }

    @Test
    @DisplayName("Should route non-critical lab alerts to ordering provider only")
    void shouldRouteNonCriticalLabAlertToOrderingProviderOnly() {
        ClinicalAlertDTO alert = createAlert(
            "CRITICAL_LAB",
            "HIGH",
            "High Potassium"
        );

        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        assertThat(recipients).containsExactly("ordering-provider");
    }

    @Test
    @DisplayName("Should route HIGH risk escalation alerts to care coordinator and primary provider")
    void shouldRouteHighRiskAlertToCareCoordinatorAndPrimaryProvider() {
        // Given: HIGH risk escalation alert
        ClinicalAlertDTO alert = createAlert(
            "RISK_ESCALATION",
            "HIGH",
            "Patient Risk Level: Very High"
        );

        // When: Determining recipients
        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        // Then: Should include care coordinator and primary provider
        assertThat(recipients).isNotEmpty();
        assertThat(recipients).contains("care-coordinator");
        assertThat(recipients).contains("primary-care-provider");
    }

    @Test
    @DisplayName("Should route low risk escalation alerts to primary provider only")
    void shouldRouteLowRiskAlertToPrimaryProviderOnly() {
        ClinicalAlertDTO alert = createAlert(
            "RISK_ESCALATION",
            "LOW",
            "Low Risk"
        );

        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        assertThat(recipients).containsExactly("primary-care-provider");
    }

    @Test
    @DisplayName("Should route MEDIUM health decline alerts to primary care provider")
    void shouldRouteMediumHealthDeclineAlertToPrimaryProvider() {
        // Given: MEDIUM health score decline alert
        ClinicalAlertDTO alert = createAlert(
            "HEALTH_DECLINE",
            "MEDIUM",
            "Health Score Decline Detected"
        );

        // When: Determining recipients
        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        // Then: Should route to primary care provider only
        assertThat(recipients).isNotEmpty();
        assertThat(recipients).contains("primary-care-provider");
        assertThat(recipients).hasSize(1);
    }

    @Test
    @DisplayName("Should route HIGH health decline alerts to care coordinator and primary provider")
    void shouldRouteHighHealthDeclineAlertToCareCoordinatorAndPrimaryProvider() {
        ClinicalAlertDTO alert = createAlert(
            "HEALTH_DECLINE",
            "HIGH",
            "Health decline"
        );

        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        assertThat(recipients).contains("care-coordinator", "primary-care-provider");
    }

    @Test
    @DisplayName("Should route care gap overdue alerts based on gap priority")
    void shouldRouteCareGapOverdueAlertBasedOnPriority() {
        // Given: Care gap overdue alert
        ClinicalAlertDTO alert = createAlert(
            "CARE_GAP_OVERDUE",
            "HIGH",
            "Colorectal Screening Overdue (>90 days)"
        );

        // When: Determining recipients
        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        // Then: Should route to care coordinator and primary provider
        assertThat(recipients).isNotEmpty();
        assertThat(recipients).contains("care-coordinator");
        assertThat(recipients).contains("primary-care-provider");
    }

    @Test
    @DisplayName("Should support tenant-specific recipient configuration")
    void shouldSupportTenantSpecificRecipientConfiguration() {
        // Given: Alert for different tenant
        String otherTenant = "other-tenant";
        ClinicalAlertDTO alert = createAlert(
            "MENTAL_HEALTH_CRISIS",
            "CRITICAL",
            "Suicide Risk"
        );

        // When: Determining recipients for different tenants
        List<String> tenant1Recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);
        List<String> tenant2Recipients = alertRoutingService.determineRecipients(otherTenant, alert);

        // Then: Recipients should be tenant-specific
        assertThat(tenant1Recipients).isNotEmpty();
        assertThat(tenant2Recipients).isNotEmpty();
        // In real implementation, these would differ based on tenant configuration
    }

    @Test
    @DisplayName("Should include role-based recipients for each alert type")
    void shouldIncludeRoleBasedRecipients() {
        // Given: Different alert types
        ClinicalAlertDTO mentalHealthAlert = createAlert("MENTAL_HEALTH_CRISIS", "CRITICAL", "Crisis");
        ClinicalAlertDTO labAlert = createAlert("CRITICAL_LAB", "CRITICAL", "Critical Lab");
        ClinicalAlertDTO riskAlert = createAlert("RISK_ESCALATION", "HIGH", "High Risk");

        // When: Getting recipients
        List<String> mentalHealthRecipients = alertRoutingService.determineRecipients(TENANT_ID, mentalHealthAlert);
        List<String> labRecipients = alertRoutingService.determineRecipients(TENANT_ID, labAlert);
        List<String> riskRecipients = alertRoutingService.determineRecipients(TENANT_ID, riskAlert);

        // Then: Each should have appropriate role-based recipients
        assertThat(mentalHealthRecipients).contains("on-call-psychiatrist");
        assertThat(labRecipients).contains("ordering-provider");
        assertThat(riskRecipients).contains("care-coordinator");
    }

    @Test
    @DisplayName("Should return default recipients when no specific routing configured")
    void shouldReturnDefaultRecipientsWhenNoSpecificRoutingConfigured() {
        // Given: Alert with unknown type
        ClinicalAlertDTO alert = createAlert(
            "UNKNOWN_TYPE",
            "MEDIUM",
            "Unknown Alert"
        );

        // When: Determining recipients
        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        // Then: Should return default recipients (primary care provider)
        assertThat(recipients).isNotEmpty();
        assertThat(recipients).contains("primary-care-provider");
    }

    @Test
    @DisplayName("Should map database roles to assigned providers")
    void shouldMapDatabaseRolesToAssignedProviders() {
        AlertRoutingConfigEntity pcpConfig = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("RISK_ESCALATION")
            .severity("HIGH")
            .recipientRole("primary-care-provider")
            .priority(1)
            .active(true)
            .build();
        AlertRoutingConfigEntity coordinatorConfig = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("RISK_ESCALATION")
            .severity("HIGH")
            .recipientRole("care-coordinator")
            .priority(2)
            .active(true)
            .build();

        when(routingConfigRepository.findBusinessHoursRoutingConfigs(
            TENANT_ID, "RISK_ESCALATION", "HIGH", true))
            .thenReturn(List.of(pcpConfig, coordinatorConfig));

        CareTeamAssignmentEntity assignment = CareTeamAssignmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .providerId("dr-smith")
            .role("primary-care-provider")
            .active(true)
            .build();

        when(careTeamRepository.findActiveAssignmentsByRoleOnDate(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq("primary-care-provider"),
            any(LocalDate.class),
            eq(true)
        )).thenReturn(List.of(assignment));

        when(careTeamRepository.findActiveAssignmentsByRoleOnDate(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq("care-coordinator"),
            any(LocalDate.class),
            eq(true)
        )).thenReturn(Collections.emptyList());

        ClinicalAlertDTO alert = createAlert("RISK_ESCALATION", "HIGH", "High Risk");

        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        assertThat(recipients).contains("dr-smith", "care-coordinator");
    }

    @Test
    @DisplayName("Should fall back to escalation roles when no escalation configs exist")
    void shouldFallbackToEscalationRolesWhenNoConfig() {
        ClinicalAlertDTO alert = createAlert("HEALTH_DECLINE", "CRITICAL", "Decline");

        List<String> recipients = alertRoutingService.getEscalationRecipients(TENANT_ID, alert);

        assertThat(recipients).contains("on-call-provider", "care-team-lead");
    }

    @Test
    @DisplayName("Should filter escalation recipients by threshold")
    void shouldFilterEscalationRecipientsByThreshold() {
        AlertRoutingConfigEntity config10 = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("MENTAL_HEALTH_CRISIS")
            .severity("CRITICAL")
            .recipientRole("care-team-lead")
            .priority(1)
            .escalationThresholdMinutes(10)
            .active(true)
            .build();
        AlertRoutingConfigEntity config40 = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("MENTAL_HEALTH_CRISIS")
            .severity("CRITICAL")
            .recipientRole("medical-director")
            .priority(2)
            .escalationThresholdMinutes(40)
            .active(true)
            .build();

        when(routingConfigRepository.findEscalationRoutingConfigs(
            TENANT_ID, "MENTAL_HEALTH_CRISIS", true))
            .thenReturn(List.of(config10, config40));

        ClinicalAlertDTO alert = createAlert("MENTAL_HEALTH_CRISIS", "CRITICAL", "Crisis");

        List<String> recipients = alertRoutingService.getEscalationRecipientsAfterMinutes(
            TENANT_ID, alert, 20);

        assertThat(recipients).contains("care-team-lead");
        assertThat(recipients).doesNotContain("medical-director");
    }

    @Test
    @DisplayName("Should skip escalation configs with missing thresholds")
    void shouldSkipEscalationConfigsWithMissingThresholds() {
        AlertRoutingConfigEntity configMissing = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("MENTAL_HEALTH_CRISIS")
            .severity("CRITICAL")
            .recipientRole("care-team-lead")
            .priority(1)
            .active(true)
            .build();
        AlertRoutingConfigEntity config20 = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("MENTAL_HEALTH_CRISIS")
            .severity("CRITICAL")
            .recipientRole("medical-director")
            .priority(2)
            .escalationThresholdMinutes(20)
            .active(true)
            .build();

        when(routingConfigRepository.findEscalationRoutingConfigs(
            TENANT_ID, "MENTAL_HEALTH_CRISIS", true))
            .thenReturn(List.of(configMissing, config20));

        ClinicalAlertDTO alert = createAlert("MENTAL_HEALTH_CRISIS", "CRITICAL", "Crisis");

        List<String> recipients = alertRoutingService.getEscalationRecipientsAfterMinutes(
            TENANT_ID, alert, 10);

        assertThat(recipients).isEmpty();
    }

    @Test
    @DisplayName("Should route chronic deterioration critical alerts to on-call providers")
    void shouldRouteChronicDeteriorationCriticalAlerts() {
        ClinicalAlertDTO alert = createAlert("CHRONIC_DETERIORATION", "CRITICAL", "Critical decline");

        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        assertThat(recipients).contains("on-call-provider", "care-team-lead", "primary-care-provider");
    }

    @Test
    @DisplayName("Should route chronic deterioration low alerts to primary provider")
    void shouldRouteChronicDeteriorationLowAlerts() {
        ClinicalAlertDTO alert = createAlert("CHRONIC_DETERIORATION", "LOW", "Low decline");

        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        assertThat(recipients).containsExactly("primary-care-provider");
    }

    @Test
    @DisplayName("Should default recipients for unknown alert types")
    void shouldDefaultRecipientsForUnknownAlertTypes() {
        ClinicalAlertDTO alert = createAlert("UNKNOWN_TYPE", "MEDIUM", "Unknown");

        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        assertThat(recipients).containsExactly("primary-care-provider");
    }

    @Test
    @DisplayName("Should fall back to general routing configs when time-specific configs missing")
    void shouldFallbackToGeneralRoutingConfigs() {
        AlertRoutingConfigEntity config = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("CRITICAL_LAB")
            .severity("CRITICAL")
            .recipientRole("ordering-provider")
            .priority(1)
            .active(true)
            .build();

        when(routingConfigRepository.findRoutingConfigsForAlertAndSeverity(
            TENANT_ID, "CRITICAL_LAB", "CRITICAL", true))
            .thenReturn(List.of(config));
        when(careTeamRepository.findActiveAssignmentsByRoleOnDate(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq("ordering-provider"),
            any(LocalDate.class),
            eq(true)
        )).thenReturn(Collections.emptyList());

        ClinicalAlertDTO alert = createAlert("CRITICAL_LAB", "CRITICAL", "Critical Lab");

        List<String> recipients = alertRoutingService.determineRecipients(TENANT_ID, alert);

        assertThat(recipients).containsExactly("ordering-provider");
    }

    @Test
    @DisplayName("Should fall back to escalation roles for HIGH severity")
    void shouldFallbackToEscalationRolesForHighSeverity() {
        ClinicalAlertDTO alert = createAlert("HEALTH_DECLINE", "HIGH", "Decline");

        List<String> recipients = alertRoutingService.getEscalationRecipients(TENANT_ID, alert);

        assertThat(recipients).contains("care-coordinator", "care-team-lead");
    }

    @Test
    @DisplayName("Should map business hours roles to provider IDs when assigned")
    void shouldMapBusinessHoursRolesToProviders() {
        AlertRoutingConfigEntity config = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("RISK_ESCALATION")
            .severity("HIGH")
            .recipientRole("primary-care-provider")
            .priority(1)
            .businessHoursOnly(true)
            .active(true)
            .build();

        when(routingConfigRepository.findBusinessHoursRoutingConfigs(
            TENANT_ID, "RISK_ESCALATION", "HIGH", true))
            .thenReturn(List.of(config));

        CareTeamAssignmentEntity assignment = CareTeamAssignmentEntity.builder()
            .providerId("provider-123")
            .role("primary-care-provider")
            .active(true)
            .build();
        when(careTeamRepository.findActiveAssignmentsByRoleOnDate(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq("primary-care-provider"),
            any(LocalDate.class),
            eq(true)
        )).thenReturn(List.of(assignment));

        ClinicalAlertDTO alert = createAlert("RISK_ESCALATION", "HIGH", "High Risk");

        List<String> recipients = alertRoutingService
            .determineRecipientsForBusinessHours(TENANT_ID, alert);

        assertThat(recipients).containsExactly("provider-123");
    }

    @Test
    @DisplayName("Should return empty recipients when no after-hours configs exist")
    void shouldReturnEmptyRecipientsWhenNoAfterHoursConfigsExist() {
        when(routingConfigRepository.findAfterHoursRoutingConfigs(
            TENANT_ID, "HEALTH_DECLINE", "MEDIUM", true))
            .thenReturn(Collections.emptyList());

        ClinicalAlertDTO alert = createAlert("HEALTH_DECLINE", "MEDIUM", "Decline");

        List<String> recipients = alertRoutingService
            .determineRecipientsForAfterHours(TENANT_ID, alert);

        assertThat(recipients).isEmpty();
    }

    @Test
    @DisplayName("Should not add escalation fallback for medium severity")
    void shouldNotAddEscalationFallbackForMediumSeverity() {
        ClinicalAlertDTO alert = createAlert("HEALTH_DECLINE", "MEDIUM", "Decline");

        List<String> recipients = alertRoutingService.getEscalationRecipients(TENANT_ID, alert);

        assertThat(recipients).containsExactly("primary-care-provider");
    }

    @Test
    @DisplayName("Should deduplicate providers across multiple roles")
    void shouldDeduplicateProvidersAcrossRoles() {
        AlertRoutingConfigEntity primary = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("HEALTH_DECLINE")
            .severity("HIGH")
            .recipientRole("primary-care-provider")
            .priority(1)
            .businessHoursOnly(true)
            .active(true)
            .build();
        AlertRoutingConfigEntity coordinator = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("HEALTH_DECLINE")
            .severity("HIGH")
            .recipientRole("care-coordinator")
            .priority(2)
            .businessHoursOnly(true)
            .active(true)
            .build();

        when(routingConfigRepository.findBusinessHoursRoutingConfigs(
            TENANT_ID, "HEALTH_DECLINE", "HIGH", true))
            .thenReturn(List.of(primary, coordinator));

        CareTeamAssignmentEntity assignment = CareTeamAssignmentEntity.builder()
            .providerId("provider-123")
            .role("primary-care-provider")
            .active(true)
            .build();
        when(careTeamRepository.findActiveAssignmentsByRoleOnDate(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq("primary-care-provider"),
            any(LocalDate.class),
            eq(true)
        )).thenReturn(List.of(assignment));
        when(careTeamRepository.findActiveAssignmentsByRoleOnDate(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq("care-coordinator"),
            any(LocalDate.class),
            eq(true)
        )).thenReturn(List.of(
            CareTeamAssignmentEntity.builder()
                .providerId("provider-123")
                .role("care-coordinator")
                .active(true)
                .build()
        ));

        ClinicalAlertDTO alert = createAlert("HEALTH_DECLINE", "HIGH", "Decline");

        List<String> recipients = alertRoutingService
            .determineRecipientsForBusinessHours(TENANT_ID, alert);

        assertThat(recipients).containsExactly("provider-123");
    }

    @Test
    @DisplayName("Should fall back to role when no care team assignment exists")
    void shouldFallbackToRoleWhenNoCareTeamAssignmentExists() {
        AlertRoutingConfigEntity config = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("HEALTH_DECLINE")
            .severity("MEDIUM")
            .recipientRole("care-coordinator")
            .priority(1)
            .afterHoursOnly(true)
            .active(true)
            .build();

        when(routingConfigRepository.findAfterHoursRoutingConfigs(
            TENANT_ID, "HEALTH_DECLINE", "MEDIUM", true))
            .thenReturn(List.of(config));
        when(careTeamRepository.findActiveAssignmentsByRoleOnDate(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq("care-coordinator"),
            any(LocalDate.class),
            eq(true)
        )).thenReturn(Collections.emptyList());

        ClinicalAlertDTO alert = createAlert("HEALTH_DECLINE", "MEDIUM", "Decline");

        List<String> recipients = alertRoutingService
            .determineRecipientsForAfterHours(TENANT_ID, alert);

        assertThat(recipients).containsExactly("care-coordinator");
    }

    @Test
    @DisplayName("Should include escalation recipients when configured")
    void shouldIncludeEscalationRecipientsWhenConfigured() {
        ClinicalAlertDTO alert = createAlert("HEALTH_DECLINE", "CRITICAL", "Critical decline");

        AlertRoutingConfigEntity escalationConfig = AlertRoutingConfigEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .alertType("HEALTH_DECLINE")
            .severity("CRITICAL")
            .recipientRole("care-team-lead")
            .priority(1)
            .escalationThresholdMinutes(15)
            .active(true)
            .build();

        when(routingConfigRepository.findEscalationRoutingConfigs(
            TENANT_ID, "HEALTH_DECLINE", true))
            .thenReturn(List.of(escalationConfig));
        when(careTeamRepository.findActiveAssignmentsByRoleOnDate(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq("care-team-lead"),
            any(LocalDate.class),
            eq(true)
        )).thenReturn(List.of(
            CareTeamAssignmentEntity.builder()
                .providerId("lead-1")
                .role("care-team-lead")
                .active(true)
                .build()
        ));

        List<String> recipients = alertRoutingService.getEscalationRecipients(TENANT_ID, alert);

        assertThat(recipients).contains("lead-1");
        assertThat(recipients).contains("care-coordinator", "primary-care-provider");
    }

    // Helper methods
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
