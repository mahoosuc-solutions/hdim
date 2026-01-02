# Phase 1: CMS Integration Foundation (Weeks 1-4)

**Status**: IN PROGRESS  
**Duration**: 4 weeks  
**Goal**: Establish OAuth2 authentication and validate HDIM-CMS compatibility  
**Team**: 2 backend engineers, 1 architect, 1 QA engineer

---

## Overview

Phase 1 focuses on establishing the technical foundation for CMS API integration. By the end of this phase, we'll have:
- ✅ OAuth2 authentication flow working
- ✅ CMS Connector Service scaffolded
- ✅ FHIR data models defined
- ✅ Sandbox test data validated
- ✅ Architecture review completed

---

## Week 1: CMS API Setup & Architecture Design

### Week 1 Goals
- [ ] Complete CMS API registration and credential acquisition
- [ ] Design OAuth2 flow and security architecture
- [ ] Create FHIR resource mapping documentation
- [ ] Set up development environment and repositories

### Week 1 Deliverables

#### 1.1 CMS API Credential Acquisition

**Task**: Register HDIM as CMS application and obtain sandbox access

**Instructions**:
1. **Register for each CMS API**:
   - BCDA: https://bcda.cms.gov/developers/
   - DPC: https://dpc.cms.gov/
   - Blue Button: https://bluebutton.cms.gov/developers/
   - (Optional) AB2D: https://ab2d.cms.gov/

2. **Documentation to collect**:
   - [ ] Client ID for each API
   - [ ] Client Secret (store in HashiCorp Vault, never in code)
   - [ ] Sandbox endpoints URL
   - [ ] API documentation links
   - [ ] Technical contact information
   - [ ] Rate limits and SLA details
   - [ ] Webhook endpoints (if supported)

3. **Testing credentials**:
   - [ ] Test beneficiary IDs for sandbox
   - [ ] Test claim data samples
   - [ ] Environment variables setup template

**Owner**: Architect  
**Timeline**: Days 1-2  
**Success Criteria**:
- [ ] Credentials obtained for 2+ CMS APIs (BCDA + DPC minimum)
- [ ] Documented in secure location (Vault)
- [ ] All APIs accessible via sandbox endpoints
- [ ] Test API calls successful

---

#### 1.2 OAuth2 Architecture Design

**Task**: Design OAuth2 flow for HDIM-CMS integration

**OAuth2 Flow (Client Credentials)**:
```
HDIM Application
    ↓ 1. Request token with client_id + client_secret
CMS OAuth2 Provider
    ↓ 2. Validate credentials
    ↓ 3. Return access_token + expires_in
HDIM Stores Token
    ↓ 4. Use token for API requests
CMS Validates Token
    ↓ 5. Return claims data
```

**Implementation Approach**:
```
┌─────────────────────────────────────┐
│  CMS Connector Service              │
├─────────────────────────────────────┤
│  OAuth2Manager                      │
│  ├─ requestToken()                  │
│  ├─ refreshToken()                  │
│  ├─ isTokenExpired()               │
│  └─ storeToken()                    │
│                                     │
│  CMS API Client                     │
│  ├─ BcdaClient                      │
│  ├─ DpcClient                       │
│  ├─ BlueButtonClient                │
│  └─ RequestExecutor (with token)    │
│                                     │
│  Error Handling                     │
│  ├─ TokenExpiredHandler             │
│  ├─ RetryPolicy                     │
│  └─ CircuitBreaker                  │
└─────────────────────────────────────┘
```

**Token Storage Strategy**:
- **Production**: HashiCorp Vault (encrypted)
- **Development**: Environment variables (for testing only)
- **Refresh Logic**: Background job refreshes 5 minutes before expiry
- **Fallback**: If refresh fails, queue requests until token available

**Design Document**:
Create `cms-connector/docs/OAUTH2-DESIGN.md` with:
- [ ] Detailed OAuth2 flow diagram
- [ ] Token lifecycle management
- [ ] Error handling for token expiry
- [ ] Retry strategy
- [ ] Security considerations

**Owner**: Architect  
**Timeline**: Days 2-3  
**Success Criteria**:
- [ ] OAuth2 flow documented
- [ ] Token management strategy defined
- [ ] Error handling approach agreed
- [ ] Review approved by architecture

---

#### 1.3 FHIR Data Model Mapping

**Task**: Define how CMS claims map to HDIM FHIR resources

**CMS → FHIR Mapping (ExplanationOfBenefit)**:

