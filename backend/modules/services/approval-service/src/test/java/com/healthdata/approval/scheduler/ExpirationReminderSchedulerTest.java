package com.healthdata.approval.scheduler;

import com.healthdata.approval.domain.entity.ApprovalRequest;
import com.healthdata.approval.domain.entity.ApprovalRequest.*;
import com.healthdata.approval.notification.EmailNotificationService;
import com.healthdata.approval.repository.ApprovalRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ExpirationReminderScheduler Tests")
class ExpirationReminderSchedulerTest {

    @Mock
    private ApprovalRequestRepository requestRepository;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ExpirationReminderScheduler scheduler;

    private static final String TENANT_ID = "tenant-123";
    private static final String ASSIGNEE_EMAIL = "reviewer@test.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "reminderEnabled", true);
        ReflectionTestUtils.setField(scheduler, "reminderHoursBefore", 4);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("Send Expiration Reminders")
    class SendExpirationRemindersTests {

        @Test
        @DisplayName("should send reminder for request expiring within 1 hour")
        void sendExpirationReminders_OneHour_SendsReminder() {
            // Given
            ApprovalRequest request = createExpiringRequest(Duration.ofMinutes(55));
            request.setAssignedTo(ASSIGNEE_EMAIL);

            when(requestRepository.findExpiringSoonAllTenants(any(Instant.class)))
                .thenReturn(List.of(request));
            when(redisTemplate.hasKey(anyString())).thenReturn(false);

            // When
            scheduler.sendExpirationReminders();

            // Then
            verify(emailNotificationService).sendExpirationReminderNotification(
                eq(request),
                eq(ASSIGNEE_EMAIL),
                eq(ASSIGNEE_EMAIL)
            );
            verify(valueOperations).set(
                contains(request.getId().toString()),
                eq("sent"),
                eq(Duration.ofHours(24).toSeconds()),
                eq(TimeUnit.SECONDS)
            );
        }

        @Test
        @DisplayName("should send reminder for request expiring within 2 hours")
        void sendExpirationReminders_TwoHours_SendsReminder() {
            // Given
            ApprovalRequest request = createExpiringRequest(Duration.ofHours(2).minus(Duration.ofMinutes(10)));
            request.setAssignedTo(ASSIGNEE_EMAIL);

            when(requestRepository.findExpiringSoonAllTenants(any(Instant.class)))
                .thenReturn(List.of(request));
            when(redisTemplate.hasKey(anyString())).thenReturn(false);

            // When
            scheduler.sendExpirationReminders();

            // Then
            verify(emailNotificationService).sendExpirationReminderNotification(
                any(ApprovalRequest.class),
                eq(ASSIGNEE_EMAIL),
                eq(ASSIGNEE_EMAIL)
            );
        }

        @Test
        @DisplayName("should send reminder for request expiring within 4 hours")
        void sendExpirationReminders_FourHours_SendsReminder() {
            // Given
            ApprovalRequest request = createExpiringRequest(Duration.ofHours(4).minus(Duration.ofMinutes(10)));
            request.setAssignedTo(ASSIGNEE_EMAIL);

            when(requestRepository.findExpiringSoonAllTenants(any(Instant.class)))
                .thenReturn(List.of(request));
            when(redisTemplate.hasKey(anyString())).thenReturn(false);

            // When
            scheduler.sendExpirationReminders();

            // Then
            verify(emailNotificationService).sendExpirationReminderNotification(
                any(ApprovalRequest.class),
                eq(ASSIGNEE_EMAIL),
                eq(ASSIGNEE_EMAIL)
            );
        }

        @Test
        @DisplayName("should skip reminder when already sent")
        void sendExpirationReminders_AlreadySent_Skips() {
            // Given
            ApprovalRequest request = createExpiringRequest(Duration.ofMinutes(55));
            request.setAssignedTo(ASSIGNEE_EMAIL);

            when(requestRepository.findExpiringSoonAllTenants(any(Instant.class)))
                .thenReturn(List.of(request));
            when(redisTemplate.hasKey(anyString())).thenReturn(true);

            // When
            scheduler.sendExpirationReminders();

            // Then
            verify(emailNotificationService, never()).sendExpirationReminderNotification(
                any(), anyString(), anyString()
            );
        }

