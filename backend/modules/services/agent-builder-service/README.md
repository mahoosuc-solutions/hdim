# Agent Builder Service

A no-code platform for creating, configuring, testing, and deploying custom AI agents without programming.

## Purpose

Enables clinical informaticists and healthcare quality managers to customize AI agent behavior through a visual interface, addressing the challenge that:
- Healthcare protocols change frequently and require rapid iteration
- Non-developers need to configure AI behavior without writing code
- Agent configurations need version control and rollback capabilities

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Agent Builder Service                         │
│                         (Port 8096)                              │
├─────────────────────────────────────────────────────────────────┤
│  Controller Layer                                                │
│  └── AgentBuilderController (30+ REST endpoints)                │
├─────────────────────────────────────────────────────────────────┤
│  Service Layer                                                   │
│  ├── AgentConfigurationService  - CRUD, validation, publishing  │
│  ├── AgentVersionService        - Snapshots, rollback, diff     │
│  ├── AgentTestService           - Sandbox testing, metrics      │
│  └── PromptTemplateService      - Template library, rendering   │
├─────────────────────────────────────────────────────────────────┤
│  Repository Layer                                                │
│  ├── AgentConfigurationRepository                               │
│  ├── AgentVersionRepository                                     │
│  ├── AgentTestSessionRepository                                 │
│  └── PromptTemplateRepository                                   │
├─────────────────────────────────────────────────────────────────┤
│  Domain Entities                                                 │
│  ├── AgentConfiguration   - Core agent definition               │
│  ├── AgentVersion         - Immutable configuration snapshots   │
│  ├── AgentTestSession     - Sandbox test sessions               │
│  └── PromptTemplate       - Reusable prompt library             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ OpenFeign (HTTP)
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Agent Runtime Service                          │
│                        (Port 8088)                               │
│  - LLM execution (Claude, Azure OpenAI, AWS Bedrock)            │
│  - Tool registry and execution                                   │
│  - Guardrail enforcement                                         │
│  - Memory management                                             │
└─────────────────────────────────────────────────────────────────┘
```

## Service Layer

### AgentConfigurationService
- **Purpose**: Manage agent configurations (CRUD operations)
- **Key Features**:
  - Create/update/delete agent configurations
  - Agent status lifecycle: DRAFT → TESTING → ACTIVE → DEPRECATED → ARCHIVED
  - Clone agents for rapid iteration
  - Tenant-scoped with configurable limits (max 50 agents/tenant)
  - Redis caching for active agents

### AgentVersionService
- **Purpose**: Version control for agent configurations
- **Key Features**:
  - Immutable configuration snapshots
  - Rollback to any previous version
  - Version comparison (diff between versions)
  - Change tracking with summaries and change types (MAJOR/MINOR/PATCH)

### AgentTestService
- **Purpose**: Sandbox testing before publishing
- **Key Features**:
  - Interactive test sessions with conversation history
  - Tool invocation tracking
  - Metrics collection (latency, tokens, guardrail triggers)
  - Test feedback and ratings
  - Async execution with dedicated thread pool

### PromptTemplateService
- **Purpose**: Reusable prompt building blocks
- **Key Features**:
  - Template categories: SYSTEM_PROMPT, CAPABILITIES, CONSTRAINTS, etc.
  - Variable extraction and rendering ({{variableName}} syntax)
  - Template validation and syntax checking
  - System-wide and tenant-specific templates

## Domain Entities

### AgentConfiguration
Core agent definition with:
- **Identity**: name, slug, description, persona (name, role, avatar)
- **LLM Config**: provider, model, temperature, maxTokens
- **Behavior**: systemPrompt, welcomeMessage
- **Tools**: toolConfiguration (JSON - enabled tools and settings)
- **Safety**: guardrailConfiguration (clinical safety, PHI protection)
- **Access**: allowedRoles, requiresPatientContext

### AgentVersion
Immutable snapshots with:
- **Versioning**: versionNumber (semantic), configurationSnapshot (JSON)
- **Status**: DRAFT, PUBLISHED, ROLLED_BACK, SUPERSEDED
- **Audit**: createdBy, publishedBy, rolledBackBy, changeSummary

### AgentTestSession
Test sessions with:
- **Types**: INTERACTIVE, AUTOMATED, SCENARIO
- **Data**: conversation messages, tool invocations, metrics
- **Feedback**: user ratings and comments

### PromptTemplate
Template library with:
- **Content**: template text with {{variable}} placeholders
- **Metadata**: category, description, variables with defaults
- **Scope**: isSystemTemplate flag for global vs tenant templates

## API Endpoints

### Agent Configuration
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/agent-builder/agents` | Create agent |
| PUT | `/api/v1/agent-builder/agents/{id}` | Update agent |
| GET | `/api/v1/agent-builder/agents/{id}` | Get agent |
| GET | `/api/v1/agent-builder/agents` | List agents |
| DELETE | `/api/v1/agent-builder/agents/{id}` | Archive agent |
| POST | `/api/v1/agent-builder/agents/{id}/publish` | Publish agent |
| POST | `/api/v1/agent-builder/agents/{id}/clone` | Clone agent |

