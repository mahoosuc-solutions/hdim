# Approval Service

Human-in-the-loop approval workflows for AI agent actions, data mutations, and clinical decisions requiring manual review.

## Overview

The Approval Service provides comprehensive workflow management for approvals requiring human oversight. It supports multiple approval types including AI agent actions, data mutations, exports, emergency access, and guardrail reviews. The service includes routing, escalation, notifications, and comprehensive audit trails.

## Key Features

### Approval Request Management
- Create approval requests with risk levels
- Assign to specific users or roles
- Auto-assignment based on request type
- Support for multiple approval types
- Correlation tracking for distributed workflows

### Approval Workflows
- Approve/reject decisions with reasoning
- Request reassignment
- Escalation to higher authority
- Expiration handling with auto-rejection
- Multi-stage approval chains

### Notification System
- Email notifications for new requests
- Expiration reminders (4 hours before)
- SMS alerts for critical requests
- Webhook integrations (n8n, Zapier)
- Real-time WebSocket updates

### Risk-Based Routing
- 4 risk levels: LOW, MEDIUM, HIGH, CRITICAL
- Risk-specific timeout durations
- Required approver roles by risk level
- Multi-approver requirements for CRITICAL
- Auto-approval for LOW risk after delay

### Analytics and Reporting
- Approval statistics by tenant
- Average approval time metrics
- Approval rate tracking
- Pending request monitoring
- Escalation frequency analysis

### Audit Trail
- Complete approval history
- State transition tracking
- Decision reasoning capture
- User attribution for all actions
- Immutable audit log

## Technology Stack

- **Spring Boot 3.x**: Core framework
- **PostgreSQL**: Approval and history storage
- **Redis**: Real-time updates and caching
- **Apache Kafka**: Event streaming
- **Spring Mail**: Email notifications
- **Thymeleaf**: Email templates
- **Flyway**: Database migrations

## API Endpoints

### Approval Requests
```
POST   /api/v1/approvals
       - Create new approval request

GET    /api/v1/approvals/{id}
       - Get specific approval request

GET    /api/v1/approvals?status={status}
       - Get all approvals for tenant

GET    /api/v1/approvals/pending?role={role}
       - Get pending approvals for role

GET    /api/v1/approvals/assigned?status={status}
       - Get approvals assigned to current user
```

### Approval Actions
```
POST   /api/v1/approvals/{id}/assign
       - Assign request to reviewer

POST   /api/v1/approvals/{id}/approve
       - Approve the request

POST   /api/v1/approvals/{id}/reject
       - Reject the request

POST   /api/v1/approvals/{id}/escalate
       - Escalate to higher authority
```

### History and Analytics
```
GET    /api/v1/approvals/{id}/history
       - Get approval history for request

GET    /api/v1/approvals/stats?days={days}
       - Get approval statistics

GET    /api/v1/approvals/expiring?hours={hours}
       - Get requests expiring soon
```

## Configuration

### Application Properties
```yaml
server.port: 8097
spring.datasource.url: jdbc:postgresql://localhost:5432/hdim_approval
hdim.approval.default-timeout-hours: 24
hdim.approval.auto-escalation-hours: 4
```

### Risk Level Configuration
```yaml
hdim.approval.risk-levels:
  LOW:
    auto-approve-delay-minutes: 30
    requires-multiple-approvers: false
  MEDIUM:
    requires-multiple-approvers: false
  HIGH:
    requires-multiple-approvers: false
    required-role: CLINICAL_SUPERVISOR
  CRITICAL:
    requires-multiple-approvers: true
    min-approvers: 2
    required-role: CLINICAL_DIRECTOR
```

### Request Type Rules
```yaml
hdim.approval.rules:
  AGENT_ACTION:
    default-risk: MEDIUM
    clinical-role-required: true
  DATA_MUTATION:
    default-risk: HIGH
    audit-required: true
  EXPORT:
    default-risk: HIGH
    compliance-review: true
  EMERGENCY_ACCESS:
    default-risk: CRITICAL
    requires-multiple-approvers: true
```

### Email Notifications
```yaml
hdim.approval.email:
  enabled: true
  from: noreply@hdim.health
  dashboard-url: http://localhost:5173/approvals
```

### Webhook Configuration
```yaml
hdim.approval.webhook:
  secret: ${WEBHOOK_SECRET}
  timeout-seconds: 30
  max-retries: 3
```

## Request Types

### Supported Request Types
- **AGENT_ACTION**: AI agent actions requiring approval
- **DATA_MUTATION**: Changes to clinical or administrative data
- **EXPORT**: Data export requests (PHI compliance)
- **WORKFLOW_DEPLOY**: Custom workflow deployment
- **DLQ_REPROCESS**: Dead letter queue reprocessing
- **GUARDRAIL_REVIEW**: AI guardrail violation review
- **CONSENT_CHANGE**: Patient consent modifications
- **EMERGENCY_ACCESS**: Break-glass emergency access

## Risk Levels

### LOW Risk
- Auto-approval after 30 minutes if no action
- Single approver required
- Standard timeout (24 hours)
- Example: DLQ reprocessing

### MEDIUM Risk
- Manual approval required
- Single approver
- Standard timeout
- Example: Agent actions

### HIGH Risk
- Clinical supervisor role required
- Single approver
- Standard timeout
- Example: Data mutations, exports

### CRITICAL Risk
- Clinical director role required
- Multiple approvers (minimum 2)
- Short timeout (4 hours)
- Example: Emergency access

## Running Locally

### Prerequisites
- Java 21
- PostgreSQL 14+
- Redis 6+
- Kafka 3+
- SMTP server (for email notifications)

### Environment Variables
```bash
export DB_PASSWORD=postgres
export WEBHOOK_SECRET=your-webhook-secret
export SMTP_USERNAME=your-email@example.com
export SMTP_PASSWORD=your-email-password
```

### Build
```bash
./gradlew :modules:services:approval-service:build
```

### Run
```bash
./gradlew :modules:services:approval-service:bootRun
```

### Run Tests
```bash
./gradlew :modules:services:approval-service:test
```

---

## Testing

### Overview

The Approval Service has comprehensive test coverage across 9 test suites validating approval workflows, routing logic, reviewer management, webhook callbacks, scheduled tasks, and event publishing.

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:approval-service:test

# Run specific test suite
./gradlew :modules:services:approval-service:test --tests "*ApprovalServiceTest"
./gradlew :modules:services:approval-service:test --tests "*ControllerTest"
./gradlew :modules:services:approval-service:test --tests "*RoutingServiceTest"

# Run with coverage
./gradlew :modules:services:approval-service:test jacocoTestReport

