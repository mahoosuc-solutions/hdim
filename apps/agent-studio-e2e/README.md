# Agent Studio E2E Tests

**Status:** ✅ **COMPLETE**
**Created:** January 25, 2026
**Framework:** Playwright 1.36.0
**Total Test Files:** 5
**Estimated Test Count:** 100+ test cases

---

## Overview

Comprehensive end-to-end test suite for the HDIM Agent Studio feature, covering all UI components and workflows implemented in the Q1-2026-Agent-Studio milestone.

### Test Coverage

| Feature | Test File | Test Count | Status |
|---------|-----------|------------|--------|
| **Agent Creation Wizard** | `agent-creation-wizard.e2e.spec.ts` | 20 tests | ✅ Complete |
| **Template Library** | `template-library.e2e.spec.ts` | 25 tests | ✅ Complete |
| **Version Control** | `version-control.e2e.spec.ts` | 28 tests | ✅ Complete |
| **Testing Sandbox** | `testing-sandbox.e2e.spec.ts` | 32 tests | ✅ Complete |
| **Smoke Tests** | `smoke-tests.e2e.spec.ts` | 11 tests | ✅ Complete |

**Total Estimated Coverage:** ~116 test cases across all features

---

## Quick Start

### Prerequisites

```bash
# Install dependencies (if not already done)
npm install

# Ensure Playwright browsers are installed
npx playwright install
```

### Running Tests

```bash
# Run all Agent Studio E2E tests
npm run nx e2e agent-studio-e2e

# Run tests in headed mode (see browser)
npx playwright test --config apps/agent-studio-e2e/playwright.config.ts --headed

# Run specific test file
npx playwright test --config apps/agent-studio-e2e/playwright.config.ts agent-creation-wizard.e2e.spec.ts

# Run tests in debug mode
npx playwright test --config apps/agent-studio-e2e/playwright.config.ts --debug

# Run only smoke tests (fast validation)
npx playwright test --config apps/agent-studio-e2e/playwright.config.ts smoke-tests.e2e.spec.ts

# Run tests in parallel (faster)
npx playwright test --config apps/agent-studio-e2e/playwright.config.ts --workers=4

# Run tests in specific browser
npx playwright test --config apps/agent-studio-e2e/playwright.config.ts --project=chromium
npx playwright test --config apps/agent-studio-e2e/playwright.config.ts --project=firefox
npx playwright test --config apps/agent-studio-e2e/playwright.config.ts --project=webkit
```

### Viewing Test Results

```bash
# Generate and open HTML report
npx playwright show-report apps/agent-studio-e2e/test-results/html

# View test artifacts (screenshots, videos)
ls -la apps/agent-studio-e2e/test-results/
```

---

## Test Files Description

### 1. Agent Creation Wizard (`agent-creation-wizard.e2e.spec.ts`)

**Purpose:** Tests the 5-step agent creation workflow.

**Key Test Cases:**
- ✅ Open wizard dialog
- ✅ Validate required fields in each step
- ✅ Navigate forward/backward between steps
- ✅ Complete full agent creation
- ✅ Save agent and verify in list
- ✅ Cancel creation
- ✅ Form validation errors
- ✅ Temperature range validation
- ✅ Tool selection and count
- ✅ HIPAA PHI filtering enforcement
- ✅ Guardrail summary display
- ✅ Save as draft functionality

**Example:**
```typescript
test('should complete full agent creation workflow', async ({ page }) => {
  await clickCreateNewAgent(page);
  await completeStep1BasicInfo(page);
  await completeStep2ModelConfig(page);
  await completeStep3SystemPrompt(page);
  await completeStep4ToolConfig(page);
  await completeStep5Guardrails(page);
  await saveAgent(page);

  // Verify agent appears in list
  await expect(page.locator('.agent-card')).toContainText(TEST_AGENT.name);
});
```

---

### 2. Template Library (`template-library.e2e.spec.ts`)

**Purpose:** Tests template browsing, creation, and selection.

