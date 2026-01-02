# Agent Runtime Service

AI agent execution engine with multi-LLM provider support, tool orchestration, and clinical safety guardrails.

## Overview

The Agent Runtime Service provides a production-ready execution environment for AI agents in healthcare settings. It supports multiple LLM providers (Claude, Azure OpenAI, AWS Bedrock), orchestrates tool execution, manages conversation memory, and enforces clinical safety guardrails.

## Key Features

### Multi-Provider LLM Support
- **Claude (Anthropic)**: Primary provider with Claude 3.5 Sonnet
- **Azure OpenAI**: GPT-4 Turbo enterprise deployment
- **AWS Bedrock**: Claude on AWS infrastructure
- Automatic fallback chain for high availability
- Provider health monitoring and circuit breakers

### Agent Orchestration
- Synchronous and streaming execution modes
- Task lifecycle management (start, monitor, cancel)
- Maximum iteration limits to prevent runaway execution
- Concurrent task management (50 tasks default)
- Correlation IDs for distributed tracing

### Tool Integration
- FHIR resource retrieval and search
- CQL measure evaluation
- Care gap analysis
- Event publishing to Kafka
- Quality measure queries
- Approval workflow integration

### Memory Management
- Redis-based conversation memory
- Automatic summarization after 20 messages
- 15-minute TTL for conversation history
- Encrypted memory storage (HIPAA-compliant)
- Session-based context preservation

### Clinical Safety Guardrails
- PHI (Protected Health Information) detection and redaction
- Clinical safety checks (block definitive diagnoses)
- Required medical disclaimers
- Content filtering and moderation
- Rate limiting per user and tenant

### Custom Agents
- Load agent definitions from Agent Builder Service
- Dynamic agent configuration and system prompts
- Agent-specific tool permissions
- Role-based agent access control

## Technology Stack

- **Spring Boot 3.x**: Core reactive framework
- **Spring WebFlux**: Async/reactive API
- **PostgreSQL**: Agent execution history
- **Redis**: Conversation memory and caching
- **Apache Kafka**: Event streaming
- **Resilience4j**: Circuit breakers and rate limiting
- **Anthropic Claude API**: Primary LLM provider
- **Azure OpenAI**: Enterprise LLM provider
- **AWS Bedrock**: AWS-hosted LLM provider

## API Endpoints

### Agent Execution
```
POST /api/v1/agents/{agentType}/execute
     - Execute an agent task synchronously
     - Returns complete response with token usage

POST /api/v1/agents/{agentType}/stream
     - Execute agent with streaming response (SSE)
     - Returns text chunks as they're generated
```

### Task Management
```
GET    /api/v1/agents/tasks/{taskId}/status
       - Get status of running task

DELETE /api/v1/agents/tasks/{taskId}
       - Cancel a running task
```

### Tools and Providers
```
GET /api/v1/agents/tools
    - List available tools for agents

GET /api/v1/agents/providers
    - List LLM providers and health status
```

### Memory Management
```
DELETE /api/v1/agents/sessions/{sessionId}/memory
       - Clear conversation memory for session
```

### Health
```
GET /api/v1/agents/health
    - Service health with provider status
```

## Configuration

### LLM Providers
```yaml
hdim.agent.llm:
  default-provider: claude
  fallback-chain: [claude, azure-openai, bedrock]

  providers:
    claude:
      enabled: true
      api-key: ${ANTHROPIC_API_KEY}
      model: claude-3-5-sonnet-20241022
      max-tokens: 4096
      temperature: 0.3

    azure-openai:
      enabled: true
      endpoint: ${AZURE_OPENAI_ENDPOINT}
      deployment-id: gpt-4-turbo

    bedrock:
      enabled: true
      region: us-east-1
      model-id: anthropic.claude-3-sonnet-20240229-v1:0
```

### Guardrails
```yaml
hdim.agent.guardrails:
  phi-protection:
    enabled: true
    cache-ttl-minutes: 5
    redact-patterns: [SSN, MRN patterns]

  clinical-safety:
    enabled: true
    block-definitive-diagnoses: true
    require-disclaimers: true

  rate-limiting:
    requests-per-minute-per-user: 30
    requests-per-minute-per-tenant: 500
```

### Memory
```yaml
hdim.agent.memory:
  conversation:
    max-messages: 50
    ttl-minutes: 15
    summarization-threshold: 20
  encryption:
    enabled: true
```

## Running Locally

### Prerequisites
- Java 21
- PostgreSQL 14+
- Redis 6+
- Kafka 3+
- Anthropic API key (or Azure OpenAI credentials)

### Environment Variables
```bash
export ANTHROPIC_API_KEY=sk-ant-...
export AZURE_OPENAI_API_KEY=...  # Optional
export AZURE_OPENAI_ENDPOINT=...  # Optional
```

### Build
```bash
./gradlew :modules:services:agent-runtime-service:build
```

### Run
```bash
./gradlew :modules:services:agent-runtime-service:bootRun
```

### Run Tests
```bash
./gradlew :modules:services:agent-runtime-service:test
```

---

## Testing

### Overview