# Run tests with verbose output
./gradlew :modules:services:approval-service:test --info

# Run integration tests only
./gradlew :modules:services:approval-service:test --tests "*IntegrationTest"

# Run WebSocket tests
./gradlew :modules:services:approval-service:test --tests "*NotificationServiceTest"
```

### Test Coverage Summary

| Test Class | Tests | Coverage Focus |
|------------|-------|----------------|
| `ApprovalServiceTest` | 35+ | Core approval workflow (create, assign, approve, reject, escalate) |
| `ApprovalControllerTest` | 25+ | REST API endpoints, pagination, status filtering |
| `ApprovalRoutingServiceTest` | 30+ | Role determination, auto-assignment, escalation hierarchy |
| `ReviewerPoolServiceTest` | 30+ | Redis-based reviewer management, round-robin selection |
| `WebhookCallbackServiceTest` | 25+ | Webhook delivery, HMAC signatures, retry logic |
| `ExpirationReminderSchedulerTest` | 25+ | Scheduled reminders, Redis deduplication |
| `ApprovalNotificationServiceTest` | 15+ | WebSocket real-time updates |
| `ApprovalEventPublisherTest` | 10+ | Kafka event publishing |
| `EmailNotificationServiceTest` | 15+ | Email template rendering, SMTP integration |

**Total: 9 test classes, 210+ test methods**

### Test Organization

```
src/test/java/com/healthdata/approval/
├── service/
│   └── ApprovalServiceTest.java              # Core service workflow tests
├── controller/
│   └── ApprovalControllerTest.java           # REST API endpoint tests
├── routing/
│   ├── ApprovalRoutingServiceTest.java       # Role-based routing tests
│   └── ReviewerPoolServiceTest.java          # Reviewer pool management tests
├── webhook/
│   └── WebhookCallbackServiceTest.java       # Webhook callback tests
├── websocket/
│   └── ApprovalNotificationServiceTest.java  # WebSocket notification tests
├── event/
│   └── ApprovalEventPublisherTest.java       # Kafka event publishing tests
├── notification/
│   └── EmailNotificationServiceTest.java     # Email notification tests
└── scheduler/
    └── ExpirationReminderSchedulerTest.java  # Scheduled task tests
