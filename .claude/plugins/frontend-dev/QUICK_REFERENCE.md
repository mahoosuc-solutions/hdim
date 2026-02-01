# Frontend Dev Plugin - Quick Reference

**Version:** 1.0.0 | **Stack:** React 19 + TypeScript 5.9 + Vite 7 + MUI v7 + Zustand 5

---

## ⚡ Quick Commands

```bash
# Generate files
/frontend-dev:generate component MyComponent
/frontend-dev:generate hook useMyHook
/frontend-dev:generate utility myUtility
/frontend-dev:generate page MyPage

# Build & Test
/frontend-dev:build              # Production build
/frontend-dev:test               # Unit tests (watch)
/frontend-dev:test coverage      # Coverage report
/frontend-dev:lint               # Check code
/frontend-dev:lint true          # Auto-fix
/frontend-dev:e2e                # E2E tests
/frontend-dev:e2e ui             # E2E interactive

# Feature workflow
/frontend-dev:feature-dev "feature description"
```

---

## 🤖 Agents (Auto-invoke)

| Agent | Purpose | When |
|-------|---------|------|
| **code-reviewer** | React/TS best practices | PR review |
| **accessibility-analyzer** | WCAG compliance | Before deploy |
| **test-coverage-analyzer** | Test gap analysis | Coverage check |
| **performance-analyzer** | Bundle optimization | Build analysis |

---

## 📚 Skills (Knowledge Base)

```bash
# Ask Claude to invoke skills:
"Show me React patterns"       → react-patterns
"How to test hooks?"           → testing-strategy
"Customize MUI theme"          → mui-customization
"Zustand store patterns"       → zustand-patterns
```

---

## 🎯 Common Workflows

### New Component
```bash
1. /frontend-dev:generate component PatientCard
2. # Implement component
3. /frontend-dev:test watch PatientCard
4. /frontend-dev:lint true
```

### Complete Feature
```bash
1. /frontend-dev:feature-dev "patient search"
2. # Follow 9-stage guided workflow
3. # Auto code review & accessibility check
4. /frontend-dev:build
5. /frontend-dev:e2e
```

### Pre-Commit Checklist
```bash
✅ /frontend-dev:lint
✅ /frontend-dev:test run
✅ /frontend-dev:build
✅ Code reviewer passed
✅ Coverage ≥80%
```

---

## 📊 Quality Gates

| Metric | Target | Command |
|--------|--------|---------|
| **Test Coverage** | ≥80% | `test coverage` |
| **Bundle Size** | <200KB | `build` |
| **Accessibility** | WCAG AA | accessibility-analyzer |
| **Lint Errors** | 0 | `lint` |
| **E2E Critical Flows** | 100% | `e2e` |

---

## 🏥 HIPAA Reminders

**When handling PHI:**
- ⚠️ Never log patient data to console
- ⚠️ Cache TTL ≤ 5 minutes
- ⚠️ Proper access control
- ⚠️ Secure error messages

**Generated templates include HIPAA comments automatically.**

---

## 🆘 Troubleshooting

```bash
# Tests failing
npm run test:ui              # Visual debugger

# Build errors
npx tsc --noEmit             # Type check
rm -rf dist && npm run build # Clean build

# E2E failures
npm run e2e:debug            # Step through
npm run e2e:headed           # See browser

# Coverage too low
npm run test:coverage        # Identify gaps
# Then use test-coverage-analyzer agent
```

---

## 📖 File Locations

```
.claude/plugins/frontend-dev/
├── agents/             # 4 specialized agents
├── commands/           # 6 slash commands
├── skills/             # 4 knowledge skills
├── plugin.json         # Plugin manifest
├── README.md           # Full documentation
└── QUICK_REFERENCE.md  # This file
```

---

## 🔗 Stack Docs

- [React](https://react.dev) | [TypeScript](https://typescriptlang.org)
- [Vite](https://vitejs.dev) | [Vitest](https://vitest.dev)
- [MUI](https://mui.com) | [Zustand](https://zustand-demo.pmnd.rs)
- [Playwright](https://playwright.dev) | [RTL](https://testing-library.com/react)

---

**Need Help?** Ask Claude:
- "How do I test a custom hook?"
- "Show me MUI theming examples"
- "What's the React.memo pattern?"
- "Run the accessibility analyzer"

**Last Updated:** January 2026
