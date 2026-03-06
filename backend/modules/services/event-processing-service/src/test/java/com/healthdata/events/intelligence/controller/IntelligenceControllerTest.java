package com.healthdata.events.intelligence.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.events.intelligence.config.IntelligenceFeatureFlags;
import com.healthdata.events.intelligence.dto.IngestRequest;
import com.healthdata.events.intelligence.dto.RecommendationResponse;
import com.healthdata.events.intelligence.dto.ReviewRecommendationRequest;
import com.healthdata.events.intelligence.dto.TenantTrustDashboardResponse;
import com.healthdata.events.intelligence.dto.TrustProfileResponse;
import com.healthdata.events.intelligence.dto.UpdateValidationFindingStatusRequest;
import com.healthdata.events.intelligence.dto.ValidationFindingResponse;
import com.healthdata.events.intelligence.projection.TenantTrustProjectionService;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.FindingStatus;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.FindingType;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.Severity;
import com.healthdata.events.intelligence.service.IntelligenceRecommendationService;
import com.healthdata.events.intelligence.security.IntelligenceActorResolver;
import com.healthdata.events.intelligence.service.IntelligenceValidationService;
import com.healthdata.eventsourcing.intelligence.RecommendationReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("IntelligenceController Tests")
class IntelligenceControllerTest {

    @Mock
    private IntelligenceRecommendationService recommendationService;

    @Mock
    private IntelligenceValidationService validationService;

    @Mock
    private TenantTrustProjectionService tenantTrustProjectionService;

    @Mock
    private IntelligenceFeatureFlags intelligenceFeatureFlags;