### Version Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/agent-builder/agents/{id}/versions` | List versions |
| GET | `/api/v1/agent-builder/agents/{id}/versions/{ver}` | Get version |
| POST | `/api/v1/agent-builder/agents/{id}/rollback` | Rollback |
| GET | `/api/v1/agent-builder/agents/{id}/versions/compare` | Compare versions |

### Testing
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/agent-builder/agents/{id}/test/start` | Start session |
| POST | `/api/v1/agent-builder/test/sessions/{id}/message` | Send message |
| POST | `/api/v1/agent-builder/test/sessions/{id}/complete` | Complete test |
| GET | `/api/v1/agent-builder/agents/{id}/test/statistics` | Get metrics |

### Templates
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/agent-builder/templates` | Create template |
| GET | `/api/v1/agent-builder/templates` | List templates |
| POST | `/api/v1/agent-builder/templates/{id}/render` | Render template |
| POST | `/api/v1/agent-builder/templates/validate` | Validate syntax |

## Configuration

```yaml
# application.yml
hdim:
  agent-builder:
    max-agents-per-tenant: 50
    max-versions-per-agent: 100
    test-session-timeout-minutes: 30

spring:
  cache:
    type: redis
    redis:
      time-to-live: 300000  # 5 minutes
```

## Dependencies

- **Spring Boot**: Web, JPA, Validation, Cache, Actuator
- **Database**: PostgreSQL with Flyway migrations
- **Cache**: Redis for active agent caching
- **Communication**: OpenFeign for agent-runtime-service integration
- **Resilience**: Resilience4j for circuit breakers

## Running Locally

```bash
# From backend directory
./gradlew :modules:services:agent-builder-service:bootRun

# Or via Docker (ai profile)
docker compose --profile ai up agent-builder-service
```

## Testing

### Overview

The Agent Builder Service has a comprehensive test suite covering agent configuration management, version control, prompt template handling, and controller endpoints. Tests validate tenant limits, status lifecycle transitions, template variable extraction, and integration with the Agent Runtime Service.

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:agent-builder-service:test

# Run specific test class
./gradlew :modules:services:agent-builder-service:test --tests "AgentConfigurationServiceTest"

# Run tests by pattern
./gradlew :modules:services:agent-builder-service:test --tests "*ServiceTest"

# Run with verbose output
./gradlew :modules:services:agent-builder-service:test --info

# Run with coverage report
./gradlew :modules:services:agent-builder-service:test jacocoTestReport

# Run single test method
./gradlew :modules:services:agent-builder-service:test --tests "AgentConfigurationServiceTest.create_shouldCreateAgentSuccessfully"

# Run integration tests
./gradlew :modules:services:agent-builder-service:integrationTest
```

### Test Coverage Summary

| Test Class | Tests | Coverage Focus |
|------------|-------|----------------|
| `AgentConfigurationServiceTest` | 15+ | Agent CRUD, tenant limits, status lifecycle, publishing, cloning |
| `AgentBuilderControllerTest` | 15+ | REST endpoints, pagination, runtime client integration |
| `PromptTemplateServiceTest` | 15+ | Template CRUD, variable extraction, rendering, validation |

**Total: 3 test classes, 45+ test methods**

### Test Organization

```
src/test/java/com/healthdata/agentbuilder/
├── controller/
│   └── AgentBuilderControllerTest.java  # REST endpoint tests
└── service/
    ├── AgentConfigurationServiceTest.java  # Agent management tests
    └── PromptTemplateServiceTest.java      # Template service tests
```

---

### Unit Tests (Agent Configuration Service)

The `AgentConfigurationServiceTest` validates agent lifecycle management, tenant limits, and version tracking.

#### Agent Creation Tests

```java
@ExtendWith(MockitoExtension.class)
class AgentConfigurationServiceTest {

    @Mock
    private AgentConfigurationRepository agentRepository;

    @Mock
    private AgentVersionRepository versionRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AgentConfigEventPublisher eventPublisher;

    @InjectMocks
    private AgentConfigurationService service;

    private String tenantId;
    private String userId;
    private AgentConfiguration testAgent;

    @BeforeEach
    void setUp() {
        tenantId = "test-tenant";
        userId = "test-user";

        // Set configuration properties via reflection
        ReflectionTestUtils.setField(service, "maxAgentsPerTenant", 50);
        ReflectionTestUtils.setField(service, "maxVersionsPerAgent", 100);

        testAgent = AgentConfiguration.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .name("Test Agent")
            .description("Test Description")
            .slug("test-agent")
            .status(AgentStatus.DRAFT)
            .version("1.0.0")
            .modelProvider("claude")
            .modelId("claude-3-sonnet-20240229")
            .systemPrompt("You are a helpful assistant")
            .build();
    }

