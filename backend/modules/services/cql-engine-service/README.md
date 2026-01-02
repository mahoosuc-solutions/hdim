# CQL Engine Service

Clinical Quality Language (CQL) execution engine for evaluating quality measures, clinical logic, and HEDIS calculations.

## Purpose

Executes CQL expressions against patient FHIR data, addressing the challenge that:
- Healthcare quality measures require complex clinical logic evaluation
- CQL libraries need versioning, validation, and dependency management
- Measure evaluation must scale across thousands of patients (batch processing)
- Results must be cached (5 min TTL) while maintaining HIPAA compliance

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    CQL Engine Service                            │
│                         (Port 8081)                              │
├─────────────────────────────────────────────────────────────────┤
│  Controller Layer                                                │
│  ├── CqlEvaluationController      - Execute CQL, batch runs     │
│  ├── CqlLibraryController         - CRUD for CQL libraries      │
│  ├── ValueSetController           - Manage value sets           │
│  └── VisualizationController      - Real-time progress (WS)     │
├─────────────────────────────────────────────────────────────────┤
│  Service Layer                                                   │
│  ├── CqlEvaluationService         - Execute CQL, retry logic    │
│  ├── CqlLibraryService            - Library versioning          │
│  ├── ValueSetService              - VSAC integration            │
│  └── CqlEngineExecutor            - CQF Engine wrapper          │
├─────────────────────────────────────────────────────────────────┤
│  Repository Layer                                                │
│  ├── CqlEvaluationRepository                                    │
│  ├── CqlLibraryRepository                                       │
│  └── ValueSetRepository                                         │
├─────────────────────────────────────────────────────────────────┤
│  Domain Entities                                                 │
│  ├── CqlEvaluation    - Execution results, status tracking      │
│  ├── CqlLibrary       - CQL code, version, dependencies         │
│  └── ValueSet         - Clinical codes, OIDs, expansion         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Feign (HTTP) / Circuit Breaker
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      FHIR Service (Port 8085)                    │
│  - Patient data retrieval for CQL evaluation                    │
│  - Observation, Condition, MedicationRequest queries            │
└─────────────────────────────────────────────────────────────────┘
```

## API Endpoints

### CQL Evaluation
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/cql/evaluations` | Execute CQL for patient |
| POST | `/api/v1/cql/evaluations/{id}/execute` | Execute existing evaluation |
| POST | `/api/v1/cql/evaluations/batch` | Batch evaluate patients |
| POST | `/api/v1/cql/evaluations/{id}/retry` | Retry failed evaluation |
| GET | `/api/v1/cql/evaluations` | List evaluations (paginated) |
| GET | `/api/v1/cql/evaluations/{id}` | Get evaluation details |
| GET | `/api/v1/cql/evaluations/patient/{patientId}` | Get patient evaluations |
| GET | `/api/v1/cql/evaluations/by-status/{status}` | Filter by status |

### CQL Libraries
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/cql/libraries` | Create library |
| PUT | `/api/v1/cql/libraries/{id}` | Update library |
| GET | `/api/v1/cql/libraries/{id}` | Get library |
| GET | `/api/v1/cql/libraries` | List libraries |
| POST | `/api/v1/cql/libraries/{id}/validate` | Validate CQL syntax |

### Value Sets
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/cql/valuesets` | Create value set |
| PUT | `/api/v1/cql/valuesets/{id}` | Update value set |
| GET | `/api/v1/cql/valuesets` | List value sets |
| GET | `/api/v1/cql/valuesets/oid/{oid}` | Get by OID |

## Configuration

```yaml
server:
  port: 8081
  servlet:
    context-path: /cql-engine

# FHIR server for data retrieval
fhir:
  server:
    url: http://localhost:8085/fhir

# Redis cache (HIPAA-compliant TTL)
spring.cache:
  type: redis
  redis:
    time-to-live: 300000  # 5 minutes for PHI

# HEDIS measures
hedis:
  measures:
    enabled: true
    cache-ttl-hours: 0.083  # 5 minutes

# Real-time visualization
visualization:
  websocket:
    enabled: true
  kafka:
    enabled: true
    topics:
      evaluation-completed: "evaluation.completed"
```

## Dependencies

- **Spring Boot**: Web, JPA, Validation, Cache
- **Database**: PostgreSQL with Liquibase migrations
- **Cache**: Redis (5 min TTL for HIPAA compliance)
- **CQF**: OpenCDS CQF Engine for CQL execution
- **FHIR**: HAPI FHIR R4 for resource parsing
- **Messaging**: Kafka for evaluation events
- **Resilience**: Resilience4j for FHIR service integration