The primary CMS claims structure is the **ExplanationOfBenefit (EOB)** resource, which includes:
- **Bundle**: Collection of related resources
- **ExplanationOfBenefit**: Main claims document (created, updated dates)
- **Patient**: Beneficiary demographics (name, DOB, ID)
- **Condition**: Diagnosis codes (ICD-10)
- **Procedure**: Procedure/service codes (CPT, HCPCS)
- **Observation**: Lab results, vital signs
- **MedicationRequest/Statement**: Medications prescribed/used
- **Practitioner**: Provider information (NPI)
- **Organization**: Facility information

**HDIM Normalization**:
```
CMS FHIR Bundle
    ├─ ExplanationOfBenefit
    │  ├─ id: claim_id (unique key)
    │  ├─ created: claim_date
    │  ├─ patient: reference to Patient
    │  └─ contains: diagnoses, procedures, medications
    ├─ Patient (from reference)
    │  ├─ id: beneficiary_id (MBI - Medicare Beneficiary ID)
    │  ├─ identifier: SSN + MBI (encrypted)
    │  └─ demographics: DOB, gender, address
    ├─ Condition resources
    │  ├─ code: ICD-10 code
    │  ├─ onsetDate: diagnosis date
    │  └─ verificationStatus: confirmed/unconfirmed
    └─ Other resources
       ├─ Procedure: CPT/HCPCS codes
       ├─ Observation: Lab values (LOINC codes)
       └─ MedicationRequest: Prescriptions
```

**Data Model Design Document**:
Create `cms-connector/docs/FHIR-MAPPING.md` with:

**Section 1: Entity Mapping**
```
CMS Element          → FHIR Resource      HDIM Storage
─────────────────────────────────────────────────────
Claim                ExplanationOfBenefit cms_claims.fhir_resource
Beneficiary          Patient              patients.external_id (encrypted)
Diagnosis Code       Condition            conditions (via CQL)
Service Code         Procedure            procedures (via CQL)
Lab Result           Observation          observations (via CQL)
Medication           MedicationRequest    medications (via CQL)
Provider             Practitioner         practitioners
Facility             Organization         organizations
```

**Section 2: Field Mapping (ExplanationOfBenefit)**
```
CMS Field                    FHIR Property              HDIM Usage
──────────────────────────────────────────────────────────────────
claim_id                     ExplanationOfBenefit.id    Deduplication key
benefit_claim_id             identifier                 Duplicate detection
claim_created_date           created                    Import timestamp
service_from_date            billablePeriod.start       Query filter
service_to_date              billablePeriod.end         Query filter
member_id                    patient.reference          Patient lookup
provider_npi                 provider.reference         Provider lookup
primary_diag_code            diagnosis[0].code          HEDIS condition check
quantity                     item.quantity              Service volume
billed_amount                item.net.value             Cost analysis
allowed_amount                benefit.value             Reimbursement tracking
```

**Section 3: Special Handling**
- **PII Encryption**: SSN, MBI stored encrypted in field-level encryption
- **Date Handling**: All dates as FHIR date format (YYYY-MM-DD)
- **Code Systems**: ICD-10 (diagnoses), CPT (procedures), LOINC (observations)
- **Nullability**: Handle missing/null fields gracefully

**Owner**: Backend engineer  
**Timeline**: Days 3-4  
**Success Criteria**:
- [ ] CMS → FHIR mapping documented
- [ ] Field mapping complete for all CMS claim elements
- [ ] Data type conversions defined
- [ ] Example mappings shown
- [ ] Review approved

---

#### 1.4 Development Environment Setup

**Task**: Set up local development environment and repositories

**Repository Structure**:
```
hdim-master/
├── backend/
│   └── modules/
│       └── services/
│           └── cms-connector-service/      ← NEW SERVICE
│               ├── src/
│               │   ├── main/
│               │   │   ├── java/
│               │   │   │   └── com/healthdata/cmsconnector/
│               │   │   │       ├── api/               (REST controllers)
│               │   │   │       ├── application/       (services)
│               │   │   │       ├── domain/            (models)
│               │   │   │       ├── infrastructure/    (CMS client)
│               │   │   │       └── config/
│               │   │   └── resources/
│               │   │       ├── application.yml
│               │   │       ├── application-dev.yml
│               │   │       └── logback-spring.xml
│               │   └── test/
│               │       └── java/...
│               ├── docs/
│               │   ├── OAUTH2-DESIGN.md
│               │   ├── FHIR-MAPPING.md
│               │   ├── API-SPECIFICATION.md
│               │   └── ARCHITECTURE.md
│               ├── build.gradle.kts        (Gradle config)
│               └── README.md
```

