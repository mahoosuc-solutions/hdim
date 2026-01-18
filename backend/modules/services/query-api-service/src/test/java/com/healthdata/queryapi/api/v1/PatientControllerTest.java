package com.healthdata.queryapi.api.v1;

import com.healthdata.eventsourcing.projection.patient.PatientProjection;
import com.healthdata.eventsourcing.query.patient.PatientQueryService;
import com.healthdata.queryapi.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PatientController (Phase 1.8)
 * Tests REST endpoints with multi-tenant isolation and exception handling
 */
@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PatientQueryService patientQueryService;

    private static final String TENANT_ID = "tenant-001";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PatientController(patientQueryService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    // ============ getPatientById Tests ============

    @Test
    void shouldReturnPatient_WhenPatientExists() throws Exception {
        // Given
        PatientProjection projection = PatientProjection.builder()
            .patientId("patient-123")
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1980, 1, 15))
            .mrn("MRN-001")
            .insuranceMemberId("INS-001")
            .build();

        when(patientQueryService.findByIdAndTenant("patient-123", TENANT_ID))
            .thenReturn(Optional.of(projection));

        // When & Then
        mockMvc.perform(get("/api/v1/patients/{patientId}", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.patientId").value("patient-123"))
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Doe"))
            .andExpect(jsonPath("$.dateOfBirth[0]").value(1980))  // Year
            .andExpect(jsonPath("$.dateOfBirth[1]").value(1))     // Month
            .andExpect(jsonPath("$.dateOfBirth[2]").value(15))    // Day
            .andExpect(jsonPath("$.mrn").value("MRN-001"))
            .andExpect(jsonPath("$.insuranceMemberId").value("INS-001"));

        verify(patientQueryService, times(1)).findByIdAndTenant("patient-123", TENANT_ID);
    }

    @Test
    void shouldReturn404_WhenPatientNotFound() throws Exception {
        // Given
        when(patientQueryService.findByIdAndTenant("nonexistent", TENANT_ID))
            .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/patients/{patientId}", "nonexistent")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isNotFound());

        verify(patientQueryService, times(1)).findByIdAndTenant("nonexistent", TENANT_ID);
    }

    @Test
    void shouldReturn400_WhenTenantHeaderMissing() throws Exception {
        // When & Then - No X-Tenant-ID header
        mockMvc.perform(get("/api/v1/patients/{patientId}", "patient-123"))
            .andExpect(status().isBadRequest());

        verify(patientQueryService, never()).findByIdAndTenant(anyString(), anyString());
    }

    @Test
    void shouldReturn400_WhenTenantHeaderEmpty() throws Exception {
        // When & Then - Empty X-Tenant-ID header
        mockMvc.perform(get("/api/v1/patients/{patientId}", "patient-123")
                .header(TENANT_HEADER, ""))
            .andExpect(status().isBadRequest());

        verify(patientQueryService, never()).findByIdAndTenant(anyString(), anyString());
    }

    @Test
    void shouldReturn400_WhenTenantHeaderBlank() throws Exception {
        // When & Then - Whitespace-only X-Tenant-ID header
        mockMvc.perform(get("/api/v1/patients/{patientId}", "patient-123")
                .header(TENANT_HEADER, "   "))
            .andExpect(status().isBadRequest());

        verify(patientQueryService, never()).findByIdAndTenant(anyString(), anyString());
    }

    // ============ getPatientByMrn Tests ============

    @Test
    void shouldReturnPatient_WhenMrnFound() throws Exception {
        // Given
        PatientProjection projection = PatientProjection.builder()
            .patientId("patient-456")
            .firstName("Jane")
            .lastName("Smith")
            .dateOfBirth(LocalDate.of(1985, 5, 20))
            .mrn("MRN-002")
            .insuranceMemberId("INS-002")
            .build();

        when(patientQueryService.findByMrnAndTenant("MRN-002", TENANT_ID))
            .thenReturn(Optional.of(projection));

        // When & Then
        mockMvc.perform(get("/api/v1/patients/mrn/{mrn}", "MRN-002")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.patientId").value("patient-456"))
            .andExpect(jsonPath("$.firstName").value("Jane"))
            .andExpect(jsonPath("$.mrn").value("MRN-002"));

        verify(patientQueryService, times(1)).findByMrnAndTenant("MRN-002", TENANT_ID);
    }

    @Test
    void shouldReturn404_WhenMrnNotFound() throws Exception {
        // Given
        when(patientQueryService.findByMrnAndTenant("NONEXISTENT-MRN", TENANT_ID))
            .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/patients/mrn/{mrn}", "NONEXISTENT-MRN")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isNotFound());

        verify(patientQueryService, times(1)).findByMrnAndTenant("NONEXISTENT-MRN", TENANT_ID);
    }

    // ============ getPatientByInsuranceMemberId Tests ============

    @Test
    void shouldReturnPatient_WhenInsuranceMemberIdFound() throws Exception {
        // Given
        PatientProjection projection = PatientProjection.builder()
            .patientId("patient-789")
            .firstName("Bob")
            .lastName("Johnson")
            .dateOfBirth(LocalDate.of(1975, 3, 10))
            .mrn("MRN-003")
            .insuranceMemberId("INS-999")
            .build();

        when(patientQueryService.findByInsuranceMemberIdAndTenant("INS-999", TENANT_ID))
            .thenReturn(Optional.of(projection));

        // When & Then
        mockMvc.perform(get("/api/v1/patients/insurance/{memberId}", "INS-999")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.patientId").value("patient-789"))
            .andExpect(jsonPath("$.insuranceMemberId").value("INS-999"));

        verify(patientQueryService, times(1))
            .findByInsuranceMemberIdAndTenant("INS-999", TENANT_ID);
    }

    // ============ getAllPatients Tests ============

    @Test
    void shouldReturnPatientList_WhenPatientsExist() throws Exception {
        // Given
        List<PatientProjection> projections = List.of(
            PatientProjection.builder()
                .patientId("patient-1")
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1980, 1, 15))
                .mrn("MRN-001")
                .insuranceMemberId("INS-001")
                .build(),
            PatientProjection.builder()
                .patientId("patient-2")
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.of(1985, 5, 20))
                .mrn("MRN-002")
                .insuranceMemberId("INS-002")
                .build()
        );

        when(patientQueryService.findAllByTenant(TENANT_ID))
            .thenReturn(projections);

        // When & Then
        mockMvc.perform(get("/api/v1/patients")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].patientId").value("patient-1"))
            .andExpect(jsonPath("$[0].firstName").value("John"))
            .andExpect(jsonPath("$[1].patientId").value("patient-2"))
            .andExpect(jsonPath("$[1].firstName").value("Jane"));

        verify(patientQueryService, times(1)).findAllByTenant(TENANT_ID);
    }

    @Test
    void shouldReturnEmptyList_WhenNoPatientsExist() throws Exception {
        // Given
        when(patientQueryService.findAllByTenant(TENANT_ID))
            .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/patients")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(patientQueryService, times(1)).findAllByTenant(TENANT_ID);
    }

    // ============ Multi-tenant Isolation Tests ============

    @Test
    void shouldEnforceTenantIsolation() throws Exception {
        // Given
        PatientProjection projection = PatientProjection.builder()
            .patientId("patient-123")
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1980, 1, 15))
            .mrn("MRN-001")
            .insuranceMemberId("INS-001")
            .build();

        when(patientQueryService.findByIdAndTenant("patient-123", "tenant-001"))
            .thenReturn(Optional.of(projection));
        when(patientQueryService.findByIdAndTenant("patient-123", "tenant-002"))
            .thenReturn(Optional.empty());

        // When & Then - tenant-001 finds patient
        mockMvc.perform(get("/api/v1/patients/{patientId}", "patient-123")
                .header(TENANT_HEADER, "tenant-001"))
            .andExpect(status().isOk());

        // When & Then - tenant-002 does NOT find patient (isolated)
        mockMvc.perform(get("/api/v1/patients/{patientId}", "patient-123")
                .header(TENANT_HEADER, "tenant-002"))
            .andExpect(status().isNotFound());

        verify(patientQueryService, times(1))
            .findByIdAndTenant("patient-123", "tenant-001");
        verify(patientQueryService, times(1))
            .findByIdAndTenant("patient-123", "tenant-002");
    }

    // ============ Response Content-Type Tests ============

    @Test
    void shouldReturnJsonContentType() throws Exception {
        // Given
        PatientProjection projection = PatientProjection.builder()
            .patientId("patient-123")
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1980, 1, 15))
            .mrn("MRN-001")
            .insuranceMemberId("INS-001")
            .build();

        when(patientQueryService.findByIdAndTenant("patient-123", TENANT_ID))
            .thenReturn(Optional.of(projection));

        // When & Then
        mockMvc.perform(get("/api/v1/patients/{patientId}", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"));

        verify(patientQueryService, times(1)).findByIdAndTenant("patient-123", TENANT_ID);
    }
}
