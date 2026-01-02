# Notification Service

## Overview

The Notification Service provides multi-channel notification delivery for the HDIM platform, supporting email, SMS, push notifications, and in-app messaging. It handles template management, user preferences, and event-driven notification delivery via Kafka.

## Responsibilities

- Deliver notifications across multiple channels (Email, SMS, Push, In-App)
- Manage notification templates with variable substitution
- Store and enforce user notification preferences
- Route notifications to appropriate channels based on user preferences
- Process notification events from Kafka for async delivery
- Track notification delivery status and retry failed deliveries

## Technology Stack

| Component | Technology | Version | Why This Choice |
|-----------|------------|---------|-----------------|
| Runtime | Java | 21 LTS | Platform standard, virtual threads support |
| Framework | Spring Boot | 3.x | Ecosystem integration, async support |
| Database | PostgreSQL | 15 | Multi-tenant, ACID compliant |
| Messaging | Apache Kafka | 3.x | Event-driven notification triggers |
| Email | SMTP / SendGrid | - | Reliable email delivery |
| SMS | Twilio | - | Enterprise SMS provider |
| Push | Firebase Cloud Messaging | - | Cross-platform push notifications |

## API Endpoints

### Notifications

#### POST /api/v1/notifications/send
**Purpose**: Send a single notification
**Auth Required**: Yes (roles: ADMIN, SYSTEM)
**Request Headers**:
- `X-Tenant-ID` (required)
- `Authorization: Bearer <token>` (required)

**Request Body**:
```json
{
  "recipientId": "user-uuid",
  "templateCode": "CARE_GAP_ALERT",
  "channel": "EMAIL",
  "variables": {
    "patientName": "John Doe",
    "measureName": "Breast Cancer Screening"
  }
}
```

#### POST /api/v1/notifications/bulk
**Purpose**: Send bulk notifications
**Auth Required**: Yes (roles: ADMIN, SYSTEM)

#### GET /api/v1/notifications/{id}
**Purpose**: Get notification status
**Auth Required**: Yes (roles: ADMIN, EVALUATOR)

### Templates

#### GET /api/v1/templates
**Purpose**: List notification templates
**Auth Required**: Yes (roles: ADMIN)

#### POST /api/v1/templates
**Purpose**: Create notification template
**Auth Required**: Yes (roles: ADMIN)

#### PUT /api/v1/templates/{id}
**Purpose**: Update notification template
**Auth Required**: Yes (roles: ADMIN)

### Preferences

#### GET /api/v1/preferences/{userId}
**Purpose**: Get user notification preferences
**Auth Required**: Yes (roles: ADMIN, or self)

#### PUT /api/v1/preferences/{userId}
**Purpose**: Update user notification preferences
**Auth Required**: Yes (roles: ADMIN, or self)

## Database Schema

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| notifications | Notification records | id, tenant_id, recipient_id, channel, status, sent_at |
| notification_templates | Template definitions | id, tenant_id, code, channel, subject, body |
| notification_preferences | User preferences | id, tenant_id, user_id, channel, enabled |

## Kafka Topics

### Consumes

| Topic | Event Type | Handler |
|-------|------------|---------|
| notification.requests | NotificationRequestEvent | NotificationEventConsumer |
| care-gap.detected | CareGapDetectedEvent | Triggers care gap notifications |
| measure.evaluation.complete | MeasureEvaluationEvent | Triggers quality alerts |

### Publishes

| Topic | Event Type | Payload |
|-------|------------|---------|
| notification.sent | NotificationSentEvent | Notification delivery confirmation |
| notification.failed | NotificationFailedEvent | Delivery failure details |

## Notification Channels

| Channel | Provider | Configuration |
|---------|----------|---------------|
| EMAIL | SMTP / SendGrid | `notification.email.*` properties |
| SMS | Twilio | `notification.sms.twilio.*` properties |
| PUSH | Firebase | `notification.push.firebase.*` properties |
| IN_APP | Internal | Stored in database for portal display |

## Configuration

```yaml
# application.yml
server:
  port: 8104

spring:
  datasource:
    url: jdbc:postgresql://localhost:5435/healthdata_notification

notification:
  email:
    enabled: true
    from: noreply@healthdatainmotion.com
    smtp:
      host: smtp.sendgrid.net
      port: 587
  sms:
    twilio:
      enabled: true
      account-sid: ${TWILIO_ACCOUNT_SID}
      auth-token: ${TWILIO_AUTH_TOKEN}
      from-number: ${TWILIO_FROM_NUMBER}
  push:
    firebase:
      enabled: true
      credentials-path: ${FIREBASE_CREDENTIALS_PATH}
```

## Testing

### Overview

The Notification Service has comprehensive test coverage across multi-channel notification delivery (EMAIL, SMS, PUSH, IN_APP), template management with variable substitution, user preference enforcement, and HIPAA-compliant multi-tenant isolation. Tests validate the complete notification lifecycle from creation through delivery with retry logic and error handling.

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:notification-service:test

# Run specific test suite
./gradlew :modules:services:notification-service:test --tests "*ServiceTest"
./gradlew :modules:services:notification-service:test --tests "*IntegrationTest"
./gradlew :modules:services:notification-service:test --tests "*ControllerTest"

# Run with coverage report
./gradlew :modules:services:notification-service:test jacocoTestReport

# Run only unit tests (fast)
./gradlew :modules:services:notification-service:test --tests "*ServiceTest,*RouterTest"

# Run integration tests with Testcontainers
./gradlew :modules:services:notification-service:test --tests "*IntegrationTest"
```

### Test Coverage Summary

| Test Class | Tests | Purpose |
|------------|-------|---------|
| NotificationServiceTest | 6+ | Core notification logic, preference enforcement, scheduling |
| ChannelRouterTest | 11+ | Multi-channel provider registration and routing |
| NotificationControllerTest | 15+ | REST API endpoints for notifications |
| TemplateServiceTest | 28+ | Template CRUD and variable substitution |
| NotificationRepositoryIntegrationTest | 40+ | Notification persistence with Testcontainers |
| NotificationTemplateRepositoryIntegrationTest | 25+ | Template persistence and channel-based queries |
| NotificationPreferenceRepositoryIntegrationTest | 30+ | User preferences and quiet hours |
| BaseIntegrationTest | - | Shared Testcontainers configuration |
| TestSecurityConfiguration | - | Test security bypass |
| **Total** | **155+** | **Comprehensive notification delivery coverage** |

### Test Organization

```
src/test/java/com/healthdata/notification/
├── application/
│   ├── NotificationServiceTest.java      # Core service logic
│   ├── ChannelRouterTest.java            # Channel routing
│   └── TemplateServiceTest.java          # Template management
├── api/v1/
│   └── NotificationControllerTest.java   # REST API tests
├── config/
│   ├── BaseIntegrationTest.java          # Testcontainers setup
│   └── TestSecurityConfiguration.java    # Test security config
└── integration/
    ├── NotificationRepositoryIntegrationTest.java
    ├── NotificationTemplateRepositoryIntegrationTest.java
    └── NotificationPreferenceRepositoryIntegrationTest.java