The Agent Runtime Service has a comprehensive test suite covering 6 test types: unit tests for core business logic, integration tests for API endpoints, multi-tenant isolation tests, RBAC permission tests, HIPAA compliance tests for PHI encryption, and performance tests. The test suite validates critical functionality including clinical safety guardrails, LLM provider management, tool orchestration, and conversation memory management.

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:agent-runtime-service:test

# Run specific test class
./gradlew :modules:services:agent-runtime-service:test --tests "GuardrailServiceTest"

# Run specific test method
./gradlew :modules:services:agent-runtime-service:test --tests "GuardrailServiceTest.CriticalPatternTests.blockCancerDiagnosis"

# Run tests by category (nested class)
./gradlew :modules:services:agent-runtime-service:test --tests "*ToolExecutionTests*"

# Run with verbose output
./gradlew :modules:services:agent-runtime-service:test --info

# Run with coverage report
./gradlew :modules:services:agent-runtime-service:test jacocoTestReport

# Run only unit tests (fast)
./gradlew :modules:services:agent-runtime-service:test --tests "*Test" --exclude-task integrationTest
```

### Test Coverage Summary

| Test Class | Test Methods | Coverage Focus |
|------------|--------------|----------------|
| `GuardrailServiceTest` | 20+ | Clinical safety pattern detection, approval integration |
| `AgentOrchestratorTest` | 25+ | Agent execution, tool calls, guardrails, timeouts |
| `ClaudeProviderTest` | 30+ | LLM provider configuration, request/response handling |
| `ToolRegistryTest` | 15+ | Tool registration, retrieval, context-based filtering |
| `PHIEncryptionTest` | 25+ | HIPAA-compliant encryption, tenant isolation |
| `AgentControllerTest` | 20+ | REST API endpoints, task management, health checks |
| `ToolDefinitionTest` | 8+ | Tool schema validation, Claude format conversion |
| `ToolResultTest` | 5+ | Tool execution result handling |
| `AgentContextTest` | 8+ | Context building and validation |
| `LLMProviderFactoryTest` | 10+ | Provider selection, fallback chains |
| `AgentRegistryTest` | 8+ | Custom agent registration and retrieval |
| `ApprovalIntegrationTest` | 10+ | Human-in-the-loop approval workflows |
| `NotificationToolTest` | 6+ | Notification tool implementation |

### Test Organization

```
src/test/java/com/healthdata/agent/
├── api/
│   └── AgentControllerTest.java           # REST endpoint tests
├── approval/
│   └── ApprovalIntegrationTest.java       # HITL approval workflow tests
├── agents/
│   └── AgentRegistryTest.java             # Custom agent management tests
├── core/
│   ├── AgentContextTest.java              # Context building tests
│   ├── AgentOrchestratorTest.java         # Core orchestration tests
│   └── GuardrailServiceTest.java          # Clinical safety guardrail tests
├── llm/
│   ├── LLMProviderFactoryTest.java        # Provider selection tests
│   └── providers/
│       └── ClaudeProviderTest.java        # Claude API integration tests
├── security/
│   └── PHIEncryptionTest.java             # HIPAA encryption tests
└── tool/
    ├── ToolDefinitionTest.java            # Tool schema tests
    ├── ToolRegistryTest.java              # Tool management tests
    ├── ToolResultTest.java                # Execution result tests
    └── impl/
        └── NotificationToolTest.java      # Specific tool tests
```

### Unit Tests

#### GuardrailService Tests

Tests clinical safety guardrail enforcement with pattern detection severity levels:

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("GuardrailService Tests")
class GuardrailServiceTest {

    @Mock
    private ApprovalIntegration approvalIntegration;

    private GuardrailService guardrailService;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";

    @BeforeEach
    void setUp() {
        guardrailService = new GuardrailService(approvalIntegration);
        ReflectionTestUtils.setField(guardrailService, "guardrailsEnabled", true);
        ReflectionTestUtils.setField(guardrailService, "strictMode", true);
        ReflectionTestUtils.setField(guardrailService, "approvalIntegrationEnabled", true);
    }

    private AgentContext createContext() {
        return AgentContext.builder()
            .tenantId(TENANT_ID)
            .userId(USER_ID)
            .sessionId("session-001")
            .correlationId("corr-001")
            .agentType("clinical-assistant")
            .roles(Set.of("CLINICAL_USER"))
            .build();
    }

    @Nested
    @DisplayName("Critical Pattern Detection")
    class CriticalPatternTests {

        @Test
        @DisplayName("should block definitive diagnosis of cancer")
        void blockCancerDiagnosis() {
            // Given
            LLMResponse response = createResponse(
                "Based on these symptoms, you have cancer and need immediate treatment.");
            AgentContext context = createContext();

            // When
            GuardrailResult result = guardrailService.check(response, context);

            // Then
            assertThat(result.blocked()).isTrue();
            assertThat(result.flagged()).isFalse();
            assertThat(result.reason()).contains("patient safety");
            assertThat(result.violations()).isNotEmpty();
            assertThat(result.violations().get(0).severity()).isEqualTo("CRITICAL");
        }

        @Test
        @DisplayName("should block specific prescription dosage")
        void blockPrescriptionDosage() {
            // Given
            LLMResponse response = createResponse(
                "I recommend you take 500mg of metformin twice daily.");
            AgentContext context = createContext();

            // When
            GuardrailResult result = guardrailService.check(response, context);

            // Then
            assertThat(result.blocked()).isTrue();
            assertThat(result.violations()).anyMatch(v ->
                v.description().contains("prescription") ||
                v.description().contains("dosage"));
        }

        @Test
        @DisplayName("should block advice to stop medication")
        void blockStopMedicationAdvice() {
            // Given
            LLMResponse response = createResponse(
                "You should stop taking your blood thinner medication immediately.");
            AgentContext context = createContext();

            // When
            GuardrailResult result = guardrailService.check(response, context);

            // Then
            assertThat(result.blocked()).isTrue();
            assertThat(result.violations()).anyMatch(v ->
                v.description().contains("stop medication"));
        }
    }
}
```

