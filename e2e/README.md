# HDIM Unified E2E Testing Framework

End-to-end testing framework for HDIM Clinical Portal (Angular) and Sales Portal (React).

## Overview

This unified E2E testing framework provides comprehensive test coverage for all customer workflows in the HDIM platform. Built with Playwright, it supports:

- Cross-browser testing (Chromium, Firefox, WebKit)
- Mobile device testing (iOS, Android)
- Accessibility testing
- Performance testing
- HIPAA-compliant PHI handling

## Directory Structure

```
e2e/
├── fixtures/              # Custom Playwright fixtures
│   └── test-fixtures.ts   # HDIM-specific test fixtures
├── pages/                 # Page Object Models
│   ├── base.page.ts       # Base page class
│   ├── login.page.ts      # Login page
│   ├── dashboard.page.ts  # Dashboard page
│   ├── patient-search.page.ts
│   ├── evaluation.page.ts
│   └── care-gap.page.ts
├── tests/                 # Test specifications
│   ├── workflows/         # Workflow-based tests
│   │   ├── authentication/
│   │   ├── patient/
│   │   ├── quality-measure/
│   │   ├── care-gap/
│   │   ├── events/
│   │   └── reporting/
│   ├── security/          # Security tests
│   ├── accessibility/     # A11y tests
│   └── performance/       # Performance tests
├── utils/                 # Utilities
│   ├── api-helpers.ts     # API interaction helpers
│   ├── websocket-helpers.ts
│   ├── phi-masking.ts     # HIPAA PHI masking
│   └── test-data-factory.ts
├── reporters/             # Custom reporters
├── playwright.config.ts   # Playwright configuration
├── global.setup.ts        # Global test setup
└── global.teardown.ts     # Global test teardown
```

## Quick Start

### Prerequisites

- Node.js 18+
- Docker (for backend services)

### Installation

```bash
cd e2e
npm install
npx playwright install
```

### Running Tests

```bash
# Run all tests
npm test

# Run with browser visible
npm run test:headed

# Run with Playwright UI
npm run test:ui

# Run specific browser
npm run test:chromium
npm run test:firefox
npm run test:webkit

# Run specific workflow
npm run test:auth
npm run test:patient
npm run test:evaluation
npm run test:care-gap

# Run accessibility tests
npm run test:accessibility

# Run mobile tests
npm run test:mobile
```

### Viewing Reports

```bash
npm run report
```

## Test Users

| Username | Password | Role |
|----------|----------|------|
| test_superadmin | password123 | SUPER_ADMIN |
| test_admin | password123 | ADMIN |
| test_evaluator | password123 | EVALUATOR |
| test_analyst | password123 | ANALYST |
| test_viewer | password123 | VIEWER |

## Workflow Test Coverage

### Authentication (AUTH)
- AUTH-001: Valid login
- AUTH-002: Invalid credentials
- AUTH-003: Session expiration
- AUTH-004: Role-based access
- AUTH-005: Logout

### Patient (PAT)
- PAT-001: Search by name
- PAT-002: Search by MRN
- PAT-003: Patient selection

### Evaluation (EVAL)
- EVAL-001: Single patient evaluation
- EVAL-002: Measure selection
- EVAL-003: Result interpretation
- EVAL-004: Care gap creation
- EVAL-005: Batch evaluation

### Care Gap (CG)
- CG-001: View all care gaps
- CG-002: Filter by urgency
- CG-003: Filter by status
- CG-004: View gap details
- CG-005: Record intervention
- CG-006: Close care gap
- CG-007: Bulk close
- CG-008: Quick actions

## HIPAA Compliance

### PHI Masking

All test utilities include automatic PHI masking:

```typescript
import { PHIMasking } from './utils/phi-masking';

const masking = new PHIMasking({ enabled: true });
const result = masking.mask('Patient SSN: 123-45-6789');
// Output: "Patient SSN: ***-**-****"
```

### Test Data

Use the `TestDataFactory` for synthetic data:

```typescript
import { TestDataFactory } from './utils/test-data-factory';

const factory = new TestDataFactory({ phiMasking: null });
const patient = factory.createPatient();
// Creates: { firstName: 'Test_Alice', lastName: 'Synthetic', ... }
```

## Writing Tests

### Using Page Objects

```typescript
import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/login.page';
import { DashboardPage } from '../pages/dashboard.page';

test('should login and view dashboard', async ({ page }) => {
  const loginPage = new LoginPage(page);
  const dashboardPage = new DashboardPage(page);

  await loginPage.goto();
  await loginPage.login('test_evaluator', 'password123');
  await dashboardPage.assertMetricsDisplayed();
});
```

### Using Custom Fixtures

```typescript
import { test } from '../fixtures/test-fixtures';

test('should run evaluation as evaluator', async ({ evaluatorPage, apiHelpers }) => {
  // evaluatorPage is pre-authenticated as evaluator
  await evaluatorPage.goto('/evaluations');

  // apiHelpers for direct backend interaction
  const measures = await apiHelpers.getQualityMeasures();
});
```

### Event-Driven Testing

```typescript
import { test } from '../fixtures/test-fixtures';

test('should receive WebSocket updates', async ({ wsHelpers, page }) => {
  // Wait for specific event
  const event = await wsHelpers.waitForEvaluationComplete('patient-123');

  // Collect events during action
  const { events } = await wsHelpers.collectEventsDuring(
    ['EVALUATION_COMPLETE', 'CARE_GAP_CREATED'],
    async () => {
      await page.click('[data-testid="evaluate-button"]');
    }
  );
});
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `BASE_URL` | http://localhost:4200 | Clinical Portal URL |
| `API_BASE_URL` | http://localhost:8087 | API base URL |
| `CI` | false | CI environment flag |
| `PHI_MASKING` | true | Enable PHI masking |
| `CLEANUP_TEST_DATA` | false | Clean up after tests |

### Custom Options

Configure in `playwright.config.ts`:

```typescript
export default defineConfig({
  use: {
    // Custom HDIM options
    defaultRole: 'EVALUATOR',
    enablePHIMasking: true,
    wsEndpoint: 'ws://localhost:8087/ws',
    apiBaseUrl: 'http://localhost:8087',
  },
});
```

## CI Integration

Tests are configured to run in CI with:
- 2 retries on failure
- 4 parallel workers
- GitHub Actions reporter
- JUnit/JSON output for reporting

```yaml
# GitHub Actions example
- name: Run E2E Tests
  run: |
    cd e2e
    npm ci
    npx playwright install --with-deps
    npm test
```

## Debugging

```bash
# Run with debug mode
npm run test:debug

# Generate code with Playwright Inspector
npm run codegen

# View trace on failure
npx playwright show-trace test-results/trace.zip
```

## Related Documentation

- [UI Testing Implementation Plan](../docs/UI_TESTING_IMPLEMENTATION_PLAN.md)
- [Quality Evaluation Workflow](../documentation-site/workflows/quality-evaluation.md)
- [Care Gap Closure Workflow](../documentation-site/workflows/care-gap-closure.md)
- [Testing Guide](../TESTING_GUIDE.md)
