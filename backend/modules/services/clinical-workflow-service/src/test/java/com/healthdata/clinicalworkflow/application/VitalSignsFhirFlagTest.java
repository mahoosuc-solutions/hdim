package com.healthdata.clinicalworkflow.application;

import com.healthdata.clinicalworkflow.client.FhirServiceClient;
import com.healthdata.clinicalworkflow.client.PatientServiceClient;
import com.healthdata.clinicalworkflow.domain.model.VitalSignsRecordEntity;
import com.healthdata.clinicalworkflow.domain.repository.RoomAssignmentRepository;
import com.healthdata.clinicalworkflow.domain.repository.VitalSignsRecordRepository;
import com.healthdata.clinicalworkflow.event.VitalSignsAlertPublisher;
import org.hl7.fhir.r4.model.Flag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FHIR Flag creation in VitalSignsService
 *
 * Tests creation of FHIR R4 Flag resources for abnormal vital signs
 * with proper SNOMED CT codes and clinical categorization.
 */
@ExtendWith(MockitoExtension.class)
class VitalSignsFhirFlagTest {

    @Mock
    private VitalSignsRecordRepository vitalsRepository;

    @Mock
    private RoomAssignmentRepository roomAssignmentRepository;

    @Mock
    private PatientServiceClient patientServiceClient;

    @Mock
    private FhirServiceClient fhirServiceClient;

    @Mock
    private VitalSignsAlertPublisher vitalSignsAlertPublisher;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private VitalSignsService vitalSignsService;

    @Captor
    private ArgumentCaptor<Flag> flagCaptor;

    private VitalSignsRecordEntity testVitals;
    private UUID patientId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        tenantId = "test-tenant";

