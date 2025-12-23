package com.healthdata.quality.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("Patient Data Service Tests")
class PatientDataServiceTest {

    @Test
    @DisplayName("Should fetch patient data from FHIR client")
    void shouldFetchPatientData() {
        IGenericClient fhirClient = Mockito.mock(IGenericClient.class, Mockito.RETURNS_DEEP_STUBS);
        PatientDataService service = new PatientDataService(fhirClient);
        UUID patientId = UUID.randomUUID();

        Patient patient = new Patient();
        patient.setId(patientId.toString());
        when(fhirClient.read().resource(Patient.class).withId(patientId.toString()).execute())
            .thenReturn(patient);

        when(fhirClient.search().forResource(Condition.class).where(Mockito.<ICriterion<?>>any())
            .returnBundle(Bundle.class).execute())
            .thenReturn(bundleWith(new Condition()));
        when(fhirClient.search().forResource(Observation.class).where(Mockito.<ICriterion<?>>any())
            .returnBundle(Bundle.class).execute())
            .thenReturn(bundleWith(new Observation()));
        when(fhirClient.search().forResource(Procedure.class).where(Mockito.<ICriterion<?>>any())
            .returnBundle(Bundle.class).execute())
            .thenReturn(bundleWith(new Procedure()));
        when(fhirClient.search().forResource(Encounter.class).where(Mockito.<ICriterion<?>>any())
            .returnBundle(Bundle.class).execute())
            .thenReturn(bundleWith(new Encounter()));
        when(fhirClient.search().forResource(MedicationStatement.class).where(Mockito.<ICriterion<?>>any())
            .returnBundle(Bundle.class).execute())
            .thenReturn(bundleWith(new MedicationStatement()));
        when(fhirClient.search().forResource(Immunization.class).where(Mockito.<ICriterion<?>>any())
            .returnBundle(Bundle.class).execute())
            .thenReturn(bundleWith(new Immunization()));

        var data = service.fetchPatientData(patientId);

        assertThat(data.getPatient()).isEqualTo(patient);
        assertThat(data.getConditions()).hasSize(1);
        assertThat(data.getObservations()).hasSize(1);
        assertThat(data.getProcedures()).hasSize(1);
        assertThat(data.getEncounters()).hasSize(1);
        assertThat(data.getMedicationStatements()).hasSize(1);
        assertThat(data.getImmunizations()).hasSize(1);
    }

    @Test
    @DisplayName("Should return social history observations")
    void shouldReturnSocialHistoryObservations() {
        IGenericClient fhirClient = Mockito.mock(IGenericClient.class, Mockito.RETURNS_DEEP_STUBS);
        PatientDataService service = new PatientDataService(fhirClient);
        UUID patientId = UUID.randomUUID();

        when(fhirClient.search().forResource(Observation.class).where(Mockito.<ICriterion<?>>any())
            .where(Mockito.<ICriterion<?>>any()).returnBundle(Bundle.class).execute())
            .thenReturn(bundleWith(new Observation()));

        List<Observation> observations = service.fetchSocialHistoryObservations("tenant-1", patientId);

        assertThat(observations).hasSize(1);
    }

    @Test
    @DisplayName("Should throw when fetch patient fails")
    void shouldThrowWhenFetchPatientFails() {
        IGenericClient fhirClient = Mockito.mock(IGenericClient.class, Mockito.RETURNS_DEEP_STUBS);
        PatientDataService service = new PatientDataService(fhirClient);
        UUID patientId = UUID.randomUUID();

        when(fhirClient.read().resource(Patient.class).withId(patientId.toString()).execute())
            .thenThrow(new RuntimeException("fail"));

        assertThatThrownBy(() -> service.fetchPatient("tenant-1", patientId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to fetch patient");
    }

    @Test
    @DisplayName("Should throw when fetch patient data fails")
    void shouldThrowWhenFetchPatientDataFails() {
        IGenericClient fhirClient = Mockito.mock(IGenericClient.class, Mockito.RETURNS_DEEP_STUBS);
        PatientDataService service = new PatientDataService(fhirClient);
        UUID patientId = UUID.randomUUID();

        when(fhirClient.read().resource(Patient.class).withId(patientId.toString()).execute())
            .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> service.fetchPatientData(patientId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to fetch patient data");
    }

    @Test
    @DisplayName("Should fetch patient observations for tenant")
    void shouldFetchPatientObservations() {
        IGenericClient fhirClient = Mockito.mock(IGenericClient.class, Mockito.RETURNS_DEEP_STUBS);
        PatientDataService service = new PatientDataService(fhirClient);
        UUID patientId = UUID.randomUUID();

        when(fhirClient.search().forResource(Observation.class).where(Mockito.<ICriterion<?>>any())
            .returnBundle(Bundle.class).execute())
            .thenReturn(bundleWith(new Observation()));

        List<Observation> observations = service.fetchPatientObservations("tenant-1", patientId);

        assertThat(observations).hasSize(1);
    }

    @Test
    @DisplayName("Should fetch patient conditions for tenant")
    void shouldFetchPatientConditions() {
        IGenericClient fhirClient = Mockito.mock(IGenericClient.class, Mockito.RETURNS_DEEP_STUBS);
        PatientDataService service = new PatientDataService(fhirClient);
        UUID patientId = UUID.randomUUID();

        when(fhirClient.search().forResource(Condition.class).where(Mockito.<ICriterion<?>>any())
            .returnBundle(Bundle.class).execute())
            .thenReturn(bundleWith(new Condition()));

        List<Condition> conditions = service.fetchPatientConditions("tenant-1", patientId);

        assertThat(conditions).hasSize(1);
    }

    private Bundle bundleWith(org.hl7.fhir.r4.model.Resource resource) {
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(resource);
        return bundle;
    }
}