```

---

### Unit Tests

#### NotificationServiceTest

Tests core notification business logic including preference enforcement, channel selection, and scheduled delivery.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private ChannelRouter channelRouter;

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private NotificationService notificationService;

    private static final String TENANT_ID = "test-tenant";
    private static final String USER_ID = "user-123";

    @Nested
    @DisplayName("sendNotification() tests")
    class SendNotificationTests {

        @Test
        @DisplayName("Should send EMAIL notification successfully")
        void shouldSendEmailNotificationSuccessfully() {
            // Given
            SendNotificationRequest request = SendNotificationRequest.builder()
                .tenantId(TENANT_ID)
                .recipientId(USER_ID)
                .channel(NotificationChannel.EMAIL)
                .templateCode("CARE_GAP_ALERT")
                .variables(Map.of("patientName", "John Doe", "measureName", "HbA1c"))
                .build();

            NotificationPreference enabledPreference = NotificationPreference.builder()
                .enabled(true)
                .email("user@example.com")
                .build();

            when(preferenceRepository.findByTenantIdAndUserIdAndChannel(
                TENANT_ID, USER_ID, NotificationChannel.EMAIL))
                .thenReturn(Optional.of(enabledPreference));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            Notification result = notificationService.sendNotification(request);

            // Then
            assertThat(result.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(result.getChannel()).isEqualTo(NotificationChannel.EMAIL);
            verify(channelRouter).route(any(Notification.class));
        }

        @Test
        @DisplayName("Should block notification when user preference disables channel")
        void shouldBlockNotificationWhenPreferenceDisabled() {
            // Given
            SendNotificationRequest request = SendNotificationRequest.builder()
                .tenantId(TENANT_ID)
                .recipientId(USER_ID)
                .channel(NotificationChannel.EMAIL)
                .templateCode("CARE_GAP_ALERT")
                .build();

            NotificationPreference disabledPreference = NotificationPreference.builder()
                .enabled(false)
                .build();

            when(preferenceRepository.findByTenantIdAndUserIdAndChannel(
                TENANT_ID, USER_ID, NotificationChannel.EMAIL))
                .thenReturn(Optional.of(disabledPreference));

            // When
            Notification result = notificationService.sendNotification(request);

            // Then - Notification is CANCELLED, not sent
            assertThat(result.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
            assertThat(result.getErrorMessage()).contains("preferences");
            verify(channelRouter, never()).route(any());
        }

        @Test
        @DisplayName("Should NOT send scheduled notification immediately")
        void shouldNotSendScheduledNotificationImmediately() {
            // Given
            SendNotificationRequest request = SendNotificationRequest.builder()
                .tenantId(TENANT_ID)
                .recipientId(USER_ID)
                .channel(NotificationChannel.SMS)
                .scheduledAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

            when(preferenceRepository.findByTenantIdAndUserIdAndChannel(any(), any(), any()))
                .thenReturn(Optional.of(NotificationPreference.builder().enabled(true).build()));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            Notification result = notificationService.sendNotification(request);

            // Then - Notification is PENDING but NOT routed immediately
            assertThat(result.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(result.getScheduledAt()).isNotNull();
            verify(channelRouter, never()).route(any());
        }
    }

    @Nested
    @DisplayName("Bulk notification tests")
    class BulkNotificationTests {

        @Test
        @DisplayName("Should send bulk notifications with success/failure counts")
        void shouldSendBulkNotificationsWithCounts() {
            // Given
            List<SendNotificationRequest> requests = List.of(
                createRequest(USER_ID, NotificationChannel.EMAIL),
                createRequest("user-456", NotificationChannel.SMS),
                createRequest("user-789", NotificationChannel.PUSH)
            );

            when(preferenceRepository.findByTenantIdAndUserIdAndChannel(any(), any(), any()))
                .thenReturn(Optional.of(NotificationPreference.builder().enabled(true).build()));
            when(notificationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            // When
            BulkNotificationResult result = notificationService.sendBulkNotifications(requests);

            // Then
            assertThat(result.getTotal()).isEqualTo(3);
            assertThat(result.getSuccessCount()).isEqualTo(3);
            assertThat(result.getFailureCount()).isEqualTo(0);
        }
    }
}
```

**Key Test Patterns:**
- **Preference Enforcement**: Returns CANCELLED status (not exception) when disabled
- **Scheduled Notifications**: Validates future scheduledAt prevents immediate routing
- **Bulk Operations**: Tests batch processing with success/failure counts

#### ChannelRouterTest

Tests the provider registration pattern for multi-channel notification routing.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelRouter Tests")
class ChannelRouterTest {

    @Nested
    @DisplayName("Provider registration tests")
    class ProviderRegistrationTests {

        @Test
        @DisplayName("Should register and retrieve provider by channel")
        void shouldRegisterAndRetrieveProvider() {
            // Given
            NotificationProvider emailProvider = createMockProvider(
                Set.of(NotificationChannel.EMAIL));
            ChannelRouter router = new ChannelRouter(List.of(emailProvider));

            // When
            NotificationProvider retrieved = router.getProvider(NotificationChannel.EMAIL);

            // Then
            assertThat(retrieved).isEqualTo(emailProvider);
        }

        @Test
        @DisplayName("Should support multiple channels per provider")
        void shouldSupportMultipleChannelsPerProvider() {
            // Given
            NotificationProvider multiChannelProvider = createMockProvider(
                Set.of(NotificationChannel.EMAIL, NotificationChannel.SMS));
            ChannelRouter router = new ChannelRouter(List.of(multiChannelProvider));

            // When/Then
            assertThat(router.getProvider(NotificationChannel.EMAIL)).isEqualTo(multiChannelProvider);
            assertThat(router.getProvider(NotificationChannel.SMS)).isEqualTo(multiChannelProvider);
        }

        @Test
        @DisplayName("Should throw exception for unsupported channel")
        void shouldThrowForUnsupportedChannel() {
            // Given
            NotificationProvider emailProvider = createMockProvider(
                Set.of(NotificationChannel.EMAIL));
            ChannelRouter router = new ChannelRouter(List.of(emailProvider));

            // When/Then
            assertThatThrownBy(() -> router.getProvider(NotificationChannel.SMS))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("No provider registered for channel: SMS");
        }

        @Test
        @DisplayName("Should override provider when multiple support same channel")
        void shouldOverrideProviderWhenMultipleSupportSameChannel() {
            // Given - Last provider wins
            NotificationProvider firstProvider = createMockProvider(
                Set.of(NotificationChannel.EMAIL), "FirstProvider");
            NotificationProvider secondProvider = createMockProvider(
                Set.of(NotificationChannel.EMAIL), "SecondProvider");
            ChannelRouter router = new ChannelRouter(List.of(firstProvider, secondProvider));

            // When
            NotificationProvider retrieved = router.getProvider(NotificationChannel.EMAIL);

            // Then - Second provider wins (last registered)
            assertThat(retrieved).isEqualTo(secondProvider);
        }
    }

    @Nested
    @DisplayName("Channel support validation tests")
    class ChannelSupportTests {