    @Mock
    private IntelligenceActorResolver actorResolver;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        IntelligenceController controller = new IntelligenceController(
                recommendationService,
                validationService,
                tenantTrustProjectionService,
                intelligenceFeatureFlags,
                actorResolver
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new IntelligenceExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST ingest should return generated recommendations")
    void ingestShouldReturnGeneratedRecommendations() throws Exception {
        doNothing().when(intelligenceFeatureFlags).requireIngestEnabled("tenant-a");
        IngestRequest request = new IngestRequest(
                "evt-1",
                "patient-1",
                "FHIR_R4",
                "Observation",
                "1.0",
                "trace-1",
                Map.of("careGap", true),
                Map.of("source", "fhir"),
                0.8,
                "HIGH"
        );

        when(recommendationService.ingestAndGenerate(any())).thenReturn(List.of(
                new RecommendationResponse(
                        UUID.randomUUID(),
                        "patient-1",
                        "CARE_GAP_OPEN",
                        "Open care gap detected",
                        "desc",
                        "HIGH",
                        0.88,
                        RecommendationReviewStatus.PROPOSED,
                        null,
                        null,
                        null,
                        Instant.now()
                )
        ));

        mockMvc.perform(post("/api/v1/intelligence/ingest")
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$[0].signalType").value("CARE_GAP_OPEN"))
                .andExpect(jsonPath("$[0].status").value("PROPOSED"));

    }

    @Test
    @DisplayName("POST review should return updated recommendation")
    void reviewShouldReturnUpdatedRecommendation() throws Exception {
        UUID recommendationId = UUID.randomUUID();
        doNothing().when(intelligenceFeatureFlags).requireRecommendationReviewEnabled("tenant-a");
        when(actorResolver.resolveRequiredActor(any())).thenReturn("actor-1");

        when(recommendationService.reviewRecommendation(eq("tenant-a"), eq(recommendationId), any()))
                .thenReturn(new RecommendationResponse(
                        recommendationId,
                        "patient-1",
                        "CARE_GAP_OPEN",
                        "Open care gap detected",
                        "desc",
                        "HIGH",
                        0.88,
                        RecommendationReviewStatus.APPROVED,
                        "reviewer",
                        "ok",
                        Instant.now(),
                        Instant.now()
                ));

        mockMvc.perform(post("/api/v1/intelligence/recommendations/{recommendationId}/review", recommendationId)
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReviewRecommendationRequest(RecommendationReviewStatus.APPROVED, "reviewer", "ok")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedBy").value("reviewer"));
    }

    @Test
    @DisplayName("GET patient recommendations should return list")
    void getRecommendationsShouldReturnList() throws Exception {
        when(recommendationService.getRecommendations("tenant-a", "patient-1"))
                .thenReturn(List.of(
                        new RecommendationResponse(
                                UUID.randomUUID(),
                                "patient-1",
                                "NOVEL_PATTERN",
                                "Novel pattern",
                                "desc",
                                "MEDIUM",
                                0.66,
                                RecommendationReviewStatus.PROPOSED,
                                null,
                                null,
                                null,
                                Instant.now()
                        )
                ));

        mockMvc.perform(get("/api/v1/intelligence/patients/{patientId}/recommendations", "patient-1")
                        .header("X-Tenant-ID", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].signalType").value("NOVEL_PATTERN"));
    }

    @Test
    @DisplayName("GET validation findings should return list")
    void getValidationFindingsShouldReturnList() throws Exception {
        when(validationService.getFindings("tenant-a", "patient-1"))
                .thenReturn(List.of(
                        new ValidationFindingResponse(
                                UUID.randomUUID(),
                                "evt-1",
                                "DATA_MISSING_OBSERVATION_VALUE",
                                "Missing observation value",
                                "desc",
                                Severity.MEDIUM,
                                FindingType.DATA_COMPLETENESS,
                                FindingStatus.OPEN,
                                "{}",
                                Instant.now()
                        )
                ));

        mockMvc.perform(get("/api/v1/intelligence/patients/{patientId}/validation-findings", "patient-1")
                        .header("X-Tenant-ID", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ruleCode").value("DATA_MISSING_OBSERVATION_VALUE"));
    }

    @Test
    @DisplayName("GET trust profile should return score")
    void getTrustProfileShouldReturnScore() throws Exception {
        when(validationService.getTrustProfile("tenant-a", "patient-1"))
                .thenReturn(new TrustProfileResponse("patient-1", 80, 2, 1, 1, 1, 0));

        mockMvc.perform(get("/api/v1/intelligence/patients/{patientId}/trust-profile", "patient-1")
                        .header("X-Tenant-ID", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientRef").value("patient-1"))
                .andExpect(jsonPath("$.trustScore").value(80));
    }

    @Test
    @DisplayName("GET tenant trust dashboard should return projection")
    void getTenantTrustDashboardShouldReturnProjection() throws Exception {
        doNothing().when(intelligenceFeatureFlags).requireTrustDashboardEnabled("tenant-a");
        when(tenantTrustProjectionService.getTenantDashboard("tenant-a"))
                .thenReturn(new TenantTrustDashboardResponse("tenant-a", 91, 3, 1, 1, 1, 1, Instant.now()));

        mockMvc.perform(get("/api/v1/intelligence/tenants/{tenantId}/trust-dashboard", "tenant-a")
                        .header("X-Tenant-ID", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.trustScore").value(91));
    }

    @Test
    @DisplayName("POST validation finding status should return updated finding")
    void updateValidationFindingStatusShouldReturnUpdatedFinding() throws Exception {
        UUID findingId = UUID.randomUUID();
        doNothing().when(intelligenceFeatureFlags).requireValidationStatusUpdateEnabled("tenant-a");
        when(actorResolver.resolveRequiredActor(any())).thenReturn("actor-1");
        when(validationService.updateFindingStatus(eq("tenant-a"), eq(findingId), any()))
                .thenReturn(new ValidationFindingResponse(
                        findingId,
                        "evt-1",
                        "RULE-1",
                        "title",
                        "desc",
                        Severity.MEDIUM,
                        FindingType.DATA_COMPLETENESS,
                        FindingStatus.RESOLVED,
                        "{}",
                        Instant.now()
                ));

        mockMvc.perform(post("/api/v1/intelligence/validation-findings/{findingId}/status", findingId)
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateValidationFindingStatusRequest(FindingStatus.RESOLVED, "analyst-1", "resolved")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    @DisplayName("POST review should return 409 for invalid state transition")
    void reviewShouldReturn409ForInvalidTransition() throws Exception {
        UUID recommendationId = UUID.randomUUID();
        doNothing().when(intelligenceFeatureFlags).requireRecommendationReviewEnabled("tenant-a");
        when(actorResolver.resolveRequiredActor(any())).thenReturn("actor-1");
        when(recommendationService.reviewRecommendation(eq("tenant-a"), eq(recommendationId), any()))
                .thenThrow(new IllegalStateException("Invalid recommendation state transition"));

        mockMvc.perform(post("/api/v1/intelligence/recommendations/{recommendationId}/review", recommendationId)
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReviewRecommendationRequest(RecommendationReviewStatus.APPROVED, "reviewer", "ok")
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET tenant trust dashboard should return 403 when tenant path mismatches header")
    void getTenantTrustDashboardShouldReturn403OnTenantMismatch() throws Exception {
        doNothing().when(intelligenceFeatureFlags).requireTrustDashboardEnabled("tenant-b");

        mockMvc.perform(get("/api/v1/intelligence/tenants/{tenantId}/trust-dashboard", "tenant-b")
                        .header("X-Tenant-ID", "tenant-a"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
