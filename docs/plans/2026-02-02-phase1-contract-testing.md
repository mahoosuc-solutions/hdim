# Phase 1: Contract Testing Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement Pact contract testing for critical service boundaries and OpenAPI validation to catch API breaking changes before deployment.

**Architecture:** Consumer-driven contracts using Pact for 3 critical boundaries (Angular↔Patient, Angular↔CareGap, CareGap↔Patient), plus OpenAPI spec validation as a CI gate. Self-hosted Pact Broker in Docker for HIPAA compliance.

**Tech Stack:** Pact (JS for Angular, JVM for Spring Boot), Swagger Request Validator, PostgreSQL (Pact Broker storage), Docker Compose

---

## Prerequisites

- Working in worktree: `/mnt/wdblack/dev/projects/hdim-master/.worktrees/phase1-contract-testing`
- Branch: `feature/phase1-contract-testing`
- Design doc: `docs/plans/2026-02-02-comprehensive-testing-strategy-design.md`

---

## Task 1: Pact Broker Infrastructure

**Files:**
- Create: `docker/pact-broker/docker-compose.pact.yml`
- Modify: `docker-compose.yml` (add include reference)
- Create: `docker/pact-broker/.env.example`

**Step 1: Create Pact Broker Docker Compose file**

```yaml
# docker/pact-broker/docker-compose.pact.yml
services:
  pact-broker:
    image: pactfoundation/pact-broker:latest
    container_name: hdim-pact-broker
    ports:
      - "9292:9292"
    environment:
      PACT_BROKER_DATABASE_URL: "postgres://pact:${PACT_BROKER_DB_PASSWORD:-pactpassword}@pact-db/pact_broker"
      PACT_BROKER_BASIC_AUTH_USERNAME: "${PACT_BROKER_USERNAME:-hdim}"
      PACT_BROKER_BASIC_AUTH_PASSWORD: "${PACT_BROKER_PASSWORD:-hdimcontract}"
      PACT_BROKER_LOG_LEVEL: "INFO"
      PACT_BROKER_SQL_LOG_LEVEL: "WARN"
    depends_on:
      pact-db:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9292/diagnostic/status/heartbeat"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - hdim-network

  pact-db:
    image: postgres:16-alpine
    container_name: hdim-pact-db
    environment:
      POSTGRES_USER: pact
      POSTGRES_PASSWORD: "${PACT_BROKER_DB_PASSWORD:-pactpassword}"
      POSTGRES_DB: pact_broker
    volumes:
      - pact-db-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U pact -d pact_broker"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - hdim-network

volumes:
  pact-db-data:

networks:
  hdim-network:
    external: true
```

**Step 2: Create environment file template**

```bash
# docker/pact-broker/.env.example
PACT_BROKER_DB_PASSWORD=pactpassword
PACT_BROKER_USERNAME=hdim
PACT_BROKER_PASSWORD=hdimcontract
```

**Step 3: Verify Pact Broker starts**

Run:
```bash
cd /mnt/wdblack/dev/projects/hdim-master/.worktrees/phase1-contract-testing
docker network create hdim-network 2>/dev/null || true
cp docker/pact-broker/.env.example docker/pact-broker/.env
docker compose -f docker/pact-broker/docker-compose.pact.yml up -d
sleep 10
curl -s http://localhost:9292/diagnostic/status/heartbeat | grep -q "ok"
echo "Pact Broker healthy: $?"
```

Expected: `Pact Broker healthy: 0`

**Step 4: Commit**

```bash
git add docker/pact-broker/
git commit -m "infra: add Pact Broker Docker configuration

Self-hosted Pact Broker for contract testing:
- PostgreSQL backend for contract storage
- Basic auth for API access
- Health checks for orchestration

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 2: Shared Contract Testing Module (Backend)

**Files:**
- Create: `backend/modules/shared/contract-testing/build.gradle.kts`
- Create: `backend/modules/shared/contract-testing/src/main/java/com/healthdata/contracts/PactConfig.java`
- Create: `backend/modules/shared/contract-testing/src/main/java/com/healthdata/contracts/ContractTestBase.java`
- Modify: `backend/settings.gradle.kts` (add module)

**Step 1: Create Gradle build file**

```kotlin
// backend/modules/shared/contract-testing/build.gradle.kts
plugins {
    id("hdim.java-library-conventions")
}

description = "Shared Pact contract testing infrastructure"