## Running Locally

```bash
# From backend directory
./gradlew :modules:services:cql-engine-service:bootRun

# Or via Docker
docker compose --profile cql up cql-engine-service
```

## Testing

CQL Engine Service has 18+ test files covering unit, integration, multi-tenant security, RBAC, HIPAA compliance, and performance testing for CQL evaluation and library management.

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:cql-engine-service:test

# Run specific test suite
./gradlew :modules:services:cql-engine-service:test --tests "*IntegrationTest"
./gradlew :modules:services:cql-engine-service:test --tests "*SecurityTest"
./gradlew :modules:services:cql-engine-service:test --tests "*PerformanceIntegrationTest"

# Run with coverage
./gradlew :modules:services:cql-engine-service:test jacocoTestReport

# Run tests excluding Docker-dependent tests
./gradlew :modules:services:cql-engine-service:test -DexcludeTags="testcontainers"
```

### Test Organization

```
src/test/java/com/healthdata/cql/
├── config/
│   ├── CqlTestContainersConfiguration.java   # PostgreSQL, Kafka containers
│   └── TestRedisConfiguration.java           # Redis mock configuration
├── test/
│   └── CqlTestcontainersBase.java            # Base class for Kafka tests
├── integration/
│   ├── CqlEngineServiceIntegrationTest.java      # Service layer tests
│   ├── CqlEvaluationControllerIntegrationTest.java  # Evaluation API tests
│   ├── CqlLibraryControllerIntegrationTest.java     # Library API tests (460 lines)
│   ├── ValueSetControllerIntegrationTest.java       # ValueSet API tests
│   ├── PerformanceIntegrationTest.java              # Performance benchmarks (420 lines)
│   ├── DataIntegrityIntegrationTest.java            # Data consistency tests
│   ├── ErrorHandlingIntegrationTest.java            # Error scenario coverage
│   └── ServiceLayerIntegrationTest.java             # Service orchestration
├── security/
│   ├── MultiTenantSecurityTest.java              # Tenant isolation (357 lines)
│   ├── InputValidationSecurityTest.java          # Input sanitization
│   ├── JwtTokenServiceTest.java                  # JWT parsing tests
│   ├── JwtAuthenticationFilterTest.java          # Auth filter tests
│   └── ApiAuthenticationSecurityTest.java        # API security tests
├── audit/
│   └── DataFlowTrackerTest.java                  # Data flow auditing
└── event/audit/
    └── AuditEventProducerTest.java               # Kafka audit events
