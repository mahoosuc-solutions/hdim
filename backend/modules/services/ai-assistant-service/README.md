# AI Assistant Service

AI-powered clinical assistant service using Claude API for natural language clinical queries, patient summaries, and care gap analysis.

## Overview

The AI Assistant Service provides conversational AI capabilities for clinical workflows. It uses Anthropic's Claude API to answer clinical questions, generate patient summaries, analyze care gaps, and provide decision support. All interactions are HIPAA-compliant with proper safeguards and audit logging.

## Key Features

### Natural Language Queries
- Answer clinical questions in natural language
- Context-aware responses with clinical data
- Query type validation and routing
- Conversation context preservation

### Patient Summary Generation
- AI-generated patient summaries from FHIR data
- Key clinical findings extraction
- Active problems and medication lists
- Recent encounters and results

### Care Gap Analysis
- Intelligent care gap prioritization
- Actionable recommendations for gap closure
- Risk-based gap ranking
- Intervention suggestions

### Clinical Decision Support
- Quality measure interpretation
- Treatment guideline recommendations
- Medication interaction awareness
- Evidence-based practice suggestions

### Safety and Compliance
- Query type allowlisting
- Rate limiting per user/tenant
- Response validation and filtering
- Comprehensive audit trail
- No storage of patient data in AI logs

## Technology Stack

- **Spring Boot 3.x**: Core framework
- **Claude API (Anthropic)**: AI language model
- **PostgreSQL**: Audit and session storage
- **Redis**: Response caching and rate limiting
- **Apache Kafka**: Event streaming for audit
- **Resilience4j**: Circuit breaker and rate limiting

## API Endpoints

### Chat Interface
```
POST /api/v1/ai/chat
     - Send natural language query to AI assistant
     - Request body: ChatRequest (query, queryType, context)
     - Returns: ChatResponse with AI-generated answer
```

### Patient Summaries
```
POST /api/v1/ai/patient-summary/{patientId}
     - Generate AI summary for a patient
     - Request body: Patient FHIR data (JSON)
     - Returns: ChatResponse with summary
```

### Care Gap Analysis
```
POST /api/v1/ai/care-gaps/analyze
     - Analyze care gaps with AI recommendations
     - Request body: Care gap data (JSON)
     - Returns: ChatResponse with analysis
```

### Clinical Queries
```
GET /api/v1/ai/query?query={query}&context={context}
    - Answer a clinical question
    - Query params: query, context (optional)
    - Returns: ChatResponse with answer
```

### Status and Health
```
GET /api/v1/ai/status
    - Get AI assistant capabilities and configuration

GET /api/v1/ai/health
    - Health check with AI availability status
```

## Configuration

### Claude API
```yaml
claude:
  enabled: true
  api-key: ${ANTHROPIC_API_KEY}
  api-url: https://api.anthropic.com/v1
  model: claude-3-5-sonnet-20241022
  max-tokens: 4096
  temperature: 0.3
  timeout-seconds: 60
  max-retries: 3
```

### Rate Limiting
```yaml
claude:
  rate-limit-per-minute: 60
  caching-enabled: true
  cache-ttl-seconds: 300
```

### Query Types
Configurable allowed query types:
- CLINICAL_QUESTION
- PATIENT_SUMMARY
- CARE_GAP_ANALYSIS
- QUALITY_MEASURE
- MEDICATION_REVIEW
- DIAGNOSIS_SUPPORT

## Running Locally

### Prerequisites
- Java 21
- PostgreSQL 14+
- Redis 6+
- Kafka 3+
- Anthropic API key

### Environment Variables
```bash
export ANTHROPIC_API_KEY=sk-ant-...
export CLAUDE_ENABLED=true
export CLAUDE_MODEL=claude-3-5-sonnet-20241022
```

### Build
```bash
./gradlew :modules:services:ai-assistant-service:build
```

### Run
```bash
./gradlew :modules:services:ai-assistant-service:bootRun
```

### Run Tests
```bash
./gradlew :modules:services:ai-assistant-service:test
```

---

## Testing

### Overview

The AI Assistant Service has a comprehensive test suite covering AI API integration, controller endpoints, configuration validation, DTO behavior, and HIPAA compliance. The test suite ensures reliable AI service behavior, proper error handling, and tenant-isolated caching.

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:ai-assistant-service:test

# Run specific test class
./gradlew :modules:services:ai-assistant-service:test --tests "ClaudeServiceTest"

# Run tests by pattern
./gradlew :modules:services:ai-assistant-service:test --tests "*ControllerTest"

# Run with verbose output
./gradlew :modules:services:ai-assistant-service:test --info

# Run with coverage report
./gradlew :modules:services:ai-assistant-service:test jacocoTestReport

# Run single test method
./gradlew :modules:services:ai-assistant-service:test --tests "ClaudeServiceTest.shouldReturnChatResponse"