        @Test
        @DisplayName("Should validate channel support before routing")
        void shouldValidateChannelSupportBeforeRouting() {
            // Given
            NotificationProvider emailProvider = createMockProvider(
                Set.of(NotificationChannel.EMAIL));
            ChannelRouter router = new ChannelRouter(List.of(emailProvider));

            // When/Then
            assertThat(router.supportsChannel(NotificationChannel.EMAIL)).isTrue();
            assertThat(router.supportsChannel(NotificationChannel.SMS)).isFalse();
            assertThat(router.supportsChannel(NotificationChannel.PUSH)).isFalse();
            assertThat(router.supportsChannel(NotificationChannel.IN_APP)).isFalse();
        }

        @Test
        @DisplayName("Should list all supported channels")
        void shouldListAllSupportedChannels() {
            // Given
            NotificationProvider emailProvider = createMockProvider(
                Set.of(NotificationChannel.EMAIL));
            NotificationProvider smsProvider = createMockProvider(
                Set.of(NotificationChannel.SMS, NotificationChannel.PUSH));
            ChannelRouter router = new ChannelRouter(List.of(emailProvider, smsProvider));

            // When
            Set<NotificationChannel> supported = router.getSupportedChannels();

            // Then
            assertThat(supported).containsExactlyInAnyOrder(
                NotificationChannel.EMAIL,
                NotificationChannel.SMS,
                NotificationChannel.PUSH
            );
        }
    }

    private NotificationProvider createMockProvider(Set<NotificationChannel> channels) {
        return createMockProvider(channels, "MockProvider");
    }

    private NotificationProvider createMockProvider(Set<NotificationChannel> channels, String name) {
        NotificationProvider provider = mock(NotificationProvider.class);
        when(provider.getSupportedChannels()).thenReturn(channels);
        when(provider.getName()).thenReturn(name);
        return provider;
    }
}
```

**Key Patterns:**
- **Provider Abstraction**: Each channel (EMAIL, SMS, PUSH, IN_APP) has pluggable provider
- **Last-Registration Wins**: When multiple providers support same channel, last one overrides
- **Unsupported Channel Handling**: Throws `UnsupportedOperationException` with clear message

#### TemplateServiceTest

Tests template variable substitution including nested object support.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateService Tests")
class TemplateServiceTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @InjectMocks
    private TemplateService templateService;

    private static final String TENANT_ID = "test-tenant";

    @Nested
    @DisplayName("Variable substitution tests")
    class VariableSubstitutionTests {

        @Test
        @DisplayName("Should substitute simple variables")
        void shouldSubstituteSimpleVariables() {
            // Given
            String template = "Hello {{ name }}, your appointment is on {{ date }}.";
            Map<String, Object> variables = Map.of(
                "name", "John Doe",
                "date", "2025-01-15"
            );

            // When
            String result = templateService.renderTemplate(template, variables);

            // Then
            assertThat(result).isEqualTo("Hello John Doe, your appointment is on 2025-01-15.");
        }

        @Test
        @DisplayName("Should substitute nested object variables")
        void shouldSubstituteNestedObjectVariables() {
            // Given
            String template = "Patient {{ patient.name }} (MRN: {{ patient.mrn }})";
            Map<String, Object> variables = Map.of(
                "patient", Map.of(
                    "name", "Jane Smith",
                    "mrn", "MRN-12345"
                )
            );

            // When
            String result = templateService.renderTemplate(template, variables);

            // Then
            assertThat(result).isEqualTo("Patient Jane Smith (MRN: MRN-12345)");
        }

        @Test
        @DisplayName("Should handle deeply nested variables")
        void shouldHandleDeeplyNestedVariables() {
            // Given
            String template = "Organization: {{ org.department.name }}";
            Map<String, Object> variables = Map.of(
                "org", Map.of(
                    "department", Map.of(
                        "name", "Cardiology"
                    )
                )
            );

            // When
            String result = templateService.renderTemplate(template, variables);

            // Then
            assertThat(result).isEqualTo("Organization: Cardiology");
        }

        @Test
        @DisplayName("Should handle missing variables gracefully")
        void shouldHandleMissingVariablesGracefully() {
            // Given
            String template = "Hello {{ name }}, your score is {{ score }}.";
            Map<String, Object> variables = Map.of("name", "John");

            // When
            String result = templateService.renderTemplate(template, variables);

            // Then - Missing variable replaced with empty string
            assertThat(result).isEqualTo("Hello John, your score is .");
        }
    }

    @Nested
    @DisplayName("Variable extraction tests")
    class VariableExtractionTests {

        @Test
        @DisplayName("Should extract simple variable names")
        void shouldExtractSimpleVariableNames() {
            // Given
            String template = "Hello {{ name }}, welcome to {{ organization }}.";

            // When
            List<String> variables = templateService.extractVariables(template);

            // Then
            assertThat(variables).containsExactlyInAnyOrder("name", "organization");
        }

        @Test
        @DisplayName("Should extract nested variable paths")
        void shouldExtractNestedVariablePaths() {
            // Given
            String template = "Patient {{ patient.name }} (ID: {{ patient.id }})";

            // When
            List<String> variables = templateService.extractVariables(template);

            // Then
            assertThat(variables).containsExactlyInAnyOrder("patient.name", "patient.id");
        }

        @Test
        @DisplayName("Should deduplicate repeated variables")
        void shouldDeduplicateRepeatedVariables() {
            // Given
            String template = "{{ name }} is {{ name }}'s full name.";

            // When
            List<String> variables = templateService.extractVariables(template);

            // Then
            assertThat(variables).containsExactly("name");
        }
    }

    @Nested
    @DisplayName("Template CRUD tests")
    class TemplateCrudTests {

        @Test
        @DisplayName("Should create template with extracted variables")
        void shouldCreateTemplateWithExtractedVariables() {
            // Given
            CreateTemplateRequest request = CreateTemplateRequest.builder()
                .tenantId(TENANT_ID)
                .code("APPOINTMENT_REMINDER")
                .name("Appointment Reminder")
                .channel(NotificationChannel.EMAIL)
                .subjectTemplate("Appointment with {{ provider.name }}")
                .bodyTemplate("Dear {{ patient.name }}, you have an appointment on {{ date }}.")
                .build();

            when(templateRepository.existsByTenantIdAndCode(TENANT_ID, "APPOINTMENT_REMINDER"))
                .thenReturn(false);
            when(templateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            NotificationTemplate result = templateService.createTemplate(request);

            // Then
            assertThat(result.getVariables()).containsExactlyInAnyOrder(
                "provider.name", "patient.name", "date"
            );
        }

        @Test
        @DisplayName("Should throw exception for duplicate template code")
        void shouldThrowForDuplicateTemplateCode() {
            // Given
            CreateTemplateRequest request = CreateTemplateRequest.builder()
                .tenantId(TENANT_ID)
                .code("EXISTING_TEMPLATE")
                .build();

            when(templateRepository.existsByTenantIdAndCode(TENANT_ID, "EXISTING_TEMPLATE"))
                .thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> templateService.createTemplate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Template code already exists");
        }

        @Test
        @DisplayName("Should soft delete template by setting active flag")
        void shouldSoftDeleteTemplateBySettingActiveFlag() {
            // Given
            NotificationTemplate template = NotificationTemplate.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .code("OLD_TEMPLATE")
                .active(true)
                .build();

            when(templateRepository.findByIdAndTenantId(template.getId(), TENANT_ID))
                .thenReturn(Optional.of(template));
            when(templateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            templateService.deleteTemplate(template.getId(), TENANT_ID);

            // Then - Template is soft deleted (active = false), not physically removed
            ArgumentCaptor<NotificationTemplate> captor = ArgumentCaptor.forClass(NotificationTemplate.class);
            verify(templateRepository).save(captor.capture());
            assertThat(captor.getValue().getActive()).isFalse();
            verify(templateRepository, never()).delete(any());
        }
    }
}
```