```

### Unit Tests (Service Layer)

Unit tests verify CQL evaluation, library management, and value set operations using Mockito.

**Example: CqlEvaluationService Unit Test**

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("CQL Evaluation Service Tests")
class CqlEvaluationServiceTest {

    @Mock
    private CqlEvaluationRepository evaluationRepository;

    @Mock
    private CqlLibraryRepository libraryRepository;

    @Mock
    private FhirServiceClient fhirServiceClient;

    @Mock
    private CqlEngineExecutor cqlEngineExecutor;

    @InjectMocks
    private CqlEvaluationService evaluationService;

    private static final String TENANT_ID = "tenant-test-001";
    private static final UUID LIBRARY_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @Nested
    @DisplayName("Single Patient Evaluation")
    class SinglePatientEvaluation {

        @Test
        @DisplayName("Should evaluate CQL successfully for patient")
        void shouldEvaluateCqlSuccessfully() {
            // Given
            CqlLibrary library = createTestLibrary("DiabetesScreening", "1.0.0");
            when(libraryRepository.findByTenantIdAndIdAndActiveTrue(TENANT_ID, LIBRARY_ID))
                .thenReturn(Optional.of(library));

            Bundle patientData = createTestPatientBundle();
            when(fhirServiceClient.getPatientBundle(PATIENT_ID, TENANT_ID))
                .thenReturn(patientData);

            EvaluationResult expectedResult = EvaluationResult.builder()
                .patientId(PATIENT_ID)
                .inNumerator(true)
                .inDenominator(true)
                .build();
            when(cqlEngineExecutor.evaluate(any())).thenReturn(expectedResult);

            // When
            CqlEvaluation result = evaluationService.evaluate(TENANT_ID, LIBRARY_ID, PATIENT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("SUCCESS");
            assertThat(result.getTenantId()).isEqualTo(TENANT_ID);

            verify(fhirServiceClient).getPatientBundle(PATIENT_ID, TENANT_ID);
            verify(cqlEngineExecutor).evaluate(argThat(req ->
                req.getLibrary().getId().equals(LIBRARY_ID) &&
                req.getPatientId().equals(PATIENT_ID)
            ));
        }

        @Test
        @DisplayName("Should throw when library not found")
        void shouldThrowWhenLibraryNotFound() {
            // Given
            when(libraryRepository.findByTenantIdAndIdAndActiveTrue(TENANT_ID, LIBRARY_ID))
                .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> evaluationService.evaluate(TENANT_ID, LIBRARY_ID, PATIENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Library")
                .hasMessageContaining(LIBRARY_ID.toString());
        }

        @Test
        @DisplayName("Should retry on transient failure")
        void shouldRetryOnTransientFailure() {
            // Given
            CqlLibrary library = createTestLibrary("RetryTest", "1.0.0");
            when(libraryRepository.findByTenantIdAndIdAndActiveTrue(TENANT_ID, LIBRARY_ID))
                .thenReturn(Optional.of(library));

            Bundle patientData = createTestPatientBundle();
            when(fhirServiceClient.getPatientBundle(PATIENT_ID, TENANT_ID))
                .thenThrow(new RuntimeException("Transient failure"))
                .thenReturn(patientData); // Success on retry

            // When
            CqlEvaluation result = evaluationService.evaluateWithRetry(
                TENANT_ID, LIBRARY_ID, PATIENT_ID, 3);

            // Then
            assertThat(result.getStatus()).isEqualTo("SUCCESS");
            verify(fhirServiceClient, times(2)).getPatientBundle(PATIENT_ID, TENANT_ID);
        }
    }

    @Nested
    @DisplayName("Batch Evaluation")
    class BatchEvaluation {

        @Test
        @DisplayName("Should evaluate batch of patients")
        void shouldEvaluateBatch() {
            // Given
            CqlLibrary library = createTestLibrary("BatchTest", "1.0.0");
            when(libraryRepository.findByTenantIdAndIdAndActiveTrue(TENANT_ID, LIBRARY_ID))
                .thenReturn(Optional.of(library));

            List<UUID> patientIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            // When
            BatchEvaluationResult result = evaluationService.evaluateBatch(
                TENANT_ID, LIBRARY_ID, patientIds);

            // Then
            assertThat(result.getTotalPatients()).isEqualTo(3);
            assertThat(result.getSuccessCount()).isGreaterThan(0);
        }
    }

    private CqlLibrary createTestLibrary(String name, String version) {
        CqlLibrary library = new CqlLibrary(TENANT_ID, name, version);
        library.setId(LIBRARY_ID);
        library.setCqlContent("library " + name + " version '" + version + "'");
        library.setStatus("ACTIVE");
        return library;
    }

    private Bundle createTestPatientBundle() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        return bundle;
    }
}
```

### Integration Tests (API Endpoints)

Integration tests verify controller endpoints with real database containers.

