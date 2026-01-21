---
name: /frontend-dev:feature-dev
description: Guided workflow for developing a complete frontend feature with tests, accessibility, and code review
args:
  feature:
    description: Feature description (e.g., "patient search with filtering")
    required: true
---

# Frontend Feature Development Workflow

Complete guided workflow for implementing a frontend feature from planning to deployment.

## Usage

```bash
/frontend-dev:feature-dev "patient search with advanced filtering"
```

## Workflow Stages

### Stage 1: Planning & Design

**Objective:** Understand requirements and plan implementation

**Questions to Answer:**
1. **What is the feature?**
   - User story/requirement
   - Acceptance criteria

2. **What components are needed?**
   - New components to create
   - Existing components to modify

3. **What data is needed?**
   - API endpoints
   - Data models/types
   - State management approach

4. **What are the user flows?**
   - Primary flow (happy path)
   - Edge cases
   - Error scenarios

**Deliverables:**
- [ ] Feature specification document
- [ ] Component hierarchy diagram
- [ ] API integration plan
- [ ] State management plan

**Example Output:**
```markdown
## Feature: Patient Search with Filtering

### Requirements
- Users can search patients by name, ID, or DOB
- Apply filters: age range, status, insurance
- Real-time search results
- Export results to CSV

### Components
1. **SearchBar** (new) - Search input with autocomplete
2. **FilterPanel** (new) - Filter controls
3. **PatientResultsList** (new) - Display results with virtualization
4. **ExportButton** (existing) - Reuse for CSV export

### API Integration
- GET /api/patients?search={query}&filters={filters}
- Uses existing patient service

### State Management
- Zustand store: `usePatientSearchStore`
- State: { query, filters, results, loading, error }
```

---

### Stage 2: Setup & Scaffolding

**Objective:** Create file structure and boilerplate

**Tasks:**
1. Create TypeScript types
2. Generate components with tests
3. Setup state management
4. Create API service functions

**Commands:**
```bash
# Generate types
# Create: src/types/patient-search.ts

# Generate components
/frontend-dev:generate component SearchBar
/frontend-dev:generate component FilterPanel
/frontend-dev:generate component PatientResultsList

# Generate custom hook
/frontend-dev:generate hook usePatientSearch

# Generate Zustand store
# Create: src/store/patient-search-store.ts
```

**Deliverables:**
- [ ] TypeScript type definitions
- [ ] Component scaffolds with tests
- [ ] Custom hooks
- [ ] State management setup
- [ ] API service functions

**File Structure Created:**
```
src/
├── components/
│   ├── SearchBar.tsx
│   ├── SearchBar.test.tsx
│   ├── FilterPanel.tsx
│   ├── FilterPanel.test.tsx
│   ├── PatientResultsList.tsx
│   └── PatientResultsList.test.tsx
├── hooks/
│   ├── usePatientSearch.ts
│   └── usePatientSearch.test.ts
├── store/
│   └── patient-search-store.ts
├── services/
│   └── patient-search-service.ts
└── types/
    └── patient-search.ts
```

---

### Stage 3: Implementation

**Objective:** Build feature functionality

**Implementation Order:**

#### 3.1 Types First
```typescript
// src/types/patient-search.ts
export interface PatientSearchFilters {
  ageRange?: { min: number; max: number };
  status?: 'active' | 'inactive';
  insurance?: string;
}

export interface PatientSearchResult {
  id: string;
  name: string;
  dateOfBirth: string;
  status: string;
  insurance: string;
}

export interface PatientSearchState {
  query: string;
  filters: PatientSearchFilters;
  results: PatientSearchResult[];
  loading: boolean;
  error: string | null;
}
```

#### 3.2 API Service
```typescript
// src/services/patient-search-service.ts
export async function searchPatients(
  query: string,
  filters: PatientSearchFilters
): Promise<PatientSearchResult[]> {
  const params = new URLSearchParams({
    search: query,
    ...filters
  });

  const response = await fetch(`/api/patients?${params}`);
  if (!response.ok) throw new Error('Search failed');

  return response.json();
}
```

