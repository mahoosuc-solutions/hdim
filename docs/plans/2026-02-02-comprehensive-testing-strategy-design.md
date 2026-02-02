# Comprehensive UI and Service-Level Testing Strategy

**Date:** 2026-02-02
**Status:** Draft - Pending Approval
**Author:** AI-Assisted Design Session

---

## Executive Summary

This document outlines a comprehensive testing strategy to address production quality issues in HDIM, specifically:

1. **API Breaking Changes** - Service-to-service and frontend-backend contracts drift
2. **UI Regressions** - Visual and behavioral changes go undetected
3. **Navigation/Flow Issues** - Patient navigation and dashboard drill-down failures

**Approach:** Contract-First, then UI stabilization over 3 phases (8 weeks).

**Expected Outcome:** 85% reduction in UI/API-related production incidents.

---

## Table of Contents

1. [Requirements Summary](#1-requirements-summary)
2. [Architecture Overview](#2-architecture-overview)
3. [Phase 1: Contract Testing](#3-phase-1-contract-testing)
4. [Phase 2: UI Journey Testing](#4-phase-2-ui-journey-testing)
5. [Phase 3: Visual Regression](#5-phase-3-visual-regression)
6. [CI/CD Integration](#6-cicd-integration)
7. [Phased Rollout Plan](#7-phased-rollout-plan)
8. [Success Metrics](#8-success-metrics)

---

## 1. Requirements Summary

| Area | Decision |
|------|----------|
| **Primary Driver** | Production quality (bugs slipping through) |
| **Secondary Drivers** | Compliance/audit, gap coverage, CI/CD performance |
| **Pain Points** | API breaking changes (service-to-service, frontend-backend), UI regressions, navigation/flow issues |
| **UI Issues** | Patient navigation (drill-down/back), Dashboard interactions (click-through failures) |
| **Contract Testing** | Pact for critical boundaries + OpenAPI validation for broad coverage |
| **Visual Regression** | Playwright + BackstopJS (self-hosted, HIPAA-friendly) |
| **Flow Coverage** | Comprehensive - all major user workflows |
| **Rollout** | Incremental phases, highest-impact first |
| **Team** | Experienced (AI-assisted), needs architecture not hand-holding |

---

## 2. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        HDIM Comprehensive Testing Architecture              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Phase 1: Contract Testing                    │   │
│  │                                                                      │   │
│  │   ┌──────────────────┐        ┌──────────────────┐                  │   │
│  │   │   Pact Broker    │        │  OpenAPI Specs   │                  │   │
│  │   │   (Self-hosted)  │        │  (62 endpoints)  │                  │   │
│  │   └────────┬─────────┘        └────────┬─────────┘                  │   │
│  │            │                           │                            │   │
│  │   ┌────────▼─────────┐        ┌────────▼─────────┐                  │   │
│  │   │ Consumer Tests   │        │ Schema Validator │                  │   │
│  │   │ (Angular, Svcs)  │        │ (CI/CD Gate)     │                  │   │
│  │   └────────┬─────────┘        └──────────────────┘                  │   │
│  │            │                                                        │   │
│  │   ┌────────▼─────────┐                                              │   │
│  │   │ Provider Verify  │                                              │   │
│  │   │ (Backend Svcs)   │                                              │   │
│  │   └──────────────────┘                                              │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                     Phase 2: UI Journey Testing                      │   │
│  │                                                                      │   │
│  │   ┌──────────────────┐   ┌──────────────────┐   ┌────────────────┐  │   │
│  │   │ Journey Tests    │   │ State Assertions │   │ Data Fixtures  │  │   │
│  │   │ (Playwright)     │──▶│ (Per-step)       │──▶│ (Factories)    │  │   │
│  │   └──────────────────┘   └──────────────────┘   └────────────────┘  │   │
│  │                                                                      │   │
│  │   Flows: Patient Nav, Dashboard Drill-down, Care Gap, Reports       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    Phase 3: Visual Regression                        │   │
│  │                                                                      │   │
│  │   ┌──────────────────┐   ┌──────────────────┐   ┌────────────────┐  │   │
│  │   │ BackstopJS       │   │ Baseline Images  │   │ Diff Reports   │  │   │
│  │   │ (Self-hosted)    │──▶│ (Git LFS)        │──▶│ (CI Artifacts) │  │   │
│  │   └──────────────────┘   └──────────────────┘   └────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      CI/CD Integration                               │   │
│  │                                                                      │   │
│  │   PR Gate: Contracts ──▶ Unit ──▶ Integration ──▶ E2E ──▶ Visual    │   │
│  │                                                                      │   │
│  │   Change Detection: Only run affected test suites (Phase 7 infra)   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Key Architectural Decisions:**

1. **Self-hosted Pact Broker** - Keeps contract data internal (HIPAA compliance), integrates with existing Docker infrastructure
2. **OpenAPI validation as CI gate** - Lightweight broad coverage using existing 62 documented endpoints
3. **Playwright as journey test foundation** - Builds on existing 295 tests, not a new framework
4. **BackstopJS self-hosted** - No external screenshot services, stores baselines in Git LFS
5. **Leverages Phase 7 change detection** - Only runs affected test suites to maintain fast feedback

---

## 3. Phase 1: Contract Testing

### 3.1 Pact Contract Testing

**Target Boundaries (Top 5 Breaking Points):**

| Consumer | Provider | Why Priority |
|----------|----------|--------------|
| Clinical Portal (Angular) | Patient Service | Patient navigation issues originate here |
| Clinical Portal (Angular) | Care Gap Service | Dashboard drill-down data issues |
| Care Gap Service | Patient Service | Service-to-service dependency |
| Quality Measure Service | Patient Service | Evaluation data flow |
| Gateway | All Backend Services | Single entry point, high blast radius |

**Directory Structure:**

```
backend/
├── modules/
│   ├── shared/
│   │   └── contract-testing/           # NEW: Shared Pact infrastructure
│   │       ├── build.gradle.kts
│   │       ├── src/main/java/
│   │       │   └── com/healthdata/contracts/
│   │       │       ├── PactBrokerConfig.java
│   │       │       └── ContractTestBase.java
│   │       └── pacts/                  # Generated pact files
│   └── services/
│       └── patient-service/
│           └── src/test/java/
│               └── contracts/
│                   └── PatientServiceProviderTest.java  # Verifies contracts
apps/
└── clinical-portal/
    └── src/test/
        └── contracts/
            ├── patient-service.consumer.spec.ts   # Consumer expectations
            └── care-gap-service.consumer.spec.ts
```

**Pact Broker Setup (Docker):**

```yaml
# docker-compose.test.yml (addition)
pact-broker:
  image: pactfoundation/pact-broker:latest
  ports:
    - "9292:9292"
  environment:
    PACT_BROKER_DATABASE_URL: "postgres://pact:pact@pact-db/pact"
    PACT_BROKER_BASIC_AUTH_USERNAME: "hdim"
    PACT_BROKER_BASIC_AUTH_PASSWORD: "${PACT_BROKER_PASSWORD}"
  depends_on:
    - pact-db

pact-db:
  image: postgres:16
  environment:
    POSTGRES_USER: pact
    POSTGRES_PASSWORD: pact
    POSTGRES_DB: pact
  volumes:
    - pact-data:/var/lib/postgresql/data
```

**Consumer Test Example (Angular):**

```typescript
// patient-service.consumer.spec.ts
import { PactV3 } from '@pact-foundation/pact';

const provider = new PactV3({
  consumer: 'ClinicalPortal',
  provider: 'PatientService',
});

describe('Patient Service Contract', () => {
  it('returns patient details for valid ID', async () => {
    // Using FHIR-compliant UUID for patient ID
    const patientId = 'f47ac10b-58cc-4372-a567-0e02b2c3d479';

    await provider
      .given('patient exists')
      .uponReceiving('a request for patient by UUID')
      .withRequest({
        method: 'GET',
        path: `/api/v1/patients/${patientId}`,
        headers: { 'X-Tenant-ID': 'tenant1' },
      })
      .willRespondWith({
        status: 200,
        body: {
          id: patientId,  // FHIR R4: string-based resource ID
          resourceType: 'Patient',
          name: like([{ family: 'Doe', given: ['John'] }]),
          birthDate: regex(/\d{4}-\d{2}-\d{2}/, '1980-01-15'),
        },
      });

    await provider.executeTest(async (mockServer) => {
      const response = await patientService.getPatient(patientId, mockServer.url);
      expect(response.id).toBe(patientId);
    });
  });
});
```

**Provider Verification (Java):**

```java
@Provider("PatientService")
@PactBroker(url = "${pact.broker.url}")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PatientServiceProviderTest {

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("patient exists")
    void setupPatient() {
        // Using FHIR-compliant UUID
        String patientId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
        patientRepository.save(TestPatientFactory.create(patientId));
    }
}
```

### 3.2 OpenAPI Validation

**Purpose:** Catch contract drift for all 62 documented endpoints without writing individual Pact tests for each.

**Spec-to-Code Validation (CI Gate):**

```java
// OpenApiComplianceTest.java - Runs in CI for each service
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiComplianceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private OpenApiInteractionValidator validator;

    @BeforeEach
    void setup() {
        validator = OpenApiInteractionValidator
            .createForSpecificationUrl("http://localhost:" + port + "/v3/api-docs")
            .build();
    }

    @ParameterizedTest
    @MethodSource("documentedEndpoints")
    void endpointMatchesSpec(String method, String path, Map<String, String> params) {
        Response response = executeRequest(method, path, params);

        ValidationReport report = validator.validate(
            SimpleRequest.Builder.get(path).build(),
            SimpleResponse.Builder.status(response.getStatusCode()).build()
        );

        assertThat(report.hasErrors())
            .withFailMessage("OpenAPI violation: %s", report.getMessages())
            .isFalse();
    }

    static Stream<Arguments> documentedEndpoints() {
        // Auto-discovered from OpenAPI spec
        return OpenApiEndpointDiscovery.discover("/v3/api-docs");
    }
}
```

**Runtime Validation (Test Environments):**

```java
// OpenApiValidationFilter.java - Intercepts requests in test/staging
@Component
@Profile({"test", "staging"})
public class OpenApiValidationFilter extends OncePerRequestFilter {

    private final OpenApiInteractionValidator validator;
    private final MeterRegistry meterRegistry;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) {
        ContentCachingResponseWrapper wrappedResponse =
            new ContentCachingResponseWrapper(response);

        chain.doFilter(request, wrappedResponse);

        ValidationReport report = validator.validate(request, wrappedResponse);

        if (report.hasErrors()) {
            meterRegistry.counter("openapi.violations",
                "path", request.getRequestURI()).increment();
            log.warn("OpenAPI violation: {} - {}",
                request.getRequestURI(), report.getMessages());
        }

        wrappedResponse.copyBodyToResponse();
    }
}
```

---

## 4. Phase 2: UI Journey Testing

### 4.1 Journey Test Architecture

**Directory Structure:**

```
apps/clinical-portal/
├── e2e/
│   ├── journeys/                        # NEW: Comprehensive flow tests
│   │   ├── patient/
│   │   │   ├── patient-search.journey.ts
│   │   │   ├── patient-detail-navigation.journey.ts
│   │   │   └── patient-history-drilldown.journey.ts
│   │   ├── dashboard/
│   │   │   ├── metrics-drilldown.journey.ts
│   │   │   ├── care-gap-interaction.journey.ts
│   │   │   └── quality-measure-view.journey.ts
│   │   ├── care-gaps/
│   │   │   ├── gap-identification.journey.ts
│   │   │   ├── gap-closure-workflow.journey.ts
│   │   │   └── gap-reporting.journey.ts
│   │   └── reports/
│   │       ├── hedis-report-generation.journey.ts
│   │       └── export-workflow.journey.ts
│   ├── fixtures/
│   │   ├── patients.fixture.ts          # FHIR-compliant test patients
│   │   ├── care-gaps.fixture.ts
│   │   └── quality-measures.fixture.ts
│   ├── support/
│   │   ├── journey-helpers.ts           # State assertion utilities
│   │   ├── navigation-validator.ts      # URL/breadcrumb validation
│   │   └── data-integrity-checker.ts    # Response data validation
│   └── playwright.config.ts
```

### 4.2 Test Fixtures (FHIR-Compliant UUIDs)

```typescript
// fixtures/patients.fixture.ts
import { v4 as uuidv4 } from 'uuid';

export interface TestPatient {
  id: string;           // FHIR R4: string-based resource ID
  resourceType: 'Patient';
  identifier: Array<{
    system: string;
    value: string;
  }>;
  name: Array<{
    family: string;
    given: string[];
  }>;
  birthDate: string;    // FHIR date format: YYYY-MM-DD
  gender: 'male' | 'female' | 'other' | 'unknown';
}

export class PatientFixtures {
  // Deterministic UUIDs for consistent test data
  static readonly PATIENT_JOHN_DOE = 'f47ac10b-58cc-4372-a567-0e02b2c3d479';
  static readonly PATIENT_JANE_SMITH = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890';
  static readonly PATIENT_WITH_GAPS = '550e8400-e29b-41d4-a716-446655440000';

  static johnDoe(): TestPatient {
    return {
      id: this.PATIENT_JOHN_DOE,
      resourceType: 'Patient',
      identifier: [
        { system: 'http://hospital.example.org/mrn', value: 'MRN-12345' }
      ],
      name: [{ family: 'Doe', given: ['John', 'Michael'] }],
      birthDate: '1980-01-15',
      gender: 'male',
    };
  }

  static patientWithCareGaps(): TestPatient {
    return {
      id: this.PATIENT_WITH_GAPS,
      resourceType: 'Patient',
      identifier: [
        { system: 'http://hospital.example.org/mrn', value: 'MRN-67890' }
      ],
      name: [{ family: 'Williams', given: ['Sarah'] }],
      birthDate: '1975-06-22',
      gender: 'female',
    };
  }

  static randomPatient(): TestPatient {
    return {
      id: uuidv4(),  // Random UUID for isolation
      resourceType: 'Patient',
      identifier: [
        { system: 'http://hospital.example.org/mrn', value: `MRN-${Date.now()}` }
      ],
      name: [{ family: 'Test', given: ['Random'] }],
      birthDate: '1990-03-10',
      gender: 'other',
    };
  }
}
```

### 4.3 Navigation State Validator

```typescript
// support/navigation-validator.ts
import { Page, expect } from '@playwright/test';

export interface NavigationState {
  url: string | RegExp;
  breadcrumbs?: string[];
  pageTitle?: string;
  activeTab?: string;
  loadedData?: {
    selector: string;
    contains?: string;
    resourceId?: string;  // FHIR UUID validation
  };
}

export class NavigationValidator {
  constructor(private page: Page) {}

  async assertState(expected: NavigationState): Promise<void> {
    // URL validation
    if (typeof expected.url === 'string') {
      await expect(this.page).toHaveURL(expected.url);
    } else {
      await expect(this.page).toHaveURL(expected.url);
    }

    // Breadcrumb validation (catches "wrong place after click" issues)
    if (expected.breadcrumbs) {
      const breadcrumbs = await this.page
        .locator('[data-testid="breadcrumb-item"]')
        .allTextContents();
      expect(breadcrumbs).toEqual(expected.breadcrumbs);
    }

    // Page title validation
    if (expected.pageTitle) {
      await expect(this.page.locator('h1, [data-testid="page-title"]'))
        .toContainText(expected.pageTitle);
    }

    // Active navigation tab
    if (expected.activeTab) {
      await expect(this.page.locator('[data-testid="nav-tab"].active'))
        .toContainText(expected.activeTab);
    }

    // Data integrity check
    if (expected.loadedData) {
      const element = this.page.locator(expected.loadedData.selector);
      await expect(element).toBeVisible();

      if (expected.loadedData.contains) {
        await expect(element).toContainText(expected.loadedData.contains);
      }

      if (expected.loadedData.resourceId) {
        // Validate FHIR UUID is displayed correctly
        const content = await element.textContent();
        expect(content).toContain(expected.loadedData.resourceId);
      }
    }
  }

  async assertNoNavigationError(): Promise<void> {
    // Check for common error states
    await expect(this.page.locator('[data-testid="error-boundary"]'))
      .not.toBeVisible();
    await expect(this.page.locator('.error-page, .not-found'))
      .not.toBeVisible();
  }
}
```

### 4.4 Patient Navigation Journey Test

```typescript
// journeys/patient/patient-detail-navigation.journey.ts
import { test, expect, Page } from '@playwright/test';
import { PatientFixtures } from '../../fixtures/patients.fixture';
import { NavigationValidator } from '../../support/navigation-validator';
import { setupTestData, cleanupTestData } from '../../support/test-data-api';

test.describe('Patient Detail Navigation Journey', () => {
  let validator: NavigationValidator;
  const testPatient = PatientFixtures.johnDoe();

  test.beforeAll(async ({ request }) => {
    await setupTestData(request, {
      patients: [testPatient],
      tenant: 'test-tenant-1',
    });
  });

  test.afterAll(async ({ request }) => {
    await cleanupTestData(request, {
      patientIds: [testPatient.id]
    });
  });

  test.beforeEach(async ({ page }) => {
    validator = new NavigationValidator(page);
    await page.goto('/login');
    await loginAsEvaluator(page);
  });

  test('complete patient lookup → detail → history → back navigation', async ({ page }) => {
    // Step 1: Start at patient search
    await page.goto('/patients');
    await validator.assertState({
      url: '/patients',
      pageTitle: 'Patient Search',
      breadcrumbs: ['Home', 'Patients'],
    });

    // Step 2: Search for patient by MRN
    await page.fill('[data-testid="patient-search-input"]', 'MRN-12345');
    await page.click('[data-testid="search-button"]');

    await expect(page.locator('[data-testid="patient-row"]')).toHaveCount(1);
    await expect(page.locator('[data-testid="patient-row"]'))
      .toContainText('Doe, John');

    // Step 3: Click into patient detail
    await page.click(`[data-testid="patient-row-${testPatient.id}"]`);

    await validator.assertState({
      url: new RegExp(`/patients/${testPatient.id}`),
      pageTitle: 'Doe, John Michael',
      breadcrumbs: ['Home', 'Patients', 'Doe, John Michael'],
      loadedData: {
        selector: '[data-testid="patient-id"]',
        resourceId: testPatient.id,
      },
    });
    await validator.assertNoNavigationError();

    // Step 4: Navigate to patient history tab
    await page.click('[data-testid="tab-history"]');

    await validator.assertState({
      url: new RegExp(`/patients/${testPatient.id}/history`),
      activeTab: 'History',
      breadcrumbs: ['Home', 'Patients', 'Doe, John Michael', 'History'],
    });

    // Step 5: Drill into a specific encounter
    await page.click('[data-testid="encounter-row"]:first-child');

    await validator.assertState({
      url: new RegExp(`/patients/${testPatient.id}/encounters/[a-f0-9-]+`),
      breadcrumbs: ['Home', 'Patients', 'Doe, John Michael', 'History', 'Encounter'],
    });

    // Step 6: Back button returns to history (THIS IS WHERE BUGS OCCUR)
    await page.goBack();

    await validator.assertState({
      url: new RegExp(`/patients/${testPatient.id}/history`),
      activeTab: 'History',
      breadcrumbs: ['Home', 'Patients', 'Doe, John Michael', 'History'],
      loadedData: {
        selector: '[data-testid="encounter-list"]',
        contains: 'Encounter',
      },
    });

    // Step 7: Back again returns to patient detail
    await page.goBack();

    await validator.assertState({
      url: new RegExp(`/patients/${testPatient.id}$`),
      pageTitle: 'Doe, John Michael',
      loadedData: {
        selector: '[data-testid="patient-demographics"]',
        contains: '1980-01-15',
      },
    });

    // Step 8: Back to search preserves search state
    await page.goBack();

    await validator.assertState({
      url: '/patients',
      pageTitle: 'Patient Search',
    });

    await expect(page.locator('[data-testid="patient-search-input"]'))
      .toHaveValue('MRN-12345');
  });

  test('browser refresh preserves patient context', async ({ page }) => {
    await page.goto(`/patients/${testPatient.id}`);

    await validator.assertState({
      url: new RegExp(`/patients/${testPatient.id}`),
      loadedData: {
        selector: '[data-testid="patient-id"]',
        resourceId: testPatient.id,
      },
    });

    await page.reload();

    await validator.assertState({
      url: new RegExp(`/patients/${testPatient.id}`),
      pageTitle: 'Doe, John Michael',
      loadedData: {
        selector: '[data-testid="patient-id"]',
        resourceId: testPatient.id,
      },
    });
    await validator.assertNoNavigationError();
  });

  test('direct URL navigation loads correct patient', async ({ page }) => {
    await page.goto(`/patients/${testPatient.id}/history`);

    await validator.assertState({
      url: new RegExp(`/patients/${testPatient.id}/history`),
      breadcrumbs: ['Home', 'Patients', 'Doe, John Michael', 'History'],
      activeTab: 'History',
    });
    await validator.assertNoNavigationError();
  });
});

async function loginAsEvaluator(page: Page): Promise<void> {
  await page.fill('[data-testid="username"]', 'test-evaluator');
  await page.fill('[data-testid="password"]', 'test-password');
  await page.click('[data-testid="login-button"]');
  await expect(page).toHaveURL('/dashboard');
}
```

### 4.5 Dashboard Drill-Down Journey Test

```typescript
// journeys/dashboard/metrics-drilldown.journey.ts
import { test, expect, Page } from '@playwright/test';
import { PatientFixtures } from '../../fixtures/patients.fixture';
import { CareGapFixtures } from '../../fixtures/care-gaps.fixture';
import { NavigationValidator } from '../../support/navigation-validator';
import { setupTestData, cleanupTestData } from '../../support/test-data-api';

test.describe('Dashboard Metrics Drill-Down Journey', () => {
  let validator: NavigationValidator;

  const testPatients = [
    PatientFixtures.johnDoe(),
    PatientFixtures.patientWithCareGaps(),
  ];

  const testCareGaps = CareGapFixtures.openGapsForPatient(
    PatientFixtures.PATIENT_WITH_GAPS,
    ['HBA1C', 'BCS', 'COL']
  );

  test.beforeAll(async ({ request }) => {
    await setupTestData(request, {
      patients: testPatients,
      careGaps: testCareGaps,
      tenant: 'test-tenant-1',
    });
  });

  test.afterAll(async ({ request }) => {
    await cleanupTestData(request, {
      patientIds: testPatients.map(p => p.id),
      careGapIds: testCareGaps.map(g => g.id),
    });
  });

  test.beforeEach(async ({ page }) => {
    validator = new NavigationValidator(page);
    await loginAsEvaluator(page);
  });

  test('dashboard → care gap metric → gap list → patient detail → back', async ({ page }) => {
    // Step 1: Verify dashboard loaded with correct metrics
    await page.goto('/dashboard');

    await validator.assertState({
      url: '/dashboard',
      pageTitle: 'Quality Dashboard',
      breadcrumbs: ['Home', 'Dashboard'],
    });

    const careGapMetric = page.locator('[data-testid="metric-open-care-gaps"]');
    await expect(careGapMetric).toBeVisible();
    const gapCount = await careGapMetric.locator('[data-testid="metric-value"]').textContent();
    expect(parseInt(gapCount || '0')).toBeGreaterThanOrEqual(3);

    // Step 2: Click care gap metric to drill down
    await careGapMetric.click();

    await validator.assertState({
      url: '/care-gaps?status=open',
      pageTitle: 'Open Care Gaps',
      breadcrumbs: ['Home', 'Dashboard', 'Care Gaps'],
      loadedData: {
        selector: '[data-testid="care-gap-table"]',
        contains: 'Williams, Sarah',
      },
    });
    await validator.assertNoNavigationError();

    // Step 3: Verify filtered data matches dashboard count
    const tableRows = page.locator('[data-testid="care-gap-row"]');
    await expect(tableRows).toHaveCount(parseInt(gapCount || '3'));

    // Step 4: Click specific care gap row
    const hba1cGap = page.locator('[data-testid="care-gap-row"]')
      .filter({ hasText: 'HBA1C' })
      .first();
    await hba1cGap.click();

    await validator.assertState({
      url: new RegExp(`/care-gaps/[a-f0-9-]+`),
      breadcrumbs: ['Home', 'Dashboard', 'Care Gaps', 'HBA1C'],
      loadedData: {
        selector: '[data-testid="gap-patient-name"]',
        contains: 'Williams, Sarah',
      },
    });

    // Step 5: Click patient link from care gap detail
    await page.click('[data-testid="gap-patient-link"]');

    await validator.assertState({
      url: new RegExp(`/patients/${PatientFixtures.PATIENT_WITH_GAPS}`),
      pageTitle: 'Williams, Sarah',
      breadcrumbs: ['Home', 'Patients', 'Williams, Sarah'],
      loadedData: {
        selector: '[data-testid="patient-id"]',
        resourceId: PatientFixtures.PATIENT_WITH_GAPS,
      },
    });

    // Step 6: Back button returns to care gap detail (NOT dashboard)
    await page.goBack();

    await validator.assertState({
      url: new RegExp(`/care-gaps/[a-f0-9-]+`),
      loadedData: {
        selector: '[data-testid="gap-measure-id"]',
        contains: 'HBA1C',
      },
    });

    // Step 7: Back to care gap list preserves filter
    await page.goBack();

    await validator.assertState({
      url: '/care-gaps?status=open',
      loadedData: {
        selector: '[data-testid="active-filter"]',
        contains: 'Open',
      },
    });

    // Step 8: Back to dashboard
    await page.goBack();

    await validator.assertState({
      url: '/dashboard',
      pageTitle: 'Quality Dashboard',
    });
  });

  test('quality measure chart → measure detail shows correct data', async ({ page }) => {
    await page.goto('/dashboard');

    const measureChart = page.locator('[data-testid="quality-measure-chart"]');
    await expect(measureChart).toBeVisible();

    await page.click('[data-testid="chart-segment-BCS"]');

    await validator.assertState({
      url: '/quality-measures/BCS',
      pageTitle: 'Breast Cancer Screening',
      breadcrumbs: ['Home', 'Dashboard', 'Quality Measures', 'BCS'],
    });

    await expect(page.locator('[data-testid="measure-denominator"]')).toBeVisible();
    await expect(page.locator('[data-testid="measure-numerator"]')).toBeVisible();
    await expect(page.locator('[data-testid="eligible-patients-table"]')).toBeVisible();

    await validator.assertNoNavigationError();
  });

  test('dashboard widget data matches drill-down list count', async ({ page }) => {
    await page.goto('/dashboard');

    const metrics = {
      openGaps: await page.locator('[data-testid="metric-open-care-gaps"] [data-testid="metric-value"]').textContent(),
      patientsAtRisk: await page.locator('[data-testid="metric-patients-at-risk"] [data-testid="metric-value"]').textContent(),
      pendingReviews: await page.locator('[data-testid="metric-pending-reviews"] [data-testid="metric-value"]').textContent(),
    };

    for (const [metricKey, expectedCount] of Object.entries(metrics)) {
      if (!expectedCount) continue;

      await page.click(`[data-testid="metric-${metricKey.replace(/([A-Z])/g, '-$1').toLowerCase()}"]`);

      const rowCount = await page.locator('[data-testid$="-row"]').count();
      expect(rowCount).toBe(parseInt(expectedCount));

      await page.goBack();
      await expect(page).toHaveURL('/dashboard');
    }
  });
});

async function loginAsEvaluator(page: Page): Promise<void> {
  await page.goto('/login');
  await page.fill('[data-testid="username"]', 'test-evaluator');
  await page.fill('[data-testid="password"]', 'test-password');
  await page.click('[data-testid="login-button"]');
  await expect(page).toHaveURL('/dashboard');
}
```

---

## 5. Phase 3: Visual Regression

### 5.1 BackstopJS Configuration

```javascript
// backstop/backstop.config.js
const scenarios = [
  ...require('./scenarios/dashboard.scenarios'),
  ...require('./scenarios/patient.scenarios'),
  ...require('./scenarios/care-gaps.scenarios'),
  ...require('./scenarios/reports.scenarios'),
];

module.exports = {
  id: 'hdim-clinical-portal',
  viewports: [
    { label: 'desktop', width: 1920, height: 1080 },
    { label: 'laptop', width: 1366, height: 768 },
    { label: 'tablet', width: 1024, height: 768 },
  ],
  scenarios,
  paths: {
    bitmaps_reference: 'backstop/baselines',
    bitmaps_test: 'backstop/reports/bitmaps_test',
    html_report: 'backstop/reports/html',
    ci_report: 'backstop/reports/ci',
  },
  engine: 'playwright',
  engineOptions: {
    browser: 'chromium',
    args: ['--no-sandbox'],
  },
  asyncCaptureLimit: 5,
  asyncCompareLimit: 50,
  debug: false,
  debugWindow: false,

  // HIPAA: No external services, all local
  report: ['browser', 'CI'],

  // Stability settings
  misMatchThreshold: 0.1,
  requireSameDimensions: true,
};
```

### 5.2 Scenario Definitions

```javascript
// backstop/scenarios/dashboard.scenarios.js
const baseUrl = process.env.BACKSTOP_BASE_URL || 'http://localhost:4200';

module.exports = [
  {
    label: 'Dashboard - Initial Load',
    url: `${baseUrl}/dashboard`,
    onBeforeScript: 'onBefore.js',
    onReadyScript: 'onReady.js',
    selectors: ['[data-testid="dashboard-container"]'],
    delay: 1000,
    postInteractionWait: 500,
  },
  {
    label: 'Dashboard - Care Gap Widget',
    url: `${baseUrl}/dashboard`,
    onBeforeScript: 'onBefore.js',
    onReadyScript: 'onReady.js',
    selectors: ['[data-testid="metric-open-care-gaps"]'],
    hoverSelector: '[data-testid="metric-open-care-gaps"]',
    postInteractionWait: 300,
  },
  {
    label: 'Dashboard - Quality Measure Chart',
    url: `${baseUrl}/dashboard`,
    onBeforeScript: 'onBefore.js',
    onReadyScript: 'onReady.js',
    selectors: ['[data-testid="quality-measure-chart"]'],
    delay: 1500,
  },
  {
    label: 'Dashboard - Mobile Responsive',
    url: `${baseUrl}/dashboard`,
    onBeforeScript: 'onBefore.js',
    onReadyScript: 'onReady.js',
    selectors: ['viewport'],
    viewports: [{ label: 'mobile', width: 375, height: 667 }],
  },
];
```

```javascript
// backstop/scenarios/patient.scenarios.js
const baseUrl = process.env.BACKSTOP_BASE_URL || 'http://localhost:4200';
const testPatientId = 'f47ac10b-58cc-4372-a567-0e02b2c3d479';

module.exports = [
  {
    label: 'Patient Search - Empty State',
    url: `${baseUrl}/patients`,
    onBeforeScript: 'onBefore.js',
    onReadyScript: 'onReady.js',
    selectors: ['[data-testid="patient-search-container"]'],
  },
  {
    label: 'Patient Search - With Results',
    url: `${baseUrl}/patients?search=Doe`,
    onBeforeScript: 'onBefore.js',
    onReadyScript: 'onReady.js',
    selectors: ['[data-testid="patient-results-table"]'],
    delay: 500,
  },
  {
    label: 'Patient Detail - Demographics',
    url: `${baseUrl}/patients/${testPatientId}`,
    onBeforeScript: 'onBefore.js',
    onReadyScript: 'onReady.js',
    selectors: ['[data-testid="patient-demographics"]'],
  },
  {
    label: 'Patient Detail - Full Page',
    url: `${baseUrl}/patients/${testPatientId}`,
    onBeforeScript: 'onBefore.js',
    onReadyScript: 'onReady.js',
    selectors: ['[data-testid="patient-detail-container"]'],
  },
  {
    label: 'Patient History Tab',
    url: `${baseUrl}/patients/${testPatientId}/history`,
    onBeforeScript: 'onBefore.js',
    onReadyScript: 'onReady.js',
    selectors: ['[data-testid="patient-history-container"]'],
  },
];
```

### 5.3 Helper Scripts

```javascript
// backstop/scripts/onBefore.js
module.exports = async (page, scenario, viewport, isReference, browserContext) => {
  await page.goto('http://localhost:4200/login');
  await page.fill('[data-testid="username"]', 'visual-test-user');
  await page.fill('[data-testid="password"]', 'visual-test-password');
  await page.click('[data-testid="login-button"]');

  await page.waitForURL('**/dashboard', { timeout: 10000 });

  await page.evaluate(() => {
    localStorage.setItem('hdim-tenant', 'visual-test-tenant');
  });
};
```

```javascript
// backstop/scripts/onReady.js
module.exports = async (page, scenario, viewport) => {
  // Wait for Angular to stabilize
  await page.waitForFunction(() => {
    const angular = (window as any).getAllAngularTestabilities?.();
    return angular?.every((t: any) => t.isStable());
  }, { timeout: 10000 }).catch(() => {});

  await page.waitForLoadState('networkidle');

  // Hide dynamic content
  await page.addStyleTag({
    content: `
      [data-testid="current-timestamp"],
      [data-testid="session-timer"],
      [data-testid="last-updated"] {
        visibility: hidden !important;
      }
    `,
  });

  await page.waitForTimeout(500);
};
```

### 5.4 Git LFS and NPM Scripts

```bash
# backstop/baselines/.gitattributes
*.png filter=lfs diff=lfs merge=lfs -text
*.jpg filter=lfs diff=lfs merge=lfs -text
```

```json
// package.json additions
{
  "scripts": {
    "visual:reference": "backstop reference --config=backstop/backstop.config.js",
    "visual:test": "backstop test --config=backstop/backstop.config.js",
    "visual:approve": "backstop approve --config=backstop/backstop.config.js",
    "visual:report": "backstop openReport --config=backstop/backstop.config.js"
  }
}
```

---

## 6. CI/CD Integration

### 6.1 Complete GitHub Actions Workflow

```yaml
# .github/workflows/comprehensive-testing.yml
name: Comprehensive Testing Pipeline

on:
  pull_request:
    branches: [main, master, develop]
  push:
    branches: [main, master]

env:
  PACT_BROKER_URL: ${{ secrets.PACT_BROKER_URL }}
  PACT_BROKER_TOKEN: ${{ secrets.PACT_BROKER_TOKEN }}

jobs:
  # ============================================
  # Stage 1: Change Detection (existing Phase 7)
  # ============================================
  detect-changes:
    runs-on: ubuntu-latest
    outputs:
      backend: ${{ steps.filter.outputs.backend }}
      frontend: ${{ steps.filter.outputs.frontend }}
      contracts: ${{ steps.filter.outputs.contracts }}
      patient-service: ${{ steps.filter.outputs.patient-service }}
      care-gap-service: ${{ steps.filter.outputs.care-gap-service }}
    steps:
      - uses: actions/checkout@v4
      - uses: dorny/paths-filter@v2
        id: filter
        with:
          filters: |
            backend:
              - 'backend/**'
            frontend:
              - 'apps/clinical-portal/**'
            contracts:
              - 'backend/**/contracts/**'
              - 'apps/clinical-portal/src/test/contracts/**'
            patient-service:
              - 'backend/modules/services/patient-service/**'
            care-gap-service:
              - 'backend/modules/services/care-gap-service/**'

  # ============================================
  # Stage 2: Contract Testing (NEW - Phase 1)
  # ============================================
  contract-consumer-tests:
    needs: detect-changes
    if: needs.detect-changes.outputs.frontend == 'true' || needs.detect-changes.outputs.contracts == 'true'
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
        working-directory: apps/clinical-portal
        run: npm run pact:publish
        env:
          PACT_BROKER_URL: ${{ env.PACT_BROKER_URL }}
          PACT_BROKER_TOKEN: ${{ env.PACT_BROKER_TOKEN }}
          GIT_COMMIT: ${{ github.sha }}
          GIT_BRANCH: ${{ github.head_ref || github.ref_name }}

  contract-provider-verification:
    needs: [detect-changes, contract-consumer-tests]
    if: always() && (needs.detect-changes.outputs.backend == 'true' || needs.detect-changes.outputs.contracts == 'true')
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [patient-service, care-gap-service, quality-measure-service]
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'gradle'

      - name: Verify Provider Contracts
        working-directory: backend
        run: |
          ./gradlew :modules:services:${{ matrix.service }}:pactVerify \
            -Dpact.verifier.publishResults=true \
            -Dpact.provider.branch=${{ github.head_ref || github.ref_name }}
        env:
          PACT_BROKER_URL: ${{ env.PACT_BROKER_URL }}
          PACT_BROKER_TOKEN: ${{ env.PACT_BROKER_TOKEN }}

  openapi-validation:
    needs: detect-changes
    if: needs.detect-changes.outputs.backend == 'true'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'gradle'

      - name: Validate OpenAPI Specs
        working-directory: backend
        run: ./gradlew validateOpenApi

      - name: Upload Violation Report
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: openapi-violations
          path: backend/build/reports/openapi/

  # ============================================
  # Stage 3: Existing Unit/Integration Tests
  # ============================================
  backend-tests:
    needs: detect-changes
    if: needs.detect-changes.outputs.backend == 'true'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'gradle'

      - name: Run Backend Tests
        working-directory: backend
        run: ./gradlew testAll

  frontend-unit-tests:
    needs: detect-changes
    if: needs.detect-changes.outputs.frontend == 'true'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: apps/clinical-portal/package-lock.json

      - name: Install and Test
        working-directory: apps/clinical-portal
        run: |
          npm ci
          npm run test:ci

  # ============================================
  # Stage 4: Journey & Visual Tests (NEW - Phase 2 & 3)
  # ============================================
  journey-tests:
    needs: [detect-changes, backend-tests, frontend-unit-tests]
    if: always() && needs.detect-changes.outputs.frontend == 'true'
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: apps/clinical-portal/package-lock.json

      - name: Install Playwright Browsers
        working-directory: apps/clinical-portal
        run: |
          npm ci
          npx playwright install --with-deps chromium

      - name: Start Backend Services
        run: docker compose -f docker-compose.test.yml up -d

      - name: Wait for Services
        run: ./scripts/wait-for-services.sh

      - name: Run Journey Tests
        working-directory: apps/clinical-portal
        run: npm run test:journeys

      - name: Upload Journey Test Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: journey-test-report
          path: apps/clinical-portal/playwright-report/

  visual-regression:
    needs: [detect-changes, journey-tests]
    if: always() && needs.detect-changes.outputs.frontend == 'true'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          lfs: true

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: apps/clinical-portal/package-lock.json

      - name: Install Dependencies
        working-directory: apps/clinical-portal
        run: |
          npm ci
          npx playwright install chromium

      - name: Start Application
        run: docker compose -f docker-compose.test.yml up -d

      - name: Wait for Services
        run: ./scripts/wait-for-services.sh

      - name: Run Visual Regression Tests
        working-directory: apps/clinical-portal
        run: npm run visual:test
        env:
          BACKSTOP_BASE_URL: http://localhost:4200

      - name: Upload Visual Diff Report
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: visual-regression-report
          path: apps/clinical-portal/backstop/reports/

  # ============================================
  # Stage 5: Merge Gate
  # ============================================
  merge-gate:
    needs: [
      contract-consumer-tests,
      contract-provider-verification,
      openapi-validation,
      backend-tests,
      frontend-unit-tests,
      journey-tests,
      visual-regression
    ]
    if: always()
    runs-on: ubuntu-latest
    steps:
      - name: Check All Jobs
        run: |
          if [[ "${{ needs.contract-consumer-tests.result }}" == "failure" ]] || \
             [[ "${{ needs.contract-provider-verification.result }}" == "failure" ]] || \
             [[ "${{ needs.openapi-validation.result }}" == "failure" ]] || \
             [[ "${{ needs.backend-tests.result }}" == "failure" ]] || \
             [[ "${{ needs.frontend-unit-tests.result }}" == "failure" ]] || \
             [[ "${{ needs.journey-tests.result }}" == "failure" ]] || \
             [[ "${{ needs.visual-regression.result }}" == "failure" ]]; then
            echo "One or more required jobs failed"
            exit 1
          fi
          echo "All required jobs passed or were skipped"
```

---

## 7. Phased Rollout Plan

### Phase 1: Contract Testing Foundation (Weeks 1-3)

| Week | Deliverable | Success Criteria |
|------|-------------|------------------|
| **Week 1** | Pact Broker setup + infrastructure | Broker running in Docker, accessible to CI |
| **Week 1** | Shared contract-testing module | `backend/modules/shared/contract-testing` compiles |
| **Week 2** | Angular consumer tests (Patient Service) | 5+ consumer expectations published to broker |
| **Week 2** | Patient Service provider verification | Provider verifies all consumer contracts |
| **Week 3** | Care Gap Service contracts (both directions) | Consumer + provider tests passing |
| **Week 3** | OpenAPI validation in CI | All 62 endpoints validated, CI gate active |

**Exit Criteria:**
- [ ] Pact Broker running and integrated with CI
- [ ] 3 critical service boundaries covered
- [ ] OpenAPI validation blocking PRs on spec violations
- [ ] Zero false positives for 1 week

### Phase 2: UI Journey Testing (Weeks 4-6)

| Week | Deliverable | Success Criteria |
|------|-------------|------------------|
| **Week 4** | Test fixtures + navigation validator | FHIR-compliant fixtures, validator utility working |
| **Week 4** | Patient navigation journey (3 tests) | Search→Detail→History→Back flow covered |
| **Week 5** | Dashboard drill-down journey (3 tests) | Metric→List→Detail→Back flow covered |
| **Week 5** | Care gap workflow journey (3 tests) | Identify→View→Close flow covered |
| **Week 6** | Report generation journey (2 tests) | HEDIS report + export flows covered |
| **Week 6** | CI integration + flakiness stabilization | Journey tests in CI, <5% flake rate |

**Exit Criteria:**
- [ ] 11+ journey tests covering all major workflows
- [ ] Patient navigation and dashboard issues reproduced and passing
- [ ] Tests integrated into CI with <5% flake rate
- [ ] Average journey test runtime <3 minutes

### Phase 3: Visual Regression (Weeks 7-8)

| Week | Deliverable | Success Criteria |
|------|-------------|------------------|
| **Week 7** | BackstopJS setup + Git LFS | Config complete, baselines stored in LFS |
| **Week 7** | Dashboard visual scenarios (5 scenarios) | All viewports, hover states captured |
| **Week 8** | Patient views visual scenarios (5 scenarios) | Search, detail, history views covered |
| **Week 8** | CI integration + baseline management | Visual tests in CI, approval workflow documented |

**Exit Criteria:**
- [ ] 10+ visual scenarios covering critical screens
- [ ] Baselines approved and committed to Git LFS
- [ ] CI blocks PRs on visual regressions
- [ ] Approval workflow documented for intentional changes

### Timeline Summary

```
Week 1  ████░░░░░░░░░░░░  Pact Broker + Infrastructure
Week 2  ████████░░░░░░░░  Consumer Tests + First Provider
Week 3  ████████████░░░░  Full Contract Coverage + OpenAPI
Week 4  ░░░░████░░░░░░░░  Journey Fixtures + Patient Nav
Week 5  ░░░░████████░░░░  Dashboard + Care Gap Journeys
Week 6  ░░░░████████████  Reports + CI Stabilization
Week 7  ░░░░░░░░████░░░░  BackstopJS Setup + Dashboard Visual
Week 8  ░░░░░░░░████████  Patient Visual + CI Integration

        ─────────────────────────────────────────────────
        Phase 1: Contracts    Phase 2: Journeys    Phase 3: Visual
```

---

## 8. Success Metrics

### Expected Impact

| Metric | Before | After Phase 1 | After Phase 2 | After Phase 3 |
|--------|--------|---------------|---------------|---------------|
| **API breaking changes caught** | ~30% (in staging) | ~90% (in CI) | ~90% | ~90% |
| **UI navigation bugs caught** | ~20% | ~20% | ~85% | ~85% |
| **Visual regressions caught** | ~5% | ~5% | ~5% | ~80% |
| **PR feedback time** | 23-25 min | 25-28 min | 28-32 min | 32-38 min |
| **Production incidents (UI/API)** | Baseline | -50% | -75% | -85% |

### Trade-offs

PR feedback time increases ~10-15 minutes, but production incident reduction justifies the investment.

### Compliance Benefits

- **HIPAA Audit Trail**: All test data uses FHIR-compliant UUIDs
- **Self-Hosted Infrastructure**: No PHI sent to external services
- **Reproducible Evidence**: Git LFS stores visual baselines for audit

---

## Appendix: File Reference

### New Files to Create

```
backend/
├── modules/shared/
│   ├── contract-testing/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/healthdata/contracts/
│   │       ├── PactBrokerConfig.java
│   │       └── ContractTestBase.java
│   └── openapi-validation/
│       ├── build.gradle.kts
│       └── src/main/java/com/healthdata/openapi/
│           ├── OpenApiValidationFilter.java
│           └── OpenApiTestValidator.java

apps/clinical-portal/
├── e2e/
│   ├── journeys/
│   │   ├── patient/
│   │   ├── dashboard/
│   │   ├── care-gaps/
│   │   └── reports/
│   ├── fixtures/
│   │   ├── patients.fixture.ts
│   │   ├── care-gaps.fixture.ts
│   │   └── quality-measures.fixture.ts
│   └── support/
│       ├── journey-helpers.ts
│       ├── navigation-validator.ts
│       └── data-integrity-checker.ts
├── backstop/
│   ├── backstop.config.js
│   ├── scenarios/
│   ├── scripts/
│   └── baselines/
└── src/test/contracts/
    ├── patient-service.consumer.spec.ts
    └── care-gap-service.consumer.spec.ts

.github/workflows/
└── comprehensive-testing.yml
```

### Modified Files

```
docker-compose.test.yml     # Add Pact Broker
package.json                # Add visual test scripts
settings.gradle.kts         # Include new shared modules
```

---

**Document Status:** Ready for Approval
**Next Steps:** Upon approval, use `superpowers:using-git-worktrees` to create isolated workspace and `superpowers:writing-plans` to create detailed implementation plan.
