package com.healthdata.events.intelligence.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.events.entity.EventEntity;
import com.healthdata.events.intelligence.audit.RecommendationAuditService;
import com.healthdata.events.intelligence.audit.ValidationFindingAuditService;
import com.healthdata.events.intelligence.config.IntelligenceFeatureFlags;
import com.healthdata.events.intelligence.entity.IntelligenceRecommendationEntity;
import com.healthdata.events.intelligence.entity.IntelligenceTenantTrustProjectionEntity;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.FindingStatus;
import com.healthdata.events.intelligence.projection.TenantTrustProjectionService;
import com.healthdata.events.intelligence.repository.IntelligenceRecommendationRepository;
import com.healthdata.events.intelligence.repository.IntelligenceTenantTrustProjectionRepository;
import com.healthdata.events.intelligence.repository.IntelligenceValidationFindingRepository;
import com.healthdata.events.intelligence.security.IntelligenceActorResolver;
import com.healthdata.events.intelligence.service.IntelligenceRecommendationService;
import com.healthdata.events.intelligence.service.IntelligenceValidationService;
import com.healthdata.events.repository.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Intelligence API Workflow Consistency Tests")
class IntelligenceApiWorkflowConsistencyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("API workflow keeps recommendation and trust projections consistent")
    void apiWorkflowKeepsRecommendationAndTrustProjectionsConsistent() throws Exception {
        IntelligenceValidationFindingRepository findingRepository = mock(IntelligenceValidationFindingRepository.class);
        IntelligenceRecommendationRepository recommendationRepository = mock(IntelligenceRecommendationRepository.class);
        IntelligenceTenantTrustProjectionRepository projectionRepository = mock(IntelligenceTenantTrustProjectionRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        IntelligenceActorResolver actorResolver = mock(IntelligenceActorResolver.class);

        Map<UUID, IntelligenceValidationFindingEntity> findingStore = new HashMap<>();
        Map<UUID, IntelligenceRecommendationEntity> recommendationStore = new HashMap<>();
        Map<String, IntelligenceTenantTrustProjectionEntity> projectionStore = new HashMap<>();
        List<EventEntity> eventStore = new ArrayList<>();

        when(findingRepository.save(any(IntelligenceValidationFindingEntity.class))).thenAnswer(invocation -> {
            IntelligenceValidationFindingEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(Instant.now());
            }
            if (entity.getUpdatedAt() == null) {
                entity.setUpdatedAt(Instant.now());
            }
            findingStore.put(entity.getId(), entity);
            return entity;
        });
        when(findingRepository.findById(any(UUID.class))).thenAnswer(invocation ->
                Optional.ofNullable(findingStore.get(invocation.getArgument(0)))
        );
        when(findingRepository.findByIdAndTenantId(any(UUID.class), anyString())).thenAnswer(invocation ->
                Optional.ofNullable(findingStore.get(invocation.getArgument(0)))
                        .filter(f -> invocation.getArgument(1).equals(f.getTenantId()))
        );
        when(findingRepository.findByTenantIdAndPatientRefOrderByCreatedAtDesc(anyString(), anyString())).thenAnswer(invocation ->
                findingStore.values().stream()
                        .filter(f -> invocation.getArgument(0).equals(f.getTenantId()))
                        .filter(f -> invocation.getArgument(1).equals(f.getPatientRef()))
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .toList()
        );
        when(findingRepository.findByTenantIdAndPatientRefAndStatusOrderByCreatedAtDesc(anyString(), anyString(), any(FindingStatus.class))).thenAnswer(invocation ->
                findingStore.values().stream()
                        .filter(f -> invocation.getArgument(0).equals(f.getTenantId()))
                        .filter(f -> invocation.getArgument(1).equals(f.getPatientRef()))
                        .filter(f -> invocation.getArgument(2).equals(f.getStatus()))
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .toList()
        );
        when(findingRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(anyString(), any(FindingStatus.class))).thenAnswer(invocation ->
                findingStore.values().stream()
                        .filter(f -> invocation.getArgument(0).equals(f.getTenantId()))
                        .filter(f -> invocation.getArgument(1).equals(f.getStatus()))
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .toList()
        );

        when(recommendationRepository.save(any(IntelligenceRecommendationEntity.class))).thenAnswer(invocation -> {
            IntelligenceRecommendationEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(Instant.now());
            }
            if (entity.getUpdatedAt() == null) {
                entity.setUpdatedAt(Instant.now());
            }
            recommendationStore.put(entity.getId(), entity);
            return entity;
        });
        when(recommendationRepository.findById(any(UUID.class))).thenAnswer(invocation ->
                Optional.ofNullable(recommendationStore.get(invocation.getArgument(0)))
        );
        when(recommendationRepository.findByIdAndTenantId(any(UUID.class), anyString())).thenAnswer(invocation ->
                Optional.ofNullable(recommendationStore.get(invocation.getArgument(0)))
                        .filter(r -> invocation.getArgument(1).equals(r.getTenantId()))
        );
        when(recommendationRepository.findByTenantIdAndPatientRefOrderByCreatedAtDesc(anyString(), anyString())).thenAnswer(invocation ->
                recommendationStore.values().stream()
                        .filter(r -> invocation.getArgument(0).equals(r.getTenantId()))
                        .filter(r -> invocation.getArgument(1).equals(r.getPatientRef()))
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .toList()
        );

        when(projectionRepository.save(any(IntelligenceTenantTrustProjectionEntity.class))).thenAnswer(invocation -> {
            IntelligenceTenantTrustProjectionEntity entity = invocation.getArgument(0);
            projectionStore.put(entity.getTenantId(), entity);
            return entity;
        });
        when(projectionRepository.findById(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(projectionStore.get(invocation.getArgument(0)))
        );

        when(eventRepository.save(any(EventEntity.class))).thenAnswer(invocation -> {
            EventEntity entity = invocation.getArgument(0);
            eventStore.add(entity);
            return entity;
        });

        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);
        when(actorResolver.resolveRequiredActor(any())).thenReturn("workflow-actor");

        TenantTrustProjectionService tenantTrustProjectionService =
                new TenantTrustProjectionService(findingRepository, projectionRepository);
        ValidationFindingAuditService validationFindingAuditService =
                new ValidationFindingAuditService(eventRepository);
        IntelligenceValidationService validationService =
                new IntelligenceValidationService(
                        findingRepository,
                        tenantTrustProjectionService,
                        validationFindingAuditService,
                        kafkaTemplate
                );
        RecommendationAuditService recommendationAuditService =
                new RecommendationAuditService(eventRepository);
        IntelligenceRecommendationService recommendationService =
                new IntelligenceRecommendationService(
                        recommendationRepository,
                        validationService,
                        recommendationAuditService,
                        kafkaTemplate
                );

        IntelligenceController controller = new IntelligenceController(
                recommendationService,
                validationService,
                tenantTrustProjectionService,
                kafkaTemplate,
                new IntelligenceFeatureFlags(),
                actorResolver
        );

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new IntelligenceExceptionHandler())
                .build();

        String ingestBody = objectMapper.writeValueAsString(Map.of(
                "eventId", "evt-api-1",
                "patientRef", "patient-api-1",
                "sourceType", "FHIR_R4",
                "resourceType", "Observation",
                "schemaVersion", "1.0",
                "traceId", "trace-1",
                "payload", Map.of("careGap", true, "conflictingIdentifiers", true),
                "provenance", Map.of("source", "fhir"),
                "confidence", 0.7,
                "riskTier", "HIGH"
        ));

        MvcResult ingestResult = mockMvc.perform(post("/api/v1/intelligence/ingest")
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ingestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$[0].id").exists())
                .andReturn();

        JsonNode ingestJson = objectMapper.readTree(ingestResult.getResponse().getContentAsString());
        String recommendationId = ingestJson.get(0).get("id").asText();

        mockMvc.perform(post("/api/v1/intelligence/recommendations/{recommendationId}/review", recommendationId)
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "TRIAGED",
                                "reviewedBy", "reviewer-api",
                                "notes", "triaged"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TRIAGED"));

        MvcResult findingsResult = mockMvc.perform(get("/api/v1/intelligence/patients/{patientId}/validation-findings", "patient-api-1")
                        .header("X-Tenant-ID", "tenant-a"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode findingsJson = objectMapper.readTree(findingsResult.getResponse().getContentAsString());
        assertThat(findingsJson.isArray()).isTrue();
        assertThat(findingsJson.size()).isGreaterThan(0);

        mockMvc.perform(get("/api/v1/intelligence/tenants/{tenantId}/trust-dashboard", "tenant-a")
                        .header("X-Tenant-ID", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustScore").value(org.hamcrest.Matchers.lessThan(100)));

        for (JsonNode finding : findingsJson) {
            String findingId = finding.get("id").asText();
            mockMvc.perform(post("/api/v1/intelligence/validation-findings/{findingId}/status", findingId)
                            .header("X-Tenant-ID", "tenant-a")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "status", "RESOLVED",
                                    "actedBy", "analyst-api",
                                    "notes", "resolved"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RESOLVED"));
        }

        mockMvc.perform(get("/api/v1/intelligence/tenants/{tenantId}/trust-dashboard", "tenant-a")
                        .header("X-Tenant-ID", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustScore").value(100))
                .andExpect(jsonPath("$.totalOpenFindings").value(0));

        assertThat(eventStore)
                .extracting(EventEntity::getEventType)
                .contains("RECOMMENDATION_STATE_CHANGED", "VALIDATION_FINDING_STATE_CHANGED");
    }
}