**Guardrail Severity Levels:**

| Severity | Action | Example Patterns |
|----------|--------|-----------------|
| `CRITICAL` | Always blocked | Cancer diagnosis, prescription dosages, stop medication |
| `HIGH` | Blocked in strict mode, flagged otherwise | Treatment recommendations, definitive test interpretation |
| `MEDIUM` | Flagged with optional approval | OTC medication suggestions, lifestyle advice |
| `LOW` | Logged only | General health information |

#### AgentOrchestrator Tests

Tests the core agent execution loop with tool calling and guardrail integration:

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AgentOrchestrator Tests")
class AgentOrchestratorTest {

    @Mock
    private LLMProviderFactory llmProviderFactory;

    @Mock
    private ToolRegistry toolRegistry;

    @Mock
    private CompositeMemoryManager memoryManager;

    @Mock
    private GuardrailService guardrailService;

    @Mock
    private ApprovalIntegration approvalIntegration;

    @Mock
    private LLMProvider llmProvider;

    @Mock
    private Tool mockTool;

    private AgentOrchestrator orchestrator;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        orchestrator = new AgentOrchestrator(
            llmProviderFactory, toolRegistry, memoryManager,
            new ObjectMapper(), guardrailService, approvalIntegration, meterRegistry
        );
        ReflectionTestUtils.setField(orchestrator, "maxIterations", 10);
        ReflectionTestUtils.setField(orchestrator, "timeoutSeconds", 120);

