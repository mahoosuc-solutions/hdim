# Sales Automation Service

## Overview

The Sales Automation Service provides CRM and sales pipeline management for HDIM's go-to-market operations. It handles lead capture, account management, opportunity tracking, email sequences, and integrations with external platforms like Zoho CRM and LinkedIn.

## Responsibilities

- Capture and qualify leads from landing pages and marketing channels
- Manage accounts, contacts, and opportunities
- Track sales pipeline and forecast revenue
- Automate email sequences for lead nurturing
- Integrate with Zoho CRM for bidirectional sync
- Track email engagement (opens, clicks)
- Provide sales dashboard and analytics
- Log all sales activities for audit trail

## Technology Stack

| Component | Technology | Version | Why This Choice |
|-----------|------------|---------|-----------------|
| Runtime | Java | 21 LTS | Platform standard |
| Framework | Spring Boot | 3.x | Enterprise integrations |
| Database | PostgreSQL | 15 | Relational data model for CRM |
| Cache | Redis | 7 | Session and rate limiting |
| External | Zoho CRM API | v2 | CRM sync |
| External | LinkedIn API | v2 | Social selling integration |

## API Endpoints

### Leads

#### POST /api/v1/leads/capture
**Purpose**: Capture lead from landing page (public endpoint)
**Auth Required**: No (rate limited)
**Request Body**:
```json
{
  "email": "prospect@healthsystem.org",
  "name": "Jane Smith",
  "organization": "Regional Health System",
  "source": "LANDING_PAGE",
  "utmSource": "google",
  "utmCampaign": "hedis-2025"
}
```

#### GET /api/v1/leads
**Purpose**: List leads with filtering
**Auth Required**: Yes (roles: SALES, ADMIN)

#### PUT /api/v1/leads/{id}/qualify
**Purpose**: Qualify or disqualify lead
**Auth Required**: Yes (roles: SALES, ADMIN)

### Accounts

#### GET /api/v1/accounts
**Purpose**: List accounts
**Auth Required**: Yes (roles: SALES, ADMIN)

#### POST /api/v1/accounts
**Purpose**: Create account
**Auth Required**: Yes (roles: SALES, ADMIN)

#### GET /api/v1/accounts/{id}
**Purpose**: Get account details with contacts and opportunities
**Auth Required**: Yes (roles: SALES, ADMIN)

### Opportunities

#### GET /api/v1/opportunities
**Purpose**: List opportunities with pipeline stage filtering
**Auth Required**: Yes (roles: SALES, ADMIN)

#### POST /api/v1/opportunities
**Purpose**: Create opportunity
**Auth Required**: Yes (roles: SALES, ADMIN)

#### PUT /api/v1/opportunities/{id}/stage
**Purpose**: Move opportunity to new pipeline stage
**Auth Required**: Yes (roles: SALES, ADMIN)

### Pipeline

#### GET /api/v1/pipeline/summary
**Purpose**: Get pipeline summary by stage
**Auth Required**: Yes (roles: SALES, ADMIN)

#### GET /api/v1/pipeline/forecast
**Purpose**: Get revenue forecast
**Auth Required**: Yes (roles: ADMIN)

### Email Sequences

#### GET /api/v1/email-sequences
**Purpose**: List email sequences
**Auth Required**: Yes (roles: SALES, ADMIN)

#### POST /api/v1/email-sequences/{id}/enroll
**Purpose**: Enroll contact in email sequence
**Auth Required**: Yes (roles: SALES, ADMIN)

### Dashboard

#### GET /api/v1/dashboard/metrics
**Purpose**: Get sales dashboard metrics
**Auth Required**: Yes (roles: SALES, ADMIN)

### Webhooks

#### POST /api/v1/webhooks/zoho
**Purpose**: Receive Zoho CRM webhook events
**Auth Required**: Webhook signature validation

## Database Schema

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| leads | Lead records | id, email, name, organization, status, source, created_at |
| accounts | Account/company records | id, name, industry, size, owner_id |
| contacts | Contact records | id, account_id, email, name, title |
| opportunities | Sales opportunities | id, account_id, name, stage, amount, close_date |
| activities | Sales activities | id, type, subject, related_to_type, related_to_id |
| email_sequences | Email sequence definitions | id, name, steps |
| email_sequence_enrollments | Sequence enrollments | id, sequence_id, contact_id, current_step |
| email_tracking_events | Email engagement tracking | id, email_id, event_type, timestamp |

## Pipeline Stages

| Stage | Description | Probability |
|-------|-------------|-------------|
| PROSPECTING | Initial outreach | 10% |
| QUALIFICATION | Qualifying fit | 20% |
| DEMO | Demo scheduled/completed | 40% |
| PROPOSAL | Proposal sent | 60% |
| NEGOTIATION | Contract negotiation | 80% |
| CLOSED_WON | Deal won | 100% |
| CLOSED_LOST | Deal lost | 0% |

## Kafka Topics

### Publishes

| Topic | Event Type | Payload |
|-------|------------|---------|
| sales.lead.created | LeadCreatedEvent | New lead details |
| sales.opportunity.stage-changed | OpportunityStageChangedEvent | Stage transition |
| sales.deal.closed | DealClosedEvent | Won/lost deal details |

### Consumes

| Topic | Event Type | Handler |
|-------|------------|---------|
| notification.email.opened | EmailOpenedEvent | Updates email tracking |
| notification.email.clicked | EmailClickedEvent | Updates email tracking |

## External Integrations

### Zoho CRM

- **Sync Direction**: Bidirectional
- **Entities Synced**: Leads, Accounts, Contacts, Deals
- **Sync Frequency**: Real-time via webhooks + hourly full sync
- **Configuration**: `zoho.crm.*` properties

### LinkedIn

- **Features**: Profile lookup, connection requests, InMail
- **Rate Limits**: Respected per LinkedIn API guidelines
- **Configuration**: `linkedin.*` properties

## Configuration

```yaml
# application.yml
server:
  port: 8106

spring:
  datasource:
    url: jdbc:postgresql://localhost:5435/healthdata_sales

zoho:
  crm:
    enabled: true
    client-id: ${ZOHO_CLIENT_ID}
    client-secret: ${ZOHO_CLIENT_SECRET}
    refresh-token: ${ZOHO_REFRESH_TOKEN}
    webhook-secret: ${ZOHO_WEBHOOK_SECRET}

linkedin:
  enabled: true
  client-id: ${LINKEDIN_CLIENT_ID}
  client-secret: ${LINKEDIN_CLIENT_SECRET}

email:
  tracking:
    base-url: https://track.healthdatainmotion.com
```

## Testing

### Overview

The Sales Automation Service has comprehensive test coverage across 8 test suites covering CRM operations, pipeline management, email sequences, external integrations, and multi-tenant isolation. Tests are designed to validate business logic without requiring actual Zoho CRM or LinkedIn API access.

**Test Types**:
- **Unit Tests**: Service layer logic with mocked dependencies
- **Controller Tests**: REST API endpoints with MockMvc
- **Multi-Tenant Isolation Tests**: Tenant data separation verification
- **RBAC Tests**: Role-based access control (SALES, ADMIN)
- **Rate Limiting Tests**: Public endpoint abuse prevention
- **Integration Tests**: Zoho webhook handling, email tracking events
- **Performance Tests**: Lead capture throughput, pipeline calculations

### Quick Start