#### 3.3 State Management
```typescript
// src/store/patient-search-store.ts
import { create } from 'zustand';

export const usePatientSearchStore = create<PatientSearchState>((set) => ({
  query: '',
  filters: {},
  results: [],
  loading: false,
  error: null,

  setQuery: (query: string) => set({ query }),
  setFilters: (filters: PatientSearchFilters) => set({ filters }),
  setResults: (results: PatientSearchResult[]) => set({ results }),
  setLoading: (loading: boolean) => set({ loading }),
  setError: (error: string | null) => set({ error })
}));
```

#### 3.4 Custom Hook
```typescript
// src/hooks/usePatientSearch.ts
import { useEffect } from 'react';
import { usePatientSearchStore } from '../store/patient-search-store';
import { searchPatients } from '../services/patient-search-service';

export function usePatientSearch() {
  const { query, filters, setResults, setLoading, setError } = usePatientSearchStore();

  useEffect(() => {
    if (!query) return;

    const search = async () => {
      setLoading(true);
      setError(null);

      try {
        const results = await searchPatients(query, filters);
        setResults(results);
      } catch (error) {
        setError(error.message);
      } finally {
        setLoading(false);
      }
    };

    const debounced = setTimeout(search, 300);
    return () => clearTimeout(debounced);
  }, [query, filters]);
}
```

#### 3.5 Components (Learning Mode - User Implements Business Logic)

**SearchBar Component:**
```typescript
// src/components/SearchBar.tsx
import React from 'react';
import { TextField, InputAdornment } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import { usePatientSearchStore } from '../store/patient-search-store';

export const SearchBar: React.FC = () => {
  const { query, setQuery } = usePatientSearchStore();

  return (
    <TextField
      fullWidth
      value={query}
      onChange={(e) => setQuery(e.target.value)}
      placeholder="Search patients by name, ID, or DOB..."
      InputProps={{
        startAdornment: (
          <InputAdornment position="start">
            <SearchIcon />
          </InputAdornment>
        )
      }}
      aria-label="Search patients"
    />
  );
};
```

**🎓 Learning Opportunity - FilterPanel:**
I've created the FilterPanel structure. Now, implement the filter state management logic that determines how users combine multiple filters (AND vs OR logic) and handle edge cases like empty filter values.

```typescript
// src/components/FilterPanel.tsx
// TODO: Implement handleFilterChange function
// Consider: Should filters be combined with AND or OR?
// Should empty filter values be ignored or treated as "all"?

const handleFilterChange = (filterType: string, value: unknown) => {
  // Your implementation here
};
```

**Deliverables:**
- [ ] All components implemented
- [ ] State management working
- [ ] API integration complete
- [ ] User interactions functional

---

### Stage 4: Testing

**Objective:** Achieve comprehensive test coverage

**Test Categories:**

#### 4.1 Unit Tests (Components)
```typescript
// SearchBar.test.tsx
describe('SearchBar', () => {
  it('should update query on input change', async () => {
    render(<SearchBar />);
    const input = screen.getByRole('textbox', { name: /search patients/i });

    await userEvent.type(input, 'John Doe');
    expect(input).toHaveValue('John Doe');
  });

  it('should be accessible', () => {
    render(<SearchBar />);
    expect(screen.getByRole('textbox')).toHaveAccessibleName('Search patients');
  });
});
```

#### 4.2 Unit Tests (Hooks)
```typescript
// usePatientSearch.test.ts
describe('usePatientSearch', () => {
  it('should debounce search requests', async () => {
    vi.useFakeTimers();
    const { result } = renderHook(() => usePatientSearch());

    act(() => {
      result.current.setQuery('John');
    });

    expect(searchPatients).not.toHaveBeenCalled();

    vi.advanceTimersByTime(300);
    await waitFor(() => {
      expect(searchPatients).toHaveBeenCalledWith('John', {});
    });
  });
});
```

#### 4.3 Integration Tests
```typescript
// PatientSearch.integration.test.tsx
describe('Patient Search Feature', () => {
  it('should search and display results', async () => {
    render(<PatientSearchPage />);

    await userEvent.type(
      screen.getByRole('textbox', { name: /search/i }),
      'John Doe'
    );

    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument();
      expect(screen.getByText('Results: 1')).toBeInTheDocument();
    });
  });
});
```