        @Test
        @DisplayName("should skip reminder when no assignee")
        void sendExpirationReminders_NoAssignee_Skips() {
            // Given
            ApprovalRequest request = createExpiringRequest(Duration.ofMinutes(55));
            request.setAssignedTo(null);

            when(requestRepository.findExpiringSoonAllTenants(any(Instant.class)))
                .thenReturn(List.of(request));

            // When
            scheduler.sendExpirationReminders();

            // Then
            verify(emailNotificationService, never()).sendExpirationReminderNotification(
                any(), anyString(), anyString()
            );
        }

        @Test
        @DisplayName("should skip when reminders disabled")
        void sendExpirationReminders_Disabled_Skips() {
            // Given
            ReflectionTestUtils.setField(scheduler, "reminderEnabled", false);

            // When
            scheduler.sendExpirationReminders();

            // Then
            verify(requestRepository, never()).findExpiringSoonAllTenants(any());
        }

        @Test
        @DisplayName("should handle multiple expiring requests")
        void sendExpirationReminders_MultipleRequests_SendsAll() {
            // Given
            ApprovalRequest request1 = createExpiringRequest(Duration.ofMinutes(55));
            request1.setAssignedTo("user1@test.com");
            ApprovalRequest request2 = createExpiringRequest(Duration.ofHours(2));
            request2.setAssignedTo("user2@test.com");

            when(requestRepository.findExpiringSoonAllTenants(any(Instant.class)))
                .thenReturn(List.of(request1, request2));
            when(redisTemplate.hasKey(anyString())).thenReturn(false);

            // When
            scheduler.sendExpirationReminders();

            // Then
            verify(emailNotificationService, times(2)).sendExpirationReminderNotification(
                any(), anyString(), anyString()
            );
        }

        @Test
        @DisplayName("should continue on individual reminder failure")
        void sendExpirationReminders_OneFailure_ContinuesProcessing() {
            // Given
            ApprovalRequest request1 = createExpiringRequest(Duration.ofMinutes(55));
            request1.setAssignedTo("user1@test.com");
            ApprovalRequest request2 = createExpiringRequest(Duration.ofHours(2));
            request2.setAssignedTo("user2@test.com");

            when(requestRepository.findExpiringSoonAllTenants(any(Instant.class)))
                .thenReturn(List.of(request1, request2));
            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            doThrow(new RuntimeException("Email error"))
                .doNothing()
                .when(emailNotificationService).sendExpirationReminderNotification(
                    any(), anyString(), anyString()
                );

            // When
            scheduler.sendExpirationReminders();

            // Then
            verify(emailNotificationService, times(2)).sendExpirationReminderNotification(
                any(), anyString(), anyString()
            );
        }

        @Test
        @DisplayName("should handle empty expiring requests list")
        void sendExpirationReminders_NoRequests_DoesNothing() {
            // Given
            when(requestRepository.findExpiringSoonAllTenants(any(Instant.class)))
                .thenReturn(Collections.emptyList());

            // When
            scheduler.sendExpirationReminders();

            // Then
            verify(emailNotificationService, never()).sendExpirationReminderNotification(
                any(), anyString(), anyString()
            );
        }

