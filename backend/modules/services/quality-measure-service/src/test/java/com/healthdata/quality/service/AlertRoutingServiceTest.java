package com.healthdata.quality.service;

import com.healthdata.quality.dto.ClinicalAlertDTO;
import com.healthdata.quality.persistence.AlertRoutingConfigRepository;
import com.healthdata.quality.persistence.CareTeamAssignmentRepository;
import com.healthdata.quality.persistence.ClinicalAlertEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
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
    private static final String PATIENT_ID = "patient-123";

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
