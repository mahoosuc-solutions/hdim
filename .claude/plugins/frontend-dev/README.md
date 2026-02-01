# Frontend Development Plugin

Comprehensive toolkit for React + TypeScript frontend development with testing, accessibility, and code quality automation.

## 🚀 Features

### 🤖 Specialized Agents
- **Code Reviewer** - Review React code for best practices, performance, accessibility
- **Accessibility Analyzer** - Ensure WCAG 2.1 AA compliance
- **Test Coverage Analyzer** - Identify gaps and improve test quality
- **Performance Analyzer** - Optimize bundle size and runtime performance

### ⚡ Quick Commands
- **generate** - Scaffold components, hooks, utilities with tests
- **build** - Build production bundle with analysis
- **test** - Run unit tests with coverage
- **lint** - Code quality checks with auto-fix
- **e2e** - End-to-end testing with Playwright
- **feature-dev** - Guided feature development workflow

### 📚 Knowledge Skills
- **React Patterns** - Modern React 19 patterns and best practices
- **Testing Strategy** - Comprehensive testing guide (Vitest + Playwright)
- **MUI Customization** - Material UI theming and styling
- **Zustand Patterns** - State management with Zustand

---

## 📦 Installation

This plugin is auto-discovered from `.claude/plugins/frontend-dev/`.

No installation required - it's ready to use!

---

## 🎯 Quick Start

### Generate a Component

```bash
/frontend-dev:generate component PatientCard
```

Creates:
- `src/components/PatientCard.tsx`
- `src/components/PatientCard.test.tsx`

### Run Tests

```bash
# Watch mode (development)
/frontend-dev:test

# With coverage
/frontend-dev:test coverage

# Specific test
/frontend-dev:test watch PatientCard
```

### Build for Production

```bash
/frontend-dev:build
```

Outputs:
- Production bundle in `dist/`
- Bundle size analysis
- Optimization recommendations

### Code Review

```bash
# Review recent changes
# Uses the code-reviewer agent automatically on git diff
```

---

## 🔧 Commands Reference

### /frontend-dev:generate

Generate scaffolded files with tests.

**Syntax:**
```bash
/frontend-dev:generate <type> <name> [directory]
```

**Types:**
- `component` - React component (TSX + test)
- `hook` - Custom React hook (TS + test)
- `utility` - Utility function (TS + test)
- `page` - Page component (TSX + test)

**Examples:**
```bash
/frontend-dev:generate component SearchBar
/frontend-dev:generate hook usePatientData
/frontend-dev:generate utility formatDate
/frontend-dev:generate page DashboardPage
```

### /frontend-dev:build

Build production bundle.

**Features:**
- TypeScript compilation
- Vite production build
- Bundle size analysis
- Optimization suggestions

### /frontend-dev:test

Run tests with Vitest.

**Modes:**
- `watch` (default) - Re-run on changes
- `run` - Run once and exit
- `ui` - Interactive UI mode
- `coverage` - Generate coverage report

**Examples:**
```bash
/frontend-dev:test                    # Watch mode
/frontend-dev:test run                # CI mode
/frontend-dev:test coverage           # Coverage report
/frontend-dev:test watch PatientCard  # Specific test
```

### /frontend-dev:lint

ESLint code quality checks.

**Syntax:**
```bash
/frontend-dev:lint [fix]
```

**Examples:**
```bash
/frontend-dev:lint       # Check only
/frontend-dev:lint true  # Auto-fix issues
```

### /frontend-dev:e2e

End-to-end testing with Playwright.

**Modes:**
- `run` (default) - Headless execution
- `ui` - Interactive UI mode
- `headed` - Visible browser
- `debug` - Step-through debugging

**Browsers:**
- `all` (default) - All browsers
- `chromium` - Chrome/Edge
- `firefox` - Firefox
- `webkit` - Safari

**Examples:**
```bash
/frontend-dev:e2e                  # All browsers, headless
/frontend-dev:e2e ui               # Interactive mode
/frontend-dev:e2e headed           # See browser
/frontend-dev:e2e run chromium     # Chrome only
```

### /frontend-dev:feature-dev

Guided feature development workflow.

**Stages:**
1. Planning & Design
2. Setup & Scaffolding
3. Implementation
4. Testing (≥85% coverage)
5. Accessibility Audit
6. Code Review
7. Performance Optimization
8. Documentation
9. Final QA

**Example:**
```bash
/frontend-dev:feature-dev "patient search with filtering"
```

---

## 🤖 Agents Reference

### Code Reviewer

**Purpose:** Review React code for quality, performance, accessibility.

**Reviews:**
- React best practices
- TypeScript type safety
- Testing coverage
- Accessibility
- Performance patterns
- Code duplication

**Usage:**
```bash
# Automatically triggered on git diff in PRs
# Or manually invoke for specific files
```

**Output:** Detailed review report with:
- Critical issues 🔴
- Warnings ⚠️
- Suggestions 💡
- Good practices ✅

### Accessibility Analyzer

**Purpose:** WCAG 2.1 AA compliance auditing.

**Checks:**
- Keyboard navigation
- ARIA labels
- Screen reader support
- Color contrast (≥4.5:1)
- Focus indicators
- Semantic HTML

**Usage:**
```bash
# Invoke on components before production
```

**Output:** Accessibility audit report with violations and fixes.

### Test Coverage Analyzer

**Purpose:** Analyze test quality and coverage gaps.

**Analyzes:**
- Unit test coverage
- Integration test coverage
- E2E test coverage
- Test quality (RTL best practices)
- Untested code paths