**Initial Project Setup**:

1. **Create Gradle Module**:
```bash
cd hdim-master/backend/modules/services
mkdir cms-connector-service
cd cms-connector-service
```

2. **Create build.gradle.kts**:
```kotlin
plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.spring") version "1.9.0"
}

group = "com.healthdata"
version = "1.0.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-security")
    
    // FHIR
    implementation("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:7.0.0")
    
    // HTTP Client
    implementation("org.apache.httpcomponents.client5:httpclient5:5.3")
    
    // JSON
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
}
```

3. **Create Project Structure**:
```bash
mkdir -p src/main/java/com/healthdata/cmsconnector/{api,application,domain,infrastructure,config}
mkdir -p src/main/resources
mkdir -p src/test/java/com/healthdata/cmsconnector
mkdir -p docs
```

4. **Create application.yml**:
```yaml
spring:
  application:
    name: cms-connector-service
  oauth2:
    client:
      registration:
        bcda:
          client-id: ${CMS_BCDA_CLIENT_ID}
          client-secret: ${CMS_BCDA_CLIENT_SECRET}
          authorization-grant-type: client_credentials
          provider: bcda
        dpc:
          client-id: ${CMS_DPC_CLIENT_ID}
          client-secret: ${CMS_DPC_CLIENT_SECRET}
          authorization-grant-type: client_credentials
          provider: dpc
      provider:
        bcda:
          token-uri: ${CMS_BCDA_TOKEN_URI:https://bcda.cms.gov/auth/token}
        dpc:
          token-uri: ${CMS_DPC_TOKEN_URI:https://dpc.cms.gov/auth/token}

server:
  port: 8089
  servlet:
    context-path: /cms-connector

logging:
  level:
    root: INFO
    com.healthdata: DEBUG
```

**Owner**: Backend engineer  
**Timeline**: Days 1-4  
**Success Criteria**:
- [ ] Gradle module created and builds successfully
- [ ] Project structure matches standards
- [ ] application.yml configured
- [ ] Build passes tests
- [ ] IDE (IntelliJ/Eclipse) recognizes module

---

### Week 1 Completion Checklist
- [ ] CMS API credentials obtained (BCDA, DPC)
- [ ] Credentials stored in Vault with documentation
- [ ] OAuth2 design document completed and reviewed
- [ ] FHIR mapping document completed and reviewed
- [ ] CMS Connector Service project created
- [ ] Gradle build successful
- [ ] Initial code scaffolding complete
- [ ] Architecture review passed

---

## Week 2: OAuth2 Implementation & Token Management

### Week 2 Goals
- [ ] Implement OAuth2Manager for token lifecycle
- [ ] Create CMS API client wrappers
- [ ] Build token refresh background job
- [ ] Write unit tests for OAuth2 flow

### Week 2 Deliverables

#### 2.1 OAuth2Manager Implementation

**File**: `src/main/java/com/healthdata/cmsconnector/infrastructure/OAuth2Manager.java`

```java
@Component
@Slf4j
public class OAuth2Manager {

    private final RestTemplate restTemplate;
    private final VaultOperations vaultOperations;
    private final CmsProperties cmsProperties;
    
    // In-memory cache with expiry
    private Map<String, TokenInfo> tokenCache = new ConcurrentHashMap<>();
    private static final int REFRESH_THRESHOLD_MINUTES = 5;

    /**
     * Get valid access token, refreshing if needed
     */
    public String getAccessToken(String provider) {
        TokenInfo token = tokenCache.get(provider);
        
        if (token != null && !isTokenExpired(token)) {
            return token.accessToken();
        }
        
        return refreshToken(provider);
    }

    /**
     * Request new token from CMS OAuth2 provider
     */
    private String refreshToken(String provider) {
        try {
            TokenRequest request = buildTokenRequest(provider);
            TokenResponse response = restTemplate.postForObject(
                cmsProperties.getTokenUri(provider),
                request,
                TokenResponse.class
            );
            
            TokenInfo tokenInfo = new TokenInfo(
                response.accessToken(),
                response.expiresIn(),
                Instant.now()
            );
            
            tokenCache.put(provider, tokenInfo);
            log.info("Token refreshed for provider: {}", provider);
            
            return response.accessToken();
            
        } catch (RestClientException e) {
            log.error("Token refresh failed for provider: {}", provider, e);
            throw new CmsAuthenticationException("Failed to refresh token", e);
        }
    }

    /**
     * Check if token is expired (with threshold buffer)
     */
    private boolean isTokenExpired(TokenInfo token) {
        Instant expiryTime = token.createdAt()
            .plusSeconds(token.expiresIn())
            .minusSeconds(REFRESH_THRESHOLD_MINUTES * 60);
        return Instant.now().isAfter(expiryTime);
    }

    /**
     * Build OAuth2 token request
     */
    private TokenRequest buildTokenRequest(String provider) {
        String clientId = vaultOperations.read(
            "secret/cms/" + provider + "/client_id"
        ).getData().get("value").toString();
        
        String clientSecret = vaultOperations.read(
            "secret/cms/" + provider + "/client_secret"
        ).getData().get("value").toString();
        
        return new TokenRequest(
            clientId,
            clientSecret,
            "client_credentials"
        );
    }

    record TokenInfo(String accessToken, int expiresIn, Instant createdAt) {}
}
```

