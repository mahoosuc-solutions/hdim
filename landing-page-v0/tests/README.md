# Landing Page Validation Suite

Comprehensive validation and testing infrastructure for the HDIM landing page.

## Overview

This validation suite ensures the HDIM landing page maintains high quality standards across:
- ✅ Content completeness and accuracy
- 🔗 Link integrity (internal and external)
- 🎭 End-to-end functionality
- ⚡ Performance (Lighthouse)
- ♿ Accessibility (WCAG 2.1)
- 🔒 Security

## Quick Start

### Install Dependencies

```bash
cd landing-page-v0
npm install
```

### Run All Validations

```bash
npm run validate:all
```

### Run Individual Validations

```bash
# Content validation
npm run validate:content

# Link checking
npm run validate:links

# E2E tests
npm run test:e2e

# Lighthouse performance
npm run test:lighthouse
```

## Validation Scripts

### 1. Content Validation (`validate:content`)

**Purpose**: Ensures all content is complete with no placeholders or missing sections.

**What it checks**:
- ❌ No placeholder text (TODO, [YOUR_...], Lorem ipsum)
- ✅ All required pages exist (home, features, pricing, demo, about, contact)
- ✅ Required content keywords present (HEDIS, FHIR, quality measures, etc.)
- ✅ All referenced images exist
- ✅ Internal links are valid

**Run**:
```bash
npm run validate:content
```

**Exit codes**:
- `0` - All validations passed
- `1` - Validation failed (errors found)

### 2. Link Checker (`validate:links`)

**Purpose**: Validates all external links return successful HTTP responses.

**What it checks**:
- ✅ All external URLs return 200-399 status codes
- ✅ No broken external links
- ⏱️ Respects rate limiting (100ms between requests)
- 🕐 10-second timeout per URL

**Run**:
```bash
npm run validate:links
```

**Exit codes**:
- `0` - All links working
- `1` - Broken links found

### 3. E2E Tests (`test:e2e`)

**Purpose**: Tests all critical user journeys and interactions using Playwright.

**Test coverage**:
- **Home Page**: Loading, navigation, hero CTA, footer links
- **Features Page**: Navigation, feature cards display
- **Pricing Page**: Pricing tiers, plan information
- **Contact/Demo Form**: Form validation, submission
- **Performance**: Page load times, meta tags
- **Responsive Design**: Mobile, tablet, desktop viewports
- **Analytics**: Tracking script presence
- **Accessibility**: Heading hierarchy, alt text, link text

**Browsers tested**:
- ✅ Chromium (Desktop Chrome)
- ✅ Firefox (Desktop Firefox)
- ✅ WebKit (Desktop Safari)
- ✅ Mobile Chrome (Pixel 5)
- ✅ Mobile Safari (iPhone 12)
- ✅ Tablet (iPad Pro)

**Run**:
```bash
# Run all tests
npm run test:e2e

# Run with UI mode for debugging
npm run test:e2e:ui

# Run specific browser
npx playwright test --project=chromium

# Run specific test file
npx playwright test tests/e2e/landing-page.spec.ts
```

**Results**:
- HTML report: `test-results/html/index.html`
- JSON report: `test-results/results.json`
- Screenshots: Captured on failure
- Videos: Recorded on failure

### 4. Lighthouse Performance (`test:lighthouse`)

**Purpose**: Validates performance, SEO, accessibility, and best practices using Lighthouse CI.

**Metrics**:
- **Performance**: ≥90% (FCP, LCP, CLS, TBT, Speed Index)
- **Accessibility**: ≥95% (ARIA, alt text, labels)
- **Best Practices**: ≥90% (HTTPS, console errors)
- **SEO**: ≥95% (meta tags, robots.txt)

**Pages tested**:
- `/` - Home
- `/features` - Features
- `/pricing` - Pricing
- `/demo` - Demo request
- `/about` - About
- `/contact` - Contact

**Run**:
```bash
npm run test:lighthouse
```

**Results**:
- Reports: `.lighthouseci/`
- Public storage: Temporary public URLs for sharing

## CI/CD Integration

### GitHub Actions Workflow

The validation suite runs automatically on:
- ✅ Push to `main`, `master`, `develop` branches
- ✅ Pull requests to `main`, `master`, `develop`
- ✅ Daily at 6 AM UTC (scheduled)
- ✅ Manual trigger via workflow_dispatch

**Workflow file**: `.github/workflows/landing-page-validation.yml`

**Jobs**:
1. **content-validation** - Checks content completeness
2. **link-check** - Validates external links
3. **e2e-tests** - Runs Playwright tests
4. **lighthouse** - Runs performance audits

**Artifacts**:
- Playwright reports (30 days retention)
- Lighthouse results (30 days retention)
- Test screenshots and videos

### Viewing Results

After workflow runs:
1. Go to **Actions** tab in GitHub
2. Select **Landing Page Validation** workflow
3. Click on specific run
4. Download artifacts from **Artifacts** section

## Local Development

### Running Tests During Development