```

### Unit Tests (Service Layer)

#### Core Approval Workflow Tests

Tests the complete approval lifecycle: PENDING → ASSIGNED → APPROVED/REJECTED/ESCALATED.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalService Tests")
class ApprovalServiceTest {

    @Mock
    private ApprovalRequestRepository requestRepository;

    @Mock
    private ApprovalHistoryRepository historyRepository;

    @Mock
    private ApprovalNotificationService notificationService;

    @Mock
    private ApprovalEventPublisher eventPublisher;

    @InjectMocks
    private ApprovalService approvalService;

    @Captor
    private ArgumentCaptor<ApprovalRequest> requestCaptor;

    @Captor
    private ArgumentCaptor<ApprovalHistory> historyCaptor;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String REVIEWER_ID = "reviewer-789";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(approvalService, "defaultTimeoutHours", 24);
        ReflectionTestUtils.setField(approvalService, "autoEscalationHours", 4);
    }

    @Nested
    @DisplayName("Create Approval Request")
    class CreateApprovalRequestTests {

        @Test
        @DisplayName("Should create approval request with all fields")
        void createApprovalRequest_WithAllFields_Success() {
            // Given
            CreateApprovalRequestDTO dto = new CreateApprovalRequestDTO(
                TENANT_ID,
                RequestType.AGENT_ACTION,
                "MedicationTool",
                "patient-123",
                "EXECUTE",
                Map.of("action", "prescribe", "medication", "aspirin"),
                new BigDecimal("0.85"),
                RiskLevel.HIGH,
                USER_ID,
                "agent-runtime-service",
                "corr-123",
                "CLINICAL_REVIEWER",
                Instant.now().plus(Duration.ofHours(48))
            );

            when(requestRepository.save(any(ApprovalRequest.class)))
                .thenAnswer(invocation -> {
                    ApprovalRequest req = invocation.getArgument(0);
                    ReflectionTestUtils.setField(req, "id", UUID.randomUUID());
                    return req;
                });
            when(historyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            ApprovalRequest result = approvalService.createApprovalRequest(dto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(result.getRequestType()).isEqualTo(RequestType.AGENT_ACTION);
            assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.PENDING);

            // Verify history created
            verify(historyRepository).save(historyCaptor.capture());
            assertThat(historyCaptor.getValue().getAction()).isEqualTo(HistoryAction.CREATED);
        }

        @Test
        @DisplayName("Should use default timeout when not provided")
        void createApprovalRequest_NoExpiration_UsesDefault() {
            // Given
            CreateApprovalRequestDTO dto = new CreateApprovalRequestDTO(
                TENANT_ID, RequestType.GUARDRAIL_REVIEW, "AI_RESPONSE", null,
                "DELIVER_TO_USER", Map.of("content", "flagged"),
                null, RiskLevel.MEDIUM, "SYSTEM", "agent-runtime-service",
                null, null, null  // No expiration
            );

            when(requestRepository.save(any())).thenAnswer(i -> {
                ApprovalRequest req = i.getArgument(0);
                ReflectionTestUtils.setField(req, "id", UUID.randomUUID());
                return req;
            });
            when(historyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            Instant before = Instant.now();
            ApprovalRequest result = approvalService.createApprovalRequest(dto);

            // Then - Default 24 hours applied
            assertThat(result.getExpiresAt())
                .isAfter(before.plus(Duration.ofHours(23)))
                .isBefore(Instant.now().plus(Duration.ofHours(25)));
        }
    }

    @Nested
    @DisplayName("Approve Request")
    class ApproveRequestTests {

        @Test
        @DisplayName("Should approve pending request with decision reason")
        void approve_PendingRequest_Success() {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest request = createPendingRequest(requestId);

            when(requestRepository.findByTenantIdAndId(TENANT_ID, requestId))
                .thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(historyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            ApprovalRequest result = approvalService.approve(
                requestId, TENANT_ID, REVIEWER_ID, "Looks good");

            // Then
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
            assertThat(result.getDecisionBy()).isEqualTo(REVIEWER_ID);
            assertThat(result.getDecisionReason()).isEqualTo("Looks good");
            assertThat(result.getDecisionAt()).isNotNull();

            verify(historyRepository).save(historyCaptor.capture());
            assertThat(historyCaptor.getValue().getAction()).isEqualTo(HistoryAction.APPROVED);
        }

        @Test
        @DisplayName("Should fail to approve already approved request")
        void approve_AlreadyApproved_Fails() {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest request = createApprovedRequest(requestId);

            when(requestRepository.findByTenantIdAndId(TENANT_ID, requestId))
                .thenReturn(Optional.of(request));

            // When/Then
            assertThatThrownBy(() ->
                approvalService.approve(requestId, TENANT_ID, REVIEWER_ID, "reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot approve request");
        }

        @Test
        @DisplayName("Should fail to approve expired request")
        void approve_ExpiredRequest_Fails() {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest request = createExpiredRequest(requestId);

            when(requestRepository.findByTenantIdAndId(TENANT_ID, requestId))
                .thenReturn(Optional.of(request));

            // When/Then
            assertThatThrownBy(() ->
                approvalService.approve(requestId, TENANT_ID, REVIEWER_ID, "reason"))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Escalate Request")
    class EscalateRequestTests {

        @Test
        @DisplayName("Should escalate pending request to supervisor")
        void escalate_PendingRequest_Success() {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest request = createPendingRequest(requestId);

            when(requestRepository.findByTenantIdAndId(TENANT_ID, requestId))
                .thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(historyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            ApprovalRequest result = approvalService.escalate(
                requestId, TENANT_ID, REVIEWER_ID, "supervisor-001", "Need senior review");

            // Then
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.ESCALATED);
            assertThat(result.getEscalatedTo()).isEqualTo("supervisor-001");
            assertThat(result.getEscalationCount()).isEqualTo(1);

            verify(historyRepository).save(historyCaptor.capture());
            assertThat(historyCaptor.getValue().getAction()).isEqualTo(HistoryAction.ESCALATED);
        }

        @Test
        @DisplayName("Should fail to escalate already escalated request")
        void escalate_AlreadyEscalated_ThrowsException() {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest request = createPendingRequest(requestId);
            request.escalate("level1", "First escalation");

            when(requestRepository.findByTenantIdAndId(TENANT_ID, requestId))
                .thenReturn(Optional.of(request));

            // When/Then
            assertThatThrownBy(() -> approvalService.escalate(
                requestId, TENANT_ID, REVIEWER_ID, "level2", "Second escalation"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot escalate request in status: ESCALATED");
        }
    }

    @Nested
    @DisplayName("Statistics")
    class StatisticsTests {

        @Test
        @DisplayName("Should calculate approval stats by status")
        void getStats_Success() {
            // Given
            Instant since = Instant.now().minus(Duration.ofDays(30));

            when(requestRepository.countByTenantIdAndStatus(TENANT_ID, ApprovalStatus.PENDING))
                .thenReturn(5L);
            when(requestRepository.countByTenantIdAndStatus(TENANT_ID, ApprovalStatus.APPROVED))
                .thenReturn(42L);
            when(requestRepository.countByTenantIdAndStatus(TENANT_ID, ApprovalStatus.REJECTED))
                .thenReturn(8L);
            when(requestRepository.countByTenantIdAndStatus(TENANT_ID, ApprovalStatus.EXPIRED))
                .thenReturn(2L);
            when(requestRepository.averageDecisionTimeSeconds(TENANT_ID, since))
                .thenReturn(3600.0);
            when(requestRepository.countPendingByRiskLevel(TENANT_ID))
                .thenReturn(List.of(
                    new Object[]{RiskLevel.HIGH, 2L},
                    new Object[]{RiskLevel.MEDIUM, 3L}
                ));

            // When
            ApprovalStats stats = approvalService.getStats(TENANT_ID, since);

            // Then
            assertThat(stats.pending()).isEqualTo(5);
            assertThat(stats.approved()).isEqualTo(42);
            assertThat(stats.rejected()).isEqualTo(8);
            assertThat(stats.avgDecisionTimeSeconds()).isEqualTo(3600.0);
            assertThat(stats.pendingByRiskLevel())
                .containsEntry(RiskLevel.HIGH, 2L)
                .containsEntry(RiskLevel.MEDIUM, 3L);
        }
    }

    // Test data generators
    private ApprovalRequest createPendingRequest(UUID id) {
        ApprovalRequest request = ApprovalRequest.builder()
            .tenantId(TENANT_ID)
            .requestType(RequestType.AGENT_ACTION)
            .entityType("TestTool")
            .entityId("entity-123")
            .actionRequested("EXECUTE")
            .payload(new HashMap<>())
            .riskLevel(RiskLevel.MEDIUM)
            .status(ApprovalStatus.PENDING)
            .requestedBy(USER_ID)
            .expiresAt(Instant.now().plus(Duration.ofHours(24)))
            .build();
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }
}
```

#### Role-Based Routing Tests

Tests the approval routing logic based on request type and risk level.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalRoutingService Tests")
class ApprovalRoutingServiceTest {

    @Mock
    private ApprovalRequestRepository requestRepository;

    @Mock
    private ReviewerPoolService reviewerPoolService;

    @InjectMocks
    private ApprovalRoutingService routingService;