        testVitals = VitalSignsRecordEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .patientId(patientId)
                .encounterId("encounter-123")
                .recordedBy("ma-smith")
                .systolicBp(new BigDecimal("180"))
                .diastolicBp(new BigDecimal("95"))
                .heartRate(new BigDecimal("102"))
                .temperatureF(new BigDecimal("98.6"))
                .respirationRate(new BigDecimal("18"))
                .oxygenSaturation(new BigDecimal("96"))
                .recordedAt(Instant.now())
                .alertStatus("critical")
                .alertMessage("Systolic BP 180 mmHg - critical high")
                .build();
    }

    @Test
    void shouldCreateFlagForHighBloodPressure() {
        // Given: Critical high blood pressure alert
        testVitals.setAlertMessage("Systolic BP > 180 mmHg (critical)");
        Flag mockFlag = new Flag();
        mockFlag.setId("flag-123");
        when(fhirServiceClient.createFlag(any(Flag.class), eq(tenantId), anyString()))
                .thenReturn(mockFlag);

        // When: Trigger alerts (which creates Flag)
        vitalSignsService.triggerAlerts(testVitals, tenantId);

        // Then: Flag created with SNOMED code 371861000 (High blood pressure)
        verify(fhirServiceClient, atLeastOnce()).createFlag(
                flagCaptor.capture(), eq(tenantId), anyString());

        Flag createdFlag = flagCaptor.getAllValues().stream()
                .filter(f -> f.getCode().getCodingFirstRep().getCode().equals("371861000"))
                .findFirst()
                .orElseThrow();

        assertThat(createdFlag.getStatus()).isEqualTo(Flag.FlagStatus.ACTIVE);
        assertThat(createdFlag.getCode().getCodingFirstRep().getSystem()).isEqualTo("http://snomed.info/sct");
        assertThat(createdFlag.getCode().getCodingFirstRep().getCode()).isEqualTo("371861000");
        assertThat(createdFlag.getCode().getCodingFirstRep().getDisplay()).isEqualTo("High blood pressure");
        assertThat(createdFlag.getSubject().getReference()).isEqualTo("Patient/" + patientId);
    }

    @Test
    void shouldCreateFlagForLowBloodPressure() {
        // Given: Low blood pressure alert
        testVitals.setAlertMessage("Systolic BP < 70 mmHg (low)");
        Flag mockFlag = new Flag();
        mockFlag.setId("flag-123");
        when(fhirServiceClient.createFlag(any(Flag.class), eq(tenantId), anyString()))
                .thenReturn(mockFlag);

        // When: Trigger alerts
        vitalSignsService.triggerAlerts(testVitals, tenantId);

        // Then: Flag created with SNOMED code 371862007 (Low blood pressure)
        verify(fhirServiceClient, atLeastOnce()).createFlag(
                flagCaptor.capture(), eq(tenantId), anyString());

        Flag createdFlag = flagCaptor.getAllValues().stream()
                .filter(f -> f.getCode().getCodingFirstRep().getCode().equals("371862007"))
                .findFirst()
                .orElseThrow();

        assertThat(createdFlag.getCode().getCodingFirstRep().getCode()).isEqualTo("371862007");
        assertThat(createdFlag.getCode().getCodingFirstRep().getDisplay()).isEqualTo("Low blood pressure");
    }

    @Test
    void shouldCreateFlagForTachycardia() {
        // Given: High heart rate alert
        testVitals.setAlertMessage("Heart Rate > 130 bpm (critical)");
        Flag mockFlag = new Flag();
        mockFlag.setId("flag-123");
        when(fhirServiceClient.createFlag(any(Flag.class), eq(tenantId), anyString()))
                .thenReturn(mockFlag);

        // When: Trigger alerts
        vitalSignsService.triggerAlerts(testVitals, tenantId);

        // Then: Flag created with SNOMED code 80313002 (Tachycardia)
        verify(fhirServiceClient, atLeastOnce()).createFlag(
                flagCaptor.capture(), eq(tenantId), anyString());

        Flag createdFlag = flagCaptor.getAllValues().stream()
                .filter(f -> f.getCode().getCodingFirstRep().getCode().equals("80313002"))
                .findFirst()
                .orElseThrow();

        assertThat(createdFlag.getCode().getCodingFirstRep().getCode()).isEqualTo("80313002");
        assertThat(createdFlag.getCode().getCodingFirstRep().getDisplay()).isEqualTo("Tachycardia");
    }

    @Test
    void shouldCreateFlagForBradycardia() {
        // Given: Low heart rate alert
        testVitals.setAlertMessage("Heart Rate < 40 bpm (critical low)");
        Flag mockFlag = new Flag();
        mockFlag.setId("flag-123");
        when(fhirServiceClient.createFlag(any(Flag.class), eq(tenantId), anyString()))
                .thenReturn(mockFlag);

        // When: Trigger alerts
        vitalSignsService.triggerAlerts(testVitals, tenantId);

        // Then: Flag created with SNOMED code 48867003 (Bradycardia)
        verify(fhirServiceClient, atLeastOnce()).createFlag(
                flagCaptor.capture(), eq(tenantId), anyString());

        Flag createdFlag = flagCaptor.getAllValues().stream()
                .filter(f -> f.getCode().getCodingFirstRep().getCode().equals("48867003"))
                .findFirst()
                .orElseThrow();

        assertThat(createdFlag.getCode().getCodingFirstRep().getCode()).isEqualTo("48867003");
        assertThat(createdFlag.getCode().getCodingFirstRep().getDisplay()).isEqualTo("Bradycardia");
    }

    @Test
    void shouldCreateFlagForFever() {
        // Given: High temperature alert
        testVitals.setAlertMessage("Temperature > 101°F (fever)");
        Flag mockFlag = new Flag();
        mockFlag.setId("flag-123");
        when(fhirServiceClient.createFlag(any(Flag.class), eq(tenantId), anyString()))
                .thenReturn(mockFlag);

        // When: Trigger alerts
        vitalSignsService.triggerAlerts(testVitals, tenantId);

        // Then: Flag created with SNOMED code 386661006 (Fever)
        verify(fhirServiceClient, atLeastOnce()).createFlag(
                flagCaptor.capture(), eq(tenantId), anyString());

        Flag createdFlag = flagCaptor.getAllValues().stream()
                .filter(f -> f.getCode().getCodingFirstRep().getCode().equals("386661006"))
                .findFirst()
                .orElseThrow();

        assertThat(createdFlag.getCode().getCodingFirstRep().getCode()).isEqualTo("386661006");
        assertThat(createdFlag.getCode().getCodingFirstRep().getDisplay()).isEqualTo("Fever");
    }

    @Test
    void shouldCreateFlagForHypothermia() {
        // Given: Low temperature alert
        testVitals.setAlertMessage("Temperature < 95°F (hypothermia)");
        Flag mockFlag = new Flag();
        mockFlag.setId("flag-123");
        when(fhirServiceClient.createFlag(any(Flag.class), eq(tenantId), anyString()))
                .thenReturn(mockFlag);

        // When: Trigger alerts
        vitalSignsService.triggerAlerts(testVitals, tenantId);

        // Then: Flag created with SNOMED code 89176007 (Hypothermia)
        verify(fhirServiceClient, atLeastOnce()).createFlag(
                flagCaptor.capture(), eq(tenantId), anyString());

        Flag createdFlag = flagCaptor.getAllValues().stream()
                .filter(f -> f.getCode().getCodingFirstRep().getCode().equals("89176007"))
                .findFirst()
                .orElseThrow();

        assertThat(createdFlag.getCode().getCodingFirstRep().getCode()).isEqualTo("89176007");
        assertThat(createdFlag.getCode().getCodingFirstRep().getDisplay()).isEqualTo("Hypothermia");
    }

    @Test
    void shouldCreateFlagForHypoxemia() {
        // Given: Low oxygen saturation alert
        testVitals.setAlertMessage("O2 < 85% (critical)");
        Flag mockFlag = new Flag();
        mockFlag.setId("flag-123");
        when(fhirServiceClient.createFlag(any(Flag.class), eq(tenantId), anyString()))
                .thenReturn(mockFlag);

        // When: Trigger alerts
        vitalSignsService.triggerAlerts(testVitals, tenantId);

        // Then: Flag created with SNOMED code 389086002 (Hypoxemia)
        verify(fhirServiceClient, atLeastOnce()).createFlag(
                flagCaptor.capture(), eq(tenantId), anyString());

        Flag createdFlag = flagCaptor.getAllValues().stream()
                .filter(f -> f.getCode().getCodingFirstRep().getCode().equals("389086002"))
                .findFirst()
                .orElseThrow();

        assertThat(createdFlag.getCode().getCodingFirstRep().getCode()).isEqualTo("389086002");
        assertThat(createdFlag.getCode().getCodingFirstRep().getDisplay()).isEqualTo("Hypoxemia");
    }

    @Test
    void shouldCreateFlagForTachypnea() {
        // Given: High respiratory rate alert
        testVitals.setAlertMessage("Respiration Rate > 24 breaths/min (high)");
        Flag mockFlag = new Flag();
        mockFlag.setId("flag-123");
        when(fhirServiceClient.createFlag(any(Flag.class), eq(tenantId), anyString()))
                .thenReturn(mockFlag);

        // When: Trigger alerts
        vitalSignsService.triggerAlerts(testVitals, tenantId);

        // Then: Flag created with SNOMED code 271823003 (Tachypnea)
        verify(fhirServiceClient, atLeastOnce()).createFlag(
                flagCaptor.capture(), eq(tenantId), anyString());

        Flag createdFlag = flagCaptor.getAllValues().stream()
                .filter(f -> f.getCode().getCodingFirstRep().getCode().equals("271823003"))
                .findFirst()
                .orElseThrow();

        assertThat(createdFlag.getCode().getCodingFirstRep().getCode()).isEqualTo("271823003");
        assertThat(createdFlag.getCode().getCodingFirstRep().getDisplay()).isEqualTo("Tachypnea");
    }

    @Test
    void shouldCreateFlagForBradypnea() {
        // Given: Low respiratory rate alert
        testVitals.setAlertMessage("Respiration Rate < 8 breaths/min (low)");
        Flag mockFlag = new Flag();
        mockFlag.setId("flag-123");
        when(fhirServiceClient.createFlag(any(Flag.class), eq(tenantId), anyString()))
                .thenReturn(mockFlag);

        // When: Trigger alerts
        vitalSignsService.triggerAlerts(testVitals, tenantId);

        // Then: Flag created with SNOMED code 271825005 (Bradypnea)
        verify(fhirServiceClient, atLeastOnce()).createFlag(
                flagCaptor.capture(), eq(tenantId), anyString());

        Flag createdFlag = flagCaptor.getAllValues().stream()
                .filter(f -> f.getCode().getCodingFirstRep().getCode().equals("271825005"))
                .findFirst()
                .orElseThrow();

        assertThat(createdFlag.getCode().getCodingFirstRep().getCode()).isEqualTo("271825005");
        assertThat(createdFlag.getCode().getCodingFirstRep().getDisplay()).isEqualTo("Bradypnea");
    }

    @Test
    void shouldCreateMultipleFlagsForMultipleAbnormalities() {
        // Given: Multiple abnormal vitals
        testVitals.setAlertMessage("Heart Rate > 130 bpm, O2 < 85%");
        Flag mockFlag = new Flag();
        mockFlag.setId("flag-123");
        when(fhirServiceClient.createFlag(any(Flag.class), eq(tenantId), anyString()))
                .thenReturn(mockFlag);

        // When: Trigger alerts
        vitalSignsService.triggerAlerts(testVitals, tenantId);

        // Then: Multiple flags created (Tachycardia + Hypoxemia)
        verify(fhirServiceClient, atLeast(2)).createFlag(
                any(Flag.class), eq(tenantId), anyString());
    }

    @Test
    void shouldIncludeClinicalCategory() {
        // Given: Abnormal vitals
        testVitals.setAlertMessage("Systolic BP > 180 mmHg");
        Flag mockFlag = new Flag();
        mockFlag.setId("flag-123");
        when(fhirServiceClient.createFlag(any(Flag.class), eq(tenantId), anyString()))
                .thenReturn(mockFlag);

        // When: Trigger alerts
        vitalSignsService.triggerAlerts(testVitals, tenantId);

        // Then: Flag includes clinical category
        verify(fhirServiceClient, atLeastOnce()).createFlag(
                flagCaptor.capture(), eq(tenantId), anyString());

        Flag createdFlag = flagCaptor.getValue();
        assertThat(createdFlag.getCategory()).isNotEmpty();
        assertThat(createdFlag.getCategory().get(0).getCodingFirstRep().getCode()).isEqualTo("clinical");
        assertThat(createdFlag.getCategory().get(0).getCodingFirstRep().getDisplay()).isEqualTo("Clinical");
    }

    @Test
    void shouldIncludePatientReference() {
        // Given: Abnormal vitals
        testVitals.setAlertMessage("Heart Rate > 130 bpm");
        Flag mockFlag = new Flag();
        mockFlag.setId("flag-123");
        when(fhirServiceClient.createFlag(any(Flag.class), eq(tenantId), anyString()))
                .thenReturn(mockFlag);

        // When: Trigger alerts
        vitalSignsService.triggerAlerts(testVitals, tenantId);

        // Then: Flag includes patient reference
        verify(fhirServiceClient, atLeastOnce()).createFlag(
                flagCaptor.capture(), eq(tenantId), anyString());

        Flag createdFlag = flagCaptor.getValue();
        assertThat(createdFlag.getSubject()).isNotNull();
        assertThat(createdFlag.getSubject().getReference()).isEqualTo("Patient/" + patientId);
    }

    @Test
    void shouldIncludeRecordedTimestamp() {
        // Given: Abnormal vitals
        testVitals.setAlertMessage("O2 < 85%");
        Flag mockFlag = new Flag();
        mockFlag.setId("flag-123");
        when(fhirServiceClient.createFlag(any(Flag.class), eq(tenantId), anyString()))
                .thenReturn(mockFlag);

        // When: Trigger alerts
        vitalSignsService.triggerAlerts(testVitals, tenantId);

        // Then: Flag includes period start (when abnormal vitals recorded)
        verify(fhirServiceClient, atLeastOnce()).createFlag(
                flagCaptor.capture(), eq(tenantId), anyString());

        Flag createdFlag = flagCaptor.getValue();
        assertThat(createdFlag.getPeriod()).isNotNull();
        assertThat(createdFlag.getPeriod().getStart()).isNotNull();
    }

    @Test
    void shouldHandleNullAlertMessage() {
        // Given: Vitals with null alert message
        testVitals.setAlertMessage(null);

        // When: Trigger alerts
        vitalSignsService.triggerAlerts(testVitals, tenantId);

        // Then: No flags created
        verify(fhirServiceClient, never()).createFlag(any(Flag.class), anyString(), anyString());
    }

    @Test
    void shouldHandleEmptyAlertMessage() {
        // Given: Vitals with empty alert message
        testVitals.setAlertMessage("");

        // When: Trigger alerts
        vitalSignsService.triggerAlerts(testVitals, tenantId);

        // Then: No flags created
        verify(fhirServiceClient, never()).createFlag(any(Flag.class), anyString(), anyString());
    }

    @Test
    void shouldHandleFhirServiceFailure() {
        // Given: FHIR service failure
        testVitals.setAlertMessage("Systolic BP > 180 mmHg");
        when(fhirServiceClient.createFlag(any(Flag.class), eq(tenantId), anyString()))
                .thenReturn(null); // Service returns null

        // When: Trigger alerts
        vitalSignsService.triggerAlerts(testVitals, tenantId);

        // Then: No exception thrown (non-blocking behavior)
        // Test passes if no exception is thrown
    }

    @Test
    void shouldHandleFhirServiceException() {
        // Given: FHIR service throws exception
        testVitals.setAlertMessage("Heart Rate > 130 bpm");
        when(fhirServiceClient.createFlag(any(Flag.class), eq(tenantId), anyString()))
                .thenThrow(new RuntimeException("FHIR service error"));

        // When: Trigger alerts
        vitalSignsService.triggerAlerts(testVitals, tenantId);

        // Then: No exception thrown (non-blocking behavior)
        // Test passes if no exception is thrown
    }

    @Test
    void shouldUseRecordedByAsAuthor() {
        // Given: Vitals with recordedBy field
        testVitals.setRecordedBy("ma-smith");
        testVitals.setAlertMessage("Systolic BP > 180 mmHg");
        Flag mockFlag = new Flag();
        mockFlag.setId("flag-123");
        when(fhirServiceClient.createFlag(any(Flag.class), eq(tenantId), anyString()))
                .thenReturn(mockFlag);

        // When: Trigger alerts
        vitalSignsService.triggerAlerts(testVitals, tenantId);

        // Then: Flag created with recordedBy as author
        verify(fhirServiceClient).createFlag(any(Flag.class), eq(tenantId), eq("ma-smith"));
    }

    @Test
    void shouldUseSystemWhenRecordedByNull() {
        // Given: Vitals without recordedBy field
        testVitals.setRecordedBy(null);
        testVitals.setAlertMessage("O2 < 85%");
        Flag mockFlag = new Flag();
        mockFlag.setId("flag-123");
        when(fhirServiceClient.createFlag(any(Flag.class), eq(tenantId), anyString()))
                .thenReturn(mockFlag);

        // When: Trigger alerts
        vitalSignsService.triggerAlerts(testVitals, tenantId);

        // Then: Flag created with "system" as author
        verify(fhirServiceClient).createFlag(any(Flag.class), eq(tenantId), eq("system"));
    }
}
