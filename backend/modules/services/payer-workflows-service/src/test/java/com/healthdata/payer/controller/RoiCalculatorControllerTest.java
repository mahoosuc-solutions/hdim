package com.healthdata.payer.controller;

import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import com.healthdata.payer.dto.RoiCalculationRequest;
import com.healthdata.payer.dto.RoiCalculationResponse;
import com.healthdata.payer.service.RoiCalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.healthdata.payer.service.RoiPdfExportService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@DisplayName("ROI Calculator Controller Tests")
@Tag("integration")
class RoiCalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoiCalculationService roiCalculationService;

    @MockBean
    private RoiPdfExportService roiPdfExportService;

    private static final String TENANT_ID = "hdim-test";

    // ===== POST /api/v1/payer/roi/calculate =====

    @Test
    @DisplayName("POST /calculate - Should return 200 with ROI results")
    void shouldCalculateROI() throws Exception {
        RoiCalculationResponse response = RoiCalculationResponse.builder()
                .orgType("ACO")
                .patientPopulation(25000)
                .currentQualityScore(new BigDecimal("70.0"))
                .currentStarRating(new BigDecimal("3.5"))
                .manualReportingHours(40)
                .qualityImprovement(new BigDecimal("17.5"))
                .projectedScore(new BigDecimal("87.5"))
                .totalYear1Value(new BigDecimal("9862245"))
                .year1Investment(new BigDecimal("36000"))
                .year1ROI(new BigDecimal("27295"))
                .paybackDays(new BigDecimal("1"))
                .threeYearNPV(new BigDecimal("28000000"))
                .build();

        when(roiCalculationService.calculate(any(), any())).thenReturn(response);

        String requestBody = objectMapper.writeValueAsString(
                RoiCalculationRequest.builder()
                        .orgType("ACO")
                        .patientPopulation(25000)
                        .currentQualityScore(70.0)
                        .currentStarRating(3.5)
                        .manualReportingHours(40)
                        .save(false)
                        .build()
        );

        mockMvc.perform(post("/api/v1/payer/roi/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orgType").value("ACO"))
                .andExpect(jsonPath("$.totalYear1Value").value(9862245))
                .andExpect(jsonPath("$.year1Investment").value(36000))
                .andExpect(jsonPath("$.year1ROI").value(27295));
    }

    @Test
    @DisplayName("POST /calculate - Should work without tenant header (public)")
    void shouldCalculateWithoutTenantHeader() throws Exception {
        when(roiCalculationService.calculate(any(), eq(null)))
                .thenReturn(RoiCalculationResponse.builder()
                        .orgType("ACO")
                        .totalYear1Value(new BigDecimal("1000000"))
                        .build());

        String requestBody = objectMapper.writeValueAsString(
                RoiCalculationRequest.builder()
                        .orgType("ACO")
                        .patientPopulation(25000)
                        .currentQualityScore(70.0)
                        .currentStarRating(3.5)
                        .manualReportingHours(40)
                        .build()
        );

        mockMvc.perform(post("/api/v1/payer/roi/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    // ===== GET /api/v1/payer/roi/{id} =====

    @Test
    @DisplayName("GET /{id} - Should return saved calculation")
    void shouldReturnSavedCalculation() throws Exception {
        RoiCalculationResponse response = RoiCalculationResponse.builder()
                .id("test-uuid-123")
                .orgType("PAYER")
                .totalYear1Value(new BigDecimal("5000000"))
                .shareUrl("/api/v1/payer/roi/test-uuid-123")
                .createdAt(Instant.now())
                .build();

        when(roiCalculationService.getById("test-uuid-123")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/v1/payer/roi/test-uuid-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-uuid-123"))
                .andExpect(jsonPath("$.shareUrl").value("/api/v1/payer/roi/test-uuid-123"));
    }

    @Test
    @DisplayName("GET /{id} - Should return 404 for non-existent calculation")
    void shouldReturn404ForMissingCalculation() throws Exception {
        when(roiCalculationService.getById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/payer/roi/nonexistent"))
                .andExpect(status().isNotFound());
    }

    // ===== GET /api/v1/payer/roi/recent =====

    @Test
    @DisplayName("POST /calculate - Should return 400 for invalid org type")
    void shouldReturn400ForInvalidOrgType() throws Exception {
        when(roiCalculationService.calculate(any(), any()))
                .thenThrow(new IllegalArgumentException("Unknown organization type: BOGUS"));

        String requestBody = objectMapper.writeValueAsString(
                RoiCalculationRequest.builder()
                        .orgType("BOGUS")
                        .patientPopulation(25000)
                        .currentQualityScore(70.0)
                        .currentStarRating(3.5)
                        .manualReportingHours(40)
                        .build()
        );

        mockMvc.perform(post("/api/v1/payer/roi/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /calculate - Should return 400 for missing required fields")
    void shouldReturn400ForMissingFields() throws Exception {
        // Empty body - all required fields missing
        mockMvc.perform(post("/api/v1/payer/roi/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ===== GET /api/v1/payer/roi/recent =====

    @Test
    @DisplayName("GET /recent - Should return paginated recent calculations")
    void shouldReturnRecentCalculations() throws Exception {
        Page<RoiCalculationResponse> page = new PageImpl<>(
                List.of(
                        RoiCalculationResponse.builder().id("1").orgType("ACO").build(),
                        RoiCalculationResponse.builder().id("2").orgType("PAYER").build()
                ),
                PageRequest.of(0, 20), 2
        );

        when(roiCalculationService.getRecent(eq(TENANT_ID), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/payer/roi/recent")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value("1"));
    }
}