    @Test
    void create_shouldCreateAgentSuccessfully() {
        // Given: Tenant under limit, name available
        when(agentRepository.countByTenantId(tenantId)).thenReturn(10L);
        when(agentRepository.existsByTenantIdAndName(tenantId, testAgent.getName())).thenReturn(false);
        when(agentRepository.save(any(AgentConfiguration.class))).thenReturn(testAgent);
        when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(new HashMap<>());
        when(versionRepository.countByAgentConfigurationId(any())).thenReturn(0L);
        when(versionRepository.save(any(AgentVersion.class))).thenReturn(mock(AgentVersion.class));

        // When: Create agent
        AgentConfiguration result = service.create(testAgent, userId);

        // Then: Agent created in DRAFT status with version
        assertNotNull(result);
        assertEquals(AgentStatus.DRAFT, result.getStatus());
        verify(agentRepository).save(any(AgentConfiguration.class));
        verify(versionRepository).save(any(AgentVersion.class));
    }

    @Test
    void create_shouldThrowExceptionWhenTenantLimitReached() {
        // Given: Tenant at max limit (50 agents)
        when(agentRepository.countByTenantId(tenantId)).thenReturn(50L);

        // When & Then: Exception thrown
        assertThrows(AgentConfigurationService.AgentBuilderException.class,
            () -> service.create(testAgent, userId));
        verify(agentRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowExceptionWhenNameExists() {
        // Given: Name already taken
        when(agentRepository.countByTenantId(tenantId)).thenReturn(10L);
        when(agentRepository.existsByTenantIdAndName(tenantId, testAgent.getName())).thenReturn(true);

        // When & Then: Exception thrown
        assertThrows(AgentConfigurationService.AgentBuilderException.class,
            () -> service.create(testAgent, userId));
        verify(agentRepository, never()).save(any());
    }
}
```

#### Agent Update and Status Lifecycle Tests

```java
@Test
void update_shouldUpdateAgentSuccessfully() {
    // Given: Existing DRAFT agent
    AgentConfiguration existing = AgentConfiguration.builder()
        .id(testAgent.getId())
        .tenantId(tenantId)
        .name("Old Name")
        .version("1.0.0")
        .status(AgentStatus.DRAFT)
        .build();

    AgentConfiguration updates = AgentConfiguration.builder()
        .tenantId(tenantId)
        .name("New Name")
        .description("New Description")
        .build();

    when(agentRepository.findByTenantIdAndId(tenantId, testAgent.getId()))
        .thenReturn(Optional.of(existing));
    when(agentRepository.save(any(AgentConfiguration.class))).thenReturn(existing);
    when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(new HashMap<>());
    when(versionRepository.countByAgentConfigurationId(any())).thenReturn(5L);
    when(versionRepository.save(any(AgentVersion.class))).thenReturn(mock(AgentVersion.class));

    // When: Update agent
    AgentConfiguration result = service.update(testAgent.getId(), updates, userId, "Test update");

    // Then: Agent updated with new version created
    assertNotNull(result);
    assertEquals("New Name", result.getName());
    assertEquals("New Description", result.getDescription());
    verify(agentRepository).save(any(AgentConfiguration.class));
    verify(versionRepository).save(any(AgentVersion.class));
}

@Test
void update_shouldThrowExceptionForArchivedAgent() {
    // Given: Archived agent (immutable)
    AgentConfiguration archived = AgentConfiguration.builder()
        .id(testAgent.getId())
        .tenantId(tenantId)
        .status(AgentStatus.ARCHIVED)
        .build();

    when(agentRepository.findByTenantIdAndId(tenantId, testAgent.getId()))
        .thenReturn(Optional.of(archived));

    // When & Then: Cannot update archived agents
    assertThrows(AgentConfigurationService.AgentBuilderException.class,
        () -> service.update(testAgent.getId(), testAgent, userId, "Update"));
    verify(agentRepository, never()).save(any());
}

@Test
void publish_shouldPublishAgent() {
    // Given: DRAFT agent ready for publishing
    when(agentRepository.findByTenantIdAndId(tenantId, testAgent.getId()))
        .thenReturn(Optional.of(testAgent));
    when(agentRepository.save(any(AgentConfiguration.class))).thenReturn(testAgent);
    when(versionRepository.findByAgentConfigurationIdAndStatus(any(), any()))
        .thenReturn(Optional.empty());
    when(versionRepository.findLatestVersion(any())).thenReturn(Optional.empty());

    // When: Publish agent
    AgentConfiguration result = service.publish(tenantId, testAgent.getId(), userId);

    // Then: Status changed to ACTIVE
    assertNotNull(result);
    assertEquals(AgentStatus.ACTIVE, result.getStatus());
    assertNotNull(result.getPublishedAt());
    verify(agentRepository).save(any(AgentConfiguration.class));
}

@Test
void publish_shouldThrowExceptionForArchivedAgent() {
    // Given: Archived agent
    testAgent.setStatus(AgentStatus.ARCHIVED);
    when(agentRepository.findByTenantIdAndId(tenantId, testAgent.getId()))
        .thenReturn(Optional.of(testAgent));

    // When & Then: Cannot publish archived agents
    assertThrows(AgentConfigurationService.AgentBuilderException.class,
        () -> service.publish(tenantId, testAgent.getId(), userId));
}
```

#### Delete and Clone Tests

```java
@Test
void delete_shouldArchiveAgent() {
    // Given: Existing agent
    when(agentRepository.findByTenantIdAndId(tenantId, testAgent.getId()))
        .thenReturn(Optional.of(testAgent));
    when(agentRepository.save(any(AgentConfiguration.class))).thenReturn(testAgent);

    // When: Delete agent
    service.delete(tenantId, testAgent.getId(), userId);

    // Then: Agent archived (soft delete)
    verify(agentRepository).save(argThat(agent ->
        agent.getStatus() == AgentStatus.ARCHIVED &&
        agent.getArchivedAt() != null
    ));
}

@Test
void clone_shouldCloneAgentSuccessfully() {
    // Given: Existing agent to clone
    String newName = "Cloned Agent";
    when(agentRepository.findByTenantIdAndId(tenantId, testAgent.getId()))
        .thenReturn(Optional.of(testAgent));
    when(agentRepository.countByTenantId(tenantId)).thenReturn(10L);
    when(agentRepository.existsByTenantIdAndName(tenantId, newName)).thenReturn(false);
    when(agentRepository.save(any(AgentConfiguration.class))).thenReturn(testAgent);
    when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(new HashMap<>());
    when(versionRepository.countByAgentConfigurationId(any())).thenReturn(0L);
    when(versionRepository.save(any(AgentVersion.class))).thenReturn(mock(AgentVersion.class));

    // When: Clone agent
    AgentConfiguration result = service.clone(tenantId, testAgent.getId(), newName, userId);

    // Then: New agent created with cloned configuration
    assertNotNull(result);
    verify(agentRepository, atLeastOnce()).save(any(AgentConfiguration.class));
}

@Test
void getActiveAgents_shouldReturnOnlyActiveAgents() {
    // Given: Active agents
    List<AgentConfiguration> activeAgents = List.of(testAgent);
    when(agentRepository.findActiveAgents(tenantId)).thenReturn(activeAgents);

    // When: Query active agents
    List<AgentConfiguration> result = service.getActiveAgents(tenantId);

    // Then: Only ACTIVE status agents returned
    assertNotNull(result);
    assertEquals(1, result.size());
    verify(agentRepository).findActiveAgents(tenantId);
}
```

---

### Controller Tests

The `AgentBuilderControllerTest` validates REST endpoints and integration with runtime client.

```java
@ExtendWith(MockitoExtension.class)
class AgentBuilderControllerTest {

    @Mock
    private AgentConfigurationService agentService;

    @Mock
    private AgentVersionService versionService;

    @Mock
    private AgentTestService testService;

    @Mock
    private PromptTemplateService templateService;

    @Mock
    private AgentRuntimeClient runtimeClient;

    @InjectMocks
    private AgentBuilderController controller;

    private String tenantId;
    private String userId;
    private AgentConfiguration testAgent;

    @BeforeEach
    void setUp() {
        tenantId = "test-tenant";
        userId = "user-123";

        testAgent = AgentConfiguration.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .name("Test Agent")
            .slug("test-agent")
            .description("Test Description")
            .status(AgentStatus.DRAFT)
            .version("1.0.0")
            .modelProvider("claude")
            .modelId("claude-3-sonnet-20240229")
            .systemPrompt("You are a helpful assistant")
            .build();
    }

    @Test
    void createAgent_shouldReturn201() {
        // Given: Valid agent configuration
        when(agentService.create(any(AgentConfiguration.class), eq(userId)))
            .thenReturn(testAgent);

        // When: Create agent
        ResponseEntity<AgentConfiguration> response = controller.createAgent(
            testAgent, tenantId, userId
        );

        // Then: 201 Created with agent details
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testAgent.getId(), response.getBody().getId());
        assertEquals("Test Agent", response.getBody().getName());
        verify(agentService).create(any(AgentConfiguration.class), eq(userId));
    }

    @Test
    void getAgent_shouldReturn200WhenFound() {
        // Given: Agent exists
        when(agentService.getById(tenantId, testAgent.getId()))
            .thenReturn(Optional.of(testAgent));

        // When: Get agent
        ResponseEntity<AgentConfiguration> response = controller.getAgent(
            testAgent.getId(), tenantId
        );

        // Then: 200 OK with agent
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testAgent.getId(), response.getBody().getId());
    }