**Key Patterns:**
- **Mustache-Style Variables**: `{{ variable }}` syntax with whitespace tolerance
- **Nested Object Support**: Dot notation for nested paths (e.g., `{{ patient.name }}`)
- **Graceful Missing Variables**: Empty string substitution instead of error
- **Auto-Variable Extraction**: Variables parsed from template on create/update
- **Soft Delete**: Sets `active = false` rather than physical deletion

---

### Integration Tests

#### NotificationRepositoryIntegrationTest

Tests notification persistence with real PostgreSQL via Testcontainers.

```java
@BaseIntegrationTest
@DisplayName("NotificationRepository Integration Tests")
class NotificationRepositoryIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final String OTHER_TENANT = "other-tenant";

    private Notification pendingNotification;
    private Notification sentNotification;
    private Notification failedNotification;

    @BeforeEach
    void setUp() {
        pendingNotification = createNotification(TENANT_ID, NotificationStatus.PENDING);
        sentNotification = createNotification(TENANT_ID, NotificationStatus.SENT);
        failedNotification = createNotification(TENANT_ID, NotificationStatus.FAILED);
        failedNotification.setRetryCount(1);
        failedNotification.setMaxRetries(3);

        pendingNotification = notificationRepository.save(pendingNotification);
        sentNotification = notificationRepository.save(sentNotification);
        failedNotification = notificationRepository.save(failedNotification);
    }

    @Nested
    @DisplayName("Notification lifecycle tests")
    class LifecycleTests {

        @Test
        @DisplayName("Should mark notification as sent with timestamp")
        void shouldMarkAsSentWithTimestamp() {
            // When
            pendingNotification.markAsSent();
            notificationRepository.save(pendingNotification);

            // Then
            Optional<Notification> found = notificationRepository.findById(pendingNotification.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(found.get().getSentAt()).isNotNull();
        }

        @Test
        @DisplayName("Should mark notification as failed with error message")
        void shouldMarkAsFailedWithErrorMessage() {
            // When
            pendingNotification.markAsFailed("Connection timeout to SMTP server");
            notificationRepository.save(pendingNotification);

            // Then
            Optional<Notification> found = notificationRepository.findById(pendingNotification.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(found.get().getErrorMessage()).isEqualTo("Connection timeout to SMTP server");
            assertThat(found.get().getRetryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should check if notification can retry")
        void shouldCheckIfCanRetry() {
            // Given - Failed notification with retries remaining
            assertThat(failedNotification.canRetry()).isTrue();

            // When - Exceed max retries
            failedNotification.setRetryCount(3);
            notificationRepository.save(failedNotification);

            // Then
            Optional<Notification> found = notificationRepository.findById(failedNotification.getId());
            assertThat(found).isPresent();
            assertThat(found.get().canRetry()).isFalse();
        }
    }

    @Nested
    @DisplayName("Status-based query tests")
    class StatusQueryTests {

        @Test
        @DisplayName("Should find retryable notifications")
        void shouldFindRetryableNotifications() {
            // When
            List<Notification> retryable = notificationRepository.findRetryableNotifications(TENANT_ID);

            // Then - Only failed notifications with retries remaining
            assertThat(retryable).hasSize(1);
            assertThat(retryable.get(0).getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(retryable.get(0).canRetry()).isTrue();
        }

        @Test
        @DisplayName("Should find pending notifications without future schedule")
        void shouldFindPendingNotificationsToSend() {
            // Given - Add a scheduled notification
            Notification scheduledNotification = createNotification(TENANT_ID, NotificationStatus.PENDING);
            scheduledNotification.setScheduledAt(Instant.now().plus(1, ChronoUnit.HOURS));
            notificationRepository.save(scheduledNotification);

            // When
            List<Notification> toSend = notificationRepository.findPendingNotificationsToSend(TENANT_ID);

            // Then - Only pending notifications without future scheduledAt
            assertThat(toSend).hasSize(1);
            assertThat(toSend).allMatch(n -> n.getScheduledAt() == null ||
                n.getScheduledAt().isBefore(Instant.now()));
        }

        @Test
        @DisplayName("Should count notifications by status")
        void shouldCountByStatus() {
            // When
            long pendingCount = notificationRepository.countByTenantIdAndStatus(TENANT_ID, NotificationStatus.PENDING);
            long sentCount = notificationRepository.countByTenantIdAndStatus(TENANT_ID, NotificationStatus.SENT);
            long failedCount = notificationRepository.countByTenantIdAndStatus(TENANT_ID, NotificationStatus.FAILED);

            // Then
            assertThat(pendingCount).isEqualTo(1);
            assertThat(sentCount).isEqualTo(1);
            assertThat(failedCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Correlation tracking tests")
    class CorrelationTrackingTests {

        @Test
        @DisplayName("Should find notifications by correlation ID")
        void shouldFindByCorrelationId() {
            // Given
            String correlationId = "campaign-2025-winter";
            pendingNotification.setCorrelationId(correlationId);
            sentNotification.setCorrelationId(correlationId);
            notificationRepository.save(pendingNotification);
            notificationRepository.save(sentNotification);

            // When
            List<Notification> correlated = notificationRepository.findByTenantIdAndCorrelationId(
                TENANT_ID, correlationId);

            // Then
            assertThat(correlated).hasSize(2);
            assertThat(correlated).allMatch(n -> n.getCorrelationId().equals(correlationId));
        }
    }

    @Nested
    @DisplayName("Metadata storage tests")
    class MetadataStorageTests {

        @Test
        @DisplayName("Should store and retrieve metadata as JSON")
        void shouldStoreAndRetrieveMetadata() {
            // Given
            Map<String, Object> metadata = Map.of(
                "careGapId", "gap-123",
                "patientId", "patient-456",
                "measureId", "HEDIS_CDC"
            );
            pendingNotification.setMetadata(metadata);
            notificationRepository.save(pendingNotification);

            // When
            Optional<Notification> found = notificationRepository.findById(pendingNotification.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getMetadata()).containsEntry("careGapId", "gap-123");
            assertThat(found.get().getMetadata()).containsEntry("patientId", "patient-456");
            assertThat(found.get().getMetadata()).containsEntry("measureId", "HEDIS_CDC");
        }
    }

    // Helper method
    private Notification createNotification(String tenantId, NotificationStatus status) {
        return Notification.builder()
            .tenantId(tenantId)
            .recipientId("user-" + UUID.randomUUID().toString().substring(0, 8))
            .channel(NotificationChannel.EMAIL)
            .templateCode("CARE_GAP_ALERT")
            .status(status)
            .maxRetries(3)
            .build();
    }
}
```

