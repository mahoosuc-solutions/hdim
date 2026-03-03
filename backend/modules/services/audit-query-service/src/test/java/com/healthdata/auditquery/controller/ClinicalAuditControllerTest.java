package com.healthdata.auditquery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.auditquery.dto.clinical.ClinicalDecisionResponse;
import com.healthdata.auditquery.dto.clinical.ClinicalReviewRequest;
import com.healthdata.auditquery.exception.GlobalExceptionHandler;
import com.healthdata.auditquery.service.ClinicalAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("ClinicalAuditController Unit Tests")
class ClinicalAuditControllerTest {

    private static final String TENANT_ID = "test-tenant-audit";
    private static final String TEST_USER = "test-user";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TestingAuthenticationToken AUTH = new TestingAuthenticationToken(TEST_USER, null, "ROLE_ADMIN");

    private MockMvc mockMvc;

    @Mock
    private ClinicalAuditService clinicalAuditService;

    @InjectMocks
    private ClinicalAuditController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/audit/ai/decisions")
    class GetClinicalDecisions {

        @Test
        @DisplayName("Should return 200 with paginated clinical decisions")
        void getClinicalDecisions_shouldReturn200() throws Exception {
            // Given
            ClinicalDecisionResponse decision = ClinicalDecisionResponse.builder()
                    .id(UUID.randomUUID())
                    .patientId("PAT-12345")
                    .decisionType("MEDICATION_ALERT")
                    .alertSeverity("HIGH")
                    .reviewStatus("PENDING")
                    .confidenceScore(0.87)
                    .build();
            Page<ClinicalDecisionResponse> page = new PageImpl<>(List.of(decision), PageRequest.of(0, 20), 1);

            when(clinicalAuditService.getClinicalDecisions(
                    eq(TENANT_ID), isNull(), isNull(), isNull(), isNull(),
                    isNull(), isNull(), eq(0), eq(20)))
                    .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/v1/audit/ai/decisions")
                            .header("X-Tenant-ID", TENANT_ID)
                            .principal(AUTH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].patientId").value("PAT-12345"))
                    .andExpect(jsonPath("$.content[0].decisionType").value("MEDICATION_ALERT"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/audit/clinical/decisions/{id}/accept")
    class AcceptRecommendation {

        @Test
        @DisplayName("Should return 200 when recommendation is accepted")
        void acceptRecommendation_shouldReturn200() throws Exception {
            // Given
            UUID decisionId = UUID.randomUUID();
            ClinicalReviewRequest request = ClinicalReviewRequest.builder()
                    .clinicalNotes("Recommendation validated against current guidelines")
                    .build();
            ClinicalDecisionResponse response = ClinicalDecisionResponse.builder()
                    .id(decisionId)
                    .reviewStatus("APPROVED")
                    .reviewedBy("test-user")
                    .build();

            when(clinicalAuditService.acceptRecommendation(
                    eq(TENANT_ID), eq(decisionId), any(ClinicalReviewRequest.class), eq(TEST_USER)))
                    .thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/v1/audit/clinical/decisions/{id}/accept", decisionId)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request))
                            .principal(AUTH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reviewStatus").value("APPROVED"));
        }

        @Test
        @DisplayName("Should return 400 when X-Tenant-ID header is missing")
        void acceptRecommendation_shouldReturn400_WhenMissingTenantHeader() throws Exception {
            // Given
            UUID decisionId = UUID.randomUUID();
            ClinicalReviewRequest request = ClinicalReviewRequest.builder()
                    .clinicalNotes("Some notes")
                    .build();

            // When / Then - no X-Tenant-ID header
            mockMvc.perform(post("/api/v1/audit/clinical/decisions/{id}/accept", decisionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request))
                            .principal(AUTH))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Missing Header"));
        }
    }
}
