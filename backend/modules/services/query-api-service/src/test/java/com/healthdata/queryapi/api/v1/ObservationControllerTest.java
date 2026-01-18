package com.healthdata.queryapi.api.v1;

import com.healthdata.eventsourcing.projection.observation.ObservationProjection;
import com.healthdata.eventsourcing.query.observation.ObservationQueryService;
import com.healthdata.queryapi.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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

@ExtendWith(MockitoExtension.class)
class ObservationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ObservationQueryService observationQueryService;

    private static final String TENANT_ID = "tenant-001";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ObservationController(observationQueryService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void shouldReturnObservations_WhenObservationsExist() throws Exception {
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

        when(observationQueryService.findByPatientAndTenant("patient-123", TENANT_ID))
            .thenReturn(observations);

        mockMvc.perform(get("/api/v1/observations/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].loincCode").value("2345-7"));

        verify(observationQueryService, times(1))
            .findByPatientAndTenant("patient-123", TENANT_ID);
    }

    @Test
    void shouldReturn400_WhenTenantHeaderMissing() throws Exception {
        mockMvc.perform(get("/api/v1/observations/patient/{patientId}", "patient-123"))
            .andExpect(status().isBadRequest());

        verify(observationQueryService, never()).findByPatientAndTenant(anyString(), anyString());
    }

    @Test
    void shouldReturnEmptyList_WhenNoObservations() throws Exception {
        when(observationQueryService.findByPatientAndTenant("patient-456", TENANT_ID))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/observations/patient/{patientId}", "patient-456")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturnObservations_WhenLoincCodeFound() throws Exception {
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

        when(observationQueryService.findByLoincCodeAndTenant("2345-7", TENANT_ID))
            .thenReturn(observations);

        mockMvc.perform(get("/api/v1/observations/loinc/{loincCode}", "2345-7")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        verify(observationQueryService, times(1))
            .findByLoincCodeAndTenant("2345-7", TENANT_ID);
    }

    @Test
    void shouldEnforceTenantIsolation_ForObservations() throws Exception {
        when(observationQueryService.findByPatientAndTenant("patient-123", "tenant-001"))
            .thenReturn(List.of(ObservationProjection.builder()
                .patientId("patient-123")
                .loincCode("2345-7")
                .value(new BigDecimal("95.5"))
                .unit("mg/dL")
                .observationDate(Instant.now())
                .notes("Glucose test")
                .build()));
        when(observationQueryService.findByPatientAndTenant("patient-123", "tenant-002"))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/observations/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, "tenant-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/v1/observations/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, "tenant-002"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
