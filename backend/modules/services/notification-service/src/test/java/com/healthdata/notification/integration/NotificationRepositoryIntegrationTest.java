package com.healthdata.notification.integration;

import com.healthdata.notification.config.BaseIntegrationTest;
import com.healthdata.notification.domain.model.Notification;
import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationPriority;
import com.healthdata.notification.domain.model.NotificationStatus;
import com.healthdata.notification.domain.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Notification Repository Integration Tests
 *
 * Tests database operations for notification persistence with real PostgreSQL
 * via Testcontainers. Covers:
 * - Basic CRUD operations
 * - Tenant-filtered queries
 * - Status-based queries (pending, sent, failed)
 * - Channel filtering
 * - Pagination and sorting
 * - Retry mechanism queries
 * - Multi-tenant data isolation
 */
@BaseIntegrationTest
@DisplayName("NotificationRepository Integration Tests")
class NotificationRepositoryIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final String OTHER_TENANT = "other-tenant";
    private static final String RECIPIENT_ID = "user-001";
    private static final String CORRELATION_ID = "corr-12345";

    private Notification pendingNotification;
    private Notification sentNotification;
    private Notification failedNotification;

    @BeforeEach
    void setUp() {
        pendingNotification = createNotification(TENANT_ID, RECIPIENT_ID, NotificationChannel.EMAIL,
                NotificationStatus.PENDING, "Test Subject 1", "Test body 1");
        sentNotification = createNotification(TENANT_ID, RECIPIENT_ID, NotificationChannel.SMS,
                NotificationStatus.SENT, null, "SMS notification body");
        failedNotification = createNotification(TENANT_ID, "user-002", NotificationChannel.PUSH,
                NotificationStatus.FAILED, "Push Title", "Push notification body");

        pendingNotification = notificationRepository.save(pendingNotification);
        sentNotification.setSentAt(Instant.now());
        sentNotification = notificationRepository.save(sentNotification);
        failedNotification.setErrorMessage("Failed to deliver notification");
        failedNotification.setRetryCount(1);
        failedNotification = notificationRepository.save(failedNotification);
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and retrieve notification by ID")
        void shouldSaveAndRetrieve() {
            Optional<Notification> found = notificationRepository.findById(pendingNotification.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getSubject()).isEqualTo("Test Subject 1");
            assertThat(found.get().getRecipientId()).isEqualTo(RECIPIENT_ID);
        }

        @Test
        @DisplayName("Should find notification by ID and tenant")
        void shouldFindByIdAndTenant() {
            Optional<Notification> found = notificationRepository.findByIdAndTenantId(
                    pendingNotification.getId(), TENANT_ID);

            assertThat(found).isPresent();
            assertThat(found.get().getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("Should not find notification with wrong tenant")
        void shouldNotFindWithWrongTenant() {
            Optional<Notification> found = notificationRepository.findByIdAndTenantId(
                    pendingNotification.getId(), OTHER_TENANT);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should update notification")
        void shouldUpdate() {
            pendingNotification.setStatus(NotificationStatus.SENDING);
            notificationRepository.save(pendingNotification);

            Optional<Notification> found = notificationRepository.findById(pendingNotification.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo(NotificationStatus.SENDING);
        }

        @Test
        @DisplayName("Should delete notification")
        void shouldDelete() {
            UUID id = pendingNotification.getId();
            notificationRepository.delete(pendingNotification);

            Optional<Notification> found = notificationRepository.findById(id);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should auto-generate timestamp on create")
        void shouldAutoGenerateTimestamp() {
            Notification newNotification = createNotification(TENANT_ID, RECIPIENT_ID, NotificationChannel.IN_APP,
                    NotificationStatus.PENDING, "New Subject", "New body");
            newNotification.setCreatedAt(null);

            Notification saved = notificationRepository.save(newNotification);

            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Paginated Queries")
    class PaginatedQueryTests {

        @Test
        @DisplayName("Should find notifications by tenant with pagination")
        void shouldFindByTenantWithPagination() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Notification> page = notificationRepository.findByTenantId(TENANT_ID, pageable);

            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getTotalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should find notifications by tenant and recipient")
        void shouldFindByTenantAndRecipient() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Notification> page = notificationRepository.findByTenantIdAndRecipientId(
                    TENANT_ID, RECIPIENT_ID, pageable);

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getContent()).allMatch(n -> n.getRecipientId().equals(RECIPIENT_ID));
        }

        @Test
        @DisplayName("Should find notifications by tenant and status")
        void shouldFindByTenantAndStatus() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Notification> page = notificationRepository.findByTenantIdAndStatus(
                    TENANT_ID, NotificationStatus.PENDING, pageable);

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getStatus()).isEqualTo(NotificationStatus.PENDING);
        }

        @Test
        @DisplayName("Should find notifications by tenant and channel")
        void shouldFindByTenantAndChannel() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Notification> page = notificationRepository.findByTenantIdAndChannel(
                    TENANT_ID, NotificationChannel.EMAIL, pageable);

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getChannel()).isEqualTo(NotificationChannel.EMAIL);
        }
    }

    @Nested
    @DisplayName("Status-Based Queries")
    class StatusBasedQueryTests {

        @Test
        @DisplayName("Should find retryable notifications")
        void shouldFindRetryableNotifications() {
            List<Notification> retryable = notificationRepository.findRetryableNotifications(NotificationStatus.FAILED);

            assertThat(retryable).hasSize(1);
            assertThat(retryable.get(0).getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(retryable.get(0).getRetryCount()).isLessThan(retryable.get(0).getMaxRetries());
        }

        @Test
        @DisplayName("Should not return notifications that exceeded max retries")
        void shouldNotReturnExceededMaxRetries() {
            failedNotification.setRetryCount(5);
            failedNotification.setMaxRetries(3);
            notificationRepository.save(failedNotification);

            List<Notification> retryable = notificationRepository.findRetryableNotifications(NotificationStatus.FAILED);

            assertThat(retryable).isEmpty();
        }

        @Test
        @DisplayName("Should find pending notifications to send")
        void shouldFindPendingNotificationsToSend() {
            List<Notification> pending = notificationRepository.findPendingNotificationsToSend(Instant.now());

            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getStatus()).isEqualTo(NotificationStatus.PENDING);
        }

        @Test
        @DisplayName("Should not return scheduled future notifications")
        void shouldNotReturnFutureScheduledNotifications() {
            Notification scheduledNotification = createNotification(TENANT_ID, RECIPIENT_ID, NotificationChannel.EMAIL,
                    NotificationStatus.PENDING, "Scheduled", "Body");
            scheduledNotification.setScheduledAt(Instant.now().plus(1, ChronoUnit.HOURS));
            notificationRepository.save(scheduledNotification);

            List<Notification> pending = notificationRepository.findPendingNotificationsToSend(Instant.now());

            assertThat(pending).noneMatch(n -> "Scheduled".equals(n.getSubject()));
        }

        @Test
        @DisplayName("Should count notifications by tenant and status")
        void shouldCountByTenantAndStatus() {
            long pendingCount = notificationRepository.countByTenantIdAndStatus(TENANT_ID, NotificationStatus.PENDING);
            long sentCount = notificationRepository.countByTenantIdAndStatus(TENANT_ID, NotificationStatus.SENT);
            long failedCount = notificationRepository.countByTenantIdAndStatus(TENANT_ID, NotificationStatus.FAILED);

            assertThat(pendingCount).isEqualTo(1);
            assertThat(sentCount).isEqualTo(1);
            assertThat(failedCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Date-Based Queries")
    class DateBasedQueryTests {

        @Test
        @DisplayName("Should find recent notifications by tenant")
        void shouldFindRecentNotifications() {
            Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
            Pageable pageable = PageRequest.of(0, 10);

            Page<Notification> page = notificationRepository.findRecentByTenantId(TENANT_ID, since, pageable);

            assertThat(page.getContent()).hasSize(3);
        }

        @Test
        @DisplayName("Should not find old notifications")
        void shouldNotFindOldNotifications() {
            Instant since = Instant.now().plus(1, ChronoUnit.HOURS);
            Pageable pageable = PageRequest.of(0, 10);

            Page<Notification> page = notificationRepository.findRecentByTenantId(TENANT_ID, since, pageable);

            assertThat(page.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Correlation ID Queries")
    class CorrelationIdQueryTests {

        @Test
        @DisplayName("Should find notifications by correlation ID")
        void shouldFindByCorrelationId() {
            pendingNotification.setCorrelationId(CORRELATION_ID);
            notificationRepository.save(pendingNotification);

            List<Notification> found = notificationRepository.findByCorrelationId(CORRELATION_ID);

            assertThat(found).hasSize(1);
            assertThat(found.get(0).getCorrelationId()).isEqualTo(CORRELATION_ID);
        }

        @Test
        @DisplayName("Should find multiple notifications with same correlation ID")
        void shouldFindMultipleByCorrelationId() {
            pendingNotification.setCorrelationId(CORRELATION_ID);
            sentNotification.setCorrelationId(CORRELATION_ID);
            notificationRepository.save(pendingNotification);
            notificationRepository.save(sentNotification);

            List<Notification> found = notificationRepository.findByCorrelationId(CORRELATION_ID);

            assertThat(found).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Notification Lifecycle")
    class NotificationLifecycleTests {

        @Test
        @DisplayName("Should mark notification as sent")
        void shouldMarkAsSent() {
            pendingNotification.markAsSent();
            notificationRepository.save(pendingNotification);

            Optional<Notification> found = notificationRepository.findById(pendingNotification.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(found.get().getSentAt()).isNotNull();
        }

        @Test
        @DisplayName("Should mark notification as failed with error message")
        void shouldMarkAsFailed() {
            pendingNotification.markAsFailed("Connection timeout");
            notificationRepository.save(pendingNotification);

            Optional<Notification> found = notificationRepository.findById(pendingNotification.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(found.get().getErrorMessage()).isEqualTo("Connection timeout");
            assertThat(found.get().getRetryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should check if notification can retry")
        void shouldCheckCanRetry() {
            failedNotification.setRetryCount(2);
            failedNotification.setMaxRetries(3);
            notificationRepository.save(failedNotification);

            Optional<Notification> found = notificationRepository.findById(failedNotification.getId());

            assertThat(found).isPresent();
            assertThat(found.get().canRetry()).isTrue();
        }

        @Test
        @DisplayName("Should return false for can retry when max retries exceeded")
        void shouldReturnFalseWhenMaxRetriesExceeded() {
            failedNotification.setRetryCount(3);
            failedNotification.setMaxRetries(3);
            notificationRepository.save(failedNotification);

            Optional<Notification> found = notificationRepository.findById(failedNotification.getId());

            assertThat(found).isPresent();
            assertThat(found.get().canRetry()).isFalse();
        }
    }

    @Nested
    @DisplayName("Metadata and JSON Queries")
    class MetadataQueryTests {

        @Test
        @DisplayName("Should store and retrieve metadata as JSON")
        void shouldStoreAndRetrieveMetadata() {
            Map<String, Object> metadata = Map.of(
                    "careGapId", "gap-123",
                    "patientId", "patient-456",
                    "measureId", "HEDIS_CDC"
            );
            pendingNotification.setMetadata(metadata);
            notificationRepository.save(pendingNotification);

            Optional<Notification> found = notificationRepository.findById(pendingNotification.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getMetadata()).containsEntry("careGapId", "gap-123");
            assertThat(found.get().getMetadata()).containsEntry("patientId", "patient-456");
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation (HIPAA Compliance)")
    class MultiTenantIsolationTests {

        @Test
        @DisplayName("Should isolate notifications between tenants")
        void shouldIsolateNotificationsBetweenTenants() {
            Notification otherTenantNotification = createNotification(OTHER_TENANT, RECIPIENT_ID,
                    NotificationChannel.EMAIL, NotificationStatus.PENDING, "Other Tenant Subject", "Other body");
            notificationRepository.save(otherTenantNotification);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Notification> tenant1Page = notificationRepository.findByTenantId(TENANT_ID, pageable);
            Page<Notification> tenant2Page = notificationRepository.findByTenantId(OTHER_TENANT, pageable);

            assertThat(tenant1Page.getContent()).noneMatch(n -> n.getTenantId().equals(OTHER_TENANT));
            assertThat(tenant2Page.getContent()).noneMatch(n -> n.getTenantId().equals(TENANT_ID));
        }

        @Test
        @DisplayName("Should not allow cross-tenant access via ID query")
        void shouldNotAllowCrossTenantAccessById() {
            Optional<Notification> result = notificationRepository.findByIdAndTenantId(
                    pendingNotification.getId(), OTHER_TENANT);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("All notifications should have non-null tenant IDs")
        void shouldHaveNonNullTenantIds() {
            List<Notification> all = notificationRepository.findAll();

            assertThat(all).allMatch(n -> n.getTenantId() != null && !n.getTenantId().isEmpty());
        }

        @Test
        @DisplayName("Should count only tenant's own notifications")
        void shouldCountOnlyTenantOwnNotifications() {
            Notification otherTenantNotification = createNotification(OTHER_TENANT, RECIPIENT_ID,
                    NotificationChannel.EMAIL, NotificationStatus.PENDING, "Other Subject", "Other body");
            notificationRepository.save(otherTenantNotification);

            long tenant1Count = notificationRepository.countByTenantIdAndStatus(TENANT_ID, NotificationStatus.PENDING);
            long tenant2Count = notificationRepository.countByTenantIdAndStatus(OTHER_TENANT, NotificationStatus.PENDING);

            assertThat(tenant1Count).isEqualTo(1);
            assertThat(tenant2Count).isEqualTo(1);
        }
    }

    // Helper method
    private Notification createNotification(String tenantId, String recipientId, NotificationChannel channel,
                                            NotificationStatus status, String subject, String body) {
        return Notification.builder()
                .tenantId(tenantId)
                .recipientId(recipientId)
                .recipientEmail(recipientId + "@example.com")
                .channel(channel)
                .subject(subject)
                .body(body)
                .status(status)
                .priority(NotificationPriority.NORMAL)
                .retryCount(0)
                .maxRetries(3)
                .build();
    }
}