```bash
# Run all unit tests
./gradlew :modules:services:sales-automation-service:test

# Run specific test class
./gradlew :modules:services:sales-automation-service:test --tests "LeadServiceTest"
./gradlew :modules:services:sales-automation-service:test --tests "OpportunityServiceTest"
./gradlew :modules:services:sales-automation-service:test --tests "EmailSequenceServiceTest"

# Run tests by pattern
./gradlew :modules:services:sales-automation-service:test --tests "*ServiceTest"
./gradlew :modules:services:sales-automation-service:test --tests "*ControllerTest"

# Run with coverage report
./gradlew :modules:services:sales-automation-service:test jacocoTestReport

# Run integration tests (requires Docker)
./gradlew :modules:services:sales-automation-service:integrationTest

# Run all tests with verbose output
./gradlew :modules:services:sales-automation-service:test --info
```

### Test Coverage Summary

| Test Class | Purpose | Test Methods | Key Scenarios |
|------------|---------|--------------|---------------|
| `LeadServiceTest` | Lead capture and conversion | 35+ | Capture, qualify, convert, score filtering |
| `OpportunityServiceTest` | Opportunity pipeline management | 40+ | CRUD, stage transitions, metrics calculation |
| `EmailSequenceServiceTest` | Email automation workflows | 30+ | Sequence CRUD, enrollment, activation |
| `AccountServiceTest` | Account/company management | 25+ | CRUD, contact linking, opportunity association |
| `ContactServiceTest` | Contact record management | 20+ | CRUD, account relationship, sequence enrollment |
| `ActivityServiceTest` | Sales activity logging | 15+ | Activity creation, timeline, filtering |
| `PipelineServiceTest` | Pipeline analytics | 20+ | Stage summaries, forecasting, win rates |
| `DashboardServiceTest` | Dashboard metrics aggregation | 15+ | KPI calculation, trend analysis |

### Test Organization

```
src/test/java/com/healthdata/sales/
├── service/
│   ├── LeadServiceTest.java              # Lead capture, qualification, conversion
│   ├── OpportunityServiceTest.java       # Pipeline management, stage transitions
│   ├── EmailSequenceServiceTest.java     # Email automation workflows
│   ├── AccountServiceTest.java           # Account management
│   ├── ContactServiceTest.java           # Contact management
│   ├── ActivityServiceTest.java          # Activity logging
│   ├── PipelineServiceTest.java          # Pipeline analytics
│   └── DashboardServiceTest.java         # Dashboard metrics
├── controller/
│   ├── LeadControllerTest.java           # Lead API endpoints
│   ├── OpportunityControllerTest.java    # Opportunity API endpoints
│   └── WebhookControllerTest.java        # Zoho webhook handling
├── integration/
│   ├── ZohoClientTest.java               # Zoho CRM integration
│   └── LinkedInClientTest.java           # LinkedIn integration
├── security/
│   ├── RateLimitingTest.java             # Lead capture rate limiting
│   └── RbacTest.java                     # Role-based access control
└── performance/
    └── LeadCapturePerformanceTest.java   # Throughput benchmarks
```

### Unit Tests

#### LeadService Tests

