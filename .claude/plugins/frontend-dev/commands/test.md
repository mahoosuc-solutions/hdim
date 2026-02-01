---
name: /frontend-dev:test
description: Run frontend unit tests with Vitest and generate coverage reports
args:
  mode:
    description: Test mode (run, watch, ui, coverage)
    required: false
    default: watch
  pattern:
    description: Test file pattern to match (e.g., PatientCard)
    required: false
---

# Run Frontend Tests

Execute unit tests using Vitest and React Testing Library.

## Usage Examples

```bash
# Run tests in watch mode (default)
/frontend-dev:test

# Run tests once (CI mode)
/frontend-dev:test run

# Run with UI
/frontend-dev:test ui

# Generate coverage report
/frontend-dev:test coverage

# Run specific test file
/frontend-dev:test watch PatientCard
```

## Test Modes

### 1. Watch Mode (Development)

```bash
cd frontend
npm run test
```

**Features:**
- ✅ Re-runs tests on file changes
- ✅ Interactive mode (filter by file, pattern)
- ✅ Fast feedback loop
- ✅ Only runs affected tests

**Output:**
```
 RERUN  src/components/PatientCard.test.tsx

 ✓ src/components/PatientCard.test.tsx (3)
   ✓ PatientCard (3)
     ✓ should render patient name
     ✓ should display patient age
     ✓ should handle click event

 Test Files  1 passed (1)
      Tests  3 passed (3)
   Start at  14:23:15
   Duration  234ms

 PASS  Waiting for file changes...
```

### 2. Run Once (CI Mode)

```bash
npm run test:run
```

**Features:**
- ✅ Runs all tests once and exits
- ✅ Useful for CI/CD pipelines
- ✅ Fails with exit code 1 if tests fail

### 3. UI Mode

```bash
npm run test:ui
```

**Features:**
- ✅ Visual test runner in browser
- ✅ File tree navigation
- ✅ Test filtering and search
- ✅ Time travel debugging
- ✅ Console output per test

**Access:** Opens browser at `http://localhost:51204/__vitest__/`

### 4. Coverage Mode

```bash
npm run test:coverage
```

**Generates:**
- HTML report: `coverage/index.html`
- JSON summary: `coverage/coverage-summary.json`
- Terminal summary

**Example Output:**
```
 % Coverage report from v8
-------------|---------|----------|---------|---------|-------------------
File         | % Stmts | % Branch | % Funcs | % Lines | Uncovered Line #s
-------------|---------|----------|---------|---------|-------------------
All files    |   78.45 |    65.23 |   82.14 |   78.91 |
 components  |   85.12 |    71.33 |   88.24 |   85.67 |
  PatientCard|   92.00 |    85.71 |   100.0 |   92.31 | 45-48
  Dashboard  |   65.00 |    50.00 |   75.00 |   65.22 | 78-92,120-135
 hooks       |   88.33 |    75.00 |   90.00 |   88.89 |
  usePatient |   95.00 |    83.33 |   100.0 |   95.00 | 23
 utils       |   100.0 |    100.0 |   100.0 |   100.0 |
-------------|---------|----------|---------|---------|-------------------
```

## Coverage Targets

| Category | Minimum | Target | Excellent |
|----------|---------|--------|-----------|
| **Statements** | 70% | 80% | 90% |
| **Branches** | 65% | 75% | 85% |
| **Functions** | 70% | 80% | 90% |
| **Lines** | 70% | 80% | 90% |

## Test Filtering

```bash
# Run tests matching pattern
npm run test PatientCard

# Run tests in specific directory
npm run test -- --dir=components

# Run tests with specific name
npm run test -- --grep="should render"
```

## Common Test Patterns

### Component Testing

```typescript
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PatientCard } from './PatientCard';

describe('PatientCard', () => {
  it('should render patient information', () => {
    render(<PatientCard patient={mockPatient} />);

    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText('Age: 45')).toBeInTheDocument();
  });

  it('should handle click events', async () => {
    const onClick = vi.fn();
    render(<PatientCard patient={mockPatient} onClick={onClick} />);

    await userEvent.click(screen.getByRole('button'));
    expect(onClick).toHaveBeenCalledWith(mockPatient.id);
  });

  it('should be accessible', () => {
    render(<PatientCard patient={mockPatient} />);

    expect(screen.getByRole('article')).toHaveAccessibleName('Patient card for John Doe');
  });
});
```

