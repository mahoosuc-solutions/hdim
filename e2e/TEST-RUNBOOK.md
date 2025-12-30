# HDIM E2E Test Runbook

## Overview

This document provides comprehensive documentation for running, maintaining, and troubleshooting the HDIM E2E test suite.

**Test Framework**: Playwright
**Node Version**: 20.x
**Total Test Suites**: 15+
**Estimated Test Count**: 300+

---

## Quick Start

### Prerequisites

```bash
# Install Node.js 20.x
nvm install 20
nvm use 20

# Install dependencies
cd e2e
npm ci

# Install Playwright browsers
npx playwright install --with-deps
```

### Running Tests

```bash
# Run all tests
npm test

# Run specific test file
npx playwright test tests/workflows/auth/login.spec.ts

# Run tests with specific tag
npx playwright test --grep @smoke

# Run in headed mode (visible browser)
npx playwright test --headed

# Run with debug
npx playwright test --debug

# Run specific project (browser)
npx playwright test --project=chromium
npx playwright test --project=firefox
npx playwright test --project=webkit
```

---

## Test Suite Structure

```
e2e/
├── fixtures/               # Test data and fixtures
│   └── test-fixtures.ts    # Users, tenants, test data
├── pages/                  # Page Object Models
│   ├── login.page.ts
│   ├── dashboard.page.ts
│   ├── patient.page.ts
│   ├── care-gap.page.ts
│   └── evaluation.page.ts
├── tests/
│   ├── smoke/              # Smoke tests (quick validation)
│   ├── workflows/          # Feature workflow tests
│   │   ├── auth/           # AUTH-001 to AUTH-005
│   │   ├── dashboard/      # DASH-001 to DASH-003
│   │   ├── patient/        # PAT-001 to PAT-005
│   │   ├── care-gap/       # CG-001 to CG-006
│   │   ├── evaluation/     # EVAL-001 to EVAL-004
│   │   ├── reporting/      # RPT-001 to RPT-005
│   │   ├── risk/           # RISK-001 to RISK-003
│   │   ├── admin/          # ADM-001 to ADM-005
│   │   └── integrations/   # INT-001 to INT-003
│   ├── security/           # Security tests
│   │   ├── rbac.spec.ts    # Role-based access control
│   │   ├── session.spec.ts # Session management
│   │   └── tenant-isolation.spec.ts
│   ├── accessibility/      # WCAG 2.1 AA tests
│   │   └── wcag-compliance.spec.ts
│   ├── visual/             # Visual regression tests
│   │   └── visual-regression.spec.ts
│   ├── performance/        # Performance tests
│   │   └── lighthouse.spec.ts
│   └── mobile/             # Mobile responsive tests
│       └── mobile-responsive.spec.ts
├── playwright.config.ts    # Playwright configuration
└── lighthouse-ci.json      # Lighthouse CI config
```

---

## Test Categories

### 1. Smoke Tests (@smoke)

**Purpose**: Quick validation that critical paths work
**Run Time**: ~5 minutes
**When to Run**: Every commit, PR

```bash
npx playwright test --grep @smoke
```

**Includes**:
- Login/logout
- Dashboard loads
- Patient list displays
- Care gaps accessible

### 2. Workflow Tests

**Purpose**: Full feature coverage
**Run Time**: ~30-45 minutes
**When to Run**: Nightly, pre-release

| Suite | Test IDs | Description |
|-------|----------|-------------|
| Auth | AUTH-001 to AUTH-005 | Login, logout, session, SSO |
| Dashboard | DASH-001 to DASH-003 | KPIs, navigation, data display |
| Patient | PAT-001 to PAT-005 | List, details, search, filters |
| Care Gap | CG-001 to CG-006 | List, details, interventions |
| Evaluation | EVAL-001 to EVAL-004 | Queue, execution, results |
| Reporting | RPT-001 to RPT-005 | Reports, exports, QRDA |
| Risk | RISK-001 to RISK-003 | Stratification, HCC |
| Admin | ADM-001 to ADM-005 | Users, roles, config |
| Integrations | INT-001 to INT-003 | EHR, FHIR, prior auth |

### 3. Security Tests

**Purpose**: Verify security controls
**Run Time**: ~15 minutes
**When to Run**: Weekly, security PRs

