package com.healthdata.caregap.api.v1.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthdata.caregap.api.v1.dto.SimulatedGapClosureRequest;
import com.healthdata.caregap.api.v1.dto.StarDomainSummaryResponse;
import com.healthdata.caregap.api.v1.dto.StarMeasureSummaryResponse;
import com.healthdata.caregap.api.v1.dto.StarRatingResponse;
import com.healthdata.caregap.api.v1.dto.StarRatingSimulationRequest;
import com.healthdata.caregap.api.v1.dto.StarRatingTrendPointResponse;
import com.healthdata.caregap.api.v1.dto.StarRatingTrendResponse;
import com.healthdata.caregap.service.StarsProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("StarRatingController REST API Tests")
class StarRatingControllerTest {

    private static final String BASE_PATH = "/api/v1/star-ratings";
    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String TENANT_A = "tenant-alpha";
    private static final String TENANT_B = "tenant-beta";

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Mock
    private StarsProjectionService starsProjectionService;

    @InjectMocks
    private StarRatingController starRatingController;

    @BeforeEach
    void setUp() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(starRatingController)
            .setMessageConverters(converter)
            .build();
    }

    // ===== GET /current =====

    @Nested
    @DisplayName("GET /current")
    class GetCurrent {

        @Test
        @DisplayName("returns 200 with complete star rating response structure")
        void returnsCurrent() throws Exception {
            StarRatingResponse response = sampleResponse(TENANT_A);
            when(starsProjectionService.getCurrentRating(TENANT_A)).thenReturn(response);

            mockMvc.perform(get(BASE_PATH + "/current")
                    .header(TENANT_HEADER, TENANT_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId", is(TENANT_A)))
                .andExpect(jsonPath("$.overallRating", is(3.75)))
                .andExpect(jsonPath("$.roundedRating", is(4.0)))
                .andExpect(jsonPath("$.measureCount", is(2)))
                .andExpect(jsonPath("$.openGapCount", is(5)))
                .andExpect(jsonPath("$.closedGapCount", is(10)))
                .andExpect(jsonPath("$.qualityBonusEligible", is(true)))
                .andExpect(jsonPath("$.lastTriggerEvent", is("on-demand-read")))
                .andExpect(jsonPath("$.calculatedAt").exists())
                .andExpect(jsonPath("$.domains", hasSize(1)))
                .andExpect(jsonPath("$.domains[0].domain", is("Effectiveness of Care")))
                .andExpect(jsonPath("$.domains[0].domainStars", is(3.75)))
                .andExpect(jsonPath("$.measures", hasSize(1)))
                .andExpect(jsonPath("$.measures[0].measureCode", is("COL")))
                .andExpect(jsonPath("$.measures[0].numerator", is(10)))
                .andExpect(jsonPath("$.measures[0].denominator", is(15)));

            verify(starsProjectionService).getCurrentRating(TENANT_A);
        }

        @Test
        @DisplayName("delegates tenant header to service layer")
        void passesTenantId() throws Exception {
            when(starsProjectionService.getCurrentRating(TENANT_B))
                .thenReturn(sampleResponse(TENANT_B));

            mockMvc.perform(get(BASE_PATH + "/current")
                    .header(TENANT_HEADER, TENANT_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId", is(TENANT_B)));

            verify(starsProjectionService).getCurrentRating(TENANT_B);
        }
    }

    // ===== GET /trend =====

    @Nested
    @DisplayName("GET /trend")
    class GetTrend {

        @Test
        @DisplayName("returns trend with default parameters (12 weeks, WEEKLY)")
        void returnsTrendDefaults() throws Exception {
            StarRatingTrendResponse trend = StarRatingTrendResponse.builder()
                .tenantId(TENANT_A)
                .points(List.of(
                    StarRatingTrendPointResponse.builder()
                        .snapshotDate(LocalDate.of(2026, 3, 1))
                        .granularity("WEEKLY")
                        .overallRating(3.5)
                        .roundedRating(3.5)
                        .openGapCount(8)
                        .closedGapCount(6)
                        .qualityBonusEligible(false)
                        .build(),
                    StarRatingTrendPointResponse.builder()
                        .snapshotDate(LocalDate.of(2026, 3, 8))
                        .granularity("WEEKLY")
                        .overallRating(3.75)
                        .roundedRating(4.0)
                        .openGapCount(6)
                        .closedGapCount(8)
                        .qualityBonusEligible(true)
                        .build()
                ))
                .build();
            when(starsProjectionService.getTrend(TENANT_A, 12, "WEEKLY")).thenReturn(trend);

            mockMvc.perform(get(BASE_PATH + "/trend")
                    .header(TENANT_HEADER, TENANT_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId", is(TENANT_A)))
                .andExpect(jsonPath("$.points", hasSize(2)))
                .andExpect(jsonPath("$.points[0].granularity", is("WEEKLY")))
                .andExpect(jsonPath("$.points[0].overallRating", is(3.5)))
                .andExpect(jsonPath("$.points[1].overallRating", is(3.75)))
                .andExpect(jsonPath("$.points[1].qualityBonusEligible", is(true)));

            verify(starsProjectionService).getTrend(TENANT_A, 12, "WEEKLY");
        }

        @Test
        @DisplayName("passes custom weeks and granularity parameters")
        void passesCustomParams() throws Exception {
            StarRatingTrendResponse trend = StarRatingTrendResponse.builder()
                .tenantId(TENANT_A)
                .points(List.of())
                .build();
            when(starsProjectionService.getTrend(TENANT_A, 4, "MONTHLY")).thenReturn(trend);

            mockMvc.perform(get(BASE_PATH + "/trend")
                    .header(TENANT_HEADER, TENANT_A)
                    .param("weeks", "4")
                    .param("granularity", "MONTHLY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId", is(TENANT_A)))
                .andExpect(jsonPath("$.points", hasSize(0)));

            verify(starsProjectionService).getTrend(TENANT_A, 4, "MONTHLY");
        }
    }

    // ===== POST /simulate =====

    @Nested
    @DisplayName("POST /simulate")
    class Simulate {

        @Test
        @DisplayName("returns simulation result with valid closure request")
        void returnsSimulation() throws Exception {
            StarRatingResponse simulated = sampleResponse(TENANT_A);
            when(starsProjectionService.simulate(eq(TENANT_A), any(StarRatingSimulationRequest.class)))
                .thenReturn(simulated);

            SimulatedGapClosureRequest closure = new SimulatedGapClosureRequest();
            closure.setGapCode("COL");
            closure.setClosures(5);

            StarRatingSimulationRequest request = new StarRatingSimulationRequest();
            request.setClosures(List.of(closure));

            mockMvc.perform(post(BASE_PATH + "/simulate")
                    .header(TENANT_HEADER, TENANT_A)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId", is(TENANT_A)))
                .andExpect(jsonPath("$.overallRating", is(3.75)))
                .andExpect(jsonPath("$.measures", hasSize(1)));

            verify(starsProjectionService).simulate(eq(TENANT_A), any(StarRatingSimulationRequest.class));
        }

        @Test
        @DisplayName("accepts multiple gap closure requests in a single simulation")
        void acceptsMultipleClosures() throws Exception {
            when(starsProjectionService.simulate(eq(TENANT_A), any(StarRatingSimulationRequest.class)))
                .thenReturn(sampleResponse(TENANT_A));

            SimulatedGapClosureRequest colClosure = new SimulatedGapClosureRequest();
            colClosure.setGapCode("COL");
            colClosure.setClosures(5);

            SimulatedGapClosureRequest cbpClosure = new SimulatedGapClosureRequest();
            cbpClosure.setGapCode("CBP");
            cbpClosure.setClosures(3);

            StarRatingSimulationRequest request = new StarRatingSimulationRequest();
            request.setClosures(List.of(colClosure, cbpClosure));

            mockMvc.perform(post(BASE_PATH + "/simulate")
                    .header(TENANT_HEADER, TENANT_A)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        }
    }

    // ===== Multi-Tenant Isolation =====

    @Nested
    @DisplayName("Multi-Tenant Isolation")
    class MultiTenant {

        @Test
        @DisplayName("different tenants receive independent star ratings")
        void isolatesByTenant() throws Exception {
            StarRatingResponse responseA = StarRatingResponse.builder()
                .tenantId(TENANT_A)
                .overallRating(4.5)
                .roundedRating(4.5)
                .measureCount(3)
                .openGapCount(2)
                .closedGapCount(15)
                .qualityBonusEligible(true)
                .lastTriggerEvent("on-demand-read")
                .calculatedAt(Instant.now())
                .domains(List.of())
                .measures(List.of())
                .build();

            StarRatingResponse responseB = StarRatingResponse.builder()
                .tenantId(TENANT_B)
                .overallRating(2.0)
                .roundedRating(2.0)
                .measureCount(1)
                .openGapCount(20)
                .closedGapCount(3)
                .qualityBonusEligible(false)
                .lastTriggerEvent("on-demand-read")
                .calculatedAt(Instant.now())
                .domains(List.of())
                .measures(List.of())
                .build();

            when(starsProjectionService.getCurrentRating(TENANT_A)).thenReturn(responseA);
            when(starsProjectionService.getCurrentRating(TENANT_B)).thenReturn(responseB);

            mockMvc.perform(get(BASE_PATH + "/current")
                    .header(TENANT_HEADER, TENANT_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId", is(TENANT_A)))
                .andExpect(jsonPath("$.overallRating", is(4.5)))
                .andExpect(jsonPath("$.qualityBonusEligible", is(true)));

            mockMvc.perform(get(BASE_PATH + "/current")
                    .header(TENANT_HEADER, TENANT_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId", is(TENANT_B)))
                .andExpect(jsonPath("$.overallRating", is(2.0)))
                .andExpect(jsonPath("$.qualityBonusEligible", is(false)));
        }
    }

    // --- helpers ---

    private StarRatingResponse sampleResponse(String tenantId) {
        return StarRatingResponse.builder()
            .tenantId(tenantId)
            .overallRating(3.75)
            .roundedRating(4.0)
            .measureCount(2)
            .openGapCount(5)
            .closedGapCount(10)
            .qualityBonusEligible(true)
            .lastTriggerEvent("on-demand-read")
            .calculatedAt(Instant.now())
            .domains(List.of(
                StarDomainSummaryResponse.builder()
                    .domain("Effectiveness of Care")
                    .domainStars(3.75)
                    .measureCount(2)
                    .averagePerformanceRate(0.67)
                    .build()
            ))
            .measures(List.of(
                StarMeasureSummaryResponse.builder()
                    .measureCode("COL")
                    .measureName("Colorectal Cancer Screening")
                    .domain("Effectiveness of Care")
                    .numerator(10)
                    .denominator(15)
                    .performanceRate(0.6667)
                    .stars(3)
                    .build()
            ))
            .build();
    }
}