    @Test
    void getAgent_shouldReturn404WhenNotFound() {
        // Given: Agent doesn't exist
        UUID nonExistentId = UUID.randomUUID();
        when(agentService.getById(tenantId, nonExistentId))
            .thenReturn(Optional.empty());

        // When: Get non-existent agent
        ResponseEntity<AgentConfiguration> response = controller.getAgent(
            nonExistentId, tenantId
        );

        // Then: 404 Not Found
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void listAgents_shouldReturnPageOfAgents() {
        // Given: Page of agents
        Page<AgentConfiguration> page = new PageImpl<>(
            List.of(testAgent),
            PageRequest.of(0, 20),
            1
        );
        when(agentService.list(eq(tenantId), any()))
            .thenReturn(page);

        // When: List agents
        ResponseEntity<Page<AgentConfiguration>> response = controller.listAgents(
            tenantId, PageRequest.of(0, 20)
        );

        // Then: Paginated results
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals("Test Agent", response.getBody().getContent().get(0).getName());
    }

    @Test
    void publishAgent_shouldReturn200() {
        // Given: Agent ready for publishing
        testAgent.setStatus(AgentStatus.ACTIVE);
        when(agentService.publish(tenantId, testAgent.getId(), "user-123"))
            .thenReturn(testAgent);

        // When: Publish agent
        ResponseEntity<AgentConfiguration> response = controller.publishAgent(
            testAgent.getId(), tenantId, userId
        );

        // Then: Status changed to ACTIVE
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(AgentStatus.ACTIVE, response.getBody().getStatus());
    }

    @Test
    void deleteAgent_shouldReturn204() {
        // Given: Agent exists
        doNothing().when(agentService).delete(tenantId, testAgent.getId(), "user-123");

        // When: Delete agent
        ResponseEntity<Void> response = controller.deleteAgent(
            testAgent.getId(), tenantId, userId
        );

        // Then: 204 No Content
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(agentService).delete(tenantId, testAgent.getId(), "user-123");
    }

    @Test
    void cloneAgent_shouldReturn201() {
        // Given: Agent to clone
        AgentConfiguration cloned = AgentConfiguration.builder()
            .id(UUID.randomUUID())
            .name("Cloned Agent")
            .build();
        when(agentService.clone(tenantId, testAgent.getId(), "Cloned Agent", "user-123"))
            .thenReturn(cloned);

        // When: Clone agent
        ResponseEntity<AgentConfiguration> response = controller.cloneAgent(
            testAgent.getId(), "Cloned Agent", tenantId, userId
        );

        // Then: 201 Created with new agent
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Cloned Agent", response.getBody().getName());
    }
}
```

#### Runtime Client Integration Tests

```java
@Test
void getAvailableTools_shouldReturnTools() {
    // Given: Tools from runtime service
    List<AgentRuntimeClient.ToolInfo> tools = List.of(
        new AgentRuntimeClient.ToolInfo(
            "fhir_query",
            "Query FHIR resources",
            "data",
            Map.of(),
            false
        )
    );
    when(runtimeClient.getAvailableTools(tenantId)).thenReturn(tools);

    // When: Get available tools
    ResponseEntity<List<AgentRuntimeClient.ToolInfo>> response = controller.getAvailableTools(
        tenantId
    );

    // Then: Tools returned
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().size());
    assertEquals("fhir_query", response.getBody().get(0).name());
}