        @Test
        @DisplayName("should handle repository exception gracefully")
        void sendExpirationReminders_RepositoryError_HandlesGracefully() {
            // Given
            when(requestRepository.findExpiringSoonAllTenants(any(Instant.class)))
                .thenThrow(new RuntimeException("Database error"));

            // When/Then
            assertThatCode(() -> scheduler.sendExpirationReminders())
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Reminder Level Detection")
    class ReminderLevelTests {

        @Test
        @DisplayName("should not send reminder for requests expiring after 4 hours")
        void sendReminder_MoreThan4Hours_Skips() {
            // Given
            ApprovalRequest request = createExpiringRequest(Duration.ofHours(5));
            request.setAssignedTo(ASSIGNEE_EMAIL);

            when(requestRepository.findExpiringSoonAllTenants(any(Instant.class)))
                .thenReturn(List.of(request));
            when(redisTemplate.hasKey(anyString())).thenReturn(false);

            // When
            scheduler.sendExpirationReminders();

            // Then
            verify(emailNotificationService, never()).sendExpirationReminderNotification(
                any(), anyString(), anyString()
            );
        }

        @Test
        @DisplayName("should send different reminders at different time intervals")
        void sendReminder_DifferentIntervals_DifferentKeys() {
            // Given
            ApprovalRequest request = createExpiringRequest(Duration.ofMinutes(55));
            request.setAssignedTo(ASSIGNEE_EMAIL);

            when(requestRepository.findExpiringSoonAllTenants(any(Instant.class)))
                .thenReturn(List.of(request));
            when(redisTemplate.hasKey(anyString())).thenReturn(false);

            // When
            scheduler.sendExpirationReminders();

            // Then
            verify(valueOperations).set(
                contains(":1h"),
                eq("sent"),
                anyLong(),
                eq(TimeUnit.SECONDS)
            );
        }
    }

    @Nested
    @DisplayName("Email Lookup")
    class EmailLookupTests {

        @Test
        @DisplayName("should use assignedTo as email if it contains @")
        void emailLookup_ContainsAt_UsesDirectly() {
            // Given
            ApprovalRequest request = createExpiringRequest(Duration.ofMinutes(55));
            request.setAssignedTo("user@example.com");

            when(requestRepository.findExpiringSoonAllTenants(any(Instant.class)))
                .thenReturn(List.of(request));
            when(redisTemplate.hasKey(anyString())).thenReturn(false);

            // When
            scheduler.sendExpirationReminders();

            // Then
            verify(emailNotificationService).sendExpirationReminderNotification(
                eq(request),
                eq("user@example.com"),
                eq("user@example.com")
            );
        }

        @Test
        @DisplayName("should skip when assignedTo is not an email")
        void emailLookup_NotEmail_Skips() {
            // Given
            ApprovalRequest request = createExpiringRequest(Duration.ofMinutes(55));
            request.setAssignedTo("username-without-at");

            when(requestRepository.findExpiringSoonAllTenants(any(Instant.class)))
                .thenReturn(List.of(request));
            when(redisTemplate.hasKey(anyString())).thenReturn(false);

            // When
            scheduler.sendExpirationReminders();

            // Then
            verify(emailNotificationService, never()).sendExpirationReminderNotification(
                any(), anyString(), anyString()
            );
        }

        @Test
        @DisplayName("should skip when assignedTo is null")
        void emailLookup_Null_Skips() {
            // Given
            ApprovalRequest request = createExpiringRequest(Duration.ofMinutes(55));
            request.setAssignedTo(null);

            when(requestRepository.findExpiringSoonAllTenants(any(Instant.class)))
                .thenReturn(List.of(request));

            // When
            scheduler.sendExpirationReminders();

            // Then
            verify(emailNotificationService, never()).sendExpirationReminderNotification(
                any(), anyString(), anyString()
            );
        }
    }

    @Nested
    @DisplayName("Redis Integration")
    class RedisIntegrationTests {

        @Test
        @DisplayName("should fallback to sending reminder when Redis unavailable")
        void redisCheck_Unavailable_SendsReminder() {
            // Given
            ApprovalRequest request = createExpiringRequest(Duration.ofMinutes(55));
            request.setAssignedTo(ASSIGNEE_EMAIL);

            when(requestRepository.findExpiringSoonAllTenants(any(Instant.class)))
                .thenReturn(List.of(request));
            when(redisTemplate.hasKey(anyString()))
                .thenThrow(new RuntimeException("Redis error"));

            // When
            scheduler.sendExpirationReminders();

            // Then
            verify(emailNotificationService).sendExpirationReminderNotification(
                any(), eq(ASSIGNEE_EMAIL), eq(ASSIGNEE_EMAIL)
            );
        }

        @Test
        @DisplayName("should handle Redis set failure gracefully")
        void redisSet_Failure_HandlesGracefully() {
            // Given
            ApprovalRequest request = createExpiringRequest(Duration.ofMinutes(55));
            request.setAssignedTo(ASSIGNEE_EMAIL);

            when(requestRepository.findExpiringSoonAllTenants(any(Instant.class)))
                .thenReturn(List.of(request));
            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            doThrow(new RuntimeException("Redis set error"))
                .when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

            // When/Then
            assertThatCode(() -> scheduler.sendExpirationReminders())
                .doesNotThrowAnyException();

            verify(emailNotificationService).sendExpirationReminderNotification(
                any(), anyString(), anyString()
            );
        }
    }

    // Helper methods

    private ApprovalRequest createExpiringRequest(Duration timeUntilExpiration) {
        UUID id = UUID.randomUUID();
        ApprovalRequest request = ApprovalRequest.builder()
            .id(id)
            .tenantId(TENANT_ID)
            .requestType(RequestType.AGENT_ACTION)
            .entityType("TestEntity")
            .entityId("entity-123")
            .actionRequested("EXECUTE")
            .riskLevel(RiskLevel.MEDIUM)
            .status(ApprovalStatus.ASSIGNED)
            .requestedBy("user-123")
            .payload(new HashMap<>())
            .expiresAt(Instant.now().plus(timeUntilExpiration))
            .build();

        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }
}