Tests lead lifecycle management including capture, qualification, scoring, and conversion.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Lead Service Tests")
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private EmailSequenceRepository sequenceRepository;

    @Mock
    private ZohoClient zohoClient;

    @InjectMocks
    private LeadService leadService;

    private static final String TENANT_ID = "tenant-sales-001";
    private static final UUID LEAD_ID = UUID.randomUUID();

    @Nested
    @DisplayName("Lead Capture Tests")
    class LeadCaptureTests {

        @Test
        @DisplayName("Should capture new lead successfully")
        void shouldCaptureNewLeadSuccessfully() {
            // Given
            LeadCaptureRequest request = LeadCaptureRequest.builder()
                .email("prospect@healthsystem.org")
                .name("Jane Smith")
                .organization("Regional Health System")
                .source(LeadSource.LANDING_PAGE)
                .utmSource("google")
                .utmCampaign("hedis-2025")
                .build();

            when(leadRepository.existsByEmailAndTenantId(request.getEmail(), TENANT_ID))
                .thenReturn(false);
            when(leadRepository.save(any(Lead.class)))
                .thenAnswer(invocation -> {
                    Lead lead = invocation.getArgument(0);
                    lead.setId(LEAD_ID);
                    return lead;
                });
            when(sequenceRepository.findActiveSequencesByTargetType(TENANT_ID, TargetType.LEAD))
                .thenReturn(Collections.emptyList());

            // When
            LeadResponse result = leadService.captureLead(request, TENANT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(LEAD_ID);
            assertThat(result.getEmail()).isEqualTo("prospect@healthsystem.org");
            assertThat(result.getStatus()).isEqualTo(LeadStatus.NEW);
            assertThat(result.getSource()).isEqualTo(LeadSource.LANDING_PAGE);

            verify(leadRepository).existsByEmailAndTenantId(request.getEmail(), TENANT_ID);
            verify(leadRepository).save(argThat(lead ->
                lead.getTenantId().equals(TENANT_ID) &&
                lead.getStatus() == LeadStatus.NEW &&
                lead.getScore() == 0 // Initial score
            ));
        }

        @Test
        @DisplayName("Should detect duplicate lead by email")
        void shouldDetectDuplicateLeadByEmail() {
            // Given
            LeadCaptureRequest request = LeadCaptureRequest.builder()
                .email("existing@healthsystem.org")
                .name("Existing Lead")
                .build();

            when(leadRepository.existsByEmailAndTenantId(request.getEmail(), TENANT_ID))
                .thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> leadService.captureLead(request, TENANT_ID))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Lead with email already exists");

            verify(leadRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should auto-enroll in active welcome sequences")
        void shouldAutoEnrollInActiveSequences() {
            // Given
            LeadCaptureRequest request = LeadCaptureRequest.builder()
                .email("newprospect@health.org")
                .name("New Prospect")
                .source(LeadSource.WEBSITE)
                .build();

            EmailSequence welcomeSequence = createSequence(SequenceType.WELCOME, TargetType.LEAD, true);

            when(leadRepository.existsByEmailAndTenantId(any(), any())).thenReturn(false);
            when(leadRepository.save(any())).thenAnswer(inv -> {
                Lead lead = inv.getArgument(0);
                lead.setId(LEAD_ID);
                return lead;
            });
            when(sequenceRepository.findActiveSequencesByTargetType(TENANT_ID, TargetType.LEAD))
                .thenReturn(List.of(welcomeSequence));

            // When
            LeadResponse result = leadService.captureLead(request, TENANT_ID);

            // Then
            assertThat(result).isNotNull();
            // Verify enrollment was created (implementation detail)
        }
    }

    @Nested
    @DisplayName("Lead Qualification Tests")
    class LeadQualificationTests {

        @Test
        @DisplayName("Should qualify lead and update score")
        void shouldQualifyLeadAndUpdateScore() {
            // Given
            Lead lead = createLead(LeadStatus.NEW, 25);
            QualifyLeadRequest request = QualifyLeadRequest.builder()
                .qualified(true)
                .score(85)
                .notes("High potential - large health system")
                .build();

            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID))
                .thenReturn(Optional.of(lead));
            when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            LeadResponse result = leadService.qualifyLead(LEAD_ID, request, TENANT_ID);

            // Then
            assertThat(result.getStatus()).isEqualTo(LeadStatus.QUALIFIED);
            assertThat(result.getScore()).isEqualTo(85);

            verify(leadRepository).save(argThat(l ->
                l.getStatus() == LeadStatus.QUALIFIED &&
                l.getScore() == 85
            ));
        }

        @Test
        @DisplayName("Should disqualify lead with reason")
        void shouldDisqualifyLeadWithReason() {
            // Given
            Lead lead = createLead(LeadStatus.NEW, 50);
            QualifyLeadRequest request = QualifyLeadRequest.builder()
                .qualified(false)
                .disqualifyReason("Not a healthcare organization")
                .build();

            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID))
                .thenReturn(Optional.of(lead));
            when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            LeadResponse result = leadService.qualifyLead(LEAD_ID, request, TENANT_ID);

            // Then
            assertThat(result.getStatus()).isEqualTo(LeadStatus.DISQUALIFIED);
        }
    }

    @Nested
    @DisplayName("Lead Conversion Tests")
    class LeadConversionTests {

        @Test
        @DisplayName("Should convert lead to account, contact, and opportunity")
        void shouldConvertLeadToAccountContactOpportunity() {
            // Given
            Lead lead = createLead(LeadStatus.QUALIFIED, 90);
            lead.setEmail("convert@healthsystem.org");
            lead.setName("Convert Lead");
            lead.setOrganization("Health System Inc");

            ConvertLeadRequest request = ConvertLeadRequest.builder()
                .opportunityName("HDIM Implementation")
                .opportunityAmount(new BigDecimal("150000"))
                .expectedCloseDate(LocalDate.now().plusMonths(3))
                .build();

            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID))
                .thenReturn(Optional.of(lead));
            when(accountRepository.save(any())).thenAnswer(inv -> {
                Account account = inv.getArgument(0);
                account.setId(UUID.randomUUID());
                return account;
            });
            when(contactRepository.save(any())).thenAnswer(inv -> {
                Contact contact = inv.getArgument(0);
                contact.setId(UUID.randomUUID());
                return contact;
            });
            when(opportunityRepository.save(any())).thenAnswer(inv -> {
                Opportunity opp = inv.getArgument(0);
                opp.setId(UUID.randomUUID());
                return opp;
            });
            when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            ConversionResult result = leadService.convertLead(LEAD_ID, request, TENANT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isNotNull();
            assertThat(result.getContactId()).isNotNull();
            assertThat(result.getOpportunityId()).isNotNull();

            // Verify account created from lead organization
            verify(accountRepository).save(argThat(account ->
                account.getName().equals("Health System Inc") &&
                account.getTenantId().equals(TENANT_ID)
            ));

            // Verify contact created from lead info
            verify(contactRepository).save(argThat(contact ->
                contact.getEmail().equals("convert@healthsystem.org") &&
                contact.getName().equals("Convert Lead")
            ));

            // Verify opportunity created with request details
            verify(opportunityRepository).save(argThat(opp ->
                opp.getName().equals("HDIM Implementation") &&
                opp.getAmount().compareTo(new BigDecimal("150000")) == 0 &&
                opp.getStage() == OpportunityStage.DISCOVERY
            ));

            // Verify lead marked as converted
            verify(leadRepository).save(argThat(l ->
                l.getStatus() == LeadStatus.CONVERTED
            ));
        }

        @Test
        @DisplayName("Should reject conversion of unqualified lead")
        void shouldRejectConversionOfUnqualifiedLead() {
            // Given
            Lead lead = createLead(LeadStatus.NEW, 30);

            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID))
                .thenReturn(Optional.of(lead));

            // When/Then
            assertThatThrownBy(() -> leadService.convertLead(LEAD_ID,
                    ConvertLeadRequest.builder().build(), TENANT_ID))
                .isInstanceOf(InvalidStageTransitionException.class)
                .hasMessageContaining("must be QUALIFIED");
        }
    }

    @Nested
    @DisplayName("Lead Scoring and Filtering Tests")
    class LeadScoringTests {

        @Test
        @DisplayName("Should find leads by minimum score threshold")
        void shouldFindLeadsByMinimumScore() {
            // Given
            int minScore = 70;
            List<Lead> highScoreLeads = List.of(
                createLead(LeadStatus.QUALIFIED, 85),
                createLead(LeadStatus.QUALIFIED, 90),
                createLead(LeadStatus.NEW, 75)
            );

            when(leadRepository.findByTenantIdAndScoreGreaterThanEqual(TENANT_ID, minScore))
                .thenReturn(highScoreLeads);

            // When
            List<LeadResponse> results = leadService.findHighScoreLeads(TENANT_ID, minScore);

            // Then
            assertThat(results).hasSize(3);
            assertThat(results).allMatch(l -> l.getScore() >= minScore);
        }

        @Test
        @DisplayName("Should find leads by status")
        void shouldFindLeadsByStatus() {
            // Given
            List<Lead> qualifiedLeads = List.of(
                createLead(LeadStatus.QUALIFIED, 80),
                createLead(LeadStatus.QUALIFIED, 85)
            );

            when(leadRepository.findByTenantIdAndStatus(TENANT_ID, LeadStatus.QUALIFIED))
                .thenReturn(qualifiedLeads);

            // When
            List<LeadResponse> results = leadService.findByStatus(TENANT_ID, LeadStatus.QUALIFIED);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).allMatch(l -> l.getStatus() == LeadStatus.QUALIFIED);
        }
    }

    // Helper methods
    private Lead createLead(LeadStatus status, int score) {
        return Lead.builder()
            .id(LEAD_ID)
            .tenantId(TENANT_ID)
            .email("test-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
            .name("Test Lead")
            .organization("Test Organization")
            .source(LeadSource.WEBSITE)
            .status(status)
            .score(score)
            .createdAt(LocalDateTime.now())
            .build();
    }

    private EmailSequence createSequence(SequenceType type, TargetType targetType, boolean active) {
        return EmailSequence.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .name("Test " + type.name() + " Sequence")
            .sequenceType(type)
            .targetType(targetType)
            .active(active)
            .build();
    }
}
```

#### OpportunityService Tests

Tests opportunity pipeline management, stage transitions, and metrics calculation.

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Opportunity Service Tests")
class OpportunityServiceTest {

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OpportunityService opportunityService;

    private static final String TENANT_ID = "tenant-sales-001";
    private static final UUID OPPORTUNITY_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @Nested
    @DisplayName("Opportunity Creation Tests")
    class OpportunityCreationTests {

        @Test
        @DisplayName("Should create opportunity with default DISCOVERY stage")
        void shouldCreateOpportunityWithDefaultStage() {
            // Given
            CreateOpportunityRequest request = CreateOpportunityRequest.builder()
                .accountId(ACCOUNT_ID)
                .name("HDIM Enterprise License")
                .amount(new BigDecimal("250000"))
                .expectedCloseDate(LocalDate.now().plusMonths(6))
                .productTier("ENTERPRISE")
                .contractLengthMonths(36)
                .build();

            when(accountRepository.existsByIdAndTenantId(ACCOUNT_ID, TENANT_ID))
                .thenReturn(true);
            when(opportunityRepository.save(any())).thenAnswer(inv -> {
                Opportunity opp = inv.getArgument(0);
                opp.setId(OPPORTUNITY_ID);
                return opp;
            });

            // When
            OpportunityResponse result = opportunityService.createOpportunity(request, TENANT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStage()).isEqualTo(OpportunityStage.DISCOVERY);
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("250000"));
            assertThat(result.getProductTier()).isEqualTo("ENTERPRISE");

            verify(opportunityRepository).save(argThat(opp ->
                opp.getStage() == OpportunityStage.DISCOVERY &&
                opp.getTenantId().equals(TENANT_ID)
            ));
        }
    }

    @Nested
    @DisplayName("Stage Transition Tests")
    class StageTransitionTests {

        @Test
        @DisplayName("Should transition from DISCOVERY to PROPOSAL")
        void shouldTransitionFromDiscoveryToProposal() {
            // Given
            Opportunity opportunity = createOpportunity(OpportunityStage.DISCOVERY);

            when(opportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity));
            when(opportunityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            OpportunityResponse result = opportunityService.updateStage(
                OPPORTUNITY_ID, OpportunityStage.PROPOSAL, TENANT_ID);

            // Then
            assertThat(result.getStage()).isEqualTo(OpportunityStage.PROPOSAL);
            assertThat(result.getActualCloseDate()).isNull(); // Not closed yet

            verify(kafkaTemplate).send(eq("sales.opportunity.stage-changed"), any());
        }

        @Test
        @DisplayName("Should set actualCloseDate when moving to CLOSED_WON")
        void shouldSetActualCloseDateWhenClosedWon() {
            // Given
            Opportunity opportunity = createOpportunity(OpportunityStage.NEGOTIATION);

            when(opportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity));
            when(opportunityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            OpportunityResponse result = opportunityService.updateStage(
                OPPORTUNITY_ID, OpportunityStage.CLOSED_WON, TENANT_ID);

            // Then
            assertThat(result.getStage()).isEqualTo(OpportunityStage.CLOSED_WON);
            assertThat(result.getActualCloseDate()).isEqualTo(LocalDate.now());

            verify(kafkaTemplate).send(eq("sales.deal.closed"), argThat(event ->
                event instanceof DealClosedEvent &&
                ((DealClosedEvent) event).isWon()
            ));
        }

        @Test
        @DisplayName("Should set actualCloseDate when moving to CLOSED_LOST")
        void shouldSetActualCloseDateWhenClosedLost() {
            // Given
            Opportunity opportunity = createOpportunity(OpportunityStage.PROPOSAL);

            when(opportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity));
            when(opportunityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            OpportunityResponse result = opportunityService.updateStage(
                OPPORTUNITY_ID, OpportunityStage.CLOSED_LOST, TENANT_ID);

            // Then
            assertThat(result.getStage()).isEqualTo(OpportunityStage.CLOSED_LOST);
            assertThat(result.getActualCloseDate()).isEqualTo(LocalDate.now());

            verify(kafkaTemplate).send(eq("sales.deal.closed"), argThat(event ->
                event instanceof DealClosedEvent &&
                !((DealClosedEvent) event).isWon()
            ));
        }
    }

    @Nested
    @DisplayName("Pipeline Metrics Tests")
    class PipelineMetricsTests {

        @Test
        @DisplayName("Should calculate total pipeline value")
        void shouldCalculateTotalPipelineValue() {
            // Given
            BigDecimal expectedTotal = new BigDecimal("500000");

            when(opportunityRepository.sumOpenPipelineValue(TENANT_ID))
                .thenReturn(expectedTotal);
            when(opportunityRepository.sumWeightedPipelineValue(TENANT_ID))
                .thenReturn(new BigDecimal("250000"));
            when(opportunityRepository.countByTenantIdAndStage(TENANT_ID, OpportunityStage.CLOSED_WON))
                .thenReturn(10L);
            when(opportunityRepository.countByTenantIdAndStage(TENANT_ID, OpportunityStage.CLOSED_LOST))
                .thenReturn(5L);

            // When
            PipelineMetrics metrics = opportunityService.getPipelineMetrics(TENANT_ID);

            // Then
            assertThat(metrics.getTotalPipeline()).isEqualByComparingTo(expectedTotal);
        }

        @Test
        @DisplayName("Should calculate weighted pipeline value")
        void shouldCalculateWeightedPipelineValue() {
            // Given
            // Stage probabilities: DISCOVERY=10%, PROPOSAL=60%, NEGOTIATION=80%
            BigDecimal weightedValue = new BigDecimal("175000");

            when(opportunityRepository.sumOpenPipelineValue(TENANT_ID))
                .thenReturn(new BigDecimal("300000"));
            when(opportunityRepository.sumWeightedPipelineValue(TENANT_ID))
                .thenReturn(weightedValue);
            when(opportunityRepository.countByTenantIdAndStage(TENANT_ID, OpportunityStage.CLOSED_WON))
                .thenReturn(5L);
            when(opportunityRepository.countByTenantIdAndStage(TENANT_ID, OpportunityStage.CLOSED_LOST))
                .thenReturn(3L);

            // When
            PipelineMetrics metrics = opportunityService.getPipelineMetrics(TENANT_ID);

            // Then
            assertThat(metrics.getWeightedPipeline()).isEqualByComparingTo(weightedValue);
        }

        @Test
        @DisplayName("Should calculate win rate correctly")
        void shouldCalculateWinRateCorrectly() {
            // Given
            // 3 won, 2 lost = 3 / (3 + 2) * 100 = 60%
            when(opportunityRepository.sumOpenPipelineValue(TENANT_ID))
                .thenReturn(BigDecimal.ZERO);
            when(opportunityRepository.sumWeightedPipelineValue(TENANT_ID))
                .thenReturn(BigDecimal.ZERO);
            when(opportunityRepository.countByTenantIdAndStage(TENANT_ID, OpportunityStage.CLOSED_WON))
                .thenReturn(3L);
            when(opportunityRepository.countByTenantIdAndStage(TENANT_ID, OpportunityStage.CLOSED_LOST))
                .thenReturn(2L);

            // When
            PipelineMetrics metrics = opportunityService.getPipelineMetrics(TENANT_ID);

            // Then
            assertThat(metrics.getWinRate()).isEqualByComparingTo(new BigDecimal("60.00"));
            assertThat(metrics.getWonOpportunities()).isEqualTo(3L);
            assertThat(metrics.getLostOpportunities()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Should handle zero closed opportunities for win rate")
        void shouldHandleZeroClosedForWinRate() {
            // Given - no closed opportunities yet
            when(opportunityRepository.sumOpenPipelineValue(TENANT_ID))
                .thenReturn(new BigDecimal("100000"));
            when(opportunityRepository.sumWeightedPipelineValue(TENANT_ID))
                .thenReturn(new BigDecimal("50000"));
            when(opportunityRepository.countByTenantIdAndStage(TENANT_ID, OpportunityStage.CLOSED_WON))
                .thenReturn(0L);
            when(opportunityRepository.countByTenantIdAndStage(TENANT_ID, OpportunityStage.CLOSED_LOST))
                .thenReturn(0L);

            // When
            PipelineMetrics metrics = opportunityService.getPipelineMetrics(TENANT_ID);

            // Then
            assertThat(metrics.getWinRate()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should default null aggregates to zero")
        void shouldDefaultNullAggregatesToZero() {
            // Given - repository returns null for empty data
            when(opportunityRepository.sumOpenPipelineValue(TENANT_ID)).thenReturn(null);
            when(opportunityRepository.sumWeightedPipelineValue(TENANT_ID)).thenReturn(null);
            when(opportunityRepository.countByTenantIdAndStage(any(), any())).thenReturn(0L);

            // When
            PipelineMetrics metrics = opportunityService.getPipelineMetrics(TENANT_ID);

            // Then
            assertThat(metrics.getTotalPipeline()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(metrics.getWeightedPipeline()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // Helper methods
    private Opportunity createOpportunity(OpportunityStage stage) {
        return Opportunity.builder()
            .id(OPPORTUNITY_ID)
            .tenantId(TENANT_ID)
            .accountId(ACCOUNT_ID)
            .name("Test Opportunity")
            .amount(new BigDecimal("100000"))
            .stage(stage)
            .probability(stage.getProbability())
            .expectedCloseDate(LocalDate.now().plusMonths(3))
            .createdAt(LocalDateTime.now())
            .build();
    }
}
```

