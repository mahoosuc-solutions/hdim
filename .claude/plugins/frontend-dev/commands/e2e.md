---
name: /frontend-dev:e2e
description: Run end-to-end tests with Playwright across multiple browsers
args:
  mode:
    description: Test mode (run, ui, debug, headed)
    required: false
    default: run
  browser:
    description: Browser to test (chromium, firefox, webkit, all)
    required: false
    default: all
---

# Run E2E Tests

Execute end-to-end tests using Playwright to test complete user workflows.

## Usage

```bash
# Run all E2E tests (headless)
/frontend-dev:e2e

# Run with Playwright UI
/frontend-dev:e2e ui

# Run in headed mode (see browser)
/frontend-dev:e2e headed

# Debug mode (step through tests)
/frontend-dev:e2e debug

# Run on specific browser
/frontend-dev:e2e run chromium
```

## Test Modes

### 1. Headless Mode (Default)

```bash
cd frontend
npm run e2e
```

**Features:**
- ✅ Runs all browsers in parallel
- ✅ Fast execution
- ✅ CI/CD friendly
- ✅ Screenshots on failure

**Output:**
```
Running 12 tests using 3 workers

  ✓ [chromium] › dashboard.spec.ts:5:3 › should display dashboard (2.1s)
  ✓ [chromium] › leads.spec.ts:8:3 › should create new lead (3.4s)
  ✓ [firefox] › dashboard.spec.ts:5:3 › should display dashboard (2.3s)
  ✓ [webkit] › dashboard.spec.ts:5:3 › should display dashboard (2.5s)

  12 passed (15.2s)

View report: npx playwright show-report
```

### 2. UI Mode (Interactive)

```bash
npm run e2e:ui
```

**Features:**
- ✅ Visual test runner
- ✅ Watch mode (re-runs on changes)
- ✅ Time travel debugging
- ✅ Network inspection
- ✅ Pick locators

**Access:** Opens browser at `http://localhost:playwright-ui`

### 3. Headed Mode (Visible)

```bash
npm run e2e:headed
```

**Features:**
- ✅ See browser during test execution
- ✅ Useful for debugging
- ✅ Slower than headless

### 4. Debug Mode

```bash
npm run e2e:debug
```

**Features:**
- ✅ Step through tests line-by-line
- ✅ Playwright Inspector
- ✅ Console access
- ✅ Pause and resume

## E2E Test Structure

### Page Object Model

**File: `e2e/pages/dashboard-page.ts`**
```typescript
import { Page, Locator } from '@playwright/test';

export class DashboardPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly statsCard: Locator;
  readonly patientTable: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: /dashboard/i });
    this.statsCard = page.getByTestId('stats-card');
    this.patientTable = page.getByRole('table', { name: /patients/i });
  }

  async goto() {
    await this.page.goto('/dashboard');
  }

  async getPatientCount(): Promise<number> {
    return await this.patientTable.locator('tbody tr').count();
  }
}
```

### Test File

**File: `e2e/tests/dashboard.spec.ts`**
```typescript
import { test, expect } from '@playwright/test';
import { DashboardPage } from '../pages/dashboard-page';

test.describe('Dashboard', () => {
  let dashboardPage: DashboardPage;

  test.beforeEach(async ({ page }) => {
    dashboardPage = new DashboardPage(page);
    await dashboardPage.goto();
  });

  test('should display dashboard heading', async () => {
    await expect(dashboardPage.heading).toBeVisible();
  });

  test('should load patient data', async () => {
    await expect(dashboardPage.patientTable).toBeVisible();
    const count = await dashboardPage.getPatientCount();
    expect(count).toBeGreaterThan(0);
  });

  test('should be accessible', async ({ page }) => {
    // Run accessibility audit
    const accessibilityScanResults = await new AxeBuilder({ page }).analyze();
    expect(accessibilityScanResults.violations).toEqual([]);
  });
});
```

## Critical User Flows

### 1. Authentication Flow

```typescript
test('user can log in', async ({ page }) => {
  await page.goto('/login');

  // Fill login form
  await page.fill('[name="email"]', 'test@example.com');
  await page.fill('[name="password"]', 'password123');
  await page.click('button[type="submit"]');

  // Verify redirect to dashboard
  await expect(page).toHaveURL('/dashboard');
  await expect(page.getByText('Welcome back')).toBeVisible();
});
```

### 2. Data Creation Flow

```typescript
test('user can create patient record', async ({ page }) => {
  await page.goto('/patients');
  await page.click('button:has-text("New Patient")');

  // Fill form
  await page.fill('[name="firstName"]', 'John');
  await page.fill('[name="lastName"]', 'Doe');
  await page.fill('[name="dateOfBirth"]', '1980-01-15');

  // Submit
  await page.click('button[type="submit"]');

  // Verify success
  await expect(page.getByText('Patient created successfully')).toBeVisible();
  await expect(page.getByText('John Doe')).toBeVisible();
});
```

### 3. Data Export Flow

```typescript
test('user can export data to CSV', async ({ page }) => {
  await page.goto('/patients');

  // Setup download listener
  const downloadPromise = page.waitForEvent('download');

  // Trigger export
  await page.click('button:has-text("Export")');
  await page.click('button:has-text("CSV")');

  // Verify download
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toContain('patients');
  expect(download.suggestedFilename()).toContain('.csv');
});
```

### 4. Error Handling