    private static final String TENANT_ID = "tenant-123";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(routingService, "autoEscalationHours", 4);
        ReflectionTestUtils.setField(routingService, "routingEnabled", true);
    }

    @Nested
    @DisplayName("Determine Required Role")
    class DetermineRequiredRoleTests {

        @Test
        @DisplayName("Should use explicitly assigned role when set")
        void determineRequiredRole_ExplicitRole_ReturnsAssignedRole() {
            // Given
            ApprovalRequest request = createRequest(RequestType.AGENT_ACTION, RiskLevel.HIGH);
            request.setAssignedRole("ADMIN");

            // When
            String role = routingService.determineRequiredRole(request);

            // Then
            assertThat(role).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("Should determine role for AGENT_ACTION request type")
        void determineRequiredRole_AgentAction_ReturnsClinicalReviewer() {
            ApprovalRequest request = createRequest(RequestType.AGENT_ACTION, RiskLevel.LOW);
            assertThat(routingService.determineRequiredRole(request))
                .isEqualTo("CLINICAL_REVIEWER");
        }

        @Test
        @DisplayName("Should determine role for DATA_MUTATION request type")
        void determineRequiredRole_DataMutation_ReturnsClinicalSupervisor() {
            ApprovalRequest request = createRequest(RequestType.DATA_MUTATION, RiskLevel.LOW);
            assertThat(routingService.determineRequiredRole(request))
                .isEqualTo("CLINICAL_SUPERVISOR");
        }

        @Test
        @DisplayName("Should escalate role for CRITICAL risk level")
        void determineRequiredRole_CriticalRisk_ReturnsHigherRole() {
            ApprovalRequest request = createRequest(RequestType.AGENT_ACTION, RiskLevel.CRITICAL);
            // AGENT_ACTION requires CLINICAL_REVIEWER, but CRITICAL elevates to CLINICAL_DIRECTOR
            assertThat(routingService.determineRequiredRole(request))
                .isEqualTo("CLINICAL_DIRECTOR");
        }

        @Test
        @DisplayName("Should require CLINICAL_DIRECTOR for EMERGENCY_ACCESS")
        void determineRequiredRole_EmergencyAccess_ReturnsClinicalDirector() {
            ApprovalRequest request = createRequest(RequestType.EMERGENCY_ACCESS, RiskLevel.LOW);
            assertThat(routingService.determineRequiredRole(request))
                .isEqualTo("CLINICAL_DIRECTOR");
        }
    }

    @Nested
    @DisplayName("Can Approve")
    class CanApproveTests {

        @Test
        @DisplayName("Should allow user with exact required role")
        void canApprove_ExactRole_ReturnsTrue() {
            assertThat(routingService.canApprove("CLINICAL_REVIEWER", "CLINICAL_REVIEWER"))
                .isTrue();
        }

        @Test
        @DisplayName("Should allow user with higher role")
        void canApprove_HigherRole_ReturnsTrue() {
            assertThat(routingService.canApprove("CLINICAL_SUPERVISOR", "CLINICAL_REVIEWER"))
                .isTrue();
        }

        @Test
        @DisplayName("Should deny user with lower role")
        void canApprove_LowerRole_ReturnsFalse() {
            assertThat(routingService.canApprove("CLINICAL_REVIEWER", "CLINICAL_SUPERVISOR"))
                .isFalse();
        }

        @Test
        @DisplayName("Should allow SUPER_ADMIN to approve anything")
        void canApprove_SuperAdmin_ReturnsTrue() {
            assertThat(routingService.canApprove("SUPER_ADMIN", "CLINICAL_DIRECTOR"))
                .isTrue();
        }
    }

    @Nested
    @DisplayName("Get Escalation Role")
    class GetEscalationRoleTests {

        @Test
        @DisplayName("Should escalate CLINICAL_REVIEWER to CLINICAL_SUPERVISOR")
        void getEscalationRole_ClinicalReviewer_ReturnsSupervisor() {
            assertThat(routingService.getEscalationRole("CLINICAL_REVIEWER"))
                .isEqualTo("CLINICAL_SUPERVISOR");
        }

        @Test
        @DisplayName("Should escalate CLINICAL_SUPERVISOR to CLINICAL_DIRECTOR")
        void getEscalationRole_ClinicalSupervisor_ReturnsDirector() {
            assertThat(routingService.getEscalationRole("CLINICAL_SUPERVISOR"))
                .isEqualTo("CLINICAL_DIRECTOR");
        }

        @Test
        @DisplayName("Should default to ADMIN for unknown role")
        void getEscalationRole_UnknownRole_ReturnsAdmin() {
            assertThat(routingService.getEscalationRole("UNKNOWN_ROLE"))
                .isEqualTo("ADMIN");
        }
    }
}
```

#### Reviewer Pool Service Tests

Tests Redis-based reviewer management with round-robin selection.

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReviewerPoolService Tests")
class ReviewerPoolServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ReviewerPoolService reviewerPoolService;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String ROLE = "CLINICAL_REVIEWER";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("Get Available Reviewers")
    class GetAvailableReviewersTests {

        @Test
        @DisplayName("Should get available online reviewers only")
        void getAvailableReviewers_OnlineReviewers_ReturnsOnlineOnly() {
            // Given
            String key = String.format("hdim:approval:reviewers:%s:%s", TENANT_ID, ROLE);
            Set<String> allReviewers = Set.of("user-1", "user-2", "user-3");

            when(setOperations.members(key)).thenReturn(allReviewers);
            when(valueOperations.get("hdim:approval:reviewer:status:user-1")).thenReturn("online");
            when(valueOperations.get("hdim:approval:reviewer:status:user-2")).thenReturn(null);
            when(valueOperations.get("hdim:approval:reviewer:status:user-3")).thenReturn("online");

            // When
            List<String> result = reviewerPoolService.getAvailableReviewers(TENANT_ID, ROLE);

            // Then
            assertThat(result).containsExactlyInAnyOrder("user-1", "user-3");
        }

        @Test
        @DisplayName("Should fallback to in-memory when Redis fails")
        void getAvailableReviewers_RedisFailure_UsesFallback() {
            when(setOperations.members(anyString()))
                .thenThrow(new RuntimeException("Redis error"));

            // Register via fallback first
            when(setOperations.add(anyString(), anyString()))
                .thenThrow(new RuntimeException("Redis error"));
            reviewerPoolService.registerReviewer(TENANT_ID, ROLE, USER_ID);

            // When
            List<String> result = reviewerPoolService.getAvailableReviewers(TENANT_ID, ROLE);

            // Then
            assertThat(result).contains(USER_ID);
        }
    }

    @Nested
    @DisplayName("Select Next Reviewer")
    class SelectNextReviewerTests {

        @Test
        @DisplayName("Should select reviewer using round-robin")
        void selectNextReviewer_MultipleReviewers_UsesRoundRobin() {
            // Given
            List<String> reviewers = List.of("reviewer-1", "reviewer-2", "reviewer-3");
            String key = String.format("hdim:approval:roundrobin:%s:%s", TENANT_ID, ROLE);

            when(valueOperations.increment(key)).thenReturn(0L, 1L, 2L, 3L);

            // When
            String first = reviewerPoolService.selectNextReviewer(TENANT_ID, ROLE, reviewers);
            String second = reviewerPoolService.selectNextReviewer(TENANT_ID, ROLE, reviewers);
            String third = reviewerPoolService.selectNextReviewer(TENANT_ID, ROLE, reviewers);
            String fourth = reviewerPoolService.selectNextReviewer(TENANT_ID, ROLE, reviewers);

            // Then
            assertThat(first).isEqualTo("reviewer-1");
            assertThat(second).isEqualTo("reviewer-2");
            assertThat(third).isEqualTo("reviewer-3");
            assertThat(fourth).isEqualTo("reviewer-1"); // Wraps around
        }

        @Test
        @DisplayName("Should throw when no reviewers available")
        void selectNextReviewer_NoReviewers_Throws() {
            List<String> reviewers = Collections.emptyList();

            assertThatThrownBy(() -> reviewerPoolService.selectNextReviewer(TENANT_ID, ROLE, reviewers))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No eligible reviewers available");
        }
    }

    @Nested
    @DisplayName("Update Reviewer Status")
    class UpdateReviewerStatusTests {

        @Test
        @DisplayName("Should set reviewer online status with TTL")
        void updateReviewerStatus_Online_SetsStatusWithTTL() {
            String key = String.format("hdim:approval:reviewer:status:%s", USER_ID);

            reviewerPoolService.updateReviewerStatus(USER_ID, true);

            verify(valueOperations).set(
                eq(key),
                eq("online"),
                eq(Duration.ofMinutes(30))
            );
        }

        @Test
        @DisplayName("Should remove offline reviewer status")
        void updateReviewerStatus_Offline_RemovesStatus() {
            String key = String.format("hdim:approval:reviewer:status:%s", USER_ID);

            reviewerPoolService.updateReviewerStatus(USER_ID, false);

            verify(redisTemplate).delete(key);
        }
    }
}
```