#### EmailSequenceService Tests

Tests email automation workflows, enrollment management, and sequence lifecycle.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Email Sequence Service Tests")
class EmailSequenceServiceTest {

    @Mock
    private EmailSequenceRepository sequenceRepository;

    @Mock
    private SequenceEnrollmentRepository enrollmentRepository;

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private EmailSequenceService emailSequenceService;

    private static final String TENANT_ID = "tenant-sales-001";
    private static final UUID SEQUENCE_ID = UUID.randomUUID();
    private static final UUID LEAD_ID = UUID.randomUUID();
    private static final UUID CONTACT_ID = UUID.randomUUID();
    private static final String USER_ID = "user-001";

    @Nested
    @DisplayName("Sequence Creation Tests")
    class SequenceCreationTests {

        @Test
        @DisplayName("Should create new email sequence")
        void shouldCreateNewSequence() {
            // Given
            CreateSequenceRequest request = CreateSequenceRequest.builder()
                .name("Welcome Series")
                .description("Onboarding sequence for new leads")
                .sequenceType(SequenceType.WELCOME)
                .targetType(TargetType.LEAD)
                .fromName("HDIM Sales Team")
                .fromEmail("sales@healthdatainmotion.com")
                .steps(List.of(
                    createStep(1, "Welcome", "welcome-template", 0),
                    createStep(2, "Features Overview", "features-template", 2),
                    createStep(3, "Case Study", "case-study-template", 4)
                ))
                .build();

            when(sequenceRepository.existsByNameAndTenantId("Welcome Series", TENANT_ID))
                .thenReturn(false);
            when(sequenceRepository.save(any())).thenAnswer(inv -> {
                EmailSequence seq = inv.getArgument(0);
                seq.setId(SEQUENCE_ID);
                return seq;
            });

            // When
            EmailSequenceResponse result = emailSequenceService.createSequence(request, TENANT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(SEQUENCE_ID);
            assertThat(result.getName()).isEqualTo("Welcome Series");
            assertThat(result.isActive()).isFalse(); // New sequences start inactive
            assertThat(result.getSteps()).hasSize(3);

            verify(sequenceRepository).save(argThat(seq ->
                seq.getTenantId().equals(TENANT_ID) &&
                !seq.isActive() &&
                seq.getSequenceType() == SequenceType.WELCOME
            ));
        }

        @Test
        @DisplayName("Should reject duplicate sequence name")
        void shouldRejectDuplicateSequenceName() {
            // Given
            CreateSequenceRequest request = CreateSequenceRequest.builder()
                .name("Existing Sequence")
                .build();

            when(sequenceRepository.existsByNameAndTenantId("Existing Sequence", TENANT_ID))
                .thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> emailSequenceService.createSequence(request, TENANT_ID))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Sequence with name already exists");
        }
    }

