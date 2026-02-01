package com.healthdata.clinicalworkflow.application;

import com.healthdata.clinicalworkflow.client.FhirServiceClient;
import com.healthdata.clinicalworkflow.client.PatientServiceClient;
import com.healthdata.clinicalworkflow.domain.model.VitalSignsRecordEntity;
import com.healthdata.clinicalworkflow.domain.repository.RoomAssignmentRepository;
import com.healthdata.clinicalworkflow.domain.repository.VitalSignsRecordRepository;
import org.hl7.fhir.r4.model.*;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FHIR Observation resource creation in VitalSignsService
 *
 * Tests issue #289 implementation: FHIR R4 Observation creation for vital signs
 * with proper LOINC codes, UCUM units, and circuit breaker handling.
 */
@ExtendWith(MockitoExtension.class)
class VitalSignsFhirObservationTest {

    @Mock
    private VitalSignsRecordRepository vitalsRepository;

    @Mock
    private RoomAssignmentRepository roomAssignmentRepository;

    @Mock
    private PatientServiceClient patientServiceClient;

    @Mock
    private FhirServiceClient fhirServiceClient;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private VitalSignsService vitalSignsService;

    @Captor
    private ArgumentCaptor<Observation> observationCaptor;

    private VitalSignsRecordEntity testVitals;

    @BeforeEach
    void setUp() {
        UUID patientId = UUID.randomUUID();
        testVitals = VitalSignsRecordEntity.builder()
                .id(UUID.randomUUID())
                .tenantId("test-tenant")
                .patientId(patientId)
                .encounterId("encounter-123")
                .recordedBy("dr-smith")
                .systolicBp(new BigDecimal("120"))
                .diastolicBp(new BigDecimal("80"))
                .heartRate(new BigDecimal("75"))
                .temperatureF(new BigDecimal("98.6"))
                .respirationRate(new BigDecimal("16"))
                .oxygenSaturation(new BigDecimal("98"))
                .weightKg(new BigDecimal("70.5"))
                .heightCm(new BigDecimal("175"))
                .bmi(new BigDecimal("23.0"))
                .recordedAt(Instant.now())
                .notes("Patient feeling well")
                .alertStatus("normal")
                .build();
    }

    @Test
    void shouldCreateObservationWithAllVitalSigns() {
        // Given: FHIR service will return created observation
        Observation createdObservation = new Observation();
        createdObservation.setId("obs-123");
        when(fhirServiceClient.createObservation(any(Observation.class), eq("test-tenant"), eq("dr-smith")))
                .thenReturn(createdObservation);

        // When: Create observation resource
        Observation result = vitalSignsService.createObservationResource(testVitals);

        // Then: Observation was created and sent to FHIR service
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("obs-123");

        // Verify observation structure
        verify(fhirServiceClient).createObservation(observationCaptor.capture(), eq("test-tenant"), eq("dr-smith"));
        Observation capturedObs = observationCaptor.getValue();

        assertThat(capturedObs.getStatus()).isEqualTo(Observation.ObservationStatus.FINAL);
        assertThat(capturedObs.getSubject().getReference()).isEqualTo("Patient/" + testVitals.getPatientId());
        assertThat(capturedObs.getEncounter().getReference()).isEqualTo("Encounter/encounter-123");
    }

    @Test
    void shouldUseCorrectLoincCodeForVitalSignsPanel() {
        // Given: FHIR service available
        Observation createdObservation = new Observation();
        createdObservation.setId("obs-123");
        when(fhirServiceClient.createObservation(any(Observation.class), any(), any()))
                .thenReturn(createdObservation);

        // When: Create observation
        vitalSignsService.createObservationResource(testVitals);

        // Then: Observation uses LOINC 85353-1 for Vital Signs Panel
        verify(fhirServiceClient).createObservation(observationCaptor.capture(), any(), any());
        Observation obs = observationCaptor.getValue();

        CodeableConcept code = obs.getCode();
        assertThat(code.getCoding()).hasSize(1);
        Coding coding = code.getCoding().get(0);
        assertThat(coding.getSystem()).isEqualTo("http://loinc.org");
        assertThat(coding.getCode()).isEqualTo("85353-1");
        assertThat(coding.getDisplay()).contains("Vital signs");
    }