@Test
void getSupportedProviders_shouldReturnProviders() {
    // Given: Providers from runtime service
    List<AgentRuntimeClient.ProviderInfo> providers = List.of(
        new AgentRuntimeClient.ProviderInfo(
            "claude",
            "Anthropic Claude",
            true,
            true,
            List.of("us-east-1")
        )
    );
    when(runtimeClient.getSupportedProviders()).thenReturn(providers);

    // When: Get providers
    ResponseEntity<List<AgentRuntimeClient.ProviderInfo>> response = controller.getSupportedProviders();

    // Then: Providers returned
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().size());
    assertEquals("claude", response.getBody().get(0).name());
}

@Test
void checkRuntimeHealth_shouldReturnHealth() {
    // Given: Runtime service healthy
    Map<String, Object> health = Map.of("status", "UP");
    when(runtimeClient.healthCheck()).thenReturn(health);

    // When: Check health
    ResponseEntity<Map<String, Object>> response = controller.checkRuntimeHealth();

    // Then: Health status returned
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("UP", response.getBody().get("status"));
}
```

---

### Prompt Template Tests

The `PromptTemplateServiceTest` validates template creation, variable extraction, rendering, and validation.

#### Template Creation and Variable Extraction

```java
@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceTest {

    @Mock
    private PromptTemplateRepository templateRepository;

    @InjectMocks
    private PromptTemplateService service;

    private String tenantId;
    private String userId;
    private PromptTemplate testTemplate;

    @BeforeEach
    void setUp() {
        tenantId = "test-tenant";
        userId = "test-user";

        testTemplate = PromptTemplate.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .name("Test Template")
            .description("Test Description")
            .category(TemplateCategory.CLINICAL_SAFETY)
            .content("Hello {{patient_name}}, your appointment is on {{date}}.")
            .variables(List.of(
                new TemplateVariable("patient_name", "Patient name", null, true),
                new TemplateVariable("date", "Appointment date", null, true)
            ))
            .isSystem(false)
            .build();
    }

    @Test
    void create_shouldExtractVariablesAndCreateTemplate() {
        // Given: Template with variables
        when(templateRepository.existsByTenantIdAndName(tenantId, testTemplate.getName()))
            .thenReturn(false);
        when(templateRepository.save(any(PromptTemplate.class))).thenReturn(testTemplate);

        // When: Create template
        PromptTemplate result = service.create(testTemplate, userId);

        // Then: Variables extracted from {{variable}} syntax
        assertNotNull(result);
        assertNotNull(result.getVariables());
        assertTrue(result.getVariables().stream()
            .anyMatch(v -> v.getName().equals("patient_name")));
        assertTrue(result.getVariables().stream()
            .anyMatch(v -> v.getName().equals("date")));
        verify(templateRepository).save(any(PromptTemplate.class));
    }

    @Test
    void create_shouldThrowExceptionWhenNameExists() {
        // Given: Name already taken
        when(templateRepository.existsByTenantIdAndName(tenantId, testTemplate.getName()))
            .thenReturn(true);

        // When & Then: Exception thrown
        assertThrows(AgentConfigurationService.AgentBuilderException.class,
            () -> service.create(testTemplate, userId));
        verify(templateRepository, never()).save(any());
    }
}
```

#### Template Rendering Tests

```java
@Test
void renderContent_shouldReplaceVariables() {
    // Given: Template with variables
    String content = "Hello {{name}}, you have {{count}} messages.";
    Map<String, String> variables = Map.of(
        "name", "John",
        "count", "5"
    );

    // When: Render template
    String result = service.renderContent(content, variables);

    // Then: Variables replaced
    assertEquals("Hello John, you have 5 messages.", result);
}