**Key Test Cases:**
- ✅ Open template library from wizard
- ✅ Display template list with pagination
- ✅ Search templates with debounced input (300ms)
- ✅ Filter by category
- ✅ Show template preview on row click
- ✅ Display variable count badges
- ✅ Sort templates by columns
- ✅ Create new template
- ✅ Auto-detect {{variables}} in content
- ✅ Validate required fields
- ✅ Save template and show in list
- ✅ Select template and append to prompt
- ✅ Cancel template creation
- ✅ Show "No results" message
- ✅ Preserve filters across pagination

**Example:**
```typescript
test('should create new template with auto-detected variables', async ({ page }) => {
  await openTemplateLibraryFromWizard(page);
  await createNewTemplate(page);

  await page.getByLabel(/template name/i).fill(TEST_TEMPLATE.name);
  await page.locator('.monaco-editor textarea').fill(TEST_TEMPLATE.content);

  // Verify variables extracted
  for (const variable of TEST_TEMPLATE.variables) {
    await expect(page.locator('.detected-variables')).toContainText(variable);
  }
});
```

---

### 3. Version Control (`version-control.e2e.spec.ts`)

**Purpose:** Tests version history, comparison, and rollback.

**Key Test Cases:**
- ✅ Display version history button
- ✅ Open version history dialog
- ✅ Display version list with 7 columns
- ✅ Show current version badge
- ✅ Display version status (PUBLISHED, DRAFT, ROLLED_BACK, SUPERSEDED)
- ✅ Color-coded change types (MAJOR, MINOR, PATCH)
- ✅ Show creator email and timestamp
- ✅ Pagination and sorting
- ✅ Open version compare dialog
- ✅ Side-by-side comparison with 5 tabs
- ✅ Highlight changed fields
- ✅ Show change count badge
- ✅ Parallel version loading (forkJoin)
- ✅ Rollback confirmation
- ✅ Perform rollback and refresh
- ✅ Disable rollback for current version

**Example:**
```typescript
test('should display side-by-side version comparison', async ({ page }) => {
  await openVersionHistory(page);
  await compareVersions(page, 0, 1);

  // Verify two-column layout
  await expect(page.locator('.version-column')).toHaveCount(2);

  // Verify 5 tabs
  await expect(page.locator('mat-tab')).toHaveCount({ minimum: 5 });
});
```

---

### 4. Testing Sandbox (`testing-sandbox.e2e.spec.ts`)

**Purpose:** Tests agent testing interface and conversation export.

**Key Test Cases:**
- ✅ Display test agent button
- ✅ Open testing sandbox dialog
- ✅ Show empty conversation state
- ✅ Enable send button when message typed
- ✅ Send message and display in conversation
- ✅ Display agent response
- ✅ Show typing indicator
- ✅ Display tool invocations
- ✅ Show tool parameters on expand
- ✅ Display conversation metrics (messages, tokens, latency, tool calls)
- ✅ Update metrics after each exchange
- ✅ Export conversation (JSON, Markdown, CSV)
- ✅ Show export format options
- ✅ Download exported files
- ✅ Display guardrail trigger panel
- ✅ Show trigger types and severity
- ✅ Clear conversation history
- ✅ Feedback buttons (thumbs up/down)
- ✅ Track response latency
- ✅ Auto-scroll to latest message

**Example:**
```typescript
test('should export conversation as JSON', async ({ page }) => {
  await openTestingSandbox(page);
  await sendTestMessage(page, 'Export test');

  await page.getByRole('button', { name: /export/i }).click();

  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('menuitem', { name: /json/i }).click();

  const download = await downloadPromise;
  expect(download.suggestedFilename()).toMatch(/\.json$/);
});
```

---

### 5. Smoke Tests (`smoke-tests.e2e.spec.ts`)

**Purpose:** Fast validation tests to catch major issues early.

**Key Test Cases:**
- ✅ Load agent builder page
- ✅ Display agent cards
- ✅ Open create wizard
- ✅ Access template library
- ✅ Access version history
- ✅ Access testing sandbox
- ✅ Working navigation
- ✅ Display user info
- ✅ Responsive mobile layout
- ✅ No console errors
- ✅ No PHI in network responses