    @Test
    void shouldCreateComponentsForEachVitalSign() {
        // Given: FHIR service available
        when(fhirServiceClient.createObservation(any(Observation.class), any(), any()))
                .thenReturn(new Observation());

        // When: Create observation
        vitalSignsService.createObservationResource(testVitals);

        // Then: Observation has components for all 9 vital signs
        verify(fhirServiceClient).createObservation(observationCaptor.capture(), any(), any());
        Observation obs = observationCaptor.getValue();

        assertThat(obs.getComponent()).hasSize(9);

        // Verify systolic BP component (LOINC 8480-6)
        Observation.ObservationComponentComponent systolicComponent = obs.getComponent().stream()
                .filter(c -> c.getCode().getCoding().get(0).getCode().equals("8480-6"))
                .findFirst()
                .orElse(null);

        assertThat(systolicComponent).isNotNull();
        assertThat(systolicComponent.getCode().getCoding().get(0).getDisplay()).isEqualTo("Systolic blood pressure");
        Quantity value = (Quantity) systolicComponent.getValue();
        assertThat(value.getValue()).isEqualByComparingTo(new BigDecimal("120"));
        assertThat(value.getUnit()).isEqualTo("mm[Hg]");
        assertThat(value.getSystem()).isEqualTo("http://unitsofmeasure.org");
    }

    @Test
    void shouldUseCorrectUcumUnitsForAllComponents() {
        // Given: FHIR service available
        when(fhirServiceClient.createObservation(any(Observation.class), any(), any()))
                .thenReturn(new Observation());

        // When: Create observation
        vitalSignsService.createObservationResource(testVitals);

        // Then: Each component uses correct UCUM unit
        verify(fhirServiceClient).createObservation(observationCaptor.capture(), any(), any());
        Observation obs = observationCaptor.getValue();

        // Verify units for key components
        assertComponentUnit(obs, "8480-6", "mm[Hg]");  // Systolic BP
        assertComponentUnit(obs, "8462-4", "mm[Hg]");  // Diastolic BP
        assertComponentUnit(obs, "8867-4", "/min");    // Heart rate
        assertComponentUnit(obs, "8310-5", "[degF]");  // Temperature
        assertComponentUnit(obs, "9279-1", "/min");    // Respiratory rate
        assertComponentUnit(obs, "2708-6", "%");       // O2 saturation
        assertComponentUnit(obs, "29463-7", "kg");     // Weight
        assertComponentUnit(obs, "8302-2", "cm");      // Height
        assertComponentUnit(obs, "39156-5", "kg/m2");  // BMI
    }

    @Test
    void shouldIncludeNotesAsAnnotation() {
        // Given: FHIR service available
        when(fhirServiceClient.createObservation(any(Observation.class), any(), any()))
                .thenReturn(new Observation());

        // When: Create observation with notes
        vitalSignsService.createObservationResource(testVitals);

        // Then: Notes are included as annotation
        verify(fhirServiceClient).createObservation(observationCaptor.capture(), any(), any());
        Observation obs = observationCaptor.getValue();

        assertThat(obs.getNote()).hasSize(1);
        assertThat(obs.getNote().get(0).getText()).isEqualTo("Patient feeling well");
    }

    @Test
    void shouldSkipComponentsForNullValues() {
        // Given: Vitals with some null values
        testVitals.setRespirationRate(null);
        testVitals.setWeightKg(null);
        testVitals.setHeightCm(null);
        testVitals.setBmi(null);

        when(fhirServiceClient.createObservation(any(Observation.class), any(), any()))
                .thenReturn(new Observation());

        // When: Create observation
        vitalSignsService.createObservationResource(testVitals);

        // Then: Only non-null components are included
        verify(fhirServiceClient).createObservation(observationCaptor.capture(), any(), any());
        Observation obs = observationCaptor.getValue();

        // Should have 5 components (systolic, diastolic, HR, temp, O2) instead of 9
        assertThat(obs.getComponent()).hasSize(5);
        assertThat(obs.getComponent()).noneMatch(c ->
                c.getCode().getCoding().get(0).getCode().equals("9279-1"));  // Respiratory rate
    }

    @Test
    void shouldHandleEncounterIdNull() {
        // Given: Vitals without encounter ID
        testVitals.setEncounterId(null);
        when(fhirServiceClient.createObservation(any(Observation.class), any(), any()))
                .thenReturn(new Observation());

        // When: Create observation
        vitalSignsService.createObservationResource(testVitals);

        // Then: Observation is created without encounter reference
        verify(fhirServiceClient).createObservation(observationCaptor.capture(), any(), any());
        Observation obs = observationCaptor.getValue();

        assertThat(obs.hasEncounter()).isFalse();
    }

    @Test
    void shouldUseSystemAsRecordedByWhenNull() {
        // Given: Vitals without recorded by
        testVitals.setRecordedBy(null);
        Observation createdObservation = new Observation();
        when(fhirServiceClient.createObservation(any(Observation.class), eq("test-tenant"), eq("system")))
                .thenReturn(createdObservation);

        // When: Create observation
        vitalSignsService.createObservationResource(testVitals);

        // Then: Uses "system" as user ID
        verify(fhirServiceClient).createObservation(any(Observation.class), eq("test-tenant"), eq("system"));
    }

