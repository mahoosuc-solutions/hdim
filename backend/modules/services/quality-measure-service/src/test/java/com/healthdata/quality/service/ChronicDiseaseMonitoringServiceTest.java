package com.healthdata.quality.service;

import com.healthdata.quality.persistence.ChronicDiseaseMonitoringEntity;
import com.healthdata.quality.persistence.ChronicDiseaseMonitoringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for ChronicDiseaseMonitoringService
 *
 * Tests chronic disease deterioration detection with:
 * - HbA1c trend detection (diabetes)
 * - Blood pressure trend detection (hypertension)
 * - Deterioration alert triggering
 * - Improvement detection
 * - Threshold-based alerts
 */
@ExtendWith(MockitoExtension.class)
class ChronicDiseaseMonitoringServiceTest {

    @Mock
    private ChronicDiseaseMonitoringRepository monitoringRepository;

    @Mock
    private DiseaseDeteriorationDetector diseaseDeteriorationDetector;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ChronicDiseaseMonitoringService monitoringService;

    private static final String TENANT_ID = "test-tenant";
    private static final String PATIENT_ID = "patient-123";

    @BeforeEach
    void setUp() {
        reset(monitoringRepository, diseaseDeteriorationDetector, kafkaTemplate);
    }

    @Test
    void testHbA1cTrendDetection_Deteriorating() {
        // Given: Previous HbA1c was 7.5%, new value is 9.2%
        ChronicDiseaseMonitoringEntity previousMonitoring = ChronicDiseaseMonitoringEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .diseaseCode("44054006")
            .diseaseName("Type 2 Diabetes Mellitus")
            .latestValue(7.5)
            .previousValue(7.0)
            .trend(ChronicDiseaseMonitoringEntity.Trend.STABLE)
            .alertTriggered(false)
            .monitoredAt(Instant.now().minusSeconds(86400 * 90)) // 90 days ago
            .build();

        when(monitoringRepository.findByTenantIdAndPatientIdAndDiseaseCode(TENANT_ID, PATIENT_ID, "44054006"))
            .thenReturn(Optional.of(previousMonitoring));

        when(diseaseDeteriorationDetector.analyzeTrend(eq("HbA1c"), eq(7.5), eq(9.2)))
            .thenReturn(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING);

        when(diseaseDeteriorationDetector.shouldTriggerAlert(eq("HbA1c"), eq(9.2), eq(7.5)))
            .thenReturn(true);

        when(diseaseDeteriorationDetector.getDeteriorationSeverity(eq("HbA1c"), eq(9.2)))
            .thenReturn("HIGH");

        Map<String, Object> observationData = Map.of(
            "resourceType", "Observation",
            "code", Map.of(
                "coding", java.util.List.of(Map.of(
                    "system", "http://loinc.org",
                    "code", "4548-4",
                    "display", "Hemoglobin A1c"
                ))
            ),
            "valueQuantity", Map.of(
                "value", 9.2,
                "unit", "%"
            ),
            "subject", Map.of("reference", "Patient/" + PATIENT_ID)
        );

        ChronicDiseaseMonitoringEntity updatedMonitoring = ChronicDiseaseMonitoringEntity.builder()
            .id(previousMonitoring.getId())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .diseaseCode("44054006")
            .diseaseName("Type 2 Diabetes Mellitus")
            .latestValue(9.2)
            .previousValue(7.5)
            .trend(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING)
            .alertTriggered(true)
            .monitoredAt(Instant.now())
            .build();

        when(monitoringRepository.save(any(ChronicDiseaseMonitoringEntity.class)))
            .thenReturn(updatedMonitoring);

        // When: New HbA1c observation is processed
        ChronicDiseaseMonitoringEntity result = monitoringService.processLabResult(
            TENANT_ID, PATIENT_ID, observationData);

        // Then: Trend should be DETERIORATING
        assertThat(result.getTrend()).isEqualTo(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING);
        assertThat(result.isAlertTriggered()).isTrue();
        assertThat(result.getLatestValue()).isEqualTo(9.2);
        assertThat(result.getPreviousValue()).isEqualTo(7.5);

        // Verify deterioration event was published
        verify(kafkaTemplate).send(eq("chronic-disease.deterioration"), argThat(event -> {
            if (event instanceof Map) {
                Map<String, Object> eventMap = (Map<String, Object>) event;
                return eventMap.get("patientId").equals(PATIENT_ID) &&
                       eventMap.get("diseaseCode").equals("44054006") &&
                       eventMap.get("metric").equals("HbA1c") &&
                       eventMap.get("previousValue").equals(7.5) &&
                       eventMap.get("newValue").equals(9.2);
            }
            return false;
        }));
    }

