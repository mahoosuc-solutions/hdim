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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.healthdata.fhir.service.FhirEverythingService;
import com.healthdata.fhir.service.PatientService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ExtendWith(MockitoExtension.class)
@DisplayName("Patient Controller Tests")
class PatientControllerTest {

    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @Mock
    private PatientService patientService;

    @Mock
    private FhirEverythingService everythingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PatientController controller = new PatientController(patientService, everythingService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Should create patient with default tenant")
    void shouldCreatePatientWithDefaultTenant() throws Exception {
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID().toString());
        when(patientService.createPatient(eq("tenant-1"), any(Patient.class), eq("admin-portal")))
                .thenReturn(patient);

        mockMvc.perform(post("/Patient")
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

        mockMvc.perform(get("/Patient/{id}", patientId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return bad request on validation error")
    void shouldReturnBadRequestOnValidationError() throws Exception {
        when(patientService.createPatient(eq("tenant-1"), any(Patient.class), eq("admin-portal")))
                .thenThrow(new PatientService.PatientValidationException("invalid"));

        mockMvc.perform(post("/Patient")
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

        mockMvc.perform(put("/Patient/{id}", patientId)
                        .contentType("application/fhir+json")
                        .content("{\"resourceType\":\"Patient\",\"id\":\"" + patientId + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should prefer family filter when both are provided")
    void shouldPreferFamilyFilterWhenBothProvided() throws Exception {
        when(patientService.searchPatients(eq("tenant-1"), any(), eq(20)))
                .thenReturn(new org.hl7.fhir.r4.model.Bundle());

        mockMvc.perform(get("/Patient")
                        .param("family", "Chen")
                        .param("name", "Maya"))
                .andExpect(status().isOk());

        ArgumentCaptor<String> filterCaptor = ArgumentCaptor.forClass(String.class);
        verify(patientService).searchPatients(eq("tenant-1"), filterCaptor.capture(), eq(20));
        org.assertj.core.api.Assertions.assertThat(filterCaptor.getValue()).isEqualTo("Chen");
    }

    // ─── Patient/$everything ──────────────────────────────────────────────────

    @Test
    @DisplayName("$everything returns collection bundle with all patient resources")
    void everythingReturnsBundleForKnownPatient() throws Exception {
        UUID patientId = UUID.randomUUID();

        org.hl7.fhir.r4.model.Bundle expectedBundle = new org.hl7.fhir.r4.model.Bundle();
        expectedBundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.COLLECTION);
        Patient patient = new Patient();
        patient.setId(patientId.toString());
        org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry = new org.hl7.fhir.r4.model.Bundle.BundleEntryComponent();
        entry.setResource(patient);
        expectedBundle.addEntry(entry);
        expectedBundle.setTotal(1);

        when(everythingService.getEverything(eq("tenant-1"), eq(patientId.toString())))
                .thenReturn(expectedBundle);

        mockMvc.perform(get("/Patient/{id}/$everything", patientId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"resourceType\":\"Bundle\"")))
                .andExpect(content().string(containsString("collection")));
    }

    @Test
    @DisplayName("$everything returns 404 when patient not found")
    void everythingReturnsNotFoundForUnknownPatient() throws Exception {
        UUID patientId = UUID.randomUUID();

        when(everythingService.getEverything(eq("tenant-1"), eq(patientId.toString())))
                .thenThrow(new PatientService.PatientNotFoundException(patientId.toString()));

        mockMvc.perform(get("/Patient/{id}/$everything", patientId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("$everything respects tenant header for multi-tenant isolation")
    void everythingRespectsExplicitTenantHeader() throws Exception {
        UUID patientId = UUID.randomUUID();
        String tenantB = "tenant-b";

        org.hl7.fhir.r4.model.Bundle bundle = new org.hl7.fhir.r4.model.Bundle();
        bundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.COLLECTION);
        bundle.setTotal(0);

        when(everythingService.getEverything(eq(tenantB), eq(patientId.toString())))
                .thenReturn(bundle);

        mockMvc.perform(get("/Patient/{id}/$everything", patientId)
                        .header("X-Tenant-Id", tenantB))
                .andExpect(status().isOk());

        verify(everythingService).getEverything(eq(tenantB), eq(patientId.toString()));
    }
}