#### 4.4 E2E Tests
```typescript
// e2e/tests/patient-search.spec.ts
test('complete search workflow', async ({ page }) => {
  await page.goto('/patients');

  // Search
  await page.fill('[aria-label="Search patients"]', 'John');
  await page.waitForResponse(response =>
    response.url().includes('/api/patients')
  );

  // Verify results
  await expect(page.getByText('John Doe')).toBeVisible();

  // Apply filter
  await page.click('button:has-text("Filters")');
  await page.selectOption('[name="status"]', 'active');

  // Export results
  const downloadPromise = page.waitForEvent('download');
  await page.click('button:has-text("Export")');
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toContain('.csv');
});
```

**Coverage Target:** ≥85% for new code

**Run Tests:**
```bash
# Unit tests
npm run test

# Coverage
npm run test:coverage

# E2E
npm run e2e
```

**Deliverables:**
- [ ] Unit tests for all components
- [ ] Unit tests for hooks
- [ ] Integration tests for workflows
- [ ] E2E test for critical path
- [ ] Coverage ≥85%

---

### Stage 5: Accessibility Audit

**Objective:** Ensure WCAG 2.1 AA compliance

**Run Accessibility Agent:**
```bash
# Use the accessibility analyzer agent
# Reviews: keyboard nav, ARIA, screen reader, contrast
```

**Checklist:**
- [ ] Keyboard navigation works (Tab, Enter, Escape)
- [ ] Screen reader announces search results
- [ ] Focus indicators visible
- [ ] ARIA labels on all interactive elements
- [ ] Color contrast ≥4.5:1
- [ ] Form inputs have associated labels
- [ ] Error messages linked to inputs

**Common Fixes:**
```typescript
// Add ARIA live region for results
<div role="status" aria-live="polite" aria-atomic="true">
  {results.length} patients found
</div>

// Accessible error messages
<TextField
  error={!!error}
  helperText={error}
  aria-errormessage={error ? 'search-error' : undefined}
/>
```

**Deliverables:**
- [ ] Accessibility audit report
- [ ] All WCAG violations fixed
- [ ] Keyboard navigation tested
- [ ] Screen reader tested

---

### Stage 6: Code Review

**Objective:** Ensure code quality and best practices

**Run Code Review Agent:**
```bash
# Use frontend code reviewer agent
# Reviews: React patterns, TypeScript, performance, testing
```

**Review Areas:**
1. **React Best Practices**
   - Proper hooks usage
   - Memoization where needed
   - No unnecessary re-renders

2. **TypeScript Quality**
   - No `any` types
   - Proper interfaces
   - Type safety

3. **Performance**
   - Bundle impact
   - List virtualization (if needed)
   - Code splitting

4. **Testing Quality**
   - Test coverage adequate
   - Tests are meaningful
   - Edge cases covered

**Address Feedback:**
```typescript
// Example fix: Add React.memo to prevent re-renders
export const PatientResultsList = React.memo(({ results }) => {
  // Component implementation
});

// Example fix: Use useCallback for handlers
const handleFilterChange = useCallback((filters) => {
  setFilters(filters);
}, []);
```

**Deliverables:**
- [ ] Code review report
- [ ] All critical issues fixed
- [ ] Warnings addressed
- [ ] Best practices followed

---

### Stage 7: Performance Optimization

**Objective:** Ensure optimal performance

**Run Performance Analyzer:**
```bash
# Use performance analyzer agent
# Checks: bundle size, re-renders, list virtualization
```

**Optimizations:**

#### 7.1 List Virtualization
```typescript
// For long result lists (>100 items)
import { FixedSizeList } from 'react-window';

<FixedSizeList
  height={600}
  itemCount={results.length}
  itemSize={80}
  width="100%"
>
  {({ index, style }) => (
    <div style={style}>
      <PatientCard patient={results[index]} />
    </div>
  )}
</FixedSizeList>
```

#### 7.2 Debouncing Search
```typescript
// Already implemented in usePatientSearch hook
const debounced = setTimeout(search, 300);
```

#### 7.3 Code Splitting
```typescript
// Lazy load heavy components
const FilterPanel = lazy(() => import('./FilterPanel'));

<Suspense fallback={<Skeleton />}>
  <FilterPanel />
</Suspense>
```

