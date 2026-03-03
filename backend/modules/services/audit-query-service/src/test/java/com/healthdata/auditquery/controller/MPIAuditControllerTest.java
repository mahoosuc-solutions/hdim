package com.healthdata.auditquery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.auditquery.dto.mpi.MPIMergeEventResponse;
import com.healthdata.auditquery.dto.mpi.MPIReviewRequest;
import com.healthdata.auditquery.exception.GlobalExceptionHandler;
import com.healthdata.auditquery.service.MPIAuditService;
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
@DisplayName("MPIAuditController Unit Tests")
class MPIAuditControllerTest {

    private static final String TENANT_ID = "test-tenant-audit";
    private static final String TEST_USER = "test-user";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TestingAuthenticationToken AUTH = new TestingAuthenticationToken(TEST_USER, null, "ROLE_ADMIN");

    private MockMvc mockMvc;

    @Mock
    private MPIAuditService mpiAuditService;

    @InjectMocks
    private MPIAuditController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/audit/ai/user-actions")
    class GetMPIMergeEvents {

        @Test
        @DisplayName("Should return 200 with paginated MPI merge events")
        void getMPIMergeEvents_shouldReturn200() throws Exception {
            // Given
            MPIMergeEventResponse event = MPIMergeEventResponse.builder()
                    .id(UUID.randomUUID())
                    .sourcePatientId("PAT-12345")
                    .targetPatientId("PAT-67890")
                    .mergeType("AUTOMATIC")
                    .confidenceScore(0.92)
                    .mergeStatus("PENDING")
                    .build();
            Page<MPIMergeEventResponse> page = new PageImpl<>(List.of(event), PageRequest.of(0, 20), 1);

            when(mpiAuditService.getMPIMergeEvents(
                    eq(TENANT_ID), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), isNull(),
                    isNull(), isNull(), eq(0), eq(20)))
                    .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/v1/audit/ai/user-actions")
                            .header("X-Tenant-ID", TENANT_ID)
                            .principal(AUTH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].sourcePatientId").value("PAT-12345"))
                    .andExpect(jsonPath("$.content[0].mergeType").value("AUTOMATIC"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/audit/mpi/merges/{id}/validate")
    class ValidateMerge {

        @Test
        @DisplayName("Should return 200 when merge is validated")
        void validateMerge_shouldReturn200() throws Exception {
            // Given
            UUID mergeId = UUID.randomUUID();
            MPIReviewRequest request = MPIReviewRequest.builder()
                    .validationNotes("Verified demographic match across all fields")
                    .dataQualityAssessment("HIGH")
                    .build();
            MPIMergeEventResponse response = MPIMergeEventResponse.builder()
                    .id(mergeId)
                    .mergeStatus("VALIDATED")
                    .validationStatus("VALIDATED")
                    .validatedBy("test-user")
                    .build();

            when(mpiAuditService.validateMerge(
                    eq(TENANT_ID), eq(mergeId), any(MPIReviewRequest.class), eq(TEST_USER)))
                    .thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/v1/audit/mpi/merges/{id}/validate", mergeId)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request))
                            .principal(AUTH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mergeStatus").value("VALIDATED"))
                    .andExpect(jsonPath("$.validationStatus").value("VALIDATED"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/audit/mpi/merges/{id}/rollback")
    class RollbackMerge {

        @Test
        @DisplayName("Should return 200 when merge is rolled back")
        void rollbackMerge_shouldReturn200() throws Exception {
            // Given
            UUID mergeId = UUID.randomUUID();
            MPIReviewRequest request = MPIReviewRequest.builder()
                    .rollbackReason("Incorrect patient match detected")
                    .build();
            MPIMergeEventResponse response = MPIMergeEventResponse.builder()
                    .id(mergeId)
                    .mergeStatus("ROLLED_BACK")
                    .rollbackReason("Incorrect patient match detected")
                    .rolledBackBy("test-user")
                    .build();

            when(mpiAuditService.rollbackMerge(
                    eq(TENANT_ID), eq(mergeId), any(MPIReviewRequest.class), eq(TEST_USER)))
                    .thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/v1/audit/mpi/merges/{id}/rollback", mergeId)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request))
                            .principal(AUTH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mergeStatus").value("ROLLED_BACK"))
                    .andExpect(jsonPath("$.rollbackReason").value("Incorrect patient match detected"));
        }
    }
}
