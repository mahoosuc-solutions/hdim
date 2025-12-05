# HealthData Platform E2E Test Suite

Comprehensive end-to-end testing suite for the HealthData Platform built with Playwright.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Running Tests](#running-tests)
- [Test Categories](#test-categories)
- [Test Structure](#test-structure)
- [Configuration](#configuration)
- [CI/CD Integration](#cicd-integration)
- [Writing Tests](#writing-tests)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

## 🎯 Overview

This E2E test suite provides comprehensive coverage of the HealthData Platform, including:

- **60+ test scenarios** across all major workflows
- **Multi-browser testing** (Chrome, Firefox, Safari, Edge)
- **Mobile device testing** (iOS, Android)
- **Accessibility compliance** (WCAG 2.1 Level AA)
- **Performance benchmarking**
- **API integration testing**
- **Security validation**

## ✨ Features

### Test Coverage

- ✅ Patient management workflows
- ✅ Quality measure calculations
- ✅ Care gap detection and closure
- ✅ Authentication and authorization
- ✅ Performance benchmarks
- ✅ Accessibility compliance
- ✅ API integration
- ✅ Security testing

### Browser & Device Support

- Chrome (Desktop & Mobile)
- Firefox
- Safari (Desktop & Mobile)
- Edge
- Tablet (iPad)

### Reporting

- HTML reports with screenshots
- JUnit XML for CI/CD
- JSON results for custom processing
- Allure reports (optional)
- GitHub Actions integration

## 📦 Prerequisites

- Node.js >= 18.0.0
- Java 21 (for backend)
- Docker & Docker Compose (optional)
- Git

## 🚀 Installation

### 1. Install Dependencies

```bash
cd e2e
npm install
```

### 2. Install Playwright Browsers

```bash
npm run test:install
```

This will install Chrome, Firefox, and WebKit browsers along with their system dependencies.

### 3. Set Up Environment

Create a `.env` file in the `e2e` directory:

```env
BASE_URL=http://localhost:8080
HEADLESS=false
WORKERS=4
TIMEOUT=30000
```

### 4. Start the Backend

```bash
# From project root
./start.sh

# Or using Docker
docker compose up -d
```

## 🧪 Running Tests

### Run All Tests

```bash
npm test
```

### Run Specific Test Suite

```bash
# Patient workflow tests
npm test -- patient-workflow.spec.ts

# Quality measure tests
npm test -- quality-measure-workflow.spec.ts

# Care gap tests
npm test -- care-gap-workflow.spec.ts

# Security tests
npm test -- security.spec.ts

# Performance tests
npm test -- performance.spec.ts

# Accessibility tests
npm test -- accessibility.spec.ts

# API integration tests
npm test -- api-integration.spec.ts
```

### Run Tests by Tag

```bash
# Workflow tests
npm run test:workflow

# Security tests
npm run test:security

# Performance tests
npm run test:performance

# Accessibility tests
npm run test:accessibility

# API tests
npm run test:api
```

### Run Tests in Specific Browser

```bash
# Chrome only
npm run test:chromium

# Firefox only
npm run test:firefox

# Safari only
npm run test:webkit

# Mobile devices
npm run test:mobile
```

### Run in Headed Mode (with browser UI)

```bash
npm run test:headed
```

### Run in Debug Mode

```bash
npm run test:debug
```

### Run with UI Mode (Interactive)

```bash
npm run test:ui
```

### Run CI Tests

```bash
npm run test:ci
```

## 📚 Test Categories

### 1. Patient Workflow Tests (`patient-workflow.spec.ts`)

**800+ lines** covering:

- Patient registration and creation
- Search and filtering
- Patient detail viewing and editing
- Data validation
- Bulk operations
- Tenant isolation
- Demographics validation

**Example:**

```typescript
test('@workflow should complete full patient registration flow', async ({ adminApiClient }) => {
  const patient = {
    firstName: 'John',
    lastName: 'Smith',
    mrn: 'MRN-12345',
    dateOfBirth: '1980-01-15',
    gender: 'male',
    tenantId: 'test-tenant-001',
  };

  const response = await adminApiClient.post('/api/patients', { data: patient });
  expect(response.ok()).toBeTruthy();
});
```

### 2. Quality Measure Workflow Tests (`quality-measure-workflow.spec.ts`)

**700+ lines** covering:

- Individual measure calculations
- Batch calculations
- HEDIS measures
- Compliance tracking
- Measure trending
- Performance monitoring

**Example:**

```typescript
test('@workflow should calculate HbA1c control measure', async ({ adminApiClient, testData }) => {
  const patient = await testData.createPatient();

  const response = await adminApiClient.post(
    `/api/measures/calculate?patientId=${patient.id}&measureId=hba1c-control`
  );

  expect(response.ok()).toBeTruthy();
});
```

### 3. Care Gap Workflow Tests (`care-gap-workflow.spec.ts`)

**700+ lines** covering:

- Gap detection
- Gap prioritization
- Gap closure workflows
- Bulk operations
- Analytics and reporting
- Intervention tracking

### 4. Security Tests (`security.spec.ts`)

**500+ lines** covering:

- Authentication flows
- Authorization (RBAC)
- Session management
- Token security
- Tenant isolation
- HIPAA compliance
- Rate limiting
- Input validation

### 5. Performance Tests (`performance.spec.ts`)

**400+ lines** covering:

- API response times
- Concurrent request handling
- Database query performance
- Cache effectiveness
- Throughput testing
- Latency benchmarks (P50, P95, P99)

**Performance Targets:**

- Health check: < 100ms
- Patient list: < 500ms
- Patient details: < 200ms
- Measure calculation: < 1000ms

### 6. Accessibility Tests (`accessibility.spec.ts`)

**400+ lines** covering:

- WCAG 2.1 Level AA compliance
- Keyboard navigation
- Screen reader compatibility
- Color contrast
- Focus management
- ARIA attributes
- Form accessibility
- Mobile touch targets

### 7. API Integration Tests (`api-integration.spec.ts`)

**500+ lines** covering:

- REST API endpoints
- FHIR API
- Request/response validation
- Error handling
- Data consistency
- API versioning
- Bulk operations

## 📁 Test Structure

```
e2e/
├── fixtures/
│   └── test-fixtures.ts          # Custom fixtures and helpers
├── tests/
│   ├── patient-workflow.spec.ts  # Patient management tests
│   ├── quality-measure-workflow.spec.ts  # Quality measure tests
│   ├── care-gap-workflow.spec.ts # Care gap tests
│   ├── security.spec.ts          # Security tests
│   ├── performance.spec.ts       # Performance tests
│   ├── accessibility.spec.ts     # Accessibility tests
│   └── api-integration.spec.ts   # API integration tests
├── .auth/                        # Authentication state files (generated)
├── test-results/                 # Test results (generated)
├── playwright-report/            # HTML reports (generated)
├── global.setup.ts               # Global setup
├── global.teardown.ts            # Global teardown
├── playwright.config.ts          # Playwright configuration
├── package.json                  # Dependencies
└── README.md                     # This file
```

## ⚙️ Configuration

### Playwright Config (`playwright.config.ts`)

```typescript
export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: [
    ['html'],
    ['junit', { outputFile: 'junit.xml' }],
    ['json', { outputFile: 'test-results.json' }],
  ],
  use: {
    baseURL: 'http://localhost:8080',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium' },
    { name: 'firefox' },
    { name: 'webkit' },
    { name: 'mobile-chrome' },
    { name: 'mobile-safari' },
  ],
});
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `BASE_URL` | Backend URL | `http://localhost:8080` |
| `HEADLESS` | Run headless | `true` |
| `WORKERS` | Parallel workers | Auto |
| `TIMEOUT` | Test timeout (ms) | `30000` |
| `RETRIES` | Test retries | `0` (2 in CI) |
| `CI` | CI mode | `false` |
| `GREP` | Test filter pattern | - |

## 🔄 CI/CD Integration

### GitHub Actions

```yaml
name: E2E Tests

on: [push, pull_request]

jobs:
  e2e:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'

      - name: Install dependencies
        run: |
          cd e2e
          npm install
          npm run test:install

      - name: Start backend
        run: |
          docker compose up -d
          ./scripts/wait-for-backend.sh

      - name: Run E2E tests
        run: cd e2e && npm run test:ci

      - name: Upload report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: playwright-report
          path: e2e/playwright-report/
```

### Jenkins

```groovy
pipeline {
  agent any

  stages {
    stage('Install') {
      steps {
        sh 'cd e2e && npm install'
        sh 'cd e2e && npm run test:install'
      }
    }

    stage('Test') {
      steps {
        sh 'docker compose up -d'
        sh 'cd e2e && npm run test:ci'
      }
    }
  }

  post {
    always {
      publishHTML([
        reportDir: 'e2e/playwright-report',
        reportFiles: 'index.html',
        reportName: 'Playwright Report'
      ])
    }
  }
}
```

## ✍️ Writing Tests

### Using Test Fixtures

```typescript
import { test, expect } from '../fixtures/test-fixtures';

test('should create patient', async ({ adminApiClient, testData }) => {
  const patient = await testData.createPatient({
    firstName: 'John',
    lastName: 'Doe',
  });

  expect(patient.id).toBeTruthy();
});
```

### Test Data Factory

```typescript
// Create patient
const patient = await testData.createPatient();

// Create observation
const observation = await testData.createObservation(patient.id, {
  valueQuantity: { value: 7.5, unit: '%' },
});

// Create care gap
const gap = await testData.createCareGap(patient.id, {
  priority: 'HIGH',
});
```

### API Testing

```typescript
test('should get patient', async ({ adminApiClient }) => {
  const response = await adminApiClient.get('/api/patients/123');

  expect(response.ok()).toBeTruthy();

  const patient = await response.json();
  expect(patient.id).toBe('123');
});
```

### Test Tags

Use tags to organize tests:

```typescript
test('@workflow @patient should create patient', async () => {
  // Test code
});

test('@api @integration should call API', async () => {
  // Test code
});

test('@performance should meet SLA', async () => {
  // Test code
});
```

Run tests by tag:

```bash
npm test -- --grep @workflow
npm test -- --grep @api
npm test -- --grep "@performance"
```

## 💡 Best Practices

### 1. Use Fixtures for Common Setup

```typescript
import { test, expect } from '../fixtures/test-fixtures';

test('test name', async ({ adminApiClient, testData }) => {
  // Fixtures provide authenticated clients and test data helpers
});
```

### 2. Clean Up Test Data

```typescript
test('should create patient', async ({ testData }) => {
  const patient = await testData.createPatient();
  // testData.cleanup() is called automatically after test
});
```

### 3. Use Descriptive Test Names

```typescript
// Good
test('@workflow should create patient and verify in list', async () => {});

// Bad
test('test1', async () => {});
```

### 4. Test One Thing Per Test

```typescript
// Good - focused test
test('should create patient', async () => {
  // Only test patient creation
});

test('should update patient', async () => {
  // Only test patient update
});

// Bad - testing multiple things
test('should create and update patient', async () => {
  // Testing two things
});
```

### 5. Handle Async Operations Properly

```typescript
// Good
await page.waitForSelector('.success-message');
await expect(page.locator('.message')).toContainText('Success');

// Bad - no waiting
page.click('button');
expect(page.locator('.message')).toContainText('Success'); // May fail
```

### 6. Use Proper Assertions

```typescript
// Good - specific assertion
expect(response.status()).toBe(200);

// Bad - vague assertion
expect(response.ok()).toBeTruthy();
```

### 7. Isolate Tests

```typescript
// Each test should be independent
test('test 1', async ({ testData }) => {
  const patient = await testData.createPatient();
  // Use own test data
});

test('test 2', async ({ testData }) => {
  const patient = await testData.createPatient();
  // Use own test data, not relying on test 1
});
```

## 🐛 Troubleshooting

### Tests Failing with "timeout"

1. Increase timeout:
   ```bash
   TIMEOUT=60000 npm test
   ```

2. Check if backend is running:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

3. Run in headed mode to see what's happening:
   ```bash
   npm run test:headed
   ```

### Authentication Issues

1. Delete auth files and regenerate:
   ```bash
   rm -rf .auth/
   npm test
   ```

2. Check credentials in `global.setup.ts`

3. Verify backend authentication endpoint:
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin@healthdata.com","password":"Admin123!","tenantId":"test-tenant-001"}'
   ```

### Browser Installation Issues

```bash
# Reinstall browsers
npx playwright install --with-deps

# Or install specific browser
npx playwright install chromium
```

### Tests Pass Locally but Fail in CI

1. Check environment variables
2. Ensure backend is fully started before tests
3. Increase timeouts in CI
4. Check for race conditions

### Debug Failed Tests

1. Run with debug mode:
   ```bash
   npm run test:debug
   ```

2. Use trace viewer:
   ```bash
   npx playwright show-trace test-results/trace.zip
   ```

3. Check screenshots in `test-results/`

4. View HTML report:
   ```bash
   npm run test:report
   ```

## 📊 Test Metrics

### Current Coverage

- **Total Test Files**: 7
- **Total Test Cases**: 60+
- **Total Lines of Code**: 4500+
- **Browser Coverage**: 5 (Chrome, Firefox, Safari, Edge, Mobile)
- **Device Coverage**: 3 (Desktop, Mobile, Tablet)
- **Accessibility Compliance**: WCAG 2.1 Level AA
- **Performance Thresholds**: Established

### Performance Targets

| Metric | Target | Current |
|--------|--------|---------|
| Health Check | < 100ms | ✅ |
| Patient List | < 500ms | ✅ |
| Patient Details | < 200ms | ✅ |
| Measure Calc | < 1000ms | ✅ |
| P50 Latency | < 50ms | ✅ |
| P95 Latency | < 200ms | ✅ |
| P99 Latency | < 500ms | ✅ |

## 📖 Additional Resources

- [Playwright Documentation](https://playwright.dev)
- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- [Axe Accessibility Testing](https://github.com/abhinaba-ghosh/axe-playwright)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)

## 🤝 Contributing

1. Write tests for new features
2. Follow the test structure and naming conventions
3. Add appropriate tags (@workflow, @api, @security, etc.)
4. Ensure all tests pass before committing
5. Update this README if adding new test categories

## 📝 License

Copyright © 2024 HealthData in Motion. All rights reserved.

---

**Happy Testing! 🎉**

For questions or issues, please contact the development team or open an issue in the repository.
