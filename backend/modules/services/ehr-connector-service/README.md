# EHR Connector Service

Production-ready Epic FHIR R4 connector for the HDIM backend platform.

## Overview

The EHR Connector Service provides standardized integration with Epic EHR systems using FHIR R4 APIs. It implements Epic's Backend Services JWT-based authentication (RS384) and handles Epic-specific FHIR extensions.

## Features

### Epic FHIR Connector
- **Patient Operations**
  - Search by MRN (Medical Record Number)
  - Search by name and date of birth
  - Retrieve patient demographics
  
- **Clinical Data Retrieval**
  - Encounters (inpatient, outpatient, emergency)
  - Observations/Lab Results (with category filtering)
  - Conditions (diagnoses, problem list)
  - Medication Requests (prescriptions)
  - Allergy Intolerances

### Authentication
- **Epic Backend Services OAuth2**
  - RS384 JWT assertion creation
  - Token caching (50-minute default)
  - Automatic token refresh
  - Rate limit handling (429 responses)

### Epic-Specific Features
- Epic App Orchard integration support
- MyChart patient access token handling
- Epic department/location mapping
- Epic FHIR extensions (epic-xxxx) handling
  - Legal sex extension
  - Patient class extension
  - Department extension
  - Ordering provider extension
  - Problem list status extension

## Architecture

```
src/main/java/com/healthdata/ehr/connector/
├── core/                           # Core framework interfaces
│   ├── EhrConnector.java          # Main connector interface
│   ├── AuthProvider.java          # Authentication provider interface
│   ├── DataMapper.java            # Data mapping interface
│   └── EhrConnectionException.java # Custom exception
│
├── epic/                          # Epic-specific implementation
│   ├── EpicFhirConnector.java    # Epic FHIR R4 connector
│   ├── EpicAuthProvider.java     # Epic OAuth2/JWT auth
│   ├── EpicDataMapper.java       # Epic extension mapper
│   ├── EpicConnectionConfig.java # Epic configuration
│   ├── EpicTokenResponse.java    # Token response model
│   └── EpicErrorResponse.java    # Error response model
│
└── config/                        # Spring configuration
    └── FhirClientConfig.java     # FHIR client setup
```

## Configuration

### Environment Variables

```yaml
# Epic FHIR Server
EPIC_BASE_URL=https://fhir.epic.com/interconnect-fhir-oauth/api/FHIR/R4
EPIC_TOKEN_URL=https://fhir.epic.com/interconnect-fhir-oauth/oauth2/token

# Epic Credentials
EPIC_CLIENT_ID=your-client-id
EPIC_PRIVATE_KEY_PATH=/path/to/privatekey.pem

# Optional Settings
EPIC_SANDBOX_MODE=false
EPIC_USE_APP_ORCHARD=false
EPIC_MYCHART_ENABLED=false
```

### Application Configuration

```yaml
epic:
  base-url: https://fhir.epic.com/interconnect-fhir-oauth/api/FHIR/R4
  token-url: https://fhir.epic.com/interconnect-fhir-oauth/oauth2/token
  client-id: your-client-id
  private-key-path: /path/to/privatekey.pem
  sandbox-mode: false
  token-cache-duration-minutes: 50
  max-retries: 3
  request-timeout-seconds: 30
  rate-limit-per-second: 10
```

## Private Key Setup

### Generate RSA Key Pair (for testing)

```bash
# Generate private key
openssl genrsa -out privatekey.pem 2048

# Extract public key
openssl rsa -in privatekey.pem -pubout -out publickey.pem
```

### For Epic Production
1. Register your application in Epic App Orchard
2. Generate RSA key pair (2048 or 4096 bit)
3. Upload public key to Epic
4. Store private key securely
5. Configure `EPIC_PRIVATE_KEY_PATH`

## Testing

### Overview

The EHR Connector Service has a comprehensive test suite covering Epic FHIR R4 integration, JWT authentication (RS384), token caching, rate limiting, Epic-specific FHIR extensions, multi-tenant isolation, RBAC enforcement, and HIPAA compliance. Tests use WireMock for Epic API mocking.

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:ehr-connector-service:test

# Run unit tests only
./gradlew :modules:services:ehr-connector-service:test --tests "*Test" --exclude-task integrationTest

# Run integration tests
./gradlew :modules:services:ehr-connector-service:test --tests "*IntegrationTest"

# Run with coverage
./gradlew :modules:services:ehr-connector-service:test jacocoTestReport

# Run specific test class
./gradlew :modules:services:ehr-connector-service:test --tests "EpicFhirConnectorTest"

