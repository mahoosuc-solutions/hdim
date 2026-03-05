package com.healthdata.admin.service;

import com.healthdata.admin.domain.AlertConfig;
import com.healthdata.admin.domain.AlertConfig.AlertSeverity;
import com.healthdata.admin.domain.AlertConfig.AlertType;
import com.healthdata.admin.domain.AlertConfig.NotificationChannel;
import com.healthdata.admin.dto.AlertConfigRequest;
import com.healthdata.admin.dto.AlertConfigResponse;
import com.healthdata.admin.dto.AlertConfigUpdateRequest;
import com.healthdata.admin.repository.AlertConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AlertConfigService.
 *
 * Validates business logic for alert configuration CRUD operations,
 * multi-tenant isolation, and partial update behavior.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AlertConfigServiceTest {

    private static final String TENANT_ID = "test-tenant-1";
    private static final String OTHER_TENANT_ID = "test-tenant-2";
    private static final String USERNAME = "admin-user";

    @Mock
    private AlertConfigRepository alertConfigRepository;

    @InjectMocks
    private AlertConfigService alertConfigService;

    private UUID alertId;
    private AlertConfig testConfig;
    private AlertConfigRequest createRequest;

    @BeforeEach
    void setUp() {
        alertId = UUID.randomUUID();

        testConfig = AlertConfig.builder()
                .id(alertId)
                .tenantId(TENANT_ID)
                .serviceName("patient-service")
                .displayName("High CPU Alert")
                .alertType(AlertType.CPU_USAGE)
                .threshold(90.0)
                .durationMinutes(5)
                .severity(AlertSeverity.CRITICAL)
                .enabled(true)
                .notificationChannels(List.of(NotificationChannel.EMAIL, NotificationChannel.SLACK))
                .createdBy(USERNAME)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        createRequest = AlertConfigRequest.builder()
                .serviceName("patient-service")
                .displayName("High CPU Alert")
                .alertType(AlertType.CPU_USAGE)
                .threshold(90.0)
                .durationMinutes(5)
                .severity(AlertSeverity.CRITICAL)
                .enabled(true)
                .notificationChannels(List.of(NotificationChannel.EMAIL, NotificationChannel.SLACK))
                .build();
    }

    // ========================================================================
    // getAllAlertConfigs
    // ========================================================================

    @Test
    void getAllAlertConfigs_shouldReturnListOfResponses() {
        AlertConfig second = AlertConfig.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .serviceName("care-gap-service")
                .displayName("Error Rate Alert")
                .alertType(AlertType.ERROR_RATE)
                .threshold(5.0)
                .durationMinutes(10)
                .severity(AlertSeverity.WARNING)
                .enabled(true)
                .notificationChannels(List.of(NotificationChannel.WEBHOOK))
                .createdBy(USERNAME)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(alertConfigRepository.findByTenantId(TENANT_ID))
                .thenReturn(List.of(testConfig, second));

        List<AlertConfigResponse> result = alertConfigService.getAllAlertConfigs(TENANT_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getServiceName()).isEqualTo("patient-service");
        assertThat(result.get(1).getServiceName()).isEqualTo("care-gap-service");
        verify(alertConfigRepository).findByTenantId(TENANT_ID);
    }

    @Test
    void getAllAlertConfigs_shouldReturnEmptyListWhenNoConfigs() {
        when(alertConfigRepository.findByTenantId(TENANT_ID))
                .thenReturn(Collections.emptyList());

        List<AlertConfigResponse> result = alertConfigService.getAllAlertConfigs(TENANT_ID);

        assertThat(result).isEmpty();
        verify(alertConfigRepository).findByTenantId(TENANT_ID);
    }

    // ========================================================================
    // getAlertConfig
    // ========================================================================

    @Test
    void getAlertConfig_shouldReturnResponse_whenFound() {
        when(alertConfigRepository.findByIdAndTenantId(alertId, TENANT_ID))
                .thenReturn(Optional.of(testConfig));

        AlertConfigResponse result = alertConfigService.getAlertConfig(TENANT_ID, alertId);

        assertThat(result.getId()).isEqualTo(alertId);
        assertThat(result.getServiceName()).isEqualTo("patient-service");
        assertThat(result.getDisplayName()).isEqualTo("High CPU Alert");
        assertThat(result.getAlertType()).isEqualTo(AlertType.CPU_USAGE);
        assertThat(result.getThreshold()).isEqualTo(90.0);
        assertThat(result.getDurationMinutes()).isEqualTo(5);
        assertThat(result.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(result.getEnabled()).isTrue();
        assertThat(result.getNotificationChannels())
                .containsExactly(NotificationChannel.EMAIL, NotificationChannel.SLACK);
        assertThat(result.getCreatedBy()).isEqualTo(USERNAME);
        verify(alertConfigRepository).findByIdAndTenantId(alertId, TENANT_ID);
    }

    @Test
    void getAlertConfig_shouldThrow_whenNotFound() {
        UUID missingId = UUID.randomUUID();
        when(alertConfigRepository.findByIdAndTenantId(missingId, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertConfigService.getAlertConfig(TENANT_ID, missingId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Alert configuration not found")
                .hasMessageContaining(missingId.toString());
    }

    @Test
    void getAlertConfig_shouldEnforceMultiTenantIsolation() {
        // Alert exists for TENANT_ID but queried with OTHER_TENANT_ID
        when(alertConfigRepository.findByIdAndTenantId(alertId, OTHER_TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertConfigService.getAlertConfig(OTHER_TENANT_ID, alertId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Alert configuration not found");

        verify(alertConfigRepository).findByIdAndTenantId(alertId, OTHER_TENANT_ID);
        verify(alertConfigRepository, never()).findByIdAndTenantId(alertId, TENANT_ID);
    }

    // ========================================================================
    // createAlertConfig
    // ========================================================================

    @Test
    void createAlertConfig_shouldSaveAndReturnResponse() {
        when(alertConfigRepository.save(any(AlertConfig.class))).thenReturn(testConfig);

        AlertConfigResponse result = alertConfigService.createAlertConfig(TENANT_ID, createRequest, USERNAME);

        assertThat(result.getId()).isEqualTo(alertId);
        assertThat(result.getServiceName()).isEqualTo("patient-service");
        assertThat(result.getDisplayName()).isEqualTo("High CPU Alert");
        assertThat(result.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);

        ArgumentCaptor<AlertConfig> captor = ArgumentCaptor.forClass(AlertConfig.class);
        verify(alertConfigRepository).save(captor.capture());

        AlertConfig saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.getServiceName()).isEqualTo("patient-service");
        assertThat(saved.getDisplayName()).isEqualTo("High CPU Alert");
        assertThat(saved.getAlertType()).isEqualTo(AlertType.CPU_USAGE);
        assertThat(saved.getThreshold()).isEqualTo(90.0);
        assertThat(saved.getDurationMinutes()).isEqualTo(5);
        assertThat(saved.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.getNotificationChannels())
                .containsExactly(NotificationChannel.EMAIL, NotificationChannel.SLACK);
        assertThat(saved.getCreatedBy()).isEqualTo(USERNAME);
    }

    @Test
    void createAlertConfig_shouldSetTenantIdFromParameter() {
        when(alertConfigRepository.save(any(AlertConfig.class))).thenReturn(testConfig);

        alertConfigService.createAlertConfig(OTHER_TENANT_ID, createRequest, USERNAME);

        ArgumentCaptor<AlertConfig> captor = ArgumentCaptor.forClass(AlertConfig.class);
        verify(alertConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(OTHER_TENANT_ID);
    }

    @Test
    void createAlertConfig_shouldSetCreatedByFromUsername() {
        String differentUser = "another-admin";
        when(alertConfigRepository.save(any(AlertConfig.class))).thenReturn(testConfig);

        alertConfigService.createAlertConfig(TENANT_ID, createRequest, differentUser);

        ArgumentCaptor<AlertConfig> captor = ArgumentCaptor.forClass(AlertConfig.class);
        verify(alertConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(differentUser);
    }

    // ========================================================================
    // updateAlertConfig
    // ========================================================================

    @Test
    void updateAlertConfig_shouldUpdateAllProvidedFields() {
        AlertConfigUpdateRequest updateRequest = AlertConfigUpdateRequest.builder()
                .threshold(95.0)
                .durationMinutes(10)
                .severity(AlertSeverity.WARNING)
                .enabled(false)
                .notificationChannels(List.of(NotificationChannel.WEBHOOK))
                .build();

        when(alertConfigRepository.findByIdAndTenantId(alertId, TENANT_ID))
                .thenReturn(Optional.of(testConfig));
        when(alertConfigRepository.save(any(AlertConfig.class))).thenReturn(testConfig);

        alertConfigService.updateAlertConfig(TENANT_ID, alertId, updateRequest);

        ArgumentCaptor<AlertConfig> captor = ArgumentCaptor.forClass(AlertConfig.class);
        verify(alertConfigRepository).save(captor.capture());

        AlertConfig updated = captor.getValue();
        assertThat(updated.getThreshold()).isEqualTo(95.0);
        assertThat(updated.getDurationMinutes()).isEqualTo(10);
        assertThat(updated.getSeverity()).isEqualTo(AlertSeverity.WARNING);
        assertThat(updated.getEnabled()).isFalse();
        assertThat(updated.getNotificationChannels())
                .containsExactly(NotificationChannel.WEBHOOK);
    }

    @Test
    void updateAlertConfig_shouldOnlyUpdateThreshold_whenOnlyThresholdProvided() {
        AlertConfigUpdateRequest updateRequest = AlertConfigUpdateRequest.builder()
                .threshold(75.0)
                .build();

        when(alertConfigRepository.findByIdAndTenantId(alertId, TENANT_ID))
                .thenReturn(Optional.of(testConfig));
        when(alertConfigRepository.save(any(AlertConfig.class))).thenReturn(testConfig);

        alertConfigService.updateAlertConfig(TENANT_ID, alertId, updateRequest);

        ArgumentCaptor<AlertConfig> captor = ArgumentCaptor.forClass(AlertConfig.class);
        verify(alertConfigRepository).save(captor.capture());

        AlertConfig updated = captor.getValue();
        // Threshold changed
        assertThat(updated.getThreshold()).isEqualTo(75.0);
        // Other fields remain unchanged
        assertThat(updated.getDurationMinutes()).isEqualTo(5);
        assertThat(updated.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(updated.getEnabled()).isTrue();
        assertThat(updated.getNotificationChannels())
                .containsExactly(NotificationChannel.EMAIL, NotificationChannel.SLACK);
        assertThat(updated.getServiceName()).isEqualTo("patient-service");
        assertThat(updated.getDisplayName()).isEqualTo("High CPU Alert");
    }

    @Test
    void updateAlertConfig_shouldThrow_whenNotFound() {
        UUID missingId = UUID.randomUUID();
        AlertConfigUpdateRequest updateRequest = AlertConfigUpdateRequest.builder()
                .threshold(50.0)
                .build();

        when(alertConfigRepository.findByIdAndTenantId(missingId, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                alertConfigService.updateAlertConfig(TENANT_ID, missingId, updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Alert configuration not found")
                .hasMessageContaining(missingId.toString());

        verify(alertConfigRepository, never()).save(any());
    }

    @Test
    void updateAlertConfig_shouldNotModifyFields_whenAllNull() {
        AlertConfigUpdateRequest emptyRequest = AlertConfigUpdateRequest.builder().build();

        when(alertConfigRepository.findByIdAndTenantId(alertId, TENANT_ID))
                .thenReturn(Optional.of(testConfig));
        when(alertConfigRepository.save(any(AlertConfig.class))).thenReturn(testConfig);

        alertConfigService.updateAlertConfig(TENANT_ID, alertId, emptyRequest);

        ArgumentCaptor<AlertConfig> captor = ArgumentCaptor.forClass(AlertConfig.class);
        verify(alertConfigRepository).save(captor.capture());

        AlertConfig updated = captor.getValue();
        assertThat(updated.getThreshold()).isEqualTo(90.0);
        assertThat(updated.getDurationMinutes()).isEqualTo(5);
        assertThat(updated.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(updated.getEnabled()).isTrue();
    }

    // ========================================================================
    // deleteAlertConfig
    // ========================================================================

    @Test
    void deleteAlertConfig_shouldDeleteExistingConfig() {
        when(alertConfigRepository.findByIdAndTenantId(alertId, TENANT_ID))
                .thenReturn(Optional.of(testConfig));

        alertConfigService.deleteAlertConfig(TENANT_ID, alertId);

        verify(alertConfigRepository).delete(testConfig);
    }

    @Test
    void deleteAlertConfig_shouldThrow_whenNotFound() {
        UUID missingId = UUID.randomUUID();
        when(alertConfigRepository.findByIdAndTenantId(missingId, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertConfigService.deleteAlertConfig(TENANT_ID, missingId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Alert configuration not found");

        verify(alertConfigRepository, never()).delete(any());
    }

    @Test
    void deleteAlertConfig_shouldEnforceMultiTenantIsolation() {
        when(alertConfigRepository.findByIdAndTenantId(alertId, OTHER_TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertConfigService.deleteAlertConfig(OTHER_TENANT_ID, alertId))
                .isInstanceOf(IllegalArgumentException.class);

        verify(alertConfigRepository, never()).delete(any());
    }

    // ========================================================================
    // getEnabledAlertConfigs
    // ========================================================================

    @Test
    void getEnabledAlertConfigs_shouldReturnEnabledEntities() {
        when(alertConfigRepository.findByTenantIdAndEnabled(TENANT_ID, true))
                .thenReturn(List.of(testConfig));

        List<AlertConfig> result = alertConfigService.getEnabledAlertConfigs(TENANT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(alertId);
        assertThat(result.get(0).getEnabled()).isTrue();
        verify(alertConfigRepository).findByTenantIdAndEnabled(TENANT_ID, true);
    }

    @Test
    void getEnabledAlertConfigs_shouldReturnEmptyList_whenNoneEnabled() {
        when(alertConfigRepository.findByTenantIdAndEnabled(TENANT_ID, true))
                .thenReturn(Collections.emptyList());

        List<AlertConfig> result = alertConfigService.getEnabledAlertConfigs(TENANT_ID);

        assertThat(result).isEmpty();
    }

    // ========================================================================
    // getAllEnabledAlertConfigs
    // ========================================================================

    @Test
    void getAllEnabledAlertConfigs_shouldReturnEnabledAcrossTenants() {
        AlertConfig otherTenantConfig = AlertConfig.builder()
                .id(UUID.randomUUID())
                .tenantId(OTHER_TENANT_ID)
                .serviceName("fhir-service")
                .displayName("Latency Alert")
                .alertType(AlertType.LATENCY)
                .threshold(500.0)
                .durationMinutes(3)
                .severity(AlertSeverity.WARNING)
                .enabled(true)
                .notificationChannels(List.of(NotificationChannel.SMS))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(alertConfigRepository.findByEnabled(true))
                .thenReturn(List.of(testConfig, otherTenantConfig));

        List<AlertConfig> result = alertConfigService.getAllEnabledAlertConfigs();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(AlertConfig::getTenantId)
                .containsExactly(TENANT_ID, OTHER_TENANT_ID);
        verify(alertConfigRepository).findByEnabled(true);
    }

    @Test
    void getAllEnabledAlertConfigs_shouldReturnEmptyList_whenNoneEnabled() {
        when(alertConfigRepository.findByEnabled(true))
                .thenReturn(Collections.emptyList());

        List<AlertConfig> result = alertConfigService.getAllEnabledAlertConfigs();

        assertThat(result).isEmpty();
    }

    // ========================================================================
    // markAlertTriggered
    // ========================================================================

    @Test
    void markAlertTriggered_shouldUpdateLastTriggered_whenAlertExists() {
        assertThat(testConfig.getLastTriggered()).isNull();

        when(alertConfigRepository.findById(alertId))
                .thenReturn(Optional.of(testConfig));
        when(alertConfigRepository.save(any(AlertConfig.class))).thenReturn(testConfig);

        alertConfigService.markAlertTriggered(alertId);

        ArgumentCaptor<AlertConfig> captor = ArgumentCaptor.forClass(AlertConfig.class);
        verify(alertConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getLastTriggered()).isNotNull();
    }

    @Test
    void markAlertTriggered_shouldDoNothing_whenAlertDoesNotExist() {
        UUID missingId = UUID.randomUUID();
        when(alertConfigRepository.findById(missingId))
                .thenReturn(Optional.empty());

        // Should not throw -- uses ifPresent
        alertConfigService.markAlertTriggered(missingId);

        verify(alertConfigRepository).findById(missingId);
        verify(alertConfigRepository, never()).save(any());
    }
}