**Owner**: Backend engineer  
**Timeline**: Days 1-2  
**Success Criteria**:
- [ ] OAuth2Manager implemented
- [ ] Token request/response handling correct
- [ ] Refresh logic working
- [ ] Unit tests passing (mocked RestTemplate)
- [ ] Code review passed

---

#### 2.2 CMS API Client Wrappers

**File**: `src/main/java/com/healthdata/cmsconnector/infrastructure/client/BcdaClient.java`

```java
@Component
@Slf4j
public class BcdaClient {

    private final RestTemplate restTemplate;
    private final OAuth2Manager oauth2Manager;
    private final CmsProperties cmsProperties;
    private final FhirContext fhirContext;

    /**
     * Get bulk claims export metadata
     */
    public BulkExportMetadata getBulkExportMetadata() {
        String token = oauth2Manager.getAccessToken("bcda");
        
        HttpHeaders headers = buildHeaders(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<BulkExportMetadata> response = restTemplate.exchange(
                cmsProperties.getBcdaBaseUri() + "/api/v2/metadata",
                HttpMethod.GET,
                request,
                BulkExportMetadata.class
            );
            
            return response.getBody();
            
        } catch (RestClientException e) {
            log.error("Failed to get BCDA metadata", e);
            throw new CmsApiException("BCDA metadata request failed", e);
        }
    }

    /**
     * Request bulk export job
     */
    public BulkExportJob requestBulkExport(BulkExportRequest request) {
        String token = oauth2Manager.getAccessToken("bcda");
        
        HttpHeaders headers = buildHeaders(token);
        HttpEntity<BulkExportRequest> entity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                cmsProperties.getBcdaBaseUri() + "/api/v2/Group/$export",
                HttpMethod.POST,
                entity,
                Void.class
            );
            
            // Extract job ID from Content-Location header
            String jobId = extractJobId(response.getHeaders());
            return new BulkExportJob(jobId, "pending", Instant.now());
            
        } catch (RestClientException e) {
            log.error("Failed to request BCDA bulk export", e);
            throw new CmsApiException("BCDA export request failed", e);
        }
    }

    /**
     * Check job status
     */
    public BulkExportJobStatus checkJobStatus(String jobId) {
        String token = oauth2Manager.getAccessToken("bcda");
        
        HttpHeaders headers = buildHeaders(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<BulkExportJobStatus> response = restTemplate.exchange(
                cmsProperties.getBcdaBaseUri() + "/api/v2/Group/$export/" + jobId,
                HttpMethod.GET,
                request,
                BulkExportJobStatus.class
            );
            
            return response.getBody();
            
        } catch (RestClientException e) {
            log.error("Failed to check BCDA job status: {}", jobId, e);
            throw new CmsApiException("BCDA status check failed", e);
        }
    }

    private HttpHeaders buildHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/fhir+json");
        return headers;
    }

    private String extractJobId(HttpHeaders headers) {
        String contentLocation = headers.getFirst("Content-Location");
        // Extract ID from URL like https://bcda.cms.gov/api/v2/Group/$export/job123
        return contentLocation.substring(contentLocation.lastIndexOf("/") + 1);
    }
}
```

**Similar files needed**:
- `DpcClient.java` - Real-time patient data queries
- `BlueButtonClient.java` - Beneficiary-initiated access

**Owner**: Backend engineer  
**Timeline**: Days 2-3  
**Success Criteria**:
- [ ] BcdaClient implemented with core methods
- [ ] DpcClient scaffolded
- [ ] HTTP request building correct
- [ ] Error handling proper
- [ ] Unit tests with mocked responses

---

#### 2.3 Token Refresh Scheduler

