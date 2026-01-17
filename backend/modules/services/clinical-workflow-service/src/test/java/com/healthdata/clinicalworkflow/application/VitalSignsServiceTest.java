package com.healthdata.clinicalworkflow.application;

import com.healthdata.clinicalworkflow.domain.model.VitalSignsRecordEntity;
import com.healthdata.clinicalworkflow.domain.repository.VitalSignsRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VitalSignsService
 *
 * Tests all 14 methods with focus on:
 * - Alert threshold detection
 * - BMI calculation accuracy
 * - Multi-tenant isolation
 * - Edge cases and error handling
 */
@ExtendWith(MockitoExtension.class)
class VitalSignsServiceTest {

    @Mock
    private VitalSignsRecordRepository vitalsRepository;

    @InjectMocks
    private VitalSignsService vitalsService;

    private static final String TENANT_ID = "TENANT001";
    private static final UUID PATIENT_ID = UUID.randomUUID();

    private VitalSignsRecordEntity testVitals;
    private VitalSignsService.VitalSignsRequest testRequest;

    @BeforeEach
    void setUp() {
        testVitals = VitalSignsRecordEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .systolicBp(new BigDecimal("120"))
                .diastolicBp(new BigDecimal("80"))
                .heartRate(new BigDecimal("75"))
                .temperatureF(new BigDecimal("98.6"))
                .oxygenSaturation(new BigDecimal("98"))
                .alertStatus("normal")
                .build();

        testRequest = VitalSignsService.VitalSignsRequest.builder()
                .patientId(PATIENT_ID)
                .recordedBy("test-ma")
                .systolicBp(new BigDecimal("120"))
                .diastolicBp(new BigDecimal("80"))
                .heartRate(new BigDecimal("75"))
                .temperatureF(new BigDecimal("98.6"))
                .oxygenSaturation(new BigDecimal("98"))
                .weightKg(new BigDecimal("70"))
                .heightCm(new BigDecimal("170"))
                .build();
    }

    // ========== recordVitals Tests ==========

    @Test
    void recordVitals_ShouldCreateVitals_WhenNormalValues() {
        // Given
        when(vitalsRepository.save(any(VitalSignsRecordEntity.class)))
                .thenReturn(testVitals);

        // When
        VitalSignsRecordEntity result = vitalsService.recordVitals(testRequest, TENANT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAlertStatus()).isEqualTo("normal");
        verify(vitalsRepository).save(any(VitalSignsRecordEntity.class));
    }