    @Test
    void testBloodPressureTrendDetection_Hypertension() {
        // Given: Previous BP was 135/85, new BP is 165/95 (deteriorating)
        ChronicDiseaseMonitoringEntity previousMonitoring = ChronicDiseaseMonitoringEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .diseaseCode("38341003")
            .diseaseName("Hypertensive disorder")
            .latestValue(135.0)
            .previousValue(130.0)
            .trend(ChronicDiseaseMonitoringEntity.Trend.STABLE)
            .alertTriggered(false)
            .monitoredAt(Instant.now().minusSeconds(86400 * 30))
            .build();

        when(monitoringRepository.findByTenantIdAndPatientIdAndDiseaseCode(TENANT_ID, PATIENT_ID, "38341003"))
            .thenReturn(Optional.of(previousMonitoring));

        when(diseaseDeteriorationDetector.analyzeTrend(eq("BP_SYSTOLIC"), eq(135.0), eq(165.0)))
            .thenReturn(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING);

        when(diseaseDeteriorationDetector.shouldTriggerAlert(eq("BP_SYSTOLIC"), eq(165.0), eq(135.0)))
            .thenReturn(true);

        when(diseaseDeteriorationDetector.getDeteriorationSeverity(eq("BP_SYSTOLIC"), eq(165.0)))
            .thenReturn("HIGH");

        Map<String, Object> observationData = Map.of(
            "resourceType", "Observation",
            "code", Map.of(
                "coding", java.util.List.of(Map.of(
                    "system", "http://loinc.org",
                    "code", "8480-6",
                    "display", "Systolic blood pressure"
                ))
            ),
            "valueQuantity", Map.of(
                "value", 165,
                "unit", "mmHg"
            )
        );

        ChronicDiseaseMonitoringEntity updatedMonitoring = ChronicDiseaseMonitoringEntity.builder()
            .id(previousMonitoring.getId())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .diseaseCode("38341003")
            .diseaseName("Hypertensive disorder")
            .latestValue(165.0)
            .previousValue(135.0)
            .trend(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING)
            .alertTriggered(true)
            .monitoredAt(Instant.now())
            .build();

        when(monitoringRepository.save(any(ChronicDiseaseMonitoringEntity.class)))
            .thenReturn(updatedMonitoring);

        // When: New BP observation is processed
        ChronicDiseaseMonitoringEntity result = monitoringService.processLabResult(
            TENANT_ID, PATIENT_ID, observationData);

        // Then: Should detect deterioration and trigger alert
        assertThat(result.getTrend()).isEqualTo(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING);
        assertThat(result.isAlertTriggered()).isTrue();

        // Verify alert was published
        verify(kafkaTemplate).send(eq("chronic-disease.deterioration"), any());
    }

    @Test
    void testDeteriorationAlertTriggering() {
        // Given: HbA1c increases by more than 1% (threshold for alert)
        when(monitoringRepository.findByTenantIdAndPatientIdAndDiseaseCode(TENANT_ID, PATIENT_ID, "44054006"))
            .thenReturn(Optional.empty());

        when(diseaseDeteriorationDetector.analyzeTrend(eq("HbA1c"), isNull(), eq(9.5)))
            .thenReturn(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING);

        when(diseaseDeteriorationDetector.shouldTriggerAlert(eq("HbA1c"), eq(9.5), isNull()))
            .thenReturn(true);

        when(diseaseDeteriorationDetector.getDeteriorationSeverity(eq("HbA1c"), eq(9.5)))
            .thenReturn("HIGH");

        Map<String, Object> observationData = Map.of(
            "resourceType", "Observation",
            "code", Map.of(
                "coding", java.util.List.of(Map.of("code", "4548-4", "display", "Hemoglobin A1c"))
            ),
            "valueQuantity", Map.of("value", 9.5, "unit", "%")
        );

        ChronicDiseaseMonitoringEntity newMonitoring = ChronicDiseaseMonitoringEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .diseaseCode("44054006")
            .diseaseName("Type 2 Diabetes Mellitus")
            .latestValue(9.5)
            .previousValue(null)
            .trend(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING)
            .alertTriggered(true)
            .monitoredAt(Instant.now())
            .build();

        when(monitoringRepository.save(any(ChronicDiseaseMonitoringEntity.class)))
            .thenReturn(newMonitoring);

        // When: Alert threshold is exceeded
        ChronicDiseaseMonitoringEntity result = monitoringService.processLabResult(
            TENANT_ID, PATIENT_ID, observationData);

        // Then: Alert should be triggered
        assertThat(result.isAlertTriggered()).isTrue();

        ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);
        verify(kafkaTemplate).send(eq("chronic-disease.deterioration"), eventCaptor.capture());