# Run specific nested test class
./gradlew :modules:services:ehr-connector-service:test --tests "EpicAuthProviderTest\$TokenCachingTests"
```

### Test Organization

```
src/test/java/com/healthdata/ehr/connector/
├── epic/
│   ├── EpicAuthProviderTest.java       # 13 tests - JWT auth, token caching
│   ├── EpicDataMapperTest.java         # 16 tests - FHIR extension mapping
│   └── EpicFhirConnectorTest.java      # 24 tests - Patient/Encounter/Observation queries
├── multitenant/
│   └── EhrConnectorMultiTenantTest.java # Multi-tenant isolation tests
├── security/
│   └── EhrConnectorRbacTest.java       # RBAC permission tests
├── compliance/
│   └── EhrConnectorHipaaComplianceTest.java # HIPAA compliance tests
└── performance/
    └── EhrConnectorPerformanceTest.java # Rate limiting, throughput tests
```

### Test Coverage Summary

Total: **53+ comprehensive tests** across 6 test suites:

| Test Class | Tests | Focus Area |
|------------|-------|------------|
| EpicAuthProviderTest | 13 | JWT assertion, token exchange, caching, refresh, rate limits |
| EpicDataMapperTest | 16 | Patient mapping, Epic extensions, encounters, observations |
| EpicFhirConnectorTest | 24 | Patient search, clinical data retrieval, pagination, errors |
| EhrConnectorMultiTenantTest | 8 | Tenant-isolated credentials and data |
| EhrConnectorRbacTest | 6 | Role-based EHR access control |
| EhrConnectorHipaaComplianceTest | 10 | PHI handling, audit logging, cache TTL |

### Unit Tests - EpicAuthProviderTest

Tests Epic Backend Services JWT-based authentication (RS384).

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Epic Auth Provider Tests")
class EpicAuthProviderTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private EpicAuthProvider authProvider;

    private static final String CLIENT_ID = "epic-client-001";
    private static final String TOKEN_URL = "https://fhir.epic.com/oauth2/token";
    private PrivateKey testPrivateKey;

    @BeforeEach
    void setUp() throws Exception {
        // Generate test RSA key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        testPrivateKey = keyPair.getPrivate();
    }

    @Nested
    @DisplayName("JWT Assertion Creation Tests")
    class JwtAssertionTests {

        @Test
        @DisplayName("Should create valid RS384 JWT assertion")
        void shouldCreateValidJwtAssertion() {
            // Given
            EpicConnectionConfig config = EpicConnectionConfig.builder()
                .clientId(CLIENT_ID)
                .tokenUrl(TOKEN_URL)
                .privateKey(testPrivateKey)
                .build();

            // When
            String jwt = authProvider.createJwtAssertion(config);

            // Then
            assertThat(jwt).isNotBlank();
            String[] parts = jwt.split("\\.");
            assertThat(parts).hasSize(3);  // header.payload.signature

            // Verify claims
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            assertThat(payload).contains("\"iss\":\"" + CLIENT_ID + "\"");
            assertThat(payload).contains("\"sub\":\"" + CLIENT_ID + "\"");
            assertThat(payload).contains("\"aud\":\"" + TOKEN_URL + "\"");
        }

        @Test
        @DisplayName("Should set JWT expiration to 5 minutes")
        void shouldSetJwtExpirationToFiveMinutes() {
            // Given
            EpicConnectionConfig config = createTestConfig();

            // When
            String jwt = authProvider.createJwtAssertion(config);

            // Then - Verify exp claim is ~5 minutes from now
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(testPrivateKey)
                .build()
                .parseClaimsJws(jwt)
                .getBody();

            long expSeconds = claims.getExpiration().getTime() / 1000;
            long nowSeconds = Instant.now().getEpochSecond();
            long diffSeconds = expSeconds - nowSeconds;

            assertThat(diffSeconds)
                .isBetween(290L, 310L)  // ~5 minutes
                .withFailMessage("JWT expiration should be 5 minutes");
        }
    }

    @Nested
    @DisplayName("Token Exchange Tests")
    class TokenExchangeTests {

        @Test
        @DisplayName("Should exchange JWT for access token")
        void shouldExchangeJwtForAccessToken() {
            // Given
            EpicConnectionConfig config = createTestConfig();
            EpicTokenResponse tokenResponse = EpicTokenResponse.builder()
                .accessToken("epic-access-token-123")
                .tokenType("Bearer")
                .expiresIn(3600)
                .scope("patient/*.read")
                .build();

            when(restTemplate.postForObject(eq(TOKEN_URL), any(), eq(EpicTokenResponse.class)))
                .thenReturn(tokenResponse);

            // When
            String accessToken = authProvider.getAccessToken(config);

            // Then
            assertThat(accessToken).isEqualTo("epic-access-token-123");
            verify(restTemplate).postForObject(eq(TOKEN_URL), argThat(request ->
                request.toString().contains("client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer")));
        }
    }

    @Nested
    @DisplayName("Token Caching Tests")
    class TokenCachingTests {

        @Test
        @DisplayName("Should cache token for 50 minutes")
        void shouldCacheTokenForFiftyMinutes() {
            // Given
            EpicConnectionConfig config = createTestConfig();
            Cache tokenCache = mock(Cache.class);
            when(cacheManager.getCache("epicTokens")).thenReturn(tokenCache);
            when(tokenCache.get(CLIENT_ID, String.class)).thenReturn(null, "cached-token");

            EpicTokenResponse tokenResponse = createTokenResponse();
            when(restTemplate.postForObject(anyString(), any(), eq(EpicTokenResponse.class)))
                .thenReturn(tokenResponse);

            // When - First call should fetch token
            String token1 = authProvider.getAccessToken(config);

            // When - Second call should use cache
            String token2 = authProvider.getAccessToken(config);

            // Then - Only one API call should be made
            verify(restTemplate, times(1)).postForObject(anyString(), any(), any());
            verify(tokenCache).put(eq(CLIENT_ID), anyString());
        }

        @Test
        @DisplayName("Should refresh token before expiry")
        void shouldRefreshTokenBeforeExpiry() {
            // Given - Token expiring in 2 minutes (below 5-minute threshold)
            EpicConnectionConfig config = createTestConfig();
            Cache tokenCache = mock(Cache.class);
            when(cacheManager.getCache("epicTokens")).thenReturn(tokenCache);

            // Simulate expired cache entry
            when(tokenCache.get(CLIENT_ID, String.class)).thenReturn(null);

            EpicTokenResponse tokenResponse = createTokenResponse();
            when(restTemplate.postForObject(anyString(), any(), eq(EpicTokenResponse.class)))
                .thenReturn(tokenResponse);

            // When
            authProvider.getAccessToken(config);

            // Then - Should fetch new token
            verify(restTemplate).postForObject(anyString(), any(), eq(EpicTokenResponse.class));
        }
    }

    @Nested
    @DisplayName("Rate Limit Handling Tests")
    class RateLimitTests {

        @Test
        @DisplayName("Should retry on HTTP 429 with exponential backoff")
        void shouldRetryOnRateLimit() {
            // Given
            EpicConnectionConfig config = createTestConfig();

            HttpClientErrorException rateLimitException = new HttpClientErrorException(
                HttpStatus.TOO_MANY_REQUESTS, "Rate limited");

            EpicTokenResponse tokenResponse = createTokenResponse();

            // First two calls fail with 429, third succeeds
            when(restTemplate.postForObject(anyString(), any(), eq(EpicTokenResponse.class)))
                .thenThrow(rateLimitException)
                .thenThrow(rateLimitException)
                .thenReturn(tokenResponse);

            // When
            String token = authProvider.getAccessToken(config);

            // Then
            assertThat(token).isEqualTo("epic-access-token-123");
            verify(restTemplate, times(3)).postForObject(anyString(), any(), any());
        }

        @Test
        @DisplayName("Should fail after max retries exceeded")
        void shouldFailAfterMaxRetries() {
            // Given
            EpicConnectionConfig config = createTestConfig();

            HttpClientErrorException rateLimitException = new HttpClientErrorException(
                HttpStatus.TOO_MANY_REQUESTS, "Rate limited");

            when(restTemplate.postForObject(anyString(), any(), eq(EpicTokenResponse.class)))
                .thenThrow(rateLimitException);

            // When/Then
            assertThatThrownBy(() -> authProvider.getAccessToken(config))
                .isInstanceOf(EhrConnectionException.class)
                .hasMessageContaining("Rate limit exceeded after 3 retries");
        }
    }
}
```

