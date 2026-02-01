package com.healthdata.featureflags;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TenantFeatureFlagService
 */
@ExtendWith(MockitoExtension.class)
class TenantFeatureFlagServiceTest {

    @Mock
    private TenantFeatureFlagRepository repository;

    private TenantFeatureFlagService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new TenantFeatureFlagService(repository, objectMapper);
    }

    @Test
    void isFeatureEnabled_ShouldReturnTrue_WhenFeatureEnabled() {
        // Given
        String tenantId = "tenant1";
        String featureKey = "twilio-sms-reminders";
        when(repository.isFeatureEnabled(tenantId, featureKey)).thenReturn(true);

        // When
        boolean enabled = service.isFeatureEnabled(tenantId, featureKey);

        // Then
        assertThat(enabled).isTrue();
        verify(repository).isFeatureEnabled(tenantId, featureKey);
    }

    @Test
    void isFeatureEnabled_ShouldReturnFalse_WhenFeatureDisabled() {
        // Given
        String tenantId = "tenant1";
        String featureKey = "twilio-sms-reminders";
        when(repository.isFeatureEnabled(tenantId, featureKey)).thenReturn(false);
        when(repository.isFeatureEnabled("default", featureKey)).thenReturn(false);

        // When
        boolean enabled = service.isFeatureEnabled(tenantId, featureKey);

        // Then
        assertThat(enabled).isFalse();
        verify(repository).isFeatureEnabled(tenantId, featureKey);
        verify(repository).isFeatureEnabled("default", featureKey);
    }

    @Test
    void isFeatureEnabled_ShouldFallbackToDefault_WhenTenantSpecificNotFound() {
        // Given
        String tenantId = "tenant1";
        String featureKey = "twilio-sms-reminders";
        when(repository.isFeatureEnabled(tenantId, featureKey)).thenReturn(false);
        when(repository.isFeatureEnabled("default", featureKey)).thenReturn(true);

        // When
        boolean enabled = service.isFeatureEnabled(tenantId, featureKey);

        // Then
        assertThat(enabled).isTrue();
        verify(repository).isFeatureEnabled(tenantId, featureKey);
        verify(repository).isFeatureEnabled("default", featureKey);
    }

    @Test
    void getFeatureConfig_ShouldReturnConfig_WhenExists() {
        // Given
        String tenantId = "tenant1";
        String featureKey = "twilio-sms-reminders";
        String configJson = "{\"reminder_days\": [1, 3, 7]}";

        TenantFeatureFlagEntity entity = TenantFeatureFlagEntity.builder()
                .tenantId(tenantId)
                .featureKey(featureKey)
                .enabled(true)
                .configJson(configJson)
                .build();

        when(repository.findByTenantIdAndFeatureKey(tenantId, featureKey))
                .thenReturn(Optional.of(entity));

        // When
        Map<String, Object> config = service.getFeatureConfig(tenantId, featureKey);

        // Then
        assertThat(config).isNotEmpty();
        assertThat(config.get("reminder_days")).isNotNull();
    }

    @Test
    void getFeatureConfig_ShouldReturnEmpty_WhenNotFound() {
        // Given
        String tenantId = "tenant1";
        String featureKey = "twilio-sms-reminders";
        when(repository.findByTenantIdAndFeatureKey(tenantId, featureKey))
                .thenReturn(Optional.empty());
        when(repository.findByTenantIdAndFeatureKey("default", featureKey))
                .thenReturn(Optional.empty());

        // When
        Map<String, Object> config = service.getFeatureConfig(tenantId, featureKey);

        // Then
        assertThat(config).isEmpty();
    }

    @Test
    void enableFeature_ShouldCreateNewFlag_WhenNotExists() {
        // Given
        String tenantId = "tenant1";
        String featureKey = "twilio-sms-reminders";
        String userId = "admin-user";
        Map<String, Object> config = Map.of("reminder_days", List.of(1, 3, 7));

        when(repository.findByTenantIdAndFeatureKey(tenantId, featureKey))
                .thenReturn(Optional.empty());
        when(repository.save(any(TenantFeatureFlagEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        service.enableFeature(tenantId, featureKey, config, userId);

        // Then
        ArgumentCaptor<TenantFeatureFlagEntity> captor = ArgumentCaptor.forClass(TenantFeatureFlagEntity.class);
        verify(repository).save(captor.capture());

        TenantFeatureFlagEntity savedEntity = captor.getValue();
        assertThat(savedEntity.getTenantId()).isEqualTo(tenantId);
        assertThat(savedEntity.getFeatureKey()).isEqualTo(featureKey);
        assertThat(savedEntity.getEnabled()).isTrue();
        assertThat(savedEntity.getConfigJson()).isNotNull();
    }

    @Test
    void enableFeature_ShouldUpdateExistingFlag_WhenExists() {
        // Given
        String tenantId = "tenant1";
        String featureKey = "twilio-sms-reminders";
        String userId = "admin-user";
        Map<String, Object> config = Map.of("reminder_days", List.of(1, 3, 7));

        TenantFeatureFlagEntity existingEntity = TenantFeatureFlagEntity.builder()
                .tenantId(tenantId)
                .featureKey(featureKey)
                .enabled(false)
                .build();

        when(repository.findByTenantIdAndFeatureKey(tenantId, featureKey))
                .thenReturn(Optional.of(existingEntity));
        when(repository.save(any(TenantFeatureFlagEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        service.enableFeature(tenantId, featureKey, config, userId);

        // Then
        verify(repository).save(existingEntity);
        assertThat(existingEntity.getEnabled()).isTrue();
        assertThat(existingEntity.getUpdatedBy()).isEqualTo(userId);
    }

    @Test
    void disableFeature_ShouldUpdateFlag_WhenExists() {
        // Given
        String tenantId = "tenant1";
        String featureKey = "twilio-sms-reminders";
        String userId = "admin-user";

        TenantFeatureFlagEntity existingEntity = TenantFeatureFlagEntity.builder()
                .tenantId(tenantId)
                .featureKey(featureKey)
                .enabled(true)
                .build();

        when(repository.findByTenantIdAndFeatureKey(tenantId, featureKey))
                .thenReturn(Optional.of(existingEntity));

        // When
        service.disableFeature(tenantId, featureKey, userId);

        // Then
        verify(repository).save(existingEntity);
        assertThat(existingEntity.getEnabled()).isFalse();
        assertThat(existingEntity.getUpdatedBy()).isEqualTo(userId);
    }

    @Test
    void disableFeature_ShouldDoNothing_WhenNotExists() {
        // Given
        String tenantId = "tenant1";
        String featureKey = "twilio-sms-reminders";
        String userId = "admin-user";

        when(repository.findByTenantIdAndFeatureKey(tenantId, featureKey))
                .thenReturn(Optional.empty());

        // When
        service.disableFeature(tenantId, featureKey, userId);

        // Then
        verify(repository, never()).save(any());
    }

    @Test
    void updateFeatureConfig_ShouldUpdateConfig_WhenExists() {
        // Given
        String tenantId = "tenant1";
        String featureKey = "twilio-sms-reminders";
        String userId = "admin-user";
        Map<String, Object> newConfig = Map.of("reminder_days", List.of(1, 7));

        TenantFeatureFlagEntity existingEntity = TenantFeatureFlagEntity.builder()
                .tenantId(tenantId)
                .featureKey(featureKey)
                .enabled(true)
                .configJson("{\"reminder_days\": [1, 3, 7]}")
                .build();

        when(repository.findByTenantIdAndFeatureKey(tenantId, featureKey))
                .thenReturn(Optional.of(existingEntity));

        // When
        service.updateFeatureConfig(tenantId, featureKey, newConfig, userId);

        // Then
        verify(repository).save(existingEntity);
        assertThat(existingEntity.getConfigJson()).contains("\"reminder_days\"");
        assertThat(existingEntity.getUpdatedBy()).isEqualTo(userId);
    }

    @Test
    void findTenantsWithFeatureEnabled_ShouldReturnTenantIds() {
        // Given
        String featureKey = "twilio-sms-reminders";
        List<String> expectedTenants = List.of("tenant1", "tenant2", "tenant3");
        when(repository.findTenantIdsWithFeatureEnabled(featureKey))
                .thenReturn(expectedTenants);

        // When
        List<String> tenants = service.findTenantsWithFeatureEnabled(featureKey);

        // Then
        assertThat(tenants).hasSize(3);
        assertThat(tenants).containsExactlyElementsOf(expectedTenants);
    }

    @Test
    void getEnabledFeatures_ShouldReturnFeatureKeys() {
        // Given
        String tenantId = "tenant1";
        List<TenantFeatureFlagEntity> enabledFlags = List.of(
                TenantFeatureFlagEntity.builder()
                        .tenantId(tenantId)
                        .featureKey("twilio-sms-reminders")
                        .enabled(true)
                        .build(),
                TenantFeatureFlagEntity.builder()
                        .tenantId(tenantId)
                        .featureKey("smart-on-fhir")
                        .enabled(true)
                        .build()
        );

        when(repository.findEnabledByTenantId(tenantId)).thenReturn(enabledFlags);

        // When
        List<String> features = service.getEnabledFeatures(tenantId);

        // Then
        assertThat(features).hasSize(2);
        assertThat(features).containsExactly("twilio-sms-reminders", "smart-on-fhir");
    }
}