```bash
npx playwright test tests/security
```

**Covers**:
- RBAC enforcement
- Session management
- Multi-tenant isolation
- Cross-tenant access prevention

### 4. Accessibility Tests

**Purpose**: WCAG 2.1 AA compliance
**Run Time**: ~10 minutes
**When to Run**: Weekly, UI PRs

```bash
npx playwright test tests/accessibility
```

**Checks**:
- Color contrast (AX-001)
- Keyboard navigation (AX-002)
- Screen reader support (AX-003)
- Focus management (AX-004)
- Form accessibility (AX-005)
- ARIA compliance (AX-006)
- Heading hierarchy (AX-007)
- Link/button accessibility (AX-008)

### 5. Visual Regression Tests

**Purpose**: Detect unintended UI changes
**Run Time**: ~10 minutes
**When to Run**: UI PRs, weekly

```bash
# Generate new baselines
npx playwright test tests/visual --update-snapshots

# Compare against baselines
npx playwright test tests/visual
```

**Pages Covered**:
- Login page
- Dashboard
- Patient list
- Care gap list
- Reports
- Forms and modals

### 6. Performance Tests

**Purpose**: Performance budget enforcement
**Run Time**: ~15 minutes
**When to Run**: Weekly, performance PRs

```bash
npx playwright test tests/performance
```

**Budgets**:
| Metric | Budget |
|--------|--------|
| LCP | < 2.5s |
| FCP | < 1.8s |
| CLS | < 0.1 |
| TTI | < 3.8s |
| Page Load | < 3s (login), < 5s (dashboard) |
| API Response | < 2s |

### 7. Mobile Tests

**Purpose**: Mobile/tablet responsiveness
**Run Time**: ~10 minutes
**When to Run**: Weekly, responsive PRs

```bash
npx playwright test tests/mobile
```

**Devices**:
- iPhone SE, 12, 14
- Pixel 5
- Galaxy S21
- iPad, iPad Pro

---

## Test Configuration

### Environment Variables

```bash
# Required
BASE_URL=http://localhost:4200
API_URL=http://localhost:8000

# Optional
TEST_USER_PASSWORD=password123
HEADLESS=true
SLOW_MO=0
CI=true
```

### Playwright Configuration

Key settings in `playwright.config.ts`:

```typescript
{
  timeout: 30000,           // Test timeout
  retries: 2,               // Retries on failure
  workers: 4,               // Parallel workers
  reporter: [
    ['html'],
    ['junit', { outputFile: 'results.xml' }]
  ],
  projects: [
    { name: 'chromium' },
    { name: 'firefox' },
    { name: 'webkit' },
    { name: 'Mobile Chrome', use: devices['Pixel 5'] },
    { name: 'Mobile Safari', use: devices['iPhone 12'] }
  ]
}
```

---

## CI/CD Integration

### GitHub Actions Workflow

The E2E tests run automatically via `.github/workflows/e2e-tests.yml`:

**Triggers**:
- Push to main/develop (with relevant path changes)
- Pull requests
- Nightly schedule (2 AM UTC)
- Manual dispatch

**Test Jobs**:
1. Smoke tests (fast feedback)
2. Workflow tests (sharded 4-way)
3. Security tests
4. Accessibility tests
5. Visual regression tests
6. Performance tests + Lighthouse CI
7. Mobile tests
8. Cross-browser tests (nightly)

### Running Locally Like CI

```bash
# Start test environment
docker compose -f docker-compose.test.yml up -d

# Wait for app
npx wait-on http://localhost:4200 --timeout 120000

# Run tests
npm test

# Stop environment
docker compose -f docker-compose.test.yml down
```

---

## Troubleshooting

### Common Issues

#### 1. Browser Installation Failed

```bash
# Reinstall browsers
npx playwright install --with-deps

# If on Linux, may need system deps
sudo npx playwright install-deps
```

#### 2. Tests Timeout

**Causes**:
- Slow test environment
- Application not fully loaded
- Network issues

**Solutions**:
```typescript
// Increase timeout in test
test.setTimeout(60000);

// Add explicit waits
await page.waitForLoadState('networkidle');
await expect(element).toBeVisible({ timeout: 10000 });
```