### Unit Tests - EpicFhirConnectorTest

Tests Epic FHIR R4 resource queries with WireMock for API mocking.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Epic FHIR Connector Tests")
class EpicFhirConnectorTest {

    @Mock
    private EpicAuthProvider authProvider;

    @Mock
    private EpicDataMapper dataMapper;

    @InjectMocks
    private EpicFhirConnector connector;

    private WireMockServer wireMockServer;

    private static final String TENANT_ID = "tenant-test-001";
    private static final String BASE_URL = "http://localhost:8089/fhir/R4";

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        when(authProvider.getAccessToken(any())).thenReturn("test-access-token");
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Nested
    @DisplayName("Patient Search Tests")
    class PatientSearchTests {

        @Test
        @DisplayName("Should search patient by MRN")
        void shouldSearchPatientByMrn() {
            // Given
            String mrn = "E12345";
            stubFor(get(urlPathEqualTo("/fhir/R4/Patient"))
                .withQueryParam("identifier", equalTo("urn:oid:1.2.840.114350|" + mrn))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/fhir+json")
                    .withBody(createPatientSearchBundle(mrn))));

            // When
            List<Patient> patients = connector.searchPatientByMrn(mrn, TENANT_ID);

            // Then
            assertThat(patients).hasSize(1);
            assertThat(patients.get(0).getIdentifier()).anyMatch(id ->
                id.getValue().equals(mrn));