```bash
# Start dev server
npm run dev

# In another terminal, run tests against localhost
BASE_URL=http://localhost:3000 npm run test:e2e
```

### Debugging Failed Tests

```bash
# Run in UI mode for interactive debugging
npm run test:e2e:ui

# Run in headed mode (see browser)
npx playwright test --headed

# Run with debug mode
npx playwright test --debug
```

### Updating Test Expectations

If page structure changes intentionally:

```bash
# Update screenshots (if using visual regression)
npx playwright test --update-snapshots
```

## Performance Benchmarks

### Target Metrics

| Metric | Target | Description |
|--------|--------|-------------|
| First Contentful Paint | < 2s | First text/image painted |
| Largest Contentful Paint | < 2.5s | Largest element painted |
| Cumulative Layout Shift | < 0.1 | Visual stability |
| Total Blocking Time | < 300ms | Main thread blocking |
| Speed Index | < 3s | Visual progress |
| Time to Interactive | < 3.5s | Fully interactive |

### Performance Optimization Tips

If Lighthouse scores drop:
1. **Images**: Use WebP/AVIF, optimize sizes
2. **JavaScript**: Code split, lazy load
3. **CSS**: Remove unused styles, minify
4. **Fonts**: Use font-display: swap
5. **Caching**: Implement proper cache headers

## Accessibility Standards

### WCAG 2.1 Level AA Compliance

All tests validate against WCAG 2.1 Level AA standards:
- ✅ Perceivable: Alt text, color contrast
- ✅ Operable: Keyboard navigation, focus states
- ✅ Understandable: Clear labels, error messages
- ✅ Robust: Valid HTML, ARIA attributes

### Common Accessibility Issues

Fix these before deployment:
- Missing alt text on images
- Insufficient color contrast (4.5:1 for normal text)
- Missing form labels
- Broken heading hierarchy
- Missing ARIA attributes

## Troubleshooting

### Content Validation Fails

**Issue**: Placeholders found
```
❌ Placeholder found in app/page.tsx: [YOUR_COMPANY]
```

**Fix**: Replace all placeholders with actual content

### Link Checker Fails

**Issue**: Broken external link
```
❌ https://example.com/old-page
   Status: 404
```

**Fix**: Update or remove broken links

### E2E Tests Fail

**Issue**: Element not found
```
Error: locator.click: Timeout 30000ms exceeded
```

**Fix**:
1. Check if element selector changed
2. Increase timeout if page is slow
3. Add `waitFor()` before interaction

### Lighthouse Fails

**Issue**: Performance score < 90%
```
Performance: 75/100
```

**Fix**:
1. Check which metrics are failing
2. Optimize images, scripts, CSS
3. Enable caching
4. Use CDN for static assets

## Configuration Files

### Playwright Config (`playwright.config.ts`)

- Test directory: `./tests/e2e`
- Browsers: Chromium, Firefox, WebKit, Mobile
- Base URL: `http://localhost:3000` (dev) or `$BASE_URL`
- Retries: 2 (in CI), 0 (locally)
- Screenshots: On failure
- Videos: On failure

### Lighthouse Config (`lighthouserc.json`)

- Desktop preset
- 3 runs per URL (median result)
- Strict thresholds (90%+ performance, 95%+ accessibility)
- Temporary public storage for reports

## Best Practices

### Before Committing

```bash
# Always run validation before committing
npm run validate:all

# If any validation fails, fix before committing
```

### Before Merging PR

1. ✅ All GitHub Actions checks pass
2. ✅ Review Lighthouse reports
3. ✅ Check accessibility report
4. ✅ Test on mobile device

### Before Production Deploy

1. ✅ All validations pass on `main` branch
2. ✅ Manual smoke test on staging
3. ✅ Verify analytics tracking
4. ✅ Check form submissions work
5. ✅ Test on real mobile devices

## Maintenance

### Updating Dependencies

```bash
# Update Playwright
npm install -D @playwright/test@latest

# Update Lighthouse CI
npm install -D @lhci/cli@latest

# Update browsers
npx playwright install
```

### Adding New Tests

1. Create test file in `tests/e2e/`
2. Follow existing test structure
3. Use descriptive test names
4. Add to CI workflow if needed

### Modifying Validation Rules

**Content Validator**: Edit `tests/validation/content-validator.ts`
- Add/remove placeholder patterns
- Update required content keywords
- Adjust required pages list

**Link Checker**: Edit `tests/validation/link-checker.ts`
- Adjust timeout
- Modify retry logic
- Update rate limiting

**Lighthouse**: Edit `lighthouserc.json`
- Adjust score thresholds
- Add/remove URLs to test
- Modify assertions

## Support

For issues or questions:
1. Check existing test output for clues
2. Review this README
3. Check Playwright/Lighthouse documentation
4. Open GitHub issue with test output

## Resources

- [Playwright Documentation](https://playwright.dev/)
- [Lighthouse CI Documentation](https://github.com/GoogleChrome/lighthouse-ci)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [Web Vitals](https://web.dev/vitals/)