### Controller Tests (API Layer)

Tests REST API endpoints with MockMvc.

```java
@WebMvcTest({ApprovalController.class, ApprovalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ApprovalController Tests")
class ApprovalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApprovalService approvalService;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";

    @Nested
    @DisplayName("POST /api/v1/approvals")
    class CreateApprovalTests {

        @Test
        @DisplayName("Should create approval request successfully")
        void createRequest_Success() throws Exception {
            CreateRequestDTO requestDto = new CreateRequestDTO(
                RequestType.AGENT_ACTION,
                "MedicationTool",
                "patient-123",
                "EXECUTE",
                Map.of("action", "prescribe"),
                new BigDecimal("0.85"),
                RiskLevel.HIGH,
                "agent-runtime-service",
                "corr-123",
                "CLINICAL_REVIEWER",
                null
            );

            ApprovalRequest created = createSampleRequest(UUID.randomUUID());
            when(approvalService.createApprovalRequest(any())).thenReturn(created);

            mockMvc.perform(post("/api/v1/approvals")
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.requestType").value("AGENT_ACTION"))
                .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("Should fail without tenant header")
        void createRequest_MissingTenantHeader_Fails() throws Exception {
            CreateRequestDTO requestDto = new CreateRequestDTO(
                RequestType.AGENT_ACTION, "TestTool", null, "EXECUTE",
                null, null, RiskLevel.LOW, null, null, null, null
            );

            mockMvc.perform(post("/api/v1/approvals")
                    .header("X-User-Id", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/approvals/{id}/approve")
    class ApproveRequestTests {

        @Test
        @DisplayName("Should approve request with decision reason")
        void approveRequest_Success() throws Exception {
            UUID requestId = UUID.randomUUID();
            ApprovalRequest approved = createSampleRequest(requestId);
            approved.approve(USER_ID, "Looks good");

            when(approvalService.approve(requestId, TENANT_ID, USER_ID, "Looks good"))
                .thenReturn(approved);

            DecisionDTO body = new DecisionDTO("Looks good");

            mockMvc.perform(post("/api/v1/approvals/{id}/approve", requestId)
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decisionBy").value(USER_ID));
        }

        @Test
        @DisplayName("Should return 400 when approval fails")
        void approveRequest_IllegalState_Fails() throws Exception {
            UUID requestId = UUID.randomUUID();
            when(approvalService.approve(eq(requestId), eq(TENANT_ID), eq(USER_ID), any()))
                .thenThrow(new IllegalStateException("Cannot approve"));

            DecisionDTO body = new DecisionDTO("reason");

            mockMvc.perform(post("/api/v1/approvals/{id}/approve", requestId)
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/approvals/{id}/escalate")
    class EscalateRequestTests {

        @Test
        @DisplayName("Should escalate request to supervisor")
        void escalateRequest_Success() throws Exception {
            UUID requestId = UUID.randomUUID();
            ApprovalRequest escalated = createSampleRequest(requestId);
            escalated.escalate("supervisor-001", "Need senior review");

            when(approvalService.escalate(requestId, TENANT_ID, USER_ID, "supervisor-001", "Need senior review"))
                .thenReturn(escalated);

            EscalateDTO body = new EscalateDTO("supervisor-001", "Need senior review");

            mockMvc.perform(post("/api/v1/approvals/{id}/escalate", requestId)
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ESCALATED"))
                .andExpect(jsonPath("$.escalatedTo").value("supervisor-001"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/approvals/stats")
    class GetStatsTests {

        @Test
        @DisplayName("Should get approval statistics")
        void getStats_Success() throws Exception {
            ApprovalStats stats = new ApprovalStats(
                5, 3, 42, 8, 2, 1, 3600.0,
                Map.of(RiskLevel.HIGH, 2L, RiskLevel.MEDIUM, 3L)
            );

            when(approvalService.getStats(eq(TENANT_ID), any(Instant.class))).thenReturn(stats);

            mockMvc.perform(get("/api/v1/approvals/stats")
                    .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(5))
                .andExpect(jsonPath("$.approved").value(42))
                .andExpect(jsonPath("$.avgDecisionTimeSeconds").value(3600.0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/approvals/{id}/history")
    class GetHistoryTests {

        @Test
        @DisplayName("Should get approval history in order")
        void getHistory_Success() throws Exception {
            UUID requestId = UUID.randomUUID();
            List<ApprovalHistory> history = List.of(
                createHistory(requestId, HistoryAction.CREATED),
                createHistory(requestId, HistoryAction.APPROVED)
            );

            when(approvalService.getHistory(requestId)).thenReturn(history);

            mockMvc.perform(get("/api/v1/approvals/{id}/history", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].action").value("CREATED"))
                .andExpect(jsonPath("$[1].action").value("APPROVED"));
        }
    }
}
```

### Webhook Callback Tests