**File**: `src/main/java/com/healthdata/cmsconnector/config/TokenRefreshScheduler.java`

```java
@Configuration
@EnableScheduling
@Slf4j
public class TokenRefreshScheduler {

    private final OAuth2Manager oauth2Manager;

    /**
     * Refresh tokens every 50 minutes
     * (tokens expire in 60 minutes, refresh threshold is 5 minutes)
     */
    @Scheduled(fixedRate = 50 * 60 * 1000)  // 50 minutes
    public void refreshTokens() {
        log.debug("Starting scheduled token refresh");
        
        List<String> providers = List.of("bcda", "dpc");
        
        for (String provider : providers) {
            try {
                oauth2Manager.getAccessToken(provider);
                log.info("Token refreshed for provider: {}", provider);
            } catch (Exception e) {
                log.warn("Token refresh failed for provider: {}", provider, e);
                // Queue alerts for monitoring
                alertMonitoring(provider, e);
            }
        }
    }

    private void alertMonitoring(String provider, Exception error) {
        // TODO: Integrate with monitoring/alerting system
        // Send alert if token refresh fails repeatedly
    }
}
```

**Owner**: Backend engineer  
**Timeline**: Day 3  
**Success Criteria**:
- [ ] Scheduler configured
- [ ] Token refresh happens automatically
- [ ] Monitoring/alerting for failures
- [ ] Tests verify scheduler runs

---

#### 2.4 Unit Tests for OAuth2

**File**: `src/test/java/com/healthdata/cmsconnector/infrastructure/OAuth2ManagerTest.java`

```java
@ExtendWith(MockitoExtension.class)
class OAuth2ManagerTest {

    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private VaultOperations vaultOperations;
    
    @Mock
    private CmsProperties cmsProperties;
    
    @InjectMocks
    private OAuth2Manager oauth2Manager;

    @Test
    void testGetAccessToken_ReturnsTokenOnSuccess() {
        // Arrange
        TokenResponse mockResponse = new TokenResponse(
            "test_token_123",
            3600,
            "Bearer"
        );
        
        when(vaultOperations.read(anyString()))
            .thenReturn(createVaultResponse());
        when(restTemplate.postForObject(anyString(), any(), eq(TokenResponse.class)))
            .thenReturn(mockResponse);

        // Act
        String token = oauth2Manager.getAccessToken("bcda");

        // Assert
        assertThat(token).isEqualTo("test_token_123");
    }

    @Test
    void testGetAccessToken_CachesToken() {
        // First call should request token
        // Second call should return cached token
        // Verify only one REST call made
    }

    @Test
    void testTokenRefresh_TriggersBeforeExpiry() {
        // Token created 55 minutes ago (expires in 60)
        // Should trigger refresh (5 min threshold)
    }

    @Test
    void testTokenRefresh_ThrowsOnFailure() {
        // RestTemplate throws exception
        // Should throw CmsAuthenticationException
    }

    private VaultResponse createVaultResponse() {
        // Create mock vault response
        return null;
    }
}
```

**Owner**: Backend engineer  
**Timeline**: Days 3-4  
**Success Criteria**:
- [ ] OAuth2Manager tests comprehensive
- [ ] Mock RestTemplate properly
- [ ] Token caching tested
- [ ] Token refresh logic tested
- [ ] Error handling tested
- [ ] 90%+ code coverage

---

### Week 2 Completion Checklist
- [ ] OAuth2Manager implemented and tested
- [ ] BcdaClient with token integration
- [ ] DpcClient scaffolded
- [ ] Token refresh scheduler
- [ ] All unit tests passing
- [ ] Code review passed
- [ ] Build successful

---

## Week 3: FHIR Parsing & Data Validation

### Week 3 Goals
- [ ] Implement FHIR resource parsing
- [ ] Build data validation pipeline
- [ ] Create claim deduplication logic
- [ ] Test with sandbox data

### Week 3 Deliverables

#### 3.1 FHIR Bundle Parser

**File**: `src/main/java/com/healthdata/cmsconnector/application/FhirBundleParser.java`