**Example: CqlLibraryController Integration Test** (from `CqlLibraryControllerIntegrationTest.java`):

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
public class CqlLibraryControllerIntegrationTest extends CqlTestcontainersBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CqlLibraryRepository libraryRepository;

    private static final String BASE_URL = "/api/v1/cql/libraries";
    private static final String TENANT_ID = "test-tenant";

    private CqlLibrary testLibrary;

    @BeforeEach
    void setUp() {
        testLibrary = new CqlLibrary(TENANT_ID, "DiabetesScreening", "1.0.0");
        testLibrary.setStatus("ACTIVE");
        testLibrary.setCqlContent("library DiabetesScreening version '1.0.0'");
        testLibrary = libraryRepository.save(testLibrary);
    }

    @Test
    @Order(1)
    @DisplayName("Should create a new library")
    void testCreateLibrary() throws Exception {
        CqlLibraryRequest newLibrary = new CqlLibraryRequest(
            "NewLibrary", "1.0.0", "library NewLibrary version '1.0.0'");
        newLibrary.setStatus("DRAFT");

        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newLibrary)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.libraryName").value("NewLibrary"))
            .andExpect(jsonPath("$.version").value("1.0.0"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.tenantId").value(TENANT_ID));
    }

    @Test
    @Order(7)
    @DisplayName("Should get latest library version")
    void testGetLatestLibraryVersion() throws Exception {
        libraryRepository.save(new CqlLibrary(TENANT_ID, "VersionTest", "1.0.0"));
        libraryRepository.save(new CqlLibrary(TENANT_ID, "VersionTest", "2.0.0"));

        mockMvc.perform(get(BASE_URL + "/by-name/VersionTest/latest")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.libraryName").value("VersionTest"))
            .andExpect(jsonPath("$.version").value("2.0.0"));
    }

    @Test
    @Order(22)
    @DisplayName("Should enforce multi-tenant isolation")
    void testMultiTenantIsolation() throws Exception {
        String otherTenant = "other-tenant";

        CqlLibrary otherLibrary = new CqlLibrary(otherTenant, "OtherTenantLibrary", "1.0.0");
        otherLibrary = libraryRepository.save(otherLibrary);

        // Try to access other tenant's library - should be 404
        mockMvc.perform(get(BASE_URL + "/" + otherLibrary.getId())
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound());

        // Verify own library is accessible
        mockMvc.perform(get(BASE_URL + "/" + testLibrary.getId())
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    @Order(23)
    @DisplayName("Should handle library lifecycle transitions")
    void testLibraryLifecycle() throws Exception {
        // Create DRAFT library
        CqlLibraryRequest lifecycle = new CqlLibraryRequest(
            "LifecycleTest", "1.0.0", "library LifecycleTest version '1.0.0'");
        lifecycle.setStatus("DRAFT");

        String createResponse = mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lifecycle)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn().getResponse().getContentAsString();

        CqlLibrary created = objectMapper.readValue(createResponse, CqlLibrary.class);

        // DRAFT -> ACTIVE
        mockMvc.perform(post(BASE_URL + "/" + created.getId() + "/activate")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        // ACTIVE -> RETIRED
        mockMvc.perform(post(BASE_URL + "/" + created.getId() + "/retire")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RETIRED"));
    }
}
```

### Multi-Tenant Isolation Tests

HIPAA-compliant multi-tenant isolation tests verify tenant data separation at repository layer.

**Example: MultiTenantSecurityTest** (from `MultiTenantSecurityTest.java`, 357 lines):

```java
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
@DisplayName("Multi-Tenant Data Isolation Security Tests")
class MultiTenantSecurityTest extends CqlTestcontainersBase {

    @Autowired
    private CqlLibraryRepository libraryRepository;

    @Autowired
    private CqlEvaluationRepository evaluationRepository;

    @Autowired
    private ValueSetRepository valueSetRepository;

    private static final String TENANT_A = "tenant-alpha";
    private static final String TENANT_B = "tenant-beta";
    private static final String TENANT_C = "tenant-gamma";  // Unknown tenant

    private CqlLibrary tenantALibrary;
    private CqlLibrary tenantBLibrary;
    private CqlEvaluation tenantAEvaluation;
    private CqlEvaluation tenantBEvaluation;
    private ValueSet tenantAValueSet;
    private ValueSet tenantBValueSet;

    @BeforeEach
    void setUp() {
        // Create test data for Tenant A
        tenantALibrary = new CqlLibrary(TENANT_A, "TenantALibrary", "1.0.0");
        tenantALibrary.setStatus("ACTIVE");
        tenantALibrary.setCqlContent("library TenantALibrary version '1.0.0'");
        tenantALibrary.setDescription("Confidential Tenant A Measure");
        tenantALibrary = libraryRepository.save(tenantALibrary);

        tenantAEvaluation = new CqlEvaluation(TENANT_A, tenantALibrary, UUID.randomUUID());
        tenantAEvaluation.setStatus("SUCCESS");
        tenantAEvaluation = evaluationRepository.save(tenantAEvaluation);

        tenantAValueSet = new ValueSet(TENANT_A,
            "2.16.840.1.113883.3.464.1003.101.12.1001", "TenantA-ValueSet", "SNOMED");
        tenantAValueSet = valueSetRepository.save(tenantAValueSet);

        // Create test data for Tenant B
        tenantBLibrary = new CqlLibrary(TENANT_B, "TenantBLibrary", "1.0.0");
        tenantBLibrary.setStatus("ACTIVE");
        tenantBLibrary.setCqlContent("library TenantBLibrary version '1.0.0'");
        tenantBLibrary.setDescription("Confidential Tenant B Measure");
        tenantBLibrary = libraryRepository.save(tenantBLibrary);

        tenantBEvaluation = new CqlEvaluation(TENANT_B, tenantBLibrary, UUID.randomUUID());
        tenantBEvaluation.setStatus("SUCCESS");
        tenantBEvaluation = evaluationRepository.save(tenantBEvaluation);

        tenantBValueSet = new ValueSet(TENANT_B,
            "2.16.840.1.113883.3.464.1003.101.12.1002", "TenantB-ValueSet", "SNOMED");
        tenantBValueSet = valueSetRepository.save(tenantBValueSet);
    }

    // ==================== CQL Library Isolation Tests ====================

    @Test
    @DisplayName("Tenant A should only see Tenant A's libraries")
    void testLibraryIsolation_TenantA_OnlySeesOwnLibraries() {
        List<CqlLibrary> tenantALibraries = libraryRepository.findByTenantIdAndActiveTrue(TENANT_A);

        assertThat(tenantALibraries).isNotEmpty();
        assertThat(tenantALibraries)
            .allMatch(lib -> TENANT_A.equals(lib.getTenantId()))
            .noneMatch(lib -> TENANT_B.equals(lib.getTenantId()));
    }

    @Test
    @DisplayName("Cross-tenant library access should return empty")
    void testLibraryIsolation_CrossTenantAccess_ReturnsEmpty() {
        Optional<CqlLibrary> crossTenantAccess = libraryRepository
            .findByTenantIdAndLibraryNameAndVersionAndActiveTrue(
                TENANT_A,
                tenantBLibrary.getLibraryName(),
                tenantBLibrary.getVersion()
            );

        assertThat(crossTenantAccess).isEmpty();
    }

    @Test
    @DisplayName("Unknown tenant should see no libraries")
    void testLibraryIsolation_UnknownTenant_ReturnsEmpty() {
        List<CqlLibrary> unknownTenantLibraries = libraryRepository
            .findByTenantIdAndActiveTrue(TENANT_C);

        assertThat(unknownTenantLibraries).isEmpty();
    }

    // ==================== CQL Evaluation Isolation Tests ====================

    @Test
    @DisplayName("Patient evaluations should be tenant-scoped")
    void testEvaluationIsolation_PatientEvaluations_TenantScoped() {
        UUID patientA = tenantAEvaluation.getPatientId();
        UUID patientB = tenantBEvaluation.getPatientId();

        List<CqlEvaluation> tenantAPatientEvals = evaluationRepository
            .findByTenantIdAndPatientId(TENANT_A, patientA);

        List<CqlEvaluation> crossTenantPatientEvals = evaluationRepository
            .findByTenantIdAndPatientId(TENANT_A, patientB);

        assertThat(tenantAPatientEvals).isNotEmpty();
        assertThat(crossTenantPatientEvals).isEmpty();
    }

    // ==================== ValueSet Isolation Tests ====================

    @Test
    @DisplayName("ValueSet OID lookup should be tenant-scoped")
    void testValueSetIsolation_OidLookup_TenantScoped() {
        Optional<ValueSet> crossTenantOidLookup = valueSetRepository
            .findByTenantIdAndOidAndActiveTrue(TENANT_A, tenantBValueSet.getOid());

        assertThat(crossTenantOidLookup).isEmpty();
    }

    // ==================== PHI Protection Tests ====================

    @Test
    @DisplayName("Patient data should not leak between tenants")
    void testPatientDataIsolation_NoLeakBetweenTenants() {
        UUID sensitivePatientId = UUID.randomUUID();

        CqlEvaluation sensitiveEval = new CqlEvaluation(TENANT_A, tenantALibrary, sensitivePatientId);
        sensitiveEval.setStatus("SUCCESS");
        evaluationRepository.save(sensitiveEval);

        // Try to access from Tenant B context
        List<CqlEvaluation> tenantBSearch = evaluationRepository
            .findByTenantIdAndPatientId(TENANT_B, sensitivePatientId);

        assertThat(tenantBSearch).isEmpty();
    }

    @Test
    @DisplayName("Library content should not leak between tenants")
    void testLibraryContentIsolation_NoLeakBetweenTenants() {
        tenantALibrary.setCqlContent("/* CONFIDENTIAL: Contains proprietary algorithms */");
        libraryRepository.save(tenantALibrary);

        List<CqlLibrary> tenantBLibraries = libraryRepository.findByTenantIdAndActiveTrue(TENANT_B);

        assertThat(tenantBLibraries)
            .noneMatch(lib -> lib.getCqlContent() != null &&
                             lib.getCqlContent().contains("CONFIDENTIAL"));
    }

    // ==================== Concurrent Tenant Access Tests ====================

    @Test
    @DisplayName("Multiple tenants can have same-named libraries")
    void testMultipleTenants_SameNamedLibraries_Isolated() {
        CqlLibrary tenantADup = new CqlLibrary(TENANT_A, "SharedMeasure", "1.0.0");
        tenantADup.setStatus("ACTIVE");
        libraryRepository.save(tenantADup);

        CqlLibrary tenantBDup = new CqlLibrary(TENANT_B, "SharedMeasure", "1.0.0");
        tenantBDup.setStatus("ACTIVE");
        libraryRepository.save(tenantBDup);

        Optional<CqlLibrary> tenantAResult = libraryRepository
            .findByTenantIdAndLibraryNameAndVersionAndActiveTrue(TENANT_A, "SharedMeasure", "1.0.0");
        Optional<CqlLibrary> tenantBResult = libraryRepository
            .findByTenantIdAndLibraryNameAndVersionAndActiveTrue(TENANT_B, "SharedMeasure", "1.0.0");

        assertThat(tenantAResult).isPresent();
        assertThat(tenantBResult).isPresent();
        assertThat(tenantAResult.get().getId()).isNotEqualTo(tenantBResult.get().getId());
    }
}
```

### RBAC/Permission Tests

Role-based access control tests verify that only authorized roles can perform operations.

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CqlRbacTest extends CqlTestcontainersBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CqlLibraryRepository libraryRepository;

    private static final String TENANT_ID = "tenant-rbac-001";

    @Test
    @DisplayName("Admin should be able to create and activate libraries")
    void adminCanManageLibraries() throws Exception {
        mockMvc.perform(post("/api/v1/cql/libraries")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "admin-001")
                .header("X-Auth-Roles", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"libraryName\":\"AdminLib\",\"version\":\"1.0.0\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Evaluator should be able to run evaluations")
    void evaluatorCanRunEvaluations() throws Exception {
        CqlLibrary library = libraryRepository.save(
            new CqlLibrary(TENANT_ID, "EvalTest", "1.0.0"));

        mockMvc.perform(post("/api/v1/cql/evaluations")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "evaluator-001")
                .header("X-Auth-Roles", "EVALUATOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"libraryId\":\"" + library.getId() + "\",\"patientId\":\"patient-123\"}"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Viewer should NOT be able to create libraries")
    void viewerCannotCreateLibraries() throws Exception {
        mockMvc.perform(post("/api/v1/cql/libraries")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "viewer-001")
                .header("X-Auth-Roles", "VIEWER")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"libraryName\":\"ViewerLib\",\"version\":\"1.0.0\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Viewer should be able to read evaluations")
    void viewerCanReadEvaluations() throws Exception {
        mockMvc.perform(get("/api/v1/cql/evaluations")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "viewer-001")
                .header("X-Auth-Roles", "VIEWER"))
            .andExpect(status().isOk());
    }
}
```

### HIPAA Compliance Tests

PHI cache TTL and audit logging verification.

```java
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
class CqlHipaaComplianceTest extends CqlTestcontainersBase {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Evaluation results cache TTL must not exceed 5 minutes")
    void evaluationCacheTtlShouldBeCompliant() {
        Cache evaluationCache = cacheManager.getCache("cqlEvaluations");

        assertThat(evaluationCache).isNotNull();

        // Verify Redis TTL configuration
        if (evaluationCache instanceof RedisCache) {
            RedisCacheConfiguration config = ((RedisCache) evaluationCache).getCacheConfiguration();
            assertThat(config.getTtl().getSeconds())
                .isLessThanOrEqualTo(300L)
                .withFailMessage("Evaluation cache TTL exceeds 5 minutes (HIPAA violation)");
        }
    }

    @Test
    @DisplayName("Evaluation responses must include no-cache headers")
    void evaluationResponsesShouldIncludeNoCacheHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/cql/evaluations/patient/patient-123")
                .header("X-Tenant-ID", "tenant-001"))
            .andExpect(header().string("Cache-Control",
                allOf(
                    containsString("no-store"),
                    containsString("no-cache")
                )))
            .andExpect(header().string("Pragma", "no-cache"));
    }

    @Test
    @DisplayName("CQL content should not contain real patient identifiers")
    void cqlContentShouldNotContainRealPhi() {
        // Verify test libraries don't contain real PHI patterns
        List<CqlLibrary> testLibraries = libraryRepository.findAll();

        for (CqlLibrary library : testLibraries) {
            String content = library.getCqlContent();
            if (content != null) {
                assertThat(content)
                    .doesNotContainPattern("\\d{3}-\\d{2}-\\d{4}") // SSN pattern
                    .doesNotContainPattern("\\d{9}");              // 9-digit numbers
            }
        }
    }
}
```

### Performance Tests

Performance benchmarks verify CQL evaluation latency and throughput SLAs.

**Example: PerformanceIntegrationTest** (from `PerformanceIntegrationTest.java`, 420 lines):

```java
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
public class PerformanceIntegrationTest extends CqlTestcontainersBase {

    @Autowired
    private CqlLibraryRepository libraryRepository;

    @Autowired
    private CqlEvaluationRepository evaluationRepository;

    @Autowired
    private ValueSetRepository valueSetRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final int BULK_SIZE = 100;

    @Test
    @DisplayName("Should handle bulk library creation efficiently")
    void testBulkLibraryCreation() {
        long startTime = System.currentTimeMillis();

        List<CqlLibrary> libraries = new ArrayList<>();
        for (int i = 0; i < BULK_SIZE; i++) {
            CqlLibrary library = new CqlLibrary(TENANT_ID, "BulkLib" + i, "1.0.0");
            library.setStatus("ACTIVE");
            libraries.add(library);
        }

        libraryRepository.saveAll(libraries);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(duration)
            .isLessThan(5000L)
            .withFailMessage("Bulk insert of %d libraries took %dms (exceeds 5s SLA)",
                BULK_SIZE, duration);

        System.out.printf("Bulk created %d libraries in %dms%n", BULK_SIZE, duration);
    }

    @Test
    @DisplayName("Should query libraries efficiently with indexes")
    void testLibraryQueryPerformance() {
        for (int i = 0; i < 50; i++) {
            CqlLibrary library = new CqlLibrary(TENANT_ID, "QueryTest" + i, "1.0.0");
            library.setStatus("ACTIVE");
            libraryRepository.save(library);
        }

        long startTime = System.currentTimeMillis();
        List<CqlLibrary> results = libraryRepository.findByTenantIdAndActiveTrue(TENANT_ID);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(results.size()).isGreaterThanOrEqualTo(50);
        assertThat(duration)
            .isLessThan(5000L)
            .withFailMessage("Query took %dms (exceeds 5s SLA)", duration);
    }

    @Test
    @DisplayName("Should handle bulk evaluation creation efficiently")
    void testBulkEvaluationCreation() {
        CqlLibrary library = libraryRepository.save(
            new CqlLibrary(TENANT_ID, "BulkEvalLib", "1.0.0"));

        long startTime = System.currentTimeMillis();

        List<CqlEvaluation> evaluations = new ArrayList<>();
        for (int i = 0; i < BULK_SIZE; i++) {
            CqlEvaluation evaluation = new CqlEvaluation(TENANT_ID, library, UUID.randomUUID());
            evaluation.setStatus("PENDING");
            evaluation.setEvaluationDate(Instant.now());
            evaluations.add(evaluation);
        }

        evaluationRepository.saveAll(evaluations);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(duration)
            .isLessThan(5000L)
            .withFailMessage("Bulk insert of %d evaluations took %dms", BULK_SIZE, duration);
    }

    @Test
    @DisplayName("Should handle date range queries efficiently")
    void testDateRangeQueryPerformance() {
        CqlLibrary library = libraryRepository.save(
            new CqlLibrary(TENANT_ID, "DateRangeLib", "1.0.0"));

        Instant now = Instant.now();
        for (int i = 0; i < 60; i++) {
            CqlEvaluation eval = new CqlEvaluation(TENANT_ID, library, UUID.randomUUID());
            eval.setEvaluationDate(now.minusSeconds(i * 3600)); // Hours apart
            evaluationRepository.save(eval);
        }

        long startTime = System.currentTimeMillis();
        List<CqlEvaluation> results = evaluationRepository.findByDateRange(
            TENANT_ID,
            now.minusSeconds(24 * 3600),
            now);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(results.size()).isGreaterThanOrEqualTo(24);
        assertThat(duration).isLessThan(5000L);
    }

    @Test
    @DisplayName("Should handle large text fields efficiently")
    void testLargeTextFields() {
        String largeCqlContent = "A".repeat(10000);  // 10KB CQL
        String largeElmJson = "{\"content\":\"" + "X".repeat(50000) + "\"}"; // 50KB ELM

        long startTime = System.currentTimeMillis();

        CqlLibrary library = new CqlLibrary(TENANT_ID, "LargeTextLib", "1.0.0");
        library.setCqlContent(largeCqlContent);
        library.setElmJson(largeElmJson);
        library = libraryRepository.save(library);

        CqlLibrary retrieved = libraryRepository.findById(library.getId()).orElse(null);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getCqlContent().length()).isEqualTo(10000);
        assertThat(duration).isLessThan(5000L);
    }

    @Test
    @DisplayName("Should maintain performance with deep pagination")
    void testDeepPaginationPerformance() {
        for (int i = 0; i < 150; i++) {
            libraryRepository.save(new CqlLibrary(TENANT_ID, "DeepPage" + i, "1.0.0"));
        }

        long startTime = System.currentTimeMillis();
        Page<CqlLibrary> deepPage = libraryRepository.findByTenantIdAndActiveTrue(
            TENANT_ID, PageRequest.of(7, 20)); // Page 7 (140-160)
        long duration = System.currentTimeMillis() - startTime;

        assertThat(deepPage.getContent()).isNotEmpty();
        assertThat(duration)
            .isLessThan(1500L)
            .withFailMessage("Deep pagination took %dms (exceeds 1.5s SLA)", duration);
    }
}
```

### Test Configuration

**CqlTestcontainersBase** - Base class for tests requiring Kafka:

```java
@Testcontainers(disabledWithoutDocker = true)
public abstract class CqlTestcontainersBase {

    @DynamicPropertySource
    static void configureKafkaProperties(DynamicPropertyRegistry registry) {
        CqlTestContainersConfiguration.configureKafka(registry);
    }
}
```

**TestRedisConfiguration** - Redis mock for unit/integration tests:

```java
@Configuration
@Profile("test")
public class TestRedisConfiguration {

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", 6379));
    }

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))  // HIPAA-compliant 5 min TTL
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
```

### Best Practices

| Practice | Description |
|----------|-------------|
| **Tenant Isolation** | Every repository query includes `tenantId` filter |
| **Library Versioning** | Test version lifecycle: DRAFT → ACTIVE → RETIRED |
| **CQL Validation** | Validate CQL syntax before saving libraries |
| **Batch Testing** | Test bulk evaluation with realistic patient counts |
| **Cache TTL** | Verify 5-minute TTL for evaluation results (HIPAA) |
| **Performance SLAs** | Single eval: 50-200ms, Batch: 10 patients/sec |
| **PHI Protection** | No real patient data in test CQL content |

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| TestContainers timeout | Docker not running | Start Docker Desktop |
| Kafka connection fails | Kafka container slow to start | Increase startup timeout |
| CQL compilation fails | Invalid CQL syntax | Validate CQL before test |
| Library not found | Wrong tenant context | Check X-Tenant-ID header |
| Cache test fails | Redis not configured | Import TestRedisConfiguration |
| Evaluation timeout | FHIR service unavailable | Mock FhirServiceClient |
| Version conflict | Duplicate library name+version | Use unique test names |

### Manual Testing (curl Examples)

```bash
# Create CQL library
curl -X POST http://localhost:8081/cql-engine/api/v1/cql/libraries \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-User-Id: admin-001" \
  -H "X-Auth-Roles: ADMIN" \
  -H "Content-Type: application/json" \
  -d '{
    "libraryName": "DiabetesScreening",
    "version": "1.0.0",
    "cqlContent": "library DiabetesScreening version '\''1.0.0'\''",
    "status": "DRAFT"
  }'

# Activate library
curl -X POST http://localhost:8081/cql-engine/api/v1/cql/libraries/{id}/activate \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-Roles: ADMIN"

# Get library versions
curl http://localhost:8081/cql-engine/api/v1/cql/libraries/by-name/DiabetesScreening/versions \
  -H "X-Tenant-ID: tenant-1"

# Create evaluation
curl -X POST http://localhost:8081/cql-engine/api/v1/cql/evaluations \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-Roles: EVALUATOR" \
  -H "Content-Type: application/json" \
  -d '{"libraryId":"lib-uuid","patientId":"patient-123"}'

# Batch evaluation (10 patients)
curl -X POST http://localhost:8081/cql-engine/api/v1/cql/evaluations/batch \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-Roles: EVALUATOR" \
  -d 'libraryId=lib-uuid&patientIds=p1,p2,p3,p4,p5,p6,p7,p8,p9,p10'

# Get evaluation by patient
curl http://localhost:8081/cql-engine/api/v1/cql/evaluations/patient/patient-123 \
  -H "X-Tenant-ID: tenant-1"

# Create value set
curl -X POST http://localhost:8081/cql-engine/api/v1/cql/valuesets \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-Roles: ADMIN" \
  -H "Content-Type: application/json" \
  -d '{
    "oid": "2.16.840.1.113883.3.464.1003.101.12.1001",
    "name": "Office Visit",
    "codeSystem": "CPT"
  }'

# Get value set by OID
curl http://localhost:8081/cql-engine/api/v1/cql/valuesets/oid/2.16.840.1.113883.3.464.1003.101.12.1001 \
  -H "X-Tenant-ID: tenant-1"
```

## Performance

- Single evaluation: 50-200ms (cached FHIR data)
- Batch evaluation: 10 patients/sec
- Redis caching: 5 min TTL (HIPAA compliant)
- WebSocket: Real-time progress for batch jobs