```typescript
test('displays error when API fails', async ({ page }) => {
  // Mock API failure
  await page.route('**/api/patients', route =>
    route.fulfill({ status: 500, body: 'Server error' })
  );

  await page.goto('/patients');

  // Verify error message
  await expect(page.getByText('Failed to load patients')).toBeVisible();
  await expect(page.getByRole('button', { name: /retry/i })).toBeVisible();
});
```

## Test Fixtures

**File: `e2e/fixtures/auth.ts`**
```typescript
import { test as base } from '@playwright/test';

type Fixtures = {
  authenticatedPage: Page;
};

export const test = base.extend<Fixtures>({
  authenticatedPage: async ({ page }, use) => {
    // Setup: Login
    await page.goto('/login');
    await page.fill('[name="email"]', 'test@example.com');
    await page.fill('[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('/dashboard');

    // Use authenticated page
    await use(page);

    // Teardown: Logout
    await page.click('[aria-label="User menu"]');
    await page.click('text=Logout');
  },
});
```

**Usage:**
```typescript
import { test } from '../fixtures/auth';

test('should access protected route', async ({ authenticatedPage }) => {
  await authenticatedPage.goto('/patients');
  // Already logged in!
});
```

## Browser Testing

### Test Across Browsers

```bash
# Chromium (Chrome/Edge)
npm run e2e:chromium

# Firefox
npm run e2e:firefox

# WebKit (Safari)
npm run e2e:webkit
```

### Responsive Testing

```typescript
test.describe('Mobile view', () => {
  test.use({ viewport: { width: 375, height: 667 } }); // iPhone SE

  test('should display mobile menu', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('button', { name: /menu/i })).toBeVisible();
  });
});

test.describe('Tablet view', () => {
  test.use({ viewport: { width: 768, height: 1024 } }); // iPad

  test('should display sidebar', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('navigation')).toBeVisible();
  });
});
```

## Visual Regression Testing

```typescript
test('dashboard matches snapshot', async ({ page }) => {
  await page.goto('/dashboard');

  // Wait for content to load
  await page.waitForSelector('[data-testid="stats-loaded"]');

  // Take screenshot and compare
  await expect(page).toHaveScreenshot('dashboard.png');
});
```

## Performance Testing

```typescript
test('page loads within performance budget', async ({ page }) => {
  await page.goto('/dashboard');

  // Measure performance
  const performanceMetrics = await page.evaluate(() => {
    const [navigation] = performance.getEntriesByType('navigation');
    return {
      domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
      loadComplete: navigation.loadEventEnd - navigation.loadEventStart
    };
  });

  expect(performanceMetrics.domContentLoaded).toBeLessThan(1000); // < 1s
  expect(performanceMetrics.loadComplete).toBeLessThan(2000); // < 2s
});
```

## Accessibility Testing

```bash
# Install axe-core
npm install --save-dev @axe-core/playwright
```

```typescript
import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

test('should not have accessibility violations', async ({ page }) => {
  await page.goto('/');

  const accessibilityScanResults = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa'])
    .analyze();

  expect(accessibilityScanResults.violations).toEqual([]);
});
```

## Test Reports

### HTML Report

```bash
# Generate and open report
npm run e2e:report
```

**Includes:**
- Test results per browser
- Screenshots on failure
- Execution timeline
- Network logs
- Console logs

### CI/CD Report

```yaml
# GitHub Actions
- name: Run E2E tests
  run: cd frontend && npm run e2e

- name: Upload test results
  if: always()
  uses: actions/upload-artifact@v3
  with:
    name: playwright-report
    path: frontend/e2e/playwright-report/
```

## Common Patterns

### Wait for Network

```typescript
// Wait for specific API call
await page.waitForResponse(response =>
  response.url().includes('/api/patients') && response.status() === 200
);
```

### Handle Dialogs

```typescript
// Accept confirmation dialog
page.on('dialog', dialog => dialog.accept());
await page.click('button:has-text("Delete")');
```

### Multiple Tabs

```typescript
const [newPage] = await Promise.all([
  context.waitForEvent('page'),
  page.click('a[target="_blank"]')
]);

await newPage.waitForLoadState();
expect(newPage.url()).toContain('report');
```

## Debugging Tips

1. **Use Playwright Inspector:**
   ```bash
   PWDEBUG=1 npm run e2e
   ```

2. **Add debug points:**
   ```typescript
   await page.pause(); // Pauses execution
   ```

3. **Console logging:**
   ```typescript
   page.on('console', msg => console.log('PAGE LOG:', msg.text()));
   ```

4. **Slow motion:**
   ```typescript
   test.use({ launchOptions: { slowMo: 1000 } }); // 1s delay
   ```

## Output Examples

### Success ✅

```
Running 24 tests using 6 workers

  ✓ [chromium] › dashboard.spec.ts:5:3 › should display dashboard (1.2s)
  ✓ [chromium] › leads.spec.ts:8:3 › should create lead (2.1s)
  ✓ [firefox] › dashboard.spec.ts:5:3 › should display dashboard (1.5s)
  ...

  24 passed (12.4s)

View HTML report: npm run e2e:report
```

### Failure ❌

```
  ✗ [chromium] › login.spec.ts:10:3 › should log in user (2.3s)

    Error: expect(locator).toHaveURL(expected)

    Expected: "http://localhost:5173/dashboard"
    Received: "http://localhost:5173/login?error=invalid_credentials"

    Screenshot: test-results/login-should-log-in-chromium/test-failed-1.png

  1 failed
  23 passed (13.1s)
```

---

**When to Run:**
- Before deployment to staging/production
- After significant UI changes
- As part of CI/CD pipeline (nightly builds)
- When testing critical user flows
- Before major releases