### Hook Testing

```typescript
import { renderHook, waitFor } from '@testing-library/react';
import { usePatientData } from './usePatientData';

describe('usePatientData', () => {
  it('should fetch patient data', async () => {
    const { result } = renderHook(() => usePatientData('123'));

    expect(result.current.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.data).toEqual(mockPatient);
  });
});
```

### Async Testing

```typescript
it('should display data after loading', async () => {
  render(<PatientList />);

  // Initially shows loading
  expect(screen.getByText('Loading...')).toBeInTheDocument();

  // Wait for data to load
  await waitFor(() => {
    expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
  });

  // Verify data displayed
  expect(screen.getByText('John Doe')).toBeInTheDocument();
});
```

## Test Best Practices

### ✅ DO

```typescript
// User-centric queries
screen.getByRole('button', { name: /submit/i });
screen.getByLabelText('Email address');
screen.getByText('Welcome back!');

// Test behavior, not implementation
expect(screen.getByText('Success')).toBeInTheDocument();

// Async utilities
await waitFor(() => expect(mockFn).toHaveBeenCalled());

// Accessibility checks
expect(button).toHaveAccessibleName('Delete patient');
```

### ❌ DON'T

```typescript
// Don't query by class or test IDs
container.querySelector('.patient-card'); // ❌
screen.getByTestId('patient-card'); // ❌ (use as last resort)

// Don't test implementation details
expect(wrapper.state().count).toBe(5); // ❌
expect(mockComponent.props.onClick).toBeDefined(); // ❌

// Don't use act() manually (use RTL utilities)
act(() => {
  // ❌ RTL handles this
});
```

## Troubleshooting

### Tests Timing Out

```typescript
// Increase timeout for slow operations
it('should load data', async () => {
  // ...
}, 10000); // 10 second timeout

// Or globally in vitest.config.ts
export default defineConfig({
  test: {
    testTimeout: 10000
  }
});
```

### Mock Not Working

```typescript
// Ensure mocks are setup before importing component
vi.mock('./api', () => ({
  fetchPatients: vi.fn(() => Promise.resolve(mockData))
}));

import { PatientList } from './PatientList'; // Import after mock
```

### Memory Leaks

```typescript
afterEach(() => {
  // Cleanup
  vi.clearAllMocks();
  cleanup(); // RTL cleanup (usually automatic)
});
```

## CI/CD Integration

**GitHub Actions Example:**
```yaml
- name: Run frontend tests
  run: |
    cd frontend
    npm ci
    npm run test:run

- name: Generate coverage report
  run: |
    cd frontend
    npm run test:coverage

- name: Upload coverage to Codecov
  uses: codecov/codecov-action@v3
  with:
    directory: ./frontend/coverage

- name: Enforce coverage threshold
  run: |
    cd frontend
    # Fail if coverage < 80%
    npm run test:coverage -- --coverage.lines=80
```

## Output Examples

### Success ✅

```
✅ All tests passed!

 Test Files  12 passed (12)
      Tests  145 passed (145)
   Start at  14:23:15
   Duration  2.34s (in thread 1.89s, 124% of CPU)

📊 Coverage: 82.5% statements, 75.3% branches
```

### Failure ❌

```
❌ Tests failed!

 FAIL  src/components/PatientCard.test.tsx > PatientCard > should render patient name
AssertionError: expected 'Jane Doe' to equal 'John Doe'

 Test Files  1 failed | 11 passed (12)
      Tests  1 failed | 144 passed (145)

💡 Tip: Run with npm run test:ui to debug failed tests
```

### Coverage Report 📊

```
📊 Coverage Report

File                        | % Stmts | % Branch | % Funcs | % Lines | Uncovered Lines
--------------------------- |---------|----------|---------|---------|----------------
All files                   |   82.5  |   75.3   |   85.1  |   82.9  |
 components/PatientCard     |   95.0  |   88.9   |   100   |   95.2  | 45-48
 components/Dashboard       |   68.0  |   55.0   |   75.0  |   68.2  | 78-92, 120-135 🔴
 hooks/usePatientData       |   90.0  |   83.3   |   100   |   90.0  |

🔴 Low coverage: components/Dashboard (68%)
💡 Recommendation: Add tests for error handling and edge cases
```

---

**When to Run:**
- During development (watch mode)
- Before committing code
- In CI/CD pipeline (run mode)
- Before PRs (coverage check)
- After refactoring (regression check)