# Run integration tests only
./gradlew :modules:services:ai-assistant-service:test --tests "*IntegrationTest"
```

### Test Coverage Summary

| Test Class | Tests | Coverage Focus |
|------------|-------|----------------|
| `ClaudeServiceTest` | 10+ | Claude API integration, response parsing, token usage, fallbacks |
| `AiAssistantControllerTest` | 15+ | REST API endpoints, query type validation, disabled state handling |
| `AiAssistantControllerUnitTest` | 5+ | Controller unit tests, null authentication handling |
| `ClaudeConfigTest` | 15+ | Configuration defaults, query type allowlisting, API key validation |
| `ChatRequestTest` | 20+ | Cache key generation, bean validation, tenant isolation |
| `ChatResponseTest` | 2+ | Response DTO defaults, builder pattern |
| `ChatMessageTest` | 2+ | Message factory methods, builder pattern |

**Total: 7 test classes, 70+ test methods**

### Test Organization

```
src/test/java/com/healthdata/aiassistant/
├── config/
│   └── ClaudeConfigTest.java              # Configuration and query type validation
├── controller/
│   ├── AiAssistantControllerTest.java     # Controller integration tests
│   └── AiAssistantControllerUnitTest.java # Controller unit tests
├── dto/
│   ├── ChatRequestTest.java               # Request DTO validation and caching
│   ├── ChatResponseTest.java              # Response DTO builder tests
│   └── ChatMessageTest.java               # Message factory tests
└── service/
    └── ClaudeServiceTest.java             # AI service integration tests
```

---

### Unit Tests (AI Service Layer)

The `ClaudeServiceTest` validates Claude API integration, response parsing, error handling, and circuit breaker behavior.

#### Claude API Response Parsing

```java
@DisplayName("ClaudeService")
class ClaudeServiceTest {

    @Test
    @DisplayName("Should return chat response when Claude returns text")
    void shouldReturnChatResponse() {
        // Given: Mock Claude API response with text content
        String responseJson = """
            {"content":[{"type":"text","text":"Hello"}],
             "usage":{"input_tokens":12,"output_tokens":34}}
            """;
        ClaudeService service = buildService(jsonExchange(responseJson));

        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("Explain care gaps.")
            .tenantId("tenant-1")
            .messages(List.of(ChatMessage.user("previous message")))
            .build();

        // When: Send chat request
        ChatResponse response = service.chatCached(request);

        // Then: Verify response parsing
        assertThat(response.getResponse()).isEqualTo("Hello");
        assertThat(response.getQueryType()).isEqualTo("care_gaps");
        assertThat(response.getModel()).isEqualTo("test-model");
        assertThat(response.getInputTokens()).isEqualTo(12);
        assertThat(response.getOutputTokens()).isEqualTo(34);
        assertThat(response.isError()).isFalse();
    }

    @Test
    @DisplayName("Should default token usage to zero when usage is missing")
    void shouldHandleMissingUsage() {
        // Given: Response without usage field
        String responseJson = """
            {"content":[{"type":"text","text":"Hello"}]}
            """;
        ClaudeService service = buildService(jsonExchange(responseJson));

        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("Explain care gaps.")
            .tenantId("tenant-1")
            .build();

        // When: Process response
        ChatResponse response = service.chat(request);

        // Then: Token usage defaults to zero
        assertThat(response.getInputTokens()).isZero();
        assertThat(response.getOutputTokens()).isZero();
    }
}
```

#### Error Handling and Fallbacks

```java
@Test
@DisplayName("Should return error response when Claude response is empty")
void shouldHandleEmptyResponse() {
    // Given: Empty content array from Claude
    ClaudeService service = buildService(jsonExchange("{\"content\":[]}"));

    ChatRequest request = ChatRequest.builder()
        .queryType("care_gaps")
        .query("Any output?")
        .tenantId("tenant-1")
        .build();

    // When: Process request
    ChatResponse response = service.chat(request);

    // Then: Error response returned gracefully
    assertThat(response.isError()).isTrue();
    assertThat(response.getErrorMessage()).contains("Empty response");
    assertThat(response.getResponse()).contains("Unable to process request");
}

@Test
@DisplayName("Should return error response when Claude call fails")
void shouldHandleClaudeErrors() {
    // Given: WebClient throws exception
    ClaudeService service = buildService(request -> Mono.error(new RuntimeException("boom")));

    ChatRequest request = ChatRequest.builder()
        .queryType("care_gaps")
        .query("Any output?")
        .tenantId("tenant-1")
        .build();

    // When: API call fails
    ChatResponse response = service.chat(request);

    // Then: Graceful error response
    assertThat(response.isError()).isTrue();
    assertThat(response.getErrorMessage()).contains("AI service unavailable");
}

@Test
@DisplayName("Should build fallback response when circuit breaker triggers")
void shouldBuildFallbackResponse() {
    // Given: Circuit breaker open
    ClaudeService service = buildService(jsonExchange("{\"content\":[]}"));
    ChatRequest request = ChatRequest.builder()
        .queryType("care_gaps")
        .query("Any output?")
        .tenantId("tenant-1")
        .build();

    // When: Fallback triggered
    ChatResponse response = service.chatFallback(request, new RuntimeException("offline"));

    // Then: Fallback response indicates temporary unavailability
    assertThat(response.isError()).isTrue();
    assertThat(response.getModel()).isEqualTo("fallback");
    assertThat(response.getQueryType()).isEqualTo("care_gaps");
    assertThat(response.getErrorMessage()).contains("Service temporarily unavailable");
}
```

#### Prompt Building Tests

```java
@Test
@DisplayName("Should build prompts for patient summary, care gaps, and clinical query")
void shouldBuildPrompts() {
    // Given: Spied service to capture requests
    ClaudeService service = spy(baseService());
    doReturn(ChatResponse.builder().build()).when(service).chat(any(ChatRequest.class));

    // When: Generate different content types
    service.generatePatientSummary("patient-123", "summary data");
    service.analyzeCareGaps("gap data");
    service.answerClinicalQuery("What is diabetes?", "context data");

    // Then: Verify correct query types and content
    ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
    verify(service, times(3)).chat(captor.capture());

    List<ChatRequest> requests = captor.getAllValues();

    // Patient summary request
    assertThat(requests.get(0).getQueryType()).isEqualTo("patient_summary");
    assertThat(requests.get(0).getQuery()).contains("patient-123").contains("summary data");

    // Care gaps request
    assertThat(requests.get(1).getQueryType()).isEqualTo("care_gaps");
    assertThat(requests.get(1).getQuery()).contains("gap data");

    // Clinical query request
    assertThat(requests.get(2).getQueryType()).isEqualTo("quality_measures");
    assertThat(requests.get(2).getQuery()).contains("What is diabetes?").contains("context data");
}
```

#### WebClient Mock Utilities

```java
/**
 * Build ClaudeService with mocked WebClient for testing.
 * Uses ExchangeFunction pattern to control API responses.
 */