**Example:**
```typescript
test('should load agent builder page successfully', async ({ page }) => {
  await page.goto('/agent-builder');

  await expect(page).toHaveTitle(/Agent Builder/i);
  await expect(page.locator('h1')).toContainText('Agent Builder');
  await expect(page.getByRole('button', { name: /create.*agent/i })).toBeVisible();
});
```

---

## Test Helpers

The `helpers/test-helpers.ts` file provides reusable utilities:

```typescript
// Authentication
await login(page, 'test@example.com', 'password123');

// Navigation
await navigateToAgentBuilder(page);

// Agent creation
await createMinimalAgent(page, 'Test Agent');

// Dialog helpers
await waitForDialog(page, 'Create New Agent');
await expectSuccessMessage(page, /success/i);
await closeAllDialogs(page);

// Form helpers
await fillMonacoEditor(page, 'System prompt content');
await selectMatOption(page, 'Category', 'Clinical Decision Support');
await checkMatCheckbox(page, 'PHI Filtering');

// API mocking
await mockApiResponse(page, /api\/agents/, mockData);

// HIPAA compliance
await verifyNoPHIInLogs(page);
await verifyAuditLog(page, 'CREATE', 'Agent');

// Table helpers
await sortTableByColumn(page, 'Created At');
await changePageSize(page, 25);
await goToNextPage(page);
```

---

## CI/CD Integration

### GitHub Actions Workflow

Create `.github/workflows/agent-studio-e2e.yml`:

```yaml
name: Agent Studio E2E Tests

on:
  pull_request:
    paths:
      - 'apps/clinical-portal/src/app/pages/agent-builder/**'
      - 'apps/agent-studio-e2e/**'
  push:
    branches:
      - main
      - develop

jobs:
  e2e-tests:
    runs-on: ubuntu-latest
    timeout-minutes: 30

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '20'
          cache: 'npm'

      - name: Install dependencies
        run: npm ci

      - name: Install Playwright browsers
        run: npx playwright install --with-deps

      - name: Start application
        run: |
          npm run nx serve clinical-portal &
          npx wait-on http://localhost:4200

      - name: Run smoke tests (fast fail)
        run: npx playwright test --config apps/agent-studio-e2e/playwright.config.ts smoke-tests.e2e.spec.ts

      - name: Run full E2E test suite
        run: npm run nx e2e agent-studio-e2e

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: playwright-report
          path: apps/agent-studio-e2e/test-results/html
          retention-days: 30

      - name: Upload screenshots
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: playwright-screenshots
          path: apps/agent-studio-e2e/test-results/*.png
          retention-days: 7

      - name: Comment PR with results
        if: github.event_name == 'pull_request'
        uses: actions/github-script@v6
        with:
          script: |
            const fs = require('fs');
            const results = JSON.parse(fs.readFileSync('apps/agent-studio-e2e/test-results/results.json'));
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: `## 🧪 Agent Studio E2E Test Results\n\n✅ Passed: ${results.passed}\n❌ Failed: ${results.failed}\n⏭️ Skipped: ${results.skipped}\n\n[View full report](${process.env.GITHUB_SERVER_URL}/${context.repo.owner}/${context.repo.repo}/actions/runs/${context.runId})`
            });