    @Nested
    @DisplayName("Sequence Enrollment Tests")
    class SequenceEnrollmentTests {

        @Test
        @DisplayName("Should enroll lead in sequence successfully")
        void shouldEnrollLeadInSequence() {
            // Given
            EmailSequence sequence = createSequence(SequenceType.NURTURE, TargetType.LEAD, true);
            Lead lead = Lead.builder()
                .id(LEAD_ID)
                .tenantId(TENANT_ID)
                .email("lead@test.com")
                .status(LeadStatus.QUALIFIED)
                .build();

            when(sequenceRepository.findByIdAndTenantId(SEQUENCE_ID, TENANT_ID))
                .thenReturn(Optional.of(sequence));
            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID))
                .thenReturn(Optional.of(lead));
            when(enrollmentRepository.existsBySequenceIdAndLeadId(SEQUENCE_ID, LEAD_ID))
                .thenReturn(false);
            when(enrollmentRepository.save(any())).thenAnswer(inv -> {
                SequenceEnrollment enrollment = inv.getArgument(0);
                enrollment.setId(UUID.randomUUID());
                return enrollment;
            });

            // When
            EnrollmentResponse result = emailSequenceService.enrollLead(
                SEQUENCE_ID, LEAD_ID, TENANT_ID, USER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
            assertThat(result.getCurrentStep()).isEqualTo(1);

            verify(enrollmentRepository).save(argThat(enrollment ->
                enrollment.getLeadId().equals(LEAD_ID) &&
                enrollment.getSequenceId().equals(SEQUENCE_ID) &&
                enrollment.getEnrolledByUserId().equals(USER_ID) &&
                enrollment.getStatus() == EnrollmentStatus.ACTIVE
            ));
        }

        @Test
        @DisplayName("Should reject enrollment when sequence is inactive")
        void shouldRejectEnrollmentWhenSequenceInactive() {
            // Given
            EmailSequence inactiveSequence = createSequence(SequenceType.NURTURE, TargetType.LEAD, false);

            when(sequenceRepository.findByIdAndTenantId(SEQUENCE_ID, TENANT_ID))
                .thenReturn(Optional.of(inactiveSequence));

            // When/Then
            assertThatThrownBy(() -> emailSequenceService.enrollLead(
                    SEQUENCE_ID, LEAD_ID, TENANT_ID, USER_ID))
                .isInstanceOf(InvalidStageTransitionException.class)
                .hasMessageContaining("Sequence is not active");
        }

        @Test
        @DisplayName("Should reject enrollment in contact-only sequence")
        void shouldRejectEnrollmentInContactOnlySequence() {
            // Given
            EmailSequence contactOnlySequence = createSequence(SequenceType.NURTURE, TargetType.CONTACT, true);

            when(sequenceRepository.findByIdAndTenantId(SEQUENCE_ID, TENANT_ID))
                .thenReturn(Optional.of(contactOnlySequence));

            // When/Then
            assertThatThrownBy(() -> emailSequenceService.enrollLead(
                    SEQUENCE_ID, LEAD_ID, TENANT_ID, USER_ID))
                .isInstanceOf(InvalidStageTransitionException.class)
                .hasMessageContaining("contacts only");
        }