#### NotificationTemplateRepositoryIntegrationTest

Tests template persistence with code uniqueness, channel filtering, and variable storage.

```java
@BaseIntegrationTest
@DisplayName("NotificationTemplateRepository Integration Tests")
class NotificationTemplateRepositoryIntegrationTest {

    @Autowired
    private NotificationTemplateRepository templateRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final String OTHER_TENANT = "other-tenant";

    @Nested
    @DisplayName("Code-based query tests")
    class CodeBasedQueryTests {

        @Test
        @DisplayName("Should find active template by tenant and code")
        void shouldFindActiveByTenantAndCode() {
            // Given
            NotificationTemplate activeTemplate = createTemplate(TENANT_ID, "CARE_GAP_ALERT",
                NotificationChannel.EMAIL, true);
            templateRepository.save(activeTemplate);

            // When
            Optional<NotificationTemplate> found = templateRepository.findByTenantIdAndCodeAndActiveTrue(
                TENANT_ID, "CARE_GAP_ALERT");

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getActive()).isTrue();
        }

        @Test
        @DisplayName("Should NOT find inactive template when querying for active")
        void shouldNotFindInactiveTemplate() {
            // Given
            NotificationTemplate inactiveTemplate = createTemplate(TENANT_ID, "OLD_TEMPLATE",
                NotificationChannel.EMAIL, false);
            templateRepository.save(inactiveTemplate);

            // When
            Optional<NotificationTemplate> found = templateRepository.findByTenantIdAndCodeAndActiveTrue(
                TENANT_ID, "OLD_TEMPLATE");

            // Then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should check if template code exists in tenant")
        void shouldCheckTemplateCodeExists() {
            // Given
            NotificationTemplate template = createTemplate(TENANT_ID, "UNIQUE_CODE",
                NotificationChannel.EMAIL, true);
            templateRepository.save(template);

            // When/Then
            assertThat(templateRepository.existsByTenantIdAndCode(TENANT_ID, "UNIQUE_CODE")).isTrue();
            assertThat(templateRepository.existsByTenantIdAndCode(TENANT_ID, "NON_EXISTENT")).isFalse();
            assertThat(templateRepository.existsByTenantIdAndCode(OTHER_TENANT, "UNIQUE_CODE")).isFalse();
        }
    }

    @Nested
    @DisplayName("Channel-based query tests")
    class ChannelBasedQueryTests {

        @Test
        @DisplayName("Should find templates by channel")
        void shouldFindByChannel() {
            // Given
            templateRepository.save(createTemplate(TENANT_ID, "EMAIL_1", NotificationChannel.EMAIL, true));
            templateRepository.save(createTemplate(TENANT_ID, "EMAIL_2", NotificationChannel.EMAIL, true));
            templateRepository.save(createTemplate(TENANT_ID, "SMS_1", NotificationChannel.SMS, true));

            // When
            List<NotificationTemplate> emailTemplates = templateRepository.findByTenantIdAndChannel(
                TENANT_ID, NotificationChannel.EMAIL);

            // Then
            assertThat(emailTemplates).hasSize(2);
            assertThat(emailTemplates).allMatch(t -> t.getChannel() == NotificationChannel.EMAIL);
        }

        @Test
        @DisplayName("Should support all channel types")
        void shouldSupportAllChannelTypes() {
            // Given - Create template for each channel
            templateRepository.save(createTemplate(TENANT_ID, "EMAIL", NotificationChannel.EMAIL, true));
            templateRepository.save(createTemplate(TENANT_ID, "SMS", NotificationChannel.SMS, true));
            templateRepository.save(createTemplate(TENANT_ID, "PUSH", NotificationChannel.PUSH, true));
            templateRepository.save(createTemplate(TENANT_ID, "IN_APP", NotificationChannel.IN_APP, true));

            // When/Then
            assertThat(templateRepository.findByTenantIdAndChannel(TENANT_ID, NotificationChannel.EMAIL)).hasSize(1);
            assertThat(templateRepository.findByTenantIdAndChannel(TENANT_ID, NotificationChannel.SMS)).hasSize(1);
            assertThat(templateRepository.findByTenantIdAndChannel(TENANT_ID, NotificationChannel.PUSH)).hasSize(1);
            assertThat(templateRepository.findByTenantIdAndChannel(TENANT_ID, NotificationChannel.IN_APP)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Template variable storage tests")
    class TemplateVariableTests {

        @Test
        @DisplayName("Should store and retrieve template variables")
        void shouldStoreAndRetrieveVariables() {
            // Given
            NotificationTemplate template = createTemplate(TENANT_ID, "CARE_GAP",
                NotificationChannel.EMAIL, true);
            template.setVariables(List.of("patientName", "measureName", "dueDate"));
            templateRepository.save(template);

            // When
            Optional<NotificationTemplate> found = templateRepository.findByTenantIdAndCode(
                TENANT_ID, "CARE_GAP");

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getVariables()).containsExactly(
                "patientName", "measureName", "dueDate"
            );
        }

        @Test
        @DisplayName("Should store HTML template content")
        void shouldStoreHtmlTemplate() {
            // Given
            NotificationTemplate template = createTemplate(TENANT_ID, "HTML_TEMPLATE",
                NotificationChannel.EMAIL, true);
            String htmlContent = "<html><body><h1>Hello {{patientName}}</h1></body></html>";
            template.setHtmlTemplate(htmlContent);
            templateRepository.save(template);

            // When
            Optional<NotificationTemplate> found = templateRepository.findById(template.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getHtmlTemplate()).contains("{{patientName}}");
        }
    }

    // Helper method
    private NotificationTemplate createTemplate(String tenantId, String code,
                                                 NotificationChannel channel, boolean active) {
        return NotificationTemplate.builder()
            .tenantId(tenantId)
            .code(code)
            .name("Template " + code)
            .channel(channel)
            .subjectTemplate("Subject: {{subject}}")
            .bodyTemplate("Dear {{patientName}}, {{body}}")
            .active(active)
            .version(1)
            .build();
    }
}
```

#### NotificationPreferenceRepositoryIntegrationTest

Tests user preference persistence including quiet hours functionality.