```java
@Component
@Slf4j
public class FhirBundleParser {

    private final FhirContext fhirContext;

    /**
     * Parse NDJSON FHIR bundle from CMS
     */
    public List<ParsedClaim> parseBundleNdjson(String ndjsonContent) {
        List<ParsedClaim> claims = new ArrayList<>();
        
        ndjsonContent.lines()
            .filter(line -> !line.trim().isEmpty())
            .forEach(line -> {
                try {
                    IBaseResource resource = fhirContext.newJsonParser()
                        .parseResource(line);
                    
                    if (resource instanceof Bundle) {
                        processBundleResource((Bundle) resource, claims);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse FHIR resource: {}", line, e);
                    // Track parsing errors for monitoring
                }
            });
        
        return claims;
    }

    /**
     * Process Bundle resource and extract claim data
     */
    private void processBundleResource(Bundle bundle, List<ParsedClaim> claims) {
        Map<String, Object> extractedData = extractBundleData(bundle);
        
        // Find the ExplanationOfBenefit resource
        bundle.getEntry().stream()
            .filter(entry -> entry.getResource() instanceof ExplanationOfBenefit)
            .forEach(entry -> {
                ExplanationOfBenefit eob = (ExplanationOfBenefit) entry.getResource();
                ParsedClaim claim = mapEobToClaim(eob, extractedData);
                claims.add(claim);
            });
    }

    /**
     * Map ExplanationOfBenefit to HDIM ParsedClaim
     */
    private ParsedClaim mapEobToClaim(ExplanationOfBenefit eob, Map<String, Object> bundleData) {
        return ParsedClaim.builder()
            .claimId(eob.getId())
            .beneficiaryId(extractBeneficiaryId(eob))
            .serviceFromDate(eob.getBillablePeriod().getStart())
            .serviceToDate(eob.getBillablePeriod().getEnd())
            .createdDate(eob.getCreated())
            .provider(extractProvider(eob))
            .diagnoses(extractDiagnoses(eob))
            .procedures(extractProcedures(eob))
            .medications(extractMedications(bundleData))
            .observations(extractObservations(bundleData))
            .raw_fhir(fhirContext.newJsonParser().encodeResourceToString(eob))
            .build();
    }

    private String extractBeneficiaryId(ExplanationOfBenefit eob) {
        // Extract from patient reference
        String patientRef = eob.getPatient().getReference();
        return patientRef.substring(patientRef.lastIndexOf("/") + 1);
    }

    private String extractProvider(ExplanationOfBenefit eob) {
        // Extract provider NPI from provider field
        if (eob.getProvider() != null && eob.getProvider().hasReference()) {
            String providerRef = eob.getProvider().getReference();
            // Look up NPI from referenced Practitioner
            return providerRef;
        }
        return null;
    }

    private List<DiagnosisInfo> extractDiagnoses(ExplanationOfBenefit eob) {
        return eob.getDiagnosis().stream()
            .map(d -> new DiagnosisInfo(
                d.getDiagnosis() instanceof CodeableConcept 
                    ? ((CodeableConcept) d.getDiagnosis()).getCodingFirstRep().getCode()
                    : null,
                d.getDiagnosisElement().hasValue() 
                    ? d.getDiagnosisElement().getValueAsString()
                    : null
            ))
            .collect(Collectors.toList());
    }

    // Similar methods for procedures, medications, observations...

    record ParsedClaim(
        String claimId,
        String beneficiaryId,
        LocalDate serviceFromDate,
        LocalDate serviceToDate,
        Date createdDate,
        String provider,
        List<DiagnosisInfo> diagnoses,
        List<ProcedureInfo> procedures,
        List<MedicationInfo> medications,
        List<ObservationInfo> observations,
        String raw_fhir
    ) {}
}
```

**Owner**: Backend engineer  
**Timeline**: Days 1-2  
**Success Criteria**:
- [ ] FHIR parsing working with NDJSON
- [ ] ExplanationOfBenefit extraction correct
- [ ] Nested resource extraction working
- [ ] Test with CMS sandbox data successful

---

#### 3.2 Data Validation Pipeline

**File**: `src/main/java/com/healthdata/cmsconnector/application/ClaimValidation Service.java`