    @Test
    void shouldHandleCircuitBreakerFallback() {
        // Given: FHIR service unavailable (circuit breaker fallback returns null)
        when(fhirServiceClient.createObservation(any(Observation.class), any(), any()))
                .thenReturn(null);

        // When: Create observation
        Observation result = vitalSignsService.createObservationResource(testVitals);

        // Then: Returns null gracefully (no exception thrown)
        assertThat(result).isNull();
        verify(fhirServiceClient).createObservation(any(Observation.class), eq("test-tenant"), eq("dr-smith"));
    }

    @Test
    void shouldHandleExceptionDuringObservationCreation() {
        // Given: FHIR service throws exception
        when(fhirServiceClient.createObservation(any(Observation.class), any(), any()))
                .thenThrow(new RuntimeException("FHIR service error"));

        // When: Create observation
        Observation result = vitalSignsService.createObservationResource(testVitals);

        // Then: Returns null gracefully (exception caught and logged)
        assertThat(result).isNull();
    }

    @Test
    void shouldUseCorrectLoincCodesForAllComponents() {
        // Given: FHIR service available
        when(fhirServiceClient.createObservation(any(Observation.class), any(), any()))
                .thenReturn(new Observation());

        // When: Create observation
        vitalSignsService.createObservationResource(testVitals);

        // Then: All components use correct LOINC codes
        verify(fhirServiceClient).createObservation(observationCaptor.capture(), any(), any());
        Observation obs = observationCaptor.getValue();

        // Verify LOINC codes for all vital signs
        assertComponentLoincCode(obs, "8480-6", "Systolic blood pressure");
        assertComponentLoincCode(obs, "8462-4", "Diastolic blood pressure");
        assertComponentLoincCode(obs, "8867-4", "Heart rate");
        assertComponentLoincCode(obs, "8310-5", "Body temperature");
        assertComponentLoincCode(obs, "9279-1", "Respiratory rate");
        assertComponentLoincCode(obs, "2708-6", "Oxygen saturation in Arterial blood");
        assertComponentLoincCode(obs, "29463-7", "Body weight");
        assertComponentLoincCode(obs, "8302-2", "Body height");
        assertComponentLoincCode(obs, "39156-5", "Body mass index (BMI)");
    }

    @Test
    void shouldSetEffectiveDateTimeFromRecordedAt() {
        // Given: FHIR service available
        Instant recordedTime = Instant.parse("2026-01-23T10:30:00Z");
        testVitals.setRecordedAt(recordedTime);
        when(fhirServiceClient.createObservation(any(Observation.class), any(), any()))
                .thenReturn(new Observation());

        // When: Create observation
        vitalSignsService.createObservationResource(testVitals);

        // Then: Effective date/time matches recorded time
        verify(fhirServiceClient).createObservation(observationCaptor.capture(), any(), any());
        Observation obs = observationCaptor.getValue();

        DateTimeType effectiveDateTime = (DateTimeType) obs.getEffective();
        assertThat(effectiveDateTime.getValue()).isEqualTo(java.util.Date.from(recordedTime));
    }

    // ========== HELPER METHODS ==========

    private void assertComponentUnit(Observation obs, String loincCode, String expectedUnit) {
        Observation.ObservationComponentComponent component = obs.getComponent().stream()
                .filter(c -> c.getCode().getCoding().get(0).getCode().equals(loincCode))
                .findFirst()
                .orElse(null);

        assertThat(component).isNotNull();
        Quantity value = (Quantity) component.getValue();
        assertThat(value.getUnit()).isEqualTo(expectedUnit);
        assertThat(value.getCode()).isEqualTo(expectedUnit);
        assertThat(value.getSystem()).isEqualTo("http://unitsofmeasure.org");
    }

    private void assertComponentLoincCode(Observation obs, String loincCode, String expectedDisplay) {
        Observation.ObservationComponentComponent component = obs.getComponent().stream()
                .filter(c -> c.getCode().getCoding().get(0).getCode().equals(loincCode))
                .findFirst()
                .orElse(null);

        assertThat(component).isNotNull();
        Coding coding = component.getCode().getCoding().get(0);
        assertThat(coding.getSystem()).isEqualTo("http://loinc.org");
        assertThat(coding.getCode()).isEqualTo(loincCode);
        assertThat(coding.getDisplay()).isEqualTo(expectedDisplay);
    }
}
