package com.healthdata.auditquery.kafka;

import com.healthdata.audit.models.AuditEvent;
import com.healthdata.audit.models.AuditOutcome;
import com.healthdata.auditquery.persistence.AuditResourceAccessDailyEntity;
import com.healthdata.auditquery.persistence.AuditUserActivityDailyEntity;
import com.healthdata.auditquery.repository.AuditResourceAccessDailyRepository;
import com.healthdata.auditquery.repository.AuditUserActivityDailyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditEventProjectionConsumer")
class AuditEventProjectionConsumerTest {

    private static final String TENANT_ID = "test-tenant-audit";
    private static final String USER_ID = "user-001";

    @Mock
    private AuditUserActivityDailyRepository userActivityRepository;

    @Mock
    private AuditResourceAccessDailyRepository resourceAccessRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private AuditEventProjectionConsumer consumer;

    private Instant eventTimestamp;
    private LocalDate eventDate;

    @BeforeEach
    void setUp() {
        eventTimestamp = Instant.parse("2026-03-01T12:00:00Z");
        eventDate = eventTimestamp.atZone(ZoneOffset.UTC).toLocalDate();
    }

    private AuditEvent buildAuditEvent(String resourceType, String resourceId, AuditOutcome outcome) {
        return AuditEvent.builder()
            .tenantId(TENANT_ID)
            .userId(USER_ID)
            .resourceType(resourceType)
            .resourceId(resourceId)
            .outcome(outcome)
            .build();
    }

    private void setupUserActivityNotFound() {
        when(userActivityRepository.findById(any(AuditUserActivityDailyEntity.UserActivityKey.class)))
            .thenReturn(Optional.empty());
    }

    private void setupRedis(String keyPattern, long size) {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add(anyString(), anyString())).thenReturn(1L);
        when(setOperations.size(anyString())).thenReturn(size);
    }

    @Nested
    @DisplayName("User Activity Projection")
    class UserActivityProjection {

        @Test
        @DisplayName("should increment total events counter for each audit event")
        void shouldIncrementUserActivityCounters() {
            // Given
            AuditEvent event = buildAuditEvent("Configuration", "config-1", AuditOutcome.SUCCESS);
            event.setTimestamp(eventTimestamp);
            setupUserActivityNotFound();
            setupRedis("audit:unique_resources:", 1L);

            // When
            consumer.consumeAuditEvent(event);

            // Then
            ArgumentCaptor<AuditUserActivityDailyEntity> captor =
                ArgumentCaptor.forClass(AuditUserActivityDailyEntity.class);
            verify(userActivityRepository).save(captor.capture());

            AuditUserActivityDailyEntity saved = captor.getValue();
            assertThat(saved.getTotalEvents()).isEqualTo(1L);
            assertThat(saved.getPhiAccessCount()).isEqualTo(0L);
            assertThat(saved.getFailedEvents()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should track PHI access when resource type is Patient")
        void shouldTrackPhiAccess_WhenPatientResourceType() {
            // Given
            AuditEvent event = buildAuditEvent("Patient", "patient-123", AuditOutcome.SUCCESS);
            event.setTimestamp(eventTimestamp);
            setupUserActivityNotFound();
            setupRedis("audit:unique_resources:", 1L);

            when(resourceAccessRepository.findById(any(AuditResourceAccessDailyEntity.ResourceAccessKey.class)))
                .thenReturn(Optional.empty());

            // When
            consumer.consumeAuditEvent(event);

            // Then
            ArgumentCaptor<AuditUserActivityDailyEntity> captor =
                ArgumentCaptor.forClass(AuditUserActivityDailyEntity.class);
            verify(userActivityRepository).save(captor.capture());

            AuditUserActivityDailyEntity saved = captor.getValue();
            assertThat(saved.getTotalEvents()).isEqualTo(1L);
            assertThat(saved.getPhiAccessCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should track failed events when outcome is not SUCCESS")
        void shouldTrackFailedEvents_WhenOutcomeNotSuccess() {
            // Given
            AuditEvent event = buildAuditEvent("Configuration", "config-1", AuditOutcome.MINOR_FAILURE);
            event.setTimestamp(eventTimestamp);
            setupUserActivityNotFound();
            setupRedis("audit:unique_resources:", 1L);

            // When
            consumer.consumeAuditEvent(event);

            // Then
            ArgumentCaptor<AuditUserActivityDailyEntity> captor =
                ArgumentCaptor.forClass(AuditUserActivityDailyEntity.class);
            verify(userActivityRepository).save(captor.capture());

            AuditUserActivityDailyEntity saved = captor.getValue();
            assertThat(saved.getTotalEvents()).isEqualTo(1L);
            assertThat(saved.getFailedEvents()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should track unique resources via Redis set")
        void shouldTrackUniqueResourcesViaRedis() {
            // Given
            AuditEvent event = buildAuditEvent("Configuration", "resource-abc", AuditOutcome.SUCCESS);
            event.setTimestamp(eventTimestamp);
            setupUserActivityNotFound();
            setupRedis("audit:unique_resources:", 3L);

            String expectedKey = "audit:unique_resources:" + USER_ID + ":" + eventDate;

            // When
            consumer.consumeAuditEvent(event);

            // Then
            verify(setOperations).add(expectedKey, "resource-abc");
            verify(redisTemplate).expire(eq(expectedKey), eq(Duration.ofHours(25)));

            ArgumentCaptor<AuditUserActivityDailyEntity> captor =
                ArgumentCaptor.forClass(AuditUserActivityDailyEntity.class);
            verify(userActivityRepository).save(captor.capture());
            assertThat(captor.getValue().getUniqueResources()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Resource Access Projection")
    class ResourceAccessProjection {

        @Test
        @DisplayName("should update resource access projection when resource is present")
        void shouldUpdateResourceAccessProjection_WhenResourcePresent() {
            // Given
            AuditEvent event = buildAuditEvent("Patient", "patient-456", AuditOutcome.SUCCESS);
            event.setTimestamp(eventTimestamp);
            setupUserActivityNotFound();
            setupRedis("audit:", 1L);

            when(resourceAccessRepository.findById(any(AuditResourceAccessDailyEntity.ResourceAccessKey.class)))
                .thenReturn(Optional.empty());

            // When
            consumer.consumeAuditEvent(event);

            // Then
            ArgumentCaptor<AuditResourceAccessDailyEntity> captor =
                ArgumentCaptor.forClass(AuditResourceAccessDailyEntity.class);
            verify(resourceAccessRepository).save(captor.capture());

            AuditResourceAccessDailyEntity saved = captor.getValue();
            assertThat(saved.getAccessCount()).isEqualTo(1L);
            assertThat(saved.getId().getResourceType()).isEqualTo("Patient");
            assertThat(saved.getId().getResourceId()).isEqualTo("patient-456");
            assertThat(saved.getId().getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("should skip resource access update when resource ID is null")
        void shouldSkipResourceAccess_WhenResourceIdNull() {
            // Given
            AuditEvent event = buildAuditEvent("Patient", null, AuditOutcome.SUCCESS);
            event.setTimestamp(eventTimestamp);
            setupUserActivityNotFound();
            // No Redis setup needed since resourceId is null (no unique resource tracking)

            // When
            consumer.consumeAuditEvent(event);

            // Then
            verify(resourceAccessRepository, never()).save(any());
        }
    }
}