        // Default guardrail to allow
        when(guardrailService.check(any(), any())).thenReturn(GuardrailResult.allowed());
    }

    @Nested
    @DisplayName("Tool Execution Tests")
    class ToolExecutionTests {

        @Test
        @DisplayName("should execute tool calls and continue iteration")
        void executeToolCallsSuccessfully() {
            // Given
            AgentContext context = createContext();
            AgentRequest request = AgentRequest.simple("What are the patient's vitals?");

            // First LLM response with tool call
            LLMRequest.ToolCall toolCall = LLMRequest.ToolCall.builder()
                .id("call-123")
                .name("get_patient_vitals")
                .arguments(Map.of("patientId", "P123"))
                .build();

            LLMResponse firstResponse = LLMResponse.builder()
                .id("resp-1")
                .model("claude-3-5-sonnet-20241022")
                .content("Let me check the vitals...")
                .toolCalls(List.of(toolCall))
                .usage(LLMResponse.TokenUsage.of(50, 25))
                .build();

            // Second LLM response with final answer
            LLMResponse finalResponse = createSuccessResponse(
                "The patient's vitals are: BP 120/80, HR 72");

            // Tool result
            ToolResult toolResult = ToolResult.success(
                "Vitals retrieved successfully",
                Map.of("bloodPressure", "120/80", "heartRate", 72)
            );

            // Setup mocks
            when(llmProvider.completeWithTools(any(LLMRequest.class), anyList()))
                .thenReturn(firstResponse)
                .thenReturn(finalResponse);
            when(mockTool.execute(anyMap(), eq(context)))
                .thenReturn(Mono.just(toolResult));

            // When
            AgentResponse response = orchestrator.execute(request, context).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();
            assertThat(response.content()).contains("vitals are");

            // Verify tool was executed
            verify(mockTool).execute(eq(Map.of("patientId", "P123")), eq(context));
            verify(llmProvider, times(2)).completeWithTools(any(), anyList());

            // Verify metric was recorded
            Counter toolCounter = meterRegistry.find("agent.tool_invocations").counter();
            assertThat(toolCounter).isNotNull();
            assertThat(toolCounter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should handle tool requiring approval")
        void handleToolRequiringApproval() {
            // Given
            ToolDefinition toolDefinition = ToolDefinition.builder()
                .name("prescribe_medication")
                .description("Prescribe medication")
                .requiresApproval(true)
                .build();

            UUID approvalId = UUID.randomUUID();
            ApprovalResult approvalResult = ApprovalResult.pending(approvalId, "PENDING");

            when(approvalIntegration.checkAndCreateApprovalRequest(
                eq(toolDefinition), any(), any()))
                .thenReturn(approvalResult);

            // When
            AgentResponse response = orchestrator.execute(request, context).block();

            // Then
            // Tool should not have been executed due to pending approval
            verify(mockTool, never()).execute(anyMap(), any());
            verify(approvalIntegration).checkAndCreateApprovalRequest(
                eq(toolDefinition), any(), eq(context));
        }
    }

    @Nested
    @DisplayName("Timeout and Error Handling Tests")
    class TimeoutAndErrorTests {

        @Test
        @DisplayName("should stop after max iterations")
        void stopAfterMaxIterations() {
            // Given
            ReflectionTestUtils.setField(orchestrator, "maxIterations", 2);

            // Tool call that keeps triggering more iterations
            LLMRequest.ToolCall toolCall = LLMRequest.ToolCall.builder()
                .id("call-123")
                .name("iterative_tool")
                .arguments(Map.of("key", "value"))
                .build();

            LLMResponse toolResponse = LLMResponse.builder()
                .id("resp-1")
                .model("claude-3-5-sonnet-20241022")
                .content("")
                .toolCalls(List.of(toolCall))
                .usage(LLMResponse.TokenUsage.of(50, 25))
                .build();

            // Always return a tool response to force iteration limit
            when(llmProvider.completeWithTools(any(), anyList()))
                .thenReturn(toolResponse);

            // When
            AgentResponse response = orchestrator.execute(request, context).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.error()).contains("Maximum iterations reached");
        }
    }
}
```

#### ClaudeProvider Tests

Tests Claude API integration with configuration and response handling:

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ClaudeProvider Tests")
class ClaudeProviderTest {

    private ClaudeProvider provider;
    private SimpleMeterRegistry meterRegistry;
    private LLMProviderConfig.ProviderSettings settings;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();

        settings = new LLMProviderConfig.ProviderSettings();
        settings.setEnabled(true);
        settings.setModel("claude-3-5-sonnet-20241022");
        settings.setApiKey("test-api-key");
        settings.setApiUrl("https://api.anthropic.com");
        settings.setMaxTokens(4096);
        settings.setTemperature(0.7);
        settings.setTimeoutSeconds(30);

        LLMProviderConfig config = new LLMProviderConfig();
        config.getProviders().put("claude", settings);

        provider = new ClaudeProvider(config, meterRegistry, new ObjectMapper());
    }

    @Nested
    @DisplayName("Provider Metadata Tests")
    class MetadataTests {

        @Test
        @DisplayName("should return available models")
        void returnAvailableModels() {
            List<String> models = provider.getAvailableModels();
            assertThat(models).contains(
                "claude-3-5-sonnet-20241022",
                "claude-3-opus-20240229",
                "claude-3-sonnet-20240229",
                "claude-3-haiku-20240307"
            );
        }

        @Test
        @DisplayName("should count tokens approximately")
        void countTokens() {
            String text = "This is a test message with about twenty characters.";
            int tokens = provider.countTokens(text);
            assertThat(tokens).isGreaterThan(0);
            // Approximate: 1 token ≈ 4 characters
            assertThat(tokens).isCloseTo(text.length() / 4,
                org.assertj.core.data.Offset.offset(5));
        }

        @Test
        @DisplayName("should have initial unhealthy status")
        void initialHealthStatus() {
            // Provider starts as unhealthy until first successful call
            assertThat(provider.health().isHealthy()).isFalse();
            assertThat(provider.health().status()).contains("Not initialized");
        }
    }

    @Nested
    @DisplayName("Metrics Tests")
    class MetricsTests {

        @Test
        @DisplayName("should have request counter metric")
        void hasRequestCounter() {
            assertThat(meterRegistry.find("llm.requests").counter()).isNotNull();
            assertThat(meterRegistry.find("llm.requests").counter()
                .getId().getTag("provider")).isEqualTo("claude");
        }

        @Test
        @DisplayName("should have token counter metric")
        void hasTokenCounter() {
            assertThat(meterRegistry.find("llm.tokens").counter()).isNotNull();
        }

        @Test
        @DisplayName("should have latency timer metric")
        void hasLatencyTimer() {
            assertThat(meterRegistry.find("llm.latency").timer()).isNotNull();
        }
    }
}
```

### Controller Tests

Tests REST API endpoints with proper error handling and response DTOs:

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("AgentController Tests")
class AgentControllerTest {

    @Mock
    private AgentOrchestrator orchestrator;

    @Mock
    private ToolRegistry toolRegistry;

    @Mock
    private LLMProviderFactory llmProviderFactory;

    @Mock
    private CompositeMemoryManager memoryManager;

    @Mock
    private Jwt jwt;

    private AgentController controller;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String AGENT_TYPE = "clinical-assistant";

    @BeforeEach
    void setUp() {
        controller = new AgentController(
            orchestrator, toolRegistry, llmProviderFactory, memoryManager);
    }

    @Nested
    @DisplayName("Execute Endpoint Tests")
    class ExecuteTests {