private static ClaudeService buildService(ExchangeFunction exchangeFunction) {
    ClaudeService service = baseService();
    WebClient client = WebClient.builder().exchangeFunction(exchangeFunction).build();
    ReflectionTestUtils.setField(service, "webClient", client);
    return service;
}

private static ClaudeService baseService() {
    ClaudeConfig config = new ClaudeConfig();
    config.setApiUrl("http://localhost");
    config.setApiKey("test-key");
    config.setModel("test-model");
    config.setMaxTokens(256);
    config.setTemperature(0.2);
    config.setTimeoutSeconds(1);
    return new ClaudeService(config, new ObjectMapper());
}

/**
 * Create mock HTTP response with JSON body.
 * Uses DefaultDataBufferFactory for reactive response body.
 */
private static ExchangeFunction jsonExchange(String json) {
    return request -> Mono.just(
        ClientResponse.create(HttpStatus.OK)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(Flux.just(new DefaultDataBufferFactory()
                .wrap(json.getBytes(StandardCharsets.UTF_8))))
            .build()
    );
}
```

---

### Controller Tests

#### Controller Integration Tests (WebMvcTest)

The `AiAssistantControllerTest` validates all REST API endpoints with MockMvc.

```java
@WebMvcTest(AiAssistantController.class)
@ContextConfiguration(classes = {AiAssistantController.class, ClaudeConfig.class})
@DisplayName("AiAssistantController Tests")
class AiAssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClaudeService claudeService;

    @Autowired
    private ClaudeConfig claudeConfig;

    @BeforeEach
    void setUp() {
        // Reset config to defaults before each test
        claudeConfig.setEnabled(false);
        claudeConfig.setModel("claude-3-5-sonnet-20241022");
        claudeConfig.setCachingEnabled(true);
        claudeConfig.setRateLimitPerMinute(60);
    }

    @Test
    @DisplayName("GET /api/v1/ai/status should return configuration information")
    @WithMockUser
    void testGetStatus_ReturnsConfigInfo() throws Exception {
        // Given: AI is enabled
        claudeConfig.setEnabled(true);

        // When/Then: Status endpoint returns config details
        mockMvc.perform(get("/api/v1/ai/status"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.model").value("claude-3-5-sonnet-20241022"))
            .andExpect(jsonPath("$.allowedQueryTypes").isArray())
            .andExpect(jsonPath("$.allowedQueryTypes", hasSize(6)))
            .andExpect(jsonPath("$.allowedQueryTypes", hasItem("care_gaps")))
            .andExpect(jsonPath("$.allowedQueryTypes", hasItem("quality_measures")))
            .andExpect(jsonPath("$.cachingEnabled").value(true))
            .andExpect(jsonPath("$.rateLimitPerMinute").value(60));
    }

    @Test
    @DisplayName("GET /api/v1/ai/health should return UP when AI enabled")
    @WithMockUser
    void testHealthCheck_WhenEnabled() throws Exception {
        // Given: AI is enabled
        claudeConfig.setEnabled(true);

        // When/Then: Health shows UP status
        mockMvc.perform(get("/api/v1/ai/health"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.aiEnabled").value(true))
            .andExpect(jsonPath("$.serviceAvailable").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/ai/health should return DOWN when AI disabled")
    @WithMockUser
    void testHealthCheck_WhenDisabled() throws Exception {
        // Given: AI is disabled
        claudeConfig.setEnabled(false);

        // When/Then: Health shows DOWN status
        mockMvc.perform(get("/api/v1/ai/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DOWN"))
            .andExpect(jsonPath("$.aiEnabled").value(false))
            .andExpect(jsonPath("$.serviceAvailable").value(true));
    }
}
```

#### Chat Endpoint Tests

```java
@Test
@DisplayName("POST /api/v1/ai/chat should return error when AI disabled")
@WithMockUser(username = "testuser")
void testChat_WhenAiDisabled() throws Exception {
    // Given: AI is disabled
    claudeConfig.setEnabled(false);

    ChatRequest request = ChatRequest.builder()
        .queryType("care_gaps")
        .query("What are the care gaps for patient?")
        .tenantId("test-tenant")
        .build();

    // When/Then: Error response (not exception)
    mockMvc.perform(post("/api/v1/ai/chat")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.error").value(true))
        .andExpect(jsonPath("$.errorMessage").value("AI service disabled"))
        .andExpect(jsonPath("$.response").value(containsString("not enabled")));
}

@Test
@DisplayName("POST /api/v1/ai/chat should reject invalid query types")
@WithMockUser(username = "testuser")
void testChat_WithInvalidQueryType() throws Exception {
    // Given: AI enabled but invalid query type
    claudeConfig.setEnabled(true);

    ChatRequest request = ChatRequest.builder()
        .queryType("invalid_type")  // Not in allowlist
        .query("Some query")
        .tenantId("test-tenant")
        .build();

    // When/Then: 400 Bad Request
    mockMvc.perform(post("/api/v1/ai/chat")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
}

@Test
@DisplayName("POST /api/v1/ai/chat should succeed with valid request")
@WithMockUser(username = "testuser")
void testChat_WithValidRequest() throws Exception {
    // Given: AI enabled with valid request
    claudeConfig.setEnabled(true);

    ChatRequest request = ChatRequest.builder()
        .queryType("care_gaps")
        .query("What are the care gaps for patient?")
        .tenantId("test-tenant")
        .build();

    ChatResponse mockResponse = ChatResponse.builder()
        .id("resp-123")
        .queryType("care_gaps")
        .response("Patient has 2 care gaps...")
        .model("claude-3-5-sonnet-20241022")
        .inputTokens(100)
        .outputTokens(200)
        .cached(false)
        .error(false)
        .build();

    when(claudeService.chat(any(ChatRequest.class))).thenReturn(mockResponse);

    // When/Then: Successful response with token metrics
    mockMvc.perform(post("/api/v1/ai/chat")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("resp-123"))
        .andExpect(jsonPath("$.queryType").value("care_gaps"))
        .andExpect(jsonPath("$.response").value(containsString("care gaps")))
        .andExpect(jsonPath("$.model").value("claude-3-5-sonnet-20241022"))
        .andExpect(jsonPath("$.inputTokens").value(100))
        .andExpect(jsonPath("$.outputTokens").value(200))
        .andExpect(jsonPath("$.error").value(false));

    verify(claudeService, times(1)).chat(any(ChatRequest.class));
}

@Test
@DisplayName("POST /api/v1/ai/chat should accept all allowed query types")
@WithMockUser(username = "testuser")
void testChat_WithAllAllowedQueryTypes() throws Exception {
    // Given: AI enabled
    claudeConfig.setEnabled(true);

    ChatResponse mockResponse = ChatResponse.builder()
        .response("Response")
        .error(false)
        .build();

    when(claudeService.chat(any(ChatRequest.class))).thenReturn(mockResponse);

    String[] allowedTypes = {
        "care_gaps",
        "quality_measures",
        "patient_summary",
        "measure_compliance",
        "population_health",
        "care_recommendations"
    };

    // When/Then: Each allowed type succeeds
    for (String queryType : allowedTypes) {
        ChatRequest request = ChatRequest.builder()
            .queryType(queryType)
            .query("Test query for " + queryType)
            .tenantId("test-tenant")
            .build();

        mockMvc.perform(post("/api/v1/ai/chat")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.error").value(false));
    }

    verify(claudeService, times(allowedTypes.length)).chat(any(ChatRequest.class));
}
```

#### Patient Summary and Care Gap Endpoints

```java
@Test
@DisplayName("POST /api/v1/ai/patient-summary should succeed when enabled")
@WithMockUser(username = "testuser")
void testGeneratePatientSummary_WhenEnabled() throws Exception {
    // Given: AI enabled
    claudeConfig.setEnabled(true);

    ChatResponse mockResponse = ChatResponse.builder()
        .response("Patient summary: 65yo male with diabetes...")
        .error(false)
        .build();

    when(claudeService.generatePatientSummary(eq("patient-123"), anyString()))
        .thenReturn(mockResponse);

    // When/Then: Summary generated
    mockMvc.perform(post("/api/v1/ai/patient-summary/patient-123")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"patientId\":\"patient-123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.response").value(containsString("Patient summary")))
        .andExpect(jsonPath("$.error").value(false));

    verify(claudeService, times(1))
        .generatePatientSummary(eq("patient-123"), anyString());
}

@Test
@DisplayName("POST /api/v1/ai/care-gaps/analyze should return error when AI disabled")
@WithMockUser(username = "testuser")
void testAnalyzeCareGaps_WhenDisabled() throws Exception {
    // Given: AI disabled
    claudeConfig.setEnabled(false);

    // When/Then: Error response
    mockMvc.perform(post("/api/v1/ai/care-gaps/analyze")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gaps\":[]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.error").value(true))
        .andExpect(jsonPath("$.errorMessage").value("AI service disabled"));
}

@Test
@DisplayName("GET /api/v1/ai/query should succeed when enabled")
@WithMockUser(username = "testuser")
void testAnswerQuery_WhenEnabled() throws Exception {
    // Given: AI enabled
    claudeConfig.setEnabled(true);

    ChatResponse mockResponse = ChatResponse.builder()
        .response("Diabetes is a chronic condition...")
        .error(false)
        .build();

    when(claudeService.answerClinicalQuery(eq("What is diabetes?"), anyString()))
        .thenReturn(mockResponse);

    // When/Then: Clinical answer returned
    mockMvc.perform(get("/api/v1/ai/query")
            .param("query", "What is diabetes?"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.response").value(containsString("Diabetes")))
        .andExpect(jsonPath("$.error").value(false));

    verify(claudeService, times(1))
        .answerClinicalQuery(eq("What is diabetes?"), anyString());
}
```

#### Controller Unit Tests

```java
@DisplayName("AiAssistantController Unit Tests")
class AiAssistantControllerUnitTest {

    @Test
    @DisplayName("Should return disabled response when service is missing")
    void shouldReturnDisabledWhenServiceMissing() {
        // Given: Controller with no ClaudeService (Optional.empty())
        ClaudeConfig config = new ClaudeConfig();
        config.setEnabled(true);
        AiAssistantController controller = new AiAssistantController(Optional.empty(), config);

        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("test")
            .tenantId("tenant-1")
            .build();

        // When: Attempt chat
        ResponseEntity<ChatResponse> response = controller.chat(request, null);

        // Then: Error response (graceful degradation)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isError()).isTrue();
    }

    @Test
    @DisplayName("Should handle chat requests when authentication is absent")
    void shouldHandleChatWithoutAuthentication() {
        // Given: Controller with service but null authentication
        ClaudeConfig config = new ClaudeConfig();
        config.setEnabled(true);
        ClaudeService service = mock(ClaudeService.class);
        ChatResponse serviceResponse = ChatResponse.builder().build();
        when(service.chat(any(ChatRequest.class))).thenReturn(serviceResponse);

        AiAssistantController controller = new AiAssistantController(Optional.of(service), config);

        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("test")
            .tenantId("tenant-1")
            .build();

        // When: Chat without authentication
        ResponseEntity<ChatResponse> response = controller.chat(request, null);

        // Then: Request processed with timing
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProcessingTimeMs()).isGreaterThanOrEqualTo(0);
        verify(service).chat(eq(request));
    }
}
```

---

### Configuration Tests

The `ClaudeConfigTest` validates configuration defaults and query type allowlisting.

```java
@DisplayName("ClaudeConfig Tests")
class ClaudeConfigTest {

    private ClaudeConfig config;

    @BeforeEach
    void setUp() {
        config = new ClaudeConfig();
    }

    @Test
    @DisplayName("Should have correct default configuration values")
    void testDefaultConfigurationValues() {
        // Verify all defaults
        assertThat(config.isEnabled()).isFalse();
        assertThat(config.getApiUrl()).isEqualTo("https://api.anthropic.com/v1");
        assertThat(config.getModel()).isEqualTo("claude-3-5-sonnet-20241022");
        assertThat(config.getMaxTokens()).isEqualTo(4096);
        assertThat(config.getTemperature()).isEqualTo(0.3);
        assertThat(config.getTopP()).isEqualTo(0.9);
        assertThat(config.getTimeoutSeconds()).isEqualTo(60);
        assertThat(config.getMaxRetries()).isEqualTo(3);
        assertThat(config.getRateLimitPerMinute()).isEqualTo(60);
        assertThat(config.isCachingEnabled()).isTrue();
        assertThat(config.getCacheTtlSeconds()).isEqualTo(300);  // 5 minutes
        assertThat(config.isDebugLogging()).isFalse();
    }

    @Test
    @DisplayName("Should have correct default allowed query types")
    void testDefaultAllowedQueryTypes() {
        // Verify 6 allowed query types
        assertThat(config.getAllowedQueryTypes())
            .hasSize(6)
            .contains(
                "care_gaps",
                "quality_measures",
                "patient_summary",
                "measure_compliance",
                "population_health",
                "care_recommendations"
            );
    }

    @Test
    @DisplayName("Should allow valid query types")
    void testIsQueryTypeAllowed_WithAllowedTypes() {
        // Each allowed type returns true
        assertThat(config.isQueryTypeAllowed("care_gaps")).isTrue();
        assertThat(config.isQueryTypeAllowed("quality_measures")).isTrue();
        assertThat(config.isQueryTypeAllowed("patient_summary")).isTrue();
        assertThat(config.isQueryTypeAllowed("measure_compliance")).isTrue();
        assertThat(config.isQueryTypeAllowed("population_health")).isTrue();
        assertThat(config.isQueryTypeAllowed("care_recommendations")).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid query types")
    void testIsQueryTypeAllowed_WithDisallowedTypes() {
        // Security: Disallowed types rejected
        assertThat(config.isQueryTypeAllowed("invalid_type")).isFalse();
        assertThat(config.isQueryTypeAllowed("sql_injection")).isFalse();
        assertThat(config.isQueryTypeAllowed("system_command")).isFalse();
        assertThat(config.isQueryTypeAllowed("")).isFalse();
        assertThat(config.isQueryTypeAllowed(null)).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when enabled but no API key")
    void testValidateConfiguration_WhenEnabledWithoutApiKey() {
        // Given: Enabled with no API key
        config.setEnabled(true);
        config.setApiKey(null);

        // When/Then: Validation fails
        assertThatThrownBy(() -> config.validateConfiguration())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Claude API key is required when claude.enabled=true");
    }

    @Test
    @DisplayName("Should not throw exception when enabled with valid API key")
    void testValidateConfiguration_WhenEnabledWithValidApiKey() {
        // Given: Enabled with valid API key
        config.setEnabled(true);
        config.setApiKey("sk-ant-api-key-12345678901234567890");

        // When/Then: Validation passes
        config.validateConfiguration();
        assertThat(config.isEnabled()).isTrue();
        assertThat(config.getApiKey()).isEqualTo("sk-ant-api-key-12345678901234567890");
    }

    @Test
    @DisplayName("Should have non-empty system prompt")
    void testSystemPrompt_IsNotEmpty() {
        // Verify system prompt configured for clinical AI
        assertThat(config.getSystemPrompt())
            .isNotNull()
            .isNotBlank()
            .contains("clinical AI assistant")
            .contains("HDIM")
            .contains("quality measures")
            .contains("care gaps");
    }
}
```

---

### DTO Validation Tests

#### ChatRequest Validation and Caching

```java
@DisplayName("ChatRequest Tests")
class ChatRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should generate cache key from tenant, queryType, and query hash")
    void testCacheKey_Generation() {
        ChatRequest request = ChatRequest.builder()
            .tenantId("tenant-123")
            .queryType("care_gaps")
            .query("What are the care gaps?")
            .patientId("patient-456")
            .build();

        String cacheKey = request.cacheKey();

        // Cache key includes tenant for isolation
        assertThat(cacheKey)
            .isNotNull()
            .startsWith("tenant-123:care_gaps:")
            .contains(":");
    }

    @Test
    @DisplayName("Should generate different cache keys for different tenants")
    void testCacheKey_DifferentTenants() {
        // Critical: Tenant isolation in cache keys
        ChatRequest request1 = ChatRequest.builder()
            .tenantId("tenant-1")
            .queryType("care_gaps")
            .query("Same query")
            .build();

        ChatRequest request2 = ChatRequest.builder()
            .tenantId("tenant-2")
            .queryType("care_gaps")
            .query("Same query")
            .build();

        String key1 = request1.cacheKey();
        String key2 = request2.cacheKey();

        // Different tenants = different cache keys
        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1).startsWith("tenant-1:");
        assertThat(key2).startsWith("tenant-2:");
    }

    @Test
    @DisplayName("Should fail validation when queryType is missing")
    void testValidation_MissingQueryType() {
        ChatRequest request = ChatRequest.builder()
            .query("Some query")
            .tenantId("tenant-123")
            .build();

        Set<ConstraintViolation<ChatRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
            .extracting(ConstraintViolation::getMessage)
            .contains("Query type is required");
    }

    @Test
    @DisplayName("Should fail validation when query exceeds max length")
    void testValidation_QueryTooLong() {
        // Given: Query exceeds 10000 characters
        String longQuery = "x".repeat(10001);
        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query(longQuery)
            .tenantId("tenant-123")
            .build();

        Set<ConstraintViolation<ChatRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
            .extracting(ConstraintViolation::getMessage)
            .contains("Query must be 10000 characters or less");
    }

    @Test
    @DisplayName("Should pass validation with all required fields")
    void testValidation_ValidRequest() {
        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("What are the care gaps?")
            .tenantId("tenant-123")
            .build();

        Set<ConstraintViolation<ChatRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
```

#### ChatResponse and ChatMessage Tests

```java
@DisplayName("ChatResponse")
class ChatResponseTest {

    @Test
    @DisplayName("Should apply default builder values")
    void shouldApplyDefaults() {
        ChatResponse response = ChatResponse.builder().build();

        assertThat(response.isCached()).isFalse();
        assertThat(response.isError()).isFalse();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should allow all fields to be set")
    void shouldSetFields() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");

        ChatResponse response = ChatResponse.builder()
            .id("resp-1")
            .queryType("care_gaps")
            .response("Result")
            .model("model-1")
            .inputTokens(10)
            .outputTokens(20)
            .cached(true)
            .timestamp(now)
            .processingTimeMs(150)
            .error(true)
            .errorMessage("failure")
            .metadata(Map.of("source", "test"))
            .sessionId("session-1")
            .suggestions(new String[] {"a", "b"})
            .build();

        // Verify all fields
        assertThat(response.getId()).isEqualTo("resp-1");
        assertThat(response.getQueryType()).isEqualTo("care_gaps");
        assertThat(response.getInputTokens()).isEqualTo(10);
        assertThat(response.getOutputTokens()).isEqualTo(20);
        assertThat(response.isCached()).isTrue();
        assertThat(response.getProcessingTimeMs()).isEqualTo(150);
        assertThat(response.getMetadata()).containsEntry("source", "test");
    }
}

@DisplayName("ChatMessage")
class ChatMessageTest {

    @Test
    @DisplayName("Should build user and assistant messages")
    void shouldBuildHelperMessages() {
        ChatMessage user = ChatMessage.user("hello");
        ChatMessage assistant = ChatMessage.assistant("hi");

        assertThat(user.getRole()).isEqualTo("user");
        assertThat(user.getContent()).isEqualTo("hello");
        assertThat(assistant.getRole()).isEqualTo("assistant");
        assertThat(assistant.getContent()).isEqualTo("hi");
    }
}
```

---

### Multi-Tenant Isolation Tests

Cache key generation ensures tenant isolation for AI responses.

```java
@Test
@DisplayName("Cache keys should be tenant-namespaced")
void cacheKeysShouldBeTenantNamespaced() {
    // Given: Same query for different tenants
    ChatRequest tenant1Request = ChatRequest.builder()
        .tenantId("tenant-acme")
        .queryType("patient_summary")
        .query("Generate patient summary")
        .patientId("patient-123")
        .build();

    ChatRequest tenant2Request = ChatRequest.builder()
        .tenantId("tenant-globex")
        .queryType("patient_summary")
        .query("Generate patient summary")
        .patientId("patient-123")
        .build();

    // When: Generate cache keys
    String key1 = tenant1Request.cacheKey();
    String key2 = tenant2Request.cacheKey();

    // Then: Different tenants have different cache keys
    assertThat(key1).startsWith("tenant-acme:");
    assertThat(key2).startsWith("tenant-globex:");
    assertThat(key1).isNotEqualTo(key2);
}

@Test
@DisplayName("Patient ID should affect cache key")
void patientIdShouldAffectCacheKey() {
    // Given: Same tenant, same query, different patients
    ChatRequest patient1Request = ChatRequest.builder()
        .tenantId("tenant-123")
        .queryType("patient_summary")
        .query("Generate summary")
        .patientId("patient-1")
        .build();

    ChatRequest patient2Request = ChatRequest.builder()
        .tenantId("tenant-123")
        .queryType("patient_summary")
        .query("Generate summary")
        .patientId("patient-2")
        .build();

    // When: Generate cache keys
    String key1 = patient1Request.cacheKey();
    String key2 = patient2Request.cacheKey();

    // Then: Different patients = different cache keys (PHI isolation)
    assertThat(key1).isNotEqualTo(key2);
}
```

---

### HIPAA Compliance Tests

#### Cache TTL Compliance

```java
@Test
@DisplayName("PHI cache TTL must not exceed 5 minutes")
void phiCacheTtlShouldBeCompliant() {
    // Given: Default configuration
    ClaudeConfig config = new ClaudeConfig();

    // Then: Cache TTL is 5 minutes (300 seconds)
    assertThat(config.getCacheTtlSeconds())
        .isLessThanOrEqualTo(300)
        .withFailMessage("PHI cache TTL exceeds 5 minutes (HIPAA violation)");
}

@Test
@DisplayName("Caching should be disabled by toggling config")
void cachingShouldBeToggleable() {
    // Given: Default config with caching enabled
    ClaudeConfig config = new ClaudeConfig();
    assertThat(config.isCachingEnabled()).isTrue();

    // When: Disable caching
    config.setCachingEnabled(false);

    // Then: Caching disabled
    assertThat(config.isCachingEnabled()).isFalse();
}
```

#### Synthetic Test Data Patterns

```java
/**
 * Test data generators use synthetic patterns to prevent PHI in tests.
 */
@Test
@DisplayName("Test data should use synthetic patient IDs")
void testDataShouldUseSyntheticPatientIds() {
    // Use clearly synthetic patient IDs in tests
    ChatRequest request = ChatRequest.builder()
        .tenantId("test-tenant-001")  // Synthetic tenant
        .queryType("patient_summary")
        .query("Generate summary for test patient")
        .patientId("TEST-PATIENT-001")  // Synthetic patient ID
        .build();

    assertThat(request.getPatientId()).startsWith("TEST-");
    assertThat(request.getTenantId()).startsWith("test-");
}

@Test
@DisplayName("No PHI should be logged in AI requests")
void noPhiInAiRequestLogging() {
    // Given: Service configured with debug logging disabled
    ClaudeConfig config = new ClaudeConfig();

    // Then: Debug logging is disabled by default
    assertThat(config.isDebugLogging()).isFalse();
}
```

#### Error Response Validation (No PHI Leakage)

```java
@Test
@DisplayName("Error responses should not leak PHI")
void errorResponsesShouldNotLeakPhi() {
    // Given: Error response
    ChatResponse errorResponse = ChatResponse.builder()
        .error(true)
        .errorMessage("AI service unavailable")
        .response("Unable to process request")
        .build();

    // Then: Generic error message (no PHI)
    assertThat(errorResponse.getErrorMessage())
        .doesNotContain("patient")
        .doesNotContain("MRN")
        .doesNotContain("SSN");
    assertThat(errorResponse.getResponse())
        .doesNotContain("patient")
        .doesNotContain("MRN");
}
```

---

### Performance Tests

#### AI Response Latency

```java
@Test
@DisplayName("AI response processing time should be tracked")
void aiResponseProcessingTimeShouldBeTracked() {
    // Given: Controller with service
    ClaudeConfig config = new ClaudeConfig();
    config.setEnabled(true);
    ClaudeService service = mock(ClaudeService.class);
    ChatResponse serviceResponse = ChatResponse.builder().build();
    when(service.chat(any(ChatRequest.class))).thenReturn(serviceResponse);

    AiAssistantController controller = new AiAssistantController(Optional.of(service), config);

    ChatRequest request = ChatRequest.builder()
        .queryType("care_gaps")
        .query("test")
        .tenantId("tenant-1")
        .build();

    // When: Process request
    ResponseEntity<ChatResponse> response = controller.chat(request, null);

    // Then: Processing time tracked
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getProcessingTimeMs()).isGreaterThanOrEqualTo(0);
}

@Test
@DisplayName("Cached responses should have zero processing overhead")
void cachedResponsesShouldHaveZeroOverhead() {
    // Given: Cached response
    ChatResponse cachedResponse = ChatResponse.builder()
        .cached(true)
        .processingTimeMs(0)
        .build();

    // Then: Cached responses have minimal latency
    assertThat(cachedResponse.isCached()).isTrue();
    assertThat(cachedResponse.getProcessingTimeMs()).isEqualTo(0);
}
```

#### Token Usage Tracking

```java
@Test
@DisplayName("Token usage should be tracked for cost analysis")
void tokenUsageShouldBeTracked() {
    // Given: Response with token metrics
    ChatResponse response = ChatResponse.builder()
        .inputTokens(500)
        .outputTokens(1000)
        .build();

    // Then: Token usage available for cost tracking
    // Cost estimate: ($3/1M input + $15/1M output)
    int totalTokens = response.getInputTokens() + response.getOutputTokens();
    assertThat(totalTokens).isEqualTo(1500);

    // Typical query cost: ~$0.015 for 500 input + 1000 output
    double estimatedCost = (500 * 3.0 / 1_000_000) + (1000 * 15.0 / 1_000_000);
    assertThat(estimatedCost).isLessThan(0.02);
}
```

---

### Test Configuration

#### Application Test Configuration

```yaml
# src/test/resources/application-test.yml
claude:
  enabled: false  # Disabled by default in tests
  api-key: test-key-not-real
  model: claude-3-5-sonnet-20241022
  max-tokens: 256  # Smaller for tests
  timeout-seconds: 5  # Faster timeout for tests
  caching-enabled: true
  cache-ttl-seconds: 300
  rate-limit-per-minute: 100
  debug-logging: false

spring:
  main:
    allow-bean-definition-overriding: true
```

#### WebClient Mock Setup Pattern

```java
/**
 * Pattern for mocking WebClient in AI service tests.
 * Uses ExchangeFunction to control HTTP responses.
 */
public class WebClientTestUtils {

    /**
     * Create mock WebClient with JSON response.
     */
    public static WebClient mockWebClient(String jsonResponse) {
        ExchangeFunction exchangeFunction = request -> Mono.just(
            ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Flux.just(new DefaultDataBufferFactory()
                    .wrap(jsonResponse.getBytes(StandardCharsets.UTF_8))))
                .build()
        );
        return WebClient.builder().exchangeFunction(exchangeFunction).build();
    }

    /**
     * Create mock WebClient with error response.
     */
    public static WebClient mockWebClientError(RuntimeException exception) {
        ExchangeFunction exchangeFunction = request -> Mono.error(exception);
        return WebClient.builder().exchangeFunction(exchangeFunction).build();
    }
}
```

---

### Best Practices

| Practice | Description |
|----------|-------------|
| **Mock AI APIs** | Never call real Claude API in tests; use ExchangeFunction pattern |
| **Test Query Types** | Validate all allowed query types and rejection of invalid types |
| **Verify Token Usage** | Assert inputTokens and outputTokens in responses |
| **Test Disabled State** | Always test behavior when `claude.enabled=false` |
| **Cache Key Isolation** | Verify tenant ID included in cache keys |
| **Error Graceful Degradation** | Test that API failures return error responses, not exceptions |
| **Circuit Breaker Fallbacks** | Test fallback behavior when circuit breaker open |
| **HIPAA Cache TTL** | Assert cache TTL ≤ 5 minutes (300 seconds) |
| **Synthetic Test Data** | Use TEST- prefixed IDs, never real PHI |
| **Configuration Validation** | Test API key required when enabled |

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| `ClaudeService` not found | Optional bean not resolved | Use `Optional.empty()` in unit tests |
| WebClient mock fails | ExchangeFunction not configured | Use `jsonExchange()` helper method |
| Query type rejected | Not in allowlist | Add to `allowedQueryTypes` in config |
| Cache key collision | Missing tenant ID | Ensure tenantId included in request |
| Token usage zero | Response missing `usage` field | Service defaults to zero; verify mock response |
| Test timeout | Real API call attempted | Verify WebClient mocked with ReflectionTestUtils |
| CSRF error in MockMvc | Missing `.with(csrf())` | Add csrf() post-processor to requests |
| Config validation fails | API key null when enabled | Set `config.setEnabled(false)` or provide test key |
| Processing time negative | Clock skew in test | Use `isGreaterThanOrEqualTo(0)` assertion |
| Health endpoint DOWN | Config `enabled=false` | Set `claudeConfig.setEnabled(true)` in test setup |

---

## Security and Compliance

### HIPAA Compliance
- No PHI sent to AI logs or monitoring
- Request/response audit logging to Kafka
- Encrypted data in transit (TLS)
- No patient data persistence in service
- Response caching with TTL controls

### Safety Guardrails
- Query type validation and allowlisting
- Rate limiting per user (prevents abuse)
- Response content filtering
- Clinical disclaimer requirements
- Human-in-the-loop for critical decisions

### Authentication
- JWT-based authentication required
- Role-based access control
- Tenant isolation enforced
- User attribution on all queries

## Response Caching

### Cache Strategy
- 5-minute TTL for identical queries
- Cache key: query + context hash
- Redis-based distributed cache
- Automatic cache invalidation
- Reduced API costs and latency

## Token Usage Tracking

All responses include token usage metrics:
- Input tokens (prompt + context)
- Output tokens (response)
- Total tokens
- Processing time in milliseconds

## Error Handling

### Service Disabled
When `claude.enabled=false`, returns:
```json
{
  "response": "AI assistant is not enabled",
  "error": true,
  "errorMessage": "AI service disabled"
}
```

### API Errors
- Automatic retry with exponential backoff
- Circuit breaker protection
- Graceful degradation
- Detailed error logging

## API Documentation

Swagger UI available at:
```
http://localhost:8090/swagger-ui.html
```

## Monitoring

### Actuator Endpoints
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

### Custom Metrics
- Total queries processed
- Average response time
- Token usage statistics
- Error rate by type
- Cache hit rate

## Integration

This service is typically called by:
- **Frontend UI**: Chatbot interface
- **Agent Runtime Service**: AI agent tool
- **Quality Measure Service**: Measure interpretation
- **Care Gap Service**: Gap analysis

## Costs and Usage

### API Costs (Approximate)
- Claude 3.5 Sonnet: $3 per million input tokens, $15 per million output tokens
- Typical query: 500-2000 tokens total
- Caching reduces costs by 60-80%

### Best Practices
- Enable caching for repeated queries
- Use appropriate context size (don't over-send data)
- Leverage rate limiting to control costs
- Monitor token usage metrics

## License

Copyright (c) 2024 Mahoosuc Solutions
