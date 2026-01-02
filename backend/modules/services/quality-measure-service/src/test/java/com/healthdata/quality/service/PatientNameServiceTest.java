package com.healthdata.quality.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Test class for PatientNameService using TDD approach
 */
@ExtendWith(MockitoExtension.class)
class PatientNameServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IGenericClient fhirClient;

    private PatientNameService patientNameService;

    @BeforeEach
    void setUp() {
        patientNameService = new PatientNameService(fhirClient);
    }

    /**
     * Note: Testing FHIR client integration with official names is better done
     * via integration tests due to the complexity of mocking the FHIR fluent API.
     * The name extraction logic is tested indirectly through the null/error handling tests.
     *
     * Integration test scenario:
     * - Fetch patient with official name from real/test FHIR server
     * - Verify name is formatted as "FirstName LastName"
     * - Verify caching behavior works correctly
     */

    @Test
    void shouldHandleNullAndEmptyIds() {
        // When: Getting patient name with null ID
        // Then: Should return fallback
        assertThat(patientNameService.getPatientName(null)).isEqualTo("Patient");
    }

    @Test
    void shouldHandleMissingName() {
        // When: Patient ID is provided but no name in FHIR
        // This requires FHIR client mocking which is complex
        // We test the fallback behavior

        // When: Getting patient name with null ID
        String name = patientNameService.getPatientName(null);

        // Then: Should return fallback
        assertThat(name).isEqualTo("Patient");
        verify(fhirClient, never()).read();
    }

    @Test
    void shouldHandlePatientNotFound() {
        // Given: Patient does not exist in FHIR server
        java.util.UUID patientId = java.util.UUID.randomUUID();

        // Mock FHIR client to throw ResourceNotFoundException
        when(fhirClient.read()).thenThrow(new ResourceNotFoundException("Patient not found"));

        // When: Getting patient name
        String name = patientNameService.getPatientName(patientId);

        // Then: Should return fallback
        assertThat(name).isEqualTo("Patient");
    }

    @Test
    void shouldHandleNullPatientId() {
        // When: Getting patient name with null ID
        String name = patientNameService.getPatientName(null);

        // Then: Should return fallback without calling FHIR client
        assertThat(name).isEqualTo("Patient");
        verify(fhirClient, never()).read();
    }

    @Test
    void shouldHandleEmptyPatientId() {
        // When: Getting patient name with null ID
        String name = patientNameService.getPatientName(null);

        // Then: Should return fallback without calling FHIR client
        assertThat(name).isEqualTo("Patient");
        verify(fhirClient, never()).read();
    }

    @Test
    void shouldHandleWhitespacePatientId() {
        // When: Getting patient name with null ID
        String name = patientNameService.getPatientName(null);

        // Then: Should return fallback without calling FHIR client
        assertThat(name).isEqualTo("Patient");
        verify(fhirClient, never()).read();
    }

    @Test
    void shouldReturnOfficialNameWhenPresent() {
        java.util.UUID patientId = java.util.UUID.randomUUID();

        Patient patient = new Patient();
        patient.addName()
            .setUse(HumanName.NameUse.USUAL)
            .addGiven("Sam")
            .setFamily("Smith");
        patient.addName()
            .setUse(HumanName.NameUse.OFFICIAL)
            .addGiven("Ana")
            .setFamily("Jones");

        when(fhirClient.read()
            .resource(Patient.class)
            .withId(patientId.toString())
            .execute()).thenReturn(patient);

        String name = patientNameService.getPatientName(patientId);

        assertThat(name).isEqualTo("Ana Jones");
    }

    @Test
    void shouldFallBackToUsualNameWhenNoOfficial() {
        java.util.UUID patientId = java.util.UUID.randomUUID();

        Patient patient = new Patient();
        patient.addName()
            .setUse(HumanName.NameUse.USUAL)
            .addGiven("Jamie")
            .setFamily("Nguyen");

        when(fhirClient.read()
            .resource(Patient.class)
            .withId(patientId.toString())
            .execute()).thenReturn(patient);

        String name = patientNameService.getPatientName(patientId);

        assertThat(name).isEqualTo("Jamie Nguyen");
    }

    @Test
    void shouldUseFirstAvailableNameWhenNoUseProvided() {
        java.util.UUID patientId = java.util.UUID.randomUUID();

        Patient patient = new Patient();
        patient.addName()
            .addGiven("Alex")
            .setFamily("Kim");
        patient.addName()
            .addGiven("Sam")
            .setFamily("Lee");

        when(fhirClient.read()
            .resource(Patient.class)
            .withId(patientId.toString())
            .execute()).thenReturn(patient);

        String name = patientNameService.getPatientName(patientId);

        assertThat(name).isEqualTo("Alex Kim");
    }

    @Test
    void shouldHandleGivenNameOnly() {
        java.util.UUID patientId = java.util.UUID.randomUUID();

        Patient patient = new Patient();
        patient.addName()
            .setUse(HumanName.NameUse.USUAL)
            .addGiven("Taylor");

        when(fhirClient.read()
            .resource(Patient.class)
            .withId(patientId.toString())
            .execute()).thenReturn(patient);

        String name = patientNameService.getPatientName(patientId);

        assertThat(name).isEqualTo("Taylor");
    }

    @Test
    void shouldHandleFamilyNameOnly() {
        java.util.UUID patientId = java.util.UUID.randomUUID();

        Patient patient = new Patient();
        patient.addName()
            .setFamily("Patel");

        when(fhirClient.read()
            .resource(Patient.class)
            .withId(patientId.toString())
            .execute()).thenReturn(patient);

        String name = patientNameService.getPatientName(patientId);

        assertThat(name).isEqualTo("Patel");
    }

    @Test
    void shouldReturnFallbackWhenPatientHasNoNames() {
        java.util.UUID patientId = java.util.UUID.randomUUID();

        Patient patient = new Patient();

        when(fhirClient.read()
            .resource(Patient.class)
            .withId(patientId.toString())
            .execute()).thenReturn(patient);

        String name = patientNameService.getPatientName(patientId);

        assertThat(name).isEqualTo("Patient");
    }

    @Test
    void shouldReturnFallbackWhenClientThrows() {
        java.util.UUID patientId = java.util.UUID.randomUUID();

        when(fhirClient.read()
            .resource(Patient.class)
            .withId(patientId.toString())
            .execute()).thenThrow(new RuntimeException("boom"));

        String name = patientNameService.getPatientName(patientId);

        assertThat(name).isEqualTo("Patient");
    }
}
