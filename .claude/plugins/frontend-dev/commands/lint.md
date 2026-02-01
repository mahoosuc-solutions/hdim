---
name: /frontend-dev:lint
description: Run ESLint to check code quality and auto-fix issues
args:
  fix:
    description: Automatically fix problems (true/false)
    required: false
    default: false
---

# Lint Frontend Code

Run ESLint to enforce code quality, style, and best practices.

## Usage

```bash
# Check for linting issues
/frontend-dev:lint

# Auto-fix fixable issues
/frontend-dev:lint true
```

## Lint Process

### 1. Check Only

```bash
cd frontend
npm run lint
```

**Output:**
```
/src/components/PatientCard.tsx
  12:5  warning  'patient' is missing in props validation  react/prop-types
  24:3  error    'useEffect' Hook has a missing dependency    react-hooks/exhaustive-deps

/src/hooks/usePatientData.ts
  8:10  warning  'data' is assigned a value but never used   @typescript-eslint/no-unused-vars

✖ 3 problems (1 error, 2 warnings)
  0 errors and 0 warnings potentially fixable with the `--fix` option.
```

### 2. Auto-Fix

```bash
npm run lint -- --fix
```

**What Gets Fixed:**
- ✅ Unused imports removed
- ✅ Quotes normalized (single vs double)
- ✅ Semicolons added/removed
- ✅ Spacing and indentation
- ✅ Trailing commas

**What Requires Manual Fix:**
- ❌ Missing React Hook dependencies
- ❌ Unused variables in complex logic
- ❌ Type errors
- ❌ Logic errors

## ESLint Configuration

**File: `frontend/eslint.config.js`**
```javascript
import js from '@eslint/js';
import globals from 'globals';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  { ignores: ['dist'] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': [
        'warn',
        { allowConstantExport: true },
      ],
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_' }
      ],
      '@typescript-eslint/no-explicit-any': 'warn',
    },
  },
);
```

## Common Linting Rules

### React Hooks Rules

**✅ Correct:**
```typescript
// Complete dependencies
useEffect(() => {
  fetchPatients(tenantId, filter);
}, [tenantId, filter]);

// Memoized callbacks
const handleClick = useCallback(() => {
  onClick(id);
}, [onClick, id]);
```

**❌ Incorrect:**
```typescript
// Missing dependency
useEffect(() => {
  fetchPatients(tenantId, filter);
}, [tenantId]); // ❌ Missing 'filter'

// Stale closure
const handleClick = () => {
  onClick(id); // ❌ 'id' might be stale
};
```

### TypeScript Rules

**✅ Correct:**
```typescript
// Explicit types
interface PatientProps {
  patient: Patient;
  onClick: (id: string) => void;
}

// Avoid 'any'
function formatData(data: unknown): string {
  if (typeof data === 'string') {
    return data;
  }
  return JSON.stringify(data);
}
```

**❌ Incorrect:**
```typescript
// Implicit any
function formatData(data): string { // ❌
  return data.toString();
}

// Unnecessary any
function process(value: any) { // ❌ Use 'unknown' instead
  return value;
}
```

### Code Quality Rules

**✅ Correct:**
```typescript
// No unused variables
import { useState, useEffect } from 'react'; // All used

// Consistent naming
const PatientCard = () => { /* ... */ };
const usePatientData = () => { /* ... */ };
```

**❌ Incorrect:**
```typescript
// Unused imports
import { useState, useEffect, useMemo } from 'react'; // ❌ useMemo not used

// Unused variables
const handleClick = () => { /* ... */ }; // ❌ Never called
```

## Custom Rules (Healthcare-Specific)

Add custom rules for PHI handling:

```javascript
// eslint.config.js
rules: {
  // Prevent console.log in production
  'no-console': process.env.NODE_ENV === 'production' ? 'error' : 'warn',

  // Require HIPAA comments on PHI components
  // (Custom plugin needed)
  'hipaa/require-phi-comment': 'error',
}
```

## IDE Integration

### VS Code

**.vscode/settings.json:**
```json
{
  "eslint.enable": true,
  "eslint.validate": [
    "javascript",
    "typescript",
    "typescriptreact"
  ],
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  }
}
```

### WebStorm/IntelliJ

1. Settings → Languages & Frameworks → JavaScript → Code Quality Tools → ESLint
2. Enable "Automatic ESLint configuration"
3. Enable "Run eslint --fix on save"

## Pre-commit Hook

**Install Husky + lint-staged:**
```bash
npm install --save-dev husky lint-staged

# Setup pre-commit hook
npx husky install
npx husky add .husky/pre-commit "npx lint-staged"
```

**package.json:**
```json
{
  "lint-staged": {
    "frontend/src/**/*.{ts,tsx}": [
      "eslint --fix",
      "prettier --write"
    ]
  }
}
```

## CI/CD Integration

**GitHub Actions:**
```yaml
- name: Lint frontend code
  run: |
    cd frontend
    npm ci
    npm run lint

- name: Fail on errors
  run: |
    cd frontend
    # Fail build if any errors (warnings ok)
    npm run lint -- --max-warnings=0
```

## Common Issues & Fixes

### Issue: "Parsing error: Cannot read file"

**Fix:**
```bash
# Ensure TypeScript config exists
ls frontend/tsconfig.json

# Reinstall dependencies
cd frontend && npm install
```

### Issue: "Rule 'react-hooks/exhaustive-deps' not found"

**Fix:**
```bash
# Install missing plugin
npm install --save-dev eslint-plugin-react-hooks
```

### Issue: Too many warnings

**Strategy:**
1. Fix errors first (blockers)
2. Gradually reduce warnings
3. Set max-warnings in CI

```javascript
// Temporarily reduce severity
rules: {
  'react-hooks/exhaustive-deps': 'warn', // Change to 'error' later
}
```

## Output Examples

### Clean Code ✅

```
✅ All files passed linting!

  12 files checked
  0 errors
  0 warnings

Build ready for production 🚀
```

### Issues Found ❌

```
❌ Linting issues found

/src/components/Dashboard.tsx
  24:5   error    React Hook useEffect has a missing dependency: 'filter'   react-hooks/exhaustive-deps
  45:10  warning  'unused' is assigned a value but never used               @typescript-eslint/no-unused-vars

/src/utils/formatDate.ts
  8:15   error    Unexpected any. Specify a different type                  @typescript-eslint/no-explicit-any

✖ 3 problems (2 errors, 1 warning)
  0 errors and 1 warning potentially fixable with the `--fix` option.

💡 Try running with --fix to auto-fix warnings
```

### Auto-fix Applied ✅

```
✅ Auto-fix completed

  Fixed 15 issues:
    - Removed 5 unused imports
    - Fixed 8 spacing issues
    - Fixed 2 quote style issues

  Remaining issues: 2 errors (require manual fix)

Please review changes and run tests.
```

## Best Practices

1. **Run lint before commit**
   - Catches issues early
   - Maintains code quality

2. **Fix errors before warnings**
   - Errors block build
   - Warnings can be addressed gradually

3. **Don't disable rules without reason**
   ```typescript
   // ❌ Bad - No explanation
   // eslint-disable-next-line react-hooks/exhaustive-deps

   // ✅ Good - Clear reason
   // eslint-disable-next-line react-hooks/exhaustive-deps
   // Intentionally run only on mount
   ```

4. **Keep rules consistent**
   - Team agreement on style
   - Document exceptions

5. **Review auto-fixes**
   - Don't blindly accept
   - Understand changes

---

**When to Run:**
- Before committing code
- As part of pre-commit hook
- In CI/CD pipeline
- After dependency updates
- During code reviews
