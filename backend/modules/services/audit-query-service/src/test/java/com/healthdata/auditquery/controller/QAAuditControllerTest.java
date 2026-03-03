package com.healthdata.auditquery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.auditquery.dto.qa.QAReviewQueueResponse;
import com.healthdata.auditquery.dto.qa.QAReviewRequest;
import com.healthdata.auditquery.exception.GlobalExceptionHandler;
import com.healthdata.auditquery.service.QAAuditService;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("QAAuditController Unit Tests")
class QAAuditControllerTest {

    private static final String TENANT_ID = "test-tenant-audit";
    private static final String TEST_USER = "test-user";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TestingAuthenticationToken AUTH = new TestingAuthenticationToken(TEST_USER, null, "ROLE_ADMIN");

    private MockMvc mockMvc;

    @Mock
    private QAAuditService qaAuditService;

    @InjectMocks
    private QAAuditController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/audit/ai/qa/review-queue")
    class GetReviewQueue {

        @Test
        @DisplayName("Should return 200 with paginated review queue")
        void getReviewQueue_shouldReturn200() throws Exception {
            // Given
            QAReviewQueueResponse item = QAReviewQueueResponse.builder()
                    .eventId(UUID.randomUUID())
                    .agentType("CARE_GAP_DETECTION")
                    .confidenceScore(0.87)
                    .qaReviewStatus("PENDING")
                    .timestamp(Instant.now())
                    .build();
            Page<QAReviewQueueResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1);

            when(qaAuditService.getReviewQueue(
                    eq(TENANT_ID), isNull(), isNull(), isNull(),
                    isNull(), isNull(), eq(false), eq(0), eq(20)))
                    .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/v1/audit/ai/qa/review-queue")
                            .header("X-Tenant-ID", TENANT_ID)
                            .principal(AUTH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].agentType").value("CARE_GAP_DETECTION"))
                    .andExpect(jsonPath("$.content[0].qaReviewStatus").value("PENDING"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/audit/ai/qa/review/{id}/approve")
    class ApproveDecision {

        @Test
        @DisplayName("Should return 200 when decision is approved")
        void approveDecision_shouldReturn200() throws Exception {
            // Given
            UUID decisionId = UUID.randomUUID();
            QAReviewRequest request = QAReviewRequest.builder()
                    .reviewNotes("AI decision validated against clinical guidelines")
                    .build();

            doNothing().when(qaAuditService).approveDecision(
                    eq(TENANT_ID), eq(decisionId), any(QAReviewRequest.class), eq(TEST_USER));

            // When / Then
            mockMvc.perform(post("/api/v1/audit/ai/qa/review/{id}/approve", decisionId)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request))
                            .principal(AUTH))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/audit/ai/qa/review/{id}")
    class GetReviewDetail {

        @Test
        @DisplayName("Should return 404 when review detail is not found")
        void getReviewDetail_shouldReturn404_WhenNotFound() throws Exception {
            // Given
            UUID decisionId = UUID.randomUUID();

            when(qaAuditService.getReviewDetail(eq(TENANT_ID), eq(decisionId)))
                    .thenReturn(Optional.empty());

            // When / Then
            mockMvc.perform(get("/api/v1/audit/ai/qa/review/{id}", decisionId)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isNotFound());
        }
    }
}