**Deliverables:**
- [ ] Performance audit report
- [ ] Bundle size within target
- [ ] No performance warnings
- [ ] Optimizations applied

---

### Stage 8: Documentation

**Objective:** Document the feature for team

**Create Documentation:**

**Component Documentation:**
```typescript
/**
 * PatientSearchBar
 *
 * @description Search input for finding patients by name, ID, or DOB.
 * Features debounced search and autocomplete suggestions.
 *
 * @example
 * ```tsx
 * <SearchBar />
 * ```
 *
 * @accessibility
 * - Keyboard accessible (Tab, Enter)
 * - Screen reader announces results count
 * - ARIA label: "Search patients"
 */
export const SearchBar: React.FC = () => { /* ... */ };
```

**Usage Guide:**
```markdown
# Patient Search Feature

## Usage

1. Navigate to Patients page
2. Enter search query in search bar
3. Apply filters (optional)
4. View results
5. Export to CSV (optional)

## Technical Details

### State Management
- Store: `usePatientSearchStore`
- Hook: `usePatientSearch`

### API
- Endpoint: GET /api/patients
- Params: `search`, `ageMin`, `ageMax`, `status`, `insurance`

### Testing
- Unit tests: `npm run test SearchBar`
- E2E tests: `npm run e2e -- patient-search`

## Troubleshooting

**Issue:** Search not working
**Fix:** Check network tab for API errors

**Issue:** Results not updating
**Fix:** Clear filters and retry
```

**Deliverables:**
- [ ] Component JSDoc comments
- [ ] Usage guide
- [ ] API documentation
- [ ] Troubleshooting guide

---

### Stage 9: Final QA

**Objective:** Final quality check before deployment

**Checklist:**
- [ ] All tests passing (unit + integration + E2E)
- [ ] Linting passing (no errors)
- [ ] Build succeeds
- [ ] Accessibility audit passed
- [ ] Code review approved
- [ ] Performance targets met
- [ ] Documentation complete
- [ ] Manual testing completed

**Manual Testing:**
1. Happy path (search → results → export)
2. Empty search
3. No results found
4. API error scenario
5. Network failure handling
6. Browser compatibility (Chrome, Firefox, Safari)
7. Responsive design (mobile, tablet, desktop)

**Build & Deploy:**
```bash
# Run full test suite
npm run test:run
npm run e2e

# Lint check
npm run lint

# Build
npm run build

# Verify build
npm run preview
```

**Deliverables:**
- [ ] QA checklist completed
- [ ] Manual testing passed
- [ ] Production build successful
- [ ] Ready for deployment

---

## Workflow Summary

```
1. Planning → Feature spec, component plan
2. Setup → Scaffold files, types, tests
3. Implementation → Build functionality
4. Testing → Unit, integration, E2E (≥85% coverage)
5. Accessibility → WCAG 2.1 AA compliance
6. Code Review → Quality, best practices
7. Performance → Optimize bundle, rendering
8. Documentation → Usage guide, API docs
9. Final QA → Manual testing, build

Total Time: 2-5 days (depending on complexity)
```

## Output Example

```markdown
✅ Feature Development Complete: Patient Search

## Summary
- Components: 3 created, 1 modified
- Tests: 24 passing (87% coverage)
- Accessibility: WCAG 2.1 AA compliant
- Performance: Bundle +15KB (within target)
- Documentation: Complete

## Files Changed
- src/components/SearchBar.tsx (new)
- src/components/FilterPanel.tsx (new)
- src/components/PatientResultsList.tsx (new)
- src/hooks/usePatientSearch.ts (new)
- src/store/patient-search-store.ts (new)
- + 24 test files

## Next Steps
1. Create PR: git checkout -b feature/patient-search
2. Run final checks: npm run test && npm run lint && npm run e2e
3. Request review from: @frontend-team
4. Deploy to staging after approval

🎉 Ready for production!
```

---

**This workflow ensures:**
- ✅ High code quality
- ✅ Comprehensive testing
- ✅ Accessibility compliance
- ✅ Performance optimization
- ✅ Proper documentation
- ✅ Team collaboration
