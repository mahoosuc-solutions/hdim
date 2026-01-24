# Accessibility Testing Guide

## Overview

The HDIM Clinical Portal implements comprehensive accessibility testing to ensure WCAG 2.1 Level AA compliance. This guide explains how to run, write, and maintain accessibility tests.

## Quick Start

```bash
# Run all accessibility tests
npm test -- --testPathPattern=a11y.spec.ts

# Run specific component accessibility tests
npm test -- navigation.component.a11y.spec.ts
npm test -- care-gap-manager.component.a11y.spec.ts
npm test -- quality-measures.component.a11y.spec.ts

# Run with coverage
npm test -- --coverage --testPathPattern=a11y.spec.ts
```

## Testing Infrastructure

### Tools & Libraries

- **[axe-core](https://www.npmjs.com/package/axe-core)**: Industry-standard accessibility testing engine
- **[jest-axe](https://www.npmjs.com/package/jest-axe)**: Jest integration for axe-core
- **Angular TestBed**: Component testing framework

### Test Organization

Accessibility tests are separate from functional tests for clarity:

```
src/
├── app/
│   ├── components/
│   │   └── navigation/
│   │       ├── navigation.component.ts
│   │       ├── navigation.component.spec.ts          # Functional tests
│   │       └── navigation.component.a11y.spec.ts     # Accessibility tests
│   └── pages/
│       └── care-gaps/
│           ├── care-gap-manager.component.ts
│           ├── care-gap-manager.component.spec.ts
│           └── care-gap-manager.component.a11y.spec.ts
└── testing/
    ├── accessibility.helper.ts                        # Test utilities
    └── setup-accessibility-tests.ts                   # Jest configuration
```

## Writing Accessibility Tests

### Basic Test Structure

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { testAccessibility } from '../../../testing/accessibility.helper';
import { MyComponent } from './my.component';

describe('MyComponent - Accessibility', () => {
  let component: MyComponent;
  let fixture: ComponentFixture<MyComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyComponent, /* required modules */],
    }).compileComponents();

    fixture = TestBed.createComponent(MyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('WCAG 2.1 Level A Compliance', () => {
    it('should have no Level A accessibility violations', async () => {
      const results = await testAccessibility(fixture);
      expect(results).toHaveNoViolations();
    });
  });

  describe('WCAG 2.1 Level AA Compliance', () => {
    it('should have valid ARIA attributes', async () => {
      const results = await testAriaAttributes(fixture);
      expect(results).toHaveNoViolations();
    });

    it('should support keyboard navigation', async () => {
      const results = await testKeyboardAccessibility(fixture);
      expect(results).toHaveNoViolations();
    });
  });
});
```

### Available Test Helpers

**`testAccessibility(fixture, config?)`**
- Runs comprehensive accessibility tests
- Default: WCAG 2.1 Level A + AA rules
- Returns axe-core results

**`testKeyboardAccessibility(fixture)`**
- Tests keyboard navigation
- Validates focus order and tabindex
- Checks interactive element roles

**`testAriaAttributes(fixture)`**
- Validates ARIA attribute usage
- Checks required ARIA properties
- Verifies ARIA roles

**`testColorContrast(fixture)`**
- Validates color contrast ratios
- WCAG 2.1 Level AA: 4.5:1 normal text, 3:1 large text
- Checks text on colored backgrounds

**`testAccessibilityForElement(fixture, selector, config?)`**
- Tests specific element within component
- Useful for complex components
- Isolates accessibility tests

### Example Tests

**Test Skip Navigation Links (WCAG 2.1 2.4.1)**

```typescript
it('should have skip navigation links', () => {
  const skipLinks = fixture.nativeElement.querySelector('.skip-links');
  expect(skipLinks).toBeTruthy();

  const skipToMain = fixture.nativeElement.querySelector('a[href="#main-content"]');
  expect(skipToMain).toBeTruthy();
  expect(skipToMain.textContent).toContain('Skip to main content');
});
```

**Test ARIA Labels on Action Buttons (WCAG 2.1 4.1.2)**

```typescript
it('should have descriptive ARIA labels on action buttons', async () => {
  await fixture.whenStable();
  fixture.detectChanges();

  const actionButtons = fixture.nativeElement.querySelectorAll('.action-button');

  actionButtons.forEach((button: HTMLElement) => {
    const ariaLabel = button.getAttribute('aria-label');
    expect(ariaLabel).toBeTruthy();
    expect(ariaLabel?.length).toBeGreaterThan(10); // Should be descriptive
  });
});
```

**Test Keyboard Navigation (WCAG 2.1 2.4.7)**

```typescript
it('should have focusable interactive elements', () => {
  const focusableElements = fixture.nativeElement.querySelectorAll(
    'button:not([disabled]), a[href], input:not([disabled])'
  );

  expect(focusableElements.length).toBeGreaterThan(0);

  focusableElements.forEach((element: HTMLElement) => {
    expect(element.tabIndex).toBeGreaterThanOrEqual(0);
  });
});
```

**Test Color Contrast (WCAG 2.1 1.4.3)**

```typescript
it('should meet color contrast requirements', async () => {
  const results = await testColorContrast(fixture);
  expect(results).toHaveNoViolations();
});
```

## WCAG 2.1 Level AA Checklist

### Perceivable

- [x] **1.1.1 Non-text Content**: All images have alt text
- [x] **1.3.1 Info and Relationships**: Semantic HTML and ARIA
- [x] **1.4.3 Contrast (Minimum)**: 4.5:1 normal text, 3:1 large text
- [ ] **1.4.11 Non-text Contrast**: UI components have 3:1 contrast

### Operable

- [x] **2.1.1 Keyboard**: All functionality via keyboard
- [x] **2.4.1 Bypass Blocks**: Skip navigation links
- [x] **2.4.3 Focus Order**: Logical focus order
- [x] **2.4.7 Focus Visible**: Visible focus indicators
- [ ] **2.5.5 Target Size**: 44x44px minimum touch targets

### Understandable

- [x] **3.2.3 Consistent Navigation**: Same navigation on all pages
- [x] **3.3.1 Error Identification**: Errors clearly described
- [x] **3.3.2 Labels or Instructions**: Form fields labeled

### Robust

- [x] **4.1.2 Name, Role, Value**: ARIA attributes on all components
- [x] **4.1.3 Status Messages**: Status updates announced

## Running Tests in CI/CD

### GitHub Actions Integration

```yaml
# .github/workflows/accessibility-tests.yml
name: Accessibility Tests

