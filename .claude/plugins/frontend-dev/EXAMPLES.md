# Frontend Dev Plugin - Usage Examples

Real-world examples demonstrating the plugin in action.

---

## Example 1: Generate Patient Status Card

### Command
```bash
/frontend-dev:generate component PatientStatusCard
```

### Generated Files

**`src/components/PatientStatusCard.tsx`**
```typescript
import React from 'react';
import { Card, CardContent, Typography, Chip, Box } from '@mui/material';

/**
 * PatientStatusCard Component
 *
 * ⚠️ HIPAA NOTICE: This component displays PHI
 * - Do NOT log patient data to console
 * - Ensure proper access control
 * - Follow HIPAA cache compliance (TTL ≤ 5 min)
 */
export interface PatientStatusCardProps {
  patientId: string;
  name: string;
  status: 'active' | 'inactive' | 'pending';
  lastVisit?: string;
}

export const PatientStatusCard: React.FC<PatientStatusCardProps> = ({
  patientId,
  name,
  status,
  lastVisit,
}) => {
  const getStatusColor = () => {
    switch (status) {
      case 'active':
        return 'success';
      case 'inactive':
        return 'error';
      case 'pending':
        return 'warning';
      default:
        return 'default';
    }
  };

  return (
    <Card
      sx={{
        p: 2,
        '&:hover': {
          boxShadow: 3,
        },
      }}
      role="article"
      aria-label={`Patient card for ${name}`}
    >
      <CardContent>
        <Box display="flex" justifyContent="space-between" alignItems="center">
          <Typography variant="h6" component="h2">
            {name}
          </Typography>
          <Chip
            label={status.toUpperCase()}
            color={getStatusColor()}
            size="small"
            aria-label={`Patient status: ${status}`}
          />
        </Box>

        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          ID: {patientId}
        </Typography>

        {lastVisit && (
          <Typography variant="body2" color="text.secondary">
            Last Visit: {lastVisit}
          </Typography>
        )}
      </CardContent>
    </Card>
  );
};
```

**`src/components/PatientStatusCard.test.tsx`**
```typescript
import { render, screen } from '@testing-library/react';
import { PatientStatusCard } from './PatientStatusCard';
import { describe, it, expect } from 'vitest';

describe('PatientStatusCard', () => {
  const mockProps = {
    patientId: 'P-12345',
    name: 'John Doe',
    status: 'active' as const,
    lastVisit: '2024-01-15',
  };

  it('should render patient information', () => {
    render(<PatientStatusCard {...mockProps} />);

    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText('ID: P-12345')).toBeInTheDocument();
    expect(screen.getByText('Last Visit: 2024-01-15')).toBeInTheDocument();
  });

  it('should display status chip with correct color', () => {
    render(<PatientStatusCard {...mockProps} />);

    const statusChip = screen.getByLabelText('Patient status: active');
    expect(statusChip).toBeInTheDocument();
    expect(statusChip).toHaveTextContent('ACTIVE');
  });

  it('should be accessible', () => {
    render(<PatientStatusCard {...mockProps} />);

    const card = screen.getByRole('article');
    expect(card).toHaveAccessibleName('Patient card for John Doe');
  });

  it('should handle missing lastVisit', () => {
    const { lastVisit, ...propsWithoutVisit } = mockProps;
    render(<PatientStatusCard {...propsWithoutVisit} />);

    expect(screen.queryByText(/Last Visit/i)).not.toBeInTheDocument();
  });

  it('should render different status colors', () => {
    const { rerender } = render(<PatientStatusCard {...mockProps} status="inactive" />);
    expect(screen.getByText('INACTIVE')).toBeInTheDocument();

    rerender(<PatientStatusCard {...mockProps} status="pending" />);
    expect(screen.getByText('PENDING')).toBeInTheDocument();
  });
});
```

### Run Tests
```bash
/frontend-dev:test watch PatientStatusCard
```

**Output:**
```
✓ src/components/PatientStatusCard.test.tsx (5)
  ✓ PatientStatusCard (5)
    ✓ should render patient information
    ✓ should display status chip with correct color
    ✓ should be accessible
    ✓ should handle missing lastVisit
    ✓ should render different status colors

Test Files  1 passed (1)
     Tests  5 passed (5)
  Duration  234ms
```

---

## Example 2: Generate Custom Hook

### Command
```bash
/frontend-dev:generate hook usePatientData
```

### Generated Files