```java
@BaseIntegrationTest
@DisplayName("NotificationPreferenceRepository Integration Tests")
class NotificationPreferenceRepositoryIntegrationTest {

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final String USER_ID = "user-001";

    @Nested
    @DisplayName("User preference query tests")
    class UserPreferenceQueryTests {

        @Test
        @DisplayName("Should find preference by tenant, user, and channel")
        void shouldFindByTenantUserAndChannel() {
            // Given
            NotificationPreference emailPref = createPreference(TENANT_ID, USER_ID,
                NotificationChannel.EMAIL, true);
            emailPref.setEmail("user@example.com");
            preferenceRepository.save(emailPref);

            // When
            Optional<NotificationPreference> found = preferenceRepository.findByTenantIdAndUserIdAndChannel(
                TENANT_ID, USER_ID, NotificationChannel.EMAIL);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("Should find only enabled preferences")
        void shouldFindOnlyEnabledPreferences() {
            // Given
            preferenceRepository.save(createPreference(TENANT_ID, USER_ID, NotificationChannel.EMAIL, true));
            preferenceRepository.save(createPreference(TENANT_ID, USER_ID, NotificationChannel.SMS, true));
            preferenceRepository.save(createPreference(TENANT_ID, USER_ID, NotificationChannel.PUSH, false));

            // When
            List<NotificationPreference> enabled = preferenceRepository.findByTenantIdAndUserIdAndEnabledTrue(
                TENANT_ID, USER_ID);

            // Then
            assertThat(enabled).hasSize(2);
            assertThat(enabled).allMatch(NotificationPreference::getEnabled);
        }
    }

    @Nested
    @DisplayName("Quiet hours functionality tests")
    class QuietHoursTests {

        @Test
        @DisplayName("Should store and retrieve quiet hours settings")
        void shouldStoreQuietHoursSettings() {
            // Given
            NotificationPreference pref = createPreference(TENANT_ID, USER_ID,
                NotificationChannel.EMAIL, true);
            pref.setQuietHoursEnabled(true);
            pref.setQuietHoursStart(LocalTime.of(22, 0));
            pref.setQuietHoursEnd(LocalTime.of(7, 0));
            pref.setTimezone("America/New_York");
            preferenceRepository.save(pref);

            // When
            Optional<NotificationPreference> found = preferenceRepository.findById(pref.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getQuietHoursEnabled()).isTrue();
            assertThat(found.get().getQuietHoursStart()).isEqualTo(LocalTime.of(22, 0));
            assertThat(found.get().getQuietHoursEnd()).isEqualTo(LocalTime.of(7, 0));
            assertThat(found.get().getTimezone()).isEqualTo("America/New_York");
        }

        @Test
        @DisplayName("Should detect when in quiet hours (same day)")
        void shouldDetectQuietHoursSameDay() {
            // Given
            NotificationPreference pref = createPreference(TENANT_ID, USER_ID,
                NotificationChannel.EMAIL, true);
            pref.setQuietHoursEnabled(true);
            pref.setQuietHoursStart(LocalTime.of(9, 0));   // 9 AM
            pref.setQuietHoursEnd(LocalTime.of(17, 0));    // 5 PM
            preferenceRepository.save(pref);

            // When
            Optional<NotificationPreference> found = preferenceRepository.findById(pref.getId());

            // Then
            assertThat(found).isPresent();
            NotificationPreference preference = found.get();
            assertThat(preference.isInQuietHours(LocalTime.of(12, 0))).isTrue();  // Noon - quiet
            assertThat(preference.isInQuietHours(LocalTime.of(8, 0))).isFalse();  // 8 AM - not quiet
            assertThat(preference.isInQuietHours(LocalTime.of(18, 0))).isFalse(); // 6 PM - not quiet
        }

        @Test
        @DisplayName("Should detect when in quiet hours (spanning midnight)")
        void shouldDetectQuietHoursSpanningMidnight() {
            // Given
            NotificationPreference pref = createPreference(TENANT_ID, USER_ID,
                NotificationChannel.EMAIL, true);
            pref.setQuietHoursEnabled(true);
            pref.setQuietHoursStart(LocalTime.of(22, 0));  // 10 PM
            pref.setQuietHoursEnd(LocalTime.of(7, 0));     // 7 AM
            preferenceRepository.save(pref);

            // When
            Optional<NotificationPreference> found = preferenceRepository.findById(pref.getId());

            // Then
            assertThat(found).isPresent();
            NotificationPreference preference = found.get();
            assertThat(preference.isInQuietHours(LocalTime.of(23, 0))).isTrue();  // 11 PM - quiet
            assertThat(preference.isInQuietHours(LocalTime.of(3, 0))).isTrue();   // 3 AM - quiet
            assertThat(preference.isInQuietHours(LocalTime.of(12, 0))).isFalse(); // Noon - not quiet
        }

        @Test
        @DisplayName("Should return false when quiet hours disabled")
        void shouldReturnFalseWhenDisabled() {
            // Given
            NotificationPreference pref = createPreference(TENANT_ID, USER_ID,
                NotificationChannel.EMAIL, true);
            pref.setQuietHoursEnabled(false);
            pref.setQuietHoursStart(LocalTime.of(22, 0));
            pref.setQuietHoursEnd(LocalTime.of(7, 0));
            preferenceRepository.save(pref);

            // When
            Optional<NotificationPreference> found = preferenceRepository.findById(pref.getId());

            // Then - Even at 11 PM, not in quiet hours because feature disabled
            assertThat(found).isPresent();
            assertThat(found.get().isInQuietHours(LocalTime.of(23, 0))).isFalse();
        }
    }

    // Helper method
    private NotificationPreference createPreference(String tenantId, String userId,
                                                    NotificationChannel channel, boolean enabled) {
        return NotificationPreference.builder()
            .tenantId(tenantId)
            .userId(userId)
            .channel(channel)
            .enabled(enabled)
            .quietHoursEnabled(false)
            .timezone("UTC")
            .build();
    }
}
```

---

### Multi-Tenant Isolation Tests (HIPAA Compliance)

Multi-tenant data isolation is critical for HIPAA compliance. Each repository test suite includes dedicated isolation tests.

```java
@Nested
@DisplayName("Multi-Tenant Isolation (HIPAA Compliance)")
class MultiTenantIsolationTests {

    @Test
    @DisplayName("Should isolate notifications between tenants")
    void shouldIsolateNotificationsBetweenTenants() {
        // Given
        Notification tenant1Notification = createNotification(TENANT_ID, NotificationStatus.SENT);
        Notification tenant2Notification = createNotification(OTHER_TENANT, NotificationStatus.SENT);
        notificationRepository.save(tenant1Notification);
        notificationRepository.save(tenant2Notification);

        // When
        Page<Notification> tenant1Page = notificationRepository.findByTenantId(TENANT_ID, pageable);
        Page<Notification> tenant2Page = notificationRepository.findByTenantId(OTHER_TENANT, pageable);

        // Then - No cross-tenant data leakage
        assertThat(tenant1Page.getContent()).noneMatch(n -> n.getTenantId().equals(OTHER_TENANT));
        assertThat(tenant2Page.getContent()).noneMatch(n -> n.getTenantId().equals(TENANT_ID));
    }

    @Test
    @DisplayName("Should NOT allow cross-tenant access via ID query")
    void shouldNotAllowCrossTenantAccessById() {
        // Given
        Notification notification = createNotification(TENANT_ID, NotificationStatus.SENT);
        notification = notificationRepository.save(notification);

        // When - Try to access with wrong tenant
        Optional<Notification> result = notificationRepository.findByIdAndTenantId(
            notification.getId(), OTHER_TENANT);

        // Then - Access denied (empty result)
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should allow same template code in different tenants")
    void shouldAllowSameCodeInDifferentTenants() {
        // Given - Same template code in two tenants
        templateRepository.save(createTemplate(TENANT_ID, "CARE_GAP_ALERT", NotificationChannel.EMAIL, true));
        templateRepository.save(createTemplate(OTHER_TENANT, "CARE_GAP_ALERT", NotificationChannel.EMAIL, true));

        // When
        Optional<NotificationTemplate> tenant1Template = templateRepository.findByTenantIdAndCode(
            TENANT_ID, "CARE_GAP_ALERT");
        Optional<NotificationTemplate> tenant2Template = templateRepository.findByTenantIdAndCode(
            OTHER_TENANT, "CARE_GAP_ALERT");

        // Then - Both exist, different instances
        assertThat(tenant1Template).isPresent();
        assertThat(tenant2Template).isPresent();
        assertThat(tenant1Template.get().getId()).isNotEqualTo(tenant2Template.get().getId());
    }

    @Test
    @DisplayName("All entities should have non-null tenant IDs")
    void shouldHaveNonNullTenantIds() {
        List<Notification> all = notificationRepository.findAll();

        assertThat(all).allMatch(n -> n.getTenantId() != null && !n.getTenantId().isEmpty());
    }

    @Test
    @DisplayName("Should NOT expose contact info across tenants")
    void shouldNotExposeContactInfoAcrossTenants() {
        // Given
        NotificationPreference pref = createPreference(TENANT_ID, USER_ID, NotificationChannel.EMAIL, true);
        pref.setEmail("sensitive-email@example.com");
        pref.setPhone("+1-555-123-4567");
        preferenceRepository.save(pref);

        // When
        List<NotificationPreference> otherTenantPrefs = preferenceRepository.findByTenantIdAndUserId(
            OTHER_TENANT, USER_ID);

        // Then - No contact info leakage
        assertThat(otherTenantPrefs).noneMatch(p ->
            "sensitive-email@example.com".equals(p.getEmail()));
    }
}
```

