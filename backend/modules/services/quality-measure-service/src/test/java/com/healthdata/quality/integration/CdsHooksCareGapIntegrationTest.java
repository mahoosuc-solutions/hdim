package com.healthdata.quality.integration;

import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import com.healthdata.quality.config.TestMessagingConfiguration;
import com.healthdata.quality.config.TestWebSocketConfiguration;
import com.healthdata.quality.dto.CdsRecommendationDTO;
import com.healthdata.quality.service.CdsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@Tag("e2e")
@SpringBootTest
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@Import({TestMessagingConfiguration.class, TestWebSocketConfiguration.class})
@DisplayName("CDS Hooks Care Gap Integration/E2E Tests")
class CdsHooksCareGapIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CdsService cdsService;

    @Test
    @DisplayName("Patient-view should return deterministic care gap card")
    void patientViewHappyPath() throws Exception {
        when(cdsService.getActiveRecommendations(eq(CareGapFixture.TENANT_ID), eq(CareGapFixture.PATIENT_ID)))
            .thenReturn(List.of(CareGapFixture.deterministicRecommendation()));

        mockMvc.perform(post("/cds-services/patient-view")
                .header("X-Tenant-ID", CareGapFixture.TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "hook": "patient-view",
                      "context": {
                        "patientId": "%s"
                      }
                    }
                    """.formatted(CareGapFixture.PATIENT_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cards.length()").value(1))
            .andExpect(jsonPath("$.cards[0].summary").value("Close diabetes gap"))
            .andExpect(jsonPath("$.cards[0].indicator").value("critical"))
            .andExpect(jsonPath("$.cards[0].links.length()").value(1));

        verify(cdsService).getActiveRecommendations(CareGapFixture.TENANT_ID, CareGapFixture.PATIENT_ID);
    }

    @Test
    @DisplayName("Patient-view should return empty cards when no care gaps")
    void patientViewEmptyPath() throws Exception {
        when(cdsService.getActiveRecommendations(eq(CareGapFixture.TENANT_ID), eq(CareGapFixture.PATIENT_ID)))
            .thenReturn(List.of());

        mockMvc.perform(post("/cds-services/patient-view")
                .header("X-Tenant-ID", CareGapFixture.TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "hook": "patient-view",
                      "context": {
                        "patientId": "%s"
                      }
                    }
                    """.formatted(CareGapFixture.PATIENT_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cards.length()").value(0));
    }

    @Test
    @DisplayName("Patient-view should return warning card on invalid request")
    void patientViewErrorPath() throws Exception {
        mockMvc.perform(post("/cds-services/patient-view")
                .header("X-Tenant-ID", CareGapFixture.TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "hook": "patient-view",
                      "context": {}
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.cards.length()").value(1))
            .andExpect(jsonPath("$.cards[0].indicator").value("warning"));
    }

    private static final class CareGapFixture {
        private static final String TENANT_ID = "acme-health";
        private static final UUID PATIENT_ID = UUID.fromString("f0734be3-30bd-4f23-b57c-125145fd95f0");

        private static CdsRecommendationDTO deterministicRecommendation() {
            return CdsRecommendationDTO.builder()
                .title("Close diabetes gap")
                .description("HbA1c is overdue for this patient.")
                .urgency("URGENT")
                .dueDate(Instant.parse("2026-03-01T00:00:00Z"))
                .build();
        }
    }
}