**Coverage Targets:**
- Overall: ≥80%
- Critical components: ≥90%
- Business logic: ≥95%

**Output:** Coverage report with gap analysis and recommendations.

### Performance Analyzer

**Purpose:** Optimize bundle size and runtime performance.

**Analyzes:**
- Bundle size (target: <200KB gzipped)
- Component re-renders
- List virtualization opportunities
- Code splitting potential
- Image optimization

**Output:** Performance report with actionable optimizations.

---

## 📚 Skills Reference

### React Patterns

**Topics:**
- Functional components
- Hooks patterns (useState, useEffect, useCallback, useMemo, useRef)
- Custom hooks
- Performance optimization (React.memo, lazy loading)
- Error boundaries
- Code organization

**Use when:**
- Building new components
- Refactoring code
- Learning React best practices
- Code review reference

### Testing Strategy

**Topics:**
- Unit testing (Vitest + RTL)
- Integration testing
- E2E testing (Playwright)
- Testing best practices
- Mocking strategies
- Coverage targets

**Use when:**
- Writing tests
- Improving coverage
- Debugging tests
- Setting up test infrastructure

### MUI Customization

**Topics:**
- Theming (colors, typography, spacing)
- Component customization
- Styling patterns (sx, styled)
- Responsive design
- Accessibility
- Performance optimization

**Use when:**
- Theming application
- Customizing MUI components
- Implementing responsive layouts
- Styling components

### Zustand Patterns

**Topics:**
- Store creation
- Selectors & performance
- Middleware (persist, devtools, immer)
- Async actions
- Store slices (modular pattern)
- Testing stores

**Use when:**
- Setting up global state
- Optimizing re-renders
- Managing async state
- Testing state management

---

## 🎨 Tech Stack

**Frontend Framework:**
- React 19.1 + TypeScript 5.9
- Vite 7 (build tool)

**UI Components:**
- Material UI v7

**State Management:**
- Zustand 5

**Testing:**
- Vitest 4 (unit tests)
- React Testing Library
- Playwright 1.57 (E2E tests)

**Code Quality:**
- ESLint 9
- TypeScript strict mode

---

## 🏥 Healthcare Context

This plugin is designed for the **HDIM (HealthData-in-Motion)** healthcare platform.

**HIPAA Considerations:**
- Never log PHI (Protected Health Information)
- Follow cache TTL compliance (≤5 minutes)
- Proper access control
- Secure error messages

**Generated components handling PHI include security comments:**
```typescript
/**
 * PatientCard Component
 *
 * ⚠️ HIPAA NOTICE: This component displays PHI
 * - Do NOT log patient data to console
 * - Ensure proper access control
 * - Follow HIPAA cache compliance (TTL ≤ 5 min)
 */
```

---

## 📊 Quality Standards

### Code Quality
- ✅ ESLint passing (no errors)
- ✅ TypeScript strict mode
- ✅ No `any` types (use `unknown`)
- ✅ Proper error handling

### Testing
- ✅ ≥80% overall coverage
- ✅ ≥90% for critical components
- ✅ All new components have tests
- ✅ E2E tests for critical flows

### Accessibility
- ✅ WCAG 2.1 AA compliant
- ✅ Keyboard navigation works
- ✅ Screen reader compatible
- ✅ Color contrast ≥4.5:1

### Performance
- ✅ Initial bundle <200KB gzipped
- ✅ First Contentful Paint <1s
- ✅ Time to Interactive <2.5s
- ✅ No unnecessary re-renders

---

## 🔄 Workflow Example

Complete feature development:

```bash
# 1. Start feature development
/frontend-dev:feature-dev "patient search with filtering"

# 2. Generate components
/frontend-dev:generate component SearchBar
/frontend-dev:generate component FilterPanel
/frontend-dev:generate hook usePatientSearch

# 3. Implement feature
# ... write code ...

# 4. Run tests during development
/frontend-dev:test watch

# 5. Check coverage
/frontend-dev:test coverage

# 6. Lint code
/frontend-dev:lint true

# 7. Build for production
/frontend-dev:build

# 8. Run E2E tests
/frontend-dev:e2e

# 9. Final review (agents run automatically)
# - Code reviewer analyzes git diff
# - Accessibility analyzer checks compliance
# - Performance analyzer optimizes bundle

# 10. Ready for PR! 🎉
```

---

## 🐛 Troubleshooting

### Tests Failing

```bash
# Check specific test
npm run test PatientCard

# Run with UI for debugging
npm run test:ui

# Check coverage
npm run test:coverage
```

### Build Errors

```bash
# Type check separately
npx tsc --noEmit

# Clean and rebuild
rm -rf dist node_modules
npm install
npm run build
```

### E2E Tests Failing

```bash
# Debug mode
npm run e2e:debug

# Run in headed mode (see browser)
npm run e2e:headed

# Run specific test
npx playwright test patient-search.spec.ts
```

---

## 📝 Contributing

When extending this plugin:

1. **Add new command:**
   - Create `.md` file in `commands/`
   - Add to `plugin.json`
   - Follow command template

2. **Add new agent:**
   - Create `.md` file in `agents/`
   - Add to `plugin.json`
   - Define tools and when_to_use

3. **Add new skill:**
   - Create `.md` file in `skills/`
   - Add to `plugin.json`
   - Provide comprehensive examples

---

## 📄 License

Part of the HDIM platform. Internal use only.

---

## 🙏 Acknowledgments

Built for the HDIM Platform Team to accelerate frontend development with quality, accessibility, and testing built-in.

**Version:** 1.0.0
**Author:** HDIM Platform Team
**Last Updated:** January 2026