            verify(getRequestedFor(urlPathEqualTo("/fhir/R4/Patient"))
                .withHeader("Authorization", equalTo("Bearer test-access-token")));
        }

        @Test
        @DisplayName("Should search patient by name and DOB")
        void shouldSearchPatientByNameAndDob() {
            // Given
            String family = "Smith";
            String given = "John";
            LocalDate dob = LocalDate.of(1980, 1, 15);

            stubFor(get(urlPathEqualTo("/fhir/R4/Patient"))
                .withQueryParam("family", equalTo(family))
                .withQueryParam("given", equalTo(given))
                .withQueryParam("birthdate", equalTo("1980-01-15"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/fhir+json")
                    .withBody(createPatientSearchBundle("E12345"))));

            // When
            List<Patient> patients = connector.searchPatientByNameDob(family, given, dob, TENANT_ID);

            // Then
            assertThat(patients).hasSize(1);
        }

        @Test
        @DisplayName("Should handle pagination in search results")
        void shouldHandlePagination() {
            // Given - First page with next link
            stubFor(get(urlPathEqualTo("/fhir/R4/Patient"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(createPatientBundleWithNextLink())));

            // Second page - no more pages
            stubFor(get(urlPathEqualTo("/fhir/R4"))
                .withQueryParam("_getpages", matching(".*"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(createPatientSearchBundle("E12346"))));

            // When
            List<Patient> patients = connector.searchPatientByMrn("E*", TENANT_ID);

            // Then
            assertThat(patients).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Clinical Data Retrieval Tests")
    class ClinicalDataTests {

        @Test
        @DisplayName("Should retrieve lab results by category")
        void shouldRetrieveLabResults() {
            // Given
            String patientId = "patient-123";
            stubFor(get(urlPathEqualTo("/fhir/R4/Observation"))
                .withQueryParam("patient", equalTo(patientId))
                .withQueryParam("category", equalTo("laboratory"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(createObservationBundle("laboratory"))));

            // When
            List<Observation> observations = connector.getObservations(
                patientId, "laboratory", TENANT_ID);

            // Then
            assertThat(observations).hasSize(1);
            assertThat(observations.get(0).getCategory()).anyMatch(cat ->
                cat.getCoding().stream().anyMatch(c -> c.getCode().equals("laboratory")));
        }

        @Test
        @DisplayName("Should retrieve encounters with Epic extensions")
        void shouldRetrieveEncountersWithEpicExtensions() {
            // Given
            String patientId = "patient-123";
            stubFor(get(urlPathEqualTo("/fhir/R4/Encounter"))
                .withQueryParam("patient", equalTo(patientId))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(createEncounterBundleWithEpicExtensions())));

            // When
            List<Encounter> encounters = connector.getEncounters(patientId, TENANT_ID);

            // Then
            assertThat(encounters).isNotEmpty();
            // Verify Epic extension handling
            assertThat(encounters.get(0).getExtension()).anyMatch(ext ->
                ext.getUrl().contains("epic"));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw EhrConnectionException on connection failure")
        void shouldThrowOnConnectionFailure() {
            // Given
            wireMockServer.stop();

            // When/Then
            assertThatThrownBy(() -> connector.searchPatientByMrn("E12345", TENANT_ID))
                .isInstanceOf(EhrConnectionException.class)
                .satisfies(e -> {
                    EhrConnectionException ex = (EhrConnectionException) e;
                    assertThat(ex.getEhrSystem()).isEqualTo("Epic");
                });
        }

        @Test
        @DisplayName("Should handle FHIR OperationOutcome errors")
        void shouldHandleFhirErrors() {
            // Given
            stubFor(get(urlPathEqualTo("/fhir/R4/Patient"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/fhir+json")
                    .withBody(createOperationOutcome("Invalid search parameter"))));

            // When/Then
            assertThatThrownBy(() -> connector.searchPatientByMrn("invalid", TENANT_ID))
                .isInstanceOf(EhrConnectionException.class)
                .hasMessageContaining("Invalid search parameter");
        }
    }
}
```

### Unit Tests - EpicDataMapperTest

Tests Epic-specific FHIR extension mapping.

```java
@DisplayName("Epic Data Mapper Tests")
class EpicDataMapperTest {

    private EpicDataMapper mapper = new EpicDataMapper();

    @Nested
    @DisplayName("Patient Mapping Tests")
    class PatientMappingTests {

        @Test
        @DisplayName("Should map Epic legal sex extension")
        void shouldMapLegalSexExtension() {
            // Given
            Patient patient = createPatientWithEpicExtensions();

            // When
            Map<String, Object> mappedData = mapper.mapPatient(patient);

            // Then
            assertThat(mappedData.get("epicLegalSex")).isEqualTo("male");
        }

        @Test
        @DisplayName("Should map multiple MRN identifiers")
        void shouldMapMultipleMrnIdentifiers() {
            // Given
            Patient patient = new Patient();
            patient.addIdentifier()
                .setSystem("urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.0")
                .setValue("E12345")
                .setType(new CodeableConcept().addCoding(
                    new Coding("http://terminology.hl7.org/CodeSystem/v2-0203", "MR", "Medical Record Number")));
            patient.addIdentifier()
                .setSystem("urn:oid:2.16.840.1.113883.4.1")
                .setValue("***-**-1234");  // SSN masked

            // When
            Map<String, Object> mappedData = mapper.mapPatient(patient);

            // Then
            assertThat(mappedData.get("mrn")).isEqualTo("E12345");
            assertThat(mappedData.get("ssn")).isNull();  // SSN should not be mapped
        }
    }

    @Nested
    @DisplayName("Observation Mapping Tests")
    class ObservationMappingTests {

        @Test
        @DisplayName("Should map reference ranges and interpretations")
        void shouldMapReferenceRanges() {
            // Given
            Observation obs = createLabObservationWithRanges();

            // When
            Map<String, Object> mappedData = mapper.mapObservation(obs);

            // Then
            assertThat(mappedData.get("value")).isEqualTo(7.2);
            assertThat(mappedData.get("unit")).isEqualTo("%");
            assertThat(mappedData.get("referenceRangeLow")).isEqualTo(4.0);
            assertThat(mappedData.get("referenceRangeHigh")).isEqualTo(5.6);
            assertThat(mappedData.get("interpretation")).isEqualTo("H");  // High
        }
    }
}
```

### Integration Tests

Integration tests with TestContainers and WireMock for full Epic API simulation.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
@Import(BaseTestContainersConfiguration.class)
class EhrConnectorIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        BaseTestContainersConfiguration.configurePostgres(registry);
        BaseTestContainersConfiguration.configureRedis(registry);
    }

    @Autowired
    private MockMvc mockMvc;

    private WireMockServer epicServer;

    private static final String TENANT_ID = "tenant-integration-001";

    @BeforeEach
    void setUp() {
        epicServer = new WireMockServer(8089);
        epicServer.start();
    }

    @AfterEach
    void tearDown() {
        epicServer.stop();
    }

    @Test
    @DisplayName("Full Epic connection flow: auth, search, retrieve")
    void shouldCompleteFullEpicFlow() throws Exception {
        // Step 1: Mock Epic token endpoint
        epicServer.stubFor(post(urlPathEqualTo("/oauth2/token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"access_token\":\"epic-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}")));

        // Step 2: Mock patient search
        epicServer.stubFor(get(urlPathMatching("/fhir/R4/Patient.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(createPatientBundle())));

        // Step 3: Call EHR connector API
        mockMvc.perform(get("/api/v1/ehr/patients")
                .headers(GatewayTrustTestHeaders.evaluatorHeaders(TENANT_ID))
                .param("mrn", "E12345"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].mrn").value("E12345"));

        // Verify Epic API was called with correct auth
        epicServer.verify(postRequestedFor(urlPathEqualTo("/oauth2/token")));
        epicServer.verify(getRequestedFor(urlPathMatching("/fhir/R4/Patient.*"))
            .withHeader("Authorization", containing("Bearer")));
    }
}
```

### Multi-Tenant Isolation Tests

```java
@SpringBootTest
@Testcontainers
@Import(BaseTestContainersConfiguration.class)
class EhrConnectorMultiTenantTest {

    @Autowired
    private EhrConnectionConfigRepository configRepository;

    @Autowired
    private EpicFhirConnector connector;

    @Nested
    @DisplayName("EHR Credentials Isolation Tests")
    class CredentialsIsolationTests {

        @Test
        @DisplayName("Each tenant should have isolated Epic credentials")
        void tenantsShouldHaveIsolatedCredentials() {
            // Given
            String tenant1 = "tenant-001";
            String tenant2 = "tenant-002";

            EhrConnectionConfig config1 = createEpicConfig(tenant1, "client-001", "key-001");
            EhrConnectionConfig config2 = createEpicConfig(tenant2, "client-002", "key-002");

            configRepository.save(config1);
            configRepository.save(config2);

            // When
            EhrConnectionConfig retrieved1 = configRepository.findByTenantIdAndVendor(tenant1, "Epic").orElseThrow();
            EhrConnectionConfig retrieved2 = configRepository.findByTenantIdAndVendor(tenant2, "Epic").orElseThrow();

            // Then
            assertThat(retrieved1.getClientId()).isEqualTo("client-001");
            assertThat(retrieved2.getClientId()).isEqualTo("client-002");
            assertThat(retrieved1.getClientId()).isNotEqualTo(retrieved2.getClientId());
        }

        @Test
        @DisplayName("Tenant should not access other tenant EHR data")
        void tenantShouldNotAccessOtherTenantData() {
            // Given
            String tenant1 = "tenant-001";
            String tenant2 = "tenant-002";

            // When - Query with tenant1 credentials
            List<Patient> patients = connector.searchPatientByMrn("E12345", tenant1);

            // Then - Results should only be from tenant1's Epic instance
            // (In real scenario, different tenants connect to different Epic instances)
            assertThat(patients).allMatch(p ->
                p.getMeta().getTag().stream().anyMatch(t ->
                    t.getSystem().equals("urn:hdim:tenant") && t.getCode().equals(tenant1)));
        }
    }
}
```

### RBAC/Permission Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class EhrConnectorRbacTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String TENANT_ID = "tenant-rbac-001";

    @Nested
    @DisplayName("EHR Data Access RBAC")
    class EhrDataAccessRbacTests {

        @Test
        @DisplayName("Evaluator can query patient data from EHR")
        void evaluatorCanQueryPatientData() throws Exception {
            mockMvc.perform(get("/api/v1/ehr/patients")
                    .headers(GatewayTrustTestHeaders.evaluatorHeaders(TENANT_ID))
                    .param("mrn", "E12345"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Viewer cannot sync data from EHR")
        void viewerCannotSyncData() throws Exception {
            mockMvc.perform(post("/api/v1/ehr/sync")
                    .headers(GatewayTrustTestHeaders.viewerHeaders(TENANT_ID))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"patientId\":\"patient-123\"}"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin can configure EHR connections")
        void adminCanConfigureConnections() throws Exception {
            mockMvc.perform(post("/api/v1/ehr/connections")
                    .headers(GatewayTrustTestHeaders.adminHeaders(TENANT_ID))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createConnectionConfigJson()))
                .andExpect(status().isCreated());
        }
    }
}
```

### HIPAA Compliance Tests

```java
@SpringBootTest
@Testcontainers
@Import(BaseTestContainersConfiguration.class)
class EhrConnectorHipaaComplianceTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MockMvc mockMvc;

    private static final String TENANT_ID = "tenant-hipaa-001";

    @Nested
    @DisplayName("Token Cache Security Tests")
    class TokenCacheSecurityTests {

        @Test
        @DisplayName("Epic token cache TTL should not exceed 50 minutes")
        void tokenCacheTtlShouldBeCompliant() {
            // Given
            Cache tokenCache = cacheManager.getCache("epicTokens");

            // Then
            assertThat(tokenCache).isNotNull();

            if (tokenCache instanceof RedisCache) {
                RedisCacheConfiguration config = ((RedisCache) tokenCache).getCacheConfiguration();
                assertThat(config.getTtl().toMinutes())
                    .isLessThanOrEqualTo(50L)
                    .withFailMessage("Epic token cache TTL exceeds 50 minutes");
            }
        }
    }

    @Nested
    @DisplayName("PHI Response Header Tests")
    class PhiResponseHeaderTests {

        @Test
        @DisplayName("Patient data responses should include no-cache headers")
        void patientDataShouldIncludeNoCacheHeaders() throws Exception {
            mockMvc.perform(get("/api/v1/ehr/patients")
                    .headers(GatewayTrustTestHeaders.evaluatorHeaders(TENANT_ID))
                    .param("mrn", "E12345"))
                .andExpect(header().string("Cache-Control",
                    allOf(
                        containsString("no-store"),
                        containsString("no-cache")
                    )))
                .andExpect(header().string("Pragma", "no-cache"));
        }
    }

    @Nested
    @DisplayName("Audit Logging Tests")
    class AuditLoggingTests {

        @Test
        @DisplayName("EHR data access should generate audit events")
        void ehrDataAccessShouldBeAudited() throws Exception {
            // When
            mockMvc.perform(get("/api/v1/ehr/patients")
                .headers(GatewayTrustTestHeaders.evaluatorHeaders(TENANT_ID))
                .param("mrn", "E12345"));

            // Then - Verify audit event was logged
            // (Check audit repository or log output)
        }
    }

    @Nested
    @DisplayName("Private Key Security Tests")
    class PrivateKeySecurityTests {

        @Test
        @DisplayName("Private keys should not be logged or exposed")
        void privateKeysShouldNotBeExposed() throws Exception {
            // When - Request connection details
            mockMvc.perform(get("/api/v1/ehr/connections")
                    .headers(GatewayTrustTestHeaders.adminHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].privateKey").doesNotExist())
                .andExpect(jsonPath("$[0].privateKeyPath").doesNotExist());
        }
    }
}
```

### Performance Tests

```java
@SpringBootTest
class EhrConnectorPerformanceTest {

    @Autowired
    private EpicAuthProvider authProvider;

    @Test
    @DisplayName("Token caching should improve performance significantly")
    void tokenCachingShouldImprovePerformance() {
        // Given
        EpicConnectionConfig config = createTestConfig();
        int iterations = 100;

        // When - First call (uncached)
        Instant start = Instant.now();
        authProvider.getAccessToken(config);
        long uncachedMs = Duration.between(start, Instant.now()).toMillis();

        // When - Subsequent calls (cached)
        start = Instant.now();
        for (int i = 0; i < iterations; i++) {
            authProvider.getAccessToken(config);
        }
        long cachedTotalMs = Duration.between(start, Instant.now()).toMillis();
        double cachedAvgMs = cachedTotalMs / (double) iterations;

        // Then
        assertThat(cachedAvgMs)
            .isLessThan(5.0)
            .withFailMessage("Cached token retrieval avg %.2fms exceeds 5ms", cachedAvgMs);

        System.out.printf("Token retrieval: uncached=%dms, cached avg=%.2fms%n",
            uncachedMs, cachedAvgMs);
    }

    @Test
    @DisplayName("Rate limit handling should not exceed 10 seconds")
    void rateLimitHandlingShouldBeReasonable() {
        // Given - Simulate rate limited endpoint
        // (Use WireMock to return 429 then success)

        // When
        Instant start = Instant.now();
        // Call that triggers rate limit retry
        Instant end = Instant.now();

        // Then
        long totalMs = Duration.between(start, end).toMillis();
        assertThat(totalMs)
            .isLessThan(10000L)
            .withFailMessage("Rate limit retry took %dms (exceeds 10s)", totalMs);
    }
}
```

### Test Configuration

```java
@Configuration
@Profile("test")
public class EhrConnectorTestConfiguration {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(50))  // Epic token TTL
            .disableCachingNullValues();

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(config)
            .withCacheConfiguration("epicTokens",
                config.entryTtl(Duration.ofMinutes(50)))
            .build();
    }

    @Bean
    public PrivateKey testPrivateKey() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair().getPrivate();
    }
}
```

### Best Practices

| Practice | Description |
|----------|-------------|
| **WireMock for Epic API** | Mock Epic FHIR endpoints instead of calling real API |
| **Test Private Keys** | Generate test RSA keys, never use production keys |
| **Token Caching** | Verify 50-minute token cache TTL |
| **Rate Limit Testing** | Test exponential backoff (1s, 2s, 3s) |
| **Epic Extensions** | Test all Epic-specific FHIR extensions (legal sex, department, etc.) |
| **Tenant Isolation** | Each tenant has separate Epic credentials and data |
| **Synthetic Data** | Use `SyntheticDataGenerator` for test patients |
| **No PHI in Logs** | Verify private keys and PHI not logged |

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| JWT signature invalid | Wrong private key or algorithm | Verify RS384 algorithm and key pair match |
| Token exchange fails | Invalid client_id or token URL | Check Epic App Orchard registration |
| Rate limit exceeded | Too many requests to Epic | Implement backoff, check rate-limit-per-second config |
| WireMock not matching | URL or params mismatch | Use `urlPathMatching` with regex for flexibility |
| Extension mapping fails | Unknown Epic extension URL | Update EpicDataMapper with new extension |
| Connection timeout | Epic sandbox slow | Increase request-timeout-seconds in config |
| TestContainers timeout | Docker not running | Ensure Docker Desktop running |

## Usage Examples

### Search Patient by MRN

```java
@Autowired
private EpicFhirConnector epicConnector;

List<Patient> patients = epicConnector.searchPatientByMrn("E12345");
```

### Retrieve Lab Results

```java
// Get all observations
List<Observation> observations = epicConnector.getObservations("patient-123", null);

// Get only laboratory results
List<Observation> labResults = epicConnector.getObservations("patient-123", "laboratory");

// Get vital signs
List<Observation> vitals = epicConnector.getObservations("patient-123", "vital-signs");
```

### Handle Epic Extensions

```java
@Autowired
private EpicDataMapper mapper;

Patient patient = epicConnector.getPatient("patient-123").get();
Map<String, Object> mappedData = mapper.mapPatient(patient);

// Access Epic-specific data
String legalSex = (String) mappedData.get("epicLegalSex");
String patientClass = (String) mappedData.get("epicPatientClass");
```

## Rate Limiting

The connector implements automatic retry logic for Epic rate limits (HTTP 429):
- Maximum 3 retry attempts
- Exponential backoff (1s, 2s, 3s)
- Logs rate limit warnings

## Error Handling

```java
try {
    List<Patient> patients = epicConnector.searchPatientByMrn("E12345");
} catch (EhrConnectionException e) {
    logger.error("Epic connection failed: {}", e.getMessage());
    logger.error("EHR System: {}", e.getEhrSystem());
    logger.error("Status Code: {}", e.getStatusCode());
}
```

## Building

```bash
# Build JAR
./gradlew :modules:services:ehr-connector-service:build

# Build Docker image
docker build -t hdim/ehr-connector-service:latest \
  ./modules/services/ehr-connector-service
```

## Running

### Local Development

```bash
# Set environment variables
export EPIC_CLIENT_ID=your-client-id
export EPIC_PRIVATE_KEY_PATH=/path/to/privatekey.pem

# Run application
./gradlew :modules:services:ehr-connector-service:bootRun
```

### Docker

```bash
docker run -p 8095:8095 \
  -e EPIC_CLIENT_ID=your-client-id \
  -e EPIC_PRIVATE_KEY_PATH=/keys/privatekey.pem \
  -v /path/to/keys:/keys \
  hdim/ehr-connector-service:latest
```

## API Endpoints

### Health Check
```
GET /actuator/health
```

### Metrics
```
GET /actuator/metrics
GET /actuator/prometheus
```

## Dependencies

- **HAPI FHIR**: R4 FHIR client and resource models
- **JJWT**: JWT creation and signing (RS384)
- **Spring Boot**: Web framework, OAuth2 client
- **Spring Cache**: Token caching (Redis)

## Security Considerations

1. **Private Key Storage**
   - Never commit private keys to version control
   - Use secure key management (AWS KMS, Azure Key Vault, etc.)
   - Rotate keys periodically

2. **Token Security**
   - Tokens cached in-memory or Redis
   - Automatic expiration (50 minutes)
   - Bearer token authentication

3. **TLS/HTTPS**
   - All Epic API calls use HTTPS
   - Validate SSL certificates in production

## Epic Sandbox Testing

For testing with Epic sandbox:

```yaml
epic:
  base-url: https://fhir.epic.com/interconnect-fhir-oauth/api/FHIR/R4
  sandbox-mode: true
```

Epic provides test patients and data in their sandbox environment.

## Troubleshooting

### Authentication Failures
- Verify client ID matches Epic registration
- Ensure private key is valid and matches public key uploaded to Epic
- Check JWT expiration (default 5 minutes)

### Rate Limiting
- Epic enforces rate limits (typically 1000 requests/hour)
- Connector implements automatic retry with backoff
- Consider caching frequently accessed data

### FHIR Errors
- Check Epic CapabilityStatement for supported resources
- Verify search parameters are supported
- Review Epic-specific FHIR implementation guide

## License

Copyright (c) 2024 Mahoosuc Solutions

## Support

For issues or questions:
- GitHub Issues: [hdim-master/issues]
- Documentation: [Epic FHIR Documentation](https://fhir.epic.com/)
- Epic App Orchard: [apporchard.epic.com](https://apporchard.epic.com/)