dependencies {
    api("au.com.dius.pact.provider:junit5:4.6.5")
    api("au.com.dius.pact.provider:spring:4.6.5")
    api("au.com.dius.pact.provider:junit5spring:4.6.5")

    implementation(project(":modules:shared:test-infrastructure"))
    implementation("org.springframework.boot:spring-boot-starter-test")
    implementation("org.springframework.boot:spring-boot-starter-web")

    // For state setup
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
```

**Step 2: Create Pact configuration class**

```java
// backend/modules/shared/contract-testing/src/main/java/com/healthdata/contracts/PactConfig.java
package com.healthdata.contracts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Pact contract testing.
 *
 * Environment variables:
 * - PACT_BROKER_URL: URL of the Pact Broker (default: http://localhost:9292)
 * - PACT_BROKER_USERNAME: Basic auth username
 * - PACT_BROKER_PASSWORD: Basic auth password
 */
@Configuration
public class PactConfig {

    @Value("${pact.broker.url:http://localhost:9292}")
    private String brokerUrl;

    @Value("${pact.broker.username:hdim}")
    private String brokerUsername;

    @Value("${pact.broker.password:hdimcontract}")
    private String brokerPassword;

    @Value("${pact.provider.version:${git.commit.id:unknown}}")
    private String providerVersion;

    @Value("${pact.provider.branch:${git.branch:main}}")
    private String providerBranch;

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public String getBrokerUsername() {
        return brokerUsername;
    }

    public String getBrokerPassword() {
        return brokerPassword;
    }

    public String getProviderVersion() {
        return providerVersion;
    }

    public String getProviderBranch() {
        return providerBranch;
    }
}
```

**Step 3: Create base test class**

```java
// backend/modules/shared/contract-testing/src/main/java/com/healthdata/contracts/ContractTestBase.java
package com.healthdata.contracts;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for Pact provider verification tests.
 *
 * Usage:
 * 1. Extend this class
 * 2. Add @Provider("YourServiceName") annotation
 * 3. Implement @State methods for each provider state
 * 4. Run tests to verify contracts
 *
 * Example:
 * <pre>
 * {@code
 * @Provider("PatientService")
 * class PatientServiceProviderTest extends ContractTestBase {
 *
 *     @State("patient exists with id f47ac10b-58cc-4372-a567-0e02b2c3d479")
 *     void setupPatientExists() {
 *         // Setup test data
 *     }
 * }
 * }
 * </pre>
 */
@Tag("contract")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@PactBroker(
    url = "${pact.broker.url:http://localhost:9292}",
    authentication = @PactBrokerAuth(
        username = "${pact.broker.username:hdim}",
        password = "${pact.broker.password:hdimcontract}"
    )
)
public abstract class ContractTestBase {

    @LocalServerPort
    protected int port;

    @Autowired(required = false)
    protected PactConfig pactConfig;

    @BeforeEach
    void setupTestTarget(PactVerificationContext context) {
        if (context != null) {
            context.setTarget(new HttpTestTarget("localhost", port));
        }
    }

    /**
     * Override to set up tenant context for multi-tenant tests.
     */
    protected String getTestTenantId() {
        return "test-tenant-contracts";
    }

    /**
     * Override to set up user context for authenticated tests.
     */
    protected String getTestUserId() {
        return "contract-test-user";
    }
}
```

**Step 4: Add module to settings.gradle.kts**

Open `backend/settings.gradle.kts` and add to the shared modules section:

```kotlin
// Find the line with shared modules and add:
include(":modules:shared:contract-testing")
```

**Step 5: Verify module compiles**

Run:
```bash
cd /mnt/wdblack/dev/projects/hdim-master/.worktrees/phase1-contract-testing/backend
./gradlew :modules:shared:contract-testing:build --no-daemon -x test
```

Expected: `BUILD SUCCESSFUL`

**Step 6: Commit**

```bash
git add backend/modules/shared/contract-testing/ backend/settings.gradle.kts
git commit -m "feat: add shared contract-testing module

Pact provider verification infrastructure:
- PactConfig for broker connection settings
- ContractTestBase for standardized provider tests
- Integration with existing test-infrastructure module

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 3: Patient Service Provider Verification

**Files:**
- Modify: `backend/modules/services/patient-service/build.gradle.kts` (add dependency)
- Create: `backend/modules/services/patient-service/src/test/java/com/healthdata/patient/contracts/PatientServiceProviderTest.java`
- Create: `backend/modules/services/patient-service/src/test/java/com/healthdata/patient/contracts/PatientContractStateSetup.java`

**Step 1: Add contract-testing dependency to patient-service**

Add to `backend/modules/services/patient-service/build.gradle.kts`:

```kotlin
dependencies {
    // ... existing dependencies ...

    // Contract testing
    testImplementation(project(":modules:shared:contract-testing"))
}
```

**Step 2: Create state setup helper**

```java
// backend/modules/services/patient-service/src/test/java/com/healthdata/patient/contracts/PatientContractStateSetup.java
package com.healthdata.patient.contracts;

import com.healthdata.patient.domain.Patient;
import com.healthdata.patient.repository.PatientRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Helper for setting up contract test states.
 * Uses deterministic UUIDs for reproducible tests.
 */
@Component
public class PatientContractStateSetup {

    // Deterministic UUIDs matching consumer contract expectations
    public static final String PATIENT_JOHN_DOE_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
    public static final String PATIENT_JANE_SMITH_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
    public static final String TEST_TENANT_ID = "test-tenant-contracts";

    private final PatientRepository patientRepository;

    public PatientContractStateSetup(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional
    public void setupPatientJohnDoe() {
        if (patientRepository.findById(UUID.fromString(PATIENT_JOHN_DOE_ID)).isEmpty()) {
            Patient patient = Patient.builder()
                .id(UUID.fromString(PATIENT_JOHN_DOE_ID))
                .tenantId(TEST_TENANT_ID)
                .mrn("MRN-12345")
                .firstName("John")
                .lastName("Doe")
                .middleName("Michael")
                .dateOfBirth(LocalDate.of(1980, 1, 15))
                .gender("male")
                .active(true)
                .build();
            patientRepository.save(patient);
        }
    }

    @Transactional
    public void setupPatientJaneSmith() {
        if (patientRepository.findById(UUID.fromString(PATIENT_JANE_SMITH_ID)).isEmpty()) {
            Patient patient = Patient.builder()
                .id(UUID.fromString(PATIENT_JANE_SMITH_ID))
                .tenantId(TEST_TENANT_ID)
                .mrn("MRN-67890")
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.of(1975, 6, 22))
                .gender("female")
                .active(true)
                .build();
            patientRepository.save(patient);
        }
    }

    @Transactional
    public void cleanupTestData() {
        patientRepository.deleteById(UUID.fromString(PATIENT_JOHN_DOE_ID));
        patientRepository.deleteById(UUID.fromString(PATIENT_JANE_SMITH_ID));
    }
}
```

**Step 3: Create provider verification test**

```java
// backend/modules/services/patient-service/src/test/java/com/healthdata/patient/contracts/PatientServiceProviderTest.java
package com.healthdata.patient.contracts;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;
import com.healthdata.contracts.ContractTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.healthdata.patient.contracts.PatientContractStateSetup.*;

/**
 * Pact provider verification for Patient Service.
 *
 * Verifies that Patient Service fulfills contracts defined by:
 * - ClinicalPortal (Angular frontend)
 * - CareGapService (service-to-service)
 * - QualityMeasureService (service-to-service)
 *
 * Run with Pact Broker available:
 * ```
 * ./gradlew :modules:services:patient-service:test --tests "*PatientServiceProviderTest"
 * ```
 */
@Provider("PatientService")
@ExtendWith(SpringExtension.class)
public class PatientServiceProviderTest extends ContractTestBase {

    @Autowired
    private PatientContractStateSetup stateSetup;

    @PactBrokerConsumerVersionSelectors
    public static SelectorBuilder consumerVersionSelectors() {
        // Verify against:
        // 1. Main branch contracts (production compatibility)
        // 2. Matching branch contracts (PR compatibility)
        // 3. Deployed contracts (what's actually running)
        return new SelectorBuilder()
            .mainBranch()
            .matchingBranch()
            .deployedOrReleased();
    }

    @BeforeEach
    void setupMocks(PactVerificationContext context) {
        super.setupTestTarget(context);
    }

    @AfterEach
    void cleanup() {
        stateSetup.cleanupTestData();
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    // ==================== Provider States ====================

    @State("patient exists with id f47ac10b-58cc-4372-a567-0e02b2c3d479")
    void patientJohnDoeExists() {
        stateSetup.setupPatientJohnDoe();
    }

    @State("patient John Doe exists")
    void patientJohnDoeExistsByName() {
        stateSetup.setupPatientJohnDoe();
    }

    @State("patient exists with MRN MRN-12345")
    void patientExistsWithMrn() {
        stateSetup.setupPatientJohnDoe();
    }

    @State("no patient exists with id 00000000-0000-0000-0000-000000000000")
    void noPatientExists() {
        // No setup needed - patient doesn't exist
    }

    @State("multiple patients exist")
    void multiplePatientsExist() {
        stateSetup.setupPatientJohnDoe();
        stateSetup.setupPatientJaneSmith();
    }

    @State("patient exists for care gap lookup")
    void patientExistsForCareGap() {
        stateSetup.setupPatientJohnDoe();
    }
}
```

**Step 4: Verify test compiles (will skip if no contracts published yet)**

Run:
```bash
cd /mnt/wdblack/dev/projects/hdim-master/.worktrees/phase1-contract-testing/backend
./gradlew :modules:services:patient-service:compileTestJava --no-daemon
```

Expected: `BUILD SUCCESSFUL`

**Step 5: Commit**

```bash
git add backend/modules/services/patient-service/
git commit -m "feat(patient-service): add Pact provider verification

Provider states for contract verification:
- patient exists with specific UUID
- patient exists with MRN
- multiple patients exist
- no patient exists (404 scenarios)

Verifies contracts from ClinicalPortal, CareGapService, QualityMeasureService.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 4: Care Gap Service Provider Verification

**Files:**
- Modify: `backend/modules/services/care-gap-service/build.gradle.kts`
- Create: `backend/modules/services/care-gap-service/src/test/java/com/healthdata/caregap/contracts/CareGapServiceProviderTest.java`
- Create: `backend/modules/services/care-gap-service/src/test/java/com/healthdata/caregap/contracts/CareGapContractStateSetup.java`

**Step 1: Add contract-testing dependency**

Add to `backend/modules/services/care-gap-service/build.gradle.kts`:

```kotlin
dependencies {
    // ... existing dependencies ...

    // Contract testing
    testImplementation(project(":modules:shared:contract-testing"))
}
```

**Step 2: Create state setup helper**

```java
// backend/modules/services/care-gap-service/src/test/java/com/healthdata/caregap/contracts/CareGapContractStateSetup.java
package com.healthdata.caregap.contracts;

import com.healthdata.caregap.domain.CareGap;
import com.healthdata.caregap.domain.CareGapStatus;
import com.healthdata.caregap.repository.CareGapRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Helper for setting up care gap contract test states.
 */
@Component
public class CareGapContractStateSetup {

    // Deterministic UUIDs matching consumer expectations
    public static final String CARE_GAP_HBA1C_ID = "550e8400-e29b-41d4-a716-446655440001";
    public static final String CARE_GAP_BCS_ID = "550e8400-e29b-41d4-a716-446655440002";
    public static final String PATIENT_WITH_GAPS_ID = "550e8400-e29b-41d4-a716-446655440000";
    public static final String TEST_TENANT_ID = "test-tenant-contracts";

    private final CareGapRepository careGapRepository;

    public CareGapContractStateSetup(CareGapRepository careGapRepository) {
        this.careGapRepository = careGapRepository;
    }

    @Transactional
    public void setupOpenCareGaps() {
        setupHba1cGap();
        setupBcsGap();
    }

    @Transactional
    public void setupHba1cGap() {
        if (careGapRepository.findById(UUID.fromString(CARE_GAP_HBA1C_ID)).isEmpty()) {
            CareGap gap = CareGap.builder()
                .id(UUID.fromString(CARE_GAP_HBA1C_ID))
                .tenantId(TEST_TENANT_ID)
                .patientId(UUID.fromString(PATIENT_WITH_GAPS_ID))
                .measureId("HBA1C")
                .measureName("Hemoglobin A1c Control")
                .status(CareGapStatus.OPEN)
                .identifiedDate(LocalDate.now().minusDays(30))
                .dueDate(LocalDate.now().plusDays(60))
                .build();
            careGapRepository.save(gap);
        }
    }

    @Transactional
    public void setupBcsGap() {
        if (careGapRepository.findById(UUID.fromString(CARE_GAP_BCS_ID)).isEmpty()) {
            CareGap gap = CareGap.builder()
                .id(UUID.fromString(CARE_GAP_BCS_ID))
                .tenantId(TEST_TENANT_ID)
                .patientId(UUID.fromString(PATIENT_WITH_GAPS_ID))
                .measureId("BCS")
                .measureName("Breast Cancer Screening")
                .status(CareGapStatus.OPEN)
                .identifiedDate(LocalDate.now().minusDays(15))
                .dueDate(LocalDate.now().plusDays(90))
                .build();
            careGapRepository.save(gap);
        }
    }

    @Transactional
    public void cleanupTestData() {
        careGapRepository.deleteById(UUID.fromString(CARE_GAP_HBA1C_ID));
        careGapRepository.deleteById(UUID.fromString(CARE_GAP_BCS_ID));
    }
}
```

**Step 3: Create provider verification test**

```java
// backend/modules/services/care-gap-service/src/test/java/com/healthdata/caregap/contracts/CareGapServiceProviderTest.java
package com.healthdata.caregap.contracts;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;
import com.healthdata.contracts.ContractTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.healthdata.caregap.contracts.CareGapContractStateSetup.*;

/**
 * Pact provider verification for Care Gap Service.
 *
 * Verifies that Care Gap Service fulfills contracts defined by:
 * - ClinicalPortal (Angular frontend)
 */
@Provider("CareGapService")
@ExtendWith(SpringExtension.class)
public class CareGapServiceProviderTest extends ContractTestBase {

    @Autowired
    private CareGapContractStateSetup stateSetup;

    @PactBrokerConsumerVersionSelectors
    public static SelectorBuilder consumerVersionSelectors() {
        return new SelectorBuilder()
            .mainBranch()
            .matchingBranch()
            .deployedOrReleased();
    }

    @BeforeEach
    void setupMocks(PactVerificationContext context) {
        super.setupTestTarget(context);
    }

    @AfterEach
    void cleanup() {
        stateSetup.cleanupTestData();
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    // ==================== Provider States ====================

    @State("open care gaps exist for patient 550e8400-e29b-41d4-a716-446655440000")
    void openCareGapsExist() {
        stateSetup.setupOpenCareGaps();
    }

    @State("care gap HBA1C exists")
    void hba1cGapExists() {
        stateSetup.setupHba1cGap();
    }

    @State("care gap BCS exists")
    void bcsGapExists() {
        stateSetup.setupBcsGap();
    }

    @State("no care gaps exist for patient 00000000-0000-0000-0000-000000000000")
    void noCareGapsExist() {
        // No setup needed
    }

    @State("care gap can be closed")
    void careGapCanBeClosed() {
        stateSetup.setupHba1cGap();
    }
}
```

**Step 4: Verify test compiles**

Run:
```bash
cd /mnt/wdblack/dev/projects/hdim-master/.worktrees/phase1-contract-testing/backend
./gradlew :modules:services:care-gap-service:compileTestJava --no-daemon
```

Expected: `BUILD SUCCESSFUL`

**Step 5: Commit**

```bash
git add backend/modules/services/care-gap-service/
git commit -m "feat(care-gap-service): add Pact provider verification

Provider states for contract verification:
- open care gaps exist for patient
- specific care gap exists (HBA1C, BCS)
- no care gaps exist (empty scenarios)
- care gap can be closed (state change)

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 5: Angular Consumer Contract Tests Setup

**Files:**
- Create: `apps/clinical-portal/src/test/contracts/pact-setup.ts`
- Create: `apps/clinical-portal/src/test/contracts/patient-service.consumer.pact.spec.ts`
- Modify: `apps/clinical-portal/package.json` (add pact dependencies and scripts)
- Create: `apps/clinical-portal/pact/pact-config.ts`

**Step 1: Add Pact dependencies to package.json**

Add to `apps/clinical-portal/package.json` devDependencies:

```json
{
  "devDependencies": {
    "@pact-foundation/pact": "^13.1.0",
    "@pact-foundation/pact-node": "^10.18.0"
  },
  "scripts": {
    "test:contracts": "jest --config jest.pact.config.js --runInBand",
    "pact:publish": "pact-broker publish ./pacts --consumer-app-version=$npm_package_version --broker-base-url=$PACT_BROKER_URL --broker-username=$PACT_BROKER_USERNAME --broker-password=$PACT_BROKER_PASSWORD"
  }
}
```

**Step 2: Create Pact configuration**

```typescript
// apps/clinical-portal/pact/pact-config.ts
import { PactV4, MatchersV3 } from '@pact-foundation/pact';
import path from 'path';

export const PACT_CONFIG = {
  consumer: 'ClinicalPortal',
  pactDir: path.resolve(__dirname, '../pacts'),
  logLevel: 'warn' as const,
};

export const PROVIDER_NAMES = {
  PATIENT_SERVICE: 'PatientService',
  CARE_GAP_SERVICE: 'CareGapService',
  QUALITY_MEASURE_SERVICE: 'QualityMeasureService',
};

// FHIR-compliant UUID matchers
export const Matchers = {
  uuid: () => MatchersV3.regex(
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/,
    'f47ac10b-58cc-4372-a567-0e02b2c3d479'
  ),
  fhirDate: () => MatchersV3.regex(
    /^\d{4}-\d{2}-\d{2}$/,
    '1980-01-15'
  ),
  tenantId: () => MatchersV3.string('test-tenant-contracts'),
};

export function createPactProvider(providerName: string): PactV4 {
  return new PactV4({
    consumer: PACT_CONFIG.consumer,
    provider: providerName,
    dir: PACT_CONFIG.pactDir,
    logLevel: PACT_CONFIG.logLevel,
  });
}
```

**Step 3: Create Pact test setup**

```typescript
// apps/clinical-portal/src/test/contracts/pact-setup.ts
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

/**
 * Creates a test HTTP client that points to the Pact mock server.
 */
export function createTestHttpClient(
  httpClient: HttpClient,
  mockServerUrl: string
): {
  get: <T>(path: string, headers?: Record<string, string>) => Promise<T>;
  post: <T>(path: string, body: unknown, headers?: Record<string, string>) => Promise<T>;
  put: <T>(path: string, body: unknown, headers?: Record<string, string>) => Promise<T>;
  delete: <T>(path: string, headers?: Record<string, string>) => Promise<T>;
} {
  const buildHeaders = (custom?: Record<string, string>): HttpHeaders => {
    let headers = new HttpHeaders()
      .set('Content-Type', 'application/json')
      .set('X-Tenant-ID', 'test-tenant-contracts');

    if (custom) {
      Object.entries(custom).forEach(([key, value]) => {
        headers = headers.set(key, value);
      });
    }

    return headers;
  };

  return {
    get: <T>(path: string, headers?: Record<string, string>): Promise<T> => {
      return firstValueFrom(
        httpClient.get<T>(`${mockServerUrl}${path}`, { headers: buildHeaders(headers) })
      );
    },
    post: <T>(path: string, body: unknown, headers?: Record<string, string>): Promise<T> => {
      return firstValueFrom(
        httpClient.post<T>(`${mockServerUrl}${path}`, body, { headers: buildHeaders(headers) })
      );
    },
    put: <T>(path: string, body: unknown, headers?: Record<string, string>): Promise<T> => {
      return firstValueFrom(
        httpClient.put<T>(`${mockServerUrl}${path}`, body, { headers: buildHeaders(headers) })
      );
    },
    delete: <T>(path: string, headers?: Record<string, string>): Promise<T> => {
      return firstValueFrom(
        httpClient.delete<T>(`${mockServerUrl}${path}`, { headers: buildHeaders(headers) })
      );
    },
  };
}
```

**Step 4: Create Patient Service consumer contract test**

```typescript
// apps/clinical-portal/src/test/contracts/patient-service.consumer.pact.spec.ts
import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { MatchersV3 } from '@pact-foundation/pact';
import { createPactProvider, PROVIDER_NAMES, Matchers } from '../../../pact/pact-config';
import { createTestHttpClient } from './pact-setup';

const { like, eachLike } = MatchersV3;

describe('Patient Service Contract', () => {
  const provider = createPactProvider(PROVIDER_NAMES.PATIENT_SERVICE);
  let httpClient: HttpClient;

  beforeAll(async () => {
    TestBed.configureTestingModule({
      imports: [HttpClientModule],
    });
    httpClient = TestBed.inject(HttpClient);
  });

  describe('GET /api/v1/patients/:id', () => {
    it('returns patient details for valid UUID', async () => {
      const patientId = 'f47ac10b-58cc-4372-a567-0e02b2c3d479';

      await provider
        .addInteraction()
        .given('patient exists with id f47ac10b-58cc-4372-a567-0e02b2c3d479')
        .uponReceiving('a request for patient details by UUID')
        .withRequest('GET', `/api/v1/patients/${patientId}`, (builder) => {
          builder.headers({
            'X-Tenant-ID': 'test-tenant-contracts',
            'Accept': 'application/json',
          });
        })
        .willRespondWith(200, (builder) => {
          builder
            .headers({ 'Content-Type': 'application/json' })
            .jsonBody({
              id: like(patientId),
              mrn: like('MRN-12345'),
              firstName: like('John'),
              lastName: like('Doe'),
              middleName: like('Michael'),
              dateOfBirth: Matchers.fhirDate(),
              gender: like('male'),
              active: like(true),
            });
        })
        .executeTest(async (mockServer) => {
          const client = createTestHttpClient(httpClient, mockServer.url);
          const response = await client.get<any>(`/api/v1/patients/${patientId}`);

          expect(response.id).toBe(patientId);
          expect(response.firstName).toBeDefined();
          expect(response.lastName).toBeDefined();
        });
    });

    it('returns 404 for non-existent patient', async () => {
      const nonExistentId = '00000000-0000-0000-0000-000000000000';

      await provider
        .addInteraction()
        .given('no patient exists with id 00000000-0000-0000-0000-000000000000')
        .uponReceiving('a request for non-existent patient')
        .withRequest('GET', `/api/v1/patients/${nonExistentId}`, (builder) => {
          builder.headers({
            'X-Tenant-ID': 'test-tenant-contracts',
            'Accept': 'application/json',
          });
        })
        .willRespondWith(404, (builder) => {
          builder
            .headers({ 'Content-Type': 'application/json' })
            .jsonBody({
              error: like('Not Found'),
              message: like('Patient not found'),
            });
        })
        .executeTest(async (mockServer) => {
          const client = createTestHttpClient(httpClient, mockServer.url);

          await expect(
            client.get(`/api/v1/patients/${nonExistentId}`)
          ).rejects.toMatchObject({ status: 404 });
        });
    });
  });

  describe('GET /api/v1/patients/search', () => {
    it('returns patients matching MRN search', async () => {
      await provider
        .addInteraction()
        .given('patient exists with MRN MRN-12345')
        .uponReceiving('a search request by MRN')
        .withRequest('GET', '/api/v1/patients/search', (builder) => {
          builder
            .headers({
              'X-Tenant-ID': 'test-tenant-contracts',
              'Accept': 'application/json',
            })
            .query({ mrn: 'MRN-12345' });
        })
        .willRespondWith(200, (builder) => {
          builder
            .headers({ 'Content-Type': 'application/json' })
            .jsonBody({
              content: eachLike({
                id: Matchers.uuid(),
                mrn: like('MRN-12345'),
                firstName: like('John'),
                lastName: like('Doe'),
              }),
              totalElements: like(1),
              page: like(0),
              size: like(20),
            });
        })
        .executeTest(async (mockServer) => {
          const client = createTestHttpClient(httpClient, mockServer.url);
          const response = await client.get<any>('/api/v1/patients/search?mrn=MRN-12345');

          expect(response.content).toBeInstanceOf(Array);
          expect(response.content.length).toBeGreaterThan(0);
          expect(response.content[0].mrn).toBe('MRN-12345');
        });
    });
  });
});
```

**Step 5: Create Jest Pact configuration**

```javascript
// apps/clinical-portal/jest.pact.config.js
module.exports = {
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: ['<rootDir>/src/test/setup.ts'],
  testMatch: ['**/contracts/**/*.pact.spec.ts'],
  moduleNameMapper: {
    '^@app/(.*)$': '<rootDir>/src/app/$1',
    '^@env/(.*)$': '<rootDir>/src/environments/$1',
  },
  testEnvironment: 'node',
  transform: {
    '^.+\\.(ts|js|html)$': [
      'jest-preset-angular',
      {
        tsconfig: '<rootDir>/tsconfig.spec.json',
        stringifyContentPathRegex: '\\.html$',
      },
    ],
  },
  moduleFileExtensions: ['ts', 'js', 'html'],
  collectCoverage: false,
  verbose: true,
};
```

**Step 6: Install dependencies and verify**

Run:
```bash
cd /mnt/wdblack/dev/projects/hdim-master/.worktrees/phase1-contract-testing/apps/clinical-portal
npm install @pact-foundation/pact @pact-foundation/pact-node --save-dev
```

Expected: Dependencies installed successfully

**Step 7: Commit**

```bash
git add apps/clinical-portal/
git commit -m "feat(clinical-portal): add Pact consumer contract tests

Consumer contracts for Patient Service:
- GET patient by UUID
- GET patient 404 response
- Search patients by MRN

Infrastructure:
- Pact configuration with FHIR-compliant matchers
- Jest Pact test configuration
- Test HTTP client utility

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 6: Care Gap Service Consumer Contract Tests

**Files:**
- Create: `apps/clinical-portal/src/test/contracts/care-gap-service.consumer.pact.spec.ts`

**Step 1: Create Care Gap consumer contract test**

```typescript
// apps/clinical-portal/src/test/contracts/care-gap-service.consumer.pact.spec.ts
import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { MatchersV3 } from '@pact-foundation/pact';
import { createPactProvider, PROVIDER_NAMES, Matchers } from '../../../pact/pact-config';
import { createTestHttpClient } from './pact-setup';

const { like, eachLike } = MatchersV3;

describe('Care Gap Service Contract', () => {
  const provider = createPactProvider(PROVIDER_NAMES.CARE_GAP_SERVICE);
  let httpClient: HttpClient;

  beforeAll(async () => {
    TestBed.configureTestingModule({
      imports: [HttpClientModule],
    });
    httpClient = TestBed.inject(HttpClient);
  });

  describe('GET /api/v1/care-gaps', () => {
    it('returns open care gaps for patient', async () => {
      const patientId = '550e8400-e29b-41d4-a716-446655440000';

      await provider
        .addInteraction()
        .given(`open care gaps exist for patient ${patientId}`)
        .uponReceiving('a request for open care gaps by patient')
        .withRequest('GET', '/api/v1/care-gaps', (builder) => {
          builder
            .headers({
              'X-Tenant-ID': 'test-tenant-contracts',
              'Accept': 'application/json',
            })
            .query({
              patientId: patientId,
              status: 'OPEN',
            });
        })
        .willRespondWith(200, (builder) => {
          builder
            .headers({ 'Content-Type': 'application/json' })
            .jsonBody({
              content: eachLike({
                id: Matchers.uuid(),
                patientId: like(patientId),
                measureId: like('HBA1C'),
                measureName: like('Hemoglobin A1c Control'),
                status: like('OPEN'),
                identifiedDate: Matchers.fhirDate(),
                dueDate: Matchers.fhirDate(),
              }),
              totalElements: like(2),
              page: like(0),
              size: like(20),
            });
        })
        .executeTest(async (mockServer) => {
          const client = createTestHttpClient(httpClient, mockServer.url);
          const response = await client.get<any>(
            `/api/v1/care-gaps?patientId=${patientId}&status=OPEN`
          );

          expect(response.content).toBeInstanceOf(Array);
          expect(response.content.length).toBeGreaterThan(0);
          expect(response.content[0].status).toBe('OPEN');
        });
    });

    it('returns empty list for patient with no care gaps', async () => {
      const patientId = '00000000-0000-0000-0000-000000000000';

      await provider
        .addInteraction()
        .given(`no care gaps exist for patient ${patientId}`)
        .uponReceiving('a request for care gaps for patient with none')
        .withRequest('GET', '/api/v1/care-gaps', (builder) => {
          builder
            .headers({
              'X-Tenant-ID': 'test-tenant-contracts',
              'Accept': 'application/json',
            })
            .query({
              patientId: patientId,
            });
        })
        .willRespondWith(200, (builder) => {
          builder
            .headers({ 'Content-Type': 'application/json' })
            .jsonBody({
              content: [],
              totalElements: like(0),
              page: like(0),
              size: like(20),
            });
        })
        .executeTest(async (mockServer) => {
          const client = createTestHttpClient(httpClient, mockServer.url);
          const response = await client.get<any>(
            `/api/v1/care-gaps?patientId=${patientId}`
          );

          expect(response.content).toEqual([]);
          expect(response.totalElements).toBe(0);
        });
    });
  });

  describe('GET /api/v1/care-gaps/:id', () => {
    it('returns care gap details', async () => {
      const careGapId = '550e8400-e29b-41d4-a716-446655440001';

      await provider
        .addInteraction()
        .given('care gap HBA1C exists')
        .uponReceiving('a request for care gap details')
        .withRequest('GET', `/api/v1/care-gaps/${careGapId}`, (builder) => {
          builder.headers({
            'X-Tenant-ID': 'test-tenant-contracts',
            'Accept': 'application/json',
          });
        })
        .willRespondWith(200, (builder) => {
          builder
            .headers({ 'Content-Type': 'application/json' })
            .jsonBody({
              id: like(careGapId),
              patientId: Matchers.uuid(),
              measureId: like('HBA1C'),
              measureName: like('Hemoglobin A1c Control'),
              status: like('OPEN'),
              identifiedDate: Matchers.fhirDate(),
              dueDate: Matchers.fhirDate(),
              recommendations: eachLike({
                action: like('Order HbA1c test'),
                priority: like('HIGH'),
              }),
            });
        })
        .executeTest(async (mockServer) => {
          const client = createTestHttpClient(httpClient, mockServer.url);
          const response = await client.get<any>(`/api/v1/care-gaps/${careGapId}`);

          expect(response.id).toBe(careGapId);
          expect(response.measureId).toBe('HBA1C');
        });
    });
  });

  describe('POST /api/v1/care-gaps/:id/close', () => {
    it('closes a care gap', async () => {
      const careGapId = '550e8400-e29b-41d4-a716-446655440001';

      await provider
        .addInteraction()
        .given('care gap can be closed')
        .uponReceiving('a request to close a care gap')
        .withRequest('POST', `/api/v1/care-gaps/${careGapId}/close`, (builder) => {
          builder
            .headers({
              'X-Tenant-ID': 'test-tenant-contracts',
              'Content-Type': 'application/json',
              'Accept': 'application/json',
            })
            .jsonBody({
              closureReason: like('Service completed'),
              closedBy: like('contract-test-user'),
              closureDate: Matchers.fhirDate(),
            });
        })
        .willRespondWith(200, (builder) => {
          builder
            .headers({ 'Content-Type': 'application/json' })
            .jsonBody({
              id: like(careGapId),
              status: like('CLOSED'),
              closureReason: like('Service completed'),
              closedDate: Matchers.fhirDate(),
            });
        })
        .executeTest(async (mockServer) => {
          const client = createTestHttpClient(httpClient, mockServer.url);
          const response = await client.post<any>(`/api/v1/care-gaps/${careGapId}/close`, {
            closureReason: 'Service completed',
            closedBy: 'contract-test-user',
            closureDate: '2026-02-02',
          });

          expect(response.status).toBe('CLOSED');
        });
    });
  });
});
```

**Step 2: Verify test file compiles**

Run:
```bash
cd /mnt/wdblack/dev/projects/hdim-master/.worktrees/phase1-contract-testing/apps/clinical-portal
npx tsc --noEmit src/test/contracts/care-gap-service.consumer.pact.spec.ts 2>&1 | head -20
```

Expected: No errors (or only unrelated type errors)

**Step 3: Commit**

```bash
git add apps/clinical-portal/src/test/contracts/care-gap-service.consumer.pact.spec.ts
git commit -m "feat(clinical-portal): add Care Gap Service consumer contracts

Consumer contracts for Care Gap Service:
- GET care gaps by patient and status
- GET care gap details
- POST close care gap
- Empty results for patient with no gaps

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 7: OpenAPI Validation Module

**Files:**
- Create: `backend/modules/shared/openapi-validation/build.gradle.kts`
- Create: `backend/modules/shared/openapi-validation/src/main/java/com/healthdata/openapi/OpenApiValidator.java`
- Create: `backend/modules/shared/openapi-validation/src/main/java/com/healthdata/openapi/OpenApiComplianceTestBase.java`
- Modify: `backend/settings.gradle.kts` (add module)

**Step 1: Create Gradle build file**

```kotlin
// backend/modules/shared/openapi-validation/build.gradle.kts
plugins {
    id("hdim.java-library-conventions")
}

description = "OpenAPI specification validation for API compliance testing"

dependencies {
    api("com.atlassian.oai:swagger-request-validator-core:2.40.0")
    api("com.atlassian.oai:swagger-request-validator-springmvc:2.40.0")

    implementation(project(":modules:shared:test-infrastructure"))
    implementation("org.springframework.boot:spring-boot-starter-test")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
```

**Step 2: Create OpenAPI validator utility**

```java
// backend/modules/shared/openapi-validation/src/main/java/com/healthdata/openapi/OpenApiValidator.java
package com.healthdata.openapi;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Collection;
import java.util.Map;

/**
 * Utility for validating HTTP requests/responses against OpenAPI specifications.
 *
 * Usage:
 * <pre>
 * OpenApiValidator validator = OpenApiValidator.forSpecUrl("http://localhost:8084/v3/api-docs");
 * ValidationReport report = validator.validate(request, response);
 * assertThat(report.hasErrors()).isFalse();
 * </pre>
 */
public class OpenApiValidator {

    private final OpenApiInteractionValidator validator;

    private OpenApiValidator(OpenApiInteractionValidator validator) {
        this.validator = validator;
    }

    public static OpenApiValidator forSpecUrl(String specUrl) {
        OpenApiInteractionValidator validator = OpenApiInteractionValidator
            .createForSpecificationUrl(specUrl)
            .build();
        return new OpenApiValidator(validator);
    }

    public static OpenApiValidator forSpecContent(String specContent) {
        OpenApiInteractionValidator validator = OpenApiInteractionValidator
            .createFor(specContent)
            .build();
        return new OpenApiValidator(validator);
    }

    public ValidationReport validate(MockHttpServletRequest request, MockHttpServletResponse response) {
        Request pactRequest = buildRequest(request);
        Response pactResponse = buildResponse(response);
        return validator.validate(pactRequest, pactResponse);
    }

    public ValidationReport validateRequest(MockHttpServletRequest request) {
        Request pactRequest = buildRequest(request);
        return validator.validateRequest(pactRequest);
    }

    public ValidationReport validateResponse(String path, HttpMethod method, MockHttpServletResponse response) {
        Response pactResponse = buildResponse(response);
        return validator.validateResponse(path, Request.Method.valueOf(method.name()), pactResponse);
    }

    private Request buildRequest(MockHttpServletRequest request) {
        SimpleRequest.Builder builder = new SimpleRequest.Builder(
            Request.Method.valueOf(request.getMethod()),
            request.getRequestURI()
        );

        // Add query parameters
        request.getParameterMap().forEach((name, values) -> {
            for (String value : values) {
                builder.withQueryParam(name, value);
            }
        });

        // Add headers
        var headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            builder.withHeader(name, request.getHeader(name));
        }

        // Add body
        if (request.getContentAsByteArray().length > 0) {
            builder.withBody(new String(request.getContentAsByteArray()));
        }

        return builder.build();
    }

    private Response buildResponse(MockHttpServletResponse response) {
        SimpleResponse.Builder builder = SimpleResponse.Builder.status(response.getStatus());

        // Add headers
        response.getHeaderNames().forEach(name -> {
            Collection<String> values = response.getHeaders(name);
            values.forEach(value -> builder.withHeader(name, value));
        });

        // Add body
        String body = response.getContentAsString();
        if (body != null && !body.isEmpty()) {
            builder.withBody(body);
        }

        return builder.build();
    }

    /**
     * Formats validation errors for assertion messages.
     */
    public static String formatErrors(ValidationReport report) {
        StringBuilder sb = new StringBuilder();
        report.getMessages().forEach(msg -> {
            sb.append("\n  - ").append(msg.getMessage());
            if (msg.getContext().isPresent()) {
                sb.append(" (").append(msg.getContext().get()).append(")");
            }
        });
        return sb.toString();
    }
}
```

**Step 3: Create base test class for OpenAPI compliance**

```java
// backend/modules/shared/openapi-validation/src/main/java/com/healthdata/openapi/OpenApiComplianceTestBase.java
package com.healthdata.openapi;

import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for OpenAPI compliance tests.
 *
 * Validates that API responses match the OpenAPI specification.
 *
 * Usage:
 * <pre>
 * class PatientApiComplianceTest extends OpenApiComplianceTestBase {
 *
 *     @Test
 *     void getPatient_matchesSpec() throws Exception {
 *         MvcResult result = mockMvc.perform(get("/api/v1/patients/123"))
 *             .andReturn();
 *
 *         assertResponseMatchesSpec(result);
 *     }
 * }
 * </pre>
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class OpenApiComplianceTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @LocalServerPort
    protected int port;

    protected OpenApiValidator validator;

    @BeforeEach
    void setupValidator() {
        String specUrl = String.format("http://localhost:%d/v3/api-docs", port);
        validator = OpenApiValidator.forSpecUrl(specUrl);
    }

    /**
     * Asserts that the response matches the OpenAPI specification.
     */
    protected void assertResponseMatchesSpec(MvcResult result) {
        ValidationReport report = validator.validate(
            result.getRequest(),
            result.getResponse()
        );

        assertThat(report.hasErrors())
            .withFailMessage("OpenAPI validation failed: %s", OpenApiValidator.formatErrors(report))
            .isFalse();
    }

    /**
     * Asserts that the request matches the OpenAPI specification.
     */
    protected void assertRequestMatchesSpec(MvcResult result) {
        ValidationReport report = validator.validateRequest(result.getRequest());

        assertThat(report.hasErrors())
            .withFailMessage("Request validation failed: %s", OpenApiValidator.formatErrors(report))
            .isFalse();
    }
}
```

**Step 4: Add module to settings.gradle.kts**

Add to `backend/settings.gradle.kts`:

```kotlin
include(":modules:shared:openapi-validation")
```

**Step 5: Verify module compiles**

Run:
```bash
cd /mnt/wdblack/dev/projects/hdim-master/.worktrees/phase1-contract-testing/backend
./gradlew :modules:shared:openapi-validation:build --no-daemon -x test
```

Expected: `BUILD SUCCESSFUL`

**Step 6: Commit**

```bash
git add backend/modules/shared/openapi-validation/ backend/settings.gradle.kts
git commit -m "feat: add OpenAPI validation module

Swagger Request Validator integration for API compliance:
- OpenApiValidator utility for request/response validation
- OpenApiComplianceTestBase for standardized compliance tests
- Supports both URL and inline spec validation

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 8: Patient Service OpenAPI Compliance Test

**Files:**
- Modify: `backend/modules/services/patient-service/build.gradle.kts`
- Create: `backend/modules/services/patient-service/src/test/java/com/healthdata/patient/api/PatientApiComplianceTest.java`

**Step 1: Add openapi-validation dependency**

Add to `backend/modules/services/patient-service/build.gradle.kts`:

```kotlin
dependencies {
    // ... existing dependencies ...

    // OpenAPI validation
    testImplementation(project(":modules:shared:openapi-validation"))
}
```

**Step 2: Create compliance test**

```java
// backend/modules/services/patient-service/src/test/java/com/healthdata/patient/api/PatientApiComplianceTest.java
package com.healthdata.patient.api;

import com.healthdata.openapi.OpenApiComplianceTestBase;
import com.healthdata.patient.domain.Patient;
import com.healthdata.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validates Patient Service API responses match OpenAPI specification.
 *
 * These tests ensure that the actual API implementation stays in sync
 * with the documented OpenAPI contract.
 */
@DisplayName("Patient API OpenAPI Compliance")
class PatientApiComplianceTest extends OpenApiComplianceTestBase {

    @Autowired
    private PatientRepository patientRepository;

    private static final String TEST_TENANT = "test-tenant-compliance";
    private static final UUID TEST_PATIENT_ID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");

    @BeforeEach
    void setupTestData() {
        patientRepository.deleteAll();

        Patient patient = Patient.builder()
            .id(TEST_PATIENT_ID)
            .tenantId(TEST_TENANT)
            .mrn("MRN-COMPLIANCE-001")
            .firstName("Compliance")
            .lastName("Test")
            .dateOfBirth(LocalDate.of(1985, 5, 15))
            .gender("male")
            .active(true)
            .build();
        patientRepository.save(patient);
    }

    @Nested
    @DisplayName("GET /api/v1/patients/{id}")
    class GetPatientById {

        @Test
        @DisplayName("200 response matches OpenAPI spec")
        void successResponse_matchesSpec() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/patients/{id}", TEST_PATIENT_ID)
                    .header("X-Tenant-ID", TEST_TENANT)
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

            assertResponseMatchesSpec(result);
        }

        @Test
        @DisplayName("404 response matches OpenAPI spec")
        void notFoundResponse_matchesSpec() throws Exception {
            UUID nonExistent = UUID.fromString("00000000-0000-0000-0000-000000000000");

            MvcResult result = mockMvc.perform(get("/api/v1/patients/{id}", nonExistent)
                    .header("X-Tenant-ID", TEST_TENANT)
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

            assertResponseMatchesSpec(result);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/patients/search")
    class SearchPatients {

        @Test
        @DisplayName("200 response with results matches OpenAPI spec")
        void searchWithResults_matchesSpec() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/patients/search")
                    .header("X-Tenant-ID", TEST_TENANT)
                    .param("mrn", "MRN-COMPLIANCE-001")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

            assertResponseMatchesSpec(result);
        }

        @Test
        @DisplayName("200 response with empty results matches OpenAPI spec")
        void searchWithNoResults_matchesSpec() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/patients/search")
                    .header("X-Tenant-ID", TEST_TENANT)
                    .param("mrn", "NON-EXISTENT-MRN")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

            assertResponseMatchesSpec(result);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/patients")
    class CreatePatient {

        @Test
        @DisplayName("201 response matches OpenAPI spec")
        void createPatient_matchesSpec() throws Exception {
            String requestBody = """
                {
                    "mrn": "MRN-NEW-001",
                    "firstName": "New",
                    "lastName": "Patient",
                    "dateOfBirth": "1990-03-20",
                    "gender": "female"
                }
                """;

            MvcResult result = mockMvc.perform(post("/api/v1/patients")
                    .header("X-Tenant-ID", TEST_TENANT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

            assertResponseMatchesSpec(result);
        }

        @Test
        @DisplayName("400 response for invalid request matches OpenAPI spec")
        void invalidRequest_matchesSpec() throws Exception {
            String invalidBody = """
                {
                    "firstName": "Missing Required Fields"
                }
                """;

            MvcResult result = mockMvc.perform(post("/api/v1/patients")
                    .header("X-Tenant-ID", TEST_TENANT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidBody)
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

            assertResponseMatchesSpec(result);
        }
    }
}
```

**Step 3: Verify test compiles**

Run:
```bash
cd /mnt/wdblack/dev/projects/hdim-master/.worktrees/phase1-contract-testing/backend
./gradlew :modules:services:patient-service:compileTestJava --no-daemon
```

Expected: `BUILD SUCCESSFUL`

**Step 4: Commit**

```bash
git add backend/modules/services/patient-service/
git commit -m "feat(patient-service): add OpenAPI compliance tests

Validates API responses match documented OpenAPI specification:
- GET patient by ID (200, 404)
- Search patients (200 with/without results)
- Create patient (201, 400)

Catches spec drift before deployment.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 9: CI/CD Integration

**Files:**
- Create: `.github/workflows/contract-testing.yml`
- Modify: `.github/workflows/pr-checks.yml` (add contract job reference)

**Step 1: Create contract testing workflow**

```yaml
# .github/workflows/contract-testing.yml
name: Contract Testing

on:
  pull_request:
    branches: [main, master, develop]
    paths:
      - 'apps/clinical-portal/**'
      - 'backend/modules/services/patient-service/**'
      - 'backend/modules/services/care-gap-service/**'
      - 'backend/modules/shared/contract-testing/**'
      - 'backend/modules/shared/openapi-validation/**'
  push:
    branches: [main, master]

env:
  PACT_BROKER_URL: ${{ secrets.PACT_BROKER_URL }}
  PACT_BROKER_USERNAME: ${{ secrets.PACT_BROKER_USERNAME }}
  PACT_BROKER_PASSWORD: ${{ secrets.PACT_BROKER_PASSWORD }}

jobs:
  consumer-contracts:
    name: Consumer Contract Tests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: apps/clinical-portal/package-lock.json

      - name: Install dependencies
        working-directory: apps/clinical-portal
        run: npm ci

      - name: Run Consumer Contract Tests
        working-directory: apps/clinical-portal
        run: npm run test:contracts

      - name: Publish Pacts to Broker
        if: github.event_name == 'push' || github.event.pull_request.head.repo.full_name == github.repository
        working-directory: apps/clinical-portal
        run: |
          npm run pact:publish
        env:
          npm_package_version: ${{ github.sha }}

      - name: Upload Pact Files
        uses: actions/upload-artifact@v4
        with:
          name: pact-files
          path: apps/clinical-portal/pacts/
          retention-days: 7

  provider-verification:
    name: Provider Verification
    needs: consumer-contracts
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        service:
          - patient-service
          - care-gap-service
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
          POSTGRES_DB: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'gradle'

      - name: Download Pact Files
        uses: actions/download-artifact@v4
        with:
          name: pact-files
          path: pacts/

      - name: Verify Provider Contracts
        working-directory: backend
        run: |
          ./gradlew :modules:services:${{ matrix.service }}:test \
            --tests "*ProviderTest" \
            -Dpact.verifier.publishResults=true \
            -Dpact.provider.version=${{ github.sha }} \
            -Dpact.provider.branch=${{ github.head_ref || github.ref_name }}
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/test
          SPRING_DATASOURCE_USERNAME: test
          SPRING_DATASOURCE_PASSWORD: test

  openapi-validation:
    name: OpenAPI Validation
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
          POSTGRES_DB: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'gradle'

      - name: Run OpenAPI Compliance Tests
        working-directory: backend
        run: |
          ./gradlew test --tests "*ComplianceTest" --no-daemon
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/test
          SPRING_DATASOURCE_USERNAME: test
          SPRING_DATASOURCE_PASSWORD: test

      - name: Upload Test Reports
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: openapi-test-reports
          path: backend/**/build/reports/tests/
          retention-days: 7

  contract-gate:
    name: Contract Testing Gate
    needs: [consumer-contracts, provider-verification, openapi-validation]
    if: always()
    runs-on: ubuntu-latest
    steps:
      - name: Check Results
        run: |
          if [[ "${{ needs.consumer-contracts.result }}" == "failure" ]] || \
             [[ "${{ needs.provider-verification.result }}" == "failure" ]] || \
             [[ "${{ needs.openapi-validation.result }}" == "failure" ]]; then
            echo "Contract testing failed"
            exit 1
          fi
          echo "All contract tests passed"
```

**Step 2: Commit**

```bash
git add .github/workflows/contract-testing.yml
git commit -m "ci: add contract testing workflow

GitHub Actions workflow for contract testing:
- Consumer contract tests (Angular → Pact files)
- Provider verification (Spring Boot services)
- OpenAPI compliance validation
- Gate job for merge blocking

Runs on PRs touching API-related paths.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 10: Documentation and Final Verification

**Files:**
- Create: `docs/development/CONTRACT_TESTING_GUIDE.md`
- Update: `CLAUDE.md` (add contract testing section)

**Step 1: Create contract testing guide**

```markdown
# Contract Testing Guide

## Overview

HDIM uses **consumer-driven contract testing** with Pact and **OpenAPI validation** to ensure API compatibility across services.

## Quick Start

### Running Consumer Tests (Angular)

```bash
cd apps/clinical-portal
npm run test:contracts
```

### Running Provider Verification (Java)

```bash
cd backend
./gradlew :modules:services:patient-service:test --tests "*ProviderTest"
./gradlew :modules:services:care-gap-service:test --tests "*ProviderTest"
```

### Running OpenAPI Compliance Tests

```bash
cd backend
./gradlew test --tests "*ComplianceTest"
```

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ Angular Portal  │────▶│  Pact Broker    │◀────│ Backend Service │
│ (Consumer)      │     │  (Contracts)    │     │ (Provider)      │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                                               │
        │ Publishes contracts                          │ Verifies contracts
        │                                               │
        └───────────────────────────────────────────────┘
```

## Writing Consumer Contracts

### 1. Define Expected Interactions

```typescript
await provider
  .addInteraction()
  .given('patient exists with id f47ac10b-58cc-4372-a567-0e02b2c3d479')
  .uponReceiving('a request for patient details')
  .withRequest('GET', '/api/v1/patients/f47ac10b-58cc-4372-a567-0e02b2c3d479')
  .willRespondWith(200, (builder) => {
    builder.jsonBody({
      id: like('f47ac10b-58cc-4372-a567-0e02b2c3d479'),
      firstName: like('John'),
      lastName: like('Doe'),
    });
  });
```

### 2. Use FHIR-Compliant UUIDs

Always use string-based UUIDs for resource IDs:

```typescript
// ✅ Correct
const patientId = 'f47ac10b-58cc-4372-a567-0e02b2c3d479';

// ❌ Wrong - don't generate random UUIDs
const patientId = uuid();
```

## Writing Provider States

### 1. Implement State Setup Methods

```java
@State("patient exists with id f47ac10b-58cc-4372-a567-0e02b2c3d479")
void setupPatientExists() {
    patientRepository.save(testPatient);
}
```

### 2. Use Deterministic Test Data

```java
public static final String PATIENT_JOHN_DOE_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
```

## Pact Broker

### Local Development

```bash
# Start Pact Broker
docker compose -f docker/pact-broker/docker-compose.pact.yml up -d

# Access UI
open http://localhost:9292
```

### Publishing Contracts

```bash
# Publish from Angular
npm run pact:publish

# Environment variables required
export PACT_BROKER_URL=http://localhost:9292
export PACT_BROKER_USERNAME=hdim
export PACT_BROKER_PASSWORD=hdimcontract
```

## Troubleshooting

### Consumer test fails with "No matching interaction found"

1. Check the request path matches exactly
2. Verify headers are included (especially X-Tenant-ID)
3. Check query parameters match

### Provider verification fails with "State not found"

1. Add missing `@State` method
2. State name must match exactly (case-sensitive)

### OpenAPI validation fails

1. Check response matches documented schema
2. Verify all required fields are present
3. Check field types match spec
```

**Step 2: Update CLAUDE.md**

Add to the Testing section in CLAUDE.md:

```markdown
## Contract Testing (Phase 1 - NEW)

### Quick Commands

```bash
# Consumer contracts (Angular)
cd apps/clinical-portal && npm run test:contracts

# Provider verification (Java)
cd backend && ./gradlew test --tests "*ProviderTest"

# OpenAPI compliance
cd backend && ./gradlew test --tests "*ComplianceTest"
```

### Coverage

| Boundary | Consumer | Provider | Status |
|----------|----------|----------|--------|
| Angular → Patient Service | ✅ | ✅ | Active |
| Angular → Care Gap Service | ✅ | ✅ | Active |
| OpenAPI Validation | - | ✅ | Active |

**Full Guide:** [Contract Testing Guide](./docs/development/CONTRACT_TESTING_GUIDE.md)
```

**Step 3: Final verification - run all contract tests**

Run:
```bash
cd /mnt/wdblack/dev/projects/hdim-master/.worktrees/phase1-contract-testing

# Start Pact Broker
docker compose -f docker/pact-broker/docker-compose.pact.yml up -d

# Wait for broker
sleep 15

# Run consumer tests
cd apps/clinical-portal && npm run test:contracts

# Run provider tests (Patient Service)
cd ../../backend
./gradlew :modules:services:patient-service:test --tests "*ProviderTest" --tests "*ComplianceTest"

# Run provider tests (Care Gap Service)
./gradlew :modules:services:care-gap-service:test --tests "*ProviderTest"
```

**Step 4: Commit documentation**

```bash
git add docs/development/CONTRACT_TESTING_GUIDE.md CLAUDE.md
git commit -m "docs: add contract testing guide and update CLAUDE.md

Comprehensive contract testing documentation:
- Quick start commands
- Architecture diagram
- Writing consumer/provider tests
- Pact Broker setup
- Troubleshooting guide

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

**Step 5: Final commit - merge preparation**

```bash
git log --oneline -10  # Review all commits
git status             # Verify clean working tree
```

---

## Summary

### Commits Made (10 total)

1. `infra: add Pact Broker Docker configuration`
2. `feat: add shared contract-testing module`
3. `feat(patient-service): add Pact provider verification`
4. `feat(care-gap-service): add Pact provider verification`
5. `feat(clinical-portal): add Pact consumer contract tests`
6. `feat(clinical-portal): add Care Gap Service consumer contracts`
7. `feat: add OpenAPI validation module`
8. `feat(patient-service): add OpenAPI compliance tests`
9. `ci: add contract testing workflow`
10. `docs: add contract testing guide and update CLAUDE.md`

### Files Created/Modified

**New Files (18):**
- `docker/pact-broker/docker-compose.pact.yml`
- `docker/pact-broker/.env.example`
- `backend/modules/shared/contract-testing/build.gradle.kts`
- `backend/modules/shared/contract-testing/src/main/java/.../PactConfig.java`
- `backend/modules/shared/contract-testing/src/main/java/.../ContractTestBase.java`
- `backend/modules/shared/openapi-validation/build.gradle.kts`
- `backend/modules/shared/openapi-validation/src/main/java/.../OpenApiValidator.java`
- `backend/modules/shared/openapi-validation/src/main/java/.../OpenApiComplianceTestBase.java`
- `backend/modules/services/patient-service/src/test/java/.../PatientServiceProviderTest.java`
- `backend/modules/services/patient-service/src/test/java/.../PatientContractStateSetup.java`
- `backend/modules/services/patient-service/src/test/java/.../PatientApiComplianceTest.java`
- `backend/modules/services/care-gap-service/src/test/java/.../CareGapServiceProviderTest.java`
- `backend/modules/services/care-gap-service/src/test/java/.../CareGapContractStateSetup.java`
- `apps/clinical-portal/pact/pact-config.ts`
- `apps/clinical-portal/src/test/contracts/pact-setup.ts`
- `apps/clinical-portal/src/test/contracts/patient-service.consumer.pact.spec.ts`
- `apps/clinical-portal/src/test/contracts/care-gap-service.consumer.pact.spec.ts`
- `.github/workflows/contract-testing.yml`
- `docs/development/CONTRACT_TESTING_GUIDE.md`

**Modified Files (5):**
- `backend/settings.gradle.kts`
- `backend/modules/services/patient-service/build.gradle.kts`
- `backend/modules/services/care-gap-service/build.gradle.kts`
- `apps/clinical-portal/package.json`
- `CLAUDE.md`

### Exit Criteria Checklist

- [ ] Pact Broker running and integrated with CI
- [ ] 3 critical service boundaries covered (Angular↔Patient, Angular↔CareGap, CareGap↔Patient)
- [ ] OpenAPI validation blocking PRs on spec violations
- [ ] Zero false positives for 1 week