on: [pull_request]

jobs:
  a11y-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'

      - name: Install dependencies
        run: npm ci

      - name: Run accessibility tests
        run: npm test -- --testPathPattern=a11y.spec.ts --coverage

      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./coverage/lcov.info
```

### Pre-commit Hooks

```bash
# .husky/pre-commit
#!/bin/sh
. "$(dirname "$0")/_/husky.sh"

npm test -- --testPathPattern=a11y.spec.ts --bail --findRelatedTests
```

## Manual Testing Checklist

Automated tests catch ~30-40% of accessibility issues. Manual testing is required for complete coverage.

### Keyboard Navigation

- [ ] Tab through all interactive elements
- [ ] Verify visible focus indicators
- [ ] Test skip navigation links
- [ ] Verify logical tab order
- [ ] Test keyboard shortcuts (if any)
- [ ] Escape key closes modals/dialogs

### Screen Reader Testing

**NVDA (Windows) - Free**

```bash
# Download from: https://www.nvaccess.org/download/
```

**JAWS (Windows) - Commercial**

```bash
# Download trial from: https://www.freedomscientific.com/products/software/jaws/
```

**VoiceOver (macOS) - Built-in**

```bash
# Enable: System Preferences > Accessibility > VoiceOver
# Toggle: Cmd + F5
```

**Test Checklist**

- [ ] Page title announced
- [ ] Headings announced with level
- [ ] Links announced with clear text
- [ ] Buttons announced with purpose
- [ ] Form fields announced with labels
- [ ] Error messages announced
- [ ] Loading states announced
- [ ] Table headers associated with data

### Color Contrast

**Tools**

- Chrome DevTools Lighthouse
- axe DevTools Chrome Extension
- Contrast Checker: https://webaim.org/resources/contrastchecker/

**Test Checklist**

- [ ] Text on primary background: 4.5:1
- [ ] Large text (18pt+): 3:1
- [ ] Button text: 4.5:1
- [ ] Link text: 4.5:1
- [ ] Form validation errors: 4.5:1
- [ ] Status indicators: 3:1

### Touch Target Size

- [ ] All buttons ≥ 44x44px
- [ ] Icon buttons ≥ 44x44px
- [ ] Form inputs ≥ 44px height
- [ ] Links have adequate spacing

## Troubleshooting

### Common Issues

**Error: "Cannot find module 'jest-axe'"**

```bash
npm install --save-dev jest-axe axe-core
```

**Error: "toHaveNoViolations is not a function"**

Ensure `setup-accessibility-tests.ts` is in `jest.config.ts`:

```typescript
setupFilesAfterEnv: [
  '<rootDir>/src/test-setup.ts',
  '<rootDir>/src/testing/setup-accessibility-tests.ts',
],
```

**Tests Timeout**

Increase timeout in test file:

```typescript
jest.setTimeout(10000); // 10 seconds
```

**Material Design Components Not Rendering**

Import `BrowserAnimationsModule` or `NoopAnimationsModule`:

```typescript
await TestBed.configureTestingModule({
  imports: [BrowserAnimationsModule, MyComponent],
}).compileComponents();
```

### False Positives

**Color Contrast on Disabled Elements**

Disabled elements may fail contrast checks but are exempt from WCAG:

```typescript
const results = await testColorContrast(fixture);