#### 3. Element Not Found

**Causes**:
- Wrong selector
- Element not rendered
- Dynamic content

**Solutions**:
```typescript
// Use more specific selectors
page.locator('[data-testid="patient-list"]')

// Wait for element
await page.waitForSelector('[data-testid="patient-row"]');

// Use flexible locators
page.locator('button:has-text("Submit"), [data-testid="submit-btn"]')
```

#### 4. Visual Regression Failures

**Causes**:
- Legitimate UI change
- Dynamic content (timestamps, IDs)
- Different rendering environments

**Solutions**:
```typescript
// Mask dynamic content
await expect(page).toHaveScreenshot('page.png', {
  mask: [page.locator('[data-testid="timestamp"]')],
});

// Update baselines
npx playwright test tests/visual --update-snapshots
```

#### 5. Flaky Tests

**Causes**:
- Race conditions
- Timing issues
- Test isolation problems

**Solutions**:
```typescript
// Add explicit waits
await page.waitForLoadState('networkidle');

// Use web-first assertions
await expect(page.locator('.data')).toHaveText('expected');

// Ensure test isolation
test.beforeEach(async ({ page }) => {
  await page.goto('/');
  // Reset state
});
```

### Debug Mode

```bash
# Run with Playwright Inspector
npx playwright test --debug

# Generate trace on failure
npx playwright test --trace on

# View trace
npx playwright show-trace trace.zip
```

---

## Maintenance

### Weekly Tasks

1. Review test results from nightly runs
2. Update flaky tests
3. Review and address accessibility violations
4. Check performance trends

### Monthly Tasks

1. Update Playwright version
2. Review and update test data
3. Add tests for new features
4. Prune obsolete tests
5. Update visual baselines if needed

### Before Release

1. Run full test suite
2. Review all test categories pass
3. Check accessibility compliance
4. Verify performance budgets met
5. Confirm mobile/responsive tests pass

---

## Test Data

### Test Users

| Username | Role | Access |
|----------|------|--------|
| test_superadmin | SUPER_ADMIN | Full system |
| test_admin | ADMIN | Tenant admin |
| test_evaluator | EVALUATOR | Run evaluations |
| test_analyst | ANALYST | View reports |
| test_viewer | VIEWER | Read-only |

**Password**: `password123` (dev only)

### Test Tenants

| ID | Name | Use Case |
|----|------|----------|
| TENANT001 | Acme Healthcare | Primary testing |
| TENANT002 | Beta Medical | Multi-tenant tests |

---

## Reporting

### Test Reports

After running tests, view reports:

```bash
# HTML report
npx playwright show-report

# Located at
e2e/playwright-report/index.html
```

### JUnit Results

```bash
# Generated at
e2e/results.xml
```

### Lighthouse Reports

```bash
# Located at
.lighthouseci/*.json
.lighthouseci/*.html
```

---

## Best Practices

### Writing Tests

1. **Use Page Objects**: Encapsulate page interactions
2. **Use data-testid**: Prefer stable selectors
3. **Isolate Tests**: Each test should be independent
4. **Clean Up**: Reset state after destructive tests
5. **Descriptive Names**: Test names should describe behavior
6. **Tag Tests**: Use @smoke, @critical for filtering

### Test Structure

```typescript
test.describe('Feature Name', () => {
  test.beforeEach(async ({ page }) => {
    // Setup
  });

  test('should do expected behavior', async ({ page }) => {
    // Arrange
    const loginPage = new LoginPage(page);

    // Act
    await loginPage.login(user, pass);

    // Assert
    await expect(page).toHaveURL(/dashboard/);
  });
});
```

### Selector Priority

1. `data-testid` attributes (most stable)
2. ARIA roles (`[role="button"]`)
3. Text content (`text=Submit`)
4. CSS selectors (least stable)

---

## Support

- **Documentation**: `/e2e/README.md`
- **Playwright Docs**: https://playwright.dev/docs
- **HDIM Architecture**: `/docs/DISTRIBUTION_ARCHITECTURE.md`
- **Test Plan**: `/gtm/hdim-ui-testing-implementation-plan.md`

---

*Last Updated*: December 2025
*Version*: 1.0
