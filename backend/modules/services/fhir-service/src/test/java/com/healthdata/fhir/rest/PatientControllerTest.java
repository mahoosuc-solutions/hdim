package com.healthdata.fhir.rest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.healthdata.fhir.service.PatientService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ExtendWith(MockitoExtension.class)
@DisplayName("Patient Controller Tests")
@Tag("integration")
class PatientControllerTest {

    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @Mock
    private PatientService patientService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PatientController controller = new PatientController(patientService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Should create patient with default tenant")
    void shouldCreatePatientWithDefaultTenant() throws Exception {
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID().toString());
        when(patientService.createPatient(eq("tenant-1"), any(Patient.class), eq("admin-portal")))
                .thenReturn(patient);

        mockMvc.perform(post("/fhir/Patient")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(patient)))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString(patient.getId())));
    }

    @Test
    @DisplayName("Should return not found when patient missing")
    void shouldReturnNotFoundWhenPatientMissing() throws Exception {
        UUID patientId = UUID.randomUUID();
        when(patientService.getPatient("tenant-1", patientId.toString()))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/fhir/Patient/{id}", patientId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return bad request on validation error")
    void shouldReturnBadRequestOnValidationError() throws Exception {
        when(patientService.createPatient(eq("tenant-1"), any(Patient.class), eq("admin-portal")))
                .thenThrow(new PatientService.PatientValidationException("invalid"));

        mockMvc.perform(post("/fhir/Patient")
                        .contentType("application/fhir+json")
                        .content("{\"resourceType\":\"Patient\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("invalid")));
    }

    @Test
    @DisplayName("Should return not found on update when missing")
    void shouldReturnNotFoundOnUpdateWhenMissing() throws Exception {
        UUID patientId = UUID.randomUUID();
        when(patientService.updatePatient(eq("tenant-1"), eq(patientId.toString()), any(Patient.class), eq("admin-portal")))
                .thenThrow(new PatientService.PatientNotFoundException("missing"));

        mockMvc.perform(put("/fhir/Patient/{id}", patientId)
                        .contentType("application/fhir+json")
                        .content("{\"resourceType\":\"Patient\",\"id\":\"" + patientId + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should prefer family filter when both are provided")
    void shouldPreferFamilyFilterWhenBothProvided() throws Exception {
        when(patientService.searchPatients(eq("tenant-1"), any(), eq(20)))
                .thenReturn(new org.hl7.fhir.r4.model.Bundle());

        mockMvc.perform(get("/fhir/Patient")
                        .param("family", "Chen")
                        .param("name", "Maya"))
                .andExpect(status().isOk());

        ArgumentCaptor<String> filterCaptor = ArgumentCaptor.forClass(String.class);
        verify(patientService).searchPatients(eq("tenant-1"), filterCaptor.capture(), eq(20));
        org.assertj.core.api.Assertions.assertThat(filterCaptor.getValue()).isEqualTo("Chen");
    }
}