```java
@Component
@Slf4j
public class ClaimValidationService {

    private final RecipientRepository claimRepository;

    /**
     * Validate parsed claim before storage
     */
    public ValidationResult validateClaim(ParsedClaim claim) {
        List<ValidationError> errors = new ArrayList<>();

        // Required field validation
        if (!hasValue(claim.claimId())) {
            errors.add(new ValidationError("claim_id", "Claim ID is required"));
        }

        if (!hasValue(claim.beneficiaryId())) {
            errors.add(new ValidationError("beneficiary_id", "Beneficiary ID is required"));
        }

        // Date validation
        if (claim.serviceFromDate() != null && claim.serviceToDate() != null) {
            if (claim.serviceFromDate().isAfter(claim.serviceToDate())) {
                errors.add(new ValidationError("service_dates", 
                    "Service from date cannot be after to date"));
            }
        }

        // Date range validation (not too old)
        if (claim.serviceFromDate() != null) {
            LocalDate fourYearsAgo = LocalDate.now().minusYears(4);
            if (claim.serviceFromDate().isBefore(fourYearsAgo)) {
                log.warn("Claim has very old service date: {}", claim.serviceFromDate());
                // Log warning but don't error (CMS may include historical data)
            }
        }

        return new ValidationResult(
            errors.isEmpty(),
            errors,
            errors.isEmpty() ? "Claim valid" : "Claim validation failed"
        );
    }

    /**
     * Detect duplicate claims
     */
    public boolean isDuplicate(ParsedClaim claim) {
        // Check if claim with same claim_id already imported
        return claimRepository.existsByClaimId(claim.claimId());
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    record ValidationResult(
        boolean valid,
        List<ValidationError> errors,
        String message
    ) {}

    record ValidationError(String field, String message) {}
}
```

**Owner**: Backend engineer  
**Timeline**: Days 2-3  
**Success Criteria**:
- [ ] Validation service implemented
- [ ] Required field validation working
- [ ] Date range validation working
- [ ] Duplicate detection logic correct
- [ ] Tests passing

---

#### 3.3 Sandbox Testing

**Task**: Test with actual CMS sandbox data

**Test Plan**:
1. [ ] Request BCDA sandbox bulk export
2. [ ] Parse returned NDJSON data
3. [ ] Extract 100+ claims
4. [ ] Validate claims using validation pipeline
5. [ ] Verify no errors on valid sandbox data
6. [ ] Test with malformed data (handles errors gracefully)

**Test Data Resources**:
- CMS provides test beneficiary IDs: https://bcda.cms.gov/test-data
- Sample claims data: ~100-1000 claims per test run
- Multiple file formats to test (NDJSON, JSON, etc.)

**Success Criteria**:
- [ ] Successfully parse sandbox NDJSON
- [ ] Extract 100+ claims without errors
- [ ] All required fields present
- [ ] No duplicate issues
- [ ] Performance acceptable (<5 sec for 1K claims)

**Owner**: QA engineer + Backend engineer  
**Timeline**: Days 3-4  
**Success Criteria**:
- [ ] Sandbox connection working
- [ ] Bulk export request successful
- [ ] Data parsing correct
- [ ] Validation passing on real data

---

### Week 3 Completion Checklist
- [ ] FHIR bundle parser implemented
- [ ] Data validation service working
- [ ] Claim deduplication logic
- [ ] Sandbox bulk export successful
- [ ] 1000+ claims parsed and validated
- [ ] Unit and integration tests passing
- [ ] Performance meets targets

---

## Week 4: Architecture Review & Phase 1 Completion

### Week 4 Goals
- [ ] Complete integration testing
- [ ] Prepare architecture review presentation
- [ ] Document decisions and rationale
- [ ] Get go/no-go decision for Phase 2

### Week 4 Deliverables

#### 4.1 Integration Testing

**Test Scenarios**:
1. **OAuth2 Flow**
   - [ ] Request token successfully
   - [ ] Use token for API requests
   - [ ] Handle token expiry gracefully
   - [ ] Refresh token before expiry

2. **Data Pipeline**
   - [ ] Request bulk export
   - [ ] Download NDJSON data
   - [ ] Parse all resources
   - [ ] Validate data quality
   - [ ] Detect duplicates

3. **Error Handling**
   - [ ] Network timeout (retry logic)
   - [ ] Invalid token (refresh)
   - [ ] Malformed data (logging, skipping)
   - [ ] Rate limiting (backoff strategy)

**Test File**: `src/test/java/com/healthdata/cmsconnector/integration/BcdaIntegrationTest.java`

**Success Criteria**:
- [ ] All integration tests passing
- [ ] 100% success rate on sandbox data
- [ ] Error handling verified
- [ ] Performance acceptable

---

#### 4.2 Architecture Review Document

**Document**: `cms-connector/docs/ARCHITECTURE-REVIEW.md`

**Contents**:
1. **System Overview**
   - Component diagram
   - Data flow diagram
   - Integration points

2. **Design Decisions**
   - OAuth2 client credentials flow (why not others)
   - Token refresh strategy
   - FHIR resource mapping approach
   - Data storage strategy

3. **Trade-offs**
   - Caching vs. freshness
   - Real-time vs. bulk import
   - Complexity vs. functionality