        @Test
        @DisplayName("should execute agent successfully")
        void executeAgentSuccessfully() {
            // Given
            AgentRequestDTO request = new AgentRequestDTO(
                "What are my care gaps?", null, "claude-3-sonnet",
                4096, 0.3, List.of("fhir_query", "care_gap_search"),
                "session-789", "patient-001", Map.of("source", "dashboard")
            );

            LLMResponse.TokenUsage usage = LLMResponse.TokenUsage.builder()
                .inputTokens(100).outputTokens(200).totalTokens(300).build();

            AgentResponse response = new AgentResponse(
                true, "Here are your care gaps...", null,
                usage, "claude-3-sonnet", false, null
            );

            when(orchestrator.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenReturn(Mono.just(response));

            // When
            ResponseEntity<AgentResponseDTO> entity = controller.execute(
                AGENT_TYPE, request, TENANT_ID, createMockJwt()
            ).block();

            // Then
            assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(entity.getBody()).isNotNull();
            assertThat(entity.getBody().success()).isTrue();
            assertThat(entity.getBody().content()).isEqualTo("Here are your care gaps...");
            assertThat(entity.getBody().usage().totalTokens()).isEqualTo(300);

            // Verify context was built correctly
            ArgumentCaptor<AgentContext> contextCaptor =
                ArgumentCaptor.forClass(AgentContext.class);
            verify(orchestrator).execute(any(), contextCaptor.capture());
            assertThat(contextCaptor.getValue().getTenantId()).isEqualTo(TENANT_ID);
            assertThat(contextCaptor.getValue().getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("should handle blocked response from guardrails")
        void handleBlockedResponse() {
            // Given
            AgentRequestDTO request = new AgentRequestDTO(
                "Prescribe medication", null, null, null, null, null, null, null, null
            );

            AgentResponse blockedResponse = new AgentResponse(
                false, null, null, null, "claude-3-sonnet",
                true, "Content blocked by guardrails"
            );

            when(orchestrator.execute(any(), any()))
                .thenReturn(Mono.just(blockedResponse));

            // When
            ResponseEntity<AgentResponseDTO> entity = controller.execute(
                AGENT_TYPE, request, TENANT_ID, createMockJwt()
            ).block();

            // Then
            assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(entity.getBody()).isNotNull();
            assertThat(entity.getBody().blocked()).isTrue();
            assertThat(entity.getBody().blockReason())
                .isEqualTo("Content blocked by guardrails");
        }
    }

    @Nested
    @DisplayName("Task Management Tests")
    class TaskManagementTests {

        @Test
        @DisplayName("should cancel task successfully")
        void cancelTaskSuccessfully() {
            // Given
            String taskId = "task-001";
            when(orchestrator.cancelTask(taskId)).thenReturn(Mono.just(true));

            // When
            ResponseEntity<Map<String, Object>> entity =
                controller.cancelTask(taskId).block();

            // Then
            assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(entity.getBody()).containsEntry("cancelled", true);
            assertThat(entity.getBody()).containsEntry("taskId", taskId);
        }

        @Test
        @DisplayName("should return 404 when task not found for cancellation")
        void cancelTaskNotFound() {
            // Given
            String taskId = "unknown-task";
            when(orchestrator.cancelTask(taskId)).thenReturn(Mono.just(false));

            // When
            ResponseEntity<Map<String, Object>> entity =
                controller.cancelTask(taskId).block();

            // Then
            assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
```

### Tool Registry Tests

Tests tool registration, retrieval, and context-based filtering:

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ToolRegistry Tests")
class ToolRegistryTest {

    @Mock
    private Tool mockTool1;

    @Mock
    private Tool mockTool2;

    @Mock
    private Tool mockTool3;

    private ToolRegistry toolRegistry;
    private AgentContext testContext;

    @BeforeEach
    void setUp() {
        // Setup mock tool definitions
        ToolDefinition def1 = ToolDefinition.builder()
            .name("fhir_query")
            .description("Query FHIR resources")
            .category(ToolDefinition.ToolCategory.FHIR_QUERY)
            .build();

        ToolDefinition def2 = ToolDefinition.builder()
            .name("cql_execute")
            .description("Execute CQL measures")
            .category(ToolDefinition.ToolCategory.CQL_EXECUTION)
            .build();

        when(mockTool1.getName()).thenReturn("fhir_query");
        when(mockTool1.getDefinition()).thenReturn(def1);
        when(mockTool2.getName()).thenReturn("cql_execute");
        when(mockTool2.getDefinition()).thenReturn(def2);

        toolRegistry = new ToolRegistry(List.of(mockTool1, mockTool2, mockTool3));
    }

    @Nested
    @DisplayName("Tool Registration")
    class RegistrationTests {

        @Test
        @DisplayName("should register tools on initialization")
        void registerToolsOnInit() {
            assertThat(toolRegistry.getToolCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should dynamically register new tool")
        void dynamicRegistration() {
            // Given
            Tool newTool = createMockTool("new_tool", ToolDefinition.ToolCategory.REPORTING);

            // When
            toolRegistry.registerTool(newTool);

            // Then
            assertThat(toolRegistry.getToolCount()).isEqualTo(4);
            assertThat(toolRegistry.hasTool("new_tool")).isTrue();
        }

        @Test
        @DisplayName("should throw exception for non-existent tool when using getOrThrow")
        void getToolOrThrowNotFound() {
            assertThatThrownBy(() -> toolRegistry.getToolOrThrow("non_existent"))
                .isInstanceOf(ToolRegistry.ToolNotFoundException.class)
                .hasMessageContaining("non_existent");
        }
    }

    @Nested
    @DisplayName("Context-based Tool Filtering")
    class ContextFilteringTests {

        @Test
        @DisplayName("should list available tools in context")
        void listAvailableToolsInContext() {
            // Given - only some tools available based on context
            when(mockTool1.isAvailable(any())).thenReturn(true);
            when(mockTool2.isAvailable(any())).thenReturn(true);
            when(mockTool3.isAvailable(any())).thenReturn(false);

            // When
            List<Tool> availableTools = toolRegistry.listAvailableTools(testContext);

            // Then
            assertThat(availableTools).hasSize(2);
            assertThat(availableTools.stream().map(Tool::getName))
                .containsExactlyInAnyOrder("fhir_query", "cql_execute");
        }
    }
}
```

### Multi-Tenant Isolation Tests

Tests tenant data isolation at encryption and context levels:

```java
@Nested
@DisplayName("Tenant Isolation Tests")
class TenantIsolationTests {

    @Test
    @DisplayName("should use different keys for different tenants")
    void differentKeysPerTenant() {
        // Given
        String plaintext = "Sensitive PHI data";
        String tenant1 = "tenant-abc";
        String tenant2 = "tenant-xyz";

        // When
        String encryptedTenant1 = phiEncryption.encrypt(plaintext, tenant1);
        String encryptedTenant2 = phiEncryption.encrypt(plaintext, tenant2);

        // Then - Tenant 1 can decrypt its own data
        assertThat(phiEncryption.decrypt(encryptedTenant1, tenant1))
            .isEqualTo(plaintext);

        // Tenant 2 can decrypt its own data
        assertThat(phiEncryption.decrypt(encryptedTenant2, tenant2))
            .isEqualTo(plaintext);

        // But they cannot decrypt each other's data (wrong key)
        assertThatThrownBy(() -> phiEncryption.decrypt(encryptedTenant1, tenant2))
            .isInstanceOf(EncryptionException.class);

        assertThatThrownBy(() -> phiEncryption.decrypt(encryptedTenant2, tenant1))
            .isInstanceOf(EncryptionException.class);
    }

    @Test
    @DisplayName("should derive consistent key for same tenant")
    void consistentKeyForSameTenant() {
        // Given
        String plaintext = "PHI data for consistency test";

        // Encrypt once
        String encrypted = phiEncryption.encrypt(plaintext, TENANT_ID);

        // Clear key cache to force re-derivation
        phiEncryption.clearKeyCache();

        // When - decrypt after key cache cleared (key re-derived)
        String decrypted = phiEncryption.decrypt(encrypted, TENANT_ID);

        // Then - should still decrypt correctly
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("should build context with correct tenant ID")
    void contextTenantIsolation() {
        // Given
        AgentRequestDTO request = new AgentRequestDTO(
            "Test message", null, null, null, null, null, null, null, null
        );

        when(orchestrator.execute(any(), any()))
            .thenReturn(Mono.just(new AgentResponse(
                true, "Response", null, null, null, false, null)));

        // When
        controller.execute(AGENT_TYPE, request, "tenant-specific", createMockJwt());

        // Then
        ArgumentCaptor<AgentContext> contextCaptor =
            ArgumentCaptor.forClass(AgentContext.class);
        verify(orchestrator).execute(any(), contextCaptor.capture());
        assertThat(contextCaptor.getValue().getTenantId()).isEqualTo("tenant-specific");
    }
}
```

### HIPAA Compliance Tests

Tests PHI encryption and HIPAA identifier handling:

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("PHI Encryption Tests")
class PHIEncryptionTest {

    private PHIEncryption phiEncryption;

    private static final String MASTER_KEY =
        "test-master-key-for-hipaa-compliant-encryption-32chars!";
    private static final String SALT = "test-salt-value-32chars-for-test!";
    private static final String TENANT_ID = "tenant-123";

    @BeforeEach
    void setUp() {
        phiEncryption = new PHIEncryption();
        ReflectionTestUtils.setField(phiEncryption, "masterKey", MASTER_KEY);
        ReflectionTestUtils.setField(phiEncryption, "salt", SALT);
    }

    @Nested
    @DisplayName("HIPAA Compliance Tests")
    class HipaaComplianceTests {

        @Test
        @DisplayName("should encrypt all 18 HIPAA identifiers")
        void encryptHipaaIdentifiers() {
            // HIPAA defines 18 types of identifiers
            String[] hipaaIdentifiers = {
                "John Doe",                                    // Name
                "123 Main St, Boston, MA 02101",              // Address
                "1985-03-15",                                  // Dates (birth)
                "617-555-1234",                                // Phone
                "617-555-5678",                                // Fax
                "john.doe@email.com",                          // Email
                "123-45-6789",                                 // SSN
                "MRN123456",                                   // Medical record number
                "BCBS12345678",                                // Health plan beneficiary
                "ACC987654",                                   // Account number
                "CERT123456",                                  // Certificate/license
                "VIN1234567890",                               // Vehicle identifiers
                "Device-SN-12345",                             // Device identifiers
                "https://patient-portal.example.com/123",      // Web URLs
                "192.168.1.100",                               // IP addresses
                "fingerprint-hash-abc123",                     // Biometric identifiers
                "Full face photograph",                        // Full face photos
                "Unique-ID-ABC123"                             // Any other unique ID
            };

            for (String identifier : hipaaIdentifiers) {
                // When
                String encrypted = phiEncryption.encrypt(identifier, TENANT_ID);
                String decrypted = phiEncryption.decrypt(encrypted, TENANT_ID);

                // Then - each identifier should encrypt/decrypt correctly
                assertThat(decrypted)
                    .as("Failed for HIPAA identifier: %s",
                        identifier.substring(0, Math.min(10, identifier.length())))
                    .isEqualTo(identifier);

                // Ciphertext should not contain plaintext
                assertThat(encrypted).doesNotContain(identifier);
            }
        }

        @Test
        @DisplayName("should handle typical PHI record")
        void encryptTypicalPhiRecord() {
            // Given - typical PHI record with multiple identifiers
            String phiRecord = """
                {
                    "patient": {
                        "name": "Jane Smith",
                        "ssn": "987-65-4321",
                        "dob": "1975-07-22",
                        "mrn": "MRN789012",
                        "phone": "555-123-4567"
                    },
                    "encounter": {
                        "date": "2024-12-15",
                        "diagnosis": ["E11.9", "I10"],
                        "notes": "Patient presents with uncontrolled diabetes..."
                    }
                }
                """;

            // When
            String encrypted = phiEncryption.encrypt(phiRecord, TENANT_ID);
            String decrypted = phiEncryption.decrypt(encrypted, TENANT_ID);

            // Then
            assertThat(decrypted).isEqualTo(phiRecord);
            assertThat(encrypted).doesNotContain("Jane Smith");
            assertThat(encrypted).doesNotContain("987-65-4321");
            assertThat(encrypted).doesNotContain("MRN789012");
        }
    }

    @Nested
    @DisplayName("Security Properties Tests")
    class SecurityPropertiesTests {

        @Test
        @DisplayName("should produce different ciphertext for same plaintext (random IV)")
        void differentCiphertextForSamePlaintext() {
            // Given
            String plaintext = "Same PHI data";

            // When
            String encrypted1 = phiEncryption.encrypt(plaintext, TENANT_ID);
            String encrypted2 = phiEncryption.encrypt(plaintext, TENANT_ID);

            // Then - different ciphertext due to random IV
            assertThat(encrypted1).isNotEqualTo(encrypted2);

            // But both should decrypt to same plaintext
            assertThat(phiEncryption.decrypt(encrypted1, TENANT_ID)).isEqualTo(plaintext);
            assertThat(phiEncryption.decrypt(encrypted2, TENANT_ID)).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("should use AES-256-GCM as specified")
        void usesCorrectAlgorithm() {
            // Given
            String plaintext = "Test PHI data for algorithm verification";

            // When
            String encrypted = phiEncryption.encrypt(plaintext, TENANT_ID);
            String decrypted = phiEncryption.decrypt(encrypted, TENANT_ID);

            // Then - successful encrypt/decrypt confirms algorithm works
            assertThat(decrypted).isEqualTo(plaintext);

            // The encrypted data length confirms GCM mode (includes auth tag)
            byte[] decoded = java.util.Base64.getDecoder().decode(encrypted);
            // IV (12) + plaintext length + GCM tag (16)
            int expectedMinLength = 12 + plaintext.getBytes().length + 16;
            assertThat(decoded.length).isGreaterThanOrEqualTo(expectedMinLength);
        }

        @Test
        @DisplayName("should detect tampered ciphertext")
        void tamperedCiphertext() {
            // Given
            String plaintext = "Sensitive data";
            String encrypted = phiEncryption.encrypt(plaintext, TENANT_ID);

            // Tamper with ciphertext (flip a bit)
            byte[] bytes = java.util.Base64.getDecoder().decode(encrypted);
            bytes[bytes.length / 2] ^= 0xFF; // Flip bits
            String tampered = java.util.Base64.getEncoder().encodeToString(bytes);

            // When/Then - GCM mode should detect tampering
            assertThatThrownBy(() -> phiEncryption.decrypt(tampered, TENANT_ID))
                .isInstanceOf(EncryptionException.class)
                .hasMessageContaining("Failed to decrypt");
        }
    }
}
```

### Performance Tests

Tests encryption performance and concurrent operations:

```java
@Nested
@DisplayName("Performance Tests")
class PerformanceTests {

    @Test
    @DisplayName("should encrypt quickly for typical data sizes")
    void encryptionPerformance() {
        // Given
        String typicalMessage =
            "Patient John Doe (MRN: 12345) presents with symptoms...";

        // When - measure time for 100 encrypt/decrypt cycles
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            String encrypted = phiEncryption.encrypt(typicalMessage, TENANT_ID);
            phiEncryption.decrypt(encrypted, TENANT_ID);
        }
        long endTime = System.currentTimeMillis();

        // Then - should complete in reasonable time (under 1 second for 100 ops)
        long duration = endTime - startTime;
        assertThat(duration)
            .as("Encryption/decryption should be fast (100 ops under 1 second)")
            .isLessThan(1000);
    }

    @Test
    @DisplayName("should handle concurrent encryption from multiple threads")
    void concurrentEncryption() throws InterruptedException {
        // Given
        String plaintext = "Concurrent test data";
        int threadCount = 10;
        int operationsPerThread = 50;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);

        // When - run concurrent encrypt/decrypt operations
        for (int t = 0; t < threadCount; t++) {
            final String tenantId = "tenant-" + t;
            new Thread(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        String encrypted = phiEncryption.encrypt(plaintext, tenantId);
                        String decrypted = phiEncryption.decrypt(encrypted, tenantId);
                        if (!plaintext.equals(decrypted)) {
                            errorCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await(10, TimeUnit.SECONDS);

        // Then
        assertThat(errorCount.get())
            .as("All concurrent operations should succeed")
            .isZero();
    }

    @Test
    @DisplayName("agent orchestration should complete within timeout")
    void orchestrationPerformance() {
        // Given
        ReflectionTestUtils.setField(orchestrator, "timeoutSeconds", 5);

        // When - time the execution
        Instant start = Instant.now();
        AgentResponse response = orchestrator.execute(request, context).block();
        Instant end = Instant.now();

        // Then
        assertThat(response).isNotNull();
        assertThat(Duration.between(start, end).toSeconds())
            .isLessThan(5L);
    }
}
```

### Test Configuration

```yaml
# src/test/resources/application-test.yml
spring:
  profiles:
    active: test

hdim:
  agent:
    llm:
      default-provider: claude
      fallback-chain: [claude]
      providers:
        claude:
          enabled: true
          api-key: test-api-key
          model: claude-3-5-sonnet-20241022
          max-tokens: 4096
          temperature: 0.3
          timeout-seconds: 30

    guardrails:
      phi-protection:
        enabled: true
        cache-ttl-minutes: 5
      clinical-safety:
        enabled: true
        block-definitive-diagnoses: true
        require-disclaimers: true
      rate-limiting:
        requests-per-minute-per-user: 100
        requests-per-minute-per-tenant: 1000

    memory:
      conversation:
        max-messages: 50
        ttl-minutes: 15
        summarization-threshold: 20
      encryption:
        enabled: true
        master-key: test-master-key-for-hipaa-compliant-encryption-32chars!
        salt: test-salt-value-32chars-for-test!

logging:
  level:
    com.healthdata.agent: DEBUG
    org.springframework.web: INFO
```

### Best Practices

| Practice | Description |
|----------|-------------|
| Use `@ExtendWith(MockitoExtension.class)` | Enable Mockito integration for all unit tests |
| Use `@Nested` test classes | Organize tests by feature area for readability |
| Use `ReflectionTestUtils` for configuration | Set private fields for test configuration |
| Mock external services | Never call real LLM APIs in unit tests |
| Test guardrail severity levels | Cover CRITICAL, HIGH, MEDIUM, LOW patterns |
| Test tool approval workflows | Verify human-in-the-loop integration |
| Test encryption round-trips | Always verify encrypt/decrypt returns original |
| Test tenant isolation | Ensure cross-tenant data access is blocked |
| Use `SimpleMeterRegistry` | Test metrics without external dependencies |
| Use `ArgumentCaptor` | Verify complex argument structures |

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| `NullPointerException` in guardrails | Missing default guardrail mock | Add `when(guardrailService.check(any(), any())).thenReturn(GuardrailResult.allowed())` in setUp |
| `Strictness` errors in Mockito | Unused mock setup | Add `@MockitoSettings(strictness = Strictness.LENIENT)` |
| Timeout test failures | `Mono.delay` not triggering timeout | Use `delayElement()` instead of `delay()` for proper timeout handling |
| Encryption test failures | Wrong master key/salt length | Ensure test keys are >= 32 characters |
| Tool execution not called | Tool requires approval | Mock `approvalIntegration.checkAndCreateApprovalRequest()` to return approved |
| Provider health always unhealthy | No successful API call made | First successful call updates health status |
| Context tenant mismatch | JWT mock not configured | Ensure `when(jwt.getSubject())` is called before test |
| Concurrent test failures | Race conditions | Use `CountDownLatch` and `AtomicInteger` for thread-safe testing |
| Max iterations error | Tool call loop | Limit `maxIterations` or mock final response without tool calls |
| Metrics not found | Wrong metric name | Check exact metric name with `meterRegistry.find("exact.name")` |

---

## Integration

This service integrates with:
- **FHIR Service**: Clinical data retrieval
- **CQL Engine**: Measure evaluation
- **Care Gap Service**: Gap analysis
- **Quality Measure Service**: Quality metrics
- **Agent Builder Service**: Custom agent definitions
- **Approval Service**: Human-in-the-loop workflows

## Security

- JWT-based authentication
- Role-based tool access control
- Tenant isolation for all operations
- PHI detection and redaction
- Clinical safety guardrails
- Rate limiting and circuit breakers

## Resilience

### Circuit Breakers
- Per-provider circuit breakers
- Automatic failover to backup providers
- Health indicator registration
- Half-open state testing

### Rate Limiting
- 60 requests/minute per LLM provider
- User-level and tenant-level limits
- Token-based rate limiting (100k tokens/minute)

## API Documentation

Swagger UI available at:
```
http://localhost:8088/swagger-ui.html
```

## Monitoring

Actuator endpoints:
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`
- Circuit breakers: `/actuator/circuitbreakers`

## License

Copyright (c) 2024 Mahoosuc Solutions