// Filter out disabled element violations
const violations = results.violations.filter(
  (v: any) => !v.nodes.some((n: any) => n.element.disabled)
);

expect(violations).toHaveLength(0);
```

**Dynamic Content Not Loaded**

Wait for data before testing:

```typescript
it('should have accessible table', async () => {
  await fixture.whenStable();
  fixture.detectChanges();

  // Allow time for data loading
  await new Promise(resolve => setTimeout(resolve, 100));

  const results = await testAccessibility(fixture);
  expect(results).toHaveNoViolations();
});
```

## Resources

### WCAG 2.1 Guidelines

- [WCAG 2.1 Quick Reference](https://www.w3.org/WAI/WCAG21/quickref/)
- [Understanding WCAG 2.1](https://www.w3.org/WAI/WCAG21/Understanding/)
- [How to Meet WCAG 2.1](https://www.w3.org/WAI/WCAG21/quickref/)

### Testing Tools

- [axe-core Documentation](https://github.com/dequelabs/axe-core)
- [jest-axe Documentation](https://github.com/nickcolley/jest-axe)
- [Chrome DevTools Lighthouse](https://developers.google.com/web/tools/lighthouse)
- [axe DevTools Extension](https://www.deque.com/axe/devtools/)

### Angular Accessibility

- [Angular Accessibility](https://angular.io/guide/accessibility)
- [Material Accessibility](https://material.angular.io/guide/accessibility)
- [CDK Accessibility](https://material.angular.io/cdk/a11y/overview)

### HIPAA Compliance

- [HIPAA Accessibility Requirements](https://www.hhs.gov/civil-rights/for-individuals/section-1557/index.html)
- [Section 508 Standards](https://www.section508.gov/)

## Continuous Improvement

### Adding New Components

When creating new components:

1. Create `component.a11y.spec.ts` file
2. Add basic accessibility tests
3. Run tests locally
4. Fix violations before committing
5. Add component-specific tests

### Monitoring Accessibility

**Weekly Review**

```bash
# Run full accessibility test suite
npm test -- --testPathPattern=a11y.spec.ts --coverage

# Generate accessibility report
npm test -- --testPathPattern=a11y.spec.ts --json --outputFile=a11y-report.json
```

**Quarterly Audit**

- Manual screen reader testing
- External accessibility audit
- Update WCAG checklist
- Review new WCAG guidelines

## Support

For questions or issues with accessibility testing:

- Review this guide
- Check [WCAG 2.1 documentation](https://www.w3.org/WAI/WCAG21/quickref/)
- Consult [axe-core rules](https://github.com/dequelabs/axe-core/blob/develop/doc/rule-descriptions.md)
- Open issue in project repository

---

**Last Updated**: January 2026
**WCAG Version**: 2.1 Level AA
**Testing Framework**: Jest + axe-core + Angular TestBed