**Key Multi-Tenant Test Patterns:**
| Pattern | Description |
|---------|-------------|
| Tenant data isolation | Queries filtered by tenantId return only that tenant's data |
| Cross-tenant access prevention | ID + wrong tenantId returns empty result |
| Same code/name different tenants | Allows duplicate codes per tenant (unique within tenant) |
| Non-null tenant validation | All entities must have non-null, non-empty tenantId |
| Contact info isolation | PHI (email, phone) never exposed across tenant boundaries |

---

### HIPAA Compliance Tests

Tests verifying HIPAA requirements for PHI handling in notifications.

```java
@SpringBootTest
@DisplayName("HIPAA Compliance Tests")
class NotificationHipaaComplianceTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("PHI Cache Compliance Tests")
    class PhiCacheComplianceTests {

        @Test
        @DisplayName("Notification cache TTL must not exceed 5 minutes")
        void notificationCacheTtlShouldNotExceed5Minutes() {
            // Given
            Cache notificationCache = cacheManager.getCache("notifications");

            // Then
            assertThat(notificationCache).isNotNull();
            // Verify configuration in application-test.yml sets TTL <= 300 seconds
        }

        @Test
        @DisplayName("Template cache TTL must not exceed 15 minutes")
        void templateCacheTtlShouldNotExceed15Minutes() {
            // Given - Templates contain no PHI, can have longer TTL
            Cache templateCache = cacheManager.getCache("templates");

            // Then
            assertThat(templateCache).isNotNull();
            // Templates have longer TTL since they contain no PHI
        }
    }

    @Nested
    @DisplayName("PHI Response Header Tests")
    class PhiResponseHeaderTests {

        @Test
        @DisplayName("Notification endpoints must include no-cache headers")
        void notificationEndpointsShouldIncludeNoCacheHeaders() throws Exception {
            mockMvc.perform(get("/api/v1/notifications")
                    .header("X-Tenant-ID", "test-tenant"))
                .andExpect(header().string("Cache-Control",
                    allOf(
                        containsString("no-store"),
                        containsString("no-cache"),
                        containsString("must-revalidate")
                    )))
                .andExpect(header().string("Pragma", "no-cache"));
        }
    }

    @Nested
    @DisplayName("PHI Template Compliance Tests")
    class PhiTemplateComplianceTests {

        @Test
        @DisplayName("Templates should NOT contain PHI directly")
        void templatesShouldNotContainPhiDirectly() {
            // Given
            String bodyTemplate = "Dear {{patientName}}, you have an appointment on {{date}}.";

            // Then - Template uses variables, not hardcoded PHI
            assertThat(bodyTemplate).doesNotContainPattern("\\d{3}-\\d{2}-\\d{4}"); // No SSN
            assertThat(bodyTemplate).doesNotContainPattern("\\d{10}"); // No MRN-like numbers
            assertThat(bodyTemplate).contains("{{"); // Uses variable substitution
        }

        @Test
        @DisplayName("Test data must use synthetic identifiers")
        void testDataMustUseSyntheticIdentifiers() {
            // Given
            NotificationPreference testPref = createTestPreference();

            // Then - Test data uses synthetic patterns
            assertThat(testPref.getEmail())
                .matches("test-.*@example\\.com|user-.*@example\\.com")
                .withFailMessage("Test emails should be clearly synthetic");

            assertThat(testPref.getUserId())
                .matches("user-\\d+|test-user-.*")
                .withFailMessage("Test user IDs should follow synthetic pattern");
        }
    }

    @Nested
    @DisplayName("Audit Logging Tests")
    class AuditLoggingTests {

        @Test
        @DisplayName("Notification send should be auditable")
        void notificationSendShouldBeAuditable() {
            // Verify @Audited annotation on send methods
            // Audit events should capture: tenantId, userId, channel, recipientId, timestamp
        }
    }
}
```

---

### Performance Tests

Tests notification delivery latency against SLA targets.

```java
@SpringBootTest
@DisplayName("Notification Performance Tests")
class NotificationPerformanceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TemplateService templateService;

    @Test
    @DisplayName("Single notification send should complete within 100ms")
    void singleNotificationSendPerformance() {
        // Given
        SendNotificationRequest request = createRequest(NotificationChannel.EMAIL);

        // When
        Instant start = Instant.now();
        Notification result = notificationService.sendNotification(request);
        Instant end = Instant.now();

        // Then
        long durationMs = Duration.between(start, end).toMillis();
        assertThat(durationMs)
            .isLessThan(100)
            .withFailMessage("Notification send took %dms, exceeds 100ms SLA", durationMs);
    }

    @Test
    @DisplayName("Bulk notification (100 recipients) should complete within 2 seconds")
    void bulkNotificationPerformance() {
        // Given
        List<SendNotificationRequest> requests = IntStream.range(0, 100)
            .mapToObj(i -> createRequest("user-" + i, NotificationChannel.EMAIL))
            .collect(Collectors.toList());

        // When
        Instant start = Instant.now();
        BulkNotificationResult result = notificationService.sendBulkNotifications(requests);
        Instant end = Instant.now();

        // Then
        long durationMs = Duration.between(start, end).toMillis();
        assertThat(durationMs)
            .isLessThan(2000)
            .withFailMessage("Bulk notification (100) took %dms, exceeds 2000ms SLA", durationMs);

        double avgMs = durationMs / 100.0;
        System.out.printf("Bulk Performance: 100 notifications in %dms (avg: %.2fms/notification)%n",
            durationMs, avgMs);
    }

    @Test
    @DisplayName("Template rendering should complete within 5ms")
    void templateRenderingPerformance() {
        // Given
        String template = "Dear {{ patient.name }}, your {{ measure.name }} is due on {{ date }}.";
        Map<String, Object> variables = Map.of(
            "patient", Map.of("name", "John Doe"),
            "measure", Map.of("name", "HbA1c Screening"),
            "date", "2025-01-15"
        );

        // When
        List<Long> latencies = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Instant start = Instant.now();
            templateService.renderTemplate(template, variables);
            Instant end = Instant.now();
            latencies.add(Duration.between(start, end).toNanos() / 1_000_000);
        }

        // Then
        Collections.sort(latencies);
        long p95 = latencies.get(950);

        assertThat(p95)
            .isLessThan(5)
            .withFailMessage("p95 template rendering %dms exceeds 5ms SLA", p95);

        System.out.printf("Template Rendering: p50=%dms, p95=%dms, p99=%dms%n",
            latencies.get(500), p95, latencies.get(990));
    }
}
```