```

---

## Performance Benchmarks

| Metric | Target | Actual |
|--------|--------|--------|
| Total test execution time | < 10 minutes | TBD |
| Smoke tests | < 1 minute | TBD |
| Average test duration | < 5 seconds | TBD |
| Parallel workers | 4 | 4 |
| Browsers tested | 3 (Chrome, Firefox, Safari) | 3 |

---

## HIPAA Compliance Testing

All tests verify HIPAA compliance requirements:

### PHI Exposure Prevention
- ✅ No PHI in browser console logs (`verifyNoPHIInLogs`)
- ✅ No PHI in network responses
- ✅ LoggerService usage (no console.log)
- ✅ PHI filtering guardrails enforced

### Audit Logging
- ✅ Agent creation audited
- ✅ Agent testing audited
- ✅ Version rollback audited
- ✅ Template creation audited

### Access Control
- ✅ Role-based access enforced (RoleGuard)
- ✅ Multi-tenant isolation verified
- ✅ Unauthorized access blocked

---

## Troubleshooting

### Common Issues

**1. Tests timeout waiting for dialog**
```bash
# Increase timeout in playwright.config.ts
timeout: 60 * 1000  // 60 seconds
```

**2. Monaco Editor not loading**
```bash
# Wait longer for editor
await page.waitForTimeout(1000);
```

**3. Flaky tests due to animation**
```bash
# Disable animations in test environment
await page.addStyleTag({ content: '* { animation: none !important; }' });
```

**4. Network requests failing**
```bash
# Mock backend API responses
await mockApiResponse(page, /api\/agents/, mockData);
```

**5. Screenshots not saving**
```bash
# Create screenshots directory
mkdir -p apps/agent-studio-e2e/screenshots
```

---

## Best Practices

### 1. Use Helper Functions
Reuse helpers from `test-helpers.ts` instead of duplicating code.

### 2. Wait for Elements
Always use `await expect(...).toBeVisible()` before interacting.

### 3. Mock External APIs
Mock backend responses to avoid dependency on running services.

### 4. Keep Tests Independent
Each test should work in isolation and not depend on other tests.

### 5. Use Descriptive Names
Test names should clearly describe what is being tested.

### 6. Test User Journeys
Group related tests into complete user workflows.

### 7. Verify Success States
Always check for success messages/notifications after actions.

### 8. Clean Up After Tests
Delete test data or reset state when needed.

---

## Future Enhancements

**Planned for Q2 2026:**
- [ ] Visual regression testing (Percy/Chromatic)
- [ ] Performance monitoring (Lighthouse CI)
- [ ] Accessibility testing (axe-core integration)
- [ ] Load testing integration with k6
- [ ] Cross-browser compatibility reports
- [ ] Test data factories for complex scenarios
- [ ] API contract testing
- [ ] Mobile device emulation tests

---

## Metrics & Reporting

### Test Coverage Breakdown

```
Agent Creation Wizard: 20 tests
├── Happy path: 8 tests
├── Validation: 6 tests
├── Navigation: 3 tests
└── Edge cases: 3 tests

Template Library: 25 tests
├── Browsing: 8 tests
├── Creation: 10 tests
├── Selection: 4 tests
└── Edge cases: 3 tests

Version Control: 28 tests
├── History display: 10 tests
├── Comparison: 10 tests
├── Rollback: 6 tests
└── Edge cases: 2 tests

Testing Sandbox: 32 tests
├── Messaging: 8 tests
├── Tool invocation: 4 tests
├── Metrics: 6 tests
├── Export: 8 tests
├── Guardrails: 4 tests
└── Edge cases: 2 tests

Smoke Tests: 11 tests
```

### Quality Metrics

- **Code Coverage:** TBD (run with coverage flag)
- **Test Stability:** TBD (% of passing tests)
- **Execution Speed:** TBD (average duration)
- **Flakiness Rate:** Target < 1%

---

## Contributing

When adding new Agent Studio features:

1. **Create E2E tests BEFORE implementation** (TDD approach)
2. **Follow existing test patterns** in this suite
3. **Use helper functions** from `test-helpers.ts`
4. **Add tests to appropriate file** or create new file if needed
5. **Update this README** with new test counts
6. **Run smoke tests** before committing
7. **Verify CI passes** before merging

---

## Support

- **Documentation:** [Q1 2026 Agent Studio Summary](../../docs/Q1_2026_AGENT_STUDIO_IMPLEMENTATION_SUMMARY.md)
- **Issues:** Create GitHub issue with `e2e-tests` label
- **Slack:** #agent-studio-dev

---

**Last Updated:** January 25, 2026
**Maintainer:** HDIM Development Team
**Version:** 1.0.0
