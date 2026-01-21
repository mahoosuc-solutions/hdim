---
name: /frontend-dev:generate
description: Generate React components, hooks, or utilities with tests and TypeScript types
args:
  type:
    description: Type of file to generate (component, hook, utility, page)
    required: true
  name:
    description: Name of the component/hook/utility (e.g., PatientCard, usePatientData, formatDate)
    required: true
  directory:
    description: Target directory (default: inferred from type)
    required: false
---

# Generate Frontend Code

Generate a new React component, hook, utility, or page with:
- ✅ TypeScript definitions
- ✅ Unit tests (Vitest + React Testing Library)
- ✅ Proper imports and structure
- ✅ MUI integration (for components)
- ✅ Accessibility best practices

## Usage Examples

```bash
# Generate a new component
/frontend-dev:generate component PatientCard

# Generate a custom hook
/frontend-dev:generate hook usePatientData

# Generate a utility function
/frontend-dev:generate utility formatPatientName

# Generate a new page/route
/frontend-dev:generate page ReportsPage
```

## Generation Logic

**1. Determine File Type:**
- `component` → Create in `frontend/src/components/`
- `hook` → Create in `frontend/src/hooks/`
- `utility` → Create in `frontend/src/utils/`
- `page` → Create in `frontend/src/pages/`

**2. Generate Files:**
Based on type, create appropriate files with templates.

### Component Template

**File: `frontend/src/components/{Name}.tsx`**
```typescript
import React from 'react';
import { Box, Typography } from '@mui/material';

export interface {Name}Props {
  // TODO: Define props
}

export const {Name}: React.FC<{Name}Props> = ({ }) => {
  return (
    <Box>
      <Typography variant="h6">{Name}</Typography>
      {/* TODO: Implement component */}
    </Box>
  );
};
```

**File: `frontend/src/components/{Name}.test.tsx`**
```typescript
import { render, screen } from '@testing-library/react';
import { {Name} } from './{Name}';
import { describe, it, expect } from 'vitest';

describe('{Name}', () => {
  it('should render successfully', () => {
    render(<{Name} />);
    expect(screen.getByText('{Name}')).toBeInTheDocument();
  });

  // TODO: Add more tests
});
```

### Hook Template

**File: `frontend/src/hooks/{name}.ts`**
```typescript
import { useState, useEffect } from 'react';

export interface Use{Name}Options {
  // TODO: Define options
}

export interface Use{Name}Return {
  // TODO: Define return type
  data: unknown | null;
  loading: boolean;
  error: Error | null;
}

export const {name} = (options?: Use{Name}Options): Use{Name}Return => {
  const [data, setData] = useState<unknown | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    // TODO: Implement hook logic
  }, []);

  return { data, loading, error };
};
```

**File: `frontend/src/hooks/{name}.test.ts`**
```typescript
import { renderHook } from '@testing-library/react';
import { {name} } from './{name}';
import { describe, it, expect } from 'vitest';

describe('{name}', () => {
  it('should return initial state', () => {
    const { result } = renderHook(() => {name}());

    expect(result.current.data).toBeNull();
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  // TODO: Add more tests
});
```

### Utility Template

**File: `frontend/src/utils/{name}.ts`**
```typescript
/**
 * {Name}
 *
 * @description TODO: Describe what this utility does
 * @param param - TODO: Describe parameter
 * @returns TODO: Describe return value
 *
 * @example
 * ```typescript
 * const result = {name}(input);
 * ```
 */
export function {name}(param: unknown): unknown {
  // TODO: Implement utility
  return param;
}
```

**File: `frontend/src/utils/{name}.test.ts`**
```typescript
import { {name} } from './{name}';
import { describe, it, expect } from 'vitest';

describe('{name}', () => {
  it('should handle basic case', () => {
    const result = {name}('test');
    expect(result).toBeDefined();
  });

  it('should handle edge cases', () => {
    expect(() => {name}(null)).not.toThrow();
  });

  // TODO: Add more tests
});
```

### Page Template

**File: `frontend/src/pages/{Name}Page.tsx`**
```typescript
import React from 'react';
import { Container, Typography, Box } from '@mui/material';

export const {Name}Page: React.FC = () => {
  return (
    <Container maxWidth="lg">
      <Box sx={{ py: 4 }}>
        <Typography variant="h4" gutterBottom>
          {Name}
        </Typography>
        {/* TODO: Implement page content */}
      </Box>
    </Container>
  );
};
```

**File: `frontend/src/pages/{Name}Page.test.tsx`**
```typescript
import { render, screen } from '@testing-library/react';
import { {Name}Page } from './{Name}Page';
import { describe, it, expect } from 'vitest';

describe('{Name}Page', () => {
  it('should render page heading', () => {
    render(<{Name}Page />);
    expect(screen.getByRole('heading', { name: /{Name}/i })).toBeInTheDocument();
  });

  // TODO: Add more tests
});
```

## Post-Generation Steps

After generating files:

1. ✅ **Verify File Creation**
   ```bash
   ls -l frontend/src/{directory}/{Name}*
   ```

2. ✅ **Run Tests**
   ```bash
   cd frontend
   npm run test {Name}
   ```

3. ✅ **Lint Check**
   ```bash
   npm run lint
   ```

4. ✅ **Add to Exports** (if needed)
   Update `frontend/src/{directory}/index.ts` with:
   ```typescript
   export { {Name} } from './{Name}';
   ```

## Next Steps

After generation, the developer should:

1. **Implement TODOs** - Replace placeholder logic
2. **Add Props/Types** - Define proper TypeScript interfaces
3. **Write Tests** - Complete test coverage
4. **Add Documentation** - JSDoc comments for public APIs
5. **Review Accessibility** - Ensure ARIA labels, keyboard navigation

## Healthcare Context

For components handling PHI (Protected Health Information):

```typescript
// Add security comments
/**
 * PatientCard Component
 *
 * ⚠️ HIPAA NOTICE: This component displays PHI
 * - Do NOT log patient data to console
 * - Ensure proper access control
 * - Follow HIPAA cache compliance (TTL ≤ 5 min)
 */
export const PatientCard: React.FC<PatientCardProps> = ({ patient }) => {
  // Implementation
};
```

## Example Output

After running `/frontend-dev:generate component PatientCard`:

```
✅ Created frontend/src/components/PatientCard.tsx
✅ Created frontend/src/components/PatientCard.test.tsx
✅ Files formatted with ESLint
✅ Tests passing (1/1)

Next steps:
1. Implement PatientCard component logic
2. Add props interface (PatientCardProps)
3. Complete test coverage
4. Review accessibility (ARIA labels)

Run tests: npm run test PatientCard
```

---

**Implementation:**

When this command is executed:
1. Parse arguments (type, name, directory)
2. Validate inputs
3. Generate files from templates
4. Create directory if needed
5. Write files with proper formatting
6. Run initial test to verify
7. Output summary and next steps