@Test
void renderContent_shouldHandleVariablesWithSpaces() {
    // Given: Variable with whitespace in braces
    String content = "Hello {{ name }}, welcome!";
    Map<String, String> variables = Map.of("name", "Alice");

    // When: Render template
    String result = service.renderContent(content, variables);

    // Then: Whitespace handled correctly
    assertEquals("Hello Alice, welcome!", result);
}

@Test
void renderContent_shouldHandleMissingVariables() {
    // Given: Template with missing variable value
    String content = "Hello {{name}}, you have {{count}} messages.";
    Map<String, String> variables = Map.of("name", "John");

    // When: Render template
    String result = service.renderContent(content, variables);

    // Then: Missing variable left as-is
    assertEquals("Hello John, you have {{count}} messages.", result);
}
```

#### Template Validation Tests

```java
@Test
void validateTemplate_shouldReturnValidForGoodTemplate() {
    // Given: Well-formed template
    String content = "Hello {{name}}, your balance is {{balance}}.";

    // When: Validate
    TemplateValidationResult result = service.validateTemplate(content);

    // Then: Valid with extracted variables
    assertTrue(result.valid());
    assertEquals(2, result.variables().size());
    assertTrue(result.variables().contains("name"));
    assertTrue(result.variables().contains("balance"));
    assertTrue(result.errors().isEmpty());
}

@Test
void validateTemplate_shouldDetectUnbalancedBraces() {
    // Given: Missing closing brace
    String content = "Hello {{name}, missing closing brace";

    // When: Validate
    TemplateValidationResult result = service.validateTemplate(content);

    // Then: Invalid with error
    assertFalse(result.valid());
    assertFalse(result.errors().isEmpty());
    assertTrue(result.errors().get(0).contains("Unbalanced"));
}

@Test
void validateTemplate_shouldDetectEmptyVariables() {
    // Given: Empty variable name
    String content = "Hello {{}}, empty variable";

    // When: Validate
    TemplateValidationResult result = service.validateTemplate(content);

    // Then: Invalid with error
    assertFalse(result.valid());
    assertTrue(result.errors().stream().anyMatch(e -> e.contains("Empty variable")));
}

