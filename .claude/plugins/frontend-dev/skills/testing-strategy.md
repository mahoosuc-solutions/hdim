---
name: frontend-dev:testing-strategy
description: Comprehensive testing strategies for React applications using Vitest, React Testing Library, and Playwright
---

# Frontend Testing Strategy

Complete guide to testing React applications in the HDIM platform.

## Testing Pyramid

```
     /\
    /E2E\         ← Few (Critical user flows)
   /──────\
  /  INT   \      ← Some (Feature integration)
 /──────────\
/    UNIT    \    ← Many (Components, hooks, utils)
──────────────
```

**Ratio:** 70% Unit, 20% Integration, 10% E2E

---

## Table of Contents
1. [Unit Testing](#unit-testing)
2. [Integration Testing](#integration-testing)
3. [E2E Testing](#e2e-testing)
4. [Testing Best Practices](#testing-best-practices)
5. [Common Testing Patterns](#common-testing-patterns)
6. [Mocking Strategies](#mocking-strategies)

---

## Unit Testing

### Component Testing with React Testing Library

**Philosophy:** Test behavior, not implementation

```typescript
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PatientCard } from './PatientCard';

describe('PatientCard', () => {
  const mockPatient = {
    id: '123',
    name: 'John Doe',
    age: 45,
    status: 'active'
  };

  it('should render patient information', () => {
    render(<PatientCard patient={mockPatient} />);

    // Query by role (most accessible)
    expect(screen.getByRole('heading', { name: /john doe/i })).toBeInTheDocument();

    // Query by text
    expect(screen.getByText('Age: 45')).toBeInTheDocument();
  });

  it('should handle click events', async () => {
    const onClick = vi.fn();
    render(<PatientCard patient={mockPatient} onClick={onClick} />);

    // User interaction
    await userEvent.click(screen.getByRole('button', { name: /view details/i }));

    // Assert callback called
    expect(onClick).toHaveBeenCalledWith('123');
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('should display loading state', () => {
    render(<PatientCard patient={mockPatient} loading />);

    expect(screen.getByRole('progressbar')).toBeInTheDocument();
    expect(screen.queryByText('John Doe')).not.toBeInTheDocument();
  });

  it('should be accessible', () => {
    render(<PatientCard patient={mockPatient} />);

    const article = screen.getByRole('article');
    expect(article).toHaveAccessibleName('Patient card for John Doe');
  });
});
```

### Query Priority (RTL Best Practice)

```typescript
// 1. getByRole (BEST - most accessible)
screen.getByRole('button', { name: /submit/i });
screen.getByRole('textbox', { name: /email/i });
screen.getByRole('heading', { name: /dashboard/i });

// 2. getByLabelText (for form inputs)
screen.getByLabelText('Email address');

// 3. getByPlaceholderText
screen.getByPlaceholderText('Enter patient name');

// 4. getByText
screen.getByText('Welcome back!');

// 5. getByTestId (LAST RESORT)
screen.getByTestId('patient-card-123');
```

### Async Testing

```typescript
import { waitFor, findBy } from '@testing-library/react';

describe('PatientList', () => {
  it('should load and display patients', async () => {
    render(<PatientList />);

    // Initially shows loading
    expect(screen.getByText('Loading...')).toBeInTheDocument();

    // Wait for data to appear (using findBy - combines getBy + waitFor)
    const patient = await screen.findByText('John Doe');
    expect(patient).toBeInTheDocument();

    // Alternative: explicit waitFor
    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument();
    });

    // Loading should be gone
    expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
  });

  it('should handle API errors', async () => {
    // Mock API failure
    vi.spyOn(global, 'fetch').mockRejectedValueOnce(new Error('API Error'));

    render(<PatientList />);

    // Wait for error message
    const error = await screen.findByText('Failed to load patients');
    expect(error).toBeInTheDocument();
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

    // Initial state
    expect(result.current.loading).toBe(true);
    expect(result.current.data).toBeNull();

    // Wait for data to load
    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    // Verify final state
    expect(result.current.data).toEqual({ id: '123', name: 'John Doe' });
    expect(result.current.error).toBeNull();
  });

  it('should refetch when patientId changes', async () => {
    const { result, rerender } = renderHook(
      ({ id }) => usePatientData(id),
      { initialProps: { id: '123' } }
    );

    await waitFor(() => expect(result.current.loading).toBe(false));

    // Change patient ID
    rerender({ id: '456' });

    // Should refetch
    expect(result.current.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.data?.id).toBe('456');
    });
  });
});
```

### Utility Function Testing

```typescript
import { formatPatientName, calculateAge } from './patient-utils';

describe('formatPatientName', () => {
  it('should format name correctly', () => {
    expect(formatPatientName('John', 'Doe')).toBe('Doe, John');
  });

  it('should handle missing last name', () => {
    expect(formatPatientName('John', '')).toBe('John');
  });

  it('should handle null/undefined', () => {
    expect(formatPatientName(null, null)).toBe('');
  });
});

describe('calculateAge', () => {
  it('should calculate age from date of birth', () => {
    const dob = new Date('1980-01-01');
    const age = calculateAge(dob);

    expect(age).toBeGreaterThanOrEqual(44);
  });

  it('should handle future dates', () => {
    const futureDob = new Date('2030-01-01');
    expect(calculateAge(futureDob)).toBe(0);
  });
});
```

---

## Integration Testing

### Feature Integration Tests

```typescript
// Test complete feature workflows
describe('Patient Search Integration', () => {
  beforeEach(() => {
    // Setup: Mock API
    vi.spyOn(global, 'fetch').mockImplementation((url) => {
      if (url.includes('/api/patients?search=John')) {
        return Promise.resolve({
          json: () => Promise.resolve([
            { id: '1', name: 'John Doe', age: 45 },
            { id: '2', name: 'John Smith', age: 32 }
          ])
        });
      }
      return Promise.resolve({ json: () => Promise.resolve([]) });
    });
  });

  it('should search, filter, and export patients', async () => {
    render(<PatientSearchPage />);

    // 1. Search
    const searchInput = screen.getByRole('textbox', { name: /search/i });
    await userEvent.type(searchInput, 'John');

    // Wait for results
    await screen.findByText('John Doe');
    expect(screen.getByText('John Smith')).toBeInTheDocument();

    // 2. Apply filter
    await userEvent.click(screen.getByRole('button', { name: /filters/i }));
    await userEvent.click(screen.getByRole('checkbox', { name: /active only/i }));

    // 3. Export
    await userEvent.click(screen.getByRole('button', { name: /export/i }));

    // Verify export triggered
    expect(mockExportFunction).toHaveBeenCalled();
  });
});
```

### Multi-Component Integration

```typescript
// Test interaction between multiple components
describe('Dashboard Integration', () => {
  it('should update chart when filter changes', async () => {
    render(<Dashboard />);

    // Initial state
    const chart = screen.getByRole('img', { name: /patient statistics/i });
    expect(chart).toBeInTheDocument();

    // Change filter
    await userEvent.selectOptions(
      screen.getByRole('combobox', { name: /time range/i }),
      'last-month'
    );

    // Wait for chart to update
    await waitFor(() => {
      expect(screen.getByText('Showing data for: Last Month')).toBeInTheDocument();
    });
  });
});
```

---

## E2E Testing

### Page Object Model Pattern

```typescript
// e2e/pages/patients-page.ts
import { Page, Locator } from '@playwright/test';

export class PatientsPage {
  readonly page: Page;
  readonly searchInput: Locator;
  readonly addButton: Locator;
  readonly patientsTable: Locator;

  constructor(page: Page) {
    this.page = page;
    this.searchInput = page.getByRole('textbox', { name: /search/i });
    this.addButton = page.getByRole('button', { name: /add patient/i });
    this.patientsTable = page.getByRole('table', { name: /patients/i });
  }

  async goto() {
    await this.page.goto('/patients');
  }

  async search(query: string) {
    await this.searchInput.fill(query);
  }

  async getPatientCount(): Promise<number> {
    return await this.patientsTable.locator('tbody tr').count();
  }

  async clickPatient(name: string) {
    await this.page.getByRole('row', { name: new RegExp(name, 'i') }).click();
  }
}
```

### E2E Test Scenarios

```typescript
// e2e/tests/patient-workflow.spec.ts
import { test, expect } from '@playwright/test';
import { PatientsPage } from '../pages/patients-page';

test.describe('Patient Management Workflow', () => {
  let patientsPage: PatientsPage;

  test.beforeEach(async ({ page }) => {
    patientsPage = new PatientsPage(page);
    await patientsPage.goto();
  });

  test('complete patient lifecycle', async ({ page }) => {
    // 1. Create patient
    await patientsPage.addButton.click();
    await page.fill('[name="firstName"]', 'Jane');
    await page.fill('[name="lastName"]', 'Doe');
    await page.fill('[name="dateOfBirth"]', '1990-05-15');
    await page.click('button[type="submit"]');

    // Verify creation
    await expect(page.getByText('Patient created successfully')).toBeVisible();

    // 2. Search for patient
    await patientsPage.search('Jane Doe');
    await expect(page.getByText('Jane Doe')).toBeVisible();

    // 3. View details
    await patientsPage.clickPatient('Jane Doe');
    await expect(page).toHaveURL(/\/patients\/\d+/);

    // 4. Update patient
    await page.click('button:has-text("Edit")');
    await page.fill('[name="phone"]', '555-1234');
    await page.click('button[type="submit"]');

    // Verify update
    await expect(page.getByText('555-1234')).toBeVisible();

    // 5. Delete patient
    await page.click('button:has-text("Delete")');
    await page.click('button:has-text("Confirm")');

    // Verify deletion
    await expect(page).toHaveURL('/patients');
    await expect(page.getByText('Patient deleted')).toBeVisible();
  });
});
```

---

## Testing Best Practices

### ✅ DO

```typescript
// Test user behavior, not implementation
it('should submit form when button clicked', async () => {
  render(<LoginForm />);

  await userEvent.type(screen.getByLabelText('Email'), 'test@example.com');
  await userEvent.type(screen.getByLabelText('Password'), 'password123');
  await userEvent.click(screen.getByRole('button', { name: /submit/i }));

  await waitFor(() => {
    expect(mockLogin).toHaveBeenCalledWith('test@example.com', 'password123');
  });
});

// Use accessible queries
screen.getByRole('button', { name: /submit/i });
screen.getByLabelText('Email address');

// Test edge cases
it('should handle empty search results', async () => {
  render(<PatientSearch />);
  await userEvent.type(screen.getByRole('textbox'), 'NonexistentName');

  await expect(screen.findByText('No patients found')).resolves.toBeInTheDocument();
});

// Cleanup side effects
afterEach(() => {
  vi.clearAllMocks();
});
```

### ❌ DON'T

```typescript
// Don't test implementation details
it('should have correct state', () => {
  const wrapper = shallow(<Component />);
  expect(wrapper.state().count).toBe(0); // ❌
});

// Don't query by class or test ID (unless necessary)
screen.getByClassName('patient-card'); // ❌
screen.getByTestId('patient-123'); // ❌ Last resort only

// Don't test library code
it('useState should work', () => {
  // ❌ Don't test React itself
});

// Don't use arbitrary waits
await new Promise(resolve => setTimeout(resolve, 1000)); // ❌
// Use waitFor instead ✅
await waitFor(() => expect(screen.getByText('Loaded')).toBeInTheDocument());
```

---

## Common Testing Patterns

### Testing Forms

```typescript
it('should validate form and submit', async () => {
  const onSubmit = vi.fn();
  render(<PatientForm onSubmit={onSubmit} />);

  // Leave required field empty
  await userEvent.click(screen.getByRole('button', { name: /submit/i }));

  // Should show validation error
  expect(await screen.findByText('Name is required')).toBeInTheDocument();
  expect(onSubmit).not.toHaveBeenCalled();

  // Fill form correctly
  await userEvent.type(screen.getByLabelText('Name'), 'John Doe');
  await userEvent.type(screen.getByLabelText('Email'), 'john@example.com');

  // Submit should work
  await userEvent.click(screen.getByRole('button', { name: /submit/i }));

  expect(onSubmit).toHaveBeenCalledWith({
    name: 'John Doe',
    email: 'john@example.com'
  });
});
```

### Testing Lists with Virtualization

```typescript
it('should render virtualized list', async () => {
  const patients = Array.from({ length: 1000 }, (_, i) => ({
    id: `${i}`,
    name: `Patient ${i}`
  }));

  render(<VirtualizedPatientList patients={patients} />);

  // Only visible items should be in DOM
  expect(screen.getAllByRole('listitem')).toHaveLength(10); // Assuming 10 visible

  // Scroll to bottom
  const list = screen.getByRole('list');
  fireEvent.scroll(list, { target: { scrollTop: 10000 } });

  // Should render different items
  await waitFor(() => {
    expect(screen.getByText('Patient 990')).toBeInTheDocument();
  });
});
```

### Testing WebSocket Connections

```typescript
it('should handle WebSocket messages', async () => {
  const mockWs = new MockWebSocket();
  vi.spyOn(global, 'WebSocket').mockImplementation(() => mockWs);

  render(<RealtimeDashboard />);

  // Trigger WebSocket message
  act(() => {
    mockWs.triggerMessage({
      type: 'PATIENT_UPDATE',
      payload: { id: '123', status: 'updated' }
    });
  });

  // Verify UI updated
  await waitFor(() => {
    expect(screen.getByText('Patient updated')).toBeInTheDocument();
  });
});
```

---

## Mocking Strategies

### Mock API Calls

```typescript
// Global fetch mock
beforeEach(() => {
  global.fetch = vi.fn();
});

it('should fetch patients', async () => {
  (global.fetch as Mock).mockResolvedValueOnce({
    ok: true,
    json: async () => [{ id: '1', name: 'John Doe' }]
  });

  render(<PatientList />);

  await waitFor(() => {
    expect(screen.getByText('John Doe')).toBeInTheDocument();
  });
});
```

### Mock Modules

```typescript
// Mock entire module
vi.mock('../services/api', () => ({
  fetchPatients: vi.fn(() => Promise.resolve([
    { id: '1', name: 'John Doe' }
  ]))
}));

// Partial mock
vi.mock('../services/api', async () => {
  const actual = await vi.importActual('../services/api');
  return {
    ...actual,
    fetchPatients: vi.fn() // Only mock this function
  };
});
```

### Mock Zustand Store

```typescript
// Mock store for testing
const mockStore = {
  patient: { id: '123', name: 'John Doe' },
  setPatient: vi.fn(),
  clearPatient: vi.fn()
};

vi.mock('../store/patient-store', () => ({
  usePatientStore: (selector) => selector(mockStore)
}));
```

---

## Coverage Targets

| Category | Target | Minimum |
|----------|--------|---------|
| **Statements** | 85% | 75% |
| **Branches** | 80% | 70% |
| **Functions** | 85% | 75% |
| **Lines** | 85% | 75% |

---

**When to use this skill:**
- Writing tests for new components
- Improving test coverage
- Debugging failing tests
- Setting up testing infrastructure
- Code review of test quality