4. **Risk Analysis**
   - API availability risk (medium)
   - Data quality risk (medium)
   - Security risk (low with proper implementation)
   - Mitigation strategies

5. **Performance Metrics**
   - Token request: <500ms
   - FHIR parsing: <5sec per 1K claims
   - Claim validation: <10sec per 1K claims
   - Deduplication: <5sec per 1K claims

6. **Success Criteria Met**
   - [x] OAuth2 integration working
   - [x] Test data retrieved and parsed
   - [x] FHIR mapping validated
   - [x] Measure calculation ready for Phase 2

---

#### 4.3 Phase 1 Completion Report

**Deliverables Summary**:
- [ ] OAuth2 authentication fully implemented
- [ ] FHIR data model mapping completed
- [ ] CMS Connector Service scaffolded with core components
- [ ] Token management and refresh working
- [ ] Bulk export and parsing functional
- [ ] Data validation pipeline complete
- [ ] 100+ unit/integration tests passing
- [ ] Sandbox testing successful with real CMS data

**Metrics**:
- Code coverage: >80%
- Performance: OAuth2 <500ms, parsing <5sec/1K claims
- Reliability: 100% success rate on sandbox data
- Security: HIPAA-ready architecture

**Issues & Resolutions**:
- List any blockers encountered and how they were resolved
- Document lessons learned

**Go/No-Go Decision Framework**:
- [ ] All acceptance criteria met
- [ ] Security review passed
- [ ] Performance acceptable
- [ ] Code quality acceptable
- [ ] Team confidence high

**Recommendation**: ✅ **GO** to Phase 2

---

### Week 4 Completion Checklist
- [ ] Integration tests all passing
- [ ] Architecture review document completed
- [ ] Presentation prepared for stakeholders
- [ ] Phase 1 completion report submitted
- [ ] Code committed and reviewed
- [ ] Go/no-go decision documented
- [ ] Phase 2 kickoff scheduled

---

## Phase 1 Success Criteria

### Technical Criteria
- [x] OAuth2 authentication working with CMS sandbox
- [x] Token refresh mechanism functional
- [x] FHIR resource parsing correct
- [x] Data validation pipeline complete
- [x] Duplicate detection working
- [x] 100+ unit tests with >80% coverage
- [x] Integration tests passing on sandbox

### Code Quality Criteria
- [x] Code follows HDIM standards
- [x] All code reviewed and approved
- [x] Documentation complete
- [x] No security vulnerabilities found
- [x] Performance meets targets

### Documentation Criteria
- [x] OAuth2 design documented
- [x] FHIR mapping documented
- [x] API specification complete
- [x] Architecture review documented
- [x] Phase 1 completion report

### Team Criteria
- [x] Team has deep understanding of CMS integration
- [x] Ready to build Phase 2 services
- [x] Confident in approach and timeline
- [x] All blockers resolved

---

## Phase 1 Timeline Summary

```
Week 1: API Setup & Design
├─ Days 1-2: Get CMS credentials
├─ Days 2-3: Design OAuth2 flow
├─ Days 3-4: FHIR mapping design
└─ Days 1-4: Dev environment setup

Week 2: OAuth2 Implementation
├─ Days 1-2: OAuth2Manager
├─ Days 2-3: CMS API clients
├─ Day 3: Token refresh scheduler
└─ Days 3-4: Unit tests (OAuth2)

Week 3: FHIR Parsing & Validation
├─ Days 1-2: FHIR bundle parser
├─ Days 2-3: Validation pipeline
├─ Days 3-4: Sandbox testing
└─ Days 3-4: Integration tests

Week 4: Review & Completion
├─ Day 1: Finish integration testing
├─ Day 2: Prepare architecture review
├─ Day 3: Document decisions
└─ Day 4: Go/no-go decision
```

---

## Risks & Mitigation

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|-----------|
| CMS API downtime | Low | High | Use sandbox early, build queue system |
| Token expiry issues | Medium | Medium | Test refresh logic thoroughly |
| FHIR parsing errors | Medium | Medium | Handle errors gracefully, log all failures |
| Security vulnerabilities | Low | Critical | Security review before Phase 2 |
| Timeline delays | Medium | Medium | Daily standups, clear blockers |

---

## Next Steps (Phase 2 Preparation)

Once Phase 1 is complete, Phase 2 will focus on:
- Building data import service
- Implementing caching layer
- Creating measure calculation pipeline
- Setting up monitoring and alerting

---

**Phase 1 Owner**: Backend Team Lead  
**Last Updated**: January 1, 2025  
**Status**: Ready to Start