**`src/hooks/usePatientData.ts`**
```typescript
import { useState, useEffect } from 'react';

export interface UsePatientDataOptions {
  patientId: string;
  autoFetch?: boolean;
}

export interface UsePatientDataReturn {
  data: Patient | null;
  loading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
}

interface Patient {
  id: string;
  name: string;
  age: number;
  status: string;
}

/**
 * usePatientData Hook
 *
 * Fetches and manages patient data with loading and error states.
 *
 * @param options - Configuration options
 * @returns Patient data, loading state, error state, and refetch function
 *
 * @example
 * ```typescript
 * const { data, loading, error, refetch } = usePatientData({
 *   patientId: 'P-12345',
 *   autoFetch: true
 * });
 * ```
 */
export const usePatientData = ({
  patientId,
  autoFetch = true,
}: UsePatientDataOptions): UsePatientDataReturn => {
  const [data, setData] = useState<Patient | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetchPatient = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`/api/patients/${patientId}`);
      if (!response.ok) throw new Error('Failed to fetch patient');

      const patient = await response.json();
      setData(patient);
    } catch (err) {
      setError(err as Error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (autoFetch && patientId) {
      fetchPatient();
    }
  }, [patientId, autoFetch]);

  return { data, loading, error, refetch: fetchPatient };
};
```

**`src/hooks/usePatientData.test.ts`**
```typescript
import { renderHook, waitFor } from '@testing-library/react';
import { usePatientData } from './usePatientData';
import { describe, it, expect, vi, beforeEach } from 'vitest';

describe('usePatientData', () => {
  beforeEach(() => {
    global.fetch = vi.fn();
  });

  it('should return initial state', () => {
    const { result } = renderHook(() =>
      usePatientData({ patientId: 'P-123', autoFetch: false })
    );

    expect(result.current.data).toBeNull();
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('should fetch patient data on mount', async () => {
    const mockPatient = {
      id: 'P-123',
      name: 'John Doe',
      age: 45,
      status: 'active',
    };

    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockPatient,
    });

    const { result } = renderHook(() =>
      usePatientData({ patientId: 'P-123', autoFetch: true })
    );

    expect(result.current.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.data).toEqual(mockPatient);
    expect(result.current.error).toBeNull();
  });

  it('should handle fetch errors', async () => {
    (global.fetch as any).mockResolvedValueOnce({
      ok: false,
    });

    const { result } = renderHook(() =>
      usePatientData({ patientId: 'P-123', autoFetch: true })
    );

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBeTruthy();
    expect(result.current.error?.message).toBe('Failed to fetch patient');
    expect(result.current.data).toBeNull();
  });

  it('should refetch on demand', async () => {
    const mockPatient = { id: 'P-123', name: 'John Doe', age: 45, status: 'active' };

    (global.fetch as any).mockResolvedValue({
      ok: true,
      json: async () => mockPatient,
    });

    const { result } = renderHook(() =>
      usePatientData({ patientId: 'P-123', autoFetch: false })
    );

    expect(result.current.data).toBeNull();

    await result.current.refetch();

    await waitFor(() => {
      expect(result.current.data).toEqual(mockPatient);
    });
  });
});
```

---

## Example 3: Complete Feature Development

### Command
```bash
/frontend-dev:feature-dev "patient search with filtering"
```

### Guided Workflow Output

```markdown
# Feature Development: Patient Search with Filtering

## Stage 1: Planning ✓

Requirements identified:
- Search by name, ID, DOB
- Filters: status, age range, insurance
- Real-time search (debounced)
- Export to CSV

Components needed:
- SearchBar (new)
- FilterPanel (new)
- PatientResultsList (new)
- ExportButton (existing)

## Stage 2: Setup ✓

Files created:
✓ src/types/patient-search.ts
✓ src/components/SearchBar.tsx + test
✓ src/components/FilterPanel.tsx + test
✓ src/components/PatientResultsList.tsx + test
✓ src/hooks/usePatientSearch.ts + test
✓ src/store/patient-search-store.ts
✓ src/services/patient-search-service.ts

## Stage 3: Implementation ✓

Types defined ✓
API service implemented ✓
Zustand store configured ✓
Custom hook created ✓

🎓 Learning Opportunity:
Implement filter logic in FilterPanel.tsx
Consider: AND vs OR for multiple filters?

## Stage 4: Testing ✓

Unit tests: 24/24 passing
Coverage: 87% (target: ≥85%) ✓
Integration tests: 3/3 passing
E2E test: patient-search.spec.ts created

## Stage 5: Accessibility ✓

Accessibility audit complete:
✓ Keyboard navigation works
✓ Screen reader compatible
✓ ARIA labels present
✓ Color contrast 4.8:1 (target: ≥4.5:1)
✓ WCAG 2.1 AA compliant

## Stage 6: Code Review ✓

Code review completed:
✓ React best practices followed
✓ No TypeScript 'any' types
✓ Proper memoization (React.memo, useCallback)
✓ Tests use RTL best practices
⚠️ 1 warning: Consider virtualizing result list for >100 items

## Stage 7: Performance ✓

Performance analysis:
✓ Bundle impact: +18KB (within budget)
✓ No unnecessary re-renders detected
✓ Debounced search (300ms)
⚠️ Recommendation: Add virtualization for large result sets

## Stage 8: Documentation ✓

Documentation created:
✓ Component JSDoc comments
✓ Usage guide in README
✓ API endpoint documentation

## Stage 9: Final QA ✓

Pre-deployment checklist:
✓ All tests passing (27/27)
✓ Linting clean (0 errors)
✓ Build successful
✓ E2E tests passing
✓ Manual testing complete
✓ Browser compatibility verified

✅ Feature ready for production!

Next steps:
1. Create PR: git checkout -b feature/patient-search
2. Run: /frontend-dev:build && /frontend-dev:e2e
3. Request review from @frontend-team
```