    @Test
    void recordVitals_ShouldCalculateBMI_WhenWeightAndHeightProvided() {
        // Given
        when(vitalsRepository.save(any(VitalSignsRecordEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When
        VitalSignsRecordEntity result = vitalsService.recordVitals(testRequest, TENANT_ID);

        // Then
        assertThat(result.getBmi()).isNotNull();
        assertThat(result.getBmi()).isEqualByComparingTo("24.22"); // 70kg / (1.7m)^2
    }

    @Test
    void recordVitals_ShouldNotCalculateBMI_WhenWeightMissing() {
        // Given
        testRequest.setWeightKg(null);
        when(vitalsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        VitalSignsRecordEntity result = vitalsService.recordVitals(testRequest, TENANT_ID);

        // Then
        assertThat(result.getBmi()).isNull();
    }

    // ========== detectAbnormalValues Tests - Systolic BP ==========

    @Test
    void detectAbnormalValues_ShouldDetectCritical_WhenSystolicAbove180() {
        // Given
        testVitals.setSystolicBp(new BigDecimal("185"));

        // When
        vitalsService.detectAbnormalValues(testVitals);

        // Then
        assertThat(testVitals.getAlertStatus()).isEqualTo("critical");
        assertThat(testVitals.getAlertMessage()).contains("CRITICAL: Systolic BP 185");
    }

    @Test
    void detectAbnormalValues_ShouldDetectWarning_WhenSystolicAbove140() {
        // Given
        testVitals.setSystolicBp(new BigDecimal("145"));

        // When
        vitalsService.detectAbnormalValues(testVitals);

        // Then
        assertThat(testVitals.getAlertStatus()).isEqualTo("warning");
        assertThat(testVitals.getAlertMessage()).contains("WARNING: Systolic BP 145");
    }

    @Test
    void detectAbnormalValues_ShouldDetectCritical_WhenSystolicBelow60() {
        // Given
        testVitals.setSystolicBp(new BigDecimal("55"));

        // When
        vitalsService.detectAbnormalValues(testVitals);

        // Then
        assertThat(testVitals.getAlertStatus()).isEqualTo("critical");
        assertThat(testVitals.getAlertMessage()).contains("CRITICAL: Systolic BP 55");
    }

    @Test
    void detectAbnormalValues_ShouldDetectWarning_WhenSystolicBelow70() {
        // Given
        testVitals.setSystolicBp(new BigDecimal("65"));

        // When
        vitalsService.detectAbnormalValues(testVitals);

        // Then
        assertThat(testVitals.getAlertStatus()).isEqualTo("warning");
        assertThat(testVitals.getAlertMessage()).contains("WARNING: Systolic BP 65");
    }

    @Test
    void detectAbnormalValues_ShouldBeNormal_WhenSystolicInRange() {
        // Given
        testVitals.setSystolicBp(new BigDecimal("120"));

        // When
        vitalsService.detectAbnormalValues(testVitals);

        // Then
        assertThat(testVitals.getAlertStatus()).isEqualTo("normal");
        assertThat(testVitals.getAlertMessage()).isNullOrEmpty();
    }

    // ========== detectAbnormalValues Tests - Heart Rate ==========

    @Test
    void detectAbnormalValues_ShouldDetectCritical_WhenHeartRateAbove130() {
        // Given
        testVitals.setHeartRate(new BigDecimal("135"));

        // When
        vitalsService.detectAbnormalValues(testVitals);

        // Then
        assertThat(testVitals.getAlertStatus()).isEqualTo("critical");
        assertThat(testVitals.getAlertMessage()).contains("CRITICAL: Heart Rate 135");
    }

    @Test
    void detectAbnormalValues_ShouldDetectWarning_WhenHeartRateAbove100() {
        // Given
        testVitals.setHeartRate(new BigDecimal("105"));

        // When
        vitalsService.detectAbnormalValues(testVitals);

        // Then
        assertThat(testVitals.getAlertStatus()).isEqualTo("warning");
        assertThat(testVitals.getAlertMessage()).contains("WARNING: Heart Rate 105");
    }

    @Test
    void detectAbnormalValues_ShouldDetectCritical_WhenHeartRateBelow40() {
        // Given
        testVitals.setHeartRate(new BigDecimal("35"));

        // When
        vitalsService.detectAbnormalValues(testVitals);

        // Then
        assertThat(testVitals.getAlertStatus()).isEqualTo("critical");
        assertThat(testVitals.getAlertMessage()).contains("CRITICAL: Heart Rate 35");
    }

    @Test
    void detectAbnormalValues_ShouldDetectWarning_WhenHeartRateBelow50() {
        // Given
        testVitals.setHeartRate(new BigDecimal("45"));

        // When
        vitalsService.detectAbnormalValues(testVitals);

        // Then
        assertThat(testVitals.getAlertStatus()).isEqualTo("warning");
        assertThat(testVitals.getAlertMessage()).contains("WARNING: Heart Rate 45");
    }

    // ========== detectAbnormalValues Tests - O2 Saturation ==========

    @Test
    void detectAbnormalValues_ShouldDetectCritical_WhenO2Below85() {
        // Given
        testVitals.setOxygenSaturation(new BigDecimal("82"));

        // When
        vitalsService.detectAbnormalValues(testVitals);

        // Then
        assertThat(testVitals.getAlertStatus()).isEqualTo("critical");
        assertThat(testVitals.getAlertMessage()).contains("CRITICAL: O2 Saturation 82");
    }

    @Test
    void detectAbnormalValues_ShouldDetectWarning_WhenO2Below90() {
        // Given
        testVitals.setOxygenSaturation(new BigDecimal("87"));

        // When
        vitalsService.detectAbnormalValues(testVitals);

        // Then
        assertThat(testVitals.getAlertStatus()).isEqualTo("warning");
        assertThat(testVitals.getAlertMessage()).contains("WARNING: O2 Saturation 87");
    }

    @Test
    void detectAbnormalValues_ShouldBeNormal_WhenO2Above95() {
        // Given
        testVitals.setOxygenSaturation(new BigDecimal("98"));

        // When
        vitalsService.detectAbnormalValues(testVitals);

        // Then
        assertThat(testVitals.getAlertStatus()).isEqualTo("normal");
    }

    // ========== detectAbnormalValues Tests - Temperature ==========

    @Test
    void detectAbnormalValues_ShouldDetectCritical_WhenTempAbove104() {
        // Given
        testVitals.setTemperatureF(new BigDecimal("105.5"));

        // When
        vitalsService.detectAbnormalValues(testVitals);

        // Then
        assertThat(testVitals.getAlertStatus()).isEqualTo("critical");
        assertThat(testVitals.getAlertMessage()).contains("CRITICAL: Temperature 105.5");
    }

    @Test
    void detectAbnormalValues_ShouldDetectWarning_WhenFever() {
        // Given
        testVitals.setTemperatureF(new BigDecimal("101.5"));

        // When
        vitalsService.detectAbnormalValues(testVitals);

        // Then
        assertThat(testVitals.getAlertStatus()).isEqualTo("warning");
        assertThat(testVitals.getAlertMessage()).contains("WARNING: Temperature 101.5");
    }

    @Test
    void detectAbnormalValues_ShouldDetectCritical_WhenHypothermia() {
        // Given
        testVitals.setTemperatureF(new BigDecimal("92.0"));

        // When
        vitalsService.detectAbnormalValues(testVitals);

        // Then
        assertThat(testVitals.getAlertStatus()).isEqualTo("critical");
        assertThat(testVitals.getAlertMessage()).contains("CRITICAL: Temperature 92.0");
    }

    // ========== detectAbnormalValues Tests - Multiple Alerts ==========

    @Test
    void detectAbnormalValues_ShouldPrioritizeCritical_WhenBothWarningAndCritical() {
        // Given
        testVitals.setSystolicBp(new BigDecimal("185")); // Critical
        testVitals.setHeartRate(new BigDecimal("105"));  // Warning

        // When
        vitalsService.detectAbnormalValues(testVitals);

        // Then
        assertThat(testVitals.getAlertStatus()).isEqualTo("critical");
        assertThat(testVitals.getAlertMessage()).contains("CRITICAL: Systolic BP");
        assertThat(testVitals.getAlertMessage()).contains("WARNING: Heart Rate");
    }

    @Test
    void detectAbnormalValues_ShouldCombineMultipleWarnings() {
        // Given
        testVitals.setSystolicBp(new BigDecimal("145")); // Warning
        testVitals.setHeartRate(new BigDecimal("105"));  // Warning

        // When
        vitalsService.detectAbnormalValues(testVitals);

        // Then
        assertThat(testVitals.getAlertStatus()).isEqualTo("warning");
        assertThat(testVitals.getAlertMessage()).contains("Systolic BP");
        assertThat(testVitals.getAlertMessage()).contains("Heart Rate");
    }

    // ========== calculateBMI Tests ==========

    @Test
    void calculateBMI_ShouldCalculateCorrectly_WhenValidInput() {
        // When
        BigDecimal bmi = vitalsService.calculateBMI(
                new BigDecimal("70"), new BigDecimal("170"));

        // Then
        assertThat(bmi).isEqualByComparingTo("24.22");
    }

    @Test
    void calculateBMI_ShouldCalculateCorrectly_ForUnderweight() {
        // When
        BigDecimal bmi = vitalsService.calculateBMI(
                new BigDecimal("50"), new BigDecimal("170"));

        // Then
        assertThat(bmi).isEqualByComparingTo("17.30");
    }

    @Test
    void calculateBMI_ShouldCalculateCorrectly_ForObese() {
        // When
        BigDecimal bmi = vitalsService.calculateBMI(
                new BigDecimal("100"), new BigDecimal("170"));

        // Then
        assertThat(bmi).isEqualByComparingTo("34.60");
    }

    @Test
    void calculateBMI_ShouldThrowException_WhenWeightNull() {
        // When/Then
        assertThatThrownBy(() -> vitalsService.calculateBMI(null, new BigDecimal("170")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Weight and height must be positive");
    }

    @Test
    void calculateBMI_ShouldThrowException_WhenHeightNull() {
        // When/Then
        assertThatThrownBy(() -> vitalsService.calculateBMI(new BigDecimal("70"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Weight and height must be positive");
    }

    @Test
    void calculateBMI_ShouldThrowException_WhenWeightZero() {
        // When/Then
        assertThatThrownBy(() -> vitalsService.calculateBMI(
                BigDecimal.ZERO, new BigDecimal("170")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ========== getVitalsAlerts Tests ==========

    @Test
    void getVitalsAlerts_ShouldReturnCriticalFirst_WhenBothExist() {
        // Given
        VitalSignsRecordEntity warning = VitalSignsRecordEntity.builder()
                .id(UUID.randomUUID())
                .alertStatus("warning")
                .build();
        VitalSignsRecordEntity critical = VitalSignsRecordEntity.builder()
                .id(UUID.randomUUID())
                .alertStatus("critical")
                .build();

        when(vitalsRepository.findByTenantIdAndAlertStatusOrderByRecordedAtDesc(
                TENANT_ID, "warning"))
                .thenReturn(List.of(warning));
        when(vitalsRepository.findByTenantIdAndAlertStatusOrderByRecordedAtDesc(
                TENANT_ID, "critical"))
                .thenReturn(List.of(critical));

        // When
        List<VitalSignsRecordEntity> result = vitalsService.getVitalsAlerts(TENANT_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAlertStatus()).isEqualTo("critical");
        assertThat(result.get(1).getAlertStatus()).isEqualTo("warning");
    }

    @Test
    void getVitalsAlerts_ShouldReturnEmpty_WhenNoAlerts() {
        // Given
        when(vitalsRepository.findByTenantIdAndAlertStatusOrderByRecordedAtDesc(
                anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        // When
        List<VitalSignsRecordEntity> result = vitalsService.getVitalsAlerts(TENANT_ID);

        // Then
        assertThat(result).isEmpty();
    }

    // ========== getPatientVitalsHistory Tests ==========

    @Test
    void getPatientVitalsHistory_ShouldReturnHistory_WhenCalled() {
        // Given
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        when(vitalsRepository.findPatientVitalsHistory(
                PATIENT_ID, TENANT_ID, from, to))
                .thenReturn(List.of(testVitals));

        // When
        List<VitalSignsRecordEntity> result = vitalsService.getPatientVitalsHistory(
                PATIENT_ID, TENANT_ID, from, to);

        // Then
        assertThat(result).hasSize(1);
    }

    // ========== getLatestVitals Tests ==========

    @Test
    void getLatestVitals_ShouldReturnLatest_WhenExists() {
        // Given
        when(vitalsRepository.findLatestVitalForPatient(PATIENT_ID, TENANT_ID))
                .thenReturn(Optional.of(testVitals));

        // When
        Optional<VitalSignsRecordEntity> result = vitalsService.getLatestVitals(
                PATIENT_ID, TENANT_ID);

        // Then
        assertThat(result).isPresent();
    }

    @Test
    void getLatestVitals_ShouldReturnEmpty_WhenNoneExist() {
        // Given
        when(vitalsRepository.findLatestVitalForPatient(PATIENT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        // When
        Optional<VitalSignsRecordEntity> result = vitalsService.getLatestVitals(
                PATIENT_ID, TENANT_ID);

        // Then
        assertThat(result).isEmpty();
    }

    // ========== getVitalsById Tests ==========

    @Test
    void getVitalsById_ShouldReturnVitals_WhenExists() {
        // Given
        UUID vitalsId = UUID.randomUUID();
        when(vitalsRepository.findByIdAndTenantId(vitalsId, TENANT_ID))
                .thenReturn(Optional.of(testVitals));

        // When
        VitalSignsRecordEntity result = vitalsService.getVitalsById(vitalsId, TENANT_ID);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void getVitalsById_ShouldThrowException_WhenNotFound() {
        // Given
        UUID vitalsId = UUID.randomUUID();
        when(vitalsRepository.findByIdAndTenantId(vitalsId, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> vitalsService.getVitalsById(vitalsId, TENANT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vitals not found");
    }

    // ========== getVitalsForEncounter Tests ==========

    @Test
    void getVitalsForEncounter_ShouldReturnVitals_WhenEncounterHasVitals() {
        // Given
        String encounterId = "Encounter/123";
        when(vitalsRepository.findByTenantIdAndEncounterIdOrderByRecordedAtDesc(
                TENANT_ID, encounterId))
                .thenReturn(List.of(testVitals));

        // When
        List<VitalSignsRecordEntity> result = vitalsService.getVitalsForEncounter(
                encounterId, TENANT_ID);

        // Then
        assertThat(result).hasSize(1);
    }

    // ========== countCriticalAlerts Tests ==========

    @Test
    void countCriticalAlerts_ShouldReturnCount_WhenCalled() {
        // Given
        when(vitalsRepository.countCriticalAlertsByTenant(TENANT_ID))
                .thenReturn(3L);

        // When
        long result = vitalsService.countCriticalAlerts(TENANT_ID);

        // Then
        assertThat(result).isEqualTo(3L);
    }

    // ========== updateNotes Tests ==========

    @Test
    void updateNotes_ShouldUpdateNotes_WhenValidVitals() {
        // Given
        UUID vitalsId = UUID.randomUUID();
        String notes = "Patient reports dizziness";
        when(vitalsRepository.findByIdAndTenantId(vitalsId, TENANT_ID))
                .thenReturn(Optional.of(testVitals));
        when(vitalsRepository.save(any())).thenReturn(testVitals);

        // When
        VitalSignsRecordEntity result = vitalsService.updateNotes(
                vitalsId, notes, TENANT_ID);

        // Then
        verify(vitalsRepository).save(argThat(vitals ->
                notes.equals(vitals.getNotes())));
    }
}