Tests webhook delivery with HMAC signatures and retry logic.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookCallbackService Tests")
class WebhookCallbackServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WebhookCallbackService webhookService;

    private static final String TENANT_ID = "tenant-123";
    private static final String CALLBACK_URL = "https://webhook.example.com/callback";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(webhookService, "webhookSecret", "test-secret");
        ReflectionTestUtils.setField(webhookService, "timeoutSeconds", 30);
        ReflectionTestUtils.setField(webhookService, "maxRetries", 3);
    }

    @Nested
    @DisplayName("Send Decision Callback")
    class SendDecisionCallbackTests {

        @Test
        @DisplayName("Should skip when no callback URL present")
        void sendDecisionCallback_NoUrl_Skips() {
            ApprovalRequest request = createRequest();
            request.setPayload(new HashMap<>()); // No callback URL

            webhookService.sendDecisionCallback(request);

            verify(webClientBuilder, never()).build();
        }

        @Test
        @DisplayName("Should compute HMAC signature when secret configured")
        void sendDecisionCallback_ComputesSignature() throws Exception {
            ReflectionTestUtils.setField(webhookService, "webhookSecret", "my-secret-key");
            ApprovalRequest request = createApprovedRequest();
            setupWebClientMock();

            when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"data\"}");
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("OK"));

            ArgumentCaptor<String> signatureCaptor = ArgumentCaptor.forClass(String.class);

            webhookService.sendDecisionCallback(request);

            verify(requestBodySpec).header(eq("X-HDIM-Signature"), signatureCaptor.capture());
            assertThat(signatureCaptor.getValue()).startsWith("sha256=");
            assertThat(signatureCaptor.getValue()).isNotEqualTo("sha256=unsigned");
        }

        @Test
        @DisplayName("Should use unsigned signature when no secret")
        void sendDecisionCallback_NoSecret_UnsignedSignature() throws Exception {
            ReflectionTestUtils.setField(webhookService, "webhookSecret", "");
            ApprovalRequest request = createApprovedRequest();
            setupWebClientMock();

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("OK"));

            ArgumentCaptor<String> signatureCaptor = ArgumentCaptor.forClass(String.class);

            webhookService.sendDecisionCallback(request);

            verify(requestBodySpec).header(eq("X-HDIM-Signature"), signatureCaptor.capture());
            assertThat(signatureCaptor.getValue()).isEqualTo("sha256=unsigned");
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle HTTP error gracefully")
        void sendDecisionCallback_HttpError_HandlesGracefully() throws Exception {
            ApprovalRequest request = createApprovedRequest();
            setupWebClientMock();

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new RuntimeException("HTTP 500")));

            assertThatCode(() -> webhookService.sendDecisionCallback(request))
                .doesNotThrowAnyException();
        }
    }
}
```

### Scheduled Task Tests

Tests expiration reminder scheduler with Redis deduplication.

```java
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
        @DisplayName("Should send reminder for request expiring within 1 hour")
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
                eq(request), eq(ASSIGNEE_EMAIL), eq(ASSIGNEE_EMAIL)
            );
            verify(valueOperations).set(
                contains(request.getId().toString()),
                eq("sent"),
                eq(Duration.ofHours(24).toSeconds()),
                eq(TimeUnit.SECONDS)
            );
        }

        @Test
        @DisplayName("Should skip reminder when already sent (Redis deduplication)")
        void sendExpirationReminders_AlreadySent_Skips() {
            ApprovalRequest request = createExpiringRequest(Duration.ofMinutes(55));
            request.setAssignedTo(ASSIGNEE_EMAIL);

            when(requestRepository.findExpiringSoonAllTenants(any()))
                .thenReturn(List.of(request));
            when(redisTemplate.hasKey(anyString())).thenReturn(true);

            scheduler.sendExpirationReminders();

            verify(emailNotificationService, never())
                .sendExpirationReminderNotification(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should skip reminder when no assignee")
        void sendExpirationReminders_NoAssignee_Skips() {
            ApprovalRequest request = createExpiringRequest(Duration.ofMinutes(55));
            request.setAssignedTo(null);

            when(requestRepository.findExpiringSoonAllTenants(any()))
                .thenReturn(List.of(request));

            scheduler.sendExpirationReminders();

            verify(emailNotificationService, never())
                .sendExpirationReminderNotification(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should skip when reminders disabled")
        void sendExpirationReminders_Disabled_Skips() {
            ReflectionTestUtils.setField(scheduler, "reminderEnabled", false);

            scheduler.sendExpirationReminders();

            verify(requestRepository, never()).findExpiringSoonAllTenants(any());
        }

        @Test
        @DisplayName("Should continue on individual reminder failure")
        void sendExpirationReminders_OneFailure_ContinuesProcessing() {
            ApprovalRequest request1 = createExpiringRequest(Duration.ofMinutes(55));
            request1.setAssignedTo("user1@test.com");
            ApprovalRequest request2 = createExpiringRequest(Duration.ofHours(2));
            request2.setAssignedTo("user2@test.com");

            when(requestRepository.findExpiringSoonAllTenants(any()))
                .thenReturn(List.of(request1, request2));
            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            doThrow(new RuntimeException("Email error"))
                .doNothing()
                .when(emailNotificationService)
                .sendExpirationReminderNotification(any(), anyString(), anyString());

            scheduler.sendExpirationReminders();

            // Both should be attempted
            verify(emailNotificationService, times(2))
                .sendExpirationReminderNotification(any(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Redis Integration")
    class RedisIntegrationTests {

        @Test
        @DisplayName("Should fallback to sending when Redis unavailable")
        void redisCheck_Unavailable_SendsReminder() {
            ApprovalRequest request = createExpiringRequest(Duration.ofMinutes(55));
            request.setAssignedTo(ASSIGNEE_EMAIL);

            when(requestRepository.findExpiringSoonAllTenants(any()))
                .thenReturn(List.of(request));
            when(redisTemplate.hasKey(anyString()))
                .thenThrow(new RuntimeException("Redis error"));

            scheduler.sendExpirationReminders();

            verify(emailNotificationService).sendExpirationReminderNotification(
                any(), eq(ASSIGNEE_EMAIL), eq(ASSIGNEE_EMAIL)
            );
        }
    }

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
```

### Multi-Tenant Isolation Tests

Verifies tenant data isolation in approval workflows.

```java
@SpringBootTest
@Testcontainers
@DisplayName("Approval Multi-Tenant Isolation Tests")
class ApprovalMultiTenantIsolationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private ApprovalRequestRepository requestRepository;

    @Autowired
    private ApprovalService approvalService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    @DisplayName("Should isolate approval requests by tenant")
    void shouldIsolateApprovalRequestsByTenant() {
        // Given
        String tenant1 = "tenant-001";
        String tenant2 = "tenant-002";

        ApprovalRequest request1 = createAndSaveRequest(tenant1, "entity-1");
        ApprovalRequest request2 = createAndSaveRequest(tenant2, "entity-2");
        ApprovalRequest request3 = createAndSaveRequest(tenant1, "entity-3");

        // When
        Page<ApprovalRequest> tenant1Requests = approvalService.getAllForTenant(
            tenant1, null, PageRequest.of(0, 100));
        Page<ApprovalRequest> tenant2Requests = approvalService.getAllForTenant(
            tenant2, null, PageRequest.of(0, 100));

        // Then
        assertThat(tenant1Requests.getContent())
            .hasSize(2)
            .extracting(ApprovalRequest::getTenantId)
            .containsOnly(tenant1);

        assertThat(tenant2Requests.getContent())
            .hasSize(1)
            .extracting(ApprovalRequest::getTenantId)
            .containsOnly(tenant2);

        // Verify no cross-tenant leakage
        assertThat(tenant1Requests.getContent()).doesNotContain(request2);
        assertThat(tenant2Requests.getContent()).doesNotContain(request1, request3);
    }

    @Test
    @DisplayName("Should not allow cross-tenant approval access")
    void shouldNotAllowCrossTenantAccess() {
        // Given
        String tenant1 = "tenant-001";
        String tenant2 = "tenant-002";
        ApprovalRequest request = createAndSaveRequest(tenant1, "entity-1");

        // When/Then - Attempting to access tenant1's request with tenant2 should fail
        assertThatThrownBy(() -> approvalService.approve(
            request.getId(), tenant2, "reviewer-123", "reason"))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("Should isolate statistics by tenant")
    void shouldIsolateStatisticsByTenant() {
        // Given
        String tenant1 = "tenant-stats-001";
        String tenant2 = "tenant-stats-002";

        createAndSaveRequest(tenant1, "entity-1");
        createAndSaveRequest(tenant1, "entity-2");
        createAndSaveRequest(tenant2, "entity-3");

        // When
        ApprovalStats tenant1Stats = approvalService.getStats(
            tenant1, Instant.now().minus(Duration.ofDays(30)));
        ApprovalStats tenant2Stats = approvalService.getStats(
            tenant2, Instant.now().minus(Duration.ofDays(30)));

        // Then
        assertThat(tenant1Stats.pending()).isEqualTo(2);
        assertThat(tenant2Stats.pending()).isEqualTo(1);
    }
}
```

### HIPAA Compliance Tests

Validates PHI-related approval handling.

```java
@SpringBootTest
@DisplayName("Approval HIPAA Compliance Tests")
class ApprovalHipaaComplianceTest {

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    @DisplayName("Should audit approval decisions for PHI access requests")
    void shouldAuditPhiAccessApprovals() {
        // Given
        CreateApprovalRequestDTO dto = new CreateApprovalRequestDTO(
            "tenant-hipaa-001",
            RequestType.DATA_MUTATION,
            "Patient",
            "patient-phi-123",
            "UPDATE",
            Map.of("field", "diagnosis"),
            null,
            RiskLevel.HIGH,
            "clinician-001",
            "fhir-service",
            null,
            "CLINICAL_SUPERVISOR",
            null
        );

        // When
        ApprovalRequest request = approvalService.createApprovalRequest(dto);
        approvalService.approve(request.getId(), "tenant-hipaa-001", "supervisor-001", "Verified");

        // Then - Verify audit trail exists
        List<AuditEvent> events = auditEventRepository.findByResourceId(request.getId().toString());
        assertThat(events).isNotEmpty();
        assertThat(events).anyMatch(e -> e.getEventType().equals("APPROVAL_DECISION"));
    }

    @Test
    @DisplayName("Should require decision reasoning for HIGH risk approvals")
    void shouldRequireReasoningForHighRiskApprovals() {
        // Given
        CreateApprovalRequestDTO dto = new CreateApprovalRequestDTO(
            "tenant-hipaa-002",
            RequestType.EXPORT,
            "PatientList",
            "export-123",
            "EXPORT_PHI",
            Map.of("recordCount", 1000),
            null,
            RiskLevel.HIGH,
            "analyst-001",
            "export-service",
            null,
            "CLINICAL_SUPERVISOR",
            null
        );

        ApprovalRequest request = approvalService.createApprovalRequest(dto);

        // When
        ApprovalRequest approved = approvalService.approve(
            request.getId(), "tenant-hipaa-002", "supervisor-001", "Verified patient consent");

        // Then
        assertThat(approved.getDecisionReason()).isNotBlank();
        assertThat(approved.getDecisionBy()).isNotBlank();
        assertThat(approved.getDecisionAt()).isNotNull();
    }

    @Test
    @DisplayName("Should track immutable approval history")
    void shouldTrackImmutableApprovalHistory() {
        // Given
        CreateApprovalRequestDTO dto = new CreateApprovalRequestDTO(
            "tenant-hipaa-003",
            RequestType.EMERGENCY_ACCESS,
            "PatientChart",
            "chart-456",
            "BREAK_GLASS",
            Map.of("reason", "Emergency treatment"),
            null,
            RiskLevel.CRITICAL,
            "clinician-emergency",
            "access-service",
            null,
            "CLINICAL_DIRECTOR",
            null
        );

        // When
        ApprovalRequest request = approvalService.createApprovalRequest(dto);
        approvalService.approve(
            request.getId(), "tenant-hipaa-003", "director-001", "Emergency confirmed");

        // Then
        List<ApprovalHistory> history = approvalService.getHistory(request.getId());
        assertThat(history).hasSize(2); // CREATED + APPROVED
        assertThat(history.get(0).getAction()).isEqualTo(HistoryAction.CREATED);
        assertThat(history.get(1).getAction()).isEqualTo(HistoryAction.APPROVED);

        // Verify timestamps are preserved
        assertThat(history.get(0).getCreatedAt()).isBefore(history.get(1).getCreatedAt());
    }

    @Test
    @DisplayName("Should use synthetic test data (no real PHI)")
    void shouldUseSyntheticTestData() {
        // Verify test data generators don't create realistic PHI
        CreateApprovalRequestDTO dto = createTestApprovalRequest();

        assertThat(dto.tenantId()).startsWith("tenant-");
        assertThat(dto.entityId()).matches("entity-\\d+|patient-\\d+|test-.*");
        assertThat(dto.requestedBy()).matches("user-\\d+|clinician-.*|system");
    }
}
```

### Performance Tests

Benchmarks approval workflow performance.

```java
@SpringBootTest
@Testcontainers
@DisplayName("Approval Performance Tests")
class ApprovalPerformanceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Autowired
    private ApprovalService approvalService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Test
    @DisplayName("Should create approval request within 100ms")
    void createApprovalRequest_ShouldCompleteWithin100ms() {
        // Given
        int iterations = 100;
        List<Long> latencies = new ArrayList<>();

        // When
        for (int i = 0; i < iterations; i++) {
            CreateApprovalRequestDTO dto = createTestRequest(i);

            Instant start = Instant.now();
            approvalService.createApprovalRequest(dto);
            Instant end = Instant.now();

            latencies.add(Duration.between(start, end).toMillis());
        }

        // Then
        Collections.sort(latencies);
        long p50 = latencies.get(iterations / 2);
        long p95 = latencies.get((int) (iterations * 0.95));
        long p99 = latencies.get((int) (iterations * 0.99));

        assertThat(p95).isLessThan(100L)
            .withFailMessage("p95 latency %dms exceeds 100ms SLA", p95);

        System.out.printf("Create Request Performance: p50=%dms, p95=%dms, p99=%dms%n",
            p50, p95, p99);
    }

    @Test
    @DisplayName("Should approve request within 50ms")
    void approveRequest_ShouldCompleteWithin50ms() {
        // Given
        int iterations = 100;
        List<Long> latencies = new ArrayList<>();

        // Create requests first
        List<UUID> requestIds = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            ApprovalRequest request = approvalService.createApprovalRequest(createTestRequest(i));
            requestIds.add(request.getId());
        }

        // When
        for (int i = 0; i < iterations; i++) {
            UUID requestId = requestIds.get(i);

            Instant start = Instant.now();
            approvalService.approve(requestId, "tenant-perf-001", "reviewer-001", "Approved");
            Instant end = Instant.now();

            latencies.add(Duration.between(start, end).toMillis());
        }

        // Then
        Collections.sort(latencies);
        long p95 = latencies.get((int) (iterations * 0.95));

        assertThat(p95).isLessThan(50L)
            .withFailMessage("p95 approval latency %dms exceeds 50ms SLA", p95);

        System.out.printf("Approve Request Performance: p95=%dms%n", p95);
    }

    @Test
    @DisplayName("Should query pending requests within 200ms")
    void queryPendingRequests_ShouldCompleteWithin200ms() {
        // Given - Create 1000 requests
        for (int i = 0; i < 1000; i++) {
            approvalService.createApprovalRequest(createTestRequest(i));
        }

        // When
        Instant start = Instant.now();
        Page<ApprovalRequest> result = approvalService.getPendingForUser(
            "tenant-perf-001", "CLINICAL_REVIEWER", PageRequest.of(0, 100));
        Instant end = Instant.now();

        // Then
        long latency = Duration.between(start, end).toMillis();
        assertThat(latency).isLessThan(200L)
            .withFailMessage("Query latency %dms exceeds 200ms SLA", latency);

        System.out.printf("Query Performance: %d results in %dms%n",
            result.getTotalElements(), latency);
    }

    private CreateApprovalRequestDTO createTestRequest(int index) {
        return new CreateApprovalRequestDTO(
            "tenant-perf-001",
            RequestType.AGENT_ACTION,
            "TestTool-" + index,
            "entity-" + index,
            "EXECUTE",
            Map.of("index", index),
            null,
            RiskLevel.MEDIUM,
            "user-perf-001",
            "perf-test-service",
            null,
            "CLINICAL_REVIEWER",
            Instant.now().plus(Duration.ofHours(24))
        );
    }
}
```

### Best Practices

| Practice | Description |
|----------|-------------|
| **Synthetic Test Data** | Use `tenant-test-xxx`, `user-test-xxx` patterns - never real PHI |
| **Multi-Tenant Testing** | Always verify tenant isolation in repository queries |
| **State Machine Testing** | Test all valid state transitions (PENDING→ASSIGNED→APPROVED) |
| **Redis Fallback Testing** | Verify graceful degradation when Redis unavailable |
| **Expiration Testing** | Use `ReflectionTestUtils` to set past `expiresAt` timestamps |
| **Role Hierarchy Testing** | Test both exact role match and elevated role scenarios |
| **Webhook Testing** | Verify HMAC signature computation and retry behavior |
| **Audit Trail Testing** | Verify `ApprovalHistory` records created for each state change |
| **Gateway Trust Headers** | Use `X-Tenant-Id`, `X-User-Id` headers in controller tests |
| **ArgumentCaptor Usage** | Capture and verify complex objects passed to mocks |

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| `NoSuchElementException` in tests | Request not found for tenant | Verify tenant ID matches in mock setup |
| `IllegalStateException` on approve | Invalid state transition | Check request status before approve/reject |
| Redis connection failures | TestContainers not started | Add `@Testcontainers` and Redis container |
| Webhook signature mismatch | Secret not configured | Set `webhookSecret` via `ReflectionTestUtils` |
| Expiration reminder not sent | Already sent (Redis key exists) | Mock `redisTemplate.hasKey()` to return `false` |
| Round-robin not working | Redis counter not incrementing | Mock `valueOperations.increment()` with sequence |
| Role escalation test fails | Escalation hierarchy not configured | Set `routingEnabled` and `autoEscalationHours` |
| History not recorded | `historyRepository.save()` not mocked | Add mock for history repository |
| Async webhook tests flaky | Not waiting for async completion | Add `Thread.sleep()` or use `Awaitility` |
| Statistics test fails | Count queries return null | Mock all status count queries |

---

## Notification Templates

### Email Templates
- New approval request notification
- Request assigned notification
- Expiration reminder (4 hours before)
- Approval/rejection confirmation
- Escalation notification

### Template Location
```
src/main/resources/templates/
├── approval-request.html
├── approval-assigned.html
├── approval-reminder.html
└── approval-decision.html
```

## Integration

### Kafka Events
Published events:
- `approval.request.created`
- `approval.request.assigned`
- `approval.decision.made`
- `approval.escalated`
- `approval.expired`

### Webhook Integration
- n8n workflow triggers
- Zapier actions
- Custom webhook handlers
- Retry with exponential backoff

### Service Integrations
This service is called by:
- **Agent Runtime Service**: AI action approvals
- **FHIR Service**: Data mutation approvals
- **Export Service**: PHI export approvals
- **Access Control Service**: Emergency access

## Scheduled Tasks

### Background Jobs
- Expiration check (every minute)
- Auto-escalation (every 5 minutes)
- Reminder notifications (every hour)
- Statistics aggregation (daily)

## Security

### Access Control
- JWT-based authentication
- Role-based approval routing
- Tenant isolation
- User attribution on all actions
- IP address logging

### Audit Compliance
- Immutable approval history
- Decision reasoning required
- Timestamp tracking (created, assigned, decided)
- State transition log
- HIPAA-compliant audit trail

## Metrics and Monitoring

### Key Metrics
- Pending approval count by risk level
- Average time to decision
- Approval/rejection rate
- Escalation rate
- Expiration rate

### Actuator Endpoints
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## API Documentation

Swagger UI available at:
```
http://localhost:8097/swagger-ui.html
```

OpenAPI specification:
```
http://localhost:8097/api-docs
```

## Best Practices

### Request Creation
- Provide detailed context in payload
- Set appropriate risk level
- Include correlation ID for tracing
- Set reasonable expiration times

### Approval Decisions
- Always provide decision reasoning
- Document any concerns or conditions
- Escalate when uncertain
- Respond promptly to critical requests

### Configuration
- Adjust timeout based on organizational needs
- Configure notification channels appropriately
- Set up role-based routing rules
- Monitor approval metrics regularly

## License

Copyright (c) 2024 Mahoosuc Solutions