---

## Example 4: Accessibility Check

### Scenario: Review Modal Dialog

**Component:**
```typescript
// ConfirmDeleteModal.tsx
<Dialog open={open} onClose={onClose}>
  <DialogTitle>Delete Patient</DialogTitle>
  <DialogContent>
    Are you sure you want to delete this patient record?
  </DialogContent>
  <DialogActions>
    <Button onClick={onClose}>Cancel</Button>
    <Button onClick={onDelete} color="error">Delete</Button>
  </DialogActions>
</Dialog>
```

**Run Accessibility Analyzer:**
```
Agent: accessibility-analyzer
Files: ConfirmDeleteModal.tsx
```

**Output:**
```markdown
# Accessibility Audit: ConfirmDeleteModal

## Critical Issues 🔴

### 1. Missing aria-labelledby
**File:** ConfirmDeleteModal.tsx:3
**Issue:** Dialog missing aria-labelledby attribute
**Impact:** Screen readers won't announce dialog title
**Fix:**
\`\`\`typescript
<Dialog
  open={open}
  onClose={onClose}
  aria-labelledby="dialog-title"
  aria-describedby="dialog-description"
>
  <DialogTitle id="dialog-title">Delete Patient</DialogTitle>
  <DialogContent id="dialog-description">
    Are you sure you want to delete this patient record?
  </DialogContent>
\`\`\`

### 2. Delete button missing aria-label
**File:** ConfirmDeleteModal.tsx:8
**Issue:** Destructive action needs clear label
**Fix:**
\`\`\`typescript
<Button
  onClick={onDelete}
  color="error"
  aria-label="Confirm delete patient record"
>
  Delete
</Button>
\`\`\`

## WCAG Compliance
Status: ❌ Fails (2 violations)
After fixes: ✅ WCAG 2.1 AA Compliant
```

---

## Example 5: Performance Optimization

### Build Analysis

**Command:**
```bash
/frontend-dev:build
```

**Output:**
```
✅ Build successful!

📦 Bundle Analysis:
   main.js:           180 KB (65 KB gzipped) ✅
   vendor-react.js:   140 KB (45 KB gzipped) ✅
   vendor-mui.js:     320 KB (98 KB gzipped) ⚠️
   vendor-charts.js:  150 KB (48 KB gzipped) ✅

   Total:            790 KB (256 KB gzipped)
   Status:           ⚠️ MUI bundle large

🔍 Performance Analyzer Recommendations:

1. 🔴 MUI bundle is 98KB gzipped (target: <85KB)
   Issue: Importing from barrel exports
   Fix:
   // ❌ Current
   import { Button, TextField, Card } from '@mui/material';

   // ✅ Recommended
   import Button from '@mui/material/Button';
   import TextField from '@mui/material/TextField';
   import Card from '@mui/material/Card';

   Expected savings: -15KB gzipped

2. ⚠️ Charts bundle could be lazy loaded
   Fix: Dynamic import for charts panel
   Expected savings: -48KB from initial bundle

3. 💡 Enable Brotli compression on server
   Expected savings: Additional 20% reduction
```

---

## Summary

These examples show:
- ✅ Component generation with tests
- ✅ Custom hook patterns
- ✅ Guided feature workflow
- ✅ Accessibility auditing
- ✅ Performance optimization

**All patterns follow:**
- React 19 best practices
- TypeScript strict mode
- WCAG 2.1 AA compliance
- HIPAA considerations
- 80%+ test coverage