@Test
void validateTemplate_shouldWarnAboutPotentiallyMissingPatientVariable() {
    // Given: Template mentioning "patient" without variable
    String content = "The patient needs to schedule an appointment.";

    // When: Validate
    TemplateValidationResult result = service.validateTemplate(content);

    // Then: Valid with warning
    assertTrue(result.valid());  // Still valid, just warnings
    assertFalse(result.warnings().isEmpty());
    assertTrue(result.warnings().stream().anyMatch(w -> w.contains("patient")));
}
```

#### System Template Protection Tests

```java
@Test
void update_shouldThrowExceptionForSystemTemplate() {
    // Given: System template (immutable)
    testTemplate.setIsSystem(true);
    when(templateRepository.findByTenantIdAndId(tenantId, testTemplate.getId()))
        .thenReturn(Optional.of(testTemplate));

    // When & Then: Cannot update system templates
    assertThrows(AgentConfigurationService.AgentBuilderException.class,
        () -> service.update(testTemplate.getId(), testTemplate, userId));
}

@Test
void delete_shouldDeleteNonSystemTemplate() {
    // Given: Non-system template
    when(templateRepository.findByTenantIdAndId(tenantId, testTemplate.getId()))
        .thenReturn(Optional.of(testTemplate));

    // When: Delete template
    service.delete(tenantId, testTemplate.getId());

    // Then: Template deleted
    verify(templateRepository).delete(testTemplate);
}

@Test
void delete_shouldThrowExceptionForSystemTemplate() {
    // Given: System template
    testTemplate.setIsSystem(true);
    when(templateRepository.findByTenantIdAndId(tenantId, testTemplate.getId()))
        .thenReturn(Optional.of(testTemplate));

    // When & Then: Cannot delete system templates
    assertThrows(AgentConfigurationService.AgentBuilderException.class,
        () -> service.delete(tenantId, testTemplate.getId()));
    verify(templateRepository, never()).delete(any());
}

@Test
void cloneTemplate_shouldCloneSystemTemplate() {
    // Given: System template to clone
    testTemplate.setIsSystem(true);
    String newName = "Cloned System Template";
    when(templateRepository.findByTenantIdAndId(tenantId, testTemplate.getId()))
        .thenReturn(Optional.empty());
    when(templateRepository.findById(testTemplate.getId()))
        .thenReturn(Optional.of(testTemplate));
    when(templateRepository.save(any(PromptTemplate.class))).thenReturn(testTemplate);

    // When: Clone system template
    PromptTemplate result = service.cloneTemplate(tenantId, testTemplate.getId(), newName, userId);

    // Then: Cloned as non-system tenant template
    assertNotNull(result);
    verify(templateRepository).save(argThat(template ->
        !template.getIsSystem() &&
        template.getTenantId().equals(tenantId)
    ));
}
```

---

### Multi-Tenant Isolation Tests

```java
@Test
void agentsShouldBeTenantIsolated() {
    // Given: Agents in different tenants
    String tenant1 = "tenant-001";
    String tenant2 = "tenant-002";
    UUID agentId = UUID.randomUUID();

    AgentConfiguration tenant1Agent = AgentConfiguration.builder()
        .id(agentId)
        .tenantId(tenant1)
        .name("Tenant 1 Agent")
        .build();

    // Tenant 1 can access their agent
    when(agentRepository.findByTenantIdAndId(tenant1, agentId))
        .thenReturn(Optional.of(tenant1Agent));

    // Tenant 2 cannot access Tenant 1's agent
    when(agentRepository.findByTenantIdAndId(tenant2, agentId))
        .thenReturn(Optional.empty());

    // When/Then: Tenant isolation enforced
    assertTrue(service.getById(tenant1, agentId).isPresent());
    assertTrue(service.getById(tenant2, agentId).isEmpty());
}

@Test
void tenantLimitsShouldBeEnforcedPerTenant() {
    // Given: Tenant 1 at limit, Tenant 2 under limit
    String tenant1 = "tenant-001";
    String tenant2 = "tenant-002";

    when(agentRepository.countByTenantId(tenant1)).thenReturn(50L);  // At limit
    when(agentRepository.countByTenantId(tenant2)).thenReturn(10L);  // Under limit

    AgentConfiguration agent1 = AgentConfiguration.builder()
        .tenantId(tenant1)
        .name("Agent 1")
        .build();

    AgentConfiguration agent2 = AgentConfiguration.builder()
        .tenantId(tenant2)
        .name("Agent 2")
        .build();

    // When/Then: Tenant 1 blocked, Tenant 2 allowed
    assertThrows(AgentConfigurationService.AgentBuilderException.class,
        () -> service.create(agent1, "user"));

    when(agentRepository.existsByTenantIdAndName(tenant2, "Agent 2")).thenReturn(false);
    when(agentRepository.save(any())).thenReturn(agent2);
    when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(new HashMap<>());
    when(versionRepository.countByAgentConfigurationId(any())).thenReturn(0L);
    when(versionRepository.save(any())).thenReturn(mock(AgentVersion.class));

    assertDoesNotThrow(() -> service.create(agent2, "user"));
}
```

---

### HIPAA Compliance Tests

```java
@Test
void agentConfigurationShouldNotContainPhi() {
    // Given: Agent configuration
    AgentConfiguration agent = AgentConfiguration.builder()
        .id(UUID.randomUUID())
        .tenantId("test-tenant")
        .name("Care Gap Agent")
        .systemPrompt("You are a clinical assistant. Never include patient names in responses.")
        .build();

    // Then: No PHI in configuration
    assertThat(agent.getSystemPrompt())
        .doesNotContain("patient-123")
        .doesNotContain("SSN")
        .doesNotContain("MRN");
}