        @Test
        @DisplayName("Should reject duplicate enrollment")
        void shouldRejectDuplicateEnrollment() {
            // Given
            EmailSequence sequence = createSequence(SequenceType.NURTURE, TargetType.LEAD, true);
            Lead lead = Lead.builder()
                .id(LEAD_ID)
                .tenantId(TENANT_ID)
                .email("lead@test.com")
                .build();

            when(sequenceRepository.findByIdAndTenantId(SEQUENCE_ID, TENANT_ID))
                .thenReturn(Optional.of(sequence));
            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID))
                .thenReturn(Optional.of(lead));
            when(enrollmentRepository.existsBySequenceIdAndLeadId(SEQUENCE_ID, LEAD_ID))
                .thenReturn(true); // Already enrolled

            // When/Then
            assertThatThrownBy(() -> emailSequenceService.enrollLead(
                    SEQUENCE_ID, LEAD_ID, TENANT_ID, USER_ID))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already enrolled");
        }
    }

    @Nested
    @DisplayName("Sequence Activation Tests")
    class SequenceActivationTests {

        @Test
        @DisplayName("Should activate sequence successfully")
        void shouldActivateSequenceSuccessfully() {
            // Given
            EmailSequence sequence = createSequence(SequenceType.WELCOME, TargetType.LEAD, false);

            when(sequenceRepository.findByIdAndTenantId(SEQUENCE_ID, TENANT_ID))
                .thenReturn(Optional.of(sequence));
            when(sequenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            EmailSequenceResponse result = emailSequenceService.activateSequence(SEQUENCE_ID, TENANT_ID);

            // Then
            assertThat(result.isActive()).isTrue();

            verify(sequenceRepository).save(argThat(seq -> seq.isActive()));
        }

        @Test
        @DisplayName("Should deactivate sequence successfully")
        void shouldDeactivateSequenceSuccessfully() {
            // Given
            EmailSequence sequence = createSequence(SequenceType.WELCOME, TargetType.LEAD, true);

            when(sequenceRepository.findByIdAndTenantId(SEQUENCE_ID, TENANT_ID))
                .thenReturn(Optional.of(sequence));
            when(sequenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            EmailSequenceResponse result = emailSequenceService.deactivateSequence(SEQUENCE_ID, TENANT_ID);

            // Then
            assertThat(result.isActive()).isFalse();

            verify(sequenceRepository).save(argThat(seq -> !seq.isActive()));
        }
    }

    @Nested
    @DisplayName("Sequence Deletion Tests")
    class SequenceDeletionTests {

        @Test
        @DisplayName("Should delete sequence with no active enrollments")
        void shouldDeleteSequenceWithNoActiveEnrollments() {
            // Given
            EmailSequence sequence = createSequence(SequenceType.WELCOME, TargetType.LEAD, false);

            when(sequenceRepository.findByIdAndTenantId(SEQUENCE_ID, TENANT_ID))
                .thenReturn(Optional.of(sequence));
            when(enrollmentRepository.countActiveBySequenceId(SEQUENCE_ID))
                .thenReturn(0L);

            // When
            emailSequenceService.deleteSequence(SEQUENCE_ID, TENANT_ID);

            // Then
            verify(sequenceRepository).delete(sequence);
        }

        @Test
        @DisplayName("Should reject deletion with active enrollments")
        void shouldRejectDeletionWithActiveEnrollments() {
            // Given
            EmailSequence sequence = createSequence(SequenceType.NURTURE, TargetType.LEAD, true);

            when(sequenceRepository.findByIdAndTenantId(SEQUENCE_ID, TENANT_ID))
                .thenReturn(Optional.of(sequence));
            when(enrollmentRepository.countActiveBySequenceId(SEQUENCE_ID))
                .thenReturn(5L);

            // When/Then
            assertThatThrownBy(() -> emailSequenceService.deleteSequence(SEQUENCE_ID, TENANT_ID))
                .isInstanceOf(InvalidStageTransitionException.class)
                .hasMessageContaining("5 active enrollments");
        }
    }

    @Nested
    @DisplayName("Enrollment Count Tests")
    class EnrollmentCountTests {

        @Test
        @DisplayName("Should track total and active enrollment counts")
        void shouldTrackEnrollmentCounts() {
            // Given
            EmailSequence sequence = createSequence(SequenceType.NURTURE, TargetType.LEAD, true);
            sequence.setTotalEnrollments(100);
            sequence.setActiveEnrollments(35);

            when(sequenceRepository.findByIdAndTenantId(SEQUENCE_ID, TENANT_ID))
                .thenReturn(Optional.of(sequence));

            // When
            EmailSequenceResponse result = emailSequenceService.getSequence(SEQUENCE_ID, TENANT_ID);

            // Then
            assertThat(result.getTotalEnrollments()).isEqualTo(100);
            assertThat(result.getActiveEnrollments()).isEqualTo(35);
        }
    }

    // Helper methods
    private EmailSequence createSequence(SequenceType type, TargetType targetType, boolean active) {
        return EmailSequence.builder()
            .id(SEQUENCE_ID)
            .tenantId(TENANT_ID)
            .name("Test " + type.name() + " Sequence")
            .description("Test description")
            .sequenceType(type)
            .targetType(targetType)
            .active(active)
            .fromName("Test Sender")
            .fromEmail("test@example.com")
            .totalEnrollments(0)
            .activeEnrollments(0)
            .createdAt(LocalDateTime.now())
            .build();
    }

    private SequenceStep createStep(int order, String name, String templateId, int delayDays) {
        return SequenceStep.builder()
            .stepOrder(order)
            .name(name)
            .templateId(templateId)
            .delayDays(delayDays)
            .build();
    }
}
```

### Controller Tests

#### LeadController Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
class LeadControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LeadRepository leadRepository;

    private static final String TENANT_ID = "tenant-test-001";
    private static final String BASE_URL = "/api/v1/leads";

    @BeforeEach
    void setUp() {
        leadRepository.deleteAll();
    }

    @Test
    @DisplayName("Should capture lead from public endpoint")
    void shouldCaptureLeadFromPublicEndpoint() throws Exception {
        // Given
        LeadCaptureRequest request = LeadCaptureRequest.builder()
            .email("newlead@healthsystem.org")
            .name("New Lead")
            .organization("Health System")
            .source(LeadSource.LANDING_PAGE)
            .utmSource("google")
            .utmCampaign("hedis-2025")
            .build();

        // When/Then
        mockMvc.perform(post(BASE_URL + "/capture")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("newlead@healthsystem.org"))
            .andExpect(jsonPath("$.status").value("NEW"))
            .andExpect(jsonPath("$.source").value("LANDING_PAGE"));
    }

    @Test
    @DisplayName("Should list leads with pagination")
    void shouldListLeadsWithPagination() throws Exception {
        // Given
        for (int i = 0; i < 15; i++) {
            leadRepository.save(createLead("lead" + i + "@test.com"));
        }

        // When/Then
        mockMvc.perform(get(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "SALES")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(10))
            .andExpect(jsonPath("$.totalElements").value(15))
            .andExpect(jsonPath("$.totalPages").value(2));
    }

    private Lead createLead(String email) {
        return Lead.builder()
            .tenantId(TENANT_ID)
            .email(email)
            .name("Test Lead")
            .organization("Test Org")
            .source(LeadSource.WEBSITE)
            .status(LeadStatus.NEW)
            .score(50)
            .createdAt(LocalDateTime.now())
            .build();
    }
}
```

### Multi-Tenant Isolation Tests

```java
@SpringBootTest
@Testcontainers
class SalesMultiTenantIsolationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private EmailSequenceRepository sequenceRepository;

    @Test
    @DisplayName("Leads should be isolated by tenant")
    void leadsShouldBeIsolatedByTenant() {
        // Given
        String tenant1 = "tenant-001";
        String tenant2 = "tenant-002";

        Lead tenant1Lead = leadRepository.save(createLead(tenant1, "lead1@tenant1.com"));
        Lead tenant2Lead = leadRepository.save(createLead(tenant2, "lead2@tenant2.com"));

        // When
        List<Lead> tenant1Leads = leadRepository.findByTenantId(tenant1);
        List<Lead> tenant2Leads = leadRepository.findByTenantId(tenant2);

        // Then
        assertThat(tenant1Leads)
            .hasSize(1)
            .extracting(Lead::getTenantId)
            .containsOnly(tenant1);

        assertThat(tenant2Leads)
            .hasSize(1)
            .extracting(Lead::getTenantId)
            .containsOnly(tenant2);

        // Verify no cross-tenant access
        assertThat(tenant1Leads).doesNotContain(tenant2Lead);
        assertThat(tenant2Leads).doesNotContain(tenant1Lead);
    }

    @Test
    @DisplayName("Pipeline metrics should be tenant-scoped")
    void pipelineMetricsShouldBeTenantScoped() {
        // Given
        String tenant1 = "tenant-001";
        String tenant2 = "tenant-002";

        // Create opportunities in both tenants
        opportunityRepository.save(createOpportunity(tenant1, new BigDecimal("100000")));
        opportunityRepository.save(createOpportunity(tenant1, new BigDecimal("200000")));
        opportunityRepository.save(createOpportunity(tenant2, new BigDecimal("500000")));

        // When
        BigDecimal tenant1Total = opportunityRepository.sumOpenPipelineValue(tenant1);
        BigDecimal tenant2Total = opportunityRepository.sumOpenPipelineValue(tenant2);

        // Then
        assertThat(tenant1Total).isEqualByComparingTo(new BigDecimal("300000"));
        assertThat(tenant2Total).isEqualByComparingTo(new BigDecimal("500000"));
    }

    @Test
    @DisplayName("Email sequences should be tenant-isolated")
    void emailSequencesShouldBeTenantIsolated() {
        // Given
        String tenant1 = "tenant-001";
        String tenant2 = "tenant-002";

        sequenceRepository.save(createSequence(tenant1, "Tenant 1 Welcome"));
        sequenceRepository.save(createSequence(tenant2, "Tenant 2 Welcome"));

        // When
        List<EmailSequence> tenant1Sequences = sequenceRepository.findByTenantId(tenant1);
        List<EmailSequence> tenant2Sequences = sequenceRepository.findByTenantId(tenant2);

        // Then
        assertThat(tenant1Sequences)
            .hasSize(1)
            .extracting(EmailSequence::getName)
            .containsOnly("Tenant 1 Welcome");

        assertThat(tenant2Sequences)
            .hasSize(1)
            .extracting(EmailSequence::getName)
            .containsOnly("Tenant 2 Welcome");
    }

    private Lead createLead(String tenantId, String email) {
        return Lead.builder()
            .tenantId(tenantId)
            .email(email)
            .name("Test Lead")
            .source(LeadSource.WEBSITE)
            .status(LeadStatus.NEW)
            .score(50)
            .build();
    }

    private Opportunity createOpportunity(String tenantId, BigDecimal amount) {
        return Opportunity.builder()
            .tenantId(tenantId)
            .name("Test Opportunity")
            .amount(amount)
            .stage(OpportunityStage.DISCOVERY)
            .expectedCloseDate(LocalDate.now().plusMonths(3))
            .build();
    }

    private EmailSequence createSequence(String tenantId, String name) {
        return EmailSequence.builder()
            .tenantId(tenantId)
            .name(name)
            .sequenceType(SequenceType.WELCOME)
            .targetType(TargetType.LEAD)
            .active(false)
            .build();
    }
}
```

### RBAC Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class SalesRbacTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String TENANT_ID = "tenant-rbac-001";

    @Test
    @DisplayName("SALES role should access leads")
    void salesRoleShouldAccessLeads() throws Exception {
        mockMvc.perform(get("/api/v1/leads")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "sales-user-001")
                .header("X-Auth-Roles", "SALES"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN role should access pipeline forecast")
    void adminRoleShouldAccessForecast() throws Exception {
        mockMvc.perform(get("/api/v1/pipeline/forecast")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "admin-001")
                .header("X-Auth-Roles", "ADMIN"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("SALES role should NOT access pipeline forecast")
    void salesRoleShouldNotAccessForecast() throws Exception {
        mockMvc.perform(get("/api/v1/pipeline/forecast")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "sales-user-001")
                .header("X-Auth-Roles", "SALES"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("VIEWER role should NOT access CRM operations")
    void viewerRoleShouldNotAccessCrmOperations() throws Exception {
        mockMvc.perform(post("/api/v1/leads")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "viewer-001")
                .header("X-Auth-Roles", "VIEWER")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }
}
```

### Rate Limiting Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class LeadCaptureRateLimitingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "tenant-ratelimit-001";

    @Test
    @DisplayName("Should rate limit excessive lead capture requests")
    void shouldRateLimitExcessiveRequests() throws Exception {
        // Given
        LeadCaptureRequest request = LeadCaptureRequest.builder()
            .email("ratelimit@test.com")
            .name("Rate Limit Test")
            .source(LeadSource.WEBSITE)
            .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // When - Send requests exceeding rate limit
        int successCount = 0;
        int rateLimitedCount = 0;

        for (int i = 0; i < 20; i++) {
            MvcResult result = mockMvc.perform(post("/api/v1/leads/capture")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Forwarded-For", "192.168.1.100") // Same IP
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson.replace("ratelimit@test.com", "test" + i + "@test.com")))
                .andReturn();

            if (result.getResponse().getStatus() == 201) {
                successCount++;
            } else if (result.getResponse().getStatus() == 429) {
                rateLimitedCount++;
            }
        }

        // Then - Some requests should be rate limited
        assertThat(rateLimitedCount).isGreaterThan(0);
        assertThat(successCount).isLessThan(20);
    }

    @Test
    @DisplayName("Should include rate limit headers in response")
    void shouldIncludeRateLimitHeaders() throws Exception {
        // Given
        LeadCaptureRequest request = LeadCaptureRequest.builder()
            .email("headers@test.com")
            .name("Header Test")
            .source(LeadSource.WEBSITE)
            .build();

        // When/Then
        mockMvc.perform(post("/api/v1/leads/capture")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(header().exists("X-RateLimit-Limit"))
            .andExpect(header().exists("X-RateLimit-Remaining"));
    }
}
```

### Zoho Integration Tests

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Zoho CRM Integration Tests")
class ZohoClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ZohoTokenManager tokenManager;

    @InjectMocks
    private ZohoClient zohoClient;

    private static final String ACCESS_TOKEN = "test-access-token";

    @BeforeEach
    void setUp() {
        when(tokenManager.getAccessToken()).thenReturn(ACCESS_TOKEN);
    }

    @Test
    @DisplayName("Should sync lead to Zoho CRM")
    void shouldSyncLeadToZoho() {
        // Given
        Lead lead = Lead.builder()
            .id(UUID.randomUUID())
            .email("sync@healthsystem.org")
            .name("Sync Test Lead")
            .organization("Health System")
            .source(LeadSource.WEBSITE)
            .build();

        ZohoResponse response = new ZohoResponse();
        response.setData(List.of(Map.of("id", "zoho-lead-123")));

        when(restTemplate.postForObject(anyString(), any(), eq(ZohoResponse.class)))
            .thenReturn(response);

        // When
        String zohoId = zohoClient.createLead(lead);

        // Then
        assertThat(zohoId).isEqualTo("zoho-lead-123");

        verify(restTemplate).postForObject(
            contains("/Leads"),
            argThat(request -> {
                HttpEntity<?> entity = (HttpEntity<?>) request;
                return entity.getHeaders().getFirst("Authorization").equals("Zoho-oauthtoken " + ACCESS_TOKEN);
            }),
            eq(ZohoResponse.class)
        );
    }

    @Test
    @DisplayName("Should handle Zoho webhook with valid signature")
    void shouldHandleWebhookWithValidSignature() {
        // Given
        String webhookSecret = "test-webhook-secret";
        String payload = "{\"module\":\"Leads\",\"operation\":\"insert\",\"ids\":[\"123\"]}";
        String signature = computeHmacSha256(payload, webhookSecret);

        // When
        boolean valid = zohoClient.validateWebhookSignature(payload, signature, webhookSecret);

        // Then
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("Should reject webhook with invalid signature")
    void shouldRejectWebhookWithInvalidSignature() {
        // Given
        String webhookSecret = "test-webhook-secret";
        String payload = "{\"module\":\"Leads\",\"operation\":\"insert\",\"ids\":[\"123\"]}";
        String invalidSignature = "invalid-signature";

        // When
        boolean valid = zohoClient.validateWebhookSignature(payload, invalidSignature, webhookSecret);

        // Then
        assertThat(valid).isFalse();
    }

    private String computeHmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

### Performance Tests

```java
@SpringBootTest
@Testcontainers
class SalesPerformanceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private LeadService leadService;

    @Autowired
    private OpportunityService opportunityService;

    @Autowired
    private LeadRepository leadRepository;

    private static final String TENANT_ID = "tenant-perf-001";

    @Test
    @DisplayName("Lead capture should complete within 100ms")
    void leadCaptureShouldCompleteQuickly() {
        // Given
        int iterations = 100;
        List<Long> latencies = new ArrayList<>();

        // When
        for (int i = 0; i < iterations; i++) {
            LeadCaptureRequest request = LeadCaptureRequest.builder()
                .email("perf-" + i + "@test.com")
                .name("Performance Test " + i)
                .source(LeadSource.WEBSITE)
                .build();

            Instant start = Instant.now();
            leadService.captureLead(request, TENANT_ID);
            Instant end = Instant.now();

            latencies.add(Duration.between(start, end).toMillis());
        }

        // Then
        Collections.sort(latencies);
        long p95 = latencies.get((int) (iterations * 0.95));

        assertThat(p95)
            .isLessThan(100L)
            .withFailMessage("p95 lead capture latency %dms exceeds 100ms threshold", p95);

        System.out.printf("Lead Capture Performance: p50=%dms, p95=%dms, p99=%dms%n",
            latencies.get(iterations / 2),
            p95,
            latencies.get((int) (iterations * 0.99)));
    }

    @Test
    @DisplayName("Pipeline metrics calculation should complete within 200ms")
    void pipelineMetricsShouldCalculateQuickly() {
        // Given - Create 1000 opportunities
        for (int i = 0; i < 1000; i++) {
            leadRepository.save(createOpportunity(i));
        }

        // When
        Instant start = Instant.now();
        PipelineMetrics metrics = opportunityService.getPipelineMetrics(TENANT_ID);
        Instant end = Instant.now();

        long latency = Duration.between(start, end).toMillis();

        // Then
        assertThat(latency)
            .isLessThan(200L)
            .withFailMessage("Pipeline metrics calculation took %dms, exceeds 200ms threshold", latency);

        System.out.printf("Pipeline Metrics: Calculated over 1000 opportunities in %dms%n", latency);
    }

    @Test
    @DisplayName("Should handle concurrent lead captures")
    void shouldHandleConcurrentLeadCaptures() throws Exception {
        // Given
        int threadCount = 10;
        int requestsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount * requestsPerThread);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // When
        Instant start = Instant.now();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                for (int i = 0; i < requestsPerThread; i++) {
                    try {
                        LeadCaptureRequest request = LeadCaptureRequest.builder()
                            .email("concurrent-" + threadId + "-" + i + "@test.com")
                            .name("Concurrent Test")
                            .source(LeadSource.WEBSITE)
                            .build();
                        leadService.captureLead(request, TENANT_ID);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        Instant end = Instant.now();
        long totalMs = Duration.between(start, end).toMillis();

        // Then
        int totalRequests = threadCount * requestsPerThread;
        double throughput = (successCount.get() * 1000.0) / totalMs;

        assertThat(errorCount.get())
            .isLessThan(totalRequests / 10) // Less than 10% error rate
            .withFailMessage("Error rate too high: %d/%d requests failed", errorCount.get(), totalRequests);

        System.out.printf("Concurrent Performance: %d requests, %d successes, %d errors, %.2f req/s%n",
            totalRequests, successCount.get(), errorCount.get(), throughput);
    }

    private Opportunity createOpportunity(int index) {
        return Opportunity.builder()
            .tenantId(TENANT_ID)
            .name("Performance Test " + index)
            .amount(new BigDecimal(10000 + (index % 100) * 1000))
            .stage(OpportunityStage.values()[index % 5])
            .expectedCloseDate(LocalDate.now().plusMonths(index % 12))
            .build();
    }
}
```

### Test Configuration

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:tc:postgresql:15:///testdb
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false

# Rate limiting configuration for tests
rate-limiting:
  lead-capture:
    requests-per-minute: 10
    requests-per-hour: 100

# Zoho CRM test configuration (disabled)
zoho:
  crm:
    enabled: false
    client-id: test-client-id
    client-secret: test-client-secret

# LinkedIn test configuration (disabled)
linkedin:
  enabled: false

# Email tracking test configuration
email:
  tracking:
    base-url: http://localhost:8106/track

# Kafka test configuration
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      auto-offset-reset: earliest
      group-id: test-group
```

### Best Practices

| Practice | Description | Implementation |
|----------|-------------|----------------|
| **Synthetic Data** | Use test-prefixed emails/names | `test-{uuid}@test.com`, `Test Lead {index}` |
| **Tenant Isolation** | Always include tenantId in tests | Every repository call filtered by tenant |
| **BigDecimal Comparison** | Use `isEqualByComparingTo()` | Avoids scale comparison issues |
| **Enum Status Checks** | Verify status transitions | Check before/after status in assertions |
| **Pipeline Metrics** | Validate calculation formulas | Win rate = won/(won+lost)*100 |
| **Null Handling** | Repository nulls default to zero | `Optional.ofNullable(sum).orElse(BigDecimal.ZERO)` |
| **Rate Limit Testing** | Use consistent IP headers | `X-Forwarded-For` for IP-based limiting |
| **Webhook Security** | Validate HMAC signatures | Use `HmacSHA256` with shared secret |
| **Async Events** | Mock KafkaTemplate | Verify event publishing without Kafka |
| **Enrollment Validation** | Check sequence state | Active status, target type matching |

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| `DuplicateResourceException` on capture | Email already exists | Use unique test emails with UUID suffix |
| `InvalidStageTransitionException` | Wrong lead status for conversion | Ensure lead is QUALIFIED before convert |
| Pipeline metrics return null | No opportunities in tenant | Check tenant filter, seed test data |
| Rate limit not triggering | Different IP per request | Set consistent `X-Forwarded-For` header |
| Zoho webhook fails | Invalid signature | Compute HMAC with correct secret |
| Win rate calculation wrong | Integer division | Use `BigDecimal.divide()` with scale |
| Enrollment fails | Sequence inactive | Activate sequence before enrollment |
| Sequence deletion fails | Active enrollments exist | Complete/cancel enrollments first |
| Stage transition blocked | Invalid state machine path | Check allowed transitions in domain |
| Kafka events not sent | KafkaTemplate not mocked | Add `@Mock KafkaTemplate` to test |

## Monitoring

- **Health Check**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`
- **Prometheus**: `GET /actuator/prometheus`

### Key Metrics

| Metric | Description |
|--------|-------------|
| `leads.captured.total` | Total leads captured by source |
| `opportunities.created.total` | Opportunities created |
| `pipeline.value` | Current pipeline value by stage |
| `deals.closed.value` | Closed deal value (won/lost) |
| `email.sequences.sent` | Emails sent from sequences |

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Zoho sync failing | Token expired | Refresh Zoho OAuth token |
| Lead capture rate limited | High traffic | Adjust rate limit config |
| Email tracking not working | Pixel blocked | Check tracking domain DNS |

## Security Considerations

- **Lead capture rate limiting**: Prevents abuse of public endpoint
- **Zoho webhook validation**: HMAC signature verification
- **No PHI storage**: Sales data separate from clinical data
- **Audit logging**: All sales activities logged
- **Role-based access**: SALES and ADMIN roles only

## References

- [Sales Events Schema](../../shared/api-contracts/sales-events.md)
- [Zoho CRM API Docs](https://www.zoho.com/crm/developer/docs/api/v2/)
- [Gateway Trust Architecture](../../../docs/GATEWAY_TRUST_ARCHITECTURE.md)

---

*Last Updated: December 2025*
*Service Version: 1.0*
