package com.healthdata.events.intelligence.service;

import com.healthdata.events.intelligence.audit.ValidationFindingAuditService;
import com.healthdata.events.intelligence.dto.UpdateValidationFindingStatusRequest;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.FindingStatus;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.FindingType;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.Severity;
import com.healthdata.events.intelligence.repository.IntelligenceValidationFindingRepository;
import com.healthdata.events.intelligence.projection.TenantTrustProjectionService;
import com.healthdata.eventsourcing.intelligence.CanonicalEventEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IntelligenceValidationService Tests")
class IntelligenceValidationServiceTest {

    @Mock
    private IntelligenceValidationFindingRepository findingRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private TenantTrustProjectionService tenantTrustProjectionService;

    @Mock
    private ValidationFindingAuditService validationFindingAuditService;

    @InjectMocks
    private IntelligenceValidationService service;

    private CanonicalEventEnvelope envelope;

    @BeforeEach
    void setUp() {
        envelope = CanonicalEventEnvelope.builder()
                .eventId("evt-1")
                .tenantId("tenant-a")
                .patientRef("patient-1")
                .sourceType("FHIR_R4")
                .resourceType("Observation")
                .schemaVersion("1.0")
                .occurredAt(Instant.now().minusSeconds(200L * 24 * 3600))
                .payload(Map.of("conflictingIdentifiers", true))
                .provenance(Map.of("source", "fhir"))
                .build();
    }

    @Test
    @DisplayName("runValidation should persist and publish findings")
    void runValidationShouldPersistAndPublishFindings() {
        when(findingRepository.save(any(IntelligenceValidationFindingEntity.class)))
                .thenAnswer(invocation -> {
                    IntelligenceValidationFindingEntity entity = invocation.getArgument(0);
                    if (entity.getId() == null) {
                        entity.setId(UUID.randomUUID());
                    }
                    if (entity.getCreatedAt() == null) {
                        entity.setCreatedAt(Instant.now());
                    }
                    return entity;
                });

        var findings = service.runValidation(envelope);

        assertThat(findings).isNotEmpty();
        verify(findingRepository, times(findings.size())).save(any(IntelligenceValidationFindingEntity.class));
        verify(kafkaTemplate, times(findings.size())).send(eq("validation.findings"), eq("tenant-a"), any());
        verify(tenantTrustProjectionService, times(1)).refreshForTenant("tenant-a");
    }

    @Test
    @DisplayName("getTrustProfile should aggregate open findings")
    void getTrustProfileShouldAggregateOpenFindings() {
        when(findingRepository.findByTenantIdAndPatientRefAndStatusOrderByCreatedAtDesc(
                "tenant-a",
                "patient-1",
                FindingStatus.OPEN
        )).thenReturn(List.of(
                IntelligenceValidationFindingEntity.builder()
                        .id(UUID.randomUUID())
                        .tenantId("tenant-a")
                        .patientRef("patient-1")
                        .sourceEventId("evt-1")
                        .ruleCode("RULE-1")
                        .title("t")
                        .description("d")
                        .severity(Severity.HIGH)
                        .findingType(FindingType.CONSISTENCY)
                        .status(FindingStatus.OPEN)
                        .details("{}")
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build(),
                IntelligenceValidationFindingEntity.builder()
                        .id(UUID.randomUUID())
                        .tenantId("tenant-a")
                        .patientRef("patient-1")
                        .sourceEventId("evt-1")
                        .ruleCode("RULE-2")
                        .title("t")
                        .description("d")
                        .severity(Severity.MEDIUM)
                        .findingType(FindingType.DATA_COMPLETENESS)
                        .status(FindingStatus.OPEN)
                        .details("{}")
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build()
        ));

        var profile = service.getTrustProfile("tenant-a", "patient-1");

        assertThat(profile.patientRef()).isEqualTo("patient-1");
        assertThat(profile.trustScore()).isEqualTo(70);
        assertThat(profile.openFindings()).isEqualTo(2);
        assertThat(profile.highSeverityOpenFindings()).isEqualTo(1);
    }

    @Test
    @DisplayName("updateFindingStatus should enforce transition, audit, and refresh projection")
    void updateFindingStatusShouldEnforceTransitionAuditAndRefreshProjection() {
        UUID findingId = UUID.randomUUID();
        IntelligenceValidationFindingEntity finding = IntelligenceValidationFindingEntity.builder()
                .id(findingId)
                .tenantId("tenant-a")
                .patientRef("patient-1")
                .sourceEventId("evt-1")
                .ruleCode("RULE-1")
                .title("title")
                .description("desc")
                .severity(Severity.HIGH)
                .findingType(FindingType.CONSISTENCY)
                .status(FindingStatus.OPEN)
                .details("{}")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(findingRepository.findByIdAndTenantId(findingId, "tenant-a")).thenReturn(java.util.Optional.of(finding));
        when(findingRepository.save(any(IntelligenceValidationFindingEntity.class))).thenAnswer(i -> i.getArgument(0));

        var response = service.updateFindingStatus(
                "tenant-a",
                findingId,
                new UpdateValidationFindingStatusRequest(FindingStatus.RESOLVED, "analyst-1", "resolved")
        );

        assertThat(response.status()).isEqualTo(FindingStatus.RESOLVED);
        verify(validationFindingAuditService, times(1)).recordStateTransition(
                eq("tenant-a"),
                eq(findingId),
                eq(FindingStatus.OPEN),
                eq(FindingStatus.RESOLVED),
                eq("analyst-1"),
                eq("resolved")
        );
        verify(tenantTrustProjectionService, times(1)).refreshForTenant("tenant-a");
    }

    @Test
    @DisplayName("updateFindingStatus should reject invalid transition")
    void updateFindingStatusShouldRejectInvalidTransition() {
        UUID findingId = UUID.randomUUID();
        IntelligenceValidationFindingEntity finding = IntelligenceValidationFindingEntity.builder()
                .id(findingId)
                .tenantId("tenant-a")
                .patientRef("patient-1")
                .sourceEventId("evt-1")
                .ruleCode("RULE-1")
                .title("title")
                .description("desc")
                .severity(Severity.HIGH)
                .findingType(FindingType.CONSISTENCY)
                .status(FindingStatus.RESOLVED)
                .details("{}")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(findingRepository.findByIdAndTenantId(findingId, "tenant-a")).thenReturn(java.util.Optional.of(finding));

        assertThatThrownBy(() -> service.updateFindingStatus(
                "tenant-a",
                findingId,
                new UpdateValidationFindingStatusRequest(FindingStatus.OPEN, "analyst-1", "reopen")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid validation finding state transition");

        verify(findingRepository, never()).save(any());
        verify(validationFindingAuditService, never()).recordStateTransition(any(), any(), any(), any(), any(), any());
    }
}
