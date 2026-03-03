package com.healthdata.auditquery.service;

import com.healthdata.audit.entity.shared.AuditEventEntity;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.audit.models.AuditOutcome;
import com.healthdata.audit.repository.shared.AuditEventRepository;
import com.healthdata.auditquery.dto.AuditEventResponse;
import com.healthdata.auditquery.dto.AuditStatisticsResponse;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuditQueryService}.
 *
 * <p>Validates multi-tenant isolation, payload masking, statistics calculations,
 * and default time range behavior.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditQueryService")
class AuditQueryServiceTest {

    private static final String TENANT_ID = "test-tenant-audit";
    private static final String OTHER_TENANT_ID = "other-tenant";

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @InjectMocks
    private AuditQueryService auditQueryService;

    private AuditEventEntity sampleEntity;
    private UUID sampleEventId;

    @BeforeEach
    void setUp() {
        sampleEventId = UUID.randomUUID();
        sampleEntity = buildEntity(sampleEventId, TENANT_ID, AuditAction.READ,
                AuditOutcome.SUCCESS, "Patient", "user-1", false);
    }

    @Nested
    @DisplayName("getAuditEvent")
    class GetAuditEvent {

        @Test
        @DisplayName("should return event when tenant matches")
        void shouldReturnEvent_WhenTenantMatches() {
            // Given
            when(auditEventRepository.findById(sampleEventId))
                    .thenReturn(Optional.of(sampleEntity));

            // When
            Optional<AuditEventResponse> result = auditQueryService.getAuditEvent(TENANT_ID, sampleEventId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(sampleEventId);
            assertThat(result.get().tenantId()).isEqualTo(TENANT_ID);
            assertThat(result.get().action()).isEqualTo(AuditAction.READ);
        }

        @Test
        @DisplayName("should return empty when tenant does not match (COMPLIANCE: multi-tenant isolation)")
        void shouldReturnEmpty_WhenTenantMismatch() {
            // Given - entity belongs to TENANT_ID, but we query with OTHER_TENANT_ID
            when(auditEventRepository.findById(sampleEventId))
                    .thenReturn(Optional.of(sampleEntity));

            // When
            Optional<AuditEventResponse> result = auditQueryService.getAuditEvent(OTHER_TENANT_ID, sampleEventId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when event not found")
        void shouldReturnEmpty_WhenEventNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(auditEventRepository.findById(nonExistentId))
                    .thenReturn(Optional.empty());

            // When
            Optional<AuditEventResponse> result = auditQueryService.getAuditEvent(TENANT_ID, nonExistentId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should mask payload when encrypted is true")
        void shouldMaskPayload_WhenEncryptedIsTrue() {
            // Given
            AuditEventEntity encryptedEntity = buildEntity(sampleEventId, TENANT_ID,
                    AuditAction.READ, AuditOutcome.SUCCESS, "Patient", "user-1", true);
            encryptedEntity.setRequestPayload("{\"sensitive\":\"data\"}");
            encryptedEntity.setResponsePayload("{\"patient\":\"PHI\"}");

            when(auditEventRepository.findById(sampleEventId))
                    .thenReturn(Optional.of(encryptedEntity));

            // When
            Optional<AuditEventResponse> result = auditQueryService.getAuditEvent(TENANT_ID, sampleEventId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().requestPayload()).isEqualTo("{encrypted}");
            assertThat(result.get().responsePayload()).isEqualTo("{encrypted}");
            assertThat(result.get().encrypted()).isTrue();
        }
    }

    @Nested
    @DisplayName("getStatistics")
    class GetStatistics {

        @Test
        @DisplayName("should default to 30 days when no time range provided")
        void shouldDefaultTo30Days_WhenNoTimeRange() {
            // Given
            Instant before = Instant.now().minus(30, ChronoUnit.DAYS).minus(1, ChronoUnit.SECONDS);
            Instant after = Instant.now().minus(30, ChronoUnit.DAYS).plus(1, ChronoUnit.SECONDS);

            when(auditEventRepository.findByTenantIdAndTimestampBetween(
                    eq(TENANT_ID), any(Instant.class), any(Instant.class), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            // When
            AuditStatisticsResponse result = auditQueryService.getStatistics(TENANT_ID, null, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.startTime()).isBetween(before, after);
            assertThat(result.totalEvents()).isZero();
        }

        @Test
        @DisplayName("should calculate PHI access count for PHI resource types")
        void shouldCalculatePhiAccessCount_ForPhiResourceTypes() {
            // Given
            List<AuditEventEntity> events = List.of(
                    buildEntity(UUID.randomUUID(), TENANT_ID, AuditAction.READ, AuditOutcome.SUCCESS, "Patient", "user-1", false),
                    buildEntity(UUID.randomUUID(), TENANT_ID, AuditAction.READ, AuditOutcome.SUCCESS, "Observation", "user-2", false),
                    buildEntity(UUID.randomUUID(), TENANT_ID, AuditAction.READ, AuditOutcome.SUCCESS, "Condition", "user-3", false),
                    buildEntity(UUID.randomUUID(), TENANT_ID, AuditAction.READ, AuditOutcome.SUCCESS, "MedicationRequest", "user-4", false),
                    buildEntity(UUID.randomUUID(), TENANT_ID, AuditAction.READ, AuditOutcome.SUCCESS, "Organization", "user-5", false)
            );

            Instant startTime = Instant.now().minus(7, ChronoUnit.DAYS);
            Instant endTime = Instant.now();

            when(auditEventRepository.findByTenantIdAndTimestampBetween(
                    eq(TENANT_ID), eq(startTime), eq(endTime), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(events));

            // When
            AuditStatisticsResponse result = auditQueryService.getStatistics(TENANT_ID, startTime, endTime);

            // Then - Patient, Observation, Condition, MedicationRequest are PHI; Organization is not
            assertThat(result.phiAccessEvents()).isEqualTo(4);
        }

        @Test
        @DisplayName("should count failed events excluding SUCCESS outcome")
        void shouldCountFailedEvents_ExcludingSuccess() {
            // Given
            List<AuditEventEntity> events = List.of(
                    buildEntity(UUID.randomUUID(), TENANT_ID, AuditAction.READ, AuditOutcome.SUCCESS, "Patient", "user-1", false),
                    buildEntity(UUID.randomUUID(), TENANT_ID, AuditAction.READ, AuditOutcome.SUCCESS, "Patient", "user-2", false),
                    buildEntity(UUID.randomUUID(), TENANT_ID, AuditAction.READ, AuditOutcome.MINOR_FAILURE, "Patient", "user-3", false),
                    buildEntity(UUID.randomUUID(), TENANT_ID, AuditAction.LOGIN_FAILED, AuditOutcome.SERIOUS_FAILURE, "System", "user-4", false),
                    buildEntity(UUID.randomUUID(), TENANT_ID, AuditAction.READ, AuditOutcome.MAJOR_FAILURE, "Patient", "user-5", false)
            );

            Instant startTime = Instant.now().minus(7, ChronoUnit.DAYS);
            Instant endTime = Instant.now();

            when(auditEventRepository.findByTenantIdAndTimestampBetween(
                    eq(TENANT_ID), eq(startTime), eq(endTime), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(events));

            // When
            AuditStatisticsResponse result = auditQueryService.getStatistics(TENANT_ID, startTime, endTime);

            // Then - 3 non-SUCCESS outcomes
            assertThat(result.failedEvents()).isEqualTo(3);
        }

        @Test
        @DisplayName("should limit top users to 10")
        void shouldLimitTopUsers_To10() {
            // Given - create 15 unique users
            List<AuditEventEntity> events = IntStream.rangeClosed(1, 15)
                    .mapToObj(i -> buildEntity(UUID.randomUUID(), TENANT_ID, AuditAction.READ,
                            AuditOutcome.SUCCESS, "Patient", "user-" + i, false))
                    .toList();

            Instant startTime = Instant.now().minus(7, ChronoUnit.DAYS);
            Instant endTime = Instant.now();

            when(auditEventRepository.findByTenantIdAndTimestampBetween(
                    eq(TENANT_ID), eq(startTime), eq(endTime), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(events));

            // When
            AuditStatisticsResponse result = auditQueryService.getStatistics(TENANT_ID, startTime, endTime);

            // Then
            assertThat(result.topUsers()).hasSize(10);
        }
    }

    // --- Helper Methods ---

    private static AuditEventEntity buildEntity(UUID id, String tenantId, AuditAction action,
                                                  AuditOutcome outcome, String resourceType,
                                                  String userId, boolean encrypted) {
        AuditEventEntity entity = new AuditEventEntity();
        entity.setId(id);
        entity.setTimestamp(Instant.now());
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setUsername(userId + "@example.com");
        entity.setRole("EVALUATOR");
        entity.setIpAddress("192.168.1.100");
        entity.setUserAgent("TestAgent/1.0");
        entity.setAction(action);
        entity.setResourceType(resourceType);
        entity.setResourceId(UUID.randomUUID().toString());
        entity.setOutcome(outcome);
        entity.setServiceName("patient-service");
        entity.setMethodName("getPatient");
        entity.setRequestPath("/api/v1/patients/" + id);
        entity.setPurposeOfUse("Treatment");
        entity.setDurationMs(127L);
        entity.setEncrypted(encrypted);
        return entity;
    }
}