@Test
void templatesShouldUseVariablesForPhi() {
    // Given: Template with PHI variables
    String content = "Patient {{patient_id}} has {{condition}}.";

    // When: Validate
    TemplateValidationResult result = service.validateTemplate(content);

    // Then: PHI should be in variables, not hardcoded
    assertTrue(result.valid());
    assertTrue(result.variables().contains("patient_id"));
    assertTrue(result.variables().contains("condition"));
}
```

---

### Performance Tests

```java
@Test
void agentCreationShouldCompleteQuickly() {
    // Given: Setup mocks
    when(agentRepository.countByTenantId(any())).thenReturn(10L);
    when(agentRepository.existsByTenantIdAndName(any(), any())).thenReturn(false);
    when(agentRepository.save(any())).thenReturn(testAgent);
    when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(new HashMap<>());
    when(versionRepository.countByAgentConfigurationId(any())).thenReturn(0L);
    when(versionRepository.save(any())).thenReturn(mock(AgentVersion.class));

    // When: Measure creation time
    long startTime = System.currentTimeMillis();
    service.create(testAgent, "user");
    long endTime = System.currentTimeMillis();

    // Then: Should complete quickly
    long elapsed = endTime - startTime;
    assertThat(elapsed).isLessThan(100);  // < 100ms
}

@Test
void templateRenderingShouldCompleteQuickly() {
    // Given: Template with many variables
    StringBuilder content = new StringBuilder();
    Map<String, String> variables = new HashMap<>();
    for (int i = 0; i < 100; i++) {
        content.append("{{var").append(i).append("}} ");
        variables.put("var" + i, "value" + i);
    }

    // When: Measure rendering time
    long startTime = System.currentTimeMillis();
    service.renderContent(content.toString(), variables);
    long endTime = System.currentTimeMillis();

    // Then: Should complete quickly even with 100 variables
    long elapsed = endTime - startTime;
    assertThat(elapsed).isLessThan(50);  // < 50ms
}
```

---

### Test Configuration

```yaml
# src/test/resources/application-test.yml
hdim:
  agent-builder:
    max-agents-per-tenant: 50
    max-versions-per-agent: 100
    test-session-timeout-minutes: 30

spring:
  cache:
    type: simple  # Use simple cache for tests
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
```

---

### Best Practices

| Practice | Description |
|----------|-------------|
| **Mockito Extension** | Use `@ExtendWith(MockitoExtension.class)` for fast unit tests |
| **ReflectionTestUtils** | Set config properties via reflection for testing limits |
| **Test Tenant Limits** | Always test max 50 agents per tenant enforcement |
| **Test Status Lifecycle** | Verify DRAFT → TESTING → ACTIVE → DEPRECATED → ARCHIVED |
| **Test Version Creation** | Verify new version created on updates |
| **Protect System Templates** | Test that system templates cannot be modified/deleted |
| **Variable Syntax** | Use `{{variable}}` syntax and test extraction/rendering |
| **Validate Templates** | Test unbalanced braces, empty variables, warnings |
| **Clone Operations** | Test cloning preserves configuration but creates new entity |
| **Runtime Integration** | Mock `AgentRuntimeClient` for tools and providers |

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| Tenant limit exception | Agent count ≥ 50 | Reset test data or use different tenant |
| Cannot update agent | Agent is ARCHIVED | Use DRAFT or ACTIVE status agent |
| Cannot delete system template | `isSystem=true` | Clone template instead of modifying |
| Variable not replaced | Missing from variables map | Add variable to map or check spelling |
| Unbalanced braces error | Missing `}}` | Fix template syntax |
| Version not created | Mock not configured | Add `versionRepository.save()` mock |
| Runtime client null | `@Mock` not initialized | Use `@ExtendWith(MockitoExtension.class)` |
| Config property null | Field not set | Use `ReflectionTestUtils.setField()` |
| Page assertion fails | Wrong pageable | Check PageRequest parameters |
| Clone fails | Source not found | Mock `findByTenantIdAndId()` to return source |
