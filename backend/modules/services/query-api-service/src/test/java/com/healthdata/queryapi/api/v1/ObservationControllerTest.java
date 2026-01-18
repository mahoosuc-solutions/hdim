package com.healthdata.queryapi.api.v1;

import com.healthdata.eventsourcing.projection.observation.ObservationProjection;
import com.healthdata.eventsourcing.query.observation.ObservationQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ObservationController (Phase 1.8)
 * Tests REST endpoints for time-series observations with LOINC filtering
 */
@WebMvcTest(ObservationController.class)
class ObservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ObservationQueryService observationQueryService;

    private static final String TENANT_ID = "tenant-001";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    // ============ getObservationsByPatient Tests ============

    @Test
    void shouldReturnObservations_WhenObservationsExistForPatient() throws Exception {
        // Given
        List<ObservationProjection> observations = List.of(
            ObservationProjection.builder()
                .patientId("patient-123")
                .loincCode("2345-7")
                .value(new BigDecimal("95.5"))
                .unit("mg/dL")
                .observationDate(Instant.now())
                .notes("Fasting glucose test")
                .build(),
            ObservationProjection.builder()
                .patientId("patient-123")
                .loincCode("2571-8")
                .value(new BigDecimal("1.2"))
                .unit("mg/dL")
                .observationDate(Instant.now())
                .notes("Creatinine test")
                .build()
        );

        when(observationQueryService.findByPatientAndTenant("patient-123", TENANT_ID))
            .thenReturn(observations);

        // When & Then
        mockMvc.perform(get("/api/v1/observations/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].patientId").value("patient-123"))
            .andExpect(jsonPath("$[0].loincCode").value("2345-7"))
            .andExpect(jsonPath("$[0].value").value(95.5))
            .andExpect(jsonPath("$[1].loincCode").value("2571-8"));

        verify(observationQueryService, times(1))
            .findByPatientAndTenant("patient-123", TENANT_ID);
    }

    @Test
    void shouldReturnEmptyList_WhenNoObservationsForPatient() throws Exception {
        // Given
        when(observationQueryService.findByPatientAndTenant("patient-456", TENANT_ID))
            .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/observations/patient/{patientId}", "patient-456")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(observationQueryService, times(1))
            .findByPatientAndTenant("patient-456", TENANT_ID);
    }

    @Test
    void shouldReturn400_WhenTenantHeaderMissingForPatientObservations() throws Exception {
        // When & Then - No X-Tenant-ID header
        mockMvc.perform(get("/api/v1/observations/patient/{patientId}", "patient-123"))
            .andExpect(status().isBadRequest());

        verify(observationQueryService, never())
            .findByPatientAndTenant(anyString(), anyString());
    }

    // ============ getObservationsByLoincCode Tests ============

    @Test
    void shouldReturnObservations_WhenLoincCodeFound() throws Exception {
        // Given
        List<ObservationProjection> observations = List.of(
            ObservationProjection.builder()
                .patientId("patient-123")
                .loincCode("2345-7")
                .value(new BigDecimal("95.5"))
                .unit("mg/dL")
                .observationDate(Instant.now())
                .notes("Glucose test")
                .build(),
            ObservationProjection.builder()
                .patientId("patient-456")
                .loincCode("2345-7")
                .value(new BigDecimal("110.0"))
                .unit("mg/dL")
                .observationDate(Instant.now())
                .notes("Glucose test")
                .build()
        );

        when(observationQueryService.findByLoincCodeAndTenant("2345-7", TENANT_ID))
            .thenReturn(observations);

        // When & Then
        mockMvc.perform(get("/api/v1/observations/loinc/{loincCode}", "2345-7")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].loincCode").value(containsInAnyOrder("2345-7", "2345-7")));

        verify(observationQueryService, times(1))
            .findByLoincCodeAndTenant("2345-7", TENANT_ID);
    }

    @Test
    void shouldReturnEmptyList_WhenLoincCodeNotFound() throws Exception {
        // Given
        when(observationQueryService.findByLoincCodeAndTenant("9999-9", TENANT_ID))
            .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/observations/loinc/{loincCode}", "9999-9")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(observationQueryService, times(1))
            .findByLoincCodeAndTenant("9999-9", TENANT_ID);
    }

    // ============ getLatestObservation Tests ============

    @Test
    void shouldReturnLatestObservation_WhenObservationExists() throws Exception {
        // Given
        Instant now = Instant.now();
        ObservationProjection observation = ObservationProjection.builder()
            .patientId("patient-123")
            .loincCode("2345-7")
            .value(new BigDecimal("95.5"))
            .unit("mg/dL")
            .observationDate(now)
            .notes("Latest glucose test")
            .build();

        when(observationQueryService.findLatestByLoincAndPatient("patient-123", "2345-7", TENANT_ID))
            .thenReturn(Optional.of(observation));

        // When & Then
        mockMvc.perform(get("/api/v1/observations/patient/{patientId}/latest", "patient-123")
                .param("loincCode", "2345-7")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.patientId").value("patient-123"))
            .andExpect(jsonPath("$.loincCode").value("2345-7"))
            .andExpect(jsonPath("$.value").value(95.5))
            .andExpect(jsonPath("$.notes").value("Latest glucose test"));

        verify(observationQueryService, times(1))
            .findLatestByLoincAndPatient("patient-123", "2345-7", TENANT_ID);
    }

    @Test
    void shouldReturn404_WhenLatestObservationNotFound() throws Exception {
        // Given
        when(observationQueryService.findLatestByLoincAndPatient("patient-123", "9999-9", TENANT_ID))
            .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/observations/patient/{patientId}/latest", "patient-123")
                .param("loincCode", "9999-9")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isNotFound());

        verify(observationQueryService, times(1))
            .findLatestByLoincAndPatient("patient-123", "9999-9", TENANT_ID);
    }

    @Test
    void shouldReturn400_WhenLoincCodeParameterMissing() throws Exception {
        // When & Then - Missing loincCode parameter
        mockMvc.perform(get("/api/v1/observations/patient/{patientId}/latest", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isBadRequest());

        verify(observationQueryService, never())
            .findLatestByLoincAndPatient(anyString(), anyString(), anyString());
    }

    // ============ getObservationsByDateRange Tests ============

    @Test
    void shouldReturnObservations_WhenObservationsInDateRange() throws Exception {
        // Given
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);
        List<ObservationProjection> observations = List.of(
            ObservationProjection.builder()
                .patientId("patient-123")
                .loincCode("2345-7")
                .value(new BigDecimal("95.5"))
                .unit("mg/dL")
                .observationDate(Instant.now())
                .notes("Glucose test")
                .build()
        );

        when(observationQueryService.findByDateRange(TENANT_ID, start, end))
            .thenReturn(observations);

        // When & Then
        mockMvc.perform(get("/api/v1/observations/date-range")
                .param("start", "2024-01-01")
                .param("end", "2024-12-31")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].patientId").value("patient-123"));

        verify(observationQueryService, times(1))
            .findByDateRange(TENANT_ID, start, end);
    }

    @Test
    void shouldReturnEmptyList_WhenNoObservationsInDateRange() throws Exception {
        // Given
        LocalDate start = LocalDate.of(2020, 1, 1);
        LocalDate end = LocalDate.of(2020, 12, 31);

        when(observationQueryService.findByDateRange(TENANT_ID, start, end))
            .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/observations/date-range")
                .param("start", "2020-01-01")
                .param("end", "2020-12-31")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(observationQueryService, times(1))
            .findByDateRange(TENANT_ID, start, end);
    }

    @Test
    void shouldReturn400_WhenStartDateMissing() throws Exception {
        // When & Then - Missing start parameter
        mockMvc.perform(get("/api/v1/observations/date-range")
                .param("end", "2024-12-31")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isBadRequest());

        verify(observationQueryService, never())
            .findByDateRange(anyString(), any(), any());
    }

    @Test
    void shouldReturn400_WhenEndDateMissing() throws Exception {
        // When & Then - Missing end parameter
        mockMvc.perform(get("/api/v1/observations/date-range")
                .param("start", "2024-01-01")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isBadRequest());

        verify(observationQueryService, never())
            .findByDateRange(anyString(), any(), any());
    }

    // ============ getAllObservations Tests ============

    @Test
    void shouldReturnEmptyList_WhenGettingAllObservations() throws Exception {
        // Note: getAllObservations is not fully implemented in controller (returns empty list)
        // When & Then
        mockMvc.perform(get("/api/v1/observations")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        // Service method not called since controller returns hardcoded empty list
        verify(observationQueryService, never())
            .findByPatientAndTenant(anyString(), anyString());
    }

    // ============ Multi-tenant Isolation Tests ============

    @Test
    void shouldEnforceTenantIsolation_ForObservations() throws Exception {
        // Given
        List<ObservationProjection> tenant1Obs = List.of(
            ObservationProjection.builder()
                .patientId("patient-123")
                .loincCode("2345-7")
                .value(new BigDecimal("95.5"))
                .unit("mg/dL")
                .observationDate(Instant.now())
                .notes("Glucose test")
                .build()
        );

        when(observationQueryService.findByPatientAndTenant("patient-123", "tenant-001"))
            .thenReturn(tenant1Obs);
        when(observationQueryService.findByPatientAndTenant("patient-123", "tenant-002"))
            .thenReturn(List.of());

        // When & Then - tenant-001 sees observation
        mockMvc.perform(get("/api/v1/observations/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, "tenant-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        // When & Then - tenant-002 does NOT see observation (isolated)
        mockMvc.perform(get("/api/v1/observations/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, "tenant-002"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(observationQueryService)
            .findByPatientAndTenant("patient-123", "tenant-001");
        verify(observationQueryService)
            .findByPatientAndTenant("patient-123", "tenant-002");
    }

    // ============ Response Field Validation Tests ============

    @Test
    void shouldIncludeAllFields_InObservationResponse() throws Exception {
        // Given
        Instant observationTime = Instant.now();
        ObservationProjection observation = ObservationProjection.builder()
            .patientId("patient-123")
            .loincCode("2345-7")
            .value(new BigDecimal("95.5"))
            .unit("mg/dL")
            .observationDate(observationTime)
            .notes("Glucose test")
            .build();

        when(observationQueryService.findByPatientAndTenant("patient-123", TENANT_ID))
            .thenReturn(List.of(observation));

        // When & Then
        mockMvc.perform(get("/api/v1/observations/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].patientId").exists())
            .andExpect(jsonPath("$[0].loincCode").exists())
            .andExpect(jsonPath("$[0].value").exists())
            .andExpect(jsonPath("$[0].unit").exists())
            .andExpect(jsonPath("$[0].observationDate").exists())
            .andExpect(jsonPath("$[0].notes").exists());

        verify(observationQueryService, times(1))
            .findByPatientAndTenant("patient-123", TENANT_ID);
    }

    // ============ Content-Type Tests ============

    @Test
    void shouldReturnJsonContentType_ForObservations() throws Exception {
        // Given
        when(observationQueryService.findByPatientAndTenant("patient-123", TENANT_ID))
            .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/observations/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"));

        verify(observationQueryService, times(1))
            .findByPatientAndTenant("patient-123", TENANT_ID);
    }
}