        Map<String, Object> event = eventCaptor.getValue();
        assertThat(event.get("alertLevel")).isEqualTo("HIGH");
        assertThat(event.get("metric")).isEqualTo("HbA1c");
        assertThat(event.get("newValue")).isEqualTo(9.5);
    }

    @Test
    void testImprovementDetection() {
        // Given: HbA1c improves from 9.0% to 7.0%
        ChronicDiseaseMonitoringEntity previousMonitoring = ChronicDiseaseMonitoringEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .diseaseCode("44054006")
            .diseaseName("Type 2 Diabetes Mellitus")
            .latestValue(9.0)
            .previousValue(9.5)
            .trend(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING)
            .alertTriggered(true)
            .monitoredAt(Instant.now().minusSeconds(86400 * 90))
            .build();

        when(monitoringRepository.findByTenantIdAndPatientIdAndDiseaseCode(TENANT_ID, PATIENT_ID, "44054006"))
            .thenReturn(Optional.of(previousMonitoring));

        when(diseaseDeteriorationDetector.analyzeTrend(eq("HbA1c"), eq(9.0), eq(7.0)))
            .thenReturn(ChronicDiseaseMonitoringEntity.Trend.IMPROVING);

        when(diseaseDeteriorationDetector.shouldTriggerAlert(eq("HbA1c"), eq(7.0), eq(9.0)))
            .thenReturn(false);

        Map<String, Object> observationData = Map.of(
            "resourceType", "Observation",
            "code", Map.of(
                "coding", java.util.List.of(Map.of("code", "4548-4", "display", "Hemoglobin A1c"))
            ),
            "valueQuantity", Map.of("value", 7.0, "unit", "%")
        );

        ChronicDiseaseMonitoringEntity updatedMonitoring = ChronicDiseaseMonitoringEntity.builder()
            .id(previousMonitoring.getId())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .diseaseCode("44054006")
            .diseaseName("Type 2 Diabetes Mellitus")
            .latestValue(7.0)
            .previousValue(9.0)
            .trend(ChronicDiseaseMonitoringEntity.Trend.IMPROVING)
            .alertTriggered(false)
            .monitoredAt(Instant.now())
            .build();

        when(monitoringRepository.save(any(ChronicDiseaseMonitoringEntity.class)))
            .thenReturn(updatedMonitoring);

        // When: Improved lab result is processed
        ChronicDiseaseMonitoringEntity result = monitoringService.processLabResult(
            TENANT_ID, PATIENT_ID, observationData);

        // Then: Trend should be IMPROVING
        assertThat(result.getTrend()).isEqualTo(ChronicDiseaseMonitoringEntity.Trend.IMPROVING);
        assertThat(result.isAlertTriggered()).isFalse();

        // Verify no deterioration event published
        verify(kafkaTemplate, never()).send(eq("chronic-disease.deterioration"), any());
    }

    @Test
    void testThresholdBasedAlerts_HbA1c() {
        // Given: Different HbA1c thresholds
        // >9% = deteriorating, increase >1% = alert

        // Test case 1: HbA1c = 9.5% (above threshold)
        when(diseaseDeteriorationDetector.shouldTriggerAlert(eq("HbA1c"), eq(9.5), isNull()))
            .thenReturn(true);

        when(diseaseDeteriorationDetector.analyzeTrend(eq("HbA1c"), isNull(), eq(9.5)))
            .thenReturn(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING);

        when(monitoringRepository.findByTenantIdAndPatientIdAndDiseaseCode(TENANT_ID, PATIENT_ID, "44054006"))
            .thenReturn(Optional.empty());

        when(diseaseDeteriorationDetector.getDeteriorationSeverity(eq("HbA1c"), eq(9.5)))
            .thenReturn("HIGH");

        Map<String, Object> highHbA1c = Map.of(
            "resourceType", "Observation",
            "code", Map.of("coding", java.util.List.of(Map.of("code", "4548-4", "display", "Hemoglobin A1c"))),
            "valueQuantity", Map.of("value", 9.5, "unit", "%")
        );

        ChronicDiseaseMonitoringEntity highResult = ChronicDiseaseMonitoringEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .diseaseCode("44054006")
            .diseaseName("Type 2 Diabetes Mellitus")
            .latestValue(9.5)
            .trend(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING)
            .alertTriggered(true)
            .monitoredAt(Instant.now())
            .build();

        when(monitoringRepository.save(any(ChronicDiseaseMonitoringEntity.class)))
            .thenReturn(highResult);

        // When: Processing high HbA1c
        ChronicDiseaseMonitoringEntity result = monitoringService.processLabResult(
            TENANT_ID, PATIENT_ID, highHbA1c);

        // Then: Alert should trigger
        assertThat(result.isAlertTriggered()).isTrue();
        verify(kafkaTemplate).send(eq("chronic-disease.deterioration"), any());
    }

    @Test
    void testThresholdBasedAlerts_BloodPressure() {
        // Given: BP thresholds - Systolic >140 = deteriorating, >160 = alert
        when(diseaseDeteriorationDetector.shouldTriggerAlert(eq("BP_SYSTOLIC"), eq(165.0), isNull()))
            .thenReturn(true);

        when(diseaseDeteriorationDetector.analyzeTrend(eq("BP_SYSTOLIC"), isNull(), eq(165.0)))
            .thenReturn(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING);

        when(monitoringRepository.findByTenantIdAndPatientIdAndDiseaseCode(TENANT_ID, PATIENT_ID, "38341003"))
            .thenReturn(Optional.empty());

        when(diseaseDeteriorationDetector.getDeteriorationSeverity(eq("BP_SYSTOLIC"), eq(165.0)))
            .thenReturn("HIGH");

        Map<String, Object> highBP = Map.of(
            "resourceType", "Observation",
            "code", Map.of("coding", java.util.List.of(Map.of("code", "8480-6", "display", "Systolic blood pressure"))),
            "valueQuantity", Map.of("value", 165, "unit", "mmHg")
        );

        ChronicDiseaseMonitoringEntity bpResult = ChronicDiseaseMonitoringEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .diseaseCode("38341003")
            .diseaseName("Hypertensive disorder")
            .latestValue(165.0)
            .trend(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING)
            .alertTriggered(true)
            .monitoredAt(Instant.now())
            .build();

        when(monitoringRepository.save(any(ChronicDiseaseMonitoringEntity.class)))
            .thenReturn(bpResult);

        // When: Processing high BP
        ChronicDiseaseMonitoringEntity result = monitoringService.processLabResult(
            TENANT_ID, PATIENT_ID, highBP);

        // Then: Alert should trigger for critical BP
        assertThat(result.isAlertTriggered()).isTrue();
        verify(kafkaTemplate).send(eq("chronic-disease.deterioration"), any());
    }

    @Test
    void testLDLCholesterolMonitoring() {
        // Given: LDL Cholesterol >190 = deteriorating
        when(diseaseDeteriorationDetector.shouldTriggerAlert(eq("LDL"), eq(195.0), isNull()))
            .thenReturn(true);

        when(diseaseDeteriorationDetector.analyzeTrend(eq("LDL"), isNull(), eq(195.0)))
            .thenReturn(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING);

        when(monitoringRepository.findByTenantIdAndPatientIdAndDiseaseCode(TENANT_ID, PATIENT_ID, "13644009"))
            .thenReturn(Optional.empty());

        when(diseaseDeteriorationDetector.getDeteriorationSeverity(eq("LDL"), eq(195.0)))
            .thenReturn("HIGH");

        Map<String, Object> highLDL = Map.of(
            "resourceType", "Observation",
            "code", Map.of(
                "coding", java.util.List.of(Map.of(
                    "system", "http://loinc.org",
                    "code", "18262-6",
                    "display", "LDL Cholesterol"
                ))
            ),
            "valueQuantity", Map.of("value", 195, "unit", "mg/dL")
        );

        ChronicDiseaseMonitoringEntity ldlResult = ChronicDiseaseMonitoringEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .diseaseCode("13644009")
            .diseaseName("Hyperlipidemia")
            .latestValue(195.0)
            .trend(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING)
            .alertTriggered(true)
            .monitoredAt(Instant.now())
            .build();

        when(monitoringRepository.save(any(ChronicDiseaseMonitoringEntity.class)))
            .thenReturn(ldlResult);

        // When: Processing high LDL
        ChronicDiseaseMonitoringEntity result = monitoringService.processLabResult(
            TENANT_ID, PATIENT_ID, highLDL);

        // Then: Should detect deterioration
        assertThat(result.getTrend()).isEqualTo(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING);
        assertThat(result.isAlertTriggered()).isTrue();
    }
}