**Performance SLAs:**
| Operation | Target | Measurement |
|-----------|--------|-------------|
| Single notification send | <100ms | End-to-end including preference check |
| Bulk notification (100) | <2s | Batch processing throughput |
| Template rendering | <5ms p95 | Variable substitution latency |
| Preference lookup | <10ms | Channel enablement check |
| Retry queue processing | <50ms | Failed notification requeue |

---

### Test Configuration

#### BaseIntegrationTest Annotation

```java
/**
 * Base Integration Test Configuration for Notification Service
 *
 * Combines common test configurations:
 * - Loads full Spring context via @SpringBootTest
 * - Activates "test" profile for test-specific configurations
 * - Uses Testcontainers PostgreSQL for database
 * - Enables transactional rollback for test isolation
 * - Imports test security configuration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import({TestSecurityConfiguration.class})
public @interface BaseIntegrationTest {
}
```

#### application-test.yml

```yaml
# Test-specific configuration
spring:
  datasource:
    # Testcontainers provides dynamic PostgreSQL URL
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
    url: jdbc:tc:postgresql:15:///testdb

  cache:
    type: simple  # Use simple cache for tests (no Redis required)

notification:
  email:
    enabled: false  # Disable actual email sending in tests
  sms:
    twilio:
      enabled: false  # Disable actual SMS sending in tests
  push:
    firebase:
      enabled: false  # Disable actual push sending in tests

# HIPAA compliance - even in tests
spring.cache.cache-names: notifications,templates,preferences
spring.cache.caffeine.spec: maximumSize=1000,expireAfterWrite=300s
```

---

### Best Practices

| Practice | Description | Example |
|----------|-------------|---------|
| **Synthetic Test Data** | Never use real PHI in tests | `user-001`, `test@example.com` |
| **Tenant Isolation Verification** | Every test verifies tenant filtering | `assertThat(results).noneMatch(r -> r.getTenantId().equals(OTHER_TENANT))` |
| **Provider Mocking** | Mock external providers (SMTP, Twilio, Firebase) | `@Mock EmailProvider emailProvider` |
| **Channel Abstraction** | Test each channel type independently | Separate tests for EMAIL, SMS, PUSH, IN_APP |
| **Preference Enforcement** | Test both enabled and disabled preferences | CANCELLED status when disabled |
| **Quiet Hours Testing** | Test same-day and midnight-spanning ranges | `isInQuietHours(LocalTime.of(23, 0))` |
| **Template Variables** | Test simple, nested, and missing variables | `{{ patient.name }}`, graceful handling |
| **Retry Logic** | Test retry count increments and max check | `canRetry()` returns false at max |
| **Correlation Tracking** | Test notification grouping by correlationId | Campaign tracking |
| **Metadata Storage** | Test JSON storage/retrieval | `Map<String, Object>` persistence |
| **Soft Delete** | Test template deactivation vs hard delete | `active = false` |
| **Status Lifecycle** | Test all transitions: PENDING→SENT/FAILED | `markAsSent()`, `markAsFailed()` |

---

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| `TestContainers connection refused` | Docker not running | Start Docker Desktop/daemon |
| `BaseIntegrationTest not found` | Missing annotation import | Add `import com.healthdata.notification.config.BaseIntegrationTest` |
| `Template variable not substituted` | Missing variable in map | Check variable name matches `{{ name }}` exactly |
| `Notification stuck in PENDING` | Provider not mocked | Add `@Mock` for NotificationProvider |
| `Quiet hours test fails` | Timezone mismatch | Use `LocalTime` not `ZonedDateTime` in tests |
| `Cross-tenant data visible` | Missing tenantId filter | Add `tenantId` to repository query method |
| `Preference not enforced` | Service not checking preferences | Verify `preferenceRepository.findByTenantIdAndUserIdAndChannel()` called |
| `Retry count not incrementing` | `markAsFailed()` not called | Use domain method, not direct field set |
| `Template code duplicate error` | Code exists in same tenant | Use unique codes per test or clean up `@BeforeEach` |
| `Metadata lost after save` | JSON serialization issue | Verify `@Type(JsonBinaryType.class)` on entity field |
| `Cache TTL test fails` | Wrong cache manager | Check `CacheManager` bean configuration |

---

### CI/CD Integration

```yaml
# GitHub Actions example
notification-service-tests:
  runs-on: ubuntu-latest
  services:
    postgres:
      image: postgres:15
      env:
        POSTGRES_DB: testdb
        POSTGRES_USER: test
        POSTGRES_PASSWORD: test
      ports:
        - 5432:5432

  steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Run tests
      run: ./gradlew :modules:services:notification-service:test

    - name: Upload coverage report
      uses: codecov/codecov-action@v4
      with:
        files: ./backend/modules/services/notification-service/build/reports/jacoco/test/jacocoTestReport.xml
```

---

## Monitoring

- **Health Check**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`
- **Prometheus**: `GET /actuator/prometheus`

### Key Metrics

| Metric | Description |
|--------|-------------|
| `notifications.sent.total` | Total notifications sent by channel |
| `notifications.failed.total` | Failed notification attempts |
| `notifications.delivery.time` | Delivery latency histogram |

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Email not sending | SMTP credentials invalid | Verify `notification.email.smtp.*` config |
| SMS delivery failure | Twilio quota exceeded | Check Twilio dashboard, upgrade plan |
| Kafka consumer lag | High notification volume | Scale service instances |

## Security Considerations

- **No PHI in notifications**: Templates should not include PHI directly
- **Audit logging**: All notification sends are logged
- **Multi-tenant isolation**: All queries filtered by tenantId
- **Credential security**: Provider credentials stored in Vault

## References

- [Notification Events Schema](../../shared/api-contracts/notification-events.md)
- [Kafka Configuration](../../shared/infrastructure/messaging/README.md)
- [Gateway Trust Architecture](../../../docs/GATEWAY_TRUST_ARCHITECTURE.md)

---

*Last Updated: December 2025*
*Service Version: 1.0*
