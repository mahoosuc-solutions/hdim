package com.healthdata.clinicalworkflow.application;

import com.healthdata.clinicalworkflow.api.v1.dto.*;
import com.healthdata.clinicalworkflow.domain.model.VitalSignsRecordEntity;
import com.healthdata.clinicalworkflow.domain.repository.VitalSignsRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    @Mock
    private com.healthdata.clinicalworkflow.domain.repository.RoomAssignmentRepository roomAssignmentRepository;

    @Mock
    private com.healthdata.clinicalworkflow.client.PatientServiceClient patientServiceClient;

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

    // ========== NEW CONTROLLER METHODS TESTS (Tier 1 Fixes) ==========

    // ========== 2a. recordVitalSigns adapter Tests ==========

    @Test
    void recordVitalSigns_ShouldConvertAndReturnDTO_WhenValidRequest() {
        // Given
        com.healthdata.clinicalworkflow.api.v1.dto.VitalSignsRequest dtoRequest =
                com.healthdata.clinicalworkflow.api.v1.dto.VitalSignsRequest.builder()
                        .patientId("123e4567-e89b-12d3-a456-426614174000")
                        .encounterId("ENC001")
                        .measuredAt(LocalDateTime.now())
                        .systolicBP(120)
                        .diastolicBP(80)
                        .heartRate(72)
                        .respiratoryRate(16)
                        .temperature(new BigDecimal("98.6"))
                        .oxygenSaturation(98)
                        .weight(new BigDecimal("175.5")) // pounds
                        .height(new BigDecimal("68")) // inches
                        .notes("Test vitals")
                        .build();

        testVitals.setCreatedAt(Instant.now());
        testVitals.setRecordedAt(Instant.now());
        when(vitalsRepository.save(any(VitalSignsRecordEntity.class)))
                .thenReturn(testVitals);

        // When
        VitalSignsResponse response = vitalsService.recordVitalSigns(
                TENANT_ID, dtoRequest, "test-user");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testVitals.getId());
        assertThat(response.getPatientId()).isEqualTo(testVitals.getPatientId().toString());
        verify(vitalsRepository).save(any(VitalSignsRecordEntity.class));
    }

    @Test
    void recordVitalSigns_ShouldConvertUnits_WhenPoundsAndInchesProvided() {
        // Given
        com.healthdata.clinicalworkflow.api.v1.dto.VitalSignsRequest dtoRequest =
                com.healthdata.clinicalworkflow.api.v1.dto.VitalSignsRequest.builder()
                        .patientId(PATIENT_ID.toString())
                        .encounterId("ENC001")
                        .measuredAt(LocalDateTime.now())
                        .systolicBP(120)
                        .weight(new BigDecimal("154.32")) // 70kg in pounds
                        .height(new BigDecimal("66.93")) // 170cm in inches
                        .build();

        when(vitalsRepository.save(any(VitalSignsRecordEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When
        VitalSignsResponse response = vitalsService.recordVitalSigns(
                TENANT_ID, dtoRequest, "test-user");

        // Then - Should convert to kg and cm
        verify(vitalsRepository).save(argThat(vitals ->
                vitals.getWeightKg() != null &&
                        vitals.getWeightKg().compareTo(new BigDecimal("69.85")) >= 0 &&
                        vitals.getWeightKg().compareTo(new BigDecimal("70.15")) <= 0 &&
                        vitals.getHeightCm() != null &&
                        vitals.getHeightCm().compareTo(new BigDecimal("169.5")) >= 0 &&
                        vitals.getHeightCm().compareTo(new BigDecimal("170.5")) <= 0
        ));
    }

    // ========== 2b. getVitalSigns Tests ==========

    @Test
    void getVitalSigns_ShouldReturnDTO_WhenVitalsExist() {
        // Given
        UUID vitalsId = UUID.randomUUID();
        testVitals.setId(vitalsId);
        testVitals.setRecordedAt(Instant.now());
        testVitals.setCreatedAt(Instant.now());
        when(vitalsRepository.findByIdAndTenantId(vitalsId, TENANT_ID))
                .thenReturn(Optional.of(testVitals));

        // When
        VitalSignsResponse response = vitalsService.getVitalSigns(TENANT_ID, vitalsId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(vitalsId);
        assertThat(response.getTenantId()).isEqualTo(TENANT_ID);
    }

    // ========== 2c. getVitalsHistory with pagination Tests ==========

    @Test
    void getVitalsHistory_ShouldReturnPaginatedDTO_WhenCalled() {
        // Given
        String patientIdStr = PATIENT_ID.toString();
        testVitals.setRecordedAt(Instant.now());
        testVitals.setCreatedAt(Instant.now());
        when(vitalsRepository.findByTenantIdAndPatientIdOrderByRecordedAtDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(List.of(testVitals));

        Pageable pageable = Pageable.ofSize(20);

        // When
        VitalsHistoryResponse response = vitalsService.getVitalsHistory(
                TENANT_ID, patientIdStr, pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getVitals()).hasSize(1);
        assertThat(response.getTotalRecords()).isEqualTo(1L);
    }

    // ========== 2d. getVitalAlerts with includeAcknowledged Tests ==========

    @Test
    void getVitalAlerts_ShouldReturnAlertDTOs_WhenAlertsExist() {
        // Given
        VitalSignsRecordEntity criticalVital = VitalSignsRecordEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .systolicBp(new BigDecimal("185"))
                .alertStatus("critical")
                .alertMessage("CRITICAL: Systolic BP 185 mmHg")
                .recordedAt(Instant.now())
                .createdAt(Instant.now())
                .build();

        when(vitalsRepository.findByTenantIdAndAlertStatusOrderByRecordedAtDesc(
                TENANT_ID, "warning"))
                .thenReturn(Collections.emptyList());
        when(vitalsRepository.findByTenantIdAndAlertStatusOrderByRecordedAtDesc(
                TENANT_ID, "critical"))
                .thenReturn(List.of(criticalVital));

        // When
        List<VitalAlertResponse> alerts = vitalsService.getVitalAlerts(
                TENANT_ID, false);

        // Then
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getSeverity()).isEqualToIgnoringCase("critical");
    }

    // ========== 2e. getLatestVitals (String patientId) Tests ==========

    @Test
    void getLatestVitals_ShouldReturnDTO_WhenLatestVitalsExist() {
        // Given
        String patientIdStr = PATIENT_ID.toString();
        testVitals.setRecordedAt(Instant.now());
        testVitals.setCreatedAt(Instant.now());
        when(vitalsRepository.findLatestVitalForPatient(PATIENT_ID, TENANT_ID))
                .thenReturn(Optional.of(testVitals));

        // When
        VitalSignsResponse response = vitalsService.getLatestVitals(
                TENANT_ID, patientIdStr);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPatientId()).isEqualTo(PATIENT_ID.toString());
    }

    @Test
    void getLatestVitals_ShouldThrowException_WhenNoVitalsExist() {
        // Given
        String patientIdStr = PATIENT_ID.toString();
        when(vitalsRepository.findLatestVitalForPatient(PATIENT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> vitalsService.getLatestVitals(TENANT_ID, patientIdStr))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vital signs not found");
    }

    // ========== 2f. getCriticalAlerts Tests ==========

    @Test
    void getCriticalAlerts_ShouldReturnCriticalAlertDTOs_WhenCriticalAlertsExist() {
        // Given
        VitalSignsRecordEntity criticalVital = VitalSignsRecordEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .heartRate(new BigDecimal("135"))
                .alertStatus("critical")
                .alertMessage("CRITICAL: Heart Rate 135 bpm")
                .recordedAt(Instant.now())
                .createdAt(Instant.now())
                .build();

        when(vitalsRepository.findByTenantIdAndAlertStatusOrderByRecordedAtDesc(
                TENANT_ID, "critical"))
                .thenReturn(List.of(criticalVital));

        // When
        List<VitalAlertResponse> alerts = vitalsService.getCriticalAlerts(TENANT_ID);

        // Then
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getSeverity()).isEqualToIgnoringCase("critical");
    }

    // ========== 2g. acknowledgeAlert Tests ==========

    @Test
    void acknowledgeAlert_ShouldMarkAlertAsAcknowledged_WhenValidRequest() {
        // Given
        UUID vitalsId = UUID.randomUUID();
        testVitals.setId(vitalsId);
        testVitals.setAlertStatus("critical");
        testVitals.setAlertMessage("CRITICAL: Systolic BP 185 mmHg");
        testVitals.setRecordedAt(Instant.now());
        testVitals.setCreatedAt(Instant.now());

        when(vitalsRepository.findByIdAndTenantId(vitalsId, TENANT_ID))
                .thenReturn(Optional.of(testVitals));
        when(vitalsRepository.save(any(VitalSignsRecordEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When
        VitalAlertResponse response = vitalsService.acknowledgeAlert(
                TENANT_ID, vitalsId, "test-user");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAcknowledgedBy()).isEqualTo("test-user");
        assertThat(response.getAcknowledged()).isTrue();
        verify(vitalsRepository).save(argThat(vitals ->
                "test-user".equals(vitals.getAcknowledgedBy()) &&
                        vitals.getAcknowledgedAt() != null
        ));
    }

    // ========== Room Number Resolution Tests (Issue #301) ==========

    @Test
    void getActiveAlerts_ShouldIncludeRoomNumber_WhenPatientHasActiveRoom() {
        // Given: Patient with critical BP in room EXAM-101
        VitalSignsRecordEntity criticalVitals = VitalSignsRecordEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .systolicBp(new BigDecimal("190"))
                .diastolicBp(new BigDecimal("120"))
                .heartRate(new BigDecimal("75"))
                .temperatureF(new BigDecimal("98.6"))
                .oxygenSaturation(new BigDecimal("98"))
                .alertStatus("critical")
                .alertMessage("Critical: Systolic BP 190 mmHg")
                .recordedAt(Instant.now())
                .build();

        com.healthdata.clinicalworkflow.domain.model.RoomAssignmentEntity roomAssignment =
                com.healthdata.clinicalworkflow.domain.model.RoomAssignmentEntity.builder()
                        .tenantId(TENANT_ID)
                        .patientId(PATIENT_ID)
                        .roomNumber("EXAM-101")
                        .status("occupied")
                        .build();

        when(vitalsRepository.findActiveAlertsByTenant(TENANT_ID))
                .thenReturn(Collections.singletonList(criticalVitals));
        when(roomAssignmentRepository.findActiveRoomForPatient(TENANT_ID, PATIENT_ID))
                .thenReturn(Optional.of(roomAssignment));

        // When
        List<VitalAlertResponse> alerts = vitalsService.getActiveAlerts(TENANT_ID);

        // Then
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getRoomNumber()).isEqualTo("EXAM-101");
        assertThat(alerts.get(0).getPatientId()).isEqualTo(PATIENT_ID.toString());
        assertThat(alerts.get(0).getSeverity()).isEqualTo("CRITICAL");
        verify(roomAssignmentRepository).findActiveRoomForPatient(TENANT_ID, PATIENT_ID);
    }

    @Test
    void getActiveAlerts_ShouldReturnNullRoomNumber_WhenPatientNotInRoom() {
        // Given: Patient with alert but no active room
        VitalSignsRecordEntity criticalVitals = VitalSignsRecordEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .systolicBp(new BigDecimal("190"))
                .diastolicBp(new BigDecimal("120"))
                .heartRate(new BigDecimal("75"))
                .alertStatus("critical")
                .alertMessage("Critical: Systolic BP 190 mmHg")
                .recordedAt(Instant.now())
                .build();

        when(vitalsRepository.findActiveAlertsByTenant(TENANT_ID))
                .thenReturn(Collections.singletonList(criticalVitals));
        when(roomAssignmentRepository.findActiveRoomForPatient(TENANT_ID, PATIENT_ID))
                .thenReturn(Optional.empty());

        // When
        List<VitalAlertResponse> alerts = vitalsService.getActiveAlerts(TENANT_ID);

        // Then
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getRoomNumber()).isNull();
        assertThat(alerts.get(0).getPatientId()).isEqualTo(PATIENT_ID.toString());
        verify(roomAssignmentRepository).findActiveRoomForPatient(TENANT_ID, PATIENT_ID);
    }

    @Test
    void getActiveAlerts_ShouldHandleRoomLookupFailure_Gracefully() {
        // Given: Patient with alert, room lookup throws exception
        VitalSignsRecordEntity criticalVitals = VitalSignsRecordEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .systolicBp(new BigDecimal("190"))
                .diastolicBp(new BigDecimal("120"))
                .heartRate(new BigDecimal("75"))
                .alertStatus("critical")
                .alertMessage("Critical: Systolic BP 190 mmHg")
                .recordedAt(Instant.now())
                .build();

        when(vitalsRepository.findActiveAlertsByTenant(TENANT_ID))
                .thenReturn(Collections.singletonList(criticalVitals));
        when(roomAssignmentRepository.findActiveRoomForPatient(TENANT_ID, PATIENT_ID))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When
        List<VitalAlertResponse> alerts = vitalsService.getActiveAlerts(TENANT_ID);

        // Then: Alert should still be delivered with null room number
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getRoomNumber()).isNull();
        assertThat(alerts.get(0).getPatientId()).isEqualTo(PATIENT_ID.toString());
        assertThat(alerts.get(0).getSeverity()).isEqualTo("CRITICAL");
    }

    @Test
    void getCriticalAlerts_ShouldIncludeRoomNumber_WhenPatientHasActiveRoom() {
        // Given: Patient with critical alert in room EXAM-202
        VitalSignsRecordEntity criticalVitals = VitalSignsRecordEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .systolicBp(new BigDecimal("190"))
                .diastolicBp(new BigDecimal("120"))
                .heartRate(new BigDecimal("75"))
                .alertStatus("critical")
                .alertMessage("Critical: Systolic BP 190 mmHg")
                .recordedAt(Instant.now())
                .build();

        com.healthdata.clinicalworkflow.domain.model.RoomAssignmentEntity roomAssignment =
                com.healthdata.clinicalworkflow.domain.model.RoomAssignmentEntity.builder()
                        .tenantId(TENANT_ID)
                        .patientId(PATIENT_ID)
                        .roomNumber("EXAM-202")
                        .status("occupied")
                        .build();

        when(vitalsRepository.findCriticalAlertsByTenant(TENANT_ID))
                .thenReturn(Collections.singletonList(criticalVitals));
        when(roomAssignmentRepository.findActiveRoomForPatient(TENANT_ID, PATIENT_ID))
                .thenReturn(Optional.of(roomAssignment));

        // When
        List<VitalAlertResponse> alerts = vitalsService.getCriticalAlerts(TENANT_ID);

        // Then
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getRoomNumber()).isEqualTo("EXAM-202");
        assertThat(alerts.get(0).getSeverity()).isEqualTo("CRITICAL");
        verify(roomAssignmentRepository).findActiveRoomForPatient(TENANT_ID, PATIENT_ID);
    }

    // ========== Patient Name Resolution Tests ==========

    @Test
    void getVitalById_ShouldIncludePatientName_WhenPatientFound() {
        // Given
        com.healthdata.clinicalworkflow.client.dto.PatientDTO patient =
                com.healthdata.clinicalworkflow.client.dto.PatientDTO.builder()
                        .id(PATIENT_ID)
                        .firstName("John")
                        .lastName("Smith")
                        .mrn("MRN123456")
                        .build();

        when(vitalsRepository.findByIdAndTenant(any(UUID.class), eq(TENANT_ID)))
                .thenReturn(Optional.of(testVitals));
        when(patientServiceClient.getPatient(PATIENT_ID, TENANT_ID))
                .thenReturn(patient);

        // When
        Optional<VitalSignsResponse> result = vitalsService.getVitalById(testVitals.getId(), TENANT_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getPatientName()).isEqualTo("Smith, John");
        verify(patientServiceClient).getPatient(PATIENT_ID, TENANT_ID);
    }

    @Test
    void getVitalById_ShouldHandleNullPatientName_WhenPatientNotFound() {
        // Given
        when(vitalsRepository.findByIdAndTenant(any(UUID.class), eq(TENANT_ID)))
                .thenReturn(Optional.of(testVitals));
        when(patientServiceClient.getPatient(PATIENT_ID, TENANT_ID))
                .thenReturn(null);

        // When
        Optional<VitalSignsResponse> result = vitalsService.getVitalById(testVitals.getId(), TENANT_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getPatientName()).isNull();
        verify(patientServiceClient).getPatient(PATIENT_ID, TENANT_ID);
    }

    @Test
    void getCriticalAlerts_ShouldIncludePatientNames_WhenPatientsFound() {
        // Given
        VitalSignsRecordEntity criticalVitals = VitalSignsRecordEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .heartRate(new BigDecimal("140"))
                .alertStatus("critical")
                .alertMessage("Critical: Heart rate 140 bpm (normal: 60-100)")
                .build();

        com.healthdata.clinicalworkflow.client.dto.PatientDTO patient =
                com.healthdata.clinicalworkflow.client.dto.PatientDTO.builder()
                        .id(PATIENT_ID)
                        .firstName("Jane")
                        .lastName("Doe")
                        .mrn("MRN789")
                        .build();

        when(vitalsRepository.findCriticalAlertsByTenant(TENANT_ID))
                .thenReturn(Collections.singletonList(criticalVitals));
        when(patientServiceClient.getPatient(PATIENT_ID, TENANT_ID))
                .thenReturn(patient);

        // When
        List<VitalAlertResponse> alerts = vitalsService.getCriticalAlerts(TENANT_ID);

        // Then
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getPatientName()).isEqualTo("Doe, Jane");
        verify(patientServiceClient).getPatient(PATIENT_ID, TENANT_ID);
    }

    @Test
    void getCriticalAlerts_ShouldHandlePatientServiceFailure_Gracefully() {
        // Given
        VitalSignsRecordEntity criticalVitals = VitalSignsRecordEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .oxygenSaturation(new BigDecimal("83"))
                .alertStatus("critical")
                .alertMessage("Critical: O2 saturation 83% (normal: >90%)")
                .build();

        when(vitalsRepository.findCriticalAlertsByTenant(TENANT_ID))
                .thenReturn(Collections.singletonList(criticalVitals));
        when(patientServiceClient.getPatient(PATIENT_ID, TENANT_ID))
                .thenThrow(new RuntimeException("Patient service unavailable"));

        // When
        List<VitalAlertResponse> alerts = vitalsService.getCriticalAlerts(TENANT_ID);

        // Then
        // Alert should still be returned despite patient service failure (circuit breaker)
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getPatientName()).isNull(); // Name unavailable but alert delivered
        assertThat(alerts.get(0).getSeverity()).isEqualTo("CRITICAL");
    }

    @Test
    void patientDTO_GetFormattedName_ShouldHandleVariousFormats() {
        // Test full name
        com.healthdata.clinicalworkflow.client.dto.PatientDTO fullName =
                com.healthdata.clinicalworkflow.client.dto.PatientDTO.builder()
                        .firstName("John")
                        .lastName("Smith")
                        .build();
        assertThat(fullName.getFormattedName()).isEqualTo("Smith, John");

        // Test first name only
        com.healthdata.clinicalworkflow.client.dto.PatientDTO firstOnly =
                com.healthdata.clinicalworkflow.client.dto.PatientDTO.builder()
                        .firstName("John")
                        .build();
        assertThat(firstOnly.getFormattedName()).isEqualTo("John");

        // Test last name only
        com.healthdata.clinicalworkflow.client.dto.PatientDTO lastOnly =
                com.healthdata.clinicalworkflow.client.dto.PatientDTO.builder()
                        .lastName("Smith")
                        .build();
        assertThat(lastOnly.getFormattedName()).isEqualTo("Smith");

        // Test no name
        com.healthdata.clinicalworkflow.client.dto.PatientDTO noName =
                com.healthdata.clinicalworkflow.client.dto.PatientDTO.builder().build();
        assertThat(noName.getFormattedName()).isEqualTo("Unknown Patient");
    }
}
